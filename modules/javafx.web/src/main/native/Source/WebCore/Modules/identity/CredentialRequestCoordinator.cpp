/*
 * Copyright (C) 2025 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
#include "config.h"
#include "CredentialRequestCoordinator.h"

#if ENABLE(WEB_AUTHN)

#include "AbortSignal.h"
#include "Chrome.h"
#include "CredentialRequestCoordinatorClient.h"
#include "DigitalCredential.h"
#include "DigitalCredentialsRequestData.h"
#include "DigitalCredentialsResponseData.h"
#include "Document.h"
#include "ExceptionData.h"
#include "JSDigitalCredential.h"
#include "Page.h"
#include <JavaScriptCore/JSObject.h>
#include <Logging.h>
#include <wtf/TZoneMallocInlines.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

WTF_MAKE_TZONE_ALLOCATED_IMPL(CredentialRequestCoordinator);
WTF_MAKE_TZONE_ALLOCATED_IMPL(CredentialRequestCoordinatorClient);

Ref<CredentialRequestCoordinator> CredentialRequestCoordinator::create(UniqueRef<CredentialRequestCoordinatorClient>&& client, Page& page)
{
    return adoptRef(*new CredentialRequestCoordinator(WTFMove(client), page));
}

CredentialRequestCoordinator::CredentialRequestCoordinator(UniqueRef<CredentialRequestCoordinatorClient>&& client, Page& page)
    : m_client(WTFMove(client))
    , m_page(page)
{
}

CredentialRequestCoordinator::PickerStateGuard::PickerStateGuard(CredentialRequestCoordinator& coordinator)
    : m_coordinator(coordinator)
{
}

CredentialRequestCoordinator::PickerStateGuard::PickerStateGuard(PickerStateGuard&& other) noexcept
    : m_coordinator(WTFMove(other.m_coordinator))
    , m_active(other.m_active)
{
    other.m_active = false;
}

CredentialRequestCoordinator::PickerStateGuard&
CredentialRequestCoordinator::PickerStateGuard::operator=(PickerStateGuard&& other) noexcept
{
    if (this != &other) {
        if (m_active)
            m_coordinator->setState(PickerState::Idle);
        m_active = other.m_active;
        other.m_active = false;
    }
    return *this;
}

CredentialRequestCoordinator::PickerStateGuard::~PickerStateGuard()
{
    if (m_active)
        m_coordinator->setState(PickerState::Idle);
}

CredentialRequestCoordinator::PickerState CredentialRequestCoordinator::currentState() const
{
    return m_state;
}

bool CredentialRequestCoordinator::canTransitionTo(PickerState newState) const
{
    switch (m_state) {
    case PickerState::Idle:
        return newState == PickerState::Presenting;
    case PickerState::Presenting:
        return newState == PickerState::Aborting || newState == PickerState::Idle;
    case PickerState::Aborting:
        return newState == PickerState::Idle;
    }
    ASSERT_NOT_REACHED();
    return false;
}

void CredentialRequestCoordinator::setState(PickerState newState)
{
    if (m_state == newState || !canTransitionTo(newState))
        return;
    m_state = newState;
}

void CredentialRequestCoordinator::setCurrentPromise(CredentialPromise&& promise)
{
    ASSERT(!m_currentPromise.has_value());
    m_currentPromise = WTFMove(promise);
}

CredentialPromise* CredentialRequestCoordinator::currentPromise()
{
    return m_currentPromise ? &m_currentPromise.value() : nullptr;
}

void CredentialRequestCoordinator::presentPicker(CredentialPromise&& promise, DigitalCredentialsRequestData&& requestData, RefPtr<AbortSignal> signal)
{
    if (!canPresentDigitalCredentialsUI()) {
        LOG(DigitalCredentials, "There's no digital credentials UI available.");
        promise.reject(Exception { ExceptionCode::NotSupportedError, "Digital credentials are not supported."_s });
        return;
    }

    if (m_state != PickerState::Idle) {
        LOG(DigitalCredentials, "A credential picker operation is already in progress");
        promise.reject(Exception {
            ExceptionCode::InvalidStateError,
            "A credential picker operation is already in progress."_s });
        return;
    }

    if (!m_page) {
        promise.reject(ExceptionCode::InvalidStateError, "Page no longer valid."_s);
                return;
    }

    if (signal) {
        if (signal->aborted()) {
            LOG(DigitalCredentials, "AbortSignal was already aborted before presenting the credential picker");
            promise.rejectType<IDLAny>(signal->reason().getValue());
            return;
        }

        auto weakThis = WeakPtr { *this };
        signal->addAlgorithm([weakThis, signal = RefPtr { signal }](JSC::JSValue reason) {
            if (!weakThis)
                return;
            LOG(DigitalCredentials, "Credential picker was aborted by AbortSignal");
            weakThis->abortPicker(reason);
            });
    }

    setState(PickerState::Presenting);
    setCurrentPromise(WTFMove(promise));

    auto weakThis = WeakPtr { *this };
    m_client->showDigitalCredentialsPicker(
        requestData,
        [weakThis = WeakPtr { *this }, signal](Expected<DigitalCredentialsResponseData, ExceptionData>&& responseOrException) {
            if (RefPtr protectedThis = weakThis.get())
                protectedThis->handleDigitalCredentialsPickerResult(WTFMove(responseOrException), signal);
        });
}

void CredentialRequestCoordinator::handleDigitalCredentialsPickerResult(Expected<DigitalCredentialsResponseData, ExceptionData>&& responseOrException, RefPtr<AbortSignal> signal)
{

    if (signal && signal->aborted()) {
        abortPicker(signal->reason().getValue());
        return;
    }

    PickerStateGuard guard(*this);
    if (!m_currentPromise)
        return;

    if (!responseOrException) {
        const auto& errorData = responseOrException.error();
        m_currentPromise->reject(errorData.toException());
        m_currentPromise.reset();
        return;
    }

    auto& responseData = responseOrException.value();
    if (responseData.responseData.isEmpty()) {
        m_currentPromise->reject(ExceptionCode::AbortError, "User aborted the operation."_s);
        m_currentPromise.reset();
        return;
    }

    finalizeDigitalCredential(responseData);
}

bool CredentialRequestCoordinator::parseDigitalCredentialsResponseData(Document& document, const String& responseData, JSC::JSObject*& outObject) const
{

    auto* globalObject = document.globalObject();
    if (!globalObject) {
        LOG(DigitalCredentials, "No JavaScript global object available for parseDigitalCredentialsResponseData.");
        return false;
    }

    JSC::VM& vm = globalObject->vm();
    auto scope = DECLARE_CATCH_SCOPE(vm);

    JSC::JSONParse(globalObject, responseData);
    if (scope.exception()) {
        LOG(DigitalCredentials, "Failed to parse response JSON data");
        scope.clearException();
        return false;
    }

    auto* object = constructEmptyObject(globalObject);
    object->putDirect(vm,
        JSC::Identifier::fromString(vm, "response"_s),
        JSC::jsString(vm, responseData));

    outObject = object;
    return true;
}

void CredentialRequestCoordinator::finalizeDigitalCredential(const DigitalCredentialsResponseData& responseData)
{
    PickerStateGuard guard(*this);

    if (!m_currentPromise) {
        LOG(DigitalCredentials, "No current promise in coordinator.");
        return;
    }

    if (!m_page) {
        m_currentPromise->reject(ExceptionCode::InvalidStateError, "Page is gone."_s);
        m_currentPromise.reset();
            return;
        }

    auto document = m_page->localTopDocument();
    if (!document) {
        m_currentPromise->reject(ExceptionCode::InvalidStateError, "No Document."_s);
        m_currentPromise.reset();
        return;
    }

    JSC::JSObject* parsedObject = nullptr;
    if (!parseDigitalCredentialsResponseData(*document, responseData.responseData, parsedObject)) {
        m_currentPromise->reject(Exception {
            ExceptionCode::TypeError,
            "Error parsing response as JSON from wallet."_s });
        m_currentPromise.reset();
                return;
            }

    Ref credential = DigitalCredential::create(
        { parsedObject->vm(), parsedObject },
        responseData.protocol);
    m_currentPromise->resolve(WTFMove(credential.ptr()));
    m_currentPromise.reset();
}

void CredentialRequestCoordinator::abortPicker(JSC::JSValue reason)
{
    if (m_state != PickerState::Presenting) {
        LOG(DigitalCredentials, "Cannot abort the credentials picker when it is not presenting.");
        return;
    }

    setState(PickerState::Aborting);

    if (m_currentPromise) {
        m_currentPromise->rejectType<IDLAny>(reason);
        m_currentPromise.reset();
    }

    m_client->dismissDigitalCredentialsPicker([this](bool success) {
        if (!success)
            LOG(DigitalCredentials, "Failed to dismiss the credentials picker.");

        setState(PickerState::Idle);
    });
}

CredentialRequestCoordinator::~CredentialRequestCoordinator()
{
    if (m_currentPromise) {
        m_currentPromise->reject(ExceptionCode::InvalidStateError);
        m_currentPromise.reset();
    }
}

} // namespace WebCore

#endif // ENABLE(WEB_AUTHN)

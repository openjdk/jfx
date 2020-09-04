/*
 * Copyright (C) 2017 Apple Inc. All rights reserved.
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
#include "SWServerWorker.h"

#if ENABLE(SERVICE_WORKER)

#include "SWServer.h"
#include "SWServerRegistration.h"
#include "SWServerToContextConnection.h"
#include <wtf/CompletionHandler.h>
#include <wtf/NeverDestroyed.h>

namespace WebCore {

HashMap<ServiceWorkerIdentifier, SWServerWorker*>& SWServerWorker::allWorkers()
{
    static NeverDestroyed<HashMap<ServiceWorkerIdentifier, SWServerWorker*>> workers;
    return workers;
}

SWServerWorker* SWServerWorker::existingWorkerForIdentifier(ServiceWorkerIdentifier identifier)
{
    return allWorkers().get(identifier);
}

// FIXME: Use r-value references for script and contentSecurityPolicy
SWServerWorker::SWServerWorker(SWServer& server, SWServerRegistration& registration, const URL& scriptURL, const String& script, const ContentSecurityPolicyResponseHeaders& contentSecurityPolicy, String&& referrerPolicy, WorkerType type, ServiceWorkerIdentifier identifier, HashMap<URL, ServiceWorkerContextData::ImportedScript>&& scriptResourceMap)
    : m_server(makeWeakPtr(server))
    , m_registrationKey(registration.key())
    , m_registration(makeWeakPtr(registration))
    , m_data { identifier, scriptURL, ServiceWorkerState::Redundant, type, registration.identifier() }
    , m_script(script)
    , m_contentSecurityPolicy(contentSecurityPolicy)
    , m_referrerPolicy(WTFMove(referrerPolicy))
    , m_registrableDomain(m_data.scriptURL)
    , m_scriptResourceMap(WTFMove(scriptResourceMap))
{
    m_data.scriptURL.removeFragmentIdentifier();

    auto result = allWorkers().add(identifier, this);
    ASSERT_UNUSED(result, result.isNewEntry);

    ASSERT(m_server->getRegistration(m_registrationKey) == &registration);
}

SWServerWorker::~SWServerWorker()
{
    ASSERT(m_whenActivatedHandlers.isEmpty());
    callWhenActivatedHandler(false);

    auto taken = allWorkers().take(identifier());
    ASSERT_UNUSED(taken, taken == this);
}

ServiceWorkerContextData SWServerWorker::contextData() const
{
    ASSERT(m_registration);

    return { WTF::nullopt, m_registration->data(), m_data.identifier, m_script, m_contentSecurityPolicy, m_referrerPolicy, m_data.scriptURL, m_data.type, false, m_scriptResourceMap };
}

void SWServerWorker::terminate()
{
    if (isRunning())
        m_server->terminateWorker(*this);
}

const ClientOrigin& SWServerWorker::origin() const
{
    if (!m_origin)
        m_origin = ClientOrigin { m_registrationKey.topOrigin(), SecurityOriginData::fromURL(m_data.scriptURL) };

    return *m_origin;
}

SWServerToContextConnection* SWServerWorker::contextConnection()
{
    return m_server ? m_server->contextConnectionForRegistrableDomain(registrableDomain()) : nullptr;
}

void SWServerWorker::scriptContextFailedToStart(const Optional<ServiceWorkerJobDataIdentifier>& jobDataIdentifier, const String& message)
{
    ASSERT(m_server);
    if (m_server)
        m_server->scriptContextFailedToStart(jobDataIdentifier, *this, message);
}

void SWServerWorker::scriptContextStarted(const Optional<ServiceWorkerJobDataIdentifier>& jobDataIdentifier, bool doesHandleFetch)
{
    m_shouldSkipHandleFetch = !doesHandleFetch;
    ASSERT(m_server);
    if (m_server)
        m_server->scriptContextStarted(jobDataIdentifier, *this);
}

void SWServerWorker::didFinishInstall(const Optional<ServiceWorkerJobDataIdentifier>& jobDataIdentifier, bool wasSuccessful)
{
    auto state = this->state();
    if (state == ServiceWorkerState::Redundant)
        return;

    ASSERT(m_server);
    RELEASE_ASSERT(state == ServiceWorkerState::Installing);
    if (m_server)
        m_server->didFinishInstall(jobDataIdentifier, *this, wasSuccessful);
}

void SWServerWorker::didFinishActivation()
{
    auto state = this->state();
    if (state == ServiceWorkerState::Redundant)
        return;

    ASSERT(m_server);
    RELEASE_ASSERT(state == ServiceWorkerState::Activating);
    if (m_server)
        m_server->didFinishActivation(*this);
}

void SWServerWorker::contextTerminated()
{
    ASSERT(m_server);
    if (m_server)
        m_server->workerContextTerminated(*this);
}

Optional<ServiceWorkerClientData> SWServerWorker::findClientByIdentifier(const ServiceWorkerClientIdentifier& clientId) const
{
    ASSERT(m_server);
    if (!m_server)
        return { };
    return m_server->serviceWorkerClientWithOriginByID(origin(), clientId);
}

void SWServerWorker::matchAll(const ServiceWorkerClientQueryOptions& options, ServiceWorkerClientsMatchAllCallback&& callback)
{
    ASSERT(m_server);
    if (!m_server)
        return callback({ });
    return m_server->matchAll(*this, options, WTFMove(callback));
}

String SWServerWorker::userAgent() const
{
    ASSERT(m_server);
    if (!m_server)
        return { };
    return m_server->serviceWorkerClientUserAgent(origin());
}

void SWServerWorker::claim()
{
    ASSERT(m_server);
    if (m_server)
        m_server->claim(*this);
}

void SWServerWorker::setScriptResource(URL&& url, ServiceWorkerContextData::ImportedScript&& script)
{
    m_scriptResourceMap.set(WTFMove(url), WTFMove(script));
}

void SWServerWorker::skipWaiting()
{
    m_isSkipWaitingFlagSet = true;

    ASSERT(m_registration || isTerminating());
    if (m_registration)
        m_registration->tryActivate();
}

void SWServerWorker::setHasPendingEvents(bool hasPendingEvents)
{
    if (m_hasPendingEvents == hasPendingEvents)
        return;

    m_hasPendingEvents = hasPendingEvents;
    if (m_hasPendingEvents)
        return;

    // Do tryClear/tryActivate, as per https://w3c.github.io/ServiceWorker/#wait-until-method.
    if (!m_registration)
        return;

    if (m_registration->isUnregistered() && m_registration->tryClear())
        return;
    m_registration->tryActivate();
}

void SWServerWorker::whenActivated(CompletionHandler<void(bool)>&& handler)
{
    if (state() == ServiceWorkerState::Activated) {
        handler(true);
        return;
    }
    ASSERT(state() == ServiceWorkerState::Activating);
    m_whenActivatedHandlers.append(WTFMove(handler));
}

void SWServerWorker::setState(ServiceWorkerState state)
{
    if (state == ServiceWorkerState::Redundant)
        terminate();

    m_data.state = state;

    ASSERT(m_registration || state == ServiceWorkerState::Redundant);
    if (m_registration) {
        m_registration->forEachConnection([&](auto& connection) {
            connection.updateWorkerStateInClient(this->identifier(), state);
        });
    }

    if (state == ServiceWorkerState::Activated || state == ServiceWorkerState::Redundant)
        callWhenActivatedHandler(state == ServiceWorkerState::Activated);
}

void SWServerWorker::callWhenActivatedHandler(bool success)
{
    auto whenActivatedHandlers = WTFMove(m_whenActivatedHandlers);
    for (auto& handler : whenActivatedHandlers)
        handler(success);
}

void SWServerWorker::setState(State state)
{
    ASSERT(state != State::Running || m_registration);
    m_state = state;

    switch (state) {
    case State::Running:
        m_shouldSkipHandleFetch = false;
        break;
    case State::Terminating:
    case State::NotRunning:
        callWhenActivatedHandler(false);
        break;
    }
}

SWServerRegistration* SWServerWorker::registration() const
{
    return m_registration.get();
}

void SWServerWorker::didFailHeartBeatCheck()
{
    if (m_server && isRunning())
        m_server->terminateWorker(*this);
}

} // namespace WebCore

#endif // ENABLE(SERVICE_WORKER)

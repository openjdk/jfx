/*
 * Copyright (C) 2016 Apple Inc. All rights reserved.
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
#include "MockCDMFactory.h"

#if ENABLE(ENCRYPTED_MEDIA)

#include "InitDataRegistry.h"
#include "SharedBuffer.h"
#include <JavaScriptCore/ArrayBuffer.h>
#include <wtf/Algorithms.h>
#include <wtf/NeverDestroyed.h>
#include <wtf/UUID.h>
#include <wtf/text/StringHash.h>
#include <wtf/text/StringView.h>

namespace WebCore {

MockCDMFactory::MockCDMFactory()
    : m_supportedSessionTypes({ MediaKeySessionType::Temporary, MediaKeySessionType::PersistentUsageRecord, MediaKeySessionType::PersistentLicense })
    , m_supportedEncryptionSchemes({ MediaKeyEncryptionScheme::cenc })
{
    CDMFactory::registerFactory(*this);
}

MockCDMFactory::~MockCDMFactory()
{
    unregister();
}

void MockCDMFactory::unregister()
{
    if (m_registered) {
        CDMFactory::unregisterFactory(*this);
        m_registered = false;
    }
}

bool MockCDMFactory::supportsKeySystem(const String& keySystem)
{
    return equalLettersIgnoringASCIICase(keySystem, "org.webkit.mock"_s);
}

bool MockCDMFactory::hasSessionWithID(const String& id)
{
    if (id.isEmpty())
        return false;

    return m_sessions.contains(id);
}

void MockCDMFactory::removeSessionWithID(const String& id)
{
    if (!id.isEmpty())
    m_sessions.remove(id);
}

void MockCDMFactory::addKeysToSessionWithID(const String& id, Vector<Ref<SharedBuffer>>&& keys)
{
    auto addResult = m_sessions.add(id, WTFMove(keys));
    if (addResult.isNewEntry)
        return;

    auto& value = addResult.iterator->value;
    for (auto& key : keys)
        value.append(WTFMove(key));
}

Vector<Ref<SharedBuffer>> MockCDMFactory::removeKeysFromSessionWithID(const String& id)
{
    auto it = m_sessions.find(id);
    if (it == m_sessions.end())
        return { };

    return WTFMove(it->value);
}

const Vector<Ref<SharedBuffer>>* MockCDMFactory::keysForSessionWithID(const String& id) const
{
    auto it = m_sessions.find(id);
    if (it == m_sessions.end())
        return nullptr;
    return &it->value;
}

void MockCDMFactory::setSupportedDataTypes(Vector<String>&& types)
{
    m_supportedDataTypes.clear();
    for (auto& type : types)
        m_supportedDataTypes.append(type);
}

std::unique_ptr<CDMPrivate> MockCDMFactory::createCDM(const String&, const CDMPrivateClient&)
{
    return makeUnique<MockCDM>(*this);
}

MockCDM::MockCDM(WeakPtr<MockCDMFactory> factory)
    : m_factory(WTFMove(factory))
{
}

Vector<AtomString> MockCDM::supportedInitDataTypes() const
{
    if (m_factory)
        return m_factory->supportedDataTypes();
    return { };
}

Vector<AtomString> MockCDM::supportedRobustnesses() const
{
    if (m_factory)
        return m_factory->supportedRobustness();
    return { };
}

bool MockCDM::supportsConfiguration(const MediaKeySystemConfiguration& configuration) const
{
    auto capabilityHasSupportedEncryptionScheme = [&] (auto& capability) {
        if (capability.encryptionScheme)
            return m_factory->supportedEncryptionSchemes().contains(capability.encryptionScheme.value());
        return true;
    };

    if (!configuration.audioCapabilities.isEmpty() && !anyOf(configuration.audioCapabilities, capabilityHasSupportedEncryptionScheme))
        return false;

    if (!configuration.videoCapabilities.isEmpty() && !anyOf(configuration.videoCapabilities, capabilityHasSupportedEncryptionScheme))
        return false;

    return true;

}

bool MockCDM::supportsConfigurationWithRestrictions(const MediaKeySystemConfiguration&, const MediaKeysRestrictions&) const
{
    // NOTE: Implement;
    return true;
}

bool MockCDM::supportsSessionTypeWithConfiguration(const MediaKeySessionType& sessionType, const MediaKeySystemConfiguration&) const
{
    if (!m_factory || !m_factory->supportedSessionTypes().contains(sessionType))
        return false;

    // NOTE: Implement configuration checking;
    return true;
}

MediaKeysRequirement MockCDM::distinctiveIdentifiersRequirement(const MediaKeySystemConfiguration&, const MediaKeysRestrictions&) const
{
    if (m_factory)
        return m_factory->distinctiveIdentifiersRequirement();
    return MediaKeysRequirement::Optional;
}

MediaKeysRequirement MockCDM::persistentStateRequirement(const MediaKeySystemConfiguration&, const MediaKeysRestrictions&) const
{
    if (m_factory)
        return m_factory->persistentStateRequirement();
    return MediaKeysRequirement::Optional;
}

bool MockCDM::distinctiveIdentifiersAreUniquePerOriginAndClearable(const MediaKeySystemConfiguration&) const
{
    // NOTE: Implement;
    return true;
}

RefPtr<CDMInstance> MockCDM::createInstance()
{
    if (m_factory && !m_factory->canCreateInstances())
        return nullptr;
    return adoptRef(new MockCDMInstance(*this));
}

void MockCDM::loadAndInitialize()
{
    // No-op.
}

bool MockCDM::supportsServerCertificates() const
{
    return m_factory && m_factory->supportsServerCertificates();
}

bool MockCDM::supportsSessions() const
{
    return m_factory && m_factory->supportsSessions();
}

bool MockCDM::supportsInitData(const AtomString& initDataType, const SharedBuffer& initData) const
{
    if (!supportedInitDataTypes().contains(initDataType))
        return false;

    UNUSED_PARAM(initData);
    return true;
}

RefPtr<SharedBuffer> MockCDM::sanitizeResponse(const SharedBuffer& response) const
{
    auto contiguousResponse = response.makeContiguous();
    if (!charactersAreAllASCII(contiguousResponse->span()))
        return nullptr;

    for (auto word : StringView(contiguousResponse->span()).split(' ')) {
        if (word == "valid-response"_s)
            return contiguousResponse;
    }

        return nullptr;
}

std::optional<String> MockCDM::sanitizeSessionId(const String& sessionId) const
{
    if (equalLettersIgnoringASCIICase(sessionId, "valid-loaded-session"_s))
        return sessionId;
    return std::nullopt;
}

MockCDMInstance::MockCDMInstance(WeakPtr<MockCDM> cdm)
    : m_cdm(cdm)
{
}

void MockCDMInstance::initializeWithConfiguration(const MediaKeySystemConfiguration& configuration, AllowDistinctiveIdentifiers distinctiveIdentifiers, AllowPersistentState persistentState, SuccessCallback&& callback)
{
    auto initialize = [&] {
        if (!m_cdm || !m_cdm->supportsConfiguration(configuration))
            return Failed;

        MockCDMFactory* factory = m_cdm ? m_cdm->factory() : nullptr;
        if (!factory)
            return Failed;

        bool distinctiveIdentifiersAllowed = (distinctiveIdentifiers == AllowDistinctiveIdentifiers::Yes);

        if (m_distinctiveIdentifiersAllowed != distinctiveIdentifiersAllowed) {
            if (!distinctiveIdentifiersAllowed && factory->distinctiveIdentifiersRequirement() == MediaKeysRequirement::Required)
                return Failed;

            m_distinctiveIdentifiersAllowed = distinctiveIdentifiersAllowed;
        }

        bool persistentStateAllowed = (persistentState == AllowPersistentState::Yes);

        if (m_persistentStateAllowed != persistentStateAllowed) {
            if (!persistentStateAllowed && factory->persistentStateRequirement() == MediaKeysRequirement::Required)
                return Failed;

            m_persistentStateAllowed = persistentStateAllowed;
        }
        return Succeeded;
    };

    callback(initialize());
}

void MockCDMInstance::setServerCertificate(Ref<SharedBuffer>&& certificate, SuccessCallback&& callback)
{
    Ref contiguousData = certificate->makeContiguous();
    callback(equalLettersIgnoringASCIICase(StringView { contiguousData->span() }, "valid"_s) ? Succeeded : Failed);
}

void MockCDMInstance::setStorageDirectory(const String&)
{
}

const String& MockCDMInstance::keySystem() const
{
    static const NeverDestroyed<String> s_keySystem = MAKE_STATIC_STRING_IMPL("org.webkit.mock");

    return s_keySystem;
}

RefPtr<CDMInstanceSession> MockCDMInstance::createSession()
{
    return adoptRef(new MockCDMInstanceSession(*this));
}

MockCDMInstanceSession::MockCDMInstanceSession(WeakPtr<MockCDMInstance>&& instance)
    : m_instance(WTFMove(instance))
{
}

void MockCDMInstanceSession::requestLicense(LicenseType licenseType, KeyGroupingStrategy, const AtomString& initDataType, Ref<SharedBuffer>&& initData, LicenseCallback&& callback)
{
    MockCDMFactory* factory = m_instance ? m_instance->factory() : nullptr;
    if (!factory) {
        callback(SharedBuffer::create(), emptyAtom(), false, SuccessValue::Failed);
        return;
    }

    if (!factory->supportedSessionTypes().contains(licenseType) || !factory->supportedDataTypes().contains(initDataType)) {
        callback(SharedBuffer::create(), emptyString(), false, SuccessValue::Failed);
        return;
    }

    auto keyIDs = InitDataRegistry::shared().extractKeyIDs(initDataType, initData);
    if (!keyIDs || keyIDs.value().isEmpty()) {
        callback(SharedBuffer::create(), emptyString(), false, SuccessValue::Failed);
        return;
    }

    String sessionID = createVersion4UUIDString();
    factory->addKeysToSessionWithID(sessionID, WTFMove(keyIDs.value()));

    CString license { "license" };
    callback(SharedBuffer::create(license.span()), sessionID, false, SuccessValue::Succeeded);
}

void MockCDMInstanceSession::updateLicense(const String& sessionID, LicenseType, Ref<SharedBuffer>&& response, LicenseUpdateCallback&& callback)
{
    MockCDMFactory* factory = m_instance ? m_instance->factory() : nullptr;
    if (!factory) {
        callback(false, std::nullopt, std::nullopt, std::nullopt, SuccessValue::Failed);
        return;
    }

    Vector<String> responseVector = String(response->makeContiguous()->span()).split(' ');

    if (responseVector.contains(String("invalid-format"_s))) {
        callback(false, std::nullopt, std::nullopt, std::nullopt, SuccessValue::Failed);
        return;
    }

    std::optional<KeyStatusVector> changedKeys;
    if (responseVector.contains(String("keys-changed"_s))) {
        const auto* keys = factory->keysForSessionWithID(sessionID);
        if (keys) {
            changedKeys = keys->map([](auto& key) {
                return std::pair { key.copyRef(), KeyStatus::Usable };
            });
        }
    }

    // FIXME: Session closure, expiration and message handling should be implemented
    // once the relevant algorithms are supported.

    callback(false, WTFMove(changedKeys), std::nullopt, std::nullopt, SuccessValue::Succeeded);
}

void MockCDMInstanceSession::loadSession(LicenseType, const String&, const String&, LoadSessionCallback&& callback)
{
    MockCDMFactory* factory = m_instance ? m_instance->factory() : nullptr;
    if (!factory) {
        callback(std::nullopt, std::nullopt, std::nullopt, SuccessValue::Failed, SessionLoadFailure::Other);
        return;
    }

    // FIXME: Key status and expiration handling should be implemented once the relevant algorithms are supported.

    CString messageData { "session loaded" };
    Message message { MessageType::LicenseRenewal, SharedBuffer::create(messageData.span()) };

    callback(std::nullopt, std::nullopt, WTFMove(message), SuccessValue::Succeeded, SessionLoadFailure::None);
}

void MockCDMInstanceSession::closeSession(const String& sessionID, CloseSessionCallback&& callback)
{
    MockCDMFactory* factory = m_instance ? m_instance->factory() : nullptr;
    if (!factory) {
        callback();
        return;
    }

    factory->removeSessionWithID(sessionID);
    callback();
}

void MockCDMInstanceSession::removeSessionData(const String& id, LicenseType, RemoveSessionDataCallback&& callback)
{
    MockCDMFactory* factory = m_instance ? m_instance->factory() : nullptr;
    if (!factory) {
        callback({ }, nullptr, SuccessValue::Failed);
        return;
    }

    auto keys = factory->removeKeysFromSessionWithID(id);
    auto keyStatusVector = WTF::map(WTFMove(keys), [](Ref<SharedBuffer>&& key) {
        return std::pair { WTFMove(key), KeyStatus::Released };
    });

    CString message { "remove-message" };
    callback(WTFMove(keyStatusVector), SharedBuffer::create(message.span()), SuccessValue::Succeeded);
}

void MockCDMInstanceSession::storeRecordOfKeyUsage(const String&)
{
    // FIXME: This should be implemented along with the support for persistent-usage-record sessions.
}

}

#endif

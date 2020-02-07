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

#pragma once

#if ENABLE(SERVICE_WORKER)

#include "ClientOrigin.h"
#include "ContentSecurityPolicyResponseHeaders.h"
#include "RegistrableDomain.h"
#include "ServiceWorkerClientData.h"
#include "ServiceWorkerContextData.h"
#include "ServiceWorkerData.h"
#include "ServiceWorkerIdentifier.h"
#include "ServiceWorkerRegistrationKey.h"
#include "ServiceWorkerTypes.h"
#include <wtf/RefCounted.h>
#include <wtf/WeakPtr.h>

namespace WebCore {

class SWServer;
class SWServerRegistration;
class SWServerToContextConnection;
struct ServiceWorkerClientIdentifier;
struct ServiceWorkerClientQueryOptions;
struct ServiceWorkerContextData;
struct ServiceWorkerJobDataIdentifier;
enum class WorkerType;

class SWServerWorker : public RefCounted<SWServerWorker> {
public:
    template <typename... Args> static Ref<SWServerWorker> create(Args&&... args)
    {
        return adoptRef(*new SWServerWorker(std::forward<Args>(args)...));
    }

    SWServerWorker(const SWServerWorker&) = delete;
    WEBCORE_EXPORT ~SWServerWorker();

    void terminate();

    WEBCORE_EXPORT void whenActivated(WTF::Function<void(bool)>&&);

    enum class State {
        Running,
        Terminating,
        NotRunning,
    };
    bool isRunning() const { return m_state == State::Running; }
    bool isTerminating() const { return m_state == State::Terminating; }
    void setState(State);

    SWServer* server() { return m_server.get(); }
    const ServiceWorkerRegistrationKey& registrationKey() const { return m_registrationKey; }
    const URL& scriptURL() const { return m_data.scriptURL; }
    const String& script() const { return m_script; }
    WorkerType type() const { return m_data.type; }

    ServiceWorkerIdentifier identifier() const { return m_data.identifier; }

    ServiceWorkerState state() const { return m_data.state; }
    void setState(ServiceWorkerState);

    bool hasPendingEvents() const { return m_hasPendingEvents; }
    void setHasPendingEvents(bool);

    void scriptContextFailedToStart(const Optional<ServiceWorkerJobDataIdentifier>&, const String& message);
    void scriptContextStarted(const Optional<ServiceWorkerJobDataIdentifier>&);
    void didFinishInstall(const Optional<ServiceWorkerJobDataIdentifier>&, bool wasSuccessful);
    void didFinishActivation();
    void contextTerminated();
    WEBCORE_EXPORT Optional<ServiceWorkerClientData> findClientByIdentifier(const ServiceWorkerClientIdentifier&) const;
    void matchAll(const ServiceWorkerClientQueryOptions&, ServiceWorkerClientsMatchAllCallback&&);
    void claim();
    void setScriptResource(URL&&, ServiceWorkerContextData::ImportedScript&&);

    void skipWaiting();
    bool isSkipWaitingFlagSet() const { return m_isSkipWaitingFlagSet; }

    WEBCORE_EXPORT static SWServerWorker* existingWorkerForIdentifier(ServiceWorkerIdentifier);
    static HashMap<ServiceWorkerIdentifier, SWServerWorker*>& allWorkers();

    const ServiceWorkerData& data() const { return m_data; }
    ServiceWorkerContextData contextData() const;

    const ClientOrigin& origin() const;
    const RegistrableDomain& registrableDomain() const { return m_registrableDomain; }

    WEBCORE_EXPORT SWServerToContextConnection* contextConnection();
    String userAgent() const;

private:
    SWServerWorker(SWServer&, SWServerRegistration&, const URL&, const String& script, const ContentSecurityPolicyResponseHeaders&, String&& referrerPolicy, WorkerType, ServiceWorkerIdentifier, HashMap<URL, ServiceWorkerContextData::ImportedScript>&&);

    void callWhenActivatedHandler(bool success);

    WeakPtr<SWServer> m_server;
    ServiceWorkerRegistrationKey m_registrationKey;
    ServiceWorkerData m_data;
    String m_script;
    ContentSecurityPolicyResponseHeaders m_contentSecurityPolicy;
    String m_referrerPolicy;
    bool m_hasPendingEvents { false };
    State m_state { State::NotRunning };
    mutable Optional<ClientOrigin> m_origin;
    RegistrableDomain m_registrableDomain;
    bool m_isSkipWaitingFlagSet { false };
    Vector<Function<void(bool)>> m_whenActivatedHandlers;
    HashMap<URL, ServiceWorkerContextData::ImportedScript> m_scriptResourceMap;
};

} // namespace WebCore

#endif // ENABLE(SERVICE_WORKER)

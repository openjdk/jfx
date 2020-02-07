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

#include "JSDOMPromiseDeferred.h"
#include "ResourceResponse.h"
#include "ServiceWorkerJobClient.h"
#include "ServiceWorkerJobData.h"
#include "ServiceWorkerTypes.h"
#include "WorkerScriptLoader.h"
#include "WorkerScriptLoaderClient.h"
#include <wtf/RefPtr.h>
#include <wtf/RunLoop.h>
#include <wtf/ThreadSafeRefCounted.h>
#include <wtf/Threading.h>

namespace WebCore {

class DeferredPromise;
class Exception;
class ScriptExecutionContext;
enum class ServiceWorkerJobType;
struct ServiceWorkerRegistrationData;

class ServiceWorkerJob : public WorkerScriptLoaderClient {
    WTF_MAKE_FAST_ALLOCATED;
public:
    ServiceWorkerJob(ServiceWorkerJobClient&, RefPtr<DeferredPromise>&&, ServiceWorkerJobData&&);
    WEBCORE_EXPORT ~ServiceWorkerJob();

    void failedWithException(const Exception&);
    void resolvedWithRegistration(ServiceWorkerRegistrationData&&, ShouldNotifyWhenResolved);
    void resolvedWithUnregistrationResult(bool);
    void startScriptFetch(FetchOptions::Cache);

    using Identifier = ServiceWorkerJobIdentifier;
    Identifier identifier() const { return m_jobData.identifier().jobIdentifier; }

    const ServiceWorkerJobData& data() const { return m_jobData; }
    bool hasPromise() const { return !!m_promise; }
    RefPtr<DeferredPromise> takePromise() { return WTFMove(m_promise); }

    void fetchScriptWithContext(ScriptExecutionContext&, FetchOptions::Cache);

    const DocumentOrWorkerIdentifier& contextIdentifier() { return m_contextIdentifier; }

    bool cancelPendingLoad();

private:
    // WorkerScriptLoaderClient
    void didReceiveResponse(unsigned long identifier, const ResourceResponse&) final;
    void notifyFinished() final;

    ServiceWorkerJobClient& m_client;
    ServiceWorkerJobData m_jobData;
    RefPtr<DeferredPromise> m_promise;

    bool m_completed { false };

    DocumentOrWorkerIdentifier m_contextIdentifier;
    RefPtr<WorkerScriptLoader> m_scriptLoader;

#if !ASSERT_DISABLED
    Ref<Thread> m_creationThread { Thread::current() };
#endif
};

} // namespace WebCore

#endif // ENABLE(SERVICE_WORKER)


/*
 * Copyright (C) 2024 Apple Inc. All rights reserved.
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

#if ENABLE(CONTENT_EXTENSIONS)

#include "ContentExtensionsBackend.h"
#include <wtf/CompletionHandler.h>
#include <wtf/Lock.h>
#include <wtf/NeverDestroyed.h>
#include <wtf/OptionSet.h>
#include <wtf/WorkQueue.h>

namespace WebCore {

class LocalFrame;
class ResourceMonitor;

enum class ResourceMonitorEligibility : uint8_t { Unsure, NotEligible, Eligible };

class ResourceMonitorChecker final {
    friend MainThreadNeverDestroyed<ResourceMonitorChecker>;
public:
    using Eligibility = ResourceMonitorEligibility;

    WEBCORE_EXPORT static ResourceMonitorChecker& singleton();

    ~ResourceMonitorChecker();

    void registerResourceMonitor(ResourceMonitor&);
    void unregisterResourceMonitor(ResourceMonitor&);

    void checkEligibility(ContentExtensions::ResourceLoadInfo&&, CompletionHandler<void(Eligibility)>&&);

    WEBCORE_EXPORT void setContentRuleList(ContentExtensions::ContentExtensionsBackend&&);
    WEBCORE_EXPORT void setNetworkUsageThreshold(size_t threshold, double randomness = defaultNetworkUsageThresholdRandomness);

    WEBCORE_EXPORT size_t networkUsageThreshold() const;
    WEBCORE_EXPORT size_t networkUsageThresholdWithNoise() const;

    static constexpr Seconds ruleListPreparationTimeout = 10_s;
    static constexpr auto defaultEligibility = ResourceMonitorEligibility::NotEligible;
    WEBCORE_EXPORT static constexpr size_t defaultNetworkUsageThreshold = 4 * MB;
    WEBCORE_EXPORT static constexpr double defaultNetworkUsageThresholdRandomness = 0.0325;

private:
    ResourceMonitorChecker();

    Eligibility checkEligibility(const ContentExtensions::ResourceLoadInfo&);
    void finishPendingQueries(Function<Eligibility(const ContentExtensions::ResourceLoadInfo&)> checker);

    Ref<WorkQueue> protectedWorkQueue() { return m_workQueue; }

    Ref<WorkQueue> m_workQueue;
    std::unique_ptr<ContentExtensions::ContentExtensionsBackend> m_ruleList;
    Vector<std::pair<ContentExtensions::ResourceLoadInfo, CompletionHandler<void(Eligibility)>>> m_pendingQueries;
    WeakHashSet<ResourceMonitor> m_resourceMonitors;
    size_t m_networkUsageThreshold { defaultNetworkUsageThreshold };
    double m_networkUsageThresholdRandomness { defaultNetworkUsageThresholdRandomness };
    bool m_ruleListIsPreparing { true };
};

} // namespace WebCore

#endif

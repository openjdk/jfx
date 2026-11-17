/*
 * Copyright (C) 2025 Igalia S.L. All rights reserved.
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
#include "WebXRHitTestResult.h"

#include <wtf/TZoneMallocInlines.h>

#if ENABLE(WEBXR_HIT_TEST)
#include "WebXRInputSpace.h"
#include "WebXRPose.h"
#include "WebXRRigidTransform.h"
#include "WebXRSpace.h"

namespace WebCore {

WTF_MAKE_TZONE_ALLOCATED_IMPL(WebXRHitTestResult);

Ref<WebXRHitTestResult> WebXRHitTestResult::create(WebXRFrame& frame, const PlatformXR::FrameData::HitTestResult& result)
{
    return adoptRef(*new WebXRHitTestResult(frame, result));
}

WebXRHitTestResult::WebXRHitTestResult(WebXRFrame& frame, const PlatformXR::FrameData::HitTestResult& result)
    : m_frame(frame)
    , m_result(result)
{
}

WebXRHitTestResult::~WebXRHitTestResult() = default;

class WebXRHitTestResultSpace : public WebXRSpace {
public:
    WebXRHitTestResultSpace(Document& document, WebXRSession& session, const PlatformXR::FrameData::Pose& pose)
        : WebXRSpace(document, WebXRRigidTransform::create())
        , m_session(session)
        , m_pose(pose)
    {
    }

private:
    WebXRSession* session() const final { return m_session.get(); }
    std::optional<TransformationMatrix> nativeOrigin() const final { return WebXRFrame::matrixFromPose(m_pose); }
    void refEventTarget() final { }
    void derefEventTarget() final { }
    void ref() const final { }
    void deref() const final { }

    WeakPtr<WebXRSession> m_session;
    PlatformXR::FrameData::Pose m_pose;
};

// https://immersive-web.github.io/hit-test/#dom-xrhittestresult-getpose
ExceptionOr<RefPtr<WebXRPose>> WebXRHitTestResult::getPose(Document& document, const WebXRSpace& baseSpace)
{
    WebXRHitTestResultSpace space { document, m_frame->session(), m_result.pose };
    auto exceptionOrPose = m_frame->populatePose(document, space, baseSpace);
    if (exceptionOrPose.hasException())
        return exceptionOrPose.releaseException();
    auto populatedPose = exceptionOrPose.releaseReturnValue();
    if (!populatedPose)
        return nullptr;
    return RefPtr<WebXRPose>(WebXRPose::create(WebXRRigidTransform::create(populatedPose->transform), populatedPose->emulatedPosition));
}

} // namespace WebCore

#endif // ENABLE(WEBXR_HIT_TEST)

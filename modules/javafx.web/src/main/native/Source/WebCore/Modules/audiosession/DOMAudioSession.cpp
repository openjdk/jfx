/*
 * Copyright (C) 2022-2024 Apple Inc. All rights reserved.
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
#include "DOMAudioSession.h"

#if ENABLE(DOM_AUDIO_SESSION)

#include "AudioSession.h"
#include "Document.h"
#include "EventNames.h"
#include "Page.h"
#include "PermissionsPolicy.h"
#include "PlatformMediaSessionManager.h"
#include <wtf/TZoneMallocInlines.h>

namespace WebCore {

WTF_MAKE_TZONE_OR_ISO_ALLOCATED_IMPL(DOMAudioSession);

static inline AudioSessionCategory fromDOMAudioSessionType(DOMAudioSession::Type type)
{
    switch (type) {
    case DOMAudioSession::Type::Auto:
        return AudioSessionCategory::None;
    case DOMAudioSession::Type::Playback:
        return AudioSessionCategory::MediaPlayback;
    case DOMAudioSession::Type::Transient:
        return AudioSessionCategory::AmbientSound;
    case DOMAudioSession::Type::TransientSolo:
        return AudioSessionCategory::SoloAmbientSound;
    case DOMAudioSession::Type::Ambient:
        return AudioSessionCategory::AmbientSound;
    case DOMAudioSession::Type::PlayAndRecord:
        return AudioSessionCategory::PlayAndRecord;
        break;
    };

    ASSERT_NOT_REACHED();
    return AudioSessionCategory::None;
}

Ref<DOMAudioSession> DOMAudioSession::create(ScriptExecutionContext* context)
{
    auto audioSession = adoptRef(*new DOMAudioSession(context));
    audioSession->suspendIfNeeded();
    return audioSession;
}

DOMAudioSession::DOMAudioSession(ScriptExecutionContext* context)
    : ActiveDOMObject(context)
{
    AudioSession::protectedSharedSession()->addInterruptionObserver(*this);
}

DOMAudioSession::~DOMAudioSession()
{
    AudioSession::protectedSharedSession()->removeInterruptionObserver(*this);
}

ExceptionOr<void> DOMAudioSession::setType(Type type)
{
    RefPtr document = downcast<Document>(scriptExecutionContext());
    if (!document)
        return Exception { ExceptionCode::InvalidStateError };

    RefPtr page = document->protectedPage();
    if (!page)
        return Exception { ExceptionCode::InvalidStateError };

    if (!PermissionsPolicy::isFeatureEnabled(PermissionsPolicy::Feature::Microphone, *document, PermissionsPolicy::ShouldReportViolation::No))
        return { };

    page->setAudioSessionType(type);

    auto categoryOverride = fromDOMAudioSessionType(type);
    AudioSession::protectedSharedSession()->setCategoryOverride(categoryOverride);

    if (categoryOverride == AudioSessionCategory::None)
        PlatformMediaSessionManager::updateAudioSessionCategoryIfNecessary();

    return { };
}

DOMAudioSession::Type DOMAudioSession::type() const
{
    RefPtr document = downcast<Document>(scriptExecutionContext());
    if (document && !PermissionsPolicy::isFeatureEnabled(PermissionsPolicy::Feature::Microphone, *document, PermissionsPolicy::ShouldReportViolation::No))
        return DOMAudioSession::Type::Auto;

    if (!document)
        return DOMAudioSession::Type::Auto;

    if (RefPtr page = document->protectedPage())
        return page->audioSessionType();

    return DOMAudioSession::Type::Auto;
}

static DOMAudioSession::State computeAudioSessionState()
{
    if (AudioSession::sharedSession().isInterrupted())
        return DOMAudioSession::State::Interrupted;

    if (!AudioSession::sharedSession().isActive())
        return DOMAudioSession::State::Inactive;

    return DOMAudioSession::State::Active;
}

DOMAudioSession::State DOMAudioSession::state() const
{
    RefPtr document = downcast<Document>(scriptExecutionContext());
    if (!document || !PermissionsPolicy::isFeatureEnabled(PermissionsPolicy::Feature::Microphone, *document, PermissionsPolicy::ShouldReportViolation::No))
        return DOMAudioSession::State::Inactive;

    if (!m_state)
        m_state = computeAudioSessionState();
    return *m_state;
}

void DOMAudioSession::stop()
{
}

bool DOMAudioSession::virtualHasPendingActivity() const
{
    return hasEventListeners(eventNames().statechangeEvent);
}

void DOMAudioSession::beginAudioSessionInterruption()
{
    scheduleStateChangeEvent();
}

void DOMAudioSession::endAudioSessionInterruption(AudioSession::MayResume)
{
    scheduleStateChangeEvent();
}

void DOMAudioSession::audioSessionActiveStateChanged()
{
    scheduleStateChangeEvent();
}

void DOMAudioSession::scheduleStateChangeEvent()
{
    RefPtr document = downcast<Document>(scriptExecutionContext());
    if (document && !PermissionsPolicy::isFeatureEnabled(PermissionsPolicy::Feature::Microphone, *document, PermissionsPolicy::ShouldReportViolation::No))
        return;

    if (m_hasScheduleStateChangeEvent)
        return;

    m_hasScheduleStateChangeEvent = true;
    queueTaskKeepingObjectAlive(*this, TaskSource::MediaElement, [weakThis = WeakPtr { *this }] {
        RefPtr protectedThis = weakThis.get();
        if (!protectedThis)
            return;

        if (protectedThis->isContextStopped())
            return;

        protectedThis->m_hasScheduleStateChangeEvent = false;
        auto newState = computeAudioSessionState();

        if (protectedThis->m_state && *protectedThis->m_state == newState)
            return;

        protectedThis->m_state = newState;
        protectedThis->dispatchEvent(Event::create(eventNames().statechangeEvent, Event::CanBubble::No, Event::IsCancelable::No));
    });
}

} // namespace WebCore

#endif // ENABLE(DOM_AUDIO_SESSION)

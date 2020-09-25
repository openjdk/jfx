/*
 * Copyright (C) 2013-2019 Apple Inc. All rights reserved.
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

#ifndef PlatformMediaSessionManager_h
#define PlatformMediaSessionManager_h

#include "AudioHardwareListener.h"
#include "PlatformMediaSession.h"
#include "RemoteCommandListener.h"
#include "Timer.h"
#include <pal/system/SystemSleepListener.h>
#include <wtf/AggregateLogger.h>
#include <wtf/Vector.h>

namespace WebCore {

class Document;
class HTMLMediaElement;
class PlatformMediaSession;
class RemoteCommandListener;

class PlatformMediaSessionManager
    : private RemoteCommandListenerClient
    , private PAL::SystemSleepListener::Client
    , private AudioHardwareListener::Client
#if !RELEASE_LOG_DISABLED
    , private LoggerHelper
#endif
{
    WTF_MAKE_FAST_ALLOCATED;
public:
    WEBCORE_EXPORT static PlatformMediaSessionManager* sharedManagerIfExists();
    WEBCORE_EXPORT static PlatformMediaSessionManager& sharedManager();

    static void updateNowPlayingInfoIfNecessary();

    WEBCORE_EXPORT static void setShouldDeactivateAudioSession(bool);
    WEBCORE_EXPORT static bool shouldDeactivateAudioSession();

    virtual ~PlatformMediaSessionManager() = default;

    virtual void scheduleUpdateNowPlayingInfo() { }
    bool has(PlatformMediaSession::MediaType) const;
    int count(PlatformMediaSession::MediaType) const;
    bool activeAudioSessionRequired() const;
    bool canProduceAudio() const;

    virtual bool hasActiveNowPlayingSession() const { return false; }
    virtual String lastUpdatedNowPlayingTitle() const { return emptyString(); }
    virtual double lastUpdatedNowPlayingDuration() const { return NAN; }
    virtual double lastUpdatedNowPlayingElapsedTime() const { return NAN; }
    virtual uint64_t lastUpdatedNowPlayingInfoUniqueIdentifier() const { return 0; }
    virtual bool registeredAsNowPlayingApplication() const { return false; }
    virtual void prepareToSendUserMediaPermissionRequest() { }

    bool willIgnoreSystemInterruptions() const { return m_willIgnoreSystemInterruptions; }
    void setWillIgnoreSystemInterruptions(bool ignore) { m_willIgnoreSystemInterruptions = ignore; }

    WEBCORE_EXPORT virtual void beginInterruption(PlatformMediaSession::InterruptionType);
    WEBCORE_EXPORT void endInterruption(PlatformMediaSession::EndInterruptionFlags);

    WEBCORE_EXPORT void applicationWillBecomeInactive();
    WEBCORE_EXPORT void applicationDidBecomeActive();
    WEBCORE_EXPORT void applicationWillEnterForeground(bool suspendedUnderLock);
    WEBCORE_EXPORT void applicationDidEnterBackground(bool suspendedUnderLock);
    WEBCORE_EXPORT void processWillSuspend();
    WEBCORE_EXPORT void processDidResume();

    void stopAllMediaPlaybackForDocument(const Document&);
    WEBCORE_EXPORT void stopAllMediaPlaybackForProcess();

    void suspendAllMediaPlaybackForDocument(const Document&);
    void resumeAllMediaPlaybackForDocument(const Document&);
    void suspendAllMediaBufferingForDocument(const Document&);
    void resumeAllMediaBufferingForDocument(const Document&);

    enum SessionRestrictionFlags {
        NoRestrictions = 0,
        ConcurrentPlaybackNotPermitted = 1 << 0,
        BackgroundProcessPlaybackRestricted = 1 << 1,
        BackgroundTabPlaybackRestricted = 1 << 2,
        InterruptedPlaybackNotPermitted = 1 << 3,
        InactiveProcessPlaybackRestricted = 1 << 4,
        SuspendedUnderLockPlaybackRestricted = 1 << 5,
    };
    typedef unsigned SessionRestrictions;

    WEBCORE_EXPORT void addRestriction(PlatformMediaSession::MediaType, SessionRestrictions);
    WEBCORE_EXPORT void removeRestriction(PlatformMediaSession::MediaType, SessionRestrictions);
    WEBCORE_EXPORT SessionRestrictions restrictions(PlatformMediaSession::MediaType);
    virtual void resetRestrictions();

    virtual bool sessionWillBeginPlayback(PlatformMediaSession&);

    virtual void sessionWillEndPlayback(PlatformMediaSession&, DelayCallingUpdateNowPlaying);
    virtual void sessionStateChanged(PlatformMediaSession&);
    virtual void sessionDidEndRemoteScrubbing(const PlatformMediaSession&) { };
    virtual void clientCharacteristicsChanged(PlatformMediaSession&) { }
    virtual void sessionCanProduceAudioChanged();

#if PLATFORM(IOS_FAMILY)
    virtual void configureWireLessTargetMonitoring() { }
#endif
    virtual bool hasWirelessTargetsAvailable() { return false; }

    void setCurrentSession(PlatformMediaSession&);
    PlatformMediaSession* currentSession() const;

    void sessionIsPlayingToWirelessPlaybackTargetChanged(PlatformMediaSession&);

    WEBCORE_EXPORT void setIsPlayingToAutomotiveHeadUnit(bool);
    bool isPlayingToAutomotiveHeadUnit() const { return m_isPlayingToAutomotiveHeadUnit; }

    void forEachMatchingSession(const Function<bool(const PlatformMediaSession&)>& predicate, const Function<void(PlatformMediaSession&)>& matchingCallback);

    bool processIsSuspended() const { return m_processIsSuspended; }

protected:
    friend class PlatformMediaSession;
    explicit PlatformMediaSessionManager();

    void addSession(PlatformMediaSession&);
    virtual void removeSession(PlatformMediaSession&);

    void forEachSession(const Function<void(PlatformMediaSession&)>&);
    void forEachDocumentSession(const Document&, const Function<void(PlatformMediaSession&)>&);
    bool anyOfSessions(const Function<bool(const PlatformMediaSession&)>&) const;

    AudioHardwareListener* audioHardwareListener() { return m_audioHardwareListener.get(); }

    bool isApplicationInBackground() const { return m_isApplicationInBackground; }
#if USE(AUDIO_SESSION)
    void maybeDeactivateAudioSession();
#endif

#if !RELEASE_LOG_DISABLED
    const Logger& logger() const final { return m_logger; }
    const void* logIdentifier() const final { return nullptr; }
    const char* logClassName() const override { return "PlatformMediaSessionManager"; }
    WTFLogChannel& logChannel() const final;
#endif

private:
    friend class Internals;

    virtual void updateSessionState() { }

    // RemoteCommandListenerClient
    WEBCORE_EXPORT void didReceiveRemoteControlCommand(PlatformMediaSession::RemoteControlCommandType, const PlatformMediaSession::RemoteCommandArgument*) override;
    WEBCORE_EXPORT bool supportsSeeking() const override;

    // AudioHardwareListenerClient
    void audioHardwareDidBecomeActive() override { }
    void audioHardwareDidBecomeInactive() override { }
    void audioOutputDeviceChanged() override;

    // PAL::SystemSleepListener
    void systemWillSleep() override;
    void systemDidWake() override;

    Vector<WeakPtr<PlatformMediaSession>> sessionsMatching(const Function<bool(const PlatformMediaSession&)>&) const;

    SessionRestrictions m_restrictions[PlatformMediaSession::MediaStreamCapturingAudio + 1];
    mutable Vector<WeakPtr<PlatformMediaSession>> m_sessions;
    std::unique_ptr<RemoteCommandListener> m_remoteCommandListener;
    std::unique_ptr<PAL::SystemSleepListener> m_systemSleepListener;
    RefPtr<AudioHardwareListener> m_audioHardwareListener;

#if ENABLE(WIRELESS_PLAYBACK_TARGET) && !PLATFORM(IOS_FAMILY)
    RefPtr<MediaPlaybackTarget> m_playbackTarget;
    bool m_canPlayToTarget { false };
#endif

    bool m_interrupted { false };
    mutable bool m_isApplicationInBackground { false };
    bool m_willIgnoreSystemInterruptions { false };
    bool m_processIsSuspended { false };
    bool m_isPlayingToAutomotiveHeadUnit { false };

#if USE(AUDIO_SESSION)
    bool m_becameActive { false };
#endif

#if !RELEASE_LOG_DISABLED
    Ref<AggregateLogger> m_logger;
#endif
};

}

#endif // PlatformMediaSessionManager_h

/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef MediaPlayerPrivateJava_h
#define MediaPlayerPrivateJava_h

#include "MediaPlayerPrivate.h"
#include <jni.h>
#include "RQRef.h"
#include "TimeRanges.h"

namespace WebCore {
    class MediaPlayerPrivate : public MediaPlayerPrivateInterface {
    public:
        //typedef MediaPlayerPrivateInterface* (*CreateMediaEnginePlayer)(MediaPlayer*);
        //typedef void (*MediaEngineSupportedTypes)(HashSet<String>& types);
        //typedef MediaPlayer::SupportsType (*MediaEngineSupportsType)(const String& type, const String& codecs);
        //typedef void (*MediaEngineRegistrar)(CreateMediaEnginePlayer, MediaEngineSupportedTypes, MediaEngineSupportsType);
        MediaPlayerPrivate(MediaPlayer *player);

        static void MediaEngineSupportedTypes(HashSet<String, ASCIICaseInsensitiveHash>& types);

        static MediaPlayer::SupportsType MediaEngineSupportsType(const MediaEngineSupportParameters&);

        static void registerMediaEngine(MediaEngineRegistrar registrar);
    private:
        // the method caches the set
        static HashSet<String, ASCIICaseInsensitiveHash>& GetSupportedTypes();

    public:
        virtual ~MediaPlayerPrivate();

        virtual void load(const String& url);
        virtual void cancelLoad();

        virtual void prepareToPlay();
        //virtual PlatformMedia platformMedia() const { return NoPlatformMedia; }
#if USE(ACCELERATED_COMPOSITING)
        //virtual PlatformLayer* platformLayer() const { return 0; }
#endif

        virtual void play();
        virtual void pause();

        //virtual bool supportsFullscreen() const { return false; }
        //virtual bool supportsSave() const { return false; }

        virtual FloatSize naturalSize() const;

        virtual bool hasVideo() const;
        virtual bool hasAudio() const;

        virtual void setVisible(bool);

        virtual float duration() const;

        virtual float currentTime() const;
        virtual void seek(float time);
        virtual bool seeking() const;

        virtual MediaTime startTime() const;

        virtual void setRate(float);
        virtual void setPreservesPitch(bool);

        virtual bool paused() const;

        virtual void setVolume(float);

        virtual bool supportsMuting() const;
        virtual void setMuted(bool);

        //virtual bool hasClosedCaptions() const { return false; }
        //virtual void setClosedCaptionsVisible(bool) { }

        virtual MediaPlayer::NetworkState networkState() const;
        virtual MediaPlayer::ReadyState readyState() const;

        virtual float maxTimeSeekable() const;
        virtual bool didLoadingProgress() const;
        virtual std::unique_ptr<PlatformTimeRanges> buffered() const;

        virtual unsigned bytesLoaded() const;

        virtual void setSize(const IntSize&);

        virtual void paint(GraphicsContext&, const FloatRect&);

        //virtual void paintCurrentFrameInContext(GraphicsContext* c, const IntRect& r) { paint(c, r); }

        virtual void setPreload(MediaPlayer::Preload);

        //virtual bool hasAvailableVideoFrame() const { return readyState() >= MediaPlayer::HaveCurrentData; }

        //virtual bool canLoadPoster() const { return false; }
        //virtual void setPoster(const String&) { }

//#if ENABLE(PLUGIN_PROXY_FOR_VIDEO)
//        virtual void deliverNotification(MediaPlayerProxyNotificationType) = 0;
//        virtual void setMediaPlayerProxy(WebMediaPlayerProxy*) = 0;
//#endif

#if USE(ACCELERATED_COMPOSITING)
        // whether accelerated rendering is supported by the media engine for the current media.
        //virtual bool supportsAcceleratedRendering() const { return false; }
        // called when the rendering system flips the into or out of accelerated rendering mode.
        //virtual void acceleratedRenderingStateChanged() { }
#endif

        virtual bool hasSingleSecurityOrigin() const { return true; }

        //virtual MediaPlayer::MovieLoadType movieLoadType() const { return MediaPlayer::Unknown; }

        //virtual double maximumDurationToCacheMediaTime() const { return 0.5; }
    // implementation
    public:
        static inline MediaPlayerPrivate* getPlayer(jlong ptr);
        void notifyNetworkStateChanged(int networkState);
        void notifyReadyStateChanged(int readyState);
        void notifyPaused(bool paused);
        void notifySeeking(bool seeking);
        void notifyFinished();
        void notifyReady(bool hasVideo, bool hasAudio);
        void notifyDurationChanged(float duration);
        void notifySizeChanged(int width, int height);
        void notifyNewFrame();
        void notifyBufferChanged(std::unique_ptr<PlatformTimeRanges> timeRanges, int bytesLoaded);

    private:
        MediaPlayer* m_player;

        volatile MediaPlayer::NetworkState m_networkState;
        volatile MediaPlayer::ReadyState m_readyState;

        bool m_isVisible;
        bool m_hasVideo;
        bool m_hasAudio;
        FloatSize m_naturalSize;
        bool m_paused;
        bool m_seeking;
        float m_seekTime;   // valid only when m_seeking is true
        float m_duration;
        std::unique_ptr<PlatformTimeRanges> m_buffered;
        unsigned m_bytesLoaded;
        mutable bool m_didLoadingProgress;  // mutable because didLoadingProgress() is declared const

        RefPtr<RQRef> m_jPlayer;

        void setNetworkState(MediaPlayer::NetworkState networkState);
        void setReadyState(MediaPlayer::ReadyState readyState);
    };
}

#endif // MediaPlayerPrivateJava_h

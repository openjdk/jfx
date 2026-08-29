/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"
#include <wkj_constants.h>

#include "GraphicsContext.h"
#include "MediaPlayerPrivateJava.h"
#include "NotImplemented.h"
#include "PlatformContextJava.h"
#include "WKJPlatformJava.h"

#include "Document.h"
#include "Settings.h"

#include <wtf/text/CString.h> // todo tav remove when building w/ pch
#include <wtf/text/StringView.h>

namespace WebCore {



///////////////////////// log support

#if defined(NDEBUG)

    #define LOG_TRACE0(szFormat)                    ((void)0)
    #define LOG_TRACE1(szFormat, p1)                ((void)0)
    #define LOG_TRACE2(szFormat, p1, p2)            ((void)0)
    #define LOG_TRACE3(szFormat, p1, p2, p3)        ((void)0)
    #define LOG_TRACE4(szFormat, p1, p2, p3, p4)    ((void)0)

    #define LOG_ERROR0(szFormat)                    ((void)0)
    #define LOG_ERROR1(szFormat, p1)                ((void)0)

    #define PLOG_TRACE0(szFormat)                    ((void)0)
    #define PLOG_TRACE1(szFormat, p1)                ((void)0)
    #define PLOG_TRACE2(szFormat, p1, p2)            ((void)0)
    #define PLOG_TRACE3(szFormat, p1, p2, p3)        ((void)0)
    #define PLOG_TRACE4(szFormat, p1, p2, p3, p4)    ((void)0)

    #define PLOG_ERROR0(szFormat)                    ((void)0)
    #define PLOG_ERROR1(szFormat, p1)                ((void)0)

#else

    #include <stdio.h>
    #include <wtf/Threading.h>

    const char* networkStateStr(MediaPlayer::NetworkState networkState) {
        switch (networkState) {
        case MediaPlayer::NetworkState::Empty:
            return "Empty";
        case MediaPlayer::NetworkState::Idle:
            return "Idle";
        case MediaPlayer::NetworkState::Loading:
            return "Loading";
        case MediaPlayer::NetworkState::Loaded:
            return "Loaded";
        case MediaPlayer::NetworkState::FormatError:
            return "FormatError";
        case MediaPlayer::NetworkState::NetworkError:
            return "NetworkError";
        case MediaPlayer::NetworkState::DecodeError:
            return "DecodeError";
        }
        return "<unknown network state>";
    }

    const char* readyStateStr(MediaPlayer::ReadyState readyState) {
        switch (readyState) {
        case MediaPlayer::ReadyState::HaveNothing:
            return "HaveNothing";
        case MediaPlayer::ReadyState::HaveMetadata:
            return "HaveMetadata";
        case MediaPlayer::ReadyState::HaveCurrentData:
            return "HaveCurrentData";
        case MediaPlayer::ReadyState::HaveFutureData:
            return "HaveFutureData";
        case MediaPlayer::ReadyState::HaveEnoughData:
            return "HaveEnoughData";
        }
        return "<unknown ready state>";
    }

    namespace Logger {

        FILE* getLogStream() {
            FILE *stream = stderr;
            //FILE *stream = fopen("webVideo.log", "a");
            return stream;
        }

        void releaseLogStream(FILE *stream) {
            fflush(stream);
            //fclose(stream);
        }

        void AMLogf(const char* szLevel, const char* szFormat, ...) {
            FILE *stream = getLogStream();
            fprintf(stream, "[%s (native)] ", szLevel);
            va_list args;
            va_start(args, szFormat);
            vfprintf(stream, szFormat, args);
            releaseLogStream(stream);
        }

        void AMLogf_p(const MediaPlayerPrivate *p, const char* szLevel, const char* szFormat, ...) {
            FILE *stream = getLogStream();
            fprintf(stream, "[%s (native),states:(%s,%s), paused:%d, seeking:%d, pos:%f/%f]",
                szLevel, networkStateStr(p->networkState()), readyStateStr(p->readyState()),
                (p->paused() ? 1 : 0), (p->seeking() ? 1 : 0),
                p->currentTime(), p->duration());
            va_list args;
            va_start(args, szFormat);
            vfprintf(stream, szFormat, args);
            releaseLogStream(stream);
        }
    }

    #define LOG_TRACE0(szFormat)                    Logger::AMLogf("INFO", szFormat)
    #define LOG_TRACE1(szFormat, p1)                Logger::AMLogf("INFO", szFormat, p1)
    #define LOG_TRACE2(szFormat, p1, p2)            Logger::AMLogf("INFO", szFormat, p1, p2)
    #define LOG_TRACE3(szFormat, p1, p2, p3)        Logger::AMLogf("INFO", szFormat, p1, p2, p3)
    #define LOG_TRACE4(szFormat, p1, p2, p3, p4)    Logger::AMLogf("INFO", szFormat, p1, p2, p3, p4)

    #define LOG_ERROR0(szFormat)                    Logger::AMLogf("SEVERE", szFormat)
    #define LOG_ERROR1(szFormat, p1)                Logger::AMLogf("SEVERE", szFormat, p1)

    #define PLOG_TRACE0(szFormat)                   Logger::AMLogf_p(this, "INFO", szFormat)
    #define PLOG_TRACE1(szFormat, p1)               Logger::AMLogf_p(this, "INFO", szFormat, p1)
    #define PLOG_TRACE2(szFormat, p1, p2)           Logger::AMLogf_p(this, "INFO", szFormat, p1, p2)
    #define PLOG_TRACE3(szFormat, p1, p2, p3)       Logger::AMLogf_p(this, "INFO", szFormat, p1, p2, p3)
    #define PLOG_TRACE4(szFormat, p1, p2, p3, p4)   Logger::AMLogf_p(this, "INFO", szFormat, p1, p2, p3, p4)

    #define PLOG_ERROR0(szFormat)                   Logger::AMLogf_p(this, "SEVERE", szFormat)
    #define PLOG_ERROR1(szFormat, p1)               Logger::AMLogf_p(this, "SEVERE", szFormat, p1)

#endif

////////////////////////

class MediaPlayerFactoryJava final : public MediaPlayerFactory {
private:
    MediaPlayerEnums::MediaEngineIdentifier identifier() const final { return MediaPlayerEnums::MediaEngineIdentifier::MediaFoundation; };
    Ref<MediaPlayerPrivateInterface> createMediaEnginePlayer(MediaPlayer& player) const final
    {
        return adoptRef(*new MediaPlayerPrivate(player));
    }

    void getSupportedTypes(HashSet<String>& types) const final
    {
        return MediaPlayerPrivate::MediaEngineSupportedTypes(types);
    }

    MediaPlayer::SupportsType supportsTypeAndCodecs(const MediaEngineSupportParameters& parameters) const final
    {
        return MediaPlayerPrivate::MediaEngineSupportsType(parameters);
    }
};


void MediaPlayerPrivate::registerMediaEngine(MediaEngineRegistrar registrar)
{
    LOG_TRACE0(">>registerMediaEngine\n");

    // The JNI version refused to register when the com.sun.webkit.graphics.WCMediaPlayer class
    // could not be found. The class lookup went with the id cache; the equivalent test is that
    // the media table is installed and can actually create a player.
    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->create_player) {
        LOG_ERROR0("<<registerMediaEngine ERROR: MediaPlayer class is unavailable\n");
        return;
    }

    registrar(makeUnique<MediaPlayerFactoryJava>());
}

void MediaPlayerPrivate::MediaEngineSupportedTypes(HashSet<String>& types)
{
    LOG_TRACE0(">>MediaEngineSupportedTypes\n");
    HashSet<String, ASCIICaseInsensitiveHash>& supportedTypes = GetSupportedTypes();
    for (const auto& type : supportedTypes) {
        types.add(type);
    }
    LOG_TRACE0("<<MediaEngineSupportedTypes\n");
}

MediaPlayer::SupportsType MediaPlayerPrivate::MediaEngineSupportsType(const MediaEngineSupportParameters& parameters)
{
    for (const auto& codecValue: parameters.type.codecs()) {
        UNUSED_PARAM(codecValue);
        LOG_TRACE2(">>MediaEngineSupportsType, type=%s, codecs=%s\n", parameters.type.raw().utf8().data(), codecValue.utf8().data());
    }

    if (parameters.type.isEmpty()) {
        LOG_TRACE0("<<MediaEngineSupportsType: NOT supported (type is empty)\n");
        return MediaPlayer::SupportsType::IsNotSupported;
    }

    if (GetSupportedTypes().contains(parameters.type.containerType())) {
        LOG_TRACE0("<<MediaEngineSupportsType: MayBeSupported/IsSupported\n");
        auto codecs = parameters.type.parameter(ContentType::codecsParameter());
        return codecs.isEmpty() ? MediaPlayer::SupportsType::MayBeSupported : MediaPlayer::SupportsType::IsSupported;
    }
    LOG_TRACE0("<<MediaEngineSupportsType: NOT supported\n");
    return MediaPlayer::SupportsType::IsNotSupported;
}


HashSet<String, ASCIICaseInsensitiveHash>& MediaPlayerPrivate::GetSupportedTypes()
{
    static HashSet<String, ASCIICaseInsensitiveHash> supportedTypes;
    // TODO: refresh after change

    if (!supportedTypes.isEmpty()) {
        return supportedTypes;
    }

    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->get_supported_types) {
        return supportedTypes;
    }

    /*
     * WCGraphicsManager.getSupportedMediaTypes() returned a String[]; the slot returns the
     * same list as one string with the elements separated by WKJ_MEDIA_TYPE_SEPARATOR, which
     * cannot appear in a MIME type. Splitting it here produces the same set of strings the
     * GetObjectArrayElement loop produced, including for an empty array.
     */
    String joined = wkjFetchString([&](uint16_t* buf, int32_t cap, int32_t* length) {
        return cb->get_supported_types(buf, cap, length);
    });
    wkjCheckAndClearException();

    if (joined.isNull()) {
        return supportedTypes;
    }

    for (auto type : StringView(joined).split(static_cast<UChar>(WKJ_MEDIA_TYPE_SEPARATOR))) {
        supportedTypes.add(type.toString());
    }

    return supportedTypes;
}

// *********************************************************
// MediaPlayerPrivate
// *********************************************************
MediaPlayerPrivate::MediaPlayerPrivate(MediaPlayer &player)
    : m_player(player)
    , m_networkState(MediaPlayer::NetworkState::Empty)
    , m_readyState(MediaPlayer::ReadyState::HaveNothing)
    , m_isVisible(false)
    , m_hasVideo(false)
    , m_hasAudio(false)
    , m_paused(true)
    , m_seeking(false)
    , m_bytesLoaded(0)
    , m_didLoadingProgress(false)
{
    m_buffered = std::make_unique<PlatformTimeRanges>();

    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->create_player)
        return;

    WKJHandle obj { cb->create_player(wkj_from_ptr(this)) };
    ASSERT(obj);
    wkjCheckAndClearException();

    m_jPlayer = RQRef::create(obj.get());
}

MediaPlayerPrivate::~MediaPlayerPrivate()
{
    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->dispose || !m_jPlayer)
        return;

    cb->dispose(wkj_ref(*m_jPlayer));
    wkjCheckAndClearException();
}

void MediaPlayerPrivate::load(const String& url)
{
    if (m_networkState == MediaPlayer::NetworkState::Loading) {
        cancelLoad();
    }

    String userAgent;
    // MediaPlayerClient mpClient = m_player->client();
    // Document* doc = mpClient.mediaPlayerOwningDocument(); //XXX: mediaPlayerOwningDocument removed
    // if (doc != NULL && doc->settings() != NULL) {
    //     userAgent = doc->settings()->userAgent();
    // }

    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->load || !m_jPlayer)
        return;

    WKJStringArg urlArg(url);
    WKJStringArg userAgentArg(userAgent);

    // An empty user agent was passed as null and still is: userAgentArg.data() is only
    // non-null for a non-null String, and the empty case is filtered here as it was before.
    cb->load(wkj_ref(*m_jPlayer), urlArg.data(), urlArg.length(),
             userAgent.isEmpty() ? nullptr : userAgentArg.data(),
             userAgent.isEmpty() ? 0 : userAgentArg.length());
    wkjCheckAndClearException();
}

void MediaPlayerPrivate::cancelLoad()
{
    m_paused = true;
    m_seeking = false;

    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->cancel_load || !m_jPlayer)
        return;

    cb->cancel_load(wkj_ref(*m_jPlayer));
    wkjCheckAndClearException();
}

void MediaPlayerPrivate::prepareToPlay()
{
    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->prepare_to_play || !m_jPlayer)
        return;

    cb->prepare_to_play(wkj_ref(*m_jPlayer));
    wkjCheckAndClearException();
}

//PlatformMedia MediaPlayerPrivate::platformMedia() const { return NoPlatformMedia; }

//#if USE(ACCELERATED_COMPOSITING)
//        PlatformLayer* MediaPlayerPrivate::platformLayer() const { return 0; }
//#endif

void MediaPlayerPrivate::play()
void MediaPlayerPrivate::play()
{
    PLOG_TRACE0(">>MediaPlayerPrivate::play\n");

    if (!paused()) {
        PLOG_TRACE0("<<MediaPlayerPrivate::play - already playing\n");
        return;
    }

    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->play || !m_jPlayer)
        return;

    cb->play(wkj_ref(*m_jPlayer));
    wkjCheckAndClearException();

    PLOG_TRACE0("<<MediaPlayerPrivate::play\n");
}

void MediaPlayerPrivate::pause()
{
    if (paused()) {
        return;
    }

    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->pause || !m_jPlayer)
        return;

    cb->pause(wkj_ref(*m_jPlayer));
    wkjCheckAndClearException();
}

//bool MediaPlayerPrivate::supportsFullscreen() const { return false; }
//bool MediaPlayerPrivate::supportsSave() const { return false; }

FloatSize MediaPlayerPrivate::naturalSize() const
{
//    PLOG_TRACE2("MediaPlayerPrivate naturalSize - return %d x %d\n", m_naturalSize.width(), m_naturalSize.height());
    return m_naturalSize;
}

bool MediaPlayerPrivate::hasVideo() const
{
//    PLOG_TRACE1("MediaPlayerPrivate hasVideo - return %d\n", m_hasVideo ? 1 : 0);
    return m_hasVideo;
}

bool MediaPlayerPrivate::hasAudio() const
{
//    PLOG_TRACE1("MediaPlayerPrivate hasAudio - return %d\n", m_hasAudio ? 1 : 0);
    return m_hasAudio;
}

void MediaPlayerPrivate::setPageIsVisible(bool visible)
{
    if (m_isVisible != visible) {
        PLOG_TRACE2("MediaPlayerPrivate setPageIsVisible: %d => %d\n", m_isVisible ? 1 : 0, visible ? 1 : 0);
        m_isVisible = visible;
    }
}

MediaTime MediaPlayerPrivate::duration() const
{
    // return numeric_limits<float>::infinity(); // "live" stream
    return m_duration;
}

MediaTime MediaPlayerPrivate::currentTime() const
{
    if (m_seeking) {
        LOG_TRACE1("MediaPlayerPrivate currentTime returns (seekTime): %f\n", m_seekTime);
        return m_seekTime;
    }

    // In case of an "Unsupported protocol Data" error in JavaMediaPlayer the native
    // MediaElement is collected by JavaScriptCore, so currentTime can be reached from the GC
    // thread. The JNI version returned zero when there was no environment there; the host
    // table test is the equivalent, and it also covers a NULL slot.
    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->get_current_time || !m_jPlayer)
        return MediaTime::zeroTime();

    double result = cb->get_current_time(wkj_ref(*m_jPlayer));
    wkjCheckAndClearException();

//    LOG_TRACE1("MediaPlayerPrivate currentTime returns: %f\n", (float)result);
    return MediaTime::createWithDouble(result);
}

void MediaPlayerPrivate::seek(float time)
{
    PLOG_TRACE1(">>MediaPlayerPrivate::seek(%f)\n", time);

    m_seekTime = MediaTime::createWithFloat(time);

    const WKJHostMedia* cb = wkjMedia();
    if (cb && cb->seek && m_jPlayer) {
        cb->seek(wkj_ref(*m_jPlayer), time);
        wkjCheckAndClearException();
    }

    PLOG_TRACE1("<<MediaPlayerPrivate::seek(%f)\n", time);
}

bool MediaPlayerPrivate::seeking() const
{
    return m_seeking;
}
constexpr MediaPlayerType MediaPlayerPrivate::mediaPlayerType() const
{
    return MediaPlayerType::Null;
}

MediaTime MediaPlayerPrivate::startTime() const
{
    // always 0
    return MediaTime::zeroTime();
}

void MediaPlayerPrivate::setRate(float rate)
{
    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->set_rate || !m_jPlayer)
        return;

    cb->set_rate(wkj_ref(*m_jPlayer), rate);
    wkjCheckAndClearException();
}

void MediaPlayerPrivate::setPreservesPitch(bool preserve)
{
    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->set_preserves_pitch || !m_jPlayer)
        return;

    cb->set_preserves_pitch(wkj_ref(*m_jPlayer), preserve ? 1 : 0);
    wkjCheckAndClearException();
}

bool MediaPlayerPrivate::paused() const
{
    return m_paused;
}

void MediaPlayerPrivate::setVolume(float volume)
{
    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->set_volume || !m_jPlayer)
        return;

    cb->set_volume(wkj_ref(*m_jPlayer), volume);
    wkjCheckAndClearException();
}

bool MediaPlayerPrivate::supportsMuting() const
{
    return true;
}

void MediaPlayerPrivate::setMuted(bool mute)
{
    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->set_mute || !m_jPlayer)
        return;

    cb->set_mute(wkj_ref(*m_jPlayer), mute ? 1 : 0);
    wkjCheckAndClearException();
}

//bool MediaPlayerPrivate::hasClosedCaptions() const { return false; }
//void MediaPlayerPrivate::setClosedCaptionsVisible(bool) { }

MediaPlayer::NetworkState MediaPlayerPrivate::networkState() const
{
//    LOG_TRACE1("MediaPlayerPrivate networkState - return %d\n", (int)m_networkState);
    return m_networkState;
}

MediaPlayer::ReadyState MediaPlayerPrivate::readyState() const
{
//    LOG_TRACE1("MediaPlayerPrivate readyState - return %d\n", (int)m_readyState);
    return m_readyState;
}

MediaTime MediaPlayerPrivate::maxTimeSeekable() const
{
    return m_duration;
}

bool MediaPlayerPrivate::didLoadingProgress() const
{
    bool didLoadingProgress = m_didLoadingProgress;
    m_didLoadingProgress = false;
    PLOG_TRACE1("MediaPlayerPrivate didLoadingProgress - returning %d", didLoadingProgress ? 1 : 0);
    return didLoadingProgress;
}

const PlatformTimeRanges& MediaPlayerPrivate::buffered() const
{
    return *m_buffered;
}

unsigned MediaPlayerPrivate::bytesLoaded() const
{
    return m_bytesLoaded;
}

void MediaPlayerPrivate::setSize(const IntSize& size)
{
    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->set_size || !m_jPlayer)
        return;

    cb->set_size(wkj_ref(*m_jPlayer), size.width(), size.height());
    wkjCheckAndClearException();
}

void MediaPlayerPrivate::paint(GraphicsContext& gc, const FloatRect& r)
{
//    PLOG_TRACE4(">>MediaPlayerPrivate paint (%d, %d), [%d x %d]\n", r.x(), r.y(), r.width(), r.height());
    if (gc.paintingDisabled()) {
        PLOG_TRACE0("<<MediaPlayerPrivate paint (!gc or paintingDisabled)\n");
        return;
    }
    if (!m_isVisible) {
        PLOG_TRACE0("<<MediaPlayerPrivate paint (!visible)\n");
        return;
    }

    gc.platformContext()->rq().freeSpace(24)
    << (int32_t)com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIAPLAYER
    << m_jPlayer << (int32_t)r.x() <<  (int32_t)r.y()
    << (int32_t)r.width() << (int32_t)r.height();

//    PLOG_TRACE0("<<MediaPlayerPrivate paint (OK)\n");
}

//void MediaPlayerPrivate::paintCurrentFrameInContext(GraphicsContext* c, const IntRect& r) { paint(c, r); }

void MediaPlayerPrivate::setPreload(MediaPlayer::Preload preload)
{
    // enum Preload { None, MetaData, Auto };
    PLOG_TRACE1("MediaPlayerPrivate setPreload, preload=%u\n", (int)preload);
    int32_t jPreload =
        (preload == MediaPlayer::Preload::None) ? com_sun_webkit_graphics_WCMediaPlayer_PRELOAD_NONE
        : (preload == MediaPlayer::Preload::MetaData) ? com_sun_webkit_graphics_WCMediaPlayer_PRELOAD_METADATA
        : (preload == MediaPlayer::Preload::Auto) ? com_sun_webkit_graphics_WCMediaPlayer_PRELOAD_AUTO
        : -1;
    if (jPreload < 0) {
        // unexpected preload value
        return;
    }

    const WKJHostMedia* cb = wkjMedia();
    if (!cb || !cb->set_preload || !m_jPlayer)
        return;

    cb->set_preload(wkj_ref(*m_jPlayer), jPreload);
    wkjCheckAndClearException();
}

//bool MediaPlayerPrivate::hasAvailableVideoFrame() const { return readyState() >= MediaPlayer::ReadyState::HaveCurrentData; }

//bool MediaPlayerPrivate::canLoadPoster() const { return false; }
//void MediaPlayerPrivate::setPoster(const String&) { }

//#if ENABLE(PLUGIN_PROXY_FOR_VIDEO)
//        virtual void deliverNotification(MediaPlayerProxyNotificationType) = 0;
//        virtual void setMediaPlayerProxy(WebMediaPlayerProxy*) = 0;
//#endif

//#if USE(ACCELERATED_COMPOSITING)
//        // whether accelerated rendering is supported by the media engine for the current media.
//        virtual bool supportsAcceleratedRendering() const { return false; }
//        // called when the rendering system flips the into or out of accelerated rendering mode.
//        virtual void acceleratedRenderingStateChanged() { }
//#endif

//bool MediaPlayerPrivate::hasSingleSecurityOrigin() const { return false; }

//MediaPlayer::MovieLoadType MediaPlayerPrivate::movieLoadType() const { return MediaPlayer::MovieLoadType::Unknown; }

void MediaPlayerPrivate::setNetworkState(MediaPlayer::NetworkState networkState)
{
    if (m_networkState != networkState) {
        PLOG_TRACE4("MediaPlayerPrivate NetworkState: %s (%d) => %s (%d)\n",
            networkStateStr(m_networkState), (int)m_networkState, networkStateStr(networkState), (int)networkState);
        m_networkState = networkState;
        m_player.networkStateChanged();
    }
}

void MediaPlayerPrivate::setReadyState(MediaPlayer::ReadyState readyState)
{
    if (m_readyState != readyState) {
        PLOG_TRACE4("MediaPlayerPrivate ReadyState: %s (%d) => %s (%d)\n",
            readyStateStr(m_readyState), (int)m_readyState, readyStateStr(readyState), (int)readyState);
        m_readyState = readyState;
        m_player.readyStateChanged();
    }
}


MediaPlayerPrivate* MediaPlayerPrivate::getPlayer(int64_t ptr)
{
    return static_cast<MediaPlayerPrivate*>(wkj_to_ptr(ptr));
}
void MediaPlayerPrivate::notifyNetworkStateChanged(int networkState)
{
    switch (networkState) {
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_EMPTY:
        setNetworkState(MediaPlayer::NetworkState::Empty);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_IDLE:
        setNetworkState(MediaPlayer::NetworkState::Idle);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_LOADING:
        setNetworkState(MediaPlayer::NetworkState::Loading);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_LOADED:
        setNetworkState(MediaPlayer::NetworkState::Loaded);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_FORMAT_ERROR:
        setNetworkState(MediaPlayer::NetworkState::FormatError);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_NETWORK_ERROR:
        setNetworkState(MediaPlayer::NetworkState::NetworkError);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_DECODE_ERROR:
        setNetworkState(MediaPlayer::NetworkState::DecodeError);
        break;
    }
}

void MediaPlayerPrivate::notifyReadyStateChanged(int readyState)
{
    switch (readyState) {
    case com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_NOTHING:
        setReadyState(MediaPlayer::ReadyState::HaveNothing);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_METADATA:
        setReadyState(MediaPlayer::ReadyState::HaveMetadata);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_CURRENT_DATA:
        setReadyState(MediaPlayer::ReadyState::HaveCurrentData);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_FUTURE_DATA:
        setReadyState(MediaPlayer::ReadyState::HaveFutureData);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_ENOUGH_DATA:
        setReadyState(MediaPlayer::ReadyState::HaveEnoughData);
        break;
    }
}

void MediaPlayerPrivate::notifyPaused(bool paused)
{
    PLOG_TRACE2(">>MediaPlayerPrivate notifyPaused: %d => %d\n", m_paused ? 1 : 0, paused ? 1 : 0);

    if (m_paused != paused) {
        m_paused = paused;
        m_player.playbackStateChanged();
    }
}

void MediaPlayerPrivate::notifySeeking(bool seeking)
{
    PLOG_TRACE2(">>MediaPlayerPrivate notifySeeking: %d => %d\n", m_seeking ? 1 : 0, seeking ? 1 : 0);
    if (m_seeking != seeking) {
        m_seeking = seeking;
        if (!seeking) {
            // notify time change after seek completed
            //LOG_TRACE0("==MediaPlayerPrivate notifySeeking: NOTIFYING time changed\n");
            m_player.timeChanged();
        }
    }
}

void MediaPlayerPrivate::notifyFinished() {
    PLOG_TRACE0(">>MediaPlayerPrivate notifyFinished\n");
    m_player.timeChanged();
}

void MediaPlayerPrivate::notifyReady(bool hasVideo, bool hasAudio)
{
    PLOG_TRACE2(">>MediaPlayerPrivate notifyReady: hasVideo=%d, hasAudio=%d\n", hasVideo ? 1 : 0, hasAudio ? 1 : 0);
    m_hasVideo = hasVideo;
    m_hasAudio = hasAudio;
    PLOG_TRACE0("<<MediaPlayerPrivate notifyReady\n");
}

void MediaPlayerPrivate::notifyDurationChanged(float duration)
{
    PLOG_TRACE2(">>MediaPlayerPrivate notifyDurationChanged, %f => %f\n",
        m_duration, duration);
    m_duration = MediaTime::createWithFloat(duration);
    m_player.durationChanged();
}

void MediaPlayerPrivate::notifySizeChanged(int width, int height)
{
    PLOG_TRACE2("MediaPlayerPrivate notifySizeChanged: %d x %d\n", width, height);
    m_naturalSize = FloatSize(width, height); //XXX leave it as IntSize?
}

void MediaPlayerPrivate::notifyNewFrame()
{
    PLOG_TRACE0(">>MediaPlayerPrivate notifyNewFrame\n");
    m_player.repaint();
    //PLOG_TRACE0("<<MediaPlayerPrivate notifyNewFrame\n");
}

DestinationColorSpace MediaPlayerPrivate::colorSpace()
{                                                // Needs to be implemented
    notImplemented();
    return DestinationColorSpace::SRGB();
}

void MediaPlayerPrivate::notifyBufferChanged(std::unique_ptr<PlatformTimeRanges> timeRanges, int bytesLoaded)
{
    PLOG_TRACE0("MediaPlayerPrivate notifyBufferChanged\n");
    m_buffered = std::move(timeRanges);
    m_bytesLoaded = bytesLoaded;
    m_didLoadingProgress = true;
}



// *********************************************************
// C ABI entry points (com.sun.webkit.graphics.WCMediaPlayer notifications)
// *********************************************************
extern "C" {

WKJ_EXPORT void wkj_media_notify_network_state(int64_t player_peer, int32_t state)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(player_peer);
    player->notifyNetworkStateChanged(state);
}

WKJ_EXPORT void wkj_media_notify_ready_state(int64_t player_peer, int32_t state)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(player_peer);
    player->notifyReadyStateChanged(state);
}

WKJ_EXPORT void wkj_media_notify_paused(int64_t player_peer, int32_t paused)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(player_peer);
    player->notifyPaused(paused != 0);
}

WKJ_EXPORT void wkj_media_notify_seeking(int64_t player_peer, int32_t seeking, int32_t /*ready_state*/)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(player_peer);
    player->notifySeeking(seeking != 0);
}

WKJ_EXPORT void wkj_media_notify_finished(int64_t player_peer)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(player_peer);
    player->notifyFinished();
}

WKJ_EXPORT void wkj_media_notify_ready(int64_t player_peer, int32_t has_video,
                                       int32_t has_audio, float duration)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(player_peer);
    player->notifyReady(has_video != 0, has_audio != 0);
    if (duration >= 0) {
        player->notifyDurationChanged(duration);
    }
}

WKJ_EXPORT void wkj_media_notify_duration_changed(int64_t player_peer, float duration)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(player_peer);
    if (duration != player->duration().toFloat()) {
        player->notifyDurationChanged(duration);
    }
}

WKJ_EXPORT void wkj_media_notify_size_changed(int64_t player_peer, int32_t width, int32_t height)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(player_peer);
    player->notifySizeChanged(width, height);
}

WKJ_EXPORT void wkj_media_notify_new_frame(int64_t player_peer)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(player_peer);
    player->notifyNewFrame();
}

WKJ_EXPORT void wkj_media_notify_buffer_changed(int64_t player_peer, const float* ranges,
                                                int32_t count, int32_t bytes_loaded)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(player_peer);

    // `count` is the number of floats, as GetArrayLength on the float[] was; the pairs are
    // read exactly as before, and an odd count reads one past the last pair in both versions.
    PlatformTimeRanges* timeRanges = new PlatformTimeRanges();
    for (int32_t i = 0; i < count; i += 2) {
        timeRanges->add(MediaTime::createWithDouble(ranges[i]),
                        MediaTime::createWithDouble(ranges[i + 1]));
    }

    player->notifyBufferChanged(std::unique_ptr<PlatformTimeRanges>(timeRanges), bytes_loaded);
}

} // extern "C"

} // namespace WebCore

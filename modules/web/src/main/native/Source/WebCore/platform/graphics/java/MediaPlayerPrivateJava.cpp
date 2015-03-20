/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "GraphicsContext.h"
#include "JavaEnv.h"
#include "MediaPlayerPrivateJava.h"
#include "NotImplemented.h"

#include "Document.h"
#include "Settings.h"

#include <wtf/text/CString.h> // todo tav remove when building w/ pch

#include "com_sun_webkit_graphics_WCMediaPlayer.h"
#include "com_sun_webkit_graphics_GraphicsDecoder.h"


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
    #include "Threading.h"
    #include "CurrentTime.h"

    const char* networkStateStr(MediaPlayer::NetworkState networkState) {
        switch (networkState) {
        case MediaPlayer::Empty:
            return "Empty";
        case MediaPlayer::Idle:
            return "Idle";
        case MediaPlayer::Loading:
            return "Loading";
        case MediaPlayer::Loaded:
            return "Loaded";
        case MediaPlayer::FormatError:
            return "FormatError";
        case MediaPlayer::NetworkError:
            return "NetworkError";
        case MediaPlayer::DecodeError:
            return "DecodeError";
        }
        return "<unknown network state>";
    }

    const char* readyStateStr(MediaPlayer::ReadyState readyState) {
        switch (readyState) {
        case MediaPlayer::HaveNothing:
            return "HaveNothing";
        case MediaPlayer::HaveMetadata:
            return "HaveMetadata";
        case MediaPlayer::HaveCurrentData:
            return "HaveCurrentData";
        case MediaPlayer::HaveFutureData:
            return "HaveFutureData";
        case MediaPlayer::HaveEnoughData:
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




void MediaPlayerPrivate::registerMediaEngine(MediaEngineRegistrar registrar)
{
    LOG_TRACE0(">>registerMediaEngine\n");
    JNIEnv* env = WebCore_GetJavaEnv();
    jclass playerCls = PG_GetMediaPlayerClass(env);
    if (!playerCls) {
        LOG_ERROR0("<<registerMediaEngine ERROR: MediaPlayer class is unavailable\n");
        return;
    }
    //CreateMediaEnginePlayer, MediaEngineSupportedTypes, MediaEngineSupportsType, 
    //MediaEngineGetSitesInMediaCache, MediaEngineClearMediaCache, MediaEngineClearMediaCacheForSite
    registrar(CreateMediaEnginePlayer, MediaEngineSupportedTypes, MediaEngineSupportsType, 0, 0, 0);
}

PassOwnPtr<MediaPlayerPrivateInterface> MediaPlayerPrivate::CreateMediaEnginePlayer(MediaPlayer *player)
{
    return adoptPtr(new MediaPlayerPrivate(player));
}

void MediaPlayerPrivate::MediaEngineSupportedTypes(HashSet<String>& types)
{
    LOG_TRACE0(">>MediaEngineSupportedTypes\n");
    HashSet<String>& supportedTypes = GetSupportedTypes();
    for (HashSet<String>::const_iterator it = supportedTypes.begin(); it != supportedTypes.end(); ++it) {
        types.add(*it);
    }
    LOG_TRACE0("<<MediaEngineSupportedTypes\n");
}

MediaPlayer::SupportsType MediaPlayerPrivate::MediaEngineSupportsType(const MediaEngineSupportParameters& parameters)
{
    LOG_TRACE2(">>MediaEngineSupportsType, type=%s, codecs=%s\n", parameters.type.utf8().data(), parameters.codecs.utf8().data());
    if (parameters.type.isEmpty()) {
        LOG_TRACE0("<<MediaEngineSupportsType: NOT supported (type is empty)\n");
        return MediaPlayer::IsNotSupported;
    }

    if (GetSupportedTypes().contains(parameters.type)) {
        LOG_TRACE0("<<MediaEngineSupportsType: MayBeSupported/IsSupported\n");
        return parameters.codecs.isEmpty() ? MediaPlayer::MayBeSupported : MediaPlayer::IsSupported;
    }
    LOG_TRACE0("<<MediaEngineSupportsType: NOT supported\n");
    return MediaPlayer::IsNotSupported;
}

HashSet<String>& MediaPlayerPrivate::GetSupportedTypes()
{
    static HashSet<String> supportedTypes;
    // TODO: refresh after change

    if (!supportedTypes.isEmpty()) {
        return supportedTypes;
    }

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID = env->GetMethodID(PG_GetGraphicsManagerClass(env),
        "getSupportedMediaTypes", "()[Ljava/lang/String;");
    ASSERT(s_mID);

    JLocalRef<jobjectArray> jArray(
        (jobjectArray)env->CallObjectMethod(PL_GetGraphicsManager(env), s_mID));
    ASSERT(jArray);
    CheckAndClearException(env);

    jsize len = env->GetArrayLength(jArray);
    for (jsize  i=0; i<len; i++) {
        JLString jStr((jstring)env->GetObjectArrayElement(jArray, i));
        String s(env, jStr);
        supportedTypes.add(s);
    }

    return supportedTypes;
}


// *********************************************************
// MediaPlayerPrivate
// *********************************************************
MediaPlayerPrivate::MediaPlayerPrivate(MediaPlayer *player)
    : m_player(player)
    , m_networkState(MediaPlayer::Empty)
    , m_readyState(MediaPlayer::HaveNothing)
    , m_didLoadingProgress(false)
    , m_isVisible(false)
    , m_hasVideo(false)
    , m_hasAudio(false)
    , m_paused(true)
    , m_seeking(false)
    , m_duration(0)
    , m_bytesLoaded(0)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID mid = env->GetMethodID(PG_GetGraphicsManagerClass(env),
        "fwkCreateMediaPlayer", "(J)Lcom/sun/webkit/graphics/WCMediaPlayer;");
    ASSERT(mid);

    JLocalRef<jobject> obj(env->CallObjectMethod(PL_GetGraphicsManager(env),
        mid, ptr_to_jlong(this)));
    ASSERT(obj);
    CheckAndClearException(env);

    m_buffered = TimeRanges::create();
    m_jPlayer = RQRef::create(obj);
}

MediaPlayerPrivate::~MediaPlayerPrivate()
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkDispose", "()V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID);
    CheckAndClearException(env);
}

void MediaPlayerPrivate::load(const String& url)
{
    if (m_networkState == MediaPlayer::Loading) {
        cancelLoad();
    }

    String userAgent;
    MediaPlayerClient* mpClient = m_player->mediaPlayerClient();
    if (mpClient != NULL) {
        Document* doc = mpClient->mediaPlayerOwningDocument();
        if (doc != NULL && doc->settings() != NULL) {
            userAgent = doc->settings()->userAgent();
        }
    }

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkLoad", "(Ljava/lang/String;Ljava/lang/String;)V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID,
        (jstring)url.toJavaString(env),
        userAgent.isEmpty() ? NULL : (jstring)userAgent.toJavaString(env));
    CheckAndClearException(env);
}

void MediaPlayerPrivate::cancelLoad()
{
    m_paused = true;
    m_seeking = false;

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkCancelLoad", "()V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID);
    CheckAndClearException(env);
}

void MediaPlayerPrivate::prepareToPlay()
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkPrepareToPlay", "()V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID);
    CheckAndClearException(env);
}

//PlatformMedia MediaPlayerPrivate::platformMedia() const { return NoPlatformMedia; }

//#if USE(ACCELERATED_COMPOSITING)
//        PlatformLayer* MediaPlayerPrivate::platformLayer() const { return 0; }
//#endif

void MediaPlayerPrivate::play()
{
    PLOG_TRACE0(">>MediaPlayerPrivate::play\n");

    if (!paused()) {
        PLOG_TRACE0("<<MediaPlayerPrivate::play - already playing\n");
        return;
    }

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkPlay", "()V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID);
    CheckAndClearException(env);

    PLOG_TRACE0("<<MediaPlayerPrivate::play\n");
}

void MediaPlayerPrivate::pause()
{
    if (paused()) {
        return;
    }

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkPause", "()V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID);
    CheckAndClearException(env);
}

//bool MediaPlayerPrivate::supportsFullscreen() const { return false; }
//bool MediaPlayerPrivate::supportsSave() const { return false; }

IntSize MediaPlayerPrivate::naturalSize() const
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

void MediaPlayerPrivate::setVisible(bool visible)
{
    if (m_isVisible != visible) {
        PLOG_TRACE2("MediaPlayerPrivate setVisible: %d => %d\n", m_isVisible ? 1 : 0, visible ? 1 : 0);
        m_isVisible = visible;
    }
}

float MediaPlayerPrivate::duration() const
{
    // return numeric_limits<float>::infinity(); // "live" stream
    return m_duration;
}

float MediaPlayerPrivate::currentTime() const
{
    if (m_seeking) {
        LOG_TRACE1("MediaPlayerPrivate currentTime returns (seekTime): %f\n", m_seekTime);
        return m_seekTime;
    }
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkGetCurrentTime", "()F");
    ASSERT(s_mID);

    double result = env->CallFloatMethod(*m_jPlayer, s_mID);
    CheckAndClearException(env);

//    LOG_TRACE1("MediaPlayerPrivate currentTime returns: %f\n", (float)result);

    return (float)result;
}

void MediaPlayerPrivate::seek(float time)
{
    PLOG_TRACE1(">>MediaPlayerPrivate::seek(%f)\n", time);

    m_seekTime = time;

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkSeek", "(F)V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID, time);
    CheckAndClearException(env);

    PLOG_TRACE1("<<MediaPlayerPrivate::seek(%f)\n", time);
}

bool MediaPlayerPrivate::seeking() const
{
    return m_seeking;
}

float MediaPlayerPrivate::startTime() const
{
    // always 0
    return 0;
}

void MediaPlayerPrivate::setRate(float rate)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkSetRate", "(F)V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID, rate);
    CheckAndClearException(env);
}

void MediaPlayerPrivate::setPreservesPitch(bool preserve)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkSetPreservesPitch", "(Z)V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID, bool_to_jbool(preserve));
    CheckAndClearException(env);
}

bool MediaPlayerPrivate::paused() const
{
    return m_paused;
}

void MediaPlayerPrivate::setVolume(float volume)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkSetVolume", "(F)V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID, volume);
    CheckAndClearException(env);
}

bool MediaPlayerPrivate::supportsMuting() const
{
    return true;
}

void MediaPlayerPrivate::setMuted(bool mute)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID
        s_mID = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkSetMute", "(Z)V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID, bool_to_jbool(mute));
    CheckAndClearException(env);
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

float MediaPlayerPrivate::maxTimeSeekable() const
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

PassRefPtr<TimeRanges> MediaPlayerPrivate::buffered() const
{
    return m_buffered;
}

unsigned MediaPlayerPrivate::bytesLoaded() const
{
    return m_bytesLoaded;
}

void MediaPlayerPrivate::setSize(const IntSize& size)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkSetSize", "(II)V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID, (jint)size.width(), (jint)size.height());
    CheckAndClearException(env);
}

void MediaPlayerPrivate::paint(GraphicsContext* gc, const IntRect& r)
{
//    PLOG_TRACE4(">>MediaPlayerPrivate paint (%d, %d), [%d x %d]\n", r.x(), r.y(), r.width(), r.height());
    if (!gc || gc->paintingDisabled()) {
        PLOG_TRACE0("<<MediaPlayerPrivate paint (!gc or paintingDisabled)\n");
        return;
    }
    if (!m_isVisible) {
        PLOG_TRACE0("<<MediaPlayerPrivate paint (!visible)\n");
        return;
    }

    gc->platformContext()->rq().freeSpace(24)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIAPLAYER
    << m_jPlayer << (jint)r.x() <<  (jint)r.y()
    << (jint)r.width() << (jint)r.height();

//    PLOG_TRACE0("<<MediaPlayerPrivate paint (OK)\n");
}

//void MediaPlayerPrivate::paintCurrentFrameInContext(GraphicsContext* c, const IntRect& r) { paint(c, r); }

void MediaPlayerPrivate::setPreload(MediaPlayer::Preload preload)
{
    // enum Preload { None, MetaData, Auto };
    PLOG_TRACE1("MediaPlayerPrivate setPreload, preload=%u\n", (int)preload);
    jint jPreload =
        (preload == MediaPlayer::None) ? com_sun_webkit_graphics_WCMediaPlayer_PRELOAD_NONE
        : (preload == MediaPlayer::MetaData) ? com_sun_webkit_graphics_WCMediaPlayer_PRELOAD_METADATA
        : (preload == MediaPlayer::Auto) ? com_sun_webkit_graphics_WCMediaPlayer_PRELOAD_AUTO
        : -1;
    if (jPreload < 0) {
        // unexpected preload value
        return;
    }
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_mID
        = env->GetMethodID(PG_GetMediaPlayerClass(env), "fwkSetPreload", "(I)V");
    ASSERT(s_mID);

    env->CallVoidMethod(*m_jPlayer, s_mID, jPreload);
    CheckAndClearException(env);
}

//bool MediaPlayerPrivate::hasAvailableVideoFrame() const { return readyState() >= MediaPlayer::HaveCurrentData; }

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
        
//MediaPlayer::MovieLoadType MediaPlayerPrivate::movieLoadType() const { return MediaPlayer::Unknown; }

void MediaPlayerPrivate::setNetworkState(MediaPlayer::NetworkState networkState)
{
    if (m_networkState != networkState) {
        PLOG_TRACE4("MediaPlayerPrivate NetworkState: %s (%d) => %s (%d)\n",
            networkStateStr(m_networkState), (int)m_networkState, networkStateStr(networkState), (int)networkState);
        m_networkState = networkState;
        m_player->networkStateChanged();
    }
}

void MediaPlayerPrivate::setReadyState(MediaPlayer::ReadyState readyState)
{
    if (m_readyState != readyState) {
        PLOG_TRACE4("MediaPlayerPrivate ReadyState: %s (%d) => %s (%d)\n",
            readyStateStr(m_readyState), (int)m_readyState, readyStateStr(readyState), (int)readyState);
        m_readyState = readyState;
        m_player->readyStateChanged();
    }
}


MediaPlayerPrivate* MediaPlayerPrivate::getPlayer(jlong ptr)
{
    return reinterpret_cast<MediaPlayerPrivate *>(jlong_to_ptr(ptr));
}

void MediaPlayerPrivate::notifyNetworkStateChanged(int networkState)
{
    switch (networkState) {
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_EMPTY:
        setNetworkState(MediaPlayer::Empty);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_IDLE:
        setNetworkState(MediaPlayer::Idle);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_LOADING:
        setNetworkState(MediaPlayer::Loading);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_LOADED:
        setNetworkState(MediaPlayer::Loaded);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_FORMAT_ERROR:
        setNetworkState(MediaPlayer::FormatError);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_NETWORK_ERROR:
        setNetworkState(MediaPlayer::NetworkError);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_NETWORK_STATE_DECODE_ERROR:
        setNetworkState(MediaPlayer::DecodeError);
        break;
    }
}

void MediaPlayerPrivate::notifyReadyStateChanged(int readyState)
{
    switch (readyState) {
    case com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_NOTHING:
        setReadyState(MediaPlayer::HaveNothing);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_METADATA:
        setReadyState(MediaPlayer::HaveMetadata);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_CURRENT_DATA:
        setReadyState(MediaPlayer::HaveCurrentData);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_FUTURE_DATA:
        setReadyState(MediaPlayer::HaveFutureData);
        break;
    case com_sun_webkit_graphics_WCMediaPlayer_READY_STATE_HAVE_ENOUGH_DATA:
        setReadyState(MediaPlayer::HaveEnoughData);
        break;
    }
}

void MediaPlayerPrivate::notifyPaused(bool paused)
{
    PLOG_TRACE2(">>MediaPlayerPrivate notifyPaused: %d => %d\n", m_paused ? 1 : 0, paused ? 1 : 0);

    if (m_paused != paused) {
        m_paused = paused;
        m_player->playbackStateChanged();
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
            m_player->timeChanged();
        }
    }
}

void MediaPlayerPrivate::notifyFinished() {
    PLOG_TRACE0(">>MediaPlayerPrivate notifyFinished\n");
    m_player->timeChanged();
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
    m_duration = duration;
    m_player->durationChanged();
}

void MediaPlayerPrivate::notifySizeChanged(int width, int height)
{
    PLOG_TRACE2("MediaPlayerPrivate notifySizeChanged: %d x %d\n", width, height);
    m_naturalSize = IntSize(width, height);
}

void MediaPlayerPrivate::notifyNewFrame()
{
    PLOG_TRACE0(">>MediaPlayerPrivate notifyNewFrame\n");
    m_player->repaint();
    //PLOG_TRACE0("<<MediaPlayerPrivate notifyNewFrame\n");
}

void MediaPlayerPrivate::notifyBufferChanged(PassRefPtr<TimeRanges> timeRanges, int bytesLoaded)
{
    PLOG_TRACE0("MediaPlayerPrivate notifyBufferChanged\n");
    m_buffered = timeRanges;
    m_bytesLoaded = bytesLoaded;
    m_didLoadingProgress = true;
}


// *********************************************************
// JNI functions
// *********************************************************
extern "C" {
JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCMediaPlayer_notifyNetworkStateChanged
    (JNIEnv *env, jobject jThis, jlong ptr, jint networkState)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(ptr);
    player->notifyNetworkStateChanged(networkState);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCMediaPlayer_notifyReadyStateChanged
    (JNIEnv *env, jobject jThis, jlong ptr, jint readyState)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(ptr);
    player->notifyReadyStateChanged(readyState);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCMediaPlayer_notifyPaused
    (JNIEnv *env, jobject jThis, jlong ptr, jboolean paused)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(ptr);
    player->notifyPaused(jbool_to_bool(paused));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCMediaPlayer_notifySeeking
    (JNIEnv *env, jobject jThis, jlong ptr, jboolean seeking, jint readyState)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(ptr);
    player->notifyReadyStateChanged(readyState);
    player->notifySeeking(jbool_to_bool(seeking));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCMediaPlayer_notifyFinished
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(ptr);
    player->notifyFinished();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCMediaPlayer_notifyReady
    (JNIEnv *env, jobject jThis, jlong ptr, jboolean hasVideo, jboolean hasAudio, jfloat duration)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(ptr);
    player->notifyReady(jbool_to_bool(hasVideo), jbool_to_bool(hasAudio));
    if (duration >= 0) {
        player->notifyDurationChanged(duration);
    }
}

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCMediaPlayer_notifyDurationChanged
  (JNIEnv *env, jobject jThis, jlong ptr, jfloat duration)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(ptr);
    if (duration != player->duration()) {
        player->notifyDurationChanged(duration);
    }
}

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCMediaPlayer_notifySizeChanged
    (JNIEnv *env, jobject jThis, jlong ptr, jint width, jint height)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(ptr);
    player->notifySizeChanged(width, height);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCMediaPlayer_notifyNewFrame
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(ptr);
    player->notifyNewFrame();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCMediaPlayer_notifyBufferChanged
  (JNIEnv *env, jobject jThis, jlong ptr, jfloatArray ranges, jint bytesLoaded)
{
    MediaPlayerPrivate* player = MediaPlayerPrivate::getPlayer(ptr);

    jboolean isCopy;
    jint len = env->GetArrayLength(ranges);
    jfloat* rangesElems = env->GetFloatArrayElements(ranges, &isCopy);

    PassRefPtr<TimeRanges> timeRanges = TimeRanges::create();
    for (int i = 0; i < len; i+=2) {
        timeRanges->add(rangesElems[i], rangesElems[i+1]);
    } 
    if (isCopy == JNI_TRUE) {
       env->ReleaseFloatArrayElements(ranges, rangesElems, JNI_ABORT);
    }

    player->notifyBufferChanged(timeRanges, bytesLoaded);
}

} // extern "C"

} // namespace WebCore


/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "JavaPlayerEventDispatcher.h"
#include "JniUtils.h"
#include "Logger.h"
#include <com_sun_media_jfxmedia_track_AudioTrack.h>
#include <com_sun_media_jfxmediaimpl_NativeMediaPlayer.h>
#include <Common/VSMemory.h>
#include <Utils/LowLevelPerf.h>
#include <jni/Logger.h>

static bool areJMethodIDsInitialized = false;

jmethodID CJavaPlayerEventDispatcher::m_SendWarningMethod = 0;

jmethodID CJavaPlayerEventDispatcher::m_SendPlayerMediaErrorEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendPlayerHaltEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendPlayerStateEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendNewFrameEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendFrameSizeChangedEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendAudioTrackEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendVideoTrackEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendSubtitleTrackEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendMarkerEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendBufferProgressEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendDurationUpdateEventMethod = 0;
jmethodID CJavaPlayerEventDispatcher::m_SendAudioSpectrumEventMethod = 0;

CJavaPlayerEventDispatcher::CJavaPlayerEventDispatcher()
: m_PlayerVM(NULL),
  m_PlayerInstance(NULL),
  m_MediaReference(0L)
{
}

CJavaPlayerEventDispatcher::~CJavaPlayerEventDispatcher()
{
    Dispose();
}

void CJavaPlayerEventDispatcher::Init(JNIEnv *env, jobject PlayerInstance, CMedia* pMedia)
{
    LOWLEVELPERF_EXECTIMESTART("CJavaPlayerEventDispatcher::Init()");

    if (env->GetJavaVM(&m_PlayerVM) != JNI_OK) {
        // FIXME: Warning/error message??
        return;
    }
    m_PlayerInstance = env->NewGlobalRef(PlayerInstance);
    m_MediaReference = (jlong) ptr_to_jlong(pMedia);

    // Initialize jmethodID data members. These are derived from the class of
    // the object and not its instance. No, this particular implementation is
    // not thread-safe, but the worst that can happen is that the jmethodIDs are
    // initialized more than once which is still better than once per player.
    if (false == areJMethodIDsInitialized)
    {
        jclass klass = env->GetObjectClass(m_PlayerInstance);

        m_SendWarningMethod               = env->GetMethodID(klass, "sendWarning", "(ILjava/lang/String;)V");

        m_SendPlayerMediaErrorEventMethod = env->GetMethodID(klass, "sendPlayerMediaErrorEvent", "(I)V");
        m_SendPlayerHaltEventMethod       = env->GetMethodID(klass, "sendPlayerHaltEvent", "(Ljava/lang/String;D)V");
        m_SendPlayerStateEventMethod      = env->GetMethodID(klass, "sendPlayerStateEvent", "(ID)V");
        m_SendNewFrameEventMethod         = env->GetMethodID(klass, "sendNewFrameEvent", "(J)V");
        m_SendFrameSizeChangedEventMethod = env->GetMethodID(klass, "sendFrameSizeChangedEvent", "(II)V");
        m_SendAudioTrackEventMethod       = env->GetMethodID(klass, "sendAudioTrack", "(ZJLjava/lang/String;ILjava/lang/String;IIF)V");
        m_SendVideoTrackEventMethod       = env->GetMethodID(klass, "sendVideoTrack", "(ZJLjava/lang/String;IIIFZ)V");
        m_SendSubtitleTrackEventMethod    = env->GetMethodID(klass, "sendSubtitleTrack", "(ZJLjava/lang/String;ILjava/lang/String;)V");
        m_SendMarkerEventMethod           = env->GetMethodID(klass, "sendMarkerEvent", "(Ljava/lang/String;D)V");
        m_SendBufferProgressEventMethod   = env->GetMethodID(klass, "sendBufferProgressEvent", "(DJJJ)V");
        m_SendDurationUpdateEventMethod  = env->GetMethodID(klass, "sendDurationUpdateEvent", "(D)V");
        m_SendAudioSpectrumEventMethod  = env->GetMethodID(klass, "sendAudioSpectrumEvent", "(DD)V");

        env->DeleteLocalRef(klass);

        areJMethodIDsInitialized = true;
    }

    LOWLEVELPERF_EXECTIMESTOP("CJavaPlayerEventDispatcher::Init()");
}

void CJavaPlayerEventDispatcher::Dispose()
{
    LOWLEVELPERF_EXECTIMESTART("CJavaPlayerEventDispatcher::Dispose()");
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        pEnv->DeleteGlobalRef(m_PlayerInstance);
    }

    LOWLEVELPERF_EXECTIMESTOP("CJavaPlayerEventDispatcher::Dispose()");
}

void CJavaPlayerEventDispatcher::Warning(int warningCode, const char* warningMessage)
{
    if (NULL == m_PlayerInstance)
        return;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jstring jmessage = NULL;
        if (warningMessage) {
            jmessage = pEnv->NewStringUTF(warningMessage);
        }
        pEnv->CallVoidMethod(m_PlayerInstance, m_SendWarningMethod,
                             (jint)warningCode, jmessage);
        if (jmessage) {
            pEnv->DeleteLocalRef(jmessage);
        }
    }
}

bool CJavaPlayerEventDispatcher::SendPlayerMediaErrorEvent(int errorCode)
{
    return SendToJava_PlayerMediaErrorEvent(errorCode);
}

bool CJavaPlayerEventDispatcher::SendPlayerHaltEvent(const char* message, double time)
{
    return SendToJava_PlayerHaltEvent(message, time);
}

bool CJavaPlayerEventDispatcher::SendPlayerStateEvent(int newState, double presentTime)
{
    long newJavaState;

    switch(newState)
    {
    case CPipeline::Unknown:
        newJavaState = com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerUnknown;
        break;
    case CPipeline::Ready:
        newJavaState = com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerReady;
        break;
    case CPipeline::Playing:
        newJavaState = com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerPlaying;
        break;
    case CPipeline::Paused:
        newJavaState = com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerPaused;
        break;
    case CPipeline::Stopped:
        newJavaState = com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerStopped;
        break;
    case CPipeline::Stalled:
        newJavaState = com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerStalled;
        break;
    case CPipeline::Finished:
        newJavaState = com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerFinished;
        break;
    case CPipeline::Error:
        newJavaState = com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerError;
        break;
    default:
        return false;
    }

    return SendToJava_PlayerStateEvent(newJavaState, presentTime);
}

bool CJavaPlayerEventDispatcher::SendNewFrameEvent(CVideoFrame* pVideoFrame)
{
    return SendToJava_NewFrameEvent(pVideoFrame);
}

bool CJavaPlayerEventDispatcher::SendFrameSizeChangedEvent(int width, int height)
{
    return SendToJava_FrameSizeChangedEvent(width, height);
}

bool CJavaPlayerEventDispatcher::SendAudioTrackEvent(CAudioTrack* pTrack)
{
    return SendToJava_AudioTrackEvent(pTrack);
}

bool CJavaPlayerEventDispatcher::SendVideoTrackEvent(CVideoTrack* pTrack)
{
    return SendToJava_VideoTrackEvent(pTrack);
}

bool CJavaPlayerEventDispatcher::SendSubtitleTrackEvent(CSubtitleTrack* pTrack)
{
    return SendToJava_SubtitleTrackEvent(pTrack);
}

bool CJavaPlayerEventDispatcher::SendMarkerEvent(string name, double time)
{
    return SendToJava_MarkerEvent(name, time);
}

bool CJavaPlayerEventDispatcher::SendBufferProgressEvent(double clipDuration, int64_t start, int64_t stop, int64_t position)
{
   return SendToJava_BufferProgressEvent(clipDuration, start, stop, position);
}

bool CJavaPlayerEventDispatcher::SendDurationUpdateEvent(double time)
{
    return SendToJava_DurationUpdateEvent(time);
}

bool CJavaPlayerEventDispatcher::SendAudioSpectrumEvent(double time, double duration)
{
    return SendToJava_AudioSpectrumEvent(time, duration);
}
/*********************************************************************************
 * SendToJava methods section
 **********************************************************************************/
bool CJavaPlayerEventDispatcher::SendToJava_PlayerMediaErrorEvent(int errorCode)
{
    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        pEnv->CallVoidMethod(m_PlayerInstance, m_SendPlayerMediaErrorEventMethod, errorCode);
        return !jenv.reportException();
    }

    return false;
}

bool CJavaPlayerEventDispatcher::SendToJava_PlayerHaltEvent(const char* message, double time)
{
    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jstring jmessage = pEnv->NewStringUTF(message);
        pEnv->CallVoidMethod(m_PlayerInstance, m_SendPlayerHaltEventMethod, jmessage, time);
        pEnv->DeleteLocalRef(jmessage);
        return !jenv.reportException();
    }

    return false;
}

bool CJavaPlayerEventDispatcher::SendToJava_PlayerStateEvent(long eventID, double presentTime)
{
    if (NULL == m_PlayerInstance)
        return false;

    switch(eventID) {
        case com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerUnknown:
        case com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerReady:
        case com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerPlaying:
        case com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerPaused:
        case com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerStopped:
        case com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerFinished:
        case com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerStalled:
        case com_sun_media_jfxmediaimpl_NativeMediaPlayer_eventPlayerError:
        {
            LOWLEVELPERF_EXECTIMESTOP("gstInitPlatformToSendToJavaPlayerStateEventPaused");
            LOWLEVELPERF_EXECTIMESTOP("gstPauseToSendToJavaPlayerStateEventPaused");
            LOWLEVELPERF_EXECTIMESTOP("gstStopToSendToJavaPlayerStateEventStopped");
            LOWLEVELPERF_EXECTIMESTOP("gstPlayToSendToJavaPlayerStateEventPlaying");
            // Send an event only if the ID is valid.
            CJavaEnvironment jenv(m_PlayerVM);
            JNIEnv *pEnv = jenv.getEnvironment();
            if (pEnv) {
                pEnv->CallVoidMethod(m_PlayerInstance, m_SendPlayerStateEventMethod, eventID, presentTime);
                return !jenv.reportException();
            }
            break;
        }
        default:
            break;
    }

    return false;
}

bool CJavaPlayerEventDispatcher::SendToJava_NewFrameEvent(CVideoFrame* pVideoFrame)
{
    LOWLEVELPERF_EXECTIMESTART("CJavaPlayerEventDispatcher::SendToJava_NewFrameEvent()");
    bool bSucceeded = false;

    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        // SendNewFrameEvent will create the NativeVideoBuffer wrapper for the java side
        pEnv->CallVoidMethod(m_PlayerInstance, m_SendNewFrameEventMethod, ptr_to_jlong(pVideoFrame));
        bSucceeded = !jenv.reportException();
    }

    LOWLEVELPERF_EXECTIMESTOP("CJavaPlayerEventDispatcher::SendToJava_NewFrameEvent()");

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendToJava_FrameSizeChangedEvent(int width, int height)
{
    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        pEnv->CallVoidMethod(m_PlayerInstance, m_SendFrameSizeChangedEventMethod, (jint)width, (jint)height);
        return !jenv.reportException();
    }

    return false;
}

bool CJavaPlayerEventDispatcher::SendToJava_AudioTrackEvent(CAudioTrack* pTrack)
{
    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jstring name = pEnv->NewStringUTF(pTrack->GetName().c_str());
        jstring language = pEnv->NewStringUTF(pTrack->GetLanguage().c_str());

        // Translate channel mask bits from native values to Java values.
        int nativeChannelMask = pTrack->GetChannelMask();
        jint javaChannelMask = 0;
        if (nativeChannelMask & CAudioTrack::UNKNOWN)
            javaChannelMask |= com_sun_media_jfxmedia_track_AudioTrack_UNKNOWN;
        if (nativeChannelMask & CAudioTrack::FRONT_LEFT)
            javaChannelMask |= com_sun_media_jfxmedia_track_AudioTrack_FRONT_LEFT;
        if (nativeChannelMask & CAudioTrack::FRONT_RIGHT)
            javaChannelMask |= com_sun_media_jfxmedia_track_AudioTrack_FRONT_RIGHT;
        if (nativeChannelMask & CAudioTrack::FRONT_CENTER)
            javaChannelMask |= com_sun_media_jfxmedia_track_AudioTrack_FRONT_CENTER;
        if (nativeChannelMask & CAudioTrack::REAR_LEFT)
            javaChannelMask |= com_sun_media_jfxmedia_track_AudioTrack_REAR_LEFT;
        if (nativeChannelMask & CAudioTrack::REAR_RIGHT)
            javaChannelMask |= com_sun_media_jfxmedia_track_AudioTrack_REAR_RIGHT;
        if (nativeChannelMask & CAudioTrack::REAR_CENTER)
            javaChannelMask |= com_sun_media_jfxmedia_track_AudioTrack_REAR_CENTER;

        pEnv->CallVoidMethod(m_PlayerInstance, m_SendAudioTrackEventMethod,
                             (jboolean)pTrack->isEnabled(), (jlong)pTrack->GetTrackID(), name, pTrack->GetEncoding(), language,
                             pTrack->GetNumChannels(), javaChannelMask, pTrack->GetSampleRate());

        pEnv->DeleteLocalRef(name);
        pEnv->DeleteLocalRef(language);
        return !jenv.reportException();
    }

    return false;
}

bool CJavaPlayerEventDispatcher::SendToJava_VideoTrackEvent(CVideoTrack* pTrack)
{
    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jstring name = pEnv->NewStringUTF(pTrack->GetName().c_str());
        pEnv->CallVoidMethod(m_PlayerInstance, m_SendVideoTrackEventMethod,
                             (jboolean)pTrack->isEnabled(), (jlong)pTrack->GetTrackID(), name, pTrack->GetEncoding(),
                             pTrack->GetWidth(), pTrack->GetHeight(),
                             pTrack->GetFrameRate(), pTrack->HasAlphaChannel());
        pEnv->DeleteLocalRef(name);
        return !jenv.reportException();
    }

    return false;
}

bool CJavaPlayerEventDispatcher::SendToJava_SubtitleTrackEvent(CSubtitleTrack* pTrack)
{
    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jstring name = pEnv->NewStringUTF(pTrack->GetName().c_str());
        jstring language = pEnv->NewStringUTF(pTrack->GetLanguage().c_str());

        pEnv->CallVoidMethod(m_PlayerInstance, m_SendSubtitleTrackEventMethod,
                             (jboolean)pTrack->isEnabled(), (jlong)pTrack->GetTrackID(),
                             name, pTrack->GetEncoding(), language);
        pEnv->DeleteLocalRef(name);
        pEnv->DeleteLocalRef(language);

        return !jenv.reportException();
    }

    return false;
}

/******************************************************************************************
 * Creates any object with any arguments
 ******************************************************************************************/
jobject CJavaPlayerEventDispatcher::CreateObject(JNIEnv *env, jmethodID *cid,
                                                 const char* class_name, const char* signature,
                                                 jvalue* value)
{
    jclass  classe;
    jobject result;

    classe = env->FindClass(class_name);
    if( classe == NULL )
        return NULL; /* can't find/load the class, exception thrown */

    if( *cid == NULL)
    {
        *cid = env->GetMethodID(classe, "<init>", signature);
        if( *cid == NULL )
        {
            env->DeleteLocalRef(classe);
            return NULL; /* can't find/get the method, exception thrown */
        }
    }

    result = env->NewObjectA(classe, *cid, value);

    env->DeleteLocalRef(classe);
    return result;
}

jobject CJavaPlayerEventDispatcher::CreateBoolean(JNIEnv *env, jboolean boolean_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.z = boolean_value;

    return CreateObject(env, &cid, "java/lang/Boolean", "(Z)V", &value);
}

jobject CJavaPlayerEventDispatcher::CreateInteger(JNIEnv *env, jint int_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.i = int_value;

    return CreateObject(env, &cid, "java/lang/Integer", "(I)V", &value);
}

jobject CJavaPlayerEventDispatcher::CreateLong(JNIEnv *env, jlong long_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.j = long_value;

    return CreateObject(env, &cid, "java/lang/Long", "(J)V", &value);
}

jobject CJavaPlayerEventDispatcher::CreateDouble(JNIEnv *env, jdouble double_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.d = double_value;

    return CreateObject(env, &cid, "java/lang/Double", "(D)V", &value);
}

jobject CJavaPlayerEventDispatcher::CreateDuration(JNIEnv *env, jlong duration)
{
    static jmethodID constructorID = NULL;
    // We receive duration in nanoseconds, but javafx.util.Duration needs in milliseconds
    jdouble millis = duration/1000000.0;

    jclass durationClass = env->FindClass("javafx/util/Duration");
    if (durationClass == NULL)
        return NULL; /* can't find/load the class, exception thrown */

    if (constructorID == NULL)
    {
        constructorID = env->GetMethodID(durationClass, "<init>", "(D)V");
        if( constructorID == NULL )
        {
            env->DeleteLocalRef(durationClass);
            return NULL; /* can't find/get the method, exception thrown */
        }
    }

    jobject result = env->NewObject(durationClass, constructorID, millis);

    env->DeleteLocalRef(durationClass);

    return result;
}

bool CJavaPlayerEventDispatcher::SendToJava_MarkerEvent(string name, double time)
{
    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject jname = pEnv->NewStringUTF(name.c_str());
        pEnv->CallVoidMethod(m_PlayerInstance, m_SendMarkerEventMethod,
                             jname, time);
        pEnv->DeleteLocalRef(jname);
        return !jenv.reportException();
    }

    return false;
}

bool CJavaPlayerEventDispatcher::SendToJava_BufferProgressEvent(double clipDuration, int64_t start, int64_t stop, int64_t position)
{
    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        pEnv->CallVoidMethod(m_PlayerInstance, m_SendBufferProgressEventMethod, clipDuration, start, stop, position);
        return !jenv.reportException();
    }

    return false;
}

bool CJavaPlayerEventDispatcher::SendToJava_DurationUpdateEvent(double time)
{
    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        pEnv->CallVoidMethod(m_PlayerInstance, m_SendDurationUpdateEventMethod,
                             (jdouble)time);
        return !jenv.reportException();
    }

    return false;
}

bool CJavaPlayerEventDispatcher::SendToJava_AudioSpectrumEvent(double time, double duration)
{
    if (NULL == m_PlayerInstance)
        return false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        pEnv->CallVoidMethod(m_PlayerInstance, m_SendAudioSpectrumEventMethod, time, duration);
        return !jenv.reportException();
    }

    return false;
}

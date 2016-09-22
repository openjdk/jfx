/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
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
        CJavaEnvironment javaEnv(env);
        bool hasException = false;
        jclass klass = env->GetObjectClass(m_PlayerInstance);

        m_SendWarningMethod = env->GetMethodID(klass, "sendWarning", "(ILjava/lang/String;)V");
        hasException = javaEnv.reportException();

        if (!hasException)
        {
            m_SendPlayerMediaErrorEventMethod = env->GetMethodID(klass, "sendPlayerMediaErrorEvent", "(I)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendPlayerHaltEventMethod = env->GetMethodID(klass, "sendPlayerHaltEvent", "(Ljava/lang/String;D)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendPlayerStateEventMethod = env->GetMethodID(klass, "sendPlayerStateEvent", "(ID)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendNewFrameEventMethod = env->GetMethodID(klass, "sendNewFrameEvent", "(J)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendFrameSizeChangedEventMethod = env->GetMethodID(klass, "sendFrameSizeChangedEvent", "(II)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendAudioTrackEventMethod = env->GetMethodID(klass, "sendAudioTrack", "(ZJLjava/lang/String;ILjava/lang/String;IIF)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendVideoTrackEventMethod = env->GetMethodID(klass, "sendVideoTrack", "(ZJLjava/lang/String;IIIFZ)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendSubtitleTrackEventMethod = env->GetMethodID(klass, "sendSubtitleTrack", "(ZJLjava/lang/String;ILjava/lang/String;)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendMarkerEventMethod = env->GetMethodID(klass, "sendMarkerEvent", "(Ljava/lang/String;D)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendBufferProgressEventMethod = env->GetMethodID(klass, "sendBufferProgressEvent", "(DJJJ)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendDurationUpdateEventMethod  = env->GetMethodID(klass, "sendDurationUpdateEvent", "(D)V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SendAudioSpectrumEventMethod  = env->GetMethodID(klass, "sendAudioSpectrumEvent", "(DD)V");
            hasException = javaEnv.reportException();
        }

        env->DeleteLocalRef(klass);

        areJMethodIDsInitialized = !hasException;
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
        m_PlayerInstance = NULL; // prevent further calls to this object
    }

    LOWLEVELPERF_EXECTIMESTOP("CJavaPlayerEventDispatcher::Dispose()");
}

void CJavaPlayerEventDispatcher::Warning(int warningCode, const char* warningMessage)
{
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            jstring jmessage = NULL;
            if (warningMessage) {
                jmessage = pEnv->NewStringUTF(warningMessage);
                if (!jenv.reportException()) {
                    pEnv->CallVoidMethod(localPlayer, m_SendWarningMethod,
                                 (jint)warningCode, jmessage);
                    jenv.reportException();
                }
            }

            if (jmessage) {
                pEnv->DeleteLocalRef(jmessage);
            }

            pEnv->DeleteLocalRef(localPlayer);
        }
    }
}

bool CJavaPlayerEventDispatcher::SendPlayerMediaErrorEvent(int errorCode)
{
    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            pEnv->CallVoidMethod(localPlayer, m_SendPlayerMediaErrorEventMethod, errorCode);
            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendPlayerHaltEvent(const char* message, double time)
{
    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            jstring jmessage = NULL;
            jmessage = pEnv->NewStringUTF(message);
            if (!jenv.reportException()) {
                pEnv->CallVoidMethod(localPlayer, m_SendPlayerHaltEventMethod, jmessage, time);
            }

            if (jmessage) {
                pEnv->DeleteLocalRef(jmessage);
            }

            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendPlayerStateEvent(int newState, double presentTime)
{
    long newJavaState;

    switch(newState) {
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

    LOWLEVELPERF_EXECTIMESTOP("gstInitPlatformToSendToJavaPlayerStateEventPaused");
    LOWLEVELPERF_EXECTIMESTOP("gstPauseToSendToJavaPlayerStateEventPaused");
    LOWLEVELPERF_EXECTIMESTOP("gstStopToSendToJavaPlayerStateEventStopped");
    LOWLEVELPERF_EXECTIMESTOP("gstPlayToSendToJavaPlayerStateEventPlaying");

    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            pEnv->CallVoidMethod(localPlayer, m_SendPlayerStateEventMethod, newJavaState, presentTime);
            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendNewFrameEvent(CVideoFrame* pVideoFrame)
{
    LOWLEVELPERF_EXECTIMESTART("CJavaPlayerEventDispatcher::SendNewFrameEvent()");
    bool bSucceeded = false;

    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            // SendNewFrameEvent will create the NativeVideoBuffer wrapper for the java side
            pEnv->CallVoidMethod(localPlayer, m_SendNewFrameEventMethod, ptr_to_jlong(pVideoFrame));
            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    LOWLEVELPERF_EXECTIMESTOP("CJavaPlayerEventDispatcher::SendNewFrameEvent()");

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendFrameSizeChangedEvent(int width, int height)
{
    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            pEnv->CallVoidMethod(localPlayer, m_SendFrameSizeChangedEventMethod, (jint)width, (jint)height);
            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendAudioTrackEvent(CAudioTrack* pTrack)
{
    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            jstring name = NULL;
            jstring language = NULL;
            name = pEnv->NewStringUTF(pTrack->GetName().c_str());
            if (!jenv.reportException()) {
                language = pEnv->NewStringUTF(pTrack->GetLanguage().c_str());

                if (!jenv.reportException()) {
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

                    pEnv->CallVoidMethod(localPlayer,
                                         m_SendAudioTrackEventMethod,
                                         (jboolean)pTrack->isEnabled(),
                                         (jlong)pTrack->GetTrackID(),
                                         name,
                                         pTrack->GetEncoding(),
                                         language,
                                         pTrack->GetNumChannels(),
                                         javaChannelMask,
                                         pTrack->GetSampleRate());
                }
            }

            if (name) {
                pEnv->DeleteLocalRef(name);
            }

            if (language) {
                pEnv->DeleteLocalRef(language);
            }

            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendVideoTrackEvent(CVideoTrack* pTrack)
{
    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            jstring name = NULL;
            name = pEnv->NewStringUTF(pTrack->GetName().c_str());
            if (!jenv.reportException()) {
                pEnv->CallVoidMethod(localPlayer, m_SendVideoTrackEventMethod,
                                     (jboolean)pTrack->isEnabled(), (jlong)pTrack->GetTrackID(), name, pTrack->GetEncoding(),
                                     pTrack->GetWidth(), pTrack->GetHeight(),
                                     pTrack->GetFrameRate(), pTrack->HasAlphaChannel());
            }

            if (name) {
                pEnv->DeleteLocalRef(name);
            }

            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendSubtitleTrackEvent(CSubtitleTrack* pTrack)
{
    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            jstring name = NULL;
            jstring language = NULL;
            name = pEnv->NewStringUTF(pTrack->GetName().c_str());
            if (!jenv.reportException()) {
                language = pEnv->NewStringUTF(pTrack->GetLanguage().c_str());
                if (!jenv.reportException()) {
                    pEnv->CallVoidMethod(localPlayer, m_SendSubtitleTrackEventMethod,
                                         (jboolean)pTrack->isEnabled(), (jlong)pTrack->GetTrackID(),
                                         name, pTrack->GetEncoding(), language);
                }
            }

            if (name) {
                pEnv->DeleteLocalRef(name);
            }

            if (language) {
                pEnv->DeleteLocalRef(language);
            }

            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendMarkerEvent(string name, double time)
{
    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            jobject jname = NULL;
            jname = pEnv->NewStringUTF(name.c_str());
            if (!jenv.reportException()) {
                pEnv->CallVoidMethod(localPlayer, m_SendMarkerEventMethod,
                                     jname, time);
            }

            if (jname) {
                pEnv->DeleteLocalRef(jname);
            }

            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendBufferProgressEvent(double clipDuration, int64_t start, int64_t stop, int64_t position)
{
    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            pEnv->CallVoidMethod(localPlayer, m_SendBufferProgressEventMethod, clipDuration, start, stop, position);
            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendDurationUpdateEvent(double time)
{
    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            pEnv->CallVoidMethod(localPlayer, m_SendDurationUpdateEventMethod,
                                 (jdouble)time);
            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
}

bool CJavaPlayerEventDispatcher::SendAudioSpectrumEvent(double time, double duration)
{
    bool bSucceeded = false;
    CJavaEnvironment jenv(m_PlayerVM);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        jobject localPlayer = pEnv->NewLocalRef(m_PlayerInstance);
        if (localPlayer) {
            pEnv->CallVoidMethod(localPlayer, m_SendAudioSpectrumEventMethod, time, duration);
            pEnv->DeleteLocalRef(localPlayer);

            bSucceeded = !jenv.reportException();
        }
    }

    return bSucceeded;
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

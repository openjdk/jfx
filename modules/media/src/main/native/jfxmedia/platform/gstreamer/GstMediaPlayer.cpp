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

#include <com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer.h>

#include <Common/ProductFlags.h>
#include <Common/VSMemory.h>
#include <jni/JniUtils.h>
#include <jni/JavaPlayerEventDispatcher.h>
#include <MediaManagement/Media.h>
#include <PipelineManagement/Pipeline.h>
#include <jfxmedia_errors.h>
#include <Utils/LowLevelPerf.h>

using namespace std;

//*************************************************************************************************
//********** com.sun.media.jfxmediaimpl.MediaPlayer JNI support functions
//*************************************************************************************************

#ifdef __cplusplus
extern "C" {
#endif

/**
 * gstInitPlayer()
 *
 * Initializes a native player.  Each media view in JavaFX is tied to a media player.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstInitPlayer
  (JNIEnv *env, jobject obj, jlong ref_media)
{
    LOWLEVELPERF_EXECTIMESTART("gstInitPlayer()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    CJavaPlayerEventDispatcher* pEventDispatcher = new(nothrow) CJavaPlayerEventDispatcher();
    if (NULL == pEventDispatcher)
        return ERROR_MEMORY_ALLOCATION;

    pEventDispatcher->Init(env, obj, pMedia);
    pPipeline->SetEventDispatcher(pEventDispatcher);

    jint iRet = (jint)pPipeline->Init();

    LOWLEVELPERF_EXECTIMESTOP("gstInitPlayer()");

    return iRet;
}

/**
 * gstGetAudioSyncDelay()
 *
 * Gets the audio sync delay for the media.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstGetAudioSyncDelay
(JNIEnv *env, jobject obj, jlong ref_media, jlongArray jrglAudioSyncDelay)
{
    LOWLEVELPERF_EXECTIMESTART("gstGetAudioSyncDelay()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    long lAudioSyncDelay;
    uint32_t uErrCode = pPipeline->GetAudioSyncDelay(&lAudioSyncDelay);
    if (ERROR_NONE != uErrCode)
        return (jint)uErrCode;
    jlong jlAudioSyncDelay = (jlong)lAudioSyncDelay;
    env->SetLongArrayRegion(jrglAudioSyncDelay, 0, 1, &jlAudioSyncDelay);

    LOWLEVELPERF_EXECTIMESTOP("gstGetAudioSyncDelay()");

    return ERROR_NONE;
}

/**
 * gstSetAudioSyncDelay()
 *
 * Sets the audio sync delay for the media.  Use if the audio and video renderers are not in sync.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstSetAudioSyncDelay
(JNIEnv *env, jobject obj, jlong ref_media, jlong audio_sync_delay)
{
    LOWLEVELPERF_EXECTIMESTART("gstSetAudioSyncDelay()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    jint iRet = (jint)pPipeline->SetAudioSyncDelay((long)audio_sync_delay);

    LOWLEVELPERF_EXECTIMESTOP("gstSetAudioSyncDelay()");

    return iRet;
}

/**
 * gstPlay()
 *
 * Makes an asynchronous call to play the media.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstPlay
(JNIEnv *env, jobject obj, jlong ref_media)
{
    LOWLEVELPERF_EXECTIMESTART("gstPlayToSendToJavaPlayerStateEventPlaying");
    LOWLEVELPERF_EXECTIMESTART("gstPlayToAudioPreroll");
    LOWLEVELPERF_EXECTIMESTART("gstPlay()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    jint iRet = (jint)pPipeline->Play();

    LOWLEVELPERF_EXECTIMESTOP("gstPlay()");

    return iRet;
}

/**
 * gstPause()
 *
 * Makes an asynchronous call to pause the media playback.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstPause
(JNIEnv *env, jobject obj, jlong ref_media)
{
    LOWLEVELPERF_EXECTIMESTART("gstPauseToSendToJavaPlayerStateEventPaused");
    LOWLEVELPERF_EXECTIMESTART("gstPause()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    jint iRet = (jint)pPipeline->Pause();

    LOWLEVELPERF_EXECTIMESTOP("gstPause()");

    return iRet;
}

/**
 * gstStop()
 *
 * Makes an asynchronous call to sotp the media playback.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstStop
(JNIEnv *env, jobject obj, jlong ref_media)
{
    LOWLEVELPERF_EXECTIMESTART("gstStopToSendToJavaPlayerStateEventStopped");
    LOWLEVELPERF_EXECTIMESTART("gstStop()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    jint iRet = (jint)pPipeline->Stop();

    LOWLEVELPERF_EXECTIMESTOP("gstStop()");

    return iRet;
}

/**
 * gstFinish()
 *
 * Makes an asynchronous call to finish the media playback.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstFinish
(JNIEnv *env, jobject obj, jlong ref_media)
{
    LOWLEVELPERF_EXECTIMESTART("gstFinishToSendToJavaPlayerStateEventFinished");
    LOWLEVELPERF_EXECTIMESTART("gstFinish()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    jint iRet = (jint)pPipeline->Finish();

    LOWLEVELPERF_EXECTIMESTOP("gstFinish()");

    return iRet;
}

/**
 * gstGetRate()
 *
 * Makes a synchronous call to get the media playback rate.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstGetRate
(JNIEnv *env, jobject obj, jlong ref_media, jfloatArray jrgfRate)
{
    LOWLEVELPERF_EXECTIMESTART("gstGetRate()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    float fRate;
    uint32_t uRetCode = pPipeline->GetRate(&fRate);
    if (ERROR_NONE != uRetCode)
        return uRetCode;
    jfloat jfRate = (jfloat)fRate;
    env->SetFloatArrayRegion (jrgfRate, 0, 1, &jfRate);

    LOWLEVELPERF_EXECTIMESTOP("gstGetRate()");

    return ERROR_NONE;
}

/**
 * gstSetRate()
 *
 * Makes an asynchronous call to set the media playback rate.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstSetRate
(JNIEnv *env, jobject obj, jlong ref_media, jfloat rate)
{
    LOWLEVELPERF_EXECTIMESTART("gstSetRate()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    jint iRet = (jint)pPipeline->SetRate(rate);

    LOWLEVELPERF_EXECTIMESTOP("gstSetRate()");

    return iRet;
}

/**
 * gstGetPresentationTime()
 *
 * Makes a synchronous call to get the media presentation/stream time.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstGetPresentationTime
(JNIEnv *env, jobject obj, jlong ref_media, jdoubleArray jrgdPresentationTime)
{
    LOWLEVELPERF_EXECTIMESTART("gstGetPresentationTime()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    double dPresentationTime;
    uint32_t uRetCode = pPipeline->GetStreamTime(&dPresentationTime);
    if (ERROR_NONE != uRetCode)
        return uRetCode;
    jdouble jdPresentationTime = (double)dPresentationTime;
    env->SetDoubleArrayRegion (jrgdPresentationTime, 0, 1, &jdPresentationTime);

    LOWLEVELPERF_EXECTIMESTOP("gstGetPresentationTime()");

    return ERROR_NONE;
}

/**
 * gstGetVolume()
 *
 * Makes a synchronous call to get the audio volume.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstGetVolume
(JNIEnv *env, jobject obj, jlong ref_media, jfloatArray jrgfVolume)
{
    LOWLEVELPERF_EXECTIMESTART("gstGetVolume()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    float fVolume;
    uint32_t uRetCode = pPipeline->GetVolume(&fVolume);
    if (ERROR_NONE != uRetCode)
        return uRetCode;
    jfloat jfVolume = (jfloat)fVolume;
    env->SetFloatArrayRegion (jrgfVolume, 0, 1, &jfVolume);

    LOWLEVELPERF_EXECTIMESTOP("gstGetVolume()");

    return ERROR_NONE;
}

/**
 * gstSetVolume()
 *
 * Makes an asynchronous call to set the audio volume.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstSetVolume
(JNIEnv *env, jobject obj, jlong ref_media, jfloat volume)
{
    LOWLEVELPERF_EXECTIMESTART("gstSetVolume()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    jint iRet = (jint)pPipeline->SetVolume((float)volume);

    LOWLEVELPERF_EXECTIMESTOP("gstSetVolume()");

    return iRet;
}

/**
 * gstGetBalance()
 *
 * Makes a synchronous call to get the audio balance.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstGetBalance
(JNIEnv *env, jobject obj, jlong ref_media, jfloatArray jrgfBalance)
{
    LOWLEVELPERF_EXECTIMESTART("gstGetBalance()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    float fBalance;
    uint32_t uErrCode = pPipeline->GetBalance(&fBalance);
    if (ERROR_NONE != uErrCode)
        return uErrCode;
    jfloat jfBalance = (jfloat)fBalance;
    env->SetFloatArrayRegion (jrgfBalance, 0, 1, &jfBalance);

    LOWLEVELPERF_EXECTIMESTOP("gstGetBalance()");

    return ERROR_NONE;
}

/**
 * gstSetBalance()
 *
 * Makes an asynchronous call to set the audio balance.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstSetBalance
(JNIEnv *env, jobject obj, jlong ref_media, jfloat balance)
{
    LOWLEVELPERF_EXECTIMESTART("gstSetBalance()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    jint iRet = (jint)pPipeline->SetBalance((float)balance);

    LOWLEVELPERF_EXECTIMESTOP("gstSetBalance()");

    return iRet;
}

/**
 * gstGetDuration()
 *
 * Makes a synchronous call to get the duration of the media.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstGetDuration
(JNIEnv *env, jobject obj, jlong ref_media, jdoubleArray jrgdDuration)
{
    LOWLEVELPERF_EXECTIMESTART("gstGetDuration()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    double dDuration;
    uint32_t uErrCode = pPipeline->GetDuration(&dDuration);
    if (ERROR_NONE != uErrCode)
        return uErrCode;
    jdouble jdDuration = (jdouble)dDuration;
    env->SetDoubleArrayRegion (jrgdDuration, 0, 1, &jdDuration);

    LOWLEVELPERF_EXECTIMESTOP("gstGetDuration()");

    return ERROR_NONE;
}

/**
 * gstSeek()
 *
 * Makes an asynchronous call to seek to a presentation time in the media.
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMediaPlayer_gstSeek
(JNIEnv *env, jobject obj, jlong ref_media, jdouble stream_time)
{
    LOWLEVELPERF_EXECTIMESTART("gstSeekToNEWSEGMENT");
    LOWLEVELPERF_EXECTIMESTART("gstSeek()");

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    if (NULL == pMedia)
        return ERROR_MEDIA_NULL;

    CPipeline* pPipeline = (CPipeline*)pMedia->GetPipeline();
    if (NULL == pPipeline)
        return ERROR_PIPELINE_NULL;

    jint iRet = (jint)pPipeline->Seek(stream_time);

    LOWLEVELPERF_EXECTIMESTOP("gstSeek()");

    return iRet;
}

#ifdef __cplusplus
}
#endif

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

#include "com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer.h"
#include "MediaPlayer.h"
#include "Media.h"
#include "JniUtils.h"
#include "jfxmedia_errors.h"

#import "debug.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosInitPlayer
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosInitPlayer
    (JNIEnv *env, jobject obj, jlong mediaRef) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        jint error;

        MediaPlayer *mediaPlayer = [MediaPlayer alloc];

        if (NULL == mediaPlayer) {
            [pool release];
            return ERROR_MEMORY_ALLOCATION;
        }

        [mediaPlayer initPlayerWithMedia: media
                         javaEnvironment: env
                              javaPlayer: obj
                                  result: &error];

        [media setMediaPlayer: mediaPlayer];

        [pool release];

        return error;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosGetAudioSyncDelay
     * Signature: (J[J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosGetAudioSyncDelay
    (JNIEnv *env, jobject obj, jlong mediaRef, jlongArray syncDelayArr) {
        // Not implemented: http://javafx-jira.kenai.com/browse/RT-27005
        return ERROR_NONE;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetAudioSyncDelay
     * Signature: (JJ)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetAudioSyncDelay
    (JNIEnv *env, jobject obj, jlong mediaRef, jlong syncDelay) {
        // Not implemented: http://javafx-jira.kenai.com/browse/RT-27005
        return ERROR_NONE;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosPlay
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosPlay
    (JNIEnv *env, jobject obj, jlong mediaRef) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint playErrStatus = [player play];

        [pool release];
        return playErrStatus;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosPause
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosPause
    (JNIEnv *env, jobject obj, jlong mediaRef) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint pauseErrStatus = [player pause];

        [pool release];
        return pauseErrStatus;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosStop
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosStop
    (JNIEnv *env, jobject obj, jlong mediaRef) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint stopErrStatus = [player stop];

        [pool release];
        return stopErrStatus;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosGetRate
     * Signature: (J[F)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosGetRate
    (JNIEnv *env, jobject obj, jlong mediaRef, jfloatArray rateArr) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        float rate;
        jint getRateErrStatus = [player getRate: &rate];

        jfloat jRate = (jfloat) rate;
        (*env)->SetFloatArrayRegion(env, rateArr, 0, 1, &jRate);

        [pool release];
        return getRateErrStatus;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetRate
     * Signature: (JF)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetRate
    (JNIEnv *env, jobject obj, jlong mediaRef, jfloat rate) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint setRateErrStatus = [player setRate: rate];

        [pool release];
        return setRateErrStatus;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosGetPresentationTime
     * Signature: (J[D)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosGetPresentationTime
    (JNIEnv *env, jobject obj, jlong mediaRef, jdoubleArray timeArr) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        double currentTime;
        jint getTimeStatus = [player getCurrentTime: &currentTime];

        jdouble jCurrentTime = (jdouble) currentTime;
        (*env)->SetDoubleArrayRegion(env, timeArr, 0, 1, &jCurrentTime);

        [pool release];
        return getTimeStatus;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosGetVolume
     * Signature: (J[F)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosGetVolume
    (JNIEnv *env, jobject obj, jlong mediaRef, jfloatArray volumeArr) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        float volume;
        jint getVolumeStatus = [player getVolume: &volume];

        jfloat jVolume = (jfloat) volume;
        (*env)->SetFloatArrayRegion(env, volumeArr, 0, 1, &jVolume);

        [pool release];
        return getVolumeStatus;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetVolume
     * Signature: (JF)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetVolume
    (JNIEnv *env, jobject obj, jlong mediaRef, jfloat volume) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint setVolumeStatus = [player setVolume: (float) volume];

        [pool release];
        return setVolumeStatus;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosGetBalance
     * Signature: (J[F)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosGetBalance
    (JNIEnv *env, jobject obj, jlong mediaRef, jfloatArray balanceArr) {

        return ERROR_NONE; // returning ERROR_NOT_IMPLEMENTED fires onError() in Java
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetBalance
     * Signature: (JF)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetBalance
    (JNIEnv *env, jobject obj, jlong mediaRef, jfloat balance) {

        return ERROR_NONE; // returning ERROR_NOT_IMPLEMENTED fires onError() in Java
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosGetDuration
     * Signature: (J[D)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosGetDuration
    (JNIEnv *env, jobject obj, jlong mediaRef, jdoubleArray durationArr) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        Float64 duration = [media duration]; // can be -1 for unknown duration
        jdouble jDuration = (jdouble) duration;

        (*env)->SetDoubleArrayRegion(env, durationArr, 0, 1, &jDuration);

        [pool release];
        return ERROR_NONE;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSeek
     * Signature: (JD)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSeek
    (JNIEnv *env, jobject obj, jlong mediaRef, jdouble seekTime) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint seekStatus = [player seek: (double) seekTime];

        [pool release];
        return seekStatus;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosDispose
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosDispose
    (JNIEnv *env, jobject obj, jlong mediaRef) {

        Media *media = jlong_to_ptr(mediaRef);

        if (NULL != media) {
            MediaPlayer *player = [media mediaPlayer];
            if (NULL != player) {
                [player dispose];
                [player release];
            }
        }
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosFinish
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosFinish
    (JNIEnv *env, jobject obj, jlong mediaRef) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint finishStatus = [player finish];

        [pool release];
        return finishStatus;
    }


    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetOverlayX
     * Signature: (JD)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetOverlayX
    (JNIEnv *env, jobject obj, jlong mediaRef, jdouble x) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint status = [player overlaySetX: (CGFloat) x];

        [pool release];
        return status;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetOverlayY
     * Signature: (JD)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetOverlayY
    (JNIEnv *env, jobject obj, jlong mediaRef, jdouble y) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint status = [player overlaySetY: (CGFloat) y];

        [pool release];
        return status;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetOverlayVisible
     * Signature: (JZ)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetOverlayVisible
    (JNIEnv *env, jobject obj, jlong mediaRef, jboolean isVisible) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint status = [player overlaySetVisible: (BOOL) isVisible];

        [pool release];
        return status;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetOverlayWidth
     * Signature: (JD)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetOverlayWidth
    (JNIEnv *env, jobject obj, jlong mediaRef, jdouble width) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint status = [player overlaySetWidth: (CGFloat) width];

        [pool release];
        return status;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetOverlayHeight
     * Signature: (JD)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetOverlayHeight
    (JNIEnv *env, jobject obj, jlong mediaRef, jdouble height) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint status = [player overlaySetHeight: (CGFloat) height];

        [pool release];
        return status;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetOverlayPreserveRatio
     * Signature: (JZ)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetOverlayPreserveRatio
    (JNIEnv *env, jobject obj, jlong mediaRef, jboolean preserveRatio) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint status = [player overlaySetPreserveRatio: (BOOL) preserveRatio];

        [pool release];
        return status;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetOverlayOpacity
     * Signature: (JD)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetOverlayOpacity
    (JNIEnv *env, jobject obj, jlong mediaRef, jdouble opacity) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint status = [player overlaySetOpacity: (CGFloat) opacity];

        [pool release];
        return status;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer
     * Method:    iosSetOverlayTransform
     * Signature: (JDDDDDDDDDDDD)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_iosSetOverlayTransform
    (JNIEnv *env, jobject obj, jlong mediaRef,
     jdouble mxx, jdouble mxy, jdouble mxz, jdouble mxt,
     jdouble myx, jdouble myy, jdouble myz, jdouble myt,
     jdouble mzx, jdouble mzy, jdouble mzz, jdouble mzt) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        Media *media = jlong_to_ptr(mediaRef);

        if (NULL == media) {
            [pool release];
            return ERROR_MEDIA_NULL;
        }

        MediaPlayer *player = [media mediaPlayer];

        if (NULL == player) {
            [pool release];
            return ERROR_PIPELINE_NULL;
        }

        jint status = [player overlaySetTransform
                       :(CGFloat) mxx :(CGFloat) mxy :(CGFloat) mxz :(CGFloat) mxt
                       :(CGFloat) myx :(CGFloat) myy :(CGFloat) myz :(CGFloat) myt
                       :(CGFloat) mzx :(CGFloat) mzy :(CGFloat) mzz :(CGFloat) mzt];

        [pool release];
        return status;
    }


#ifdef __cplusplus
}
#endif

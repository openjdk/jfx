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

#include "JniUtils.h"
#include "com_sun_media_jfxmediaimpl_NativeAudioClip.h"

#import "AudioClip.h"


#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacInit
     * Signature: ()Z
     */
    JNIEXPORT jboolean JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacInit
    (JNIEnv *env, jclass clazz) {

        return (jboolean) true;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacLoad
     * Signature: (Lcom/sun/media/jfxmedia/locator/Locator;)J
     */
    JNIEXPORT jlong JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacLoad
    (JNIEnv *env, jclass clazz, jobject locator) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        jlong ref = 0L;
        jclass klass = (*env)->GetObjectClass(env, locator);
        jmethodID mid = (*env)->GetMethodID(
                                            env,
                                            klass,
                                            "getStringLocation",
                                            "()Ljava/lang/String;");

        if (mid != 0) {
            jstring uriJavaString = (*env)->CallObjectMethod(env, locator, mid);
            const char *uriNativeString = (*env)->GetStringUTFChars(env, uriJavaString, 0);

            AudioClip *audioClip = [[AudioClip alloc] init];
            if (audioClip != nil) {
                NSString *urlString = [NSString stringWithCString: uriNativeString
                                                         encoding: NSUTF8StringEncoding];
                [audioClip load: urlString];
                ref = ptr_to_jlong(audioClip);
            }

            (*env)->ReleaseStringUTFChars(env, uriJavaString, uriNativeString);
            (*env)->DeleteLocalRef(env, klass);
        }

        [pool release];
        return ref;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacPlay
     * Signature: (JDDDDII)V
     */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacPlay
    (JNIEnv *env, jobject obj, jlong ref, jdouble volume, jdouble balance, jdouble pan,
     jdouble rate, jint loopCount, jint priority) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        AudioClip *audioClip = jlong_to_ptr(ref);

        if (NULL == audioClip) {
            [pool release];
            return;
        }

        [audioClip setPan: pan];
        [audioClip setVolume: volume];
        [audioClip setRate: rate];
        [audioClip setLoopCount: loopCount];
        [audioClip play];

        [pool release];
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacIsPlaying
     * Signature: (J)Z
     */
    JNIEXPORT jboolean JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacIsPlaying
    (JNIEnv *env, jobject obj, jlong ref) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        AudioClip *audioClip = jlong_to_ptr(ref);

        if (NULL == audioClip) {
            [pool release];
            return (jboolean) false;
        }

        BOOL isPlaying = [audioClip isPlaying];

        [pool release];
        return (jboolean) isPlaying;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacStop
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacStop
    (JNIEnv *env, jobject obj, jlong ref) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        AudioClip *audioClip = jlong_to_ptr(ref);

        if (NULL == audioClip) {
            [pool release];
            return;
        }

        [audioClip stop];

        [pool release];
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacStopAll
     * Signature: ()V
     */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacStopAll
    (JNIEnv *env, jclass clazz) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        [AudioClip stopAll];

        [pool release];
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacUnload
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacUnload
    (JNIEnv *env, jclass clazz, jlong ref) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        AudioClip *audioClip = jlong_to_ptr(ref);

        if (NULL == audioClip) {
            [pool release];
            return;
        }

        [audioClip unload];

        [pool release];
    }


    //////////////////////////////////////////////////////////////////////////////////////////
    //
    // On iOS we don't support the following methods.
    // They're not called from Java anyway since there's no public API yet that would use them.
    //
    ///////////////////////////////////////////////////////////////////////////////////////////


    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacCreate
     * Signature: ([BIIIII)J
     */
    JNIEXPORT jlong JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacCreate
    (JNIEnv *env, jclass clazz, jbyteArray ba, jint i1, jint i2, jint i3, jint i4, jint i5) {

        // UNSUPPORTED

        return 0L;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacCreateSegment
     * Signature: (JDD)Lcom/sun/media/jfxmediaimpl/NativeAudioClip;
     */
    JNIEXPORT jobject JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacCreateSegment__JDD
    (JNIEnv *env, jobject obj, jlong jl, jdouble d1, jdouble d2) {

        // UNSUPPORTED

        return 0;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacCreateSegment
     * Signature: (JII)Lcom/sun/media/jfxmediaimpl/NativeAudioClip;
     */
    JNIEXPORT jobject JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacCreateSegment__JII
    (JNIEnv *env, jobject obj, jlong jl, jint i1, jint i2) {

        // UNSUPPORTED

        return 0;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacResample
     * Signature: (JIII)Lcom/sun/media/jfxmediaimpl/NativeAudioClip;
     */
    JNIEXPORT jobject JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacResample
    (JNIEnv *env, jobject obj, jlong l1, jint i1, jint i2, jint i3) {

        // UNSUPPORTED

        return 0;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioClip
     * Method:    nacAppend
     * Signature: (JJ)Lcom/sun/media/jfxmediaimpl/NativeAudioClip;
     */
    JNIEXPORT jobject JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioClip_nacAppend
    (JNIEnv *env, jobject obj, jlong ref, jlong otherClip) {

        // UNSUPPORTED

        return 0;
    }

#ifdef __cplusplus
}
#endif

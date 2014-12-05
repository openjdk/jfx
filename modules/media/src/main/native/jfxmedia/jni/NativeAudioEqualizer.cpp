/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_media_jfxmediaimpl_NativeAudioEqualizer.h"
#include <PipelineManagement/AudioEqualizer.h>
#include "JniUtils.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioEqualizer_nativeGetEnabled(JNIEnv *env, jobject obj, jlong nativeRef)
{
    // nativeRef is a pointer to a CAudioEqualizer object
    CAudioEqualizer *pEqualizer = (CAudioEqualizer*)jlong_to_ptr(nativeRef);
    return (NULL != pEqualizer) ? pEqualizer->IsEnabled() : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioEqualizer_nativeSetEnabled(JNIEnv *env, jobject obj, jlong nativeRef, jboolean enabled)
{
    CAudioEqualizer *pEqualizer = (CAudioEqualizer*)jlong_to_ptr(nativeRef);
    if (NULL != pEqualizer)
        pEqualizer->SetEnabled(enabled==JNI_TRUE);
}

JNIEXPORT jint JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioEqualizer_nativeGetNumBands(JNIEnv *env, jobject obj, jlong nativeRef)
{
    CAudioEqualizer *pEqualizer = (CAudioEqualizer*)jlong_to_ptr(nativeRef);
    return (NULL != pEqualizer) ? pEqualizer->GetNumBands() : 0;
}

JNIEXPORT jobject JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioEqualizer_nativeAddBand(JNIEnv *env, jobject obj, jlong nativeRef,
                                                                                jdouble centerFrequency, jdouble bandWidth, jdouble gain)
{
    static jmethodID mid_EqualizerBandConstructor = NULL;

    CAudioEqualizer *pEqualizer = (CAudioEqualizer*)jlong_to_ptr(nativeRef);
    if (NULL != pEqualizer) {
        CEqualizerBand *band = pEqualizer->AddBand(centerFrequency, bandWidth, gain);
        if (NULL != band) {
            jclass bandClass = env->FindClass("com/sun/media/jfxmediaimpl/NativeEqualizerBand");

            if (NULL == mid_EqualizerBandConstructor)
                mid_EqualizerBandConstructor = env->GetMethodID(bandClass, "<init>", "(J)V");

            jobject band_instance = env->NewObject(bandClass, mid_EqualizerBandConstructor, ptr_to_jlong(band));
            env->DeleteLocalRef(bandClass);

            return band_instance;
        }
    }

    return  NULL;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioEqualizer_nativeRemoveBand(JNIEnv *env, jobject obj, jlong nativeRef,
                                                                                   jdouble centerFrequency)
{
    CAudioEqualizer *pEqualizer = (CAudioEqualizer*)jlong_to_ptr(nativeRef);
    return (NULL != pEqualizer) ? pEqualizer->RemoveBand(centerFrequency) : JNI_FALSE;
}

#ifdef __cplusplus
}
#endif

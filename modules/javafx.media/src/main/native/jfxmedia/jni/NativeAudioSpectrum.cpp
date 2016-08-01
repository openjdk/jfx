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

#include "com_sun_media_jfxmediaimpl_NativeAudioSpectrum.h"
#include <PipelineManagement/AudioSpectrum.h>
#include "JavaBandsHolder.h"
#include <jni.h>
#include "JniUtils.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeGetEnabled(JNIEnv *env, jobject obj, jlong nativeRef)
{
    CAudioSpectrum *pSpectrum = (CAudioSpectrum*)jlong_to_ptr(nativeRef);
    return (NULL != pSpectrum) ? pSpectrum->IsEnabled() : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeSetEnabled(JNIEnv *env, jobject obj, jlong nativeRef,
                                                                                  jboolean enabled)
{
    CAudioSpectrum *pSpectrum = (CAudioSpectrum*)jlong_to_ptr(nativeRef);
    if (pSpectrum != NULL)
        pSpectrum->SetEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeSetBands(JNIEnv *env, jobject obj, jlong nativeRef,
                                                                                jint bands, jfloatArray magnitudes, jfloatArray phases)
{
    CAudioSpectrum *pSpectrum = (CAudioSpectrum*)jlong_to_ptr(nativeRef);
    CBandsHolder *pHolder = new (std::nothrow) CJavaBandsHolder(env, bands, magnitudes, phases);

    if (pSpectrum != NULL && pHolder != NULL)
        pSpectrum->SetBands(bands, pHolder);
}

JNIEXPORT jdouble JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeGetInterval(JNIEnv *env, jobject obj, jlong nativeRef)
{
    CAudioSpectrum *pSpectrum = (CAudioSpectrum*)jlong_to_ptr(nativeRef);
    return (NULL != pSpectrum) ? pSpectrum->GetInterval() : 0.0;
}

JNIEXPORT void JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeSetInterval(JNIEnv *env, jobject obj, jlong nativeRef,
                                                                                                          jdouble interval)
{
    CAudioSpectrum *pSpectrum = (CAudioSpectrum*)jlong_to_ptr(nativeRef);
    if (pSpectrum != NULL)
        pSpectrum->SetInterval(interval);
}

JNIEXPORT jint JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeGetThreshold(JNIEnv *env, jobject obj, jlong nativeRef)
{
    CAudioSpectrum *pSpectrum = (CAudioSpectrum*)jlong_to_ptr(nativeRef);
    return (NULL != pSpectrum) ? pSpectrum->GetThreshold() : 0;
}

JNIEXPORT void JNICALL
Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeSetThreshold(JNIEnv *env, jobject obj, jlong nativeRef,
                                                                                    jint threshold)
{
    CAudioSpectrum *pSpectrum = (CAudioSpectrum*)jlong_to_ptr(nativeRef);
    if (pSpectrum != NULL)
        pSpectrum->SetThreshold(threshold);
}

#ifdef __cplusplus
}
#endif

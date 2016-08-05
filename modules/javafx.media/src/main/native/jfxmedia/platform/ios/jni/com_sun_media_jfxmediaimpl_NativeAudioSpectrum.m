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

#include "com_sun_media_jfxmediaimpl_NativeAudioSpectrum.h"

#import "debug.h"

#ifdef __cplusplus
extern "C" {
#endif
    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioSpectrum
     * Method:    nativeGetEnabled
     * Signature: (J)Z
     */
    JNIEXPORT jboolean JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeGetEnabled
    (JNIEnv *, jobject, jlong);

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioSpectrum
     * Method:    nativeSetEnabled
     * Signature: (JZ)V
     */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeSetEnabled
    (JNIEnv *, jobject, jlong, jboolean);

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioSpectrum
     * Method:    nativeSetBands
     * Signature: (JI[F[F)V
     */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeSetBands
    (JNIEnv *env, jobject obj, jlong jl, jint ji, jfloatArray jfa1, jfloatArray jfa2);

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioSpectrum
     * Method:    nativeGetInterval
     * Signature: (J)D
     */
    JNIEXPORT jdouble JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeGetInterval
    (JNIEnv *, jobject, jlong);

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioSpectrum
     * Method:    nativeSetInterval
     * Signature: (JD)V
     */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeSetInterval
    (JNIEnv *, jobject, jlong, jdouble);

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioSpectrum
     * Method:    nativeGetThreshold
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeGetThreshold
    (JNIEnv *, jobject, jlong);

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeAudioSpectrum
     * Method:    nativeSetThreshold
     * Signature: (JI)V
     */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_NativeAudioSpectrum_nativeSetThreshold
    (JNIEnv *, jobject, jlong, jint);

#ifdef __cplusplus
}
#endif

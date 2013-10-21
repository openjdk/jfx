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

#include "com_sun_media_jfxmediaimpl_platform_gstreamer_GSTEqualizerBand.h"

#include <jni/JniUtils.h>
#include <PipelineManagement/AudioEqualizer.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jdouble JNICALL
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTEqualizerBand_gstGetCenterFrequency(JNIEnv *env, jobject obj, jlong band_ref)
{
    CEqualizerBand* pBand = (CEqualizerBand*)jlong_to_ptr(band_ref);
    return (jdouble)pBand->GetCenterFrequency();
}

JNIEXPORT void JNICALL
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTEqualizerBand_gstSetCenterFrequency(JNIEnv *env, jobject obj, jlong band_ref,
    jdouble centerFrequency)
{
    CEqualizerBand* pBand = (CEqualizerBand*)jlong_to_ptr(band_ref);
    pBand->SetCenterFrequency(centerFrequency);
}

JNIEXPORT jdouble JNICALL
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTEqualizerBand_gstGetBandwidth(JNIEnv *env, jobject obj, jlong band_ref)
{
    CEqualizerBand* pBand = (CEqualizerBand*)jlong_to_ptr(band_ref);
    return (jdouble)pBand->GetBandwidth();
}

JNIEXPORT void JNICALL
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTEqualizerBand_gstSetBandwidth(JNIEnv *env, jobject obj, jlong band_ref,
    jdouble bandwidth)
{
    CEqualizerBand* pBand = (CEqualizerBand*)jlong_to_ptr(band_ref);
    pBand->SetBandwidth(bandwidth);
}

JNIEXPORT jdouble JNICALL
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTEqualizerBand_gstGetGain(JNIEnv *env, jobject obj, jlong band_ref)
{
    CEqualizerBand* pBand = (CEqualizerBand*)jlong_to_ptr(band_ref);
    return (jdouble)pBand->GetGain();
}

JNIEXPORT void JNICALL
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTEqualizerBand_gstSetGain(JNIEnv *env, jobject obj, jlong band_ref, jdouble gain)
{
    CEqualizerBand* pBand = (CEqualizerBand*)jlong_to_ptr(band_ref);
    pBand->SetGain(gain);
}

#ifdef __cplusplus
}
#endif

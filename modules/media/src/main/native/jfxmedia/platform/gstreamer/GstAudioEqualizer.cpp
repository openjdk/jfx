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

#include "GstAudioEqualizer.h"

#include <com_sun_media_jfxmediaimpl_platform_gstreamer_GSTAudioEqualizer.h>

#include <jni/JniUtils.h>
#include <MediaManagement/Media.h>
#include <PipelineManagement/Pipeline.h>
#include <PipelineManagement/AudioEqualizer.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTAudioEqualizer_gstGetEnabled(JNIEnv *env, jobject obj, jlong ref_media)
{
    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    CAudioEqualizer *pEqualizer = pMedia->GetPipeline()->GetAudioEqualizer();

    return (NULL != pEqualizer) ? pEqualizer->IsEnabled() : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTAudioEqualizer_gstSetEnabled(JNIEnv *env, jobject obj, jlong ref_media, jboolean enabled)
{
    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    CAudioEqualizer *pEqualizer = pMedia->GetPipeline()->GetAudioEqualizer();

    if (NULL != pEqualizer)
        pEqualizer->SetEnabled(enabled==JNI_TRUE);
}

JNIEXPORT jint JNICALL
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTAudioEqualizer_gstGetNumBands(JNIEnv *env, jobject obj, jlong ref_media)
{
    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    CAudioEqualizer *pEqualizer = pMedia->GetPipeline()->GetAudioEqualizer();

    return (NULL != pEqualizer) ? pEqualizer->GetNumBands() : 0;
}

JNIEXPORT jobject JNICALL
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTAudioEqualizer_gstAddBand(JNIEnv *env, jobject obj, jlong ref_media,
                                                                                jdouble centerFrequency, jdouble bandWidth, jdouble gain)
{
    static jmethodID mid_EqualizerBandConstructor = NULL;

    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    CAudioEqualizer *pEqualizer = pMedia->GetPipeline()->GetAudioEqualizer();
    if (NULL != pEqualizer)
    {
        CEqualizerBand *band = pEqualizer->AddBand(centerFrequency, bandWidth, gain);
        if (NULL != band)
        {
            jclass bandClass = env->FindClass("com/sun/media/jfxmediaimpl/platform/gstreamer/GSTEqualizerBand");

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
Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTAudioEqualizer_gstRemoveBand(JNIEnv *env, jobject obj, jlong ref_media,
                                                                                   jdouble centerFrequency)
{
    CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);
    CAudioEqualizer *pEqualizer = pMedia->GetPipeline()->GetAudioEqualizer();

    return (NULL != pEqualizer) ? pEqualizer->RemoveBand(centerFrequency) : JNI_FALSE;
}

#ifdef __cplusplus
}
#endif

/***********************************************************************************
 * CGstEqualizerBand
 ***********************************************************************************/
CGstEqualizerBand::CGstEqualizerBand() : m_Band(NULL), m_Equalizer(NULL)
{}

CGstEqualizerBand::CGstEqualizerBand(double bandwidth, double gain, CGstAudioEqualizer* p_Equalizer)
    : CEqualizerBand(bandwidth, gain), m_Band(NULL), m_Equalizer(p_Equalizer)
{}

CGstEqualizerBand::CGstEqualizerBand(const CGstEqualizerBand& other)
    : CEqualizerBand(other.m_Bandwidth, other.m_Gain)
{
    m_Band = (other.m_Band) ? GST_OBJECT(gst_object_ref(other.m_Band)) : NULL;
    m_Equalizer = other.m_Equalizer;
}

CGstEqualizerBand::~CGstEqualizerBand()
{
    if (m_Band)
        gst_object_unref(m_Band);
}

double CGstEqualizerBand::GetCenterFrequency()
{
    gdouble result;
    g_object_get(m_Band, "freq", &result, NULL);
    return result;
}

void CGstEqualizerBand::SetCenterFrequency(double centerFrequency)
{
    g_object_set(m_Band, "freq", centerFrequency, NULL);
}

double CGstEqualizerBand::GetBandwidth()
{
    return m_Bandwidth;
}

void CGstEqualizerBand::SetBandwidth(double bandwidth)
{
    if (m_Bandwidth != bandwidth)
    {
        m_Bandwidth = bandwidth;
        g_object_set(m_Band, "bandwidth", bandwidth, NULL);
    }
}

double CGstEqualizerBand::GetGain()
{
    return m_Gain;
}

void CGstEqualizerBand::SetGain(double gain)
{
    if (m_Gain != gain)
    {
        m_Gain = gain;

        if (m_Equalizer->m_IsEnabled)
            g_object_set(m_Band, "gain", gain, NULL);
    }
}

void CGstEqualizerBand::ReplaceBand(GstObject* p_Band)
{
    if (m_Band)
        gst_object_unref(m_Band);

    m_Band = p_Band;
}
/***********************************************************************************
 * CGstAudioEqualizer
 ***********************************************************************************/
CGstAudioEqualizer::CGstAudioEqualizer(GstElement *pEqualizer)
    : m_IsEnabled(true)
{
    m_pEqualizer = GST_ELEMENT(gst_object_ref(pEqualizer));
}

CGstAudioEqualizer::~CGstAudioEqualizer()
{
    gst_object_unref(m_pEqualizer);
}

bool CGstAudioEqualizer::IsEnabled()
{
    return m_BandMap.size() > 0 && m_IsEnabled;
}

void CGstAudioEqualizer::SetEnabled(bool isEnabled)
{
    m_IsEnabled = isEnabled;
    for (BandMap::iterator band_it = m_BandMap.begin(); band_it != m_BandMap.end(); band_it++)
        g_object_set(band_it->second.m_Band, "gain", m_IsEnabled ? band_it->second.GetGain() : 0.0, NULL);
}

int CGstAudioEqualizer::GetNumBands()
{
    return (int)m_BandMap.size();
}

void CGstAudioEqualizer::UpdateBands()
{
    g_object_set(m_pEqualizer, "num-bands", m_BandMap.size(), NULL);

    int index = 0;
    for (BandMap::iterator band_it = m_BandMap.begin(); band_it != m_BandMap.end(); band_it++)
    {
        band_it->second.ReplaceBand(gst_child_proxy_get_child_by_index (GST_CHILD_PROXY (m_pEqualizer), index++));
        g_object_set(band_it->second.m_Band, "freq", band_it->first,
                                             "bandwidth", band_it->second.GetBandwidth(),
                                             "gain", m_IsEnabled ? band_it->second.GetGain() : 0.0, NULL);
    }
}

CEqualizerBand* CGstAudioEqualizer::AddBand(double frequency, double bandwidth, double gain)
{
    BandMap::iterator band_it = m_BandMap.find(frequency);

    if (band_it == m_BandMap.end()) // Add a new band
    {
        m_BandMap[frequency] = CGstEqualizerBand(bandwidth, gain, this);
        UpdateBands();
        return &m_BandMap[frequency];
    }
    else // Update an existing band
        return NULL;
}

bool CGstAudioEqualizer::RemoveBand(double frequency)
{
    BandMap::iterator band_it = m_BandMap.find(frequency);

    if (band_it != m_BandMap.end())
    {
        m_BandMap.erase(band_it);
        UpdateBands();
        return true;
    }
    else
        return false;
}

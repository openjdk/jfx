/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _GST_AUDIO_EQUALIZER_H_
#define _GST_AUDIO_EQUALIZER_H_

#include <PipelineManagement/AudioEqualizer.h>
#include <gst/gst.h>
#include <map>

using namespace std;

class CGstAudioEqualizer;

class CGstEqualizerBand : public CEqualizerBand
{
    friend class CGstAudioEqualizer;

public:
    CGstEqualizerBand();
    CGstEqualizerBand(double bandwidth, double gain, CGstAudioEqualizer* p_Equalizer);
    CGstEqualizerBand(const CGstEqualizerBand& other);
    ~CGstEqualizerBand();

    double  GetCenterFrequency();
    void    SetCenterFrequency(double centerFrequency);
    double  GetBandwidth();
    void    SetBandwidth(double bandwidth);
    double  GetGain();
    void    SetGain(double gain);

private:
    void    ReplaceBand (GstObject* p_Band);

    GstObject*          m_Band;
    CGstAudioEqualizer* m_Equalizer;
};

class CGstAudioEqualizer : public CAudioEqualizer
{
    friend class CGstEqualizerBand;

public:
    CGstAudioEqualizer(GstElement* equalizer);
    virtual ~CGstAudioEqualizer();

    bool             IsEnabled();
    void             SetEnabled(bool isEnabled);
    int              GetNumBands();
    CEqualizerBand*  AddBand(double frequency, double bandwidth, double gain);
    bool             RemoveBand(double frequency);

private:
    typedef map<double,CGstEqualizerBand> BandMap; // freq -> [bandwidth, gain, GstObject*]

    void        UpdateBands();

    GstElement* m_pEqualizer;
    BandMap     m_BandMap;
    bool        m_IsEnabled;
};

#endif // _GST_AUDIO_EQUALIZER_H_

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

#ifndef _AUDIO_EQUALIZER_H_
#define _AUDIO_EQUALIZER_H_

class CEqualizerBand
{
public:
        CEqualizerBand() : m_Bandwidth(0.0), m_Gain(0.0) {}
        CEqualizerBand(double bandwidth, double gain) : m_Bandwidth(bandwidth), m_Gain(gain) {}
virtual ~CEqualizerBand() {}

    virtual double  GetCenterFrequency() = 0;
    virtual void    SetCenterFrequency(double centerFrequency) = 0;
    virtual double  GetBandwidth() = 0;
    virtual void    SetBandwidth(double bandwidth) = 0;
    virtual double  GetGain() = 0;
    virtual void    SetGain(double gain) = 0;

protected:
    double     m_Bandwidth;
    double     m_Gain;
};

class CAudioEqualizer
{
public:
    virtual ~CAudioEqualizer() {}

    virtual bool             IsEnabled() = 0;
    virtual void             SetEnabled(bool isEnabled) = 0;
    virtual int              GetNumBands() = 0;
    virtual CEqualizerBand*  AddBand(double frequency, double bandwidth, double gain) = 0;
    virtual bool             RemoveBand(double frequency) = 0;
};

#endif // _AUDIO_EQUALIZER_H_

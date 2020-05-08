/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __JFXMedia__AVFAudioEqualizer__
#define __JFXMedia__AVFAudioEqualizer__

#include <PipelineManagement/AudioEqualizer.h>

#include <map>
#include <memory>

#include <AudioUnit/AudioUnit.h>

class AVFAudioEqualizer;

struct AVFEQBandHistory {
public:
    double x1, x2; // input history
    double y1, y2; // output history

    AVFEQBandHistory() :
    x1(0.0),
    x2(0.0),
    y1(0.0),
    y2(0.0) {
    }
};

class AVFEqualizerBand : public CEqualizerBand {
public:

    enum AVFEqualizerFilterType {
        Peak, // Use for middle bands
        LowShelf, // Use for lowest freq band
        HighShelf // Use for highest freq band
    };
    AVFEqualizerBand(AVFAudioEqualizer *eq, double frequency, double bandwidth, double gain);

    virtual ~AVFEqualizerBand();

    virtual double GetCenterFrequency() {
        return mFrequency;
    }

    virtual void SetCenterFrequency(double centerFrequency);

    virtual double GetBandwidth() {
        return m_Bandwidth;
    }

    virtual void SetBandwidth(double bandwidth) {
        m_Bandwidth = bandwidth;
        RecalculateParams();
    }

    virtual double GetGain() {
        return m_Gain;
    }

    virtual void SetGain(double gain) {
        m_Gain = gain;
        RecalculateParams();
    }

    void ApplyFilter(double *inSource, double *inDest, int frameCount, int channel);
    void SetChannelCount(int newCount);
    void RecalculateParams();
    void SetFilterType(AVFEqualizerFilterType type);

private:
    AVFAudioEqualizer *mEQ;
    bool mBypass;
    int mChannels; // number of channels to process
    AVFEQBandHistory *mHistory; // one per channel
    double mFrequency;
    AVFEqualizerFilterType mFilterType;

    // We're implementing a simple biquadratic peak/notch filter (depending on gain)
    // We need: center frequency (Hz), sample rate (Hz), Q and gain (dB)
    // We are provided: center frequency (Hz), bandwidth (Hz) and gain (dB)
    // Sample rate is fetched from the associated audio unit
    double mCoefficients[5];

    void SetupPeakFilter(double omega, double bw, double absGain);
    void SetupLowShelfFilter(double omega, double bw, double absGain);
    void SetupHighShelfFilter(double omega, double bw, double absGain);
};

typedef std::map<double, AVFEqualizerBand*> AVFEQBandMap;
typedef AVFEQBandMap::iterator AVFEQBandIterator;

// Simple bridge class that forwards messages to it's AVFMediaPlayer

class AVFAudioEqualizer : public CAudioEqualizer {
public:

    AVFAudioEqualizer();
    virtual ~AVFAudioEqualizer();

    virtual bool IsEnabled();
    virtual void SetEnabled(bool isEnabled);
    virtual int GetNumBands();
    virtual CEqualizerBand *AddBand(double frequency, double bandwidth, double gain);
    virtual bool RemoveBand(double frequency);

    void MoveBand(double oldFrequency, double newFrequency);

    void SetSampleRate(UInt32 rate);
    void SetChannels(UInt32 count);
    UInt32 GetSampleRate();
    UInt32 GetChannels();

    bool ProcessBufferLists(const AudioBufferList & buffer,
                                UInt32 inFramesToProcess);

    void RunFilter(const Float32 *inSourceP, Float32 *inDestP, UInt32 inFramesToProcess, UInt32 channel);

    // Call this after adding, removing or reordering bands
    void ResetBandParameters();

private:
    bool mEnabled;
    AVFEQBandMap mEQBands;
    int mEQBufferSize;
    double *mEQBufferA; // temp storage since we have to process out of line
    double *mEQBufferB;
    UInt32 mSampleRate;
    UInt32 mChannels;
};

typedef std::shared_ptr<AVFAudioEqualizer> AVFAudioEqualizerPtr;

#endif /* defined(__JFXMedia__AVFAudioEqualizer__) */

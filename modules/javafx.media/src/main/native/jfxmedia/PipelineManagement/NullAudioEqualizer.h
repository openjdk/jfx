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

#ifndef __JFXMedia__NullAudioEqualizer__
#define __JFXMedia__NullAudioEqualizer__

#include "AudioEqualizer.h"
#include <map>

/*
 * Audio Equalizer that does nothing, but provides behavior expected by the stack.
 */

class CNullEqualizerBand : public CEqualizerBand {
public:
    CNullEqualizerBand() : CEqualizerBand() {}
    CNullEqualizerBand(double frequency, double bandwidth, double gain)
        : CEqualizerBand(bandwidth, gain), mFrequency(frequency) {}
    virtual ~CNullEqualizerBand() {}

    virtual double  GetCenterFrequency() {
        return mFrequency;
    }

    virtual void SetCenterFrequency(double centerFrequency) {
        mFrequency = centerFrequency;
    }

    virtual double  GetBandwidth() {
        return m_Bandwidth;
    }

    virtual void    SetBandwidth(double bandwidth) {
        m_Bandwidth = bandwidth;
    }

    virtual double  GetGain() {
        return m_Gain;
    }

    virtual void    SetGain(double gain) {
        m_Gain = gain;
    }

private:
    double mFrequency;
};

class CNullAudioEqualizer : public CAudioEqualizer {
public:
    CNullAudioEqualizer() : mEnabled(false) {}
    virtual ~CNullAudioEqualizer() {}

    virtual bool IsEnabled() {
        return mEQBands.size() > 0 && mEnabled;
    }

    virtual void SetEnabled(bool isEnabled) {
        mEnabled = isEnabled;
    }

    virtual int GetNumBands() {
        return (int)mEQBands.size();
    }

    // TODO: critical section?
    virtual CEqualizerBand*  AddBand(double frequency, double bandwidth, double gain) {
        BandMap::iterator band_it = mEQBands.find(frequency);
        if (band_it == mEQBands.end()) {
            // add new band
            mEQBands[frequency] = CNullEqualizerBand(frequency, bandwidth, gain);
            return &mEQBands[frequency];
        }

        // Update an existing band
        return NULL;
    }

    virtual bool RemoveBand(double frequency) {
        BandMap::iterator band = mEQBands.find(frequency);
        if (band != mEQBands.end()) {
            mEQBands.erase(band);
        }
        return true;
    }

private:
    typedef std::map<double,CNullEqualizerBand> BandMap; // freq -> [bandwidth, gain]

    bool mEnabled;
    BandMap mEQBands;
};

#endif /* defined(__JFXMedia__NullAudioEqualizer__) */

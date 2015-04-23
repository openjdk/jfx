/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

#include "AVFAudioEqualizer.h"
#include <Accelerate/Accelerate.h>

#define kAVFMinimumQFactor 1e-9

#define IND_A0 0
#define IND_A1 1
#define IND_A2 2
#define IND_B1 3
#define IND_B2 4

class AVFEqualizerKernel : public AUKernelBase {
public:
    AVFEqualizerKernel(AVFAudioEqualizer *eq, AUEffectBase *inAudioUnit) :
        AUKernelBase(dynamic_cast<AUEffectBase*>(inAudioUnit)),
        mEQ(eq)
    {}

    virtual ~AVFEqualizerKernel() {}

    virtual void Process(const Float32 *inSourceP,
                         Float32 *inDestP,
                         UInt32 inFramesToProcess,
                         UInt32 inNumChannels,
                         bool& ioSilence) {
        if (ioSilence) {
            return;
        }

        mEQ->RunFilter(inSourceP, inDestP, inFramesToProcess, mChannelNum);
    }

private:
    AVFAudioEqualizer *mEQ;
};

#pragma mark -

AVFEqualizerBand::AVFEqualizerBand(AVFAudioEqualizer *eq, double frequency, double bandwidth, double gain) :
    CEqualizerBand(bandwidth, gain),
    mEQ(eq),
    mBypass(true),
    mChannels(0),
    mHistory(NULL),
    mFrequency(frequency),
    mFilterType(Peak)  // set later by the EQ, can be changed if band moves
{
    // we may not have an audio unit yet
    int channels = mEQ->GetChannelCount();
    if (channels > 0) {
        SetChannelCount(channels);
    }
    RecalculateParams();
}

AVFEqualizerBand::~AVFEqualizerBand() {
    if (mHistory) {
        delete[] mHistory;
        mHistory = NULL;
    }
}

void AVFEqualizerBand::SetFilterType(AVFEqualizerBand::AVFEqualizerFilterType type) {
    mFilterType = type;
    RecalculateParams();
}

void AVFEqualizerBand::SetCenterFrequency(double centerFrequency) {
    mEQ->MoveBand(mFrequency, centerFrequency);
    mFrequency = centerFrequency;
    RecalculateParams();
}

void AVFEqualizerBand::SetChannelCount(int newCount) {
    if (newCount == mChannels) {
        return;
    }

    if (mHistory) {
        delete[] mHistory;
        mHistory = NULL;
    }

    mChannels = newCount;
    if (mChannels > 0) {
        mHistory = new AVFEQBandHistory[mChannels];
    }
}

// These calculations are based on the GStreamer equalizer, so we can produce
// basically the same results
static inline double calculate_omega(double centerFreq, double sampleRate) {
    if (centerFreq / sampleRate >= 0.5) {
        return M_PI;
    }
    if (centerFreq < 0.0) {
        return 0.0;
    }
    return 2.0 * M_PI * (centerFreq / sampleRate);
}

static inline double calculate_bandwidth(double bw, double rate) {
    if (bw / rate >= 0.5) {
        // tan(M_PI/2) fails, so set to slightly less than pi
        return M_PI - 0.00000001;
    }
    if (bw <= 0.0) {
        // this effectively disables the filter
        return 0.0;
    }
    return 2.0 * M_PI * (bw / rate);
}

/*
 * vDSP_deq22:
 * https://developer.apple.com/library/mac/documentation/Accelerate/Reference/vDSPRef/Reference/reference.html#//apple_ref/c/func/vDSP_deq22
 *
 * The biquadratic filter equation for the nth sample is:
 * D[n] = S[n] * a0 + S[n-1] * a1 + S[n-2] * a2 - D[n-1] * b1 - D[n-2] * b2
 *
 * vDSP_deq22 stuffs all coefficients in vector B and uses:
 * for p in [0,2]:
 *     A(n-p)i * B(p) -> A(n)*B[0] + A(n-1)*B[1] + A(n-2)*B[2]
 *
 * for p in [3,4]:
 *     C(n-p+2)k * B(p) -> C(n-1)*B[3] + C(n-2)*B[4]
 *
 * where A and C are vectors of at least size N+2
 * so B[0..2] is a0 to a2 respectively and B[3..4] is b1 and b2 respectively
 *
 * The formulae used to calculate the coefficients are taken from GStreamer so
 * we can match the behavior of the GStreamer pipeline (and they work well enough)
 * though modified for SIMD operations using vDSP_deq22.
 *
 * Note that the GStreamer coefficient names (a0-a2,b0-b2) are swapped from other
 * examples, but the use is the same.
 */
void AVFEqualizerBand::SetupPeakFilter(double omega, double bw, double gain) {
    double cosF = cos(omega);
    double alpha =  tan(bw / 2.0);
    double alpha1 = alpha * gain;
    double alpha2 = alpha / gain;

    // set up peak filter coefficients
    mCoefficients[IND_A0] = 1.0 + alpha1;
    mCoefficients[IND_A1] = -2.0 * cosF;
    mCoefficients[IND_A2] = 1.0 - alpha1;
    double b0 = 1.0 + alpha2;
    mCoefficients[IND_B1] = -2.0 * cosF;
    mCoefficients[IND_B2] = 1.0 - alpha2;

    // pre-scale coefficients
    vDSP_vsdivD(mCoefficients, 1, &b0, mCoefficients, 1, 5);
}

void AVFEqualizerBand::SetupLowShelfFilter(double omega, double bw, double gain) {
    double egm = gain - 1.0;
    double egp = gain + 1.0;
    double alpha = tan(bw / 2.0);
    double delta = 2.0 * sqrt(gain) * alpha;
    double cosF = cos(omega);

    mCoefficients[IND_A0] = (egp - egm * cosF + delta) * gain;
    mCoefficients[IND_A1] = (egm - egp * cosF) * 2.0 * gain;
    mCoefficients[IND_A2] = (egp - egm * cosF - delta) * gain;
    double b0 = egp + egm * cosF + delta;
    mCoefficients[IND_B1] = (egm + egp * cosF) * -2.0;
    mCoefficients[IND_B2] = egp + egm * cosF - delta;

    // pre-scale coefficients
    vDSP_vsdivD(mCoefficients, 1, &b0, mCoefficients, 1, 5);
}

void AVFEqualizerBand::SetupHighShelfFilter(double omega, double bw, double gain) {
    double egm = gain - 1.0;
    double egp = gain + 1.0;
    double alpha = tan(bw / 2.0);
    double delta = 2.0 * sqrt(gain) * alpha;
    double cosF = cos(omega);

    mCoefficients[IND_A0] = (egp + egm * cosF + delta) * gain;
    mCoefficients[IND_A1] = (egm + egp * cosF) * -2.0 * gain;
    mCoefficients[IND_A2] = (egp + egm * cosF - delta) * gain;
    double b0 = egp - egm * cosF + delta;
    mCoefficients[IND_B1] = (egm - egp * cosF) * 2.0;
    mCoefficients[IND_B2] = egp - egm * cosF - delta;

    // pre-scale coefficients
    vDSP_vsdivD(mCoefficients, 1, &b0, mCoefficients, 1, 5);
}

void AVFEqualizerBand::RecalculateParams() {
    double rate = mEQ->GetSampleRate();

    mBypass = (rate == 0.0);
    if (mBypass) {
        // can't calculate until we have a sample rate
        return;
    }

    // recalculate coefficients based on new parameters
    double bw = calculate_bandwidth(m_Bandwidth, rate);
    if (bw <= 0.0) {
        // no bandwidth, no filter...
        mBypass = true;
        return;
    }
    double absGain = pow(10, m_Gain / 40);      // convert dB to scale
    double omega = calculate_omega(mFrequency, rate);

    switch (mFilterType) {
        case Peak:
            SetupPeakFilter(omega, bw, absGain);
            break;
        case LowShelf:
            SetupLowShelfFilter(omega, bw, absGain);
            break;
        case HighShelf:
            SetupHighShelfFilter(omega, bw, absGain);
            break;
    }
}

void AVFEqualizerBand::ApplyFilter(double *inSource, double *inDest, int frameCount, int channel) {
    if (mBypass && mEQ->GetSampleRate() > 0.0) {
        // Have a sample rate now, can recalculate
        RecalculateParams();
    }

    if (mBypass || channel < 0) {
        return;
    }

    // We may have more channels now than when we were initialized
    if (channel > mChannels) {
        mChannels = mEQ->GetChannelCount();
        SetChannelCount(mChannels);
    }

    if (mChannels > 0 && mHistory != NULL) {
        // copy source and dest history
        inSource[1] = mHistory[channel].x1;
        inSource[0] = mHistory[channel].x2;
        inDest[1] = mHistory[channel].y1;
        inDest[0] = mHistory[channel].y2;
        
        vDSP_deq22D(inSource, 1, mCoefficients, inDest, 1, frameCount);

        // update history
        mHistory[channel].x1 = inSource[frameCount+1];
        mHistory[channel].x2 = inSource[frameCount];
        mHistory[channel].y1 = inDest[frameCount+1];
        mHistory[channel].y2 = inDest[frameCount];
    }
}

#pragma mark -

AVFAudioEqualizer::~AVFAudioEqualizer() {
    mEQBufferA.free();
    mEQBufferB.free();

    // Free the EQ bands, otherwise they'll leak
    for (AVFEQBandIterator iter = mEQBands.begin(); iter != mEQBands.end(); iter++) {
        if (iter->second) {
            delete iter->second;
        }
    }
    mEQBands.clear();
}

AUKernelBase *AVFAudioEqualizer::NewKernel() {
    return new AVFEqualizerKernel(this, mAudioUnit);
}

bool AVFAudioEqualizer::IsEnabled() {
    return mEnabled;
}

void AVFAudioEqualizer::SetEnabled(bool isEnabled) {
    mEnabled = isEnabled;
}

int AVFAudioEqualizer::GetNumBands() {
    return (int)mEQBands.size();
}

CEqualizerBand *AVFAudioEqualizer::AddBand(double frequency, double bandwidth, double gain) {
    if (!mEQBands[frequency]) {
        mEQBands[frequency] = new AVFEqualizerBand(this, frequency, bandwidth, gain);
    } else {
        mEQBands[frequency]->SetBandwidth(bandwidth);
        mEQBands[frequency]->SetGain(gain);
    }
    ResetBandParameters();
    return mEQBands[frequency];
}

bool AVFAudioEqualizer::RemoveBand(double frequency) {
    AVFEqualizerBand *band = mEQBands[frequency];
    if (band) {
        mEQBands.erase(frequency);
        delete band;
        ResetBandParameters();
        return true;
    }
    return false;
}

void AVFAudioEqualizer::MoveBand(double oldFrequency, double newFrequency) {
    // only if freq actually changes
    if (oldFrequency != newFrequency) {
        AVFEqualizerBand *band = mEQBands[oldFrequency];
        if (band) {
            RemoveBand(newFrequency);
            mEQBands[newFrequency] = band;
            mEQBands.erase(oldFrequency);
        }
        ResetBandParameters();
    }
}

void AVFAudioEqualizer::ResetBandParameters() {
    // Update channel counts, recalculate params if necessary
    // bands are automatically sorted by the map from low to high
    for (AVFEQBandIterator iter = mEQBands.begin(); iter != mEQBands.end();) {
        if (!iter->second) {
            // NULL pointer protection, just remove the offending band
            mEQBands.erase(iter++);
            if (!mEQBands.empty() && (iter == mEQBands.end())) {
                // re-process the last valid band, otherwise it won't be set to
                // HighShelf filter type
                --iter;
            } else {
                continue;
            }
        }
        AVFEqualizerBand *band = iter->second;
        // middle bands are peak/notch filters
        AVFEqualizerBand::AVFEqualizerFilterType type = AVFEqualizerBand::Peak;

        if (iter == mEQBands.begin()) {
            type = AVFEqualizerBand::LowShelf;
        } else if (iter == --(mEQBands.end())) {
            type = AVFEqualizerBand::HighShelf;
        }

        band->SetFilterType(type);
        band->SetChannelCount(GetChannelCount());
        band->RecalculateParams();
        iter++; // here due to NULL ptr protection, otherwise we double increment
    }
}

void AVFAudioEqualizer::SetAudioUnit(AUEffectBase *unit) {
    this->AVFKernelProcessor::SetAudioUnit(unit);
    ResetBandParameters();
}

void AVFAudioEqualizer::RunFilter(const Float32 *inSourceP,
                                  Float32 *inDestP,
                                  UInt32 inFramesToProcess,
                                  UInt32 channel) {
    if (mEnabled && !mEQBands.empty()) {
        if (inFramesToProcess + 2 > mEQBufferSize) {
            mEQBufferSize = inFramesToProcess + 2;
            mEQBufferA.free();
            mEQBufferA.alloc(mEQBufferSize);
            mEQBufferB.free();
            mEQBufferB.alloc(mEQBufferSize);
        }

        // start processing with A buffer first
        bool srcA = true;

        // The first two elements are copied each time we call a band to process
            // float* cast is needed for Xcode 4.5
        vDSP_vspdp((float*)inSourceP, 1, mEQBufferA.get() + 2, 1, inFramesToProcess);

        // Run each band in sequence
        for (AVFEQBandIterator iter = mEQBands.begin(); iter != mEQBands.end(); iter++) {
            if (iter->second) {
                if (srcA) {
                    iter->second->ApplyFilter(mEQBufferA, mEQBufferB, inFramesToProcess, channel);
                } else {
                    iter->second->ApplyFilter(mEQBufferB, mEQBufferA, inFramesToProcess, channel);
                }
                srcA = !srcA;
            }
        }

        // Copy back to dest stream
        vDSP_vdpsp((srcA ? mEQBufferA : mEQBufferB)+2, 1, inDestP, 1, inFramesToProcess);
    }
}

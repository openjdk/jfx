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

#ifndef __JFXMedia__AVFAudioSpectrumUnit__
#define __JFXMedia__AVFAudioSpectrumUnit__

#include <AudioUnit/AudioUnit.h>
#include "PipelineManagement/AudioSpectrum.h"
#include "AVFKernelProcessor.h"
#include "CASpectralProcessor.h"
#include "CAStreamBasicDescription.h"
#include "CAAutoDisposer.h"

// Defaults, these match the current defaults in JavaFX which get set anyways
// but we can optimize things a bit here...
#define kDefaultAudioSpectrumUpdateInterval 0.1 // every 1/10 second
#define kDefaultAudioSpectrumThreshold -60.0    // -60 dB

/*
 * Callback proc invoked by the audio spectrum unit. This call is made periodically
 * depending on the requested update interval. The band data is updated out-of-line.
 *
 * callbackContext: user specified context pointer
 * timeStamp: the beginning time in seconds of the sample period (from beginning of stream)
 * duration: the length of time in seconds of the sample period
 */
typedef void (*AVFSpectrumUnitCallbackProc)(void *callbackContext, double duration);

class AVFAudioSpectrumUnit : public AVFKernelProcessor, public CAudioSpectrum {
public:
    AVFAudioSpectrumUnit();
    virtual ~AVFAudioSpectrumUnit();

    // We'll use ProcessBufferLists as it sends all channels at once instead
    // of individual channels
    virtual OSStatus ProcessBufferLists(AudioUnitRenderActionFlags& ioActionFlags,
                                        const AudioBufferList& inBuffer,
                                        AudioBufferList& outBuffer,
                                        UInt32 inFramesToProcess);

    virtual void StreamFormatChanged(const CAStreamBasicDescription &newFormat);

    // Parameter accessors
    virtual bool IsEnabled();
    virtual void SetEnabled(bool isEnabled);

    virtual void SetBands(int bands, CBandsHolder* holder);
    virtual size_t GetBands();

    virtual double GetInterval();
    virtual void SetInterval(double interval);

    virtual int GetThreshold();
    virtual void SetThreshold(int threshold);

    virtual void UpdateBands(int size, const float* magnitudes, const float* phases);

    void Reset();
    void SetSampleRate(Float32 rate);
    void SetChannelCount(int count);
    
    void SetSpectrumCallbackProc(AVFSpectrumUnitCallbackProc proc, void *context) {
        mSpectrumCallbackProc = proc;
        mSpectrumCallbackContext = context;
    }

    // Called by the spectrum processor, do not call
    void SpectralFunction(SpectralBufferList* inSpectra);

private:
    AVFSpectrumUnitCallbackProc mSpectrumCallbackProc;
    void *mSpectrumCallbackContext;
    bool mEnabled;
    int mBandCount;
    CBandsHolder *mBands;
    double mUpdateInterval;
    Float32 mThreshold;
    CASpectralProcessor *mProcessor;

    AudioBufferList mMixBuffer;
    int mMixBufferFrameCapacity;    // number of frames that can currently be stored in mix buffer

    UInt32 mSamplesPerInterval;
    UInt32 mFFTSize;                // number of samples per FFT
    UInt32 mFFTsPerInterval;        // integral number of FFTs per update interval
    UInt32 mFFTCount;               // number of FFTs performed since last update

    CAAutoFree<Float32> mWorkBuffer; // temp vectors for calculations
    CAAutoFree<Float32> mMagnitudes; // magnitude accumulator
    CAAutoFree<Float32> mPhases;     // phase accumulator

    bool mRebuildCrunch; // atomic lock avoidance...
    CASpectralProcessor *mSpectralCrunch;

    void SetupSpectralProcessor();
};

#endif /* defined(__JFXMedia__AVFAudioSpectrumUnit__) */

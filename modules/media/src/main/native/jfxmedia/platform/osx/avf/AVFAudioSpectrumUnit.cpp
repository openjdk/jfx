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

#include "AVFAudioSpectrumUnit.h"
#include "AUEffectBase.h"

#include <iostream>
#include <Accelerate/Accelerate.h>

// Determines the amount of overlap when running FFT operations
// More oversampling produces smoother results, at the cost of CPU time
// This doesn't have much effect until you get to high bin counts, with JavaFX
// running 128 as the default, there doesn't seem to be much gain to doing
// more than 2x
// NOTE: this should be a user configurable option...
#define kSpectrumOversampleFactor 2

AVFAudioSpectrumUnit::AVFAudioSpectrumUnit() :
    AVFKernelProcessor(),
    mSpectrumCallbackProc(NULL),
    mSpectrumCallbackContext(NULL),

    mEnabled(true),
    mBandCount(128),
    mBands(NULL),
    mUpdateInterval(kDefaultAudioSpectrumUpdateInterval),
    mThreshold(kDefaultAudioSpectrumThreshold),
    mProcessor(NULL),

    mMixBufferFrameCapacity(0),

    mSamplesPerInterval(0),
    mFFTSize(0),
    mFFTsPerInterval(0),
    mFFTCount(0),
    mWorkBuffer(),
    mMagnitudes(),
    mPhases(),

    mRebuildCrunch(true),
    mSpectralCrunch(NULL)
{
    mMixBuffer.mNumberBuffers = 1;
    mMixBuffer.mBuffers[0].mData = NULL;
}

AVFAudioSpectrumUnit::~AVFAudioSpectrumUnit() {
    if (mMixBuffer.mBuffers[0].mData) {
        free(mMixBuffer.mBuffers[0].mData);
        mMixBuffer.mBuffers[0].mData = NULL;
    }
    if (mSpectralCrunch) {
        delete mSpectralCrunch;
        mSpectralCrunch = NULL;
    }
    mWorkBuffer.free();
    mMagnitudes.free();
    mPhases.free();
}

OSStatus AVFAudioSpectrumUnit::ProcessBufferLists(AudioUnitRenderActionFlags& ioActionFlags,
                                                  const AudioBufferList& inBuffer,
                                                  AudioBufferList& outBuffer,
                                                  UInt32 inFramesToProcess)
{
    // (Re)allocate mix buffer if needed
    if (!mMixBuffer.mBuffers[0].mData || mMixBufferFrameCapacity < inFramesToProcess) {
        // allocate buffer list (only need to do this once)
        if (mMixBuffer.mBuffers[0].mData) {
            free(mMixBuffer.mBuffers[0].mData);
            mMixBuffer.mBuffers[0].mData = NULL;
        }

        mMixBufferFrameCapacity = mAudioUnit->GetMaxFramesPerSlice();

        mMixBuffer.mBuffers[0].mNumberChannels = 1;
        mMixBuffer.mBuffers[0].mData = calloc(mMixBufferFrameCapacity, sizeof(Float32));
        mMixBuffer.mBuffers[0].mDataByteSize = 0; // size of actual contained data, not size of buffer
    }

    if (mRebuildCrunch) {
        SetupSpectralProcessor();
    }
    if (mSpectralCrunch) {
        // Mix the audio into one channel since JavaFX only supports single channel spectrum
        // Just use an arithmetic average, nothing fancy here
        float *buffer = (float*)mMixBuffer.mBuffers[0].mData;
        vDSP_vclr(buffer, 1, mMixBufferFrameCapacity);
        for (int ii = 0; ii < inBuffer.mNumberBuffers; ii++) {
            vDSP_vadd((float*)inBuffer.mBuffers[ii].mData, 1,
                      buffer, 1,
                      buffer, 1, inFramesToProcess);
        }
        float divisor = (float)inBuffer.mNumberBuffers;
        vDSP_vsdiv(buffer, 1,
                   &divisor,
                   buffer, 1, inFramesToProcess);
        mMixBuffer.mBuffers[0].mDataByteSize = inFramesToProcess * sizeof(Float32);

        mSpectralCrunch->ProcessForwards(inFramesToProcess, &mMixBuffer);
    }
    return noErr;
}

void AVFAudioSpectrumUnit::StreamFormatChanged(const CAStreamBasicDescription &newFormat) {
    // just trigger rebuilding the spectrum based on an updated format
    mRebuildCrunch = true;
}

bool AVFAudioSpectrumUnit::IsEnabled() {
    return mEnabled;
}

void AVFAudioSpectrumUnit::SetEnabled(bool isEnabled) {
    mEnabled = isEnabled;
    mRebuildCrunch = true;
}

void AVFAudioSpectrumUnit::SetBands(int bands, CBandsHolder* holder) {
    mBandCount = bands;
    mBands = holder;
    mRebuildCrunch = true;
}

size_t AVFAudioSpectrumUnit::GetBands() {
    return mBandCount;
}

double AVFAudioSpectrumUnit::GetInterval() {
    return mUpdateInterval;
}

void AVFAudioSpectrumUnit::SetInterval(double interval) {
    if (mUpdateInterval != interval) {
        mUpdateInterval = interval;
        mRebuildCrunch = true;
    }
}

int AVFAudioSpectrumUnit::GetThreshold() {
    return (int)mThreshold;
}

void AVFAudioSpectrumUnit::SetThreshold(int threshold) {
    if (mThreshold != (Float32)threshold) {
        mThreshold = (Float32)threshold;
    }
}

void AVFAudioSpectrumUnit::UpdateBands(int size, const float* magnitudes, const float* phases) {
}

void AVFAudioSpectrumUnit::Reset() {
    mRebuildCrunch = true;
}

static void AVFAudioSpectrum_SpectralFunction(SpectralBufferList* inSpectra, void* inUserData) {
    AVFAudioSpectrumUnit *unit = static_cast<AVFAudioSpectrumUnit*>(inUserData);
    if (unit) {
        unit->SpectralFunction(inSpectra);
    }
}

void AVFAudioSpectrumUnit::SpectralFunction(SpectralBufferList* inSpectra) {
    // https://developer.apple.com/library/mac/documentation/Performance/Conceptual/vDSP_Programming_Guide/UsingFourierTransforms/UsingFourierTransforms.html
    // Scale the results properly, scale factor is 2x for 1D real forward transforms
    float scale = 2.0;
    DSPSplitComplex *cplx = inSpectra->mDSPSplitComplex;
    vDSP_vsmul(cplx->realp, 1, &scale, cplx->realp, 1, mBandCount);
    vDSP_vsmul(cplx->imagp, 1, &scale, cplx->imagp, 1, mBandCount);

    if (mMagnitudes()) {
        // Calculate magnitudes: (C.r^^2 + C.i^^2)
        vDSP_zvmags(cplx, 1, mWorkBuffer, 1, mBandCount);

        // Convert magnitudes to dB: 10 * log10(mags[n] / nfft^^2)
        Float32 nfft_sq = mFFTSize * mFFTSize;
        vDSP_vdbcon(mWorkBuffer, 1, &nfft_sq, mWorkBuffer, 1, mBandCount, 0);

        // Set threshold: M = (M > T) ? M : T
        vDSP_vthr(mWorkBuffer, 1, &mThreshold, mWorkBuffer, 1, mBandCount);

        // Now have magnitudes in dB, just accumulate it
        vDSP_vadd(mWorkBuffer, 1, mMagnitudes, 1, mMagnitudes, 1, mBandCount);
    }

    if (mPhases()) {
        // Just use vDSP to calculate phase directly
        vDSP_zvphas(cplx, 1, mWorkBuffer, 1, mBandCount);
        vDSP_vadd(mWorkBuffer, 1, mPhases, 1, mPhases, 1, mBandCount);
    }

    mFFTCount++;
    if (mFFTCount >= mFFTsPerInterval) {
        float divisor = (float)mFFTCount;

        // Get averages
        vDSP_vsdiv(mMagnitudes, 1, &divisor, mMagnitudes, 1, mBandCount);
        vDSP_vsdiv(mPhases, 1, &divisor, mPhases, 1, mBandCount);

        // Update band data
        if (mBands) {
            mBands->UpdateBands(mBandCount, mMagnitudes, mPhases);
        }

        // Call our listener to dispatch the spectrum event
        if (mSpectrumCallbackProc) {
            double duration = (double)mSamplesPerInterval / (double)mAudioUnit->GetSampleRate();
            mSpectrumCallbackProc(mSpectrumCallbackContext, duration);
        }

        // Reset things
        vDSP_vclr(mMagnitudes, 1, mBandCount);
        vDSP_vclr(mPhases, 1, mBandCount);
        mFFTCount = 0;
    }
}

void AVFAudioSpectrumUnit::SetupSpectralProcessor() {
    if (mSpectralCrunch) {
        delete mSpectralCrunch;
        mSpectralCrunch = NULL;

        mWorkBuffer.free();
        mMagnitudes.free();
        mPhases.free();
    }

    if (mEnabled && mBandCount > 0) {
        // inFFTSize = 2x number of bins (this is adjusted properly later)
        // inHopSize = the number of samples to increment per update, depends on
        //             how much oversampling we want
        // inNumChannels = number of audio channels, we mix down to 1 since FX
        //                 lamely only supports one channel spectrum output
        // inMaxFrames = maximum number of frames we should ever process at once
        //               this is not relevant to anything but how much memory
        //               the analyzer allocates up front
        mFFTSize = mBandCount * 2;
        mSpectralCrunch = new CASpectralProcessor(mFFTSize,
                                                  mFFTSize / kSpectrumOversampleFactor,
                                                  1,
                                                  mAudioUnit->GetMaxFramesPerSlice());

        // Set up a Hamming window to match GStreamer
        vDSP_hamm_window(mSpectralCrunch->Window(), mFFTSize, vDSP_HALF_WINDOW);

        mSpectralCrunch->SetSpectralFunction(AVFAudioSpectrum_SpectralFunction, this);

        // Allocate mag/phase buffers and calculate FFT count per iteration
        mWorkBuffer.alloc(mBandCount);

        mMagnitudes.alloc(mBandCount);
        vDSP_vclr(mMagnitudes(), 1, mBandCount);

        mPhases.alloc(mBandCount);
        vDSP_vclr(mPhases(), 1, mBandCount);

        mSamplesPerInterval = (UInt32)(mAudioUnit->GetSampleRate() * mUpdateInterval);
        
        // Clamp FFTs per interval to an integral number
        mFFTCount = 0;
        mFFTsPerInterval = mSamplesPerInterval / mFFTSize * kSpectrumOversampleFactor;

        // Recalculate mSamplesPerInterval so we report duration correctly
        mSamplesPerInterval = mFFTsPerInterval / kSpectrumOversampleFactor * mFFTSize;
    } // else leave disabled

    mRebuildCrunch = false;
}

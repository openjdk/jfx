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

#ifndef __JFXMedia__AVFAudioSpectrumUnit__
#define __JFXMedia__AVFAudioSpectrumUnit__

#include <AudioUnit/AudioUnit.h>

#include <pthread.h>
#include <memory>

#include "PipelineManagement/AudioSpectrum.h"

#include <gst/gst.h>
#include <gstspectrum.h>

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

class AVFAudioSpectrumUnit : public CAudioSpectrum {
public:
    AVFAudioSpectrumUnit();
    virtual ~AVFAudioSpectrumUnit();

    // We'll use ProcessBufferLists as it sends all channels at once instead
    // of individual channels
    bool ProcessBufferLists(const AudioBufferList& inBuffer,
                            UInt32 inFramesToProcess);

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

    void SetSampleRate(UInt32 rate);
    void SetChannels(UInt32 count);
    void SetMaxFrames(UInt32 maxFrames);
    void SetSpectrumCallbackProc(AVFSpectrumUnitCallbackProc proc, void *context);

private:
    AVFSpectrumUnitCallbackProc mSpectrumCallbackProc;
    void *mSpectrumCallbackContext;
    bool mEnabled;

    pthread_mutex_t mBandLock;      // prevent bands from disappearing while we're processing
    int mBandCount;
    CBandsHolder *mBands;
    double mUpdateInterval;
    Float32 mThreshold;

    AudioBufferList mMixBuffer;
    int mMixBufferFrameCapacity;    // number of frames that can currently be stored in mix buffer

    // Audio parameters
    UInt32 mSampleRate;
    UInt32 mChannels;
    UInt32 mMaxFrames;
    UInt32 mSamplesPerInterval;

    bool mRebuildCrunch;

    // GStreamer
    GstElement *mSpectrumElement;
    GstSpectrum *mSpectrum;

    void lockBands() {
        pthread_mutex_lock(&mBandLock);
    }

    void unlockBands() {
        pthread_mutex_unlock(&mBandLock);
    }

    void SetupSpectralProcessor();
    void ReleaseSpectralProcessor();
};

typedef std::shared_ptr<AVFAudioSpectrumUnit> AVFAudioSpectrumUnitPtr;

#endif /* defined(__JFXMedia__AVFAudioSpectrumUnit__) */

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

#include "AVFAudioSpectrumUnit.h"

#include <iostream>
#include <Accelerate/Accelerate.h>

AVFAudioSpectrumUnit::AVFAudioSpectrumUnit() : mSpectrumCallbackProc(NULL),
                                               mSpectrumCallbackContext(NULL),
                                               mEnabled(true),
                                               mBandCount(128),
                                               mBands(NULL),
                                               mUpdateInterval(kDefaultAudioSpectrumUpdateInterval),
                                               mThreshold(kDefaultAudioSpectrumThreshold),
                                               mMixBufferFrameCapacity(0),
                                               mSampleRate(0),
                                               mChannels(0),
                                               mMaxFrames(0),
                                               mSamplesPerInterval(0),
                                               mRebuildCrunch(true),
                                               mSpectrumElement(NULL),
                                               mSpectrum(NULL) {
    mMixBuffer.mNumberBuffers = 1;
    mMixBuffer.mBuffers[0].mData = NULL;

    pthread_mutex_init(&mBandLock, NULL);

    gst_init_check(NULL, NULL, NULL);
}

AVFAudioSpectrumUnit::~AVFAudioSpectrumUnit() {
    if (mMixBuffer.mBuffers[0].mData) {
        free(mMixBuffer.mBuffers[0].mData);
        mMixBuffer.mBuffers[0].mData = NULL;
    }

    ReleaseSpectralProcessor();
}

bool AVFAudioSpectrumUnit::ProcessBufferLists(const AudioBufferList& inBuffer,
                                                  UInt32 inFramesToProcess) {
    if (!mEnabled) {
        return true;
    }

    // (Re)allocate mix buffer if needed
    if (!mMixBuffer.mBuffers[0].mData || mMixBufferFrameCapacity < inFramesToProcess) {
        // allocate buffer list (only need to do this once)
        if (mMixBuffer.mBuffers[0].mData) {
            free(mMixBuffer.mBuffers[0].mData);
            mMixBuffer.mBuffers[0].mData = NULL;
        }

        mMixBufferFrameCapacity = mMaxFrames;

        mMixBuffer.mBuffers[0].mNumberChannels = 1;
        mMixBuffer.mBuffers[0].mData = calloc(mMixBufferFrameCapacity, sizeof (Float32));
        mMixBuffer.mBuffers[0].mDataByteSize = 0; // size of actual contained data, not size of buffer
    }

    if (mRebuildCrunch) {
        SetupSpectralProcessor();
    }

    if (mSpectrum != NULL) {
        // Mix the audio into one channel since JavaFX only supports single channel spectrum
        // Just use an arithmetic average, nothing fancy here
        float *buffer = (float*) mMixBuffer.mBuffers[0].mData;
        vDSP_vclr(buffer, 1, mMixBufferFrameCapacity);
        for (int ii = 0; ii < inBuffer.mNumberBuffers; ii++) {
            vDSP_vadd((float*) inBuffer.mBuffers[ii].mData, 1,
                    buffer, 1,
                    buffer, 1, inFramesToProcess);
        }
        float divisor = (float) inBuffer.mNumberBuffers;
        vDSP_vsdiv(buffer, 1,
                &divisor,
                buffer, 1, inFramesToProcess);
        mMixBuffer.mBuffers[0].mDataByteSize = inFramesToProcess * sizeof (Float32);

        // Just reuse already allocated memory from mMixBuffer and do not free it
        // in GStreamer
        GstBuffer *gstBuffer =
                gst_buffer_new_wrapped_full(GST_MEMORY_FLAG_READONLY, // Allow only reading
                mMixBuffer.mBuffers[0].mData,
                mMixBuffer.mBuffers[0].mDataByteSize,
                0,
                mMixBuffer.mBuffers[0].mDataByteSize,
                NULL,
                NULL); // No need to free memory
        if (gstBuffer == NULL) {
            return false;
        }

        GstFlowReturn result = gst_spectrum_transform_ip_api((GstBaseTransform *) mSpectrum, gstBuffer);
        if (result != GST_FLOW_OK) {
            return false;
        }
        gst_buffer_unref(gstBuffer);
    }

    return true;
}

bool AVFAudioSpectrumUnit::IsEnabled() {
    return mEnabled;
}

void AVFAudioSpectrumUnit::SetEnabled(bool isEnabled) {
    mEnabled = isEnabled;
    mRebuildCrunch = true;
}

void AVFAudioSpectrumUnit::SetBands(int bands, CBandsHolder* holder) {
    lockBands();
    if (mBands) {
        CBandsHolder::ReleaseRef(mBands);
        mBands = NULL;
    }
    mBandCount = 0;
    if (holder) {
        mBands = CBandsHolder::AddRef(holder);
        if (mBands) {
            mBandCount = bands;
        }
    }
    mRebuildCrunch = true;
    unlockBands();
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
    return (int) mThreshold;
}

void AVFAudioSpectrumUnit::SetThreshold(int threshold) {
    if (mThreshold != (Float32) threshold) {
        mThreshold = (Float32) threshold;
        mRebuildCrunch = true;
    }
}

void AVFAudioSpectrumUnit::UpdateBands(int size, const float* magnitudes, const float* phases) {
    // lock now otherwise the bands could change while we're processing
    lockBands();
    if (!mBands || size <= 0 || !mEnabled) {
        unlockBands();
        return;
    }

    // Update band data
    mBands->UpdateBands(size, magnitudes, magnitudes);

    // Call our listener to dispatch the spectrum event
    if (mSpectrumCallbackProc) {
        double duration = (double) mSamplesPerInterval / (double) 44100;
        mSpectrumCallbackProc(mSpectrumCallbackContext, duration);
    }

    unlockBands();
}

void AVFAudioSpectrumUnit::SetSampleRate(UInt32 rate) {
    mSampleRate = rate;
}

void AVFAudioSpectrumUnit::SetChannels(UInt32 count) {
    mChannels = count;
}

void AVFAudioSpectrumUnit::SetMaxFrames(UInt32 maxFrames) {
    mMaxFrames = maxFrames;
}

void AVFAudioSpectrumUnit::SetSpectrumCallbackProc(AVFSpectrumUnitCallbackProc proc, void *context) {
    mSpectrumCallbackProc = proc;
    mSpectrumCallbackContext = context;
}

static gboolean PostMessageCallback(GstElement * element, GstMessage * message) {
    if (message == NULL) {
        return FALSE;
    }

    GstSpectrum *pSpectrum = GST_SPECTRUM(element);
    if (pSpectrum == NULL || pSpectrum->user_data == NULL) {
        return FALSE;
    }

    AVFAudioSpectrumUnit *pSpectrumUnit = (AVFAudioSpectrumUnit*)pSpectrum->user_data;

    const GstStructure *pStr = gst_message_get_structure(message);
    if (gst_structure_has_name(pStr, "spectrum")) {
        GstClockTime timestamp, duration;

        if (!gst_structure_get_clock_time(pStr, "timestamp", &timestamp))
            timestamp = GST_CLOCK_TIME_NONE;

        if (!gst_structure_get_clock_time(pStr, "duration", &duration))
            duration = GST_CLOCK_TIME_NONE;

        size_t bandsNum = pSpectrumUnit->GetBands();

        if (bandsNum > 0) {
            float *magnitudes = new float[bandsNum];
            float *phases = new float[bandsNum];

            const GValue *magnitudes_value = gst_structure_get_value(pStr, "magnitude");
            const GValue *phases_value = gst_structure_get_value(pStr, "phase");
            for (int i = 0; i < bandsNum; i++) {
                magnitudes[i] = g_value_get_float(gst_value_list_get_value(magnitudes_value, i));
                phases[i] = g_value_get_float(gst_value_list_get_value(phases_value, i));
            }
            pSpectrumUnit->UpdateBands((int) bandsNum, magnitudes, phases);

            delete [] magnitudes;
            delete [] phases;
        }
    }

    gst_message_unref(message);

    return TRUE;
}

void AVFAudioSpectrumUnit::SetupSpectralProcessor() {
    ReleaseSpectralProcessor();

    lockBands();

    mSpectrumElement = gst_element_factory_make("spectrum", NULL);
    mSpectrum = GST_SPECTRUM(mSpectrumElement);
    mSpectrum->user_data = (void*)this;

    // Set our own callback for post message
    GstElementClass *klass;
    klass = GST_ELEMENT_GET_CLASS(mSpectrumElement);
    klass->post_message = PostMessageCallback;

    // Configure spectrum element
    // Do send magnitude and phase information, off by default
    g_object_set(mSpectrumElement, "post-messages", TRUE,
                                   "message-magnitude", TRUE,
                                   "message-phase", TRUE, NULL);

    g_object_set(mSpectrumElement, "bands", mBandCount, NULL);

    mSamplesPerInterval = (UInt32)(mSampleRate * mUpdateInterval);
    guint64 value = (guint64) (mUpdateInterval * GST_SECOND);
    g_object_set(mSpectrumElement, "interval", value, NULL);

    g_object_set(mSpectrumElement, "threshold", (int) mThreshold, NULL);

    // Since we do not run spectrum element in pipeline and it will not get configured
    // correctly, we need to set required information directly.
    GST_AUDIO_FILTER_RATE(mSpectrum) = mSampleRate;
    GST_AUDIO_FILTER_CHANNELS(mSpectrum) = 1; // Always 1 channel

    // gst_spectrum_setup()
    GstAudioInfo *info = gst_audio_info_new();
    gst_audio_info_init(info);
    gst_audio_info_set_format(info, GST_AUDIO_FORMAT_F32, mSampleRate, 1, NULL);
    // bps = 4 bytes - 32-bit float, bpf = 4 bytes - 32-bit float mono
    gst_spectrum_setup_api((GstAudioFilter*) mSpectrum, info, 4, 4);
    gst_audio_info_free(info);

    // Set element to playing state
    gst_element_set_state(mSpectrumElement, GST_STATE_PLAYING);

    mRebuildCrunch = false;
    unlockBands();
}

void AVFAudioSpectrumUnit::ReleaseSpectralProcessor() {
    lockBands();

    if (mSpectrumElement) {
        gst_element_set_state(mSpectrumElement, GST_STATE_NULL);
        gst_object_unref(GST_OBJECT(mSpectrumElement));
        mSpectrumElement = NULL;
        mSpectrum = NULL;
    }

    unlockBands();
}
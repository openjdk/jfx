/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "AVFSoundLevelUnit.h"
#include <Accelerate/Accelerate.h>

AVFSoundLevelUnit::AVFSoundLevelUnit() :
    mVolume(kDefaultSoundLevelParam_Volume),
    mBalance(kDefaultSoundLevelParam_Balance),
    mChannels(0) {
}

AVFSoundLevelUnit::~AVFSoundLevelUnit() {
}

Float32 AVFSoundLevelUnit::volume() {
    return mVolume;
}

void AVFSoundLevelUnit::setVolume(Float32 volume) {
    if (volume < 0.0) {
        volume = 0.0;
    } else if (volume > 1.0) {
        volume = 1.0;
    }
    mVolume = volume;
}

Float32 AVFSoundLevelUnit::balance() {
    return mBalance;
}

void AVFSoundLevelUnit::setBalance(Float32 balance) {
    if (balance < -1.0) {
        balance = -1.0;
    } else if (balance > 1.0) {
        balance = 1.0;
    }
    mBalance = balance;
}

void AVFSoundLevelUnit::SetChannels(UInt32 count) {
    mChannels = count;
}

// For stereo (2 channel), channel 0 is left, channel 1 is right
Float32 AVFSoundLevelUnit::CalculateChannelLevel(int channelNum, int channelCount) {
    Float32 volume = mVolume;
    Float32 balance = mBalance;
    Float32 level = volume;

    if (channelCount == 2) {
        // balance is only done on stereo audio
        if (((balance < 0.0) && channelNum == 1) ||
                ((balance > 0.0) && channelNum == 0)) {
            // attenuate according to balance
            balance = 1.0 - fabsf(balance);
            level *= balance; // invert so it ramps the right direction
        }
    }
    return level;
}

bool AVFSoundLevelUnit::ProcessBufferLists(const AudioBufferList & buffer,
                                           UInt32 inFramesToProcess) {
    for (UInt32 i = 0; i < buffer.mNumberBuffers; i++) {
        Process((const Float32 *) buffer.mBuffers[i].mData,
                (Float32 *) buffer.mBuffers[i].mData,
                inFramesToProcess,
                i,
                buffer.mBuffers[i].mNumberChannels);
    }

    return true;
}

void AVFSoundLevelUnit::Process(const Float32 *inSourceP,
        Float32 *inDestP,
        UInt32 inFramesToProcess,
        UInt32 channelNum,
        UInt32 inNumChannels) {

    Float32 level = CalculateChannelLevel(channelNum, mChannels);
    if (level == 1.0f) {
        // Unity volume and balance
        // if we're processing in-place then no need to do anything
        if (inDestP != inSourceP) {
            // There's no vector copy for non-complex numbers, so we'll just add zero
            // We could just do memcpy, but if the channels are interleaved we don't
            // want to modify other channels
            Float32 addend = 0;
            // float* casts are needed for Xcode 4.5
            vDSP_vsadd((float*) inSourceP, inNumChannels,
                    &addend,
                    (float*) inDestP, inNumChannels,
                    inFramesToProcess);
        }
    } else if (level == 0.0) {
        // Just zero out the channel
        vDSP_vclr(inDestP, inNumChannels, inFramesToProcess);
    } else {
        // Just multiply vector inSourceP by scalar volume, storing in vector inDestP
        // we only attenuate the signal, so we don't need to be concerned about clipping
        vDSP_vsmul(inSourceP,
                inNumChannels,
                &level,
                inDestP,
                inNumChannels,
                inFramesToProcess);
    }
}

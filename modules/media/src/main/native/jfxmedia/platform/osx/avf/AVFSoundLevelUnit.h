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

#ifndef __JFXMedia__AVFSoundLevelUnit__
#define __JFXMedia__AVFSoundLevelUnit__

#include <AudioUnit/AudioUnit.h>
#include <Accelerate/Accelerate.h>
#include "AVFKernelProcessor.h"
#include "AUEffectBase.h"

#define kDefaultSoundLevelParam_Volume 1.0f
#define kDefaultSoundLevelParam_Balance 0.0f

/*
 * This unit applies the volume/balance controls.
 */
class AVFSoundLevelUnit : public AVFKernelProcessor {
public:
    AVFSoundLevelUnit() :
        AVFKernelProcessor(),
        mVolume(kDefaultSoundLevelParam_Volume),
        mBalance(kDefaultSoundLevelParam_Balance)
    {}

    virtual ~AVFSoundLevelUnit() {}
    
    virtual AUKernelBase *NewKernel();

    Float32 volume() {
        return mVolume;
    }
    void setVolume(Float32 volume) {
        if (volume < 0.0) {
            volume = 0.0;
        } else if (volume > 1.0) {
            volume = 1.0;
        }
        mVolume = volume;
    }

    Float32 balance() {
        return mBalance;
    }
    void setBalance(Float32 balance) {
        if (balance < -1.0) {
            balance = -1.0;
        } else if (balance > 1.0) {
            balance = 1.0;
        }
        mBalance = balance;
    }

    // For stereo (2 channel), channel 0 is left, channel 1 is right
    Float32 CalculateChannelLevel(int channelNum, int channelCount) {
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

private:
    Float32 mVolume;
    Float32 mBalance;
};

#endif /* defined(__JFXMedia__AVFSoundLevelUnit__) */

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

#ifndef __JFXMedia__AVFSoundLevelUnit__
#define __JFXMedia__AVFSoundLevelUnit__

#include <AudioUnit/AudioUnit.h>
#include <Accelerate/Accelerate.h>

#include <memory>

#define kDefaultSoundLevelParam_Volume 1.0f
#define kDefaultSoundLevelParam_Balance 0.0f

/*
 * This unit applies the volume/balance controls.
 */
class AVFSoundLevelUnit {
public:
    AVFSoundLevelUnit();

    ~AVFSoundLevelUnit();

    Float32 volume();
    void setVolume(Float32 volume);
    Float32 balance();
    void setBalance(Float32 balance);
    void SetChannels(UInt32 count);
    Float32 CalculateChannelLevel(int channelNum, int channelCount);
    bool ProcessBufferLists(const AudioBufferList & buffer,
                                UInt32 inFramesToProcess);
    void Process(const Float32 *inSourceP,
                 Float32 *inDestP,
                 UInt32 inFramesToProcess,
                 UInt32 channelNum,
                 UInt32 inNumChannels);

private:
    Float32 mVolume;
    Float32 mBalance;
    UInt32 mChannels;
};

typedef std::shared_ptr<AVFSoundLevelUnit> AVFSoundLevelUnitPtr;

#endif /* defined(__JFXMedia__AVFSoundLevelUnit__) */

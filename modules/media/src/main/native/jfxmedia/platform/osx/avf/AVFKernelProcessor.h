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

#ifndef __JFXMedia__AVFKernelProcessor__
#define __JFXMedia__AVFKernelProcessor__

#include "AUEffectBase.h"


/*
 * Instead of writing N components that all basically function the same (at the
 * component level), we'll skip the complexity and just write one component that
 * handles everything by use of a abstract base class.
 */

/*
 * Abstract base class used by the common AudioUnit to manage kernels. All
 * processing states must be managed in this class, rather than using properties
 * or parameters of the AudioUnit (since this is all private and in-process)
 */
class AVFKernelProcessor {
public:
    AVFKernelProcessor() :
        mAudioUnit(NULL)
    {}

    virtual ~AVFKernelProcessor() {}

    // This is used internally, do not call directly
    virtual void SetAudioUnit(AUEffectBase *audioUnit) {
        mAudioUnit = audioUnit;
    }

    virtual void Reset() {}

    /*
     * Create a new processing kernel. This is called by the AudioUnit to create
     * a kernel that will be called to process audio data. The audioUnit parameter
     * is supplied by the component, generally you do not have to do anything with
     * it, but it's needed for the AUKernelBase class.
     */
    virtual AUKernelBase *NewKernel() {
        return NULL;
    }

    virtual OSStatus ProcessBufferLists(AudioUnitRenderActionFlags& ioActionFlags,
                                        const AudioBufferList& inBuffer,
                                        AudioBufferList& outBuffer,
                                        UInt32 inFramesToProcess) {
        return noErr;
    }


    virtual void StreamFormatChanged(const CAStreamBasicDescription &newFormat) {}

protected:
    AUEffectBase *mAudioUnit;
};

/*
 * Returns an instance of our common AudioUnit set up to use the given kernel
 */
extern AudioUnit NewKernelProcessorUnit(AVFKernelProcessor *kernel);

#endif /* defined(__JFXMedia__AVFKernelProcessor__) */

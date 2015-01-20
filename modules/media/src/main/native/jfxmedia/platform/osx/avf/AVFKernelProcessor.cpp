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

#include "AVFKernelProcessor.h"

#include <pthread.h>

// Apple reserves 0-1024 for their own properties
#define kAVFProperty_KernelProcessor 2099

class AVFKernelComponent : AUEffectBase {
public:
    AVFKernelComponent(AudioComponentInstance audioUnit, bool inProcessesInPlace = true)
        : AUEffectBase(audioUnit, inProcessesInPlace),
        mUsesKernel(false),
        mProcessor(NULL)
    {}

    virtual ~AVFKernelComponent() {
        // Don't do anything with mProcessor as it could have been deleted already
    }

    virtual AUKernelBase *NewKernel() {
        if (mProcessor) {
            AUKernelBase *kernel = mProcessor->NewKernel();
            mUsesKernel = (kernel != NULL);
            return kernel;
        }
        mUsesKernel = false;
        return NULL;
    }

	virtual	OSStatus ChangeStreamFormat(AudioUnitScope inScope,
                                        AudioUnitElement inElement,
                                        const CAStreamBasicDescription &inPrevFormat,
                                        const CAStreamBasicDescription &inNewFormat) {
        OSStatus status = this->AUEffectBase::ChangeStreamFormat(inScope,
                                                                 inElement,
                                                                 inPrevFormat,
                                                                 inNewFormat);
        if (inScope == kAudioUnitScope_Input && inElement == 0) {
            mProcessor->StreamFormatChanged(inNewFormat);
        }
        return status;
    }

    virtual OSStatus ProcessBufferLists(AudioUnitRenderActionFlags& ioActionFlags,
                                        const AudioBufferList& inBuffer,
                                        AudioBufferList& outBuffer,
                                        UInt32 inFramesToProcess) {
        // Call our base class if we have kernels
        if (mUsesKernel) {
            return this->AUEffectBase::ProcessBufferLists(ioActionFlags,
                                                          inBuffer,
                                                          outBuffer,
                                                          inFramesToProcess);
        }
        // Otherwise call ProcessBufferLists
        if (mProcessor) {
            return mProcessor->ProcessBufferLists(ioActionFlags,
                                                  inBuffer,
                                                  outBuffer,
                                                  inFramesToProcess);
        }
        return noErr; // ??? just in case
    }

	virtual OSStatus SetProperty(AudioUnitPropertyID inID,
                                 AudioUnitScope inScope,
                                 AudioUnitElement inElement,
                                 const void *inData,
                                 UInt32 inDataSize) {
        if (inID == kAVFProperty_KernelProcessor &&
            inScope == kAudioUnitScope_Global &&
            inElement == 0) {
            if (inDataSize == sizeof(AVFKernelProcessor*)) {
                AVFKernelProcessor *processor = *((AVFKernelProcessor **)inData);
                if (mProcessor != processor) {
                    if (mProcessor) {
                        mProcessor->SetAudioUnit(NULL);
                        mProcessor = NULL;
                    }
                    
                    mProcessor = processor;
                    if (mProcessor) {
                        mProcessor->SetAudioUnit(this);
                        const AudioStreamBasicDescription& format =
                                GetStreamFormat(kAudioUnitScope_Input, 0);
                        mProcessor->StreamFormatChanged(format);
                    }
                }
                return noErr;
            }
            return kAudioUnitErr_InvalidPropertyValue;
        }
        return this->AUEffectBase::SetProperty(inID, inScope, inElement, inData, inDataSize);
    }
private:
    bool mUsesKernel;
    AVFKernelProcessor *mProcessor;
};

// Synchronize registration of the component
volatile AudioComponent gAVFComponent = NULL;
pthread_mutex_t gAVFComponentLock = PTHREAD_MUTEX_INITIALIZER;

static inline AudioComponent GetAVFComponent() {
    AudioComponent component = NULL;
    pthread_mutex_lock(&gAVFComponentLock);

    if (!gAVFComponent) {
        gAVFComponent = AUBaseFactory<AVFKernelComponent>::Register(kAudioUnitType_Effect,
                                                                    'Krnl',
                                                                    'JAVA',
                                                                    CFSTR("JavaFX Kernel Processor"),
                                                                    0x00010000);
    }
    component = gAVFComponent;

    pthread_mutex_unlock(&gAVFComponentLock);
    return component;
}


AudioUnit NewKernelProcessorUnit(AVFKernelProcessor *kernel) {
    OSStatus status = noErr;
    AudioUnit unit = NULL;
    AudioComponent ac = GetAVFComponent();

    if (!ac) {
        return NULL;
    }

    status = AudioComponentInstanceNew(ac, &unit);
    if (noErr == status) {
        status = AudioUnitSetProperty(unit,
                                      kAVFProperty_KernelProcessor,
                                      kAudioUnitScope_Global,
                                      0,
                                      &kernel,
                                      sizeof(AVFKernelProcessor**));
    }

    if (noErr != status) {
        if (unit) {
            AudioComponentInstanceDispose(unit);
        }
    }
    return unit;
}

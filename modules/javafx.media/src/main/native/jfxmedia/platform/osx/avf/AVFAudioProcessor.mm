/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

#import "AVFAudioProcessor.h"
#import "AVFMediaPlayer.h"

#import <AVFoundation/AVFoundation.h>
#import <MediaToolbox/MediaToolbox.h>

#import "AVFKernelProcessor.h"
#import <CoreFoundation/CoreFoundation.h>

#import <pthread.h>
#import <objc/message.h>

static void InitAudioTap(MTAudioProcessingTapRef tapRef, void *clientInfo, void **tapStorageOut);
static void FinalizeAudioTap(MTAudioProcessingTapRef tapRef);
static void PrepareAudioTap(MTAudioProcessingTapRef tapRef,
                            CMItemCount maxFrames,
                            const AudioStreamBasicDescription *processingFormat);
static void UnprepareAudioTap(MTAudioProcessingTapRef tapRef);
static void ProcessAudioTap(MTAudioProcessingTapRef tapRef, CMItemCount numberFrames,
                            MTAudioProcessingTapFlags flags,
                            AudioBufferList *bufferListInOut,
                            CMItemCount *numberFramesOut,
                            MTAudioProcessingTapFlags *flagsOut);

static OSStatus AVFTapRenderCallback(void *inRefCon,
                                     AudioUnitRenderActionFlags *ioActionFlags,
                                     const AudioTimeStamp *inTimeStamp,
                                     UInt32 inBusNumber,
                                     UInt32 inNumberFrames,
                                     AudioBufferList *ioData);

class AVFTapContext {
public:
    AVFTapContext(AVFSoundLevelUnitPtr slu, AVFAudioSpectrumUnitPtr spectrum, AVFAudioEqualizerPtr eq) :
        audioSLU(slu),
        audioSpectrum(spectrum),
        audioEQ(eq)
    {
    }

    ~AVFTapContext() {
        // AudioUnits have already been deallocated by now
        // shared_ptrs get freed automatically
    }

    AudioUnit spectrumUnit;
    AudioUnit volumeUnit;
    AudioUnit eqUnit;

    AudioUnit renderUnit; // the last unit in our chain
    CMItemCount totalFrames;
    
    // Hold on to these while we're running
    AVFSoundLevelUnitPtr audioSLU;
    AVFAudioSpectrumUnitPtr audioSpectrum;
    AVFAudioEqualizerPtr audioEQ;
};

@implementation AVFAudioProcessor

- (id) init {
    if ((self = [super init]) != nil) {
        _soundLevelUnit = AVFSoundLevelUnitPtr(new AVFSoundLevelUnit());
        _audioSpectrum = AVFAudioSpectrumUnitPtr(new AVFAudioSpectrumUnit());
        _audioEqualizer = AVFAudioEqualizerPtr(new AVFAudioEqualizer());

        _volume = 1.0f;
        _balance = 0.0f;
        _audioDelay = 0LL;
    }
    return self;
}

- (void) dealloc {
    _soundLevelUnit = nullptr;
    _audioSpectrum = nullptr;
    _audioEqualizer = nullptr;
}

- (void) setAudioTrack:(AVAssetTrack *)track {
    if (track != _audioTrack) {
        // reset the audio mixer if it's already been created
        // this theoretically should never happen...
        _mixer = nil;
    }
    _audioTrack = track;
}

- (AVAudioMix*) mixer {
    if (!self.audioTrack) {
        return nil;
    }

    if (!_mixer) {
        AVMutableAudioMix *mixer = [AVMutableAudioMix audioMix];
        if (mixer) {
            AVMutableAudioMixInputParameters *audioMixInputParameters =
                [AVMutableAudioMixInputParameters audioMixInputParametersWithTrack:self.audioTrack];
            if (audioMixInputParameters &&
                [audioMixInputParameters respondsToSelector:@selector(setAudioTapProcessor:)]) {
                MTAudioProcessingTapCallbacks callbacks;

                callbacks.version = kMTAudioProcessingTapCallbacksVersion_0;
                callbacks.clientInfo = (__bridge void *)self;
                callbacks.init = InitAudioTap;
                callbacks.finalize = FinalizeAudioTap;
                callbacks.prepare = PrepareAudioTap;
                callbacks.unprepare = UnprepareAudioTap;
                callbacks.process = ProcessAudioTap;

                MTAudioProcessingTapRef audioProcessingTap;
                if (noErr == MTAudioProcessingTapCreate(kCFAllocatorDefault, &callbacks,
                                             kMTAudioProcessingTapCreationFlag_PreEffects,
                                             &audioProcessingTap))
                {
                    objc_msgSend(audioMixInputParameters,
                                 @selector(setAudioTapProcessor:),
                                 audioProcessingTap);

                    CFRelease(audioProcessingTap); // owned by the mixer now
                    mixer.inputParameters = @[audioMixInputParameters];

                    _mixer = mixer;
                }
            }
        }
    }
    return _mixer;
}

- (void) setVolume:(float)volume {
    _volume = volume;
    if (_soundLevelUnit != nullptr) {
        _soundLevelUnit->setVolume(volume);
    }
}

- (void) setBalance:(float)balance {
    _balance = balance;
    if (_soundLevelUnit != nullptr) {
        _soundLevelUnit->setBalance(balance);
    }
}

@end

void InitAudioTap(MTAudioProcessingTapRef tapRef, void *clientInfo, void **tapStorageOut)
{
    // retain the AU kernels so they don't get freed while we're running
    AVFAudioProcessor *processor = (__bridge AVFAudioProcessor *)clientInfo;
    if (processor) {
        AVFTapContext *context = new AVFTapContext(processor.soundLevelUnit,
                                                   processor.audioSpectrum,
                                                   processor.audioEqualizer);
        *tapStorageOut = context;
    }
}

void FinalizeAudioTap(MTAudioProcessingTapRef tapRef)
{
    AVFTapContext *context = (AVFTapContext*)MTAudioProcessingTapGetStorage(tapRef);

    if (context) {
        delete context;
    }
}

static OSStatus SetupAudioUnit(AudioUnit unit,
                               const AudioStreamBasicDescription *processingFormat,
                               UInt32 maxFrames) {
    OSStatus status = noErr;
    if (noErr == status) {
        status = AudioUnitSetProperty(unit,
                                      kAudioUnitProperty_StreamFormat,
                                      kAudioUnitScope_Input, 0,
                                      processingFormat, sizeof(AudioStreamBasicDescription));
    }
    if (noErr == status) {
        status = AudioUnitSetProperty(unit,
                                      kAudioUnitProperty_StreamFormat,
                                      kAudioUnitScope_Output, 0,
                                      processingFormat, sizeof(AudioStreamBasicDescription));
    }
    if (noErr == status) {
        status = AudioUnitSetProperty(unit,
                                      kAudioUnitProperty_MaximumFramesPerSlice,
                                      kAudioUnitScope_Global, 0,
                                      &maxFrames, sizeof(UInt32));
    }
    if (noErr == status) {
        status = AudioUnitInitialize(unit);
    }
    return status;
}

static OSStatus ConnectAudioUnits(AudioUnit source, AudioUnit sink) {
    AudioUnitConnection connection;
    connection.sourceAudioUnit = source;
    connection.sourceOutputNumber = 0;
    connection.destInputNumber = 0;
    return AudioUnitSetProperty(sink, kAudioUnitProperty_MakeConnection,
                                kAudioUnitScope_Input, 0,
                                &connection, sizeof(connection));
}

AudioUnit FindAudioUnit(OSType type, OSType subType, OSType manu) {
    AudioUnit audioUnit = NULL;

    AudioComponentDescription audioComponentDescription;
    audioComponentDescription.componentType = type;
    audioComponentDescription.componentSubType = subType;
    audioComponentDescription.componentManufacturer = manu;
    audioComponentDescription.componentFlags = 0;
    audioComponentDescription.componentFlagsMask = 0;

    AudioComponent audioComponent = AudioComponentFindNext(NULL, &audioComponentDescription);
    if (audioComponent) {
        AudioComponentInstanceNew(audioComponent, &audioUnit);
    }
    return audioUnit;
}

void PrepareAudioTap(MTAudioProcessingTapRef tapRef,
                     CMItemCount maxFrames,
                     const AudioStreamBasicDescription *processingFormat)
{
    AVFTapContext *context = (AVFTapContext*)MTAudioProcessingTapGetStorage(tapRef);

    // Validate the audio format before we enable the processor

    // Failures here should rarely, if ever, happen so leave the NSLogs in for
    // easier diagnosis in the field
    if (processingFormat->mFormatID != kAudioFormatLinearPCM) {
        NSLog(@"AVFAudioProcessor needs linear PCM");
        return;
    }

    // Use the convenient kAudioFormatFlagsNativeFloatPacked to check if we can
    // process the incoming audio
    if ((processingFormat->mFormatFlags & kAudioFormatFlagsNativeFloatPacked)
        != kAudioFormatFlagsNativeFloatPacked) {
        NSLog(@"AVFAudioProcessor needs native endian packed float samples!!");
        return;
    }

    // Get an instance of our sound level unit
    context->eqUnit = NULL;
    if (context->audioEQ != nullptr) {
        context->eqUnit = NewKernelProcessorUnit(static_pointer_cast<AVFKernelProcessor>(context->audioEQ));
        if (context->eqUnit) {
            OSStatus status = SetupAudioUnit(context->eqUnit,
                                             processingFormat,
                                             (UInt32)maxFrames);
            if (noErr != status) {
                NSLog(@"Error creating audio equalizer unit: %d", status);
                AudioComponentInstanceDispose(context->eqUnit);
                context->eqUnit = NULL;
            }
        }
    }

    context->spectrumUnit = NULL;
    if (context->audioSpectrum != nullptr) {
        context->spectrumUnit = NewKernelProcessorUnit(static_pointer_cast<AVFKernelProcessor>(context->audioSpectrum));
        if (context->spectrumUnit) {
            OSStatus status = SetupAudioUnit(context->spectrumUnit,
                                             processingFormat,
                                             (UInt32)maxFrames);
            if (noErr != status) {
                NSLog(@"Error creating audio spectrum unit: %d", status);
                AudioComponentInstanceDispose(context->spectrumUnit);
                context->spectrumUnit = NULL;
            }
        }
    }

    context->volumeUnit = NULL;
    if (context->audioSLU != nullptr) {
        context->volumeUnit = NewKernelProcessorUnit(static_pointer_cast<AVFKernelProcessor>(context->audioSLU));
        if (context->volumeUnit) {
            OSStatus status = SetupAudioUnit(context->volumeUnit,
                                             processingFormat,
                                             (UInt32)maxFrames);
            if (noErr != status) {
                NSLog(@"Error setting up Sound Level Unit: %d", status);
                AudioComponentInstanceDispose(context->volumeUnit);
                context->volumeUnit = NULL;
            }
        }
    }

    /*
     * Use AudioUnitConnections to build a processing graph
     * The last unit in the chain will be the unit we call to render, it will
     * pull through the graph until we get to the first, which will fetch samples
     * via the render proc we install.
     *
     * The graph will look like this:
     *    (render proc) -> eqUnit -> spectrumUnit -> volUnit
     *
     * This will allow the EQ settings to affect the spectrum output, but not
     * the volume or balance.
     */
    AudioUnit firstUnit = NULL;
    context->renderUnit = NULL;

    // Set initial settings
    if (context->eqUnit) {
        if (context->renderUnit) {
            ConnectAudioUnits(context->renderUnit, context->eqUnit);
        }
        context->renderUnit = context->eqUnit;
        if (!firstUnit) {
            firstUnit = context->eqUnit;
        }
    }
    if (context->spectrumUnit) {
        if (context->renderUnit) {
            ConnectAudioUnits(context->renderUnit, context->spectrumUnit);
        }
        context->renderUnit = context->spectrumUnit;
        if (!firstUnit) {
            firstUnit = context->spectrumUnit;
        }
    }
    if (context->volumeUnit) {
        if (context->renderUnit) {
            ConnectAudioUnits(context->renderUnit, context->volumeUnit);
        }
        context->renderUnit = context->volumeUnit;
        if (!firstUnit) {
            firstUnit = context->volumeUnit;
        }
    }

    // Set up a render callback on our first unit
    if (firstUnit) {
        AURenderCallbackStruct renderCB;
        renderCB.inputProc = (AURenderCallback)AVFTapRenderCallback;
        renderCB.inputProcRefCon = (void*)tapRef;
        AudioUnitSetProperty(firstUnit,
                             kAudioUnitProperty_SetRenderCallback,
                             kAudioUnitScope_Input, 0,
                             &renderCB, sizeof(renderCB));
    }
    context->totalFrames = 0;
}

void UnprepareAudioTap(MTAudioProcessingTapRef tapRef)
{
    AVFTapContext *context = (AVFTapContext*)MTAudioProcessingTapGetStorage(tapRef);
    context->renderUnit = NULL;

    if (context->spectrumUnit) {
        AudioUnitUninitialize(context->spectrumUnit);
        AudioComponentInstanceDispose(context->spectrumUnit);
        context->spectrumUnit = NULL;
    }
    if (context->volumeUnit) {
        AudioUnitUninitialize(context->volumeUnit);
        AudioComponentInstanceDispose(context->volumeUnit);
        context->volumeUnit = NULL;
    }
    if (context->eqUnit) {
        AudioUnitUninitialize(context->eqUnit);
        AudioComponentInstanceDispose(context->eqUnit);
        context->eqUnit = NULL;
    }
}

void ProcessAudioTap(MTAudioProcessingTapRef tapRef,
                     CMItemCount numberFrames,
                     uint32_t flags,
                     AudioBufferList *bufferListInOut,
                     CMItemCount *numberFramesOut,
                     uint32_t *flagsOut)
{
    AVFTapContext *context = (AVFTapContext*)MTAudioProcessingTapGetStorage(tapRef);
    OSStatus status = noErr;

    if (context->renderUnit) {
        AudioTimeStamp audioTimeStamp;
        audioTimeStamp.mSampleTime = context->totalFrames;
        audioTimeStamp.mFlags = kAudioTimeStampSampleTimeValid;

        status = AudioUnitRender(context->renderUnit,
                                 0,
                                 &audioTimeStamp,
                                 0,
                                 (UInt32)numberFrames,
                                 bufferListInOut);
        if (noErr != status) {
            return;
        }
        context->totalFrames += numberFrames;
        *numberFramesOut = numberFrames;
    } else {
        MTAudioProcessingTapGetSourceAudio(tapRef, numberFrames, bufferListInOut,
                                flagsOut, NULL, numberFramesOut);
    }
}

static OSStatus AVFTapRenderCallback(void *inRefCon,
                                     AudioUnitRenderActionFlags *ioActionFlags,
                                     const AudioTimeStamp *inTimeStamp,
                                     UInt32 inBusNumber,
                                     UInt32 inNumberFrames,
                                     AudioBufferList *ioData)
{
    MTAudioProcessingTapRef tapRef = static_cast<MTAudioProcessingTapRef>(inRefCon);
    return MTAudioProcessingTapGetSourceAudio(tapRef, inNumberFrames, ioData, NULL, NULL, NULL);
}

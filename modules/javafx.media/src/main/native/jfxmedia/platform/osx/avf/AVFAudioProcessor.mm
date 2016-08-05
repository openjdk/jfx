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

typedef struct AVFTapContext {
    BOOL enabled;
    AVFAudioProcessor *processor; // we want this object retained, so don't use __bridge to set this!

    AudioUnit delayUnit;
    AudioUnit spectrumUnit;
    AudioUnit volumeUnit;
    AudioUnit eqUnit;

    AudioUnit renderUnit; // the last unit in our chain
    CMItemCount totalFrames;
} AVFTapContext;

@implementation AVFAudioProcessor

- (id) initWithPlayer:(AVFMediaPlayer*)player assetTrack:(AVAssetTrack *)assetTrack {
    if ((self = [super init]) != nil) {
        _player = player;

        // Create a mixer for this asset track
        [self createMixerWithTrack:assetTrack];
        if (_mixer) {
            _player.playerItem.audioMix = _mixer;
        }

        _soundLevelUnit = new AVFSoundLevelUnit();
        _audioSpectrum = NULL;
        _audioEqualizer = NULL;
    }
    return self;
}

- (void) dealloc {
    if (_soundLevelUnit) {
        delete _soundLevelUnit;
        _soundLevelUnit = NULL;
    }

    // We don't own these objects
    _audioSpectrum = NULL;
    _audioEqualizer = NULL;
}

- (void) createMixerWithTrack:(AVAssetTrack*)audioTrack {
    if (!_mixer) {
        AVMutableAudioMix *mixer = [AVMutableAudioMix audioMix];
        if (mixer) {
            AVMutableAudioMixInputParameters *audioMixInputParameters =
                [AVMutableAudioMixInputParameters audioMixInputParametersWithTrack:audioTrack];
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
}

- (void) setVolume:(float)volume {
    _volume = volume;
    if (_soundLevelUnit) {
        _soundLevelUnit->setVolume(volume);
    }
}

- (void) setBalance:(float)balance {
    _balance = balance;
    if (_soundLevelUnit) {
        _soundLevelUnit->setBalance(balance);
    }
}

@end

void InitAudioTap(MTAudioProcessingTapRef tapRef, void *clientInfo, void **tapStorageOut)
{
    AVFAudioProcessor *processor = (__bridge AVFAudioProcessor*)clientInfo;

    AVFTapContext *context = (AVFTapContext*)calloc(1, sizeof(AVFTapContext));
    if (context) {
        context->enabled = NO;
            // processor should be retained, else we can crash when closing the media player
        context->processor = processor;
        *tapStorageOut = context;

        processor.tapStorage = context;
    }
}

void FinalizeAudioTap(MTAudioProcessingTapRef tapRef)
{
    AVFTapContext *context = (AVFTapContext*)MTAudioProcessingTapGetStorage(tapRef);

    if (context) {
        context->processor.tapStorage = NULL;
        context->processor = NULL;

        free(context);
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
    context->delayUnit = FindAudioUnit(kAudioUnitType_Effect,
                                       kAudioUnitSubType_SampleDelay,
                                       kAudioUnitManufacturer_Apple);
    if (context->delayUnit) {
        OSStatus status = SetupAudioUnit(context->delayUnit, processingFormat, (UInt32)maxFrames);
        if (noErr != status) {
            NSLog(@"Error setting up delay unit: %d", status);
            AudioComponentInstanceDispose(context->delayUnit);
            context->delayUnit = NULL;
        }
    }

    context->eqUnit = NULL;
    if (context->processor.audioEqualizer) {
        context->eqUnit = NewKernelProcessorUnit(context->processor.audioEqualizer);
        if (context->eqUnit) {
            OSStatus status = SetupAudioUnit(context->eqUnit,
                                             processingFormat,
                                             (UInt32)maxFrames);
            if (noErr != status) {
                NSLog(@"Error creating audio equalizer unit: %d", status);
                // Don't delete the instance, that will happen when we dispose the unit
                AudioComponentInstanceDispose(context->eqUnit);
                context->eqUnit = NULL;
            }
        }
    }

    context->spectrumUnit = NULL;
    if (context->processor.audioSpectrum) {
        context->spectrumUnit = NewKernelProcessorUnit(context->processor.audioSpectrum);
        if (context->spectrumUnit) {
            OSStatus status = SetupAudioUnit(context->spectrumUnit,
                                             processingFormat,
                                             (UInt32)maxFrames);
            if (noErr != status) {
                NSLog(@"Error creating audio spectrum unit: %d", status);
                // Don't delete the instance, that will happen when we dispose the unit
                AudioComponentInstanceDispose(context->spectrumUnit);
                context->spectrumUnit = NULL;
            }
        }
    }

    context->volumeUnit = NULL;
    if (context->processor.soundLevelUnit) {
        context->volumeUnit = NewKernelProcessorUnit(context->processor.soundLevelUnit);
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
     *    (render proc) -> delayUnit -> eqUnit -> spectrumUnit -> volUnit
     *
     * This will allow the EQ settings to affect the spectrum output, but not
     * the volume or balance.
     */
    AudioUnit firstUnit = NULL;
    context->renderUnit = NULL;

    // Set initial settings
    if (context->delayUnit) {
        if (context->renderUnit) {
            // Connect renderUnit output to this input
            ConnectAudioUnits(context->renderUnit, context->delayUnit);
        }
        context->renderUnit = context->delayUnit;
        if (!firstUnit) {
            firstUnit = context->delayUnit;
        }
    }
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
    context->enabled = YES;
}

void UnprepareAudioTap(MTAudioProcessingTapRef tapRef)
{
    AVFTapContext *context = (AVFTapContext*)MTAudioProcessingTapGetStorage(tapRef);
    context->enabled = NO;
    context->renderUnit = NULL;

    if (context->delayUnit) {
        AudioUnitUninitialize(context->delayUnit);
        AudioComponentInstanceDispose(context->delayUnit);
        context->delayUnit = NULL;
    }
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

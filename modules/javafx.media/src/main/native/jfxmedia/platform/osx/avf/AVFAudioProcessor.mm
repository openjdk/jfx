/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

-(void) dealloc {
    _soundLevelUnit = nullptr;
    _audioSpectrum = nullptr;
    _audioEqualizer = nullptr;
}

-(void) setAudioTrack : (AVAssetTrack *) track {
    if (track != _audioTrack) {
        // reset the audio mixer if it's already been created
        // this theoretically should never happen...
        _mixer = nil;
    }
    _audioTrack = track;
}

-(AVAudioMix*) mixer {
    if (!self.audioTrack) {
        return nil;
    }

    if (!_mixer) {
        AVMutableAudioMix *mixer = [AVMutableAudioMix audioMix];
        if (mixer) {
            AVMutableAudioMixInputParameters *audioMixInputParameters =
                    [AVMutableAudioMixInputParameters audioMixInputParametersWithTrack : self.audioTrack];
            if (audioMixInputParameters &&
                    [audioMixInputParameters respondsToSelector : @selector(setAudioTapProcessor :)]) {
                MTAudioProcessingTapCallbacks callbacks;

                callbacks.version = kMTAudioProcessingTapCallbacksVersion_0;
                callbacks.clientInfo = (__bridge void *) self;
                callbacks.init = InitAudioTap;
                callbacks.finalize = FinalizeAudioTap;
                callbacks.prepare = PrepareAudioTap;
                callbacks.unprepare = UnprepareAudioTap;
                callbacks.process = ProcessAudioTap;

                MTAudioProcessingTapRef audioProcessingTap;
                if (noErr == MTAudioProcessingTapCreate(kCFAllocatorDefault, &callbacks,
                        kMTAudioProcessingTapCreationFlag_PreEffects,
                        &audioProcessingTap)) {
                    [audioMixInputParameters setAudioTapProcessor:audioProcessingTap];

                    CFRelease(audioProcessingTap); // owned by the mixer now
                    mixer.inputParameters = @[audioMixInputParameters];

                    _mixer = mixer;
                }
            }
        }
    }
    return _mixer;
}

-(void) setVolume : (float) volume {
    _volume = volume;
    if (_soundLevelUnit != nullptr) {
        _soundLevelUnit->setVolume(volume);
    }
}

-(void) setBalance : (float) balance {
    _balance = balance;
    if (_soundLevelUnit != nullptr) {
        _soundLevelUnit->setBalance(balance);
    }
}

@end

AVFTapContext::AVFTapContext(AVFSoundLevelUnitPtr slu, AVFAudioSpectrumUnitPtr spectrum,
                             AVFAudioEqualizerPtr eq) : audioSLU(slu),
                                                        audioSpectrum(spectrum),
                                                        audioEQ(eq),
                                                        // Some reasonable defaults
                                                        mSampleRate(48000),
                                                        mChannels(2) {
}

AVFTapContext::~AVFTapContext() {
    // AudioUnits have already been deallocated by now
    // shared_ptrs get freed automatically
}

void InitAudioTap(MTAudioProcessingTapRef tapRef, void *clientInfo, void **tapStorageOut) {
    // retain the AU kernels so they don't get freed while we're running
    AVFAudioProcessor *processor = (__bridge AVFAudioProcessor *) clientInfo;
    if (processor) {
        AVFTapContext *context = new AVFTapContext(processor.soundLevelUnit,
                processor.audioSpectrum,
                processor.audioEqualizer);
        *tapStorageOut = context;
    }
}

void FinalizeAudioTap(MTAudioProcessingTapRef tapRef) {
    AVFTapContext *context = (AVFTapContext*) MTAudioProcessingTapGetStorage(tapRef);
    if (context) {
        delete context;
    }
}

void PrepareAudioTap(MTAudioProcessingTapRef tapRef,
        CMItemCount maxFrames,
        const AudioStreamBasicDescription *processingFormat) {
    AVFTapContext *context = (AVFTapContext*) MTAudioProcessingTapGetStorage(tapRef);

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

    context->mSampleRate = processingFormat->mSampleRate;
    context->mChannels = processingFormat->mChannelsPerFrame;
    context->mMaxFrames = maxFrames;

    // Configure audio equalizer
    if (context->audioEQ != nullptr) {
        context->audioEQ.get()->SetSampleRate(context->mSampleRate);
        context->audioEQ.get()->SetChannels(context->mChannels);
        context->audioEQ.get()->ResetBandParameters();
    }

    // Configure spectrum
    if (context->audioSpectrum != nullptr) {
        context->audioSpectrum.get()->SetSampleRate(context->mSampleRate);
        context->audioSpectrum.get()->SetChannels(context->mChannels);
        context->audioSpectrum.get()->SetMaxFrames(context->mMaxFrames);
    }

    if (context->audioSLU != nullptr) {
        context->audioSLU.get()->SetChannels(context->mChannels);
    }
}

void UnprepareAudioTap(MTAudioProcessingTapRef tapRef) {
    // We do not need it anymore
}

void ProcessAudioTap(MTAudioProcessingTapRef tapRef,
        CMItemCount numberFrames,
        uint32_t flags,
        AudioBufferList *bufferListInOut,
        CMItemCount *numberFramesOut,
        uint32_t *flagsOut) {
    AVFTapContext *context = (AVFTapContext*) MTAudioProcessingTapGetStorage(tapRef);
    OSStatus status = MTAudioProcessingTapGetSourceAudio(tapRef, numberFrames, bufferListInOut,
            flagsOut, NULL, numberFramesOut);
    if (status != noErr) {
        NSLog(@"MTAudioProcessingTapGetSourceAudio failed: %d", status);
        return;
    }

    if (context->audioEQ != nullptr) {
        if (!context->audioEQ.get()->ProcessBufferLists(*bufferListInOut, numberFrames)) {
            NSLog(@"audioEQ ProcessBufferLists() failed");
            return;
        }
    }

    if (context->audioSpectrum != nullptr) {
        if (!context->audioSpectrum.get()->ProcessBufferLists(*bufferListInOut, numberFrames)) {
            NSLog(@"audioSpectrum ProcessBufferLists() failed");
            return;
        }
    }

    if (context->audioSLU != nullptr) {
        if (!context->audioSLU.get()->ProcessBufferLists(*bufferListInOut, numberFrames)) {
            NSLog(@"audioSLU ProcessBufferLists() failed");
            return;
        }
    }
}

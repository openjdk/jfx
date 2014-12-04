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

#import "AVFMediaPlayer.h"
#import <objc/runtime.h>
#import "CVVideoFrame.h"

#import <PipelineManagement/NullAudioEqualizer.h>
#import <PipelineManagement/NullAudioSpectrum.h>

#import "AVFAudioProcessor.h"

// "borrowed" from green screen player on ADC
// These are used to reduce power consumption when there are no video frames
// to be rendered, which is generally A Good Thing
#define FREEWHEELING_PERIOD_IN_SECONDS 0.5
#define ADVANCE_INTERVAL_IN_SECONDS 0.1

// set to 1 to debug track information
#define DUMP_TRACK_INFO 0

// trick used by Apple in AVGreenScreenPlayer
// This avoids calling [NSString isEqualTo:@"..."]
// The actual value is meaningless, but needs to be unique
static void *AVFMediaPlayerItemStatusContext = &AVFMediaPlayerItemStatusContext;
static void *AVFMediaPlayerItemDurationContext = &AVFMediaPlayerItemDurationContext;
static void *AVFMediaPlayerItemTracksContext = &AVFMediaPlayerItemTracksContext;

#define FORCE_VO_FORMAT 0
#if FORCE_VO_FORMAT
// #define FORCED_VO_FORMAT kCVPixelFormatType_32BGRA
// #define FORCED_VO_FORMAT kCVPixelFormatType_422YpCbCr8
// #define FORCED_VO_FORMAT kCVPixelFormatType_420YpCbCr8Planar
 #define FORCED_VO_FORMAT kCVPixelFormatType_422YpCbCr8_yuvs // Unsupported, use to test fallback
#endif

// Apple really likes to output '2vuy', this should be the least expensive conversion
#define FALLBACK_VO_FORMAT kCVPixelFormatType_422YpCbCr8

@implementation AVFMediaPlayer

static void SpectrumCallbackProc(void *context, double duration);

static CVReturn displayLinkCallback(CVDisplayLinkRef displayLink,
                                    const CVTimeStamp *inNow,
                                    const CVTimeStamp *inOutputTime,
                                    CVOptionFlags flagsIn,
                                    CVOptionFlags *flagsOut,
                                    void *displayLinkContext);

+ (BOOL) playerAvailable {
    // Check if AVPlayerItemVideoOutput exists, if not we're running on 10.7 or
    // earlier and have to fall back on QTKit
    Class klass = objc_getClass("AVPlayerItemVideoOutput");
    return (klass != nil);
}

- (id) initWithURL:(NSURL *)source eventHandler:(CJavaPlayerEventDispatcher*)hdlr {
    if ((self = [super init]) != nil) {
        _audioSyncDelay = 0LL;
        _volume = 1.0f;
        _balance = 0.0f;

        previousWidth = -1;
        previousHeight = -1;
        previousPlayerState = kPlayerState_UNKNOWN;

        eventHandler = hdlr;

        self.movieURL = source;
        _buggyHLSSupport = NO;
        _hlsBugResetCount = 0;

        // Create our own work queue
        playerQueue = dispatch_queue_create(NULL, NULL);

        // Create the player
        _player = [[AVPlayer alloc] init];
        if (!_player) {
            return nil;
        }
        _player.volume = 1.0f;
        _player.muted = NO;

        _playerItem = [AVPlayerItem playerItemWithURL:_movieURL];
        if (!_playerItem) {
            return nil;
        }
        [_player replaceCurrentItemWithPlayerItem:_playerItem];

        // Set the player item end action to NONE since we'll handle it internally
        _player.actionAtItemEnd = AVPlayerActionAtItemEndNone;

        /*
         * AVPlayerItem notifications we could listen for:
         * 10.7 AVPlayerItemTimeJumpedNotification -> the item's current time has changed discontinuously
         * 10.7 AVPlayerItemDidPlayToEndTimeNotification -> item has played to its end time
         * 10.7 AVPlayerItemFailedToPlayToEndTimeNotification (userInfo = NSError) -> item has failed to play to its end time
         * 10.9 AVPlayerItemPlaybackStalledNotification -> media did not arrive in time to continue playback
         */
        playerObservers = [[NSMutableArray alloc] init];
        id<NSObject> observer;
        __weak AVFMediaPlayer *blockSelf = self; // retain cycle avoidance
        NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
        observer = [center addObserverForName:AVPlayerItemDidPlayToEndTimeNotification
                                       object:_playerItem
                                        queue:[NSOperationQueue mainQueue]
                                   usingBlock:^(NSNotification *note) {
                                       // promote FINISHED state...
                                       [blockSelf setPlayerState:kPlayerState_FINISHED];
                                   }];
        if (observer) {
            [playerObservers addObject:observer];
        }

        keyPathsObserved = [[NSMutableArray alloc] init];
        [self observeKeyPath:@"self.playerItem.status"
                 withContext:AVFMediaPlayerItemStatusContext];

        [self observeKeyPath:@"self.playerItem.duration"
                 withContext:AVFMediaPlayerItemDurationContext];

        [self observeKeyPath:@"self.playerItem.tracks"
                 withContext:AVFMediaPlayerItemTracksContext];


        [self setPlayerState:kPlayerState_UNKNOWN];

        // filled out later
        _videoFormat = nil;
        _lastHostTime = 0LL;

        // Don't create video output until we know we have video
        _playerOutput = nil;
        _displayLink = NULL;

        _audioSpectrum = new AVFAudioSpectrumUnit();
        _audioSpectrum->SetSpectrumCallbackProc(SpectrumCallbackProc, (__bridge void*)self);

        _audioEqualizer = new AVFAudioEqualizer();
    }
    return self;
}

- (void) dealloc {
    [self dispose];

    self.movieURL = nil;
    self.player = nil;
    self.playerItem = nil;
    self.playerOutput = nil;
    
    if (_audioSpectrum) {
        delete _audioSpectrum;
        _audioSpectrum = NULL;
    }

    if (_audioEqualizer) {
        delete _audioEqualizer;
        _audioEqualizer = NULL;
    }
}

- (CAudioSpectrum*) audioSpectrum {
    return _audioSpectrum;
}

- (CAudioEqualizer*) audioEqualizer {
    return _audioEqualizer;
}

- (void) observeKeyPath:(NSString*)keyPath withContext:(void*)context {
    [self addObserver:self forKeyPath:keyPath options:NSKeyValueObservingOptionNew context:context];
    [keyPathsObserved addObject:keyPath];
}

// If we get an unsupported pixel format in the video output, call this to
// force it to output our fallback format
- (void) setFallbackVideoFormat {
    AVPlayerItemVideoOutput *newOutput =
        [[AVPlayerItemVideoOutput alloc] initWithPixelBufferAttributes:
         @{(id)kCVPixelBufferPixelFormatTypeKey: @(FALLBACK_VO_FORMAT)}];

    if (newOutput) {
        CVDisplayLinkStop(_displayLink);
        [_playerItem removeOutput:_playerOutput];

        self.playerOutput = newOutput;
        [_playerOutput setDelegate:self queue:playerQueue];
        [_playerOutput requestNotificationOfMediaDataChangeWithAdvanceInterval:ADVANCE_INTERVAL_IN_SECONDS];
        [_playerItem addOutput:_playerOutput];
    }
}

- (void) createVideoOutput {
    @synchronized(self) {
        // Skip if already created
        if (!_playerOutput) {
            // Create the player video output
            // kCVPixelFormatType_32ARGB comes out inverted, so don't use it
            // '2vuy' -> kCVPixelFormatType_422YpCbCr8 -> YCbCr_422 (uses less CPU too)
            // kCVPixelFormatType_420YpCbCr8Planar
            _playerOutput = [[AVPlayerItemVideoOutput alloc] initWithPixelBufferAttributes:
#if FORCE_VO_FORMAT
                             @{(id)kCVPixelBufferPixelFormatTypeKey: @(FORCED_VO_FORMAT)}];
#else
                             @{}]; // let AVFoundation decide the format...
#endif
            if (!_playerOutput) {
                return;
            }
            _playerOutput.suppressesPlayerRendering = YES;
            
            // Set up the display link (do we need this??)
            // might need to create a display link context struct that retains us
            // rather than passing self as the context
            CVDisplayLinkCreateWithActiveCGDisplays(&_displayLink);
            CVDisplayLinkSetOutputCallback(_displayLink, displayLinkCallback, (__bridge void *)self);
            // Pause display link to conserve power
            CVDisplayLinkStop(_displayLink);

            // Set up playerOutput delegate
            [_playerOutput setDelegate:self queue:playerQueue];
            [_playerOutput requestNotificationOfMediaDataChangeWithAdvanceInterval:ADVANCE_INTERVAL_IN_SECONDS];

            [_playerItem addOutput:_playerOutput];
        }
    }
}

- (void) setPlayerState:(int)newState {
    if (newState != previousPlayerState) {
        // For now just send up to client
        eventHandler->SendPlayerStateEvent(newState, 0.0);
        previousPlayerState = newState;
    }
}

- (void) observeValueForKeyPath:(NSString *)keyPath
                       ofObject:(id)object
                         change:(NSDictionary *)change
                        context:(void *)context {
	if (context == AVFMediaPlayerItemStatusContext) {
        AVPlayerStatus status = (AVPlayerStatus)[[change objectForKey:NSKeyValueChangeNewKey] longValue];
        if (status == AVPlayerStatusReadyToPlay) {
            if (!_movieReady) {
                // Only send this once, though we'll receive notification a few times
                [self setPlayerState:kPlayerState_READY];
                _movieReady = true;
            }
        }
    } else if (context == AVFMediaPlayerItemDurationContext) {
        // send update duration event
        double duration = CMTimeGetSeconds(_playerItem.duration);
        eventHandler->SendDurationUpdateEvent(duration);
    } else if (context == AVFMediaPlayerItemTracksContext) {
        [self extractTrackInfo];
	} else {
		[super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
	}
}

- (double) currentTime
{
	return CMTimeGetSeconds([self.player currentTime]);
}

- (void) setCurrentTime:(double)time
{
	[self.player seekToTime:CMTimeMakeWithSeconds(time, 1)];
}

- (BOOL) mute {
    return self.player.muted;
}

- (void) setMute:(BOOL)state {
    self.player.muted = state;
}

- (void) setAudioSyncDelay:(int64_t)audioSyncDelay {
    _audioSyncDelay = audioSyncDelay;
    if (_audioProcessor) {
        _audioProcessor.audioDelay = audioSyncDelay;
    }
}

- (float) balance {
    return _balance;
}

- (void) setBalance:(float)balance {
    _balance = balance;
    if (_audioProcessor) {
        _audioProcessor.balance = balance;
    }
}

- (float) volume {
    return _volume;
}

- (void) setVolume:(float)volume {
    _volume = volume;
    if (_audioProcessor) {
        _audioProcessor.volume = volume;
    }
}

- (float) rate {
    return self.player.rate;
}

- (void) setRate:(float)rate {
    self.player.rate = rate;
}

- (double) duration {
    if (self.playerItem.status == AVPlayerItemStatusReadyToPlay) {
        return CMTimeGetSeconds(self.playerItem.duration);
    }
    return -1.0;
}

- (void) play {
    [self.player play];
    [self setPlayerState:kPlayerState_PLAYING];
}

- (void) pause {
    [self.player pause];
    [self setPlayerState:kPlayerState_PAUSED];
}

- (void) stop {
    [self.player pause];
    [self.player seekToTime:kCMTimeZero];
    [self setPlayerState:kPlayerState_STOPPED];
}

- (void) finish {
}

- (void) dispose {
    @synchronized(self) {
        if (!isDisposed) {
            [self setPlayerState:kPlayerState_HALTED];

            NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
            for (id<NSObject> observer in playerObservers) {
                [center removeObserver:observer];
            }

            for (NSString *keyPath in keyPathsObserved) {
                [self removeObserver:self forKeyPath:keyPath];
            }

            if (_displayLink) {
                CVDisplayLinkStop(_displayLink);
                CVDisplayLinkRelease(_displayLink);
                _displayLink = NULL;
            }
            isDisposed = YES;
        }
    }
}

#if DUMP_TRACK_INFO
static void append_log(NSMutableString *s, NSString *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    NSString *appString = [[NSString alloc] initWithFormat:fmt arguments:args];
    [s appendFormat:@"%@\n", appString];
    va_end(args);
}
#define TRACK_LOG(fmt, ...) append_log(trackLog, fmt, ##__VA_ARGS__)
#else
#define TRACK_LOG(...) {}
#endif

#define FOURCC_CHAR(f) ((f)&0xff)?(char)((f)&0xff):'?'

- (void) extractTrackInfo {
#if DUMP_TRACK_INFO
    NSMutableString *trackLog = [[NSMutableString alloc] initWithFormat:
                                 @"Parsing tracks for player item %@:\n",
                                 _playerItem];
#endif
    NSArray *tracks = self.playerItem.tracks;
    int videoIndex = 1;
    int audioIndex = 1;
    int textIndex = 1;
    BOOL createVideo = NO;

    for (AVPlayerItemTrack *trackObj in tracks) {
        AVAssetTrack *track = trackObj.assetTrack;
        NSString *type = track.mediaType;
        NSString *name = nil;
        NSString *lang = @"und";
        CTrack::Encoding encoding = CTrack::CUSTOM;
        FourCharCode fcc = 0;

        CMFormatDescriptionRef desc = NULL;
        NSArray *formatDescList = track.formatDescriptions;
        if (formatDescList && formatDescList.count > 0) {
            desc = (__bridge CMFormatDescriptionRef)[formatDescList objectAtIndex:0];
            if (!desc) {
                TRACK_LOG(@"Can't get format description, skipping track");
                continue;
            }
            fcc = CMFormatDescriptionGetMediaSubType(desc);
            switch (fcc) {
                case 'avc1':
                    encoding = CTrack::H264;
                    break;
                case kAudioFormatLinearPCM:
                    encoding = CTrack::PCM;
                    break;
                case kAudioFormatMPEG4AAC:
                    encoding = CTrack::AAC;
                    break;
                case kAudioFormatMPEGLayer1:
                case kAudioFormatMPEGLayer2:
                    encoding = CTrack::MPEG1AUDIO;
                    break;
                case kAudioFormatMPEGLayer3:
                    encoding = CTrack::MPEG1LAYER3;
                    break;
                default:
                    // Everything else will show up as custom
                    break;
            }
        }

        if (track.languageCode) {
            lang = track.languageCode;
        }

        TRACK_LOG(@"Track %d (%@)", index, track.mediaType);
        TRACK_LOG(@"  enabled: %s", track.enabled ? "YES" : "NO");
        TRACK_LOG(@"  track ID: %d", track.trackID);
        TRACK_LOG(@"  language code: %@ (%sprovided)", lang, track.languageCode ? "" : "NOT ");
        TRACK_LOG(@"  encoding (FourCC): '%c%c%c%c' (JFX encoding %d)",
                  FOURCC_CHAR(fcc>>24),
                  FOURCC_CHAR(fcc>>16),
                  FOURCC_CHAR(fcc>>8),
                  FOURCC_CHAR(fcc),
                  (int)encoding);

        // Tracks in AVFoundation don't have names, so we'll need to give them
        // sequential names based on their type, e.g., "Video Track 1"
        if ([type isEqualTo:AVMediaTypeVideo]) {
            int width = -1;
            int height = -1;
            float frameRate = -1.0;
            if ([track hasMediaCharacteristic:AVMediaCharacteristicVisual]) {
                width = (int)track.naturalSize.width;
                height = (int)track.naturalSize.height;
                frameRate = track.nominalFrameRate;
            }
            name = [NSString stringWithFormat:@"Video Track %d", videoIndex++];
            CVideoTrack *outTrack = new CVideoTrack((int64_t)track.trackID,
                                                   [name UTF8String],
                                                   encoding,
                                                   (bool)track.enabled,
                                                   width,
                                                   height,
                                                   frameRate,
                                                   false);

            TRACK_LOG(@"  track name: %@", name);
            TRACK_LOG(@"  video attributes:");
            TRACK_LOG(@"    width: %d", width);
            TRACK_LOG(@"    height: %d", height);
            TRACK_LOG(@"    frame rate: %2.2f", frameRate);
            
            eventHandler->SendVideoTrackEvent(outTrack);
            delete outTrack;

            // signal to create the video output when we're done
            createVideo = YES;
        } else if ([type isEqualTo:AVMediaTypeAudio]) {
            name = [NSString stringWithFormat:@"Audio Track %d", audioIndex++];
            TRACK_LOG(@"  track name: %@", name);

            // Create audio processor
            if (!_audioProcessor) {
                _audioProcessor = [[AVFAudioProcessor alloc] initWithPlayer:self
                                                                 assetTrack:track];
                _audioProcessor.volume = _volume;
                _audioProcessor.balance = _balance;
                // Make sure the players volume is set to 1.0
                self.player.volume = 1.0;

                // Set up EQ and spectrum
                _audioProcessor.audioSpectrum = _audioSpectrum;
                _audioProcessor.audioEqualizer = _audioEqualizer;
            }

            // We have to get the audio information from the format description
            const AudioStreamBasicDescription *asbd = CMAudioFormatDescriptionGetStreamBasicDescription(desc);
            size_t layoutSize;
            const AudioChannelLayout *layout = CMAudioFormatDescriptionGetChannelLayout(desc, &layoutSize);
            int channels = 2;
            int channelMask = CAudioTrack::FRONT_LEFT | CAudioTrack::FRONT_RIGHT;
            float sampleRate = 44100.0;

            TRACK_LOG(@"  audio attributes:");
            if (asbd) {
                sampleRate = (float)asbd->mSampleRate;
                TRACK_LOG(@"    sample rate: %2.2f", sampleRate);
            }
            if (layout) {
                channels = (int)AudioChannelLayoutTag_GetNumberOfChannels(layout->mChannelLayoutTag);

                TRACK_LOG(@"    channel count: %d", channels);
                TRACK_LOG(@"    channel mask: %02x", channelMask);
            }

            CAudioTrack *audioTrack = new CAudioTrack((int64_t)track.trackID,
                                   [name UTF8String],
                                   encoding,
                                   (bool)track.enabled,
                                   [lang UTF8String],
                                   channels, channelMask, sampleRate);
            eventHandler->SendAudioTrackEvent(audioTrack);
            delete audioTrack;
        } else if ([type isEqualTo:AVMediaTypeClosedCaption]) {
            name = [NSString stringWithFormat:@"Subtitle Track %d", textIndex++];
            TRACK_LOG(@"  track name: %@", name);
            CSubtitleTrack *subTrack = new CSubtitleTrack((int64_t)track.trackID,
                                                         [name UTF8String],
                                                         encoding,
                                                         (bool)track.enabled,
                                                         [lang UTF8String]);
            eventHandler->SendSubtitleTrackEvent(subTrack);
            delete subTrack;
        }
    }

#if DUMP_TRACK_INFO
    LOGGER_INFOMSG([trackLog UTF8String]);
#endif

    if (createVideo) {
        [self createVideoOutput];
    }
}

- (void) outputMediaDataWillChange:(AVPlayerItemOutput *)sender {
    _lastHostTime = CVGetCurrentHostTime();
    CVDisplayLinkStart(_displayLink);
    _hlsBugResetCount = 0;
}

- (void) outputSequenceWasFlushed:(AVPlayerItemOutput *)output {
    _hlsBugResetCount = 0;
    _lastHostTime = CVGetCurrentHostTime();
}

- (void) sendPixelBuffer:(CVPixelBufferRef)buf frameTime:(double)frameTime hostTime:(int64_t)hostTime {
    _lastHostTime = hostTime;
    CVVideoFrame *frame = NULL;
    try {
        frame = new CVVideoFrame(buf, frameTime, _lastHostTime);
    } catch (const char *message) {
        // Check if the video format is supported, if not try our fallback format
        OSType format = CVPixelBufferGetPixelFormatType(buf);
        if (!CVVideoFrame::IsFormatSupported(format)) {
            [self setFallbackVideoFormat];
            return;
        }
        // Can't use this frame, report an error and ignore it
        LOGGER_DEBUGMSG(message);
        return;
    }

    if (previousWidth < 0 || previousHeight < 0
        || previousWidth != frame->GetWidth() || previousHeight != frame->GetHeight())
    {
        // Send/Queue frame size changed event
        previousWidth = frame->GetWidth();
        previousHeight = frame->GetHeight();
        eventHandler->SendFrameSizeChangedEvent(previousWidth, previousHeight);
    }
    eventHandler->SendNewFrameEvent(frame);
}

- (void) sendSpectrumEventDuration:(double)duration {
    if (eventHandler) {
        double timestamp = self.currentTime;
        eventHandler->SendAudioSpectrumEvent(timestamp, duration);
    }
}

@end

static void SpectrumCallbackProc(void *context, double duration) {
    if (context) {
        AVFMediaPlayer *player = (__bridge AVFMediaPlayer*)context;
        [player sendSpectrumEventDuration:duration];
    }
}

static CVReturn displayLinkCallback(CVDisplayLinkRef displayLink, const CVTimeStamp *inNow, const CVTimeStamp *inOutputTime, CVOptionFlags flagsIn, CVOptionFlags *flagsOut, void *displayLinkContext)
{
	AVFMediaPlayer *self = (__bridge AVFMediaPlayer *)displayLinkContext;
	AVPlayerItemVideoOutput *playerItemVideoOutput = self.playerOutput;

	// The displayLink calls back at every vsync (screen refresh)
	// Compute itemTime for the next vsync
	CMTime outputItemTime = [playerItemVideoOutput itemTimeForCVTimeStamp:*inOutputTime];
    if ([playerItemVideoOutput hasNewPixelBufferForItemTime:outputItemTime]) {
        CVPixelBufferRef pixBuff = [playerItemVideoOutput copyPixelBufferForItemTime:outputItemTime itemTimeForDisplay:NULL];
		// Copy the pixel buffer to be displayed next and add it to AVSampleBufferDisplayLayer for display
        double frameTime = CMTimeGetSeconds(outputItemTime);
		[self sendPixelBuffer:pixBuff frameTime:frameTime hostTime:inOutputTime->hostTime];
        self.hlsBugResetCount = 0;

		CVBufferRelease(pixBuff);
	} else {
		CMTime delta = CMClockMakeHostTimeFromSystemUnits(inNow->hostTime - self.lastHostTime);
        NSTimeInterval elapsedTime = CMTimeGetSeconds(delta);

		if (elapsedTime > FREEWHEELING_PERIOD_IN_SECONDS) {
            if (self.player.rate != 0.0) {
                if (self.hlsBugResetCount > 9) {
                    /*
                     * There is a bug in AVFoundation where if we're playing a HLS
                     * stream and it switches to a different bitrate, the video
                     * output will stop receiving frames. So far, the only workaround
                     * for this has been to remove then re-add the video output
                     * This causes the video to pause for a bit, but it's better
                     * than not playing at all, and this should not happen once
                     * the bug is fixed in AVFoundation.
                     */
                    [self.playerItem removeOutput:playerItemVideoOutput];
                    [self.playerItem addOutput:playerItemVideoOutput];
                    self.hlsBugResetCount = 0;
                    self.lastHostTime = inNow->hostTime;
                    // fall through to allow it to stop the display link
                } else {
                    self.hlsBugResetCount++;
                    self.lastHostTime = inNow->hostTime;
                    return kCVReturnSuccess;
                }
            }
			// No new images for a while.  Shut down the display link to conserve
            // power, but request a wakeup call if new images are coming.
			CVDisplayLinkStop(displayLink);
			[playerItemVideoOutput requestNotificationOfMediaDataChangeWithAdvanceInterval:ADVANCE_INTERVAL_IN_SECONDS];
		}
	}

	return kCVReturnSuccess;
}

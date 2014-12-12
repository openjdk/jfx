/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

#import "QTKMediaPlayer.h"
#import <Utils/MTObjectProxy.h>
#import <jni/Logger.h>
#import "CVVideoFrame.h"
#import <PipelineManagement/NullAudioEqualizer.h>
#import <PipelineManagement/NullAudioSpectrum.h>

#import <limits.h>

#define DUMP_TRACK_INFO 0

// this is annoying... all because we had to have a STOPPED state...
#define kPlaybackState_Stop 0
#define kPlaybackState_Play 1
#define kPlaybackState_Pause 2
#define kPlaybackState_Finished 3

// Non-public selectors for QTMovie
// WARNING: These aren't guaranteed to be there, you must check
// the movie object with respondsToSelector: first!
@interface QTMovie(HiddenStuff)

- (void) setAudioDevice:(id)device error:(NSError**)err;

- (float) balance;
- (void) setBalance:(float)b;

- (BOOL) isBuffering;
- (BOOL) hasEqualizer;

- (NSSet*) imageConsumers;
- (void) removeImageConsumer:(id)consumer flush:(BOOL)flush;
- (void) addImageConsumer:(id)consumer;

- (NSArray *) availableRanges;
- (NSArray *) loadedRanges;

@end

@interface QTTrack(HiddenStuff)

- (NSString*) channels; // ex: "Stereo (L R)"
- (int) audioChannelCount;
- (float) floatFrameRate;
- (float) audioSampleRate;
- (NSString*) codecName; // ex: "H.264"
- (NSString*) isoLanguageCodeAsString;

@end

@interface QTKMediaPlayer(PrivateStuff)

- (void) sendVideoFrame:(CVPixelBufferRef)pixelBuffer hostTime:(uint64_t)hostTime;

@end


@interface ImageConsumerProxy : NSObject
{
    QTKMediaPlayer *player;
}

@end

@implementation ImageConsumerProxy

- (id) initWithPlayer:(QTKMediaPlayer*)inPlayer
{
    if ((self = [super init]) != nil) {
        // don't retain the player or we'll cause a retain loop
        player = inPlayer;
    }
    return self;
}

- (void) dealloc
{
    [super dealloc];
}

- (NSDictionary *) preferredAttributes
{
    NSDictionary *pba = [NSDictionary dictionaryWithObjectsAndKeys:
                         [NSNumber numberWithBool:YES], @"IOSurfaceCoreAnimationCompatibility", // doesn't seem necessary
                         [NSArray arrayWithObjects:
                         [NSNumber numberWithLong:k2vuyPixelFormat],
                          nil], @"PixelFormatType",
                         nil];
    
    NSDictionary *attr = [NSDictionary dictionaryWithObjectsAndKeys:
                          [NSColorSpace genericRGBColorSpace], @"colorspace",
                          pba, @"pixelBufferAttributes",
                          nil];
    return attr;
}

- (void) flushImageBuffersAfterHostTime:(unsigned long long)hostTime
{
    // FIXME: Can't do anything? All the frames are pushed up...
}

- (void) setImageBuffer:(CVBufferRef)buf forHostTime:(unsigned long long)hostTime
{
    [player sendVideoFrame:buf hostTime:hostTime];
}

@end


@implementation QTKMediaPlayer

- (id) initWithURL:(NSURL *)source eventHandler:(CJavaPlayerEventDispatcher*)hdlr
{
    if ((self = [self init]) != nil) {
        movieURL = [source retain];
        
        movie = nil;
        movieReady = NO;
        
        frameHandler = [[ImageConsumerProxy alloc] initWithPlayer:self];
        
        notificationCookies = [[NSMutableSet alloc] init];
        
        isLiveStream = NO; // we'll determine later
        
        audioSyncDelay = 0;
        requestedRate = 1.0;
        updateHostTimeBase = NO;
        currentTime = 0.0;
        suppressDurationEvents = NO;
        mute = NO;
        volume = 1.0;
        balance = 0.0;
        
        eventHandler = hdlr;
        
        previousWidth = -1;
        previousHeight = -1;
        
        previousPlayerState = kPlayerState_UNKNOWN;
        
        requestedState = kPlaybackState_Stop;
        
        isDisposed = NO;

        _audioEqualizer = new CNullAudioEqualizer();
        _audioSpectrum = new CNullAudioSpectrum();

        // create the movie on the main thread, but don't wait for it to happen
        if (![NSThread isMainThread]) {
            [self performSelectorOnMainThread:@selector(createMovie) withObject:nil waitUntilDone:NO];
        } else {
            [self createMovie];
        }
    }
    return self;
}

- (void) dealloc
{
    [self dispose]; // just in case
    
    [frameHandler release];
    frameHandler = nil;
    
    [movieURL release];
    
    if (_audioEqualizer) {
        delete _audioEqualizer;
    }

    if (_audioSpectrum) {
        delete _audioSpectrum;
    }

    [super dealloc];
}

- (CAudioEqualizer*) audioEqualizer
{
    return _audioEqualizer;
}

- (CAudioSpectrum*) audioSpectrum
{
    return _audioSpectrum;
}

- (void) dispose
{
    @synchronized(self) {
        [movie invalidate];
        if (frameHandler) {
            // remove the image consumer
            [movie removeImageConsumer:frameHandler flush:NO];
        }
        [movie release];
        movie = nil;
        
        if (notificationCookies) {
            NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
            for (id cookie in notificationCookies) {
                [center removeObserver:cookie];
            }
            [notificationCookies release];
            notificationCookies = nil;
        }
        
        // eventHandler is cleaned up separately, just drop the reference
        eventHandler = NULL;
        
        isDisposed = YES;
    }
}

- (void) registerForNotification:(NSString*)name object:(id)object withBlock:(void (^)(NSNotification*))block
{
    id cookie = [[NSNotificationCenter defaultCenter]
                 addObserverForName:name
                 object:object
                 queue:nil
                 usingBlock:block];
    
    if (cookie) {
        [notificationCookies addObject:cookie];
    }
}

- (void) createMovie
{
    @synchronized(self) {
        if (isDisposed) {
            return;
        }
        
        if (![NSThread isMainThread]) {
            LOGGER_ERRORMSG_CM("QTKMediaPlayer", "createMovie", "was NOT called on the main app thread!\n");
            if (eventHandler) {
                eventHandler->SendPlayerMediaErrorEvent(ERROR_OSX_INIT);
            }
            return;
        }
        
        NSError *err = nil;
        QTMovie *qtMovie =
        [QTMovie movieWithAttributes:[NSDictionary dictionaryWithObjectsAndKeys:
                                      movieURL, QTMovieURLAttribute,
                                      [NSNumber numberWithBool:YES], QTMovieOpenForPlaybackAttribute,
                                      [NSNumber numberWithBool:YES], QTMovieOpenAsyncOKAttribute,
                                      //                                  [NSNumber numberWithBool:NO], QTMovieAskUnresolvedDataRefsAttribute,
                                      [NSNumber numberWithBool:YES], QTMovieDontInteractWithUserAttribute,
                                      nil]
                               error:&err];
        if (err || !qtMovie) {
            LOGGER_ERRORMSG_CM("QTKMediaPlayer", "createMovie", ([[NSString stringWithFormat:@"Error creating QTMovie: %@\n", err] UTF8String]));
            if (eventHandler) {
                eventHandler->SendPlayerMediaErrorEvent(ERROR_OSX_INIT);
            }
            qtMovie = nil;
        }
        
        /*
         *******************************************************************************
         BIG FAT WARNING!!!!!!!!!!!!!
         *******************************************************************************
         
         Do NOT reference "self" inside a block registered with the
         Notification Center or you will create a retain loop and prevent this
         object from ever releasing. Instead, use the stack variable "blockSelf"
         defined below, this will prevent the retain loop.
         
         *******************************************************************************
         */
        
        __block __typeof__(self) blockSelf = self;
        [self registerForNotification:QTMovieDidEndNotification
                               object:qtMovie
                            withBlock:
         ^(NSNotification*note) {
             [blockSelf finish];
         }];
        
        [self registerForNotification:QTMovieLoadStateDidChangeNotification
                               object:qtMovie
                            withBlock:
         ^(NSNotification *note) {
             /*
              * QTMovieLoadStateError - an error occurred while loading the movie
              * QTMovieLoadStateLoading - the movie is loading
              * QTMovieLoadStateLoaded - the movie atom has loaded; it's safe to query movie properties
              * QTMovieLoadStatePlayable - the movie has loaded enough media data to begin playing
              * QTMovieLoadStatePlaythroughOK - the movie has loaded enough media data to play through to the end
              * QTMovieLoadStateComplete - the movie has loaded completely
              */
             long loadState = [(NSNumber*)[movie attributeForKey:QTMovieLoadStateAttribute] longValue];
             NSError *loadError = (NSError*)[movie attributeForKey:QTMovieLoadStateErrorAttribute];
             if (loadError) {
                 LOGGER_ERRORMSG(([[NSString stringWithFormat:@"Error loading QTMovie: %@\n", loadError] UTF8String]));
                 if (eventHandler) {
                     eventHandler->SendPlayerMediaErrorEvent(ERROR_OSX_INIT);
                 }
             }
             
             if (!movieReady) {
                 if (loadState > QTMovieLoadStateLoaded) {
                     [blockSelf setMovieReady];
                 }
             } else if (requestedState == kPlaybackState_Play) {
                 // if state is QTMovieLoadStatePlayable then we've stalled
                 // if state is QTMovieLoadStatePlaythroughOK then we're playing
                 if (loadState == QTMovieLoadStatePlayable && previousPlayerState == kPlayerState_PLAYING) {
                     [blockSelf setPlayerState:kPlayerState_STALLED];
                 } else if (loadState == QTMovieLoadStatePlaythroughOK) {
                     [blockSelf setPlayerState:kPlayerState_PLAYING];
                 }
             }
         }];
        
        [self registerForNotification:QTMovieTimeDidChangeNotification
                               object:qtMovie
                            withBlock:
         ^(NSNotification *note) {
             // grab currentTime and current host time and set our host time base accordingly
             double now = blockSelf.currentTime;
             uint64_t hostTime = CVGetCurrentHostTime();
             hostTimeFreq = CVGetHostClockFrequency();
             uint64_t nowDelta = (uint64_t)(now * hostTimeFreq); // current time in host frequency units
             hostTimeBase = hostTime - nowDelta; // Host time at movie time zero
             LOGGER_DEBUGMSG(([[NSString stringWithFormat:@"Movie time changed %lf", currentTime] UTF8String]));
             
             // http://javafx-jira.kenai.com/browse/RT-27041
             // TODO: flush video buffers
         }];
        
        [self registerForNotification:QTMovieRateDidChangeNotification
                               object:qtMovie
                            withBlock:
         ^(NSNotification *note) {
             NSNumber *newRate = [note.userInfo objectForKey:QTMovieRateDidChangeNotificationParameter];
             [blockSelf rateChanged:newRate.floatValue];
         }];
        
        // QTMovieNaturalSizeDidChangeNotification is unreliable, especially with HTTP live streaming
        // so just use the pixel buffer sizes to send frame size changed events
        
        // QTMovieAvailableRangesDidChangeNotification
        [self registerForNotification:@"QTMovieAvailableRangesDidChangeNotification"
                               object:qtMovie
                            withBlock:
         ^(NSNotification *note) {
             NSArray *ranges = nil;
             if ([movie respondsToSelector:@selector(availableRanges)]) {
                 ranges = [movie performSelector:@selector(availableRanges)];
             }
             if (!suppressDurationEvents && ranges) {
                 for (NSValue *rangeVal in ranges) {
                     QTTimeRange timeRange = [rangeVal QTTimeRangeValue]; // .time, .duration
                     // if duration is indefinite then it's a live stream and we need to report as such
                     if (QTTimeIsIndefinite(timeRange.duration)) {
                         eventHandler->SendDurationUpdateEvent(INFINITY);
                         // and suppress all other subsequent events
                         suppressDurationEvents = YES;
                         isLiveStream = YES;
                         break;
                     }
                 }
             }
         }];
        
        // QTMovieLoadedRangesDidChangeNotification
        [self registerForNotification:@"QTMovieLoadedRangesDidChangeNotification"
                               object:qtMovie
                            withBlock:
         ^(NSNotification *note) {
             NSArray *ranges = nil;
             if ([movie respondsToSelector:@selector(loadedRanges)]) {
                 ranges = [movie performSelector:@selector(loadedRanges)];
             }
             // don't emit progress events for live streams
             if (!suppressDurationEvents && ranges) {
                 int64_t total = 0;
                 for (NSValue *rangeVal in ranges) {
                     QTTimeRange timeRange = [rangeVal QTTimeRangeValue]; // .time, .duration
                     NSTimeInterval duration;
                     QTGetTimeInterval(timeRange.duration, &duration);
                     
                     total += (int64_t)(duration * 1000);
                 }
                 // send buffer progress event
                 double movieDur = blockSelf.duration;
                 eventHandler->SendBufferProgressEvent(movieDur, 0, (int64_t)(movieDur * 1000), total);
             }
         }];
        
#if 0
        // show all notifications, use to find possibly missed notifications
        [[NSNotificationCenter defaultCenter]
         addObserverForName:nil
         object:qtMovie
         queue:nil
         usingBlock:^(NSNotification *note) {
             NSLog(@"Movie notification: %@", note.name);
         }
         ];
#endif
        
#if 0
        // Template notification block, remember to use blockSelf instead of self
        [[NSNotificationCenter defaultCenter]
         addObserverForName:QTMovieXXX
         object:qtMovie
         queue:nil
         usingBlock:
         ^(NSNotification *note) {
             
         }
         ];
#endif
        // http://javafx-jira.kenai.com/browse/RT-27041
        // TODO: test for addImageConsumer first, fall back on CARenderer hack if it's not available
        [qtMovie addImageConsumer:frameHandler];
        
        movie = (QTMovie*)[[MTObjectProxy objectProxyWithTarget:qtMovie] retain];
    }
}


- (void) play
{
    requestedState = kPlaybackState_Play;
    if (movie && movieReady) {
        [movie play];
        [movie setRate:requestedRate];
    }
}

- (void) pause
{
    if (requestedState == kPlaybackState_Stop) {
        requestedState = kPlaybackState_Pause;
        [self setPlayerState:kPlayerState_PAUSED];
    } else {
        requestedState = kPlaybackState_Pause;
        if (movie && movieReady) {
            [movie stop];
        }
        
        if (previousPlayerState == kPlayerState_STALLED) {
            [self setPlayerState:kPlayerState_PAUSED];
        }
    }
}

- (void) finish
{
    requestedState = kPlaybackState_Finished;
    [self setPlayerState:kPlayerState_FINISHED];
    if (movie && movieReady) {
        [movie stop];
    }
}

- (void) stop
{
    if (requestedState == kPlaybackState_Finished || requestedState == kPlaybackState_Pause) {
        requestedState = kPlaybackState_Stop;
        [self setPlayerState:kPlayerState_STOPPED];
    } else {
        requestedState = kPlaybackState_Stop;
        if (movie && movieReady) {
            // we need to just nuke the "STOPPED" state...
            [movie stop];
        } else {
            currentTime = 0.0;
        }
        
        if (previousPlayerState == kPlayerState_STALLED) {
            [self setPlayerState:kPlayerState_STOPPED];
        }
    }
}


- (void) rateChanged:(float)newRate
{
    /*
     * Relevant PlayerState values:
     *      PLAYING - rate != 0
     *      PAUSED  - reqRate == 0, rate == 0
     *      STOPPED - stopFlag && reqRate == 0, rate == 0
     *      STALLED - detected by load state or reqRate != 0, rate == 0 and state is PLAYING
     */
    if (newRate == 0.0) {
        // slop for FP/timescale error
        if (requestedState == kPlaybackState_Stop) {
            [self setPlayerState:kPlayerState_STOPPED];
        } else if (requestedState == kPlaybackState_Play && previousPlayerState == kPlayerState_PLAYING && requestedRate != 0.0) {
            [self setPlayerState:kPlayerState_STALLED];        
        } else if (requestedState != kPlaybackState_Finished) {
            [self setPlayerState:kPlayerState_PAUSED];
        }
        
    } else {
        // non-zero is always playing
        [self setPlayerState:kPlayerState_PLAYING];
    }
}

@synthesize audioSyncDelay;

- (BOOL) mute
{
    if (movie && movieReady) {
        mute = movie.muted;
        return mute;
    }
    return mute;
}

- (void) setMute:(BOOL)state
{
    mute = state;
    if (movie && movieReady) {
        movie.muted = state;
    }
}

- (float) volume
{
    if (movie && movieReady) {
        volume = movie.volume;
    }
    return volume;
}

- (void) setVolume:(float)newVolume
{
    volume = newVolume;
    if (movie && movieReady) {
        movie.volume = (float)volume;
    }
}

- (float) balance
{
    if (movie && movieReady) {
        if ([movie respondsToSelector:@selector(balance)]) {
            balance = [movie balance];
        } else if (eventHandler) {
            eventHandler->Warning(WARNING_JFXMEDIA_BALANCE, NULL);
        }
    }
    return balance;
}

- (void) setBalance:(float)newBalance
{
    balance = newBalance;
    if (movie && movieReady) {
        if ([movie respondsToSelector:@selector(setBalance:)]) {
            [movie setBalance:balance];
        } else if (eventHandler) {
            eventHandler->Warning(WARNING_JFXMEDIA_BALANCE, NULL);
        }
    }
}

- (double) duration
{
    if (movie && movieReady) {
        NSNumber *hasDuration = [movie attributeForKey:QTMovieHasDurationAttribute];
        if (hasDuration.boolValue) {
            QTTime movieDur = movie.duration;
            NSTimeInterval duration;
            if (QTGetTimeInterval(movieDur, &duration)) {
                return duration;
            }
        }
    }
    return -1.0; // hack value for UNKNOWN, since duration must be >= 0
}

- (float) rate
{
    return requestedRate;
}

- (void) setRate:(float)newRate
{
    if (isLiveStream) {
        LOGGER_WARNMSG("Cannot set playback rate on LIVE stream!");
        return;
    }

    requestedRate = newRate;
    if (movie && movieReady && requestedState == kPlaybackState_Play) {
        [movie setRate:requestedRate];
    }
}

- (double) currentTime
{
    if (movie && movieReady) {
        QTTime time = movie.currentTime;
        NSTimeInterval timeIval;
        if (QTGetTimeInterval(time, &timeIval)) {
            currentTime = timeIval;
        }
    }
    return currentTime;
}

- (void) setCurrentTime:(double)newTime
{
    if (isLiveStream) {
        LOGGER_WARNMSG("Cannot seek LIVE stream!");
        return;
    }
    
    currentTime = newTime;
    
    if (movie && movieReady) {
        movie.currentTime = QTMakeTimeWithTimeInterval(newTime);
        
        // make sure we're playing if requested
        if (requestedState == kPlaybackState_Play) {
            [movie play];
            [movie setRate:requestedRate];
        } else if (requestedState == kPlaybackState_Finished) {
            requestedState = kPlaybackState_Play;
            [movie play];
            [movie setRate:requestedRate];
        }
    }
}

- (void) setPlayerState:(int)newState
{
    if (newState != previousPlayerState) {
        if (newState == kPlayerState_PLAYING) {
            updateHostTimeBase = YES;
        }
        // For now just send up to client
        eventHandler->SendPlayerStateEvent(newState, 0.0);
        previousPlayerState = newState;
    }
}

#if DUMP_TRACK_INFO
static void append_log(NSMutableString *s, NSString *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    NSString *appString = [[NSString alloc] initWithFormat:fmt arguments:args];
    [s appendFormat:@"%@\n", appString];
    va_end(args);
    [appString release];
}
#define TRACK_LOG(fmt, ...) append_log(trackLog, fmt, ##__VA_ARGS__)
#else
#define TRACK_LOG(...) {}
#endif

- (void) parseMovieTracks
{
#if DUMP_TRACK_INFO
    NSMutableString *trackLog = [[NSMutableString alloc] initWithFormat:@"Parsing tracks for movie %@:\n", movie];
#endif    
    /*
     * Track properties we care about at the FX level:
     *
     * track:
     *   + trackEnabled (boolean)
     *   + trackID (jlong)
     *   + name (string)
     *   + locale (Locale) - language is derived from Locale or null
     *   + language (3 char iso code)
     * video track:
     *   + width (int)
     *   + height (int)
     * audio track: (no additional properties)
     *
     *
     * Track properties at the com.sun level:
     *
     * track:
     *   X trackEnabled (boolean) == QTTrackEnabledAttribute
     *   X trackID (long) == QTTrackIDAttribute
     *   X name (string) == QTTrackDisplayNameAttribute
     *   X encoding (enum) == non-public selector: - (NSString*) codecName
     * video track: (QTTrackMediaTypeAttribute == 'vide')
     *   X frame size (w,h) == QTTrackDimensionsAttribute (NSValue:NSSize)
     *   X frame rate
     *   X hasAlpha (boolean) == false (for now)
     * audio track: (QTTrackMediaTypeAttribute == 'soun')
     *   X language == non-public selector: - (NSString*) isoLanguageCodeAsString
     *   X channels (int) == non-public selector: - (int) audioChannelCount
     *   X channel mask (int) == parsed from channel count (or ASBD)
     *   X sample rate (float) == non-public selector: - (float) audioSampleRate
     *
     * just create CVideoTrack or CAudioTrack and send them up with eventHandler and we're good
     */
    NSArray *tracks = movie.tracks;
    if (tracks) {
        // get video tracks
        NSArray *tracks = [movie tracksOfMediaType:QTMediaTypeVideo];
        for (QTTrack *track in tracks) {
            long trackID = [[track attributeForKey:QTTrackIDAttribute] longValue];
            BOOL trackEnabled = [[track attributeForKey:QTTrackEnabledAttribute] boolValue];
            NSSize videoSize = [[track attributeForKey:QTTrackDimensionsAttribute] sizeValue];
            QTMedia *trackMedia;
            float frameRate = 29.97; // default
            NSString *codecName = nil;
            
            TRACK_LOG(@"Video QTTrack: %@", track);
            TRACK_LOG(@" - id %ld (%sabled)", trackID, trackEnabled ? "en" : "dis");

            CTrack::Encoding encoding = CTrack::CUSTOM;
            if ([track respondsToSelector:@selector(codecName)]) {
                codecName = [[track codecName] lowercaseString];
                if ([codecName hasPrefix:@"h.264"] || [codecName hasPrefix:@"avc"]) {
                    encoding = CTrack::H264;
                }
            }
            TRACK_LOG(@" - encoding %d (name %@)", encoding, codecName);
            
            if ([track respondsToSelector:@selector(floatFrameRate)]) {
                frameRate = [track floatFrameRate];
                TRACK_LOG(@" - provided frame rate %0.2f", frameRate);
            } else if ((trackMedia = track.media) != nil) {
                // estimate frame rate based on sample count and track duration
                if ([trackMedia hasCharacteristic:QTMediaCharacteristicHasVideoFrameRate]) {
                    QTTime duration = [[trackMedia attributeForKey:QTMediaDurationAttribute] QTTimeValue];
                    float samples = (float)[[trackMedia attributeForKey:QTMediaSampleCountAttribute] longValue];
                    frameRate = samples * ((float)duration.timeScale / (float)duration.timeValue);
                    TRACK_LOG(@" - estimated frame rate %0.2f", frameRate);
                } else {
                    TRACK_LOG(@" - Unable to determine frame rate!");
                }
            }
            
            // If we will support more media formats in OS X Platform, then select apropriate name.
            // Now only "video/x-h264" is supported
            CVideoTrack *cvt = new CVideoTrack((int64_t)trackID, "video/x-h264", encoding, trackEnabled,
                                               (int)videoSize.width, (int)videoSize.height, frameRate, false);
            eventHandler->SendVideoTrackEvent(cvt);
            delete cvt;
        }

        // get audio tracks
        tracks = [movie tracksOfMediaType:QTMediaTypeSound];
        for (QTTrack *track in tracks) {
            long trackID = [[track attributeForKey:QTTrackIDAttribute] longValue];
            BOOL trackEnabled = [[track attributeForKey:QTTrackEnabledAttribute] boolValue];
            NSString *codecName = nil;
            
            TRACK_LOG(@"Audio QTTrack: %@", track);
            TRACK_LOG(@" - id %ld (%sabled)", trackID, trackEnabled ? "en" : "dis");

            CTrack::Encoding encoding = CTrack::CUSTOM;
            if ([track respondsToSelector:@selector(codecName)]) {
                codecName = [[track codecName] lowercaseString];
                
                if ([codecName hasPrefix:@"aac"]) {
                    encoding = CTrack::AAC;
                } else if ([codecName hasPrefix:@"mp3"]) { // FIXME: verify these values, if we ever officially support them
                    encoding = CTrack::MPEG1LAYER3;
                } else if ([codecName hasPrefix:@"mpeg"] || [codecName hasPrefix:@"mp2"]) {
                    encoding = CTrack::MPEG1AUDIO;
                }
            }
            TRACK_LOG(@" - encoding %d (name %@)", encoding, codecName);
            
            float rate = 44100.0; // sane default
            if ([track respondsToSelector:@selector(audioSampleRate)]) {
                rate = floor([track audioSampleRate] * 1000.0); // audioSampleRate returns KHz
            }
            TRACK_LOG(@" - sample rate %0.0f", rate);
            
            int channelCount = 2;
            if ([track respondsToSelector:@selector(audioChannelCount)]) {
                channelCount = [track audioChannelCount];
                if (channelCount == 0) {
                    // we may not know (happens with some HLS streams) so just report stereo and hope for the best
                    channelCount = 2;
                }
            }
            TRACK_LOG(@" - channels %d", channelCount);
            
            int channelMask;
            switch (channelCount) {
                default:
                    channelMask = CAudioTrack::FRONT_LEFT | CAudioTrack::FRONT_RIGHT;
                    break;
                case 5:
                case 6:
                    // FIXME: Umm.. why don't we have a SUBWOOFER channel, which is what 5.1 (aka 6) channel audio is???
                    channelMask = CAudioTrack::FRONT_LEFT | CAudioTrack::FRONT_RIGHT
                    | CAudioTrack::REAR_LEFT | CAudioTrack::REAR_RIGHT
                    | CAudioTrack::FRONT_CENTER;
                    break;
            }
            TRACK_LOG(@" - channel mask %02x", channelMask);
            
            NSString *lang = @"und";
            if ([track respondsToSelector:@selector(isoLanguageCodeAsString)]) {
                NSString *newLang = [track isoLanguageCodeAsString];
                // it could return nil, in which case it's undetermined
                if (newLang) {
                    lang = newLang;
                }
            }
            TRACK_LOG(@" - language %@", lang);
            
            // If we will support more media formats in OS X Platform, then select apropriate name.
            // Now only "audio/mpeg" is supported
            CAudioTrack *cat = new CAudioTrack((int64_t)trackID, "audio/mpeg", encoding, (bool)trackEnabled,
                                               [lang UTF8String], channelCount, channelMask, rate);
            eventHandler->SendAudioTrackEvent(cat);
            delete cat;
        }

        // get subtitle tracks
        // FIXME: also QTMediaType{ClosedCaption,Text,etc...}
        tracks = [movie tracksOfMediaType:QTMediaTypeSubtitle];
        for (QTTrack *track in tracks) {
            long trackID = [[track attributeForKey:QTTrackIDAttribute] longValue];
            BOOL trackEnabled = [[track attributeForKey:QTTrackEnabledAttribute] boolValue];
            NSString *name = [track attributeForKey:QTTrackDisplayNameAttribute];
            NSString *codecName = nil;

            TRACK_LOG(@"Subtitle QTTrack: %@", track);
            TRACK_LOG(@" - id %ld (%sabled)", trackID, trackEnabled ? "en" : "dis");

            CTrack::Encoding encoding = CTrack::CUSTOM;
            if ([track respondsToSelector:@selector(codecName)]) {
                codecName = [[track codecName] lowercaseString];

                if ([codecName hasPrefix:@"aac"]) {
                    encoding = CTrack::AAC;
                } else if ([codecName hasPrefix:@"mp3"]) { // FIXME: verify these values, if we ever officially support them
                    encoding = CTrack::MPEG1LAYER3;
                } else if ([codecName hasPrefix:@"mpeg"] || [codecName hasPrefix:@"mp2"]) {
                    encoding = CTrack::MPEG1AUDIO;
                }
            }
            TRACK_LOG(@" - encoding %d (name %@)", encoding, codecName);

            NSString *lang = nil;
            if ([track respondsToSelector:@selector(isoLanguageCodeAsString)]) {
                NSString *newLang = [track isoLanguageCodeAsString];
                // it could return nil, in which case it's undetermined
                if (newLang) {
                    lang = newLang;
                }
            }
            TRACK_LOG(@" - language %@", lang);

            CSubtitleTrack *cat = new CSubtitleTrack((int64_t)trackID, [name UTF8String], encoding, (bool)trackEnabled,
                                                     [lang UTF8String]);
            eventHandler->SendSubtitleTrackEvent(cat);
            delete cat;
        }
    }
    
#if DUMP_TRACK_INFO
    LOGGER_INFOMSG([trackLog UTF8String]);
    [trackLog release];
#endif
}

- (void) setMovieReady
{
    if (movieReady) {
        return;
    }
    
    movieReady = YES;
    
    // send player ready
    [self setPlayerState:kPlayerState_READY];
    
    // get duration
    NSNumber *hasDuration = [movie attributeForKey:QTMovieHasDurationAttribute];
    if (!suppressDurationEvents && hasDuration.boolValue) {
        QTTime movieDur = movie.duration;
        // send INFINITY if it's indefinite
        if (QTTimeIsIndefinite(movieDur)) {
            eventHandler->SendDurationUpdateEvent(INFINITY);
            // and suppress all other duration events
            suppressDurationEvents = YES;
        } else {
            // otherwise send duration
            NSTimeInterval duration;
            if (QTGetTimeInterval(movieDur, &duration)) {
                eventHandler->SendDurationUpdateEvent(self.duration);
            }
        }
    }
    
    // Get movie tracks (deferred)
    [self parseMovieTracks];
    
    // Assert settings
    if (currentTime != 0.0) {
        movie.currentTime = QTMakeTimeWithTimeInterval(self.currentTime);
    }
    
    if (mute) {
        movie.muted = YES;
    }
    
    if (volume != 1.0) {
        movie.volume = volume;
    }
    
    if (requestedState == kPlaybackState_Play) {
        [movie play];
        [movie setRate:requestedRate];
    }
}

- (void) sendVideoFrame:(CVPixelBufferRef)buf hostTime:(uint64_t)hostTime
{
    // http://javafx-jira.kenai.com/browse/RT-27041
    // TODO: send off to a work queue for processing on a separate thread to avoid deadlock issues during shutdown
    
    if (movie && movieReady && eventHandler) {
        if (updateHostTimeBase) {
            double now = currentTime;
            uint64_t hostTime = CVGetCurrentHostTime();
            hostTimeFreq = CVGetHostClockFrequency();
            uint64_t nowDelta = (uint64_t)(now * hostTimeFreq); // current time in host frequency units
            hostTimeBase = hostTime - nowDelta; // Host time at movie time zero
            updateHostTimeBase = NO;
        }
        double frameTime = (double)(hostTime - hostTimeBase) / hostTimeFreq;

        CVVideoFrame *frame = NULL;
        try {
            frame = new CVVideoFrame(buf, frameTime, hostTime);
        } catch (const char *message) {
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
}

@end

/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import "MediaPlayer.h"

#import "jfxmedia_errors.h"
#import "com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer.h"
#import "ErrorHandler.h"
#import "JniUtils.h"

#import <MediaPlayer/MediaPlayer.h>
#import <UIKit/UIKit.h>

#import "debug.h"


#define KEY_STATUS                      @"status"
#define KEY_ERROR                       @"error"
#define KEY_LOADED_TIME_RANGES          @"loadedTimeRanges"
#define KEY_PLAYBACK_LIKELY_TO_KEEP_UP  @"playbackLikelyToKeepUp"
#define KEY_PLAYBACK_BUFFER_EMPTY       @"playbackBufferEmpty"
#define KEY_PLAYBACK_BUFFER_FULL        @"playbackBufferFull"
#define KEY_PRESENTATION_SIZE           @"currentItem.presentationSize"

#define TIMESCALE_ONE_SECOND            1

#define TICKS_PER_SECOND                10 // 100ms time resolution for timeObserver

#define TICKER_INTERVAL                 0.1 // 100ms time resolution for system timer


@implementation MediaPlayer


@synthesize player;
@synthesize playerItem;
@synthesize state;
@synthesize media;
@synthesize eventDispatcher;
//@synthesize timeObserver;
@synthesize videoLayer;


// PlayerItem status, error and buffer context constants
static const NSString *ItemStatusContext = @"AVPlayerItemStatusContext";

static const NSString *ItemErrorContext = @"AVPlayerItemErrorContext";

static const NSString *ItemBufferContext = @"AVPlayerItemBufferContext";

// used only with HLS because we can't rely on tracks information
static const NSString *ItemPresentationSizeContext = @"AVPlayerItemPresentationSizeContext";


// Tick callback for timeObserver
//- (void) tick: (CMTime) time {
//    NSLog(@"MediaPlayer::tick");
//    if (CMTimeCompare(stopTime, kCMTimeInvalid) != 0) {
//        CMTime currentCMTime = [player currentTime];
//        if (CMTimeCompare(currentCMTime, stopTime) >= 0) {
//            Float64 seconds = CMTimeGetSeconds(currentCMTime);
//
//            //[eventDispatcher sendStopReachedEvent: (double) seconds];
//            [self stop]; // takes care of time observer unregisteratration
//        }
//    }
//}

- (void) sendErrorToJava: (NSError *) error {

    jint errCode = [ErrorHandler mapAVErrorToFXError: error];

    [[self eventDispatcher]
     sendPlayerMediaErrorEvent: errCode];
}

- (void) sendTrackInfoToJava {

    AVAssetTrack *track;
    NSEnumerator *enumerator;

    NSArray *audioTracks = [[self media] audioTracks];
    if ([audioTracks count] > 0) {
        enumerator = [audioTracks objectEnumerator];
        while (track = [enumerator nextObject]) {
            [eventDispatcher sendAudioTrackEvent: track];
        }
    }

    NSArray *videoTracks = [[self media] videoTracks];
    if ([videoTracks count] > 0) {

        /*
         enumerator = [videoTracks objectEnumerator];
         while (track = [enumerator nextObject]) {
         [eventDispatcher sendVideoTrackEvent: track];
         }
         */

        // NOTE: the Java part is not ready for multiple video tracks
        //       for the time being, sending just the first track
        track = [videoTracks objectAtIndex: 0];
        [eventDispatcher sendVideoTrackEvent: track];
    }
}

//- (void) removeTimeObserver {
//    NSLog(@"MediaPlayer::removeTimeObserver");
//    if (timeObserver != nil) {
//        [player removeTimeObserver: timeObserver];
//        [self setTimeObserver: nil];
//    }
//}

- (void) dealloc {

    [self dispose];

    [player release];
    [playerItem release];
    [media release];
    [eventDispatcher release];
    [videoLayer release];

    [super dealloc];
}

- (void) dispose {

    [playerItem removeObserver: self forKeyPath: KEY_ERROR];
    [playerItem removeObserver: self forKeyPath: KEY_STATUS];
    [playerItem removeObserver: self forKeyPath: KEY_LOADED_TIME_RANGES];
    [playerItem removeObserver: self forKeyPath: KEY_PLAYBACK_BUFFER_FULL];
    [playerItem removeObserver: self forKeyPath: KEY_PLAYBACK_BUFFER_EMPTY];
    [playerItem removeObserver: self forKeyPath: KEY_PLAYBACK_LIKELY_TO_KEEP_UP];

    [[NSNotificationCenter defaultCenter] removeObserver: self
                                                    name: AVPlayerItemDidPlayToEndTimeNotification
                                                  object: [self playerItem]];

    //    if (timeObserver != nil) {
    //        [self removeTimeObserver];
    //    }
}

- (jint) getError {
    return (player.status == AVPlayerStatusFailed) ?
    [ErrorHandler mapAVErrorToFXError: player.error] : ERROR_NONE;
}

- (jint) finish {

    if ([self state] == PLAYING) {
        //[self removeTimeObserver];
        [player pause];
        [self setState: EOM];

        double time = 0.0;
        [self getCurrentTime: &time];
        [eventDispatcher sendEndOfMediaEvent: time];
    }

    return [self getError];
}

- (void) registerPlayerItemKVO {

    const NSKeyValueObservingOptions kvoOptions = NSKeyValueObservingOptionNew;

    [playerItem addObserver: self
                 forKeyPath: KEY_STATUS
                    options: kvoOptions
                    context: &ItemStatusContext];

    [playerItem addObserver: self
                 forKeyPath: KEY_ERROR
                    options: kvoOptions
                    context: &ItemErrorContext];

    [playerItem addObserver: self
                 forKeyPath: KEY_LOADED_TIME_RANGES
                    options: kvoOptions
                    context: &ItemBufferContext];

    [playerItem addObserver: self
                 forKeyPath: KEY_PLAYBACK_BUFFER_FULL
                    options: kvoOptions
                    context: &ItemBufferContext];

    [playerItem addObserver: self
                 forKeyPath: KEY_PLAYBACK_BUFFER_EMPTY
                    options: kvoOptions
                    context: &ItemBufferContext];

    [playerItem addObserver: self
                 forKeyPath: KEY_PLAYBACK_LIKELY_TO_KEEP_UP
                    options: kvoOptions
                    context: &ItemBufferContext];

}

- (void) initOverlayIfNecessary {
    if ([[[self media] videoTracks] count] > 0) {
        [self overlayInit];
    }
}

- (void) registerEOMListener {
    [[NSNotificationCenter defaultCenter] addObserver: self
                                             selector: @selector(playerItemDidReachEnd:)
                                                 name: AVPlayerItemDidPlayToEndTimeNotification
                                               object: [self playerItem]];
}

- (jint) initializePlayerWithURL: (NSURL *) url {

    // HTTP Live Streaming test URL:
    // http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8
    // http://www.nasa.gov/multimedia/nasatv/NTV-Public-IPS.m3u8
    // http://liveips.nasa.gov.edgesuite.net/msfc/Wifi.m3u8
    // http://88.212.10.27/streams/joj.m3u8
    // http://iphoned5.akamai.com.edgesuite.net/mhbarron/nasatv/nasatv_all.m3u8


    AVPlayer *avPlayer = [AVPlayer playerWithURL: url];
    if (avPlayer == nil) {
        return ERROR_MEMORY_ALLOCATION;
    }

    AVPlayerItem *item = [avPlayer currentItem];
    if (item == nil) {
        return ERROR_MEMORY_ALLOCATION;
    }

    [self setPlayerItem: item];
    [self setPlayer: avPlayer];
    [self registerPlayerItemKVO];
    [self registerEOMListener];

    AVAsset *asset = [item asset];
    [[self media] setMediaAsset: asset];

    // with HLS we can't rely on observing the tracks, the tracks array is always empty
    // instead we'll be watching for a change of the presentation size
    [avPlayer addObserver: self
               forKeyPath: KEY_PRESENTATION_SIZE
                  options: NSKeyValueObservingOptionNew
                  context: &ItemPresentationSizeContext];

    return ERROR_NONE;
}

- (jint) initializePlayerItemWithAsset: (AVAsset *) mediaAsset {

    AVPlayerItem *item = [AVPlayerItem playerItemWithAsset: mediaAsset];
    if (item == nil) {
        return ERROR_MEMORY_ALLOCATION;
    }

    [self setPlayerItem: item];

    AVPlayer *avPlayer = [AVPlayer playerWithPlayerItem: playerItem];
    if (avPlayer == nil) {
        return ERROR_MEMORY_ALLOCATION;
    }

    [self setPlayer: avPlayer];
    [self registerPlayerItemKVO];
    [self registerEOMListener];

    [self sendTrackInfoToJava];

    return ERROR_NONE;
}

- (id) initPlayerWithMedia: (Media *) newMedia
           javaEnvironment: (JNIEnv *) env
                javaPlayer: (jobject) playerInstance
                    result: (jint *) errorCode {

    *errorCode = ERROR_NONE;

    if (self = [super init]) {
        [self setMedia: newMedia];
        [self setState: INITIAL];

        playbackVolume = 1.0f;
        playbackRate = 1.0f;

        overlayX = 0.0f;
        overlayY = 0.0f;
        overlayWidth = 0.0f;
        overlayHeight = 0.0f;
        overlayOpacity = 1.0f;
        overlayPreserveRatio = TRUE;
        overlayVisible = FALSE;
        overlayTransform = CATransform3DIdentity;

        EventDispatcher *ed = [EventDispatcher alloc];
        if (NULL == ed) {
            *errorCode = ERROR_MEMORY_ALLOCATION;
        }
        else {
            [ed initWithJavaEnv: env
                 playerInstance: playerInstance];
        }
        [self setEventDispatcher: ed];

        if (NULL != [self media]) {
            NSError *mediaError = [[self media] error];
            if (mediaError != nil) {
                *errorCode = [ErrorHandler mapAVErrorToFXError: mediaError];
            }
            else {
                if ([[self media] readyForPlayback] && [self playerItem] == nil) {
                    *errorCode = [self initializePlayerItemWithAsset: [[self media] mediaAsset]];
                }
                else if ([[self media] isHls]) {
                    *errorCode = [self initializePlayerWithURL: [[self media] url]];
                }
            }
        }
        else {
            *errorCode = ERROR_MEDIA_NULL;
        }

        if (*errorCode != ERROR_NONE && [self eventDispatcher] != NULL) {
            [[self eventDispatcher] sendPlayerMediaErrorEvent: *errorCode];
        }
    }
    else {
        *errorCode = ERROR_MEMORY_ALLOCATION;
    }

    return self;
}

- (jint) getCurrentTime: (double *) time {

    CMTime cmTime = [player currentTime];
    Float64 seconds = CMTimeGetSeconds(cmTime);

    const char *debugMsg = [[NSString stringWithFormat: @"current time %f", seconds] UTF8String];
    [ErrorHandler logMsg: LOGGER_DEBUG
                 message: debugMsg];

    *time = (double) seconds;

    //CMTimeShow(cmTime);

    return [self getError];
}

//- (void) addTimeObserver {
//
//    CMTime observeInterval = CMTimeMake(1, TICKS_PER_SECOND);
//
//    [self setTimeObserver:
//     [player addPeriodicTimeObserverForInterval: observeInterval
//                                          queue: dispatch_get_main_queue()
//
//                                     usingBlock: ^(CMTime time) {
//
//                                                [self tick: time];
//
//                                            }
//    ]];
//}

- (void) handleStatusChange: (AVPlayerStatus) status {

    switch (status) {
            /* Indicates that the status of the player is not yet known because
             it has not tried to load new media resources for playback */
        case AVPlayerStatusUnknown: {
            [ErrorHandler logMsg: LOGGER_INFO
                         message: "PlayerItem status UNKNOWN"];
            break;
        }
        case AVPlayerStatusReadyToPlay: {
            // NOTE: it seems this status change can occur more than once at initilization, find out why
            [ErrorHandler logMsg: LOGGER_INFO
                         message: "PlayerItem status READY TO PLAY"];
            if ([self state] == INITIAL) {
                [self setState: READY];
                [self initOverlayIfNecessary];

                double time;
                [self getCurrentTime: &time];

                [[self eventDispatcher]
                 sendPlayerStateEvent: com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_eventPlayerReady
                 presentTime: time];
            }
            break;
        }
        case AVPlayerStatusFailed: {
            [ErrorHandler logMsg: LOGGER_WARNING
                         message: "PlayerItem status FAILED"];
            [ErrorHandler logError: [player error]];
            break;
        }
    }
}

- (void) handleBufferChange: (NSString *) keyPath {

    if ([keyPath isEqualToString: KEY_LOADED_TIME_RANGES]) {

        NSArray *loadedTimeRanges = [playerItem loadedTimeRanges];
        CMTimeRange timeRange = [[loadedTimeRanges objectAtIndex:0] CMTimeRangeValue];
        float start_time = CMTimeGetSeconds(timeRange.start) * 1000000;
        float duration_time = CMTimeGetSeconds(timeRange.duration) * 1000000;

        long bufferProgressTime = (long) (start_time + duration_time);

        [eventDispatcher sendBufferProgressEvent: (double) [[self media] duration]
                                                : 0L
                                                : (long) ([[self media] duration] * 1000000)
                                                : bufferProgressTime];
    }
    else if ([keyPath isEqualToString: KEY_PLAYBACK_BUFFER_EMPTY]) {

        if ([self state] == PLAYING) {

            [ErrorHandler logMsg: LOGGER_INFO message: "Player is stalling"];

            [self setState: STALLED];

            double time = 0.0;
            [self getCurrentTime: &time];

            const jint newJavaState = com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_eventPlayerStalled;
            [eventDispatcher sendPlayerStateEvent: newJavaState presentTime: time];
        }
    }
    else if ([keyPath isEqualToString: KEY_PLAYBACK_LIKELY_TO_KEEP_UP]) {

        [ErrorHandler logMsg: LOGGER_INFO message: "Playback is likely to keep up"];

        if ([self state] == STALLED) {

            [player setRate: playbackRate];
            [self setState: PLAYING];

            const jint newJavaState = com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_eventPlayerPlaying;

            double time = 0.0;
            [self getCurrentTime: &time];

            [eventDispatcher sendPlayerStateEvent: newJavaState
                                      presentTime: time];
        }

    }

    /*    else if ([keyPath isEqualToString: KEY_PLAYBACK_BUFFER_FULL]) {}*/

}

// NSKeyValueObserving Protocol
- (void) observeValueForKeyPath: (NSString *) keyPath
                       ofObject: (id)object
                         change: (NSDictionary *) change
                        context: (void *) context {

    const char *debugMsg = [[NSString stringWithFormat:
                             @"MediaPlayer::observeValueForKeyPath: keyPath=%@\nobject=%@\nchange=%@\n",
                             keyPath, object, change]
                            UTF8String];
    [ErrorHandler logMsg: LOGGER_DEBUG
                 message: debugMsg];

    if (context == &ItemStatusContext) {
        AVPlayerStatus status = [[change objectForKey: NSKeyValueChangeNewKey]
                                 integerValue];
        [self handleStatusChange: status];
    }
    else if (context == &ItemErrorContext) {
        NSError *error = [change objectForKey: NSKeyValueChangeNewKey];
        [self sendErrorToJava: error];
    }
    else if (context == &ItemBufferContext) {
        [self handleBufferChange: keyPath];
    }
    else if (context == &ItemPresentationSizeContext) {
        // occurs only in case of HLS
        CGSize size = [playerItem presentationSize];
        int newWidth = (int) size.width;
        int newHeight = (int) size.height;
        if (newWidth != [[self media] width] && newHeight != [[self media] height]) {
            [[self media] setWidth: newWidth];
            [[self media] setHeight: newHeight];
            [self overlayInit];
            [eventDispatcher sendFrameSizeChangedEvent: newWidth
                                                      : newHeight];
        }
    }
    else {
        [ErrorHandler logMsg: LOGGER_INFO message: "MediaPlayer::observeValueForKeyPath: context unknown"];
    }

}

- (jint) play {

    // watching for stop time moved to Java so we don't need to run the ticker anymore
    //[self addTimeObserver];

    [player setRate: playbackRate];

    [self setState: PLAYING];

    const jint newJavaState = com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_eventPlayerPlaying;

    double time = 0.0;
    [self getCurrentTime: &time];

    [eventDispatcher sendPlayerStateEvent: newJavaState
                              presentTime: time];

    return [self getError];
}

- (jint) pause {

    [player pause];

    [self setState: PAUSED];

    //[self removeTimeObserver];

    jint newJavaState = com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_eventPlayerPaused;

    double time = 0.0;
    [self getCurrentTime: &time];

    [eventDispatcher sendPlayerStateEvent: newJavaState
                              presentTime: time];

    return [self getError];
}

- (jint) stop {

    [player pause];

    //[self removeTimeObserver];

    jint newJavaState = com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_eventPlayerStopped;

    double time = 0.0;
    [self getCurrentTime: &time];

    [eventDispatcher sendPlayerStateEvent: newJavaState
                              presentTime: time];

    [self setState: STOPPED];

    return [self getError];
}


- (void) playerItemDidReachEnd: (NSNotification *) notification {

    [ErrorHandler logMsg: LOGGER_INFO message: "MediaPlayer::playerItemDidReachEnd (EOM)"];

    [self setState: EOM];

    //[self removeTimeObserver];

    double time = 0.0;
    [self getCurrentTime: &time];

    [eventDispatcher sendEndOfMediaEvent: time];
}

- (jint) getRate: (float *) rate {

    *rate = [player rate];

    return [self getError];
}

- (jint) setRate: (float) rate {

    // we need to store this setting because AVPlayer.play() sets rate to 1.0
    playbackRate = rate;

    if ([self state] == PLAYING) {
        [player setRate: rate];
    }

    return [self getError];
}

- (jint) getVolume: (float *) volume {

    // http://javafx-jira.kenai.com/browse/RT-27005
    // TODO: Figure out how to retrieve current volume setting from AVPlayer.
    //       For the time being we assume that the last setVolume() was successful
    //       and return the last value
    // Note that AVAudioPlayer has a 'volume' property whereas AVPlayer doesn't. :-(

    *volume = playbackVolume;

    return ERROR_NONE;
}

- (jint) setVolume: (float) volume {

    // AVPlayer has no 'volume' property like AVAudioPlayer does
    if ([[self media] isHls]) {
        // with HLS there are no tracks in the asset :-( we need to change the global volume
        MPMusicPlayerController *playerController = [MPMusicPlayerController iPodMusicPlayer];
        playerController.volume = volume;
    }
    else {
        NSMutableArray *allAudioParams = [NSMutableArray array];
        AVAsset *asset = [playerItem asset];
        NSArray *audioTracks = [asset tracksWithMediaType: AVMediaTypeAudio];

        for (AVAssetTrack *track in audioTracks) {
            AVMutableAudioMixInputParameters *audioInputParams =
            [AVMutableAudioMixInputParameters audioMixInputParameters];
            [audioInputParams setVolume: volume
                                 atTime: kCMTimeZero];
            [audioInputParams setTrackID: [track trackID]];
            [allAudioParams addObject: audioInputParams];
        }

        AVMutableAudioMix *audioMix = [AVMutableAudioMix audioMix];
        [audioMix setInputParameters: allAudioParams];
        [playerItem setAudioMix: audioMix];
    }

    playbackVolume = volume;

    return [self getError];
}

- (jint) seek: (double) time {

    CMTime cmTime = CMTimeMakeWithSeconds(
                                          (Float64) time,
                                          TIMESCALE_ONE_SECOND);
    [player seekToTime: cmTime
       toleranceBefore: kCMTimeZero
        toleranceAfter: kCMTimeZero
     completionHandler:
     ^(BOOL finished) {
         if (finished) {
             if ([self state] == EOM) {
                 // if a seek occurs in the EOM state, it means we must be looping
                 [self play];
             }
         }
     }
     ];

    return [self getError];
}

- (void) notifyDurationChanged {

    double duration = (double) [media duration];

    [[self eventDispatcher]
     sendDurationUpdateEvent: duration];
}

- (void) notifyError: (NSError *) error {

    [self sendErrorToJava: error];
}

////////////////////////////////////////////////////////////////////////
////////////////////// Video Overlay extension /////////////////////////
////////////////////////////////////////////////////////////////////////

- (UIView *) getMainView {
    UIApplication *application = [UIApplication sharedApplication];
    UIWindow *window = [application keyWindow];
    return [[window rootViewController] view];
}

- (void) printOverlayProperties {

    const char *debugMsg = [[NSString stringWithFormat:
                             @"Video overlay properties:\nframe: [%f,%f] [%fx%f]\nbounds: [%f,%f] [%fx%f]\nposition: [%f,%f]\nanchorPoint: [%f,%f]",
                             videoLayer.frame.origin.x, videoLayer.frame.origin.y, videoLayer.frame.size.width, videoLayer.frame.size.height,
                             videoLayer.bounds.origin.x, videoLayer.bounds.origin.y, videoLayer.bounds.size.width, videoLayer.bounds.size.height,
                             videoLayer.position.x, videoLayer.position.y,
                             videoLayer.anchorPoint.x, videoLayer.anchorPoint.y]
                            UTF8String];
    [ErrorHandler logMsg: LOGGER_DEBUG
                 message: debugMsg];
}

- (void) overlayInit {

    [self setVideoLayer: [AVPlayerLayer playerLayerWithPlayer: player]];

    [CATransaction begin];
    [CATransaction setAnimationDuration: 0];
    [CATransaction setDisableActions: YES];

    [videoLayer removeAllAnimations];
    [videoLayer setHidden: !overlayVisible];
    [videoLayer setVideoGravity: overlayPreserveRatio ?
AVLayerVideoGravityResizeAspect : AVLayerVideoGravityResize];
    [videoLayer setAnchorPoint: CGPointMake(0.0f, 0.0f)];

    CGFloat initialWidth = (overlayWidth > 0.0f) ? overlayWidth : media.width;
    CGFloat initialHeight = (overlayHeight > 0.0f) ? overlayHeight : media.height;
    CGRect frame = CGRectMake(0.0f, 0.0f, initialWidth, initialHeight);
    [videoLayer setFrame: frame]; // will set bounds and position

    [videoLayer setTransform: overlayTransform];

    [videoLayer setOpacity: overlayOpacity];

    UIView *view = [self getMainView];
    [view.layer addSublayer: videoLayer];

    [self printOverlayProperties];

    [CATransaction commit];
}

// Overlay Visibility

- (jint) updateOverlayVisibility {
    [CATransaction begin];
    [CATransaction setAnimationDuration: 0];
    [CATransaction setDisableActions: YES];

    [videoLayer setHidden: !overlayVisible];

    [CATransaction commit];

    return ERROR_NONE;
}

- (jint) overlaySetVisible: (BOOL) visible {

    overlayVisible = visible;

    jint result = ERROR_NONE;
    if ([self state] != INITIAL) {
        result = [self updateOverlayVisibility];
    }
    return result;
}

// Overlay X, Y

- (jint) updateOverlayTransform {
    [CATransaction begin];
    [CATransaction setAnimationDuration: 0];
    [CATransaction setDisableActions: YES];

    if (overlayX != 0.0 || overlayY != 0.0) {
        [videoLayer setTransform: CATransform3DTranslate(overlayTransform,
                                                         overlayX, overlayY, 0)];
    }
    else {
        [videoLayer setTransform: overlayTransform];
    }

    [CATransaction commit];

    return ERROR_NONE;
}

- (jint) overlaySetX: (CGFloat) x {

    overlayX = x;

    jint result;
    if ([self state] != INITIAL) {
        result = [self updateOverlayTransform];
    }
    return result;
}

- (jint) overlaySetY: (CGFloat) y {

    overlayY = y;

    jint result;
    if ([self state] != INITIAL) {
        result = [self updateOverlayTransform];
    }
    return result;
}

// Overlay Width

- (jint) updateOverlayWidth {
    [CATransaction begin];
    [CATransaction setAnimationDuration: 0];
    [CATransaction setDisableActions: YES];

    CGRect newFrame = videoLayer.frame;
    newFrame.size.width = overlayWidth;

    videoLayer.frame = newFrame;

    [CATransaction commit];

    return ERROR_NONE;
}

- (jint) overlaySetWidth: (CGFloat) width {

    overlayWidth = width;

    jint result;
    if ([self state] != INITIAL) {
        result = [self updateOverlayWidth];
    }
    return result;
}

// Overlay Height

- (jint) updateOverlayHeight {
    [CATransaction begin];
    [CATransaction setAnimationDuration: 0];
    [CATransaction setDisableActions: YES];

    CGRect newFrame = videoLayer.frame;
    newFrame.size.height = overlayHeight;

    videoLayer.frame = newFrame;

    [CATransaction commit];

    return ERROR_NONE;
}

- (jint) overlaySetHeight: (CGFloat) height {

    overlayHeight = height;

    jint result;
    if ([self state] != INITIAL) {
        result = [self updateOverlayHeight];
    }
    return result;
}

// Overlay Preserve Ratio

- (jint) updateOverlayPreserveRatio {
    [CATransaction begin];
    [CATransaction setAnimationDuration: 0];
    [CATransaction setDisableActions: YES];

    if (overlayPreserveRatio) {
        [videoLayer setVideoGravity: AVLayerVideoGravityResizeAspect];
    }
    else {
        [videoLayer setVideoGravity: AVLayerVideoGravityResize];
    }

    [CATransaction commit];

    return ERROR_NONE;
}

- (jint) overlaySetPreserveRatio: (BOOL) preserveRatio {

    overlayPreserveRatio = preserveRatio;

    jint result;
    if ([self state] != INITIAL) {
        result = [self updateOverlayPreserveRatio];
    }
    return result;
}

// Overlay opacity

- (jint) updateOverlayOpacity {
    [CATransaction begin];
    [CATransaction setAnimationDuration: 0];
    [CATransaction setDisableActions: YES];

    [videoLayer setOpacity: overlayOpacity];

    [CATransaction commit];

    return ERROR_NONE;
}

- (jint) overlaySetOpacity: (CGFloat) opacity {

    overlayOpacity = opacity;

    jint result;
    if ([self state] != INITIAL) {
        result = [self updateOverlayOpacity];
    }
    return result;
}

// Overlay Transform

- (jint) overlaySetTransform
:(CGFloat) mxx :(CGFloat) mxy :(CGFloat) mxz :(CGFloat) mxt
:(CGFloat) myx :(CGFloat) myy :(CGFloat) myz :(CGFloat) myt
:(CGFloat) mzx :(CGFloat) mzy :(CGFloat) mzz :(CGFloat) mzt {

    const char *debugMsg = [[NSString stringWithFormat:
                             @"MediaPlayer::overlaySetTransform [%f %f %f %f] [%f %f %f %f] [%f %f %f %f]",
                             mxx, mxy, mxz, mxt, myx, myy, myz, myt, mzx, mzy, mzz, mzt]
                            UTF8String];
    [ErrorHandler logMsg: LOGGER_DEBUG
                 message: debugMsg];

    overlayTransform.m11 = mxx;
    overlayTransform.m21 = mxy;
    overlayTransform.m31 = mxz;
    overlayTransform.m41 = mxt;

    overlayTransform.m12 = myx;
    overlayTransform.m22 = myy;
    overlayTransform.m32 = myz;
    overlayTransform.m42 = myt;

    overlayTransform.m13 = mzx;
    overlayTransform.m23 = mzy;
    overlayTransform.m33 = mzz;
    overlayTransform.m43 = mzt;

    jint result;
    if ([self state] != INITIAL) {
        result = [self updateOverlayTransform];
    }
    return result;
}


@end

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

#import "Media.h"
#import "ErrorHandler.h"
#import "JniUtils.h"
#import "MediaUtils.h"

#import "debug.h"


@implementation Media


@synthesize mediaAsset;
@synthesize mediaPlayer;
@synthesize readyForPlayback;
@synthesize isHls;
@synthesize width;
@synthesize height;
@synthesize duration;
@synthesize error;
@synthesize audioTracks;
@synthesize videoTracks;
@synthesize metadata;
@synthesize url;


- (void) updateDuration {

    CMTime assetDuration = [mediaAsset duration];
    Float64 fAssetDuration = CMTimeGetSeconds(assetDuration);
    [self setDuration: fAssetDuration];

    if (NULL != mediaPlayer) {
        [mediaPlayer notifyDurationChanged];
    }
}

- (void) updateMetadata {

    NSArray *commonMetadata = [mediaAsset commonMetadata];

    [self setMetadata: commonMetadata];

    NSEnumerator *enumerator = [commonMetadata objectEnumerator];
    AVMetadataItem* item;
    while (item = [enumerator nextObject]) {
        const char* msg = [[NSString stringWithFormat: @"Metadata %@ : %@",
                            [item commonKey], [item stringValue]] UTF8String];
        [ErrorHandler logMsg: LOGGER_INFO message: msg];
    }
}

- (void) updateTracks {

    NSArray *tracks = [mediaAsset tracks];
    NSEnumerator *enumerator = [tracks objectEnumerator];
    AVAssetTrack *track;

    while (track = [enumerator nextObject]) {

        // we'll use trackID for the track's name - have a better idea?
        CMPersistentTrackID id = [track trackID];
        NSString *trackName = [NSString stringWithFormat:@"%d", id];

        BOOL isVideo = [track hasMediaCharacteristic: AVMediaCharacteristicVisual];
        BOOL isAudio = [track hasMediaCharacteristic: AVMediaCharacteristicAudible];

        const char* msg = [[NSString stringWithFormat: @"Track id=%@ video=%@ audio=%@",
                            trackName,
                            boolToString(isVideo),
                            boolToString(isAudio)]
                           UTF8String];
        [ErrorHandler logMsg: LOGGER_INFO message: msg];

        // for audio tracks we need to get their languages
        if (isAudio) {
            NSString *language = [track languageCode];

            const char* msg = [[NSString stringWithFormat: @" - audio track language %@", language]
                               UTF8String];
            [ErrorHandler logMsg: LOGGER_INFO message: msg];

            [audioTracks addObject: track];
        }
        // for video tracks we need to get their dimensions
        else if (isVideo) {
            CGSize size = [track naturalSize];
            if (width == 0 || height == 0) {
                [self setWidth: size.width];
                [self setHeight: size.height];
            }
            const char* msg = [[NSString stringWithFormat: @" - video track dimensions %fx%f",
                                size.width, size.height]
                               UTF8String];
            [ErrorHandler logMsg: LOGGER_INFO message: msg];

            [videoTracks addObject: track];
        }

        // JavaFX doesn't care about legible tracks (subtitles etc.)
    }

    if (([audioTracks count] > 0) || ([videoTracks count] > 0)) {
        readyForPlayback = TRUE;
        if (mediaPlayer && !isHls) {
            [mediaPlayer initializePlayerItemWithAsset: mediaAsset];
        }
    }
}

- (void) reportError: (NSError *) newError {

    [ErrorHandler logError: newError];

    [self setError: newError];

    if (mediaPlayer != nil) {
        [mediaPlayer notifyError: [self error]];
    }

}

- (void) handleTracksStatusChange {

    NSError *err = nil;

    AVKeyValueStatus tracksStatus =
    [mediaAsset statusOfValueForKey: KEY_TRACKS
                              error: &err];

    switch (tracksStatus) {
        case AVKeyValueStatusLoaded:
            [self updateTracks];
            break;
        case AVKeyValueStatusFailed:
            [self reportError: err];
            break;
        case AVKeyValueStatusCancelled:
            [ErrorHandler logMsg: LOGGER_INFO
                         message: "Track status change: AVKeyValueStatusCancelled"];
            break;
    }
}

- (void) handleDurationStatusChange {

    NSError *err = nil;

    AVKeyValueStatus durationStatus =
    [mediaAsset statusOfValueForKey: KEY_DURATION
                              error: &err];

    switch (durationStatus) {
        case AVKeyValueStatusLoaded:
            [self updateDuration];
            break;
        case AVKeyValueStatusFailed:
            [ErrorHandler logMsg: LOGGER_WARNING
                         message: "Media duration loading failed"];
            [self setDuration: -1.0f];
            break;
        case AVKeyValueStatusCancelled:
            [ErrorHandler logMsg: LOGGER_INFO
                         message: "Media duration loading canceled"];
            break;
    }
}

- (void) handleMetadataStatusChange {

    NSError *err = nil;

    AVKeyValueStatus metadataStatus =
    [mediaAsset statusOfValueForKey: KEY_METADATA
                              error: &err];

    switch (metadataStatus) {
        case AVKeyValueStatusLoaded:
            [self updateMetadata];
            break;
        case AVKeyValueStatusFailed:
            [ErrorHandler logMsg: LOGGER_WARNING
                         message: "Media metadata loading failed"];
            // http://javafx-jira.kenai.com/browse/RT-27005
            // TODO: deal with errors
            break;
        case AVKeyValueStatusCancelled:
            [ErrorHandler logMsg: LOGGER_INFO
                         message: "Media metadata loading canceled"];
            break;
    }
}

- (void) handlePlayableStatusChange {

    NSError *err = nil;

    AVKeyValueStatus playableStatus =
    [mediaAsset statusOfValueForKey: KEY_PLAYABLE
                              error: &err];

    switch (playableStatus) {
        case AVKeyValueStatusLoaded:
            [ErrorHandler logMsg: LOGGER_INFO
                         message: "Media playable status loaded"];
            // http://javafx-jira.kenai.com/browse/RT-27005
            // TODO: we've got playable content
            break;
        case AVKeyValueStatusFailed:
            [ErrorHandler logMsg: LOGGER_WARNING
                         message: "Media playable status loading failed"];
            // http://javafx-jira.kenai.com/browse/RT-27005
            // TODO: deal with errors
            break;
        case AVKeyValueStatusCancelled:
            [ErrorHandler logMsg: LOGGER_INFO
                         message: "Media playable status loading canceled"];
            break;
    }
}

- (void) loadMetadataAsync {

    // load metadata asynchronously
    NSArray *metadataKeys = [NSArray arrayWithObjects:
                             KEY_TRACKS, KEY_DURATION,
                             KEY_METADATA, KEY_PLAYABLE,
                             nil];

    [mediaAsset loadValuesAsynchronouslyForKeys: metadataKeys
                              completionHandler: ^ {

                                  [self handleTracksStatusChange];

                                  [self handleDurationStatusChange];

                                  [self handleMetadataStatusChange];

                                  [self handlePlayableStatusChange];

                              }];
}

- (BOOL) isM3UFile: (NSString *) fileName {

    // http://javafx-jira.kenai.com/browse/RT-27005
    // TODO: don't rely on file extension, analyze the file
    //       check the file header for '#EXTM3U'

    fileName = [fileName stringByTrimmingCharactersInSet:
                [NSCharacterSet whitespaceAndNewlineCharacterSet]];

    return [[fileName lowercaseString] hasSuffix: M3U8_SUFFIX];
}


// Factory method to initialize Media with a string URL
- (id) initMedia: (NSString *) urlString {

    self = [super init];

    if (self) {

        readyForPlayback = FALSE;

        [self setDuration: -1.0f];
        [self setWidth: 0.0f];
        [self setHeight: 0.0f];
        [self setAudioTracks: [[NSMutableArray alloc] init]];
        [self setVideoTracks: [[NSMutableArray alloc] init]];

        NSURL *mediaUrl = [MediaUtils urlFromString: urlString];
        [self setUrl: mediaUrl];

        if (mediaUrl != nil) {
            if ([self isM3UFile: urlString]) {
                // AVPlayer will be initialized directly using a URL
                [self setIsHls: TRUE];
            }
            else {
                // consider passing "YES" if we need precise random access
                NSDictionary *options = [NSDictionary
                                         dictionaryWithObject: [NSNumber numberWithBool: NO]
                                         forKey: AVURLAssetPreferPreciseDurationAndTimingKey];

                AVAsset *asset = [[AVURLAsset alloc] initWithURL: url
                                                         options: options];

                [self setMediaAsset: asset];
                [self loadMetadataAsync];
            }
        }
        else {
            // malformed URL
            NSMutableDictionary* errorDetails = [NSMutableDictionary dictionary];
            [errorDetails setValue: @"Malformed URL"
                            forKey: NSLocalizedDescriptionKey];
            // -30773 = Invalid URL error as defined in MacErrors.h
            NSError *newError = [NSError errorWithDomain: @"NSOSStatusErrorDomain"
                                                    code: -30773
                                                userInfo: errorDetails];
            [self setError: newError];
        }
    }

    return self;
}

- (void) dispose {
    // TODO: http://javafx-jira.kenai.com/browse/RT-27005
}

- (void) dealloc {

    [mediaAsset release];
    [mediaPlayer release];
    [error release];
    [audioTracks release];
    [videoTracks release];
    [metadata release];
    [url release];

    [super dealloc];
}


@end

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

#import "AudioClip.h"
#import "MediaUtils.h"
#import "ErrorHandler.h"

#include "debug.h"
#include "JniUtils.h"


@implementation AudioClip


@synthesize player;


static NSMutableArray* players;


#pragma mark -
#pragma mark Static initialization

+ (void) initialize {
    if (self == [AudioClip class]) {
        players = [[NSMutableArray alloc] init];
    }
}

#pragma mark -
#pragma mark Instance initialization

- (id) init {
    self = [super init];
    return self;
}

#pragma mark -
#pragma mark Static methods

+ (void) stopAll {

    if ([players count] > 0) {
        for (AVAudioPlayer* audioPlayer in players) {
            [audioPlayer stop];
        }
    }
}

#pragma mark -
#pragma mark Instance methods

- (void) load: (NSString *) urlString {

    NSError *error = nil;
    NSData *audioData = nil;

    // take care of jar: URLs
    NSURL *url = [MediaUtils urlFromString: urlString];

    NSDataReadingOptions options = NSDataReadingUncached; // should improve performance

    // AVAudioPlayer (unlike AVPlayer) doesn't support HTTP URLs, so we use NSData
    if ([url isFileURL]) {
        audioData = [NSData dataWithContentsOfFile: [url path]
                                           options: options
                                             error: &error];
    }
    else {
        //assuming it's HTTP
        audioData = [NSData dataWithContentsOfURL: url
                                          options: options
                                            error: &error];
    }

    if (audioData != nil && error == nil) {
        AVAudioPlayer* audioPlayer = [[AVAudioPlayer alloc] initWithData: audioData
                                                                   error: &error];

        [self setPlayer: audioPlayer];
        [audioPlayer release];

        if (self.player == nil) {
            [ErrorHandler logError: error];
        }
        else {
            [self.player setNumberOfLoops: 0];
            [self.player setEnableRate: YES];
            [self.player prepareToPlay];

            [players addObject: self.player];
        }
    }
    else {
        [ErrorHandler logError: error];
    }
}

- (void) unload {

    if (self.player != nil) {

        [players removeObject: self.player];

        [self.player stop];
        [self.player release];
    }
}

- (BOOL) isPlaying {

    BOOL playing = FALSE;

    if (self.player != nil) {
        playing = [self.player isPlaying];
    }

    return playing;
}

- (void) play {

    if (self.player != nil) {
        [self.player play];
    }
}

- (void) stop {

    if (self.player != nil) {
        [self.player stop];
    }
}

- (void) setVolume: (double) volume {

    if (self.player != nil) {
        [self.player setVolume: (float) volume];
    }
}

- (void) setRate: (double) rate {

    if (self.player != nil) {
        [self.player setRate: (float) rate];
    }
}

- (void) setPan: (double) pan {

    if (self.player != nil) {
        [self.player setPan: (float) pan];
    }
}

- (void) setLoopCount: (int) count {

    if (self.player != nil) {
        [self.player setNumberOfLoops: count];
    }
}

#pragma mark -
#pragma mark Deallocation

- (void) dealloc {

    if (self.player != nil) {
        [self.player stop];
        [players removeObject: self.player];
        [self.player release];
    }

    [super dealloc];
}

@end

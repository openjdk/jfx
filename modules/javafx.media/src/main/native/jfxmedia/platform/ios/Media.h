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

#ifndef _MEDIA_H_
#define _MEDIA_H_


#import "jni.h"
#import <AVFoundation/AVFoundation.h>
#import "MediaPlayer.h"

#define KEY_TRACKS          @"tracks"
#define KEY_DURATION        @"duration"
#define KEY_METADATA        @"commonMetadata"
#define KEY_STATUS          @"status"
#define KEY_PLAYABLE        @"playable"

#define M3U8_SUFFIX         @"m3u8"


@class MediaPlayer;


@interface Media : NSObject


@property (readwrite, retain) AVAsset *mediaAsset;

@property (readwrite, retain) MediaPlayer *mediaPlayer;

@property (readwrite, retain) NSError *error;

@property (readwrite, retain) NSMutableArray *audioTracks;

@property (readwrite, retain) NSMutableArray *videoTracks;

@property (readwrite, retain) NSArray *metadata;

@property (readwrite, retain) NSURL *url;

@property (readwrite) BOOL readyForPlayback;

@property (readwrite) BOOL isHls;

@property (readwrite) Float64 duration;

@property (readwrite) CGFloat width;

@property (readwrite) CGFloat height;


- (id) initMedia: (NSString *) uri;

- (void) dispose;


@end

#endif

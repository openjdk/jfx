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

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

#import "AVFSoundLevelUnit.h"
#import "AVFAudioSpectrumUnit.h"
#import "AVFAudioEqualizer.h"

@class AVFMediaPlayer;

@interface AVFAudioProcessor : NSObject

@property (nonatomic,readonly) AVFSoundLevelUnit *soundLevelUnit;

// These will be set by the media player since the objects must be persistent
@property (nonatomic,assign) AVFAudioSpectrumUnit *audioSpectrum;
@property (nonatomic,assign) AVFAudioEqualizer *audioEqualizer;

// Player we'll send events to
@property (weak,readonly,nonatomic) AVFMediaPlayer *player; // weak to avoid retain loop
@property (readonly,nonatomic) AVAudioMix *mixer;
@property (nonatomic,assign) void *tapStorage;

// Settings from player
@property (nonatomic,assign) float volume;
@property (nonatomic,assign) float balance;
@property (nonatomic,assign) int64_t audioDelay;

// Asset track and mixer will be pulled from player
- (id) initWithPlayer:(AVFMediaPlayer*)player assetTrack:(AVAssetTrack*)assetTrack;

@end

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
#import <AVFoundation/AVPlayerItemOutput.h>
#import <CoreVideo/CoreVideo.h>

#import "OSXPlayerProtocol.h"
#import "jni/JavaPlayerEventDispatcher.h"
#import "AVFAudioSpectrumUnit.h"
#import "AVFAudioEqualizer.h"

@class AVFAudioProcessor;

@interface AVFMediaPlayer : NSObject<OSXPlayerProtocol,AVPlayerItemOutputPullDelegate>
{
    CVDisplayLinkRef _displayLink;
    CMVideoFormatDescriptionRef _videoFormat;

    dispatch_queue_t playerQueue;

    CJavaPlayerEventDispatcher *eventHandler;

    int requestedState; // 0 - stop, 1 - play, 2 - pause
    float requestedRate;

    int previousWidth;
    int previousHeight;
    int previousPlayerState; // avoid repeated states

    BOOL isDisposed;
    NSMutableArray *keyPathsObserved;

    // placeholders until audio processor is created
    float _volume;
    float _balance;
    int64_t _audioSyncDelay; // delay is time in milliseconds

    AVFAudioSpectrumUnit *_audioSpectrum;
    AVFAudioEqualizer *_audioEqualizer;
}

@property (nonatomic,retain) NSURL *movieURL;
@property (nonatomic,retain) AVPlayer *player;
@property (nonatomic,retain) AVPlayerItem *playerItem;
@property (nonatomic,retain) AVPlayerItemVideoOutput *playerOutput;
@property (nonatomic,retain) AVFAudioProcessor *audioProcessor;
@property (nonatomic,assign) uint64_t lastHostTime;

@property (nonatomic,assign) int64_t audioSyncDelay;
@property (nonatomic,assign) float balance;


@property (nonatomic,assign) BOOL movieReady;   // set to YES the first time we get ready to play state
@property (nonatomic,assign) BOOL isLiveStream; // YES if the stream is indeterminate

// There's a bug in AVFoundation where when a HLS stream switches to a new
// sub-stream, the call to hasNewPixelBufferForItemTime will begin to return
// NO. So, we'll attempt to detect that case and when we encounter it we'll
// stop asking and always ask for a new frame instead.
// This *should* be fixed in a near-future version of AVFoundation, and it does
// not happen with non HLS sources, so we'll leave the current behavior as the
// default
@property (nonatomic,assign) BOOL buggyHLSSupport;
@property (nonatomic,assign) int hlsBugResetCount;

@property (nonatomic,readonly) CAudioEqualizer *audioEqualizer;
@property (nonatomic,readonly) CAudioSpectrum *audioSpectrum;

- (id) initWithURL:(NSURL *)source eventHandler:(CJavaPlayerEventDispatcher*)hdlr;
- (void) setPlayerState:(int)newState;

@end

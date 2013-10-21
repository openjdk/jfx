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

#ifndef _MEDIAPLAYER_H_
#define _MEDIAPLAYER_H_

#import "jni.h"
#import "Media.h"
#import "EventDispatcher.h"

enum _PlayerStates {

    INITIAL,
    READY,
    PLAYING,
    STOPPED,
    PAUSED,
    EOM,
    STALLED

};

typedef enum _PlayerStates PlayerStates;

@class Media;
@class EventDispatcher;

@interface MediaPlayer : NSObject {

@private

    float playbackVolume;

    float playbackRate;

    /***************** Video Layer extension *****************/

    CGFloat overlayX, overlayY;

    CGFloat overlayWidth, overlayHeight;

    CGFloat overlayOpacity;

    BOOL overlayVisible;

    BOOL overlayPreserveRatio;

    CATransform3D overlayTransform;

}

@property (readwrite, retain) AVPlayer *player;

@property (readwrite, retain) AVPlayerItem *playerItem;

@property (readwrite, retain) Media *media;

@property (readwrite, retain) EventDispatcher *eventDispatcher;

//@property (readwrite, retain) id timeObserver;

@property (readwrite, retain) AVPlayerLayer* videoLayer;

@property (readwrite) volatile PlayerStates state;


- (id) initPlayerWithMedia: (Media *) newMedia
           javaEnvironment: (JNIEnv *) env
                javaPlayer: (jobject) playerInstance
                    result: (jint *) errorCode;

- (void) dispose;

- (jint) finish;

- (jint) initializePlayerItemWithAsset: (AVAsset *) mediaAsset;

- (jint) play;

- (jint) pause;

- (jint) stop;

- (jint) getCurrentTime: (double *) time;

- (jint) getVolume: (float *) volume;

- (jint) setVolume: (float) volume;

- (jint) getRate: (float *) rate;

- (jint) setRate: (float) rate;

- (jint) seek: (double) time;

// Called by the Media object when media properties are retrieved or changed
- (void) notifyDurationChanged;

- (void) notifyError: (NSError *) error;

// Video Overlay extension methods
- (void) overlayInit;
- (jint) overlaySetVisible: (BOOL) visible;
- (jint) overlaySetX: (CGFloat) x;
- (jint) overlaySetY: (CGFloat) y;
- (jint) overlaySetWidth: (CGFloat) width;
- (jint) overlaySetHeight: (CGFloat) height;
- (jint) overlaySetPreserveRatio: (BOOL) preserveRatio;
- (jint) overlaySetOpacity: (CGFloat) opacity;
- (jint) overlaySetTransform
:(CGFloat) mxx :(CGFloat) mxy :(CGFloat) mxz :(CGFloat) mxt
:(CGFloat) myx :(CGFloat) myy :(CGFloat) myz :(CGFloat) myt
:(CGFloat) mzx :(CGFloat) mzy :(CGFloat) mzz :(CGFloat) mzt;


@end

#endif

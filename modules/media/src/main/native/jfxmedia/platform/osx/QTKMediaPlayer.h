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

#import <Foundation/Foundation.h>
#import <QTKit/QTKit.h>

#import "OSXPlayerProtocol.h"
#import <jni/JavaPlayerEventDispatcher.h>

@interface QTKMediaPlayer : NSObject<OSXPlayerProtocol>
{
    NSURL *movieURL;

    QTMovie *movie;
    BOOL movieReady;
    id frameHandler;

    NSMutableSet *notificationCookies; // need these to deregister from the notification center

    CJavaPlayerEventDispatcher *eventHandler;

    int64_t audioSyncDelay;

    BOOL isLiveStream; // YES if the stream is indeterminate

    int requestedState; // 0 - stop, 1 - play, 2 - pause
    float requestedRate;

    uint64_t hostTimeBase; // Host time for media time 0.0, updated every time we get a time changed notification
    double hostTimeFreq;   // frequency of the host clock, for conversion to seconds
    BOOL updateHostTimeBase;

    double currentTime;

    BOOL suppressDurationEvents;

    BOOL mute;
    float volume;
    float balance;

    int previousWidth;
    int previousHeight;

    int previousPlayerState; // avoid repeated states

    BOOL isDisposed;
}

- (id) initWithURL:(NSURL *)source eventHandler:(CJavaPlayerEventDispatcher*)hdlr;

- (void) rateChanged:(float)newRate;
- (void) setPlayerState:(int)newState;
- (void) setMovieReady;
- (void) createMovie;

@end

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

#ifndef _EVENTDISPATCHER_H_
#define _EVENTDISPATCHER_H_

#import <jni.h>
#import <AVFoundation/AVFoundation.h>


enum _Encoding {

    NONE,

    // Audio encodings
    PCM,                // Uncompressed PCM
    MPEG1AUDIO,         // MPEG1 Audio (layer1,2)
    MPEG1LAYER3,        // MPEG1 Layer3 (mp3)
    MPEG4AUDIO,         // MPEG-4 Audio
    AAC,                // Advanced Audio Coding

    // Video encodings
    H264,
    VP6,                // On2 VP6
    VP8,

    // text encodings
    ANSITEXT,           // plain text (ANSI)
    UNICODETEXT,        // plain text (Unicode)

    // custom encoding
    CUSTOM
};

typedef enum _Encoding Encoding;

@interface EventDispatcher : NSObject {

    // TODO: these fields should be static global variables, no need to initialize them for every instance
    // http://javafx-jira.kenai.com/browse/RT-27005
    jmethodID midSendPlayerStateEvent;
    jmethodID midSendPlayerMediaErrorEvent;
    jmethodID midSendPlayerHaltEvent;
    jmethodID midSendNewFrameEvent;
    jmethodID midSendFrameSizeChangedEvent;
    jmethodID midSendAudioTrackEvent;
    jmethodID midSendVideoTrackEvent;
    jmethodID midSendMarkerEvent;
    jmethodID midSendBufferProgressEvent;
    jmethodID midSendDurationUpdateEvent;
    jmethodID midSendAudioSpectrumEvent;

}


@property jobject javaPlayerInstance;


- (id) initWithJavaEnv: (JNIEnv *) env
        playerInstance: (jobject) playerInstance;

- (void) dispose;

- (void) sendPlayerStateEvent: (jint) newState
                  presentTime: (double) presentTime;

- (void) sendPlayerMediaErrorEvent: (jint) errorCode;

- (void) sendPlayerHaltEvent: (NSString *) message
                        time: (double) time;

- (void) sendEndOfMediaEvent: (double) presentTime;

- (void) sendAudioTrackEvent: (AVAssetTrack *) track;

- (void) sendVideoTrackEvent: (AVAssetTrack *) track;


- (void) sendDurationUpdateEvent: (double) time;

- (void) sendBufferProgressEvent: (double) duration
                                : (long) start
                                : (long) stop
                                : (long) position;

- (void) sendFrameSizeChangedEvent: (int) width
                                  : (int) height;

@end

#endif

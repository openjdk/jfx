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

#import "EventDispatcher.h"
#import "JniUtils.h"
#import "com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer.h"
#import "ErrorHandler.h"

#import "debug.h"


@implementation EventDispatcher


@synthesize javaPlayerInstance;


- (void) initMethodIDs: (JNIEnv *) env
              forClass: (jclass) klass {

    midSendPlayerStateEvent = (*env)->GetMethodID(env,
                                                  klass,
                                                  "sendPlayerStateEvent",
                                                  "(ID)V");

    midSendPlayerMediaErrorEvent = (*env)->GetMethodID(env,
                                                       klass,
                                                       "sendPlayerMediaErrorEvent",
                                                       "(I)V");

    midSendPlayerHaltEvent = (*env)->GetMethodID(env,
                                                 klass,
                                                 "sendPlayerHaltEvent",
                                                 "(Ljava/lang/String;D)V");

    midSendNewFrameEvent = (*env)->GetMethodID(env,
                                               klass,
                                               "sendNewFrameEvent",
                                               "(J)V");

    midSendFrameSizeChangedEvent = (*env)->GetMethodID(env,
                                                       klass,
                                                       "sendFrameSizeChangedEvent",
                                                       "(II)V");

    midSendAudioTrackEvent = (*env)->GetMethodID(env,
                                                 klass,
                                                 "sendAudioTrack",
                                                 "(ZJLjava/lang/String;ILjava/lang/String;IIF)V");

    midSendVideoTrackEvent = (*env)->GetMethodID(env,
                                                 klass,
                                                 "sendVideoTrack",
                                                 "(ZJLjava/lang/String;IIIFZ)V");

    /*midSendMetadataEvent = (*env)->GetMethodID(env,
     klass,
     "sendMetadata",
     "(Ljava/util/Map;)V");
     */
    midSendMarkerEvent = (*env)->GetMethodID(env,
                                             klass,
                                             "sendMarkerEvent",
                                             "(Ljava/lang/String;D)V");

    midSendBufferProgressEvent = (*env)->GetMethodID(env,
                                                     klass,
                                                     "sendBufferProgressEvent",
                                                     "(DJJJ)V");

    /*midSendStopReachedEvent = (*env)->GetMethodID(env,
     klass,
     "sendStopReachedEvent",
     "(D)V");
     */
    midSendDurationUpdateEvent = (*env)->GetMethodID(env,
                                                     klass,
                                                     "sendDurationUpdateEvent",
                                                     "(D)V");

    midSendAudioSpectrumEvent = (*env)->GetMethodID(env,
                                                    klass,
                                                    "sendAudioSpectrumEvent",
                                                    "(DD)V");

}

- (id) initWithJavaEnv: (JNIEnv *) env
        playerInstance: (jobject) playerInstance {

    self = [super init];

    if (self) {

        [self setJavaPlayerInstance: (*env)->NewGlobalRef(env, playerInstance)];

        jclass klass = (*env)->GetObjectClass(env, playerInstance);

        [self initMethodIDs: env
                   forClass: klass];

        (*env)->DeleteLocalRef(env, klass);
    }

    return self;
}

- (void) dispose {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        (*env)->DeleteGlobalRef(env, javaPlayerInstance);
        if (attached) {
            detachThread();
        }
    }
}

- (void) sendPlayerStateEvent: (jint) newState
                  presentTime: (double) presentTime {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        (*env)->CallVoidMethod(env,
                               javaPlayerInstance,
                               midSendPlayerStateEvent,
                               newState,
                               presentTime);
        if (attached) {
            detachThread();
        }
    }
}

- (void) sendPlayerMediaErrorEvent: (jint) errorCode {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        (*env)->CallVoidMethod(env,
                               javaPlayerInstance,
                               midSendPlayerMediaErrorEvent,
                               errorCode);
        if (attached) {
            detachThread();
        }
    }
}

- (void) sendPlayerHaltEvent: (NSString *) message
                        time: (double) time {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        (*env)->CallVoidMethod(env,
                               javaPlayerInstance,
                               midSendPlayerHaltEvent,
                               message,
                               (jdouble) time);
        if (attached) {
            detachThread();
        }
    }
}

- (void) sendFrameSizeChangedEvent: (int) width
                                  : (int) height {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        (*env)->CallVoidMethod(env,
                               javaPlayerInstance,
                               midSendFrameSizeChangedEvent,
                               (jint) width,
                               (jint) height);
        if (attached) {
            detachThread();
        }
    }
}

- (void) sendEndOfMediaEvent: (double) presentTime {

    jint finalState = com_sun_media_jfxmediaimpl_platform_ios_IOSMediaPlayer_eventPlayerFinished;

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        (*env)->CallVoidMethod(env,
                               javaPlayerInstance,
                               midSendPlayerStateEvent,
                               finalState,
                               (jdouble) presentTime);
        if (attached) {
            detachThread();
        }
    }
}

- (void) sendBufferProgressEvent: (double) duration
                                : (long) start
                                : (long) stop
                                : (long) position {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        (*env)->CallVoidMethod(env,
                               javaPlayerInstance,
                               midSendBufferProgressEvent,
                               (jdouble) duration,
                               (jlong) start,
                               (jlong) stop,
                               (jlong) position);
        if (attached) {
            detachThread();
        }
    }
}


/*

 For audio tracks we report only

 - name (made up of the track's ID)
 - language code conforming to ISO 639-2/T

 Which is to say that information about sample rate, encoding, number of channels and
 the type of channels is incorrect because there doesn't seem to be a way how to find
 that out using the AVFoundation framework.

 */

// NOTE: find out what information can be retrieved using AVAssetTrack.formatDescription

- (void) sendAudioTrackEvent: (AVAssetTrack *) track {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {

        // using ID for JFX track's name - have a better idea?
        CMPersistentTrackID trackID = [track trackID];
        NSString *name = [NSString stringWithFormat: @"Audio Track %d", trackID];
        const char* cStrName = [name UTF8String];
        jstring jName = (*env)->NewStringUTF(env, cStrName);

        // ISO 639-2/T Language code. Consider making use of the extended language tag.
        NSString *language = [track languageCode];
        if (language == nil) {
            language = @"unknown";
        }

        const char* cStrLanguage = [language UTF8String];
        jstring jLanguage = (*env)->NewStringUTF(env, cStrLanguage);

        Encoding encoding = NONE;

        (*env)->CallVoidMethod(env,
                               javaPlayerInstance,
                               midSendAudioTrackEvent,
                               (jboolean) [track isEnabled],
                               (jlong) trackID,
                               jName,
                               encoding,
                               jLanguage,
                               2,            // unknown number of channels (must be > 0)
                               0,            // unknown channel mask
                               44100.0        // unknown sample rate (must be > 0)
                               );
        (*env)->DeleteLocalRef(env, jName);
        (*env)->DeleteLocalRef(env, jLanguage);

        if (attached) {
            detachThread();
        }
    }
}

/*
 In case of video tracks we are able to provide only

 - width
 - height
 - frame rate

 Which means that information about encoding, bitrate and alpha channel is incorrect.

 */

// NOTE: find out what information can be retrieved using AVAssetTrack.formatDescription

- (void) sendVideoTrackEvent: (AVAssetTrack *) track {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {

        // using ID for JFX track's name - have a better idea?
        CMPersistentTrackID trackID = [track trackID];
        NSString *name = [NSString stringWithFormat: @"Video Track %d", trackID];
        const char* cStrName = [name UTF8String];
        jstring jName = (*env)->NewStringUTF(env, cStrName);

        Encoding encoding = NONE;

        jfloat frameRate = (jfloat) [track nominalFrameRate];

        CGSize size = [track naturalSize];
        CGFloat width = size.width;
        CGFloat height = size.height;

        (*env)->CallVoidMethod(env,
                               javaPlayerInstance,
                               midSendVideoTrackEvent,
                               (jboolean) [track isEnabled],
                               (jlong) trackID,
                               jName,
                               encoding,
                               (jint) width,
                               (jint) height,
                               frameRate,
                               (jboolean) FALSE            // alpha channel info is unknown
                               );

        (*env)->DeleteLocalRef(env, jName);

        if (attached) {
            detachThread();
        }
    }
}

/*
 - (void) sendStopReachedEvent: (double) time {

 NSLog(@"EventDispatcher::sendStopReachedEvent %f", time);

 bool attached;
 JNIEnv *env = media_getEnv(&attached);

 if (env) {
 (*env)->CallVoidMethod(env,
 javaPlayerInstance,
 midSendStopReachedEvent,
 (jdouble) time);
 if (attached) {
 detachThread();
 }
 }
 }
 */

- (void) sendDurationUpdateEvent: (double) time {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        (*env)->CallVoidMethod(env,
                               javaPlayerInstance,
                               midSendDurationUpdateEvent,
                               (jdouble) time);
        if (attached) {
            detachThread();
        }
    }
}

- (jobject) createObjectOfClass: (const char *) class_name
                    constructor: (jmethodID *) cid
                      signature: (const char *) sig
                          value: (jvalue *) val {

    jobject result = NULL;

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {

        jclass klass = (*env)->FindClass(env, class_name);
        if (klass == NULL) {
            return NULL; // can't find/load the class
        }

        if (*cid == NULL) {
            *cid = (*env)->GetMethodID(env, klass, "<init>", sig);
            if (*cid == NULL) {
                (*env)->DeleteLocalRef(env, klass);
                return NULL; // can't find the constructor
            }
        }

        result = (*env)->NewObjectA(env, klass, *cid, val);

        (*env)->DeleteLocalRef(env, klass);

        if (attached) {
            detachThread();
        }
    }

    return result;
}

- (jobject) createBoolean: (jboolean) boolean_value {

    static jmethodID cid = NULL;
    jvalue value;
    value.z = boolean_value;

    return [self createObjectOfClass: "java/lang/Boolean"
                         constructor: &cid
                           signature: "(Z)V"
                               value: &value];
}

- (jobject) createInteger: (jint) int_value {

    static jmethodID cid = NULL;
    jvalue value;
    value.i = int_value;

    return [self createObjectOfClass: "java/lang/Integer"
                         constructor: &cid
                           signature: "(I)V"
                               value: &value];
}

- (jobject) createLong: (jlong) long_value {

    static jmethodID cid = NULL;
    jvalue value;
    value.j = long_value;

    return [self createObjectOfClass: "java/lang/Long"
                         constructor: &cid
                           signature: "(J)V"
                               value: &value];
}

- (jobject) createDouble: (jdouble) double_value {

    static jmethodID cid = NULL;
    jvalue value;
    value.d = double_value;

    return [self createObjectOfClass: "java/lang/Double"
                         constructor: &cid
                           signature: "(D)V"
                               value: &value];
}

- (jobject) createDate: (NSDate *) date {

    jobject result;
    static jmethodID cid = NULL;

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        jclass calendarClass = (*env)->FindClass(env, "java/util/GregorianCalendar");
        if (calendarClass == NULL) {
            return NULL; // can't find the class
        }

        if (cid == NULL) {
            cid = (*env)->GetMethodID(env, calendarClass, "<init>", "(III)V");
            if (cid == NULL) {
                (*env)->DeleteLocalRef(env, calendarClass);
                return NULL; // can't find the constructor
            }
        }

        NSDateComponents *components = [[NSCalendar currentCalendar]
                                        components: NSDayCalendarUnit | NSMonthCalendarUnit | NSYearCalendarUnit
                                        fromDate: date];
        jint day = (jint) [components day];
        jint month = (jint) ([components month] - 1);
        jint year = (jint) [components year];

        result = (*env)->NewObject(env, calendarClass, cid, year, month, day);

        (*env)->DeleteLocalRef(env, calendarClass);

        if (attached) {
            detachThread();
        }
    }

    return result;
}

// Duration must be in milliseconds
- (jobject) createDuration: (jlong) duration {

    jobject result;
    static jmethodID cid = NULL;

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        jclass durationClass = (*env)->FindClass(env, "javafx/util/Duration");
        if (durationClass == NULL) {
            return NULL; // can't find the class
        }

        if (cid == NULL) {
            cid = (*env)->GetMethodID(env, durationClass, "<init>", "(D)V");
            if (cid == NULL) {
                (*env)->DeleteLocalRef(env, durationClass);
                return NULL; // can't find the constructor
            }
        }

        result = (*env)->NewObject(env, durationClass, cid, duration);

        (*env)->DeleteLocalRef(env, durationClass);

        if (attached) {
            detachThread();
        }
    }

    return result;
}

- (jobject) createMetadataMap: (NSArray *) metadata {

    jobject hashMap = NULL;
    static jmethodID cid = NULL;
    static jmethodID putID = NULL;

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        jclass hashClass = (*env)->FindClass(env, "java/util/HashMap");
        if (hashClass == NULL) {
            return NULL; // can't find the class
        }

        if (cid == NULL) {
            cid = (*env)->GetMethodID(env, hashClass, "<init>", "()V");
            if (cid == NULL) {
                (*env)->DeleteLocalRef(env, hashClass);
                return NULL; // can't find the constructor
            }
        }

        hashMap = (*env)->NewObject(env, hashClass, cid);

        if (putID == NULL) {
            putID = (*env)->GetMethodID(env, hashClass, "put",
                                        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
            if (putID == NULL) {
                (*env)->DeleteLocalRef(env, hashMap);
                (*env)->DeleteLocalRef(env, hashClass);
                return NULL; // can't find the put method
            }
        }

        (*env)->DeleteLocalRef(env, hashClass);

        NSEnumerator *enumerator = [metadata objectEnumerator];
        AVMetadataItem* item;
        while (item = [enumerator nextObject]) {
            id itemValue = [item value];
            jobject jValue = NULL;
            if ([itemValue isKindOfClass: [NSString class]]) {
                jValue = (*env)->NewStringUTF(env, [itemValue UTF8String]);
            }
            else if ([itemValue isKindOfClass: [NSDate class]]) {
                jValue = [self createDate: (NSDate *) itemValue];
            }
            else if ([itemValue isKindOfClass: [NSNumber class]]) {
                // TODO: distinguish long/int/double, now passing all numbers as double
                // http://javafx-jira.kenai.com/browse/RT-27005
                jValue = [self createDouble: [(NSNumber *) itemValue doubleValue]];
            }
            // TODO: handle duration as Duration object
            // http://javafx-jira.kenai.com/browse/RT-27005
            else {
                [ErrorHandler logMsg: LOGGER_WARNING message: "Metadata conversion failed. Unrecognized value type"];
            }

            if (jValue) {
                NSString *itemKey = [item commonKey];
                jobject jKey = (*env)->NewStringUTF(env, [itemKey UTF8String]);
                (*env)->CallObjectMethod(env, hashMap, putID, jKey, jValue);
                (*env)->DeleteLocalRef(env, jKey);
                (*env)->DeleteLocalRef(env, jValue);
            }
        }

        if (attached) {
            detachThread();
        }
    }

    return hashMap;
}

// Sending of metadata was removed from the Java code in rev. 1754:e824b858a685. Why???

/*- (void) sendMetadataEvent: (NSArray *) metadata {

 NSLog(@"EventDispatcher::sendMetadataEvent %@", metadata);

 JNIEnv *env = getEnv();
 if (env) {

 jobject jmetadata = [self createMetadataMap: metadata];
 if (jmetadata) {
 (*env)->CallVoidMethod(env,
 javaPlayerInstance,
 midSendMetadataEvent,
 jmetadata);
 (*env)->DeleteLocalRef(env, jmetadata);
 }
 }
 }
 */

@end

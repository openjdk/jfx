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

#import "ErrorHandler.h"
#import "jfxmedia_errors.h"
#import "AVFoundation/AVError.h"

#import "debug.h"

#include "JniUtils.h"


@implementation ErrorHandler


static int level;
static jclass javaLoggerClass;
static jmethodID logMsg1Method = NULL;
static jmethodID logMsg2Method = NULL;


+ (int) getLevel {
    return level;
}

+ (void) setLevel: (int) newLevel {
    level = newLevel;
}

+ (void) initHandler {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env) {
        jclass klazz = (*env)->FindClass(env, "com/sun/media/jfxmedia/logging/Logger");

        if (NULL != klazz) {
            javaLoggerClass = (jclass) (*env)->NewWeakGlobalRef(env, klazz);
            (*env)->DeleteLocalRef(env, klazz);

            if (NULL != javaLoggerClass) {
                logMsg1Method = (*env)->GetStaticMethodID(env, javaLoggerClass, "logMsg", "(ILjava/lang/String;)V");
                logMsg2Method = (*env)->GetStaticMethodID(env, javaLoggerClass, "logMsg", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
            }
        }

        if (attached) {
            detachThread();
        }
    }
}

+ (void) logMsg: (int) level
        message: (const char *) msg {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env && (NULL != logMsg1Method)) {
        jstring jmsg = (*env)->NewStringUTF(env, msg);
        (*env)->CallStaticVoidMethod(env, javaLoggerClass, logMsg1Method, (jint) level, jmsg);

        if (attached) {
            detachThread();
        }
    }
}

+ (void) logMsg: (int) level
    sourceClass: (const char *) sourceClass
         method: (const char *) sourceMethod
        message: (const char *) msg {

    bool attached;
    JNIEnv *env = media_getEnv(&attached);

    if (env && (NULL != logMsg2Method)) {

        jstring jsourceClass = (*env)->NewStringUTF(env, sourceClass);
        jstring jsourceMethod = (*env)->NewStringUTF(env, sourceMethod);
        jstring jmsg = (*env)->NewStringUTF(env, msg);

        (*env)->CallStaticVoidMethod(env, javaLoggerClass, logMsg2Method,
                                     (jint) level, jsourceClass, jsourceMethod, jmsg);
        if (attached) {
            detachThread();
        }
    }
}

+ (void) logError: (NSError *) error {

    NSString *strError = [NSString stringWithFormat: @"%@", error];

    [ErrorHandler logMsg: LOGGER_ERROR
                 message: [strError UTF8String]];
}

+ (jint) mapAVErrorToFXError: (NSError *) error {

    jint jfxError = ERROR_NONE;

    switch ([error code]) {
        case AVErrorUnknown:
            jfxError = ERROR_BASE_JNI; // what to return in this case?
            break;
        case AVErrorFileFormatNotRecognized:
            jfxError = ERROR_LOCATOR_UNSUPPORTED_MEDIA_FORMAT;
            break;
        case AVErrorOutOfMemory:
            jfxError = ERROR_MEMORY_ALLOCATION;
            break;
        case AVErrorInvalidSourceMedia:
            jfxError = ERROR_MEDIA_INVALID;
            break;
        case AVErrorDecoderNotFound:
            jfxError = ERROR_LOCATOR_UNSUPPORTED_MEDIA_FORMAT; // something like ERROR_GSTREAMER_AUDIO_DECODER_SINK_PAD
            break;
        case AVErrorFileFailedToParse:
            jfxError = ERROR_MEDIA_CORRUPTED;
            break;
        case AVErrorDecodeFailed:
            jfxError = ERROR_MEDIA_CORRUPTED;
            break;
        case AVErrorSessionNotRunning:                                // recording related
        case AVErrorSessionWasInterrupted:                            // recording related
        case AVErrorDiskFull:                                        // recording related
        case AVErrorNoDataCaptured:                                    // recording related
        case AVErrorExportFailed:
        case AVErrorFileAlreadyExists:                                // recording related
        case AVErrorMaximumStillImageCaptureRequestsExceeded:        // photo culd not be taken
        case AVErrorCompositionTrackSegmentsNotContiguous:            // composition related
        case AVErrorInvalidCompositionTrackSegmentDuration:            // composition related
        case AVErrorInvalidCompositionTrackSegmentSourceStartTime:    // composition related
        case AVErrorInvalidCompositionTrackSegmentSourceDuration:    // composition related
        case AVErrorDeviceAlreadyUsedByAnotherSession:                // recording related
        case AVErrorDeviceWasDisconnected:                            // recording related
        case AVErrorSessionConfigurationChanged:                    // recording related
        case AVErrorMediaChanged:                                    // recording related
        case AVErrorMediaDiscontinuity:                                // recording related
        case AVErrorMaximumDurationReached:                            // recording related
        case AVErrorMaximumFileSizeReached:                            // recording related
        case AVErrorMaximumNumberOfSamplesForFileFormatReached:        // recording related

        case AVErrorDeviceNotConnected:
        case AVErrorDeviceInUseByAnotherApplication:
        case AVErrorDeviceLockedForConfigurationByAnotherProcess:

        case AVErrorMediaServicesWereReset:                            // operation could not be completed

        case AVErrorContentIsProtected:                                // protected content
        case AVErrorContentIsNotAuthorized:                            // protected content
        case AVErrorApplicationIsNotAuthorized:                        // protected content

        case AVErrorNoImageAtTime:                                    // no video frame available ?
        case AVErrorEncoderNotFound:                                // encoding related

        case AVErrorDeviceIsNotAvailableInBackground:                // attempted to play capture session in background

        case AVErrorOperationNotSupportedForAsset:
        case AVErrorDecoderTemporarilyUnavailable:
        case AVErrorEncoderTemporarilyUnavailable:
        case AVErrorInvalidVideoComposition:
        case AVErrorReferenceForbiddenByReferencePolicy:

        default:
            //NSLog(@"ErrorHandler: don't know how to handle error %@", error);
            break;
    }

    return jfxError;
}


@end

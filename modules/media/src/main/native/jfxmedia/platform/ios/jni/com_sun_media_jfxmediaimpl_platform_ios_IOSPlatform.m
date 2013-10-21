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

#include "com_sun_media_jfxmediaimpl_platform_ios_IOSPlatform.h"
#include "jfxmedia_errors.h"

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <AudioToolbox/AudioToolbox.h>

#import "debug.h"

#ifdef __cplusplus
extern "C" {
#endif

    /* Initialize the Java VM instance variable when the library is first loaded */
    JavaVM *jvm;

    /*
     * Specify the required JNI version.
     */
    JNIEXPORT jint JNICALL JNI_OnLoad_jfxmedia(JavaVM *vm, void *reserved) {

        jvm = vm;
#ifdef JNI_VERSION_1_8
        //min. returned JNI_VERSION required by JDK8 for builtin libraries
        JNIEnv *env;
        if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
            return JNI_VERSION_1_4;
        }
        return JNI_VERSION_1_8;
#else
        return JNI_VERSION_1_2;
#endif
    }

    void interruptionListenerCallback(void *inUserData, UInt32 interruptionState) {
        // TODO: take care of interruption
        // http://javafx-jira.kenai.com/browse/RT-27005
    }

    jint initAudioSession() {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        AudioSessionInitialize (
                                NULL,
                                NULL,
                                interruptionListenerCallback,
                                NULL /* userData TBD */
                                );

        jint error = ERROR_NONE;

        AVAudioSession *session = [AVAudioSession sharedInstance];
        NSError *setCategoryError = nil;

        UInt32 otherAudioIsPlaying;
        UInt32 propertySize = sizeof(otherAudioIsPlaying);

        AudioSessionGetProperty(
                                kAudioSessionProperty_OtherAudioIsPlaying,
                                &propertySize,
                                &otherAudioIsPlaying);

        if (otherAudioIsPlaying) {
            [session setCategory: AVAudioSessionCategoryAmbient
                           error: &setCategoryError];
        } else {
            [session setCategory: AVAudioSessionCategorySoloAmbient
                           error: &setCategoryError];
        }

        /* TODO: add fine-grained error handling http://javafx-jira.kenai.com/browse/RT-27005
         if (setCategoryError) {
         //error = ERROR_MANAGER_ENGINEINIT_FAIL;
         }*/

        [pool release];

        return error;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSPlatform
     * Method:    iosPlatformInit
     * Signature: ()V
     */
    JNIEXPORT void JNICALL
    Java_com_sun_media_jfxmediaimpl_platform_ios_IOSPlatform_iosPlatformInit(JNIEnv *env, jclass jc) {

        const jint error = initAudioSession();

        // TODO: send error to Java
        // http://javafx-jira.kenai.com/browse/RT-27005
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_NativeMediaManager
     * Method:    nativeCanPlayContentType
     * Signature: (Ljava/lang/String;)Z
     */
    /*
     JNIEXPORT jboolean JNICALL Java_com_sun_media_jfxmediaimpl_NativeMediaManager_nativeCanPlayContentType
     (JNIEnv *env, jclass klazz, jstring js_content) {

     NSLog(@"Java_com_sun_media_jfxmediaimpl_NativeMediaManager_nativeCanPlayContentType");

     NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

     static NSArray *supportedFormats = nil;

     if (supportedFormats == nil) {
     supportedFormats = [[NSArray arrayWithObjects:

     @"audio/3gpp",        // 3GPP media (.3gp, .3gpp)
     @"audio/3gpp2",        // 3GPP2 media (.3g2, .3gp2)
     @"audio/aiff",        // AIFF audio (.aiff, .aif, .aifc, .cdda)
     @"audio/x-aiff",
     @"audio/amr",        // AMR audio (.amr)
     @"audio/mp3",        // MP3 audio (.mp3, .swa)
     @"audio/mpeg3",
     @"audio/x-mp3",
     @"audio/x-mpeg3",
     @"audio/mp4",        // MPEG-4 media (.mp4)
     @"audio/mpeg",        // MPEG audio (.mpeg, .mpg, .mp3, .swa)
     @"audio/x-mpeg",
     @"audio/wav",        // WAVE audio (.wav, .bwf)
     @"audio/x-wav",
     @"audio/x-m4a",        // AAC audio (.m4a)
     @"audio/x-m4b",        // AAC audio book (.m4b)
     @"audio/x-m4p",        // AAC audio protected (.m4p)
     @"video/3gpp",        // 3GPP media (.3gp, .3gpp)
     @"video/3gpp2",        // 3GPP2 media (.3g2, .3gp2)
     @"video/mp4",        // MPEG-4 media (.mp4)
     @"video/quicktime",    // QuickTime Movie (.mov, .qt, .mqv)
     @"video/x-m4v",        // Video (.m4v)

     nil]
     retain];
     }

     const char *contentNativeString = (*env)->GetStringUTFChars(env, js_content, 0);

     NSString *requestedContent = [NSString stringWithCString: contentNativeString
     encoding: NSUTF8StringEncoding];

     BOOL isSupported = FALSE;

     NSEnumerator *enumerator = [supportedFormats objectEnumerator];
     id format;
     while (format = [enumerator nextObject]) {
     if ([requestedContent isEqualToString: format]) {
     NSLog(@"Content type %s is supported", contentNativeString);
     isSupported = TRUE;
     break;
     }
     }

     (*env)->ReleaseStringUTFChars(env, js_content, contentNativeString);

     [pool release];

     return (jboolean) isSupported;
     }
     */

#ifdef __cplusplus
}
#endif

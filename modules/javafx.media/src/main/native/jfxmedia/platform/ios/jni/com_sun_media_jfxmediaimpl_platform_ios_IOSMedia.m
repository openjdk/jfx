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

#include "com_sun_media_jfxmediaimpl_platform_ios_IOSMedia.h"
#include "Media.h"
#include "JniUtils.h"
#include "jfxmedia_errors.h"

#import "debug.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMedia
     * Method:    iosInitNativeMedia
     * Signature: (Lcom/sun/media/jfxmedia/locator/Locator;Ljava/lang/String;J[J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMedia_iosInitNativeMedia
    (JNIEnv *env, jobject obj,
     jobject locator,
     jstring contentType,
     jlong sizeHint,
     jlongArray nativeMediaHandleArr) {

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        Media *media = NULL;
        jclass klass = (*env)->GetObjectClass(env, locator);
        jmethodID mid = (*env)->GetMethodID(
                                            env,
                                            klass,
                                            "getStringLocation",
                                            "()Ljava/lang/String;");

        if (mid != 0) {

            jstring uriJavaString = (*env)->CallObjectMethod(env, locator, mid);

            const char *uriNativeString = (*env)->GetStringUTFChars(env, uriJavaString, 0);

            media = [[Media alloc] initMedia:
                     [NSString stringWithCString: uriNativeString
                                        encoding: NSUTF8StringEncoding]];

            (*env)->ReleaseStringUTFChars(env, uriJavaString, uriNativeString);
            (*env)->DeleteLocalRef(env, klass);
        }

        jlong handle = ptr_to_jlong(media);

        (*env)->SetLongArrayRegion(env, nativeMediaHandleArr, 0, 1, &handle);

        [pool release];

        // TODO: check for errors and return an appropriate code
        // http://javafx-jira.kenai.com/browse/RT-27005
        return ERROR_NONE;
    }

    /*
     * Class:     com_sun_media_jfxmediaimpl_platform_ios_IOSMedia
     * Method:    iosDispose
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_ios_IOSMedia_iosDispose
    (JNIEnv *env, jobject obj, jlong ref) {

        Media *media = jlong_to_ptr(ref);

        if (NULL != media) {
            [media dispose];
        }
    }

#ifdef __cplusplus
}
#endif

/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "jni_utils.h"
#include "com_sun_javafx_iio_ios_IosImageLoader.h"

#import "ImageLoader.h"

#include "debug.h"

#define BUFFER_SIZE 8 * 1024


// NOTE:
// It seems to be inefficient to decode the stream progressively.
// Thus we download the input stream to memory at once and decompress it then.

// Maybe we could get motivated here:
//http://www.cocoaintheshell.com/2011/05/progressive-images-download-imageio/


#ifdef __cplusplus
extern "C" {
#endif

    static jmethodID InputStream_readID;
    static jmethodID InputStream_skipID;
    static jmethodID IosImageLoader_setInputParametersID;
    static jmethodID IosImageLoader_updateImageProgressID;

    /*
     * Class:     com_sun_javafx_iio_ios_IosImageLoader
     * Method:    initNativeLoading
     * Signature: ()V
     */
    JNIEXPORT void JNICALL Java_com_sun_javafx_iio_ios_IosImageLoader_initNativeLoading
    (JNIEnv *env, jclass klazz) {

        IIOLog(@"IosImageLoader_initNativeLoading");

        jclass cls_InputStream = (*env)->FindClass(env, "java/io/InputStream");
        if (cls_InputStream == NULL) {

            return; // can't find/load the class
            // should throw an exception in Java
        }

        InputStream_readID = (*env)->GetMethodID(env,
                                                 cls_InputStream,
                                                 "read",
                                                 "([BII)I");
        InputStream_skipID = (*env)->GetMethodID(env,
                                                 cls_InputStream,
                                                 "skip",
                                                 "(J)J");

        IosImageLoader_setInputParametersID = (*env)->GetMethodID(env,
                                                                  klazz,
                                                                  "setInputParameters",
                                                                  "(III)V");
        IosImageLoader_updateImageProgressID = (*env)->GetMethodID(env,
                                                                   klazz,
                                                                   "updateProgress",
                                                                   "(F)V");

        (*env)->DeleteLocalRef(env, cls_InputStream);
    }

    /*
     * Class:     com_sun_javafx_iio_ios_IosImageLoader
     * Method:    loadImage
     * Signature: (Ljava/lang/String;Z)J
     */
    JNIEXPORT jlong JNICALL Java_com_sun_javafx_iio_ios_IosImageLoader_loadImageFromURL
    (JNIEnv *env, jobject obj, jstring jStringURL, jboolean reportProgress) {

        IIOLog(@"IosImageLoader_loadImageFromURL");

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        ImageLoader *loader = [[ImageLoader alloc] init];

        const char *urlNativeString = (*env)->GetStringUTFChars(env, jStringURL, 0);
        NSString *url = [NSString stringWithCString : urlNativeString
                                           encoding : NSUTF8StringEncoding];
        (*env)->ReleaseStringUTFChars(env, jStringURL, urlNativeString);

        IIOLog(@"Image URL: %@", url);

        BOOL isLoaded = [loader loadFromURL : url
                                     JNIEnv : env];

        if (!isLoaded) {
            [pool release];
            return 0L;
        }

        (*env)->CallVoidMethod(env,
                               obj,
                               IosImageLoader_setInputParametersID,
                               (jint) [loader width],
                               (jint) [loader height],
                               (jint) [loader nImages]);

        (*env)->CallVoidMethod(env,
                               obj,
                               IosImageLoader_updateImageProgressID,
                               100.0f);

        [pool release];

        return ptr_to_jlong(loader);

    }

    /*
     * Class:     com_sun_javafx_iio_ios_IosImageLoader
     * Method:    loadImage
     * Signature: (Ljava/io/InputStream;Z)J
     */
    JNIEXPORT jlong JNICALL Java_com_sun_javafx_iio_ios_IosImageLoader_loadImage
    (JNIEnv *env, jobject obj, jobject inputStream, jboolean reportProgress) {

        IIOLog(@"IosImageLoader_loadImage");

        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

        jbyteArray buffer = (*env)->NewByteArray(env, BUFFER_SIZE);
        if (buffer == NULL) {
            throwException(env,
                           "java/lang/OutOfMemoryError",
                           "Cannot initilialize memory buffer for native image loader");

            [pool release];
            return 0L;
        }

        ImageLoader *loader = [[ImageLoader alloc] init];

        int bytesRead = 0;
        jboolean iscopy = FALSE;
        jbyte *primitiveArray = (*env)->GetPrimitiveArrayCritical(env, buffer, &iscopy);

        do {
            bytesRead = (*env)->CallIntMethod(env,
                                              inputStream,
                                              InputStream_readID,
                                              buffer,
                                              0,
                                              (jint) BUFFER_SIZE);
            if (bytesRead != -1) {
                [loader addToBuffer : (const unsigned char *) primitiveArray
                             length : bytesRead];
            }
        } while (bytesRead != -1);

        BOOL isLoaded = [loader loadFromBuffer : env];

        (*env)->ReleasePrimitiveArrayCritical(env, buffer, primitiveArray, JNI_ABORT);

        if (!isLoaded) {
            [pool release];
            return 0L;
        }

        (*env)->CallVoidMethod(env,
                               obj,
                               IosImageLoader_setInputParametersID,
                               (jint) [loader width],
                               (jint) [loader height],
                               (jint) [loader nImages]);

        (*env)->CallVoidMethod(env,
                               obj,
                               IosImageLoader_updateImageProgressID,
                               100.0f);

        [pool release];

        return ptr_to_jlong(loader);
    }


    /*
     * Class:     com_sun_javafx_iio_ios_IosImageLoader
     * Method:    resizeImage
     * Signature: (JII)V
     */
    JNIEXPORT void JNICALL Java_com_sun_javafx_iio_ios_IosImageLoader_resizeImage
    (JNIEnv *env, jobject obj, jlong loaderRef, jint width, jint height) {

        IIOLog(@"IosImageLoader_resizeImage");

        ImageLoader *loader = (ImageLoader *) jlong_to_ptr(loaderRef);

        [loader resize : (int) width
                       : (int) height];
    }

    /*
     * Class:     com_sun_javafx_iio_ios_IosImageLoader
     * Method:    getImageBuffer
     * Signature: (JI)[B
     */
    JNIEXPORT jbyteArray JNICALL Java_com_sun_javafx_iio_ios_IosImageLoader_getImageBuffer
    (JNIEnv *env, jobject obj, jlong loaderRef, jint imageIndex) {

        IIOLog(@"IosImageLoader_getImageBuffer for image number %ld", imageIndex);

        ImageLoader *loader = (ImageLoader *) jlong_to_ptr(loaderRef);

        return [loader getDecompressedBuffer : env
                                  imageIndex : imageIndex];

    }

    /*
     * Class:     com_sun_javafx_iio_ios_IosImageLoader
     * Method:    getNumberOfComponents
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_javafx_iio_ios_IosImageLoader_getNumberOfComponents
    (JNIEnv *env, jobject obj, jlong loaderRef) {

        IIOLog(@"IosImageLoader_getNumberOfComponents");

        ImageLoader *loader = (ImageLoader *) jlong_to_ptr(loaderRef);

        return (jint) [loader nComponents];
    }

    /*
     * Class:     com_sun_javafx_iio_ios_IosImageLoader
     * Method:    getColorSpaceCode
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_javafx_iio_ios_IosImageLoader_getColorSpaceCode
    (JNIEnv *env, jobject obj, jlong loaderRef) {

        IIOLog(@"IosImageLoader_getColorSpaceCode");

        ImageLoader *loader = (ImageLoader *) jlong_to_ptr(loaderRef);

        return (jint) [loader colorSpace];
    }

    /*
     * Class:     com_sun_javafx_iio_ios_IosImageLoader
     * Method:    getDelayTime
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_sun_javafx_iio_ios_IosImageLoader_getDelayTime
    (JNIEnv *env, jobject obj, jlong loaderRef) {

        IIOLog(@"IosImageLoader_getDelayTime");

        ImageLoader *loader = (ImageLoader *) jlong_to_ptr(loaderRef);

        return (jint) [loader delayTime];
    }

    /*
     * Class:     com_sun_javafx_iio_ios_IosImageLoader
     * Method:    disposeLoader
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_sun_javafx_iio_ios_IosImageLoader_disposeLoader
    (JNIEnv *env, jclass klazz, jlong loaderRef) {

        IIOLog(@"IosImageLoader_disposeLoader");

        ImageLoader *loader = (ImageLoader *) jlong_to_ptr(loaderRef);

        [loader release];
    }

#ifdef __cplusplus
}
#endif

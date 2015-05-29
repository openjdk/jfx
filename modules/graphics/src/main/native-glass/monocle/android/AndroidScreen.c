/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include <android/native_window_jni.h>
// #include "activity.h"
#include "dalvikInput.h"
#include "com_sun_glass_ui_monocle_AndroidScreen.h"
#include "Monocle.h"
#include "logging.h"


/*
 * Class:     com_sun_glass_ui_monocle_AndroidScreen
 * Method:    _getWidth
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_AndroidScreen__1getWidth
  (JNIEnv *env, jobject obj) {
    ANativeWindow* androidWindow = android_getNativeWindow(env);
    if (androidWindow == NULL) {
        return 0;
    }
    int32_t width = ANativeWindow_getWidth(androidWindow);
    return width;
}

/*
 * Class:     com_sun_glass_ui_monocle_AndroidScreen
 * Method:    _getHeight
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_AndroidScreen__1getHeight
  (JNIEnv *env, jobject obj) {
    ANativeWindow* androidWindow = android_getNativeWindow(env);
    if (androidWindow == NULL) {
        return 0;
    }
    int32_t height = ANativeWindow_getHeight(androidWindow);
    return height;
}

/*
 * Class:     com_sun_glass_ui_monocle_AndroidScreen
 * Method:    _getNativeHandle
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_AndroidScreen__1getNativeHandle
  (JNIEnv *env, jobject obj) {
    ANativeWindow* androidWindow = android_getNativeWindow(env);
    return androidWindow;
}

/*
 * Class:     com_sun_glass_ui_monocle_AndroidScreen
 * Method:    _getDensity
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_sun_glass_ui_monocle_AndroidScreen__1getDensity
  (JNIEnv *env, jobject obj) {
    jfloat answer = android_getDensity();
LOGI("DENSITY", "GETDENSITY, answer = %f\n",answer);
    return answer;
}


/*
 * Class:     com_sun_glass_ui_monocle_AndroidScreen
 * Method:    _shutdown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_AndroidScreen__1shutdown
  (JNIEnv *env, jobject obj) {
}

/*
 * Class:     com_sun_glass_ui_monocle_AndroidScreen
 * Method:    _uploadPixels
 * Signature: (Ljava/nio/Buffer;IIIIF)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_AndroidScreen__1uploadPixels
  (JNIEnv *env, jobject obj, jobject obj2, jint ji1, jint ji2, jint ji3, jint j4, jfloat jf1) {
}

/*
 * Class:     com_sun_glass_ui_monocle_AndroidScreen
 * Method:    _swapBuffers
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_AndroidScreen__1swapBuffers
  (JNIEnv *env, jobject obj) {
}

/*
 * Class:     com_sun_glass_ui_monocle_AndroidScreen
 * Method:    _getScreenCapture
 * Signature: ()Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_monocle_AndroidScreen__1getScreenCapture
  (JNIEnv *env, jobject obj) {
    return NULL;
}



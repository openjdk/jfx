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
 
#include "LensCommon.h"
#include "com_sun_glass_ui_lens_LensRobot.h"

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensRobot_postScrollEvent
(JNIEnv *env, jobject _this, jint wheelAmt) {

    jboolean isSuccessful =
        glass_robot_postScrollEvent(env, wheelAmt);

    if (!isSuccessful) {
        glass_throw_exception_by_name(env, glass_RuntimeException,
                                      "Failed to post scroll event");
    }
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensRobot_postKeyEvent
(JNIEnv *env, jobject _this, jint keyEventType, jint jfxKeyCode) {

    jboolean isSuccessful = glass_robot_postKeyEvent(env, keyEventType, jfxKeyCode);

    if (!isSuccessful) {
        glass_throw_exception_by_name(env, glass_RuntimeException,
                                      "Failed to post key event");
    }
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensRobot_postMouseEvent
(JNIEnv *env, jobject _this, jint mouseEventType, jint x, jint y, jint buttons) {

    jboolean isSuccessful =
        glass_robot_postMouseEvent(env, mouseEventType,
                                   x, y, buttons);

    if (!isSuccessful) {
        glass_throw_exception_by_name(env, glass_RuntimeException,
                                      "Failed to post mouse event");
    }
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_lens_LensRobot_getMouseLocation
(JNIEnv *env, jobject _this, jint axsis) {

    jint x;
    jint y;
    jint result;
    jboolean isSuccessful = glass_robot_getMouseLocation(&x, &y);

    if (isSuccessful) {
        result = (axsis == com_sun_glass_ui_lens_LensRobot_GET_X) ? x : y;
    } else {
        glass_throw_exception_by_name(env, glass_RuntimeException,
                                      "Failed to get mouse location");
        result = -1;
    }

    return result;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_lens_LensRobot__1getPixelColor
(JNIEnv *env, jobject _this, jint x, jint y) {

    jint pixelColor = 0;
    GLASS_LOG_FINEST("Getting pixel at %i,%i", x, y);
    jboolean isSuccessful = glass_screen_capture(x, y, 1, 1, &pixelColor);
    GLASS_LOG_FINEST("PixelColor = 0x08%x", pixelColor);

    if (!isSuccessful) {
        glass_throw_exception_by_name(env, glass_RuntimeException,
                                      "Failed to get pixel color");
    }

    return pixelColor;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensRobot__1getScreenCapture
(JNIEnv *env, jobject _this, jint x, jint y,
 jint width, jint height, jintArray data) {

    GLASS_LOG_FINEST("Capturing screen region %i,%i+%ix%i",
                     x, y, width, height);

    jint pixelsBufferLength = width * height;
    jint *pixels = (jint *)malloc(sizeof(jint) * pixelsBufferLength);
    GLASS_LOG_FINEST("Allocated pixel offer at %p, size=%i bytes",
                     pixels, sizeof(jint) * pixelsBufferLength);
    jboolean isSuccessful;

    if (pixels != NULL) {


        isSuccessful = glass_screen_capture(x, y, width, height, pixels);
        if (isSuccessful) {
            GLASS_LOG_FINEST("JNI SetIntArrayRegion");
            (*env)->SetIntArrayRegion(env, data, 0, pixelsBufferLength, pixels);
        } else {
            glass_throw_exception_by_name(env, glass_RuntimeException,
                                          "Failed to capture screen");
        }

        GLASS_LOG_FINEST("free(%p)", pixels);
        free(pixels);
    } else {
        glass_throw_exception_by_name(env, "java/lang/OutOfMemoryError",
                                      "Failed to allocate a buffer for"
                                      " screen capture");
    }
}

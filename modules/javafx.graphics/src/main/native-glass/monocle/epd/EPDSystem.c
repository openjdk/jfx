/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

// Implementation of native methods in EPDSystem.java

#include <sys/ioctl.h>  // For ioctl
#include <sys/types.h>  // For uint
#include <linux/fb.h>   // For fb_var_screeninfo

#include "com_sun_glass_ui_monocle_EPDSystem.h"
#include "com_sun_glass_ui_monocle_EPDSystem_FbVarScreenInfo.h"

#include "Monocle.h"

// EPDSystem

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_ioctl
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong fd, jint request, jint value) {
    return ioctl((int) fd, (int) request, (__u32 *) & value);
}

// EPDSystem.FbVarScreenInfo

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getGrayscale
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->grayscale;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getRedOffset
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->red.offset;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getRedLength
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->red.length;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getRedMsbRight
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->red.msb_right;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getGreenOffset
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->green.offset;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getGreenLength
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->green.length;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getGreenMsbRight
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->green.msb_right;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getBlueOffset
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->blue.offset;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getBlueLength
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->blue.length;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getBlueMsbRight
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->blue.msb_right;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getTranspOffset
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->transp.offset;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getTranspLength
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->transp.length;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getTranspMsbRight
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->transp.msb_right;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getNonstd
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->nonstd;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getActivate
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->activate;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getHeight
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->height;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getWidth
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->width;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getAccelFlags
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->accel_flags;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getPixclock
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->pixclock;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getLeftMargin
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->left_margin;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getRightMargin
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->right_margin;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getUpperMargin
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->upper_margin;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getLowerMargin
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->lower_margin;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getHsyncLen
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->hsync_len;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getVsyncLen
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->vsync_len;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getSync
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->sync;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getVmode
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->vmode;
}

JNIEXPORT jint JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_getRotate
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p) {
    return (jint) ((struct fb_var_screeninfo *) asPtr(p))->rotate;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setGrayscale
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint grayscale) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->grayscale = grayscale;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setNonstd
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint nonstd) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->nonstd = nonstd;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setHeight
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint height) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->height = height;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setWidth
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint width) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->width = width;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setAccelFlags
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint accelFlags) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->accel_flags = accelFlags;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setPixclock
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint pixclock) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->pixclock = pixclock;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setLeftMargin
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint leftMargin) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->left_margin = leftMargin;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setRightMargin
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint rightMargin) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->right_margin = rightMargin;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setUpperMargin
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint upperMargin) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->upper_margin = upperMargin;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setLowerMargin
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint lowerMargin) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->lower_margin = lowerMargin;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setHsyncLen
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint hsyncLen) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->hsync_len = hsyncLen;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setVsyncLen
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint vsyncLen) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->vsync_len = vsyncLen;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setSync
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint sync) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->sync = sync;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setVmode
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint vmode) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->vmode = vmode;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_EPDSystem_00024FbVarScreenInfo_setRotate
(JNIEnv *UNUSED(env), jobject UNUSED(object), jlong p, jint rotate) {
    struct fb_var_screeninfo *ptr = (struct fb_var_screeninfo *) asPtr(p);
    ptr->rotate = rotate;
}

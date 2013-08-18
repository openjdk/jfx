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
 
#include "input/LensInput.h"

#if defined(OMAP3)

#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <linux/fb.h>
#include <linux/omapfb.h>
#include <sys/ioctl.h>

#include "platform-util/platformUtil.h"

static void fbCreateCursor(jbyte *cursorImage, int width, int height, int bpp) {
    if (fbPlatformCreateCursor) {
        (*fbPlatformCreateCursor)(cursorImage, width, height, bpp);
    } 
    else {fprintf(stderr, "missing native fbPlatformCreateCursor"); }
}

void fbCursorInitialize(int screenWidth, int screenHeight) {
    if (fbPlatformCursorInitialize) {
        (*fbPlatformCursorInitialize)(screenWidth, screenHeight);
    }
    else {fprintf(stderr, "missing native fbPlatformCursorInitialize"); }
}

void fbCursorSetPosition(int x, int y) {

    if (fbPlatformCursorSetPosition) {
        return (*fbPlatformCursorSetPosition)(x, y);
    }
    else {fprintf(stderr, "missing native fbPlatformCursorSetPosition"); }
}


void fbCursorClose() {
    if (fbPlatformCursorClose) {
        (*fbPlatformCursorClose)();
    }
    else {fprintf(stderr, "missing native fbPlatformCursorClose"); }
}

void glass_cursor_setVisible(jboolean isVisible) {
    if (fbPlatformSetVisible) {
        (*fbPlatformSetVisible)(isVisible);
    }
    else {fprintf(stderr, "missing native fbPlatformSetVisible"); }
}

void glass_cursor_setNativeCursor(jlong nativeCursorPointer) {
    if (fbPlatformSetNativeCursor) {
        (*fbPlatformSetNativeCursor)(nativeCursorPointer);
    }
    else {fprintf(stderr, "missing native fbPlatformSetNativeCursor"); }
}

void glass_cursor_releaseNativeCursor(jlong nativeCursorPointer) {
    if (fbPlatformReleaseNativeCursor) {
         (*fbPlatformReleaseNativeCursor)(nativeCursorPointer);
    }
    else {fprintf(stderr, "missing native fbPlatformReleaseNativeCursor"); }
}


jlong glass_cursor_createNativeCursor(JNIEnv *env, jint x, jint y, jbyte *srcArray, jint width, jint height) {
    if (fbPlatformCreateNativeCursor) {
        return (*fbPlatformCreateNativeCursor)(env, x, y, srcArray, width, height);
    } else {
       fprintf(stderr, "missing native fbPlatformCreateNativeCursor"); 
        return 0;
    } 
}


jboolean glass_cursor_supportsTranslucency() {
    return fbPlatformCursorTranslucency;
}



void glass_cursor_terminate(void) {
    fbCursorClose();
}

#else /* !defined(OMAP3) */

void fbCursorInitialize(int screenWidth, int screenHeight) { }
void fbCursorSetPosition(int x, int y) { }
void fbCursorClose() { }


void glass_cursor_setVisible(jboolean isVisible) {}
void glass_cursor_setNativeCursor(jlong nativeCursorPointer) {}
void glass_cursor_releaseNativeCursor(jlong nativeCursorPointer) {}
jlong glass_cursor_createNativeCursor(JNIEnv *env, jint x, jint y, jbyte *srcArray, jint width, jint height) {
    return 0;
}
jboolean glass_cursor_supportsTranslucency() {
    return 0;
}
void glass_cursor_terminate(void) {}

#endif

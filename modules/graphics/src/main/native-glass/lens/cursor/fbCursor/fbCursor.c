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

#if defined(OMAP3) || defined(IMX6_PLATFORM)

#include <assert.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <linux/fb.h>
#include <linux/omapfb.h>
#include <sys/ioctl.h>

static void fbCreateCursor(jbyte *cursorImage, int width, int height, int bpp) {
    assert (lensPort.createCursor);
    (*lensPort.createCursor)(cursorImage, width, height, bpp);
}

void fbCursorInitialize(int screenWidth, int screenHeight) {
    assert (lensPort.cursorInitialize);
    (*lensPort.cursorInitialize)(screenWidth, screenHeight);
}

void fbCursorSetPosition(int x, int y) {
    assert (lensPort.cursorSetPosition);
    return (*lensPort.cursorSetPosition)(x, y);
}


void fbCursorClose() {
    assert (lensPort.cursorClose);
    (*lensPort.cursorClose)();
}

void glass_cursor_setVisible(jboolean isVisible) {
    assert (lensPort.setVisible);
    (*lensPort.setVisible)(isVisible);
}

void glass_cursor_setNativeCursor(jlong nativeCursorPointer) {
    assert (lensPort.setNativeCursor);
    (*lensPort.setNativeCursor)(nativeCursorPointer);
}

void glass_cursor_releaseNativeCursor(jlong nativeCursorPointer) {
    assert (lensPort.releaseNativeCursor);
    (*lensPort.releaseNativeCursor)(nativeCursorPointer);
}


jlong glass_cursor_createNativeCursor(JNIEnv *env, jint x, jint y, jbyte *srcArray, jint width, jint height) {
    assert (lensPort.createNativeCursor);
    return (*lensPort.createNativeCursor)(env, x, y, srcArray, width, height);
}


jboolean glass_cursor_supportsTranslucency() {
    return lensPort.cursorTranslucency;
}


void glass_cursor_terminate(void) {
    fbCursorClose();
}

#else /* #if defined(OMAP3) || defined(IMX6_PLATFORM) */

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

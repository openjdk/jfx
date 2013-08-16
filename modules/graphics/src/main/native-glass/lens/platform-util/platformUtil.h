/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>

#define FB_DEVICE "/dev/fb0"

#define FB_CURSOR_DEVICE "/dev/fb1"
#define LENSFB_CURSOR_COLOR_KEY 0xABABABAB

#if defined(OMAP3)
#include <linux/omapfb.h>
#endif

#ifndef FB_PLATFORM_DECLARE
#define FB_PLATFORM_DECLARE extern
#endif

// Called to intitialize the platform specific functions.
// Must be called prior to other utility calls.
extern void platform_initialize();

// Cursor Specific Entries
FB_PLATFORM_DECLARE void (*fbPlatformCreateCursor)(jbyte *cursorImage, int width, int height, int bpp);
FB_PLATFORM_DECLARE void (*fbPlatformSetNativeCursor)(jlong nativeCursorPointer);
FB_PLATFORM_DECLARE void (*fbPlatformCursorInitialize)(int screenWidth, int screenHeight);
FB_PLATFORM_DECLARE void (*fbPlatformCursorSetPosition)(int x, int y);
FB_PLATFORM_DECLARE void (*fbPlatformCursorClose)();
FB_PLATFORM_DECLARE void (*fbPlatformCursorTerminate)();
FB_PLATFORM_DECLARE jlong (*fbPlatformCreateNativeCursor)(JNIEnv *env, jint x, jint y,  jbyte *srcArray, jint width, jint height);
FB_PLATFORM_DECLARE void (*fbPlatformReleaseNativeCursor)(jlong nativeCursorPointer);
FB_PLATFORM_DECLARE void (*fbPlatformSetVisible)(jboolean isVisible);
FB_PLATFORM_DECLARE jboolean fbPlatformCursorTranslucency;
FB_PLATFORM_DECLARE void (*fbPlatformSetNativeCursor)(jlong nativeCursorPointer);
FB_PLATFORM_DECLARE jlong (*fbPlatformCreateNativeCursor)(JNIEnv *env, jint x, jint y,  jbyte *srcArray, jint width, jint height);
FB_PLATFORM_DECLARE void (*fbPlatformReleaseNativeCursor)(jlong nativeCursorPointer);
FB_PLATFORM_DECLARE void (*fbPlatformSetVisible)(jboolean isVisible);
FB_PLATFORM_DECLARE jboolean fbPlatformCursorTranslucency;
FB_PLATFORM_DECLARE void (*fbPlatformSetNativeCursor)(jlong nativeCursorPointer);
FB_PLATFORM_DECLARE void (*fbPlatformSetNativeCursor)(jlong nativeCursorPointer);

// Robot Specific Entries
FB_PLATFORM_DECLARE jboolean (*fbRobotScreenCapture)(jint x, jint y, jint width, jint height, jint *pixels);

/*
  todo:
     extern void load_bcm_symbols();
     static int loadedBcm = 0;
     #ifdef USE_DISPMAN
*/

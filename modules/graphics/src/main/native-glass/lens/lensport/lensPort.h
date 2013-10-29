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

#ifndef PLATFORM_UTIL_H
#define PLATFORM_UTIL_H

#include <jni.h>

#define NATIVE_LENS_PORT_VERSION 1
#define NATIVE_PRISM_PORT_VERSION 1

#define LENSPORT_LIBRARY_NAME "liblens_porting.so"

typedef void Platform_Logger(int level,
                const char *func,
                const char *file,
                int line,
                const char *format, ...);

typedef struct _lens_native_port {
    int version;
    char *platformName;
    void (*setLogger)(Platform_Logger *logger, int level);
    // Cursor Specific Entries
    void (*createCursor)(jbyte *cursorImage, int width, int height, int bpp);
    void (*setNativeCursor)(jlong nativeCursorPointer);
    void (*cursorInitialize)(int screenWidth, int screenHeight);
    void (*cursorSetPosition)(int x, int y);
    void (*cursorClose)();
    void (*cursorTerminate)();
    jlong (*createNativeCursor)(JNIEnv *env, jint x, jint y,  jbyte *srcArray, jint width, jint height);
    void (*releaseNativeCursor)(jlong nativeCursorPointer);
    void (*setVisible)(jboolean isVisible);
    jboolean cursorTranslucency;

    // Robot Specific Entries
    jboolean (*robotScreenCapture)(jint x, jint y, jint width, jint height, jint *pixels);
} LensNativePort;

typedef struct _prism_native_port {
    int version;
    char *platformName;
    //returns a EGLNativeWindowType
    void * (*getNativeWindowType)();
    //returns a EGLNativeDisplayType
    void * (*getNativeDisplayType)();
    void * (*wr_eglGetDisplay)(void *id);
    void * (*getLibGLEShandle)();
} PrismNativePort;

// Called to initialize the lens specific platform functions.
// Must be called prior to other utility calls.
// return of true on success
extern jboolean lens_platform_initialize(LensNativePort *lensPort);

// Called to initialize the prism specific platform functions.
// Must be called prior to other utility calls.
// return of true on success
extern jboolean prism_platform_initialize(PrismNativePort *prismsPort);

#endif // PLATFORM_UTIL_H

/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
#ifndef __EGL_EXT__
#define __EGL_EXT__
#include <jni.h>

// This header file declares functions that need to be provided by low-level
// drivers or libraries.

// get a handle to the native window (without specifying what window is)
extern jlong getNativeWindowHandle(const char *v);

// get a handle to the EGL display
extern jlong getEglDisplayHandle();

// initialize the EGL system with the specified handle
extern jboolean doEglInitialize(void* handle);

// bind a specific API to the EGL system
extern jboolean doEglBindApi(int api);

// instruct the system to choose an EGL configuration matching the provided attributes
extern jlong doEglChooseConfig (jlong eglDisplay, int* attribs);

// create an EGL Surface for the given display, configuration and window
extern jlong doEglCreateWindowSurface(jlong eglDisplay, jlong config,
     jlong nativeWindow);

// create an EGL Context for the given display and configuration
extern jlong doEglCreateContext(jlong eglDisplay, jlong config);

// enable the specified EGL system
extern jboolean doEglMakeCurrent(jlong eglDisplay, jlong drawSurface,
     jlong readSurface, jlong eglContext);

// swap buffers (and render frontbuffer)
extern jboolean doEglSwapBuffers(jlong eglDisplay, jlong eglSurface);

// get the number of native screens in the current configuration
extern jint doGetNumberOfScreens();

// get specific information about each screen
// the idx parameter specifies which screen needs to be queried
extern jlong doGetHandle(jint idx);
extern jint doGetDepth(jint idx);
extern jint doGetWidth(jint idx);
extern jint doGetHeight(jint idx);
extern jint doGetOffsetX(jint idx);
extern jint doGetOffsetY(jint idx);
extern jint doGetDpi(jint idx);
extern jint doGetNativeFormat(jint idx);
extern jfloat doGetScale(jint idx);

// initialize a hardware cursor with specified dimensions
extern void doInitCursor(jint width, jint height);

// show/hide the hardware cursor
extern void doSetCursorVisibility(jboolean val);

// point the hardware cursor to the provided location
extern void doSetLocation(jint x, jint y);

// use the specified image as cursor image
extern void doSetCursorImage(jbyte* img, int length);

#endif // EGL_EXT

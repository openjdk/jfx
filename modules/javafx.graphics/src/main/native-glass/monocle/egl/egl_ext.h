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
extern jlong getNativeWindowHandle(const char *v);
extern jlong getEglDisplayHandle();
extern jboolean doEglInitialize(void* handle);
extern jboolean doEglBindApi(int api);
extern jlong doEglChooseConfig (jlong eglDisplay, int* attribs);

extern jlong doEglCreateWindowSurface(jlong eglDisplay, jlong config,
     jlong nativeWindow);

extern jlong doEglCreateContext(jlong eglDisplay, jlong config);

extern jboolean doEglMakeCurrent(jlong eglDisplay, jlong drawSurface,
     jlong readSurface, jlong eglContext);

extern jboolean doEglSwapBuffers(jlong eglDisplay, jlong eglSurface);
#endif // EGL_EXT

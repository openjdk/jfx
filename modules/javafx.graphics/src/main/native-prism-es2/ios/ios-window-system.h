/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* C routines encapsulating small amounts of Objective C code to allow
   EAGLContext creation and manipulation to occur from Java
*/
#import <jni.h>

#define jlong_to_ptr(value) ((void*)((long)value))
#define ptr_to_jlong(value) (jlong)((long)(value))

void* createPixelFormat(jint* ivalues);
void deletePixelFormat(void* pixelFormat);

void *getCurrentContext(void);

void* createContext(void* shareContext,
                    void* nsView,
                    void* pixelFormat,
                    int* viewNotReady);
void *getCGLContext(void* nsContext);
jboolean  makeCurrentContext(void* nsContext);
jboolean  clearCurrentContext(void *nsContext);
jboolean  deleteContext(void* nsContext);
jboolean  flushBuffer(void* nsContext);
void* createDummyWindow();
void* getProcAddress(const char *procName);

void setSwapInterval(void* nsContext, int interval);

extern jboolean pulseLoggingRequested;

/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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
   nsContext creation and manipulation to occur from Java
*/
#include "prism_es2_api.h"

// NS ..
void* createPixelFormat(const Es2PixelFormatAttrs* attrs);
void deletePixelFormat(void* pixelFormat);

// NS ..
void *getCurrentContext(void);
void *getNSView(void* nsContext);

void* createContext(void* shareContext,
                    void* nsView,
                    void* pixelFormat,
                    int* viewNotReady);
void *getCGLContext(void* nsContext);
int makeCurrentContext(void* nsContext);
int clearCurrentContext(void *nsContext);
int deleteContext(void* nsContext);
int flushBuffer(void* nsContext);
void* createDummyWindow();

void setSwapInterval(void* nsContext, int interval);

#include <OpenGL/OpenGL.h>

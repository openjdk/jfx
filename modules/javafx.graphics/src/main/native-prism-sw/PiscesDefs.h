/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#ifndef PISCES_DEFS_H
#define PISCES_DEFS_H

#include <stddef.h>
#include <stdint.h>

/*
 * Fixed-width spellings inherited from the JNI era, kept so the rasteriser sources stay as they
 * were. They are internal to the library - the exported ABI (prism_sw_api.h) uses the <stdint.h>
 * types directly. jboolean is a plain int32_t: nothing in the library depends on its width (it is
 * only ever a Renderer field, a local, a parameter or a return value, never an array element).
 */
typedef int32_t jint;
typedef int64_t jlong;
typedef int8_t jbyte;
typedef float jfloat;
typedef int32_t jboolean;

#define XNI_TRUE 1
#define XNI_FALSE 0

#ifndef INTEGER_MIN_VALUE
#define INTEGER_MIN_VALUE 0x80000000
#endif

#ifndef INTEGER_MAX_VALUE
#define INTEGER_MAX_VALUE 0x7fffffff
#endif

#ifndef NULL
#define NULL ((void*)0)
#endif

#ifndef INLINE
#define INLINE
#endif

#define MIN_X 0
#define MIN_Y 1
#define MAX_X 2
#define MAX_Y 3

#define floor CVMfdlibmFloor

#endif

/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>

#if defined (_LP64) || defined(_WIN64)
#define jlong_to_ptr(a) ((void*)(a))
#define ptr_to_jlong(a) ((jlong)(a))
#else
#define jlong_to_ptr(a) ((void*)(int)(a))
#define ptr_to_jlong(a) ((jlong)(int)(a))
#endif

#define XNI_TRUE JNI_TRUE
#define XNI_FALSE JNI_FALSE

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

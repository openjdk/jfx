/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _VS_MEMORY_H_
#define _VS_MEMORY_H_

#include "ProductFlags.h"

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32

#include "crtdbg.h"

#ifdef _DEBUG
#pragma warning(disable:4005)
#define MYDEBUG_NEW new(_NORMAL_BLOCK, __FILE__, __LINE__)
// Replace _NORMAL_BLOCK with _CLIENT_BLOCK if you want the
//allocations to be of _CLIENT_BLOCK type
#define new MYDEBUG_NEW
#define new(a) MYDEBUG_NEW
#else
#define MYDEBUG_NEW
#pragma warning(default:4005)
#endif // _DEBUG

#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION 

#endif // VS_MEMORY

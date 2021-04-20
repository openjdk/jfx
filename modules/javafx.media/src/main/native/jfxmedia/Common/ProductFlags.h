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

#ifndef _PRODUCT_FLAGS_H_
#define _PRODUCT_FLAGS_H_

#if defined(__APPLE__) || defined(__APPLE_CC__)
#ifndef TARGET_OS_MAC
#define TARGET_OS_MAC   1
#endif
#if defined(__arm64__)
#ifndef TARGET_OS_MAC_ARM64
#define TARGET_OS_MAC_ARM64   1
#endif
#endif // __arm64__
#elif defined(LINUX)
#ifndef TARGET_OS_LINUX
#define TARGET_OS_LINUX 1
#endif
#endif

// Whether to print debugging messages, etc.
#define JFXMEDIA_DEBUG                      0

#define JFXMEDIA_ENABLE_GST_TRACE           0

#define PLAYBACK_DEMO                       1

#define ENABLE_APP_SINK                     1

#define ENABLE_PLATFORM_GSTREAMER           1
#define ENABLE_PLATFORM_PACKETVIDEO         0

#define ENABLE_LOGGING                      1
#define ENABLE_LOWLEVELPERF                 0
#define ENABLE_INSTRUMENTS                  0
#define ENABLE_PROGRESS_BUFFER              1

#define ENABLE_BREAK_MY_DATA                0
#define BREAK_MY_DATA_SKIP                  250000
#define BREAK_MY_DATA_PROBABILITY           0.0001

// Enable detection of memory leaks using Visual Studio
// This option will only work in Debug mode
#define ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION 0

#endif // _PRODUCT_FLAGS_H_

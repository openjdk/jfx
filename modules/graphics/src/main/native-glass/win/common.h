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

#ifndef _GLASS_COMMON_
#define _GLASS_COMMON_

#ifndef _WIN32_WINNT
    #define _WIN32_WINNT 0x0601
#endif
#ifndef _WIN32_IE
    #define _WIN32_IE 0x0500
#endif

#ifndef _WIN32_WINNT_
    #define _WIN32_WINNT_ _WIN32_WINNT
#endif

#pragma warning(disable : 4675)

#include <assert.h>
#include <comdef.h>
#include <comutil.h>
#include <hash_map>
#include <hash_set>
#include <imm.h>
#include <jni.h>
#include <malloc.h>
#include <manipulations.h>
#include <memory>
#include <mmsystem.h>
#include <new>
#include <ole2.h>
#include <shlobj.h>
#include <stdio.h>
#include <string.h>
#include <Tpcshrd.h>
#include <tchar.h>
#include <vector>
#include <wchar.h>
#include <windows.h>
#include <windowsx.h>

#include "Utils.h"
#include "OleUtils.h"
#include "Dwmapi.h"

#endif /* #ifndef _GLASS_COMMON_ */

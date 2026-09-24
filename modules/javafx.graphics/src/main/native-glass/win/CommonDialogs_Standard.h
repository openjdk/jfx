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

#ifndef _COMMONDIALOGS_STANDARD_INCLUDED_
#define _COMMONDIALOGS_STANDARD_INCLUDED_

#include "glass_win_api.h"

/*
 * The pre-Vista comdlg32 / SHBrowseForFolder path, JNI-free, with the same contract as the
 * COM path in CommonDialogs_COM.h.
 */
int32_t StandardFileChooser_Show(HWND owner, LPCWSTR folder, LPCWSTR filename, LPCWSTR title, int32_t type,
                                 int32_t multipleMode, const GwinFileFilter* filters, int32_t filterCount,
                                 int32_t defaultFilterIndex,
                                 uint16_t** outFiles, int32_t* outCount, int32_t* outFilterIndex);

int32_t StandardFolderChooser_Show(HWND owner, LPCWSTR folder, LPCWSTR title, uint16_t** outPath);

#endif // _COMMONDIALOGS_STANDARD_INCLUDED_

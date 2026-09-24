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

#ifndef _GLASSCLIPBOARD_H
#define _GLASSCLIPBOARD_H

#include "glass_win_api.h"

typedef DWORD DROPEFFECT;

DROPEFFECT getDROPEFFECT(int32_t actions);

int32_t getACTION(DROPEFFECT df);

/*
 * The callback tables of glass_win_api.h's clipboard section, file statics of
 * GlassClipboard.cpp. NULL while Java has installed nothing - and an upcall site (ClipboardData in
 * GlassClipboard.cpp, GlassDropTarget / GlassDropSource in GlassDnD.cpp, the WM_DRAWCLIPBOARD and
 * DisposeRegisteredClipboard sites in GlassApplication.cpp) that finds NULL delivers nothing; there
 * is no JNI path to take over. Once installed no slot is ever NULL: the setters
 * replace a NULL slot with a no-op.
 * Written on the launcher thread before the toolkit thread exists and read on the toolkit thread
 * only - no lock, like the view and window tables.
 */
const GwinClipboardCallbacks* GlassClipboardCallbacks();
const GwinDndCallbacks* GlassDndCallbacks();
void SetGlassClipboardCallbacks(const GwinClipboardCallbacks* cb);
void SetGlassDndCallbacks(const GwinDndCallbacks* cb);

// A slot status -> the HRESULT the former JNI site produced from a pending exception.
inline HRESULT GwinStatusToHR(int32_t status)
{
    return status == GWIN_OK ? S_OK : E_JAVAEXCEPTION;
}

#endif //_GLASSCLIPBOARD_H

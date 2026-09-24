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

#include "common.h"

#include "glass_win_api.h"

/*
 * ---- glass_win_api.h: the menu callback table ----
 *
 * The menu callback table. Written once by gwin_menu_set_callbacks, from Java, before any window can
 * receive WM_COMMAND; read by HandleMenuCommand, which runs on the Glass toolkit thread only - every
 * WM_COMMAND arrives through DispatchMessage -> BaseWnd::StaticWindowProc -> GlassWindow::WindowProc
 * -> GlassWindow::HandleCommand, and an inter-thread SendMessage still executes the WndProc on the
 * window's owning thread. No lock, by design, same as g_prefsCallbacks in PlatformSupport.cpp.
 */
static GwinMenuCallbacks g_menuCallbacks = { NULL };
static void* g_menuUser = NULL;

bool HandleMenuCommand(HWND hWnd, WORD cmdID)
{
    /*
     * Java installs the callback table through gwin_menu_set_callbacks before any window can receive
     * WM_COMMAND; the cmd_id is the whole message, and WinMenuItemDelegate's CommandIDManager - not
     * this library - maps it to a peer. While no table is installed the command is "not handled",
     * which is what the JNI path answered before WinMenuImpl._initIDs had run.
     *
     * The contract is "non-zero means handled", never "== 1": a handled command makes
     * GlassWindow::WindowProc answer `return 0` instead of falling through to DefWindowProc. A
     * target that fails must answer 0, which is what CallStaticBooleanMethod yielded when the Java
     * threw and CheckAndClearException swallowed it.
     *
     * hWnd is not forwarded: notifyCommand's com.sun.glass.ui.Window argument, which the JNI path
     * computed here with GlassWindow::FromHandle(hWnd)->GetJObject(), was never read on the Java side.
     */
    if (g_menuCallbacks.notify_command != NULL) {
        return g_menuCallbacks.notify_command(g_menuUser, (int32_t) cmdID) != 0;
    }
    return false;
}

/* ---- glass_win_api.h exports. Definitions take C linkage from that header's extern "C" block. ---- */

extern "C" {

int32_t gwin_menu_set_callbacks(const GwinMenuCallbacks* cb, void* user)
{
    if (cb == NULL) {
        g_menuCallbacks.notify_command = NULL;
        g_menuUser = NULL;
    } else {
        g_menuCallbacks = *cb;   // by value: this library never retains the caller's struct
        g_menuUser = user;
    }
    return GWIN_OK;
}

} // extern "C"

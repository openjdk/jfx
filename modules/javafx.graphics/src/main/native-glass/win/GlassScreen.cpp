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

#include "GlassScreen.h"

/*
 * ---- The callback table of glass_win_api.h's screen section ----
 *
 * Installed by gwin_screen_set_callbacks. A NULL slot is replaced by the no-op below, so
 * HandleDisplayChange dials settings_changed without testing it; what it tests is ScreenCallbacks()
 * returning NULL, which means "nothing installed, deliver nothing". Slots are written before the
 * flag, as in GlassView::SetViewCallbacks. Written on the launcher thread before the toolkit thread
 * exists and read on the toolkit thread only - no lock, like the other tables.
 *
 * The monitor enumeration, the DPI query and the FX-space anchoring that used to live in this file are
 * Java (WinScreenLayout); they were deleted with WinApplication.staticScreen_getScreens.
 */
namespace {

void NoopSettingsChanged() {}

const GwinScreenCallbacks NOOP_SCREEN_CALLBACKS = { NoopSettingsChanged };

GwinScreenCallbacks g_screenCallbacks = NOOP_SCREEN_CALLBACKS;
bool g_screenCallbacksInstalled = false;

} // namespace

void GlassScreen::SetScreenCallbacks(const GwinScreenCallbacks* cb)
{
    GwinScreenCallbacks t = NOOP_SCREEN_CALLBACKS;
    if (cb != NULL) {
        if (cb->settings_changed) t.settings_changed = cb->settings_changed;
    }
    g_screenCallbacksInstalled = false;
    g_screenCallbacks = t;
    g_screenCallbacksInstalled = cb != NULL;
}

const GwinScreenCallbacks* GlassScreen::ScreenCallbacks()
{
    return g_screenCallbacksInstalled ? &g_screenCallbacks : NULL;
}

/*
 * WM_DISPLAYCHANGE / WM_SETTINGCHANGE(SPI_SETWORKAREA) (GlassApplication) and WM_DPICHANGED (GlassWindow):
 * the table's one slot, or nothing when no table is installed.
 */
void GlassScreen::HandleDisplayChange()
{
    const GwinScreenCallbacks* cb = ScreenCallbacks();
    if (cb != NULL) {
        cb->settings_changed();
    }
}

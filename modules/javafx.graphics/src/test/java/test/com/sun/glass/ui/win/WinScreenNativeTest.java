/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.glass.ui.win;

import com.sun.glass.ui.win.WinGlassNativeShim;
import com.sun.glass.ui.win.WinGlassNativeShim.ScreenFire;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The binding half of the screen section: the layouts and constants the Java enumeration relies on, and
 * the one-slot {@code GwinScreenCallbacks} table through {@code gwin_test_fire_screen_callback}. Every layout here
 * is also pinned from the C side by a {@code static_assert} in {@code glass_win_api.cpp}; what this adds is the
 * Java side of the same numbers. The layouts and the argument contract of {@code gwin_test_screen_anchor}
 * ({@code GwinTestMonitor}, {@code GwinScreenInfo}) were asserted here too until the deletion of that
 * hook; its answers live on in {@code screen-anchor-golden.txt}.
 * <p>
 * The table tests install a table of their own and put back what was there
 * ({@code WinGlassNative.rewriteScreenCallbacks}): the production table once {@code WinApplication} installs
 * it, no table before.
 */
@EnabledOnOs(OS.WINDOWS)
public class WinScreenNativeTest {

    private static final int GWIN_OK = 0;
    private static final int GWIN_ERR_INVALID_ARG = -1;

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        // The four lazy holders, in the order WinGlassNativeTest's exact symbol list expects.
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        WinGlassNativeShim.bindBrowserSymbols();
        WinGlassNativeShim.bindScreenSymbols();
    }

    @Test
    public void monitorInfoExIsTheWinuserLayout() {
        assertEquals(104, WinGlassNativeShim.layoutByteSize("MONITORINFOEXW"));
        assertEquals(0, WinGlassNativeShim.offset("MONITORINFOEXW", "cbSize"));
        assertEquals(4, WinGlassNativeShim.offset("MONITORINFOEXW", "rcMonitor"));
        assertEquals(20, WinGlassNativeShim.offset("MONITORINFOEXW", "rcWork"));
        assertEquals(36, WinGlassNativeShim.offset("MONITORINFOEXW", "dwFlags"));
        assertEquals(40, WinGlassNativeShim.offset("MONITORINFOEXW", "szDevice"));
    }

    @Test
    public void theScreenTableIsTheSizeTheCompilerMadeIt() {
        assertEquals(8, WinGlassNativeShim.layoutByteSize("GwinScreenCallbacks"));
        assertEquals(WinGlassNativeShim.sizeOfScreenCallbacks(),
                WinGlassNativeShim.layoutByteSize("GwinScreenCallbacks"));
    }

    @Test
    public void theEnumerationConstantsAreTheSdkValues() {
        assertEquals(1, WinGlassNativeShim.constant("MONITORINFOF_PRIMARY"));
        assertEquals(12, WinGlassNativeShim.constant("BITSPIXEL"));
        assertEquals(14, WinGlassNativeShim.constant("PLANES"));
        assertEquals(88, WinGlassNativeShim.constant("LOGPIXELSX"));
        assertEquals(90, WinGlassNativeShim.constant("LOGPIXELSY"));
        assertEquals(32, WinGlassNativeShim.constant("CCHDEVICENAME"));
        assertEquals(0, WinGlassNativeShim.constant("MDT_EFFECTIVE_DPI"));
        assertEquals(2, WinGlassNativeShim.constant("MDT_RAW_DPI"));
        assertEquals(2, WinGlassNativeShim.constant("PROCESS_PER_MONITOR_DPI_AWARE"));
        assertEquals(0, WinGlassNativeShim.constant("S_OK"));
    }

    @Test
    public void slotZeroFiresTheInstalledSettingsChangedOnce() {
        assertEquals(new ScreenFire(0, 1), WinGlassNativeShim.fireScreenCallbackIntoRecordingTable(0, false));
    }

    @Test
    public void anyOtherSlotIsRefusedAndFiresNothing() {
        assertEquals(new ScreenFire(GWIN_ERR_INVALID_ARG, 0),
                WinGlassNativeShim.fireScreenCallbackIntoRecordingTable(1, false));
        assertEquals(new ScreenFire(GWIN_ERR_INVALID_ARG, 0),
                WinGlassNativeShim.fireScreenCallbackIntoRecordingTable(-1, false));
    }

    @Test
    public void aNullSlotIsAnInternalNoOp() {
        assertEquals(new ScreenFire(GWIN_OK, 0), WinGlassNativeShim.fireScreenCallbackIntoRecordingTable(0, true));
    }

    @Test
    public void withNoTableTheHookReportsIt() {
        assertEquals(GWIN_ERR_INVALID_ARG, WinGlassNativeShim.fireScreenCallbackWithNoTable(0));
    }
}

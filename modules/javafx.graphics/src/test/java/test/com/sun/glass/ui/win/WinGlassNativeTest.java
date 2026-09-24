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

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.win.WinGlassNativeShim;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binding tests for {@code com.sun.glass.ui.win.WinGlassNative}, the FFM facade behind
 * {@code WinRobot}: the eleven {@code gwin_*} symbols of {@code glass.dll}
 * ({@code src/main/native-glass/win/glass_win_api.h}, ABI 2), the nine {@code user32} functions that
 * replace the deleted JNI input bodies, the {@code INPUT} layout against the winuser.h x64 ABI, the
 * key table and the screen capture, plus the FX to Windows transform of {@code WinScreenTransform}
 * over a synthetic screen list. ABI 3 added two more system libraries to that set -
 * {@code kernel32!GetVersion} and {@code shlwapi!AssocQueryStringW}, the whole of the two
 * {@code WinApplication} natives it deleted - whose behaviour is asserted in
 * {@code WinApplicationNativeTest}; what is asserted here is only that they are bound, in order, and
 * that they resolve. The menus added eleven more {@code user32} functions and no {@code glass} symbol at
 * all - the whole of {@code GlassMenu.cpp}'s eleven functional JNI bodies - whose behaviour is
 * asserted in {@code WinMenuNativeTest}, and which appear here only in {@link #BOUND_SYMBOLS}.
 * The view peer added the sixteen exports of the header's view section and {@code user32!GetDoubleClickTime},
 * asserted in {@code WinViewNativeTest} and listed here. The window peer added the twenty-nine exports of the
 * window section, three more {@code user32} functions ({@code IsWindow}, {@code GetCapture},
 * {@code GetWindowRect} - the {@code _getAnchor} WRAPPER), {@code user32!LoadCursorW} and
 * {@code kernel32!GetModuleHandleW} (the cursor mapping), asserted in {@code WinWindowNativeTest}.
 * The clipboard, drag and dialog peers added the nineteen exports of that section of the header plus its two
 * test hooks, and {@code ole32!OleIsCurrentClipboard} (the {@code isOwner} WRAPPER), asserted in
 * {@code WinClipboardNativeTest}, {@code WinDndNativeTest} and {@code WinCommonDialogsNativeTest}.
 * ABI 5 added {@code gwin_app_create}, {@code gwin_view_create} and the screen table's installer and
 * probe, the four {@code user32} / {@code kernel32} calls of the screen enumeration, its display DC
 * ({@code gdi32!CreateDCW}, {@code GetDeviceCaps}, {@code DeleteDC}, in the lazy {@code gdi32} block) and a
 * fourth lazy holder for {@code shcore}'s three DPI functions, asserted in {@code WinScreenNativeTest},
 * {@code WinScreenLayoutParityTest} and {@code WinScreenParityTest}.
 * <p>
 * Nothing here calls {@code SendInput} or {@code mouse_event}: injecting input would move the cursor
 * and type into whatever window has focus on the machine running the build. That half of the binding
 * is exercised by {@code tests/system} under {@code -DUSE_ROBOT=true}, which owns a window to receive
 * it. What is asserted here is that the symbols resolve and that the structure they are handed has the
 * layout the ABI requires.
 * <p>
 * The GDI capture and {@code GetSystemMetrics} need a window station with a display, which a build
 * running as a service or in session 0 does not have. Those assertions are gated on a probe (a 1x1
 * capture that has to succeed); everything else runs anywhere the library loads.
 * <p>
 * Line numbers into the {@code native-glass/win} C++ sources refer to those files at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>}).
 */
@EnabledOnOs(OS.WINDOWS)
public class WinGlassNativeTest {

    /**
     * Every symbol the facade binds, in binding order: the ABI guard first, then the rest of glass,
     * then user32 and kernel32, then - once a timer, a cursor, a {@code showDocument} call and a screen
     * enumeration have forced the four lazy holders, in that order - winmm, gdi32 and shlwapi, with
     * {@link #SHCORE_SYMBOLS} after them when they resolve. {@link #requireNatives()} forces all four, so
     * {@link #expectedBoundSymbols()} stays an exact ordered equality instead of depending on which test
     * class ran first in the shared surefire JVM.
     */
    static final List<String> BOUND_SYMBOLS = List.of(
            "glass!gwin_abi_version", "glass!gwin_robot_capture", "glass!gwin_key_java_to_windows",
            "glass!gwin_key_windows_to_java",
            "glass!gwin_key_code_for_char", "glass!gwin_sizeof_ui_settings",
            "glass!gwin_sizeof_network_info", "glass!gwin_prefs_set_callbacks",
            "glass!gwin_prefs_query_ui_settings", "glass!gwin_prefs_query_network",
            "glass!gwin_sizeof_app_callbacks", "glass!gwin_app_set_callbacks", "glass!gwin_app_create",
            "glass!gwin_run_loop",
            "glass!gwin_terminate_loop", "glass!gwin_enter_nested_event_loop",
            "glass!gwin_leave_nested_event_loop", "glass!gwin_invoke_and_wait",
            "glass!gwin_invoke_later", "glass!gwin_sizeof_menu_callbacks",
            "glass!gwin_menu_set_callbacks",
            "glass!gwin_sizeof_view_callbacks", "glass!gwin_sizeof_gesture_callbacks",
            "glass!gwin_view_set_callbacks", "glass!gwin_gesture_set_callbacks",
            "glass!gwin_test_fire_callback", "glass!gwin_view_create", "glass!gwin_view_close",
            "glass!gwin_view_get_native_view",
            "glass!gwin_view_set_parent", "glass!gwin_view_get_x", "glass!gwin_view_get_y",
            "glass!gwin_view_schedule_repaint", "glass!gwin_view_upload_pixels",
            "glass!gwin_view_enter_fullscreen", "glass!gwin_view_exit_fullscreen",
            "glass!gwin_view_enable_ime", "glass!gwin_view_finish_ime_composition",
            "glass!gwin_sizeof_window_callbacks", "glass!gwin_window_set_callbacks",
            "glass!gwin_window_create", "glass!gwin_window_close", "glass!gwin_window_set_view",
            "glass!gwin_window_update_view_size", "glass!gwin_window_set_menubar",
            "glass!gwin_window_set_level", "glass!gwin_window_set_focusable",
            "glass!gwin_window_set_enabled", "glass!gwin_window_set_alpha",
            "glass!gwin_window_set_dark_frame", "glass!gwin_window_get_insets",
            "glass!gwin_window_set_bounds", "glass!gwin_window_set_title",
            "glass!gwin_window_set_resizable", "glass!gwin_window_set_visible",
            "glass!gwin_window_request_focus", "glass!gwin_window_grab_focus",
            "glass!gwin_window_ungrab_focus", "glass!gwin_window_minimize", "glass!gwin_window_maximize",
            "glass!gwin_window_set_min_size", "glass!gwin_window_set_max_size",
            "glass!gwin_window_set_icon", "glass!gwin_window_to_front", "glass!gwin_window_to_back",
            "glass!gwin_window_set_cursor", "glass!gwin_window_show_system_menu",
            "glass!gwin_sizeof_screen_callbacks", "glass!gwin_screen_set_callbacks",
            "glass!gwin_sizeof_clipboard_callbacks", "glass!gwin_sizeof_dnd_callbacks",
            "glass!gwin_clipboard_set_callbacks", "glass!gwin_dnd_set_callbacks",
            "glass!gwin_test_fire_clipboard_callback", "glass!gwin_test_fire_dnd_callback",
            "glass!gwin_alloc", "glass!gwin_free", "glass!gwin_clipboard_register_viewer",
            "glass!gwin_clipboard_dispose", "glass!gwin_clipboard_push", "glass!gwin_clipboard_pop",
            "glass!gwin_clipboard_pop_bytes", "glass!gwin_clipboard_pop_mimes",
            "glass!gwin_clipboard_push_target_action", "glass!gwin_clipboard_pop_supported_actions",
            "glass!gwin_dnd_push", "glass!gwin_dnd_dispose", "glass!gwin_sizeof_file_filter",
            "glass!gwin_dialog_file", "glass!gwin_dialog_folder",
            "glass!gwin_sizeof_accessible_callbacks", "glass!gwin_sizeof_text_range_callbacks",
            "glass!gwin_sizeof_variant", "glass!gwin_a11y_set_callbacks",
            "glass!gwin_a11y_text_range_set_callbacks", "glass!gwin_a11y_create", "glass!gwin_a11y_destroy",
            "glass!gwin_a11y_text_range_create", "glass!gwin_a11y_text_range_destroy",
            "glass!gwin_a11y_raise_property_changed",
            "user32!SendInput", "user32!mouse_event", "user32!GetCursorPos", "user32!GetSystemMetrics",
            "user32!MapVirtualKeyW", "user32!ShowCursor", "user32!GetKeyState", "user32!GetSysColor",
            "user32!SystemParametersInfoW", "kernel32!GetVersion",
            "user32!CreateMenu", "user32!DestroyMenu", "user32!IsMenu", "user32!GetMenuItemCount",
            "user32!GetMenuItemInfoW", "user32!SetMenuItemInfoW", "user32!InsertMenuItemW",
            "user32!InsertMenuW", "user32!RemoveMenu", "user32!EnableMenuItem",
            "user32!CheckMenuItem", "user32!GetDoubleClickTime",
            "user32!IsWindow", "user32!GetCapture", "user32!GetWindowRect", "user32!LoadCursorW",
            "kernel32!GetModuleHandleW", "ole32!OleIsCurrentClipboard",
            "user32!EnumDisplayMonitors", "user32!GetMonitorInfoW", "user32!SetProcessDPIAware",
            "kernel32!GetSystemDirectoryW",
            "winmm!timeGetDevCaps", "winmm!timeBeginPeriod", "winmm!timeEndPeriod",
            "winmm!timeSetEvent", "winmm!timeKillEvent",
            "gdi32!CreateBitmap", "gdi32!CreateDIBSection", "gdi32!DeleteObject", "gdi32!GdiFlush",
            "user32!CreateIconIndirect", "gdi32!CreateDCW", "gdi32!GetDeviceCaps", "gdi32!DeleteDC",
            "shlwapi!AssocQueryStringW");

    /**
     * The fourth lazy holder, {@code shcore}, which binds its three DPI functions all or none - appended to
     * {@link #BOUND_SYMBOLS} only when {@code WinGlassNativeShim.dpiFunctionsResolved()} says they resolved,
     * in {@code GlassScreen::LoadDPIFuncs}' {@code GetProcAddress} order.
     */
    static final List<String> SHCORE_SYMBOLS = List.of(
            "shcore!GetProcessDpiAwareness", "shcore!SetProcessDpiAwareness", "shcore!GetDpiForMonitor");

    /**
     * The fifth lazy holder, {@code UIAutomationCore}, which the facade forces only when accessibility
     * raises an event or asks whether a client is listening - the {@code /DELAYLOAD} the JNI had. This
     * class never forces it, so it is expected only when another class in the shared surefire JVM
     * already did; the class that does ({@code WinAccessibilityNativeTest}) forces the four older
     * holders first, so this pair is always last however the two are ordered.
     */
    static final List<String> UIA_SYMBOLS = List.of(
            "UIAutomationCore!UiaRaiseAutomationEvent", "UIAutomationCore!UiaClientsAreListening");

    /** winuser.h virtual keys used by the tests, none of which is layout-dependent. */
    static final int VK_RETURN = 0x0D;
    static final int VK_ESCAPE = 0x1B;
    static final int VK_LEFT = 0x25;
    static final int VK_A = 0x41;
    static final int VK_LWIN = 0x5B;
    static final int VK_RWIN = 0x5C;
    static final int VK_DIVIDE = 0x6F;
    static final int VK_RCONTROL = 0xA3;
    static final int VK_RMENU = 0xA5;

    /**
     * The seventeen hints of the {@code gwin_key_code_for_char} matrix: {@code VK_UNDEFINED} for the
     * ordinary path, and the sixteen codes {@code KeyTable.cpp}'s {@code IsNumericKeypadCode}
     * accepts, which are the only ones that reach the branch that asks
     * {@code JavaKeyToWindowsKey} whether the hinted key really produces the character.
     */
    static final int[] KEY_CODE_HINTS = {
        KeyEvent.VK_UNDEFINED, KeyEvent.VK_DIVIDE, KeyEvent.VK_MULTIPLY, KeyEvent.VK_SUBTRACT,
        KeyEvent.VK_ADD, KeyEvent.VK_DECIMAL, KeyEvent.VK_SEPARATOR,
        KeyEvent.VK_NUMPAD0, KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3,
        KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, KeyEvent.VK_NUMPAD7,
        KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9};

    /** The 22 extended virtual keys of {@code KeyTable.cpp}'s {@code IsExtendedKey} (L315-343). */
    static final int[] EXTENDED_KEYS = {
        0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x2C, 0x2D, 0x2E,
        0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x90};

    /** Why a screen-reading assertion was skipped; see {@link #probeDesktop()}. */
    static final String DESKTOP_REQUIRED =
            "this JVM has no interactive window station, so GDI cannot read the screen: the capture and"
            + " GetSystemMetrics assertions need a desktop session, not a service or session 0.";

    private static boolean desktopAvailable;

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(WinGlassNativeTest.class);

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        WinGlassNativeShim.bindBrowserSymbols();
        WinGlassNativeShim.bindScreenSymbols();
        desktopAvailable = probeDesktop();
    }

    /**
     * Whether this session has a display GDI can read: a primary screen with a size, and a 1x1 capture
     * that reports {@code GWIN_OK}. Every assertion that reads the screen is gated on it, because a
     * build running without an interactive window station fails all of them for a reason that is not a
     * regression.
     */
    private static boolean probeDesktop() {
        if (WinGlassNativeShim.getSystemMetrics(constant("SM_CXSCREEN")) <= 0) {
            return false;
        }
        return WinGlassNativeShim.robotCapture(0, 0, 1, 1, new int[1]) == constant("GWIN_OK");
    }

    /**
     * The gate of every screen-reading case: a skip with {@link #DESKTOP_REQUIRED} where there is no desktop,
     * a failure with the same text under {@code -Djfx.parity.require=true} - the property says this machine
     * has one - and either way the body that follows counts as having run on the device ({@link ParityGate}).
     */
    private static void requireDesktop() {
        LEDGER.requireOracle(desktopAvailable, () -> DESKTOP_REQUIRED);
        LEDGER.compared();
    }

    /** If the desktop was there, at least one screen-reading body ran. */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    private static int constant(String name) {
        return WinGlassNativeShim.constant(name);
    }

    // ---------------------------------------------------------------------------------------------
    // Symbols, ABI version and constants
    // ---------------------------------------------------------------------------------------------

    /**
     * {@link #BOUND_SYMBOLS}, plus {@link #SHCORE_SYMBOLS} when the facade found all three, plus
     * {@link #UIA_SYMBOLS} when accessibility has already forced that holder in this JVM.
     */
    static List<String> expectedBoundSymbols() {
        List<String> expected = new ArrayList<>(BOUND_SYMBOLS);
        if (WinGlassNativeShim.dpiFunctionsResolved()) {
            expected.addAll(SHCORE_SYMBOLS);
        }
        if (WinGlassNativeShim.uiaSymbolsBound()) {
            expected.addAll(UIA_SYMBOLS);
        }
        return expected;
    }

    @Test
    public void facadeBindsEverySymbolItNeedsInOrder() {
        assertEquals(expectedBoundSymbols(), WinGlassNativeShim.boundSymbols());
    }

    @Test
    public void everyBoundSymbolResolvesInItsLibrary() {
        for (String symbol : expectedBoundSymbols()) {
            assertTrue(WinGlassNativeShim.resolves(symbol), symbol);
        }
        // Every Windows JDK 26 supports has shcore's three DPI functions (Windows 8.1 and later), so on a
        // supported machine the "none" arm of the holder is unreachable - and taking it silently would move
        // every screen to LOGPIXELS scaling.
        assertTrue(WinGlassNativeShim.dpiFunctionsResolved(), "shcore!GetDpiForMonitor and its two siblings");
        assertFalse(WinGlassNativeShim.resolves("glass!gwin_no_such_function"));
        assertFalse(WinGlassNativeShim.resolves("user32!NoSuchFunctionForJfx"));
        assertFalse(WinGlassNativeShim.resolves("winmm!NoSuchFunctionForJfx"));
        assertFalse(WinGlassNativeShim.resolves("gdi32!NoSuchFunctionForJfx"));
        assertFalse(WinGlassNativeShim.resolves("kernel32!NoSuchFunctionForJfx"));
        assertFalse(WinGlassNativeShim.resolves("shlwapi!NoSuchFunctionForJfx"));
        assertFalse(WinGlassNativeShim.resolves("ole32!NoSuchFunctionForJfx"));
        assertFalse(WinGlassNativeShim.resolves("shcore!NoSuchFunctionForJfx"));
        // kernel32 joined the set with ABI 3 (GetVersion), shlwapi with it (AssocQueryStringW), ole32 with
        // the clipboard peers (OleIsCurrentClipboard) and shcore with ABI 5 (GetDpiForMonitor), so the "not a library
        // this class binds" rejection needs a DLL genuinely outside the eight. advapi32 is in every Win32
        // process - the registry and security APIs - and is still not one this class binds, which is the
        // distinction being asserted.
        assertThrows(IllegalArgumentException.class,
                () -> WinGlassNativeShim.resolves("advapi32!RegCloseKey"));
        assertThrows(IllegalArgumentException.class, () -> WinGlassNativeShim.resolves("gwin_abi_version"));
    }

    @Test
    public void abiVersionIsTheOneTheFacadeWasWrittenFor() {
        // 3 since the loop/invoke/menu flip: gwin_run_loop, gwin_terminate_loop, the two
        // gwin_*_nested_event_loop, gwin_invoke_and_wait, gwin_invoke_later, gwin_app_set_callbacks,
        // gwin_menu_set_callbacks and the two matching gwin_sizeof_*_callbacks probes were added to
        // glass_win_api.h, which bumped GLASS_WIN_ABI_VERSION 2 -> 3. 4 since the view flip: the
        // sixteen exports of the header's view section landed additively under 3, and the bump 3 -> 4
        // is the change set that flips WinView - Java_..._WinView__1create takes a jlong view id from
        // then on - so the literal here, WinGlassNative.ABI_VERSION and the define move together. 5 since
        // the screen flip: gwin_app_create, gwin_view_create and the screen section landed additively under 4,
        // and the bump is the change set in which the facade binds them and stops binding gwin_robot_pixel_color -
        // which, with gwin_test_screen_anchor, ABI 5 defined as test-only; the shim bound both until they were
        // deleted, without a bump. 6 since the accessibility flip: the thirteen exports of the header's
        // accessibility section landed additively under 5, and the bump 5 -> 6 is the change set in which
        // WinAccessible and WinTextRangeProvider are flipped onto the two callback tables and the nine
        // Java_* bodies and this library's JNI_OnLoad are deleted.
        assertEquals(6, WinGlassNativeShim.expectedAbiVersion());
        assertEquals(WinGlassNativeShim.expectedAbiVersion(), WinGlassNativeShim.abiVersion());
    }

    @Test
    public void statusCodesMatchGlassWinApiHeader() {
        assertEquals(0, constant("GWIN_OK"));
        assertEquals(-1, constant("GWIN_ERR_INVALID_ARG"));
        assertEquals(-2, constant("GWIN_ERR_NO_TOOLKIT"));
        assertEquals(1, constant("GWIN_ROBOT_ERR_CREATE_DC"));
        assertEquals(2, constant("GWIN_ROBOT_ERR_CREATE_MEM_DC"));
        assertEquals(3, constant("GWIN_ROBOT_ERR_CREATE_BITMAP"));
        assertEquals(4, constant("GWIN_ROBOT_ERR_BITBLT"));
        assertEquals(5, constant("GWIN_ROBOT_ERR_GETDIBITS"));
    }

    @Test
    public void constantsMatchWinuser() {
        assertEquals(0, constant("INPUT_MOUSE"));
        assertEquals(1, constant("INPUT_KEYBOARD"));
        assertEquals(0x0001, constant("KEYEVENTF_EXTENDEDKEY"));
        assertEquals(0x0002, constant("KEYEVENTF_KEYUP"));
        assertEquals(0, constant("MAPVK_VK_TO_VSC"));
        assertEquals(0x0001, constant("MOUSEEVENTF_MOVE"));
        assertEquals(0x0002, constant("MOUSEEVENTF_LEFTDOWN"));
        assertEquals(0x0004, constant("MOUSEEVENTF_LEFTUP"));
        assertEquals(0x0008, constant("MOUSEEVENTF_RIGHTDOWN"));
        assertEquals(0x0010, constant("MOUSEEVENTF_RIGHTUP"));
        assertEquals(0x0020, constant("MOUSEEVENTF_MIDDLEDOWN"));
        assertEquals(0x0040, constant("MOUSEEVENTF_MIDDLEUP"));
        assertEquals(0x0080, constant("MOUSEEVENTF_XDOWN"));
        assertEquals(0x0100, constant("MOUSEEVENTF_XUP"));
        assertEquals(0x0800, constant("MOUSEEVENTF_WHEEL"));
        assertEquals(0x8000, constant("MOUSEEVENTF_ABSOLUTE"));
        assertEquals(1, constant("XBUTTON1"));
        assertEquals(2, constant("XBUTTON2"));
        assertEquals(120, constant("WHEEL_DELTA"));
        assertEquals(0, constant("SM_CXSCREEN"));
        assertEquals(1, constant("SM_CYSCREEN"));
        assertEquals(23, constant("SM_SWAPBUTTON"));
        assertEquals(0x14, constant("VK_CAPITAL"));
        assertEquals(0x90, constant("VK_NUMLOCK"));
        assertEquals(0x0042, constant("SPI_GETHIGHCONTRAST"));
        assertEquals(0x1042, constant("SPI_GETCLIENTAREAANIMATION"));
        assertEquals(0x0001, constant("HCF_HIGHCONTRASTON"));
        assertEquals(15, constant("COLOR_3DFACE"));
        assertEquals(18, constant("COLOR_BTNTEXT"));
        assertEquals(17, constant("COLOR_GRAYTEXT"));
        assertEquals(13, constant("COLOR_HIGHLIGHT"));
        assertEquals(14, constant("COLOR_HIGHLIGHTTEXT"));
        assertEquals(26, constant("COLOR_HOTLIGHT"));
        assertEquals(5, constant("COLOR_WINDOW"));
        assertEquals(8, constant("COLOR_WINDOWTEXT"));
    }

    // ---------------------------------------------------------------------------------------------
    // Layouts: the winuser.h x64 ABI that SendInput and GetCursorPos require
    // ---------------------------------------------------------------------------------------------

    @Test
    public void structuresHaveTheirWinuserX64Sizes() {
        assertEquals(40, WinGlassNativeShim.layoutByteSize("INPUT"));
        assertEquals(32, WinGlassNativeShim.layoutByteSize("MOUSEINPUT"));
        assertEquals(24, WinGlassNativeShim.layoutByteSize("KEYBDINPUT"));
        assertEquals(8, WinGlassNativeShim.layoutByteSize("POINT"));
        assertEquals(16, WinGlassNativeShim.layoutByteSize("HIGHCONTRAST"));
    }

    /**
     * The offsets {@code glass_win_api.cpp} static_asserts against the real {@code INPUT}: {@code type}
     * at 0 and the union at 8, so that both members start where the C compiler puts them. A wrong
     * offset here would not fail to link - it would silently synthesise the wrong event.
     */
    @Test
    public void inputMembersSitWhereTheUnionPutsThem() {
        assertEquals(0, WinGlassNativeShim.offset("INPUT", "type"));

        assertEquals(8, WinGlassNativeShim.inputOffset("mi", "dx"));
        assertEquals(12, WinGlassNativeShim.inputOffset("mi", "dy"));
        assertEquals(16, WinGlassNativeShim.inputOffset("mi", "mouseData"));
        assertEquals(20, WinGlassNativeShim.inputOffset("mi", "dwFlags"));
        assertEquals(24, WinGlassNativeShim.inputOffset("mi", "time"));
        assertEquals(32, WinGlassNativeShim.inputOffset("mi", "dwExtraInfo"));

        assertEquals(8, WinGlassNativeShim.inputOffset("ki", "wVk"));
        assertEquals(10, WinGlassNativeShim.inputOffset("ki", "wScan"));
        assertEquals(12, WinGlassNativeShim.inputOffset("ki", "dwFlags"));
        assertEquals(16, WinGlassNativeShim.inputOffset("ki", "time"));
        assertEquals(24, WinGlassNativeShim.inputOffset("ki", "dwExtraInfo"));

        assertEquals(0, WinGlassNativeShim.offset("POINT", "x"));
        assertEquals(4, WinGlassNativeShim.offset("POINT", "y"));

        assertEquals(0, WinGlassNativeShim.offset("HIGHCONTRAST", "cbSize"));
        assertEquals(4, WinGlassNativeShim.offset("HIGHCONTRAST", "dwFlags"));
        assertEquals(8, WinGlassNativeShim.offset("HIGHCONTRAST", "lpszDefaultScheme"));
    }

    // ---------------------------------------------------------------------------------------------
    // Key table (gwin_key_*, KeyTable.cpp)
    // ---------------------------------------------------------------------------------------------

    /**
     * Only layout-independent codes are asserted: none of these is one of the 13 OEM virtual keys, so
     * {@code JavaKeyToWindowsKey} takes them straight from the table and never runs its
     * {@code MAPVK_VK_TO_CHAR} search.
     */
    @Test
    public void keyJavaToWindowsMapsTheTableCodes() {
        assertEquals(VK_RETURN, WinGlassNativeShim.keyJavaToWindows(KeyEvent.VK_ENTER));
        assertEquals(VK_ESCAPE, WinGlassNativeShim.keyJavaToWindows(KeyEvent.VK_ESCAPE));
        assertEquals(VK_LEFT, WinGlassNativeShim.keyJavaToWindows(KeyEvent.VK_LEFT));
        assertEquals(VK_A, WinGlassNativeShim.keyJavaToWindows(KeyEvent.VK_A));
        // Both VK_LWIN and VK_RWIN carry VK_WINDOWS; the first table entry wins going back.
        assertEquals(VK_LWIN, WinGlassNativeShim.keyJavaToWindows(KeyEvent.VK_WINDOWS));
    }

    /**
     * 0 is the sentinel {@code WinGlassNative.keyEvent} reads as "send nothing", and it is what the C
     * returns for {@code VK_UNDEFINED} and for a code that is in no table entry and matches no OEM
     * character either.
     */
    @Test
    public void keyJavaToWindowsReturnsZeroForAnUnmappedCode() {
        assertEquals(0, WinGlassNativeShim.keyJavaToWindows(KeyEvent.VK_UNDEFINED));
        assertEquals(0, WinGlassNativeShim.keyJavaToWindows(0x7FFF0000));
        assertEquals(0, WinGlassNativeShim.keyJavaToWindows(-1));
    }

    @Test
    public void keyWindowsToJavaIsThePlainTableLookup() {
        assertEquals(KeyEvent.VK_ENTER, WinGlassNativeShim.keyWindowsToJava(VK_RETURN));
        assertEquals(KeyEvent.VK_ESCAPE, WinGlassNativeShim.keyWindowsToJava(VK_ESCAPE));
        assertEquals(KeyEvent.VK_LEFT, WinGlassNativeShim.keyWindowsToJava(VK_LEFT));
        assertEquals(KeyEvent.VK_A, WinGlassNativeShim.keyWindowsToJava(VK_A));
        assertEquals(KeyEvent.VK_WINDOWS, WinGlassNativeShim.keyWindowsToJava(VK_LWIN));
        assertEquals(KeyEvent.VK_WINDOWS, WinGlassNativeShim.keyWindowsToJava(VK_RWIN));
        assertEquals(KeyEvent.VK_UNDEFINED, WinGlassNativeShim.keyWindowsToJava(0));
        assertEquals(KeyEvent.VK_UNDEFINED, WinGlassNativeShim.keyWindowsToJava(0x7FFF0000));
    }

    /**
     * {@code KeyTable.cpp}'s {@code IsExtendedKey} (L315-343), which decides
     * {@code KEYEVENTF_EXTENDEDKEY}. What is absent matters as much as what is present: the C does not
     * mark {@code VK_RCONTROL}, {@code VK_RMENU} or {@code VK_DIVIDE}, all of which really are extended
     * keys on a PC keyboard, and reproducing that omission is what keeps the flip behaviour-neutral.
     */
    @Test
    public void extendedKeySetIsExactlyKeyTablesTwentyTwo() {
        for (int virtualKey : EXTENDED_KEYS) {
            assertTrue(WinGlassNativeShim.isExtendedKey(virtualKey), () -> "VK 0x" + Integer.toHexString(virtualKey));
        }
        assertEquals(22, EXTENDED_KEYS.length);
        for (int virtualKey = 0; virtualKey <= 0xFF; virtualKey++) {
            boolean expected = contains(EXTENDED_KEYS, virtualKey);
            assertEquals(expected, WinGlassNativeShim.isExtendedKey(virtualKey),
                    "VK 0x" + Integer.toHexString(virtualKey));
        }
        assertFalse(WinGlassNativeShim.isExtendedKey(VK_RCONTROL));
        assertFalse(WinGlassNativeShim.isExtendedKey(VK_RMENU));
        assertFalse(WinGlassNativeShim.isExtendedKey(VK_DIVIDE));
        assertFalse(WinGlassNativeShim.isExtendedKey(VK_RETURN));
    }

    /**
     * <b>The A/B for {@code gwin_key_code_for_char}</b>, and the only oracle this export will ever
     * have: the peer's {@code _getKeyCodeForChar} against the facade's binding of the export, over
     * every printable ASCII code unit crossed with every hint that reaches the numeric-keypad branch.
     * <p>
     * What it proves is the <em>binding</em>, not the logic. The C engineer deliberately left
     * {@code Java_com_sun_glass_ui_win_WinApplication__1getKeyCodeForChar} in place as a one-line
     * forwarder to {@code gwin_key_code_for_char} (an intermediate state of {@code KeyTable.cpp} that no commit
     * records), so when this test was written both arms were already the same C body and any disagreement could
     * only come from the marshalling: the {@code char} narrowed to {@code JAVA_SHORT} against the
     * {@code jchar} the JNI received, the {@code hint} that follows it, and the {@code int32_t} that
     * comes back. After the flip the peer simply calls the facade and the two arms are the same Java,
     * which is why the run that mattered could only be made while the JNI body existed.
     * <p>
     * The hint list is {@code KeyTable.cpp}'s {@code IsNumericKeypadCode} (L346-367) plus
     * {@code VK_UNDEFINED} for the ordinary path. {@code VK_SEPARATOR} is the interesting cell: it is
     * in that switch and has no key on a US layout, so it is the one that actually exercises
     * {@code JavaKeyToWindowsKey}'s OEM search. Both arms run on this thread on purpose - the OEM
     * search reads the <em>calling</em> thread's keyboard layout while the rest of the body reads the
     * toolkit thread's, so a comparison across threads would prove nothing.
     */
    @Test
    public void theKeyCodeForCharExportAgreesWithTheJniItReplaces() {
        int cells = 0;
        for (char c = 0x20; c <= 0x7F; c++) {
            final char codeUnit = c;
            for (int hint : KEY_CODE_HINTS) {
                int throughPeer = WinGlassNativeShim.keyCodeForCharThroughPeer(codeUnit, hint);
                int throughExport = WinGlassNativeShim.keyCodeForChar(codeUnit, hint);
                assertEquals(throughPeer, throughExport, () -> "char 0x"
                        + Integer.toHexString(codeUnit) + ", hint 0x" + Integer.toHexString(hint));
                cells++;
            }
        }
        assertEquals(96 * 17, cells);
    }

    /**
     * The comparison above is only worth running if the two arms answer something other than
     * {@code VK_UNDEFINED}: a session whose {@code VkKeyScanEx} finds no layout would agree on 0
     * everywhere and prove nothing. This is that check, and it is the one assertion of the pair that
     * needs a real desktop.
     */
    @Test
    public void theKeyCodeForCharMatrixIsNotUniformlyUndefined() {
        requireDesktop();
        long mapped = 0;
        for (char c = 0x20; c <= 0x7F; c++) {
            if (WinGlassNativeShim.keyCodeForChar(c, KeyEvent.VK_UNDEFINED) != KeyEvent.VK_UNDEFINED) {
                mapped++;
            }
        }
        assertTrue(mapped >= 36, "only " + mapped + " of 96 printable characters mapped to a key code");
        assertEquals(KeyEvent.VK_A, WinGlassNativeShim.keyCodeForChar('a', KeyEvent.VK_UNDEFINED));
        assertEquals(KeyEvent.VK_A, WinGlassNativeShim.keyCodeForChar('A', KeyEvent.VK_UNDEFINED));
        assertEquals(KeyEvent.VK_0, WinGlassNativeShim.keyCodeForChar('0', KeyEvent.VK_UNDEFINED));
        assertEquals(KeyEvent.VK_SPACE, WinGlassNativeShim.keyCodeForChar(' ', KeyEvent.VK_UNDEFINED));
    }

    /**
     * {@code 0x7F} is answered before any OS call at all (the first statement of {@code gwin_key_code_for_char}
     * in {@code KeyTable.cpp}):
     * {@code ViewContainer::HandleViewKeyEvent} synthesises that character for the Delete key and this
     * reverses it. It holds with any hint, including a numeric-keypad one, because the early return
     * comes first - which is why it is asserted separately from the matrix.
     */
    @Test
    public void deleteIsAnsweredWithoutAskingWindows() {
        assertEquals(KeyEvent.VK_DELETE, WinGlassNativeShim.keyCodeForChar((char) 0x7F, KeyEvent.VK_UNDEFINED));
        assertEquals(KeyEvent.VK_DELETE, WinGlassNativeShim.keyCodeForChar((char) 0x7F, KeyEvent.VK_DECIMAL));
        assertEquals(KeyEvent.VK_DELETE,
                WinGlassNativeShim.keyCodeForCharThroughPeer((char) 0x7F, KeyEvent.VK_UNDEFINED));
    }

    /**
     * A code unit above {@code 0x7FFF} is negative as a Java {@code short}, which is the one thing the
     * narrowing in {@link WinGlassNativeShim#keyCodeForChar} could get wrong and no printable
     * character would reveal. The C parameter is a {@code uint16_t}, so the answer must be the same
     * one the JNI gives for the same {@code jchar}, whatever that answer is on this layout.
     */
    @Test
    public void aHighCodeUnitCrossesAsUnsignedSixteenBits() {
        for (char c : new char[] {0x00FF, 0x7FFF, 0x8000, 0xFEFF, 0xFFFD, 0xFFFF}) {
            assertEquals(WinGlassNativeShim.keyCodeForCharThroughPeer(c, KeyEvent.VK_UNDEFINED),
                    WinGlassNativeShim.keyCodeForChar(c, KeyEvent.VK_UNDEFINED),
                    () -> "char 0x" + Integer.toHexString(c));
        }
    }

    private static boolean contains(int[] values, int value) {
        for (int candidate : values) {
            if (candidate == value) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // The user32 primitives the deleted JNI bodies called
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code MapVirtualKeyW(vk, MAPVK_VK_TO_VSC)} is what fills {@code KEYBDINPUT.wScan}. Which scan
     * code a virtual key has depends on the calling thread's layout - {@code VK_A} is 0x1E on QWERTY
     * and 0x10 on AZERTY - so only the two invariants the binding depends on are asserted: a key that
     * exists has a non-zero scan code, and it fits the {@code WORD} that {@code wScan} is.
     */
    @Test
    public void mapVirtualKeyGivesAWordSizedScanCode() {
        int mapType = constant("MAPVK_VK_TO_VSC");
        for (int virtualKey : new int[] {VK_RETURN, VK_ESCAPE, VK_LEFT, VK_A}) {
            int scanCode = WinGlassNativeShim.mapVirtualKey(virtualKey, mapType);
            assertTrue(scanCode > 0 && scanCode <= 0xFFFF,
                    "scan code of VK 0x" + Integer.toHexString(virtualKey) + " was " + scanCode);
        }
    }

    @Test
    public void primaryScreenMetricsAreThoseMouseMoveNormalisesAgainst() {
        requireDesktop();
        assertTrue(WinGlassNativeShim.getSystemMetrics(constant("SM_CXSCREEN")) > 0);
        assertTrue(WinGlassNativeShim.getSystemMetrics(constant("SM_CYSCREEN")) > 0);
        assertTrue(WinGlassNativeShim.getSystemMetrics(constant("SM_SWAPBUTTON")) >= 0);
    }

    /**
     * {@code GetCursorPos} into the {@code POINT} this class lays out. The value is whatever the user's
     * mouse is doing, so only its shape and its range are assertable: a pair, inside the virtual screen
     * that {@code SM_XVIRTUALSCREEN} and friends describe, widened here to a generous bound because a
     * disconnected monitor can leave the cursor briefly outside it.
     */
    @Test
    public void cursorPositionIsAPairOfWindowsPixels() {
        requireDesktop();
        int[] cursor = WinGlassNativeShim.cursorPosition();
        assertEquals(2, cursor.length, "cursorPosition() returns exactly [x, y]");
        assertTrue(Math.abs(cursor[0]) < 1 << 20, "cursor x out of any plausible range: " + cursor[0]);
        assertTrue(Math.abs(cursor[1]) < 1 << 20, "cursor y out of any plausible range: " + cursor[1]);
    }

    // ---------------------------------------------------------------------------------------------
    // Screen capture (gwin_robot_capture)
    // ---------------------------------------------------------------------------------------------

    /**
     * The four rejections of {@code Java_com_sun_glass_ui_win_WinRobot__1getScreenCapture}, which
     * returned silently and left the array alone. {@code WinGlassNative.robotCapture} makes them in
     * Java, before any buffer is allocated.
     */
    @Test
    public void captureRejectsEveryBadArgumentAndLeavesTheArrayAlone() {
        int invalid = constant("GWIN_ERR_INVALID_ARG");
        int[] data = filled(16, 0x12345678);
        assertEquals(invalid, WinGlassNativeShim.robotCapture(0, 0, 1, 1, null));
        assertEquals(invalid, WinGlassNativeShim.robotCapture(0, 0, 0, 1, data));
        assertEquals(invalid, WinGlassNativeShim.robotCapture(0, 0, 1, 0, data));
        assertEquals(invalid, WinGlassNativeShim.robotCapture(0, 0, -1, 4, data));
        // width >= (INT_MAX / 4) / height, i.e. 1000000 >= 536870
        assertEquals(invalid, WinGlassNativeShim.robotCapture(0, 0, 1_000_000, 1_000, data));
        // width * height = 20 > data.length
        assertEquals(invalid, WinGlassNativeShim.robotCapture(0, 0, 4, 5, data));
        assertArrayEquals(filled(16, 0x12345678), data);
    }

    /** The C keeps its own copy of those four checks; this drives {@code gwin_robot_capture} directly. */
    @Test
    public void theCAbiRepeatsEveryOneOfThoseRejections() {
        int invalid = constant("GWIN_ERR_INVALID_ARG");
        int[] data = filled(16, 0x12345678);
        assertEquals(invalid, WinGlassNativeShim.captureDirect(0, 0, 1, 1, null, 16));
        assertEquals(invalid, WinGlassNativeShim.captureDirect(0, 0, 0, 1, data, 16));
        assertEquals(invalid, WinGlassNativeShim.captureDirect(0, 0, 1, 0, data, 16));
        assertEquals(invalid, WinGlassNativeShim.captureDirect(0, 0, -1, 4, data, 16));
        assertEquals(invalid, WinGlassNativeShim.captureDirect(0, 0, 1_000_000, 1_000, data, 16));
        assertEquals(invalid, WinGlassNativeShim.captureDirect(0, 0, 4, 5, data, 16));
        assertArrayEquals(filled(16, 0x12345678), data);
    }

    @Test
    public void captureFillsExactlyTheRequestedPixelsAndNoMore() {
        requireDesktop();
        int[] data = filled(20, 0x12345678);
        assertEquals(constant("GWIN_OK"), WinGlassNativeShim.robotCapture(0, 0, 4, 4, data));
        for (int i = 0; i < 16; i++) {
            assertEquals(0xFF000000, data[i] & 0xFF000000, "captured pixel " + i + " must be opaque");
        }
        for (int i = 16; i < 20; i++) {
            assertEquals(0x12345678, data[i], "element " + i + " is past width * height");
        }
    }

    /**
     * A smoke test of the two wrappers over {@code gwin_robot_capture}: the product's two ways to read a
     * pixel have to give the same value - element {@code [y * width + x]} of a block
     * {@code WinGlassNative.robotCapture} returns, and {@code WinGlassNative.robotPixelColorAt(x, y)},
     * which is a 1x1 capture and is what {@code WinRobot.getPixelColor} calls once
     * {@code WinScreenTransform.fx2Win} has mapped the point. Readings are retried because the screen is
     * live: a pixel that changes between two captures is not a binding failure, but two readings
     * disagreeing on every one of twenty attempts is.
     * <p>
     * Three points. (0, 0) from a 1x1 block is the original check, and it is symmetric: it cannot tell
     * {@code x} from {@code y}, nor a row stride from a column one. (5, 2) from an 8x4 block and
     * (5, height - 3) from an 8 x {@code SM_CYSCREEN} block are not: an x/y swap or a wrong stride in
     * either wrapper makes the two reads sample different screen pixels, so it can only pass where those
     * pixels happen to be equal. The second point is near the top-left corner, where a desktop is often
     * uniform - a swapped {@code robotPixelColorAt} passed it on the machine this was written on - so the
     * third reaches the bottom edge, where a taskbar often is, and a swap would read near the top
     * instead. A uniform desktop can still hide a swap at both; both reads are also the same C function,
     * so this is no oracle for the capture itself. The robot suite, which compares against pixels it
     * painted, remains the real one.
     * <p>
     * This was {@code theThreePixelReadsAgree}. Its third read, {@code gwin_robot_pixel_color}, was
     * {@code GlassScreen::FX2Win} over the native monitor table {@code g_MonitorInfos}, then the same capture;
     * only the JNI {@code CreateJavaScreens} ever filled that table, so once {@code staticScreen_getScreens} was
     * Java, {@code FX2Win} always took its {@code numInfos == 0} return and the third read was the capture a
     * second time - a comparison that passed whatever the transform did. The export has since been
     * deleted. The transform the product uses is {@code WinScreenTransform}, in Java, tested below.
     */
    @Test
    public void theCaptureAndTheJavaPixelReadAgree() {
        requireDesktop();
        assertTheTwoReadsAgreeAt(0, 0, 1, 1);
        assertTheTwoReadsAgreeAt(5, 2, 8, 4);
        int screenHeight = WinGlassNativeShim.getSystemMetrics(constant("SM_CYSCREEN"));
        assertTheTwoReadsAgreeAt(5, screenHeight - 3, 8, screenHeight);
    }

    /** Element {@code [y * width + x]} of a {@code width} x {@code height} capture at (0, 0) against the 1x1 read. */
    private static void assertTheTwoReadsAgreeAt(int x, int y, int width, int height) {
        int[] block = new int[width * height];
        int viaCapture = 0;
        int viaJavaPath = 0;
        boolean agreed = false;
        for (int attempt = 0; attempt < 20 && !agreed; attempt++) {
            assertEquals(constant("GWIN_OK"), WinGlassNativeShim.robotCapture(0, 0, width, height, block));
            viaCapture = block[y * width + x];
            viaJavaPath = WinGlassNativeShim.robotPixelColorAt(x, y);
            agreed = viaCapture == viaJavaPath;
        }
        assertTrue(agreed, String.format("the pixel at (%d, %d) never read the same twice: capture %dx%d[%d]=%08X,"
                + " robotPixelColorAt=%08X", x, y, width, height, y * width + x, viaCapture, viaJavaPath));
        assertEquals(0xFF000000, viaCapture & 0xFF000000, "a captured pixel is always opaque");
    }

    private static int[] filled(int length, int value) {
        int[] data = new int[length];
        Arrays.fill(data, value);
        return data;
    }

    // ---------------------------------------------------------------------------------------------
    // WinScreenTransform: GlassScreen.cpp's FX2Win / Win2FX in Java
    // ---------------------------------------------------------------------------------------------

    /** {@code numInfos == 0} returned {@code FALSE} and left the point exactly as it was. */
    @Test
    public void anEmptyScreenListLeavesTheCoordinatesUnchanged() {
        float[] point = {12.5f, 34.5f};
        assertFalse(WinGlassNativeShim.fx2Win(List.of(), point));
        assertArrayEquals(new float[] {12.5f, 34.5f}, point);
        assertFalse(WinGlassNativeShim.win2Fx(List.of(), point));
        assertArrayEquals(new float[] {12.5f, 34.5f}, point);
    }

    /**
     * The nearest rectangle centre picks the monitor and the per-axis lerp does the rest. The numbers
     * are powers of two so that every intermediate is exact in {@code float}, which is what the C used.
     */
    @Test
    public void theNearestMonitorRescalesEachAxis() {
        List<Screen> screens = List.of(
                screen(0, 0, 128, 128, 0, 0, 256, 256),
                screen(128, 0, 128, 128, 256, 0, 256, 256));

        float[] onTheFirst = {32f, 64f};
        assertTrue(WinGlassNativeShim.fx2Win(screens, onTheFirst));
        assertArrayEquals(new float[] {64f, 128f}, onTheFirst);

        float[] onTheSecond = {160f, 64f};
        assertTrue(WinGlassNativeShim.fx2Win(screens, onTheSecond));
        assertArrayEquals(new float[] {320f, 128f}, onTheSecond);

        float[] back = {64f, 128f};
        assertTrue(WinGlassNativeShim.win2Fx(screens, back));
        assertArrayEquals(new float[] {32f, 64f}, back);
    }

    /**
     * {@code distSq < nearest} is strict in the C, so a point equidistant from two monitor centres goes
     * to the one that comes first in the table - here the 2x monitor, not the 4x one.
     */
    @Test
    public void theFirstMonitorWinsATie() {
        List<Screen> screens = List.of(
                screen(0, 0, 128, 128, 0, 0, 256, 256),
                screen(0, 0, 128, 128, 0, 0, 512, 512));
        float[] point = {64f, 64f};
        assertTrue(WinGlassNativeShim.fx2Win(screens, point));
        assertArrayEquals(new float[] {128f, 128f}, point);
    }

    /**
     * The {@code Screen} the transform reads: {@code x/y/width/height} is {@code fxMonitor} and
     * {@code platformX/Y/Width/Height} is {@code rcMonitor}, which is the order
     * {@code CreateJavaMonitorFromMIS} passes them to this constructor.
     */
    private static Screen screen(int fxX, int fxY, int fxWidth, int fxHeight,
                                 int winX, int winY, int winWidth, int winHeight) {
        return new Screen(0L, 32, fxX, fxY, fxWidth, fxHeight, winX, winY, winWidth, winHeight,
                fxX, fxY, fxWidth, fxHeight, 96, 96, 1f, 1f, 1f, 1f);
    }

    // ---------------------------------------------------------------------------------------------
    // Strings handed to Win32 and glass.dll
    // ---------------------------------------------------------------------------------------------

    /**
     * Every string the facade hands Win32 or {@code glass.dll} - menu and window titles, dialog and filter
     * strings, clipboard mimes, cursor and module names, the association - goes through
     * {@code WinGlassNative.wideString}, which copies UTF-16 code units and a NUL as {@code JString} did
     * ({@code GetStringLength} + {@code GetStringRegion}, {@code native-glass/win/Utils.h:108-112}).
     * Windows does not require well-formed UTF-16, so an unpaired surrogate must reach it as the unit it is; a
     * charset encoder writes U+FFFD in its place. The units are compared as hex first, because a failure message
     * cannot print an unpaired surrogate.
     */
    @Test
    public void wideStringsCrossAsTheirExactCodeUnitsAndOneNul() {
        char[] text = {
            '\uDC00',           // a lone low surrogate, first
            'M', 'e', 'n', 'u',
            '\uD83D', '\uDE00', // a valid pair, U+1F600
            '\uD800', 'x',      // a lone high surrogate followed by an ASCII char
            '\uDBFF'            // a lone high surrogate, last
        };
        char[] expected = Arrays.copyOf(text, text.length + 1);
        char[] units = WinGlassNativeShim.wideStringCodeUnits(new String(text));
        assertEquals(hex(expected), hex(units), "the WCHARs written, the terminator included");
        assertArrayEquals(expected, units);
        assertEquals(new String(text), new String(units, 0, units.length - 1), "the round trip back to a String");

        assertEquals(hex(new char[] {'a', '\0', 'b', '\0'}), hex(WinGlassNativeShim.wideStringCodeUnits("a\0b")),
                "an embedded U+0000 is copied, as GetStringRegion copied it");
        assertEquals(hex(new char[] {'\0'}), hex(WinGlassNativeShim.wideStringCodeUnits("")), "the empty string");
    }

    /** {@code units} as space-separated four-digit hex, so that an unpaired surrogate survives into a message. */
    private static String hex(char[] units) {
        StringBuilder text = new StringBuilder();
        for (char unit : units) {
            text.append(text.isEmpty() ? "" : " ").append(String.format("%04X", (int) unit));
        }
        return text.toString();
    }

    // ---------------------------------------------------------------------------------------------
    // The flip itself
    // ---------------------------------------------------------------------------------------------

    /** No {@code native} method may come back into {@code WinRobot}: every one of them moved to the facade. */
    @Test
    public void winRobotDeclaresNoNativeMethod() {
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOfWinRobot());
    }
}

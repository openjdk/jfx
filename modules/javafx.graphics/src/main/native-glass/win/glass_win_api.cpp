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

/*
 * glass_win_api.cpp - implementation of the flat C ABI declared in glass_win_api.h.
 *
 * WinRobot (ABI 1). The capture path is the GDI sequence of Robot.cpp's GetScreenCapture, copied
 * verbatim and instrumented with a status code; the key lookups forward to KeyTable.cpp. Robot.cpp
 * went once WinRobot had been flipped, so the static copy below is the only one.
 *
 * ABI 3 adds the application section: the callback table, the GwinRunnableAction that replaces
 * ENTER_MAIN_THREAD's _MyAction, and the six loop/invoke entry points, whose statements are those of
 * GlassApplication.cpp's _runLoop / _terminateLoop / _invokeAndWait / _submitForLaterInvocation
 * moved rather than rewritten. For WinMenuImpl only gwin_sizeof_menu_callbacks is here; the menu table and
 * gwin_menu_set_callbacks live in GlassMenu.cpp beside their one reader, as the preferences table
 * lives in PlatformSupport.cpp.
 *
 * The view section (ABI 4): the two installers (the tables themselves are GlassView statics,
 * because ViewContainer.cpp and FullScreenWindow.cpp dial them too), the test hook, and the eleven
 * gwin_view_* bodies, which are GlassView.cpp's former JNI bodies moved onto the opaque handle and
 * wrapped try { } catch (...). While no table was installed nothing here changed what ran: every
 * upcall site took its JNI path (since removed), and GLASS_WIN_ABI_VERSION stayed.
 *
 * The window section (ABI 4): the installer (the table itself is a GlassWindow static, read by
 * GlassWindow.cpp's 11 upcall sites and its destructor), the sizeof probe, and the twenty-seven
 * gwin_window_* bodies, which are GlassWindow.cpp's former JNI bodies moved onto the HWND handle and
 * wrapped try { } catch (...); the JNI entry points, which forwarded here where that changed no
 * behaviour, are gone. It landed additive in the same sense as the view section.
 *
 * The clipboard / drag-and-drop / common-dialog section (ABI 4): the two installers (the tables
 * are file statics of GlassClipboard.cpp - GlassDnD.cpp and GlassApplication.cpp dial them too, through
 * the accessors in GlassClipboard.h), the three sizeof probes and the gwin_alloc / gwin_free pair. The
 * functional bodies live beside the state they touch: gwin_clipboard_* and gwin_dnd_* in
 * GlassClipboard.cpp (it owns ClipboardData and the mime maps), gwin_dialog_* in CommonDialogs.cpp.
 * It landed additive in the same sense as the view and window sections; a site with no table
 * installed delivers nothing.
 *
 * After the ABI 4 bump, and additive under it: the three test hooks gwin_test_fire_window_callback,
 * gwin_test_fire_clipboard_callback and gwin_test_fire_dnd_callback, each beside its installer -
 * the view section's gwin_test_fire_callback for the window, clipboard and drag tables.
 *
 * ABI 5 (its exports first additive under ABI 4) adds gwin_app_create and gwin_view_create - the statements of
 * WinApplication._init (minus its DPI-awareness step) and WinView._create, over the no-JNI
 * GlassApplication() / PlatformSupport() twins - and the screen section: the installer, sizeof probe and
 * test hook of the one-slot table GlassScreen::HandleDisplayChange dials (the table is a GlassScreen.cpp
 * static). Under ABI 5 the JNI twins, gwin_robot_pixel_color and the anchoring test hook
 * gwin_test_screen_anchor were then deleted with the GlassScreen.cpp code it ran; with no table installed
 * HandleDisplayChange delivers nothing. gwin_test_report_exception_in_downcall, the test hook of the
 * CheckAndClearException fix, moved with CheckAndClearException into GlassAccessibleJni.cpp and went
 * with that file when the accessibility JNI was deleted, so no export here calls into the JVM.
 *
 * No local header this file includes spells a JNI type any more, and common.h no longer includes jni.h. It
 * still reaches this TU through line 2 of the javac -h constants headers below (Window_Level, Clipboard,
 * CommonDialogs_Type); nothing in this file uses it.
 */

#include "glass_win_api.h"

#include "common.h"

#include <stddef.h>
// Declarations and enums only, no #pragma comment(lib): read for the MDT_* / PROCESS_* pins below.
// Nothing in this library calls shcore.
#include <ShellScalingApi.h>

#include "GlassApplication.h"
#include "GlassClipboard.h"
#include "GlassScreen.h"
#include "GlassStringBlock.h"
#include "GlassView.h"
#include "GlassWindow.h"
#include "KeyTable.h"

// The @Native Window.Level constants gwin_window_set_level maps (they are not JNI: javac -h emits
// this header for the annotated constants whether or not any native method remains).
#include "com_sun_glass_ui_Window_Level.h"
// Clipboard / dialog pins: Clipboard.ACTION_* and CommonDialogs.Type.OPEN / SAVE, all @Native constants.
#include "com_sun_glass_ui_Clipboard.h"
#include "com_sun_glass_ui_CommonDialogs_Type.h"

/*
 * Win64 layouts the Java side of the WinRobot input binding relies on (glass_win_api.h, "WinRobot
 * input synthesis"). A failure here means the header comment - and the Java layouts - are stale.
 */
#if defined(_WIN64)
static_assert(sizeof(INPUT) == 40, "INPUT layout changed - update glass_win_api.h and the Java layouts");
static_assert(offsetof(INPUT, type) == 0, "INPUT.type");
static_assert(offsetof(INPUT, mi) == 8, "INPUT.mi");
static_assert(offsetof(INPUT, ki) == 8, "INPUT.ki");
static_assert(sizeof(MOUSEINPUT) == 32, "MOUSEINPUT size");
static_assert(offsetof(MOUSEINPUT, dx) == 0 && offsetof(MOUSEINPUT, dy) == 4 &&
              offsetof(MOUSEINPUT, mouseData) == 8 && offsetof(MOUSEINPUT, dwFlags) == 12 &&
              offsetof(MOUSEINPUT, time) == 16 && offsetof(MOUSEINPUT, dwExtraInfo) == 24,
              "MOUSEINPUT field offsets");
static_assert(sizeof(KEYBDINPUT) == 24, "KEYBDINPUT size");
static_assert(offsetof(KEYBDINPUT, wVk) == 0 && offsetof(KEYBDINPUT, wScan) == 2 &&
              offsetof(KEYBDINPUT, dwFlags) == 4 && offsetof(KEYBDINPUT, time) == 8 &&
              offsetof(KEYBDINPUT, dwExtraInfo) == 16,
              "KEYBDINPUT field offsets");
static_assert(sizeof(POINT) == 8 && offsetof(POINT, x) == 0 && offsetof(POINT, y) == 4, "POINT layout");

/*
 * WinMenuImpl (ABI 3): the MENUITEMINFOW the Java side lays out by hand for InsertMenuItemW /
 * SetMenuItemInfoW / GetMenuItemInfoW (WinMenuNativeTest asserts the same thirteen numbers from the
 * Java side), pinned in the same way as the WinRobot INPUT layouts above.
 */
static_assert(sizeof(MENUITEMINFOW) == 80, "MENUITEMINFOW: WinGlassNative's Java layout assumes 80 bytes on x64");
static_assert(offsetof(MENUITEMINFOW, cbSize) == 0 && offsetof(MENUITEMINFOW, fMask) == 4 &&
              offsetof(MENUITEMINFOW, fType) == 8 && offsetof(MENUITEMINFOW, fState) == 12 &&
              offsetof(MENUITEMINFOW, wID) == 16 && offsetof(MENUITEMINFOW, hSubMenu) == 24 &&
              offsetof(MENUITEMINFOW, hbmpChecked) == 32 && offsetof(MENUITEMINFOW, hbmpUnchecked) == 40 &&
              offsetof(MENUITEMINFOW, dwItemData) == 48 && offsetof(MENUITEMINFOW, dwTypeData) == 56 &&
              offsetof(MENUITEMINFOW, cch) == 64 && offsetof(MENUITEMINFOW, hbmpItem) == 72,
              "MENUITEMINFOW field offsets");
#endif

/* The constants the header comment quotes, pinned to the SDK. */
static_assert(INPUT_MOUSE == 0 && INPUT_KEYBOARD == 1, "INPUT.type values");
static_assert(KEYEVENTF_EXTENDEDKEY == 0x0001 && KEYEVENTF_KEYUP == 0x0002, "KEYEVENTF_*");
static_assert(MAPVK_VK_TO_VSC == 0 && MAPVK_VK_TO_CHAR == 2, "MAPVK_*");
static_assert(MOUSEEVENTF_MOVE == 0x0001 && MOUSEEVENTF_ABSOLUTE == 0x8000 &&
              MOUSEEVENTF_LEFTDOWN == 0x0002 && MOUSEEVENTF_LEFTUP == 0x0004 &&
              MOUSEEVENTF_RIGHTDOWN == 0x0008 && MOUSEEVENTF_RIGHTUP == 0x0010 &&
              MOUSEEVENTF_MIDDLEDOWN == 0x0020 && MOUSEEVENTF_MIDDLEUP == 0x0040 &&
              MOUSEEVENTF_XDOWN == 0x0080 && MOUSEEVENTF_XUP == 0x0100 &&
              MOUSEEVENTF_WHEEL == 0x0800, "MOUSEEVENTF_*");
static_assert(XBUTTON1 == 1 && XBUTTON2 == 2 && WHEEL_DELTA == 120, "XBUTTON* / WHEEL_DELTA");
static_assert(SM_CXSCREEN == 0 && SM_CYSCREEN == 1 && SM_SWAPBUTTON == 23, "SM_*");
static_assert(VK_PRIOR == 0x21 && VK_NEXT == 0x22 && VK_END == 0x23 && VK_HOME == 0x24 &&
              VK_LEFT == 0x25 && VK_UP == 0x26 && VK_RIGHT == 0x27 && VK_DOWN == 0x28 &&
              VK_SNAPSHOT == 0x2C && VK_INSERT == 0x2D && VK_DELETE == 0x2E &&
              VK_NUMPAD0 == 0x60 && VK_NUMPAD9 == 0x69 && VK_NUMLOCK == 0x90, "extended VK_* values");

/*
 * WinTimer exports nothing: WinGlassNative binds winmm!timeGetDevCaps, timeBeginPeriod,
 * timeEndPeriod, timeSetEvent and timeKillEvent itself, so glass.dll no longer links winmm. These
 * pin the layout and the constants that binding assumes (mmsystem.h still arrives via common.h).
 */
static_assert(sizeof(TIMECAPS) == 8 && offsetof(TIMECAPS, wPeriodMin) == 0
        && offsetof(TIMECAPS, wPeriodMax) == 4, "TIMECAPS layout");
static_assert(TIME_PERIODIC == 0x0001 && TIMERR_NOERROR == 0, "mmsystem timer constants");

/*
 * WinCursor / WinPixels export nothing either: WinGlassNative binds user32!ShowCursor,
 * user32!CreateIconIndirect, gdi32!CreateBitmap, gdi32!CreateDIBSection, gdi32!GdiFlush and
 * gdi32!DeleteObject itself and builds the ICONINFO / BITMAPINFOHEADER by hand. These pin the
 * constants and the two struct layouts that binding hardcodes; a Windows SDK that disagrees is a
 * compile error here instead of a wrong cursor at runtime.
 */
static_assert(SM_CXCURSOR == 13, "WinGlassNative.SM_CXCURSOR");
static_assert(SM_CYCURSOR == 14, "WinGlassNative.SM_CYCURSOR");
static_assert(sizeof(BITMAPINFOHEADER) == 40, "WinGlassNative.BITMAPINFOHEADER_LAYOUT");
static_assert(offsetof(BITMAPINFOHEADER, biBitCount) == 14, "WinGlassNative.BITMAPINFOHEADER_LAYOUT");
static_assert(offsetof(BITMAPINFOHEADER, biCompression) == 16, "WinGlassNative.BITMAPINFOHEADER_LAYOUT");
static_assert(offsetof(BITMAPINFOHEADER, biSizeImage) == 20, "WinGlassNative.BITMAPINFOHEADER_LAYOUT");
static_assert(sizeof(ICONINFO) == 32, "WinGlassNative.ICONINFO_LAYOUT");
static_assert(offsetof(ICONINFO, xHotspot) == 4, "WinGlassNative.ICONINFO_LAYOUT");
static_assert(offsetof(ICONINFO, yHotspot) == 8, "WinGlassNative.ICONINFO_LAYOUT");
static_assert(offsetof(ICONINFO, hbmMask) == 16, "WinGlassNative.ICONINFO_LAYOUT");
static_assert(offsetof(ICONINFO, hbmColor) == 24, "WinGlassNative.ICONINFO_LAYOUT");
static_assert(BI_RGB == 0, "WinGlassNative.BI_RGB");
static_assert(DIB_RGB_COLORS == 0, "WinGlassNative.DIB_RGB_COLORS");

/*
 * The Glass helper files (ABI 2). WinPreferences reads the eight GetSysColor indices and the two
 * SystemParametersInfoW keys from Java now, with the constants and the HIGHCONTRAST layout hard-coded
 * in WinGlassNative. These pin every one of them to the Windows SDK: a wrong Java constant becomes a
 * compile error here instead of a plausible wrong colour, a silently missing preference key, or - for
 * the layout - a SystemParametersInfoW that returns FALSE and drops both high-contrast keys.
 * WinPreferencesNativeTest and WinGlassNativeTest.constantsMatchWinuser assert the same numbers from
 * the Java side, which catches a mistake on this machine; these catch an SDK that disagrees.
 */
static_assert(VK_CAPITAL == 0x14, "WinGlassNative.VK_CAPITAL");
static_assert(SPI_GETHIGHCONTRAST == 0x0042, "WinGlassNative.SPI_GETHIGHCONTRAST");
static_assert(SPI_GETCLIENTAREAANIMATION == 0x1042, "WinGlassNative.SPI_GETCLIENTAREAANIMATION");
static_assert(HCF_HIGHCONTRASTON == 0x00000001, "WinGlassNative.HCF_HIGHCONTRASTON");
static_assert(sizeof(HIGHCONTRASTW) == 16, "WinGlassNative.HIGHCONTRAST_LAYOUT");
static_assert(offsetof(HIGHCONTRASTW, cbSize) == 0, "WinGlassNative.HIGHCONTRAST_LAYOUT");
static_assert(offsetof(HIGHCONTRASTW, dwFlags) == 4, "WinGlassNative.HIGHCONTRAST_LAYOUT");
static_assert(offsetof(HIGHCONTRASTW, lpszDefaultScheme) == 8, "WinGlassNative.HIGHCONTRAST_LAYOUT");
static_assert(COLOR_3DFACE == 15 && COLOR_BTNTEXT == 18 && COLOR_GRAYTEXT == 17
        && COLOR_HIGHLIGHT == 13 && COLOR_HIGHLIGHTTEXT == 14 && COLOR_HOTLIGHT == 26
        && COLOR_WINDOW == 5 && COLOR_WINDOWTEXT == 8, "WinPreferences GetSysColor indices");

/*
 * The two preferences structs. gwin_sizeof_ui_settings / gwin_sizeof_network_info let a Java test
 * compare a MemoryLayout against the real sizeof; these say what that sizeof has to be, so a padding
 * surprise is a compile error on every platform that ever builds this file, not a failing test on one.
 */
static_assert(sizeof(GwinUiSettings) == 60, "GwinUiSettings layout - update glass_win_api.h");
static_assert(offsetof(GwinUiSettings, colors) == 8, "GwinUiSettings.colors");
static_assert(offsetof(GwinUiSettings, advanced_effects_valid) == 44, "GwinUiSettings.advanced_effects_valid");
static_assert(offsetof(GwinUiSettings, auto_hide_valid) == 52, "GwinUiSettings.auto_hide_valid");
static_assert(sizeof(GwinNetworkInfo) == 8, "GwinNetworkInfo layout - update glass_win_api.h");
static_assert(offsetof(GwinNetworkInfo, cost_type) == 4, "GwinNetworkInfo.cost_type");

/*
 * WinView / WinGestureSupport (ABI 4). The two multi-click metrics Java reads through
 * user32!GetSystemMetrics with the indices hard-coded in WinGlassNative (the former
 * _getMultiClickMaxX_impl / _getMultiClickMaxY_impl), and the byte sizes of the two callback tables
 * the Java layouts hard-code - glass_win_api.h pins them as N * sizeof(void*) on every platform,
 * this pins the x64 number the MemoryLayouts carry.
 */
static_assert(SM_CXDOUBLECLK == 36, "WinGlassNative.SM_CXDOUBLECLK");
static_assert(SM_CYDOUBLECLK == 37, "WinGlassNative.SM_CYDOUBLECLK");
#if defined(_WIN64)
static_assert(sizeof(GwinViewCallbacks) == 80, "WinGlassNative.VIEW_CALLBACKS_LAYOUT");
static_assert(sizeof(GwinGestureCallbacks) == 40, "WinGlassNative.GESTURE_CALLBACKS_LAYOUT");
#endif

/*
 * WinWindow (ABI 4): the table size, the RECT WinGlassNative fills for the _getAnchor WRAPPER's
 * user32!GetWindowRect, the IDC_* ordinals the Java Cursor -> HCURSOR mapping passes to
 * user32!LoadCursorW (MAKEINTRESOURCE values, the ones GlassCursor.cpp's GetNativeCursor picked), and
 * the HT* values WinWindow.nonClientHitTest returns / GwinWindowCallbacks.non_client_hit_test
 * documents. Java hardcodes every one of these; nothing else pins them.
 */
#if defined(_WIN64)
static_assert(sizeof(GwinWindowCallbacks) == 96, "WinGlassNative.WINDOW_CALLBACKS_LAYOUT");
#endif
static_assert(sizeof(RECT) == 16 && offsetof(RECT, left) == 0 && offsetof(RECT, top) == 4 &&
              offsetof(RECT, right) == 8 && offsetof(RECT, bottom) == 12, "WinGlassNative.RECT_LAYOUT");
static_assert((uintptr_t) IDC_ARROW == 32512 && (uintptr_t) IDC_IBEAM == 32513 &&
              (uintptr_t) IDC_WAIT == 32514 && (uintptr_t) IDC_CROSS == 32515 &&
              (uintptr_t) IDC_SIZENWSE == 32642 && (uintptr_t) IDC_SIZENESW == 32643 &&
              (uintptr_t) IDC_SIZEWE == 32644 && (uintptr_t) IDC_SIZENS == 32645 &&
              (uintptr_t) IDC_SIZEALL == 32646 && (uintptr_t) IDC_HAND == 32649,
              "WinGlassNative.IDC_* - the ordinals the former GlassCursor.cpp GetNativeCursor picked");
static_assert(HTNOWHERE == 0 && HTCLIENT == 1 && HTCAPTION == 2 && HTMINBUTTON == 8 && HTMAXBUTTON == 9 &&
              HTTOP == 12 && HTCLOSE == 20, "WinWindow.nonClientHitTest HT* values");

/*
 * Clipboard / DnD / dialogs (ABI 4): the byte sizes of the two callback tables and of GwinFileFilter
 * the Java layouts hard-code, the CommonDialogs.Type values gwin_dialog_file receives (the C switches
 * on the generated constants, Java passes the @Native ints), and the Clipboard.ACTION_* values the
 * C reads through the generated Clipboard header - Java pins its own copies of the same five.
 */
#if defined(_WIN64)
static_assert(sizeof(GwinClipboardCallbacks) == 56, "WinGlassNative.CLIPBOARD_CALLBACKS_LAYOUT");
static_assert(sizeof(GwinDndCallbacks) == 72, "WinGlassNative.DND_CALLBACKS_LAYOUT");
static_assert(sizeof(GwinFileFilter) == 16, "WinGlassNative.FILE_FILTER_LAYOUT");
static_assert(offsetof(GwinFileFilter, extensions) == 8, "WinGlassNative.FILE_FILTER_LAYOUT.extensions");
#endif
static_assert(com_sun_glass_ui_CommonDialogs_Type_OPEN == 0 && com_sun_glass_ui_CommonDialogs_Type_SAVE == 1,
              "CommonDialogs.Type.OPEN / SAVE - gwin_dialog_file's `type` argument");
static_assert(com_sun_glass_ui_Clipboard_ACTION_NONE == 0 && com_sun_glass_ui_Clipboard_ACTION_COPY == 1
              && com_sun_glass_ui_Clipboard_ACTION_MOVE == 2
              && com_sun_glass_ui_Clipboard_ACTION_REFERENCE == 0x40000000
              && com_sun_glass_ui_Clipboard_ACTION_ANY == 0x4FFFFFFF,
              "Clipboard.ACTION_* values glass_win_api.h documents");

/*
 * Application / screens (ABI 5): the screen table the Java layout hard-codes, and every Win32 layout
 * and constant the Java port of GlassScreen.cpp's former enumeration and the display-change probe of
 * tests/system hard-code - nothing else pins them. MONITORINFOEXW derives from MONITORINFO in the C++
 * headers, so offsetof is taken on the base (a standard-layout type).
 */
#if defined(_WIN64)
static_assert(sizeof(GwinScreenCallbacks) == 8, "WinGlassNative.SCREEN_CALLBACKS_LAYOUT");
#endif
static_assert(sizeof(MONITORINFO) == 40 && offsetof(MONITORINFO, rcMonitor) == 4
        && offsetof(MONITORINFO, rcWork) == 20 && offsetof(MONITORINFO, dwFlags) == 36
        && sizeof(MONITORINFOEXW) == 104 && CCHDEVICENAME == 32,
        "WinGlassNative.MONITORINFOEXW_LAYOUT (szDevice at 40)");
static_assert(MONITORINFOF_PRIMARY == 1, "WinGlassNative.MONITORINFOF_PRIMARY");
static_assert(BITSPIXEL == 12 && PLANES == 14 && LOGPIXELSX == 88 && LOGPIXELSY == 90, "GetDeviceCaps indices");
static_assert(USER_DEFAULT_SCREEN_DPI == 96, "WinScreenLayout's 96.0f");
static_assert(WM_SETTINGCHANGE == 0x001A && SPI_SETWORKAREA == 0x002F && WM_DISPLAYCHANGE == 0x007E
        && WM_DPICHANGED == 0x02E0, "the tests/system display-change probe");
/*
 * The two shcore enums Java passes as ints: GetDpiForMonitor's MONITOR_DPI_TYPE (WinGlassNative.MDT_*)
 * and all three PROCESS_DPI_AWARENESS levels WinApplication can hand SetProcessDpiAwareness. Pinned to
 * the SDK's own definitions; the former GlassScreen.cpp pin checked only that file's private copies of
 * the enums and went with them.
 */
static_assert(MDT_EFFECTIVE_DPI == 0 && MDT_RAW_DPI == 2, "WinGlassNative.MDT_EFFECTIVE_DPI / MDT_RAW_DPI");
static_assert(PROCESS_DPI_UNAWARE == 0 && PROCESS_SYSTEM_DPI_AWARE == 1 && PROCESS_PER_MONITOR_DPI_AWARE == 2,
              "WinApplication's three DPI awareness levels (WinGlassNative.PROCESS_PER_MONITOR_DPI_AWARE)");

/* Robot.cpp WinToJavaPixel, verbatim (jint -> int32_t). */
inline static int32_t WinToJavaPixel(USHORT r, USHORT g, USHORT b)
{
    int32_t value =
            0xFF << 24 | // alpha channel is always turned all the way up
            r << 16 |
            g << 8  |
            b << 0;
    return value;
}

/* Records the first failure of the capture sequence; later failures do not overwrite it. */
static void NoteFailure(int32_t &status, BOOL succeeded, int32_t code)
{
    if (!succeeded && status == GWIN_OK) {
        status = code;
    }
}

/*
 * Robot.cpp GetScreenCapture, step for step. The only additions are the handle / BOOL checks feeding
 * NoteFailure: every step still runs after a failure, exactly as the JNI did, so the buffer holds the
 * same content afterwards; the return value names the first step that failed. The JNI's hOldPalette
 * branch (always NULL, never selected) is not carried over.
 */
static int32_t CaptureScreen(int32_t x, int32_t y, int32_t width, int32_t height, int32_t *pixelData)
{
    int32_t status = GWIN_OK;

    HDC hdcScreen = ::CreateDC(TEXT("DISPLAY"), NULL, NULL, NULL);
    NoteFailure(status, hdcScreen != NULL, GWIN_ROBOT_ERR_CREATE_DC);
    HDC hdcMem = ::CreateCompatibleDC(hdcScreen);
    NoteFailure(status, hdcMem != NULL, GWIN_ROBOT_ERR_CREATE_MEM_DC);
    HBITMAP hbitmap;
    HBITMAP hOldBitmap;

    // create an offscreen bitmap
    hbitmap = ::CreateCompatibleBitmap(hdcScreen, width, height);
    NoteFailure(status, hbitmap != NULL, GWIN_ROBOT_ERR_CREATE_BITMAP);
    hOldBitmap = (HBITMAP)::SelectObject(hdcMem, hbitmap);

    // copy screen image to offscreen bitmap
    // CAPTUREBLT flag is required to capture WS_EX_LAYERED windows' contents
    // correctly on Win2K/XP
    static const DWORD dwRop = SRCCOPY|CAPTUREBLT;
    NoteFailure(status, ::BitBlt(hdcMem, 0, 0, width, height, hdcScreen, x, y, dwRop), GWIN_ROBOT_ERR_BITBLT);

    static const int BITS_PER_PIXEL = 32;

    struct {
        BITMAPINFOHEADER bmiHeader;
        RGBQUAD          bmiColors[3];
    } BitmapInfo;

    // prepare BITMAPINFO for a 32-bit RGB bitmap
    ::memset(&BitmapInfo, 0, sizeof(BitmapInfo));
    BitmapInfo.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    BitmapInfo.bmiHeader.biWidth = width;
    BitmapInfo.bmiHeader.biHeight = -height; // negative height means a top-down DIB
    BitmapInfo.bmiHeader.biPlanes = 1;
    BitmapInfo.bmiHeader.biBitCount = BITS_PER_PIXEL;
    BitmapInfo.bmiHeader.biCompression = BI_BITFIELDS;

    // Setup up color masks
    static const RGBQUAD redMask =   {0, 0, 0xFF, 0};
    static const RGBQUAD greenMask = {0, 0xFF, 0, 0};
    static const RGBQUAD blueMask =  {0xFF, 0, 0, 0};

    BitmapInfo.bmiColors[0] = redMask;
    BitmapInfo.bmiColors[1] = greenMask;
    BitmapInfo.bmiColors[2] = blueMask;

    // Get the bitmap data in device-independent, 32-bit packed pixel format
    int scanLines = ::GetDIBits(hdcMem, hbitmap, 0, height, pixelData, (BITMAPINFO *)&BitmapInfo, DIB_RGB_COLORS);
    NoteFailure(status, scanLines > 0, GWIN_ROBOT_ERR_GETDIBITS);

    // convert Win32 pixel format (BGRX) to Java format (ARGB)
    static_assert(sizeof(int32_t) == sizeof(RGBQUAD), "the swizzle reinterprets each int as an RGBQUAD");
    int32_t numPixels = width * height;
    int32_t *pPixel = pixelData;
    for(int nPixel = 0; nPixel < numPixels; nPixel++) {
        RGBQUAD * prgbq = (RGBQUAD *) pPixel;
        *pPixel++ = WinToJavaPixel(prgbq->rgbRed, prgbq->rgbGreen, prgbq->rgbBlue);
    }

    // free all the GDI objects we made
    ::SelectObject(hdcMem, hOldBitmap);
    ::DeleteObject(hbitmap);
    ::DeleteDC(hdcMem);
    ::DeleteDC(hdcScreen);

    return status;
}


/*
 * ---- Application: the state behind glass_win_api.h's loop / invoke family ----
 *
 * The application callback table. Written once by gwin_app_set_callbacks, from Java, on the launcher
 * thread before the Glass toolkit thread exists; read on the toolkit thread only - the launchable
 * inside gwin_run_loop, and every WM_DO_ACTION / WM_DO_ACTION_LATER the toolkit window dispatches.
 * No lock, by design, same as g_prefsCallbacks in PlatformSupport.cpp; see gwin_app_set_callbacks in
 * glass_win_api.h.
 */
static GwinAppCallbacks g_appCallbacks = { NULL };
static void* g_appUser = NULL;

/* A NULL slot is tolerated and does nothing, which is what a NULL jmethodID did. */
static void RunRunnable(int64_t runnableId)
{
    if (g_appCallbacks.run_runnable != NULL) {
        g_appCallbacks.run_runnable(g_appUser, runnableId);
    }
}

/*
 * What ENTER_MAIN_THREAD's _MyAction was, with the JGlobalRef<jobject> replaced by the int64 id.
 * GlassApplication::WindowProc's WM_DO_ACTION / WM_DO_ACTION_LATER arm calls Do() and, for the LATER
 * form only, deletes the action afterwards - so gwin_invoke_and_wait passes a stack instance and
 * gwin_invoke_later a heap one, exactly as LEAVE_MAIN_THREAD and LEAVE_MAIN_THREAD_LATER did.
 */
class GwinRunnableAction : public Action {
public:
    explicit GwinRunnableAction(int64_t runnableId) : m_runnableId(runnableId) {}

    virtual void Do()
    {
        RunRunnable(m_runnableId);
    }

private:
    int64_t m_runnableId;
};

/*
 * ---- View: the opaque handle and its marshal helper ----
 */
static inline GlassView* ToGlassView(gwin_view_t view)
{
    return static_cast<GlassView*>(view);
}

// Helper LEAVE_MAIN_THREAD for the view exports: GlassView.cpp's LEAVE_MAIN_THREAD_WITH_view over
// the opaque handle instead of the jlong. Inside the action `view` is the GlassView*.
#define LEAVE_MAIN_THREAD_WITH_view  \
    GlassView * view;  \
    LEAVE_MAIN_THREAD;  \
    ARG(view) = ToGlassView(view);

// Helper LEAVE_MAIN_THREAD for the window exports: GlassWindow.cpp's LEAVE_MAIN_THREAD_WITH_hWnd over
// the opaque handle instead of the jlong. Inside the action `hWnd` is the HWND.
#define LEAVE_MAIN_THREAD_WITH_hWnd  \
    HWND hWnd;  \
    LEAVE_MAIN_THREAD;  \
    ARG(hWnd) = (HWND) win;

// The pointer the three test hooks below (gwin_test_fire_window_callback, _clipboard_callback,
// _dnd_callback) hand to a void* slot: bits above 32 set so a descriptor that truncates it shows,
// never dereferenced by this library. The Java tests transcribe the value from glass_win_api.h.
#define GWIN_TEST_HOOK_POINTER ((void*) (uintptr_t) 0x00007FFE12345678ULL)

extern "C" {

int32_t gwin_abi_version(void)
{
    return GLASS_WIN_ABI_VERSION;
}

int32_t gwin_robot_capture(int32_t x, int32_t y, int32_t width, int32_t height, int32_t *argb, int64_t argb_len)
{
    // The four rejections of Java_com_sun_glass_ui_win_WinRobot__1getScreenCapture, in its order.
    if (argb == NULL) {
        return GWIN_ERR_INVALID_ARG;
    }
    if (width <= 0 || height <= 0) {
        return GWIN_ERR_INVALID_ARG;
    }

    const int32_t maxPixels = INT32_MAX / (int32_t) sizeof(int32_t);
    if (width >= maxPixels / height) {
        return GWIN_ERR_INVALID_ARG;
    }

    const int32_t numPixels = width * height;
    if (argb_len < numPixels) {
        return GWIN_ERR_INVALID_ARG;
    }

    return CaptureScreen(x, y, width, height, argb);
}

int32_t gwin_key_java_to_windows(int32_t java_key_code)
{
    UINT vkey = 0;
    UINT modifiers = 0;
    JavaKeyToWindowsKey(java_key_code, vkey, modifiers);
    return (int32_t) vkey;
}

int32_t gwin_key_windows_to_java(int32_t vk)
{
    return WindowsKeyToJavaKey((UINT) vk);
}

/*
 * ---- Application (glass_win_api.h) ----
 *
 * The statements below are the former JNI bodies of WinApplication._runLoop, _terminateLoop,
 * _invokeAndWait and _submitForLaterInvocation (GlassApplication.cpp) moved, not rewritten. The
 * two nested-loop entry points forward to GlassApplication, which owns sm_shouldLeaveNestedLoop.
 */
int32_t gwin_sizeof_app_callbacks(void)
{
    return (int32_t) sizeof(GwinAppCallbacks);
}

int32_t gwin_app_set_callbacks(const GwinAppCallbacks* cb, void* user)
{
    if (cb == NULL) {
        g_appCallbacks.run_runnable = NULL;
        g_appUser = NULL;
    } else {
        g_appCallbacks = *cb;   // by value: this library never retains the caller's struct
        g_appUser = user;
    }
    return GWIN_OK;
}

/*
 * WinApplication._init's former JNI body (GlassApplication.cpp) without its DPI-awareness step, over the
 * no-JNI constructor: the object is deleted again when no toolkit window exists afterwards.
 */
void* gwin_app_create(void)
{
    try {
        GlassApplication *pApp = new GlassApplication();

        HWND hWnd = GlassApplication::GetToolkitHWND();
        if (hWnd == NULL) {
            delete pApp;
        }

        return (void*) hWnd;
    } catch (...) {
        return NULL;
    }
}

void gwin_run_loop(int64_t launchable_id)
{
    OLEHolder _ole_;
    if (launchable_id != 0) {
        RunRunnable(launchable_id);
    }

    MSG msg;
    // The GlassApplication instance may be destroyed in a nested loop.
    // Note that we leave the WM_QUIT message on the queue but who cares?
    while (GlassApplication::GetInstance() && ::GetMessage(&msg, NULL, 0, 0) > 0) {
        ::TranslateMessage(&msg);
        ::DispatchMessage(&msg);
    }

    if (GlassApplication::GetAccessibilityCount() > 0 && !IS_WIN8) {
        // Bug in Windows 7. For some reason, JavaFX crashes when the application
        // is shutting down while Narrator (the screen reader) is running. It is
        // suspected the crash happens because the event thread is finalized while
        // accessible objects are still receiving release messages. Not all the
        // circumstances around this crash are well understood,  but calling
        // GetMessage() one last time fixes the crash.
        UINT_PTR timerId = ::SetTimer(NULL, NULL, 1000, NULL);
        ::GetMessage(&msg, NULL, 0, 0);
        ::KillTimer(NULL, timerId);
    }
}

void gwin_terminate_loop(void)
{
    HWND hWnd = GlassApplication::GetToolkitHWND();
    if (::IsWindow(hWnd)) {
        ::DestroyWindow(hWnd);
    }
}

void gwin_enter_nested_event_loop(void)
{
    GlassApplication::EnterNestedEventLoop();
}

void gwin_leave_nested_event_loop(void)
{
    GlassApplication::LeaveNestedEventLoop();
}

int32_t gwin_invoke_and_wait(int64_t runnable_id)
{
    if (GlassApplication::GetInstance() == NULL) {
        return GWIN_ERR_NO_TOOLKIT;
    }

    // A stack action, which is what LEAVE_MAIN_THREAD built: ExecAction uses SendMessage and so
    // does not return until WindowProc has called Do(). ExecAction re-checks pInstance, so a window
    // torn down in between still cannot be messaged; that benign race is pre-existing.
    GwinRunnableAction action(runnable_id);
    GlassApplication::ExecAction(&action);
    return GWIN_OK;
}

int32_t gwin_invoke_later(int64_t runnable_id)
{
    // Read the toolkit window ONCE. This function is callable from any thread, so testing
    // GetInstance() and then calling GetToolkitHWND() would leave a window in which the HWND comes
    // back NULL - and PostMessage(NULL, ...) posts a thread message to the CALLER instead of
    // failing, which would leak the action and report GWIN_OK for a runnable that never runs.
    // ExecActionLater read pInstance twice too, but dereferenced it, so it crashed instead.
    HWND hWnd = GlassApplication::GetToolkitHWND();
    if (hWnd == NULL) {
        return GWIN_ERR_NO_TOOLKIT;
    }

    // A heap action, which is what LEAVE_MAIN_THREAD_LATER built: the WM_DO_ACTION_LATER arm of
    // GlassApplication::WindowProc deletes it after Do(). Deleting it when PostMessage fails is the
    // one deliberate deviation from ExecActionLater, which ignored PostMessage's result and leaked.
    GwinRunnableAction* action = new GwinRunnableAction(runnable_id);
    if (!::PostMessage(hWnd, WM_DO_ACTION_LATER, (WPARAM) action, (LPARAM) 0)) {
        delete action;
        return GWIN_ERR_NO_TOOLKIT;
    }
    return GWIN_OK;
}

/*
 * Preferences: gwin_prefs_set_callbacks, gwin_prefs_query_ui_settings and gwin_prefs_query_network
 * live in PlatformSupport.cpp, which owns the WinRT objects and the RO_CHECKED machinery. Only the
 * two sizeof probes belong here, beside the static_asserts that pin the same numbers.
 */
int32_t gwin_sizeof_ui_settings(void)
{
    return (int32_t) sizeof(GwinUiSettings);
}

int32_t gwin_sizeof_network_info(void)
{
    return (int32_t) sizeof(GwinNetworkInfo);
}

/*
 * Menus: gwin_menu_set_callbacks and the table it writes live in GlassMenu.cpp, which owns
 * HandleMenuCommand - the one reader. Only the sizeof probe belongs here, beside the other two.
 */
int32_t gwin_sizeof_menu_callbacks(void)
{
    return (int32_t) sizeof(GwinMenuCallbacks);
}

/*
 * ---- WinView + WinGestureSupport (glass_win_api.h) ----
 *
 * The tables live in GlassView (three TUs read them); the installers forward there. The eleven
 * gwin_view_* bodies below are the former JNI bodies of GlassView.cpp moved onto the opaque handle,
 * marshalled with the same ENTER_MAIN_THREAD / LEAVE_MAIN_THREAD macros, and wrapped
 * try { } catch (...) as the header promises. _uploadPixels' GDI sequence lives in
 * GlassView::UploadPixels, which gwin_view_upload_pixels calls; the JNI body that shared it (and
 * resolved the Pixels jobject on the toolkit thread) is gone.
 */
int32_t gwin_sizeof_view_callbacks(void)
{
    return (int32_t) sizeof(GwinViewCallbacks);
}

int32_t gwin_sizeof_gesture_callbacks(void)
{
    return (int32_t) sizeof(GwinGestureCallbacks);
}

int32_t gwin_view_set_callbacks(const GwinViewCallbacks* cb)
{
    GlassView::SetViewCallbacks(cb);   // by value, NULL slots become no-ops; cb == NULL clears
    return GWIN_OK;
}

int32_t gwin_gesture_set_callbacks(const GwinGestureCallbacks* cb)
{
    GlassView::SetGestureCallbacks(cb);
    return GWIN_OK;
}

int64_t gwin_test_fire_callback(int32_t slot, int64_t view_id)
{
    static const uint16_t chars[2] = { 0x0041, 0x0042 };
    static const int32_t bounds[2] = { 0, 2 };
    static const uint8_t attrs[1] = { 1 };

    try {
        if (slot >= 0 && slot <= 9) {
            const GwinViewCallbacks* cb = GlassView::ViewCallbacks();
            if (cb == NULL) {
                return GWIN_ERR_INVALID_ARG;
            }
            switch (slot) {
                case 0: cb->notify_view(view_id, 1001); return 0;
                case 1: cb->notify_resize(view_id, 1001, 1002); return 0;
                case 2: cb->notify_repaint(view_id, 1001, 1002, 1003, 1004); return 0;
                case 3: cb->notify_menu(view_id, 1001, 1002, 1003, 1004, 1); return 0;
                case 4: cb->notify_key(view_id, 1001, 1002, chars, 2, 1003); return 0;
                case 5: cb->notify_mouse(view_id, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1, 0); return 0;
                case 6: cb->notify_scroll(view_id, 1001, 1002, 1003, 1004, 1.5, 2.5, 1005, 1006, 1007,
                                          1008, 1009, 3.5, 4.5); return 0;
                case 7: cb->notify_input_method(view_id, chars, 2, bounds, 1, bounds, attrs, 1,
                                                1001, 1002, 1003); return 0;
                case 8: {
                    double xy[2] = { 0.0, 0.0 };
                    return cb->notify_ime_candidate_pos_request(view_id, 1001, xy);
                }
                case 9: return cb->get_accessible(view_id);
            }
        } else if (slot >= 100 && slot <= 104) {
            const GwinGestureCallbacks* cb = GlassView::GestureCallbacks();
            if (cb == NULL) {
                return GWIN_ERR_INVALID_ARG;
            }
            switch (slot) {
                case 100: cb->gesture_performed(view_id, 1001, 1, 0, 1002, 1003, 1004, 1005,
                                                1.5f, 2.5f, 3.5f, 4.5f, 5.5f, 6.5f, 7.5f); return 0;
                case 101: cb->inertia_gesture_finished(view_id); return 0;
                case 102: cb->notify_begin_touch_event(view_id, 1001, 1, 1002); return 0;
                case 103: cb->notify_next_touch_event(view_id, 1001, (int64_t) 0x100000002LL,
                                                      1002, 1003, 1004, 1005); return 0;
                case 104: cb->notify_end_touch_event(view_id); return 0;
            }
        }
        return GWIN_ERR_INVALID_ARG;
    } catch (...) {
        return GWIN_ERR_INVALID_ARG;
    }
}

/*
 * The statement of the former Java_com_sun_glass_ui_win_WinView__1create (GlassView.cpp; deleted under
 * ABI 5). The object must not be allocated
 * zeroed (calloc / memset): m_InputMethodEventsEnabled stays as uninitialised as it always was, and the
 * constructor, which does not set it, is what runs.
 */
gwin_view_t gwin_view_create(int64_t view_id)
{
    try {
        return (gwin_view_t) new GlassView(view_id);
    } catch (...) {
        return NULL;
    }
}

int32_t gwin_view_close(gwin_view_t view)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            int32_t result = view->Close() ? 1 : 0;
            delete view;
            return result;
        }
        LEAVE_MAIN_THREAD_WITH_view;

        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

void* gwin_view_get_native_view(gwin_view_t view)
{
    try {
        return (void*) ToGlassView(view)->GetHostHwnd();
    } catch (...) {
        return NULL;
    }
}

void gwin_view_set_parent(gwin_view_t view, void* parent_hwnd)
{
    try {
        ENTER_MAIN_THREAD()
        {
            // The action may send ADD/REMOVE events. Let them be on the main thread
            view->SetHostHwnd((HWND) parent_hwnd);
        }
        void* parent_hwnd;
        LEAVE_MAIN_THREAD_WITH_view;

        ARG(parent_hwnd) = parent_hwnd;
        PERFORM();
    } catch (...) {
    }
}

int32_t gwin_view_get_x(gwin_view_t view)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            HWND hWnd = view->GetHostHwnd();

            if (!hWnd) {
                return 0;
            }

            RECT rect1, rect2;

            ::GetWindowRect(hWnd, &rect1);
            ::GetClientRect(hWnd, &rect2);
            ::MapWindowPoints(hWnd, (HWND)NULL, (LPPOINT)&rect2, (sizeof(RECT)/sizeof(POINT)));

            return rect2.left - rect1.left;
        }
        LEAVE_MAIN_THREAD_WITH_view;

        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

int32_t gwin_view_get_y(gwin_view_t view)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            HWND hWnd = view->GetHostHwnd();

            if (!hWnd) {
                return 0;
            }

            RECT rect1, rect2;

            ::GetWindowRect(hWnd, &rect1);
            ::GetClientRect(hWnd, &rect2);
            ::MapWindowPoints(hWnd, (HWND)NULL, (LPPOINT)&rect2, (sizeof(RECT)/sizeof(POINT)));

            return rect2.top - rect1.top;
        }
        LEAVE_MAIN_THREAD_WITH_view;

        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

void gwin_view_schedule_repaint(gwin_view_t view)
{
    try {
        ENTER_MAIN_THREAD()
        {
            if (view->GetHostHwnd()) {
                ::InvalidateRect(view->GetHostHwnd(), NULL, FALSE);
            }
        }
        LEAVE_MAIN_THREAD_WITH_view;

        PERFORM();
    } catch (...) {
    }
}

void gwin_view_upload_pixels(gwin_view_t view, int32_t width, int32_t height, const void* bits)
{
    try {
        ENTER_MAIN_THREAD()
        {
            view->UploadPixels(width, height, bits);
        }
        int32_t width;
        int32_t height;
        const void* bits;
        LEAVE_MAIN_THREAD_WITH_view;

        ARG(width) = width;
        ARG(height) = height;
        ARG(bits) = bits;
        PERFORM();
    } catch (...) {
    }
}

int32_t gwin_view_enter_fullscreen(gwin_view_t view, int32_t animate, int32_t keep_ratio, int32_t hide_cursor)
{
    // hide_cursor is accepted and ignored, as the JNI ignored it.
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            return view->EnterFullScreen(animate, keepRatio) ? 1 : 0;
        }
        BOOL animate;
        BOOL keepRatio;
        LEAVE_MAIN_THREAD_WITH_view;

        ARG(animate) = animate != 0 ? TRUE : FALSE;
        ARG(keepRatio) = keep_ratio != 0 ? TRUE : FALSE;
        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

void gwin_view_exit_fullscreen(gwin_view_t view, int32_t animate)
{
    try {
        ENTER_MAIN_THREAD()
        {
            view->ExitFullScreen(animate);
        }
        BOOL animate;
        LEAVE_MAIN_THREAD_WITH_view;

        ARG(animate) = animate != 0 ? TRUE : FALSE;
        PERFORM();
    } catch (...) {
    }
}

void gwin_view_enable_ime(gwin_view_t view, int32_t enable)
{
    try {
        // the JNI jboolean rule, kept: only 1 enables.
        ToGlassView(view)->EnableInputMethodEvents(enable == 1 ? TRUE : FALSE);
    } catch (...) {
    }
}

void gwin_view_finish_ime_composition(gwin_view_t view)
{
    try {
        ToGlassView(view)->FinishInputMethodComposition();
    } catch (...) {
    }
}


/*
 * ---- WinWindow (glass_win_api.h) ----
 *
 * The table lives in GlassWindow (its 11 upcall sites and ~GlassWindow read it); the installer
 * forwards there. The bodies below are the former JNI bodies of GlassWindow.cpp moved onto the
 * opaque HWND, marshalled with the same ENTER_MAIN_THREAD / LEAVE_MAIN_THREAD macros, and wrapped
 * try { } catch (...) as the header promises. Three action bodies are GlassWindow statics
 * (CreateFromMask, DoSetView, DoSetVisible) because they need the generated com_sun_glass_ui_Window_*
 * mask bits or GlassWindow.cpp's file-static activeTouchWindow. The JNI entry points that shared them,
 * and the _setIcon / _setCursor JNI bodies with their Pixels.attachData and Cursor -> HCURSOR upcalls,
 * are gone: Java resolves the HICON / HCURSOR, and gwin_window_set_icon /
 * gwin_window_set_cursor hand it to GlassWindow::SetIcon / ApplyCursor.
 */
int32_t gwin_sizeof_window_callbacks(void)
{
    return (int32_t) sizeof(GwinWindowCallbacks);
}

int32_t gwin_window_set_callbacks(const GwinWindowCallbacks* cb)
{
    GlassWindow::SetWindowCallbacks(cb);   // by value, NULL slots become no-ops; cb == NULL clears
    return GWIN_OK;
}

int64_t gwin_test_fire_window_callback(int32_t slot, int64_t window_id, int32_t* out_bounds)
{
    try {
        const GwinWindowCallbacks* cb = GlassWindow::WindowCallbacks();
        if (cb == NULL) {
            return GWIN_ERR_INVALID_ARG;
        }
        switch (slot) {
            case 0: cb->notify_close(window_id); return 0;
            case 1: cb->notify_destroy(window_id); return 0;
            case 2:
                if (out_bounds == NULL) {
                    return GWIN_ERR_INVALID_ARG;
                }
                return cb->notify_moving(window_id, 1001, 1002, 1003, 1004, 1.5f, 2.5f, 1005, 1006, 1,
                                         1007, 1008, 1009, 1010, out_bounds);
            case 3: cb->notify_move(window_id, 1001, 1002); return 0;
            case 4: cb->notify_resize(window_id, 1001, 1002, 1003); return 0;
            case 5: cb->notify_scale_changed(window_id, 1.5f, 2.5f, 3.5f, 4.5f); return 0;
            case 6: cb->notify_focus(window_id, 1001); return 0;
            case 7: cb->notify_focus_disabled(window_id); return 0;
            case 8: cb->notify_focus_ungrab(window_id); return 0;
            case 9: cb->notify_delegate_ptr(window_id, GWIN_TEST_HOOK_POINTER); return 0;
            case 10: return cb->non_client_hit_test(window_id, 1001, 1002);
            case 11: cb->notify_dispose(window_id); return 0;
        }
        return GWIN_ERR_INVALID_ARG;
    } catch (...) {
        return GWIN_ERR_INVALID_ARG;
    }
}

gwin_window_t gwin_window_create(void* owner_hwnd, void* monitor, int32_t mask, int64_t window_id)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(HWND)
        {
            return GlassWindow::CreateFromMask(windowId, owner, hMonitor, mask);
        }
        int64_t windowId;
        HWND owner;
        HMONITOR hMonitor;
        int32_t mask;
        LEAVE_MAIN_THREAD;

        ARG(windowId) = window_id;
        ARG(owner) = (HWND) owner_hwnd;
        ARG(hMonitor) = (HMONITOR) monitor;
        ARG(mask) = mask;

        return (gwin_window_t) PERFORM_AND_RETURN();
    } catch (...) {
        return NULL;
    }
}

int32_t gwin_window_close(gwin_window_t win)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            pWindow->Close();
            return ::DestroyWindow(hWnd) ? 1 : 0;
        }
        LEAVE_MAIN_THREAD_WITH_hWnd;

        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

int32_t gwin_window_set_view(gwin_window_t win, gwin_view_t view)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow::DoSetView(hWnd, view);
        }
        GlassView * view;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(view) = ToGlassView(view);
        PERFORM();
        return 1;
    } catch (...) {
        return 0;
    }
}

void gwin_window_update_view_size(gwin_window_t win)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);

            // The condition below may be restricted to WS_POPUP windows
            if (::IsWindowVisible(hWnd)) {
                pWindow->NotifyViewSize(hWnd);
            }
        }
        LEAVE_MAIN_THREAD_WITH_hWnd;

        PERFORM();
    } catch (...) {
    }
}

int32_t gwin_window_set_menubar(gwin_window_t win, void* hmenu)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            if (::SetMenu(hWnd, hMenu))
            {
                GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
                if (pWindow) {
                    pWindow->SetMenu(hMenu);
                }

                return 1;
            }
            return 0;
        }
        HMENU hMenu;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(hMenu) = (HMENU) hmenu;
        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

void gwin_window_set_level(gwin_window_t win, int32_t level)
{
    try {
        ENTER_MAIN_THREAD()
        {
            ::SetWindowPos(hWnd, hWndInsertAfter, 0, 0, 0, 0,
                    SWP_ASYNCWINDOWPOS | SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOOWNERZORDER | SWP_NOSIZE);
        }
        HWND hWndInsertAfter;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(hWndInsertAfter) = HWND_NOTOPMOST;
        switch (level) {
            case com_sun_glass_ui_Window_Level_FLOATING:
            case com_sun_glass_ui_Window_Level_TOPMOST:
                ARG(hWndInsertAfter) = HWND_TOPMOST;
                break;
        }
        PERFORM();
    } catch (...) {
    }
}

void gwin_window_set_focusable(gwin_window_t win, int32_t focusable)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            pWindow->SetFocusable(isFocusable);
        }
        bool isFocusable;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(isFocusable) = focusable == 1;   // the JNI compared its jboolean against JNI_TRUE
        PERFORM();
    } catch (...) {
    }
}

void gwin_window_set_enabled(gwin_window_t win, int32_t enabled)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            pWindow->SetEnabled(isEnabled);
            ::EnableWindow(hWnd, isEnabled);
        }
        bool isEnabled;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(isEnabled) = enabled == 1;   // the JNI compared its jboolean against JNI_TRUE
        PERFORM();
    } catch (...) {
    }
}

void gwin_window_set_alpha(gwin_window_t win, float alpha)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            pWindow->SetAlpha(alpha);
        }
        BYTE alpha;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(alpha) = F2B(alpha);
        PERFORM();
    } catch (...) {
    }
}

void gwin_window_set_dark_frame(gwin_window_t win, int32_t dark)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            if (pWindow) {
                pWindow->SetDarkFrame(dark);
            }
        }
        bool dark;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(dark) = dark != 0;
        PERFORM();
    } catch (...) {
    }
}

int64_t gwin_window_get_insets(gwin_window_t win)
{
    try {
        HWND hWnd = (HWND) win;
        if (!::IsWindow(hWnd)) return 0;
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);

        pWindow->UpdateInsets();
        RECT is = pWindow->GetInsets();

        return ((((int64_t) is.left)  << 48) |
                (((int64_t) is.top)   << 32) |
                (((int64_t) is.right) << 16) |
                (((int64_t) is.bottom)     ));
    } catch (...) {
        return 0;
    }
}

void gwin_window_set_bounds(gwin_window_t win,
                            int32_t x, int32_t y, int32_t x_set, int32_t y_set,
                            int32_t w, int32_t h, int32_t cw, int32_t ch,
                            float x_gravity, float y_gravity)
{
    (void) x_gravity;   // declared and never read, as in the JNI
    (void) y_gravity;
    try {
        ENTER_MAIN_THREAD()
        {
            if (!::IsWindow(hWnd)) return;
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);

            pWindow->UpdateInsets();
            RECT is = pWindow->GetInsets();

            RECT r;
            ::GetWindowRect(hWnd, &r);

            int newX = xSet == 1 ? x : r.left;   // the JNI jboolean rule: == 1
            int newY = ySet == 1 ? y : r.top;
            int newW = w > 0 ? w :
                           cw > 0 ? cw + is.right + is.left : r.right - r.left;
            int newH = h > 0 ? h :
                           ch > 0 ? ch + is.bottom + is.top : r.bottom - r.top;

            POINT minSize = pWindow->getMinSize();
            POINT maxSize = pWindow->getMaxSize();
            if (minSize.x >= 0) newW = max(newW, minSize.x);
            if (minSize.y >= 0) newH = max(newH, minSize.y);
            if (maxSize.x >= 0) newW = min(newW, maxSize.x);
            if (maxSize.y >= 0) newH = min(newH, maxSize.y);

            if (xSet || ySet) {   // non-zero, NOT == 1 - as the JNI tested it
                ::SetWindowPos(hWnd, NULL, newX, newY, newW, newH,
                               SWP_NOACTIVATE | SWP_NOZORDER | SWP_NOSENDCHANGING);
            } else {
                ::SetWindowPos(hWnd, NULL, 0, 0, newW, newH,
                               SWP_NOACTIVATE | SWP_NOZORDER | SWP_NOMOVE | SWP_NOSENDCHANGING);
            }
        }
        int32_t x, y;
        int32_t xSet, ySet;
        int32_t w, h, cw, ch;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(x) = x;
        ARG(y) = y;
        ARG(xSet) = x_set;
        ARG(ySet) = y_set;
        ARG(w) = w;
        ARG(h) = h;
        ARG(cw) = cw;
        ARG(ch) = ch;
        PERFORM();
    } catch (...) {
    }
}

int32_t gwin_window_set_title(gwin_window_t win, const uint16_t* title)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            if (::SetWindowText(hWnd, title)) {
                return 1;
            }
            return 0;
        }
        LPCTSTR title;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(title) = (LPCTSTR) title;
        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

int32_t gwin_window_set_resizable(gwin_window_t win, int32_t resizable)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            if (pWindow && pWindow->SetResizable(resizable == 1)) {   // the JNI jboolean rule: == 1
                return 1;
            }

            return 0;
        }
        int32_t resizable;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(resizable) = resizable;
        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

int32_t gwin_window_set_visible(gwin_window_t win, int32_t visible)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow::DoSetVisible(hWnd, visible);
        }
        bool visible;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(visible) = visible != 0;
        PERFORM();
        return visible;
    } catch (...) {
        return 0;
    }
}

int32_t gwin_window_request_focus(gwin_window_t win, int32_t event)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            return (pWindow && pWindow->RequestFocus(event)) ? 1 : 0;
        }
        int32_t event;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(event) = event;

        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

int32_t gwin_window_grab_focus(gwin_window_t win)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            return (pWindow && pWindow->GrabFocus()) ? 1 : 0;
        }
        LEAVE_MAIN_THREAD_WITH_hWnd;

        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

void gwin_window_ungrab_focus(gwin_window_t win)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            if (pWindow) {
                pWindow->UngrabFocus();
            }
        }
        LEAVE_MAIN_THREAD_WITH_hWnd;

        PERFORM();
    } catch (...) {
    }
}

int32_t gwin_window_minimize(gwin_window_t win, int32_t minimize)
{
    try {
        ENTER_MAIN_THREAD()
        {
            ::ShowWindow(hWnd, minimize ? SW_MINIMIZE : SW_RESTORE);
        }
        int32_t minimize;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(minimize) = minimize;
        PERFORM();

        return 1;
    } catch (...) {
        return 0;
    }
}

int32_t gwin_window_maximize(gwin_window_t win, int32_t maximize, int32_t was_maximized)
{
    (void) was_maximized;   // declared and never read, as in the JNI
    try {
        ENTER_MAIN_THREAD()
        {
            ::ShowWindow(hWnd, maximize ? SW_MAXIMIZE : SW_RESTORE);
        }
        int32_t maximize;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(maximize) = maximize;
        PERFORM();

        return 1;
    } catch (...) {
        return 0;
    }
}

int32_t gwin_window_set_min_size(gwin_window_t win, int32_t width, int32_t height)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            if (pWindow) {
                pWindow->setMinSize(minWidth, minHeight);
                return 1;
            }
            return 0;
        }
        int32_t minWidth;
        int32_t minHeight;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(minWidth) = width == 0 ? -1 : width;
        ARG(minHeight) = height == 0 ? -1 : height;
        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

int32_t gwin_window_set_max_size(gwin_window_t win, int32_t width, int32_t height)
{
    try {
        ENTER_MAIN_THREAD_AND_RETURN(int32_t)
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            if (pWindow) {
                pWindow->setMaxSize(maxWidth, maxHeight);
                return 1;
            }
            return 0;
        }
        int32_t maxWidth;
        int32_t maxHeight;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(maxWidth) = width;
        ARG(maxHeight) = height;
        return PERFORM_AND_RETURN();
    } catch (...) {
        return 0;
    }
}

void gwin_window_set_icon(gwin_window_t win, void* hicon)
{
    try {
        HWND hWnd = (HWND) win;
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        if (pWindow) {
            pWindow->SetIcon((HICON) hicon);
        }
    } catch (...) {
    }
}

void gwin_window_to_front(gwin_window_t win)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            // See the comment in gwin_window_set_visible / __1setVisible about unfocusable windows
            if (pWindow && !pWindow->IsFocusable()) {
                ::SetWindowPos(hWnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
            }
            ::SetWindowPos(hWnd, HWND_TOP, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
        }
        LEAVE_MAIN_THREAD_WITH_hWnd;

        PERFORM();
    } catch (...) {
    }
}

void gwin_window_to_back(gwin_window_t win)
{
    try {
        ENTER_MAIN_THREAD()
        {
            ::SetWindowPos(hWnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
        }
        LEAVE_MAIN_THREAD_WITH_hWnd;

        PERFORM();
    } catch (...) {
    }
}

void gwin_window_set_cursor(gwin_window_t win, void* hcursor)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            if (pWindow) {
                pWindow->ApplyCursor(cursor);
            }
        }
        HCURSOR cursor;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(cursor) = (HCURSOR) hcursor;
        PERFORM();
    } catch (...) {
    }
}

void gwin_window_show_system_menu(gwin_window_t win, int32_t x, int32_t y)
{
    try {
        ENTER_MAIN_THREAD()
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            if (pWindow) {
                pWindow->ShowSystemMenu(x, y);
            }
        }
        int32_t x, y;
        LEAVE_MAIN_THREAD_WITH_hWnd;

        ARG(x) = x;
        ARG(y) = y;
        PERFORM();
    } catch (...) {
    }
}

/*
 * ---- Screens (glass_win_api.h) ----
 *
 * The table is a GlassScreen.cpp static (GlassScreen::HandleDisplayChange is its one reader); the
 * installer forwards there.
 */
int32_t gwin_sizeof_screen_callbacks(void)
{
    return (int32_t) sizeof(GwinScreenCallbacks);
}

int32_t gwin_screen_set_callbacks(const GwinScreenCallbacks* cb)
{
    GlassScreen::SetScreenCallbacks(cb);   // by value, a NULL slot becomes a no-op; cb == NULL clears
    return GWIN_OK;
}

int64_t gwin_test_fire_screen_callback(int32_t slot)
{
    try {
        const GwinScreenCallbacks* cb = GlassScreen::ScreenCallbacks();
        if (cb == NULL) {
            return GWIN_ERR_INVALID_ARG;
        }
        switch (slot) {
            case 0: cb->settings_changed(); return 0;
        }
        return GWIN_ERR_INVALID_ARG;
    } catch (...) {
        return GWIN_ERR_INVALID_ARG;
    }
}

/*
 * ---- Clipboard, drag-and-drop, common dialogs (glass_win_api.h) ----
 *
 * Only the installers, the probes and the allocator pair live here; the tables are GlassClipboard.cpp
 * statics (three TUs dial them) and the functional bodies sit beside the state they touch - see the
 * file comment.
 */
int32_t gwin_sizeof_clipboard_callbacks(void)
{
    return (int32_t) sizeof(GwinClipboardCallbacks);
}

int32_t gwin_sizeof_dnd_callbacks(void)
{
    return (int32_t) sizeof(GwinDndCallbacks);
}

int32_t gwin_sizeof_file_filter(void)
{
    return (int32_t) sizeof(GwinFileFilter);
}

int32_t gwin_clipboard_set_callbacks(const GwinClipboardCallbacks* cb)
{
    SetGlassClipboardCallbacks(cb);   // by value, NULL slots become no-ops; cb == NULL clears
    return GWIN_OK;
}

int32_t gwin_dnd_set_callbacks(const GwinDndCallbacks* cb)
{
    SetGlassDndCallbacks(cb);
    return GWIN_OK;
}

int64_t gwin_test_fire_clipboard_callback(int32_t slot, int64_t clipboard_id)
{
    static const uint16_t mime[3] = { 0x0041, 0x0042, 0 };

    try {
        const GwinClipboardCallbacks* cb = GlassClipboardCallbacks();
        if (cb == NULL) {
            return GWIN_ERR_INVALID_ARG;
        }
        switch (slot) {
            case 0: {
                uint8_t* data = NULL;
                int32_t len = -1;
                int32_t status = cb->fos_serialize(clipboard_id, mime, (int64_t) 0x100000002LL, &data, &len);
                // Owned from the moment the slot returned, on every path - as ClipboardData::GetData's
                // GwinOwnedBytes holder frees it.
                gwin_free(data);
                if (status != GWIN_OK) {
                    return status;
                }
                return data == NULL ? -100 : len;   // -100: Java returned null (see the header)
            }
            case 1: return cb->action_performed(clipboard_id, 1001);
            case 2: cb->drag_action_performed(clipboard_id, 1001); return 0;
            case 3: cb->content_changed(clipboard_id); return 0;
            case 4: cb->dispose_peer(clipboard_id); return 0;
            case 5: cb->set_data_object(clipboard_id, GWIN_TEST_HOOK_POINTER); return 0;
            case 6: cb->data_object_disposed(clipboard_id); return 0;
        }
        return GWIN_ERR_INVALID_ARG;
    } catch (...) {
        return GWIN_ERR_INVALID_ARG;
    }
}

int64_t gwin_test_fire_dnd_callback(int32_t slot, int64_t view_id, void* out)
{
    try {
        const GwinDndCallbacks* cb = GlassDndCallbacks();
        if (cb == NULL) {
            return GWIN_ERR_INVALID_ARG;
        }
        if (out == NULL && (slot == 0 || slot == 1 || slot == 2 || slot == 4 || slot == 8)) {
            return GWIN_ERR_INVALID_ARG;
        }
        switch (slot) {
            case 0: return cb->drag_enter(view_id, 1001, 1002, 1003, 1004, 1005, (int32_t*) out);
            case 1: return cb->drag_over(view_id, 1001, 1002, 1003, 1004, 1005, (int32_t*) out);
            case 2: return cb->drag_drop(view_id, 1001, 1002, 1003, 1004, 1005, (int32_t*) out);
            case 3: return cb->drag_leave(view_id);
            case 4: return cb->dnd_get_data_object((void**) out);
            case 5: return cb->dnd_set_data_object(GWIN_TEST_HOOK_POINTER);
            case 6: return cb->dnd_set_source_supported_actions(1001);
            case 7: return cb->dnd_set_drag_button(1001);
            case 8: return cb->dnd_get_drag_button((int32_t*) out);
        }
        return GWIN_ERR_INVALID_ARG;
    } catch (...) {
        return GWIN_ERR_INVALID_ARG;
    }
}

int32_t gwin_test_string_block(uint16_t** out_block, int32_t* out_count)
{
    try {
        if (out_block == NULL || out_count == NULL) {
            return GWIN_ERR_INVALID_ARG;
        }
        *out_block = NULL;
        *out_count = 0;
        std::vector<std::wstring> items;
        items.push_back(L"alpha");
        items.push_back(L"");
        items.push_back(L"\x03B3\xD83D\xDE00");   // U+03B3, then U+1F600 as its surrogate pair
        uint16_t* block = GwinMakeStringBlock(items);   // malloc'd: what gwin_free releases
        if (block == NULL) {
            return GWIN_ERR_INVALID_ARG;
        }
        *out_block = block;
        *out_count = (int32_t) items.size();
        return GWIN_OK;
    } catch (...) {
        return GWIN_ERR_INVALID_ARG;
    }
}

void* gwin_alloc(int64_t size)
{
    if (size < 0 || (uint64_t) size > (uint64_t) (SIZE_MAX - 1)) {
        return NULL;
    }
    // An empty array is a real block, never NULL: NULL means "Java returned null" to fos_serialize.
    return malloc(size == 0 ? 1 : (size_t) size);
}

void gwin_free(void* block)
{
    free(block);   // free(NULL) is a no-op
}

} // extern "C"

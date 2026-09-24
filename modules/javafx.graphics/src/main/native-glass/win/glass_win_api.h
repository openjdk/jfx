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
 * glass_win_api.h - the flat C ABI of the Windows Glass library (glass.dll). Windows only.
 *
 * This is the surface com.sun.glass.ui.win.WinGlassNative binds through java.lang.foreign. The library
 * knows nothing about the JVM: every function takes and returns <stdint.h> scalars or caller-owned
 * buffers, and the only calls back into Java are through the callback tables installed with
 * gwin_*_set_callbacks. No exception crosses this boundary: fallible functions return a status code,
 * the others return what the JNI entry point they replace returned.
 *
 * The header grows one WinGlass peer at a time. Each peer's exports are added beside
 * the Java_com_sun_glass_ui_win_* entry points they replace; the JNI functions are deleted only once
 * their Java callers have been flipped. First WinRobot (ABI 1: gwin_robot_*, gwin_key_*).
 * WinTimer added no exports by design: Java binds winmm!time* directly, so Timer.cpp went
 * away without anything taking its place and GLASS_WIN_ABI_VERSION stays 1.
 * WinCursor, and WinPixels._initIDs / _fillDirectByteBuffer, added ZERO exports, also
 * deliberately: Java binds gdi32!CreateBitmap, gdi32!CreateDIBSection, user32!CreateIconIndirect,
 * gdi32!GdiFlush and gdi32!DeleteObject directly and builds the cursor itself, so no C wrapper
 * around those OS calls was written - such a wrapper around OS calls is an anti-pattern. Nothing in
 * this header changed, so GLASS_WIN_ABI_VERSION stays 1.
 *
 * The Glass helper files (KeyTable.cpp and PlatformSupport.cpp) are the first peer since
 * WinRobot to add exports, so GLASS_WIN_ABI_VERSION goes 1 -> 2. They add six: gwin_key_code_for_char,
 * the OS-CALL half of WinApplication._getKeyCodeForChar - it needs two keyboard layouts and three
 * file-static KeyTable.cpp tables, so it stays native behind one export instead of being decomposed -
 * and the five preferences functions gwin_sizeof_ui_settings, gwin_sizeof_network_info,
 * gwin_prefs_set_callbacks, gwin_prefs_query_ui_settings and gwin_prefs_query_network, which keep the
 * WinRT half of PlatformSupport (RoActivateInstance / RoGetActivationFactory and the IUISettings /
 * INetworkInformationStatics vtables) native while handing the ten map-building upcalls to Java.
 * Java-side COM was considered for the WinRT half and rejected: it would need hard-coded IIDs,
 * hard-coded vtable slot indices and a fabricated COM vtable for the two event sinks, none of which
 * any test in this tree can verify. The same change deletes the JNI body of WinApplication._isKeyLocked,
 * whose Java caller now binds user32!GetKeyState itself.
 *
 * THE JAVA SIDE MUST MOVE WinGlassNative.ABI_VERSION 1 -> 2 IN THE SAME CHANGE SET: the facade
 * compares gwin_abi_version() against that constant in its class initializer and throws
 * UnsatisfiedLinkError on a mismatch, which fails WinRobot, WinTimer and WinCursor as well.
 *
 * WinApplication's loop/invoke family and WinMenuImpl land together as ONE ABI
 * bump, 2 -> 3, because gwin_abi_version is compared with exact equality in both directions and two
 * bumps would break the tree twice. They add ten exports: the eight of the application section
 * (gwin_sizeof_app_callbacks, gwin_app_set_callbacks, gwin_run_loop, gwin_terminate_loop,
 * gwin_enter_nested_event_loop, gwin_leave_nested_event_loop, gwin_invoke_and_wait,
 * gwin_invoke_later), which move the Win32 message pump, the nested-loop pump and the two
 * WM_DO_ACTION dispatch paths behind a C ABI and replace the three java.lang.Runnable.run upcalls
 * with one callback slot keyed by a Java-assigned int64 id; and the two of the menu section
 * (gwin_sizeof_menu_callbacks, gwin_menu_set_callbacks), which replace the single
 * WinMenuImpl.notifyCommand upcall that GlassMenu.cpp's HandleMenuCommand made for every
 * WM_COMMAND.
 *
 * THE JAVA SIDE MUST MOVE WinGlassNative.ABI_VERSION 2 -> 3 IN THE SAME CHANGE SET, for the same
 * reason the 1 -> 2 bump had to travel with its own half.
 *
 * WinView + WinGestureSupport (ABI 4) add sixteen exports, listed at the head of their section:
 * the two callback tables GwinViewCallbacks (10 slots, the former javaIDs.View jmethodIDs) and
 * GwinGestureCallbacks (5 slots, the former javaIDs.Gestures static jmethodIDs) with their
 * installers and sizeof probes, the eleven gwin_view_* entry points that were OS-CALL or native
 * state, and a test hook that fires any slot with a known pattern. The three multi-click getters
 * and the empty _begin / _end were WRAPPER or empty and get no export. The section landed ADDITIVE:
 * while no table was installed every one of the 26 upcall sites took its JNI path and every JNI
 * body kept running (both since removed), so it landed under ABI 3 without moving the
 * version. GLASS_WIN_ABI_VERSION went 3 -> 4 in the change set that flipped WinView.java /
 * WinGestureSupport.java onto the tables
 * - the same change set that gave the JNI WinView._create (gwin_view_create since ABI 5) its viewId
 * parameter and WinGlassNative.ABI_VERSION its 4. THE JAVA SIDE AND THAT BUMP TRAVELLED TOGETHER, for
 * the same reason as the two bumps before.
 *
 * WinWindow (ABI 4) adds twenty-nine exports, listed at the head of its section: the callback table
 * GwinWindowCallbacks (12 slots - the eleven jmethodIDs GlassWindow.cpp dialled plus notify_dispose,
 * which stands where ~GlassWindow's DeleteGlobalRef stood) with its installer and sizeof probe, and
 * the twenty-seven gwin_window_* entry points that were OS-CALL or native state. _getAnchor was a
 * WRAPPER (Java binds user32!IsWindow / GetCapture / GetCursorPos / GetWindowRect itself) and
 * _setBackground2 is dead; neither gets an export. The section landed ADDITIVE in the same sense as
 * the view section: while no table was installed every one of the 11 upcall sites took its JNI path and all 30
 * JNI bodies kept running (both since removed), so it landed under ABI 3 without moving the
 * version. The version moved with the view section's bump, in the change set that flipped WinWindow.java onto
 * the table. THE JAVA SIDE AND THAT BUMP TRAVELLED TOGETHER.
 *
 * WinSystemClipboard, WinDnDClipboard and WinCommonDialogs (ABI 4) add nineteen exports, listed at the
 * head of its section: the callback tables GwinClipboardCallbacks (7 slots - the three jmethodIDs of
 * WinSystemClipboard.initIDs, the dispose that GlassApplication::RegisterClipboardViewer re-entered,
 * the setPtr field write, and ~ClipboardData) and GwinDndCallbacks (9 slots - the four
 * View.notifyDrag* ids and the five WinDnDClipboard members GlassDnD.cpp reached through
 * ClassForName + getInstance) with their installers and sizeof probes, the allocator pair
 * gwin_alloc / gwin_free that lets a byte block cross the boundary with one owner, ten
 * gwin_clipboard_* / gwin_dnd_* entry points (OS-CALL: OLE, GDI, the process-wide mime maps and the
 * COM objects stay native), the two gwin_dialog_* entry points and GwinFileFilter with its probe.
 * WinSystemClipboard.isOwner was the one WRAPPER (Java binds ole32!OleIsCurrentClipboard) and the
 * two initIDs are JNI bookkeeping; neither gets an export. ADDITIVE like the view and window sections: with no
 * table installed every upcall site took its JNI path and all 15 JNI bodies kept running (both since
 * removed), so it landed under ABI 3. The version moved with the change set that flipped the
 * three peers - the SAME change set as the WinView flip, because the drag slots deliver the view
 * section's view_id. THE JAVA SIDE AND THAT BUMP TRAVELLED TOGETHER.
 *
 * WinApplication's last natives and WinView._create (ABI 5) add six exports: gwin_app_create
 * (in the application section), gwin_view_create (in the view section), and the screen
 * section after the window section - the one-slot GwinScreenCallbacks with its sizeof probe, installer
 * and test hook, and gwin_test_screen_anchor with its two structs. The monitor enumeration, the DPI
 * query and the FX-space anchoring get NO export: they are WRAPPER + PURE and move to Java; the table
 * carries only the display-change event the WndProc sees. They landed ADDITIVE under ABI 4 (with no table
 * installed GlassScreen::HandleDisplayChange took its JNI path); the version moved 4 -> 5 together with
 * WinGlassNative.ABI_VERSION, in the change set that binds them. THE JAVA SIDE AND THAT BUMP TRAVEL TOGETHER.
 * Beside them, additive as well and also under version 5: gwin_test_report_exception_in_downcall, the
 * shim-only test hook of the fix that let CheckAndClearException deliver exceptions from inside a
 * downcall; it went with the accessibility delete below, which left this library with no JNI to report
 * from. Still under version 5, WinApplication's and
 * WinView's six JNI bodies and the two test-only exports gwin_robot_pixel_color and
 * gwin_test_screen_anchor were then deleted (see the ABI 5 note at GLASS_WIN_ABI_VERSION).
 *
 * WinAccessible + WinTextRangeProvider, the last JNI of this library, add thirteen exports, listed at
 * the head of their section: the two callback tables GwinAccessibleCallbacks (70 slots - the 69
 * jmethodIDs of WinAccessible._initIDs plus accessible_disposed, which stands where ~GlassAccessible's
 * DeleteGlobalRef stood) and GwinTextRangeCallbacks (19 slots, the same shape for
 * WinTextRangeProvider) with their installers and sizeof probes, the four gwin_a11y_* entry points
 * that own the two COM objects' lifetime, gwin_a11y_raise_property_changed, GwinVariant with its
 * sizeof and offset probes, and the two fire hooks. UiaRaiseAutomationEvent and UiaClientsAreListening
 * are WRAPPERs and get NO export (Java binds UIAutomationCore itself) and the two _initIDs were JNI
 * bookkeeping that disappeared with them. The section landed ADDITIVE in the same sense as the view,
 * window and clipboard sections - while no table was installed every one of the 87 upcall sites took
 * its JNI arm and all nine JNI bodies kept running - and the nine Java_* bodies, the cached ids, the
 * two global refs, GlassAccessibleJni.cpp/.h and this library's JNI_OnLoad were then deleted. With
 * that deletion glass.dll exports no Java_* and no JNI_OnLoad and includes no javac -h header that
 * declares one; what remains of jni.h is the <jni.h> that the javac -h CONSTANTS headers pull in
 * (com_sun_glass_events_*.h, com_sun_glass_ui_*.h - eleven translation units include one), which is
 * why the glass target still asks win.cmake for the JDK include path. With no table installed the 87
 * sites now answer the E_FAIL their JNI arm answered without a JNIEnv.
 * GLASS_WIN_ABI_VERSION WENT 5 -> 6 IN THE CHANGE SET THAT FLIPPED WinAccessible.java and
 * WinTextRangeProvider.java ONTO THE TABLES, together with WinGlassNative.ABI_VERSION. THE JAVA SIDE
 * AND THAT BUMP TRAVELLED TOGETHER, for the reason every bump before it did: the facade compares
 * gwin_abi_version() with its own ABI_VERSION for exact equality in both directions, so a C-only bump
 * makes every Windows glass test fail with UnsatisfiedLinkError. BOTH SIDES NOW READ 6.
 *
 * Threading is a PER-FUNCTION rule, not a blanket one, and the per-function comment below is the
 * authority. Most functions declared here run on the JavaFX application thread, which on Windows is
 * the Glass/Win32 UI thread - WinRobot calls Application.checkEventThread() before each native, and the
 * JNI bodies assumed the same. What ties the WinRobot functions to that thread:
 * gwin_key_java_to_windows consults the CALLING thread's keyboard layout (MapVirtualKey, not the
 * MapVirtualKeyEx-with-HKL form). There are no locks.
 * The exceptions, all in the application section: gwin_run_loop does not run ON the Glass toolkit
 * thread, it DEFINES it - the thread that calls it is the toolkit thread for as long as it blocks;
 * and gwin_invoke_later is callable from ANY thread, its usual caller being Glass's
 * InvokeLaterDispatcher thread.
 *
 * None of the WinRobot functions upcalls or blocks on Java, so Linker.Option.critical(true) is legal for
 * all of them - that statement is scoped to WinRobot and must not be read as a property of this header;
 * see gwin_robot_capture for why an off-heap buffer is still the better default there.
 * critical(true) is FORBIDDEN on gwin_key_code_for_char (its user32 entry points enter the kernel and
 * can fault in a keyboard-layout DLL), on gwin_prefs_query_ui_settings / gwin_prefs_query_network
 * (they call across COM apartments into WinRT and can block), and on EVERY function of the
 * application section and of the menu section: gwin_run_loop blocks for the life of the application
 * and upcalls, gwin_enter_nested_event_loop blocks and upcalls, gwin_invoke_and_wait blocks and
 * upcalls, gwin_invoke_later posts to a window it does not own, gwin_terminate_loop destroys a
 * window, and the two set_callbacks functions publish a pointer another thread will dial.
 * The preferences callback table is the ABI's first upcall, and its two WinRT sinks are the first
 * thing here that does not run on the JavaFX application thread - see GwinPrefsCallbacks.
 * The view and window sections carry their own critical(true) and threading rules at their heads.
 */

#ifndef GLASS_WIN_API_H
#define GLASS_WIN_API_H

#include <stdint.h>

#if defined(_WIN32)
#  define GLASS_WIN_EXPORT __declspec(dllexport)
#else
#  define GLASS_WIN_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Must be bumped for a removal, or a prototype, struct-layout or enum-value change, in anything
 * WinGlassNative binds. An export added together with its Java binding rides the current version, and
 * shim-only test hooks never bump it. Java binds gwin_abi_version first, then every symbol it uses, eagerly.
 */
#define GLASS_WIN_ABI_VERSION 6
/* ABI 4 = the WinView / WinGestureSupport and WinWindow exports, flipped together; the clipboard / DnD /
 * dialog exports ride the same version - they are additive under it, as are the four test hooks
 * gwin_test_fire_window_callback / gwin_test_fire_clipboard_callback / gwin_test_fire_dnd_callback /
 * gwin_test_string_block added after the bump (two of them bound by the facade), siblings of the view
 * section's gwin_test_fire_callback.
 * ABI 5 = the six exports of the WinApplication / WinView._create flip (gwin_app_create, gwin_view_create,
 * gwin_sizeof_screen_callbacks, gwin_screen_set_callbacks, gwin_test_fire_screen_callback,
 * gwin_test_screen_anchor), paired with WinGlassNative.ABI_VERSION 5, and beside them
 * gwin_test_report_exception_in_downcall. Three test-only exports the facade never bound,
 * gwin_robot_pixel_color, gwin_test_screen_anchor and gwin_test_report_exception_in_downcall, were
 * later deleted without a further bump - the last of them with the accessibility JNI it reported for.
 * Every bump from 1 -> 2 to 4 -> 5 was taken at a peer flip - which the rule allows, but does not require.
 * ABI 6 = the accessibility section: its thirteen exports (gwin_sizeof_accessible_callbacks,
 * gwin_sizeof_text_range_callbacks, gwin_sizeof_variant, gwin_a11y_set_callbacks,
 * gwin_a11y_text_range_set_callbacks, gwin_a11y_create, gwin_a11y_destroy, gwin_a11y_text_range_create,
 * gwin_a11y_text_range_destroy, gwin_a11y_raise_property_changed, gwin_test_fire_accessible_callback,
 * gwin_test_fire_text_range_callback, gwin_test_variant_offsets), the two callback tables
 * GwinAccessibleCallbacks and GwinTextRangeCallbacks, and GwinVariant. They landed ADDITIVE under 5 -
 * nothing the facade already bound changed its prototype, layout or value - and the bump 5 -> 6 was
 * taken in the change set that flipped WinAccessible.java and WinTextRangeProvider.java onto the two
 * tables, deleted the nine Java_* arms and this library's JNI_OnLoad, and moved
 * WinGlassNative.ABI_VERSION 5 -> 6 with it.
 * SHIM-ONLY TEST HOOKS - exported, bound by WinGlassNativeShim and NOWHERE in WinGlassNative:
 * gwin_test_fire_window_callback, gwin_test_string_block, gwin_test_fire_screen_callback,
 * gwin_test_fire_accessible_callback, gwin_test_fire_text_range_callback and
 * gwin_test_variant_offsets. The other three hooks are bound by
 * the facade, so a removal or a prototype change of theirs bumps the version. */

GLASS_WIN_EXPORT int32_t gwin_abi_version(void);

/* Status codes. Negative: rejected before any OS call was made. Positive: the first OS step that failed. */
enum GwinStatus {
    GWIN_OK              = 0,
    GWIN_ERR_INVALID_ARG = -1,  /* NULL buffer, non-positive size, int overflow or a buffer that is too short */
    GWIN_ERR_NO_TOOLKIT  = -2,  /* No toolkit window - never created, or already destroyed. NOTHING was
                                 * scheduled and NO callback will arrive for the id that was passed, so
                                 * Java may drop its registry entry. The JNI entry points this replaces
                                 * failed exactly this silently and were declared void: ExecAction returned
                                 * without sending, ExecActionLater deleted the action and returned
                                 * (both in GlassApplication.cpp). */
    GWIN_ERR_UPCALL      = -3,  /* A callback slot reported a Java Throwable; the C mapped it to
                                 * E_JAVAEXCEPTION exactly as OleUtils.h's checkJavaException did.
                                 * (ABI 4) */
    GWIN_ERR_OLE         = -4   /* An OLE / COM step failed and the JNI body swallowed the HRESULT
                                 * (OLE_CATCH); reported so Java CAN see it, ignored so it stays
                                 * neutral. (ABI 4) */
};

/* gwin_robot_capture: the first step of Robot.cpp's GetScreenCapture GDI sequence that failed. */
enum GwinRobotCaptureError {
    GWIN_ROBOT_ERR_CREATE_DC     = 1,   /* CreateDC("DISPLAY") returned NULL                            */
    GWIN_ROBOT_ERR_CREATE_MEM_DC = 2,   /* CreateCompatibleDC returned NULL                             */
    GWIN_ROBOT_ERR_CREATE_BITMAP = 3,   /* CreateCompatibleBitmap(width, height) returned NULL          */
    GWIN_ROBOT_ERR_BITBLT        = 4,   /* BitBlt(SRCCOPY | CAPTUREBLT) from the screen returned FALSE  */
    GWIN_ROBOT_ERR_GETDIBITS     = 5    /* GetDIBits copied no scan lines                               */
};

/*
 * ---- WinRobot: screen capture (OS-CALL, stays native) ----
 *
 * Fills argb[0 .. width * height - 1] with the screen rectangle (x, y, width, height), row-major,
 * top-down, no row padding, one opaque 0xFFRRGGBB int per pixel. x and y are Windows (physical, unscaled)
 * pixels of the virtual screen, exactly what Java_com_sun_glass_ui_win_WinRobot__1getScreenCapture
 * received - no FX-to-Windows transform is applied (GlassRobot.getScreenCapture(WritableImage, ...) has
 * already scaled them by the primary screen's output scale). argb_len is the buffer length in ints.
 *
 * Argument checks, all -> GWIN_ERR_INVALID_ARG with the buffer untouched, are the JNI ones (which
 * returned silently): argb == NULL; width <= 0 or height <= 0; width >= (INT32_MAX / 4) / height;
 * argb_len < width * height.
 *
 * The GDI sequence is Robot.cpp's GetScreenCapture, unchanged: CreateDC("DISPLAY"), CreateCompatibleDC,
 * CreateCompatibleBitmap(width, height), SelectObject, BitBlt(mem, 0, 0, width, height, screen, x, y,
 * SRCCOPY | CAPTUREBLT), GetDIBits as a top-down 32-bit BI_BITFIELDS DIB (R 0x00FF0000, G 0x0000FF00,
 * B 0x000000FF), then the in-place BGRX -> ARGB swizzle (alpha forced to 0xFF), then the GDI objects are
 * released. A rectangle that is partly or wholly off the virtual screen is not clipped: BitBlt copies the
 * visible part and leaves the rest of the bitmap uninitialised (in practice zero, i.e. 0xFF000000 after
 * the swizzle), and the return value is still GWIN_OK because BitBlt still succeeds - same as the JNI.
 *
 * Return codes are the one addition. The JNI ignored every GDI failure and handed Java whatever the
 * buffer held; this function runs the same steps to the end after a failure, so the buffer ends up with
 * the same content (the swizzle has run over whatever GetDIBits left, alpha 0xFF everywhere), and then
 * returns the GwinRobotCaptureError of the FIRST failing step instead of GWIN_OK. Java may ignore the
 * code to stay behaviour-neutral. The only observable difference on failure: the JNI swizzled a fresh
 * uninitialised heap block, this function swizzles the caller's buffer.
 *
 * Duration: a synchronous screen read through GDI (CAPTUREBLT includes layered windows), typically
 * milliseconds to tens of milliseconds for large rectangles. A critical(true) downcall with a heap
 * int[] (MemorySegment.ofArray) is legal - no upcall, no blocking on Java - but pins the array and
 * blocks GC for the whole capture; an off-heap buffer with a plain downcall is the safe default.
 */
GLASS_WIN_EXPORT int32_t gwin_robot_capture(int32_t x, int32_t y, int32_t width, int32_t height,
                                            int32_t* argb, int64_t argb_len);

/*
 * ---- Key map (PURE, kept native: KeyTable.cpp is shared with the WndProc input path, one copy only) ----
 *
 * com.sun.glass.events.KeyEvent.VK_* code -> Windows virtual-key code, KeyTable.cpp's
 * JavaKeyToWindowsKey. Returns 0 when there is no VK for the code (KeyEvent.VK_UNDEFINED, a code
 * missing from the table, or an OEM key the current layout does not produce). The OEM step is
 * layout-dependent: when the table gives no VK, or gives one of the 13 OEM VKs (VK_OEM_1, VK_OEM_PLUS,
 * VK_OEM_COMMA, VK_OEM_MINUS, VK_OEM_PERIOD, VK_OEM_2 .. VK_OEM_8, VK_OEM_102), the table result is
 * discarded and the OEM VKs are searched for the one whose MapVirtualKey(vk, MAPVK_VK_TO_CHAR) character
 * (on the calling thread's keyboard layout; bit 31 = dead key) maps back to the requested Java code
 * through OEMCharToJavaKey - so e.g. VK_EQUALS yields VK_OEM_PLUS only on layouts where that key types
 * '='. The `modifiers` out-param of the C++ function is always 0 and is not exposed.
 */
GLASS_WIN_EXPORT int32_t gwin_key_java_to_windows(int32_t java_key_code);

/*
 * Windows virtual-key code -> KeyEvent.VK_* code, KeyTable.cpp's WindowsKeyToJavaKey: the first table
 * entry with that VK, or KeyEvent.VK_UNDEFINED (0) when there is none (including vk == 0 and any value
 * outside 0..255, which is passed to the UINT lookup unchanged). Both VK_LWIN and VK_RWIN map to
 * VK_WINDOWS; the other direction yields VK_LWIN, the first entry. Pure table lookup, no layout
 * dependency, no OEM character search - the OEM VKs map to their US-layout Java codes (VK_OEM_PLUS ->
 * VK_EQUALS, VK_OEM_1 -> VK_SEMICOLON, ...).
 */
GLASS_WIN_EXPORT int32_t gwin_key_windows_to_java(int32_t vk);

/*
 * ---- WinApplication._getKeyCodeForChar (OS-CALL, stays native) ----
 *
 * com.sun.glass.ui.win.WinApplication._getKeyCodeForChar, KeyTable.cpp, unchanged:
 *   c == 0x7F  -> KeyEvent.VK_DELETE, before any OS call (ViewContainer synthesises that character
 *                 for the Delete key and this reverses it).
 *   layout = GetKeyboardLayout(GlassApplication::GetMainThreadId()) - the Glass toolkit thread's
 *                 layout, and 0 (meaning the calling thread) until the toolkit window exists
 *                 (GlassApplication.h, GetMainThreadId).
 *   hint is a KeyEvent.VK_* code; when IsNumericKeypadCode(hint) and the hinted key maps back to c
 *                 through MapVirtualKeyEx(vk, MAPVK_VK_TO_CHAR, layout), hint is returned unchanged.
 *   otherwise vk = VkKeyScanEx(c, layout) & 0xFF; 0 or 0xFF -> KeyEvent.VK_UNDEFINED (0). The mask
 *                 is applied FIRST, so the shift-state high byte is discarded and never consulted.
 *   an OEM vk goes through MapVirtualKeyEx + OEMCharToJavaKey (bit 31 = dead key); everything else
 *                 through WindowsKeyToJavaKey.
 * c is the UTF-16 code unit the Java char holds. FFM has no char layout, so Java passes it as
 * JAVA_SHORT and the C reads the same 16 bits; the parameter is uint16_t, never a signed short.
 *
 * THREADING: this function reads TWO keyboard layouts. VkKeyScanEx and MapVirtualKeyEx use the
 *   toolkit thread's layout as above, but the numeric-keypad branch calls JavaKeyToWindowsKey, whose
 *   OEM search calls MapVirtualKey(oemKeys[i], MAPVK_VK_TO_CHAR) on the CALLING thread's layout -
 *   the same caller-thread binding the "Threading" paragraph at the top of this header states for
 *   the whole ABI. Call it on the thread the JNI was called on; do not move it or cache its result.
 *   (Application.getKeyCodeForChar does NOT call checkEventThread().)
 * Blocks: yes - these user32 entry points enter the kernel and can fault in a keyboard-layout DLL.
 *   critical(true) is FORBIDDEN on this downcall.
 */
GLASS_WIN_EXPORT int32_t gwin_key_code_for_char(uint16_t c, int32_t hint);

/*
 * ---- WinRobot input synthesis: WRAPPER, no C. Java binds user32 directly. ----
 *
 * This is what the JNI entry points did (Robot.cpp / KeyTable.cpp / GlassScreen.cpp at commit 8492cb03b0), so
 * that the Java binding can reproduce it exactly. Layouts on x64: INPUT is 40 bytes - DWORD type at 0,
 * the union at 8: MOUSEINPUT = LONG dx @8, LONG dy @12, DWORD mouseData @16, DWORD dwFlags @20,
 * DWORD time @24, ULONG_PTR dwExtraInfo @32; KEYBDINPUT = WORD wVk @8, WORD wScan @10, DWORD dwFlags
 * @12, DWORD time @16, ULONG_PTR dwExtraInfo @24 (glass_win_api.cpp static_asserts every offset).
 * POINT is LONG x @0, LONG y @4. Every SendInput call is SendInput(1, &input, 40) with time = 0 and
 * dwExtraInfo = 0, and its return value is ignored.
 *
 * keyPress / keyRelease(code) - Robot.cpp:35-60, KeyTable.cpp:315-343:
 *   vk = gwin_key_java_to_windows(code); if vk == 0 nothing is sent. INPUT_KEYBOARD (1): wVk = vk;
 *   wScan = MapVirtualKey(vk, MAPVK_VK_TO_VSC = 0) - calling thread's layout, low 16 bits, no 0xE0
 *   prefix; dwFlags = 0 for press, KEYEVENTF_KEYUP (0x0002) for release, OR-ed with
 *   KEYEVENTF_EXTENDEDKEY (0x0001) when vk is one of the 22 VKs of IsExtendedKey: VK_PRIOR 0x21,
 *   VK_NEXT 0x22, VK_END 0x23, VK_HOME 0x24, VK_LEFT 0x25, VK_UP 0x26, VK_RIGHT 0x27, VK_DOWN 0x28,
 *   VK_SNAPSHOT 0x2C, VK_INSERT 0x2D, VK_DELETE 0x2E, VK_NUMPAD0..VK_NUMPAD9 0x60..0x69, VK_NUMLOCK 0x90.
 *   KEYEVENTF_SCANCODE and KEYEVENTF_UNICODE are never set; no modifier key is synthesised, ordered or
 *   released on the caller's behalf.
 *
 * mouseMove(x, y) - Robot.cpp:101-114:
 *   fx = (float) x + 0.5f; fy = (float) y + 0.5f; FX2Win(&fx, &fy). INPUT_MOUSE (0): dwFlags =
 *   MOUSEEVENTF_MOVE (0x0001) | MOUSEEVENTF_ABSOLUTE (0x8000) - no MOUSEEVENTF_VIRTUALDESK, so the
 *   normalisation is to the PRIMARY monitor and points on other monitors fall outside 0..65535;
 *   dx = (int) (fx * 65536.0 / GetSystemMetrics(SM_CXSCREEN = 0)),
 *   dy = (int) (fy * 65536.0 / GetSystemMetrics(SM_CYSCREEN = 1)) - float widened to double, divided by
 *   the int metric in double, truncated toward zero; mouseData = 0.
 *
 * getMouseX / getMouseY - Robot.cpp:121-146:
 *   each is its own GetCursorPos(&pt) (WinRobot makes two calls for a location, return value ignored);
 *   fx = (float) pt.x + 0.5f; fy = (float) pt.y + 0.5f; Win2FX(&fx, &fy); return fx (resp. fy) as a
 *   float, which WinRobot widens to double.
 *
 * mousePress / mouseRelease(buttons) - Robot.cpp:153-234:
 *   swap = GetSystemMetrics(SM_SWAPBUTTON = 23) != 0. buttons bit 0 (GlassRobot.MOUSE_LEFT_BTN) ->
 *   MOUSEEVENTF_LEFTDOWN 0x0002, or RIGHTDOWN 0x0008 when swap; bit 1 (MOUSE_RIGHT_BTN) -> RIGHTDOWN,
 *   or LEFTDOWN when swap; bit 2 (MOUSE_MIDDLE_BTN) -> MIDDLEDOWN 0x0020; bit 3 (MOUSE_BACK_BTN) ->
 *   XDOWN 0x0080 and mouseData |= XBUTTON1 (1); bit 4 (MOUSE_FORWARD_BTN) -> XDOWN and mouseData |=
 *   XBUTTON2 (2). Release uses LEFTUP 0x0004, RIGHTUP 0x0010, MIDDLEUP 0x0040, XUP 0x0100. All flags are
 *   OR-ed into ONE INPUT_MOUSE (dx = dy = 0, no MOVE / ABSOLUTE), sent even when no bit was set.
 *
 * mouseWheel(wheelAmt) - Robot.cpp:241-245: the legacy user32!mouse_event, not SendInput:
 *   mouse_event(MOUSEEVENTF_WHEEL = 0x0800, 0, 0, (DWORD) (wheelAmt * -1 * WHEEL_DELTA (120)), 0) -
 *   positive wheelAmt scrolls down (negative delta), the negative int passed as a DWORD unchanged
 *   (two's complement), dx = dy = 0, dwExtraInfo = 0.
 *
 * FX2Win / Win2FX - the former GlassScreen::FX2Win / Win2FX, the FX <-> Windows pixel transform of
 *   mouseMove and getMouseX/Y; both are deleted (Win2FX under ABI 2, FX2Win under ABI 5)
 *   and WinScreenTransform.fx2Win / win2Fx, which documents the arithmetic, is the only copy.
 */

/*
 * ---- Platform preferences (PlatformSupport.cpp) ----
 *
 * PlatformSupport built a java.util.Map in C. Its eleven Call*Method / NewObject sites are
 * HashMap.<init>, Map.put x5, Color.rgb x2, Object.equals, Collections.unmodifiableMap and
 * Application.notifyPreferencesChanged, and two GetStaticObjectField reads of Boolean.TRUE/FALSE go
 * with them - all of it Java calling Java through C. Ten of the eleven go away because
 * Java can ask the OS itself: com.sun.glass.ui.win.WinPreferences reads the eight GetSysColor indices
 * and the two SystemParametersInfoW keys directly. What cannot go away is the WinRT half - the
 * IUISettings colours and flags and the INetworkInformationStatics connection cost - which needs
 * RoActivateInstance / RoGetActivationFactory, three generations of the IUISettings vtable and two
 * ITypedEventHandler sinks. Doing that from Java would mean hard-coded IIDs, hard-coded vtable slot
 * indices and a fabricated COM vtable built out of upcall stubs; nothing in this tree can test it, and
 * behaviour-neutrality outranks the pure-Java goal. So the WinRT objects stay where they are and this
 * ABI hands their results out as POD, and the eleventh upcall - the preferences-changed EVENT - stays
 * an upcall, because only native code knows when a preference may have changed.
 */

/* PlatformSupport::PreferenceType, PlatformSupport.h. A bitmask, not an index. */
enum GwinPreferenceType {
    GWIN_PT_SYSTEM_COLORS       = 1,
    GWIN_PT_SYSTEM_PARAMS       = 2,
    GWIN_PT_UI_SETTINGS         = 4,
    GWIN_PT_NETWORK_INFORMATION = 8,
    GWIN_PT_ALL                 = 15
};

/* Background, Foreground, AccentDark3..1, Accent, AccentLight1..3 - the order of the former
 * PlatformSupport::queryUISettings (collectUISettings keeps it), which is also the order WinPreferences
 * must name the keys in. */
#define GWIN_UI_COLOR_COUNT 9

/*
 * Three generations of IUISettings, each with its own validity flag, because the C returned out of the
 * former queryUISettings on the first RoException and so dropped every later key too (collectUISettings
 * returns at the same point). The flags are monotone:
 * !colors_valid => !advanced_effects_valid => !auto_hide_valid. A 0 flag means "no key at all for
 * this group", not "false" - the C emitted no map entry in that case, and Java must not either.
 *
 * colors are 0xAARRGGBB (ABI::Windows::UI::Color is BYTE A, R, G, B); Java rebuilds them as
 * Color.rgb(r, g, b, a / 255.0) in double, which is what putColor did.
 *
 * THREE unchecked HRESULTs are reproduced, not fixed: GetColorValue x9, get_AdvancedEffectsEnabled
 * and get_AutoHideScrollBars all ignore their HRESULT and the C then read an uninitialised stack
 * value on failure. The struct is zero-filled before any of them runs and the locals feeding it are
 * zero-initialised, so the same failure yields transparent black / false instead of garbage - a
 * deliberate, strictly safer deviation, the only one in this block.
 *
 * The two booleans come from an `unsigned char` handed to the former putBoolean(const bool), so the semantics
 * are "any nonzero is true": Java must test != 0, never == 1.
 */
typedef struct GwinUiSettings {
    int32_t  available;                     /* 0: UISettings could not be activated - no keys at all */
    int32_t  colors_valid;
    uint32_t colors[GWIN_UI_COLOR_COUNT];
    int32_t  advanced_effects_valid;
    int32_t  advanced_effects_enabled;
    int32_t  auto_hide_valid;
    int32_t  auto_hide_scroll_bars;
} GwinUiSettings;                           /* 15 x int32 = 60 bytes, 4-aligned, no padding */

/*
 * The Windows.NetworkInformation.InternetCostType value. These are NOT the WinRT NetworkCostType
 * enumerators (there Fixed is 2 and Variable is 3); gwin_prefs_query_network maps between them with
 * the same explicit switch the former PlatformSupport::queryNetworkInformation used to pick its string, so the
 * two enums are free to differ and Java pins these values, not the WinRT ones.
 */
enum GwinNetworkCost {
    GWIN_NET_COST_UNKNOWN      = 0,   /* "Unknown"      */
    GWIN_NET_COST_UNRESTRICTED = 1,   /* "Unrestricted" */
    GWIN_NET_COST_VARIABLE     = 2,   /* "Variable"     */
    GWIN_NET_COST_FIXED        = 3    /* "Fixed"        */
};

typedef struct GwinNetworkInfo {
    /*
     * 0: the activation factory was unavailable, or an RoException was caught before the C reached
     * its putString - in both cases there is NO key. 1 with cost_type == GWIN_NET_COST_UNKNOWN is the
     * different case where the C DID write the key, with the value "Unknown": either there is no
     * internet connection profile, or the profile reported a cost the switch does not name.
     */
    int32_t available;
    int32_t cost_type;
} GwinNetworkInfo;                          /* 8 bytes */

typedef struct GwinPrefsCallbacks {
    /*
     * The former PlatformSupport::updatePreferences, now on the Java side. `types` is a bitmask of
     * GwinPreferenceType; the values the native side actually sends are 2 (SPI_SETCLIENTAREAANIMATION),
     * 4 (the "ImmersiveColorSet" WM_SETTINGCHANGE and the AutoHideScrollBars sink), 5
     * (WM_THEMECHANGED / WM_SYSCOLORCHANGE / WM_DWMCOLORIZATIONCOLORCHANGED), 6 (SPI_SETHIGHCONTRAST)
     * and 8 (the NetworkStatusChanged sink). GWIN_PT_ALL is never sent here - that is the
     * collect-only path, WinPreferences.collect(GWIN_PT_ALL) behind WinApplication.getPlatformPreferences.
     *
     * RETURNS 1 if the preferences changed, 0 otherwise. GlassApplication::WindowProc uses it to
     * choose between `return 0` and DefWindowProc, so a wrong answer changes message handling.
     * The exact mapping the Java must reproduce:
     *     collect/compare threw             -> 0
     *     map unchanged                     -> 0
     *     Collections.unmodifiableMap threw -> 0, but `preferences` has ALREADY been reassigned
     *     the NOTIFY threw                  -> 1  (the C swallowed it and still returned true)
     * so the try/catch around notifyPreferencesChanged belongs INSIDE the Java update(), not only in
     * the upcall target's outer catch. The upcall target must catch Throwable and return a default:
     * an exception escaping an upcall stub terminates the JVM.
     *
     * THREAD: the WM_SETTINGCHANGE / WM_THEMECHANGED / WM_SYSCOLORCHANGE /
     * WM_DWMCOLORIZATIONCOLORCHANGED arms run on the Glass toolkit thread, but the two WinRT sinks
     * (AutoHideScrollBarsChanged, NetworkStatusChanged) are dispatched by WinRT on a thread of its
     * own choosing. This is the first callback in this ABI and the first thing here that is not
     * toolkit-thread-only; the stub must be thread-safe.
     *
     * LIFETIME: the two sinks are registered with add_AutoHideScrollBarsChanged /
     * add_NetworkStatusChanged and their EventRegistrationToken is discarded - there is no
     * remove_*Changed anywhere in this library, and PlatformSupport's destructor does not unregister
     * them. So there is no point at which native code provably can no longer call this table:
     * clearing it with gwin_prefs_set_callbacks(NULL, NULL) does not make the stub unreachable, it
     * only makes the native side stop dialling it. That is why the Java stub belongs in
     * Arena.global(): no arena with a close path would be correct.
     */
    int32_t (*preferences_changed)(void* user, int32_t types);
} GwinPrefsCallbacks;

/* sizeof checks for the two structs above, so a MemoryLayout drift fails a test instead of a user. */
GLASS_WIN_EXPORT int32_t gwin_sizeof_ui_settings(void);
GLASS_WIN_EXPORT int32_t gwin_sizeof_network_info(void);

/*
 * Installs (or, with cb == NULL or a NULL slot, clears) the preferences callback table. The table is
 * COPIED by value: this library never retains `cb`, so Java may build it in a confined arena and
 * close it, but the upcall stub it holds must outlive the process (see GwinPrefsCallbacks LIFETIME).
 * `user` is passed back unchanged and is never dereferenced here.
 *
 * THE TABLE IS THE ONLY NOTIFICATION PATH. While no table is installed,
 * PlatformSupport::updatePreferences delivers nothing and answers false; its JNI arm, which built the
 * java.util.Map in C and called Application.notifyPreferencesChanged, is gone.
 *
 * No lock, by design: install once from WinApplication's static initializer, which runs on the
 * launcher thread before runLoop starts the Glass toolkit thread, so "installed before the first
 * WndProc message" and "never written from a second thread" both hold by construction. Lazy
 * installation would drop every notification that arrives during startup.
 *
 * Always returns GWIN_OK.
 */
GLASS_WIN_EXPORT int32_t gwin_prefs_set_callbacks(const GwinPrefsCallbacks* cb, void* user);

/*
 * Fills *out with the IUISettings half of the preferences (PlatformSupport::collectUISettings, the former
 * queryUISettings) resp. the INetworkInformationStatics half (collectNetworkInfo, the former
 * queryNetworkInformation). Both zero-fill *out first, so an
 * `available` of 0 always comes with a fully zeroed struct.
 *
 * out == NULL -> GWIN_ERR_INVALID_ARG, nothing written. Otherwise GWIN_OK; every failure the C
 * tolerated is reported through the struct's flags, not the return value, because the C tolerated it
 * by emitting fewer map keys and this ABI has to be able to say the same thing.
 *
 * available == 0 also covers "the Glass toolkit is not running": these read the WinRT objects the
 * live GlassApplication's PlatformSupport activated, and before Application.run() (or after the
 * toolkit window is destroyed) there are none. That means no WinRT keys, not no keys: the method
 * itself is Java now - WinApplication.getPlatformPreferences is WinPreferences.collect, which calls
 * these two and still returns the user32 keys it reads itself, where the former JNI
 * getPlatformPreferences (GlassApplication::GetPlatformPreferences) answered NULL without a toolkit.
 *
 * THREAD: call on the Glass toolkit thread - that is the apartment the WinRT objects were activated
 * in, and the thread the JNI collected on. Blocks: yes, these are cross-apartment COM calls;
 * critical(true) is FORBIDDEN on both.
 */
GLASS_WIN_EXPORT int32_t gwin_prefs_query_ui_settings(GwinUiSettings* out);
GLASS_WIN_EXPORT int32_t gwin_prefs_query_network(GwinNetworkInfo* out);

/*
 * ---- WinApplication: the loop / invoke family (ABI 3) ----
 *
 * The Win32 message pump of the Glass toolkit thread, the nested-loop pump, and the two WM_DO_ACTION
 * dispatch paths. All of it was GlassApplication.cpp; the statements are moved, not rewritten.
 *
 * critical(true) is FORBIDDEN on every function of this section - see the file comment.
 */

typedef struct GwinAppCallbacks {
    /*
     * The three former Runnable.run upcalls of GlassApplication.cpp (the launchable of _runLoop, the
     * action of _invokeAndWait and the action of _submitForLaterInvocation). All three called the one
     * cached jmethodID of Runnable.run, so ONE slot replaces all three. The runnable is identified by a
     * Java-assigned int64 id: this library never holds a Java reference, so there is nothing to add
     * or delete when one is scheduled, and nothing to clean up if one is never dispatched.
     *
     * user is the value handed to gwin_app_set_callbacks, passed back unchanged, never dereferenced.
     * A NULL slot is tolerated: the runnable is simply not run, which is what a NULL jmethodID did.
     *
     * THREAD: run_runnable is ALWAYS called on the Glass toolkit thread - the thread that called
     * gwin_run_loop, i.e. "WindowsNativeRunloopThread" - in three situations: (a) inside
     * gwin_run_loop, before the pump and INSIDE the OLE STA, for the launchable; (b) inside
     * DispatchMessage -> BaseWnd::StaticWindowProc -> GlassApplication::WindowProc's WM_DO_ACTION
     * arm, while another thread blocks in SendMessage; (c) the same for WM_DO_ACTION_LATER. In the
     * SWT-embedded case (javafx.embed.isEventThread) there is no gwin_run_loop and (a) does not
     * occur at all.
     *
     * RE-ENTRANT: a runnable may call gwin_invoke_and_wait, gwin_invoke_later or
     * gwin_enter_nested_event_loop, and a nested pump dispatches further WM_DO_ACTION messages on top
     * of it. Nothing in the stub may hold a lock or a ThreadLocal.
     *
     * RETURNS NOTHING AND CANNOT FAIL: the JNI swallowed every exception the runnable threw
     * (CheckAndClearException -> Application.reportException) and so must the stub. None of the three
     * sites consumed a return value, and the WM_DO_ACTION / WM_DO_ACTION_LATER arm answers `return 0`
     * rather than DefWindowProc whatever the runnable did. This slot is deliberately NOT int32_t
     * "for symmetry" with preferences_changed, whose return really is load-bearing.
     */
    void (*run_runnable)(void* user, int64_t runnable_id);
} GwinAppCallbacks;

#if defined(__cplusplus)
static_assert(sizeof(GwinAppCallbacks) == 1 * sizeof(void*), "GwinAppCallbacks must be 1 pointer");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinAppCallbacks) == 1 * sizeof(void*), "GwinAppCallbacks must be 1 pointer");
#else
typedef char gwin_app_callbacks_size_check[(sizeof(GwinAppCallbacks) == 1 * sizeof(void*)) ? 1 : -1];
#endif

/*
 * sizeof probe, beside gwin_sizeof_ui_settings / gwin_sizeof_network_info. It RETURNS
 * sizeof(GwinAppCallbacks) and only DOCUMENTS today's value: the point of the probe is to fail a
 * test the day a second slot is appended.
 */
GLASS_WIN_EXPORT int32_t gwin_sizeof_app_callbacks(void);   /* == sizeof(void*) today */

/*
 * Copies *cb by value and stores user verbatim; cb == NULL clears the table. Idempotent, last call
 * wins, no lock - Java installs it once from WinApplication's static initializer, on the launcher
 * thread, before Application.run() creates the toolkit window, exactly as with the preferences
 * table. Always returns GWIN_OK.
 */
GLASS_WIN_EXPORT int32_t gwin_app_set_callbacks(const GwinAppCallbacks* cb, void* user);

/*
 * WinApplication._init's former JNI body without its DPI-awareness step, which Java performs itself
 * (shcore!SetProcessDpiAwareness, else user32!SetProcessDPIAware) immediately before this call:
 * new GlassApplication() - RegisterClassExW + CreateWindowExW of the hidden toolkit window, whose
 * WM_CREATE publishes the instance every other export reads - including PlatformSupport's WinRT
 * activation (RoInitialize(RO_INIT_SINGLETHREADED), UISettings, NetworkInformation and their two event
 * sinks). If no toolkit window exists afterwards the object is deleted, as the JNI did. Returns the
 * toolkit HWND or NULL; Java DISCARDS it, exactly as it discarded _init's jlong, and must never cache it
 * for DestroyWindow (gwin_terminate_loop reads the live instance - HWND values are recycled).
 * THREAD: the thread that will call gwin_run_loop (SWT-embedded: the embedder's UI thread), before it -
 * RoInitialize's apartment is the one gwin_run_loop's OleInitialize then joins. A second call leaks the
 * first instance, as the JNI did. No upcall on the calling thread; the two WinRT sinks may call
 * GwinPrefsCallbacks.preferences_changed from a WinRT thread as soon as they are registered (see
 * GwinPrefsCallbacks THREAD). NULL when a C++ exception was caught. critical(true) FORBIDDEN.
 */
GLASS_WIN_EXPORT void* gwin_app_create(void);

/*
 * WinApplication._runLoop's former JNI body, step for step:
 *   1. OLEHolder: OleInitialize(NULL) on THIS thread, undone by OleUninitialize when this returns
 *      (OleUtils.h). The apartment exists for exactly the span of this call.
 *   2. launchable_id != 0 -> run_runnable(user, launchable_id) INSIDE that apartment. Java must NOT
 *      run the launchable itself before the downcall: the JNI ran it after OleInitialize and before
 *      the pump, and Application.run passes Screen.initScreens() + FX startup as it.
 *   3. while (GlassApplication::GetInstance() && GetMessage(&msg, NULL, 0, 0) > 0)
 *          { TranslateMessage; DispatchMessage; }
 *      - the instance may be destroyed by a nested loop, and WM_QUIT is deliberately LEFT on the
 *      queue. The GetInstance() term is load-bearing; the queue is not drained.
 *   4. the Windows 7 + Narrator workaround: if GetAccessibilityCount() > 0 && !IS_WIN8, one more
 *      SetTimer(NULL, NULL, 1000, NULL) / GetMessage / KillTimer round. Unreachable on Windows 10+;
 *      carried over anyway.
 *
 * launchable_id == 0 means "no launchable", which is what a NULL jobject meant.
 *
 * THREAD: the calling thread BECOMES the Glass toolkit thread for the whole call. BLOCKS for the
 * life of the application. NEVER critical.
 */
GLASS_WIN_EXPORT void gwin_run_loop(int64_t launchable_id);

/*
 * WinApplication._terminateLoop's former JNI body: hWnd = GetToolkitHWND();
 * if (IsWindow(hWnd)) DestroyWindow(hWnd). NULL when there is no instance, and IsWindow(NULL) is
 * FALSE, so a second call does nothing. DestroyWindow only works from the thread that created the
 * window - the toolkit thread - which is where Application.terminate -> finishTerminating runs.
 * Note what happens INSIDE it: WM_DESTROY -> RegisterClipboardViewerId(0) ->
 * GwinClipboardCallbacks.dispose_peer for the registered clipboard peer (an upcall from inside the
 * FFM downcall, on an attached thread); then WM_NCDESTROY clears pInstance
 * and the pump in gwin_run_loop returns. NEVER critical.
 */
GLASS_WIN_EXPORT void gwin_terminate_loop(void);

/*
 * GlassApplication::EnterNestedEventLoop, now JVM-free: sm_shouldLeaveNestedLoop = false;
 * pump while (GetInstance() && !sm_shouldLeaveNestedLoop && GetMessage(...) > 0);
 * sm_shouldLeaveNestedLoop = false again on the way out. The RETURN VALUE is not part of this ABI:
 * it never left Java in the first place and WinApplication holds it now. Toolkit thread only.
 * BLOCKS and dispatches, so it upcalls re-entrantly. NEVER critical.
 */
GLASS_WIN_EXPORT void gwin_enter_nested_event_loop(void);

/*
 * GlassApplication::LeaveNestedEventLoop, now JVM-free: sm_shouldLeaveNestedLoop = true. Java
 * stores its return value BEFORE calling this, in the order the C used (Attach, then flag). Toolkit
 * thread only, which is why the flag is a plain bool with no barrier. NEVER critical.
 */
GLASS_WIN_EXPORT void gwin_leave_nested_event_loop(void);

/*
 * WinApplication._invokeAndWait's former JNI body: wraps runnable_id in an Action and hands
 * it to GlassApplication::ExecAction = SendMessage(toolkit HWND, WM_DO_ACTION, action, 0). BLOCKS
 * until run_runnable has returned on the toolkit thread; if the caller IS the toolkit thread,
 * SendMessage runs the WindowProc inline on this stack - Application.invokeAndWait short-circuits
 * that case in Java first. GWIN_OK when dispatched, GWIN_ERR_NO_TOOLKIT when there is no toolkit
 * window, in which case the runnable is NOT run and no callback arrives, exactly as ExecAction's
 * early return did silently. Java must IGNORE GWIN_ERR_NO_TOOLKIT: the JNI threw nothing and blocked
 * nobody. NEVER critical.
 */
GLASS_WIN_EXPORT int32_t gwin_invoke_and_wait(int64_t runnable_id);

/*
 * WinApplication._submitForLaterInvocation's former JNI body:
 * PostMessage(toolkit HWND, WM_DO_ACTION_LATER, new Action(runnable_id), 0); the WindowProc runs it
 * and then deletes the action. Returns immediately.
 *
 * CALLABLE FROM ANY THREAD - Glass's InvokeLaterDispatcher thread is the usual one.
 *
 * GWIN_OK: posted. GWIN_ERR_NO_TOOLKIT: there is no toolkit window, or PostMessage failed; in both
 * cases the action is destroyed and no callback will arrive, so Java may drop the id. Two deliberate
 * deviations from ExecActionLater, both unobservable because the runnable does not run either way:
 * it ignored PostMessage's result and leaked the action, and it read pInstance twice - the HWND is
 * read ONCE here, so a teardown racing a call from another thread cannot degrade into
 * PostMessage(NULL, ...), which posts a thread message to the CALLER and would report GWIN_OK for a
 * runnable that never runs. An action posted but never dispatched - messages left on the queue at
 * shutdown - leaks its id exactly as the JNI leaked its JGlobalRef. NEVER critical.
 */
GLASS_WIN_EXPORT int32_t gwin_invoke_later(int64_t runnable_id);

/*
 * ---- WinMenuImpl: the one WM_COMMAND upcall (ABI 3) ----
 *
 * Menus themselves need no downcall wrapper: WinMenuImpl binds user32!CreateMenu, InsertMenuItemW,
 * SetMenuItemInfoW, RemoveMenu, DestroyMenu and friends directly. The only thing that could not
 * cross was the upcall GlassMenu.cpp's HandleMenuCommand makes for every WM_COMMAND.
 *
 * critical(true) is FORBIDDEN on gwin_menu_set_callbacks - see the file comment.
 */

typedef struct GwinMenuCallbacks {
    /*
     * A menu command was chosen. cmd_id is LOWORD(wParam) of WM_COMMAND, i.e. the 16-bit
     * MENUITEMINFOW.wID Java put there (WinMenuItemDelegate.CommandIDManager, 1..0xFFFF), so it is
     * always in 0..0xFFFF and never negative.
     *
     * RETURNS non-zero if Java handled it: GlassWindow::WindowProc then answers `return 0` and does
     * NOT call DefWindowProc. Zero means not handled, and zero is also what the target must answer
     * when it fails - no exception may cross this boundary in either direction. The ABI promises
     * "non-zero means handled"; do not write `== 1` anywhere, and do not let a later change introduce
     * a second convention.
     *
     * THREAD: the Glass toolkit thread, always - inside DispatchMessage -> BaseWnd::StaticWindowProc
     * -> GlassWindow::WindowProc -> GlassWindow::HandleCommand -> HandleMenuCommand. Never a foreign
     * thread: the only AttachCurrentThread in this library was in Timer.cpp, which is gone.
     * Frequently nested inside the system's modal menu loop.
     *
     * RE-ENTRANT: the target runs arbitrary Java (MenuItem.Callback.action) which may show a dialog
     * or nest another message loop. This library holds no lock across the call - the JNI did not
     * either.
     *
     * LIFETIME: there is no point at which this library can promise no further WM_COMMAND, so
     * gwin_menu_set_callbacks(NULL, NULL) does not make the stub unreachable - it only stops this
     * library dialling it. The Java stub therefore belongs in Arena.global(), exactly as for
     * GwinPrefsCallbacks.
     *
     * WHAT IS DROPPED: the JNI passed the owning com.sun.glass.ui.Window as the first argument,
     * computed natively with the former GlassWindow::GetJObject(). WinMenuImpl.notifyCommand
     * never used it - the registry is CommandIDManager, keyed by the cmd_id this callback already
     * carries - so no window handle and no pointer to a Java object crosses here.
     *
     * user is whatever gwin_menu_set_callbacks was given, passed back unchanged and never
     * dereferenced by this library. It is NULL today.
     */
    int32_t (*notify_command)(void* user, int32_t cmd_id);
} GwinMenuCallbacks;

#if defined(__cplusplus)
static_assert(sizeof(GwinMenuCallbacks) == 1 * sizeof(void*), "GwinMenuCallbacks must be 1 pointer");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinMenuCallbacks) == 1 * sizeof(void*), "GwinMenuCallbacks must be 1 pointer");
#else
typedef char gwin_menu_callbacks_size_check[(sizeof(GwinMenuCallbacks) == 1 * sizeof(void*)) ? 1 : -1];
#endif

/*
 * sizeof probe, beside gwin_sizeof_ui_settings / gwin_sizeof_network_info: it exists to fail a test
 * the day a second slot is appended.
 */
GLASS_WIN_EXPORT int32_t gwin_sizeof_menu_callbacks(void);   /* == sizeof(void*) today */

/*
 * Copies *cb by value and stores user verbatim; cb == NULL clears the whole table. Idempotent, last
 * call wins, no lock - Java installs it once, before any window can receive WM_COMMAND. Always
 * returns GWIN_OK.
 *
 * THIS TABLE IS THE ONLY NOTIFICATION PATH: the JNI one (WinMenuImpl.notifyCommand through the
 * method id _initIDs cached) went with WinMenuImpl's last native. While no table is installed,
 * every WM_COMMAND is silently "not handled" and goes to DefWindowProc - exactly what the JNI path
 * answered before _initIDs had run, so the pre-existing behaviour is reproduced.
 */
GLASS_WIN_EXPORT int32_t gwin_menu_set_callbacks(const GwinMenuCallbacks* cb, void* user);

/*
 * ---- WinView + WinGestureSupport: the view peer and its 26 upcall sites (ABI 4) ----
 *
 * The view peer is GlassView (GlassView.cpp), a lightweight object that knows its host HWND and
 * its FullScreenWindow; the message handling that feeds it is ViewContainer.cpp, the WndProc body
 * shared by GlassWindow and FullScreenWindow. Eleven of WinView's JNI entry points were OS-CALL or
 * native-state and become the gwin_view_* exports below; three (_getMultiClickTime_impl and the two
 * _getMultiClickMax*_impl) were WRAPPERs over user32!GetDoubleClickTime / GetSystemMetrics(36 / 37)
 * that Java binds directly; two (_begin / _end) had empty bodies and become empty Java overrides;
 * _initIDs lost its last reader with the WinWindow and clipboard flips, was emptied and then
 * deleted with its Java half; and the last, _create, whose whole body had become
 * `new GlassView(viewId)` (the view holds no jobject), is gwin_view_create (below, ABI 5) - its JNI
 * body is gone, so WinView declares no native.
 *
 * The 26 direct upcall sites (GlassView.cpp 3, ViewContainer.cpp 22, FullScreenWindow.cpp 1) dial
 * the two tables below, and they are the only path: the JNI arms are gone, so with no
 * table installed a site delivers nothing. Java installs both from WinApplication's static initializer,
 * before any view exists (see the installers).
 *
 * IDENTITY. Every slot's first parameter is an int64_t view_id. It is the id Java assigns to the
 * WinView and hands to the C when the GlassView is created; the library never dereferences it and
 * never holds a Java reference - the same shape as GwinAppCallbacks.run_runnable's runnable_id.
 * DELIBERATELY UNLIKE gwin_prefs_set_callbacks / gwin_app_set_callbacks / gwin_menu_set_callbacks,
 * neither installer here takes a void* user and no slot carries one: the identity is per VIEW, not
 * per table, so a table-level context would always be NULL (both existing installers are in fact
 * passed NULL today), and an ADDRESS parameter on an upcall stub materialises a MemorySegment per
 * call, which the hot slots below cannot afford. A parameter that is always NULL is worse than no
 * parameter. view_id == 0 means the container had no GlassView attached (only the four touch /
 * gesture slots can see it - HandleViewTouchEvent has no GetGlassView() guard - and the JNI passed
 * a null View there); a non-zero id Java no longer knows is a stale peer: do nothing, return the
 * slot default, and do NOT log it - the animated-fullscreen path (gwin_view_exit_fullscreen below)
 * reaches it through a pre-existing defect the JNI crashed on.
 *
 * THREAD. All 26 sites run on the JavaFX application thread, which is the Glass toolkit thread:
 * inside DispatchMessage -> BaseWnd::StaticWindowProc -> the virtual WindowProc, or inside a
 * gwin_view_* downcall that ExecAction has marshalled there with SendMessage(WM_DO_ACTION). No
 * foreign thread exists in this section: the inertia timer is a user32!SetTimer WM_TIMER on the same
 * thread and the IManipulationEvents COM sink is called synchronously from HandleViewTouchEvent.
 * RE-ENTRANT: a slot may close the window before it returns (notify_mouse(ENTER) is followed by an
 * IsWindow / BaseWnd::FromHandle re-check that stays in C); three downcalls (set_parent,
 * enter_fullscreen, exit_fullscreen) upcall from INSIDE the downcall; and with animate != 0
 * exit_fullscreen returns BEFORE its REMOVE / ADD / notify_resize arrive from a later WM_TIMER. No
 * Java target may take a lock a downcall on the same thread already holds.
 *
 * EXCEPTIONS. No slot may let one escape: the Java stub catches Throwable and routes it to
 * Application.reportException, which is where CheckAndClearException sent it. In the other
 * direction every gwin_view_* body below is wrapped try { } catch (...) and answers its failure
 * value: this header's "no exception crosses this boundary" was not kept by construction before -
 * GlassInputTextInfo::GetClauseInfo / GetAttributeInfo rethrow std::bad_alloc and nothing above
 * WmImeComposition catches - so the wrapper is strictly an improvement on undefined behaviour and
 * the underlying rethrow is recorded as a follow-up, not fixed here.
 *
 * NULL SLOTS. The installers copy the struct by value and replace every NULL slot with an internal
 * no-op that returns 0, so the 26 sites never test a slot. A NULL slot therefore behaves exactly as
 * a cleared JNI exception did: nothing happens and the C reads 0.
 *
 * critical(true) is FORBIDDEN on every gwin_view_* function that marshals (they block in
 * SendMessage; three also upcall) and pointless on the three that do not (get_native_view,
 * enable_ime, finish_ime_composition pass no Java array).
 */

/* An opaque GlassView*: the value gwin_view_create returns. */
typedef void* gwin_view_t;

/*
 * ---- GwinViewCallbacks: the former javaIDs.View jmethodIDs (GlassView.cpp _initIDs) ----
 *
 * Every slot: JavaFX application thread, synchronous, possibly re-entrant, possibly nested inside a
 * gwin_view_* downcall (see the section comment). Boolean arguments are int32_t 0 / 1.
 */
typedef struct GwinViewCallbacks {
    /*
     * View.notifyView(int) - a com.sun.glass.events.ViewEvent constant: FULLSCREEN_ENTER /
     * FULLSCREEN_EXIT from GlassView::NotifyFullscreen, REMOVE then ADD from GlassView::SetHostHwnd,
     * MOVE from ViewContainer::NotifyViewMoved. 4 sites; per frame during a move.
     */
    void (*notify_view)(int64_t view_id, int32_t type);

    /* View.notifyResize(int,int) - client width / height. 2 sites (one in FullScreenWindow.cpp). */
    void (*notify_resize)(int64_t view_id, int32_t width, int32_t height);

    /* View.notifyRepaint(int,int,int,int) - the GetUpdateRect dirty rect. 1 site; per repaint. */
    void (*notify_repaint)(int64_t view_id, int32_t x, int32_t y, int32_t width, int32_t height);

    /*
     * View.notifyMenu(int,int,int,int,boolean) - client x / y (RTL-unmirrored), screen x / y,
     * keyboard trigger. 1 site. WinView overrides notifyMenu (the EXTENDED-window system menu), so
     * the Java target must dispatch virtually through a WinView reference.
     */
    void (*notify_menu)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                        int32_t is_keyboard_trigger);

    /*
     * View.notifyKey(int,int,char[],int). key_chars points at key_char_count UTF-16 code units and
     * is borrowed for the duration of the call; key_char_count may be 0 and then key_chars may be
     * NULL - Java must still build a zero-length char[], never null (the JNI passed an allocated
     * empty jcharArray). type is KeyEvent.PRESS / RELEASE / TYPED; the synthesised PrintScreen
     * PRESS and the synthesised Delete TYPED (U+007F) arrive through here too. 3 sites; per
     * keystroke, not per frame.
     */
    void (*notify_key)(int64_t view_id, int32_t type, int32_t key_code,
                       const uint16_t* key_chars, int32_t key_char_count, int32_t modifiers);

    /*
     * View.notifyMouse(int,int,int,int,int,int,int,boolean,boolean). HOT: one per WM_MOUSEMOVE that
     * moved (ViewContainer drops repeats), i.e. up to the mouse report rate. Scalars only; the stub
     * must allocate nothing. 5 sites, including the non-client ENTER / event pair and the EXIT from
     * ResetMouseTracking, which passes button 0 (NOT MouseEvent.BUTTON_NONE) and GetCursorPos
     * coordinates, exactly as the JNI did.
     */
    void (*notify_mouse)(int64_t view_id, int32_t type, int32_t button,
                         int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                         int32_t modifiers, int32_t is_popup_trigger, int32_t is_synthesized);

    /*
     * View.notifyScroll(int,int,int,int,double,double,int,int,int,int,int,double,double). lines and
     * chars are SPI_GETWHEELSCROLLLINES / SPI_GETWHEELSCROLLCHARS; default_lines, default_chars,
     * x_multiplier, y_multiplier are the literals 3, 3, 40.0, 40.0. 1 site; per wheel notch.
     */
    void (*notify_scroll)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                          double delta_x, double delta_y, int32_t modifiers,
                          int32_t lines, int32_t chars, int32_t default_lines, int32_t default_chars,
                          double x_multiplier, double y_multiplier);

    /*
     * View.notifyInputMethod(String,int[],int[],byte[],int,int,int). All four buffers are borrowed
     * for the call. text is UTF-16 and NOT NUL-terminated; text == NULL with text_len 0 is the
     * WM_IME_ENDCOMPOSITION reset and also what a failed result / composition join produces
     * (ConcatJStrings returned NULL then). NULL-NESS IS CARRIED BY THE POINTER, NOT BY THE COUNT:
     * clause_boundary is non-NULL only when the JNI would have built the jintArray, and
     * attr_boundary / attr_value are both NULL or both non-NULL; Java must test the pointers and
     * ignore the counts of NULL buffers. clause_boundary holds clause_count + 1 int32,
     * attr_boundary attr_count + 1 int32, attr_value attr_count bytes. The JNI called this void
     * method through CallBooleanMethod and discarded the result; the slot is void. 1 site.
     */
    void (*notify_input_method)(int64_t view_id,
                                const uint16_t* text, int32_t text_len,
                                const int32_t* clause_boundary, int32_t clause_count,
                                const int32_t* attr_boundary, const uint8_t* attr_value,
                                int32_t attr_count,
                                int32_t committed_text_length, int32_t caret_pos,
                                int32_t visible_pos);

    /*
     * QUERY. View.notifyInputMethodCandidatePosRequest(int) -> double[2]. Writes out_xy[0] = x and
     * out_xy[1] = y and returns 1; returns 0 and leaves out_xy untouched when the Java side failed.
     * GetCandidatePos truncates both with a C cast to int. Write both elements or neither; offset is
     * always 0 today. 1 site, once per IMN_OPENCANDIDATE / IMN_CHANGECANDIDATE.
     */
    int32_t (*notify_ime_candidate_pos_request)(int64_t view_id, int32_t offset, double* out_xy);

    /*
     * QUERY. View.getAccessible() -> long: the IRawElementProviderSimple* the Java Accessible owns,
     * or 0. THE RETURN IS LOAD-BEARING: for UiaRootObjectId a non-zero value goes straight to
     * UiaReturnRawElementProvider and its LRESULT becomes the WM_GETOBJECT reply (GlassWindow's
     * `if (lr) return lr;`); 0 falls through to DefWindowProc, and a wrong 0 silently disables
     * accessibility for the window. The OBJID_CLIENT site discards the value and wants only the
     * side effect. 2 sites, cold.
     */
    int64_t (*get_accessible)(int64_t view_id);
} GwinViewCallbacks;   /* 10 pointers */

#if defined(__cplusplus)
static_assert(sizeof(GwinViewCallbacks) == 10 * sizeof(void*), "GwinViewCallbacks must be 10 pointers");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinViewCallbacks) == 10 * sizeof(void*), "GwinViewCallbacks must be 10 pointers");
#else
typedef char gwin_view_callbacks_size_check[(sizeof(GwinViewCallbacks) == 10 * sizeof(void*)) ? 1 : -1];
#endif

/*
 * ---- GwinGestureCallbacks: the former javaIDs.Gestures static jmethodIDs (ViewContainer.cpp) ----
 *
 * The Java targets are static methods of WinGestureSupport whose first parameter is a View; Java
 * resolves view_id to that View, or to null when view_id is 0. Same thread, same NULL-slot and
 * no-escaping-exception rules as GwinViewCallbacks. The JNI reached these through a jclass global
 * ref taken by GlassApplication::ClassForName with initialize = true at GlassWindow::Create time,
 * so WinGestureSupport.<clinit> ran eagerly; the Java installer must initialise the class itself,
 * or the first touch event initialises it inside a WndProc frame.
 */
typedef struct GwinGestureCallbacks {
    /*
     * WinGestureSupport.gesturePerformed(View,int,boolean,boolean,int,int,int,int,
     *                                    float,float,float,float,float,float,float)
     * = SEVEN int32 then SEVEN float. Count them against ViewContainer::NotifyGesturePerformed before
     * writing the FunctionDescriptor: one integer too many silently shifts every float by an 8-byte
     * stack slot on x64 and a pinch delivers the bit pattern of y_abs as delta_x - no crash, no
     * exception. gwin_test_fire_callback(100, ...) exists for exactly that mistake. The deltas are
     * already divided by 100 (except total_scale and total_rotation), as the JNI divided them.
     * HOT: one per IManipulationProcessor delta while a gesture runs.
     */
    void (*gesture_performed)(int64_t view_id, int32_t modifiers, int32_t is_direct,
                              int32_t is_inertia,
                              int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                              float delta_x, float delta_y,
                              float total_delta_x, float total_delta_y,
                              float total_scale, float total_expansion, float total_rotation);

    /* WinGestureSupport.inertiaGestureFinished(View), from the IDT_GLASS_INERTIAPROCESSOR WM_TIMER. */
    void (*inertia_gesture_finished)(int64_t view_id);

    /* WinGestureSupport.notifyBeginTouchEvent(View,int,boolean,int). is_direct is taken from the
     * FIRST point of the batch (touch screen vs. pen), touch_event_count is the batch size. */
    void (*notify_begin_touch_event)(int64_t view_id, int32_t modifiers, int32_t is_direct,
                                     int32_t touch_event_count);

    /*
     * WinGestureSupport.notifyNextTouchEvent(View,int,long,int,int,int,int). state is a
     * TouchEvent.TOUCH_* constant, id the TOUCHINPUT.dwID widened to int64, x / y client
     * (RTL-unmirrored) and x_abs / y_abs screen, both LONG(ti->x / 100) as the JNI truncated them.
     * HOT: once per touch point per WM_TOUCH - up to ten points at the digitiser rate.
     */
    void (*notify_next_touch_event)(int64_t view_id, int32_t state, int64_t id,
                                    int32_t x, int32_t y, int32_t x_abs, int32_t y_abs);

    /* WinGestureSupport.notifyEndTouchEvent(View). */
    void (*notify_end_touch_event)(int64_t view_id);
} GwinGestureCallbacks;   /* 5 pointers */

#if defined(__cplusplus)
static_assert(sizeof(GwinGestureCallbacks) == 5 * sizeof(void*), "GwinGestureCallbacks must be 5 pointers");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinGestureCallbacks) == 5 * sizeof(void*), "GwinGestureCallbacks must be 5 pointers");
#else
typedef char gwin_gesture_callbacks_size_check[(sizeof(GwinGestureCallbacks) == 5 * sizeof(void*)) ? 1 : -1];
#endif

/*
 * sizeof probes, beside gwin_sizeof_ui_settings / gwin_sizeof_network_info. They RETURN the real
 * sizeof and only DOCUMENT today's value, so appending a slot fails a test.
 */
GLASS_WIN_EXPORT int32_t gwin_sizeof_view_callbacks(void);      /* == 10 * sizeof(void*) today */
GLASS_WIN_EXPORT int32_t gwin_sizeof_gesture_callbacks(void);   /* ==  5 * sizeof(void*) today */

/*
 * Install (cb == NULL clears) the view resp. gesture callback table. Copied by value, NULL slots
 * replaced by no-ops (see the section comment), so the caller may free the struct; the stubs in it
 * must outlive the process, because there is no point at which this library can promise no further
 * WndProc message - Arena.global(), as for the other three tables. Last call wins, no lock: Java
 * installs both once from WinApplication's static initializer, on the launcher thread, before
 * Application.run() creates the toolkit window. NO void* user - see IDENTITY above. Always GWIN_OK.
 */
GLASS_WIN_EXPORT int32_t gwin_view_set_callbacks(const GwinViewCallbacks* cb);
GLASS_WIN_EXPORT int32_t gwin_gesture_set_callbacks(const GwinGestureCallbacks* cb);

/*
 * TEST HOOK, unconditionally exported, never called by production code. Fires slot `slot` of the
 * installed table with a fixed argument pattern and returns what the slot returned - 0 for the void
 * slots and for a slot that was installed as NULL, GWIN_ERR_INVALID_ARG for an unknown slot or when
 * the table that owns it is not installed. It exists because the sizeof probes cannot catch a
 * FunctionDescriptor whose parameter list disagrees with the prototype (a table stores only
 * pointers) and because neither touch hardware nor an IME exists in CI; it is the only automated
 * check of the fifteen descriptors. Returns int64_t so slot 9's value round-trips untruncated.
 *
 * Slot numbers and the exact pattern delivered (view_id is passed through unchanged):
 *     0 notify_view(view_id, 1001)
 *     1 notify_resize(view_id, 1001, 1002)
 *     2 notify_repaint(view_id, 1001, 1002, 1003, 1004)
 *     3 notify_menu(view_id, 1001, 1002, 1003, 1004, 1)
 *     4 notify_key(view_id, 1001, 1002, {0x0041, 0x0042}, 2, 1003)
 *     5 notify_mouse(view_id, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1, 0)
 *     6 notify_scroll(view_id, 1001, 1002, 1003, 1004, 1.5, 2.5, 1005, 1006, 1007, 1008, 1009,
 *                     3.5, 4.5)
 *     7 notify_input_method(view_id, {0x0041, 0x0042}, 2, {0, 2}, 1, {0, 2}, {1}, 1, 1001, 1002,
 *                           1003)
 *     8 notify_ime_candidate_pos_request(view_id, 1001, out) - out is a local double[2] that is
 *       discarded; the slot's int32 return is what comes back
 *     9 get_accessible(view_id) - its int64 return comes back
 *   100 gesture_performed(view_id, 1001, 1, 0, 1002, 1003, 1004, 1005, 1.5f, 2.5f, 3.5f, 4.5f, 5.5f,
 *                         6.5f, 7.5f)
 *   101 inertia_gesture_finished(view_id)
 *   102 notify_begin_touch_event(view_id, 1001, 1, 1002)
 *   103 notify_next_touch_event(view_id, 1001, 0x100000002 (int64), 1002, 1003, 1004, 1005)
 *   104 notify_end_touch_event(view_id)
 */
GLASS_WIN_EXPORT int64_t gwin_test_fire_callback(int32_t slot, int64_t view_id);

/*
 * new GlassView(view_id): the body of the former Java_com_sun_glass_ui_win_WinView__1create (deleted
 * under ABI 5). view_id is what every
 * GwinViewCallbacks / GwinGestureCallbacks / GwinDndCallbacks slot delivers (IDENTITY). No OS call and
 * no thread marshal, so it works without a toolkit. Released by gwin_view_close. NULL only when the
 * allocation threw. The object's m_InputMethodEventsEnabled is left uninitialised, as it always was.
 */
GLASS_WIN_EXPORT gwin_view_t gwin_view_create(int64_t view_id);

/*
 * GlassView::Close + delete, marshalled to the toolkit thread by SendMessage(WM_DO_ACTION) exactly
 * as ENTER_MAIN_THREAD_AND_RETURN did: closes the FullScreenWindow if one is attached (which can
 * upcall on the way out), then deletes the GlassView. `view` is DANGLING on return and the
 * allocator will hand the same address to the next view - Java must null its handle and never
 * call a gwin_view_* function with it again. Returns 1 (GlassView::Close has no failure path); 0
 * only when a C++ exception was caught. BLOCKS and UPCALLS.
 */
GLASS_WIN_EXPORT int32_t gwin_view_close(gwin_view_t view);

/*
 * GlassView::GetHostHwnd: the HWND the view is embedded in (a GlassWindow, a FullScreenWindow, or
 * whatever else hosts it), NULL when detached. Native state, so it stays native: SetHostHwnd is
 * driven by C (the fullscreen attach / detach), not only by gwin_view_set_parent. No thread marshal,
 * as in the JNI; NULL when a C++ exception was caught.
 */
GLASS_WIN_EXPORT void* gwin_view_get_native_view(gwin_view_t view);

/*
 * GlassView::SetHostHwnd on the toolkit thread. Returns without doing anything when parent_hwnd is
 * the current host; otherwise clears the host and fires notify_view(REMOVE) if there was one, then
 * sets the new host and fires notify_view(ADD) if it is non-NULL - in that order, from inside the
 * downcall. BLOCKS and UPCALLS.
 */
GLASS_WIN_EXPORT void gwin_view_set_parent(gwin_view_t view, void* parent_hwnd);

/*
 * Host-window insets on the toolkit thread: GetWindowRect(host), GetClientRect(host),
 * MapWindowPoints(host -> screen) on the client rect, then client.left - window.left (resp.
 * client.top - window.top). 0 when the view has no host HWND, and 0 when a C++ exception was
 * caught. BLOCKS.
 */
GLASS_WIN_EXPORT int32_t gwin_view_get_x(gwin_view_t view);
GLASS_WIN_EXPORT int32_t gwin_view_get_y(gwin_view_t view);

/* InvalidateRect(host, NULL, FALSE) on the toolkit thread when there is a host HWND. BLOCKS. */
GLASS_WIN_EXPORT void gwin_view_schedule_repaint(gwin_view_t view);

/*
 * The former _uploadPixels with the Pixels object resolved by Java. `bits` is width * height
 * 32-bit BGRA-premultiplied pixels, top-down, no row padding - the block Pixels.getBits() / the
 * direct buffer's base address held - borrowed for the call and never retained. bits == NULL is
 * legal and is handed to SetDIBitsToDevice / the DIB section copy unchanged, which is what a failed
 * Pixels attach did. Behaviour kept verbatim, on the toolkit thread: silent return when the host
 * HWND is not a window; a non-transparent host (or a non-GlassWindow host such as the
 * FullScreenWindow) goes through SetDIBitsToDevice with a negative biHeight; a transparent
 * GlassWindow goes through UpdateLayeredWindow(ULW_ALPHA) with the window's alpha, and returns
 * silently when the window rect and (width, height) disagree. BLOCKS.
 */
GLASS_WIN_EXPORT void gwin_view_upload_pixels(gwin_view_t view, int32_t width, int32_t height,
                                              const void* bits);

/*
 * GlassView::EnterFullScreen / ExitFullScreen on the toolkit thread. Non-zero animate / keep_ratio
 * mean TRUE. enter returns FullScreenWindow::EnterFullScreenMode's BOOL as 1 / 0 (0 also when a C++
 * exception was caught) and fires notify_view(FULLSCREEN_ENTER) only on success; exit fires
 * notify_view(FULLSCREEN_EXIT) unconditionally. hide_cursor is accepted and IGNORED, as the JNI
 * ignored it.
 *
 * Upcall ordering depends on the host. A GlassWindow host (every FX stage) runs
 * GlassWindow::EnterFullScreenMode, which upcalls nothing - so FX sees FULLSCREEN_ENTER / EXIT and
 * nothing else. An ownerless view gets a FullScreenWindow: enter is AttachView -> SetHostHwnd ->
 * REMOVE, ADD, then FULLSCREEN_ENTER; exit with animate == 0 is DetachView -> REMOVE, ADD,
 * notify_resize, then FULLSCREEN_EXIT; exit with animate != 0 only starts a timer, FULLSCREEN_EXIT
 * fires at once and REMOVE / ADD / notify_resize arrive LATER from WM_TIMER - after this call has
 * returned, and after a View.close() in between would have deleted the GlassView the animation
 * still holds (pre-existing defect; the JNI dereferenced a deleted global ref there, the table
 * path delivers a stale id that Java must ignore silently). BLOCK and UPCALL.
 */
GLASS_WIN_EXPORT int32_t gwin_view_enter_fullscreen(gwin_view_t view, int32_t animate,
                                                    int32_t keep_ratio, int32_t hide_cursor);
GLASS_WIN_EXPORT void gwin_view_exit_fullscreen(gwin_view_t view, int32_t animate);

/*
 * GlassView::EnableInputMethodEvents: sets the flag the WndProc IME path tests and that decides a
 * WndProc return (GlassWindow answers `return 0` for the four WM_IME_* messages iff it is set), so
 * the flag stays native rather than becoming a second copy in Java. No thread marshal, as in the
 * JNI. The JNI took a jboolean through jbool_to_bool, i.e. `== JNI_TRUE`, so ANY VALUE OTHER THAN 1
 * MEANS DISABLE. Kept.
 */
GLASS_WIN_EXPORT void gwin_view_enable_ime(gwin_view_t view, int32_t enable);

/*
 * GlassView::FinishInputMethodComposition: ImmGetContext / ImmNotifyIME(NI_COMPOSITIONSTR,
 * CPS_COMPLETE) / ImmReleaseContext on the host HWND; no-op without a host or an IME context. No
 * thread marshal, as in the JNI.
 */
GLASS_WIN_EXPORT void gwin_view_finish_ime_composition(gwin_view_t view);

/*
 * ---- WinWindow: the GlassWindow C++ object, addressed by its HWND (ABI 4) ----
 *
 * The window peer is GlassWindow (GlassWindow.cpp): a BaseWnd whose virtual WindowProc handles the
 * top-level window messages and a ViewContainer that forwards the input messages to the view. Of
 * WinWindow's 30 JNI entry points, 24 were OS-CALL (ten of them only a SendMessage(WM_DO_ACTION)
 * marshal around one SetWindowPos / ShowWindow - the marshal is observable, so they stay), four were
 * PURE state the WndProc reads synchronously (grab, min/max size) and stay native for that reason,
 * _initIDs resolved the 12 jmethodIDs the table below replaces, _getAnchor was a WRAPPER over four
 * user32 calls that Java binds itself, and _setBackground2 has no caller. Hence 27 gwin_window_*
 * entry points plus the installer and the sizeof probe: 29 exports.
 *
 * THE HANDLE IS THE HWND. gwin_window_t is the value Java_com_sun_glass_ui_win_WinWindow__1createWindow
 * returned as Window.ptr and the value Window.getNativeWindow() / getRawHandle() hand to Prism and to
 * embedders - not a GlassWindow*. The C++ object is found the way it always was, through
 * ::GetProp(hWnd, "BaseWndProp") (BaseWnd::FromHandle). An HWND that is not a Glass window is
 * undefined here exactly as it was under JNI: FromHandle returns NULL and the bodies that never
 * null-checked (close, set_view, update_view_size, set_focusable, set_enabled, set_alpha, get_insets,
 * set_bounds, and the touch tail of set_visible) dereference it; the others return their failure
 * value. Nothing was added or removed on either list.
 *
 * The 11 direct upcall sites - all in GlassWindow.cpp; FullScreenWindow.cpp reaches the owner window
 * only through GlassWindow members - dial the table below, and it is the only path: the JNI arms and the
 * 30 JNI bodies are gone, so with no table installed a site delivers nothing (as in the
 * view section). Java installs it from WinApplication's static initializer, before any window exists.
 *
 * IDENTITY. Every slot's first parameter is an int64_t window_id: the id Java assigns to the
 * WinWindow before it calls gwin_window_create and hands over as that call's last argument. The
 * library stores it in the GlassWindow, never dereferences it, never holds a Java reference, and no
 * installer or slot carries a void* user - the same shape and the same reasons as the view section's
 * IDENTITY paragraph. window_id is whatever Java passed to gwin_window_create, the only way a GlassWindow
 * is made (the JNI entry point, whose windows delivered 0 and held a global ref instead, is
 * gone); a non-zero id Java no longer knows is a stale peer: do nothing,
 * return the slot default, do not log.
 *
 * THREAD. All 11 sites and all 12 slots run on the JavaFX application thread, which is the Glass
 * toolkit thread: inside DispatchMessage -> BaseWnd::StaticWindowProc -> GlassWindow::WindowProc,
 * inside the WH_CBT hook GlassWindow::CBTFilter installed for the toolkit thread, or inside a
 * gwin_window_* downcall that ExecAction has marshalled there. Four slots can arrive on ANOTHER
 * window's WndProc frame: FullScreenWindow::AttachView / DetachView call the owner's
 * SetDelegateWindow (notify_delegate_ptr, and an UngrabFocus that can fire notify_focus_ungrab on a
 * third window), and its WM_ACTIVATE / WM_CLOSE arms call the owner's HandleActivateEvent /
 * HandleCloseEvent. The window_id is always the right one - it comes from the GlassWindow whose
 * member ran. RE-ENTRANT: a slot can be entered while another slot of this table is on the stack and
 * while a gwin_window_* downcall is on the stack below that; two slots (notify_moving,
 * non_client_hit_test) return a value the OS consumes while nested. No Java target may take a lock a
 * downcall on the same thread already holds.
 *
 * DESTRUCTION ORDER, kept verbatim. Window.close() -> gwin_window_close -> GlassWindow::Close (which
 * can fire notify_focus_ungrab) -> ::DestroyWindow, synchronous: WM_DESTROY -> notify_destroy (Java
 * zeroes Window.ptr there), WM_NCDESTROY marks the BaseWnd dead, and when the OUTERMOST message on the
 * window returns BaseWnd::StaticWindowProc removes the prop and deletes the object: ~GlassWindow ->
 * notify_dispose. Slots CAN fire between notify_destroy and notify_dispose (the JNI global ref was
 * alive then and the Java methods ran; Window.shouldHandleEvent and the ptr == 0 guard in
 * notifyDestroy suppress their effect). Java's registry entry therefore tracks the C++ object's
 * lifetime, not the peer's disposal state: inserted BEFORE gwin_window_create (the CreateWindowEx
 * failure path deletes the object - and fires notify_dispose - before create returns), removed ONLY
 * from notify_dispose, and by nobody else. The one exception is when NO GlassWindow was constructed at
 * all, i.e. ExecAction no-oped because the toolkit is gone: then nothing will ever fire
 * notify_dispose and Java must remove the entry itself when create returns a value the Window
 * constructor rejects. notify_dispose fires AFTER RemoveProp, so FromHandle(hWnd) is already NULL
 * inside it: the Java target may touch its registry and nothing else.
 *
 * EXCEPTIONS. No slot may let one escape - the Java stub catches Throwable, hands it to
 * Application.reportException inside a second try/catch (reportException runs the thread's
 * UncaughtExceptionHandler, i.e. application code, and CheckAndClearException swallowed a throw from
 * there too), and returns the slot default. Every gwin_window_* body below is wrapped
 * try { } catch (...) and answers its failure value.
 *
 * NULL SLOTS. The installer copies the struct by value and replaces every NULL slot with an internal
 * no-op that returns 0, so the 11 sites never test a slot. For non_client_hit_test that 0 is
 * HTNOWHERE - an EXTENDED window whose table lacks that slot loses its caption behaviour silently.
 * Fill all 12.
 *
 * NO TOOLKIT. ENTER_MAIN_THREAD_AND_RETURN's _retValue is UNINITIALISED and ExecAction returns
 * without running the action when GlassApplication has no instance (after Application termination),
 * so create, close, set_menubar, set_title, set_resizable, request_focus, grab_focus, set_min_size
 * and set_max_size return INDETERMINATE stack contents then - exactly what the JNI returned. The
 * bodies keep it that way and Java must not normalise it: initialising _retValue is a separate,
 * defensible change with its own justification.
 *
 * critical(true) is FORBIDDEN on every function of this section: 25 of them block in
 * SendMessage(WM_DO_ACTION), most of those can upcall, gwin_window_show_system_menu runs a nested
 * modal message loop, and the two that neither block nor upcall (get_insets, set_icon) gain nothing
 * from it.
 */

/*
 * An opaque HWND: what Java_com_sun_glass_ui_win_WinWindow__1createWindow returned as Window.ptr and
 * what gwin_window_create returns. See THE HANDLE IS THE HWND above.
 */
typedef void* gwin_window_t;

/*
 * ---- GwinWindowCallbacks: the six file-static jmethodIDs of GlassWindow.cpp plus javaIDs.Window
 * (5) and javaIDs.WinWindow (1), minus the never-called midNotifyMoveToAnotherScreen, plus
 * notify_dispose ----
 *
 * Every slot: JavaFX application thread, synchronous, possibly re-entrant, possibly nested inside a
 * gwin_window_* downcall, possibly on another window's WndProc frame (see THREAD above). Boolean
 * arguments are int32_t 0 / 1.
 */
typedef struct GwinWindowCallbacks {
    /* WinWindow.notifyClose(): WM_CLOSE (GlassWindow::HandleCloseEvent) and the FullScreenWindow's
     * WM_CLOSE on the owner. The WndProc returns 0 without reaching DefWindowProc, so Alt+F4 produces
     * this and nothing else until Java calls close(). */
    void (*notify_close)(int64_t window_id);

    /* Window.notifyDestroy(): WM_DESTROY (HandleDestroyEvent). Java sets Window.ptr = 0 here. */
    void (*notify_destroy)(int64_t window_id);

    /*
     * QUERY. WinWindow.notifyMoving(int,int,int,int,float,float,int,int,int,int,int,int,int) -> int[4]
     * or null, from WM_WINDOWPOSCHANGING (HandleWindowPosChangingEvent). The former jintArray return
     * is an out-parameter: write out_bounds[0..3] = x, y, w, h and return 1 to override the pending
     * position, or return 0 for "no override" - which is what a null return, a Java throw and a
     * failed GetIntArrayRegion all produced. out_bounds is a caller-owned 4-int buffer valid for the
     * duration of the call. The JNI's "bad array length" stderr branch is unreachable with an
     * out-parameter and is gone. fx_x / fx_y are ALWAYS 0.0f from this site (the JNI passed two int
     * literals where CallObjectMethod's varargs wanted doubles - the values are dead here because
     * resize_mode is never RESIZE_TO_FX_ORIGIN from the WndProc; typed zeros are the neutral fix).
     * anchor_x / anchor_y are cursor-minus-origin when this window holds the capture, else 0 / 0;
     * resize_mode is WinWindow.RESIZE_DISABLE after WM_SIZING and RESIZE_AROUND_ANCHOR otherwise;
     * the four insets are m_insets after UpdateInsets(). Per WM_WINDOWPOSCHANGING during a drag.
     */
    int32_t (*notify_moving)(int64_t window_id,
                             int32_t x, int32_t y, int32_t w, int32_t h,
                             float fx_x, float fx_y,
                             int32_t anchor_x, int32_t anchor_y, int32_t resize_mode,
                             int32_t inset_left, int32_t inset_top,
                             int32_t inset_right, int32_t inset_bottom,
                             int32_t* out_bounds);

    /* WinWindow.notifyMove(int,int): WM_MOVE / WM_DISPLAYCHANGE / WM_SETTINGCHANGE (when not iconic)
     * and WM_SHOWWINDOW (HandleMoveEvent). x / y are the GetWindowRect origin. */
    void (*notify_move)(int64_t window_id, int32_t x, int32_t y);

    /* WinWindow.notifyResize(int,int,int): WM_SIZE and WM_SHOWWINDOW (HandleSizeEvent). type is a
     * WindowEvent constant (RESIZE, RESTORE, MINIMIZE, MAXIMIZE); width / height are the
     * GetWindowRect extent. */
    void (*notify_resize)(int64_t window_id, int32_t type, int32_t width, int32_t height);

    /* WinWindow.notifyScaleChanged(float,float,float,float): WM_DPICHANGED (HandleDPIEvent). All four
     * floats are the SAME value, LOWORD(wParam) / 96. */
    void (*notify_scale_changed)(int64_t window_id, float platform_x, float platform_y,
                                 float output_x, float output_y);

    /* Window.notifyFocus(int): WM_ACTIVATE when there is no delegate window (HandleActivateEvent, which
     * first ungrabs on FOCUS_LOST - notify_focus_ungrab can precede this), and the FullScreenWindow's
     * WM_ACTIVATE on the owner. event is WindowEvent.FOCUS_GAINED / FOCUS_LOST. */
    void (*notify_focus)(int64_t window_id, int32_t event);

    /* Window.notifyFocusDisabled(): WM_MOUSEACTIVATE and every WM_*BUTTON* / WM_MOUSE* while the window
     * is disabled, and the CBT hook's HCBT_ACTIVATE / HCBT_SETFOCUS (HandleFocusDisabledEvent). */
    void (*notify_focus_disabled)(int64_t window_id);

    /* Window.notifyFocusUngrab(): GlassWindow::UngrabFocus when this window held the process-wide grab -
     * reached from Close, set_visible(0), the WM_NC*BUTTONDOWN arm, HandleActivateEvent(FOCUS_LOST),
     * SetDelegateWindow, and ANOTHER window's GrabFocus / CheckUngrab. */
    void (*notify_focus_ungrab)(int64_t window_id);

    /* Window.notifyDelegatePtr(long): GlassWindow::SetDelegateWindow, from FullScreenWindow attach /
     * detach. delegate_hwnd is the fullscreen HWND, NULL on detach. Cold. */
    void (*notify_delegate_ptr)(int64_t window_id, void* delegate_hwnd);

    /*
     * QUERY. WinWindow.nonClientHitTest(int,int) -> int: WM_NCHITTEST for EXTENDED windows only
     * (HandleNCHitTestEvent), after DefWindowProc said HTCLIENT and with x / y in client coordinates,
     * RTL-unmirrored. Returns an HT* constant or WinWindow's HTUNSPECIFIED; the C then applies its own
     * HTTOP / HTCLIENT / m_caption post-processing, which stays in C. On a Java throw return 0
     * (HTNOWHERE): HotSpot's CallIntMethod returns 0 with a pending exception, and the shipped result
     * of that is res == 0 -> not HTCAPTION -> the WndProc answers HTNOWHERE. HTCLIENT would be an
     * improvement, hence not neutral. Per mouse move over an EXTENDED window's frame.
     */
    int32_t (*non_client_hit_test)(int64_t window_id, int32_t x, int32_t y);

    /*
     * ~GlassWindow, at the point where DeleteGlobalRef(m_grefThis) was: after WM_NCDESTROY, after the
     * outermost nested message on the window returned, after BaseWnd::StaticWindowProc's RemoveProp
     * (FromHandle(hWnd) is NULL inside this slot), and also from the gwin_window_create failure path
     * - possibly before gwin_window_create has returned. This is the ONLY place Java may drop its
     * registry entry for window_id; the target must do that and nothing else. It must NOT call
     * Window.notifyDestroy, which is the separate notify_destroy slot fired earlier from WM_DESTROY.
     */
    void (*notify_dispose)(int64_t window_id);
} GwinWindowCallbacks;   /* 12 pointers */

#if defined(__cplusplus)
static_assert(sizeof(GwinWindowCallbacks) == 12 * sizeof(void*), "GwinWindowCallbacks must be 12 pointers");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinWindowCallbacks) == 12 * sizeof(void*), "GwinWindowCallbacks must be 12 pointers");
#else
typedef char gwin_window_callbacks_size_check[(sizeof(GwinWindowCallbacks) == 12 * sizeof(void*)) ? 1 : -1];
#endif

/* sizeof probe, beside gwin_sizeof_view_callbacks: returns the real sizeof, documents today's. */
GLASS_WIN_EXPORT int32_t gwin_sizeof_window_callbacks(void);    /* == 12 * sizeof(void*) today */

/*
 * Install (cb == NULL clears) the window callback table. Copied by value, NULL slots replaced by
 * no-ops (see NULL SLOTS), so the caller may free the struct; the stubs in it must outlive the
 * process - Arena.global(), as for the other tables - because there is no point at which this
 * library can promise no further WndProc message. Last call wins, no lock: Java installs it once
 * from WinApplication's static initializer, on the launcher thread, before Application.run() creates
 * the toolkit window. NO void* user - see IDENTITY. Always GWIN_OK.
 */
GLASS_WIN_EXPORT int32_t gwin_window_set_callbacks(const GwinWindowCallbacks* cb);

/*
 * TEST HOOK, unconditionally exported, never called by production code - gwin_test_fire_callback for
 * the window table. Fires slot `slot` of the installed table with a fixed argument pattern and returns
 * what the slot returned: 0 for the void slots and for a slot that was installed as NULL, the int32
 * answer of slots 2 and 10; GWIN_ERR_INVALID_ARG for an unknown slot, when no table is installed, or
 * when out_bounds is NULL for slot 2. It exists for the same reason as the view hook: the sizeof probe
 * cannot catch a FunctionDescriptor whose parameter list disagrees with the prototype, and
 * notify_moving's four-int / two-float / seven-int / pointer sequence is where a shifted parameter
 * hides. Every value is distinct - the production notify_scale_changed site passes ONE value four
 * times, which would never show a swapped float - so a shift, a swap or a truncation is visible.
 *
 * out_bounds is a caller-owned int32[4] handed to slot 2 UNCHANGED - the test pre-fills it with a
 * sentinel and reads back what its notify_moving target wrote, which is nothing on null / throw / a
 * wrong length - and ignored by every other slot, which accept NULL.
 *
 * Slot numbers (GwinWindowCallbacks declaration order) and the exact pattern delivered (window_id is
 * passed through unchanged):
 *     0 notify_close(window_id)
 *     1 notify_destroy(window_id)
 *     2 notify_moving(window_id, 1001, 1002, 1003, 1004, 1.5f, 2.5f, 1005, 1006, 1, 1007, 1008, 1009,
 *                     1010, out_bounds) - its int32 return comes back
 *     3 notify_move(window_id, 1001, 1002)
 *     4 notify_resize(window_id, 1001, 1002, 1003)
 *     5 notify_scale_changed(window_id, 1.5f, 2.5f, 3.5f, 4.5f)
 *     6 notify_focus(window_id, 1001)
 *     7 notify_focus_disabled(window_id)
 *     8 notify_focus_ungrab(window_id)
 *     9 notify_delegate_ptr(window_id, (void*) 0x00007FFE12345678) - bits above 32 set so a truncated
 *       pointer shows; never dereferenced
 *    10 non_client_hit_test(window_id, 1001, 1002) - its int32 return comes back
 *    11 notify_dispose(window_id)
 * With the PRODUCTION table installed slots 1 and 11 reach WinWindow's registry bookkeeping (Window.ptr
 * = 0, registry remove) for window_id - fire them at an id the registry does not hold, or through a
 * recording table.
 */
GLASS_WIN_EXPORT int64_t gwin_test_fire_window_callback(int32_t slot, int64_t window_id, int32_t* out_bounds);

/*
 * new GlassWindow + BaseWnd::Create, marshalled to the toolkit thread by SendMessage(WM_DO_ACTION)
 * exactly as ENTER_MAIN_THREAD_AND_RETURN(jlong) did in Java_..._1createWindow.
 *
 * owner_hwnd is Window.getNativeHandle() of the owner, or NULL. monitor is the HMONITOR
 * Screen.getNativeScreen() holds; it is stored as m_hMonitor and NEVER READ AGAIN (GetMonitor /
 * SetMonitor have no caller) - CreateWindowEx gets CW_USEDEFAULT unless the style is WS_POPUP, in
 * which case BaseWnd::GetDefaultWindowBounds supplies a rect. mask is the com.sun.glass.ui.Window
 * style bit set; the whole style computation stays in C, including that EXTENDED implies TITLED and
 * that a non-TITLED non-MODAL window silently gains WS_MINIMIZEBOX. window_id is stored and handed
 * back unchanged as the first argument of every slot (IDENTITY); it takes the place of the
 * NewGlobalRef(jThis) the JNI body took, and a GlassWindow created here holds no Java reference.
 *
 * Returns the HWND, or NULL when CreateWindowEx failed - in which case the GlassWindow has been
 * deleted before returning and notify_dispose(window_id) has ALREADY fired. Java must therefore
 * register window_id BEFORE calling this, and must unregister it itself when the toolkit is gone
 * (NO TOOLKIT above: nothing was constructed, nothing will fire). NULL also when a C++ exception was
 * caught. BLOCKS (SendMessage) and UPCALLS on failure - never critical(true).
 */
GLASS_WIN_EXPORT gwin_window_t gwin_window_create(void* owner_hwnd, void* monitor,
                                                  int32_t mask, int64_t window_id);

/*
 * GlassWindow::Close (UngrabFocus, ReleaseDropTarget, ReleaseManipProcessor) then ::DestroyWindow.
 * Returns DestroyWindow's BOOL as 1 / 0 (0 also when a C++ exception was caught). DestroyWindow
 * sends WM_DESTROY and WM_NCDESTROY synchronously, so notify_close is possible, notify_destroy is
 * certain and notify_dispose is likely (DESTRUCTION ORDER) before this returns. Does not null-check
 * the GlassWindow. BLOCKS and UPCALLS.
 */
GLASS_WIN_EXPORT int32_t gwin_window_close(gwin_window_t win);

/*
 * Java_..._1setView's action: clears the active touch window when it is this one, ResetMouseTracking,
 * then ViewContainer::SetGlassView(view). `view` is the gwin_view_t of the view section, or NULL.
 * Always returns 1: the JNI returned JNI_TRUE from outside the action, so a failure inside was
 * invisible (0 only when a C++ exception was caught). Does not null-check the GlassWindow. BLOCKS.
 */
GLASS_WIN_EXPORT int32_t gwin_window_set_view(gwin_window_t win, gwin_view_t view);

/* ::IsWindowVisible(hWnd) ? ViewContainer::NotifyViewSize : nothing. Does not null-check the
 * GlassWindow. BLOCKS and UPCALLS (the VIEW table's notify_resize). */
GLASS_WIN_EXPORT void gwin_window_update_view_size(gwin_window_t win);

/*
 * ::SetMenu(hWnd, hmenu) and, on success, GlassWindow::SetMenu so UpdateInsets adds SM_CYMENU. hmenu
 * is the HMENU WinMenuImpl holds (its long ptr). Returns 1 when SetMenu succeeded, 0 otherwise.
 * BLOCKS.
 */
GLASS_WIN_EXPORT int32_t gwin_window_set_menubar(gwin_window_t win, void* hmenu);

/*
 * ::SetWindowPos(hWnd, HWND_TOPMOST or HWND_NOTOPMOST, SWP_ASYNCWINDOWPOS | SWP_NOACTIVATE |
 * SWP_NOMOVE | SWP_NOOWNERZORDER | SWP_NOSIZE). level is com.sun.glass.ui.Window.Level: FLOATING (2)
 * and TOPMOST (3) map to HWND_TOPMOST, EVERY other value - NORMAL (1) and anything out of range -
 * maps to HWND_NOTOPMOST. The mapping stays in C so (HWND)-1 does not become a Java constant.
 * BLOCKS.
 */
GLASS_WIN_EXPORT void gwin_window_set_level(gwin_window_t win, int32_t level);

/*
 * GlassWindow::SetFocusable: toggles WS_EX_NOACTIVATE and, when losing focusability while ::GetFocus()
 * is this window, ::SetFocus(NULL); sets m_isFocusable, which CBTFilter and WM_MOUSEACTIVATE read.
 * The JNI compared its jboolean against JNI_TRUE, so ANY VALUE OTHER THAN 1 MEANS FALSE. Kept. Does
 * not null-check the GlassWindow. BLOCKS.
 */
GLASS_WIN_EXPORT void gwin_window_set_focusable(gwin_window_t win, int32_t focusable);

/*
 * GlassWindow::SetEnabled (which ResetMouseTracking's when disabling) then ::EnableWindow. m_isEnabled
 * gates six WindowProc arms and CBTFilter. Same "!= 1 means false" rule as set_focusable. Does not
 * null-check the GlassWindow. BLOCKS.
 */
GLASS_WIN_EXPORT void gwin_window_set_enabled(gwin_window_t win, int32_t enabled);

/*
 * GlassWindow::SetAlpha. The float -> BYTE conversion is F2B(value) = BYTE(255.f * value), computed
 * on the CALLING thread before the action is queued and truncating toward zero: 1.0f -> 255,
 * 0.999f -> 254, and values outside [0, 1] wrap - reproduced, not clamped. A transparent window
 * returns early and keeps the value for the layered upload's BLENDFUNCTION; an opaque one toggles
 * WS_EX_LAYERED and calls SetLayeredWindowAttributes(RGB(0,0,0), alpha, LWA_ALPHA). Does not
 * null-check the GlassWindow. BLOCKS.
 */
GLASS_WIN_EXPORT void gwin_window_set_alpha(gwin_window_t win, float alpha);

/*
 * GlassWindow::SetDarkFrame: resolves DWMWA_USE_IMMERSIVE_DARK_MODE once, from dwmapi.dll's file
 * version (19 for Windows 10 builds 17763..18984, 20 from 18985, 0 = unsupported) in a function-local
 * static - on whichever thread calls first, never again - then DwmSetWindowAttribute. Non-zero dark
 * means TRUE. A NULL GlassWindow is checked. BLOCKS.
 */
GLASS_WIN_EXPORT void gwin_window_set_dark_frame(gwin_window_t win, int32_t dark);

/*
 * UpdateInsets then the packed insets, exactly as Java_..._1getInsets: (left << 48) | (top << 32) |
 * (right << 16) | bottom, each masked to 16 bits by the shift only - an inset above 65535 or below
 * 0 aliases into its neighbour, as it did (WinWindow unpacks with >> and & 0xffff). Returns 0 when
 * ::IsWindow is false. NOT marshalled to the toolkit thread (there was no ENTER_MAIN_THREAD), and it
 * MUTATES m_insets, which HandleWindowPosChangingEvent reads; off the toolkit thread it races exactly
 * as the JNI did. Does not null-check the GlassWindow.
 */
GLASS_WIN_EXPORT int64_t gwin_window_get_insets(gwin_window_t win);

/*
 * Java_..._1setBounds, unchanged: UpdateInsets, GetWindowRect, then newX / newY = x / y when x_set /
 * y_set == 1 (jbool_to_bool) else the current origin; newW / newH = w / h when > 0, else cw / ch +
 * the matching insets when > 0, else the current extent; clamped by m_minSize / m_maxSize where those
 * are >= 0; then SetWindowPos(SWP_NOACTIVATE | SWP_NOZORDER | SWP_NOSENDCHANGING), with SWP_NOMOVE
 * when x_set and y_set are BOTH ZERO (that test is `xSet || ySet`, non-zero, not jbool_to_bool -
 * reproduced). Returns silently when ::IsWindow is false; does not null-check the GlassWindow after
 * that. x_gravity / y_gravity are accepted and IGNORED - the JNI declared them and never read them.
 * SWP_NOSENDCHANGING means this path does NOT produce notify_moving. BLOCKS.
 */
GLASS_WIN_EXPORT void gwin_window_set_bounds(gwin_window_t win,
                                             int32_t x, int32_t y, int32_t x_set, int32_t y_set,
                                             int32_t w, int32_t h, int32_t cw, int32_t ch,
                                             float x_gravity, float y_gravity);

/*
 * ::SetWindowTextW(hWnd, title). title is a NUL-terminated UTF-16 string; the JNI built it with
 * JString = GetStringLength + GetStringRegion + an appended L'\0' - UTF-16 code units, never
 * modified UTF-8, surrogates untouched - so Java passes allocateFrom(JAVA_CHAR, ...) plus a
 * terminator, NOT allocateFrom(String) (UTF-8). A title containing U+0000 was truncated there by
 * SetWindowText and still is. NULL is not handled (JString dereferenced a null jstring); Window.setTitle
 * substitutes "" for null and the Java side keeps doing that. Returns 1 on success, 0 otherwise.
 * BLOCKS.
 */
GLASS_WIN_EXPORT int32_t gwin_window_set_title(gwin_window_t win, const uint16_t* title);

/*
 * GlassWindow::SetResizable: returns 0 without doing anything for a WS_CHILD window; otherwise
 * toggles WS_MAXIMIZEBOX (plus WS_THICKFRAME when decorated), SetStyle (SetWindowLong + a
 * SWP_FRAMECHANGED SetWindowPos), sets m_isResizable and returns 1. Also 0 when the GlassWindow is
 * NULL. resizable goes through jbool_to_bool: ONLY 1 MEANS TRUE. BLOCKS.
 */
GLASS_WIN_EXPORT int32_t gwin_window_set_resizable(gwin_window_t win, int32_t resizable);

/*
 * Java_..._1setVisible, unchanged. Hiding first UngrabFocus's (null-checked) and, when this window
 * owns the active touch sequence, synthesises HandleViewTouchEvent(hWnd, 0, 0, 0) to end it (NOT
 * null-checked). Then ShowWindow(SW_SHOW / SW_HIDE). Showing then either SetForegroundWindow or, for
 * an unfocusable window, the TOPMOST-then-TOP two-step of JDK-8112905, and UpdateWindow. Non-zero
 * visible means TRUE. RETURNS ITS OWN ARGUMENT, not a status - the JNI's `return visible;` sat
 * outside the action and Window.setVisible only tests it for truth (0 only when a C++ exception was
 * caught). BLOCKS and UPCALLS (notify_focus_ungrab; the touch teardown reaches the view / gesture
 * tables).
 */
GLASS_WIN_EXPORT int32_t gwin_window_set_visible(gwin_window_t win, int32_t visible);

/*
 * GlassWindow::RequestFocus -> ::SetForegroundWindow. Returns 1 when that succeeded and the
 * GlassWindow was non-NULL, else 0. The focus event itself arrives later, from WM_ACTIVATE. event is
 * ASSERTed to be WindowEvent.FOCUS_GAINED and otherwise unused - a no-op in release builds. BLOCKS.
 */
GLASS_WIN_EXPORT int32_t gwin_window_request_focus(gwin_window_t win, int32_t event);

/*
 * GlassWindow::GrabFocus / UngrabFocus. The grab is one process-wide HWND (sm_grabWindow) over the
 * DELEGATE window when one exists. Grabbing a second window ungrabs the first, which fires
 * notify_focus_ungrab on THAT window's id, not this one's. grab_focus returns 1 for a non-NULL
 * GlassWindow, 0 otherwise; ungrab_focus does nothing when this window does not hold the grab or
 * the GlassWindow is NULL. Both BLOCK and UPCALL.
 */
GLASS_WIN_EXPORT int32_t gwin_window_grab_focus(gwin_window_t win);
GLASS_WIN_EXPORT void    gwin_window_ungrab_focus(gwin_window_t win);

/*
 * ::ShowWindow(SW_MINIMIZE / SW_RESTORE) and ::ShowWindow(SW_MAXIMIZE / SW_RESTORE); non-zero means
 * the first. Both ALWAYS return 1 - the `return JNI_TRUE` sat outside the action and ShowWindow's
 * result was discarded (0 only when a C++ exception was caught). was_maximized is accepted and never
 * read, exactly as in the JNI. The state change comes back asynchronously as WM_SIZE ->
 * notify_resize. BLOCK and UPCALL.
 */
GLASS_WIN_EXPORT int32_t gwin_window_minimize(gwin_window_t win, int32_t minimize);
GLASS_WIN_EXPORT int32_t gwin_window_maximize(gwin_window_t win, int32_t maximize,
                                              int32_t was_maximized);

/*
 * GlassWindow::setMinSize / setMaxSize. Pure state that stays native: WM_GETMINMAXINFO answers it
 * synchronously inside DefWindowProc's sizing loop and gwin_window_set_bounds clamps with it. -1 means
 * "not set". Return 1, or 0 when the GlassWindow is NULL.
 *
 * ASYMMETRY, reproduced: set_min_size maps a 0 argument to -1 on the caller's side of the marshal;
 * set_max_size does NOT, so a maximum of 0 really pins the window to zero. BLOCK.
 */
GLASS_WIN_EXPORT int32_t gwin_window_set_min_size(gwin_window_t win, int32_t width, int32_t height);
GLASS_WIN_EXPORT int32_t gwin_window_set_max_size(gwin_window_t win, int32_t width, int32_t height);

/*
 * GlassWindow::SetIcon(hicon): WM_SETICON for ICON_SMALL and ICON_BIG, then DestroyIcon of the
 * PREVIOUS icon, and the window takes OWNERSHIP of hicon - it is destroyed on the next set_icon and
 * in ~GlassWindow, so Java must never DestroyIcon a handle it has passed here. hicon is the HICON
 * Java built from the Pixels with the gdi32 / user32 binds it has had since WinCursor was flipped
 * (CreateBitmap mask + CreateDIBSection colour + CreateIconIndirect with fIcon = TRUE + GdiFlush -
 * Pixels::CreateIcon's sequence); NULL means "no icon", the JNI's null-Pixels branch. NOT marshalled
 * - there was no ENTER_MAIN_THREAD in the JNI body. A NULL GlassWindow is checked (and hicon is
 * then NOT destroyed - the caller still owns it in that one case, as the JNI leaked it). Neither
 * blocks nor upcalls.
 */
GLASS_WIN_EXPORT void gwin_window_set_icon(gwin_window_t win, void* hicon);

/*
 * ::SetWindowPos(HWND_TOP) - preceded by a SetWindowPos(HWND_TOPMOST) for an unfocusable window, the
 * JDK-8112905 workaround again - and ::SetWindowPos(HWND_BOTTOM). Both with SWP_NOMOVE | SWP_NOSIZE |
 * SWP_NOACTIVATE. to_front null-checks the GlassWindow only for the unfocusable test. BLOCK.
 */
GLASS_WIN_EXPORT void gwin_window_to_front(gwin_window_t win);
GLASS_WIN_EXPORT void gwin_window_to_back(gwin_window_t win);

/*
 * BaseWnd::SetCursor on this window and, when one exists, on the delegate window. hcursor is the
 * HCURSOR Java resolves from the com.sun.glass.ui.Cursor: Cursor.getNativeCursor() for CURSOR_CUSTOM,
 * NULL for a null Cursor and for CURSOR_NONE, and otherwise the IDC_* ordinal (or GlassResources.rc
 * name) WinCursor maps it to - the switch of the former GlassCursor.cpp GetNativeCursor, its ordinals
 * pinned by static_assert in glass_win_api.cpp - loaded through the unconditional
 * three-step LoadCursorW(NULL, id), LoadCursorW(GetModuleHandleW(L"glass.dll"), id) - the two
 * GlassResources.rc cursors "IDC_CLOSED_HAND" / "IDC_OPEN_HAND" only resolve there - then
 * LoadCursorW(NULL, IDC_ARROW). The C mapping and the JNI body are gone.
 * SetCursor stores hcursor in m_hCursor - replayed by WM_SETCURSOR on every mouse move -
 * AND calls ::SetCursor immediately, unconditionally. The conversion the JNI did INSIDE the action,
 * on the toolkit thread, runs in Java before the call - the same thread on Windows unless
 * -Dglass.disableThreadChecks is set. A NULL GlassWindow is checked. BLOCKS.
 */
GLASS_WIN_EXPORT void gwin_window_set_cursor(gwin_window_t win, void* hcursor);

/*
 * GlassWindow::ShowSystemMenu: GetWindowPlacement (returns early when it fails), the RTL x mirror for
 * WS_EX_LAYOUTRTL windows, six SetMenuItemInfo calls that enable / disable SC_RESTORE / SC_MOVE /
 * SC_SIZE / SC_MINIMIZE / SC_MAXIMIZE / SC_CLOSE from the style bits and m_isResizable,
 * SetMenuDefaultItem(UINT_MAX), ClientToScreen, then TrackPopupMenu(TPM_RETURNCMD) - which RUNS A
 * NESTED MODAL MESSAGE LOOP on the toolkit thread - and PostMessage(WM_SYSCOMMAND) with the chosen
 * item. x / y are CLIENT coordinates in physical pixels. A NULL GlassWindow is checked. BLOCKS for as
 * long as the menu is open and every message dispatched inside it can upcall; never call it from a
 * Java upcall target.
 */
GLASS_WIN_EXPORT void gwin_window_show_system_menu(gwin_window_t win, int32_t x, int32_t y);

/*
 * ---- Screens (ABI 5) ----
 *
 * GlassScreen.cpp's former enumeration, DPI query and FX-space anchoring are Java (WinScreenLayout over
 * user32 / gdi32 / shcore binds; the C is gone), which also builds the
 * com.sun.glass.ui.Screen objects. What only the
 * WndProc can see is WHEN they may have changed: GlassApplication's WM_DISPLAYCHANGE and
 * WM_SETTINGCHANGE(SPI_SETWORKAREA), GlassWindow's WM_DPICHANGED - GlassScreen::HandleDisplayChange.
 * This table's one slot replaces its Screen.notifySettingsChanged() upcall.
 * INSTALL once from WinApplication.<clinit>, beside the other tables. Until then HandleDisplayChange
 * delivers nothing (it has no JNI path).
 * THREAD: toolkit thread, inside DispatchMessage, synchronous and RE-ENTRANT - the Java target
 * re-enumerates (EnumDisplayMonitors from inside this callback), replaces the Screen list and rebinds
 * every Window before it returns. EXCEPTIONS: the stub catches Throwable -> Application.reportException,
 * where CheckAndClearException sent it. NULL slot -> internal no-op. No void* user (see the view
 * section's IDENTITY paragraph).
 */
typedef struct GwinScreenCallbacks {
    void (*settings_changed)(void);
} GwinScreenCallbacks;

#if defined(__cplusplus)
static_assert(sizeof(GwinScreenCallbacks) == 1 * sizeof(void*), "GwinScreenCallbacks must be 1 pointer");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinScreenCallbacks) == 1 * sizeof(void*), "GwinScreenCallbacks must be 1 pointer");
#else
typedef char gwin_screen_callbacks_size_check[(sizeof(GwinScreenCallbacks) == 1 * sizeof(void*)) ? 1 : -1];
#endif

GLASS_WIN_EXPORT int32_t gwin_sizeof_screen_callbacks(void);                          /* == sizeof(void*) today */
GLASS_WIN_EXPORT int32_t gwin_screen_set_callbacks(const GwinScreenCallbacks* cb);    /* NULL clears; always GWIN_OK */
/* TEST HOOK: slot 0 fires settings_changed() and returns 0; another slot or no table -> GWIN_ERR_INVALID_ARG. */
GLASS_WIN_EXPORT int64_t gwin_test_fire_screen_callback(int32_t slot);

/*
 * ---- Clipboard, drag-and-drop and the common dialogs (ABI 4) ----
 *
 * Three JNI peers share this section. WinSystemClipboard (GlassClipboard.cpp, 12 exports) owns one
 * IDataObject at a time - either its own ClipboardData, a C++ IDataObject/IEnumFORMATETC pair that
 * renders Java data on demand for whoever pastes, or a foreign one taken with OleGetClipboard.
 * WinDnDClipboard is the same peer wired to DoDragDrop instead of OleSetClipboard, plus the OLE
 * IDropTarget / IDropSource pair of GlassDnD.cpp, which is constructed by ViewContainer::InitDropTarget
 * and is never exported. WinCommonDialogs (CommonDialogs.cpp, 3 exports) runs the Vista IFileDialog or,
 * below Vista, comdlg32 / SHBrowseForFolder. The COM objects stay in C++ - Windows and other processes
 * call their vtables through the OLE marshaller - and the mime <-> CLIPFORMAT maps stay process-wide
 * native state. What crosses this boundary is scalars, UTF-16 strings, byte blocks and two callback
 * tables. Of the 15 JNI exports 13 get a gwin_* twin here; WinSystemClipboard.isOwner is a WRAPPER
 * (Java binds ole32!OleIsCurrentClipboard itself, keeping the `ptr == NULL -> false` short-circuit of
 * the JNI body) and the two initIDs vanish with JNI.
 *
 * The section landed ADDITIVE under ABI 3 in the sense of the view and window sections (every upcall site took its
 * JNI path while no table was installed). Since the Java flip and the removal of the JNI paths the two
 * tables are the only path - ClipboardData::GetData / SetData, ~ClipboardData, GlassApplication's
 * WM_DRAWCLIPBOARD and DisposeRegisteredClipboard, and the eight sites of GlassDnD.cpp deliver nothing
 * while no table is installed - and the 15 JNI bodies are gone. That flip came with version 4: it travelled
 * with the WinView flip, because the drag slots below deliver the view_id that only the bumped
 * Java_com_sun_glass_ui_win_WinView__1create handed to the C (gwin_view_create since ABI 5).
 *
 * IDENTITY. Every clipboard slot's first parameter is an int64_t clipboard_id: the id Java assigns to
 * the WinSystemClipboard (or WinDnDClipboard) peer and passes to gwin_clipboard_register_viewer,
 * gwin_clipboard_push and gwin_dnd_push, which store it in the ClipboardData they construct (in place
 * of the NewGlobalRef the JNI body took) and in GlassApplication (in place of m_clipboard). The four
 * drag slots carry the int64_t view_id of the view under the cursor - ViewContainer::GetViewId(), the
 * view section's IDENTITY. The library never dereferences an id and never holds a Java
 * reference. No installer and no slot carries a void* user (see the view section's IDENTITY). The ONE
 * deliberate exception: the five DnD-singleton slots (dnd_get_data_object .. dnd_get_drag_button) carry
 * NO id, because their peer is WinDnDClipboard.getInstance() - a call that CREATES the peer when none
 * exists yet and throws off the FX thread, both of which the JNI relied on (it called exactly that
 * static method); the Java target must call it, not a registry. A clipboard_id Java no longer knows
 * is a stale peer: do nothing, return the slot default, do not log.
 *
 * THREAD. Every slot and every function here runs on the JavaFX application thread, which is the
 * Glass toolkit thread (an STA). Nine of the eleven clipboard functions are marshalled through
 * SendMessage(WM_DO_ACTION) exactly as the JNI bodies were - the caller is that thread, so the
 * message is dispatched inline, but the body runs inside a WndProc frame, which is what the nested
 * pumping below relies on. THE SLOTS ARE NOT NECESSARILY NESTED IN A DOWNCALL: fos_serialize,
 * action_performed, content_changed and the four drag slots fire from inside DispatchMessage when
 * another process pastes, drags over us or changes the clipboard - through a COM vtable, through
 * the OLE marshaller, with NO Java frame below them. RE-ENTRANT: gwin_clipboard_dispose pumps
 * messages (OLE_CoPump) while OleFlushClipboard cannot open the clipboard; gwin_dnd_push blocks in
 * DoDragDrop's modal loop for the whole drag, during which the drag slots, fos_serialize (delayed
 * render by the drop target) and dnd_* fire nested inside it; gwin_dialog_* block in a modal dialog
 * that re-enters the message pump; fos_serialize re-enters itself (an IE shortcut renders
 * text/uri-list and message/external-body from inside its own GetData). No Java target may take a
 * lock a downcall on the same thread already holds, and NO TARGET MAY CALL gwin_dnd_push or a
 * gwin_dialog_* function - that would nest a modal loop inside an OLE callback.
 *
 * EXCEPTIONS - the single most important rule of this section. A Java Throwable escaping an upcall
 * stub here unwinds through a COM vtable frame (ClipboardData::GetData, GlassDropTarget::Drop) into
 * the OLE marshaller and, for a paste or a drop from another application, into that application.
 * Every target catches Throwable and returns the slot default; the sink differs per slot and each
 * slot below says which: `printStackTrace` where the C used checkJavaException (OleUtils.h: describe,
 * clear, E_JAVAEXCEPTION - no Java code runs), Application.reportException where it used
 * CheckAndClearException (the thread's UncaughtExceptionHandler, i.e. application code -
 * itself inside a second try/catch). In the other direction every gwin_* body below is wrapped
 * try { } catch (...) and answers its failure value.
 *
 * NULL SLOTS. The installers copy the struct by value and replace every NULL slot with an internal
 * no-op that returns GWIN_OK and writes nothing, so the sites never test a slot. For fos_serialize
 * that no-op yields *out_data == NULL, which GetData turns into E_POINTER: an installed table without
 * that slot serves nothing to anyone who pastes. Fill all 16.
 *
 * NO TOOLKIT. DELIBERATELY UNLIKE the view and window sections, the marshalled functions here return
 * a DEFINED value when GlassApplication has no instance and ExecAction does not run the body:
 * gwin_clipboard_pop -> NULL, gwin_clipboard_pop_supported_actions -> Clipboard.ACTION_NONE, the
 * int32_t functions -> GWIN_ERR_NO_TOOLKIT with their out-parameters NULL / 0. The former JNI bodies
 * returned an uninitialised jobject / jbyteArray / jobjectArray there; a garbage HANDLE is stored by
 * Java and dereferenced by the next call, which a garbage int was not, so the divergence was
 * deliberate.
 *
 * MEMORY. A "string block" is count NUL-terminated UTF-16 strings laid end to end, terminated by one
 * extra NUL (so it is double-NUL-terminated; count is passed beside it, never derived, so an empty
 * string in the middle cannot truncate the list). Mime names never contain U+0000 (they come from
 * RegisterClipboardFormat names and the GLASS_* literals of GlassClipboard.cpp). Every string Java
 * passes IN is NUL-terminated UTF-16, borrowed for the duration of the call, and copied by the
 * library before it returns (pushCommit copies mimes into its own maps, the dialogs into
 * COMDLG_FILTERSPEC / DNTString) - a confined per-call arena is correct. Every block the library
 * hands OUT (gwin_clipboard_pop_bytes, gwin_clipboard_pop_mimes, gwin_dialog_file, gwin_dialog_folder)
 * is owned by the caller from the moment the function returns and is released with gwin_free
 * exactly once; the block Java hands the library from fos_serialize is the mirror image (see
 * gwin_alloc). Byte payloads are (uint8_t*, int32_t length): clipboard data is bytes, as
 * fosSerialize / popBytes always were.
 *
 * critical(true) is FORBIDDEN on every function of this section: nine block in SendMessage and can
 * upcall, gwin_dnd_push and the two dialogs run nested modal loops, gwin_clipboard_dispose pumps,
 * and the two set_callbacks functions publish a pointer the OLE marshaller will dial.
 */

/*
 * One IDataObject*: what WinSystemClipboard.ptr held as a long. Either the library's own ClipboardData
 * (after gwin_clipboard_push / gwin_dnd_push) or a foreign data object (after gwin_clipboard_pop).
 * NULL means "no data object", exactly as ptr == 0 did.
 */
typedef void* gwin_clipboard_t;

/*
 * ---- GwinClipboardCallbacks: the three jmethodIDs of WinSystemClipboard.initIDs (fosSerialize,
 * actionPerformed x2 sites, contentChanged), the re-entered dispose export of
 * GlassApplication::RegisterClipboardViewer, the setPtr field write, and ~ClipboardData ----
 *
 * Every slot: JavaFX application thread, synchronous, re-entrant, possibly with NO Java frame below
 * (see THREAD). Boolean arguments are int32_t 0 / 1. Sinks per slot as noted.
 */
typedef struct GwinClipboardCallbacks {
    /*
     * WinSystemClipboard.fosSerialize(String mime, long index) -> byte[], from ClipboardData::GetData:
     * delayed rendering, i.e. whoever pastes (this process, OleFlushClipboard, or another application
     * through the marshaller) asks for one format. mime is the decoded mime the C registered in
     * pushCommit (never the ";locale" / ";cf=" variants), lindex the FORMATETC.lindex (-1, or 0 for
     * the IE-shortcut file content). Returns GWIN_OK with *out_data == NULL when Java returned null
     * (GetData answers E_POINTER, as OLE_CHECK_NOTNULL did), or GWIN_OK with *out_data pointing at a
     * block of *out_len bytes THAT JAVA OBTAINED FROM gwin_alloc - the library takes ownership on
     * return and frees it with gwin_free on every path, including its own failures (an RAII holder,
     * so the release cannot be skipped by OLE_HRT). A zero-length array is a gwin_alloc(0) block with
     * *out_len 0, not NULL. GWIN_ERR_UPCALL: the target threw - nothing is owned, and GetData returns
     * E_JAVAEXCEPTION, the HRESULT checkJavaException produced (sink: printStackTrace, no Java code).
     * ONE invocation per GetData, as the JNI made one CallObjectMethod; re-entrant (IE shortcut).
     * RAW_IMAGE_TYPE arrives as 4-byte big-endian width, height, then BGRA rows - PushImageBytes
     * validates and converts exactly as PushImage did.
     */
    int32_t (*fos_serialize)(int64_t clipboard_id, const uint16_t* mime, int64_t lindex,
                             uint8_t** out_data, int32_t* out_len);

    /*
     * WinSystemClipboard.actionPerformed(int) from ClipboardData::SetData of CFSTR_PERFORMEDDROPEFFECT
     * (the drop target of a cut-and-paste reports the effect; cross-process, no Java frame below).
     * action is getACTION(DROPEFFECT), a Clipboard.ACTION_* set. GWIN_ERR_UPCALL makes SetData
     * return E_JAVAEXCEPTION, as OLE_HRT(checkJavaException) did (sink: printStackTrace).
     */
    int32_t (*action_performed)(int64_t clipboard_id, int32_t action);

    /*
     * The SAME Java method, actionPerformed(int), from the tail of gwin_dnd_push after DoDragDrop
     * returned: getACTION(performedDropEffect) on success, getACTION(DROPEFFECT_NONE) on failure,
     * fired unconditionally and BEFORE dnd_set_drag_button(0). A separate slot because the JNI used
     * the other sink here - a bare CheckAndClearException, i.e. Application.reportException - and
     * discarded the result; the return is ignored.
     */
    void (*drag_action_performed)(int64_t clipboard_id, int32_t action);

    /*
     * WinSystemClipboard.contentChanged() from GlassApplication's WM_DRAWCLIPBOARD, for the
     * clipboard_id the last gwin_clipboard_register_viewer supplied, and only while one is
     * registered. Can arrive NESTED INSIDE gwin_clipboard_push: CloseClipboard sends WM_DRAWCLIPBOARD
     * to the viewer chain synchronously from inside OleSetClipboard - which is why set_data_object
     * below fires first. The JNI cleared a throw with CheckAndClearException (sink:
     * Application.reportException); the result is ignored.
     */
    void (*content_changed)(int64_t clipboard_id);

    /*
     * The former re-entered Java_com_sun_glass_ui_win_WinSystemClipboard_dispose of
     * GlassApplication::RegisterClipboardViewer, now id-keyed: fires for the PREVIOUSLY registered
     * clipboard_id when a second gwin_clipboard_register_viewer arrives (a second
     * WinSystemClipboard.create - "user skipped ClipboardAssistance close") and from the toolkit
     * window's WM_DESTROY (RegisterClipboardViewerId(0): "alarm clipboard dispose if any" - THIS is
     * what renders the delayed data with OleFlushClipboard so a paste still works after the
     * application quit). The Java target must call gwin_clipboard_dispose with that peer's handle -
     * the JNI read the peer's ptr field and ran the whole dispose body - and may null the field
     * (the JNI did not; a later close() then Released a dead object). If the target does not
     * unregister (no-op slot, unknown id), RegisterClipboardViewer unregisters the viewer itself so
     * the clipboard chain is never entered twice - the one deviation on this path. Sink:
     * Application.reportException (the JNI export cleared nothing; a throw propagated into
     * WindowProc, which is worse).
     */
    void (*dispose_peer)(int64_t clipboard_id);

    /*
     * The JNI's setPtr(env, obj, pcd): store data_object as the peer's handle. Fired by
     * gwin_clipboard_push and gwin_dnd_push right after the ClipboardData is constructed and BEFORE
     * pushCommit / OleSetClipboard / setDragImage / DoDragDrop - the instant the JNI wrote the field -
     * so that a content_changed nested in OleSetClipboard, or a drag_enter of a self-drag inside
     * DoDragDrop (whose dnd_get_data_object must find THIS object, or the AddRef / Release pair
     * runs and the object leaks), sees the new handle. The push functions therefore return a status,
     * not the handle: a returned handle would re-store an object Java may have closed in between.
     * Without this slot the object is unreachable and leaks - it is not optional. Cannot fail
     * (SetLongField could not); a throw is reported and ignored.
     */
    void (*set_data_object)(int64_t clipboard_id, void* data_object);

    /*
     * ~ClipboardData, at the point where DeleteGlobalRef(m_jclipboard) was: the last Release of a
     * ClipboardData bound to clipboard_id - from gwin_clipboard_dispose / gwin_dnd_dispose, from the
     * next push, or from OLE when another application takes the clipboard, and possibly LONG AFTER
     * WinSystemClipboard.close() when a paste holder kept it alive. The JNI global ref kept the peer
     * reachable exactly that long, so fosSerialize could still serve it; Java must keep its registry
     * entry strong until the peer is closed AND every ClipboardData it pushed has fired this (count
     * the pushes, decrement here). Several objects can be bound to one id (each push constructs a
     * new one; the old one may outlive the Release). The target must touch its registry and nothing
     * else.
     */
    void (*data_object_disposed)(int64_t clipboard_id);
} GwinClipboardCallbacks;   /* 7 pointers */

#if defined(__cplusplus)
static_assert(sizeof(GwinClipboardCallbacks) == 7 * sizeof(void*), "GwinClipboardCallbacks must be 7 pointers");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinClipboardCallbacks) == 7 * sizeof(void*), "GwinClipboardCallbacks must be 7 pointers");
#else
typedef char gwin_clipboard_callbacks_size_check[(sizeof(GwinClipboardCallbacks) == 7 * sizeof(void*)) ? 1 : -1];
#endif

/*
 * ---- GwinDndCallbacks: the four View.notifyDrag* jmethodIDs GlassDnD.cpp dialled through
 * javaIDs.View, and the five WinDnDClipboard members it reached through ClassForName + getInstance ----
 *
 * Every slot: JavaFX application thread, synchronous, re-entrant, nested inside gwin_dnd_push for a
 * drag this application started and with NO Java frame below for a drag from another application.
 * The Java targets of the four drag slots are the protected View.notifyDragEnter / Over / Drop /
 * Leave, reachable only through a WinView-typed reference resolved from view_id; the five others
 * are private members of WinDnDClipboard and must live there.
 */
typedef struct GwinDndCallbacks {
    /*
     * View.notifyDragEnter / notifyDragOver / notifyDragDrop (int,int,int,int,int) -> int, from
     * IDropTarget::DragEnter / DragOver / Drop. x / y are client coordinates (ScreenToClient), x_abs /
     * y_abs the screen POINTL, recommended_action getACTION(like) after the Explorer-style
     * resolution - Ctrl+Shift or Alt -> link, Ctrl -> copy, else move, then the first of copy / move /
     * link the source allows - which stays in C. The target writes the Clipboard.ACTION_* the view
     * chose to *out_action and returns GWIN_OK; the C writes *pdwEffect = getDROPEFFECT(*out_action)
     * BEFORE testing the status, with *out_action left at 0 on GWIN_ERR_UPCALL - so a throwing
     * notifyDragOver leaves DROPEFFECT_NONE in the out-parameter exactly as HotSpot's CallIntMethod
     * returning 0 with a pending exception did - and then returns E_JAVAEXCEPTION from the vtable
     * method (sink: printStackTrace). Before each of the three the C fires
     * dnd_set_source_supported_actions (see there). Not fired at all when the container has no
     * view: DragEnter / DragOver / Drop then return S_OK without touching *pdwEffect, as before.
     */
    int32_t (*drag_enter)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                          int32_t recommended_action, int32_t* out_action);
    int32_t (*drag_over)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                         int32_t recommended_action, int32_t* out_action);
    int32_t (*drag_drop)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
                         int32_t recommended_action, int32_t* out_action);

    /*
     * View.notifyDragLeave() from IDropTarget::DragLeave. The Java method is void and the JNI called
     * it with CallIntMethod and discarded the garbage - the slot returns a status, nothing else. Not
     * fired without a view. GWIN_ERR_UPCALL -> E_JAVAEXCEPTION (sink: printStackTrace).
     */
    int32_t (*drag_leave)(int64_t view_id);

    /*
     * The former GlassDropTarget::UpdateDnDClipboardData: getPtr / setPtr on WinDnDClipboard.getInstance()
     * from DragEnter and Drop, so the DnD clipboard holds the data object being dragged in. The
     * AddRef / Release stays in C and keeps its order: get old; if old != new { if (new) new->AddRef();
     * set(new); if (old) old->Release(); } - two slots, not one exchange, so the unchanged case
     * touches no refcount. Each target calls getInstance() (creating the peer if needed) and reads or
     * writes its handle. GWIN_ERR_UPCALL from either -> E_JAVAEXCEPTION from DragEnter / Drop (sink:
     * printStackTrace); when set fails after get succeeded the C releases the AddRef it just took and
     * leaves the old handle in place.
     */
    int32_t (*dnd_get_data_object)(void** out_data_object);
    int32_t (*dnd_set_data_object)(void* data_object);

    /*
     * WinDnDClipboard.setSourceSupportedActions(int) on getInstance(), from the prologue of every
     * drag_enter / drag_over / drag_drop, AFTER the has-a-view test and BEFORE the keyboard
     * resolution: actions is getACTION(*pdwEffect), the effects the source allows. The C discards the
     * status (it discarded the HRESULT), so a throw here is swallowed and the drag goes on into the
     * notify slot (sink: printStackTrace).
     */
    int32_t (*dnd_set_source_supported_actions)(int32_t actions);

    /*
     * WinDnDClipboard.setDragButton(int) on getInstance(), fired with 0 from the tail of gwin_dnd_push
     * after drag_action_performed; and getDragButton() from the GlassDropSource constructor at the
     * start of the same push - the MouseEvent.BUTTON_* the Java side stored, which the C maps to
     * MK_LBUTTON / RBUTTON / MBUTTON / XBUTTON1 / XBUTTON2 (0 for anything else) to decide when
     * QueryContinueDrag drops. Both read / write the STATIC WinDnDClipboard.dragButton. On
     * GWIN_ERR_UPCALL from the getter the C uses 0 - the JNI never checked, so a throw made the
     * button 0 and the drop immediate - and continues; the setter's status is discarded by its only
     * caller. Sink for both: printStackTrace.
     */
    int32_t (*dnd_set_drag_button)(int32_t button);
    int32_t (*dnd_get_drag_button)(int32_t* out_button);
} GwinDndCallbacks;   /* 9 pointers */

#if defined(__cplusplus)
static_assert(sizeof(GwinDndCallbacks) == 9 * sizeof(void*), "GwinDndCallbacks must be 9 pointers");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinDndCallbacks) == 9 * sizeof(void*), "GwinDndCallbacks must be 9 pointers");
#else
typedef char gwin_dnd_callbacks_size_check[(sizeof(GwinDndCallbacks) == 9 * sizeof(void*)) ? 1 : -1];
#endif

/* sizeof probes, beside gwin_sizeof_view_callbacks: they return the real sizeof and document today's. */
GLASS_WIN_EXPORT int32_t gwin_sizeof_clipboard_callbacks(void);   /* == 7 * sizeof(void*) today */
GLASS_WIN_EXPORT int32_t gwin_sizeof_dnd_callbacks(void);         /* == 9 * sizeof(void*) today */

/*
 * Install (cb == NULL clears) the clipboard resp. drag table. Copied by value, NULL slots replaced by
 * no-ops (see NULL SLOTS), so the caller may free the struct; the stubs in it must outlive the
 * process - Arena.global() - because OLE may deliver a paste for as long as the process owns a
 * clipboard data object and a drag from another application can arrive at any window. Last call
 * wins, no lock: Java installs BOTH once from WinGlassNative's own static initializer, next to
 * NativeLibLoader.loadLibrary - NOT from WinDnDClipboard's: the JNI reached that class through
 * ClassForName(initialize = true) on the first drag, which forced its initializer; a table installed
 * from a lazily loaded peer would never be installed for a drag INTO a process that has not yet
 * started a drag of its own, and every drop from Explorer would silently do nothing. Always GWIN_OK.
 */
GLASS_WIN_EXPORT int32_t gwin_clipboard_set_callbacks(const GwinClipboardCallbacks* cb);
GLASS_WIN_EXPORT int32_t gwin_dnd_set_callbacks(const GwinDndCallbacks* cb);

/*
 * TEST HOOKS, unconditionally exported, never called by production code - gwin_test_fire_callback for
 * the two tables of this section. Each fires slot `slot` of ITS installed table with a fixed argument
 * pattern and returns what the slot returned: 0 for the void slots and for a slot that was installed
 * as NULL, the int32 status of the status slots; GWIN_ERR_INVALID_ARG for an unknown slot, when that
 * table is not installed, or when `out` is NULL for a slot that needs it. Every value is distinct so
 * that a shifted, swapped or truncated parameter is visible on the Java side.
 *
 * gwin_test_fire_clipboard_callback - GwinClipboardCallbacks declaration order, clipboard_id passed
 * through unchanged:
 *     0 fos_serialize(clipboard_id, {0x0041, 0x0042, 0} ("AB", NUL-terminated), 0x100000002 (int64),
 *       &data, &len) - data / len are locals pre-set to NULL / -1. Returns the slot's status when it is
 *       not GWIN_OK; -100 when the slot left *out_data NULL (Java returned null; *out_len is not
 *       consulted); otherwise *out_len. On EVERY path a non-NULL *out_data is released with gwin_free
 *       before returning - the hook owns the block from the moment the slot returned, exactly as
 *       ClipboardData::GetData does, so nothing Java gwin_alloc'd leaks.
 *     1 action_performed(clipboard_id, 1001) - its int32 status comes back
 *     2 drag_action_performed(clipboard_id, 1001)
 *     3 content_changed(clipboard_id)
 *     4 dispose_peer(clipboard_id)
 *     5 set_data_object(clipboard_id, (void*) 0x00007FFE12345678) - bits above 32 set; never
 *       dereferenced by this library
 *     6 data_object_disposed(clipboard_id)
 *   With the PRODUCTION table installed slots 4, 5 and 6 reach WinSystemClipboard's registry and
 *   handle for clipboard_id: 4 disposes the peer, 5 stores the fake pointer as its data object (a
 *   later close() would Release it), 6 decrements its live-object count - fire them at an id the
 *   registry does not hold, or through a recording table.
 *
 * gwin_test_fire_dnd_callback - GwinDndCallbacks declaration order, view_id passed through unchanged.
 * `out` is a caller-owned 8-byte, 8-byte-aligned block handed UNCHANGED to the slots that have an
 * out-parameter - as (int32_t*) to slots 0, 1, 2 and 8 (the target writes 4 bytes) and as (void**)
 * to slot 4 (8 bytes); the test pre-fills it with a sentinel and reads back what its target wrote.
 * Slots 3, 5, 6 and 7 ignore it and accept NULL. Every slot's int32 status comes back.
 *     0 drag_enter(view_id, 1001, 1002, 1003, 1004, 1005, (int32_t*) out)
 *     1 drag_over(view_id, 1001, 1002, 1003, 1004, 1005, (int32_t*) out)
 *     2 drag_drop(view_id, 1001, 1002, 1003, 1004, 1005, (int32_t*) out)
 *     3 drag_leave(view_id)
 *     4 dnd_get_data_object((void**) out)
 *     5 dnd_set_data_object((void*) 0x00007FFE12345678) - never dereferenced by this library
 *     6 dnd_set_source_supported_actions(1001)
 *     7 dnd_set_drag_button(1001)
 *     8 dnd_get_drag_button((int32_t*) out)
 *   With the PRODUCTION table installed slots 5, 6 and 7 WRITE WinDnDClipboard.getInstance()'s state
 *   (5 stores the fake pointer as its data object, which the next drag's Release would dereference;
 *   7 sets the static dragButton to 1001) and slots 0-3 reach View.notifyDrag* of the view view_id
 *   resolves to - fire the writers through a recording table only, or restore the state afterwards.
 */
GLASS_WIN_EXPORT int64_t gwin_test_fire_clipboard_callback(int32_t slot, int64_t clipboard_id);
GLASS_WIN_EXPORT int64_t gwin_test_fire_dnd_callback(int32_t slot, int64_t view_id, void* out);

/*
 * TEST HOOK, unconditionally exported, never called by production code - a string block BUILT BY THIS
 * LIBRARY (GwinMakeStringBlock, the builder behind gwin_clipboard_pop_mimes and gwin_dialog_file) for
 * Java's readStringBlock to parse against a C-built block. Writes a fixed three-string block and its
 * count, in this order:
 *     "alpha"
 *     ""            - empty: the middle string is one bare NUL, which only the count keeps from
 *                     truncating the list (the block is double-NUL-terminated, see MEMORY)
 *     U+03B3 U+1F600 - GREEK SMALL LETTER GAMMA, then GRINNING FACE as the surrogate pair D83D DE00:
 *                     3 UTF-16 code units for 2 code points
 * i.e. the 12 code units 0061 006C 0070 0068 0061 0000 | 0000 | 03B3 D83D DE00 0000 | 0000, and
 * *out_count = 3. The block is owned by the caller from the moment the function returns and is
 * released with gwin_free. Returns GWIN_OK; GWIN_ERR_INVALID_ARG when either pointer is NULL (nothing
 * written) or when the heap is exhausted (*out_block NULL, *out_count 0).
 */
GLASS_WIN_EXPORT int32_t gwin_test_string_block(uint16_t** out_block, int32_t* out_count);

/*
 * The library's allocator, exposed so that a block can cross the boundary in either direction with
 * one owner at a time. gwin_alloc(size) returns a block of at least size bytes (size <= 0 yields a
 * 1-byte block, so an empty array is a real pointer, never NULL) or NULL when the heap is
 * exhausted. gwin_free releases a block obtained from gwin_alloc OR handed out by any gwin_* function
 * below; gwin_free(NULL) is a no-op. Java uses gwin_alloc only inside its fos_serialize target and
 * gwin_free only on the four out-blocks. Both are plain malloc / free of this module's CRT; neither
 * marshals, blocks or upcalls.
 */
GLASS_WIN_EXPORT void* gwin_alloc(int64_t size);
GLASS_WIN_EXPORT void gwin_free(void* block);

/*
 * WinSystemClipboard.create: GlassApplication::RegisterClipboardViewerId, marshalled with
 * SendMessage(WM_DO_ACTION) as ENTER_MAIN_THREAD did. If a clipboard is already registered (a
 * previous peer) it is disposed first through dispose_peer; then clipboard_id is remembered as the
 * target of content_changed
 * and the toolkit window joins the clipboard viewer chain with user32!SetClipboardViewer. Returns
 * GWIN_OK, or GWIN_ERR_NO_TOOLKIT when there is no GlassApplication - the JNI body did nothing then
 * and was void. BLOCKS (SendMessage) and can UPCALL (dispose_peer).
 */
GLASS_WIN_EXPORT int32_t gwin_clipboard_register_viewer(int64_t clipboard_id);

/*
 * WinSystemClipboard.dispose, marshalled: GlassApplication::UnregisterClipboardViewer (leaves the
 * viewer chain, forgets the id) FIRST and unconditionally, then - if clip is non-NULL and
 * OleIsCurrentClipboard(clip) is S_OK - up to 1000 OleFlushClipboard attempts, each retried after
 * OLE_CoPump (a PeekMessage / DispatchMessage drain of THIS thread's queue) while it answers
 * CLIPBRD_E_CANT_OPEN, then clip->Release(). The flush renders every delayed format through
 * fos_serialize, so the data survives the application; the pump makes this RE-ENTRANT and BLOCKING.
 * Java must null its handle afterwards, as WinSystemClipboard.close does - the library does not
 * (it cannot, and the JNI did not). clip == NULL: only the unregister runs. No toolkit: nothing runs.
 */
GLASS_WIN_EXPORT void gwin_clipboard_dispose(gwin_clipboard_t clip);

/*
 * WinSystemClipboard.push(Object[] keys, int supportedActions), marshalled. old is the peer's current
 * handle (Released first if non-NULL - "we need to create a new object here due to the postponed
 * release algorithm in the data provider"), mimes a string block of mime_count mime names in the
 * order Java's Set.toArray() gave them (the order drives FORMATETC insertion), supported_actions a
 * Clipboard.ACTION_* set. Constructs a ClipboardData bound to clipboard_id, PUBLISHES IT THROUGH
 * set_data_object, then pushCommit (registers each mime's CLIPFORMAT for delayed rendering,
 * synthesises the FILEGROUPDESCRIPTORW / FILECONTENTS pair when text/uri-list and
 * text/ie-shortcut-filename are both present and message/external-body is not, stores
 * CFSTR_PREFERREDDROPEFFECT unless supported_actions is ACTION_ANY) and OleSetClipboard. THE HANDLE
 * IS PUBLISHED EVEN WHEN pushCommit OR OleSetClipboard FAIL - the JNI stored it before either could
 * fail and swallowed the HRESULT - so the peer owns a live object either way and the next push or
 * dispose Releases it. Returns GWIN_OK, GWIN_ERR_OLE (an OLE step failed; Java ignores it to stay
 * neutral), or GWIN_ERR_NO_TOOLKIT (nothing ran, nothing published). BLOCKS and UPCALLS
 * (set_data_object always, content_changed from inside OleSetClipboard, fos_serialize if the shell
 * renders eagerly). Requires the clipboard table: without set_data_object the object is unreachable.
 */
GLASS_WIN_EXPORT int32_t gwin_clipboard_push(gwin_clipboard_t old, int64_t clipboard_id,
                                             const uint16_t* mimes, int32_t mime_count,
                                             int32_t supported_actions);

/*
 * WinSystemClipboard.pop, marshalled: old->Release() if non-NULL, then OleGetClipboard. Returns the
 * new handle, or NULL when OleGetClipboard failed - the old object is Released whether or not the
 * new call succeeds and a failure leaves the peer with NO data object, exactly the `NULL != p` the
 * JNI returned as its boolean after storing p. Java stores the return unconditionally. NULL also
 * when there is no toolkit.
 */
GLASS_WIN_EXPORT gwin_clipboard_t gwin_clipboard_pop(gwin_clipboard_t old);

/*
 * WinSystemClipboard.popBytes(String mime, long index), marshalled. mime is the DECODED mime
 * (MimeTypeParser.getMime(), so "message/external-body" without its parameters) and lindex its
 * ";index=" or -1. application/x-java-rawimage goes through OleQueryCreateFromData +
 * OleCreateStaticFromData + IViewObject2::Draw into a 32-bit DIB and comes back as 4-byte
 * big-endian width, height, then BGRA rows; everything else through IDataObject::GetData of the
 * mime's CLIPFORMAT, with a CF_HDROP payload stripped of its DROPFILES header (ANSI file lists are
 * rejected as empty) and CF_UNICODETEXT cut at the first embedded U+0000. On GWIN_OK *out_data is
 * either NULL with *out_len 0 - no data, an empty medium, or an OLE failure, ALL INDISTINGUISHABLE
 * as the JNI's null byte[] was, and Java must keep treating them alike (popFromSystem then tries
 * mime + ";locale" and the file-list fallback) - or a gwin_free-able block of *out_len bytes.
 * clip == NULL -> GWIN_OK with NULL. GWIN_ERR_NO_TOOLKIT -> NULL / 0.
 */
GLASS_WIN_EXPORT int32_t gwin_clipboard_pop_bytes(gwin_clipboard_t clip, const uint16_t* mime,
                                                  int64_t lindex, uint8_t** out_data,
                                                  int32_t* out_len);

/*
 * WinSystemClipboard.popMimesFromSystem, marshalled: EnumFormatEtc over clip, every TYMED_HGLOBAL
 * format mapped to its mime (";cf=<n>" appended when a foreign registration collides with a known
 * name, "cf<n>" for a nameless one), CF_HDROP and CFSTR_INETURLA adding text/uri-list, CF_TEXT
 * adding text/plain, OleQueryCreateFromData == OLE_S_STATIC adding application/x-java-rawimage, and
 * a FILEGROUPDESCRIPTOR(W|A) expanded into one "message/external-body;access-type=clipboard;index=
 * <k>[;size=][;clsid=];name=\"...\"" per item in place of the three ms-stuff/* mimes. *out_mimes is
 * NULL with *out_count 0 when the JNI would have returned null (clip == NULL, or the set came out
 * empty), else a gwin_free-able string block. Set iteration order is UNSPECIFIED
 * (std::unordered_set), as it always was; no test may assert it.
 */
GLASS_WIN_EXPORT int32_t gwin_clipboard_pop_mimes(gwin_clipboard_t clip, uint16_t** out_mimes,
                                                  int32_t* out_count);

/*
 * WinSystemClipboard.pushTargetActionToSystem(int actionDone), marshalled: IDataObject::SetData of
 * CFSTR_PASTESUCCEEDED then CFSTR_PERFORMEDDROPEFFECT on clip, each a DROPEFFECT-sized HGLOBAL
 * holding getDROPEFFECT(action_done). Silent on failure and when clip == NULL, like the JNI.
 */
GLASS_WIN_EXPORT void gwin_clipboard_push_target_action(gwin_clipboard_t clip, int32_t action_done);

/*
 * WinSystemClipboard.popSupportedSourceActions, marshalled. Clipboard.ACTION_NONE (0) when clip is
 * NULL (or there is no toolkit); Clipboard.ACTION_ANY (0x4FFFFFFF) when CFSTR_PREFERREDDROPEFFECT
 * is absent or shorter than a DROPEFFECT; else getACTION of the stored DROPEFFECT.
 */
GLASS_WIN_EXPORT int32_t gwin_clipboard_pop_supported_actions(gwin_clipboard_t clip);

/*
 * WinDnDClipboard.push(Object[] keys, int supportedActions), marshalled. Same arguments and the same
 * first three steps as gwin_clipboard_push (Release old, construct, PUBLISH through set_data_object,
 * pushCommit), then setDragImage (the application/x-java-drag-image or CF_DIB payload becomes the
 * IDragSourceHelper bitmap; failure ignored - "pictured drag is not a primary functionality"), then
 * DoDragDrop with a GlassDropSource whose button comes from dnd_get_drag_button. BLOCKS FOR THE
 * WHOLE DRAG in DoDragDrop's modal loop, during which the drag slots (a self-drag), fos_serialize
 * (the target renders) and dnd_* fire nested inside this call. Before returning it fires, in this
 * order and unconditionally, drag_action_performed(clipboard_id, getACTION(performed drop effect,
 * or DROPEFFECT_NONE on failure)) then dnd_set_drag_button(0). Returns GWIN_OK, GWIN_ERR_OLE
 * (pushCommit or DoDragDrop failed - the handle was published anyway) or GWIN_ERR_NO_TOOLKIT.
 * Requires both tables.
 */
GLASS_WIN_EXPORT int32_t gwin_dnd_push(gwin_clipboard_t old, int64_t clipboard_id,
                                       const uint16_t* mimes, int32_t mime_count,
                                       int32_t supported_actions);

/*
 * WinDnDClipboard.dispose, marshalled: clip->Release() and nothing else. It does NOT unregister a
 * clipboard viewer and does NOT flush - WinDnDClipboard overrides create() to a no-op and has its
 * own dispose, and that asymmetry is deliberate. clip == NULL: nothing.
 */
GLASS_WIN_EXPORT void gwin_dnd_dispose(gwin_clipboard_t clip);

/*
 * One CommonDialogs.ExtensionFilter, flattened by Java: description is getDescription(),
 * extensions the extensions joined with ';' - no leading, no trailing semicolon - which is the join
 * the C did with ConcatJStrings resp. DNTString::append. Both NUL-terminated UTF-16 and NEVER NULL:
 * a null description would have crashed JString, so Java substitutes "" (a hardening, not a
 * behaviour change). Two pointers; verify against gwin_sizeof_file_filter.
 */
typedef struct GwinFileFilter {
    const uint16_t* description;
    const uint16_t* extensions;
} GwinFileFilter;

#if defined(__cplusplus)
static_assert(sizeof(GwinFileFilter) == 2 * sizeof(void*), "GwinFileFilter must be 2 pointers");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinFileFilter) == 2 * sizeof(void*), "GwinFileFilter must be 2 pointers");
#else
typedef char gwin_file_filter_size_check[(sizeof(GwinFileFilter) == 2 * sizeof(void*)) ? 1 : -1];
#endif

GLASS_WIN_EXPORT int32_t gwin_sizeof_file_filter(void);           /* == 2 * sizeof(void*) today */

/*
 * What gwin_dialog_file / gwin_dialog_folder return. INFORMATIONAL: the JNI could not tell the three
 * apart at its Java boundary (see the out-parameters below), so Java keys on *out_files /
 * *out_path and ignores this to stay neutral; it is reported so a later commit can act on it.
 */
enum GwinDialogStatus {
    GWIN_DIALOG_OK        = 0,   /* the dialog ran and something was chosen                     */
    GWIN_DIALOG_CANCELLED = 1,   /* the dialog ran and was dismissed (ERROR_CANCELLED / FALSE)  */
    GWIN_DIALOG_FAILED    = 2    /* the dialog could not run, or its result could not be read  */
};

/*
 * WinCommonDialogs._showFileChooser. NOT marshalled - the dialog runs on the calling thread, which
 * must be the toolkit thread. owner is Window.getNativeWindow() or NULL; while the call lasts the
 * owning BaseWnd is marked a common-dialog owner (CommonDialogOwner RAII, BaseWnd::FromHandle - it
 * stays in C for that reason). folder, filename and title are NEVER NULL, possibly "" (the JNI
 * always allocated a JString, so IFileDialog::SetFolder / SetTitle always ran - SetTitle(L"") gives
 * an empty title bar, and a NULL here would give the shell default instead); filename is applied
 * only to a SAVE dialog and only when non-empty. type is CommonDialogs.Type.OPEN (0) / SAVE (1),
 * multiple 0 / 1 (OPEN only: FOS_ALLOWMULTISELECT / OFN_ALLOWMULTISELECT).
 * filters == NULL skips the filter setup entirely, exactly as jFilters == NULL did. filters != NULL
 * with filter_count == 0 does NOT: the Vista path still calls SetDefaultExtension(L"") and
 * SetFileTypes(0, ...) (and skips only SetFileTypeIndex), which is what stops the shell appending
 * an extension to a typed SAVE name - a javafx.stage.FileChooser without filters hands down an
 * empty array, so Java must pass a non-NULL pointer for an empty array. default_filter_index is
 * 0-based; the C adds the 1.
 * BLOCKS in a nested modal loop that re-enters the message pump; WndProc-driven slots of every
 * section keep firing on this thread meanwhile. The Vista path: CoCreateInstance(CLSID_FileOpenDialog
 * / CLSID_FileSaveDialog), SHCreateItemFromParsingName(folder), IFileDialog::Show, then GetResult /
 * IFileOpenDialog::GetResults and GetFileTypeIndex - the last one OUTSIDE any try block on a
 * possibly-NULL IFileDialogPtr, as it always was (a pre-existing defect: the export's catch turns
 * the _com_error into GWIN_DIALOG_FAILED where the JNI entry point terminated the process). Below
 * Vista: comdlg32!GetOpenFileNameW / GetSaveFileNameW with the buffer-growing hook, or
 * shell32!SHBrowseForFolder.
 * On return *out_files is NULL - the result could not be read (the JNI fed a null String[] to
 * createFileChooserResult, which threw NullPointerException, which was cleared, so
 * _showFileChooser returned NULL: Java must return null then) - or a gwin_free-able string block
 * of *out_count full paths: 0 of them when the user cancelled (the JNI's empty String[]), and for
 * a multi-select on the legacy path assembled from the directory and each name exactly as
 * ConvertFiles did. A SAVE dialog whose GetResult failed yields 0 paths (the JNI produced a
 * one-element array holding null, which createFileChooserResult skipped - same result).
 * *out_filter_index is (the 1-based selected index) - 1: GetFileTypeIndex's UINT starts at 0, so a
 * failed query yields -1 (and createFileChooserResult maps any index < 0 to no filter); the legacy
 * path gives ofn.nFilterIndex - 1. Java passes it through UNTOUCHED - no clamping.
 */
GLASS_WIN_EXPORT int32_t gwin_dialog_file(void* owner, const uint16_t* folder,
                                          const uint16_t* filename, const uint16_t* title,
                                          int32_t type, int32_t multiple,
                                          const GwinFileFilter* filters, int32_t filter_count,
                                          int32_t default_filter_index,
                                          uint16_t** out_files, int32_t* out_count,
                                          int32_t* out_filter_index);

/*
 * WinCommonDialogs._showFolderChooser. Same thread, owner, never-NULL string and blocking rules as
 * gwin_dialog_file. Vista path: IFileOpenDialog with FOS_PICKFOLDERS | FOS_FORCEFILESYSTEM; legacy:
 * SHBrowseForFolder with BIF_USENEWUI, the initial selection set from BFFM_INITIALIZED, and the
 * chosen PIDL resolved through its shell link (a folder shortcut yields its target). *out_path is
 * NULL when the user cancelled or the path could not be resolved - both produced a null jstring and
 * therefore a null File - else a gwin_free-able NUL-terminated UTF-16 path.
 */
GLASS_WIN_EXPORT int32_t gwin_dialog_folder(void* owner, const uint16_t* folder,
                                            const uint16_t* title, uint16_t** out_path);

/*
 * ---- WinAccessible + WinTextRangeProvider: the two UI Automation provider objects ----
 *
 * The providers are GlassAccessible (GlassAccessible.cpp, 19 COM interfaces) and
 * GlassTextRangeProvider (GlassTextRangeProvider.cpp, ITextRangeProvider). They are the INBOUND edge
 * of UI Automation: UIAutomationCore hands a GlassAccessible to a client through
 * UiaReturnRawElementProvider (ViewContainer.cpp, the WM_GETOBJECT reply) and Windows then invokes
 * their vtables. That is why they stay native - a Java-synthesised COM object would need 19 hand-built
 * vtables, 87 entries, the IIDs, and refcounts other processes hold references into, none of which any
 * test in this tree can drive. What moves to Java is only the 87 upcalls their bodies made: each former
 * jmethodID is one slot of GwinAccessibleCallbacks resp. GwinTextRangeCallbacks below, and the four
 * entry points that owned the objects' lifetime become gwin_a11y_create / gwin_a11y_destroy /
 * gwin_a11y_text_range_create / gwin_a11y_text_range_destroy.
 *
 * NOT exported, deliberately: WinAccessible.UiaRaiseAutomationEvent and
 * WinAccessible.UiaClientsAreListening are WRAPPERs - one cast plus one delay-loaded UIAutomationCore
 * call - so Java binds UIAutomationCore!UiaRaiseAutomationEvent and !UiaClientsAreListening itself, as
 * it binds shlwapi!AssocQueryStringW. A C wrapper around an OS call is the anti-pattern this migration
 * exists to remove. UiaRaiseAutomationPropertyChangedEvent is the exception and DOES get an export
 * (gwin_a11y_raise_property_changed): it takes its two VARIANTs BY VALUE, 24 bytes each, and nothing in
 * this ABI pins the VARIANT union's layout - unlike GwinVariant, whose size and eleven field offsets
 * gwin_test_variant_offsets pins one by one against the C compiler - so a VARIANT built in Java and
 * passed by value would be unverifiable. That building one needs oleaut32!SysAllocStringLen,
 * SafeArrayCreateVector, SafeArrayPutElement and IUnknown::AddRef is NOT on its own a reason: this
 * module already calls oleaut32 and COM vtables from Java (com.sun.javafx.font.directwrite.DWNative).
 * It is, however, the same marshalling GetPropertyValue / GetAttributeValue need in C anyway for the
 * inbound direction. One export is smaller and checkable; the same code in two languages is neither.
 *
 * IDENTITY. Every slot's first parameter is an int64_t id Java assigned to the WinAccessible resp. the
 * WinTextRangeProvider and handed to gwin_a11y_create / gwin_a11y_text_range_create. The library never
 * dereferences it and holds no Java reference - GwinViewCallbacks' view_id, exactly. It is NOT
 * WinAccessible.id, which is part of the UIA runtime id a client can see. Java must never pass 0: 0 is
 * this library's "no Java peer" value (see the sibling-range guard of compare / compare_endpoints /
 * move_endpoint_by_range). THE ID MUST STAY VALID UNTIL accessible_disposed / range_disposed, WHICH IS
 * NOT WinAccessible.dispose(): the COM object routinely outlives the Java peer's dispose() - UIA holds
 * its own references - and provider methods keep arriving until the last Release. Those two slots stand
 * exactly where DeleteGlobalRef stood in ~GlassAccessible / ~GlassTextRangeProvider
 * (GwinWindowCallbacks.notify_dispose is the precedent) and are the only place a registry entry may be
 * dropped. The handles gwin_a11y_create / gwin_a11y_text_range_create return, and every int64_t a slot
 * hands back or takes for ANOTHER provider, are raw C++ object pointers, not ids: an accessible handle
 * is also its IRawElementProviderSimple* (the first base - ViewContainer.cpp reinterpret-casts it into
 * the WM_GETOBJECT reply, and gwin_a11y_raise_property_changed does the same) and a range handle is its
 * ITextRangeProvider*.
 *
 * THREAD. get_ProviderOptions answers ProviderOptions_ServerSideProvider | ProviderOptions_UseComThreading,
 * so UIA marshals provider calls into the apartment the provider was handed out from: the OLE STA that
 * gwin_run_loop entered, i.e. the JavaFX application thread, delivered by its message pump from inside
 * the gwin_run_loop downcall. TWO DOCUMENTED EXCEPTIONS, both on IRawElementProviderAdviseEvents:
 * Windows calls AdviseEventRemoved on another thread while Narrator shuts down (the comment in
 * GlassAccessible::AdviseEventAdded says so), and either destructor runs on whichever thread drops the
 * last reference, which for a UIA-held reference is a COM/RPC thread. BEHAVIOUR DIFFERENCE, stated
 * rather than hidden: the JNI arm answered E_FAIL on such a thread, because GetEnv() returns NULL for a
 * thread the JVM never saw and the JNI bodies gave up rather than attaching. A slot dialled through an
 * FFM upcall stub does NOT give up - the JVM attaches the calling thread and runs the Java target - so
 * after the flip those calls REACH Java instead of failing. Measured on JDK 26 and JDK 25, with a
 * plain CreateThread thread, an MTA CoInitializeEx thread and a Win32 thread-pool thread: the upcall
 * runs, the thread appears as a daemon Thread in the main ThreadGroup WITH A NULL CONTEXT CLASS
 * LOADER, its return value arrives intact, and the thread stays attached until it exits. That
 * attachment is permanent for the life of the thread and is ACCEPTED: the RPC pool is bounded, the four
 * slots that can arrive there (advise_event_added, advise_event_removed, accessible_disposed,
 * range_disposed) touch nothing but the id registries, and the cost is one Thread object per RPC thread
 * that ever entered a provider. REACHING Java is not the same as succeeding, and the sentence above is
 * scoped to those four slots: a provider method that reaches a REGISTERED peer off the JavaFX
 * application thread still ends in E_FAIL, one layer further in - WinAccessible.getNativeAccessible()
 * calls Application.checkEventThread(), the Java stub reports the IllegalStateException and answers
 * GWIN_ERR_UPCALL, which is the E_FAIL CheckAndClearException produced for the same call in commit
 * 033187ad90. An out-of-process UIA client's RPC thread could not be probed here and remains
 * unverified. Keeping the old behaviour would need the toolkit thread id recorded at gwin_run_loop
 * entry and an E_FAIL for every other thread; that is a decision to take deliberately, not a side
 * effect.
 *
 * EXCEPTIONS. No Throwable may escape a slot into a COM vtable. Every slot returns int32_t GwinStatus
 * (even the ones whose Java target is void, because the JNI turned a pending Throwable into E_FAIL for
 * those too): GWIN_OK means the out-parameters are written, GWIN_ERR_UPCALL means the target threw and
 * the Java stub has already reported it through Application.reportException - where
 * CheckAndClearException sent it - and the provider method then returns the E_FAIL the JNI returned.
 * A slot that reports GWIN_ERR_UPCALL must still leave its out-parameters as CallXxxMethod left them
 * (zero), because the C wrote *pRetVal BEFORE it checked for the exception; this library pre-zeroes
 * every out-parameter it passes, so a stub that writes nothing produces exactly that.
 *
 * NULL SLOTS. Both installers copy the struct by value and replace every NULL slot with an internal
 * no-op that returns GWIN_OK with zeroed out-parameters, so no upcall site tests a slot. What the sites
 * test is whether a table was installed at all: while this section was additive, no table meant the JNI
 * arm each site still had; with the JNI deleted it means the E_FAIL that arm answered without a JNIEnv.
 *
 * ABI VERSION. These exports and both tables landed additive under GLASS_WIN_ABI_VERSION 5: nothing
 * bound by WinGlassNative changed its prototype, layout or value, and a bump on its own would break the
 * facade immediately - it compares gwin_abi_version() with its own ABI_VERSION for exact equality in
 * both directions. GLASS_WIN_ABI_VERSION WENT 5 -> 6 IN THE CHANGE SET THAT FLIPPED WinAccessible.java
 * and WinTextRangeProvider.java ONTO THESE TABLES, together with WinGlassNative.ABI_VERSION 5 -> 6 -
 * the same pairing the view, window and clipboard sections took at their flips. THAT BUMP WAS TAKEN
 * WITH THIS SECTION'S FLIP, and the pairing is what it protects: a glass.dll without the deleted Java_*
 * arms now refuses to link against a Java side that installs no table, instead of meeting it and
 * answering E_FAIL everywhere.
 *
 * critical(true) IS FORBIDDEN on every export of this section: gwin_a11y_raise_property_changed calls
 * across an apartment into UIAutomationCore and can block; gwin_a11y_destroy and
 * gwin_a11y_text_range_destroy run a destructor that dials a callback slot and therefore re-enters the
 * JVM; the two create functions and the two installers publish pointers another thread will dial.
 */

/* An opaque GlassAccessible*: what gwin_a11y_create returns, and also its IRawElementProviderSimple*. */
typedef void* gwin_accessible_t;

/* An opaque GlassTextRangeProvider*: what gwin_a11y_text_range_create returns, and also its
 * ITextRangeProvider*. */
typedef void* gwin_text_range_t;

/*
 * ---- GwinVariant: the flat form of com.sun.glass.ui.win.WinVariant ----
 *
 * The nine fields GlassAccessible::copyVariant read through cached jfieldIDs, plus the two lengths a
 * flat ABI needs. vt is the VARTYPE that selects the live field; a vt this library does not know
 * leaves the VARIANT it builds with that vt and no value, exactly as the JNI's switch did. Only VT_I4,
 * VT_BSTR, VT_BOOL, VT_R8, VT_R8 | VT_ARRAY and VT_UNKNOWN are produced by Java today; VT_I2 and VT_R4
 * are carried because the C has always handled them.
 *
 * A NULL bstr_val / p_dbl_val is Java's null and is DISTINCT from a zero-length block: the JNI's
 * copyString(NULL) / copyList(NULL) answered E_FAIL while an empty String or double[] answered S_OK,
 * and that difference reaches the UIA client. bstr_len / p_dbl_count are read only when their pointer
 * is non-NULL.
 *
 * OWNERSHIP, by direction:
 *   - Filled BY JAVA into an out-parameter (get_property_value, get_attribute_value): the two blocks
 *     are gwin_alloc-ed by the Java stub and THIS LIBRARY TAKES OWNERSHIP, releasing them with
 *     gwin_free once copied into the BSTR / SAFEARRAY, on every path including its own failures -
 *     GwinClipboardCallbacks.fos_serialize's rule, for the same reason: the stub has returned and can
 *     free nothing.
 *   - Passed BY JAVA into a downcall (gwin_a11y_raise_property_changed): the CALLER owns the struct
 *     and both blocks for the duration of the call and frees nothing of this library's; the call is
 *     synchronous, so a confined Arena is enough.
 *   - Passed BY THIS LIBRARY into a slot (find_attribute): this library owns it. It is NULL today,
 *     reproducing the jobject jVal = NULL of GlassTextRangeProvider::FindAttribute ("//TODO VAL TO
 *     JVAL"); that defect is carried verbatim, not fixed in a migration.
 *
 * The layout is natural x64 alignment and has three padding holes. DO NOT HARD-CODE ITS SIZE OR ITS
 * OFFSETS FROM THIS COMMENT: gwin_sizeof_variant() and gwin_test_variant_offsets() report them.
 */
typedef struct GwinVariant {
    int16_t         vt;           /* VARTYPE; VT_EMPTY (0) when Java had nothing                */
    int16_t         i_val;        /* VT_I2                                                      */
    int32_t         l_val;        /* VT_I4                                                      */
    float           flt_val;      /* VT_R4                                                      */
    double          dbl_val;      /* VT_R8                                                      */
    int32_t         bool_val;     /* VT_BOOL: 0 / 1; the C writes VARIANT_FALSE / VARIANT_TRUE  */
    int64_t         punk_val;     /* VT_UNKNOWN: an IUnknown*, AddRefed by the C; 0 -> E_FAIL   */
    const uint16_t* bstr_val;     /* VT_BSTR: UTF-16 code units, not NUL-terminated; NULL= null */
    int32_t         bstr_len;     /* code units in bstr_val                                     */
    const double*   p_dbl_val;    /* VT_R8 | VT_ARRAY; NULL = null                              */
    int32_t         p_dbl_count;  /* doubles in p_dbl_val                                       */
} GwinVariant;

/*
 * ---- GwinAccessibleCallbacks: the 69 jmethodIDs of WinAccessible._initIDs, plus one disposal ----
 *
 * Declaration order IS the _initIDs order, which is the UIA interface order of GlassAccessible.cpp;
 * it is also the slot numbering gwin_test_fire_accessible_callback uses. The Java target of each slot
 * is the WinAccessible method named in its comment - the names are the UIA ones and keep their
 * original capitalisation on the Java side. Every slot: JavaFX application thread except where the
 * section comment says otherwise, synchronous, re-entrant (a slot may dispose the provider before it
 * returns), and int32_t 0 / 1 for booleans.
 *
 * Blocks handed BACK by a slot (int64_t** / int32_t** / uint16_t** out-parameters) are gwin_alloc-ed
 * by the Java stub; this library takes ownership and gwin_frees them once copied into the SAFEARRAY or
 * BSTR, on every path. A NULL block is Java's null: copyList(NULL) and copyString(NULL) answered
 * E_FAIL, so the provider method does too.
 */
typedef struct GwinAccessibleCallbacks {
    /* ---- IRawElementProviderSimple ---- */

    /* GetPatternProvider(int)->long. *out is a GlassAccessible*, ADDREFED by this library. */
    int32_t (*get_pattern_provider)(int64_t accessible_id, int32_t pattern_id, int64_t* out);

    /* get_HostRawElementProvider()->long: an HWND, or 0 for a "lightweight" accessible. It is fed to
     * UiaHostProviderFromHwnd, WHOSE HRESULT IS DELIBERATELY IGNORED (E_INVALIDARG for a NULL hwnd
     * would break accessibility on Windows 7 - the comment in get_HostRawElementProvider). */
    int32_t (*get_host_raw_element_provider)(int64_t accessible_id, int64_t* out_hwnd);

    /* GetPropertyValue(int)->WinVariant. Java's null is GWIN_OK with out->vt = VT_EMPTY, which this
     * library turns into E_FAIL with VT_EMPTY, as copyVariant(NULL) did; GWIN_ERR_UPCALL leaves the
     * caller's VARIANT UNTOUCHED, as the JNI's early return did. The two paths differ and a UIA client
     * can tell them apart. */
    int32_t (*get_property_value)(int64_t accessible_id, int32_t property_id, GwinVariant* out);

    /* ---- IRawElementProviderFragment ---- */

    /* get_BoundingRectangle()->float[4] = left, top, width, height. *out_written is 0 when Java
     * answered null, and the provider then returns S_OK with the UiaRect UNTOUCHED - what the JNI did
     * when the jfloatArray was null. Write all four floats or none. */
    int32_t (*get_bounding_rectangle)(int64_t accessible_id, float* out4, int32_t* out_written);

    /* get_FragmentRoot()->long: a GlassAccessible*, ADDREFED. */
    int32_t (*get_fragment_root)(int64_t accessible_id, int64_t* out);

    /* GetEmbeddedFragmentRoots()->long[]: GlassAccessible*s, into a VT_UNKNOWN SAFEARRAY whose
     * SafeArrayPutElement AddRefs each element. */
    int32_t (*get_embedded_fragment_roots)(int64_t accessible_id, int64_t** out, int32_t* out_count);

    /* GetRuntimeId()->int[]: into a VT_I4 SAFEARRAY. */
    int32_t (*get_runtime_id)(int64_t accessible_id, int32_t** out, int32_t* out_count);

    /* Navigate(int)->long: a NavigateDirection in, a GlassAccessible* out, ADDREFED. */
    int32_t (*navigate)(int64_t accessible_id, int32_t direction, int64_t* out);

    /* SetFocus(). */
    int32_t (*set_focus)(int64_t accessible_id);

    /* ---- IRawElementProviderFragmentRoot ---- */

    /* ElementProviderFromPoint(double,double)->long: a GlassAccessible*, ADDREFED. */
    int32_t (*element_provider_from_point)(int64_t accessible_id, double x, double y, int64_t* out);

    /* GetFocus()->long: a GlassAccessible*, ADDREFED. */
    int32_t (*get_focus)(int64_t accessible_id, int64_t* out);

    /* ---- IRawElementProviderAdviseEvents ---- *
     * THE TWO SLOTS DOCUMENTED TO ARRIVE ON A COM/RPC THREAD (Narrator shutdown); see THREAD above.
     * property_ids is the raw SAFEARRAY* UIA passed, which the Java body ignores. */

    /* AdviseEventAdded(int,long). */
    int32_t (*advise_event_added)(int64_t accessible_id, int32_t event_id, int64_t property_ids);

    /* AdviseEventRemoved(int,long). */
    int32_t (*advise_event_removed)(int64_t accessible_id, int32_t event_id, int64_t property_ids);

    /* ---- IInvokeProvider ---- */

    /* Invoke(). */
    int32_t (*invoke)(int64_t accessible_id);

    /* ---- ISelectionProvider ---- */

    /* GetSelection()->long[]: GlassAccessible*s, VT_UNKNOWN. */
    int32_t (*get_selection)(int64_t accessible_id, int64_t** out, int32_t* out_count);

    /* get_CanSelectMultiple()->boolean. */
    int32_t (*get_can_select_multiple)(int64_t accessible_id, int32_t* out);

    /* get_IsSelectionRequired()->boolean. */
    int32_t (*get_is_selection_required)(int64_t accessible_id, int32_t* out);

    /* ---- ISelectionItemProvider ---- */

    /* Select(). */
    int32_t (*select)(int64_t accessible_id);

    /* AddToSelection(). */
    int32_t (*add_to_selection)(int64_t accessible_id);

    /* RemoveFromSelection(). */
    int32_t (*remove_from_selection)(int64_t accessible_id);

    /* get_IsSelected()->boolean. */
    int32_t (*get_is_selected)(int64_t accessible_id, int32_t* out);

    /* get_SelectionContainer()->long: a GlassAccessible*, ADDREFED. */
    int32_t (*get_selection_container)(int64_t accessible_id, int64_t* out);

    /* ---- IRangeValueProvider ---- */

    /* SetValue(double). */
    int32_t (*set_value)(int64_t accessible_id, double value);

    /* get_Value()->double. */
    int32_t (*get_value)(int64_t accessible_id, double* out);

    /* get_IsReadOnly()->boolean. */
    int32_t (*get_is_read_only)(int64_t accessible_id, int32_t* out);

    /* get_Maximum()->double. */
    int32_t (*get_maximum)(int64_t accessible_id, double* out);

    /* get_Minimum()->double. */
    int32_t (*get_minimum)(int64_t accessible_id, double* out);

    /* get_LargeChange()->double. */
    int32_t (*get_large_change)(int64_t accessible_id, double* out);

    /* get_SmallChange()->double. */
    int32_t (*get_small_change)(int64_t accessible_id, double* out);

    /* ---- IValueProvider ---- */

    /* SetValueString(String): UTF-16 code units, NOT NUL-terminated, borrowed for the call. The
     * provider method still returns S_OK for a NULL BSTR without dialling the slot, as it did. */
    int32_t (*set_value_string)(int64_t accessible_id, const uint16_t* text, int32_t len);

    /* get_ValueString()->String: a gwin_alloc-ed block this library frees; NULL = null -> E_FAIL. */
    int32_t (*get_value_string)(int64_t accessible_id, uint16_t** out, int32_t* out_len);

    /* ---- ITextProvider ---- */

    /* GetVisibleRanges()->long[]: GlassTextRangeProvider*s, VT_UNKNOWN. */
    int32_t (*get_visible_ranges)(int64_t accessible_id, int64_t** out, int32_t* out_count);

    /* RangeFromChild(long)->long. child_element is the raw IRawElementProviderSimple* UIA passed.
     * *out is a GlassTextRangeProvider* and is NOT AddRefed: JavaFX returns a fresh range each time and
     * the COM caller owns its only reference (the commented-out AddRef in RangeFromChild). */
    int32_t (*range_from_child)(int64_t accessible_id, int64_t child_element, int64_t* out);

    /* RangeFromPoint(double,double)->long: a GlassTextRangeProvider*, NOT AddRefed. */
    int32_t (*range_from_point)(int64_t accessible_id, double x, double y, int64_t* out);

    /* get_DocumentRange()->long: a GlassTextRangeProvider*, and THE ONE RANGE GETTER THAT ADDREFS -
     * WinAccessible caches its document range. Keep the asymmetry. */
    int32_t (*get_document_range)(int64_t accessible_id, int64_t* out);

    /* get_SupportedTextSelection()->int: a SupportedTextSelection. */
    int32_t (*get_supported_text_selection)(int64_t accessible_id, int32_t* out);

    /* ---- IGridProvider ---- */

    /* get_ColumnCount()->int. */
    int32_t (*get_column_count)(int64_t accessible_id, int32_t* out);

    /* get_RowCount()->int. */
    int32_t (*get_row_count)(int64_t accessible_id, int32_t* out);

    /* GetItem(int,int)->long: row then column; a GlassAccessible*, ADDREFED. */
    int32_t (*get_item)(int64_t accessible_id, int32_t row, int32_t column, int64_t* out);

    /* ---- IGridItemProvider ---- */

    /* get_Column()->int. */
    int32_t (*get_column)(int64_t accessible_id, int32_t* out);

    /* get_ColumnSpan()->int. */
    int32_t (*get_column_span)(int64_t accessible_id, int32_t* out);

    /* get_ContainingGrid()->long: a GlassAccessible*, ADDREFED. */
    int32_t (*get_containing_grid)(int64_t accessible_id, int64_t* out);

    /* get_Row()->int. */
    int32_t (*get_row)(int64_t accessible_id, int32_t* out);

    /* get_RowSpan()->int. */
    int32_t (*get_row_span)(int64_t accessible_id, int32_t* out);

    /* ---- ITableProvider ---- */

    /* GetColumnHeaders()->long[]: GlassAccessible*s, VT_UNKNOWN. */
    int32_t (*get_column_headers)(int64_t accessible_id, int64_t** out, int32_t* out_count);

    /* GetRowHeaders()->long[]: GlassAccessible*s, VT_UNKNOWN. */
    int32_t (*get_row_headers)(int64_t accessible_id, int64_t** out, int32_t* out_count);

    /* get_RowOrColumnMajor()->int: a RowOrColumnMajor. */
    int32_t (*get_row_or_column_major)(int64_t accessible_id, int32_t* out);

    /* ---- ITableItemProvider ---- */

    /* GetColumnHeaderItems()->long[]: GlassAccessible*s, VT_UNKNOWN. */
    int32_t (*get_column_header_items)(int64_t accessible_id, int64_t** out, int32_t* out_count);

    /* GetRowHeaderItems()->long[]: GlassAccessible*s, VT_UNKNOWN. */
    int32_t (*get_row_header_items)(int64_t accessible_id, int64_t** out, int32_t* out_count);

    /* ---- IToggleProvider ---- */

    /* Toggle(). */
    int32_t (*toggle)(int64_t accessible_id);

    /* get_ToggleState()->int: a ToggleState. */
    int32_t (*get_toggle_state)(int64_t accessible_id, int32_t* out);

    /* ---- IExpandCollapseProvider ---- */

    /* Collapse(). */
    int32_t (*collapse)(int64_t accessible_id);

    /* Expand(). */
    int32_t (*expand)(int64_t accessible_id);

    /* get_ExpandCollapseState()->int: an ExpandCollapseState. */
    int32_t (*get_expand_collapse_state)(int64_t accessible_id, int32_t* out);

    /* ---- ITransformProvider ---- */

    /* get_CanMove()->boolean. */
    int32_t (*get_can_move)(int64_t accessible_id, int32_t* out);

    /* get_CanResize()->boolean. */
    int32_t (*get_can_resize)(int64_t accessible_id, int32_t* out);

    /* get_CanRotate()->boolean. */
    int32_t (*get_can_rotate)(int64_t accessible_id, int32_t* out);

    /* Move(double,double). */
    int32_t (*move)(int64_t accessible_id, double x, double y);

    /* Resize(double,double). */
    int32_t (*resize)(int64_t accessible_id, double width, double height);

    /* Rotate(double). */
    int32_t (*rotate)(int64_t accessible_id, double degrees);

    /* ---- IScrollProvider ---- */

    /* Scroll(int,int): two ScrollAmounts. */
    int32_t (*scroll)(int64_t accessible_id, int32_t horizontal_amount, int32_t vertical_amount);

    /* SetScrollPercent(double,double). */
    int32_t (*set_scroll_percent)(int64_t accessible_id, double horizontal_percent,
                                  double vertical_percent);

    /* get_HorizontallyScrollable()->boolean. */
    int32_t (*get_horizontally_scrollable)(int64_t accessible_id, int32_t* out);

    /* get_HorizontalScrollPercent()->double. */
    int32_t (*get_horizontal_scroll_percent)(int64_t accessible_id, double* out);

    /* get_HorizontalViewSize()->double. */
    int32_t (*get_horizontal_view_size)(int64_t accessible_id, double* out);

    /* get_VerticallyScrollable()->boolean. */
    int32_t (*get_vertically_scrollable)(int64_t accessible_id, int32_t* out);

    /* get_VerticalScrollPercent()->double. */
    int32_t (*get_vertical_scroll_percent)(int64_t accessible_id, double* out);

    /* get_VerticalViewSize()->double. */
    int32_t (*get_vertical_view_size)(int64_t accessible_id, double* out);

    /* ---- IScrollItemProvider ---- */

    /* ScrollIntoView(). */
    int32_t (*scroll_into_view)(int64_t accessible_id);

    /* ---- Lifetime ---- *
     * No jmethodID behind this one: it fires from ~GlassAccessible, exactly where DeleteGlobalRef
     * stood, i.e. when the LAST COM reference went - not at WinAccessible.dispose(). It is the only
     * place Java may drop the registry entry for accessible_id (see IDENTITY), and it can arrive on
     * the COM/RPC thread that dropped that reference. void, and it must not throw. */
    void (*accessible_disposed)(int64_t accessible_id);
} GwinAccessibleCallbacks;   /* 70 pointers */

#if defined(__cplusplus)
static_assert(sizeof(GwinAccessibleCallbacks) == 70 * sizeof(void*),
              "GwinAccessibleCallbacks must be 70 pointers");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinAccessibleCallbacks) == 70 * sizeof(void*),
               "GwinAccessibleCallbacks must be 70 pointers");
#else
typedef char gwin_accessible_callbacks_size_check[(sizeof(GwinAccessibleCallbacks)
                                                   == 70 * sizeof(void*)) ? 1 : -1];
#endif

/*
 * ---- GwinTextRangeCallbacks: the 18 jmethodIDs of WinTextRangeProvider._initIDs, plus one disposal ----
 *
 * Declaration order IS the _initIDs order and the slot numbering of
 * gwin_test_fire_text_range_callback. Same thread, NULL-slot, ownership and exception rules as
 * GwinAccessibleCallbacks.
 *
 * THE THREE SIBLING-RANGE SLOTS (compare, compare_endpoints, move_endpoint_by_range) carry the id of
 * ANOTHER range. This library keeps the guard it had: when UIA passes a NULL range, or one with no
 * Java peer (id 0), it answers *pRetVal = FALSE resp. nothing, S_OK, WITHOUT dialling the slot. It
 * still reinterpret-casts the ITextRangeProvider* it was given WITHOUT a QueryInterface check, so a
 * foreign range would be type-confused - pre-existing, carried verbatim, not fixed here.
 */
typedef struct GwinTextRangeCallbacks {
    /* Clone()->long: a GlassTextRangeProvider*, NOT AddRefed (the caller owns the only reference). */
    int32_t (*clone)(int64_t range_id, int64_t* out);

    /* Compare(WinTextRangeProvider)->boolean. */
    int32_t (*compare)(int64_t range_id, int64_t other_range_id, int32_t* out);

    /* CompareEndpoints(int,WinTextRangeProvider,int)->int. */
    int32_t (*compare_endpoints)(int64_t range_id, int32_t endpoint, int64_t other_range_id,
                                 int32_t target_endpoint, int32_t* out);

    /* ExpandToEnclosingUnit(int): a TextUnit. */
    int32_t (*expand_to_enclosing_unit)(int64_t range_id, int32_t unit);

    /* FindAttribute(int,WinVariant,boolean)->long. val IS ALWAYS NULL - the C never converted the
     * VARIANT UIA passed ("//TODO VAL TO JVAL"), and a migration carries that verbatim. *out is a
     * GlassTextRangeProvider*, NOT AddRefed. backward is UIA's BOOL widened - see the BOOL note on
     * find_text. */
    int32_t (*find_attribute)(int64_t range_id, int32_t attribute_id, const GwinVariant* val,
                              int32_t backward, int64_t* out);

    /* FindText(String,boolean,boolean)->long. text is the BSTR's SysStringLen code units, borrowed
     * for the call and not NUL-terminated. *out is a GlassTextRangeProvider*, NOT AddRefed.
     * BOOL, the note for backward / ignore_case here and for find_attribute's backward and
     * scroll_into_view's align_to_top: the BOOL crosses as the full int32_t and the Java stub tests
     * != 0; the JNI of commit 033187ad90 passed a jboolean through CallVoidMethod varargs, where only
     * the low byte is read, so a non-canonical BOOL such as 0x100 was false under JNI and is true here.
     * UIA canonicalises BOOL to 0/1 and only find_text's ignore_case has its value used, so no input
     * can distinguish the two. */
    int32_t (*find_text)(int64_t range_id, const uint16_t* text, int32_t len, int32_t backward,
                         int32_t ignore_case, int64_t* out);

    /* GetAttributeValue(int)->WinVariant. Same null / throw rules as get_property_value. */
    int32_t (*get_attribute_value)(int64_t range_id, int32_t attribute_id, GwinVariant* out);

    /* GetBoundingRectangles()->double[]: into a VT_R8 SAFEARRAY. */
    int32_t (*get_bounding_rectangles)(int64_t range_id, double** out, int32_t* out_count);

    /* GetEnclosingElement()->long: a GlassAccessible*, ADDREFED. */
    int32_t (*get_enclosing_element)(int64_t range_id, int64_t* out);

    /* GetText(int)->String: a gwin_alloc-ed block this library frees; NULL = null -> E_FAIL. */
    int32_t (*get_text)(int64_t range_id, int32_t max_length, uint16_t** out, int32_t* out_len);

    /* Move(int,int)->int: a TextUnit and a count. */
    int32_t (*move)(int64_t range_id, int32_t unit, int32_t count, int32_t* out);

    /* MoveEndpointByUnit(int,int,int)->int. */
    int32_t (*move_endpoint_by_unit)(int64_t range_id, int32_t endpoint, int32_t unit, int32_t count,
                                     int32_t* out);

    /* MoveEndpointByRange(int,WinTextRangeProvider,int). */
    int32_t (*move_endpoint_by_range)(int64_t range_id, int32_t endpoint, int64_t other_range_id,
                                      int32_t target_endpoint);

    /* Select(). */
    int32_t (*select)(int64_t range_id);

    /* AddToSelection(). */
    int32_t (*add_to_selection)(int64_t range_id);

    /* RemoveFromSelection(). */
    int32_t (*remove_from_selection)(int64_t range_id);

    /* ScrollIntoView(boolean). align_to_top is UIA's BOOL widened - see the BOOL note on find_text;
     * the Java target does not read its value. */
    int32_t (*scroll_into_view)(int64_t range_id, int32_t align_to_top);

    /* GetChildren()->long[]: GlassAccessible*s, VT_UNKNOWN. */
    int32_t (*get_children)(int64_t range_id, int64_t** out, int32_t* out_count);

    /* From ~GlassTextRangeProvider, where DeleteGlobalRef stood - the accessible_disposed rules, for
     * the range registry. */
    void (*range_disposed)(int64_t range_id);
} GwinTextRangeCallbacks;   /* 19 pointers */

#if defined(__cplusplus)
static_assert(sizeof(GwinTextRangeCallbacks) == 19 * sizeof(void*),
              "GwinTextRangeCallbacks must be 19 pointers");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(GwinTextRangeCallbacks) == 19 * sizeof(void*),
               "GwinTextRangeCallbacks must be 19 pointers");
#else
typedef char gwin_text_range_callbacks_size_check[(sizeof(GwinTextRangeCallbacks)
                                                   == 19 * sizeof(void*)) ? 1 : -1];
#endif

/*
 * sizeof probes, beside gwin_sizeof_view_callbacks: they return the real sizeof and only DOCUMENT
 * today's value, so appending a slot or a GwinVariant field fails a test instead of shifting a layout
 * silently.
 */
GLASS_WIN_EXPORT int32_t gwin_sizeof_accessible_callbacks(void);   /* == 70 * sizeof(void*) today */
GLASS_WIN_EXPORT int32_t gwin_sizeof_text_range_callbacks(void);   /* == 19 * sizeof(void*) today */
GLASS_WIN_EXPORT int32_t gwin_sizeof_variant(void);                /* == 72 on x64 today          */

/*
 * Install (cb == NULL clears) the accessible resp. text-range callback table. Copied by value, NULL
 * slots replaced by no-ops (see NULL SLOTS), so the caller may free the struct; the stubs in it must
 * outlive the process - Arena.global() - because there is no point at which this library can promise
 * that no further UIA call will arrive, and a call can arrive on a thread the JVM has never seen.
 * Last call wins, no lock. NO void* user: the identity is per provider, not per table. Always GWIN_OK.
 *
 * Install both BEFORE the first gwin_a11y_create. While neither is installed every upcall site answers
 * E_FAIL - the JNI arm it had before this section existed answered that without a JNIEnv, and the arm
 * itself is gone. Both tables go in together, so the two halves can never disagree.
 */
GLASS_WIN_EXPORT int32_t gwin_a11y_set_callbacks(const GwinAccessibleCallbacks* cb);
GLASS_WIN_EXPORT int32_t gwin_a11y_text_range_set_callbacks(const GwinTextRangeCallbacks* cb);

/*
 * new GlassAccessible(accessible_id): the body of Java_com_sun_glass_ui_win_WinAccessible__1createGlassAccessible
 * without its jobject. The object starts at refcount 1 - that one reference belongs to the Java peer
 * and gwin_a11y_destroy releases it - and bumps GlassApplication's live-accessible count, which the
 * Windows 7 + Narrator extra-pump workaround in gwin_run_loop reads. No OS call and no thread marshal,
 * so it works without a toolkit, exactly as gwin_view_create does. NULL only when the allocation
 * failed; WinAccessible turned that into RuntimeException("could not create platform accessible").
 * accessible_id must be non-zero (see IDENTITY) and must stay valid until accessible_disposed.
 *
 * THE RETURNED POINTER IS ALSO THE PROVIDER'S IRawElementProviderSimple* - the first base class - and
 * that is the value View.getAccessible() hands to UiaReturnRawElementProvider. Pre-existing fragility
 * of this library, recorded here, not introduced here.
 */
GLASS_WIN_EXPORT gwin_accessible_t gwin_a11y_create(int64_t accessible_id);

/*
 * GlassAccessible::Release: drops the Java peer's reference. NOT a delete - UIA, a live range, a
 * SAFEARRAY element or a VARIANT may still hold references, and the object stays alive, keeps serving
 * provider calls (WinAccessible's isDisposed() guards answer them) and fires accessible_disposed only
 * when the LAST one goes. `acc` MUST NOT BE NULL: the JNI entry point this replaces dereferenced it
 * unconditionally and WinAccessible guarded with `if (peer != 0L)`; that split stays as it is.
 */
GLASS_WIN_EXPORT void gwin_a11y_destroy(gwin_accessible_t acc);

/*
 * new GlassTextRangeProvider(range_id) owned by `acc`: the body of
 * Java_com_sun_glass_ui_win_WinTextRangeProvider__1createTextRangeProvider without its jobject. The
 * range AddRefs its accessible, so a live range pins it. Refcount 1, released by
 * gwin_a11y_text_range_destroy. NULL when acc is NULL (as the JNI answered) or the allocation failed -
 * WinTextRangeProvider does not check for 0 today. range_id must be non-zero and must stay valid until
 * range_disposed. The returned pointer is also the range's ITextRangeProvider*.
 */
GLASS_WIN_EXPORT gwin_text_range_t gwin_a11y_text_range_create(gwin_accessible_t acc,
                                                               int64_t range_id);

/*
 * GlassTextRangeProvider::Release, with gwin_a11y_destroy's rules: not a delete, range_disposed fires
 * when the last reference goes, and the accessible it pinned is released then. `range` MUST NOT BE
 * NULL - neither the JNI entry point nor WinTextRangeProvider.dispose() checks, and a range that
 * failed to be created crashes here today. Carried as it is; fixing it is a separate change.
 */
GLASS_WIN_EXPORT void gwin_a11y_text_range_destroy(gwin_text_range_t range);

/*
 * UiaRaiseAutomationPropertyChangedEvent(acc, property_id, old_value, new_value), the one UIA entry
 * point of this section that keeps a C body: it builds the two VARIANTs with the same copyVariant
 * marshalling the inbound direction needs anyway (see the section comment). `acc` is reinterpret-cast
 * to its IRawElementProviderSimple*, as the JNI did. Returns the HRESULT, sign-extended to int64_t;
 * the first VARIANT that cannot be built short-circuits with ITS HRESULT AND NO OS CALL IS MADE, which
 * is what the JNI returned. Both GwinVariants are owned by the caller (see OWNERSHIP) and are read
 * only during the call.
 *
 * Carried verbatim from the JNI body, deliberately: neither VARIANT is VariantClear-ed, so a VT_BSTR
 * property change leaks its BSTR and a failing second conversion leaks the first. Behaviour-neutral
 * beats tidy in a migration; fix it in its own change with its own test.
 */
GLASS_WIN_EXPORT int64_t gwin_a11y_raise_property_changed(gwin_accessible_t acc, int32_t property_id,
                                                          const GwinVariant* old_value,
                                                          const GwinVariant* new_value);

/*
 * TEST HOOKS, unconditionally exported, never called by production code and never bound by
 * WinGlassNative - gwin_test_fire_callback for the two tables of this section. With 89 slots they are
 * the only automated check that a FunctionDescriptor agrees with its prototype: the sizeof probes see
 * only pointers, and a descriptor with one parameter too many silently shifts every following argument
 * by a stack slot on x64 - no crash, no exception, a wrong number in a screen reader.
 *
 * Each fires slot `slot` of ITS table, numbered in declaration order, with a fixed pattern: the int32
 * in-parameters take 1001, 1002, ... in order, the doubles 1.5 and 2.5, a text in-parameter the two
 * code units { 0x0041, 0x0042 } with len 2, a SAFEARRAY* in-parameter 0x100000002 (bits above 32 set,
 * never dereferenced), find_attribute's GwinVariant* NULL as production passes it, and every boolean 1.
 * Returns what the slot returned - the int32 status, widened; 0 for the void disposal slots and for a
 * slot installed as NULL. GWIN_ERR_INVALID_ARG for an unknown slot, when that table is not installed,
 * or when `out` is NULL for a slot that has an out-parameter.
 *
 * `out` is a caller-owned block of at least gwin_sizeof_variant() bytes, 8-byte aligned, pre-filled by
 * the test with a sentinel. It receives the slot's out-parameters:
 *     - one scalar out-parameter (int64_t, double or int32_t): at offset 0, in its own type;
 *     - a block out-parameter: the POINTER as int64_t at offset 0 and the count as int32_t at offset
 *       8. THE BLOCK IS NOT FREED BY THE HOOK - the caller owns it and must gwin_free it, unlike
 *       gwin_test_fire_clipboard_callback, whose production path frees;
 *     - get_property_value / get_attribute_value: the GwinVariant at offset 0, and its two blocks are
 *       likewise the caller's to gwin_free;
 *     - get_bounding_rectangle: four floats at offset 0 and *out_written as int32_t at offset 16.
 * Slots without an out-parameter ignore `out` and accept NULL.
 *
 * WITH THE PRODUCTION TABLE INSTALLED both hooks reach the WinAccessible / WinTextRangeProvider that
 * the id resolves to and will run real code on it - fire them at an id no registry holds, or through a
 * recording table.
 */
GLASS_WIN_EXPORT int64_t gwin_test_fire_accessible_callback(int32_t slot, int64_t accessible_id,
                                                            void* out);
GLASS_WIN_EXPORT int64_t gwin_test_fire_text_range_callback(int32_t slot, int64_t range_id,
                                                            void* out);

/*
 * TEST HOOK, unconditionally exported, never called by production code. Writes the offsetof of the
 * ELEVEN GwinVariant fields, in declaration order (vt, i_val, l_val, flt_val, dbl_val, bool_val,
 * punk_val, bstr_val, bstr_len, p_dbl_val, p_dbl_count), so that a Java StructLayout is asserted
 * against the compiler's real layout instead of against a comment. Eleven, not the nine WinVariant
 * fields of the inventory: the two lengths are part of the struct and a probe that skipped them would
 * not pin it. Returns GWIN_OK, or GWIN_ERR_INVALID_ARG when out11 is NULL (nothing written).
 */
GLASS_WIN_EXPORT int32_t gwin_test_variant_offsets(int32_t* out11);

#ifdef __cplusplus
}
#endif

#endif /* GLASS_WIN_API_H */

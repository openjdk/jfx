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

package com.sun.glass.ui.win;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.utils.NativeLibLoader;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_CHAR_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * The single point of contact between {@code com.sun.glass.ui.win} and native code: the {@code gwin_*}
 * C ABI of {@code glass.dll} ({@code src/main/native-glass/win/glass_win_api.h}) and the Win32
 * functions the deleted JNI bodies called. It owns the library load, both {@link SymbolLookup}s, the
 * {@link Linker}, every downcall handle and every {@link MemoryLayout}, and it is the only class in
 * this package that uses a restricted {@code java.lang.foreign} method in product code (the test-only
 * shim {@code WinGlassNativeShim} binds a handful of read-back symbols of its own).
 * <p>
 * <b>{@link WinRobot}</b> (ABI 1). Eight of its ten natives were {@code WRAPPER}s - their C bodies did
 * nothing but marshal one or two Win32 calls -
 * so there is no C left for them at all: this class binds {@code user32!SendInput},
 * {@code user32!mouse_event}, {@code user32!GetCursorPos}, {@code user32!GetSystemMetrics} and
 * {@code user32!MapVirtualKeyW} directly and fills the {@code INPUT} structure itself. The two that
 * stay native are the GDI screen capture ({@code gwin_robot_capture}; {@code gwin_robot_pixel_color}, its
 * transforming twin, was test-only under ABI 5 and has been deleted)
 * and the key table ({@code gwin_key_java_to_windows}, {@code gwin_key_windows_to_java}), which
 * {@code KeyTable.cpp} shares with the native {@code WndProc} input path and must therefore exist in
 * exactly one copy.
 * <p>
 * <b>{@link WinTimer}</b> adds no C at all: all four of its natives were marshalling
 * around five {@code winmm} functions, so this class binds {@code winmm!timeGetDevCaps},
 * {@code timeBeginPeriod}, {@code timeEndPeriod}, {@code timeSetEvent} and {@code timeKillEvent}
 * directly and hands {@code timeSetEvent} one process-wide upcall stub. {@code Timer.cpp} and
 * {@code Timer.h} - 238 lines, and the only {@code AttachCurrentThread*} site in javafx.graphics -
 * have nothing left to do.
 * <p>
 * Every method here reproduces what the JNI body at commit {@code 8492cb03b0} did, step for step; the
 * javadoc of each names the {@code Robot.cpp} lines it comes from. The one intentional difference is
 * that the capture buffer is zero-filled where the JNI allocated an uninitialised {@code new BYTE[]},
 * which can only show on a GDI failure the JNI silently ignored (see {@link #robotCapture}).
 * <p>
 * <b>Threading.</b> {@link WinRobot} calls {@code Application.checkEventThread()} before every one of
 * these, so they all run on the JavaFX application thread, which on Windows is the Glass/Win32 UI
 * thread. That matters twice: {@code MapVirtualKeyW} and {@code gwin_key_java_to_windows} consult the
 * calling thread's keyboard layout, and {@code SendInput} is documented for the thread that owns the
 * input desktop.
 * <p>
 * The timer half is the exception, and the only one: {@link #timerStart} and {@link #timerStop} are
 * called from whatever thread drives {@code com.sun.glass.ui.Timer} (the FX thread, for
 * {@code QuantumToolkit}'s pulse timer), {@code onTimerFired} is entered from a winmm
 * multimedia-timer worker thread that nothing attaches, and the class therefore does hold state: the
 * timer registry and the {@code timeBeginPeriod} refcount, guarded as described at {@link #TIMERS}
 * and {@link #TIMER_LOCK}.
 * <p>
 * <b>{@link WinCursor} and {@link WinPixels}</b> again add no C: all four
 * {@code WinCursor} natives and two of the four {@code WinPixels} ones were {@code WRAPPER} or
 * {@code PURE}. {@code user32!ShowCursor} and the existing {@code user32!GetSystemMetrics} replace
 * {@code _setVisible} and {@code _getBestSize}; the {@code ICONINFO} sequence that
 * {@code Pixels::CreateIcon} ran for a custom cursor is built here from {@code gdi32!CreateBitmap},
 * {@code gdi32!CreateDIBSection}, {@code user32!CreateIconIndirect}, {@code gdi32!GdiFlush} and
 * {@code gdi32!DeleteObject} - five plain OS entry points and two SDK structures, which is the
 * {@code WRAPPER} case, not a reason for a new {@code gwin_*} export. {@code WinPixels} kept
 * {@code _attachInt} and {@code _attachByte} for a while, because the JNI bodies of
 * {@code GlassView._uploadPixels} and {@code GlassWindow._setIcon} still drove them through
 * {@code Pixels.attachData}; both peers have since been flipped and {@code WinPixels} declares no
 * {@code native} at all - its two attach overrides throw (see its class documentation).
 * <p>
 * <b>The Glass helper files</b> ({@code KeyTable.cpp}, {@code PlatformSupport.cpp}) add no C either.
 * {@code WinApplication._isKeyLocked}
 * was a two-case switch over {@code user32!GetKeyState} ({@code KeyTable.cpp:421-439}), so this class
 * binds {@code GetKeyState} and {@link #keyLockState} is all that is left of it. Of the eleven
 * upcalls {@code PlatformSupport.cpp} had, ten existed only so that C could build a {@code java.util.Map}
 * (they have since been deleted); two of the four sources that filled that map - the eight
 * {@code user32!GetSysColor} calls of {@code querySystemColors} and the two {@code user32!SystemParametersInfoW}
 * calls of {@code querySystemParameters} - were {@code WRAPPER}s and are bound here, with {@link WinPreferences}
 * turning them into preference entries. The other two sources are WinRT ({@code IUISettings},
 * {@code INetworkInformation}) and stay native, as does the key table itself, which the native
 * {@code WndProc} input path shares.
 * <p>
 * The rest of the helper files is the six exports {@code GLASS_WIN_ABI_VERSION} 2 added.
 * {@code gwin_key_code_for_char} is {@code WinApplication._getKeyCodeForChar}: it stays native
 * because its body is 40% {@code KeyTable.cpp} table lookup and it needs
 * {@code GlassApplication::GetMainThreadId()}, which is native-only state. The other five are the
 * WinRT half of the preferences - {@code gwin_prefs_query_ui_settings} and
 * {@code gwin_prefs_query_network} hand out POD instead of building a {@code java.util.Map} in C,
 * {@code gwin_sizeof_ui_settings} and {@code gwin_sizeof_network_info} pin the two layouts, and
 * {@code gwin_prefs_set_callbacks} installs the one upcall that survives, the preferences-changed
 * event. That last one is this class's second upcall stub, and like the timer's it lives in
 * {@link Arena#global()}, for the same reason: nothing ever unregisters the native side.
 * <p>
 * <b>{@code GlassApplication.cpp}</b> (ABI 3). Of the thirteen natives {@code WinApplication}
 * declared, exactly two were {@code WRAPPER}s: {@code _supportsUnifiedWindows} was
 * {@code return (IS_WINVISTA);}, i.e. {@code kernel32!GetVersion} and four lines of byte arithmetic
 * ({@link #isWindowsVersionAtLeast}), and {@code _getDefaultBrowser} was one
 * {@code shlwapi!AssocQueryStringW} plus a {@code CreateJString} ({@link #defaultBrowser()}). The
 * other eleven are not wrappers, and six of them are the {@code OS-CALL} family that
 * {@code GLASS_WIN_ABI_VERSION} 3 exists for: the message pump ({@link #runLoop}), its nested twin
 * ({@link #enterNestedEventLoop}, {@link #leaveNestedEventLoop}), {@code DestroyWindow} on the
 * toolkit window ({@link #terminateLoop}) and the {@code SendMessage} / {@code PostMessage} pair that
 * gets a {@code Runnable} onto the toolkit thread ({@link #invokeAndWait},
 * {@link #submitForLaterInvocation}). Their bodies did not change: they moved out of the JNI entry
 * points into {@code gwin_*} exports statement for statement, and the three
 * {@code CallVoidMethod(runnable, javaIDs.Runnable.run)} upcalls they contained collapse into the one
 * {@code GwinAppCallbacks.run_runnable} slot, because all three resolved the same cached method id.
 * A {@code Runnable} now crosses as a {@code long} id into {@link #RUNNABLES} where it used to cross
 * as a {@code JGlobalRef}, and {@code glass.dll} holds no Java reference at all.
 * <p>
 * <b>What this deliberately does not move is the identity of the toolkit thread.</b>
 * {@code WinApplication.runLoop} still creates {@code WindowsNativeRunloopThread} and still calls
 * {@code setEventThread} on it before starting it; the thread body is now one FFM downcall where a
 * JNI native method used to be, on the same thread, so every {@code Application.checkEventThread()}
 * passes and fails in exactly the same cases. That is an invariant and not an accident: the surviving
 * JNI half of {@code glass.dll} reaches its {@code JNIEnv} through a {@code GetEnv()} that neither
 * attaches nor checks its return code, so it is correct only on an attached Java thread. No helper
 * thread may be introduced anywhere on this path.
 * <p>
 * That left five natives on {@code WinApplication}; ABI 5 removed all of them.
 * {@code getPlatformPreferences} is {@code WinPreferences.collect}; {@code _init} is {@link #loadDpiFuncs}
 * then {@link #appCreate()}; {@code staticScreen_getScreens} is {@link #collectMonitors()} arranged by
 * {@link WinScreenLayout}; and {@code initIDs} and {@code _setClassLoader}, JNI-only bodies that served the
 * rest of the JNI half of the library, have no replacement at all.
 * <p>
 * <b>The menus</b> (also ABI 3) are the cleanest {@code WRAPPER} case in the whole of Glass: eleven of
 * {@code GlassMenu.cpp}'s twelve JNI bodies were marshalling around a single {@code user32} menu call
 * each, so all eleven are bound here - {@code CreateMenu}, {@code DestroyMenu}, {@code IsMenu},
 * {@code GetMenuItemCount}, {@code GetMenuItemInfoW}, {@code SetMenuItemInfoW},
 * {@code InsertMenuItemW}, {@code InsertMenuW}, {@code RemoveMenu}, {@code EnableMenuItem} and
 * {@code CheckMenuItem} - and no {@code gwin_menu_*} <em>downcall</em> exists or is wanted. The HMENU
 * stays what it always was, an opaque handle value Java owns as a {@code long}
 * ({@code WinMenuImpl.ptr}); only {@code MENUITEMINFOW} crosses, from a per-call confined arena,
 * because user32 copies the {@code MIIM_STRING} title into the menu and retains nothing. The twelfth
 * body could not move, because {@code HandleMenuCommand} is <em>called by</em>
 * {@code GlassWindow::WindowProc}'s WM_COMMAND arm from inside {@code DispatchMessage}: it is the two
 * exports {@code GLASS_WIN_ABI_VERSION} 3 adds for the menus, {@code gwin_menu_set_callbacks} and its
 * {@code gwin_sizeof_menu_callbacks} probe. With them installed {@code WinMenuImpl} declares no
 * native at all, and {@code glass.dll} no longer holds a {@code jclass} global ref for it.
 * <p>
 * <b>The view peer</b> (ABI 4), {@code GlassView.cpp}, and the {@code WndProc} body that feeds it,
 * {@code ViewContainer.cpp}. Eleven of {@code WinView}'s eighteen natives were {@code OS-CALL} or
 * native-state bodies and become the {@code gwin_view_*} exports ({@link #viewClose} through
 * {@link #viewFinishImeComposition}); the three multi-click getters were {@code WRAPPER}s over
 * {@code user32!GetDoubleClickTime} and {@code GetSystemMetrics(SM_CXDOUBLECLK / SM_CYDOUBLECLK)} and
 * are bound here directly; {@code _create} and a shrunken {@code _initIDs} stayed JNI then, because
 * {@code GlassDnD.cpp} still dereferenced the global ref {@code _create} took and
 * {@code GlassWindow.cpp} still read {@code View.ptr} - the window flip passed the view in from Java
 * ({@link #windowSetView}), the clipboard flip keyed the drop target by view id, and ABI 5 made
 * {@code _create} {@link #viewCreate}, so {@code WinView} declares no native any more. The other direction
 * is the bulk of the view work:
 * the 26 {@code Call*Method} sites of {@code GlassView.cpp}, {@code ViewContainer.cpp} and
 * {@code FullScreenWindow.cpp} - every mouse move, key, wheel notch, resize, repaint, IME step, touch
 * point and gesture delta - become the fifteen slots of {@code GwinViewCallbacks} and
 * {@code GwinGestureCallbacks}, keyed by an {@code int64_t} view id that {@code WinView} assigns and
 * resolves through its own registry, so that {@code glass.dll} holds no Java reference on the view
 * path either. The fifteen targets are in this class ({@link #onNotifyMouse} and its siblings) and do
 * only marshalling; the dispatch is in {@code WinView}, because {@code View.notify*} are
 * {@code protected} members of another package that only a {@code WinView}-typed reference can reach,
 * and because the overrides {@code WinView} has for {@code notifyResize} and {@code notifyMenu} must
 * be the ones that run. The hot slots carry primitives only and allocate nothing per event; the four
 * that carry pointers bound them with {@code reinterpret} and copy before returning. See
 * {@link #installViewCallbacks} for why this table pair, unlike the other three, has no
 * {@code void* user}.
 * <p>
 * <b>The window peer</b> (ABI 4), {@code GlassWindow.cpp}. Twenty-four of {@code WinWindow}'s thirty
 * natives were {@code OS-CALL} - ten of them only a {@code SendMessage(WM_DO_ACTION)} marshal around
 * one {@code SetWindowPos} or {@code ShowWindow}, and the marshal is observable, so they stay - and
 * four were {@code PURE} state the {@code WndProc} reads synchronously (the grab, the minimum and
 * maximum size); those twenty-seven become the {@code gwin_window_*} exports ({@link #windowCreate}
 * through {@link #windowShowSystemMenu}). {@code _getAnchor} was a {@code WRAPPER} over four
 * {@code user32} calls and is {@link #windowAnchor}, with no C behind it; {@code _initIDs} cached
 * the twelve method ids the table below replaces; {@code _setBackground2} had no caller. The
 * {@code Cursor} to {@code HCURSOR} switch of {@code GlassCursor.cpp} moves to {@code WinCursor}
 * with the three-step {@code LoadCursorW} here ({@link #loadSystemCursor(int)}), and the icon half
 * of {@code Pixels::CreateIcon} is {@link #iconCreate} - the cursor sequence of {@link #cursorCreateCustom} with
 * {@code fIcon = TRUE}, whose {@code HICON} the window then owns. The other direction is the eleven
 * {@code Call*Method} sites of {@code GlassWindow.cpp} plus the {@code DeleteGlobalRef} of
 * {@code ~GlassWindow}: the twelve slots of {@code GwinWindowCallbacks}, keyed by an {@code int64_t}
 * window id {@code WinWindow} assigns and resolves through its own registry, so that {@code glass.dll}
 * holds no Java reference on the window path either. <b>The handle stays the HWND</b>:
 * {@code gwin_window_create} returns what {@code _createWindow} returned, and
 * {@code Window.getNativeWindow()} still hands it to Prism and to embedders. The install
 * ({@link #installWindowCallbacks}) is the flip and travels with {@code WinWindow}'s forwarders.
 * <p>
 * <b>The clipboard, drag-and-drop and dialog peers</b> (additive under ABI 4), of {@code GlassClipboard.cpp},
 * {@code GlassDnD.cpp} and
 * {@code CommonDialogs*.cpp}: {@code WinSystemClipboard}, {@code WinDnDClipboard} and
 * {@code WinCommonDialogs}. Thirteen of their fifteen natives reached OLE, COM vtables, GDI,
 * {@code comdlg32} or {@code shell32} and become the {@code gwin_clipboard_*}, {@code gwin_dnd_*} and
 * {@code gwin_dialog_*} exports ({@link #clipboardRegisterViewer} through {@link #dialogFolder}); the
 * COM objects themselves - {@code ClipboardData : IDataObject}, {@code GlassDropTarget},
 * {@code GlassDropSource} - stay in C++ because Windows and other processes call their vtables
 * through the OLE marshaller, and are addressed by an opaque {@code gwin_clipboard_t}. The one
 * {@code WRAPPER}, {@code isOwner}, was {@code ole32!OleIsCurrentClipboard} behind a NULL test and is
 * {@link #oleIsCurrentClipboard}, the first {@code ole32} symbol this class binds; the two
 * {@code initIDs} cached the method ids the two tables replace. The other direction is the harder
 * half: the sixteen slots of {@code GwinClipboardCallbacks} (7) and {@code GwinDndCallbacks} (9) -
 * the delayed render of a paste, the drop-effect reports, the viewer-chain notification, the four
 * drop-target notifications and the drag clipboard's singleton state - keyed by an {@code int64_t}
 * clipboard id {@code WinSystemClipboard} assigns and by the view id of {@code WinView} for the drop target, so
 * that {@code glass.dll} holds no Java reference on this path either. Several of them arrive with
 * <em>no Java frame below</em>, through a COM vtable and the OLE marshaller, from another process's
 * paste or drag: a {@code Throwable} escaping one would unwind into that process, which is why every
 * target here catches {@code Throwable} and answers its slot default, with the sink the JNI used at
 * that site ({@link #describeUpcallFailure} where it was {@code checkJavaException},
 * {@link #reportUpcallFailure} where it was {@code CheckAndClearException}). The push exports return a
 * status and publish the new handle through {@code set_data_object} at the instant the JNI wrote the
 * field; {@code fos_serialize} hands the C a {@link #gwinAlloc} block the C frees. Both tables are
 * installed from <em>this</em> class's static initializer ({@link #installClipboardCallbacks},
 * {@link #installDndCallbacks}), not from {@code WinApplication}'s and not from a peer's: the JNI
 * reached {@code WinDnDClipboard} through {@code ClassForName(initialize = true)} on the first drag,
 * and a table installed from a lazily loaded peer would never exist for a drop from Explorer into
 * a process that has not started a drag of its own.
 * <p>
 * <b>The last of {@code GlassApplication.cpp} and {@code GlassScreen.cpp}</b> (ABI 5). The screen
 * enumeration was {@code WRAPPER} plus {@code PURE}: two {@code EnumDisplayMonitors} passes,
 * {@code GetMonitorInfoW}, a display DC for {@code GetDeviceCaps} and {@code shcore!GetDpiForMonitor},
 * then an anchoring computation that touches no OS at all. The first half is bound here
 * ({@link #collectMonitors()}, with {@code GlassScreen::LoadDPIFuncs} as {@link #loadDpiFuncs}); the
 * second is {@link WinScreenLayout}; and the {@code Screen} objects the JNI built in C are built in
 * Java. What stays native is what only the {@code WndProc} can see - that the monitors may have
 * changed - which is the one-slot {@code GwinScreenCallbacks} ({@link #installScreenCallbacks}), and
 * the two constructors {@code _init} and {@code WinView._create} ran, now {@link #appCreate()} and
 * {@link #viewCreate}.
 * <p>
 * <b>The last JNI of {@code glass.dll}: the two UI Automation providers</b> (ABI 6). The nine native
 * methods of {@code WinAccessible} and {@code WinTextRangeProvider} are the ten exports and the two
 * callback tables of {@link #installAccessibilityCallbacks}. The provider objects themselves stay in C,
 * because they are the <em>inbound</em> edge of UI Automation - 19 COM interfaces whose vtables Windows
 * invokes, plus {@code oleaut32} {@code BSTR} and {@code SAFEARRAY} marshalling - but the 87
 * {@code Call*Method} sites in their bodies become slots keyed by an {@code int64_t} id, and the two
 * {@code _initIDs} disappear with everything they cached. That is not a preference: {@code FindClass}
 * inside a downcall cannot see a javafx.graphics class on JDK 26, so the only safe design is one in
 * which {@code glass.dll} looks nothing up. Two of the nine get no export at all -
 * {@code UiaRaiseAutomationEvent} and {@code UiaClientsAreListening} were one-line {@code WRAPPER}s, and
 * Java binds {@code UIAutomationCore} itself, as it binds {@code shlwapi!AssocQueryStringW}. Stated
 * behaviour difference: a slot dialled on a thread the JVM has never seen - which Windows does for the
 * two advise slots during a Narrator shutdown, and for either disposal slot - answered {@code E_FAIL}
 * under JNI, because {@code GetEnv()} returned {@code NULL} and the bodies gave up rather than
 * attaching; an FFM upcall stub attaches the caller and runs the Java target.
 * <p>
 * <b>No {@link Linker.Option#critical critical} downcalls.</b> {@code gwin_robot_capture} is a
 * multi-step GDI sequence ({@code CreateDC}, {@code BitBlt}, {@code GetDIBits}) that can block on the
 * compositor for tens of milliseconds; pinning a Java {@code int[]} across it with
 * {@code MemorySegment.ofArray} would hold GC off for that whole time. It gets an off-heap buffer from
 * a per-call confined arena instead, and the result is copied into the caller's array afterwards -
 * which is also what the JNI did ({@code new BYTE[]} plus {@code SetIntArrayRegion}). The Win32 calls
 * are cold and pass no arrays at all. Neither is any of the five winmm functions:
 * {@code timeSetEvent} installs a callback, {@code timeKillEvent} can block, and none of them is ever
 * handed a Java array. The cursor block is the same story from the other direction: the pixels are
 * copied into a GDI DIB section by {@link MemorySegment#copy}, never handed to a downcall, so no call
 * in it takes a buffer at all and {@code critical} has nothing to apply to. That the JNI held
 * {@code GetPrimitiveArrayCritical} across {@code CreateDIBSection} and {@code CreateIconIndirect}
 * ({@code Utils.h:438}, {@code Pixels.cpp:122-145}) is a defect this migration drops rather than
 * reproduces. The eight exports of ABI 3 forbid it outright rather than merely not needing it:
 * every one of them either runs arbitrary Java through {@code run_runnable} or pumps a Win32
 * message loop, and {@code gwin_run_loop} does both for the entire life of the application.
 * <p>
 * Line numbers into the {@code native-glass/win} C++ sources ({@code Robot.cpp}, {@code Timer.cpp},
 * {@code GlassApplication.cpp} and the rest), bare {@code :N} and {@code LN} forms included, refer to those files
 * at commit {@code 8492cb03b0} ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>});
 * a bare number belongs to the C file named last before it.
 */
final class WinGlassNative {

    /**
     * The {@code gwin_*} ABI revision this class is written against ({@code GLASS_WIN_ABI_VERSION}). 5 adds
     * six exports, with {@code gwin_robot_pixel_color} and {@code gwin_test_screen_anchor} defined as
     * test-only - this class never bound either, and both have since been deleted without a bump.
     * <p>
     * 6 is the accessibility section: thirteen exports and two callback tables, of which this class binds
     * ten - {@code gwin_test_fire_accessible_callback}, {@code gwin_test_fire_text_range_callback} and
     * {@code gwin_test_variant_offsets} are test hooks the shim binds and this class does not. They landed
     * additive under 5, and this class binding them changed nothing a mismatched pair could not already
     * detect: a {@code glass.dll} without those exports fails in {@link #bindGlass} by name. What the bump
     * to 6 protects is the other direction, a {@code glass.dll} whose {@code Java_*} arms are gone meeting
     * a Java side that installs no tables, and that shape first existed when those arms were deleted. The
     * bump therefore travelled with that deletion: this literal and {@code GLASS_WIN_ABI_VERSION} moved in
     * one change set, because the check below is an exact equality in both directions.
     */
    static final int ABI_VERSION = 6;

    /** {@code enum GwinStatus} of {@code glass_win_api.h}. */
    static final int GWIN_OK = 0;
    static final int GWIN_ERR_INVALID_ARG = -1;

    /**
     * No toolkit window - never created, or already destroyed. Nothing was scheduled and no callback
     * will arrive for the id that was passed, so the caller may drop its registry entry. The two JNI
     * entry points this replaces returned {@code void} and failed exactly this silently
     * ({@code GlassApplication.cpp:229-235}, {@code :238-245}).
     */
    static final int GWIN_ERR_NO_TOOLKIT = -2;

    /**
     * A callback slot reported a Java {@code Throwable}; the C maps it to {@code E_JAVAEXCEPTION}
     * exactly as {@code OleUtils.h}'s {@code checkJavaException} did.
     */
    static final int GWIN_ERR_UPCALL = -3;

    /**
     * An OLE / COM step failed and the JNI body swallowed the {@code HRESULT} ({@code OLE_CATCH});
     * reported so Java can see it, ignored by every caller here so it stays neutral.
     */
    static final int GWIN_ERR_OLE = -4;

    /**
     * {@code enum GwinDialogStatus}: what {@code gwin_dialog_file} / {@code gwin_dialog_folder}
     * return. Informational only - the JNI could not tell the three apart at its Java boundary, so
     * {@link #dialogFile} and {@link #dialogFolder} key on the out-block and ignore this.
     */
    static final int GWIN_DIALOG_OK = 0;
    static final int GWIN_DIALOG_CANCELLED = 1;
    static final int GWIN_DIALOG_FAILED = 2;

    /** {@code enum GwinRobotCaptureError}: the first GDI step of {@code gwin_robot_capture} that failed. */
    static final int GWIN_ROBOT_ERR_CREATE_DC = 1;
    static final int GWIN_ROBOT_ERR_CREATE_MEM_DC = 2;
    static final int GWIN_ROBOT_ERR_CREATE_BITMAP = 3;
    static final int GWIN_ROBOT_ERR_BITBLT = 4;
    static final int GWIN_ROBOT_ERR_GETDIBITS = 5;

    /* winuser.h: INPUT.type. */
    static final int INPUT_MOUSE = 0;
    static final int INPUT_KEYBOARD = 1;

    /* winuser.h: KEYBDINPUT.dwFlags. KEYEVENTF_SCANCODE and KEYEVENTF_UNICODE are deliberately absent. */
    static final int KEYEVENTF_EXTENDEDKEY = 0x0001;
    static final int KEYEVENTF_KEYUP = 0x0002;

    /* winuser.h: MapVirtualKeyW map types. The C used the bare 0. */
    static final int MAPVK_VK_TO_VSC = 0;

    /* winuser.h: MOUSEINPUT.dwFlags. MOUSEEVENTF_VIRTUALDESK is deliberately absent (see robotMouseMove). */
    static final int MOUSEEVENTF_MOVE = 0x0001;
    static final int MOUSEEVENTF_LEFTDOWN = 0x0002;
    static final int MOUSEEVENTF_LEFTUP = 0x0004;
    static final int MOUSEEVENTF_RIGHTDOWN = 0x0008;
    static final int MOUSEEVENTF_RIGHTUP = 0x0010;
    static final int MOUSEEVENTF_MIDDLEDOWN = 0x0020;
    static final int MOUSEEVENTF_MIDDLEUP = 0x0040;
    static final int MOUSEEVENTF_XDOWN = 0x0080;
    static final int MOUSEEVENTF_XUP = 0x0100;
    static final int MOUSEEVENTF_WHEEL = 0x0800;
    static final int MOUSEEVENTF_ABSOLUTE = 0x8000;

    /* winuser.h: MOUSEINPUT.mouseData for the X buttons, and one wheel notch. */
    static final int XBUTTON1 = 0x0001;
    static final int XBUTTON2 = 0x0002;
    static final int WHEEL_DELTA = 120;

    /* winuser.h: GetSystemMetrics indices. */
    static final int SM_CXSCREEN = 0;
    static final int SM_CYSCREEN = 1;
    static final int SM_SWAPBUTTON = 23;
    static final int SM_CXCURSOR = 13;
    static final int SM_CYCURSOR = 14;
    /* The double-click rectangle (_getMultiClickMaxX/Y); both pinned by static_assert in glass_win_api.cpp. */
    static final int SM_CXDOUBLECLK = 36;
    static final int SM_CYDOUBLECLK = 37;

    /* wingdi.h: what the cursor's colour bitmap is (Pixels.cpp:119,122). */
    static final int BI_RGB = 0;
    static final int DIB_RGB_COLORS = 0;

    /** {@code ICONINFO.fIcon}: {@code FALSE} makes {@code CreateIconIndirect} build a cursor. */
    private static final int ICONINFO_CURSOR = 0;

    /**
     * {@code ICONINFO.fIcon}: {@code TRUE} makes {@code CreateIconIndirect} build an icon, which is
     * {@code Pixels::CreateIcon}'s default.
     */
    private static final int ICONINFO_ICON = 1;

    /**
     * The three messages {@code Bitmap::Bitmap} printed when the cursor's monochrome mask could not
     * be built ({@code Pixels.cpp:43,49,61}), kept to the character. They go to {@code System.err} and
     * not to {@code PlatformLogger} because that is where the C put them, and the paths that reach
     * them are unreachable from a valid {@code Pixels} anyway.
     */
    private static final String MASK_FAILED = "Bitmap: Failed to create monochrome bitmap for icon.";

    /* winuser.h: the two toggle keys KeyTable.cpp's _isKeyLocked asked GetKeyState about (L421-439). */
    static final int VK_CAPITAL = 0x14;

    /* winuser.h: the virtual-key codes of KeyTable.cpp's IsExtendedKey (L315-343). */
    static final int VK_PRIOR = 0x21;
    static final int VK_NEXT = 0x22;
    static final int VK_END = 0x23;
    static final int VK_HOME = 0x24;
    static final int VK_LEFT = 0x25;
    static final int VK_UP = 0x26;
    static final int VK_RIGHT = 0x27;
    static final int VK_DOWN = 0x28;
    static final int VK_SNAPSHOT = 0x2C;
    static final int VK_INSERT = 0x2D;
    static final int VK_DELETE = 0x2E;
    static final int VK_NUMPAD0 = 0x60;
    static final int VK_NUMPAD9 = 0x69;
    static final int VK_NUMLOCK = 0x90;

    /*
     * winuser.h: the two SystemParametersInfoW actions PlatformSupport::querySystemParameters used
     * and the HIGHCONTRAST flag it tested. SPI_GETHIGHCONTRAST is the one that needs its structure
     * size passed twice - see systemParametersHighContrast.
     */
    static final int SPI_GETHIGHCONTRAST = 0x0042;
    static final int SPI_GETCLIENTAREAANIMATION = 0x1042;
    static final int HCF_HIGHCONTRASTON = 0x00000001;

    /*
     * winuser.h: the eight GetSysColor indices of PlatformSupport::querySystemColors, in the order it
     * queried them. COLOR_3DFACE is winuser.h's alias for COLOR_BTNFACE. A wrong index here returns
     * a plausible colour instead of failing, which is why glass_win_api.cpp
     * static_asserts all eight.
     */
    static final int COLOR_3DFACE = 15;
    static final int COLOR_BTNTEXT = 18;
    static final int COLOR_GRAYTEXT = 17;
    static final int COLOR_HIGHLIGHT = 13;
    static final int COLOR_HIGHLIGHTTEXT = 14;
    static final int COLOR_HOTLIGHT = 26;
    static final int COLOR_WINDOW = 5;
    static final int COLOR_WINDOWTEXT = 8;

    /*
     * shlwapi.h: the three values Java_..._1getDefaultBrowser passed to AssocQueryStringW
     * (GlassApplication.cpp:541-547). ASSOCF_NONE is "no special flags", ASSOCSTR_COMMAND asks for
     * the command line registered for the association, and MAX_PATH (minwindef.h) is the size of the
     * WCHAR buffer the C put on its stack - in CHARACTERS, which is the one thing about this call
     * that is easy to get wrong; see defaultBrowser().
     */
    static final int ASSOCF_NONE = 0;
    static final int ASSOCSTR_COMMAND = 1;
    static final int MAX_PATH = 260;

    /*
     * enum GwinPreferenceType (glass_win_api.h), which is PlatformSupport::PreferenceType
     * (PlatformSupport.h). A bitmask of the four sources of the preference map, not an index.
     * GWIN_PT_ALL is what WinApplication.getPlatformPreferences collects; the values the native side
     * sends to the callback are 2, 4, 5, 6 and 8.
     */
    static final int GWIN_PT_SYSTEM_COLORS = 1;
    static final int GWIN_PT_SYSTEM_PARAMS = 2;
    static final int GWIN_PT_UI_SETTINGS = 4;
    static final int GWIN_PT_NETWORK_INFORMATION = 8;
    static final int GWIN_PT_ALL = 15;

    /**
     * {@code GWIN_UI_COLOR_COUNT}: Background, Foreground, AccentDark3..1, Accent, AccentLight1..3,
     * in the order {@code PlatformSupport::queryUISettings} asked for them, which is the order
     * {@link WinPreferences} names the nine {@code Windows.UIColor.*} keys in.
     */
    static final int GWIN_UI_COLOR_COUNT = 9;

    /*
     * enum GwinNetworkCost (glass_win_api.h). These are NOT the WinRT NetworkCostType enumerators -
     * there Fixed is 2 and Variable is 3 - because the C picked its string with an explicit switch and
     * this ABI pins its own numbering. gwin_prefs_query_network maps between them.
     */
    static final int GWIN_NET_COST_UNKNOWN = 0;
    static final int GWIN_NET_COST_UNRESTRICTED = 1;
    static final int GWIN_NET_COST_VARIABLE = 2;
    static final int GWIN_NET_COST_FIXED = 3;

    /* mmsystem.h: the multimedia timer service that Timer.h L46-94 drove. */
    static final int TIMERR_NOERROR = 0;
    static final int TIME_ONESHOT = 0x0000;
    static final int TIME_PERIODIC = 0x0001;

    /*
     * winuser.h menu flags. GlassMenu.cpp used two overlapping families and the difference is
     * load-bearing, so both are pinned here and neither is normalised into the other:
     *
     *   MIIM_* / MFT_* / MFS_* belong to MENUITEMINFOW, which InsertMenuItemW, SetMenuItemInfoW and
     *   GetMenuItemInfoW take;
     *   MF_* belong to the older positional calls, which InsertMenuW, RemoveMenu, EnableMenuItem and
     *   CheckMenuItem take.
     *
     * MFS_GRAYED is 3 (MF_GRAYED | MF_DISABLED) while MF_GRAYED is 1, and GlassMenu.cpp wrote
     * MFS_GRAYED when inserting (L172, L207) and MF_GRAYED when enabling/disabling (L325, L346).
     * Both are reproduced exactly.
     */
    static final int MIIM_STATE = 0x00000001;
    static final int MIIM_ID = 0x00000002;
    static final int MIIM_SUBMENU = 0x00000004;
    static final int MIIM_DATA = 0x00000020;
    static final int MIIM_STRING = 0x00000040;
    static final int MIIM_FTYPE = 0x00000100;

    static final int MFT_STRING = 0x00000000;
    static final int MFT_SEPARATOR = 0x00000800;

    static final int MFS_ENABLED = 0x00000000;
    static final int MFS_GRAYED = 0x00000003;
    static final int MFS_CHECKED = 0x00000008;
    static final int MFS_UNCHECKED = 0x00000000;

    static final int MF_BYCOMMAND = 0x00000000;
    static final int MF_BYPOSITION = 0x00000400;
    static final int MF_SEPARATOR = 0x00000800;
    static final int MF_ENABLED = 0x00000000;
    static final int MF_GRAYED = 0x00000001;
    static final int MF_CHECKED = 0x00000008;
    static final int MF_UNCHECKED = 0x00000000;

    /*
     * TIME_KILL_SYNCHRONOUS (0x0100) is deliberately absent from that list, and has to stay absent.
     * Timer.h:71-72 passed TIME_PERIODIC alone, so timeKillEvent may return while a callback is still
     * running - and that, not any lock discipline of ours, is what makes timerStop safe to call from
     * the JavaFX application thread. The stopping thread holds the com.sun.glass.ui.Timer instance
     * monitor (Timer.java:129 synchronized stop()), and the tick path takes that same monitor:
     * QuantumToolkit.java:536 pulseTimer.pause() -> Timer.java:141 synchronized pause(). A
     * timeKillEvent that joined an in-flight callback would therefore deadlock the FX thread against a
     * winmm worker on every application shutdown (QuantumToolkit.java:869 and :892). Setting the flag
     * is not a follow-up detail: it requires breaking that monitor overlap first.
     */

    /*
     * The Win32 values of GlassScreen.cpp's former enumeration. glass_win_api.cpp pins
     * MONITORINFOF_PRIMARY, the GetDeviceCaps indices and CCHDEVICENAME by static_assert. It pins the two
     * shcore enums too: all five values Java passes - MDT_EFFECTIVE_DPI, MDT_RAW_DPI, PROCESS_DPI_UNAWARE,
     * PROCESS_SYSTEM_DPI_AWARE and PROCESS_PER_MONITOR_DPI_AWARE - against the SDK's own definitions in
     * <ShellScalingApi.h>, included for its declarations only (nothing in glass.dll calls shcore). The
     * former GlassScreen.cpp pin only checked that file's private copies of the enums and was deleted
     * with them.
     */
    static final int MONITORINFOF_PRIMARY = 1;
    static final int BITSPIXEL = 12;
    static final int PLANES = 14;
    static final int LOGPIXELSX = 88;
    static final int LOGPIXELSY = 90;
    static final int CCHDEVICENAME = 32;
    static final int MDT_EFFECTIVE_DPI = 0;
    static final int MDT_RAW_DPI = 2;
    static final int PROCESS_PER_MONITOR_DPI_AWARE = 2;

    /** winerror.h {@code S_OK}: the only {@code HRESULT} {@code GetMonitorSettings} tested for. */
    static final int S_OK = 0;

    /**
     * {@code MOUSEINPUT} (winuser.h) on x64: 32 bytes, four bytes of padding between {@code time} and
     * the pointer-sized {@code dwExtraInfo}. {@code glass_win_api.cpp} static_asserts these offsets.
     */
    static final StructLayout MOUSEINPUT_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("dx"),
            JAVA_INT.withName("dy"),
            JAVA_INT.withName("mouseData"),
            JAVA_INT.withName("dwFlags"),
            JAVA_INT.withName("time"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("dwExtraInfo"));

    /** {@code KEYBDINPUT} (winuser.h) on x64: 24 bytes, the same padding before {@code dwExtraInfo}. */
    static final StructLayout KEYBDINPUT_LAYOUT = MemoryLayout.structLayout(
            JAVA_SHORT.withName("wVk"),
            JAVA_SHORT.withName("wScan"),
            JAVA_INT.withName("dwFlags"),
            JAVA_INT.withName("time"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("dwExtraInfo"));

    /**
     * {@code INPUT} (winuser.h) on x64: 40 bytes - {@code DWORD type} at 0, four bytes of padding, then
     * the union of {@code MOUSEINPUT} and {@code KEYBDINPUT} at 8. Every {@code SendInput} call below
     * passes {@code byteSize()} as {@code cbSize}, as the C passed {@code sizeof(INPUT)}.
     */
    static final StructLayout INPUT_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("type"),
            MemoryLayout.paddingLayout(4),
            MemoryLayout.unionLayout(
                    MOUSEINPUT_LAYOUT.withName("mi"),
                    KEYBDINPUT_LAYOUT.withName("ki")).withName("u"));

    /** {@code POINT} (windef.h): two {@code LONG}s, 8 bytes; what {@code GetCursorPos} fills. */
    static final StructLayout POINT_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("x"),
            JAVA_INT.withName("y"));

    /**
     * {@code RECT} (windef.h): four {@code LONG}s, 16 bytes; what {@code GetWindowRect} fills for the
     * {@code _getAnchor} WRAPPER ({@link #windowAnchor}). Pinned by {@code static_assert} in
     * {@code glass_win_api.cpp}; there is no sizeof export for a Windows SDK structure.
     */
    static final StructLayout RECT_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("left"),
            JAVA_INT.withName("top"),
            JAVA_INT.withName("right"),
            JAVA_INT.withName("bottom"));

    /**
     * {@code TIMECAPS} (mmsystem.h): two {@code UINT}s, 8 bytes on x64 and arm64, filled by
     * {@code timeGetDevCaps} ({@code Timer.h:85}). {@code WinTimerNativeTest} pins the size and both
     * offsets and then reads the values back, which is what catches a swapped or shifted layout - the
     * size and offsets on their own can only restate the numbers this layout was written from. A
     * {@code static_assert} against the real {@code TIMECAPS} belongs in {@code glass_win_api.cpp},
     * beside the {@code INPUT} ones, and is the C side of the same check.
     */
    static final StructLayout TIMECAPS_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("wPeriodMin"),
            JAVA_INT.withName("wPeriodMax"));

    /**
     * {@code BITMAPINFOHEADER} (wingdi.h): 40 bytes, two {@code WORD}s in the middle and no padding -
     * every member is naturally aligned where it falls. It describes the 32-bit top-down
     * {@code BI_RGB} DIB section a cursor's colour bitmap is made of ({@code Pixels.cpp:113-121}).
     * {@code WinCursorPixelsNativeTest} pins the size and every offset; the C side of the same check
     * is a {@code static_assert} in {@code glass_win_api.cpp}.
     */
    static final StructLayout BITMAPINFOHEADER_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("biSize"),
            JAVA_INT.withName("biWidth"),
            JAVA_INT.withName("biHeight"),
            JAVA_SHORT.withName("biPlanes"),
            JAVA_SHORT.withName("biBitCount"),
            JAVA_INT.withName("biCompression"),
            JAVA_INT.withName("biSizeImage"),
            JAVA_INT.withName("biXPelsPerMeter"),
            JAVA_INT.withName("biYPelsPerMeter"),
            JAVA_INT.withName("biClrUsed"),
            JAVA_INT.withName("biClrImportant"));

    /**
     * {@code ICONINFO} (winuser.h) on x64: 32 bytes - three {@code DWORD}s, four bytes of padding
     * before the first {@code HBITMAP}, then the two handles ({@code Pixels.cpp:138-144}). On a
     * 32-bit Windows it would be 20 bytes; the JDK's {@link Linker} supports x64 and aarch64 there,
     * both of which have 8-byte handles, and {@link #INPUT_LAYOUT} rests on the same fact.
     */
    static final StructLayout ICONINFO_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("fIcon"),
            JAVA_INT.withName("xHotspot"),
            JAVA_INT.withName("yHotspot"),
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("hbmMask"),
            ADDRESS.withName("hbmColor"));

    /**
     * {@code HIGHCONTRASTW} (winuser.h) on x64: 16 bytes - two {@code UINT}s and an
     * {@code LPWSTR} that Windows fills in ({@code PlatformSupport::querySystemParameters}). {@code cbSize} has
     * to be set to that size <em>and</em> passed again as {@code uiParam}, which is what the C did;
     * {@code lpszDefaultScheme} points into memory Windows owns and is only ever read.
     */
    static final StructLayout HIGHCONTRAST_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("cbSize"),
            JAVA_INT.withName("dwFlags"),
            ADDRESS.withName("lpszDefaultScheme"));

    /**
     * {@code GwinUiSettings} ({@code glass_win_api.h}): fifteen {@code int32_t} in a row, 60 bytes,
     * 4-aligned, no padding anywhere - the nine colours are a {@code uint32_t[9]} inside the struct,
     * not a pointer, so the sequence layout has to sit at offset 8 and be read in place.
     * <p>
     * Every flag is its own field because the C returned out of {@code queryUISettings} on the first
     * {@code RoException} and so dropped every later key too: a 0 flag means "no key at all for this
     * group", never "false" (the three {@code catch (RoException const&)} arms of
     * {@code PlatformSupport::queryUISettings} each {@code return}). The three flags are monotone.
     * {@link #GWIN_SIZEOF_UI_SETTINGS} exists so that a drift in this layout fails a test rather
     * than silently misreading the struct.
     */
    static final StructLayout GWIN_UI_SETTINGS_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("available"),
            JAVA_INT.withName("colors_valid"),
            MemoryLayout.sequenceLayout(GWIN_UI_COLOR_COUNT, JAVA_INT).withName("colors"),
            JAVA_INT.withName("advanced_effects_valid"),
            JAVA_INT.withName("advanced_effects_enabled"),
            JAVA_INT.withName("auto_hide_valid"),
            JAVA_INT.withName("auto_hide_scroll_bars"));

    /** {@code GwinNetworkInfo} ({@code glass_win_api.h}): two {@code int32_t}, 8 bytes. */
    static final StructLayout GWIN_NETWORK_INFO_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("available"),
            JAVA_INT.withName("cost_type"));

    /**
     * {@code GwinPrefsCallbacks} ({@code glass_win_api.h}): one function pointer, which is the single
     * upcall of {@code PlatformSupport.cpp} that survives - the preferences-changed <em>event</em>.
     * The library copies the table by value, so this may be built in a confined arena and closed; it
     * is the stub inside it that has to outlive the process.
     */
    static final StructLayout GWIN_PREFS_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("preferences_changed"));

    /**
     * {@code GwinAppCallbacks} ({@code glass_win_api.h}): one function pointer, which is all three of
     * {@code GlassApplication.cpp}'s former {@code Runnable.run} upcalls at once - the launchable
     * ({@code :398}), {@code _invokeAndWait} ({@code :469}) and {@code _submitForLaterInvocation}
     * ({@code :489}) all went through the single cached {@code javaIDs.Runnable.run} ({@code :344}).
     * <p>
     * It is a second table rather than a slot appended to {@link #GWIN_PREFS_CALLBACKS_LAYOUT}: the
     * preferences table lives beside the WinRT state it serves, this one beside the message pump, and
     * two tables mean neither struct ever changes shape.
     */
    static final StructLayout GWIN_APP_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("run_runnable"));

    /**
     * {@code GwinMenuCallbacks} ({@code glass_win_api.h}): one function pointer, the only upcall
     * {@code GlassMenu.cpp} ever made ({@code :83-84}, the one {@code CallStaticBooleanMethod} in the whole
     * file). Unlike {@link #GWIN_APP_CALLBACKS_LAYOUT}'s slot this one <em>returns</em>, and the
     * return is load-bearing.
     */
    static final StructLayout GWIN_MENU_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("notify_command"));

    /**
     * {@code GwinViewCallbacks} ({@code glass_win_api.h}): the ten former {@code javaIDs.View} method
     * ids of {@code GlassView.cpp}'s {@code _initIDs}, in declaration order. Every slot's first
     * parameter is the {@code int64_t} view id {@code WinView} assigned; there is no table-level
     * {@code void* user} on this table or the next, deliberately - see {@link #installViewCallbacks}.
     * {@code gwin_sizeof_view_callbacks} pins the size; {@link #VIEW_SLOT_NAMES} is the order the
     * installer writes the stubs in, and the order {@code gwin_test_fire_callback} numbers them, 0..9.
     */
    static final StructLayout GWIN_VIEW_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("notify_view"),
            ADDRESS.withName("notify_resize"),
            ADDRESS.withName("notify_repaint"),
            ADDRESS.withName("notify_menu"),
            ADDRESS.withName("notify_key"),
            ADDRESS.withName("notify_mouse"),
            ADDRESS.withName("notify_scroll"),
            ADDRESS.withName("notify_input_method"),
            ADDRESS.withName("notify_ime_candidate_pos_request"),
            ADDRESS.withName("get_accessible"));

    /**
     * {@code GwinGestureCallbacks} ({@code glass_win_api.h}): the five former {@code javaIDs.Gestures}
     * static method ids of {@code ViewContainer.cpp}, in declaration order;
     * {@code gwin_test_fire_callback} numbers them 100..104.
     */
    static final StructLayout GWIN_GESTURE_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("gesture_performed"),
            ADDRESS.withName("inertia_gesture_finished"),
            ADDRESS.withName("notify_begin_touch_event"),
            ADDRESS.withName("notify_next_touch_event"),
            ADDRESS.withName("notify_end_touch_event"));

    /**
     * {@code GwinWindowCallbacks} ({@code glass_win_api.h}): the six file-static {@code jmethodID}s
     * of {@code GlassWindow.cpp} plus {@code javaIDs.Window} (5) and {@code javaIDs.WinWindow} (1),
     * minus the never-called {@code midNotifyMoveToAnotherScreen}, plus {@code notify_dispose} in
     * place of the {@code DeleteGlobalRef} of {@code ~GlassWindow}: twelve function pointers, in
     * declaration order. Every slot's first parameter is the {@code int64_t} window id
     * {@code WinWindow} assigned; no table-level {@code void* user}, as for the view tables.
     * {@code gwin_sizeof_window_callbacks} pins the size; {@link #WINDOW_SLOT_NAMES} is the order the
     * installer writes the stubs in.
     */
    static final StructLayout GWIN_WINDOW_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("notify_close"),
            ADDRESS.withName("notify_destroy"),
            ADDRESS.withName("notify_moving"),
            ADDRESS.withName("notify_move"),
            ADDRESS.withName("notify_resize"),
            ADDRESS.withName("notify_scale_changed"),
            ADDRESS.withName("notify_focus"),
            ADDRESS.withName("notify_focus_disabled"),
            ADDRESS.withName("notify_focus_ungrab"),
            ADDRESS.withName("notify_delegate_ptr"),
            ADDRESS.withName("non_client_hit_test"),
            ADDRESS.withName("notify_dispose"));

    /**
     * {@code GwinClipboardCallbacks} ({@code glass_win_api.h}): the three method ids of
     * {@code WinSystemClipboard.initIDs} ({@code fosSerialize}, {@code actionPerformed} at its two
     * sites with their two sinks, {@code contentChanged}), the re-entered dispose export of
     * {@code GlassApplication::RegisterClipboardViewer}, the {@code setPtr} field write and
     * {@code ~ClipboardData}: seven function pointers, in declaration order, every one keyed by the
     * {@code int64_t} clipboard id. {@code gwin_sizeof_clipboard_callbacks} pins the size;
     * {@link #CLIPBOARD_SLOT_NAMES} is the order the installer writes the stubs in.
     */
    static final StructLayout GWIN_CLIPBOARD_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("fos_serialize"),
            ADDRESS.withName("action_performed"),
            ADDRESS.withName("drag_action_performed"),
            ADDRESS.withName("content_changed"),
            ADDRESS.withName("dispose_peer"),
            ADDRESS.withName("set_data_object"),
            ADDRESS.withName("data_object_disposed"));

    /**
     * {@code GwinDndCallbacks} ({@code glass_win_api.h}): the four {@code View.notifyDrag*}
     * ids {@code GlassDnD.cpp} dialled through {@code javaIDs.View}, keyed by {@code WinView}'s view id, and
     * the five {@code WinDnDClipboard} members it reached through {@code ClassForName} +
     * {@code getInstance}, which carry no id at all - their peer is the singleton
     * {@code getInstance()} creates on demand. Nine function pointers, in declaration order;
     * {@code gwin_sizeof_dnd_callbacks} pins the size.
     */
    static final StructLayout GWIN_DND_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("drag_enter"),
            ADDRESS.withName("drag_over"),
            ADDRESS.withName("drag_drop"),
            ADDRESS.withName("drag_leave"),
            ADDRESS.withName("dnd_get_data_object"),
            ADDRESS.withName("dnd_set_data_object"),
            ADDRESS.withName("dnd_set_source_supported_actions"),
            ADDRESS.withName("dnd_set_drag_button"),
            ADDRESS.withName("dnd_get_drag_button"));

    /**
     * {@code GwinScreenCallbacks} ({@code glass_win_api.h}): one function pointer, the
     * {@code Screen.notifySettingsChanged()} upcall of {@code GlassScreen::HandleDisplayChange}. No
     * {@code void* user}: the event has no identity. {@code gwin_sizeof_screen_callbacks} pins the size;
     * the name is the one {@code glass_win_api.cpp}'s {@code static_assert} message cites.
     */
    static final StructLayout SCREEN_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("settings_changed"));

    /**
     * {@code GwinVariant} ({@code glass_win_api.h}): the flat form of {@link WinVariant}, the nine
     * fields {@code GlassAccessible::copyVariant} read through cached {@code jfieldID}s plus the two
     * lengths a flat ABI needs. Natural x64 alignment, so it has three padding holes and the size the
     * C compiler gives it is 72 bytes - {@code gwin_sizeof_variant} and {@code gwin_test_variant_offsets}
     * are what pin that, never this declaration.
     * <p>
     * {@code boolVal} crosses as an {@code int32_t} 0 / 1, because {@code java.lang.foreign} has no
     * boolean layout and the C turns it into {@code VARIANT_FALSE} / {@code VARIANT_TRUE} itself. A
     * {@code NULL} {@code bstr_val} / {@code p_dbl_val} is Java's {@code null} and is distinct from a
     * zero-length block: {@code copyString(NULL)} and {@code copyList(NULL)} answered {@code E_FAIL}
     * where an empty {@code String} or {@code double[]} answered {@code S_OK}.
     */
    static final StructLayout GWIN_VARIANT_LAYOUT = MemoryLayout.structLayout(
            JAVA_SHORT.withName("vt"),
            JAVA_SHORT.withName("i_val"),
            JAVA_INT.withName("l_val"),
            JAVA_FLOAT.withName("flt_val"),
            MemoryLayout.paddingLayout(4),
            JAVA_DOUBLE.withName("dbl_val"),
            JAVA_INT.withName("bool_val"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("punk_val"),
            ADDRESS.withName("bstr_val"),
            JAVA_INT.withName("bstr_len"),
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("p_dbl_val"),
            JAVA_INT.withName("p_dbl_count"),
            MemoryLayout.paddingLayout(4));

    /**
     * {@code GwinAccessibleCallbacks} ({@code glass_win_api.h}): the 69 {@code jmethodID}s of
     * {@code WinAccessible._initIDs} in that order - which is the UI Automation interface order of
     * {@code GlassAccessible.cpp} - plus {@code accessible_disposed}, which stands where
     * {@code DeleteGlobalRef} stood in {@code ~GlassAccessible}. Every slot's first parameter is the
     * {@code int64_t} id {@code WinAccessible} registered itself under; there is no table-level
     * {@code void* user}, for the reason {@link #installViewCallbacks} gives.
     * {@code gwin_sizeof_accessible_callbacks} pins the size; {@link #ACCESSIBLE_SLOT_NAMES} is the
     * order the installer writes the stubs in, and the order
     * {@code gwin_test_fire_accessible_callback} numbers them, 0..69.
     */
    static final StructLayout GWIN_ACCESSIBLE_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("get_pattern_provider"),
            ADDRESS.withName("get_host_raw_element_provider"),
            ADDRESS.withName("get_property_value"),
            ADDRESS.withName("get_bounding_rectangle"),
            ADDRESS.withName("get_fragment_root"),
            ADDRESS.withName("get_embedded_fragment_roots"),
            ADDRESS.withName("get_runtime_id"),
            ADDRESS.withName("navigate"),
            ADDRESS.withName("set_focus"),
            ADDRESS.withName("element_provider_from_point"),
            ADDRESS.withName("get_focus"),
            ADDRESS.withName("advise_event_added"),
            ADDRESS.withName("advise_event_removed"),
            ADDRESS.withName("invoke"),
            ADDRESS.withName("get_selection"),
            ADDRESS.withName("get_can_select_multiple"),
            ADDRESS.withName("get_is_selection_required"),
            ADDRESS.withName("select"),
            ADDRESS.withName("add_to_selection"),
            ADDRESS.withName("remove_from_selection"),
            ADDRESS.withName("get_is_selected"),
            ADDRESS.withName("get_selection_container"),
            ADDRESS.withName("set_value"),
            ADDRESS.withName("get_value"),
            ADDRESS.withName("get_is_read_only"),
            ADDRESS.withName("get_maximum"),
            ADDRESS.withName("get_minimum"),
            ADDRESS.withName("get_large_change"),
            ADDRESS.withName("get_small_change"),
            ADDRESS.withName("set_value_string"),
            ADDRESS.withName("get_value_string"),
            ADDRESS.withName("get_visible_ranges"),
            ADDRESS.withName("range_from_child"),
            ADDRESS.withName("range_from_point"),
            ADDRESS.withName("get_document_range"),
            ADDRESS.withName("get_supported_text_selection"),
            ADDRESS.withName("get_column_count"),
            ADDRESS.withName("get_row_count"),
            ADDRESS.withName("get_item"),
            ADDRESS.withName("get_column"),
            ADDRESS.withName("get_column_span"),
            ADDRESS.withName("get_containing_grid"),
            ADDRESS.withName("get_row"),
            ADDRESS.withName("get_row_span"),
            ADDRESS.withName("get_column_headers"),
            ADDRESS.withName("get_row_headers"),
            ADDRESS.withName("get_row_or_column_major"),
            ADDRESS.withName("get_column_header_items"),
            ADDRESS.withName("get_row_header_items"),
            ADDRESS.withName("toggle"),
            ADDRESS.withName("get_toggle_state"),
            ADDRESS.withName("collapse"),
            ADDRESS.withName("expand"),
            ADDRESS.withName("get_expand_collapse_state"),
            ADDRESS.withName("get_can_move"),
            ADDRESS.withName("get_can_resize"),
            ADDRESS.withName("get_can_rotate"),
            ADDRESS.withName("move"),
            ADDRESS.withName("resize"),
            ADDRESS.withName("rotate"),
            ADDRESS.withName("scroll"),
            ADDRESS.withName("set_scroll_percent"),
            ADDRESS.withName("get_horizontally_scrollable"),
            ADDRESS.withName("get_horizontal_scroll_percent"),
            ADDRESS.withName("get_horizontal_view_size"),
            ADDRESS.withName("get_vertically_scrollable"),
            ADDRESS.withName("get_vertical_scroll_percent"),
            ADDRESS.withName("get_vertical_view_size"),
            ADDRESS.withName("scroll_into_view"),
            ADDRESS.withName("accessible_disposed"));

    /**
     * {@code GwinTextRangeCallbacks} ({@code glass_win_api.h}): the 18 {@code jmethodID}s of
     * {@code WinTextRangeProvider._initIDs} plus {@code range_disposed}, with
     * {@link #GWIN_ACCESSIBLE_CALLBACKS_LAYOUT}'s rules. {@code gwin_sizeof_text_range_callbacks} pins
     * the size; {@code gwin_test_fire_text_range_callback} numbers the slots 0..18.
     */
    static final StructLayout GWIN_TEXT_RANGE_CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("clone"),
            ADDRESS.withName("compare"),
            ADDRESS.withName("compare_endpoints"),
            ADDRESS.withName("expand_to_enclosing_unit"),
            ADDRESS.withName("find_attribute"),
            ADDRESS.withName("find_text"),
            ADDRESS.withName("get_attribute_value"),
            ADDRESS.withName("get_bounding_rectangles"),
            ADDRESS.withName("get_enclosing_element"),
            ADDRESS.withName("get_text"),
            ADDRESS.withName("move"),
            ADDRESS.withName("move_endpoint_by_unit"),
            ADDRESS.withName("move_endpoint_by_range"),
            ADDRESS.withName("select"),
            ADDRESS.withName("add_to_selection"),
            ADDRESS.withName("remove_from_selection"),
            ADDRESS.withName("scroll_into_view"),
            ADDRESS.withName("get_children"),
            ADDRESS.withName("range_disposed"));

    /**
     * {@code GwinFileFilter} ({@code glass_win_api.h}): one
     * {@code CommonDialogs.ExtensionFilter} flattened by Java - the description and the extensions
     * joined with {@code ';'}, both NUL-terminated UTF-16 and never NULL. Two pointers;
     * {@code gwin_sizeof_file_filter} pins the size.
     */
    static final StructLayout GWIN_FILE_FILTER_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("description"),
            ADDRESS.withName("extensions"));

    /**
     * {@code MENUITEMINFOW} (winuser.h {@code tagMENUITEMINFOW}) on LLP64, which is the only shape
     * this fork builds for Windows: 80 bytes, with four bytes of padding after the {@code UINT wID}
     * because {@code HMENU hSubMenu} is pointer-aligned, and four more after the {@code UINT cch}
     * because {@code HBITMAP hbmpItem} is. Offsets 0, 4, 8, 12, 16, (pad), 24, 32, 40, 48, 56, 64,
     * (pad), 72.
     * <p>
     * <b>Getting this wrong does not fail loudly.</b> {@code cbSize} is not a checksum: user32 accepts
     * both {@code sizeof(MENUITEMINFOW)} and the pre-Windows-2000 compatibility size
     * {@code sizeof(MENUITEMINFOW) - sizeof(HBITMAP)}, which on x64 is exactly the 72 bytes this
     * layout would have if the first padding were forgotten. Every pointer field would then be eight
     * bytes low and {@code MIIM_STRING} would make user32 dereference whatever sat where
     * {@code hbmpUnchecked} was written - an access violation inside user32, not a rejected call. What
     * actually pins it is {@code WinMenuNativeTest}: the golden transcript it compares against was
     * captured by reading menus <em>built by the JNI</em>, that is by the C compiler's own
     * {@code MENUITEMINFOW}, through this layout. A layout that disagrees with the C struct cannot
     * produce that transcript.
     *
     * @see <a href="https://learn.microsoft.com/windows/win32/api/winuser/ns-winuser-menuiteminfow">MENUITEMINFOW</a>
     */
    static final StructLayout MENUITEMINFOW_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("cbSize"),
            JAVA_INT.withName("fMask"),
            JAVA_INT.withName("fType"),
            JAVA_INT.withName("fState"),
            JAVA_INT.withName("wID"),
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("hSubMenu"),
            ADDRESS.withName("hbmpChecked"),
            ADDRESS.withName("hbmpUnchecked"),
            JAVA_LONG.withName("dwItemData"),
            ADDRESS.withName("dwTypeData"),
            JAVA_INT.withName("cch"),
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("hbmpItem"));

    /**
     * {@code MONITORINFOEXW} (winuser.h): 104 bytes, no padding - {@code cbSize} at 0, the two
     * {@link #RECT_LAYOUT}s at 4 and 20, {@code dwFlags} at 36, then {@code WCHAR szDevice[CCHDEVICENAME]}
     * in place at 40. {@code GetMonitorSettings} passed the struct with {@code cbSize = sizeof(MONITORINFOEX)},
     * which is what makes {@code GetMonitorInfoW} fill {@code szDevice} at all, and then handed
     * {@code szDevice}'s address to {@code CreateDCW}. Pinned by {@code static_assert} in
     * {@code glass_win_api.cpp} (on the {@code MONITORINFO} base, which is a standard-layout type).
     */
    static final StructLayout MONITORINFOEXW_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("cbSize"),
            RECT_LAYOUT.withName("rcMonitor"),
            RECT_LAYOUT.withName("rcWork"),
            JAVA_INT.withName("dwFlags"),
            MemoryLayout.sequenceLayout(CCHDEVICENAME, JAVA_CHAR).withName("szDevice"));

    private static final long INPUT_TYPE_OFFSET = INPUT_LAYOUT.byteOffset(PathElement.groupElement("type"));
    private static final long MI_DX_OFFSET = inputOffset("mi", "dx");
    private static final long MI_DY_OFFSET = inputOffset("mi", "dy");
    private static final long MI_MOUSE_DATA_OFFSET = inputOffset("mi", "mouseData");
    private static final long MI_FLAGS_OFFSET = inputOffset("mi", "dwFlags");
    private static final long KI_WVK_OFFSET = inputOffset("ki", "wVk");
    private static final long KI_WSCAN_OFFSET = inputOffset("ki", "wScan");
    private static final long KI_FLAGS_OFFSET = inputOffset("ki", "dwFlags");
    private static final long POINT_X_OFFSET = POINT_LAYOUT.byteOffset(PathElement.groupElement("x"));
    private static final long POINT_Y_OFFSET = POINT_LAYOUT.byteOffset(PathElement.groupElement("y"));
    private static final long RECT_LEFT_OFFSET = RECT_LAYOUT.byteOffset(PathElement.groupElement("left"));
    private static final long RECT_TOP_OFFSET = RECT_LAYOUT.byteOffset(PathElement.groupElement("top"));
    private static final long RECT_RIGHT_OFFSET = RECT_LAYOUT.byteOffset(PathElement.groupElement("right"));
    private static final long RECT_BOTTOM_OFFSET = RECT_LAYOUT.byteOffset(PathElement.groupElement("bottom"));
    private static final long TIMECAPS_MIN_OFFSET =
            TIMECAPS_LAYOUT.byteOffset(PathElement.groupElement("wPeriodMin"));
    private static final long TIMECAPS_MAX_OFFSET =
            TIMECAPS_LAYOUT.byteOffset(PathElement.groupElement("wPeriodMax"));
    private static final long BMI_SIZE_OFFSET = bitmapInfoOffset("biSize");
    private static final long BMI_WIDTH_OFFSET = bitmapInfoOffset("biWidth");
    private static final long BMI_HEIGHT_OFFSET = bitmapInfoOffset("biHeight");
    private static final long BMI_PLANES_OFFSET = bitmapInfoOffset("biPlanes");
    private static final long BMI_BIT_COUNT_OFFSET = bitmapInfoOffset("biBitCount");
    private static final long BMI_COMPRESSION_OFFSET = bitmapInfoOffset("biCompression");
    private static final long BMI_SIZE_IMAGE_OFFSET = bitmapInfoOffset("biSizeImage");
    private static final long ICONINFO_FICON_OFFSET = iconInfoOffset("fIcon");
    private static final long ICONINFO_XHOTSPOT_OFFSET = iconInfoOffset("xHotspot");
    private static final long ICONINFO_YHOTSPOT_OFFSET = iconInfoOffset("yHotspot");
    private static final long ICONINFO_HBMMASK_OFFSET = iconInfoOffset("hbmMask");
    private static final long ICONINFO_HBMCOLOR_OFFSET = iconInfoOffset("hbmColor");
    private static final long HC_CBSIZE_OFFSET = highContrastOffset("cbSize");
    private static final long HC_FLAGS_OFFSET = highContrastOffset("dwFlags");
    private static final long HC_SCHEME_OFFSET = highContrastOffset("lpszDefaultScheme");
    private static final long UI_AVAILABLE_OFFSET = uiSettingsOffset("available");
    private static final long UI_COLORS_VALID_OFFSET = uiSettingsOffset("colors_valid");
    private static final long UI_COLORS_OFFSET = uiSettingsOffset("colors");
    private static final long UI_ADVANCED_VALID_OFFSET = uiSettingsOffset("advanced_effects_valid");
    private static final long UI_ADVANCED_ENABLED_OFFSET = uiSettingsOffset("advanced_effects_enabled");
    private static final long UI_AUTO_HIDE_VALID_OFFSET = uiSettingsOffset("auto_hide_valid");
    private static final long UI_AUTO_HIDE_OFFSET = uiSettingsOffset("auto_hide_scroll_bars");
    private static final long NET_AVAILABLE_OFFSET = networkInfoOffset("available");
    private static final long NET_COST_TYPE_OFFSET = networkInfoOffset("cost_type");
    private static final long PREFS_CHANGED_OFFSET =
            GWIN_PREFS_CALLBACKS_LAYOUT.byteOffset(PathElement.groupElement("preferences_changed"));
    private static final long RUN_RUNNABLE_OFFSET =
            GWIN_APP_CALLBACKS_LAYOUT.byteOffset(PathElement.groupElement("run_runnable"));
    private static final long NOTIFY_COMMAND_OFFSET =
            GWIN_MENU_CALLBACKS_LAYOUT.byteOffset(PathElement.groupElement("notify_command"));
    private static final int MENUITEMINFOW_SIZE = (int) MENUITEMINFOW_LAYOUT.byteSize();
    private static final long MII_CBSIZE_OFFSET = menuItemInfoOffset("cbSize");
    private static final long MII_FMASK_OFFSET = menuItemInfoOffset("fMask");
    private static final long MII_FTYPE_OFFSET = menuItemInfoOffset("fType");
    private static final long MII_FSTATE_OFFSET = menuItemInfoOffset("fState");
    private static final long MII_WID_OFFSET = menuItemInfoOffset("wID");
    private static final long MII_HSUBMENU_OFFSET = menuItemInfoOffset("hSubMenu");
    private static final long MII_DWTYPEDATA_OFFSET = menuItemInfoOffset("dwTypeData");
    private static final long MII_CCH_OFFSET = menuItemInfoOffset("cch");
    private static final long MIX_CBSIZE_OFFSET = monitorInfoOffset("cbSize");
    private static final long MIX_MONITOR_OFFSET = monitorInfoOffset("rcMonitor");
    private static final long MIX_WORK_OFFSET = monitorInfoOffset("rcWork");
    private static final long MIX_FLAGS_OFFSET = monitorInfoOffset("dwFlags");
    private static final long MIX_DEVICE_OFFSET = monitorInfoOffset("szDevice");
    private static final long SETTINGS_CHANGED_OFFSET =
            SCREEN_CALLBACKS_LAYOUT.byteOffset(PathElement.groupElement("settings_changed"));

    private static final String LIBRARY_NAME = "glass";
    private static final String USER32_NAME = "user32";
    private static final String KERNEL32_NAME = "kernel32";
    private static final String OLE32_NAME = "ole32";

    private static final Linker LINKER = Linker.nativeLinker();
    /**
     * Copy-on-write because the winmm block below binds lazily: an append from the thread that makes
     * the first timer call can otherwise race a {@link #boundSymbols()} read on another thread.
     */
    private static final List<String> BOUND_SYMBOLS = new CopyOnWriteArrayList<>();

    /** {@code glass.dll}, loaded the way every Glass class loads it, then resolved by the class loader. */
    private static final SymbolLookup GLASS;

    static {
        // The library has to be loaded by this class loader before loaderLookup() can see it.
        // WinApplication loads it too; NativeLibLoader and System.loadLibrary are both idempotent.
        NativeLibLoader.loadLibrary(LIBRARY_NAME);
        GLASS = SymbolLookup.loaderLookup();
    }

    /** A system DLL the JDK does not load for us, looked up once for the life of the process. */
    private static final SymbolLookup USER32 = systemLibrary(USER32_NAME + ".dll");

    /**
     * {@code kernel32.dll}, for the one function of it this package needs: {@code GetVersion}, which
     * is what {@code Utils.h}'s {@code IS_WINVER_ATLEAST} macro asked. Eager, like {@code user32}:
     * {@code kernel32} is mapped into every Win32 process before any Java code runs, so there is
     * nothing to defer and nothing that can fail here that has not failed already.
     */
    private static final SymbolLookup KERNEL32 = systemLibrary(KERNEL32_NAME + ".dll");

    /**
     * {@code ole32.dll}, for the one function of it this package needs: {@code OleIsCurrentClipboard},
     * the whole of the former {@code WinSystemClipboard.isOwner} body. Eager, like
     * {@code user32}: {@code glass.dll} imports {@code ole32} itself, so it is mapped before this
     * class can load, and a lookup that cannot fail has nothing to defer.
     */
    private static final SymbolLookup OLE32 = systemLibrary(OLE32_NAME + ".dll");

    /* Binding order matters: the ABI guard is bound and checked before any other gwin_* symbol. */
    private static final MethodHandle GWIN_ABI_VERSION = bindGlass("gwin_abi_version",
            FunctionDescriptor.of(JAVA_INT));

    static {
        checkAbiVersion();
    }

    private static final MethodHandle GWIN_ROBOT_CAPTURE = bindGlass("gwin_robot_capture",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG));
    private static final MethodHandle GWIN_KEY_JAVA_TO_WINDOWS = bindGlass("gwin_key_java_to_windows",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));
    private static final MethodHandle GWIN_KEY_WINDOWS_TO_JAVA = bindGlass("gwin_key_windows_to_java",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    /**
     * {@code int32_t gwin_key_code_for_char(uint16_t c, int32_t hint)}. FFM has no {@code char}
     * layout, so the UTF-16 code unit crosses as a {@code JAVA_SHORT} and the C reads the same 16
     * bits; see {@link #keyCodeForChar}.
     */
    private static final MethodHandle GWIN_KEY_CODE_FOR_CHAR = bindGlass("gwin_key_code_for_char",
            FunctionDescriptor.of(JAVA_INT, JAVA_SHORT, JAVA_INT));

    /** {@code int32_t gwin_sizeof_ui_settings(void)} - the C's own {@code sizeof(GwinUiSettings)}. */
    private static final MethodHandle GWIN_SIZEOF_UI_SETTINGS = bindGlass("gwin_sizeof_ui_settings",
            FunctionDescriptor.of(JAVA_INT));

    /** {@code int32_t gwin_sizeof_network_info(void)}. */
    private static final MethodHandle GWIN_SIZEOF_NETWORK_INFO = bindGlass("gwin_sizeof_network_info",
            FunctionDescriptor.of(JAVA_INT));

    /**
     * {@code int32_t gwin_prefs_set_callbacks(const GwinPrefsCallbacks* cb, void* user)}. Installing a
     * table replaces the JNI notification path wholesale, so it happens exactly once, from
     * {@code WinApplication}'s static initializer - see {@link #installPreferencesCallback}.
     */
    private static final MethodHandle GWIN_PREFS_SET_CALLBACKS = bindGlass("gwin_prefs_set_callbacks",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code int32_t gwin_prefs_query_ui_settings(GwinUiSettings* out)}; zero-fills {@code out}. */
    private static final MethodHandle GWIN_PREFS_QUERY_UI_SETTINGS =
            bindGlass("gwin_prefs_query_ui_settings", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int32_t gwin_prefs_query_network(GwinNetworkInfo* out)}; zero-fills {@code out}. */
    private static final MethodHandle GWIN_PREFS_QUERY_NETWORK =
            bindGlass("gwin_prefs_query_network", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int32_t gwin_sizeof_app_callbacks(void)}; {@link #GWIN_APP_CALLBACKS_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_APP_CALLBACKS = bindGlass("gwin_sizeof_app_callbacks",
            FunctionDescriptor.of(JAVA_INT));

    /**
     * {@code int32_t gwin_app_set_callbacks(const GwinAppCallbacks* cb, void* user)} - see
     * {@link #installApplicationCallback}.
     */
    private static final MethodHandle GWIN_APP_SET_CALLBACKS = bindGlass("gwin_app_set_callbacks",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /**
     * {@code void* gwin_app_create(void)}: {@code _init}'s body without its DPI step - the toolkit HWND, or
     * NULL. Creates a window and initializes WinRT on the calling thread; see {@link #appCreate()}.
     */
    private static final MethodHandle GWIN_APP_CREATE = bindGlass("gwin_app_create",
            FunctionDescriptor.of(ADDRESS));

    /** {@code void gwin_run_loop(int64_t launchable_id)}; blocks for the life of the application. */
    private static final MethodHandle GWIN_RUN_LOOP = bindGlass("gwin_run_loop",
            FunctionDescriptor.ofVoid(JAVA_LONG));

    /** {@code void gwin_terminate_loop(void)}. */
    private static final MethodHandle GWIN_TERMINATE_LOOP = bindGlass("gwin_terminate_loop",
            FunctionDescriptor.ofVoid());

    /** {@code void gwin_enter_nested_event_loop(void)}; blocks and dispatches. */
    private static final MethodHandle GWIN_ENTER_NESTED_EVENT_LOOP =
            bindGlass("gwin_enter_nested_event_loop", FunctionDescriptor.ofVoid());

    /** {@code void gwin_leave_nested_event_loop(void)}. */
    private static final MethodHandle GWIN_LEAVE_NESTED_EVENT_LOOP =
            bindGlass("gwin_leave_nested_event_loop", FunctionDescriptor.ofVoid());

    /** {@code int32_t gwin_invoke_and_wait(int64_t runnable_id)}; blocks until the runnable has run. */
    private static final MethodHandle GWIN_INVOKE_AND_WAIT = bindGlass("gwin_invoke_and_wait",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));

    /** {@code int32_t gwin_invoke_later(int64_t runnable_id)}; returns as soon as it has posted. */
    private static final MethodHandle GWIN_INVOKE_LATER = bindGlass("gwin_invoke_later",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));

    /** {@code int32_t gwin_sizeof_menu_callbacks(void)}; {@link #GWIN_MENU_CALLBACKS_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_MENU_CALLBACKS =
            bindGlass("gwin_sizeof_menu_callbacks", FunctionDescriptor.of(JAVA_INT));

    /**
     * {@code int32_t gwin_menu_set_callbacks(const GwinMenuCallbacks* cb, void* user)} - see
     * {@link #installMenuCallback}.
     */
    private static final MethodHandle GWIN_MENU_SET_CALLBACKS = bindGlass("gwin_menu_set_callbacks",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /*
     * The view section of glass_win_api.h, in the header's order. None of these is critical:
     * eight of them block in SendMessage(WM_DO_ACTION) and three of those upcall from inside the
     * downcall (set_parent, enter_fullscreen, exit_fullscreen); the three that do not marshal take no
     * Java array. See viewUploadPixels for the one that carries a buffer, and why it is copied.
     */

    /** {@code int32_t gwin_sizeof_view_callbacks(void)}; {@link #GWIN_VIEW_CALLBACKS_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_VIEW_CALLBACKS =
            bindGlass("gwin_sizeof_view_callbacks", FunctionDescriptor.of(JAVA_INT));

    /** {@code int32_t gwin_sizeof_gesture_callbacks(void)}; {@link #GWIN_GESTURE_CALLBACKS_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_GESTURE_CALLBACKS =
            bindGlass("gwin_sizeof_gesture_callbacks", FunctionDescriptor.of(JAVA_INT));

    /** {@code int32_t gwin_view_set_callbacks(const GwinViewCallbacks* cb)} - no {@code void* user}. */
    private static final MethodHandle GWIN_VIEW_SET_CALLBACKS = bindGlass("gwin_view_set_callbacks",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int32_t gwin_gesture_set_callbacks(const GwinGestureCallbacks* cb)} - no {@code void* user}. */
    private static final MethodHandle GWIN_GESTURE_SET_CALLBACKS = bindGlass("gwin_gesture_set_callbacks",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * {@code int64_t gwin_test_fire_callback(int32_t slot, int64_t view_id)} - the test hook that
     * drives one slot of an installed table with a fixed pattern. It returns {@code int64_t}, not
     * {@code int32_t}, so that slot 9's {@code get_accessible} value round-trips untruncated; binding
     * it as {@code JAVA_INT} would read half a return register and never notice.
     */
    private static final MethodHandle GWIN_TEST_FIRE_CALLBACK = bindGlass("gwin_test_fire_callback",
            FunctionDescriptor.of(JAVA_LONG, JAVA_INT, JAVA_LONG));

    /** {@code gwin_view_t gwin_view_create(int64_t view_id)}: {@code new GlassView(view_id)}, NULL if it threw. */
    private static final MethodHandle GWIN_VIEW_CREATE = bindGlass("gwin_view_create",
            FunctionDescriptor.of(ADDRESS, JAVA_LONG));

    /** {@code int32_t gwin_view_close(gwin_view_t view)}; blocks and upcalls, {@code view} dangles after. */
    private static final MethodHandle GWIN_VIEW_CLOSE = bindGlass("gwin_view_close",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code void* gwin_view_get_native_view(gwin_view_t view)}: the host HWND, no thread marshal. */
    private static final MethodHandle GWIN_VIEW_GET_NATIVE_VIEW = bindGlass("gwin_view_get_native_view",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    /** {@code void gwin_view_set_parent(gwin_view_t view, void* parent_hwnd)}; blocks and upcalls. */
    private static final MethodHandle GWIN_VIEW_SET_PARENT = bindGlass("gwin_view_set_parent",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /** {@code int32_t gwin_view_get_x(gwin_view_t view)}; blocks. */
    private static final MethodHandle GWIN_VIEW_GET_X = bindGlass("gwin_view_get_x",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int32_t gwin_view_get_y(gwin_view_t view)}; blocks. */
    private static final MethodHandle GWIN_VIEW_GET_Y = bindGlass("gwin_view_get_y",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code void gwin_view_schedule_repaint(gwin_view_t view)}; blocks. */
    private static final MethodHandle GWIN_VIEW_SCHEDULE_REPAINT = bindGlass("gwin_view_schedule_repaint",
            FunctionDescriptor.ofVoid(ADDRESS));

    /**
     * {@code void gwin_view_upload_pixels(gwin_view_t view, int32_t width, int32_t height,
     * const void* bits)}; blocks. {@code bits} is borrowed for the call and may be {@code NULL}.
     */
    private static final MethodHandle GWIN_VIEW_UPLOAD_PIXELS = bindGlass("gwin_view_upload_pixels",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));

    /**
     * {@code int32_t gwin_view_enter_fullscreen(gwin_view_t view, int32_t animate, int32_t keep_ratio,
     * int32_t hide_cursor)}; blocks and upcalls. {@code hide_cursor} is accepted and ignored, as the
     * JNI ignored it.
     */
    private static final MethodHandle GWIN_VIEW_ENTER_FULLSCREEN = bindGlass("gwin_view_enter_fullscreen",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));

    /** {@code void gwin_view_exit_fullscreen(gwin_view_t view, int32_t animate)}; blocks and upcalls. */
    private static final MethodHandle GWIN_VIEW_EXIT_FULLSCREEN = bindGlass("gwin_view_exit_fullscreen",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /** {@code void gwin_view_enable_ime(gwin_view_t view, int32_t enable)}: only 1 enables; no marshal. */
    private static final MethodHandle GWIN_VIEW_ENABLE_IME = bindGlass("gwin_view_enable_ime",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /** {@code void gwin_view_finish_ime_composition(gwin_view_t view)}; no thread marshal. */
    private static final MethodHandle GWIN_VIEW_FINISH_IME_COMPOSITION =
            bindGlass("gwin_view_finish_ime_composition", FunctionDescriptor.ofVoid(ADDRESS));

    /*
     * The window section of glass_win_api.h, in the header's order. critical(true) is
     * FORBIDDEN on every one of them: twenty-five block in SendMessage(WM_DO_ACTION), most of those
     * can upcall, show_system_menu runs a nested modal message loop, and the two that neither block
     * nor upcall (get_insets, set_icon) take no Java array.
     */

    /** {@code int32_t gwin_sizeof_window_callbacks(void)}; {@link #GWIN_WINDOW_CALLBACKS_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_WINDOW_CALLBACKS =
            bindGlass("gwin_sizeof_window_callbacks", FunctionDescriptor.of(JAVA_INT));

    /** {@code int32_t gwin_window_set_callbacks(const GwinWindowCallbacks* cb)} - no {@code void* user}, always OK. */
    private static final MethodHandle GWIN_WINDOW_SET_CALLBACKS = bindGlass("gwin_window_set_callbacks",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * {@code gwin_window_t gwin_window_create(void* owner_hwnd, void* monitor, int32_t mask,
     * int64_t window_id)}: the HWND, or NULL. Blocks and upcalls ({@code notify_dispose} on failure).
     */
    private static final MethodHandle GWIN_WINDOW_CREATE = bindGlass("gwin_window_create",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_LONG));

    /** {@code int32_t gwin_window_close(gwin_window_t win)}; blocks and upcalls. */
    private static final MethodHandle GWIN_WINDOW_CLOSE = bindGlass("gwin_window_close",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int32_t gwin_window_set_view(gwin_window_t win, gwin_view_t view)}; always 1; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_VIEW = bindGlass("gwin_window_set_view",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code void gwin_window_update_view_size(gwin_window_t win)}; blocks and upcalls (view table). */
    private static final MethodHandle GWIN_WINDOW_UPDATE_VIEW_SIZE = bindGlass("gwin_window_update_view_size",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code int32_t gwin_window_set_menubar(gwin_window_t win, void* hmenu)}; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_MENUBAR = bindGlass("gwin_window_set_menubar",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code void gwin_window_set_level(gwin_window_t win, int32_t level)}; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_LEVEL = bindGlass("gwin_window_set_level",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /** {@code void gwin_window_set_focusable(gwin_window_t win, int32_t focusable)}: only 1 is true; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_FOCUSABLE = bindGlass("gwin_window_set_focusable",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /** {@code void gwin_window_set_enabled(gwin_window_t win, int32_t enabled)}: only 1 is true; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_ENABLED = bindGlass("gwin_window_set_enabled",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /** {@code void gwin_window_set_alpha(gwin_window_t win, float alpha)}; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_ALPHA = bindGlass("gwin_window_set_alpha",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_FLOAT));

    /** {@code void gwin_window_set_dark_frame(gwin_window_t win, int32_t dark)}; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_DARK_FRAME = bindGlass("gwin_window_set_dark_frame",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /** {@code int64_t gwin_window_get_insets(gwin_window_t win)}: packed, 0 when not a window; no marshal. */
    private static final MethodHandle GWIN_WINDOW_GET_INSETS = bindGlass("gwin_window_get_insets",
            FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    /**
     * {@code void gwin_window_set_bounds(gwin_window_t win, int32_t x, int32_t y, int32_t x_set,
     * int32_t y_set, int32_t w, int32_t h, int32_t cw, int32_t ch, float x_gravity, float y_gravity)};
     * blocks. The two floats are accepted and ignored, as the JNI ignored them.
     */
    private static final MethodHandle GWIN_WINDOW_SET_BOUNDS = bindGlass("gwin_window_set_bounds",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_FLOAT, JAVA_FLOAT));

    /**
     * {@code int32_t gwin_window_set_title(gwin_window_t win, const uint16_t* title)}: NUL-terminated
     * UTF-16; blocks.
     */
    private static final MethodHandle GWIN_WINDOW_SET_TITLE = bindGlass("gwin_window_set_title",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code int32_t gwin_window_set_resizable(gwin_window_t win, int32_t resizable)}: only 1 is true; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_RESIZABLE = bindGlass("gwin_window_set_resizable",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /**
     * {@code int32_t gwin_window_set_visible(gwin_window_t win, int32_t visible)}: returns its
     * argument; blocks and upcalls.
     */
    private static final MethodHandle GWIN_WINDOW_SET_VISIBLE = bindGlass("gwin_window_set_visible",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /** {@code int32_t gwin_window_request_focus(gwin_window_t win, int32_t event)}; blocks. */
    private static final MethodHandle GWIN_WINDOW_REQUEST_FOCUS = bindGlass("gwin_window_request_focus",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /** {@code int32_t gwin_window_grab_focus(gwin_window_t win)}; blocks and upcalls. */
    private static final MethodHandle GWIN_WINDOW_GRAB_FOCUS = bindGlass("gwin_window_grab_focus",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code void gwin_window_ungrab_focus(gwin_window_t win)}; blocks and upcalls. */
    private static final MethodHandle GWIN_WINDOW_UNGRAB_FOCUS = bindGlass("gwin_window_ungrab_focus",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code int32_t gwin_window_minimize(gwin_window_t win, int32_t minimize)}: always 1; blocks and upcalls. */
    private static final MethodHandle GWIN_WINDOW_MINIMIZE = bindGlass("gwin_window_minimize",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /** {@code int32_t gwin_window_maximize(gwin_window_t win, int32_t maximize, int32_t was_maximized)}: always 1. */
    private static final MethodHandle GWIN_WINDOW_MAXIMIZE = bindGlass("gwin_window_maximize",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));

    /** {@code int32_t gwin_window_set_min_size(gwin_window_t win, int32_t width, int32_t height)}; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_MIN_SIZE = bindGlass("gwin_window_set_min_size",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));

    /** {@code int32_t gwin_window_set_max_size(gwin_window_t win, int32_t width, int32_t height)}; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_MAX_SIZE = bindGlass("gwin_window_set_max_size",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));

    /** {@code void gwin_window_set_icon(gwin_window_t win, void* hicon)}: ownership transfers; no marshal. */
    private static final MethodHandle GWIN_WINDOW_SET_ICON = bindGlass("gwin_window_set_icon",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /** {@code void gwin_window_to_front(gwin_window_t win)}; blocks. */
    private static final MethodHandle GWIN_WINDOW_TO_FRONT = bindGlass("gwin_window_to_front",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code void gwin_window_to_back(gwin_window_t win)}; blocks. */
    private static final MethodHandle GWIN_WINDOW_TO_BACK = bindGlass("gwin_window_to_back",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code void gwin_window_set_cursor(gwin_window_t win, void* hcursor)}; blocks. */
    private static final MethodHandle GWIN_WINDOW_SET_CURSOR = bindGlass("gwin_window_set_cursor",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

    /** {@code void gwin_window_show_system_menu(gwin_window_t win, int32_t x, int32_t y)}: a nested modal loop. */
    private static final MethodHandle GWIN_WINDOW_SHOW_SYSTEM_MENU = bindGlass("gwin_window_show_system_menu",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT));

    /*
     * The screen section of glass_win_api.h: the installer and sizeof probe of the one-slot
     * table HandleDisplayChange dials. Its test hook, gwin_test_fire_screen_callback, is bound by
     * WinGlassNativeShim, never here (as gwin_test_screen_anchor was, until it was deleted).
     */

    /** {@code int32_t gwin_sizeof_screen_callbacks(void)}; {@link #SCREEN_CALLBACKS_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_SCREEN_CALLBACKS =
            bindGlass("gwin_sizeof_screen_callbacks", FunctionDescriptor.of(JAVA_INT));

    /** {@code int32_t gwin_screen_set_callbacks(const GwinScreenCallbacks* cb)} - copied by value, always OK. */
    private static final MethodHandle GWIN_SCREEN_SET_CALLBACKS = bindGlass("gwin_screen_set_callbacks",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /*
     * The clipboard / drag-and-drop / common-dialog section of glass_win_api.h, in the
     * header's order. critical(true) is FORBIDDEN on every one of them: nine block in
     * SendMessage(WM_DO_ACTION) and can upcall, gwin_dnd_push and the two dialogs run nested modal
     * loops, gwin_clipboard_dispose pumps messages, the two set_callbacks publish a pointer the OLE
     * marshaller will dial, and gwin_alloc / gwin_free are called from inside an upcall.
     */

    /** {@code int32_t gwin_sizeof_clipboard_callbacks(void)}; {@link #GWIN_CLIPBOARD_CALLBACKS_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_CLIPBOARD_CALLBACKS =
            bindGlass("gwin_sizeof_clipboard_callbacks", FunctionDescriptor.of(JAVA_INT));

    /** {@code int32_t gwin_sizeof_dnd_callbacks(void)}; {@link #GWIN_DND_CALLBACKS_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_DND_CALLBACKS =
            bindGlass("gwin_sizeof_dnd_callbacks", FunctionDescriptor.of(JAVA_INT));

    /** {@code int32_t gwin_clipboard_set_callbacks(const GwinClipboardCallbacks* cb)} - copied by value, always OK. */
    private static final MethodHandle GWIN_CLIPBOARD_SET_CALLBACKS = bindGlass("gwin_clipboard_set_callbacks",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int32_t gwin_dnd_set_callbacks(const GwinDndCallbacks* cb)} - copied by value, always OK. */
    private static final MethodHandle GWIN_DND_SET_CALLBACKS = bindGlass("gwin_dnd_set_callbacks",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * {@code int64_t gwin_test_fire_clipboard_callback(int32_t slot, int64_t clipboard_id)}: the test
     * hook that drives one slot of the installed clipboard table with the header's fixed pattern -
     * the only automated check of the seven descriptors, as {@code gwin_test_fire_callback} is for
     * the view table. Frees the {@code fos_serialize} block itself.
     */
    private static final MethodHandle GWIN_TEST_FIRE_CLIPBOARD_CALLBACK =
            bindGlass("gwin_test_fire_clipboard_callback", FunctionDescriptor.of(JAVA_LONG, JAVA_INT, JAVA_LONG));

    /**
     * {@code int64_t gwin_test_fire_dnd_callback(int32_t slot, int64_t view_id, void* out)}: the same
     * hook for the drag table; {@code out} is a caller-owned 8-byte block handed unchanged to the slots
     * that have an out-parameter.
     */
    private static final MethodHandle GWIN_TEST_FIRE_DND_CALLBACK = bindGlass("gwin_test_fire_dnd_callback",
            FunctionDescriptor.of(JAVA_LONG, JAVA_INT, JAVA_LONG, ADDRESS));

    /**
     * {@code void* gwin_alloc(int64_t size)}: the library's {@code malloc}, so that the block
     * {@code fos_serialize} hands the C is one the C can {@code free}. {@code size <= 0} yields a
     * one-byte block, never NULL; NULL only when the heap is exhausted.
     */
    private static final MethodHandle GWIN_ALLOC = bindGlass("gwin_alloc",
            FunctionDescriptor.of(ADDRESS, JAVA_LONG));

    /**
     * {@code void gwin_free(void* block)}: releases a {@code gwin_alloc} block or any out-block below; NULL is a no-op.
     */
    private static final MethodHandle GWIN_FREE = bindGlass("gwin_free",
            FunctionDescriptor.ofVoid(ADDRESS));

    /**
     * {@code int32_t gwin_clipboard_register_viewer(int64_t clipboard_id)}; blocks, can upcall {@code dispose_peer}.
     */
    private static final MethodHandle GWIN_CLIPBOARD_REGISTER_VIEWER = bindGlass("gwin_clipboard_register_viewer",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));

    /** {@code void gwin_clipboard_dispose(gwin_clipboard_t clip)}; blocks, pumps, re-entrant. */
    private static final MethodHandle GWIN_CLIPBOARD_DISPOSE = bindGlass("gwin_clipboard_dispose",
            FunctionDescriptor.ofVoid(ADDRESS));

    /**
     * {@code int32_t gwin_clipboard_push(gwin_clipboard_t old, int64_t clipboard_id, const uint16_t*
     * mimes, int32_t mime_count, int32_t supported_actions)}: a status, not the handle - that
     * arrives through {@code set_data_object}. Blocks and upcalls.
     */
    private static final MethodHandle GWIN_CLIPBOARD_PUSH = bindGlass("gwin_clipboard_push",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT));

    /** {@code gwin_clipboard_t gwin_clipboard_pop(gwin_clipboard_t old)}: the new handle, or NULL. Blocks. */
    private static final MethodHandle GWIN_CLIPBOARD_POP = bindGlass("gwin_clipboard_pop",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    /**
     * {@code int32_t gwin_clipboard_pop_bytes(gwin_clipboard_t clip, const uint16_t* mime, int64_t
     * lindex, uint8_t** out_data, int32_t* out_len)}; the out-block is {@code gwin_free}d here.
     */
    private static final MethodHandle GWIN_CLIPBOARD_POP_BYTES = bindGlass("gwin_clipboard_pop_bytes",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS));

    /** {@code int32_t gwin_clipboard_pop_mimes(gwin_clipboard_t clip, uint16_t** out_mimes, int32_t* out_count)}. */
    private static final MethodHandle GWIN_CLIPBOARD_POP_MIMES = bindGlass("gwin_clipboard_pop_mimes",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /**
     * {@code void gwin_clipboard_push_target_action(gwin_clipboard_t clip, int32_t action_done)}; silent on failure.
     */
    private static final MethodHandle GWIN_CLIPBOARD_PUSH_TARGET_ACTION =
            bindGlass("gwin_clipboard_push_target_action", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    /**
     * {@code int32_t gwin_clipboard_pop_supported_actions(gwin_clipboard_t clip)}: a {@code Clipboard.ACTION_*} set.
     */
    private static final MethodHandle GWIN_CLIPBOARD_POP_SUPPORTED_ACTIONS =
            bindGlass("gwin_clipboard_pop_supported_actions", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * {@code int32_t gwin_dnd_push(gwin_clipboard_t old, int64_t clipboard_id, const uint16_t* mimes,
     * int32_t mime_count, int32_t supported_actions)}: blocks for the whole drag in
     * {@code DoDragDrop}'s modal loop, with every slot of both tables able to fire nested inside.
     */
    private static final MethodHandle GWIN_DND_PUSH = bindGlass("gwin_dnd_push",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT));

    /** {@code void gwin_dnd_dispose(gwin_clipboard_t clip)}: {@code Release} only. Blocks. */
    private static final MethodHandle GWIN_DND_DISPOSE = bindGlass("gwin_dnd_dispose",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code int32_t gwin_sizeof_file_filter(void)}; {@link #GWIN_FILE_FILTER_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_FILE_FILTER =
            bindGlass("gwin_sizeof_file_filter", FunctionDescriptor.of(JAVA_INT));

    /**
     * {@code int32_t gwin_dialog_file(void* owner, const uint16_t* folder, const uint16_t* filename,
     * const uint16_t* title, int32_t type, int32_t multiple, const GwinFileFilter* filters, int32_t
     * filter_count, int32_t default_filter_index, uint16_t** out_files, int32_t* out_count, int32_t*
     * out_filter_index)}: a modal dialog on this thread, not marshalled.
     */
    private static final MethodHandle GWIN_DIALOG_FILE = bindGlass("gwin_dialog_file",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS,
                    JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    /**
     * {@code int32_t gwin_dialog_folder(void* owner, const uint16_t* folder, const uint16_t* title,
     * uint16_t** out_path)}: a modal dialog on this thread, not marshalled.
     */
    private static final MethodHandle GWIN_DIALOG_FOLDER = bindGlass("gwin_dialog_folder",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /** {@code int32_t gwin_sizeof_accessible_callbacks(void)}; {@link #GWIN_ACCESSIBLE_CALLBACKS_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_ACCESSIBLE_CALLBACKS =
            bindGlass("gwin_sizeof_accessible_callbacks", FunctionDescriptor.of(JAVA_INT));

    /** {@code int32_t gwin_sizeof_text_range_callbacks(void)}; {@link #GWIN_TEXT_RANGE_CALLBACKS_LAYOUT} must agree. */
    private static final MethodHandle GWIN_SIZEOF_TEXT_RANGE_CALLBACKS =
            bindGlass("gwin_sizeof_text_range_callbacks", FunctionDescriptor.of(JAVA_INT));

    /** {@code int32_t gwin_sizeof_variant(void)}; {@link #GWIN_VARIANT_LAYOUT} must agree, field for field. */
    private static final MethodHandle GWIN_SIZEOF_VARIANT =
            bindGlass("gwin_sizeof_variant", FunctionDescriptor.of(JAVA_INT));

    /** {@code int32_t gwin_a11y_set_callbacks(const GwinAccessibleCallbacks* cb)} - no {@code void* user}. */
    private static final MethodHandle GWIN_A11Y_SET_CALLBACKS =
            bindGlass("gwin_a11y_set_callbacks", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int32_t gwin_a11y_text_range_set_callbacks(const GwinTextRangeCallbacks* cb)}. */
    private static final MethodHandle GWIN_A11Y_TEXT_RANGE_SET_CALLBACKS =
            bindGlass("gwin_a11y_text_range_set_callbacks", FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /**
     * {@code gwin_accessible_t gwin_a11y_create(int64_t accessible_id)}. Not
     * {@link Linker.Option#critical}, as nothing in this section is: the destroy calls re-enter the JVM
     * from a destructor and the raise call blocks in UIAutomationCore.
     */
    private static final MethodHandle GWIN_A11Y_CREATE =
            bindGlass("gwin_a11y_create", FunctionDescriptor.of(ADDRESS, JAVA_LONG));

    /** {@code void gwin_a11y_destroy(gwin_accessible_t acc)}: {@code Release}, not {@code delete}. */
    private static final MethodHandle GWIN_A11Y_DESTROY =
            bindGlass("gwin_a11y_destroy", FunctionDescriptor.ofVoid(ADDRESS));

    /** {@code gwin_text_range_t gwin_a11y_text_range_create(gwin_accessible_t acc, int64_t range_id)}. */
    private static final MethodHandle GWIN_A11Y_TEXT_RANGE_CREATE = bindGlass("gwin_a11y_text_range_create",
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));

    /** {@code void gwin_a11y_text_range_destroy(gwin_text_range_t range)}. */
    private static final MethodHandle GWIN_A11Y_TEXT_RANGE_DESTROY =
            bindGlass("gwin_a11y_text_range_destroy", FunctionDescriptor.ofVoid(ADDRESS));

    /**
     * {@code int64_t gwin_a11y_raise_property_changed(gwin_accessible_t acc, int32_t property_id,
     * const GwinVariant* old_value, const GwinVariant* new_value)}: the {@code HRESULT},
     * sign-extended.
     */
    private static final MethodHandle GWIN_A11Y_RAISE_PROPERTY_CHANGED =
            bindGlass("gwin_a11y_raise_property_changed",
                    FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));

    /** {@code UINT SendInput(UINT cInputs, LPINPUT pInputs, int cbSize)}. */
    private static final MethodHandle SEND_INPUT = bindUser32("SendInput",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

    /**
     * {@code void mouse_event(DWORD dwFlags, DWORD dx, DWORD dy, DWORD dwData, ULONG_PTR dwExtraInfo)} -
     * superseded by {@code SendInput}, but it is what {@code Robot.cpp} used for the wheel (L241-245)
     * and the two are not interchangeable in every driver stack, so the wheel keeps calling it.
     */
    private static final MethodHandle MOUSE_EVENT = bindUser32("mouse_event",
            FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG));

    /** {@code BOOL GetCursorPos(LPPOINT lpPoint)}. */
    private static final MethodHandle GET_CURSOR_POS = bindUser32("GetCursorPos",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int GetSystemMetrics(int nIndex)}. */
    private static final MethodHandle GET_SYSTEM_METRICS = bindUser32("GetSystemMetrics",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    /**
     * {@code UINT MapVirtualKeyW(UINT uCode, UINT uMapType)} - the {@code W} form is what
     * {@code ::MapVirtualKey} resolved to in the C, which compiles with {@code UNICODE} defined.
     */
    private static final MethodHandle MAP_VIRTUAL_KEY_W = bindUser32("MapVirtualKeyW",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT));

    /**
     * {@code int ShowCursor(BOOL bShow)} - {@code GlassCursor.cpp:173}. Bound eagerly, unlike the GDI
     * block below: hiding and showing the pointer must keep working on a machine where a GDI symbol
     * could not be resolved, and it is one call with no structure behind it.
     */
    private static final MethodHandle SHOW_CURSOR = bindUser32("ShowCursor",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    /**
     * {@code SHORT GetKeyState(int nVirtKey)} - {@code KeyTable.cpp:427,431}. The return really is
     * 16 bits wide: bit 15 is "down" and bit 0 is the toggle state, which is the one
     * {@code _isKeyLocked} tested. Binding it as {@code JAVA_INT} would read three bytes of whatever
     * the ABI leaves in the register.
     */
    private static final MethodHandle GET_KEY_STATE = bindUser32("GetKeyState",
            FunctionDescriptor.of(JAVA_SHORT, JAVA_INT));

    /**
     * {@code DWORD GetSysColor(int nIndex)} - {@code PlatformSupport::querySystemColors}. The result is a
     * {@code COLORREF}, i.e. {@code 0x00BBGGRR}: blue in the high byte, not red (see
     * {@link #sysColorRed}).
     */
    private static final MethodHandle GET_SYS_COLOR = bindUser32("GetSysColor",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    /**
     * {@code BOOL SystemParametersInfoW(UINT uiAction, UINT uiParam, PVOID pvParam, UINT fWinIni)} -
     * {@code PlatformSupport::querySystemParameters}. The {@code W} form is what {@code ::SystemParametersInfo}
     * resolved to in the C, which compiles with {@code UNICODE} defined, and it is what makes
     * {@code HIGHCONTRAST.lpszDefaultScheme} a UTF-16 string.
     */
    private static final MethodHandle SYSTEM_PARAMETERS_INFO_W = bindUser32("SystemParametersInfoW",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

    /**
     * {@code DWORD GetVersion(void)} - the whole of {@code Utils.h}'s {@code IS_WINVER_ATLEAST}
     * (L61-64), and so the whole of {@code WinApplication._supportsUnifiedWindows}
     * ({@code GlassApplication.cpp:504-508}, whose body was {@code return (IS_WINVISTA);}).
     * <p>
     * {@code GetVersion} is deprecated and, for a process whose manifest does not claim a newer
     * Windows, reports 6.2 rather than the true build. That lie is per <em>process</em>, so this
     * facade is told the same one the C was told, in the same process, on the same thread:
     * {@link #isWindowsVersionAtLeast} is byte-for-byte the answer {@code IS_WINVISTA} gave. Do not
     * "fix" it to {@code RtlGetVersion}, {@code VerifyVersionInfo} or {@code IsWindows8OrGreater} -
     * that would change what {@code Application.supportsUnifiedWindows()} returns on some machines,
     * which is a behaviour change and not a migration.
     */
    private static final MethodHandle GET_VERSION = bindKernel32("GetVersion",
            FunctionDescriptor.of(JAVA_INT));

    /*
     * The eleven user32 menu entry points that were the whole of GlassMenu.cpp's eleven functional
     * JNI bodies. An HMENU is a USER handle value, never an address this process may dereference, so
     * it crosses as ADDRESS built by MemorySegment.ofAddress (unrestricted) and is never
     * reinterpret()ed. None of these is critical(true): they pass an off-heap MENUITEMINFOW rather
     * than a Java array, and they re-enter the window manager.
     *
     * Note the two return types that are NOT interchangeable: EnableMenuItem returns BOOL (signed, so
     * its -1 "no such item" is detectable) and CheckMenuItem returns DWORD (unsigned, so the C's
     * "0 <= result" test could never fail). See enableMenuItem/menuCheckItem.
     */

    /** {@code HMENU CreateMenu(void)}; {@code NULL} on failure. */
    private static final MethodHandle CREATE_MENU = bindUser32("CreateMenu",
            FunctionDescriptor.of(ADDRESS));

    /** {@code BOOL DestroyMenu(HMENU hMenu)}. */
    private static final MethodHandle DESTROY_MENU = bindUser32("DestroyMenu",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code BOOL IsMenu(HMENU hMenu)} - the guard every one of the eleven bodies opened with. */
    private static final MethodHandle IS_MENU = bindUser32("IsMenu",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code int GetMenuItemCount(HMENU hMenu)}; {@code -1} on failure, which empties every loop. */
    private static final MethodHandle GET_MENU_ITEM_COUNT = bindUser32("GetMenuItemCount",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code BOOL GetMenuItemInfoW(HMENU, UINT item, BOOL fByPosition, LPMENUITEMINFOW lpmii)}. */
    private static final MethodHandle GET_MENU_ITEM_INFO_W = bindUser32("GetMenuItemInfoW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));

    /** {@code BOOL SetMenuItemInfoW(HMENU, UINT item, BOOL fByPositon, LPCMENUITEMINFOW lpmii)}. */
    private static final MethodHandle SET_MENU_ITEM_INFO_W = bindUser32("SetMenuItemInfoW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));

    /** {@code BOOL InsertMenuItemW(HMENU, UINT item, BOOL fByPosition, LPCMENUITEMINFOW lpmi)}. */
    private static final MethodHandle INSERT_MENU_ITEM_W = bindUser32("InsertMenuItemW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));

    /**
     * {@code BOOL InsertMenuW(HMENU, UINT uPosition, UINT uFlags, UINT_PTR uIDNewItem, LPCWSTR lpNewItem)}.
     * {@code UINT_PTR} is 64 bits on LLP64; the one call site passes 0.
     */
    private static final MethodHandle INSERT_MENU_W = bindUser32("InsertMenuW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS));

    /** {@code BOOL RemoveMenu(HMENU, UINT uPosition, UINT uFlags)}. */
    private static final MethodHandle REMOVE_MENU = bindUser32("RemoveMenu",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));

    /** {@code BOOL EnableMenuItem(HMENU, UINT uIDEnableItem, UINT uEnable)} - <b>signed</b>. */
    private static final MethodHandle ENABLE_MENU_ITEM = bindUser32("EnableMenuItem",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));

    /** {@code DWORD CheckMenuItem(HMENU, UINT uIDCheckItem, UINT uCheck)} - <b>unsigned</b>. */
    private static final MethodHandle CHECK_MENU_ITEM = bindUser32("CheckMenuItem",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));

    /**
     * {@code UINT GetDoubleClickTime(void)} - the whole of
     * {@code Java_com_sun_glass_ui_win_WinView__1getMultiClickTime_1impl}
     * ({@code GlassView.cpp}). {@code UINT} is unsigned and the JNI widened it with a
     * {@code (jlong)} cast, so {@link #getDoubleClickTime()} widens with
     * {@link Integer#toUnsignedLong}.
     */
    private static final MethodHandle GET_DOUBLE_CLICK_TIME = bindUser32("GetDoubleClickTime",
            FunctionDescriptor.of(JAVA_INT));

    /*
     * The four user32 calls and the one kernel32 call of the window peer: Java_..._1getAnchor's whole body
     * (IsWindow, GetCapture, GetWindowRect, plus the GetCursorPos bound above), the LoadCursorW of
     * GlassCursor.cpp's GetNativeCursor, and GetModuleHandleW, which stands in for the module handle that
     * function took from GlassApplication::GetHInstance(). HWNDs, HCURSORs and
     * HMODULEs are handle values this process never dereferences: they cross as ADDRESS built by
     * MemorySegment.ofAddress and are never reinterpret()ed.
     */

    /** {@code BOOL IsWindow(HWND hWnd)} - the first line of {@code _getAnchor}. */
    private static final MethodHandle IS_WINDOW = bindUser32("IsWindow",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /** {@code HWND GetCapture(void)} - per thread; {@code NULL} when no window of this thread holds it. */
    private static final MethodHandle GET_CAPTURE = bindUser32("GetCapture",
            FunctionDescriptor.of(ADDRESS));

    /** {@code BOOL GetWindowRect(HWND hWnd, LPRECT lpRect)} - fills {@link #RECT_LAYOUT}. */
    private static final MethodHandle GET_WINDOW_RECT = bindUser32("GetWindowRect",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /**
     * {@code HCURSOR LoadCursorW(HINSTANCE hInstance, LPCWSTR lpCursorName)} - the {@code W} form
     * {@code ::LoadCursor} resolved to under {@code UNICODE}. {@code lpCursorName} is either a
     * {@code MAKEINTRESOURCE} ordinal (the integer in pointer clothing) or a real string.
     */
    private static final MethodHandle LOAD_CURSOR_W = bindUser32("LoadCursorW",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    /** {@code HMODULE GetModuleHandleW(LPCWSTR lpModuleName)} - {@code glass.dll}'s own {@code HINSTANCE}. */
    private static final MethodHandle GET_MODULE_HANDLE_W = bindKernel32("GetModuleHandleW",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    /**
     * {@code HRESULT OleIsCurrentClipboard(LPDATAOBJECT pDataObj)} - the whole of the former
     * {@code WinSystemClipboard.isOwner} body behind its NULL test (the one {@code WRAPPER}
     * of the clipboard peers). {@code HRESULT} is a 32-bit {@code LONG}; {@link #oleIsCurrentClipboard}
     * answers {@code hr == S_OK}, as {@code S_OK == ::OleIsCurrentClipboard(p)} did.
     */
    private static final MethodHandle OLE_IS_CURRENT_CLIPBOARD = bindOle32("OleIsCurrentClipboard",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    /*
     * The user32 and kernel32 calls of GlassScreen.cpp's enumeration. HMONITORs and HDCs are
     * handle values this process never dereferences: they cross as ADDRESS and are never reinterpret()ed.
     */

    /**
     * {@code BOOL EnumDisplayMonitors(HDC hdc, LPCRECT lprcClip, MONITORENUMPROC lpfnEnum, LPARAM dwData)} -
     * {@code GlassScreen::CreateJavaScreens}' two passes, both with {@code NULL, NULL, proc, 0}. Synchronous:
     * every callback runs on the calling thread before it returns.
     */
    private static final MethodHandle ENUM_DISPLAY_MONITORS = bindUser32("EnumDisplayMonitors",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));

    /**
     * {@code BOOL GetMonitorInfoW(HMONITOR hMonitor, LPMONITORINFO lpmi)} - the {@code W} form
     * {@code ::GetMonitorInfo} resolved to under {@code UNICODE}, which is what makes {@code szDevice}
     * UTF-16. Fills {@link #MONITORINFOEXW_LAYOUT}.
     */
    private static final MethodHandle GET_MONITOR_INFO_W = bindUser32("GetMonitorInfoW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    /** {@code BOOL SetProcessDPIAware(void)} - {@code LoadDPIFuncs}' fallback when {@code shcore} is not there. */
    private static final MethodHandle SET_PROCESS_DPI_AWARE = bindUser32("SetProcessDPIAware",
            FunctionDescriptor.of(JAVA_INT));

    /**
     * {@code UINT GetSystemDirectoryW(LPWSTR lpBuffer, UINT uSize)} - the directory {@code LoadDPIFuncs}
     * loaded {@code SHCore.dll} from, by full path ({@link Shcore}). {@code uSize} is in characters.
     */
    private static final MethodHandle GET_SYSTEM_DIRECTORY_W = bindKernel32("GetSystemDirectoryW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    /**
     * {@code winmm.dll} and the five timer functions of it, bound on first use rather than in this
     * class's initializer.
     * <p>
     * The laziness is behaviour parity, not taste: {@code glass.dll} delay-loaded {@code winmm}
     * ({@code win.cmake} {@code /DELAYLOAD:winmm.dll}), so a process that used {@link WinRobot} and no
     * timer never loaded it, and a machine on which {@code winmm} could not be resolved lost timers
     * only. Binding it in {@link WinGlassNative}'s own initializer would load it on the first robot
     * call and, worse, would turn a {@code winmm} failure into a failed initializer for this whole
     * class - every {@code WinRobot} call would then die with {@code NoClassDefFoundError} for a
     * library it does not use. A holder class keeps both the load point and the blast radius where the
     * JNI had them. Nothing is lost in a real application: {@code QuantumToolkit} starts the pulse
     * timer on every run ({@code QuantumToolkit.java:391-401}).
     * <p>
     * The cost is that {@link WinGlassNative#boundSymbols()} grows when the first timer call happens;
     * {@code WinGlassNativeTest} forces this class to initialise in its {@code @BeforeAll} so that its
     * ordered assertion stays an exact equality rather than a prefix check.
     */
    private static final class Winmm {

        private static final String NAME = "winmm";

        /** A system DLL the JDK does not load for us; the lookup lives for the life of the process. */
        private static final SymbolLookup LOOKUP = systemLibrary(NAME + ".dll");

        /** {@code MMRESULT timeGetDevCaps(LPTIMECAPS ptc, UINT cbtc)} - {@code Timer.h:85}. */
        private static final MethodHandle TIME_GET_DEV_CAPS = bindWinmm("timeGetDevCaps",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

        /** {@code MMRESULT timeBeginPeriod(UINT uPeriod)} - {@code Timer.h:53}. */
        private static final MethodHandle TIME_BEGIN_PERIOD = bindWinmm("timeBeginPeriod",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT));

        /** {@code MMRESULT timeEndPeriod(UINT uPeriod)} - {@code Timer.h:63}. */
        private static final MethodHandle TIME_END_PERIOD = bindWinmm("timeEndPeriod",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT));

        /**
         * {@code MMRESULT timeSetEvent(UINT uDelay, UINT uResolution, LPTIMECALLBACK lpTimeProc,
         * DWORD_PTR dwUser, UINT fuEvent)} - {@code Timer.h:71-72}. Returns the timer id, 0 on failure.
         * {@code DWORD_PTR} is pointer-sized, hence {@code JAVA_LONG} on the two Windows architectures
         * the JDK's {@link Linker} supports.
         */
        private static final MethodHandle TIME_SET_EVENT = bindWinmm("timeSetEvent",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT));

        /** {@code MMRESULT timeKillEvent(UINT uTimerID)} - {@code Timer.h:60}. */
        private static final MethodHandle TIME_KILL_EVENT = bindWinmm("timeKillEvent",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT));

        /**
         * {@code void CALLBACK LPTIMECALLBACK(UINT uTimerID, UINT uMsg, DWORD_PTR dwUser, DWORD_PTR
         * dw1, DWORD_PTR dw2)}. {@code CALLBACK} is {@code __stdcall}, a no-op on x64 and arm64.
         */
        private static final FunctionDescriptor TIME_CALLBACK_FD =
                FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_LONG);

        /**
         * The one {@code LPTIMECALLBACK} of the process, in {@link Arena#global()}: the target is
         * static and only {@code dwUser} differs between timers, so one stub serves every timer and
         * there is no arena to close - which matters, because {@code timeKillEvent} does not join an
         * in-flight callback (see {@link WinGlassNative#TIME_PERIODIC}) and so there is no point at
         * which closing a per-timer arena would be safe.
         */
        private static final MemorySegment TIME_CALLBACK = upcallStub(
                upcallTarget("onTimerFired", TIME_CALLBACK_FD), TIME_CALLBACK_FD, Arena.global());

        private Winmm() {
        }

        private static MethodHandle bindWinmm(String name, FunctionDescriptor descriptor) {
            return bind(LOOKUP, NAME, name, descriptor);
        }
    }

    /**
     * The GDI object calls behind a custom cursor, and the display device context of the screen
     * enumeration, bound on the first {@link #cursorCreateCustom} or {@link #collectMonitors()} rather than
     * in this class's initializer.
     * <p>
     * The laziness is blast radius, as it is for {@link Winmm}: a process that shows no custom cursor
     * and enumerates no screen never binds these, and a machine on which one of them could not be
     * resolved loses those two rather than {@link WinRobot}, {@link WinTimer} and
     * {@code Cursor.setVisible}, all of which are bound eagerly above. It is not about load order -
     * {@code gdi32.lib} is linked normally by {@code glass} ({@code win.cmake:171}, no {@code /DELAYLOAD}),
     * so {@code gdi32.dll} is already in the process by the time any of this runs.
     * <p>
     * {@code user32!CreateIconIndirect} is bound here and not beside {@code ShowCursor} because it is
     * only ever reached with a pair of GDI bitmaps in hand: it shares this block's fate exactly. The three
     * screen calls come last, so the cursor block keeps its binding order.
     */
    private static final class Gdi32 {

        private static final String NAME = "gdi32";

        /** A system DLL the JDK does not load for us; the lookup lives for the life of the process. */
        private static final SymbolLookup LOOKUP = systemLibrary(NAME + ".dll");

        /**
         * {@code HBITMAP CreateBitmap(int nWidth, int nHeight, UINT nPlanes, UINT nBitCount,
         * const void *lpBits)} - the cursor's monochrome AND mask ({@code Pixels.cpp:57}).
         */
        private static final MethodHandle CREATE_BITMAP = bindGdi32("CreateBitmap",
                FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));

        /**
         * {@code HBITMAP CreateDIBSection(HDC hdc, const BITMAPINFO *pbmi, UINT usage,
         * void **ppvBits, HANDLE hSection, DWORD offset)} - the 32-bit colour bitmap
         * ({@code Pixels.cpp:122}). {@code hdc} and {@code hSection} are both {@code NULL} there.
         */
        private static final MethodHandle CREATE_DIB_SECTION = bindGdi32("CreateDIBSection",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

        /** {@code BOOL DeleteObject(HGDIOBJ ho)} - {@code BaseBitmap}'s destructor, {@code Pixels.h:35}. */
        private static final MethodHandle DELETE_OBJECT = bindGdi32("DeleteObject",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** {@code BOOL GdiFlush(void)} - {@code Pixels.cpp:148}. */
        private static final MethodHandle GDI_FLUSH = bindGdi32("GdiFlush",
                FunctionDescriptor.of(JAVA_INT));

        /** {@code HICON CreateIconIndirect(PICONINFO piconinfo)} - {@code Pixels.cpp:145}, user32. */
        private static final MethodHandle CREATE_ICON_INDIRECT = bindUser32("CreateIconIndirect",
                FunctionDescriptor.of(ADDRESS, ADDRESS));

        /**
         * {@code HDC CreateDCW(LPCWSTR pwszDriver, LPCWSTR pwszDevice, LPCWSTR pszPort, const DEVMODEW* pdm)} -
         * {@code GetMonitorSettings}' {@code CreateDC(TEXT("DISPLAY"), mix.szDevice, NULL, NULL)}.
         */
        private static final MethodHandle CREATE_DC_W = bindGdi32("CreateDCW",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

        /** {@code int GetDeviceCaps(HDC hdc, int index)} - the colour depth and the {@code LOGPIXELS} fallback. */
        private static final MethodHandle GET_DEVICE_CAPS = bindGdi32("GetDeviceCaps",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

        /** {@code BOOL DeleteDC(HDC hdc)} - the last statement of {@code GetMonitorSettings}. */
        private static final MethodHandle DELETE_DC = bindGdi32("DeleteDC",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        private Gdi32() {
        }

        private static MethodHandle bindGdi32(String name, FunctionDescriptor descriptor) {
            return bind(LOOKUP, NAME, name, descriptor);
        }
    }

    /**
     * {@code shlwapi.dll} and the one function of it {@code GlassApplication.cpp} called, bound on
     * the first {@link #defaultBrowser()} rather than in this class's initializer.
     * <p>
     * The laziness is behaviour parity, exactly as it is for {@link Winmm}: {@code glass.dll}
     * delay-loaded {@code shlwapi} ({@code win.cmake:175}, {@code /DELAYLOAD:shlwapi.dll}) and
     * {@code AssocQueryStringW} was its only use of that DLL in the whole library
     * ({@code GlassApplication.cpp:28,545}), so a process that never called
     * {@code Application.showDocument} never loaded it. {@code shlwapi} in turn pulls in the shell
     * association machinery, so this is not a free load. A holder class keeps the load point where
     * the JNI had it, and keeps a {@code shlwapi} that cannot be resolved from failing
     * {@link WinRobot}, {@link WinTimer} and {@link WinCursor} with it.
     * <p>
     * The cost, again as for {@link Winmm}: {@link #boundSymbols()} grows when the first
     * {@code showDocument} happens, which is why that list is a {@link CopyOnWriteArrayList} and why
     * the tests that assert it force this holder in their {@code @BeforeAll}.
     */
    private static final class Shlwapi {

        private static final String NAME = "shlwapi";

        /** A system DLL the JDK does not load for us; the lookup lives for the life of the process. */
        private static final SymbolLookup LOOKUP = systemLibrary(NAME + ".dll");

        /**
         * {@code HRESULT AssocQueryStringW(ASSOCF flags, ASSOCSTR str, LPCWSTR pszAssoc,
         * LPCWSTR pszExtra, LPWSTR pszOut, DWORD *pcchOut)} - {@code GlassApplication.cpp:545-552}.
         * Both enums are 32-bit, {@code pszExtra} was {@code NULL} there, and {@code pcchOut} is an
         * in/out count in <em>characters</em>.
         */
        private static final MethodHandle ASSOC_QUERY_STRING_W = bindShlwapi("AssocQueryStringW",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

        private Shlwapi() {
        }

        private static MethodHandle bindShlwapi(String name, FunctionDescriptor descriptor) {
            return bind(LOOKUP, NAME, name, descriptor);
        }
    }

    /**
     * {@code SHCore.dll} and the three DPI functions {@code GlassScreen::LoadDPIFuncs} looked up in it,
     * resolved on the first {@link #loadDpiFuncs} - which is also where the C loaded it.
     * <p>
     * The load is the C's: {@code GetSystemDirectoryW}, then {@code "\\SHCore.dll"} appended, then a load
     * by that full path. Only when that load fails is it retried by the bare name {@code SHCore.dll} (see
     * {@code load()} for why: the JDK's path conversion can fail where the C's did not, and
     * {@code SHCORE} is a KnownDLL, so the bare name cannot resolve to any other copy). The three symbols
     * are kept <b>all or none</b>, from whichever load succeeded ({@code LoadDPIFuncs} zeroed all three
     * pointers when one was missing), and a directory or a library that cannot be had is "none", not an
     * error: it selects {@code SetProcessDPIAware} and the {@code LOGPIXELSX/Y} resolution, exactly as the
     * C's NULL pointers did. So {@link #boundSymbols()} gains these three only when {@link #RESOLVED}. One
     * difference from the C, unreachable on a Windows a JDK 26 supports: a system directory of
     * {@code MAX_PATH} characters or more is "none" here where the C appended to an unterminated buffer.
     */
    private static final class Shcore {

        private static final String NAME = "shcore";

        /** A lookup that finds nothing: what a missing directory or library leaves. */
        private static final SymbolLookup NOTHING = name -> Optional.empty();

        /** The library for the life of the process, as the C never freed it; {@link #NOTHING} when it failed. */
        private static final SymbolLookup LOOKUP = load();

        /** Whether all three resolved. When false, none is bound and every handle below is {@code null}. */
        static final boolean RESOLVED = LOOKUP.find("GetProcessDpiAwareness").isPresent()
                && LOOKUP.find("SetProcessDpiAwareness").isPresent()
                && LOOKUP.find("GetDpiForMonitor").isPresent();

        /* In LoadDPIFuncs' GetProcAddress order. */

        /** {@code HRESULT GetProcessDpiAwareness(HANDLE hprocess, PROCESS_DPI_AWARENESS* value)}. */
        private static final MethodHandle GET_PROCESS_DPI_AWARENESS = RESOLVED
                ? bindShcore("GetProcessDpiAwareness", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS))
                : null;

        /** {@code HRESULT SetProcessDpiAwareness(PROCESS_DPI_AWARENESS value)}. */
        private static final MethodHandle SET_PROCESS_DPI_AWARENESS = RESOLVED
                ? bindShcore("SetProcessDpiAwareness", FunctionDescriptor.of(JAVA_INT, JAVA_INT))
                : null;

        /**
         * {@code HRESULT GetDpiForMonitor(HMONITOR hmonitor, MONITOR_DPI_TYPE dpiType, UINT* dpiX, UINT* dpiY)};
         * the enum is a 32-bit {@code int}.
         */
        private static final MethodHandle GET_DPI_FOR_MONITOR = RESOLVED
                ? bindShcore("GetDpiForMonitor", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS))
                : null;

        private Shcore() {
        }

        @SuppressWarnings("restricted")
        private static SymbolLookup load() {
            String directory = systemDirectory();
            if (directory == null) {
                return NOTHING;
            }
            try {
                return SymbolLookup.libraryLookup(Path.of(directory + "\\SHCore.dll"), Arena.global());
            } catch (IllegalArgumentException e) {
                // InvalidPathException is one too. Not necessarily the C's NULL: see the retry below.
            }
            // The C's LoadLibraryW takes the UTF-16 path as it is; the JDK first converts it to the ANSI code
            // page (RawNativeLibraries.load0, JNU_GetStringPlatformChars), which fails for a system directory
            // outside that code page - where the C loaded SHCore and this would silently fall back to
            // SetProcessDPIAware and LOGPIXELS. So a failed full-path load is retried by bare name before it
            // counts as "none". SHCORE is a KnownDLL: the bare name resolves to the system directory's copy
            // only, through the loader's KnownDLLs section, never through the search path.
            try {
                return SymbolLookup.libraryLookup("SHCore.dll", Arena.global());
            } catch (IllegalArgumentException e) {
                // Both loads failed: the C's LoadLibrary returned NULL too - no DPI functions.
                return NOTHING;
            }
        }

        private static MethodHandle bindShcore(String name, FunctionDescriptor descriptor) {
            return bind(LOOKUP, NAME, name, descriptor);
        }
    }

    private WinGlassNative() {
    }

    private static long inputOffset(String member, String field) {
        return INPUT_LAYOUT.byteOffset(PathElement.groupElement("u"), PathElement.groupElement(member),
                PathElement.groupElement(field));
    }

    private static long bitmapInfoOffset(String field) {
        return BITMAPINFOHEADER_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    private static long iconInfoOffset(String field) {
        return ICONINFO_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    private static long highContrastOffset(String field) {
        return HIGHCONTRAST_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    private static long uiSettingsOffset(String field) {
        return GWIN_UI_SETTINGS_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    private static long networkInfoOffset(String field) {
        return GWIN_NETWORK_INFO_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    private static long menuItemInfoOffset(String field) {
        return MENUITEMINFOW_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    private static long monitorInfoOffset(String field) {
        return MONITORINFOEXW_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    @SuppressWarnings("restricted")
    private static SymbolLookup systemLibrary(String fileName) {
        return SymbolLookup.libraryLookup(fileName, Arena.global());
    }

    private static MethodHandle bindGlass(String name, FunctionDescriptor descriptor) {
        return bind(GLASS, LIBRARY_NAME, name, descriptor);
    }

    private static MethodHandle bindUser32(String name, FunctionDescriptor descriptor) {
        return bind(USER32, USER32_NAME, name, descriptor);
    }

    private static MethodHandle bindKernel32(String name, FunctionDescriptor descriptor) {
        return bind(KERNEL32, KERNEL32_NAME, name, descriptor);
    }

    private static MethodHandle bindOle32(String name, FunctionDescriptor descriptor) {
        return bind(OLE32, OLE32_NAME, name, descriptor);
    }

    /**
     * Resolves {@code name} in {@code lookup} and binds it as a plain downcall. None of these is
     * {@link Linker.Option#critical critical}: the capture blocks on GDI and the Win32 calls pass no
     * Java array (see the class documentation).
     *
     * @throws UnsatisfiedLinkError if the library does not export {@code name}
     */
    @SuppressWarnings("restricted")
    private static MethodHandle bind(SymbolLookup lookup, String library, String name,
                                     FunctionDescriptor descriptor) {
        MemorySegment symbol = lookup.find(name).orElseThrow(
                () -> new UnsatisfiedLinkError("missing native symbol: " + name + " in " + library));
        BOUND_SYMBOLS.add(library + "!" + name);
        return LINKER.downcallHandle(symbol, descriptor);
    }

    /**
     * Builds the upcall stub for a static method of this class. {@link Linker#upcallStub} is
     * restricted and a field initializer cannot carry {@code @SuppressWarnings}, so the call lives in a
     * method, exactly as {@link #systemLibrary} and {@link #bind} hold the other two restricted calls.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment upcallStub(MethodHandle target, FunctionDescriptor fd, Arena arena) {
        return LINKER.upcallStub(target, fd, arena);
    }

    /**
     * The static method of this class an upcall stub is to call, resolved by name against the
     * descriptor's method type.
     *
     * @throws ExceptionInInitializerError if the method is missing or does not have that signature
     */
    private static MethodHandle upcallTarget(String name, FunctionDescriptor descriptor) {
        try {
            return MethodHandles.lookup().findStatic(WinGlassNative.class, name, descriptor.toMethodType());
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void checkAbiVersion() {
        int actual;
        try {
            actual = (int) GWIN_ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (actual != ABI_VERSION) {
            throw new UnsatisfiedLinkError(LIBRARY_NAME + " ABI version mismatch: expected " + ABI_VERSION
                    + ", found " + actual);
        }
    }

    private static Error unexpected(Throwable t) {
        if (t instanceof Error e) {
            return e;
        }
        if (t instanceof RuntimeException e) {
            throw e;
        }
        // Only linkage failures can land here: neither glass.dll nor user32 throws.
        return new AssertionError(t);
    }

    /** Runs this class initializer: loads {@code glass}, binds every symbol and checks the ABI version. */
    static void ensureLoaded() {
    }

    /** The symbols this class bound, as {@code <library>!<name>}, in binding order. */
    static List<String> boundSymbols() {
        return Collections.unmodifiableList(new ArrayList<>(BOUND_SYMBOLS));
    }

    /** Whether {@code <library>!<name>} resolves; the library must be one this class binds. */
    static boolean resolves(String qualifiedName) {
        int separator = qualifiedName.indexOf('!');
        if (separator < 0) {
            throw new IllegalArgumentException("not a <library>!<name> pair: " + qualifiedName);
        }
        String library = qualifiedName.substring(0, separator);
        String name = qualifiedName.substring(separator + 1);
        SymbolLookup lookup = switch (library) {
            case LIBRARY_NAME -> GLASS;
            case USER32_NAME -> USER32;
            case KERNEL32_NAME -> KERNEL32;
            case OLE32_NAME -> OLE32;
            // Naming a lazy library here initialises its holder, and so binds it: a test that
            // asks whether winmm!timeSetEvent or shlwapi!AssocQueryStringW resolves gets the true
            // answer rather than "not yet", at the price of loading the DLL it just asked about.
            // shcore is the one holder that binds nothing unless all three of its symbols resolve.
            case Winmm.NAME -> Winmm.LOOKUP;
            case Gdi32.NAME -> Gdi32.LOOKUP;
            case Shlwapi.NAME -> Shlwapi.LOOKUP;
            case Shcore.NAME -> Shcore.LOOKUP;
            case UiaCore.NAME -> UiaCore.LOOKUP;
            default -> throw new IllegalArgumentException("not a library this class binds: " + library);
        };
        return lookup.find(name).isPresent();
    }

    static int abiVersion() {
        try {
            return (int) GWIN_ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Key table (gwin_key_*, KeyTable.cpp)
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code KeyEvent.VK_*} to Windows virtual-key code, {@code KeyTable.cpp}'s
     * {@code JavaKeyToWindowsKey}: 0 when the code has no virtual key, which is the sentinel
     * {@link #robotKeyPress} and {@link #robotKeyRelease} treat as "send nothing". Layout-dependent for
     * the OEM keys, and therefore bound to the calling thread.
     */
    static int keyJavaToWindows(int javaKeyCode) {
        try {
            return (int) GWIN_KEY_JAVA_TO_WINDOWS.invokeExact(javaKeyCode);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Windows virtual-key code to {@code KeyEvent.VK_*}, {@code KeyTable.cpp}'s
     * {@code WindowsKeyToJavaKey}: {@code KeyEvent.VK_UNDEFINED} (0) when the table has no entry.
     * Nothing in {@code WinRobot} calls this; it completes the key-table pair the ABI exports and is covered by
     * {@code WinGlassNativeTest}.
     */
    static int keyWindowsToJava(int virtualKey) {
        try {
            return (int) GWIN_KEY_WINDOWS_TO_JAVA.invokeExact(virtualKey);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_key_code_for_char}, which is {@code WinApplication._getKeyCodeForChar} -
     * {@code KeyTable.cpp}'s body, unchanged: {@code 0x7F} is {@code KeyEvent.VK_DELETE} before any OS
     * call; a numeric-keypad {@code hint} that maps back to {@code c} is returned unchanged; otherwise
     * {@code VkKeyScanEx(c, layout) & 0xFF}, then the OEM or the plain table lookup, with
     * {@code KeyEvent.VK_UNDEFINED} (0) for 0 and 0xFF.
     * <p>
     * <b>Why {@code short} and not {@code char}.</b> There is no {@code char} value layout in FFM, so
     * the caller narrows: {@code keyCodeForChar((short) c, hint)}. The 16 bits are unchanged by that
     * cast and the C parameter is a {@code uint16_t}, so a code unit above {@code 0x7FFF} - which is
     * negative as a Java {@code short} - still arrives as the same unsigned value. Do not widen this
     * to {@code JAVA_INT}: the C would then read a sign-extended pattern in the low half of a register
     * whose upper half the ABI does not define.
     * <p>
     * <b>Threading.</b> Two keyboard layouts are read: {@code VkKeyScanEx} and {@code MapVirtualKeyEx}
     * use the Glass toolkit thread's layout ({@code GetKeyboardLayout(GetMainThreadId())}, 0 before
     * the toolkit window exists), while the numeric-keypad branch reaches
     * {@code JavaKeyToWindowsKey}, whose OEM search uses the <em>calling</em> thread's layout. The
     * result is therefore bound to the calling thread and must not be cached or moved;
     * {@code Application.getKeyCodeForChar} does not call {@code checkEventThread} either.
     */
    static int keyCodeForChar(short c, int hint) {
        try {
            return (int) GWIN_KEY_CODE_FOR_CHAR.invokeExact(c, hint);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code KeyTable.cpp}'s {@code IsExtendedKey} (L315-343), the 22 virtual keys that get
     * {@code KEYEVENTF_EXTENDEDKEY}: {@code VK_PRIOR}, {@code VK_NEXT}, {@code VK_END},
     * {@code VK_HOME}, the four arrows, {@code VK_SNAPSHOT}, {@code VK_INSERT}, {@code VK_DELETE},
     * {@code VK_NUMLOCK} and the contiguous {@code VK_NUMPAD0..VK_NUMPAD9}. Note what is <em>not</em>
     * in it, exactly as in the C: no {@code VK_RCONTROL}, {@code VK_RMENU} or {@code VK_DIVIDE}, which
     * are extended keys on a real keyboard but were never marked here.
     */
    static boolean isExtendedKey(int virtualKey) {
        return switch (virtualKey) {
            case VK_PRIOR, VK_NEXT, VK_END, VK_HOME, VK_LEFT, VK_UP, VK_RIGHT, VK_DOWN,
                 VK_SNAPSHOT, VK_INSERT, VK_DELETE, VK_NUMLOCK -> true;
            default -> virtualKey >= VK_NUMPAD0 && virtualKey <= VK_NUMPAD9;
        };
    }

    /**
     * {@code WinApplication._isKeyLocked}, which was {@code KeyTable.cpp:421-439} - a two-case switch
     * over {@code user32!GetKeyState} and nothing else, so there is no C left for it.
     * <p>
     * Three details of the C survive here because they are observable. Every code other than the two
     * named ones returns {@code KEY_LOCK_UNKNOWN} <em>without asking the OS at all</em> (L434-436).
     * The bit tested is bit 0, the toggle state, not the high "is down" bit (L437-438). And the two
     * Glass key codes happen to have the same numeric values as the Windows virtual keys they map to
     * ({@code VK_CAPS_LOCK == VK_CAPITAL == 0x14}, {@code VK_NUM_LOCK == VK_NUMLOCK == 0x90}), which
     * is a coincidence the C did not rely on and neither does this: the switch is over
     * {@code KeyEvent} constants and what reaches {@code GetKeyState} is a winuser.h constant.
     * <p>
     * {@code GetKeyState} reports the calling thread's view of the keyboard, and this runs on the FX
     * thread exactly as the JNI did: {@code Application.isKeyLocked} calls {@code checkEventThread()}
     * first ({@code Application.java:754-756}).
     */
    static int keyLockState(int keyCode) {
        int virtualKey = switch (keyCode) {
            case KeyEvent.VK_CAPS_LOCK -> VK_CAPITAL;
            case KeyEvent.VK_NUM_LOCK -> VK_NUMLOCK;
            default -> 0;
        };
        if (virtualKey == 0) {
            return KeyEvent.KEY_LOCK_UNKNOWN;
        }
        return (getKeyState(virtualKey) & 0x1) != 0 ? KeyEvent.KEY_LOCK_ON : KeyEvent.KEY_LOCK_OFF;
    }

    /* ---------------------------------------------------------------------------------------------
     * Win32 primitives
     * ------------------------------------------------------------------------------------------- */

    /** {@code SHORT GetKeyState(int nVirtKey)}: bit 15 is "down", bit 0 is the toggle state. */
    static short getKeyState(int virtualKey) {
        try {
            return (short) GET_KEY_STATE.invokeExact(virtualKey);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code MapVirtualKeyW(uCode, uMapType)} on the calling thread's keyboard layout. */
    static int mapVirtualKey(int code, int mapType) {
        try {
            return (int) MAP_VIRTUAL_KEY_W.invokeExact(code, mapType);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code GetSystemMetrics(nIndex)}. */
    static int getSystemMetrics(int index) {
        try {
            return (int) GET_SYSTEM_METRICS.invokeExact(index);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code SendInput(1, &input, sizeof(INPUT))}, whose result the C ignored at every call site
     * ({@code Robot.cpp} L56, L113, L189, L233), so this ignores it too.
     */
    private static void sendOneInput(MemorySegment input) {
        try {
            int ignoredSentCount = (int) SEND_INPUT.invokeExact(1, input, (int) INPUT_LAYOUT.byteSize());
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Application (WinApplication; was GlassApplication.cpp)
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code kernel32!GetVersion} as the unsigned 32-bit value it is: the low word is the version
     * (low byte major, high byte minor), the high word the build number - or 0 when the process runs
     * under a compatibility shim. Package-private so a test can check the decoding of
     * {@link #isWindowsVersionAtLeast} against the raw value instead of against a platform.
     */
    static int windowsVersion() {
        try {
            return (int) GET_VERSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Utils.h}'s {@code IS_WINVER_ATLEAST(major, minor)} (L61-64) - and therefore the whole of
     * {@code WinApplication._supportsUnifiedWindows}, whose C body was {@code return (IS_WINVISTA);}
     * ({@code GlassApplication.cpp:504-508}), {@code IS_WINVISTA} being {@code IS_WINVER_ATLEAST(6, 0)}
     * ({@code Utils.h:66}).
     * <p>
     * The arithmetic is the macro's, including its operator precedence: {@code &&} binds tighter than
     * {@code ||}, so the C reads {@code a > maj || (a == maj && b >= min)}. {@code LOBYTE(LOWORD(v))}
     * is the major version and {@code HIBYTE(LOWORD(v))} the minor, both masked back to 0..255 so
     * that neither can arrive negative.
     * <p>
     * What this deliberately does <em>not</em> do is tell the truth about the platform. An
     * unmanifested process is told 6.2 by {@code GetVersion} whatever Windows it is running on; that
     * lie is a property of the process, not of the caller, so Java is told the same one the C was
     * ({@link #GET_VERSION}).
     */
    static boolean isWindowsVersionAtLeast(int major, int minor) {
        int version = windowsVersion();
        int actualMajor = version & 0xFF;
        int actualMinor = (version >>> 8) & 0xFF;
        return actualMajor > major || actualMajor == major && actualMinor >= minor;
    }

    /**
     * {@code gwin_app_create}: what {@code WinApplication._init} did after its DPI step
     * ({@code GlassApplication.cpp}) - {@code new GlassApplication()}, which registers the window class,
     * creates the hidden toolkit window whose {@code WM_CREATE} publishes the instance every other export
     * reads, and initializes WinRT ({@code RoInitialize(RO_INIT_SINGLETHREADED)}, {@code UISettings},
     * {@code NetworkInformation}) on <b>this</b> thread; the object is deleted again if no window resulted.
     * <p>
     * Call it on the thread that will call {@link #runLoop}, before it, and after {@link #loadDpiFuncs}
     * with the requested awareness: a window created before the process is DPI-aware stays unaware. Never
     * from a test JVM that does not own the toolkit: a second call leaks the first instance, as
     * {@code _init} did.
     *
     * @return the toolkit HWND as a {@code long}, or 0; the caller discards it, as it discarded
     *         {@code _init}'s {@code jlong} - {@code gwin_terminate_loop} reads the live instance, and an
     *         HWND value can be recycled
     */
    static long appCreate() {
        try {
            MemorySegment hwnd = (MemorySegment) GWIN_APP_CREATE.invokeExact();
            return hwnd.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code WinApplication._getDefaultBrowser}, which was {@code GlassApplication.cpp:537-559}: the
     * command line Windows has registered for the {@code https} scheme, or {@code null} when the
     * association cannot be read. {@code WinApplication._showDocument} parses it - a leading quoted
     * path, then {@code %1} substitution - so what has to be preserved here is the raw string,
     * including its quotes and its {@code %1}.
     */
    static String defaultBrowser() {
        return assocQueryCommand("https");
    }

    /**
     * {@code AssocQueryStringW(ASSOCF_NONE, ASSOCSTR_COMMAND, association, NULL, buffer, &cch)},
     * which is {@code GlassApplication.cpp:545-552} with its literal {@code L"https"} ({@code :540}) made
     * a parameter so that a test can drive the failure arm on an association it controls. The
     * association crosses as its code units and a NUL ({@link #wideString}), which for {@code "https"}
     * are the literal's own {@code WCHAR}s; a charset encoder would have written U+FFFD for an unpaired
     * surrogate in a test's association.
     * <p>
     * <b>The buffer is 260 characters, not 260 bytes.</b> The C declared
     * {@code WCHAR defaultBrowser_c[MAX_PATH]} - 520 bytes - and passed {@code cchBuffer = MAX_PATH}
     * (:541-542), a count in characters. Allocating 260 <em>bytes</em> and still claiming 260
     * characters would let {@code shlwapi} write 520 bytes into it, past the end of the segment and
     * into the arena's neighbours: a native-heap corruption FFM cannot bounds-check, because the
     * write happens inside {@code shlwapi.dll}, and one that only shows on a machine whose browser
     * command line is longer than 129 characters - which is the normal case.
     * <p>
     * {@code FAILED(hr)}, i.e. {@code hr < 0}, is {@code null}, exactly as at :554-556; that is also
     * what a command line longer than the buffer gives ({@code E_POINTER}), and the C had no
     * retry-with-a-bigger-buffer either. The string is read to its first NUL as code units
     * ({@link #codeUnitsToNul}), which is what {@code CreateJString} did with {@code NewString} and
     * {@code wcslen} ({@code Utils.h:279-285}), and it is read <em>inside</em> the arena, so the
     * {@code String} copy exists before the segment dies.
     * <p>
     * One deliberate difference, the same one {@link #robotCapture} makes with its buffer: the arena
     * zero-fills, so a call that reports success without writing anything yields {@code ""} where the
     * C read an uninitialised stack buffer and made a {@code String} out of whatever was there.
     */
    static String assocQueryCommand(String association) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment assoc = wideString(arena, association);
            MemorySegment buffer = arena.allocate(JAVA_CHAR, MAX_PATH);
            MemorySegment count = arena.allocateFrom(JAVA_INT, MAX_PATH);
            int hr = (int) Shlwapi.ASSOC_QUERY_STRING_W.invokeExact(ASSOCF_NONE, ASSOCSTR_COMMAND,
                    assoc, MemorySegment.NULL, buffer, count);
            if (hr < 0) {
                return null;
            }
            return codeUnitsToNul(buffer);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Robot input synthesis (Robot.cpp L35-245, now Java over user32)
     * ------------------------------------------------------------------------------------------- */

    /** {@code Robot.cpp} {@code KeyEvent(env, code, true)} (L35-60, L79). */
    static void robotKeyPress(int javaKeyCode) {
        keyEvent(javaKeyCode, true);
    }

    /** {@code Robot.cpp} {@code KeyEvent(env, code, false)} (L35-60, L90). */
    static void robotKeyRelease(int javaKeyCode) {
        keyEvent(javaKeyCode, false);
    }

    /**
     * One {@code INPUT_KEYBOARD} event, exactly as {@code KeyEvent} built it: no virtual key means
     * nothing is sent; the scan code comes from {@code MapVirtualKey(vk, MAPVK_VK_TO_VSC)} on this
     * thread's layout and is truncated to the {@code WORD} that {@code wScan} is;
     * {@code KEYEVENTF_KEYUP} marks a release and {@code KEYEVENTF_EXTENDEDKEY} an extended key.
     * {@code time} and {@code dwExtraInfo} stay 0, which the zero-filled arena allocation gives us as
     * the C's {@code INPUT keyInput = {0}} did.
     */
    private static void keyEvent(int javaKeyCode, boolean press) {
        int virtualKey = keyJavaToWindows(javaKeyCode);
        if (virtualKey == 0) {
            return;
        }
        int scanCode = mapVirtualKey(virtualKey, MAPVK_VK_TO_VSC);
        int flags = press ? 0 : KEYEVENTF_KEYUP;
        if (isExtendedKey(virtualKey)) {
            flags |= KEYEVENTF_EXTENDEDKEY;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment input = arena.allocate(INPUT_LAYOUT);
            input.set(JAVA_INT, INPUT_TYPE_OFFSET, INPUT_KEYBOARD);
            input.set(JAVA_SHORT, KI_WVK_OFFSET, (short) virtualKey);
            input.set(JAVA_SHORT, KI_WSCAN_OFFSET, (short) scanCode);
            input.set(JAVA_INT, KI_FLAGS_OFFSET, flags);
            sendOneInput(input);
        }
    }

    /**
     * {@code Robot.cpp} {@code _mouseMove} (L101-114) from {@code GlassScreen::FX2Win} onwards:
     * {@code MOUSEEVENTF_ABSOLUTE | MOUSEEVENTF_MOVE} with the coordinates normalised as
     * {@code (int) (w * 65536.0 / GetSystemMetrics(SM_CXSCREEN))} - the float widened to double,
     * divided in double by the metric, truncated toward zero. Without
     * {@code MOUSEEVENTF_VIRTUALDESK} that normalisation is against the <em>primary</em> monitor, so
     * points on another monitor legitimately fall outside 0..65535; that is the JNI behaviour and it is
     * kept.
     *
     * @param windowsX x in Windows pixels, already through {@link WinScreenTransform#fx2Win}
     * @param windowsY y in Windows pixels
     */
    static void robotMouseMove(float windowsX, float windowsY) {
        int dx = (int) (windowsX * 65536.0 / getSystemMetrics(SM_CXSCREEN));
        int dy = (int) (windowsY * 65536.0 / getSystemMetrics(SM_CYSCREEN));
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment input = arena.allocate(INPUT_LAYOUT);
            input.set(JAVA_INT, INPUT_TYPE_OFFSET, INPUT_MOUSE);
            input.set(JAVA_INT, MI_DX_OFFSET, dx);
            input.set(JAVA_INT, MI_DY_OFFSET, dy);
            input.set(JAVA_INT, MI_FLAGS_OFFSET, MOUSEEVENTF_ABSOLUTE | MOUSEEVENTF_MOVE);
            sendOneInput(input);
        }
    }

    /** {@code Robot.cpp} {@code _mousePress} (L153-190). */
    static void robotMousePress(int buttons) {
        mouseButtons(buttons, true);
    }

    /** {@code Robot.cpp} {@code _mouseRelease} (L197-234). */
    static void robotMouseRelease(int buttons) {
        mouseButtons(buttons, false);
    }

    /**
     * One {@code INPUT_MOUSE} event carrying every requested button flag, as both C bodies built it:
     * {@code SM_SWAPBUTTON} swaps the primary and secondary buttons ("Software Driving Software"), the
     * middle button is never swapped, and the two X buttons share {@code MOUSEEVENTF_XDOWN}/{@code XUP}
     * and are told apart by {@code mouseData}. {@code dx}, {@code dy} stay 0 and no
     * {@code MOUSEEVENTF_MOVE} is set. The event is sent even when no bit was set and {@code dwFlags}
     * is 0 - the C sent it too, and {@code GlassRobot} relies on nothing here.
     */
    private static void mouseButtons(int buttons, boolean press) {
        boolean swapped = getSystemMetrics(SM_SWAPBUTTON) != 0;
        int left = press ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_LEFTUP;
        int right = press ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_RIGHTUP;
        int flags = 0;
        int mouseData = 0;
        if ((buttons & GlassRobot.MOUSE_LEFT_BTN) != 0) {
            flags |= swapped ? right : left;
        }
        if ((buttons & GlassRobot.MOUSE_RIGHT_BTN) != 0) {
            flags |= swapped ? left : right;
        }
        if ((buttons & GlassRobot.MOUSE_MIDDLE_BTN) != 0) {
            flags |= press ? MOUSEEVENTF_MIDDLEDOWN : MOUSEEVENTF_MIDDLEUP;
        }
        if ((buttons & GlassRobot.MOUSE_BACK_BTN) != 0) {
            flags |= press ? MOUSEEVENTF_XDOWN : MOUSEEVENTF_XUP;
            mouseData |= XBUTTON1;
        }
        if ((buttons & GlassRobot.MOUSE_FORWARD_BTN) != 0) {
            flags |= press ? MOUSEEVENTF_XDOWN : MOUSEEVENTF_XUP;
            mouseData |= XBUTTON2;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment input = arena.allocate(INPUT_LAYOUT);
            input.set(JAVA_INT, INPUT_TYPE_OFFSET, INPUT_MOUSE);
            input.set(JAVA_INT, MI_FLAGS_OFFSET, flags);
            input.set(JAVA_INT, MI_MOUSE_DATA_OFFSET, mouseData);
            sendOneInput(input);
        }
    }

    /**
     * {@code Robot.cpp} {@code _mouseWheel} (L241-245):
     * {@code mouse_event(MOUSEEVENTF_WHEEL, 0, 0, wheelAmt * -1 * WHEEL_DELTA, 0)} - the legacy call,
     * not {@code SendInput}, and a positive {@code wheelAmt} therefore scrolls down. The negative
     * product crosses as the {@code DWORD} of its two's complement, which is what the int argument of
     * the descriptor does.
     */
    static void robotMouseWheel(int wheelAmt) {
        try {
            MOUSE_EVENT.invokeExact(MOUSEEVENTF_WHEEL, 0, 0, wheelAmt * -1 * WHEEL_DELTA, 0L);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code GetCursorPos} into a fresh {@code POINT}, as {@code _getMouseX} and {@code _getMouseY}
     * each did for themselves ({@code Robot.cpp} L121-146). The result is ignored exactly as the C
     * ignored it; on failure the zero-filled allocation reads back as (0, 0) where the C read an
     * uninitialised stack {@code POINT}.
     *
     * @return the cursor position in Windows pixels, {@code [x, y]}
     */
    static int[] cursorPosition() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment point = arena.allocate(POINT_LAYOUT);
            try {
                int ignoredResult = (int) GET_CURSOR_POS.invokeExact(point);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            return new int[] {point.get(JAVA_INT, POINT_X_OFFSET), point.get(JAVA_INT, POINT_Y_OFFSET)};
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Screen capture (gwin_robot_*, Robot.cpp L254-384)
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code Robot.cpp} {@code _getScreenCapture} (L275-307): fills the first {@code width * height}
     * elements of {@code data} with the screen rectangle, whose {@code x} and {@code y} are raw Windows
     * pixels - {@code GlassRobot.getScreenCapture} has already scaled them by the output scale, and the
     * JNI applied no transform here either.
     * <p>
     * The four argument checks are the JNI ones, in its order, and like the JNI they reject silently:
     * a null array, a non-positive width or height, {@code width >= (INT_MAX / 4) / height} and an
     * array shorter than the rectangle all leave {@code data} untouched. They are made here rather
     * than left to the C so that no buffer is allocated for a rectangle that will be refused.
     * {@code gwin_robot_capture} repeats them.
     * <p>
     * The pixels land in an off-heap buffer of a confined arena and are copied into {@code data}
     * afterwards, whatever the status - the JNI copied its {@code new BYTE[]} block unconditionally
     * too, and every GDI failure it ignored. The one difference is that this buffer starts zeroed, so
     * where a failed {@code GetDIBits} left the JNI's uninitialised block to be swizzled into arbitrary
     * opaque colours, it now yields {@code 0xFF000000}.
     *
     * @return {@link #GWIN_OK}, {@link #GWIN_ERR_INVALID_ARG}, or the {@code GWIN_ROBOT_ERR_*} code of
     *         the first GDI step that failed; {@link WinRobot} ignores it, as the JNI did
     */
    static int robotCapture(int x, int y, int width, int height, int[] data) {
        if (data == null) {
            return GWIN_ERR_INVALID_ARG;
        }
        if (width <= 0 || height <= 0) {
            return GWIN_ERR_INVALID_ARG;
        }
        final int maxPixels = Integer.MAX_VALUE / Integer.BYTES;
        if (width >= maxPixels / height) {
            return GWIN_ERR_INVALID_ARG;
        }
        final int numPixels = width * height;
        if (numPixels > data.length) {
            return GWIN_ERR_INVALID_ARG;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pixels = arena.allocate(JAVA_INT, numPixels);
            int status = capture(x, y, width, height, pixels, numPixels);
            MemorySegment.copy(pixels, JAVA_INT, 0, data, 0, numPixels);
            return status;
        }
    }

    /**
     * The colour of one Windows-pixel position, as {@code Robot.cpp} {@code _getPixelColor} (L254-268)
     * read it once its coordinates were through {@code FX2Win} and truncated: a 1x1 capture into a
     * value that starts at 0, with the status ignored, so a failed capture reads back as opaque black.
     * Deliberately not {@code GetPixel} on the screen DC, which does not see layered windows correctly.
     * <p>
     * The transform itself is {@link WinScreenTransform#fx2Win}, in Java, so the coordinates arriving
     * here are already Windows pixels. {@code gwin_robot_pixel_color}, the C-side equivalent that did its own
     * transform, was test-only under ABI 5, never bound here, and has been deleted.
     */
    static int robotPixelColorAt(int windowsX, int windowsY) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pixel = arena.allocate(JAVA_INT);
            int ignoredStatus = capture(windowsX, windowsY, 1, 1, pixel, 1);
            return pixel.get(JAVA_INT, 0);
        }
    }

    /** {@code gwin_robot_capture(x, y, width, height, argb, argb_len)}. */
    private static int capture(int x, int y, int width, int height, MemorySegment argb, long length) {
        try {
            return (int) GWIN_ROBOT_CAPTURE.invokeExact(x, y, width, height, argb, length);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * A capture into a caller-supplied segment, for the tests that drive {@code gwin_robot_capture}
     * directly - including the argument rejections, which {@link #robotCapture} makes in Java before
     * the C ever sees them.
     */
    static int captureInto(int x, int y, int width, int height, MemorySegment argb, long length) {
        return capture(x, y, width, height, argb, length);
    }

    /* ---------------------------------------------------------------------------------------------
     * Cursors (WinCursor; was GlassCursor.cpp and the Pixels.cpp icon sequence)
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code ShowCursor(bShow)} ({@code GlassCursor.cpp:173}).
     * <p>
     * The returned value is Windows' cursor <em>display counter</em>, not a success flag: every
     * {@code TRUE} raises it by one, every {@code FALSE} lowers it by one, and the pointer is drawn
     * while it is non-negative. {@link WinCursor} discards it, exactly as the C did - and gets away
     * with it because the latch in {@link WinCursor} only ever calls this on an actual change, so the
     * counter alternates between two adjacent values instead of drifting one step further down per
     * call until nothing can show the pointer again. It is returned here rather than swallowed so
     * that the tests can assert that discipline.
     *
     * @return the display counter after the call
     */
    static int showCursor(boolean show) {
        try {
            return (int) SHOW_CURSOR.invokeExact(show ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Pixels::CreateCursor}, i.e. {@link #buildIcon} with {@code fIcon = FALSE} and the
     * unclamped hotspot - what {@code Java_com_sun_glass_ui_win_WinCursor__1createCursor} ran
     * ({@code GlassCursor.cpp:154-158}). The returned {@code HCURSOR} is owned by the caller and is
     * never destroyed by this class: {@code native-glass/win} contains no {@code DestroyCursor} call
     * at all, and {@code com.sun.glass.ui.Cursor} keeps the value in its {@code ptr} field for the
     * life of the object, where {@code WinCursor.nativeHandleFor} reads it back. Adding a
     * {@code Cleaner} here would be a leak fix, not a migration.
     *
     * @return the {@code HCURSOR} as a {@code long}, or 0
     */
    static long cursorCreateCustom(int width, int height, Buffer pixels, int hotspotX, int hotspotY) {
        return buildIcon(width, height, pixels, ICONINFO_CURSOR, hotspotX, hotspotY);
    }

    /**
     * {@code Pixels::CreateIcon(env, jPixels)} with its defaults - {@code fIcon = TRUE}, hotspot
     * (0, 0) - which is what {@code Java_com_sun_glass_ui_win_WinWindow__1setIcon} built for a
     * non-null {@code Pixels}: the same five GDI / user32 steps as the cursor, on the calling thread
     * as before. <b>The {@code HICON} is handed to {@link #windowSetIcon} and is the window's from
     * then on</b>; this class never destroys it and must not. The one behaviour delta, on record:
     * the JNI reached this only after {@code FromHandle} found the GlassWindow, so a stale window
     * built no icon, whereas {@code WinWindow} builds first and a stale window then leaks the one
     * handle the C declined - unreachable through {@code Window.setIcon}, which checks the window is
     * open. The {@code GetBits() == NULL} crash of the JNI (a {@code Pixels} whose buffer could not
     * be attached) is not reproduced: {@link MemorySegment#ofBuffer} answers for every buffer.
     *
     * @return the {@code HICON} as a {@code long}, or 0
     */
    static long iconCreate(int width, int height, Buffer pixels) {
        return buildIcon(width, height, pixels, ICONINFO_ICON, 0, 0);
    }

    /**
     * The sequence of {@code Pixels::CreateIcon} ({@code Pixels.cpp:131-151}), shared by the custom
     * cursor ({@code fIcon = FALSE}, {@link #cursorCreateCustom}) and the window icon
     * ({@code fIcon = TRUE}, {@link #iconCreate}): a monochrome AND
     * mask, a 32-bit top-down {@code BI_RGB} DIB section holding the premultiplied BGRA pixels, then
     * {@code CreateIconIndirect} over an {@code ICONINFO} carrying the unclamped hotspot, a
     * {@code GdiFlush} and the deletion of both bitmaps ({@code CreateIconIndirect} has copied them).
     * <p>
     * Every failure is silent and yields 0, as the JNI's were ({@code Pixels.cpp:146} asserts only in
     * a debug build): a mask or colour bitmap that could not be built is still handed to
     * {@code CreateIconIndirect}, which then fails. The one check the JNI did not need is the first:
     * {@code com.sun.glass.ui.Pixels}' constructor rejects those dimensions before a {@code Pixels}
     * can exist ({@code Pixels.java:97-105,122-130}), but this method is also reachable from the
     * tests with arbitrary arguments, and {@code width * height * 4} is computed in {@code int} here
     * as it was in the C ({@code Pixels.cpp:111}).
     * <p>
     * Ownership of the result is the caller's business: see the two public entry points.
     *
     * @param pixels the premultiplied BGRA pixels, row-major and top-down; position and limit are
     *               ignored, as {@code GetDirectBufferAddress} and {@code array()} ignored them
     * @param kind {@link #ICONINFO_CURSOR} or {@link #ICONINFO_ICON}
     * @return the {@code HCURSOR} / {@code HICON} as a {@code long}, or 0
     */
    private static long buildIcon(int width, int height, Buffer pixels, int kind, int hotspotX, int hotspotY) {
        if (width <= 0 || height <= 0 || width > ((Integer.MAX_VALUE / 4) / height)) {
            return 0;
        }
        int imageSize = width * height * 4;
        Buffer whole = pixels.duplicate();
        whole.clear();
        MemorySegment bits = MemorySegment.ofBuffer(whole);
        if (bits.byteSize() < imageSize) {
            return 0;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mask = MemorySegment.NULL;
            MemorySegment colour = MemorySegment.NULL;
            try {
                mask = monochromeMask(arena, width, height);
                colour = colourBitmap(arena, width, height, bits, imageSize);
                MemorySegment iconInfo = arena.allocate(ICONINFO_LAYOUT);
                iconInfo.set(JAVA_INT, ICONINFO_FICON_OFFSET, kind);
                iconInfo.set(JAVA_INT, ICONINFO_XHOTSPOT_OFFSET, hotspotX);
                iconInfo.set(JAVA_INT, ICONINFO_YHOTSPOT_OFFSET, hotspotY);
                iconInfo.set(ADDRESS, ICONINFO_HBMMASK_OFFSET, mask);
                iconInfo.set(ADDRESS, ICONINFO_HBMCOLOR_OFFSET, colour);
                long icon = createIconIndirect(iconInfo);
                gdiFlush();
                return icon;
            } finally {
                // The C++ locals were destructors (Pixels.h:35), so both bitmaps went whatever the
                // way out of CreateIcon was, in reverse construction order; DeleteObject(NULL) is a
                // no-op the C made too, through the same `if (hBitmap)`, which is what makes the NULL
                // initialisers safe when the first helper is the one that throws.
                deleteObject(colour);
                deleteObject(mask);
            }
        }
    }

    /**
     * {@code Bitmap::Bitmap(int, int)} ({@code Pixels.cpp:33-66}): a zero-filled, word-aligned
     * monochrome bitmap. The two overflow guards and their three {@code stderr} messages are kept
     * although a valid {@code Pixels} cannot reach them, because that is what the C did; the
     * {@code width <= 0 || height <= 0} guard of {@code Pixels.cpp:35-37} is the one made by
     * {@link #cursorCreateCustom} before this is called.
     *
     * @return the {@code HBITMAP}, or {@link MemorySegment#NULL}
     */
    private static MemorySegment monochromeMask(Arena arena, int width, int height) {
        if (width > Integer.MAX_VALUE - 15) {
            System.err.println(MASK_FAILED);
            return MemorySegment.NULL;
        }
        // Each scan line of a monochrome bitmap must be word aligned; one plane, one bit per pixel.
        int scanLength = (((width + 15) >> 4) << 1);
        if (scanLength > Integer.MAX_VALUE / height) {
            System.err.println(MASK_FAILED);
            return MemorySegment.NULL;
        }
        MemorySegment maskBits = arena.allocate((long) scanLength * height);
        MemorySegment mask = createBitmap(width, height, 1, 1, maskBits);
        if (mask.address() == 0) {
            System.err.println(MASK_FAILED);
        }
        return mask;
    }

    /**
     * {@code DIBitmap::DIBitmap} ({@code Pixels.cpp:104-129}): a 32-bit top-down {@code BI_RGB} DIB
     * section, filled with the {@code Pixels} bytes. The negative {@code biHeight} is what makes the
     * rows top-down, and the copy is the {@code memcpy} of {@code Pixels.cpp:125} - the pixels never
     * cross as an argument to a downcall, so there is nothing here for
     * {@link Linker.Option#critical} to make faster and no Java array is pinned across a GDI call.
     *
     * @return the {@code HBITMAP}, or {@link MemorySegment#NULL}
     */
    private static MemorySegment colourBitmap(Arena arena, int width, int height, MemorySegment bits,
                                              int imageSize) {
        MemorySegment header = arena.allocate(BITMAPINFOHEADER_LAYOUT);
        header.set(JAVA_INT, BMI_SIZE_OFFSET, (int) BITMAPINFOHEADER_LAYOUT.byteSize());
        header.set(JAVA_INT, BMI_WIDTH_OFFSET, width);
        header.set(JAVA_INT, BMI_HEIGHT_OFFSET, -height);
        header.set(JAVA_SHORT, BMI_PLANES_OFFSET, (short) 1);
        header.set(JAVA_SHORT, BMI_BIT_COUNT_OFFSET, (short) 32);
        header.set(JAVA_INT, BMI_COMPRESSION_OFFSET, BI_RGB);
        header.set(JAVA_INT, BMI_SIZE_IMAGE_OFFSET, imageSize);

        MemorySegment bitmapBits = arena.allocate(ADDRESS);
        MemorySegment bitmap = createDibSection(header, bitmapBits);
        MemorySegment target = bitmapBits.get(ADDRESS, 0);
        if (target.address() == 0) {
            // The C dropped the HBITMAP here without deleting it (Pixels.cpp:124-127: Attach is
            // inside the `if`). Unreachable - CreateDIBSection either fails and returns NULL or sets
            // the pointer - and invisible to the caller, so the handle is released rather than lost.
            deleteObject(bitmap);
            return MemorySegment.NULL;
        }
        MemorySegment.copy(bits, 0, bound(target, imageSize), 0, imageSize);
        return bitmap;
    }

    /**
     * Bounds the {@code void*} {@code CreateDIBSection} wrote into its out-parameter, which arrives
     * as a zero-length segment. {@link MemorySegment#reinterpret} is restricted, which is why this is
     * a method of this class and not an expression in {@link #colourBitmap}.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment bound(MemorySegment address, long byteSize) {
        return address.reinterpret(byteSize);
    }

    private static MemorySegment createBitmap(int width, int height, int planes, int bitCount,
                                              MemorySegment bits) {
        try {
            return (MemorySegment) Gdi32.CREATE_BITMAP.invokeExact(width, height, planes, bitCount, bits);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static MemorySegment createDibSection(MemorySegment header, MemorySegment bitsOut) {
        try {
            return (MemorySegment) Gdi32.CREATE_DIB_SECTION.invokeExact(MemorySegment.NULL, header,
                    DIB_RGB_COLORS, bitsOut, MemorySegment.NULL, 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static long createIconIndirect(MemorySegment iconInfo) {
        try {
            MemorySegment icon = (MemorySegment) Gdi32.CREATE_ICON_INDIRECT.invokeExact(iconInfo);
            return icon.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static void deleteObject(MemorySegment object) {
        try {
            int ignoredResult = (int) Gdi32.DELETE_OBJECT.invokeExact(object);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static void gdiFlush() {
        try {
            int ignoredResult = (int) Gdi32.GDI_FLUSH.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Platform preferences, the half of PlatformSupport.cpp that is not WinRT
     * (querySystemColors, querySystemParameters), now Java over user32
     * ------------------------------------------------------------------------------------------- */

    /**
     * What {@code SystemParametersInfo(SPI_GETHIGHCONTRAST, ...)} reported, or {@code null} when it
     * returned {@code FALSE} - which the C treated as "put neither key"
     * (the {@code SPI_GETHIGHCONTRAST} arm of {@code PlatformSupport::querySystemParameters}).
     *
     * @param on {@code dwFlags & HCF_HIGHCONTRASTON}
     * @param scheme {@code lpszDefaultScheme}, and {@code null} whenever high contrast is off,
     *               because the C put an explicit null value for the scheme in that branch of
     *               {@code PlatformSupport::querySystemParameters}
     */
    record HighContrast(boolean on, String scheme) {
    }

    /**
     * {@code GetSysColor(nIndex)}, returning the raw {@code COLORREF}. It is {@code 0x00BBGGRR}: use
     * {@link #sysColorRed}, {@link #sysColorGreen} and {@link #sysColorBlue} rather than shifting it
     * by hand, because the wrong order yields plausible colours that no test would question.
     */
    static int sysColor(int index) {
        try {
            return (int) GET_SYS_COLOR.invokeExact(index);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** wingdi.h {@code GetRValue}: the LOW byte of a {@code COLORREF}. */
    static int sysColorRed(int colorRef) {
        return colorRef & 0xFF;
    }

    /** wingdi.h {@code GetGValue}. */
    static int sysColorGreen(int colorRef) {
        return (colorRef >>> 8) & 0xFF;
    }

    /** wingdi.h {@code GetBValue}: the HIGH byte of the three, not the low one. */
    static int sysColorBlue(int colorRef) {
        return (colorRef >>> 16) & 0xFF;
    }

    /**
     * {@code PlatformSupport::querySystemParameters}' first call, which passes the structure size
     * <em>twice</em> - once in {@code cbSize} and once as {@code uiParam} - because
     * {@code SPI_GETHIGHCONTRAST} is documented to want both and the C did exactly that.
     * <p>
     * One deliberate, strictly safer difference from the C, of the same kind as {@link #robotCapture}'s
     * zeroed buffer: the C left {@code lpszDefaultScheme} uninitialised, so a
     * {@code SystemParametersInfoW} that returned {@code TRUE} without writing it dereferenced stack
     * garbage; the arena zero-fills, so the same case yields a {@code NULL} pointer here, which is
     * read as "no scheme". The string itself is read exactly as the C read it - its UTF-16 code units up
     * to the first NUL, a lone surrogate included, which is what
     * {@code env->NewString((jchar*)value, wcslen(value))} did in {@code PlatformSupport::putString}; the
     * memory belongs to Windows and is neither retained nor freed.
     *
     * @return {@code null} when the call failed, which means "put neither high-contrast key"
     */
    static HighContrast highContrast() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(HIGHCONTRAST_LAYOUT);
            int structureSize = (int) HIGHCONTRAST_LAYOUT.byteSize();
            info.set(JAVA_INT, HC_CBSIZE_OFFSET, structureSize);
            if (systemParametersInfo(SPI_GETHIGHCONTRAST, structureSize, info) == 0) {
                return null;
            }
            boolean on = (info.get(JAVA_INT, HC_FLAGS_OFFSET) & HCF_HIGHCONTRASTON) != 0;
            return new HighContrast(on, on ? schemeName(info.get(ADDRESS, HC_SCHEME_OFFSET)) : null);
        }
    }

    /**
     * {@code PlatformSupport::querySystemParameters}' second call:
     * {@code SystemParametersInfo(SPI_GETCLIENTAREAANIMATION, 0, &BOOL, 0)}. The C's {@code BOOL} was
     * uninitialised and reached {@code putBoolean(const bool)}, so its semantics are "any nonzero is
     * true"; the arena zero-fills, so a {@code TRUE} return that writes nothing gives {@code false}
     * instead of garbage.
     *
     * @return {@code null} when the call failed, which means "do not put the key"
     */
    static Boolean clientAreaAnimation() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment value = arena.allocate(JAVA_INT);
            if (systemParametersInfo(SPI_GETCLIENTAREAANIMATION, 0, value) == 0) {
                return null;
            }
            return value.get(JAVA_INT, 0) != 0;
        }
    }

    /** {@code SystemParametersInfoW(uiAction, uiParam, pvParam, 0)}; the C never passed {@code fWinIni}. */
    private static int systemParametersInfo(int action, int uiParam, MemorySegment parameter) {
        try {
            return (int) SYSTEM_PARAMETERS_INFO_W.invokeExact(action, uiParam, parameter, 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The {@code LPWSTR} Windows wrote into {@code HIGHCONTRAST.lpszDefaultScheme}, read to its NUL as
     * code units ({@link #codeUnitsToNul}). The bound is {@link Long#MAX_VALUE} rather than a guessed
     * maximum because the C measured the string with {@code wcslen} and had no maximum either;
     * {@link #bound} is where the restricted call lives.
     * <p>
     * Package-private rather than private so that the shim can drive it with a synthetic string: the
     * real one only exists while high contrast is on, so this is otherwise the one line of the
     * preferences code that no automated test can reach.
     */
    static String schemeName(MemorySegment scheme) {
        if (scheme.address() == 0) {
            return null;
        }
        return codeUnitsToNul(bound(scheme, Long.MAX_VALUE));
    }

    /**
     * The UTF-16 code units of {@code chars} up to its first {@code U+0000}, as a {@code String}: what
     * {@code env->NewString((jchar*)p, wcslen(p))} made of a {@code wchar_t*}, and so what the former
     * {@code CreateJString} and {@code PlatformSupport::putString} returned. Copied unit for unit, not
     * decoded: {@code MemorySegment.getString(0, UTF_16LE)} runs a charset decoder, which replaces an
     * unpaired surrogate with {@code U+FFFD}, while {@code String(char[])} keeps it as the {@code char}
     * it was. Windows does not promise well-formed UTF-16 - a name is a sequence of {@code WCHAR}s -
     * so the decoder would be a silent change of value. Unaligned, as {@code getString} was, because
     * the pointer can be Windows' own.
     * <p>
     * The walk stops at the end of {@code chars}: a buffer with no NUL inside it throws
     * {@link IndexOutOfBoundsException}, as {@code getString} did.
     */
    private static String codeUnitsToNul(MemorySegment chars) {
        long length = 0;
        while (chars.getAtIndex(JAVA_CHAR_UNALIGNED, length) != '\0') {
            length++;
        }
        return new String(chars.asSlice(0, length * JAVA_CHAR_UNALIGNED.byteSize()).toArray(JAVA_CHAR_UNALIGNED));
    }

    /* ---------------------------------------------------------------------------------------------
     * Platform preferences, the WinRT half: PlatformSupport keeps the IUISettings and
     * INetworkInformation objects and hands their results out as POD (gwin_prefs_query_*), and the
     * one upcall that survives - the preferences-changed event - comes back through
     * GwinPrefsCallbacks
     * ------------------------------------------------------------------------------------------- */

    /**
     * What {@code gwin_prefs_query_ui_settings} reported: {@code PlatformSupport::queryUISettings}
     * with the map building removed.
     * <p>
     * Each group has its own validity flag because the C returned out of {@code queryUISettings} on
     * the first {@code RoException} and dropped every later key with it; the flags are monotone, and a
     * false flag means <em>no key at all</em> for that group, never a key with the value {@code false}.
     * The two booleans came from an {@code unsigned char} handed to {@code putBoolean(const bool)}, so
     * their semantics are "any nonzero is true" and the C already reduced them to that.
     *
     * @param available false when {@code UISettings} could not be activated, or when there is no live
     *                  toolkit for it to be activated in - which is the case the JNI answered with a
     *                  {@code NULL} map
     * @param colors the nine {@code 0xAARRGGBB} values in the order of {@link #GWIN_UI_COLOR_COUNT},
     *               always {@link #GWIN_UI_COLOR_COUNT} long and read straight out of the struct; the
     *               array is this record's own copy, but it is an array, so this record has no useful
     *               {@code equals} and nothing compares two of them
     */
    record UiSettings(boolean available, boolean colorsValid, int[] colors, boolean advancedEffectsValid,
                      boolean advancedEffectsEnabled, boolean autoHideValid, boolean autoHideScrollBars) {
    }

    /**
     * What {@code gwin_prefs_query_network} reported: {@code PlatformSupport::queryNetworkInformation}
     * with the map building removed.
     *
     * @param available false when the activation factory was unavailable or an {@code RoException} was
     *                  caught before the C reached its {@code putString} - in both cases there is no
     *                  key. True with {@link #GWIN_NET_COST_UNKNOWN} is the <em>different</em> case
     *                  where the C did write the key, with the value {@code "Unknown"}
     * @param costType one of the four {@code GWIN_NET_COST_*} values, which are this ABI's numbering
     *                 and not WinRT's
     */
    record NetworkInfo(boolean available, int costType) {
    }

    /** {@code sizeof(GwinUiSettings)} as the C compiler saw it; {@link #GWIN_UI_SETTINGS_LAYOUT} must agree. */
    static int sizeOfUiSettings() {
        try {
            return (int) GWIN_SIZEOF_UI_SETTINGS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code sizeof(GwinNetworkInfo)}; {@link #GWIN_NETWORK_INFO_LAYOUT} must agree. */
    static int sizeOfNetworkInfo() {
        try {
            return (int) GWIN_SIZEOF_NETWORK_INFO.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The {@code IUISettings} half of the preference map. Call on the Glass toolkit thread: that is
     * the apartment those WinRT objects were activated in, and the thread the JNI collected on.
     *
     * @return a record whose {@code available} is false when there is no toolkit or no
     *         {@code UISettings} - not an exception, because the C answered that case with fewer keys
     */
    static UiSettings uiSettings() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(GWIN_UI_SETTINGS_LAYOUT);
            queryUiSettings(out);
            int[] colors = new int[GWIN_UI_COLOR_COUNT];
            MemorySegment.copy(out, JAVA_INT, UI_COLORS_OFFSET, colors, 0, GWIN_UI_COLOR_COUNT);
            return new UiSettings(out.get(JAVA_INT, UI_AVAILABLE_OFFSET) != 0,
                    out.get(JAVA_INT, UI_COLORS_VALID_OFFSET) != 0,
                    colors,
                    out.get(JAVA_INT, UI_ADVANCED_VALID_OFFSET) != 0,
                    out.get(JAVA_INT, UI_ADVANCED_ENABLED_OFFSET) != 0,
                    out.get(JAVA_INT, UI_AUTO_HIDE_VALID_OFFSET) != 0,
                    out.get(JAVA_INT, UI_AUTO_HIDE_OFFSET) != 0);
        }
    }

    /** The {@code INetworkInformation} half of the preference map; same threading as {@link #uiSettings}. */
    static NetworkInfo networkInfo() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(GWIN_NETWORK_INFO_LAYOUT);
            queryNetwork(out);
            return new NetworkInfo(out.get(JAVA_INT, NET_AVAILABLE_OFFSET) != 0,
                    out.get(JAVA_INT, NET_COST_TYPE_OFFSET));
        }
    }

    private static void queryUiSettings(MemorySegment out) {
        try {
            int status = (int) GWIN_PREFS_QUERY_UI_SETTINGS.invokeExact(out);
            requireOk(status, "gwin_prefs_query_ui_settings");
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static void queryNetwork(MemorySegment out) {
        try {
            int status = (int) GWIN_PREFS_QUERY_NETWORK.invokeExact(out);
            requireOk(status, "gwin_prefs_query_network");
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The only status either query can return besides {@link #GWIN_OK} is
     * {@link #GWIN_ERR_INVALID_ARG}, and only for a {@code NULL} buffer - every tolerated failure is
     * reported through the struct's flags instead. So anything else here is a facade bug, not a
     * platform condition.
     */
    private static void requireOk(int status, String function) {
        if (status != GWIN_OK) {
            throw new AssertionError(function + " returned " + status);
        }
    }

    /** {@code int32_t (*preferences_changed)(void* user, int32_t types)}. */
    private static final FunctionDescriptor PREFS_CHANGED_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT);

    private static boolean preferencesCallbackInstalled;

    /**
     * Hands {@code glass.dll} the one callback that replaced the JNI body of
     * {@code PlatformSupport::updatePreferences}: {@link WinPreferences} collects, compares and
     * notifies, and no {@code java.util.Map} is built in C. The C built the map with eleven upcalls;
     * the ten map-building ones became unreachable with this install and have since been deleted,
     * so a change that finds no callback installed is not delivered at all.
     * <p>
     * <b>Call this exactly once, from {@code WinApplication}'s static initializer.</b> That runs on the
     * launcher thread before {@code runLoop} starts the Glass toolkit thread, so "installed before the
     * first {@code WndProc} message" and "never written from a second thread" both hold by
     * construction. Installing it any later drops every notification that arrives before the install;
     * installing it lazily, on the first preference read, would drop the ones during startup.
     * <p>
     * The table is copied by the library, so the confined arena that holds it is closed here. The stub
     * inside it is in {@link Arena#global()} and stays there: the two WinRT sinks are registered with
     * {@code add_AutoHideScrollBarsChanged} / {@code add_NetworkStatusChanged} and their
     * {@code EventRegistrationToken} is discarded, there is no {@code remove_*Changed} anywhere in the
     * library, so there is no point at which native code provably can no longer call it and no arena
     * with a close path would be correct.
     */
    static synchronized void installPreferencesCallback() {
        if (preferencesCallbackInstalled) {
            return;
        }
        MemorySegment callback = upcallStub(upcallTarget("onPreferencesChanged", PREFS_CHANGED_FD),
                PREFS_CHANGED_FD, Arena.global());
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(GWIN_PREFS_CALLBACKS_LAYOUT);
            table.set(ADDRESS, PREFS_CHANGED_OFFSET, callback);
            int status = (int) GWIN_PREFS_SET_CALLBACKS.invokeExact(table, MemorySegment.NULL);
            requireOk(status, "gwin_prefs_set_callbacks");
        } catch (Throwable t) {
            throw unexpected(t);
        }
        preferencesCallbackInstalled = true;
    }

    /** Whether {@link #installPreferencesCallback} has run in this JVM. */
    static synchronized boolean preferencesCallbackInstalled() {
        return preferencesCallbackInstalled;
    }

    /**
     * The {@code GwinPrefsCallbacks.preferences_changed} body: {@code PlatformSupport::updatePreferences}
     * minus the collecting, which {@link WinPreferences#update} now does in Java. {@code user} is the
     * pointer {@link #installPreferencesCallback} passed, which is {@code NULL} - the registry the C
     * needed a {@code this} for is a static field here.
     * <p>
     * <b>Thread.</b> The {@code WM_SETTINGCHANGE} / {@code WM_THEMECHANGED} / {@code WM_SYSCOLORCHANGE}
     * / {@code WM_DWMCOLORIZATIONCOLORCHANGED} arms reach this on the Glass toolkit thread. The two
     * WinRT sinks may not: they are WRL {@code Callback<>} delegates, agile by default (they aggregate
     * the free-threaded marshaler), so WinRT does not marshal them into the apartment
     * {@code RoInitialize(RO_INIT_SINGLETHREADED)} created on the toolkit thread and can run them on a
     * WinRT thread of its own; the upcall stub attaches such a thread. {@link WinPreferences} is
     * written for that second thread.
     * <p>
     * <b>It must not throw.</b> An exception escaping an upcall stub terminates the JVM, so
     * {@code Throwable} is caught and answered with 0, which is the C's "nothing changed" and its
     * {@code DefWindowProc} arm. The reporting is guarded in turn, exactly as
     * {@code CheckAndClearException} re-checked after its own report ({@code Utils.cpp:64-67}).
     *
     * <p>
     * Package-private, like {@link #onNotifyCommand}, so that {@code WinGlassNativeShim} can drive the
     * target and {@code WinPreferencesNativeTest} can assert the swallow; nothing but the stub calls it.
     *
     * @return 1 if the preferences changed, 0 otherwise; {@code GlassApplication::WindowProc} chooses
     *         between {@code return 0} and {@code DefWindowProc} on this
     */
    static int onPreferencesChanged(MemorySegment user, int types) {
        try {
            return WinPreferences.update(types) ? 1 : 0;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return 0;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Application: the Win32 message pump, its nested twin, and the two ways of getting a Runnable
     * onto the Glass toolkit thread (WinApplication; was six JNI entry points of GlassApplication.cpp)
     * ------------------------------------------------------------------------------------------- */

    /** {@code void (*run_runnable)(void* user, int64_t runnable_id)}. */
    private static final FunctionDescriptor RUN_RUNNABLE_FD = FunctionDescriptor.ofVoid(ADDRESS, JAVA_LONG);

    /**
     * The runnables the library has been told about but has not yet run, keyed by the id that crosses
     * the ABI where the JNI passed a {@code jobject}. {@code glass.dll} holds no Java reference at all
     * now, so there is nothing for it to add, delete or leak.
     * <p>
     * The references are strong, exactly as the {@code JGlobalRef} the {@code Action} held was
     * ({@code GlassApplication.h:61-62}): a runnable submitted and then dropped by its submitter could
     * not be collected before it ran, and must still not be. {@code JPEGNative}'s weak registry is the
     * wrong model here for the same reason it was for {@link #TIMERS}.
     * <p>
     * <b>There is no sweeper, and that is deliberate.</b> An entry is removed by whoever can prove the
     * callback will not arrive: {@link #onRunRunnable} when it runs one (one-shot),
     * {@link #invokeAndWait} in a {@code finally} because the downcall does not return until the
     * runnable has run or has been refused, {@link #runLoop} in a {@code finally} because the JNI's
     * launchable was a parameter whose local ref died with the call, and
     * {@link #submitForLaterInvocation} when the downcall answers {@link #GWIN_ERR_NO_TOOLKIT}, which
     * is the case where the C destroyed the action. What is left over is a runnable posted to the
     * toolkit window whose message was still on the queue at shutdown, and that leaked its
     * {@code JGlobalRef} in precisely the same way. Reproducing a pre-existing leak is the job;
     * inventing cleanup for it is not.
     */
    private static final Map<Long, Runnable> RUNNABLES = new ConcurrentHashMap<>();

    /**
     * Ids run from 1, because {@code gwin_run_loop} spells "no launchable" as 0 - which is what
     * {@code jLaunchable != NULL} tested ({@code GlassApplication.cpp:397}). They are monotone and
     * never reused, so a late or duplicate callback for an id that has already been removed is a
     * no-op rather than a call on the wrong runnable.
     */
    private static final AtomicLong NEXT_RUNNABLE_ID = new AtomicLong(1L);

    private static boolean applicationCallbackInstalled;

    /** {@code sizeof(GwinAppCallbacks)} as the C compiler saw it; {@link #GWIN_APP_CALLBACKS_LAYOUT} must agree. */
    static int sizeOfAppCallbacks() {
        try {
            return (int) GWIN_SIZEOF_APP_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Hands {@code glass.dll} the one callback that replaces all three of {@code GlassApplication.cpp}'s
     * {@code Runnable.run} upcalls - the launchable ({@code :398}), {@code _invokeAndWait}
     * ({@code :469}) and {@code _submitForLaterInvocation} ({@code :489}), which all resolved the same
     * cached {@code javaIDs.Runnable.run} ({@code :344}).
     * <p>
     * <b>Call this exactly once, from {@code WinApplication}'s static initializer</b>, beside
     * {@link #installPreferencesCallback}: that runs on the launcher thread, after the library is
     * loaded and before {@code Application.run()} creates the toolkit window, so the table is
     * installed before the first {@code WM_DO_ACTION} and is never written from a second thread.
     * Installing it lazily would drop the launchable itself, which is the first thing
     * {@code gwin_run_loop} does.
     * <p>
     * The table is copied by the library, so the confined arena that holds it is closed here. The stub
     * inside it is in {@link Arena#global()} and stays there, for the reason
     * {@code GwinPrefsCallbacks} gives: there is no point at which the library can promise no further
     * callback - a message posted by {@code gwin_invoke_later} can be dispatched at any time up to the
     * last {@code GetMessage} - so no arena with a close path would be correct.
     */
    static synchronized void installApplicationCallback() {
        if (applicationCallbackInstalled) {
            return;
        }
        MemorySegment callback = upcallStub(upcallTarget("onRunRunnable", RUN_RUNNABLE_FD),
                RUN_RUNNABLE_FD, Arena.global());
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(GWIN_APP_CALLBACKS_LAYOUT);
            table.set(ADDRESS, RUN_RUNNABLE_OFFSET, callback);
            int status = (int) GWIN_APP_SET_CALLBACKS.invokeExact(table, MemorySegment.NULL);
            requireOk(status, "gwin_app_set_callbacks");
        } catch (Throwable t) {
            throw unexpected(t);
        }
        applicationCallbackInstalled = true;
    }

    /** Whether {@link #installApplicationCallback} has run in this JVM. */
    static synchronized boolean applicationCallbackInstalled() {
        return applicationCallbackInstalled;
    }

    /**
     * The {@code GwinAppCallbacks.run_runnable} body: the three {@code CallVoidMethod(runnable,
     * javaIDs.Runnable.run)} sites of {@code GlassApplication.cpp} in one method. {@code user} is the
     * pointer {@link #installApplicationCallback} passed, which is {@code NULL} - the registry the C
     * needed a {@code this} for is a static field here.
     * <p>
     * <b>Thread.</b> Always the Glass toolkit thread, in three situations: inside
     * {@code gwin_run_loop} before the pump starts and inside the OLE apartment it created, for the
     * launchable; and inside {@code DispatchMessage} for the {@code WM_DO_ACTION} and
     * {@code WM_DO_ACTION_LATER} arms of {@code GlassApplication::WindowProc}. An inter-thread
     * {@code SendMessage} still runs the window procedure on the window's owning thread, so no foreign
     * thread can reach this.
     * <p>
     * <b>Re-entrant.</b> The runnable may call {@link #invokeAndWait}, {@link #submitForLaterInvocation}
     * or {@link #enterNestedEventLoop}, and a nested pump dispatches further {@code WM_DO_ACTION}
     * messages on top of this frame. Nothing here holds a lock; the C held none either.
     * <p>
     * <b>It must not throw, and it returns nothing.</b> An exception escaping an upcall stub terminates
     * the JVM, so {@code Throwable} is caught and reported exactly as {@code CheckAndClearException}
     * did ({@code Utils.cpp:50-71}). None of the three sites consumed a return value and the
     * {@code WM_DO_ACTION} arms answer {@code return 0} whatever the runnable did, so this slot is
     * {@code void} - unlike {@link #onPreferencesChanged}, whose return really does choose between
     * {@code return 0} and {@code DefWindowProc}.
     * <p>
     * A callback for an id that has already been removed is dropped, where the C would have called
     * {@code CallVoidMethod} on a released global ref.
     */
    static void onRunRunnable(MemorySegment user, long runnableId) {
        try {
            // One-shot, and the local is what keeps the runnable reachable across run().
            Runnable runnable = RUNNABLES.remove(runnableId);
            if (runnable != null) {
                runnable.run();
            }
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinApplication__1runLoop} ({@code GlassApplication.cpp:393-421}):
     * {@code OleInitialize}, run the launchable inside that apartment, then pump until the
     * {@code GlassApplication} instance is destroyed. <b>Blocks for the life of the application</b> and
     * must be called on the thread that is to be the Glass toolkit thread - {@code WinApplication}
     * names it {@code WindowsNativeRunloopThread} and calls {@code setEventThread} on it before
     * starting it, which this call does not change and must not.
     * <p>
     * The launchable is registered rather than passed, and removed in a {@code finally} because the
     * JNI's {@code jLaunchable} was a parameter whose local ref died when the entry point returned.
     * In practice {@link #onRunRunnable} has already removed it, before the pump even started.
     *
     * @param launchable what to run before the first {@code GetMessage}, or {@code null} for nothing -
     *                   which is the {@code jLaunchable != NULL} test, spelled as the id 0
     */
    static void runLoop(Runnable launchable) {
        long id = launchable == null ? 0L : registerRunnable(launchable);
        try {
            GWIN_RUN_LOOP.invokeExact(id);
        } catch (Throwable t) {
            throw unexpected(t);
        } finally {
            RUNNABLES.remove(id);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinApplication__1terminateLoop} ({@code :428-435}):
     * {@code DestroyWindow} on the toolkit window if it is still a window. Idempotent - the handle is
     * {@code NULL} once there is no instance and {@code IsWindow(NULL)} is false - and callable only
     * from the thread that created the window, which is where
     * {@code Application.finishTerminating()} runs.
     * <p>
     * What happens inside it is worth knowing: {@code WM_DESTROY} runs
     * {@code RegisterClipboardViewerId(0)}, which dials the {@code dispose_peer} slot of the clipboard
     * table for a registered clipboard peer ({@code GlassApplication::DisposeRegisteredClipboard}), so
     * this downcall can re-enter Java through an upcall on this thread; then {@code WM_NCDESTROY} clears
     * the instance and the pump in {@link #runLoop} returns.
     */
    static void terminateLoop() {
        try {
            GWIN_TERMINATE_LOOP.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code GlassApplication::EnterNestedEventLoop} ({@code :248-270}) without its {@code jobject}
     * half: pump until {@code gwin_leave_nested_event_loop} raises the flag or the instance goes away.
     * <b>Blocks and dispatches</b>, so {@link #onRunRunnable} can be re-entered on top of it. The
     * return value never left Java in the first place and {@code WinApplication} holds it now.
     */
    static void enterNestedEventLoop() {
        try {
            GWIN_ENTER_NESTED_EVENT_LOOP.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code GlassApplication::LeaveNestedEventLoop} ({@code :273-277}) minus the value: raise the
     * flag. Toolkit thread only, which is why the flag needs no barrier. The caller stores its return
     * value <em>before</em> calling this, in the order the C used.
     */
    static void leaveNestedEventLoop() {
        try {
            GWIN_LEAVE_NESTED_EVENT_LOOP.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinApplication__1invokeAndWait} ({@code :464-477}): wrap the
     * runnable in an {@code Action} and hand it to {@code GlassApplication::ExecAction}, which is
     * {@code SendMessage(toolkit HWND, WM_DO_ACTION, action, 0)}. <b>Blocks</b> until
     * {@link #onRunRunnable} has returned on the toolkit thread.
     * <p>
     * <b>{@link #GWIN_ERR_NO_TOOLKIT} is ignored, on purpose.</b> There was no toolkit window, so the
     * runnable did not run and no callback will arrive - and that is exactly what the JNI did, because
     * {@code ExecAction} returned early and silently ({@code :229-235}). Throwing here, or blocking,
     * would be a new behaviour on a path that runs during shutdown.
     * <p>
     * If the caller is itself the toolkit thread, {@code SendMessage} runs the window procedure inline
     * on this stack; {@code Application.invokeAndWait} short-circuits that case in Java first
     * ({@code Application.java:463-465}), as it always did.
     */
    static void invokeAndWait(Runnable runnable) {
        long id = registerRunnable(runnable);
        try {
            execAction(id);
        } finally {
            RUNNABLES.remove(id);
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinApplication__1submitForLaterInvocation} ({@code :484-497}):
     * {@code PostMessage(toolkit HWND, WM_DO_ACTION_LATER, action, 0)}; the window procedure runs the
     * action and then deletes it. Returns as soon as the message is posted.
     * <p>
     * <b>Callable from any thread</b> - Glass's {@code InvokeLaterDispatcher} thread is the usual one
     * ({@code InvokeLaterDispatcher.java:122}, and {@code :143} through its own
     * {@code invokeAndWait}).
     * <p>
     * On {@link #GWIN_ERR_NO_TOOLKIT} the C destroyed the action, so no callback will ever arrive for
     * this id and the registry entry is dropped here. The runnable is not run and nothing is thrown,
     * which is what {@code ExecActionLater} did ({@code :238-245}).
     */
    static void submitForLaterInvocation(Runnable runnable) {
        long id = registerRunnable(runnable);
        if (!postAction(id)) {
            RUNNABLES.remove(id);
        }
    }

    /**
     * Takes the next id and publishes the runnable under it before any downcall can carry it, which is
     * the ordering {@link #onRunRunnable} needs: the callback can arrive on the toolkit thread while
     * the submitting thread is still inside {@code PostMessage}. Package-private, with
     * {@link #onRunRunnable}, so that {@code WinGlassNativeShim} can drive the target's one-shot and
     * {@code Throwable}-swallowing arms without a pump.
     */
    static long registerRunnable(Runnable runnable) {
        long id = NEXT_RUNNABLE_ID.getAndIncrement();
        RUNNABLES.put(id, runnable);
        return id;
    }

    /** {@code gwin_invoke_and_wait}; the status is discarded - see {@link #invokeAndWait}. */
    private static void execAction(long id) {
        try {
            int ignoredStatus = (int) GWIN_INVOKE_AND_WAIT.invokeExact(id);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_invoke_later}.
     *
     * @return whether the action was posted; false is {@link #GWIN_ERR_NO_TOOLKIT}, after which the
     *         library has destroyed the action and no callback will arrive
     */
    private static boolean postAction(long id) {
        try {
            return scheduled((int) GWIN_INVOKE_LATER.invokeExact(id), "gwin_invoke_later");
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@link #GWIN_OK} and {@link #GWIN_ERR_NO_TOOLKIT} are the only two statuses either scheduling
     * export can answer, so anything else is a facade bug rather than a platform condition - the same
     * reasoning as {@link #requireOk}.
     */
    private static boolean scheduled(int status, String function) {
        if (status == GWIN_OK) {
            return true;
        }
        if (status == GWIN_ERR_NO_TOOLKIT) {
            return false;
        }
        throw new AssertionError(function + " returned " + status);
    }

    /**
     * How many runnables this class is holding for {@code glass.dll}; for
     * {@code WinApplicationNativeTest}. It is the FFM counterpart of the {@code JGlobalRef}s the
     * {@code Action}s used to hold, and the only externally visible evidence that they are released.
     */
    static int runnableRegistrySize() {
        return RUNNABLES.size();
    }

    /* ---------------------------------------------------------------------------------------------
     * Multimedia timer (WinTimer; was Timer.cpp and Timer.h over winmm)
     * ------------------------------------------------------------------------------------------- */

    /**
     * One running timer: what {@code RunnableTimer} held ({@code Timer.cpp:53-67},
     * {@code Timer.h:106}) minus the two members that only existed because a winmm worker thread had
     * no {@code JNIEnv} - the cached {@code JNIEnv*} and the JNI global ref.
     */
    private static final class TimerEntry {

        /** Was {@code JGlobalRef<jobject> runnable} ({@code Timer.cpp:67}). */
        private final Runnable runnable;

        /** Was {@code UINT Timer::id} ({@code Timer.h:106}); 0 until {@code timeSetEvent} returns. */
        private volatile int mmTimerId;

        private TimerEntry(Runnable runnable) {
            this.runnable = runnable;
        }
    }

    /**
     * The running timers, keyed by the id {@link #timerStart} hands out and {@code timeSetEvent}
     * carries back in {@code dwUser}.
     * <p>
     * The references are strong, on purpose: a {@code com.sun.glass.ui.Timer} that becomes unreachable
     * without {@link com.sun.glass.ui.Timer#stop() stop()} keeps ticking and pins its {@code Runnable}
     * for the life of the JVM. That is exactly what the C did - an abandoned {@code RunnableTimer} was
     * a leaked C++ object plus a leaked JNI global ref ({@code Timer.cpp:42,67}), and its
     * {@code timeSetEvent} was never killed either. Making this a weak map would silently stop such a
     * timer, which is a behaviour change and not a leak fix; it does not belong in this migration.
     * {@code WinTimerNativeTest.anAbandonedTimerKeepsTickingAndPinsItsRunnable} pins the parity.
     */
    private static final Map<Long, TimerEntry> TIMERS = new ConcurrentHashMap<>();

    /**
     * Ids run from 1, so {@code dwUser} is never 0 - as it never was in the C, where it was a non-null
     * {@code this} ({@code Timer.h:72}). 0 stays reserved for "failed to start" at the
     * {@code com.sun.glass.ui.Timer} boundary ({@code Timer.java:97,117}). It is a Java-assigned id,
     * never a pointer.
     */
    private static final AtomicLong NEXT_TIMER_ID = new AtomicLong(1L);

    /**
     * Guards the four fields below, which were the plain statics {@code Timer::timersCount},
     * {@code Timer::wTimerRes} and {@code Timer::tc} ({@code Timer.h:102-104}) - mutated from any
     * thread that started or stopped a timer, with the C's own comment admitting the race
     * ({@code Timer.h:81-82}). Locking them can only remove corruption: the lock is never held across
     * {@code timeSetEvent} or {@code timeKillEvent}, and the callback path never takes it.
     */
    private static final Object TIMER_LOCK = new Object();

    /** {@code Timer::timersCount} ({@code Timer.h:102}): the {@code timeBeginPeriod} refcount. */
    private static int timersCount;

    /** {@code Timer::wTimerRes} ({@code Timer.h:103}): the resolution every timer is started with. */
    private static int timerRes;

    /** {@code Timer::tc.wPeriodMin} ({@code Timer.h:104}). */
    private static int periodMin;

    /** {@code Timer::tc.wPeriodMax} ({@code Timer.h:104}). */
    private static int periodMax;

    /**
     * {@code Timer::InitTC} ({@code Timer.h:80-94}), including its memo and its failure value: the
     * query is repeated whenever <em>both</em> fields are 0, so a machine on which
     * {@code timeGetDevCaps} fails retries on every call and reports 0/0 in the meantime. The caller
     * holds {@link #TIMER_LOCK}.
     *
     * @return false exactly when {@code timeGetDevCaps} failed
     */
    private static boolean initTimeCaps() {
        if (periodMin == 0 && periodMax == 0) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment caps = arena.allocate(TIMECAPS_LAYOUT);
                int result;
                try {
                    result = (int) Winmm.TIME_GET_DEV_CAPS.invokeExact(caps,
                            (int) TIMECAPS_LAYOUT.byteSize());
                } catch (Throwable t) {
                    throw unexpected(t);
                }
                if (result != TIMERR_NOERROR) {
                    periodMin = 0;
                    periodMax = 0;
                    return false;
                }
                periodMin = caps.get(JAVA_INT, TIMECAPS_MIN_OFFSET);
                periodMax = caps.get(JAVA_INT, TIMECAPS_MAX_OFFSET);
                timerRes = clampToDeviceRange(periodMin);
            }
        }
        return true;
    }

    /**
     * {@code min(max(tc.wPeriodMin, 1), tc.wPeriodMax)} ({@code Timer.h:90}, "We want 1 ms accuracy"),
     * on {@code UINT}s through the MSVC {@code min}/{@code max} macros - that is, with
     * <em>unsigned</em> comparisons. {@link Integer#compareUnsigned} keeps that exact, rather than
     * resting on an argument about the range {@code timeGetDevCaps} returns.
     */
    private static int clampToDeviceRange(int wPeriodMin) {
        int atLeastOneMillisecond = Integer.compareUnsigned(wPeriodMin, 1) > 0 ? wPeriodMin : 1;
        return Integer.compareUnsigned(atLeastOneMillisecond, periodMax) < 0
                ? atLeastOneMillisecond : periodMax;
    }

    /**
     * {@code Timer::GetMinPeriod} ({@code Timer.h:32-36}): the C ignores {@code InitTC}'s result and
     * returns the field, so a failed query reads back as 0 here too.
     */
    static int timerMinPeriod() {
        synchronized (TIMER_LOCK) {
            boolean ignoredResult = initTimeCaps();
            return periodMin;
        }
    }

    /** {@code Timer::GetMaxPeriod} ({@code Timer.h:38-42}), the same one query. */
    static int timerMaxPeriod() {
        synchronized (TIMER_LOCK) {
            boolean ignoredResult = initTimeCaps();
            return periodMax;
        }
    }

    /**
     * {@code RunnableTimer::Start} ({@code Timer.cpp:39-46}) over {@code Timer::Timer}
     * ({@code Timer.h:46-55}) and {@code Timer::start} ({@code Timer.h:67-75}), returning 0 in exactly
     * the two places the C returned 0 - both of them a C++ throw that {@code Timer.cpp:41-45} caught.
     * <p>
     * The two throw sites are not symmetric, and the difference is reproduced:
     * <ul>
     * <li>{@code InitTC} failing throws from the <em>base</em> constructor body
     *     ({@code Timer.h:49-51}), so {@code ~Timer()} never runs and {@code timersCount} stays
     *     incremented for the life of the process - the next start then sees 2, skips both
     *     {@code InitTC} and {@code timeBeginPeriod}, and passes {@code uResolution == 0}. Hence no
     *     decrement on that path here either. It is unreachable in practice, because 0/0 caps make
     *     {@code Timer.start} throw first ({@code Timer.java:88-90}), and is reproduced only because
     *     it is cheap to reproduce.</li>
     * <li>{@code start()} failing throws from the <em>derived</em> body ({@code Timer.cpp:55-57}), so
     *     the members are destroyed and the base {@code ~Timer()} does run with {@code id == 0},
     *     taking only its {@code --timersCount} branch - which is {@link #releaseOnePeriod()}.</li>
     * </ul>
     * The order is the C's ({@code Timer.h:46-75}, {@code Timer.cpp:53-58}). The registry entry is
     * created before {@code timeSetEvent} - the C++ object was reachable from the callback through
     * {@code dwUser} from that call on, with {@code runnable} already valid - so a tick that arrives
     * while {@code timeSetEvent} is still returning already finds it. The multimedia id is stored the
     * moment {@code timeSetEvent} returns and tested afterwards, as {@code id = ::timeSetEvent(...)}
     * was ({@code Timer.h:71-74}). The id is handed out last, as the pointer was. An entry whose
     * {@code mmTimerId} is still 0 is therefore visible only to a {@link #timerStop} that holds an id
     * this method has not returned yet, which {@code Timer.java} cannot produce: {@code start} and
     * {@code stop} share the {@code Timer}'s monitor, and {@code stop} passes the value {@code start}
     * returned.
     *
     * @param runnable the timer's {@code Runnable}, called on a winmm worker thread
     * @param period the period in milliseconds, passed straight to {@code timeSetEvent} as
     *        {@code uDelay}; the resolution is <em>not</em> the period ({@code Timer.h:71})
     * @return the id to hand back to {@link #timerStop}, or 0 if the timer did not start
     */
    static long timerStart(Runnable runnable, int period) {
        int resolution;
        synchronized (TIMER_LOCK) {
            if (++timersCount == 1) {
                if (!initTimeCaps()) {
                    return 0;
                }
                beginPeriod(timerRes);
            }
            resolution = timerRes;
        }
        long id = NEXT_TIMER_ID.getAndIncrement();
        TimerEntry entry = new TimerEntry(runnable);
        TIMERS.put(id, entry);
        int mmTimerId = setEvent(period, resolution, id);
        entry.mmTimerId = mmTimerId;
        if (mmTimerId == 0) {
            TIMERS.remove(id);
            releaseOnePeriod();
            return 0;
        }
        return id;
    }

    /**
     * {@code RunnableTimer::Stop} ({@code Timer.cpp:48-51}) over {@code ~Timer} ({@code Timer.h:57-65})
     * and in the destructors' own order, which is also the order that makes the callback safe:
     * <ol>
     * <li>remove the registry entry - the stand-in for {@code ~JGlobalRef} -&gt;
     *     {@code DeleteGlobalRef} ({@code Utils.h:374-378}), which the C also ran first because
     *     {@code runnable} is a member of the derived class ({@code Timer.cpp:67}). After it returns,
     *     every later tick reads {@code null} and drops;</li>
     * <li>{@code timeKillEvent} ({@code Timer.h:60}), <em>outside</em> {@link #TIMER_LOCK}: winmm is
     *     free to block here, and the callback path must never be able to wait on a lock this thread
     *     holds;</li>
     * <li>{@link #releaseOnePeriod()} ({@code Timer.h:62-64}).</li>
     * </ol>
     * The residual window is the C's: a tick that read its entry just before step 1 may still be
     * inside {@code run()} when this returns, because {@code TIME_KILL_SYNCHRONOUS} is not set. Nothing
     * is freed in that window - the stub is global and the {@code Runnable} is held by the callback's
     * own local - where the C called {@code CallVoidMethod} on a global ref it had already released.
     * The visible delta is the other way round: a tick the C would in practice still have delivered is
     * dropped here (a stop-then-restart, {@code Timer.java:92-94}, can lose one pulse).
     * <p>
     * An unknown or already-stopped id returns silently, where {@code Timer.cpp:50} would have deleted
     * whatever it was given a second time. {@code Timer.java:129-135} makes that unreachable through
     * the public API, so no caller can tell.
     */
    static void timerStop(long id) {
        TimerEntry entry = TIMERS.remove(id);
        if (entry == null) {
            return;
        }
        int mmTimerId = entry.mmTimerId;
        if (mmTimerId != 0) {
            killEvent(mmTimerId);
        }
        releaseOnePeriod();
    }

    /**
     * The half of {@code ~Timer} that runs on every stop and on a failed {@code timeSetEvent}
     * ({@code Timer.h:62-64}). {@code timeEndPeriod} is skipped when {@code timerRes} is 0, while
     * {@code timeBeginPeriod} is called even then ({@code Timer.h:53}) - an asymmetry reachable only
     * when {@code wPeriodMax} is 0 with a non-zero {@code wPeriodMin}, and kept verbatim.
     */
    private static void releaseOnePeriod() {
        synchronized (TIMER_LOCK) {
            if (--timersCount == 0 && timerRes != 0) {
                endPeriod(timerRes);
            }
        }
    }

    /**
     * The {@code LPTIMECALLBACK} body, {@code Timer::StaticTimeCallback} plus
     * {@code RunnableTimer::TimerCallback} ({@code Timer.h:97-100}, {@code Timer.cpp:60-64}) in one
     * method. {@code uTimerID}, {@code uMsg}, {@code dw1} and {@code dw2} are ignored exactly as the C
     * ignored them; {@code dwUser} is the registry id.
     * <p>
     * <b>This runs on a winmm multimedia-timer worker thread</b> - never the JavaFX application
     * thread - and nothing attaches that thread. This is the whole point of moving the timer: the JNI had to
     * call {@code AttachCurrentThreadAsDaemon} here ({@code Timer.cpp:69-77}, the only thread attach in
     * javafx.graphics) and cache the {@code JNIEnv} forever, while an FFM upcall stub is callable from
     * a thread the JVM has never seen. {@code com.sun.glass.ui.Timer} promises only that "the run()
     * method may be invoked on a thread other than the UI thread" ({@code Timer.java:35-37}), so no
     * test asserts anything about the identity of this thread.
     * <p>
     * It must not throw: an exception escaping an upcall stub terminates the JVM, so {@code Throwable}
     * is caught here and the report itself is wrapped as well - {@code CheckAndClearException}
     * ({@code Utils.cpp:50-71}) swallowed every failure of its own reporting for the same reason. A
     * tick whose entry has already been removed is dropped, where the C called {@code CallVoidMethod}
     * on a released global ref.
     */
    private static void onTimerFired(int uTimerID, int uMsg, long dwUser, long dw1, long dw2) {
        try {
            TimerEntry entry = TIMERS.get(dwUser);
            if (entry != null) {
                entry.runnable.run();
            }
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code CheckAndClearException} (the former {@code Utils.cpp:50-71}): report on this thread, and swallow
     * anything the reporting itself raises. {@link com.sun.glass.ui.Application#reportException} routes
     * to the current thread's uncaught-exception handler ({@code Application.java:448-453}), which is
     * where the C's {@code CallStaticVoidMethod(reportExceptionMID, t)} ended up too - one failure mode
     * fewer, because the {@code FindClass} it needed cannot fail here.
     * <p>
     * Shared by both upcall stubs of this class, the winmm timer callback and
     * {@link #onPreferencesChanged}, because letting anything out of either of them kills the JVM.
     */
    private static void reportUpcallFailure(Throwable t) {
        try {
            Application.reportException(t);
        } catch (Throwable ignored) {
            // Nothing left to try: this is an upcall, and letting anything out of it kills the JVM.
        }
    }

    /** {@code ::timeBeginPeriod(wTimerRes)} ({@code Timer.h:53}); the C ignored the result. */
    private static void beginPeriod(int resolution) {
        try {
            int ignoredResult = (int) Winmm.TIME_BEGIN_PERIOD.invokeExact(resolution);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code ::timeEndPeriod(wTimerRes)} ({@code Timer.h:63}); the C ignored the result. */
    private static void endPeriod(int resolution) {
        try {
            int ignoredResult = (int) Winmm.TIME_END_PERIOD.invokeExact(resolution);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code ::timeSetEvent(period, wTimerRes, StaticTimeCallback, (DWORD_PTR)this, TIME_PERIODIC)}
     * ({@code Timer.h:71-72}), with the Java id where the C passed a pointer.
     *
     * @return the winmm timer id, or 0 if the timer did not start
     */
    private static int setEvent(int period, int resolution, long id) {
        try {
            return (int) Winmm.TIME_SET_EVENT.invokeExact(period, resolution, Winmm.TIME_CALLBACK, id,
                    TIME_PERIODIC);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code ::timeKillEvent(id)} ({@code Timer.h:60}); the C ignored the result. */
    private static void killEvent(int mmTimerId) {
        try {
            int ignoredResult = (int) Winmm.TIME_KILL_EVENT.invokeExact(mmTimerId);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** How many timers this class has started and not stopped; for {@code WinTimerNativeTest}. */
    static int timerRegistrySize() {
        return TIMERS.size();
    }

    /**
     * {@code Timer::timersCount} ({@code Timer.h:102}), the refcount that decides when
     * {@code timeBeginPeriod} and {@code timeEndPeriod} are called; for {@code WinTimerNativeTest},
     * which asserts it returns to its baseline on every path including a failed start.
     */
    static int timerPeriodRefCount() {
        synchronized (TIMER_LOCK) {
            return timersCount;
        }
    }

    /** {@code Timer::wTimerRes} ({@code Timer.h:103}); for {@code WinTimerNativeTest}. */
    static int timerResolution() {
        synchronized (TIMER_LOCK) {
            return timerRes;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Menus (WinMenuImpl; was GlassMenu.cpp's eleven functional JNI bodies, now Java over user32)
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code JString} ({@code Utils.h:108-114}) with no JNI: {@code GetStringLength} code units
     * copied into a {@code wchar_t[len + 1]} with a manual NUL, and <em>no charset conversion at
     * all</em>. {@code String.length()} counts UTF-16 code units exactly as {@code GetStringLength}
     * did, {@code JAVA_CHAR} is the platform's native 16-bit order, and an embedded {@code U+0000}
     * survives the copy the same way it did in C - Windows then truncates the visible title there
     * while {@code cch} still reports the full length.
     * <p>
     * Deliberately not {@code arena.allocateFrom(text, UTF_16LE)}: that runs a charset encoder, which
     * writes U+FFFD for every unpaired surrogate that {@code JString} copied as the unit it was. (It
     * does not reject an embedded {@code U+0000}; it copies it, as this does.)
     *
     * @throws NullPointerException if {@code text} is {@code null}. This is the menus' one behaviour
     *         divergence and it replaces undefined behaviour: {@code JString} did not null-check, so
     *         a {@code null} title reached {@code GetStringLength(NULL)}. {@code com.sun.glass.ui.MenuItem}
     *         and {@code Menu} do accept a {@code null} title, so the path was reachable through the
     *         Glass menu API - which nothing in this repository drives on Windows
     */
    private static MemorySegment wideString(Arena arena, String text) {
        char[] chars = text.toCharArray();
        MemorySegment segment = arena.allocate(JAVA_CHAR, chars.length + 1L);
        MemorySegment.copy(chars, 0, segment, JAVA_CHAR, 0, chars.length);
        segment.setAtIndex(JAVA_CHAR, chars.length, '\0');
        return segment;
    }

    /**
     * Zeroes {@code info} and writes the two fields every use of {@code MENUITEMINFOW} needs. The
     * zeroing covers both C idioms at once: the four sites that {@code memset} first
     * ({@code GlassMenu.cpp:136,274,301,59}) and the two that do not and instead assign all twelve
     * fields ({@code :168-182}, {@code :203-215}). The only difference is the padding at offsets
     * 20-23 and 68-71, which held stack garbage in C and holds zero here; user32 does not read it.
     */
    private static void prepareItemInfo(MemorySegment info, int mask) {
        info.fill((byte) 0);
        info.set(JAVA_INT, MII_CBSIZE_OFFSET, MENUITEMINFOW_SIZE);
        info.set(JAVA_INT, MII_FMASK_OFFSET, mask);
    }

    private static boolean isMenu(MemorySegment hMenu) {
        try {
            return (int) IS_MENU.invokeExact(hMenu) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code GetMenuItemCount}; {@code -1} when {@code hMenu} is not a menu, as in C. */
    private static int itemCount(MemorySegment hMenu) {
        try {
            return (int) GET_MENU_ITEM_COUNT.invokeExact(hMenu);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static boolean getItemInfo(MemorySegment hMenu, int item, boolean byPosition,
                                       MemorySegment info) {
        try {
            return (int) GET_MENU_ITEM_INFO_W.invokeExact(hMenu, item, byPosition ? 1 : 0, info) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static boolean setItemInfo(MemorySegment hMenu, int item, boolean byPosition,
                                       MemorySegment info) {
        try {
            return (int) SET_MENU_ITEM_INFO_W.invokeExact(hMenu, item, byPosition ? 1 : 0, info) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static boolean insertItemInfo(MemorySegment hMenu, int item, boolean byPosition,
                                          MemorySegment info) {
        try {
            return (int) INSERT_MENU_ITEM_W.invokeExact(hMenu, item, byPosition ? 1 : 0, info) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code FindItemBySubmenu} ({@code GlassMenu.cpp:51-70}) transliterated, including the
     * {@code hSubmenu == NULL -> -1} shortcut and the fact that a {@code GetMenuItemCount} of
     * {@code -1} makes the loop empty.
     * <p>
     * The C compared raw handle values ({@code itemInfo.hSubMenu == hSubmenu}, {@code :63}). Read as
     * {@code ADDRESS} the slot is a {@link MemorySegment}, whose {@code equals} is <em>segment</em>
     * equality and is not the same test, so the comparison is written on {@link MemorySegment#address}.
     * For the same reason {@code hSubMenu} must stay a plain {@code ADDRESS} in
     * {@link #MENUITEMINFOW_LAYOUT}: giving it a target layout would bound the segment and change
     * what {@code equals} means without changing this code.
     *
     * @return the position of the item whose submenu is {@code hSubmenu}, or {@code -1}
     */
    private static int findItemBySubmenu(MemorySegment hMenu, long hSubmenu) {
        if (hSubmenu == 0) {
            return -1;
        }
        int count = itemCount(hMenu);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(MENUITEMINFOW_LAYOUT);
            for (int pos = 0; pos < count; pos++) {
                prepareItemInfo(info, MIIM_SUBMENU);
                if (getItemInfo(hMenu, pos, true, info)
                        && info.get(ADDRESS, MII_HSUBMENU_OFFSET).address() == hSubmenu) {
                    return pos;
                }
            }
        }
        return -1;
    }

    /**
     * {@code _create} ({@code GlassMenu.cpp:116-120}): {@code (jlong)::CreateMenu()}.
     * <p>
     * The C called {@code CreateMenu} for submenus too ({@code WinMenuDelegate.createMenu} ->
     * {@code WinMenuImpl.create}), where the Win32 convention is {@code CreatePopupMenu}. That is not
     * corrected here: substituting {@code CreatePopupMenu} is a behaviour change, not a migration.
     *
     * @return the HMENU as an opaque handle value, or 0 on failure - which is exactly what
     *         {@code WinMenuImpl.create()} already tests
     */
    static long menuCreate() {
        try {
            return ((MemorySegment) CREATE_MENU.invokeExact()).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code _destroy} ({@code GlassMenu.cpp:127-152}): detach every child menu, then destroy this
     * one. The walk is backwards ({@code count-1 .. 0}) because {@code RemoveMenu} shifts the
     * positions after it, and it removes <em>only</em> entries that carry a submenu, so a child HMENU
     * outlives its parent and its own {@code WinMenuDelegate} can still destroy it. Plain items are
     * left for {@code DestroyMenu}.
     * <p>
     * The mask is {@code MIIM_SUBMENU | MIIM_DATA} as in C ({@code :138}), even though
     * {@code dwItemData} is read only inside the block comment at {@code :143-147}: this is a
     * migration, and an unused bit of a query mask is not the place to start editing.
     * <p>
     * Silent on a handle that is not a menu, and {@code void} either way - {@code WinMenuImpl.destroy()}
     * clears its {@code ptr} regardless, so a non-menu handle stays silently leaked exactly as before.
     */
    static void menuDestroy(long hMenu) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        if (!isMenu(menu)) {
            return;
        }
        int count = itemCount(menu);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(MENUITEMINFOW_LAYOUT);
            for (int pos = count - 1; pos >= 0; pos--) {
                prepareItemInfo(info, MIIM_SUBMENU | MIIM_DATA);
                if (getItemInfo(menu, pos, true, info)
                        && info.get(ADDRESS, MII_HSUBMENU_OFFSET).address() != 0) {
                    removeMenu(menu, pos, MF_BYPOSITION);
                }
            }
        }
        try {
            int ignoredResult = (int) DESTROY_MENU.invokeExact(menu);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code _insertItem} ({@code GlassMenu.cpp:159-188}). The three arguments the C took and never
     * used - {@code Callback callback}, {@code int shortcut}, {@code int modifiers} - do not cross
     * this boundary; {@code WinMenuImpl.insertItem} still evaluates them, and says why.
     * <p>
     * {@code MFS_GRAYED} here is 3, not the {@code MF_GRAYED} of 1 that {@link #menuEnableItem} uses.
     * Both are what the C wrote.
     *
     * @return {@code false} if {@code hMenu} is not a menu or {@code InsertMenuItemW} fails; no
     *         exception, no log, no {@code GetLastError} - the C read none of those either
     */
    static boolean menuInsertItem(long hMenu, int pos, int cmdID, String title,
                                  boolean enabled, boolean checked) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        if (!isMenu(menu)) {
            return false;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(MENUITEMINFOW_LAYOUT);
            MemorySegment text = wideString(arena, title);
            prepareItemInfo(info, MIIM_FTYPE | MIIM_STATE | MIIM_ID | MIIM_STRING);
            info.set(JAVA_INT, MII_FTYPE_OFFSET, MFT_STRING);
            info.set(JAVA_INT, MII_FSTATE_OFFSET, (enabled ? MFS_ENABLED : MFS_GRAYED)
                    | (checked ? MFS_CHECKED : MFS_UNCHECKED));
            info.set(JAVA_INT, MII_WID_OFFSET, cmdID);
            info.set(ADDRESS, MII_DWTYPEDATA_OFFSET, text);
            info.set(JAVA_INT, MII_CCH_OFFSET, title.length());
            return insertItemInfo(menu, pos, true, info);
        }
    }

    /**
     * {@code _insertSubmenu} ({@code GlassMenu.cpp:195-221}). This is the one body whose guard is a
     * <b>conjunction</b>: {@code ::IsMenu(hMenu) && ::IsMenu(hSubmenu)} ({@code :201}). Checking only
     * the parent would not merely change a return value - HMENU values are recycled, so a stale child
     * handle that Windows has since reissued would be accepted and a foreign menu attached.
     * <p>
     * The parent does not take ownership of the child: {@code menuDestroy} detaches children before
     * {@code DestroyMenu} precisely so each {@code WinMenuDelegate} can still destroy its own.
     */
    static boolean menuInsertSubmenu(long hMenu, int pos, long hSubmenu, String title,
                                     boolean enabled) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        MemorySegment submenu = MemorySegment.ofAddress(hSubmenu);
        if (!isMenu(menu) || !isMenu(submenu)) {
            return false;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(MENUITEMINFOW_LAYOUT);
            MemorySegment text = wideString(arena, title);
            prepareItemInfo(info, MIIM_FTYPE | MIIM_STATE | MIIM_STRING | MIIM_SUBMENU);
            info.set(JAVA_INT, MII_FTYPE_OFFSET, MFT_STRING);
            info.set(JAVA_INT, MII_FSTATE_OFFSET, enabled ? MFS_ENABLED : MFS_GRAYED);
            info.set(ADDRESS, MII_HSUBMENU_OFFSET, submenu);
            info.set(ADDRESS, MII_DWTYPEDATA_OFFSET, text);
            info.set(JAVA_INT, MII_CCH_OFFSET, title.length());
            return insertItemInfo(menu, pos, true, info);
        }
    }

    /**
     * {@code _insertSeparator} ({@code GlassMenu.cpp:228-236}):
     * {@code ::InsertMenu(hMenu, pos, MF_SEPARATOR, NULL, NULL)}.
     * <p>
     * <b>{@code MF_BYPOSITION} is absent</b>, so {@code MF_BYCOMMAND} (0) applies and {@code pos} is
     * interpreted as a <em>command id</em>, not a position - unlike every other insert in this class,
     * which passes {@code fByPosition = TRUE}. That is what the C did and it is reproduced verbatim;
     * {@code WinMenuNativeTest} records what Windows actually does with it.
     */
    static boolean menuInsertSeparator(long hMenu, int pos) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        if (!isMenu(menu)) {
            return false;
        }
        try {
            return (int) INSERT_MENU_W.invokeExact(menu, pos, MF_SEPARATOR, 0L, MemorySegment.NULL) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code _removeAtPos} ({@code GlassMenu.cpp:243-260}); the dead {@code dwItemData} block goes. */
    static boolean menuRemoveAtPos(long hMenu, int pos) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        return isMenu(menu) && removeMenu(menu, pos, MF_BYPOSITION);
    }

    private static boolean removeMenu(MemorySegment hMenu, int position, int flags) {
        try {
            return (int) REMOVE_MENU.invokeExact(hMenu, position, flags) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code _setItemTitle} ({@code GlassMenu.cpp:267-284}), which addresses the item <b>by command
     * id</b> ({@code fByPosition = FALSE}, {@code :279}) - the opposite of
     * {@link #menuSetSubmenuTitle}. Keep them distinct.
     */
    static boolean menuSetItemTitle(long hMenu, int cmdID, String title) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        if (!isMenu(menu)) {
            return false;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(MENUITEMINFOW_LAYOUT);
            MemorySegment text = wideString(arena, title);
            prepareItemInfo(info, MIIM_STRING);
            info.set(ADDRESS, MII_DWTYPEDATA_OFFSET, text);
            info.set(JAVA_INT, MII_CCH_OFFSET, title.length());
            return setItemInfo(menu, cmdID, false, info);
        }
    }

    /**
     * {@code _setSubmenuTitle} ({@code GlassMenu.cpp:291-312}), by <b>position</b>.
     * <p>
     * <b>Latent bug, carried verbatim.</b> The test is {@code pos > 0} ({@code :298}) while
     * {@link #findItemBySubmenu} returns 0 for the first item and -1 for none, so the <em>first</em>
     * submenu of a menu bar - typically "File" - can never be renamed. Correcting it to
     * {@code pos >= 0} would be a behaviour change inside a migration; it needs its own commit, its
     * own test and a deliberate update of the golden transcript.
     */
    static boolean menuSetSubmenuTitle(long hMenu, long hSubmenu, String title) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        if (!isMenu(menu)) {
            return false;
        }
        int pos = findItemBySubmenu(menu, hSubmenu);
        if (pos <= 0) {
            return false;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(MENUITEMINFOW_LAYOUT);
            MemorySegment text = wideString(arena, title);
            prepareItemInfo(info, MIIM_STRING);
            info.set(ADDRESS, MII_DWTYPEDATA_OFFSET, text);
            info.set(JAVA_INT, MII_CCH_OFFSET, title.length());
            return setItemInfo(menu, pos, true, info);
        }
    }

    /**
     * {@code _enableItem} ({@code GlassMenu.cpp:319-330}): {@code 0 <= EnableMenuItem(...)}.
     * {@code EnableMenuItem} is declared {@code BOOL} (signed), so its {@code -1} "no such item"
     * really does come back as failure - which is <em>not</em> true of {@link #menuCheckItem}, and
     * the asymmetry is the C's, faithfully kept.
     */
    static boolean menuEnableItem(long hMenu, int cmdID, boolean enable) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        return isMenu(menu)
                && enableMenuItem(menu, cmdID, MF_BYCOMMAND | (enable ? MF_ENABLED : MF_GRAYED)) >= 0;
    }

    /**
     * {@code _enableSubmenu} ({@code GlassMenu.cpp:337-352}). Carries the same {@code pos > 0} bug as
     * {@link #menuSetSubmenuTitle}: the first submenu cannot be enabled or disabled.
     */
    static boolean menuEnableSubmenu(long hMenu, long hSubmenu, boolean enable) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        if (!isMenu(menu)) {
            return false;
        }
        int pos = findItemBySubmenu(menu, hSubmenu);
        return pos > 0
                && enableMenuItem(menu, pos, MF_BYPOSITION | (enable ? MF_ENABLED : MF_GRAYED)) >= 0;
    }

    private static int enableMenuItem(MemorySegment hMenu, int item, int flags) {
        try {
            return (int) ENABLE_MENU_ITEM.invokeExact(hMenu, item, flags);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code _checkItem} ({@code GlassMenu.cpp:359-370}).
     * <p>
     * <b>Latent bug, carried verbatim.</b> The C wrote {@code 0 <= ::CheckMenuItem(...)}, but
     * {@code CheckMenuItem} returns {@code DWORD}: the comparison is unsigned and therefore always
     * true, so the method reported success even for a command id no item has, where
     * {@code CheckMenuItem} returns {@code (DWORD)-1}. Reading the {@code JAVA_INT} result as
     * {@code >= 0} would turn that into {@code false} - a silent bug fix inside a migration. So the
     * result is called for, and discarded.
     *
     * @return {@code true} whenever {@code hMenu} is a menu, whatever {@code CheckMenuItem} answered
     */
    static boolean menuCheckItem(long hMenu, int cmdID, boolean check) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        if (!isMenu(menu)) {
            return false;
        }
        try {
            int unsignedResultTheCNeverChecked = (int) CHECK_MENU_ITEM.invokeExact(
                    menu, cmdID, MF_BYCOMMAND | (check ? MF_CHECKED : MF_UNCHECKED));
        } catch (Throwable t) {
            throw unexpected(t);
        }
        return true;
    }

    /* -- read-back, for WinMenuNativeTest's parity transcript; no product code calls these -------- */

    /** {@code IsMenu}; the test needs it to prove a child HMENU outlived its destroyed parent. */
    static boolean menuIsMenu(long hMenu) {
        return isMenu(MemorySegment.ofAddress(hMenu));
    }

    /** {@code GetMenuItemCount}; {@code -1} when the handle is not a menu. */
    static int menuItemCount(long hMenu) {
        return itemCount(MemorySegment.ofAddress(hMenu));
    }

    /**
     * One item's {@code MENUITEMINFOW} scalars, read with {@code MIIM_FTYPE | MIIM_STATE | MIIM_ID}.
     *
     * @return {@code {fType, fState, wID}}, or {@code null} when the item cannot be read
     */
    static int[] menuItemFlags(long hMenu, int pos) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(MENUITEMINFOW_LAYOUT);
            prepareItemInfo(info, MIIM_FTYPE | MIIM_STATE | MIIM_ID);
            if (!getItemInfo(menu, pos, true, info)) {
                return null;
            }
            return new int[] {
                info.get(JAVA_INT, MII_FTYPE_OFFSET),
                info.get(JAVA_INT, MII_FSTATE_OFFSET),
                info.get(JAVA_INT, MII_WID_OFFSET)};
        }
    }

    /** The item's {@code hSubMenu} as a handle value; 0 when it has none or cannot be read. */
    static long menuItemSubmenu(long hMenu, int pos) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(MENUITEMINFOW_LAYOUT);
            prepareItemInfo(info, MIIM_SUBMENU);
            if (!getItemInfo(menu, pos, true, info)) {
                return 0;
            }
            return info.get(ADDRESS, MII_HSUBMENU_OFFSET).address();
        }
    }

    /**
     * The item's title, read back as UTF-16 through the two-step {@code MIIM_STRING} protocol: ask
     * once with {@code dwTypeData == NULL} to learn {@code cch}, then again into a buffer of
     * {@code cch + 1} code units. {@code cch} comes back as the number of units actually copied.
     *
     * @return the title, or {@code null} when the item cannot be read (a separator answers {@code ""})
     */
    static String menuItemTitle(long hMenu, int pos) {
        MemorySegment menu = MemorySegment.ofAddress(hMenu);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(MENUITEMINFOW_LAYOUT);
            prepareItemInfo(info, MIIM_STRING);
            if (!getItemInfo(menu, pos, true, info)) {
                return null;
            }
            int length = info.get(JAVA_INT, MII_CCH_OFFSET);
            if (length <= 0) {
                return "";
            }
            MemorySegment buffer = arena.allocate(JAVA_CHAR, length + 1L);
            prepareItemInfo(info, MIIM_STRING);
            info.set(ADDRESS, MII_DWTYPEDATA_OFFSET, buffer);
            info.set(JAVA_INT, MII_CCH_OFFSET, length + 1);
            if (!getItemInfo(menu, pos, true, info)) {
                return null;
            }
            int copied = Math.min(info.get(JAVA_INT, MII_CCH_OFFSET), length);
            char[] chars = new char[copied];
            MemorySegment.copy(buffer, JAVA_CHAR, 0, chars, 0, copied);
            return new String(chars);
        }
    }

    /* -- the one upcall: WM_COMMAND (was GlassMenu.cpp's CallStaticBooleanMethod, since deleted) ---------- */

    /** {@code int32_t (*notify_command)(void* user, int32_t cmd_id)}. */
    private static final FunctionDescriptor NOTIFY_COMMAND_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT);

    private static boolean menuCallbackInstalled;

    /** {@code sizeof(GwinMenuCallbacks)}; {@link #GWIN_MENU_CALLBACKS_LAYOUT} must agree. */
    static int sizeOfMenuCallbacks() {
        try {
            return (int) GWIN_SIZEOF_MENU_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Hands {@code glass.dll} the callback that replaces {@code WinMenuImpl._initIDs}. There is no JNI
     * path behind it any more: {@code HandleMenuCommand} ({@code GlassMenu.cpp}) dials
     * {@code g_menuCallbacks.notify_command} when a table is installed and answers "not handled"
     * when none is.
     * <p>
     * <b>Call this exactly once, from {@code WinMenuImpl}'s static initializer</b> - the line
     * {@code _initIDs()} occupied. That preserves the one behaviour the {@code _initIDs} scheme had
     * that a table does not automatically reproduce: until {@code WinMenuImpl} is touched at all,
     * there is no target, and every {@code WM_COMMAND} is silently "not handled" and goes to
     * {@code DefWindowProc}. Installing it earlier - from this class's initializer, say - would be a
     * behaviour change.
     * <p>
     * The table is copied by the library, so the confined arena that holds it is closed here; the stub
     * inside it is in {@link Arena#global()} for the reason the other two are, which the header states
     * outright: there is no point at which the library can promise no further {@code WM_COMMAND}, and
     * {@code gwin_menu_set_callbacks(NULL, NULL)} would only stop the library dialling the stub, not
     * make it unreachable.
     */
    static synchronized void installMenuCallback() {
        if (menuCallbackInstalled) {
            return;
        }
        MemorySegment callback = upcallStub(upcallTarget("onNotifyCommand", NOTIFY_COMMAND_FD),
                NOTIFY_COMMAND_FD, Arena.global());
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(GWIN_MENU_CALLBACKS_LAYOUT);
            table.set(ADDRESS, NOTIFY_COMMAND_OFFSET, callback);
            int status = (int) GWIN_MENU_SET_CALLBACKS.invokeExact(table, MemorySegment.NULL);
            requireOk(status, "gwin_menu_set_callbacks");
        } catch (Throwable t) {
            throw unexpected(t);
        }
        menuCallbackInstalled = true;
    }

    /** Whether {@link #installMenuCallback} has run in this JVM. */
    static synchronized boolean menuCallbackInstalled() {
        return menuCallbackInstalled;
    }

    /**
     * The {@code GwinMenuCallbacks.notify_command} body: a menu command was chosen. {@code user} is
     * the pointer {@link #installMenuCallback} passed, which is {@code NULL} - the map from command id
     * to peer is {@code WinMenuItemDelegate.CommandIDManager}, and always was.
     * <p>
     * <b>Thread.</b> The Glass toolkit thread, always: {@code DispatchMessage} ->
     * {@code BaseWnd::StaticWindowProc} -> {@code GlassWindow::WindowProc}'s {@code WM_COMMAND} arm ->
     * {@code GlassWindow::HandleCommand} -> {@code HandleMenuCommand}. An inter-thread
     * {@code SendMessage} still runs the window procedure on the window's owning thread, and the only
     * {@code AttachCurrentThread} this library ever had was {@code Timer.cpp}'s, which is
     * deleted. Frequently nested inside the system's modal menu loop.
     * <p>
     * <b>Re-entrant.</b> {@code MenuItem.Callback.action()} is arbitrary application code: it may open
     * a dialog or nest a message loop. Nothing here holds a lock; the C held none either.
     * <p>
     * <b>The return value is load-bearing, and the contract is "non-zero means handled".</b>
     * {@code GlassWindow::WindowProc} answers {@code return 0} for a handled command and falls through
     * to {@code DefWindowProc} for an unhandled one ({@code GlassWindow.cpp:461-465}), so answering
     * wrongly hands a command the application consumed to the default window procedure, or hides one
     * it did not. The C tests {@code != 0}; nothing on either side may narrow that to {@code == 1}.
     * <p>
     * <b>A throwing target counts as unhandled.</b> {@code CallStaticBooleanMethod} had already
     * yielded {@code JNI_FALSE} by the time {@code CheckAndClearException} reported and swallowed the
     * throwable ({@code Utils.cpp:50-71}), so 0 here is parity and not a choice. Letting anything out
     * of an upcall stub would terminate the JVM.
     *
     * @param cmdId {@code LOWORD(wParam)} of {@code WM_COMMAND}, i.e. the {@code MENUITEMINFOW.wID}
     *              Java put there; {@code CommandIDManager} hands out 1..0xFFFF, so it is never
     *              negative and never truncated by the {@code WORD} it travelled in
     * @return 1 if a peer with a callback consumed it, 0 otherwise
     */
    static int onNotifyCommand(MemorySegment user, int cmdId) {
        try {
            return WinMenuImpl.notifyCommand(cmdId) ? 1 : 0;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return 0;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * View: the eleven gwin_view_* entry points of GlassView.cpp, and the two callback tables that
     * carry its 26 upcall sites - GwinViewCallbacks (View.notify*, View.getAccessible) and
     * GwinGestureCallbacks (WinGestureSupport's five statics) - keyed by the int64 id WinView assigns
     * ------------------------------------------------------------------------------------------- */

    /*
     * The fifteen upcall descriptors; the first JAVA_LONG of each is the view id. Package-private,
     * not private, on purpose: WinGlassNativeShim builds its recording stubs from these same objects,
     * so the arity test in WinViewNativeTest proves THESE descriptors against gwin_test_fire_callback's
     * patterns and not a copy of them. The hot slots - notify_mouse (per WM_MOUSEMOVE),
     * notify_next_touch_event (per touch point per WM_TOUCH), gesture_performed (per manipulation
     * delta) and the per-frame notify_view / notify_resize / notify_repaint / notify_scroll - carry
     * primitives only, so no MemorySegment can be materialised per event; the four that carry a
     * pointer are per keystroke (notify_key) or cold (the IME pair, and get_accessible's return).
     */
    static final FunctionDescriptor NOTIFY_VIEW_FD = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT);
    static final FunctionDescriptor NOTIFY_RESIZE_FD = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_REPAINT_FD =
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_MENU_FD =
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_KEY_FD =
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_MOUSE_FD = FunctionDescriptor.ofVoid(JAVA_LONG,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_SCROLL_FD = FunctionDescriptor.ofVoid(JAVA_LONG,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_INT,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    static final FunctionDescriptor NOTIFY_INPUT_METHOD_FD = FunctionDescriptor.ofVoid(JAVA_LONG,
            ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_IME_CANDIDATE_POS_REQUEST_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS);
    static final FunctionDescriptor GET_ACCESSIBLE_FD = FunctionDescriptor.of(JAVA_LONG, JAVA_LONG);
    /**
     * SEVEN {@code int32_t} then SEVEN {@code float}, counted against
     * {@code ViewContainer::NotifyGesturePerformed}: one integer too many shifts every float by an
     * 8-byte stack slot on x64 and a pinch delivers the bit pattern of {@code y_abs} as
     * {@code delta_x}, with no crash and no exception. {@code gwin_test_fire_callback(100, ...)} and
     * {@code WinViewNativeTest}'s recording table exist for exactly this descriptor.
     */
    static final FunctionDescriptor GESTURE_PERFORMED_FD = FunctionDescriptor.ofVoid(JAVA_LONG,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
            JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT);
    static final FunctionDescriptor INERTIA_GESTURE_FINISHED_FD = FunctionDescriptor.ofVoid(JAVA_LONG);
    static final FunctionDescriptor NOTIFY_BEGIN_TOUCH_EVENT_FD =
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_NEXT_TOUCH_EVENT_FD =
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor NOTIFY_END_TOUCH_EVENT_FD = FunctionDescriptor.ofVoid(JAVA_LONG);

    /**
     * The slot names of {@link #GWIN_VIEW_CALLBACKS_LAYOUT} in declaration order, which is the order
     * the installer writes them, the order of {@link #VIEW_SLOT_TARGETS}, and the numbering
     * {@code gwin_test_fire_callback} uses (0..9).
     */
    static final List<String> VIEW_SLOT_NAMES = List.of("notify_view", "notify_resize", "notify_repaint",
            "notify_menu", "notify_key", "notify_mouse", "notify_scroll", "notify_input_method",
            "notify_ime_candidate_pos_request", "get_accessible");

    /** The slot names of {@link #GWIN_GESTURE_CALLBACKS_LAYOUT}; the hook numbers them 100..104. */
    static final List<String> GESTURE_SLOT_NAMES = List.of("gesture_performed", "inertia_gesture_finished",
            "notify_begin_touch_event", "notify_next_touch_event", "notify_end_touch_event");

    /** The target methods of this class, parallel to {@link #VIEW_SLOT_NAMES}. */
    private static final String[] VIEW_SLOT_TARGETS = {"onNotifyView", "onNotifyResize", "onNotifyRepaint",
            "onNotifyMenu", "onNotifyKey", "onNotifyMouse", "onNotifyScroll", "onNotifyInputMethod",
            "onNotifyImeCandidatePosRequest", "onGetAccessible"};
    private static final FunctionDescriptor[] VIEW_SLOT_DESCRIPTORS = {NOTIFY_VIEW_FD, NOTIFY_RESIZE_FD,
            NOTIFY_REPAINT_FD, NOTIFY_MENU_FD, NOTIFY_KEY_FD, NOTIFY_MOUSE_FD, NOTIFY_SCROLL_FD,
            NOTIFY_INPUT_METHOD_FD, NOTIFY_IME_CANDIDATE_POS_REQUEST_FD, GET_ACCESSIBLE_FD};

    /** The target methods of this class, parallel to {@link #GESTURE_SLOT_NAMES}. */
    private static final String[] GESTURE_SLOT_TARGETS = {"onGesturePerformed", "onInertiaGestureFinished",
            "onNotifyBeginTouchEvent", "onNotifyNextTouchEvent", "onNotifyEndTouchEvent"};
    private static final FunctionDescriptor[] GESTURE_SLOT_DESCRIPTORS = {GESTURE_PERFORMED_FD,
            INERTIA_GESTURE_FINISHED_FD, NOTIFY_BEGIN_TOUCH_EVENT_FD, NOTIFY_NEXT_TOUCH_EVENT_FD,
            NOTIFY_END_TOUCH_EVENT_FD};

    private static boolean viewCallbacksInstalled;

    /**
     * The ten and the five stubs, created once by {@link #installViewCallbacks} in
     * {@link Arena#global()} and kept so that {@link #reinstallViewCallbacks} can write the same
     * addresses again. Never replaced: a stub the library has been given must outlive the process.
     */
    private static MemorySegment[] viewCallbackStubs;
    private static MemorySegment[] gestureCallbackStubs;

    /** {@code sizeof(GwinViewCallbacks)} as the C compiler saw it; {@link #GWIN_VIEW_CALLBACKS_LAYOUT} must agree. */
    static int sizeOfViewCallbacks() {
        try {
            return (int) GWIN_SIZEOF_VIEW_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code sizeof(GwinGestureCallbacks)}; {@link #GWIN_GESTURE_CALLBACKS_LAYOUT} must agree. */
    static int sizeOfGestureCallbacks() {
        try {
            return (int) GWIN_SIZEOF_GESTURE_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Hands {@code glass.dll} the two tables that replaced the 26 {@code Call*Method} sites of
     * {@code GlassView.cpp}, {@code ViewContainer.cpp} and {@code FullScreenWindow.cpp}. The install
     * was the flip: before it every one of those sites took its JNI path. Those paths are deleted, so
     * a site that finds no table installed now delivers nothing ({@code ViewCallbacks()} /
     * {@code GestureCallbacks()} answer {@code NULL}), and the install is what makes view events
     * arrive at all.
     * <p>
     * <b>Call this exactly once, from {@code WinApplication}'s static initializer</b>, beside the
     * preferences and application installs and for the same reason: the launcher thread, after the
     * library is loaded and before {@code Application.run()} creates the toolkit window, so the tables
     * exist before the first {@code WndProc} message and are never written from a second thread. A
     * lazy install - from {@code WinView}'s initializer, say - would leave a window in which a touch
     * event arrives before the table exists. What this does <em>not</em> do is initialise
     * {@code WinGestureSupport}: its static initializer must run on the toolkit thread
     * ({@code TouchInputSupport}'s constructor checks it), which the launcher thread is not, so
     * {@code WinApplication.runLoop} does that on the thread it creates.
     * <p>
     * <b>No {@code void* user}, unlike the other three tables.</b> The identity here is per view, not
     * per table - every slot's first parameter is the {@code int64_t} id {@code WinView} registered
     * itself under - so a table-level context would always be {@code NULL} (the three existing
     * installers are in fact passed {@code NULL}), and an {@code ADDRESS} parameter on an upcall stub
     * materialises a {@code MemorySegment} per invocation, which the per-mouse-move and per-touch-point
     * slots cannot afford. A parameter that is always {@code NULL} is worse than no parameter.
     * <p>
     * A slot left {@code NULL} would be replaced by an internal no-op in C; none is, all fifteen are
     * written. The tables are copied by the library, so the confined arena that holds them is closed
     * here; the stubs are in {@link Arena#global()}, because there is no point at which the library
     * can promise no further {@code WndProc} message.
     */
    static synchronized void installViewCallbacks() {
        if (viewCallbacksInstalled) {
            return;
        }
        viewCallbackStubs = stubsFor(VIEW_SLOT_TARGETS, VIEW_SLOT_DESCRIPTORS);
        gestureCallbackStubs = stubsFor(GESTURE_SLOT_TARGETS, GESTURE_SLOT_DESCRIPTORS);
        writeViewTables();
        viewCallbacksInstalled = true;
    }

    /** Whether {@link #installViewCallbacks} has run in this JVM. */
    static synchronized boolean viewCallbacksInstalled() {
        return viewCallbacksInstalled;
    }

    /**
     * Writes the production tables again, with the stubs {@link #installViewCallbacks} created. The
     * one caller is {@code WinGlassNativeShim}, after it has installed a recording table to drive
     * {@code gwin_test_fire_callback} through the facade's descriptors: {@code gwin_view_set_callbacks}
     * copies by value and the last call wins, so this is the restore. Installs first if nothing has
     * been installed yet; never creates a second set of stubs.
     */
    static synchronized void reinstallViewCallbacks() {
        if (!viewCallbacksInstalled) {
            installViewCallbacks();
            return;
        }
        writeViewTables();
    }

    /** The fifteen installed stubs, view slots then gesture slots, for the test that reads them back. */
    static synchronized List<MemorySegment> installedViewCallbackStubs() {
        List<MemorySegment> stubs = new ArrayList<>(15);
        if (viewCallbacksInstalled) {
            Collections.addAll(stubs, viewCallbackStubs);
            Collections.addAll(stubs, gestureCallbackStubs);
        }
        return Collections.unmodifiableList(stubs);
    }

    private static MemorySegment[] stubsFor(String[] targets, FunctionDescriptor[] descriptors) {
        MemorySegment[] stubs = new MemorySegment[targets.length];
        for (int i = 0; i < targets.length; i++) {
            stubs[i] = upcallStub(upcallTarget(targets[i], descriptors[i]), descriptors[i], Arena.global());
        }
        return stubs;
    }

    private static void writeViewTables() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment viewTable = arena.allocate(GWIN_VIEW_CALLBACKS_LAYOUT);
            fillCallbackTable(viewTable, GWIN_VIEW_CALLBACKS_LAYOUT, VIEW_SLOT_NAMES, viewCallbackStubs);
            requireOk((int) GWIN_VIEW_SET_CALLBACKS.invokeExact(viewTable), "gwin_view_set_callbacks");
            MemorySegment gestureTable = arena.allocate(GWIN_GESTURE_CALLBACKS_LAYOUT);
            fillCallbackTable(gestureTable, GWIN_GESTURE_CALLBACKS_LAYOUT, GESTURE_SLOT_NAMES,
                    gestureCallbackStubs);
            requireOk((int) GWIN_GESTURE_SET_CALLBACKS.invokeExact(gestureTable), "gwin_gesture_set_callbacks");
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Writes {@code stubs[i]} at the offset of {@code names.get(i)} in {@code layout}, read through
     * {@link PathElement} and never a literal. Shared with {@code WinGlassNativeShim}'s recording table.
     */
    static void fillCallbackTable(MemorySegment table, StructLayout layout, List<String> names,
                                  MemorySegment[] stubs) {
        for (int i = 0; i < stubs.length; i++) {
            table.set(ADDRESS, layout.byteOffset(PathElement.groupElement(names.get(i))), stubs[i]);
        }
    }

    /**
     * {@code gwin_test_fire_callback}: drives slot {@code slot} of the installed table with the
     * header's fixed pattern and returns what the slot returned - 0 for a void slot,
     * {@link #GWIN_ERR_INVALID_ARG} for an unknown slot or an uninstalled table. A test hook, and the
     * only automated check of the fifteen descriptors: the {@code sizeof} probes cannot see a
     * parameter list, a table stores only pointers, and neither touch hardware nor an IME exists in a
     * build. The return is {@code long} because slot 9 returns an {@code int64_t}.
     */
    static long testFireCallback(int slot, long viewId) {
        try {
            return (long) GWIN_TEST_FIRE_CALLBACK.invokeExact(slot, viewId);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /*
     * The fifteen targets. Each does the marshalling its slot needs and nothing else - the registry
     * lookup and the virtual call are WinView's - and each catches Throwable, because letting anything
     * out of an upcall stub terminates the JVM. Reporting through reportUpcallFailure is
     * CheckAndClearException (the former Utils.cpp:50-71): report on this thread, swallow, carry on. Thread:
     * always the JavaFX application thread, inside DispatchMessage or inside a gwin_view_* downcall
     * that ExecAction marshalled there; re-entrant, and nested in the downcall for set_parent,
     * enter_fullscreen and exit_fullscreen. Nothing here takes a lock (R8: View.uploadPixels already
     * holds View.lock() around gwin_view_upload_pixels, and a notify_view can arrive inside it).
     * Booleans arrive as int32_t 0 / 1 and are tested with != 0, never == 1. Package-private so that
     * WinGlassNativeShim can drive every arm without a table.
     */

    /** {@code notify_view}: {@code View.notifyView(int)} with a {@code ViewEvent} constant. Per frame during a move. */
    static void onNotifyView(long viewId, int type) {
        try {
            WinView.dispatchView(viewId, type);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_resize}: {@code View.notifyResize(int,int)}, through {@code WinView}'s override. */
    static void onNotifyResize(long viewId, int width, int height) {
        try {
            WinView.dispatchResize(viewId, width, height);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_repaint}: {@code View.notifyRepaint(int,int,int,int)}, the {@code GetUpdateRect} rect. */
    static void onNotifyRepaint(long viewId, int x, int y, int width, int height) {
        try {
            WinView.dispatchRepaint(viewId, x, y, width, height);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_menu}: {@code View.notifyMenu(int,int,int,int,boolean)}, through {@code WinView}'s override. */
    static void onNotifyMenu(long viewId, int x, int y, int xAbs, int yAbs, int isKeyboardTrigger) {
        try {
            WinView.dispatchMenu(viewId, x, y, xAbs, yAbs, isKeyboardTrigger != 0);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code notify_key}: {@code View.notifyKey(int,int,char[],int)}. {@code keyChars} points at
     * {@code keyCharCount} UTF-16 code units borrowed for the call; a count of 0 yields an empty
     * array, never {@code null}, because the JNI passed an allocated empty {@code jcharArray}
     * ({@code ViewContainer.cpp}, the {@code NewCharArray(keyCharCount)} before the PRESS / RELEASE
     * sites). One {@code char[]} per keystroke, as the JNI allocated.
     */
    static void onNotifyKey(long viewId, int type, int keyCode, MemorySegment keyChars, int keyCharCount,
                            int modifiers) {
        try {
            WinView.dispatchKey(viewId, type, keyCode, utf16(keyChars, keyCharCount), modifiers);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code notify_mouse}: {@code View.notifyMouse(int,int,int,int,int,int,int,boolean,boolean)}.
     * <b>Hot</b> - one per {@code WM_MOUSEMOVE} that moved, up to the mouse report rate - and so
     * scalars only: nothing here allocates, boxes or builds varargs. The one allocation that can
     * happen is the {@code Long} the registry lookup boxes, which {@code Long.valueOf} caches for the
     * first 127 ids; see {@code WinView.dispatchMouse}.
     */
    static void onNotifyMouse(long viewId, int type, int button, int x, int y, int xAbs, int yAbs,
                              int modifiers, int isPopupTrigger, int isSynthesized) {
        try {
            WinView.dispatchMouse(viewId, type, button, x, y, xAbs, yAbs, modifiers, isPopupTrigger != 0,
                    isSynthesized != 0);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_scroll}: {@code View.notifyScroll(...)}; per wheel notch, scalars only. */
    static void onNotifyScroll(long viewId, int x, int y, int xAbs, int yAbs, double deltaX, double deltaY,
                               int modifiers, int lines, int chars, int defaultLines, int defaultChars,
                               double xMultiplier, double yMultiplier) {
        try {
            WinView.dispatchScroll(viewId, x, y, xAbs, yAbs, deltaX, deltaY, modifiers, lines, chars,
                    defaultLines, defaultChars, xMultiplier, yMultiplier);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code notify_input_method}: {@code View.notifyInputMethod(String,int[],int[],byte[],int,int,int)}.
     * All four buffers are borrowed for the call and copied here. {@code text} is UTF-16, not
     * NUL-terminated, {@code textLen} code units long; {@code NULL} is the
     * {@code WM_IME_ENDCOMPOSITION} reset and also a failed result / composition join, and becomes
     * {@code null}. <b>Null-ness comes from the pointer, never from the count</b>: the C passes
     * {@code clauseBoundary} only when the JNI would have built the {@code jintArray}, and
     * {@code attrBoundary} / {@code attrValue} both or neither, but an individual {@code NewIntArray}
     * could fail and leave one of a pair {@code null}, so each pointer is tested on its own and the
     * count of a {@code NULL} buffer is ignored. {@code clauseBoundary} holds {@code clauseCount + 1}
     * ints, {@code attrBoundary} {@code attrCount + 1}, {@code attrValue} {@code attrCount} bytes
     * ({@code ViewContainer.cpp}'s {@code NewIntArray(cClause+1)}, {@code NewIntArray(cAttrBlock+1)},
     * {@code NewByteArray(cAttrBlock)}). Per composition step; allocates what the JNI allocated.
     */
    static void onNotifyInputMethod(long viewId, MemorySegment text, int textLen, MemorySegment clauseBoundary,
                                    int clauseCount, MemorySegment attrBoundary, MemorySegment attrValue,
                                    int attrCount, int committedTextLength, int caretPos, int visiblePos) {
        try {
            String composed = text.equals(MemorySegment.NULL) ? null : new String(utf16(text, textLen));
            int[] clauses = clauseBoundary.equals(MemorySegment.NULL) ? null : int32s(clauseBoundary, clauseCount + 1);
            int[] attrBounds = attrBoundary.equals(MemorySegment.NULL) ? null : int32s(attrBoundary, attrCount + 1);
            byte[] attrs = attrValue.equals(MemorySegment.NULL) ? null : bytes(attrValue, attrCount);
            WinView.dispatchInputMethod(viewId, composed, clauses, attrBounds, attrs, committedTextLength,
                    caretPos, visiblePos);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code notify_ime_candidate_pos_request}: {@code View.notifyInputMethodCandidatePosRequest(int)}.
     * <b>Query.</b> Writes {@code outXy[0] = x}, {@code outXy[1] = y} and returns 1, or touches
     * nothing and returns 0 - and then {@code GetCandidatePos} keeps its own {@code (0, 0)}. Both
     * elements are read before either is written, so a short array writes neither. On a throw the
     * segment is untouched and the answer is 0; the JNI had no such arm at all - {@code CallObjectMethod}
     * yields {@code NULL} with an exception pending and {@code GetDoubleArrayElements(NULL, NULL)}
     * followed with no null check ({@code ViewContainer.cpp GetCandidatePos}), reachable only because
     * {@code View.java} substitutes {@code {0, 0}} - so 0-and-untouched is the header's contract, not a
     * reproduction. Once per {@code IMN_OPENCANDIDATE} / {@code IMN_CHANGECANDIDATE}; cold.
     */
    static int onNotifyImeCandidatePosRequest(long viewId, int offset, MemorySegment outXy) {
        try {
            double[] pos = WinView.dispatchInputMethodCandidatePosRequest(viewId, offset);
            if (pos == null) {
                return 0;
            }
            double x = pos[0];
            double y = pos[1];
            MemorySegment out = bounded(outXy, 2 * JAVA_DOUBLE.byteSize());
            out.setAtIndex(JAVA_DOUBLE, 0, x);
            out.setAtIndex(JAVA_DOUBLE, 1, y);
            return 1;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return 0;
        }
    }

    /**
     * {@code get_accessible}: {@code View.getAccessible()}, the {@code IRawElementProviderSimple*} the
     * Java {@code Accessible} owns, or 0. <b>The return is load-bearing</b>: for
     * {@code UiaRootObjectId} a non-zero value goes to {@code UiaReturnRawElementProvider} and its
     * {@code LRESULT} is the {@code WM_GETOBJECT} reply ({@code GlassWindow.cpp}'s
     * {@code if (lr) return lr;}); 0 falls through to {@code DefWindowProc}, and a wrong 0 silently
     * disables accessibility for the window. A throw answers 0, which is what the JNI answered:
     * {@code CallLongMethod} returns 0 with an exception pending and {@code CheckAndClearException}
     * then reported it ({@code ViewContainer.cpp}, the {@code pProvider} read of {@code WM_GETOBJECT}).
     * Two sites, cold.
     */
    static long onGetAccessible(long viewId) {
        try {
            return WinView.dispatchGetAccessible(viewId);
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return 0L;
        }
    }

    /**
     * {@code gesture_performed}: {@code WinGestureSupport.gesturePerformed(View, ...)}, seven ints and
     * seven floats after the id. <b>Hot</b> - one per manipulation delta - and scalars only. The view
     * is the registry's answer, {@code null} for id 0 (a container with no {@code GlassView}
     * attached, which {@code HandleViewTouchEvent} produces) and for an id that is gone; the JNI
     * passed a {@code null} {@code View} in the first case too.
     */
    static void onGesturePerformed(long viewId, int modifiers, int isDirect, int isInertia, int x, int y,
                                   int xAbs, int yAbs, float deltaX, float deltaY, float totalDeltaX,
                                   float totalDeltaY, float totalScale, float totalExpansion,
                                   float totalRotation) {
        try {
            WinGestureSupport.gesturePerformed(WinView.viewFor(viewId), modifiers, isDirect != 0,
                    isInertia != 0, x, y, xAbs, yAbs, deltaX, deltaY, totalDeltaX, totalDeltaY, totalScale,
                    totalExpansion, totalRotation);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code inertia_gesture_finished}: {@code WinGestureSupport.inertiaGestureFinished(View)}; per timer tick. */
    static void onInertiaGestureFinished(long viewId) {
        try {
            WinGestureSupport.inertiaGestureFinished(WinView.viewFor(viewId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_begin_touch_event}: {@code WinGestureSupport.notifyBeginTouchEvent(View,int,boolean,int)}. */
    static void onNotifyBeginTouchEvent(long viewId, int modifiers, int isDirect, int touchEventCount) {
        try {
            WinGestureSupport.notifyBeginTouchEvent(WinView.viewFor(viewId), modifiers, isDirect != 0,
                    touchEventCount);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code notify_next_touch_event}: {@code WinGestureSupport.notifyNextTouchEvent(View,int,long,int,int,int,int)}.
     * <b>Hot</b> - once per touch point per {@code WM_TOUCH} - and scalars only; {@code id} is the
     * {@code TOUCHINPUT.dwID} widened to 64 bits.
     */
    static void onNotifyNextTouchEvent(long viewId, int state, long id, int x, int y, int xAbs, int yAbs) {
        try {
            WinGestureSupport.notifyNextTouchEvent(WinView.viewFor(viewId), state, id, x, y, xAbs, yAbs);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_end_touch_event}: {@code WinGestureSupport.notifyEndTouchEvent(View)}. */
    static void onNotifyEndTouchEvent(long viewId) {
        try {
            WinGestureSupport.notifyEndTouchEvent(WinView.viewFor(viewId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * Bounds a borrowed C pointer to {@code byteSize} bytes - the {@code JPEGNative.bounded} pattern.
     * {@link MemorySegment#reinterpret} is the restricted call, so it lives in this one method;
     * {@code withTargetLayout} is not used for upcall parameters anywhere in this class.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment bounded(MemorySegment pointer, long byteSize) {
        return pointer.reinterpret(byteSize);
    }

    /**
     * {@code count} UTF-16 code units at {@code pointer}, copied. A count of 0 - or a {@code NULL}
     * pointer, which the C only ever pairs with 0 - is an empty array, never {@code null}.
     */
    private static char[] utf16(MemorySegment pointer, int count) {
        if (count <= 0 || pointer.equals(MemorySegment.NULL)) {
            return new char[0];
        }
        return bounded(pointer, count * JAVA_CHAR.byteSize()).toArray(JAVA_CHAR);
    }

    private static int[] int32s(MemorySegment pointer, int count) {
        return bounded(pointer, count * JAVA_INT.byteSize()).toArray(JAVA_INT);
    }

    private static byte[] bytes(MemorySegment pointer, int count) {
        return bounded(pointer, count).toArray(JAVA_BYTE);
    }

    /* -- the twelve downcalls, one per former JNI body of GlassView.cpp ------------------------------- */

    /**
     * {@code gwin_view_create}: {@code new GlassView(viewId)}, all that the former JNI {@code WinView._create} did.
     * No OS call and no thread marshal, so it works without a toolkit. The {@code viewId} is what every
     * view, gesture and drop-target slot delivers; the object is released by {@link #viewClose}.
     *
     * @return the {@code gwin_view_t} as a {@code long}; 0 only when the allocation threw
     */
    static long viewCreate(long viewId) {
        try {
            MemorySegment view = (MemorySegment) GWIN_VIEW_CREATE.invokeExact(viewId);
            return view.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_view_close}: {@code GlassView::Close} plus {@code delete}, marshalled to the toolkit
     * thread as {@code ENTER_MAIN_THREAD_AND_RETURN} did. <b>Blocks and upcalls</b> - closing the
     * {@code FullScreenWindow} can fire {@code notify_view} on the way out, which is why
     * {@code WinView._close} removes its registry entry only after this returns. {@code view} is
     * dangling afterwards and must never be passed to a {@code gwin_view_*} function again;
     * {@code View.close()} nulls its handle.
     *
     * @return true; false only when the C caught a C++ exception
     */
    static boolean viewClose(long view) {
        try {
            return (int) GWIN_VIEW_CLOSE.invokeExact(MemorySegment.ofAddress(view)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_view_get_native_view}: the host HWND as a {@code long}, 0 when detached. No thread marshal. */
    static long viewGetNativeView(long view) {
        try {
            MemorySegment hwnd = (MemorySegment) GWIN_VIEW_GET_NATIVE_VIEW.invokeExact(MemorySegment.ofAddress(view));
            return hwnd.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_view_set_parent}: {@code GlassView::SetHostHwnd} on the toolkit thread, which fires
     * {@code notify_view(REMOVE)} then {@code notify_view(ADD)} from inside this call when the host
     * changes. <b>Blocks and upcalls.</b>
     */
    static void viewSetParent(long view, long parentHwnd) {
        try {
            GWIN_VIEW_SET_PARENT.invokeExact(MemorySegment.ofAddress(view), MemorySegment.ofAddress(parentHwnd));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_view_get_x}: the host window's left inset, 0 without a host. Blocks. */
    static int viewGetX(long view) {
        try {
            return (int) GWIN_VIEW_GET_X.invokeExact(MemorySegment.ofAddress(view));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_view_get_y}: the host window's top inset, 0 without a host. Blocks. */
    static int viewGetY(long view) {
        try {
            return (int) GWIN_VIEW_GET_Y.invokeExact(MemorySegment.ofAddress(view));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_view_schedule_repaint}: {@code InvalidateRect(host, NULL, FALSE)}. Blocks. */
    static void viewScheduleRepaint(long view) {
        try {
            GWIN_VIEW_SCHEDULE_REPAINT.invokeExact(MemorySegment.ofAddress(view));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_view_upload_pixels} with the {@code Pixels} resolved here instead of by the
     * {@code Pixels.attachData} upcall the JNI made from inside its {@code SendMessage}
     * ({@code GlassView.cpp _uploadPixels}, {@code Pixels.cpp}). {@link #pixelBits} reproduces what
     * {@code WinPixels._attachInt / _attachByte} handed the C: the base address of a direct buffer,
     * the array of a heap buffer from its {@code arrayOffset()}, or {@code NULL}. The buffer crosses
     * as a borrowed pointer the C never retains, from a confined arena closed on return; a copy rather
     * than a pinned array, because the call blocks in {@code SendMessage} and the C released with
     * {@code JNI_ABORT}, i.e. read it only. The JNI checked {@code IsWindow(host)} before resolving
     * the {@code Pixels} and this resolves first; the export checks again, so a stale host costs one
     * wasted copy and nothing else. Blocks.
     */
    static void viewUploadPixels(long view, Pixels pixels) {
        int width = pixels.getWidth();
        int height = pixels.getHeight();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bits = pixelBits(arena, pixels.getBuffer(), width, height);
            GWIN_VIEW_UPLOAD_PIXELS.invokeExact(MemorySegment.ofAddress(view), width, height, bits);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The {@code bits} pointer {@code gwin_view_upload_pixels} is given for a {@code Pixels} buffer.
     * <ul>
     * <li>Direct: the buffer's base address. {@code duplicate().clear()} is load-bearing -
     * {@link MemorySegment#ofBuffer} honours position and limit, {@code GetDirectBufferAddress}
     * ignored both, and {@code Pixels.getBuffer()} hands out "the original pixels buffer,
     * unmodified", shared with other callers, so it is not rewound in place.</li>
     * <li>Heap with an accessible array: {@code width * height * 4} bytes copied from
     * {@code array()} at {@code arrayOffset()} into {@code arena}, which is what
     * {@code Pixels.attachData} passed ({@code Pixels.java}: {@code array()} and
     * {@code arrayOffset()}, not the position). An array shorter than that is copied as far as it
     * goes and the rest stays zero, where the C read past the end of the Java array.</li>
     * <li>Anything else - a read-only heap buffer, whose {@code array()} threw
     * {@code ReadOnlyBufferException} inside {@code attachData}, which {@code CheckAndClearException}
     * swallowed so that {@code GetBits()} answered {@code NULL}: {@link MemorySegment#NULL}, which the
     * export accepts and hands to {@code SetDIBitsToDevice} unchanged.</li>
     * </ul>
     */
    static MemorySegment pixelBits(Arena arena, Buffer buffer, int width, int height) {
        if (buffer.isDirect()) {
            return MemorySegment.ofBuffer(buffer.duplicate().clear());
        }
        if (!buffer.hasArray()) {
            return MemorySegment.NULL;
        }
        long byteCount = (long) width * height * 4L;
        MemorySegment copy = arena.allocate(byteCount, JAVA_INT.byteAlignment());
        if (buffer instanceof IntBuffer ints) {
            int available = ints.array().length - ints.arrayOffset();
            int count = (int) Math.min(available, byteCount / JAVA_INT.byteSize());
            MemorySegment.copy(ints.array(), ints.arrayOffset(), copy, JAVA_INT, 0L, count);
        } else if (buffer instanceof ByteBuffer bytes) {
            int available = bytes.array().length - bytes.arrayOffset();
            int count = (int) Math.min(available, byteCount);
            MemorySegment.copy(bytes.array(), bytes.arrayOffset(), copy, JAVA_BYTE, 0L, count);
        } else {
            // Pixels only ever holds an IntBuffer or a ByteBuffer (Pixels.java's two constructors).
            return MemorySegment.NULL;
        }
        return copy;
    }

    /**
     * {@code gwin_view_enter_fullscreen}: fires {@code notify_view(FULLSCREEN_ENTER)} on success from
     * inside the call. {@code hideCursor} is passed and ignored, as the JNI ignored it. <b>Blocks and
     * upcalls.</b>
     */
    static boolean viewEnterFullscreen(long view, boolean animate, boolean keepRatio, boolean hideCursor) {
        try {
            return (int) GWIN_VIEW_ENTER_FULLSCREEN.invokeExact(MemorySegment.ofAddress(view), animate ? 1 : 0,
                    keepRatio ? 1 : 0, hideCursor ? 1 : 0) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_view_exit_fullscreen}: fires {@code notify_view(FULLSCREEN_EXIT)} from inside the
     * call; with {@code animate} the REMOVE / ADD / resize of a {@code FullScreenWindow} host arrive
     * later from a {@code WM_TIMER}, possibly after the view is closed, which is why a registry miss
     * is silent. <b>Blocks and upcalls.</b>
     */
    static void viewExitFullscreen(long view, boolean animate) {
        try {
            GWIN_VIEW_EXIT_FULLSCREEN.invokeExact(MemorySegment.ofAddress(view), animate ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_view_enable_ime}: the flag the {@code WndProc} IME path tests, native because it
     * decides a {@code WndProc} return. The C keeps {@code jbool_to_bool}'s rule - only 1 enables - so
     * {@code true} crosses as exactly 1.
     */
    static void viewEnableIme(long view, boolean enable) {
        try {
            GWIN_VIEW_ENABLE_IME.invokeExact(MemorySegment.ofAddress(view), enable ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_view_finish_ime_composition}: {@code ImmNotifyIME(NI_COMPOSITIONSTR, CPS_COMPLETE)}. No marshal. */
    static void viewFinishImeComposition(long view) {
        try {
            GWIN_VIEW_FINISH_IME_COMPOSITION.invokeExact(MemorySegment.ofAddress(view));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code GetDoubleClickTime()}, the whole of {@code _getMultiClickTime_impl}, widened as the JNI's
     * {@code (jlong)} cast widened an unsigned {@code UINT}. The two multi-click extents are
     * {@link #getSystemMetrics} with {@link #SM_CXDOUBLECLK} / {@link #SM_CYDOUBLECLK}.
     */
    static long getDoubleClickTime() {
        try {
            return Integer.toUnsignedLong((int) GET_DOUBLE_CLICK_TIME.invokeExact());
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Window: the twenty-seven gwin_window_* entry points of GlassWindow.cpp, the callback table that
     * carries its eleven upcall sites plus notify_dispose (GwinWindowCallbacks, keyed by the int64 id
     * WinWindow assigns), the _getAnchor WRAPPER over four user32 calls, the three-step LoadCursorW of
     * GlassCursor.cpp's GetNativeCursor, and the icon half of Pixels::CreateIcon
     * ------------------------------------------------------------------------------------------- */

    /*
     * The twelve upcall descriptors; the first JAVA_LONG of each is the window id. Package-private
     * for the reason the view section's are: the shim reads them, and any table a test installs must
     * be built from THESE objects. gwin_test_fire_window_callback - added after the ABI-4 bump as a
     * sibling of gwin_test_fire_callback - fires each slot with the header's fixed pattern; the shim
     * binds it, not this class (the product never calls it), and WinWindowNativeTest proves every
     * descriptor through it, argument by argument, against a recording table built from these objects.
     *
     * Hot: notify_moving (per WM_WINDOWPOSCHANGING during a drag - 13 scalars and one bounded
     * out-pointer, nothing allocated on the "no override" path) and non_client_hit_test (per mouse
     * move over an EXTENDED window's frame - scalars only). Warm: notify_move, notify_resize,
     * notify_focus. Cold: the rest, including notify_delegate_ptr, whose one pointer is an HWND that
     * is never dereferenced and crosses as its address.
     */
    static final FunctionDescriptor WINDOW_NOTIFY_CLOSE_FD = FunctionDescriptor.ofVoid(JAVA_LONG);
    static final FunctionDescriptor WINDOW_NOTIFY_DESTROY_FD = FunctionDescriptor.ofVoid(JAVA_LONG);
    /**
     * Four {@code int32_t}, two {@code float}, seven {@code int32_t}, then the out-pointer - counted
     * against {@code GlassWindow::HandleWindowPosChangingEvent}'s dial and against
     * {@code WinWindow.notifyMoving(IIIIFFIIIIIII)}. One integer out of place would hand the anchor
     * the bit pattern of a float, with no crash and no exception.
     */
    static final FunctionDescriptor WINDOW_NOTIFY_MOVING_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_FLOAT, JAVA_FLOAT, JAVA_INT, JAVA_INT, JAVA_INT,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);
    static final FunctionDescriptor WINDOW_NOTIFY_MOVE_FD =
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor WINDOW_NOTIFY_RESIZE_FD =
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor WINDOW_NOTIFY_SCALE_CHANGED_FD =
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT);
    static final FunctionDescriptor WINDOW_NOTIFY_FOCUS_FD = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT);
    static final FunctionDescriptor WINDOW_NOTIFY_FOCUS_DISABLED_FD = FunctionDescriptor.ofVoid(JAVA_LONG);
    static final FunctionDescriptor WINDOW_NOTIFY_FOCUS_UNGRAB_FD = FunctionDescriptor.ofVoid(JAVA_LONG);
    static final FunctionDescriptor WINDOW_NOTIFY_DELEGATE_PTR_FD =
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS);
    static final FunctionDescriptor WINDOW_NON_CLIENT_HIT_TEST_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT);
    static final FunctionDescriptor WINDOW_NOTIFY_DISPOSE_FD = FunctionDescriptor.ofVoid(JAVA_LONG);

    /**
     * The slot names of {@link #GWIN_WINDOW_CALLBACKS_LAYOUT} in declaration order, which is the
     * order the installer writes them and the order of {@link #WINDOW_SLOT_TARGETS}.
     */
    static final List<String> WINDOW_SLOT_NAMES = List.of("notify_close", "notify_destroy", "notify_moving",
            "notify_move", "notify_resize", "notify_scale_changed", "notify_focus", "notify_focus_disabled",
            "notify_focus_ungrab", "notify_delegate_ptr", "non_client_hit_test", "notify_dispose");

    /** The target methods of this class, parallel to {@link #WINDOW_SLOT_NAMES}. */
    private static final String[] WINDOW_SLOT_TARGETS = {"onWindowClose", "onWindowDestroy", "onWindowMoving",
            "onWindowMove", "onWindowResize", "onWindowScaleChanged", "onWindowFocus", "onWindowFocusDisabled",
            "onWindowFocusUngrab", "onWindowDelegatePtr", "onWindowNonClientHitTest", "onWindowDispose"};
    private static final FunctionDescriptor[] WINDOW_SLOT_DESCRIPTORS = {WINDOW_NOTIFY_CLOSE_FD,
            WINDOW_NOTIFY_DESTROY_FD, WINDOW_NOTIFY_MOVING_FD, WINDOW_NOTIFY_MOVE_FD, WINDOW_NOTIFY_RESIZE_FD,
            WINDOW_NOTIFY_SCALE_CHANGED_FD, WINDOW_NOTIFY_FOCUS_FD, WINDOW_NOTIFY_FOCUS_DISABLED_FD,
            WINDOW_NOTIFY_FOCUS_UNGRAB_FD, WINDOW_NOTIFY_DELEGATE_PTR_FD, WINDOW_NON_CLIENT_HIT_TEST_FD,
            WINDOW_NOTIFY_DISPOSE_FD};

    /**
     * winuser.h {@code HTNOWHERE}: what {@code non_client_hit_test} answers on a Java throw, because
     * HotSpot's {@code CallIntMethod} returned 0 with an exception pending and the shipped result of
     * that was "not HTCAPTION, not HTUNSPECIFIED, answer HTNOWHERE" ({@code HandleNCHitTestEvent}).
     * {@code HTCLIENT} (1) would be an improvement, hence not neutral. Pinned by {@code static_assert}
     * in {@code glass_win_api.cpp}.
     */
    static final int HTNOWHERE = 0;

    private static boolean windowCallbacksInstalled;

    /**
     * The twelve stubs, created once by {@link #installWindowCallbacks} in {@link Arena#global()} and
     * kept so that {@link #reinstallWindowCallbacks} can write the same addresses again. Never
     * replaced: a stub the library has been given must outlive the process.
     */
    private static MemorySegment[] windowCallbackStubs;

    /**
     * {@code sizeof(GwinWindowCallbacks)} as the C compiler saw it; {@link #GWIN_WINDOW_CALLBACKS_LAYOUT}
     * must agree.
     */
    static int sizeOfWindowCallbacks() {
        try {
            return (int) GWIN_SIZEOF_WINDOW_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Hands {@code glass.dll} the table that replaced the eleven {@code Call*Method} sites of
     * {@code GlassWindow.cpp} and the {@code DeleteGlobalRef} of {@code ~GlassWindow}. The install was
     * the flip: before it every site took its JNI path. Those paths are deleted, so a site that finds
     * no table installed now delivers nothing ({@code WindowCallbacks()} answers {@code NULL}). The
     * flip travelled in the same change set as {@code WinWindow}'s forwarders: a table with no
     * registry behind it drops every window event, and a JNI-created window under an installed table
     * would have delivered id 0 to a registry that knows no 0.
     * <p>
     * <b>Call this exactly once, from {@code WinApplication}'s static initializer</b>, beside
     * {@link #installViewCallbacks} and for the same reason: the launcher thread, after the library
     * is loaded and before {@code Application.run()} creates the toolkit window, so the table exists
     * before the first {@code WndProc} message and is never written from a second thread. No
     * {@code void* user}, as for the view tables: the identity is per window, the {@code int64_t} id
     * {@code WinWindow} registered itself under before it called {@code gwin_window_create}.
     * <p>
     * A slot left {@code NULL} would be replaced by an internal no-op in C - and for
     * {@code non_client_hit_test} that no-op answers {@code HTNOWHERE}, which silently turns an
     * EXTENDED window's caption off - so all twelve are written. The table is copied by the library;
     * the confined arena that holds it is closed here, the stubs live in {@link Arena#global()}.
     */
    static synchronized void installWindowCallbacks() {
        if (windowCallbacksInstalled) {
            return;
        }
        windowCallbackStubs = stubsFor(WINDOW_SLOT_TARGETS, WINDOW_SLOT_DESCRIPTORS);
        writeWindowTable();
        windowCallbacksInstalled = true;
    }

    /** Whether {@link #installWindowCallbacks} has run in this JVM. */
    static synchronized boolean windowCallbacksInstalled() {
        return windowCallbacksInstalled;
    }

    /**
     * Writes the production table again with the stubs {@link #installWindowCallbacks} created;
     * {@code gwin_window_set_callbacks} copies by value and the last call wins, so this restores
     * after a test has installed a table of its own. Installs first if nothing has been installed
     * yet; never creates a second set of stubs.
     */
    static synchronized void reinstallWindowCallbacks() {
        if (!windowCallbacksInstalled) {
            installWindowCallbacks();
            return;
        }
        writeWindowTable();
    }

    /** The twelve installed stubs in slot order, for the test that reads them back; empty before the install. */
    static synchronized List<MemorySegment> installedWindowCallbackStubs() {
        if (!windowCallbacksInstalled) {
            return List.of();
        }
        return List.of(windowCallbackStubs);
    }

    private static void writeWindowTable() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(GWIN_WINDOW_CALLBACKS_LAYOUT);
            fillCallbackTable(table, GWIN_WINDOW_CALLBACKS_LAYOUT, WINDOW_SLOT_NAMES, windowCallbackStubs);
            requireOk((int) GWIN_WINDOW_SET_CALLBACKS.invokeExact(table), "gwin_window_set_callbacks");
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /*
     * The twelve targets. As for the view section: the marshalling is here, the registry lookup and
     * the protected Window.notify* call are WinWindow's (they are protected members of another
     * package, reachable only through a WinWindow body), and each catches Throwable, because letting
     * anything out of an upcall stub terminates the JVM. reportUpcallFailure is CheckAndClearException
     * (the former Utils.cpp:50-71): report on this thread, swallow, carry on - including a throw from the
     * application's own UncaughtExceptionHandler. Thread: always the JavaFX application thread,
     * inside DispatchMessage, inside the WH_CBT hook, or inside a gwin_window_* downcall that
     * ExecAction marshalled there; re-entrant; possibly on ANOTHER window's WndProc frame
     * (FullScreenWindow reaches the owner's HandleActivateEvent / HandleCloseEvent / SetDelegateWindow,
     * and SetDelegateWindow's UngrabFocus can fire notify_focus_ungrab on a third window) - the id is
     * always the right one. Nothing here takes a lock. An id the registry does not know is a stale
     * peer or a JNI-created window (id 0): the slot default, silently, no report. Package-private so
     * that WinGlassNativeShim can drive every arm without a table.
     */

    /**
     * {@code notify_close}: {@code Window.notifyClose()}; WM_CLOSE, and the FullScreenWindow's
     * WM_CLOSE on the owner.
     */
    static void onWindowClose(long windowId) {
        try {
            WinWindow.dispatchClose(windowId);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_destroy}: {@code Window.notifyDestroy()}, which zeroes {@code Window.ptr}; WM_DESTROY. */
    static void onWindowDestroy(long windowId) {
        try {
            WinWindow.dispatchDestroy(windowId);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code notify_moving}: {@code WinWindow.notifyMoving(...)} from WM_WINDOWPOSCHANGING.
     * <b>Query, hot.</b> A non-null answer is written to {@code out_bounds[0..3]} as x, y, w, h and
     * 1 is returned; {@code null}, a throw - and an answer that is not exactly four ints, the JNI's
     * "bad array length" arm minus its {@code stderr} line, unreachable from {@code WinWindow} itself
     * - leave the buffer untouched and answer 0, which is what a null return, a thrown exception and
     * a failed {@code GetIntArrayRegion} all produced. {@code fxX} / {@code fxY} arrive as typed
     * {@code 0.0f}: the JNI passed two {@code int} literals where {@code CallObjectMethod}'s varargs
     * wanted doubles, and the values are dead on this path because {@code resize_mode} is never
     * {@code RESIZE_TO_FX_ORIGIN} from the WndProc. Nothing is allocated on the null path.
     */
    static int onWindowMoving(long windowId, int x, int y, int w, int h, float fxX, float fxY, int anchorX,
                              int anchorY, int resizeMode, int insetLeft, int insetTop, int insetRight,
                              int insetBottom, MemorySegment outBounds) {
        try {
            int[] bounds = WinWindow.dispatchMoving(windowId, x, y, w, h, fxX, fxY, anchorX, anchorY, resizeMode,
                    insetLeft, insetTop, insetRight, insetBottom);
            if (bounds == null || bounds.length != 4) {
                return 0;
            }
            writeBounds(outBounds, bounds);
            return 1;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return 0;
        }
    }

    /**
     * {@code notify_move}: {@code Window.notifyMove(int,int)}; WM_MOVE, WM_DISPLAYCHANGE,
     * WM_SETTINGCHANGE and WM_SHOWWINDOW.
     */
    static void onWindowMove(long windowId, int x, int y) {
        try {
            WinWindow.dispatchMove(windowId, x, y);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_resize}: {@code WinWindow.notifyResize(int,int,int)} - the override, not {@code Window}'s. */
    static void onWindowResize(long windowId, int type, int width, int height) {
        try {
            WinWindow.dispatchResize(windowId, type, width, height);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code notify_scale_changed}: {@code Window.notifyScaleChanged(float x4)}; WM_DPICHANGED, all
     * four the same value.
     */
    static void onWindowScaleChanged(long windowId, float platformX, float platformY, float outputX, float outputY) {
        try {
            WinWindow.dispatchScaleChanged(windowId, platformX, platformY, outputX, outputY);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_focus}: {@code Window.notifyFocus(int)} with FOCUS_GAINED / FOCUS_LOST; WM_ACTIVATE. */
    static void onWindowFocus(long windowId, int event) {
        try {
            WinWindow.dispatchFocus(windowId, event);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_focus_disabled}: {@code Window.notifyFocusDisabled()}; WM_MOUSEACTIVATE and the CBT hook. */
    static void onWindowFocusDisabled(long windowId) {
        try {
            WinWindow.dispatchFocusDisabled(windowId);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code notify_focus_ungrab}: {@code Window.notifyFocusUngrab()}; {@code GlassWindow::UngrabFocus}. */
    static void onWindowFocusUngrab(long windowId) {
        try {
            WinWindow.dispatchFocusUngrab(windowId);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code notify_delegate_ptr}: {@code Window.notifyDelegatePtr(long)} with the FullScreenWindow's
     * HWND, or {@code NULL} on detach. The pointer is a USER handle value, never dereferenced by
     * either side, and crosses as its address; cold.
     */
    static void onWindowDelegatePtr(long windowId, MemorySegment delegateHwnd) {
        try {
            WinWindow.dispatchDelegatePtr(windowId, delegateHwnd.address());
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code non_client_hit_test}: {@code WinWindow.nonClientHitTest(int,int)} for EXTENDED windows.
     * <b>Query, hot.</b> The return is load-bearing - an HT* constant or {@code HTUNSPECIFIED}, which
     * the C post-processes into the WndProc's WM_NCHITTEST answer. A throw, and an unknown id, answer
     * {@link #HTNOWHERE}.
     */
    static int onWindowNonClientHitTest(long windowId, int x, int y) {
        try {
            return WinWindow.dispatchNonClientHitTest(windowId, x, y);
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return HTNOWHERE;
        }
    }

    /**
     * {@code notify_dispose}: {@code ~GlassWindow}, at the point {@code DeleteGlobalRef} was. Drops
     * the registry entry and does <em>nothing else</em>: it fires after {@code RemoveProp}, so
     * {@code FromHandle} is already {@code NULL} inside it and any {@code gwin_window_*} call from
     * here would dereference NULL in a C++ destructor. It must not call
     * {@code Window.notifyDestroy}, which is the separate slot fired earlier from WM_DESTROY.
     */
    static void onWindowDispose(long windowId) {
        try {
            WinWindow.dispose(windowId);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code out_bounds[0..3] = x, y, w, h}: the one {@code reinterpret} of the window section,
     * bounding the caller-owned four-int buffer to its 16 bytes before the copy, which writes all
     * four or - if the segment could not be bounded - none.
     */
    private static void writeBounds(MemorySegment outBounds, int[] bounds) {
        MemorySegment out = bounded(outBounds, 4 * JAVA_INT.byteSize());
        MemorySegment.copy(bounds, 0, out, JAVA_INT, 0L, 4);
    }

    /* -- the twenty-seven downcalls, one per former JNI body of GlassWindow.cpp --------------------- */

    /**
     * {@code gwin_window_create}: {@code new GlassWindow} + {@code BaseWnd::Create} on the toolkit
     * thread, as {@code ENTER_MAIN_THREAD_AND_RETURN(jlong)} did. {@code ownerPtr} is the owner's
     * HWND or 0; {@code monitor} is the HMONITOR {@code Screen.getNativeScreen()} holds, stored and
     * never read again; {@code mask} is the {@code Window} style bit set, and the whole style
     * computation stays in C. {@code windowId} is what every slot will carry back, so
     * {@code WinWindow} registers it BEFORE this call: on the {@code CreateWindowEx}-failed path the
     * GlassWindow is deleted, and {@code notify_dispose(windowId)} fires, before this returns.
     * <b>Blocks and upcalls.</b>
     *
     * @return the HWND - {@code Window.ptr}, the value {@code getNativeWindow()} hands to Prism and
     *         to embedders - or 0 when {@code CreateWindowEx} failed or a C++ exception was caught.
     *         With no toolkit the return is INDETERMINATE stack contents, exactly as the JNI's
     *         was; it is not normalised here, and {@code Window}'s constructor rejects only 0
     */
    static long windowCreate(long ownerPtr, long monitor, int mask, long windowId) {
        try {
            MemorySegment hwnd = (MemorySegment) GWIN_WINDOW_CREATE.invokeExact(MemorySegment.ofAddress(ownerPtr),
                    MemorySegment.ofAddress(monitor), mask, windowId);
            return hwnd.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_close}: {@code GlassWindow::Close} then {@code ::DestroyWindow}, synchronous
     * - {@code notify_destroy} is certain, {@code notify_dispose} likely, before this returns; the
     * registry entry goes with the latter and never here. Does not null-check the GlassWindow.
     * <b>Blocks and upcalls.</b>
     *
     * @return {@code DestroyWindow}'s result; indeterminate with no toolkit
     */
    static boolean windowClose(long hwnd) {
        try {
            return (int) GWIN_WINDOW_CLOSE.invokeExact(MemorySegment.ofAddress(hwnd)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_view}: {@code ViewContainer::SetGlassView(view)} on the toolkit thread,
     * after the active-touch-window clear and {@code ResetMouseTracking}. {@code view} is the
     * {@code GlassView*} the {@code WinView} holds ({@code View.ptr}, which the JNI read through
     * {@code javaIDs.View.ptr}), or 0. Always true, as the JNI returned {@code JNI_TRUE} from
     * outside its action. Does not null-check the GlassWindow. <b>Blocks.</b>
     */
    static boolean windowSetView(long hwnd, long view) {
        try {
            return (int) GWIN_WINDOW_SET_VIEW.invokeExact(MemorySegment.ofAddress(hwnd),
                    MemorySegment.ofAddress(view)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_update_view_size}: {@code IsWindowVisible ? NotifyViewSize : nothing}.
     * <b>Blocks and upcalls</b> (the view table).
     */
    static void windowUpdateViewSize(long hwnd) {
        try {
            GWIN_WINDOW_UPDATE_VIEW_SIZE.invokeExact(MemorySegment.ofAddress(hwnd));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_window_set_menubar}: {@code ::SetMenu(hwnd, hmenu)} and the insets bookkeeping. <b>Blocks.</b> */
    static boolean windowSetMenubar(long hwnd, long hmenu) {
        try {
            return (int) GWIN_WINDOW_SET_MENUBAR.invokeExact(MemorySegment.ofAddress(hwnd),
                    MemorySegment.ofAddress(hmenu)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_level}: {@code SetWindowPos(HWND_TOPMOST / HWND_NOTOPMOST)}. The
     * {@code Window.Level} mapping stays in C so that {@code (HWND)-1} is not a Java constant:
     * FLOATING and TOPMOST are topmost, every other value is not. <b>Blocks.</b>
     */
    static void windowSetLevel(long hwnd, int level) {
        try {
            GWIN_WINDOW_SET_LEVEL.invokeExact(MemorySegment.ofAddress(hwnd), level);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_focusable}: only 1 is true, as the JNI compared against
     * {@code JNI_TRUE}. <b>Blocks.</b>
     */
    static void windowSetFocusable(long hwnd, boolean focusable) {
        try {
            GWIN_WINDOW_SET_FOCUSABLE.invokeExact(MemorySegment.ofAddress(hwnd), focusable ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_window_set_enabled}: same "only 1 is true" rule. <b>Blocks.</b> */
    static void windowSetEnabled(long hwnd, boolean enabled) {
        try {
            GWIN_WINDOW_SET_ENABLED.invokeExact(MemorySegment.ofAddress(hwnd), enabled ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_alpha}: the {@code float} crosses as is and the C computes
     * {@code F2B(value) = BYTE(255.f * value)} on this side of its marshal, truncating toward zero
     * and wrapping outside [0, 1] - reproduced, not clamped. <b>Blocks.</b>
     */
    static void windowSetAlpha(long hwnd, float alpha) {
        try {
            GWIN_WINDOW_SET_ALPHA.invokeExact(MemorySegment.ofAddress(hwnd), alpha);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_dark_frame}: {@code DwmSetWindowAttribute(DWMWA_USE_IMMERSIVE_DARK_MODE)}.
     * <b>Blocks.</b>
     */
    static void windowSetDarkFrame(long hwnd, boolean dark) {
        try {
            GWIN_WINDOW_SET_DARK_FRAME.invokeExact(MemorySegment.ofAddress(hwnd), dark ? 1 : 0);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_get_insets}: {@code UpdateInsets} then {@code (left << 48) | (top << 32) |
     * (right << 16) | bottom}, aliasing above 65535 or below 0 exactly as the JNI's did and as
     * {@code WinWindow} unpacks. 0 when {@code ::IsWindow} is false - tested before the GlassWindow
     * is looked up, so a stale or fake handle is safe here. Not marshalled: it runs on the calling
     * thread and mutates {@code m_insets}, racing off the toolkit thread as the JNI did.
     */
    static long windowGetInsets(long hwnd) {
        try {
            return (long) GWIN_WINDOW_GET_INSETS.invokeExact(MemorySegment.ofAddress(hwnd));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_bounds}: the JNI body unchanged, {@code SWP_NOSENDCHANGING} included,
     * so this path never produces {@code notify_moving}. {@code xSet} / {@code ySet} cross as 0 / 1;
     * {@code xGravity} / {@code yGravity} are accepted and ignored, as the JNI declared and never
     * read them. Silent when {@code ::IsWindow} is false. <b>Blocks.</b>
     */
    static void windowSetBounds(long hwnd, int x, int y, boolean xSet, boolean ySet, int w, int h, int cw, int ch,
                                float xGravity, float yGravity) {
        try {
            GWIN_WINDOW_SET_BOUNDS.invokeExact(MemorySegment.ofAddress(hwnd), x, y, xSet ? 1 : 0, ySet ? 1 : 0,
                    w, h, cw, ch, xGravity, yGravity);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_title}: {@code ::SetWindowTextW} with the title as {@link #wideString}
     * builds it - UTF-16 code units and a NUL, never UTF-8, an embedded {@code U+0000} truncating
     * the visible title there as it did. {@code Window.setTitle} substitutes {@code ""} for
     * {@code null} before it gets here; a {@code null} here is a {@link NullPointerException} where
     * {@code JString} dereferenced NULL. <b>Blocks.</b>
     */
    static boolean windowSetTitle(long hwnd, String title) {
        try (Arena arena = Arena.ofConfined()) {
            return (int) GWIN_WINDOW_SET_TITLE.invokeExact(MemorySegment.ofAddress(hwnd),
                    wideString(arena, title)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_resizable}: only 1 is true ({@code jbool_to_bool}); false for a child
     * window. <b>Blocks.</b>
     */
    static boolean windowSetResizable(long hwnd, boolean resizable) {
        try {
            return (int) GWIN_WINDOW_SET_RESIZABLE.invokeExact(MemorySegment.ofAddress(hwnd),
                    resizable ? 1 : 0) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_visible}: {@code ShowWindow} and the focus / touch bookkeeping around
     * it. <b>Returns its own argument</b>, not a status - the JNI's {@code return visible;} sat
     * outside the action, and {@code Window.setVisible} only tests it for truth. <b>Blocks and
     * upcalls</b> ({@code notify_focus_ungrab}; the touch teardown reaches the view tables).
     */
    static boolean windowSetVisible(long hwnd, boolean visible) {
        try {
            return (int) GWIN_WINDOW_SET_VISIBLE.invokeExact(MemorySegment.ofAddress(hwnd), visible ? 1 : 0) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_request_focus}: {@code ::SetForegroundWindow}; {@code event} is only
     * ASSERTed. <b>Blocks.</b>
     */
    static boolean windowRequestFocus(long hwnd, int event) {
        try {
            return (int) GWIN_WINDOW_REQUEST_FOCUS.invokeExact(MemorySegment.ofAddress(hwnd), event) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_grab_focus}: the process-wide grab, which ungrabs - and fires
     * {@code notify_focus_ungrab} on - whichever window held it before. <b>Blocks and upcalls.</b>
     */
    static boolean windowGrabFocus(long hwnd) {
        try {
            return (int) GWIN_WINDOW_GRAB_FOCUS.invokeExact(MemorySegment.ofAddress(hwnd)) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_window_ungrab_focus}: nothing unless this window holds the grab. <b>Blocks and upcalls.</b> */
    static void windowUngrabFocus(long hwnd) {
        try {
            GWIN_WINDOW_UNGRAB_FOCUS.invokeExact(MemorySegment.ofAddress(hwnd));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_minimize}: {@code ShowWindow(SW_MINIMIZE / SW_RESTORE)}; always true, as the
     * JNI was. <b>Blocks and upcalls.</b>
     */
    static boolean windowMinimize(long hwnd, boolean minimize) {
        try {
            return (int) GWIN_WINDOW_MINIMIZE.invokeExact(MemorySegment.ofAddress(hwnd), minimize ? 1 : 0) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_maximize}: {@code ShowWindow(SW_MAXIMIZE / SW_RESTORE)}; always true.
     * {@code wasMaximized} crosses and is never read, as in the JNI. <b>Blocks and upcalls.</b>
     */
    static boolean windowMaximize(long hwnd, boolean maximize, boolean wasMaximized) {
        try {
            return (int) GWIN_WINDOW_MAXIMIZE.invokeExact(MemorySegment.ofAddress(hwnd), maximize ? 1 : 0,
                    wasMaximized ? 1 : 0) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_min_size}: the state {@code WM_GETMINMAXINFO} answers. The C maps a 0
     * argument to -1 ("not set") on its side of the marshal; {@link #windowSetMaxSize} does NOT, so
     * a maximum of 0 really pins the window to zero - the shipped asymmetry, kept where it was.
     * <b>Blocks.</b>
     */
    static boolean windowSetMinSize(long hwnd, int width, int height) {
        try {
            return (int) GWIN_WINDOW_SET_MIN_SIZE.invokeExact(MemorySegment.ofAddress(hwnd), width, height) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_window_set_max_size}: see {@link #windowSetMinSize} for the asymmetry. <b>Blocks.</b> */
    static boolean windowSetMaxSize(long hwnd, int width, int height) {
        try {
            return (int) GWIN_WINDOW_SET_MAX_SIZE.invokeExact(MemorySegment.ofAddress(hwnd), width, height) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_icon}: {@code WM_SETICON} for ICON_SMALL and ICON_BIG, then the window
     * <b>takes ownership</b> of {@code hicon} - it destroys the previous icon now and this one on the
     * next call or in {@code ~GlassWindow}. Nothing in this class ever calls {@code DestroyIcon},
     * and nothing may: the handle {@link #iconCreate} built is the C's from here on. 0 means "no
     * icon". Not marshalled and does not upcall. A NULL GlassWindow - a stale or foreign HWND - is
     * checked, and the icon is then neither installed nor destroyed: the one case in which the
     * caller still owns it, which {@code Window.setIcon} cannot reach because it checks the window is
     * open first.
     */
    static void windowSetIcon(long hwnd, long hicon) {
        try {
            GWIN_WINDOW_SET_ICON.invokeExact(MemorySegment.ofAddress(hwnd), MemorySegment.ofAddress(hicon));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_to_front}: {@code SetWindowPos(HWND_TOP)}, after a TOPMOST step for an
     * unfocusable window. <b>Blocks.</b>
     */
    static void windowToFront(long hwnd) {
        try {
            GWIN_WINDOW_TO_FRONT.invokeExact(MemorySegment.ofAddress(hwnd));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_window_to_back}: {@code SetWindowPos(HWND_BOTTOM)}. <b>Blocks.</b> */
    static void windowToBack(long hwnd) {
        try {
            GWIN_WINDOW_TO_BACK.invokeExact(MemorySegment.ofAddress(hwnd));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_set_cursor}: {@code BaseWnd::SetCursor} on this window and on its delegate
     * - stores the HCURSOR for {@code WM_SETCURSOR} to replay and calls {@code ::SetCursor} at once.
     * {@code hcursor} is what {@code WinCursor.nativeHandleFor} resolved, 0 for no cursor; the
     * resolution the JNI made inside its action now runs before the call, on the same thread on
     * Windows. <b>Blocks.</b>
     */
    static void windowSetCursor(long hwnd, long hcursor) {
        try {
            GWIN_WINDOW_SET_CURSOR.invokeExact(MemorySegment.ofAddress(hwnd), MemorySegment.ofAddress(hcursor));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_window_show_system_menu}: {@code TrackPopupMenu(TPM_RETURNCMD)}, which <b>runs a
     * nested modal message loop</b> on the toolkit thread for as long as the menu is open, and every
     * message dispatched inside it can upcall. {@code x} / {@code y} are client coordinates in
     * physical pixels. Never call it from an upcall target.
     */
    static void windowShowSystemMenu(long hwnd, int x, int y) {
        try {
            GWIN_WINDOW_SHOW_SYSTEM_MENU.invokeExact(MemorySegment.ofAddress(hwnd), x, y);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* -- the WRAPPER: _getAnchor over user32, no C ---------------------------------------------------- */

    /**
     * {@code Java_com_sun_glass_ui_win_WinWindow__1getAnchor} ({@code GlassWindow.cpp}), step for
     * step and on the calling thread, as it had no {@code ENTER_MAIN_THREAD}:
     * <ol>
     * <li>{@code !IsWindow(hwnd)} answers <b>0</b> - not {@code ANCHOR_NO_CAPTURE}. The two are not
     * interchangeable: 0 sends {@code WinWindow.setBounds} down {@code RESIZE_AROUND_ANCHOR} with a
     * (0, 0) anchor, {@code ANCHOR_NO_CAPTURE} down {@code RESIZE_TO_FX_ORIGIN};</li>
     * <li>{@code GetCapture() != hwnd} answers {@code ANCHOR_NO_CAPTURE}. {@code GetCapture} is
     * per-thread, so off the toolkit thread this is the answer, as before;</li>
     * <li>{@code GetCursorPos && GetWindowRect}, short-circuit - a failed {@code GetCursorPos} skips
     * {@code GetWindowRect} - packs cursor minus window origin with {@link #packAnchor};</li>
     * <li>either call failing falls through to {@code ANCHOR_NO_CAPTURE}, not 0.</li>
     * </ol>
     */
    static long windowAnchor(long hwnd) {
        MemorySegment window = MemorySegment.ofAddress(hwnd);
        if (isWindow(window) == 0) {
            return 0L;
        }
        if (captureWindow() != hwnd) {
            return WinWindow.ANCHOR_NO_CAPTURE;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment point = arena.allocate(POINT_LAYOUT);
            MemorySegment rect = arena.allocate(RECT_LAYOUT);
            if (cursorPos(point) != 0 && windowRect(window, rect) != 0) {
                return packAnchor(point.get(JAVA_INT, POINT_X_OFFSET) - rect.get(JAVA_INT, RECT_LEFT_OFFSET),
                        point.get(JAVA_INT, POINT_Y_OFFSET) - rect.get(JAVA_INT, RECT_TOP_OFFSET));
            }
        }
        return WinWindow.ANCHOR_NO_CAPTURE;
    }

    /**
     * {@code ((jlong) anchor.x << 32) | ((jlong) anchor.y & 0xffffffffL)} with the two
     * {@code LONG} differences already taken in 32 bits: the x half is a signed shift, the y half is
     * masked, so a negative y comes back as a large unsigned low word that {@code WinWindow.setBounds}
     * recovers with {@code (int) anchor}.
     */
    static long packAnchor(int deltaX, int deltaY) {
        return ((long) deltaX << 32) | ((long) deltaY & 0xffffffffL);
    }

    private static int isWindow(MemorySegment hwnd) {
        try {
            return (int) IS_WINDOW.invokeExact(hwnd);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code GetCapture()} as a {@code long}, 0 when no window of this thread has the capture. */
    private static long captureWindow() {
        try {
            MemorySegment hwnd = (MemorySegment) GET_CAPTURE.invokeExact();
            return hwnd.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static int cursorPos(MemorySegment point) {
        try {
            return (int) GET_CURSOR_POS.invokeExact(point);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static int windowRect(MemorySegment hwnd, MemorySegment rect) {
        try {
            return (int) GET_WINDOW_RECT.invokeExact(hwnd, rect);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* -- the system cursors: GetNativeCursor's three-step LoadCursorW (GlassCursor.cpp) --------------- */

    /**
     * winuser.h {@code IDC_*}: the {@code MAKEINTRESOURCE} ordinals {@code GlassCursor.cpp}'s
     * {@code GetNativeCursor} hands {@code LoadCursorW}, pinned by {@code static_assert} in
     * {@code glass_win_api.cpp}. {@code WinCursor} owns the {@code Cursor} type to ordinal switch.
     */
    static final int IDC_ARROW = 32512;
    static final int IDC_IBEAM = 32513;
    static final int IDC_WAIT = 32514;
    static final int IDC_CROSS = 32515;
    static final int IDC_SIZENWSE = 32642;
    static final int IDC_SIZENESW = 32643;
    static final int IDC_SIZEWE = 32644;
    static final int IDC_SIZENS = 32645;
    static final int IDC_SIZEALL = 32646;
    static final int IDC_HAND = 32649;

    /** The two cursors {@code GlassResources.rc} carries, loaded by name from {@code glass.dll} itself. */
    static final String IDC_CLOSED_HAND = "IDC_CLOSED_HAND";
    static final String IDC_OPEN_HAND = "IDC_OPEN_HAND";

    /** {@link #loadSystemCursor(MemorySegment)} for an {@code IDC_*} ordinal. */
    static long loadSystemCursor(int ordinal) {
        return loadSystemCursor(MemorySegment.ofAddress(ordinal));
    }

    /** {@link #loadSystemCursor(MemorySegment)} for a resource name, as a NUL-terminated UTF-16 string. */
    static long loadSystemCursor(String name) {
        try (Arena arena = Arena.ofConfined()) {
            return loadSystemCursor(wideString(arena, name));
        }
    }

    /**
     * The tail of the former {@code GetNativeCursor} ({@code GlassCursor.cpp}), unconditionally for every
     * resource, ordinal or name: {@code LoadCursorW(NULL, id)}, then - "not a system cursor,
     * check for resource" - {@code LoadCursorW} on {@code glass.dll}'s own module, the only place the two
     * {@code GlassResources.rc} cursors resolve, then {@code LoadCursorW(NULL, IDC_ARROW)}. The C passed
     * {@code GlassApplication::GetHInstance()} for that module; Java resolves the handle itself with
     * {@link #moduleHandle} ({@code GetModuleHandleW(L"glass.dll")}). The C then only ASSERTed the
     * result. Shared system cursors are never destroyed, and the JNI never destroyed these either.
     *
     * @return the {@code HCURSOR} as a {@code long}; 0 only if even the arrow could not be loaded
     */
    private static long loadSystemCursor(MemorySegment resource) {
        long cursor = loadCursor(MemorySegment.NULL, resource);
        if (cursor == 0L) {
            cursor = loadCursor(MemorySegment.ofAddress(moduleHandle(LIBRARY_NAME + ".dll")), resource);
        }
        if (cursor == 0L) {
            cursor = loadCursor(MemorySegment.NULL, MemorySegment.ofAddress(IDC_ARROW));
        }
        return cursor;
    }

    /**
     * {@code GetModuleHandleW(fileName)}: the {@code HMODULE} of a DLL this process has loaded, by
     * base name, or 0. For {@code glass.dll} it is the handle the loader mapped the library at - the
     * {@code HINSTANCE} the C once recorded from {@code DllMain} for {@code GlassApplication::GetHInstance()},
     * a chain deleted once {@code GlassCursor.cpp} went and nothing read it. Java resolves the handle
     * itself, on every {@code NativeLibLoader} path that keeps the file name - {@code WinWindowNativeTest}
     * asserts it by loading a cursor through it.
     */
    static long moduleHandle(String fileName) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment module = (MemorySegment) GET_MODULE_HANDLE_W.invokeExact(wideString(arena, fileName));
            return module.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static long loadCursor(MemorySegment module, MemorySegment resource) {
        try {
            MemorySegment cursor = (MemorySegment) LOAD_CURSOR_W.invokeExact(module, resource);
            return cursor.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ==== Screens: GlassScreen.cpp's enumeration over user32, gdi32 and shcore ======================= */

    /** Guards {@link #dpiFuncsTried}. */
    private static final Object DPI_LOCK = new Object();

    /** {@code triedToFindDPIFuncs} ({@code GlassScreen.cpp}): whether {@link #loadDpiFuncs} has run. */
    private static boolean dpiFuncsTried;

    /**
     * {@code GlassScreen::LoadDPIFuncs(awareRequested)}, behind the same one-shot latch: the first call
     * resolves {@code SHCore.dll} ({@link Shcore}), sets the process DPI awareness -
     * {@code SetProcessDpiAwareness(awarenessRequested)} when the three shcore functions exist, else
     * {@code SetProcessDPIAware()}, both results ignored - and then calls {@code GetProcessDpiAwareness}
     * once and discards the answer, as the release C did (only its {@code fprintf} was {@code DEBUG_DPI}).
     * Every later call does nothing, whatever it asks for.
     * <p>
     * Two callers, as in C: the toolkit start-up, with the level {@code WinApplication} computed, on the
     * toolkit thread before {@link #appCreate()}; and {@link #collectMonitors()}, with
     * {@link #PROCESS_PER_MONITOR_DPI_AWARE}, which only has an effect when no start-up ran first. Under
     * the JDK launchers neither changes anything - their manifest already declares PerMonitorV2, so
     * {@code SetProcessDpiAwareness} answers {@code E_ACCESSDENIED} - so the order matters to a
     * manifest-less host only, and no test in this module can observe it. <b>Never call it from a static
     * initializer.</b>
     * <p>
     * This is the only latch there is. Both callers of {@code GlassScreen::LoadDPIFuncs} and its own
     * {@code triedToFindDPIFuncs} - the JNI {@code _init} and {@code CreateJavaScreens} - lost their Java entry
     * points in the change set that made the enumeration Java, and all four have since been deleted
     * from {@code glass.dll}.
     */
    static void loadDpiFuncs(int awarenessRequested) {
        synchronized (DPI_LOCK) {
            if (dpiFuncsTried) {
                return;
            }
            dpiFuncsTried = true;
            try {
                if (Shcore.RESOLVED) {
                    int ignoredResult = (int) Shcore.SET_PROCESS_DPI_AWARENESS.invokeExact(awarenessRequested);
                } else {
                    int ignoredResult = (int) SET_PROCESS_DPI_AWARE.invokeExact();
                }
                if (Shcore.RESOLVED) {
                    try (Arena arena = Arena.ofConfined()) {
                        MemorySegment ignoredAwareness = arena.allocate(JAVA_INT);
                        int ignoredResult = (int) Shcore.GET_PROCESS_DPI_AWARENESS.invokeExact(MemorySegment.NULL,
                                ignoredAwareness);
                    }
                }
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    /**
     * Whether the three {@code shcore} DPI functions resolved, which initializes {@link Shcore} (loading
     * {@code SHCore.dll}) but sets nothing. For the tests that fix the order of the lazy holders in
     * {@link #boundSymbols()}, and for {@code GetMonitorSettings}' choice between the DPI query and
     * {@code LOGPIXELSX/Y}.
     */
    static boolean dpiFunctionsResolved() {
        return Shcore.RESOLVED;
    }

    /** {@code MONITORENUMPROC}: {@code BOOL CALLBACK (HMONITOR, HDC, LPRECT, LPARAM)}. */
    private static final FunctionDescriptor MONITOR_ENUM_PROC_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG);

    /**
     * {@code GlassScreen::CreateJavaScreens} up to its arrangement, with {@code GetMonitorSettings} for
     * each monitor: one {@code EnumDisplayMonitors(NULL, NULL, proc, 0)} pass that only counts, then a
     * second that collects at most that many monitors, skipping a NULL handle. A monitor attached between
     * the two passes is left out, as the C left it out. The list is in enumeration order and empty when
     * Windows reported no monitor; {@link WinScreenLayout#arrange} turns that into the {@code null} the
     * JNI returned.
     * <p>
     * Each pass has its own upcall stub, in a confined arena that is closed when both passes have returned
     * - {@code EnumDisplayMonitors} is synchronous, so nothing can call a stub afterwards. The targets never
     * let a {@code Throwable} out, which would end the JVM: the first one is kept, the pass is allowed to
     * finish, and it is thrown from here once {@code EnumDisplayMonitors} has returned.
     * <p>
     * Runs on the calling thread - the toolkit thread in the product, from inside
     * {@code Screen.initScreens} and from inside the {@code settings_changed} upcall - and calls
     * {@link #loadDpiFuncs} with {@link #PROCESS_PER_MONITOR_DPI_AWARE} before the first monitor's DPI
     * query, as {@code GetMonitorSettings} did.
     */
    static List<WinScreenLayout.MonitorSettings> collectMonitors() {
        try (MonitorEnumeration enumeration = new MonitorEnumeration()) {
            return enumeration.collect();
        }
    }

    /** The state {@code CountMonitorsCallback} and {@code CollectMonitorsCallback} kept in {@code g_MonitorInfos}. */
    private static final class MonitorEnumeration implements AutoCloseable {

        private static final MethodHandle COUNT_TARGET = target("onCount");
        private static final MethodHandle COLLECT_TARGET = target("onCollect");

        private final Arena arena = Arena.ofConfined();
        private final List<WinScreenLayout.MonitorSettings> monitors = new ArrayList<>();

        /** {@code g_MonitorInfos.maxInfos}: what the counting pass saw. */
        private int maxInfos;

        private Throwable pending;

        private static MethodHandle target(String name) {
            try {
                return MethodHandles.lookup().findVirtual(MonitorEnumeration.class, name,
                        MONITOR_ENUM_PROC_FD.toMethodType());
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        List<WinScreenLayout.MonitorSettings> collect() {
            maxInfos = 0;
            enumerate(COUNT_TARGET);
            enumerate(COLLECT_TARGET);
            return monitors;
        }

        private void enumerate(MethodHandle target) {
            MemorySegment proc = upcallStub(target.bindTo(this), MONITOR_ENUM_PROC_FD, arena);
            try {
                int ignoredResult = (int) ENUM_DISPLAY_MONITORS.invokeExact(MemorySegment.NULL, MemorySegment.NULL,
                        proc, 0L);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            Throwable failure = pending;
            if (failure != null) {
                throw unexpected(failure);
            }
        }

        /** {@code CountMonitorsCallback}. */
        private int onCount(MemorySegment hMonitor, MemorySegment hdc, MemorySegment clip, long data) {
            try {
                maxInfos++;
            } catch (Throwable t) {
                remember(t);
            }
            return 1;
        }

        /** {@code CollectMonitorsCallback}: {@code GetMonitorSettings} into the next slot, if there is one. */
        private int onCollect(MemorySegment hMonitor, MemorySegment hdc, MemorySegment clip, long data) {
            try {
                if (!hMonitor.equals(MemorySegment.NULL) && monitors.size() < maxInfos) {
                    monitors.add(monitorSettings(hMonitor));
                }
            } catch (Throwable t) {
                remember(t);
            }
            return 1;
        }

        /** The first failure of a pass is thrown when the pass has returned; later ones ride along with it. */
        private void remember(Throwable t) {
            if (pending == null) {
                pending = t;
            } else {
                pending.addSuppressed(t);
            }
        }

        @Override
        public void close() {
            arena.close();
        }
    }

    /**
     * {@code GetMonitorSettings(hMonitor, mis)}, statement for statement, returning what it wrote:
     * {@code GetMonitorInfoW} into a zeroed {@code MONITORINFOEXW} whose result is ignored (a stale handle
     * publishes zeros, as it did); a display DC for {@code szDevice}, whose NULL is not checked either
     * ({@code GetDeviceCaps(NULL, ...)} answers 0); the colour depth as {@code BITSPIXEL * PLANES}; then,
     * with {@code shcore}, the effective DPI - {@code LOGPIXELSX/Y} if that query fails - as the UI DPI,
     * and the raw DPI into the <b>same</b> two {@code UINT}s with its result ignored, so a failed raw query
     * leaves the effective values there; without {@code shcore}, {@code LOGPIXELSX/Y} for both.
     */
    private static WinScreenLayout.MonitorSettings monitorSettings(MemorySegment hMonitor) {
        loadDpiFuncs(PROCESS_PER_MONITOR_DPI_AWARE);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mix = arena.allocate(MONITORINFOEXW_LAYOUT);
            mix.set(JAVA_INT, MIX_CBSIZE_OFFSET, (int) MONITORINFOEXW_LAYOUT.byteSize());
            int ignoredInfoResult = (int) GET_MONITOR_INFO_W.invokeExact(hMonitor, mix);

            MemorySegment hDC = (MemorySegment) Gdi32.CREATE_DC_W.invokeExact(wideString(arena, "DISPLAY"),
                    mix.asSlice(MIX_DEVICE_OFFSET), MemorySegment.NULL, MemorySegment.NULL);

            boolean primary = (mix.get(JAVA_INT, MIX_FLAGS_OFFSET) & MONITORINFOF_PRIMARY) != 0;
            int colorDepth = deviceCaps(hDC, BITSPIXEL) * deviceCaps(hDC, PLANES);
            MemorySegment resx = arena.allocate(JAVA_INT, 2);
            MemorySegment resy = resx.asSlice(JAVA_INT.byteSize());
            int uiresx;
            int uiresy;
            if (Shcore.RESOLVED) {
                int res = (int) Shcore.GET_DPI_FOR_MONITOR.invokeExact(hMonitor, MDT_EFFECTIVE_DPI, resx, resy);
                if (res != S_OK) {
                    resx.set(JAVA_INT, 0, deviceCaps(hDC, LOGPIXELSX));
                    resy.set(JAVA_INT, 0, deviceCaps(hDC, LOGPIXELSY));
                }
                uiresx = resx.get(JAVA_INT, 0);
                uiresy = resy.get(JAVA_INT, 0);
                int ignoredRawResult = (int) Shcore.GET_DPI_FOR_MONITOR.invokeExact(hMonitor, MDT_RAW_DPI, resx, resy);
            } else {
                resx.set(JAVA_INT, 0, deviceCaps(hDC, LOGPIXELSX));
                resy.set(JAVA_INT, 0, deviceCaps(hDC, LOGPIXELSY));
                uiresx = resx.get(JAVA_INT, 0);
                uiresy = resy.get(JAVA_INT, 0);
            }
            int dpiX = resx.get(JAVA_INT, 0);
            int dpiY = resy.get(JAVA_INT, 0);

            int ignoredDeleteResult = (int) Gdi32.DELETE_DC.invokeExact(hDC);

            return new WinScreenLayout.MonitorSettings(hMonitor.address(),
                    mix.get(JAVA_INT, MIX_MONITOR_OFFSET + RECT_LEFT_OFFSET),
                    mix.get(JAVA_INT, MIX_MONITOR_OFFSET + RECT_TOP_OFFSET),
                    mix.get(JAVA_INT, MIX_MONITOR_OFFSET + RECT_RIGHT_OFFSET),
                    mix.get(JAVA_INT, MIX_MONITOR_OFFSET + RECT_BOTTOM_OFFSET),
                    mix.get(JAVA_INT, MIX_WORK_OFFSET + RECT_LEFT_OFFSET),
                    mix.get(JAVA_INT, MIX_WORK_OFFSET + RECT_TOP_OFFSET),
                    mix.get(JAVA_INT, MIX_WORK_OFFSET + RECT_RIGHT_OFFSET),
                    mix.get(JAVA_INT, MIX_WORK_OFFSET + RECT_BOTTOM_OFFSET),
                    primary, colorDepth, dpiX, dpiY, uiresx, uiresy);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code ::GetDeviceCaps(hDC, index)}. */
    private static int deviceCaps(MemorySegment hDC, int index) {
        try {
            return (int) Gdi32.GET_DEVICE_CAPS.invokeExact(hDC, index);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code GetSystemDirectoryW} into a {@code MAX_PATH}-character buffer: the directory, or {@code null}
     * when the call failed (0) or the buffer was too small (the required size, {@code >= MAX_PATH}).
     * The path is kept as code units ({@link #codeUnitsToNul}): {@code LoadDPIFuncs} appended to that
     * {@code wchar_t} buffer with {@code wcscat_s} and never decoded it.
     */
    private static String systemDirectory() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment path = arena.allocate(JAVA_CHAR, MAX_PATH);
            int length = (int) GET_SYSTEM_DIRECTORY_W.invokeExact(path, MAX_PATH);
            if (length == 0 || length >= MAX_PATH) {
                return null;
            }
            return codeUnitsToNul(path);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code void (*settings_changed)(void)}. Package-private: the shim builds its recording stub from it. */
    static final FunctionDescriptor SCREEN_SETTINGS_CHANGED_FD = FunctionDescriptor.ofVoid();

    private static boolean screenCallbacksInstalled;

    /** The stub {@link #installScreenCallbacks} created, kept for {@link #rewriteScreenCallbacks}; never replaced. */
    private static MemorySegment screenCallbackStub;

    /** {@code sizeof(GwinScreenCallbacks)} as the C compiler saw it; {@link #SCREEN_CALLBACKS_LAYOUT} must agree. */
    static int sizeOfScreenCallbacks() {
        try {
            return (int) GWIN_SIZEOF_SCREEN_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Hands {@code glass.dll} the table that replaces the {@code Screen.notifySettingsChanged()} upcall of
     * {@code GlassScreen::HandleDisplayChange} - {@code WM_DISPLAYCHANGE}, {@code WM_SETTINGCHANGE} with
     * {@code SPI_SETWORKAREA} and {@code WM_DPICHANGED}. Once a table is installed that function dials the
     * slot; while none is, it delivers nothing - the JNI path it took before the install has been
     * deleted.
     * <p>
     * <b>The install is a flip, and travelled with the flip of the enumeration.</b> It comes from
     * {@code WinApplication}'s static initializer, beside the other tables - the launcher thread, before
     * {@code Application.run()} creates the toolkit window, so no display change can arrive before it - and
     * landed in the change set that made {@code staticScreen_getScreens} Java. The JNI path it replaced
     * looked {@code Screen} up through the class loader {@code _setClassLoader} handed the C, which that same
     * change set deleted, so from then until that path was deleted a display change that found no table would have
     * reached Java through a NULL class; and installed lazily, one that arrives before the install is now
     * dropped.
     * <p>
     * The table is copied by the library; the stub is in {@link Arena#global()}, because no
     * {@code remove} exists and a {@code WM_DISPLAYCHANGE} can be dispatched until the last
     * {@code GetMessage}. Idempotent.
     */
    static synchronized void installScreenCallbacks() {
        if (screenCallbacksInstalled) {
            return;
        }
        screenCallbackStub = upcallStub(upcallTarget("onScreenSettingsChanged", SCREEN_SETTINGS_CHANGED_FD),
                SCREEN_SETTINGS_CHANGED_FD, Arena.global());
        writeScreenTable(screenCallbackStub);
        screenCallbacksInstalled = true;
    }

    /** Whether {@link #installScreenCallbacks} has run in this JVM. */
    static synchronized boolean screenCallbacksInstalled() {
        return screenCallbacksInstalled;
    }

    /**
     * Puts back what {@link #installScreenCallbacks} left in the library, after a test has installed a
     * table of its own: the production table if the install has run, no table if it has not.
     * {@code gwin_screen_set_callbacks} copies by value and the last call wins. Never installs and never
     * creates a second stub.
     */
    static synchronized void rewriteScreenCallbacks() {
        if (screenCallbacksInstalled) {
            writeScreenTable(screenCallbackStub);
        } else {
            try {
                requireOk((int) GWIN_SCREEN_SET_CALLBACKS.invokeExact(MemorySegment.NULL), "gwin_screen_set_callbacks");
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
    }

    private static void writeScreenTable(MemorySegment settingsChanged) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(SCREEN_CALLBACKS_LAYOUT);
            table.set(ADDRESS, SETTINGS_CHANGED_OFFSET, settingsChanged);
            requireOk((int) GWIN_SCREEN_SET_CALLBACKS.invokeExact(table), "gwin_screen_set_callbacks");
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The {@code GwinScreenCallbacks.settings_changed} body: what {@code HandleDisplayChange}'s JNI arm did
     * until it was deleted, {@code CallStaticVoidMethod(Screen.notifySettingsChanged)} and its
     * {@code CheckAndClearException}.
     * <p>
     * <b>Thread and re-entrancy.</b> The toolkit thread, inside {@code DispatchMessage} - and for
     * {@code WM_DPICHANGED} after {@code GlassWindow} has already moved the window. The body re-enumerates
     * the monitors from inside this upcall, replaces the {@code Screen} list and rebinds every window before
     * it returns, so nothing on the path may hold a lock that code below a {@code DispatchMessage} can want.
     * A pass that finds no monitor makes {@code Screen.initScreens} throw; that, like anything else, is
     * reported here and never thrown through the stub, which would end the JVM.
     */
    private static void onScreenSettingsChanged() {
        try {
            Screen.notifySettingsChanged();
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /* ==== Clipboard, drag-and-drop and the common dialogs ============================================ */

    /*
     * The sixteen upcall descriptors, one per slot typedef of GwinClipboardCallbacks and
     * GwinDndCallbacks in glass_win_api.h, in declaration order. Every int32_t is a JAVA_INT, every
     * int64_t id a JAVA_LONG, every pointer an ADDRESS; no slot returns a boolean. The shim builds its
     * recording stubs from these same objects, so what a through-C test proves is the facade's
     * descriptor and not a copy.
     */

    /**
     * {@code int32_t (*fos_serialize)(int64_t clipboard_id, const uint16_t* mime, int64_t lindex, uint8_t** out_data,
     * int32_t* out_len)}.
     */
    static final FunctionDescriptor CLIPBOARD_FOS_SERIALIZE_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS);
    /** {@code int32_t (*action_performed)(int64_t clipboard_id, int32_t action)}. */
    static final FunctionDescriptor CLIPBOARD_ACTION_PERFORMED_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT);
    /** {@code void (*drag_action_performed)(int64_t clipboard_id, int32_t action)}. */
    static final FunctionDescriptor CLIPBOARD_DRAG_ACTION_PERFORMED_FD =
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT);
    /** {@code void (*content_changed)(int64_t clipboard_id)}. */
    static final FunctionDescriptor CLIPBOARD_CONTENT_CHANGED_FD = FunctionDescriptor.ofVoid(JAVA_LONG);
    /** {@code void (*dispose_peer)(int64_t clipboard_id)}. */
    static final FunctionDescriptor CLIPBOARD_DISPOSE_PEER_FD = FunctionDescriptor.ofVoid(JAVA_LONG);
    /** {@code void (*set_data_object)(int64_t clipboard_id, void* data_object)}. */
    static final FunctionDescriptor CLIPBOARD_SET_DATA_OBJECT_FD = FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS);
    /** {@code void (*data_object_disposed)(int64_t clipboard_id)}. */
    static final FunctionDescriptor CLIPBOARD_DATA_OBJECT_DISPOSED_FD = FunctionDescriptor.ofVoid(JAVA_LONG);

    /**
     * {@code int32_t (*drag_enter)(int64_t view_id, int32_t x, int32_t y, int32_t x_abs, int32_t y_abs,
     * int32_t recommended_action, int32_t* out_action)}; {@code drag_over} and {@code drag_drop} are
     * declared with the identical prototype, so the three constants are one descriptor.
     */
    static final FunctionDescriptor DND_DRAG_ENTER_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);
    static final FunctionDescriptor DND_DRAG_OVER_FD = DND_DRAG_ENTER_FD;
    static final FunctionDescriptor DND_DRAG_DROP_FD = DND_DRAG_ENTER_FD;
    /** {@code int32_t (*drag_leave)(int64_t view_id)} - a status for a void Java target. */
    static final FunctionDescriptor DND_DRAG_LEAVE_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG);
    /** {@code int32_t (*dnd_get_data_object)(void** out_data_object)}. */
    static final FunctionDescriptor DND_GET_DATA_OBJECT_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS);
    /** {@code int32_t (*dnd_set_data_object)(void* data_object)}. */
    static final FunctionDescriptor DND_SET_DATA_OBJECT_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS);
    /** {@code int32_t (*dnd_set_source_supported_actions)(int32_t actions)}. */
    static final FunctionDescriptor DND_SET_SOURCE_SUPPORTED_ACTIONS_FD = FunctionDescriptor.of(JAVA_INT, JAVA_INT);
    /** {@code int32_t (*dnd_set_drag_button)(int32_t button)}. */
    static final FunctionDescriptor DND_SET_DRAG_BUTTON_FD = FunctionDescriptor.of(JAVA_INT, JAVA_INT);
    /** {@code int32_t (*dnd_get_drag_button)(int32_t* out_button)}. */
    static final FunctionDescriptor DND_GET_DRAG_BUTTON_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS);

    /**
     * The slot names of {@link #GWIN_CLIPBOARD_CALLBACKS_LAYOUT} in declaration order, the order the installer writes
     * them.
     */
    static final List<String> CLIPBOARD_SLOT_NAMES = List.of("fos_serialize", "action_performed",
            "drag_action_performed", "content_changed", "dispose_peer", "set_data_object", "data_object_disposed");

    /** The slot names of {@link #GWIN_DND_CALLBACKS_LAYOUT} in declaration order. */
    static final List<String> DND_SLOT_NAMES = List.of("drag_enter", "drag_over", "drag_drop", "drag_leave",
            "dnd_get_data_object", "dnd_set_data_object", "dnd_set_source_supported_actions",
            "dnd_set_drag_button", "dnd_get_drag_button");

    /** The target methods of this class, parallel to {@link #CLIPBOARD_SLOT_NAMES}. */
    private static final String[] CLIPBOARD_SLOT_TARGETS = {"onFosSerialize", "onActionPerformed",
            "onDragActionPerformed", "onContentChanged", "onDisposePeer", "onSetDataObject", "onDataObjectDisposed"};
    private static final FunctionDescriptor[] CLIPBOARD_SLOT_DESCRIPTORS = {CLIPBOARD_FOS_SERIALIZE_FD,
            CLIPBOARD_ACTION_PERFORMED_FD, CLIPBOARD_DRAG_ACTION_PERFORMED_FD, CLIPBOARD_CONTENT_CHANGED_FD,
            CLIPBOARD_DISPOSE_PEER_FD, CLIPBOARD_SET_DATA_OBJECT_FD, CLIPBOARD_DATA_OBJECT_DISPOSED_FD};

    /** The target methods of this class, parallel to {@link #DND_SLOT_NAMES}. */
    private static final String[] DND_SLOT_TARGETS = {"onDragEnter", "onDragOver", "onDragDrop", "onDragLeave",
            "onDndGetDataObject", "onDndSetDataObject", "onDndSetSourceSupportedActions", "onDndSetDragButton",
            "onDndGetDragButton"};
    private static final FunctionDescriptor[] DND_SLOT_DESCRIPTORS = {DND_DRAG_ENTER_FD, DND_DRAG_OVER_FD,
            DND_DRAG_DROP_FD, DND_DRAG_LEAVE_FD, DND_GET_DATA_OBJECT_FD, DND_SET_DATA_OBJECT_FD,
            DND_SET_SOURCE_SUPPORTED_ACTIONS_FD, DND_SET_DRAG_BUTTON_FD, DND_GET_DRAG_BUTTON_FD};

    private static boolean clipboardCallbacksInstalled;
    private static boolean dndCallbacksInstalled;

    /**
     * The seven and the nine stubs, created once in {@link Arena#global()} and kept so that the
     * reinstall methods can write the same addresses again. Never replaced: OLE may deliver a paste
     * for as long as the process owns a clipboard data object, and a drag from another application
     * can arrive at any window.
     */
    private static MemorySegment[] clipboardCallbackStubs;
    private static MemorySegment[] dndCallbackStubs;

    /**
     * {@code sizeof(GwinClipboardCallbacks)} as the C compiler saw it; {@link #GWIN_CLIPBOARD_CALLBACKS_LAYOUT} must
     * agree.
     */
    static int sizeOfClipboardCallbacks() {
        try {
            return (int) GWIN_SIZEOF_CLIPBOARD_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code sizeof(GwinDndCallbacks)}; {@link #GWIN_DND_CALLBACKS_LAYOUT} must agree. */
    static int sizeOfDndCallbacks() {
        try {
            return (int) GWIN_SIZEOF_DND_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code sizeof(GwinFileFilter)}; {@link #GWIN_FILE_FILTER_LAYOUT} must agree. */
    static int sizeOfFileFilter() {
        try {
            return (int) GWIN_SIZEOF_FILE_FILTER.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_test_fire_clipboard_callback}: drives slot {@code slot} of the installed clipboard
     * table with the header's fixed pattern and returns what the hook reports - a status slot's
     * status, 0 for a void slot, {@code fos_serialize}'s {@code *out_len} (or -100 for a NULL block),
     * {@link #GWIN_ERR_INVALID_ARG} for an unknown slot or an uninstalled table. A test hook.
     */
    static long testFireClipboardCallback(int slot, long clipboardId) {
        try {
            return (long) GWIN_TEST_FIRE_CLIPBOARD_CALLBACK.invokeExact(slot, clipboardId);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_test_fire_dnd_callback}: the drag-table twin; {@code out} is handed unchanged to
     * the slots with an out-parameter (8 bytes, 8-aligned, or {@code NULL} for the others).
     */
    static long testFireDndCallback(int slot, long viewId, MemorySegment out) {
        try {
            return (long) GWIN_TEST_FIRE_DND_CALLBACK.invokeExact(slot, viewId, out);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Hands {@code glass.dll} the table that replaces the three {@code Call*Method} sites of
     * {@code GlassClipboard.cpp}, the re-entered dispose export and the {@code WM_DRAWCLIPBOARD}
     * arm of {@code GlassApplication.cpp}, the {@code setPtr} field write and the
     * {@code DeleteGlobalRef} of {@code ~ClipboardData} ({@code GwinClipboardCallbacks}). Called from
     * <b>this class's own static initializer</b>, at its end, and nowhere else - not from
     * {@code WinApplication}'s as the view and window tables are, and not from a peer's. The reason
     * is the drag table it travels with (see {@link #installDndCallbacks}); the two are installed
     * together so that a {@code gwin_dnd_push}, which needs both, can never find one missing. The
     * install is the flip: without {@code set_data_object} a pushed object is unreachable and leaks,
     * so it travels in the same change set as {@code WinSystemClipboard}'s forwarders.
     * <p>
     * A slot left {@code NULL} would be replaced by an internal no-op in C - and for
     * {@code fos_serialize} that no-op serves nothing to anyone who pastes - so all seven are
     * written. The table is copied by the library; the confined arena that holds it is closed here,
     * the stubs live in {@link Arena#global()}.
     */
    static synchronized void installClipboardCallbacks() {
        if (clipboardCallbacksInstalled) {
            return;
        }
        clipboardCallbackStubs = stubsFor(CLIPBOARD_SLOT_TARGETS, CLIPBOARD_SLOT_DESCRIPTORS);
        writeClipboardTable();
        clipboardCallbacksInstalled = true;
    }

    /**
     * Hands {@code glass.dll} the table that replaces the eight {@code Call*Method} sites of
     * {@code GlassDnD.cpp} ({@code GwinDndCallbacks}): the four {@code View.notifyDrag*} of the drop
     * target, keyed by {@code WinView}'s view id, and the five {@code WinDnDClipboard} members the C reached
     * through {@code ClassForName} + {@code getInstance}. Called from <b>this class's own static
     * initializer</b>. The JNI's {@code ClassForName(name, initialize = true)} on the first drag
     * forced {@code WinDnDClipboard}'s initializer to run; a table installed from that initializer
     * would therefore never be installed for a drop from Explorer into a process that has not
     * started a drag of its own - {@code IDropTarget::DragEnter} would find no table and every drop
     * would silently do nothing. Installing from the facade's initializer, which runs before any
     * Glass peer can exist, closes that window, and installing from {@code WinApplication}'s would
     * merely move the dependency one class over.
     * <p>
     * No slot is left {@code NULL}: the C's no-op for the five singleton slots would leave the drag
     * clipboard without its data object. The table is copied by the library; the stubs live in
     * {@link Arena#global()}.
     */
    static synchronized void installDndCallbacks() {
        if (dndCallbacksInstalled) {
            return;
        }
        dndCallbackStubs = stubsFor(DND_SLOT_TARGETS, DND_SLOT_DESCRIPTORS);
        writeDndTable();
        dndCallbacksInstalled = true;
    }

    static synchronized boolean clipboardCallbacksInstalled() {
        return clipboardCallbacksInstalled;
    }

    static synchronized boolean dndCallbacksInstalled() {
        return dndCallbacksInstalled;
    }

    /**
     * Writes the production clipboard table again with the stubs {@link #installClipboardCallbacks}
     * created; {@code gwin_clipboard_set_callbacks} copies by value and the last call wins, so this
     * restores after a test has installed a table of its own. Never creates a second set of stubs.
     */
    static synchronized void reinstallClipboardCallbacks() {
        if (!clipboardCallbacksInstalled) {
            installClipboardCallbacks();
            return;
        }
        writeClipboardTable();
    }

    /** The drag-table twin of {@link #reinstallClipboardCallbacks}. */
    static synchronized void reinstallDndCallbacks() {
        if (!dndCallbacksInstalled) {
            installDndCallbacks();
            return;
        }
        writeDndTable();
    }

    /** The seven installed stubs in slot order, for the test that reads them back; empty before the install. */
    static synchronized List<MemorySegment> installedClipboardCallbackStubs() {
        return clipboardCallbacksInstalled ? List.of(clipboardCallbackStubs) : List.of();
    }

    /** The nine installed stubs in slot order; empty before the install. */
    static synchronized List<MemorySegment> installedDndCallbackStubs() {
        return dndCallbacksInstalled ? List.of(dndCallbackStubs) : List.of();
    }

    private static void writeClipboardTable() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(GWIN_CLIPBOARD_CALLBACKS_LAYOUT);
            fillCallbackTable(table, GWIN_CLIPBOARD_CALLBACKS_LAYOUT, CLIPBOARD_SLOT_NAMES, clipboardCallbackStubs);
            requireOk((int) GWIN_CLIPBOARD_SET_CALLBACKS.invokeExact(table), "gwin_clipboard_set_callbacks");
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    private static void writeDndTable() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(GWIN_DND_CALLBACKS_LAYOUT);
            fillCallbackTable(table, GWIN_DND_CALLBACKS_LAYOUT, DND_SLOT_NAMES, dndCallbackStubs);
            requireOk((int) GWIN_DND_SET_CALLBACKS.invokeExact(table), "gwin_dnd_set_callbacks");
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /*
     * The sixteen targets. As for the view and window sections the marshalling is here and the
     * registry lookup and the peer call are the peer's (WinSystemClipboard for the seven clipboard
     * slots, WinDnDClipboard for the five singleton slots, WinView for the four drag slots - the
     * View.notifyDrag* they reach are protected members of another package). Each catches Throwable,
     * and here that is not merely about the JVM: an escaping Throwable would unwind through a COM
     * vtable frame (ClipboardData::GetData, GlassDropTarget::Drop) into the OLE marshaller and, for a
     * paste or a drop from another application, into that application. The sink is the one the JNI
     * used at each site: describeUpcallFailure (checkJavaException: ExceptionDescribe + clear, no
     * Java code runs) for the slots that answer a status the C maps to E_JAVAEXCEPTION and for
     * data_object_disposed (which fires inside ~ClipboardData, possibly on a foreign OLE frame);
     * reportUpcallFailure (CheckAndClearException: the application's UncaughtExceptionHandler) where
     * the JNI did that - drag_action_performed, content_changed, dispose_peer, set_data_object. Thread:
     * always the JavaFX application thread, nested in a downcall or with no Java frame below; nothing
     * here takes a lock, and no target may call gwin_dnd_push or a gwin_dialog_* function. An id the
     * registry does not know is a stale peer: the slot default, silently. Package-private so that
     * WinGlassNativeShim can drive every arm without a table.
     */

    /**
     * {@code fos_serialize}: the delayed render of one format. The bytes Java produces are copied into
     * a {@link #gwinAlloc} block the C owns from the moment this returns and frees on every path; a
     * null {@code byte[]} is {@code NULL} with length 0 and {@link #GWIN_OK} (the C answers
     * {@code E_POINTER}); an empty one is a real one-byte block with length 0. On a throw nothing
     * is written and nothing is owned. Re-entrant: an IE shortcut renders {@code text/uri-list} and
     * {@code message/external-body} from inside its own {@code GetData}.
     */
    static int onFosSerialize(long clipboardId, MemorySegment mime, long lindex, MemorySegment outData,
                              MemorySegment outLen) {
        try {
            byte[] bytes = WinSystemClipboard.dispatchFosSerialize(clipboardId, cString(mime), lindex);
            MemorySegment block = MemorySegment.NULL;
            int length = 0;
            if (bytes != null) {
                block = gwinAlloc(bytes.length);
                if (block.equals(MemorySegment.NULL)) {
                    throw new OutOfMemoryError("gwin_alloc(" + bytes.length + ") returned NULL");
                }
                MemorySegment.copy(bytes, 0, bounded(block, bytes.length), JAVA_BYTE, 0, bytes.length);
                length = bytes.length;
            }
            bounded(outData, ADDRESS.byteSize()).set(ADDRESS, 0, block);
            bounded(outLen, JAVA_INT.byteSize()).set(JAVA_INT, 0, length);
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /**
     * {@code action_performed}: {@code Clipboard.actionPerformed(int)} from {@code IDataObject::SetData},
     * cross-process.
     */
    static int onActionPerformed(long clipboardId, int action) {
        try {
            WinSystemClipboard.dispatchActionPerformed(clipboardId, action);
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code drag_action_performed}: the same method from the tail of {@code gwin_dnd_push}; result ignored. */
    static void onDragActionPerformed(long clipboardId, int action) {
        try {
            WinSystemClipboard.dispatchActionPerformed(clipboardId, action);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /**
     * {@code content_changed}: {@code Clipboard.contentChanged()} from {@code WM_DRAWCLIPBOARD}; may be nested in a
     * push.
     */
    static void onContentChanged(long clipboardId) {
        try {
            WinSystemClipboard.dispatchContentChanged(clipboardId);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code dispose_peer}: the previously registered peer's dispose, from {@code RegisterClipboardViewer}. */
    static void onDisposePeer(long clipboardId) {
        try {
            WinSystemClipboard.dispatchDisposePeer(clipboardId);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code set_data_object}: the JNI's {@code setPtr}, before anything in the push can fail. */
    static void onSetDataObject(long clipboardId, MemorySegment dataObject) {
        try {
            WinSystemClipboard.dispatchSetDataObject(clipboardId, dataObject);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code data_object_disposed}: {@code ~ClipboardData}; touches the registry and nothing else. */
    static void onDataObjectDisposed(long clipboardId) {
        try {
            WinSystemClipboard.dispatchDataObjectDisposed(clipboardId);
        } catch (Throwable t) {
            describeUpcallFailure(t);
        }
    }

    /**
     * {@code drag_enter}: {@code View.notifyDragEnter}. The view's answer is written to
     * {@code *out_action}; for an unknown id nothing is written and {@link #GWIN_OK} is answered, so
     * the C's own default ({@code DROPEFFECT_NONE}) stands; on a throw nothing is written either,
     * and the C writes {@code *pdwEffect} from its 0 before testing the status, as the JNI did.
     */
    static int onDragEnter(long viewId, int x, int y, int xAbs, int yAbs, int recommendedAction,
                           MemorySegment outAction) {
        try {
            writeAction(outAction, WinView.dispatchDragEnter(viewId, x, y, xAbs, yAbs, recommendedAction));
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code drag_over}: {@code View.notifyDragOver}; per mouse move during a drag. */
    static int onDragOver(long viewId, int x, int y, int xAbs, int yAbs, int recommendedAction,
                          MemorySegment outAction) {
        try {
            writeAction(outAction, WinView.dispatchDragOver(viewId, x, y, xAbs, yAbs, recommendedAction));
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code drag_drop}: {@code View.notifyDragDrop}. */
    static int onDragDrop(long viewId, int x, int y, int xAbs, int yAbs, int recommendedAction,
                          MemorySegment outAction) {
        try {
            writeAction(outAction, WinView.dispatchDragDrop(viewId, x, y, xAbs, yAbs, recommendedAction));
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code drag_leave}: {@code View.notifyDragLeave()}, void; the slot answers a status only. */
    static int onDragLeave(long viewId) {
        try {
            boolean ignoredHadView = WinView.dispatchDragLeave(viewId);
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /**
     * {@code dnd_get_data_object}: the drag clipboard's handle, through {@code getInstance()} (which may create it).
     */
    static int onDndGetDataObject(MemorySegment outDataObject) {
        try {
            MemorySegment dataObject = WinDnDClipboard.dispatchGetDataObject();
            bounded(outDataObject, ADDRESS.byteSize()).set(ADDRESS, 0, dataObject);
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code dnd_set_data_object}: the object being dragged in, AddRef'd by the C. */
    static int onDndSetDataObject(MemorySegment dataObject) {
        try {
            WinDnDClipboard.dispatchSetDataObject(dataObject);
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code dnd_set_source_supported_actions}: before every drag notification; the C discards the status. */
    static int onDndSetSourceSupportedActions(int actions) {
        try {
            WinDnDClipboard.dispatchSetSourceSupportedActions(actions);
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code dnd_set_drag_button}: 0 from the tail of {@code gwin_dnd_push}; the C discards the status. */
    static int onDndSetDragButton(int button) {
        try {
            WinDnDClipboard.dispatchSetDragButton(button);
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code dnd_get_drag_button}: for the {@code GlassDropSource} constructor; on a throw the C uses 0. */
    static int onDndGetDragButton(MemorySegment outButton) {
        try {
            int button = WinDnDClipboard.dispatchGetDragButton();
            bounded(outButton, JAVA_INT.byteSize()).set(JAVA_INT, 0, button);
            return GWIN_OK;
        } catch (Throwable t) {
            describeUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** Writes a drag answer through the out-pointer, or nothing for {@code null} (no view). */
    private static void writeAction(MemorySegment outAction, Integer action) {
        if (action != null) {
            bounded(outAction, JAVA_INT.byteSize()).set(JAVA_INT, 0, action);
        }
    }

    /**
     * The {@code checkJavaException} sink ({@code OleUtils.h}): {@code ExceptionDescribe} then
     * {@code ExceptionClear} - a stack trace on {@code System.err}, and no Java code of the
     * application runs. Used by every slot whose status the C maps to {@code E_JAVAEXCEPTION},
     * because those fire on OLE frames where an application handler that opened a dialog would nest
     * a modal loop inside a COM callback. Cannot itself throw.
     */
    private static void describeUpcallFailure(Throwable t) {
        try {
            t.printStackTrace();
        } catch (Throwable ignored) {
            // Nothing left to try: this is an upcall, and letting anything out of it kills the JVM.
        }
    }

    /* -- strings and blocks ----------------------------------------------------------------------- */

    /**
     * A NUL-terminated UTF-16 string the C handed an upcall, copied. The pointer arrives with no
     * length, so the one {@code reinterpret} is to the unbounded size the FFM API documents for
     * exactly this case and the walk stops at the first NUL.
     */
    private static String cString(MemorySegment pointer) {
        return readStringBlock(pointer, 1)[0];
    }

    /**
     * Reads a string block - {@code count} NUL-terminated UTF-16 strings laid end to end - into Java
     * strings, copied. The block's byte length is not known up front (only its string count is), so
     * the single {@code reinterpret} in this section is to an unbounded size and the walk is bounded
     * by the NULs the C wrote; an empty string in the middle is one NUL and does not truncate the
     * list, which is why the count is passed and never derived.
     */
    static String[] readStringBlock(MemorySegment block, int count) {
        MemorySegment chars = bounded(block, Long.MAX_VALUE);
        String[] strings = new String[Math.max(count, 0)];
        long index = 0;
        for (int i = 0; i < strings.length; i++) {
            long start = index;
            while (chars.getAtIndex(JAVA_CHAR, index) != '\0') {
                index++;
            }
            strings[i] = new String(chars.asSlice(start * JAVA_CHAR.byteSize(), (index - start) * JAVA_CHAR.byteSize())
                    .toArray(JAVA_CHAR));
            index++;
        }
        return strings;
    }

    /**
     * A string as the C will read it out of a block: up to its first U+0000. {@code GwinNextString}
     * ({@code GlassStringBlock.h}) walks a block with {@code wcslen}, so a NUL inside a string would end
     * that string there and make its tail the <em>next</em> string, shifting every later entry by one
     * and dropping the last off the count: {@code "x\0y"} + {@code "text/plain"} with count 2 would
     * register "x" and "y" and never advertise {@code text/plain}. Cutting is parity, not sanitising:
     * under JNI each key crossed as its own {@code JString} and the C read it with {@code wcscmp} /
     * {@code RegisterClipboardFormat}, which stop at the first NUL of that key alone - so the JNI implementation
     * in commit {@code 8492cb03b0} advertised "x" and "text/plain", and so does the block.
     */
    private static String upToNul(String string) {
        int nul = string.indexOf('\0');
        return nul < 0 ? string : string.substring(0, nul);
    }

    /**
     * The UTF-16 code units a string block of {@code strings} occupies, including every NUL and the
     * extra one; each string counts {@link #upToNul up to its first NUL}, as it is written.
     */
    static long stringBlockUnits(String[] strings) {
        long units = 1;
        for (String string : strings) {
            units += upToNul(string).length() + 1L;
        }
        return units;
    }

    /**
     * Writes {@code strings} as a string block into {@code block}, which must hold
     * {@link #stringBlockUnits} code units: each string {@link #upToNul up to its first NUL}, its NUL,
     * and one extra NUL at the end. Strings cross as UTF-16 code units, so a surrogate pair is two
     * units and an unpaired surrogate is one, exactly as {@code GetStringRegion} passed them.
     */
    static void writeStringBlock(MemorySegment block, String[] strings) {
        long index = 0;
        for (String string : strings) {
            char[] chars = upToNul(string).toCharArray();
            MemorySegment.copy(chars, 0, block, JAVA_CHAR, index * JAVA_CHAR.byteSize(), chars.length);
            index += chars.length;
            block.setAtIndex(JAVA_CHAR, index, '\0');
            index++;
        }
        block.setAtIndex(JAVA_CHAR, index, '\0');
    }

    /** A string block of {@code strings} in {@code arena}; a block of one NUL for no strings, never NULL. */
    static MemorySegment allocStringBlock(Arena arena, String[] strings) {
        MemorySegment block = arena.allocate(JAVA_CHAR, stringBlockUnits(strings));
        writeStringBlock(block, strings);
        return block;
    }

    /**
     * The mime names {@code push} receives: an {@code Object[]} from {@code Set.toArray()}, cast
     * element by element. The C cast blindly ({@code JString(env, (jstring) element)}), so a
     * non-String element was undefined behaviour there and is a {@code ClassCastException} here.
     */
    private static String[] mimeNames(Object[] keys) {
        String[] mimes = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            mimes[i] = (String) keys[i];
        }
        return mimes;
    }

    /**
     * {@code GwinFileFilter[descriptions.length]} in {@code arena}, or {@code NULL} for a null array.
     * The two are distinct to the C: a NULL pointer skips the filter setup entirely, a non-NULL
     * pointer with a count of 0 still runs {@code SetDefaultExtension(L"")} and
     * {@code SetFileTypes(0, ...)}, which is what stops the shell appending an extension to a typed
     * SAVE name - so an empty array is a real (one-entry-sized, zero-count) block. A null description
     * or extension string becomes {@code ""}; the C's {@code JString} would have crashed on it.
     */
    static MemorySegment allocFileFilters(Arena arena, String[] descriptions, String[] extensions) {
        if (descriptions == null) {
            return MemorySegment.NULL;
        }
        long entries = Math.max(descriptions.length, 1);
        MemorySegment filters = arena.allocate(GWIN_FILE_FILTER_LAYOUT.byteSize() * entries,
                GWIN_FILE_FILTER_LAYOUT.byteAlignment());
        long descriptionOffset = GWIN_FILE_FILTER_LAYOUT.byteOffset(PathElement.groupElement("description"));
        long extensionsOffset = GWIN_FILE_FILTER_LAYOUT.byteOffset(PathElement.groupElement("extensions"));
        for (int i = 0; i < descriptions.length; i++) {
            long base = i * GWIN_FILE_FILTER_LAYOUT.byteSize();
            filters.set(ADDRESS, base + descriptionOffset, wideString(arena, dialogString(descriptions[i])));
            filters.set(ADDRESS, base + extensionsOffset, wideString(arena, dialogString(extensions[i])));
        }
        return filters;
    }

    /**
     * A dialog string is never NULL and possibly empty: the JNI always allocated a {@code JString},
     * so {@code SetFolder} / {@code SetTitle} always ran ({@code SetTitle(L"")} gives an empty title
     * bar, where NULL would give the shell's default). {@code CommonDialogs} already substitutes
     * {@code ""} for a null folder and title; this covers the filename and the filter strings too.
     */
    static String dialogString(String text) {
        return text == null ? "" : text;
    }

    /* -- the downcalls ---------------------------------------------------------------------------- */

    /**
     * {@code gwin_alloc(size)}: a block the C can free; NULL only when the heap is exhausted. Called from inside {@code
     * fos_serialize}.
     */
    static MemorySegment gwinAlloc(long size) {
        try {
            return (MemorySegment) GWIN_ALLOC.invokeExact(size);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_free(block)}: every out-block this class reads is released here, exactly once; NULL is a no-op. */
    static void gwinFree(MemorySegment block) {
        try {
            GWIN_FREE.invokeExact(block);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_clipboard_register_viewer}: {@code WinSystemClipboard.create}. Disposes the
     * previously registered peer through {@code dispose_peer} first, then joins the viewer chain.
     * {@link #GWIN_ERR_NO_TOOLKIT} when there is no {@code GlassApplication}.
     */
    static int clipboardRegisterViewer(long clipboardId) {
        try {
            return (int) GWIN_CLIPBOARD_REGISTER_VIEWER.invokeExact(clipboardId);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_clipboard_dispose}: unregister, flush if current, release; pumps and re-enters. The caller nulls its
     * handle.
     */
    static void clipboardDispose(MemorySegment dataObject) {
        try {
            GWIN_CLIPBOARD_DISPOSE.invokeExact(dataObject);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_clipboard_push}: {@code WinSystemClipboard.push(Object[] keys, int supportedActions)}.
     * The keys are marshalled as a string block in the order {@code Set.toArray()} gave them (the
     * order drives {@code FORMATETC} insertion); the block is borrowed for the call and copied by
     * {@code pushCommit}. Returns the status; the handle arrives through {@code set_data_object}.
     */
    static int clipboardPush(MemorySegment old, long clipboardId, Object[] keys, int supportedActions) {
        String[] mimes = mimeNames(keys);
        try (Arena arena = Arena.ofConfined()) {
            return (int) GWIN_CLIPBOARD_PUSH.invokeExact(old, clipboardId, allocStringBlock(arena, mimes),
                    mimes.length, supportedActions);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_clipboard_pop}: releases {@code old}, then {@code OleGetClipboard}; the new handle, or NULL on
     * failure.
     */
    static MemorySegment clipboardPop(MemorySegment old) {
        try {
            return (MemorySegment) GWIN_CLIPBOARD_POP.invokeExact(old);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_clipboard_pop_bytes}: the bytes of one format, copied out of the library's block,
     * which is freed here; {@code null} for no data, an empty medium and an OLE failure alike. The
     * status is ignored - the JNI's null {@code byte[]} covered every failure and the caller's
     * fallbacks depend on that.
     */
    static byte[] clipboardPopBytes(MemorySegment dataObject, String mime, long lindex) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outData = arena.allocate(ADDRESS);
            MemorySegment outLen = arena.allocate(JAVA_INT);
            int ignoredStatus = (int) GWIN_CLIPBOARD_POP_BYTES.invokeExact(dataObject, wideString(arena, mime), lindex,
                    outData, outLen);
            MemorySegment block = outData.get(ADDRESS, 0);
            if (block.equals(MemorySegment.NULL)) {
                return null;
            }
            try {
                return bounded(block, outLen.get(JAVA_INT, 0)).toArray(JAVA_BYTE);
            } finally {
                gwinFree(block);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_clipboard_pop_mimes}: the mimes the data object advertises, copied out of the
     * library's string block, which is freed here; {@code null} when there is no data object or the
     * set came out empty. Iteration order was never specified on either side.
     */
    static String[] clipboardPopMimes(MemorySegment dataObject) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outMimes = arena.allocate(ADDRESS);
            MemorySegment outCount = arena.allocate(JAVA_INT);
            int ignoredStatus = (int) GWIN_CLIPBOARD_POP_MIMES.invokeExact(dataObject, outMimes, outCount);
            MemorySegment block = outMimes.get(ADDRESS, 0);
            if (block.equals(MemorySegment.NULL)) {
                return null;
            }
            try {
                return readStringBlock(block, outCount.get(JAVA_INT, 0));
            } finally {
                gwinFree(block);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_clipboard_push_target_action}: {@code CFSTR_PASTESUCCEEDED} + {@code CFSTR_PERFORMEDDROPEFFECT};
     * silent on failure.
     */
    static void clipboardPushTargetAction(MemorySegment dataObject, int actionDone) {
        try {
            GWIN_CLIPBOARD_PUSH_TARGET_ACTION.invokeExact(dataObject, actionDone);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_clipboard_pop_supported_actions}: {@code ACTION_NONE} for NULL, {@code ACTION_ANY} when unset, else
     * the stored set.
     */
    static int clipboardPopSupportedActions(MemorySegment dataObject) {
        try {
            return (int) GWIN_CLIPBOARD_POP_SUPPORTED_ACTIONS.invokeExact(dataObject);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_dnd_push}: {@code WinDnDClipboard.push}. Same marshalling as
     * {@link #clipboardPush}; blocks for the whole drag, with every slot of both tables able to fire
     * nested inside, then fires {@code drag_action_performed} and {@code dnd_set_drag_button(0)}.
     */
    static int dndPush(MemorySegment old, long clipboardId, Object[] keys, int supportedActions) {
        String[] mimes = mimeNames(keys);
        try (Arena arena = Arena.ofConfined()) {
            return (int) GWIN_DND_PUSH.invokeExact(old, clipboardId, allocStringBlock(arena, mimes), mimes.length,
                    supportedActions);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code gwin_dnd_dispose}: {@code Release} and nothing else. */
    static void dndDispose(MemorySegment dataObject) {
        try {
            GWIN_DND_DISPOSE.invokeExact(dataObject);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code WinSystemClipboard.isOwner}: {@code false} for a NULL handle <em>without calling OLE</em>
     * - the JNI body's first test ({@code GlassClipboard.cpp}, {@code if (NULL == p) return false})
     * - else {@code OleIsCurrentClipboard(handle) == S_OK}.
     */
    static boolean oleIsCurrentClipboard(MemorySegment dataObject) {
        if (dataObject.equals(MemorySegment.NULL)) {
            return false;
        }
        try {
            return (int) OLE_IS_CURRENT_CLIPBOARD.invokeExact(dataObject) == 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * What {@code gwin_dialog_file} answered: {@code files} is {@code null} when the paths could not
     * be read (the caller returns a null result), empty for cancel, else the full paths;
     * {@code filterIndex} is the C's {@code (1-based selected index) - 1}, untouched - {@code -1}
     * when the query failed.
     */
    record FileDialogResult(String[] files, int filterIndex) {
    }

    /**
     * {@code gwin_dialog_file}: {@code WinCommonDialogs._showFileChooser}. A modal dialog on this
     * thread; the owner is an HWND handle value, never dereferenced here. Strings are never NULL
     * ({@link #dialogString}), the filters are {@code NULL} for a null array and a real block for an
     * empty one ({@link #allocFileFilters}), and the out-block is freed here.
     */
    static FileDialogResult dialogFile(long owner, String folder, String filename, String title, int type,
                                       boolean multiple, String[] filterDescriptions, String[] filterExtensions,
                                       int defaultFilterIndex) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment filters = allocFileFilters(arena, filterDescriptions, filterExtensions);
            int filterCount = filterDescriptions == null ? 0 : filterDescriptions.length;
            MemorySegment outFiles = arena.allocate(ADDRESS);
            MemorySegment outCount = arena.allocate(JAVA_INT);
            MemorySegment outFilterIndex = arena.allocate(JAVA_INT);
            int ignoredStatus = (int) GWIN_DIALOG_FILE.invokeExact(MemorySegment.ofAddress(owner),
                    wideString(arena, dialogString(folder)), wideString(arena, dialogString(filename)),
                    wideString(arena, dialogString(title)), type, multiple ? 1 : 0, filters, filterCount,
                    defaultFilterIndex, outFiles, outCount, outFilterIndex);
            int filterIndex = outFilterIndex.get(JAVA_INT, 0);
            MemorySegment block = outFiles.get(ADDRESS, 0);
            if (block.equals(MemorySegment.NULL)) {
                return new FileDialogResult(null, filterIndex);
            }
            try {
                return new FileDialogResult(readStringBlock(block, outCount.get(JAVA_INT, 0)), filterIndex);
            } finally {
                gwinFree(block);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_dialog_folder}: {@code WinCommonDialogs._showFolderChooser}. {@code null} when the
     * user cancelled or the path could not be resolved, else the path; the out-block is freed here.
     */
    static String dialogFolder(long owner, String folder, String title) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outPath = arena.allocate(ADDRESS);
            int ignoredStatus = (int) GWIN_DIALOG_FOLDER.invokeExact(MemorySegment.ofAddress(owner),
                    wideString(arena, dialogString(folder)), wideString(arena, dialogString(title)), outPath);
            MemorySegment block = outPath.get(ADDRESS, 0);
            if (block.equals(MemorySegment.NULL)) {
                return null;
            }
            try {
                return readStringBlock(block, 1)[0];
            } finally {
                gwinFree(block);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /*
     * The install of the two tables above, from THIS class's initializer (the header's
     * installer comment): after every handle and descriptor they need, before any peer can exist.
     * Last in the class so that nothing below it could be read uninitialised by a stub target.
     */
    static {
        installClipboardCallbacks();
        installDndCallbacks();
    }

    /* ==== Accessibility: the two UI Automation provider objects ====================================== */

    /*
     * The 87 Call*Method sites of GlassAccessible.cpp and GlassTextRangeProvider.cpp, plus the two
     * DeleteGlobalRef points of their destructors, as two callback tables; the four entry points that
     * owned the COM objects' lifetime as four downcalls; UiaRaiseAutomationPropertyChangedEvent as one
     * more, because its two VARIANTs need the copyVariant marshalling the inbound direction keeps in C
     * anyway; and UiaRaiseAutomationEvent / UiaClientsAreListening bound straight from
     * UIAutomationCore.dll, because a C wrapper around a one-line OS call is what this migration removes.
     * WinAccessible._initIDs and WinTextRangeProvider._initIDs are gone: everything they cached is a
     * table slot or an argument now, and the library looks nothing up - which is the point, since
     * FindClass cannot see a javafx.graphics class from inside a downcall (WinDowncallExceptionReporting-
     * Test names that regression).
     *
     * Identity is the int64_t id the peer registered itself under, never WinAccessible.id or
     * WinTextRangeProvider.id - those two are part of the UIA runtime id a client can see. The handles
     * that cross as long are raw C++ object pointers, not ids.
     *
     * Every slot returns int32_t GwinStatus even where its Java target is void, because the JNI turned a
     * pending Throwable into E_FAIL for those too. GWIN_ERR_UPCALL is that E_FAIL, and a stub that
     * reports it writes no out-parameter: the library pre-zeroes them, which is exactly what
     * CallXxxMethod left behind when it returned with an exception pending.
     *
     * Thread: the JavaFX application thread, inside the gwin_run_loop downcall's message pump, except
     * advise_event_added / advise_event_removed and the two disposal slots, which Windows can dial on a
     * COM/RPC thread. That is a stated behaviour difference of this flip: the JNI arm answered E_FAIL on
     * a thread the JVM had never seen, because GetEnv() returned NULL and the bodies gave up rather than
     * attaching; an FFM upcall stub attaches the caller and runs the Java target.
     *
     * No Linker.Option#critical anywhere here: gwin_a11y_raise_property_changed calls across an
     * apartment into UIAutomationCore and can block, and both destroy calls run a destructor that dials a
     * callback slot and therefore re-enters the JVM.
     */

    /** {@code int32_t (*)(int64_t id)} - a slot whose Java target takes nothing and returns nothing. */
    static final FunctionDescriptor A11Y_ACT_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG);

    /** {@code int32_t (*)(int64_t id, int32_t)}. */
    static final FunctionDescriptor A11Y_ACT_I_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT);

    /** {@code int32_t (*)(int64_t id, int32_t, int32_t)}. */
    static final FunctionDescriptor A11Y_ACT_II_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT);

    /** {@code int32_t (*)(int64_t id, double)}. */
    static final FunctionDescriptor A11Y_ACT_D_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_DOUBLE);

    /** {@code int32_t (*)(int64_t id, double, double)}. */
    static final FunctionDescriptor A11Y_ACT_DD_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE);

    /** {@code int32_t (*)(int64_t id, int32_t, int64_t)} - the two advise slots' raw {@code SAFEARRAY*}. */
    static final FunctionDescriptor A11Y_ACT_IJ_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_LONG);

    /** {@code int32_t (*)(int64_t id, int32_t, int64_t other_id, int32_t)} - {@code move_endpoint_by_range}. */
    static final FunctionDescriptor A11Y_ACT_IJI_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_INT);

    /** {@code int32_t (*)(int64_t id, const uint16_t* text, int32_t len)} - borrowed UTF-16 code units. */
    static final FunctionDescriptor A11Y_ACT_TEXT_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT);

    /** {@code int32_t (*)(int64_t id, T* out)} - one scalar or one {@code GwinVariant} out-parameter. */
    static final FunctionDescriptor A11Y_OUT_FD = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS);

    /** {@code int32_t (*)(int64_t id, T** out, int32_t* out_count)} - a block and its count. */
    static final FunctionDescriptor A11Y_OUT2_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, ADDRESS);

    /** {@code int32_t (*)(int64_t id, int32_t, T* out)}. */
    static final FunctionDescriptor A11Y_I_OUT_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS);

    /** {@code int32_t (*)(int64_t id, int32_t, T** out, int32_t* out_count)} - {@code get_text}. */
    static final FunctionDescriptor A11Y_I_OUT2_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, ADDRESS);

    /** {@code int32_t (*)(int64_t id, int64_t, T* out)} - a raw pointer or another provider's id in. */
    static final FunctionDescriptor A11Y_J_OUT_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, ADDRESS);

    /** {@code int32_t (*)(int64_t id, double, double, T* out)}. */
    static final FunctionDescriptor A11Y_DD_OUT_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE, ADDRESS);

    /** {@code int32_t (*)(int64_t id, int32_t, int32_t, T* out)}. */
    static final FunctionDescriptor A11Y_II_OUT_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS);

    /** {@code int32_t (*)(int64_t id, int32_t, int32_t, int32_t, T* out)} - {@code move_endpoint_by_unit}. */
    static final FunctionDescriptor A11Y_III_OUT_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);

    /** {@code int32_t (*)(int64_t id, int32_t, int64_t other_id, int32_t, T* out)} - {@code compare_endpoints}. */
    static final FunctionDescriptor A11Y_IJI_OUT_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS);

    /**
     * {@code int32_t (*)(int64_t id, int32_t, const GwinVariant*, int32_t, int64_t* out)} -
     * {@code find_attribute}, whose {@code GwinVariant*} the library always passes as {@code NULL}.
     */
    static final FunctionDescriptor A11Y_IVB_OUT_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);

    /**
     * {@code int32_t (*)(int64_t id, const uint16_t* text, int32_t len, int32_t, int32_t, int64_t* out)} -
     * {@code find_text}.
     */
    static final FunctionDescriptor A11Y_FIND_TEXT_FD =
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);

    /**
     * {@code void (*)(int64_t id)} - the two disposal slots, which stand where {@code DeleteGlobalRef}
     * stood and are the only place a registry entry may be dropped. Void, and they must not throw.
     */
    static final FunctionDescriptor A11Y_DISPOSED_FD = FunctionDescriptor.ofVoid(JAVA_LONG);

    /**
     * The slot order of {@link #GWIN_ACCESSIBLE_CALLBACKS_LAYOUT}: the declaration order of
     * {@code GwinAccessibleCallbacks}, and the numbering {@code gwin_test_fire_accessible_callback}
     * uses.
     */
    static final List<String> ACCESSIBLE_SLOT_NAMES = List.of("get_pattern_provider",
            "get_host_raw_element_provider", "get_property_value", "get_bounding_rectangle", "get_fragment_root",
            "get_embedded_fragment_roots", "get_runtime_id", "navigate", "set_focus", "element_provider_from_point",
            "get_focus", "advise_event_added", "advise_event_removed", "invoke", "get_selection",
            "get_can_select_multiple", "get_is_selection_required", "select", "add_to_selection",
            "remove_from_selection", "get_is_selected", "get_selection_container", "set_value", "get_value",
            "get_is_read_only", "get_maximum", "get_minimum", "get_large_change", "get_small_change",
            "set_value_string", "get_value_string", "get_visible_ranges", "range_from_child", "range_from_point",
            "get_document_range", "get_supported_text_selection", "get_column_count", "get_row_count", "get_item",
            "get_column", "get_column_span", "get_containing_grid", "get_row", "get_row_span", "get_column_headers",
            "get_row_headers", "get_row_or_column_major", "get_column_header_items", "get_row_header_items",
            "toggle", "get_toggle_state", "collapse", "expand", "get_expand_collapse_state", "get_can_move",
            "get_can_resize", "get_can_rotate", "move", "resize", "rotate", "scroll", "set_scroll_percent",
            "get_horizontally_scrollable", "get_horizontal_scroll_percent", "get_horizontal_view_size",
            "get_vertically_scrollable", "get_vertical_scroll_percent", "get_vertical_view_size",
            "scroll_into_view", "accessible_disposed");

    /** The target methods of this class, parallel to {@link #ACCESSIBLE_SLOT_NAMES}. */
    private static final List<String> ACCESSIBLE_SLOT_TARGETS = List.of("onGetPatternProvider",
            "onGetHostRawElementProvider", "onGetPropertyValue", "onGetBoundingRectangle", "onGetFragmentRoot",
            "onGetEmbeddedFragmentRoots", "onGetRuntimeId", "onNavigate", "onSetFocus",
            "onElementProviderFromPoint", "onGetFocus", "onAdviseEventAdded", "onAdviseEventRemoved", "onInvoke",
            "onGetSelection", "onGetCanSelectMultiple", "onGetIsSelectionRequired", "onSelect", "onAddToSelection",
            "onRemoveFromSelection", "onGetIsSelected", "onGetSelectionContainer", "onSetValue", "onGetValue",
            "onGetIsReadOnly", "onGetMaximum", "onGetMinimum", "onGetLargeChange", "onGetSmallChange",
            "onSetValueString", "onGetValueString", "onGetVisibleRanges", "onRangeFromChild", "onRangeFromPoint",
            "onGetDocumentRange", "onGetSupportedTextSelection", "onGetColumnCount", "onGetRowCount", "onGetItem",
            "onGetColumn", "onGetColumnSpan", "onGetContainingGrid", "onGetRow", "onGetRowSpan",
            "onGetColumnHeaders", "onGetRowHeaders", "onGetRowOrColumnMajor", "onGetColumnHeaderItems",
            "onGetRowHeaderItems", "onToggle", "onGetToggleState", "onCollapse", "onExpand",
            "onGetExpandCollapseState", "onGetCanMove", "onGetCanResize", "onGetCanRotate", "onMove", "onResize",
            "onRotate", "onScroll", "onSetScrollPercent", "onGetHorizontallyScrollable",
            "onGetHorizontalScrollPercent", "onGetHorizontalViewSize", "onGetVerticallyScrollable",
            "onGetVerticalScrollPercent", "onGetVerticalViewSize", "onScrollIntoView", "onAccessibleDisposed");

    /**
     * The descriptors, parallel to {@link #ACCESSIBLE_SLOT_NAMES}. Package-private because
     * {@code WinGlassNativeShim} builds its recording table from these very constants, so that what a
     * fire hook proves is the production descriptor and not a copy of it.
     */
    static final List<FunctionDescriptor> ACCESSIBLE_SLOT_DESCRIPTORS = List.of(A11Y_I_OUT_FD, A11Y_OUT_FD,
            A11Y_I_OUT_FD, A11Y_OUT2_FD, A11Y_OUT_FD, A11Y_OUT2_FD, A11Y_OUT2_FD, A11Y_I_OUT_FD, A11Y_ACT_FD,
            A11Y_DD_OUT_FD, A11Y_OUT_FD, A11Y_ACT_IJ_FD, A11Y_ACT_IJ_FD, A11Y_ACT_FD, A11Y_OUT2_FD, A11Y_OUT_FD,
            A11Y_OUT_FD, A11Y_ACT_FD, A11Y_ACT_FD, A11Y_ACT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_ACT_D_FD,
            A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_ACT_TEXT_FD,
            A11Y_OUT2_FD, A11Y_OUT2_FD, A11Y_J_OUT_FD, A11Y_DD_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD,
            A11Y_OUT_FD, A11Y_II_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD,
            A11Y_OUT2_FD, A11Y_OUT2_FD, A11Y_OUT_FD, A11Y_OUT2_FD, A11Y_OUT2_FD, A11Y_ACT_FD, A11Y_OUT_FD,
            A11Y_ACT_FD, A11Y_ACT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_ACT_DD_FD,
            A11Y_ACT_DD_FD, A11Y_ACT_D_FD, A11Y_ACT_II_FD, A11Y_ACT_DD_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD,
            A11Y_OUT_FD, A11Y_OUT_FD, A11Y_OUT_FD, A11Y_ACT_FD, A11Y_DISPOSED_FD);

    /**
     * The slot order of {@link #GWIN_TEXT_RANGE_CALLBACKS_LAYOUT}: the declaration order of
     * {@code GwinTextRangeCallbacks}, and the numbering {@code gwin_test_fire_text_range_callback}
     * uses.
     */
    static final List<String> TEXT_RANGE_SLOT_NAMES = List.of("clone", "compare", "compare_endpoints",
            "expand_to_enclosing_unit", "find_attribute", "find_text", "get_attribute_value",
            "get_bounding_rectangles", "get_enclosing_element", "get_text", "move", "move_endpoint_by_unit",
            "move_endpoint_by_range", "select", "add_to_selection", "remove_from_selection", "scroll_into_view",
            "get_children", "range_disposed");

    /** The target methods of this class, parallel to {@link #TEXT_RANGE_SLOT_NAMES}. */
    private static final List<String> TEXT_RANGE_SLOT_TARGETS = List.of("onRangeClone", "onRangeCompare",
            "onRangeCompareEndpoints", "onRangeExpandToEnclosingUnit", "onRangeFindAttribute", "onRangeFindText",
            "onRangeGetAttributeValue", "onRangeGetBoundingRectangles", "onRangeGetEnclosingElement",
            "onRangeGetText", "onRangeMove", "onRangeMoveEndpointByUnit", "onRangeMoveEndpointByRange",
            "onRangeSelect", "onRangeAddToSelection", "onRangeRemoveFromSelection", "onRangeScrollIntoView",
            "onRangeGetChildren", "onRangeDisposed");

    /**
     * The descriptors, parallel to {@link #TEXT_RANGE_SLOT_NAMES}. Package-private because
     * {@code WinGlassNativeShim} builds its recording table from these very constants, so that what a
     * fire hook proves is the production descriptor and not a copy of it.
     */
    static final List<FunctionDescriptor> TEXT_RANGE_SLOT_DESCRIPTORS = List.of(A11Y_OUT_FD, A11Y_J_OUT_FD,
            A11Y_IJI_OUT_FD, A11Y_ACT_I_FD, A11Y_IVB_OUT_FD, A11Y_FIND_TEXT_FD, A11Y_I_OUT_FD, A11Y_OUT2_FD,
            A11Y_OUT_FD, A11Y_I_OUT2_FD, A11Y_II_OUT_FD, A11Y_III_OUT_FD, A11Y_ACT_IJI_FD, A11Y_ACT_FD, A11Y_ACT_FD,
            A11Y_ACT_FD, A11Y_ACT_I_FD, A11Y_OUT2_FD, A11Y_DISPOSED_FD);

    private static final long VARIANT_VT_OFFSET = variantOffset("vt");
    private static final long VARIANT_I_VAL_OFFSET = variantOffset("i_val");
    private static final long VARIANT_L_VAL_OFFSET = variantOffset("l_val");
    private static final long VARIANT_FLT_VAL_OFFSET = variantOffset("flt_val");
    private static final long VARIANT_DBL_VAL_OFFSET = variantOffset("dbl_val");
    private static final long VARIANT_BOOL_VAL_OFFSET = variantOffset("bool_val");
    private static final long VARIANT_PUNK_VAL_OFFSET = variantOffset("punk_val");
    private static final long VARIANT_BSTR_VAL_OFFSET = variantOffset("bstr_val");
    private static final long VARIANT_BSTR_LEN_OFFSET = variantOffset("bstr_len");
    private static final long VARIANT_P_DBL_VAL_OFFSET = variantOffset("p_dbl_val");
    private static final long VARIANT_P_DBL_COUNT_OFFSET = variantOffset("p_dbl_count");

    private static long variantOffset(String field) {
        return GWIN_VARIANT_LAYOUT.byteOffset(PathElement.groupElement(field));
    }

    private static boolean accessibilityCallbacksInstalled;

    /**
     * The 70 and the 19 stubs, created once by {@link #installAccessibilityCallbacks} in
     * {@link Arena#global()} and kept so that {@link #reinstallAccessibilityCallbacks} can write the same
     * addresses again. Never replaced: a stub the library has been given must outlive the process,
     * because there is no point at which {@code glass.dll} can promise that no further UI Automation
     * call will arrive, and one can arrive on a thread the JVM has never seen.
     */
    private static MemorySegment[] accessibleCallbackStubs;
    private static MemorySegment[] textRangeCallbackStubs;

    /** {@code sizeof(GwinAccessibleCallbacks)}; {@link #GWIN_ACCESSIBLE_CALLBACKS_LAYOUT} must agree. */
    static int sizeOfAccessibleCallbacks() {
        try {
            return (int) GWIN_SIZEOF_ACCESSIBLE_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code sizeof(GwinTextRangeCallbacks)}; {@link #GWIN_TEXT_RANGE_CALLBACKS_LAYOUT} must agree. */
    static int sizeOfTextRangeCallbacks() {
        try {
            return (int) GWIN_SIZEOF_TEXT_RANGE_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code sizeof(GwinVariant)}; {@link #GWIN_VARIANT_LAYOUT} must agree, field for field. */
    static int sizeOfVariant() {
        try {
            return (int) GWIN_SIZEOF_VARIANT.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Hands {@code glass.dll} the two tables that replaced the 87 {@code Call*Method} sites of
     * {@code GlassAccessible.cpp} and {@code GlassTextRangeProvider.cpp} and the {@code DeleteGlobalRef}
     * of their two destructors.
     * <p>
     * <b>Called from the static initializer of {@code WinAccessible} and of
     * {@code WinTextRangeProvider}</b> - where {@code _initIDs} stood, and the only two places a provider
     * can come into existence, so both tables are installed before the first {@code gwin_a11y_create}
     * whichever class initializes first. Both go in together because the library chooses its arm per
     * table: the install is the flip that starts passing ids, and the two arms must never be mixed.
     * Idempotent and synchronized, because these two initializers can run on different threads.
     * <p>
     * No table-level {@code void* user}: the identity is per provider, not per table - every slot's
     * first parameter is the {@code int64_t} id the peer registered itself under. The tables are copied
     * by the library, so the confined arena that holds them is closed here; the stubs are in
     * {@link Arena#global()}.
     */
    static synchronized void installAccessibilityCallbacks() {
        if (accessibilityCallbacksInstalled) {
            return;
        }
        accessibleCallbackStubs = stubsFor(ACCESSIBLE_SLOT_TARGETS, ACCESSIBLE_SLOT_DESCRIPTORS);
        textRangeCallbackStubs = stubsFor(TEXT_RANGE_SLOT_TARGETS, TEXT_RANGE_SLOT_DESCRIPTORS);
        writeAccessibilityTables();
        accessibilityCallbacksInstalled = true;
    }

    /** Whether {@link #installAccessibilityCallbacks} has run in this JVM. */
    static synchronized boolean accessibilityCallbacksInstalled() {
        return accessibilityCallbacksInstalled;
    }

    /**
     * Writes the production tables again, with the stubs {@link #installAccessibilityCallbacks} created.
     * The one caller is {@code WinGlassNativeShim}, after it has installed a recording table to drive the
     * two fire hooks through the facade's own descriptors: both installers copy by value and the last
     * call wins, so this is the restore. Installs first if nothing has been installed yet; never creates
     * a second set of stubs.
     */
    static synchronized void reinstallAccessibilityCallbacks() {
        if (!accessibilityCallbacksInstalled) {
            installAccessibilityCallbacks();
            return;
        }
        writeAccessibilityTables();
    }

    /** The 89 installed stubs, accessible slots then text-range slots, for the test that reads them back. */
    static synchronized List<MemorySegment> installedAccessibilityCallbackStubs() {
        List<MemorySegment> stubs = new ArrayList<>(ACCESSIBLE_SLOT_NAMES.size() + TEXT_RANGE_SLOT_NAMES.size());
        if (accessibilityCallbacksInstalled) {
            Collections.addAll(stubs, accessibleCallbackStubs);
            Collections.addAll(stubs, textRangeCallbackStubs);
        }
        return Collections.unmodifiableList(stubs);
    }

    private static void writeAccessibilityTables() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment accessibleTable = arena.allocate(GWIN_ACCESSIBLE_CALLBACKS_LAYOUT);
            fillCallbackTable(accessibleTable, GWIN_ACCESSIBLE_CALLBACKS_LAYOUT, ACCESSIBLE_SLOT_NAMES,
                    accessibleCallbackStubs);
            requireOk((int) GWIN_A11Y_SET_CALLBACKS.invokeExact(accessibleTable), "gwin_a11y_set_callbacks");
            MemorySegment textRangeTable = arena.allocate(GWIN_TEXT_RANGE_CALLBACKS_LAYOUT);
            fillCallbackTable(textRangeTable, GWIN_TEXT_RANGE_CALLBACKS_LAYOUT, TEXT_RANGE_SLOT_NAMES,
                    textRangeCallbackStubs);
            requireOk((int) GWIN_A11Y_TEXT_RANGE_SET_CALLBACKS.invokeExact(textRangeTable),
                    "gwin_a11y_text_range_set_callbacks");
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@link #stubsFor(String[], FunctionDescriptor[])} over the two {@code List}-shaped slot tables. */
    private static MemorySegment[] stubsFor(List<String> targets, List<FunctionDescriptor> descriptors) {
        MemorySegment[] stubs = new MemorySegment[targets.size()];
        for (int i = 0; i < stubs.length; i++) {
            FunctionDescriptor descriptor = descriptors.get(i);
            stubs[i] = upcallStub(upcallTarget(targets.get(i), descriptor), descriptor, Arena.global());
        }
        return stubs;
    }

    /*
     * The 89 targets. Each does the marshalling its slot needs and nothing else - the registry lookup
     * and the call are WinAccessible's resp. WinTextRangeProvider's - and each catches Throwable,
     * because letting anything out of an upcall stub terminates the JVM (measured on JDK 26 and JDK 25:
     * "Unrecoverable uncaught exception encountered. The VM will now exit"). Reporting through
     * reportUpcallFailure is CheckAndClearException (the former GlassAccessibleJni.cpp:90-129): report to
     * Application.reportException on this thread, swallow, carry on - and a reported Throwable becomes
     * GWIN_ERR_UPCALL, which the provider method turns into the E_FAIL the JNI returned. A stub that
     * reports writes no out-parameter: the library pre-zeroes them, which is what CallXxxMethod left
     * behind when it returned with an exception pending. Booleans cross as int32_t 0 / 1 and are tested
     * with != 0, never == 1. Package-private so that WinGlassNativeShim can drive every arm without a
     * table.
     */

    /** {@code get_pattern_provider}: {@code GetPatternProvider(int)}. */
    static int onGetPatternProvider(long accessibleId, int patternId, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchGetPatternProvider(accessibleId, patternId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_host_raw_element_provider}: {@code get_HostRawElementProvider()}. */
    static int onGetHostRawElementProvider(long accessibleId, MemorySegment outHwnd) {
        try {
            return outLong(outHwnd, WinAccessible.dispatchGetHostRawElementProvider(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_property_value}: {@code GetPropertyValue(int)}. */
    static int onGetPropertyValue(long accessibleId, int propertyId, MemorySegment out) {
        try {
            return outVariant(out, WinAccessible.dispatchGetPropertyValue(accessibleId, propertyId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_bounding_rectangle}: {@code get_BoundingRectangle()}. */
    static int onGetBoundingRectangle(long accessibleId, MemorySegment out4, MemorySegment outWritten) {
        try {
            return outRectangle(out4, outWritten, WinAccessible.dispatchGetBoundingRectangle(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_fragment_root}: {@code get_FragmentRoot()}. */
    static int onGetFragmentRoot(long accessibleId, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchGetFragmentRoot(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_embedded_fragment_roots}: {@code GetEmbeddedFragmentRoots()}. */
    static int onGetEmbeddedFragmentRoots(long accessibleId, MemorySegment out, MemorySegment outCount) {
        try {
            return outLongBlock(out, outCount, WinAccessible.dispatchGetEmbeddedFragmentRoots(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_runtime_id}: {@code GetRuntimeId()}. */
    static int onGetRuntimeId(long accessibleId, MemorySegment out, MemorySegment outCount) {
        try {
            return outIntBlock(out, outCount, WinAccessible.dispatchGetRuntimeId(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code navigate}: {@code Navigate(int)}. */
    static int onNavigate(long accessibleId, int direction, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchNavigate(accessibleId, direction));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code set_focus}: {@code SetFocus()}. */
    static int onSetFocus(long accessibleId) {
        try {
            WinAccessible.dispatchSetFocus(accessibleId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code element_provider_from_point}: {@code ElementProviderFromPoint(double,double)}. */
    static int onElementProviderFromPoint(long accessibleId, double x, double y, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchElementProviderFromPoint(accessibleId, x, y));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_focus}: {@code GetFocus()}. */
    static int onGetFocus(long accessibleId, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchGetFocus(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code advise_event_added}: {@code AdviseEventAdded(int,long)}. */
    static int onAdviseEventAdded(long accessibleId, int eventId, long propertyIds) {
        try {
            WinAccessible.dispatchAdviseEventAdded(accessibleId, eventId, propertyIds);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code advise_event_removed}: {@code AdviseEventRemoved(int,long)}. */
    static int onAdviseEventRemoved(long accessibleId, int eventId, long propertyIds) {
        try {
            WinAccessible.dispatchAdviseEventRemoved(accessibleId, eventId, propertyIds);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code invoke}: {@code Invoke()}. */
    static int onInvoke(long accessibleId) {
        try {
            WinAccessible.dispatchInvoke(accessibleId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_selection}: {@code GetSelection()}. */
    static int onGetSelection(long accessibleId, MemorySegment out, MemorySegment outCount) {
        try {
            return outLongBlock(out, outCount, WinAccessible.dispatchGetSelection(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_can_select_multiple}: {@code get_CanSelectMultiple()}. */
    static int onGetCanSelectMultiple(long accessibleId, MemorySegment out) {
        try {
            return outBoolean(out, WinAccessible.dispatchGetCanSelectMultiple(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_is_selection_required}: {@code get_IsSelectionRequired()}. */
    static int onGetIsSelectionRequired(long accessibleId, MemorySegment out) {
        try {
            return outBoolean(out, WinAccessible.dispatchGetIsSelectionRequired(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code select}: {@code Select()}. */
    static int onSelect(long accessibleId) {
        try {
            WinAccessible.dispatchSelect(accessibleId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code add_to_selection}: {@code AddToSelection()}. */
    static int onAddToSelection(long accessibleId) {
        try {
            WinAccessible.dispatchAddToSelection(accessibleId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code remove_from_selection}: {@code RemoveFromSelection()}. */
    static int onRemoveFromSelection(long accessibleId) {
        try {
            WinAccessible.dispatchRemoveFromSelection(accessibleId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_is_selected}: {@code get_IsSelected()}. */
    static int onGetIsSelected(long accessibleId, MemorySegment out) {
        try {
            return outBoolean(out, WinAccessible.dispatchGetIsSelected(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_selection_container}: {@code get_SelectionContainer()}. */
    static int onGetSelectionContainer(long accessibleId, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchGetSelectionContainer(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code set_value}: {@code SetValue(double)}. */
    static int onSetValue(long accessibleId, double value) {
        try {
            WinAccessible.dispatchSetValue(accessibleId, value);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_value}: {@code get_Value()}. */
    static int onGetValue(long accessibleId, MemorySegment out) {
        try {
            return outDouble(out, WinAccessible.dispatchGetValue(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_is_read_only}: {@code get_IsReadOnly()}. */
    static int onGetIsReadOnly(long accessibleId, MemorySegment out) {
        try {
            return outBoolean(out, WinAccessible.dispatchGetIsReadOnly(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_maximum}: {@code get_Maximum()}. */
    static int onGetMaximum(long accessibleId, MemorySegment out) {
        try {
            return outDouble(out, WinAccessible.dispatchGetMaximum(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_minimum}: {@code get_Minimum()}. */
    static int onGetMinimum(long accessibleId, MemorySegment out) {
        try {
            return outDouble(out, WinAccessible.dispatchGetMinimum(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_large_change}: {@code get_LargeChange()}. */
    static int onGetLargeChange(long accessibleId, MemorySegment out) {
        try {
            return outDouble(out, WinAccessible.dispatchGetLargeChange(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_small_change}: {@code get_SmallChange()}. */
    static int onGetSmallChange(long accessibleId, MemorySegment out) {
        try {
            return outDouble(out, WinAccessible.dispatchGetSmallChange(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code set_value_string}: {@code SetValueString(String)}. */
    static int onSetValueString(long accessibleId, MemorySegment text, int length) {
        try {
            WinAccessible.dispatchSetValueString(accessibleId, utf16String(text, length));
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_value_string}: {@code get_ValueString()}. */
    static int onGetValueString(long accessibleId, MemorySegment out, MemorySegment outLength) {
        try {
            return outString(out, outLength, WinAccessible.dispatchGetValueString(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_visible_ranges}: {@code GetVisibleRanges()}. */
    static int onGetVisibleRanges(long accessibleId, MemorySegment out, MemorySegment outCount) {
        try {
            return outLongBlock(out, outCount, WinAccessible.dispatchGetVisibleRanges(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code range_from_child}: {@code RangeFromChild(long)}. */
    static int onRangeFromChild(long accessibleId, long childElement, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchRangeFromChild(accessibleId, childElement));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code range_from_point}: {@code RangeFromPoint(double,double)}. */
    static int onRangeFromPoint(long accessibleId, double x, double y, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchRangeFromPoint(accessibleId, x, y));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_document_range}: {@code get_DocumentRange()}. */
    static int onGetDocumentRange(long accessibleId, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchGetDocumentRange(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_supported_text_selection}: {@code get_SupportedTextSelection()}. */
    static int onGetSupportedTextSelection(long accessibleId, MemorySegment out) {
        try {
            return outInt(out, WinAccessible.dispatchGetSupportedTextSelection(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_column_count}: {@code get_ColumnCount()}. */
    static int onGetColumnCount(long accessibleId, MemorySegment out) {
        try {
            return outInt(out, WinAccessible.dispatchGetColumnCount(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_row_count}: {@code get_RowCount()}. */
    static int onGetRowCount(long accessibleId, MemorySegment out) {
        try {
            return outInt(out, WinAccessible.dispatchGetRowCount(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_item}: {@code GetItem(int,int)}. */
    static int onGetItem(long accessibleId, int row, int column, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchGetItem(accessibleId, row, column));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_column}: {@code get_Column()}. */
    static int onGetColumn(long accessibleId, MemorySegment out) {
        try {
            return outInt(out, WinAccessible.dispatchGetColumn(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_column_span}: {@code get_ColumnSpan()}. */
    static int onGetColumnSpan(long accessibleId, MemorySegment out) {
        try {
            return outInt(out, WinAccessible.dispatchGetColumnSpan(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_containing_grid}: {@code get_ContainingGrid()}. */
    static int onGetContainingGrid(long accessibleId, MemorySegment out) {
        try {
            return outLong(out, WinAccessible.dispatchGetContainingGrid(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_row}: {@code get_Row()}. */
    static int onGetRow(long accessibleId, MemorySegment out) {
        try {
            return outInt(out, WinAccessible.dispatchGetRow(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_row_span}: {@code get_RowSpan()}. */
    static int onGetRowSpan(long accessibleId, MemorySegment out) {
        try {
            return outInt(out, WinAccessible.dispatchGetRowSpan(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_column_headers}: {@code GetColumnHeaders()}. */
    static int onGetColumnHeaders(long accessibleId, MemorySegment out, MemorySegment outCount) {
        try {
            return outLongBlock(out, outCount, WinAccessible.dispatchGetColumnHeaders(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_row_headers}: {@code GetRowHeaders()}. */
    static int onGetRowHeaders(long accessibleId, MemorySegment out, MemorySegment outCount) {
        try {
            return outLongBlock(out, outCount, WinAccessible.dispatchGetRowHeaders(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_row_or_column_major}: {@code get_RowOrColumnMajor()}. */
    static int onGetRowOrColumnMajor(long accessibleId, MemorySegment out) {
        try {
            return outInt(out, WinAccessible.dispatchGetRowOrColumnMajor(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_column_header_items}: {@code GetColumnHeaderItems()}. */
    static int onGetColumnHeaderItems(long accessibleId, MemorySegment out, MemorySegment outCount) {
        try {
            return outLongBlock(out, outCount, WinAccessible.dispatchGetColumnHeaderItems(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_row_header_items}: {@code GetRowHeaderItems()}. */
    static int onGetRowHeaderItems(long accessibleId, MemorySegment out, MemorySegment outCount) {
        try {
            return outLongBlock(out, outCount, WinAccessible.dispatchGetRowHeaderItems(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code toggle}: {@code Toggle()}. */
    static int onToggle(long accessibleId) {
        try {
            WinAccessible.dispatchToggle(accessibleId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_toggle_state}: {@code get_ToggleState()}. */
    static int onGetToggleState(long accessibleId, MemorySegment out) {
        try {
            return outInt(out, WinAccessible.dispatchGetToggleState(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code collapse}: {@code Collapse()}. */
    static int onCollapse(long accessibleId) {
        try {
            WinAccessible.dispatchCollapse(accessibleId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code expand}: {@code Expand()}. */
    static int onExpand(long accessibleId) {
        try {
            WinAccessible.dispatchExpand(accessibleId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_expand_collapse_state}: {@code get_ExpandCollapseState()}. */
    static int onGetExpandCollapseState(long accessibleId, MemorySegment out) {
        try {
            return outInt(out, WinAccessible.dispatchGetExpandCollapseState(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_can_move}: {@code get_CanMove()}. */
    static int onGetCanMove(long accessibleId, MemorySegment out) {
        try {
            return outBoolean(out, WinAccessible.dispatchGetCanMove(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_can_resize}: {@code get_CanResize()}. */
    static int onGetCanResize(long accessibleId, MemorySegment out) {
        try {
            return outBoolean(out, WinAccessible.dispatchGetCanResize(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_can_rotate}: {@code get_CanRotate()}. */
    static int onGetCanRotate(long accessibleId, MemorySegment out) {
        try {
            return outBoolean(out, WinAccessible.dispatchGetCanRotate(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code move}: {@code Move(double,double)}. */
    static int onMove(long accessibleId, double x, double y) {
        try {
            WinAccessible.dispatchMove(accessibleId, x, y);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code resize}: {@code Resize(double,double)}. */
    static int onResize(long accessibleId, double width, double height) {
        try {
            WinAccessible.dispatchResize(accessibleId, width, height);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code rotate}: {@code Rotate(double)}. */
    static int onRotate(long accessibleId, double degrees) {
        try {
            WinAccessible.dispatchRotate(accessibleId, degrees);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code scroll}: {@code Scroll(int,int)}. */
    static int onScroll(long accessibleId, int horizontalAmount, int verticalAmount) {
        try {
            WinAccessible.dispatchScroll(accessibleId, horizontalAmount, verticalAmount);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code set_scroll_percent}: {@code SetScrollPercent(double,double)}. */
    static int onSetScrollPercent(long accessibleId, double horizontalPercent, double verticalPercent) {
        try {
            WinAccessible.dispatchSetScrollPercent(accessibleId, horizontalPercent, verticalPercent);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_horizontally_scrollable}: {@code get_HorizontallyScrollable()}. */
    static int onGetHorizontallyScrollable(long accessibleId, MemorySegment out) {
        try {
            return outBoolean(out, WinAccessible.dispatchGetHorizontallyScrollable(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_horizontal_scroll_percent}: {@code get_HorizontalScrollPercent()}. */
    static int onGetHorizontalScrollPercent(long accessibleId, MemorySegment out) {
        try {
            return outDouble(out, WinAccessible.dispatchGetHorizontalScrollPercent(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_horizontal_view_size}: {@code get_HorizontalViewSize()}. */
    static int onGetHorizontalViewSize(long accessibleId, MemorySegment out) {
        try {
            return outDouble(out, WinAccessible.dispatchGetHorizontalViewSize(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_vertically_scrollable}: {@code get_VerticallyScrollable()}. */
    static int onGetVerticallyScrollable(long accessibleId, MemorySegment out) {
        try {
            return outBoolean(out, WinAccessible.dispatchGetVerticallyScrollable(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_vertical_scroll_percent}: {@code get_VerticalScrollPercent()}. */
    static int onGetVerticalScrollPercent(long accessibleId, MemorySegment out) {
        try {
            return outDouble(out, WinAccessible.dispatchGetVerticalScrollPercent(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_vertical_view_size}: {@code get_VerticalViewSize()}. */
    static int onGetVerticalViewSize(long accessibleId, MemorySegment out) {
        try {
            return outDouble(out, WinAccessible.dispatchGetVerticalViewSize(accessibleId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code scroll_into_view}: {@code ScrollIntoView()}. */
    static int onScrollIntoView(long accessibleId) {
        try {
            WinAccessible.dispatchScrollIntoView(accessibleId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code clone}: {@code Clone()}. */
    static int onRangeClone(long rangeId, MemorySegment out) {
        try {
            return outLong(out, WinTextRangeProvider.dispatchClone(rangeId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code compare}: {@code Compare(WinTextRangeProvider)}. */
    static int onRangeCompare(long rangeId, long otherRangeId, MemorySegment out) {
        try {
            return outBoolean(out, WinTextRangeProvider.dispatchCompare(rangeId, otherRangeId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code compare_endpoints}: {@code CompareEndpoints(int,WinTextRangeProvider,int)}. */
    static int onRangeCompareEndpoints(long rangeId, int endpoint, long otherRangeId, int targetEndpoint,
            MemorySegment out) {
        try {
            return outInt(out, WinTextRangeProvider.dispatchCompareEndpoints(rangeId, endpoint, otherRangeId,
                    targetEndpoint));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code expand_to_enclosing_unit}: {@code ExpandToEnclosingUnit(int)}. */
    static int onRangeExpandToEnclosingUnit(long rangeId, int unit) {
        try {
            WinTextRangeProvider.dispatchExpandToEnclosingUnit(rangeId, unit);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code find_attribute}: {@code FindAttribute(int,WinVariant,boolean)}. */
    static int onRangeFindAttribute(long rangeId, int attributeId, MemorySegment value, int backward,
            MemorySegment out) {
        try {
            return outLong(out, WinTextRangeProvider.dispatchFindAttribute(rangeId, attributeId, readVariant(value),
                    backward != 0));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code find_text}: {@code FindText(String,boolean,boolean)}. */
    static int onRangeFindText(long rangeId, MemorySegment text, int length, int backward, int ignoreCase,
            MemorySegment out) {
        try {
            return outLong(out, WinTextRangeProvider.dispatchFindText(rangeId, utf16String(text, length),
                    backward != 0, ignoreCase != 0));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_attribute_value}: {@code GetAttributeValue(int)}. */
    static int onRangeGetAttributeValue(long rangeId, int attributeId, MemorySegment out) {
        try {
            return outVariant(out, WinTextRangeProvider.dispatchGetAttributeValue(rangeId, attributeId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_bounding_rectangles}: {@code GetBoundingRectangles()}. */
    static int onRangeGetBoundingRectangles(long rangeId, MemorySegment out, MemorySegment outCount) {
        try {
            return outDoubleBlock(out, outCount, WinTextRangeProvider.dispatchGetBoundingRectangles(rangeId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_enclosing_element}: {@code GetEnclosingElement()}. */
    static int onRangeGetEnclosingElement(long rangeId, MemorySegment out) {
        try {
            return outLong(out, WinTextRangeProvider.dispatchGetEnclosingElement(rangeId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_text}: {@code GetText(int)}. */
    static int onRangeGetText(long rangeId, int maxLength, MemorySegment out, MemorySegment outLength) {
        try {
            return outString(out, outLength, WinTextRangeProvider.dispatchGetText(rangeId, maxLength));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code move}: {@code Move(int,int)}. */
    static int onRangeMove(long rangeId, int unit, int count, MemorySegment out) {
        try {
            return outInt(out, WinTextRangeProvider.dispatchMove(rangeId, unit, count));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code move_endpoint_by_unit}: {@code MoveEndpointByUnit(int,int,int)}. */
    static int onRangeMoveEndpointByUnit(long rangeId, int endpoint, int unit, int count, MemorySegment out) {
        try {
            return outInt(out, WinTextRangeProvider.dispatchMoveEndpointByUnit(rangeId, endpoint, unit, count));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code move_endpoint_by_range}: {@code MoveEndpointByRange(int,WinTextRangeProvider,int)}. */
    static int onRangeMoveEndpointByRange(long rangeId, int endpoint, long otherRangeId, int targetEndpoint) {
        try {
            WinTextRangeProvider.dispatchMoveEndpointByRange(rangeId, endpoint, otherRangeId, targetEndpoint);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code select}: {@code Select()}. */
    static int onRangeSelect(long rangeId) {
        try {
            WinTextRangeProvider.dispatchSelect(rangeId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code add_to_selection}: {@code AddToSelection()}. */
    static int onRangeAddToSelection(long rangeId) {
        try {
            WinTextRangeProvider.dispatchAddToSelection(rangeId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code remove_from_selection}: {@code RemoveFromSelection()}. */
    static int onRangeRemoveFromSelection(long rangeId) {
        try {
            WinTextRangeProvider.dispatchRemoveFromSelection(rangeId);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code scroll_into_view}: {@code ScrollIntoView(boolean)}. */
    static int onRangeScrollIntoView(long rangeId, int alignToTop) {
        try {
            WinTextRangeProvider.dispatchScrollIntoView(rangeId, alignToTop != 0);
            return GWIN_OK;
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /** {@code get_children}: {@code GetChildren()}. */
    static int onRangeGetChildren(long rangeId, MemorySegment out, MemorySegment outCount) {
        try {
            return outLongBlock(out, outCount, WinTextRangeProvider.dispatchGetChildren(rangeId));
        } catch (Throwable t) {
            reportUpcallFailure(t);
            return GWIN_ERR_UPCALL;
        }
    }

    /**
     * {@code accessible_disposed}: the last COM reference on the {@code GlassAccessible} is gone, so the
     * registry entry may go. It stands exactly where {@code DeleteGlobalRef} stood in
     * {@code ~GlassAccessible}, which is <em>not</em> {@code WinAccessible.dispose()} - UI Automation
     * holds its own references and keeps calling provider methods on a disposed peer, which the
     * {@code isDisposed()} guards in {@code WinAccessible} answer. Can arrive on the COM/RPC thread that
     * dropped that reference, and must not throw.
     */
    static void onAccessibleDisposed(long accessibleId) {
        try {
            WinAccessible.dispatchDisposed(accessibleId);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /** {@code range_disposed}: {@link #onAccessibleDisposed}'s rules, for the range registry. */
    static void onRangeDisposed(long rangeId) {
        try {
            WinTextRangeProvider.dispatchDisposed(rangeId);
        } catch (Throwable t) {
            reportUpcallFailure(t);
        }
    }

    /* -- the out-parameters the 89 slots write ------------------------------------------------------ */

    /** One {@code int64_t} out-parameter, always {@link #GWIN_OK}. */
    private static int outLong(MemorySegment out, long value) {
        bounded(out, JAVA_LONG.byteSize()).set(JAVA_LONG, 0, value);
        return GWIN_OK;
    }

    /** One {@code int32_t} out-parameter. */
    private static int outInt(MemorySegment out, int value) {
        bounded(out, JAVA_INT.byteSize()).set(JAVA_INT, 0, value);
        return GWIN_OK;
    }

    /** One {@code int32_t} out-parameter carrying a boolean, as 0 or 1; the C writes the VARIANT form. */
    private static int outBoolean(MemorySegment out, boolean value) {
        return outInt(out, value ? 1 : 0);
    }

    /** One {@code double} out-parameter. */
    private static int outDouble(MemorySegment out, double value) {
        bounded(out, JAVA_DOUBLE.byteSize()).set(JAVA_DOUBLE, 0, value);
        return GWIN_OK;
    }

    /**
     * {@code get_bounding_rectangle}: four floats and the flag that says whether they were written.
     * {@code null} - which is what {@code get_BoundingRectangle} answers for a node with no bounds - is
     * {@code *out_written == 0} and the provider then returns {@code S_OK} with the {@code UiaRect}
     * untouched, exactly as the JNI did for a null {@code jfloatArray}.
     * <p>
     * An array shorter than four raises {@code ArrayIndexOutOfBoundsException} here, which is reported
     * and becomes {@code E_FAIL}; the JNI read four floats out of whatever array arrived, through
     * {@code GetPrimitiveArrayCritical} and with no length check. {@code WinAccessible} produces only
     * {@code null} or a four-element array, so no caller can tell the two apart - but a garbage read is
     * not worth reproducing.
     */
    private static int outRectangle(MemorySegment out4, MemorySegment outWritten, float[] bounds) {
        int written = 0;
        if (bounds != null) {
            MemorySegment.copy(bounds, 0, bounded(out4, 4L * Float.BYTES), JAVA_FLOAT, 0, 4);
            written = 1;
        }
        return outInt(outWritten, written);
    }

    /**
     * A {@code gwin_alloc} block of {@code byteSize} bytes, bounded so that it can be written. Never
     * {@code NULL}: a heap that cannot give the block is an {@code OutOfMemoryError}, which the stub
     * catches and reports as {@link #GWIN_ERR_UPCALL}.
     */
    private static MemorySegment libraryBlock(long byteSize) {
        MemorySegment block = gwinAlloc(byteSize);
        if (block.equals(MemorySegment.NULL)) {
            throw new OutOfMemoryError("gwin_alloc(" + byteSize + ") returned NULL");
        }
        return bounded(block, byteSize);
    }

    /**
     * An {@code int64_t*} block and its count, {@code gwin_alloc}-ed and owned by the library from the
     * moment this returns. A {@code null} array is {@code NULL} with count 0, which the C turns into
     * {@code E_FAIL} - what {@code copyList(NULL)} answered; an empty array is a real block with count 0,
     * which is {@code S_OK} and an empty {@code SAFEARRAY}. The difference reaches the UIA client.
     */
    private static int outLongBlock(MemorySegment out, MemorySegment outCount, long[] values) {
        MemorySegment block = MemorySegment.NULL;
        int count = 0;
        if (values != null) {
            block = libraryBlock(Math.max(values.length, 1) * (long) Long.BYTES);
            MemorySegment.copy(values, 0, block, JAVA_LONG, 0, values.length);
            count = values.length;
        }
        bounded(out, ADDRESS.byteSize()).set(ADDRESS, 0, block);
        return outInt(outCount, count);
    }

    /** {@link #outLongBlock}'s rules for an {@code int32_t*} block - {@code get_runtime_id}. */
    private static int outIntBlock(MemorySegment out, MemorySegment outCount, int[] values) {
        MemorySegment block = MemorySegment.NULL;
        int count = 0;
        if (values != null) {
            block = libraryBlock(Math.max(values.length, 1) * (long) Integer.BYTES);
            MemorySegment.copy(values, 0, block, JAVA_INT, 0, values.length);
            count = values.length;
        }
        bounded(out, ADDRESS.byteSize()).set(ADDRESS, 0, block);
        return outInt(outCount, count);
    }

    /** {@link #outLongBlock}'s rules for a {@code double*} block - {@code get_bounding_rectangles}. */
    private static int outDoubleBlock(MemorySegment out, MemorySegment outCount, double[] values) {
        MemorySegment block = MemorySegment.NULL;
        int count = 0;
        if (values != null) {
            block = libraryBlock(Math.max(values.length, 1) * (long) Double.BYTES);
            MemorySegment.copy(values, 0, block, JAVA_DOUBLE, 0, values.length);
            count = values.length;
        }
        bounded(out, ADDRESS.byteSize()).set(ADDRESS, 0, block);
        return outInt(outCount, count);
    }

    /**
     * A {@code uint16_t*} block of UTF-16 code units and its length, {@code gwin_alloc}-ed and owned by
     * the library. Not NUL-terminated - the C wraps it in {@code SysAllocStringLen}. {@code null} is
     * {@code NULL} with length 0, which is the {@code E_FAIL} of {@code copyString(NULL)}; an empty
     * {@code String} is a real block with length 0, which is an empty {@code BSTR} and {@code S_OK}.
     * Code units move, never characters: a lone surrogate survives the crossing.
     */
    private static int outString(MemorySegment out, MemorySegment outLength, String text) {
        MemorySegment block = MemorySegment.NULL;
        int length = 0;
        if (text != null) {
            char[] chars = text.toCharArray();
            block = libraryBlock(Math.max(chars.length, 1) * (long) Character.BYTES);
            MemorySegment.copy(chars, 0, block, JAVA_CHAR, 0, chars.length);
            length = chars.length;
        }
        bounded(out, ADDRESS.byteSize()).set(ADDRESS, 0, block);
        return outInt(outLength, length);
    }

    /**
     * {@code count} borrowed UTF-16 code units as a {@code String} - the {@code NewString(text, len)} of
     * {@code SetValue(LPCWSTR)} and {@code FindText}. A {@code NULL} pointer is the empty string; the
     * library only dials those two slots with a non-NULL {@code BSTR}.
     */
    private static String utf16String(MemorySegment pointer, int count) {
        return new String(utf16(pointer, count));
    }

    /* -- GwinVariant -------------------------------------------------------------------------------- */

    /**
     * Writes {@code variant} into the library's {@code GwinVariant} out-parameter, with both blocks
     * {@code gwin_alloc}-ed and owned by the library from the moment this returns
     * ({@code GwinClipboardCallbacks.fos_serialize}'s rule). A {@code null} variant leaves
     * {@code vt = VT_EMPTY} and no value, which the C turns into {@code E_FAIL} with {@code VT_EMPTY} -
     * what {@code copyVariant(NULL)} did. Nothing is written until both blocks exist, so a failure
     * leaves the caller's struct as the library pre-zeroed it.
     */
    private static int outVariant(MemorySegment out, WinVariant variant) {
        MemorySegment target = bounded(out, GWIN_VARIANT_LAYOUT.byteSize());
        writeVariant(target, variant, WinGlassNative::libraryBlock);
        return GWIN_OK;
    }

    /**
     * Fills a {@code GwinVariant}. {@code allocator} owns the direction: {@link #libraryBlock} when the
     * library takes the blocks over, an arena when the caller keeps them for the length of a downcall.
     */
    private static void writeVariant(MemorySegment target, WinVariant variant, BlockAllocator allocator) {
        MemorySegment bstr = MemorySegment.NULL;
        int bstrLength = 0;
        MemorySegment doubles = MemorySegment.NULL;
        int doubleCount = 0;
        if (variant != null) {
            if (variant.bstrVal != null) {
                char[] chars = variant.bstrVal.toCharArray();
                bstr = allocator.allocate(Math.max(chars.length, 1) * (long) Character.BYTES);
                MemorySegment.copy(chars, 0, bstr, JAVA_CHAR, 0, chars.length);
                bstrLength = chars.length;
            }
            if (variant.pDblVal != null) {
                double[] values = variant.pDblVal;
                doubles = allocator.allocate(Math.max(values.length, 1) * (long) Double.BYTES);
                MemorySegment.copy(values, 0, doubles, JAVA_DOUBLE, 0, values.length);
                doubleCount = values.length;
            }
        }
        target.fill((byte) 0);
        if (variant != null) {
            target.set(JAVA_SHORT, VARIANT_VT_OFFSET, variant.vt);
            target.set(JAVA_SHORT, VARIANT_I_VAL_OFFSET, variant.iVal);
            target.set(JAVA_INT, VARIANT_L_VAL_OFFSET, variant.lVal);
            target.set(JAVA_FLOAT, VARIANT_FLT_VAL_OFFSET, variant.fltVal);
            target.set(JAVA_DOUBLE, VARIANT_DBL_VAL_OFFSET, variant.dblVal);
            target.set(JAVA_INT, VARIANT_BOOL_VAL_OFFSET, variant.boolVal ? 1 : 0);
            target.set(JAVA_LONG, VARIANT_PUNK_VAL_OFFSET, variant.punkVal);
        }
        target.set(ADDRESS, VARIANT_BSTR_VAL_OFFSET, bstr);
        target.set(JAVA_INT, VARIANT_BSTR_LEN_OFFSET, bstrLength);
        target.set(ADDRESS, VARIANT_P_DBL_VAL_OFFSET, doubles);
        target.set(JAVA_INT, VARIANT_P_DBL_COUNT_OFFSET, doubleCount);
    }

    /**
     * Reads a {@code GwinVariant} the library passed in. The one such parameter is
     * {@code find_attribute}'s, and the library always passes {@code NULL} there, because
     * {@code GlassTextRangeProvider::FindAttribute} never converted the {@code VARIANT} UI Automation
     * gave it ("//TODO VAL TO JVAL") and a migration carries that verbatim - so this normally answers
     * {@code null}, and decodes only if the library ever starts passing one.
     */
    private static WinVariant readVariant(MemorySegment value) {
        if (value.equals(MemorySegment.NULL)) {
            return null;
        }
        MemorySegment source = bounded(value, GWIN_VARIANT_LAYOUT.byteSize());
        WinVariant variant = new WinVariant();
        variant.vt = source.get(JAVA_SHORT, VARIANT_VT_OFFSET);
        variant.iVal = source.get(JAVA_SHORT, VARIANT_I_VAL_OFFSET);
        variant.lVal = source.get(JAVA_INT, VARIANT_L_VAL_OFFSET);
        variant.fltVal = source.get(JAVA_FLOAT, VARIANT_FLT_VAL_OFFSET);
        variant.dblVal = source.get(JAVA_DOUBLE, VARIANT_DBL_VAL_OFFSET);
        variant.boolVal = source.get(JAVA_INT, VARIANT_BOOL_VAL_OFFSET) != 0;
        variant.punkVal = source.get(JAVA_LONG, VARIANT_PUNK_VAL_OFFSET);
        MemorySegment bstr = source.get(ADDRESS, VARIANT_BSTR_VAL_OFFSET);
        if (!bstr.equals(MemorySegment.NULL)) {
            variant.bstrVal = utf16String(bstr, source.get(JAVA_INT, VARIANT_BSTR_LEN_OFFSET));
        }
        MemorySegment doubles = source.get(ADDRESS, VARIANT_P_DBL_VAL_OFFSET);
        if (!doubles.equals(MemorySegment.NULL)) {
            int count = source.get(JAVA_INT, VARIANT_P_DBL_COUNT_OFFSET);
            variant.pDblVal = bounded(doubles, count * JAVA_DOUBLE.byteSize()).toArray(JAVA_DOUBLE);
        }
        return variant;
    }

    /**
     * One {@code const GwinVariant*} argument of {@link #raiseAutomationPropertyChangedEvent}:
     * {@code NULL} for a {@code null} {@code WinVariant}, because that is the {@code jobject} the JNI
     * handed {@code copyVariant}, which answered {@code E_FAIL} and short-circuited before the OS call.
     */
    private static MemorySegment variantArgument(Arena arena, WinVariant variant, BlockAllocator allocator) {
        if (variant == null) {
            return MemorySegment.NULL;
        }
        MemorySegment target = arena.allocate(GWIN_VARIANT_LAYOUT);
        writeVariant(target, variant, allocator);
        return target;
    }

    /** Where a {@code GwinVariant}'s two blocks come from; see {@link #writeVariant}. */
    private interface BlockAllocator {

        /** A block of {@code byteSize} bytes, aligned for a {@code double} and writable. */
        MemorySegment allocate(long byteSize);
    }

    /* -- the downcalls ------------------------------------------------------------------------------ */

    /**
     * {@code gwin_a11y_create(accessible_id)}: {@code new GlassAccessible}, the body of
     * {@code Java_com_sun_glass_ui_win_WinAccessible__1createGlassAccessible} without its
     * {@code jobject}. No OS call and no thread marshal, so it works without a toolkit. 0 only when the
     * allocation failed, which {@code WinAccessible} turns into
     * {@code RuntimeException("could not create platform accessible")}.
     *
     * @return the {@code GlassAccessible*}, which is also its {@code IRawElementProviderSimple*}
     */
    static long createAccessible(long accessibleId) {
        try {
            return ((MemorySegment) GWIN_A11Y_CREATE.invokeExact(accessibleId)).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_a11y_destroy(acc)}: {@code GlassAccessible::Release}, which drops the Java peer's
     * reference and deletes only when the last one goes. {@code acc} must not be 0 - the JNI entry point
     * dereferenced it unconditionally and {@code WinAccessible} guards with {@code if (peer != 0L)};
     * that split stays as it is.
     */
    static void destroyAccessible(long accessible) {
        try {
            GWIN_A11Y_DESTROY.invokeExact(MemorySegment.ofAddress(accessible));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_a11y_text_range_create(acc, range_id)}: {@code new GlassTextRangeProvider} owned by
     * {@code acc}, which it AddRefs, so a live range pins its accessible. 0 when {@code accessible} is 0
     * or the allocation failed - and {@code WinTextRangeProvider} does not check, as it never did.
     *
     * @return the {@code GlassTextRangeProvider*}, which is also its {@code ITextRangeProvider*}
     */
    static long createTextRange(long accessible, long rangeId) {
        try {
            MemorySegment range = (MemorySegment) GWIN_A11Y_TEXT_RANGE_CREATE.invokeExact(
                    MemorySegment.ofAddress(accessible), rangeId);
            return range.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_a11y_text_range_destroy(range)}: {@code GlassTextRangeProvider::Release}, with
     * {@link #destroyAccessible}'s rules. {@code range} must not be 0: neither the JNI entry point nor
     * {@code WinTextRangeProvider.dispose()} checks, and a range that failed to be created crashes here
     * today. Carried as it is; fixing it is a separate change.
     */
    static void destroyTextRange(long range) {
        try {
            GWIN_A11Y_TEXT_RANGE_DESTROY.invokeExact(MemorySegment.ofAddress(range));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code gwin_a11y_raise_property_changed(acc, property_id, old_value, new_value)}: the one UI
     * Automation entry point of this section that keeps a C body, because building the two
     * {@code VARIANT}s means {@code oleaut32!SysAllocStringLen}, {@code SafeArrayCreateVector},
     * {@code SafeArrayPutElement} and an {@code IUnknown::AddRef} through a vtable slot - the
     * {@code copyVariant} marshalling the inbound direction needs in C anyway.
     * <p>
     * Both structs and both of their blocks live in a confined arena for the length of the call, which
     * is all the library asks: it reads them and frees nothing. The first variant that cannot be built
     * short-circuits with its own {@code HRESULT} and no OS call is made, which is what the JNI
     * returned.
     *
     * @return the {@code HRESULT}, sign-extended, as the JNI's {@code jlong} was
     */
    static long raiseAutomationPropertyChangedEvent(long provider, int propertyId, WinVariant oldValue,
                                                    WinVariant newValue) {
        try (Arena arena = Arena.ofConfined()) {
            BlockAllocator allocator = byteSize -> arena.allocate(byteSize, JAVA_DOUBLE.byteAlignment());
            // A null WinVariant crosses as a NULL pointer, not as a VT_EMPTY struct: copyVariant(NULL)
            // answered E_FAIL and made no OS call, and a VT_EMPTY struct is a value the OS accepts.
            MemorySegment previous = variantArgument(arena, oldValue, allocator);
            MemorySegment current = variantArgument(arena, newValue, allocator);
            return (long) GWIN_A11Y_RAISE_PROPERTY_CHANGED.invokeExact(MemorySegment.ofAddress(provider),
                    propertyId, previous, current);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code UIAutomationCore!UiaRaiseAutomationEvent(pProvider, id)} - bound straight from the OS,
     * because the JNI body was one {@code reinterpret_cast}, one {@code static_cast} and that call.
     * {@code pProvider} is a {@code GlassAccessible*}, whose first base <em>is</em> the
     * {@code IRawElementProviderSimple*} the function wants; that identity is the library's, recorded in
     * {@code glass_win_api.h}, and this crossing relies on it exactly as the JNI did.
     *
     * @return the {@code HRESULT}, sign-extended, as the JNI's {@code jlong} was
     */
    static long raiseAutomationEvent(long provider, int eventId) {
        try {
            return (int) UiaCore.RAISE_AUTOMATION_EVENT.invokeExact(MemorySegment.ofAddress(provider), eventId);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code UIAutomationCore!UiaClientsAreListening()} - the whole of the former JNI body. */
    static boolean uiaClientsAreListening() {
        try {
            return ((int) UiaCore.CLIENTS_ARE_LISTENING.invokeExact()) != 0;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code UIAutomationCore.dll} and the two functions of it this package needs, bound on first use
     * rather than in this class's initializer.
     * <p>
     * The laziness is behaviour parity, not taste: {@code glass.dll} delay-loads
     * {@code UIAutomationCore} ({@code win.cmake}, {@code /DELAYLOAD:Uiautomationcore.dll}), so a
     * process that never reaches accessibility never loads it, and a machine on which it could not be
     * resolved lost accessibility only. Binding it in {@link WinGlassNative}'s own initializer would
     * turn that into a failed initializer for this whole class - every Glass call would then die with
     * {@code NoClassDefFoundError} for a library it does not use. A holder class keeps both the load
     * point and the blast radius where the JNI had them, exactly as {@code Winmm} does for the timer.
     * <p>
     * The cost is that {@link WinGlassNative#boundSymbols()} grows when accessibility is first used;
     * the tests that assert that list force this holder first.
     */
    private static final class UiaCore {

        private static final String NAME = "UIAutomationCore";

        private static final SymbolLookup LOOKUP = systemLibrary(NAME + ".dll");

        /** {@code HRESULT UiaRaiseAutomationEvent(IRawElementProviderSimple* pProvider, EVENTID id)}. */
        static final MethodHandle RAISE_AUTOMATION_EVENT = bindUiaCore("UiaRaiseAutomationEvent",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

        /** {@code BOOL UiaClientsAreListening(void)}. */
        static final MethodHandle CLIENTS_ARE_LISTENING = bindUiaCore("UiaClientsAreListening",
                FunctionDescriptor.of(JAVA_INT));

        private UiaCore() {
        }

        private static MethodHandle bindUiaCore(String name, FunctionDescriptor descriptor) {
            return bind(LOOKUP, NAME, name, descriptor);
        }
    }
}

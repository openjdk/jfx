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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.SystemClipboard;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_CHAR_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * Test access to {@link WinGlassNative} and {@link WinScreenTransform}, the binding layer behind
 * {@link WinRobot} and {@link WinTimer}.
 * <p>
 * Compiled into the module by the {@code compile-shims} execution of the javafx.graphics pom, so it may
 * call the package-private facade; {@code test.com.sun.glass.ui.win.WinGlassNativeTest} reaches it
 * through the {@code --add-exports javafx.graphics/com.sun.glass.ui.win=ALL-UNNAMED} line of
 * {@code src/test/addExports}. Every downcall a test makes goes through here, so the restricted
 * {@code java.lang.foreign} calls stay inside the module that {@code --enable-native-access} names.
 * <p>
 * Line numbers into the {@code native-glass/win} C++ sources refer to those files at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>}).
 */
public final class WinGlassNativeShim {

    private WinGlassNativeShim() {
    }

    /**
     * Loads {@code glass}, binds the eleven {@code gwin_*} symbols, the nine {@code user32} ones and
     * {@code kernel32!GetVersion}, and checks the ABI version. Three blocks bind lazily instead: the
     * five {@code winmm} ones on the first timer call, the four {@code gdi32} ones (plus
     * {@code user32!CreateIconIndirect}) on the first custom cursor, and
     * {@code shlwapi!AssocQueryStringW} on the first {@code showDocument}; see
     * {@link #bindTimerSymbols()}, {@link #bindCursorSymbols()} and {@link #bindBrowserSymbols()}.
     *
     * @throws UnsatisfiedLinkError if a library cannot be loaded, lacks a symbol or reports another
     *         ABI version
     */
    public static void loadLibrary() {
        WinGlassNative.ensureLoaded();
    }

    public static List<String> boundSymbols() {
        return WinGlassNative.boundSymbols();
    }

    public static boolean resolves(String qualifiedName) {
        return WinGlassNative.resolves(qualifiedName);
    }

    public static int expectedAbiVersion() {
        return WinGlassNative.ABI_VERSION;
    }

    public static int abiVersion() {
        return WinGlassNative.abiVersion();
    }

    /** A {@code GwinStatus} / {@code GwinRobotCaptureError} / winuser.h constant the facade defines. */
    public static int constant(String name) {
        return switch (name) {
            case "GWIN_OK" -> WinGlassNative.GWIN_OK;
            case "GWIN_ERR_INVALID_ARG" -> WinGlassNative.GWIN_ERR_INVALID_ARG;
            case "GWIN_ERR_NO_TOOLKIT" -> WinGlassNative.GWIN_ERR_NO_TOOLKIT;
            case "GWIN_ROBOT_ERR_CREATE_DC" -> WinGlassNative.GWIN_ROBOT_ERR_CREATE_DC;
            case "GWIN_ROBOT_ERR_CREATE_MEM_DC" -> WinGlassNative.GWIN_ROBOT_ERR_CREATE_MEM_DC;
            case "GWIN_ROBOT_ERR_CREATE_BITMAP" -> WinGlassNative.GWIN_ROBOT_ERR_CREATE_BITMAP;
            case "GWIN_ROBOT_ERR_BITBLT" -> WinGlassNative.GWIN_ROBOT_ERR_BITBLT;
            case "GWIN_ROBOT_ERR_GETDIBITS" -> WinGlassNative.GWIN_ROBOT_ERR_GETDIBITS;
            case "INPUT_MOUSE" -> WinGlassNative.INPUT_MOUSE;
            case "INPUT_KEYBOARD" -> WinGlassNative.INPUT_KEYBOARD;
            case "KEYEVENTF_EXTENDEDKEY" -> WinGlassNative.KEYEVENTF_EXTENDEDKEY;
            case "KEYEVENTF_KEYUP" -> WinGlassNative.KEYEVENTF_KEYUP;
            case "MAPVK_VK_TO_VSC" -> WinGlassNative.MAPVK_VK_TO_VSC;
            case "MOUSEEVENTF_MOVE" -> WinGlassNative.MOUSEEVENTF_MOVE;
            case "MOUSEEVENTF_LEFTDOWN" -> WinGlassNative.MOUSEEVENTF_LEFTDOWN;
            case "MOUSEEVENTF_LEFTUP" -> WinGlassNative.MOUSEEVENTF_LEFTUP;
            case "MOUSEEVENTF_RIGHTDOWN" -> WinGlassNative.MOUSEEVENTF_RIGHTDOWN;
            case "MOUSEEVENTF_RIGHTUP" -> WinGlassNative.MOUSEEVENTF_RIGHTUP;
            case "MOUSEEVENTF_MIDDLEDOWN" -> WinGlassNative.MOUSEEVENTF_MIDDLEDOWN;
            case "MOUSEEVENTF_MIDDLEUP" -> WinGlassNative.MOUSEEVENTF_MIDDLEUP;
            case "MOUSEEVENTF_XDOWN" -> WinGlassNative.MOUSEEVENTF_XDOWN;
            case "MOUSEEVENTF_XUP" -> WinGlassNative.MOUSEEVENTF_XUP;
            case "MOUSEEVENTF_WHEEL" -> WinGlassNative.MOUSEEVENTF_WHEEL;
            case "MOUSEEVENTF_ABSOLUTE" -> WinGlassNative.MOUSEEVENTF_ABSOLUTE;
            case "XBUTTON1" -> WinGlassNative.XBUTTON1;
            case "XBUTTON2" -> WinGlassNative.XBUTTON2;
            case "WHEEL_DELTA" -> WinGlassNative.WHEEL_DELTA;
            case "SM_CXSCREEN" -> WinGlassNative.SM_CXSCREEN;
            case "SM_CYSCREEN" -> WinGlassNative.SM_CYSCREEN;
            case "SM_SWAPBUTTON" -> WinGlassNative.SM_SWAPBUTTON;
            case "SM_CXCURSOR" -> WinGlassNative.SM_CXCURSOR;
            case "SM_CYCURSOR" -> WinGlassNative.SM_CYCURSOR;
            case "SM_CXDOUBLECLK" -> WinGlassNative.SM_CXDOUBLECLK;
            case "SM_CYDOUBLECLK" -> WinGlassNative.SM_CYDOUBLECLK;
            case "BI_RGB" -> WinGlassNative.BI_RGB;
            case "DIB_RGB_COLORS" -> WinGlassNative.DIB_RGB_COLORS;
            case "VK_CAPITAL" -> WinGlassNative.VK_CAPITAL;
            case "VK_NUMLOCK" -> WinGlassNative.VK_NUMLOCK;
            case "SPI_GETHIGHCONTRAST" -> WinGlassNative.SPI_GETHIGHCONTRAST;
            case "SPI_GETCLIENTAREAANIMATION" -> WinGlassNative.SPI_GETCLIENTAREAANIMATION;
            case "HCF_HIGHCONTRASTON" -> WinGlassNative.HCF_HIGHCONTRASTON;
            case "COLOR_3DFACE" -> WinGlassNative.COLOR_3DFACE;
            case "COLOR_BTNTEXT" -> WinGlassNative.COLOR_BTNTEXT;
            case "COLOR_GRAYTEXT" -> WinGlassNative.COLOR_GRAYTEXT;
            case "COLOR_HIGHLIGHT" -> WinGlassNative.COLOR_HIGHLIGHT;
            case "COLOR_HIGHLIGHTTEXT" -> WinGlassNative.COLOR_HIGHLIGHTTEXT;
            case "COLOR_HOTLIGHT" -> WinGlassNative.COLOR_HOTLIGHT;
            case "COLOR_WINDOW" -> WinGlassNative.COLOR_WINDOW;
            case "COLOR_WINDOWTEXT" -> WinGlassNative.COLOR_WINDOWTEXT;
            case "TIMERR_NOERROR" -> WinGlassNative.TIMERR_NOERROR;
            case "TIME_ONESHOT" -> WinGlassNative.TIME_ONESHOT;
            case "TIME_PERIODIC" -> WinGlassNative.TIME_PERIODIC;
            case "GWIN_PT_SYSTEM_COLORS" -> WinGlassNative.GWIN_PT_SYSTEM_COLORS;
            case "GWIN_PT_SYSTEM_PARAMS" -> WinGlassNative.GWIN_PT_SYSTEM_PARAMS;
            case "GWIN_PT_UI_SETTINGS" -> WinGlassNative.GWIN_PT_UI_SETTINGS;
            case "GWIN_PT_NETWORK_INFORMATION" -> WinGlassNative.GWIN_PT_NETWORK_INFORMATION;
            case "GWIN_PT_ALL" -> WinGlassNative.GWIN_PT_ALL;
            case "GWIN_UI_COLOR_COUNT" -> WinGlassNative.GWIN_UI_COLOR_COUNT;
            case "GWIN_NET_COST_UNKNOWN" -> WinGlassNative.GWIN_NET_COST_UNKNOWN;
            case "GWIN_NET_COST_UNRESTRICTED" -> WinGlassNative.GWIN_NET_COST_UNRESTRICTED;
            case "GWIN_NET_COST_VARIABLE" -> WinGlassNative.GWIN_NET_COST_VARIABLE;
            case "GWIN_NET_COST_FIXED" -> WinGlassNative.GWIN_NET_COST_FIXED;
            case "MIIM_STATE" -> WinGlassNative.MIIM_STATE;
            case "MIIM_ID" -> WinGlassNative.MIIM_ID;
            case "MIIM_SUBMENU" -> WinGlassNative.MIIM_SUBMENU;
            case "MIIM_DATA" -> WinGlassNative.MIIM_DATA;
            case "MIIM_STRING" -> WinGlassNative.MIIM_STRING;
            case "MIIM_FTYPE" -> WinGlassNative.MIIM_FTYPE;
            case "MFT_STRING" -> WinGlassNative.MFT_STRING;
            case "MFT_SEPARATOR" -> WinGlassNative.MFT_SEPARATOR;
            case "MFS_ENABLED" -> WinGlassNative.MFS_ENABLED;
            case "MFS_GRAYED" -> WinGlassNative.MFS_GRAYED;
            case "MFS_CHECKED" -> WinGlassNative.MFS_CHECKED;
            case "MFS_UNCHECKED" -> WinGlassNative.MFS_UNCHECKED;
            case "MF_BYCOMMAND" -> WinGlassNative.MF_BYCOMMAND;
            case "MF_BYPOSITION" -> WinGlassNative.MF_BYPOSITION;
            case "MF_SEPARATOR" -> WinGlassNative.MF_SEPARATOR;
            case "MF_ENABLED" -> WinGlassNative.MF_ENABLED;
            case "MF_GRAYED" -> WinGlassNative.MF_GRAYED;
            case "MF_CHECKED" -> WinGlassNative.MF_CHECKED;
            case "MF_UNCHECKED" -> WinGlassNative.MF_UNCHECKED;
            case "IDC_ARROW" -> WinGlassNative.IDC_ARROW;
            case "IDC_IBEAM" -> WinGlassNative.IDC_IBEAM;
            case "IDC_WAIT" -> WinGlassNative.IDC_WAIT;
            case "IDC_CROSS" -> WinGlassNative.IDC_CROSS;
            case "IDC_SIZENWSE" -> WinGlassNative.IDC_SIZENWSE;
            case "IDC_SIZENESW" -> WinGlassNative.IDC_SIZENESW;
            case "IDC_SIZEWE" -> WinGlassNative.IDC_SIZEWE;
            case "IDC_SIZENS" -> WinGlassNative.IDC_SIZENS;
            case "IDC_SIZEALL" -> WinGlassNative.IDC_SIZEALL;
            case "IDC_HAND" -> WinGlassNative.IDC_HAND;
            case "HTNOWHERE" -> WinGlassNative.HTNOWHERE;
            case "RESIZE_DISABLE" -> WinWindow.RESIZE_DISABLE;
            case "RESIZE_AROUND_ANCHOR" -> WinWindow.RESIZE_AROUND_ANCHOR;
            case "RESIZE_TO_FX_ORIGIN" -> WinWindow.RESIZE_TO_FX_ORIGIN;
            case "GWIN_ERR_UPCALL" -> WinGlassNative.GWIN_ERR_UPCALL;
            case "GWIN_ERR_OLE" -> WinGlassNative.GWIN_ERR_OLE;
            case "GWIN_DIALOG_OK" -> WinGlassNative.GWIN_DIALOG_OK;
            case "GWIN_DIALOG_CANCELLED" -> WinGlassNative.GWIN_DIALOG_CANCELLED;
            case "GWIN_DIALOG_FAILED" -> WinGlassNative.GWIN_DIALOG_FAILED;
            case "MONITORINFOF_PRIMARY" -> WinGlassNative.MONITORINFOF_PRIMARY;
            case "BITSPIXEL" -> WinGlassNative.BITSPIXEL;
            case "PLANES" -> WinGlassNative.PLANES;
            case "LOGPIXELSX" -> WinGlassNative.LOGPIXELSX;
            case "LOGPIXELSY" -> WinGlassNative.LOGPIXELSY;
            case "CCHDEVICENAME" -> WinGlassNative.CCHDEVICENAME;
            case "MDT_EFFECTIVE_DPI" -> WinGlassNative.MDT_EFFECTIVE_DPI;
            case "MDT_RAW_DPI" -> WinGlassNative.MDT_RAW_DPI;
            case "PROCESS_PER_MONITOR_DPI_AWARE" -> WinGlassNative.PROCESS_PER_MONITOR_DPI_AWARE;
            case "S_OK" -> WinGlassNative.S_OK;
            default -> throw new IllegalArgumentException("no such constant: " + name);
        };
    }

    /* Layouts: byte size and field offsets, for the comparison with the winuser.h x64 ABI. */

    public static long layoutByteSize(String struct) {
        return layout(struct).byteSize();
    }

    public static long inputOffset(String member, String field) {
        return WinGlassNative.INPUT_LAYOUT.byteOffset(PathElement.groupElement("u"),
                PathElement.groupElement(member), PathElement.groupElement(field));
    }

    public static long offset(String struct, String field) {
        return layout(struct).byteOffset(PathElement.groupElement(field));
    }

    private static MemoryLayout layout(String struct) {
        return switch (struct) {
            case "INPUT" -> WinGlassNative.INPUT_LAYOUT;
            case "MOUSEINPUT" -> WinGlassNative.MOUSEINPUT_LAYOUT;
            case "KEYBDINPUT" -> WinGlassNative.KEYBDINPUT_LAYOUT;
            case "POINT" -> WinGlassNative.POINT_LAYOUT;
            case "TIMECAPS" -> WinGlassNative.TIMECAPS_LAYOUT;
            case "BITMAPINFOHEADER" -> WinGlassNative.BITMAPINFOHEADER_LAYOUT;
            case "ICONINFO" -> WinGlassNative.ICONINFO_LAYOUT;
            case "HIGHCONTRAST" -> WinGlassNative.HIGHCONTRAST_LAYOUT;
            case "GwinUiSettings" -> WinGlassNative.GWIN_UI_SETTINGS_LAYOUT;
            case "GwinNetworkInfo" -> WinGlassNative.GWIN_NETWORK_INFO_LAYOUT;
            case "GwinPrefsCallbacks" -> WinGlassNative.GWIN_PREFS_CALLBACKS_LAYOUT;
            case "GwinAppCallbacks" -> WinGlassNative.GWIN_APP_CALLBACKS_LAYOUT;
            case "GwinMenuCallbacks" -> WinGlassNative.GWIN_MENU_CALLBACKS_LAYOUT;
            case "GwinViewCallbacks" -> WinGlassNative.GWIN_VIEW_CALLBACKS_LAYOUT;
            case "GwinGestureCallbacks" -> WinGlassNative.GWIN_GESTURE_CALLBACKS_LAYOUT;
            case "GwinWindowCallbacks" -> WinGlassNative.GWIN_WINDOW_CALLBACKS_LAYOUT;
            case "GwinClipboardCallbacks" -> WinGlassNative.GWIN_CLIPBOARD_CALLBACKS_LAYOUT;
            case "GwinDndCallbacks" -> WinGlassNative.GWIN_DND_CALLBACKS_LAYOUT;
            case "GwinFileFilter" -> WinGlassNative.GWIN_FILE_FILTER_LAYOUT;
            case "RECT" -> WinGlassNative.RECT_LAYOUT;
            case "MENUITEMINFOW" -> WinGlassNative.MENUITEMINFOW_LAYOUT;
            case "MONITORINFOEXW" -> WinGlassNative.MONITORINFOEXW_LAYOUT;
            case "GwinScreenCallbacks" -> WinGlassNative.SCREEN_CALLBACKS_LAYOUT;
            case "BITMAP" -> BITMAP_LAYOUT;
            default -> throw new IllegalArgumentException("no such layout: " + struct);
        };
    }

    /* The key table and the Win32 primitives. */

    public static int keyJavaToWindows(int javaKeyCode) {
        return WinGlassNative.keyJavaToWindows(javaKeyCode);
    }

    public static int keyWindowsToJava(int virtualKey) {
        return WinGlassNative.keyWindowsToJava(virtualKey);
    }

    public static boolean isExtendedKey(int virtualKey) {
        return WinGlassNative.isExtendedKey(virtualKey);
    }

    public static int mapVirtualKey(int code, int mapType) {
        return WinGlassNative.mapVirtualKey(code, mapType);
    }

    public static int getSystemMetrics(int index) {
        return WinGlassNative.getSystemMetrics(index);
    }

    public static int[] cursorPosition() {
        return WinGlassNative.cursorPosition();
    }

    /* The key lock state, and the preference sources that are plain user32 calls. */

    public static short getKeyState(int virtualKey) {
        return WinGlassNative.getKeyState(virtualKey);
    }

    /** The facade's {@code _isKeyLocked}: {@code KEY_LOCK_ON}, {@code OFF} or {@code UNKNOWN}. */
    public static int keyLockState(int keyCode) {
        return WinGlassNative.keyLockState(keyCode);
    }

    /**
     * The same question asked through the {@link WinApplication} peer, which is what
     * {@code Application.isKeyLocked} calls ({@code Application.java:754-756}). Before the flip this
     * was the JNI body in {@code KeyTable.cpp:421-439} and the comparison with
     * {@link #keyLockState(int)} was the A/B oracle; after it, it is the same Java through one more
     * frame, and what it still proves is that the override is wired up.
     * <p>
     * Building the peer runs {@code WinApplication}'s static initializer -
     * {@code Toolkit.loadMSWindowsLibraries()}, {@code Application.loadNativeLibrary()} and the five
     * {@code WinGlassNative.install*} calls of the preferences, application, view, window and screen tables -
     * and nothing else (its {@code initIDs} native is deleted); the peer is built once and kept.
     * Nothing about it creates the toolkit window, so no table installed here is ever dialled by a real
     * message in the module JVM.
     * {@code javafx.embed.isEventThread} is set around the constructor so that no
     * {@code InvokeLaterDispatcher} thread is started in the shared test JVM
     * ({@code WinApplication}'s constructor).
     */
    public static synchronized int isKeyLockedThroughPeer(int keyCode) {
        return applicationPeer()._isKeyLocked(keyCode);
    }

    private static WinApplication applicationPeer;

    private static WinApplication applicationPeer() {
        if (applicationPeer == null) {
            String property = "javafx.embed.isEventThread";
            String previous = System.getProperty(property);
            System.setProperty(property, "true");
            try {
                applicationPeer = new WinApplication();
            } finally {
                if (previous == null) {
                    System.clearProperty(property);
                } else {
                    System.setProperty(property, previous);
                }
            }
        }
        return applicationPeer;
    }

    /* The screen table and the machine state around it. */

    /**
     * {@code WinApplication.staticScreen_getScreens()} on the shim's peer: the product method, Java since ABI
     * 5 - {@code WinScreenLayout.arrange(WinGlassNative.collectMonitors(), WinApplication.overrideUIScale)}. It
     * is {@code protected}, which this package reaches. It needs no toolkit; without one, the first enumeration in
     * a JVM runs the facade's {@code loadDpiFuncs(PROCESS_PER_MONITOR_DPI_AWARE)}, i.e.
     * {@code SetProcessDpiAwareness(2)}, which {@link #processDpiAwareness()} exists to watch.
     */
    public static synchronized Screen[] screensThroughApplicationPeer() {
        return applicationPeer().staticScreen_getScreens();
    }

    /** {@code WinApplication.overrideUIScale}: what its static initializer read from {@code glass.win.uiScale}. */
    public static float uiScaleOverride() {
        return WinApplication.overrideUIScale;
    }

    /**
     * {@code shcore!GetProcessDpiAwareness(NULL, &value)}: {@code {hresult, value}}, where the value
     * is a {@code PROCESS_DPI_AWARENESS} ({@code 0} unaware, {@code 1} system, {@code 2} per monitor)
     * and is {@code -1} if the call did not write it. Bound here and not in {@link WinGlassNative}:
     * product code never asks, and a facade bind would enter {@link #boundSymbols()}, which the
     * binding tests assert exactly.
     */
    public static int[] processDpiAwareness() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment value = arena.allocate(JAVA_INT);
            value.set(JAVA_INT, 0, -1);
            int hresult = (int) DpiReadback.GET_PROCESS_DPI_AWARENESS.invokeExact(MemorySegment.NULL, value);
            return new int[] {hresult, value.get(JAVA_INT, 0)};
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /** The one {@code shcore} symbol the screen snapshot's DPI guard needs; see {@link #processDpiAwareness()}. */
    private static final class DpiReadback {

        /** {@code HRESULT GetProcessDpiAwareness(HANDLE hprocess, PROCESS_DPI_AWARENESS *value)}. */
        private static final MethodHandle GET_PROCESS_DPI_AWARENESS = bind();

        private DpiReadback() {
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bind() {
            MemorySegment symbol = SymbolLookup.libraryLookup("shcore.dll", Arena.global())
                    .find("GetProcessDpiAwareness")
                    .orElseThrow(() -> new UnsatisfiedLinkError("missing native symbol: GetProcessDpiAwareness"));
            return Linker.nativeLinker().downcallHandle(symbol, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        }
    }

    /**
     * Initializes {@code WinAccessible}, whose static initializer installs the two accessibility callback
     * tables - where {@code _initIDs} stood until the nine accessibility natives became downcalls.
     *
     * @return {@code null}, or what prevented the initialization
     */
    public static Throwable initializeWinAccessible() {
        try {
            Class.forName("com.sun.glass.ui.win.WinAccessible", true, WinGlassNativeShim.class.getClassLoader());
            return null;
        } catch (ClassNotFoundException | LinkageError e) {
            return e;
        }
    }

    /* The screen enumeration in Java, its arrangement over synthetic monitors, and the goldens' gate. */

    /**
     * Initializes the facade's lazy {@code shcore} holder, the fourth lazy block: it binds its three
     * symbols only when all three resolve. Call it from a {@code @BeforeAll} <b>after</b>
     * {@link #bindTimerSymbols()}, {@link #bindCursorSymbols()} and {@link #bindBrowserSymbols()}, which
     * fixes the order of all four for the exact-equality assertion on {@link #boundSymbols()}. Loads
     * {@code SHCore.dll} and sets nothing.
     */
    public static void bindScreenSymbols() {
        boolean ignoredResolved = WinGlassNative.dpiFunctionsResolved();
    }

    /** Whether {@code shcore}'s three DPI functions resolved, i.e. which arm {@code GetMonitorSettings} takes. */
    public static boolean dpiFunctionsResolved() {
        return WinGlassNative.dpiFunctionsResolved();
    }

    public static int sizeOfScreenCallbacks() {
        return WinGlassNative.sizeOfScreenCallbacks();
    }

    public static boolean screenCallbacksInstalled() {
        return WinGlassNative.screenCallbacksInstalled();
    }

    /**
     * {@link WinGlassNative#collectMonitors()} arranged by {@link WinScreenLayout} with
     * {@code WinApplication.overrideUIScale} read now - the body of {@code WinApplication.staticScreen_getScreens},
     * without needing a peer. Without a toolkit the first call in a JVM runs the facade's
     * {@code loadDpiFuncs(PROCESS_PER_MONITOR_DPI_AWARE)}, i.e. {@code SetProcessDpiAwareness(2)}, which
     * {@link #processDpiAwareness()} exists to watch.
     */
    public static Screen[] screensThroughJava() {
        return WinScreenLayout.arrange(WinGlassNative.collectMonitors(), WinApplication.overrideUIScale);
    }

    /** What {@link WinGlassNative#collectMonitors()} returned, one record's text per monitor, for a log. */
    public static List<String> collectedMonitors() {
        return WinGlassNative.collectMonitors().stream().map(Object::toString).toList();
    }

    /**
     * What Windows says about this machine's monitors through queries that share no code with
     * {@link WinGlassNative#collectMonitors()} or {@link WinScreenLayout}: the facts a screen golden's machine
     * gate needs, obtained without the enumeration the golden is compared with. {@code monitors} is
     * {@code GetSystemMetrics(SM_CMONITORS)}; {@code width} and {@code height} are the primary monitor's
     * {@code SM_CXSCREEN} and {@code SM_CYSCREEN}, its rectangle starting at the origin by definition; the four
     * {@code work*} values are its work area as {@code SystemParametersInfoW(SPI_GETWORKAREA)} wrote it, 0 unless
     * {@code workAreaRead}; {@code dpiX} and {@code dpiY} are its effective DPI from {@code shcore!GetDpiForMonitor}
     * with {@code MDT_EFFECTIVE_DPI} on {@code MonitorFromPoint((0, 0), MONITOR_DEFAULTTOPRIMARY)}, -1 unless that
     * call wrote them, and {@code dpiResult} is its {@code HRESULT}.
     */
    public record MachineMonitors(int monitors, int width, int height, boolean workAreaRead, int workLeft,
                                  int workTop, int workRight, int workBottom, int dpiResult, int dpiX, int dpiY) {
    }

    /**
     * {@link MachineMonitors} for this machine, through {@link ScreenGateProbe}'s own handles - not the
     * facade's, so that no binding, layout or arithmetic of the code under test is on this path. The values are
     * physical pixels and the effective DPI only in a per-monitor DPI aware process, which {@code java.exe} is by
     * its manifest; a DPI-unaware one would read them virtualized, as it would read the enumeration.
     */
    public static MachineMonitors machineMonitors() {
        try (Arena arena = Arena.ofConfined()) {
            int monitors = (int) ScreenGateProbe.GET_SYSTEM_METRICS.invokeExact(ScreenGateProbe.SM_CMONITORS);
            int width = (int) ScreenGateProbe.GET_SYSTEM_METRICS.invokeExact(ScreenGateProbe.SM_CXSCREEN);
            int height = (int) ScreenGateProbe.GET_SYSTEM_METRICS.invokeExact(ScreenGateProbe.SM_CYSCREEN);
            MemorySegment work = arena.allocate(JAVA_INT, 4);
            boolean workAreaRead = (int) ScreenGateProbe.SYSTEM_PARAMETERS_INFO_W.invokeExact(
                    ScreenGateProbe.SPI_GETWORKAREA, 0, work, 0) != 0;
            MemorySegment origin = arena.allocate(ScreenGateProbe.POINT);
            MemorySegment primary = (MemorySegment) ScreenGateProbe.MONITOR_FROM_POINT.invokeExact(origin,
                    ScreenGateProbe.MONITOR_DEFAULTTOPRIMARY);
            MemorySegment dpi = arena.allocate(JAVA_INT, 2);
            dpi.fill((byte) 0xFF);
            int dpiResult = (int) ScreenGateProbe.GET_DPI_FOR_MONITOR.invokeExact(primary,
                    ScreenGateProbe.MDT_EFFECTIVE_DPI, dpi, dpi.asSlice(JAVA_INT.byteSize()));
            return new MachineMonitors(monitors, width, height, workAreaRead,
                    work.getAtIndex(JAVA_INT, 0), work.getAtIndex(JAVA_INT, 1),
                    work.getAtIndex(JAVA_INT, 2), work.getAtIndex(JAVA_INT, 3),
                    dpiResult, dpi.getAtIndex(JAVA_INT, 0), dpi.getAtIndex(JAVA_INT, 1));
        } catch (Throwable t) {
            throw new AssertionError("the screen gate's Win32 queries could not be called", t);
        }
    }

    /**
     * The screen gate's symbols, bound here for {@link #machineMonitors()} and never in the facade: the facade
     * binds {@code GetSystemMetrics} and {@code SystemParametersInfoW} too, but a gate through the handles of the
     * code under test would not be independent of it, and {@link #boundSymbols()} must keep listing the
     * product's bindings only.
     */
    private static final class ScreenGateProbe {

        /** winuser.h. */
        static final int SM_CXSCREEN = 0;
        static final int SM_CYSCREEN = 1;
        static final int SM_CMONITORS = 80;
        static final int SPI_GETWORKAREA = 0x0030;
        static final int MONITOR_DEFAULTTOPRIMARY = 0x00000001;

        /** shellscalingapi.h {@code MDT_EFFECTIVE_DPI}. */
        static final int MDT_EFFECTIVE_DPI = 0;

        /** windef.h {@code POINT}, passed by value. */
        static final StructLayout POINT = MemoryLayout.structLayout(JAVA_INT.withName("x"), JAVA_INT.withName("y"));

        private static final Linker LINKER = Linker.nativeLinker();
        private static final SymbolLookup USER32 = library("user32.dll");
        private static final SymbolLookup SHCORE = library("shcore.dll");

        /** {@code int GetSystemMetrics(int nIndex)}. */
        static final MethodHandle GET_SYSTEM_METRICS = bind(USER32, "GetSystemMetrics",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT));

        /** {@code BOOL SystemParametersInfoW(UINT uiAction, UINT uiParam, PVOID pvParam, UINT fWinIni)}. */
        static final MethodHandle SYSTEM_PARAMETERS_INFO_W = bind(USER32, "SystemParametersInfoW",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

        /** {@code HMONITOR MonitorFromPoint(POINT pt, DWORD dwFlags)}. */
        static final MethodHandle MONITOR_FROM_POINT = bind(USER32, "MonitorFromPoint",
                FunctionDescriptor.of(ADDRESS, POINT, JAVA_INT));

        /** {@code HRESULT GetDpiForMonitor(HMONITOR, MONITOR_DPI_TYPE, UINT *dpiX, UINT *dpiY)}. */
        static final MethodHandle GET_DPI_FOR_MONITOR = bind(SHCORE, "GetDpiForMonitor",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));

        private ScreenGateProbe() {
        }

        @SuppressWarnings("restricted")
        private static SymbolLookup library(String fileName) {
            return SymbolLookup.libraryLookup(fileName, Arena.global());
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bind(SymbolLookup lookup, String name, FunctionDescriptor fd) {
            MemorySegment symbol = lookup.find(name).orElseThrow(
                    () -> new UnsatisfiedLinkError("missing native symbol: " + name));
            return LINKER.downcallHandle(symbol, fd);
        }
    }

    /**
     * Sets {@code WinApplication.overrideUIScale}, which {@link #screensThroughJava()} and
     * {@code staticScreen_getScreens} read at enumeration time; its only reader since ABI 5 (the C copy
     * {@code initIDs} kept went with it).
     */
    public static synchronized void setJavaUiScaleOverride(float scale) {
        WinApplication.overrideUIScale = scale;
    }

    /** {@code GlassApplication::GetUIScale} in Java, over an explicit override. */
    public static float uiScale(int dpi, float overrideUIScale) {
        return WinScreenLayout.uiScale(dpi, overrideUIScale);
    }

    /** {@code (jint) floorf(value)} as the port computes it, MSVC's {@code cvttss2si} answer included. */
    public static int floorToInt(float value) {
        return WinScreenLayout.floorToInt(value);
    }

    /**
     * One synthetic monitor: what {@code GetMonitorSettings} learns about a monitor, with the two
     * {@code GetUIScale} results given directly - so a corpus can use scales no integer DPI produces. The
     * fields are those of the {@code GwinTestMonitor} that {@code screen-anchor-golden.txt} was captured from,
     * in its order; that struct and its hook have since been deleted.
     */
    public record TestMonitor(int monitorLeft, int monitorTop, int monitorRight, int monitorBottom,
                              int workLeft, int workTop, int workRight, int workBottom,
                              boolean primary, int colorDepth, int dpiX, int dpiY, float uiScaleX, float uiScaleY) {
    }

    /**
     * {@link WinScreenLayout} over synthetic monitors - {@code hMonitor} = the input index, the scales as given,
     * as the deleted {@code gwin_test_screen_anchor} took them - reporting every branch it enters to
     * {@code hits}, then the {@code Screen} each slot makes.
     */
    public static Screen[] anchorInJava(List<TestMonitor> monitors, AnchorHits hits) {
        WinScreenLayout.Monitor[] infos = new WinScreenLayout.Monitor[monitors.size()];
        for (int i = 0; i < infos.length; i++) {
            TestMonitor m = monitors.get(i);
            infos[i] = WinScreenLayout.Monitor.of(new WinScreenLayout.MonitorSettings(i,
                    m.monitorLeft(), m.monitorTop(), m.monitorRight(), m.monitorBottom(),
                    m.workLeft(), m.workTop(), m.workRight(), m.workBottom(),
                    m.primary(), m.colorDepth(), m.dpiX(), m.dpiY(), 0, 0), m.uiScaleX(), m.uiScaleY());
        }
        WinScreenLayout.arrange(infos, hits.probe());
        Screen[] screens = new Screen[infos.length];
        for (int i = 0; i < infos.length; i++) {
            screens[i] = infos[i].toScreen();
        }
        return screens;
    }

    /**
     * The branches of {@code GlassScreen.cpp}'s anchoring, counted the way the C differential harness that
     * compared the port with the C counted them: an entry per helper, the arm taken where
     * a helper has arms, and the eight rounding inputs of every dividing {@code anchorTo} classified by the
     * fraction of {@code v / scale}. The two {@code MAX_*} entries are maxima, everything else a count.
     */
    public enum AnchorArm {
        ANCHOR_TO, SCALE_X_DIVIDES, SCALE_Y_DIVIDES, X_BEFORE, Y_BEFORE,
        ROUND_FRACTION_ZERO, ROUND_BELOW_HALF, ROUND_AT_HALF, ROUND_ABOVE_HALF, ROUND_NEGATIVE,
        ORIGIN_OFFSET_EQUAL_START, ORIGIN_OFFSET_EQUAL_END, ORIGIN_OFFSET_MIDPOINT,
        ANCHOR_H_BEFORE, ANCHOR_H_AFTER, ANCHOR_V_BEFORE, ANCHOR_V_AFTER,
        TOUCHES_LEFT_TRUE, TOUCHES_LEFT_FALSE, TOUCHES_ABOVE_TRUE, TOUCHES_ABOVE_FALSE,
        ANCHOR, PROPAGATE, MAX_PROPAGATE_PASS, MAX_ANCHOR_PASS
    }

    /** Counters for {@link AnchorArm}, fed by the {@link WinScreenLayout.Probe} {@link #anchorInJava} installs. */
    public static final class AnchorHits {

        private final long[] counts = new long[AnchorArm.values().length];

        public long get(AnchorArm arm) {
            return counts[arm.ordinal()];
        }

        /** Adds {@code other} into this: sums, except the two maxima. */
        public void add(AnchorHits other) {
            for (AnchorArm arm : AnchorArm.values()) {
                if (arm == AnchorArm.MAX_PROPAGATE_PASS || arm == AnchorArm.MAX_ANCHOR_PASS) {
                    counts[arm.ordinal()] = Math.max(counts[arm.ordinal()], other.get(arm));
                } else {
                    counts[arm.ordinal()] += other.get(arm);
                }
            }
        }

        private void hit(AnchorArm arm) {
            counts[arm.ordinal()]++;
        }

        private void atLeast(AnchorArm arm, long value) {
            counts[arm.ordinal()] = Math.max(counts[arm.ordinal()], value);
        }

        private void rounding(float q) {
            if (q < 0.0f) {
                hit(AnchorArm.ROUND_NEGATIVE);
            }
            float fraction = q - (float) Math.floor(q);
            if (fraction == 0.0f) {
                hit(AnchorArm.ROUND_FRACTION_ZERO);
            } else if (fraction < 0.5f) {
                hit(AnchorArm.ROUND_BELOW_HALF);
            } else if (fraction == 0.5f) {
                hit(AnchorArm.ROUND_AT_HALF);
            } else {
                hit(AnchorArm.ROUND_ABOVE_HALF);
            }
        }

        private WinScreenLayout.Probe probe() {
            return new WinScreenLayout.Probe() {
                @Override
                public void anchorTo(WinScreenLayout.Monitor p, boolean xBefore, boolean yBefore, int pass) {
                    hit(AnchorArm.ANCHOR_TO);
                    atLeast(AnchorArm.MAX_ANCHOR_PASS, pass);
                    if (xBefore) {
                        hit(AnchorArm.X_BEFORE);
                    }
                    if (yBefore) {
                        hit(AnchorArm.Y_BEFORE);
                    }
                    int monX = p.rcMonitor.left;
                    int monY = p.rcMonitor.top;
                    if (p.uiScaleX != 1.0f) {
                        hit(AnchorArm.SCALE_X_DIVIDES);
                        float s = p.uiScaleX;
                        rounding(p.dpiX / s);
                        rounding((p.rcMonitor.right - monX) / s);
                        rounding((p.rcWork.left - monX) / s);
                        rounding((p.rcWork.right - monX) / s);
                    }
                    if (p.uiScaleY != 1.0f) {
                        hit(AnchorArm.SCALE_Y_DIVIDES);
                        float s = p.uiScaleY;
                        rounding(p.dpiY / s);
                        rounding((p.rcMonitor.bottom - monY) / s);
                        rounding((p.rcWork.top - monY) / s);
                        rounding((p.rcWork.bottom - monY) / s);
                    }
                }

                @Override
                public void anchor() {
                    hit(AnchorArm.ANCHOR);
                }

                @Override
                public void originOffset(int aV0, int aV1, int mV0, int mV1) {
                    if (aV0 == mV0) {
                        hit(AnchorArm.ORIGIN_OFFSET_EQUAL_START);
                    } else if (aV1 == mV1) {
                        hit(AnchorArm.ORIGIN_OFFSET_EQUAL_END);
                    } else {
                        hit(AnchorArm.ORIGIN_OFFSET_MIDPOINT);
                    }
                }

                @Override
                public void anchorH(boolean before) {
                    hit(before ? AnchorArm.ANCHOR_H_BEFORE : AnchorArm.ANCHOR_H_AFTER);
                }

                @Override
                public void anchorV(boolean before) {
                    hit(before ? AnchorArm.ANCHOR_V_BEFORE : AnchorArm.ANCHOR_V_AFTER);
                }

                @Override
                public void touchesLeft(boolean touches) {
                    hit(touches ? AnchorArm.TOUCHES_LEFT_TRUE : AnchorArm.TOUCHES_LEFT_FALSE);
                }

                @Override
                public void touchesAbove(boolean touches) {
                    hit(touches ? AnchorArm.TOUCHES_ABOVE_TRUE : AnchorArm.TOUCHES_ABOVE_FALSE);
                }

                @Override
                public void propagate(int pass) {
                    hit(AnchorArm.PROPAGATE);
                    atLeast(AnchorArm.MAX_PROPAGATE_PASS, pass);
                }
            };
        }
    }

    /**
     * {@code gwin_test_fire_screen_callback(slot)} with no table installed; the table is put back
     * afterwards ({@code WinGlassNative.rewriteScreenCallbacks}).
     */
    public static synchronized long fireScreenCallbackWithNoTable(int slot) {
        try {
            requireScreenTableWritten((int) ScreenHooks.SET.invokeExact(MemorySegment.NULL));
            return (long) ScreenHooks.FIRE.invokeExact(slot);
        } catch (Throwable t) {
            throw new AssertionError("the screen table could not be cleared or fired", t);
        } finally {
            WinGlassNative.rewriteScreenCallbacks();
        }
    }

    /** What firing a slot of the recording screen table answered, and how often the recording stub ran. */
    public record ScreenFire(long returned, int calls) {
    }

    /**
     * Installs a table whose {@code settings_changed} is a recording stub built from the facade's own
     * descriptor - or {@code NULL}, which the library replaces by an internal no-op, when
     * {@code nullSlot} - fires {@code slot}, and puts the table back in a {@code finally}.
     */
    public static synchronized ScreenFire fireScreenCallbackIntoRecordingTable(int slot, boolean nullSlot) {
        ScreenHooks.calls = 0;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(WinGlassNative.SCREEN_CALLBACKS_LAYOUT);
            table.set(ADDRESS, 0L, nullSlot ? MemorySegment.NULL : ScreenHooks.RECORDING_STUB);
            requireScreenTableWritten((int) ScreenHooks.SET.invokeExact(table));
            long returned = (long) ScreenHooks.FIRE.invokeExact(slot);
            return new ScreenFire(returned, ScreenHooks.calls);
        } catch (Throwable t) {
            throw new AssertionError("the recording screen table could not be installed or fired", t);
        } finally {
            WinGlassNative.rewriteScreenCallbacks();
        }
    }

    private static void requireScreenTableWritten(int status) {
        if (status != WinGlassNative.GWIN_OK) {
            throw new AssertionError("gwin_screen_set_callbacks answered " + status);
        }
    }

    /**
     * The screen table's test hook, bound here and never in the facade, plus the installer again for the
     * recording table, as {@code WindowRecording} does. (Its sibling {@code gwin_test_screen_anchor}, which ran
     * the C anchoring over synthetic monitors, was bound here too until it was deleted.)
     */
    private static final class ScreenHooks {

        private static final Linker LINKER = Linker.nativeLinker();

        /** {@code int64_t gwin_test_fire_screen_callback(int32_t slot)}. */
        private static final MethodHandle FIRE = bindGlass("gwin_test_fire_screen_callback",
                FunctionDescriptor.of(JAVA_LONG, JAVA_INT));

        /** {@code int32_t gwin_screen_set_callbacks(const GwinScreenCallbacks* cb)}. */
        private static final MethodHandle SET = bindGlass("gwin_screen_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** How often {@link #recordSettingsChanged} ran since the last reset; written under the shim's lock. */
        private static int calls;

        private static final MemorySegment RECORDING_STUB = recordingStub();

        private ScreenHooks() {
        }

        static void recordSettingsChanged() {
            calls++;
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bindGlass(String name, FunctionDescriptor fd) {
            MemorySegment symbol = SymbolLookup.loaderLookup().find(name).orElseThrow(
                    () -> new UnsatisfiedLinkError("missing native symbol: " + name + " in glass"));
            return LINKER.downcallHandle(symbol, fd);
        }

        @SuppressWarnings("restricted")
        private static MemorySegment recordingStub() {
            try {
                MethodHandle target = MethodHandles.lookup().findStatic(ScreenHooks.class, "recordSettingsChanged",
                        WinGlassNative.SCREEN_SETTINGS_CHANGED_FD.toMethodType());
                return LINKER.upcallStub(target, WinGlassNative.SCREEN_SETTINGS_CHANGED_FD, Arena.global());
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("no recording target recordSettingsChanged", e);
            }
        }
    }

    public static int sysColor(int index) {
        return WinGlassNative.sysColor(index);
    }

    public static int[] sysColorChannels(int colorRef) {
        return new int[] {WinGlassNative.sysColorRed(colorRef), WinGlassNative.sysColorGreen(colorRef),
                WinGlassNative.sysColorBlue(colorRef)};
    }

    /** {@code SPI_GETHIGHCONTRAST}, or {@code null} when the call returned {@code FALSE}. */
    public static HighContrastInfo highContrast() {
        WinGlassNative.HighContrast highContrast = WinGlassNative.highContrast();
        return highContrast == null ? null
                : new HighContrastInfo(highContrast.on(), highContrast.scheme());
    }

    /** {@link WinGlassNative.HighContrast} in a shape the test package can name. */
    public record HighContrastInfo(boolean on, String scheme) {
    }

    /** {@code SPI_GETCLIENTAREAANIMATION}, or {@code null} when the call returned {@code FALSE}. */
    public static Boolean clientAreaAnimation() {
        return WinGlassNative.clientAreaAnimation();
    }

    /**
     * Drives the facade's {@code HIGHCONTRAST.lpszDefaultScheme} reader over a string this side
     * allocated, NUL-terminated and UTF-16 little-endian exactly as Windows leaves it. That path only
     * runs while high contrast is on, so without this hook a wrong charset would be discovered by a
     * user and not by a test.
     */
    public static String schemeNameOf(String value) {
        try (Arena arena = Arena.ofConfined()) {
            return WinGlassNative.schemeName(arena.allocateFrom(value, StandardCharsets.UTF_16LE));
        }
    }

    /**
     * The same reader over raw code units this side wrote, unit for unit, into a native buffer - which a
     * {@code String} encoded through a charset cannot do for an unpaired surrogate. The reader walks to
     * the first {@code U+0000} with no bound, so {@code units} must contain one; anything after it is
     * written too, and must not be read.
     *
     * @throws IllegalArgumentException if {@code units} contains no {@code U+0000}
     */
    public static String schemeNameOfCodeUnits(char[] units) {
        if (new String(units).indexOf('\0') < 0) {
            throw new IllegalArgumentException("the reader would walk past the buffer: no U+0000 in the units");
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(JAVA_CHAR, units.length);
            MemorySegment.copy(units, 0, buffer, JAVA_CHAR, 0, units.length);
            return WinGlassNative.schemeName(buffer);
        }
    }

    /** The same reader handed a NULL pointer, which is what a zero-filled struct leaves behind. */
    public static String schemeNameOfNull() {
        return WinGlassNative.schemeName(MemorySegment.NULL);
    }

    /** {@code PlatformSupport::querySystemColors} in Java, into a map that accepts null values. */
    public static Map<String, Object> collectSystemColors() {
        Map<String, Object> preferences = new HashMap<>();
        WinPreferences.systemColors(preferences);
        return preferences;
    }

    /** {@code PlatformSupport::querySystemParameters} in Java, into a map that accepts null values. */
    public static Map<String, Object> collectSystemParameters() {
        Map<String, Object> preferences = new HashMap<>();
        WinPreferences.systemParameters(preferences);
        return preferences;
    }

    /** The union of every key the four collectors can emit - all 23 since ABI 2. */
    public static Set<String> preferenceKeys() {
        return WinPreferences.keys();
    }

    /**
     * {@code WinApplication.getPlatformKeys().keySet()} - the 23 keys the peer declares, which the
     * comments above {@code WinApplication.getPlatformKeys} / {@code getPlatformKeyMappings} ask to be
     * kept in sync with the collector by hand (as the "kept in sync" comments in {@code PlatformSupport}'s
     * three query methods did until those methods were deleted). Exposed so a test can do
     * the comparison instead.
     */
    public static synchronized Set<String> platformKeys() {
        return applicationPeer().getPlatformKeys().keySet();
    }

    /** The same declaration with its types, so a collected value can be checked against it. */
    public static synchronized Map<String, Class<?>> platformKeyTypes() {
        return applicationPeer().getPlatformKeys();
    }

    /* The six exports of ABI 2: the key-code lookup and the WinRT half of the preferences. */

    /** {@code gwin_key_code_for_char} through the facade, which narrows the {@code char} to a short. */
    public static int keyCodeForChar(char c, int hint) {
        return WinGlassNative.keyCodeForChar((short) c, hint);
    }

    /**
     * The same question asked through the {@link WinApplication} peer, which is what
     * {@code Application.getKeyCodeForChar} calls ({@code Application.java:745}). Both arms run on the
     * calling thread, which matters: {@code gwin_key_code_for_char} reads the calling thread's
     * keyboard layout in its numeric-keypad branch.
     */
    public static synchronized int keyCodeForCharThroughPeer(char c, int hint) {
        return applicationPeer()._getKeyCodeForChar(c, hint);
    }

    /** {@code gwin_sizeof_ui_settings}: the C compiler's own {@code sizeof(GwinUiSettings)}. */
    public static int sizeOfUiSettings() {
        return WinGlassNative.sizeOfUiSettings();
    }

    /** {@code gwin_sizeof_network_info}. */
    public static int sizeOfNetworkInfo() {
        return WinGlassNative.sizeOfNetworkInfo();
    }

    /** {@link WinGlassNative.UiSettings} in a shape the test package can name. */
    public record UiSettingsInfo(boolean available, boolean colorsValid, int[] colors,
                                 boolean advancedEffectsValid, boolean advancedEffectsEnabled,
                                 boolean autoHideValid, boolean autoHideScrollBars) {
    }

    /** {@link WinGlassNative.NetworkInfo} in a shape the test package can name. */
    public record NetworkCostInfo(boolean available, int costType) {
    }

    /**
     * {@code gwin_prefs_query_ui_settings}. Without a running toolkit there are no WinRT objects to
     * read, so {@code available} is false and every other field is its zero - which is the case the
     * JNI answered with a {@code NULL} map, and the only case a unit test can reach.
     */
    public static UiSettingsInfo uiSettings() {
        WinGlassNative.UiSettings settings = WinGlassNative.uiSettings();
        return new UiSettingsInfo(settings.available(), settings.colorsValid(), settings.colors(),
                settings.advancedEffectsValid(), settings.advancedEffectsEnabled(),
                settings.autoHideValid(), settings.autoHideScrollBars());
    }

    /** {@code gwin_prefs_query_network}; same "no toolkit, no keys" rule as {@link #uiSettings()}. */
    public static NetworkCostInfo networkInfo() {
        WinGlassNative.NetworkInfo info = WinGlassNative.networkInfo();
        return new NetworkCostInfo(info.available(), info.costType());
    }

    /** {@code PlatformSupport::collectPreferences(types)} in Java. */
    public static Map<String, Object> collect(int types) {
        return WinPreferences.collect(types);
    }

    /** {@code PlatformSupport::updatePreferences(types)} in Java: collect, compare, notify. */
    public static boolean updatePreferences(int types) {
        return WinPreferences.update(types);
    }

    /** The map the last {@link #updatePreferences} collected, or null before the first one. */
    public static Map<String, Object> lastCollectedPreferences() {
        return WinPreferences.collected();
    }

    /** Clears that state, so the update tests do not depend on the order they run in. */
    public static void forgetCollectedPreferences() {
        WinPreferences.forgetCollected();
    }

    /**
     * Installs the {@code GwinPrefsCallbacks} table, which is idempotent and which
     * {@code WinApplication}'s static initializer has already done by the time any test that touches
     * the peer runs.
     */
    public static void installPreferencesCallback() {
        WinGlassNative.installPreferencesCallback();
    }

    /** Whether the table has been installed in this JVM. */
    public static boolean preferencesCallbackInstalled() {
        return WinGlassNative.preferencesCallbackInstalled();
    }

    /**
     * The {@code preferences_changed} upcall body itself, called with the {@code user} pointer the
     * table really carries ({@code NULL}). Driving this rather than {@link #updatePreferences} is
     * what makes the upcall's own contract testable - {@code Throwable} in, 0 or 1 out, never an
     * exception - just as {@link #notifyMenuCommand} does for the menu table.
     *
     * @return 1 if the preferences changed, 0 if not; {@code GlassApplication::WindowProc} turns that
     *         into {@code return 0} versus {@code DefWindowProc}
     */
    public static int firePreferencesChanged(int types) {
        return WinGlassNative.onPreferencesChanged(MemorySegment.NULL, types);
    }

    /**
     * {@link #firePreferencesChanged} with somebody to notify. A unit-test JVM has no
     * {@code Application} - {@code Application.run} never ran - so the peer's static
     * {@code firePreferencesChanged} finds nobody and the notify arm of the upcall is unreachable.
     * For the duration of the call the shim's peer is installed as {@code Application.GetApplication()}
     * with an {@code EventHandler} that forwards {@code handlePreferencesChanged} to {@code listener},
     * and both are put back afterwards.
     * <p>
     * The two fields are private to {@code com.sun.glass.ui.Application} and are written by
     * reflection, which needs no {@code --add-opens} because the shims are compiled into
     * javafx.graphics and a module's packages are open to itself. {@code setEventHandler} is not
     * used because it insists on the Glass event thread, which this JVM does not have.
     */
    public static synchronized int firePreferencesChangedInto(Consumer<Map<String, Object>> listener,
                                                              int types) {
        Application.EventHandler handler = new Application.EventHandler() {
            @Override
            public void handlePreferencesChanged(Map<String, Object> preferences) {
                listener.accept(preferences);
            }
        };
        try {
            Field application = Application.class.getDeclaredField("application");
            Field eventHandler = Application.class.getDeclaredField("eventHandler");
            application.setAccessible(true);
            eventHandler.setAccessible(true);
            WinApplication peer = applicationPeer();
            Object previousApplication = application.get(null);
            Object previousHandler = eventHandler.get(peer);
            application.set(null, peer);
            eventHandler.set(peer, handler);
            try {
                return firePreferencesChanged(types);
            } finally {
                eventHandler.set(peer, previousHandler);
                application.set(null, previousApplication);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Application.application / Application.eventHandler moved", e);
        }
    }

    /* The application: the message pump, its nested twin and the two scheduling exports. */

    /** {@code gwin_sizeof_app_callbacks}. */
    public static int sizeOfAppCallbacks() {
        return WinGlassNative.sizeOfAppCallbacks();
    }

    /**
     * Installs the {@code GwinAppCallbacks} table, which is idempotent and which
     * {@code WinApplication}'s static initializer has already done by the time any test that touches
     * the peer runs.
     */
    public static void installApplicationCallback() {
        WinGlassNative.installApplicationCallback();
    }

    /** Whether the table has been installed in this JVM. */
    public static boolean applicationCallbackInstalled() {
        return WinGlassNative.applicationCallbackInstalled();
    }

    /**
     * How many runnables the facade is holding for {@code glass.dll}. The registry is what replaced
     * the {@code JGlobalRef} each {@code Action} carried, so this is the measurable half of "no Java
     * reference crosses the ABI any more".
     */
    public static int runnableRegistrySize() {
        return WinGlassNative.runnableRegistrySize();
    }

    /**
     * Registers {@code runnable} exactly as the facade's scheduling calls do and returns the id the
     * library would have been given, so that {@link #runRunnableTarget} can play the callback.
     */
    public static long registerRunnable(Runnable runnable) {
        return WinGlassNative.registerRunnable(runnable);
    }

    /**
     * The {@code run_runnable} upcall body itself, with the {@code user} pointer the table really
     * carries ({@code NULL}). Driving it directly is what makes its one-shot, unknown-id and
     * {@code Throwable}-swallowing arms testable without a pump.
     */
    public static void runRunnableTarget(long id) {
        WinGlassNative.onRunRunnable(MemorySegment.NULL, id);
    }

    /**
     * {@code gwin_invoke_later} with no toolkit window. Four of the six loop/invoke exports can be
     * called from a unit-test JVM, which never has a toolkit window: this one and
     * {@code gwin_invoke_and_wait} answer {@link WinGlassNative#GWIN_ERR_NO_TOOLKIT} and run nothing;
     * {@code gwin_terminate_loop} is {@code IsWindow(NULL)} and so a no-op; and
     * {@code gwin_leave_nested_event_loop} only raises a flag that {@code EnterNestedEventLoop}
     * resets first. The two that cannot be called are {@code gwin_run_loop} and
     * {@code gwin_enter_nested_event_loop}, which both block on a message pump. Here the interesting
     * case is the one the registry has to clean up after.
     *
     * @return the number of registry entries this call left behind, which must be 0
     */
    public static int submitWithNoToolkit(Runnable runnable) {
        int before = WinGlassNative.runnableRegistrySize();
        WinGlassNative.submitForLaterInvocation(runnable);
        return WinGlassNative.runnableRegistrySize() - before;
    }

    /**
     * {@code gwin_invoke_and_wait} with no toolkit window. It must neither throw nor block - the JNI
     * did neither, because {@code ExecAction} returned early and silently.
     *
     * @return the number of registry entries this call left behind, which must be 0
     */
    public static int invokeAndWaitWithNoToolkit(Runnable runnable) {
        int before = WinGlassNative.runnableRegistrySize();
        WinGlassNative.invokeAndWait(runnable);
        return WinGlassNative.runnableRegistrySize() - before;
    }

    /**
     * {@code gwin_terminate_loop} with no toolkit window: {@code GetToolkitHWND()} is {@code NULL}
     * without an instance and {@code IsWindow(NULL)} is false, so the export returns without
     * destroying anything - the arm the JNI's {@code _terminateLoop} took on any call after the
     * window had gone. What this proves is the wrapper and its {@code FunctionDescriptor.ofVoid()}:
     * {@link #boundSymbols()} pins the symbol name, not the call.
     *
     * @return the number of registry entries this call left behind, which must be 0
     */
    public static int terminateLoopWithNoToolkit() {
        int before = WinGlassNative.runnableRegistrySize();
        WinGlassNative.terminateLoop();
        return WinGlassNative.runnableRegistrySize() - before;
    }

    /**
     * {@code gwin_leave_nested_event_loop} with no nested loop running: the export sets
     * {@code sm_shouldLeaveNestedLoop}, which {@code GlassApplication::EnterNestedEventLoop} resets
     * to false as its first statement, so nothing is left behind for any later pump. The JNI's
     * {@code _leaveNestedEventLoopImpl} did the same plus a value store that {@code WinApplication}
     * now keeps in Java.
     */
    public static void leaveNestedEventLoopWithNoLoop() {
        WinGlassNative.leaveNestedEventLoop();
    }

    /* The menus: the one WM_COMMAND upcall. */

    /** {@code gwin_sizeof_menu_callbacks}. */
    public static int sizeOfMenuCallbacks() {
        return WinGlassNative.sizeOfMenuCallbacks();
    }

    /**
     * Installs the {@code GwinMenuCallbacks} table, which is idempotent and which {@code WinMenuImpl}'s
     * static initializer has already done by the time any menu call in this class has run.
     */
    public static void installMenuCallback() {
        WinGlassNative.installMenuCallback();
    }

    /** Whether the table has been installed in this JVM. */
    public static boolean menuCallbackInstalled() {
        return WinGlassNative.menuCallbackInstalled();
    }

    /**
     * Runs {@code WinMenuImpl}'s static initializer, which is what installs the table. Constructing
     * one is the cheapest way to do it and is what the first real menu does; neither
     * {@code nativeMethodsOf("WinMenuImpl")} nor any {@code menu*} call in this class initializes
     * that class at all, so without this the timing under test would depend on which test ran first.
     */
    public static void touchWinMenuImpl() {
        WinMenuImpl unused = new WinMenuImpl();
    }

    /**
     * The {@code notify_command} upcall body itself, called with the {@code user} pointer the table
     * really carries ({@code NULL}). Driving this rather than {@code WinMenuImpl.notifyCommand}
     * directly is what makes the {@code Throwable}-to-0 arm testable, which is the half of the
     * contract that matters most: a throwing target counted as <em>unhandled</em> under JNI too,
     * because {@code CallStaticBooleanMethod} had already yielded {@code JNI_FALSE}.
     *
     * @return 1 if handled, 0 if not - never a wider value, and never compared with {@code == 1}
     */
    public static int notifyMenuCommand(int cmdID) {
        return WinGlassNative.onNotifyCommand(MemorySegment.NULL, cmdID);
    }

    /**
     * Registers a {@link WinMenuItemDelegate} with no owner and returns its command id. Its
     * {@code getOwner()} is {@code null}, so {@code notifyCommand} throws a
     * {@code NullPointerException} on it - which is the only way a unit-test JVM can reach the
     * failure arm of the upcall at all, because a real {@code MenuItem} cannot be built without a
     * Glass event thread.
     */
    public static int registerOwnerlessMenuItem() {
        return WinMenuItemDelegate.CommandIDManager.getID(new WinMenuItemDelegate(null));
    }

    /** Releases a command id taken by {@link #registerOwnerlessMenuItem()}. */
    public static void freeMenuCommandID(int cmdID) {
        WinMenuItemDelegate.CommandIDManager.freeID(cmdID);
    }

    /* The screen capture. */

    public static int robotCapture(int x, int y, int width, int height, int[] data) {
        return WinGlassNative.robotCapture(x, y, width, height, data);
    }

    /**
     * Drives {@code gwin_robot_capture} directly, past the argument checks {@link WinGlassNative} makes
     * in Java, so that the C's own copy of those four rejections is covered too. The buffer is copied
     * off heap and back, and {@code declaredLength} is what the C is told the buffer holds - which is
     * how a "buffer too short" rejection can be provoked without allocating a huge array.
     *
     * @param buffer the pixels, copied in and out; {@code null} passes {@code NULL} to the C
     */
    public static int captureDirect(int x, int y, int width, int height, int[] buffer, long declaredLength) {
        if (buffer == null) {
            return WinGlassNative.captureInto(x, y, width, height, MemorySegment.NULL, declaredLength);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pixels = arena.allocate(JAVA_INT, buffer.length);
            MemorySegment.copy(buffer, 0, pixels, JAVA_INT, 0, buffer.length);
            int status = WinGlassNative.captureInto(x, y, width, height, pixels, declaredLength);
            MemorySegment.copy(pixels, JAVA_INT, 0, buffer, 0, buffer.length);
            return status;
        }
    }

    /**
     * The names of every method {@link WinRobot} still declares {@code native}. The flip is only done
     * when this is empty, so the test asserts it rather than trusting a grep.
     */
    public static List<String> nativeMethodsOfWinRobot() {
        return nativeMethodsOf("WinRobot");
    }

    /**
     * The names of every method a Glass peer class of this package still declares {@code native}, in
     * alphabetical order. A peer's move off JNI is only finished when this is what the move promised, so
     * the tests assert it rather than trusting a grep.
     */
    public static List<String> nativeMethodsOf(String simpleName) {
        Class<?> peer = switch (simpleName) {
            case "WinRobot" -> WinRobot.class;
            case "WinTimer" -> WinTimer.class;
            case "WinCursor" -> WinCursor.class;
            case "WinPixels" -> WinPixels.class;
            case "WinApplication" -> WinApplication.class;
            case "WinMenuImpl" -> WinMenuImpl.class;
            case "WinView" -> WinView.class;
            case "WinGestureSupport" -> WinGestureSupport.class;
            case "WinWindow" -> WinWindow.class;
            case "WinSystemClipboard" -> WinSystemClipboard.class;
            case "WinDnDClipboard" -> WinDnDClipboard.class;
            case "WinCommonDialogs" -> WinCommonDialogs.class;
            default -> throw new IllegalArgumentException("not a peer class this shim exposes: "
                    + simpleName);
        };
        return Arrays.stream(peer.getDeclaredMethods())
                .filter(method -> Modifier.isNative(method.getModifiers()))
                .map(Method::getName)
                .sorted()
                .toList();
    }

    /** The Java path: a 1x1 {@code gwin_robot_capture} at coordinates that are already Windows pixels. */
    public static int robotPixelColorAt(int windowsX, int windowsY) {
        return WinGlassNative.robotPixelColorAt(windowsX, windowsY);
    }

    /* The multimedia timer: winmm, bound lazily behind the facade. */

    /**
     * Forces {@link WinGlassNative}'s lazy {@code winmm} block to bind, so that
     * {@link #boundSymbols()} is the whole list whatever order the tests run in. Called from the
     * {@code @BeforeAll} of both Windows Glass binding test classes.
     */
    public static void bindTimerSymbols() {
        int ignoredMinPeriod = WinGlassNative.timerMinPeriod();
    }

    public static int timerMinPeriod() {
        return WinGlassNative.timerMinPeriod();
    }

    public static int timerMaxPeriod() {
        return WinGlassNative.timerMaxPeriod();
    }

    /** {@code Timer::wTimerRes}: the resolution every {@code timeSetEvent} is given. */
    public static int timerResolution() {
        return WinGlassNative.timerResolution();
    }

    public static long timerStart(Runnable runnable, int period) {
        return WinGlassNative.timerStart(runnable, period);
    }

    public static void timerStop(long id) {
        WinGlassNative.timerStop(id);
    }

    /** How many timers are running: the registry that replaced the JNI global refs. */
    public static int timerRegistrySize() {
        return WinGlassNative.timerRegistrySize();
    }

    /** {@code Timer::timersCount}: the {@code timeBeginPeriod}/{@code timeEndPeriod} refcount. */
    public static int timerPeriodRefCount() {
        return WinGlassNative.timerPeriodRefCount();
    }

    /* WinCursor: the GDI cursor sequence and the two Win32 one-liners. */

    /**
     * Forces {@link WinGlassNative}'s lazy {@code gdi32} block to bind, so that
     * {@link #boundSymbols()} is the whole list whatever order the tests run in. Called from the
     * {@code @BeforeAll} of both Windows Glass binding test classes, after
     * {@link #bindTimerSymbols()}, which fixes the order of the two lazy blocks.
     */
    public static void bindCursorSymbols() {
        long cursor = WinGlassNative.cursorCreateCustom(1, 1, ByteBuffer.allocateDirect(4), 0, 0);
        if (cursor != 0) {
            destroyCursor(cursor);
        }
    }

    public static long cursorCreateCustom(int width, int height, Buffer pixels, int hotspotX,
                                          int hotspotY) {
        return WinGlassNative.cursorCreateCustom(width, height, pixels, hotspotX, hotspotY);
    }

    /** {@code user32!ShowCursor}, which returns the display counter this call left behind. */
    public static int showCursor(boolean show) {
        return WinGlassNative.showCursor(show);
    }

    /** {@code WinCursor.setVisible_impl}: the latched {@code ShowCursor} of {@code Cursor.setVisible}. */
    public static void cursorSetVisible(boolean visible) {
        WinCursor.setVisible_impl(visible);
    }

    /** {@code WinCursor.getBestSize_impl}, as {@code width, height}. */
    public static int[] cursorBestSize(int width, int height) {
        Size size = WinCursor.getBestSize_impl(width, height);
        return new int[] {size.width, size.height};
    }

    /* WinPixels: the four buffer paths of com.sun.glass.ui.Pixels. */

    /**
     * A {@code WinPixels} over {@code data}, built without an {@code Application}: the constructors
     * are {@code protected} in this package and {@code Pixels} makes no thread check
     * ({@code Pixels.java:87-135}).
     */
    public static Pixels newPixels(int width, int height, ByteBuffer data) {
        return new WinPixels(width, height, data);
    }

    public static Pixels newPixels(int width, int height, IntBuffer data) {
        return new WinPixels(width, height, data);
    }

    /**
     * {@code Pixels._fillDirectByteBuffer} without the {@code Application.checkEventThread()} that
     * {@code Pixels.asByteBuffer} ({@code Pixels.java:228}) would add on the way in.
     */
    public static void fillDirectByteBuffer(Pixels pixels, ByteBuffer bb) {
        ((WinPixels) pixels)._fillDirectByteBuffer(bb);
    }

    /** {@code WinPixels.getNativeFormat_impl()}: what {@code WinApplication.java:259-261} returns. */
    public static int pixelsNativeFormat() {
        return WinPixels.getNativeFormat_impl();
    }

    /* Test-only Win32 read-back: what the HCURSOR the facade built actually contains. */

    /**
     * {@code BITMAP} (wingdi.h) on x64: 32 bytes, four bytes of padding before the {@code LPVOID}.
     * Only {@code GetObjectW} fills it, and only this shim calls that, so the layout stays here
     * rather than in {@link WinGlassNative}.
     */
    private static final StructLayout BITMAP_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("bmType"),
            JAVA_INT.withName("bmWidth"),
            JAVA_INT.withName("bmHeight"),
            JAVA_INT.withName("bmWidthBytes"),
            JAVA_SHORT.withName("bmPlanes"),
            JAVA_SHORT.withName("bmBitsPixel"),
            MemoryLayout.paddingLayout(4),
            ADDRESS.withName("bmBits"));

    /**
     * The symbols only the read-back needs. They are bound here rather than in
     * {@link WinGlassNative} because product code must not carry a function it never calls, and
     * {@link #boundSymbols()} - which the tests assert exactly - must keep listing the product's
     * bindings only.
     */
    private static final class Readback {

        private static final Linker LINKER = Linker.nativeLinker();
        private static final SymbolLookup USER32 = library("user32.dll");
        private static final SymbolLookup GDI32 = library("gdi32.dll");

        /** {@code BOOL GetIconInfo(HICON hIcon, PICONINFO piconinfo)}. */
        private static final MethodHandle GET_ICON_INFO = bind(USER32, "GetIconInfo",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        /** {@code BOOL DestroyCursor(HCURSOR hCursor)}. */
        private static final MethodHandle DESTROY_CURSOR = bind(USER32, "DestroyCursor",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** {@code HDC GetDC(HWND hWnd)}. */
        private static final MethodHandle GET_DC = bind(USER32, "GetDC",
                FunctionDescriptor.of(ADDRESS, ADDRESS));

        /** {@code int ReleaseDC(HWND hWnd, HDC hDC)}. */
        private static final MethodHandle RELEASE_DC = bind(USER32, "ReleaseDC",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        /** {@code int GetObjectW(HANDLE h, int c, LPVOID pv)}. */
        private static final MethodHandle GET_OBJECT_W = bind(GDI32, "GetObjectW",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));

        /**
         * {@code int GetDIBits(HDC hdc, HBITMAP hbm, UINT start, UINT cLines, LPVOID lpvBits,
         * LPBITMAPINFO lpbmi, UINT usage)}.
         */
        private static final MethodHandle GET_DIBITS = bind(GDI32, "GetDIBits",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS,
                        ADDRESS, JAVA_INT));

        /** {@code BOOL DeleteObject(HGDIOBJ ho)}. */
        private static final MethodHandle DELETE_OBJECT = bind(GDI32, "DeleteObject",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /* What the window tests need and the product must not bind. */

        /** {@code BOOL DestroyIcon(HICON hIcon)} - the release the product never makes. */
        private static final MethodHandle DESTROY_ICON = bind(USER32, "DestroyIcon",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** {@code HWND GetDesktopWindow(void)}: a real window this thread has not captured. */
        private static final MethodHandle GET_DESKTOP_WINDOW = bind(USER32, "GetDesktopWindow",
                FunctionDescriptor.of(ADDRESS));

        /**
         * {@code HWND CreateWindowExW(DWORD dwExStyle, LPCWSTR lpClassName, LPCWSTR lpWindowName, DWORD dwStyle,
         * int X, int Y, int nWidth, int nHeight, HWND hWndParent, HMENU hMenu, HINSTANCE hInstance,
         * LPVOID lpParam)}: a hidden window of the calling thread, for the capture.
         */
        private static final MethodHandle CREATE_WINDOW_EX_W = bind(USER32, "CreateWindowExW",
                FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                        JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

        /** {@code BOOL DestroyWindow(HWND hWnd)}. */
        private static final MethodHandle DESTROY_WINDOW = bind(USER32, "DestroyWindow",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** {@code HWND SetCapture(HWND hWnd)}: the previous capture window. */
        private static final MethodHandle SET_CAPTURE = bind(USER32, "SetCapture",
                FunctionDescriptor.of(ADDRESS, ADDRESS));

        /** {@code BOOL ReleaseCapture(void)}. */
        private static final MethodHandle RELEASE_CAPTURE = bind(USER32, "ReleaseCapture",
                FunctionDescriptor.of(JAVA_INT));

        /** {@code BOOL GetCursorPos(LPPOINT)} - the oracle's own, not the facade's. */
        private static final MethodHandle GET_CURSOR_POS = bind(USER32, "GetCursorPos",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** {@code BOOL GetWindowRect(HWND, LPRECT)} - the oracle's own, not the facade's. */
        private static final MethodHandle GET_WINDOW_RECT = bind(USER32, "GetWindowRect",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        /** {@code HCURSOR LoadCursorW(HINSTANCE, LPCWSTR)} - the oracle's own, not the facade's. */
        private static final MethodHandle LOAD_CURSOR_W = bind(USER32, "LoadCursorW",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

        private Readback() {
        }

        @SuppressWarnings("restricted")
        private static SymbolLookup library(String fileName) {
            return SymbolLookup.libraryLookup(fileName, Arena.global());
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bind(SymbolLookup lookup, String name, FunctionDescriptor fd) {
            MemorySegment symbol = lookup.find(name).orElseThrow(
                    () -> new UnsatisfiedLinkError("missing native symbol: " + name));
            return LINKER.downcallHandle(symbol, fd);
        }
    }

    /**
     * Everything Windows will say about an {@code HCURSOR}: its hotspot, the size of its colour
     * bitmap and that bitmap's pixels, read back through {@code user32!GetIconInfo},
     * {@code gdi32!GetObjectW} and {@code gdi32!GetDIBits}. This is the parity oracle for the
     * {@code ICONINFO} sequence, and it is written against the {@code HCURSOR} rather than against
     * the implementation, so it holds whether that sequence is run by Java or by C.
     * <p>
     * The two bitmaps {@code GetIconInfo} hands back are copies the caller owns; they are deleted
     * here. The cursor itself is not - {@link #destroyCursor} is a separate call, so a test can
     * decide when to make it.
     *
     * @return {@code {xHotspot, yHotspot, width, height}} followed by {@code width * height} pixels,
     *         top-down, each a native-order int of the four bytes in the bitmap; or {@code null} if
     *         Windows does not recognise the handle
     */
    public static int[] readCursor(long cursor) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment iconInfo = arena.allocate(WinGlassNative.ICONINFO_LAYOUT);
            if (call(Readback.GET_ICON_INFO, MemorySegment.ofAddress(cursor), iconInfo) == 0) {
                return null;
            }
            MemorySegment mask = iconInfo.get(ADDRESS, offset("ICONINFO", "hbmMask"));
            MemorySegment colour = iconInfo.get(ADDRESS, offset("ICONINFO", "hbmColor"));
            try {
                MemorySegment bitmap = arena.allocate(BITMAP_LAYOUT);
                if (call(Readback.GET_OBJECT_W, colour, (int) BITMAP_LAYOUT.byteSize(), bitmap) == 0) {
                    return null;
                }
                int width = bitmap.get(JAVA_INT, BITMAP_LAYOUT.byteOffset(
                        PathElement.groupElement("bmWidth")));
                int height = bitmap.get(JAVA_INT, BITMAP_LAYOUT.byteOffset(
                        PathElement.groupElement("bmHeight")));
                int[] result = new int[4 + width * height];
                result[0] = iconInfo.get(JAVA_INT, offset("ICONINFO", "xHotspot"));
                result[1] = iconInfo.get(JAVA_INT, offset("ICONINFO", "yHotspot"));
                result[2] = width;
                result[3] = height;
                readPixels(arena, colour, width, height, result);
                return result;
            } finally {
                call(Readback.DELETE_OBJECT, colour);
                call(Readback.DELETE_OBJECT, mask);
            }
        }
    }

    /** {@code GetDIBits} into {@code result[4..]}, top-down and 32 bits per pixel. */
    private static void readPixels(Arena arena, MemorySegment colour, int width, int height,
                                   int[] result) {
        MemorySegment header = arena.allocate(WinGlassNative.BITMAPINFOHEADER_LAYOUT);
        header.set(JAVA_INT, offset("BITMAPINFOHEADER", "biSize"),
                (int) WinGlassNative.BITMAPINFOHEADER_LAYOUT.byteSize());
        header.set(JAVA_INT, offset("BITMAPINFOHEADER", "biWidth"), width);
        header.set(JAVA_INT, offset("BITMAPINFOHEADER", "biHeight"), -height);
        header.set(JAVA_SHORT, offset("BITMAPINFOHEADER", "biPlanes"), (short) 1);
        header.set(JAVA_SHORT, offset("BITMAPINFOHEADER", "biBitCount"), (short) 32);
        header.set(JAVA_INT, offset("BITMAPINFOHEADER", "biCompression"), WinGlassNative.BI_RGB);
        header.set(JAVA_INT, offset("BITMAPINFOHEADER", "biSizeImage"), width * height * 4);

        MemorySegment pixels = arena.allocate(JAVA_INT, (long) width * height);
        MemorySegment dc = callAddress(Readback.GET_DC, MemorySegment.NULL);
        try {
            call(Readback.GET_DIBITS, dc, colour, 0, height, pixels, header,
                    WinGlassNative.DIB_RGB_COLORS);
        } finally {
            call(Readback.RELEASE_DC, MemorySegment.NULL, dc);
        }
        MemorySegment.copy(pixels, JAVA_INT, 0, result, 4, width * height);
    }

    /** {@code user32!DestroyCursor}: the test destroys what it creates; product code never does. */
    public static boolean destroyCursor(long cursor) {
        return call(Readback.DESTROY_CURSOR, MemorySegment.ofAddress(cursor)) != 0;
    }

    private static int call(MethodHandle handle, Object... arguments) {
        try {
            return (int) handle.invokeWithArguments(arguments);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static MemorySegment callAddress(MethodHandle handle, Object... arguments) {
        try {
            return (MemorySegment) handle.invokeWithArguments(arguments);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /* GlassApplication.cpp: the two WRAPPER natives of WinApplication and their A/B. */

    /**
     * Binds the lazy {@code shlwapi} block, the third of the facade's three lazy library holders.
     * Call it from a {@code @BeforeAll} <b>after</b> {@link #bindTimerSymbols()} and
     * {@link #bindCursorSymbols()}, which fixes the order of all three for the exact-equality
     * assertion on {@link #boundSymbols()}.
     */
    public static void bindBrowserSymbols() {
        String ignoredBrowser = WinGlassNative.defaultBrowser();
    }

    /** {@code kernel32!GetVersion}, raw, so a test can check the decoding rather than the platform. */
    public static int windowsVersion() {
        return WinGlassNative.windowsVersion();
    }

    /** {@code Utils.h}'s {@code IS_WINVER_ATLEAST(major, minor)}, now Java over {@code GetVersion}. */
    public static boolean isWindowsVersionAtLeast(int major, int minor) {
        return WinGlassNative.isWindowsVersionAtLeast(major, minor);
    }

    /**
     * The same question asked through the {@link WinApplication} peer, which is what
     * {@code Application.supportsUnifiedWindows()} calls ({@code Application.java:710-714}). While the
     * JNI body existed ({@code GlassApplication.cpp:504-508}) this was the A/B oracle for
     * {@link #isWindowsVersionAtLeast}; after the flip it is the same Java through one more frame and
     * proves the override is wired up. {@code _supportsUnifiedWindows} is {@code protected}, which in
     * Java also means package-accessible, so no reflection is needed here.
     */
    public static synchronized boolean supportsUnifiedWindowsThroughPeer() {
        return applicationPeer()._supportsUnifiedWindows();
    }

    /** {@code AssocQueryStringW(ASSOCF_NONE, ASSOCSTR_COMMAND, ...)} on any association. */
    public static String assocQueryCommand(String association) {
        return WinGlassNative.assocQueryCommand(association);
    }

    /** {@code WinApplication._getDefaultBrowser}'s replacement: the {@code https} command line. */
    public static String defaultBrowser() {
        return WinGlassNative.defaultBrowser();
    }

    /**
     * The same question asked through {@code WinApplication._getDefaultBrowser}, the method
     * {@code _showDocument} calls ({@code WinApplication.java:314}). It is {@code private static},
     * which package access does not reach, so this goes through reflection - legal without an
     * {@code opens} because the shim is compiled into javafx.graphics itself, so the caller and the
     * declaring class share a module.
     * <p>
     * While the JNI body existed this was the A/B oracle for {@link #defaultBrowser()}; after the
     * flip it is the same Java through one more frame, and what it still proves is that the peer
     * routes to the facade.
     */
    public static String defaultBrowserThroughPeer() {
        try {
            Method method = WinApplication.class.getDeclaredMethod("_getDefaultBrowser");
            method.setAccessible(true);
            return (String) method.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("WinApplication._getDefaultBrowser is not callable", e);
        }
    }

    /* The FX to Windows transform, over an explicit screen list. */

    public static boolean fx2Win(List<Screen> screens, float[] point) {
        return WinScreenTransform.transform(screens, true, point);
    }

    public static boolean win2Fx(List<Screen> screens, float[] point) {
        return WinScreenTransform.transform(screens, false, point);
    }

    /* Menus: the eleven user32 wrappers that replaced GlassMenu.cpp's eleven functional JNI bodies. */

    public static long menuCreate() {
        return WinGlassNative.menuCreate();
    }

    public static void menuDestroy(long hMenu) {
        WinGlassNative.menuDestroy(hMenu);
    }

    public static boolean menuInsertItem(long hMenu, int pos, int cmdID, String title,
                                         boolean enabled, boolean checked) {
        return WinGlassNative.menuInsertItem(hMenu, pos, cmdID, title, enabled, checked);
    }

    public static boolean menuInsertSubmenu(long hMenu, int pos, long hSubmenu, String title,
                                            boolean enabled) {
        return WinGlassNative.menuInsertSubmenu(hMenu, pos, hSubmenu, title, enabled);
    }

    public static boolean menuInsertSeparator(long hMenu, int pos) {
        return WinGlassNative.menuInsertSeparator(hMenu, pos);
    }

    public static boolean menuRemoveAtPos(long hMenu, int pos) {
        return WinGlassNative.menuRemoveAtPos(hMenu, pos);
    }

    public static boolean menuSetItemTitle(long hMenu, int cmdID, String title) {
        return WinGlassNative.menuSetItemTitle(hMenu, cmdID, title);
    }

    public static boolean menuSetSubmenuTitle(long hMenu, long hSubmenu, String title) {
        return WinGlassNative.menuSetSubmenuTitle(hMenu, hSubmenu, title);
    }

    public static boolean menuEnableItem(long hMenu, int cmdID, boolean enable) {
        return WinGlassNative.menuEnableItem(hMenu, cmdID, enable);
    }

    public static boolean menuEnableSubmenu(long hMenu, long hSubmenu, boolean enable) {
        return WinGlassNative.menuEnableSubmenu(hMenu, hSubmenu, enable);
    }

    public static boolean menuCheckItem(long hMenu, int cmdID, boolean check) {
        return WinGlassNative.menuCheckItem(hMenu, cmdID, check);
    }

    /* Menu read-back: what user32 says a menu contains, whoever built it. */

    public static boolean menuIsMenu(long hMenu) {
        return WinGlassNative.menuIsMenu(hMenu);
    }

    public static int menuItemCount(long hMenu) {
        return WinGlassNative.menuItemCount(hMenu);
    }

    /** {@code {fType, fState, wID}}, or {@code null} when the item cannot be read. */
    public static int[] menuItemFlags(long hMenu, int pos) {
        return WinGlassNative.menuItemFlags(hMenu, pos);
    }

    public static long menuItemSubmenu(long hMenu, int pos) {
        return WinGlassNative.menuItemSubmenu(hMenu, pos);
    }

    public static String menuItemTitle(long hMenu, int pos) {
        return WinGlassNative.menuItemTitle(hMenu, pos);
    }

    /* The view: the gwin_view_* section, its two callback tables and the fifteen targets behind them. */

    /** {@code gwin_sizeof_view_callbacks}. */
    public static int sizeOfViewCallbacks() {
        return WinGlassNative.sizeOfViewCallbacks();
    }

    /** {@code gwin_sizeof_gesture_callbacks}. */
    public static int sizeOfGestureCallbacks() {
        return WinGlassNative.sizeOfGestureCallbacks();
    }

    /** Installs both tables, which is idempotent and which {@code WinApplication}'s initializer has done. */
    public static void installViewCallbacks() {
        WinGlassNative.installViewCallbacks();
    }

    public static boolean viewCallbacksInstalled() {
        return WinGlassNative.viewCallbacksInstalled();
    }

    /** Writes the production tables again; what {@link #fireIntoRecordingTable} restores with. */
    public static void reinstallViewCallbacks() {
        WinGlassNative.reinstallViewCallbacks();
    }

    /** The addresses of the fifteen installed stubs, view slots then gesture slots; 0 would be a NULL slot. */
    public static List<Long> installedViewCallbackStubAddresses() {
        return WinGlassNative.installedViewCallbackStubs().stream().map(MemorySegment::address).toList();
    }

    public static List<String> viewSlotNames() {
        return WinGlassNative.VIEW_SLOT_NAMES;
    }

    public static List<String> gestureSlotNames() {
        return WinGlassNative.GESTURE_SLOT_NAMES;
    }

    /**
     * {@code gwin_test_fire_callback} through the <em>production</em> table, with the calling thread
     * made the event thread for the duration: the slot lands in a facade target, then in
     * {@code WinView}'s dispatch, then in the registered view's {@code View.notify*}, several of
     * which check the thread.
     */
    public static long fireCallback(int slot, long viewId) {
        return asEventThread(() -> WinGlassNative.testFireCallback(slot, viewId));
    }

    /**
     * Runs {@code body} with the calling thread installed as the Glass event thread and the shim's
     * {@link WinApplication} peer installed as the {@code Application}, both restored afterwards.
     * A unit-test JVM has neither ({@code Application.run} never ran), and a {@code View} cannot be
     * constructed, given a handler, or asked anything without them: {@code View()} and most of its
     * methods call {@code Application.checkEventThread()}, and {@code shouldHandleEvent()} answers
     * false while {@code Application.GetApplication()} is null. Serialised with the other peer-installing
     * shim calls by this class's lock.
     */
    public static synchronized <T> T asEventThread(Supplier<T> body) {
        try {
            Field application = Application.class.getDeclaredField("application");
            Field eventThread = Application.class.getDeclaredField("eventThread");
            application.setAccessible(true);
            eventThread.setAccessible(true);
            WinApplication peer = applicationPeer();
            Object previousApplication = application.get(null);
            Object previousThread = eventThread.get(null);
            application.set(null, peer);
            eventThread.set(null, Thread.currentThread());
            try {
                return body.get();
            } finally {
                eventThread.set(null, previousThread);
                application.set(null, previousApplication);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Application.application / Application.eventThread moved", e);
        }
    }

    /** A {@code WinView} built with no toolkit, and the registry id it was given. */
    public record HeadlessView(long id, View view) {
    }

    /**
     * A real {@code WinView}, built headlessly: the constructor's {@code _create(Map)} registers the
     * view and calls {@code gwin_view_create}, which only allocates a {@code GlassView} with that id
     * and needs no toolkit. The id is the constructor's - nothing is registered here, or the view
     * would hold two entries - and the view is given {@code handler}, which is where everything a
     * {@code View.notify*} does becomes observable.
     * <p>
     * <b>It is never closed.</b> {@code gwin_view_close} marshals to the toolkit thread through
     * {@code ExecAction}, which without a toolkit returns without running the action and leaves its
     * return value uninitialised; the same holds for {@code get_x}, {@code get_y} and
     * {@code enter_fullscreen}. {@link #unregisterView} drops the registry entry, and the
     * {@code GlassView} lives for the rest of the JVM.
     */
    public static HeadlessView createHeadlessView(View.EventHandler handler) {
        return asEventThread(() -> {
            WinView view = new WinView();
            view.setEventHandler(handler);
            return new HeadlessView(view.viewId(), view);
        });
    }

    public static void unregisterView(long id) {
        WinView.unregister(id);
    }

    public static int viewRegistrySize() {
        return WinView.viewRegistrySize();
    }

    public static long viewIdOf(View view) {
        return ((WinView) view).viewId();
    }

    public static void setEventHandler(View view, View.EventHandler handler) {
        asEventThread(() -> {
            view.setEventHandler(handler);
            return null;
        });
    }

    /** {@code {getWidth(), getHeight()}}, which {@code notifyResize} sets and which check the thread. */
    public static int[] viewSize(View view) {
        return asEventThread(() -> new int[] {view.getWidth(), view.getHeight()});
    }

    /*
     * The targets driven directly, as the event thread, for the arms the hook's fixed pattern cannot
     * reach: a real ViewEvent type, a zero-length key array, NULL IME buffers with non-zero counts,
     * an out-parameter whose contents can be read back, and gesture arguments of the test's choosing.
     */

    public static void notifyViewTarget(long id, int type) {
        asEventThread(() -> {
            WinGlassNative.onNotifyView(id, type);
            return null;
        });
    }

    /** {@code notify_key} with a {@code NULL} pointer and {@code count} code units. */
    public static void notifyKeyTargetWithNullChars(long id, int type, int keyCode, int count, int modifiers) {
        asEventThread(() -> {
            WinGlassNative.onNotifyKey(id, type, keyCode, MemorySegment.NULL, count, modifiers);
            return null;
        });
    }

    /** {@code notify_input_method} with every buffer pointer {@code NULL} and the counts as given. */
    public static void notifyInputMethodTargetWithNullBuffers(long id, int textLen, int clauseCount, int attrCount,
                                                              int committedTextLength, int caretPos,
                                                              int visiblePos) {
        asEventThread(() -> {
            WinGlassNative.onNotifyInputMethod(id, MemorySegment.NULL, textLen, MemorySegment.NULL, clauseCount,
                    MemorySegment.NULL, MemorySegment.NULL, attrCount, committedTextLength, caretPos, visiblePos);
            return null;
        });
    }

    /** What {@code notify_ime_candidate_pos_request} answered and what it left in {@code out_xy}. */
    public record CandidatePos(int status, double x, double y) {
    }

    /**
     * The {@code notify_ime_candidate_pos_request} target with an {@code out_xy} pre-filled with
     * {@code NaN}, so that "untouched" is distinguishable from "written 0, 0".
     */
    public static CandidatePos imeCandidatePosRequestTarget(long id, int offset) {
        return asEventThread(() -> {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment out = arena.allocate(JAVA_DOUBLE, 2);
                out.setAtIndex(JAVA_DOUBLE, 0, Double.NaN);
                out.setAtIndex(JAVA_DOUBLE, 1, Double.NaN);
                int status = WinGlassNative.onNotifyImeCandidatePosRequest(id, offset, out);
                return new CandidatePos(status, out.getAtIndex(JAVA_DOUBLE, 0), out.getAtIndex(JAVA_DOUBLE, 1));
            }
        });
    }

    public static long getAccessibleTarget(long id) {
        return asEventThread(() -> WinGlassNative.onGetAccessible(id));
    }

    public static void gesturePerformedTarget(long id, int modifiers, boolean isDirect, boolean isInertia,
                                              int x, int y, int xAbs, int yAbs, float dx, float dy,
                                              float totalDx, float totalDy, float totalScale,
                                              float totalExpansion, float totalRotation) {
        asEventThread(() -> {
            WinGlassNative.onGesturePerformed(id, modifiers, isDirect ? 1 : 0, isInertia ? 1 : 0, x, y, xAbs,
                    yAbs, dx, dy, totalDx, totalDy, totalScale, totalExpansion, totalRotation);
            return null;
        });
    }

    public static void inertiaGestureFinishedTarget(long id) {
        asEventThread(() -> {
            WinGlassNative.onInertiaGestureFinished(id);
            return null;
        });
    }

    public static void notifyBeginTouchEventTarget(long id, int modifiers, boolean isDirect, int touchEventCount) {
        asEventThread(() -> {
            WinGlassNative.onNotifyBeginTouchEvent(id, modifiers, isDirect ? 1 : 0, touchEventCount);
            return null;
        });
    }

    public static void notifyNextTouchEventTarget(long id, int state, long touchId, int x, int y, int xAbs,
                                                  int yAbs) {
        asEventThread(() -> {
            WinGlassNative.onNotifyNextTouchEvent(id, state, touchId, x, y, xAbs, yAbs);
            return null;
        });
    }

    public static void notifyEndTouchEventTarget(long id) {
        asEventThread(() -> {
            WinGlassNative.onNotifyEndTouchEvent(id);
            return null;
        });
    }

    /* The recording table: the descriptor-arity oracle. */

    /** What a slot returned to {@code gwin_test_fire_callback}, and every argument it received, in order. */
    public record RecordedCall(long returned, List<Object> arguments) {
    }

    /** What the recording {@code notify_ime_candidate_pos_request} answers, distinct from 0 and 1. */
    public static final int RECORDING_CANDIDATE_STATUS = 7;

    /** What the recording {@code get_accessible} answers: bits above 32 set, so a truncated return shows. */
    public static final long RECORDING_ACCESSIBLE = 0x1_0000_1234L;

    /**
     * Installs a table of <em>recording</em> stubs - built from the facade's own
     * {@code FunctionDescriptor} constants, so what is proved is those descriptors and not a copy -
     * fires {@code slot} through {@code gwin_test_fire_callback}, and restores the production table
     * in a {@code finally}. The recorded tuple is what the C put on the stack as read through the
     * facade's descriptor; the header's pattern is what it should be. Pointer arguments are decoded
     * with the counts that accompany them and recorded as strings.
     */
    public static synchronized RecordedCall fireIntoRecordingTable(int slot, long viewId) {
        Recording.ARGUMENTS.clear();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment viewTable = arena.allocate(WinGlassNative.GWIN_VIEW_CALLBACKS_LAYOUT);
            WinGlassNative.fillCallbackTable(viewTable, WinGlassNative.GWIN_VIEW_CALLBACKS_LAYOUT,
                    WinGlassNative.VIEW_SLOT_NAMES, Recording.VIEW_STUBS);
            int viewStatus = (int) Recording.SET_VIEW_CALLBACKS.invokeExact(viewTable);
            MemorySegment gestureTable = arena.allocate(WinGlassNative.GWIN_GESTURE_CALLBACKS_LAYOUT);
            WinGlassNative.fillCallbackTable(gestureTable, WinGlassNative.GWIN_GESTURE_CALLBACKS_LAYOUT,
                    WinGlassNative.GESTURE_SLOT_NAMES, Recording.GESTURE_STUBS);
            int gestureStatus = (int) Recording.SET_GESTURE_CALLBACKS.invokeExact(gestureTable);
            if (viewStatus != WinGlassNative.GWIN_OK || gestureStatus != WinGlassNative.GWIN_OK) {
                throw new AssertionError("gwin_*_set_callbacks answered " + viewStatus + " / " + gestureStatus);
            }
            long returned = WinGlassNative.testFireCallback(slot, viewId);
            return new RecordedCall(returned, List.copyOf(Recording.ARGUMENTS));
        } catch (Throwable t) {
            throw new AssertionError("the recording table could not be installed or fired", t);
        } finally {
            WinGlassNative.reinstallViewCallbacks();
        }
    }

    /**
     * The recording targets, one per slot, each with exactly the method type the facade's descriptor
     * yields ({@code FunctionDescriptor.toMethodType()}); the stubs and the two installer binds are
     * built once, in {@link Arena#global()}, because a stub the library has been given must outlive
     * every later call, and the table is only ever swapped back to the production one, never cleared.
     * The installers are bound here rather than exposed by the product, for the reason
     * {@link Readback} gives: {@code boundSymbols()} lists the product's bindings only.
     */
    private static final class Recording {

        private static final Linker LINKER = Linker.nativeLinker();
        private static final List<Object> ARGUMENTS = new ArrayList<>();

        private static final MethodHandle SET_VIEW_CALLBACKS = bindGlass("gwin_view_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        private static final MethodHandle SET_GESTURE_CALLBACKS = bindGlass("gwin_gesture_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        private static final MemorySegment[] VIEW_STUBS = stubs(
                new String[] {"recordNotifyView", "recordNotifyResize", "recordNotifyRepaint", "recordNotifyMenu",
                    "recordNotifyKey", "recordNotifyMouse", "recordNotifyScroll", "recordNotifyInputMethod",
                    "recordNotifyImeCandidatePosRequest", "recordGetAccessible"},
                new FunctionDescriptor[] {WinGlassNative.NOTIFY_VIEW_FD, WinGlassNative.NOTIFY_RESIZE_FD,
                    WinGlassNative.NOTIFY_REPAINT_FD, WinGlassNative.NOTIFY_MENU_FD, WinGlassNative.NOTIFY_KEY_FD,
                    WinGlassNative.NOTIFY_MOUSE_FD, WinGlassNative.NOTIFY_SCROLL_FD,
                    WinGlassNative.NOTIFY_INPUT_METHOD_FD, WinGlassNative.NOTIFY_IME_CANDIDATE_POS_REQUEST_FD,
                    WinGlassNative.GET_ACCESSIBLE_FD});

        private static final MemorySegment[] GESTURE_STUBS = stubs(
                new String[] {"recordGesturePerformed", "recordInertiaGestureFinished",
                    "recordNotifyBeginTouchEvent", "recordNotifyNextTouchEvent", "recordNotifyEndTouchEvent"},
                new FunctionDescriptor[] {WinGlassNative.GESTURE_PERFORMED_FD,
                    WinGlassNative.INERTIA_GESTURE_FINISHED_FD, WinGlassNative.NOTIFY_BEGIN_TOUCH_EVENT_FD,
                    WinGlassNative.NOTIFY_NEXT_TOUCH_EVENT_FD, WinGlassNative.NOTIFY_END_TOUCH_EVENT_FD});

        private Recording() {
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bindGlass(String name, FunctionDescriptor fd) {
            MemorySegment symbol = SymbolLookup.loaderLookup().find(name).orElseThrow(
                    () -> new UnsatisfiedLinkError("missing native symbol: " + name + " in glass"));
            return LINKER.downcallHandle(symbol, fd);
        }

        @SuppressWarnings("restricted")
        private static MemorySegment[] stubs(String[] targets, FunctionDescriptor[] descriptors) {
            MemorySegment[] stubs = new MemorySegment[targets.length];
            for (int i = 0; i < targets.length; i++) {
                try {
                    MethodHandle target = MethodHandles.lookup().findStatic(Recording.class, targets[i],
                            descriptors[i].toMethodType());
                    stubs[i] = LINKER.upcallStub(target, descriptors[i], Arena.global());
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError("no recording target " + targets[i], e);
                }
            }
            return stubs;
        }

        private static void record(Object... values) {
            ARGUMENTS.addAll(Arrays.asList(values));
        }

        static void recordNotifyView(long viewId, int type) {
            record(viewId, type);
        }

        static void recordNotifyResize(long viewId, int width, int height) {
            record(viewId, width, height);
        }

        static void recordNotifyRepaint(long viewId, int x, int y, int width, int height) {
            record(viewId, x, y, width, height);
        }

        static void recordNotifyMenu(long viewId, int x, int y, int xAbs, int yAbs, int isKeyboardTrigger) {
            record(viewId, x, y, xAbs, yAbs, isKeyboardTrigger);
        }

        @SuppressWarnings("restricted")
        static void recordNotifyKey(long viewId, int type, int keyCode, MemorySegment chars, int count,
                                    int modifiers) {
            record(viewId, type, keyCode, new String(chars.reinterpret(count * 2L).toArray(JAVA_CHAR)), count,
                    modifiers);
        }

        static void recordNotifyMouse(long viewId, int type, int button, int x, int y, int xAbs, int yAbs,
                                      int modifiers, int isPopupTrigger, int isSynthesized) {
            record(viewId, type, button, x, y, xAbs, yAbs, modifiers, isPopupTrigger, isSynthesized);
        }

        static void recordNotifyScroll(long viewId, int x, int y, int xAbs, int yAbs, double deltaX,
                                       double deltaY, int modifiers, int lines, int chars, int defaultLines,
                                       int defaultChars, double xMultiplier, double yMultiplier) {
            record(viewId, x, y, xAbs, yAbs, deltaX, deltaY, modifiers, lines, chars, defaultLines, defaultChars,
                    xMultiplier, yMultiplier);
        }

        @SuppressWarnings("restricted")
        static void recordNotifyInputMethod(long viewId, MemorySegment text, int textLen,
                                            MemorySegment clauseBoundary, int clauseCount,
                                            MemorySegment attrBoundary, MemorySegment attrValue, int attrCount,
                                            int committedTextLength, int caretPos, int visiblePos) {
            record(viewId, new String(text.reinterpret(textLen * 2L).toArray(JAVA_CHAR)), textLen,
                    Arrays.toString(clauseBoundary.reinterpret((clauseCount + 1) * 4L).toArray(JAVA_INT)),
                    clauseCount,
                    Arrays.toString(attrBoundary.reinterpret((attrCount + 1) * 4L).toArray(JAVA_INT)),
                    Arrays.toString(attrValue.reinterpret(attrCount).toArray(JAVA_BYTE)), attrCount,
                    committedTextLength, caretPos, visiblePos);
        }

        @SuppressWarnings("restricted")
        static int recordNotifyImeCandidatePosRequest(long viewId, int offset, MemorySegment outXy) {
            MemorySegment out = outXy.reinterpret(2 * JAVA_DOUBLE.byteSize());
            out.setAtIndex(JAVA_DOUBLE, 0, 1.25);
            out.setAtIndex(JAVA_DOUBLE, 1, 2.5);
            record(viewId, offset);
            return RECORDING_CANDIDATE_STATUS;
        }

        static long recordGetAccessible(long viewId) {
            record(viewId);
            return RECORDING_ACCESSIBLE;
        }

        static void recordGesturePerformed(long viewId, int modifiers, int isDirect, int isInertia, int x, int y,
                                           int xAbs, int yAbs, float deltaX, float deltaY, float totalDeltaX,
                                           float totalDeltaY, float totalScale, float totalExpansion,
                                           float totalRotation) {
            record(viewId, modifiers, isDirect, isInertia, x, y, xAbs, yAbs, deltaX, deltaY, totalDeltaX,
                    totalDeltaY, totalScale, totalExpansion, totalRotation);
        }

        static void recordInertiaGestureFinished(long viewId) {
            record(viewId);
        }

        static void recordNotifyBeginTouchEvent(long viewId, int modifiers, int isDirect, int touchEventCount) {
            record(viewId, modifiers, isDirect, touchEventCount);
        }

        static void recordNotifyNextTouchEvent(long viewId, int state, long id, int x, int y, int xAbs,
                                               int yAbs) {
            record(viewId, state, id, x, y, xAbs, yAbs);
        }

        static void recordNotifyEndTouchEvent(long viewId) {
            record(viewId);
        }
    }

    /* The Pixels resolution of _uploadPixels, and the multi-click A/B. */

    /**
     * What {@code WinGlassNative.pixelBits} produced for {@code buffer}: whether it was {@code NULL},
     * its address, the address {@link MemorySegment#ofBuffer} would give the buffer <em>as
     * positioned</em> (direct buffers only, 0 otherwise), and the first {@code width * height} ints
     * behind it.
     */
    public record PixelBits(boolean isNull, long address, long positionedAddress, int[] ints) {
    }

    @SuppressWarnings("restricted")
    public static PixelBits pixelBits(Buffer buffer, int width, int height) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bits = WinGlassNative.pixelBits(arena, buffer, width, height);
            if (bits.equals(MemorySegment.NULL)) {
                return new PixelBits(true, 0L, 0L, null);
            }
            long positioned = buffer.isDirect() ? MemorySegment.ofBuffer(buffer).address() : 0L;
            int[] ints = bits.reinterpret((long) width * height * 4L).toArray(JAVA_INT);
            return new PixelBits(false, bits.address(), positioned, ints);
        }
    }

    /*
     * WinWindow: the table, the registry, a headless window, the twelve targets driven
     * directly, the _getAnchor WRAPPER with the OS support it needs, the cursor mapping and the icon.
     */

    public static int sizeOfWindowCallbacks() {
        return WinGlassNative.sizeOfWindowCallbacks();
    }

    public static void installWindowCallbacks() {
        WinGlassNative.installWindowCallbacks();
    }

    public static boolean windowCallbacksInstalled() {
        return WinGlassNative.windowCallbacksInstalled();
    }

    public static void reinstallWindowCallbacks() {
        WinGlassNative.reinstallWindowCallbacks();
    }

    /** The addresses of the twelve installed stubs in slot order; 0 would be a NULL slot. */
    public static List<Long> installedWindowCallbackStubAddresses() {
        return WinGlassNative.installedWindowCallbackStubs().stream().map(MemorySegment::address).toList();
    }

    public static List<String> windowSlotNames() {
        return WinGlassNative.WINDOW_SLOT_NAMES;
    }

    /**
     * The HWND a headless window carries. Not a window and never one: a USER handle's low 16 bits
     * index the handle table and entry 0 is reserved, so {@code IsWindow} answers FALSE for it and
     * {@code gwin_window_get_insets} - the one export that tests {@code IsWindow} before it looks
     * the GlassWindow up - answers 0. Non-zero, because {@code Window}'s constructor rejects 0.
     */
    public static final long HEADLESS_HWND = 0x8000_0000L;

    /** The value an out-parameter is pre-filled with, so that "untouched" is distinguishable from "written 0". */
    public static final int UNTOUCHED = Integer.MIN_VALUE + 7;

    /**
     * A real {@code WinWindow} with no C++ object behind it. {@code _createWindow} registers the
     * window as the product one does and answers {@link #HEADLESS_HWND} instead of calling
     * {@code gwin_window_create} - which, with no toolkit, would return indeterminate stack contents
     * and register a peer no {@code notify_dispose} will ever release. {@code _setView} answers
     * true and {@code _updateViewSize} does nothing, for the same reason, so that a headless
     * {@code WinView} can be attached and {@code nonClientHitTest} reach its handler. Every other
     * former native is the product forwarder; the ones that run here reach the C with the fake
     * handle, which the marshalled exports never act on without a toolkit and which
     * {@code gwin_window_get_insets} rejects with 0.
     * <p>
     * {@code notifyMoving} records its thirteen arguments and answers what the test set - or throws,
     * or, with {@link #realMoving}, runs {@code WinWindow}'s own body.
     */
    static final class HeadlessWinWindow extends WinWindow {

        final List<String> movingCalls = new ArrayList<>();
        int[] movingAnswer;
        RuntimeException movingFailure;
        boolean realMoving;

        HeadlessWinWindow(Window owner, Screen screen, int styleMask) {
            super(owner, screen, styleMask);
        }

        @Override
        protected long _createWindow(long ownerPtr, long screenPtr, int mask) {
            WinWindow.register(this);
            return HEADLESS_HWND;
        }

        @Override
        protected boolean _setView(long ptr, View view) {
            return true;
        }

        @Override
        protected void _updateViewSize(long ptr) {
        }

        @Override
        protected int[] notifyMoving(int x, int y, int w, int h, float fxX, float fxY, int anchorX, int anchorY,
                                     int resizeMode, int iLft, int iTop, int iRgt, int iBot) {
            if (realMoving) {
                return super.notifyMoving(x, y, w, h, fxX, fxY, anchorX, anchorY, resizeMode, iLft, iTop, iRgt, iBot);
            }
            movingCalls.add("moving" + Arrays.toString(new Object[] {x, y, w, h, fxX, fxY, anchorX, anchorY,
                resizeMode, iLft, iTop, iRgt, iBot}));
            if (movingFailure != null) {
                throw movingFailure;
            }
            return movingAnswer;
        }
    }

    /** A headless {@code WinWindow}, and the registry id its constructor gave it. */
    public record HeadlessWindow(long id, Window window) {
    }

    /**
     * A {@link HeadlessWinWindow} of {@code styleMask} on {@code screen}, with {@code handler}
     * installed, built as the event thread. Never closed - {@code gwin_window_close} needs a toolkit -
     * so the test drops the registry entry with {@link #unregisterWindow} or through the
     * {@code notify_dispose} target.
     */
    public static HeadlessWindow createHeadlessWindow(Window.EventHandler handler, Screen screen, int styleMask) {
        return asEventThread(() -> {
            HeadlessWinWindow window = new HeadlessWinWindow(null, screen, styleMask);
            window.setEventHandler(handler);
            return new HeadlessWindow(window.nativeId(), window);
        });
    }

    /** {@code window.setView(view)} as the event thread; {@code null} detaches. */
    public static void attachView(Window window, View view) {
        asEventThread(() -> {
            window.setView(view);
            return null;
        });
    }

    public static void unregisterWindow(long id) {
        WinWindow.unregister(id);
    }

    public static int windowRegistrySize() {
        return WinWindow.windowRegistrySize();
    }

    public static long windowIdOf(Window window) {
        return ((WinWindow) window).nativeId();
    }

    public static void setMovingAnswer(Window window, int[] answer) {
        ((HeadlessWinWindow) window).movingAnswer = answer;
    }

    public static void setMovingFailure(Window window, RuntimeException failure) {
        ((HeadlessWinWindow) window).movingFailure = failure;
    }

    public static void setRealMoving(Window window, boolean real) {
        ((HeadlessWinWindow) window).realMoving = real;
    }

    public static List<String> movingCalls(Window window) {
        return List.copyOf(((HeadlessWinWindow) window).movingCalls);
    }

    /** What a {@code Window} publishes about itself, read as the event thread because most getters check it. */
    public record WindowState(int x, int y, int width, int height, boolean focused, boolean closed,
                              long rawHandle, long nativeHandle, float platformScaleX, float platformScaleY,
                              float outputScaleX, float outputScaleY) {
    }

    public static WindowState windowState(Window window) {
        return asEventThread(() -> new WindowState(window.getX(), window.getY(), window.getWidth(),
                window.getHeight(), window.isFocused(), window.isClosed(), window.getRawHandle(),
                window.getNativeHandle(), window.getPlatformScaleX(), window.getPlatformScaleY(),
                window.getOutputScaleX(), window.getOutputScaleY()));
    }

    /* The twelve targets, driven directly as the event thread. */

    public static void windowCloseTarget(long id) {
        asEventThread(() -> {
            WinGlassNative.onWindowClose(id);
            return null;
        });
    }

    public static void windowDestroyTarget(long id) {
        asEventThread(() -> {
            WinGlassNative.onWindowDestroy(id);
            return null;
        });
    }

    /**
     * What {@code notify_moving} answered and what it left in {@code out_bounds}, pre-filled with
     * {@link #UNTOUCHED}.
     */
    public record MovingResult(int status, int[] bounds) {
    }

    public static MovingResult windowMovingTarget(long id, int x, int y, int w, int h, float fxX, float fxY,
                                                  int anchorX, int anchorY, int resizeMode, int iLft, int iTop,
                                                  int iRgt, int iBot) {
        return asEventThread(() -> {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment out = arena.allocate(JAVA_INT, 4);
                for (int i = 0; i < 4; i++) {
                    out.setAtIndex(JAVA_INT, i, UNTOUCHED);
                }
                int status = WinGlassNative.onWindowMoving(id, x, y, w, h, fxX, fxY, anchorX, anchorY, resizeMode,
                        iLft, iTop, iRgt, iBot, out);
                return new MovingResult(status, out.toArray(JAVA_INT));
            }
        });
    }

    public static void windowMoveTarget(long id, int x, int y) {
        asEventThread(() -> {
            WinGlassNative.onWindowMove(id, x, y);
            return null;
        });
    }

    public static void windowResizeTarget(long id, int type, int width, int height) {
        asEventThread(() -> {
            WinGlassNative.onWindowResize(id, type, width, height);
            return null;
        });
    }

    public static void windowScaleChangedTarget(long id, float platformX, float platformY, float outputX,
                                                float outputY) {
        asEventThread(() -> {
            WinGlassNative.onWindowScaleChanged(id, platformX, platformY, outputX, outputY);
            return null;
        });
    }

    public static void windowFocusTarget(long id, int event) {
        asEventThread(() -> {
            WinGlassNative.onWindowFocus(id, event);
            return null;
        });
    }

    public static void windowFocusDisabledTarget(long id) {
        asEventThread(() -> {
            WinGlassNative.onWindowFocusDisabled(id);
            return null;
        });
    }

    public static void windowFocusUngrabTarget(long id) {
        asEventThread(() -> {
            WinGlassNative.onWindowFocusUngrab(id);
            return null;
        });
    }

    public static void windowDelegatePtrTarget(long id, long delegateHwnd) {
        asEventThread(() -> {
            WinGlassNative.onWindowDelegatePtr(id, MemorySegment.ofAddress(delegateHwnd));
            return null;
        });
    }

    public static int windowNonClientHitTestTarget(long id, int x, int y) {
        return asEventThread(() -> WinGlassNative.onWindowNonClientHitTest(id, x, y));
    }

    public static void windowDisposeTarget(long id) {
        asEventThread(() -> {
            WinGlassNative.onWindowDispose(id);
            return null;
        });
    }

    /* The window test hook: gwin_test_fire_window_callback through the production table and a recording table. */

    /** What the window hook returned and what {@code out_bounds} held afterwards; {@code null} when none was passed. */
    public record WindowFire(long returned, int[] bounds) {
    }

    /**
     * {@code gwin_test_fire_window_callback} through the <em>production</em> table, as the event
     * thread, with a four-int {@code out_bounds} pre-filled with {@link #UNTOUCHED} - or {@code NULL}
     * when {@code withOut} is false, which slot 2 rejects and every other slot ignores. The hook is
     * bound here and not in the facade, as the recording installers are: the product never calls it,
     * and {@code boundSymbols()} lists the product's bindings only.
     */
    public static WindowFire fireWindowCallback(int slot, long windowId, boolean withOut) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = withOut ? untouchedBounds(arena) : MemorySegment.NULL;
            long returned = asEventThread(() -> WindowRecording.fire(slot, windowId, out));
            return new WindowFire(returned, withOut ? out.toArray(JAVA_INT) : null);
        }
    }

    /**
     * {@code gwin_test_fire_window_callback} with <em>no</em> table installed:
     * {@code gwin_window_set_callbacks(NULL)} clears it, the slot is fired, and the production table
     * is written back in a {@code finally}.
     */
    public static synchronized long fireWindowCallbackWithNoTable(int slot, long windowId) {
        try (Arena arena = Arena.ofConfined()) {
            int status = (int) WindowRecording.SET_WINDOW_CALLBACKS.invokeExact(MemorySegment.NULL);
            if (status != WinGlassNative.GWIN_OK) {
                throw new AssertionError("gwin_window_set_callbacks(NULL) answered " + status);
            }
            return WindowRecording.fire(slot, windowId, untouchedBounds(arena));
        } catch (Throwable t) {
            throw new AssertionError("the window table could not be cleared or fired", t);
        } finally {
            WinGlassNative.reinstallWindowCallbacks();
        }
    }

    /**
     * What a recording window slot answered, every argument it received in order, and
     * {@code out_bounds} afterwards.
     */
    public record RecordedWindowCall(long returned, List<Object> arguments, int[] bounds) {
    }

    /** What the recording {@code notify_moving} writes into {@code out_bounds} when asked to: not the sentinel. */
    public static final int[] RECORDING_BOUNDS = {-7, 8, 900, 1000};

    /** What the recording {@code notify_moving} answers when it wrote the bounds: neither production 0 nor 1. */
    public static final int RECORDING_MOVING_STATUS = 7;

    /** What the recording {@code non_client_hit_test} answers: not an {@code HT*} value. */
    public static final int RECORDING_HIT_TEST = 0x7C7C;

    /**
     * Installs a recording window table built from the facade's own twelve descriptor constants -
     * so what is proved is those descriptors and not a copy - fires {@code slot} through
     * {@code gwin_test_fire_window_callback}, and restores the production table in a {@code finally}.
     * {@code out_bounds} is a four-int block pre-filled with {@link #UNTOUCHED}, or {@code NULL} when
     * {@code withOut} is false. The recording {@code notify_moving} writes {@link #RECORDING_BOUNDS}
     * and answers {@link #RECORDING_MOVING_STATUS} when {@code movingWrites}, else writes nothing and
     * answers 0 - the two things a production target can do with the buffer. The recorded tuple is
     * what the C put on the stack as read through the facade's descriptor; the header's pattern is
     * what it should be.
     */
    public static synchronized RecordedWindowCall fireWindowIntoRecordingTable(int slot, long windowId,
                                                                               boolean withOut, boolean movingWrites) {
        WindowRecording.ARGUMENTS.clear();
        WindowRecording.movingWrites = movingWrites;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(WinGlassNative.GWIN_WINDOW_CALLBACKS_LAYOUT);
            WinGlassNative.fillCallbackTable(table, WinGlassNative.GWIN_WINDOW_CALLBACKS_LAYOUT,
                    WinGlassNative.WINDOW_SLOT_NAMES, WindowRecording.STUBS);
            int status = (int) WindowRecording.SET_WINDOW_CALLBACKS.invokeExact(table);
            if (status != WinGlassNative.GWIN_OK) {
                throw new AssertionError("gwin_window_set_callbacks answered " + status);
            }
            MemorySegment out = withOut ? untouchedBounds(arena) : MemorySegment.NULL;
            long returned = (long) WindowRecording.FIRE.invokeExact(slot, windowId, out);
            return new RecordedWindowCall(returned, List.copyOf(WindowRecording.ARGUMENTS),
                    withOut ? out.toArray(JAVA_INT) : null);
        } catch (Throwable t) {
            throw new AssertionError("the recording window table could not be installed or fired", t);
        } finally {
            WinGlassNative.reinstallWindowCallbacks();
        }
    }

    /** A four-int block filled with {@link #UNTOUCHED}: what {@code windowMovingTarget} hands the target too. */
    private static MemorySegment untouchedBounds(Arena arena) {
        MemorySegment out = arena.allocate(JAVA_INT, 4);
        for (int i = 0; i < 4; i++) {
            out.setAtIndex(JAVA_INT, i, UNTOUCHED);
        }
        return out;
    }

    /**
     * The recording targets of the window table, each with exactly the method type the facade's
     * descriptor yields ({@code FunctionDescriptor.toMethodType()}); the stubs, the installer bind
     * and the hook bind are built once, in {@link Arena#global()}, as {@link Recording} does for the
     * view tables. A recording target never throws: a throwable escaping an upcall stub ends the
     * JVM, so the "target threw" arm of {@code notify_moving} is proved through the production table
     * with the headless window's {@code movingFailure} instead.
     */
    private static final class WindowRecording {

        private static final Linker LINKER = Linker.nativeLinker();
        private static final List<Object> ARGUMENTS = new ArrayList<>();

        /** Whether the recording {@code notify_moving} writes {@link #RECORDING_BOUNDS}; set under the shim's lock. */
        private static boolean movingWrites;

        private static final MethodHandle SET_WINDOW_CALLBACKS = bindGlass("gwin_window_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** {@code int64_t gwin_test_fire_window_callback(int32_t slot, int64_t window_id, int32_t* out_bounds)}. */
        private static final MethodHandle FIRE = bindGlass("gwin_test_fire_window_callback",
                FunctionDescriptor.of(java.lang.foreign.ValueLayout.JAVA_LONG, JAVA_INT,
                        java.lang.foreign.ValueLayout.JAVA_LONG, ADDRESS));

        private static final MemorySegment[] STUBS = stubs(
                new String[] {"recordNotifyClose", "recordNotifyDestroy", "recordNotifyMoving", "recordNotifyMove",
                    "recordNotifyResize", "recordNotifyScaleChanged", "recordNotifyFocus",
                    "recordNotifyFocusDisabled", "recordNotifyFocusUngrab", "recordNotifyDelegatePtr",
                    "recordNonClientHitTest", "recordNotifyDispose"},
                new FunctionDescriptor[] {WinGlassNative.WINDOW_NOTIFY_CLOSE_FD,
                    WinGlassNative.WINDOW_NOTIFY_DESTROY_FD, WinGlassNative.WINDOW_NOTIFY_MOVING_FD,
                    WinGlassNative.WINDOW_NOTIFY_MOVE_FD, WinGlassNative.WINDOW_NOTIFY_RESIZE_FD,
                    WinGlassNative.WINDOW_NOTIFY_SCALE_CHANGED_FD, WinGlassNative.WINDOW_NOTIFY_FOCUS_FD,
                    WinGlassNative.WINDOW_NOTIFY_FOCUS_DISABLED_FD, WinGlassNative.WINDOW_NOTIFY_FOCUS_UNGRAB_FD,
                    WinGlassNative.WINDOW_NOTIFY_DELEGATE_PTR_FD, WinGlassNative.WINDOW_NON_CLIENT_HIT_TEST_FD,
                    WinGlassNative.WINDOW_NOTIFY_DISPOSE_FD});

        private WindowRecording() {
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bindGlass(String name, FunctionDescriptor fd) {
            MemorySegment symbol = SymbolLookup.loaderLookup().find(name).orElseThrow(
                    () -> new UnsatisfiedLinkError("missing native symbol: " + name + " in glass"));
            return LINKER.downcallHandle(symbol, fd);
        }

        @SuppressWarnings("restricted")
        private static MemorySegment[] stubs(String[] targets, FunctionDescriptor[] descriptors) {
            MemorySegment[] stubs = new MemorySegment[targets.length];
            for (int i = 0; i < targets.length; i++) {
                try {
                    MethodHandle target = MethodHandles.lookup().findStatic(WindowRecording.class, targets[i],
                            descriptors[i].toMethodType());
                    stubs[i] = LINKER.upcallStub(target, descriptors[i], Arena.global());
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError("no recording target " + targets[i], e);
                }
            }
            return stubs;
        }

        /** The hook, whichever table is installed. */
        static long fire(int slot, long windowId, MemorySegment outBounds) {
            try {
                return (long) FIRE.invokeExact(slot, windowId, outBounds);
            } catch (Throwable t) {
                throw new AssertionError("gwin_test_fire_window_callback failed", t);
            }
        }

        private static void record(Object... values) {
            ARGUMENTS.addAll(Arrays.asList(values));
        }

        static void recordNotifyClose(long windowId) {
            record(windowId);
        }

        static void recordNotifyDestroy(long windowId) {
            record(windowId);
        }

        static int recordNotifyMoving(long windowId, int x, int y, int w, int h, float fxX, float fxY, int anchorX,
                                      int anchorY, int resizeMode, int insetLeft, int insetTop, int insetRight,
                                      int insetBottom, MemorySegment outBounds) {
            record(windowId, x, y, w, h, fxX, fxY, anchorX, anchorY, resizeMode, insetLeft, insetTop, insetRight,
                    insetBottom);
            if (!movingWrites) {
                return 0;
            }
            MemorySegment.copy(RECORDING_BOUNDS, 0, bounded(outBounds, 4 * JAVA_INT.byteSize()), JAVA_INT, 0L, 4);
            return RECORDING_MOVING_STATUS;
        }

        static void recordNotifyMove(long windowId, int x, int y) {
            record(windowId, x, y);
        }

        static void recordNotifyResize(long windowId, int type, int width, int height) {
            record(windowId, type, width, height);
        }

        static void recordNotifyScaleChanged(long windowId, float platformX, float platformY, float outputX,
                                             float outputY) {
            record(windowId, platformX, platformY, outputX, outputY);
        }

        static void recordNotifyFocus(long windowId, int event) {
            record(windowId, event);
        }

        static void recordNotifyFocusDisabled(long windowId) {
            record(windowId);
        }

        static void recordNotifyFocusUngrab(long windowId) {
            record(windowId);
        }

        static void recordNotifyDelegatePtr(long windowId, MemorySegment delegateHwnd) {
            record(windowId, delegateHwnd.address());
        }

        static int recordNonClientHitTest(long windowId, int x, int y) {
            record(windowId, x, y);
            return RECORDING_HIT_TEST;
        }

        static void recordNotifyDispose(long windowId) {
            record(windowId);
        }
    }

    /* The downcalls that can run without a toolkit, and the WRAPPER. */

    public static long windowGetInsets(long hwnd) {
        return WinGlassNative.windowGetInsets(hwnd);
    }

    public static void windowSetIcon(long hwnd, long hicon) {
        WinGlassNative.windowSetIcon(hwnd, hicon);
    }

    /**
     * Every export that marshals through {@code ExecAction}, once each with {@code hwnd}. With no
     * toolkit {@code ExecAction} returns before running the action, so nothing happens and the
     * returns are indeterminate - they are discarded here, and a test may only assert that
     * the calls link and come back.
     *
     * @return how many were invoked
     */
    public static int invokeEveryMarshalledWindowOp(long hwnd) {
        int count = 0;
        WinGlassNative.windowCreate(0L, 0L, 0, Long.MAX_VALUE);
        count++;
        WinGlassNative.windowClose(hwnd);
        count++;
        WinGlassNative.windowSetView(hwnd, 0L);
        count++;
        WinGlassNative.windowUpdateViewSize(hwnd);
        count++;
        WinGlassNative.windowSetMenubar(hwnd, 0L);
        count++;
        WinGlassNative.windowSetLevel(hwnd, 1);
        count++;
        WinGlassNative.windowSetFocusable(hwnd, true);
        count++;
        WinGlassNative.windowSetEnabled(hwnd, true);
        count++;
        WinGlassNative.windowSetAlpha(hwnd, 1f);
        count++;
        WinGlassNative.windowSetDarkFrame(hwnd, false);
        count++;
        WinGlassNative.windowSetBounds(hwnd, 0, 0, false, false, 0, 0, 0, 0, 0f, 0f);
        count++;
        WinGlassNative.windowSetTitle(hwnd, "WinWindowNativeTest");
        count++;
        WinGlassNative.windowSetResizable(hwnd, true);
        count++;
        WinGlassNative.windowSetVisible(hwnd, false);
        count++;
        WinGlassNative.windowRequestFocus(hwnd, 542);
        count++;
        WinGlassNative.windowGrabFocus(hwnd);
        count++;
        WinGlassNative.windowUngrabFocus(hwnd);
        count++;
        WinGlassNative.windowMinimize(hwnd, false);
        count++;
        WinGlassNative.windowMaximize(hwnd, false, false);
        count++;
        WinGlassNative.windowSetMinSize(hwnd, 0, 0);
        count++;
        WinGlassNative.windowSetMaxSize(hwnd, 0, 0);
        count++;
        WinGlassNative.windowToFront(hwnd);
        count++;
        WinGlassNative.windowToBack(hwnd);
        count++;
        WinGlassNative.windowSetCursor(hwnd, 0L);
        count++;
        WinGlassNative.windowShowSystemMenu(hwnd, 0, 0);
        count++;
        return count;
    }

    public static long windowAnchor(long hwnd) {
        return WinGlassNative.windowAnchor(hwnd);
    }

    public static long packAnchor(int deltaX, int deltaY) {
        return WinGlassNative.packAnchor(deltaX, deltaY);
    }

    public static long anchorNoCapture() {
        return WinWindow.ANCHOR_NO_CAPTURE;
    }

    /* The OS support the anchor tests need: a window of this thread, the capture, and the two reads. */

    public static long desktopWindow() {
        try {
            MemorySegment hwnd = (MemorySegment) Readback.GET_DESKTOP_WINDOW.invokeExact();
            return hwnd.address();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * A hidden {@code STATIC} popup window owned by the calling thread, or 0; destroy it with
     * {@link #destroyWindow}.
     */
    public static long createHiddenWindow() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment hwnd = (MemorySegment) Readback.CREATE_WINDOW_EX_W.invokeExact(0,
                    arena.allocateFrom("STATIC", StandardCharsets.UTF_16LE), MemorySegment.NULL, WS_POPUP,
                    0, 0, 64, 64, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
            return hwnd.address();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /** winuser.h {@code WS_POPUP}. */
    private static final int WS_POPUP = 0x8000_0000;

    public static boolean destroyWindow(long hwnd) {
        try {
            return (int) Readback.DESTROY_WINDOW.invokeExact(MemorySegment.ofAddress(hwnd)) != 0;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /** {@code SetCapture(hwnd)}: the window that had it before, or 0. */
    public static long setCapture(long hwnd) {
        try {
            MemorySegment previous = (MemorySegment) Readback.SET_CAPTURE.invokeExact(MemorySegment.ofAddress(hwnd));
            return previous.address();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static boolean releaseCapture() {
        try {
            return (int) Readback.RELEASE_CAPTURE.invokeExact() != 0;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /** {@code GetCursorPos} through the read-back's own bind: {@code {x, y}}, or {@code null} if it failed. */
    public static int[] cursorPosNow() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment point = arena.allocate(WinGlassNative.POINT_LAYOUT);
            if ((int) Readback.GET_CURSOR_POS.invokeExact(point) == 0) {
                return null;
            }
            return new int[] {point.get(JAVA_INT, offset("POINT", "x")), point.get(JAVA_INT, offset("POINT", "y"))};
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /** {@code GetWindowRect} through the read-back's own bind: {@code {left, top, right, bottom}}, or {@code null}. */
    public static int[] windowRectNow(long hwnd) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rect = arena.allocate(WinGlassNative.RECT_LAYOUT);
            if ((int) Readback.GET_WINDOW_RECT.invokeExact(MemorySegment.ofAddress(hwnd), rect) == 0) {
                return null;
            }
            return rect.toArray(JAVA_INT);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /* The cursor mapping and the three-step load. */

    public static int cursorOrdinal(int type) {
        return WinCursor.systemCursorOrdinal(type);
    }

    public static String cursorName(int type) {
        return WinCursor.systemCursorName(type);
    }

    public static long systemCursorHandle(int type) {
        return WinCursor.systemCursorHandle(type);
    }

    /** {@code WinCursor.nativeHandleFor} over a real {@code WinCursor} of {@code type}, as the event thread. */
    public static long cursorHandleFor(int type) {
        return asEventThread(() -> WinCursor.nativeHandleFor(new WinCursor(type)));
    }

    public static long cursorHandleForNull() {
        return WinCursor.nativeHandleFor(null);
    }

    /**
     * {@code WinCursor.nativeHandleFor} over a custom {@code WinCursor} built from {@code pixels}
     * (premultiplied BGRA, {@code width * height * 4} bytes): the {@code HCURSOR} its constructor
     * made, which the test reads back with {@link #readCursor} and releases with {@link #destroyCursor}.
     */
    public static long customCursorHandle(int hotspotX, int hotspotY, int width, int height, ByteBuffer pixels) {
        return asEventThread(() -> WinCursor.nativeHandleFor(
                new WinCursor(hotspotX, hotspotY, new WinPixels(width, height, pixels))));
    }

    public static long loadSystemCursor(int ordinal) {
        return WinGlassNative.loadSystemCursor(ordinal);
    }

    public static long loadSystemCursorByName(String name) {
        return WinGlassNative.loadSystemCursor(name);
    }

    public static long moduleHandle(String fileName) {
        return WinGlassNative.moduleHandle(fileName);
    }

    /** {@code LoadCursorW(module, MAKEINTRESOURCE(ordinal))} through the read-back's own bind: the oracle. */
    public static long loadCursorDirect(long module, int ordinal) {
        try {
            MemorySegment cursor = (MemorySegment) Readback.LOAD_CURSOR_W.invokeExact(MemorySegment.ofAddress(module),
                    MemorySegment.ofAddress(ordinal));
            return cursor.address();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /* The icon. */

    public static long iconCreate(int width, int height, Buffer pixels) {
        return WinGlassNative.iconCreate(width, height, pixels);
    }

    /**
     * {@code ICONINFO.fIcon} of {@code handle} through {@code GetIconInfo}: 1 for an icon, 0 for a
     * cursor, -1 if unknown.
     */
    public static int iconKind(long handle) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment iconInfo = arena.allocate(WinGlassNative.ICONINFO_LAYOUT);
            if (call(Readback.GET_ICON_INFO, MemorySegment.ofAddress(handle), iconInfo) == 0) {
                return -1;
            }
            call(Readback.DELETE_OBJECT, iconInfo.get(ADDRESS, offset("ICONINFO", "hbmColor")));
            call(Readback.DELETE_OBJECT, iconInfo.get(ADDRESS, offset("ICONINFO", "hbmMask")));
            return iconInfo.get(JAVA_INT, offset("ICONINFO", "fIcon"));
        }
    }

    public static boolean destroyIcon(long handle) {
        try {
            return (int) Readback.DESTROY_ICON.invokeExact(MemorySegment.ofAddress(handle)) != 0;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /* The clipboard, drag-and-drop and dialog section: two tables, sixteen targets, nineteen exports. */

    public static int sizeOfClipboardCallbacks() {
        return WinGlassNative.sizeOfClipboardCallbacks();
    }

    public static int sizeOfDndCallbacks() {
        return WinGlassNative.sizeOfDndCallbacks();
    }

    public static int sizeOfFileFilter() {
        return WinGlassNative.sizeOfFileFilter();
    }

    /** Installs the clipboard table; idempotent, and already done by {@code WinGlassNative}'s own initializer. */
    public static void installClipboardCallbacks() {
        WinGlassNative.installClipboardCallbacks();
    }

    public static void installDndCallbacks() {
        WinGlassNative.installDndCallbacks();
    }

    public static boolean clipboardCallbacksInstalled() {
        return WinGlassNative.clipboardCallbacksInstalled();
    }

    public static boolean dndCallbacksInstalled() {
        return WinGlassNative.dndCallbacksInstalled();
    }

    public static void reinstallClipboardCallbacks() {
        WinGlassNative.reinstallClipboardCallbacks();
    }

    public static void reinstallDndCallbacks() {
        WinGlassNative.reinstallDndCallbacks();
    }

    /** The addresses of the seven installed clipboard stubs in slot order; 0 would be a NULL slot. */
    public static List<Long> installedClipboardCallbackStubAddresses() {
        return WinGlassNative.installedClipboardCallbackStubs().stream().map(MemorySegment::address).toList();
    }

    /** The addresses of the nine installed drag stubs in slot order. */
    public static List<Long> installedDndCallbackStubAddresses() {
        return WinGlassNative.installedDndCallbackStubs().stream().map(MemorySegment::address).toList();
    }

    public static List<String> clipboardSlotNames() {
        return WinGlassNative.CLIPBOARD_SLOT_NAMES;
    }

    public static List<String> dndSlotNames() {
        return WinGlassNative.DND_SLOT_NAMES;
    }

    /* Headless clipboard peers and their registry. */

    /** A {@code WinSystemClipboard} built with no toolkit, and the registry id its constructor gave it. */
    public record HeadlessClipboard(long id, SystemClipboard peer) {
    }

    /**
     * A real {@code WinSystemClipboard}, built headlessly under {@link #asEventThread}: the
     * constructor registers it and calls {@code gwin_clipboard_register_viewer}, which without a
     * toolkit answers {@code GWIN_ERR_NO_TOOLKIT} and does nothing. It is not in
     * {@code Clipboard.clipboards} (it was not made through {@code Clipboard.get}), so nothing but
     * the test holds it; close it with {@link #closeClipboard} or drop its entry with
     * {@link #unregisterClipboard}.
     */
    public static HeadlessClipboard createHeadlessClipboard() {
        return asEventThread(() -> {
            WinSystemClipboard clipboard = new WinSystemClipboard("headless");
            return new HeadlessClipboard(clipboard.nativeId(), clipboard);
        });
    }

    public static long clipboardIdOf(SystemClipboard peer) {
        return ((WinSystemClipboard) peer).nativeId();
    }

    /** The peer's {@code IDataObject*} as an address; 0 for NULL. */
    public static long clipboardHandleOf(SystemClipboard peer) {
        return ((WinSystemClipboard) peer).getPtr().address();
    }

    public static int clipboardLiveObjectsOf(SystemClipboard peer) {
        return ((WinSystemClipboard) peer).liveObjects();
    }

    public static boolean clipboardClosed(SystemClipboard peer) {
        return ((WinSystemClipboard) peer).isClosed();
    }

    /**
     * {@code Clipboard.close()} on the peer, as the event thread: dispose, null the handle, drop the entry if nothing
     * is live.
     */
    public static void closeClipboard(SystemClipboard peer) {
        asEventThread(() -> {
            ((WinSystemClipboard) peer).close();
            return null;
        });
    }

    /**
     * {@code SystemClipboard.flush(dataSource, cacheData, supportedActions)} as the event thread: stores
     * the local data {@code fosSerialize} serves and the {@code dataSource} that
     * {@code actionPerformed} reaches, then {@code pushToSystem} - whose {@code gwin_clipboard_push}
     * answers {@code GWIN_ERR_NO_TOOLKIT} headlessly and publishes nothing.
     */
    public static void flushClipboard(SystemClipboard peer, ClipboardAssistance dataSource,
                                      Map<String, Object> cacheData, int supportedActions) {
        asEventThread(() -> {
            peer.flush(dataSource, new HashMap<>(cacheData), supportedActions);
            return null;
        });
    }

    /** {@code SystemClipboard.isOwner()} on the peer, as the event thread. */
    public static boolean clipboardIsOwner(SystemClipboard peer) {
        return asEventThread(() -> ((WinSystemClipboard) peer).isOwner());
    }

    /** {@code Clipboard.getSupportedSourceActions()} on the peer, as the event thread. */
    public static int clipboardSupportedSourceActions(SystemClipboard peer) {
        return asEventThread(peer::getSupportedSourceActions);
    }

    public static int clipboardRegistrySize() {
        return WinSystemClipboard.clipboardRegistrySize();
    }

    public static boolean clipboardRegistered(long id) {
        return WinSystemClipboard.clipboardFor(id) != null;
    }

    /** The registered peer {@code id}, or null. */
    public static SystemClipboard registeredClipboard(long id) {
        return WinSystemClipboard.clipboardFor(id);
    }

    /** The {@code Clipboard.getName()} of the registered peer {@code id}, or null; as the event thread. */
    public static String registeredClipboardName(long id) {
        WinSystemClipboard clipboard = WinSystemClipboard.clipboardFor(id);
        return clipboard == null ? null : asEventThread(clipboard::getName);
    }

    public static void unregisterClipboard(long id) {
        WinSystemClipboard.unregister(id);
    }

    /**
     * {@code new ClipboardAssistance(name)} as the event thread: creates the named clipboard through the delegate if
     * needed.
     */
    public static ClipboardAssistance openClipboardAssistance(String clipboardName) {
        return asEventThread(() -> new ClipboardAssistance(clipboardName));
    }

    /** A {@code ClipboardAssistance} that records what {@code actionPerformed} / {@code contentChanged} received. */
    public static final class RecordingAssistance extends ClipboardAssistance {

        public final List<String> calls = new ArrayList<>();
        public RuntimeException failure;

        private RecordingAssistance(String clipboardName) {
            super(clipboardName);
        }

        @Override
        public void actionPerformed(int action) {
            record("actionPerformed[" + action + "]");
        }

        @Override
        public void contentChanged() {
            record("contentChanged[]");
        }

        private void record(String call) {
            calls.add(call);
            if (failure != null) {
                throw failure;
            }
        }
    }

    public static RecordingAssistance openRecordingAssistance(String clipboardName) {
        return asEventThread(() -> new RecordingAssistance(clipboardName));
    }

    public static void closeAssistance(ClipboardAssistance assistance) {
        asEventThread(() -> {
            assistance.close();
            return null;
        });
    }

    /* The seven clipboard targets, driven directly. */

    /**
     * What {@code fos_serialize} answered: the status, and the bytes the block held - copied out and
     * freed here with {@code gwin_free}, the test JVM standing in for the C that owns the block from
     * the moment the target returns - or null for a NULL block. {@code length} is what
     * {@code *out_len} said, so a NULL block with a non-zero length would show.
     */
    public record FosResult(int status, byte[] bytes, int length, boolean blockWasNull) {
    }

    public static FosResult fosSerializeTarget(long id, String mime, long lindex) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outData = arena.allocate(ADDRESS);
            MemorySegment outLen = arena.allocate(JAVA_INT);
            outData.set(ADDRESS, 0, MemorySegment.ofAddress(0x5A5A_5A5AL));
            outLen.set(JAVA_INT, 0, 0x5A5A);
            int status = WinGlassNative.onFosSerialize(id, cString(arena, mime), lindex, outData, outLen);
            MemorySegment block = outData.get(ADDRESS, 0);
            int length = outLen.get(JAVA_INT, 0);
            if (status != WinGlassNative.GWIN_OK) {
                // On a throw nothing may have been written: report the sentinels as "untouched".
                boolean untouched = block.address() == 0x5A5A_5A5AL && length == 0x5A5A;
                return new FosResult(status, null, untouched ? -1 : length, false);
            }
            if (block.equals(MemorySegment.NULL)) {
                return new FosResult(status, null, length, true);
            }
            try {
                return new FosResult(status, bounded(block, length).toArray(JAVA_BYTE), length, false);
            } finally {
                WinGlassNative.gwinFree(block);
            }
        }
    }

    public static int actionPerformedTarget(long id, int action) {
        return WinGlassNative.onActionPerformed(id, action);
    }

    public static void dragActionPerformedTarget(long id, int action) {
        WinGlassNative.onDragActionPerformed(id, action);
    }

    public static void contentChangedTarget(long id) {
        WinGlassNative.onContentChanged(id);
    }

    public static void disposePeerTarget(long id) {
        WinGlassNative.onDisposePeer(id);
    }

    public static void setDataObjectTarget(long id, long handle) {
        WinGlassNative.onSetDataObject(id, MemorySegment.ofAddress(handle));
    }

    public static void dataObjectDisposedTarget(long id) {
        WinGlassNative.onDataObjectDisposed(id);
    }

    /* The nine drag targets, driven directly. */

    /** What a drag slot answered: the status and {@code *out_action}, or null when the target left it untouched. */
    public record DragResult(int status, Integer action) {
    }

    /** {@code *out_action} is pre-filled with this, distinct from every {@code Clipboard.ACTION_*} value. */
    private static final int UNTOUCHED_ACTION = 0x7E7E_7E7E;

    public static DragResult dragEnterTarget(long viewId, int x, int y, int xAbs, int yAbs, int recommended) {
        return dragTarget(out -> WinGlassNative.onDragEnter(viewId, x, y, xAbs, yAbs, recommended, out));
    }

    public static DragResult dragOverTarget(long viewId, int x, int y, int xAbs, int yAbs, int recommended) {
        return dragTarget(out -> WinGlassNative.onDragOver(viewId, x, y, xAbs, yAbs, recommended, out));
    }

    public static DragResult dragDropTarget(long viewId, int x, int y, int xAbs, int yAbs, int recommended) {
        return dragTarget(out -> WinGlassNative.onDragDrop(viewId, x, y, xAbs, yAbs, recommended, out));
    }

    private static DragResult dragTarget(java.util.function.ToIntFunction<MemorySegment> target) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(JAVA_INT);
            out.set(JAVA_INT, 0, UNTOUCHED_ACTION);
            int status = target.applyAsInt(out);
            int action = out.get(JAVA_INT, 0);
            return new DragResult(status, action == UNTOUCHED_ACTION ? null : action);
        }
    }

    public static int dragLeaveTarget(long viewId) {
        return WinGlassNative.onDragLeave(viewId);
    }

    /**
     * What {@code dnd_get_data_object} answered: the status and {@code *out_data_object} as an address (-1 when
     * untouched).
     */
    public record HandleResult(int status, long handle) {
    }

    public static HandleResult dndGetDataObjectTarget() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(ADDRESS);
            out.set(ADDRESS, 0, MemorySegment.ofAddress(0x5A5A_5A5AL));
            int status = WinGlassNative.onDndGetDataObject(out);
            long handle = out.get(ADDRESS, 0).address();
            return new HandleResult(status, handle == 0x5A5A_5A5AL ? -1L : handle);
        }
    }

    public static int dndSetDataObjectTarget(long handle) {
        return WinGlassNative.onDndSetDataObject(MemorySegment.ofAddress(handle));
    }

    public static int dndSetSourceSupportedActionsTarget(int actions) {
        return WinGlassNative.onDndSetSourceSupportedActions(actions);
    }

    public static int dndSetDragButtonTarget(int button) {
        return WinGlassNative.onDndSetDragButton(button);
    }

    /** What {@code dnd_get_drag_button} answered: the status and {@code *out_button} (-1 when untouched). */
    public record ButtonResult(int status, int button) {
    }

    public static ButtonResult dndGetDragButtonTarget() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(JAVA_INT);
            out.set(JAVA_INT, 0, UNTOUCHED_ACTION);
            int status = WinGlassNative.onDndGetDragButton(out);
            int button = out.get(JAVA_INT, 0);
            return new ButtonResult(status, button == UNTOUCHED_ACTION ? -1 : button);
        }
    }

    /* The downcalls, headless: every marshalled one answers its defined no-toolkit value. */

    public static long gwinAlloc(long size) {
        return WinGlassNative.gwinAlloc(size).address();
    }

    public static void gwinFree(long block) {
        WinGlassNative.gwinFree(MemorySegment.ofAddress(block));
    }

    /** The UTF-16 code units the block of {@code strings} occupies. */
    public static long stringBlockUnits(String[] strings) {
        return WinGlassNative.stringBlockUnits(strings);
    }

    /**
     * Writes {@code strings} as a string block into a {@code gwin_alloc} block, reads it back through
     * the facade's reader with {@code count}, frees the block, and returns what came back - the
     * round trip of a mime list or a file list, through the library's allocator.
     */
    @SuppressWarnings("restricted")
    public static String[] stringBlockRoundTrip(String[] strings) {
        long bytes = WinGlassNative.stringBlockUnits(strings) * JAVA_CHAR.byteSize();
        MemorySegment block = WinGlassNative.gwinAlloc(bytes);
        if (block.equals(MemorySegment.NULL)) {
            throw new AssertionError("gwin_alloc(" + bytes + ") returned NULL");
        }
        try {
            WinGlassNative.writeStringBlock(block.reinterpret(bytes), strings);
            return WinGlassNative.readStringBlock(block, strings.length);
        } finally {
            WinGlassNative.gwinFree(block);
        }
    }

    /** The raw code units of the block of {@code strings}, for the layout assertion. */
    public static char[] stringBlockUnitsOf(String[] strings) {
        try (Arena arena = Arena.ofConfined()) {
            return WinGlassNative.allocStringBlock(arena, strings).toArray(JAVA_CHAR);
        }
    }

    public static int clipboardRegisterViewer(long id) {
        return WinGlassNative.clipboardRegisterViewer(id);
    }

    public static void clipboardDispose(long handle) {
        WinGlassNative.clipboardDispose(MemorySegment.ofAddress(handle));
    }

    public static int clipboardPush(long handle, long id, Object[] keys, int supportedActions) {
        return WinGlassNative.clipboardPush(MemorySegment.ofAddress(handle), id, keys, supportedActions);
    }

    public static long clipboardPop(long handle) {
        return WinGlassNative.clipboardPop(MemorySegment.ofAddress(handle)).address();
    }

    public static byte[] clipboardPopBytes(long handle, String mime, long lindex) {
        return WinGlassNative.clipboardPopBytes(MemorySegment.ofAddress(handle), mime, lindex);
    }

    public static String[] clipboardPopMimes(long handle) {
        return WinGlassNative.clipboardPopMimes(MemorySegment.ofAddress(handle));
    }

    public static void clipboardPushTargetAction(long handle, int actionDone) {
        WinGlassNative.clipboardPushTargetAction(MemorySegment.ofAddress(handle), actionDone);
    }

    public static int clipboardPopSupportedActions(long handle) {
        return WinGlassNative.clipboardPopSupportedActions(MemorySegment.ofAddress(handle));
    }

    public static int dndPush(long handle, long id, Object[] keys, int supportedActions) {
        return WinGlassNative.dndPush(MemorySegment.ofAddress(handle), id, keys, supportedActions);
    }

    public static void dndDispose(long handle) {
        WinGlassNative.dndDispose(MemorySegment.ofAddress(handle));
    }

    public static boolean oleIsCurrentClipboard(long handle) {
        return WinGlassNative.oleIsCurrentClipboard(MemorySegment.ofAddress(handle));
    }

    /* The dialogs: the marshalling only - a gwin_dialog_* call would show a real dialog. */

    /**
     * What {@code allocFileFilters} built for the two arrays, read back through the layout: whether
     * the pointer was NULL, and each entry's two strings.
     */
    public record FilterBlock(boolean isNull, List<String> descriptions, List<String> extensions) {
    }

    public static FilterBlock fileFilterBlock(String[] descriptions, String[] extensions) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment filters = WinGlassNative.allocFileFilters(arena, descriptions, extensions);
            if (filters.equals(MemorySegment.NULL)) {
                return new FilterBlock(true, List.of(), List.of());
            }
            List<String> readDescriptions = new ArrayList<>();
            List<String> readExtensions = new ArrayList<>();
            long size = WinGlassNative.GWIN_FILE_FILTER_LAYOUT.byteSize();
            long descriptionOffset = offset("GwinFileFilter", "description");
            long extensionsOffset = offset("GwinFileFilter", "extensions");
            for (int i = 0; i < descriptions.length; i++) {
                readDescriptions.add(WinGlassNative.readStringBlock(
                        filters.get(ADDRESS, i * size + descriptionOffset), 1)[0]);
                readExtensions.add(WinGlassNative.readStringBlock(
                        filters.get(ADDRESS, i * size + extensionsOffset), 1)[0]);
            }
            return new FilterBlock(false, readDescriptions, readExtensions);
        }
    }

    /** {@code WinGlassNative.dialogString}: the never-NULL rule for folder, filename, title and filter strings. */
    public static String dialogString(String text) {
        return WinGlassNative.dialogString(text);
    }

    /**
     * The {@code WCHAR}s {@code WinGlassNative.wideString} writes for {@code text}, the terminating NUL included,
     * read back from native memory. {@code wideString} is private and used by every string the facade hands
     * Win32 or {@code glass.dll}; {@code allocFileFilters} is a package-private caller that hands its segment out,
     * as a filter's {@code description} pointer, so {@code text} goes in as a one-entry filter array.
     * {@code dialogString} passes a non-null string through unchanged. Exactly {@code text.length() + 1} units are
     * read, not up to the first NUL, so an embedded U+0000 and everything after it are read too.
     */
    @SuppressWarnings("restricted")
    public static char[] wideStringCodeUnits(String text) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment filters = WinGlassNative.allocFileFilters(arena, new String[] {text}, new String[] {""});
            MemorySegment description = filters.get(ADDRESS, offset("GwinFileFilter", "description"));
            return description.reinterpret((text.length() + 1L) * JAVA_CHAR_UNALIGNED.byteSize())
                    .toArray(JAVA_CHAR_UNALIGNED);
        }
    }

    /** {@code ExtensionFilter}s built as the event thread, one per description with the given extensions. */
    public static ExtensionFilter[] extensionFilters(String[] descriptions, List<List<String>> extensions) {
        return asEventThread(() -> {
            ExtensionFilter[] filters = new ExtensionFilter[descriptions.length];
            for (int i = 0; i < descriptions.length; i++) {
                filters[i] = descriptions[i] == null ? null : new ExtensionFilter(descriptions[i], extensions.get(i));
            }
            return filters;
        });
    }

    /** {@code WinCommonDialogs.filterDescriptions}, as the event thread ({@code getDescription()} checks it). */
    public static String[] filterDescriptions(ExtensionFilter[] filters) {
        return asEventThread(() -> WinCommonDialogs.filterDescriptions(filters));
    }

    /** {@code WinCommonDialogs.filterExtensions}, as the event thread. */
    public static String[] filterExtensions(ExtensionFilter[] filters) {
        return asEventThread(() -> WinCommonDialogs.filterExtensions(filters));
    }

    /** {@code WinCommonDialogs.fileChooserResult} over what {@code gwin_dialog_file} would have answered. */
    public static FileChooserResult fileChooserResult(String[] files, int filterIndex, ExtensionFilter[] filters) {
        return WinCommonDialogs.fileChooserResult(new WinGlassNative.FileDialogResult(files, filterIndex), filters);
    }

    /* The one gwin_view_* export that neither marshals nor needs a host window. */

    /** The {@code GlassView*} of a headless view ({@code View.ptr}), for {@link #viewGetNativeView}. */
    public static long viewNativeHandle(View view) {
        return ((WinView) view).nativeHandle();
    }

    /** {@code gwin_view_get_native_view}: the host HWND, 0 for a view that was never attached. */
    public static long viewGetNativeView(long glassView) {
        return WinGlassNative.viewGetNativeView(glassView);
    }

    /**
     * {@code gwin_view_create}, straight through the facade and past the registry: a {@code GlassView}
     * carrying {@code viewId}, 0 only if the allocation threw. The caller cannot release it headlessly
     * ({@code gwin_view_close} needs a toolkit, see {@link #createHeadlessView}).
     */
    public static long viewCreate(long viewId) {
        return WinGlassNative.viewCreate(viewId);
    }

    private static MemorySegment cString(Arena arena, String text) {
        char[] chars = text.toCharArray();
        MemorySegment segment = arena.allocate(JAVA_CHAR, chars.length + 1L);
        MemorySegment.copy(chars, 0, segment, JAVA_CHAR, 0, chars.length);
        segment.setAtIndex(JAVA_CHAR, chars.length, '\0');
        return segment;
    }

    @SuppressWarnings("restricted")
    private static MemorySegment bounded(MemorySegment pointer, long byteSize) {
        return pointer.reinterpret(byteSize);
    }

    /* The clipboard and drag test hooks, through the production tables and through recording tables. */

    /** What a recording status slot answers, distinct from every GwinStatus. */
    public static final int RECORDING_STATUS = 7;

    /** What the recording {@code fos_serialize} hands the C: three bytes in a {@code gwin_alloc} block. */
    public static final byte[] RECORDING_FOS_BYTES = {7, 8, 9};

    /** What the recording drag slots write to {@code *out_action}. */
    public static final int RECORDING_ACTION = 0x7A7A;

    /** What the recording {@code dnd_get_data_object} writes: bits above 32 set. */
    public static final long RECORDING_DATA_OBJECT = 0x1_0000_5678L;

    /** What the recording {@code dnd_get_drag_button} writes. */
    public static final int RECORDING_BUTTON = 0x7B7B;

    /** The sentinel the drag hook's {@code out} block is pre-filled with, as an int and as a long. */
    public static final long UNTOUCHED_OUT = 0x5A5A_5A5A_5A5A_5A5AL;

    /** {@code gwin_test_fire_clipboard_callback} through the production table, as the event thread. */
    public static long fireClipboardCallback(int slot, long clipboardId) {
        return asEventThread(() -> WinGlassNative.testFireClipboardCallback(slot, clipboardId));
    }

    /** What the drag hook returned and what its {@code out} block held afterwards, read as an int and as a long. */
    public record DndFire(long returned, int outInt, long outLong) {
    }

    /**
     * {@code gwin_test_fire_dnd_callback} through the production table, as the event thread, with an
     * 8-byte {@code out} pre-filled with {@link #UNTOUCHED_OUT} (or NULL when {@code withOut} is false).
     */
    public static DndFire fireDndCallback(int slot, long viewId, boolean withOut) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = withOut ? arena.allocate(8, 8) : MemorySegment.NULL;
            if (withOut) {
                out.set(java.lang.foreign.ValueLayout.JAVA_LONG, 0, UNTOUCHED_OUT);
            }
            long returned = asEventThread(() -> WinGlassNative.testFireDndCallback(slot, viewId, out));
            return withOut
                    ? new DndFire(returned, out.get(JAVA_INT, 0), out.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0))
                    : new DndFire(returned, 0, 0L);
        }
    }


    /**
     * What {@code gwin_test_string_block} handed over: the status, the count the C wrote, the strings
     * the facade's reader parsed out of the C-built block with that count, and the block's raw code
     * units - every string, its NUL and the terminator - copied before the block was released here
     * with {@code gwin_free}, exactly once. {@code strings} and {@code units} are null when no block
     * was handed over.
     */
    public record CBuiltStringBlock(int status, int count, String[] strings, char[] units) {
    }

    /**
     * {@code gwin_test_string_block} into two out-parameters: the block the library built with
     * {@code GwinMakeStringBlock} - the builder behind {@code gwin_clipboard_pop_mimes} and
     * {@code gwin_dialog_file} - parsed by the facade's {@code readStringBlock} with the count the C
     * wrote, its layout copied, and the block freed in a {@code finally}. The C-built twin of
     * {@link #stringBlockRoundTrip}: the one string block Java parses here that Java did not write.
     */
    public static CBuiltStringBlock cBuiltStringBlock() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outBlock = arena.allocate(ADDRESS);
            MemorySegment outCount = arena.allocate(JAVA_INT);
            outBlock.set(ADDRESS, 0, MemorySegment.ofAddress(UNTOUCHED_OUT));
            outCount.set(JAVA_INT, 0, UNTOUCHED);
            int status = SliceNineRecording.testStringBlock(outBlock, outCount);
            MemorySegment block = outBlock.get(ADDRESS, 0);
            int count = outCount.get(JAVA_INT, 0);
            if (status != WinGlassNative.GWIN_OK || block.equals(MemorySegment.NULL)) {
                return new CBuiltStringBlock(status, count, null, null);
            }
            try {
                String[] strings = WinGlassNative.readStringBlock(block, count);
                long bytes = WinGlassNative.stringBlockUnits(strings) * JAVA_CHAR.byteSize();
                return new CBuiltStringBlock(status, count, strings, bounded(block, bytes).toArray(JAVA_CHAR));
            } finally {
                WinGlassNative.gwinFree(block);
            }
        }
    }

    /** What the hook answered with an out-pointer NULL, and whether the other out-parameter kept its sentinel. */
    public record StringBlockRejection(int status, boolean untouched) {
    }

    /** {@code gwin_test_string_block(NULL, &count)}: nothing written, nothing handed over, nothing to free. */
    public static StringBlockRejection stringBlockWithNullBlockPointer() {
        return stringBlockRejection(count -> SliceNineRecording.testStringBlock(MemorySegment.NULL, count));
    }

    /** {@code gwin_test_string_block(&block, NULL)}: the same rejection with the other pointer NULL. */
    public static StringBlockRejection stringBlockWithNullCountPointer() {
        return stringBlockRejection(block -> SliceNineRecording.testStringBlock(block, MemorySegment.NULL));
    }

    /** Fires the hook with an 8-byte survivor pre-filled with {@link #UNTOUCHED_OUT} as its one non-NULL pointer. */
    private static StringBlockRejection stringBlockRejection(java.util.function.ToIntFunction<MemorySegment> hook) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment survivor = arena.allocate(8, 8);
            survivor.set(java.lang.foreign.ValueLayout.JAVA_LONG, 0, UNTOUCHED_OUT);
            int status = hook.applyAsInt(survivor);
            return new StringBlockRejection(status,
                    survivor.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0) == UNTOUCHED_OUT);
        }
    }

    /**
     * Installs a recording clipboard table built from the facade's own descriptor constants, fires
     * {@code slot} through {@code gwin_test_fire_clipboard_callback}, and restores the production
     * table in a {@code finally}. The recorded tuple is what the C put on the stack as read through
     * the facade's descriptor; the header's pattern is what it should be.
     */
    public static synchronized RecordedCall fireClipboardIntoRecordingTable(int slot, long clipboardId) {
        SliceNineRecording.ARGUMENTS.clear();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(WinGlassNative.GWIN_CLIPBOARD_CALLBACKS_LAYOUT);
            WinGlassNative.fillCallbackTable(table, WinGlassNative.GWIN_CLIPBOARD_CALLBACKS_LAYOUT,
                    WinGlassNative.CLIPBOARD_SLOT_NAMES, SliceNineRecording.CLIPBOARD_STUBS);
            int status = (int) SliceNineRecording.SET_CLIPBOARD_CALLBACKS.invokeExact(table);
            if (status != WinGlassNative.GWIN_OK) {
                throw new AssertionError("gwin_clipboard_set_callbacks answered " + status);
            }
            long returned = WinGlassNative.testFireClipboardCallback(slot, clipboardId);
            return new RecordedCall(returned, List.copyOf(SliceNineRecording.ARGUMENTS));
        } catch (Throwable t) {
            throw new AssertionError("the recording clipboard table could not be installed or fired", t);
        } finally {
            WinGlassNative.reinstallClipboardCallbacks();
        }
    }

    /** The drag-table twin of {@link #fireClipboardIntoRecordingTable}, with the {@code out} block read back. */
    public record RecordedDndCall(long returned, List<Object> arguments, int outInt, long outLong) {
    }

    public static synchronized RecordedDndCall fireDndIntoRecordingTable(int slot, long viewId, boolean withOut) {
        SliceNineRecording.ARGUMENTS.clear();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment table = arena.allocate(WinGlassNative.GWIN_DND_CALLBACKS_LAYOUT);
            WinGlassNative.fillCallbackTable(table, WinGlassNative.GWIN_DND_CALLBACKS_LAYOUT,
                    WinGlassNative.DND_SLOT_NAMES, SliceNineRecording.DND_STUBS);
            int status = (int) SliceNineRecording.SET_DND_CALLBACKS.invokeExact(table);
            if (status != WinGlassNative.GWIN_OK) {
                throw new AssertionError("gwin_dnd_set_callbacks answered " + status);
            }
            MemorySegment out = withOut ? arena.allocate(8, 8) : MemorySegment.NULL;
            if (withOut) {
                out.set(java.lang.foreign.ValueLayout.JAVA_LONG, 0, UNTOUCHED_OUT);
            }
            long returned = WinGlassNative.testFireDndCallback(slot, viewId, out);
            List<Object> arguments = List.copyOf(SliceNineRecording.ARGUMENTS);
            return withOut
                    ? new RecordedDndCall(returned, arguments, out.get(JAVA_INT, 0),
                            out.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0))
                    : new RecordedDndCall(returned, arguments, 0, 0L);
        } catch (Throwable t) {
            throw new AssertionError("the recording drag table could not be installed or fired", t);
        } finally {
            WinGlassNative.reinstallDndCallbacks();
        }
    }

    /**
     * The recording targets of the clipboard and drag tables, each with exactly the method type the facade's
     * descriptor yields; stubs and installer binds built once in {@link Arena#global()}, as
     * {@link Recording} does for the view tables. Also binds {@code gwin_test_string_block}, the
     * section's third hook, here rather than in the facade so that {@code boundSymbols()} stays the
     * product's own list, as {@link WindowRecording} does for the window hook.
     */
    private static final class SliceNineRecording {

        private static final Linker LINKER = Linker.nativeLinker();
        private static final List<Object> ARGUMENTS = new ArrayList<>();

        private static final MethodHandle SET_CLIPBOARD_CALLBACKS = bindGlass("gwin_clipboard_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        private static final MethodHandle SET_DND_CALLBACKS = bindGlass("gwin_dnd_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** {@code int32_t gwin_test_string_block(uint16_t** out_block, int32_t* out_count)}. */
        private static final MethodHandle STRING_BLOCK = bindGlass("gwin_test_string_block",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        private static final MemorySegment[] CLIPBOARD_STUBS = stubs(
                new String[] {"recordFosSerialize", "recordActionPerformed", "recordDragActionPerformed",
                    "recordContentChanged", "recordDisposePeer", "recordSetDataObject", "recordDataObjectDisposed"},
                new FunctionDescriptor[] {WinGlassNative.CLIPBOARD_FOS_SERIALIZE_FD,
                    WinGlassNative.CLIPBOARD_ACTION_PERFORMED_FD, WinGlassNative.CLIPBOARD_DRAG_ACTION_PERFORMED_FD,
                    WinGlassNative.CLIPBOARD_CONTENT_CHANGED_FD, WinGlassNative.CLIPBOARD_DISPOSE_PEER_FD,
                    WinGlassNative.CLIPBOARD_SET_DATA_OBJECT_FD, WinGlassNative.CLIPBOARD_DATA_OBJECT_DISPOSED_FD});

        private static final MemorySegment[] DND_STUBS = stubs(
                new String[] {"recordDragEnter", "recordDragOver", "recordDragDrop", "recordDragLeave",
                    "recordDndGetDataObject", "recordDndSetDataObject", "recordDndSetSourceSupportedActions",
                    "recordDndSetDragButton", "recordDndGetDragButton"},
                new FunctionDescriptor[] {WinGlassNative.DND_DRAG_ENTER_FD, WinGlassNative.DND_DRAG_OVER_FD,
                    WinGlassNative.DND_DRAG_DROP_FD, WinGlassNative.DND_DRAG_LEAVE_FD,
                    WinGlassNative.DND_GET_DATA_OBJECT_FD, WinGlassNative.DND_SET_DATA_OBJECT_FD,
                    WinGlassNative.DND_SET_SOURCE_SUPPORTED_ACTIONS_FD, WinGlassNative.DND_SET_DRAG_BUTTON_FD,
                    WinGlassNative.DND_GET_DRAG_BUTTON_FD});

        private SliceNineRecording() {
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bindGlass(String name, FunctionDescriptor fd) {
            MemorySegment symbol = SymbolLookup.loaderLookup().find(name).orElseThrow(
                    () -> new UnsatisfiedLinkError("missing native symbol: " + name + " in glass"));
            return LINKER.downcallHandle(symbol, fd);
        }

        @SuppressWarnings("restricted")
        private static MemorySegment[] stubs(String[] targets, FunctionDescriptor[] descriptors) {
            MemorySegment[] stubs = new MemorySegment[targets.length];
            for (int i = 0; i < targets.length; i++) {
                try {
                    MethodHandle target = MethodHandles.lookup().findStatic(SliceNineRecording.class, targets[i],
                            descriptors[i].toMethodType());
                    stubs[i] = LINKER.upcallStub(target, descriptors[i], Arena.global());
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError("no recording target " + targets[i], e);
                }
            }
            return stubs;
        }

        private static void record(Object... values) {
            ARGUMENTS.addAll(Arrays.asList(values));
        }

        /** The C-built block hook: the block is the caller's from the moment this returns. */
        static int testStringBlock(MemorySegment outBlock, MemorySegment outCount) {
            try {
                return (int) STRING_BLOCK.invokeExact(outBlock, outCount);
            } catch (Throwable t) {
                throw new AssertionError("gwin_test_string_block failed", t);
            }
        }

        /** Records the tuple and hands the hook a real {@code gwin_alloc} block, which the hook frees. */
        static int recordFosSerialize(long clipboardId, MemorySegment mime, long lindex, MemorySegment outData,
                                      MemorySegment outLen) {
            record(clipboardId, WinGlassNative.readStringBlock(mime, 1)[0], lindex);
            MemorySegment block = WinGlassNative.gwinAlloc(RECORDING_FOS_BYTES.length);
            MemorySegment.copy(RECORDING_FOS_BYTES, 0, bounded(block, RECORDING_FOS_BYTES.length), JAVA_BYTE, 0,
                    RECORDING_FOS_BYTES.length);
            bounded(outData, ADDRESS.byteSize()).set(ADDRESS, 0, block);
            bounded(outLen, JAVA_INT.byteSize()).set(JAVA_INT, 0, RECORDING_FOS_BYTES.length);
            return WinGlassNative.GWIN_OK;
        }

        static int recordActionPerformed(long clipboardId, int action) {
            record(clipboardId, action);
            return RECORDING_STATUS;
        }

        static void recordDragActionPerformed(long clipboardId, int action) {
            record(clipboardId, action);
        }

        static void recordContentChanged(long clipboardId) {
            record(clipboardId);
        }

        static void recordDisposePeer(long clipboardId) {
            record(clipboardId);
        }

        static void recordSetDataObject(long clipboardId, MemorySegment dataObject) {
            record(clipboardId, dataObject.address());
        }

        static void recordDataObjectDisposed(long clipboardId) {
            record(clipboardId);
        }

        static int recordDragEnter(long viewId, int x, int y, int xAbs, int yAbs, int recommended,
                                   MemorySegment out) {
            record(viewId, x, y, xAbs, yAbs, recommended);
            bounded(out, JAVA_INT.byteSize()).set(JAVA_INT, 0, RECORDING_ACTION);
            return RECORDING_STATUS;
        }

        static int recordDragOver(long viewId, int x, int y, int xAbs, int yAbs, int recommended,
                                  MemorySegment out) {
            record(viewId, x, y, xAbs, yAbs, recommended);
            bounded(out, JAVA_INT.byteSize()).set(JAVA_INT, 0, RECORDING_ACTION);
            return RECORDING_STATUS;
        }

        static int recordDragDrop(long viewId, int x, int y, int xAbs, int yAbs, int recommended,
                                  MemorySegment out) {
            record(viewId, x, y, xAbs, yAbs, recommended);
            bounded(out, JAVA_INT.byteSize()).set(JAVA_INT, 0, RECORDING_ACTION);
            return RECORDING_STATUS;
        }

        static int recordDragLeave(long viewId) {
            record(viewId);
            return RECORDING_STATUS;
        }

        static int recordDndGetDataObject(MemorySegment out) {
            record("get_data_object");
            bounded(out, ADDRESS.byteSize()).set(ADDRESS, 0, MemorySegment.ofAddress(RECORDING_DATA_OBJECT));
            return RECORDING_STATUS;
        }

        static int recordDndSetDataObject(MemorySegment dataObject) {
            record(dataObject.address());
            return RECORDING_STATUS;
        }

        static int recordDndSetSourceSupportedActions(int actions) {
            record(actions);
            return RECORDING_STATUS;
        }

        static int recordDndSetDragButton(int button) {
            record(button);
            return RECORDING_STATUS;
        }

        static int recordDndGetDragButton(MemorySegment out) {
            record("get_drag_button");
            bounded(out, JAVA_INT.byteSize()).set(JAVA_INT, 0, RECORDING_BUTTON);
            return RECORDING_STATUS;
        }
    }

    /* ==== Accessibility: the two UI Automation provider tables ======================================= */


    /** Forces {@code WinTextRangeProvider}'s initializer, which installs both accessibility tables. */
    public static Throwable initializeWinTextRangeProvider() {
        try {
            Class.forName("com.sun.glass.ui.win.WinTextRangeProvider", true,
                    WinGlassNativeShim.class.getClassLoader());
            return null;
        } catch (ClassNotFoundException | LinkageError e) {
            return e;
        }
    }

    /** Whether {@code WinGlassNative.installAccessibilityCallbacks} has run in this JVM. */
    public static boolean accessibilityCallbacksInstalled() {
        return WinGlassNative.accessibilityCallbacksInstalled();
    }

    /** The 89 installed stubs, accessible slots then text-range slots. */
    public static List<MemorySegment> installedAccessibilityCallbackStubs() {
        return WinGlassNative.installedAccessibilityCallbackStubs();
    }

    /**
     * Binds the two {@code UIAutomationCore} functions, so that a test class which asserts
     * {@link #boundSymbols()} sees them in a position that does not depend on which test class ran
     * first: every class that forces this one forces the four older lazy holders before it.
     */
    public static void bindAccessibilitySymbols() {
        WinGlassNative.uiaClientsAreListening();
    }

    /** Whether the {@code UIAutomationCore} holder has been forced in this JVM. */
    public static boolean uiaSymbolsBound() {
        return boundSymbols().contains("UIAutomationCore!UiaClientsAreListening");
    }

    /** {@code gwin_sizeof_accessible_callbacks()}. */
    public static int sizeOfAccessibleCallbacks() {
        return WinGlassNative.sizeOfAccessibleCallbacks();
    }

    /** {@code gwin_sizeof_text_range_callbacks()}. */
    public static int sizeOfTextRangeCallbacks() {
        return WinGlassNative.sizeOfTextRangeCallbacks();
    }

    /** {@code gwin_sizeof_variant()}. */
    public static int sizeOfVariant() {
        return WinGlassNative.sizeOfVariant();
    }

    /** {@code GWIN_VARIANT_LAYOUT.byteSize()} - what the Java side believes {@code GwinVariant} is. */
    public static long variantLayoutSize() {
        return WinGlassNative.GWIN_VARIANT_LAYOUT.byteSize();
    }

    /** The eleven {@code GwinVariant} field names, in declaration order. */
    public static final List<String> VARIANT_FIELDS = List.of("vt", "i_val", "l_val", "flt_val", "dbl_val",
            "bool_val", "punk_val", "bstr_val", "bstr_len", "p_dbl_val", "p_dbl_count");

    /** The offset of every {@link #VARIANT_FIELDS} entry in {@code GWIN_VARIANT_LAYOUT}. */
    public static int[] variantLayoutOffsets() {
        int[] offsets = new int[VARIANT_FIELDS.size()];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = (int) WinGlassNative.GWIN_VARIANT_LAYOUT.byteOffset(
                    PathElement.groupElement(VARIANT_FIELDS.get(i)));
        }
        return offsets;
    }

    /** {@code gwin_test_variant_offsets}: the eleven offsets the C compiler gave the struct. */
    public static int[] variantOffsetsFromC() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(JAVA_INT, VARIANT_FIELDS.size());
            int status = (int) Accessibility.VARIANT_OFFSETS.invokeExact(out);
            if (status != WinGlassNative.GWIN_OK) {
                throw new AssertionError("gwin_test_variant_offsets answered " + status);
            }
            return out.toArray(JAVA_INT);
        } catch (Throwable t) {
            throw new AssertionError("gwin_test_variant_offsets could not be called", t);
        }
    }

    /** The slot names of {@code GwinAccessibleCallbacks}, in declaration order. */
    public static List<String> accessibleSlotNames() {
        return WinGlassNative.ACCESSIBLE_SLOT_NAMES;
    }

    /** The slot names of {@code GwinTextRangeCallbacks}, in declaration order. */
    public static List<String> textRangeSlotNames() {
        return WinGlassNative.TEXT_RANGE_SLOT_NAMES;
    }

    /** How many accessibles the {@code WinAccessible} registry holds. */
    public static int accessibleRegistrySize() {
        return WinAccessible.accessibleRegistrySize();
    }

    /** How many ranges the {@code WinTextRangeProvider} registry holds. */
    public static int rangeRegistrySize() {
        return WinTextRangeProvider.rangeRegistrySize();
    }

    /*
     * Real registered peers, built with no toolkit. Both constructors publish their registry entry and
     * then call gwin_a11y_create / gwin_a11y_text_range_create, which only allocate a COM object with
     * that id - no window, no message pump - so the id registries that replaced NewGlobalRef and
     * DeleteGlobalRef can be driven end to end here. The types are package-private, so they cross as
     * Object, exactly as variant() does. WinAccessible's constructor, dispose() and
     * getNativeAccessible() all call Application.checkEventThread(), so everything that can reach one
     * runs through asEventThread - the rule fireCallback already follows for the view table.
     */

    /** A real {@code WinAccessible}. The caller must {@link #disposeAccessible} it. */
    public static Object createRegisteredAccessible() {
        return asEventThread(WinAccessible::new);
    }

    /** The id {@link #createRegisteredAccessible}'s peer is registered under; 0 if it never was. */
    public static long accessibleIdOf(Object accessible) {
        return ((WinAccessible) accessible).accessibleId();
    }

    /**
     * {@code WinAccessible.dispose()}: a COM {@code Release}, not a delete - the registry entry goes
     * when the library fires {@code accessible_disposed}, which a live range can defer.
     */
    public static void disposeAccessible(Object accessible) {
        asEventThread(() -> {
            ((WinAccessible) accessible).dispose();
            return null;
        });
    }

    /** A real {@code WinTextRangeProvider} on {@code accessible}, which it pins. */
    public static Object createRegisteredRange(Object accessible) {
        return asEventThread(() -> new WinTextRangeProvider((WinAccessible) accessible));
    }

    /** The id {@link #createRegisteredRange}'s peer is registered under; 0 if it never was. */
    public static long rangeIdOf(Object range) {
        return ((WinTextRangeProvider) range).rangeId();
    }

    /** {@code WinTextRangeProvider.dispose()}: the {@code Release} that lets {@code range_disposed} fire. */
    public static void disposeRange(Object range) {
        ((WinTextRangeProvider) range).dispose();
    }

    /** {@code gwin_a11y_create}: a {@code GlassAccessible} with no toolkit and no Java peer behind the id. */
    public static long createAccessible(long accessibleId) {
        return WinGlassNative.createAccessible(accessibleId);
    }

    /** {@code gwin_a11y_destroy}. */
    public static void destroyAccessible(long accessible) {
        WinGlassNative.destroyAccessible(accessible);
    }

    /** {@code gwin_a11y_text_range_create}. */
    public static long createTextRange(long accessible, long rangeId) {
        return WinGlassNative.createTextRange(accessible, rangeId);
    }

    /** {@code gwin_a11y_text_range_destroy}. */
    public static void destroyTextRange(long range) {
        WinGlassNative.destroyTextRange(range);
    }

    /** {@code UIAutomationCore!UiaClientsAreListening}. */
    public static boolean uiaClientsAreListening() {
        return WinGlassNative.uiaClientsAreListening();
    }

    /** {@code UIAutomationCore!UiaRaiseAutomationEvent}, the {@code HRESULT} sign-extended. */
    public static long raiseAutomationEvent(long provider, int eventId) {
        return WinGlassNative.raiseAutomationEvent(provider, eventId);
    }

    /** {@code gwin_a11y_raise_property_changed}, the {@code HRESULT} sign-extended. */
    public static long raiseAutomationPropertyChangedEvent(long provider, int propertyId, Object oldValue,
                                                           Object newValue) {
        return WinGlassNative.raiseAutomationPropertyChangedEvent(provider, propertyId,
                (WinVariant) oldValue, (WinVariant) newValue);
    }

    /** A {@code WinVariant} built out of the test's own values, so that a test need not see the class. */
    public static Object variant(short vt, int lVal, String bstrVal, boolean boolVal, double dblVal,
                                 double[] pDblVal, long punkVal) {
        WinVariant variant = new WinVariant();
        variant.vt = vt;
        variant.lVal = lVal;
        variant.bstrVal = bstrVal;
        variant.boolVal = boolVal;
        variant.dblVal = dblVal;
        variant.pDblVal = pDblVal;
        variant.punkVal = punkVal;
        return variant;
    }

    /** What firing an accessibility slot recorded: the status it answered and every argument it saw. */
    public record RecordedSlot(long returned, List<Object> arguments) {
    }

    /**
     * Installs a table of <em>recording</em> stubs - one per {@code FunctionDescriptor} of the facade, so
     * what is proved is those descriptors and not a copy - fires {@code slot} of the accessible table
     * through {@code gwin_test_fire_accessible_callback}, and restores the production tables in a
     * {@code finally}. With 89 slots this is the only automated check that a descriptor agrees with its
     * prototype: a {@code sizeof} probe sees only pointers, and one parameter too many silently shifts
     * every following argument by a stack slot on x64.
     */
    public static synchronized RecordedSlot fireAccessibleIntoRecordingTable(int slot, long accessibleId,
                                                                             MemorySegment out) {
        return Accessibility.fireIntoRecordingTable(true, slot, accessibleId, out);
    }

    /** {@link #fireAccessibleIntoRecordingTable} for {@code gwin_test_fire_text_range_callback}. */
    public static synchronized RecordedSlot fireTextRangeIntoRecordingTable(int slot, long rangeId,
                                                                            MemorySegment out) {
        return Accessibility.fireIntoRecordingTable(false, slot, rangeId, out);
    }

    /**
     * Fires {@code slot} against the <em>production</em> table, which reaches the registry and runs real
     * {@code WinAccessible} code - so a test must fire at an id no registry holds, or accept what the
     * peer does.
     */
    public static long fireAccessibleCallback(int slot, long accessibleId, MemorySegment out) {
        try {
            return (long) Accessibility.FIRE_ACCESSIBLE.invokeExact(slot, accessibleId, out);
        } catch (Throwable t) {
            throw new AssertionError("gwin_test_fire_accessible_callback could not be called", t);
        }
    }

    /**
     * {@link #fireAccessibleCallback} with the calling thread made the event thread for the duration,
     * as {@link #fireCallback} is for the view table: a slot that resolves to a registered peer runs
     * real {@code WinAccessible} code, and {@code getNativeAccessible()} - which {@code isDisposed()}
     * calls, so almost every provider method reaches it - calls {@code Application.checkEventThread()}.
     */
    public static long fireAccessibleCallbackAsEventThread(int slot, long accessibleId, MemorySegment out) {
        return asEventThread(() -> fireAccessibleCallback(slot, accessibleId, out));
    }

    /** {@link #fireAccessibleCallback} for the text-range table. */
    public static long fireTextRangeCallback(int slot, long rangeId, MemorySegment out) {
        try {
            return (long) Accessibility.FIRE_TEXT_RANGE.invokeExact(slot, rangeId, out);
        } catch (Throwable t) {
            throw new AssertionError("gwin_test_fire_text_range_callback could not be called", t);
        }
    }

    /**
     * Installs a table whose every slot throws, fires {@code slot}, and restores the production tables.
     * The status the library reports back is what a provider method turns into {@code E_FAIL}, and the
     * {@code Throwable} must have reached {@code Application.reportException} - the calling thread's
     * uncaught-exception handler - exactly as {@code CheckAndClearException} delivered it.
     */
    public static synchronized long fireAccessibleIntoThrowingTable(int slot, long accessibleId,
                                                                    MemorySegment out) {
        return Accessibility.fireIntoThrowingTable(slot, accessibleId, out);
    }

    /** The message the throwing table's targets give their {@code RuntimeException}. */
    public static final String THROWING_SLOT_MESSAGE = "accessibility slot target threw";

    /**
     * Destroys {@code accessible} with the recording tables installed, so that the
     * {@code accessible_disposed} the destructor fires is observed. Restores the production tables.
     */
    public static synchronized List<Object> destroyAccessibleRecordingDisposal(long accessible) {
        return Accessibility.recordDestroy(() -> WinGlassNative.destroyAccessible(accessible));
    }

    /** {@link #destroyAccessibleRecordingDisposal} for {@code gwin_a11y_text_range_destroy}. */
    public static synchronized List<Object> destroyTextRangeRecordingDisposal(long range) {
        return Accessibility.recordDestroy(() -> WinGlassNative.destroyTextRange(range));
    }

    /*
     * Driving the provider in process, through its own COM vtable: the returned gwin_a11y_create handle
     * IS the object's IRawElementProviderSimple*, so the first machine word is the vtable and slots 3, 4
     * and 5 are get_ProviderOptions, GetPatternProvider and GetPropertyValue after the three IUnknown
     * entries. That is the real C body, the real upcall table and the real HRESULT - the only way to see
     * a provider method run without a UI Automation client. The DWNative precedent for calling a COM
     * vtable through FFM is in com.sun.javafx.font.directwrite.
     */

    /** {@code get_ProviderOptions} (vtable slot 3): {@code {HRESULT, options}}. */
    public static int[] providerGetProviderOptions(long provider) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(JAVA_INT);
            out.set(JAVA_INT, 0, -1);
            int hr = (int) Accessibility.vtableSlot(provider, 3,
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS))
                    .invokeExact(MemorySegment.ofAddress(provider), out);
            return new int[] {hr, out.get(JAVA_INT, 0)};
        } catch (Throwable t) {
            throw new AssertionError("get_ProviderOptions could not be called", t);
        }
    }

    /** {@code GetPatternProvider(int)} (vtable slot 4): {@code {HRESULT, the IUnknown* it answered}}. */
    public static long[] providerGetPatternProvider(long provider, int patternId) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(ADDRESS);
            out.set(ADDRESS, 0, MemorySegment.ofAddress(0xDEADL));
            int hr = (int) Accessibility.vtableSlot(provider, 4,
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS))
                    .invokeExact(MemorySegment.ofAddress(provider), patternId, out);
            return new long[] {hr, out.get(ADDRESS, 0).address()};
        } catch (Throwable t) {
            throw new AssertionError("GetPatternProvider could not be called", t);
        }
    }

    /** {@code GetPropertyValue(int)} (vtable slot 5): the {@code HRESULT}. */
    public static int providerGetPropertyValue(long provider, int propertyId) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(24);
            return (int) Accessibility.vtableSlot(provider, 5,
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS))
                    .invokeExact(MemorySegment.ofAddress(provider), propertyId, out);
        } catch (Throwable t) {
            throw new AssertionError("GetPropertyValue could not be called", t);
        }
    }

    /**
     * The three shim-only accessibility hooks and the recording tables. The two installers are bound
     * here rather than exposed by the product, for the reason {@link Readback} gives: the product's
     * {@code boundSymbols()} lists the product's own bindings only.
     */
    private static final class Accessibility {

        private static final Linker LINKER = Linker.nativeLinker();
        private static final List<Object> ARGUMENTS = new ArrayList<>();
        private static boolean throwing;

        private static final MethodHandle VARIANT_OFFSETS = bindGlass("gwin_test_variant_offsets",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        private static final MethodHandle FIRE_ACCESSIBLE = bindGlass("gwin_test_fire_accessible_callback",
                FunctionDescriptor.of(JAVA_LONG, JAVA_INT, JAVA_LONG, ADDRESS));
        private static final MethodHandle FIRE_TEXT_RANGE = bindGlass("gwin_test_fire_text_range_callback",
                FunctionDescriptor.of(JAVA_LONG, JAVA_INT, JAVA_LONG, ADDRESS));
        private static final MethodHandle SET_ACCESSIBLE_CALLBACKS = bindGlass("gwin_a11y_set_callbacks",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        private static final MethodHandle SET_TEXT_RANGE_CALLBACKS =
                bindGlass("gwin_a11y_text_range_set_callbacks", FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /**
         * Which recording target each of the facade's 20 accessibility descriptors takes. Declared
         * before {@link #STUBS}, which reads it: a static field initializer sees only what stands above
         * it.
         */
        private static final Map<FunctionDescriptor, String> RECORDERS = Map.ofEntries(
                Map.entry(WinGlassNative.A11Y_ACT_FD, "recordAct"),
                Map.entry(WinGlassNative.A11Y_ACT_I_FD, "recordActI"),
                Map.entry(WinGlassNative.A11Y_ACT_II_FD, "recordActII"),
                Map.entry(WinGlassNative.A11Y_ACT_D_FD, "recordActD"),
                Map.entry(WinGlassNative.A11Y_ACT_DD_FD, "recordActDD"),
                Map.entry(WinGlassNative.A11Y_ACT_IJ_FD, "recordActIJ"),
                Map.entry(WinGlassNative.A11Y_ACT_IJI_FD, "recordActIJI"),
                Map.entry(WinGlassNative.A11Y_ACT_TEXT_FD, "recordActText"),
                Map.entry(WinGlassNative.A11Y_OUT_FD, "recordOut"),
                Map.entry(WinGlassNative.A11Y_OUT2_FD, "recordOut2"),
                Map.entry(WinGlassNative.A11Y_I_OUT_FD, "recordIOut"),
                Map.entry(WinGlassNative.A11Y_I_OUT2_FD, "recordIOut2"),
                Map.entry(WinGlassNative.A11Y_J_OUT_FD, "recordJOut"),
                Map.entry(WinGlassNative.A11Y_DD_OUT_FD, "recordDDOut"),
                Map.entry(WinGlassNative.A11Y_II_OUT_FD, "recordIIOut"),
                Map.entry(WinGlassNative.A11Y_III_OUT_FD, "recordIIIOut"),
                Map.entry(WinGlassNative.A11Y_IJI_OUT_FD, "recordIJIOut"),
                Map.entry(WinGlassNative.A11Y_IVB_OUT_FD, "recordIVBOut"),
                Map.entry(WinGlassNative.A11Y_FIND_TEXT_FD, "recordFindText"),
                Map.entry(WinGlassNative.A11Y_DISPOSED_FD, "recordDisposed"));

        /** One stub per distinct descriptor: a fire drives one slot at a time, so the target is unambiguous. */
        private static final Map<FunctionDescriptor, MemorySegment> STUBS = buildStubs();

        private Accessibility() {
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bindGlass(String name, FunctionDescriptor fd) {
            MemorySegment symbol = SymbolLookup.loaderLookup().find(name).orElseThrow(
                    () -> new UnsatisfiedLinkError("missing native symbol: " + name + " in glass"));
            return LINKER.downcallHandle(symbol, fd);
        }

        @SuppressWarnings("restricted")
        private static Map<FunctionDescriptor, MemorySegment> buildStubs() {
            Map<FunctionDescriptor, MemorySegment> stubs = new HashMap<>();
            List<FunctionDescriptor> descriptors = new ArrayList<>(WinGlassNative.ACCESSIBLE_SLOT_DESCRIPTORS);
            descriptors.addAll(WinGlassNative.TEXT_RANGE_SLOT_DESCRIPTORS);
            for (FunctionDescriptor descriptor : descriptors) {
                if (stubs.containsKey(descriptor)) {
                    continue;
                }
                String target = RECORDERS.get(descriptor);
                if (target == null) {
                    throw new AssertionError("no recording target for descriptor " + descriptor);
                }
                try {
                    MethodHandle handle = MethodHandles.lookup().findStatic(Accessibility.class, target,
                            descriptor.toMethodType());
                    stubs.put(descriptor, LINKER.upcallStub(handle, descriptor, Arena.global()));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError("no recording target " + target, e);
                }
            }
            return stubs;
        }

        private static void writeTables() {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment accessibleTable =
                        arena.allocate(WinGlassNative.GWIN_ACCESSIBLE_CALLBACKS_LAYOUT);
                fill(accessibleTable, WinGlassNative.GWIN_ACCESSIBLE_CALLBACKS_LAYOUT,
                        WinGlassNative.ACCESSIBLE_SLOT_NAMES, WinGlassNative.ACCESSIBLE_SLOT_DESCRIPTORS);
                int accessibleStatus = (int) SET_ACCESSIBLE_CALLBACKS.invokeExact(accessibleTable);
                MemorySegment textRangeTable =
                        arena.allocate(WinGlassNative.GWIN_TEXT_RANGE_CALLBACKS_LAYOUT);
                fill(textRangeTable, WinGlassNative.GWIN_TEXT_RANGE_CALLBACKS_LAYOUT,
                        WinGlassNative.TEXT_RANGE_SLOT_NAMES, WinGlassNative.TEXT_RANGE_SLOT_DESCRIPTORS);
                int textRangeStatus = (int) SET_TEXT_RANGE_CALLBACKS.invokeExact(textRangeTable);
                if (accessibleStatus != WinGlassNative.GWIN_OK || textRangeStatus != WinGlassNative.GWIN_OK) {
                    throw new AssertionError("gwin_a11y_*_set_callbacks answered " + accessibleStatus + " / "
                            + textRangeStatus);
                }
            } catch (Throwable t) {
                throw new AssertionError("the recording accessibility tables could not be installed", t);
            }
        }

        private static void fill(MemorySegment table, StructLayout layout, List<String> names,
                                 List<FunctionDescriptor> descriptors) {
            MemorySegment[] stubs = new MemorySegment[names.size()];
            for (int i = 0; i < stubs.length; i++) {
                stubs[i] = STUBS.get(descriptors.get(i));
            }
            WinGlassNative.fillCallbackTable(table, layout, names, stubs);
        }

        static RecordedSlot fireIntoRecordingTable(boolean accessible, int slot, long id, MemorySegment out) {
            ARGUMENTS.clear();
            throwing = false;
            writeTables();
            try {
                long returned = accessible ? (long) FIRE_ACCESSIBLE.invokeExact(slot, id, out)
                        : (long) FIRE_TEXT_RANGE.invokeExact(slot, id, out);
                return new RecordedSlot(returned, List.copyOf(ARGUMENTS));
            } catch (Throwable t) {
                throw new AssertionError("the recording accessibility table could not be fired", t);
            } finally {
                WinGlassNative.reinstallAccessibilityCallbacks();
            }
        }

        /** The {@code slot}-th entry of {@code provider}'s vtable, bound as a {@code __stdcall} method. */
        @SuppressWarnings("restricted")
        static MethodHandle vtableSlot(long provider, int slot, FunctionDescriptor descriptor) {
            MemorySegment object = MemorySegment.ofAddress(provider).reinterpret(ADDRESS.byteSize());
            MemorySegment vtable = object.get(ADDRESS, 0)
                    .reinterpret((slot + 1L) * ADDRESS.byteSize());
            return LINKER.downcallHandle(vtable.getAtIndex(ADDRESS, slot), descriptor);
        }

        static List<Object> recordDestroy(Runnable destroy) {
            ARGUMENTS.clear();
            throwing = false;
            writeTables();
            try {
                destroy.run();
                return List.copyOf(ARGUMENTS);
            } finally {
                WinGlassNative.reinstallAccessibilityCallbacks();
            }
        }

        static long fireIntoThrowingTable(int slot, long id, MemorySegment out) {
            ARGUMENTS.clear();
            throwing = true;
            writeTables();
            try {
                return (long) FIRE_ACCESSIBLE.invokeExact(slot, id, out);
            } catch (Throwable t) {
                throw new AssertionError("the throwing accessibility table could not be fired", t);
            } finally {
                throwing = false;
                WinGlassNative.reinstallAccessibilityCallbacks();
            }
        }

        private static int record(Object... values) {
            ARGUMENTS.addAll(Arrays.asList(values));
            if (!throwing) {
                return WinGlassNative.GWIN_OK;
            }
            // What every production stub does with a Throwable from its target, in the same two steps:
            // report it where CheckAndClearException sent it, then answer GWIN_ERR_UPCALL. Letting it out
            // of an upcall stub terminates the JVM - measured, not feared.
            try {
                Application.reportException(new RuntimeException(THROWING_SLOT_MESSAGE));
            } catch (Throwable ignored) {
                // Nothing left to try: this is an upcall.
            }
            return WinGlassNative.GWIN_ERR_UPCALL;
        }

        static int recordAct(long id) {
            return record(id);
        }

        static int recordActI(long id, int a) {
            return record(id, a);
        }

        static int recordActII(long id, int a, int b) {
            return record(id, a, b);
        }

        static int recordActD(long id, double a) {
            return record(id, a);
        }

        static int recordActDD(long id, double a, double b) {
            return record(id, a, b);
        }

        static int recordActIJ(long id, int a, long b) {
            return record(id, a, b);
        }

        static int recordActIJI(long id, int a, long b, int c) {
            return record(id, a, b, c);
        }

        static int recordActText(long id, MemorySegment text, int length) {
            return record(id, text(text, length), length);
        }

        static int recordOut(long id, MemorySegment out) {
            return record(id, pointer(out));
        }

        static int recordOut2(long id, MemorySegment out, MemorySegment outCount) {
            return record(id, pointer(out), pointer(outCount));
        }

        static int recordIOut(long id, int a, MemorySegment out) {
            return record(id, a, pointer(out));
        }

        static int recordIOut2(long id, int a, MemorySegment out, MemorySegment outCount) {
            return record(id, a, pointer(out), pointer(outCount));
        }

        static int recordJOut(long id, long a, MemorySegment out) {
            return record(id, a, pointer(out));
        }

        static int recordDDOut(long id, double a, double b, MemorySegment out) {
            return record(id, a, b, pointer(out));
        }

        static int recordIIOut(long id, int a, int b, MemorySegment out) {
            return record(id, a, b, pointer(out));
        }

        static int recordIIIOut(long id, int a, int b, int c, MemorySegment out) {
            return record(id, a, b, c, pointer(out));
        }

        static int recordIJIOut(long id, int a, long b, int c, MemorySegment out) {
            return record(id, a, b, c, pointer(out));
        }

        static int recordIVBOut(long id, int a, MemorySegment value, int b, MemorySegment out) {
            return record(id, a, pointer(value), b, pointer(out));
        }

        static int recordFindText(long id, MemorySegment text, int length, int backward, int ignoreCase,
                                  MemorySegment out) {
            return record(id, text(text, length), length, backward, ignoreCase, pointer(out));
        }

        static void recordDisposed(long id) {
            ARGUMENTS.add(id);
        }

        /** A pointer argument as a {@code Boolean}: whether it was {@code NULL}. Addresses are not stable. */
        private static Object pointer(MemorySegment segment) {
            return !segment.equals(MemorySegment.NULL);
        }

        @SuppressWarnings("restricted")
        private static String text(MemorySegment pointer, int length) {
            if (pointer.equals(MemorySegment.NULL) || length <= 0) {
                return "";
            }
            return new String(pointer.reinterpret(length * 2L).toArray(JAVA_CHAR));
        }

    }
}

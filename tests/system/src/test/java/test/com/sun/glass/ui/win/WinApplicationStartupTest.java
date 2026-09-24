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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Screen;
import com.sun.javafx.PlatformUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import test.util.Util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * What {@code WinApplication._init} leaves behind on a started Windows toolkit, and the display-change event path
 * from the toolkit window procedure back to {@code Screen.notifySettingsChanged}.
 * <p>
 * The assertions do not name the mechanism, on purpose: this class passes with {@code _init},
 * {@code _setClassLoader}, {@code initIDs} and {@code staticScreen_getScreens} still JNI, and must pass unchanged
 * once the toolkit is created through {@code gwin_app_create}, the screens are enumerated in Java and
 * {@code GlassScreen::HandleDisplayChange} dials a callback table instead of calling into Java through JNI. The two
 * runs together are the evidence that those four natives were replaced without a behaviour change. The provenance
 * line printed before the tests says which of the two builds a run exercised.
 * <p>
 * The window-procedure step sends {@code WM_SETTINGCHANGE(SPI_SETWORKAREA)} to the toolkit's own hidden window from
 * the toolkit thread, which runs the window procedure synchronously; it is not synthesized user input and changes
 * no system setting.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WinApplicationStartupTest {

    /** {@code PROCESS_PER_MONITOR_DPI_AWARE} ({@code shellscalingapi.h}), which java.exe's manifest selects. */
    private static final int PROCESS_PER_MONITOR_DPI_AWARE = 2;

    private static final int S_OK = 0;

    private static final int WM_SETTINGCHANGE = 0x001A;

    private static final long SPI_SETWORKAREA = 0x002F;

    /** {@code BaseWnd::Create}'s {@code "GlassWndClass-%s-%u"} with {@code GlassApplication}'s suffix. */
    private static final String TOOLKIT_WINDOW_CLASS_PREFIX = "GlassWndClass-GlassToolkitWindowClass-";

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    @BeforeAll
    static void initFX() {
        assumeTrue(PlatformUtil.isWindows());
        Util.startup(startupLatch, startupLatch::countDown);
        AtomicReference<String> provenance = new AtomicReference<>();
        Util.runAndWait(() -> provenance.set(WinToolkitProbe.provenance()));
        System.out.println("WinApplicationStartupTest provenance\n" + provenance.get());
    }

    @AfterAll
    static void shutdown() {
        assumeTrue(PlatformUtil.isWindows());
        Util.shutdown();
    }

    @Test
    @Order(1)
    public void theToolkitIsTheWindowsOne() {
        AtomicReference<String> toolkit = new AtomicReference<>();
        Util.runAndWait(() -> toolkit.set(Application.GetApplication().getClass().getName()));
        assertEquals("com.sun.glass.ui.win.WinApplication", toolkit.get());
    }

    @Test
    @Order(2)
    public void uiSettingsWereActivatedUnderTheToolkit() {
        AtomicReference<Boolean> available = new AtomicReference<>();
        Util.runAndWait(() -> available.set(WinToolkitProbe.uiSettingsAvailable()));
        System.out.println("WinApplicationStartupTest uiSettings().available() = " + available.get());
        WinToolkitProbe.requireOracle(WinApplicationStartupTest.class, available.get(),
                () -> "gwin_prefs_query_ui_settings reports no WinRT UISettings object after startup: PlatformSupport's"
                        + " RoInitialize(RO_INIT_SINGLETHREADED) or RoActivateInstance(UISettings) failed on the"
                        + " toolkit thread (see the stderr of the test JVM)");
    }

    @Test
    @Order(3)
    public void networkInformationWasActivatedUnderTheToolkit() {
        AtomicReference<Boolean> available = new AtomicReference<>();
        Util.runAndWait(() -> available.set(WinToolkitProbe.networkInfoAvailable()));
        System.out.println("WinApplicationStartupTest networkInfo().available() = " + available.get());
        WinToolkitProbe.requireOracle(WinApplicationStartupTest.class, available.get(),
                () -> "gwin_prefs_query_network reports no WinRT NetworkInformation factory after startup:"
                        + " RoGetActivationFactory(NetworkInformation) failed on the toolkit thread, or RoInitialize"
                        + " did not run there");
    }

    @Test
    @Order(4)
    public void theProcessIsPerMonitorDpiAware() {
        AtomicReference<int[]> awareness = new AtomicReference<>();
        Util.runAndWait(() -> awareness.set(Win32.processDpiAwareness()));
        System.out.println("WinApplicationStartupTest GetProcessDpiAwareness(NULL) = {hresult 0x"
                + Integer.toHexString(awareness.get()[0]) + ", value " + awareness.get()[1] + "}");
        assertEquals(S_OK, awareness.get()[0], "GetProcessDpiAwareness HRESULT");
        assertEquals(PROCESS_PER_MONITOR_DPI_AWARE, awareness.get()[1], "PROCESS_DPI_AWARENESS");
    }

    @Test
    @Order(5)
    public void theStartupScreensWereEnumerated() {
        AtomicReference<List<Screen>> screens = new AtomicReference<>();
        Util.runAndWait(() -> screens.set(Screen.getScreens()));
        System.out.println("WinApplicationStartupTest startup screens " + describe(screens.get()));
        assertFalse(screens.get().isEmpty(), "Screen.getScreens() after startup");
        for (Screen screen : screens.get()) {
            assertNotEquals(0L, screen.getNativeScreen(), "nativeScreen of " + screen);
        }
    }

    @Test
    @Order(6)
    public void aWorkAreaChangeReplacesTheScreenList() {
        AtomicReference<List<Screen>> before = new AtomicReference<>();
        AtomicReference<List<Screen>> beforeSend = new AtomicReference<>();
        AtomicReference<List<Screen>> after = new AtomicReference<>();
        List<Long> toolkitWindows = new ArrayList<>();
        List<String> threadWindows = new ArrayList<>();
        Util.runAndWait(() -> {
            before.set(Screen.getScreens());
            for (long hwnd : Win32.threadWindows(Win32.currentThreadId())) {
                String className = Win32.className(hwnd);
                threadWindows.add("0x" + Long.toHexString(hwnd) + " " + className);
                if (className.startsWith(TOOLKIT_WINDOW_CLASS_PREFIX)) {
                    toolkitWindows.add(hwnd);
                }
            }
            beforeSend.set(Screen.getScreens());
            if (toolkitWindows.size() == 1) {
                Win32.sendMessage(toolkitWindows.get(0), WM_SETTINGCHANGE, SPI_SETWORKAREA, 0L);
                after.set(Screen.getScreens());
            }
        });
        System.out.println("WinApplicationStartupTest toolkit-thread windows " + threadWindows);
        System.out.println("WinApplicationStartupTest screens before " + describe(before.get())
                + " identity " + System.identityHashCode(before.get()));

        assertEquals(1, toolkitWindows.size(),
                "top-level windows of the toolkit thread whose class starts with " + TOOLKIT_WINDOW_CLASS_PREFIX
                        + ": " + threadWindows);
        assertSame(before.get(), beforeSend.get(),
                "Screen.getScreens() was replaced with no display event, so a new instance would prove nothing");
        System.out.println("WinApplicationStartupTest screens after " + describe(after.get())
                + " identity " + System.identityHashCode(after.get()));
        assertNotSame(before.get(), after.get(),
                "WM_SETTINGCHANGE(SPI_SETWORKAREA) did not reach Screen.notifySettingsChanged");
        assertFalse(after.get().isEmpty(), "Screen.getScreens() after the work-area change");
        assertEquals(nativeScreens(before.get()), nativeScreens(after.get()),
                "the re-enumeration after a work-area change that changed nothing");
    }

    private static List<Long> nativeScreens(List<Screen> screens) {
        return screens.stream().map(Screen::getNativeScreen).toList();
    }

    private static String describe(List<Screen> screens) {
        StringBuilder text = new StringBuilder().append(screens.size()).append(" [");
        for (Screen screen : screens) {
            text.append("{native 0x").append(Long.toHexString(screen.getNativeScreen()))
                    .append(" ").append(screen.getX()).append(',').append(screen.getY())
                    .append(' ').append(screen.getWidth()).append('x').append(screen.getHeight())
                    .append(" scale ").append(screen.getPlatformScaleX()).append('}');
        }
        return text.append(']').toString();
    }

    /**
     * The Win32 calls this test makes itself, bound here rather than through the Glass facade so that the facade's
     * bound-symbol list is untouched. Every one runs on the toolkit thread.
     */
    private static final class Win32 {

        private static final int CLASS_NAME_CHARS = 256;

        /** {@code DWORD GetCurrentThreadId(void)}. */
        private static final MethodHandle GET_CURRENT_THREAD_ID = bind("kernel32.dll", "GetCurrentThreadId",
                FunctionDescriptor.of(JAVA_INT));

        /** {@code BOOL EnumThreadWindows(DWORD dwThreadId, WNDENUMPROC lpfn, LPARAM lParam)}. */
        private static final MethodHandle ENUM_THREAD_WINDOWS = bind("user32.dll", "EnumThreadWindows",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG));

        /** {@code int GetClassNameW(HWND hWnd, LPWSTR lpClassName, int nMaxCount)}. */
        private static final MethodHandle GET_CLASS_NAME = bind("user32.dll", "GetClassNameW",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

        /** {@code LRESULT SendMessageW(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam)}. */
        private static final MethodHandle SEND_MESSAGE = bind("user32.dll", "SendMessageW",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG));

        /** {@code HRESULT GetProcessDpiAwareness(HANDLE hprocess, PROCESS_DPI_AWARENESS *value)}. */
        private static final MethodHandle GET_PROCESS_DPI_AWARENESS = bind("shcore.dll", "GetProcessDpiAwareness",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        /** {@code BOOL CALLBACK EnumWindowsProc(HWND hwnd, LPARAM lParam)}. */
        private static final FunctionDescriptor ENUM_WINDOWS_PROC = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG);

        private static final MethodHandle COLLECT_WINDOW = findCollectWindow();

        private Win32() {
        }

        static int currentThreadId() {
            try {
                return (int) GET_CURRENT_THREAD_ID.invokeExact();
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }

        /** {@code {hresult, value}} of {@code GetProcessDpiAwareness(NULL, &value)}; value -1 if not written. */
        static int[] processDpiAwareness() {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment value = arena.allocate(JAVA_INT);
                value.set(JAVA_INT, 0, -1);
                int hresult = (int) GET_PROCESS_DPI_AWARENESS.invokeExact(MemorySegment.NULL, value);
                return new int[] {hresult, value.get(JAVA_INT, 0)};
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }

        /** The top-level windows {@code EnumThreadWindows} reports for {@code threadId}, in its order. */
        @SuppressWarnings("restricted")
        static List<Long> threadWindows(int threadId) {
            List<Long> windows = new ArrayList<>();
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment callback = Linker.nativeLinker().upcallStub(
                        MethodHandles.insertArguments(COLLECT_WINDOW, 0, windows), ENUM_WINDOWS_PROC, arena);
                int ignored = (int) ENUM_THREAD_WINDOWS.invokeExact(threadId, callback, 0L);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return windows;
        }

        static String className(long hwnd) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment buffer = arena.allocate(JAVA_CHAR, CLASS_NAME_CHARS);
                int length = (int) GET_CLASS_NAME.invokeExact(windowHandle(hwnd), buffer, CLASS_NAME_CHARS);
                return length <= 0 ? "" : buffer.getString(0, StandardCharsets.UTF_16LE);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }

        static long sendMessage(long hwnd, int message, long wParam, long lParam) {
            try {
                return (long) SEND_MESSAGE.invokeExact(windowHandle(hwnd), message, wParam, lParam);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }

        /** The upcall target: records one window and continues the enumeration. Must not throw. */
        private static int collectWindow(List<Long> windows, MemorySegment hwnd, long lParam) {
            windows.add(hwnd.address());
            return 1;
        }

        private static MemorySegment windowHandle(long hwnd) {
            return MemorySegment.ofAddress(hwnd);
        }

        private static MethodHandle findCollectWindow() {
            try {
                return MethodHandles.lookup().findStatic(Win32.class, "collectWindow",
                        MethodType.methodType(int.class, List.class, MemorySegment.class, long.class));
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bind(String library, String symbol, FunctionDescriptor descriptor) {
            MemorySegment address = SymbolLookup.libraryLookup(library, Arena.global()).find(symbol)
                    .orElseThrow(() -> new UnsatisfiedLinkError(library + " has no " + symbol));
            return Linker.nativeLinker().downcallHandle(address, descriptor);
        }
    }
}

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

import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.win.WinGlassNativeShim;
import com.sun.glass.ui.win.WinGlassNativeShim.HeadlessView;
import com.sun.glass.ui.win.WinGlassNativeShim.HeadlessWindow;
import com.sun.glass.ui.win.WinGlassNativeShim.MovingResult;
import com.sun.glass.ui.win.WinGlassNativeShim.RecordedWindowCall;
import com.sun.glass.ui.win.WinGlassNativeShim.WindowFire;
import com.sun.glass.ui.win.WinGlassNativeShim.WindowState;
import com.sun.javafx.tk.HeaderAreaType;
import com.sun.prism.impl.PrismSettings;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The window peer, {@code WinWindow}, on the {@code gwin_window_*} ABI: the
 * twenty-nine exports plus the test hook, the callback table ({@code GwinWindowCallbacks}, twelve slots) and the twelve
 * upcall targets behind it, which replace the eleven {@code Call*Method} sites of
 * {@code GlassWindow.cpp} and the {@code DeleteGlobalRef} of {@code ~GlassWindow}; the
 * {@code _getAnchor} WRAPPER over four {@code user32} calls; the {@code Cursor} to {@code HCURSOR}
 * mapping of {@code GlassCursor.cpp}, now in {@code WinCursor}; and the icon that
 * {@code Pixels::CreateIcon} built, now built in Java and owned by the window.
 * <p>
 * <b>Two oracles, as for the view table.</b> {@code gwin_test_fire_window_callback} drives one slot
 * of the installed table with a pattern fixed in the header. Fired into a <em>recording</em> table
 * built from the facade's own descriptor objects ({@link WinGlassNativeShim#fireWindowIntoRecordingTable})
 * it proves each of the twelve {@code FunctionDescriptor}s argument by argument - above all
 * {@code notify_moving}'s four ints, two floats, seven ints and out-pointer, where one integer out
 * of place shifts the anchor into a float's bit pattern with no crash - and the out-parameter both
 * ways. Fired into the <em>production</em> table with a headless {@code WinWindow} registered under
 * the id ({@link WinGlassNativeShim#createHeadlessWindow}) it proves the registry lookup, the
 * virtual dispatch and {@code Window}'s bookkeeping, observed through {@code Window.EventHandler},
 * the window's own getters and the recording {@code notifyMoving} of the headless subclass. Each
 * target is also driven directly, for the arms the fixed pattern cannot reach.
 * <p>
 * <b>Nothing here has a toolkit.</b> Every marshalled export no-ops without one and returns
 * indeterminate stack contents, so no return of theirs is asserted, the headless window's
 * handle is a value {@code IsWindow} rejects, and the real-window behaviour - moving between
 * monitors, focus and grab, the non-client frame of an EXTENDED stage, the dark frame, fullscreen
 * and the destruction order - belongs to {@code tests/system}.
 */
@EnabledOnOs(OS.WINDOWS)
public class WinWindowNativeTest {

    /** The twenty-nine exports of the header's window section plus its test hook, in the header's order. */
    static final List<String> WINDOW_EXPORTS = List.of("gwin_sizeof_window_callbacks",
            "gwin_window_set_callbacks", "gwin_test_fire_window_callback", "gwin_window_create", "gwin_window_close",
            "gwin_window_set_view",
            "gwin_window_update_view_size", "gwin_window_set_menubar", "gwin_window_set_level",
            "gwin_window_set_focusable", "gwin_window_set_enabled", "gwin_window_set_alpha",
            "gwin_window_set_dark_frame", "gwin_window_get_insets", "gwin_window_set_bounds",
            "gwin_window_set_title", "gwin_window_set_resizable", "gwin_window_set_visible",
            "gwin_window_request_focus", "gwin_window_grab_focus", "gwin_window_ungrab_focus",
            "gwin_window_minimize", "gwin_window_maximize", "gwin_window_set_min_size",
            "gwin_window_set_max_size", "gwin_window_set_icon", "gwin_window_to_front", "gwin_window_to_back",
            "gwin_window_set_cursor", "gwin_window_show_system_menu");

    /** An id the registry never hands out: it counts up from 1 and no test registers this many. */
    static final long UNKNOWN_ID = 0x7FFF_FFF0L;

    /** {@code GwinStatus.GWIN_ERR_INVALID_ARG}, what the hook answers for an unknown slot, no table, or a NULL out. */
    static final long INVALID_ARG = -1L;

    /** The header's {@code notify_delegate_ptr} pattern: bits above 32 set, so a truncated pointer shows. */
    static final long HOOK_POINTER = 0x0000_7FFE_1234_5678L;

    /* winuser.h WM_NCHITTEST answers, and WinWindow's HTUNSPECIFIED; pinned by static_assert in glass_win_api.cpp. */
    static final int HTNOWHERE = 0;
    static final int HTCLIENT = 1;
    static final int HTCAPTION = 2;
    static final int HTCLOSE = 20;

    /** The ten IDC_* ordinals GetNativeCursor picks, static_assert-pinned in glass_win_api.cpp. */
    static final int[] IDC_ORDINALS = {32512, 32513, 32514, 32515, 32642, 32643, 32644, 32645, 32646, 32649};

    /** Why a desktop-dependent assertion was skipped; see {@link #requireDesktop()}. */
    static final String DESKTOP_REQUIRED =
            "this JVM has no interactive window station, so user32 cannot create a window, take the capture or"
            + " load a cursor: the anchor and cursor assertions need a desktop session, not a service or session 0.";

    private static boolean desktopAvailable;

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(WinWindowNativeTest.class);

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        // The three lazy holders, in the order WinGlassNativeTest's exact BOUND_SYMBOLS expects them.
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        WinGlassNativeShim.bindBrowserSymbols();
        desktopAvailable = WinGlassNativeShim.getSystemMetrics(WinGlassNativeShim.constant("SM_CXSCREEN")) > 0
                && WinGlassNativeShim.desktopWindow() != 0L;
    }

    private static void requireDesktop() {
        LEDGER.requireOracle(desktopAvailable, () -> DESKTOP_REQUIRED);
        LEDGER.compared();
    }

    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    // ---------------------------------------------------------------------------------------------
    // Symbols, sizes, the install, the flip
    // ---------------------------------------------------------------------------------------------

    @Test
    public void theThirtyWindowExportsAndTheFiveWin32SymbolsResolve() {
        for (String export : WINDOW_EXPORTS) {
            assertTrue(WinGlassNativeShim.resolves("glass!" + export), export);
        }
        assertEquals(30, WINDOW_EXPORTS.size());
        for (String symbol : List.of("user32!IsWindow", "user32!GetCapture", "user32!GetWindowRect",
                "user32!LoadCursorW", "kernel32!GetModuleHandleW")) {
            assertTrue(WinGlassNativeShim.resolves(symbol), symbol);
        }
    }

    /**
     * The table against the {@code sizeof} the C compiler computed, every slot at {@code 8 * index}
     * read through {@code PathElement}, and {@code RECT} at its four {@code LONG} offsets. A slot
     * appended in C without the layout following it would have the installer write twelve pointers
     * into a thirteen-pointer table and leave the last one holding the arena's leftovers.
     */
    @Test
    public void theWindowTableHasTheSizeTheCCompilerGaveItAndTwelveSlotsEightBytesApart() {
        assertEquals(96, WinGlassNativeShim.sizeOfWindowCallbacks());
        assertEquals(96, WinGlassNativeShim.layoutByteSize("GwinWindowCallbacks"));
        List<String> slots = WinGlassNativeShim.windowSlotNames();
        assertEquals(List.of("notify_close", "notify_destroy", "notify_moving", "notify_move", "notify_resize",
                "notify_scale_changed", "notify_focus", "notify_focus_disabled", "notify_focus_ungrab",
                "notify_delegate_ptr", "non_client_hit_test", "notify_dispose"), slots);
        for (int i = 0; i < slots.size(); i++) {
            assertEquals(8L * i, WinGlassNativeShim.offset("GwinWindowCallbacks", slots.get(i)), slots.get(i));
        }
        assertEquals(16, WinGlassNativeShim.layoutByteSize("RECT"));
        assertEquals(0, WinGlassNativeShim.offset("RECT", "left"));
        assertEquals(4, WinGlassNativeShim.offset("RECT", "top"));
        assertEquals(8, WinGlassNativeShim.offset("RECT", "right"));
        assertEquals(12, WinGlassNativeShim.offset("RECT", "bottom"));
    }

    /**
     * Installed by {@code WinApplication}'s static initializer beside the view tables; touching the
     * peer is enough. All twelve slots are written - a {@code NULL} slot would become a C no-op, and
     * for {@code non_client_hit_test} that no-op answers {@code HTNOWHERE} - and installing again, or
     * re-installing, changes nothing.
     */
    @Test
    public void theTableIsInstalledByWinApplicationsStaticInitializerWithNoNullSlot() {
        WinGlassNativeShim.platformKeys();   // forces the peer, and so WinApplication's initializer
        assertTrue(WinGlassNativeShim.windowCallbacksInstalled());
        List<Long> stubs = WinGlassNativeShim.installedWindowCallbackStubAddresses();
        assertEquals(12, stubs.size());
        assertFalse(stubs.contains(0L), "a NULL slot: " + stubs);
        assertEquals(12, stubs.stream().distinct().count(), "two slots sharing a stub: " + stubs);
        WinGlassNativeShim.installWindowCallbacks();
        WinGlassNativeShim.reinstallWindowCallbacks();
        assertEquals(stubs, WinGlassNativeShim.installedWindowCallbackStubAddresses());
    }

    /**
     * Nothing is left {@code native} on {@code WinWindow}: the twenty-seven exports, the WRAPPER,
     * the deleted {@code _initIDs} (its twelve method ids are the table) and the dead
     * {@code _setBackground2}. Nor on {@code WinPixels}: {@code _attachInt} / {@code _attachByte}, the
     * return leg of the {@code _setIcon} upcall, are throwing overrides now that no JNI body dials them.
     */
    @Test
    public void winWindowDeclaresNoNativeMethod() {
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinWindow"));
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinPixels"));
    }

    /**
     * {@code DestroyIcon} is bound nowhere in the product: the window owns the {@code HICON}
     * {@code iconCreate} builds from the moment {@code gwin_window_set_icon} takes it, and destroying
     * it from Java would be a double free in {@code GlassWindow::SetIcon}. The read-back binds it for
     * the tests, in the shim only.
     */
    @Test
    public void theProductBindsNoDestroyIconOrDestroyCursor() {
        List<String> bound = WinGlassNativeShim.boundSymbols();
        assertFalse(bound.stream().anyMatch(s -> s.endsWith("!DestroyIcon")), bound.toString());
        assertFalse(bound.stream().anyMatch(s -> s.endsWith("!DestroyCursor")), bound.toString());
        assertTrue(bound.contains("glass!gwin_window_set_icon"));
    }

    // ---------------------------------------------------------------------------------------------
    // The registry
    // ---------------------------------------------------------------------------------------------

    /**
     * Empty at rest; ids count up from 1 and are never reused; {@code notify_dispose} is the removal
     * and nothing else - the window is untouched, no event is sent, nothing is reported - and a
     * second dispose, or any other slot, for a removed id is silent.
     */
    @Test
    public void theWindowRegistryIsEmptyAtRestAndDisposeIsTheOnlyRemoval() {
        assertEquals(0, WinGlassNativeShim.windowRegistrySize());
        RecordingWindowHandler handler = new RecordingWindowHandler();
        HeadlessWindow first = WinGlassNativeShim.createHeadlessWindow(handler, screen(), Window.TITLED);
        HeadlessWindow second = WinGlassNativeShim.createHeadlessWindow(new RecordingWindowHandler(), screen(),
                Window.TITLED);
        try {
            assertTrue(first.id() >= 1L);
            assertTrue(second.id() > first.id());
            assertEquals(first.id(), WinGlassNativeShim.windowIdOf(first.window()));
            assertEquals(2, WinGlassNativeShim.windowRegistrySize());
            assertEquals(WinGlassNativeShim.HEADLESS_HWND, first.window().getRawHandle());

            assertNull(reportedBy(() -> WinGlassNativeShim.windowDisposeTarget(first.id())));
            assertEquals(1, WinGlassNativeShim.windowRegistrySize());
            assertEquals(List.of(), handler.calls, "dispose must not send an event");
            assertFalse(WinGlassNativeShim.windowState(first.window()).closed(), "dispose must not touch ptr");

            assertNull(reportedBy(() -> WinGlassNativeShim.windowDisposeTarget(first.id())));
            assertNull(reportedBy(() -> WinGlassNativeShim.windowCloseTarget(first.id())));
            assertEquals(List.of(), handler.calls, "a removed id is silent");
            assertEquals(1, WinGlassNativeShim.windowRegistrySize());
        } finally {
            WinGlassNativeShim.unregisterWindow(first.id());
            WinGlassNativeShim.unregisterWindow(second.id());
        }
        assertEquals(0, WinGlassNativeShim.windowRegistrySize());
    }

    // ---------------------------------------------------------------------------------------------
    // The twelve targets, driven directly with a registered headless WinWindow
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code notify_close} is the CLOSE event and nothing more (Alt+F4 destroys nothing until Java
     * calls {@code close()}); {@code notify_destroy} is the DESTROY event and {@code ptr = 0}, after
     * which a second destroy is a no-op inside {@code Window.notifyDestroy} and the window reads
     * as closed.
     */
    @Test
    public void notifyCloseAndNotifyDestroyReachTheWindow() {
        withWindow(Window.TITLED, handler -> window -> {
            WinGlassNativeShim.windowCloseTarget(window.id());
            assertEquals(List.of(event(WindowEvent.CLOSE)), handler.calls);
            assertSame(window.window(), handler.lastWindow);
            assertFalse(WinGlassNativeShim.windowState(window.window()).closed());

            handler.calls.clear();
            WinGlassNativeShim.windowDestroyTarget(window.id());
            assertEquals(List.of(event(WindowEvent.DESTROY)), handler.calls);
            WindowState state = WinGlassNativeShim.windowState(window.window());
            assertTrue(state.closed());
            assertEquals(0L, state.rawHandle());

            handler.calls.clear();
            WinGlassNativeShim.windowDestroyTarget(window.id());
            assertEquals(List.of(), handler.calls, "Window.notifyDestroy returns early once ptr is 0");
        });
    }

    /**
     * {@code notify_move} stores the origin and sends MOVE; {@code notify_resize} runs
     * {@code WinWindow}'s override - which asks {@code gwin_window_get_insets} for the fake handle
     * and gets 0 - then {@code Window}'s, which stores the extent and sends the type, plus RESIZE
     * after a MAXIMIZE or RESTORE.
     */
    @Test
    public void notifyMoveAndNotifyResizeReachTheWindow() {
        withWindow(Window.TITLED, handler -> window -> {
            WinGlassNativeShim.windowMoveTarget(window.id(), 11, -22);
            assertEquals(List.of(event(WindowEvent.MOVE)), handler.calls);
            WindowState moved = WinGlassNativeShim.windowState(window.window());
            assertEquals(11, moved.x());
            assertEquals(-22, moved.y());

            handler.calls.clear();
            WinGlassNativeShim.windowResizeTarget(window.id(), WindowEvent.RESIZE, 300, 200);
            assertEquals(List.of(event(WindowEvent.RESIZE)), handler.calls);
            WindowState resized = WinGlassNativeShim.windowState(window.window());
            assertEquals(300, resized.width());
            assertEquals(200, resized.height());

            handler.calls.clear();
            WinGlassNativeShim.windowResizeTarget(window.id(), WindowEvent.MAXIMIZE, 1920, 1040);
            assertEquals(List.of(event(WindowEvent.MAXIMIZE), event(WindowEvent.RESIZE)), handler.calls);
            assertEquals(1920, WinGlassNativeShim.windowState(window.window()).width());
        });
    }

    /**
     * {@code notify_scale_changed} stores the four scales and sends RESCALE - when HiDPI scaling is
     * allowed at all; {@code Window.notifyScaleChanged} returns first otherwise, and the test reads
     * the same flag it does.
     */
    @Test
    public void notifyScaleChangedReachesTheWindow() {
        withWindow(Window.TITLED, handler -> window -> {
            WinGlassNativeShim.windowScaleChangedTarget(window.id(), 1.5f, 1.75f, 2f, 2.25f);
            WindowState state = WinGlassNativeShim.windowState(window.window());
            if (PrismSettings.allowHiDPIScaling) {
                assertEquals(List.of(event(WindowEvent.RESCALE)), handler.calls);
                assertEquals(1.5f, state.platformScaleX());
                assertEquals(1.75f, state.platformScaleY());
            } else {
                assertEquals(List.of(), handler.calls);
                assertEquals(1f, state.platformScaleX());
            }
        });
    }

    /**
     * {@code notify_focus} toggles {@code isFocused} and sends the event only on a change;
     * {@code notify_focus_disabled} and {@code notify_focus_ungrab} are one event each.
     */
    @Test
    public void theThreeFocusSlotsReachTheWindow() {
        withWindow(Window.TITLED, handler -> window -> {
            WinGlassNativeShim.windowFocusTarget(window.id(), WindowEvent.FOCUS_GAINED);
            assertEquals(List.of(event(WindowEvent.FOCUS_GAINED)), handler.calls);
            assertTrue(WinGlassNativeShim.windowState(window.window()).focused());

            WinGlassNativeShim.windowFocusTarget(window.id(), WindowEvent.FOCUS_GAINED);
            assertEquals(List.of(event(WindowEvent.FOCUS_GAINED)), handler.calls, "no change, no event");

            WinGlassNativeShim.windowFocusTarget(window.id(), WindowEvent.FOCUS_LOST);
            assertEquals(List.of(event(WindowEvent.FOCUS_GAINED), event(WindowEvent.FOCUS_LOST)), handler.calls);
            assertFalse(WinGlassNativeShim.windowState(window.window()).focused());

            handler.calls.clear();
            WinGlassNativeShim.windowFocusDisabledTarget(window.id());
            WinGlassNativeShim.windowFocusUngrabTarget(window.id());
            assertEquals(List.of(event(WindowEvent.FOCUS_DISABLED), event(WindowEvent.FOCUS_UNGRAB)), handler.calls);
        });
    }

    /**
     * {@code notify_delegate_ptr} stores the fullscreen HWND, which {@code Window.getNativeHandle()}
     * then answers in place of {@code ptr} - and {@code getRawHandle()} does not; {@code NULL}
     * detaches. The pointer crosses as its address, sixty-four bits of it.
     */
    @Test
    public void notifyDelegatePtrIsStoredAndPublishedByGetNativeHandle() {
        withWindow(Window.TITLED, handler -> window -> {
            long delegate = 0x0000_7FFE_1234_5678L;
            WinGlassNativeShim.windowDelegatePtrTarget(window.id(), delegate);
            WindowState attached = WinGlassNativeShim.windowState(window.window());
            assertEquals(delegate, attached.nativeHandle());
            assertEquals(WinGlassNativeShim.HEADLESS_HWND, attached.rawHandle());
            assertEquals(List.of(), handler.calls, "no event");

            WinGlassNativeShim.windowDelegatePtrTarget(window.id(), 0L);
            assertEquals(WinGlassNativeShim.HEADLESS_HWND,
                    WinGlassNativeShim.windowState(window.window()).nativeHandle());
        });
    }

    /**
     * {@code notify_moving}, the query: the thirteen scalars reach {@code notifyMoving} as sent, a
     * null answer writes nothing and returns 0, a four-int answer is written and returns 1, an
     * answer of any other length - the JNI's "bad array length" arm - writes nothing, returns 0 and
     * is silent, and {@code WinWindow}'s real body, for a rectangle inside the window's own screen,
     * answers null.
     */
    @Test
    public void notifyMovingWritesTheOverrideOrNothing() {
        withWindow(Window.TITLED, handler -> window -> {
            MovingResult none = WinGlassNativeShim.windowMovingTarget(window.id(), 10, 20, 300, 200, 0f, 0f,
                    7, 8, 1, 1, 2, 3, 4);
            assertEquals(0, none.status());
            assertUntouched(none);
            assertEquals(List.of("moving[10, 20, 300, 200, 0.0, 0.0, 7, 8, 1, 1, 2, 3, 4]"),
                    WinGlassNativeShim.movingCalls(window.window()));

            WinGlassNativeShim.setMovingAnswer(window.window(), new int[] {-5, 6, 700, 800});
            MovingResult four = WinGlassNativeShim.windowMovingTarget(window.id(), 10, 20, 300, 200, 1.5f, -2.5f,
                    7, 8, 0, 1, 2, 3, 4);
            assertEquals(1, four.status());
            assertArrayEquals(new int[] {-5, 6, 700, 800}, four.bounds());
            assertEquals("moving[10, 20, 300, 200, 1.5, -2.5, 7, 8, 0, 1, 2, 3, 4]",
                    WinGlassNativeShim.movingCalls(window.window()).get(1));

            WinGlassNativeShim.setMovingAnswer(window.window(), new int[] {1, 2, 3});
            AtomicReference<MovingResult> three = new AtomicReference<>();
            assertNull(reportedBy(() -> three.set(WinGlassNativeShim.windowMovingTarget(window.id(), 0, 0, 1, 1,
                    0f, 0f, 0, 0, 1, 0, 0, 0, 0))));
            assertEquals(0, three.get().status());
            assertUntouched(three.get());

            WinGlassNativeShim.setRealMoving(window.window(), true);
            MovingResult real = WinGlassNativeShim.windowMovingTarget(window.id(), 10, 20, 300, 200, 0f, 0f,
                    0, 0, 1, 0, 0, 0, 0);
            assertEquals(0, real.status(), "a rect inside the window's screen is no screen switch");
            assertUntouched(real);
            assertEquals(List.of(), handler.calls);
        });
    }

    /**
     * {@code non_client_hit_test}, the query, through the real {@code nonClientHitTest}: HTCLIENT for
     * a window that is not EXTENDED and for an EXTENDED one with no view - distinguishable from the
     * 0 an unknown id or a throw answers - and, with a headless {@code WinView} attached, whatever
     * the view's handler picks: HTCAPTION for the drag bar, HTCLOSE for the close region.
     */
    @Test
    public void nonClientHitTestAnswersThroughTheRealMethod() {
        withWindow(Window.TITLED, handler -> window ->
                assertEquals(HTCLIENT, WinGlassNativeShim.windowNonClientHitTestTarget(window.id(), 5, 5)));
        withWindow(Window.EXTENDED, handler -> window -> {
            assertEquals(HTCLIENT, WinGlassNativeShim.windowNonClientHitTestTarget(window.id(), 5, 5), "no view");
            HeaderPicker picker = new HeaderPicker();
            HeadlessView view = WinGlassNativeShim.createHeadlessView(picker);
            try {
                WinGlassNativeShim.attachView(window.window(), view.view());
                picker.area = HeaderAreaType.DRAGBAR;
                assertEquals(HTCAPTION, WinGlassNativeShim.windowNonClientHitTestTarget(window.id(), 50, 10));
                assertEquals(List.of("pick[50.0, 10.0]"), picker.picks);
                picker.area = HeaderAreaType.CLOSE;
                assertEquals(HTCLOSE, WinGlassNativeShim.windowNonClientHitTestTarget(window.id(), 1, 2));
                picker.area = null;
                assertEquals(HTCLIENT, WinGlassNativeShim.windowNonClientHitTestTarget(window.id(), 1, 2));
            } finally {
                WinGlassNativeShim.attachView(window.window(), null);
                WinGlassNativeShim.unregisterView(view.id());
            }
        });
    }

    /**
     * An id the registry does not know - a stale peer, or a JNI-created window's 0 - answers the
     * slot default and is <em>not</em> reported: no handler runs, nothing reaches
     * {@code Application.reportException}, the two queries answer 0 and write nothing.
     */
    @Test
    public void anUnknownIdIsSilentOnEveryWindowSlot() {
        withWindow(Window.TITLED, handler -> window -> {
            for (long id : new long[] {UNKNOWN_ID, 0L}) {
                List<Runnable> slots = List.of(
                        () -> WinGlassNativeShim.windowCloseTarget(id),
                        () -> WinGlassNativeShim.windowDestroyTarget(id),
                        () -> WinGlassNativeShim.windowMoveTarget(id, 1, 2),
                        () -> WinGlassNativeShim.windowResizeTarget(id, WindowEvent.RESIZE, 3, 4),
                        () -> WinGlassNativeShim.windowScaleChangedTarget(id, 2f, 2f, 2f, 2f),
                        () -> WinGlassNativeShim.windowFocusTarget(id, WindowEvent.FOCUS_GAINED),
                        () -> WinGlassNativeShim.windowFocusDisabledTarget(id),
                        () -> WinGlassNativeShim.windowFocusUngrabTarget(id),
                        () -> WinGlassNativeShim.windowDelegatePtrTarget(id, 5L),
                        () -> WinGlassNativeShim.windowDisposeTarget(id));
                for (int slot = 0; slot < slots.size(); slot++) {
                    Throwable reported = reportedBy(slots.get(slot));
                    assertNull(reported, "id " + id + " slot " + slot + " reported " + reported);
                }
                AtomicReference<MovingResult> moving = new AtomicReference<>();
                assertNull(reportedBy(() -> moving.set(WinGlassNativeShim.windowMovingTarget(id, 1, 2, 3, 4, 0f, 0f,
                        0, 0, 1, 0, 0, 0, 0))));
                assertEquals(0, moving.get().status());
                assertUntouched(moving.get());
                AtomicReference<Integer> hit = new AtomicReference<>();
                assertNull(reportedBy(() -> hit.set(WinGlassNativeShim.windowNonClientHitTestTarget(id, 1, 2))));
                assertEquals(HTNOWHERE, hit.get());
            }
            assertEquals(List.of(), handler.calls, "the registered window must not have been touched");
            assertEquals(1, WinGlassNativeShim.windowRegistrySize());
        });
    }

    /**
     * <b>A throwing target is reported and answers the slot default; nothing escapes.</b> That is
     * {@code CheckAndClearException}: {@code Application.reportException} on this thread, then carry
     * on. The handler throws after recording, so each slot is seen to reach it exactly once;
     * {@code notify_moving} throws from {@code notifyMoving} and answers 0 with the buffer untouched;
     * {@code non_client_hit_test} throws from the view's picker and answers {@code HTNOWHERE}, not
     * {@code HTCLIENT}.
     */
    @Test
    public void aThrowingTargetIsReportedAndAnswersTheSlotDefault() {
        withWindow(Window.EXTENDED, handler -> window -> {
            handler.failure = new IllegalStateException("WinWindowNativeTest: deliberate");
            List<Runnable> slots = List.of(
                    () -> WinGlassNativeShim.windowCloseTarget(window.id()),
                    () -> WinGlassNativeShim.windowMoveTarget(window.id(), 1, 2),
                    () -> WinGlassNativeShim.windowResizeTarget(window.id(), WindowEvent.RESIZE, 3, 4),
                    () -> WinGlassNativeShim.windowFocusTarget(window.id(), WindowEvent.FOCUS_GAINED),
                    () -> WinGlassNativeShim.windowFocusDisabledTarget(window.id()),
                    () -> WinGlassNativeShim.windowFocusUngrabTarget(window.id()));
            for (int slot = 0; slot < slots.size(); slot++) {
                handler.calls.clear();
                Throwable reported = reportedBy(slots.get(slot));
                assertSame(handler.failure, reported, "slot " + slot);
                assertEquals(1, handler.calls.size(), "slot " + slot + " reached the handler once: " + handler.calls);
            }
            if (PrismSettings.allowHiDPIScaling) {
                handler.calls.clear();
                assertSame(handler.failure,
                        reportedBy(() -> WinGlassNativeShim.windowScaleChangedTarget(window.id(), 2f, 2f, 2f, 2f)));
                assertEquals(1, handler.calls.size());
            }

            RuntimeException movingFailure = new IllegalArgumentException("WinWindowNativeTest: moving");
            WinGlassNativeShim.setMovingFailure(window.window(), movingFailure);
            AtomicReference<MovingResult> moving = new AtomicReference<>();
            assertSame(movingFailure, reportedBy(() -> moving.set(WinGlassNativeShim.windowMovingTarget(window.id(),
                    1, 2, 3, 4, 0f, 0f, 0, 0, 1, 0, 0, 0, 0))));
            assertEquals(0, moving.get().status());
            assertUntouched(moving.get());

            HeaderPicker picker = new HeaderPicker();
            picker.failure = new UnsupportedOperationException("WinWindowNativeTest: pick");
            HeadlessView view = WinGlassNativeShim.createHeadlessView(picker);
            try {
                WinGlassNativeShim.attachView(window.window(), view.view());
                AtomicReference<Integer> hit = new AtomicReference<>();
                assertSame(picker.failure,
                        reportedBy(() -> hit.set(WinGlassNativeShim.windowNonClientHitTestTarget(window.id(), 3, 4))));
                assertEquals(HTNOWHERE, hit.get(), "HTNOWHERE, as CallIntMethod's 0 became; not HTCLIENT");
            } finally {
                WinGlassNativeShim.attachView(window.window(), null);
                WinGlassNativeShim.unregisterView(view.id());
            }

            // DESTROY last: it zeroes ptr, after which the window is closed for every later slot.
            handler.calls.clear();
            assertSame(handler.failure, reportedBy(() -> WinGlassNativeShim.windowDestroyTarget(window.id())));
            assertEquals(List.of(event(WindowEvent.DESTROY)), handler.calls);
        });
    }

    // ---------------------------------------------------------------------------------------------
    // The through-C oracle, part one: every descriptor, argument by argument, out-parameter both ways
    // ---------------------------------------------------------------------------------------------

    /**
     * Each of the twelve slots, fired into recording stubs built from the facade's own
     * {@code FunctionDescriptor} constants, delivers exactly the pattern
     * {@code gwin_test_fire_window_callback}'s header comment promises: a descriptor with a parameter
     * out of place shows as a shifted tuple or the bit pattern of a float where an int was expected,
     * with no crash. Slot 2's four ints, two floats and seven ints are the sequence that matters;
     * slot 5's four floats are distinct, which the production site never makes them; slot 9's
     * pointer has bits above 32 set, as has the id in every pattern. Slot 2's out-parameter is proved
     * both ways - the four ints arrive when the target writes them, the sentinel is intact when it
     * answers 0 - and a NULL one is rejected by the C before the target, while the other eleven
     * slots accept NULL.
     */
    @Test
    public void everySlotDeliversTheHeadersPatternThroughTheFacadesDescriptor() {
        long id = 0x0102_0304_0506_0708L;
        assertNotEquals(0L, id >>> 32);
        assertNotEquals(0L, HOOK_POINTER >>> 32);
        assertRecorded(0, id, 0L, List.of(id));
        assertRecorded(1, id, 0L, List.of(id));

        RecordedWindowCall written = WinGlassNativeShim.fireWindowIntoRecordingTable(2, id, true, true);
        assertEquals(List.of(id, 1001, 1002, 1003, 1004, 1.5f, 2.5f, 1005, 1006, 1, 1007, 1008, 1009, 1010),
                written.arguments());
        assertEquals(WinGlassNativeShim.RECORDING_MOVING_STATUS, written.returned(), "the target's answer comes back");
        assertArrayEquals(WinGlassNativeShim.RECORDING_BOUNDS, written.bounds(), "the four ints arrived");
        RecordedWindowCall untouched = WinGlassNativeShim.fireWindowIntoRecordingTable(2, id, true, false);
        assertEquals(written.arguments(), untouched.arguments());
        assertEquals(0L, untouched.returned());
        assertUntouched(untouched.bounds());
        RecordedWindowCall rejected = WinGlassNativeShim.fireWindowIntoRecordingTable(2, id, false, true);
        assertEquals(INVALID_ARG, rejected.returned(), "NULL out_bounds is rejected before the target");
        assertEquals(List.of(), rejected.arguments());

        assertRecorded(3, id, 0L, List.of(id, 1001, 1002));
        assertRecorded(4, id, 0L, List.of(id, 1001, 1002, 1003));
        assertRecorded(5, id, 0L, List.of(id, 1.5f, 2.5f, 3.5f, 4.5f));
        assertRecorded(6, id, 0L, List.of(id, 1001));
        assertRecorded(7, id, 0L, List.of(id));
        assertRecorded(8, id, 0L, List.of(id));
        assertRecorded(9, id, 0L, List.of(id, HOOK_POINTER));
        assertRecorded(10, id, WinGlassNativeShim.RECORDING_HIT_TEST, List.of(id, 1001, 1002));
        assertRecorded(11, id, 0L, List.of(id));
    }

    /** Fires {@code slot} into the recording table with a NULL {@code out_bounds}, which every slot but 2 ignores. */
    private static void assertRecorded(int slot, long id, long expectedReturn, List<Object> expectedArguments) {
        RecordedWindowCall call = WinGlassNativeShim.fireWindowIntoRecordingTable(slot, id, false, false);
        assertEquals(expectedArguments, call.arguments(), "slot " + slot);
        assertEquals(expectedReturn, call.returned(), "slot " + slot + " return");
        assertNull(call.bounds());
    }

    /**
     * An unknown slot answers {@code GWIN_ERR_INVALID_ARG} without touching the buffer, whichever
     * table is installed - and so does every slot while no table is installed at all. The production
     * table is back afterwards, with the same twelve stubs.
     */
    @Test
    public void anUnknownSlotOrNoTableIsRejected() {
        List<Long> stubs = WinGlassNativeShim.installedWindowCallbackStubAddresses();
        for (int slot : new int[] {-1, 12, 99, 1000}) {
            RecordedWindowCall call = WinGlassNativeShim.fireWindowIntoRecordingTable(slot, 1L, true, true);
            assertEquals(INVALID_ARG, call.returned(), "slot " + slot);
            assertEquals(List.of(), call.arguments(), "slot " + slot);
            assertUntouched(call.bounds());
            WindowFire fired = WinGlassNativeShim.fireWindowCallback(slot, 1L, true);
            assertEquals(INVALID_ARG, fired.returned(), "slot " + slot);
            assertUntouched(fired.bounds());
        }
        for (int slot = 0; slot < 12; slot++) {
            assertEquals(INVALID_ARG, WinGlassNativeShim.fireWindowCallbackWithNoTable(slot, 1L),
                    "no table, slot " + slot);
        }
        assertEquals(stubs, WinGlassNativeShim.installedWindowCallbackStubAddresses(), "the production table is back");
    }

    // ---------------------------------------------------------------------------------------------
    // The through-C oracle, part two: the production table, a registered WinWindow, the same pattern
    // ---------------------------------------------------------------------------------------------

    /**
     * The seven event slots through the production table: the registry lookup, the virtual dispatch
     * and {@code Window}'s bookkeeping, observed on the handler and the getters against the header's
     * pattern, with the peer asserted by identity. {@code notify_resize}'s 1001 is not a
     * {@code WindowEvent} type, so {@code Window} files the state as NORMAL, stores 1002 x 1003 and
     * delivers 1001 itself - which is what proves the three ints landed in order; the pattern's
     * 1001 for {@code notify_focus} is "not FOCUS_LOST", so the window gains focus once.
     */
    @Test
    public void theEventSlotsReachTheRegisteredWindowThroughTheHook() {
        withWindow(Window.TITLED, handler -> window -> {
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(0, window.id(), false).returned());
            assertEquals(List.of(event(WindowEvent.CLOSE)), handler.calls);
            assertSame(window.window(), handler.lastWindow);
            assertFalse(WinGlassNativeShim.windowState(window.window()).closed());

            handler.calls.clear();
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(3, window.id(), false).returned());
            assertEquals(List.of(event(WindowEvent.MOVE)), handler.calls);
            WindowState moved = WinGlassNativeShim.windowState(window.window());
            assertEquals(1001, moved.x());
            assertEquals(1002, moved.y());

            handler.calls.clear();
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(4, window.id(), false).returned());
            assertEquals(List.of(event(1001)), handler.calls, "1001 is delivered as the type it is");
            WindowState resized = WinGlassNativeShim.windowState(window.window());
            assertEquals(1002, resized.width());
            assertEquals(1003, resized.height());

            handler.calls.clear();
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(5, window.id(), false).returned());
            WindowState scaled = WinGlassNativeShim.windowState(window.window());
            if (PrismSettings.allowHiDPIScaling) {
                assertEquals(List.of(event(WindowEvent.RESCALE)), handler.calls);
                assertEquals(1.5f, scaled.platformScaleX());
                assertEquals(2.5f, scaled.platformScaleY());
                assertEquals(3.5f, scaled.outputScaleX());
                assertEquals(4.5f, scaled.outputScaleY());
            } else {
                assertEquals(List.of(), handler.calls);
                assertEquals(1f, scaled.platformScaleX());
            }

            handler.calls.clear();
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(6, window.id(), false).returned());
            assertEquals(List.of(event(1001)), handler.calls, "1001 is not FOCUS_LOST: the window gains focus");
            assertTrue(WinGlassNativeShim.windowState(window.window()).focused());
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(6, window.id(), false).returned());
            assertEquals(List.of(event(1001)), handler.calls, "already focused: no second event");

            handler.calls.clear();
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(7, window.id(), false).returned());
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(8, window.id(), false).returned());
            assertEquals(List.of(event(WindowEvent.FOCUS_DISABLED), event(WindowEvent.FOCUS_UNGRAB)), handler.calls);
            assertSame(window.window(), handler.lastWindow);
        });
    }

    /**
     * {@code notify_moving} through the production table: the thirteen scalars reach the headless
     * window's {@code notifyMoving} as the header sends them; a null answer leaves the sentinel and
     * answers 0, a four-int answer is written and answers 1, a three-int answer - the JNI's "bad
     * array length" arm - leaves the sentinel and answers 0, a throw is reported and does the same,
     * and a NULL {@code out_bounds} is refused by the C before the target runs.
     */
    @Test
    public void notifyMovingThroughTheHookWritesTheOverrideOrNothing() {
        withWindow(Window.TITLED, handler -> window -> {
            WindowFire none = WinGlassNativeShim.fireWindowCallback(2, window.id(), true);
            assertEquals(0L, none.returned());
            assertUntouched(none.bounds());
            assertEquals(List.of("moving[1001, 1002, 1003, 1004, 1.5, 2.5, 1005, 1006, 1, 1007, 1008, 1009, 1010]"),
                    WinGlassNativeShim.movingCalls(window.window()));

            WinGlassNativeShim.setMovingAnswer(window.window(), new int[] {-5, 6, 700, 800});
            WindowFire four = WinGlassNativeShim.fireWindowCallback(2, window.id(), true);
            assertEquals(1L, four.returned());
            assertArrayEquals(new int[] {-5, 6, 700, 800}, four.bounds());

            WinGlassNativeShim.setMovingAnswer(window.window(), new int[] {1, 2, 3});
            AtomicReference<WindowFire> three = new AtomicReference<>();
            assertNull(reportedBy(() -> three.set(WinGlassNativeShim.fireWindowCallback(2, window.id(), true))));
            assertEquals(0L, three.get().returned());
            assertUntouched(three.get().bounds());

            RuntimeException failure = new IllegalArgumentException("WinWindowNativeTest: moving through the hook");
            WinGlassNativeShim.setMovingFailure(window.window(), failure);
            AtomicReference<WindowFire> thrown = new AtomicReference<>();
            assertSame(failure,
                    reportedBy(() -> thrown.set(WinGlassNativeShim.fireWindowCallback(2, window.id(), true))));
            assertEquals(0L, thrown.get().returned());
            assertUntouched(thrown.get().bounds());
            assertEquals(4, WinGlassNativeShim.movingCalls(window.window()).size());

            assertEquals(INVALID_ARG, WinGlassNativeShim.fireWindowCallback(2, window.id(), false).returned());
            assertEquals(4, WinGlassNativeShim.movingCalls(window.window()).size(),
                    "NULL out_bounds never reached Java");
            assertEquals(List.of(), handler.calls);
        });
    }

    /**
     * {@code notify_delegate_ptr} through the production table stores all sixty-four bits of the
     * header's pointer, which {@code getNativeHandle()} then answers in place of the raw handle;
     * {@code non_client_hit_test} answers HTCLIENT for a window that is not EXTENDED and, for an
     * EXTENDED one with a headless view attached, whatever the view's handler picks at (1001, 1002)
     * - the two ints, seen through the pick.
     */
    @Test
    public void notifyDelegatePtrAndNonClientHitTestThroughTheHook() {
        withWindow(Window.TITLED, handler -> window -> {
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(9, window.id(), false).returned());
            WindowState attached = WinGlassNativeShim.windowState(window.window());
            assertEquals(HOOK_POINTER, attached.nativeHandle());
            assertEquals(WinGlassNativeShim.HEADLESS_HWND, attached.rawHandle());
            assertEquals(List.of(), handler.calls, "no event");
            assertEquals(HTCLIENT, WinGlassNativeShim.fireWindowCallback(10, window.id(), false).returned());
        });
        withWindow(Window.EXTENDED, handler -> window -> {
            HeaderPicker picker = new HeaderPicker();
            HeadlessView view = WinGlassNativeShim.createHeadlessView(picker);
            try {
                WinGlassNativeShim.attachView(window.window(), view.view());
                picker.area = HeaderAreaType.DRAGBAR;
                assertEquals(HTCAPTION, WinGlassNativeShim.fireWindowCallback(10, window.id(), false).returned());
                assertEquals(List.of("pick[1001.0, 1002.0]"), picker.picks);
                picker.area = HeaderAreaType.CLOSE;
                assertEquals(HTCLOSE, WinGlassNativeShim.fireWindowCallback(10, window.id(), false).returned());
            } finally {
                WinGlassNativeShim.attachView(window.window(), null);
                WinGlassNativeShim.unregisterView(view.id());
            }
        });
    }

    /**
     * An id the registry does not hold answers the slot default through the hook - 0, the sentinel
     * intact for slot 2 - and is not reported; the registered window is untouched and the registry
     * unchanged, slots 1 and 11 included.
     */
    @Test
    public void anUnknownIdIsSilentOnEverySlotThroughTheHook() {
        withWindow(Window.TITLED, handler -> window -> {
            for (long id : new long[] {UNKNOWN_ID, 0L}) {
                for (int slot = 0; slot < 12; slot++) {
                    int fired = slot;
                    AtomicReference<WindowFire> result = new AtomicReference<>();
                    Throwable reported = reportedBy(
                            () -> result.set(WinGlassNativeShim.fireWindowCallback(fired, id, true)));
                    assertNull(reported, "id " + id + " slot " + slot + " reported " + reported);
                    assertEquals(0L, result.get().returned(), "id " + id + " slot " + slot);
                    assertUntouched(result.get().bounds());
                }
            }
            assertEquals(List.of(), handler.calls, "the registered window must not have been touched");
            assertEquals(List.of(), WinGlassNativeShim.movingCalls(window.window()));
            assertEquals(1, WinGlassNativeShim.windowRegistrySize());
        });
    }

    /**
     * A throwing target through the hook is reported and answers the slot default: nothing escapes
     * the stub, and the C sees only the default. The handler throws after recording, so each slot is
     * seen to reach it exactly once; slot 10 throws from the view's picker and answers HTNOWHERE,
     * not HTCLIENT. DESTROY (slot 1) last: {@code Window.notifyDestroy} delivers the event before it
     * zeroes {@code ptr}, so a throwing handler leaves the window open - as it did under the JNI, where
     * {@code CheckAndClearException} ran after the {@code CallVoidMethod} that had already thrown.
     */
    @Test
    public void aThrowingTargetAnswersTheSlotDefaultThroughTheHook() {
        withWindow(Window.EXTENDED, handler -> window -> {
            handler.failure = new IllegalStateException("WinWindowNativeTest: deliberate, through the hook");
            for (int slot : new int[] {0, 3, 4, 6, 7, 8}) {
                handler.calls.clear();
                AtomicReference<WindowFire> result = new AtomicReference<>();
                Throwable reported = reportedBy(
                        () -> result.set(WinGlassNativeShim.fireWindowCallback(slot, window.id(), false)));
                assertSame(handler.failure, reported, "slot " + slot);
                assertEquals(1, handler.calls.size(), "slot " + slot + " reached the handler once: " + handler.calls);
                assertEquals(0L, result.get().returned(), "slot " + slot);
            }
            if (PrismSettings.allowHiDPIScaling) {
                handler.calls.clear();
                assertSame(handler.failure,
                        reportedBy(() -> WinGlassNativeShim.fireWindowCallback(5, window.id(), false)));
                assertEquals(1, handler.calls.size());
            }
            HeaderPicker picker = new HeaderPicker();
            picker.failure = new UnsupportedOperationException("WinWindowNativeTest: pick through the hook");
            HeadlessView view = WinGlassNativeShim.createHeadlessView(picker);
            try {
                WinGlassNativeShim.attachView(window.window(), view.view());
                AtomicReference<WindowFire> hit = new AtomicReference<>();
                assertSame(picker.failure,
                        reportedBy(() -> hit.set(WinGlassNativeShim.fireWindowCallback(10, window.id(), false))));
                assertEquals(HTNOWHERE, hit.get().returned(), "HTNOWHERE, as CallIntMethod's 0 became; not HTCLIENT");
            } finally {
                WinGlassNativeShim.attachView(window.window(), null);
                WinGlassNativeShim.unregisterView(view.id());
            }
            handler.calls.clear();
            assertSame(handler.failure, reportedBy(() -> WinGlassNativeShim.fireWindowCallback(1, window.id(), false)));
            assertEquals(List.of(event(WindowEvent.DESTROY)), handler.calls);
            assertFalse(WinGlassNativeShim.windowState(window.window()).closed(),
                    "the handler threw before Window.notifyDestroy reached ptr = 0");
        });
    }

    /**
     * Slots 1 and 11 through the production table mutate the registry's world, so they fire last, at
     * throwaway windows, and the effect is asserted rather than avoided: {@code notify_dispose} is
     * the registry removal and nothing else - the count drops, no event is sent, the window is not
     * closed by it, and every later slot for that id is silent; {@code notify_destroy} is the
     * DESTROY event and {@code ptr = 0}, after which the window reads as closed, a second destroy is
     * silent, and the registry still holds it until dispose - the destruction order, end to end.
     */
    @Test
    public void notifyDestroyAndNotifyDisposeThroughTheHookMutateTheRegistry() {
        RecordingWindowHandler disposedHandler = new RecordingWindowHandler();
        HeadlessWindow disposed = WinGlassNativeShim.createHeadlessWindow(disposedHandler, screen(), Window.TITLED);
        try {
            assertEquals(1, WinGlassNativeShim.windowRegistrySize());
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(11, disposed.id(), false).returned());
            assertEquals(0, WinGlassNativeShim.windowRegistrySize(), "notify_dispose is the removal");
            assertEquals(List.of(), disposedHandler.calls, "and nothing else");
            assertFalse(WinGlassNativeShim.windowState(disposed.window()).closed(), "dispose does not touch ptr");
            assertNull(reportedBy(() -> assertEquals(0L,
                    WinGlassNativeShim.fireWindowCallback(0, disposed.id(), false).returned())));
            assertEquals(List.of(), disposedHandler.calls, "a disposed id is silent");
        } finally {
            WinGlassNativeShim.unregisterWindow(disposed.id());
        }

        RecordingWindowHandler destroyedHandler = new RecordingWindowHandler();
        HeadlessWindow destroyed = WinGlassNativeShim.createHeadlessWindow(destroyedHandler, screen(), Window.TITLED);
        try {
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(1, destroyed.id(), false).returned());
            assertEquals(List.of(event(WindowEvent.DESTROY)), destroyedHandler.calls);
            assertSame(destroyed.window(), destroyedHandler.lastWindow);
            WindowState state = WinGlassNativeShim.windowState(destroyed.window());
            assertTrue(state.closed());
            assertEquals(0L, state.rawHandle());
            assertEquals(1, WinGlassNativeShim.windowRegistrySize(), "notify_destroy does not touch the registry");

            destroyedHandler.calls.clear();
            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(1, destroyed.id(), false).returned());
            assertEquals(List.of(), destroyedHandler.calls, "Window.notifyDestroy returns early once ptr is 0");

            assertEquals(0L, WinGlassNativeShim.fireWindowCallback(11, destroyed.id(), false).returned());
            assertEquals(0, WinGlassNativeShim.windowRegistrySize(), "the destruction order ends in dispose");
        } finally {
            WinGlassNativeShim.unregisterWindow(destroyed.id());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The exports that run without a toolkit
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code gwin_window_get_insets} tests {@code IsWindow} before it looks the GlassWindow up, so
     * the fake handle and NULL both answer 0 - which is also why the headless window's
     * {@code notifyResize} above could run. The twenty-five marshalled exports go through
     * {@code ExecAction}, which without a toolkit returns before the action: every one links, is
     * called once with the fake handle, and comes back; their returns are indeterminate and not
     * asserted.
     */
    @Test
    public void getInsetsRejectsANonWindowAndTheMarshalledExportsNoOpWithoutAToolkit() {
        assertEquals(0L, WinGlassNativeShim.windowGetInsets(WinGlassNativeShim.HEADLESS_HWND));
        assertEquals(0L, WinGlassNativeShim.windowGetInsets(0L));
        assertEquals(25, WinGlassNativeShim.invokeEveryMarshalledWindowOp(WinGlassNativeShim.HEADLESS_HWND));
        assertEquals(0, WinGlassNativeShim.windowRegistrySize(), "the facade's windowCreate registers nothing");
    }

    // ---------------------------------------------------------------------------------------------
    // The WRAPPER: _getAnchor over IsWindow / GetCapture / GetCursorPos / GetWindowRect
    // ---------------------------------------------------------------------------------------------

    /**
     * Branch one: a handle that is not a window answers 0 - not {@code ANCHOR_NO_CAPTURE}, which
     * would change the resize mode.
     */
    @Test
    public void theAnchorOfANonWindowIsZero() {
        assertEquals(0L, WinGlassNativeShim.windowAnchor(WinGlassNativeShim.HEADLESS_HWND));
        assertEquals(0L, WinGlassNativeShim.windowAnchor(0L));
        assertNotEquals(0L, WinGlassNativeShim.anchorNoCapture());
        assertEquals(1L << 63, WinGlassNativeShim.anchorNoCapture());
    }

    /** Branch two: a real window that this thread has not captured answers {@code ANCHOR_NO_CAPTURE}. */
    @Test
    public void theAnchorOfAWindowWithoutTheCaptureIsNoCapture() {
        requireDesktop();
        long desktop = WinGlassNativeShim.desktopWindow();
        assertNotEquals(0L, desktop);
        assertEquals(WinGlassNativeShim.anchorNoCapture(), WinGlassNativeShim.windowAnchor(desktop));
    }

    /**
     * Branch three: while this thread holds the capture on a window of its own, the anchor is the
     * cursor position minus the window origin, packed as the C packed it - compared against the
     * read-back's own {@code GetCursorPos} / {@code GetWindowRect} taken before and after, and only
     * when those two agree, because the mouse may move under the test. Releasing the capture puts
     * the same window back on branch two. Branch four - a failed {@code GetCursorPos} or
     * {@code GetWindowRect} while holding the capture - cannot be induced from user mode; it is
     * pinned by reading {@code windowAnchor} against {@code GlassWindow.cpp} and by the packing test.
     */
    @Test
    public void theAnchorOfACapturedWindowIsCursorMinusOrigin() {
        requireDesktop();
        long hwnd = WinGlassNativeShim.createHiddenWindow();
        assertNotEquals(0L, hwnd, "CreateWindowExW(STATIC, WS_POPUP)");
        try {
            WinGlassNativeShim.setCapture(hwnd);
            boolean compared = false;
            for (int attempt = 0; attempt < 20 && !compared; attempt++) {
                int[] posBefore = WinGlassNativeShim.cursorPosNow();
                int[] rectBefore = WinGlassNativeShim.windowRectNow(hwnd);
                long anchor = WinGlassNativeShim.windowAnchor(hwnd);
                int[] posAfter = WinGlassNativeShim.cursorPosNow();
                int[] rectAfter = WinGlassNativeShim.windowRectNow(hwnd);
                assertNotNull(posBefore);
                assertNotNull(rectBefore);
                if (Arrays.equals(posBefore, posAfter) && Arrays.equals(rectBefore, rectAfter)) {
                    long expected = WinGlassNativeShim.packAnchor(posBefore[0] - rectBefore[0],
                            posBefore[1] - rectBefore[1]);
                    assertEquals(expected, anchor);
                    assertNotEquals(WinGlassNativeShim.anchorNoCapture(), anchor);
                    assertEquals(posBefore[0] - rectBefore[0], (int) (anchor >> 32));
                    assertEquals(posBefore[1] - rectBefore[1], (int) anchor);
                    compared = true;
                }
            }
            assertTrue(compared, "the mouse never held still long enough for two agreeing readings");
            WinGlassNativeShim.releaseCapture();
            assertEquals(WinGlassNativeShim.anchorNoCapture(), WinGlassNativeShim.windowAnchor(hwnd));
        } finally {
            WinGlassNativeShim.releaseCapture();
            WinGlassNativeShim.destroyWindow(hwnd);
        }
        assertEquals(0L, WinGlassNativeShim.windowAnchor(hwnd), "destroyed: branch one again");
    }

    /**
     * {@code ((jlong) x << 32) | ((jlong) y & 0xffffffffL)}: a negative x is a signed shift, a
     * negative y a large unsigned low word, and {@code WinWindow.setBounds} recovers both with
     * {@code (int) (anchor >> 32)} and {@code (int) anchor}.
     */
    @Test
    public void packAnchorKeepsTheSignOfXAndMasksY() {
        assertEquals((5L << 32) | 0xFFFF_FFFDL, WinGlassNativeShim.packAnchor(5, -3));
        assertEquals(0xFFFF_FFFF_0000_0007L, WinGlassNativeShim.packAnchor(-1, 7));
        assertEquals(0x8000_0000L, WinGlassNativeShim.packAnchor(0, Integer.MIN_VALUE));
        assertEquals(0L, WinGlassNativeShim.packAnchor(0, 0));
        for (int[] pair : new int[][] {{5, -3}, {-1, 7}, {0, Integer.MIN_VALUE}, {Integer.MAX_VALUE, -1}}) {
            long packed = WinGlassNativeShim.packAnchor(pair[0], pair[1]);
            assertEquals(pair[0], (int) (packed >> 32));
            assertEquals(pair[1], (int) packed);
            assertNotEquals(WinGlassNativeShim.anchorNoCapture(), packed);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The cursor mapping: GetNativeCursor's switch, and its three-step LoadCursorW
    // ---------------------------------------------------------------------------------------------

    /**
     * The {@code switch} of {@code GlassCursor.cpp}'s {@code GetNativeCursor} for all 21
     * {@code Cursor.CURSOR_*} values and one unknown type: the ordinal or the resource name, exactly
     * as the C picked it. {@code CURSOR_NONE} has neither; {@code CURSOR_DISAPPEAR}, the unknown
     * type and {@code CURSOR_CUSTOM} (which never reaches the switch from a real cursor) fall to the
     * arrow.
     */
    @Test
    public void everyCursorTypeMapsToTheResourceGetNativeCursorPicked() {
        assertOrdinal(32512, Cursor.CURSOR_DEFAULT, Cursor.CURSOR_DISAPPEAR, Cursor.CURSOR_CUSTOM, 99, -7);
        assertOrdinal(32513, Cursor.CURSOR_TEXT);
        assertOrdinal(32514, Cursor.CURSOR_WAIT);
        assertOrdinal(32515, Cursor.CURSOR_CROSSHAIR);
        assertOrdinal(32642, Cursor.CURSOR_RESIZE_NORTHWEST, Cursor.CURSOR_RESIZE_SOUTHEAST);
        assertOrdinal(32643, Cursor.CURSOR_RESIZE_NORTHEAST, Cursor.CURSOR_RESIZE_SOUTHWEST);
        assertOrdinal(32644, Cursor.CURSOR_RESIZE_LEFT, Cursor.CURSOR_RESIZE_RIGHT, Cursor.CURSOR_RESIZE_LEFTRIGHT);
        assertOrdinal(32645, Cursor.CURSOR_RESIZE_UP, Cursor.CURSOR_RESIZE_DOWN, Cursor.CURSOR_RESIZE_UPDOWN);
        assertOrdinal(32646, Cursor.CURSOR_MOVE);
        assertOrdinal(32649, Cursor.CURSOR_POINTING_HAND);
        assertEquals(0, WinGlassNativeShim.cursorOrdinal(Cursor.CURSOR_NONE));
        assertNull(WinGlassNativeShim.cursorName(Cursor.CURSOR_NONE));
        assertEquals(0, WinGlassNativeShim.cursorOrdinal(Cursor.CURSOR_CLOSED_HAND));
        assertEquals("IDC_CLOSED_HAND", WinGlassNativeShim.cursorName(Cursor.CURSOR_CLOSED_HAND));
        assertEquals(0, WinGlassNativeShim.cursorOrdinal(Cursor.CURSOR_OPEN_HAND));
        assertEquals("IDC_OPEN_HAND", WinGlassNativeShim.cursorName(Cursor.CURSOR_OPEN_HAND));
        for (int ordinal : IDC_ORDINALS) {
            assertEquals(ordinal, WinGlassNativeShim.constant(idcName(ordinal)), idcName(ordinal));
        }
    }

    private static void assertOrdinal(int ordinal, int... types) {
        for (int type : types) {
            assertEquals(ordinal, WinGlassNativeShim.cursorOrdinal(type), "type " + type);
            assertNull(WinGlassNativeShim.cursorName(type), "type " + type);
        }
    }

    private static String idcName(int ordinal) {
        return switch (ordinal) {
            case 32512 -> "IDC_ARROW";
            case 32513 -> "IDC_IBEAM";
            case 32514 -> "IDC_WAIT";
            case 32515 -> "IDC_CROSS";
            case 32642 -> "IDC_SIZENWSE";
            case 32643 -> "IDC_SIZENESW";
            case 32644 -> "IDC_SIZEWE";
            case 32645 -> "IDC_SIZENS";
            case 32646 -> "IDC_SIZEALL";
            case 32649 -> "IDC_HAND";
            default -> throw new AssertionError(ordinal);
        };
    }

    /**
     * The three-step load on this machine, against the read-back's own {@code LoadCursorW}: every
     * one of the ten ordinals loads, to the same shared handle the direct call gives, and the ten
     * handles are distinct - so the test cannot pass on ten copies of the arrow. The two
     * {@code GlassResources.rc} cursors load only through {@code GetModuleHandleW("glass.dll")} and
     * come back distinct from the arrow, which settles that {@code glass.dll}'s own module handle finds
     * them; a name nobody has falls to the arrow, the third step. {@code CURSOR_NONE} loads nothing,
     * before any step.
     */
    @Test
    public void everySystemCursorLoadsAndTheTenOrdinalsAreDistinct() {
        requireDesktop();
        Set<Long> handles = new HashSet<>();
        for (int ordinal : IDC_ORDINALS) {
            long handle = WinGlassNativeShim.loadSystemCursor(ordinal);
            assertNotEquals(0L, handle, idcName(ordinal));
            assertEquals(WinGlassNativeShim.loadCursorDirect(0L, ordinal), handle, idcName(ordinal));
            handles.add(handle);
        }
        assertEquals(IDC_ORDINALS.length, handles.size(), "shared system cursors must be distinct: " + handles);
        long arrow = WinGlassNativeShim.loadSystemCursor(32512);

        assertNotEquals(0L, WinGlassNativeShim.moduleHandle("glass.dll"));
        assertEquals(0L, WinGlassNativeShim.moduleHandle("no-such-module-for-jfx.dll"));
        assertEquals(0L, WinGlassNativeShim.loadCursorDirect(0L, 1), "no system cursor 1: the first step fails");
        long closedHand = WinGlassNativeShim.loadSystemCursorByName("IDC_CLOSED_HAND");
        long openHand = WinGlassNativeShim.loadSystemCursorByName("IDC_OPEN_HAND");
        assertNotEquals(0L, closedHand);
        assertNotEquals(0L, openHand);
        assertNotEquals(arrow, closedHand, "IDC_CLOSED_HAND fell through to the arrow: glass.dll's resources unseen");
        assertNotEquals(arrow, openHand, "IDC_OPEN_HAND fell through to the arrow: glass.dll's resources unseen");
        assertNotEquals(closedHand, openHand);
        assertEquals(arrow, WinGlassNativeShim.loadSystemCursorByName("NoSuchCursorForJfx"), "the third step");

        assertEquals(0L, WinGlassNativeShim.systemCursorHandle(Cursor.CURSOR_NONE));
        assertEquals(arrow, WinGlassNativeShim.systemCursorHandle(Cursor.CURSOR_DEFAULT));
        assertEquals(arrow, WinGlassNativeShim.systemCursorHandle(Cursor.CURSOR_DISAPPEAR));
        assertEquals(WinGlassNativeShim.loadSystemCursor(32513),
                WinGlassNativeShim.systemCursorHandle(Cursor.CURSOR_TEXT));
        assertEquals(closedHand, WinGlassNativeShim.systemCursorHandle(Cursor.CURSOR_CLOSED_HAND));
        assertEquals(openHand, WinGlassNativeShim.systemCursorHandle(Cursor.CURSOR_OPEN_HAND));
    }

    /**
     * {@code JCursorToHCURSOR} over real cursors: null is no cursor, {@code CURSOR_NONE} is no
     * cursor, a system type is what {@code GetNativeCursor} gives, and a custom cursor is the
     * {@code HCURSOR} its constructor built - read back as a cursor of that size with that hotspot
     * and those pixels, then released by the test, because nothing in the product ever does.
     */
    @Test
    public void nativeHandleForResolvesNullNoneSystemAndCustomCursors() {
        requireDesktop();
        assertEquals(0L, WinGlassNativeShim.cursorHandleForNull());
        assertEquals(0L, WinGlassNativeShim.cursorHandleFor(Cursor.CURSOR_NONE));
        assertEquals(WinGlassNativeShim.loadSystemCursor(32646),
                WinGlassNativeShim.cursorHandleFor(Cursor.CURSOR_MOVE));
        assertEquals(WinGlassNativeShim.loadSystemCursorByName("IDC_OPEN_HAND"),
                WinGlassNativeShim.cursorHandleFor(Cursor.CURSOR_OPEN_HAND));

        ByteBuffer pixels = ByteBuffer.allocateDirect(2 * 2 * 4).order(ByteOrder.nativeOrder());
        int[] expected = {0xFF102030, 0xFF405060, 0xFF708090, 0xFFA0B0C0};
        for (int i = 0; i < expected.length; i++) {
            // Absolute and native-order: the DIB is raw memory, and Pixels reads from position 0.
            pixels.putInt(i * 4, expected[i]);
        }
        long custom = WinGlassNativeShim.customCursorHandle(1, 0, 2, 2, pixels);
        assertNotEquals(0L, custom);
        try {
            int[] read = WinGlassNativeShim.readCursor(custom);
            assertNotNull(read, "not a cursor Windows recognises");
            assertArrayEquals(new int[] {1, 0, 2, 2}, Arrays.copyOf(read, 4));
            assertEquals(0, WinGlassNativeShim.iconKind(custom), "fIcon = FALSE: a cursor");
        } finally {
            WinGlassNativeShim.destroyCursor(custom);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The icon: Pixels::CreateIcon with fIcon = TRUE, owned by the window from set_icon on
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code iconCreate} builds an icon ({@code fIcon = TRUE}) of the given size from the given
     * pixels; {@code gwin_window_set_icon} on a handle that is not a Glass window installs nothing
     * and - the one case in which the caller keeps ownership - does not destroy it, so the handle is
     * still an icon afterwards and the test releases it; once released it is nothing. A 0 handle is
     * "no icon" and a no-op.
     */
    @Test
    public void iconCreateBuildsAnIconTheWindowWillOwn() {
        ByteBuffer pixels = ByteBuffer.allocateDirect(3 * 2 * 4).order(ByteOrder.nativeOrder());
        int[] expected = {0xFF112233, 0xFF445566, 0xFF778899, 0xFFAABBCC, 0xFFDDEEFF, 0xFF010203};
        for (int i = 0; i < expected.length; i++) {
            pixels.putInt(i * 4, expected[i]);   // absolute and native-order, as above
        }
        long icon = WinGlassNativeShim.iconCreate(3, 2, pixels);
        assertNotEquals(0L, icon);
        boolean destroyed = false;
        try {
            assertEquals(1, WinGlassNativeShim.iconKind(icon), "fIcon = TRUE: an icon, not a cursor");
            int[] read = WinGlassNativeShim.readCursor(icon);
            assertNotNull(read);
            assertEquals(3, read[2]);
            assertEquals(2, read[3]);
            assertArrayEquals(expected, Arrays.copyOfRange(read, 4, 10), "the DIB holds the pixels as given");

            WinGlassNativeShim.windowSetIcon(WinGlassNativeShim.HEADLESS_HWND, icon);
            assertEquals(1, WinGlassNativeShim.iconKind(icon),
                    "not a Glass window: the icon is neither taken nor destroyed");
            WinGlassNativeShim.windowSetIcon(WinGlassNativeShim.HEADLESS_HWND, 0L);
            WinGlassNativeShim.windowSetIcon(0L, icon);
            assertEquals(1, WinGlassNativeShim.iconKind(icon));

            assertTrue(WinGlassNativeShim.destroyIcon(icon));
            destroyed = true;
            assertEquals(-1, WinGlassNativeShim.iconKind(icon), "destroyed");
        } finally {
            if (!destroyed) {
                WinGlassNativeShim.destroyIcon(icon);
            }
        }
        assertEquals(0L, WinGlassNativeShim.iconCreate(0, 2, pixels));
        assertEquals(0L, WinGlassNativeShim.iconCreate(3, 2, ByteBuffer.allocateDirect(4)), "too few pixels");
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** A registered headless window of {@code styleMask} for the duration of {@code body}, unregistered afterwards. */
    private static void withWindow(int styleMask,
                                   java.util.function.Function<RecordingWindowHandler, Consumer<HeadlessWindow>> body) {
        RecordingWindowHandler handler = new RecordingWindowHandler();
        HeadlessWindow window = WinGlassNativeShim.createHeadlessWindow(handler, screen(), styleMask);
        try {
            body.apply(handler).accept(window);
        } finally {
            WinGlassNativeShim.unregisterWindow(window.id());
        }
    }

    /**
     * A 1920 x 1080 screen at the origin, unscaled, as {@code WinGlassNativeTest} builds them:
     * {@code Window}'s constructor takes one instead of asking {@code Screen.getMainScreen()}, which a
     * unit-test JVM cannot answer.
     */
    private static Screen screen() {
        return new Screen(0L, 32, 0, 0, 1920, 1080, 0, 0, 1920, 1080, 0, 0, 1920, 1080, 96, 96, 1f, 1f, 1f, 1f);
    }

    private static String event(int type) {
        return "window[" + type + "]";
    }

    private static void assertUntouched(MovingResult result) {
        assertUntouched(result.bounds());
    }

    private static void assertUntouched(int[] bounds) {
        int[] untouched = new int[4];
        Arrays.fill(untouched, WinGlassNativeShim.UNTOUCHED);
        assertArrayEquals(untouched, bounds, "out_bounds was written");
    }

    /** Runs {@code body} and returns the first throwable {@code Application.reportException} routed to this thread. */
    private static Throwable reportedBy(Runnable body) {
        Thread current = Thread.currentThread();
        Thread.UncaughtExceptionHandler previous = current.getUncaughtExceptionHandler();
        AtomicReference<Throwable> reported = new AtomicReference<>();
        current.setUncaughtExceptionHandler((thread, throwable) -> reported.compareAndSet(null, throwable));
        try {
            body.run();
        } finally {
            current.setUncaughtExceptionHandler(previous);
        }
        return reported.get();
    }

    /**
     * Records every {@code Window.EventHandler} call as {@code name[arguments]}, the window it was
     * called with, and throws {@link #failure} after recording when one is set.
     */
    private static final class RecordingWindowHandler extends Window.EventHandler {

        final List<String> calls = new ArrayList<>();
        Window lastWindow;
        RuntimeException failure;

        @Override
        public void handleWindowEvent(Window window, long time, int type) {
            lastWindow = window;
            record(event(type));
        }

        @Override
        public void handleLevelEvent(int level) {
            record("level[" + level + "]");
        }

        @Override
        public void handleScreenChangedEvent(Window window, long time, Screen oldScreen, Screen newScreen) {
            record("screen[]");
        }

        private void record(String call) {
            calls.add(call);
            if (failure != null) {
                throw failure;
            }
        }
    }

    /** A {@code View.EventHandler} whose {@code pickHeaderArea} answers {@link #area}, records the pick, or throws. */
    private static final class HeaderPicker extends View.EventHandler {

        final List<String> picks = new ArrayList<>();
        HeaderAreaType area;
        RuntimeException failure;

        @Override
        public HeaderAreaType pickHeaderArea(double x, double y) {
            picks.add("pick[" + x + ", " + y + "]");
            if (failure != null) {
                throw failure;
            }
            return area;
        }
    }
}

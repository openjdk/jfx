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

import com.sun.glass.events.GestureEvent;
import com.sun.glass.events.TouchEvent;
import com.sun.glass.events.ViewEvent;
import com.sun.glass.ui.Accessible;
import com.sun.glass.ui.View;
import com.sun.glass.ui.win.WinGlassNativeShim;
import com.sun.glass.ui.win.WinGlassNativeShim.CandidatePos;
import com.sun.glass.ui.win.WinGlassNativeShim.HeadlessView;
import com.sun.glass.ui.win.WinGlassNativeShim.PixelBits;
import com.sun.glass.ui.win.WinGlassNativeShim.RecordedCall;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javafx.scene.AccessibleAttribute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The view peer, {@code WinView}, on the {@code gwin_view_*} ABI (4, and ABI 5's
 * {@code gwin_view_create}): the seventeen exports, the two callback tables ({@code GwinViewCallbacks}, ten slots, and
 * {@code GwinGestureCallbacks}, five) and the fifteen upcall targets behind them, which replace the
 * 26 {@code Call*Method} sites of {@code GlassView.cpp}, {@code ViewContainer.cpp} and
 * {@code FullScreenWindow.cpp}.
 * <p>
 * <b>Two oracles, because neither alone is enough.</b> {@code gwin_test_fire_callback} drives one
 * slot of the installed table with a pattern fixed in the header, and is the only automated check
 * of the fifteen {@code FunctionDescriptor}s: the {@code sizeof} probes cannot see a parameter list,
 * and neither touch hardware nor an IME exists in a build. Fired into a <em>recording</em> table built
 * from the facade's own descriptor objects ({@link WinGlassNativeShim#fireIntoRecordingTable}), it
 * proves each descriptor argument by argument - including the one that matters most,
 * {@code gesture_performed}'s seven ints then seven floats, where one integer too many would shift
 * every float by a stack slot with no crash and no exception. Fired into the <em>production</em>
 * table with a real {@code WinView} registered under the id
 * ({@link WinGlassNativeShim#createHeadlessView}), it proves the target, the registry lookup and the
 * virtual dispatch: what arrives at the view's {@code EventHandler} is asserted against the same
 * pattern. Two of the fixed patterns do not reach a handler at all - {@code notify_view}'s 1001 is
 * not a {@code ViewEvent} and {@code notify_next_touch_event}'s 1001 is not a {@code TouchEvent}, so
 * {@code View} and {@code TouchInputSupport} print "Unknown ... 1001" and return - and for those the
 * production path is observed through that line, and the descriptor through the recording table.
 * <p>
 * <b>The headless view is never closed.</b> {@code gwin_view_close}, {@code get_x}, {@code get_y} and
 * {@code enter_fullscreen} marshal through {@code ExecAction}, which without a toolkit returns
 * without running the action and leaves the return value uninitialised. The close-then-remove
 * ordering of {@code WinView._close}, the A/B of the event stream and the allocation rate on the hot
 * slots therefore belong to {@code tests/system}, not here.
 * <p>
 * Line numbers into {@code Utils.cpp} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/Utils.cpp}).
 */
@EnabledOnOs(OS.WINDOWS)
public class WinViewNativeTest {

    /** The seventeen exports of the header's view section, in the header's order. */
    static final List<String> VIEW_EXPORTS = List.of("gwin_sizeof_view_callbacks",
            "gwin_sizeof_gesture_callbacks", "gwin_view_set_callbacks", "gwin_gesture_set_callbacks",
            "gwin_test_fire_callback", "gwin_view_create", "gwin_view_close", "gwin_view_get_native_view",
            "gwin_view_set_parent", "gwin_view_get_x", "gwin_view_get_y", "gwin_view_schedule_repaint",
            "gwin_view_upload_pixels", "gwin_view_enter_fullscreen", "gwin_view_exit_fullscreen",
            "gwin_view_enable_ime", "gwin_view_finish_ime_composition");

    /** An id the registry never hands out: it counts up from 1 and no test registers this many. */
    static final long UNKNOWN_ID = 0x7FFF_FFF0L;

    /** {@code GwinStatus.GWIN_ERR_INVALID_ARG}, what the hook answers for an unknown slot. */
    static final long INVALID_ARG = -1L;

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        // The three lazy holders, in the order WinGlassNativeTest's exact BOUND_SYMBOLS expects them.
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        WinGlassNativeShim.bindBrowserSymbols();
    }

    // ---------------------------------------------------------------------------------------------
    // Symbols, sizes, the install
    // ---------------------------------------------------------------------------------------------

    @Test
    public void theSeventeenViewExportsAndGetDoubleClickTimeResolve() {
        for (String export : VIEW_EXPORTS) {
            assertTrue(WinGlassNativeShim.resolves("glass!" + export), export);
        }
        assertTrue(WinGlassNativeShim.resolves("user32!GetDoubleClickTime"));
        assertEquals(17, VIEW_EXPORTS.size());
    }

    /**
     * The one {@code gwin_view_*} export that neither marshals through {@code ExecAction} nor needs a
     * host window, invoked on the headless {@code GlassView} the constructor really created: its
     * host HWND is NULL by construction, so the answer is 0. Without it the eleven view downcalls would be
     * resolved but never invoked here.
     */
    @Test
    public void viewGetNativeViewOfAHeadlessViewIsZero() {
        withView(handler -> view -> {
            long glassView = WinGlassNativeShim.viewNativeHandle(view.view());
            assertNotEquals(0L, glassView, "the constructor's gwin_view_create allocated a GlassView");
            assertEquals(0L, WinGlassNativeShim.viewGetNativeView(glassView));
        });
    }

    /**
     * Both tables against the {@code sizeof} the C compiler computed, and every slot at
     * {@code 8 * index} read through {@code PathElement}. A slot appended in C without the layout
     * following it would have the installer write ten pointers into an eleven-pointer table and
     * leave the last holding whatever the arena's allocation left there.
     */
    @Test
    public void bothCallbackTablesHaveTheSizeTheCCompilerGaveThem() {
        assertEquals(80, WinGlassNativeShim.sizeOfViewCallbacks());
        assertEquals(80, WinGlassNativeShim.layoutByteSize("GwinViewCallbacks"));
        assertEquals(40, WinGlassNativeShim.sizeOfGestureCallbacks());
        assertEquals(40, WinGlassNativeShim.layoutByteSize("GwinGestureCallbacks"));
        List<String> viewSlots = WinGlassNativeShim.viewSlotNames();
        assertEquals(10, viewSlots.size());
        for (int i = 0; i < viewSlots.size(); i++) {
            assertEquals(8L * i, WinGlassNativeShim.offset("GwinViewCallbacks", viewSlots.get(i)), viewSlots.get(i));
        }
        List<String> gestureSlots = WinGlassNativeShim.gestureSlotNames();
        assertEquals(5, gestureSlots.size());
        for (int i = 0; i < gestureSlots.size(); i++) {
            assertEquals(8L * i, WinGlassNativeShim.offset("GwinGestureCallbacks", gestureSlots.get(i)),
                    gestureSlots.get(i));
        }
    }

    /**
     * Installed by {@code WinApplication}'s static initializer, beside the preferences and
     * application tables; touching the peer is enough. All fifteen slots are written - a
     * {@code NULL} slot would be replaced by a no-op in C and its events silently dropped - and
     * installing again, or re-installing, changes nothing.
     */
    @Test
    public void theTablesAreInstalledByWinApplicationsStaticInitializerWithNoNullSlot() {
        WinGlassNativeShim.platformKeys();   // forces the peer, and so WinApplication's initializer
        assertTrue(WinGlassNativeShim.viewCallbacksInstalled());
        List<Long> stubs = WinGlassNativeShim.installedViewCallbackStubAddresses();
        assertEquals(15, stubs.size());
        assertFalse(stubs.contains(0L), "a NULL slot: " + stubs);
        assertEquals(15, stubs.stream().distinct().count(), "two slots sharing a stub: " + stubs);
        WinGlassNativeShim.installViewCallbacks();
        WinGlassNativeShim.reinstallViewCallbacks();
        assertEquals(stubs, WinGlassNativeShim.installedViewCallbackStubAddresses());
    }

    // ---------------------------------------------------------------------------------------------
    // The recording table: every descriptor, argument by argument
    // ---------------------------------------------------------------------------------------------

    /**
     * Each of the fifteen slots, fired into recording stubs built from the facade's own
     * {@code FunctionDescriptor} constants, delivers exactly the pattern
     * {@code gwin_test_fire_callback}'s header comment promises. This is the test that catches a
     * descriptor with one parameter too many or of the wrong width; nothing else can.
     */
    @Test
    public void everySlotDeliversTheHeadersPatternThroughTheFacadesDescriptor() {
        long id = 0x0102_0304_0506_0708L;
        assertRecorded(0, id, 0L, List.of(id, 1001));
        assertRecorded(1, id, 0L, List.of(id, 1001, 1002));
        assertRecorded(2, id, 0L, List.of(id, 1001, 1002, 1003, 1004));
        assertRecorded(3, id, 0L, List.of(id, 1001, 1002, 1003, 1004, 1));
        assertRecorded(4, id, 0L, List.of(id, 1001, 1002, "AB", 2, 1003));
        assertRecorded(5, id, 0L, List.of(id, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1, 0));
        assertRecorded(6, id, 0L, List.of(id, 1001, 1002, 1003, 1004, 1.5, 2.5, 1005, 1006, 1007, 1008, 1009,
                3.5, 4.5));
        assertRecorded(7, id, 0L, List.of(id, "AB", 2, "[0, 2]", 1, "[0, 2]", "[1]", 1, 1001, 1002, 1003));
        assertRecorded(8, id, WinGlassNativeShim.RECORDING_CANDIDATE_STATUS, List.of(id, 1001));
        assertRecorded(9, id, WinGlassNativeShim.RECORDING_ACCESSIBLE, List.of(id));
        assertRecorded(100, id, 0L, List.of(id, 1001, 1, 0, 1002, 1003, 1004, 1005,
                1.5f, 2.5f, 3.5f, 4.5f, 5.5f, 6.5f, 7.5f));
        assertRecorded(101, id, 0L, List.of(id));
        assertRecorded(102, id, 0L, List.of(id, 1001, 1, 1002));
        assertRecorded(103, id, 0L, List.of(id, 1001, 0x1_0000_0002L, 1002, 1003, 1004, 1005));
        assertRecorded(104, id, 0L, List.of(id));
    }

    private static void assertRecorded(int slot, long id, long expectedReturn, List<Object> expectedArguments) {
        RecordedCall call = WinGlassNativeShim.fireIntoRecordingTable(slot, id);
        assertEquals(expectedArguments, call.arguments(), "slot " + slot);
        assertEquals(expectedReturn, call.returned(), "slot " + slot + " return");
    }

    /**
     * The hook returns {@code int64_t}: slot 9's value comes back with its high 32 bits intact,
     * through the recording table and, below, through the production path. Had it been bound as
     * {@code JAVA_INT} - or declared {@code int32_t}, as the design first said - the one load-bearing
     * return of the view section would have been the one the hook could not check.
     */
    @Test
    public void theTestHookReturnsAllSixtyFourBits() {
        long returned = WinGlassNativeShim.fireIntoRecordingTable(9, 1L).returned();
        assertEquals(WinGlassNativeShim.RECORDING_ACCESSIBLE, returned);
        assertNotEquals(0L, returned >>> 32, "the high word was lost");
    }

    /** An unknown slot answers {@code GWIN_ERR_INVALID_ARG}, whichever table is installed. */
    @Test
    public void anUnknownSlotIsRejected() {
        for (int slot : new int[] {-1, 10, 99, 105, 1000}) {
            assertEquals(INVALID_ARG, WinGlassNativeShim.fireIntoRecordingTable(slot, 1L).returned(), "slot " + slot);
            assertEquals(INVALID_ARG, WinGlassNativeShim.fireCallback(slot, 1L), "slot " + slot);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The production path: a registered WinView receives what the C sent
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code notify_view}. The hook's 1001 is not a {@code ViewEvent}, so {@code View.notifyView}
     * takes its {@code default} arm - prints "Unknown view event type: 1001" and returns - which is
     * the only trace the production path can leave; an unknown id leaves none. A real type, driven
     * into the same target directly, reaches the handler: ADD makes the view valid and synthesises a
     * MOVE, and only then does {@code notify_repaint} get through, because {@code View.notifyRepaint}
     * is gated on {@code isValid}.
     */
    @Test
    public void notifyViewAndNotifyRepaintReachTheRegisteredView() {
        withView(handler -> view -> {
            String err = captureStderr(() -> assertEquals(0L, WinGlassNativeShim.fireCallback(0, view.id())));
            assertTrue(err.contains("Unknown view event type: 1001"), err);
            assertEquals(List.of(), handler.calls);
            assertFalse(captureStderr(() -> WinGlassNativeShim.fireCallback(0, UNKNOWN_ID)).contains("Unknown"));

            assertEquals(0L, WinGlassNativeShim.fireCallback(2, view.id()));
            assertEquals(List.of(), handler.calls, "REPAINT before ADD is dropped by View.notifyView");

            WinGlassNativeShim.notifyViewTarget(view.id(), ViewEvent.ADD);
            assertEquals(List.of(viewEvent(ViewEvent.ADD), viewEvent(ViewEvent.MOVE)), handler.calls);
            assertSame(view.view(), handler.lastView);
            handler.calls.clear();

            assertEquals(0L, WinGlassNativeShim.fireCallback(2, view.id()));
            assertEquals(List.of(viewEvent(ViewEvent.REPAINT)), handler.calls);
        });
    }

    /**
     * {@code notify_resize} runs {@code WinView}'s override, not {@code View}'s: the size is stored
     * and RESIZE delivered by the superclass, then the override's {@code updateLocation()} adds the
     * MOVE. A target that called {@code View.notifyResize} directly would deliver RESIZE alone.
     */
    @Test
    public void notifyResizeRunsWinViewsOverride() {
        withView(handler -> view -> {
            assertEquals(0L, WinGlassNativeShim.fireCallback(1, view.id()));
            assertEquals(List.of(viewEvent(ViewEvent.RESIZE), viewEvent(ViewEvent.MOVE)), handler.calls);
            assertArrayEquals(new int[] {1001, 1002}, WinGlassNativeShim.viewSize(view.view()));
        });
    }

    /**
     * {@code notify_menu} runs {@code WinView}'s override too. Consumed by the handler, the tuple is
     * all that is observable; not consumed, the override goes on to its EXTENDED-window logic, which
     * dereferences a window this view does not have - a {@code NullPointerException} that
     * {@code View.notifyMenu} could never produce, reported and swallowed by the target.
     */
    @Test
    public void notifyMenuRunsWinViewsOverride() {
        withView(handler -> view -> {
            assertEquals(0L, WinGlassNativeShim.fireCallback(3, view.id()));
            assertEquals(List.of("menu[1001, 1002, 1003, 1004, true]"), handler.calls);

            handler.consumeMenu = false;
            handler.calls.clear();
            Throwable reported = reportedBy(() -> assertEquals(0L, WinGlassNativeShim.fireCallback(3, view.id())));
            assertEquals(List.of("menu[1001, 1002, 1003, 1004, true]"), handler.calls);
            assertNotNull(reported, "WinView.notifyMenu's window dereference is what proves the override ran");
            assertTrue(reported instanceof NullPointerException, reported.toString());
        });
    }

    /** {@code notify_key}: the two borrowed UTF-16 code units arrive as a fresh {@code char[]}. */
    @Test
    public void notifyKeyCarriesTheBorrowedChars() {
        withView(handler -> view -> {
            assertEquals(0L, WinGlassNativeShim.fireCallback(4, view.id()));
            assertEquals(List.of("key[1001, 1002, AB, 1003]"), handler.calls);
        });
    }

    /** {@code notify_mouse}: nine scalars, the two booleans by {@code != 0}. */
    @Test
    public void notifyMouseIsDeliveredScalarForScalar() {
        withView(handler -> view -> {
            assertEquals(0L, WinGlassNativeShim.fireCallback(5, view.id()));
            assertEquals(List.of("mouse[1001, 1002, 1003, 1004, 1005, 1006, 1007, true, false]"), handler.calls);
            assertSame(view.view(), handler.lastView);
        });
    }

    /** {@code notify_scroll}: the four doubles land in the four double parameters, not shifted. */
    @Test
    public void notifyScrollIsDeliveredScalarForScalar() {
        withView(handler -> view -> {
            assertEquals(0L, WinGlassNativeShim.fireCallback(6, view.id()));
            assertEquals(List.of("scroll[1001, 1002, 1003, 1004, 1.5, 2.5, 1005, 1006, 1007, 1008, 1009, 3.5, 4.5]"),
                    handler.calls);
        });
    }

    /**
     * {@code notify_input_method}: the text, the two {@code count + 1} int arrays and the byte array
     * are copied; {@code View.notifyInputMethod} drops {@code visiblePos} (1003) on the way to the
     * handler, as it always did.
     */
    @Test
    public void notifyInputMethodCopiesAllFourBuffers() {
        withView(handler -> view -> {
            assertEquals(0L, WinGlassNativeShim.fireCallback(7, view.id()));
            assertEquals(List.of("ime[AB, [0, 2], [0, 2], [1], 1001, 1002]"), handler.calls);
        });
    }

    /**
     * {@code notify_ime_candidate_pos_request} writes both doubles and answers 1, or touches
     * neither and answers 0. Through the hook only the status and the offset are visible (the C
     * discards its local {@code out}); the target driven directly with a {@code NaN}-filled
     * out-parameter shows the writes: the handler's pair, {@code View}'s {@code {0, 0}} substitute for
     * a {@code null}, and - for a throw and for a one-element array, whose second read fails before
     * the first write - nothing at all.
     */
    @Test
    public void theCandidatePositionIsWrittenBothOrNeither() {
        withView(handler -> view -> {
            assertEquals(1L, WinGlassNativeShim.fireCallback(8, view.id()));
            assertEquals(List.of("candidate[1001]"), handler.calls);

            long id = view.id();
            assertEquals(new CandidatePos(1, 12.5, 34.25), WinGlassNativeShim.imeCandidatePosRequestTarget(id, 5));

            handler.candidatePos = null;
            assertEquals(new CandidatePos(1, 0.0, 0.0), WinGlassNativeShim.imeCandidatePosRequestTarget(id, 5),
                    "View.notifyInputMethodCandidatePosRequest substitutes {0, 0} for null");

            handler.candidatePos = new double[] {99.0};
            AtomicReference<CandidatePos> answer = new AtomicReference<>();
            Throwable reported = reportedBy(() -> answer.set(WinGlassNativeShim.imeCandidatePosRequestTarget(id, 5)));
            assertTrue(reported instanceof ArrayIndexOutOfBoundsException, String.valueOf(reported));
            assertUntouched(answer.get());

            handler.candidatePos = new double[] {1.0, 2.0};
            handler.failure = new IllegalStateException("WinViewNativeTest: deliberate");
            reported = reportedBy(() -> answer.set(WinGlassNativeShim.imeCandidatePosRequestTarget(id, 5)));
            assertSame(handler.failure, reported);
            assertUntouched(answer.get());
        });
    }

    private static void assertUntouched(CandidatePos answer) {
        assertEquals(0, answer.status());
        assertTrue(Double.isNaN(answer.x()) && Double.isNaN(answer.y()), "out_xy was written: " + answer);
    }

    /**
     * {@code get_accessible}: the {@code long} the Java {@code Accessible} owns comes back through
     * {@code View.getAccessible()} - reached from {@code com.sun.glass.ui.win} through the new
     * {@code getAccessibleForNative()} bridge - and through the {@code int64_t} hook, high word
     * intact. The value is what {@code WM_GETOBJECT} answers with, and a wrong 0 would silently
     * disable accessibility. {@code View.accessible} is true on every Windows this test runs on
     * unless {@code glass.accessible.force} says otherwise, which the expectation follows.
     */
    @Test
    public void getAccessibleReturnsSixtyFourBitsThroughTheProductionPath() {
        String force = System.getProperty("glass.accessible.force");
        boolean accessible = force == null || Boolean.parseBoolean(force);
        long nativeAccessible = 0x1_0000_1234L;
        withView(handler -> view -> {
            handler.accessible = new FakeAccessible(nativeAccessible);
            long returned = WinGlassNativeShim.fireCallback(9, view.id());
            if (accessible) {
                assertEquals(nativeAccessible, returned);
                assertEquals(List.of("accessible[]"), handler.calls);
                assertSame(view.view(), handler.accessible.getView(), "Accessible.setView(this) ran");
            } else {
                assertEquals(0L, returned);
            }
            handler.accessible = null;
            assertEquals(0L, WinGlassNativeShim.fireCallback(9, view.id()), "no scene accessible: 0");
        });
    }

    /**
     * An id the registry does not hold - the animated-fullscreen path can produce one after a close,
     * where the JNI dereferenced a deleted global ref - answers the slot default and is <em>not</em>
     * reported: no handler runs, nothing reaches {@code Application.reportException}, the query
     * slots answer 0.
     */
    @Test
    public void anUnknownIdIsSilentOnEveryViewSlot() {
        withView(handler -> view -> {
            for (int slot = 0; slot <= 9; slot++) {
                int fired = slot;
                Throwable reported = reportedBy(
                        () -> assertEquals(0L, WinGlassNativeShim.fireCallback(fired, UNKNOWN_ID), "slot " + fired));
                assertNull(reported, "slot " + slot + " reported " + reported);
            }
            assertEquals(List.of(), handler.calls, "the registered view must not have been touched");
            assertEquals(0L, WinGlassNativeShim.getAccessibleTarget(UNKNOWN_ID));
            assertUntouched(WinGlassNativeShim.imeCandidatePosRequestTarget(UNKNOWN_ID, 0));
        });
    }

    /**
     * <b>A throwing target is reported and answers the slot default; nothing escapes.</b> That is
     * {@code CheckAndClearException} (the former {@code Utils.cpp:50-71}): {@code Application.reportException}
     * on this thread, then carry on. Slots 1 to 8 are fired through the C with a handler that
     * throws; slot 0's pattern never reaches a handler, so its target is driven with ADD; slot 9's
     * throw comes from {@code getSceneAccessible()}. The two query slots answer 0, which for
     * {@code get_accessible} is what {@code CallLongMethod} answered with an exception pending.
     */
    @Test
    public void aThrowingTargetIsReportedAndAnswersTheSlotDefault() {
        withView(handler -> view -> {
            WinGlassNativeShim.notifyViewTarget(view.id(), ViewEvent.ADD);   // so that REPAINT gets through
            handler.calls.clear();
            handler.failure = new IllegalStateException("WinViewNativeTest: deliberate");

            Throwable reported = reportedBy(() -> WinGlassNativeShim.notifyViewTarget(view.id(), ViewEvent.MOVE));
            assertSame(handler.failure, reported, "slot 0");
            for (int slot = 1; slot <= 8; slot++) {
                int fired = slot;
                handler.calls.clear();
                AtomicReference<Long> returned = new AtomicReference<>();
                reported = reportedBy(() -> returned.set(WinGlassNativeShim.fireCallback(fired, view.id())));
                assertSame(handler.failure, reported, "slot " + slot);
                assertEquals(1, handler.calls.size(), "slot " + slot + " reached the handler once: " + handler.calls);
                assertEquals(0L, returned.get(), "slot " + slot);
            }
            handler.calls.clear();
            AtomicReference<Long> returned = new AtomicReference<>();
            reported = reportedBy(() -> returned.set(WinGlassNativeShim.fireCallback(9, view.id())));
            assertSame(handler.failure, reported, "slot 9");
            assertEquals(List.of("accessible[]"), handler.calls);
            assertEquals(0L, returned.get(), "get_accessible answers 0 on a throw, as CallLongMethod did");
        });
    }

    // ---------------------------------------------------------------------------------------------
    // Marshalling arms the fixed pattern cannot reach
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code notify_key} with {@code key_char_count == 0} yields an empty array, never {@code null}:
     * the JNI passed an allocated empty {@code jcharArray}. A {@code NULL} pointer is never read,
     * whatever the count says.
     */
    @Test
    public void notifyKeyWithNoCharsYieldsAnEmptyArray() {
        withView(handler -> view -> {
            WinGlassNativeShim.notifyKeyTargetWithNullChars(view.id(), 7, 8, 0, 9);
            assertEquals(List.of("key[7, 8, , 9]"), handler.calls);
            assertNotNull(handler.lastKeyChars);
            assertEquals(0, handler.lastKeyChars.length);
            handler.calls.clear();
            WinGlassNativeShim.notifyKeyTargetWithNullChars(view.id(), 7, 8, 3, 9);
            assertEquals(List.of("key[7, 8, , 9]"), handler.calls, "NULL with a count is not a read of address 0");
        });
    }

    /**
     * {@code notify_input_method} takes null-ness from the pointer, never from the count: a
     * {@code NULL} buffer with a non-zero count is {@code null}, not a read of address 0, and a
     * {@code NULL} text with length 0 - the {@code WM_IME_ENDCOMPOSITION} reset - is a {@code null}
     * text.
     */
    @Test
    public void notifyInputMethodTakesNullnessFromThePointer() {
        withView(handler -> view -> {
            WinGlassNativeShim.notifyInputMethodTargetWithNullBuffers(view.id(), 5, 3, 2, 11, 12, 13);
            assertEquals(List.of("ime[null, null, null, null, 11, 12]"), handler.calls);
            handler.calls.clear();
            WinGlassNativeShim.notifyInputMethodTargetWithNullBuffers(view.id(), 0, 0, 0, 0, 0, 0);
            assertEquals(List.of("ime[null, null, null, null, 0, 0]"), handler.calls);
        });
    }

    // ---------------------------------------------------------------------------------------------
    // The gesture table
    // ---------------------------------------------------------------------------------------------

    /**
     * The five gesture slots reach {@code WinGestureSupport} with the registered view, in one
     * scripted sequence because {@code GestureSupport}'s state is static and its scroll totals are
     * never reset: the hook's {@code gesture_performed} pattern (totals 3.5 / 4.5) is
     * handler-observable exactly once per JVM, so this method is the one place it is fired with a
     * view. {@code inertia_gesture_finished} sends nothing to a handler by design
     * ({@code handleScrollingEnd} returns before notifying when {@code isInertia}); what it does is
     * set the scroll state idle, and that shows as the <em>absence</em> of a
     * {@code GESTURE_FINISHED} at the next end-touch - the control arm, with a fresh start and no
     * inertia end, shows the {@code GESTURE_FINISHED} that would otherwise have been there.
     */
    @Test
    public void theGestureSlotsReachWinGestureSupportWithTheRegisteredView() {
        withView(handler -> view -> {
            long id = view.id();
            WinGlassNativeShim.notifyEndTouchEventTarget(id);   // drains any scroll state a test left
            handler.calls.clear();

            assertEquals(0L, WinGlassNativeShim.fireCallback(102, id));
            assertEquals(List.of("touchBegin[1001, true, 1002]"), handler.calls);
            assertSame(view.view(), handler.lastView);
            handler.calls.clear();

            String err = captureStderr(() -> assertEquals(0L, WinGlassNativeShim.fireCallback(103, id)));
            assertTrue(err.contains("Unknown touch state: 1001"), err);
            assertEquals(List.of(), handler.calls);

            assertEquals(0L, WinGlassNativeShim.fireCallback(100, id));
            assertEquals(List.of("scrollGesture[" + GestureEvent.GESTURE_STARTED
                    + ", 1001, true, false, 0, 1002, 1003, 1004, 1005, 3.5, 4.5, 3.5, 4.5, 1.0, 1.0]"), handler.calls);
            handler.calls.clear();

            assertEquals(0L, WinGlassNativeShim.fireCallback(101, id));
            assertEquals(List.of(), handler.calls, "an inertia end notifies nothing");
            assertEquals(0L, WinGlassNativeShim.fireCallback(104, id));
            assertEquals(List.of("touchEnd[]"), handler.calls,
                    "after inertia_gesture_finished the scroll is idle, so the end-touch finishes nothing");
            handler.calls.clear();

            // Control: a fresh scroll with no inertia end, and the end-touch does emit GESTURE_FINISHED.
            WinGlassNativeShim.gesturePerformedTarget(id, 1001, true, false, 1002, 1003, 1004, 1005, 0f, 0f,
                    50f, 60f, 1f, 0f, 0f);
            assertEquals(1, handler.calls.size(), handler.calls.toString());
            assertTrue(handler.calls.get(0).startsWith("scrollGesture[" + GestureEvent.GESTURE_STARTED + ", 1001"),
                    handler.calls.toString());
            handler.calls.clear();
            assertEquals(0L, WinGlassNativeShim.fireCallback(104, id));
            assertEquals(2, handler.calls.size(), handler.calls.toString());
            assertEquals("touchEnd[]", handler.calls.get(0));
            assertTrue(handler.calls.get(1).startsWith("scrollGesture[" + GestureEvent.GESTURE_FINISHED + ", 1001"),
                    handler.calls.toString());
        });
    }

    /**
     * Id 0 - a container with no {@code GlassView} attached, which {@code HandleViewTouchEvent} can
     * produce - reaches {@code WinGestureSupport} with a {@code null} view, as the JNI passed one,
     * and the begin / end / inertia slots take it without reporting anything. The next-touch slot
     * prints its unknown-state line either way. {@code gesture_performed} is not fired with 0
     * here: with a view of {@code null} and totals that pass the threshold it dies with a
     * {@code NullPointerException} inside {@code GestureSupport}, which the JNI produced too - see the
     * throwing-target test, which drives it directly so as not to consume the hook's one observable
     * pattern.
     */
    @Test
    public void aNullViewIsPassedThroughToWinGestureSupport() {
        withView(handler -> view -> {
            for (int slot : new int[] {102, 101, 104}) {
                Throwable reported = reportedBy(() -> assertEquals(0L, WinGlassNativeShim.fireCallback(slot, 0L)));
                assertNull(reported, "slot " + slot + " reported " + reported);
            }
            String err = captureStderr(() -> assertEquals(0L, WinGlassNativeShim.fireCallback(103, 0L)));
            assertTrue(err.contains("Unknown touch state: 1001"), err);
            assertEquals(List.of(), handler.calls, "the registered view must not have been touched");
        });
    }

    /**
     * The gesture targets' catch arms, through the targets directly, with a handler that throws:
     * begin, next (a real PRESSED, balanced by a RELEASED), end and performed (totals of its own).
     * Plus the parity case: {@code gesture_performed} with no view and totals past the threshold is
     * a {@code NullPointerException} inside {@code GestureSupport.handleTotalScrolling} - reported,
     * not fatal, exactly as {@code CheckAndClearException} handled the same {@code null View} under
     * JNI. {@code inertia_gesture_finished} cannot be made to throw from outside: it notifies nothing
     * and dereferences nothing, so its catch is exercised by no test.
     */
    @Test
    public void aThrowingGestureTargetIsReported() {
        withView(handler -> view -> {
            long id = view.id();
            handler.failure = new IllegalStateException("WinViewNativeTest: deliberate");
            assertSame(handler.failure,
                    reportedBy(() -> WinGlassNativeShim.notifyBeginTouchEventTarget(id, 1, true, 1)));
            assertSame(handler.failure, reportedBy(() -> WinGlassNativeShim.notifyNextTouchEventTarget(id,
                    TouchEvent.TOUCH_PRESSED, 7L, 1, 2, 3, 4)));
            assertSame(handler.failure, reportedBy(() -> WinGlassNativeShim.notifyNextTouchEventTarget(id,
                    TouchEvent.TOUCH_RELEASED, 7L, 1, 2, 3, 4)));
            assertSame(handler.failure, reportedBy(() -> WinGlassNativeShim.notifyEndTouchEventTarget(id)));
            assertSame(handler.failure, reportedBy(() -> WinGlassNativeShim.gesturePerformedTarget(id, 1, true, false,
                    0, 0, 0, 0, 0f, 0f, 100f, 200f, 1f, 0f, 0f)));
            Throwable nullView = reportedBy(() -> WinGlassNativeShim.gesturePerformedTarget(0L, 1, true, false,
                    0, 0, 0, 0, 0f, 0f, 300f, 400f, 1f, 0f, 0f));
            assertTrue(nullView instanceof NullPointerException, String.valueOf(nullView));
            assertEquals(List.of("touchBegin[1, true, 1]", "touchNext[" + TouchEvent.TOUCH_PRESSED + ", 7, 1, 2, 3, 4]",
                    "touchNext[" + TouchEvent.TOUCH_RELEASED + ", 7, 1, 2, 3, 4]", "touchEnd[]",
                    "scrollGesture[" + GestureEvent.GESTURE_STARTED + ", 1, true, false, 0, 0, 0, 0, 0, 100.0, 200.0,"
                    + " 100.0, 200.0, 1.0, 1.0]"), handler.calls);

            handler.failure = null;
            handler.calls.clear();
            assertNull(reportedBy(() -> WinGlassNativeShim.inertiaGestureFinishedTarget(id)));
            assertNull(reportedBy(() -> WinGlassNativeShim.notifyEndTouchEventTarget(id)));   // leaves the scroll idle
            assertEquals("touchEnd[]", handler.calls.get(0));
        });
    }

    // ---------------------------------------------------------------------------------------------
    // The registry, the natives, the A/B, the pixels
    // ---------------------------------------------------------------------------------------------

    /**
     * Empty at rest, and a seeded entry round-trips: ids count up from 1 and are never reused, so a
     * second view gets a higher id and the first id stays dead after removal.
     */
    @Test
    public void theViewRegistryIsEmptyAtRestAndRoundTrips() {
        assertEquals(0, WinGlassNativeShim.viewRegistrySize());
        HeadlessView first = WinGlassNativeShim.createHeadlessView(new RecordingHandler());
        HeadlessView second = WinGlassNativeShim.createHeadlessView(new RecordingHandler());
        try {
            assertTrue(first.id() >= 1L);
            assertTrue(second.id() > first.id());
            assertEquals(first.id(), WinGlassNativeShim.viewIdOf(first.view()));
            assertEquals(2, WinGlassNativeShim.viewRegistrySize());
            WinGlassNativeShim.unregisterView(first.id());
            assertEquals(1, WinGlassNativeShim.viewRegistrySize());
            assertNull(reportedBy(() -> assertEquals(0L, WinGlassNativeShim.fireCallback(5, first.id()))));
        } finally {
            WinGlassNativeShim.unregisterView(first.id());
            WinGlassNativeShim.unregisterView(second.id());
        }
        assertEquals(0, WinGlassNativeShim.viewRegistrySize());
    }

    /**
     * {@code gwin_view_create} called straight through the facade, past the registry.
     * Each call is a fresh {@code new GlassView(view_id)} - non-zero, and distinct from the other - and
     * neither object has a host window by construction, so {@code gwin_view_get_native_view}, the one
     * view export that neither marshals nor needs a toolkit, answers 0 on both. The ids are ones the
     * registry never hands out, so nothing a slot delivered for them could reach a view.
     * <p>
     * The two objects are not closed: without a toolkit {@code gwin_view_close} runs nothing
     * ({@code ExecAction} returns on a NULL instance) and answers an uninitialised value, so they live
     * for the JVM like every headless view of this class. That {@code view_id} is what the slots carry
     * back through {@code GetViewId()} on real mouse, key and resize events belongs to the robot suite.
     */
    @Test
    public void gwinViewCreateAnswersADistinctGlassViewWithNoHostWindowPerCall() {
        long first = WinGlassNativeShim.viewCreate(UNKNOWN_ID);
        long second = WinGlassNativeShim.viewCreate(UNKNOWN_ID + 1L);
        assertNotEquals(0L, first, "gwin_view_create answered NULL: new GlassView threw");
        assertNotEquals(0L, second, "gwin_view_create answered NULL: new GlassView threw");
        assertNotEquals(first, second, "two calls answered the same GlassView");
        assertEquals(0L, WinGlassNativeShim.viewGetNativeView(first));
        assertEquals(0L, WinGlassNativeShim.viewGetNativeView(second));
    }

    /**
     * The product path of the same export: {@code WinView._create(Map)} registers first and then calls
     * {@code gwin_view_create} with the id it registered, and keeps the entry because the answer is
     * non-zero. Two views, two ids, two distinct {@code GlassView}s, neither attached.
     */
    @Test
    public void everyWinViewHoldsItsOwnGlassViewAndStaysRegistered() {
        int before = WinGlassNativeShim.viewRegistrySize();
        HeadlessView first = WinGlassNativeShim.createHeadlessView(new RecordingHandler());
        HeadlessView second = WinGlassNativeShim.createHeadlessView(new RecordingHandler());
        try {
            long firstView = WinGlassNativeShim.viewNativeHandle(first.view());
            long secondView = WinGlassNativeShim.viewNativeHandle(second.view());
            assertNotEquals(0L, firstView);
            assertNotEquals(0L, secondView);
            assertNotEquals(firstView, secondView);
            assertNotEquals(first.id(), second.id());
            assertEquals(before + 2, WinGlassNativeShim.viewRegistrySize(), "a non-zero answer keeps the entry");
            assertEquals(0L, WinGlassNativeShim.viewGetNativeView(firstView));
            assertEquals(0L, WinGlassNativeShim.viewGetNativeView(secondView));
        } finally {
            WinGlassNativeShim.unregisterView(first.id());
            WinGlassNativeShim.unregisterView(second.id());
        }
        assertEquals(before, WinGlassNativeShim.viewRegistrySize());
    }

    /**
     * What is still {@code native} on the two peers, as an exact list: nothing. {@code _create} became
     * {@code gwin_view_create} (ABI 5); {@code _initIDs} (both classes, bodies emptied once nothing
     * reached them), the three multi-click getters (the A/B oracle of the facade's {@code GetDoubleClickTime} /
     * {@code GetSystemMetrics} binds, which agreed on every run while both existed) and the always-empty
     * {@code _begin} / {@code _end} went before it. {@code WinGestureSupport} declares nothing either.
     */
    @Test
    public void winViewAndWinGestureSupportDeclareExactlyTheNativesStillNeedingC() {
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinView"));
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinGestureSupport"));
    }

    /**
     * {@code _uploadPixels} resolves the {@code Pixels} buffer in Java, reproducing what the
     * {@code Pixels.attachData} upcall handed the C: a direct buffer's <em>base</em> address whatever
     * its position ({@code GetDirectBufferAddress} ignored it, and {@code duplicate().clear()} is what
     * keeps {@code ofBuffer} from honouring it), a heap buffer's array from its {@code arrayOffset()}
     * (a slice), and {@code NULL} for a read-only heap buffer, whose {@code array()} threw inside
     * {@code attachData} and left {@code GetBits()} answering {@code NULL}.
     */
    @Test
    public void pixelBitsResolvesTheBufferAsAttachDataDid() {
        int width = 3;
        int height = 2;
        int[] pixels = {0x11, 0x22, 0x33, 0x44, 0x55, 0x66};

        IntBuffer direct = ByteBuffer.allocateDirect(pixels.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        direct.put(pixels).position(2);
        PixelBits directBits = WinGlassNativeShim.pixelBits(direct, width, height);
        assertFalse(directBits.isNull());
        assertEquals(directBits.positionedAddress() - 2L * 4L, directBits.address(),
                "the base address, not the position's: duplicate().clear() is load-bearing");
        assertArrayEquals(pixels, directBits.ints());

        int[] backing = new int[pixels.length + 4];
        System.arraycopy(pixels, 0, backing, 4, pixels.length);
        IntBuffer slice = IntBuffer.wrap(backing, 4, pixels.length).slice();
        assertEquals(4, slice.arrayOffset());
        PixelBits heapBits = WinGlassNativeShim.pixelBits(slice, width, height);
        assertFalse(heapBits.isNull());
        assertArrayEquals(pixels, heapBits.ints(), "copied from array() at arrayOffset()");

        ByteBuffer bytes = ByteBuffer.allocate(pixels.length * 4).order(ByteOrder.nativeOrder());
        bytes.asIntBuffer().put(pixels);
        assertArrayEquals(pixels, WinGlassNativeShim.pixelBits(bytes, width, height).ints());

        assertTrue(WinGlassNativeShim.pixelBits(IntBuffer.wrap(pixels).asReadOnlyBuffer(), width, height).isNull(),
                "a read-only heap buffer has no array(): NULL, as a swallowed ReadOnlyBufferException left it");
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** A registered headless view for the duration of {@code body}, unregistered afterwards, never closed. */
    private static void withView(java.util.function.Function<RecordingHandler, Consumer<HeadlessView>> body) {
        RecordingHandler handler = new RecordingHandler();
        HeadlessView view = WinGlassNativeShim.createHeadlessView(handler);
        try {
            body.apply(handler).accept(view);
        } finally {
            WinGlassNativeShim.unregisterView(view.id());
        }
    }

    private static String viewEvent(int type) {
        return "view[" + type + "]";
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

    /** What {@code body} printed to {@code System.err}. */
    private static String captureStderr(Runnable body) {
        PrintStream previous = System.err;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        System.setErr(capture);
        try {
            body.run();
        } finally {
            System.setErr(previous);
            capture.flush();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    /**
     * Records every {@code View.EventHandler} call as {@code name[arguments]}, records the view it
     * was called with, and throws {@link #failure} after recording when one is set.
     */
    private static final class RecordingHandler extends View.EventHandler {

        final List<String> calls = new ArrayList<>();
        View lastView;
        char[] lastKeyChars;
        boolean consumeMenu = true;
        double[] candidatePos = {12.5, 34.25};
        Accessible accessible;
        RuntimeException failure;

        private void record(String name, Object... arguments) {
            calls.add(name + Arrays.toString(arguments));
            if (failure != null) {
                throw failure;
            }
        }

        @Override
        public void handleViewEvent(View view, long time, int type) {
            lastView = view;
            record("view", type);
        }

        @Override
        public boolean handleKeyEvent(View view, long time, int action, int keyCode, char[] keyChars, int modifiers) {
            lastView = view;
            lastKeyChars = keyChars;
            record("key", action, keyCode, keyChars == null ? null : new String(keyChars), modifiers);
            return false;
        }

        @Override
        public boolean handleMenuEvent(View view, int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger) {
            lastView = view;
            record("menu", x, y, xAbs, yAbs, isKeyboardTrigger);
            return consumeMenu;
        }

        @Override
        public void handleMouseEvent(View view, long time, int type, int button, int x, int y, int xAbs, int yAbs,
                                     int modifiers, boolean isPopupTrigger, boolean isSynthesized) {
            lastView = view;
            record("mouse", type, button, x, y, xAbs, yAbs, modifiers, isPopupTrigger, isSynthesized);
        }

        @Override
        public void handleScrollEvent(View view, long time, int x, int y, int xAbs, int yAbs, double deltaX,
                                      double deltaY, int modifiers, int lines, int chars, int defaultLines,
                                      int defaultChars, double xMultiplier, double yMultiplier) {
            lastView = view;
            record("scroll", x, y, xAbs, yAbs, deltaX, deltaY, modifiers, lines, chars, defaultLines, defaultChars,
                    xMultiplier, yMultiplier);
        }

        @Override
        public void handleInputMethodEvent(long time, String text, int[] clauseBoundary, int[] attrBoundary,
                                           byte[] attrValue, int commitCount, int cursorPos) {
            record("ime", text, Arrays.toString(clauseBoundary), Arrays.toString(attrBoundary),
                    Arrays.toString(attrValue), commitCount, cursorPos);
        }

        @Override
        public double[] getInputMethodCandidatePos(int offset) {
            record("candidate", offset);
            return candidatePos;
        }

        @Override
        public Accessible getSceneAccessible() {
            record("accessible");
            return accessible;
        }

        @Override
        public void handleBeginTouchEvent(View view, long time, int modifiers, boolean isDirect, int touchEventCount) {
            lastView = view;
            record("touchBegin", modifiers, isDirect, touchEventCount);
        }

        @Override
        public void handleNextTouchEvent(View view, long time, int type, long touchId, int x, int y, int xAbs,
                                         int yAbs) {
            lastView = view;
            record("touchNext", type, touchId, x, y, xAbs, yAbs);
        }

        @Override
        public void handleEndTouchEvent(View view, long time) {
            lastView = view;
            record("touchEnd");
        }

        @Override
        public void handleScrollGestureEvent(View view, long time, int type, int modifiers, boolean isDirect,
                                             boolean isInertia, int touchCount, int x, int y, int xAbs, int yAbs,
                                             double dx, double dy, double totaldx, double totaldy,
                                             double multiplierX, double multiplierY) {
            lastView = view;
            record("scrollGesture", type, modifiers, isDirect, isInertia, touchCount, x, y, xAbs, yAbs, dx, dy,
                    totaldx, totaldy, multiplierX, multiplierY);
        }

        @Override
        public void handleZoomGestureEvent(View view, long time, int type, int modifiers, boolean isDirect,
                                           boolean isInertia, int originx, int originy, int originxAbs,
                                           int originyAbs, double scale, double expansion, double totalscale,
                                           double totalexpansion) {
            lastView = view;
            record("zoomGesture", type, modifiers, isDirect, isInertia, originx, originy, originxAbs, originyAbs,
                    scale, expansion, totalscale, totalexpansion);
        }

        @Override
        public void handleRotateGestureEvent(View view, long time, int type, int modifiers, boolean isDirect,
                                             boolean isInertia, int originx, int originy, int originxAbs,
                                             int originyAbs, double dangle, double totalangle) {
            lastView = view;
            record("rotateGesture", type, modifiers, isDirect, isInertia, originx, originy, originxAbs, originyAbs,
                    dangle, totalangle);
        }
    }

    /** An {@code Accessible} whose native half is a number of the test's choosing; no COM behind it. */
    private static final class FakeAccessible extends Accessible {

        private final long nativeAccessible;

        FakeAccessible(long nativeAccessible) {
            this.nativeAccessible = nativeAccessible;
        }

        @Override
        protected long getNativeAccessible() {
            return nativeAccessible;
        }

        @Override
        public void sendNotification(AccessibleAttribute notification) {
        }
    }
}

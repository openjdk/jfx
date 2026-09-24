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

import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.View;
import com.sun.glass.ui.win.WinGlassNativeShim;
import com.sun.glass.ui.win.WinGlassNativeShim.ButtonResult;
import com.sun.glass.ui.win.WinGlassNativeShim.DndFire;
import com.sun.glass.ui.win.WinGlassNativeShim.DragResult;
import com.sun.glass.ui.win.WinGlassNativeShim.HandleResult;
import com.sun.glass.ui.win.WinGlassNativeShim.HeadlessView;
import com.sun.glass.ui.win.WinGlassNativeShim.RecordedDndCall;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The drag-and-drop half of the clipboard ABI: the {@code GwinDndCallbacks} table (nine slots) and its
 * targets - the four {@code View.notifyDrag*} of the drop target, keyed by {@code WinView}'s view id, and
 * the five id-less singleton slots whose peer is {@code WinDnDClipboard.getInstance()} - plus
 * {@code WinDnDClipboard}'s two former natives, which are headless-safe no-ops.
 * <p>
 * <b>Two oracles, as for the view table.</b> {@code gwin_test_fire_dnd_callback} drives one slot with
 * the header's fixed pattern; into a <em>recording</em> table it proves the descriptor argument by
 * argument and the out-parameter both ways; into the <em>production</em> table with a headless
 * {@code WinView} registered under the id it proves the registry lookup and the protected
 * {@code View.notifyDrag*} call. The four drag targets are driven directly too, for the arms the
 * pattern cannot reach - an unknown id, id 0, a throwing handler - and the five singleton targets
 * for the two effects the JNI's {@code getInstance()} call carried: it <em>creates</em> the DnD peer
 * when none exists, and it throws off the application thread.
 * <p>
 * The DnD singleton, once created through {@code Clipboard.get(DND)}, lives in
 * {@code Clipboard.clipboards} until the last {@code ClipboardAssistance} on it closes; the drag
 * sequence (enter, then over, then drop or leave) opens and closes one, so each sequence here is
 * complete and the singleton's static {@code dragButton} is restored to 0.
 */
@EnabledOnOs(OS.WINDOWS)
public class WinDndNativeTest {

    static final long UNKNOWN_ID = 0x7FFF_FFF0L;
    static final long INVALID_ARG = -1L;
    static final int UPCALL = -3;
    static final int OK = 0;
    static final long FAKE_DATA_OBJECT = 0x0000_7FFE_1234_5678L;
    static final int UNTOUCHED_INT = (int) WinGlassNativeShim.UNTOUCHED_OUT;

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        // The three lazy holders, in the order WinGlassNativeTest's exact BOUND_SYMBOLS expects them.
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        WinGlassNativeShim.bindBrowserSymbols();
    }

    // ---------------------------------------------------------------------------------------------
    // The through-C oracle: every drag slot's descriptor, argument by argument, out-parameter and all
    // ---------------------------------------------------------------------------------------------

    /**
     * Each slot fired into the recording table delivers the header's pattern through the facade's
     * descriptor, returns the recording status, and - for the four slots with an out-parameter -
     * leaves in {@code out} what the recording target wrote: an int for the three drag answers and
     * {@code dnd_get_drag_button}, a pointer with bits above 32 for {@code dnd_get_data_object}.
     * The four slots without one accept NULL.
     */
    @Test
    public void everyDndSlotDeliversTheHeadersPatternThroughTheFacadesDescriptor() {
        long id = 0x0102_0304_0506_0708L;
        for (int slot = 0; slot <= 2; slot++) {
            RecordedDndCall call = WinGlassNativeShim.fireDndIntoRecordingTable(slot, id, true);
            assertEquals(List.of(id, 1001, 1002, 1003, 1004, 1005), call.arguments(), "slot " + slot);
            assertEquals(WinGlassNativeShim.RECORDING_STATUS, call.returned(), "slot " + slot);
            assertEquals(WinGlassNativeShim.RECORDING_ACTION, call.outInt(), "slot " + slot + " *out_action");
        }
        RecordedDndCall leave = WinGlassNativeShim.fireDndIntoRecordingTable(3, id, false);
        assertEquals(List.of(id), leave.arguments());
        assertEquals(WinGlassNativeShim.RECORDING_STATUS, leave.returned());

        RecordedDndCall get = WinGlassNativeShim.fireDndIntoRecordingTable(4, id, true);
        assertEquals(List.of("get_data_object"), get.arguments());
        assertEquals(WinGlassNativeShim.RECORDING_STATUS, get.returned());
        assertEquals(WinGlassNativeShim.RECORDING_DATA_OBJECT, get.outLong(), "all 64 bits of *out_data_object");

        RecordedDndCall set = WinGlassNativeShim.fireDndIntoRecordingTable(5, id, false);
        assertEquals(List.of(FAKE_DATA_OBJECT), set.arguments());
        assertEquals(WinGlassNativeShim.RECORDING_STATUS, set.returned());

        RecordedDndCall actions = WinGlassNativeShim.fireDndIntoRecordingTable(6, id, false);
        assertEquals(List.of(1001), actions.arguments());
        RecordedDndCall button = WinGlassNativeShim.fireDndIntoRecordingTable(7, id, false);
        assertEquals(List.of(1001), button.arguments());

        RecordedDndCall getButton = WinGlassNativeShim.fireDndIntoRecordingTable(8, id, true);
        assertEquals(List.of("get_drag_button"), getButton.arguments());
        assertEquals(WinGlassNativeShim.RECORDING_STATUS, getButton.returned());
        assertEquals(WinGlassNativeShim.RECORDING_BUTTON, getButton.outInt());
    }

    /** An unknown slot answers {@code GWIN_ERR_INVALID_ARG}, whichever table is installed. */
    @Test
    public void anUnknownDndSlotIsRejected() {
        for (int slot : new int[] {-1, 9, 100, 1000}) {
            assertEquals(INVALID_ARG, WinGlassNativeShim.fireDndIntoRecordingTable(slot, 1L, true).returned(),
                    "slot " + slot);
            assertEquals(INVALID_ARG, WinGlassNativeShim.fireDndCallback(slot, 1L, true).returned(), "slot " + slot);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The production path: the drop target's four slots reach the registered view
    // ---------------------------------------------------------------------------------------------

    /**
     * A full drag through the hook: {@code drag_enter} reaches {@code View.notifyDragEnter}, which
     * opens the drop-target {@code ClipboardAssistance} (creating the DnD singleton if needed) and
     * hands the handler the fixed pattern; the handler's answer is what lands in {@code *out_action}
     * and the C reads back as {@code DROPEFFECT}; {@code drag_over} the same; {@code drag_drop}
     * closes the assistance. Then a second enter and a {@code drag_leave}, which closes it too.
     */
    @Test
    public void theDragSlotsReachTheRegisteredViewAndWriteItsAnswer() {
        withView(handler -> view -> {
            handler.answer = Clipboard.ACTION_REFERENCE;
            DndFire enter = WinGlassNativeShim.fireDndCallback(0, view.id(), true);
            assertEquals(OK, enter.returned());
            assertEquals(Clipboard.ACTION_REFERENCE, enter.outInt());
            assertEquals(List.of("dragEnter[1001, 1002, 1003, 1004, 1005]"), handler.calls);
            assertNotNull(handler.lastAssistance, "notifyDragEnter opened the drop-target assistance");

            handler.calls.clear();
            handler.answer = Clipboard.ACTION_MOVE;
            DndFire over = WinGlassNativeShim.fireDndCallback(1, view.id(), true);
            assertEquals(OK, over.returned());
            assertEquals(Clipboard.ACTION_MOVE, over.outInt());
            assertEquals(List.of("dragOver[1001, 1002, 1003, 1004, 1005]"), handler.calls);

            handler.calls.clear();
            handler.answer = Clipboard.ACTION_COPY;
            DndFire drop = WinGlassNativeShim.fireDndCallback(2, view.id(), true);
            assertEquals(OK, drop.returned());
            assertEquals(Clipboard.ACTION_COPY, drop.outInt());
            assertEquals(List.of("dragDrop[1001, 1002, 1003, 1004, 1005]"), handler.calls);

            handler.calls.clear();
            assertEquals(OK, WinGlassNativeShim.fireDndCallback(0, view.id(), true).returned());
            DndFire leave = WinGlassNativeShim.fireDndCallback(3, view.id(), false);
            assertEquals(OK, leave.returned());
            assertEquals(List.of("dragEnter[1001, 1002, 1003, 1004, 1005]", "dragLeave[]"), handler.calls);
        });
    }

    /**
     * The drag targets driven directly deliver their scalars and write the view's answer; an id the
     * registry does not know - and 0, what a drop target whose container has no view would carry -
     * writes nothing and answers {@code GWIN_OK}, so the C's own {@code DROPEFFECT_NONE} stands.
     */
    @Test
    public void anUnknownViewIdWritesNothingAndAnswersOk() {
        withView(handler -> view -> {
            handler.answer = Clipboard.ACTION_COPY;
            DragResult enter = asEventThread(() -> WinGlassNativeShim.dragEnterTarget(view.id(), 1, 2, 3, 4, 5));
            assertEquals(OK, enter.status());
            assertEquals(Clipboard.ACTION_COPY, enter.action());
            assertEquals(List.of("dragEnter[1, 2, 3, 4, 5]"), handler.calls);
            asEventThread(() -> WinGlassNativeShim.dragLeaveTarget(view.id()));
        });
        for (long id : new long[] {0L, UNKNOWN_ID}) {
            DragResult enter = WinGlassNativeShim.dragEnterTarget(id, 1, 2, 3, 4, 5);
            assertEquals(OK, enter.status(), "id " + id);
            assertNull(enter.action(), "id " + id + ": *out_action untouched");
            assertNull(WinGlassNativeShim.dragOverTarget(id, 1, 2, 3, 4, 5).action());
            assertNull(WinGlassNativeShim.dragDropTarget(id, 1, 2, 3, 4, 5).action());
            assertEquals(OK, WinGlassNativeShim.dragLeaveTarget(id));
            DndFire fired = WinGlassNativeShim.fireDndCallback(1, id, true);
            assertEquals(OK, fired.returned());
            assertEquals(UNTOUCHED_INT, fired.outInt(), "through the hook: the sentinel survives");
        }
    }

    /**
     * A throwing handler is <em>described</em> (a stack trace on {@code System.err}, the
     * {@code checkJavaException} sink), not reported to the uncaught-exception handler, the slot
     * answers {@code GWIN_ERR_UPCALL}, and {@code *out_action} is left untouched - the C then writes
     * {@code DROPEFFECT_NONE} from its own 0 before it tests the status, as HotSpot's
     * {@code CallIntMethod} returning 0 with a pending exception did.
     */
    @Test
    public void aThrowingDragHandlerIsDescribedAndLeavesTheOutUntouched() {
        withView(handler -> view -> {
            asEventThread(() -> WinGlassNativeShim.dragEnterTarget(view.id(), 1, 2, 3, 4, 5));
            handler.failure = new IllegalStateException("WinDndNativeTest: deliberate");
            AtomicReference<DragResult> result = new AtomicReference<>();
            AtomicReference<String> err = new AtomicReference<>();
            Throwable reported = WinClipboardNativeTest.reportedBy(() -> err.set(WinClipboardNativeTest.captureStderr(
                    () -> result.set(asEventThread(
                            () -> WinGlassNativeShim.dragOverTarget(view.id(), 1, 2, 3, 4, 5))))));
            assertNull(reported, "checkJavaException ran no Java code");
            assertEquals(UPCALL, result.get().status());
            assertNull(result.get().action(), "*out_action untouched");
            assertTrue(err.get().contains("WinDndNativeTest: deliberate"), err.get());
            AtomicReference<DndFire> fired = new AtomicReference<>();
            WinClipboardNativeTest.captureStderr(
                    () -> fired.set(WinGlassNativeShim.fireDndCallback(1, view.id(), true)));
            assertEquals(UPCALL, fired.get().returned());
            assertEquals(UNTOUCHED_INT, fired.get().outInt());
            handler.failure = null;
            asEventThread(() -> WinGlassNativeShim.dragLeaveTarget(view.id()));
        });
    }

    // ---------------------------------------------------------------------------------------------
    // The five singleton slots: getInstance() creates the peer, and throws off the event thread
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code dnd_get_data_object} through {@code getInstance()} creates the DnD peer when none is
     * cached - the effect the JNI relied on for a drag from another application - and answers its
     * handle (NULL); {@code dnd_set_data_object} writes it and get reads it back, through the hook
     * (the fake pointer with bits above 32) and directly; the write counts no live object, because
     * the object being dragged in is not one of the peer's own.
     */
    @Test
    public void theSingletonSlotsCreateTheDndPeerAndRoundTripItsHandle() {
        HandleResult get = asEventThread(WinGlassNativeShim::dndGetDataObjectTarget);
        assertEquals(OK, get.status());
        assertEquals(0L, get.handle());
        long dndId = WinClipboardNativeTest.registeredIdNamed(Clipboard.DND);
        assertTrue(dndId >= 1L, "the DnD peer is registered");

        assertEquals(OK, asEventThread(() -> WinGlassNativeShim.dndSetDataObjectTarget(0x20L)));
        assertEquals(0x20L, asEventThread(WinGlassNativeShim::dndGetDataObjectTarget).handle());
        DndFire set = WinGlassNativeShim.fireDndCallback(5, 0L, false);
        assertEquals(OK, set.returned());
        DndFire got = WinGlassNativeShim.fireDndCallback(4, 0L, true);
        assertEquals(OK, got.returned());
        assertEquals(FAKE_DATA_OBJECT, got.outLong(), "all 64 bits through the hook");
        assertEquals(OK, asEventThread(() -> WinGlassNativeShim.dndSetDataObjectTarget(0L)));
        assertEquals(0L, asEventThread(WinGlassNativeShim::dndGetDataObjectTarget).handle());
    }

    /**
     * {@code dnd_set_drag_button} / {@code dnd_get_drag_button} round-trip the static
     * {@code WinDnDClipboard.dragButton}; {@code dnd_set_source_supported_actions} is what
     * {@code supportedSourceActionsFromSystem} answers while non-zero, and with 0 the peer falls
     * through to {@code pop()}, which finds no data object and answers {@code ACTION_NONE}. Through
     * the hook (the fixed 1001s) and directly; everything restored to 0 afterwards.
     */
    @Test
    public void theButtonAndSourceActionSlotsRoundTripTheSingletonsState() {
        try {
            assertEquals(OK, asEventThread(() -> WinGlassNativeShim.dndSetDragButtonTarget(3)));
            ButtonResult button = asEventThread(WinGlassNativeShim::dndGetDragButtonTarget);
            assertEquals(OK, button.status());
            assertEquals(3, button.button());
            assertEquals(OK, WinGlassNativeShim.fireDndCallback(7, 0L, false).returned());
            DndFire read = WinGlassNativeShim.fireDndCallback(8, 0L, true);
            assertEquals(OK, read.returned());
            assertEquals(1001, read.outInt());

            assertEquals(OK, asEventThread(() -> WinGlassNativeShim.dndSetDragButtonTarget(0)));
            // The assistance keeps the DnD peer alive (and cached) for the three reads below.
            ClipboardAssistance assistance = WinGlassNativeShim.openClipboardAssistance(Clipboard.DND);
            try {
                assertEquals(OK, asEventThread(() -> WinGlassNativeShim.dndSetSourceSupportedActionsTarget(2)));
                long dndId = WinClipboardNativeTest.registeredIdNamed(Clipboard.DND);
                assertEquals(2, WinGlassNativeShim.clipboardSupportedSourceActions(assistanceClipboard(dndId)));
                assertEquals(OK, WinGlassNativeShim.fireDndCallback(6, 0L, false).returned());
                assertEquals(1001, WinGlassNativeShim.clipboardSupportedSourceActions(assistanceClipboard(dndId)));
                assertEquals(OK, asEventThread(() -> WinGlassNativeShim.dndSetSourceSupportedActionsTarget(0)));
                assertEquals(Clipboard.ACTION_NONE,
                        WinGlassNativeShim.clipboardSupportedSourceActions(assistanceClipboard(dndId)),
                        "0 falls through to pop(), which finds no data object");
            } finally {
                WinGlassNativeShim.closeAssistance(assistance);
            }
        } finally {
            asEventThread(() -> WinGlassNativeShim.dndSetDragButtonTarget(0));
            asEventThread(() -> WinGlassNativeShim.dndSetSourceSupportedActionsTarget(0));
        }
    }

    /**
     * Off the application thread {@code getInstance()} throws {@code IllegalStateException} from
     * {@code Clipboard.get}'s thread check - the second effect the JNI relied on - and every one of
     * the five singleton slots describes it to stderr, reports nothing to the uncaught-exception
     * handler, answers {@code GWIN_ERR_UPCALL} and writes nothing.
     */
    @Test
    public void theSingletonSlotsThrowOffTheEventThreadAndAreDescribed() {
        AtomicReference<String> err = new AtomicReference<>();
        Throwable reported = WinClipboardNativeTest.reportedBy(() -> err.set(WinClipboardNativeTest.captureStderr(
                () -> {
            HandleResult get = WinGlassNativeShim.dndGetDataObjectTarget();
            assertEquals(UPCALL, get.status());
            assertEquals(-1L, get.handle(), "*out_data_object untouched");
            assertEquals(UPCALL, WinGlassNativeShim.dndSetDataObjectTarget(0x20L));
            assertEquals(UPCALL, WinGlassNativeShim.dndSetSourceSupportedActionsTarget(1));
            assertEquals(UPCALL, WinGlassNativeShim.dndSetDragButtonTarget(1));
            ButtonResult button = WinGlassNativeShim.dndGetDragButtonTarget();
            assertEquals(UPCALL, button.status());
            assertEquals(-1, button.button(), "*out_button untouched");
        })));
        assertNull(reported);
        assertTrue(err.get().contains("IllegalStateException"), err.get());
        assertEquals(5, err.get().split("IllegalStateException", -1).length - 1, "five traces");
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** The registered DnD peer, as a {@code SystemClipboard} the shim accessors accept. */
    private static com.sun.glass.ui.SystemClipboard assistanceClipboard(long dndId) {
        return WinGlassNativeShim.registeredClipboard(dndId);
    }

    static <T> T asEventThread(java.util.function.Supplier<T> body) {
        return WinGlassNativeShim.asEventThread(body);
    }

    /**
     * A registered headless view with a recording handler for the duration of {@code body}, unregistered afterwards.
     */
    private static void withView(java.util.function.Function<DragHandler, Consumer<HeadlessView>> body) {
        DragHandler handler = new DragHandler();
        HeadlessView view = WinGlassNativeShim.createHeadlessView(handler);
        try {
            body.apply(handler).accept(view);
        } finally {
            WinGlassNativeShim.unregisterView(view.id());
        }
    }

    /**
     * Records the four drag callbacks as {@code name[arguments]}, answers {@link #answer}, throws {@link #failure} when
     * set.
     */
    private static final class DragHandler extends View.EventHandler {

        final List<String> calls = new ArrayList<>();
        ClipboardAssistance lastAssistance;
        int answer = Clipboard.ACTION_NONE;
        RuntimeException failure;

        private int record(String name, ClipboardAssistance assistance, Object... arguments) {
            calls.add(name + Arrays.toString(arguments));
            lastAssistance = assistance;
            if (failure != null) {
                throw failure;
            }
            return answer;
        }

        @Override
        public int handleDragEnter(View view, int x, int y, int xAbs, int yAbs, int recommendedDropAction,
                                   ClipboardAssistance dropTargetAssistant) {
            return record("dragEnter", dropTargetAssistant, x, y, xAbs, yAbs, recommendedDropAction);
        }

        @Override
        public int handleDragOver(View view, int x, int y, int xAbs, int yAbs, int recommendedDropAction,
                                  ClipboardAssistance dropTargetAssistant) {
            return record("dragOver", dropTargetAssistant, x, y, xAbs, yAbs, recommendedDropAction);
        }

        @Override
        public void handleDragLeave(View view, ClipboardAssistance dropTargetAssistant) {
            record("dragLeave", dropTargetAssistant);
        }

        @Override
        public int handleDragDrop(View view, int x, int y, int xAbs, int yAbs, int recommendedDropAction,
                                  ClipboardAssistance dropTargetAssistant) {
            return record("dragDrop", dropTargetAssistant, x, y, xAbs, yAbs, recommendedDropAction);
        }
    }
}

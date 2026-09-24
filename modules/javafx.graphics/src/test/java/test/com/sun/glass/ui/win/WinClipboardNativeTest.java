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
import com.sun.glass.ui.DelayedCallback;
import com.sun.glass.ui.win.WinGlassNativeShim;
import com.sun.glass.ui.win.WinGlassNativeShim.CBuiltStringBlock;
import com.sun.glass.ui.win.WinGlassNativeShim.FosResult;
import com.sun.glass.ui.win.WinGlassNativeShim.HeadlessClipboard;
import com.sun.glass.ui.win.WinGlassNativeShim.RecordedCall;
import com.sun.glass.ui.win.WinGlassNativeShim.RecordingAssistance;
import com.sun.glass.ui.win.WinGlassNativeShim.StringBlockRejection;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The system clipboard peer, {@code WinSystemClipboard}, on the {@code gwin_clipboard_*} ABI:
 * the nineteen exports of the clipboard / drag-and-drop / dialog section plus their three
 * test hooks, {@code ole32!OleIsCurrentClipboard}, the {@code GwinClipboardCallbacks} table (seven
 * slots) and the seven upcall targets behind it, and the registry whose lifetime rule keeps a
 * {@code ClipboardData} that outlives {@code close()} servable.
 * <p>
 * <b>Two oracles, as for the view table.</b> {@code gwin_test_fire_clipboard_callback} drives one
 * slot of the installed table with a pattern fixed in the header; fired into a <em>recording</em>
 * table built from the facade's own descriptor objects it proves each descriptor argument by
 * argument - including the {@code fos_serialize} block hand-over, which the hook frees exactly as
 * {@code ClipboardData::GetData} does - and fired into the <em>production</em> table with a headless
 * peer registered under the id it proves the target, the registry lookup and the peer call. Every
 * target is also driven directly, for the arms the fixed pattern cannot reach: a throwing peer, an
 * unknown id, a null {@code byte[]}.
 * <p>
 * <b>Every downcall here is headless-safe.</b> The nine marshalled exports go through
 * {@code SendMessage(WM_DO_ACTION)}, which without a {@code GlassApplication} does not run the body
 * and answers the DEFINED no-toolkit value this section promises (unlike the view and window
 * sections): that promise is asserted for each. Nothing here touches the real OLE clipboard - a
 * push, a pop or a flush needs the toolkit, and belongs to {@code tests/system}'s
 * {@code ClipboardTest} against AWT.
 */
@EnabledOnOs(OS.WINDOWS)
public class WinClipboardNativeTest {

    /** The nineteen exports of the header's section plus its three test hooks, in the header's order. */
    static final List<String> SECTION_EXPORTS = List.of("gwin_sizeof_clipboard_callbacks",
            "gwin_sizeof_dnd_callbacks", "gwin_clipboard_set_callbacks", "gwin_dnd_set_callbacks",
            "gwin_test_fire_clipboard_callback", "gwin_test_fire_dnd_callback", "gwin_test_string_block",
            "gwin_alloc", "gwin_free", "gwin_clipboard_register_viewer", "gwin_clipboard_dispose",
            "gwin_clipboard_push", "gwin_clipboard_pop", "gwin_clipboard_pop_bytes", "gwin_clipboard_pop_mimes",
            "gwin_clipboard_push_target_action", "gwin_clipboard_pop_supported_actions", "gwin_dnd_push",
            "gwin_dnd_dispose", "gwin_sizeof_file_filter", "gwin_dialog_file", "gwin_dialog_folder");

    /** An id the registry never hands out: it counts up from 1 and no test registers this many. */
    static final long UNKNOWN_ID = 0x7FFF_FFF0L;

    static final long INVALID_ARG = -1L;
    static final int NO_TOOLKIT = -2;
    static final int UPCALL = -3;
    static final int OK = 0;

    /** The header's {@code fos_serialize} pattern: "AB" and an int64 index with bits above 32 set. */
    static final long FOS_INDEX = 0x1_0000_0002L;
    static final long FAKE_DATA_OBJECT = 0x0000_7FFE_1234_5678L;

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
    public void theSectionExportsAndOleIsCurrentClipboardResolve() {
        for (String export : SECTION_EXPORTS) {
            assertTrue(WinGlassNativeShim.resolves("glass!" + export), export);
        }
        assertTrue(WinGlassNativeShim.resolves("ole32!OleIsCurrentClipboard"));
        assertEquals(22, SECTION_EXPORTS.size());
        assertEquals(WinGlassNativeShim.constant("GWIN_ERR_UPCALL"), UPCALL);
        assertEquals(WinGlassNativeShim.constant("GWIN_ERR_OLE"), -4);
        assertEquals(WinGlassNativeShim.constant("GWIN_ERR_NO_TOOLKIT"), NO_TOOLKIT);
    }

    /**
     * All three layouts against the {@code sizeof} the C compiler computed, both ways, and every slot
     * at {@code 8 * index} read through {@code PathElement}: a slot appended in C without the layout
     * following it is what the probe exists to catch, and a stale {@code glass.dll} from a cache
     * shadowing a fresh build is what the probe catches first.
     */
    @Test
    public void allThreeSizeofProbesAgreeWithTheLayoutsBothWays() {
        assertEquals(56, WinGlassNativeShim.sizeOfClipboardCallbacks());
        assertEquals(56, WinGlassNativeShim.layoutByteSize("GwinClipboardCallbacks"));
        assertEquals(72, WinGlassNativeShim.sizeOfDndCallbacks());
        assertEquals(72, WinGlassNativeShim.layoutByteSize("GwinDndCallbacks"));
        assertEquals(16, WinGlassNativeShim.sizeOfFileFilter());
        assertEquals(16, WinGlassNativeShim.layoutByteSize("GwinFileFilter"));

        List<String> clipboardSlots = WinGlassNativeShim.clipboardSlotNames();
        assertEquals(List.of("fos_serialize", "action_performed", "drag_action_performed", "content_changed",
                "dispose_peer", "set_data_object", "data_object_disposed"), clipboardSlots);
        for (int i = 0; i < clipboardSlots.size(); i++) {
            assertEquals(8L * i, WinGlassNativeShim.offset("GwinClipboardCallbacks", clipboardSlots.get(i)));
        }
        List<String> dndSlots = WinGlassNativeShim.dndSlotNames();
        assertEquals(List.of("drag_enter", "drag_over", "drag_drop", "drag_leave", "dnd_get_data_object",
                "dnd_set_data_object", "dnd_set_source_supported_actions", "dnd_set_drag_button",
                "dnd_get_drag_button"), dndSlots);
        for (int i = 0; i < dndSlots.size(); i++) {
            assertEquals(8L * i, WinGlassNativeShim.offset("GwinDndCallbacks", dndSlots.get(i)));
        }
        assertEquals(0L, WinGlassNativeShim.offset("GwinFileFilter", "description"));
        assertEquals(8L, WinGlassNativeShim.offset("GwinFileFilter", "extensions"));
    }

    /**
     * Both tables are installed by {@code WinGlassNative}'s own static initializer - loading the
     * facade is enough, no {@code WinApplication} and no peer has been touched - with no NULL slot,
     * and installing again creates no second set of stubs.
     */
    @Test
    public void bothTablesAreInstalledByTheFacadesInitializerWithNoNullSlot() {
        WinGlassNativeShim.loadLibrary();
        assertTrue(WinGlassNativeShim.clipboardCallbacksInstalled());
        assertTrue(WinGlassNativeShim.dndCallbacksInstalled());
        List<Long> clipboardStubs = WinGlassNativeShim.installedClipboardCallbackStubAddresses();
        List<Long> dndStubs = WinGlassNativeShim.installedDndCallbackStubAddresses();
        assertEquals(7, clipboardStubs.size());
        assertEquals(9, dndStubs.size());
        for (long stub : clipboardStubs) {
            assertNotEquals(0L, stub);
        }
        for (long stub : dndStubs) {
            assertNotEquals(0L, stub);
        }
        assertEquals(16, clipboardStubs.size() + dndStubs.size(), "sixteen distinct stubs");
        assertEquals(16, java.util.stream.Stream.concat(clipboardStubs.stream(), dndStubs.stream())
                .distinct().count());
        WinGlassNativeShim.installClipboardCallbacks();
        WinGlassNativeShim.installDndCallbacks();
        WinGlassNativeShim.reinstallClipboardCallbacks();
        WinGlassNativeShim.reinstallDndCallbacks();
        assertEquals(clipboardStubs, WinGlassNativeShim.installedClipboardCallbackStubAddresses());
        assertEquals(dndStubs, WinGlassNativeShim.installedDndCallbackStubAddresses());
    }

    // ---------------------------------------------------------------------------------------------
    // The through-C oracle: every clipboard slot's descriptor, argument by argument
    // ---------------------------------------------------------------------------------------------

    /**
     * Each slot fired into the recording table delivers the header's pattern through the facade's
     * descriptor. Slot 0's return is {@code *out_len} of the block the recording target handed over
     * through {@code gwin_alloc} - the hook read it, freed it, and reported its length, which is the
     * block ownership hand-over of {@code ClipboardData::GetData} end to end. Slot 5's pointer has
     * bits above 32 set so a truncated {@code void*} would show.
     */
    @Test
    public void everyClipboardSlotDeliversTheHeadersPatternThroughTheFacadesDescriptor() {
        long id = 0x0102_0304_0506_0708L;
        assertClipboardRecorded(0, id, WinGlassNativeShim.RECORDING_FOS_BYTES.length, List.of(id, "AB", FOS_INDEX));
        assertClipboardRecorded(1, id, WinGlassNativeShim.RECORDING_STATUS, List.of(id, 1001));
        assertClipboardRecorded(2, id, 0L, List.of(id, 1001));
        assertClipboardRecorded(3, id, 0L, List.of(id));
        assertClipboardRecorded(4, id, 0L, List.of(id));
        assertClipboardRecorded(5, id, 0L, List.of(id, FAKE_DATA_OBJECT));
        assertClipboardRecorded(6, id, 0L, List.of(id));
    }

    private static void assertClipboardRecorded(int slot, long id, long expectedReturn,
                                                List<Object> expectedArguments) {
        RecordedCall call = WinGlassNativeShim.fireClipboardIntoRecordingTable(slot, id);
        assertEquals(expectedArguments, call.arguments(), "slot " + slot);
        assertEquals(expectedReturn, call.returned(), "slot " + slot + " return");
    }

    /** An unknown slot answers {@code GWIN_ERR_INVALID_ARG}, whichever table is installed. */
    @Test
    public void anUnknownClipboardSlotIsRejected() {
        for (int slot : new int[] {-1, 7, 100, 1000}) {
            assertEquals(INVALID_ARG, WinGlassNativeShim.fireClipboardIntoRecordingTable(slot, 1L).returned(),
                    "slot " + slot);
            assertEquals(INVALID_ARG, WinGlassNativeShim.fireClipboardCallback(slot, 1L), "slot " + slot);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The production path: a registered headless peer receives what the C sent
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code fos_serialize} through the production table: the peer's {@code fosSerialize("AB", ...)}
     * serves the local data flushed under that mime as UTF-16LE plus its terminator, and the hook
     * reports the length of the {@code gwin_alloc} block it received and freed - 8 for "xyz". With
     * no local data the peer answers null, the target writes NULL, and the hook reports -100.
     */
    @Test
    public void fosSerializeServesTheLocalDataThroughTheHook() {
        withClipboard(clipboard -> {
            assertEquals(-100L, WinGlassNativeShim.fireClipboardCallback(0, clipboard.id()), "no local data yet");
            WinGlassNativeShim.flushClipboard(clipboard.peer(), null, Map.of("AB", "xyz"), Clipboard.ACTION_COPY);
            assertEquals(8L, WinGlassNativeShim.fireClipboardCallback(0, clipboard.id()));
        });
    }

    /**
     * {@code fos_serialize} driven directly: the bytes are what {@code fosSerialize} produced, in a
     * block obtained from {@code gwin_alloc} that the driver (standing in for the C) frees; a null
     * result is a NULL block with length 0 and {@code GWIN_OK}; an unknown id is the same. An EMPTY
     * {@code byte[]} is neither: a real block ({@code gwin_alloc(0)} is one byte, never NULL) with
     * length 0, which is how the C tells "nothing to render" from "rendered nothing".
     */
    @Test
    public void fosSerializeHandsOverAGwinAllocBlockOrNull() {
        withClipboard(clipboard -> {
            FosResult empty = asEventThread(
                    () -> WinGlassNativeShim.fosSerializeTarget(clipboard.id(), "text/plain", -1L));
            assertEquals(OK, empty.status());
            assertTrue(empty.blockWasNull());
            assertEquals(0, empty.length());

            WinGlassNativeShim.flushClipboard(clipboard.peer(), null, Map.of("x", ByteBuffer.allocate(0)),
                    Clipboard.ACTION_COPY);
            FosResult zeroBytes = asEventThread(() -> WinGlassNativeShim.fosSerializeTarget(clipboard.id(), "x", -1L));
            assertEquals(OK, zeroBytes.status());
            assertFalse(zeroBytes.blockWasNull(), "an empty byte[] is a real gwin_alloc(0) block, not NULL");
            assertEquals(0, zeroBytes.length());
            assertArrayEquals(new byte[0], zeroBytes.bytes());

            WinGlassNativeShim.flushClipboard(clipboard.peer(), null,
                    Map.of("text/plain", "ab\ncd", "text/rtf", "{rtf}"), Clipboard.ACTION_COPY);
            FosResult text = asEventThread(
                    () -> WinGlassNativeShim.fosSerializeTarget(clipboard.id(), "text/plain", -1L));
            assertEquals(OK, text.status());
            assertFalse(text.blockWasNull());
            byte[] expected = "ab\r\ncd\0".getBytes(StandardCharsets.UTF_16LE);
            assertArrayEquals(expected, text.bytes(), "CRLF-normalised UTF-16LE plus the terminator");
            assertEquals(expected.length, text.length());

            FosResult rtf = asEventThread(() -> WinGlassNativeShim.fosSerializeTarget(clipboard.id(), "text/rtf", -1L));
            assertArrayEquals("{rtf}\0".getBytes(StandardCharsets.US_ASCII), rtf.bytes());
        });
        FosResult unknown = WinGlassNativeShim.fosSerializeTarget(UNKNOWN_ID, "text/plain", -1L);
        assertEquals(OK, unknown.status());
        assertTrue(unknown.blockWasNull());
    }

    /**
     * A throwing {@code fosSerialize} - a {@code DelayedCallback} whose {@code providedData()} throws,
     * the user code the local data can carry - is <em>described</em> (a stack trace on
     * {@code System.err}, as {@code checkJavaException}'s {@code ExceptionDescribe}), NOT reported to
     * the application's uncaught-exception handler, answers {@code GWIN_ERR_UPCALL}, and writes
     * nothing: the out-parameters keep their sentinels.
     */
    @Test
    public void aThrowingFosSerializeIsDescribedNotReportedAndWritesNothing() {
        withClipboard(clipboard -> {
            RuntimeException failure = new IllegalStateException("WinClipboardNativeTest: deliberate");
            DelayedCallback throwing = () -> {
                throw failure;
            };
            WinGlassNativeShim.flushClipboard(clipboard.peer(), null, Map.of("text/plain", throwing),
                    Clipboard.ACTION_COPY);
            AtomicReference<FosResult> result = new AtomicReference<>();
            AtomicReference<String> err = new AtomicReference<>();
            Throwable reported = reportedBy(() -> err.set(captureStderr(() -> result.set(asEventThread(
                    () -> WinGlassNativeShim.fosSerializeTarget(clipboard.id(), "text/plain", -1L))))));
            assertNull(reported, "checkJavaException ran no Java code");
            assertEquals(UPCALL, result.get().status());
            assertEquals(-1, result.get().length(), "the out-parameters were left untouched");
            assertTrue(err.get().contains("WinClipboardNativeTest: deliberate"), err.get());
        });
    }

    /**
     * {@code action_performed} and {@code drag_action_performed} both reach
     * {@code Clipboard.actionPerformed}, i.e. the data source the peer was flushed with, and
     * {@code content_changed} reaches every assistance; through the hook (the fixed 1001) and
     * directly. The data source is a real {@code ClipboardAssistance} on the SYSTEM clipboard, which
     * makes the delegate create the real {@code WinSystemClipboard} - one more registry entry, gone
     * again when the assistance closes and the clipboard with it.
     */
    @Test
    public void theActionAndContentSlotsReachTheDataSourceAndTheAssistants() {
        int before = WinGlassNativeShim.clipboardRegistrySize();
        RecordingAssistance assistance = WinGlassNativeShim.openRecordingAssistance(Clipboard.SYSTEM);
        try {
            assertEquals(before + 1, WinGlassNativeShim.clipboardRegistrySize(), "the delegate made a SYSTEM peer");
            withClipboard(clipboard -> {
                WinGlassNativeShim.flushClipboard(clipboard.peer(), assistance, Map.of("text/plain", "x"),
                        Clipboard.ACTION_COPY);
                assertEquals(0L, WinGlassNativeShim.fireClipboardCallback(1, clipboard.id()));
                assertEquals(0L, WinGlassNativeShim.fireClipboardCallback(2, clipboard.id()));
                assertEquals(List.of("actionPerformed[1001]", "actionPerformed[1001]"), assistance.calls);
                assistance.calls.clear();
                assertEquals(OK, asEventThread(() -> WinGlassNativeShim.actionPerformedTarget(clipboard.id(), 2)));
                asEventThread(() -> {
                    WinGlassNativeShim.dragActionPerformedTarget(clipboard.id(), 0x40000000);
                    return null;
                });
                assertEquals(List.of("actionPerformed[2]", "actionPerformed[1073741824]"), assistance.calls);
            });
            // content_changed reaches the assistants of the clipboard the id resolves to: the SYSTEM one.
            long systemId = registeredIdNamed(Clipboard.SYSTEM);
            assistance.calls.clear();
            assertEquals(0L, WinGlassNativeShim.fireClipboardCallback(3, systemId));
            asEventThread(() -> {
                WinGlassNativeShim.contentChangedTarget(systemId);
                return null;
            });
            assertEquals(List.of("contentChanged[]", "contentChanged[]"), assistance.calls);
        } finally {
            WinGlassNativeShim.closeAssistance(assistance);
        }
        assertEquals(before, WinGlassNativeShim.clipboardRegistrySize(), "closing the last assistance closed the peer");
    }

    /**
     * The two sinks, per site. {@code action_performed} (the {@code IDataObject::SetData} site,
     * {@code checkJavaException}) describes to stderr and answers {@code GWIN_ERR_UPCALL};
     * {@code drag_action_performed} and {@code content_changed} ({@code CheckAndClearException})
     * report through the uncaught-exception handler. Off the event thread every one of them throws
     * {@code IllegalStateException} from {@code Application.checkEventThread()}, which is the throw
     * that is used here.
     */
    @Test
    public void eachSlotUsesTheSinkTheJniUsedAtItsSite() {
        withClipboard(clipboard -> {
            AtomicReference<Integer> status = new AtomicReference<>();
            AtomicReference<String> err = new AtomicReference<>();
            assertNull(reportedBy(() -> err.set(captureStderr(
                    () -> status.set(WinGlassNativeShim.actionPerformedTarget(clipboard.id(), 1))))));
            assertEquals(UPCALL, status.get());
            assertTrue(err.get().contains("IllegalStateException"), err.get());

            Throwable dragReported = reportedBy(() -> WinGlassNativeShim.dragActionPerformedTarget(clipboard.id(), 1));
            assertNotNull(dragReported);
            assertTrue(dragReported instanceof IllegalStateException, String.valueOf(dragReported));

            Throwable contentReported = reportedBy(() -> WinGlassNativeShim.contentChangedTarget(clipboard.id()));
            assertNotNull(contentReported);
            assertTrue(contentReported instanceof IllegalStateException, String.valueOf(contentReported));
        });
    }

    // ---------------------------------------------------------------------------------------------
    // The registry and the handle: set_data_object, data_object_disposed, dispose_peer, close
    // ---------------------------------------------------------------------------------------------

    /** Ids count up from 1, the constructor registers, the handle starts NULL, nothing is live, and close removes. */
    @Test
    public void theRegistryStartsAtOneAndRoundTrips() {
        int before = WinGlassNativeShim.clipboardRegistrySize();
        HeadlessClipboard first = WinGlassNativeShim.createHeadlessClipboard();
        HeadlessClipboard second = WinGlassNativeShim.createHeadlessClipboard();
        try {
            assertTrue(first.id() >= 1L);
            assertTrue(second.id() > first.id());
            assertEquals(first.id(), WinGlassNativeShim.clipboardIdOf(first.peer()));
            assertEquals(before + 2, WinGlassNativeShim.clipboardRegistrySize());
            assertTrue(WinGlassNativeShim.clipboardRegistered(first.id()));
            assertEquals(0L, WinGlassNativeShim.clipboardHandleOf(first.peer()));
            assertEquals(0, WinGlassNativeShim.clipboardLiveObjectsOf(first.peer()));
            assertFalse(WinGlassNativeShim.clipboardClosed(first.peer()));
            WinGlassNativeShim.closeClipboard(first.peer());
            assertTrue(WinGlassNativeShim.clipboardClosed(first.peer()));
            assertFalse(WinGlassNativeShim.clipboardRegistered(first.id()), "closed with nothing live: gone");
            assertEquals(before + 1, WinGlassNativeShim.clipboardRegistrySize());
        } finally {
            WinGlassNativeShim.unregisterClipboard(first.id());
            WinGlassNativeShim.unregisterClipboard(second.id());
        }
        assertEquals(before, WinGlassNativeShim.clipboardRegistrySize());
    }

    /**
     * {@code set_data_object} publishes the handle and counts a live object for a non-NULL one; a
     * NULL publish stores NULL and counts nothing; {@code data_object_disposed} counts down and never
     * below zero. Through the hook (the fake pointer with bits above 32) and directly.
     */
    @Test
    public void setDataObjectPublishesTheHandleAndCountsLiveObjects() {
        withClipboard(clipboard -> {
            assertEquals(0L, WinGlassNativeShim.fireClipboardCallback(5, clipboard.id()));
            assertEquals(FAKE_DATA_OBJECT, WinGlassNativeShim.clipboardHandleOf(clipboard.peer()), "all 64 bits");
            assertEquals(1, WinGlassNativeShim.clipboardLiveObjectsOf(clipboard.peer()));
            WinGlassNativeShim.setDataObjectTarget(clipboard.id(), 0x10L);
            assertEquals(0x10L, WinGlassNativeShim.clipboardHandleOf(clipboard.peer()));
            assertEquals(2, WinGlassNativeShim.clipboardLiveObjectsOf(clipboard.peer()));
            WinGlassNativeShim.setDataObjectTarget(clipboard.id(), 0L);
            assertEquals(0L, WinGlassNativeShim.clipboardHandleOf(clipboard.peer()));
            assertEquals(2, WinGlassNativeShim.clipboardLiveObjectsOf(clipboard.peer()), "NULL is not an object");
            assertEquals(0L, WinGlassNativeShim.fireClipboardCallback(6, clipboard.id()));
            WinGlassNativeShim.dataObjectDisposedTarget(clipboard.id());
            WinGlassNativeShim.dataObjectDisposedTarget(clipboard.id());
            assertEquals(0, WinGlassNativeShim.clipboardLiveObjectsOf(clipboard.peer()), "clamped at zero");
            assertTrue(WinGlassNativeShim.clipboardRegistered(clipboard.id()), "not closed: still registered");
        });
    }

    /**
     * The lifetime rule, both orders. A peer closed while an object is live keeps its entry (closed,
     * handle NULL) until the C destroys that object, so a paste that reaches {@code fos_serialize}
     * after {@code close()} still finds it; the last {@code data_object_disposed} removes it. The other
     * order - disposed first, then closed - removes at close.
     */
    @Test
    public void theEntryOutlivesCloseWhileAnObjectIsLive() {
        HeadlessClipboard clipboard = WinGlassNativeShim.createHeadlessClipboard();
        try {
            WinGlassNativeShim.setDataObjectTarget(clipboard.id(), 0x10L);
            WinGlassNativeShim.closeClipboard(clipboard.peer());
            assertTrue(WinGlassNativeShim.clipboardClosed(clipboard.peer()));
            assertEquals(0L, WinGlassNativeShim.clipboardHandleOf(clipboard.peer()), "close nulls the handle");
            assertTrue(WinGlassNativeShim.clipboardRegistered(clipboard.id()), "one object is still live");
            WinGlassNativeShim.dataObjectDisposedTarget(clipboard.id());
            assertFalse(WinGlassNativeShim.clipboardRegistered(clipboard.id()), "the last object went: gone");
        } finally {
            WinGlassNativeShim.unregisterClipboard(clipboard.id());
        }
        HeadlessClipboard other = WinGlassNativeShim.createHeadlessClipboard();
        try {
            WinGlassNativeShim.setDataObjectTarget(other.id(), 0x20L);
            WinGlassNativeShim.dataObjectDisposedTarget(other.id());
            assertTrue(WinGlassNativeShim.clipboardRegistered(other.id()), "not closed yet");
            WinGlassNativeShim.closeClipboard(other.peer());
            assertFalse(WinGlassNativeShim.clipboardRegistered(other.id()));
        } finally {
            WinGlassNativeShim.unregisterClipboard(other.id());
        }
    }

    /**
     * {@code dispose_peer}: the previously registered peer's dispose - {@code gwin_clipboard_dispose}
     * of its handle (nothing runs without a toolkit) - and then the handle is nulled, which the JNI
     * did not do and which keeps a later {@code close()} from releasing the same object twice. The
     * live count is untouched: only {@code ~ClipboardData} can lower it.
     */
    @Test
    public void disposePeerDisposesAndNullsTheHandle() {
        withClipboard(clipboard -> {
            WinGlassNativeShim.setDataObjectTarget(clipboard.id(), 0x10L);
            assertEquals(0L, WinGlassNativeShim.fireClipboardCallback(4, clipboard.id()));
            assertEquals(0L, WinGlassNativeShim.clipboardHandleOf(clipboard.peer()));
            assertEquals(1, WinGlassNativeShim.clipboardLiveObjectsOf(clipboard.peer()));
            assertFalse(WinGlassNativeShim.clipboardClosed(clipboard.peer()), "disposed is not closed");
            WinGlassNativeShim.setDataObjectTarget(clipboard.id(), 0x30L);
            WinGlassNativeShim.disposePeerTarget(clipboard.id());
            assertEquals(0L, WinGlassNativeShim.clipboardHandleOf(clipboard.peer()));
            WinGlassNativeShim.dataObjectDisposedTarget(clipboard.id());
            WinGlassNativeShim.dataObjectDisposedTarget(clipboard.id());
        });
    }

    /** Every clipboard slot is silent for an id the registry does not know: no report, no trace, the slot default. */
    @Test
    public void anUnknownIdIsSilentOnEveryClipboardSlot() {
        AtomicReference<String> err = new AtomicReference<>();
        Throwable reported = reportedBy(() -> err.set(captureStderr(() -> {
            assertEquals(-100L, WinGlassNativeShim.fireClipboardCallback(0, UNKNOWN_ID), "fos_serialize -> NULL");
            assertEquals(0L, WinGlassNativeShim.fireClipboardCallback(1, UNKNOWN_ID), "action_performed -> OK");
            for (int slot = 2; slot <= 6; slot++) {
                assertEquals(0L, WinGlassNativeShim.fireClipboardCallback(slot, UNKNOWN_ID), "slot " + slot);
            }
            assertEquals(OK, WinGlassNativeShim.actionPerformedTarget(UNKNOWN_ID, 1));
            WinGlassNativeShim.dragActionPerformedTarget(UNKNOWN_ID, 1);
            WinGlassNativeShim.contentChangedTarget(UNKNOWN_ID);
            WinGlassNativeShim.disposePeerTarget(UNKNOWN_ID);
            WinGlassNativeShim.setDataObjectTarget(UNKNOWN_ID, 0x10L);
            WinGlassNativeShim.dataObjectDisposedTarget(UNKNOWN_ID);
        })));
        assertNull(reported);
        assertEquals("", err.get());
    }

    // ---------------------------------------------------------------------------------------------
    // Strings, blocks and the allocator
    // ---------------------------------------------------------------------------------------------

    /**
     * A string block round-trips through {@code gwin_alloc} / {@code gwin_free}: the facade's writer
     * lays the strings end to end with their NULs and the extra one, the reader walks them back by
     * count. Non-ASCII, a surrogate pair (two code units, as {@code GetStringRegion} passed them), an
     * empty string in the middle (one NUL, not a terminator - the count is what bounds the walk), an
     * empty list (one NUL, never NULL, because {@code gwin_clipboard_push} rejects a NULL block), and a
     * U+0000 inside a string: the C walks a block with {@code wcslen}, so the writer cuts there - the
     * same first-NUL view {@code wcscmp} / {@code RegisterClipboardFormat} took of each key's own
     * {@code JString} under JNI - instead of letting the tail become the next string and the count
     * drop the last one.
     */
    @Test
    public void aStringBlockRoundTripsThroughGwinAllocAndGwinFree() {
        String[] mimes = {"text/plain", "application/x-java-rawimage",
            "message/external-body;access-type=clipboard;index=0;name=\"a b\"", "\u00e9\u4e2d", "\uD83D\uDE00-x"};
        assertArrayEquals(mimes, WinGlassNativeShim.stringBlockRoundTrip(mimes));
        long units = 1;
        for (String mime : mimes) {
            units += mime.length() + 1;
        }
        assertEquals(units, WinGlassNativeShim.stringBlockUnits(mimes), "every NUL and the extra one");
        assertEquals(4, mimes[4].length(), "the surrogate pair is two code units, plus '-' and 'x'");

        String[] withEmpty = {"a", "", "b"};
        assertArrayEquals(withEmpty, WinGlassNativeShim.stringBlockRoundTrip(withEmpty));
        assertArrayEquals(new char[] {'a', 0, 0, 'b', 0, 0}, WinGlassNativeShim.stringBlockUnitsOf(withEmpty));

        String[] none = {};
        assertArrayEquals(none, WinGlassNativeShim.stringBlockRoundTrip(none));
        assertEquals(1L, WinGlassNativeShim.stringBlockUnits(none));
        assertArrayEquals(new char[] {0}, WinGlassNativeShim.stringBlockUnitsOf(none));

        String[] withNul = {"x\0y", "text/plain"};
        assertArrayEquals(new String[] {"x", "text/plain"}, WinGlassNativeShim.stringBlockRoundTrip(withNul),
                "the tail after the NUL is not a second string and text/plain is still the second");
        assertArrayEquals(new char[] {'x', 0, 't', 'e', 'x', 't', '/', 'p', 'l', 'a', 'i', 'n', 0, 0},
                WinGlassNativeShim.stringBlockUnitsOf(withNul));
        assertEquals(14L, WinGlassNativeShim.stringBlockUnits(withNul), "the cut is counted, not just written");
        assertArrayEquals(new String[] {"", "b"}, WinGlassNativeShim.stringBlockRoundTrip(new String[] {"\0abc", "b"}),
                "a leading NUL is an empty string, as RegisterClipboardFormat(L\"\") saw it");
    }

    /**
     * A string block BUILT BY THE C - {@code GwinMakeStringBlock}, the builder behind
     * {@code gwin_clipboard_pop_mimes} and {@code gwin_dialog_file} - parsed by the facade's reader.
     * The round trip above proves Java's writer and reader against each other; this proves the reader
     * against the C's writer, which until now it had only met over the hook's single "AB".
     * {@code gwin_test_string_block} hands over "alpha", "" and U+03B3 U+1F600 with count 3: the empty
     * middle string is one bare NUL that only the count keeps from ending the list, the surrogate pair
     * is two of the C's {@code wchar_t} units for one code point - 12 code units in all, as the
     * header's listing and {@code GwinMakeStringBlock}'s {@code 1 + sum(len + 1)} have it (the header's
     * prose says 13). The block is the caller's from the moment the hook returns and is freed exactly
     * once. With either out-pointer NULL the hook answers {@code GWIN_ERR_INVALID_ARG}, writes nothing
     * and hands over nothing.
     */
    @Test
    public void aCBuiltStringBlockIsParsedByTheFacadesReader() {
        CBuiltStringBlock built = WinGlassNativeShim.cBuiltStringBlock();
        assertEquals(OK, built.status());
        assertEquals(3, built.count());
        assertArrayEquals(new String[] {"alpha", "", "\u03B3\uD83D\uDE00"}, built.strings());
        char[] layout = {'a', 'l', 'p', 'h', 'a', 0, 0, '\u03B3', '\uD83D', '\uDE00', 0, 0};
        assertArrayEquals(layout, built.units(), "five, NUL | NUL | three, NUL | NUL: 12 code units");
        assertEquals(12, built.units().length);
        assertEquals(2, built.strings()[2].codePointCount(0, 3), "two code points in three units");

        int invalidArg = WinGlassNativeShim.constant("GWIN_ERR_INVALID_ARG");
        StringBlockRejection nullBlock = WinGlassNativeShim.stringBlockWithNullBlockPointer();
        assertEquals(invalidArg, nullBlock.status());
        assertTrue(nullBlock.untouched(), "out_count is not written when out_block is NULL");
        StringBlockRejection nullCount = WinGlassNativeShim.stringBlockWithNullCountPointer();
        assertEquals(invalidArg, nullCount.status());
        assertTrue(nullCount.untouched(), "out_block is not written when out_count is NULL");
    }

    /**
     * {@code gwin_alloc(0)} is a real block, {@code gwin_alloc(-1)} is NULL, and {@code gwin_free(NULL)} is a no-op.
     */
    @Test
    public void gwinAllocOfZeroIsARealBlockAndGwinFreeOfNullIsANoOp() {
        long empty = WinGlassNativeShim.gwinAlloc(0L);
        assertNotEquals(0L, empty);
        WinGlassNativeShim.gwinFree(empty);
        assertEquals(0L, WinGlassNativeShim.gwinAlloc(-1L));
        WinGlassNativeShim.gwinFree(0L);
    }

    // ---------------------------------------------------------------------------------------------
    // The downcalls headlessly: the defined no-toolkit answers, the NULL guard, the cast
    // ---------------------------------------------------------------------------------------------

    /**
     * Without a {@code GlassApplication} the marshalled exports do not run their body and answer the
     * value this section DEFINES (unlike the view and window sections, whose garbage is why a
     * headless view is never closed): {@code GWIN_ERR_NO_TOOLKIT}, NULL, {@code ACTION_NONE}. A push
     * publishes nothing, so the peer's handle and live count stay as they were.
     */
    @Test
    public void everyMarshalledDownCallAnswersItsDefinedNoToolkitValue() {
        withClipboard(clipboard -> {
            assertEquals(NO_TOOLKIT, WinGlassNativeShim.clipboardRegisterViewer(clipboard.id()));
            assertEquals(0L, WinGlassNativeShim.clipboardPop(0L));
            assertNull(WinGlassNativeShim.clipboardPopBytes(0L, "text/plain", -1L));
            assertNull(WinGlassNativeShim.clipboardPopMimes(0L));
            assertEquals(Clipboard.ACTION_NONE, WinGlassNativeShim.clipboardPopSupportedActions(0L));
            Object[] keys = {"text/plain", "text/html"};
            assertEquals(NO_TOOLKIT, WinGlassNativeShim.clipboardPush(0L, clipboard.id(), keys, Clipboard.ACTION_COPY));
            assertEquals(NO_TOOLKIT, WinGlassNativeShim.dndPush(0L, clipboard.id(), keys, Clipboard.ACTION_ANY));
            assertEquals(NO_TOOLKIT, WinGlassNativeShim.clipboardPush(0L, clipboard.id(), new Object[0], 0),
                    "an empty mime set is a real block of one NUL, not the NULL the C rejects");
            assertEquals(0L, WinGlassNativeShim.clipboardHandleOf(clipboard.peer()), "nothing was published");
            assertEquals(0, WinGlassNativeShim.clipboardLiveObjectsOf(clipboard.peer()));
            WinGlassNativeShim.clipboardDispose(0L);
            WinGlassNativeShim.dndDispose(0L);
            WinGlassNativeShim.clipboardPushTargetAction(0L, Clipboard.ACTION_MOVE);
            assertFalse(WinGlassNativeShim.clipboardIsOwner(clipboard.peer()));
            assertEquals(Clipboard.ACTION_NONE, WinGlassNativeShim.clipboardSupportedSourceActions(clipboard.peer()),
                    "not the owner and pop() finds no data object");
        });
    }

    /**
     * {@code isOwner}'s NULL guard: a NULL handle is {@code false} without calling OLE at all (the
     * JNI body's first line), which is also what a headless peer answers.
     */
    @Test
    public void isOwnerIsFalseForANullHandleWithoutCallingOle() {
        assertFalse(WinGlassNativeShim.oleIsCurrentClipboard(0L));
    }

    /**
     * The keys {@code push} receives are cast to {@code String}, as the C cast the {@code jobject}s
     * blindly: a non-String element is a {@code ClassCastException} where it was undefined behaviour.
     */
    @Test
    public void aNonStringKeyIsAClassCastException() {
        assertThrows(ClassCastException.class,
                () -> WinGlassNativeShim.clipboardPush(0L, 1L, new Object[] {"text/plain", 42}, 0));
    }

    /**
     * The three clipboard, drag and dialog peers declare no native at all: every former native has a gwin_* twin or an
     * ole32 bind.
     */
    @Test
    public void theThreePeersDeclareNoNative() {
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinSystemClipboard"));
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinDnDClipboard"));
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinCommonDialogs"));
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** A registered headless peer for the duration of {@code body}, its entry dropped afterwards. */
    static void withClipboard(java.util.function.Consumer<HeadlessClipboard> body) {
        HeadlessClipboard clipboard = WinGlassNativeShim.createHeadlessClipboard();
        try {
            body.accept(clipboard);
        } finally {
            WinGlassNativeShim.unregisterClipboard(clipboard.id());
        }
    }

    /** The registry id of the peer named {@code name}, which must be registered. */
    static long registeredIdNamed(String name) {
        for (long id = 1L; id < 4096L; id++) {
            if (name.equals(WinGlassNativeShim.registeredClipboardName(id))) {
                return id;
            }
        }
        throw new AssertionError("no registered clipboard named " + name);
    }

    static <T> T asEventThread(java.util.function.Supplier<T> body) {
        return WinGlassNativeShim.asEventThread(body);
    }

    /** Runs {@code body} and returns the first throwable {@code Application.reportException} routed to this thread. */
    static Throwable reportedBy(Runnable body) {
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
    static String captureStderr(Runnable body) {
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
}

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

package com.sun.javafx.webkit.drt;

import com.sun.webkit.WKJStringCodec;
import com.sun.webkit.WebKitNative;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the {@code drt_*} C ABI of the {@code DumpRenderTreeJava} library, declared by
 * {@code Tools/DumpRenderTree/java/api/drt_java_api.h} and used by {@link DumpRenderTree}.
 * <p>
 * {@code DumpRenderTreeJava} is a <em>separate</em> shared library from {@code jfxwebkit}, with its
 * own entry points, its own {@code DRT_ABI_VERSION} and its own host table, so this class does its
 * own symbol resolution rather than going through {@link WebKitNative}. What the two share is the
 * string protocol in {@link WKJStringCodec}, which is exactly the vocabulary
 * {@code drt_java_api.h} includes {@code webkit_java_api.h} for, and which loads no library of its
 * own.
 * <p>
 * Ordering matters and is preserved: {@link DumpRenderTree} calls
 * {@code System.loadLibrary("DumpRenderTreeJava")} before the first entry point below, so that
 * {@link SymbolLookup#loaderLookup()} can see the library. The static initializer then installs the
 * host table with {@code drt_init} exactly once, which is what this library's deleted
 * {@code JNI_OnLoad} used to do.
 * <p>
 * No downcall here uses {@code Linker.Option.critical(true)}: {@code drt_did_finish_load} drains the
 * work queue and upcalls, and {@code drt_did_clear_window_object} installs JavaScript objects that
 * upcall later.
 */
final class DumpRenderTreeNative {

    /** The {@code drt_*} ABI revision this Java code is written against ({@code DRT_ABI_VERSION}). */
    private static final int DRT_ABI_VERSION = 1;

    /* drt_init result codes. */
    private static final int DRT_INIT_OK = 0;

    /* Slot counts, in the declaration order of drt_java_api.h. */
    private static final int CORE_SLOTS = 2;
    private static final int DRT_SLOTS = 8;
    private static final int EVENT_SENDER_SLOTS = 22;

    private static final Linker LINKER = Linker.nativeLinker();

    /*
     * The library is loaded by DumpRenderTree before anything here runs, so loaderLookup sees it.
     * Resolving it here rather than in WebKitNative is deliberate: the two libraries are versioned
     * and rebuilt separately, and mixing their lookups would let a jfxwebkit failure be reported as
     * a DumpRenderTreeJava one.
     */
    private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

    /*
     * The host table outlives everything and is created exactly once, which is the case the
     * migration playbook allows a shared, never closed arena for. The library keeps the pointer for
     * as long as it is loaded.
     */
    private static final Arena HOST_ARENA = Arena.ofShared();

    private static final MethodHandle ABI_VERSION = downcall(
            "drt_abi_version", FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle INIT = downcall(
            "drt_init", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle INIT_DRT = downcall(
            "drt_init_drt", FunctionDescriptor.ofVoid());
    private static final MethodHandle INIT_TEST = downcall(
            "drt_init_test", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle DID_CLEAR_WINDOW_OBJECT = downcall(
            "drt_did_clear_window_object",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_LONG));
    private static final MethodHandle DISPOSE = downcall(
            "drt_dispose", FunctionDescriptor.ofVoid());
    private static final MethodHandle DUMP_AS_TEXT = downcall(
            "drt_dump_as_text", FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle DUMP_CHILD_FRAMES_AS_TEXT = downcall(
            "drt_dump_child_frames_as_text", FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle DID_FINISH_LOAD = downcall(
            "drt_did_finish_load", FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle DUMP_BACK_FORWARD_LIST = downcall(
            "drt_dump_back_forward_list", FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle SHOULD_STAY_ON_PAGE = downcall(
            "drt_should_stay_on_page_after_handling_before_unload",
            FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle OPEN_PANEL_FILE_COUNT = downcall(
            "drt_open_panel_file_count", FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle OPEN_PANEL_FILE = downcall(
            "drt_open_panel_file",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));

    static {
        checkAbiVersion();
        installHost();
    }

    private DumpRenderTreeNative() {
    }

    // =====================================================================================
    // Downcalls
    // =====================================================================================

    static void initDRT() {
        try {
            INIT_DRT.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Starts one test. Both strings travel as UTF-16 where the JNI form read them with
     * {@code GetStringUTFChars}, i.e. modified UTF-8; the two agree on every path expressible in the
     * BMP and differ only for {@code U+0000} and supplementary characters in a test path.
     *
     * @param testPath the test path
     * @param pixelsHash the expected pixel hash
     */
    static void initTest(String testPath, String pixelsHash) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment path = WKJStringCodec.encode(arena, testPath);
            MemorySegment hash = WKJStringCodec.encode(arena, pixelsHash);
            try {
                INIT_TEST.invokeExact(path, WKJStringCodec.length(testPath), hash,
                        WKJStringCodec.length(pixelsHash));
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    /**
     * Installs the test objects on a freshly cleared window object. The {@link EventSender} is
     * registered only for the duration of the call: the library mints its own id from
     * {@code WKJDrtHostCore.retain} and releases that one when the JavaScript wrapper is finalized,
     * which is the {@code NewGlobalRef} / {@code DeleteGlobalRef} pair of the JNI form.
     *
     * @param pContext the {@code JSGlobalContextRef}
     * @param pWindowObject the {@code JSObjectRef} of the window object
     * @param eventSender the event sender
     */
    static void didClearWindowObject(long pContext, long pWindowObject, EventSender eventSender) {
        long ref = WebKitNative.register(eventSender);
        try {
            DID_CLEAR_WINDOW_OBJECT.invokeExact(pContext, pWindowObject, ref);
        } catch (Throwable t) {
            throw new AssertionError(t);
        } finally {
            WebKitNative.unregister(ref);
        }
    }

    static void dispose() {
        try {
            DISPOSE.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static boolean dumpAsText() {
        return flag(DUMP_AS_TEXT);
    }

    static boolean dumpChildFramesAsText() {
        return flag(DUMP_CHILD_FRAMES_AS_TEXT);
    }

    static boolean dumpBackForwardList() {
        return flag(DUMP_BACK_FORWARD_LIST);
    }

    static boolean didFinishLoad() {
        return flag(DID_FINISH_LOAD);
    }

    static boolean shouldStayOnPageAfterHandlingBeforeUnload() {
        return flag(SHOULD_STAY_ON_PAGE);
    }

    /**
     * The files the running test set with {@code testRunner.setOpenPanelFiles}. The count and the
     * entries are two calls, as the header prescribes; the count cannot change between them because
     * only JavaScript running in the test changes it and it is not running while Java reads.
     *
     * @return the file names, possibly empty
     */
    static String[] openPanelFiles() {
        int count;
        try {
            count = (int) OPEN_PANEL_FILE_COUNT.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        if (count <= 0) {
            return new String[0];
        }
        String[] files = new String[count];
        for (int i = 0; i < count; i++) {
            files[i] = openPanelFile(i);
        }
        return files;
    }

    private static String openPanelFile(int index) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment resultLength = arena.allocate(JAVA_INT);
            MemorySegment resultBuffer = arena.allocate(JAVA_CHAR, WKJStringCodec.CAPACITY);
            int status;
            try {
                status = (int) OPEN_PANEL_FILE.invokeExact(index, resultBuffer,
                        WKJStringCodec.CAPACITY, resultLength);
                if (status == WKJStringCodec.OVERFLOW) {
                    int required = resultLength.get(JAVA_INT, 0);
                    resultBuffer = arena.allocate(JAVA_CHAR, required);
                    status = (int) OPEN_PANEL_FILE.invokeExact(index, resultBuffer, required,
                            resultLength);
                }
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return WKJStringCodec.decode(status, resultBuffer, resultLength);
        }
    }

    private static boolean flag(MethodHandle handle) {
        try {
            return (int) handle.invokeExact() != 0;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    // =====================================================================================
    // Library binding and the host table
    // =====================================================================================

    @SuppressWarnings("restricted")
    private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
        MemorySegment symbol = LOOKUP.find(name).orElseThrow(() -> new UnsatisfiedLinkError(
                "DumpRenderTreeJava does not export " + name + ": the library on"
                        + " java.library.path is stale, rebuild it from this source tree"));
        return LINKER.downcallHandle(symbol, descriptor);
    }

    private static void checkAbiVersion() {
        int actual;
        try {
            actual = (int) ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        if (actual != DRT_ABI_VERSION) {
            throw new UnsatisfiedLinkError("DumpRenderTreeJava reports drt_* ABI version "
                    + Integer.toUnsignedString(actual) + ", but this harness requires ABI version "
                    + DRT_ABI_VERSION + ". Rebuild DumpRenderTreeJava from this source tree.");
        }
    }

    /*
     * Builds WKJDrtHost and installs it. The struct is one int32_t followed by three tables of
     * function pointers, so its offsets are computed from the pointer size rather than written down:
     * on a 64-bit ABI that reproduces the sizeof(WKJDrtHost) = 264 the C side reports.
     */
    private static void installHost() {
        long pointer = ADDRESS.byteSize();
        long coreOffset = pointer;
        long drtOffset = coreOffset + CORE_SLOTS * pointer;
        long eventSenderOffset = drtOffset + DRT_SLOTS * pointer;
        long hostSize = eventSenderOffset + EVENT_SENDER_SLOTS * pointer;

        MemorySegment host = HOST_ARENA.allocate(hostSize, pointer);
        host.set(JAVA_INT, 0, (int) hostSize);

        MemorySegment[] core = {
            stub("retain", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG)),
            stub("release", FunctionDescriptor.ofVoid(JAVA_LONG)),
        };
        MemorySegment[] drt = {
            stub("waitUntilDone", FunctionDescriptor.ofVoid()),
            stub("notifyDone", FunctionDescriptor.ofVoid()),
            stub("overridePreference", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS,
                    JAVA_INT)),
            stub("getBackForwardItemCount", FunctionDescriptor.of(JAVA_INT)),
            stub("clearBackForwardList", FunctionDescriptor.ofVoid()),
            stub("resolveURL", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT,
                    ADDRESS)),
            stub("loadURL", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT)),
            stub("goBackForward", FunctionDescriptor.ofVoid(JAVA_INT)),
        };
        MemorySegment[] eventSender = {
            stub("keyDown", FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT)),
            stub("mouseUpDown", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT)),
            stub("mouseMoveTo", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT)),
            stub("mouseScroll", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_FLOAT, JAVA_FLOAT,
                    JAVA_INT)),
            stub("leapForward", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT)),
            stub("contextClick", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("scheduleAsynchronousClick", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("touchStart", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("touchCancel", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("touchMove", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("touchEnd", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("addTouchPoint", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT)),
            stub("updateTouchPoint", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT,
                    JAVA_INT)),
            stub("cancelTouchPoint", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT)),
            stub("releaseTouchPoint", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT)),
            stub("clearTouchPoints", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("setTouchModifier", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT)),
            stub("scalePageBy", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_FLOAT, JAVA_INT,
                    JAVA_INT)),
            stub("zoom", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT)),
            stub("beginDragWithFiles", FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, ADDRESS,
                    JAVA_INT)),
            stub("getDragMode", FunctionDescriptor.of(JAVA_INT, JAVA_LONG)),
            stub("setDragMode", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT)),
        };
        fill(host, coreOffset, CORE_SLOTS, core);
        fill(host, drtOffset, DRT_SLOTS, drt);
        fill(host, eventSenderOffset, EVENT_SENDER_SLOTS, eventSender);

        int result;
        try {
            result = (int) INIT.invokeExact(host, (int) hostSize, DRT_ABI_VERSION);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        if (result != DRT_INIT_OK) {
            throw new UnsatisfiedLinkError("drt_init rejected the host table with code " + result
                    + "; DumpRenderTreeJava and this harness disagree about WKJDrtHost");
        }
    }

    private static void fill(MemorySegment host, long offset, int slots, MemorySegment[] stubs) {
        if (stubs.length != slots) {
            throw new AssertionError("a callback table of " + slots + " slots was given "
                    + stubs.length + " function pointers");
        }
        for (int i = 0; i < slots; i++) {
            host.set(ADDRESS, offset + (long) i * ADDRESS.byteSize(), stubs[i]);
        }
    }

    /*
     * Derives the upcall target's Java signature from the descriptor rather than restating it, so a
     * descriptor that does not match the method it names fails here, at class initialization, with
     * the method name in the message - instead of corrupting the stack at the first callback.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment stub(String name, FunctionDescriptor descriptor) {
        MethodHandle target;
        try {
            target = MethodHandles.lookup().findStatic(DumpRenderTreeNative.class, name,
                    descriptor.toMethodType());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("no upcall target " + name + descriptor.toMethodType(), e);
        }
        return LINKER.upcallStub(target, descriptor, HOST_ARENA);
    }

    // =====================================================================================
    // Upcall targets: WKJDrtHostCore
    // =====================================================================================

    static long retain(long ref) {
        try {
            return WebKitNative.retain(ref);
        } catch (Throwable t) {
            failed("retain", t);
            return 0L;
        }
    }

    static void release(long ref) {
        try {
            WebKitNative.release(ref);
        } catch (Throwable t) {
            failed("release", t);
        }
    }

    // =====================================================================================
    // Upcall targets: WKJDrtCallbacks
    // =====================================================================================

    static void waitUntilDone() {
        try {
            DumpRenderTree.waitUntilDone();
        } catch (Throwable t) {
            failed("wait_until_done", t);
        }
    }

    static void notifyDone() {
        try {
            DumpRenderTree.notifyDone();
        } catch (Throwable t) {
            failed("notify_done", t);
        }
    }

    static void overridePreference(MemorySegment key, int keyLength, MemorySegment value,
                                   int valueLength) {
        try {
            DumpRenderTree.overridePreference(readString(key, keyLength),
                    readString(value, valueLength));
        } catch (Throwable t) {
            failed("override_preference", t);
        }
    }

    static int getBackForwardItemCount() {
        try {
            return DumpRenderTree.getBackForwardItemCount();
        } catch (Throwable t) {
            failed("get_back_forward_item_count", t);
            return 0;
        }
    }

    static void clearBackForwardList() {
        try {
            DumpRenderTree.clearBackForwardList();
        } catch (Throwable t) {
            failed("clear_back_forward_list", t);
        }
    }

    static int resolveURL(MemorySegment relative, int relativeLength, MemorySegment resultBuffer,
                          int resultCapacity, MemorySegment resultLength) {
        try {
            String resolved = DumpRenderTree.resolveURL(readString(relative, relativeLength));
            return WKJStringCodec.emit(resolved,
                    outSegment(resultBuffer, (long) resultCapacity * Character.BYTES),
                    resultCapacity, outSegment(resultLength, Integer.BYTES));
        } catch (Throwable t) {
            failed("resolve_url", t);
            return WKJStringCodec.NULL;
        }
    }

    static void loadURL(MemorySegment url, int urlLength) {
        try {
            DumpRenderTree.loadURL(readString(url, urlLength));
        } catch (Throwable t) {
            failed("load_url", t);
        }
    }

    static void goBackForward(int howFar) {
        try {
            DumpRenderTree.goBackForward(howFar);
        } catch (Throwable t) {
            failed("go_back_forward", t);
        }
    }

    // =====================================================================================
    // Upcall targets: WKJEventSenderCallbacks
    // =====================================================================================

    static void keyDown(long user, MemorySegment key, int keyLength, int modifiers) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.keyDown(readString(key, keyLength), modifiers);
            }
        } catch (Throwable t) {
            failed("key_down", t);
        }
    }

    static void mouseUpDown(long user, int button, int modifiers) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.mouseUpDown(button, modifiers);
            }
        } catch (Throwable t) {
            failed("mouse_up_down", t);
        }
    }

    static void mouseMoveTo(long user, int x, int y) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.mouseMoveTo(x, y);
            }
        } catch (Throwable t) {
            failed("mouse_move_to", t);
        }
    }

    static void mouseScroll(long user, float dx, float dy, int continuous) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.mouseScroll(dx, dy, continuous != 0);
            }
        } catch (Throwable t) {
            failed("mouse_scroll", t);
        }
    }

    static void leapForward(long user, int timeOffset) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.leapForward(timeOffset);
            }
        } catch (Throwable t) {
            failed("leap_forward", t);
        }
    }

    static void contextClick(long user) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.contextClick();
            }
        } catch (Throwable t) {
            failed("context_click", t);
        }
    }

    static void scheduleAsynchronousClick(long user) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.scheduleAsynchronousClick();
            }
        } catch (Throwable t) {
            failed("schedule_asynchronous_click", t);
        }
    }

    static void touchStart(long user) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.touchStart();
            }
        } catch (Throwable t) {
            failed("touch_start", t);
        }
    }

    static void touchCancel(long user) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.touchCancel();
            }
        } catch (Throwable t) {
            failed("touch_cancel", t);
        }
    }

    static void touchMove(long user) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.touchMove();
            }
        } catch (Throwable t) {
            failed("touch_move", t);
        }
    }

    static void touchEnd(long user) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.touchEnd();
            }
        } catch (Throwable t) {
            failed("touch_end", t);
        }
    }

    static void addTouchPoint(long user, int x, int y) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.addTouchPoint(x, y);
            }
        } catch (Throwable t) {
            failed("add_touch_point", t);
        }
    }

    static void updateTouchPoint(long user, int index, int x, int y) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.updateTouchPoint(index, x, y);
            }
        } catch (Throwable t) {
            failed("update_touch_point", t);
        }
    }

    static void cancelTouchPoint(long user, int index) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.cancelTouchPoint(index);
            }
        } catch (Throwable t) {
            failed("cancel_touch_point", t);
        }
    }

    static void releaseTouchPoint(long user, int index) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.releaseTouchPoint(index);
            }
        } catch (Throwable t) {
            failed("release_touch_point", t);
        }
    }

    static void clearTouchPoints(long user) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.clearTouchPoints();
            }
        } catch (Throwable t) {
            failed("clear_touch_points", t);
        }
    }

    static void setTouchModifier(long user, int modifier, int set) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.setTouchModifier(modifier, set != 0);
            }
        } catch (Throwable t) {
            failed("set_touch_modifier", t);
        }
    }

    static void scalePageBy(long user, float scale, int x, int y) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.scalePageBy(scale, x, y);
            }
        } catch (Throwable t) {
            failed("scale_page_by", t);
        }
    }

    static void zoom(long user, int in, int textOnly) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.zoom(in != 0, textOnly != 0);
            }
        } catch (Throwable t) {
            failed("zoom", t);
        }
    }

    /**
     * {@code eventSender.beginDragWithFiles}. The array arrives flat, as {@code count} pointers and
     * {@code count} lengths, and a null element is a null pointer.
     *
     * @param user the registry id of the event sender
     * @param files the file name pointers
     * @param fileLengths their lengths in code units
     * @param count the element count, possibly zero
     */
    static void beginDragWithFiles(long user, MemorySegment files, MemorySegment fileLengths,
                                   int count) {
        try {
            EventSender sender = sender(user);
            if (sender == null) {
                return;
            }
            String[] names = new String[Math.max(count, 0)];
            if (names.length > 0 && files.address() != 0L && fileLengths.address() != 0L) {
                MemorySegment pointers =
                        outSegment(files, (long) names.length * ADDRESS.byteSize());
                MemorySegment lengths =
                        outSegment(fileLengths, (long) names.length * Integer.BYTES);
                for (int i = 0; i < names.length; i++) {
                    names[i] = readString(pointers.getAtIndex(ADDRESS, i),
                            lengths.getAtIndex(JAVA_INT, i));
                }
            }
            sender.beginDragWithFiles(names);
        } catch (Throwable t) {
            failed("begin_drag_with_files", t);
        }
    }

    static int getDragMode(long user) {
        try {
            EventSender sender = sender(user);
            return sender != null && sender.getDragMode() ? 1 : 0;
        } catch (Throwable t) {
            failed("get_drag_mode", t);
            return 0;
        }
    }

    static void setDragMode(long user, int mode) {
        try {
            EventSender sender = sender(user);
            if (sender != null) {
                sender.setDragMode(mode != 0);
            }
        } catch (Throwable t) {
            failed("set_drag_mode", t);
        }
    }

    // =====================================================================================
    // Upcall helpers
    // =====================================================================================

    private static EventSender sender(long ref) {
        return WebKitNative.lookup(ref) instanceof EventSender sender ? sender : null;
    }

    /*
     * An upcall target may not let a Throwable escape: an exception crossing the boundary terminates
     * the JVM. Every JNI upcall site in this harness cleared a pending Java exception and ignored
     * it, so logging and returning the documented default is what preserves behaviour.
     */
    /* See WebPageNative.failed: one place, so that check_and_clear_exception cannot miss one. */
    private static void failed(String slot, Throwable t) {
        WebKitNative.upcallFailed("DumpRenderTree callback " + slot, t);
    }

    @SuppressWarnings("restricted")
    private static String readString(MemorySegment s, int length) {
        if (s.address() == 0L) {
            return null;
        }
        if (length <= 0) {
            return "";
        }
        char[] chars = new char[length];
        MemorySegment.copy(s.reinterpret((long) length * Character.BYTES), JAVA_CHAR, 0L, chars, 0,
                length);
        return new String(chars);
    }

    @SuppressWarnings("restricted")
    private static MemorySegment outSegment(MemorySegment segment, long byteSize) {
        return segment.address() == 0L ? segment : segment.reinterpret(byteSize);
    }
}

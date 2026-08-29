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

package com.sun.webkit.dom;

import com.sun.webkit.WKJLayouts;
import com.sun.webkit.WKJStringCodec;
import com.sun.webkit.WebKitNative;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.MethodHandle;
import netscape.javascript.JSException;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the {@code wkj_js_*} functions of the {@code jfxwebkit} C ABI: the nine entry
 * points behind {@link JSObject} plus {@code wkj_frame_execute_script}, which is declared with them
 * because everything it does after finding the frame is LiveConnect.
 * <p>
 * The library no longer constructs Java objects. Each call describes its result in a
 * {@code WKJJSValue}, a tagged union of the nine kinds a JavaScript value can reach Java as, and
 * this class builds the {@link Integer}, {@link Double}, {@link Boolean}, {@link String},
 * {@link JSObject} or {@code NodeImpl} from it. The integer versus double split is load bearing
 * rather than an optimisation: the JNI code produced an {@code Integer} for a JavaScript value that
 * was an exact {@code int32} and a {@code Double} otherwise, and an application can tell the
 * difference.
 * <p>
 * <b>Strings.</b> A result string is copied into a buffer this class provides, like every other
 * string on this ABI. What is different here is the overflow path: calling {@code wkj_js_eval} or
 * {@code wkj_js_call} again to grow the buffer would run the script a second time, and even
 * {@code wkj_js_get_member} can run a user defined getter. So an oversized string comes back as a
 * retained {@code string_handle}, which {@code wkj_js_string_copy} drains and
 * {@code wkj_js_string_release} frees - an obligation this class always discharges in a
 * {@code finally}.
 * <p>
 * <b>Java objects.</b> A {@code wkj_ref} parameter is borrowed for the duration of the call, so an
 * argument that is neither a string, a boxed primitive nor a {@link JSObject} is registered before
 * the call and released after it. The library retains what it keeps, exactly as
 * {@code JobjectWrapper} turned the {@code jobject} it was handed into a global or weak global
 * reference. A {@code wkj_ref} coming the other way is a new strong id that this class owns, so it
 * is looked up and released once.
 * <p>
 * <b>Threading.</b> Everything here runs on the JavaFX application thread, which owns the
 * JavaScript context; {@link JSObject} asserts it with {@code Invoker.checkEventThread()} before
 * every call. {@code Linker.Option.critical(true)} is forbidden on every function of this header,
 * because all of them run JavaScript and several call back into Java while doing it.
 *
 * @see com.sun.webkit.WebKitNative
 */
public final class JSObjectNative {

    /*
     * WKJJSValue as webkit_java_api_bridge.h declares it, from the one place every struct of this
     * ABI is declared. Two int32_t, then a double which forces eight byte alignment, three eight
     * byte fields, the string pointer, and two trailing int32_t that fill the last eight bytes
     * exactly - so the struct is 56 bytes with no tail padding.
     *
     * The layout and the offsets below are package private rather than private because
     * LiveConnectNative fills the same struct from the other direction, for the describe_object
     * callback slot.
     */
    static final StructLayout JS_VALUE_LAYOUT = WKJLayouts.JS_VALUE;

    static final long OFFSET_KIND = offsetOf("kind");
    static final long OFFSET_PEER_TYPE = offsetOf("peer_type");
    static final long OFFSET_NUMBER = offsetOf("number");
    static final long OFFSET_PEER = offsetOf("peer");
    static final long OFFSET_STRING_HANDLE = offsetOf("string_handle");
    static final long OFFSET_OBJECT = offsetOf("object");
    static final long OFFSET_STRING = offsetOf("string");
    static final long OFFSET_STRING_CAP = offsetOf("string_cap");
    static final long OFFSET_STRING_LENGTH = offsetOf("string_length");

    /* WKJ_JS_KIND_*: the kind of a JavaScript value as it crosses to or from Java. */
    static final int KIND_NULL = 0;
    static final int KIND_UNDEFINED = 1;
    static final int KIND_BOOLEAN = 2;
    static final int KIND_INT = 3;
    static final int KIND_DOUBLE = 4;
    static final int KIND_STRING = 5;
    static final int KIND_JS_OBJECT = 6;
    static final int KIND_DOM_NODE = 7;
    static final int KIND_JAVA_OBJECT = 8;

    /* WKJ_JS_*: what an entry point returns. */
    private static final int JS_OK = 0;
    private static final int JS_EXCEPTION = 1;
    private static final int JS_NULL_ARGUMENT = 2;
    private static final int JS_NO_CONTEXT = 3;
    private static final int JS_INVALID_FUNCTION = 4;

    private static final MethodHandle EVAL = WebKitNative.downcall(
            "wkj_js_eval",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle GET_MEMBER = WebKitNative.downcall(
            "wkj_js_get_member",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle SET_MEMBER = WebKitNative.downcall(
            "wkj_js_set_member",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS,
                    JAVA_LONG, ADDRESS));
    private static final MethodHandle REMOVE_MEMBER = WebKitNative.downcall(
            "wkj_js_remove_member",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle GET_SLOT = WebKitNative.downcall(
            "wkj_js_get_slot",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS));
    private static final MethodHandle SET_SLOT = WebKitNative.downcall(
            "wkj_js_set_slot",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG));
    private static final MethodHandle TO_STRING = WebKitNative.downcall(
            "wkj_js_to_string",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS,
                    ADDRESS));
    private static final MethodHandle CALL = WebKitNative.downcall(
            "wkj_js_call",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS,
                    JAVA_INT, JAVA_LONG, ADDRESS));
    private static final MethodHandle UNPROTECT = WebKitNative.downcall(
            "wkj_js_unprotect",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle STRING_COPY = WebKitNative.downcall(
            "wkj_js_string_copy",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle STRING_RELEASE = WebKitNative.downcall(
            "wkj_js_string_release",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle FRAME_EXECUTE_SCRIPT = WebKitNative.downcall(
            "wkj_frame_execute_script",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));

    static {
        // wkj_live_connect_init must precede every wkj_js_* call, and every wkj_js_* call in this
        // module is a method of this class, so this is the one place that can guarantee the order.
        // It is at the end of the initializer, after the twelve handles above are bound, because
        // installing the table is the last thing the LiveConnect half of the ABI needs.
        LiveConnectNative.initResult();
    }

    private JSObjectNative() {
    }

    private static long offsetOf(String member) {
        return JS_VALUE_LAYOUT.byteOffset(PathElement.groupElement(member));
    }

    // =====================================================================================
    // The nine com.sun.webkit.dom.JSObject entry points
    // =====================================================================================

    static Object eval(long peer, int peerType, String script) {
        try (Arena arena = Arena.ofConfined()) {
            Result result = newResult(arena);
            MemorySegment text = WKJStringCodec.encode(arena, script);
            int status;
            try {
                status = (int) EVAL.invokeExact(peer, peerType, text,
                        WKJStringCodec.length(script), result.value);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return takeResult(status, result);
        }
    }

    static Object getMember(long peer, int peerType, String name) {
        try (Arena arena = Arena.ofConfined()) {
            Result result = newResult(arena);
            MemorySegment text = WKJStringCodec.encode(arena, name);
            int status;
            try {
                status = (int) GET_MEMBER.invokeExact(peer, peerType, text,
                        WKJStringCodec.length(name), result.value);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return takeResult(status, result);
        }
    }

    static void setMember(long peer, int peerType, String name, Object value, Object acc) {
        try (Arena arena = Arena.ofConfined()) {
            Result result = newResult(arena);
            MemorySegment text = WKJStringCodec.encode(arena, name);
            MemorySegment argument = arena.allocate(JS_VALUE_LAYOUT);
            long argumentRef = encode(arena, argument, value);
            long accRef = WebKitNative.register(acc);
            int status;
            try {
                status = (int) SET_MEMBER.invokeExact(peer, peerType, text,
                        WKJStringCodec.length(name), argument, accRef, result.value);
            } catch (Throwable t) {
                throw new AssertionError(t);
            } finally {
                WebKitNative.release(accRef);
                WebKitNative.release(argumentRef);
            }
            // A setter can throw: JSObjectSetProperty is the one writing call the JNI code passed an
            // exception out parameter to. The out value is written only for WKJ_JS_EXCEPTION, so it
            // is checked rather than decoded.
            checkStatus(status, result);
        }
    }

    static void removeMember(long peer, int peerType, String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment text = WKJStringCodec.encode(arena, name);
            int status;
            try {
                status = (int) REMOVE_MEMBER.invokeExact(peer, peerType, text,
                        WKJStringCodec.length(name));
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            checkStatus(status, null);
        }
    }

    static Object getSlot(long peer, int peerType, int index) {
        try (Arena arena = Arena.ofConfined()) {
            Result result = newResult(arena);
            int status;
            try {
                status = (int) GET_SLOT.invokeExact(peer, peerType, index, result.value);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return takeResult(status, result);
        }
    }

    static void setSlot(long peer, int peerType, int index, Object value, Object acc) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment argument = arena.allocate(JS_VALUE_LAYOUT);
            long argumentRef = encode(arena, argument, value);
            long accRef = WebKitNative.register(acc);
            int status;
            try {
                status = (int) SET_SLOT.invokeExact(peer, peerType, index, argument, accRef);
            } catch (Throwable t) {
                throw new AssertionError(t);
            } finally {
                WebKitNative.release(accRef);
                WebKitNative.release(argumentRef);
            }
            checkStatus(status, null);
        }
    }

    /**
     * {@code JSObject.toString()}. Being a plain string return this uses the contract 13 status
     * codes rather than the {@code WKJ_JS_*} ones, and it has never thrown: a peer that names no
     * live context answers {@code null}, which is what the JNI form returned there.
     *
     * @param peer the JavaScript peer
     * @param peerType the peer type
     * @return the string form, or {@code null}
     */
    static String stringValue(long peer, int peerType) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(JAVA_CHAR, WKJStringCodec.CAPACITY);
            MemorySegment length = arena.allocate(JAVA_INT);
            MemorySegment handle = arena.allocate(JAVA_LONG);
            int status;
            try {
                status = (int) TO_STRING.invokeExact(peer, peerType, buffer,
                        WKJStringCodec.CAPACITY, length, handle);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            if (status == WKJStringCodec.OVERFLOW) {
                // toString can run a user defined toString, so the call is never repeated: the
                // library has kept the value alive and the transfer finishes through the handle.
                return drain(handle.get(JAVA_LONG, 0), length.get(JAVA_INT, 0));
            }
            return WKJStringCodec.decode(status, buffer, length);
        }
    }

    static Object call(long peer, int peerType, String methodName, Object[] args, Object acc) {
        try (Arena arena = Arena.ofConfined()) {
            Result result = newResult(arena);
            MemorySegment name = WKJStringCodec.encode(arena, methodName);
            int count = args == null ? 0 : args.length;
            MemorySegment arguments = MemorySegment.NULL;
            long[] refs = new long[count];
            if (args != null) {
                arguments = arena.allocate(JS_VALUE_LAYOUT, Math.max(count, 1));
                for (int i = 0; i < count; i++) {
                    MemorySegment slot =
                            arguments.asSlice(i * JS_VALUE_LAYOUT.byteSize(), JS_VALUE_LAYOUT);
                    refs[i] = encode(arena, slot, args[i]);
                }
            }
            long accRef = WebKitNative.register(acc);
            int status;
            try {
                status = (int) CALL.invokeExact(peer, peerType, name,
                        WKJStringCodec.length(methodName), arguments, count, accRef, result.value);
            } catch (Throwable t) {
                throw new AssertionError(t);
            } finally {
                WebKitNative.release(accRef);
                for (long ref : refs) {
                    WebKitNative.release(ref);
                }
            }
            return takeResult(status, result);
        }
    }

    static void unprotect(long peer, int peerType) {
        try {
            UNPROTECT.invokeExact(peer, peerType);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Runs {@code script} in the global scope of a frame and describes the result, which is what
     * {@code WebPage.twkExecuteScript} did. It is here rather than in {@code WebPageNative} because
     * everything it does after finding the frame is LiveConnect: the same call the library makes for
     * {@code wkj_js_eval}, and a result that needs a {@code WKJJSValue}.
     *
     * @param pFrame the frame handle
     * @param script the script
     * @return the result value
     */
    public static Object executeScript(long pFrame, String script) {
        try (Arena arena = Arena.ofConfined()) {
            Result result = newResult(arena);
            MemorySegment text = WKJStringCodec.encode(arena, script);
            int status;
            try {
                status = (int) FRAME_EXECUTE_SCRIPT.invokeExact(pFrame, text,
                        WKJStringCodec.length(script), result.value);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return takeResult(status, result);
        }
    }

    // =====================================================================================
    // WKJJSValue marshalling
    // =====================================================================================

    /** One out parameter and the buffer a string result is copied into. */
    private record Result(MemorySegment value, MemorySegment buffer) {
    }

    private static Result newResult(Arena arena) {
        MemorySegment value = arena.allocate(JS_VALUE_LAYOUT);
        MemorySegment buffer = arena.allocate(JAVA_CHAR, WKJStringCodec.CAPACITY);
        value.set(ADDRESS, OFFSET_STRING, buffer);
        value.set(JAVA_INT, OFFSET_STRING_CAP, WKJStringCodec.CAPACITY);
        return new Result(value, buffer);
    }

    /**
     * Turns a completed call into its Java value, raising the same exception the JNI code raised for
     * each of the four failure statuses.
     */
    private static Object takeResult(int status, Result result) {
        if (status != JS_OK) {
            checkStatus(status, result);
            return null;
        }
        return decode(result);
    }

    private static void checkStatus(int status, Result result) {
        switch (status) {
            case JS_OK -> {
            }
            // The thrown JavaScript value is described in the out parameter, converted the way a
            // result would be, and JSObject.fwkMakeException builds the JSException from it - which
            // is what throwJavaException did with the same value.
            case JS_EXCEPTION ->
                    throw JSObject.fwkMakeException(result == null ? null : decode(result));
            // Both of these were throwNullPointerException, which used the no argument constructor.
            case JS_NULL_ARGUMENT, JS_NO_CONTEXT -> throw new NullPointerException();
            case JS_INVALID_FUNCTION -> throw new JSException("Invalid function reference");
            default -> throw new IllegalStateException(
                    "the C library returned JavaScript status " + status);
        }
    }

    private static Object decode(Result result) {
        MemorySegment value = result.value();
        int kind = value.get(JAVA_INT, OFFSET_KIND);
        return switch (kind) {
            case KIND_NULL -> null;
            case KIND_UNDEFINED -> JSObject.UNDEFINED;
            case KIND_BOOLEAN -> Boolean.valueOf(value.get(JAVA_DOUBLE, OFFSET_NUMBER) != 0.0);
            case KIND_INT -> Integer.valueOf((int) value.get(JAVA_DOUBLE, OFFSET_NUMBER));
            case KIND_DOUBLE -> Double.valueOf(value.get(JAVA_DOUBLE, OFFSET_NUMBER));
            case KIND_STRING -> decodeString(result);
            case KIND_JS_OBJECT -> new JSObject(value.get(JAVA_LONG, OFFSET_PEER),
                    value.get(JAVA_INT, OFFSET_PEER_TYPE));
            // The library has already ref()ed the node for Java, and NodeImpl's disposer drops that
            // reference - or getCachedImpl drops it at once on a cache hit. Neither side may add or
            // remove one here.
            case KIND_DOM_NODE -> NodeImpl.create(value.get(JAVA_LONG, OFFSET_PEER));
            case KIND_JAVA_OBJECT -> takeObject(value.get(JAVA_LONG, OFFSET_OBJECT));
            default -> throw new IllegalStateException(
                    "the C library described a JavaScript value of kind " + kind);
        };
    }

    /**
     * The id in a {@code JAVA_OBJECT} result is a new strong one that this side owns, so it is
     * resolved and released exactly once. The object itself is the original, not a copy.
     */
    private static Object takeObject(long ref) {
        try {
            return WebKitNative.lookup(ref);
        } finally {
            WebKitNative.release(ref);
        }
    }

    private static String decodeString(Result result) {
        MemorySegment value = result.value();
        long handle = value.get(JAVA_LONG, OFFSET_STRING_HANDLE);
        int length = value.get(JAVA_INT, OFFSET_STRING_LENGTH);
        if (handle != 0L) {
            return drain(handle, length);
        }
        if (length <= 0) {
            return "";
        }
        char[] chars = new char[length];
        MemorySegment.copy(result.buffer(), JAVA_CHAR, 0L, chars, 0, length);
        return new String(chars);
    }

    /**
     * Finishes the transfer of a string the library kept alive because it did not fit. A non zero
     * handle is an obligation to release, discharged here whatever happens.
     *
     * @param handle the retained string, may be zero
     * @param required the capacity the library asked for
     * @return the string, or {@code null} for a zero handle
     */
    private static String drain(long handle, int required) {
        if (handle == 0L) {
            return null;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment length = arena.allocate(JAVA_INT);
            MemorySegment buffer = arena.allocate(JAVA_CHAR, Math.max(required, 1));
            int status;
            try {
                status = (int) STRING_COPY.invokeExact(handle, buffer, Math.max(required, 1),
                        length);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return WKJStringCodec.decode(status, buffer, length);
        } finally {
            try {
                STRING_RELEASE.invokeExact(handle);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    /**
     * Describes one Java value for the library, in the order the {@code instanceof} chain of
     * {@code Java_Object_to_JSValue} used. Every {@link Number}, {@link Integer} included, becomes a
     * double, which is why there is deliberately no inbound integer kind.
     *
     * @param arena the arena the string characters are allocated from
     * @param slot the {@code WKJJSValue} to fill, already zeroed
     * @param value the value
     * @return a registry id the caller must release after the call, or zero
     */
    private static long encode(Arena arena, MemorySegment slot, Object value) {
        int kind = kindOf(value);
        slot.set(JAVA_INT, OFFSET_KIND, kind);
        switch (kind) {
            case KIND_NULL -> {
                return 0L;
            }
            case KIND_JS_OBJECT -> {
                // Java reads its own two fields, so the library needs neither their ids nor their
                // names.
                JSObject js = (JSObject) value;
                slot.set(JAVA_LONG, OFFSET_PEER, js.getPeer());
                slot.set(JAVA_INT, OFFSET_PEER_TYPE, js.getPeerType());
                return 0L;
            }
            case KIND_STRING -> {
                String s = (String) value;
                slot.set(ADDRESS, OFFSET_STRING, WKJStringCodec.encode(arena, s));
                slot.set(JAVA_INT, OFFSET_STRING_LENGTH, s.length());
                return 0L;
            }
            case KIND_BOOLEAN -> {
                slot.set(JAVA_DOUBLE, OFFSET_NUMBER, ((Boolean) value) ? 1.0 : 0.0);
                return 0L;
            }
            case KIND_DOUBLE -> {
                slot.set(JAVA_DOUBLE, OFFSET_NUMBER, ((Number) value).doubleValue());
                return 0L;
            }
            default -> {
                // Borrowed for the duration of the call; the library retains it if it keeps it,
                // exactly as JobjectWrapper turned the jobject it was handed into a global or weak
                // global reference.
                long ref = WebKitNative.register(value);
                slot.set(JAVA_LONG, OFFSET_OBJECT, ref);
                return ref;
            }
        }
    }

    /**
     * Classifies one Java value as the {@code WKJ_JS_KIND_} it crosses the boundary as, in the order
     * the {@code instanceof} chain of {@code Java_Object_to_JSValue} used.
     * <p>
     * There is one implementation and two callers - {@link #encode} for an argument and
     * {@code LiveConnectNative.describeObject} for a field value or an exposed object - which is
     * what the C header requires, so that a value cannot be classified one way as an argument and
     * another way as a field value. Every {@link Number}, {@link Integer} included, is a double,
     * which is why there is deliberately no inbound integer kind.
     *
     * @param value the value, may be {@code null}
     * @return the {@code WKJ_JS_KIND_} code
     */
    static int kindOf(Object value) {
        if (value == null) {
            return KIND_NULL;
        }
        if (value instanceof JSObject) {
            return KIND_JS_OBJECT;
        }
        if (value instanceof String) {
            return KIND_STRING;
        }
        if (value instanceof Boolean) {
            return KIND_BOOLEAN;
        }
        if (value instanceof Number) {
            return KIND_DOUBLE;
        }
        return KIND_JAVA_OBJECT;
    }
}

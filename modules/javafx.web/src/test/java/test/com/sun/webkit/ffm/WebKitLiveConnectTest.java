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

package test.com.sun.webkit.ffm;

import com.sun.webkit.WebKitNativeShim;
import com.sun.webkit.WkjStubShim;
import com.sun.webkit.dom.LiveConnectShim;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code WKJLiveConnectHost}: the reflective half of LiveConnect, which is what
 * {@code Source/WebCore/bridge/jni} calls when page script touches a Java object.
 * <p>
 * {@code JSObjectNative} takes a {@code JSObject} into JavaScript and was already bound. This is the
 * way back, and until it was installed there was none: {@code JavaClassJSC} could not enumerate a
 * class, {@code JavaMethodJSC} could not invoke one and {@code JavaFieldJSC} could not read a field,
 * so exposing a Java object to script produced an object with no members.
 * <p>
 * Every slot is called the way the library calls it, through the function pointer in the installed
 * table, so a {@link java.lang.foreign.FunctionDescriptor} that disagrees with the C prototype shows
 * up here rather than as a corrupted stack in a browser.
 */
@Tag("ffm")
public class WebKitLiveConnectTest {

    /** {@code sizeof(WKJLiveConnectHost)}: an int32_t, four bytes of padding and 26 pointers. */
    private static final long HOST_SIZE = 216L;

    /** {@code WKJ_INIT_OK}. */
    private static final int INIT_OK = 0;

    /** {@code WKJ_JT_*}, from {@code webkit_java_api_bridge.h}. */
    private static final int JT_OBJECT = 2;
    private static final int JT_INT = 7;
    private static final int JT_DOUBLE = 10;

    /** {@code WKJ_JS_KIND_*}, from the same header. */
    private static final int KIND_NULL = 0;
    private static final int KIND_STRING = 5;
    private static final int KIND_DOUBLE = 4;
    private static final int KIND_JAVA_OBJECT = 8;

    /** {@code WKJ_STR_OK}, {@code WKJ_STR_NULL}. */
    private static final int STR_OK = 0;
    private static final int STR_NULL = 1;

    private static long javaValueSize;
    private static long offsetType;
    private static long offsetI;
    private static long offsetD;
    private static long offsetL;

    private static long jsValueSize;
    private static long offsetKind;
    private static long offsetNumber;
    private static long offsetObject;
    private static long offsetString;
    private static long offsetStringCap;
    private static long offsetStringLength;

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
        javaValueSize = WkjStubShim.sizeOf("WKJJavaValue");
        offsetType = WkjStubShim.offsetOf("WKJJavaValue", "type");
        offsetI = WkjStubShim.offsetOf("WKJJavaValue", "i");
        offsetD = WkjStubShim.offsetOf("WKJJavaValue", "d");
        offsetL = WkjStubShim.offsetOf("WKJJavaValue", "l");

        jsValueSize = WkjStubShim.sizeOf("WKJJSValue");
        offsetKind = WkjStubShim.offsetOf("WKJJSValue", "kind");
        offsetNumber = WkjStubShim.offsetOf("WKJJSValue", "number");
        offsetObject = WkjStubShim.offsetOf("WKJJSValue", "object");
        offsetString = WkjStubShim.offsetOf("WKJJSValue", "string");
        offsetStringCap = WkjStubShim.offsetOf("WKJJSValue", "string_cap");
        offsetStringLength = WkjStubShim.offsetOf("WKJJSValue", "string_length");
    }

    // -------------------------------------------------------------------- the installation

    @Test
    public void theTableIsInstalledWithTheSizeTheLibraryExpects() {
        assertEquals(INIT_OK, LiveConnectShim.initResult(),
                "wkj_live_connect_init rejected the table, so the reflective half of LiveConnect"
                        + " has no path back to Java");
        assertEquals(WkjStubShim.sizeOf("WKJLiveConnectHost"), LiveConnectShim.byteSize(),
                "the Java WKJLiveConnectHost layout disagrees with the C sizeof");
        assertEquals(HOST_SIZE, LiveConnectShim.byteSize(),
                "sizeof(WKJLiveConnectHost) is no longer 216; the ABI version must be bumped");
        assertEquals(HOST_SIZE, LiveConnectShim.sizeField(),
                "the table must declare its own size, which is what wkj_live_connect_init checks"
                        + " against its host_size argument");
    }

    @Test
    public void theInitCallCarriedTheSizeAndTheAbiVersion() {
        LiveConnectShim.initResult();
        int call = WkjStubShim.findCall("wkj_live_connect_init", 0);
        assumeTrue(call >= 0, "the call ring no longer holds the wkj_live_connect_init call");
        assertEquals(HOST_SIZE, WkjStubShim.callArgBits(call, 1),
                "the host_size argument must be sizeof(WKJLiveConnectHost)");
        assertEquals(WebKitNativeShim.abiVersionExpected(), WkjStubShim.callArgBits(call, 2));
        assertNotEquals(0L, WkjStubShim.callArgBits(call, 0), "the table pointer was NULL");
    }

    /**
     * All 26 slots carry a stub. There is no exemption list here, unlike {@code WKJHost}: every one
     * of these has a Java target, and a NULL slot would silently disable one reflective operation -
     * a method that cannot be found, or a field that always reads as invalid.
     */
    @Test
    public void everySlotIsFilled() {
        List<String> names = LiveConnectShim.slotNames();
        assertEquals(26, names.size(), "the C header no longer declares 26 callback slots");
        List<String> unfilled = new ArrayList<>();
        for (String name : names) {
            if (LiveConnectShim.slotPointer(name) == 0L) {
                unfilled.add(name);
            }
        }
        assertTrue(unfilled.isEmpty(), "these LiveConnect slots are NULL: " + unfilled);
    }

    /** Every slot is where the C compiler put it, member by member. */
    @Test
    public void everySlotIsWhereTheCCompilerPutIt() {
        int struct = WkjStubShim.findStruct("WKJLiveConnectHost");
        assertNotEquals(-1, struct, "the library declares no WKJLiveConnectHost");
        for (int i = 0, n = WkjStubShim.structFieldCount(struct); i < n; i++) {
            String field = WkjStubShim.structFieldName(struct, i);
            assertEquals(WkjStubShim.structFieldOffset(struct, i), LiveConnectShim.offsetOf(field),
                    "offsetof(WKJLiveConnectHost, " + field + ") disagrees");
        }
    }

    // ------------------------------------------------------------------------- reflection

    @Test
    public void aClassIsReachedAndNamed() {
        long text = WebKitNativeShim.register("hello");
        try (Arena arena = Arena.ofConfined()) {
            long cls = call("object_get_class", "ll", text);
            assertNotEquals(0L, cls);
            assertSame(String.class, WebKitNativeShim.lookup(cls));
            assertEquals("java.lang.String", string("class_get_name", "ilpip", arena, cls));
            assertEquals(0L, call("class_is_array", "il", cls));

            long array = WebKitNativeShim.register(new int[] { 1 });
            long arrayClass = call("object_get_class", "ll", array);
            assertEquals(1L, call("class_is_array", "il", arrayClass));
            assertEquals("[I", string("class_get_name", "ilpip", arena, arrayClass));
        } finally {
            WebKitNativeShim.unregister(text);
        }
    }

    /**
     * The stand-in {@code JavaClass} builds when the object it was asked about has already been
     * collected, so that the rest of its constructor has something to reflect on.
     */
    @Test
    public void aDummyObjectIsAPlainObject() {
        long dummy = call("create_dummy_object", "l");
        assertNotEquals(0L, dummy);
        Object value = WebKitNativeShim.lookup(dummy);
        assertNotNull(value);
        assertSame(Object.class, value.getClass());
    }

    /**
     * The search {@code resolve_method} performs has to reach the same {@code Method} JNI would
     * have, because {@code Utilities.fwkInvokeWithContext} decides whether the call is permitted
     * from {@code method.getDeclaringClass()}. Matching the return type as well as the parameters is
     * what {@link #aCovariantOverrideResolvesToTheDeclaredReturnType} is about.
     */
    @Test
    public void aMethodIsResolvedAndDescribed() {
        long target = WebKitNativeShim.register(new Greeter());
        try (Arena arena = Arena.ofConfined()) {
            long method = resolve(arena, target, "greet", "(I)Ljava/lang/String;");
            assertNotEquals(0L, method, "greet(int) was not found");
            assertEquals("greet", string("method_get_name", "ilpip", arena, method));
            assertEquals("java.lang.String",
                    string("method_get_return_type_name", "ilpip", arena, method));
            assertEquals(1L, call("method_get_parameter_count", "il", method));
            assertEquals("int", stringAt("method_get_parameter_type_name", "ilipip", arena, method,
                    0));
            assertTrue(Modifier.isPublic((int) call("method_get_modifiers", "il", method)));

            assertEquals(0L, resolve(arena, target, "greet", "()V"),
                    "a descriptor that matches no overload must answer 0, not the wrong method");
            assertEquals(0L, resolve(arena, target, "noSuchMethod", "()V"));
        } finally {
            WebKitNativeShim.unregister(target);
        }
    }

    /**
     * A covariant override gives a class two methods with one name and one parameter list - the real
     * one and the compiler's bridge. Matching the return type as well is what keeps them apart, and
     * picking the bridge would change what {@code invoke} returns.
     */
    @Test
    public void aCovariantOverrideResolvesToTheDeclaredReturnType() {
        long target = WebKitNativeShim.register(new NarrowFactory());
        try (Arena arena = Arena.ofConfined()) {
            long narrow = resolve(arena, target, "make", "()Ljava/lang/String;");
            assertNotEquals(0L, narrow);
            assertEquals("java.lang.String",
                    string("method_get_return_type_name", "ilpip", arena, narrow));

            long bridge = resolve(arena, target, "make", "()Ljava/lang/Object;");
            assertNotEquals(0L, bridge, "the bridge method is also in getMethods() and is reachable");
            assertEquals("java.lang.Object",
                    string("method_get_return_type_name", "ilpip", arena, bridge));
            assertNotEquals(narrow, bridge, "the two must not resolve to one Method");
        } finally {
            WebKitNativeShim.unregister(target);
        }
    }

    @Test
    public void aMethodIsInvokedThroughTheUtilitiesAllowList() {
        Greeter greeter = new Greeter();
        long target = WebKitNativeShim.register(greeter);
        try (Arena arena = Arena.ofConfined()) {
            long method = resolve(arena, target, "greet", "(I)Ljava/lang/String;");
            long argument = WebKitNativeShim.register(Integer.valueOf(7));
            MemorySegment args = arena.allocateFrom(JAVA_LONG, argument);
            MemorySegment exception = arena.allocate(JAVA_LONG);

            long result = call("invoke", "lllpilp", method, target, args.address(), 1L, 0L,
                    exception.address());
            assertEquals(0L, exception.get(JAVA_LONG, 0L), "nothing was thrown");
            assertEquals("hello 7", WebKitNativeShim.lookup(result));
            WebKitNativeShim.unregister(result);
            WebKitNativeShim.unregister(argument);
        } finally {
            WebKitNativeShim.unregister(target);
        }
    }

    /**
     * A Java method that throws is a normal LiveConnect outcome, not a failed callback: the
     * {@code Throwable} comes back through {@code out_exception} and the caller wraps it in a
     * {@code JavaInstance} and throws it into JavaScript. Letting it escape the upcall would
     * terminate the JVM, and reporting it through {@code check_and_clear_exception} would make the
     * next unrelated call look like it failed.
     */
    @Test
    public void aThrownExceptionComesBackThroughTheOutParameter() {
        long target = WebKitNativeShim.register(new Greeter());
        WebKitNativeShim.checkAndClearUpcallFailure();
        try (Arena arena = Arena.ofConfined()) {
            long method = resolve(arena, target, "explode", "()V");
            MemorySegment exception = arena.allocate(JAVA_LONG);
            long result = call("invoke", "lllpilp", method, target, 0L, 0L, 0L,
                    exception.address());
            assertEquals(0L, result, "a throwing invocation has no result");
            long thrown = exception.get(JAVA_LONG, 0L);
            assertNotEquals(0L, thrown, "the Throwable must reach the library as an id");
            assertTrue(WebKitNativeShim.lookup(thrown) instanceof IllegalStateException);
            assertEquals("boom", ((Throwable) WebKitNativeShim.lookup(thrown)).getMessage());
            WebKitNativeShim.unregister(thrown);
            assertEquals(0, WebKitNativeShim.checkAndClearUpcallFailure(),
                    "a Java method that throws is not a failed upcall");
        } finally {
            WebKitNativeShim.unregister(target);
        }
    }

    // ----------------------------------------------------------------------------- fields

    @Test
    public void aFieldIsReadWrittenAndDescribed() throws Exception {
        Greeter greeter = new Greeter();
        long target = WebKitNativeShim.register(greeter);
        long field = WebKitNativeShim.register(Greeter.class.getField("name"));
        try (Arena arena = Arena.ofConfined()) {
            assertEquals("name", string("field_get_name", "ilpip", arena, field));
            assertEquals("java.lang.String", string("field_get_type_name", "ilpip", arena, field));

            MemorySegment value = arena.allocate(javaValueSize);
            assertEquals(1L, call("field_get", "illip", field, target, JT_OBJECT, value.address()));
            assertEquals(JT_OBJECT, value.get(JAVA_INT, offsetType));
            long read = value.get(JAVA_LONG, offsetL);
            assertEquals("world", WebKitNativeShim.lookup(read));
            WebKitNativeShim.unregister(read);

            long replacement = WebKitNativeShim.register("moon");
            value.set(JAVA_INT, offsetType, JT_OBJECT);
            value.set(JAVA_LONG, offsetL, replacement);
            assertEquals(1L, call("field_set", "illip", field, target, JT_OBJECT, value.address()));
            assertEquals("moon", greeter.name);
            WebKitNativeShim.unregister(replacement);

            long count = WebKitNativeShim.register(Greeter.class.getField("count"));
            value.set(JAVA_INT, offsetI, 0);
            assertEquals(1L, call("field_get", "illip", count, target, JT_INT, value.address()));
            assertEquals(JT_INT, value.get(JAVA_INT, offsetType));
            assertEquals(3, value.get(JAVA_INT, offsetI));

            value.set(JAVA_INT, offsetType, JT_INT);
            value.set(JAVA_INT, offsetI, 11);
            assertEquals(1L, call("field_set", "illip", count, target, JT_INT, value.address()));
            assertEquals(11, greeter.count);
            WebKitNativeShim.unregister(count);
        } finally {
            WebKitNativeShim.unregister(field);
            WebKitNativeShim.unregister(target);
        }
    }

    // ----------------------------------------------------------------------------- arrays

    @Test
    public void anArrayIsMeasuredReadAndWritten() {
        int[] numbers = { 1, 2, 3 };
        long array = WebKitNativeShim.register(numbers);
        try (Arena arena = Arena.ofConfined()) {
            assertEquals(3L, call("array_length", "il", array));

            MemorySegment value = arena.allocate(javaValueSize);
            assertEquals(1L, call("array_get", "iliip", array, 1, JT_INT, value.address()));
            assertEquals(2, value.get(JAVA_INT, offsetI));

            value.set(JAVA_INT, offsetType, JT_INT);
            value.set(JAVA_INT, offsetI, 42);
            assertEquals(1L, call("array_set", "iliip", array, 1, JT_INT, value.address()));
            assertArrayEquals(new int[] { 1, 42, 3 }, numbers);

            assertEquals(0L, call("array_get", "iliip", array, 9, JT_INT, value.address()),
                    "an out of range index is 0, which the JNI code left to the JVM to report");
        } finally {
            WebKitNativeShim.unregister(array);
        }
    }

    // -------------------------------------------------------------- boxing and strings

    @Test
    public void aPrimitiveIsBoxedAndUnboxed() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment value = arena.allocate(javaValueSize);
            value.set(JAVA_INT, offsetType, JT_INT);
            value.set(JAVA_INT, offsetI, 42);
            long boxed = call("box", "lp", value.address());
            assertEquals(Integer.valueOf(42), WebKitNativeShim.lookup(boxed));

            MemorySegment out = arena.allocate(javaValueSize);
            assertEquals(1L, call("unbox", "ilip", boxed, JT_INT, out.address()));
            assertEquals(42, out.get(JAVA_INT, offsetI));

            value.set(JAVA_INT, offsetType, JT_DOUBLE);
            value.set(JAVA_DOUBLE, offsetD, 2.5);
            long boxedDouble = call("box", "lp", value.address());
            assertEquals(Double.valueOf(2.5), WebKitNativeShim.lookup(boxedDouble));
            assertEquals(1L, call("unbox", "ilip", boxedDouble, JT_DOUBLE, out.address()));
            assertEquals(2.5, out.get(JAVA_DOUBLE, offsetD));

            assertEquals(0L, call("unbox", "ilip", WebKitNativeShim.register("not a number"),
                    JT_INT, out.address()), "unboxing a String must fail rather than throw");

            WebKitNativeShim.unregister(boxed);
            WebKitNativeShim.unregister(boxedDouble);
        }
    }

    @Test
    public void aStringCrossesInBothDirections() {
        try (Arena arena = Arena.ofConfined()) {
            String text = "café 😀";
            MemorySegment chars = arena.allocateFrom(JAVA_CHAR, text.toCharArray());
            long boxed = call("box_string", "lpi", chars.address(), text.length());
            assertEquals(text, WebKitNativeShim.lookup(boxed));
            assertEquals(text, string("string_value", "ilpip", arena, boxed));

            assertEquals(0L, call("box_string", "lpi", 0L, 0L),
                    "a NULL pointer is a null String and answers 0");
            assertEquals(STR_NULL, stringStatus("string_value", "ilpip", arena,
                    WebKitNativeShim.register(Integer.valueOf(1))),
                    "a value that is not a String is WKJ_STR_NULL");
            WebKitNativeShim.unregister(boxed);
        }
    }

    // -------------------------------------------------------------------------- describe

    @Test
    public void anObjectIsDescribedByTheSameRulesAsAnArgument() {
        try (Arena arena = Arena.ofConfined()) {
            assertEquals(KIND_NULL, describe(arena, 0L).kind(), "id 0 is JavaScript null");

            long text = WebKitNativeShim.register("hello");
            Described string = describe(arena, text);
            assertEquals(KIND_STRING, string.kind());
            assertEquals("hello", string.text());

            long number = WebKitNativeShim.register(Integer.valueOf(5));
            Described described = describe(arena, number);
            assertEquals(KIND_DOUBLE, described.kind(),
                    "every Number is a double inbound, Integer included, which is why the ABI has"
                            + " no inbound integer kind");
            assertEquals(5.0, described.number());

            Greeter greeter = new Greeter();
            long plain = WebKitNativeShim.register(greeter);
            Described object = describe(arena, plain);
            assertEquals(KIND_JAVA_OBJECT, object.kind());
            assertNotEquals(plain, object.object(),
                    "out->object is a NEW strong id which the library releases when it is done");
            assertSame(greeter, WebKitNativeShim.lookup(object.object()));
            WebKitNativeShim.unregister(object.object());

            WebKitNativeShim.unregister(text);
            WebKitNativeShim.unregister(number);
            WebKitNativeShim.unregister(plain);
        }
    }

    /**
     * A described string that does not fit answers {@code WKJ_STR_OVERFLOW} with the capacity
     * required and no side effect, so the library grows its buffer and asks again.
     */
    @Test
    public void anOversizedDescribedStringOverflowsRatherThanTruncating() {
        String text = "0123456789";
        long id = WebKitNativeShim.register(text);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment value = arena.allocate(jsValueSize);
            MemorySegment buffer = arena.allocate(JAVA_CHAR, 4);
            value.set(ADDRESS, offsetString, buffer);
            value.set(JAVA_INT, offsetStringCap, 4);
            assertEquals(2L, call("describe_object", "ilp", id, value.address()),
                    "WKJ_STR_OVERFLOW is 2");
            assertEquals(text.length(), value.get(JAVA_INT, offsetStringLength),
                    "the length must be the capacity required");

            MemorySegment big = arena.allocate(JAVA_CHAR, 32);
            value.set(ADDRESS, offsetString, big);
            value.set(JAVA_INT, offsetStringCap, 32);
            assertEquals(STR_OK, call("describe_object", "ilp", id, value.address()),
                    "describing an object twice has no side effects");
            assertEquals(text, new String(big.toArray(JAVA_CHAR), 0,
                    value.get(JAVA_INT, offsetStringLength)));
        } finally {
            WebKitNativeShim.unregister(id);
        }
    }

    // ------------------------------------------------- the three objects with no generic form

    /**
     * {@code JSObject.UNDEFINED} is created as {@code new String("undefined")} precisely so that it
     * is not equal by reference to any other {@code "undefined"}, and callers compare against it
     * with {@code ==}. The slot therefore has to answer that one object every time.
     */
    @Test
    public void theUndefinedSingletonIsAlwaysTheSameObject() {
        long first = call("undefined_object", "l");
        long second = call("undefined_object", "l");
        assertNotEquals(0L, first);
        Object value = WebKitNativeShim.lookup(first);
        assertEquals("undefined", value);
        assertSame(value, WebKitNativeShim.lookup(second),
                "the identity of the singleton is the whole point of the slot");
        WebKitNativeShim.unregister(first);
        WebKitNativeShim.unregister(second);
    }

    /**
     * A DOM node peer of 0 is not a node, and {@code NodeImpl.getCachedImpl} answers null for it, so
     * the slot answers 0 rather than minting an id for nothing.
     */
    @Test
    public void aNullNodePeerIsNotCached() {
        assertEquals(0L, call("node_get_cached_impl", "ll", 0L));
    }

    // ------------------------------------------------------------------------- plumbing

    private static long call(String slot, String signature, long... args) {
        long address = LiveConnectShim.slotPointer(slot);
        assertNotEquals(0L, address, "the LiveConnect slot " + slot + " is NULL");
        return WkjStubShim.callSlot(address, signature, args);
    }

    private static long resolve(Arena arena, long target, String name, String descriptor) {
        MemorySegment methodName = arena.allocateFrom(JAVA_CHAR, name.toCharArray());
        MemorySegment signature = arena.allocateFrom(JAVA_CHAR, descriptor.toCharArray());
        return call("resolve_method", "llpipi", target, methodName.address(), name.length(),
                signature.address(), descriptor.length());
    }

    /** Calls a slot whose last three parameters are the contract 13 out-buffer triple. */
    private static String string(String slot, String signature, Arena arena, long... leading) {
        MemorySegment buffer = arena.allocate(JAVA_CHAR, 256);
        MemorySegment length = arena.allocate(JAVA_INT);
        long[] args = args(leading, buffer.address(), 256L, length.address());
        assertEquals(STR_OK, call(slot, signature, args), slot + " did not answer a string");
        return new String(buffer.toArray(JAVA_CHAR), 0, length.get(JAVA_INT, 0L));
    }

    private static String stringAt(String slot, String signature, Arena arena, long target,
                                   long index) {
        return string(slot, signature, arena, target, index);
    }

    private static int stringStatus(String slot, String signature, Arena arena, long... leading) {
        MemorySegment buffer = arena.allocate(JAVA_CHAR, 256);
        MemorySegment length = arena.allocate(JAVA_INT);
        return (int) call(slot, signature, args(leading, buffer.address(), 256L,
                length.address()));
    }

    private static long[] args(long[] leading, long... trailing) {
        long[] all = new long[leading.length + trailing.length];
        System.arraycopy(leading, 0, all, 0, leading.length);
        System.arraycopy(trailing, 0, all, leading.length, trailing.length);
        return all;
    }

    /** What {@code describe_object} wrote, in the fields this test reads. */
    private record Described(int kind, double number, long object, String text) {
    }

    private static Described describe(Arena arena, long id) {
        MemorySegment value = arena.allocate(jsValueSize);
        MemorySegment buffer = arena.allocate(JAVA_CHAR, 256);
        value.set(ADDRESS, offsetString, buffer);
        value.set(JAVA_INT, offsetStringCap, 256);
        assertEquals(STR_OK, call("describe_object", "ilp", id, value.address()));
        int kind = value.get(JAVA_INT, offsetKind);
        String text = kind == KIND_STRING
                ? new String(buffer.toArray(JAVA_CHAR), 0, value.get(JAVA_INT, offsetStringLength))
                : null;
        return new Described(kind, value.get(JAVA_DOUBLE, offsetNumber),
                value.get(JAVA_LONG, offsetObject), text);
    }

    /** A plain Java object of the kind an application exposes to page script. */
    public static class Greeter {

        public String name = "world";

        public int count = 3;

        public String greet(int times) {
            return "hello " + times;
        }

        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    /** A covariant override, so that one name and one parameter list give two Methods. */
    public static class WideFactory {

        public Object make() {
            return "wide";
        }
    }

    /** The narrowing subclass; {@code getMethods()} answers both {@code make}s for it. */
    public static class NarrowFactory extends WideFactory {

        @Override
        public String make() {
            return "narrow";
        }
    }
}

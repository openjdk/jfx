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

package com.sun.webkit;

import com.sun.glass.utils.NativeLibLoader;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The driver for the {@code wkjstub_*} query ABI of the {@code wkjstub} recording library
 * ({@code modules/javafx.web/src/test/native/wkjstub}), and the only place in test facing code that
 * touches {@link Linker}, {@link SymbolLookup} or {@code MemorySegment.reinterpret}. It is compiled
 * into {@code javafx.web}, which the surefire argLine covers with {@code --enable-native-access};
 * the tests themselves run on the class path in the unnamed module, where a restricted call is not
 * permitted.
 * <p>
 * Every method here is a question about calls the facades made, never a substitute for making one: a
 * test drives a real downcall through a real facade and then asks this class what the library saw.
 * See {@code modules/javafx.web/FFM-TEST-PLAN.md} sections 2.5 and 4.0.
 */
public final class WkjStubShim {

    /** The library name. Deliberately not {@code jfxwebkit}, so the two can never be confused. */
    private static final String LIBRARY_NAME = "wkjstub";

    private static final Linker LINKER = Linker.nativeLinker();

    private static final ConcurrentMap<String, MethodHandle> HANDLES = new ConcurrentHashMap<>();

    /** Keeps the confined arenas of {@link #allocateConfinedSegment} alive for the run. */
    private static final List<Arena> RETAINED = new ArrayList<>();

    private static final ThreadLocal<long[]> LAST_FIRE = ThreadLocal.withInitial(() -> new long[1]);

    private static SymbolLookup lookup;
    private static boolean loaded;
    private static boolean attempted;
    private static String loadFailure;

    private WkjStubShim() {
    }

    /**
     * Loads {@code wkjstub}, once. A failure is recorded rather than thrown, so that a suite can turn
     * it into a JUnit assumption and skip instead of erroring when {@code -DskipNative=true} left no
     * stub built.
     *
     * @return true if the library is loaded and exports the query ABI
     */
    public static synchronized boolean load() {
        if (attempted) {
            return loaded;
        }
        attempted = true;
        try {
            NativeLibLoader.loadLibrary(LIBRARY_NAME);
            lookup = SymbolLookup.loaderLookup();
            if (lookup.find("wkjstub_stub_version").isEmpty()) {
                loadFailure = LIBRARY_NAME + " loaded but exports no wkjstub_* query ABI";
                return false;
            }
            loaded = true;
        } catch (Throwable t) {
            loadFailure = LIBRARY_NAME + " is not available on java.library.path (" + t
                    + "); build it with mvn -pl modules/javafx.web test, without -DskipNative=true";
        }
        return loaded;
    }

    /**
     * Returns why {@link #load} failed.
     *
     * @return the failure text, or a note that the library did load
     */
    public static synchronized String loadFailure() {
        return loadFailure == null ? LIBRARY_NAME + " loaded" : loadFailure;
    }

    /**
     * Reports whether the libraries loaded by this class loader export a symbol. The symbol
     * resolution test needs this without binding a handle.
     *
     * @param name the symbol name
     * @return true if the symbol is exported
     */
    public static boolean exports(String name) {
        return lookup.find(name).isPresent();
    }

    // ------------------------------------------------------------------ control

    /**
     * Returns the stub's own version, so that a stale stub fails here rather than as noise elsewhere.
     *
     * @return the stub version
     */
    public static int stubVersion() {
        return callInt("wkjstub_stub_version");
    }

    /** Clears the call ring, the programmed returns, the armed exceptions and this thread's slot. */
    public static void reset() {
        callVoid("wkjstub_reset");
    }

    /** Clears the programmed returns and the armed exceptions, keeping the recorded calls. */
    public static void clearReturns() {
        callVoid("wkjstub_clear_returns");
    }

    /** Uninstalls the host table, as if {@code wkj_init} had never been called. */
    public static void clearHost() {
        callVoid("wkjstub_clear_host");
    }

    /**
     * Overrides what {@code wkj_abi_version()} reports, so that the version guard can be driven.
     *
     * @param version the version to report
     */
    public static void setAbiVersion(int version) {
        callVoidInt("wkjstub_set_abi_version", version);
    }

    /**
     * Returns the capacity of the call ring.
     *
     * @return the ring capacity in calls
     */
    public static int ringCapacity() {
        return callInt("wkjstub_ring_capacity");
    }

    // ---------------------------------------------------------------- recording

    /**
     * Returns the number of calls still in the ring.
     *
     * @return the retained call count, at most {@link #ringCapacity}
     */
    public static int callCount() {
        return callInt("wkjstub_call_count");
    }

    /**
     * Returns the number of calls ever made, including those the ring has dropped.
     *
     * @return the total call count
     */
    public static long callTotal() {
        return callLong("wkjstub_call_total");
    }

    /**
     * Returns the name of a recorded call.
     *
     * @param index the index into the ring, oldest retained first
     * @return the symbol name, or {@code null} if there is no such call
     */
    public static String callName(int index) {
        return outString("wkjstub_call_name", index);
    }

    /**
     * Returns the argument count of a recorded call. It equals the C parameter count, so it lines up
     * with the argument count of the bound {@link FunctionDescriptor}.
     *
     * @param index the index into the ring
     * @return the argument count, or -1
     */
    public static int callArgc(int index) {
        return callIntInt("wkjstub_call_argc", index);
    }

    /**
     * Returns the recorded kind of one argument: one of {@code v b h i l f d p}, plus {@code s} for a
     * UTF-16 string and {@code a} for a primitive array.
     *
     * @param index the index into the ring
     * @param arg the argument index
     * @return the kind character
     */
    public static char callArgKind(int index, int arg) {
        return (char) callIntIntInt("wkjstub_call_arg_kind", index, arg);
    }

    /**
     * Returns the raw bits of one argument: the value for an integer, the address for a pointer, and
     * the raw IEEE bit pattern for a {@code float} or a {@code double}.
     *
     * @param index the index into the ring
     * @param arg the argument index
     * @return the recorded bits
     */
    public static long callArgBits(int index, int arg) {
        return callLongIntInt("wkjstub_call_arg", index, arg);
    }

    /**
     * Reports whether a pointer argument arrived as {@code NULL}, which is how a Java {@code null}
     * string reaches the library.
     *
     * @param index the index into the ring
     * @param arg the argument index
     * @return true if the pointer was {@code NULL}
     */
    public static boolean callArgIsNull(int index, int arg) {
        return callIntIntInt("wkjstub_call_arg_is_null", index, arg) == 1;
    }

    /**
     * Returns a recorded UTF-16 string argument exactly as the library received it, embedded NUL and
     * lone surrogates included.
     *
     * @param index the index into the ring
     * @param arg the argument index
     * @return the string, {@code null} if the argument was {@code NULL}, {@code ""} if it was the
     *         empty string
     */
    public static String callArgString(int index, int arg) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment length = arena.allocate(JAVA_INT);
            MemorySegment text = (MemorySegment) invoke("wkjstub_call_arg_string",
                    FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS), index, arg, length);
            return decode(text, length.get(JAVA_INT, 0L));
        }
    }

    /**
     * Returns the captured payload of a string or array argument.
     *
     * @param index the index into the ring
     * @param arg the argument index
     * @return the bytes, or an empty array
     */
    public static byte[] callArgBytes(int index, int arg) {
        try (Arena arena = Arena.ofConfined()) {
            int capacity = 1 << 20;
            MemorySegment out = arena.allocate(capacity);
            int bytes = (int) invoke("wkjstub_call_arg_bytes",
                    FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT),
                    index, arg, out, capacity);
            if (bytes <= 0) {
                return new byte[0];
            }
            return out.asSlice(0L, bytes).toArray(JAVA_BYTE);
        }
    }

    /**
     * Finds the first recorded call to a symbol.
     *
     * @param name the symbol name
     * @param from the ring index to start at
     * @return the index, or -1
     */
    public static int findCall(String name, int from) {
        try (Arena arena = Arena.ofConfined()) {
            return (int) invoke("wkjstub_find_call",
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT),
                    string(arena, name), name.length(), from);
        }
    }

    /**
     * Counts the recorded calls to a symbol.
     *
     * @param name the symbol name
     * @return the number of such calls still in the ring
     */
    public static int countCalls(String name) {
        try (Arena arena = Arena.ofConfined()) {
            return (int) invoke("wkjstub_count_calls",
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT), string(arena, name),
                    name.length());
        }
    }

    // ------------------------------------------------------- programmed returns

    /**
     * Programs the integer a function returns.
     *
     * @param symbol the symbol name
     * @param value the value, narrowed by the stub to the function's return type
     */
    public static void setReturnLong(String symbol, long value) {
        try (Arena arena = Arena.ofConfined()) {
            invoke("wkjstub_set_return_i64",
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_LONG),
                    string(arena, symbol), symbol.length(), value);
        }
    }

    /**
     * Programs the floating point value a function returns.
     *
     * @param symbol the symbol name
     * @param value the value, narrowed by the stub to {@code float} where the function returns one
     */
    public static void setReturnDouble(String symbol, double value) {
        try (Arena arena = Arena.ofConfined()) {
            invoke("wkjstub_set_return_f64",
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_DOUBLE),
                    string(arena, symbol), symbol.length(), value);
        }
    }

    /**
     * Programs the string a function returns. A {@code null} value programs {@code WKJ_STR_NULL},
     * that is a Java {@code null}; an empty value programs {@code WKJ_STR_OK} with length zero, that
     * is {@code ""}. Keeping those two apart on the way out is load bearing (contract section 11.1).
     *
     * @param symbol the symbol name
     * @param value the value, may be {@code null}
     */
    public static void setReturnString(String symbol, String value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment text = value == null ? MemorySegment.NULL : string(arena, value);
            invoke("wkjstub_set_return_string",
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, JAVA_INT),
                    string(arena, symbol), symbol.length(), text,
                    value == null ? 0 : value.length());
        }
    }

    /**
     * Programs the bytes a fill an array function copies out.
     *
     * @param symbol the symbol name
     * @param data the bytes
     */
    public static void setReturnBytes(String symbol, byte[] data) {
        try (Arena arena = Arena.ofConfined()) {
            invoke("wkjstub_set_return_bytes",
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, JAVA_INT),
                    string(arena, symbol), symbol.length(), arena.allocateFrom(JAVA_BYTE, data),
                    data.length);
        }
    }

    // --------------------------------------------------------------- exceptions

    /**
     * Sets this thread's exception slot immediately, without going through a {@code wkj_*} call.
     *
     * @param type the {@code WKJ_EXC_*} kind
     * @param code the DOM exception code
     * @param message the message, may be {@code null}
     */
    public static void raise(int type, int code, String message) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment text = message == null ? MemorySegment.NULL : string(arena, message);
            invoke("wkjstub_raise",
                    FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT),
                    type, code, text, message == null ? 0 : message.length());
        }
    }

    /**
     * Arms a one shot exception on a symbol: the next call to it fills the calling thread's slot,
     * which is the real control flow rather than a poke at memory.
     *
     * @param symbol the symbol name
     * @param type the {@code WKJ_EXC_*} kind
     * @param code the DOM exception code
     * @param message the message, may be {@code null}
     */
    public static void armException(String symbol, int type, int code, String message) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment text = message == null ? MemorySegment.NULL : string(arena, message);
            invoke("wkjstub_arm_exception",
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS,
                            JAVA_INT),
                    string(arena, symbol), symbol.length(), type, code, text,
                    message == null ? 0 : message.length());
        }
    }

    /**
     * Returns the {@code type} field of this thread's exception slot.
     *
     * @return the pending kind, zero when the slot is clean
     */
    public static int exceptionPending() {
        return callInt("wkjstub_exception_pending");
    }

    // -------------------------------------------------------------- host table

    /**
     * Reports whether {@code wkj_init} has installed a host table.
     *
     * @return true if a table is installed
     */
    public static boolean hostInstalled() {
        return callInt("wkjstub_host_installed") == 1;
    }

    /**
     * Returns the {@code host_size} the installed table was accepted with.
     *
     * @return the size in bytes, zero if nothing is installed
     */
    public static int hostSize() {
        return callInt("wkjstub_host_size");
    }

    /**
     * Returns the ABI version the installed table was accepted with.
     *
     * @return the version, zero if nothing is installed
     */
    public static int hostAbiVersion() {
        return callInt("wkjstub_host_abi_version");
    }

    /**
     * Returns the result code of the last {@code wkj_init} call.
     *
     * @return {@code WKJ_INIT_OK} or one of the negative {@code WKJ_INIT_ERR_*} codes
     */
    public static int hostInitResult() {
        return callInt("wkjstub_host_init_result");
    }

    /**
     * Returns the number of callback slots the C header declares.
     *
     * @return the slot count
     */
    public static int hostSlotCount() {
        return callInt("wkjstub_host_slot_count");
    }

    /**
     * Returns the dotted path of a callback slot, for example {@code core.release}.
     *
     * @param index the slot index
     * @return the slot name
     */
    public static String hostSlotName(int index) {
        return outString("wkjstub_host_slot_name", index);
    }

    /**
     * Returns the signature of a callback slot: the return kind followed by one kind per parameter.
     *
     * @param index the slot index
     * @return the signature string
     */
    public static String hostSlotSignature(int index) {
        return outString("wkjstub_host_slot_signature", index);
    }

    /**
     * Returns the byte offset of a callback slot inside {@code WKJHost}, as the C compiler computed
     * it.
     *
     * @param index the slot index
     * @return the offset
     */
    public static long hostSlotOffset(int index) {
        return callLongInt("wkjstub_host_slot_offset", index);
    }

    /**
     * Returns the function pointer installed in a callback slot.
     *
     * @param index the slot index
     * @return the address, zero when the slot is {@code NULL} or nothing is installed
     */
    public static long hostSlotPointer(int index) {
        return callLongInt("wkjstub_host_slot_pointer", index);
    }

    /**
     * Finds a callback slot by its dotted path.
     *
     * @param name the slot name
     * @return the slot index, or -1
     */
    public static int findHostSlot(String name) {
        try (Arena arena = Arena.ofConfined()) {
            return (int) invoke("wkjstub_find_host_slot",
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT), string(arena, name),
                    name.length());
        }
    }

    /**
     * Calls one installed callback with the correct C signature. Arguments are one {@code int64_t}
     * per parameter: integers sign extended, {@code float} and {@code double} as raw bits, pointers
     * as addresses.
     *
     * @param slot the slot index
     * @param args the arguments
     * @return 0 on success, -1 unknown slot, -2 the slot is {@code NULL}, -3 wrong argument count,
     *         -4 no host installed
     */
    public static int fireHost(int slot, long... args) {
        return fire("wkjstub_fire_host", slot, args);
    }

    /**
     * The same, from an OS thread the JVM has never seen: the case JNI needed
     * {@code AttachCurrentThread} for, and the one WebKit's worker threads take.
     *
     * @param slot the slot index
     * @param args the arguments
     * @return the status, or -5 when the thread could not be started
     */
    public static int fireHostOnForeignThread(int slot, long... args) {
        return fire("wkjstub_fire_host_on_foreign_thread", slot, args);
    }

    /**
     * Returns the value the last fire on this thread produced.
     *
     * @return the raw return bits
     */
    public static long lastFireResult() {
        return LAST_FIRE.get()[0];
    }

    // --------------------------------------------------------- ABI description

    /**
     * Returns the number of {@code wkj_*} functions the library implements.
     *
     * @return the symbol count
     */
    public static int symbolCount() {
        return callInt("wkjstub_symbol_count");
    }

    /**
     * Returns the name of one exported function.
     *
     * @param index the symbol index
     * @return the name
     */
    public static String symbolName(int index) {
        return outString("wkjstub_symbol_name", index);
    }

    /**
     * Returns the C library's own view of a function's shape, derived from its C types rather than
     * from the spec's layout columns: the return kind followed by one kind per parameter.
     *
     * @param index the symbol index
     * @return the signature string
     */
    public static String symbolSignature(int index) {
        return outString("wkjstub_symbol_signature", index);
    }

    /**
     * Reports whether the DOM spec marks a function as raising.
     *
     * @param index the symbol index
     * @return true if the function can raise
     */
    public static boolean symbolThrows(int index) {
        return callIntInt("wkjstub_symbol_throws", index) == 1;
    }

    // ----------------------------------------------------------------- layouts

    /**
     * Returns the number of structs the C header declares.
     *
     * @return the struct count
     */
    public static int structCount() {
        return callInt("wkjstub_struct_count");
    }

    /**
     * Returns the name of a struct.
     *
     * @param index the struct index
     * @return the struct name
     */
    public static String structName(int index) {
        return outString("wkjstub_struct_name", index);
    }

    /**
     * Returns {@code sizeof} the struct, as the C compiler computed it.
     *
     * @param index the struct index
     * @return the size in bytes
     */
    public static long structSize(int index) {
        return callLongInt("wkjstub_struct_size", index);
    }

    /**
     * Returns the number of members of a struct.
     *
     * @param index the struct index
     * @return the field count
     */
    public static int structFieldCount(int index) {
        return callIntInt("wkjstub_struct_field_count", index);
    }

    /**
     * Returns the name of a struct member.
     *
     * @param index the struct index
     * @param field the field index
     * @return the field name
     */
    public static String structFieldName(int index, int field) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment length = arena.allocate(JAVA_INT);
            MemorySegment text = (MemorySegment) invoke("wkjstub_struct_field_name",
                    FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS), index, field,
                    length);
            return decode(text, length.get(JAVA_INT, 0L));
        }
    }

    /**
     * Returns {@code offsetof} a struct member.
     *
     * @param index the struct index
     * @param field the field index
     * @return the offset in bytes
     */
    public static long structFieldOffset(int index, int field) {
        return callLongIntInt("wkjstub_struct_field_offset", index, field);
    }

    /**
     * Returns the size of a struct member, including every element of an array member.
     *
     * @param index the struct index
     * @param field the field index
     * @return the size in bytes
     */
    public static long structFieldSize(int index, int field) {
        return callLongIntInt("wkjstub_struct_field_size", index, field);
    }

    /**
     * Returns the kind of a struct member; for an array member it is the kind of one element, and
     * {@code 'S'} for a nested struct.
     *
     * @param index the struct index
     * @param field the field index
     * @return the kind character
     */
    public static char structFieldKind(int index, int field) {
        return (char) callIntIntInt("wkjstub_struct_field_kind", index, field);
    }

    /**
     * Returns 1 for a scalar member and the extent for a fixed size array member.
     *
     * @param index the struct index
     * @param field the field index
     * @return the element count
     */
    public static int structFieldElements(int index, int field) {
        return callIntIntInt("wkjstub_struct_field_elements", index, field);
    }

    /**
     * Finds a struct by name.
     *
     * @param name the struct name
     * @return the struct index, or -1
     */
    public static int findStruct(String name) {
        for (int i = 0, n = structCount(); i < n; i++) {
            if (name.equals(structName(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds a struct member by name.
     *
     * @param struct the struct index
     * @param field the field name
     * @return the field index, or -1
     */
    public static int findStructField(int struct, String field) {
        for (int i = 0, n = structFieldCount(struct); i < n; i++) {
            if (field.equals(structFieldName(struct, i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns {@code sizeof} a named struct.
     *
     * @param struct the struct name
     * @return the size in bytes
     * @throws IllegalArgumentException if the library declares no such struct
     */
    public static long sizeOf(String struct) {
        int index = findStruct(struct);
        if (index < 0) {
            throw new IllegalArgumentException("no such struct in " + LIBRARY_NAME + ": " + struct);
        }
        return structSize(index);
    }

    /**
     * Returns {@code offsetof} a named member of a named struct.
     *
     * @param struct the struct name
     * @param field the field name
     * @return the offset in bytes
     * @throws IllegalArgumentException if the library declares no such struct or member
     */
    public static long offsetOf(String struct, String field) {
        int index = findStruct(struct);
        if (index < 0) {
            throw new IllegalArgumentException("no such struct in " + LIBRARY_NAME + ": " + struct);
        }
        int fieldIndex = findStructField(index, field);
        if (fieldIndex < 0) {
            throw new IllegalArgumentException("no member " + field + " of " + struct);
        }
        return structFieldOffset(index, fieldIndex);
    }

    // ------------------------------------------------------- lifetime fixtures

    /**
     * Returns a segment whose confined arena has already been closed, so that handing it to a
     * downcall surfaces the use after free as an {@link IllegalStateException} rather than as a
     * native crash.
     *
     * @return a segment backed by a closed session
     */
    public static MemorySegment allocateClosedSegment() {
        Arena arena = Arena.ofConfined();
        MemorySegment segment = arena.allocate(JAVA_CHAR, 8L);
        arena.close();
        return segment;
    }

    /**
     * Returns a live segment confined to the calling thread. Its arena is retained for the run, so
     * the segment stays valid until the JVM exits.
     *
     * @return a live confined segment
     */
    public static synchronized MemorySegment allocateConfinedSegment() {
        Arena arena = Arena.ofConfined();
        RETAINED.add(arena);
        return arena.allocateFrom(JAVA_CHAR, new char[] { 'a', 'b', 'c' });
    }

    /**
     * Calls {@code wkj_dom_Attr_setValue} with a caller supplied segment, so that a test can hand the
     * ABI a segment whose arena is closed. The facades take a {@link String} and allocate their own,
     * so this is the only way to reach that path.
     *
     * @param peer the peer
     * @param value the UTF-16 characters, or {@link MemorySegment#NULL}
     * @param length the length in code units
     */
    public static void callAttrSetValueRaw(long peer, MemorySegment value, int length) {
        invoke("wkj_dom_Attr_setValue", FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT),
                peer, value, length);
    }

    /**
     * Calls {@code wkj_dom_Attr_getName} with a caller supplied buffer and capacity, so that a test
     * can exercise a capacity the facade never offers, such as zero.
     *
     * @param peer the peer
     * @param buffer the result buffer
     * @param capacity the buffer capacity in code units
     * @param length the {@code int32_t*} the library writes the length through
     * @return the {@code WKJ_STR_*} status
     */
    public static int callAttrGetNameRaw(long peer, MemorySegment buffer, int capacity,
                                         MemorySegment length) {
        return (int) invoke("wkj_dom_Attr_getName",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS),
                peer, buffer, capacity, length);
    }

    // ------------------------------------------------- calling a slot of any table

    /**
     * Calls a function pointer read out of a callback table, with one {@code long} per parameter.
     * {@link #fireHost} does this for {@code WKJHost}, where the stub generates a typed dispatcher
     * from the C header; this is the same thing for a table the stub does not flatten - notably
     * {@code WKJLiveConnectHost}, which is installed by {@code wkj_live_connect_init} rather than
     * being a {@code WKJHost} member.
     * <p>
     * The signature is the stub's own kind notation: the return kind followed by one kind per
     * parameter, from {@code v i l p} - the only four the reflective tables use. A {@code p}
     * argument is an address, and a {@code p} return comes back as one.
     *
     * @param address the function pointer, which must not be zero
     * @param signature the return kind followed by one kind per parameter
     * @param args one value per parameter
     * @return the result, or zero for a {@code void} slot
     */
    @SuppressWarnings("restricted")
    public static long callSlot(long address, String signature, long... args) {
        if (address == 0L) {
            throw new IllegalArgumentException("the slot is NULL, so there is nothing to call");
        }
        if (signature.length() - 1 != args.length) {
            throw new IllegalArgumentException("signature " + signature + " wants "
                    + (signature.length() - 1) + " arguments and was given " + args.length);
        }
        MemoryLayout[] parameters = new MemoryLayout[args.length];
        Object[] values = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            char kind = signature.charAt(i + 1);
            parameters[i] = layoutOfKind(kind);
            values[i] = switch (kind) {
                case 'i' -> (int) args[i];
                case 'p' -> MemorySegment.ofAddress(args[i]);
                default -> args[i];
            };
        }
        char returnKind = signature.charAt(0);
        FunctionDescriptor descriptor = returnKind == 'v'
                ? FunctionDescriptor.ofVoid(parameters)
                : FunctionDescriptor.of(layoutOfKind(returnKind), parameters);
        MethodHandle handle = LINKER.downcallHandle(MemorySegment.ofAddress(address), descriptor);
        Object result;
        try {
            result = handle.invokeWithArguments(values);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        return switch (returnKind) {
            case 'v' -> 0L;
            case 'i' -> (int) result;
            case 'p' -> ((MemorySegment) result).address();
            default -> (long) result;
        };
    }

    private static MemoryLayout layoutOfKind(char kind) {
        return switch (kind) {
            case 'i' -> JAVA_INT;
            case 'l' -> JAVA_LONG;
            case 'p' -> ADDRESS;
            default -> throw new IllegalArgumentException("unsupported slot kind: " + kind);
        };
    }

    // ---------------------------------------------------------------- plumbing

    private static int fire(String symbol, int slot, long[] args) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment argv = args.length == 0
                    ? MemorySegment.NULL
                    : arena.allocateFrom(JAVA_LONG, args);
            MemorySegment out = arena.allocate(JAVA_LONG);
            int status = (int) invoke(symbol,
                    FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS),
                    slot, argv, args.length, out);
            LAST_FIRE.get()[0] = out.get(JAVA_LONG, 0L);
            return status;
        }
    }

    private static String outString(String symbol, int index) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment length = arena.allocate(JAVA_INT);
            MemorySegment text = (MemorySegment) invoke(symbol,
                    FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS), index, length);
            return decode(text, length.get(JAVA_INT, 0L));
        }
    }

    /*
     * The query ABI reports a NULL string as a NULL pointer with a length of -1, and the empty string
     * as a valid pointer with a length of 0. Both survive as far as the caller.
     */
    @SuppressWarnings("restricted")
    private static String decode(MemorySegment text, int length) {
        if (text.address() == 0L || length < 0) {
            return null;
        }
        if (length == 0) {
            return "";
        }
        char[] chars = new char[length];
        MemorySegment.copy(text.reinterpret((long) length * Character.BYTES), JAVA_CHAR, 0L,
                chars, 0, length);
        return new String(chars);
    }

    private static MemorySegment string(Arena arena, String s) {
        if (s.isEmpty()) {
            return arena.allocate(JAVA_CHAR);
        }
        return arena.allocateFrom(JAVA_CHAR, s.toCharArray());
    }

    private static void callVoid(String symbol) {
        invoke(symbol, FunctionDescriptor.ofVoid());
    }

    private static void callVoidInt(String symbol, int a) {
        invoke(symbol, FunctionDescriptor.ofVoid(JAVA_INT), a);
    }

    private static int callInt(String symbol) {
        return (int) invoke(symbol, FunctionDescriptor.of(JAVA_INT));
    }

    private static int callIntInt(String symbol, int a) {
        return (int) invoke(symbol, FunctionDescriptor.of(JAVA_INT, JAVA_INT), a);
    }

    private static int callIntIntInt(String symbol, int a, int b) {
        return (int) invoke(symbol, FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT), a, b);
    }

    private static long callLong(String symbol) {
        return (long) invoke(symbol, FunctionDescriptor.of(JAVA_LONG));
    }

    private static long callLongInt(String symbol, int a) {
        return (long) invoke(symbol, FunctionDescriptor.of(JAVA_LONG, JAVA_INT), a);
    }

    private static long callLongIntInt(String symbol, int a, int b) {
        return (long) invoke(symbol, FunctionDescriptor.of(JAVA_LONG, JAVA_INT, JAVA_INT), a, b);
    }

    /*
     * One binding site instead of fifty fields. The query ABI is on no hot path, so the boxing of
     * invokeWithArguments costs nothing that matters here; the facades under test use invokeExact.
     */
    private static Object invoke(String symbol, FunctionDescriptor descriptor, Object... args) {
        MethodHandle handle = HANDLES.computeIfAbsent(symbol, name -> bind(name, descriptor));
        try {
            return handle.invokeWithArguments(args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new AssertionError(symbol, t);
        }
    }

    @SuppressWarnings("restricted")
    private static MethodHandle bind(String symbol, FunctionDescriptor descriptor) {
        MemorySegment address = lookup.find(symbol)
                .orElseThrow(() -> new UnsatisfiedLinkError("missing native symbol: " + symbol));
        return LINKER.downcallHandle(address, descriptor);
    }
}

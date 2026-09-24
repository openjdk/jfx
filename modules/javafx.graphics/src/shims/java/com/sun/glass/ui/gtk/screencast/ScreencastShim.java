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

package com.sun.glass.ui.gtk.screencast;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Test access to the screen capture part of the GTK glass, for the Linux binding tests in
 * {@code test.com.sun.glass.ui.gtk}, which reach this class through the
 * {@code --add-exports javafx.graphics/com.sun.glass.ui.gtk.screencast=ALL-UNNAMED} line of
 * {@code src/test/addExports}.
 * <p>
 * Everything about the binding is reached reflectively, so that this class also compiles against the JNI build of
 * commit {@code 033187ad90}, where {@code ScreencastNative} does not exist and the tests of the debug output run
 * on the natives. The few {@code java.lang.foreign} bindings of its own - the version and {@code sizeof} probes of
 * {@code native-glass/gtk/screencast_api.h}, and the call into the production token stub - are yardsticks the
 * scenarios measure the binding against; they stay in this class so that the restricted calls stay inside the
 * module {@code --enable-native-access} names.
 */
public final class ScreencastShim {

    /**
     * {@code int32_t (*store_token)(const char *old_token, const char *new_token, const int32_t *bounds,
     * int32_t bounds_len)}: the slot the library dials, as this class dials the stub in it.
     */
    private static final FunctionDescriptor STORE_TOKEN = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS,
            JAVA_INT);

    private ScreencastShim() {
    }

    /** {@code library!symbol} of every symbol the screen capture binding bound, in binding order. */
    @SuppressWarnings("unchecked")
    public static List<String> boundSymbols() {
        return (List<String>) call("boundSymbols", new Class<?>[0]);
    }

    /** Whether the token callback table has been handed to the library in this JVM. */
    public static boolean tokenCallbacksInstalled() {
        return (Boolean) call("tokenCallbacksInstalled", new Class<?>[0]);
    }

    /** Hands the library the token callback table, as the successful half of {@code loadPipewire} does. */
    public static void installTokenCallbacks() {
        call("installTokenCallbacks", new Class<?>[0]);
    }

    /** {@code sc_load_pipewire}: the first half of {@code loadPipewire}, without the portal probe. */
    public static boolean loadPipewire(int method, boolean debug) {
        return (Boolean) call("loadPipewire", new Class<?>[] {int.class, boolean.class}, method, debug);
    }

    /** {@code ScreencastHelper.isAvailable()}: whether the natives and the portal were there. */
    public static boolean helperIsAvailable() {
        return ScreencastHelper.isAvailable();
    }

    /** Whether {@code ScreencastHelper} still declares its work as {@code native} methods. */
    public static boolean helperUsesNativeMethods() {
        for (Method method : ScreencastHelper.class.getDeclaredMethods()) {
            if (Modifier.isNative(method.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    /** The constants of the binding that {@code screencast_api.h} also states, by their Java names. */
    public static Map<String, Integer> constants() {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String name : List.of("ABI_VERSION", "OK", "UPCALL_OK", "UPCALL_THREW", "METHOD_SCREENCAST",
                "METHOD_REMOTE_DESKTOP", "RESULT_OK", "RESULT_ERROR", "RESULT_DENIED", "RESULT_OUT_OF_BOUNDS",
                "RESULT_NO_STREAMS")) {
            values.put(name, (Integer) field(nativeClass(), name));
        }
        return values;
    }

    /** The result codes and method ids {@code ScreencastHelper} keeps for itself, by their Java names. */
    public static Map<String, Integer> helperConstants() {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String name : List.of("ERROR", "DENIED", "OUT_OF_BOUNDS", "NO_STREAMS", "XDG_METHOD_SCREENCAST",
                "XDG_METHOD_REMOTE_DESKTOP")) {
            values.put(name, (Integer) field(ScreencastHelper.class, name));
        }
        return values;
    }

    /** The size the binding lays {@code struct ScTokenCallbacks} out as. */
    public static long tokenCallbacksLayoutSize() {
        return ((MemoryLayout) field(nativeClass(), "TOKEN_CALLBACKS_LAYOUT")).byteSize();
    }

    /** {@code sc_abi_version} of the library this build loaded, bound by this class. */
    public static int abiVersion() {
        return probe("sc_abi_version");
    }

    /** {@code sc_sizeof_token_callbacks} of that library. */
    public static int sizeofTokenCallbacks() {
        return probe("sc_sizeof_token_callbacks");
    }

    /**
     * Calls the production token stub as the library calls the slot: {@code oldToken} and {@code newToken} are the
     * bytes {@code NewStringUTF} would have been given, {@code null} for a {@code NULL} pointer and without a
     * terminator of their own (one is added), and {@code bounds} is the screen-bounds buffer, {@code null} for a
     * {@code NULL} pointer. {@code boundsLen} is what the C would pass as the number of ints, so that a test can
     * hand the slot a length the C never would.
     *
     * @return the status the stub answered: {@code SC_UPCALL_OK} or {@code SC_UPCALL_THREW}
     */
    @SuppressWarnings("restricted")
    public static int fireStoreToken(byte[] oldToken, byte[] newToken, int[] bounds, int boundsLen) {
        MemorySegment stub = (MemorySegment) call("storeTokenStub", new Class<?>[0]);
        MethodHandle slot = Linker.nativeLinker().downcallHandle(stub, STORE_TOKEN);
        try (Arena arena = Arena.ofConfined()) {
            try {
                return (int) slot.invokeExact(cString(arena, oldToken), cString(arena, newToken),
                        ints(arena, bounds), boundsLen);
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }
    }

    /**
     * A restore token through the production encode and decode: the modified UTF-8 the binding hands the C, read
     * back as the C string the C hands a slot. {@code null} stays {@code null}, which is the {@code NULL} pointer
     * of both directions.
     */
    public static String tokenRoundTrip(String token) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment block = (MemorySegment) call(nativeClass(), "token",
                    new Class<?>[] {Arena.class, String.class}, arena, token);
            return (String) call(nativeClass(), "newStringUtf", new Class<?>[] {MemorySegment.class}, block);
        }
    }

    /** The bytes the binding hands the C for {@code token}, terminator included; {@code null} for {@code null}. */
    public static byte[] tokenBytes(String token) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment block = (MemorySegment) call(nativeClass(), "token",
                    new Class<?>[] {Arena.class, String.class}, arena, token);
            return block.address() == 0 ? null : block.toArray(JAVA_BYTE);
        }
    }

    /** {@code values} through the native staging copy the blocking calls use, and back again. */
    public static int[] stagingRoundTrip(int[] values) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment block = staging(arena, values);
            int[] back = new int[values.length];
            call(nativeClass(), "copyFromNative", new Class<?>[] {MemorySegment.class, int[].class}, block, back);
            return back;
        }
    }

    /** The size in bytes of the staging block the binding allocates for {@code values}. */
    public static long stagingByteSize(int[] values) {
        try (Arena arena = Arena.ofConfined()) {
            return staging(arena, values).byteSize();
        }
    }

    /** Whether the staging block the binding allocates for {@code values} is a {@code NULL} pointer. */
    public static boolean stagingIsNull(int[] values) {
        try (Arena arena = Arena.ofConfined()) {
            return staging(arena, values).address() == 0;
        }
    }

    /**
     * The message of the {@link ArrayIndexOutOfBoundsException} the binding throws for a rejected row copy, or
     * {@code null} when it throws none.
     */
    public static String rejectedRegionMessage(int count, int start, int length, int pixelsLen) {
        Object exception = call(nativeClass(), "rejectedRegion",
                new Class<?>[] {int.class, int.class, int.class, int.class}, count, start, length, pixelsLen);
        return exception == null ? null : ((Throwable) exception).getMessage();
    }

    // ---------------------------------------------------------------------------------------------
    // Support
    // ---------------------------------------------------------------------------------------------

    private static MemorySegment staging(Arena arena, int[] values) {
        return (MemorySegment) call(nativeClass(), "copyToNative", new Class<?>[] {Arena.class, int[].class},
                arena, values);
    }

    @SuppressWarnings("restricted")
    private static int probe(String symbol) {
        MemorySegment address = SymbolLookup.loaderLookup().find(symbol).orElseThrow(
                () -> new UnsatisfiedLinkError("the glass GTK library does not export " + symbol));
        MethodHandle handle = Linker.nativeLinker().downcallHandle(address, FunctionDescriptor.of(JAVA_INT));
        try {
            return (int) handle.invokeExact();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private static MemorySegment cString(Arena call, byte[] bytes) {
        if (bytes == null) {
            return MemorySegment.NULL;
        }
        MemorySegment block = call.allocate(bytes.length + 1L);
        MemorySegment.copy(bytes, 0, block, JAVA_BYTE, 0, bytes.length);
        return block;
    }

    private static MemorySegment ints(Arena call, int[] values) {
        return values == null ? MemorySegment.NULL : call.allocateFrom(JAVA_INT, values);
    }

    private static Class<?> nativeClass() {
        try {
            return Class.forName("com.sun.glass.ui.gtk.screencast.ScreencastNative");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object call(String name, Class<?>[] types, Object... arguments) {
        return call(nativeClass(), name, types, arguments);
    }

    private static Object call(Class<?> owner, String name, Class<?>[] types, Object... arguments) {
        try {
            Method method = owner.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(null, arguments);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object field(Class<?> owner, String name) {
        try {
            Field declared = owner.getDeclaredField(name);
            declared.setAccessible(true);
            return declared.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

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

package com.sun.javafx.font;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * The running JVM's own {@code NewStringUTF}, {@code NewString}, {@code GetStringUTFLength} and
 * {@code GetStringUTFRegion}, reached through {@code java.lang.foreign} for the tests of {@link JniStringCodec}: the
 * oracle a Java reproduction of HotSpot's string conversions is measured against, needing no C of the project and
 * no JNI library of its own. It never calls {@link JniStringCodec}. {@code GetStringUTFRegion} and
 * {@code GetStringUTFChars} share one encoder ({@code java_lang_String::as_utf8_string} over
 * {@code UNICODE::utf8_write}); the region form writes into a buffer this class owns, which the rule below requires.
 *
 * <h2>How the JNI functions are reached</h2>
 * {@code JNI_GetCreatedJavaVMs}, exported by the JVM library under {@code java.home} ({@code lib/server/libjvm.so},
 * {@code bin/server/jvm.dll}), gives the {@code JavaVM*}; {@code GetEnv} in its function table gives the calling
 * thread's {@code JNIEnv*}; the {@code JNIEnv} function table ({@code struct JNINativeInterface_} in {@code jni.h})
 * holds one function pointer per slot in declaration order after four reserved words. Every function is called
 * through the address-less {@link Linker#downcallHandle(FunctionDescriptor, Linker.Option...)} handle of its
 * signature with the slot's pointer as the leading argument, which is what {@code (*env)->NewStringUTF(env, s)}
 * expands to in C.
 *
 * <h2>Slot numbers</h2>
 * The constants below are the positions in the {@code jni.h} of JDK 25 and JDK 26 (236 members; the struct only
 * ever grows at its end). They are checked at run time before any of them is called: {@code java.home/include/jni.h}
 * is parsed when present and any disagreement fails; then {@code GetVersion} must report a JNI version and the ASCII
 * string {@code JniStringOracle} must come back unchanged through every function used, first through a global
 * reference and then through the local-reference sequences below. A wrong slot therefore fails a test instead of
 * calling an unrelated JNI function with these arguments.
 *
 * <h2>A local reference lives only between adjacent downcalls</h2>
 * HotSpot's native-method wrapper resets the calling thread's local-reference block when a JNI native method
 * returns ({@code SharedRuntime::generate_native_wrapper}, "reset handle block": {@code JNIHandleBlock::top} is set
 * to 0), because a native method's local references end with the method. A local reference made by a downcall has
 * no such method around it: it lives in the same block and is destroyed - its slot reused - by the next JNI native
 * method that returns on this thread, whatever called it. In interpreted code that includes every
 * {@code Unsafe}-backed memory access ({@code MemorySegment} reads and writes, arena allocation), every restricted
 * method ({@code Reflection.getCallerClass} behind {@code MemorySegment.reinterpret}), {@code System.out.println},
 * {@code Object.hashCode} and the class spinning behind a method handle's first invocation; measured on JDK 25.0.4:
 * a {@code println} between {@code NewStringUTF} and {@code GetStringLength} makes {@code jni_GetStringLength}
 * fault. Every sequence here therefore reads its function pointers and allocates its buffers before the reference
 * is created, keeps nothing but downcalls between the call that makes a local reference and the calls that consume
 * and delete it, and has the JVM write results into buffers it owns ({@code GetStringRegion},
 * {@code GetStringUTFRegion}) rather than handing out pointers to read afterwards. The constructor primes every
 * call site and checks every slot through a global reference (immune to the reset) to the self-test string before
 * the local-reference sequences run. The output size is bounded before it is used:
 * {@code NewStringUTF} makes at most one char per input byte, {@code GetStringUTFRegion} at most three bytes per
 * char plus the terminator.
 *
 * <h2>One method, one thread; bounded batches</h2>
 * A {@code JNIEnv*} belongs to the thread that obtained it, for that thread's lifetime; one obtained on a thread
 * that later exits points at freed memory. Nothing here is kept across calls: each public method obtains the
 * {@code JavaVM*} and the {@code JNIEnv*} afresh, uses them on the calling thread only and deletes every reference
 * it created before returning. A JNI function called during a {@code java.lang.foreign} downcall sees the thread
 * in the native state, exactly as it would from a JNI native method.
 * <p>
 * A JNI string-creating function called this way returns a local reference into the calling thread's
 * {@code JNIHandleBlock}. That block is only reclaimed when a real JNI <em>native method</em> returns on the thread
 * - which an FFM downcall never is - so the references accumulate, and when the block must chain a second block the
 * next allocation faults ({@code jni_GetStringLength} reading a bad slot). Measured on JDK 25.0.4 and JDK 26+35,
 * with and without GC (Epsilon included, so it is not oop movement): a freshly started JVM sustains on the order of
 * a hundred such references before the fault, a JVM already busy with other JNI work far fewer. Neither
 * {@code DeleteLocalRef} (it nulls a slot without lowering the block's top) nor {@code PushLocalFrame} /
 * {@code PopLocalFrame} nor an upcall boundary reclaims them through FFM. The conversions are therefore capped at
 * {@link #MAX_BATCH} references per call; the fuzz oracle feeds a large corpus by launching one short-lived child
 * JVM per batch (see {@code test.com.sun.javafx.font.JniStringOracleTest}), each of which starts with an empty
 * block. This limitation is the tool's, not the codec's; it does not exist once the C is gone, because the codec
 * never calls JNI.
 */
public final class JniStringOracleShim {

    private static final Linker LINKER = Linker.nativeLinker();

    /**
     * The most string references one call may create in the calling thread's JNI handle block. Chosen well below
     * the measured fault point of a freshly started JVM so a child JVM processing one batch never chains a block.
     */
    public static final int MAX_BATCH = 32;

    /** {@code jni.h}: {@code JNI_OK}, and {@code JNI_VERSION_1_8}, the version {@code GetEnv} is asked for. */
    private static final int JNI_OK = 0;
    private static final int JNI_VERSION_1_8 = 0x00010008;

    /**
     * {@code struct JNIInvokeInterface_}: three reserved words, DestroyJavaVM, AttachCurrentThread,
     * DetachCurrentThread, then GetEnv.
     */
    private static final int VM_GET_ENV = 6;

    /* struct JNINativeInterface_: four reserved words, then the functions in declaration order. */
    private static final int GET_VERSION = 4;
    private static final int NEW_GLOBAL_REF = 21;
    private static final int DELETE_GLOBAL_REF = 22;
    private static final int DELETE_LOCAL_REF = 23;
    private static final int NEW_STRING = 163;
    private static final int GET_STRING_LENGTH = 164;
    private static final int NEW_STRING_UTF = 167;
    private static final int GET_STRING_UTF_LENGTH = 168;
    private static final int GET_STRING_REGION = 220;
    private static final int GET_STRING_UTF_REGION = 221;

    private static final Map<String, Integer> ENV_SLOTS = envSlots();
    private static final Map<String, Integer> VM_SLOTS = Map.of("GetEnv", VM_GET_ENV);

    /** ASCII, so every conversion must return it unchanged. */
    private static final String SELF_TEST = "JniStringOracle";

    /* Address-less handles, one per signature: the function pointer read from a table is the leading argument. */
    private static final MethodHandle JNI_GET_CREATED_JAVA_VMS_MH =     // jint (JavaVM**, jsize, jsize*)
            handle(FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle GET_ENV_MH =                      // jint (JavaVM*, void**, jint)
            handle(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
    private static final MethodHandle ENV_TO_INT_MH =                   // jint (JNIEnv*): GetVersion
            handle(FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle REF_TO_REF_MH =                   // jobject (JNIEnv*, void*)
            handle(FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle REF_TO_INT_MH =                   // jsize (JNIEnv*, jstring): the two lengths
            handle(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private static final MethodHandle REF_TO_VOID_MH =                  // void (JNIEnv*, jobject): the two deletes
            handle(FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
    private static final MethodHandle REGION_MH =                       // void (JNIEnv*, jstring, jsize, jsize, buf)
            handle(FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));
    private static final MethodHandle NEW_STRING_MH =                   // jstring (JNIEnv*, const jchar*, jsize)
            handle(FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT));

    private JniStringOracleShim() {
    }

    /**
     * What {@link #probe} established: the JNI version {@code GetVersion} reports, how the slot numbers were checked
     * and which JVM library answered.
     */
    public record Probe(int jniVersion, String slotEvidence, String libjvm) {
    }

    /** Obtains the {@code JNIEnv*}, checks the slots, the version and the self-test string, and reports. */
    public static Probe probe() {
        try (Arena arena = Arena.ofConfined()) {
            Env jni = new Env(arena);
            return new Probe(jni.version, jni.evidence, jni.libjvm.toString());
        }
    }

    /**
     * {@code NewStringUTF} of each input, as UTF-16 code units read back with {@code GetStringLength} and
     * {@code GetStringRegion}. A {@code 00} terminator is appended to every input; an embedded {@code 00} ends the
     * string where C would end it.
     */
    public static char[][] newStringUtf(byte[][] inputs) {
        requireBatch(inputs.length);
        char[][] results = new char[inputs.length][];
        try (Arena arena = Arena.ofConfined()) {
            Env jni = new Env(arena);
            for (int i = 0; i < inputs.length; i++) {
                try (Arena scratch = Arena.ofConfined()) {
                    results[i] = jni.decodeOne(scratch, inputs[i]);
                }
            }
        }
        return results;
    }

    /**
     * {@code NewString} of each input's code units, then {@code GetStringUTFRegion} over the whole string: the
     * modified UTF-8 bytes with their {@code 00} terminator (the last element), cut at {@code GetStringUTFLength}.
     */
    public static byte[][] getStringUtfRegion(char[][] inputs) {
        requireBatch(inputs.length);
        byte[][] results = new byte[inputs.length][];
        try (Arena arena = Arena.ofConfined()) {
            Env jni = new Env(arena);
            for (int i = 0; i < inputs.length; i++) {
                try (Arena scratch = Arena.ofConfined()) {
                    results[i] = jni.encodeOne(scratch, inputs[i]);
                }
            }
        }
        return results;
    }

    private static void requireBatch(int count) {
        if (count > MAX_BATCH) {
            throw new IllegalArgumentException("batch of " + count + " exceeds MAX_BATCH " + MAX_BATCH
                    + ": one call may create at most that many JNI string references before the handle block faults;"
                    + " split the corpus across child JVMs");
        }
    }

    /** The calling thread's {@code JNIEnv*} and the function pointers it needs, for one method invocation only. */
    private static final class Env {

        final String evidence;
        final Path libjvm;
        final MemorySegment env;
        final int version;

        /* Read before any local reference exists (a restricted call in interpreted code ends every local reference). */
        private final MemorySegment newStringUtf;
        private final MemorySegment getStringLength;
        private final MemorySegment getStringRegion;
        private final MemorySegment newString;
        private final MemorySegment getStringUtfLength;
        private final MemorySegment getStringUtfRegion;
        private final MemorySegment deleteLocalRef;
        private final MemorySegment newGlobalRef;
        private final MemorySegment deleteGlobalRef;

        Env(Arena arena) {
            evidence = verifySlots();
            libjvm = findLibjvm();
            MemorySegment getCreatedJavaVMs = lookup(libjvm, arena).find("JNI_GetCreatedJavaVMs")
                    .orElseThrow(() -> new IllegalStateException(libjvm + " exports no JNI_GetCreatedJavaVMs"));
            MemorySegment vms = arena.allocate(ADDRESS);
            MemorySegment count = arena.allocate(JAVA_INT);
            int rc;
            try {
                rc = (int) JNI_GET_CREATED_JAVA_VMS_MH.invokeExact(getCreatedJavaVMs, vms, 1, count);
            } catch (Throwable t) {
                throw new AssertionError("JNI_GetCreatedJavaVMs", t);
            }
            if (rc != JNI_OK || count.get(JAVA_INT, 0) != 1) {
                throw new IllegalStateException("JNI_GetCreatedJavaVMs: rc " + rc + ", " + count.get(JAVA_INT, 0)
                        + " VMs");
            }
            MemorySegment vm = vms.get(ADDRESS, 0);
            MemorySegment penv = arena.allocate(ADDRESS);
            try {
                rc = (int) GET_ENV_MH.invokeExact(slot(vm, VM_GET_ENV), vm, penv, JNI_VERSION_1_8);
            } catch (Throwable t) {
                throw new AssertionError("GetEnv", t);
            }
            if (rc != JNI_OK) {
                throw new IllegalStateException("GetEnv(JNI_VERSION_1_8): rc " + rc);
            }
            env = penv.get(ADDRESS, 0);
            newStringUtf = slot(env, NEW_STRING_UTF);
            getStringLength = slot(env, GET_STRING_LENGTH);
            getStringRegion = slot(env, GET_STRING_REGION);
            newString = slot(env, NEW_STRING);
            getStringUtfLength = slot(env, GET_STRING_UTF_LENGTH);
            getStringUtfRegion = slot(env, GET_STRING_UTF_REGION);
            deleteLocalRef = slot(env, DELETE_LOCAL_REF);
            newGlobalRef = slot(env, NEW_GLOBAL_REF);
            deleteGlobalRef = slot(env, DELETE_GLOBAL_REF);
            version = getVersion();
            if (version < JNI_VERSION_1_8 || (version >>> 16) > 0xFF) {
                throw new IllegalStateException("GetVersion (slot " + GET_VERSION + ") returned 0x"
                        + Integer.toHexString(version) + ", which is not a JNI version");
            }
            prime(arena);
            selfTest(arena);
        }

        /**
         * Primes every call site and checks every slot through a reference that survives anything: a global
         * reference to the self-test string, made from the local one the moment it exists.
         */
        private void prime(Arena arena) {
            byte[] ascii = SELF_TEST.getBytes(StandardCharsets.US_ASCII);
            MemorySegment cString = cString(arena, ascii);
            MemorySegment chars = arena.allocateFrom(JAVA_CHAR, SELF_TEST.toCharArray());
            MemorySegment units = arena.allocate(JAVA_CHAR, ascii.length);
            MemorySegment bytes = arena.allocate(3L * ascii.length + 1);
            MemorySegment local = newStringUtf(cString);
            MemorySegment global = newGlobalRef(local);
            deleteLocalRef(local);
            if (global.address() == 0) {
                throw new IllegalStateException("NewStringUTF or NewGlobalRef (slots " + NEW_STRING_UTF + ", "
                        + NEW_GLOBAL_REF + ") returned NULL for " + SELF_TEST);
            }
            int length = getStringLength(global);
            if (length != ascii.length) {
                throw new IllegalStateException("GetStringLength (slot " + GET_STRING_LENGTH + ") of " + SELF_TEST
                        + " is " + length);
            }
            getStringRegion(global, length, units);
            String decoded = new String(units.toArray(JAVA_CHAR));
            if (!SELF_TEST.equals(decoded)) {
                throw new IllegalStateException("GetStringRegion (slot " + GET_STRING_REGION + ") turned "
                        + SELF_TEST + " into " + decoded);
            }
            int utfLength = getStringUtfLength(global);
            if (utfLength != ascii.length) {
                throw new IllegalStateException("GetStringUTFLength (slot " + GET_STRING_UTF_LENGTH + ") of "
                        + SELF_TEST + " is " + utfLength);
            }
            getStringUtfRegion(global, length, bytes);
            byte[] encoded = bytes.asSlice(0, ascii.length + 1L).toArray(JAVA_BYTE);
            if (!Arrays.equals(encoded, Arrays.copyOf(ascii, ascii.length + 1))) {
                throw new IllegalStateException("GetStringUTFRegion (slot " + GET_STRING_UTF_REGION + ") turned "
                        + SELF_TEST + " into " + HexFormat.of().formatHex(encoded));
            }
            deleteLocalRef(newString(chars, ascii.length));
            deleteGlobalRef(global);
        }

        /** The local-reference sequences on the self-test string, after every site is primed. */
        private void selfTest(Arena arena) {
            byte[] ascii = SELF_TEST.getBytes(StandardCharsets.US_ASCII);
            String decoded = new String(decodeOne(arena, ascii));
            if (!SELF_TEST.equals(decoded)) {
                throw new IllegalStateException("NewStringUTF, GetStringLength and GetStringRegion (slots "
                        + NEW_STRING_UTF + ", " + GET_STRING_LENGTH + ", " + GET_STRING_REGION + ") turned "
                        + SELF_TEST + " into " + decoded);
            }
            byte[] encoded = encodeOne(arena, SELF_TEST.toCharArray());
            if (!Arrays.equals(encoded, Arrays.copyOf(ascii, ascii.length + 1))) {
                throw new IllegalStateException("NewString, GetStringUTFLength and GetStringUTFRegion (slots "
                        + NEW_STRING + ", " + GET_STRING_UTF_LENGTH + ", " + GET_STRING_UTF_REGION + ") turned "
                        + SELF_TEST + " into " + HexFormat.of().formatHex(encoded));
            }
        }

        /**
         * {@code NewStringUTF}, {@code GetStringLength}, {@code GetStringRegion}, {@code DeleteLocalRef}: nothing
         * else runs between the first and the last of them.
         */
        char[] decodeOne(Arena scratch, byte[] input) {
            MemorySegment cString = cString(scratch, input);
            MemorySegment units = scratch.allocate(JAVA_CHAR, input.length);
            MemorySegment string = newStringUtf(cString);
            if (string.address() == 0) {
                throw new IllegalStateException("NewStringUTF returned NULL for " + HexFormat.of().formatHex(input));
            }
            int length = getStringLength(string);
            if (length < 0 || length > input.length) {
                throw new IllegalStateException("GetStringLength " + length + " for the " + input.length + " bytes "
                        + HexFormat.of().formatHex(input));
            }
            getStringRegion(string, length, units);
            deleteLocalRef(string);
            return units.asSlice(0, 2L * length).toArray(JAVA_CHAR);
        }

        /**
         * {@code NewString}, {@code GetStringUTFLength}, {@code GetStringUTFRegion}, {@code DeleteLocalRef}: nothing
         * else runs between the first and the last of them.
         */
        byte[] encodeOne(Arena scratch, char[] input) {
            MemorySegment chars = scratch.allocateFrom(JAVA_CHAR, input);
            MemorySegment bytes = scratch.allocate(3L * input.length + 1);
            bytes.fill((byte) 0x5A);
            MemorySegment string = newString(chars, input.length);
            if (string.address() == 0) {
                throw new IllegalStateException("NewString returned NULL for " + input.length + " code units");
            }
            int utfLength = getStringUtfLength(string);
            if (utfLength < 0 || utfLength > 3 * input.length) {
                throw new IllegalStateException("GetStringUTFLength " + utfLength + " for " + input.length
                        + " code units");
            }
            getStringUtfRegion(string, input.length, bytes);
            deleteLocalRef(string);
            byte[] result = bytes.asSlice(0, utfLength + 1L).toArray(JAVA_BYTE);
            if (result[utfLength] != 0) {
                throw new IllegalStateException("GetStringUTFRegion wrote no terminator at GetStringUTFLength "
                        + utfLength + ": " + HexFormat.of().formatHex(result));
            }
            return result;
        }

        /** {@code bytes} followed by a {@code 00}, in memory from {@code allocator}. */
        private static MemorySegment cString(Arena allocator, byte[] bytes) {
            MemorySegment cString = allocator.allocate(bytes.length + 1L);
            MemorySegment.copy(bytes, 0, cString, JAVA_BYTE, 0, bytes.length);
            cString.set(JAVA_BYTE, bytes.length, (byte) 0);
            return cString;
        }

        int getVersion() {
            try {
                return (int) ENV_TO_INT_MH.invokeExact(slot(env, GET_VERSION), env);
            } catch (Throwable t) {
                throw new AssertionError("GetVersion", t);
            }
        }

        MemorySegment newStringUtf(MemorySegment cString) {
            try {
                return (MemorySegment) REF_TO_REF_MH.invokeExact(newStringUtf, env, cString);
            } catch (Throwable t) {
                throw new AssertionError("NewStringUTF", t);
            }
        }

        MemorySegment newGlobalRef(MemorySegment reference) {
            try {
                return (MemorySegment) REF_TO_REF_MH.invokeExact(newGlobalRef, env, reference);
            } catch (Throwable t) {
                throw new AssertionError("NewGlobalRef", t);
            }
        }

        int getStringLength(MemorySegment string) {
            try {
                return (int) REF_TO_INT_MH.invokeExact(getStringLength, env, string);
            } catch (Throwable t) {
                throw new AssertionError("GetStringLength", t);
            }
        }

        void getStringRegion(MemorySegment string, int length, MemorySegment units) {
            try {
                REGION_MH.invokeExact(getStringRegion, env, string, 0, length, units);
            } catch (Throwable t) {
                throw new AssertionError("GetStringRegion", t);
            }
        }

        MemorySegment newString(MemorySegment chars, int length) {
            try {
                return (MemorySegment) NEW_STRING_MH.invokeExact(newString, env, chars, length);
            } catch (Throwable t) {
                throw new AssertionError("NewString", t);
            }
        }

        int getStringUtfLength(MemorySegment string) {
            try {
                return (int) REF_TO_INT_MH.invokeExact(getStringUtfLength, env, string);
            } catch (Throwable t) {
                throw new AssertionError("GetStringUTFLength", t);
            }
        }

        void getStringUtfRegion(MemorySegment string, int length, MemorySegment bytes) {
            try {
                REGION_MH.invokeExact(getStringUtfRegion, env, string, 0, length, bytes);
            } catch (Throwable t) {
                throw new AssertionError("GetStringUTFRegion", t);
            }
        }

        void deleteLocalRef(MemorySegment reference) {
            try {
                REF_TO_VOID_MH.invokeExact(deleteLocalRef, env, reference);
            } catch (Throwable t) {
                throw new AssertionError("DeleteLocalRef", t);
            }
        }

        void deleteGlobalRef(MemorySegment reference) {
            try {
                REF_TO_VOID_MH.invokeExact(deleteGlobalRef, env, reference);
            } catch (Throwable t) {
                throw new AssertionError("DeleteGlobalRef", t);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Slot verification against the JDK's own jni.h
    // ---------------------------------------------------------------------------------------------

    private static Map<String, Integer> envSlots() {
        Map<String, Integer> slots = new LinkedHashMap<>();
        slots.put("GetVersion", GET_VERSION);
        slots.put("NewGlobalRef", NEW_GLOBAL_REF);
        slots.put("DeleteGlobalRef", DELETE_GLOBAL_REF);
        slots.put("DeleteLocalRef", DELETE_LOCAL_REF);
        slots.put("NewString", NEW_STRING);
        slots.put("GetStringLength", GET_STRING_LENGTH);
        slots.put("NewStringUTF", NEW_STRING_UTF);
        slots.put("GetStringUTFLength", GET_STRING_UTF_LENGTH);
        slots.put("GetStringRegion", GET_STRING_REGION);
        slots.put("GetStringUTFRegion", GET_STRING_UTF_REGION);
        return slots;
    }

    /**
     * Compares every slot constant with the member order of {@code java.home/include/jni.h}; a JDK without the
     * header (a runtime image) leaves the constants to the {@code GetVersion} and self-test checks.
     *
     * @return where the slot numbers were verified, for the test report
     * @throws IllegalStateException when the header disagrees with a constant
     */
    static String verifySlots() {
        Path header = Path.of(System.getProperty("java.home"), "include", "jni.h");
        if (!Files.isRegularFile(header)) {
            return "no " + header + ": slot numbers from the jni.h table of JDK 25 and 26, checked by GetVersion"
                    + " and the self-test string only";
        }
        String source;
        try {
            source = Files.readString(header, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String> envMembers = members(source, "JNINativeInterface_");
        List<String> vmMembers = members(source, "JNIInvokeInterface_");
        List<String> problems = new ArrayList<>();
        compare(problems, "JNINativeInterface_", envMembers, ENV_SLOTS);
        compare(problems, "JNIInvokeInterface_", vmMembers, VM_SLOTS);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(header + " disagrees with the slot table: " + problems);
        }
        return "slot numbers verified against " + header + " (JNINativeInterface_ " + envMembers.size()
                + " members, JNIInvokeInterface_ " + vmMembers.size() + " members)";
    }

    /** The member names of {@code struct <name> { ... };} in declaration order, one per pointer-sized slot. */
    private static List<String> members(String source, String struct) {
        Matcher block = Pattern.compile("struct\\s+" + struct + "\\s*\\{(.*?)\\n\\};", Pattern.DOTALL).matcher(source);
        if (!block.find()) {
            throw new IllegalStateException("no struct " + struct + " in jni.h");
        }
        String body = block.group(1).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        Pattern function = Pattern.compile("\\(\\s*JNICALL\\s*\\*\\s*(\\w+)\\s*\\)");
        Pattern reserved = Pattern.compile("void\\s*\\*\\s*(reserved\\d+)\\s*$");
        List<String> names = new ArrayList<>();
        for (String declaration : body.split(";")) {
            if (declaration.isBlank()) {
                continue;
            }
            Matcher f = function.matcher(declaration);
            Matcher r = reserved.matcher(declaration);
            if (f.find()) {
                names.add(f.group(1));
            } else if (r.find()) {
                names.add(r.group(1));
            } else {
                throw new IllegalStateException("unparsed member of " + struct + ": " + declaration.strip());
            }
        }
        return names;
    }

    private static void compare(List<String> problems, String struct, List<String> members,
                                Map<String, Integer> expected) {
        for (Map.Entry<String, Integer> slot : expected.entrySet()) {
            int actual = members.indexOf(slot.getKey());
            if (actual != slot.getValue()) {
                problems.add(struct + "." + slot.getKey() + " is slot " + actual + " in jni.h, " + slot.getValue()
                        + " in the table");
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The JVM library and the restricted java.lang.foreign calls
    // ---------------------------------------------------------------------------------------------

    /** The JVM library of the running JDK, under {@code java.home} in one of the layouts the JDK uses. */
    private static Path findLibjvm() {
        Path home = Path.of(System.getProperty("java.home"));
        String name = System.mapLibraryName("jvm");
        List<Path> candidates = new ArrayList<>();
        for (String directory : List.of("lib/server", "lib/client", "lib/minimal", "lib/zero", "bin/server",
                "bin/client", "lib", "bin")) {
            candidates.add(home.resolve(directory).resolve(name));
        }
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("no " + name + " under " + home + ": tried " + candidates);
    }

    /** The already loaded JVM library, opened once more for the duration of {@code arena}. */
    @SuppressWarnings("restricted")
    private static SymbolLookup lookup(Path library, Arena arena) {
        return SymbolLookup.libraryLookup(library, arena);
    }

    @SuppressWarnings("restricted")
    private static MethodHandle handle(FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(descriptor);
    }

    /**
     * Slot {@code index} of the function table a {@code JNIEnv*} or {@code JavaVM*} points at: the interface pointer
     * is the address of a pointer to the table, the table is an array of function pointers. Restricted, so never
     * called while a local reference is alive.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment slot(MemorySegment self, int index) {
        MemorySegment table = self.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
        return table.reinterpret((index + 1L) * ADDRESS.byteSize()).getAtIndex(ADDRESS, index);
    }
}

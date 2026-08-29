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
import com.sun.javafx.logging.PlatformLogger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * Exposes what is package private in {@link WebKitNative}, plus the two things the binding tests
 * need that no production code owns yet: the descriptor of every symbol the generated facades bind,
 * read straight out of their class files, and a {@code WKJHost} table to install.
 * <p>
 * <b>Why the descriptors are read from bytecode.</b> Test 4.3 of {@code FFM-TEST-PLAN.md} checks all
 * 1796 DOM descriptors against {@code buildtools/ffm-web/dom-abi.tsv}, and it has to work whether or
 * not a native library is present, because a descriptor mismatch is silent memory corruption rather
 * than a clean failure and is therefore the one check that must never be skipped. The generated
 * facades hold their {@link FunctionDescriptor}s inline in their {@code MethodHandle} initialisers,
 * so reading them at runtime would mean initialising 102 classes, which means loading the library.
 * Parsing the class files with the {@code java.lang.classfile} API reads exactly what {@code javac}
 * emitted, needs nothing loaded, and cannot drift from what the facade will actually bind.
 * <p>
 * <b>Why a second host table lives here.</b> {@code WebKitNative} owns the layout and installs the
 * production table from its class initializer. The table below is a second one with the same layout
 * whose targets delegate to the same registry operations, adding only a call log and a fault
 * injector: a test cannot see what a production target was passed, and cannot make one throw, and
 * both are needed to prove that C dispatches with the declared signature and that a {@link Throwable}
 * is contained rather than allowed to reach the boundary. Everything that does not need scaffolding
 * is tested against the production table instead.
 * <p>
 * Only one table can be installed at a time - {@code wkj_init} refuses the second - so
 * {@link #installHostTable} and {@link #installProductionHostTable} clear the library's current
 * table first and each test declares which one it wants.
 */
public final class WebKitNativeShim {

    private static final PlatformLogger LOGGER =
            PlatformLogger.getLogger(WebKitNativeShim.class.getName());

    private static final String MODULE_NAME = "javafx.web";

    private static final String VALUE_LAYOUT = "java/lang/foreign/ValueLayout";
    private static final String FUNCTION_DESCRIPTOR = "java/lang/foreign/FunctionDescriptor";
    private static final String WEBKIT_NATIVE = "com/sun/webkit/WebKitNative";

    /** Upcalls the installed table made, in order, as {@code slot(arg, ...)} text. */
    private static final List<String> UPCALLS = new CopyOnWriteArrayList<>();

    /** Throwables the upcall targets caught, which must never escape into C. */
    private static final List<String> CONTAINED = new CopyOnWriteArrayList<>();

    /** The thread each recorded upcall ran on, so that a foreign thread can be recognised. */
    private static final List<String> UPCALL_THREADS = new CopyOnWriteArrayList<>();

    private static volatile boolean failUpcalls;

    private static Map<String, String> descriptors;
    private static List<String> duplicateBindings;
    private static MemorySegment host;
    private static Boolean abiAvailable;
    private static String abiUnavailableReason;

    private WebKitNativeShim() {
    }

    // ------------------------------------------------------- descriptor metadata

    /**
     * Returns every symbol any facade binds, sorted, with no duplicates. Read from the class files,
     * so it needs no native library.
     *
     * @return the bound symbol names
     */
    public static List<String> boundSymbols() {
        return List.copyOf(descriptors().keySet());
    }

    /**
     * Returns the {@link FunctionDescriptor} a facade binds a symbol with, as a kind string: the
     * return kind followed by one kind per parameter, using the alphabet of
     * {@code FFM-TEST-PLAN.md} section 2.5 ({@code v b h i l f d p}).
     *
     * @param symbol the symbol name
     * @return the kind string, or {@code null} if no facade binds that symbol
     */
    public static String descriptorOf(String symbol) {
        return descriptors().get(symbol);
    }

    /**
     * Returns the symbols actually linked so far in this JVM, in binding order. Unlike
     * {@link #boundSymbols} this reflects which facade classes have been initialised.
     *
     * @return the linked symbol names
     */
    public static List<String> linkedSymbols() {
        return WebKitNative.boundSymbols();
    }

    /**
     * Returns the symbols more than one facade field binds. Binding a symbol twice is not in itself
     * wrong, but it is never intended by the generator, so it is worth failing on.
     *
     * @return the duplicated symbol names
     */
    public static List<String> duplicateBindings() {
        descriptors();
        return List.copyOf(duplicateBindings);
    }

    /**
     * Binds a symbol through the production helper, so that a test can assert what happens when the
     * library does not export it without calling {@code Linker} itself.
     *
     * @param name the symbol name
     * @throws UnsatisfiedLinkError if the library does not export it
     */
    public static void bindSymbol(String name) {
        WebKitNative.downcall(name, FunctionDescriptor.ofVoid());
    }

    private static synchronized Map<String, String> descriptors() {
        if (descriptors == null) {
            duplicateBindings = new ArrayList<>();
            descriptors = scanFacades(duplicateBindings);
        }
        return descriptors;
    }

    private static Map<String, String> scanFacades(List<String> duplicates) {
        Map<String, String> result = new TreeMap<>();
        ResolvedModule resolved = ModuleLayer.boot().configuration().findModule(MODULE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        MODULE_NAME + " is not in the boot layer, so its facades cannot be read"));
        ModuleReference reference = resolved.reference();
        try (ModuleReader reader = reference.open()) {
            List<String> entries;
            try (var names = reader.list()) {
                entries = names.filter(name -> name.endsWith("Native.class")).sorted().toList();
            }
            for (String entry : entries) {
                Optional<ByteBuffer> bytes = reader.read(entry);
                if (bytes.isEmpty()) {
                    continue;
                }
                try {
                    scanClass(toArray(bytes.get()), result, duplicates);
                } finally {
                    reader.release(bytes.get());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the " + MODULE_NAME + " module content", e);
        }
        return result;
    }

    private static byte[] toArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    /*
     * Walks the instruction stream and rebuilds each binding from the three things javac emits for
     * it, in this order: an ldc of the symbol name, one getstatic per ValueLayout constant, and the
     * FunctionDescriptor.of or ofVoid call that consumes them. The generated facades emit exactly
     * that shape, one binding per field, so a linear pass is enough and no stack modelling is needed.
     * An unrecognised layout constant is fatal rather than ignored: silently dropping one would make
     * a descriptor look shorter than it is, which is the very error this metadata exists to catch.
     */
    private static void scanClass(byte[] bytes, Map<String, String> out, List<String> duplicates) {
        ClassModel model = ClassFile.of().parse(bytes);
        for (MethodModel method : model.methods()) {
            Optional<CodeModel> code = method.code();
            if (code.isEmpty()) {
                continue;
            }
            String pendingName = null;
            String pendingKinds = null;
            StringBuilder layouts = new StringBuilder();
            for (CodeElement element : code.get()) {
                if (element instanceof ConstantInstruction constant
                        && constant.constantValue() instanceof String text
                        && text.startsWith("wkj_")) {
                    pendingName = text;
                    pendingKinds = null;
                    layouts.setLength(0);
                } else if (element instanceof FieldInstruction field
                        && field.opcode() == Opcode.GETSTATIC
                        && VALUE_LAYOUT.equals(field.owner().asInternalName())
                        && pendingName != null) {
                    layouts.append(kindOf(field.name().stringValue()));
                } else if (element instanceof InvokeInstruction invoke && pendingName != null) {
                    String owner = invoke.owner().asInternalName();
                    String name = invoke.name().stringValue();
                    if (FUNCTION_DESCRIPTOR.equals(owner) && "of".equals(name)) {
                        pendingKinds = layouts.toString();
                        layouts.setLength(0);
                    } else if (FUNCTION_DESCRIPTOR.equals(owner) && "ofVoid".equals(name)) {
                        pendingKinds = "v" + layouts;
                        layouts.setLength(0);
                    } else if (WEBKIT_NATIVE.equals(owner) && isBindingCall(name)
                            && pendingKinds != null) {
                        if (out.put(pendingName, pendingKinds) != null) {
                            duplicates.add(pendingName);
                        }
                        pendingName = null;
                        pendingKinds = null;
                    }
                }
            }
        }
    }

    private static boolean isBindingCall(String name) {
        return "downcall".equals(name) || "downcallOptional".equals(name)
                || "bind".equals(name) || "bindOptional".equals(name);
    }

    private static char kindOf(String valueLayoutConstant) {
        return switch (valueLayoutConstant) {
            case "ADDRESS" -> 'p';
            case "JAVA_BYTE" -> 'b';
            case "JAVA_SHORT", "JAVA_CHAR" -> 'h';
            case "JAVA_INT" -> 'i';
            case "JAVA_LONG" -> 'l';
            case "JAVA_FLOAT" -> 'f';
            case "JAVA_DOUBLE" -> 'd';
            default -> throw new IllegalStateException(
                    "a facade binds with an unrecognised layout constant ValueLayout."
                            + valueLayoutConstant);
        };
    }

    // ---------------------------------------------------------- library and ABI

    /**
     * Returns the ABI version javafx.web is written against.
     *
     * @return {@code WKJ_ABI_VERSION}
     */
    public static int abiVersionExpected() {
        return WebKitNative.WKJ_ABI_VERSION;
    }

    /**
     * Returns the name of the library the facades bind, which the
     * {@code javafx.web.nativeLibrary} property overrides for the binding tests.
     *
     * @return the library name
     */
    public static String libraryName() {
        return WebKitNative.libraryName();
    }

    /**
     * Returns the sentence an ABI version mismatch fails with, without loading anything.
     *
     * @param expected the version javafx.web requires
     * @param actual the version the library reports
     * @return the message
     */
    public static String abiVersionMessage(int expected, int actual) {
        return WebKitNative.abiVersionMessage(expected, actual);
    }

    /**
     * Returns the sentence a library with no {@code wkj_*} ABI at all fails with.
     *
     * @return the message
     */
    public static String abiMissingMessage() {
        return WebKitNative.abiMissingMessage();
    }

    /**
     * Reports whether the library the production path loads exports a matching {@code wkj_*} ABI.
     * The probe is a symbol lookup rather than a broad {@code try new WebPage()}, which would also
     * swallow a real initialisation bug and skip a suite for the wrong reason.
     *
     * @return true if the loaded library carries this ABI version
     */
    public static synchronized boolean abiAvailable() {
        if (abiAvailable != null) {
            return abiAvailable;
        }
        abiAvailable = Boolean.FALSE;
        String name = WebKitNative.libraryName();
        try {
            NativeLibLoader.loadLibrary(name);
        } catch (Throwable t) {
            abiUnavailableReason = name + " could not be loaded: " + t;
            return false;
        }
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        if (lookup.find("wkj_abi_version").isEmpty()) {
            abiUnavailableReason = WebKitNative.abiMissingMessage();
            return false;
        }
        int actual = probeAbiVersion(lookup);
        if (actual != WebKitNative.WKJ_ABI_VERSION) {
            abiUnavailableReason = WebKitNative.abiVersionMessage(WebKitNative.WKJ_ABI_VERSION, actual);
            return false;
        }
        abiAvailable = Boolean.TRUE;
        return true;
    }

    /**
     * Returns why {@link #abiAvailable} is false.
     *
     * @return the reason, or a note that the ABI is present
     */
    public static synchronized String abiUnavailableReason() {
        return abiUnavailableReason == null
                ? WebKitNative.libraryName() + " exports a matching wkj_* ABI"
                : abiUnavailableReason;
    }

    @SuppressWarnings("restricted")
    private static int probeAbiVersion(SymbolLookup lookup) {
        MemorySegment symbol = lookup.find("wkj_abi_version").orElseThrow();
        MethodHandle handle = java.lang.foreign.Linker.nativeLinker()
                .downcallHandle(symbol, FunctionDescriptor.of(JAVA_INT));
        try {
            return (int) handle.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    // ------------------------------------------------------------------ layouts

    /**
     * Returns the layout of {@code WKJExceptionSlot} that {@link WebKitNative} reads the pending
     * exception through.
     *
     * @return the exception slot layout
     */
    public static MemoryLayout exceptionSlotLayout() {
        return WebKitNative.exceptionSlotLayout();
    }

    /**
     * Returns the layout of {@code WKJHost} that production builds its table from.
     *
     * @return the host table layout
     */
    public static MemoryLayout hostLayout() {
        return WebKitNative.hostLayout();
    }

    /**
     * Returns every struct layout Java declares for this ABI, keyed by its C name. The layout test
     * drives itself from the C tables and asserts that this map covers them exactly, so a struct
     * added to the header with no Java layout fails rather than being silently ignored.
     *
     * @return the declared layouts
     */
    public static Map<String, MemoryLayout> declaredLayouts() {
        return WebKitNative.declaredLayouts();
    }

    /**
     * Returns the byte offset the Java layout computes for a dotted host slot path, for example
     * {@code core.release}.
     *
     * @param dottedPath the slot path
     * @return the offset in bytes
     */
    public static long hostSlotOffset(String dottedPath) {
        return WebKitNative.hostSlotOffset(dottedPath);
    }

    /**
     * Returns the shape production bound each filled {@code WKJHost} slot with, in the stub's own
     * kind notation - the return kind followed by one kind per parameter, from {@code v b h i l f d
     * p} - so that a test can compare it with the signature the library derives from the C
     * prototype without naming a {@code java.lang.foreign} type.
     *
     * @return the signature per dotted slot path, in installation order
     */
    public static Map<String, String> hostSlotSignatures() {
        Map<String, String> signatures = new LinkedHashMap<>();
        WebKitNative.hostSlotDescriptors().forEach(
                (slot, descriptor) -> signatures.put(slot, signatureOf(descriptor)));
        return signatures;
    }

    /*
     * The kinds are wkjstub.h's, which is also gen-wkjstub.pl's mapping from a C type: void has no
     * layout at all, and every other kind is one value layout. A descriptor that carries anything
     * else - a struct by value, say - has no kind, and saying so is better than guessing.
     */
    private static String signatureOf(FunctionDescriptor descriptor) {
        StringBuilder signature = new StringBuilder();
        signature.append(descriptor.returnLayout().map(WebKitNativeShim::kindOf).orElse('v'));
        for (MemoryLayout parameter : descriptor.argumentLayouts()) {
            signature.append(kindOf(parameter));
        }
        return signature.toString();
    }

    private static char kindOf(MemoryLayout layout) {
        if (layout instanceof AddressLayout) {
            return 'p';
        }
        if (JAVA_BYTE.equals(layout)) {
            return 'b';
        }
        if (JAVA_SHORT.equals(layout)) {
            return 'h';
        }
        if (JAVA_INT.equals(layout)) {
            return 'i';
        }
        if (JAVA_LONG.equals(layout)) {
            return 'l';
        }
        if (JAVA_FLOAT.equals(layout)) {
            return 'f';
        }
        if (JAVA_DOUBLE.equals(layout)) {
            return 'd';
        }
        throw new IllegalArgumentException("no wkjstub kind for the layout " + layout);
    }

    // --------------------------------------------------------------- host table

    /**
     * Installs the recording table, mapping {@code wkj_init}'s result codes to the failures
     * production uses: an ABI version mismatch is an {@link UnsatisfiedLinkError} carrying the
     * guard's message (contract section 5), anything else is an {@link IllegalStateException}.
     * <p>
     * The library's currently installed table is dropped first, because {@link WebKitNative}
     * installs the production one from its own class initializer and {@code wkj_init} refuses to
     * install a second table over a first. Clearing makes this deterministic wherever it is called
     * from, which is what a {@code @BeforeEach} needs.
     */
    public static void installHostTable() {
        // Building the recording table initializes WebKitNative, whose class initializer installs
        // the production table. That has to happen before the clear, or the clear would be undone
        // by it and this call would be the second install rather than the first.
        hostTable();
        WkjStubShim.clearHost();
        int result = callWkjInit(hostByteSize(), hostByteSize(), abiVersionExpected());
        if (result == 0 || result == -4) {
            return;
        }
        if (result == -2) {
            throw new UnsatisfiedLinkError(abiVersionMessage(abiVersionExpected(), actualAbiVersion()));
        }
        throw new IllegalStateException("wkj_init rejected the host table with code " + result);
    }

    /**
     * Installs the table {@link WebKitNative} built and filled, in place of the recording one, so
     * that a test can drive the production upcall targets rather than the shim's copies of them.
     * Production installed this same table once already, from its class initializer; the library is
     * cleared first for the reason {@link #installHostTable} gives.
     */
    public static void installProductionHostTable() {
        // Reading the table first, for the reason installHostTable gives: touching WebKitNative at
        // all may be what installs it, and that must not happen after the clear.
        MemorySegment table = WebKitNative.hostTable();
        int size = WebKitNative.hostByteSize();
        WkjStubShim.clearHost();
        int result = WebKitNative.callWkjInit(table, size, abiVersionExpected());
        if (result != 0) {
            throw new IllegalStateException("wkj_init rejected the production host table with code "
                    + result);
        }
    }

    /**
     * Returns the code {@code wkj_init} returned when {@link WebKitNative} installed its table, at
     * class initialization time and before any other {@code wkj_*} call could be made.
     *
     * @return the {@code WKJ_INIT_*} result code
     */
    public static int productionHostInitResult() {
        return WebKitNative.hostInitResultForTesting();
    }

    /**
     * Returns the {@code size} field production wrote into its own {@code WKJHost} table, which the
     * library checks against its {@code sizeof(WKJHost)} and against the {@code host_size} argument.
     *
     * @return the size the table declares for itself
     */
    public static int productionHostSizeField() {
        return WebKitNative.hostTable().get(JAVA_INT, 0L);
    }

    /**
     * Calls {@code wkj_init} with the arguments given and returns its result code, throwing nothing.
     * The {@code size} field of the recording table is set to {@code sizeField} for the duration of
     * the call and restored afterwards, so that the library's "the caller's struct is not my struct"
     * guard can be driven without corrupting the table for later tests.
     *
     * @param sizeField the value to write into {@code WKJHost.size}
     * @param hostSizeArgument the {@code host_size} argument
     * @param abiVersionArgument the {@code abi_version} argument
     * @return the {@code WKJ_INIT_*} result code
     */
    public static synchronized int callWkjInit(int sizeField, int hostSizeArgument,
                                               int abiVersionArgument) {
        MemorySegment table = hostTable();
        int previous = table.get(JAVA_INT, 0L);
        table.set(JAVA_INT, 0L, sizeField);
        try {
            return WebKitNative.callWkjInit(table, hostSizeArgument, abiVersionArgument);
        } finally {
            table.set(JAVA_INT, 0L, previous);
        }
    }

    /**
     * Returns {@code sizeof(WKJHost)} as the Java layout computes it.
     *
     * @return the host table size in bytes
     */
    public static int hostByteSize() {
        return WebKitNative.hostByteSize();
    }

    /**
     * Returns the ABI version the loaded library reports right now, which the version guard test
     * changes underneath the library.
     *
     * @return the reported version
     */
    public static int actualAbiVersion() {
        return probeAbiVersion(SymbolLookup.loaderLookup());
    }

    /**
     * Returns the upcalls the installed targets have recorded, oldest first.
     *
     * @return the recorded upcalls
     */
    public static List<String> upcalls() {
        return List.copyOf(UPCALLS);
    }

    /**
     * Returns the name of the thread each recorded upcall ran on, in the same order as
     * {@link #upcalls}.
     *
     * @return the thread names
     */
    public static List<String> upcallThreads() {
        return List.copyOf(UPCALL_THREADS);
    }

    /** Forgets the recorded upcalls and contained throwables. */
    public static void clearUpcalls() {
        UPCALLS.clear();
        UPCALL_THREADS.clear();
        CONTAINED.clear();
    }

    /**
     * Returns the throwables the upcall targets caught. An empty list is the normal state; anything
     * in it was stopped from crossing the boundary, which would have terminated the JVM.
     *
     * @return the contained failures
     */
    public static List<String> containedFailures() {
        return List.copyOf(CONTAINED);
    }

    /**
     * Makes every upcall target throw, so that the containment rule can be tested.
     *
     * @param fail true to make the targets throw
     */
    public static void setUpcallFailure(boolean fail) {
        failUpcalls = fail;
    }

    private static synchronized MemorySegment hostTable() {
        if (host == null) {
            host = Arena.global().allocate(WebKitNative.hostLayout());
            host.set(JAVA_INT, 0L, hostByteSize());
            installSlot("core.retain", "retain", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG),
                    MethodType.methodType(long.class, long.class));
            installSlot("core.retain_weak", "retainWeak",
                    FunctionDescriptor.of(JAVA_LONG, JAVA_LONG),
                    MethodType.methodType(long.class, long.class));
            installSlot("core.release", "release", FunctionDescriptor.ofVoid(JAVA_LONG),
                    MethodType.methodType(void.class, long.class));
            installSlot("core.is_live", "isLive", FunctionDescriptor.of(JAVA_INT, JAVA_LONG),
                    MethodType.methodType(int.class, long.class));
            installSlot("core.hash_code", "hashCodeOf", FunctionDescriptor.of(JAVA_INT, JAVA_LONG),
                    MethodType.methodType(int.class, long.class));
            installSlot("core.equals", "equalsOf",
                    FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG),
                    MethodType.methodType(int.class, long.class, long.class));
            installSlot("core.check_and_clear_exception", "checkAndClearException",
                    FunctionDescriptor.of(JAVA_INT), MethodType.methodType(int.class));
        }
        return host;
    }

    private static void installSlot(String dottedPath, String method, FunctionDescriptor descriptor,
                                    MethodType type) {
        MethodHandle target;
        try {
            target = MethodHandles.lookup().findStatic(WebKitNativeShim.class, method, type);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        host.set(ADDRESS, hostSlotOffset(dottedPath), WebKitNative.upcallStub(target, descriptor));
    }

    // ------------------------------------------------------------ upcall targets

    /*
     * Every target below catches Throwable, logs it and returns the default the C header documents
     * for a NULL slot. An exception escaping an upcall stub terminates the JVM, so this is not a
     * style choice: it is the only thing standing between a Java bug and a dead process.
     */

    private static long retain(long ref) {
        try {
            record("core.retain", ref);
            return WebKitNative.retain(ref);
        } catch (Throwable t) {
            return contain("core.retain", t, 0L);
        }
    }

    private static long retainWeak(long ref) {
        try {
            record("core.retain_weak", ref);
            return WebKitNative.retainWeak(ref);
        } catch (Throwable t) {
            return contain("core.retain_weak", t, 0L);
        }
    }

    private static void release(long ref) {
        try {
            record("core.release", ref);
            WebKitNative.release(ref);
        } catch (Throwable t) {
            contain("core.release", t, 0L);
        }
    }

    private static int isLive(long ref) {
        try {
            record("core.is_live", ref);
            return WebKitNative.isLive(ref) ? 1 : 0;
        } catch (Throwable t) {
            return (int) contain("core.is_live", t, 0L);
        }
    }

    private static int hashCodeOf(long ref) {
        try {
            record("core.hash_code", ref);
            Object object = WebKitNative.lookup(ref);
            return object == null ? 0 : object.hashCode();
        } catch (Throwable t) {
            return (int) contain("core.hash_code", t, 0L);
        }
    }

    private static int equalsOf(long a, long b) {
        try {
            record("core.equals", a, b);
            Object first = WebKitNative.lookup(a);
            Object second = WebKitNative.lookup(b);
            return first != null && first.equals(second) ? 1 : 0;
        } catch (Throwable t) {
            return (int) contain("core.equals", t, a == b ? 1L : 0L);
        }
    }

    private static int checkAndClearException() {
        try {
            record("core.check_and_clear_exception");
            boolean failed = !CONTAINED.isEmpty();
            CONTAINED.clear();
            return failed ? 1 : 0;
        } catch (Throwable t) {
            return (int) contain("core.check_and_clear_exception", t, 0L);
        }
    }

    private static void record(String slot, long... args) {
        StringBuilder text = new StringBuilder(slot).append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(args[i]);
        }
        UPCALLS.add(text.append(')').toString());
        UPCALL_THREADS.add(Thread.currentThread().getName());
        if (failUpcalls) {
            throw new IllegalStateException("deliberate failure inside the " + slot + " upcall target");
        }
    }

    private static long contain(String slot, Throwable t, long fallback) {
        CONTAINED.add(slot + ": " + t);
        LOGGER.severe("javafx.web upcall target " + slot + " failed and was contained", t);
        return fallback;
    }

    // --------------------------------------------------------------- registry

    /**
     * Returns the number of live entries in the {@code wkj_ref} registry.
     *
     * @return the registry size
     */
    public static int registrySize() {
        return WebKitNative.registrySizeForTesting();
    }

    /**
     * Registers an object and returns its {@code wkj_ref}.
     *
     * @param o the object, may be {@code null}
     * @return the id, zero for {@code null}
     */
    public static long register(Object o) {
        return WebKitNative.register(o);
    }

    /**
     * Resolves a {@code wkj_ref}.
     *
     * @param id the id
     * @return the object, or {@code null}
     */
    public static Object lookup(long id) {
        return WebKitNative.lookup(id);
    }

    /**
     * Drops a {@code wkj_ref}.
     *
     * @param id the id
     */
    public static void unregister(long id) {
        WebKitNative.unregister(id);
    }

    /**
     * Adds a reference to a {@code wkj_ref}, as {@code WKJHostCore::retain} does.
     *
     * @param id the id
     * @return an id the caller owns, or zero
     */
    public static long retainRef(long id) {
        return WebKitNative.retain(id);
    }

    /**
     * Mints a weak {@code wkj_ref}, as {@code WKJHostCore::retain_weak} does.
     *
     * @param id the id
     * @return a weak id the caller owns, or zero
     */
    public static long retainWeakRef(long id) {
        return WebKitNative.retainWeak(id);
    }

    /**
     * Drops one reference to a {@code wkj_ref}, as {@code WKJHostCore::release} does.
     *
     * @param id the id
     */
    public static void releaseRef(long id) {
        WebKitNative.release(id);
    }

    /**
     * Reports whether a {@code wkj_ref} still names a live object.
     *
     * @param id the id
     * @return true while the object is there
     */
    public static boolean isRefLive(long id) {
        return WebKitNative.isLive(id);
    }

    /**
     * Returns the number of unreleased ids for a registry entry.
     *
     * @param id the id
     * @return the reference count, zero if the id is not registered
     */
    public static int referenceCount(long id) {
        return WebKitNative.referenceCountForTesting(id);
    }

    /**
     * Reads and clears this thread's upcall failure flag, which is what
     * {@code WKJHostCore::check_and_clear_exception} does.
     *
     * @return 1 if an upcall on this thread failed since the last check, 0 otherwise
     */
    public static int checkAndClearUpcallFailure() {
        return WebKitNative.checkAndClearUpcallFailure();
    }

    // ------------------------------------------------------------- string codec

    /**
     * Encodes a string the way a facade encodes a {@code const uint16_t*, int32_t} argument pair and
     * decodes it back, without a downcall. A {@code null} stays {@code null} and an empty string
     * stays empty, which is the distinction the ABI carries inbound even though the library collapses
     * the two (contract section 11.1).
     *
     * @param s the string, may be {@code null}
     * @return the round tripped string
     */
    public static String toNativeAndBack(String s) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = WebKitNative.allocString(arena, s);
            int length = WebKitNative.stringLength(s);
            if (segment.address() == 0L) {
                return null;
            }
            if (length == 0) {
                return "";
            }
            char[] chars = new char[length];
            MemorySegment.copy(segment, java.lang.foreign.ValueLayout.JAVA_CHAR, 0L, chars, 0, length);
            return new String(chars);
        }
    }

    /**
     * Runs the post call exception check directly.
     */
    public static void checkPendingException() {
        WebKitNative.checkException();
    }
}

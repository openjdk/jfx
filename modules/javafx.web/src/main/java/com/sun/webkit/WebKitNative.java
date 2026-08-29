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
import com.sun.webkit.graphics.GraphicsUpcalls;
import com.sun.webkit.graphics.MediaUpcalls;
import com.sun.webkit.graphics.RenderThemeUpcalls;
import com.sun.webkit.network.NetworkUpcalls;
import com.sun.webkit.security.PalUpcalls;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.w3c.dom.DOMException;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The single point of contact between {@code javafx.web} and the {@code wkj_*} C ABI exported by the
 * {@code jfxwebkit} library. It owns the library load, the {@link SymbolLookup}, the {@link Linker}, the
 * downcall helpers, the UTF-16 string codec, the thread-local exception slot, the Java object registry
 * and the process-wide upcall arena.
 * <p>
 * Every restricted {@code java.lang.foreign} operation used by this module lives in this class; the
 * per-type {@code *Native} facades only build {@link FunctionDescriptor}s and call the helpers here.
 * See {@code modules/javafx.web/FFM-ABI-CONTRACT.md} for the ABI this class implements.
 * <p>
 * This class is not public API: the {@code com.sun.webkit} package is not exported by the
 * {@code javafx.web} module, so it is reachable only from within the module.
 */
public final class WebKitNative {

    /**
     * The {@code wkj_*} ABI revision this Java code is written against. It must match the value returned
     * by {@code wkj_abi_version()} in the loaded {@code jfxwebkit} library (contract section 5).
     */
    public static final int WKJ_ABI_VERSION = 1;

    /**
     * The library that carries the {@code wkj_*} ABI. It is {@code jfxwebkit} in production; the
     * property exists so that the module's own binding tests can point the whole facade layer at the
     * {@code wkjstub} recording library instead (test plan section 3.1). Naming the stub
     * {@code jfxwebkit} and shadowing the real one on the library path was rejected: it makes "which
     * library did I just test?" unanswerable from a log.
     */
    private static final String LIBRARY_NAME = System.getProperty("javafx.web.nativeLibrary", "jfxwebkit");

    private static final String ABI_VERSION_SYMBOL = "wkj_abi_version";
    private static final String EXCEPTION_SLOT_SYMBOL = "wkj_exception_slot";
    private static final String INIT_SYMBOL = "wkj_init";

    /*
     * Exception slot type codes. The ordering follows JavaExceptionType in JavaDOMUtils.h, shifted by one
     * because zero means "no exception pending". Only WKJ_EXC_DOM is ever raised: every raise path in this
     * tree goes through raiseDOMErrorException, which builds one org.w3c.dom.DOMException. The other three
     * names exist because the JNI enum named them, and they all decode to that same exception; inventing an
     * EventException or a RangeException here would throw something the JNI bindings never threw
     * (contract section 13.1, finding 8).
     */
    private static final int WKJ_EXC_NONE = 0;
    private static final int WKJ_EXC_DOM = 1;
    private static final int WKJ_EXC_EVENT = 2;
    private static final int WKJ_EXC_RANGE = 3;
    private static final int WKJ_EXC_UNDEFINED = 4;

    /** Capacity of the inline message buffer, in UTF-16 code units ({@code WKJ_EXC_MESSAGE_MAX}). */
    private static final int WKJ_EXC_MESSAGE_MAX = WKJLayouts.EXCEPTION_MESSAGE_MAX;

    /**
     * {@code WKJExceptionSlot} as {@code webkit_java_api.h} declares it (contract section 13). The shape
     * lives in {@link WKJLayouts} with every other struct of this ABI, so that one test compares all of
     * them against the C {@code sizeof} and {@code offsetof} rather than each facade minding its own.
     */
    private static final StructLayout EXCEPTION_SLOT_LAYOUT = WKJLayouts.EXCEPTION_SLOT;

    private static final long OFFSET_TYPE =
            EXCEPTION_SLOT_LAYOUT.byteOffset(PathElement.groupElement("type"));
    private static final long OFFSET_CODE =
            EXCEPTION_SLOT_LAYOUT.byteOffset(PathElement.groupElement("code"));
    private static final long OFFSET_MESSAGE =
            EXCEPTION_SLOT_LAYOUT.byteOffset(PathElement.groupElement("message"));
    private static final long OFFSET_MESSAGE_LENGTH =
            EXCEPTION_SLOT_LAYOUT.byteOffset(PathElement.groupElement("message_length"));

    /* The {@code wkj_init} result codes of webkit_java_api.h. */
    private static final int WKJ_INIT_OK = 0;
    private static final int WKJ_INIT_ERR_NULL_HOST = -1;
    private static final int WKJ_INIT_ERR_ABI_VERSION = -2;
    private static final int WKJ_INIT_ERR_HOST_SIZE = -3;
    private static final int WKJ_INIT_ERR_ALREADY_INITED = -4;

    /**
     * {@code WKJHost}: an {@code int32_t}, four bytes of padding and sixteen groups, of which seven
     * are still one-pointer placeholders and nine carry real slots. Its shape, and the shape of
     * every group in it, is declared once in {@link WKJLayouts}.
     */
    private static final MemoryLayout HOST_LAYOUT = WKJLayouts.HOST;

    private static final long OFFSET_HOST_SIZE =
            HOST_LAYOUT.byteOffset(PathElement.groupElement("size"));

    private static final Linker LINKER = Linker.nativeLinker();

    /** Names of the symbols bound so far, in binding order; read by the symbol resolution smoke test. */
    private static final List<String> BOUND_SYMBOLS = Collections.synchronizedList(new ArrayList<>());

    /**
     * The descriptor each filled {@code WKJHost} slot was bound with, in installation order. It
     * exists so that the layout test can compare all 159 of them against the C prototypes the
     * library reports, which is the only check that catches a descriptor naming the right slot with
     * the wrong shape.
     */
    private static final Map<String, FunctionDescriptor> HOST_SLOT_DESCRIPTORS =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /*
     * The one and only arena backing upcall stubs. The host table installed by wkj_init is created exactly
     * once per process and the library retains it for as long as it is loaded, so its stubs outlive every
     * object that uses them. That is the case the migration playbook allows a shared, never closed arena
     * for; per object stubs must never be allocated here.
     */
    private static final Arena UPCALL_ARENA = Arena.ofShared();

    private static final PlatformLogger LOGGER = PlatformLogger.getLogger(WebKitNative.class.getName());

    /**
     * The {@code wkj_ref} registry: one {@link Entry} per id, each carrying its own reference count.
     * There is no lock over the map. An id is looked up with one {@link ConcurrentHashMap} read and
     * its count is changed under that entry's own monitor, so two threads working on two ids never
     * contend - which matters because these calls arrive on WebKit's main thread, on its network
     * threads and on JavaScript worker threads at the same time.
     */
    private static final ConcurrentHashMap<Long, Entry> REGISTRY = new ConcurrentHashMap<>();

    /** Registry ids are Java assigned and monotonic; zero is reserved for null (contract section 3). */
    private static final AtomicLong NEXT_REF = new AtomicLong(1L);

    /**
     * Whether the last upcall on this thread ended in a {@link Throwable}, which is what
     * {@code WKJHostCore::check_and_clear_exception} reports and clears. It is per thread because
     * the JNI pending exception it replaces was per thread: {@code WTF::CheckAndClearException(env)}
     * asked the calling thread's environment, and about a dozen of its call sites branch on the
     * answer. The flag is a one element array rather than a {@code Boolean} so that setting it does
     * not have to write back through {@link ThreadLocal#set}, which allocates on a failure path that
     * may already be handling an {@link OutOfMemoryError}.
     */
    private static final ThreadLocal<boolean[]> UPCALL_FAILED =
            ThreadLocal.withInitial(() -> new boolean[1]);

    private static final SymbolLookup LOOKUP;
    private static final MethodHandle WKJ_EXCEPTION_SLOT;
    private static final MethodHandle WKJ_INIT;

    /**
     * The one {@code WKJHost} table of this process, or {@link MemorySegment#NULL} if the library
     * could not be loaded. It is allocated in {@link #UPCALL_ARENA} and never freed, because the
     * library keeps the pointer for as long as it is loaded.
     */
    private static final MemorySegment HOST;

    /** The code {@code wkj_init} returned at class initialization; read by the host table test. */
    private static final int HOST_INIT_RESULT;

    /**
     * The failure that loading or version checking the library ended in, or {@code null}. Caching it is
     * what keeps the one readable sentence readable: an error escaping a static initializer is reported
     * once as an {@code ExceptionInInitializerError}, and every later touch of the class gets a bare
     * {@code NoClassDefFoundError} naming nothing (contract section 13.1, finding 11). Every entry point
     * below rethrows this instance instead, so the tenth caller sees the same message as the first.
     */
    private static final Error INIT_FAILURE;

    /**
     * The exception slot pointer of a platform thread, fetched once and reinterpreted to the struct size.
     * A virtual thread must not use this cache: {@code wkj_exception_slot()} returns the slot of the thread
     * that calls it, which for a virtual thread is its current carrier, so a virtual thread that unmounted
     * and remounted elsewhere would read another thread's slot (contract section 13.1, finding 10).
     */
    private static final ThreadLocal<MemorySegment> EXCEPTION_SLOT =
            ThreadLocal.withInitial(WebKitNative::fetchExceptionSlot);

    static {
        // Load the library exactly the way com.sun.webkit.WebPage loads it, through the same loader:
        // NativeLibLoader is synchronized and remembers the libraries it has loaded, so whichever of the
        // two classes is initialized first performs the load and the other one is a no-op. WebPage keeps
        // its own load call and the WebCore initialization that follows it, so the ordering it relies on
        // is unchanged.
        //
        // The host table is installed here, at the end of this static initializer, and nowhere else.
        // That is the earliest point at which it can be installed and it is early enough, because
        // every downcall handle in this module is created by WebKitNative.downcall: touching any
        // facade at all runs this initializer to completion first, so no wkj_* function can be
        // called, and therefore no callback can be fired, before wkj_init has returned. In
        // particular WebPage's own static block reaches wkj_set_startup_options through
        // WebPageNative, whose class initializer calls downcall, so the table is in place before
        // WebCore starts up; and wkj_page_create, which installs a page's callback tables as the
        // tail of page creation, is later still. Installing from WebPage instead would leave every
        // other facade - the DOM, the back-forward list, the network stack - able to call in first.
        SymbolLookup lookup = null;
        MethodHandle exceptionSlot = null;
        MethodHandle init = null;
        MemorySegment host = MemorySegment.NULL;
        int initResult = WKJ_INIT_ERR_NULL_HOST;
        Error failure = null;
        try {
            NativeLibLoader.loadLibrary(LIBRARY_NAME);
            lookup = SymbolLookup.loaderLookup();
            checkAbiVersion(lookup);
            exceptionSlot = bind(lookup, EXCEPTION_SLOT_SYMBOL, FunctionDescriptor.of(ADDRESS));
            init = bind(lookup, INIT_SYMBOL,
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
            host = buildHostTable();
            initResult = callWkjInit(init, host, hostByteSize(), WKJ_ABI_VERSION);
            if (initResult != WKJ_INIT_OK) {
                throw new UnsatisfiedLinkError(initFailureMessage(initResult));
            }
        } catch (Error e) {
            failure = e;
        } catch (RuntimeException e) {
            UnsatisfiedLinkError error = new UnsatisfiedLinkError(
                    "javafx.web could not initialize " + LIBRARY_NAME + ": " + e);
            error.initCause(e);
            failure = error;
        }
        LOOKUP = lookup;
        WKJ_EXCEPTION_SLOT = exceptionSlot;
        WKJ_INIT = init;
        HOST = host;
        HOST_INIT_RESULT = initResult;
        INIT_FAILURE = failure;
    }

    private WebKitNative() {
    }

    /**
     * Binds an exported {@code wkj_*} symbol.
     *
     * @param name the exported symbol name
     * @param descriptor the descriptor matching the C prototype
     * @param options linker options
     * @return the downcall handle
     * @throws UnsatisfiedLinkError if the library does not export {@code name}
     */
    public static MethodHandle downcall(String name, FunctionDescriptor descriptor,
                                        Linker.Option... options) {
        requireLibrary();
        return bind(LOOKUP, name, descriptor, options);
    }

    /**
     * Rethrows the failure the library load ended in, so that a caller of any entry point below sees the
     * same message the first caller saw rather than a bare {@code NoClassDefFoundError}.
     *
     * @throws Error the cached load or ABI version failure, if there was one
     */
    private static void requireLibrary() {
        if (INIT_FAILURE != null) {
            throw INIT_FAILURE;
        }
    }

    @SuppressWarnings("restricted")
    private static MethodHandle bind(SymbolLookup lookup, String name, FunctionDescriptor descriptor,
                                     Linker.Option... options) {
        MemorySegment symbol = lookup.find(name)
                .orElseThrow(() -> new UnsatisfiedLinkError("missing native symbol: " + name));
        MethodHandle handle = LINKER.downcallHandle(symbol, descriptor, options);
        BOUND_SYMBOLS.add(name);
        return handle;
    }

    /**
     * Binds a symbol that may legitimately be absent, for example one that only exists in a build with
     * test hooks enabled.
     *
     * @param name the exported symbol name
     * @param descriptor the descriptor matching the C prototype
     * @param options linker options
     * @return the downcall handle, or {@code null} if the library does not export {@code name}
     */
    public static MethodHandle downcallOptional(String name, FunctionDescriptor descriptor,
                                                Linker.Option... options) {
        requireLibrary();
        return bindOptional(LOOKUP, name, descriptor, options);
    }

    @SuppressWarnings("restricted")
    private static MethodHandle bindOptional(SymbolLookup lookup, String name, FunctionDescriptor descriptor,
                                             Linker.Option... options) {
        Optional<MemorySegment> symbol = lookup.find(name);
        if (symbol.isEmpty()) {
            return null;
        }
        MethodHandle handle = LINKER.downcallHandle(symbol.get(), descriptor, options);
        BOUND_SYMBOLS.add(name);
        return handle;
    }

    /**
     * Returns the names of the symbols bound so far, in binding order.
     *
     * @return a snapshot of the bound symbol names
     */
    static List<String> boundSymbols() {
        synchronized (BOUND_SYMBOLS) {
            return List.copyOf(BOUND_SYMBOLS);
        }
    }

    /**
     * Allocates the UTF-16 form of a string for a {@code const uint16_t* s, int32_t s_len} parameter pair.
     * A {@code null} string becomes {@link MemorySegment#NULL}, an empty string becomes a non null segment
     * whose {@link #stringLength} is zero; that distinction is load bearing across the DOM ABI.
     *
     * @param allocator the allocator to allocate from
     * @param s the string, may be {@code null}
     * @return the UTF-16 characters of {@code s}, or {@link MemorySegment#NULL} if {@code s} is null
     */
    public static MemorySegment allocString(SegmentAllocator allocator, String s) {
        return WKJStringCodec.encode(allocator, s);
    }

    /**
     * Returns the length in UTF-16 code units of the buffer {@link #allocString} produces.
     *
     * @param s the string, may be {@code null}
     * @return the length, zero for {@code null} and for the empty string
     */
    public static int stringLength(String s) {
        return WKJStringCodec.length(s);
    }

    /**
     * Throws the exception the library recorded in this thread's exception slot, if any, and clears the
     * slot. Called after every fallible downcall; when nothing was thrown, which is the common case, it
     * costs one plain read and no downcall.
     */
    public static void checkException() {
        MemorySegment slot = exceptionSlot();
        int type = slot.get(JAVA_INT, OFFSET_TYPE);
        if (type == WKJ_EXC_NONE) {
            return;
        }
        throw takePendingException(slot, type);
    }

    /**
     * Registers a Java object and returns the {@code wkj_ref} native code uses to name it, with a
     * reference count of one. Native code never holds a Java reference (contract section 3).
     * <p>
     * A fresh id is minted on every call, so two ids can name one object; that is what the JNI code
     * did, where {@code NewGlobalRef} returned a new reference each time, and what the C header
     * permits. What the header does <em>not</em> permit is dropping an entry while another owner
     * still holds an id for it, which is why every id carries a count: {@code WKJHandle}'s copy
     * constructor retains and its destructor releases, so one object routinely has several live
     * handles.
     *
     * @param o the object, may be {@code null}
     * @return the registry id, zero for {@code null}
     */
    public static long register(Object o) {
        return o == null ? 0L : put(new Entry(o, false));
    }

    /**
     * Adds a reference to an id, which is {@code WKJHostCore::retain}. A strong id keeps its object
     * reachable, so the same id comes back with its count raised by one; a weak id cannot, so a new
     * strong id is minted for the same object.
     * <p>
     * {@code retain(0)} is zero, mirroring the {@code (env && ref)} guard that {@code JLocalRef}'s
     * and {@code JGlobalRef}'s copy paths had, and so is a retain of an id that is gone or whose
     * weakly held object has been collected.
     *
     * @param id the registry id, may be zero
     * @return an id the caller owns and must release exactly once, or zero
     */
    public static long retain(long id) {
        Entry entry = entry(id);
        if (entry == null) {
            return 0L;
        }
        if (entry.isWeak()) {
            Object referent = entry.referent();
            return referent == null ? 0L : register(referent);
        }
        return entry.acquire() ? id : 0L;
    }

    /**
     * Mints a weak id for the object {@code id} names, which is {@code WKJHostCore::retain_weak}. A
     * weak id does not keep its object reachable: once the object is collected {@link #isLive}
     * answers false and {@link #lookup} answers {@code null}, and neither throws.
     * <p>
     * This exists because {@code Source/WebCore/bridge/jni/JobjectWrapper.cpp} takes
     * {@code NewWeakGlobalRef} by default - {@code useGlobalRef} defaults to false - and the
     * LiveConnect code around it is written to tolerate collection. Modelling every id as strong
     * would pin every Java object a page script has ever touched, which is a behaviour change and
     * not a cleanup. A new id is always minted, because the caller may be asking for a weak view of
     * a strong id.
     *
     * @param id the registry id, may be zero
     * @return a weak id the caller owns and must release exactly once, or zero
     */
    public static long retainWeak(long id) {
        Object referent = lookup(id);
        return referent == null ? 0L : put(new Entry(referent, true));
    }

    /**
     * Drops one reference to an id, which is {@code WKJHostCore::release}, and removes the entry
     * when the count reaches zero. Other ids for the same object are unaffected;
     * {@code release(0)} is a no-op, and so is releasing an id that is already gone.
     *
     * @param id the registry id, may be zero
     */
    public static void release(long id) {
        Entry entry = entry(id);
        if (entry != null && entry.drop()) {
            REGISTRY.remove(id, entry);
        }
    }

    /**
     * Drops the reference {@link #register} minted. This is {@link #release} under the name the Java
     * side of a dispose path reads better with: the owner is giving up the id it created, not
     * destroying an object that native code may still hold ids for.
     *
     * @param id the registry id, zero is ignored
     */
    public static void unregister(long id) {
        release(id);
    }

    /**
     * Resolves a {@code wkj_ref} back to its Java object.
     *
     * @param id the registry id
     * @return the object, {@code null} for id zero, for an id that is no longer registered and for a
     *         weak id whose object has been collected
     */
    public static Object lookup(long id) {
        Entry entry = entry(id);
        return entry == null ? null : entry.referent();
    }

    /**
     * Reports whether an id still names a live object, which is {@code WKJHostCore::is_live} and the
     * replacement for testing a weak global reference against {@code NULL}.
     *
     * @param id the registry id
     * @return true if the object is still there
     */
    public static boolean isLive(long id) {
        return lookup(id) != null;
    }

    private static long put(Entry entry) {
        long id = NEXT_REF.getAndIncrement();
        REGISTRY.put(id, entry);
        return id;
    }

    private static Entry entry(long id) {
        return id == 0L ? null : REGISTRY.get(id);
    }

    /**
     * Returns the number of live registry entries. An entry counts until its last id is released,
     * even if it is weak and its object has already been collected: the id remains valid to release,
     * which is the promise the ABI makes to whoever holds it.
     *
     * @return the registry size
     */
    static int registrySizeForTesting() {
        return REGISTRY.size();
    }

    /**
     * Returns the number of live ids for a registry entry, so that the reference counting test can
     * assert on the count itself rather than only on its effects.
     *
     * @param id the registry id
     * @return the count, or zero if the id is not registered
     */
    static int referenceCountForTesting(long id) {
        Entry entry = entry(id);
        return entry == null ? 0 : entry.count();
    }

    /**
     * Returns the layout of {@code WKJExceptionSlot}, for the test that checks it against the C
     * {@code sizeof} and {@code offsetof}.
     *
     * @return the exception slot layout
     */
    static StructLayout exceptionSlotLayout() {
        return EXCEPTION_SLOT_LAYOUT;
    }

    /**
     * Creates an upcall stub in the process-wide upcall arena. The target must never let a
     * {@link Throwable} escape, because an exception crossing the boundary terminates the JVM.
     *
     * @param target the Java method to call
     * @param descriptor the descriptor matching the C function pointer type
     * @return the stub, valid for the lifetime of the process
     */
    @SuppressWarnings("restricted")
    public static MemorySegment upcallStub(MethodHandle target, FunctionDescriptor descriptor) {
        return LINKER.upcallStub(target, descriptor, UPCALL_ARENA);
    }

    /**
     * Allocates a table of function pointers in the process-wide upcall arena. A C callback table is
     * a struct whose every member is a function pointer, so it has the layout of an {@code ADDRESS}
     * sequence on every ABI this module supports; the caller writes the slots in the declaration
     * order of the C struct.
     * <p>
     * The table lives as long as the process because the library keeps the pointer for as long as it
     * is loaded, which is the one case the migration playbook allows a shared, never closed arena
     * for.
     *
     * @param slots the stubs, in the declaration order of the C struct; a slot may be
     *              {@link MemorySegment#NULL}
     * @return the table
     */
    public static MemorySegment upcallTable(MemorySegment... slots) {
        MemorySegment table = UPCALL_ARENA.allocate(ADDRESS, slots.length);
        for (int i = 0; i < slots.length; i++) {
            table.setAtIndex(ADDRESS, i, slots[i]);
        }
        return table;
    }

    // ------------------------------------------------------- upcall parameter marshalling

    /*
     * A pointer parameter of an upcall stub arrives as a zero length MemorySegment, because the C
     * declaration carries no size the linker could use. Reading or writing through one therefore has
     * to say how big it is, and MemorySegment.reinterpret is the restricted call that says so.
     *
     * Every such read and write in this module goes through the helpers below, for two reasons: the
     * restricted call stays inside this class, where --enable-native-access is granted, and the
     * bounds are stated once instead of at each of the 168 callback slots. Every one of them
     * tolerates a NULL pointer, because the C header uses NULL for "no value" in both directions.
     */

    /**
     * Gives a pointer parameter a size, so that it can be read or written through.
     *
     * @param pointer the segment an upcall stub was handed
     * @param byteSize the number of bytes the C caller guarantees are there
     * @return the same address, bounded
     */
    @SuppressWarnings("restricted")
    public static MemorySegment resize(MemorySegment pointer, long byteSize) {
        return pointer.reinterpret(byteSize);
    }

    /**
     * Reads a {@code const uint16_t* s, int32_t s_len} parameter pair.
     *
     * @param chars the characters, may be {@link MemorySegment#NULL}
     * @param length the code unit count
     * @return the string, {@code null} for a {@code NULL} pointer, {@code ""} for length zero
     */
    public static String readString(MemorySegment chars, int length) {
        if (chars.address() == 0L) {
            return null;
        }
        if (length <= 0) {
            return "";
        }
        char[] value = new char[length];
        MemorySegment.copy(resize(chars, (long) length * Character.BYTES), JAVA_CHAR, 0L, value, 0,
                length);
        return new String(value);
    }

    /**
     * Reads a {@code const uint8_t* data, int32_t length} parameter pair.
     *
     * @param data the bytes, may be {@link MemorySegment#NULL}
     * @param length the byte count
     * @return the bytes, or {@code null} for a {@code NULL} pointer
     */
    public static byte[] readBytes(MemorySegment data, int length) {
        if (data.address() == 0L) {
            return null;
        }
        if (length <= 0) {
            return new byte[0];
        }
        return resize(data, length).toArray(JAVA_BYTE);
    }

    /**
     * Reads a {@code const int32_t* data, int32_t count} parameter pair.
     *
     * @param data the values, may be {@link MemorySegment#NULL}
     * @param count the element count
     * @return the values, or {@code null} for a {@code NULL} pointer
     */
    public static int[] readInts(MemorySegment data, int count) {
        if (data.address() == 0L) {
            return null;
        }
        if (count <= 0) {
            return new int[0];
        }
        return resize(data, (long) count * Integer.BYTES).toArray(JAVA_INT);
    }

    /**
     * Reads a {@code const double* data, int32_t count} parameter pair.
     *
     * @param data the values, may be {@link MemorySegment#NULL}
     * @param count the element count
     * @return the values, or {@code null} for a {@code NULL} pointer
     */
    public static double[] readDoubles(MemorySegment data, int count) {
        if (data.address() == 0L) {
            return null;
        }
        if (count <= 0) {
            return new double[0];
        }
        return resize(data, (long) count * Double.BYTES).toArray(JAVA_DOUBLE);
    }

    /**
     * Reads a {@code const int64_t* data, int32_t count} parameter pair, which is also how an array
     * of {@code wkj_ref} arrives.
     *
     * @param data the values, may be {@link MemorySegment#NULL}
     * @param count the element count
     * @return the values, or {@code null} for a {@code NULL} pointer
     */
    public static long[] readLongs(MemorySegment data, int count) {
        if (data.address() == 0L) {
            return null;
        }
        if (count <= 0) {
            return new long[0];
        }
        return resize(data, (long) count * Long.BYTES).toArray(JAVA_LONG);
    }

    /**
     * Reads the parallel {@code const uint16_t* const* values, const int32_t* lengths, int32_t
     * count} triple a Java {@code String[]} crosses as. An element pointer of {@code NULL} is a
     * {@code null} element, which the ABI keeps distinct from the empty string.
     *
     * @param values the array of character pointers, may be {@link MemorySegment#NULL}
     * @param lengths the array of lengths, may be {@link MemorySegment#NULL}
     * @param count the element count
     * @return the strings, or {@code null} when either pointer is {@code NULL} and the count is not
     *         zero
     */
    public static String[] readStringArray(MemorySegment values, MemorySegment lengths, int count) {
        if (count <= 0) {
            return new String[0];
        }
        if (values.address() == 0L || lengths.address() == 0L) {
            return null;
        }
        MemorySegment pointers = resize(values, (long) count * ADDRESS.byteSize());
        MemorySegment sizes = resize(lengths, (long) count * Integer.BYTES);
        String[] strings = new String[count];
        for (int i = 0; i < count; i++) {
            strings[i] = readString(pointers.getAtIndex(ADDRESS, i), sizes.getAtIndex(JAVA_INT, i));
        }
        return strings;
    }

    /**
     * Writes an {@code int32_t} out parameter array, for example the {@code int32_t out_wh[2]} of
     * {@code graphics.image_frame_get_size}.
     *
     * @param out the buffer, may be {@link MemorySegment#NULL}
     * @param values the values, may be {@code null}
     * @param count the number of elements the C caller provided room for
     * @return true if the values were written, false if there was nothing to write or nowhere to
     *         write it, which is the 0 return every such slot documents
     */
    public static boolean writeInts(MemorySegment out, int[] values, int count) {
        if (out.address() == 0L || values == null || values.length < count) {
            return false;
        }
        MemorySegment.copy(values, 0, resize(out, (long) count * Integer.BYTES), JAVA_INT, 0L,
                count);
        return true;
    }

    /**
     * Writes a {@code float} out parameter array, for example the {@code float out_xywh[4]} the
     * rectangle returning slots fill.
     *
     * @param out the buffer, may be {@link MemorySegment#NULL}
     * @param values the values, may be {@code null}
     * @param count the number of elements the C caller provided room for
     * @return true if the values were written
     */
    public static boolean writeFloats(MemorySegment out, float[] values, int count) {
        if (out.address() == 0L || values == null || values.length < count) {
            return false;
        }
        MemorySegment.copy(values, 0, resize(out, (long) count * Float.BYTES), JAVA_FLOAT, 0L,
                count);
        return true;
    }

    /**
     * Writes an {@code int64_t} out parameter array, which is also how an array of {@code wkj_ref}
     * leaves.
     *
     * @param out the buffer, may be {@link MemorySegment#NULL}
     * @param values the values, may be {@code null}
     * @param count the number of elements to write
     * @return true if the values were written
     */
    public static boolean writeLongs(MemorySegment out, long[] values, int count) {
        if (out.address() == 0L || values == null || values.length < count) {
            return false;
        }
        MemorySegment.copy(values, 0, resize(out, (long) count * Long.BYTES), JAVA_LONG, 0L, count);
        return true;
    }

    /**
     * Writes a string into the {@code uint16_t* out_buf, int32_t out_cap, int32_t* out_length}
     * triple a string returning callback slot ends in.
     *
     * @param value the string, may be {@code null}
     * @param out the buffer, may be {@link MemorySegment#NULL}
     * @param capacity the buffer capacity in code units
     * @param length the {@code int32_t*} to report the length through
     * @return {@code WKJ_STR_OK}, {@code WKJ_STR_NULL} or {@code WKJ_STR_OVERFLOW}
     */
    public static int emitString(String value, MemorySegment out, int capacity,
                                 MemorySegment length) {
        // A capacity of zero is a real case, not a broken one: the caller may be asking only for the
        // size. It gets a zero length buffer rather than a NULL one, so that the empty string still
        // answers WKJ_STR_OK with length 0 instead of asking to be retried with room for nothing.
        int room = Math.max(capacity, 0);
        MemorySegment buffer = out.address() == 0L
                ? MemorySegment.NULL
                : resize(out, (long) room * Character.BYTES);
        return WKJStringCodec.emit(value, buffer, room, sizedInt(length));
    }

    /**
     * The same for a byte payload: {@code uint8_t* out_buf, int32_t out_cap, int32_t* out_length},
     * where the contract 13 status codes count bytes rather than code units.
     *
     * @param value the bytes, may be {@code null}
     * @param out the buffer, may be {@link MemorySegment#NULL}
     * @param capacity the buffer capacity in bytes
     * @param length the {@code int32_t*} to report the length through
     * @return {@code WKJ_STR_OK}, {@code WKJ_STR_NULL} or {@code WKJ_STR_OVERFLOW}
     */
    public static int emitBytes(byte[] value, MemorySegment out, int capacity,
                                MemorySegment length) {
        MemorySegment count = sizedInt(length);
        if (value == null) {
            setLength(count, 0);
            return WKJStringCodec.NULL;
        }
        int room = Math.max(capacity, 0);
        if (out.address() == 0L || value.length > room) {
            setLength(count, value.length);
            return WKJStringCodec.OVERFLOW;
        }
        if (value.length > 0) {
            MemorySegment.copy(value, 0, resize(out, room), JAVA_BYTE, 0L, value.length);
        }
        setLength(count, value.length);
        return WKJStringCodec.OK;
    }

    /**
     * Writes one {@code int32_t} out parameter.
     *
     * @param out the pointer, may be {@link MemorySegment#NULL}
     * @param value the value
     */
    public static void writeInt(MemorySegment out, int value) {
        setLength(sizedInt(out), value);
    }

    /**
     * Writes one {@code int64_t} out parameter.
     *
     * @param out the pointer, may be {@link MemorySegment#NULL}
     * @param value the value
     */
    public static void writeLong(MemorySegment out, long value) {
        if (out.address() != 0L) {
            resize(out, Long.BYTES).set(JAVA_LONG, 0L, value);
        }
    }

    private static MemorySegment sizedInt(MemorySegment pointer) {
        return pointer.address() == 0L ? MemorySegment.NULL : resize(pointer, Integer.BYTES);
    }

    private static void setLength(MemorySegment target, int value) {
        if (target.address() != 0L) {
            target.set(JAVA_INT, 0L, value);
        }
    }

    /**
     * Returns the name of the library this class binds, {@code jfxwebkit} unless the
     * {@code javafx.web.nativeLibrary} property overrides it.
     *
     * @return the library name
     */
    static String libraryName() {
        return LIBRARY_NAME;
    }

    /**
     * Builds the sentence an ABI version mismatch fails with. It names the library, both versions and what
     * to do about it, because "an old prebuilt library is on the library path" is otherwise an obscure
     * crash somewhere else entirely (contract section 5).
     *
     * @param expected the version javafx.web is written against
     * @param actual the version the loaded library reports
     * @return the message
     */
    static String abiVersionMessage(int expected, int actual) {
        return LIBRARY_NAME + " reports wkj_* ABI version " + Integer.toUnsignedString(actual)
                + ", but javafx.web requires ABI version " + expected + ". The " + LIBRARY_NAME
                + " library on java.library.path is stale: rebuild it from this source tree.";
    }

    /**
     * Builds the sentence a library with no {@code wkj_*} ABI at all fails with, which is what a prebuilt
     * JNI {@code jfxwebkit} produces (contract section 8).
     *
     * @return the message
     */
    static String abiMissingMessage() {
        return LIBRARY_NAME + " does not export " + ABI_VERSION_SYMBOL + ", so it carries no wkj_* ABI:"
                + " javafx.web requires ABI version " + WKJ_ABI_VERSION + " and the loaded library reports"
                + " none. That library is a stale JNI build; rebuild " + LIBRARY_NAME
                + " from this source tree.";
    }

    private static void checkAbiVersion(SymbolLookup lookup) {
        MethodHandle handle = bindOptional(lookup, ABI_VERSION_SYMBOL, FunctionDescriptor.of(JAVA_INT));
        if (handle == null) {
            throw new UnsatisfiedLinkError(abiMissingMessage());
        }
        int actual;
        try {
            actual = (int) handle.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        if (actual != WKJ_ABI_VERSION) {
            throw new UnsatisfiedLinkError(abiVersionMessage(WKJ_ABI_VERSION, actual));
        }
    }

    /*
     * The calling thread's exception slot. A platform thread caches the pointer, which is stable for the
     * lifetime of the thread; a virtual thread fetches it every time, because the slot it gets belongs to
     * whichever carrier it is mounted on right now.
     */
    private static MemorySegment exceptionSlot() {
        return Thread.currentThread().isVirtual() ? fetchExceptionSlot() : EXCEPTION_SLOT.get();
    }

    private static MemorySegment fetchExceptionSlot() {
        requireLibrary();
        MemorySegment slot;
        try {
            slot = (MemorySegment) WKJ_EXCEPTION_SLOT.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        return reinterpret(slot, EXCEPTION_SLOT_LAYOUT.byteSize());
    }

    /*
     * Builds the exception the slot describes, clears the slot and returns the exception to throw. The
     * message is inline in the slot, so it is copied out before the slot is cleared and nothing outlives
     * this call.
     *
     * Every kind decodes to org.w3c.dom.DOMException(short, String), carrying the DOM legacy code and the
     * description message, because that is the only exception the JNI bindings ever raised: every raise
     * path in the tree goes through raiseDOMErrorException. WKJ_EXC_EVENT and WKJ_EXC_RANGE named
     * org.w3c.dom.events.EventException and org.w3c.dom.ranges.RangeException in the JNI enum, but no C
     * code ever selected them, so throwing them here would invent behaviour rather than preserve it
     * (contract section 13.1, finding 8). A value outside the enum is a protocol error between the two
     * sides rather than a DOM condition, and says so.
     */
    private static RuntimeException takePendingException(MemorySegment slot, int type) {
        int code = slot.get(JAVA_INT, OFFSET_CODE);
        String message = readMessage(slot);
        clearExceptionSlot(slot);
        return switch (type) {
            case WKJ_EXC_DOM, WKJ_EXC_EVENT, WKJ_EXC_RANGE, WKJ_EXC_UNDEFINED ->
                    new DOMException((short) code, message);
            default -> new IllegalStateException(LIBRARY_NAME + " reported an unknown exception type "
                    + type + ", code " + code + (message.isEmpty() ? "" : ": " + message));
        };
    }

    /*
     * Copies the inline message out of the slot. message_length is authoritative: the buffer is not NUL
     * terminated, and a longer message was truncated to WKJ_EXC_MESSAGE_MAX code units by the library.
     * There is no null message on this ABI, only an empty one.
     */
    private static String readMessage(MemorySegment slot) {
        int length = slot.get(JAVA_INT, OFFSET_MESSAGE_LENGTH);
        if (length <= 0) {
            return "";
        }
        int count = Math.min(length, WKJ_EXC_MESSAGE_MAX);
        char[] chars = new char[count];
        MemorySegment.copy(slot, JAVA_CHAR, OFFSET_MESSAGE, chars, 0, count);
        return new String(chars);
    }

    /*
     * Storing WKJ_EXC_NONE into `type` is the whole of clearing the slot: the other fields are meaningless
     * while `type` is WKJ_EXC_NONE, and the library rewrites all of them on the next raise. The length is
     * zeroed as well so that a stale length can never describe a stale message.
     */
    private static void clearExceptionSlot(MemorySegment slot) {
        slot.set(JAVA_INT, OFFSET_TYPE, WKJ_EXC_NONE);
        slot.set(JAVA_INT, OFFSET_CODE, 0);
        slot.set(JAVA_INT, OFFSET_MESSAGE_LENGTH, 0);
    }

    @SuppressWarnings("restricted")
    private static MemorySegment reinterpret(MemorySegment segment, long byteSize) {
        return segment.reinterpret(byteSize);
    }

    // ------------------------------------------------------------------------- the host table

    /**
     * Returns the layout of {@code WKJHost}, for the test that checks it against the C
     * {@code sizeof} and {@code offsetof}.
     *
     * @return the host table layout
     */
    static MemoryLayout hostLayout() {
        return HOST_LAYOUT;
    }

    /**
     * Returns every struct layout this class declares for the {@code wkj_*} ABI, keyed by its C
     * name. The layout test drives itself from the C side and asserts that this map covers it
     * exactly, so a struct added to the header with no Java layout fails rather than going
     * unchecked.
     *
     * @return the declared layouts
     */
    static Map<String, MemoryLayout> declaredLayouts() {
        return WKJLayouts.all();
    }

    /**
     * Returns the byte offset of a dotted host slot path, for example {@code core.release}.
     *
     * @param dottedPath the slot path
     * @return the offset in bytes
     */
    static long hostSlotOffset(String dottedPath) {
        String[] parts = dottedPath.split("\\.");
        PathElement[] elements = new PathElement[parts.length];
        for (int i = 0; i < parts.length; i++) {
            elements[i] = PathElement.groupElement(parts[i]);
        }
        return HOST_LAYOUT.byteOffset(elements);
    }

    /**
     * Returns {@code sizeof(WKJHost)} as the Java layout computes it, which is what
     * {@code wkj_init} is given as {@code host_size} and what the table carries in its own
     * {@code size} field.
     *
     * @return the host table size in bytes
     */
    static int hostByteSize() {
        return (int) HOST_LAYOUT.byteSize();
    }

    /**
     * Returns the installed {@code WKJHost} table.
     *
     * @return the table, or {@link MemorySegment#NULL} if the library could not be loaded
     */
    static MemorySegment hostTable() {
        return HOST;
    }

    /**
     * Returns the code {@code wkj_init} returned when this class was initialized.
     *
     * @return {@code WKJ_INIT_OK} on a healthy process
     */
    static int hostInitResultForTesting() {
        return HOST_INIT_RESULT;
    }

    /**
     * Calls {@code wkj_init} with the arguments given and returns its result code, throwing nothing.
     * Production installs the table once from the static initializer above; this exists so that the
     * binding tests can drive the library's guards, and can re-install a table the suite has
     * deliberately uninstalled, without holding a downcall handle outside this class.
     *
     * @param host the table
     * @param hostSize the {@code host_size} argument
     * @param abiVersion the {@code abi_version} argument
     * @return the {@code WKJ_INIT_*} result code
     */
    static int callWkjInit(MemorySegment host, int hostSize, int abiVersion) {
        requireLibrary();
        return callWkjInit(WKJ_INIT, host, hostSize, abiVersion);
    }

    private static int callWkjInit(MethodHandle init, MemorySegment host, int hostSize,
                                   int abiVersion) {
        try {
            return (int) init.invokeExact(host, hostSize, abiVersion);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /*
     * Builds the table: its own byte size in the size field, one upcall stub per core slot, and then
     * one call per filled group, each of which lives beside the Java classes it forwards to.
     *
     * The seven remaining placeholder groups - webpage, frameloader, chrome, editor, contextmenu,
     * inspector and drag - are left NULL, and so is any slot inside a filled group whose Java target
     * does not exist. The library is required to tolerate that: it checks every pointer before
     * calling it and falls back to the default documented on the slot, which is what lets a newer
     * javafx.web run against an older library at all.
     *
     * Each installer is reached statically rather than through a list, so that a group whose class
     * fails to compile fails here rather than at the first callback. They run inside this class's
     * own initializer, which is safe because everything they use - the linker, the upcall arena and
     * the host layout - is initialized above, and because none of them touches HOST, LOOKUP or any
     * other field this initializer has yet to assign.
     */
    private static MemorySegment buildHostTable() {
        MemorySegment host = UPCALL_ARENA.allocate(HOST_LAYOUT);
        host.set(JAVA_INT, OFFSET_HOST_SIZE, hostByteSize());
        installSlot(host, "core.retain", "coreRetain",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
        installSlot(host, "core.retain_weak", "coreRetainWeak",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
        installSlot(host, "core.release", "coreRelease",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        installSlot(host, "core.is_live", "coreIsLive",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        installSlot(host, "core.hash_code", "coreHashCode",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        installSlot(host, "core.equals", "coreEquals",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG));
        installSlot(host, "core.check_and_clear_exception", "coreCheckAndClearException",
                FunctionDescriptor.of(JAVA_INT));

        WtfUpcalls.install(host);
        PalUpcalls.install(host);
        FileSystemUpcalls.install(host);
        ThemeUpcalls.install(host);
        RenderThemeUpcalls.install(host);
        GraphicsUpcalls.install(host);
        MediaUpcalls.install(host);
        NetworkUpcalls.install(host);
        return host;
    }

    private static void installSlot(MemorySegment host, String dottedPath, String method,
                                    FunctionDescriptor descriptor) {
        installHostSlot(host, dottedPath, MethodHandles.lookup(), method, descriptor);
    }

    /**
     * Installs one callback into a {@code WKJHost} table under construction, resolving the target as
     * a static method of the caller's own class.
     * <p>
     * The Java signature is derived from {@code descriptor} rather than restated, so a descriptor
     * that does not match the method it names fails here, at class initialization, with the slot and
     * the method in the message - instead of corrupting the stack at the first callback.
     *
     * @param host the table
     * @param dottedPath the slot, for example {@code graphics.rq_flush}
     * @param lookup the caller's own {@link MethodHandles#lookup}, which is what gives the private
     *               access an upcall target needs
     * @param method the name of the static method in {@code lookup.lookupClass()}
     * @param descriptor the descriptor matching the C function pointer type
     */
    public static void installHostSlot(MemorySegment host, String dottedPath,
                                       MethodHandles.Lookup lookup, String method,
                                       FunctionDescriptor descriptor) {
        host.set(ADDRESS, hostSlotOffset(dottedPath), stub(dottedPath, lookup, method, descriptor));
        HOST_SLOT_DESCRIPTORS.put(dottedPath, descriptor);
    }

    /**
     * Returns the descriptor each filled {@code WKJHost} slot was bound with, keyed by its dotted
     * path. The layout test compares every one of them against the C prototype the library reports:
     * a descriptor that names the right slot with the wrong shape installs a stub that corrupts the
     * stack on the first call, and nothing else in this module would notice.
     *
     * @return the descriptors, in installation order
     */
    static Map<String, FunctionDescriptor> hostSlotDescriptors() {
        synchronized (HOST_SLOT_DESCRIPTORS) {
            return new LinkedHashMap<>(HOST_SLOT_DESCRIPTORS);
        }
    }

    /**
     * Allocates a callback table of a named struct layout in the process-wide upcall arena, zeroed.
     * Every slot the caller does not fill therefore stays {@code NULL}, which the library is
     * required to tolerate.
     *
     * @param layout the layout of the C struct
     * @return the table, valid for the lifetime of the process
     */
    public static MemorySegment allocateTable(MemoryLayout layout) {
        return UPCALL_ARENA.allocate(layout);
    }

    /**
     * Installs one callback into a table of any layout, which is what a subsystem table such as
     * {@code WKJLiveConnectHost} needs; {@link #installHostSlot} is the same thing for
     * {@code WKJHost}.
     *
     * @param table the table
     * @param layout the layout the table was allocated with
     * @param member the member name, exactly as the C struct spells it
     * @param lookup the caller's own {@link MethodHandles#lookup}
     * @param method the name of the static method in {@code lookup.lookupClass()}
     * @param descriptor the descriptor matching the C function pointer type
     */
    public static void installSlot(MemorySegment table, MemoryLayout layout, String member,
                                   MethodHandles.Lookup lookup, String method,
                                   FunctionDescriptor descriptor) {
        table.set(ADDRESS, layout.byteOffset(PathElement.groupElement(member)),
                stub(member, lookup, method, descriptor));
    }

    private static MemorySegment stub(String slot, MethodHandles.Lookup lookup, String method,
                                      FunctionDescriptor descriptor) {
        MethodHandle target;
        try {
            target = lookup.findStatic(lookup.lookupClass(), method, descriptor.toMethodType());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("no upcall target for " + slot + ": "
                    + lookup.lookupClass().getName() + "." + method + descriptor.toMethodType(), e);
        }
        return upcallStub(target, descriptor);
    }

    /*
     * The sentence a rejected host table fails with. wkj_init has four ways to say no and each one
     * means that something different was mismatched, so the code is named rather than printed bare.
     */
    private static String initFailureMessage(int code) {
        String reason = switch (code) {
            case WKJ_INIT_ERR_NULL_HOST -> "it was given a null table";
            case WKJ_INIT_ERR_ABI_VERSION ->
                    "it does not accept wkj_* ABI version " + WKJ_ABI_VERSION;
            case WKJ_INIT_ERR_HOST_SIZE -> "sizeof(WKJHost) is " + hostByteSize()
                    + " bytes in javafx.web and something else in the library";
            case WKJ_INIT_ERR_ALREADY_INITED -> "a host table is already installed";
            default -> "it returned the unknown code " + code;
        };
        return LIBRARY_NAME + " rejected the javafx.web host table: " + reason + ". The "
                + LIBRARY_NAME + " library on java.library.path does not match this source tree:"
                + " rebuild it.";
    }

    // ----------------------------------------------------------------------- upcall targets

    /*
     * The seven WKJHostCore targets. Every one of them catches Throwable, records the failure for
     * check_and_clear_exception, logs it and returns the value the C header documents for a NULL
     * slot. An exception escaping an upcall stub terminates the JVM, so this is not a style choice.
     */

    private static long coreRetain(long ref) {
        try {
            return retain(ref);
        } catch (Throwable t) {
            upcallFailed("core.retain", t);
            return 0L;
        }
    }

    private static long coreRetainWeak(long ref) {
        try {
            return retainWeak(ref);
        } catch (Throwable t) {
            upcallFailed("core.retain_weak", t);
            return 0L;
        }
    }

    private static void coreRelease(long ref) {
        try {
            release(ref);
        } catch (Throwable t) {
            upcallFailed("core.release", t);
        }
    }

    private static int coreIsLive(long ref) {
        try {
            return isLive(ref) ? 1 : 0;
        } catch (Throwable t) {
            upcallFailed("core.is_live", t);
            return 0;
        }
    }

    /*
     * hash_code and equals answer for the referents, not for the ids, which are handles. Both are
     * provisioned rather than load bearing: their C counterparts, getJavaHashCode and isJavaEquals
     * in the old JavaDOMUtils.cpp, were defined and called from nowhere in the tree. They are
     * implemented correctly anyway, because identity questions become unavoidable once one object
     * can have several ids, which is where the LiveConnect phase lands.
     */
    private static int coreHashCode(long ref) {
        try {
            Object referent = lookup(ref);
            return referent == null ? 0 : referent.hashCode();
        } catch (Throwable t) {
            upcallFailed("core.hash_code", t);
            return 0;
        }
    }

    private static int coreEquals(long a, long b) {
        try {
            if (a == b) {
                return 1;
            }
            Object first = lookup(a);
            return first != null && first.equals(lookup(b)) ? 1 : 0;
        } catch (Throwable t) {
            upcallFailed("core.equals", t);
            // The default the header documents for a NULL slot, which is all that is left to say.
            return a == b ? 1 : 0;
        }
    }

    /*
     * Deliberately not routed through upcallFailed: this slot reports failures, so recording its own
     * would make the next call report the failure of this one. Reading and clearing a one element
     * array cannot throw, but the catch is kept because the containment rule is absolute.
     */
    private static int coreCheckAndClearException() {
        try {
            return checkAndClearUpcallFailure();
        } catch (Throwable t) {
            logContainedFailure("core.check_and_clear_exception", t);
            return 0;
        }
    }

    /**
     * Records that an upcall on this thread ended in a {@link Throwable} and logs it. Every upcall
     * target in this module funnels its {@code catch} through here, so that
     * {@code WKJHostCore::check_and_clear_exception} - the replacement for
     * {@code WTF::CheckAndClearException(env)}, whose result about a dozen C++ sites branch on - has
     * one place to observe and cannot miss a client callback's failure.
     * <p>
     * The flag is set before anything else runs, so that a logger which is itself broken still
     * leaves the failure visible to C.
     *
     * @param slot the name of the callback that failed, for the log
     * @param t the throwable that was contained
     */
    public static void upcallFailed(String slot, Throwable t) {
        try {
            UPCALL_FAILED.get()[0] = true;
        } catch (Throwable ignored) {
            // Nothing useful is left to do here; the log below is still worth attempting.
        }
        logContainedFailure(slot, t);
    }

    /**
     * Reports whether the last upcall on this thread ended in a {@link Throwable}, and clears that
     * state. This is {@code WKJHostCore::check_and_clear_exception}.
     * <p>
     * The semantics are narrower than the {@code WTF::CheckAndClearException(env)} they replace, and
     * deliberately so: JNI's version reported <em>any</em> pending exception, including one raised
     * by a failed {@code FindClass} or {@code GetMethodID} or by an allocation failure inside JNI
     * itself, and it called {@code ExceptionDescribe()} on the way past. Class and method lookup no
     * longer happen - a callback is a function pointer in a table - so the only failure left to
     * report is a Java one, which has already been caught and logged where it happened.
     *
     * @return 1 if an upcall failed since the last check, 0 otherwise
     */
    static int checkAndClearUpcallFailure() {
        boolean[] flag = UPCALL_FAILED.get();
        boolean failed = flag[0];
        flag[0] = false;
        return failed ? 1 : 0;
    }

    private static void logContainedFailure(String slot, Throwable t) {
        try {
            LOGGER.severe("javafx.web upcall " + slot + " failed and was contained", t);
        } catch (Throwable ignored) {
            // Even the logger must not be allowed to take the process down.
        }
    }

    // --------------------------------------------------------------------------- the registry

    /**
     * One registry entry: the object, or a weak reference to it, plus the number of ids that name it
     * and have not been released.
     * <p>
     * The count is guarded by the entry's own monitor rather than by anything global, and the entry
     * leaves the map only through the {@link #drop} that takes the count to zero. A count of zero is
     * final: an {@link #acquire} arriving after it answers false rather than resurrecting an entry
     * that is on its way out, and because ids are monotonic and never reused, the raced id cannot
     * name something else by the time the loser looks again.
     */
    private static final class Entry {

        private final Object strong;
        private final WeakReference<Object> weak;
        private int count = 1;

        Entry(Object referent, boolean weakly) {
            this.strong = weakly ? null : referent;
            this.weak = weakly ? new WeakReference<>(referent) : null;
        }

        boolean isWeak() {
            return weak != null;
        }

        Object referent() {
            return weak != null ? weak.get() : strong;
        }

        synchronized boolean acquire() {
            if (count == 0) {
                return false;
            }
            count++;
            return true;
        }

        synchronized boolean drop() {
            return count != 0 && --count == 0;
        }

        synchronized int count() {
            return count;
        }
    }
}

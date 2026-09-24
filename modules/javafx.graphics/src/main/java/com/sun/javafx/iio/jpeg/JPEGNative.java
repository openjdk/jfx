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

package com.sun.javafx.iio.jpeg;

import com.sun.glass.utils.NativeLibLoader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The single point of contact between {@code com.sun.javafx.iio.jpeg} and the {@code iio_*} C ABI
 * exported by the {@code javafx_iio} library ({@code src/main/native-iio/iio_api.h}). It owns the
 * library load, the {@link SymbolLookup}, the {@link Linker}, every downcall handle, the two struct
 * layouts, the four upcall stubs and the registry behind them, and it is the only class in this
 * package that uses a restricted {@code java.lang.foreign} method.
 * <p>
 * {@link JPEGImageLoader} holds its decoder as a {@link MemorySegment} and calls the typed static
 * wrappers below. No downcall is bound with {@link Linker.Option#critical}: {@code iio_create},
 * {@code iio_start_decompression} and {@code iio_decompress} block on the {@link InputStream} and
 * call back into Java, and the other six are cold, so {@code iio_decompress} decodes into an
 * off-heap buffer that {@link #decompress} copies into the caller's array afterwards.
 *
 * <h2>Callbacks and the pending-exception slot</h2>
 * libjpeg pulls compressed bytes through an {@code IioJpegCallbacks} table, synchronously, on the
 * thread inside the downcall. There is one table for the whole process, {@link #CALLBACK_TABLE},
 * built by the class initializer: its four upcall stubs target the static {@code on*} methods below,
 * which carry no per-loader state, so creating a loader generates no stub and disposing one closes
 * no arena - the JNI decoder did no native work per image either. C never sees a Java reference:
 * {@code void* user} is a Java-assigned id that {@link #REGISTRY} maps back to the loader, and an id
 * the registry no longer holds behaves like a {@code NULL} slot (an exhausted stream, a no-op
 * warning or progress report).
 * <p>
 * Upcall targets must not throw, but the JNI decoder let an exception from {@code InputStream.read},
 * {@code InputStream.skip} or a listener abort the decode and surface from the enclosing native
 * method. The stubs reproduce that in two halves: the target catches the {@link Throwable}, stashes
 * it in the loader's {@linkplain JPEGImageLoader#setPendingUpcallFailure pending slot} and returns
 * {@link #IIO_READ_ERROR}; the library unwinds through libjpeg's {@code error_exit} and the downcall
 * returns {@link #IIO_ERR_PENDING}, which {@link #check} turns back into the stashed exception,
 * thrown from the wrapper on the calling thread. One site differs deliberately: a libjpeg warning
 * ({@code emit_warning} with a non-{@code NULL} message) had its exception cleared by the JNI glue, so
 * {@link #onEmitWarning} swallows a failure there and the library ignores that return value; only the
 * missing-EOI warning ({@code NULL} message) can abort.
 *
 * <h2>Registry lifetime</h2>
 * The table and its stubs live in {@link Arena#global()}: created once, valid for the life of the
 * process, which satisfies {@code iio_create}'s contract that the stub addresses stay valid until
 * {@code iio_dispose} returns, for every decoder there will ever be. What is per loader is the
 * registry entry, represented by a {@link Callbacks}: added before {@code iio_create}, removed from
 * {@link JPEGImageLoader#dispose} after {@code iio_dispose} has returned, so that the id resolves for
 * as long as any downcall on the decoder can call back. {@link Callbacks#unregister} is idempotent,
 * as {@code dispose} is.
 * <p>
 * The registry holds {@link WeakReference}s, as the JNI glue held a weak global reference: a loader
 * that is constructed and then abandoned without {@code dispose} is not pinned, stream and all, for
 * the life of the JVM by its own callback plumbing. An upcall can never observe a cleared reference:
 * each downcall that calls back ({@link #create}, {@link #startDecompression}, {@link #decompress})
 * takes the loader as a parameter and passes it to {@link #check} after the call has returned, so
 * the loader is strongly reachable from the calling frame for the whole of the downcall.
 * <p>
 * Nothing is thrown across the boundary. Every fallible function returns an {@code IIO_*} status and
 * writes a NUL-terminated message into a caller-owned buffer; {@link #check} is the one place that
 * turns it into the exception the JNI glue threw, with the same text: {@link #IIO_ERR_IO} into
 * {@link IOException}, {@link #IIO_ERR_OOM} into {@link OutOfMemoryError}.
 */
final class JPEGNative {

    /**
     * The {@code iio_*} ABI revision this class is written against ({@code IIO_ABI_VERSION}).
     * 2: added {@code iio_sizeof_callbacks}. Nothing existing changed; the bump rests on the rule
     * that every symbol is bound eagerly, {@code iio_abi_version} first, so against a library built
     * before the symbol existed it turns "missing native symbol: iio_sizeof_callbacks" into the
     * version mismatch {@link #checkAbiVersion} exists to report.
     */
    static final int ABI_VERSION = 2;

    static final int IIO_OK = 0;
    static final int IIO_ERR_IO = 1;
    static final int IIO_ERR_OOM = 2;
    static final int IIO_ERR_PENDING = 3;

    /**
     * Returned by a stub that caught a Java exception and stashed it. Distinct from the
     * {@code InputStream} end-of-stream values 0 and -1, which the stubs normalise every negative
     * result to.
     */
    static final int IIO_READ_ERROR = -2;

    /** {@code IIO_ERR_BUF_SIZE}: holds every message the library writes. */
    static final int IIO_ERR_BUF_SIZE = 256;

    /**
     * libjpeg's {@code JMSG_LENGTH_MAX}. The library formats a warning into
     * {@code char buffer[JMSG_LENGTH_MAX]} and {@code format_message} always NUL-terminates it, so a
     * read bounded here stops at the terminator and can never leave the buffer.
     */
    private static final int MESSAGE_CAP = 200;

    /**
     * {@code IioImageInfo}: five consecutive {@code int32_t}, no padding, {@code sizeof == 20}. A test
     * compares {@code byteSize()} with {@link #sizeofImageInfo()}.
     */
    static final StructLayout IMAGE_INFO_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("width"),
            JAVA_INT.withName("height"),
            JAVA_INT.withName("jpeg_color_space"),
            JAVA_INT.withName("out_color_space"),
            JAVA_INT.withName("num_components"));

    /**
     * {@code IioJpegCallbacks}: four function pointers in declaration order, {@code 4 * sizeof(void*)}
     * and no padding. A test compares {@code byteSize()} with {@link #sizeofCallbacks()}.
     */
    static final StructLayout CALLBACKS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("read"),
            ADDRESS.withName("skip"),
            ADDRESS.withName("emit_warning"),
            ADDRESS.withName("update_progress"));

    private static final long WIDTH_OFFSET = offset(IMAGE_INFO_LAYOUT, "width");
    private static final long HEIGHT_OFFSET = offset(IMAGE_INFO_LAYOUT, "height");
    private static final long JPEG_COLOR_SPACE_OFFSET = offset(IMAGE_INFO_LAYOUT, "jpeg_color_space");
    private static final long OUT_COLOR_SPACE_OFFSET = offset(IMAGE_INFO_LAYOUT, "out_color_space");
    private static final long NUM_COMPONENTS_OFFSET = offset(IMAGE_INFO_LAYOUT, "num_components");

    private static final long READ_OFFSET = offset(CALLBACKS_LAYOUT, "read");
    private static final long SKIP_OFFSET = offset(CALLBACKS_LAYOUT, "skip");
    private static final long EMIT_WARNING_OFFSET = offset(CALLBACKS_LAYOUT, "emit_warning");
    private static final long UPDATE_PROGRESS_OFFSET = offset(CALLBACKS_LAYOUT, "update_progress");

    private static final String LIBRARY_NAME = "javafx_iio";

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;
    private static final List<String> BOUND_SYMBOLS = new ArrayList<>();

    static {
        // The library has to be loaded by this class loader before loaderLookup() can see it.
        NativeLibLoader.loadLibrary(LIBRARY_NAME);
        LOOKUP = SymbolLookup.loaderLookup();
    }

    /* Binding order matters: the ABI guard is bound and checked before any other symbol. */
    private static final MethodHandle IIO_ABI_VERSION = bind("iio_abi_version",
            FunctionDescriptor.of(JAVA_INT));

    static {
        checkAbiVersion();
    }

    private static final MethodHandle IIO_SIZEOF_IMAGE_INFO = bind("iio_sizeof_image_info",
            FunctionDescriptor.of(JAVA_LONG));
    private static final MethodHandle IIO_SIZEOF_CALLBACKS = bind("iio_sizeof_callbacks",
            FunctionDescriptor.of(JAVA_LONG));
    private static final MethodHandle IIO_CREATE = bind("iio_create",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));
    private static final MethodHandle IIO_GET_ICC_PROFILE_LENGTH = bind("iio_get_icc_profile_length",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle IIO_GET_ICC_PROFILE = bind("iio_get_icc_profile",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
    private static final MethodHandle IIO_START_DECOMPRESSION = bind("iio_start_decompression",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS,
                    ADDRESS, JAVA_INT));
    private static final MethodHandle IIO_DECOMPRESS = bind("iio_decompress",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, JAVA_INT));
    private static final MethodHandle IIO_DISPOSE = bind("iio_dispose",
            FunctionDescriptor.ofVoid(ADDRESS));

    /* The four slots of IioJpegCallbacks, in declaration order. */
    private static final FunctionDescriptor READ_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT);
    private static final FunctionDescriptor SKIP_FD = FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_LONG);
    private static final FunctionDescriptor EMIT_WARNING_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS);
    private static final FunctionDescriptor UPDATE_PROGRESS_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT);

    private static final MethodHandle READ_TARGET = upcallTarget("onRead", READ_FD);
    private static final MethodHandle SKIP_TARGET = upcallTarget("onSkip", SKIP_FD);
    private static final MethodHandle EMIT_WARNING_TARGET = upcallTarget("onEmitWarning", EMIT_WARNING_FD);
    private static final MethodHandle UPDATE_PROGRESS_TARGET = upcallTarget("onUpdateProgress", UPDATE_PROGRESS_FD);

    /**
     * The {@code void* user} registry: one entry per live loader, keyed by the id its
     * {@link Callbacks} passes to C. Never a pointer; native code never holds a Java reference. The
     * values are weak, and safely so: see the class comment.
     */
    private static final Map<Long, WeakReference<JPEGImageLoader>> REGISTRY = new ConcurrentHashMap<>();

    /** Ids start at 1 so that no table ever carries a {@code NULL} user. */
    private static final AtomicLong NEXT_ID = new AtomicLong(1);

    /**
     * The one {@code IioJpegCallbacks} table of the process and, in the same global arena, the four
     * stubs it points to. Stateless: the targets resolve the loader from {@code user}. Declared after
     * the targets, descriptors and offsets it is built from.
     */
    private static final MemorySegment CALLBACK_TABLE = allocateCallbackTable();

    private JPEGNative() {
    }

    /**
     * Resolves {@code name} and binds it as a plain downcall. None of the {@code iio_*} functions
     * may be {@link Linker.Option#critical critical}: three of them block and call back into Java,
     * and the rest take no heap array.
     *
     * @throws UnsatisfiedLinkError if the library does not export {@code name}
     */
    @SuppressWarnings("restricted")
    private static MethodHandle bind(String name, FunctionDescriptor descriptor) {
        MemorySegment symbol = LOOKUP.find(name).orElseThrow(
                () -> new UnsatisfiedLinkError("missing native symbol: " + name + " in " + LIBRARY_NAME));
        BOUND_SYMBOLS.add(name);
        return LINKER.downcallHandle(symbol, descriptor);
    }

    private static void checkAbiVersion() {
        int actual;
        try {
            actual = (int) IIO_ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (actual != ABI_VERSION) {
            throw new UnsatisfiedLinkError(LIBRARY_NAME + " ABI version mismatch: expected " + ABI_VERSION
                    + ", found " + actual);
        }
    }

    private static MethodHandle upcallTarget(String name, FunctionDescriptor descriptor) {
        try {
            return MethodHandles.lookup().findStatic(JPEGNative.class, name, descriptor.toMethodType());
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("restricted")
    private static MemorySegment upcallStub(MethodHandle target, FunctionDescriptor descriptor, Arena arena) {
        return LINKER.upcallStub(target, descriptor, arena);
    }

    /** Builds {@link #CALLBACK_TABLE}: the four stubs and the table that points to them, once per process. */
    private static MemorySegment allocateCallbackTable() {
        Arena arena = Arena.global();
        MemorySegment table = arena.allocate(CALLBACKS_LAYOUT);
        table.set(ADDRESS, READ_OFFSET, upcallStub(READ_TARGET, READ_FD, arena));
        table.set(ADDRESS, SKIP_OFFSET, upcallStub(SKIP_TARGET, SKIP_FD, arena));
        table.set(ADDRESS, EMIT_WARNING_OFFSET, upcallStub(EMIT_WARNING_TARGET, EMIT_WARNING_FD, arena));
        table.set(ADDRESS, UPDATE_PROGRESS_OFFSET, upcallStub(UPDATE_PROGRESS_TARGET, UPDATE_PROGRESS_FD, arena));
        return table;
    }

    /**
     * Gives a zero-length segment handed to an upcall the bound the C contract promises. The only
     * {@code reinterpret} in this package; every other segment C hands out is read here.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment bounded(MemorySegment segment, long byteSize) {
        return segment.reinterpret(byteSize);
    }

    private static long offset(StructLayout layout, String field) {
        return layout.byteOffset(PathElement.groupElement(field));
    }

    private static Error unexpected(Throwable t) {
        if (t instanceof Error e) {
            return e;
        }
        if (t instanceof RuntimeException e) {
            throw e;
        }
        // Only linkage failures can land here: C cannot throw.
        return new AssertionError(t);
    }

    private static int flag(boolean value) {
        return value ? 1 : 0;
    }

    /**
     * The one place an {@code iio_*} status becomes an exception.
     *
     * @param err the NUL-terminated message the library wrote; {@code ""} for {@link #IIO_ERR_PENDING}
     * @param loader the loader whose pending slot holds the exception behind {@link #IIO_ERR_PENDING}
     */
    private static void check(int status, MemorySegment err, JPEGImageLoader loader) throws IOException {
        switch (status) {
            case IIO_OK:
                return;
            case IIO_ERR_IO:
                throw new IOException(err.getString(0));
            case IIO_ERR_OOM:
                throw new OutOfMemoryError(err.getString(0));
            case IIO_ERR_PENDING:
                throw pending(loader);
            default:
                throw new IllegalStateException("unknown " + LIBRARY_NAME + " status " + status);
        }
    }

    /**
     * Takes the exception a stub stashed and rethrows it as the JNI native method did: unchecked
     * exceptions and errors directly, an {@link IOException} returned for the caller to throw.
     * {@code InputStream.read} and {@code skip} declare only {@link IOException} and the listener
     * callbacks declare nothing checked, so the final wrap is unreachable and exists to keep the
     * method total.
     */
    private static IOException pending(JPEGImageLoader loader) {
        Throwable failure = loader.takePendingUpcallFailure();
        if (failure instanceof IOException e) {
            return e;
        }
        if (failure instanceof RuntimeException e) {
            throw e;
        }
        if (failure instanceof Error e) {
            throw e;
        }
        if (failure == null) {
            throw new IllegalStateException(LIBRARY_NAME
                    + " reported a pending exception but no upcall stashed one");
        }
        return new IOException(failure);
    }

    /* ---------------------------------------------------------------------------------------------
     * Registry
     * ------------------------------------------------------------------------------------------- */

    /**
     * One loader's registration: the id C receives as {@code void* user}, and the process-wide table
     * to hand to {@code iio_create} with it. Registered before {@code iio_create}; unregistered after
     * {@code iio_dispose} has returned. Nothing native belongs to it.
     */
    static final class Callbacks {

        private final long id;

        private Callbacks(long id) {
            this.id = id;
        }

        /** The table to pass as {@code cb}: the process's, valid for ever. */
        MemorySegment table() {
            return CALLBACK_TABLE;
        }

        /** The registry id as the {@code void* user} C hands back unchanged. */
        MemorySegment user() {
            return MemorySegment.ofAddress(id);
        }

        /**
         * Removes the registry entry; nothing is freed. Safe to call more than once. Must run only
         * after {@code iio_dispose} of the decoder that received this id has returned.
         */
        void unregister() {
            REGISTRY.remove(id);
        }
    }

    /** Registers {@code loader} under a fresh id. */
    static Callbacks register(JPEGImageLoader loader) {
        long id = NEXT_ID.getAndIncrement();
        REGISTRY.put(id, new WeakReference<>(loader));
        return new Callbacks(id);
    }

    /**
     * The loader behind {@code user}: {@code null} for an id never registered or already
     * unregistered, and never for a collected one while a downcall is in progress (class comment).
     */
    private static JPEGImageLoader loader(MemorySegment user) {
        WeakReference<JPEGImageLoader> ref = REGISTRY.get(user.address());
        return ref == null ? null : ref.get();
    }

    /* ---------------------------------------------------------------------------------------------
     * Upcall targets: invoked by libjpeg on the thread inside iio_create, iio_start_decompression or
     * iio_decompress. Each catches Throwable; see the class comment for the stash-and-abort contract.
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code read}: {@code InputStream.read(buffer, 0, len)} copied into {@code dst}. Returns the
     * count, 0 or -1 at end of stream (every negative result is normalised to -1, as the source
     * manager treated every value {@code <= 0} alike), never more than {@code len} however much the
     * stream claims; {@link #IIO_READ_ERROR} when the stream threw.
     */
    private static int onRead(MemorySegment user, MemorySegment dst, int len) {
        JPEGImageLoader loader = loader(user);
        if (loader == null) {
            return -1;
        }
        try {
            byte[] buffer = loader.streamBuffer(len);
            int count = loader.stream().read(buffer, 0, len);
            if (count <= 0) {
                return count < 0 ? -1 : 0;
            }
            count = Math.min(count, len);
            MemorySegment.copy(buffer, 0, bounded(dst, len), JAVA_BYTE, 0, count);
            return count;
        } catch (Throwable t) {
            loader.setPendingUpcallFailure(t);
            return IIO_READ_ERROR;
        }
    }

    /**
     * {@code skip}: {@code InputStream.skip(count)}. A negative result is normalised to 0 - the
     * source manager treated every value {@code <= 0} as end of stream - so that
     * {@link #IIO_READ_ERROR} stays unambiguous; a short positive skip is returned as-is.
     */
    private static long onSkip(MemorySegment user, long count) {
        JPEGImageLoader loader = loader(user);
        if (loader == null) {
            return 0L;
        }
        try {
            long skipped = loader.stream().skip(count);
            return skipped < 0L ? 0L : skipped;
        } catch (Throwable t) {
            loader.setPendingUpcallFailure(t);
            return IIO_READ_ERROR;
        }
    }

    /**
     * {@code emit_warning}. A {@code NULL} message is the source manager's missing-EOI warning, which
     * the JNI delivered as {@code emitWarning((String) null)} and let abort the decode if the listener
     * threw; that is preserved, null included. A non-{@code NULL} message is a libjpeg warning whose
     * exception the JNI glue cleared and swallowed without logging; the library ignores this return
     * value there, so the stub swallows too.
     */
    private static int onEmitWarning(MemorySegment user, MemorySegment msg) {
        JPEGImageLoader loader = loader(user);
        if (loader == null) {
            return 0;
        }
        if (msg.address() == 0L) {
            try {
                loader.emitDecoderWarning(null);
                return 0;
            } catch (Throwable t) {
                loader.setPendingUpcallFailure(t);
                return IIO_READ_ERROR;
            }
        }
        try {
            loader.emitDecoderWarning(bounded(msg, MESSAGE_CAP).getString(0));
        } catch (Throwable swallowed) {
            // checkAndClearException parity: the JNI glue cleared the exception and carried on.
        }
        return 0;
    }

    /** {@code update_progress}: {@code updateImageProgress(scanline)}; a throw aborts the decode. */
    private static int onUpdateProgress(MemorySegment user, int scanline) {
        JPEGImageLoader loader = loader(user);
        if (loader == null) {
            return 0;
        }
        try {
            loader.updateImageProgress(scanline);
            return 0;
        } catch (Throwable t) {
            loader.setPendingUpcallFailure(t);
            return IIO_READ_ERROR;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Downcalls
     * ------------------------------------------------------------------------------------------- */

    /**
     * What {@code iio_create} reports: the decoder handle and the {@code IioImageInfo} fields, the
     * scalars the JNI {@code setInputAttributes} callback carried. A tables-only datastream leaves
     * {@code width} 0, in which case the JNI made no {@code setInputAttributes} call at all.
     */
    record Header(MemorySegment decoder, int width, int height, int jpegColorSpace, int outColorSpace,
            int numComponents) {
    }

    /**
     * What {@code iio_start_decompression} reports: the pair the JNI {@code setOutputAttributes} callback
     * carried plus the component count.
     */
    record Geometry(int width, int height, int components) {
    }

    /**
     * Former {@code initDecompressor}: allocates a decoder bound to {@code callbacks}, reads the JPEG
     * header through them and reports the image info. Blocks and calls back.
     *
     * @throws IOException for a libjpeg or ICC-marker error, or the stream's own exception
     * @throws OutOfMemoryError {@code "Initializing Reader"} or {@code "Reading ICC profile"}
     */
    static Header create(JPEGImageLoader loader, Callbacks callbacks) throws IOException {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment handle = scratch.allocate(ADDRESS);
            MemorySegment info = scratch.allocate(IMAGE_INFO_LAYOUT);
            MemorySegment err = scratch.allocate(IIO_ERR_BUF_SIZE);
            int status;
            try {
                status = (int) IIO_CREATE.invokeExact(callbacks.table(), callbacks.user(), handle, info, err,
                        IIO_ERR_BUF_SIZE);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            check(status, err, loader);
            return new Header(handle.get(ADDRESS, 0),
                    info.get(JAVA_INT, WIDTH_OFFSET),
                    info.get(JAVA_INT, HEIGHT_OFFSET),
                    info.get(JAVA_INT, JPEG_COLOR_SPACE_OFFSET),
                    info.get(JAVA_INT, OUT_COLOR_SPACE_OFFSET),
                    info.get(JAVA_INT, NUM_COMPONENTS_OFFSET));
        }
    }

    /**
     * {@code iio_get_icc_profile_length}: the byte count, 0 when the file carries none, -1 for a
     * {@code NULL} handle.
     */
    static int iccProfileLength(MemorySegment decoder) {
        try {
            return (int) IIO_GET_ICC_PROFILE_LENGTH.invokeExact(decoder);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * The embedded ICC profile {@code iio_create} reassembled, or {@code null} when the file carries
     * none - the JNI {@code read_icc_profile} returned a null array then, never an empty one.
     */
    static byte[] iccProfile(MemorySegment decoder) {
        int length = iccProfileLength(decoder);
        if (length == 0) {
            return null;
        }
        if (length < 0) {
            throw new IllegalStateException("ICC profile length of a null " + LIBRARY_NAME + " decoder");
        }
        byte[] profile = new byte[length];
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment dst = scratch.allocate(length);
            int copied;
            try {
                copied = (int) IIO_GET_ICC_PROFILE.invokeExact(decoder, dst, length);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (copied != length) {
                throw new IllegalStateException("ICC profile copy returned " + copied + " for " + length + " bytes");
            }
            MemorySegment.copy(dst, JAVA_BYTE, 0, profile, 0, length);
        }
        return profile;
    }

    /**
     * Former {@code startDecompression}: selects the output colour space and the libjpeg scale for
     * {@code destWidth x destHeight}, starts decompression and reports the output geometry. Blocks
     * and calls back.
     *
     * @throws IOException for a libjpeg error, or the stream's own exception
     */
    static Geometry startDecompression(JPEGImageLoader loader, MemorySegment decoder, int outColorSpace,
            int destWidth, int destHeight) throws IOException {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment outWidth = scratch.allocate(JAVA_INT);
            MemorySegment outHeight = scratch.allocate(JAVA_INT);
            MemorySegment outComponents = scratch.allocate(JAVA_INT);
            MemorySegment err = scratch.allocate(IIO_ERR_BUF_SIZE);
            int status;
            try {
                status = (int) IIO_START_DECOMPRESSION.invokeExact(decoder, outColorSpace, destWidth, destHeight,
                        outWidth, outHeight, outComponents, err, IIO_ERR_BUF_SIZE);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            check(status, err, loader);
            return new Geometry(outWidth.get(JAVA_INT, 0), outHeight.get(JAVA_INT, 0),
                    outComponents.get(JAVA_INT, 0));
        }
    }

    /**
     * Former {@code decompressIndirect}: decodes every scanline, row stride
     * {@code width * components} with no padding, into {@code pixels}, which must hold
     * {@code stride * height} bytes. The library writes into an off-heap buffer of that size and the
     * result is copied into the array once the call has returned, because the call blocks and calls
     * back and so cannot pin a heap array. Blocks and calls back.
     *
     * @param reportProgress whether to drive {@code update_progress} once per scanline and once more
     *        with the output height, as the JNI loop did when the loader had listeners
     * @throws IOException for a libjpeg error, or the stream's or a listener's own exception
     * @throws OutOfMemoryError {@code "Reading JPEG Stream"}
     */
    static void decompress(JPEGImageLoader loader, MemorySegment decoder, boolean reportProgress, byte[] pixels)
            throws IOException {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment dst = scratch.allocate(pixels.length);
            MemorySegment err = scratch.allocate(IIO_ERR_BUF_SIZE);
            int status;
            try {
                status = (int) IIO_DECOMPRESS.invokeExact(decoder, flag(reportProgress), dst, (long) pixels.length,
                        err, IIO_ERR_BUF_SIZE);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            check(status, err, loader);
            MemorySegment.copy(dst, JAVA_BYTE, 0, pixels, 0, pixels.length);
        }
    }

    /**
     * Former {@code disposeNative}: {@code jpeg_destroy} plus every buffer the handle owns. {@code NULL}
     * is ignored.
     */
    static void dispose(MemorySegment decoder) {
        try {
            IIO_DISPOSE.invokeExact(decoder);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Test hooks (reached through JPEGNativeShim)
     * ------------------------------------------------------------------------------------------- */

    /** Runs the class initializer: loads the library, binds every symbol, checks the ABI version. */
    static void ensureLoaded() {
    }

    /** The symbols this class bound, in binding order. */
    static List<String> boundSymbols() {
        return Collections.unmodifiableList(new ArrayList<>(BOUND_SYMBOLS));
    }

    static int abiVersion() {
        try {
            return (int) IIO_ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The C compiler's {@code sizeof(IioImageInfo)}. */
    static long sizeofImageInfo() {
        try {
            return (long) IIO_SIZEOF_IMAGE_INFO.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code sizeof(IioJpegCallbacks)} as the C compiler laid it out. */
    static long sizeofCallbacks() {
        try {
            return (long) IIO_SIZEOF_CALLBACKS.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The number of loaders currently registered, that is, created and not yet disposed. */
    static int registrySize() {
        return REGISTRY.size();
    }

    /** The address of the process-wide callback table, the same for every loader. */
    static long callbackTableAddress() {
        return CALLBACK_TABLE.address();
    }

    /** The stub address in slot {@code field} of the process-wide callback table. */
    static long callbackSlotAddress(String field) {
        return CALLBACK_TABLE.get(ADDRESS, offset(CALLBACKS_LAYOUT, field)).address();
    }
}

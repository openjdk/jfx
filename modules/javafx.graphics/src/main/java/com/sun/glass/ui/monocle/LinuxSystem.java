/*
 * Copyright (c) 2014, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * LinuxSystem provides access to Linux system calls. Except where noted, each
 * method in LinuxSystem corresponds to exactly one system call taking
 * parameters in the same order and returning the same result as the
 * corresponding C function.
 *
 * LinuxSystem is a singleton. Its instance is obtained by calling
 * LinuxSystem.getLinuxSystem().
 * <p>
 * Each system call binds the libc function of the same name through {@code java.lang.foreign}, where
 * LinuxSystem.c of commit 21d5a654f6 wrapped it in a JNI function of the Monocle glass library (Udev.c of
 * that commit the socket calls, EPDSystem.c the EPD ioctl and field accessors); the struct accessors read and
 * write the {@link #FB_VAR_SCREENINFO}, {@link #INPUT_ABSINFO} and {@link #SOCKADDR_NL} layouts, and the
 * ioctl request macros are the arithmetic of {@code asm-generic/ioctl.h}. Together with {@link C} this is the
 * facade class of the package: every restricted method of the FFM API that Monocle uses is called from these
 * two classes. libc is bound by the lazy holder {@code Libc} on {@link #loadLibrary()} or on the first system
 * call, never when this class initialises, which happens on every platform and in the Headless platform that
 * makes no system call at all. The binding assumes a C {@code long} of 8 bytes and refuses anything else with
 * an {@link UnsatisfiedLinkError}, see {@link #requireLp64}. The constants taken from the kernel and libc
 * headers (the ioctl encoding, the socket options) are those of x86-64 and aarch64, the two LP64 targets; other
 * LP64 ABIs, which that check would accept, are not targets.
 * <p>
 * {@code errno} is captured by the linker right after every fallible call into a per-thread segment that
 * {@link #errno()} reads, so the value is the one the call left, whatever the JVM did in between. The C read
 * {@code errno} in a second JNI call, after whatever ran between the two.
 * <p>
 * The calls the frame buffer and input device classes make go through a {@link Backend}, which is the libc
 * binding unless a test installs another with {@link #setBackendForTesting}.
 */
class LinuxSystem {
    private static LinuxSystem instance = new LinuxSystem();

    /**
     * Obtains the single instance of LinuxSystem.
     *
     * loadLibrary() must be called on the LinuxSystem instance before any
     * system calls can be made using it.
     */
    static LinuxSystem getLinuxSystem() {
        return instance;
    }

    private LinuxSystem() {
    }

    /**
     * Binds the libc functions behind the system calls of LinuxSystem. This
     * method must be called before any other instance methods of LinuxSystem.
     * If this method is called multiple times, it has no effect after the
     * first call.
     *
     * @throws UnsatisfiedLinkError if a libc symbol cannot be found or the
     * platform is not LP64: the error the JNI library loader raised here when
     * the Monocle glass library could not be loaded
     */
    void loadLibrary() {
        try {
            Libc.require();
        } catch (NoClassDefFoundError e) {
            throw unbound(e);
        }
    }

    /**
     * Whether libc can be bound: binds it on the first call, as {@link #loadLibrary()} does, and answers false
     * instead of throwing when that fails.
     */
    static boolean isLibraryLoaded() {
        try {
            Libc.require();
            return Libc.LINKED;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * The failure of the {@code Libc} holder's initialisation, seen again. The linker's UnsatisfiedLinkError is
     * an Error, so the first use sees it unwrapped; every later use sees the NoClassDefFoundError the JVM raises
     * for a class whose initialisation failed, turned back into the UnsatisfiedLinkError callers handle.
     */
    private static UnsatisfiedLinkError unbound(NoClassDefFoundError e) {
        UnsatisfiedLinkError error = new UnsatisfiedLinkError("libc is not bound: " + e.getMessage());
        error.initCause(e);
        return error;
    }

    /** What a call through {@code Libc} can throw: linkage failures and the argument checks of this class. */
    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException runtime) {
            return runtime;
        }
        if (t instanceof NoClassDefFoundError e) {
            throw unbound(e);
        }
        if (t instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(t);
    }

    /**
     * Refuses a C {@code long} that is not 8 bytes: {@code ssize_t}, {@code size_t}, {@code off_t} and the
     * {@code ioctl} request are bound as 8-byte values, the data model of aarch64 and x86-64 Linux (LP64). The
     * message names the reason and the size found.
     *
     * @param longByteSize the byte size of the linker's canonical {@code long} layout
     * @throws UnsatisfiedLinkError if it is not 8
     */
    static void requireLp64(long longByteSize) {
        if (longByteSize != 8) {
            throw new UnsatisfiedLinkError("cannot bind libc for Monocle: C long is " + longByteSize
                    + " bytes, but LinuxSystem binds long, size_t, off_t and the ioctl request as 8-byte"
                    + " values (LP64 only: aarch64, x86-64)");
        }
    }

    /**
     * libc, through the linker's default lookup; bound on first use only, by {@link #loadLibrary()} or the first
     * system call. Every function that reports through {@code errno} is linked with
     * {@code captureCallState("errno")}: the linker copies the thread's {@code errno} into the leading capture
     * segment right after the call returns, and {@link LinuxSystem#errno()} reads the calling thread's
     * segment - one per thread, allocated from an automatic arena the thread-local keeps reachable. Nothing
     * resets it between calls, as nothing reset the C's {@code errno}. {@code dlopen}, {@code dlsym} and
     * {@code dlclose} report through {@code dlerror}; {@code strerror} and {@code dlerror} cannot fail: those
     * are plain downcalls.
     */
    @SuppressWarnings("restricted")
    private static final class Libc {

        static final Linker LINKER = Linker.nativeLinker();

        /** C {@code long}, also {@code ssize_t} and, on LP64 glibc, {@code off_t}; refused unless 8 bytes. */
        static final MemoryLayout C_LONG = lp64Long();

        /** C {@code size_t}. */
        static final MemoryLayout SIZE_T = LINKER.canonicalLayouts().get("size_t");

        /** C {@code off_t}: the linker has no canonical off_t, and it is {@code long} on LP64 glibc. */
        static final MemoryLayout OFF_T = C_LONG;

        /** The {@code ioctl} request: {@code unsigned long} in glibc. */
        static final MemoryLayout REQUEST_T = C_LONG;

        static final StructLayout CAPTURE_LAYOUT = Linker.Option.captureStateLayout();

        static final VarHandle CAPTURED_ERRNO = CAPTURE_LAYOUT.varHandle(groupElement("errno"));

        /** The calling thread's capture segment: where the linker writes {@code errno} after each call. */
        static final ThreadLocal<MemorySegment> CAPTURE = ThreadLocal.withInitial(
                () -> Arena.ofAuto().allocate(CAPTURE_LAYOUT));

        /** {@code libc!symbol} for every symbol this class binds, in binding order; read by tests. */
        static final Map<String, Long> BOUND = Collections.synchronizedMap(new LinkedHashMap<>());

        /** {@code int setenv(const char *name, const char *value, int overwrite)}. */
        static final MethodHandle SETENV = fallible("setenv",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

        /** {@code int open(const char *pathname, int flags, ...)}: variadic, the mode is its third argument. */
        static final MethodHandle OPEN = fallible("open",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT), Linker.Option.firstVariadicArg(2));

        /** {@code int close(int fd)}. */
        static final MethodHandle CLOSE = fallible("close", FunctionDescriptor.of(JAVA_INT, JAVA_INT));

        /** {@code off_t lseek(int fd, off_t offset, int whence)}. */
        static final MethodHandle LSEEK = fallible("lseek", FunctionDescriptor.of(OFF_T, JAVA_INT, OFF_T, JAVA_INT));

        /** {@code ssize_t write(int fd, const void *buf, size_t count)}. */
        static final MethodHandle WRITE = fallible("write", FunctionDescriptor.of(C_LONG, JAVA_INT, ADDRESS, SIZE_T));

        /** {@code ssize_t read(int fd, void *buf, size_t count)}. */
        static final MethodHandle READ = fallible("read", FunctionDescriptor.of(C_LONG, JAVA_INT, ADDRESS, SIZE_T));

        /** {@code long sysconf(int name)}. */
        static final MethodHandle SYSCONF = fallible("sysconf", FunctionDescriptor.of(C_LONG, JAVA_INT));

        /** {@code int ioctl(int fd, unsigned long request, ...)}: variadic, the argument is a pointer here. */
        static final MethodHandle IOCTL = fallible("ioctl",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, REQUEST_T, ADDRESS), Linker.Option.firstVariadicArg(2));

        /** {@code char *strerror(int errnum)}. */
        static final MethodHandle STRERROR = plain("strerror", FunctionDescriptor.of(ADDRESS, JAVA_INT));

        /**
         * {@code void *dlopen(const char *filename, int flags)}, through libc and not
         * {@code SymbolLookup.libraryLookup} because the callers' {@code RTLD_GLOBAL} cannot be expressed there:
         * the EGL and GLES symbols have to land in the global scope of the process.
         */
        static final MethodHandle DLOPEN = plain("dlopen", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));

        /** {@code char *dlerror(void)}. */
        static final MethodHandle DLERROR = plain("dlerror", FunctionDescriptor.of(ADDRESS));

        /** {@code void *dlsym(void *handle, const char *symbol)}. */
        static final MethodHandle DLSYM = plain("dlsym", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

        /** {@code int dlclose(void *handle)}. */
        static final MethodHandle DLCLOSE = plain("dlclose", FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** {@code void *mmap(void *addr, size_t length, int prot, int flags, int fd, off_t offset)}. */
        static final MethodHandle MMAP = fallible("mmap",
                FunctionDescriptor.of(ADDRESS, ADDRESS, SIZE_T, JAVA_INT, JAVA_INT, JAVA_INT, OFF_T));

        /** {@code int munmap(void *addr, size_t length)}. */
        static final MethodHandle MUNMAP = fallible("munmap", FunctionDescriptor.of(JAVA_INT, ADDRESS, SIZE_T));

        /** {@code int mkfifo(const char *pathname, mode_t mode)}. */
        static final MethodHandle MKFIFO = fallible("mkfifo", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

        /** {@code int socket(int domain, int type, int protocol)}. */
        static final MethodHandle SOCKET = fallible("socket",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));

        /** {@code int setsockopt(int sockfd, int level, int optname, const void *optval, socklen_t optlen)}. */
        static final MethodHandle SETSOCKOPT = fallible("setsockopt",
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

        /** {@code int bind(int sockfd, const struct sockaddr *addr, socklen_t addrlen)}. */
        static final MethodHandle BIND = fallible("bind", FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

        /** {@code ssize_t recv(int sockfd, void *buf, size_t len, int flags)}. */
        static final MethodHandle RECV = fallible("recv",
                FunctionDescriptor.of(C_LONG, JAVA_INT, ADDRESS, SIZE_T, JAVA_INT));

        /** True once every handle above is bound; set last, and reading it initialises this class. */
        static final boolean LINKED = !BOUND.isEmpty();

        /** Initialises this class, binding libc, and does nothing else. */
        static void require() {
        }

        static MemorySegment capture() {
            return CAPTURE.get();
        }

        static int errno() {
            return (int) CAPTURED_ERRNO.get(capture(), 0L);
        }

        private static MemoryLayout lp64Long() {
            MemoryLayout layout = LINKER.canonicalLayouts().get("long");
            requireLp64(layout.byteSize());
            return layout;
        }

        /** Links {@code name} with its {@code errno} captured into the leading segment argument. */
        private static MethodHandle fallible(String name, FunctionDescriptor descriptor, Linker.Option... options) {
            Linker.Option[] all = new Linker.Option[options.length + 1];
            all[0] = Linker.Option.captureCallState("errno");
            System.arraycopy(options, 0, all, 1, options.length);
            return bind(name, descriptor, all);
        }

        private static MethodHandle plain(String name, FunctionDescriptor descriptor) {
            return bind(name, descriptor);
        }

        /** Resolves {@code name} in libc and links it as an ordinary (never critical) downcall. */
        private static MethodHandle bind(String name, FunctionDescriptor descriptor, Linker.Option... options) {
            MemorySegment symbol = LINKER.defaultLookup().find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError("libc exports no " + name));
            BOUND.put("libc!" + name, symbol.address());
            return LINKER.downcallHandle(symbol, descriptor, options);
        }
    }

    /** {@code libc!symbol} of every bound symbol, in binding order. Binds libc if that has not happened. */
    static List<String> boundSymbols() {
        synchronized (Libc.BOUND) {
            return new ArrayList<>(Libc.BOUND.keySet());
        }
    }

    /** The address {@code libc!symbol} was bound at, or 0 if it was not bound. */
    static long boundAddress(String qualifiedName) {
        Long address = Libc.BOUND.get(qualifiedName);
        return address == null ? 0L : address;
    }

    /**
     * The system calls of the frame buffer and input device classes, as an interface a test can stand in for:
     * the libc binding is the default, and {@link #setBackendForTesting} installs another, so that an
     * EPDFrameBuffer or a LinuxFrameBuffer can be driven against a recorded ioctl sequence on any platform.
     * {@code errno} and {@code strerror} belong to it because every caller reads them right after one of these
     * calls. Everything else in this class (lseek, sysconf, mkfifo, setenv, the socket and dl calls, memcpy)
     * always goes to libc.
     */
    interface Backend {
        long open(String path, int flags);

        int close(long fd);

        long read(long fd, ByteBuffer buf, int position, int limit);

        long write(long fd, ByteBuffer buf, int position, int limit);

        int ioctl(long fd, int request, long data);

        long mmap(long addr, long length, long prot, long flags, long fd, long offset);

        int munmap(long addr, long length);

        int errno();

        String strerror(int errnum);
    }

    private static volatile Backend backend = LibcBackend.INSTANCE;

    /** Installs the system calls a test drives the frame buffer classes with; null restores the libc binding. */
    static void setBackendForTesting(Backend replacement) {
        backend = replacement == null ? LibcBackend.INSTANCE : replacement;
    }

    /**
     * The struct at {@code p} as a segment of the layout's size: the {@code (struct x *) asPtr(p)} cast of the C.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment struct(long p, StructLayout layout) {
        return MemorySegment.ofAddress(p).reinterpret(layout.byteSize());
    }

    /** The NUL-terminated string at {@code s}, or null for NULL, as {@code NewStringUTF} made of a {@code char *}. */
    @SuppressWarnings("restricted")
    private static String cString(MemorySegment s) {
        return s.address() == 0 ? null : s.reinterpret(Long.MAX_VALUE).getString(0);
    }

    /**
     * The bytes {@code [position, limit)} of a direct buffer's backing memory, counted from its start whatever
     * its own position and limit, as {@code GetDirectBufferAddress} plus the C's {@code data + position} did.
     * A buffer that is not direct is rejected, where the C passed the NULL {@code GetDirectBufferAddress}
     * answered for it; a range outside the buffer is rejected, where the C passed {@code data + position} and
     * {@code limit - position} unchecked.
     */
    private static MemorySegment range(ByteBuffer buf, int position, int limit) {
        if (!buf.isDirect()) {
            throw new IllegalArgumentException("direct ByteBuffer required");
        }
        return MemorySegment.ofBuffer(buf.duplicate().clear()).asSlice(position, limit - position);
    }

    /** The libc binding behind {@link Backend}: what production always runs. */
    private static final class LibcBackend implements Backend {

        static final LibcBackend INSTANCE = new LibcBackend();

        private LibcBackend() {
        }

        @Override
        public long open(String path, int flags) {
            try (Arena arena = Arena.ofConfined()) {
                return (int) Libc.OPEN.invokeExact(Libc.capture(), arena.allocateFrom(path), flags, 0);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @Override
        public int close(long fd) {
            try {
                return (int) Libc.CLOSE.invokeExact(Libc.capture(), (int) fd);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @Override
        public long read(long fd, ByteBuffer buf, int position, int limit) {
            try {
                MemorySegment data = range(buf, position, limit);
                return (long) Libc.READ.invokeExact(Libc.capture(), (int) fd, data, data.byteSize());
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @Override
        public long write(long fd, ByteBuffer buf, int position, int limit) {
            try {
                MemorySegment data = range(buf, position, limit);
                return (long) Libc.WRITE.invokeExact(Libc.capture(), (int) fd, data, data.byteSize());
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @Override
        public int ioctl(long fd, int request, long data) {
            try {
                return (int) Libc.IOCTL.invokeExact(Libc.capture(), (int) fd, Integer.toUnsignedLong(request),
                        MemorySegment.ofAddress(data));
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @Override
        public long mmap(long addr, long length, long prot, long flags, long fd, long offset) {
            try {
                return ((MemorySegment) Libc.MMAP.invokeExact(Libc.capture(), MemorySegment.ofAddress(addr), length,
                        (int) prot, (int) flags, (int) fd, offset)).address();
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @Override
        public int munmap(long addr, long length) {
            try {
                return (int) Libc.MUNMAP.invokeExact(Libc.capture(), MemorySegment.ofAddress(addr), length);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @Override
        public int errno() {
            try {
                return Libc.errno();
            } catch (NoClassDefFoundError e) {
                throw unbound(e);
            }
        }

        @Override
        public String strerror(int errnum) {
            try {
                return cString((MemorySegment) Libc.STRERROR.invokeExact(errnum));
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
    }

    // stdlib.h

    /** {@code setenv}; its result is discarded, as the C discarded it. */
    void setenv(String key, String value, boolean overwrite) {
        try (Arena arena = Arena.ofConfined()) {
            int ignored = (int) Libc.SETENV.invokeExact(Libc.capture(), arena.allocateFrom(key),
                    arena.allocateFrom(value), overwrite ? 1 : 0);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    // fcntl.h

    static final int O_RDONLY = 0;
    static final int O_WRONLY = 1;
    static final int O_RDWR = 2;
    static final int O_NONBLOCK = 00004000;

    /**
     * {@code open(path, flags)}: the path in UTF-8 (the C used modified UTF-8; no path contains a NUL or a
     * supplementary character), a mode of 0 in the variadic slot the C left unset.
     */
    long open(String path, int flags) {
        return backend.open(path, flags);
    }

    // unistd.h
    int close(long fd) {
        return backend.close(fd);
    }

    long lseek(long fd, long offset, int whence) {
        try {
            return (long) Libc.LSEEK.invokeExact(Libc.capture(), (int) fd, offset, whence);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * Calls the "write" function defined in unistd.h. The parameters have
     * the same meaning as in the "write" C system call,
     * except that a ByteBuffer with a position and limit are used for the
     * source data. The position and limit set on the ByteBuffer are ignored;
     * the position and limit provided as method parameters are used instead.
     * @param fd The file descriptor to which to write
     * @param buf The buffer from which to write; must be direct
     * @param position The index in buf of the first byte to write
     * @param limit The index in buf up to which to write
     * @return The number of bytes written, or -1 on failure
     * @throws IllegalArgumentException if buf is not a direct buffer
     * @throws IndexOutOfBoundsException if position is negative, or limit is
     * less than position or greater than the capacity of buf
     */
    long write(long fd, ByteBuffer buf, int position, int limit) {
        return backend.write(fd, buf, position, limit);
    }

    /**
     * Calls the "read" function defined in unistd.h. The parameters have
     * the same meaning as in the "read" C system call,
     * except that a ByteBuffer with a position and limit are used for the
     * data sink. The position and limit set on the ByteBuffer are ignored;
     * the position and limit provided as method parameters are used instead.
     * @param fd The file descriptor from which to read
     * @param buf The buffer to which to write; must be direct
     * @param position The index in buf to which to being reading data
     * @param limit The index in buf up to which to read data
     * @return The number of bytes read, or -1 on failure
     * @throws IllegalArgumentException if buf is not a direct buffer
     * @throws IndexOutOfBoundsException if position is negative, or limit is
     * less than position or greater than the capacity of buf
     */
    long read(long fd, ByteBuffer buf, int position, int limit) {
        return backend.read(fd, buf, position, limit);
    }

    static final int SEEK_SET = 0;

    /**
     * Calls the "sysconf" function defined in unistd.h
     * @param name The name of the POSIX variable to query
     * @return The value of the system resource, or -1 if name is invalid
     */
    long sysconf(int name) {
        try {
            return (long) Libc.SYSCONF.invokeExact(Libc.capture(), name);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    static final int _SC_LONG_BIT = 106;
    // input.h

    /** {@code struct input_absinfo} of linux/input.h: six {@code __s32}, no padding on any architecture. */
    static final StructLayout INPUT_ABSINFO = MemoryLayout.structLayout(
            JAVA_INT.withName("value"),
            JAVA_INT.withName("minimum"),
            JAVA_INT.withName("maximum"),
            JAVA_INT.withName("fuzz"),
            JAVA_INT.withName("flat"),
            JAVA_INT.withName("resolution"));

    /**
     * InputAbsInfo wraps the C structure input_absinfo, defined in
     * linux/input.h
     */
    static class InputAbsInfo extends C.Structure {

        private static final VarHandle VALUE = INPUT_ABSINFO.varHandle(groupElement("value"));
        private static final VarHandle MINIMUM = INPUT_ABSINFO.varHandle(groupElement("minimum"));
        private static final VarHandle MAXIMUM = INPUT_ABSINFO.varHandle(groupElement("maximum"));
        private static final VarHandle FUZZ = INPUT_ABSINFO.varHandle(groupElement("fuzz"));
        private static final VarHandle FLAT = INPUT_ABSINFO.varHandle(groupElement("flat"));
        private static final VarHandle RESOLUTION = INPUT_ABSINFO.varHandle(groupElement("resolution"));

        /**
         * @return the size of the C struct input_absinfo
         */
        @Override
        int sizeof() {
            return (int) INPUT_ABSINFO.byteSize();
        }

        private static int get(VarHandle field, long p) {
            return (int) field.get(struct(p, INPUT_ABSINFO), 0L);
        }

        /**
         * @param p a pointer to a C struct of type input_absinfo
         * @return the "value" field of the structure pointed to by p
         */
        static int getValue(long p) {
            return get(VALUE, p);
        }

        /**
         * @param p a pointer to a C struct of type input_absinfo
         * @return the "minimum" field of the structure pointed to by p
         */
        static int getMinimum(long p) {
            return get(MINIMUM, p);
        }

        /**
         * @param p a pointer to a C struct of type input_absinfo
         * @return the "maximum" field of the structure pointed to by p
         */
        static int getMaximum(long p) {
            return get(MAXIMUM, p);
        }

        /**
         * @param p a pointer to a C struct of type input_absinfo
         * @return the "fuzz" field of the structure pointed to by p
         */
        static int getFuzz(long p) {
            return get(FUZZ, p);
        }

        /**
         * @param p a pointer to a C struct of type input_absinfo
         * @return the "flat" field of the structure pointed to by p
         */
        static int getFlat(long p) {
            return get(FLAT, p);
        }

        /**
         * @param p a pointer to a C struct of type input_absinfo
         * @return the "resolution" field of the structure pointed to by p
         */
        static int getResolution(long p) {
            return get(RESOLUTION, p);
        }
    }

    // asm-generic/ioctl.h: _IOC(dir, type, nr, size) with 8 nr bits, 8 type bits, 14 size bits and 2 dir
    // bits, the encoding x86, x86-64, arm and arm64 share (their asm/ioctl.h include the generic one).
    private static final int IOC_NRSHIFT = 0;
    private static final int IOC_TYPESHIFT = 8;
    private static final int IOC_SIZESHIFT = 16;
    private static final int IOC_DIRSHIFT = 30;
    private static final int IOC_WRITE = 1;
    private static final int IOC_READ = 2;

    private static int ioc(int dir, int type, int nr, int size) {
        return (dir << IOC_DIRSHIFT) | (size << IOC_SIZESHIFT) | (type << IOC_TYPESHIFT) | (nr << IOC_NRSHIFT);
    }

    /** {@code EVIOCGABS(abs) = _IOR('E', 0x40 + (abs), struct input_absinfo)} of linux/input.h. */
    int EVIOCGABS(int type) {
        return ioc(IOC_READ, 'E', 0x40 + type, (int) INPUT_ABSINFO.byteSize());
    }

    // fb.h

    static final int FBIOGET_VSCREENINFO = 0x4600;
    static final int FBIOPUT_VSCREENINFO = 0x4601;
    static final int FBIOPAN_DISPLAY = 0x4606;
    static final int FBIOBLANK = 0x4611;

    static final int FB_BLANK_UNBLANK = 0;
    static final int FB_ACTIVATE_NOW = 0;
    static final int FB_ACTIVATE_VBL = 16;

    /** {@code struct fb_bitfield} of linux/fb.h: three {@code __u32}. */
    static final StructLayout FB_BITFIELD = MemoryLayout.structLayout(
            JAVA_INT.withName("offset"),
            JAVA_INT.withName("length"),
            JAVA_INT.withName("msb_right"));

    /** {@code struct fb_var_screeninfo} of linux/fb.h: 40 {@code __u32} in all, 160 bytes, no padding. */
    static final StructLayout FB_VAR_SCREENINFO = MemoryLayout.structLayout(
            JAVA_INT.withName("xres"),
            JAVA_INT.withName("yres"),
            JAVA_INT.withName("xres_virtual"),
            JAVA_INT.withName("yres_virtual"),
            JAVA_INT.withName("xoffset"),
            JAVA_INT.withName("yoffset"),
            JAVA_INT.withName("bits_per_pixel"),
            JAVA_INT.withName("grayscale"),
            FB_BITFIELD.withName("red"),
            FB_BITFIELD.withName("green"),
            FB_BITFIELD.withName("blue"),
            FB_BITFIELD.withName("transp"),
            JAVA_INT.withName("nonstd"),
            JAVA_INT.withName("activate"),
            JAVA_INT.withName("height"),
            JAVA_INT.withName("width"),
            JAVA_INT.withName("accel_flags"),
            JAVA_INT.withName("pixclock"),
            JAVA_INT.withName("left_margin"),
            JAVA_INT.withName("right_margin"),
            JAVA_INT.withName("upper_margin"),
            JAVA_INT.withName("lower_margin"),
            JAVA_INT.withName("hsync_len"),
            JAVA_INT.withName("vsync_len"),
            JAVA_INT.withName("sync"),
            JAVA_INT.withName("vmode"),
            JAVA_INT.withName("rotate"),
            JAVA_INT.withName("colorspace"),
            MemoryLayout.sequenceLayout(4, JAVA_INT).withName("reserved"));

    /**
     * FbVarScreenInfo wraps the C structure fb_var_screeninfo, defined in
     * linux/fb.h. The field handles are shared with the EPDSystem subclass,
     * which adds the accessors of the fields this class does not use.
     */
    static class FbVarScreenInfo extends C.Structure {

        static final VarHandle XRES = field("xres");
        static final VarHandle YRES = field("yres");
        static final VarHandle XRES_VIRTUAL = field("xres_virtual");
        static final VarHandle YRES_VIRTUAL = field("yres_virtual");
        static final VarHandle XOFFSET = field("xoffset");
        static final VarHandle YOFFSET = field("yoffset");
        static final VarHandle BITS_PER_PIXEL = field("bits_per_pixel");
        static final VarHandle GRAYSCALE = field("grayscale");
        static final VarHandle RED_OFFSET = field("red", "offset");
        static final VarHandle RED_LENGTH = field("red", "length");
        static final VarHandle RED_MSB_RIGHT = field("red", "msb_right");
        static final VarHandle GREEN_OFFSET = field("green", "offset");
        static final VarHandle GREEN_LENGTH = field("green", "length");
        static final VarHandle GREEN_MSB_RIGHT = field("green", "msb_right");
        static final VarHandle BLUE_OFFSET = field("blue", "offset");
        static final VarHandle BLUE_LENGTH = field("blue", "length");
        static final VarHandle BLUE_MSB_RIGHT = field("blue", "msb_right");
        static final VarHandle TRANSP_OFFSET = field("transp", "offset");
        static final VarHandle TRANSP_LENGTH = field("transp", "length");
        static final VarHandle TRANSP_MSB_RIGHT = field("transp", "msb_right");
        static final VarHandle NONSTD = field("nonstd");
        static final VarHandle ACTIVATE = field("activate");
        static final VarHandle HEIGHT = field("height");
        static final VarHandle WIDTH = field("width");
        static final VarHandle ACCEL_FLAGS = field("accel_flags");
        static final VarHandle PIXCLOCK = field("pixclock");
        static final VarHandle LEFT_MARGIN = field("left_margin");
        static final VarHandle RIGHT_MARGIN = field("right_margin");
        static final VarHandle UPPER_MARGIN = field("upper_margin");
        static final VarHandle LOWER_MARGIN = field("lower_margin");
        static final VarHandle HSYNC_LEN = field("hsync_len");
        static final VarHandle VSYNC_LEN = field("vsync_len");
        static final VarHandle SYNC = field("sync");
        static final VarHandle VMODE = field("vmode");
        static final VarHandle ROTATE = field("rotate");

        FbVarScreenInfo() {
        }

        /** The {@code __u32} field at {@code names}, a path into {@link #FB_VAR_SCREENINFO}. */
        static VarHandle field(String... names) {
            MemoryLayout.PathElement[] path = new MemoryLayout.PathElement[names.length];
            for (int i = 0; i < names.length; i++) {
                path[i] = groupElement(names[i]);
            }
            return FB_VAR_SCREENINFO.varHandle(path);
        }

        static int get(VarHandle field, long p) {
            return (int) field.get(struct(p, FB_VAR_SCREENINFO), 0L);
        }

        static void set(VarHandle field, long p, int value) {
            field.set(struct(p, FB_VAR_SCREENINFO), 0L, value);
        }

        @Override
        int sizeof() {
            return (int) FB_VAR_SCREENINFO.byteSize();
        }

        int getBitsPerPixel(long p) {
            return get(BITS_PER_PIXEL, p);
        }

        int getXRes(long p) {
            return get(XRES, p);
        }

        int getYRes(long p) {
            return get(YRES, p);
        }

        int getXResVirtual(long p) {
            return get(XRES_VIRTUAL, p);
        }

        int getYResVirtual(long p) {
            return get(YRES_VIRTUAL, p);
        }

        int getOffsetX(long p) {
            return get(XOFFSET, p);
        }

        int getOffsetY(long p) {
            return get(YOFFSET, p);
        }

        void setRes(long p, int x, int y) {
            set(XRES, p, x);
            set(YRES, p, y);
        }

        void setVirtualRes(long p, int x, int y) {
            set(XRES_VIRTUAL, p, x);
            set(YRES_VIRTUAL, p, y);
        }

        void setOffset(long p, int x, int y) {
            set(XOFFSET, p, x);
            set(YOFFSET, p, y);
        }

        void setActivate(long p, int activate) {
            set(ACTIVATE, p, activate);
        }

        void setBitsPerPixel(long p, int bpp) {
            set(BITS_PER_PIXEL, p, bpp);
        }

        /** Writes {@code red.length} then {@code red.offset}: the argument order is the one the C took. */
        void setRed(long p, int length, int offset) {
            set(RED_LENGTH, p, length);
            set(RED_OFFSET, p, offset);
        }

        void setGreen(long p, int length, int offset) {
            set(GREEN_LENGTH, p, length);
            set(GREEN_OFFSET, p, offset);
        }

        void setBlue(long p, int length, int offset) {
            set(BLUE_LENGTH, p, length);
            set(BLUE_OFFSET, p, offset);
        }

        void setTransp(long p, int length, int offset) {
            set(TRANSP_LENGTH, p, length);
            set(TRANSP_OFFSET, p, offset);
        }
    }

    // ioctl.h

    /**
     * {@code ioctl(fd, request, data)}: the request zero-extended to the {@code unsigned long} glibc declares
     * (the C's {@code (int)} cast sign-extended requests with {@code _IOC_READ} set; the kernel reads 32 bits
     * either way), the data as the pointer-sized third argument whether it is a struct address or a scalar such
     * as the 1 of {@code EVIOCGRAB}, exactly as the C's {@code asPtr(dataL)} passed it.
     */
    int ioctl(long fd, int request, long data) {
        return backend.ioctl(fd, request, data);
    }

    int IOW(int type, int number, int size) {
        return ioc(IOC_WRITE, type, number, size);
    }

    int IOR(int type, int number, int size) {
        return ioc(IOC_READ, type, number, size);
    }

    int IOWR(int type, int number, int size) {
        return ioc(IOC_READ | IOC_WRITE, type, number, size);
    }

    // stropts.h
    private static int __SID = ('S' << 8);
    static int I_FLUSH = __SID | 5;

    static int FLUSHRW = 0x03;

    // errno.h

    /** The {@code errno} the calling thread's last fallible system call through this class left. */
    int errno() {
        return backend.errno();
    }

    static final int ENXIO = 6;
    static final int EAGAIN = 11;

    // string.h

    /** {@code strerror}, decoded as UTF-8; libc runs in the C locale here, so the text is ASCII. */
    String strerror(int errnum) {
        return backend.strerror(errnum);
    }

    // dlfcn.h
    static final int RTLD_LAZY = 0x00001;
    static final int RTLD_GLOBAL = 0x00100;

    long dlopen(String filename, int flag) {
        try (Arena arena = Arena.ofConfined()) {
            return ((MemorySegment) Libc.DLOPEN.invokeExact(arena.allocateFrom(filename), flag)).address();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** The pending {@code dlerror} text, or null when there is none, as {@code NewStringUTF(NULL)} was null. */
    String dlerror() {
        try {
            return cString((MemorySegment) Libc.DLERROR.invokeExact());
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    long dlsym(long handle, String symbol) {
        try (Arena arena = Arena.ofConfined()) {
            return ((MemorySegment) Libc.DLSYM.invokeExact(MemorySegment.ofAddress(handle),
                    arena.allocateFrom(symbol))).address();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    int dlclose(long handle) {
        try {
            return (int) Libc.DLCLOSE.invokeExact(MemorySegment.ofAddress(handle));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    // mman.h
    static final long PROT_READ = 0x1l;
    static final long PROT_WRITE = 0x2l;
    static final long MAP_PRIVATE = 0x02l;
    static final long MAP_ANONYMOUS = 0x20l;
    static final long MAP_SHARED = 0x1l;

    /**
     * {@code (void *) -1} as the C's {@code asJLong} produced it on a 32-bit platform. On LP64 a failed mmap
     * returns -1L, which no caller compares against: kept as it was, a pre-existing bug for a separate fix.
     */
    static final long MAP_FAILED = 0xffffffffl;

    /**
     * {@code mmap}, with {@code prot}, {@code flags} and {@code fd} narrowed to the {@code int} the C cast
     * them to.
     *
     * @return the address of the mapping, or of {@code MAP_FAILED} as this platform defines it
     */
    long mmap(long addr, long length, long prot, long flags,
                            long fd, long offset) {
        return backend.mmap(addr, length, prot, flags, fd, offset);
    }

    int munmap(long addr, long length) {
        return backend.munmap(addr, length);
    }

    // string.h

    /**
     * {@code memcpy}, as {@code MemorySegment.copy} of {@code length} bytes: pure Java, so it binds nothing.
     * {@code MemorySegment.copy} also handles overlapping ranges, which {@code memcpy} left undefined.
     *
     * @return {@code destAddr}, as {@code memcpy} returns its destination
     */
    @SuppressWarnings("restricted")
    long memcpy(long destAddr, long srcAddr, long length) {
        MemorySegment.copy(MemorySegment.ofAddress(srcAddr).reinterpret(length), 0L,
                MemorySegment.ofAddress(destAddr).reinterpret(length), 0L, length);
        return destAddr;
    }

    /** Returns a string description of the last error reported by a system call
     * @return a String describing the error
     */
    String getErrorMessage() {
        return strerror(errno());
    }

    // stat.h
    static int S_IRWXU = 00700;

    int mkfifo(String pathname, int mode) {
        try (Arena arena = Arena.ofConfined()) {
            return (int) Libc.MKFIFO.invokeExact(Libc.capture(), arena.allocateFrom(pathname), mode);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    // sys/socket.h, linux/netlink.h

    /** {@code PF_NETLINK} of bits/socket.h; {@code AF_NETLINK} is the same value. */
    static final int PF_NETLINK = 16;
    static final int AF_NETLINK = PF_NETLINK;

    /** {@code SOCK_DGRAM} of bits/socket_type.h. */
    static final int SOCK_DGRAM = 2;

    /** {@code NETLINK_KOBJECT_UEVENT} of linux/netlink.h: the kernel and udev event socket protocol. */
    static final int NETLINK_KOBJECT_UEVENT = 15;

    /** {@code SOL_SOCKET} and {@code SO_RCVBUF} of asm-generic/socket.h. */
    static final int SOL_SOCKET = 1;
    static final int SO_RCVBUF = 8;

    int socket(int domain, int type, int protocol) {
        try {
            return (int) Libc.SOCKET.invokeExact(Libc.capture(), domain, type, protocol);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code setsockopt} of an {@code int} option: the C passed the address and size of its {@code int}. */
    int setsockopt(long fd, int level, int optname, int value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment option = arena.allocateFrom(JAVA_INT, value);
            return (int) Libc.SETSOCKOPT.invokeExact(Libc.capture(), (int) fd, level, optname, option,
                    (int) option.byteSize());
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code bind(fd, addr, addrlen)}.
     *
     * @param addr the address of a socket address structure, such as a {@link SockaddrNl}
     * @param addrlen its size in bytes
     */
    int bind(long fd, long addr, int addrlen) {
        try {
            return (int) Libc.BIND.invokeExact(Libc.capture(), (int) fd, MemorySegment.ofAddress(addr), addrlen);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code recv(fd, buf + position, limit - position, flags)} over a direct buffer, its own position and
     * limit ignored as in {@link #read}.
     *
     * @throws IllegalArgumentException if buf is not a direct buffer
     * @throws IndexOutOfBoundsException if position is negative, or limit is less than position or greater
     * than the capacity of buf
     */
    long recv(long fd, ByteBuffer buf, int position, int limit, int flags) {
        try {
            MemorySegment data = range(buf, position, limit);
            return (long) Libc.RECV.invokeExact(Libc.capture(), (int) fd, data, data.byteSize(), flags);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code struct sockaddr_nl} of linux/netlink.h: {@code __kernel_sa_family_t nl_family} (unsigned short),
     * {@code unsigned short nl_pad}, {@code __u32 nl_pid}, {@code __u32 nl_groups}; 12 bytes, no padding.
     */
    static final StructLayout SOCKADDR_NL = MemoryLayout.structLayout(
            JAVA_SHORT.withName("nl_family"),
            JAVA_SHORT.withName("nl_pad"),
            JAVA_INT.withName("nl_pid"),
            JAVA_INT.withName("nl_groups"));

    /** SockaddrNl wraps the C structure sockaddr_nl, defined in linux/netlink.h; a new one is all zeros. */
    static class SockaddrNl extends C.Structure {

        private static final VarHandle NL_FAMILY = SOCKADDR_NL.varHandle(groupElement("nl_family"));
        private static final VarHandle NL_PID = SOCKADDR_NL.varHandle(groupElement("nl_pid"));
        private static final VarHandle NL_GROUPS = SOCKADDR_NL.varHandle(groupElement("nl_groups"));

        @Override
        int sizeof() {
            return (int) SOCKADDR_NL.byteSize();
        }

        int getFamily(long p) {
            return Short.toUnsignedInt((short) NL_FAMILY.get(struct(p, SOCKADDR_NL), 0L));
        }

        int getPid(long p) {
            return (int) NL_PID.get(struct(p, SOCKADDR_NL), 0L);
        }

        int getGroups(long p) {
            return (int) NL_GROUPS.get(struct(p, SOCKADDR_NL), 0L);
        }

        void setFamily(long p, int family) {
            NL_FAMILY.set(struct(p, SOCKADDR_NL), 0L, (short) family);
        }

        void setPid(long p, int pid) {
            NL_PID.set(struct(p, SOCKADDR_NL), 0L, pid);
        }

        void setGroups(long p, int groups) {
            NL_GROUPS.set(struct(p, SOCKADDR_NL), 0L, groups);
        }
    }

}

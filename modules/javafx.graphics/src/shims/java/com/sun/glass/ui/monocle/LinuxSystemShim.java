/*
 * Copyright (c) 2016, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.StructLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes {@link LinuxSystem} and {@link C} to tests. The first group of methods is the one the system tests
 * have always used and keeps its argument order; {@link #setenvExact}, {@link #writeExact} and
 * {@link #ioctlExact} delegate argument for argument and are what the binding tests call. {@link FakeBackend}
 * stands in for the system calls of the frame buffer classes on any platform.
 */
public class LinuxSystemShim {

    public static final int O_RDONLY = LinuxSystem.O_RDONLY;
    public static final int O_WRONLY = LinuxSystem.O_WRONLY;
    public static final int O_RDWR = LinuxSystem.O_RDWR;
    public static final int O_NONBLOCK = LinuxSystem.O_NONBLOCK;
    public static final int _SC_LONG_BIT = LinuxSystem._SC_LONG_BIT;
    public static final int FBIOGET_VSCREENINFO = LinuxSystem.FBIOGET_VSCREENINFO;
    public static final int FBIOPUT_VSCREENINFO = LinuxSystem.FBIOPUT_VSCREENINFO;
    public static final int FBIOPAN_DISPLAY = LinuxSystem.FBIOPAN_DISPLAY;
    public static final int FBIOBLANK = LinuxSystem.FBIOBLANK;
    public static final int FB_BLANK_UNBLANK = LinuxSystem.FB_BLANK_UNBLANK;
    public static final int FB_ACTIVATE_NOW = LinuxSystem.FB_ACTIVATE_NOW;
    public static final int FB_ACTIVATE_VBL = LinuxSystem.FB_ACTIVATE_VBL;
    public static final int I_FLUSH = LinuxSystem.I_FLUSH;
    public static final int FLUSHRW = LinuxSystem.FLUSHRW;
    public static final int ENXIO = LinuxSystem.ENXIO;
    public static final int EAGAIN = LinuxSystem.EAGAIN;
    public static final int RTLD_LAZY = LinuxSystem.RTLD_LAZY;
    public static final int RTLD_GLOBAL = LinuxSystem.RTLD_GLOBAL;
    public static final int S_IRWXU = LinuxSystem.S_IRWXU;
    public static final int SEEK_SET = LinuxSystem.SEEK_SET;
    public static final int PF_NETLINK = LinuxSystem.PF_NETLINK;
    public static final int AF_NETLINK = LinuxSystem.AF_NETLINK;
    public static final int SOCK_DGRAM = LinuxSystem.SOCK_DGRAM;
    public static final int NETLINK_KOBJECT_UEVENT = LinuxSystem.NETLINK_KOBJECT_UEVENT;
    public static final int SOL_SOCKET = LinuxSystem.SOL_SOCKET;
    public static final int SO_RCVBUF = LinuxSystem.SO_RCVBUF;

    public static final long PROT_READ = LinuxSystem.PROT_READ;
    public static final long PROT_WRITE = LinuxSystem.PROT_WRITE;
    public static final long MAP_PRIVATE = LinuxSystem.MAP_PRIVATE;
    public static final long MAP_ANONYMOUS = LinuxSystem.MAP_ANONYMOUS;
    public static final long MAP_SHARED = LinuxSystem.MAP_SHARED;
    public static final long MAP_FAILED = LinuxSystem.MAP_FAILED;

    public static void loadLibrary() {
        LinuxSystem.getLinuxSystem().loadLibrary();
    }

    public static void setenv(String key, String value, boolean overwrite) {
        LinuxSystem.getLinuxSystem().setenv(value, value, overwrite);
    }

    public static String getErrorMessage() {
        return LinuxSystem.getLinuxSystem().getErrorMessage();
    }

    public static int mkfifo(String pathname, int mode) {
        return LinuxSystem.getLinuxSystem().mkfifo(pathname, mode);
    }

    public static long write(long fd, ByteBuffer buf, int position, int limit) {
        return LinuxSystem.getLinuxSystem().write(fd, buf, limit, limit);
    }

    public static int close(long fd) {
        return LinuxSystem.getLinuxSystem().close(fd);
    }

    public static long open(String path, int flags) {
        return LinuxSystem.getLinuxSystem().open(path, flags);
    }

    public static int errno() {
        return LinuxSystem.getLinuxSystem().errno();
    }

    public static int ioctl(long fd, int request, long data) {
        return LinuxSystem.getLinuxSystem().ioctl(fd, request, fd);
    }

    // Exact delegates for the binding tests

    public static boolean isLibraryLoaded() {
        return LinuxSystem.isLibraryLoaded();
    }

    /** {@code libc!symbol} of every symbol LinuxSystem bound, in binding order. */
    public static List<String> boundSymbols() {
        return LinuxSystem.boundSymbols();
    }

    public static long boundAddress(String qualifiedName) {
        return LinuxSystem.boundAddress(qualifiedName);
    }

    /** The LP64 check LinuxSystem applies to the canonical {@code long} of the linker, for any byte size. */
    public static void requireLp64(long longByteSize) {
        LinuxSystem.requireLp64(longByteSize);
    }

    public static StructLayout inputAbsInfoLayout() {
        return LinuxSystem.INPUT_ABSINFO;
    }

    public static StructLayout fbBitfieldLayout() {
        return LinuxSystem.FB_BITFIELD;
    }

    public static StructLayout fbVarScreenInfoLayout() {
        return LinuxSystem.FB_VAR_SCREENINFO;
    }

    public static StructLayout sockaddrNlLayout() {
        return LinuxSystem.SOCKADDR_NL;
    }

    public static int IOR(int type, int number, int size) {
        return LinuxSystem.getLinuxSystem().IOR(type, number, size);
    }

    public static int IOW(int type, int number, int size) {
        return LinuxSystem.getLinuxSystem().IOW(type, number, size);
    }

    public static int IOWR(int type, int number, int size) {
        return LinuxSystem.getLinuxSystem().IOWR(type, number, size);
    }

    public static int EVIOCGABS(int type) {
        return LinuxSystem.getLinuxSystem().EVIOCGABS(type);
    }

    public static void setenvExact(String key, String value, boolean overwrite) {
        LinuxSystem.getLinuxSystem().setenv(key, value, overwrite);
    }

    public static long writeExact(long fd, ByteBuffer buf, int position, int limit) {
        return LinuxSystem.getLinuxSystem().write(fd, buf, position, limit);
    }

    public static long read(long fd, ByteBuffer buf, int position, int limit) {
        return LinuxSystem.getLinuxSystem().read(fd, buf, position, limit);
    }

    public static int ioctlExact(long fd, int request, long data) {
        return LinuxSystem.getLinuxSystem().ioctl(fd, request, data);
    }

    public static long lseek(long fd, long offset, int whence) {
        return LinuxSystem.getLinuxSystem().lseek(fd, offset, whence);
    }

    public static long sysconf(int name) {
        return LinuxSystem.getLinuxSystem().sysconf(name);
    }

    public static String strerror(int errnum) {
        return LinuxSystem.getLinuxSystem().strerror(errnum);
    }

    public static long dlopen(String filename, int flag) {
        return LinuxSystem.getLinuxSystem().dlopen(filename, flag);
    }

    public static String dlerror() {
        return LinuxSystem.getLinuxSystem().dlerror();
    }

    public static long dlsym(long handle, String symbol) {
        return LinuxSystem.getLinuxSystem().dlsym(handle, symbol);
    }

    public static int dlclose(long handle) {
        return LinuxSystem.getLinuxSystem().dlclose(handle);
    }

    public static long mmap(long addr, long length, long prot, long flags, long fd, long offset) {
        return LinuxSystem.getLinuxSystem().mmap(addr, length, prot, flags, fd, offset);
    }

    public static int munmap(long addr, long length) {
        return LinuxSystem.getLinuxSystem().munmap(addr, length);
    }

    public static long memcpy(long destAddr, long srcAddr, long length) {
        return LinuxSystem.getLinuxSystem().memcpy(destAddr, srcAddr, length);
    }

    public static int socket(int domain, int type, int protocol) {
        return LinuxSystem.getLinuxSystem().socket(domain, type, protocol);
    }

    public static int setsockopt(long fd, int level, int optname, int value) {
        return LinuxSystem.getLinuxSystem().setsockopt(fd, level, optname, value);
    }

    public static int bind(long fd, long addr, int addrlen) {
        return LinuxSystem.getLinuxSystem().bind(fd, addr, addrlen);
    }

    public static long recv(long fd, ByteBuffer buf, int position, int limit, int flags) {
        return LinuxSystem.getLinuxSystem().recv(fd, buf, position, limit, flags);
    }

    public static ByteBuffer newDirectByteBuffer(long ptr, int size) {
        return C.getC().NewDirectByteBuffer(ptr, size);
    }

    public static long getDirectBufferAddress(ByteBuffer b) {
        return C.getC().GetDirectBufferAddress(b);
    }

    /** A {@code LinuxSystem.FbVarScreenInfo} and the accessors LinuxSystem gives it, over its own address. */
    public static final class FbVarScreenInfoShim {

        private final LinuxSystem.FbVarScreenInfo struct = new LinuxSystem.FbVarScreenInfo();

        public long address() {
            return struct.p;
        }

        /** The direct buffer over the struct, in the byte order ByteBuffer.allocateDirect gave it. */
        public ByteBuffer buffer() {
            return struct.b;
        }

        public int sizeof() {
            return struct.sizeof();
        }

        public int getBitsPerPixel() {
            return struct.getBitsPerPixel(struct.p);
        }

        public int getXRes() {
            return struct.getXRes(struct.p);
        }

        public int getYRes() {
            return struct.getYRes(struct.p);
        }

        public int getXResVirtual() {
            return struct.getXResVirtual(struct.p);
        }

        public int getYResVirtual() {
            return struct.getYResVirtual(struct.p);
        }

        public int getOffsetX() {
            return struct.getOffsetX(struct.p);
        }

        public int getOffsetY() {
            return struct.getOffsetY(struct.p);
        }

        public void setRes(int x, int y) {
            struct.setRes(struct.p, x, y);
        }

        public void setVirtualRes(int x, int y) {
            struct.setVirtualRes(struct.p, x, y);
        }

        public void setOffset(int x, int y) {
            struct.setOffset(struct.p, x, y);
        }

        public void setActivate(int activate) {
            struct.setActivate(struct.p, activate);
        }

        public void setBitsPerPixel(int bpp) {
            struct.setBitsPerPixel(struct.p, bpp);
        }

        public void setRed(int length, int offset) {
            struct.setRed(struct.p, length, offset);
        }

        public void setGreen(int length, int offset) {
            struct.setGreen(struct.p, length, offset);
        }

        public void setBlue(int length, int offset) {
            struct.setBlue(struct.p, length, offset);
        }

        public void setTransp(int length, int offset) {
            struct.setTransp(struct.p, length, offset);
        }
    }

    /** A {@code LinuxSystem.InputAbsInfo} and the getters LinuxSystem gives it, over its own address. */
    public static final class InputAbsInfoShim {

        private final LinuxSystem.InputAbsInfo struct = new LinuxSystem.InputAbsInfo();

        public long address() {
            return struct.p;
        }

        /** The direct buffer over the struct, in the byte order ByteBuffer.allocateDirect gave it. */
        public ByteBuffer buffer() {
            return struct.b;
        }

        public int sizeof() {
            return struct.sizeof();
        }

        public int getValue() {
            return LinuxSystem.InputAbsInfo.getValue(struct.p);
        }

        public int getMinimum() {
            return LinuxSystem.InputAbsInfo.getMinimum(struct.p);
        }

        public int getMaximum() {
            return LinuxSystem.InputAbsInfo.getMaximum(struct.p);
        }

        public int getFuzz() {
            return LinuxSystem.InputAbsInfo.getFuzz(struct.p);
        }

        public int getFlat() {
            return LinuxSystem.InputAbsInfo.getFlat(struct.p);
        }

        public int getResolution() {
            return LinuxSystem.InputAbsInfo.getResolution(struct.p);
        }
    }

    /** A {@code LinuxSystem.SockaddrNl} and its accessors, over its own address. */
    public static final class SockaddrNlShim {

        private final LinuxSystem.SockaddrNl struct = new LinuxSystem.SockaddrNl();

        public long address() {
            return struct.p;
        }

        public ByteBuffer buffer() {
            return struct.b;
        }

        public int sizeof() {
            return struct.sizeof();
        }

        public int getFamily() {
            return struct.getFamily(struct.p);
        }

        public int getPid() {
            return struct.getPid(struct.p);
        }

        public int getGroups() {
            return struct.getGroups(struct.p);
        }

        public void setFamily(int family) {
            struct.setFamily(struct.p, family);
        }

        public void setPid(int pid) {
            struct.setPid(struct.p, pid);
        }

        public void setGroups(int groups) {
            struct.setGroups(struct.p, groups);
        }
    }

    /** Installs {@code backend} as the system calls of the frame buffer classes until {@link #restoreLibcBackend}. */
    public static void installBackendForTesting(FakeBackend backend) {
        LinuxSystem.setBackendForTesting(backend);
    }

    public static void restoreLibcBackend() {
        LinuxSystem.setBackendForTesting(null);
    }

    /**
     * A stand-in for the libc system calls of the frame buffer classes, usable on any platform. It records the
     * paths opened, the descriptors closed, every ioctl request with a snapshot of the bytes it was given, and
     * every mapping; it answers {@code FBIOGET_VSCREENINFO} by writing its canned {@code fb_var_screeninfo},
     * keeps what {@code FBIOPUT_VSCREENINFO} writes as the new canned one, and answers every other request
     * with the result configured for it, 0 by default.
     * <p>
     * The snapshot of an ioctl argument is as long as the size field of its request code, or 160 bytes for the
     * size-less {@code FBIO*} requests of linux/fb.h, and is taken only when the argument looks like a pointer
     * (at or above 64 KiB): the scalars callers pass for {@code EVIOCGRAB} or {@code FBIO_WAITFORVSYNC} must
     * not be dereferenced.
     */
    public static final class FakeBackend implements LinuxSystem.Backend {

        private static final int FB_VAR_SCREENINFO_BYTES = 160;
        private static final long LOWEST_PLAUSIBLE_POINTER = 65536L;
        private static final int IOC_SIZE_SHIFT = 16;
        private static final int IOC_SIZE_MASK = 0x3fff;

        private final List<String> openedPaths = new ArrayList<>();
        private final List<Long> closedDescriptors = new ArrayList<>();
        private final List<Integer> requests = new ArrayList<>();
        private final List<byte[]> arguments = new ArrayList<>();
        private final List<long[]> mappings = new ArrayList<>();
        private final List<long[]> unmappings = new ArrayList<>();
        private final List<ByteBuffer> mappedMemory = new ArrayList<>();
        private final Map<Integer, Integer> results = new HashMap<>();
        private final byte[] screenInfo = new byte[FB_VAR_SCREENINFO_BYTES];
        private int errno;
        private int nextDescriptor = 3;
        private boolean openFails;

        /** Sets the geometry and depth of the canned {@code fb_var_screeninfo}. */
        public FakeBackend screenInfo(int xres, int yres, int xresVirtual, int yresVirtual, int bitsPerPixel) {
            ByteBuffer info = ByteBuffer.wrap(screenInfo).order(ByteOrder.nativeOrder());
            info.putInt(0, xres);
            info.putInt(4, yres);
            info.putInt(8, xresVirtual);
            info.putInt(12, yresVirtual);
            info.putInt(24, bitsPerPixel);
            return this;
        }

        /** The result every ioctl with {@code request} answers, instead of 0. */
        public FakeBackend result(int request, int rc) {
            results.put(request, rc);
            return this;
        }

        /** What {@code errno()} answers. */
        public FakeBackend errno(int value) {
            errno = value;
            return this;
        }

        /** Makes {@code open} answer -1. */
        public FakeBackend openFails() {
            openFails = true;
            return this;
        }

        public List<String> openedPaths() {
            return Collections.unmodifiableList(openedPaths);
        }

        public List<Long> closedDescriptors() {
            return Collections.unmodifiableList(closedDescriptors);
        }

        /** The ioctl request codes, in call order. */
        public List<Integer> requests() {
            return Collections.unmodifiableList(requests);
        }

        /** The bytes the ioctl at {@code index} was given (after this fake wrote to them, for a get). */
        public byte[] argument(int index) {
            return arguments.get(index).clone();
        }

        /** {@link #argument} as host-order ints. */
        public int[] argumentInts(int index) {
            byte[] bytes = arguments.get(index);
            int[] ints = new int[bytes.length / Integer.BYTES];
            ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asIntBuffer().get(ints);
            return ints;
        }

        /** Every mmap as {@code {address, length, prot, flags, fd, offset}}, in call order. */
        public List<long[]> mappings() {
            return Collections.unmodifiableList(mappings);
        }

        /** Every munmap as {@code {address, length}}, in call order. */
        public List<long[]> unmappings() {
            return Collections.unmodifiableList(unmappings);
        }

        /** The canned {@code fb_var_screeninfo} as it stands. */
        public byte[] screenInfoBytes() {
            return screenInfo.clone();
        }

        @Override
        public long open(String path, int flags) {
            openedPaths.add(path);
            return openFails ? -1L : nextDescriptor++;
        }

        @Override
        public int close(long fd) {
            closedDescriptors.add(fd);
            return 0;
        }

        @Override
        public long read(long fd, ByteBuffer buf, int position, int limit) {
            return 0L;
        }

        @Override
        public long write(long fd, ByteBuffer buf, int position, int limit) {
            return limit - position;
        }

        @Override
        public int ioctl(long fd, int request, long data) {
            requests.add(request);
            int size = legacyFramebufferRequest(request)
                    ? FB_VAR_SCREENINFO_BYTES : (request >>> IOC_SIZE_SHIFT) & IOC_SIZE_MASK;
            byte[] snapshot = new byte[0];
            if (size > 0 && data >= LOWEST_PLAUSIBLE_POINTER) {
                ByteBuffer memory = C.getC().NewDirectByteBuffer(data, size);
                if (request == LinuxSystem.FBIOGET_VSCREENINFO) {
                    memory.put(0, screenInfo);
                }
                snapshot = new byte[size];
                memory.get(0, snapshot);
                if (request == LinuxSystem.FBIOPUT_VSCREENINFO) {
                    System.arraycopy(snapshot, 0, screenInfo, 0, FB_VAR_SCREENINFO_BYTES);
                }
            }
            arguments.add(snapshot);
            return results.getOrDefault(request, 0);
        }

        private static boolean legacyFramebufferRequest(int request) {
            return request == LinuxSystem.FBIOGET_VSCREENINFO
                    || request == LinuxSystem.FBIOPUT_VSCREENINFO
                    || request == LinuxSystem.FBIOPAN_DISPLAY;
        }

        @Override
        public long mmap(long addr, long length, long prot, long flags, long fd, long offset) {
            ByteBuffer memory = ByteBuffer.allocateDirect((int) length);
            mappedMemory.add(memory);
            long address = C.getC().GetDirectBufferAddress(memory);
            mappings.add(new long[] {address, length, prot, flags, fd, offset});
            return address;
        }

        @Override
        public int munmap(long addr, long length) {
            unmappings.add(new long[] {addr, length});
            return 0;
        }

        @Override
        public int errno() {
            return errno;
        }

        @Override
        public String strerror(int errnum) {
            return "fake error " + errnum;
        }
    }

}

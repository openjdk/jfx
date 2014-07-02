/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.utils.NativeLibLoader;

import java.nio.ByteBuffer;
import java.security.Permission;

class LinuxSystem {
    private static Permission permission = new RuntimePermission("loadLibrary.*");

    private static LinuxSystem instance = new LinuxSystem();

    static LinuxSystem getLinuxSystem() {
        checkPermissions();
        return instance;
    }

    private static void checkPermissions() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(permission);
        }
    }

    private LinuxSystem() {
    }

    void loadLibrary() {
        NativeLibLoader.loadLibrary("glass_monocle");
    }

    // stdlib.h
    native void setenv(String key, String value, boolean overwrite);

    // fcntl.h

    static final int O_RDONLY = 0;
    static final int O_WRONLY = 1;
    static final int O_RDWR = 2;
    static final int O_NONBLOCK = 00004000;

    native long open(String path, int flags);

    // unistd.h
    native int close(long fd);
    native long lseek(long fd, long offset, int whence);
    native long write(long fd, ByteBuffer buf, int position, int limit);
    native long read(long fd, ByteBuffer buf, int position, int limit);

    static final int SEEK_SET = 0;

    // input.h

    static class InputAbsInfo extends C.Structure {
        @Override
        native int sizeof();
        static native int getValue(long p);
        static native int getMinimum(long p);
        static native int getMaximum(long p);
        static native int getFuzz(long p);
        static native int getFlat(long p);
        static native int getResolution(long p);
    }

    native int EVIOCGABS(int type);

    // fb.h

    static final int FBIOGET_VSCREENINFO = 0x4600;
    static final int FBIOPUT_VSCREENINFO = 0x4601;
    static final int FBIOPAN_DISPLAY = 0x4606;
    static final int FBIOBLANK = 0x4611;

    static final int FB_BLANK_UNBLANK = 0;
    static final int FB_ACTIVATE_NOW = 0;
    static final int FB_ACTIVATE_VBL = 16;

    static class FbVarScreenInfo extends C.Structure {
        FbVarScreenInfo() {
            checkPermissions();
        }
        @Override
        native int sizeof();
        native int getBitsPerPixel(long p);
        native int getXRes(long p);
        native int getYRes(long p);
        native int getXResVirtual(long p);
        native int getYResVirtual(long p);
        native int getOffsetX(long p);
        native int getOffsetY(long p);
        native void setRes(long p, int x, int y);
        native void setVirtualRes(long p, int x, int y);
        native void setOffset(long p, int x, int y);
        native void setActivate(long p, int activate);
        native void setBitsPerPixel(long p, int bpp);
        native void setRed(long p, int length, int offset);
        native void setGreen(long p, int length, int offset);
        native void setBlue(long p, int length, int offset);
        native void setTransp(long p, int length, int offset);
    }

    // ioctl.h

    native int ioctl(long fd, int request, long data);
    native int IOW(int type, int number, int size);
    native int IOR(int type, int number, int size);
    native int IOWR(int type, int number, int size);

    // stropts.h
    private static int __SID = ('S' << 8);
    static int I_FLUSH = __SID | 5;

    static int FLUSHRW = 0x03;

    // errno.h
    native int errno();

    static final int ENXIO = 6;
    static final int EAGAIN = 11;

    // string.h
    native String strerror(int errnum);

    // dlfcn.h
    static final int RTLD_LAZY = 0x00001;
    static final int RTLD_GLOBAL = 0x00100;
    native long dlopen(String filename, int flag);
    native String dlerror();
    native long dlsym(long handle, String symbol);
    native int dlclose(long handle);

    // mman.h
    static final long PROT_READ = 0x1l;
    static final long PROT_WRITE = 0x2l;
    static final long MAP_PRIVATE = 0x02l;
    static final long MAP_ANONYMOUS = 0x20l;
    static final long MAP_SHARED = 0x1l;
    static final long MAP_FAILED = 0xffffffffl;
    native long mmap(long addr, long length, long prot, long flags,
                            long fd, long offset);
    native int munmap(long addr, long length);

    // string.h
    native long memcpy(long destAddr, long srcAddr, long length);

    String getErrorMessage() {
        return strerror(errno());
    }

    // stat.h
    static int S_IRWXU = 00700;

    native int mkfifo(String pathname, int mode);

}

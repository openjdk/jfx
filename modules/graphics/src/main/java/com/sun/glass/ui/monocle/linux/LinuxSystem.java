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

package com.sun.glass.ui.monocle.linux;

import com.sun.glass.ui.monocle.util.C;

import java.nio.ByteBuffer;
import java.security.Permission;

public class LinuxSystem {
    private static Permission permission = new RuntimePermission("loadLibrary.*");

    private static LinuxSystem instance = new LinuxSystem();

    public static LinuxSystem getLinuxSystem() {
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


    // fcntl.h

    public static final int O_RDONLY = 0;
    public static final int O_WRONLY = 1;
    public static final int O_RDWR = 2;

    public native long open(String path, int flags);

    // unistd.h
    public native int close(long fd);
    public native long lseek(long fd, long offset, int whence);
    public native long write(long fd, ByteBuffer buf);

    public static final int SEEK_SET = 0;

    // input.h

    static class InputAbsInfo extends C.Structure {
        @Override
        public native int sizeof();
        static native int getValue(long p);
        static native int getMinimum(long p);
        static native int getMaximum(long p);
        static native int getFuzz(long p);
        static native int getFlat(long p);
        static native int getResolution(long p);
    }

    native int EVIOCGABS(int type);

    // fb.h

    public static final int FBIOGET_VSCREENINFO = 0x4600;
    public static final int FBIOPUT_VSCREENINFO = 0x4601;
    public static final int FBIOBLANK = 0x4611;

    public static final int FB_BLANK_UNBLANK = 0;

    public static class FbVarScreenInfo extends C.Structure {
        public FbVarScreenInfo() {
            checkPermissions();
        }
        @Override
        public native int sizeof();
        public native int getXRes(long p);
        public native int getYRes(long p);
        public native void setRes(long p, int x, int y);
        public native void setVirtualRes(long p, int x, int y);
        public native void setOffset(long p, int x, int y);
        public native void setActivate(long p, int activate);
        public native void setBitsPerPixel(long p, int bpp);
        public native void setRed(long p, int length, int offset);
        public native void setGreen(long p, int length, int offset);
        public native void setBlue(long p, int length, int offset);
        public native void setTransp(long p, int length, int offset);
    }

    // ioctl.h

    public native int ioctl(long fd, int request, long data);
    public native int IOW(int type, int number, int size);
    public native int IOR(int type, int number, int size);
    public native int IOWR(int type, int number, int size);

    // errno.h
    public native int errno();

    // string.h
    public native String strerror(int errnum);

    // dlfcn.h
    public native long dlopen(String filename, int flag);
    public native String dlerror();
    public native long dlsym(long handle, String symbol);
    public native int dlclose(long handle);
}

/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

public class LinuxSystem {

    private static LinuxSystem instance = new LinuxSystem();

    public static LinuxSystem getLinuxSystem() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("loadLibrary.*"));
        }
        return instance;
    }

    private LinuxSystem() {
    }


    // fcntl.h

    static final int O_RDONLY = 0;

    native long open(String path, int flags);

    // unistd.h
    native int close(long fd);

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

    static final int FBIOGET_VSCREENINFO = 0x4600;

    static class FbVarScreenInfo extends C.Structure {
        @Override
        public native int sizeof();
        static native int getXRes(long p);
        static native int getYRes(long p);
    }

    // ioctl.h

    native int ioctl(long fd, int request, long data);

    // errno.h
    native int errno();

    // string.h
    native String strerror(int errnum);

    // dlfcn.h
    public native long dlopen(String filename, int flag);
    public native String dlerror();
    public native long dlsym(long handle, String symbol);
    public native int dlclose(long handle);
}

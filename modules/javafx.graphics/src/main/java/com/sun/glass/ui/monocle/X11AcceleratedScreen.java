/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;

/** Provide X11 implementation of AcceleratedScreen
 *
 */
class X11AcceleratedScreen extends AcceleratedScreen {
    private static X xLib = X.getX();
    private X.XDisplay nativeDisplay;

    X11AcceleratedScreen(int[] attributes) throws GLException {
        super(attributes);
    }

    @Override
    protected long platformGetNativeDisplay() {
        /* There is a bug in the graphics drivers for the mali chip on the
         * ODROID-U3.  EGL improperly treats the XDisplay pointer as a number
         * and rejects negative values.  A pointer whose address has the high-bit
         * set will be interpreted as a negative number and rejected.  The
         * (slightly distasteful) workaround is to use mmap to allocate a chunk
         * of memory in a location whose address does NOT have the high bit set
         * and copy the XDisplay pointer there before passing it into EGL.
         * This workaround can be removed when the bug in the drivers is fixed.
         */
        if (nativeDisplay == null) {
            @SuppressWarnings("removal")
            boolean doMaliWorkaround =
                    AccessController.doPrivileged(
                            (PrivilegedAction<Boolean>) () ->
                                    Boolean.getBoolean(
                                            "monocle.maliSignedStruct"));
            X.XDisplay display = new X.XDisplay(xLib.XOpenDisplay(null));
            if (doMaliWorkaround) {
                long address = 0x7000000;
                nativeDisplay = new X.XDisplay(
                        ls.mmap(address, display.sizeof(),
                                LinuxSystem.PROT_READ | LinuxSystem.PROT_WRITE,
                                LinuxSystem.MAP_PRIVATE
                                        | LinuxSystem.MAP_ANONYMOUS,
                                -1, 0)
                );
                ls.memcpy(nativeDisplay.p, display.p, display.sizeof());
            } else {
                nativeDisplay = display;
            }
        }
        return nativeDisplay.p;
    }

    @Override
    protected long platformGetNativeWindow() {
        return NativePlatformFactory.getNativePlatform()
                .getScreen().getNativeHandle();
    }

    @Override
    public boolean swapBuffers() {
        /* Since we are accessing X from multiple threads, we need to lock the
         * display before we swap and potentially move the cursor.
         */
        xLib.XLockDisplay(nativeDisplay.p);
        super.swapBuffers();
        NativeCursor cursor = NativePlatformFactory.getNativePlatform().getCursor();
        if (cursor instanceof X11WarpingCursor) {
            ((X11WarpingCursor) cursor).warp();
        }
        xLib.XUnlockDisplay(nativeDisplay.p);
        return true;
    }
}

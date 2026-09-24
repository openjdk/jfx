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

package com.sun.glass.ui.monocle;

import com.sun.glass.ui.Size;
import java.lang.foreign.FunctionDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exposes {@link EglVendorNative}, its contract and the EGL platform pieces it serves to tests. No call of
 * the vendor library is made from here: the stub library of EglVendorNativeTest reports what it saw through
 * a file, not through a symbol a shim would have to bind.
 */
public final class EglVendorShim {

    public static final String LIBRARY_PROPERTY = EglVendorNative.LIBRARY_PROPERTY;
    public static final String DEFAULT_DISPLAY_ID = EGLAcceleratedScreen.DEFAULT_DISPLAY_ID;
    public static final int CURSOR_WIDTH = EGLCursor.CURSOR_WIDTH;
    public static final int CURSOR_HEIGHT = EGLCursor.CURSOR_HEIGHT;
    public static final int EGL_OPENGL_ES_API = EGL.EGL_OPENGL_ES_API;

    private EglVendorShim() {
    }

    public static Map<String, FunctionDescriptor> contract() {
        return EglVendorNative.CONTRACT;
    }

    public static boolean truth(byte value) {
        return EglVendorNative.truth(value);
    }

    public static void loadLibrary() {
        EglVendorNative.loadLibrary();
    }

    public static boolean isLibraryLoaded() {
        return EglVendorNative.isLibraryLoaded();
    }

    public static List<String> boundSymbols() {
        return EglVendorNative.boundSymbols();
    }

    /** The lock {@code swapBuffers} holds around {@code doEglSwapBuffers}. */
    public static Object framebufferSwapLock() {
        return NativeScreen.framebufferSwapLock;
    }

    /** {@code platform.createScreens()}, each screen as one line of its nine properties. */
    public static List<String> describeScreens(NativePlatform platform) {
        List<String> lines = new ArrayList<>();
        for (NativeScreen screen : platform.createScreens()) {
            lines.add("handle=" + screen.getNativeHandle() + " depth=" + screen.getDepth()
                    + " format=" + screen.getNativeFormat() + " width=" + screen.getWidth()
                    + " height=" + screen.getHeight() + " offsetX=" + screen.getOffsetX()
                    + " offsetY=" + screen.getOffsetY() + " dpi=" + screen.getDPI()
                    + " scale=" + screen.getScale());
        }
        return lines;
    }

    /** An {@link EGLCursor} and the package-private calls Glass makes on it. */
    public static final class Cursor {

        private final EGLCursor cursor = new EGLCursor();

        public int[] bestSize() {
            Size size = cursor.getBestSize();
            return new int[] {size.width, size.height};
        }

        public void setVisibility(boolean visibility) {
            cursor.setVisibility(visibility);
        }

        public void setLocation(int x, int y) {
            cursor.setLocation(x, y);
        }

        public void setImage(byte[] image) {
            cursor.setImage(image);
        }
    }
}

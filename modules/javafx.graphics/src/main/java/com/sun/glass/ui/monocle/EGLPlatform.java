/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

public class EGLPlatform extends LinuxPlatform {

    private List<NativeScreen> screens;

    /**
     * Create an <code>EGLPlatform</code>. The library with the platform-specific code, named by the system
     * property <code>monocle.egl.lib</code>, is loaded and its functions are bound now, by
     * {@link EglVendorNative}, where EGLPlatform of commit 21d5a654f6 opened it and left the binding to the
     * JNI forwarders of eglBridge.c.
     *
     * @throws UnsatisfiedLinkError if the library cannot be loaded or does not export every function of
     *         monocle_egl_ext.h
     */
    public EGLPlatform() {
        EglVendorNative.loadLibrary();
    }

    @Override
    protected NativeCursor createCursor() {
        // By default, hardware cursor will be used
        // Fallback to software cursor will be used in case monocle.egl.swcursor is set to true
        boolean swcursor = Boolean.getBoolean("monocle.egl.swcursor");
        final NativeCursor c = useCursor ? (swcursor ? new SoftwareCursor() : new EGLCursor()) : new NullCursor();
        return logSelectedCursor(c);
    }


    @Override
    protected NativeScreen createScreen() {
        return new EGLScreen(0);
    }

    @Override
    protected synchronized List<NativeScreen> createScreens() {
        if (screens == null) {
            int numScreens = EglVendorNative.doGetNumberOfScreens();
            screens = new ArrayList<>(numScreens);
            for (int i = 0; i < numScreens; i++) {
                screens.add(new EGLScreen(i));
            }
        }
        return screens;
    }

    @Override
    public synchronized AcceleratedScreen getAcceleratedScreen(int[] attributes) throws GLException {
        if (accScreen == null) {
            accScreen = new EGLAcceleratedScreen(attributes);
        }
        return accScreen;

    }

}

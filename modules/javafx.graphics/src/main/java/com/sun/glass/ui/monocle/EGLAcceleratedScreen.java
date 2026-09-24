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

/**
 * The <code>EGLAcceleratedScreen</code> manages the link to the hardware-accelerated
 * component, using EGL. This class is not directly using EGL commands,
 * as the order and meaning of parameters might vary between implementations.
 * Also, implementation-specific logic may be applied before, in between, or
 * after the EGL commands. The commands go to the vendor library through
 * {@link EglVendorNative}, in the order and with the arguments that eglBridge.c
 * of commit 21d5a654f6 forwarded.
 */
public class EGLAcceleratedScreen extends AcceleratedScreen {

    /** The card the display handle is asked for when {@code -Degl.displayid} is not set. */
    static final String DEFAULT_DISPLAY_ID = "/dev/dri/card1";

    private long eglWindowHandle = -1;

    /**
     * Create a new <code>EGLAcceleratedScreen</code> with a set of attributes.
     * This will create an <code>EGL Context</code> that can be used by the
     * Prism component.
     * @param attributes an array of attributes that will be used by the underlying
     *        implementation to get the best matching configuration.
     */
    EGLAcceleratedScreen(int[] attributes) {
        eglWindowHandle = platformGetNativeWindow();
        eglDisplay = EglVendorNative.getEglDisplayHandle();
        EglVendorNative.doEglInitialize(eglDisplay);
        EglVendorNative.doEglBindApi(EGL.EGL_OPENGL_ES_API);
        long eglConfig = EglVendorNative.doEglChooseConfig(eglDisplay, attributes);
        if (eglConfig == -1) {
            throw new IllegalArgumentException("Could not create an EGLChooseConfig");
        }
        eglSurface = EglVendorNative.doEglCreateWindowSurface(eglDisplay, eglConfig, eglWindowHandle);
        eglContext = EglVendorNative.doEglCreateContext(eglDisplay, eglConfig);
    }

    @Override
    protected long platformGetNativeWindow() {
        String displayID = System.getProperty("egl.displayid", DEFAULT_DISPLAY_ID);
        return EglVendorNative.getNativeWindowHandle(displayID);
    }

    @Override
    public void enableRendering(boolean flag) {
        if (flag) {
            EglVendorNative.doEglMakeCurrent(eglDisplay, eglSurface, eglSurface,
                                       eglContext);
        } else {
            EglVendorNative.doEglMakeCurrent(eglDisplay, 0, 0, eglContext);
        }
    }

    @Override
    public boolean swapBuffers() {
        boolean result = false;
        synchronized (NativeScreen.framebufferSwapLock) {
            result = EglVendorNative.doEglSwapBuffers(eglDisplay, eglSurface);
        }
        return result;
    }
}

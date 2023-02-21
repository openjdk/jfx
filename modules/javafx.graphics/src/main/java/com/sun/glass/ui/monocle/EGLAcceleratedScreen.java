/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * after the EGL commands.
 */
public class EGLAcceleratedScreen extends AcceleratedScreen {

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
        eglDisplay = nGetEglDisplayHandle();
        nEglInitialize(eglDisplay);
        nEglBindApi(EGL.EGL_OPENGL_ES_API);
        long eglConfig = nEglChooseConfig(eglDisplay, attributes);
        if (eglConfig == -1) {
            throw new IllegalArgumentException("Could not create an EGLChooseConfig");
        }
        eglSurface = nEglCreateWindowSurface(eglDisplay, eglConfig, eglWindowHandle);
        eglContext = nEglCreateContext(eglDisplay, eglConfig);
    }

    @Override
    protected long platformGetNativeWindow() {
        String displayID = System.getProperty("egl.displayid", "/dev/dri/card1" );
        return nPlatformGetNativeWindow(displayID);
    }

    @Override
    public void enableRendering(boolean flag) {
        if (flag) {
            nEglMakeCurrent(eglDisplay, eglSurface, eglSurface,
                                       eglContext);
        } else {
            nEglMakeCurrent(eglDisplay, 0, 0, eglContext);
        }
    }

    @Override
    public boolean swapBuffers() {
        boolean result = false;
        synchronized (NativeScreen.framebufferSwapLock) {
            result = nEglSwapBuffers(eglDisplay, eglSurface);
        }
        return result;
    }

    private native long nPlatformGetNativeWindow(String displayID);
    private native long nGetEglDisplayHandle();
    private native boolean nEglInitialize(long handle);
    private native boolean nEglBindApi(int v);
    private native long nEglChooseConfig(long eglDisplay, int[] attribs);
    private native boolean nEglMakeCurrent(long eglDisplay, long eglDrawSurface, long eglReadSurface, long eglContext);
    private native long nEglCreateWindowSurface(long eglDisplay, long eglConfig, long nativeWindow);
    private native long nEglCreateContext(long eglDisplay, long eglConfig);
    private native boolean nEglSwapBuffers(long eglDisplay, long eglSurface);
}

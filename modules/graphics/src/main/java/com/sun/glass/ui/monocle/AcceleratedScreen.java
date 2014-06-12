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

import com.sun.glass.ui.monocle.linux.LinuxSystem;

public class AcceleratedScreen {

    private static long glesLibraryHandle;
    private static long eglLibraryHandle;
    protected static boolean initialized = false;
    long eglSurface, eglContext, eglDisplay;
    protected static LinuxSystem ls = LinuxSystem.getLinuxSystem();

    protected long platformGetNativeDisplay() {
        return 0L;
    }

    protected long platformGetNativeWindow() {
        return 0L;
    }

    public AcceleratedScreen(int[] attributes) throws GLException, UnsatisfiedLinkError {
        initPlatformLibraries();

        int major[] = {0}, minor[]={0};
        long nativeDisplay = platformGetNativeDisplay();
        long nativeWindow = platformGetNativeWindow();

        if (nativeDisplay == -1l) { // error condition
            throw new GLException(0, "Could not get native display");
        }
        if (nativeWindow == -1l) { // error condition
            throw new GLException(0, "Could not get native window");
        }

        eglDisplay =
                EGL.eglGetDisplay(nativeDisplay);
        if (eglDisplay == EGL.EGL_NO_DISPLAY) {
            throw new GLException(EGL.eglGetError(), "Could not get EGL display");
        }

        if (!EGL.eglInitialize(eglDisplay, major, minor)) {
            throw new GLException(EGL.eglGetError(), "Error initializing EGL");
        }

        if (!EGL.eglBindAPI(EGL.EGL_OPENGL_ES_API)) {
            throw new GLException(EGL.eglGetError(), "Error binding OPENGL API");
        }

        long eglConfigs[] = {0};
        int configCount[] = {0};

        if (!EGL.eglChooseConfig(eglDisplay, attributes, eglConfigs,
                                 1, configCount)) {
            throw new GLException(EGL.eglGetError(), "Error choosing EGL config");
        }

        eglSurface =
                EGL.eglCreateWindowSurface(eglDisplay, eglConfigs[0],
                        nativeWindow, null);
        if (eglSurface == EGL.EGL_NO_SURFACE) {
            throw new GLException(EGL.eglGetError(), "Could not get EGL surface");
        }

        int emptyAttrArray [] = {};
        eglContext = EGL.eglCreateContext(eglDisplay, eglConfigs[0],
                0, emptyAttrArray);
        if (eglContext == EGL.EGL_NO_CONTEXT) {
            throw new GLException(EGL.eglGetError(), "Could not get EGL context");
        }
    }

    public void enableRendering(boolean flag) {
        if (flag) {
            EGL.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
        } else {
            EGL.eglMakeCurrent(eglDisplay, 0, 0, eglContext);
        }
    }

    protected boolean initPlatformLibraries() throws UnsatisfiedLinkError{
        if (!initialized) {
            glesLibraryHandle = ls.dlopen("libGLESv2.so",
                    LinuxSystem.RTLD_LAZY | LinuxSystem.RTLD_GLOBAL);
            if (glesLibraryHandle == 0l) {
                throw new UnsatisfiedLinkError("Error loading libGLESv2.so");
            }
            eglLibraryHandle = ls.dlopen("libEGL.so",
                    LinuxSystem.RTLD_LAZY | LinuxSystem.RTLD_GLOBAL);
            if (eglLibraryHandle == 0l) {
                throw new UnsatisfiedLinkError("Error loading libEGL.so");
            }
            initialized = true;
        }
        return true;
    }

    public long getGLHandle() {
        return glesLibraryHandle;
    }

    public long getEGLHandle() { return eglLibraryHandle; }

    public boolean swapBuffers() {
        EGL.eglSwapBuffers(eglDisplay, eglSurface);
        return true;
    }

}

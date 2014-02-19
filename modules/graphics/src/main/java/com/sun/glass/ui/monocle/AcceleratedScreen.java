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
    private static boolean initialized = false;
    long eglSurface, eglContext, eglDisplay;

    protected long platformGetNativeDisplay() {
        return 0L;
    }

    protected long platformGetNativeWindow() {
        return 0L;
    }

    public AcceleratedScreen(int[] attributes) {
        initPlatformLibraries();

        int major[] = {0}, minor[]={0};
        eglDisplay =
                EGL.eglGetDisplay(platformGetNativeDisplay());

        EGL.eglInitialize(eglDisplay, major, minor);

        EGL.eglBindAPI(EGL.EGL_OPENGL_ES_BIT);

        long eglConfigs[] = {0};
        int configCount[] = {0};

        EGL.eglChooseConfig(eglDisplay, attributes, eglConfigs, 1, configCount);

        eglSurface =
                EGL.eglCreateWindowSurface(eglDisplay, eglConfigs[0],
                        platformGetNativeWindow(), null);

        int emptyAttrArray [] = {};
        eglContext = EGL.eglCreateContext(eglDisplay, eglConfigs[0],
                0, emptyAttrArray);
    }

    public void enableRendering(boolean flag) {
        if (flag) {
            EGL.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
        } else {
            EGL.eglMakeCurrent(eglDisplay, 0, 0, eglContext);
        }
    }

    protected boolean initPlatformLibraries() {
        if (!initialized) {
            LinuxSystem ls = LinuxSystem.getLinuxSystem();
            glesLibraryHandle = ls.dlopen("libGLESv2.so",
                    LinuxSystem.RTLD_LAZY | LinuxSystem.RTLD_GLOBAL);
            eglLibraryHandle = ls.dlopen("libEGL.so",
                    LinuxSystem.RTLD_LAZY | LinuxSystem.RTLD_GLOBAL);
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

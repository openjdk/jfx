/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

/** AcceleratedScreen provides methods necessary to instantiate and intitialize
 * a hardware-accelerated screen for rendering.
 */
public class AcceleratedScreen {

    private static long glesLibraryHandle;
    private static long eglLibraryHandle;
    private static boolean initialized = false;
    long eglSurface;
    long eglContext;
    long eglDisplay;
    long nativeWindow;
    protected static final LinuxSystem ls = LinuxSystem.getLinuxSystem();
    private EGL egl;
    long eglConfigs[] = {0};

    /** Returns a platform-specific native display handle suitable for use with
     * eglGetDisplay.
     */
    protected long platformGetNativeDisplay() {
        return 0L;
    }

    /** Returns a platform-specific native window handle suitable for use with
     * eglCreateWindowSurface.
     */
    protected long platformGetNativeWindow() {
        return 0L;
    }

    /**
     * Create and initialize an AcceleratedScreen. Subclasses should override
     * this constructor in case the {@link #AcceleratedScreen(int[]) AcceleratedScreen(int[])}
     * constructor is not sufficient.
     */
    AcceleratedScreen() {
    }

    /**
     * Perform basic egl intialization - open the display, create the drawing
     * surface, and create a GL context to that drawing surface.
     * @param attributes - attributes to be used for filtering the EGL
     *                   configurations to choose from
     * @throws GLException
     * @throws UnsatisfiedLinkError
     */
    AcceleratedScreen(int[] attributes) throws GLException, UnsatisfiedLinkError {
        egl = EGL.getEGL();
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
                egl.eglGetDisplay(nativeDisplay);
        if (eglDisplay == EGL.EGL_NO_DISPLAY) {
            throw new GLException(egl.eglGetError(),
                                 "Could not get EGL display");
        }

        if (!egl.eglInitialize(eglDisplay, major, minor)) {
            throw new GLException(egl.eglGetError(),
                                  "Error initializing EGL");
        }

        if (!egl.eglBindAPI(EGL.EGL_OPENGL_ES_API)) {
            throw new GLException(egl.eglGetError(),
                                  "Error binding OPENGL API");
        }

        int configCount[] = {0};

        if (!egl.eglChooseConfig(eglDisplay, attributes, eglConfigs,
                                         1, configCount)) {
            throw new GLException(egl.eglGetError(),
                                  "Error choosing EGL config");
        }

        eglSurface =
                egl.eglCreateWindowSurface(eglDisplay, eglConfigs[0],
                                                   nativeWindow, null);
        if (eglSurface == EGL.EGL_NO_SURFACE) {
            throw new GLException(egl.eglGetError(),
                                  "Could not get EGL surface");
        }

        int emptyAttrArray [] = {};
        eglContext = egl.eglCreateContext(eglDisplay, eglConfigs[0],
                0, emptyAttrArray);
        if (eglContext == EGL.EGL_NO_CONTEXT) {
            throw new GLException(egl.eglGetError(),
                                  "Could not get EGL context");
        }
    }

    private void createSurface() {
        nativeWindow = platformGetNativeWindow();
        eglSurface = egl._eglCreateWindowSurface(eglDisplay, eglConfigs[0],
                                                   nativeWindow, null);
    }


    /** Make the EGL drawing surface current or not
     *
     * @param flag
     */
    public void enableRendering(boolean flag) {
        if (flag) {
            egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface,
                                       eglContext);
        } else {
            egl.eglMakeCurrent(eglDisplay, 0, 0, eglContext);
        }
    }

    /** Load any native libraries needed to instantiate and initialize the
     * native drawing surface and rendering context
     * @return success or failure
     * @throws UnsatisfiedLinkError
     */
    boolean initPlatformLibraries() throws UnsatisfiedLinkError{
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

    /** Return the GL library handle - for use in looking up native symbols
     *
     */
    public long getGLHandle() {
        return glesLibraryHandle;
    }

    /** Return the EGL library handle - for use in looking up native symbols
     *
     */
    protected long getEGLHandle() { return eglLibraryHandle; }

    /** Copy the contents of the GL backbuffer to the screen
     *
     * @return success or failure
     */
    public boolean swapBuffers() {
        boolean result = false;
        synchronized(NativeScreen.framebufferSwapLock) {
            result = egl.eglSwapBuffers(eglDisplay, eglSurface);
// TODO this shouldn't happen. In case the surface is invalid, we need to have recreated it before this method is called
            if (!result) {
                createSurface();
                result = egl.eglSwapBuffers(eglDisplay, eglSurface);
            }
        }
        return result;

    }

}

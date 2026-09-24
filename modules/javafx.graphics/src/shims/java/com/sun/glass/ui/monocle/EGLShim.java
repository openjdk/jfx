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

import java.lang.reflect.Field;
import java.util.List;

/** Exposes {@link EGL}, its constants, {@code setEGLAttrs} and its libEGL calls to tests. */
public final class EGLShim {

    public static final List<String> LIBRARY_NAMES = EGL.LIBRARY_NAMES;

    private EGLShim() {
    }

    public static void loadLibrary() {
        EGL.loadLibrary();
    }

    public static boolean isLibraryLoaded() {
        return EGL.isLibraryLoaded();
    }

    public static String libraryName() {
        return EGL.libraryName();
    }

    public static List<String> boundSymbols() {
        return EGL.boundSymbols();
    }

    /** The value of the {@code EGL_*} constant {@code name} of EGL. */
    public static int constant(String name) {
        try {
            Field field = EGL.class.getDeclaredField(name);
            return field.getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("EGL has no int constant " + name, e);
        }
    }

    public static int[] setEGLAttrs(int[] attrs) {
        return EGL.setEGLAttrs(attrs);
    }

    public static long eglGetDisplay(long nativeDisplay) {
        return EGL.getEGL().eglGetDisplay(nativeDisplay);
    }

    public static boolean eglInitialize(long eglDisplay, int[] major, int[] minor) {
        return EGL.getEGL().eglInitialize(eglDisplay, major, minor);
    }

    public static boolean eglBindAPI(int api) {
        return EGL.getEGL().eglBindAPI(api);
    }

    public static boolean eglChooseConfig(long eglDisplay, int[] attribs, long[] eglConfigs, int configSize,
                                          int[] configCount) {
        return EGL.getEGL().eglChooseConfig(eglDisplay, attribs, eglConfigs, configSize, configCount);
    }

    /** {@code eglChooseConfig} with a raw EGL attribute list instead of GLPixelFormat attributes. */
    public static boolean eglChooseConfigList(long eglDisplay, int[] eglAttribs, long[] eglConfigs, int configSize,
                                              int[] configCount) {
        return EGL.getEGL().eglChooseConfigList(eglDisplay, eglAttribs, eglConfigs, configSize, configCount);
    }

    public static long eglCreateContext(long eglDisplay, long eglConfig, long shareContext, int[] attribs) {
        return EGL.getEGL().eglCreateContext(eglDisplay, eglConfig, shareContext, attribs);
    }

    public static boolean eglMakeCurrent(long eglDisplay, long draw, long read, long eglContext) {
        return EGL.getEGL().eglMakeCurrent(eglDisplay, draw, read, eglContext);
    }

    public static boolean eglSwapBuffers(long eglDisplay, long eglSurface) {
        return EGL.getEGL().eglSwapBuffers(eglDisplay, eglSurface);
    }

    public static int eglGetError() {
        return EGL.getEGL().eglGetError();
    }

    public static String eglErrorToString(int errorCode) {
        return EGL.getEGL().eglErrorToString(errorCode);
    }
}

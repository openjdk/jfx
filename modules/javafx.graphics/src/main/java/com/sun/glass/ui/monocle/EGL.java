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

import java.lang.reflect.Field;
import java.security.Permission;
import java.util.Formatter;

/** Java wrapper for the EGL API */
class EGL {
    private static long eglWindowSurface = 0l;

    static final long EGL_DEFAULT_DISPLAY = 0l;
    static final long EGL_NO_CONTEXT = 0l;
    static final long EGL_NO_DISPLAY = 0l;
    static final long EGL_NO_SURFACE = 0l;
    static final int EGL_DONT_CARE = -1;
    static final int EGL_SUCCESS = 0x3000;
    static final int EGL_NOT_INITIALIZED = 0x3001;
    static final int EGL_BAD_ACCESS = 0x3002;
    static final int EGL_BAD_ALLOC = 0x3003;
    static final int EGL_BAD_ATTRIBUTE = 0x3004;
    static final int EGL_BAD_CONFIG = 0x3005;
    static final int EGL_BAD_CONTEXT = 0x3006;
    static final int EGL_BAD_CURRENT_SURFACE = 0x3007;
    static final int EGL_BAD_DISPLAY = 0x3008;
    static final int EGL_BAD_MATCH = 0x3009;
    static final int EGL_BAD_NATIVE_PIXMAP = 0x300A;
    static final int EGL_BAD_NATIVE_WINDOW = 0x300B;
    static final int EGL_BAD_PARAMETER = 0x300C;
    static final int EGL_BAD_SURFACE = 0x300D;
    static final int EGL_CONTEXT_LOST = 0x300E;
    static final int EGL_BUFFER_SIZE = 0x3020;
    static final int EGL_ALPHA_SIZE = 0x3021;
    static final int EGL_BLUE_SIZE = 0x3022;
    static final int EGL_GREEN_SIZE = 0x3023;
    static final int EGL_RED_SIZE = 0x3024;
    static final int EGL_DEPTH_SIZE = 0x3025;
    static final int EGL_STENCIL_SIZE = 0x3026;
    static final int EGL_CONFIG_CAVEAT = 0x3027;
    static final int EGL_CONFIG_ID = 0x3028;
    static final int EGL_LEVEL = 0x3029;
    static final int EGL_MAX_PBUFFER_HEIGHT = 0x302A;
    static final int EGL_MAX_PBUFFER_PIXELS = 0x302B;
    static final int EGL_MAX_PBUFFER_WIDTH = 0x302C;
    static final int EGL_NATIVE_RENDERABLE = 0x302D;
    static final int EGL_NATIVE_VISUAL_ID = 0x302E;
    static final int EGL_NATIVE_VISUAL_TYPE = 0x302F;
    static final int EGL_SAMPLES = 0x3031;
    static final int EGL_SAMPLE_BUFFERS = 0x3032;
    static final int EGL_SURFACE_TYPE = 0x3033;
    static final int EGL_TRANSPARENT_TYPE = 0x3034;
    static final int EGL_TRANSPARENT_BLUE_VALUE = 0x3035;
    static final int EGL_TRANSPARENT_GREEN_VALUE = 0x3036;
    static final int EGL_TRANSPARENT_RED_VALUE = 0x3037;
    static final int EGL_NONE = 0x3038;
    static final int EGL_BIND_TO_TEXTURE_RGB = 0x3039;
    static final int EGL_BIND_TO_TEXTURE_RGBA = 0x303A;
    static final int EGL_MIN_SWAP_INTERVAL = 0x303B;
    static final int EGL_MAX_SWAP_INTERVAL = 0x303C;
    static final int EGL_LUMINANCE_SIZE = 0x303D;
    static final int EGL_ALPHA_MASK_SIZE = 0x303E;
    static final int EGL_COLOR_BUFFER_TYPE = 0x303F;
    static final int EGL_RENDERABLE_TYPE = 0x3040;
    static final int EGL_MATCH_NATIVE_PIXMAP = 0x3041;
    static final int EGL_CONFORMANT = 0x3042;
    static final int EGL_SLOW_CONFIG = 0x3050;
    static final int EGL_NON_CONFORMANT_CONFIG = 0x3051;
    static final int EGL_TRANSPARENT_RGB = 0x3052;
    static final int EGL_RGB_BUFFER = 0x308E;
    static final int EGL_LUMINANCE_BUFFER = 0x308F;
    static final int EGL_NO_TEXTURE = 0x305C;
    static final int EGL_TEXTURE_RGB = 0x305D;
    static final int EGL_TEXTURE_RGBA = 0x305E;
    static final int EGL_TEXTURE_2D = 0x305F;
    static final int EGL_PBUFFER_BIT = 0x0001;
    static final int EGL_PIXMAP_BIT = 0x0002;
    static final int EGL_WINDOW_BIT = 0x0004;
    static final int EGL_VG_COLORSPACE_LINEAR_BIT = 0x0020;
    static final int EGL_VG_ALPHA_FORMAT_PRE_BIT = 0x0040;
    static final int EGL_MULTISAMPLE_RESOLVE_BOX_BIT = 0x0200;
    static final int EGL_SWAP_BEHAVIOR_PRESERVED_BIT = 0x0400;
    static final int EGL_OPENGL_ES_BIT = 0x0001;
    static final int EGL_OPENVG_BIT = 0x0002;
    static final int EGL_OPENGL_ES = 2;
    static final int EGL_OPENGL_BIT = 0x0008;
    static final int EGL_VENDOR = 0x3053;
    static final int EGL_VERSION = 0x3054;
    static final int EGL_EXTENSIONS = 0x3055;
    static final int EGL_CLIENT_APIS = 0x308D;
    static final int EGL_HEIGHT = 0x3056;
    static final int EGL_WIDTH = 0x3057;
    static final int EGL_LARGEST_PBUFFER = 0x3058;
    static final int EGL_TEXTURE_FORMAT = 0x3080;
    static final int EGL_TEXTURE_TARGET = 0x3081;
    static final int EGL_MIPMAP_TEXTURE = 0x3082;
    static final int EGL_MIPMAP_LEVEL = 0x3083;
    static final int EGL_RENDER_BUFFER = 0x3086;
    static final int EGL_VG_COLORSPACE = 0x3087;
    static final int EGL_VG_ALPHA_FORMAT = 0x3088;
    static final int EGL_HORIZONTAL_RESOLUTION = 0x3090;
    static final int EGL_VERTICAL_RESOLUTION = 0x3091;
    static final int EGL_PIXEL_ASPECT_RATIO = 0x3092;
    static final int EGL_SWAP_BEHAVIOR = 0x3093;
    static final int EGL_MULTISAMPLE_RESOLVE = 0x3099;
    static final int EGL_BACK_BUFFER = 0x3084;
    static final int EGL_SINGLE_BUFFER = 0x3085;
    static final int EGL_VG_COLORSPACE_sRGB = 0x3089;
    static final int EGL_VG_COLORSPACE_LINEAR = 0x308A;
    static final int EGL_VG_ALPHA_FORMAT_NONPRE = 0x308B;
    static final int EGL_VG_ALPHA_FORMAT_PRE = 0x308C;
    static final int EGL_DISPLAY_SCALING = 10000;
    static final int EGL_UNKNOWN = -1;
    static final int EGL_BUFFER_PRESERVED = 0x3094;
    static final int EGL_BUFFER_DESTROYED = 0x3095;
    static final int EGL_OPENVG_IMAGE = 0x3096;
    static final int EGL_CONTEXT_CLIENT_TYPE = 0x3097;
    static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    static final int EGL_MULTISAMPLE_RESOLVE_DEFAULT = 0x309A;
    static final int EGL_MULTISAMPLE_RESOLVE_BOX = 0x309B;
    static final int EGL_OPENGL_ES_API = 0x30A0;
    static final int EGL_OPENVG_API = 0x30A1;
    static final int EGL_OPENGL_API = 0x30A2;
    static final int EGL_DRAW = 0x3059;
    static final int EGL_READ = 0x305A;
    static final int EGL_CORE_NATIVE_ENGINE = 0x305B;

    private static Permission permission = new RuntimePermission("loadLibrary.*");

    private static EGL instance = new EGL();

    private EGL() {}

    /**
     * Obtains the single instance of EGL. Calling this method requires
     * the RuntimePermission "loadLibrary.*".
     *
     */
    static EGL getEGL() {
        checkPermissions();
        return instance;
    }

    private static void checkPermissions() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(permission);
        }
    }

    native void loadFunctions(long dlHandle);

    native boolean eglBindAPI(int api);

    native boolean eglChooseConfig(
            long eglDisplay,
            int[] attribs,
            long[] eglConfigs,
            int configSize,
            int[] configCount);

    native long eglContextFromConfig(long eglDisplay, long eglConfig);

    native long eglCreateContext(
            long eglDisplay,
            long eglConfig,
            long shareContext,
            int[] attribs);

    long eglCreateWindowSurface(long eglDisplay,
                                              long eglConfig,
                                              long nativeWindow,
                                              int[] attribs) {
        if (eglWindowSurface == 0) {
            eglWindowSurface = _eglCreateWindowSurface(eglDisplay, eglConfig,
                                                       nativeWindow, attribs);

        }
        return eglWindowSurface;
    }

    native long _eglCreateWindowSurface(
            long eglDisplay,
            long eglConfig,
            long nativeWindow,
            int[] attribs);

    native boolean eglDestroyContext(long eglDisplay, long eglContext);

    native boolean eglGetConfigAttrib(
            long eglDisplay,
            long eglConfig,
            int attrib,
            int[] value);

    native long eglGetDisplay(long nativeDisplay);

    native int eglGetError();

    native boolean eglInitialize(long eglDisplay, int[] major,
                                        int[] minor);

    native boolean eglMakeCurrent(
            long eglDisplay,
            long eglDrawSurface,
            long eglReadSurface,
            long eglContext);

    native String eglQueryString(long eglDisplay, int name);

    native String eglQueryVersion(long eglDisplay, int versionType);

    native boolean eglSwapBuffers(long eglDisplay, long eglSurface);

    /** Convert an EGL error code such as EGL_BAD_CONTEXT to a string
     * representation.
     * @param errorCode the EGL error code
     * @return the constant name of the error code. If errorCode cannot be
     * matched to an EGL error, a string representation of the error code's
     * value is returned.
     */
    String eglErrorToString(int errorCode) {
        if (errorCode >= 0x3000 && errorCode < 0x3020) {
            for (Field field : EGL.class.getFields()) {
                try {
                    if (field.getName().startsWith("EGL_")
                            && field.getType() == Integer.TYPE
                            && field.getInt(null) == errorCode) {
                        return field.getName();
                    }
                } catch (IllegalAccessException e) {
                }
            }
        }
        return new Formatter()
                .format("0x%04x", errorCode & 0xffff)
                .out()
                .toString();
    }

}

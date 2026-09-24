/*
 * Copyright (c) 2014, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * Java wrapper for the EGL API.
 * <p>
 * Each call binds the libEGL function of the same name through {@code java.lang.foreign}, where EGL.c of
 * commit 21d5a654f6 wrapped it in a JNI function whose undefined {@code egl*} references the dynamic linker
 * resolved against the libEGL that AcceleratedScreen had opened with {@code dlopen(RTLD_GLOBAL)}. That
 * {@code dlopen} sequence (libGLESv2.so, then libEGL.so) is unchanged, and the lookup here opens
 * {@code libEGL.so}, the instance already in the process. The {@code libEGL.so.1} candidate is for a test JVM
 * that binds without AcceleratedScreen: a board reaches this class only through AcceleratedScreen, whose own
 * {@code dlopen("libEGL.so")} has succeeded first. EGL is a facade class of this package: every restricted
 * method of the FFM API it uses is called from here. libEGL is bound by the lazy holder {@code Lib} on the
 * first call, never when this class initialises. {@code setEGLAttrs}, the C translation of the GLPixelFormat
 * attributes into an EGL attribute list, is Java here, held to the golden captured from the C.
 */
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
    /** {@code EGL_OPENGL_ES2_BIT} of egl.h: the renderable type the C asked every configuration for. */
    static final int EGL_OPENGL_ES2_BIT = 0x0004;
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

    /** The library names tried, in order: the development name the C opened, then the versioned soname. */
    static final List<String> LIBRARY_NAMES = List.of("libEGL.so", "libEGL.so.1");

    /** The indices of the GLPixelFormat.Attributes array {@link #setEGLAttrs} translates. */
    private static final int ATTRIBUTE_RED = 0;
    private static final int ATTRIBUTE_GREEN = 1;
    private static final int ATTRIBUTE_BLUE = 2;
    private static final int ATTRIBUTE_ALPHA = 3;
    private static final int ATTRIBUTE_DEPTH = 4;
    private static final int ATTRIBUTE_ONSCREEN = 6;

    /** An EGL attribute list is at most seven pairs and the terminator. */
    private static final int MAX_EGL_ATTRIBUTES = 15;

    private static EGL instance = new EGL();

    private EGL() {}

    /**
     * Obtains the single instance of EGL.
     *
     */
    static EGL getEGL() {
        return instance;
    }

    /**
     * Binds libEGL, as the first EGL call does. An {@link UnsatisfiedLinkError} surfaces here when no libEGL
     * can be opened or a symbol does not resolve.
     */
    static void loadLibrary() {
        try {
            Lib.require();
        } catch (NoClassDefFoundError e) {
            throw unbound(e);
        }
    }

    /** Whether libEGL can be bound: binds it on the first call and answers false instead of throwing. */
    static boolean isLibraryLoaded() {
        try {
            Lib.require();
            return Lib.LINKED;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            return false;
        }
    }

    /** The name libEGL was opened under. Binds libEGL if that has not happened. */
    static String libraryName() {
        return Lib.LIBRARY_NAME;
    }

    /** {@code <library>!symbol} of every bound symbol, in binding order. Binds libEGL if that has not happened. */
    static List<String> boundSymbols() {
        synchronized (Lib.BOUND) {
            return new ArrayList<>(Lib.BOUND.keySet());
        }
    }

    private static UnsatisfiedLinkError unbound(NoClassDefFoundError e) {
        UnsatisfiedLinkError error = new UnsatisfiedLinkError("libEGL is not bound: " + e.getMessage());
        error.initCause(e);
        return error;
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException runtime) {
            return runtime;
        }
        if (t instanceof NoClassDefFoundError e) {
            throw unbound(e);
        }
        if (t instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(t);
    }

    /**
     * libEGL, opened once for the process (the JNI never closed it) and bound on first use only. EGLBoolean is
     * an {@code unsigned int} and is bound as JAVA_INT; EGLDisplay, EGLConfig, EGLContext, EGLSurface and
     * EGLNativeDisplayType are pointers; EGLNativeWindowType is an X Window or a {@code khronos_uintptr_t}, one
     * integer register on LP64, bound as JAVA_LONG.
     */
    @SuppressWarnings("restricted")
    private static final class Lib {

        static final Linker LINKER = Linker.nativeLinker();

        /** {@code <library>!symbol} for every symbol this class binds, in binding order; read by tests. */
        static final Map<String, Long> BOUND = Collections.synchronizedMap(new LinkedHashMap<>());

        static final SymbolLookup LIBRARY;
        static final String LIBRARY_NAME;

        static {
            SymbolLookup lookup = null;
            String name = null;
            IllegalArgumentException failure = null;
            for (String candidate : LIBRARY_NAMES) {
                try {
                    lookup = SymbolLookup.libraryLookup(candidate, Arena.global());
                    name = candidate;
                    break;
                } catch (IllegalArgumentException e) {
                    failure = e;
                }
            }
            if (lookup == null) {
                UnsatisfiedLinkError error = new UnsatisfiedLinkError("Error loading libEGL.so: tried "
                        + LIBRARY_NAMES + ": " + failure.getMessage());
                error.initCause(failure);
                throw error;
            }
            LIBRARY = lookup;
            LIBRARY_NAME = name;
        }

        /** {@code EGLDisplay eglGetDisplay(EGLNativeDisplayType)}. */
        static final MethodHandle GET_DISPLAY = bind("eglGetDisplay", FunctionDescriptor.of(ADDRESS, ADDRESS));

        /** {@code EGLBoolean eglInitialize(EGLDisplay, EGLint *major, EGLint *minor)}. */
        static final MethodHandle INITIALIZE = bind("eglInitialize",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

        /** {@code EGLBoolean eglBindAPI(EGLenum)}. */
        static final MethodHandle BIND_API = bind("eglBindAPI", FunctionDescriptor.of(JAVA_INT, JAVA_INT));

        /**
         * {@code EGLBoolean eglChooseConfig(EGLDisplay, const EGLint *attrib_list, EGLConfig *configs,
         * EGLint config_size, EGLint *num_config)}.
         */
        static final MethodHandle CHOOSE_CONFIG = bind("eglChooseConfig",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));

        /**
         * {@code EGLSurface eglCreateWindowSurface(EGLDisplay, EGLConfig, EGLNativeWindowType,
         * const EGLint *attrib_list)}.
         */
        static final MethodHandle CREATE_WINDOW_SURFACE = bind("eglCreateWindowSurface",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS));

        /** {@code EGLContext eglCreateContext(EGLDisplay, EGLConfig, EGLContext share, const EGLint *attrib_list)}. */
        static final MethodHandle CREATE_CONTEXT = bind("eglCreateContext",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

        /** {@code EGLBoolean eglMakeCurrent(EGLDisplay, EGLSurface draw, EGLSurface read, EGLContext)}. */
        static final MethodHandle MAKE_CURRENT = bind("eglMakeCurrent",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

        /** {@code EGLBoolean eglSwapBuffers(EGLDisplay, EGLSurface)}. */
        static final MethodHandle SWAP_BUFFERS = bind("eglSwapBuffers",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        /** {@code EGLint eglGetError(void)}. */
        static final MethodHandle GET_ERROR = bind("eglGetError", FunctionDescriptor.of(JAVA_INT));

        /** True once every handle above is bound; set last, and reading it initialises this class. */
        static final boolean LINKED = !BOUND.isEmpty();

        static void require() {
        }

        /** Resolves {@code name} in libEGL and links it as an ordinary (never critical) downcall. */
        private static MethodHandle bind(String name, FunctionDescriptor descriptor) {
            MemorySegment symbol = LIBRARY.find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError(LIBRARY_NAME + " does not export " + name));
            BOUND.put(LIBRARY_NAME + "!" + name, symbol.address());
            return LINKER.downcallHandle(symbol, descriptor);
        }
    }

    /**
     * The EGL attribute list for a GLPixelFormat attribute array, exactly as {@code setEGLAttrs} of EGL.c of
     * commit 21d5a654f6 built it: EGL_SURFACE_TYPE is EGL_WINDOW_BIT when ONSCREEN (index 6) is not 0 and
     * EGL_PBUFFER_BIT otherwise; red, green, blue and alpha sizes 5, 6, 5, 0 collapse to EGL_BUFFER_SIZE 16
     * (less per-frame overhead on the Raspberry Pi model B), any other sizes are listed one by one;
     * EGL_DEPTH_SIZE is index 4; EGL_RENDERABLE_TYPE is EGL_OPENGL_ES2_BIT; EGL_NONE ends the list. Index 5
     * (DOUBLEBUFFER) is not read.
     *
     * @param attrs the GLPixelFormat.Attributes array: RED, GREEN, BLUE, ALPHA, DEPTH, DOUBLEBUFFER, ONSCREEN
     * @return the attribute list, EGL_NONE included
     */
    static int[] setEGLAttrs(int[] attrs) {
        int[] eglAttrs = new int[MAX_EGL_ATTRIBUTES];
        int index = 0;
        eglAttrs[index++] = EGL_SURFACE_TYPE;
        eglAttrs[index++] = attrs[ATTRIBUTE_ONSCREEN] != 0 ? EGL_WINDOW_BIT : EGL_PBUFFER_BIT;
        if (attrs[ATTRIBUTE_RED] == 5 && attrs[ATTRIBUTE_GREEN] == 6
                && attrs[ATTRIBUTE_BLUE] == 5 && attrs[ATTRIBUTE_ALPHA] == 0) {
            eglAttrs[index++] = EGL_BUFFER_SIZE;
            eglAttrs[index++] = 16;
        } else {
            eglAttrs[index++] = EGL_RED_SIZE;
            eglAttrs[index++] = attrs[ATTRIBUTE_RED];
            eglAttrs[index++] = EGL_GREEN_SIZE;
            eglAttrs[index++] = attrs[ATTRIBUTE_GREEN];
            eglAttrs[index++] = EGL_BLUE_SIZE;
            eglAttrs[index++] = attrs[ATTRIBUTE_BLUE];
            eglAttrs[index++] = EGL_ALPHA_SIZE;
            eglAttrs[index++] = attrs[ATTRIBUTE_ALPHA];
        }
        eglAttrs[index++] = EGL_DEPTH_SIZE;
        eglAttrs[index++] = attrs[ATTRIBUTE_DEPTH];
        eglAttrs[index++] = EGL_RENDERABLE_TYPE;
        eglAttrs[index++] = EGL_OPENGL_ES2_BIT;
        eglAttrs[index++] = EGL_NONE;
        return Arrays.copyOf(eglAttrs, index);
    }

    boolean eglBindAPI(int api) {
        try {
            return (int) Lib.BIND_API.invokeExact(api) != 0;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code eglChooseConfig} for a GLPixelFormat attribute array, translated by {@link #setEGLAttrs}. On
     * success the number of configurations found goes into {@code configCount[0]} and that many configurations
     * into {@code eglConfigs}; the C copied {@code configSize} slots, the ones beyond the count being
     * uninitialised memory, and this copies the count and leaves the rest as they were.
     */
    boolean eglChooseConfig(
            long eglDisplay,
            int[] attribs,
            long[] eglConfigs,
            int configSize,
            int[] configCount) {
        return eglChooseConfigList(eglDisplay, setEGLAttrs(attribs), eglConfigs, configSize, configCount);
    }

    /** {@link #eglChooseConfig} with an EGL attribute list already built. */
    boolean eglChooseConfigList(long eglDisplay, int[] eglAttribs, long[] eglConfigs, int configSize,
                                int[] configCount) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment attribs = arena.allocateFrom(JAVA_INT, eglAttribs);
            MemorySegment configs = arena.allocate(ADDRESS, configSize);
            MemorySegment count = arena.allocate(JAVA_INT);
            int ok = (int) Lib.CHOOSE_CONFIG.invokeExact(MemorySegment.ofAddress(eglDisplay), attribs, configs,
                    configSize, count);
            if (ok == 0) {
                return false;
            }
            int found = count.get(JAVA_INT, 0);
            configCount[0] = found;
            for (int i = 0; i < found; i++) {
                eglConfigs[i] = configs.getAtIndex(ADDRESS, i).address();
            }
            return true;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code eglCreateContext} with no share context and the attribute list
     * {@code {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE}}: EGL.c of commit 21d5a654f6 ignored the shareContext
     * and attribs arguments and passed those.
     */
    long eglCreateContext(
            long eglDisplay,
            long eglConfig,
            long shareContext,
            int[] attribs) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment contextAttribs = arena.allocateFrom(JAVA_INT, EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE);
            return ((MemorySegment) Lib.CREATE_CONTEXT.invokeExact(MemorySegment.ofAddress(eglDisplay),
                    MemorySegment.ofAddress(eglConfig), MemorySegment.NULL, contextAttribs)).address();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

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

    /**
     * {@code eglCreateWindowSurface} with a NULL attribute list: EGL.c of commit 21d5a654f6 ignored the attribs
     * argument and passed NULL.
     */
    long _eglCreateWindowSurface(
            long eglDisplay,
            long eglConfig,
            long nativeWindow,
            int[] attribs) {
        try {
            return ((MemorySegment) Lib.CREATE_WINDOW_SURFACE.invokeExact(MemorySegment.ofAddress(eglDisplay),
                    MemorySegment.ofAddress(eglConfig), nativeWindow, MemorySegment.NULL)).address();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    long eglGetDisplay(long nativeDisplay) {
        try {
            return ((MemorySegment) Lib.GET_DISPLAY.invokeExact(MemorySegment.ofAddress(nativeDisplay))).address();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    int eglGetError() {
        try {
            return (int) Lib.GET_ERROR.invokeExact();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code eglInitialize}; the version goes into {@code major[0]} and {@code minor[0]} on success only. */
    boolean eglInitialize(long eglDisplay, int[] major,
                                        int[] minor) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment majorOut = arena.allocate(JAVA_INT);
            MemorySegment minorOut = arena.allocate(JAVA_INT);
            int ok = (int) Lib.INITIALIZE.invokeExact(MemorySegment.ofAddress(eglDisplay), majorOut, minorOut);
            if (ok == 0) {
                return false;
            }
            major[0] = majorOut.get(JAVA_INT, 0);
            minor[0] = minorOut.get(JAVA_INT, 0);
            return true;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    boolean eglMakeCurrent(
            long eglDisplay,
            long eglDrawSurface,
            long eglReadSurface,
            long eglContext) {
        try {
            return (int) Lib.MAKE_CURRENT.invokeExact(MemorySegment.ofAddress(eglDisplay),
                    MemorySegment.ofAddress(eglDrawSurface), MemorySegment.ofAddress(eglReadSurface),
                    MemorySegment.ofAddress(eglContext)) != 0;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    boolean eglSwapBuffers(long eglDisplay, long eglSurface) {
        try {
            return (int) Lib.SWAP_BUFFERS.invokeExact(MemorySegment.ofAddress(eglDisplay),
                    MemorySegment.ofAddress(eglSurface)) != 0;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

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

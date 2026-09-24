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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The vendor bridge of the EGL platform ({@code -Dmonocle.platform=EGL}): the 23 functions of
 * {@code monocle_egl_ext.h} that the library named by {@code -Dmonocle.egl.lib} exports, bound through
 * {@code java.lang.foreign} where eglBridge.c of commit 21d5a654f6 forwarded to them through JNI. The
 * contract is {@link #CONTRACT}: {@code int64_t} and every handle is {@code JAVA_LONG}, {@code int32_t} is
 * {@code JAVA_INT}, {@code uint8_t} is {@code JAVA_BYTE} (any non-zero byte is true, as the JNI's
 * {@code jboolean} was), {@code float} is {@code JAVA_FLOAT} and the pointers are {@code ADDRESS}. The
 * strings, attribute lists and cursor images cross in arenas confined to the call, as the JNI copied them
 * in ({@code GetStringUTFChars}, {@code GetIntArrayElements} released with {@code JNI_ABORT},
 * {@code GetByteArrayElements}) and never back.
 * <p>
 * The library is bound by the lazy holder {@code Lib} on {@link #loadLibrary()}, which {@link EGLPlatform}
 * calls from its constructor, or on the first call: the library is opened first with the libc
 * {@code dlopen(RTLD_LAZY | RTLD_GLOBAL)} that EGLPlatform of that commit performed, so that its own
 * dependencies (libEGL, libGLESv2, libgbm, libdrm, ...) stay visible to the whole process, then its 23
 * symbols are looked up and bound; a symbol it does not export is an {@link UnsatisfiedLinkError} naming
 * it. Without the property the symbols are looked up in the global scope of the process, where the lazy
 * binding of the JNI forwarders found them. This is the facade class of the vendor bridge: every
 * restricted method of the FFM API the bridge uses is called from here.
 */
final class EglVendorNative {

    /** The system property naming the vendor library. */
    static final String LIBRARY_PROPERTY = "monocle.egl.lib";

    /** The 23 functions of {@code monocle_egl_ext.h}, in the header's order, with their C signatures. */
    static final Map<String, FunctionDescriptor> CONTRACT = contract();

    private static final Linker LINKER = Linker.nativeLinker();

    private EglVendorNative() {
    }

    private static Map<String, FunctionDescriptor> contract() {
        Map<String, FunctionDescriptor> contract = new LinkedHashMap<>();
        contract.put("getNativeWindowHandle", FunctionDescriptor.of(JAVA_LONG, ADDRESS));
        contract.put("getEglDisplayHandle", FunctionDescriptor.of(JAVA_LONG));
        contract.put("doEglInitialize", FunctionDescriptor.of(JAVA_BYTE, ADDRESS));
        contract.put("doEglBindApi", FunctionDescriptor.of(JAVA_BYTE, JAVA_INT));
        contract.put("doEglChooseConfig", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, ADDRESS));
        contract.put("doEglCreateWindowSurface", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_LONG));
        contract.put("doEglCreateContext", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_LONG));
        contract.put("doEglMakeCurrent",
                FunctionDescriptor.of(JAVA_BYTE, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_LONG));
        contract.put("doEglSwapBuffers", FunctionDescriptor.of(JAVA_BYTE, JAVA_LONG, JAVA_LONG));
        contract.put("doGetNumberOfScreens", FunctionDescriptor.of(JAVA_INT));
        contract.put("doGetHandle", FunctionDescriptor.of(JAVA_LONG, JAVA_INT));
        contract.put("doGetDepth", FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        contract.put("doGetWidth", FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        contract.put("doGetHeight", FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        contract.put("doGetOffsetX", FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        contract.put("doGetOffsetY", FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        contract.put("doGetDpi", FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        contract.put("doGetNativeFormat", FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        contract.put("doGetScale", FunctionDescriptor.of(JAVA_FLOAT, JAVA_INT));
        contract.put("doInitCursor", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT));
        contract.put("doSetCursorVisibility", FunctionDescriptor.ofVoid(JAVA_BYTE));
        contract.put("doSetLocation", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT));
        contract.put("doSetCursorImage", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        return Collections.unmodifiableMap(contract);
    }

    /** The vendor library and its 23 downcall handles, bound when this class initialises. */
    private static final class Lib {

        static final String NAME = System.getProperty(LIBRARY_PROPERTY);
        private static final SymbolLookup LOOKUP = open(NAME);
        private static final List<String> BOUND = new ArrayList<>();

        static final MethodHandle GET_NATIVE_WINDOW_HANDLE = bind("getNativeWindowHandle");
        static final MethodHandle GET_EGL_DISPLAY_HANDLE = bind("getEglDisplayHandle");
        static final MethodHandle DO_EGL_INITIALIZE = bind("doEglInitialize");
        static final MethodHandle DO_EGL_BIND_API = bind("doEglBindApi");
        static final MethodHandle DO_EGL_CHOOSE_CONFIG = bind("doEglChooseConfig");
        static final MethodHandle DO_EGL_CREATE_WINDOW_SURFACE = bind("doEglCreateWindowSurface");
        static final MethodHandle DO_EGL_CREATE_CONTEXT = bind("doEglCreateContext");
        static final MethodHandle DO_EGL_MAKE_CURRENT = bind("doEglMakeCurrent");
        static final MethodHandle DO_EGL_SWAP_BUFFERS = bind("doEglSwapBuffers");
        static final MethodHandle DO_GET_NUMBER_OF_SCREENS = bind("doGetNumberOfScreens");
        static final MethodHandle DO_GET_HANDLE = bind("doGetHandle");
        static final MethodHandle DO_GET_DEPTH = bind("doGetDepth");
        static final MethodHandle DO_GET_WIDTH = bind("doGetWidth");
        static final MethodHandle DO_GET_HEIGHT = bind("doGetHeight");
        static final MethodHandle DO_GET_OFFSET_X = bind("doGetOffsetX");
        static final MethodHandle DO_GET_OFFSET_Y = bind("doGetOffsetY");
        static final MethodHandle DO_GET_DPI = bind("doGetDpi");
        static final MethodHandle DO_GET_NATIVE_FORMAT = bind("doGetNativeFormat");
        static final MethodHandle DO_GET_SCALE = bind("doGetScale");
        static final MethodHandle DO_INIT_CURSOR = bind("doInitCursor");
        static final MethodHandle DO_SET_CURSOR_VISIBILITY = bind("doSetCursorVisibility");
        static final MethodHandle DO_SET_LOCATION = bind("doSetLocation");
        static final MethodHandle DO_SET_CURSOR_IMAGE = bind("doSetCursorImage");

        /** The symbols bound, in the header's order; complete once the class has initialised. */
        static final List<String> BOUND_SYMBOLS = Collections.unmodifiableList(BOUND);

        private Lib() {
        }

        /**
         * The library of {@code -Dmonocle.egl.lib}, opened RTLD_GLOBAL and then looked up, or the global
         * scope of the process when the property is not set.
         */
        @SuppressWarnings("restricted")
        private static SymbolLookup open(String name) {
            LinuxSystem system = LinuxSystem.getLinuxSystem();
            if (name == null) {
                return symbol -> {
                    long address = system.dlsym(0L, symbol);
                    return address == 0L ? Optional.empty() : Optional.of(MemorySegment.ofAddress(address));
                };
            }
            long handle = system.dlopen(name, LinuxSystem.RTLD_LAZY | LinuxSystem.RTLD_GLOBAL);
            if (handle == 0L) {
                throw new UnsatisfiedLinkError("EGLPlatform failed to load the requested library " + name);
            }
            return SymbolLookup.libraryLookup(name, Arena.global());
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bind(String name) {
            MemorySegment symbol = LOOKUP.find(name).orElseThrow(() -> new UnsatisfiedLinkError(
                    (NAME == null
                            ? "no library of this process exports "
                            : "the library " + NAME + " of -D" + LIBRARY_PROPERTY + " does not export ")
                    + name + ", which monocle_egl_ext.h requires"));
            BOUND.add(name);
            return LINKER.downcallHandle(symbol, CONTRACT.get(name));
        }
    }

    /**
     * Loads and binds the vendor library, or does nothing when that has happened.
     *
     * @throws UnsatisfiedLinkError if the library named by {@code -Dmonocle.egl.lib} cannot be opened, or it
     *         (or the process, when the property is not set) does not export one of the 23 functions
     */
    static void loadLibrary() {
        boundSymbols();
    }

    /** Whether the vendor library is bound; a first call binds it, a library that cannot be bound is false. */
    static boolean isLibraryLoaded() {
        try {
            return !boundSymbols().isEmpty();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** The 23 symbols bound, in the header's order. */
    static List<String> boundSymbols() {
        try {
            return Lib.BOUND_SYMBOLS;
        } catch (NoClassDefFoundError e) {
            throw unbound(e);
        }
    }

    /** Any non-zero {@code uint8_t} is true, as HotSpot read the JNI's {@code jboolean}. */
    static boolean truth(byte value) {
        return value != 0;
    }

    /* ---------------------------------------------------------------------------------------------
     * EGLAcceleratedScreen
     * ------------------------------------------------------------------------------------------- */

    /** {@code int64_t getNativeWindowHandle(const char *cardId)}; the card id crosses as UTF-8. */
    static long getNativeWindowHandle(String cardId) {
        try (Arena arena = Arena.ofConfined()) {
            return (long) Lib.GET_NATIVE_WINDOW_HANDLE.invokeExact(arena.allocateFrom(cardId));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int64_t getEglDisplayHandle(void)}. */
    static long getEglDisplayHandle() {
        try {
            return (long) Lib.GET_EGL_DISPLAY_HANDLE.invokeExact();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code uint8_t doEglInitialize(void *eglDisplay)}: the handle crosses as the pointer it is. */
    static boolean doEglInitialize(long eglDisplay) {
        try {
            return truth((byte) Lib.DO_EGL_INITIALIZE.invokeExact(MemorySegment.ofAddress(eglDisplay)));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code uint8_t doEglBindApi(int32_t api)}. */
    static boolean doEglBindApi(int api) {
        try {
            return truth((byte) Lib.DO_EGL_BIND_API.invokeExact(api));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code int64_t doEglChooseConfig(int64_t eglDisplay, int32_t *attribs)}: the list is copied in for the
     * call and never back, as the JNI released it with {@code JNI_ABORT}. The vendor library reads it by its
     * own convention; nothing is appended.
     */
    static long doEglChooseConfig(long eglDisplay, int[] attribs) {
        try (Arena arena = Arena.ofConfined()) {
            return (long) Lib.DO_EGL_CHOOSE_CONFIG.invokeExact(eglDisplay, arena.allocateFrom(JAVA_INT, attribs));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int64_t doEglCreateWindowSurface(int64_t eglDisplay, int64_t config, int64_t nativeWindow)}. */
    static long doEglCreateWindowSurface(long eglDisplay, long config, long nativeWindow) {
        try {
            return (long) Lib.DO_EGL_CREATE_WINDOW_SURFACE.invokeExact(eglDisplay, config, nativeWindow);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int64_t doEglCreateContext(int64_t eglDisplay, int64_t config)}. */
    static long doEglCreateContext(long eglDisplay, long config) {
        try {
            return (long) Lib.DO_EGL_CREATE_CONTEXT.invokeExact(eglDisplay, config);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code uint8_t doEglMakeCurrent(int64_t eglDisplay, int64_t draw, int64_t read, int64_t eglContext)}. */
    static boolean doEglMakeCurrent(long eglDisplay, long drawSurface, long readSurface, long eglContext) {
        try {
            return truth((byte) Lib.DO_EGL_MAKE_CURRENT.invokeExact(eglDisplay, drawSurface, readSurface,
                    eglContext));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code uint8_t doEglSwapBuffers(int64_t eglDisplay, int64_t eglSurface)}. */
    static boolean doEglSwapBuffers(long eglDisplay, long eglSurface) {
        try {
            return truth((byte) Lib.DO_EGL_SWAP_BUFFERS.invokeExact(eglDisplay, eglSurface));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * EGLPlatform / EGLScreen
     * ------------------------------------------------------------------------------------------- */

    /** {@code int32_t doGetNumberOfScreens(void)}. */
    static int doGetNumberOfScreens() {
        try {
            return (int) Lib.DO_GET_NUMBER_OF_SCREENS.invokeExact();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int64_t doGetHandle(int32_t idx)}. */
    static long doGetHandle(int idx) {
        try {
            return (long) Lib.DO_GET_HANDLE.invokeExact(idx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int32_t doGetDepth(int32_t idx)}. */
    static int doGetDepth(int idx) {
        try {
            return (int) Lib.DO_GET_DEPTH.invokeExact(idx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int32_t doGetWidth(int32_t idx)}. */
    static int doGetWidth(int idx) {
        try {
            return (int) Lib.DO_GET_WIDTH.invokeExact(idx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int32_t doGetHeight(int32_t idx)}. */
    static int doGetHeight(int idx) {
        try {
            return (int) Lib.DO_GET_HEIGHT.invokeExact(idx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int32_t doGetOffsetX(int32_t idx)}. */
    static int doGetOffsetX(int idx) {
        try {
            return (int) Lib.DO_GET_OFFSET_X.invokeExact(idx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int32_t doGetOffsetY(int32_t idx)}. */
    static int doGetOffsetY(int idx) {
        try {
            return (int) Lib.DO_GET_OFFSET_Y.invokeExact(idx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int32_t doGetDpi(int32_t idx)}. */
    static int doGetDpi(int idx) {
        try {
            return (int) Lib.DO_GET_DPI.invokeExact(idx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code int32_t doGetNativeFormat(int32_t idx)}. */
    static int doGetNativeFormat(int idx) {
        try {
            return (int) Lib.DO_GET_NATIVE_FORMAT.invokeExact(idx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code float doGetScale(int32_t idx)}. */
    static float doGetScale(int idx) {
        try {
            return (float) Lib.DO_GET_SCALE.invokeExact(idx);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * EGLCursor
     * ------------------------------------------------------------------------------------------- */

    /** {@code void doInitCursor(int32_t width, int32_t height)}. */
    static void doInitCursor(int width, int height) {
        try {
            Lib.DO_INIT_CURSOR.invokeExact(width, height);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code void doSetCursorVisibility(uint8_t val)}: true crosses as 1, false as 0. */
    static void doSetCursorVisibility(boolean visible) {
        try {
            Lib.DO_SET_CURSOR_VISIBILITY.invokeExact((byte) (visible ? 1 : 0));
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code void doSetLocation(int32_t x, int32_t y)}. */
    static void doSetLocation(int x, int y) {
        try {
            Lib.DO_SET_LOCATION.invokeExact(x, y);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code void doSetCursorImage(const uint8_t *img, int32_t length)}: the image is copied in for the call
     * and its length is the array's, as the JNI passed them.
     */
    static void doSetCursorImage(byte[] image) {
        try (Arena arena = Arena.ofConfined()) {
            Lib.DO_SET_CURSOR_IMAGE.invokeExact(arena.allocateFrom(JAVA_BYTE, image), image.length);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * The failure of the {@code Lib} holder's initialisation, seen again. The UnsatisfiedLinkError is an
     * Error, so the first use sees it unwrapped; every later use sees the NoClassDefFoundError the JVM raises
     * for a class whose initialisation failed, turned back into the UnsatisfiedLinkError callers handle.
     */
    private static UnsatisfiedLinkError unbound(NoClassDefFoundError e) {
        UnsatisfiedLinkError error = new UnsatisfiedLinkError("the EGL vendor library is not bound: "
                + e.getMessage());
        error.initCause(e);
        return error;
    }

    /** What a call through {@code Lib} can throw: linkage failures and the argument checks of this class. */
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
        return new IllegalStateException("unexpected exception from the EGL vendor library", t);
    }
}

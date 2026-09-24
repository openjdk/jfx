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

package com.sun.prism.es2;

import java.lang.foreign.MemorySegment;
import java.util.List;

/**
 * Test access to the {@code prism_es2} binding layer of {@code com.sun.prism.es2}.
 * <p>
 * Compiled into the module by the {@code compile-shims} execution of the javafx.graphics pom, which
 * compiles {@code src/main/java} and {@code src/shims/java} together, so it may call the package-private
 * {@link ES2Native} and read its package-private constants and layout; the tests in
 * {@code test.com.sun.prism.es2} reach it through the
 * {@code --add-exports javafx.graphics/com.sun.prism.es2=ALL-UNNAMED} line of {@code src/test/addExports}.
 * The restricted {@code java.lang.foreign} calls stay inside {@link ES2Native}, the only class the module
 * grants native access to; this shim just forwards the binding facts the tests assert - the symbols the
 * facade bound, the ABI version and the one struct layout - without opening a rendering path.
 */
public final class ES2NativeShim {

    private ES2NativeShim() {
    }

    /**
     * Loads the {@code prism_es2} library, binds every {@code es2_*} symbol and checks the ABI version,
     * without going through {@code ES2Pipeline}.
     *
     * @throws UnsatisfiedLinkError if the library cannot be loaded, lacks a symbol the facade binds, was
     *         denied native access, or reports another ABI version
     */
    public static void loadLibrary() {
        ES2Native.loadLibrary();
    }

    public static List<String> boundSymbols() {
        return ES2Native.boundSymbols();
    }

    public static List<String> missingSymbols() {
        return ES2Native.missingSymbols();
    }

    public static int expectedAbiVersion() {
        return ES2Native.ABI_VERSION;
    }

    public static int abiVersion() {
        return ES2Native.abiVersion();
    }

    public static long sizeofPixelFormatAttrs() {
        return ES2Native.sizeofPixelFormatAttrs();
    }

    public static long pixelFormatAttrsLayoutByteSize() {
        return ES2Native.PIXEL_FORMAT_ATTRS.byteSize();
    }

    /** {@code es2_gl_enum_count()}: the length of the library's GL enum table. */
    public static int glEnumCount() {
        return ES2Native.glEnumCount();
    }

    /** {@code es2_gl_enum(index)}: the library's GL enum at {@code index}, or -1 when out of range. */
    public static int glEnum(int index) {
        return ES2Native.glEnum(index);
    }

    /* ---------------------------------------------------------------------------------------------
     * Pure Java the port took over from the C - no library involved, so these never skip
     * ------------------------------------------------------------------------------------------- */

    /** The space-bounded token match of {@code GLFactory.c isExtensionSupported}, now in {@link ES2Native}. */
    public static boolean isExtensionSupported(String allExtensions, String extension) {
        return ES2Native.isExtensionSupported(allExtensions, extension);
    }

    /** {@code GLContext.translateScaleFactor}: a {@code GLContext.GL_*} blend factor to its GL value. */
    public static int translateScaleFactor(int scaleFactor) {
        return GLContext.translateScaleFactor(scaleFactor);
    }

    /** {@code GLContext.translatePrismToGL}: a {@code GLContext.GL_*} or {@code WRAPMODE_*} index to its GL value. */
    public static int translatePrismToGL(int value) {
        return GLContext.translatePrismToGL(value);
    }

    /** {@code GLContext.translatePixelStore}: a {@code GLContext.GL_UNPACK_*} index to its GL value. */
    public static int translatePixelStore(int pname) {
        return GLContext.translatePixelStore(pname);
    }

    /** {@code ES2Native.GL_FRONT}, the cull face the folded {@code nSetCullingMode} passes for {@code GL_FRONT}. */
    public static int glFront() {
        return ES2Native.GL_FRONT;
    }

    /** {@code ES2Native.GL_BACK}, the cull face for {@code GL_BACK} and for {@code GL_NONE} (culling disabled). */
    public static int glBack() {
        return ES2Native.GL_BACK;
    }

    /** {@code ES2Native.GL_LINE}, the polygon mode the folded {@code nSetWireframe} passes for wireframe. */
    public static int glLine() {
        return ES2Native.GL_LINE;
    }

    /** {@code ES2Native.GL_FILL}, the polygon mode for solid. */
    public static int glFill() {
        return ES2Native.GL_FILL;
    }

    /** {@code GLFactory.DESKTOP_GL2}: what every desktop factory's {@code initialize} assigns for {@code nGetIsGL2}. */
    public static boolean desktopIsGL2() {
        return GLFactory.DESKTOP_GL2;
    }

    /** {@code X11GLFactory.getAdapterCount()}: the absorbed {@code nGetAdapterCount}. */
    public static int x11AdapterCount() {
        return new X11GLFactory().getAdapterCount();
    }

    /** {@code X11GLFactory.getAdapterOrdinal(nativeScreen)}: the absorbed {@code nGetAdapterOrdinal}. */
    public static int x11AdapterOrdinal(long nativeScreen) {
        return new X11GLFactory().getAdapterOrdinal(nativeScreen);
    }

    /** {@code WinGLFactory.getAdapterCount()}: the absorbed {@code nGetAdapterCount}. */
    public static int winAdapterCount() {
        return new WinGLFactory().getAdapterCount();
    }

    /** {@code WinGLFactory.getAdapterOrdinal(nativeScreen)}: the absorbed {@code nGetAdapterOrdinal}. */
    public static int winAdapterOrdinal(long nativeScreen) {
        return new WinGLFactory().getAdapterOrdinal(nativeScreen);
    }

    /** {@code MacGLFactory.getAdapterCount()}: the absorbed {@code nGetAdapterCount}. */
    public static int macAdapterCount() {
        return new MacGLFactory().getAdapterCount();
    }

    /** {@code MacGLFactory.getAdapterOrdinal(nativeScreen)}: the absorbed {@code nGetAdapterOrdinal}. */
    public static int macAdapterOrdinal(long nativeScreen) {
        return new MacGLFactory().getAdapterOrdinal(nativeScreen);
    }

    /* ---------------------------------------------------------------------------------------------
     * es2_context_adopt and the context queries the adopt test drives
     * ------------------------------------------------------------------------------------------- */

    public static final int STR_VENDOR = ES2Native.STR_VENDOR;
    public static final int STR_RENDERER = ES2Native.STR_RENDERER;
    public static final int STR_VERSION = ES2Native.STR_VERSION;
    public static final int STR_EXTENSIONS = ES2Native.STR_EXTENSIONS;

    /** {@code ES2Native.LIBRARY_NAME}: the library the facade loads for the {@code glass.platform} of this JVM. */
    public static String libraryName() {
        return ES2Native.LIBRARY_NAME;
    }

    /** A test's {@code ES2Native.ProcLoader}. */
    public interface ProcLoader {
        long lookup(long handle, String name);
    }

    /** {@code ES2Native.contextAdopt}; a null loader is passed through as NULL. */
    public static long contextAdopt(long glHandle, ProcLoader loader) {
        return ES2Native.contextAdopt(glHandle, loader == null ? null : loader::lookup);
    }

    /** Whether the loader stub of the last adopt is still alive: false once the call has returned. */
    public static boolean loaderStubAlive() {
        MemorySegment stub = ES2Native.lastLoaderStub;
        return stub != null && stub.scope().isAlive();
    }

    public static String contextGetString(long ctx, int kind) {
        return ES2Native.contextGetString(ctx, kind);
    }

    public static long contextGetProcAddress(long ctx, String name) {
        return ES2Native.contextGetProcAddress(ctx, name);
    }

    public static void contextRelease(long ctx) {
        ES2Native.contextRelease(ctx);
    }
}

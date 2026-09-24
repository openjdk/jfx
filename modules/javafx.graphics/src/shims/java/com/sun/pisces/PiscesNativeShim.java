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

package com.sun.pisces;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.List;

/**
 * Test access to the {@code prism_sw} binding layer of {@code com.sun.pisces}.
 * <p>
 * Compiled into the module by the {@code compile-shims} execution of the javafx.graphics pom, so it
 * may call the package-private {@link PiscesNative}; the tests in {@code test.com.sun.pisces} reach it
 * through the {@code --add-exports javafx.graphics/com.sun.pisces=ALL-UNNAMED} line of
 * {@code src/test/addExports}. Every downcall a test makes goes through here, so the restricted
 * {@code java.lang.foreign} calls stay inside the module that {@code --enable-native-access} names.
 */
public final class PiscesNativeShim {

    private PiscesNativeShim() {
    }

    /**
     * Loads the {@code prism_sw} library, binds every {@code psw_*} symbol and checks the ABI version,
     * so that a test can drive {@link PiscesRenderer} without going through
     * {@code com.sun.prism.sw.SWPipeline}.
     *
     * @throws UnsatisfiedLinkError if the library cannot be loaded, lacks a symbol or reports another
     *         ABI version
     */
    public static void loadLibrary() {
        PiscesNative.ensureLoaded();
    }

    public static List<String> boundSymbols() {
        return PiscesNative.boundSymbols();
    }

    public static int expectedAbiVersion() {
        return PiscesNative.ABI_VERSION;
    }

    public static int abiVersion() {
        return PiscesNative.abiVersion();
    }

    public static int sizeofTransform6() {
        return PiscesNative.sizeofTransform6();
    }

    public static long transform6LayoutByteSize() {
        return PiscesNative.TRANSFORM6_LAYOUT.byteSize();
    }

    public static long transform6LayoutOffset(String field) {
        return PiscesNative.TRANSFORM6_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    public static int transform6Ints() {
        return PiscesNative.TRANSFORM6_INTS;
    }

    /** {@code Transform6.fill} into a fresh scratch array of {@link #transform6Ints()} ints. */
    public static int[] transform6ToInts(Transform6 transform) {
        int[] scratch = new int[PiscesNative.TRANSFORM6_INTS];
        transform.fill(scratch);
        return scratch;
    }

    public static int constantCount() {
        return PiscesNative.CONSTANT_COUNT;
    }

    public static int constant(int index) {
        return PiscesNative.constant(index);
    }

    public static MemorySegment surfaceCreate(int imageType, int width, int height) {
        return PiscesNative.surfaceCreate(imageType, width, height);
    }

    public static void surfaceDispose(MemorySegment surface) {
        PiscesNative.surfaceDispose(surface);
    }

    public static MemorySegment rendererCreate(MemorySegment surface) {
        return PiscesNative.rendererCreate(surface);
    }

    public static void rendererDispose(MemorySegment renderer) {
        PiscesNative.rendererDispose(renderer);
    }

    public static void rendererSetClip(MemorySegment renderer, int minX, int minY, int width, int height) {
        PiscesNative.rendererSetClip(renderer, minX, minY, width, height);
    }

    public static void rendererFillRect(MemorySegment renderer, int[] pixels, int x, int y, int w, int h) {
        PiscesNative.rendererFillRect(renderer, pixels, x, y, w, h);
    }

    public static String oomMessage() {
        return PiscesNative.OOM_MESSAGE;
    }

    public static String stateMessage() {
        return PiscesNative.STATE_MESSAGE;
    }

    public static String argMessage() {
        return PiscesNative.ARG_MESSAGE;
    }
}

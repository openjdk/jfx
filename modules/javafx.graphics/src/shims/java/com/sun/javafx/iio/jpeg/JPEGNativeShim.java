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

package com.sun.javafx.iio.jpeg;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.List;

/**
 * Test access to the {@code javafx_iio} binding layer of {@code com.sun.javafx.iio.jpeg}.
 * <p>
 * Compiled into the module by the {@code compile-shims} execution of the javafx.graphics pom, so it
 * may call the package-private {@link JPEGNative}; the tests in {@code test.com.sun.javafx.iio.jpeg}
 * reach it through the {@code --add-exports javafx.graphics/com.sun.javafx.iio.jpeg=ALL-UNNAMED} line
 * of {@code src/test/addExports}. Every downcall a test makes goes through here, so the restricted
 * {@code java.lang.foreign} calls stay inside the module that {@code --enable-native-access} names.
 */
public final class JPEGNativeShim {

    private JPEGNativeShim() {
    }

    /**
     * Loads the {@code javafx_iio} library, binds every {@code iio_*} symbol and checks the ABI version.
     *
     * @throws UnsatisfiedLinkError if the library cannot be loaded, lacks a symbol or reports another
     *         ABI version
     */
    public static void loadLibrary() {
        JPEGNative.ensureLoaded();
    }

    public static List<String> boundSymbols() {
        return JPEGNative.boundSymbols();
    }

    public static int expectedAbiVersion() {
        return JPEGNative.ABI_VERSION;
    }

    public static int abiVersion() {
        return JPEGNative.abiVersion();
    }

    public static long sizeofImageInfo() {
        return JPEGNative.sizeofImageInfo();
    }

    public static long sizeofCallbacks() {
        return JPEGNative.sizeofCallbacks();
    }

    public static long imageInfoLayoutByteSize() {
        return JPEGNative.IMAGE_INFO_LAYOUT.byteSize();
    }

    public static long imageInfoLayoutOffset(String field) {
        return JPEGNative.IMAGE_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    public static long callbacksLayoutByteSize() {
        return JPEGNative.CALLBACKS_LAYOUT.byteSize();
    }

    public static long callbacksLayoutOffset(String field) {
        return JPEGNative.CALLBACKS_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    /** {@code iio_get_icc_profile_length(decoder)}, raw: -1 for a {@code NULL} handle. */
    public static int iccProfileLength(MemorySegment decoder) {
        return JPEGNative.iccProfileLength(decoder);
    }

    /** {@code iio_dispose(decoder)}; {@code NULL} is ignored. */
    public static void dispose(MemorySegment decoder) {
        JPEGNative.dispose(decoder);
    }

    /** The number of loaders created and not yet disposed. */
    public static int registrySize() {
        return JPEGNative.registrySize();
    }

    /** The address of the process-wide {@code IioJpegCallbacks} table. */
    public static long callbackTableAddress() {
        return JPEGNative.callbackTableAddress();
    }

    /** The stub address in slot {@code field} of the process-wide {@code IioJpegCallbacks} table. */
    public static long callbackSlotAddress(String field) {
        return JPEGNative.callbackSlotAddress(field);
    }
}

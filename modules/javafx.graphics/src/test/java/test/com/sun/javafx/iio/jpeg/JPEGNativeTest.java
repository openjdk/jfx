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

package test.com.sun.javafx.iio.jpeg;

import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.jpeg.JPEGImageLoaderFactory;
import com.sun.javafx.iio.jpeg.JPEGNativeShim;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.iio.JpegNatives;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binding tests for {@code com.sun.javafx.iio.jpeg.JPEGNative}, the FFM facade over the {@code iio_*}
 * C ABI of {@code javafx_iio} ({@code src/main/native-iio/iio_api.h}): symbol resolution, the ABI
 * guard, the two layouts against the C compiler's {@code sizeof} and the header's declaration order,
 * the {@code NULL}-handle contract of the two cold functions, the one callback table of the process,
 * and the registry that maps {@code void* user} back to a loader.
 * <p>
 * What the decoder produces through this binding - pixels, callback order, exception identity - is
 * pinned by the parity tests in {@code test.com.sun.javafx.iio} against goldens captured from the JNI
 * build, so nothing here decodes for its own sake.
 */
public class JPEGNativeTest {

    /** Every function {@code iio_api.h} exports, in header order. */
    static final List<String> EXPORTED_SYMBOLS = List.of(
            "iio_abi_version", "iio_sizeof_image_info", "iio_sizeof_callbacks",
            "iio_create", "iio_get_icc_profile_length", "iio_get_icc_profile",
            "iio_start_decompression", "iio_decompress", "iio_dispose");

    /** {@code IioImageInfo} in declaration order: five {@code int32_t}, no padding. */
    static final String[] IMAGE_INFO_FIELDS = {
        "width", "height", "jpeg_color_space", "out_color_space", "num_components",
    };

    /** {@code IioJpegCallbacks} in declaration order: four function pointers. */
    static final String[] CALLBACK_SLOTS = {
        "read", "skip", "emit_warning", "update_progress",
    };

    /** A corpus member next to this test on the classpath; the smallest valid one. */
    private static final String FIXTURE = "gray-32x32.jpg";

    @BeforeAll
    static void requireNatives() {
        JpegNatives.require();
        JPEGNativeShim.loadLibrary();
    }

    @Test
    public void facadeBindsEveryExportedSymbolAndNothingElse() {
        List<String> bound = JPEGNativeShim.boundSymbols();
        assertEquals(EXPORTED_SYMBOLS.size(), bound.size(), "bound symbols: " + bound);
        for (String name : EXPORTED_SYMBOLS) {
            assertTrue(bound.contains(name), "facade does not bind " + name);
        }
        assertEquals("iio_abi_version", bound.get(0), "the ABI guard must be bound before anything else");
    }

    @Test
    public void everyExportedSymbolResolvesInTheLoadedLibrary() {
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        for (String name : EXPORTED_SYMBOLS) {
            assertTrue(lookup.find(name).isPresent(), "javafx_iio does not export " + name);
        }
    }

    @Test
    public void abiVersionIsTheOneTheFacadeWasWrittenFor() {
        assertEquals(2, JPEGNativeShim.expectedAbiVersion());
        assertEquals(JPEGNativeShim.expectedAbiVersion(), JPEGNativeShim.abiVersion());
    }

    @Test
    public void imageInfoLayoutMatchesTheCStruct() {
        assertEquals(20L, JPEGNativeShim.sizeofImageInfo(), "sizeof(IioImageInfo)");
        assertEquals(JPEGNativeShim.sizeofImageInfo(), JPEGNativeShim.imageInfoLayoutByteSize());
        for (int i = 0; i < IMAGE_INFO_FIELDS.length; i++) {
            assertEquals(4L * i, JPEGNativeShim.imageInfoLayoutOffset(IMAGE_INFO_FIELDS[i]),
                    "offset of " + IMAGE_INFO_FIELDS[i]);
        }
    }

    @Test
    public void callbacksLayoutMatchesTheCStruct() {
        assertEquals(JPEGNativeShim.sizeofCallbacks(), JPEGNativeShim.callbacksLayoutByteSize(),
                "sizeof(IioJpegCallbacks)");
        long pointer = ValueLayout.ADDRESS.byteSize();
        if (pointer == 8L) {
            assertEquals(32L, JPEGNativeShim.sizeofCallbacks(), "sizeof(IioJpegCallbacks) on a 64-bit target");
        }
        for (int i = 0; i < CALLBACK_SLOTS.length; i++) {
            assertEquals(i * pointer, JPEGNativeShim.callbacksLayoutOffset(CALLBACK_SLOTS[i]),
                    "offset of " + CALLBACK_SLOTS[i]);
        }
    }

    /**
     * The callback table is built once, in the class initializer, and every loader hands the same one
     * to {@code iio_create}: the stubs carry no per-loader state, so nothing is generated per image
     * and nothing is closed per dispose. Every slot holds a stub, none is {@code NULL}.
     */
    @Test
    public void theCallbackTableIsBuiltOnceForTheProcess() throws IOException {
        long table = JPEGNativeShim.callbackTableAddress();
        assertNotEquals(0L, table, "the callback table must exist once the facade is initialised");
        for (String slot : CALLBACK_SLOTS) {
            assertNotEquals(0L, JPEGNativeShim.callbackSlotAddress(slot), "slot " + slot + " holds no stub");
        }
        byte[] jpeg = fixture();
        for (int i = 0; i < 4; i++) {
            ImageLoader loader = JPEGImageLoaderFactory.getInstance().createImageLoader(new ByteArrayInputStream(jpeg));
            try {
                assertEquals(table, JPEGNativeShim.callbackTableAddress(),
                        "creating a loader must not replace the process-wide table");
            } finally {
                loader.dispose();
            }
        }
        assertEquals(table, JPEGNativeShim.callbackTableAddress(), "disposing loaders must not free the table");
    }

    @Test
    public void disposingANullHandleIsIgnored() {
        assertDoesNotThrow(() -> JPEGNativeShim.dispose(MemorySegment.NULL));
    }

    @Test
    public void iccProfileLengthOfANullHandleIsMinusOne() {
        assertEquals(-1, JPEGNativeShim.iccProfileLength(MemorySegment.NULL));
    }

    /**
     * Every loader registers itself for the {@code void* user} round trip and must unregister on
     * dispose, or the map - and the loader and its stream behind it - grows for the life of the JVM.
     * Measured as a delta: another test in this fork may hold a loader of its own.
     */
    @Test
    public void aDecodedAndDisposedLoaderLeavesNoRegistryEntryBehind() throws IOException {
        byte[] jpeg = fixture();
        int before = JPEGNativeShim.registrySize();
        for (int i = 0; i < 16; i++) {
            ImageLoader loader = JPEGImageLoaderFactory.getInstance()
                    .createImageLoader(new ByteArrayInputStream(jpeg));
            try {
                assertNotNull(loader.load(0, 0, 0, true, true, 1, 1), "the fixture must decode");
            } finally {
                loader.dispose();
            }
        }
        assertEquals(before, JPEGNativeShim.registrySize(), "registry entries leaked by decoded loaders");
    }

    /**
     * A loader whose constructor fails - here on an empty stream, which libjpeg rejects while reading
     * the header - never had a decoder, but it did have a callback table and a registry entry, and
     * both must be released on the way out.
     */
    @Test
    public void aLoaderThatFailsToConstructLeavesNoRegistryEntryBehind() {
        int before = JPEGNativeShim.registrySize();
        for (int i = 0; i < 16; i++) {
            assertThrows(IOException.class, () -> JPEGImageLoaderFactory.getInstance()
                    .createImageLoader(new ByteArrayInputStream(new byte[0])));
        }
        assertEquals(before, JPEGNativeShim.registrySize(), "registry entries leaked by failed loaders");
    }

    /** A loader disposed without ever decoding releases its entry as well. */
    @Test
    public void aLoaderDisposedWithoutDecodingLeavesNoRegistryEntryBehind() throws IOException {
        byte[] jpeg = fixture();
        int before = JPEGNativeShim.registrySize();
        ImageLoader loader = JPEGImageLoaderFactory.getInstance().createImageLoader(new ByteArrayInputStream(jpeg));
        assertEquals(before + 1, JPEGNativeShim.registrySize(), "a live loader must be registered");
        loader.dispose();
        loader.dispose();
        assertEquals(before, JPEGNativeShim.registrySize(), "dispose must unregister exactly once");
    }

    private static byte[] fixture() throws IOException {
        try (InputStream in = JPEGNativeTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull(in, "corpus member " + FIXTURE + " is not on the classpath next to this test");
            return in.readAllBytes();
        }
    }
}

/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.image;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.util.Callback;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class PixelBufferTest {
    private static final int WIDTH = 10;
    private static final int HEIGHT = 15;

    private static final PixelFormat<ByteBuffer> BYTE_BGRA_PRE_PF = PixelFormat.getByteBgraPreInstance();
    private static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4);

    private static final PixelFormat<IntBuffer> INT_ARGB_PRE_PF = PixelFormat.getIntArgbPreInstance();
    private static final IntBuffer INT_BUFFER = IntBuffer.allocate(WIDTH * HEIGHT);

    @Test
    public void testCreatePixelBufferWithByteBGRAPrePF() {
        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(WIDTH, HEIGHT, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
        assertEquals(WIDTH, pixelBuffer.getWidth());
        assertEquals(HEIGHT, pixelBuffer.getHeight());
        assertSame(BYTE_BUFFER, pixelBuffer.getBuffer());
        assertSame(BYTE_BGRA_PRE_PF, pixelBuffer.getPixelFormat());
    }

    @Test
    public void testCreatePixelBufferWithLessCapacityBuffer() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(WIDTH, HEIGHT + 1, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
        });
    }

    @Test
    public void testCreatePixelBufferDimensionsOverflow() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(0xFFFFF, 0xFFFFF, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
        });
    }

    @Test
    public void testCreatePixelBufferWx4xHIsMaxint() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(0x4000, 0x8000, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
        });
    }

    @Test
    public void testCreatePixelBufferWidth0() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(0, HEIGHT, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
        });
    }

    @Test
    public void testCreatePixelBufferHeight0() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(WIDTH, 0, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
        });
    }

    @Test
    public void testCreatePixelBufferBufferNull() {
        assertThrows(NullPointerException.class, () -> {
            new PixelBuffer<>(WIDTH, HEIGHT, null, BYTE_BGRA_PRE_PF);
        });
    }

    @Test
    public void testCreatePixeBufferPixelFormatNull() {
        assertThrows(NullPointerException.class, () -> {
            new PixelBuffer<>(WIDTH, HEIGHT, BYTE_BUFFER, null);
        });
    }

    @Test
    public void testUpdatePixelBufferPartialBufferUpdate() {
        // This test verifies that a correct set of calls does not cause any exception
        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(WIDTH, HEIGHT, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
        Callback<PixelBuffer<ByteBuffer>, Rectangle2D> callback = pixBuf -> {
            // Assuming this Callback modifies the buffer.
            return new Rectangle2D(1, 1, WIDTH - 1, HEIGHT - 1);
        };
        pixelBuffer.updateBuffer(callback);
    }

    @Test
    public void testUpdatePixelBufferEmptyBufferUpdate() {
        // This test verifies that an empty dirty region does not cause any exception
        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(WIDTH, HEIGHT, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
        Callback<PixelBuffer<ByteBuffer>, Rectangle2D> callback = pixBuf -> {
            // Assuming no pixels were modified.
            return Rectangle2D.EMPTY;
        };
        pixelBuffer.updateBuffer(callback);
    }

    @Test
    public void testUpdatePixelBufferCallbackNull() {
        assertThrows(NullPointerException.class, () -> {
            PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(WIDTH, HEIGHT, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
            pixelBuffer.updateBuffer(null);
        });
    }

    @Test
    public void testCreatePixelBufferIntArgbPrePF() {
        PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer<>(WIDTH, HEIGHT, INT_BUFFER, INT_ARGB_PRE_PF);
        assertEquals(WIDTH, pixelBuffer.getWidth());
        assertEquals(HEIGHT, pixelBuffer.getHeight());
        assertSame(INT_BUFFER, pixelBuffer.getBuffer());
        assertSame(INT_ARGB_PRE_PF, pixelBuffer.getPixelFormat());
    }

    @Test
    public void testCreatePixelBufferIntArgbPreLessCapacityBuffer() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(WIDTH, HEIGHT + 1, INT_BUFFER, INT_ARGB_PRE_PF);
        });
    }

    @Test
    public void testCreatePixelBufferIntArgbPreDimensionsOverflow() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(0xFFFFF, 0xFFFFF, INT_BUFFER, INT_ARGB_PRE_PF);
        });
    }

    @Test
    public void testCreatePixelBufferIntArgbPreWxHIsMaxint() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(0x8000, 0x10000, INT_BUFFER, INT_ARGB_PRE_PF);
        });
    }

    @Test
    public void testCreatePixelBufferIntArgbPF() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(WIDTH, HEIGHT, INT_BUFFER, PixelFormat.getIntArgbInstance());
        });
    }

    @Test
    public void testCreatePixelBufferByteBgraPF() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(WIDTH, HEIGHT, BYTE_BUFFER, PixelFormat.getByteBgraInstance());
        });
    }

    @Test
    public void testCreatePixelBufferByteRgbPF() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer<>(WIDTH, HEIGHT, BYTE_BUFFER, PixelFormat.getByteRgbInstance());
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePixelBufferIntBufferByteBgraPrePF() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer(WIDTH, HEIGHT, INT_BUFFER, BYTE_BGRA_PRE_PF);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePixelBufferByteBufferIntArgbPrePF() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PixelBuffer(WIDTH, HEIGHT, BYTE_BUFFER, INT_ARGB_PRE_PF);
        });
    }

    @Test
    public void testCreateWritableImageUsingPB() {
        // Test should complete without throwing any exception
        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(WIDTH, HEIGHT, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
        new WritableImage(pixelBuffer);
    }

    @Test
    public void testWritableImageGetPixelWriter() {
        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(WIDTH, HEIGHT, BYTE_BUFFER, BYTE_BGRA_PRE_PF);
        WritableImage image = new WritableImage(pixelBuffer);
        assertThrows(UnsupportedOperationException.class, () -> {
            image.getPixelWriter();
        });
    }
}

/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.image;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * A {@link PixelFormat} object representing a pixel format that can store
 * full colors and so can be used as a destination format to write pixel
 * data from an arbitrary image.
 * @since JavaFX 2.2
 */
public abstract class WritablePixelFormat<T extends Buffer>
    extends PixelFormat<T>
{
    WritablePixelFormat(Type type) {
        super(type);
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    /**
     * Stores the appropriate pixel data that represents the specified
     * 32-bit integer representation of a color in the buffer
     * at the specified coordinates.
     * The 32-bit integer will contain the 4 color components in separate
     * 8-bit fields in ARGB order from the most significant byte to the least
     * significant byte.
     * The buffer should be positioned to the start of the pixel data such
     * that {@code buf.get(0)} would return the pixel information for the
     * pixel at coordinates {@code (0, 0)}.
     * The {@code scanlineStride} parameter defines the distance from the pixel
     * data at the start of one row to the pixel data at the start of the
     * immediately following row at the next higher Y coordinate.  Usually,
     * {@code scanlineStride} is the same as the width of the image multiplied
     * by the number of data elements per pixel (1 for the case of the
     * integer and indexed formats, or 3 or 4 in the case of the byte
     * formats), but some images may have further padding between rows for
     * alignment or other purposes.
     * <p>
     * Color components can be composed into an integer using the following
     * sample code:
     * <pre>
     *     int argb = ((alpha &lt;&lt; 24) |
     *                 (red   &lt;&lt; 16) |
     *                 (green &lt;&lt;  8) |
     *                 (blue       );
     * </pre>
     *
     * @param buf the buffer of pixel data
     * @param x the X coordinate of the pixel to be read
     * @param y the Y coordinate of the pixel to be read
     * @param scanlineStride the number of buffer elements between the
     *        start of adjacent pixel rows in the buffer
     * @param argb a 32-bit value with the color to be stored in the pixel
     *        in a format similar to the {@code Type.INT_ARGB} pixel format
     */
    public abstract void setArgb(T buf, int x, int y, int scanlineStride,
                                 int argb);

    static class IntArgb extends WritablePixelFormat<IntBuffer> {
        static final IntArgb INSTANCE = new IntArgb();

        private IntArgb() {
            super(Type.INT_ARGB);
        }

        @Override
        public boolean isPremultiplied() {
            return false;
        }

        @Override
        public int getArgb(IntBuffer buf, int x, int y, int scanlineStride) {
            return buf.get(y * scanlineStride + x);
        }

        @Override
        public void setArgb(IntBuffer buf, int x, int y, int scanlineStride,
                            int argb)
        {
            buf.put(y * scanlineStride + x, argb);
        }
    }

    static class IntArgbPre extends WritablePixelFormat<IntBuffer> {
        static final IntArgbPre INSTANCE = new IntArgbPre();

        private IntArgbPre() {
            super(Type.INT_ARGB_PRE);
        }

        @Override
        public boolean isPremultiplied() {
            return true;
        }

        @Override
        public int getArgb(IntBuffer buf, int x, int y, int scanlineStride) {
            return PretoNonPre(buf.get(y * scanlineStride + x));
        }

        @Override
        public void setArgb(IntBuffer buf, int x, int y, int scanlineStride,
                            int argb)
        {
            buf.put(y * scanlineStride + x, NonPretoPre(argb));
        }
    }

    static class ByteBgra extends WritablePixelFormat<ByteBuffer> {
        static final ByteBgra INSTANCE = new ByteBgra();

        private ByteBgra() {
            super(Type.BYTE_BGRA);
        }

        @Override
        public boolean isPremultiplied() {
            return false;
        }

        @Override
        public int getArgb(ByteBuffer buf, int x, int y, int scanlineStride) {
            int index = y * scanlineStride + x * 4;
            int b = buf.get(index    ) & 0xff;
            int g = buf.get(index + 1) & 0xff;
            int r = buf.get(index + 2) & 0xff;
            int a = buf.get(index + 3) & 0xff;
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        @Override
        public void setArgb(ByteBuffer buf, int x, int y, int scanlineStride,
                            int argb)
        {
            int index = y * scanlineStride + x * 4;
            buf.put(index,     (byte) (argb      ));
            buf.put(index + 1, (byte) (argb >>  8));
            buf.put(index + 2, (byte) (argb >> 16));
            buf.put(index + 3, (byte) (argb >> 24));
        }
    }

    static class ByteBgraPre extends WritablePixelFormat<ByteBuffer> {
        static final ByteBgraPre INSTANCE = new ByteBgraPre();

        private ByteBgraPre() {
            super(Type.BYTE_BGRA_PRE);
        }

        @Override
        public boolean isPremultiplied() {
            return true;
        }

        @Override
        public int getArgb(ByteBuffer buf, int x, int y, int scanlineStride) {
            int index = y * scanlineStride + x * 4;
            int b = buf.get(index    ) & 0xff;
            int g = buf.get(index + 1) & 0xff;
            int r = buf.get(index + 2) & 0xff;
            int a = buf.get(index + 3) & 0xff;
            if (a > 0x00 && a < 0xff) {
                int halfa = a >> 1;
                r = (r >= a) ? 0xff : (r * 0xff + halfa) / a;
                g = (g >= a) ? 0xff : (g * 0xff + halfa) / a;
                b = (b >= a) ? 0xff : (b * 0xff + halfa) / a;
            }
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        @Override
        public void setArgb(ByteBuffer buf, int x, int y, int scanlineStride,
                            int argb)
        {
            int index = y * scanlineStride + x * 4;
            int a = (argb >>> 24);
            int r, g, b;
            if (a > 0x00) {
                r = (argb >> 16) & 0xff;
                g = (argb >>  8) & 0xff;
                b = (argb      ) & 0xff;
                if (a < 0xff) {
                    r = (r * a + 127) / 0xff;
                    g = (g * a + 127) / 0xff;
                    b = (b * a + 127) / 0xff;
                }
            } else {
                a = r = g = b = 0;
            }
            buf.put(index,     (byte) b);
            buf.put(index + 1, (byte) g);
            buf.put(index + 2, (byte) r);
            buf.put(index + 3, (byte) a);
        }
    }
}

/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;

/**
 * A {@code PixelFormat} object defines the layout of data for a pixel of
 * a given format.
 * @since JavaFX 2.2
 */
public abstract class PixelFormat<T extends Buffer> {
    /**
     * An enum describing the in-array storage format of a single pixel
     * managed by a {@link PixelFormat}.
     *
     * @since JavaFX 2.2
     */
    public enum Type {
        /**
         * The pixels are stored in 32-bit integers with the premultiplied
         * components stored in order, from MSb to LSb:
         * alpha, red, green, blue.
         */
        INT_ARGB_PRE,
        /**
         * The pixels are stored in 32-bit integers with the non-premultiplied
         * components stored in order, from MSb to LSb:
         * alpha, red, green, blue.
         */
        INT_ARGB,
        /**
         * The pixels are stored in adjacent bytes with the premultiplied
         * components stored in order of increasing index:
         * blue, green, red, alpha.
         */
        BYTE_BGRA_PRE,
        /**
         * The pixels are stored in adjacent bytes with the non-premultiplied
         * components stored in order of increasing index:
         * blue, green, red, alpha.
         */
        BYTE_BGRA,
        /**
         * The opaque pixels are stored in adjacent bytes with the color
         * components stored in order of increasing index:
         * red, green, blue.
         */
        BYTE_RGB,
        /**
         * The pixel colors are referenced by byte indices stored in the
         * pixel array, with the byte interpreted as an unsigned index into
         * a list of colors provided by the {@code PixelFormat} object.
         */
        BYTE_INDEXED,
    }

    private Type type;

    PixelFormat(Type type) {
        this.type = type;
    }

    /**
     * Returns a {@code WritablePixelFormat} instance describing a pixel
     * layout with the pixels stored in 32-bit integers with the
     * <b>non-premultiplied</b> components stored in order, from MSb to LSb:
     * alpha, red, green, blue.
     * <p>
     * Pixels in this format can be decoded using the following sample code:
     * <pre>{@code
     *     int pixel = array[rowstart + x];
     *
     *     int alpha = ((pixel >> 24) & 0xff);
     *     int red   = ((pixel >> 16) & 0xff);
     *     int green = ((pixel >>  8) & 0xff);
     *     int blue  = ((pixel >>   ) & 0xff);
     * }</pre>
     *
     * @return a {@code WritabelPixelFormat<IntBuffer>} describing the
     *         indicated pixel format
     */
    public static WritablePixelFormat<IntBuffer> getIntArgbInstance() {
        return WritablePixelFormat.IntArgb.INSTANCE;
    }

    /**
     * Returns a {@code WritablePixelFormat} instance describing a pixel
     * layout with the pixels stored in 32-bit integers with the
     * <b>premultiplied</b> components stored in order, from MSb to LSb:
     * alpha, red, green, blue.
     * <p>
     * Pixels in this format can be decoded using the following sample code:
     * <pre>{@code
     *     int pixel = array[rowstart + x];
     *
     *     int alpha = ((pixel >> 24) & 0xff);
     *     int red   = ((pixel >> 16) & 0xff);
     *     int green = ((pixel >>  8) & 0xff);
     *     int blue  = ((pixel >>   ) & 0xff);
     * }</pre>
     *
     * @return a {@code WritabelPixelFormat<IntBuffer>} describing the
     *         indicated pixel format
     */
    public static WritablePixelFormat<IntBuffer> getIntArgbPreInstance() {
        return WritablePixelFormat.IntArgbPre.INSTANCE;
    }

    /**
     * Returns a {@code WritablePixelFormat} instance describing a pixel
     * layout with the pixels stored in adjacent bytes with the
     * <b>non-premultiplied</b> components stored in order of increasing index:
     * blue, green, red, alpha.
     * <p>
     * Pixels in this format can be decoded using the following sample code:
     * <pre>{@code
     *     int i = rowstart + x * 4;
     *
     *     int blue  = (array[i+0] & 0xff);
     *     int green = (array[i+1] & 0xff);
     *     int red   = (array[i+2] & 0xff);
     *     int alpha = (array[i+3] & 0xff);
     * }</pre>
     *
     * @return a {@code WritablePixelFormat<ByteBuffer>} describing the
     *         indicated pixel format
     */
    public static WritablePixelFormat<ByteBuffer> getByteBgraInstance() {
        return WritablePixelFormat.ByteBgra.INSTANCE;
    }

    /**
     * Returns a {@code WritablePixelFormat} instance describing a pixel
     * layout with the pixels stored in adjacent bytes with the
     * <b>premultiplied</b> components stored in order of increasing index:
     * blue, green, red, alpha.
     * <p>
     * Pixels in this format can be decoded using the following sample code:
     * <pre>{@code
     *     int i = rowstart + x * 4;
     *
     *     int blue  = (array[i+0] & 0xff);
     *     int green = (array[i+1] & 0xff);
     *     int red   = (array[i+2] & 0xff);
     *     int alpha = (array[i+3] & 0xff);
     * }</pre>
     *
     * @return a {@code WritablePixelFormat<ByteBuffer>} describing the
     *         indicated pixel format
     */
    public static WritablePixelFormat<ByteBuffer> getByteBgraPreInstance() {
        return WritablePixelFormat.ByteBgraPre.INSTANCE;
    }

    /**
     * Returns a {@code PixelFormat} instance describing a pixel
     * layout with the pixels stored in adjacent bytes with the
     * color components stored in order of increasing index:
     * red, green, blue.
     * <p>
     * Pixels in this format can be decoded using the following sample code:
     * <pre>{@code
     *     int i = rowstart + x * 3;
     *
     *     int red   = (array[i+0] & 0xff);
     *     int green = (array[i+1] & 0xff);
     *     int blue  = (array[i+2] & 0xff);
     * }</pre>
     *
     * @return a {@code PixelFormat<ByteBuffer>} describing the
     *         indicated pixel format
     */
    public static PixelFormat<ByteBuffer> getByteRgbInstance() {
        return ByteRgb.instance;
    }

    /**
     * Creates a {@code PixelFormat} instance describing a pixel layout
     * with the pixels stored as single bytes representing an index
     * into the specified lookup table of <b>premultiplied</b> color
     * values in the {@link Type#INT_ARGB_PRE INT_ARGB_PRE} format.
     * <p>
     * Pixels in this format can be decoded using the following sample code:
     * <pre>{@code
     *     int pixel = array[rowstart + x] & 0xff;
     *     int argb  = colors[pixel];
     *
     *     int alpha = ((argb >> 24) & 0xff);
     *     int red   = ((argb >> 16) & 0xff);
     *     int green = ((argb >>  8) & 0xff);
     *     int blue  = ((argb      ) & 0xff);
     * }</pre>
     *
     * @param colors an {@code int[]} array of 32-bit color values in
     *               the {@link Type#INT_ARGB_PRE INT_ARGB_PRE} format
     * @return a {@code PixelFormat<ByteBuffer>} describing the indicated
     *         pixel format with the specified list of premultiplied colors
     */
    public static PixelFormat<ByteBuffer>
        createByteIndexedPremultipliedInstance(int colors[])
    {
        return IndexedPixelFormat.createByte(colors, true);
    }

    /**
     * Creates a {@code PixelFormat} instance describing a pixel layout
     * with the pixels stored as single bytes representing an index
     * into the specified lookup table of <b>non-premultiplied</b> color
     * values in the {@link Type#INT_ARGB INT_ARGB} format.
     * <p>
     * Pixels in this format can be decoded using the following sample code:
     * <pre>{@code
     *     int pixel = array[rowstart + x] & 0xff;
     *     int argb  = colors[pixel];
     *
     *     int alpha = ((argb >> 24) & 0xff);
     *     int red   = ((argb >> 16) & 0xff);
     *     int green = ((argb >>  8) & 0xff);
     *     int blue  = ((argb      ) & 0xff);
     * }</pre>
     *
     * @param colors an {@code int[]} array of 32-bit color values in
     *               the {@link Type#INT_ARGB INT_ARGB} format
     * @return a {@code PixelFormat<ByteBuffer>} describing the indicated
     *         pixel format with the specified list of non-premultiplied colors
     */
    public static PixelFormat<ByteBuffer>
        createByteIndexedInstance(int colors[])
    {
        return IndexedPixelFormat.createByte(colors, false);
    }

    /**
     * Returns the enum representing the storage format of the pixels
     * managed by this {@code PixelFormat} object.
     *
     * @return the {@code Type} enum of the pixels
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns true iff this {@code PixelFormat} object can convert
     * color information into a pixel representation.
     *
     * @return true iff this {@code PixelFormat} can convert colors to
     *         pixel data
     */
    public abstract boolean isWritable();

    /**
     * Returns true iff the color components decoded (or encoded) by this
     * format are pre-multiplied by the alpha component for more efficient
     * blending calculations.
     *
     * @return true iff the managed color components are premultiplied
     *         by alpha
     */
    public abstract boolean isPremultiplied();

    static int NonPretoPre(int nonpre) {
        int a = nonpre >>> 24;
        if (a == 0xff) return nonpre;
        if (a == 0x00) return 0;
        int r = (nonpre >> 16) & 0xff;
        int g = (nonpre >>  8) & 0xff;
        int b = (nonpre      ) & 0xff;
        r = (r * a + 127) / 0xff;
        g = (g * a + 127) / 0xff;
        b = (b * a + 127) / 0xff;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static int PretoNonPre(int pre) {
        int a = pre >>> 24;
        if (a == 0xff || a == 0x00) return pre;
        int r = (pre >> 16) & 0xff;
        int g = (pre >>  8) & 0xff;
        int b = (pre      ) & 0xff;
        int halfa = a >> 1;
        r = (r >= a) ? 0xff : (r * 0xff + halfa) / a;
        g = (g >= a) ? 0xff : (g * 0xff + halfa) / a;
        b = (b >= a) ? 0xff : (b * 0xff + halfa) / a;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Reads pixel data from the buffer at the specified coordinates and
     * converts it to a 32-bit integer representation of the color in the
     * {@link Type#INT_ARGB INT_ARGB} format.
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
     * The color components can be extracted from the returned integer using
     * the following sample code:
     * <pre>
     *     int alpha = ((retval &gt;&gt; 24) &amp; 0xff);
     *     int red   = ((retval &gt;&gt; 16) &amp; 0xff);
     *     int green = ((retval &gt;&gt;  8) &amp; 0xff);
     *     int blue  = ((retval      ) &amp; 0xff);
     * </pre>
     *
     * @param buf the buffer of pixel data
     * @param x the X coordinate of the pixel to be read
     * @param y the Y coordinate of the pixel to be read
     * @param scanlineStride the number of buffer elements between the
     *        start of adjacent pixel rows in the buffer
     * @return a 32-bit value with the color of the pixel in a format
     *         similar to the {@link Type#INT_ARGB INT_ARGB} pixel format
     */
    public abstract int getArgb(T buf, int x, int y, int scanlineStride);

    static class ByteRgb extends PixelFormat<ByteBuffer> {
        static final ByteRgb instance = new ByteRgb();

        private ByteRgb() {
            super(Type.BYTE_RGB);
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        public boolean isPremultiplied() {
            return false;
        }

        @Override
        public int getArgb(ByteBuffer buf, int x, int y, int scanlineStride) {
            int index = y * scanlineStride + x * 3;
            int r = buf.get(index    ) & 0xff;
            int g = buf.get(index + 1) & 0xff;
            int b = buf.get(index + 2) & 0xff;
            return (0xff << 24) | (r << 16) | (g << 8) | b;
        }
    }

    static class IndexedPixelFormat extends PixelFormat<ByteBuffer> {
        int precolors[];
        int nonprecolors[];
        boolean premult;

        static PixelFormat createByte(int colors[], boolean premult) {
            return new IndexedPixelFormat(Type.BYTE_INDEXED, premult,
                                          Arrays.copyOf(colors, 256));
        }

        private IndexedPixelFormat(Type type, boolean premult, int colors[]) {
            super(type);
            if (premult) {
                this.precolors = colors;
            } else {
                this.nonprecolors = colors;
            }
            this.premult = premult;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public boolean isPremultiplied() {
            return premult;
        }

        int[] getPreColors() {
            if (precolors == null) {
                int colors[] = new int[nonprecolors.length];
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = NonPretoPre(nonprecolors[i]);
                }
                this.precolors = colors;
            }
            return precolors;
        }

        int[] getNonPreColors() {
            if (nonprecolors == null) {
                int colors[] = new int[precolors.length];
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = PretoNonPre(precolors[i]);
                }
                this.nonprecolors = colors;
            }
            return nonprecolors;
        }

        @Override
        public int getArgb(ByteBuffer buf, int x, int y, int scanlineStride) {
            return getNonPreColors()[buf.get(y * scanlineStride + x) & 0xff];
        }
    }
}

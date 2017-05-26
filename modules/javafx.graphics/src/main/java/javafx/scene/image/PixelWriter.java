/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.paint.Color;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * This interface defines methods for writing the pixel data of a
 * {@link WritableImage} or other surface containing writable pixels.
 * @since JavaFX 2.2
 */
public interface PixelWriter {
    /**
     * This method returns the {@code PixelFormat} in which the surface
     * stores its pixels, or a roughly equivalent pixel format from which
     * it can easily convert pixels for purposes of writing them.
     *
     * @return the {@code PixelFormat} that best describes the underlying
     *         pixels
     */
    public PixelFormat getPixelFormat();

    /**
     * Stores pixel data for a color into the specified coordinates of the
     * surface.
     * The 32-bit integer {@code argb} parameter should contain the 4 color
     * components in separate 8-bit fields in ARGB order from the most
     * significant byte to the least significant byte.
     *
     * @param x the X coordinate of the pixel color to write
     * @param y the Y coordinate of the pixel color to write
     * @param argb the color information to write, specified in the format
     *         described by the {@link PixelFormat.Type#INT_ARGB INT_ARGB}
     *         PixelFormat type.
     */
    public void setArgb(int x, int y, int argb);

    /**
     * Stores pixel data for a {@link Color} into the specified coordinates
     * of the surface.
     *
     * @param x the X coordinate of the pixel color to write
     * @param y the Y coordinate of the pixel color to write
     * @param c the Color to write or null
     *
     * @throws java.lang.NullPointerException if {@code color} is {@code null}
     */
    public void setColor(int x, int y, Color c);

    /**
     * Stores pixel data from a buffer into a rectangular region of the
     * surface.
     * The format of the pixels in the buffer is defined by the
     * {@link PixelFormat} object and pixel format conversions will be
     * performed as needed to store the data into the surface.
     * The buffer is assumed to be positioned to the location where the
     * first pixel data to be stored in the surface pixel at location
     * {@code (x, y)} is located.
     * Pixel data for a row will be read from adjacent locations within
     * the buffer packed as tightly as possible for increasing X
     * coordinates.
     * Pixel data for adjacent rows will be read offset from each other
     * by the number of buffer data elements defined by
     * {@code scanlineStride}.
     *
     * @param <T> the type of the buffer
     * @param x the X coordinate of the rectangular region to write
     * @param y the Y coordinate of the rectangular region to write
     * @param w the width of the rectangular region to write
     * @param h the height of the rectangular region to write
     * @param pixelformat the {@code PixelFormat} object defining the format
     *        to read the pixels from the buffer
     * @param buffer a buffer of a type appropriate for the indicated
     *        {@code PixelFormat} object
     * @param scanlineStride the distance between the pixel data for the
     *        start of one row of data in the buffer to the start of the
     *        next row of data.
     *
     * @throws java.lang.NullPointerException if {@code pixelformat} or {@code buffer} is {@code null}
     */
    public <T extends Buffer>
        void setPixels(int x, int y, int w, int h,
                       PixelFormat<T> pixelformat,
                       T buffer, int scanlineStride);

    /**
     * Stores pixel data from a byte array into a rectangular region of the
     * surface.
     * The format of the pixels in the buffer is defined by the
     * {@link PixelFormat} object and pixel format conversions will be
     * performed as needed to store the data into the surface.
     * The {@code pixelformat} must be a compatible
     * {@code PixelFormat<ByteBuffer>} type.
     * The data for the first pixel at location {@code (x, y)} will be
     * read from the array index specified by the {@code offset} parameter.
     * Pixel data for a row will be read from adjacent locations within
     * the array packed as tightly as possible for increasing X
     * coordinates.
     * Pixel data for adjacent rows will be read offset from each other
     * by the number of byte array elements defined by
     * {@code scanlineStride}.
     *
     * @param x the X coordinate of the rectangular region to write
     * @param y the Y coordinate of the rectangular region to write
     * @param w the width of the rectangular region to write
     * @param h the height of the rectangular region to write
     * @param pixelformat the {@code PixelFormat<ByteBuffer>} object
     *        defining the byte format to read the pixels from buffer
     * @param buffer a byte array containing the pixel data to store
     * @param offset the offset into {@code buffer} to read the first
     *        pixel data
     * @param scanlineStride the distance between the pixel data for the
     *        start of one row of data in the buffer to the start of the
     *        next row of data
     *
     * @throws java.lang.NullPointerException if {@code pixelformat} or {@code buffer} is {@code null}
     */
    public void setPixels(int x, int y, int w, int h,
                          PixelFormat<ByteBuffer> pixelformat,
                          byte buffer[], int offset, int scanlineStride);

    /**
     * Stores pixel data from an int array into a rectangular region of the
     * surface.
     * The format of the pixels in the buffer is defined by the
     * {@link PixelFormat} object and pixel format conversions will be
     * performed as needed to store the data into the surface.
     * The {@code pixelformat} must be a compatible
     * {@code PixelFormat<IntBuffer>} type.
     * The data for the first pixel at location {@code (x, y)} will be
     * read from the array index specified by the {@code offset} parameter.
     * Pixel data for a row will be read from adjacent locations within
     * the array packed as tightly as possible for increasing X
     * coordinates.
     * Pixel data for adjacent rows will be read offset from each other
     * by the number of int array elements defined by
     * {@code scanlineStride}.
     *
     * @param x the X coordinate of the rectangular region to write
     * @param y the Y coordinate of the rectangular region to write
     * @param w the width of the rectangular region to write
     * @param h the height of the rectangular region to write
     * @param pixelformat the {@code PixelFormat<IntBuffer>} object
     *        defining the int format to read the pixels from buffer
     * @param buffer an int array to containing the pixel data to store
     * @param offset the offset into {@code buffer} to read the first
     *        pixel data
     * @param scanlineStride the distance between the pixel data for the
     *        start of one row of data in the buffer to the start of the
     *        next row of data
     *
     * @throws java.lang.NullPointerException if {@code pixelformat} or {@code buffer} is {@code null}
     */
    public void setPixels(int x, int y, int w, int h,
                          PixelFormat<IntBuffer> pixelformat,
                          int buffer[], int offset, int scanlineStride);

    /**
     * Stores pixel data retrieved from a {@code PixelReader} instance
     * into a rectangular region of the surface.
     * The data for the pixel on the surface at {@code (dstx, dsty)}
     * will be retrieved from the {@code reader} from its location
     * {@code (srcx, srcy)}.
     * This method performs an operation which is semantically equivalent to
     * (though likely much faster than) this pseudo-code:
     * <pre>
     *     for (int y = 0; y &lt; h, y++) {
     *         for (int x = 0; x &lt; w; x++) {
     *             setArgb(dstx + x, dsty + y,
     *                     reader.getArgb(srcx + x, srcy + y));
     *         }
     *     }
     * </pre>
     *
     *
     * @param dstx the X coordinate of the rectangular region to write
     * @param dsty the Y coordinate of the rectangular region to write
     * @param w the width of the rectangular region to write
     * @param h the height of the rectangular region to write
     * @param reader the {@link PixelReader} used to get the pixel data
     *        to write
     * @param srcx the X coordinate of the data to read from {@code reader}
     * @param srcy the Y coordinate of the data to read from {@code reader}
     *
     * @throws java.lang.NullPointerException if {@code reader} is {@code null}
     */
    public void setPixels(int dstx, int dsty, int w, int h,
                          PixelReader reader, int srcx, int srcy);
}

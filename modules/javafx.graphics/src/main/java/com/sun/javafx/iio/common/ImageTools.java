/*
 * Copyright (c) 2009, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.common;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageStorage;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * A set of format-independent convenience methods useful in image loading
 * and saving.
 */
public class ImageTools {

    /**
     * The percentage increment between progress report updates.
     */
    public static final int PROGRESS_INTERVAL = 5;

    /**
     * See the general contract of the <code>readFully</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the specified
     * input stream.
     *
     * @param      stream the stream from which to read the data.
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset of the data.
     * @param      len   the number of bytes to read.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading all the bytes.
     * @exception  IOException   if another I/O error occurs.
     */
    public static int readFully(InputStream stream,
            byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int requestedLength = len;
        // Fix 4430357 - if off + len < 0, overflow occurred
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > b.length!");
        }

        while (len > 0) {
            int nbytes = stream.read(b, off, len);
            if (nbytes == -1) {
                throw new EOFException();
            }
            off += nbytes;
            len -= nbytes;
        }

        return requestedLength;
    }

    /**
     * See the general contract of the <code>readFully</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @param      stream the stream from which to read the data.
     * @param      b   the buffer into which the data is read.
     * @exception  EOFException  if this input stream reaches the end before
     *             reading all the bytes.
     * @exception  IOException   if another I/O error occurs.
     */
    public static int readFully(InputStream stream, byte[] b) throws IOException {
        return readFully(stream, b, 0, b.length);
    }

    /**
     * Skips over <code>n</code> bytes of data from the input stream.
     * @param      stream the stream to skip.
     * @param      n the number of bytes to be skipped.
     * @exception  EOFException if this input stream reaches the end before
     *             skipping all the bytes.
     * @exception  IOException if another I/O error occurs.
     */
    public static void skipFully(InputStream stream, long n) throws IOException {
        while (n > 0) {
            long skipped = stream.skip(n);
            if (skipped <= 0) {
                // check if the EOF is reached
                if (stream.read() == -1) {
                    throw new EOFException();
                }
                n--;
            } else {
                n -= skipped;
            }
        }
    }

    public static String getScaledImageName(String path, int scaleFactor) {
        StringBuilder result = new StringBuilder();
        int slash = path.lastIndexOf('/');
        String name = (slash < 0) ? path : path.substring(slash + 1);
        int dot = name.lastIndexOf(".");
        if (dot < 0) {
            dot = name.length();
        }
        if (slash >= 0) {
            result.append(path.substring(0, slash + 1));
        }
        result.append(name.substring(0, dot));
        result.append("@");
        result.append(scaleFactor);
        result.append("x");
        result.append(name.substring(dot));
        return result.toString();
    }

    public static InputStream createInputStream(String input) throws IOException {
        InputStream stream = null;

        // there should be a central utility  for mapping these Strings to their
        // inputStreams
        try {
            File file = new File(input);
            if (file.exists()) {
                stream = new FileInputStream(file);
            }
        } catch (Exception e) {
            // ignore exception and try as url.
        }
        if (stream == null) {
            URL url = new URL(input);
            stream = url.openStream();
        }
        return stream;
    }

    public static int[] computeDimensions(int sourceWidth, int sourceHeight,
            int maxWidth, int maxHeight, boolean preserveAspectRatio) {
        // ensure non-negative dimensions (0 implies default)
        int finalWidth = maxWidth < 0 ? 0 : maxWidth;
        int finalHeight = maxHeight < 0 ? 0 : maxHeight;

        if(finalWidth == 0 && finalHeight == 0) {
            // default to source dimensions
            finalWidth = sourceWidth;
            finalHeight = sourceHeight;
        } else if (finalWidth != sourceWidth || finalHeight != sourceHeight) {
            if (preserveAspectRatio) {
                // compute the final dimensions
                if (finalWidth == 0) {
                    finalWidth = Math.round((float) sourceWidth * finalHeight / sourceHeight);
                } else if (finalHeight == 0) {
                    finalHeight = Math.round((float) sourceHeight * finalWidth / sourceWidth);
                } else {
                    float scale = Math.min((float) finalWidth / sourceWidth, (float) finalHeight / sourceHeight);
                    finalWidth = Math.round(sourceWidth * scale);
                    finalHeight = Math.round(sourceHeight * scale);
                }
            } else {
                // set final dimensions to default if zero
                if (finalHeight == 0) {
                    finalHeight = sourceHeight;
                }
                if (finalWidth == 0) {
                    finalWidth = sourceWidth;
                }
            }


            // clamp dimensions to positive values
            if (finalWidth <= 0) {
                finalWidth = 1;
            }
            if (finalHeight <= 0) {
                finalHeight = 1;
            }
        }


        return new int[]{finalWidth, finalHeight};
    }

    public static void validateMaxDimensions(double width, double height, double scaleFactor) {
        if (width * scaleFactor > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format(
                Locale.ROOT, "Image width exceeds maximum value (width = %f, scale = %f)", width, scaleFactor));
        }

        if (height * scaleFactor > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format(
                Locale.ROOT, "Image height exceeds maximum value (height = %f, scale = %f)", height, scaleFactor));
        }

        // Use a long multiplication to prevent int overflow.
        if ((long)(width * scaleFactor) * (long)(height * scaleFactor) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format(
                Locale.ROOT, "Image size exceeds maximum value (width = %f, height = %f, scale = %f)",
                width, height, scaleFactor));
        }
    }

    public static ImageFrame scaleImageFrame(ImageFrame src,
            int destWidth, int destHeight, boolean isSmooth)
    {
        int numBands = ImageStorage.getInstance().getNumBands(src.getImageType());
        ByteBuffer dst = scaleImage((ByteBuffer) src.getImageData(),
                src.getWidth(), src.getHeight(), numBands,
                destWidth, destHeight, isSmooth);
        return new ImageFrame(src.getImageType(), dst,
                destWidth, destHeight, destWidth * numBands, src.getMetadata());
    }

    public static ByteBuffer scaleImage(ByteBuffer src,
            int sourceWidth, int sourceHeight, int numBands,
            int destWidth, int destHeight, boolean isSmooth)
    {
        PushbroomScaler scaler = ScalerFactory.createScaler(
                sourceWidth, sourceHeight, numBands,
                destWidth, destHeight, isSmooth);

        int stride = sourceWidth * numBands;
        if (src.hasArray()) {
            byte image[] = src.array();
            for (int y = 0; y != sourceHeight; ++y) {
                scaler.putSourceScanline(image, y * stride);
            }
        } else {
            byte scanline[] = new byte[stride];
            for (int y = 0; y != sourceHeight; ++y) {
                src.get(scanline);
                scaler.putSourceScanline(scanline, 0);
            }
        }

        return scaler.getDestination();
    }
}

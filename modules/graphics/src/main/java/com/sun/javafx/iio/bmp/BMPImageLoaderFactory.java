/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.bmp;

import com.sun.javafx.iio.*;
import com.sun.javafx.iio.common.*;
import java.io.*;
import java.nio.ByteBuffer;

final class BMPDescriptor extends ImageDescriptor {

    static final String formatName = "BMP";
    static final String extensions[] = { "bmp" };
    static final Signature signatures[] = {new Signature((byte)0x42, (byte)0x4D)};
    static final ImageDescriptor theInstance = new BMPDescriptor();

    private BMPDescriptor() {
        super(formatName, extensions, signatures);
    }
}

// the difference of LEInputStream from DataInputStream is Endianness
final class LEInputStream {

    final public InputStream in;

    LEInputStream(InputStream is) {
        in = is;
    }

    public final short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short)((ch2 << 8) + ch1);
    }

    public final int readInt() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
    }

    public final void skipBytes(int n) throws IOException {
        int m = (int)in.skip(n);
        if (m < n) {
            throw new EOFException();
        }
    }
}

final class BitmapInfoHeader {

    static final int BIH_SIZE = 40;
    static final int BIH4_SIZE = 108;
    static final int BIH5_SIZE = 124;

    final int    biSize;
    final int    biWidth;
    final int    biHeight;
    final short  biPlanes;
    final short  biBitCount;
    final int    biCompression;
    final int    biSizeImage;
    final int    biXPelsPerMeter;
    final int    biYPelsPerMeter;
    final int    biClrUsed;
    final int    biClrImportant;

    BitmapInfoHeader(LEInputStream data) throws IOException {
        biSize = data.readInt();
        biWidth = data.readInt();
        biHeight = data.readInt();
        biPlanes = data.readShort();
        biBitCount = data.readShort();
        biCompression = data.readInt();
        biSizeImage = data.readInt();
        biXPelsPerMeter = data.readInt();
        biYPelsPerMeter = data.readInt();
        biClrUsed = data.readInt();
        biClrImportant = data.readInt();

        if (biSize > BIH_SIZE) {
            if (biSize == BIH4_SIZE || biSize == BIH5_SIZE) {
                data.skipBytes(biSize - BIH_SIZE);
            } else {
                throw new IOException("BitmapInfoHeader is corrupt");
            }
        }
        validate();
    }

    void validate() {
        if (biCompression != 0 || biPlanes != 1 || biBitCount != 24) {
            throw new RuntimeException(
                    "Unsupported BMP image: " +
                    "only 24 bit uncompressed BMP`s is supported");
        }
    }
}

final class BMPImageLoader extends ImageLoaderImpl {

    static final short BM = 0x4D42;
    static final int BFH_SIZE = 14;

    final LEInputStream data;

    short bfType; // must be equal to BM
    int   bfSize;
    int   bfOffBits;
    int   bgra_palette[];
    BitmapInfoHeader bih;

    BMPImageLoader(InputStream input) throws IOException {
        super(BMPDescriptor.theInstance);
        data = new LEInputStream(input);
        bfType = data.readShort();
        if (isValid()) {
            readHeader();
        }
    }

    private void readHeader() throws IOException {
        bfSize = data.readInt();
        data.skipBytes(4); // 32  bits reserved
        bfOffBits = data.readInt();
        bih = new BitmapInfoHeader(data);
        if (bih.biSize + BFH_SIZE != bfOffBits) {
            data.skipBytes(bfOffBits - bih.biSize - BFH_SIZE);
        }
    }

    private boolean isValid() {
        return bfType == BM;
    }

    public void dispose() { }

    static void GBRtoRGB(byte data[], int pos, int size) {
        for (int sz = size / 3; sz != 0; --sz) {
            byte x = data[pos], y = data[pos + 2];
            data[pos + 2] = x; data[pos] = y;
            pos += 3;
        }
    }

    public ImageFrame load(int imageIndex, int width, int height,
            boolean preserveAspectRatio, boolean smooth) throws IOException
{
        if (0 != imageIndex) {
            return null;
        }

        if ((width > 0 && width != bih.biWidth) ||
            (height > 0 && height != bih.biHeight))
        {
            throw new RuntimeException("scaling for BMP is not supported");
        }

        // Pass image metadata to any listeners.
        ImageMetadata imageMetadata = new ImageMetadata(null, Boolean.TRUE,
            null, null, null, null, bih.biWidth, bih.biHeight,
            null, null, null);
        updateImageMetadata(imageMetadata);

        int bmpStride = (bih.biBitCount*bih.biWidth/8 + 3) & ~3;
        int rowLength = (bih.biBitCount/8)*bih.biWidth;

        int hght = Math.abs(bih.biHeight);

        byte image[] = new byte[rowLength * hght];

        for (int i = 0; i != hght; ++i) {
            int line = bih.biHeight < 0 ? i : hght-i-1;
            int nRead = data.in.read(image, line * rowLength, rowLength);
            GBRtoRGB(image, line * rowLength, nRead);

            if (nRead != rowLength) {
                break;
            }

            if (nRead < bmpStride) {
                data.skipBytes(bmpStride-nRead);
            }
        }

        return new ImageFrame(ImageStorage.ImageType.RGB, ByteBuffer.wrap(image),
                bih.biWidth, hght, rowLength, null, null);
    }
}

public final class BMPImageLoaderFactory implements ImageLoaderFactory {

    private static final BMPImageLoaderFactory theInstance =
            new BMPImageLoaderFactory();

    public static ImageLoaderFactory getInstance() {
        return theInstance;
    }

    public ImageFormatDescription getFormatDescription() {
        return BMPDescriptor.theInstance;
    }

    public ImageLoader createImageLoader(InputStream input) throws IOException {
        return new BMPImageLoader(input);
    }
}


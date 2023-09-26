/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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
    static final String[] extensions = { "bmp" };
    static final Signature[] signatures = {new Signature((byte)0x42, (byte)0x4D)};
    static final String[] mimeSubtypes = { "bmp" };
    static final ImageDescriptor theInstance = new BMPDescriptor();

    private BMPDescriptor() {
        super(formatName, extensions, signatures, mimeSubtypes);
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
        ImageTools.skipFully(in, n);
    }
}

final class BitmapInfoHeader {

    static final int BIH_SIZE = 40;
    static final int BIH4_SIZE = 108;
    static final int BIH5_SIZE = 124;
    static final int BI_RGB = 0;
    static final int BI_RLE8 = 1;
    static final int BI_RLE4 = 2;
    static final int BI_BITFIELDS = 3;
    static final int BI_JPEG = 4;
    static final int BI_PNG = 5;

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
        if (biWidth <= 0) {
            throw new IOException("Bad BMP image width, must be > 0!");
        }
        // If biHeight is negative, the bitmap is a top-down DIB
        // See: https://learn.microsoft.com/en-us/windows/win32/api/wingdi/ns-wingdi-bitmapinfoheader

        int height = Math.abs(biHeight);

        if (height == 0) {
            throw new IOException("Bad BMP image height, must be != 0!");
        }

        if (biWidth >= (Integer.MAX_VALUE / height)) {
            throw new IOException("Bad BMP image size!");
        }

        validate();
    }

    void validate() throws IOException {
        if (biBitCount < 1 ||
                biCompression == BI_JPEG || biCompression == BI_PNG)
        {
            throw new IOException("Unsupported BMP image: " +
                    "Embedded JPEG or PNG images are not supported");
        }

        switch (biCompression) {
            case BI_RLE4:
                if (biBitCount != 4) {
                    throw new IOException("Invalid BMP image: " +
                            "Only 4 bpp images can be RLE4 compressed");
                }
                break;
            case BI_RLE8:
                if (biBitCount != 8) {
                    throw new IOException("Invalid BMP image: " +
                            "Only 8 bpp images can be RLE8 compressed");
                }
                break;
            case BI_BITFIELDS:
                if (biBitCount != 16 && biBitCount != 32) {
                    throw new IOException("Invalid BMP image: " +
                            "Only 16 or 32 bpp images can use BITFIELDS compression");
                }
                break;
            case BI_RGB:
                break;
            default:
                throw new IOException("Unknown BMP compression type");
        }
    }
}

final class BMPImageLoader extends ImageLoaderImpl {

    static final short BM = 0x4D42;
    static final int BFH_SIZE = 14;

    final LEInputStream data;

    int   bfSize;
    int   bfOffBits;
    byte  bgra_palette[];
    BitmapInfoHeader bih;

    // BI_BITFIELDS support
    int bitMasks[];
    int bitOffsets[];

    BMPImageLoader(InputStream input) throws IOException {
        super(BMPDescriptor.theInstance);
        data = new LEInputStream(input);
        if (data.readShort() != BM) {
            throw new IOException("Invalid BMP file signature");
        }
        readHeader();
    }

    private void readHeader() throws IOException {
        bfSize = data.readInt();
        data.skipBytes(4); // 32  bits reserved
        bfOffBits = data.readInt();
        bih = new BitmapInfoHeader(data);
        if (bfOffBits < bih.biSize + BFH_SIZE) {
            throw new IOException("Invalid bitmap bits offset");
        }

        if (bih.biSize + BFH_SIZE != bfOffBits) {
            int length = bfOffBits - bih.biSize - BFH_SIZE;
            int paletteSize = length / 4;
            bgra_palette = new byte[paletteSize * 4];
            int read = data.in.read(bgra_palette);
            // goto bitmap bits
            if (read < length) {
                data.skipBytes(length - read);
            }
        }

        if (bih.biCompression == BitmapInfoHeader.BI_BITFIELDS) {
            parseBitfields();
        } else if (bih.biCompression == BitmapInfoHeader.BI_RGB &&
                bih.biBitCount == 16)
        {
            bitMasks = new int[] { 0x7C00, 0x3E0, 0x1F };
            bitOffsets = new int[] { 10, 5, 0 };
        }
    }

    private void parseBitfields() throws IOException {
        if (bgra_palette.length != 12) {
            throw new IOException("Invalid bit masks");
        }
        bitMasks = new int[3];
        bitOffsets = new int[3];
        for (int i = 0; i < 3; i++) {
            int mask = getDWord(bgra_palette, i * 4);
            bitMasks[i] = mask;
            int offset = 0;
            if (mask != 0) {
                while ((mask & 1) == 0) {
                    offset++;
                    mask = mask >>> 1;
                }
                if (!isPow2Minus1(mask)) {
                    throw new IOException("Bit mask is not contiguous");
                }
            }
            bitOffsets[i] = offset;
        }
        if (!checkDisjointMasks(bitMasks[0], bitMasks[1], bitMasks[2])) {
            throw new IOException("Bit masks overlap");
        }
    }

    static boolean checkDisjointMasks(int m1, int m2, int m3) {
        return ((m1 & m2) | (m1 & m3) | (m2 & m3)) == 0;
    }

    static boolean isPow2Minus1(int i) {
        return (i & (i + 1)) == 0;
    }

    @Override
    public void dispose() {
    }

    private void readRLE(byte[] image, int rowLength, int hght, boolean isRLE4)
            throws IOException
    {
        int imgSize = bih.biSizeImage;
        if (imgSize == 0) {
            imgSize = bfSize - bfOffBits;
        }
        byte imgData[] = new byte[imgSize];
        ImageTools.readFully(data.in, imgData);

        boolean isBottomUp = bih.biHeight > 0;
        int line = isBottomUp ? hght - 1 : 0;
        int i = 0;
        int dstOffset = line * rowLength;
        while (i < imgSize) {
            int b1 = getByte(imgData, i++);
            int b2 = getByte(imgData, i++);
            if (b1 == 0) { // absolute
                switch (b2) {
                    case 0: // end of line
                        line += isBottomUp ? -1 : 1;
                        dstOffset = line * rowLength;
                        break;
                    case 1: // end of bitmap
                        return;
                    case 2: // delta
                        int deltaX = getByte(imgData, i++);
                        int deltaY = getByte(imgData, i++);
                        line += deltaY;
                        dstOffset += (deltaY * rowLength);
                        dstOffset += deltaX * 3;
                        break;
                    default:
                        int indexData = 0;
                        int index;
                        for (int p = 0; p < b2; p++) {
                            if (isRLE4) {
                                if ((p & 1) == 0) {
                                    indexData = getByte(imgData, i++);
                                    index = (indexData & 0xf0) >> 4;
                                } else {
                                    index = indexData & 0x0f;
                                }
                            } else {
                                index = getByte(imgData, i++);
                            }
                            dstOffset = setRGBFromPalette(image, dstOffset, index);
                        }
                        if (isRLE4) {
                            if ((b2 & 3) == 1 || (b2 & 3) == 2) i++;
                        } else {
                            if ((b2 & 1) == 1) i++;
                        }
                        break;
                }
            } else { // encoded
                if (isRLE4) {
                    int index1 = (b2 & 0xf0) >> 4;
                    int index2 = b2 & 0x0f;
                    for (int p = 0; p < b1; p++) {
                        dstOffset = setRGBFromPalette(image, dstOffset,
                                (p & 1) == 0 ? index1 : index2);
                    }
                } else {
                    for (int p = 0; p < b1; p++) {
                        dstOffset = setRGBFromPalette(image, dstOffset, b2);
                    }
                }
            }
        }

    }

    private int setRGBFromPalette(byte[] image, int dstOffset, int index) {
        index *= 4;
        image[dstOffset++] = bgra_palette[index + 2];
        image[dstOffset++] = bgra_palette[index + 1];
        image[dstOffset++] = bgra_palette[index];
        return dstOffset;
    }

    private void readPackedBits(byte[] image, int rowLength, int hght)
            throws IOException
    {
        int pixPerByte = 8 / bih.biBitCount;
        int bytesPerLine = (bih.biWidth + pixPerByte - 1) / pixPerByte;
        int srcStride = (bytesPerLine + 3) & ~3;
        int bitMask = (1 << bih.biBitCount) - 1;

        byte lineBuf[] = new byte[srcStride];
        for (int i = 0; i != hght; ++i) {
            ImageTools.readFully(data.in, lineBuf);
            int line = bih.biHeight < 0 ? i : hght - i - 1;
            int dstOffset = line * rowLength;

            for (int x = 0; x != bih.biWidth; x++) {
                int bitnum = x * bih.biBitCount;
                int element = lineBuf[bitnum / 8];
                int shift = 8 - (bitnum & 7) - bih.biBitCount;
                int index = (element >> shift) & bitMask;
                dstOffset = setRGBFromPalette(image, dstOffset, index);
            }
        }
    }

    private static int getDWord(byte[] buf, int pos) {
        return ((buf[pos    ] & 0xff)     ) |
               ((buf[pos + 1] & 0xff) << 8) |
               ((buf[pos + 2] & 0xff) << 16) |
               ((buf[pos + 3] & 0xff) << 24);
    }

    private static int getWord(byte[] buf, int pos) {
        return ((buf[pos    ] & 0xff)     ) |
               ((buf[pos + 1] & 0xff) << 8);
    }

    private static int getByte(byte[] buf, int pos) {
        return buf[pos] & 0xff;
    }

    @FunctionalInterface
    private interface BitConverter {
        public byte convert(int i, int mask, int offset);
    }

    private static byte convertFrom5To8Bit(int i, int mask, int offset) {
        int b = (i & mask) >>> offset;
        return (byte)(b << 3 | b >> 2);
    }

    private static byte convertFromXTo8Bit(int i, int mask, int offset) {
        int b = (i & mask) >>> offset;
        return (byte)(b * 255.0 / (mask >>> offset));
    }

    private void read16Bit(byte[] image, int rowLength, int hght, BitConverter converter)
            throws IOException
    {
        int bytesPerLine = bih.biWidth * 2;
        int srcStride = (bytesPerLine + 3) & ~3;
        byte lineBuf[] = new byte[srcStride];
        for (int i = 0; i != hght; ++i) {
            ImageTools.readFully(data.in, lineBuf);
            int line = bih.biHeight < 0 ? i : hght - i - 1;
            int dstOffset = line * rowLength;

            for (int x = 0; x != bih.biWidth; x++) {
                int element = getWord(lineBuf, x * 2);
                for (int j = 0; j < 3; j++) {
                    image[dstOffset++] =
                            converter.convert(element, bitMasks[j], bitOffsets[j]);
                }
            }
        }
    }

    private void read32BitRGB(byte[] image, int rowLength, int hght) throws IOException {
        int bytesPerLine = bih.biWidth * 4;
        byte lineBuf[] = new byte[bytesPerLine];
        for (int i = 0; i != hght; ++i) {
            ImageTools.readFully(data.in, lineBuf);
            int line = bih.biHeight < 0 ? i : hght - i - 1;
            int dstOff = line * rowLength;

            for (int x = 0; x != bih.biWidth; x++) {
                int srcOff = x * 4;
                image[dstOff++] = lineBuf[srcOff + 2];
                image[dstOff++] = lineBuf[srcOff + 1];
                image[dstOff++] = lineBuf[srcOff    ];
            }
        }
    }

    private void read32BitBF(byte[] image, int rowLength, int hght) throws IOException {
        int bytesPerLine = bih.biWidth * 4;
        byte lineBuf[] = new byte[bytesPerLine];
        for (int i = 0; i != hght; ++i) {
            ImageTools.readFully(data.in, lineBuf);
            int line = bih.biHeight < 0 ? i : hght - i - 1;
            int dstOff = line * rowLength;

            for (int x = 0; x != bih.biWidth; x++) {
                int srcOff = x * 4;
                int element = getDWord(lineBuf, srcOff);
                for (int j = 0; j < 3; j++) {
                    image[dstOff++] =
                            convertFromXTo8Bit(element, bitMasks[j], bitOffsets[j]);
                }
            }
        }
    }

    private void read24Bit(byte[] image, int rowLength, int hght) throws IOException {
        int bmpStride = (rowLength + 3) & ~3;
        int padding = bmpStride - rowLength;

        for (int i = 0; i != hght; ++i) {
            int line = bih.biHeight < 0 ? i : hght - i - 1;
            int lineOffset = line * rowLength;
            ImageTools.readFully(data.in, image, lineOffset, rowLength);
            data.skipBytes(padding);
            BGRtoRGB(image, lineOffset, rowLength);
        }
    }

    static void BGRtoRGB(byte data[], int pos, int size) {
        for (int sz = size / 3; sz != 0; --sz) {
            byte b = data[pos], r = data[pos + 2];
            data[pos + 2] = b; data[pos] = r;
            pos += 3;
        }
    }

    @Override
    public ImageFrame load(int imageIndex, int width, int height,
            boolean preserveAspectRatio, boolean smooth) throws IOException
    {
        if (0 != imageIndex) {
            return null;
        }
        int hght = Math.abs(bih.biHeight);

        int[] outWH = ImageTools.computeDimensions(bih.biWidth, hght, width, height, preserveAspectRatio);
        width = outWH[0];
        height = outWH[1];

        int bpp = 3;
        if (width >= (Integer.MAX_VALUE / height / bpp)) {
            throw new IOException("Bad BMP image size!");
        }

        // Pass image metadata to any listeners.
        ImageMetadata imageMetadata = new ImageMetadata(null, Boolean.TRUE,
            null, null, null, null, null, width, height,
            null, null, null);
        updateImageMetadata(imageMetadata);

        int stride = bih.biWidth * bpp;
        byte image[] = new byte[stride * hght];

        switch (bih.biBitCount) {
            case 1:
                readPackedBits(image, stride, hght);
                break;
            case 4:
                if (bih.biCompression == BitmapInfoHeader.BI_RLE4) {
                    readRLE(image, stride, hght, true);
                } else {
                    readPackedBits(image, stride, hght);
                }
                break;
            case 8:
                if (bih.biCompression == BitmapInfoHeader.BI_RLE8) {
                    readRLE(image, stride, hght, false);
                } else {
                    readPackedBits(image, stride, hght);
                }
                break;
            case 16:
                if (bih.biCompression == BitmapInfoHeader.BI_BITFIELDS) {
                    read16Bit(image, stride, hght, BMPImageLoader::convertFromXTo8Bit);
                } else {
                    read16Bit(image, stride, hght, BMPImageLoader::convertFrom5To8Bit);
                }
                break;
            case 32:
                if (bih.biCompression == BitmapInfoHeader.BI_BITFIELDS) {
                    read32BitBF(image, stride, hght);
                } else {
                    read32BitRGB(image, stride, hght);
                }
                break;
            case 24:
                read24Bit(image, stride, hght);
                break;
            default:
                throw new IOException("Unknown BMP bit depth");
        }

        ByteBuffer img = ByteBuffer.wrap(image);
        if (bih.biWidth != width || hght != height) {
            img = ImageTools.scaleImage(img, bih.biWidth, hght, bpp,
                    width, height, smooth);
        }

        return new ImageFrame(ImageStorage.ImageType.RGB, img,
                width, height, width * bpp, null, imageMetadata);
    }
}

public final class BMPImageLoaderFactory implements ImageLoaderFactory {

    private static final BMPImageLoaderFactory theInstance =
            new BMPImageLoaderFactory();

    public static ImageLoaderFactory getInstance() {
        return theInstance;
    }

    @Override
    public ImageFormatDescription getFormatDescription() {
        return BMPDescriptor.theInstance;
    }

    @Override
    public ImageLoader createImageLoader(InputStream input) throws IOException {
        return new BMPImageLoader(input);
    }
}

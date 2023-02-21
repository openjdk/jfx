/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

/*
 * loader implementation for PNG file format
 * specification http://www.w3.org/TR/PNG/
 */
package com.sun.javafx.iio.png;

import com.sun.javafx.iio.*;
import com.sun.javafx.iio.common.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.*;

public final class PNGImageLoader2 extends ImageLoaderImpl {

    // file signature
    static final byte FILE_SIG[] = {(byte) 137, (byte) 80, (byte) 78,
        (byte) 71, (byte) 13, (byte) 10, (byte) 26, (byte) 10};
    // Critical chunks
    static final int IHDR_TYPE = 0x49484452;
    static final int PLTE_TYPE = 0x504c5445;
    static final int IDAT_TYPE = 0x49444154;
    static final int IEND_TYPE = 0x49454e44;
    // Ancillary chunks
    static final int tRNS_TYPE = 0x74524e53;
    // color model
    static final int PNG_COLOR_GRAY = 0;
    static final int PNG_COLOR_RGB = 2;
    static final int PNG_COLOR_PALETTE = 3;
    static final int PNG_COLOR_GRAY_ALPHA = 4;
    static final int PNG_COLOR_RGB_ALPHA = 6;
    // channels per pixel
    static final int[] numBandsPerColorType = {1, -1, 3, 1, 2, -1, 4};
    // filters
    static final int PNG_FILTER_NONE = 0;
    static final int PNG_FILTER_SUB = 1;
    static final int PNG_FILTER_UP = 2;
    static final int PNG_FILTER_AVERAGE = 3;
    static final int PNG_FILTER_PAETH = 4;
    // data stream
    private final DataInputStream stream;
    private int width, height, bitDepth, colorType;
    private boolean isInterlaced;
    // transparency information
    private boolean tRNS_present = false;
    private boolean tRNS_GRAY_RGB = false;
    private int trnsR, trnsG, trnsB;
    // Palette data : r,g,b,[a]  -  alpha optional
    private byte palette[][];

    public PNGImageLoader2(InputStream input) throws IOException {
        super(PNGDescriptor.getInstance());
        stream = new DataInputStream(input);

        byte signature[] = readBytes(new byte[8]);

        if (!Arrays.equals(FILE_SIG, signature)) {
            throw new IOException("Bad PNG signature!");
        }

        readHeader();
    }

    private void readHeader() throws IOException {
        int hdrData[] = readChunk();

        if (hdrData[1] != IHDR_TYPE && hdrData[0] != 13) {
            throw new IOException("Bad PNG header!");
        }

        width = stream.readInt();
        height = stream.readInt();

        if (width <= 0) {
            throw new IOException("Bad PNG image width, must be > 0!");
        }
        if (height <= 0) {
            throw new IOException("Bad PNG image height, must be > 0!");
        }
        if (width >= (Integer.MAX_VALUE / height)) {
            throw new IOException("Bad PNG image size!");
        }

        bitDepth = stream.readByte();
        if (bitDepth != 1 && bitDepth != 2 && bitDepth != 4
                && bitDepth != 8 && bitDepth != 16) {
            throw new IOException("Bad PNG bit depth");
        }

        colorType = stream.readByte();

        if (colorType > 6 || colorType == 1 || colorType == 5) {
            throw new IOException("Bad PNG color type");
        }

        // bitDepth<8 only for palette and gray
        // bitDepth==16 not for palette
        if ((colorType != PNG_COLOR_PALETTE && colorType != PNG_COLOR_GRAY && bitDepth < 8)
                || (colorType == PNG_COLOR_PALETTE && bitDepth == 16)) {
            throw new IOException("Bad color type/bit depth combination!");
        }

        byte compressionMethod = stream.readByte();
        if (compressionMethod != 0) {
            throw new IOException("Bad PNG comression!");
        }

        byte filterMethod = stream.readByte();
        if (filterMethod != 0) {
            throw new IOException("Bad PNG filter method!");
        }

        byte interlaceMethod = stream.readByte();

        if (interlaceMethod != 0 && interlaceMethod != 1) {
            throw new IOException("Unknown interlace method (not 0 or 1)!");
        }

        int crc = stream.readInt();

        isInterlaced = interlaceMethod == 1;
    }

    private int[] readChunk() throws IOException {
        return new int[]{stream.readInt(), stream.readInt()};
    }

    private byte[] readBytes(byte data[]) throws IOException {
        return readBytes(data, 0, data.length);
    }

    private byte[] readBytes(byte data[], int offs, int size) throws IOException {
        stream.readFully(data, offs, size);
        return data;
    }

    private void skip(int n) throws IOException {
        if (n != stream.skipBytes(n)) {
            throw new EOFException();
        }
    }

    private void readPaletteChunk(int chunkLength) throws IOException {
        int numEntries = chunkLength / 3;
        int paletteEntries = 1 << bitDepth;
        if (numEntries > paletteEntries) {
            emitWarning("PLTE chunk contains too many entries for bit depth, ignoring extras.");
            numEntries = paletteEntries;
        }

        palette = new byte[3][paletteEntries];

        byte paletteData[] = readBytes(new byte[chunkLength]);

        for (int i = 0, idx = 0; i != numEntries; ++i) {
            for (int k = 0; k != 3; ++k) {
                palette[k][i] = paletteData[idx++];
            }
        }
    }

    private void parsePaletteChunk(int chunkLength) throws IOException {
        if (palette != null) {
            emitWarning(
                    "A PNG image may not contain more than one PLTE chunk.\n"
                    + "The chunk wil be ignored.");
            skip(chunkLength);
            return;
        }

        switch (colorType) {
            case PNG_COLOR_PALETTE:
                readPaletteChunk(chunkLength);
                return;
            case PNG_COLOR_GRAY:
            case PNG_COLOR_GRAY_ALPHA:
                emitWarning("A PNG gray or gray alpha image cannot have a PLTE chunk.\n"
                        + "The chunk wil be ignored.");
            // silently ignore palette for RGB
            default:
                skip(chunkLength);
        }
    }

    private boolean readPaletteTransparency(int chunkLength) throws IOException {
        if (palette == null) {
            emitWarning("tRNS chunk without prior PLTE chunk, ignoring it.");
            skip(chunkLength);
            return false;
        }

        byte newPal[][] = new byte[4][];

        System.arraycopy(palette, 0, newPal, 0, 3);

        int paletteLength = palette[0].length;
        newPal[3] = new byte[paletteLength];

        int nRead = chunkLength < paletteLength ? chunkLength : paletteLength;
        readBytes(newPal[3], 0, nRead);

        for (int i = nRead; i < paletteLength; ++i) {
            newPal[3][i] = -1;
        }

        if (nRead < chunkLength) {
            skip(chunkLength - nRead);
        }

        palette = newPal;

        return true;
    }

    private boolean readGrayTransparency(int chunkLength) throws IOException {
        if (chunkLength == 2) {
            trnsG = stream.readShort();
            return true;
        }
        return false;
    }

    private boolean readRgbTransparency(int chunkLength) throws IOException {
        if (chunkLength == 6) {
            trnsR = stream.readShort();
            trnsG = stream.readShort();
            trnsB = stream.readShort();
            return true;
        }
        return false;
    }

    private void parseTransparencyChunk(int chunkLength) throws IOException {
        switch (colorType) {
            case PNG_COLOR_PALETTE:
                tRNS_present = readPaletteTransparency(chunkLength);
                break;
            case PNG_COLOR_GRAY:
                tRNS_GRAY_RGB = tRNS_present = readGrayTransparency(chunkLength);
                break;
            case PNG_COLOR_RGB:
                tRNS_GRAY_RGB = tRNS_present = readRgbTransparency(chunkLength);
                break;
            default:
                emitWarning("TransparencyChunk may not present when alpha explicitly defined");
                skip(chunkLength);
        }
    }

    // return sizeof first IDAT chunk or 0 of error
    private int parsePngMeta() throws IOException {
        while (true) {
            int chunk[] = readChunk();

            if (chunk[0] < 0) {
                throw new IOException("Invalid chunk length");
            }
            switch (chunk[1]) {
                case IDAT_TYPE:
                    return chunk[0];
                case IEND_TYPE:
                    return 0;
                case PLTE_TYPE:
                    parsePaletteChunk(chunk[0]);
                    break;
                case tRNS_TYPE:
                    parseTransparencyChunk(chunk[0]);
                    break;
                default:
                    skip(chunk[0]);
            }
            int crc = stream.readInt();
        }
    }

    @Override
    public void dispose() {
    }

    private ImageStorage.ImageType getType() {
        switch (colorType) {
            case PNG_COLOR_GRAY:
                return tRNS_present
                        ? ImageStorage.ImageType.GRAY_ALPHA
                        : ImageStorage.ImageType.GRAY;
            case PNG_COLOR_RGB:
                return tRNS_present
                        ? ImageStorage.ImageType.RGBA
                        : ImageStorage.ImageType.RGB;
            case PNG_COLOR_PALETTE:
                return ImageStorage.ImageType.PALETTE;
            case PNG_COLOR_GRAY_ALPHA:
                return ImageStorage.ImageType.GRAY_ALPHA;
            case PNG_COLOR_RGB_ALPHA:
                return ImageStorage.ImageType.RGBA;
            default: // unreacheble
                throw new RuntimeException();
        }
    }

    private void doSubFilter(byte line[], int bpp) {
        int l = line.length;
        for (int i = bpp; i != l; ++i) {
            line[i] = (byte) (line[i] + line[i - bpp]);
        }
    }

    private void doUpFilter(byte line[], byte pline[]) {
        int l = line.length;
        for (int i = 0; i != l; ++i) {
            line[i] = (byte) (line[i] + pline[i]);
        }
    }

    private void doAvrgFilter(byte line[], byte pline[], int bpp) {
        int l = line.length;
        for (int i = 0; i != bpp; ++i) {
            line[i] = (byte) (line[i] + (pline[i] & 0xFF) / 2);
        }
        for (int i = bpp; i != l; ++i) {
            line[i] = (byte) (line[i]
                    + (((line[i - bpp] & 0xFF) + (pline[i] & 0xFF))) / 2);
        }
    }

    private static int paethPr(int a, int b, int c) {
        // int p = a + b - c
        int pa = Math.abs(b - c);      // p-a
        int pb = Math.abs(a - c);      // p-b
        int pc = Math.abs(b - c + a - c);  // p-c
        return (pa <= pb && pa <= pc) ? a : (pb <= pc) ? b : c;
    }

    private void doPaethFilter(byte line[], byte pline[], int bpp) {
        int l = line.length;
        for (int i = 0; i != bpp; ++i) {
            line[i] = (byte) (line[i] + pline[i]);
        }
        for (int i = bpp; i != l; ++i) {
            line[i] = (byte) (line[i]
                    + paethPr(line[i - bpp] & 0xFF, pline[i] & 0xFF, pline[i - bpp] & 0xFF));
        }
    }

    private void doFilter(byte line[], byte pline[], int fType, int bpp) {
        switch (fType) {
            case PNG_FILTER_SUB:
                doSubFilter(line, bpp);
                break;
            case PNG_FILTER_UP:
                doUpFilter(line, pline);
                break;
            case PNG_FILTER_AVERAGE:
                doAvrgFilter(line, pline, bpp);
                break;
            case PNG_FILTER_PAETH:
                doPaethFilter(line, pline, bpp);
                break;
        }
    }

    private void downsample16to8trns_gray(byte line[], byte image[], int pos, int step) {
        int l = line.length / 2;
        for (int i = 0, oPos = pos; i < l; oPos += step * 2, ++i) {
            int gray16 = (short) ((line[i * 2] & 0xFF) * 256 + (line[i * 2 + 1] & 0xFF));
            image[oPos + 0] = line[i * 2];
            image[oPos + 1] = (gray16 == trnsG) ? 0 : (byte) 255;
        }
    }

    private void downsample16to8trns_rgb(byte line[], byte image[], int pos, int step) {
        int l = line.length / 2 / 3;
        for (int i = 0, oPos = pos; i < l; oPos += step * 4, ++i) {
            int iPos = i * 6;
            int r16 = (short) ((line[iPos + 0] & 0xFF) * 256 + (line[iPos + 1] & 0xFF));
            int g16 = (short) ((line[iPos + 2] & 0xFF) * 256 + (line[iPos + 3] & 0xFF));
            int b16 = (short) ((line[iPos + 4] & 0xFF) * 256 + (line[iPos + 5] & 0xFF));
            image[oPos + 0] = line[iPos + 0];
            image[oPos + 1] = line[iPos + 2];
            image[oPos + 2] = line[iPos + 4];
            image[oPos + 3] =
                    (r16 == trnsR && g16 == trnsG && b16 == trnsB) ? 0 : (byte) 255;
        }
    }

    private void downsample16to8_plain(byte line[], byte image[], int pos, int step, int bpp) {
        int l = (line.length / 2 / bpp) * bpp, stepBpp = step * bpp;
        for (int i = 0, oPos = pos; i != l; oPos += stepBpp, i += bpp) {
            for (int b = 0; b != bpp; ++b) {
                image[oPos + b] = line[(i + b) * 2];
            }
        }
    }

    private void downsample16to8(byte line[], byte image[], int pos, int step, int bpp) {
        if (!tRNS_GRAY_RGB) {
            downsample16to8_plain(line, image, pos, step, bpp);
        } else if (colorType == PNG_COLOR_GRAY) {
            downsample16to8trns_gray(line, image, pos, step);
        } else if (colorType == PNG_COLOR_RGB) {
            downsample16to8trns_rgb(line, image, pos, step);
        }
    }

    private void copyTrns_gray(byte line[], byte image[], int pos, int step) {
        byte tG = (byte) trnsG;
        for (int i = 0, oPos = pos, l = line.length; i < l; oPos += 2 * step, ++i) {
            byte gray = line[i];
            image[oPos] = gray;
            image[oPos + 1] = (gray == tG) ? 0 : (byte) 255;
        }
    }

    private void copyTrns_rgb(byte line[], byte image[], int pos, int step) {
        byte tR = (byte) trnsR, tG = (byte) trnsG, tB = (byte) trnsB;
        int l = line.length / 3;
        for (int i = 0, oPos = pos; i < l; oPos += step * 4, ++i) {
            byte r = line[i * 3], g = line[i * 3 + 1], b = line[i * 3 + 2];
            image[oPos + 0] = r;
            image[oPos + 1] = g;
            image[oPos + 2] = b;
            image[oPos + 3] = (r == tR && g == tG && b == tB) ? 0 : (byte) 255;
        }
    }

    private void copy_plain(byte line[], byte image[], int pos, int step, int bpp) {
        int l = line.length, stepBpp = step * bpp;
        for (int i = 0, oPos = pos; i != l; oPos += stepBpp, i += bpp) {
            for (int b = 0; b != bpp; ++b) {
                image[oPos + b] = line[i + b];
            }
        }
    }

    private void copy(byte line[], byte image[], int pos, int step, int resultBpp) {
        if (!tRNS_GRAY_RGB) {
            if (step == 1) {
                System.arraycopy(line, 0, image, pos, line.length);
            } else {
                copy_plain(line, image, pos, step, resultBpp);
            }
        } else if (colorType == PNG_COLOR_GRAY) {
            copyTrns_gray(line, image, pos, step); // resultBpp==2
        } else if (colorType == PNG_COLOR_RGB) {
            copyTrns_rgb(line, image, pos, step); // resultBpp==4
        }
    }

    private void upsampleTo8Palette(byte line[], byte image[], int pos, int w, int step) {
        int samplesInByte = 8 / bitDepth;
        int maxV = (1 << bitDepth) - 1;
        for (int i = 0, k = 0; i < w; k++, i += samplesInByte) {
            int p = (w - i < samplesInByte) ? w - i : samplesInByte;
            int in = line[k] >> (samplesInByte - p) * bitDepth;
            for (int pp = p - 1; pp >= 0; --pp) {
                image[pos + (i + pp) * step] = (byte) (in & maxV);
                in >>= bitDepth;
            }
        }
    }

    private void upsampleTo8Gray(byte line[], byte image[], int pos, int w, int step) {
        int samplesInByte = 8 / bitDepth;
        int maxV = (1 << bitDepth) - 1, hmaxV = maxV / 2;
        for (int i = 0, k = 0; i < w; k++, i += samplesInByte) {
            int p = (w - i < samplesInByte) ? w - i : samplesInByte;
            int in = line[k] >> (samplesInByte - p) * bitDepth;
            for (int pp = p - 1; pp >= 0; --pp) {
                image[pos + (i + pp) * step] = (byte) (((in & maxV) * 255 + hmaxV) / maxV);
                in >>= bitDepth;
            }
        }
    }

    private void upsampleTo8GrayTrns(byte line[], byte image[], int pos, int w, int step) {
        int samplesInByte = 8 / bitDepth;
        int maxV = (1 << bitDepth) - 1, hmaxV = maxV / 2;
        for (int i = 0, k = 0; i < w; k++, i += samplesInByte) {
            int p = (w - i < samplesInByte) ? w - i : samplesInByte;
            int in = line[k] >> (samplesInByte - p) * bitDepth;
            for (int pp = p - 1; pp >= 0; --pp) {
                int idx = pos + (i + pp) * step * 2;
                int value = in & maxV;
                image[idx] = (byte) ((value * 255 + hmaxV) / maxV);
                image[idx + 1] = value == trnsG ? 0 : (byte) 255;
                in >>= bitDepth;
            }
        }
    }

    private void upsampleTo8(byte line[], byte image[], int pos, int w, int step, int bpp) {
        if (colorType == PNG_COLOR_PALETTE) { // as is decoder
            upsampleTo8Palette(line, image, pos, w, step);
        } else if (bpp == 1) {
            upsampleTo8Gray(line, image, pos, w, step);
        } else if (tRNS_GRAY_RGB && bpp == 2) {
            upsampleTo8GrayTrns(line, image, pos, w, step);
        }
    }

    private static final int starting_y[] = {0, 0, 4, 0, 2, 0, 1, 0};
    private static final int starting_x[] = {0, 4, 0, 2, 0, 1, 0, 0};
    private static final int increment_y[] = {8, 8, 8, 4, 4, 2, 2, 1};
    private static final int increment_x[] = {8, 8, 4, 4, 2, 2, 1, 1};

    private static int mipSize(int size, int mip, int start[], int increment[]) {
        return (size - start[mip] + increment[mip] - 1) / increment[mip];
    }

    private static int mipPos(int pos, int mip, int start[], int increment[]) {
        return start[mip] + pos * increment[mip];
    }

    private void loadMip(byte image[], InputStream data, int mip) throws IOException {

        int mipWidth = mipSize(width, mip, starting_x, increment_x);
        int mipHeight = mipSize(height, mip, starting_y, increment_y);

        int scanLineSize = (mipWidth * bitDepth * numBandsPerColorType[colorType] + 7) / 8;
        byte scanLine0[] = new byte[scanLineSize];
        byte scanLine1[] = new byte[scanLineSize];

        // numBands might be more than numBandsPerColorType[colorType]
        // to support tRNS
        int resultBpp = bpp(), srcBpp = numBandsPerColorType[colorType] * bytesPerColor();

        for (int y = 0; y != mipHeight; ++y) {
            int filterByte = data.read();
            if (filterByte == -1) {
                throw new EOFException();
            }

            if (data.read(scanLine0) != scanLineSize) {
                throw new EOFException();
            }

            doFilter(scanLine0, scanLine1, filterByte, srcBpp);

            int pos = (mipPos(y, mip, starting_y, increment_y) * width + starting_x[mip]) * resultBpp;
            int step = increment_x[mip];

            if (bitDepth == 16) {
                downsample16to8(scanLine0, image, pos, step, resultBpp);
            } else if (bitDepth < 8) {
                upsampleTo8(scanLine0, image, pos, mipWidth, step, resultBpp);
            } else {
                copy(scanLine0, image, pos, step, resultBpp);
            }

            byte scanLineSwp[] = scanLine0;
            scanLine0 = scanLine1;
            scanLine1 = scanLineSwp;
        }
    }

    private void load(byte image[], InputStream data) throws IOException {
        if (isInterlaced) {
            for (int mip = 0; mip != 7; ++mip) {
                if (width > starting_x[mip] && height > starting_y[mip]) {
                    loadMip(image, data, mip);
                }
            }
        } else {
            loadMip(image, data, 7);
        }
    }

    private ImageFrame decodePalette(byte srcImage[], ImageMetadata metadata) throws IOException {
        int bpp = tRNS_present ? 4 : 3;
        if (width >= (Integer.MAX_VALUE / height / bpp)) {
            throw new IOException("Bad PNG image size!");
        }
        byte newImage[] = new byte[width * height * bpp];
        int l = width * height;

        if (tRNS_present) {
            for (int i = 0, j = 0; i != l; j += 4, i++) {
                int index = 0xFF & srcImage[i];
                newImage[j + 0] = palette[0][index];
                newImage[j + 1] = palette[1][index];
                newImage[j + 2] = palette[2][index];
                newImage[j + 3] = palette[3][index];
            }
        } else {
            for (int i = 0, j = 0; i != l; j += 3, i++) {
                int index = 0xFF & srcImage[i];
                newImage[j + 0] = palette[0][index];
                newImage[j + 1] = palette[1][index];
                newImage[j + 2] = palette[2][index];
            }
        }

        ImageStorage.ImageType type = tRNS_present
                ? ImageStorage.ImageType.RGBA
                : ImageStorage.ImageType.RGB;

        return new ImageFrame(type, ByteBuffer.wrap(newImage), width, height,
                width * bpp, null, metadata);
    }

    // we won`t decode palette on fly, we will do it later
    // it is possible that we might want original paletteized image
    // ImageFrame does not support 16 bit color depth,
    // numBandsPerColorType == bytesPerColorType
    // but we will convert RGB->RGBA and L->LA on order to support tRNS
    private int bpp() {
        return numBandsPerColorType[colorType] + (tRNS_GRAY_RGB ? 1 : 0);
    }

    private int bytesPerColor() {
        return bitDepth == 16 ? 2 : 1;
    }

    @Override
    public ImageFrame load(int imageIndex, int rWidth, int rHeight,
            boolean preserveAspectRatio, boolean smooth) throws IOException {

        if (imageIndex != 0) {
            return null;
        }

        int dataSize = parsePngMeta();

        if (dataSize == 0) {
            emitWarning("No image data in PNG");
            return null;
        }

        int bpp = bpp();
        if (width >= (Integer.MAX_VALUE / height / bpp)) {
            throw new IOException("Bad PNG image size!");
        }

        int[] outWH = ImageTools.computeDimensions(width, height, rWidth, rHeight, preserveAspectRatio);
        rWidth = outWH[0];
        rHeight = outWH[1];

        ImageMetadata metaData = new ImageMetadata(null, true,
                null, null, null, null, null, rWidth, rHeight, null, null, null);
        updateImageMetadata(metaData);

        ByteBuffer bb = ByteBuffer.allocate(bpp * width * height);

        PNGIDATChunkInputStream iDat = new PNGIDATChunkInputStream(stream, dataSize);
        Inflater inf = new Inflater();
        InputStream data = new BufferedInputStream(new InflaterInputStream(iDat, inf));

        try {
            load(bb.array(), data);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inf != null) {
                inf.end();
            }
        }

        ImageFrame imgPNG = colorType == PNG_COLOR_PALETTE
                ? decodePalette(bb.array(), metaData)
                : new ImageFrame(getType(), bb, width, height, bpp * width, palette, metaData);

        if (width != rWidth || height != rHeight) {
            imgPNG = ImageTools.scaleImageFrame(imgPNG, rWidth, rHeight, smooth);
        }

        return imgPNG;
    }
}

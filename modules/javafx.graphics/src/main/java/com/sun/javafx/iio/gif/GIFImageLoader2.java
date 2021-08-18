/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.gif;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.iio.common.ImageLoaderImpl;
import com.sun.javafx.iio.common.ImageTools;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/*
 * loader implementation for GIF89 file format
 */

public class GIFImageLoader2 extends ImageLoaderImpl {

    static final byte FILE_SIG87[] = {'G', 'I', 'F', '8', '7', 'a'};
    static final byte FILE_SIG89[] = {'G', 'I', 'F', '8', '9', 'a'};
    static final byte NETSCAPE_SIG[] = {'N', 'E', 'T', 'S', 'C', 'A', 'P', 'E', '2', '.', '0'};
    static final int DEFAULT_FPS = 25;

    InputStream stream = null;
    int screenW, screenH, bgColor;
    byte globalPalette[][];  // r,g,b,a
    byte image[];
    int loopCount = 1;

    public GIFImageLoader2(InputStream input) throws IOException {
        super(GIFDescriptor.getInstance());
        this.stream = input;
        readGlobalHeader();
    }

    // read GIF file header
    private void readGlobalHeader() throws IOException {
        byte signature[] = readBytes(new byte[6]);
        if (!Arrays.equals(FILE_SIG87, signature) && !Arrays.equals(FILE_SIG89, signature)) {
            throw new IOException("Bad GIF signature!");
        }
        screenW = readShort();
        screenH = readShort();
        int cInfo = readByte();
        bgColor = readByte();
        int aspectR = readByte();

        if ((cInfo & 0x80) != 0) {
            globalPalette = readPalete(2 << (cInfo & 7), -1);
        }
        image = new byte[screenW * screenH * 4];
    }

    // read palette data from the stream
    private byte[][] readPalete(int size, int trnsIndex) throws IOException {
        byte palette[][] = new byte[4][size];
        byte paletteData[] = readBytes(new byte[size*3]);
        for (int i = 0, idx = 0; i != size; ++i) {
            for (int k = 0; k != 3; ++k) {
                palette[k][i] = paletteData[idx++];
            }
            palette[3][i] = (i == trnsIndex) ? 0 : (byte)0xFF;
        }
        return palette;
    }

    // skip an extension
    private void consumeAnExtension() throws IOException {
        for (int blSize = readByte(); blSize != 0; blSize = readByte()) {
            skipBytes(blSize);
        }
    }

    private void readAppExtension() throws IOException {
        int size = readByte();
        byte buf[] = readBytes(new byte[size]);
        if (Arrays.equals(NETSCAPE_SIG, buf)) {
            for (int subBlockSize = readByte(); subBlockSize != 0; subBlockSize = readByte()) {
                byte subBlock[] = readBytes(new byte[subBlockSize]);
                int subBlockId = subBlock[0];
                if (subBlockSize == 3 && subBlockId == 1) { // loop count extension
                    loopCount = (subBlock[1] & 0xff) | ((subBlock[2] & 0xff) << 8);
                }
            }
        } else {
            consumeAnExtension(); // read data sub-blocks
        }
    }

    // reads Image Control extension information
    // returns ((pField & 0x1F) << 24) + (trnsIndex << 16) + frameDelay;
    private int readControlCode() throws IOException {
        int size = readByte();
        int pField = readByte();
        int frameDelay = readShort();
        int trnsIndex = readByte();

        if (size != 4 || readByte() != 0) {
            throw new IOException("Bad GIF GraphicControlExtension");
        }
        return ((pField & 0x1F) << 24) + (trnsIndex << 16) + frameDelay;
    }

    // The method waits until image data in the stream
    // The method also reads and return Image Control extension information
    // returns -1 if EOF reached or the value of readControlCode
    private int waitForImageFrame() throws IOException {
        int controlData = 0;
        while (true) {
            int ch = stream.read();
            switch (ch) {
                case 0x2C:
                    return controlData;
                case 0x21:
                    switch (readByte()) {
                        case 0xF9:
                            controlData = readControlCode();
                            break;
                        case 0xFF:
                            readAppExtension();
                            break;
                        default:
                            consumeAnExtension();
                    }
                    break;
                case -1: case 0x3B: // EOF or end of GIF
                    return -1;
                default:
                    throw new IOException("Unexpected GIF control characher 0x"
                            + String.format("%02X", ch));
            }
        }
    }

    // Decode the one frame of GIF form the input stread using internal LZWDecoder class
    private void decodeImage(byte image[], int w, int h, int interlace[]) throws IOException {
        LZWDecoder dec = new LZWDecoder();
        byte data[] = dec.getString();
        int y = 0, iPos = 0, xr = w;
        while (true) {
            int len = dec.readString();
            if (len == -1) { // end of stream
                dec.waitForTerminator();
                return;
            }
            for (int pos = 0; pos != len;) {
                int ax = xr < (len - pos) ? xr : (len - pos);
                System.arraycopy(data, pos, image, iPos, ax);
                iPos += ax;
                pos += ax;
                if ((xr -= ax) == 0) {
                    if (++y == h) { // image is full
                        dec.waitForTerminator();
                        return;
                    }
                    int iY = interlace == null ? y : interlace[y];
                    iPos = iY * w;
                    xr = w;
                }
            }
        }
    }

    // computes row re-index for interlaced case
    private int[] computeInterlaceReIndex(int h) {
        int data[] = new int[h], pos = 0;
        for (int i = 0; i < h; i += 8) data[pos++] = i;
        for (int i = 4; i < h; i += 8) data[pos++] = i;
        for (int i = 2; i < h; i += 4) data[pos++] = i;
        for (int i = 1; i < h; i += 2) data[pos++] = i;
        return data;
    }

    // loads next image frame or null if no more
    public ImageFrame load(int imageIndex, int width, int height, boolean preserveAspectRatio, boolean smooth) throws IOException {
        int imageControlCode = waitForImageFrame();

        if (imageControlCode < 0) {
            return null;
        }

        int left = readShort(), top = readShort(), w = readShort(), h = readShort();

        // check if the image is in the virtual screen boundaries
        if (left + w > screenW || top + h > screenH) {
            throw new IOException("Wrong GIF image frame size");
        }

        int imgCtrl = readByte();

        boolean isTRNS = ((imageControlCode >>> 24) & 1) == 1;
        int trnsIndex = isTRNS ? (imageControlCode >>> 16) & 0xFF : -1;
        boolean localPalette = (imgCtrl & 0x80) != 0;
        boolean isInterlaced = (imgCtrl & 0x40) != 0;

        byte palette[][] = localPalette ? readPalete(2 << (imgCtrl & 7), trnsIndex) : globalPalette;

        int[] outWH = ImageTools.computeDimensions(screenW, screenH, width, height, preserveAspectRatio);
        width = outWH[0];
        height = outWH[1];

        ImageMetadata metadata = updateMetadata(width, height, imageControlCode & 0xFFFF);

        int disposalCode = (imageControlCode >>> 26) & 7;
        byte pImage[] = new byte[w * h];
        decodeImage(pImage, w, h, isInterlaced ? computeInterlaceReIndex(h) : null);

        ByteBuffer img = decodePalette(pImage, palette, trnsIndex,
                left, top, w, h, disposalCode);

        if (screenW != width || screenH != height) {
            img = ImageTools.scaleImage(img, screenW, screenH, 4,
                    width, height, smooth);
        }

        return new ImageFrame(ImageStorage.ImageType.RGBA, img,
                width, height, width * 4, null, metadata);
    }

    // IO helpers
    private int readByte() throws IOException {
        int ch = stream.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    private int readShort() throws IOException {
        int lsb = readByte(), msb = readByte();
        return lsb + (msb << 8);
    }

    private byte[] readBytes(byte data[]) throws IOException {
        return readBytes(data, 0, data.length);
    }

    private byte[] readBytes(byte data[], int offs, int size) throws IOException {
        while (size > 0) {
            int sz = stream.read(data, offs, size);
            if (sz < 0) {
                throw new EOFException();
            }
            offs += sz;
            size -= sz;
        }
        return data;
    }

    private void skipBytes(int n) throws IOException {
        ImageTools.skipFully(stream, n);
    }

    public void dispose() {}

    // GIF specification states that restore to background should fill the frame
    // with background color, but actually all modern programs fill with transparent color.
    private void restoreToBackground(byte img[], int left, int top, int w, int h) {
        for (int y = 0; y != h; ++y) {
            int iPos = ((top + y) * screenW + left) * 4;
            for (int x = 0; x != w; iPos += 4, ++x) {
                img[iPos + 3] = 0;
            }
        }
    }

    // decode palletized image into RGBA
    private ByteBuffer decodePalette(byte[] srcImage, byte[][] palette, int trnsIndex,
            int left, int top, int w, int h, int disposalCode) {

        byte img[] = (disposalCode == 3) ? image.clone() : image;

        for (int y = 0; y != h; ++y) {
            int iPos = ((top + y) * screenW + left) * 4;
            int i = y * w;
            if (trnsIndex < 0) {
                for (int x = 0; x != w; iPos += 4, ++x) {
                    int index = 0xFF & srcImage[i + x];
                    img[iPos + 0] = palette[0][index];
                    img[iPos + 1] = palette[1][index];
                    img[iPos + 2] = palette[2][index];
                    img[iPos + 3] = palette[3][index];
                }
            } else {
                for (int x = 0; x != w; iPos += 4, ++x) {
                    int index = 0xFF & srcImage[i + x];
                    if (index != trnsIndex) {
                        img[iPos + 0] = palette[0][index];
                        img[iPos + 1] = palette[1][index];
                        img[iPos + 2] = palette[2][index];
                        img[iPos + 3] = palette[3][index];
                    }
                }
            }
        }

        if (disposalCode != 3) img = img.clone();
        if (disposalCode == 2) restoreToBackground(image, left, top, w, h);

        return ByteBuffer.wrap(img);
    }

    // fill metadata
    private ImageMetadata updateMetadata(int w, int h, int delayTime) {
        ImageMetadata metaData = new ImageMetadata(null, true, null, null, null,
                delayTime != 0 ? delayTime*10 : 1000/DEFAULT_FPS, loopCount, w, h, null, null, null);
        updateImageMetadata(metaData);
        return metaData;
    }

    class LZWDecoder {
        private final int initCodeSize, clearCode, eofCode;
        private int codeSize, codeMask, tableIndex, oldCode;

        // input data buffer
        private int blockLength = 0, blockPos = 0;
        private byte block[] = new byte[255];
        private int inData = 0, inBits = 0;

        // table
        private int[] prefix = new int[4096];
        private byte[] suffix = new byte[4096];
        private byte[] initial = new byte[4096];
        private int[] length = new int[4096];
        private byte[] string = new byte[4096];

        public LZWDecoder() throws IOException {
            initCodeSize = readByte();
            clearCode = 1 << initCodeSize;
            eofCode = clearCode + 1;
            initTable();
        }

        // decode next string of data, which can be accessed by getString() method
        public final int readString() throws IOException {
            int code = getCode();
            if (code == eofCode) {
                return -1;
            } else if (code == clearCode) {
                initTable();
                code = getCode();
                if (code == eofCode) {
                    return -1;
                }
            } else {
                int newSuffixIndex;
                if (code < tableIndex) {
                    newSuffixIndex = code;
                } else { // code == tableIndex
                    newSuffixIndex = oldCode;
                    if (code != tableIndex) {
                        throw new IOException("Bad GIF LZW: Out-of-sequence code!");
                    }
                }

                if (tableIndex < 4096) {
                    int ti = tableIndex;
                    int oc = oldCode;

                    prefix[ti] = oc;
                    suffix[ti] = initial[newSuffixIndex];
                    initial[ti] = initial[oc];
                    length[ti] = length[oc] + 1;

                    ++tableIndex;
                    if ((tableIndex == (1 << codeSize)) && (tableIndex < 4096)) {
                        ++codeSize;
                        codeMask = (1 << codeSize) - 1;
                    }
                }
            }
            // Reverse code
            int c = code;
            int len = length[c];
            for (int i = len - 1; i >= 0; i--) {
                string[i] = suffix[c];
                c = prefix[c];
            }

            oldCode = code;
            return len;
        }

        // data accessor, the data length returned by readString method
        public final byte[] getString() { return string; }

        // waits until data ends
        public final void waitForTerminator() throws IOException {
            consumeAnExtension();
        }

        // initialize LZW dctionary
        private void initTable() {
            int numEntries = 1 << initCodeSize;
            for (int i = 0; i < numEntries; i++) {
                prefix[i] = -1;
                suffix[i] = (byte) i;
                initial[i] = (byte) i;
                length[i] = 1;
            }

            // fill in the entire table for robustness against
            // out-of-sequence codes.
            for (int i = numEntries; i < 4096; i++) {
                prefix[i] = -1;
                length[i] = 1;
            }

            codeSize = initCodeSize + 1;
            codeMask = (1 << codeSize) - 1;
            tableIndex = numEntries + 2;
            oldCode = 0;
        }

        // reads codeSize bits from the stream
        private int getCode()  throws IOException  {
            while (inBits < codeSize) {
                inData |= nextByte() << inBits;
                inBits += 8;
            }
            int code = inData & codeMask;
            inBits -= codeSize;
            inData >>>= codeSize;
            return code;
        }

        // reads next in byte
        private int nextByte() throws IOException {
            if (blockPos == blockLength) {
                readData();
            }
            return (int)block[blockPos++] & 0xFF;
        }

        // reads next block if data
        private void readData() throws IOException {
            blockPos = 0;
            blockLength = readByte();
            if (blockLength > 0) {
                readBytes(block, 0, blockLength);
            } else {
                throw new EOFException();
            }
        }
    }
}

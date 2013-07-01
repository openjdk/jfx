/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.iio.ImageStorage.ImageType;
import com.sun.javafx.iio.common.ImageLoaderImpl;
import com.sun.javafx.iio.common.ImageTools;
import com.sun.javafx.iio.common.PushbroomScaler;
import com.sun.javafx.iio.common.ScalerFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GIFImageLoader extends ImageLoaderImpl {

    // The current ImageInputStream source.
    InputStream stream = null;
    // Per-stream settings
    // True if the file header including stream metadata has been read.
    boolean gotHeader = false;
    boolean skipTransparency = false;
    // Global metadata, read once per input setting.
    GIFStreamMetadata streamMetadata = null;
    // The current image index
    int currIndex = -1;
    // Metadata for image at 'currIndex', or null.
    GIFImageMetadata imageMetadata = null;
    // A List of Longs indicating the stream positions of the
    // start of the metadata for each image.  Entries are added
    // as needed.
    List imageStartPosition = new ArrayList();
    // Length of metadata for image at 'currIndex', valid only if
    // imageMetadata != null.
//    int imageMetadataLength;
    // The number of images in the stream, if known, otherwise -1.
    int numImages = -1;
    // Variables used by the LZW decoding process
    byte[] block = new byte[255];
    int blockLength = 0;
    int bitPos = 0;
    int nextByte = 0;
    int initCodeSize;
    int clearCode;
    int eofCode;
    // 32-bit lookahead buffer
    int next32Bits = 0;
    // Try if the end of the data blocks has been found,
    // and we are simply draining the 32-bit buffer
    boolean lastBlockFound = false;
    // The raw image type.
    ImageType sourceType;
    // The image to be written.
    ImageFrame theImage = null;
    boolean firstImageflag = true;
    // The image's tile.
    byte[] theTile = null;
    // The image dimensions (from the stream).
    int width = -1, height = -1;
    int fullImageWidth = -1;
    int fullImageHeight = -1;
    int fullBytesPerRow = -1;
    // The pixel currently being decoded (in the stream's coordinates).
    int streamX = -1, streamY = -1;
    // The number of rows decoded
    int rowsDone = 0;
    // The current interlace pass, starting with 0.
    int interlacePass = 0;
    // End per-stream settings
    // Load parameters.
    int destWidth;
    int destHeight;
    boolean isScaling; // derived parameter - downscaling flag
    // End load parameters
    // Scaler
    PushbroomScaler scaler;
    // Constants used to control interlacing.
    static final int[] interlaceIncrement = {8, 8, 4, 2, -1};
    static final int[] interlaceOffset = {0, 4, 2, 1, -1};

    static short readUnsignedShort(InputStream stream) throws IOException {
        return (short) (stream.read() | (stream.read() << 8));
    }

    public GIFImageLoader(InputStream input) throws IOException {
        super(GIFDescriptor.getInstance());
        if(input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        this.stream = input;
    }


    public byte[][] getPalette() {
        byte[] colorTable;
        if (imageMetadata.localColorTable != null) {
            colorTable = imageMetadata.localColorTable;
        } else {
            colorTable = streamMetadata.globalColorTable;
        }

        // Normalize color table length to 2^1, 2^2, 2^4, or 2^8
        int length = colorTable.length / 3;
        int bits;
        if (length == 2) {
            bits = 1;
        } else if (length == 4) {
            bits = 2;
        } else if (length == 8 || length == 16) {
            // Bump from 3 to 4 bits
            bits = 4;
        } else {
            // Bump to 8 bits
            bits = 8;
        }
        int lutLength = 1 << bits;
        byte[] r = new byte[lutLength];
        byte[] g = new byte[lutLength];
        byte[] b = new byte[lutLength];

        // Entries from length + 1 to lutLength - 1 will be 0
        int rgbIndex = 0;
        for (int i = 0; i < length; i++) {
            r[i] = colorTable[rgbIndex++];
            g[i] = colorTable[rgbIndex++];
            b[i] = colorTable[rgbIndex++];
        }

        byte[] a = null;
        if (imageMetadata.transparentColorFlag) {
            a = new byte[lutLength];
            Arrays.fill(a, (byte) 255);

            // Some files erroneously have a transparent color index
            // of 255 even though there are fewer than 256 colors.
            int idx = Math.min(imageMetadata.transparentColorIndex,
                    lutLength - 1);
            a[idx] = (byte) 0;
        }

        int[] bitsPerSample = new int[1];
        bitsPerSample[0] = bits;
        byte[][] palette = new byte[a != null ? 4 : 3][];
        palette[0] = r;
        palette[1] = g;
        palette[2] = b;
        if (a != null) {
            palette[3] = a;
        }

        return palette;
    }

    // BEGIN LZW STUFF
    private void initNext32Bits() {
        next32Bits = block[0] & 0xff;
        next32Bits |= (block[1] & 0xff) << 8;
        next32Bits |= (block[2] & 0xff) << 16;
        next32Bits |= block[3] << 24;
        nextByte = 4;
    }

    // Load a block (1-255 bytes) at a time, and maintain
    // a 32-bit lookahead buffer that is filled from the left
    // and extracted from the right.
    //
    // When the last block is found, we continue to
    //
    private int getCode(int codeSize, int codeMask) throws IOException {
        if (bitPos + codeSize > 32) {
            return eofCode; // No more data available
        }

        int code = (next32Bits >> bitPos) & codeMask;
        bitPos += codeSize;

        // Shift in a byte of new data at a time
        while (bitPos >= 8 && !lastBlockFound) {
            next32Bits >>>= 8;
            bitPos -= 8;

            // Check if current block is out of bytes
            if (nextByte >= blockLength) {
                // Get next block size
                blockLength = stream.read();
                if (blockLength == 0) {
                    lastBlockFound = true;
                    return code;
                } else {
                    int left = blockLength;
                    int off = 0;
                    while (left > 0) {
                        int nbytes = ImageTools.readFully(stream, block, off, left);
                        off += nbytes;
                        left -= nbytes;
                    }
                    nextByte = 0;
                }
            }

            next32Bits |= block[nextByte++] << 24;
        }

        return code;
    }

    public void initializeStringTable(int[] prefix,
            byte[] suffix,
            byte[] initial,
            int[] length) {
        int numEntries = 1 << initCodeSize;
        for (int i = 0; i < numEntries; i++) {
            prefix[i] = -1;
            suffix[i] = (byte) i;
            initial[i] = (byte) i;
            length[i] = 1;
        }

        // Fill in the entire table for robustness against
        // out-of-sequence codes.
        for (int i = numEntries; i < 4096; i++) {
            prefix[i] = -1;
            length[i] = 1;
        }
    }
    Rectangle sourceRegion;
    int sourceXSubsampling;
    int sourceYSubsampling;
    int sourceMinProgressivePass;
    int sourceMaxProgressivePass;
    Point2D destinationOffset;
    Rectangle destinationRegion;
    // Used only if IIOReadUpdateListeners are present
    int updateMinY;
    int updateYStep;
    boolean decodeThisRow = true;
    int destY = 0;
    byte[] rowBuf;
    byte[] destBuf;
    boolean doneScaling = false;

    private void outputRow() {
        // Clip against ImageReadParam
        int clippedWidth = Math.min(sourceRegion.width,
                destinationRegion.width * sourceXSubsampling);
        int destX = destinationRegion.x * (theImage.getStride() / theImage.getWidth());

        //Read
        System.arraycopy(theTile, destY * theImage.getStride() + destX, destBuf, 0, destBuf.length);

        if (!(imageMetadata.interlaceFlag && this.isScaling)) {
            //Modify
            ImageTools.convert(clippedWidth, 1, sourceType,
                    rowBuf, 0, rowBuf.length,
                    destBuf, 0, destBuf.length,
                    getPalette(), imageMetadata.transparentColorIndex, skipTransparency);
        }

        //Write
        if (sourceXSubsampling == 1) {
            if (!imageMetadata.interlaceFlag) {
                if (this.isScaling) {
                    if (!this.doneScaling) {
                        doneScaling = scaler.putSourceScanline(destBuf, 0);
                    }
                } else {
                    System.arraycopy(destBuf, 0, theTile, destY * theImage.getStride() + destX,
                            destBuf.length);
                }
            } else {
                byte[] buf = this.isScaling ? rowBuf : destBuf;
                System.arraycopy(buf, 0, theTile, destY * theImage.getStride() + destX,
                        buf.length);
            }
        } else {
            assert false;
            for (int x = 0; x < clippedWidth; x += sourceXSubsampling, destX++) {
                theTile[destY * theImage.getStride() + destX] = rowBuf[x];
            }
        }

    }

    private void computeDecodeThisRow() {
        this.decodeThisRow =
                (destY < destinationRegion.y + destinationRegion.height) &&
                (streamY >= sourceRegion.y) &&
                (streamY < sourceRegion.y + sourceRegion.height) &&
                (((streamY - sourceRegion.y) % sourceYSubsampling) == 0);
    }

    private void outputPixels(byte[] string, int len) {
        if (interlacePass < sourceMinProgressivePass ||
                interlacePass > sourceMaxProgressivePass) {
            return;
        }

        for (int i = 0; i < len; i++) {
            if (streamX >= sourceRegion.x) {
                rowBuf[streamX - sourceRegion.x] = string[i];
            }

            // Process end-of-row
            ++streamX;
            if (streamX == width) {
                ++rowsDone;
                updateImageProgress(100.0F * rowsDone / height);

                if (decodeThisRow) {
                    outputRow();
                }

                streamX = 0;
                if (imageMetadata.interlaceFlag) {
                    streamY += interlaceIncrement[interlacePass];
                    if (streamY >= height) {
                        ++interlacePass;
                        if (interlacePass > sourceMaxProgressivePass) {
                            return;
                        }
                        streamY = interlaceOffset[interlacePass];
                        startPass();
                    }
                } else {
                    ++streamY;
                }

                // Determine whether pixels from this row will
                // be written to the destination
                this.destY = destinationRegion.y +
                        (streamY - sourceRegion.y) / sourceYSubsampling;
                computeDecodeThisRow();
            }
        }
    }

    // END LZW STUFF
    private void readHeader() throws IOException {
        if (gotHeader) {
            return;
        }
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }

        // Create an object to store the stream metadata
        this.streamMetadata = new GIFStreamMetadata();

        try {
            byte[] signature = new byte[6];
            stream.read(signature);

            StringBuffer version = new StringBuffer(3);
            version.append((char) signature[3]);
            version.append((char) signature[4]);
            version.append((char) signature[5]);
            streamMetadata.version = version.toString();

            streamMetadata.logicalScreenWidth = readUnsignedShort(stream);
            streamMetadata.logicalScreenHeight = readUnsignedShort(stream);

            int packedFields = stream.read();
            boolean globalColorTableFlag = (packedFields & 0x80) != 0;
            streamMetadata.colorResolution = ((packedFields >> 4) & 0x7) + 1;
            streamMetadata.sortFlag = (packedFields & 0x8) != 0;
            int numGCTEntries = 1 << ((packedFields & 0x7) + 1);

            streamMetadata.backgroundColorIndex = stream.read();
            streamMetadata.pixelAspectRatio = stream.read();

            if (globalColorTableFlag) {
                streamMetadata.globalColorTable = new byte[3 * numGCTEntries];
                ImageTools.readFully(stream, streamMetadata.globalColorTable);
            } else {
                streamMetadata.globalColorTable = null;
            }
        } catch (IOException e) {
            IOException ex = new IOException("I/O error reading header!");
            ex.initCause(e);
            throw ex;
        }

        gotHeader = true;
    }

    // Read blocks of 1-255 bytes, stop at a 0-length block
    private byte[] concatenateBlocks() throws IOException {
        byte[] data = new byte[0];
        while (true) {
            int length = stream.read();
            if (length == 0) {
                break;
            }
            byte[] newData = new byte[data.length + length];
            System.arraycopy(data, 0, newData, 0, data.length);
            ImageTools.readFully(stream, newData, data.length, length);
            data = newData;
        }

        return data;
    }

    // Stream must be positioned at start of metadata for next
    // Graphics Control Extension.
    private boolean readMetadata() throws IOException {
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }

        boolean isEndOfImageStream = false;

        try {
            // Create an object to store the image metadata
            this.imageMetadata = new GIFImageMetadata();
            while (!isEndOfImageStream) {
                int blockType = stream.read();
                if (blockType == 0x2c) { // Image Descriptor
                    imageMetadata.imageLeftPosition =
                            readUnsignedShort(stream);
                    imageMetadata.imageTopPosition =
                            readUnsignedShort(stream);
                    imageMetadata.imageWidth = readUnsignedShort(stream);
                    imageMetadata.imageHeight = readUnsignedShort(stream);

                    int idPackedFields = stream.read();
                    boolean localColorTableFlag =
                            (idPackedFields & 0x80) != 0;
                    imageMetadata.interlaceFlag = (idPackedFields & 0x40) != 0;
                    imageMetadata.sortFlag = (idPackedFields & 0x20) != 0;
                    int numLCTEntries = 1 << ((idPackedFields & 0x7) + 1);

                    if (localColorTableFlag) {
                        // Read color table if any
                        imageMetadata.localColorTable =
                                new byte[3 * numLCTEntries];
                        ImageTools.readFully(stream, imageMetadata.localColorTable);
                    } else {
                        imageMetadata.localColorTable = null;
                    }

                    // Now positioned at start of LZW-compressed pixels
                    return true;
                } else if (blockType == 0x21) { // Extension block
                    int label = stream.read();

                    if (label == 0xf9) { // Graphics Control Extension
                        int gceLength = stream.read(); // 4
                        int gcePackedFields = stream.read();
                        imageMetadata.disposalMethod =
                                (gcePackedFields >> 2) & 0x3;
                        imageMetadata.userInputFlag =
                                (gcePackedFields & 0x2) != 0;
                        imageMetadata.transparentColorFlag =
                                (gcePackedFields & 0x1) != 0;

                        imageMetadata.delayTime = readUnsignedShort(stream);
                        imageMetadata.transparentColorIndex = stream.read();

                        int terminator = stream.read();
                    } else if (label == 0x1) { // Plain text extension
                        int length = stream.read();
                        imageMetadata.hasPlainTextExtension = true;
                        imageMetadata.textGridLeft =
                                readUnsignedShort(stream);
                        imageMetadata.textGridTop =
                                readUnsignedShort(stream);
                        imageMetadata.textGridWidth =
                                readUnsignedShort(stream);
                        imageMetadata.textGridHeight =
                                readUnsignedShort(stream);
                        imageMetadata.characterCellWidth =
                                stream.read();
                        imageMetadata.characterCellHeight =
                                stream.read();
                        imageMetadata.textForegroundColor =
                                stream.read();
                        imageMetadata.textBackgroundColor =
                                stream.read();
                        imageMetadata.text = concatenateBlocks();
                    } else if (label == 0xfe) { // Comment extension
                        byte[] comment = concatenateBlocks();
                        if (imageMetadata.comments == null) {
                            imageMetadata.comments = new ArrayList<byte[]>();
                        }
                        imageMetadata.comments.add(comment);
                    } else if (label == 0xff) { // Application extension
                        int blockSize = stream.read();
                        byte[] applicationID = new byte[8];
                        byte[] authCode = new byte[3];

                        // read available data
                        byte[] blockData = new byte[blockSize];
                        stream.read(blockData);

                        int offset = copyData(blockData, 0, applicationID);
                        offset = copyData(blockData, offset, authCode);

                        byte[] applicationData = concatenateBlocks();

                        if (offset < blockSize) {
                            int len = blockSize - offset;
                            byte[] data =
                                    new byte[len + applicationData.length];

                            System.arraycopy(blockData, offset, data, 0, len);
                            System.arraycopy(applicationData, 0, data, len,
                                    applicationData.length);

                            applicationData = data;
                        }

                        // Init lists if necessary
                        if (imageMetadata.applicationIDs == null) {
                            imageMetadata.applicationIDs = new ArrayList<byte[]>();
                            imageMetadata.authenticationCodes =
                                    new ArrayList<byte[]>();
                            imageMetadata.applicationData = new ArrayList<byte[]>();
                        }
                        imageMetadata.applicationIDs.add(applicationID);
                        imageMetadata.authenticationCodes.add(authCode);
                        imageMetadata.applicationData.add(applicationData);
                    } else {
                        // Skip over unknown extension blocks
                        int length = 0;
                        do {
                            length = stream.read();
                            stream.skip(length);
                        } while (length > 0);
                    }
                } else if (blockType == 0x3b) { // Trailer
                    isEndOfImageStream = true;
                } else {
                    throw new IOException("Unexpected block type " +
                            blockType + "!");
                }
            }
        } catch (EOFException eofe) {
            isEndOfImageStream = true;
        } catch (IOException ioe) {
            IOException ex = new IOException("I/O error reading image metadata!");
            ex.initCause(ioe);
            throw ex;
        }

        return !isEndOfImageStream;
    }

    private int copyData(byte[] src, int offset, byte[] dst) {
        int len = dst.length;
        int rest = src.length - offset;
        if (len > rest) {
            len = rest;
        }
        System.arraycopy(src, offset, dst, 0, len);
        return offset + len;
    }

    private void startPass() {
        int y = 0;
        int yStep = 1;
        if (imageMetadata.interlaceFlag) {
            y = interlaceOffset[interlacePass];
            yStep = interlaceIncrement[interlacePass];
        }

        int[] vals = ImageTools.computeUpdatedPixels(sourceRegion,
                destinationOffset,
                destinationRegion.x,
                destinationRegion.y,
                destinationRegion.x +
                destinationRegion.width - 1,
                destinationRegion.y +
                destinationRegion.height - 1,
                sourceXSubsampling,
                sourceYSubsampling,
                0,
                y,
                destinationRegion.width,
                (destinationRegion.height + yStep - 1) / yStep,
                1,
                yStep);

        // Initialized updateMinY and updateYStep
        this.updateMinY = vals[1];
        this.updateYStep = vals[5];

    }

    public ImageFrame read(int imageIndex, int destWidth, int destHeight, boolean preserveAspectRatio, boolean smooth)
            throws IOException {
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }

        //Header Contains Global Color Table, Spec String
        readHeader();

        //GCE, image desc, and other chunk metadata
        if (!readMetadata()) {
            return null;
        }

        // Save source image layout, these can vary with update images,
        // Because each image frame has a GCE and Image Desc
        this.width = imageMetadata.imageWidth;
        this.height = imageMetadata.imageHeight;
        this.sourceType = imageMetadata.transparentColorFlag ? ImageType.PALETTE_TRANS : ImageType.PALETTE;

        // Determine output image dimensions.
        int[] widthHeight = ImageTools.computeDimensions(width, height, destWidth, destHeight, preserveAspectRatio);
        this.destWidth = destWidth = widthHeight[0];
        this.destHeight = destHeight = widthHeight[1];

        // Set scaling flag.
        this.isScaling = destWidth != width || destHeight != height;

        // Set up metadata for ImageFrame.
        Integer backgroundIndex = null;
        if (streamMetadata.globalColorTable != null) {
            backgroundIndex = streamMetadata.backgroundColorIndex;
        }
        Integer transparentIndex = null;
        if (imageMetadata.transparentColorFlag) {
            transparentIndex = imageMetadata.transparentColorIndex;
        }

        ImageMetadata theMetadata = new ImageMetadata(null, true,
                backgroundIndex, null, transparentIndex,
                10 * imageMetadata.delayTime,
                imageMetadata.imageWidth, imageMetadata.imageHeight,
                imageMetadata.imageLeftPosition, imageMetadata.imageTopPosition,
                imageMetadata.disposalMethod);
        updateImageMetadata(theMetadata);

        // Set up image to be read into. If the source is interlaced and scaling
        // is being done, this will be an intermediate image; otherwise it will
        // be the final image.
        ImageType destType = ImageTools.getConvertedType(sourceType);
        int numDestBands = ImageStorage.getNumBands(destType);
        ImageType type;
        int bytesPerRow;
        ByteBuffer buffer;
        int w;
        int h;
        byte[][] palette = null;
        ImageMetadata md = theMetadata;
        if (isScaling) {
            scaler = ScalerFactory.createScaler(width, height, numDestBands,
                    destWidth, destHeight, smooth);
            if (imageMetadata.interlaceFlag) {
                type = sourceType;
                if (!firstImageflag) {
                    w = fullImageWidth;
                    h = fullImageHeight;
                    bytesPerRow = fullBytesPerRow;
                } else {
                    w = width;
                    h = height;
                    bytesPerRow = 4 * ((width + 3) / 4);//width * numDestBands;
                    //4 * ((width + 3) / 4);
                }
                palette = getPalette();
                md = theMetadata;
                buffer = ByteBuffer.wrap(new byte[height * bytesPerRow]);
            } else {
                type = destType;
                w = destWidth;
                h = destHeight;
                bytesPerRow = destWidth * numDestBands;
                buffer = scaler.getDestination();
            }
        } else {
            type = destType;
            if (!firstImageflag) {
                w = fullImageWidth;
                h = fullImageHeight;
                bytesPerRow = fullBytesPerRow;
            } else {
                w = destWidth;
                h = destHeight;
                bytesPerRow = destWidth * numDestBands;
            }
            buffer = ByteBuffer.wrap(new byte[h * bytesPerRow]);
        }

        //If this is the first image, we create an image frame and set appropriate
        //values, otherwise we DEEP copy from the previous image and create an
        //ImageFrame based from the previous image data.
        if (firstImageflag) {
            theImage = new ImageFrame(type, buffer, w, h, bytesPerRow, palette, md);
            this.fullImageHeight = h;
            this.fullImageWidth = w;
            this.fullBytesPerRow = bytesPerRow;
            this.sourceRegion = new Rectangle(this.width, this.height);
            this.destinationOffset = new Point2D();
            this.destinationRegion = new Rectangle(this.width, this.height);
            firstImageflag = false;
        } else {
            //Deep Copy
            System.arraycopy((byte[])theImage.getImageData().array(), 0, buffer.array(), 0, buffer.array().length);
            theImage = new ImageFrame(type, buffer, w, h, bytesPerRow, palette, md);
            this.sourceRegion = new Rectangle(this.width, this.height);
            this.destinationOffset = new Point2D(md.imageLeftPosition , md.imageTopPosition);
            this.destinationRegion = new Rectangle(md.imageLeftPosition , md.imageTopPosition, this.width, this.height);
            skipTransparency = true;
        }
        this.sourceXSubsampling = 1;
        this.sourceYSubsampling = 1;
        this.sourceMinProgressivePass = 0;
        this.sourceMaxProgressivePass = 3;

        this.theTile = ((ByteBuffer) theImage.getImageData()).array();
        this.streamX = 0;
        this.streamY = 0;
        this.rowsDone = 0;
        this.interlacePass = 0;

        this.destY = destinationRegion.y +
                (streamY - sourceRegion.y) / sourceYSubsampling;
        computeDecodeThisRow();

        // Inform IIOReadProgressListeners of start of image
        updateImageProgress(0.0F);
        startPass();

        this.rowBuf = new byte[width];
        this.destBuf = new byte[width * numDestBands];

        try {
            // Read and decode the image data, fill in theImage
            this.initCodeSize = stream.read();

            // Read first data block
            this.blockLength = stream.read();
            int left = blockLength;
            int off = 0;
            while (left > 0) {
                int nbytes = ImageTools.readFully(stream, block, off, left);
                left -= nbytes;
                off += nbytes;
            }

            this.bitPos = 0;
            this.nextByte = 0;
            this.lastBlockFound = false;
            this.interlacePass = 0;

            // Init 32-bit buffer
            initNext32Bits();

            this.clearCode = 1 << initCodeSize;
            this.eofCode = clearCode + 1;

            int code, oldCode = 0;

            int[] prefix = new int[4096];
            byte[] suffix = new byte[4096];
            byte[] initial = new byte[4096];
            int[] length = new int[4096];
            byte[] string = new byte[4096];

            initializeStringTable(prefix, suffix, initial, length);
            int tableIndex = (1 << initCodeSize) + 2;
            int codeSize = initCodeSize + 1;
            int codeMask = (1 << codeSize) - 1;

            while (true) {
                code = getCode(codeSize, codeMask);

                if (code == clearCode) {
                    initializeStringTable(prefix, suffix, initial, length);
                    tableIndex = (1 << initCodeSize) + 2;
                    codeSize = initCodeSize + 1;
                    codeMask = (1 << codeSize) - 1;

                    code = getCode(codeSize, codeMask);
                    if (code == eofCode) {
                        // Inform IIOReadProgressListeners of end of image
                        updateImageProgress(100.0F);
                        return theImage;
                    }
                } else if (code == eofCode) {
                    // Process the intermediate image if interlaced and scaling.
                    if (imageMetadata.interlaceFlag && this.isScaling) {
                        // Downscale the image.
                        ByteBuffer bb = (ByteBuffer) theImage.getImageData();
                        byte[] b = bb.array();
                        int stride = theImage.getStride();
                        int offset = 0;
                        byte[] scanline = new byte[width * numDestBands];
                        for (int y = 0; y < height; y++) {
                            ImageTools.convert(width, 1, sourceType,
                                    b, offset, stride, scanline, 0, 0,
                                    palette, transparentIndex, false);
                            if (scaler.putSourceScanline(scanline, 0)) {
                                break;
                            }
                            offset += stride;
                        }
                        theImage = new ImageFrame(destType, scaler.getDestination(),
                                destWidth, destHeight, destWidth * numDestBands,
                                null, md);
                    }
                    // Inform IIOReadProgressListeners of end of image
                    updateImageProgress(100.0F);
                    return theImage;
                } else {
                    int newSuffixIndex;
                    if (code < tableIndex) {
                        newSuffixIndex = code;
                    } else { // code == tableIndex
                        newSuffixIndex = oldCode;
                        if (code != tableIndex) {
                            // warning - code out of sequence
                            // possibly data corruption
                            emitWarning("Out-of-sequence code!");
                        }
                    }

                    int ti = tableIndex;
                    int oc = oldCode;

                    prefix[ti] = oc;
                    suffix[ti] = initial[newSuffixIndex];
                    initial[ti] = initial[oc];
                    length[ti] = length[oc] + 1;

                    ++tableIndex;
                    if ((tableIndex == (1 << codeSize)) &&
                            (tableIndex < 4096)) {
                        ++codeSize;
                        codeMask = (1 << codeSize) - 1;
                    }
                }

                // Reverse code
                int c = code;
                int len = length[c];
                for (int i = len - 1; i >= 0; i--) {
                    string[i] = suffix[c];
                    c = prefix[c];
                }

                outputPixels(string, len);
                oldCode = code;
            }

//            processReadAborted();
//            return theImage;
        } catch (IOException e) {
            IOException ex = new IOException("Error reading GIF image data");
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * Remove all settings including global settings such as
     * <code>Locale</code>s and listeners, as well as stream settings.
     */
    public void reset() {
//        super.reset();
        resetStreamSettings();
    }

    /**
     * Remove local settings based on parsing of a stream.
     */
    private void resetStreamSettings() {
        gotHeader = false;
        streamMetadata = null;
        currIndex = -1;
        imageMetadata = null;
        imageStartPosition = new ArrayList();
        numImages = -1;

        // No need to reinitialize 'block'
        blockLength = 0;
        bitPos = 0;
        nextByte = 0;

        next32Bits = 0;
        lastBlockFound = false;

        theImage = null;
        theTile = null;
        width = -1;
        height = -1;
        streamX = -1;
        streamY = -1;
        rowsDone = 0;
        interlacePass = 0;
    }

    public void dispose() {
        // no-op
    }

    public ImageFrame load(int imageIndex, int width, int height, boolean preserveAspectRatio, boolean smooth) throws IOException {
        return read(imageIndex, width, height, preserveAspectRatio, smooth);
    }

}

/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.iio.ImageStorage.ImageType;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;

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
//    public static PixelFormat getPixelFormat(ImageType type) {
//        PixelFormat format;
//        switch (type) {
//            case GRAY:
//                format = PixelFormat.BYTE_GRAY;
//                break;
//            case GRAY_ALPHA:
//            case GRAY_ALPHA_PRE:
//                format = PixelFormat.BYTE_RGBA_PRE;
//                break;
//            case PALETTE:
//                format = PixelFormat.BYTE_RGB;
//                break;
//            case PALETTE_ALPHA:
//            case PALETTE_ALPHA_PRE:
//                format = PixelFormat.BYTE_RGBA_PRE;
//                break;
//            case RGB:
//                format = PixelFormat.BYTE_RGB;
//                break;
//            case RGBA:
//            case RGBA_PRE:
//                format = PixelFormat.BYTE_RGBA_PRE;
//                break;
//            default:
//                // This should not be possible ...
//                throw new IllegalArgumentException("Unknown ImageType " + type);
//        }
//
//        return format;
//    }
//    public static boolean isConversionACopy(ImageType type, PixelFormat format) {
//        return (type == ImageType.GRAY && format == PixelFormat.BYTE_GRAY) ||
//                (type == ImageType.RGB && format == PixelFormat.BYTE_RGB) ||
//                (type == ImageType.RGBA_PRE && format == PixelFormat.BYTE_RGBA_PRE);
//    }
    public static ImageType getConvertedType(ImageType type) {
        ImageType retType = type;
        switch (type) {
            case GRAY:
                retType = ImageType.GRAY;
                break;
            case GRAY_ALPHA:
            case GRAY_ALPHA_PRE:
            case PALETTE_ALPHA:
            case PALETTE_ALPHA_PRE:
            case PALETTE_TRANS:
            case RGBA:
                retType = ImageType.RGBA_PRE;
                break;
            case PALETTE:
            case RGB:
                retType = ImageType.RGB;
                break;
            case RGBA_PRE:
                retType = ImageType.RGBA_PRE;
                break;
            default:
                throw new IllegalArgumentException("Unsupported ImageType " + type);
        }
        return retType;
    }

    public static byte[] createImageArray(ImageType type, int width, int height) {
        int numBands = 0;
        switch (type) {
            case GRAY:
            case PALETTE:
            case PALETTE_ALPHA:
            case PALETTE_ALPHA_PRE:
                numBands = 1;
                break;
            case GRAY_ALPHA:
            case GRAY_ALPHA_PRE:
                numBands = 2;
                break;
            case RGB:
                numBands = 3;
                break;
            case RGBA:
            case RGBA_PRE:
                numBands = 4;
                break;
            default:
                throw new IllegalArgumentException("Unsupported ImageType " + type);
        }
        return new byte[width * height * numBands];
    }

    public static ImageFrame convertImageFrame(ImageFrame frame) {
        ImageFrame retFrame;
        ImageType type = frame.getImageType();
        ImageType convertedType = getConvertedType(type);
        if (convertedType == type) {
            retFrame = frame;
        } else {
            byte[] inArray = null;
            Buffer buf = frame.getImageData();
            if (!(buf instanceof ByteBuffer)) {
                throw new IllegalArgumentException("!(frame.getImageData() instanceof ByteBuffer)");
            }
            ByteBuffer bbuf = (ByteBuffer) buf;
            if (bbuf.hasArray()) {
                inArray = bbuf.array();
            } else {
                inArray = new byte[bbuf.capacity()];
                bbuf.get(inArray);
            }
            int width = frame.getWidth();
            int height = frame.getHeight();
            int inStride = frame.getStride();
            byte[] outArray = createImageArray(convertedType, width, height);
            ByteBuffer newBuf = ByteBuffer.wrap(outArray);
            int outStride = outArray.length / height;
            byte[][] palette = frame.getPalette();
            ImageMetadata metadata = frame.getMetadata();
            int transparentIndex = metadata.transparentIndex != null ? metadata.transparentIndex : 0;
            convert(width, height, type,
                    inArray, 0, inStride, outArray, 0, outStride,
                    palette, transparentIndex, false);
            ImageMetadata imd = new ImageMetadata(metadata.gamma,
                    metadata.blackIsZero, null,
                    metadata.backgroundColor, null,
                    metadata.delayTime, metadata.loopCount,
                    metadata.imageWidth, metadata.imageHeight,
                    metadata.imageLeftPosition, metadata.imageTopPosition,
                    metadata.disposalMethod);
            retFrame = new ImageFrame(convertedType, newBuf, width, height,
                    outStride, null, imd);
        }
        return retFrame;
    }

    public static byte[] convert(int width, int height, ImageType inputType,
            byte[] input, int inputOffset, int inRowStride,
            byte[] output, int outputOffset, int outRowStride,
            byte[][] palette, int transparentIndex, boolean skipTransparent) {
        //
        // Take care of the layouts that are a direct copy.
        //
        if (inputType == ImageType.GRAY ||
                inputType == ImageType.RGB ||
                inputType == ImageType.RGBA_PRE) {
            if (input != output) {
                int bytesPerRow = width;
                if (inputType == ImageType.RGB) {
                    bytesPerRow *= 3;
                } else if (inputType == ImageType.RGBA_PRE) {
                    bytesPerRow *= 4;
                }
                if (height == 1) {
                    System.arraycopy(input, inputOffset, output, outputOffset, bytesPerRow);
                } else {
                    int inRowOffset = inputOffset;
                    int outRowOffset = outputOffset;
                    for (int row = 0; row < height; row++) {
                        System.arraycopy(input, inRowOffset, output, outRowOffset, bytesPerRow);
                        inRowOffset += inRowStride;
                        outRowOffset += outRowStride;
                    }
                }
            }
        } else if (inputType == ImageType.GRAY_ALPHA || inputType == ImageType.GRAY_ALPHA_PRE) {
            int inOffset = inputOffset;
            int outOffset = outputOffset;
            if (inputType == ImageType.GRAY_ALPHA) {
                for (int y = 0; y < height; y++) {
                    int inOff = inOffset;
                    int outOff = outOffset;
                    for (int x = 0; x < width; x++) {
                        // copy input to local variables in case operating in place
                        byte gray = input[inOff++];
                        int alpha = input[inOff++] & 0xff;
                        float f = alpha / 255.0F;
                        gray = (byte) (f * (gray & 0xff));
                        output[outOff++] = gray;
                        output[outOff++] = gray;
                        output[outOff++] = gray;
                        output[outOff++] = (byte) alpha;
                    }
                    inOffset += inRowStride;
                    outOffset += outRowStride;
                }
            } else {
                for (int y = 0; y < height; y++) {
                    int inOff = inOffset;
                    int outOff = outOffset;
                    for (int x = 0; x < width; x++) {
                        // copy input to local variables in case operating in place
                        byte gray = input[inOff++];
                        output[outOff++] = gray;
                        output[outOff++] = gray;
                        output[outOff++] = gray;
                        output[outOff++] = input[inOff++];
                    }
                    inOffset += inRowStride;
                    outOffset += outRowStride;
                }
            }
        } else if (inputType == ImageType.PALETTE) {
            int inOffset = inputOffset;
            int outOffset = outputOffset;
            byte[] red = palette[0];
            byte[] green = palette[1];
            byte[] blue = palette[2];
            int inOff = inOffset;
            int outOff = outOffset;
            //loop through the scanline and mask for the value if each byte.
            //the byte is an index into the palette array for that pixel.
            for (int x = 0; x < width; x++) {
                int index = (input[inOff++] & 0xff);

                output[outOff++] = red[index];
                output[outOff++] = green[index];
                output[outOff++] = blue[index];

                outOffset += outRowStride;
            }
        } else if (inputType == ImageType.PALETTE_ALPHA) {
            int inOffset = inputOffset;
            int outOffset = outputOffset;
            byte[] red = palette[0];
            byte[] green = palette[1];
            byte[] blue = palette[2];
            byte[] alpha = palette[3];
                int inOff = inOffset;
                int outOff = outOffset;
                for (int x = 0; x < width; x++) {
                    int index = input[inOff++] & 0xff;
                    byte r = red[index];
                    byte g = green[index];
                    byte b = blue[index];
                    int a = alpha[index] & 0xff;
                    float f = a / 255.0F;
                    output[outOff++] = (byte) (f * (r & 0xff));
                    output[outOff++] = (byte) (f * (g & 0xff));
                    output[outOff++] = (byte) (f * (b & 0xff));
                    output[outOff++] = (byte) a;
                }
                inOffset += inRowStride;
                outOffset += outRowStride;
        } else if (inputType == ImageType.PALETTE_ALPHA_PRE) {
            int inOffset = inputOffset;
            int outOffset = outputOffset;
            byte[] red = palette[0];
            byte[] green = palette[1];
            byte[] blue = palette[2];
            byte[] alpha = palette[3];
            for (int y = 0; y < height; y++) {
                int inOff = inOffset;
                int outOff = outOffset;
                for (int x = 0; x < width; x++) {
                    int index = input[inOff++] & 0xff;
                    output[outOff++] = red[index];
                    output[outOff++] = green[index];
                    output[outOff++] = blue[index];
                    output[outOff++] = alpha[index];
                }
                inOffset += inRowStride;
                outOffset += outRowStride;
            }
        } else if (inputType == ImageType.PALETTE_TRANS) {
            int inOffset = inputOffset;
            int outOffset = outputOffset;
            for (int y = 0; y < height; y++) {
                int inOff = inOffset;
                int outOff = outOffset;
                byte[] red = palette[0];
                byte[] green = palette[1];
                byte[] blue = palette[2];
                for (int x = 0; x < width; x++) {
                    int index = input[inOff++] & 0xff;
                    if (index == transparentIndex) {
                        if (skipTransparent) {
                            outOff+=4;
                        } else {
                            output[outOff++] = (byte) 0;
                            output[outOff++] = (byte) 0;
                            output[outOff++] = (byte) 0;
                            output[outOff++] = (byte) 0;
                        }
                    } else {
                        output[outOff++] = red[index];
                        output[outOff++] = green[index];
                        output[outOff++] = blue[index];
                        output[outOff++] = (byte) 255;
                    }
                }
                inOffset += inRowStride;
                outOffset += outRowStride;
            }
        } else if (inputType == ImageType.RGBA) {
            int inOffset = inputOffset;
            int outOffset = outputOffset;
            for (int y = 0; y < height; y++) {
                int inOff = inOffset;
                int outOff = outOffset;
                for (int x = 0; x < width; x++) {
                    // copy input to local variables in case operating in place
                    byte red = input[inOff++];
                    byte green = input[inOff++];
                    byte blue = input[inOff++];
                    int alpha = input[inOff++] & 0xff;
                    float f = alpha / 255.0F;
                    output[outOff++] = (byte) (f * (red & 0xff));
                    output[outOff++] = (byte) (f * (green & 0xff));
                    output[outOff++] = (byte) (f * (blue & 0xff));
                    output[outOff++] = (byte) alpha;
                }
//                System.arraycopy(input, inOffset, output, outOffset, width*4);
                inOffset += inRowStride;
                outOffset += outRowStride;
            }
        } else {
            throw new UnsupportedOperationException("Unsupported ImageType " +
                    inputType);
        }

        return output;
    }

    public static String getScaledImageName(String path) {
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
        result.append("@2x");
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

    // Helper for computeUpdatedPixels method
    private static void computeUpdatedPixels(int sourceOffset,
            int sourceExtent,
            int destinationOffset,
            int dstMin,
            int dstMax,
            int sourceSubsampling,
            int passStart,
            int passExtent,
            int passPeriod,
            int[] vals,
            int offset) {
        // We need to satisfy the congruences:
        // dst = destinationOffset + (src - sourceOffset)/sourceSubsampling
        //
        // src - passStart == 0 (mod passPeriod)
        // src - sourceOffset == 0 (mod sourceSubsampling)
        //
        // subject to the inequalities:
        //
        // src >= passStart
        // src < passStart + passExtent
        // src >= sourceOffset
        // src < sourceOffset + sourceExtent
        // dst >= dstMin
        // dst <= dstmax
        //
        // where
        //
        // dst = destinationOffset + (src - sourceOffset)/sourceSubsampling
        //
        // For now we use a brute-force approach although we could
        // attempt to analyze the congruences.  If passPeriod and
        // sourceSubsamling are relatively prime, the period will be
        // their product.  If they share a common factor, either the
        // period will be equal to the larger value, or the sequences
        // will be completely disjoint, depending on the relationship
        // between passStart and sourceOffset.  Since we only have to do this
        // twice per image (once each for X and Y), it seems cheap enough
        // to do it the straightforward way.

        boolean gotPixel = false;
        int firstDst = -1;
        int secondDst = -1;
        int lastDst = -1;

        for (int i = 0; i < passExtent; i++) {
            int src = passStart + i * passPeriod;
            if (src < sourceOffset) {
                continue;
            }
            if ((src - sourceOffset) % sourceSubsampling != 0) {
                continue;
            }
            if (src >= sourceOffset + sourceExtent) {
                break;
            }

            int dst = destinationOffset +
                    (src - sourceOffset) / sourceSubsampling;
            if (dst < dstMin) {
                continue;
            }
            if (dst > dstMax) {
                break;
            }

            if (!gotPixel) {
                firstDst = dst; // Record smallest valid pixel
                gotPixel = true;
            } else if (secondDst == -1) {
                secondDst = dst; // Record second smallest valid pixel
            }
            lastDst = dst; // Record largest valid pixel
        }

        vals[offset] = firstDst;

        // If we never saw a valid pixel, set width to 0
        if (!gotPixel) {
            vals[offset + 2] = 0;
        } else {
            vals[offset + 2] = lastDst - firstDst + 1;
        }

        // The period is given by the difference of any two adjacent pixels
        vals[offset + 4] = Math.max(secondDst - firstDst, 1);
    }

    /**
     * A utility method that computes the exact set of destination
     * pixels that will be written during a particular decoding pass.
     * The intent is to simplify the work done by readers in combining
     * the source region, source subsampling, and destination offset
     * information obtained from the <code>ImageReadParam</code> with
     * the offsets and periods of a progressive or interlaced decoding
     * pass.
     *
     * @param sourceRegion a <code>Rectangle</code> containing the
     * source region being read, offset by the source subsampling
     * offsets, and clipped against the source bounds, as returned by
     * the <code>getSourceRegion</code> method.
     * @param destinationOffset a <code>Point</code> containing the
     * coordinates of the upper-left pixel to be written in the
     * destination.
     * @param dstMinX the smallest X coordinate (inclusive) of the
     * destination <code>Raster</code>.
     * @param dstMinY the smallest Y coordinate (inclusive) of the
     * destination <code>Raster</code>.
     * @param dstMaxX the largest X coordinate (inclusive) of the destination
     * <code>Raster</code>.
     * @param dstMaxY the largest Y coordinate (inclusive) of the destination
     * <code>Raster</code>.
     * @param sourceXSubsampling the X subsampling factor.
     * @param sourceYSubsampling the Y subsampling factor.
     * @param passXStart the smallest source X coordinate (inclusive)
     * of the current progressive pass.
     * @param passYStart the smallest source Y coordinate (inclusive)
     * of the current progressive pass.
     * @param passWidth the width in pixels of the current progressive
     * pass.
     * @param passHeight the height in pixels of the current progressive
     * pass.
     * @param passPeriodX the X period (horizontal spacing between
     * pixels) of the current progressive pass.
     * @param passPeriodY the Y period (vertical spacing between
     * pixels) of the current progressive pass.
     *
     * @return an array of 6 <code>int</code>s containing the
     * destination min X, min Y, width, height, X period and Y period
     * of the region that will be updated.
     */
    public static int[] computeUpdatedPixels(Rectangle sourceRegion,
            Point2D destinationOffset,
            int dstMinX,
            int dstMinY,
            int dstMaxX,
            int dstMaxY,
            int sourceXSubsampling,
            int sourceYSubsampling,
            int passXStart,
            int passYStart,
            int passWidth,
            int passHeight,
            int passPeriodX,
            int passPeriodY) {
        int[] vals = new int[6];
        computeUpdatedPixels(sourceRegion.x, sourceRegion.width,
                (int) (destinationOffset.x + 0.5F),
                dstMinX, dstMaxX, sourceXSubsampling,
                passXStart, passWidth, passPeriodX,
                vals, 0);
        computeUpdatedPixels(sourceRegion.y, sourceRegion.height,
                (int) (destinationOffset.y + 0.5F),
                dstMinY, dstMaxY, sourceYSubsampling,
                passYStart, passHeight, passPeriodY,
                vals, 1);
        return vals;
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
                    finalWidth = (int) ((float) sourceWidth * finalHeight / sourceHeight);
                } else if (finalHeight == 0) {
                    finalHeight = (int) ((float) sourceHeight * finalWidth / sourceWidth);
                } else {
                    float scale = Math.min((float) finalWidth / sourceWidth, (float) finalHeight / sourceHeight);
                    finalWidth = (int) (sourceWidth * scale);
                    finalHeight = (int) (sourceHeight * scale);
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
            if (finalWidth == 0) {
                finalWidth = 1;
            }
            if (finalHeight == 0) {
                finalHeight = 1;
            }
        }


        return new int[]{finalWidth, finalHeight};
    }

    public static ImageFrame scaleImageFrame(ImageFrame src,
            int destWidth, int destHeight, boolean isSmooth)
    {
        int numBands = ImageStorage.getNumBands(src.getImageType());
        ByteBuffer dst = scaleImage((ByteBuffer) src.getImageData(),
                src.getWidth(), src.getHeight(), numBands,
                destWidth, destHeight, isSmooth);
        return new ImageFrame(src.getImageType(), dst,
                destWidth, destHeight, destWidth * numBands, null, src.getMetadata());
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
//    public static final java.awt.image.BufferedImage getAsBufferedImage(Image prismImage) {
//        java.awt.image.BufferedImage image = null;
//
//        int width = prismImage.getWidth();
//        int height = prismImage.getHeight();
//        int scanlineStride = prismImage.getScanlineStride();
//        byte[] pixels = ((java.nio.ByteBuffer) prismImage.getPixelBuffer()).array();
//        switch (prismImage.getPixelFormat()) {
//            case BYTE_GRAY: {
//                image = new java.awt.image.BufferedImage(width, height,
//                        java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
//                java.awt.image.DataBufferByte db =
//                        (java.awt.image.DataBufferByte) image.getRaster().getDataBuffer();
//                byte[] data = db.getData();
//                System.arraycopy(pixels, 0, data, 0, width * height);
//            }
//            break;
//            case BYTE_RGB: {
//                image = new java.awt.image.BufferedImage(width, height,
//                        java.awt.image.BufferedImage.TYPE_3BYTE_BGR);
//                for (int y = 0; y < height; y++) {
//                    int off = y * scanlineStride;
//                    for (int x = 0; x < width; x++) {
//                        int rgb = ((pixels[off++] & 0xff) << 16) |
//                                ((pixels[off++] & 0xff) << 8) |
//                                (pixels[off++] & 0xff);
//                        image.setRGB(x, y, rgb);
//                    }
//                }
//            }
//            break;
//            case BYTE_RGBA_PRE: {
//                image = new java.awt.image.BufferedImage(width, height,
//                        java.awt.image.BufferedImage.TYPE_4BYTE_ABGR_PRE);
////                for (int y = 0; y < height; y++) {
////                    int off = y * scanlineStride;
////                    for (int x = 0; x < width; x++) {
////                        int rgb = ((pixels[off++] & 0xff) << 16) |
////                                ((pixels[off++] & 0xff) << 8) |
////                                (pixels[off++] & 0xff) |
////                                ((pixels[off++] & 0xff) << 24);
////                        image.setRGB(x, y, rgb);
////                    }
////                }
//                java.awt.image.DataBufferByte db =
//                        (java.awt.image.DataBufferByte) image.getRaster().getDataBuffer();
//                byte[] data = db.getData();
//                for (int y = 0; y < height; y++) {
//                    int offPrism = y * scanlineStride;
//                    int offImage = y * width * 4;
//                    for (int x = 0; x < width; x++) {
//                        data[offImage++] = pixels[offPrism + 3]; // A
//                        data[offImage++] = pixels[offPrism + 2]; // B
//                        data[offImage++] = pixels[offPrism + 1]; // G
//                        data[offImage++] = pixels[offPrism];     // R
//                        offPrism += 4;
//                    }
//                }
//            }
//            break;
//            default:
//                throw new UnsupportedOperationException("Unsupported test case " +
//                        prismImage.getPixelFormat());
//        }
//
//        return image;
//    }
}

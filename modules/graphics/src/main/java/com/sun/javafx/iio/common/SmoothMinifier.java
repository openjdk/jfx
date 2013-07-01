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

package com.sun.javafx.iio.common;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A class which implements smooth downscaling for sources acquired line-by-line.
 * Source scanlines are pushed into the algorithm one at a time in sequence from
 * the top to the bottom of the source image. The destination is also populated
 * in line-by-line fashion as soon as enough source lines are available to
 * calculate a given line. The class is used as follows:
 *
 * <pre>
 * SmoothMinifier downscaler = new SmoothMinifier(sourceWidth, sourceHeight, numBands,
 *                                        destWidth, destHeight);
 * for(int i = 0; i < sourceHeight; i++) {
 *     byte[] b = source.getLine(i);
 *     if (shrinker.putSourceScanline(b, 0)) {
 *         break;
 *     }
 * }
 * NIOBuffer destBuf = downscaler.getDestination();
 * </pre>
 *
 * The algorithm used calculates the destination sample in each band by
 * averaging over a box centered on the backward mapped location of the
 * destination pixel. The box has dimensions
 * <code>ceil(sourceWidth/destWidth)&nbsp;x&nbsp;ceil(sourceHeight/destHeight)</code>.
 */
public class SmoothMinifier implements PushbroomScaler {

    protected int sourceWidth; // source width
    protected int sourceHeight; // source height
    protected int numBands; // number of bands
    protected int destWidth; // destination width
    protected int destHeight; // destination height
    protected double scaleY;
    protected ByteBuffer destBuf; // destination image buffer
    protected int boxHeight; // number of rows of pixels over which to average
    protected byte[][] sourceData; // array of source lines
    protected int[] leftPoints; // left interval end points in source
    protected int[] rightPoints; // right interval end points in source
    protected int[] topPoints; // top interval end points in source
    protected int[] bottomPoints; // bottom interval end points in source
    protected int sourceLine;    // current scanline in the source
    protected int sourceDataLine; // current row in the source data array
    protected int destLine;       // current scanline in the destination
    protected int[] tmpBuf; // buffer into which one box of rows is accumulated

    /**
     * Instantiates a new <code>SmoothMinifier</code> object.
     *
     * @param sourceWidth The source image width
     * @param sourceHeight The source image height
     * @param numBands The number of components per pixel in the images
     * @param destWidth The destination image width
     * @param destHeight The destination image height
     * @throws IllegalArgumentException if any of the parameters is non-positive
     * or either destination dimension is greater than the corresponding source
     * dimension.
     */
    SmoothMinifier(int sourceWidth, int sourceHeight, int numBands,
            int destWidth, int destHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || numBands <= 0 ||
                destWidth <= 0 || destHeight <= 0 ||
                destWidth > sourceWidth || destHeight > sourceHeight) {
            throw new IllegalArgumentException();
        }

        // save parameters to instance variables
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.numBands = numBands;
        this.destWidth = destWidth;
        this.destHeight = destHeight;

        // allocate the destination
        this.destBuf = ByteBuffer.wrap(new byte[destHeight * destWidth * numBands]);

        // calculate the destination-to-source scale factors
        double scaleX = (double) sourceWidth / (double) destWidth;
        this.scaleY = (double) sourceHeight / (double) destHeight;

        // calculate the dimensions of the averaging box
        int boxWidth = (sourceWidth + destWidth - 1) / destWidth;
        this.boxHeight = (sourceHeight + destHeight - 1) / destHeight;


        // calculate the number of pixels in the surround, excluding the center
        int boxLeft = boxWidth / 2;
        int boxRight = boxWidth - boxLeft - 1;
        int boxTop = boxHeight / 2;
        int boxBottom = boxHeight - boxTop - 1;

        // allocate memory for source data
        this.sourceData = new byte[boxHeight][destWidth * numBands];

        // calculate the source positions of the points which form the left and
        // right closed bounds of the region contributing to all columns in the
        // destination.
        this.leftPoints = new int[destWidth];
        this.rightPoints = new int[destWidth];
        for (int dx = 0; dx < destWidth; dx++) {
            int sx = (int) (dx * scaleX); // floor
            leftPoints[dx] = sx - boxLeft;
            rightPoints[dx] = sx + boxRight;
        }

        // calculate the source positions of the points which form the top and
        // bottom closed bounds of the region contributing to all rows in the
        // destination.
        this.topPoints = new int[destHeight];
        this.bottomPoints = new int[destHeight];
        for (int dy = 0; dy < destHeight; dy++) {
            int sy = (int) (dy * scaleY); // floor
            topPoints[dy] = sy - boxTop;
            bottomPoints[dy] = sy + boxBottom;
        }

        // initialize line numbers to track source and destination lines
        this.sourceLine = 0;
        this.sourceDataLine = 0;
        this.destLine = 0;

        this.tmpBuf = new int[destWidth * numBands];
    }

    /**
     * Retrieves the destination buffer.
     *
     * @return The destination buffer.
     */
    public ByteBuffer getDestination() {
        return this.destBuf;
    }

    /**
     * Push one scanline of source pixels into the downscaling engine. A smooth
     * downscaling algorithm is used.
     *
     * @param scanline One scanline of source data.
     * @param off The offline into the buffer.
     * @throws IllegalArgumentException if <code>off&nbsp;&lt;&nbsp;0</code>.
     * @return Whether the destination image is complete.
     */
    public boolean putSourceScanline(byte[] scanline, int off) {
        if (off < 0) {
            throw new IllegalArgumentException("off < 0!");
        }

        // XXX Optimize this later:
        // XXX Precalculate transition values from left end to center to right end
        // and use three loops:
        // left = min {i | leftPoints[i] >= 0 ^ rightPoints[i] < W - 1}
        // right = max {i | leftPoints[i] >= 0 ^ rightPoints[i] < W - 1}

        // Horizontally average the data into the intermediate buffer.
        if (numBands == 1) {
            int leftSample = scanline[off] & 0xff;
            int rightSample = scanline[off + sourceWidth - 1] & 0xff;
            for (int i = 0; i < destWidth; i++) {
                int val = 0;
                int rightBound = rightPoints[i];
                for (int j = leftPoints[i]; j <= rightBound; j++) {
                    if (j < 0) {
                        val += leftSample;
                    } else if (j >= sourceWidth) {
                        val += rightSample;
                    } else {
                        val += scanline[off + j] & 0xff;
                    }
                }
                val /= (rightBound - leftPoints[i] + 1);
                sourceData[sourceDataLine][i] = (byte) val;
            }
        } else { // numBands != 1
            int rightOff = off + (sourceWidth - 1) * numBands;
            for (int i = 0; i < destWidth; i++) {
                int leftBound = leftPoints[i];
                int rightBound = rightPoints[i];
                int numPoints = rightBound - leftBound + 1;
                int iBands = i * numBands;
                for (int k = 0; k < numBands; k++) {
                    // XXX For multi-band could loop over bands with "val" becoming an
                    // array "int val[numBands]". left/rightPoints could continue to
                    // point to the first band only and incrementing would be used in
                    // between or left/rightPoints could be used for all bands.
                    int leftSample = scanline[off + k] & 0xff;
                    int rightSample = scanline[rightOff + k] & 0xff;

                    int val = 0;
                    for (int j = leftBound; j <= rightBound; j++) {
                        if (j < 0) {
                            val += leftSample;
                        } else if (j >= sourceWidth) {
                            val += rightSample;
                        } else {
                            val += scanline[off + j * numBands + k] & 0xff;
                        }
//                        } else if (j * numBands + k >= sourceWidth * numBands) {
//                            val += scanline[off + (sourceWidth - 1) * numBands + k] & 0xff;
//                        } else {
//                            val += scanline[off + j * numBands + k] & 0xff;
//                        }
                    }
                    val /= numPoints;
                    sourceData[sourceDataLine][iBands + k] = (byte) val;
                }
            }
        }

        // Compute a destination line if the source has no more data or the
        // last line of the destination has been reached. Note that the last
        // destination line can be reached before the source has been
        // exhausted so the second part of the logical expression waits for
        // the last line of the source to be available.
        if (sourceLine == bottomPoints[destLine] ||
                (destLine == destHeight - 1 && sourceLine == sourceHeight - 1)) {
            // Vertically average the data from the intermediate buffer into
            // the destination
            assert destBuf.hasArray() : "destBuf.hasArray() == false => destBuf is direct";
            byte[] dest = destBuf.array();

            int destOffset = destLine * destWidth * numBands;
            Arrays.fill(tmpBuf, 0);
            for (int y = topPoints[destLine]; y <= bottomPoints[destLine]; y++) {
                int index = 0;
                if (y < 0) {
                    index = 0 - sourceLine + sourceDataLine;
                } else if (y >= sourceHeight) {
                    index = (sourceHeight - 1 - sourceLine + sourceDataLine) % boxHeight;
                } else {
                    index = (y - sourceLine + sourceDataLine) % boxHeight;
                }
                if (index < 0) {
                    index += boxHeight;
                }
                byte[] b = sourceData[index];
                int destLen = b.length;
                for (int x = 0; x < destLen; x++) {
                    tmpBuf[x] += b[x] & 0xff;
                }
            }
            int sourceLen = tmpBuf.length;
            for (int x = 0; x < sourceLen; x++) {
                dest[destOffset + x] = (byte) (tmpBuf[x] / boxHeight);
            }

            if (destLine < destHeight - 1) {
                destLine++;
            }
        }

        // Increment
        if (++sourceLine != sourceHeight) {
            sourceDataLine = (sourceDataLine + 1) % boxHeight;
        }

        return destLine == destHeight;
    }
}

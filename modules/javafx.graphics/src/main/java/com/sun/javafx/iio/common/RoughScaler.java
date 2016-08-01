/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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

public class RoughScaler implements PushbroomScaler {

    protected int numBands; // number of bands
    protected int destWidth; // destination width
    protected int destHeight; // destination height
    protected double scaleY;
    protected ByteBuffer destBuf; // destination image buffer
    protected int[] colPositions; // source column positions (non-smooth only)
    protected int sourceLine;    // current scanline in the source
    protected int nextSourceLine; // next source line which generates a non-smooth output
    protected int destLine;       // current scanline in the destination

    /**
     * Instantiates a new <code>RoughScaler</code> object.
     *
     * @param sourceWidth The source image width
     * @param sourceHeight The source image height
     * @param numBands The number of components per pixel in the images
     * @param destWidth The destination image width
     * @param destHeight The destination image height
     * @throws IllegalArgumentException if any of the parameters is non-positive.
     */
    public RoughScaler(int sourceWidth, int sourceHeight, int numBands,
            int destWidth, int destHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || numBands <= 0 ||
                destWidth <= 0 || destHeight <= 0) {
            throw new IllegalArgumentException();
        }

        // save parameters to instance variables
        this.numBands = numBands;
        this.destWidth = destWidth;
        this.destHeight = destHeight;

        // allocate the destination
        this.destBuf = ByteBuffer.wrap(new byte[destHeight * destWidth * numBands]);

        // calculate the destination-to-source scale factors
        double scaleX = (double) sourceWidth / (double) destWidth;
        this.scaleY = (double) sourceHeight / (double) destHeight;

        this.colPositions = new int[destWidth];
        for (int i = 0; i < destWidth; i++) {
            int pos = (int) ((i + 0.5) * scaleX);
            colPositions[i] = pos * numBands;
        }

        // initialize line numbers to track source and destination lines
        this.sourceLine = 0;
        this.destLine = 0;
        // find nearest source line
        this.nextSourceLine = (int) (0.5 * scaleY);
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
     * Push one scanline of source pixels into the scaling engine. A nearest
     * neighbor interpolation algorithm is used.
     *
     * @param scanline One scanline of source data.
     * @param off The offset into the buffer.
     * @throws IllegalArgumentException if <code>off&nbsp;&lt;&nbsp;0</code>.
     * @return Whether the destination image is complete.
     */
    public boolean putSourceScanline(byte[] scanline, int off) {
        if (off < 0) {
            throw new IllegalArgumentException("off < 0!");
        }

        if (destLine < destHeight) {
            if (sourceLine == nextSourceLine) {
                assert destBuf.hasArray() : "destBuf.hasArray() == false => destBuf is direct";
                byte[] dest = destBuf.array();

                int destOffset = destLine * destWidth * numBands;

                int doff = destOffset;
                for (int i = 0; i < destWidth; i++) {
                    int sourceOffset = off + this.colPositions[i];
                    for (int j = 0; j < numBands; j++) {
                        dest[doff++] = scanline[sourceOffset + j];
                    }
                }

                while ((int) ((++destLine + 0.5) * scaleY) == sourceLine)
                {
                    System.arraycopy(dest, destOffset, dest, doff, destWidth * numBands);
                    doff += destWidth * numBands;
                }
                nextSourceLine = (int) ((destLine + 0.5) * scaleY);
            }

            ++sourceLine;
        }

        return destLine == destHeight;
    }
}

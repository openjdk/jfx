/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.robot;

import java.nio.Buffer;
import java.nio.IntBuffer;

/**
 * This class encapsulates a bucket of pixels stored in IntArgbPre format.
 * 
 */
public class FXRobotImage {
    private final IntBuffer pixelBuffer;
    private final int width;
    private final int height;
    private final int scanlineStride;

    public static FXRobotImage create(Buffer pixelBuffer,
                                      int width, int height, int scanlineStride)
    {
        return new FXRobotImage(pixelBuffer, width, height, scanlineStride);
    }

    private FXRobotImage(Buffer pixelBuffer,
                         int width, int height, int scanlineStride)
    {
        if (pixelBuffer == null) {
            throw new IllegalArgumentException("Pixel buffer must be non-null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be > 0");
        }
        this.pixelBuffer = (IntBuffer)pixelBuffer;
        this.width = width;
        this.height = height;
        this.scanlineStride = scanlineStride;
    }

    /**
     * Returns {@link java.nio.Buffer} which holds the data.
     * 
     * @return {@code Buffer} holding the data for this image
     */
    public Buffer getPixelBuffer() {
        return pixelBuffer;
    }

    /**
     * Width of the image.
     * @return width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Height of the image
     * @return height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns scanline stride of this image in bytes
     * 
     * @return scan line stride in bytes
     */
    public int getScanlineStride() {
        return scanlineStride;
    }

    /**
     * Returns pixel stride of this image in bytes.
     *
     * @return pixel stride in bytes
     */
    public int getPixelStride() {
        return 4;
    }

    /**
     * Returns pixel (in IntArgbPre) format (Argb premultiplied).
     *
     * @param x coordinate
     * @param y coordinate
     * @return pixel in IntArgbPre format
     */
    public int getArgbPre(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("x,y must be >0, <width, height");
        }
        return pixelBuffer.get(x + y*scanlineStride/4);
    }

    /**
     * Returns pixel in IntArgb format (non-premultiplied).
     * 
     * @param x coordinate
     * @param y coordinate
     * @return pixel in IntArgb format
     */
    public int getArgb(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("x,y must be >0, <width, height");
        }
        int argb = pixelBuffer.get(x + y*scanlineStride/4);
        if ((argb >> 24) == -1) {
            return argb;
        }
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xff;
        int g = (argb >>  8) & 0xff;
        int b = (argb      ) & 0xff;
        int a2 = a + (a >> 7);
        r = (r * a2) >> 8;
        g = (g * a2) >> 8;
        b = (b * a2) >> 8;
        return ((a << 24) | (r << 16) | (g << 8) | (b));
    }

    @Override
    public String toString() {
        return super.toString() +
            " [format=INT_ARGB_PRE width=" + width + " height=" + height +
            " scanlineStride=" + scanlineStride  +" pixelStride=" + getPixelStride()+
            " pixelBuffer=" + pixelBuffer + "]";
    }
}

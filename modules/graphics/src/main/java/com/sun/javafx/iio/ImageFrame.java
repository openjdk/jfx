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

package com.sun.javafx.iio;

import com.sun.javafx.iio.ImageStorage.ImageType;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * A class representing the data and metadata of a single image.
 */
public class ImageFrame {
    private ImageType imageType;
    private ByteBuffer imageData;
    private int width;
    private int height;
    private int stride;
    private float pixelScale;
    private byte[][] palette;
    private ImageMetadata metadata;

    /**
     * Create an <code>ImageFrame</code> with a default 72DPI pixel scale.
     *
     * @param imageType The type of image data. The value of this field also
     * implies the number of bands.
     * @param imageData The image data.
     * @param width The image width.
     * @param height The image height.
     * @param stride The stride from a pixel position in one row to the same
     * horizontal position in the next row.
     * @param palette The image palette. This is ignored unless the type is
     * one of the palette types.
     * @param metadata The image metadata.
     */
    public ImageFrame(ImageType imageType, ByteBuffer imageData,
                      int width, int height, int stride, byte[][] palette,
                      ImageMetadata metadata)
    {
        this(imageType, imageData,
             width, height, stride, palette,
             1.0f, metadata);
    }

    /**
     * Create an <code>ImageFrame</code>.
     *
     * @param imageType The type of image data. The value of this field also
     * implies the number of bands.
     * @param imageData The image data.
     * @param width The image width.
     * @param height The image height.
     * @param stride The stride from a pixel position in one row to the same
     * horizontal position in the next row.
     * @param palette The image palette. This is ignored unless the type is
     * one of the palette types.
     * @param pixelScale The scale of a 72DPI virtual pixel in the resolution
     * of the image (1.0f for 72DPI images, 2.0f for 144DPI images, etc.).
     * @param metadata The image metadata.
     */
    public ImageFrame(ImageType imageType, ByteBuffer imageData,
                      int width, int height, int stride, byte[][] palette,
                      float pixelScale, ImageMetadata metadata)
    {
        this.imageType = imageType;
        this.imageData = imageData;
        this.width = width;
        this.height = height;
        this.stride = stride;
        this.palette = palette;
        this.pixelScale = pixelScale;
        this.metadata = metadata;
    }

    public ImageType getImageType() {
        return this.imageType;
    }

    public Buffer getImageData() {
        return this.imageData;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getStride() {
        return this.stride;
    }

    public byte[][] getPalette() {
        return this.palette;
    }

    public void setPixelScale(float pixelScale) {
        this.pixelScale = pixelScale;
    }

    public float getPixelScale() {
        return pixelScale;
    }

    public ImageMetadata getMetadata() {
        return this.metadata;
    }
}

/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.javax;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorageException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

import static com.sun.javafx.iio.ImageStorage.*;
import static java.awt.image.BufferedImage.*;

public final class BufferedImageConverter {

    private record ImageInfo(ImageType imageType, byte[] data) {}

    private BufferedImageConverter() {}

    public static ImageFrame convert(BufferedImage image, ImageMetadata metadata) throws ImageStorageException {
        ImageInfo imageInfo = switch (image.getType()) {
            case TYPE_BYTE_GRAY -> new ImageInfo(ImageType.GRAY, byteData(image));
            case TYPE_3BYTE_BGR -> new ImageInfo(ImageType.RGB, convertBGRtoRGB(byteData(image)));
            case TYPE_4BYTE_ABGR -> new ImageInfo(ImageType.RGBA, convertABGRtoRGBA(byteData(image)));
            case TYPE_4BYTE_ABGR_PRE -> new ImageInfo(ImageType.RGBA_PRE, convertABGRtoRGBA(byteData(image)));
            case TYPE_INT_RGB -> new ImageInfo(ImageType.RGB, convertIntRGBtoRGBA(intData(image)));
            case TYPE_INT_BGR -> new ImageInfo(ImageType.RGB, convertIntBGRtoRGB(intData(image)));
            case TYPE_INT_ARGB -> new ImageInfo(ImageType.RGBA, convertIntARGBtoRGBA(intData(image)));
            case TYPE_INT_ARGB_PRE -> new ImageInfo(ImageType.RGBA_PRE, convertIntARGBtoRGBA(intData(image)));
            default -> throw new ImageStorageException("Unsupported image format: " + image.getType());
        };

        return new ImageFrame(
            imageInfo.imageType(), ByteBuffer.wrap(imageInfo.data()), image.getWidth(), image.getHeight(),
            image.getWidth() * image.getRaster().getNumBands(), null, metadata);
    }

    private static byte[] byteData(BufferedImage image) {
        return ((DataBufferByte)image.getData().getDataBuffer()).getData();
    }

    private static int[] intData(BufferedImage image) {
        return ((DataBufferInt)image.getData().getDataBuffer()).getData();
    }

    static byte[] convertBGRtoRGB(byte[] buffer) {
        for (int i = 0; i < buffer.length; i += 3) {
            byte b = buffer[i], r = buffer[i + 2];
            buffer[i] = r;
            buffer[i + 2] = b;
        }

        return buffer;
    }

    static byte[] convertABGRtoRGBA(byte[] buffer) {
        for (int i = 0; i < buffer.length; i += 4) {
            byte a = buffer[i],
                 b = buffer[i + 1],
                 g = buffer[i + 2],
                 r = buffer[i + 3];
            buffer[i] = r;
            buffer[i + 1] = g;
            buffer[i + 2] = b;
            buffer[i + 3] = a;
        }

        return buffer;
    }

    static byte[] convertIntBGRtoRGB(int[] buffer) {
        byte[] out = new byte[buffer.length * 3];
        for (int i = 0, j = 0; i < buffer.length; ++i, j += 3) {
            int v = buffer[i];
            out[j] = (byte)(v & 0xFF);
            out[j + 1] = (byte)((v >> 8) & 0xFF);
            out[j + 2] = (byte)((v >> 16) & 0xFF);
        }

        return out;
    }

    static byte[] convertIntARGBtoRGBA(int[] buffer) {
        byte[] out = new byte[buffer.length * 4];
        for (int i = 0, j = 0; i < buffer.length; ++i, j += 4) {
            int v = buffer[i];
            out[j] = (byte)((v >> 16) & 0xFF);
            out[j + 1] = (byte)((v >> 8) & 0xFF);
            out[j + 2] = (byte)(v & 0xFF);
            out[j + 3] = (byte)((v >> 24) & 0xFF);
        }

        return out;
    }

    static byte[] convertIntRGBtoRGBA(int[] buffer) {
        byte[] out = new byte[buffer.length * 4];
        for (int i = 0, j = 0; i < buffer.length; ++i, j += 4) {
            int v = buffer[i];
            out[j] = (byte)((v >> 16) & 0xFF);
            out[j + 1] = (byte)((v >> 8) & 0xFF);
            out[j + 2] = (byte)(v & 0xFF);
        }
        return out;
    }

}

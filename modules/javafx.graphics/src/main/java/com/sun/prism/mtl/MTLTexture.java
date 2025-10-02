/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.mtl;

import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseTexture;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

class MTLTexture<T extends MTLTextureData> extends BaseTexture<MTLTextureResource<T>> {

    private final MTLContext context;
    private final long texPtr;

    MTLTexture(MTLContext context, MTLTextureResource<T> resource,
               PixelFormat format, WrapMode wrapMode,
               int physicalWidth, int physicalHeight,
               int contentX, int contentY, int contentWidth, int contentHeight,
               boolean useMipmap) {

        super(resource, format, wrapMode,
              physicalWidth, physicalHeight,
              contentX, contentY, contentWidth, contentHeight, useMipmap);
        this.context = context;
        texPtr = resource.getResource().getResource();
    }

    MTLTexture(MTLContext context, MTLTextureResource<T> resource,
               PixelFormat format, WrapMode wrapMode,
               int physicalWidth, int physicalHeight,
               int contentX, int contentY, int contentWidth, int contentHeight,
               int maxContentWidth, int maxContentHeight, boolean useMipmap) {

        super(resource, format, wrapMode,
              physicalWidth, physicalHeight,
              contentX, contentY, contentWidth, contentHeight,
              maxContentWidth, maxContentHeight, useMipmap);
        this.context = context;
        texPtr = resource.getResource().getResource();
    }

    public long getNativeHandle() {
        return texPtr;
    }

    public MTLContext getContext() {
        return context;
    }

    // We don't handle mipmap in shared texture yet.
    private MTLTexture(MTLTexture<T> sharedTex, WrapMode newMode) {
        super(sharedTex, newMode, false);
        this.context = sharedTex.context;
        this.texPtr = sharedTex.texPtr;
    }

    @Override
    protected Texture createSharedTexture(WrapMode newMode) {
        return new MTLTexture<>(this, newMode);
    }

    private void updateTextureInt(Buffer buffer, PixelFormat format,
                                int dstx, int dsty,
                                int srcx, int srcy,
                                int srcw, int srch,
                                int srcscan) {
        if (format == PixelFormat.INT_ARGB_PRE) {
            IntBuffer buf = (IntBuffer) buffer;
            int[] arr = buf.hasArray() ? buf.array() : null;
            nUpdateInt(getNativeHandle(), buf, arr,
                       dstx, dsty, srcx, srcy, srcw, srch, srcscan);
        } else {
            throw new IllegalArgumentException("Unsupported INT PixelFormat: " + format);
        }
    }

    private void updateTextureFloat(Buffer buffer, PixelFormat format,
                                int dstx, int dsty,
                                int srcx, int srcy,
                                int srcw, int srch,
                                int srcscan) {
        if (format == PixelFormat.FLOAT_XYZW) {
            FloatBuffer buf = (FloatBuffer) buffer;
            float[] arr = buf.hasArray() ? buf.array() : null;
            nUpdateFloat(getNativeHandle(), buf, arr,
                         dstx, dsty, srcx, srcy, srcw, srch, srcscan);
        } else {
            throw new IllegalArgumentException("Unsupported FLOAT PixelFormat: " + format);
        }
    }

    private void updateTextureByte(Buffer buffer, PixelFormat format,
                                int dstx, int dsty,
                                int srcx, int srcy,
                                int srcw, int srch,
                                int srcscan) {
        ByteBuffer buf = (ByteBuffer) buffer;
        buf.rewind();
        byte[] arr = buf.hasArray() ? buf.array() : null;

        switch (format) {
            case PixelFormat.BYTE_BGRA_PRE,
                 PixelFormat.BYTE_ALPHA ->
                nUpdate(getNativeHandle(), buf, arr,
                        dstx, dsty, srcx, srcy, srcw, srch, srcscan);

            case PixelFormat.BYTE_RGB -> {
                // Convert 24-bit RGB to 32-bit BGRA
                // Metal does not support 24-bit format
                // hence `arr` data needs to be converted to BGRA format
                if (arr == null) {
                    arr = new byte[buf.remaining()];
                    buf.get(arr);
                }
                byte[] arr32Bit = new byte[srcw * srch * 4];
                int dstIndex = 0;
                int index = 0;

                int rowStride = srcw * 3;
                int totalBytes = srch * rowStride;

                for (int rowIndex = 0; rowIndex < totalBytes; rowIndex += rowStride) {
                    for (int colIndex = 0; colIndex < rowStride; colIndex += 3) {
                        index = rowIndex + colIndex;
                        arr32Bit[dstIndex++] = arr[index + 2];
                        arr32Bit[dstIndex++] = arr[index + 1];
                        arr32Bit[dstIndex++] = arr[index];
                        arr32Bit[dstIndex++] = (byte)255;
                    }
                }
                nUpdate(getNativeHandle(), null, arr32Bit,
                        dstx, dsty, srcx, srcy, srcw, srch, srcw * 4);
            }

            case PixelFormat.BYTE_GRAY -> {
                // Suitable 8-bit native formats are MTLPixelFormatA8Unorm & MTLPixelFormatR8Unorm.
                // These formats do not work well with our generated shader - Texture_RGB.
                // hence `arr` data is converted to BGRA format here.
                // In future, if needed for performance reason:
                // Texture_RGB shader can be tweaked to fill up R,G,B fields from single byte grayscale value.
                // Care must be taken not to break current behavior of this shader.
                if (arr == null) {
                    arr = new byte[buf.remaining()];
                    buf.get(arr);
                }
                byte[] arr32Bit = new byte[srcw * srch * 4];
                int dstIndex = 0;
                int index = 0;
                int totalBytes = srch * srcw;

                for (int rowIndex = 0; rowIndex < totalBytes; rowIndex += srcw) {
                    for (int colIndex = 0; colIndex < srcw; colIndex++) {
                        index = rowIndex + colIndex;
                        arr32Bit[dstIndex++] = arr[index];
                        arr32Bit[dstIndex++] = arr[index];
                        arr32Bit[dstIndex++] = arr[index];
                        arr32Bit[dstIndex++] = (byte) 255;
                    }
                }
                nUpdate(getNativeHandle(), null, arr32Bit,
                        dstx, dsty, srcx, srcy, srcw, srch, srcw * 4);
            }

            case PixelFormat.MULTI_YCbCr_420,
                 PixelFormat.BYTE_APPLE_422 ->
                throw new IllegalArgumentException("Unsupported PixelFormat " + format);
        }
    }

    @Override
    public void update(Buffer buffer, PixelFormat format,
                        int dstx, int dsty,
                        int srcx, int srcy,
                        int srcw, int srch,
                        int srcscan, boolean skipFlush) {

        if (!resource.isValid()) {
            return;
        }

        switch (format.getDataType()) {
            case PixelFormat.DataType.INT -> updateTextureInt(buffer, format,
                dstx, dsty, srcx, srcy, srcw, srch, srcscan);

            case PixelFormat.DataType.FLOAT -> updateTextureFloat(buffer, format,
                dstx, dsty, srcx, srcy, srcw, srch, srcscan);

            case PixelFormat.DataType.BYTE -> updateTextureByte(buffer, format,
                dstx, dsty, srcx, srcy, srcw, srch, srcscan);
        }
    }


    @Override
    public void update(MediaFrame frame, boolean skipFlush) {
        if (frame.getPixelFormat() == PixelFormat.MULTI_YCbCr_420 ||
            frame.getPixelFormat() != PixelFormat.BYTE_APPLE_422) {
            // Shouldn't have gotten this far
            throw new IllegalArgumentException("Unsupported format: " + frame.getPixelFormat());
        }

        frame.holdFrame();

        ByteBuffer pixels = frame.getBufferForPlane(0);
        byte[] arr = pixels.hasArray() ? pixels.array() : null;
        if (arr == null) {
            arr = new byte[pixels.remaining()];
            pixels.get(arr);
        }

        nUpdateYUV422(this.getNativeHandle(),
                      arr, 0, 0, 0, 0,
                      frame.getEncodedWidth(), frame.getEncodedHeight(),
                      frame.strideForPlane(0));

        frame.releaseFrame();
    }


    // Native methods

    private static native void nUpdate(long pResource, ByteBuffer buf, byte[] pixels,
                                       int dstx, int dsty, int srcx, int srcy,
                                       int w, int h, int stride);

    private static native void nUpdateFloat(long pResource, FloatBuffer buf, float[] pixels,
                                            int dstx, int dsty, int srcx, int srcy,
                                            int w, int h, int stride);

    private static native void nUpdateInt(long pResource, IntBuffer buf, int[] pixels,
                                          int dstx, int dsty, int srcx, int srcy,
                                          int w, int h, int stride);

    private static native void nUpdateYUV422(long pResource, byte[] pixels,
                                             int dstx, int dsty, int srcx, int srcy,
                                             int w, int h, int stride);
}

/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.sw;


import com.sun.javafx.image.PixelConverter;
import com.sun.javafx.image.PixelGetter;
import com.sun.javafx.image.PixelUtils;
import com.sun.javafx.image.impl.ByteBgraPre;
import com.sun.javafx.image.impl.ByteGray;
import com.sun.javafx.image.impl.ByteRgb;
import com.sun.javafx.image.impl.IntArgbPre;
import com.sun.prism.Image;
import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.PrismSettings;

import java.nio.Buffer;
import java.nio.IntBuffer;

class SWTexture implements Texture {

    private int data[];
    private boolean allocated = false;
    private boolean hasAlpha = true;
    private int width, height;
    private int stride, offset;
    private SWResourceFactory factory;
    private int lastImageSerial;
    private final WrapMode wrapMode;

    SWTexture(SWResourceFactory factory, WrapMode wrapMode, int w, int h) {
        this.factory = factory;
        this.wrapMode = wrapMode;
        width = w;
        height = h;
        stride = w;
        offset = 0;
    }

    SWTexture(SWTexture sharedTex, WrapMode altMode) {
        this.data = sharedTex.data;
        this.allocated = sharedTex.allocated;
        this.hasAlpha = sharedTex.hasAlpha;
        this.width = sharedTex.width;
        this.height = sharedTex.height;
        this.factory = sharedTex.factory;
        // REMIND: Use indirection to share the serial number?
        this.lastImageSerial = sharedTex.lastImageSerial;
        this.wrapMode = altMode;
    }

    SWResourceFactory getResourceFactory() {
        return this.factory;
    }

    int[] getDataNoClone() {
        return data;
    }

    boolean hasAlpha() {
        return hasAlpha;
    }
    
    int getStride() {
        return stride;
    }
    
    int getOffset() {
        return offset;
    }

    @Override
    public void dispose() { }

    @Override
    public PixelFormat getPixelFormat() {
        return PixelFormat.INT_ARGB_PRE;
    }

    @Override
    public int getPhysicalWidth() {
        return width;
    }

    @Override
    public int getPhysicalHeight() {
        return height;
    }

    @Override
    public int getContentX() {
        return 0;
    }

    @Override
    public int getContentY() {
        return 0;
    }

    @Override
    public int getContentWidth() {
        return width;
    }

    @Override
    public int getContentHeight() {
        return height;
    }

    public int getLastImageSerial() {
        return lastImageSerial;
    }

    public void setLastImageSerial(int serial) {
        lastImageSerial = serial;
    }

    @Override
    public void update(Image img) {
        throw new UnsupportedOperationException("update1:unimp");
    }

    @Override
    public void update(Image img, int dstx, int dsty) {
        throw new UnsupportedOperationException("update2:unimp");
    }

    @Override
    public void update(Image img, int dstx, int dsty, int srcw, int srch) {
        throw new UnsupportedOperationException("update3:unimp");
    }

    @Override
    public void update(Image img, int dstx, int dsty, int srcw, int srch, boolean skipFlush) {
        if (PrismSettings.debug) {
            System.out.println("Image format: " + img.getPixelFormat());
            System.out.println("Bytes per pixel: " + img.getBytesPerPixelUnit());
            System.out.println("dstx:" + dstx + " dsty:" + dsty + " srcw:" + srcw + " srch:" + srch);
        }
        this.allocate();

        final PixelGetter getter;
        switch (img.getPixelFormat()) {
            case BYTE_RGB:
                getter = ByteRgb.getter;
                this.hasAlpha = false;
                break;
            case INT_ARGB_PRE:
                getter = IntArgbPre.getter;
                this.hasAlpha = true;
                break;
            case BYTE_BGRA_PRE:
                getter = ByteBgraPre.getter;
                this.hasAlpha = true;
                break;
            case BYTE_GRAY:
                getter = ByteGray.getter;
                this.hasAlpha = false;
                break;
            default:
                throw new UnsupportedOperationException("!!! UNSUPPORTED PIXEL FORMAT: " + img.getPixelFormat());
        }

        PixelConverter converter = PixelUtils.getConverter(getter, IntArgbPre.setter);
        converter.convert(img.getPixelBuffer(), 0, srcw * img.getBytesPerPixelUnit(),
                IntBuffer.wrap(this.data), (dsty * width) + dstx, width, srcw, srch);
    }

    @Override
    public void update(Buffer buffer, PixelFormat format, int dstx, int dsty, int srcx, int srcy, int srcw, int srch, int srcscan, boolean skipFlush) {
        throw new UnsupportedOperationException("update5:unimp");
    }

    @Override
    public void update(MediaFrame frame, boolean skipFlush) {
        if (PrismSettings.debug) {
            System.out.println("Media Pixel format: " + frame.getPixelFormat());
        }

        frame.holdFrame();

        if (frame.getPixelFormat() != PixelFormat.INT_ARGB_PRE) {
            MediaFrame f = frame.convertToFormat(PixelFormat.INT_ARGB_PRE);
            frame.releaseFrame();
            frame = f;
        }

        IntBuffer ib = frame.getBuffer().asIntBuffer();
        if (ib.hasArray()) {
            this.allocated = false;
            this.data = ib.array();
        } else {
            this.allocate();
            ib.get(this.data);
        }
        this.offset = frame.offsetForPlane(0) / 4;
        this.stride = frame.strideForPlane(0) / 4;

        frame.releaseFrame();
    }

    @Override
    public WrapMode getWrapMode() {
        return wrapMode;
    }

    public Texture getSharedTexture(WrapMode altMode) {
        if (wrapMode == altMode) {
            return this;
        }
        switch (altMode) {
            case REPEAT:
                if (wrapMode != WrapMode.CLAMP_TO_EDGE) {
                    return null;
                }
                break;
            case CLAMP_TO_EDGE:
                if (wrapMode != WrapMode.REPEAT) {
                    return null;
                }
            default:
                return null;
        }
        return new SWTexture(this, altMode);
    }

    @Override
    public boolean getLinearFiltering() {
        return false;
    }

    @Override
    public void setLinearFiltering(boolean linear) { }

    void allocate() {
        if (allocated) {
            return;
        }
        if (PrismSettings.debug) {
            System.out.println("PCS Texture allocating buffer: " + this + ", " + width + "x" + height);
        }
        this.data = new int[width * height];
        allocated = true;
    }

    void applyCompositeAlpha(float alpha) {
        if (allocated) {
            int finalAlpha;
            this.hasAlpha = this.hasAlpha || (alpha < 1f);
            for (int i = 0; i < this.data.length; i++) {
                finalAlpha = ((int)((this.data[i] >> 24) * alpha + 0.5f)) & 0xFF;
                this.data[i] = (finalAlpha << 24) | (this.data[i] & 0xFFFFFF);
            }
        } else {
            throw new IllegalStateException("Cannot apply composite alpha to texture with non-allocated data");
        }
    }
}

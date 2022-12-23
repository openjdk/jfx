/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.j2d;

import com.sun.glass.ui.Screen;
import com.sun.javafx.image.impl.IntArgbPre;
import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

class J2DRTTexture extends J2DTexture implements RTTexture {
    protected J2DResourceFactory factory;
    private boolean opaque;

    J2DRTTexture(int w, int h, J2DResourceFactory factory) {
        super(new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE),
              PixelFormat.INT_ARGB_PRE,
              IntArgbPre.setter, WrapMode.CLAMP_TO_ZERO);
        this.factory = factory;
        this.opaque = false;
    }

    @Override
    public int[] getPixels() {
        BufferedImage bimg = getBufferedImage();
        java.awt.image.DataBuffer db = bimg.getRaster().getDataBuffer();
        if (db instanceof java.awt.image.DataBufferInt) {
            return ((java.awt.image.DataBufferInt) db).getData();
        }
        return null;
    }

    @Override
    public boolean readPixels(Buffer pixels, int x, int y, int width, int height) {
        if (x != getContentX() || y != getContentY()
                || width != getContentWidth() || height != getContentHeight())
        {
            throw new IllegalArgumentException("reading subtexture not yet supported!");
        }
        return readPixels(pixels);
    }

    @Override
    public boolean readPixels(Buffer pixels) {
//        int x = getContentX();
//        int y = getContentY();
        int w = getContentWidth();
        int h = getContentHeight();
        int pixbuf[] = getPixels();
        // NOTE: Caller should clear this, not the callee...
        pixels.clear();

        // REMIND: This assumes that the caller wants BGRA PRE data...?
        for (int i = 0; i < w * h; i++) {
            int argb = pixbuf[i];
            if (pixels instanceof IntBuffer) {
                ((IntBuffer)pixels).put(argb);
            } else if (pixels instanceof ByteBuffer) {
                byte a = (byte) (argb >> 24);
                byte r = (byte) (argb >> 16);
                byte g = (byte) (argb >>  8);
                byte b = (byte) (argb      );
                ((ByteBuffer)pixels).put(b);
                ((ByteBuffer)pixels).put(g);
                ((ByteBuffer)pixels).put(r);
                ((ByteBuffer)pixels).put(a);
            }
        }
        pixels.rewind();
        return true;
    }

    @Override
    public Graphics createGraphics() {
        BufferedImage bimg = getBufferedImage();
        J2DPresentable presentable = J2DPresentable.create(bimg, factory);
        java.awt.Graphics2D g2d = bimg.createGraphics();
        return factory.createJ2DPrismGraphics(presentable, g2d);
    }

    java.awt.Graphics2D createAWTGraphics2D() {
        return getBufferedImage().createGraphics();
    }

    @Override
    public Screen getAssociatedScreen() {
        return factory.getScreen();
    }

    @Override
    public void update(Image img) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Image img, int dstx, int dsty) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Image img, int dstx, int dsty, int w, int h) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Image img, int dstx, int dsty, int srcw, int srch,
                       boolean skipFlush)
    {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Buffer buffer, PixelFormat format, int dstx, int dsty,
                       int srcx, int srcy, int srcw, int srch,
                       int srcscan,
                       boolean skipFlush)
    {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public boolean isOpaque() {
        return opaque;
    }

    @Override
    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }

    @Override
    public boolean isVolatile() {
        return false;
    }

    @Override public boolean isMSAA() {
        return false;
    }
}

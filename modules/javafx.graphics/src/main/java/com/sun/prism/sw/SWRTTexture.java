/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.pisces.AbstractSurface;
import com.sun.pisces.DirectBufferSurface;
import com.sun.pisces.JavaSurface;
import com.sun.pisces.PiscesRenderer;
import com.sun.pisces.RendererBase;
import com.sun.prism.Graphics;
import com.sun.prism.RTTexture;
import com.sun.prism.impl.PrismSettings;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

class SWRTTexture extends SWArgbPreTexture implements RTTexture {

    private final AbstractSurface surface;
    private final Rectangle dimensions = new Rectangle();
    private final int dataOffset;

    private PiscesRenderer pr;
    private boolean isOpaque;

    SWRTTexture(SWResourceFactory factory, int w, int h) {
        this(factory, w, h, (int[]) null, 0);
    }

    SWRTTexture(SWResourceFactory factory, int w, int h, int[] data, int dataOffset) {
        super(factory, WrapMode.CLAMP_TO_ZERO, w, h, data);

        this.allocate();  // allocates only if provided data was null
        this.dataOffset = dataOffset;
        this.surface = new JavaSurface(getDataNoClone(), RendererBase.TYPE_INT_ARGB_PRE, w, h, dataOffset);
        this.dimensions.setBounds(0, 0, w, h);
    }

    /*
     * Constructs a texture using a direct (off-heap) IntBuffer.
     */
    SWRTTexture(SWResourceFactory factory, int w, int h, IntBuffer data) {
        super(factory, WrapMode.CLAMP_TO_ZERO, w, h, null);

        this.allocated = true;  // provided by the direct buffer, so no internal pixel array needs allocation
        this.dataOffset = 0;
        this.surface = new DirectBufferSurface(data, RendererBase.TYPE_INT_ARGB_PRE, w, h);
        this.dimensions.setBounds(0, 0, w, h);
    }

    AbstractSurface getSurface() {
        return this.surface;
    }

    @Override
    public int[] getPixels() {
        if (contentWidth == physicalWidth) {
            return getDataNoClone();
        } else {
            return null;
        }
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
        if (PrismSettings.debug) {
            System.out.println("+ SWRTT.readPixels: this: " + this);
        }

        int[] pixbuf = getDataNoClone();
        pixels.clear();
        // REMIND: This assumes that the caller wants BGRA PRE data...?

        if (pixels instanceof IntBuffer iPixels) {
            if (pixbuf != null) {  // is it a heap buffer?
                for (int i = 0; i < contentHeight; i++) {
                    iPixels.put(pixbuf, dataOffset + i * physicalWidth, contentWidth);
                }
            } else {
                IntBuffer src = directSource();  // duplicate to ensure position is undisturbed
                int[] row = new int[contentWidth];

                for (int i = 0; i < contentHeight; i++) {
                    src.position(i * physicalWidth);
                    src.get(row, 0, contentWidth);
                    iPixels.put(row, 0, contentWidth);
                }
            }
        } else if (pixels instanceof ByteBuffer bPixels) {
            if (pixbuf != null) {  // is it a heap buffer?
                for (int i = 0; i < contentHeight; i++) {
                    for (int j = 0; j < contentWidth; j++) {
                        int argb = pixbuf[dataOffset + i * physicalWidth + j];
                        byte a = (byte) (argb >> 24);
                        byte r = (byte) (argb >> 16);
                        byte g = (byte) (argb >> 8);
                        byte b = (byte) argb;

                        bPixels.put(b).put(g).put(r).put(a);
                    }
                }
            } else {
                IntBuffer src = directSource();  // duplicate to ensure position is undisturbed

                for (int i = 0; i < contentHeight; i++) {
                    for (int j = 0; j < contentWidth; j++) {
                        int argb = src.get(i * physicalWidth + j);
                        byte a = (byte) (argb >> 24);
                        byte r = (byte) (argb >> 16);
                        byte g = (byte) (argb >> 8);
                        byte b = (byte) argb;

                        bPixels.put(b).put(g).put(r).put(a);
                    }
                }
            }
        } else {
            return false;
        }
        pixels.rewind();
        return true;
    }

    @Override
    public Screen getAssociatedScreen() {
        return getResourceFactory().getScreen();
    }

    @Override
    public Graphics createGraphics() {
        if (pr == null) {
            pr = new PiscesRenderer(this.surface);
        }
        return new SWGraphics(this, getResourceFactory().getContext(), pr);
    }

    @Override
    public boolean isOpaque() {
        return isOpaque;
    }

    @Override
    public void setOpaque(boolean opaque) {
        this.isOpaque = opaque;
    }

    Rectangle getDimensions() { return dimensions; }

    @Override
    public boolean isVolatile() {
        return false;
    }

    @Override
    public boolean isMSAA() {
        return false;
    }

    private IntBuffer directSource() {
        if (surface instanceof DirectBufferSurface directSurface) {
            return directSurface.getDataBuffer().duplicate();
        }

        throw new IllegalStateException("texture is not backed by a direct buffer: " + surface);
    }
}

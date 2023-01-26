/*
 * Copyright (c) 2009, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.prism.sw;

import java.nio.IntBuffer;
import java.util.Arrays;
import com.sun.glass.ui.Screen;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.Image;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.scenario.effect.impl.HeapImage;
import com.sun.scenario.effect.impl.prism.PrDrawable;

public class PSWDrawable extends PrDrawable implements HeapImage {

    private RTTexture rtt;
    private Image image;
    private boolean heapDirty;
    private boolean vramDirty;

    private PSWDrawable(RTTexture rtt, boolean isDirty) {
        super(rtt);
        this.rtt = rtt;
        vramDirty = isDirty;
    }

    public static PSWDrawable create(RTTexture rtt) {
        return new PSWDrawable(rtt, true);
    }

    static int getCompatibleWidth(Screen screen, int w) {
        ResourceFactory factory =
            GraphicsPipeline.getPipeline().getResourceFactory(screen);
        return factory.getRTTWidth(w, WrapMode.CLAMP_TO_ZERO);
    }

    static int getCompatibleHeight(Screen screen, int h) {
        ResourceFactory factory =
            GraphicsPipeline.getPipeline().getResourceFactory(screen);
        return factory.getRTTHeight(h, WrapMode.CLAMP_TO_ZERO);
    }

    static PSWDrawable create(Screen screen, int width, int height) {
        ResourceFactory factory =
            GraphicsPipeline.getPipeline().getResourceFactory(screen);
        // force the wrap mode to CLAMP_TO_ZERO, as that is the mode
        // required by most Decora effects (blurs, etc)
        RTTexture rtt =
            factory.createRTTexture(width, height, WrapMode.CLAMP_TO_ZERO);
        return new PSWDrawable(rtt, false);
    }

    @Override
    public boolean isLost() {
        return rtt == null || rtt.isSurfaceLost();
    }

    @Override
    public void flush() {
        if (rtt != null) {
            rtt.dispose();
            rtt = null;
            image = null;
        }
    }

    @Override
    public Object getData() {
        return this;
    }

    @Override
    public int getContentWidth() {
        return rtt.getContentWidth();
    }

    @Override
    public int getContentHeight() {
        return rtt.getContentHeight();
    }

    @Override
    public int getMaxContentWidth() {
        return rtt.getMaxContentWidth();
    }

    @Override
    public int getMaxContentHeight() {
        return rtt.getMaxContentHeight();
    }

    @Override
    public void setContentWidth(int contentW) {
        rtt.setContentWidth(contentW);
    }

    @Override
    public void setContentHeight(int contentH) {
        rtt.setContentHeight(contentH);
    }

    @Override
    public int getPhysicalWidth() {
        // physical width in this case refers to the size of the system
        // memory copy, which is the size of the content region of the rtt
        return rtt.getContentWidth();
    }

    @Override
    public int getPhysicalHeight() {
        // physical height in this case refers to the size of the system
        // memory copy, which is the size of the content region of the rtt
        return rtt.getContentHeight();
    }

    @Override
    public int getScanlineStride() {
        return rtt.getContentWidth();
    }

    @Override
    public int[] getPixelArray() {
        int pixels[] = rtt.getPixels();
        if (pixels != null) {
            return pixels;
        }
        if (image == null) {
            int width = rtt.getContentWidth();
            int height = rtt.getContentHeight();
            pixels = new int[width*height];
            image = Image.fromIntArgbPreData(pixels, width, height);
        }
        IntBuffer buf = (IntBuffer)image.getPixelBuffer();
        if (vramDirty) {
            // copy texture data into heap array
            rtt.readPixels(buf);
            vramDirty = false;
        }
        heapDirty = true;
        return buf.array();
    }

    @Override
    public RTTexture getTextureObject() {
        if (heapDirty) {
            // RT-27562
            // TODO: inefficient approach: upload heap array to (cached)
            // texture, then render that texture to rtt
            int width = rtt.getContentWidth();
            int height = rtt.getContentHeight();
            Screen screen = rtt.getAssociatedScreen();
            ResourceFactory factory =
                GraphicsPipeline.getPipeline().getResourceFactory(screen);
            Texture tex =
                factory.createTexture(image, Usage.DEFAULT, WrapMode.CLAMP_TO_EDGE);
            Graphics g = createGraphics();
            g.drawTexture(tex, 0, 0, width, height);
            g.sync();
            tex.dispose();
            heapDirty = false;
        }
        return rtt;
    }

    @Override
    public Graphics createGraphics() {
        vramDirty = true;
        return rtt.createGraphics();
    }

    @Override
    public void clear() {
        Graphics g = createGraphics();
        g.clear();
        if (image != null) {
            IntBuffer buf = (IntBuffer)image.getPixelBuffer();
            Arrays.fill(buf.array(), 0);
        }
        heapDirty = false;
        vramDirty = false;
    }
}

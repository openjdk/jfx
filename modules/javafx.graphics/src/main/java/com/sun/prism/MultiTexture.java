/*
 * Copyright (c) 2008, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * A "texture" that wraps a list of sub-textures and values needed for multitexturing.
 */
public final class MultiTexture implements Texture {
    private int width;
    private int height;
    private PixelFormat format;
    private WrapMode wrapMode;
    private boolean linearFiltering = true;
    private final ArrayList<Texture> textures;
    private int lastImageSerial;

    public MultiTexture(PixelFormat format, WrapMode wrapMode, int width, int height) {
        this.width = width;
        this.height = height;
        this.format = format;
        this.wrapMode = wrapMode;
        textures = new ArrayList<>(4);
    }

    private MultiTexture(MultiTexture sharedTex, WrapMode newMode) {
        this(sharedTex.format, newMode, sharedTex.width, sharedTex.height);
        for (int i = 0; i < sharedTex.textureCount(); i++) {
            Texture t = sharedTex.getTexture(i);
            setTexture(t.getSharedTexture(newMode), i);
        }
        // REMIND: Do I need to use indirection to share these 2 field values?
        this.linearFiltering = sharedTex.linearFiltering;
        this.lastImageSerial = sharedTex.lastImageSerial;
    }

    @Override
    public Texture getSharedTexture(WrapMode altMode) {
        assertLocked();
        if (wrapMode == altMode) {
            lock();
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
                break;
            default:
                return null;
        }
        Texture altTex = new MultiTexture(this, altMode);
        altTex.lock();
        return altTex;
    }

    public int textureCount() {
        return textures.size();
    }

    public void setTexture(Texture tex, int index) {
        if (!tex.getWrapMode().isCompatibleWith(wrapMode)) {
            throw new IllegalArgumentException("texture wrap mode must match multi-texture mode");
        }
        if (textures.size() < (index+1)) {
            // add null entries to fill in, then add tex to the end
            for (int ii = textures.size(); ii < index; ii++) {
                textures.add(null);
            }
            textures.add(tex);
        } else {
            textures.set(index, tex);
        }
        tex.setLinearFiltering(linearFiltering);
    }

    public Texture getTexture(int index) {
        return textures.get(index);
    }

    public Texture[] getTextures() {
        return textures.toArray(new Texture[textures.size()]);
    }

    public void removeTexture(Texture tex) {
        textures.remove(tex);
    }

    public void removeTexture(int index) {
        textures.remove(index);
    }

    @Override
    public PixelFormat getPixelFormat() {
        return format;
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

    @Override
    public int getLastImageSerial() {
        return lastImageSerial;
    }

    @Override
    public void setLastImageSerial(int serial) {
        lastImageSerial = serial;
    }

    @Override
    public void update(Image img) {
        throw new UnsupportedOperationException("Update from Image not supported");
    }

    @Override
    public void update(Image img, int dstx, int dsty) {
        throw new UnsupportedOperationException("Update from Image not supported");
    }

    @Override
    public void update(Image img, int dstx, int dsty, int srcw, int srch) {
        throw new UnsupportedOperationException("Update from Image not supported");
    }

    @Override
    public void update(Image img, int dstx, int dsty, int srcw, int srch, boolean skipFlush) {
        throw new UnsupportedOperationException("Update from Image not supported");
    }

    @Override
    public void update(Buffer buffer, PixelFormat format,
                       int dstx, int dsty,
                       int srcx, int srcy, int srcw, int srch,
                       int srcscan, boolean skipFlush)
    {
        throw new UnsupportedOperationException("Update from generic Buffer not supported");
    }

    @Override
    public void update(MediaFrame frame, boolean skipFlush) {
        if (frame.getPixelFormat() == PixelFormat.MULTI_YCbCr_420) {
            // call update(..) on each texture
            Texture tex;
            int encWidth = frame.getEncodedWidth();
            int encHeight = frame.getEncodedHeight();

            for (int index = 0; index < frame.planeCount(); index++) {
                tex = textures.get(index);
                if (null != tex) {
                    int texWidth = encWidth;
                    int texHeight = encHeight;

                    if (index == PixelFormat.YCBCR_PLANE_CHROMABLUE
                            || index == PixelFormat.YCBCR_PLANE_CHROMARED) {
                        texWidth /= 2;
                        texHeight /= 2;
                    }

                    ByteBuffer pixels = frame.getBufferForPlane(index);
                    tex.update(pixels, PixelFormat.BYTE_ALPHA,
                            0, 0,
                            0, 0, texWidth, texHeight,
                            frame.strideForPlane(index), skipFlush);
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid pixel format in MediaFrame");
        }
    }

    @Override
    public WrapMode getWrapMode() {
        return wrapMode;
    }

    @Override
    public boolean getUseMipmap() {
        // TODO: MultiTexture doesn't support mipmap yet
        return false;
    }

    @Override
    public boolean getLinearFiltering() {
        return linearFiltering;
    }

    @Override
    public void setLinearFiltering(boolean linear) {
        this.linearFiltering = linear;
        for (Texture tex : textures) {
            tex.setLinearFiltering(linear);
        }
    }

    @Override
    public void lock() {
        for (Texture tex : textures) {
            tex.lock();
        }
    }

    @Override
    public void unlock() {
        for (Texture tex : textures) {
            tex.unlock();
        }
    }

    @Override
    public boolean isLocked() {
        for (Texture tex : textures) {
            if (tex.isLocked()) return true;
        }
        return false;
    }

    @Override
    public int getLockCount() {
        int count = 0;
        for (Texture tex : textures) {
            count = Math.max(count, tex.getLockCount());
        }
        return count;
    }

    @Override
    public void assertLocked() {
        for (Texture tex : textures) {
            tex.assertLocked();
        }
    }

    @Override
    public void makePermanent() {
        for (Texture tex : textures) {
            tex.makePermanent();
        }
    }

    @Override
    public void contentsUseful() {
        for (Texture tex : textures) {
            tex.contentsUseful();
        }
    }

    @Override
    public void contentsNotUseful() {
        for (Texture tex : textures) {
            tex.contentsNotUseful();
        }
    }

    @Override
    public boolean isSurfaceLost() {
        for (Texture tex : textures) {
            if (tex.isSurfaceLost()) return true;
        }
        return false;
    }

    @Override
    public void dispose() {
        for (Texture tex : textures) {
            tex.dispose();
        }
        textures.clear();
    }

    @Override
    public int getMaxContentWidth() {
        return getPhysicalWidth();
    }

    @Override
    public int getMaxContentHeight() {
        return getPhysicalHeight();
    }

    @Override
    public void setContentWidth(int contentWidth) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setContentHeight(int contentHeight) {
        throw new UnsupportedOperationException("Not supported.");
    }

}

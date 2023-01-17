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

package com.sun.prism.impl;

import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import java.nio.Buffer;

public abstract class BaseTexture<T extends ManagedResource> implements Texture {

    protected final T resource;

    private final PixelFormat format;
    private final int physicalWidth;
    private final int physicalHeight;
    private final int contentX;
    private final int contentY;
    protected int contentWidth;
    protected int contentHeight;

    //maximum possible user pixels of a texture (in case of POW2 texture allocation size)
    //currently only matters with RTT's
    private final int maxContentWidth;
    private final int maxContentHeight;

    // We do not provide a default wrapMode because it is so dependent on
    // how the texture will be used.
    private final WrapMode wrapMode;
    private final boolean useMipmap;
    private boolean linearFiltering = true;
    private int lastImageSerial;

    protected BaseTexture(BaseTexture<T> sharedTex, WrapMode newMode, boolean useMipmap) {
        this.resource = sharedTex.resource;
        this.format = sharedTex.format;
        this.wrapMode = newMode;
        this.physicalWidth = sharedTex.physicalWidth;
        this.physicalHeight = sharedTex.physicalHeight;
        this.contentX = sharedTex.contentX;
        this.contentY = sharedTex.contentY;
        this.contentWidth = sharedTex.contentWidth;
        this.contentHeight = sharedTex.contentHeight;
        this.maxContentWidth = sharedTex.maxContentWidth;
        this.maxContentHeight = sharedTex.maxContentHeight;
        this.useMipmap = useMipmap;
    }

    protected BaseTexture(T resource,
                          PixelFormat format, WrapMode wrapMode,
                          int width, int height)
    {
        this(resource, format, wrapMode, width, height, 0, 0, width, height, false);
    }

    protected BaseTexture(T resource,
                          PixelFormat format, WrapMode wrapMode,
                          int physicalWidth, int physicalHeight,
                          int contentX, int contentY,
                          int contentWidth, int contentHeight, boolean useMipmap)
    {
        this.resource = resource;
        this.format = format;
        this.wrapMode = wrapMode;
        this.physicalWidth = physicalWidth;
        this.physicalHeight = physicalHeight;
        this.contentX = contentX;
        this.contentY = contentY;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        this.maxContentWidth = physicalWidth;
        this.maxContentHeight = physicalHeight;
        this.useMipmap = useMipmap;
    }

    protected BaseTexture(T resource,
                          PixelFormat format, WrapMode wrapMode,
                          int physicalWidth, int physicalHeight,
                          int contentX, int contentY,
                          int contentWidth, int contentHeight,
                          int maxContentWidth, int maxContentHeight, boolean useMipmap)
    {
        this.resource = resource;
        this.format = format;
        this.wrapMode = wrapMode;
        this.physicalWidth = physicalWidth;
        this.physicalHeight = physicalHeight;
        this.contentX = contentX;
        this.contentY = contentY;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        this.maxContentWidth = maxContentWidth;
        this.maxContentHeight = maxContentHeight;
        this.useMipmap = useMipmap;
    }

    @Override
    public final PixelFormat getPixelFormat() {
        return format;
    }

    @Override
    public final int getPhysicalWidth() {
        return physicalWidth;
    }

    @Override
    public final int getPhysicalHeight() {
        return physicalHeight;
    }

    @Override
    public final int getContentX() {
        return contentX;
    }

    @Override
    public final int getContentY() {
        return contentY;
    }

    @Override
    public final int getContentWidth() {
        return contentWidth;
    }

    @Override
    public final int getContentHeight() {
        return contentHeight;
    }

    @Override
    public int getMaxContentWidth() {
        return maxContentWidth;
    }

    @Override
    public int getMaxContentHeight() {
        return maxContentHeight;
    }

    @Override
    public void setContentWidth(int contentW) {
        if (contentW > maxContentWidth) {
            throw new IllegalArgumentException("ContentWidth must be less than or "
                    + "equal to maxContentWidth");
        }
        contentWidth = contentW;
    }

    @Override
    public void setContentHeight(int contentH) {
        if (contentH > maxContentHeight) {
            throw new IllegalArgumentException("ContentWidth must be less than or "
                    + "equal to maxContentHeight");
        }
        contentHeight = contentH;
   }

    @Override
    public final WrapMode getWrapMode() {
        return wrapMode;
    }

    @Override
    public boolean getUseMipmap() {
        return useMipmap;
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
        Texture altTex = createSharedTexture(altMode);
        altTex.lock();
        return altTex;
    }

    /**
     * Create a new version of this texture which shares the same
     * {@code ManagedResource} reference.
     * The lock counts for the shared resource reference are not
     * changed in the processing of this method.
     *
     * @param newMode the {@code WrapMode} to use for the new texture.
     * @return a new {@code Texture} object sharing the underlying resource.
     */
    protected abstract Texture createSharedTexture(WrapMode newMode);

    @Override
    public final boolean getLinearFiltering() {
        return linearFiltering;
    }

    @Override
    public void setLinearFiltering(boolean linear) {
        this.linearFiltering = linear;
    }

    @Override
    public final int getLastImageSerial() {
        return lastImageSerial;
    }

    @Override
    public final void setLastImageSerial(int serial) {
        lastImageSerial = serial;
    }

    @Override
    public final void lock() {
        resource.lock();
    }

    @Override
    public final boolean isLocked() {
        return resource.isLocked();
    }

    @Override
    public final int getLockCount() {
        return resource.getLockCount();
    }

    @Override
    public final void assertLocked() {
        resource.assertLocked();
    }

    @Override
    public final void unlock() {
        resource.unlock();
    }

    @Override
    public final void makePermanent() {
        resource.makePermanent();
    }

    @Override
    public final void contentsUseful() {
        resource.contentsUseful();
    }

    @Override
    public final void contentsNotUseful() {
        resource.contentsNotUseful();
    }

    @Override
    public final boolean isSurfaceLost() {
        return !resource.isValid();
    }

    @Override
    public final void dispose() {
        resource.dispose();
    }

    @Override
    public void update(Image img) {
        update(img, 0, 0);
    }

    @Override
    public void update(Image img, int dstx, int dsty) {
        update(img, dstx, dsty, img.getWidth(), img.getHeight());
    }

    @Override
    public void update(Image img, int dstx, int dsty, int w, int h) {
        update(img, dstx, dsty, w, h, false);
    }

    @Override
    public void update(Image img, int dstx, int dsty, int srcw, int srch,
                       boolean skipFlush)
    {
        Buffer pbuffer = img.getPixelBuffer();
        int pos = pbuffer.position();
        update(pbuffer, img.getPixelFormat(),
               dstx, dsty, img.getMinX(), img.getMinY(),
               srcw, srch, img.getScanlineStride(),
               skipFlush);
        pbuffer.position(pos);
    }

    protected void checkUpdateParams(Buffer buf, PixelFormat fmt,
                                     int dstx, int dsty,
                                     int srcx, int srcy,
                                     int srcw, int srch,
                                     int srcscan)
    {
        if (format == PixelFormat.MULTI_YCbCr_420) {
            throw new IllegalArgumentException("MULTI_YCbCr_420 requires multitexturing");
        }
        if (buf == null) {
            throw new IllegalArgumentException("Pixel buffer must be non-null");
        }
        if (fmt != format) {
            throw new IllegalArgumentException(
                "Image format (" + fmt + ") " +
                "must match texture format (" + format + ")");
        }
        if (dstx < 0 || dsty < 0) {
            throw new IllegalArgumentException(
                "dstx (" + dstx + ") and dsty (" + dsty + ") must be >= 0");
        }
        if (srcx < 0 || srcy < 0) {
            throw new IllegalArgumentException(
                "srcx (" + srcx + ") and srcy (" + srcy + ") must be >= 0");
        }
        if (srcw <= 0 || srch <= 0) {
            throw new IllegalArgumentException(
                "srcw (" + srcw + ") and srch (" + srch + ") must be > 0");
        }
        if (srcscan >= (Integer.MAX_VALUE / srch)) {
            throw new IllegalArgumentException(
                "srcscan * srch (" + srcscan + " * " + srch + ") must be < "
                + "Integer.MAX_VALUE (" + Integer.MAX_VALUE + ")");
        }
        int bytesPerPixel = fmt.getBytesPerPixelUnit();
        if (srcscan % bytesPerPixel != 0) {
            throw new IllegalArgumentException(
                "srcscan (" + srcscan + ") " +
                "must be a multiple of the pixel stride (" + bytesPerPixel + ")");
        }
        if (srcw > srcscan / bytesPerPixel) {
            throw new IllegalArgumentException(
                "srcw (" + srcw + ") " +
                "must be <= srcscan/bytesPerPixel ("
                + (srcscan/bytesPerPixel) + ")");
        }
        if (dstx+srcw > contentWidth || dsty+srch > contentHeight) {
            throw new IllegalArgumentException(
                "Destination region " +
                "(x=" + dstx + ", y=" + dsty +
                ", w=" + srcw + ", h=" + srch + ") " +
                "must fit within texture content bounds " +
                "(contentWidth=" + contentWidth +
                ", contentHeight=" + contentHeight + ")");
        }
        int bytesNeeded =
            (srcx * bytesPerPixel) + (srcy * srcscan) +
            ((srch-1) * srcscan) + (srcw * bytesPerPixel);
        int elemsNeeded = bytesNeeded / format.getDataType().getSizeInBytes();
        if (elemsNeeded < 0 || elemsNeeded > buf.remaining()) {
            throw new IllegalArgumentException(
                "Upload requires " + elemsNeeded + " elements, but only " +
                buf.remaining() + " elements remain in the buffer");
        }
    }

    @Override
    public String toString() {
        return super.toString() + " [format="+format+
            " physicalWidth="+physicalWidth+
            " physicalHeight="+physicalHeight+
            " contentX="+contentX+
            " contentY="+contentY+
            " contentWidth="+contentWidth+
            " contentHeight="+contentHeight+
            " wrapMode="+wrapMode+
            " linearFiltering="+linearFiltering+
            "]";
    }
}

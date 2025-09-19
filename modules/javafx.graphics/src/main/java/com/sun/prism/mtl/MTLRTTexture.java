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

import com.sun.glass.ui.Screen;
import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ReadbackRenderTarget;
import com.sun.prism.Texture;

import java.nio.Buffer;
import java.nio.IntBuffer;


class MTLRTTexture extends MTLTexture<MTLTextureData>
                       implements RTTexture, ReadbackRenderTarget {
    private final int[] pixels;
    private final int  rttWidth;
    private final int  rttHeight;
    private final long nTexPtr;

    private boolean opaque;
    private final boolean MSAA;

    private MTLRTTexture(MTLContext context, MTLTextureResource<MTLTextureData> resource,
                         WrapMode wrapMode,
                         int physicalWidth, int physicalHeight,
                         int contentX, int contentY,
                         int contentWidth, int contentHeight,
                         int maxContentWidth, int maxContentHeight, boolean msaa) {

        super(context, resource, PixelFormat.BYTE_BGRA_PRE, wrapMode,
                physicalWidth, physicalHeight,
                contentX, contentY,
                contentWidth, contentHeight,
                maxContentWidth, maxContentHeight, false);
        rttWidth  = contentWidth;
        rttHeight = contentHeight;
        pixels  = new int[rttWidth * rttHeight];
        nTexPtr = resource.getResource().getResource();
        opaque  = false;
        MSAA    = msaa;

        // pixels array contains all 0s by default
        // Initialize native texture to clear color (0,0,0,0) using pixels
        nInitRTT(nTexPtr, pixels);
    }

    static MTLRTTexture create(MTLContext context,
                               int physicalWidth, int physicalHeight,
                               int contentWidth, int contentHeight,
                               WrapMode wrapMode, boolean msaa,
                               long size) {
        long nPtr = nCreateRT(context.getContextHandle(),
                physicalWidth, physicalHeight,
                contentWidth, contentHeight,
                wrapMode, msaa);
        MTLTextureData textData = new MTLRTTextureData(context, nPtr, size);
        MTLTextureResource<MTLTextureData> resource = new MTLTextureResource<>(textData, true);
        return new MTLRTTexture(context, resource, wrapMode,
                physicalWidth, physicalHeight,
                0, 0,
                contentWidth, contentHeight,
                contentWidth, contentHeight, msaa);
    }

    static MTLRTTexture create(MTLContext context, long pTex, int width, int height, long size) {
        long nPtr = nCreateRT2(context.getContextHandle(), pTex, width, height);

        MTLTextureData textData = new MTLFBOTextureData(context, nPtr, size);
        MTLTextureResource<MTLTextureData> resource = new MTLTextureResource<>(textData, false);

        return new MTLRTTexture(context, resource, WrapMode.CLAMP_NOT_NEEDED,
                width, height,
                0, 0,
                width, height,
                width, height, false);
    }

    @Override
    public long getNativeHandle() {
        return nTexPtr;
    }

    @Override
    public Texture getBackBuffer() {
        return this;
    }

    @Override
    public int[] getPixels() {
        // Flush the VB before reading the pixels.
        getContext().flushVertexBuffer();
        nReadPixels(nTexPtr, pixels);
        return pixels;
    }

    @Override
    public boolean readPixels(Buffer pix) {
        // The call from Canvas rendering expects IntBuffer, which is implemented here.
        // In future, if needed, need to implement pix as ByteBuffer
        if (pix instanceof IntBuffer pixBuf) {
            nReadPixelsFromRTT(nTexPtr, pixBuf);
            // pix = IntBuffer.wrap(pixels);
            return true;
        }
        return false;
    }

    @Override
    public boolean isVolatile() {
        return false;
    }

    @Override
    public Screen getAssociatedScreen() {
        return getContext().getAssociatedScreen();
    }

    @Override
    public Graphics createGraphics() {
        return MTLGraphics.create(getContext(), this);
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
    public boolean isMSAA() {
        return MSAA;
    }

    // Native methods

    private static native long nCreateRT(long context, int pw, int ph, int cw, int ch,
                                         WrapMode wrapMode, boolean msaa);
    private static native long nCreateRT2(long context, long pTex, int pw, int ph);
    private static native void nReadPixels(long nativeHandle, int[] pixBuffer);
    private static native void nReadPixelsFromRTT(long nativeHandle, IntBuffer pixBuffer);
    private static native long nGetPixelDataPtr(long nativeHandle);
    private static native void nInitRTT(long pTex, int[] pix);


    // Unsupported Operation methods

    @Override
    public boolean readPixels(Buffer pixels, int x, int y, int width, int height) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setContentWidth(int contentWidth) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setContentHeight(int contentHeight) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean getUseMipmap() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Texture getSharedTexture(WrapMode altMode) {
        throw new UnsupportedOperationException("Not implemented");
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
    public void update(Image img, int dstx, int dsty, int srcw, int srch) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Image img, int dstx, int dsty, int srcw, int srch, boolean skipFlush) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Buffer buffer, PixelFormat format, int dstx, int dsty, int srcx, int srcy,
                       int srcw, int srch, int srcscan, boolean skipFlush) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(MediaFrame frame, boolean skipFlush) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }
}

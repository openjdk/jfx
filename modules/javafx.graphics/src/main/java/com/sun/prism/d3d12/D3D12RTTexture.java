/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d12;

import java.nio.Buffer;

import com.sun.glass.ui.Screen;
import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ReadbackRenderTarget;
import com.sun.prism.Texture;
import com.sun.prism.d3d12.ni.D3D12NativeTexture;
import com.sun.prism.d3d12.ni.D3D12NativeRenderTarget;

public class D3D12RTTexture extends D3D12Texture implements RTTexture, ReadbackRenderTarget {

    private boolean mOpaque = false;
    private int mMSAALevel = 1;

    D3D12RTTexture(D3D12Resource<D3D12TextureData> resource, D3D12Context context, PixelFormat format, WrapMode wrapMode,
                   int physWidth, int physHeight, int contentWidth, int contentHeight, int msaaLevel) {
        super(resource, context, format, wrapMode, physWidth, physHeight, contentWidth, contentHeight);
        mMSAALevel = msaaLevel;
    }

    public static D3D12RTTexture create(D3D12Context context, int physWidth, int physHeight, int contentWidth, int contentHeight,
                                        PixelFormat format, WrapMode wrapMode, int aaSamples, boolean enableDirtyBBox) {
        D3D12NativeTexture tex = context.getDevice().createTexture(physWidth, physHeight, format, Usage.DEFAULT, wrapMode, aaSamples, false, true);
        D3D12NativeRenderTarget rt = context.getDevice().createRenderTarget(tex, enableDirtyBBox);

        return new D3D12RTTexture(new D3D12Resource<D3D12TextureData>(
                                      new D3D12TextureData(
                                         tex, rt, physWidth, physHeight, format
                                      )
                                  ),
                                  context,
                                  format,
                                  wrapMode,
                                  physWidth, physHeight,
                                  contentWidth, contentHeight,
                                  aaSamples);
    }

    D3D12NativeRenderTarget getNativeRenderTarget() {
        return resource.getResource().getRenderTarget();
    }

    // Resizes the RTT - resizes underlying texture and recreates its RTV if needed
    boolean resize(int width, int height) {
        if (!resource.getResource().getNativeTexture().resize(width, height))
            return false;

        return resource.getResource().getRenderTarget().refresh();
    }

    public int getWidth() {
        return resource.getResource().getRenderTarget().getWidth();
    }

    public int getHeight() {
        return resource.getResource().getRenderTarget().getHeight();
    }

    public boolean isValid() {
        return resource.getResource().isValid();
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
    public void update(Buffer buffer, PixelFormat format, int dstx, int dsty, int srcx, int srcy, int srcw, int srch,
            int srcscan, boolean skipFlush) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(MediaFrame frame, boolean skipFlush) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public Screen getAssociatedScreen() {
        return mContext.getAssociatedScreen();
    }

    @Override
    public Graphics createGraphics() {
        return new D3D12Graphics(mContext, this);
    }

    @Override
    public boolean isOpaque() {
        return mOpaque;
    }

    @Override
    public void setOpaque(boolean opaque) {
        mOpaque = opaque;
    }

    @Override
    public boolean isMSAA() {
        return (mMSAALevel > 1);
    }

    @Override
    public int[] getPixels() {
        return null;
    }

    @Override
    public boolean readPixels(Buffer pixels) {
        return readPixels(pixels, 0, 0, getWidth(), getHeight());
    }

    @Override
    public boolean readPixels(Buffer pixels, int x, int y, int width, int height) {
        // true on success, false on fail
        if (mContext.isDisposed()) return false;

        mContext.flushVertexBuffer();
        return mContext.getDevice().readTexture(resource.getResource().getNativeTexture(), pixels, x, y, width, height);
    }

    @Override
    public boolean isVolatile() {
        return false;
    }

    @Override
    public Texture getBackBuffer() {
        return this;
    }
}

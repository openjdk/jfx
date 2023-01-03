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
import com.sun.prism.Image;
import com.sun.prism.MediaFrame;
import com.sun.prism.Mesh;
import com.sun.prism.MeshView;
import com.sun.prism.PhongMaterial;
import com.sun.prism.PixelFormat;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.TextureResourcePool;
import com.sun.prism.impl.shape.BasicShapeRep;
import com.sun.prism.shape.ShapeRep;
import java.util.Map;
import java.util.WeakHashMap;

class J2DResourceFactory extends BaseResourceFactory
{
    private static final Map<Image,Texture> clampTexCache = new WeakHashMap<>();
    private static final Map<Image,Texture> repeatTexCache = new WeakHashMap<>();
    private static final Map<Image,Texture> mipmapTexCache = new WeakHashMap<>();

    private Screen screen;

    J2DResourceFactory(Screen screen) {
        super(clampTexCache, repeatTexCache, mipmapTexCache);
        this.screen = screen;
    }

    J2DPrismGraphics createJ2DPrismGraphics(J2DPresentable target,
                                            java.awt.Graphics2D g2d) {
        return new J2DPrismGraphics(target, g2d);
    }

    @Override
    public TextureResourcePool getTextureResourcePool() {
        return J2DTexturePool.instance;
    }

    Screen getScreen() {
        return screen;
    }

    private static ShapeRep theRep = new BasicShapeRep();

    @Override
    public ShapeRep createArcRep() {
        return theRep;
    }

    @Override
    public ShapeRep createEllipseRep() {
        return theRep;
    }

    @Override
    public ShapeRep createRoundRectRep() {
        return theRep;
    }

    @Override
    public ShapeRep createPathRep() {
        return theRep;
    }

    @Override
    public Presentable createPresentable(PresentableState pState) {
        return J2DPresentable.create(pState, this);
    }

    @Override
    public int getRTTWidth(int w, WrapMode wrapMode) {
        return w;
    }

    @Override
    public int getRTTHeight(int h, WrapMode wrapMode) {
        return h;
    }

    @Override
    public RTTexture createRTTexture(int width, int height, Texture.WrapMode wrapMode, boolean msaa) {
        return createRTTexture(width, height, wrapMode);
    }

    @Override
    public RTTexture createRTTexture(int width, int height, WrapMode wrapMode) {
        J2DTexturePool pool = J2DTexturePool.instance;
        long size = pool.estimateRTTextureSize(width, height, false);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }
        return new J2DRTTexture(width, height, this);
    }

    @Override
    public Texture createTexture(PixelFormat formatHint,
                                 Usage usageHint, WrapMode wrapMode,
                                 int w, int h)
    {
        return J2DTexture.create(formatHint, wrapMode, w, h);
    }

    @Override
    public Texture createTexture(PixelFormat formatHint,
            Usage usageHint, WrapMode wrapMode, int w, int h, boolean useMipmap) {
        return createTexture(formatHint, usageHint, wrapMode, w, h);
    }

    @Override
    public Texture createTexture(MediaFrame vdb) {
        Texture tex;

        vdb.holdFrame();

        if (vdb.getPixelFormat() != PixelFormat.INT_ARGB_PRE) {
            MediaFrame newFrame = vdb.convertToFormat(PixelFormat.INT_ARGB_PRE);
            vdb.releaseFrame();
            vdb = newFrame;
            if (null == vdb) {
                // FIXME: error condition?
                return null;
            }
        }

        tex = J2DTexture.create(vdb.getPixelFormat(), WrapMode.CLAMP_TO_EDGE,
                                vdb.getWidth(), vdb.getHeight());
        vdb.releaseFrame();
        return tex;
    }

    @Override
    public boolean isCompatibleTexture(Texture tex) {
        return tex instanceof J2DTexture;
    }

    @Override
    protected boolean canClampToZero() {
        return false;
    }

    @Override
    public int getMaximumTextureSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFormatSupported(PixelFormat format) {
        switch (format) {
            case BYTE_RGB:
            case BYTE_GRAY:
            case INT_ARGB_PRE:
            case BYTE_BGRA_PRE:
                return true;
            case BYTE_ALPHA:
            case BYTE_APPLE_422:
            case MULTI_YCbCr_420:
            case FLOAT_XYZW:
            default:
                return false;
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public PhongMaterial createPhongMaterial() {
        throw new UnsupportedOperationException("Not supported yet.");
}

    @Override
    public MeshView createMeshView(Mesh mesh) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Mesh createMesh() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

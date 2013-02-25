/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.RenderingContext;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.BaseRenderingContext;
import com.sun.prism.impl.VertexBuffer;
import com.sun.prism.impl.shape.BasicShapeRep;
import com.sun.prism.shape.ShapeRep;

class J2DResourceFactory extends BaseResourceFactory
{
    private Screen screen;

    J2DResourceFactory(Screen screen) {
        this.screen = screen;
    }

    Screen getScreen() {
        return screen;
    }

    private static ShapeRep theRep = new BasicShapeRep();

    public ShapeRep createArcRep(boolean needs3D) {
        return theRep;
    }

    public ShapeRep createEllipseRep(boolean needs3D) {
        return theRep;
    }

    public ShapeRep createRoundRectRep(boolean needs3D) {
        return theRep;
    }

    public ShapeRep createPathRep(boolean needs3D) {
        return theRep;
    }

    public Presentable createPresentable(PresentableState pState) {
        return J2DPresentable.create(pState, this);
    }

    public RTTexture createRTTexture(int width, int height, WrapMode wrapMode) {
        return new J2DRTTexture(width, height, this);
    }

    public Texture createTexture(PixelFormat formatHint,
                                 Usage usageHint, WrapMode wrapMode,
                                 int w, int h)
    {
        return new J2DTexture(formatHint, wrapMode, w, h);
    }

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

        tex = new J2DTexture(vdb.getPixelFormat(), WrapMode.CLAMP_TO_EDGE,
                             vdb.getWidth(), vdb.getHeight());
        vdb.releaseFrame();
        return tex;
    }

    public int getMaximumTextureSize() {
        return Integer.MAX_VALUE;
    }

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

    public VertexBuffer createVertexBuffer(int maxQuads) {
        // This is only used by ES1 and ES2 - it should perhaps be
        // moved to an ES-specific subclass of ResourceFactory?
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns dummy implementation of the RenderingContext interface.
     *
     * @param view the view to construct rendering context. if view is null,
     *             then the rendering context is in offscreen mode.
     */
    @Override
    public RenderingContext createRenderingContext(PresentableState pState) {
        return new BaseRenderingContext();
    }

    public void dispose() {
    }
}

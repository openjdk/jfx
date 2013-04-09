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

import com.sun.glass.ui.Screen;
import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;
import com.sun.prism.RenderingContext;
import com.sun.prism.ResourceFactory;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.BaseRenderingContext;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.TextureResourcePool;
import com.sun.prism.impl.VertexBuffer;
import com.sun.prism.impl.shape.BasicRoundRectRep;
import com.sun.prism.impl.shape.BasicShapeRep;
import com.sun.prism.shape.ShapeRep;

final class SWResourceFactory
    extends BaseResourceFactory
        implements ResourceFactory {
            
    private static final ShapeRep theRep = new BasicShapeRep();
    private static final ShapeRep rectRep = new BasicRoundRectRep();

    private Screen screen;
    private final SWContext context;

    public SWResourceFactory(Screen screen) {
        this.screen = screen;
        this.context = new SWContext(this);
    }

    public TextureResourcePool getTextureResourcePool() {
        return SWTexturePool.instance;
    }

    public Screen getScreen() {
        return screen;
    }

    SWContext getContext() {
        return context;
    }
    
    @Override public void dispose() {
        context.dispose();
    }

    @Override public RenderingContext createRenderingContext(PresentableState pstate) {
        return new BaseRenderingContext();
    }
    
    @Override public ShapeRep createArcRep(boolean needs3D) {
        return theRep;
    }
    
    @Override public ShapeRep createEllipseRep(boolean needs3D) {
        return theRep;
    }
    
    @Override public ShapeRep createRoundRectRep(boolean needs3D) {
        return rectRep;
    }
    
    @Override public ShapeRep createPathRep(boolean needs3D) {
        return theRep;
    }
            
    @Override public VertexBuffer createVertexBuffer(int maxQuads) {
        throw new UnsupportedOperationException("createVertexBuffer:unimp");
    }
            
    @Override public Presentable createPresentable(PresentableState pstate) {
        if (PrismSettings.debug) {
            System.out.println("+ SWRF.createPresentable()");
        }
        return new SWPresentable(pstate, this);
    }
            
    @Override public RTTexture createRTTexture(int width, int height,
                                               WrapMode wrapMode)
    {
        SWTexturePool pool = SWTexturePool.instance;
        long size = pool.estimateRTTextureSize(width, height, false);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }
        return new SWRTTexture(this, width, height);
    }

    @Override public int getMaximumTextureSize() {
        return Integer.MAX_VALUE;
    }
            
    @Override public boolean isFormatSupported(PixelFormat format) {
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

    @Override public Texture createTexture(MediaFrame vdb) {
        return new SWArgbPreTexture(this, WrapMode.CLAMP_TO_EDGE, vdb.getWidth(), vdb.getHeight());
    }
            
    @Override public Texture createTexture(PixelFormat formatHint,
                                           Usage usageHint,
                                           WrapMode wrapMode,
                                           int w, int h)
    {
        SWTexturePool pool = SWTexturePool.instance;
        long size = pool.estimateTextureSize(w, h, formatHint);
        if (!pool.prepareForAllocation(size)) {
            return null;
        }
        return SWTexture.create(this, formatHint, wrapMode, w, h);
    }
}

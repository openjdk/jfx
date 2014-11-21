/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.openpisces.Renderer;
import com.sun.pisces.PiscesRenderer;
import com.sun.prism.BasicStroke;
import com.sun.prism.PixelFormat;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.shape.MaskData;
import com.sun.prism.impl.shape.OpenPiscesPrismUtils;
import com.sun.prism.impl.shape.ShapeUtil;

import java.lang.ref.SoftReference;

final class SWContext {

    private final ResourceFactory factory;
    private final ShapeRenderer shapeRenderer;
    private SoftReference<SWRTTexture> readBackBufferRef;
    private SoftReference<SWArgbPreTexture> imagePaintTextureRef;

    interface ShapeRenderer {
        void renderShape(PiscesRenderer pr, Shape shape, BasicStroke stroke, BaseTransform tr, Rectangle clip);
        void dispose();
    }

    class NativeShapeRenderer implements ShapeRenderer {
        private SoftReference<SWMaskTexture> maskTextureRef;

        public void renderShape(PiscesRenderer pr, Shape shape, BasicStroke stroke, BaseTransform tr, Rectangle clip) {
            final MaskData mask = ShapeUtil.rasterizeShape(shape, stroke, clip.toRectBounds(), tr, true);
            final SWMaskTexture tex = this.validateMaskTexture(mask.getWidth(), mask.getHeight());
            mask.uploadToTexture(tex, 0, 0, false);
            pr.fillAlphaMask(tex.getDataNoClone(), mask.getOriginX(), mask.getOriginY(),
                             mask.getWidth(), mask.getHeight(), 0, tex.getPhysicalWidth());
        }

        private SWMaskTexture initMaskTexture(int width, int height) {
            final SWMaskTexture tex = (SWMaskTexture)factory.createMaskTexture(width, height, Texture.WrapMode.CLAMP_NOT_NEEDED);
            maskTextureRef = new SoftReference<SWMaskTexture>(tex);
            return tex;
        }

        private void disposeMaskTexture() {
            if (maskTextureRef != null){
                maskTextureRef.clear();
                maskTextureRef = null;
            }
        }

        private SWMaskTexture validateMaskTexture(int width, int height) {
            SWMaskTexture tex;
            if (maskTextureRef == null) {
                tex = this.initMaskTexture(width, height);
            } else {
                tex = maskTextureRef.get();
                if (tex == null ||
                    tex.getPhysicalWidth() < width ||
                    tex.getPhysicalHeight() < height)
                {
                    this.disposeMaskTexture();
                    tex = this.initMaskTexture(width, height);
                }
            }
            return tex;
        }

        public void dispose() {
            this.disposeMaskTexture();
        }
    }

    class JavaShapeRenderer implements ShapeRenderer {
        private final DirectRTPiscesAlphaConsumer alphaConsumer = new DirectRTPiscesAlphaConsumer();

        public void renderShape(PiscesRenderer pr, Shape shape, BasicStroke stroke, BaseTransform tr, Rectangle clip) {
            if (stroke != null && stroke.getType() != BasicStroke.TYPE_CENTERED) {
                // RT-27427
                // TODO: Optimize the combinatorial strokes for simple
                // shapes and/or teach the rasterizer to be able to
                // do a "differential fill" between two shapes.
                // Note that most simple shapes will use a more optimized path
                // than this method for the INNER/OUTER strokes anyway.
                shape = stroke.createStrokedShape(shape);
                stroke = null;
            }
            final Renderer r = OpenPiscesPrismUtils.setupRenderer(shape, stroke, tr, clip);
            alphaConsumer.initConsumer(r, pr);
            r.produceAlphas(alphaConsumer);
        }

        public void dispose() { }
    }

    SWContext(ResourceFactory factory) {
        this.factory = factory;
        this.shapeRenderer = (PrismSettings.doNativePisces) ? new NativeShapeRenderer() : new JavaShapeRenderer();
    }

    void renderShape(PiscesRenderer pr, Shape shape, BasicStroke stroke, BaseTransform tr, Rectangle clip) {
        this.shapeRenderer.renderShape(pr, shape, stroke, tr, clip);
    }

    private SWRTTexture initRBBuffer(int width, int height) {
        final SWRTTexture tex = (SWRTTexture)factory.createRTTexture(width, height, Texture.WrapMode.CLAMP_NOT_NEEDED);
        readBackBufferRef = new SoftReference<SWRTTexture>(tex);
        return tex;
    }

    private void disposeRBBuffer() {
        if (readBackBufferRef != null) {
            readBackBufferRef.clear();
            readBackBufferRef = null;
        }
    }

    SWRTTexture validateRBBuffer(int width, int height) {
        SWRTTexture tex;
        if (readBackBufferRef == null) {
            tex = this.initRBBuffer(width, height);
        } else {
            tex = readBackBufferRef.get();
            if (tex == null ||
                tex.getPhysicalWidth() < width ||
                tex.getPhysicalHeight() < height)
            {
                this.disposeRBBuffer();
                tex = this.initRBBuffer(width, height);
            }
            tex.setContentWidth(width);
            tex.setContentHeight(height);
        }
        return tex;
    }

    private SWArgbPreTexture initImagePaintTexture(int width, int height) {
        final SWArgbPreTexture tex = (SWArgbPreTexture)factory.createTexture(PixelFormat.INT_ARGB_PRE,
                Texture.Usage.DEFAULT, Texture.WrapMode.REPEAT, width, height);
        imagePaintTextureRef = new SoftReference<SWArgbPreTexture>(tex);
        return tex;
    }

    private void disposeImagePaintTexture() {
        if (imagePaintTextureRef != null) {
            imagePaintTextureRef.clear();
            imagePaintTextureRef = null;
        }
    }

    SWArgbPreTexture validateImagePaintTexture(int width, int height) {
        SWArgbPreTexture tex;
        if (imagePaintTextureRef == null) {
            tex = this.initImagePaintTexture(width, height);
        } else {
            tex = imagePaintTextureRef.get();
            if (tex == null ||
                tex.getPhysicalWidth() < width ||
                tex.getPhysicalHeight() < height)
            {
                this.disposeImagePaintTexture();
                tex = this.initImagePaintTexture(width, height);
            }
            tex.setContentWidth(width);
            tex.setContentHeight(height);
        }
        return tex;
    }


    void dispose() {
        this.disposeRBBuffer();
        this.disposeImagePaintTexture();
        this.shapeRenderer.dispose();
    }
}

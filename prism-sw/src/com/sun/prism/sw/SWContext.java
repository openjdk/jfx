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

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.openpisces.Renderer;
import com.sun.pisces.PiscesRenderer;
import com.sun.prism.BasicStroke;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.shape.MaskData;
import com.sun.prism.impl.shape.OpenPiscesPrismUtils;
import com.sun.prism.impl.shape.ShapeUtil;

final class SWContext {

    private final ResourceFactory factory;
    private final ShapeRenderer shapeRenderer;
    private RTTexture readBackBuffer;

    interface ShapeRenderer {
        void renderShape(PiscesRenderer pr, Shape shape, BasicStroke stroke, BaseTransform tr, Rectangle clip);
        void dispose();
    }

    class NativeShapeRenderer implements ShapeRenderer {
        private SWMaskTexture maskTexture;

        public void renderShape(PiscesRenderer pr, Shape shape, BasicStroke stroke, BaseTransform tr, Rectangle clip) {
            final MaskData mask = ShapeUtil.rasterizeShape(shape, stroke, clip.toRectBounds(), tr, true);
            this.validateMaskTexture(mask.getWidth(), mask.getHeight());
            mask.uploadToTexture(maskTexture, 0, 0, false);
            pr.fillAlphaMask(maskTexture.getDataNoClone(), mask.getOriginX(), mask.getOriginY(),
                             mask.getWidth(), mask.getHeight(), 0, maskTexture.getStride());
        }

        private void initMaskTexture(int width, int height) {
            maskTexture = (SWMaskTexture)factory.createMaskTexture(width, height, Texture.WrapMode.CLAMP_NOT_NEEDED);
        }

        private void disposeMaskTexture() {
            if (maskTexture != null){
                maskTexture.dispose();
                maskTexture = null;
            }
        }

        private void validateMaskTexture(int width, int height) {
            if (maskTexture == null ||
                maskTexture.getPhysicalWidth() < width ||
                maskTexture.getPhysicalHeight() < height)
            {
                this.disposeMaskTexture();
                this.initMaskTexture(width, height);
            }
        }

        public void dispose() {
            this.disposeMaskTexture();
        }
    }

    class JavaShapeRenderer implements ShapeRenderer {
        private final DirectRTPiscesAlphaConsumer alphaConsumer = new DirectRTPiscesAlphaConsumer();

        public void renderShape(PiscesRenderer pr, Shape shape, BasicStroke stroke, BaseTransform tr, Rectangle clip) {
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

    RTTexture getReadBackBuffer() {
        return readBackBuffer;
    }

    void renderShape(PiscesRenderer pr, Shape shape, BasicStroke stroke, BaseTransform tr, Rectangle clip) {
        this.shapeRenderer.renderShape(pr, shape, stroke, tr, clip);
    }

    private void initRBBuffer(int width, int height) {
        readBackBuffer = factory.createRTTexture(width, height, Texture.WrapMode.CLAMP_NOT_NEEDED);
    }

    private void disposeRBBuffer() {
        if (readBackBuffer != null) {
            readBackBuffer.dispose();
            readBackBuffer = null;
        }
    }

    void validateRBBuffer(int width, int height) {
        if (readBackBuffer == null ||
            readBackBuffer.getPhysicalWidth() < width ||
            readBackBuffer.getPhysicalHeight() < height)
        {
            this.disposeRBBuffer();
            this.initRBBuffer(width, height);
        }
    }

    void dispose() {
        this.disposeRBBuffer();
        this.shapeRenderer.dispose();
    }
}

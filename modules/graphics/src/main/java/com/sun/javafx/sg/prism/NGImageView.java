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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.RectBounds;
import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.image.CachingCompoundImage;
import com.sun.prism.image.CompoundCoords;
import com.sun.prism.image.Coords;
import com.sun.prism.image.ViewPort;

/**
 */

public class NGImageView extends NGNode {

    private Image image;
    private CachingCompoundImage compoundImage;
    private CompoundCoords compoundCoords;
    private float x, y, w, h;

    // Coords will be null if there was no viewport specified.
    // In case when we draw a huge image, coords are never null.
    private Coords coords;
    private ViewPort reqviewport;  // ViewPort requested by ImageView
    private ViewPort imgviewport;  // ViewPort scaled to the current image

    private boolean renderable = false;
    private boolean coordsOK = false;

    private void invalidate() {
        coordsOK = false;
        coords = null;
        compoundCoords = null;
        imgviewport = null;
        geometryChanged();
    }

    public void setViewport(float vx, float vy, float vw, float vh, float cw, float ch)
    {
        if (vw > 0 && vh > 0) {
            reqviewport = new ViewPort(vx, vy, vw, vh);
        } else {
            reqviewport = null;
        }

        this.w = cw;
        this.h = ch;

        invalidate();
    }

    private void calculatePositionAndClipping() {
        renderable = false;
        coordsOK = true;

        if (reqviewport == null || image == null) {
            renderable = image != null;
            return;
        }

        float iw = image.getWidth();
        float ih = image.getHeight();
        if (iw == 0 || ih == 0) return;
        imgviewport = reqviewport.getScaledVersion(image.getPixelScale());

        coords = imgviewport.getClippedCoords(iw, ih, w, h);
        renderable = coords != null;
    }

    @Override
    protected void doRender(Graphics g) {
        if (!coordsOK) {
            calculatePositionAndClipping();
        }
        if (renderable) {
            super.doRender(g);
        }
    }

    // method for testing reasons
    final static int MAX_SIZE_OVERRIDE = 0; // 64
    private int maxSizeWrapper(ResourceFactory factory) {
        return MAX_SIZE_OVERRIDE > 0 ? MAX_SIZE_OVERRIDE : factory.getMaximumTextureSize();
    }

    @Override
    protected void renderContent(Graphics g) {
        int imgW = image.getWidth();
        int imgH = image.getHeight();

        ResourceFactory factory = g.getResourceFactory();
        int maxSize = maxSizeWrapper(factory);
        if (imgW <= maxSize && imgH <= maxSize) {
            Texture texture = factory.getCachedTexture(image, Texture.WrapMode.CLAMP_TO_EDGE);
            if (coords == null) {
                g.drawTexture(texture, x, y, x + w, y + h, 0, 0, imgW, imgH);
            } else {
                coords.draw(texture, g, x, y);
            }
            texture.unlock();
        } else {
            if (compoundImage == null) compoundImage = new CachingCompoundImage(image, maxSize);
            // coords is null iff there was no viewport specified, but
            // MegaCoords needs a non-null Coords so we create a dummy one
            if (coords == null) coords = new Coords(w, h, new ViewPort(0, 0, imgW, imgH));
            if (compoundCoords == null) compoundCoords = new CompoundCoords(compoundImage, coords);
            compoundCoords.draw(g, compoundImage, x, y);
        }
    }

    @Override
    protected boolean hasOverlappingContents() {
        return false;
    }

    public void setImage(Object img) {
        Image newImage = (Image)img;

        if (image == newImage) return;

        boolean needsInvalidate = newImage == null || image == null
                || image.getPixelScale() != newImage.getPixelScale()
                || image.getHeight() != newImage.getHeight()
                || image.getWidth() != newImage.getWidth();

        image = newImage;
        compoundImage = null;

        if (needsInvalidate) invalidate();
    }

    public void setX(float x) {
        if (this.x != x) {
            this.x = x;
            geometryChanged();
        }
    }

    public void setY(float y) {
        if (this.y != y) {
            this.y = y;
            geometryChanged();
        }
    }

    // RT-18701: this method does nothing
    public void setSmooth(boolean s) {}

    @Override
    protected boolean supportsOpaqueRegions() { return true; }

    @Override
    protected boolean hasOpaqueRegion() {
        // An image, being a raster, needs to be at least 1 pixel in width to have any opaque
        // pixel content, even when scaled up. So we check against w >= 1 and h >= 1 here, unlike
        // in NGCircle or others where we test against > 0.
        assert image == null || (image.getWidth() >= 1 && image.getHeight() >= 1);
        return super.hasOpaqueRegion() && w >= 1 && h >= 1 && image != null && image.isOpaque();
    }

    @Override
    protected RectBounds computeOpaqueRegion(RectBounds opaqueRegion) {
        return (RectBounds) opaqueRegion.deriveWithNewBounds(x, y, 0, x+w, y+h, 0);
    }
}

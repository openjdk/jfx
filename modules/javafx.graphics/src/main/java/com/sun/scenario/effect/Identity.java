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

package com.sun.scenario.effect;

import java.util.HashMap;
import java.util.Map;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;

/**
 * A type of source effect that returns a version of the given
 * platform-specific image object (e.g. a {@code BufferedImage})
 * that is suitable for the destination {@code FilterContext}.
 * No other processing is performed on the specified image object.
 */
public class Identity extends Effect {

    private Filterable src;
    private Point2D loc = new Point2D();
    private final Map<FilterContext, ImageData> datacache =
        new HashMap<>();

    /**
     * Constructs a new {@code Identity} effect with the
     * given platform-specific image.
     *
     * @param src the source image, or null
     */
    public Identity(Filterable src) {
        this.src = src;
    }

    /**
     * Returns the source image (can be null).
     *
     * @return the source image
     */
    public final Filterable getSource() {
        return src;
    }

    /**
     * Sets the source image.
     *
     * @param src the source image, or null
     */
    public void setSource(Filterable src) {
        Filterable old = this.src;
        this.src = src;
        clearCache();
    }

    /**
     * Returns the location of the source image, relative to the untransformed
     * source content bounds.
     *
     * @return the location of the source image
     */
    public final Point2D getLocation() {
        return loc;
    }

    /**
     * Sets the location of the source image, relative to the untransformed
     * source content bounds.
     *
     * @param pt the new location of the source image
     * @throws IllegalArgumentException if {@code pt} is null
     */
    public void setLocation(Point2D pt) {
        if (pt == null) {
            throw new IllegalArgumentException("Location must be non-null");
        }
        Point2D old = this.loc;
        this.loc.setLocation(pt);
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform,
                              Effect defaultInput)
    {
        if (src == null) {
            // just an empty rectangle
            return new RectBounds();
        }
        float srcw = src.getPhysicalWidth() / src.getPixelScale();
        float srch = src.getPhysicalHeight() / src.getPixelScale();
        BaseBounds r = new RectBounds(loc.x, loc.y, loc.x + srcw, loc.y + srch);
        if (transform != null && !transform.isIdentity()) {
            r = transformBounds(transform, r);
        }
        return r;
    }

    @Override
    public ImageData filter(FilterContext fctx,
                            BaseTransform transform,
                            Rectangle outputClip,
                            Object renderHelper,
                            Effect defaultInput)
    {
        // RT-27396
        // TODO: cache needs to be cleared on display changes
        // TODO: cache based on transform?
        ImageData id = datacache.get(fctx);
        if (id != null && !id.addref()) {
            id.setReusable(false);
            datacache.remove(fctx);
            id.unref();
            id = null;
        }
        if (id == null) {
            Renderer r = Renderer.getRenderer(fctx);
            Filterable f = src;
            if (f == null) {
                f = getCompatibleImage(fctx, 1, 1);
                id = new ImageData(fctx, f, new Rectangle(1, 1));
            } else {
                id = r.createImageData(fctx, f);
            }
            if (id == null) {
                return new ImageData(fctx, null, null);
            }
            id.setReusable(true);
            datacache.put(fctx, id);
        }

        transform = Offset.getOffsetTransform(transform, loc.x, loc.y);
        id = id.transform(transform);
        return id;
    }

    @Override
    public AccelType getAccelType(FilterContext fctx) {
        // RT-27396
        // TODO: perhaps we should look at the image type here...
        return AccelType.INTRINSIC;
    }

    private void clearCache() {
        datacache.clear();
    }

    @Override
    public boolean reducesOpaquePixels() {
        return true;
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        DirtyRegionContainer drc = regionPool.checkOut();
        drc.reset();
        return drc;
    }
}

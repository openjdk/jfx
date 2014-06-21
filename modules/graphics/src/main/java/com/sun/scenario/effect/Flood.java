/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

/**
 * An effect that renders a rectangular region that is filled ("flooded")
 * with the given paint.  This is equivalent to rendering a
 * filled rectangle into an image and using an {@code Identity} effect,
 * except that it is more convenient and potentially much more efficient.
 */
public class Flood extends CoreEffect<RenderState> {

    private Object paint;
    private RectBounds bounds = new RectBounds();

    /**
     * Constructs a new {@code Flood} effect using the given platform-specific
     * paint object with an empty bounds for the flood region.
     *
     * @param paint the platform-specific paint object used to flood the region
     * @throws IllegalArgumentException if {@code paint} is null
     */
    public Flood(Object paint) {
        if (paint == null) {
            throw new IllegalArgumentException("Paint must be non-null");
        }
        this.paint = paint;
        updatePeerKey("Flood");
    }

    /**
     * Constructs a new {@code Flood} effect using the given platform-specific
     * paint object to cover the indicated rectangular {@code bounds}.
     *
     * @param paint the platform-specific paint object used to flood the region
     * @param bounds the rectangular area to cover
     * @throws IllegalArgumentException if either {@code paint} or
     *                                  {@code bounds} is null
     */
    public Flood(Object paint, RectBounds bounds) {
        this(paint);
        if (bounds == null) {
            throw new IllegalArgumentException("Bounds must be non-null");
        }
        this.bounds.setBounds(bounds);
    }

    /**
     * Returns the platform-specific paint object used to flood the region.
     *
     * @return the flood paint
     */
    public Object getPaint() {
        return paint;
    }

    /**
     * Sets the platform-specific paint object used to flood the region.
     *
     * @param paint the flood paint
     * @throws IllegalArgumentException if {@code paint} is null
     */
    public void setPaint(Object paint) {
        if (paint == null) {
            throw new IllegalArgumentException("Paint must be non-null");
        }
        Object old = this.paint;
        this.paint = paint;
    }

    public RectBounds getFloodBounds() {
        return new RectBounds(bounds);
    }

    public void setFloodBounds(RectBounds bounds) {
        if (bounds == null) {
            throw new IllegalArgumentException("Bounds must be non-null");
        }
        RectBounds old = new RectBounds(this.bounds);
        this.bounds.setBounds(bounds);
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform,
                              Effect defaultInput)
    {
        return transformBounds(transform, bounds);
    }

    /**
     * Transform the specified point {@code p} from the coordinate space
     * of the primary content input to the coordinate space of the effect
     * output.
     * In essence, this method asks the question "Which output coordinate
     * is most affected by the data at the specified coordinate in the
     * primary source input?"
     * <p>
     * Since no source input is used, any output coordinate is equally
     * affected, or unaffected, by any source coordinate so an undefined
     * point {@code (NaN, NaN)} is returned.
     *
     * @param p the point in the coordinate space of the primary content
     *          input to be transformed
     * @param defaultInput the default input {@code Effect} to be used in
     *                     all cases where a filter has a null input
     * @return the undefined point {@code (NaN, NaN)}
     */
    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        return new Point2D(Float.NaN, Float.NaN);
    }

    /**
     * Transform the specified point {@code p} from the coordinate space
     * of the output of the effect into the coordinate space of the
     * primary content input.
     * In essence, this method asks the question "Which source coordinate
     * contributes most to the definition of the output at the specified
     * coordinate?"
     * <p>
     * Since the Flood effect does not use any source data it returns
     * an undefined coordinate {@code (NaN, NaN)} for all requests.
     *
     * @param p the point in the coordinate space of the result output
     *          to be transformed
     * @param defaultInput the default input {@code Effect} to be used in
     *                     all cases where a filter has a null input
     * @return the undefined point {@code (NaN, NaN)}
     */
    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        return new Point2D(Float.NaN, Float.NaN);
    }

    @Override
    public RenderState getRenderState(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      Object renderHelper,
                                      Effect defaultInput)
    {
        // TODO: Intersect with the flood bounds?  For now just use the
        // stock RenderSpaceRenderState which performs no modifications
        // on the output bounds for its inputs.
        return RenderState.RenderSpaceRenderState;
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

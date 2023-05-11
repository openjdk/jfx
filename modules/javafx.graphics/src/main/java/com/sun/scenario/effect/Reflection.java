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

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

/**
 * An effect that renders a reflected version of the input below the
 * actual input content.
 */
public class Reflection extends CoreEffect<RenderState> {

    private float topOffset;
    private float topOpacity;
    private float bottomOpacity;
    private float fraction;

    /**
     * Constructs a new {@code Reflection} effect with default values,
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new Reflection(DefaultInput)
     * </pre>
     */
    public Reflection() {
        this(DefaultInput);
    }

    /**
     * Constructs a new {@code Reflection} effect with default values.
     *
     * @param input the single input {@code Effect}
     */
    public Reflection(Effect input) {
        super(input);
        this.topOffset = 0f;
        this.topOpacity = 0.5f;
        this.bottomOpacity = 0f;
        this.fraction = 0.75f;
        updatePeerKey("Reflection");
    }

    /**
     * Returns the input for this {@code Effect}.
     *
     * @return the input for this {@code Effect}
     */
    public final Effect getInput() {
        return getInputs().get(0);
    }

    /**
     * Sets the input for this {@code Effect} to a specific
     * {@code Effect} or to the default input if {@code input} is
     * {@code null}.
     *
     * @param input the input for this {@code Effect}
     */
    public void setInput(Effect input) {
        setInput(0, input);
    }

    /**
     * Returns the top offset adjustment, which is the distance between the
     * bottom of the input and the top of the reflection.
     *
     * @return the top offset adjustment
     */
    public float getTopOffset() {
        return topOffset;
    }

    /**
     * Sets the top offset adjustment, which is the distance between the
     * bottom of the input and the top of the reflection.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     *
     * @param topOffset the top offset adjustment
     */
    public void setTopOffset(float topOffset) {
        float old = this.topOffset;
        this.topOffset = topOffset;
    }

    /**
     * Returns the top opacity value, which is the opacity of the reflection
     * at its top extreme.
     *
     * @return the top opacity value
     */
    public float getTopOpacity() {
        return topOpacity;
    }

    /**
     * Sets the top opacity value, which is the opacity of the reflection
     * at its top extreme.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.5
     *  Identity: 1.0
     * </pre>
     *
     * @param topOpacity the top opacity value
     * @throws IllegalArgumentException if {@code topOpacity} is outside the
     * allowable range
     */
    public void setTopOpacity(float topOpacity) {
        if (topOpacity < 0f || topOpacity > 1f) {
            throw new IllegalArgumentException("Top opacity must be in the range [0,1]");
        }
        float old = this.topOpacity;
        this.topOpacity = topOpacity;
    }

    /**
     * Returns the bottom opacity value, which is the opacity of the reflection
     * at its bottom extreme.
     *
     * @return the bottom opacity value
     */
    public float getBottomOpacity() {
        return bottomOpacity;
    }

    /**
     * Sets the bottom opacity value, which is the opacity of the reflection
     * at its bottom extreme.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.0
     *  Identity: 1.0
     * </pre>
     *
     * @param bottomOpacity the bottom opacity value
     * @throws IllegalArgumentException if {@code bottomOpacity} is outside the
     * allowable range
     */
    public void setBottomOpacity(float bottomOpacity) {
        if (bottomOpacity < 0f || bottomOpacity > 1f) {
            throw new IllegalArgumentException("Bottom opacity must be in the range [0,1]");
        }
        float old = this.bottomOpacity;
        this.bottomOpacity = bottomOpacity;
    }

    /**
     * Returns the fraction of the input that is visible in the reflection.
     *
     * @return the fraction value
     */
    public float getFraction() {
        return fraction;
    }

    /**
     * Sets the fraction of the input that is visible in the reflection.
     * For example, a value of 0.5 means that only the bottom half of the
     * input will be visible in the reflection.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.75
     *  Identity: 1.0
     * </pre>
     *
     * @param fraction the fraction of the input that is visible
     * in the reflection
     * @throws IllegalArgumentException if {@code fraction} is outside the
     * allowable range
     */
    public void setFraction(float fraction) {
        if (fraction < 0f || fraction > 1f) {
            throw new IllegalArgumentException("Fraction must be in the range [0,1]");
        }
        float old = this.fraction;
        this.fraction = fraction;
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform,
                              Effect defaultInput)
    {
        Effect input = getDefaultedInput(0, defaultInput);
        BaseBounds r = input.getBounds(BaseTransform.IDENTITY_TRANSFORM, defaultInput);
        r.roundOut(); // NOTE is this really necessary?
        float x1 = r.getMinX();
        float y1 = r.getMaxY() + topOffset;
        float x2 = r.getMaxX();
        float y2 = y1 + (fraction * r.getHeight());
        BaseBounds ret = new RectBounds(x1, y1, x2, y2);
        ret = ret.deriveWithUnion(r);
        return transformBounds(transform, ret);
    }

    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        return getDefaultedInput(0, defaultInput).transform(p, defaultInput);
    }

    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        return getDefaultedInput(0, defaultInput).untransform(p, defaultInput);
    }

    @Override
    public RenderState getRenderState(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      Object renderHelper,
                                      Effect defaultInput)
    {
        // RT-27405
        // TODO: We could calculate which parts are needed based on the two
        // ways that the input is rendered into this ouput rectangle. For now,
        // we will just use the stock object that requests unclipped inputs.
        return RenderState.UnclippedUserSpaceRenderState;
    }

    @Override
    public boolean reducesOpaquePixels() {
        final Effect input = getInput();
        return input != null && input.reducesOpaquePixels();
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        Effect di = getDefaultedInput(0, defaultInput);
        DirtyRegionContainer drc = di.getDirtyRegions(defaultInput, regionPool);

        BaseBounds contentBounds = di.getBounds(BaseTransform.IDENTITY_TRANSFORM, defaultInput);
        float cbMaxY = contentBounds.getMaxY();
        float reflectedMaxYBase = (2 * cbMaxY) + getTopOffset();
        float reflecteCbMaxY = cbMaxY + getTopOffset() + (fraction * contentBounds.getHeight());
        DirtyRegionContainer newDRC = regionPool.checkOut();
        for (int i = 0; i < drc.size(); i++) {
            BaseBounds regionBounds = drc.getDirtyRegion(i);
            float reflectedRegionMinY = reflectedMaxYBase - regionBounds.getMaxY();
            float reflectedRegionMaxY = Math.min(reflecteCbMaxY, reflectedRegionMinY + regionBounds.getHeight());

            newDRC.addDirtyRegion(new RectBounds(regionBounds.getMinX(),
                                                 reflectedRegionMinY,
                                                 regionBounds.getMaxX(),
                                                 reflectedRegionMaxY));
        }
        drc.merge(newDRC);
        regionPool.checkIn(newDRC);

        return drc;
    }

}

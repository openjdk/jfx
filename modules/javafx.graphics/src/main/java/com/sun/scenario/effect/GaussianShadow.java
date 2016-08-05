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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.GaussianShadowState;
import com.sun.scenario.effect.impl.state.LinearConvolveKernel;

/**
 * A blurred shadow effect using a Gaussian convolution kernel, with a
 * configurable radius and shadow color.  Only the alpha channel of the
 * input is used to create the shadow effect.  The alpha value of each
 * pixel from the result of the blur operation is modulated with the
 * specified shadow color to produce the resulting image.
 */
public class GaussianShadow extends AbstractShadow {

    private GaussianShadowState state = new GaussianShadowState();

    /**
     * Constructs a new {@code GaussianShadow} effect with the default radius
     * (10.0) and the default color ({@code Color4f.BLACK}), using the
     * default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new GaussianShadow(10f, Color4f.BLACK, DefaultInput)
     * </pre>
     */
    public GaussianShadow() {
        this(10f);
    }

    /**
     * Constructs a new {@code GaussianShadow} effect with the given radius
     * and the default color ({@code Color4f.BLACK}), using the
     * default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new GaussianShadow(radius, Color4f.BLACK, DefaultInput)
     * </pre>
     *
     * @param radius the radius of the Gaussian kernel
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public GaussianShadow(float radius) {
        this(radius, Color4f.BLACK);
    }

    /**
     * Constructs a new {@code GaussianShadow} effect with the given radius
     * and color, using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new GaussianShadow(radius, color, DefaultInput)
     * </pre>
     *
     * @param radius the radius of the Gaussian kernel
     * @param color the shadow {@code Color4f}
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public GaussianShadow(float radius, Color4f color) {
        this(radius, color, DefaultInput);
    }

    /**
     * Constructs a new {@code GaussianShadow} effect with the given
     * radius and color.
     *
     * @param radius the radius of the Gaussian kernel
     * @param color the shadow {@code Color4f}
     * @param input the single input {@code Effect}
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range, or if {@code color} is null
     */
    public GaussianShadow(float radius, Color4f color, Effect input) {
        super(input);
        state.setRadius(radius);
        state.setShadowColor(color);
    }


    @Override
    LinearConvolveKernel getState() {
        return state;
    }

    @Override
    public AccelType getAccelType(FilterContext fctx) {
        return Renderer.getRenderer(fctx).getAccelType();
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
     * Sets the input for this {@code Effect} to a specific {@code Effect}
     * or to the default input if {@code input} is {@code null}.
     *
     * @param input the input for this {@code Effect}
     */
    public void setInput(Effect input) {
        setInput(0, input);
    }

    /**
     * Returns the radius of the Gaussian kernel.
     *
     * @return the radius of the Gaussian kernel
     */
    public float getRadius() {
        return state.getRadius();
    }

    /**
     * Sets the radius of the Gaussian kernel.
     * <pre>
     *       Min:   0.0
     *       Max: 127.0
     *   Default:  10.0
     *  Identity:   0.0
     * </pre>
     *
     * @param radius the radius of the Gaussian kernel
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public void setRadius(float radius) {
        float old = state.getRadius();
        state.setRadius(radius);
    }

    /**
     * Returns the horizontal radius of the Gaussian kernel.
     *
     * @return the horizontal radius of the Gaussian kernel
     */
    public float getHRadius() {
        return state.getHRadius();
    }

    /**
     * Sets the horizontal radius of the Gaussian kernel.
     * <pre>
     *       Min:   0.0
     *       Max: 127.0
     *   Default:  10.0
     *  Identity:   0.0
     * </pre>
     *
     * @param hradius the horizontal radius of the Gaussian kernel
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public void setHRadius(float hradius) {
        float old = state.getHRadius();
        state.setHRadius(hradius);
    }

    /**
     * Returns the vertical radius of the Gaussian kernel.
     *
     * @return the vertical radius of the Gaussian kernel
     */
    public float getVRadius() {
        return state.getVRadius();
    }

    /**
     * Sets the vertical radius of the Gaussian kernel.
     * <pre>
     *       Min:   0.0
     *       Max: 127.0
     *   Default:  10.0
     *  Identity:   0.0
     * </pre>
     *
     * @param vradius the vertical radius of the Gaussian kernel
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public void setVRadius(float vradius) {
        float old = state.getVRadius();
        state.setVRadius(vradius);
    }

    /**
     * Gets the spread of the shadow effect.
     *
     * @return the spread of the shadow effect
     */
    public float getSpread() {
        return state.getSpread();
    }

    /**
     * Sets the spread of the shadow effect.
     * The spread is the portion of the radius where the contribution of
     * the source material will be 100%.
     * The remaining portion of the radius will have a contribution
     * controlled by the Gaussian kernel.
     * A spread of {@code 0.0} will result in a pure Gaussian distribution
     * of the shadow.
     * A spread of {@code 1.0} will result in a solid growth outward of the
     * source material opacity to the limit of the radius with a very sharp
     * cutoff to transparency at the radius.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     *
     * @param spread the spread of the shadow effect
     * @throws IllegalArgumentException if {@code spread} is outside the
     * allowable range
     */
    public void setSpread(float spread) {
        float old = state.getSpread();
        state.setSpread(spread);
    }

    /**
     * Returns the shadow color.
     *
     * @return the shadow color
     */
    public Color4f getColor() {
        return state.getShadowColor();
    }

    /**
     * Sets the shadow color.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: Color4f.BLACK
     *  Identity: n/a
     * </pre>
     *
     * @param color the shadow color
     * @throws IllegalArgumentException if {@code color} is null
     */
    public void setColor(Color4f color) {
        Color4f old = state.getShadowColor();
        state.setShadowColor(color);
    }

    public float getGaussianRadius() {
        return getRadius();
    }

    public float getGaussianWidth() {
        return getHRadius() * 2.0f + 1.0f;
    }

    public float getGaussianHeight() {
        return getVRadius() * 2.0f + 1.0f;
    }

    public void setGaussianRadius(float r) {
        setRadius(r);
    }

    public void setGaussianWidth(float w) {
        setHRadius(w < 1.0f ? 0.0f : ((w - 1.0f) / 2.0f));
    }

    public void setGaussianHeight(float h) {
        setVRadius(h < 1.0f ? 0.0f : ((h - 1.0f) / 2.0f));
    }

    public ShadowMode getMode() {
        return ShadowMode.GAUSSIAN;
    }

    public AbstractShadow implFor(ShadowMode mode) {
        int passes = 0;
        switch (mode) {
            case GAUSSIAN:
                return this;
            case ONE_PASS_BOX:
                passes = 1;
                break;
            case TWO_PASS_BOX:
                passes = 2;
                break;
            case THREE_PASS_BOX:
                passes = 3;
                break;
        }
        BoxShadow box = new BoxShadow();
        box.setInput(getInput());
        box.setGaussianWidth(getGaussianWidth());
        box.setGaussianHeight(getGaussianHeight());
        box.setColor(getColor());
        box.setPasses(passes);
        box.setSpread(getSpread());
        return box;
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform, Effect defaultInput) {
        BaseBounds r = super.getBounds(null, defaultInput);
        int hpad = state.getPad(0);
        int vpad = state.getPad(1);
        RectBounds ret = new RectBounds(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
        ret.grow(hpad, vpad);
        return transformBounds(transform, ret);
    }

    @Override
    public Rectangle getResultBounds(BaseTransform transform,
                                     Rectangle outputClip,
                                     ImageData... inputDatas)
    {
        Rectangle r = super.getResultBounds(transform, outputClip, inputDatas);
        int hpad = state.getPad(0);
        int vpad = state.getPad(1);
        Rectangle ret = new Rectangle(r);
        ret.grow(hpad, vpad);
        return ret;
    }

    @Override
    public boolean reducesOpaquePixels() {
        return true;
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        Effect di = getDefaultedInput(0, defaultInput);
        DirtyRegionContainer drc = di.getDirtyRegions(defaultInput, regionPool);

        drc.grow(state.getPad(0), state.getPad(1));

        return drc;
    }
}

/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.scenario.effect.impl.state.BoxShadowState;
import com.sun.scenario.effect.impl.state.LinearConvolveKernel;

/**
 * A shadow effect using a box-shaped convolution kernel, with a configurable
 * size for each dimension of the kernel and a number of passes to control
 * the quality of the blur.
 */
public class BoxShadow extends AbstractShadow {

    private final BoxShadowState state = new BoxShadowState();

    /**
     * Constructs a new {@code BoxShadow} effect with
     * the default blur sizes (1, 1)
     * and the default number of passes (1),
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new BoxShadow(1, 1, 1, DefaultInput)
     * </pre>
     */
    public BoxShadow() {
        this(1, 1);
    }

    /**
     * Constructs a new {@code BoxShadow} effect with
     * the given blur sizes
     * and the default number of passes (1),
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new BoxShadow(hsize, vsize, 1, DefaultInput)
     * </pre>
     *
     * @param hsize the horizontal size of the BoxShadow kernel
     * @param vsize the vertical size of the BoxShadow kernel
     * @throws IllegalArgumentException if either {@code hsize}
     * or {@code vsize} is outside the allowable range
     */
    public BoxShadow(int hsize, int vsize) {
        this(hsize, vsize, 1, DefaultInput);
    }

    /**
     * Constructs a new {@code BoxShadow} effect with
     * the given blur sizes
     * and number of passes,
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new BoxShadow(hsize, vsize, passes, DefaultInput)
     * </pre>
     *
     * @param hsize the horizontal size of the BoxShadow kernel
     * @param vsize the vertical size of the BoxShadow kernel
     * @param passes the number of blur passes to execute
     * @throws IllegalArgumentException if either {@code hsize}
     * or {@code vsize} or {@code passes}
     * is outside the allowable range
     */
    public BoxShadow(int hsize, int vsize, int passes) {
        this(hsize, vsize, passes, DefaultInput);
    }

    /**
     * Constructs a new {@code BoxShadow} effect with
     * the given blur sizes
     * and number of passes,
     * using the output of the specified effect for source data.
     *
     * @param hsize the horizontal size of the BoxShadow kernel
     * @param vsize the vertical size of the BoxShadow kernel
     * @param passes the number of blur passes to execute
     * @param input the single input {@code Effect}
     * @throws IllegalArgumentException if either {@code hsize}
     * or {@code vsize} or {@code passes}
     * is outside the allowable range
     */
    public BoxShadow(int hsize, int vsize, int passes, Effect input) {
        super(input);
        setHorizontalSize(hsize);
        setVerticalSize(vsize);
        setPasses(passes);
        setColor(Color4f.BLACK);
        setSpread(0f);
    }

    @Override
    LinearConvolveKernel getState() {
        return state;
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
     * Sets the input for this {@code Effect}.
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
     * Returns the horizontal size of the effect kernel.
     *
     * @return the horizontal size of the effect kernel
     */
    public int getHorizontalSize() {
        return state.getHsize();
    }

    /**
     * Sets the horizontal size of the effect kernel.
     * <pre>
     *       Min:   0
     *       Max: 255
     *   Default:   1
     *  Identity:   0
     * </pre>
     *
     * @param hsize the horizontal size of the effect kernel
     * @throws IllegalArgumentException if {@code hsize}
     * is outside the allowable range
     */
    public final void setHorizontalSize(int hsize) {
        state.setHsize(hsize);
    }

    /**
     * Returns the vertical size of the effect kernel.
     *
     * @return the vertical size of the effect kernel
     */
    public int getVerticalSize() {
        return state.getVsize();
    }

    /**
     * Sets the vertical size of the effect kernel.
     * <pre>
     *       Min:   0
     *       Max: 255
     *   Default:   1
     *  Identity:   0
     * </pre>
     *
     * @param vsize the vertical size of the effect kernel
     * @throws IllegalArgumentException if {@code vsize}
     * is outside the allowable range
     */
    public final void setVerticalSize(int vsize) {
        state.setVsize(vsize);
    }

    /**
     * Returns the number of passes of the effect kernel to control the
     * quality of the blur.
     *
     * @return the number of passes of the effect kernel
     */
    public int getPasses() {
        return state.getBlurPasses();
    }

    /**
     * Sets the number of passes of the effect kernel to control the
     * quality of the blur.
     * <pre>
     *       Min:   0
     *       Max:   3
     *   Default:   1
     *  Identity:   0
     * </pre>
     * A setting of 1 creates a low quality blur.  A setting of 3 creates
     * a blur that is very close to a Gaussian blur.
     *
     * @param passes
     * @throws IllegalArgumentException if {@code passes} is outside the
     * allowable range
     */
    public final void setPasses(int passes) {
        state.setBlurPasses(passes);
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
    public final void setColor(Color4f color) {
        state.setShadowColor(color);
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
     * controlled by the Blur kernel.
     * A spread of {@code 0.0} will result in a pure box-blur distribution
     * of the shadow.
     * A spread of {@code 1.0} will result in a solid growth outward of the
     * source material opacity to the limit of the kernel sizes with a very
     * sharp cutoff to transparency at the edge of the kernel.
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
    public final void setSpread(float spread) {
        state.setSpread(spread);
    }

    public float getGaussianRadius() {
        float d = (getHorizontalSize() + getVerticalSize()) / 2.0f;
        d *= 3.0f;
        return (d < 1.0f ? 0.0f : ((d - 1.0f) / 2.0f));
    }

    public float getGaussianWidth() {
        return getHorizontalSize() * 3.0f;
    }

    public float getGaussianHeight() {
        return getVerticalSize() * 3.0f;
    }

    public void setGaussianRadius(float r) {
        float d = r * 2.0f + 1.0f;
        setGaussianWidth(d);
        setGaussianHeight(d);
    }

    public void setGaussianWidth(float w) {
        w /= 3.0f;
        setHorizontalSize(Math.round(w));
    }

    public void setGaussianHeight(float h) {
        h /= 3.0f;
        setVerticalSize(Math.round(h));
    }

    public ShadowMode getMode() {
        switch (getPasses()) {
            case 1:
                return ShadowMode.ONE_PASS_BOX;
            case 2:
                return ShadowMode.TWO_PASS_BOX;
            default:
                return ShadowMode.THREE_PASS_BOX;
        }
    }

    public AbstractShadow implFor(ShadowMode mode) {
        switch (mode) {
            case GAUSSIAN:
                GaussianShadow gs = new GaussianShadow();
                gs.setInput(getInput());
                gs.setGaussianWidth(getGaussianWidth());
                gs.setGaussianHeight(getGaussianHeight());
                gs.setColor(getColor());
                gs.setSpread(getSpread());
                return gs;
            case ONE_PASS_BOX:
                setPasses(1);
                break;
            case TWO_PASS_BOX:
                setPasses(2);
                break;
            case THREE_PASS_BOX:
                setPasses(3);
                break;
        }
        return this;
    }

    @Override
    public AccelType getAccelType(FilterContext fctx) {
        return Renderer.getRenderer(fctx).getAccelType();
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform, Effect defaultInput) {
        BaseBounds r = super.getBounds(null, defaultInput);
        int hgrow = state.getKernelSize(0) / 2;
        int vgrow = state.getKernelSize(1) / 2;
        RectBounds ret = new RectBounds(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
        ret.grow(hgrow, vgrow);
        return transformBounds(transform, ret);
    }

    @Override
    public Rectangle getResultBounds(BaseTransform transform,
                                     Rectangle outputClip,
                                     ImageData... inputDatas)
    {
        Rectangle r = inputDatas[0].getUntransformedBounds();
        r = state.getResultBounds(r, 0);
        r = state.getResultBounds(r, 1);
        r.intersectWith(outputClip);
        return r;
    }

    @Override
    public boolean reducesOpaquePixels() {
        return true;
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        Effect di = getDefaultedInput(0, defaultInput);
        DirtyRegionContainer drc = di.getDirtyRegions(defaultInput, regionPool);

        drc.grow(state.getKernelSize(0) / 2, state.getKernelSize(1) / 2);

        return drc;
    }
}

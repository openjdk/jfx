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
import com.sun.scenario.effect.impl.state.BoxBlurState;
import com.sun.scenario.effect.impl.state.LinearConvolveKernel;

/**
 * A blur effect using a box-shaped convolution kernel, with a configurable
 * size for each dimension of the kernel and a number of passes to control
 * the quality of the blur.
 */
public class BoxBlur extends LinearConvolveCoreEffect {

    private final BoxBlurState state = new BoxBlurState();

    /**
     * Constructs a new {@code BoxBlur} effect with
     * the default blur sizes (1, 1)
     * and the default number of passes (1),
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new BoxBlur(1, 1, 1, DefaultInput)
     * </pre>
     */
    public BoxBlur() {
        this(1, 1);
    }

    /**
     * Constructs a new {@code BoxBlur} effect with
     * the given blur sizes
     * and the default number of passes (1),
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new BoxBlur(hsize, vsize, 1, DefaultInput)
     * </pre>
     *
     * @param hsize the horizontal size of the BoxBlur kernel
     * @param vsize the vertical size of the BoxBlur kernel
     * @throws IllegalArgumentException if either {@code hsize}
     * or {@code vsize} is outside the allowable range
     */
    public BoxBlur(int hsize, int vsize) {
        this(hsize, vsize, 1, DefaultInput);
    }

    /**
     * Constructs a new {@code BoxBlur} effect with
     * the given blur sizes
     * and number of passes,
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new BoxBlur(hsize, vsize, passes, DefaultInput)
     * </pre>
     *
     * @param hsize the horizontal size of the BoxBlur kernel
     * @param vsize the vertical size of the BoxBlur kernel
     * @param passes the number of blur passes to execute
     * @throws IllegalArgumentException if either {@code hsize}
     * or {@code vsize} or {@code passes}
     * is outside the allowable range
     */
    public BoxBlur(int hsize, int vsize, int passes) {
        this(hsize, vsize, passes, DefaultInput);
    }

    /**
     * Constructs a new {@code BoxBlur} effect with
     * the given blur sizes
     * and number of passes,
     * using the output of the specified effect for source data.
     *
     * @param hsize the horizontal size of the BoxBlur kernel
     * @param vsize the vertical size of the BoxBlur kernel
     * @param passes the number of blur passes to execute
     * @param input the single input {@code Effect}
     * @throws IllegalArgumentException if either {@code hsize}
     * or {@code vsize} or {@code passes}
     * is outside the allowable range
     */
    public BoxBlur(int hsize, int vsize, int passes, Effect input) {
        super(input);
        setHorizontalSize(hsize);
        setVerticalSize(vsize);
        setPasses(passes);
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
        Rectangle r = inputDatas[0].getTransformedBounds(null);
        r = state.getResultBounds(r, 0);
        r = state.getResultBounds(r, 1);
        r.intersectWith(outputClip);
        return r;
    }

    @Override
    public boolean reducesOpaquePixels() {
        if (!state.isNop()) {
            return true;
        }
        final Effect input = getInput();
        return input != null && input.reducesOpaquePixels();
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        Effect di = getDefaultedInput(0, defaultInput);
        DirtyRegionContainer drc = di.getDirtyRegions(defaultInput, regionPool);

        drc.grow(state.getKernelSize(0) / 2, state.getKernelSize(1) / 2);

        return drc;
    }
}

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

import com.sun.scenario.effect.impl.state.ZoomRadialBlurState;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

/**
 * Zoom radial blur effect with a configurable center and radius of the kernel.
 */
public class ZoomRadialBlur extends CoreEffect<RenderState> {

    private int r;
    private float centerX;
    private float centerY;
    private final ZoomRadialBlurState state = new ZoomRadialBlurState(this);

    /**
     * Constructs a new {@code ZoomRadialBlur} effect with the default
     * radius (1), using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new ZoomRadialBlur(1, DefaultInput)
     * </pre>
     */
    public ZoomRadialBlur() {
        this(1);
    }

    /**
     * Constructs a new {@code ZoomRadialBlur} effect with the given radius,
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new ZoomRadialBlur(radius, DefaultInput)
     * </pre>
     *
     * @param radius the radius of the kernel
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public ZoomRadialBlur(int radius) {
        this(radius, DefaultInput);
    }

    /**
     * Constructs a new {@code ZoomRadialBlur} effect with the given radius.
     *
     * @param radius of ZoomRadialBlur
     * @param input the single input {@code Effect}
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public ZoomRadialBlur(int radius, Effect input) {
        super(input);
        setRadius(radius);
    }

    @Override
    Object getState() {
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
     * Returns the radius of the effect kernel.
     *
     * @return the radius of the effect kernel
     */
    public int getRadius() {
        return r;
    }

    /**
     * Sets the radius of the effect kernel.
     * <pre>
     *       Min:   1
     *       Max:  64
     *   Default:   1
     *  Identity: n/a
     * </pre>
     *
     * @param radius the radius of the effect kernel
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public void setRadius(int radius) {
        if (radius < 1 || radius > 64) {
            throw new IllegalArgumentException("Radius must be in the range [1,64]");
        }
        int old = this.r;
        this.r = radius;
        state.invalidateDeltas();
        updatePeer();
    }

    /**
     * Updates the peer "key" for the current radius.
     */
    private void updatePeer() {
        int psize = 4 + r - (r%4);
        updatePeerKey("ZoomRadialBlur", psize);
    }

    /**
     * Returns the X coordinate of the center point of this effect.
     *
     * @return the X coordinate of the center point
     */
    public float getCenterX() {
        return centerX;
    }

    /**
     * Sets the X coordinate of the center point of this effect.
     *
     * @param centerX the X coordinate of the center point
     */
    public void setCenterX(float centerX) {
        float old = this.centerX;
        this.centerX = centerX;
    }

    /**
     * Returns the Y coordinate of the center point of this effect.
     *
     * @return the Y coordinate of the center point
     */
    public float getCenterY() {
        return centerY;
    }

    /**
     * Sets the Y coordinate of the center point of this effect.
     *
     * @param centerY the Y coordinate of the center point
     */
    public void setCenterY(float centerY) {
        float old = this.centerY;
        this.centerY = centerY;
    }

    @Override
    public ImageData filterImageDatas(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      RenderState rstate,
                                      ImageData... inputs)
    {
        Rectangle bnd = inputs[0].getUntransformedBounds();
        state.updateDeltas(1f/bnd.width, 1f/bnd.height);
        return super.filterImageDatas(fctx, transform, outputClip, rstate, inputs);
    }

    @Override
    public RenderState getRenderState(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      Object renderHelper,
                                      Effect defaultInput)
    {
        // This effect does not appear to expand bounds as it operates,
        // the blur may zoom "outward", but only to the edge of the
        // original image...
        return RenderState.UserSpaceRenderState;
    }

    @Override
    public boolean reducesOpaquePixels() {
        return true;
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        Effect di = getDefaultedInput(0, defaultInput);
        DirtyRegionContainer drc = di.getDirtyRegions(defaultInput, regionPool);

        int radius = getRadius();
        drc.grow(radius, radius);

        return drc;
    }
}

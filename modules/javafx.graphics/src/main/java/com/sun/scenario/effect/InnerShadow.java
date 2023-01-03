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
import com.sun.javafx.geom.transform.BaseTransform;

/**
 * A high-level effect that renders a shadow inside the edges of the
 * given content with the specified color, radius, and offset.
 */
public class InnerShadow extends DelegateEffect {

    private final InvertMask invert;
    private AbstractShadow shadow;
    private final Blend blend;

    /**
     * Constructs a new {@code InnerShadow} effect, with the default
     * blur radius (10.0), x offset (0.0), and y offset (0.0), using the
     * default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new InnerShadow(DefaultInput, DefaultInput)
     * </pre>
     */
    public InnerShadow() {
        this(DefaultInput, DefaultInput);
    }

    /**
     * Constructs a new {@code InnerShadow} effect, with the default
     * blur radius (10.0), x offset (0.0), and y offset (0.0).
     * This is a shorthand equivalent to:
     * <pre>
     *     new InnerShadow(input, input);
     * </pre>
     *
     * @param input the single input {@code Effect}
     */
    public InnerShadow(Effect input) {
        this(input, input);
    }

    /**
     * Constructs a new {@code InnerShadow} effect, with the default
     * blur radius (10.0), x offset (0.0), and y offset (0.0).
     * <p>
     * This constructor is intended for advanced developers only.  Most
     * developers will only ever need to use the default constructor.
     * <p>
     * The {@code shadowSourceInput} is used to create the background shadow,
     * and the {@code contentInput} is used to render the content over that
     * shadow.
     *
     * @param shadowSourceInput the input {@code Effect} used to create
     * the background shadow
     * @param contentInput the input {@code Effect} used to render the content
     * over the shadow
     */
    public InnerShadow(Effect shadowSourceInput, Effect contentInput) {
        super(shadowSourceInput, contentInput);
        //
        //          (ssInput)
        //             |
        //           Invert
        //             |
        //  (cInput) Shadow
        //       |   |
        //       Blend
        //         |
        //
        this.invert = new InvertMask(10, shadowSourceInput);
        this.shadow = new GaussianShadow(10f, Color4f.BLACK, invert);
        this.blend = new Blend(Blend.Mode.SRC_ATOP, contentInput, shadow);
    }

    public AbstractShadow.ShadowMode getShadowMode() {
        return shadow.getMode();
    }

    public void setShadowMode(AbstractShadow.ShadowMode mode) {
        AbstractShadow.ShadowMode old = shadow.getMode();
        AbstractShadow s = shadow.implFor(mode);
        if (s != shadow) {
            blend.setTopInput(s);
        }
        this.shadow = s;
    }

    @Override
    protected Effect getDelegate() {
        return blend;
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform,
                              Effect defaultInput)
    {
        Effect input = getDefaultedInput(getContentInput(), defaultInput);
        return input.getBounds(transform, defaultInput);
    }

    /**
     * Returns the shadow source input for this {@code Effect}.
     *
     * @return the shadow source input for this {@code Effect}
     */
    public final Effect getShadowSourceInput() {
        return invert.getInput();
    }

    /**
     * Sets the shadow source input for this {@code Effect} to a specific
     * {@code Effect} or to the default input if {@code input} is
     * {@code null}.
     *
     * @param shadowSourceInput the shadow source input for this {@code Effect}
     */
    public void setShadowSourceInput(Effect shadowSourceInput) {
        invert.setInput(shadowSourceInput);
    }

    /**
     * Returns the content input for this {@code Effect}.
     *
     * @return the content input for this {@code Effect}
     */
    public final Effect getContentInput() {
        return blend.getBottomInput();
    }

    /**
     * Sets the content input for this {@code Effect} to a specific
     * {@code Effect} or to the default input if {@code input} is
     * {@code null}.
     *
     * @param contentInput the content input for this {@code Effect}
     */
    public void setContentInput(Effect contentInput) {
        blend.setBottomInput(contentInput);
    }

    /**
     * Returns the radius of the Gaussian kernel.
     *
     * @return the radius of the Gaussian kernel
     */
    public float getRadius() {
        return shadow.getGaussianRadius();
    }

    /**
     * Sets the radius of the shadow blur kernel.
     * <pre>
     *       Min:   0.0
     *       Max: 127.0
     *   Default:  10.0
     *  Identity:   0.0
     * </pre>
     *
     * @param radius the radius of the shadow blur kernel
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public void setRadius(float radius) {
        float old = shadow.getGaussianRadius();
        invert.setPad((int)Math.ceil(radius));
        shadow.setGaussianRadius(radius);
    }

    public float getGaussianRadius() {
        return shadow.getGaussianRadius();
    }

    public float getGaussianWidth() {
        return shadow.getGaussianWidth();
    }

    public float getGaussianHeight() {
        return shadow.getGaussianHeight();
    }

    public void setGaussianRadius(float r) {
        setRadius(r);
    }

    public void setGaussianWidth(float w) {
        float old = shadow.getGaussianWidth();
        float maxr = (Math.max(w, shadow.getGaussianHeight()) - 1.0f) / 2.0f;
        invert.setPad((int) Math.ceil(maxr));
        shadow.setGaussianWidth(w);
    }

    public void setGaussianHeight(float h) {
        float old = shadow.getGaussianHeight();
        float maxr = (Math.max(shadow.getGaussianWidth(), h) - 1.0f) / 2.0f;
        invert.setPad((int) Math.ceil(maxr));
        shadow.setGaussianHeight(h);
    }

    /**
     * Gets the choke of the shadow effect.
     *
     * @return the choke of the shadow effect
     */
    public float getChoke() {
        return shadow.getSpread();
    }

    /**
     * Sets the choke of the shadow effect.
     * The choke is the portion of the radius where the contribution of
     * the source material will be 100%.
     * The remaining portion of the radius will have a contribution
     * controlled by the Gaussian kernel.
     * A choke of {@code 0.0} will result in a pure Gaussian distribution
     * of the shadow.
     * A choke of {@code 1.0} will result in a solid growth inward of the
     * shadow from the edges to the limit of the radius with a very sharp
     * cutoff to transparency inside the radius.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     *
     * @param choke the choke of the shadow effect
     * @throws IllegalArgumentException if {@code choke} is outside the
     * allowable range
     */
    public void setChoke(float choke) {
        float old = shadow.getSpread();
        shadow.setSpread(choke);
    }

    /**
     * Returns the shadow color.
     *
     * @return the shadow color
     */
    public Color4f getColor() {
        return shadow.getColor();
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
        Color4f old = shadow.getColor();
        shadow.setColor(color);
    }

    /**
     * Returns the offset in the x direction, in pixels.
     *
     * @return the offset in the x direction, in pixels.
     */
    public int getOffsetX() {
        return invert.getOffsetX();
    }

    /**
     * Sets the offset in the x direction, in pixels.
     * <pre>
     *       Min: Integer.MIN_VALUE
     *       Max: Integer.MAX_VALUE
     *   Default: 0
     *  Identity: 0
     * </pre>
     *
     * @param xoff the offset in the x direction, in pixels
     */
    public void setOffsetX(int xoff) {
        int old = invert.getOffsetX();
        invert.setOffsetX(xoff);
    }

    /**
     * Returns the offset in the x direction, in pixels.
     *
     * @return the offset in the x direction, in pixels.
     */
    public int getOffsetY() {
        return invert.getOffsetY();
    }

    /**
     * Sets the offset in the y direction, in pixels.
     * <pre>
     *       Min: Integer.MIN_VALUE
     *       Max: Integer.MAX_VALUE
     *   Default: 0
     *  Identity: 0
     * </pre>
     *
     * @param yoff the offset in the y direction, in pixels
     */
    public void setOffsetY(int yoff) {
        int old = invert.getOffsetY();
        invert.setOffsetY(yoff);
    }

    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        return getDefaultedInput(1, defaultInput).transform(p, defaultInput);
    }

    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        return getDefaultedInput(1, defaultInput).untransform(p, defaultInput);
    }
}

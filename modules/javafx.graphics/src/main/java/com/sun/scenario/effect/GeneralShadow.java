/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A blurred shadow effect using either a Gaussian convolution or a Box filter
 * kernel, with a configurable radius and shadow color.  Only the alpha channel
 * of the input is used to create the shadow effect.  The alpha value of each
 * pixel from the result of the blur operation is modulated with the
 * specified shadow color to produce the resulting image.
 */
public class GeneralShadow extends DelegateEffect {

    private AbstractShadow shadow;

    /**
     * Constructs a new {@code GeneralShadow} effect, with the default
     * blur radius (10.0), using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new GeneralShadow(DefaultInput)
     * </pre>
     */
    public GeneralShadow() {
        this(DefaultInput);
    }

    /**
     * Constructs a new {@code GeneralShadow} effect, with the default
     * blur radius (10.0).
     * <p>
     * This constructor is intended for advanced developers only.  Most
     * developers will only ever need to use the default constructor.
     *
     * @param input the input {@code Effect} used to create the shadow
     */
    public GeneralShadow(Effect input) {
        super(input);
        this.shadow = new GaussianShadow(10f, Color4f.BLACK, input);
    }

    public AbstractShadow.ShadowMode getShadowMode() {
        return shadow.getMode();
    }

    public void setShadowMode(AbstractShadow.ShadowMode mode) {
        AbstractShadow.ShadowMode old = shadow.getMode();
        this.shadow = shadow.implFor(mode);
    }

    protected Effect getDelegate() {
        return shadow;
    }

    /**
     * Returns the shadow source input for this {@code Effect}.
     *
     * @return the shadow source input for this {@code Effect}
     */
    public final Effect getInput() {
        return shadow.getInput();
    }

    /**
     * Sets the shadow source input for this {@code Effect}.
     *
     * @param input the shadow source input for this {@code Effect}
     * @throws IllegalArgumentException if {@code input} is null
     */
    public void setInput(Effect input) {
        shadow.setInput(input);
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
        shadow.setGaussianWidth(w);
    }

    public void setGaussianHeight(float h) {
        float old = shadow.getGaussianHeight();
        shadow.setGaussianHeight(h);
    }

    /**
     * Gets the spread of the shadow effect.
     *
     * @return the spread of the shadow effect
     */
    public float getSpread() {
        return shadow.getSpread();
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
        float old = shadow.getSpread();
        shadow.setSpread(spread);
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
}

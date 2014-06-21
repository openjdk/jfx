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

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

/**
 * An effect that allows for per-pixel adjustments of hue, saturation,
 * brightness, and contrast.
 */
public class ColorAdjust extends CoreEffect<RenderState> {

    private float hue;
    private float saturation;
    private float brightness;
    private float contrast;

    /**
     * Constructs a new {@code ColorAdjust} effect with the default hue (0.0),
     * saturation (0.0), brightness (0.0), and contrast (1.0),
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new ColorAdjust(DefaultInput)
     * </pre>
     */
    public ColorAdjust() {
        this(DefaultInput);
    }

    /**
     * Constructs a new {@code ColorAdjust} effect with the default hue (0.0),
     * saturation (0.0), brightness (0.0), and contrast (0.0).
     *
     * @param input the single input {@code Effect}
     */
    public ColorAdjust(Effect input) {
        super(input);
        this.hue = 0f;
        this.saturation = 0f;
        this.brightness = 0f;
        this.contrast = 0f;
        updatePeerKey("ColorAdjust");
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
     * @throws IllegalArgumentException if {@code input} is null
     */
    public void setInput(Effect input) {
        setInput(0, input);
    }

    /**
     * Returns the hue adjustment.
     *
     * @return the hue adjustment
     */
    public float getHue() {
        return hue;
    }

    /**
     * Sets the hue adjustment.
     * <pre>
     *       Min: -1.0
     *       Max: +1.0
     *   Default:  0.0
     *  Identity:  0.0
     * </pre>
     *
     * @param hue the hue adjustment
     * @throws IllegalArgumentException if {@code hue} is outside the
     * allowable range
     */
    public void setHue(float hue) {
        if (hue < -1f || hue > 1f) {
            throw new IllegalArgumentException("Hue must be in the range [-1, 1]");
        }
        float old = this.hue;
        this.hue = hue;
    }

    /**
     * Returns the saturation adjustment.
     *
     * @return the saturation adjustment
     */
    public float getSaturation() {
        return saturation;
    }

    /**
     * Sets the saturation adjustment.
     * <pre>
     *       Min: -1.0
     *       Max: +1.0
     *   Default:  0.0
     *  Identity:  0.0
     * </pre>
     *
     * @param saturation the saturation adjustment
     * @throws IllegalArgumentException if {@code saturation} is outside the
     * allowable range
     */
    public void setSaturation(float saturation) {
        if (saturation < -1f || saturation > 1f) {
            throw new IllegalArgumentException("Saturation must be in the range [-1, 1]");
        }
        float old = this.saturation;
        this.saturation = saturation;
    }

    /**
     * Returns the brightness adjustment.
     *
     * @return the brightness adjustment
     */
    public float getBrightness() {
        return brightness;
    }

    /**
     * Sets the brightness adjustment.
     * <pre>
     *       Min: -1.0
     *       Max: +1.0
     *   Default:  0.0
     *  Identity:  0.0
     * </pre>
     *
     * @param brightness the brightness adjustment
     * @throws IllegalArgumentException if {@code brightness} is outside the
     * allowable range
     */
    public void setBrightness(float brightness) {
        if (brightness < -1f || brightness > 1f) {
            throw new IllegalArgumentException("Brightness must be in the range [-1, 1]");
        }
        float old = this.brightness;
        this.brightness = brightness;
    }

    /**
     * Returns the contrast adjustment.
     *
     * @return the contrast adjustment
     */
    public float getContrast() {
        return contrast;
    }

    /**
     * Sets the contrast adjustment.
     * <pre>
     *       Min: -1.0
     *       Max: +1.0
     *   Default:  0.0
     *  Identity:  0.0
     * </pre>
     *
     * @param contrast the contrast adjustment
     * @throws IllegalArgumentException if {@code contrast} is outside the
     * allowable range
     */
    public void setContrast(float contrast) {
        if (contrast < -1f || contrast > 1f) {
            throw new IllegalArgumentException("Contrast must be in the range [-1, 1]");
        }
        float old = this.contrast;
        this.contrast = contrast;
    }

    @Override
    public RenderState getRenderState(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      Object renderHelper,
                                      Effect defaultInput)
    {
        return RenderState.RenderSpaceRenderState;
    }

    @Override
    public boolean reducesOpaquePixels() {
        final Effect input = getInput();
        return input != null && input.reducesOpaquePixels();
    }
}

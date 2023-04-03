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

/**
 * A high-level effect that makes the input image appear to glow,
 * based on a configurable threshold.
 */
public class Glow extends DelegateEffect {

    private final GaussianBlur blur;
    private final Blend blend;

    /**
     * Constructs a new {@code Glow} effect with the default level (0.3),
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new Glow(DefaultInput)
     * </pre>
     */
    public Glow() {
        this(DefaultInput);
    }

    /**
     * Constructs a new {@code Glow} effect with the default level (0.3).
     *
     * @param input the single input {@code Effect}
     */
    public Glow(Effect input) {
        super(input);

        //
        //    (input)
        //       |
        //  GaussianBlur
        //       |
        //       Crop   (input)
        //          |   |
        //          Blend
        //            |
        //
        this.blur = new GaussianBlur(10f, input);
        // crop to prevent blur fringes from being included in final result
        Crop crop = new Crop(blur, input);
        this.blend = new Blend(Blend.Mode.ADD, input, crop);
        this.blend.setOpacity(0.3f);
    }

    @Override
    protected Effect getDelegate() {
        return blend;
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
        blur.setInput(input);
        blend.setBottomInput(input);
    }

    /**
     * Returns the level value, which controls the intensity of the glow effect.
     *
     * @return the level value
     */
    public float getLevel() {
        return blend.getOpacity();
    }

    /**
     * Sets the level value, which controls the intensity of the glow effect.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.3
     *  Identity: 0.0
     * </pre>
     *
     * @param level the level value
     * @throws IllegalArgumentException if {@code level} is outside
     * the allowable range
     */
    public void setLevel(float level) {
        float old = blend.getOpacity();
        blend.setOpacity(level);
    }

    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        return getDefaultedInput(0, defaultInput).transform(p, defaultInput);
    }

    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        return getDefaultedInput(0, defaultInput).untransform(p, defaultInput);
    }
}

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
 * A filter that produces a sepia tone effect, similar to antique photographs.
 */
public class SepiaTone extends CoreEffect<RenderState> {

    private float level;

    /**
     * Constructs a new {@code SepiaTone} effect with the default
     * level value (1.0), using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new SepiaTone(DefaultInput)
     * </pre>
     */
    public SepiaTone() {
        this(DefaultInput);
    }

    /**
     * Constructs a new {@code SepiaTone} effect with the default
     * level value (1.0).
     *
     * @param input the single input {@code Effect}
     */
    public SepiaTone(Effect input) {
        super(input);
        setLevel(1f);
        updatePeerKey("SepiaTone");
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
     * Returns the level value, which controls the intensity of the
     * sepia effect.
     *
     * @return the level value
     */
    public float getLevel() {
        return level;
    }

    /**
     * Sets the level value, which controls the intensity of the sepia effect.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 1.0
     *  Identity: 0.0
     * </pre>
     *
     * @param level the level value
     * @throws IllegalArgumentException if {@code level} is outside
     * the allowable range
     */
    public void setLevel(float level) {
        if (level < 0f || level > 1f) {
            throw new IllegalArgumentException("Level must be in the range [0,1]");
        }
        float old = this.level;
        this.level = level;
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

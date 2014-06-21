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

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

/**
 * An effect that merges two inputs together into one result.  This produces
 * the same result as using the {@code Blend} effect with
 * {@code Blend.Mode.SRC_OVER} and {@code opacity=1.0}, except possibly
 * more efficient.
 */
public class Merge extends CoreEffect<RenderState> {

    /**
     * Constructs a new {@code Merge} effect for the given inputs.
     *
     * @param bottomInput the bottom input
     * @param topInput the top input
     */
    public Merge(Effect bottomInput, Effect topInput) {
        super(bottomInput, topInput);
        updatePeerKey("Merge");
    }

    /**
     * Returns the bottom input for this {@code Effect}.
     *
     * @return the bottom input for this {@code Effect}
     */
    public final Effect getBottomInput() {
        return getInputs().get(0);
    }

    /**
     * Sets the bottom input for this {@code Effect} to a specific
     * {@code Effect} or to the default input if {@code input} is
     * {@code null}.
     *
     * @param bottomInput the bottom input for this {@code Effect}
     */
    public void setBottomInput(Effect bottomInput) {
        setInput(0, bottomInput);
    }

    /**
     * Returns the top input for this {@code Effect}.
     *
     * @return the top input for this {@code Effect}
     */
    public final Effect getTopInput() {
        return getInputs().get(1);
    }

    /**
     * Sets the top input for this {@code Effect} to a specific
     * {@code Effect} or to the default input if {@code input} is
     * {@code null}.
     *
     * @param topInput the top input for this {@code Effect}
     */
    public void setTopInput(Effect topInput) {
        setInput(1, topInput);
    }

    /**
     * Transform the specified point {@code p} from the coordinate space
     * of the primary content input to the coordinate space of the effect
     * output.
     * In essence, this method asks the question "Which output coordinate
     * is most affected by the data at the specified coordinate in the
     * primary source input?"
     * <p>
     * The {@code Merge} effect delegates this operation to its {@code top}
     * input, or the {@code defaultInput} if the {@code top} input is
     * {@code null}.
     *
     * @param p the point in the coordinate space of the primary content
     *          input to be transformed
     * @param defaultInput the default input {@code Effect} to be used in
     *                     all cases where a filter has a null input
     * @return the transformed point in the coordinate space of the result
     */
    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        return getDefaultedInput(1, defaultInput).transform(p, defaultInput);
    }

    /**
     * Transform the specified point {@code p} from the coordinate space
     * of the output of the effect into the coordinate space of the
     * primary content input.
     * In essence, this method asks the question "Which source coordinate
     * contributes most to the definition of the output at the specified
     * coordinate?"
     * <p>
     * The {@code Merge} effect delegates this operation to its {@code top}
     * input, or the {@code defaultInput} if the {@code top} input is
     * {@code null}.
     *
     * @param p the point in the coordinate space of the result output
     *          to be transformed
     * @param defaultInput the default input {@code Effect} to be used in
     *                     all cases where a filter has a null input
     * @return the untransformed point in the coordinate space of the
     *         primary content input
     */
    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        return getDefaultedInput(1, defaultInput).untransform(p, defaultInput);
    }

    @Override
    public ImageData filter(FilterContext fctx,
                            BaseTransform transform,
                            Rectangle outputClip,
                            Object renderHelper,
                            Effect defaultInput)
    {
        Effect botinput = getDefaultedInput(0, defaultInput);
        Effect topinput = getDefaultedInput(1, defaultInput);
        ImageData botimg = botinput.filter(fctx, transform, outputClip,
                                           renderHelper, defaultInput);
        if (botimg != null) {
            if (!botimg.validate(fctx)) {
                return new ImageData(fctx, null, null);
            }
            if (renderHelper instanceof ImageDataRenderer) {
                ImageDataRenderer imgr = (ImageDataRenderer) renderHelper;
                imgr.renderImage(botimg, BaseTransform.IDENTITY_TRANSFORM, fctx);
                botimg.unref();
                botimg = null;
            }
        }
        if (botimg == null) {
            return topinput.filter(fctx, transform, outputClip,
                                   renderHelper, defaultInput);
        }
        ImageData topimg = topinput.filter(fctx, transform, outputClip,
                                           null, defaultInput);
        if (!topimg.validate(fctx)) {
            return new ImageData(fctx, null, null);
        }
        RenderState rstate = getRenderState(fctx, transform, outputClip,
                                            renderHelper, defaultInput);
        ImageData ret = filterImageDatas(fctx, transform, outputClip, rstate,
                                         botimg, topimg);
        botimg.unref();
        topimg.unref();
        return ret;
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
        final Effect topInput = getTopInput();
        final Effect bottomInput = getBottomInput();
        return topInput != null && topInput.reducesOpaquePixels() && bottomInput != null && bottomInput.reducesOpaquePixels();
    }

}

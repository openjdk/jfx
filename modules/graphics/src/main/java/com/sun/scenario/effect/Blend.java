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
 * An effect that blends the two inputs together using one of the
 * pre-defined {@code Mode}s.
 */
public class Blend extends CoreEffect<RenderState> {

    /**
     * A blending mode that defines the manner in which the inputs
     * are composited together.
     * Each {@code Mode} describes a mathematical equation that
     * combines premultiplied inputs to produce some premultiplied result.
     */
    public enum Mode {
        /**
         * The top input is blended over the bottom input.
         * (Equivalent to the Porter-Duff "source over destination" rule.)
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>C<sub>r</sub></em> = <em>C<sub>top</sub></em> + <em>C<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         * </pre>
         */
        SRC_OVER,

        /**
         * The part of the top input lying inside of the bottom input
         * is kept in the resulting image.
         * (Equivalent to the Porter-Duff "source in destination" rule.)
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em>*<em>A<sub>bot</sub></em>
         *      <em>C<sub>r</sub></em> = <em>C<sub>top</sub></em>*<em>A<sub>bot</sub></em>
         * </pre>
         */
        SRC_IN,

        /**
         * The part of the top input lying outside of the bottom input
         * is kept in the resulting image.
         * (Equivalent to the Porter-Duff "source held out by destination"
         * rule.)
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em>*(1-<em>A<sub>bot</sub></em>)
         *      <em>C<sub>r</sub></em> = <em>C<sub>top</sub></em>*(1-<em>A<sub>bot</sub></em>)
         * </pre>
         */
        SRC_OUT,

        /**
         * The part of the top input lying inside of the bottom input
         * is blended with the bottom input.
         * (Equivalent to the Porter-Duff "source atop destination" rule.)
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em>*<em>A<sub>bot</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>) = <em>A<sub>bot</sub></em>
         *      <em>C<sub>r</sub></em> = <em>C<sub>top</sub></em>*<em>A<sub>bot</sub></em> + <em>C<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         * </pre>
         */
        SRC_ATOP,

        /**
         * The color and alpha components from the top input are
         * added to those from the bottom input.
         * The result is clamped to 1.0 if it exceeds the logical
         * maximum of 1.0.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = min(1, <em>A<sub>top</sub></em>+<em>A<sub>bot</sub></em>)
         *      <em>C<sub>r</sub></em> = min(1, <em>C<sub>top</sub></em>+<em>C<sub>bot</sub></em>)
         * </pre>
         * <p>
         * Notes:
         * <ul>
         * <li>This mode is commutative (ordering of inputs
         * does not matter).
         * <li>This mode is sometimes referred to as "linear dodge" in
         * imaging software packages.
         * </ul>
         */
        ADD,

        /**
         * The color components from the first input are multiplied with those
         * from the second input.
         * The alpha components are blended according to
         * the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>C<sub>r</sub></em> = <em>C<sub>top</sub></em> * <em>C<sub>bot</sub></em>
         * </pre>
         * <p>
         * Notes:
         * <ul>
         * <li>This mode is commutative (ordering of inputs
         * does not matter).
         * <li>This mode is the mathematical opposite of
         * the {@link #SCREEN} mode.
         * <li>The resulting color is always at least as dark as either
         * of the input colors.
         * <li>Rendering with a completely black top input produces black;
         * rendering with a completely white top input produces a result
         * equivalent to the bottom input.
         * </ul>
         */
        MULTIPLY,

        /**
         * The color components from both of the inputs are
         * inverted, multiplied with each other, and that result
         * is again inverted to produce the resulting color.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>C<sub>r</sub></em> = 1 - ((1-<em>C<sub>top</sub></em>) * (1-<em>C<sub>bot</sub></em>))
         * </pre>
         * <p>
         * Notes:
         * <ul>
         * <li>This mode is commutative (ordering of inputs
         * does not matter).
         * <li>This mode is the mathematical opposite of
         * the {@link #MULTIPLY} mode.
         * <li>The resulting color is always at least as light as either
         * of the input colors.
         * <li>Rendering with a completely white top input produces white;
         * rendering with a completely black top input produces a result
         * equivalent to the bottom input.
         * </ul>
         */
        SCREEN,

        /**
         * The input color components are either multiplied or screened,
         * depending on the bottom input color.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      REMIND: not sure how to express this succinctly yet...
         * </pre>
         * <p>
         * Notes:
         * <ul>
         * <li>This mode is a combination of {@link #SCREEN} and
         * {@link #MULTIPLY}, depending on the bottom input color.
         * <li>This mode is the mathematical opposite of
         * the {@link #HARD_LIGHT} mode.
         * <li>In this mode, the top input colors "overlay" the bottom input
         * while preserving highlights and shadows of the latter.
         * </ul>
         */
        OVERLAY,

        /**
         * REMIND: cross check this formula with OpenVG spec...
         *
         * The darker of the color components from the two inputs are
         * selected to produce the resulting color.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>C<sub>r</sub></em> = min(<em>C<sub>top</sub></em>, <em>C<sub>bot</sub></em>)
         * </pre>
         * <p>
         * Notes:
         * <ul>
         * <li>This mode is commutative (ordering of inputs
         * does not matter).
         * <li>This mode is the mathematical opposite of
         * the {@link #LIGHTEN} mode.
         * </ul>
         */
        DARKEN,

        /**
         * REMIND: cross check this formula with OpenVG spec...
         *
         * The lighter of the color components from the two inputs are
         * selected to produce the resulting color.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>C<sub>r</sub></em> = max(<em>C<sub>top</sub></em>, <em>C<sub>bot</sub></em>)
         * </pre>
         * <p>
         * Notes:
         * <ul>
         * <li>This mode is commutative (ordering of inputs
         * does not matter).
         * <li>This mode is the mathematical opposite of
         * the {@link #DARKEN} mode.
         * </ul>
         */
        LIGHTEN,

        /**
         * The bottom input color components are divided by the inverse
         * of the top input color components to produce the resulting color.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>C<sub>r</sub></em> = <em>C<sub>bot</sub></em> / (1-<em>C<sub>top</sub></em>)
         * </pre>
         */
        COLOR_DODGE,

        /**
         * The inverse of the bottom input color components are divided by
         * the top input color components, all of which is then inverted
         * to produce the resulting color.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>C<sub>r</sub></em> = 1-((1-<em>C<sub>bot</sub></em>) / <em>C<sub>top</sub></em>)
         * </pre>
         */
        COLOR_BURN,

        /**
         * The input color components are either multiplied or screened,
         * depending on the top input color.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      REMIND: not sure how to express this succinctly yet...
         * </pre>
         * <p>
         * Notes:
         * <ul>
         * <li>This mode is a combination of {@link #SCREEN} and
         * {@link #MULTIPLY}, depending on the top input color.
         * <li>This mode is the mathematical opposite of
         * the {@link #OVERLAY} mode.
         * </ul>
         */
        HARD_LIGHT,

        /**
         * REMIND: this is a complicated formula, TBD...
         */
        SOFT_LIGHT,

        /**
         * The darker of the color components from the two inputs are
         * subtracted from the lighter ones to produce the resulting color.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>C<sub>r</sub></em> = abs(<em>C<sub>top</sub></em>-<em>C<sub>bot</sub></em>)
         * </pre>
         * <p>
         * Notes:
         * <ul>
         * <li>This mode is commutative (ordering of inputs
         * does not matter).
         * <li>This mode can be used to invert parts of the bottom input
         * image, or to quickly compare two images (equal pixels will result
         * in black).
         * <li>Rendering with a completely white top input inverts the
         * bottom input; rendering with a completely black top input produces
         * a result equivalent to the bottom input.
         * </ul>
         */
        DIFFERENCE,

        /**
         * The color components from the two inputs are multiplied and
         * doubled, and then subtracted from the sum of the bottom input
         * color components, to produce the resulting color.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>C<sub>r</sub></em> = <em>C<sub>top</sub></em> + <em>C<sub>bot</sub></em> - (2*<em>C<sub>top</sub></em>*<em>C<sub>bot</sub></em>)
         * </pre>
         * <p>
         * Notes:
         * <ul>
         * <li>This mode is commutative (ordering of inputs
         * does not matter).
         * <li>This mode can be used to invert parts of the bottom input.
         * <li>This mode produces results that are similar to those of
         * {@link #DIFFERENCE}, except with lower contrast.
         * <li>Rendering with a completely white top input inverts the
         * bottom input; rendering with a completely black top input produces
         * a result equivalent to the bottom input.
         * </ul>
         */
        EXCLUSION,

        /**
         * The red component of the bottom input is replaced with the
         * red component of the top input; the other color components
         * are unaffected.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>R<sub>r</sub></em> = <em>R<sub>top</sub></em>
         *      <em>G<sub>r</sub></em> = <em>G<sub>bot</sub></em>
         *      <em>B<sub>r</sub></em> = <em>B<sub>bot</sub></em>
         * </pre>
         */
        RED,

        /**
         * The green component of the bottom input is replaced with the
         * green component of the top input; the other color components
         * are unaffected.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>R<sub>r</sub></em> = <em>R<sub>bot</sub></em>
         *      <em>G<sub>r</sub></em> = <em>G<sub>top</sub></em>
         *      <em>B<sub>r</sub></em> = <em>B<sub>bot</sub></em>
         * </pre>
         */
        GREEN,

        /**
         * The blue component of the bottom input is replaced with the
         * blue component of the top input; the other color components
         * are unaffected.
         * The alpha components are blended according
         * to the {@link #SRC_OVER} equation.
         * <p>
         * Thus:
         * <pre>
         *      <em>A<sub>r</sub></em> = <em>A<sub>top</sub></em> + <em>A<sub>bot</sub></em>*(1-<em>A<sub>top</sub></em>)
         *      <em>R<sub>r</sub></em> = <em>R<sub>bot</sub></em>
         *      <em>G<sub>r</sub></em> = <em>G<sub>bot</sub></em>
         *      <em>B<sub>r</sub></em> = <em>B<sub>top</sub></em>
         * </pre>
         */
        BLUE,
    }

    private Mode mode;
    private float opacity;

    /**
     * Constructs a new {@code Blend} effect with the given mode and the
     * default opacity (1.0).
     * Either or both inputs may be {@code null} to indicate that the default
     * input should be used.
     *
     * @param mode the blending mode
     * @param bottomInput the bottom input
     * @param topInput the top input
     * @throws IllegalArgumentException if {@code mode} is null
     */
    public Blend(Mode mode, Effect bottomInput, Effect topInput) {
        super(bottomInput, topInput);
        setMode(mode);
        setOpacity(1f);
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
     * Returns the {@code Mode} used to blend the two inputs together.
     *
     * @return the {@code Mode} used to blend the two inputs together.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Sets the {@code Mode} used to blend the two inputs together.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: Mode.SRC_OVER
     *  Identity: n/a
     * </pre>
     *
     * @param mode the blending mode
     * @throws IllegalArgumentException if {@code mode} is null
     */
    public void setMode(Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode must be non-null");
        }
        Blend.Mode old = this.mode;
        this.mode = mode;
        updatePeerKey("Blend_" + mode.name());
    }

    /**
     * Returns the opacity value, which is modulated with the top input
     * prior to blending.
     *
     * @return the opacity value
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * Sets the opacity value, which is modulated with the top input prior
     * to blending.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 1.0
     *  Identity: 1.0
     * </pre>
     *
     * @param opacity the opacity value
     * @throws IllegalArgumentException if {@code opacity} is outside the
     * allowable range
     */
    public void setOpacity(float opacity) {
        if (opacity < 0f || opacity > 1f) {
            throw new IllegalArgumentException("Opacity must be in the range [0,1]");
        }
        float old = this.opacity;
        this.opacity = opacity;
    }

    /**
     * Transform the specified point {@code p} from the coordinate space
     * of the primary content input to the coordinate space of the effect
     * output.
     * In essence, this method asks the question "Which output coordinate
     * is most affected by the data at the specified coordinate in the
     * primary source input?"
     * <p>
     * The {@code Blend} effect delegates this operation to its {@code top}
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
     * The {@code Blend} effect delegates this operation to its {@code top}
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
    public RenderState getRenderState(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      Object renderHelper,
                                      Effect defaultInput)
    {
        // A blend operation operates on its inputs pixel-by-pixel
        // with no expansion or contraction.
        // RT-27563
        // TODO: The RenderSpaceRenderState object uses the output clip unchanged
        // for its inputs, but we could further restrict the amount we ask for
        // each input to the intersection of the two input bounds, but for now we
        // will simply let it pass along the output clip as the input clip.
        return RenderState.RenderSpaceRenderState;
    }

    @Override
    public boolean reducesOpaquePixels() {
        final Effect bottomInput = getBottomInput();
        final Effect topInput = getTopInput();
        switch (getMode()) {
            case SRC_IN:
            case SRC_OUT:
                return true;
            case SRC_ATOP:
                return bottomInput != null && bottomInput.reducesOpaquePixels();
            case SRC_OVER:
            case ADD:
            case MULTIPLY:
            case SCREEN:
            case OVERLAY:
            case DARKEN:
            case LIGHTEN:
            case COLOR_DODGE:
            case COLOR_BURN:
            case HARD_LIGHT:
            case SOFT_LIGHT:
            case DIFFERENCE:
            case EXCLUSION:
            case RED:
            case GREEN:
            case BLUE:
                return topInput != null && topInput.reducesOpaquePixels() && bottomInput != null && bottomInput.reducesOpaquePixels();
        }
        return true;
    }
}

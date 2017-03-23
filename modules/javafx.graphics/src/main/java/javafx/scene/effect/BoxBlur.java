/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.effect;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

import com.sun.javafx.util.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;


/**
 * A blur effect using a simple box filter kernel, with separately
 * configurable sizes in both dimensions, and an iteration parameter
 * that controls the quality of the resulting blur.
 *
 * <p>
 * Example:
 * <pre>{@code
 * BoxBlur boxBlur = new BoxBlur();
 * boxBlur.setWidth(10);
 * boxBlur.setHeight(3);
 * boxBlur.setIterations(3);
 *
 * Text text = new Text();
 * text.setText("Blurry Text!");
 * text.setFill(Color.web("0x3b596d"));
 * text.setFont(Font.font(null, FontWeight.BOLD, 50));
 * text.setX(10);
 * text.setY(50);
 * text.setEffect(boxBlur);
 * }</pre>
 * <p>
 * The code above produces the following:
 * </p>
 * <p>
 * <img src="doc-files/boxblur.png" alt="The visual effect of BoxBlur on text">
 * </p>
 * @since JavaFX 2.0
 */
public class BoxBlur extends Effect {

    /**
     * Creates a new instance of BoxBlur with default parameters.
     */
    public BoxBlur() {}

    /**
     * Creates a new instance of BoxBlur with specified width, height and
     * iterations.
     * @param width the horizontal dimension of the blur effect
     * @param height the vertical dimension of the blur effect
     * @param iterations the number of times to iterate the blur effect to
     * improve its "quality" or "smoothness"
     */
    public BoxBlur(double width, double height, int iterations) {
        setWidth(width);
        setHeight(height);
        setIterations(iterations);
    }

    @Override
    com.sun.scenario.effect.BoxBlur createPeer() {
        return new com.sun.scenario.effect.BoxBlur();
    };
    /**
     * The input for this {@code Effect}.
     * If set to {@code null}, or left unspecified, a graphical image of
     * the {@code Node} to which the {@code Effect} is attached will be
     * used as the input.
     * @defaultValue null
     */
    private ObjectProperty<Effect> input;


    public final void setInput(Effect value) {
        inputProperty().set(value);
    }

    public final Effect getInput() {
        return input == null ? null : input.get();
    }

    public final ObjectProperty<Effect> inputProperty() {
        if (input == null) {
            input = new EffectInputProperty("input");
        }
        return input;
    }

    @Override
    boolean checkChainContains(Effect e) {
        Effect localInput = getInput();
        if (localInput == null)
            return false;
        if (localInput == e)
            return true;
        return localInput.checkChainContains(e);
    }

    /**
     * The horizontal dimension of the blur effect.
     * The color information for a given pixel will be spread across
     * a Box of the indicated width centered over the pixel.
     * Values less than or equal to 1 will not spread the color data
     * beyond the pixel where it originated from and so will have
     * no effect.
     * <pre>
     *       Min:   0.0
     *       Max: 255.0
     *   Default:   5.0
     *  Identity:  &lt;1.0
     * </pre>
     * @defaultValue 5.0
     */
    private DoubleProperty width;


    public final void setWidth(double value) {
        widthProperty().set(value);
    }

    public final double getWidth() {
        return width == null ? 5 : width.get();
    }

    public final DoubleProperty widthProperty() {
        if (width == null) {
            width = new DoublePropertyBase(5) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return BoxBlur.this;
                }

                @Override
                public String getName() {
                    return "width";
                }
            };
        }
        return width;
    }

    /**
     * The vertical dimension of the blur effect.
     * The color information for a given pixel will be spread across
     * a Box of the indicated height centered over the pixel.
     * Values less than or equal to 1 will not spread the color data
     * beyond the pixel where it originated from and so will have
     * no effect.
     * <pre>
     *       Min:   0.0
     *       Max: 255.0
     *   Default:   5.0
     *  Identity:  &lt;1.0
     * </pre>
     * @defaultValue 5.0
     */
    private DoubleProperty height;


    public final void setHeight(double value) {
        heightProperty().set(value);
    }

    public final double getHeight() {
        return height == null ? 5 : height.get();
    }

    public final DoubleProperty heightProperty() {
        if (height == null) {
            height = new DoublePropertyBase(5) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return BoxBlur.this;
                }

                @Override
                public String getName() {
                    return "height";
                }
            };
        }
        return height;
    }

    /**
     * The number of times to iterate the blur effect to improve its
     * "quality" or "smoothness".
     * Iterating the effect 3 times approximates the quality of a
     * Gaussian Blur to within 3%.
     * <pre>
     *       Min:   0
     *       Max:   3
     *   Default:   1
     *  Identity:   0
     * </pre>
     * @defaultValue 1
     */
    private IntegerProperty iterations;


    public final void setIterations(int value) {
        iterationsProperty().set(value);
    }

    public final int getIterations() {
        return iterations == null ? 1 : iterations.get();
    }

    public final IntegerProperty iterationsProperty() {
        if (iterations == null) {
            iterations = new IntegerPropertyBase(1) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return BoxBlur.this;
                }

                @Override
                public String getName() {
                    return "iterations";
                }
            };
        }
        return iterations;
    }

    private int getClampedWidth() {
        return Utils.clamp(0, (int) getWidth(), 255);
    }

    private int getClampedHeight() {
        return Utils.clamp(0, (int) getHeight(), 255);
    }

    private int getClampedIterations() {
        return Utils.clamp(0, getIterations(), 3);
    }

    @Override
    void update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.sync();
        }

        com.sun.scenario.effect.BoxBlur peer =
                (com.sun.scenario.effect.BoxBlur) getPeer();
        peer.setInput(localInput == null ? null : localInput.getPeer());
        peer.setHorizontalSize(getClampedWidth());
        peer.setVerticalSize(getClampedHeight());
        peer.setPasses(getClampedIterations());
    }

    @Override
    BaseBounds getBounds(BaseBounds bounds,
                         BaseTransform tx,
                         Node node,
                         BoundsAccessor boundsAccessor) {
        bounds = getInputBounds(bounds,
                                BaseTransform.IDENTITY_TRANSFORM,
                                node, boundsAccessor,
                                getInput());

        int localIterations = getClampedIterations();

        int hgrow = getKernelSize(getClampedWidth(), localIterations);
        int vgrow = getKernelSize(getClampedHeight(), localIterations);

        bounds = bounds.deriveWithPadding(hgrow, vgrow, 0);

        return transformBounds(tx, bounds);
    }

    @Override
    Effect copy() {
        BoxBlur bb = new BoxBlur(this.getWidth(), this.getHeight(), this.getIterations());
        bb.setInput(this.getInput());
        return bb;
    }
}

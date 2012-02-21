/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.effect.EffectUtils;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;


/**
 * A blur effect using a simple box filter kernel, with separately
 * configurable sizes in both dimensions, and an iteration parameter
 * that contols the quality of the resulting blur.
 *
<PRE>
import javafx.scene.*;
import javafx.scene.text.*;
import javafx.scene.paint.*;
import javafx.scene.effect.*;

Text t = new Text();
t.setText("Blurry Text!");
t.setFill(Color.RED);
t.setFont(Font.font(null, FontWeight.BOLD, 36));
t.setX(10);
t.setY(40);

BoxBlur bb = new BoxBlur();
bb.setWidth(15);
bb.setHeight(15);
bb.setIterations(3);

t.setEffect(bb);

</PRE>
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
    com.sun.scenario.effect.BoxBlur impl_createImpl() {
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
    boolean impl_checkChainContains(Effect e) {
        Effect localInput = getInput();
        if (localInput == null)
            return false;
        if (localInput == e)
            return true;
        return localInput.impl_checkChainContains(e);
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
    void impl_update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.impl_sync();
        }

        com.sun.scenario.effect.BoxBlur peer =
                (com.sun.scenario.effect.BoxBlur) impl_getImpl();
        peer.setInput(localInput == null ? null : localInput.impl_getImpl());
        peer.setHorizontalSize(getClampedWidth());
        peer.setVerticalSize(getClampedHeight());
        peer.setPasses(getClampedIterations());
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_getBounds(BaseBounds bounds,
                                     BaseTransform tx,
                                     Node node,
                                     BoundsAccessor boundsAccessor) {
        bounds = EffectUtils.getInputBounds(bounds,
                                            BaseTransform.IDENTITY_TRANSFORM,
                                            node, boundsAccessor,
                                            getInput());

        int localIterations = getClampedIterations();

        int hgrow = EffectUtils.getKernelSize(getClampedWidth(), localIterations);
        int vgrow = EffectUtils.getKernelSize(getClampedHeight(), localIterations);

        bounds = bounds.deriveWithPadding(hgrow, vgrow, 0);

        return EffectUtils.transformBounds(tx, bounds);
    }
}

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
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

import com.sun.javafx.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.effect.EffectUtils;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;


/**
 * A blur effect using a Gaussian convolution kernel, with a configurable
 * radius.
 *
<PRE>
import javafx.scene.*;
import javafx.scene.text.*;
import javafx.scene.paint.*;
import javafx.scene.effect.*;

Text t = new Text();
t.setX(10.0);
t.setY(40.0);
t.setCache(true);
t.setText("Blurry Text");
t.setFill(Color.RED);
t.setFont(Font.font(null, FontWeight.BOLD, 36));

t.setEffect(new GaussianBlur());
</PRE>
 *
 * @profile common conditional effect
 */
public class GaussianBlur extends Effect {
    /**
     * Creates a new instance of GaussianBlur with default parameters.
     */
    public GaussianBlur() {}

    /**
     * Creates a new instance of GaussianBlur with the specified radius.
     * @param radius the radius of the blur kernel
     */
    public GaussianBlur(double radius) {
        setRadius(radius);
    }

    @Override
    com.sun.scenario.effect.GaussianBlur impl_createImpl() {
        return new com.sun.scenario.effect.GaussianBlur();
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
     * The radius of the blur kernel.
     * <pre>
     *       Min:  0.0
     *       Max: 63.0
     *   Default: 10.0
     *  Identity:  0.0
     * </pre>
     * @defaultValue 10.0
     */
    private DoubleProperty radius;


    public final void setRadius(double value) {
        radiusProperty().set(value);
    }

    public final double getRadius() {
        return radius == null ? 10 : radius.get();
    }

    public final DoubleProperty radiusProperty() {
        if (radius == null) {
            radius = new DoublePropertyBase(10) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return GaussianBlur.this;
                }

                @Override
                public String getName() {
                    return "radius";
                }
            };
        }
        return radius;
    }

    private float getClampedRadius() {
        return (float) Utils.clamp(0, getRadius(), 63);
    }

    @Override
    void impl_update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.impl_sync();
        }

        com.sun.scenario.effect.GaussianBlur peer =
                (com.sun.scenario.effect.GaussianBlur) impl_getImpl();
        peer.setRadius(getClampedRadius());
        peer.setInput(localInput == null ? null : localInput.impl_getImpl());
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
        float r = getClampedRadius();
        bounds = bounds.deriveWithPadding(r, r, 0);
        return EffectUtils.transformBounds(tx, bounds);
    }
}

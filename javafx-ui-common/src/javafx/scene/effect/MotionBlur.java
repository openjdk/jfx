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
 * A motion blur effect using a Gaussian convolution kernel, with a
 * configurable radius and angle.
 *
<PRE>
import javafx.scene.*;
import javafx.scene.text.*;
import javafx.scene.paint.*;
import javafx.scene.effect.*;

Text t = new Text();
t.setX(20.0);
t.setY(100.0);
t.setText("Motion");
t.setFill(Color.RED);
t.setFont(Font.font(null, FontWeight.BOLD, 60));

MotionBlur mb = new MotionBlur();
mb.setRadius(15.0);
mb.setAngle(-30.0);

t.setEffect(mb);
</PRE>
 *
 * @profile common conditional effect
 */
public class MotionBlur extends Effect {
    /**
     * Creates a new instance of MotionBlur with default parameters.
     */
    public MotionBlur() {}

    /**
     * Creates a new instance of MotionBlur with the specified angle and radius.
     * @param angle the angle of the motion effect, in degrees
     * @param radius the radius of the blur kernel
     */
    public MotionBlur(double angle, double radius) {
        setAngle(angle);
        setRadius(radius);
    }

    @Override
    com.sun.scenario.effect.MotionBlur impl_createImpl() {
        return new com.sun.scenario.effect.MotionBlur();
    };
    /**
     * The input for this {@code Effect}.
     * If set to {@code null}, or left unspecified, a graphical image of
     * the {@code Node} to which the {@code Effect} is attached will be
     * used as the input.
     * @defaultvalue null
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
     * @defaultvalue 10.0
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
                    return MotionBlur.this;
                }

                @Override
                public String getName() {
                    return "radius";
                }
            };
        }
        return radius;
    }

    /**
     * The angle of the motion effect, in degrees.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: n/a
     * </pre>
     * @defaultvalue 0.0
     */
    private DoubleProperty angle;


    public final void setAngle(double value) {
        angleProperty().set(value);
    }

    public final double getAngle() {
        return angle == null ? 0 : angle.get();
    }

    public final DoubleProperty angleProperty() {
        if (angle == null) {
            angle = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return MotionBlur.this;
                }

                @Override
                public String getName() {
                    return "angle";
                }
            };
        }
        return angle;
    }

    private float getClampedRadius() {
        return (float)Utils.clamp(0, getRadius(), 63);
    }

    @Override
    void impl_update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.impl_sync();
        }

        com.sun.scenario.effect.MotionBlur peer =
                (com.sun.scenario.effect.MotionBlur) impl_getImpl();
        peer.setInput(localInput == null ? null : localInput.impl_getImpl());
        peer.setRadius(getClampedRadius());
        peer.setAngle((float)Math.toRadians(getAngle()));
    }

    private int getHPad() {
        return (int) Math.ceil(Math.abs(Math.cos(Math.toRadians(getAngle())))
                * getClampedRadius());
    }

    private int getVPad() {
        return (int) Math.ceil(Math.abs(Math.sin(Math.toRadians(getAngle())))
                * getClampedRadius());
    }

    /**
     * @treatasprivate implementation detail
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

        int hpad = getHPad();
        int vpad = getVPad();
        bounds = bounds.deriveWithPadding(hpad, vpad, 0);

        return EffectUtils.transformBounds(tx, bounds);
    }
}

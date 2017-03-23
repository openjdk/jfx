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
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

import com.sun.javafx.util.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;


/**
 * A motion blur effect using a Gaussian convolution kernel, with a
 * configurable radius and angle.
 *
 * <p>
 * Example:
 * <pre>{@code
 * MotionBlur motionBlur = new MotionBlur();
 * motionBlur.setRadius(30);
 * motionBlur.setAngle(-15.0);
 *
 * Text text = new Text();
 * text.setX(20.0);
 * text.setY(100.0);
 * text.setText("Motion!");
 * text.setFill(Color.web("0x3b596d"));
 * text.setFont(Font.font(null, FontWeight.BOLD, 60));
 * text.setEffect(motionBlur);
 * }</pre>
 * <p>
 * The code above produces the following:
 * </p>
 * <p>
 * <img src="doc-files/motionblur.png" alt="The visual effect of MotionBlur on text">
 * </p>
 * @since JavaFX 2.0
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
     * @since JavaFX 2.1
     */
    public MotionBlur(double angle, double radius) {
        setAngle(angle);
        setRadius(radius);
    }

    @Override
    com.sun.scenario.effect.MotionBlur createPeer() {
        return new com.sun.scenario.effect.MotionBlur();
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
     * @defaultValue 0.0
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
    void update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.sync();
        }

        com.sun.scenario.effect.MotionBlur peer =
                (com.sun.scenario.effect.MotionBlur) getPeer();
        peer.setInput(localInput == null ? null : localInput.getPeer());
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

    @Override
    BaseBounds getBounds(BaseBounds bounds,
                         BaseTransform tx,
                         Node node,
                         BoundsAccessor boundsAccessor) {
        bounds = getInputBounds(bounds,
                                BaseTransform.IDENTITY_TRANSFORM,
                                node, boundsAccessor,
                                getInput());

        int hpad = getHPad();
        int vpad = getVPad();
        bounds = bounds.deriveWithPadding(hpad, vpad, 0);

        return transformBounds(tx, bounds);
    }

    @Override
    Effect copy() {
        MotionBlur mb = new MotionBlur(this.getAngle(), this.getRadius());
        mb.setInput(mb.getInput());
        return mb;

    }
}

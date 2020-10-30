/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.SpotLightHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGSpotLight;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * A {@code PointLight} that radiates light in a cone in a specific direction. The direction of the {@code SpotLight} is
 * defined by the {@link #directionProperty() direction} property.
 * <p>
 * The light cone is defined by 3 factors: an {@link #innerAngleProperty() inner angle}, an {@link #outerAngleProperty()
 * outer angle}, and a {@link #falloffProperty() falloff} factor. For a point whose angle to the light is {@code a}, if
 * {@code a < innerAngle} then that point receives maximum illumination, if {@code a > outerAngle} then that point
 * receives no illumination, and if {@code innerAngle < a < outerAngle} then the illumination is determined by the
 * formula
 * <pre>I = pow((cos(a) - cos(outer)) / (cos(inner) - cos(outer)), falloff)</pre>
 * which represents a drop in illumination from the inner angle to the outer angle. {@code falloff} determines the
 * behavior of the drop.
 * <p>
 * <img src="doc-files/Spotlight.png" alt="Image of the Spotlight">
 *
 * @since 16
 * @see PhongMaterial
 */
public class SpotLight extends PointLight {
    static {
        SpotLightHelper.setSpotLightAccessor(new SpotLightHelper.SpotLightAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((SpotLight) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((SpotLight) node).doUpdatePeer();
            }
        });
    }

    {
        // To initialize the class helper at the beginning each constructor of this class
        SpotLightHelper.initHelper(this);
    }

    /**
     * Creates a new instance of {@code SpotLight} class with a default {@code Color.WHITE} light source.
     */
    public SpotLight() {
        super();
    }

    /**
     * Creates a new instance of {@code SpotLight} class using the specified color.
     *
     * @param color the color of the light source
     */
    public SpotLight(Color color) {
        super(color);
    }


    /**
     * The direction the spotlight is facing. The vector need not be normalized.
     *
     * @defaultValue {@code Point3D(0, 0, -1)}
     */
    private ObjectProperty<Point3D> direction;

    public final void setDirection(Point3D value) {
        directionProperty().set(value);
    }

    private static final Point3D DEFAULT_DIRECTION = NGSpotLight.getDefaultDirection();

    public final Point3D getDirection() {
        return direction == null ? DEFAULT_DIRECTION : direction.get();
    }

    public final ObjectProperty<Point3D> directionProperty() {
        if (direction == null) {
            direction = new SimpleObjectProperty<>(this, "direction", DEFAULT_DIRECTION) {
                @Override
                protected void invalidated() {
                    NodeHelper.markDirty(SpotLight.this, DirtyBits.NODE_LIGHT);
                }
            };
        }
        return direction;
    }


    /**
     * The angle of the spotlight's inner cone. Surfaces whose angle to the light's origin is less than this angle
     * receive the full light's intensity. Beyond this angle, the light intensity starts to drop.
     *
     * @defaultValue 0
     */
    private DoubleProperty innerAngle;

    public final void setInnerAngle(double value) {
        innerAngleProperty().set(value);
    }

    private static final double DEFAULT_INNER_ANGLE = NGSpotLight.getDefaultInnerAngle();

    public final double getInnerAngle() {
        return innerAngle == null ? DEFAULT_INNER_ANGLE : innerAngle.get();
    }

    public final DoubleProperty innerAngleProperty() {
        if (innerAngle == null) {
            innerAngle = getLightDoubleProperty("innerAngle", DEFAULT_INNER_ANGLE);
        }
        return innerAngle;
    }


    /**
     * The angle of the spotlight's outer cone. Surfaces whose angle to the light's origin is greater than this angle
     * receive no light. Before this angle, the light intensity starts to increase.
     *
     * @defaultValue 90
     */
    private DoubleProperty outerAngle;

    public final void setOuterAngle(double value) {
        outerAngleProperty().set(value);
    }

    private static final double DEFAULT_OUTER_ANGLE = NGSpotLight.getDefaultOuterAngle();

    public final double getOuterAngle() {
        return outerAngle == null ? DEFAULT_OUTER_ANGLE : outerAngle.get();
    }

    public final DoubleProperty outerAngleProperty() {
        if (outerAngle == null) {
            outerAngle = getLightDoubleProperty("outerAngle", DEFAULT_OUTER_ANGLE);
        }
        return outerAngle;
    }


    /**
     * The intensity falloff factor of the spotlight's outer cone. Surfaces whose angle to the light's origin is greater
     * than the inner angle but less than the outer angle receive partial intensity governed by this factor. The larger
     * the falloff, the sharper the drop in intensity from the inner cone. A falloff factor of 1 gives a linear drop in
     * intensity, values greater than 1 give a convex drop, and values smaller than 1 give a concave drop. Negative
     * values are allowed, but give unrealistic lighting.
     *
     * @defaultValue 1
     */
    private DoubleProperty falloff;

    public final void setFalloff(double value) {
        falloffProperty().set(value);
    }

    private static final double DEFAULT_FALLOFF = NGSpotLight.getDefaultFalloff();

    public final double getFalloff() {
        return falloff == null ? DEFAULT_FALLOFF : falloff.get();
    }

    public final DoubleProperty falloffProperty() {
        if (falloff == null) {
            falloff = getLightDoubleProperty("falloff", DEFAULT_FALLOFF);
        }
        return falloff;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGSpotLight();
    }

    private void doUpdatePeer() {
        if (isDirty(DirtyBits.NODE_LIGHT)) {
            NGSpotLight peer = getPeer();
            peer.setDirection(getDirection());
            peer.setInnerAngle((float) getInnerAngle());
            peer.setOuterAngle((float) getOuterAngle());
            peer.setFalloff((float) getFalloff());
        }
    }
}

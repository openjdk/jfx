/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * A {@code SpotLight} is a {@code PointLight} that radiates light in a cone in a specific direction.
 * The direction is defined by the {@link #directionProperty() direction} vector property of the light. The direction
 * can be rotated by setting a rotation transform on the {@code SpotLight}. For example, if the direction vector is
 * {@code (1, 1, 1)} and the light is not rotated, it will point in the {@code (1, 1, 1)} direction, and if the light is
 * rotated 90 degrees on the y axis, it will point in the {@code (1, 1, -1)} direction.
 * <p>
 * In addition to the factors that control the light intensity of a {@code PointLight}, a {@code SpotLight} has a
 * light-cone attenuation factor, {@code spot}, that is determined by 3 properties:
 * <ul>
 * <li> {@link #innerAngleProperty() innerAngle}: the angle of the inner cone (see image below)
 * <li> {@link #outerAngleProperty() outerAngle}: the angle of the outer cone (see image below)
 * <li> {@link #falloffProperty() falloff}: the factor that controls the light's intensity drop inside the outer cone
 * </ul>
 * The valid ranges for these properties are {@code 0 <= innerAngle <= outerAngle <= 180} and {@code falloff >= 0};
 * values outside either of these ranges can produce unexpected results.
 * <p>
 * The angle of a point to the light is defined as the angle between its vector to the light's position and the
 * direction of the light. For such an angle {@code theta}, if
 * <ul>
 * <li>{@code theta < innerAngle} then {@code spot = 1}
 * <li>{@code theta > outerAngle} then {@code spot = 0}
 * <li>{@code innerAngle <= theta <= outerAngle} then
 *
 * <pre>spot = pow((cos(theta) - cos(outer)) / (cos(inner) - cos(outer)), falloff)</pre>
 *
 * which represents a drop in intensity from the inner angle to the outer angle.
 * </ul>
 * As a result, {@code 0 <= spot <= 1}. The overall intensity of the light is {@code I = lambert * atten * spot}.
 * <p>
 * <img src="doc-files/spotlight.png" alt="Image of the Spotlight">
 *
 * @since 17
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
        // To initialize the class helper at the beginning of each constructor of this class
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
     * The direction vector of the spotlight. It can be rotated by setting a rotation transform on the
     * {@code SpotLight}. The vector need not be normalized.
     *
     * @defaultValue {@code Point3D(0, 0, 1)}
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
     * The angle of the spotlight's inner cone, in degrees. A point whose angle to the light is less than this angle is
     * not attenuated by the spotlight factor ({@code spot = 1}). At larger angles, the light intensity starts to drop.
     * See the class doc for more information.
     * <p>
     * The valid range is {@code 0 <= innerAngle <= outerAngle}; values outside of this range can produce unexpected
     * results.
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
     * The angle of the spotlight's outer cone, in degrees (as shown in the class doc image). A point whose angle to the
     * light is greater than this angle receives no light ({@code spot = 0}). A point whose angle to the light is less
     * than the outer angle but greater than the inner angle receives partial intensity governed by the falloff factor.
     * See the class doc for more information.
     * <p>
     * The valid range is {@code innerAngle <= outerAngle <= 180}; values outside of this range can produce unexpected
     * results.
     *
     * @defaultValue 30
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
     * The intensity falloff factor of the spotlight's outer cone. A point whose angle to the light is
     * greater than the inner angle but less than the outer angle receives partial intensity governed by this factor.
     * The larger the falloff, the sharper the drop in intensity from the inner cone. A falloff factor of 1 gives a
     * linear drop in intensity, values greater than 1 give a convex drop, and values smaller than 1 give a concave
     * drop. See the class doc for more information.
     * <p>
     * The valid range is {@code 0 <= falloff}; values outside of this range can produce unexpected results.
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

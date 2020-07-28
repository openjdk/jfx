/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.scene.PointLightHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGPointLight;

import javafx.beans.property.DoubleProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * A light source that radiates light equally in all directions away from itself. The location of the light
 * source is a single point in space. Any pixel within the range of the light will be illuminated by it,
 * unless it belongs to a {@code Shape3D} outside of its {@code scope}.
 * <p>
 * The light's intensity can be set to decrease over distance by attenuating it. The attenuation formula
 * <p>
 * {@code attn = 1 / (ca + la * dist + qa * dist^2)}
 * <p>
 * defines 3 coefficients: {@code ca}, {@code la}, and {@code qa}, which control the constant, linear, and
 * quadratic behaviors of intensity falloff over distance, respectively. The effective color of the light
 * at a given point in space is {@code color * attn}. It is possible, albeit unrealistic, to specify negative
 * values to attenuation coefficients. This allows the resulting attenuation factor to be negative, which
 * results in the light's color being subtracted from the material instead of added to it, thus creating a
 * "shadow caster".
 * <p>
 * For a realistic effect, {@code maxRange} should be set to a distance at which the attenuation is close to 0
 * as this will give a soft cutoff.
 *
 * @since JavaFX 8.0
 * @see PhongMaterial
 */
public class PointLight extends LightBase {
    static {
        PointLightHelper.setPointLightAccessor(new PointLightHelper.PointLightAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((PointLight) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((PointLight) node).doUpdatePeer();
            }
        });
    }

    {
        // To initialize the class helper at the beginning each constructor of this class
        PointLightHelper.initHelper(this);
    }

    /**
     * Creates a new instance of {@code PointLight} class with a default {@code Color.WHITE} light source.
     */
    public PointLight() {
        super();
    }

    /**
     * Creates a new instance of {@code PointLight} class using the specified color.
     *
     * @param color the color of the light source
     */
    public PointLight(Color color) {
        super(color);
    }

    /**
     * The maximum range of this {@code PointLight}. For a pixel to be affected by this light, its distance to the
     * light source must be less than or equal to the light's maximum range. Any negative value will be treated as 0.
     * <p>
     * Lower {@code maxRange} values can give better performance as pixels outside the range of the light
     * will not require complex calculation. The attenuation formula can be used to calculate a realistic
     * {@code maxRange} value by finding the distance where the attenuation is close enough to 0.
     * <p>
     * Nodes that are inside the light's range can still be excluded from the light's effect by removing them from
     * its {@link #getScope() scope} (or including them in its {@link #getExclusionScope() exclusion scope}). If a
     * node is known to always be outside of the light's range, it is more performant to exclude it from its scope.
     *
     * @defaultValue {@code Double.POSITIVE_INFINITY}
     * @since 16
     */
    private DoubleProperty maxRange;

    public final void setMaxRange(double value) {
        maxRangeProperty().set(value);
    }

    private static final double DEFAULT_MAX_RANGE = NGPointLight.getDefaultMaxRange();

    public final double getMaxRange() {
        return maxRange == null ? DEFAULT_MAX_RANGE : maxRange.get();
    }

    public final DoubleProperty maxRangeProperty() {
        if (maxRange == null) {
            maxRange = getLightDoubleProperty("maxRange", DEFAULT_MAX_RANGE);
        }
        return maxRange;
    }

    /**
     * The constant attenuation coefficient. This is the term {@code ca} in the attenuation formula:
     * <p>
     * {@code attn = 1 / (ca + la * dist + qa * dist^2)}
     * <p>
     * where {@code dist} is the distance between the light source and the pixel.
     *
     * @defaultValue 1
     * @since 16
     */
    private DoubleProperty constantAttenuation;

    public final void setConstantAttenuation(double value) {
        constantAttenuationProperty().set(value);
    }

    private static final double DEFAULT_CONSTANT_ATTENUATION = NGPointLight.getDefaultCa();

    public final double getConstantAttenuation() {
        return constantAttenuation == null ? DEFAULT_CONSTANT_ATTENUATION : constantAttenuation.get();
    }

    public final DoubleProperty constantAttenuationProperty() {
        if (constantAttenuation == null) {
            constantAttenuation = getLightDoubleProperty("constantAttenuation", DEFAULT_CONSTANT_ATTENUATION);
        }
        return constantAttenuation;
    }

    /**
     * The linear attenuation coefficient. This is the term {@code la} in the attenuation formula:
     * <p>
     * {@code attn = 1 / (ca + la * dist + qa * dist^2)}
     * <p>
     * where {@code dist} is the distance between the light source and the pixel.
     *
     * @defaultValue 0
     * @since 16
     */
    private DoubleProperty linearAttenuation;

    public final void setLinearAttenuation(double value) {
        linearAttenuationProperty().set(value);
    }

    private static final double DEFAULT_LINEAR_ATTENUATION = NGPointLight.getDefaultLa();

    public final double getLinearAttenuation() {
        return linearAttenuation == null ? DEFAULT_LINEAR_ATTENUATION : linearAttenuation.get();
    }

    public final DoubleProperty linearAttenuationProperty() {
        if (linearAttenuation == null) {
            linearAttenuation = getLightDoubleProperty("linearAttenuation", DEFAULT_LINEAR_ATTENUATION);
        }
        return linearAttenuation;
    }

    /**
     * The quadratic attenuation coefficient. This is the term {@code qa} in the attenuation formula:
     * <p>
     * {@code attn = 1 / (ca + la * dist + qa * dist^2)}
     * <p>
     * where {@code dist} is the distance between the light source and the pixel.
     *
     * @defaultValue 0
     * @since 16
     */
    private DoubleProperty quadraticAttenuation;

    public final void setQuadraticAttenuation(double value) {
        quadraticAttenuationProperty().set(value);
    }

    private static final double DEFAULT_QUADRATIC_ATTENUATION = NGPointLight.getDefaultQa();

    public final double getQuadraticAttenuation() {
        return quadraticAttenuation == null ? DEFAULT_QUADRATIC_ATTENUATION : quadraticAttenuation.get();
    }

    public final DoubleProperty quadraticAttenuationProperty() {
        if (quadraticAttenuation == null) {
            quadraticAttenuation = getLightDoubleProperty("quadraticAttenuation", DEFAULT_QUADRATIC_ATTENUATION);
        }
        return quadraticAttenuation;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGPointLight();
    }

    private void doUpdatePeer() {
        if (isDirty(DirtyBits.NODE_LIGHT)) {
            NGPointLight peer = getPeer();
            peer.setCa((float) getConstantAttenuation());
            peer.setLa((float) getLinearAttenuation());
            peer.setQa((float) getQuadraticAttenuation());
            peer.setMaxRange((float) getMaxRange());
        }
    }
}

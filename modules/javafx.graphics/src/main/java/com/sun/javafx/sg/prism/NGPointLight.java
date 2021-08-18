/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import javafx.geometry.Point3D;

/**
 * The peer of the {@code PointLight} class. Holds the default values of {@code PointLight}'s
 * properties and updates the visuals via {@link NGNode#visualsChanged} when one of the current
 * values changes. The peer receives its changes by {@code PointLight.doUpdatePeer} calls.
 */
public class NGPointLight extends NGLightBase {

    /** Constant attenuation factor default value */
    private static final float DEFAULT_CA = 1;
    /** Linear attenuation factor default value */
    private static final float DEFAULT_LA = 0;
    /** Quadratic attenuation factor default value */
    private static final float DEFAULT_QA = 0;
    /** Max range default value */
    private static final float DEFAULT_MAX_RANGE = Float.POSITIVE_INFINITY;

    /**
     * The direction of a {@code SpotLight} that simulates a {@code PointLight}.
     * Since the light radiates equally in all directions, this value is meaningless.
     **/
    private static final Point3D SIMULATED_DIRECTION = new Point3D(0, 0, 1);
    /** The inner angle value of a {@code SpotLight} that simulates a {@code PointLight} */
    private static final float SIMULATED_INNER_ANGLE = 0;
    /** The outer angle value of a {@code SpotLight} that simulates a {@code PointLight} */
    private static final float SIMULATED_OUTER_ANGLE = 180;
    /** The falloff value of a {@code SpotLight} that simulates a {@code PointLight} */
    private static final float SIMULATED_FALLOFF = 0;

    public NGPointLight() {
    }

    public static float getDefaultCa() {
        return DEFAULT_CA;
    }

    public static float getDefaultLa() {
        return DEFAULT_LA;
    }

    public static float getDefaultQa() {
        return DEFAULT_QA;
    }

    public static float getDefaultMaxRange() {
        return DEFAULT_MAX_RANGE;
    }

    public static Point3D getSimulatedDirection() {
        return SIMULATED_DIRECTION;
    }

    public static float getSimulatedInnerAngle() {
        return SIMULATED_INNER_ANGLE;
    }

    public static float getSimulatedOuterAngle() {
        return SIMULATED_OUTER_ANGLE;
    }

    public static float getSimulatedFalloff() {
        return SIMULATED_FALLOFF;
    }

    public Point3D getDirection() {
        return SIMULATED_DIRECTION;
    }

    public float getInnerAngle() {
        return SIMULATED_INNER_ANGLE;
    }

    public float getOuterAngle() {
        return SIMULATED_OUTER_ANGLE;
    }

    public float getFalloff() {
        return SIMULATED_FALLOFF;
    }


    private float ca = DEFAULT_CA;

    public float getCa() {
        return ca;
    }

    public void setCa(float ca) {
        if (this.ca != ca) {
            this.ca = ca;
            visualsChanged();
        }
    }


    private float la = DEFAULT_LA;

    public float getLa() {
        return la;
    }

    public void setLa(float la) {
        if (this.la != la) {
            this.la = la;
            visualsChanged();
        }
    }


    private float qa = DEFAULT_QA;

    public float getQa() {
        return qa;
    }

    public void setQa(float qa) {
        if (this.qa != qa) {
            this.qa = qa;
            visualsChanged();
        }
    }


    private float maxRange = DEFAULT_MAX_RANGE;

    public float getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(float maxRange) {
        maxRange = maxRange < 0 ? 0 : maxRange;
        if (this.maxRange != maxRange) {
            this.maxRange = maxRange;
            visualsChanged();
        }
    }
}

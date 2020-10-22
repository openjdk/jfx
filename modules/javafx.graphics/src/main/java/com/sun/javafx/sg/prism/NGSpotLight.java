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

package com.sun.javafx.sg.prism;

import com.sun.javafx.util.Utils;

import javafx.geometry.Point3D;

/**
 * The peer of the {@code SpotLight} class. Holds the default values of {@code SpotLight}'s
 * properties and updates the visuals via {@link NGNode#visualsChanged} when one of the current
 * values changes. The peer receives its changes by {@code SpotLight.doUpdatePeer} calls.
 */
public class NGSpotLight extends NGLightBase {

    /** Constant attenuation factor default value */
    private static final float DEFAULT_CA = 1;
    /** Linear attenuation factor default value */
    private static final float DEFAULT_LA = 0;
    /** Quadratic attenuation factor default value */
    private static final float DEFAULT_QA = 0;
    /** Max range default value */
    private static final float DEFAULT_MAX_RANGE = Float.POSITIVE_INFINITY;
    /** Direction default value */
    private static final Point3D DEFAULT_DIRECTION = new Point3D(0, 0, 1);
    /** Inner angle default value */
    private static final float DEFAULT_INNER_ANGLE = 0;
    /** Outer angle default value */
    private static final float DEFAULT_OUTER_ANGLE = 90;
    /** Falloff default value */
    private static final float DEFAULT_FALLOFF = 1;

    public NGSpotLight() {
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

    public static Point3D getDefaultDirection() {
        return DEFAULT_DIRECTION;
    }

    public static float getDefaultInnerAngle() {
        return DEFAULT_INNER_ANGLE;
    }
    
    public static float getDefaultOuterAngle() {
        return DEFAULT_OUTER_ANGLE;
    }

    public static float getDefaultFalloff() {
        return DEFAULT_FALLOFF;
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


    private Point3D direction = DEFAULT_DIRECTION;

    public Point3D getDirection() {
        return direction;
    }

    public void setDirection(Point3D direction) {
        if (!this.direction.equals(direction)) {
            this.direction = direction;
            visualsChanged();
        }
    }

    private float innerAngle = DEFAULT_INNER_ANGLE;

    public float getInnerAngle() {
        return innerAngle;
    }

    public void setInnerAngle(float innerAngle) {
        innerAngle = innerAngle < 0 ? 0 : innerAngle;
        this.innerAngle = Utils.clamp(0, innerAngle, outerAngle);
        visualsChanged();
    }

    private float outerAngle = DEFAULT_OUTER_ANGLE;

    public float getOuterAngle() {
        return outerAngle;
    }

    public void setOuterAngle(float outerAngle) {
        this.outerAngle = outerAngle < innerAngle ? innerAngle : outerAngle; // limit to 360?
        visualsChanged();
    }

    private float falloff = DEFAULT_FALLOFF;

    public float getFalloff() {
        return falloff;
    }

    public void setFalloff(float falloff) {
        if (this.falloff != falloff) {
            this.falloff = falloff;
            visualsChanged();
        }
    }
}

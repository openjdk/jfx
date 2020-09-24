/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * The peer of the {@code PointLight} class. Holds the default values of {@code PointLight}'s
 * properties and updates the visuals via {@link NGNode#visualsChanged} when one of the current
 * values changes. The peer receives its changes by {@code PointLight.doUpdatePeer} calls.
 */
public class NGSpotLight extends NGLightBase {

    /** Constant attenuation factor default value */
    private static final double DEFAULT_CA = 1;
    /** Linear attenuation factor default value */
    private static final double DEFAULT_LA = 0;
    /** Quadratic attenuation factor default value */
    private static final double DEFAULT_QA = 0;
    /** Max range default value */
    private static final double DEFAULT_MAX_RANGE = Double.POSITIVE_INFINITY;
    /** Direction default value */
    private static final Point3D DEFAULT_DIRECTION = new Point3D(0, 0, 1);
    /** Inner angle default value */
    private static final double DEFAULT_INNER_ANGLE = 0;
    /** Outer angle default value */
    private static final double DEFAULT_OUTER_ANGLE = 90;
    /** Falloff default value */
    private static final double DEFAULT_FALLOFF = 1;

    public NGSpotLight() {
    }

    public static double getDefaultCa() {
        return DEFAULT_CA;
    }

    public static double getDefaultLa() {
        return DEFAULT_LA;
    }

    public static double getDefaultQa() {
        return DEFAULT_QA;
    }

    public static double getDefaultMaxRange() {
        return DEFAULT_MAX_RANGE;
    }

    public static Point3D getDefaultDirection() {
        return DEFAULT_DIRECTION;
    }

    public static double getDefaultInnerAngle() {
        return DEFAULT_INNER_ANGLE;
    }
    
    public static double getDefaultOuterAngle() {
        return DEFAULT_OUTER_ANGLE;
    }

    public static double getDefaultFalloff() {
        return DEFAULT_FALLOFF;
    }


    private double ca = DEFAULT_CA;

    public double getCa() {
        return ca;
    }

    public void setCa(double ca) {
        this.ca = ca;
        visualsChanged();
    }

    private double la = DEFAULT_LA;

    public double getLa() {
        return la;
    }

    public void setLa(double la) {
        this.la = la;
        visualsChanged();
    }

    private double qa = DEFAULT_QA;

    public double getQa() {
        return qa;
    }

    public void setQa(double qa) {
        this.qa = qa;
        visualsChanged();
    }

    private double maxRange = DEFAULT_MAX_RANGE;

    public double getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(double maxRange) {
        this.maxRange = maxRange < 0 ? 0 : maxRange;
        visualsChanged();
    }

    private Point3D direction = DEFAULT_DIRECTION;

    public Point3D getDirection() {
        return direction;
    }

    public void setDirection(Point3D direction) {
        this.direction = direction;
        visualsChanged();
    }

    private double innerAngle = DEFAULT_INNER_ANGLE;

    public double getInnerAngle() {
        return innerAngle;
    }

    public void setInnerAngle(double innerAngle) {
        this.innerAngle = Utils.clamp(0, innerAngle, outerAngle);
        visualsChanged();
    }

    private double outerAngle = DEFAULT_OUTER_ANGLE;

    public double getOuterAngle() {
        return outerAngle;
    }

    public void setOuterAngle(double outerAngle) {
        this.outerAngle = outerAngle < innerAngle ? innerAngle : outerAngle; // limit to 360?
        visualsChanged();
    }

    private double falloff = DEFAULT_FALLOFF;

    public double getFalloff() {
        return falloff;
    }

    public void setFalloff(double falloff) {
        this.falloff = falloff;
        visualsChanged();
    }
}
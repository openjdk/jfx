/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.light;

import com.sun.scenario.effect.Color4f;

/**
 * Represents a distant light source.
 */
public class DistantLight extends Light {

    private float azimuth;
    private float elevation;

    /**
     * Constructs a new {@code DistantLight} with default azimuth (0.0),
     * elevation (0.0), and color ({@code Color4f.WHITE}).
     */
    public DistantLight() {
        this(0f, 0f, Color4f.WHITE);
    }

    /**
     * Constructs a new {@code DistantLight} with the given azimuth,
     * elevation, and color.
     *
     * @param azimuth the azimuth of the light, in degrees
     * @param elevation the elevation of the light, in degrees
     * @param color the color of the light
     * @throws IllegalArgumentException if {@code color} is null
     */
    public DistantLight(float azimuth, float elevation, Color4f color) {
        super(Type.DISTANT, color);
        this.azimuth = azimuth;
        this.elevation = elevation;
    }

    /**
     * Returns the azimuth of the light.  The azimuth is the direction angle
     * for the light source on the XY plane, in degrees.
     *
     * @return the azimuth of the light, in degrees
     */
    public float getAzimuth() {
        return azimuth;
    }

    /**
     * Sets the azimuth of the light.  The azimuth is the direction angle
     * for the light source on the XY plane, in degrees.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: n/a
     * </pre>
     *
     * @param azimuth the azimuth of the light, in degrees
     */
    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
    }

    /**
     * Returns the elevation of the light.  The elevation is the
     * direction angle for the light source on the YZ plane, in degrees.
     *
     * @return the elevation of the light, in degrees
     */
    public float getElevation() {
        return elevation;
    }

    /**
     * Sets the elevation of the light.  The elevation is the
     * direction angle for the light source on the YZ plane, in degrees.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: n/a
     * </pre>
     *
     * @param elevation the elevation of the light, in degrees
     */
    public void setElevation(float elevation) {
        this.elevation = elevation;
    }

    @Override
    public float[] getNormalizedLightPosition() {
        double a = Math.toRadians(azimuth);
        double e = Math.toRadians(elevation);
        float x = (float)(Math.cos(a) * Math.cos(e));
        float y = (float)(Math.sin(a) * Math.cos(e));
        float z = (float)(Math.sin(e));
        // normalize
        float len = (float)Math.sqrt(x*x + y*y + z*z);
        if (len == 0f) len = 1f;
        float[] pos = new float[] {x/len, y/len, z/len};
        return pos;
    }
}

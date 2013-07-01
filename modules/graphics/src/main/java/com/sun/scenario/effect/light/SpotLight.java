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
 * Represents a spot light source at a given position in 3D space, with
 * configurable direction and focus.
 */
public class SpotLight extends PointLight {

    private float pointsAtX;
    private float pointsAtY;
    private float pointsAtZ;
    private float specularExponent;
    //private float limitingConeAngle;

    /**
     * Constructs a new {@code PointLight} with default position (0,0,0),
     * direction (pointing at (0,0,0)) and specular exponent (1.0).
     */
    public SpotLight() {
        this(0f, 0f, 0f, Color4f.WHITE);
    }

    /**
     * Constructs a new {@code SpotLight} with the given position and color,
     * with the default direction (pointing at (0,0,0)) and specular exponent
     * (1.0).
     *
     * @param x the x coordinate of the light position
     * @param y the y coordinate of the light position
     * @param z the z coordinate of the light position
     * @param color the color of the light
     * @throws IllegalArgumentException if {@code color} is null
     */
    public SpotLight(float x, float y, float z, Color4f color) {
        super(Type.SPOT, x, y, z, color);
        this.pointsAtX = 0f;
        this.pointsAtY = 0f;
        this.pointsAtZ = 0f;
        this.specularExponent = 1f;
    }

    /**
     * Returns the x coordinate of the direction vector for this light.
     *
     * @return the x coordinate of the direction vector for this light
     */
    public float getPointsAtX() {
        return pointsAtX;
    }

    /**
     * Sets the x coordinate of the direction vector for this light.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: n/a
     * </pre>
     *
     * @param pointsAtX the x coordinate of the direction vector for this light
     */
    public void setPointsAtX(float pointsAtX) {
        this.pointsAtX = pointsAtX;
    }

    /**
     * Returns the y coordinate of the direction vector for this light.
     *
     * @return the y coordinate of the direction vector for this light
     */
    public float getPointsAtY() {
        return pointsAtY;
    }

    /**
     * Sets the y coordinate of the direction vector for this light.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: n/a
     * </pre>
     *
     * @param pointsAtY the y coordinate of the direction vector for this light
     */
    public void setPointsAtY(float pointsAtY) {
        float old = this.pointsAtY;
        this.pointsAtY = pointsAtY;
    }

    /**
     * Returns the z coordinate of the direction vector for this light.
     *
     * @return the z coordinate of the direction vector for this light
     */
    public float getPointsAtZ() {
        return pointsAtZ;
    }

    /**
     * Sets the z coordinate of the direction vector for this light.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: n/a
     * </pre>
     *
     * @param pointsAtZ the z coordinate of the direction vector for this light
     */
    public void setPointsAtZ(float pointsAtZ) {
        this.pointsAtZ = pointsAtZ;
    }

    /**
     * Returns the specular exponent, which controls the focus of this
     * light source.
     *
     * @return the specular exponent of this light
     */
    public float getSpecularExponent() {
        return specularExponent;
    }

    /**
     * Sets the specular exponent, which controls the focus of this
     * light source.
     * <pre>
     *       Min: 0.0
     *       Max: 4.0
     *   Default: 1.0
     *  Identity: 1.0
     * </pre>
     *
     * @param specularExponent the specular exponent of this light
     */
    public void setSpecularExponent(float specularExponent) {
        if (specularExponent < 0f || specularExponent > 4f) {
            throw new IllegalArgumentException("Specular exponent must be in the range [0,4]");
        }
        this.specularExponent = specularExponent;
    }

    @Override
    public float[] getNormalizedLightPosition() {
        // normalize
        float x = getX();
        float y = getY();
        float z = getZ();
        float len = (float)Math.sqrt(x*x + y*y + z*z);
        if (len == 0f) len = 1f;
        float[] pos = new float[] {x/len, y/len, z/len};
        return pos;
    }

    /**
     * Returns a float array containing the normalized {@code (x,y,z)}
     * direction vector of this light source.
     *
     * @return the normalized direction vector of this light source
     */
    public float[] getNormalizedLightDirection() {
        float sx = pointsAtX - getX();
        float sy = pointsAtY - getY();
        float sz = pointsAtZ - getZ();
        // normalize
        float len = (float)Math.sqrt(sx*sx + sy*sy + sz*sz);
        if (len == 0f) len = 1f;
        float[] vec = new float[] {sx/len, sy/len, sz/len};
        return vec;
    }
}

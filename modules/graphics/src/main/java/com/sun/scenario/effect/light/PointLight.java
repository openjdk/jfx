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
 * Represents a light source at a given position in 3D space.
 */
public class PointLight extends Light {

    private float x;
    private float y;
    private float z;

    /**
     * Constructs a new {@code PointLight} with default position (0,0,0)
     * and color ({@code Color4f.WHITE}).
     */
    public PointLight() {
        this(0f, 0f, 0f, Color4f.WHITE);
    }

    /**
     * Constructs a new {@code PointLight} with the given position and color.
     *
     * @param x the x coordinate of the light position
     * @param y the y coordinate of the light position
     * @param z the z coordinate of the light position
     * @param color the color of the light
     * @throws IllegalArgumentException if {@code color} is null
     */
    public PointLight(float x, float y, float z, Color4f color) {
        this(Type.POINT, x, y, z, color);
    }

    /**
     * Package-private constructor.
     *
     * @param type the type of the light (either {@code POINT} or {@code SPOT})
     * @param x the x coordinate of the light position
     * @param y the y coordinate of the light position
     * @param z the z coordinate of the light position
     * @param color the color of the light
     * @throws IllegalArgumentException if {@code color} is null
     */
    PointLight(Type type, float x, float y, float z, Color4f color) {
        super(type, color);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Returns the x coordinate of the light position.
     *
     * @return the x coordinate of the light position
     */
    public float getX() {
        return x;
    }

    /**
     * Sets the x coordinate of the light position.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: n/a
     * </pre>
     *
     * @param x the x coordinate of the light position
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * Returns the y coordinate of the light position.
     *
     * @return the y coordinate of the light position
     */
    public float getY() {
        return y;
    }

    /**
     * Sets the y coordinate of the light position.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: n/a
     * </pre>
     *
     * @param y the y coordinate of the light position
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * Returns the z coordinate of the light position.
     *
     * @return the z coordinate of the light position
     */
    public float getZ() {
        return z;
    }

    /**
     * Sets the z coordinate of the light position.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: n/a
     * </pre>
     *
     * @param z the z coordinate of the light position
     */
    public void setZ(float z) {
        this.z = z;
    }

    @Override
    public float[] getNormalizedLightPosition() {
        // normalize
        float len = (float)Math.sqrt(x*x + y*y + z*z);
        if (len == 0f) len = 1f;
        float[] pos = new float[] {x/len, y/len, z/len};
        return pos;
    }
}

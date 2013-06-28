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

package com.sun.scenario.effect;

/**
 * An immutable color, represented by four floating point values.
 * The color components should be provided in non-premultiplied format.
 */
public final class Color4f {

    public static final Color4f BLACK = new Color4f(0f, 0f, 0f, 1f);
    public static final Color4f WHITE = new Color4f(1f, 1f, 1f, 1f);

    private final float r;
    private final float g;
    private final float b;
    private final float a;

    public Color4f(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public float getRed() {
        return r;
    }

    public float getGreen() {
        return g;
    }

    public float getBlue() {
        return b;
    }

    public float getAlpha() {
        return a;
    }

    /**
     * Calculates and return the premultiplied color components of a
     * this {@code Color4f} object in an array of 4 {@code float}s.
     * @return an array of 4 {@code float}s containing the premultiplied
     *         color components of the color.
     */
    public float[] getPremultipliedRGBComponents() {
        float[] comps = new float[4];
        comps[0] = r*a;
        comps[1] = g*a;
        comps[2] = b*a;
        comps[3] = a;
        return comps;
    }
}

/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font;

import com.sun.javafx.geom.transform.BaseTransform;

public class FontStrikeDesc {

    // We don't need to reference the font resource here so
    // long as the map is stored on a resource.
    float[] matrix;
    float size;
    int aaMode; // grey scale or LCD.
    // TBD .. other rendering attributes.

    public FontStrikeDesc(float fontSize, BaseTransform transform, int aaMode) {

        //Affine2D tx2d = new Affine2D(transform);
        //tx2d.scale((double)fontSize, (double)fontSize);
        BaseTransform tx2d = transform;
        size = fontSize;
        this.aaMode = aaMode;
        matrix = new float[4];
        matrix[0] = (float)tx2d.getMxx();
        matrix[1] = (float)tx2d.getMxy();
        matrix[2] = (float)tx2d.getMyx();
        matrix[3] = (float)tx2d.getMyy();
    }

    private int hash;
    @Override
    public int hashCode() {
        if (hash == 0) {
            hash =
                aaMode +
                Float.floatToIntBits(size) +
                Float.floatToIntBits(matrix[0]) +
                Float.floatToIntBits(matrix[1]) +
                Float.floatToIntBits(matrix[2]) +
                Float.floatToIntBits(matrix[3]);
        }
        return hash;
    }

    /*
     * Seems like in some performance cases, its possible
     * to measure a difference between comparing floats
     * and doubles. I think if they are equal as floats
     * that's enough precision. Maybe we don't even need
     * that much precision.
     */
    @Override
    public boolean equals(Object o) {
        FontStrikeDesc other = (FontStrikeDesc)o;
        return
            this.aaMode == other.aaMode &&
            this.matrix[0] == other.matrix[0] &&
            this.matrix[1] == other.matrix[1] &&
            this.matrix[2] == other.matrix[2] &&
            this.matrix[3] == other.matrix[3] &&
            this.size == other.size;
    }

}

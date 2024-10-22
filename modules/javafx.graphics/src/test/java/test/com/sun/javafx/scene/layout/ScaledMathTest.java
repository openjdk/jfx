/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.layout;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.javafx.scene.layout.ScaledMath;

public class ScaledMathTest {
    private static final double[] SCALES = new double[] {0.5, 2.0 / 3.0, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 4.0 / 3.0};

    @Test
    void ceilShouldBeStable() {
        for (double scale : SCALES) {
            for (double d = 0; d < 1e13; d++, d *= 1.1) {  // larger values break down because there are not enough fractional digits anymore
                double expected = Math.ceil(d * scale) / scale;

                assertEquals(expected, ScaledMath.ceil(d, scale), 0.0);
                assertEquals(expected, ScaledMath.ceil(ScaledMath.ceil(d, scale), scale), 0.0);
            }
        }

        for (double scale : SCALES) {
            for (double d = 0; d > -1e13; d--, d *= 1.1) {  // larger values break down because there are not enough fractional digits anymore
                double expected = Math.ceil(d * scale) / scale;

                assertEquals(expected, ScaledMath.ceil(d, scale), 0.0);
                assertEquals(expected, ScaledMath.ceil(ScaledMath.ceil(d, scale), scale), 0.0);
            }
        }
    }

    @Test
    void floorShouldBeStable() {
        for (double scale : SCALES) {
            for (double d = 0; d < 1e13; d++, d *= 1.1) {  // larger values break down because there are not enough fractional digits anymore
                double expected = Math.floor(d * scale) / scale;

                assertEquals(expected, ScaledMath.floor(d, scale), 0.0);
                assertEquals(expected, ScaledMath.floor(ScaledMath.floor(d, scale), scale), 0.0);
            }
        }

        for (double scale : SCALES) {
            for (double d = 0; d > -1e13; d--, d *= 1.1) {  // larger values break down because there are not enough fractional digits anymore
                double expected = Math.floor(d * scale) / scale;

                assertEquals(expected, ScaledMath.floor(d, scale), 0.0);
                assertEquals(expected, ScaledMath.floor(ScaledMath.floor(d, scale), scale), 0.0);
            }
        }
    }

    @Test
    void ceilShouldHandleLargeMagnitudeValuesWithoutReturningNaN() {
        assertEquals(Double.MAX_VALUE, ScaledMath.ceil(Double.MAX_VALUE, 0.5), Math.ulp(Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, ScaledMath.ceil(Double.MAX_VALUE, 1.0), Math.ulp(Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, ScaledMath.ceil(Double.MAX_VALUE, 1.5), Math.ulp(Double.MAX_VALUE));

        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.ceil(-Double.MAX_VALUE, 0.5), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.ceil(-Double.MAX_VALUE, 1.0), 0.0);
        assertEquals(-Double.MAX_VALUE, ScaledMath.ceil(-Double.MAX_VALUE, 1.5), Math.ulp(-Double.MAX_VALUE));

        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.ceil(Double.POSITIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.ceil(Double.POSITIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.ceil(Double.POSITIVE_INFINITY, 1.5), 0.0);

        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.ceil(Double.NEGATIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.ceil(Double.NEGATIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.ceil(Double.NEGATIVE_INFINITY, 1.5), 0.0);
    }

    @Test
    void floorShouldHandleLargeMagnitudeValuesWithoutReturningNaN() {
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.floor(Double.MAX_VALUE, 0.5), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.floor(Double.MAX_VALUE, 1.0), 0.0);
        assertEquals(Double.MAX_VALUE, ScaledMath.floor(Double.MAX_VALUE, 1.5), Math.ulp(Double.MAX_VALUE));

        assertEquals(-Double.MAX_VALUE, ScaledMath.floor(-Double.MAX_VALUE, 0.5), Math.ulp(-Double.MAX_VALUE));
        assertEquals(-Double.MAX_VALUE, ScaledMath.floor(-Double.MAX_VALUE, 1.0), Math.ulp(-Double.MAX_VALUE));
        assertEquals(-Double.MAX_VALUE, ScaledMath.floor(-Double.MAX_VALUE, 1.5), Math.ulp(-Double.MAX_VALUE));

        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.floor(Double.POSITIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.floor(Double.POSITIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.floor(Double.POSITIVE_INFINITY, 1.5), 0.0);

        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.floor(Double.NEGATIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.floor(Double.NEGATIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.floor(Double.NEGATIVE_INFINITY, 1.5), 0.0);
    }

    @Test
    void rintShouldHandleLargeMagnitudeValuesWithoutReturningNaN() {
        assertEquals(Double.MAX_VALUE, ScaledMath.rint(Double.MAX_VALUE, 0.5), 0.0);
        assertEquals(Double.MAX_VALUE, ScaledMath.rint(Double.MAX_VALUE, 1.0), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.rint(Double.MAX_VALUE, 1.5), 0.0);

        assertEquals(-Double.MAX_VALUE, ScaledMath.rint(-Double.MAX_VALUE, 0.5), 0.0);
        assertEquals(-Double.MAX_VALUE, ScaledMath.rint(-Double.MAX_VALUE, 1.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.rint(-Double.MAX_VALUE, 1.5), 0.0);

        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.rint(Double.POSITIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.rint(Double.POSITIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.rint(Double.POSITIVE_INFINITY, 1.5), 0.0);

        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.rint(Double.NEGATIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.rint(Double.NEGATIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.rint(Double.NEGATIVE_INFINITY, 1.5), 0.0);
    }
}

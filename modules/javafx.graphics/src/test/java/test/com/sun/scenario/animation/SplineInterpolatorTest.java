/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.scenario.animation;

import com.sun.scenario.animation.SplineInterpolator;
import javafx.animation.Interpolator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SplineInterpolatorTest {

    private SplineInterpolator interpolator;

    @BeforeEach
    public void setUp() throws Exception {
        interpolator = new SplineInterpolator(0.2, 0.1, 0.3, 0.4);
    }

    private static void testEqualsAndHashCode(Interpolator one, Interpolator another) {
        assertTrue(one.equals(another));
        assertTrue(another.equals(one));
        assertEquals(one.hashCode(), another.hashCode());
    }

    private static void testNotEqualsAndHashCode(Interpolator one, Interpolator another) {
        assertFalse(one.equals(another));
        assertFalse(another.equals(one));
        assertFalse(one.hashCode() == another.hashCode());
    }

    @Test
    public void testEqualsAndHashCode() {
        Interpolator another = new SplineInterpolator(0.2, 0.1, 0.3, 0.4);
        testEqualsAndHashCode(interpolator, another);
    }

    @Test
    public void testNotEqualsAndHashCode() {
        Interpolator another = new SplineInterpolator(0.2, 0.1, 0.3, 0.5);
        testNotEqualsAndHashCode(interpolator, another);

        another = new SplineInterpolator(0.3, 0.5, 0.2, 0.1);
        testNotEqualsAndHashCode(interpolator, another);

        another = new SplineInterpolator(0.2, 0.1, 0.6, 0.4);
        testNotEqualsAndHashCode(interpolator, another);

        another = new SplineInterpolator(0.2, 0.14, 0.3, 0.4);
        testNotEqualsAndHashCode(interpolator, another);

        another = new SplineInterpolator(0.25, 0.1, 0.3, 0.4);
        testNotEqualsAndHashCode(interpolator, another);
    }

    public enum CubicBezierCurve {
        YCoordinateWithinIntervalZeroToOne(0.17, 0.67, 1, 0.29, new double[] {
            0, 0.1364, 0.2175, 0.2750, 0.3187, 0.3535, 0.3821, 0.4062, 0.4272, 0.4460, 0.4635,
            0.4804, 0.4974, 0.5155, 0.5355, 0.5588, 0.5869, 0.6230, 0.6723, 0.7489, 1
        }),

        YCoordinateLessThanZeroAndGreaterThanOne(0.1, 4, 1, -3, new double[] {
            0, 0.8725, 1.1568, 1.2653, 1.2807, 1.2379, 1.1555, 1.0451, 0.9148, 0.7707, 0.6178,
            0.4605, 0.3031, 0.1503, 0.0072, -0.1192, -0.2193, -0.2775, -0.2643, -0.1041, 1
        });

        CubicBezierCurve(double x1, double y1, double x2, double y2, double[] expectedOutputs) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.expectedOutputs = expectedOutputs;
        }

        final double x1, y1, x2, y2;
        final double[] expectedOutputs;
    }

    @ParameterizedTest
    @EnumSource(CubicBezierCurve.class)
    public void testSampleCurve(CubicBezierCurve curve) {
        var interpolator = new SplineInterpolator(curve.x1, curve.y1, curve.x2, curve.y2);

        for (int i = 0; i < curve.expectedOutputs.length; i++) {
            double actual = interpolator.curve(i * (1.0 / (curve.expectedOutputs.length - 1)));
            assertEquals(curve.expectedOutputs[i], actual, 0.001);
        }
    }
}

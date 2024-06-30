/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.layout;

import javafx.scene.layout.CornerRadii;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.stream.DoubleStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
public class CornerRadiiTest {
    @Test
    public void instanceCreation_singleConstructor() {
        CornerRadii r = new CornerRadii(1);
        assertEquals(1, r.getTopLeftHorizontalRadius(), 0);
        assertEquals(1, r.getTopLeftVerticalRadius(), 0);
        assertEquals(1, r.getTopRightVerticalRadius(), 0);
        assertEquals(1, r.getTopRightHorizontalRadius(), 0);
        assertEquals(1, r.getBottomRightHorizontalRadius(), 0);
        assertEquals(1, r.getBottomRightVerticalRadius(), 0);
        assertEquals(1, r.getBottomLeftVerticalRadius(), 0);
        assertEquals(1, r.getBottomLeftHorizontalRadius(), 0);
        assertFalse(r.isTopLeftHorizontalRadiusAsPercentage());
        assertFalse(r.isTopLeftVerticalRadiusAsPercentage());
        assertFalse(r.isTopRightVerticalRadiusAsPercentage());
        assertFalse(r.isTopRightHorizontalRadiusAsPercentage());
        assertFalse(r.isBottomRightHorizontalRadiusAsPercentage());
        assertFalse(r.isBottomRightVerticalRadiusAsPercentage());
        assertFalse(r.isBottomLeftVerticalRadiusAsPercentage());
        assertFalse(r.isBottomLeftHorizontalRadiusAsPercentage());
    }

    @Test
    public void negativeRadiusNotAllowed_singleConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new CornerRadii(-1));
    }

    @Test
    public void equality() {
        for (int i = 0; i < 8; ++i) {
            double[] r = new double[8];
            boolean[] p = new boolean[8];

            r[i] = 1;
            p[i] = true;

            var expected = new CornerRadii(
                r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7],
                p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7]);

            var a = new CornerRadii(
                r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7],
                p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7]);

            assertEquals(expected, a);

            // change one radius at a time
            r[i] = 0;
            p[i] = false;

            var b = new CornerRadii(
                r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7],
                p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7]);

            assertNotEquals(expected, b);
        }
    }

    @Nested
    class IsUniformTests {
        @Test
        public void isUniform_1ArgConstructor() {
            assertTrue(new CornerRadii(1).isUniform());
        }

        @Test
        public void isUniform_2ArgConstructor() {
            assertTrue(new CornerRadii(1, false).isUniform());
            assertTrue(new CornerRadii(1, true).isUniform());
        }

        @Test
        public void isUniform_5ArgConstructor() {
            for (boolean percent : new boolean[] { true, false }) {
                assertTrue(new CornerRadii(0, 0, 0, 0, percent).isUniform());
                assertTrue(new CornerRadii(1, 1, 1, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 0, 0, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 1, 0, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 0, 1, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 0, 0, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 1, 0, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 1, 1, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 0, 1, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 0, 0, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 0, 1, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 1, 0, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 1, 1, 0, percent).isUniform());
                assertFalse(new CornerRadii(0, 1, 1, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 0, 1, 1, percent).isUniform());
                assertFalse(new CornerRadii(1, 1, 0, 1, percent).isUniform());
            }
        }

        @Test
        public void isUniform_16ArgConstructor() {
            final int max = 1 << 16;
            double[] arg = new double[16];

            // Test all combinations of constructor arguments with a radius of either 0 or 1.
            for (int i = 0; i < max; ++i) {
                boolean uniform = true;

                for (int j = 0; j < 16; ++j) {
                    arg[j] = (i >> j) & 1;
                    uniform &= arg[j] == arg[j < 8 ? 0 : 8]; // args 0-8 must be equal, and args 8-16 must be equal
                }

                var cornerRadii = new CornerRadii(
                    arg[0], arg[1], arg[2], arg[3], arg[4], arg[5], arg[6], arg[7],
                    arg[8] > 0, arg[9] > 0, arg[10] > 0, arg[11] > 0, arg[12] > 0, arg[13] > 0, arg[14] > 0, arg[15] > 0);

                final boolean expectUniform = uniform;

                assertEquals(expectUniform, cornerRadii.isUniform(), () ->
                    "Expected " + (expectUniform ? "" : "not ") + "isUniform for constructor: " +
                    String.join(", ", DoubleStream.of(arg).limit(8).mapToObj(Double::toString).toList()) + ", " +
                    String.join(", ", DoubleStream.of(arg).skip(8).mapToObj(d -> Boolean.toString(d > 0)).toList()));
            }
        }
    }

    @Nested
    class InterpolationTests {
        @Test
        public void interpolateBetweenDifferentValuesReturnsNewInstance() {
            // non-uniform values
            var a = new CornerRadii(10, 20, 30, 40, false);
            var b = new CornerRadii(20, 40, 60, 80, false);
            var expect = new CornerRadii(15, 30, 45, 60, false);
            assertEquals(expect, a.interpolate(b, 0.5));

            // uniform values
            a = new CornerRadii(10, 10, 10, 10, false);
            b = new CornerRadii(20, 20, 20, 20, false);
            expect = new CornerRadii(15, 15, 15, 15, false);
            assertEquals(expect, a.interpolate(b, 0.5));
        }

        @Test
        public void interpolateBetweenEqualValuesReturnsStartInstance() {
            // non-uniform values
            var a = new CornerRadii(10, 20, 30, 40, false);
            var b = new CornerRadii(10, 20, 30, 40, false);
            assertSame(a, a.interpolate(b, 0.5));

            // uniform values
            a = new CornerRadii(10, 10, 10, 10, false);
            b = new CornerRadii(10, 10, 10, 10, false);
            assertSame(a, a.interpolate(b, 0.5));
        }

        @Test
        public void interpolateBetweenAbsoluteAndRelativeValuesReturnsStartInstanceOrNewInstanceEqualToEndValue() {
            var a = new CornerRadii(10, 20, 30, 40, false);
            var b = new CornerRadii(0.5, 0.6, 0.7, 0.8, true);
            assertSame(a, a.interpolate(b, 0)); // start value for t == 0

            var v = a.interpolate(b, 0.5); // new instance for t > 0
            assertEquals(b, v);
            assertNotSame(a, v);
            assertNotSame(b, v);
        }

        @Test
        public void interpolationFactorSmallerThanOrEqualToZeroReturnsStartInstance() {
            var a = new CornerRadii(10, 20, 30, 40, false);
            var b = new CornerRadii(20, 30, 40, 50, false);
            assertSame(a, a.interpolate(b, 0));
            assertSame(a, a.interpolate(b, -1));
        }

        @Test
        public void interpolationFactorGreaterThanOrEqualToOneReturnsEndInstance() {
            var a = new CornerRadii(10, 20, 30, 40, false);
            var b = new CornerRadii(20, 30, 40, 50, false);
            assertSame(b, a.interpolate(b, 1));
            assertSame(b, a.interpolate(b, 1.5));
        }
    }
}

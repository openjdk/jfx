/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.scenario.animation.LinearInterpolator;
import javafx.geometry.Point2D;
import test.javafx.util.ReflectionUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LinearInterpolatorTest {

    @Test
    void constructor_nullArray_throws() {
        assertThrows(NullPointerException.class, () -> new LinearInterpolator(null));
    }

    @Test
    void constructor_emptyArray_throws() {
        assertThrows(IllegalArgumentException.class, () -> new LinearInterpolator(new Point2D[0]));
    }

    @Test
    void constructor_singleElement_throws() {
        assertThrows(IllegalArgumentException.class, () -> new LinearInterpolator(new Point2D[] { new Point2D(0, 1) }));
    }

    @Nested
    class CanonicalizationTest {
        @Test
        void firstAndLastNaN_areCanonicalizedTo0And1() {
            var interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(Double.NaN, 0.2),
                new Point2D(Double.NaN, 0.8),
            });

            assertArrayEquals(
                new double[] {
                    0.0, 0.2, // first x: 0
                    1.0, 0.8 // last x: 1
                },
                ReflectionUtils.getFieldValue(interpolator, "controlPoints"),
                1e-9);
        }

        @Test
        void outOfOrderInputProgress_isCanonicalizedToMonotonicallyNonDecreasing() {
            // Intentionally out of order: 0.5, 0.25, 0.75
            LinearInterpolator interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(0.5, 0.0),
                new Point2D(0.25, 1.0),
                new Point2D(0.75, 0.5)
            });

            // After canonicalization, x's should be non-decreasing:
            // x0 = 0.5
            // x1 < x0 => clamped to 0.5
            // x2 = 0.75
            assertArrayEquals(
                new double[] { 0.5, 0.0, 0.5, 1.0, 0.75, 0.5 },
                ReflectionUtils.getFieldValue(interpolator, "controlPoints"),
                1e-9);
        }

        @Test
        void missingInputProgressValue_isCanonicalizedHalfWayBetweenPreviousAndNext() {
            var interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(0.0, 0.0),
                new Point2D(Double.NaN, 0.5),
                new Point2D(1.0, 1.0)
            });

            // Middle x should be halfway between 0.0 and 1.0 => 0.5
            assertArrayEquals(
                new double[] { 0.0, 0.0, 0.5, 0.5, 1.0, 1.0 },
                ReflectionUtils.getFieldValue(interpolator, "controlPoints"),
                1e-9);
        }

        @Test
        void contiguousNaNRange_isCanonicalizedToEvenlySpacedValues() {
            // x: 0.0, NaN, NaN, NaN, 1.0
            var interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(0.0, 0.0),
                new Point2D(Double.NaN, 0.2),
                new Point2D(Double.NaN, 0.4),
                new Point2D(Double.NaN, 0.6),
                new Point2D(1.0, 1.0)
            });

            // After canonicalization, the missing x's should be evenly spaced:
            // 0.0, 0.25, 0.5, 0.75, 1.0
            assertArrayEquals(
                new double[] {
                    0.0,  0.0,
                    0.25, 0.2,
                    0.5,  0.4,
                    0.75, 0.6,
                    1.0,  1.0
                },
                ReflectionUtils.getFieldValue(interpolator, "controlPoints"),
                1e-9);
        }

        @Test
        void twoContiguousNaNRanges_areCanonicalizedToEvenlySpacedValues() {
            // x: NaN, NaN, NaN, 0.5, NaN, NaN, NaN
            var interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(Double.NaN, 0.0),
                new Point2D(Double.NaN, 0.2),
                new Point2D(Double.NaN, 0.4),
                new Point2D(0.5,        0.6),
                new Point2D(Double.NaN, 0.8),
                new Point2D(Double.NaN, 1.0),
                new Point2D(Double.NaN, 1.2)
            });

            // After canonicalization:
            // - first NaN becomes 0.0 (first point)
            // - last NaN becomes 1.0 (last point)
            // We have two NaN runs:
            //   [0, NaN, NaN, 0.5]  ->  0.0, 1/6, 2/6, 0.5
            //   [0.5, NaN, NaN, 1]  ->  0.5, 4/6, 5/6, 1.0
            assertArrayEquals(
                new double[] {
                    0.0,        0.0,
                    1.0 / 6.0,  0.2,
                    2.0 / 6.0,  0.4,
                    0.5,        0.6,
                    4.0 / 6.0,  0.8,
                    5.0 / 6.0,  1.0,
                    1.0,        1.2
                },
                ReflectionUtils.getFieldValue(interpolator, "controlPoints"),
                1e-9);
        }
    }

    @Nested
    class CurveTest {
        @Test
        void twoPointsNaN_givesSimpleLinearInterpolation() {
            var interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(Double.NaN, 0.0), // -> x=0
                new Point2D(Double.NaN, 1.0)  // -> x=1
            });

            assertEquals(0.0, interpolator.curve(0.0), 1e-9);
            assertEquals(0.5, interpolator.curve(0.5), 1e-9);
            assertEquals(1.0, interpolator.curve(1.0), 1e-9);
        }

        @Test
        void curveUsesLastControlPointForMatchingX() {
            // x=0, x=0.5, x=0.5, x=1
            var interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(0.0, 0.0),
                new Point2D(0.5, 1.0),
                new Point2D(0.5, 0.2),  // same x as previous, different y
                new Point2D(1.0, 1.0)
            });

            assertEquals(0.0, interpolator.curve(0.0), 1e-9);
            assertEquals(0.2, interpolator.curve(0.5), 1e-9); // last y for x=0.5
            assertEquals(1.0, interpolator.curve(1.0), 1e-9);
        }

        @Test
        void curveInterpolatesBetweenInnerPoints() {
            var interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(0.0, 0.0),
                new Point2D(0.5, 0.5),
                new Point2D(1.0, 1.0)
            });

            assertEquals(0.25, interpolator.curve(0.25), 1e-9);
            assertEquals(0.75, interpolator.curve(0.75), 1e-9);
        }

        @Test
        void curveExtrapolatesBeforeFirstAndAfterLast() {
            var interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(Double.NaN, 0),
                new Point2D(0.5, 0.25),
                new Point2D(Double.NaN, 1)
            });

            // Extrapolate for t <= 0 with first segment
            assertEquals(0, interpolator.curve(0), 1e-9);
            assertEquals(-0.125, interpolator.curve(-0.25), 1e-9);

            // Extrapolate for t >= 1 with last segment
            assertEquals(1.0, interpolator.curve(1), 1e-9);
            assertEquals(1.375, interpolator.curve(1.25), 1e-9);
        }

        @Test
        void curveHandlesTBetweenDuplicateXsCorrectly() {
            var interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(0.0, 0.0),
                new Point2D(0.5, 0.0),
                new Point2D(0.5, 1.0),
                new Point2D(1.0, 1.0)
            });

            // For t between 0.0 and 0.5, we use segment (0,0)-(0.5,0)
            assertEquals(0.0, interpolator.curve(0.25), 1e-9);

            // For t between 0.5 and 1.0, we use segment (0.5,1)-(1,1)
            assertEquals(1.0, interpolator.curve(0.75), 1e-9);
        }

        @Test
        void curveHandlesNumericOverflowToInfinity() {
            var interpolator = new LinearInterpolator(new Point2D[] {
                new Point2D(0, 0.5),
                new Point2D(9e-310, 1)
            });

            assertEquals(0.5, interpolator.curve(0));
            assertEquals(Double.POSITIVE_INFINITY, interpolator.curve(0.5));
            assertEquals(Double.NEGATIVE_INFINITY, interpolator.curve(-0.5));
        }
    }
}

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

import javafx.geometry.Side;
import javafx.scene.layout.BackgroundPosition;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
public class BackgroundPositionTest {
    @Test
    public void valuesAreCorrectAfterConstruction() {
        BackgroundPosition pos = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);

        assertEquals(Side.LEFT, pos.getHorizontalSide());
        assertEquals(10, pos.getHorizontalPosition(), 0);
        assertEquals(false, pos.isHorizontalAsPercentage());
        assertEquals(Side.TOP, pos.getVerticalSide());
        assertEquals(20, pos.getVerticalPosition(), 0);
        assertEquals(false, pos.isVerticalAsPercentage());
    }

    @Test public void valuesAreCorrectAfterConstruction2() {
        BackgroundPosition pos = new BackgroundPosition(Side.RIGHT, 10, true, Side.BOTTOM, 20, true);

        assertEquals(Side.RIGHT, pos.getHorizontalSide());
        assertEquals(10, pos.getHorizontalPosition(), 0);
        assertEquals(true, pos.isHorizontalAsPercentage());
        assertEquals(Side.BOTTOM, pos.getVerticalSide());
        assertEquals(20, pos.getVerticalPosition(), 0);
        assertEquals(true, pos.isVerticalAsPercentage());
    }

    @Test public void nullHorizontalSideEqualsLEFT() {
        BackgroundPosition pos = new BackgroundPosition(null, 10, true, Side.BOTTOM, 20, true);
        assertEquals(Side.LEFT, pos.getHorizontalSide());
    }

    @Test
    public void TOPHorizontalSideFails() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundPosition(Side.TOP, 10, true, Side.BOTTOM, 20, true));
    }

    @Test
    public void BOTTOMHorizontalSideFails() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundPosition(Side.BOTTOM, 10, true, Side.BOTTOM, 20, true));
    }

    @Test public void negativeHorizontalPositionOK() {
        BackgroundPosition pos = new BackgroundPosition(null, -10, true, Side.BOTTOM, 20, true);
        assertEquals(-10, pos.getHorizontalPosition(), 0);
    }

    @Test public void nullVerticalSideEqualsTOP() {
        BackgroundPosition pos = new BackgroundPosition(Side.LEFT, 10, true, null, 20, true);
        assertEquals(Side.TOP, pos.getVerticalSide());
    }

    @Test
    public void LEFTVerticalSideFails() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundPosition(Side.LEFT, 10, true, Side.LEFT, 20, true));
    }

    @Test
    public void RIGHTVerticalSideFails() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundPosition(Side.LEFT, 10, true, Side.RIGHT, 20, true));
    }

    @Test public void negativeVerticalPositionOK() {
        BackgroundPosition pos = new BackgroundPosition(Side.LEFT, 10, true, Side.BOTTOM, -20, true);
        assertEquals(-20, pos.getVerticalPosition(), 0);
    }

    @Test public void equivalence() {
        BackgroundPosition pos = new BackgroundPosition(Side.LEFT, 0, true, Side.TOP, 0, true);
        assertEquals(BackgroundPosition.DEFAULT, pos);
    }

    @Test public void equivalence2() {
        BackgroundPosition a = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
        BackgroundPosition b = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
        assertEquals(a, b);
    }

    @Test public void unequal() {
        BackgroundPosition a = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
        BackgroundPosition b = new BackgroundPosition(Side.RIGHT, 10, false, Side.TOP, 20, false);
        assertFalse(a.equals(b));
    }

    @Test public void unequal2() {
        BackgroundPosition a = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
        BackgroundPosition b = new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 20, false);
        assertFalse(a.equals(b));
    }

    @Test public void unequal3() {
        BackgroundPosition a = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
        BackgroundPosition b = new BackgroundPosition(Side.LEFT, 10, true, Side.TOP, 20, false);
        assertFalse(a.equals(b));
    }

    @Test public void unequal4() {
        BackgroundPosition a = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
        BackgroundPosition b = new BackgroundPosition(Side.LEFT, 10, false, Side.BOTTOM, 20, false);
        assertFalse(a.equals(b));
    }

    @Test public void unequal5() {
        BackgroundPosition a = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
        BackgroundPosition b = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 0, false);
        assertFalse(a.equals(b));
    }

    @Test public void unequal6() {
        BackgroundPosition a = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
        BackgroundPosition b = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEqualWithNull() {
        BackgroundPosition a = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
        assertFalse(a.equals(null));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test public void notEqualWithRandom() {
        BackgroundPosition a = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
        assertFalse(a.equals("Random Object"));
    }

    @Test public void equalPositionsHaveSameHashCode() {
        BackgroundPosition pos = new BackgroundPosition(Side.LEFT, 0, true, Side.TOP, 0, true);
        assertEquals(BackgroundPosition.DEFAULT.hashCode(), pos.hashCode());
    }

    @Test public void CENTER() {
        assertEquals(Side.LEFT, BackgroundPosition.CENTER.getHorizontalSide());
        assertEquals(.5, BackgroundPosition.CENTER.getHorizontalPosition(), 0);
        assertTrue(BackgroundPosition.CENTER.isHorizontalAsPercentage());
        assertEquals(Side.TOP, BackgroundPosition.CENTER.getVerticalSide());
        assertEquals(.5, BackgroundPosition.CENTER.getVerticalPosition(), 0);
        assertTrue(BackgroundPosition.CENTER.isVerticalAsPercentage());
    }

    @Nested
    class InterpolationTests {
        @Test
        public void interpolateComponentWithAbsoluteAndPercentageMismatch() {
            record TestCase(BackgroundPosition endValue, BackgroundPosition expected) {}

            final double v0 = 0, v25 = 10, v50 = 20, v100 = 40;
            final var startValue = new BackgroundPosition(Side.LEFT, v0, false, Side.TOP, v0, false);

            // For each component: interpolation with t=0.25 returns start value on absolute/percentage mismatch.
            for (var testCase : new TestCase[] {
                new TestCase(
                    new BackgroundPosition(Side.LEFT, v100, false, Side.TOP, v100, false),
                    new BackgroundPosition(Side.LEFT, v25, false, Side.TOP, v25, false)),
                new TestCase(
                    new BackgroundPosition(Side.LEFT, v100, true, Side.TOP, v100, false),
                    new BackgroundPosition(Side.LEFT, v0, false, Side.TOP, v25, false)),
                new TestCase(
                    new BackgroundPosition(Side.LEFT, v100, false, Side.TOP, v100, true),
                    new BackgroundPosition(Side.LEFT, v25, false, Side.TOP, v0, false))
            }) {
                assertEquals(testCase.expected, startValue.interpolate(testCase.endValue, 0.25));
            }

            // For each component: interpolation with t=0.5 returns end value on absolute/percentage mismatch.
            for (var testCase : new TestCase[] {
                new TestCase(
                    new BackgroundPosition(Side.LEFT, v100, false, Side.TOP, v100, false),
                    new BackgroundPosition(Side.LEFT, v50, false, Side.TOP, v50, false)),
                new TestCase(
                    new BackgroundPosition(Side.LEFT, v100, true, Side.TOP, v100, false),
                    new BackgroundPosition(Side.LEFT, v100, true, Side.TOP, v50, false)),
                new TestCase(
                    new BackgroundPosition(Side.LEFT, v100, false, Side.TOP, v100, true),
                    new BackgroundPosition(Side.LEFT, v50, false, Side.TOP, v100, true))
            }) {
                assertEquals(testCase.expected, startValue.interpolate(testCase.endValue, 0.5));
            }
        }

        @Test
        public void interpolateBetweenDifferentValuesReturnsNewInstance() {
            var startValue = new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false);
            var endValue = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
            var expected = new BackgroundPosition(Side.LEFT, 5, false, Side.TOP, 10, false);
            assertEquals(expected, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolateBetweenEqualValuesReturnsStartInstance() {
            var startValue = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
            var endValue = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
            assertSame(startValue, startValue.interpolate(endValue, 0.25));
            assertSame(startValue, startValue.interpolate(endValue, 0.5));
            assertSame(startValue, startValue.interpolate(endValue, 0.75));
        }

        @Test
        public void interpolationFactorSmallerThanOrEqualToZeroReturnsStartInstance() {
            var startValue = new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false);
            var endValue = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
            assertSame(startValue, startValue.interpolate(endValue, 0));
            assertSame(startValue, startValue.interpolate(endValue, -0.5));
        }

        @Test
        public void interpolationFactorGreaterThanOrEqualToOneReturnsEndInstance() {
            var startValue = new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false);
            var endValue = new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false);
            assertSame(endValue, startValue.interpolate(endValue, 1));
            assertSame(endValue, startValue.interpolate(endValue, 1.5));
        }

        @Test
        public void percentageAndAbsolutePositionsCannotBeInterpolated() {
            var startValue = new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false);
            var endValue = new BackgroundPosition(Side.LEFT, 10, true, Side.TOP, 20, true);
            assertEquals(startValue, startValue.interpolate(endValue, 0.25)); // equal to 'startValue' for t < 0.5
            assertEquals(endValue, startValue.interpolate(endValue, 0.5)); // equal to 'endValue' otherwise
        }

        @Test
        public void differentSidesCannotBeInterpolated() {
            var startValue = new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false);
            var endValue = new BackgroundPosition(Side.RIGHT, 10, false, Side.TOP, 20, false);
            var expect = new BackgroundPosition(Side.RIGHT, 10, false, Side.TOP, 10, false);
            assertEquals(expect, startValue.interpolate(endValue, 0.5));

            startValue = new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false);
            endValue = new BackgroundPosition(Side.LEFT, 10, false, Side.BOTTOM, 20, false);
            expect = new BackgroundPosition(Side.LEFT, 5, false, Side.BOTTOM, 20, false);
            assertEquals(expect, startValue.interpolate(endValue, 0.5));
        }
    }
}

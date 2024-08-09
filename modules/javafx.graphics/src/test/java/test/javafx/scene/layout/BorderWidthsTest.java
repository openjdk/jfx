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

import javafx.scene.layout.BorderWidths;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO The spec doesn't seem to indicate, but what do we do (if anything) with percentages
 * above 100%?
 */
public class BorderWidthsTest {
    @Test public void instanceCreation() {
        BorderWidths widths = new BorderWidths(1, 2, 3, 4, false, true, false, true);
        assertEquals(1, widths.getTop(), 0);
        assertEquals(2, widths.getRight(), 0);
        assertEquals(3, widths.getBottom(), 0);
        assertEquals(4, widths.getLeft(), 0);
        assertFalse(widths.isTopAsPercentage());
        assertTrue(widths.isRightAsPercentage());
        assertFalse(widths.isBottomAsPercentage());
        assertTrue(widths.isLeftAsPercentage());
    }

    @Test public void instanceCreation2() {
        BorderWidths widths = new BorderWidths(1, 2, 3, 4, true, false, true, false);
        assertEquals(1, widths.getTop(), 0);
        assertEquals(2, widths.getRight(), 0);
        assertEquals(3, widths.getBottom(), 0);
        assertEquals(4, widths.getLeft(), 0);
        assertTrue(widths.isTopAsPercentage());
        assertFalse(widths.isRightAsPercentage());
        assertTrue(widths.isBottomAsPercentage());
        assertFalse(widths.isLeftAsPercentage());
    }

    @Test public void instanceCreation3() {
        BorderWidths widths = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        assertEquals(1, widths.getTop(), 0);
        assertEquals(2, widths.getRight(), 0);
        assertEquals(3, widths.getBottom(), 0);
        assertEquals(4, widths.getLeft(), 0);
        assertTrue(widths.isTopAsPercentage());
        assertFalse(widths.isRightAsPercentage());
        assertFalse(widths.isBottomAsPercentage());
        assertTrue(widths.isLeftAsPercentage());
    }

    @Test public void instanceCreation4() {
        BorderWidths widths = new BorderWidths(100);
        assertEquals(100, widths.getTop(), 0);
        assertEquals(100, widths.getRight(), 0);
        assertEquals(100, widths.getBottom(), 0);
        assertEquals(100, widths.getLeft(), 0);
        assertFalse(widths.isTopAsPercentage());
        assertFalse(widths.isRightAsPercentage());
        assertFalse(widths.isBottomAsPercentage());
        assertFalse(widths.isLeftAsPercentage());
    }

    @Test public void instanceCreation5() {
        BorderWidths widths = new BorderWidths(1, 2, 3, 4);
        assertEquals(1, widths.getTop(), 0);
        assertEquals(2, widths.getRight(), 0);
        assertEquals(3, widths.getBottom(), 0);
        assertEquals(4, widths.getLeft(), 0);
        assertFalse(widths.isTopAsPercentage());
        assertFalse(widths.isRightAsPercentage());
        assertFalse(widths.isBottomAsPercentage());
        assertFalse(widths.isLeftAsPercentage());
    }

    @Test
    public void cannotSpecifyNegativeWidth() {
        assertThrows(IllegalArgumentException.class, () -> new BorderWidths(-2));
    }

    @Test
    public void cannotSpecifyNegativeTop() {
        assertThrows(IllegalArgumentException.class, () -> new BorderWidths(-2, 0, 0, 0, false, false, false, false));
    }

    @Test
    public void cannotSpecifyNegativeTop2() {
        assertThrows(IllegalArgumentException.class, () -> new BorderWidths(-2, 0, 0, 0));
    }

    @Test
    public void cannotSpecifyNegativeRight() {
        assertThrows(IllegalArgumentException.class, () -> new BorderWidths(0, -2, 0, 0, false, false, false, false));
    }

    @Test
    public void cannotSpecifyNegativeRight2() {
        assertThrows(IllegalArgumentException.class, () -> new BorderWidths(0, -2, 0, 0));
    }

    @Test
    public void cannotSpecifyNegativeBottom() {
        assertThrows(IllegalArgumentException.class, () -> new BorderWidths(0, 0, -2, 0, false, false, false, false));
    }

    @Test
    public void cannotSpecifyNegativeBottom2() {
        assertThrows(IllegalArgumentException.class, () -> new BorderWidths(0, 0, -2, 0));
    }

    @Test
    public void cannotSpecifyNegativeLeft() {
        assertThrows(IllegalArgumentException.class, () -> new BorderWidths(0, 0, 0, -2, false, false, false, false));
    }

    @Test
    public void cannotSpecifyNegativeLeft2() {
        assertThrows(IllegalArgumentException.class, () -> new BorderWidths(0, 0, 0, -2));
    }

    @Test public void equality() {
        BorderWidths a = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        BorderWidths b = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        assertEquals(a, b);
    }

    @Test public void same() {
        assertEquals(BorderWidths.DEFAULT, BorderWidths.DEFAULT);
    }

    @Test public void different() {
        BorderWidths a = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        BorderWidths b = new BorderWidths(2, 2, 3, 4, true, false, false, true);
        assertFalse(a.equals(b));
    }

    @Test public void different2() {
        BorderWidths a = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        BorderWidths b = new BorderWidths(1, 3, 3, 4, true, false, false, true);
        assertFalse(a.equals(b));
    }

    @Test public void different3() {
        BorderWidths a = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        BorderWidths b = new BorderWidths(1, 2, 4, 4, true, false, false, true);
        assertFalse(a.equals(b));
    }

    @Test public void different4() {
        BorderWidths a = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        BorderWidths b = new BorderWidths(1, 2, 3, 5, true, false, false, true);
        assertFalse(a.equals(b));
    }

    @Test public void different5() {
        BorderWidths a = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        BorderWidths b = new BorderWidths(1, 2, 3, 4, false, false, false, true);
        assertFalse(a.equals(b));
    }

    @Test public void different6() {
        BorderWidths a = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        BorderWidths b = new BorderWidths(1, 2, 3, 4, true, true, false, true);
        assertFalse(a.equals(b));
    }

    @Test public void different7() {
        BorderWidths a = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        BorderWidths b = new BorderWidths(1, 2, 3, 4, true, false, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void different8() {
        BorderWidths a = new BorderWidths(1, 2, 3, 4, true, false, false, true);
        BorderWidths b = new BorderWidths(1, 2, 3, 4, true, false, false, false);
        assertFalse(a.equals(b));
    }

    @Test public void noEqualToNull() {
        assertFalse(BorderWidths.DEFAULT.equals(null));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test public void noEqualToRandom() {
        assertFalse(BorderWidths.DEFAULT.equals("Some random value"));
    }

    @Nested
    class InterpolationTests {
        @Test
        public void interpolateComponentWithAbsoluteAndPercentageMismatch() {
            record TestCase(BorderWidths endValue, BorderWidths expected) {}

            final double v0 = 10, v25 = 12.5, v50 = 15, v100 = 20;
            final var startValue = new BorderWidths(v0, v0, v0, v0, false, false, false, false);

            // For each component: interpolation with t=0.25 returns start value on absolute/percentage mismatch
            for (var testCase : new TestCase[] {
                new TestCase(
                    new BorderWidths(v100, v100, v100, v100, false, false, false, false),
                    new BorderWidths(v25, v25, v25, v25, false, false, false, false)),
                new TestCase(
                    new BorderWidths(v100, v100, v100, v100, true, false, false, false),
                    new BorderWidths(v0, v25, v25, v25, false, false, false, false)),
                new TestCase(
                    new BorderWidths(v100, v100, v100, v100, false, true, false, false),
                    new BorderWidths(v25, v0, v25, v25, false, false, false, false)),
                new TestCase(
                    new BorderWidths(v100, v100, v100, v100, false, false, true, false),
                    new BorderWidths(v25, v25, v0, v25, false, false, false, false)),
                new TestCase(
                    new BorderWidths(v100, v100, v100, v100, false, false, false, true),
                    new BorderWidths(v25, v25, v25, v0, false, false, false, false))
            }) {
                assertEquals(testCase.expected, startValue.interpolate(testCase.endValue, 0.25));
            }

            // For each component: interpolation with t=0.5 returns end value on absolute/percentage mismatch
            for (var testCase : new TestCase[] {
                new TestCase(
                    new BorderWidths(v100, v100, v100, v100, false, false, false, false),
                    new BorderWidths(v50, v50, v50, v50, false, false, false, false)),
                new TestCase(
                    new BorderWidths(v100, v100, v100, v100, true, false, false, false),
                    new BorderWidths(v100, v50, v50, v50, true, false, false, false)),
                new TestCase(
                    new BorderWidths(v100, v100, v100, v100, false, true, false, false),
                    new BorderWidths(v50, v100, v50, v50, false, true, false, false)),
                new TestCase(
                    new BorderWidths(v100, v100, v100, v100, false, false, true, false),
                    new BorderWidths(v50, v50, v100, v50, false, false, true, false)),
                new TestCase(
                    new BorderWidths(v100, v100, v100, v100, false, false, false, true),
                    new BorderWidths(v50, v50, v50, v100, false, false, false, true))
            }) {
                assertEquals(testCase.expected, startValue.interpolate(testCase.endValue, 0.5));
            }
        }

        @Test
        public void interpolateReturnsStartOrEndInstanceWhenResultIsEqual() {
            var startValue = new BorderWidths(10, 20, 30, 40, true, true, true, true);
            var endValue = new BorderWidths(20, 40, 60, 80, false, false, false, false);
            assertSame(startValue, startValue.interpolate(endValue, 0.25));
            assertSame(endValue, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolateBetweenDifferentValuesReturnsNewInstance() {
            var startValue = new BorderWidths(10, 20, 30, 40, true, false, true, false);
            var endValue = new BorderWidths(20, 40, 60, 80, true, false, true, false);
            var expect = new BorderWidths(15, 30, 45, 60, true, false, true, false);
            var actual = startValue.interpolate(endValue, 0.5);
            assertEquals(expect, actual);
            assertNotSame(startValue, actual);
            assertNotSame(endValue, actual);
        }

        @Test
        public void interpolateBetweenEqualValuesReturnsStartInstance() {
            var startValue = new BorderWidths(10, 20, 30, 40, true, false, true, false);
            var endValue = new BorderWidths(10, 20, 30, 40, true, false, true, false);
            assertSame(startValue, startValue.interpolate(endValue, 0.25));
            assertSame(startValue, startValue.interpolate(endValue, 0.5));
            assertSame(startValue, startValue.interpolate(endValue, 0.75));
        }

        @Test
        public void interpolationFactorSmallerThanOrEqualToZeroReturnsStartInstance() {
            var startValue = new BorderWidths(10, 20, 30, 40, true, false, true, false);
            var endValue = new BorderWidths(20, 40, 60, 80, true, false, true, false);
            assertSame(startValue, startValue.interpolate(endValue, 0));
            assertSame(startValue, startValue.interpolate(endValue, -1));
        }

        @Test
        public void interpolationFactorGreaterThanOrEqualToOneReturnsEndInstance() {
            var startValue = new BorderWidths(10, 20, 30, 40, true, false, true, false);
            var endValue = new BorderWidths(20, 40, 60, 80, true, false, true, false);
            assertSame(endValue, startValue.interpolate(endValue, 1));
            assertSame(endValue, startValue.interpolate(endValue, 1.5));
        }
    }
}

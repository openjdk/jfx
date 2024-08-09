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

import javafx.scene.layout.BackgroundSize;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static javafx.scene.layout.BackgroundSize.AUTO;
import static org.junit.jupiter.api.Assertions.*;

/**
 */
public class BackgroundSizeTest {
    @Test
    public void instanceCreation() {
        BackgroundSize size = new BackgroundSize(1, 2, true, false, true, false);
        assertEquals(1, size.getWidth(), 0);
        assertEquals(2, size.getHeight(), 0);
        assertTrue(size.isWidthAsPercentage());
        assertFalse(size.isHeightAsPercentage());
        assertTrue(size.isContain());
        assertFalse(size.isCover());
    }

    @Test public void instanceCreation2() {
        BackgroundSize size = new BackgroundSize(0, Double.MAX_VALUE, false, true, false, true);
        assertEquals(0, size.getWidth(), 0);
        assertEquals(Double.MAX_VALUE, size.getHeight(), 0);
        assertFalse(size.isWidthAsPercentage());
        assertTrue(size.isHeightAsPercentage());
        assertFalse(size.isContain());
        assertTrue(size.isCover());
    }

    @Test public void instanceCreation3() {
        BackgroundSize size = new BackgroundSize(.5, .5, true, true, false, false);
        assertEquals(.5, size.getWidth(), 0);
        assertEquals(.5, size.getHeight(), 0);
        assertTrue(size.isWidthAsPercentage());
        assertTrue(size.isHeightAsPercentage());
        assertFalse(size.isContain());
        assertFalse(size.isCover());
    }

    @Test
    public void negativeWidthThrowsException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundSize(-.2, 1, true, true, false, false));
    }

    @Test
    public void negativeWidthThrowsException2() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundSize(-2, 1, true, true, false, false));
    }

    @Disabled("JDK-8234090")
    @Test
    public void positiveInfinityWidthThrowsException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundSize(Double.POSITIVE_INFINITY, 1, true, true, false, false));
    }

    @Disabled("JDK-8234090")
    @Test
    public void negativeInfinityWidthThrowsException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundSize(Double.NEGATIVE_INFINITY, 1, true, true, false, false));
    }

    @Disabled("JDK-8234090")
    @Test
    public void nanWidthThrowsException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundSize(Double.NaN, 1, true, true, false, false));
    }

    @Test public void negativeZeroWidthIsOK() {
        BackgroundSize size = new BackgroundSize(-0, 1, true, true, false, false);
        assertEquals(0, size.getWidth(), 0);
    }

    @Test public void autoWidthIsOK() {
        BackgroundSize size = new BackgroundSize(-1, 1, true, true, false, false);
        assertEquals(AUTO, size.getWidth(), 0);
    }

    @Test
    public void negativeHeightThrowsException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundSize(1, -.1, true, true, false, false));
    }

    @Test
    public void negativeHeightThrowsException2() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundSize(1, -2, true, true, false, false));
    }

    @Disabled("JDK-8234090")
    @Test
    public void positiveInfinityHeightThrowsException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundSize(1, Double.POSITIVE_INFINITY, true, true, false, false));
    }

    @Disabled("JDK-8234090")
    @Test
    public void negativeInfinityHeightThrowsException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundSize(1, Double.NEGATIVE_INFINITY, true, true, false, false));
    }

    @Disabled("JDK-8234090")
    @Test
    public void nanHeightThrowsException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BackgroundSize(1, Double.NaN, true, true, false, false));
    }

    @Test public void negativeZeroHeightIsOK() {
        BackgroundSize size = new BackgroundSize(1, -0, true, true, false, false);
        assertEquals(0, size.getHeight(), 0);
    }

    @Test public void autoHeightIsOK() {
        BackgroundSize size = new BackgroundSize(1, -1, true, true, false, false);
        assertEquals(AUTO, size.getHeight(), 0);
    }

    @Test public void equivalent() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertEquals(a, b);
    }

    @Test public void equivalent2() {
        BackgroundSize a = new BackgroundSize(1, .5, false, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, false, true, true, true);
        assertEquals(a, b);
    }

    @Test public void equivalent3() {
        BackgroundSize a = new BackgroundSize(1, .5, true, false, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, false, true, true);
        assertEquals(a, b);
    }

    @Test public void equivalent4() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, false, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, false, true);
        assertEquals(a, b);
    }

    @Test public void equivalent5() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, false);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, false);
        assertEquals(a, b);
    }

    @Test public void equivalentHaveSameHashCode() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHaveSameHashCode2() {
        BackgroundSize a = new BackgroundSize(1, .5, false, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, false, true, true, true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHaveSameHashCode3() {
        BackgroundSize a = new BackgroundSize(1, .5, true, false, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, false, true, true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHaveSameHashCode4() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, false, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, false, true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHaveSameHashCode5() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, false);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, false);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void notEquivalent() {
        BackgroundSize a = new BackgroundSize(0, .5, true, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEquivalent2() {
        BackgroundSize a = new BackgroundSize(1, 1, true, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEquivalent3() {
        BackgroundSize a = new BackgroundSize(1, .5, false, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEquivalent4() {
        BackgroundSize a = new BackgroundSize(1, .5, true, false, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEquivalent5() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, false, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEquivalent6() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, false);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEqualToNull() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, false);
        assertFalse(a.equals(null));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test public void notEqualToRandom() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, false);
        assertFalse(a.equals("Some random object"));
    }

    @Nested
    class InterpolationTests {
        @Test
        public void interpolateComponentWithAbsoluteAndPercentageMismatch() {
            record TestCase(BackgroundSize endValue, BackgroundSize expected) {}

            final double v0 = 0, v25 = 10, v50 = 20, v100 = 40;
            final var startValue = new BackgroundSize(v0, v0, false, false, false, false);

            // For each component: interpolation with t=0.25 returns start value on absolute/percentage mismatch.
            for (var testCase : new TestCase[] {
                new TestCase(
                    new BackgroundSize(v100, v100, false, false, false, false),
                    new BackgroundSize(v25, v25, false, false, false, false)),
                new TestCase(
                    new BackgroundSize(v100, v100, true, false, false, false),
                    new BackgroundSize(v0, v25, false, false, false, false)),
                new TestCase(
                    new BackgroundSize(v100, v100, false, true, false, false),
                    new BackgroundSize(v25, v0, false, false, false, false))
            }) {
                assertEquals(testCase.expected, startValue.interpolate(testCase.endValue, 0.25));
            }

            // For each component: interpolation with t=0.5 returns end value on absolute/percentage mismatch.
            for (var testCase : new TestCase[] {
                new TestCase(
                    new BackgroundSize(v100, v100, false, false, false, false),
                    new BackgroundSize(v50, v50, false, false, false, false)),
                new TestCase(
                    new BackgroundSize(v100, v100, true, false, false, false),
                    new BackgroundSize(v100, v50, true, false, false, false)),
                new TestCase(
                    new BackgroundSize(v100, v100, false, true, false, false),
                    new BackgroundSize(v50, v100, false, true, false, false))
            }) {
                assertEquals(testCase.expected, startValue.interpolate(testCase.endValue, 0.5));
            }
        }

        @Test
        public void interpolateBetweenDifferentValuesReturnsNewInstance() {
            var startValue = new BackgroundSize(10, 20, false, false, false, false);
            var endValue = new BackgroundSize(20, 40, false, false, false, false);
            var expect = new BackgroundSize(15, 30, false, false, false, false);
            assertEquals(expect, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolateBetweenEqualValuesReturnsStartInstance() {
            var startValue = new BackgroundSize(10, 20, false, false, false, false);
            var endValue = new BackgroundSize(10, 20, false, false, false, false);
            assertSame(startValue, startValue.interpolate(endValue, 0.25));
            assertSame(startValue, startValue.interpolate(endValue, 0.5));
            assertSame(startValue, startValue.interpolate(endValue, 0.75));
        }

        @Test
        public void interpolationFactorSmallerThanOrEqualToZeroReturnsStartInstance() {
            var startValue = new BackgroundSize(10, 20, false, false, false, false);
            var endValue = new BackgroundSize(20, 40, false, false, false, false);
            assertSame(startValue, startValue.interpolate(endValue, 0));
            assertSame(startValue, startValue.interpolate(endValue, -0.5));
        }

        @Test
        public void interpolationFactorGreaterThanOrEqualToOneReturnsEndInstance() {
            var startValue = new BackgroundSize(10, 20, false, false, false, false);
            var endValue = new BackgroundSize(20, 40, false, false, false, false);
            assertSame(endValue, startValue.interpolate(endValue, 1));
            assertSame(endValue, startValue.interpolate(endValue, 1.5));
        }

        @Test
        public void widthOrHeightLessThanZeroCannotBeInterpolated() {
            var startValue = new BackgroundSize(10, 20, false, false, false, false);
            var endValue = new BackgroundSize(AUTO, 40, false, false, false, false);
            var expect = new BackgroundSize(AUTO, 30, false, false, false, false);
            assertEquals(expect, startValue.interpolate(endValue, 0.5));

            startValue = new BackgroundSize(20, 10, false, false, false, false);
            endValue = new BackgroundSize(40, AUTO, false, false, false, false);
            expect = new BackgroundSize(30, AUTO, false, false, false, false);
            assertEquals(expect, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void notInterpolatableReturnsStartOrEndInstance() {
            var startValue = new BackgroundSize(10, 10, false, false, false, true);
            var endValue = new BackgroundSize(20, 20, false, false, false, false);
            assertSame(startValue, startValue.interpolate(endValue, 0.25));
            assertSame(endValue, startValue.interpolate(endValue, 0.5));

            startValue = new BackgroundSize(10, 10, false, false, false, false);
            endValue = new BackgroundSize(20, 20, false, false, true, false);
            assertSame(startValue, startValue.interpolate(endValue, 0.25));
            assertSame(endValue, startValue.interpolate(endValue, 0.5));

            startValue = new BackgroundSize(AUTO, AUTO, false, false, false, false);
            endValue = new BackgroundSize(20, 20, false, false, false, false);
            assertSame(startValue, startValue.interpolate(endValue, 0.25));
            assertSame(endValue, startValue.interpolate(endValue, 0.5));
        }
    }
}

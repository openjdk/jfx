/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.layout.region;

import com.sun.javafx.scene.layout.region.Margins;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MarginsTest {

    @Test
    void testEquals() {
        var a = new Margins(1, 2, 3, 4, false);
        var b = new Margins(1, 2, 3, 4, false);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));

        a = new Margins(1, 2, 3, 4, false);
        b = new Margins(1, 2, 3, 4, true);
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));

        a = new Margins(1, 2, 3, 4, false);
        b = new Margins(5, 2, 3, 4, false);
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));

        a = new Margins(1, 2, 3, 4, false);
        b = new Margins(1, 5, 3, 4, false);
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));

        a = new Margins(1, 2, 3, 4, false);
        b = new Margins(1, 2, 5, 4, false);
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));

        a = new Margins(1, 2, 3, 4, false);
        b = new Margins(1, 2, 3, 5, false);
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
    }

    @Test
    void testHashCode() {
        var a = new Margins(1, 2, 3, 4, false);
        var b = new Margins(1, 2, 3, 4, false);
        assertEquals(a.hashCode(), b.hashCode());

        a = new Margins(1, 2, 3, 4, false);
        b = new Margins(5, 2, 3, 4, false);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Nested
    class InterpolationTest {
        @Test
        public void interpolateBetweenTwoDifferentValuesReturnsNewInstance() {
            var startValue = new Margins(2, 4, 6, 8, false);
            var endValue = new Margins(4, 8, 12, 16, false);
            var expected = new Margins(3, 6, 9, 12, false);
            assertEquals(expected, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolateBetweenProportionalAndNonProportionalValuesReturnsStartOrEndInstance() {
            var startValue = new Margins(2, 4, 6, 8, true);
            var endValue = new Margins(4, 8, 12, 16, false);
            assertEquals(startValue, startValue.interpolate(endValue, 0.4));
            assertEquals(endValue, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolateBetweenTwoEqualValuesReturnsStartInstance() {
            var startValue = new Margins(2, 4, 6, 8, false);
            var endValue = new Margins(2, 4, 6, 8, false);
            assertSame(startValue, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolationFactorSmallerThanOrEqualToZeroReturnsStartInstance() {
            var startValue = new Margins(2, 4, 6, 8, false);
            var endValue = new Margins(4, 8, 12, 16, false);
            assertSame(startValue, startValue.interpolate(endValue, 0));
            assertSame(startValue, startValue.interpolate(endValue, -1));
        }

        @Test
        public void interpolationFactorGreaterThanOrEqualToOneReturnsEndInstance() {
            var startValue = new Margins(2, 4, 6, 8, false);
            var endValue = new Margins(4, 8, 12, 16, false);
            assertSame(endValue, startValue.interpolate(endValue, 1));
            assertSame(endValue, startValue.interpolate(endValue, 1.5));
        }
    }
}

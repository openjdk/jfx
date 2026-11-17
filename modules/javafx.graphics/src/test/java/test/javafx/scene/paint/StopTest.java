/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.paint;

import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static javafx.scene.paint.Color.*;
import static org.junit.jupiter.api.Assertions.*;

public class StopTest {

    @Test
    public void testStop() {
        Color color = Color.rgb(0xAA, 0xBB, 0xCC);
        Stop stop = new Stop(0.5f, color);

        assertEquals(color.getRed(), stop.getColor().getRed(), 0.0001);
        assertEquals(color.getGreen(), stop.getColor().getGreen(), 0.0001);
        assertEquals(color.getBlue(), stop.getColor().getBlue(), 0.0001);
        assertEquals(0.5f, stop.getOffset(), 0.0001);
    }

    @Test
    public void testNullColorIsTransparent() {
        var stop = new Stop(0.2f, null);
        assertEquals(TRANSPARENT, stop.getColor());
    }

    @Test
    public void testEquals() {
        Color color1 = Color.rgb(0xAA, 0xBB, 0xCC);
        Color color2 = Color.rgb(0, 0, 0);

        Stop basic = new Stop(0.2f, color1);
        Stop equal = new Stop(0.2f, color1);
        Stop diffColor = new Stop(0.2f, color2);
        Stop diffOffset = new Stop(0.4f, color1);

        assertFalse(basic.equals(null));
        assertFalse(basic.equals(new Object()));
        assertTrue(basic.equals(basic));
        assertTrue(basic.equals(equal));
        assertFalse(basic.equals(diffColor));
        assertFalse(basic.equals(diffOffset));
    }

    @Test
    public void testHashCode() {
        Color color1 = Color.rgb(0xAA, 0xBB, 0xCC);
        Color color2 = Color.rgb(0xAA, 0xBB, 0xCC);
        Color color3 = Color.rgb(0, 0, 0);

        Stop basic = new Stop(0.2f, color1);
        Stop equal = new Stop(0.2f, color2);
        Stop different1 = new Stop(0.4f, color1);
        Stop different2 = new Stop(0.2f, color3);

        int code = basic.hashCode();
        int second = basic.hashCode();
        assertTrue(code == second);
        assertTrue(code == equal.hashCode());
        assertFalse(code == different1.hashCode());
        assertFalse(code == different2.hashCode());
    }

    @Test
    public void testToString() {
        Stop empty = new Stop(0, Color.TRANSPARENT);
        Stop nonempty = new Stop(0.5f, Color.rgb(0xAA, 0xBB, 0xCC, 0.5f));

        String s = empty.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());

        s = nonempty.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Nested
    class InterpolationTest {
        @Test
        public void interpolateBetweenTwoDifferentValuesReturnsNewInstance() {
            var startValue = new Stop(0, RED);
            var endValue = new Stop(1, GREEN);
            assertEquals(new Stop(0.5, RED.interpolate(GREEN, 0.5)), startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolateBetweenTwoEqualValuesReturnsSameInstance() {
            var startValue = new Stop(0.25, RED);
            var endValue = new Stop(0.25, RED);
            assertSame(startValue, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolationFactorZeroReturnsStartInstance() {
            var startValue = new Stop(0.25, RED);
            var endValue = new Stop(0.75, GREEN);
            assertSame(startValue, startValue.interpolate(endValue, 0));
        }

        @Test
        public void interpolationFactorOneReturnsEndInstance() {
            var startValue = new Stop(0.25, RED);
            var endValue = new Stop(0.75, GREEN);
            assertSame(endValue, startValue.interpolate(endValue, 1));
        }

        @Test
        public void interpolationFactorLessThanZero() {
            var startValue = new Stop(0.25, new Color(0.5, 0.5, 0.5, 1));
            var endValue = new Stop(0.75, new Color(1, 1, 1, 1));
            assertEquals(new Stop(0, new Color(0, 0, 0, 1)), startValue.interpolate(endValue, -1));
            assertEquals(new Stop(0, new Color(0, 0, 0, 1)), startValue.interpolate(endValue, -2));
        }

        @Test
        public void interpolationFactorGreaterThanOne() {
            var startValue = new Stop(0.25, new Color(0, 0, 0, 1));
            var endValue = new Stop(0.75, new Color(0.5, 0.5, 0.5, 1));
            assertEquals(new Stop(1, new Color(1, 1, 1, 1)), startValue.interpolate(endValue, 2));
            assertEquals(new Stop(1, new Color(1, 1, 1, 1)), startValue.interpolate(endValue, 3));
        }
    }
}

/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.Scene;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LinearGradientTest {

    private final Color color1 = Color.rgb(0, 0, 0);
    private final Color color2 = Color.rgb(255, 255, 255);
    private final Stop stop1 = new Stop(0.1, color1);
    private final Stop stop2 = new Stop(0.2, color2);
    private final Stop[] noStop = new Stop[0];
    private final Stop[] oneStop = new Stop[] { stop1 };
    private final Stop[] twoStops = new Stop[] { stop1, stop2 };
    private final Stop[] twoStopsWithNulls =
            new Stop[] { stop1, null, stop2, null };
    private final List<Stop> normalizedTwoStops = Arrays.asList(
        new Stop(0.0, color1),
        stop1, stop2,
        new Stop(1.0, color2)
    );

    @Test
    public void testLinearGradient() {
        LinearGradient gradient = new LinearGradient(1f, 2f, 3f, 4f, true,
                CycleMethod.REPEAT, twoStops);

        assertEquals(1f, gradient.getStartX(), 0.0001);
        assertEquals(2f, gradient.getStartY(), 0.0001);
        assertEquals(3f, gradient.getEndX(), 0.0001);
        assertEquals(4f, gradient.getEndY(), 0.0001);
        assertTrue(gradient.isProportional());
        assertEquals(CycleMethod.REPEAT, gradient.getCycleMethod());
        assertEquals(normalizedTwoStops, gradient.getStops());
    }

    @Test
    public void testGetStopsNullsRemoved() {
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true,
                CycleMethod.NO_CYCLE, twoStopsWithNulls);

        assertEquals(normalizedTwoStops, gradient.getStops());
    }

    @Test
    public void testGetStopsCannotChangeGradient() {
        assertThrows(UnsupportedOperationException.class, () -> {
            LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true,
                    CycleMethod.NO_CYCLE, twoStopsWithNulls);

            List<Stop> returned = gradient.getStops();
            returned.set(0, stop2);
        });
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEquals() {
        LinearGradient basic = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient equal = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStopsWithNulls);
        LinearGradient beginX = new LinearGradient(2, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient beginY = new LinearGradient(1, 1, 3, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient endX = new LinearGradient(1, 2, 4, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient endY = new LinearGradient(1, 2, 3, 3, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient proportional = new LinearGradient(1, 2, 3, 4, false,
                CycleMethod.REPEAT, twoStops);
        LinearGradient cycleMethod = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REFLECT, twoStops);
        LinearGradient stops = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, oneStop);

        assertFalse(basic.equals(null));
        assertFalse(basic.equals(color1));
        assertFalse(basic.equals(color2));
        assertTrue(basic.equals(basic));
        assertTrue(basic.equals(equal));
        assertFalse(basic.equals(beginX));
        assertFalse(basic.equals(beginY));
        assertFalse(basic.equals(endX));
        assertFalse(basic.equals(endY));
        assertFalse(basic.equals(proportional));
        assertFalse(basic.equals(cycleMethod));
        assertFalse(basic.equals(stops));
    }

    @Test
    public void testHashCode() {
        LinearGradient basic = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient equal = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStops);
        LinearGradient different = new LinearGradient(1, 2, 3, 4, false,
                CycleMethod.REPEAT, twoStops);
        LinearGradient different2 = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, oneStop);

        int code = basic.hashCode();
        int second = basic.hashCode();
        assertTrue(code == second);
        assertTrue(code == equal.hashCode());
        assertFalse(code == different.hashCode());
        assertFalse(code == different2.hashCode());
    }

    @Test
    public void testToString() {
        LinearGradient empty = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, noStop);
        LinearGradient nonempty = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, twoStops);

        String s = empty.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());

        s = nonempty.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test
    public void testToStringEquals() {
        LinearGradient lg =
                LinearGradient.valueOf("linear-gradient(from 0% 0% to 100% 100%, red  0% , blue 30%,  black 100%)");
        assertEquals(lg, LinearGradient.valueOf(lg.toString()));

        lg = LinearGradient.valueOf("linear-gradient(from 100px 0 to 200px 0px, red 0px, blue 50px,  black 100px)");
        assertEquals(lg, LinearGradient.valueOf(lg.toString()));

        lg = LinearGradient.valueOf("linear-gradient(to top, red  0%, blue  30%,black 100%)");
        assertEquals(lg, LinearGradient.valueOf(lg.toString()));

        lg = LinearGradient.valueOf("linear-gradient(from 10% 20% to 30% 40%, ff00ff 0%, 0xffffff 30%,black 100%)");
        assertEquals(lg, LinearGradient.valueOf(lg.toString()));
    }

    @Test
    public void testImpl_getPlatformPaint() {
        LinearGradient gradient = new LinearGradient(1, 2, 3, 4, true,
                CycleMethod.REPEAT, noStop);

        Object paint = Toolkit.getPaintAccessor().getPlatformPaint(gradient);
        assertNotNull(paint);
        assertSame(paint, Toolkit.getPaintAccessor().getPlatformPaint(gradient));
    }

    @Test
    public void testValueOfNullValue() {
        assertThrows(NullPointerException.class, () -> {
            LinearGradient.valueOf(null);
        });
    }

    @Test
    public void testValueOfEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            LinearGradient.valueOf("");
        });
    }

    @Test
    public void testValueOfIllegalNotations() {
        try {
            LinearGradient.valueOf("abcd");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            LinearGradient.valueOf("linear-gradient()");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // rgb( must end with ')'
        try {
            LinearGradient.valueOf("linear-gradient(hsl(240,100%,100%), rgb(250, 0, 0)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // stop can't be empty
        try {
            LinearGradient.valueOf("linear-gradient(from 0 100 to 100 100, red  0% ,,   black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            LinearGradient.valueOf("linear-gradient(from 0 100 to 100 100, red  0% ,black 100%,   )");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

       // token can't be empty
        try {
            LinearGradient.valueOf("linear-gradient(, from 0 100 to 100 100, red  0% ,   black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            LinearGradient.valueOf("linear-gradient(from 0 100 to 100 100, ,repeat, red  0% ,   black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

    }

    @Test
    public void testValueOfRelativeAbsoluteMixed() {
        try {
            LinearGradient.valueOf("linear-gradient(from 0px 100% to 100% 100%, red  0% , blue 30%,  black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            LinearGradient.valueOf("linear-gradient(from 0% 100% to 100px 100%, red  0% , blue 30%,  black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            LinearGradient.valueOf("linear-gradient(from 0% 100% to 100% 100%, red  0% , blue 30px,  black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            LinearGradient.valueOf("linear-gradient(from 0 100 to 100 100%, red  0% , blue 30px,  black 100%)");
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

    }

    @Test
    public void testValueOfCycleMethod() {
        LinearGradient actual = LinearGradient.valueOf("linear-gradient(repeat, red  0%, blue  30%,black 100%)");
        LinearGradient expected = new LinearGradient(0, 0, 0, 1,
                true, CycleMethod.REPEAT,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);

        actual = LinearGradient.valueOf("linear-gradient(reflect, red  0%, blue 30%, black 100%)");
        expected = new LinearGradient(0, 0, 0, 1,
                true, CycleMethod.REFLECT,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfFromTo() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(from 10% 20% to 30% 40%, red  0%, blue  30%,black 100%)");
        LinearGradient expected = new LinearGradient(0.1, 0.2, 0.3, 0.4,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);

        actual =
                LinearGradient.valueOf("linear-gradient(from 10px 20px to 30px 40px, red  0%, blue 30%, black 100%)");
        expected = new LinearGradient(10, 20, 30, 40,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfTo() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(to top, red  0%, blue  30%,black 100%)");
        LinearGradient expected = new LinearGradient(0, 1, 0, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);

        actual =
                LinearGradient.valueOf("linear-gradient(to bottom, red  0%, blue  30%,black 100%)");
        expected = new LinearGradient(0, 0, 0, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);

        actual =
                LinearGradient.valueOf("linear-gradient(to left, red  0%, blue  30%,black 100%)");
        expected = new LinearGradient(1, 0, 0, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);

        actual =
                LinearGradient.valueOf("linear-gradient(to right, red  0%, blue  30%,black 100%)");
        expected = new LinearGradient(0, 0, 1, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);

        actual =
                LinearGradient.valueOf("linear-gradient(to bottom left, red  0%, blue  30%,black 100%)");
        expected = new LinearGradient(1, 0, 0, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);

        actual =
                LinearGradient.valueOf("linear-gradient(to right top, red  0%, blue  30%,black 100%)");
        expected = new LinearGradient(0, 1, 1, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfRelativeValues() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(from 0% 0% to 100% 100%, red  0% , blue 30%,  black 100%)");
        LinearGradient expected = new LinearGradient(0, 0, 1, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfAbsoluteValues() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(from 100px 0 to 200px 0px, red 0px, blue 50px,  black 100px)");
        LinearGradient expected = new LinearGradient(100, 0, 200, 0,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.5, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfColor() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(rgb(0,0,255), rgb(255, 0, 0))");
        LinearGradient expected = new LinearGradient(0, 0, 0, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.BLUE),
                new Stop(1.0, Color.RED));
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfDefaultsToBottom() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(red  0%, blue 30%, black 100%)");
        LinearGradient expected = new LinearGradient(0, 0, 0, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.3, Color.BLUE),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);
    }

    @Test
    public void testValueOfStopsNormalizeLargerValues() {
        LinearGradient actual =
                LinearGradient.valueOf("linear-gradient(red  10%, blue 9%, red 8%, black 100%)");
        LinearGradient expected = new LinearGradient(0, 0, 0, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0.1, Color.RED),
                new Stop(0.1, Color.BLUE),
                new Stop(0.1, Color.RED),
                new Stop(1.0, Color.BLACK));
        assertEquals(expected, actual);
    }

    @Test
    public void testCycleMethodCSSStyle() {
        Region region = new Region();
        Scene scene = new Scene(region);
        LinearGradient lGradient;

        region.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 10% 10%,"
            + " reflect, red 30%, black 70%);");
        region.applyCss();
        lGradient = (LinearGradient) region.backgroundProperty().get().getFills().get(0).getFill();
        assertEquals(CycleMethod.REFLECT, lGradient.getCycleMethod());

        region.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 10% 10%,"
            + " repeat, red 30%, black 70%);");
        region.applyCss();
        lGradient = (LinearGradient) region.backgroundProperty().get().getFills().get(0).getFill();
        assertEquals(CycleMethod.REPEAT, lGradient.getCycleMethod());
    }

    @Nested
    class InterpolationTest {
        @Test
        public void interpolateBetweenDifferentValuesReturnsNewInstance() {
            var startValue = new LinearGradient(
                10, 20, 30, 40,
                true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.BLACK), new Stop(1, Color.WHITE)));

            var endValue = new LinearGradient(
                20, 40, 60, 80,
                true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.WHITE), new Stop(1, Color.BLACK)));

            var expected = new LinearGradient(
                15, 30, 45, 60,
                true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.gray(0.5)), new Stop(1, Color.gray(0.5))));

            assertEquals(expected, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolateBetweenProportionalAndNonProportionalReturnsStartValuesOrEndValues() {
            var startValue = new LinearGradient(
                10, 20, 30, 40,
                true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.BLUE)));

            var endValue = new LinearGradient(
                10, 20, 30, 40,
                false, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.BLUE)));

            assertSame(startValue, startValue.interpolate(endValue, 0.25));
            assertSame(endValue, startValue.interpolate(endValue, 0.5));
            assertSame(endValue, startValue.interpolate(endValue, 0.75));
        }

        @Test
        public void interpolateBetweenTwoEqualValuesReturnsStartInstance() {
            var startValue = new LinearGradient(
                10, 20, 30, 40,
                true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.BLUE)));

            var endValue = new LinearGradient(
                10, 20, 30, 40,
                true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.BLUE)));

            assertSame(startValue, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolationFactorSmallerThanOrEqualToZeroReturnsStartInstance() {
            var startValue = new LinearGradient(
                10, 20, 30, 40,
                true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.BLUE)));

            var endValue = new LinearGradient(
                20, 40, 60, 80,
                true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.RED)));

            assertSame(startValue, startValue.interpolate(endValue, 0));
            assertSame(startValue, startValue.interpolate(endValue, -1));
        }

        @Test
        public void interpolationFactorGreaterThanOrEqualToOneReturnsEndInstance() {
            var startValue = new LinearGradient(
                10, 20, 30, 40,
                true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.BLUE)));

            var endValue = new LinearGradient(
                20, 40, 60, 80,
                true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0, Color.RED)));

            assertSame(endValue, startValue.interpolate(endValue, 1));
            assertSame(endValue, startValue.interpolate(endValue, 1.5));
        }
    }
}

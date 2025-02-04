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

package test.com.sun.javafx.util;

import com.sun.javafx.util.InterpolationUtils;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.List;

import static javafx.scene.paint.Color.*;
import static org.junit.jupiter.api.Assertions.*;

public class InterpolationUtilsTest {

    @Nested
    class PairwiseListInterpolationTest {
        @Test
        void differentValuesReturnsNewInstance() {
            var startValue = List.<Paint>of(RED, BLUE);
            var endValue = List.<Paint>of(GREEN, YELLOW);
            var expected = List.of(RED.interpolate(GREEN, 0.5), BLUE.interpolate(YELLOW, 0.5));
            var actual = InterpolationUtils.interpolateListsPairwise(startValue, endValue, 0.5);
            assertEquals(expected, actual);
            assertNotSame(expected, startValue);
            assertNotSame(expected, endValue);
            assertThrows(UnsupportedOperationException.class, () -> actual.add(RED));
        }

        @Test
        void sameValuesReturnsStartInstance() {
            var startValue = List.<Paint>of(RED, BLUE);
            var endValue = List.<Paint>of(RED, BLUE);
            var actual = InterpolationUtils.interpolateListsPairwise(startValue, endValue, 0.5);
            assertSame(startValue, actual);
        }

        @Test
        void secondListHasMoreElements() {
            var startValue = List.<Paint>of(RED, BLUE);
            var endValue = List.<Paint>of(GREEN, YELLOW, PURPLE);
            var expected = List.of(RED.interpolate(GREEN, 0.5), BLUE.interpolate(YELLOW, 0.5), PURPLE);
            var actual = InterpolationUtils.interpolateListsPairwise(startValue, endValue, 0.5);
            assertEquals(expected, actual);
        }

        @Test
        void secondListHasLessElements() {
            var startValue = List.<Paint>of(RED, BLUE);
            var endValue = List.<Paint>of(GREEN);
            var expected = List.of(RED.interpolate(GREEN, 0.5));
            var actual = InterpolationUtils.interpolateListsPairwise(startValue, endValue, 0.5);
            assertEquals(expected, actual);
        }
    }

    @Nested
    class PairwiseArrayInterpolationTest {
        @Test
        void differentValuesReturnsNewInstance() {
            var startValue = arrayOf(RED, BLUE);
            var endValue = arrayOf(GREEN, YELLOW);
            var expected = arrayOf(RED.interpolate(GREEN, 0.5), BLUE.interpolate(YELLOW, 0.5));
            var actual = InterpolationUtils.interpolateArraysPairwise(startValue, endValue, 0.5);
            assertArrayEquals(expected, actual);
            assertNotSame(expected, startValue);
            assertNotSame(expected, endValue);
        }

        @Test
        void sameValuesReturnsStartInstance() {
            var startValue = arrayOf(RED, BLUE);
            var endValue = arrayOf(RED, BLUE);
            var actual = InterpolationUtils.interpolateArraysPairwise(startValue, endValue, 0.5);
            assertSame(startValue, actual);
        }

        @Test
        void secondArrayHasMoreElements() {
            var startValue = arrayOf(RED, BLUE);
            var endValue = arrayOf(GREEN, YELLOW, PURPLE);
            var expected = arrayOf(RED.interpolate(GREEN, 0.5), BLUE.interpolate(YELLOW, 0.5), PURPLE);
            var actual = InterpolationUtils.interpolateArraysPairwise(startValue, endValue, 0.5);
            assertArrayEquals(expected, actual);
        }

        @Test
        void secondArrayHasLessElements() {
            var startValue = arrayOf(RED, BLUE);
            var endValue = arrayOf(GREEN);
            var expected = arrayOf(RED.interpolate(GREEN, 0.5));
            var actual = InterpolationUtils.interpolateArraysPairwise(startValue, endValue, 0.5);
            assertArrayEquals(expected, actual);
        }
    }

    @Nested
    class InterpolateColorTest {
        @Test
        void toLinearGradient() {
            var paint = InterpolationUtils.interpolatePaint(
                BLUE,
                new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, RED), new Stop(1, GREEN)),
                0.5);

            var expected = new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                              new Stop(0, BLUE.interpolate(RED, 0.5)),
                                              new Stop(1, BLUE.interpolate(GREEN, 0.5)));

            assertEquals(expected, paint);
        }

        @Test
        void toRadialGradient() {
            var paint = InterpolationUtils.interpolatePaint(
                BLUE,
                new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, RED), new Stop(1, GREEN)),
                0.5);

            var expected = new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                              new Stop(0, BLUE.interpolate(RED, 0.5)),
                                              new Stop(1, BLUE.interpolate(GREEN, 0.5)));

            assertEquals(expected, paint);
        }
    }

    @Nested
    class InterpolateLinearGradientTest {
        @Test
        void toColor() {
            var paint = InterpolationUtils.interpolatePaint(
                new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, RED), new Stop(1, GREEN)),
                BLUE,
                0.5);

            var expected = new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                              new Stop(0, RED.interpolate(BLUE, 0.5)),
                                              new Stop(1, GREEN.interpolate(BLUE, 0.5)));

            assertEquals(expected, paint);
        }

        @Test
        void toRadialGradient() {
            var toPaint = new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                             new Stop(0, RED), new Stop(1, GREEN));

            var actual = InterpolationUtils.interpolatePaint(
                new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, RED), new Stop(1, GREEN)),
                toPaint,
                0.5);

            assertSame(actual, toPaint);
        }
    }

    @Nested
    class InterpolateRadialGradientTest {
        @Test
        void toColor() {
            var paint = InterpolationUtils.interpolatePaint(
                new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, RED), new Stop(1, GREEN)),
                BLUE,
                0.5);

            var expected = new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                              new Stop(0, RED.interpolate(BLUE, 0.5)),
                                              new Stop(1, GREEN.interpolate(BLUE, 0.5)));

            assertEquals(expected, paint);
        }

        @Test
        void toLinearGradient() {
            var toPaint = new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                             new Stop(0, RED), new Stop(1, GREEN));

            var actual = InterpolationUtils.interpolatePaint(
                new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, RED), new Stop(1, GREEN)),
                toPaint,
                0.5);

            assertSame(actual, toPaint);
        }
    }

    @SafeVarargs
    private static <T> T[] arrayOf(T... values) {
        return values;
    }
}

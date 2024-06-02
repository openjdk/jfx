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

package test.javafx.scene.paint;

import com.sun.javafx.scene.paint.PaintUtils;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PaintUtilsTest {

    @Nested
    class InterpolateColorTest {
        @Test
        void toLinearGradient() {
            var paint = PaintUtils.interpolate(
                Color.BLUE,
                new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, Color.RED), new Stop(1, Color.GREEN)),
                0.5);

            var expected = new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                              new Stop(0, Color.BLUE.interpolate(Color.RED, 0.5)),
                                              new Stop(1, Color.BLUE.interpolate(Color.GREEN, 0.5)));

            assertEquals(expected, paint);
        }

        @Test
        void toRadialGradient() {
            var paint = PaintUtils.interpolate(
                Color.BLUE,
                new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, Color.RED), new Stop(1, Color.GREEN)),
                0.5);

            var expected = new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                              new Stop(0, Color.BLUE.interpolate(Color.RED, 0.5)),
                                              new Stop(1, Color.BLUE.interpolate(Color.GREEN, 0.5)));

            assertEquals(expected, paint);
        }
    }

    @Nested
    class InterpolateLinearGradientTest {
        @Test
        void toColor() {
            var paint = PaintUtils.interpolate(
                new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, Color.RED), new Stop(1, Color.GREEN)),
                Color.BLUE,
                0.5);

            var expected = new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                              new Stop(0, Color.RED.interpolate(Color.BLUE, 0.5)),
                                              new Stop(1, Color.GREEN.interpolate(Color.BLUE, 0.5)));

            assertEquals(expected, paint);
        }

        @Test
        void toRadialGradient() {
            var toPaint = new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                             new Stop(0, Color.RED), new Stop(1, Color.GREEN));

            var actual = PaintUtils.interpolate(
                new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, Color.RED), new Stop(1, Color.GREEN)),
                toPaint,
                0.5);

            assertSame(actual, toPaint);
        }
    }

    @Nested
    class InterpolateRadialGradientTest {
        @Test
        void toColor() {
            var paint = PaintUtils.interpolate(
                new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, Color.RED), new Stop(1, Color.GREEN)),
                Color.BLUE,
                0.5);

            var expected = new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                              new Stop(0, Color.RED.interpolate(Color.BLUE, 0.5)),
                                              new Stop(1, Color.GREEN.interpolate(Color.BLUE, 0.5)));

            assertEquals(expected, paint);
        }

        @Test
        void toLinearGradient() {
            var toPaint = new LinearGradient(0, 0, 1, 1, false, CycleMethod.NO_CYCLE,
                                             new Stop(0, Color.RED), new Stop(1, Color.GREEN));

            var actual = PaintUtils.interpolate(
                new RadialGradient(0, 0, 0, 0, 10, false, CycleMethod.NO_CYCLE,
                                   new Stop(0, Color.RED), new Stop(1, Color.GREEN)),
                toPaint,
                0.5);

            assertSame(actual, toPaint);
        }
    }
}

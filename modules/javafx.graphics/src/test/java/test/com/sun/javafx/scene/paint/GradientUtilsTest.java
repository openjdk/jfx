/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.paint;

import com.sun.javafx.scene.paint.GradientUtils;
import javafx.geometry.BoundingBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GradientUtilsTest {

    private static final double EPS = 1e-6;

    @Nested
    class LinearGradientSamplerTest {

        @Test
        void noCycle_proportional_samplesAndClamps() {
            var b = new BoundingBox(0, 0, 100, 100);
            var g = new LinearGradient(
                0, 0, 1, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(1, Color.BLUE));

            var sampler = GradientUtils.newLinearGradientSampler(g, b);

            assertClose(Color.RED, sampler.sample(0, 50));
            assertClose(expectedInterpolationSRGB(Color.RED, Color.BLUE, 0.5), sampler.sample(50, 50));
            assertClose(Color.BLUE, sampler.sample(100, 50));

            // Outside range should clamp for NO_CYCLE
            assertClose(Color.RED, sampler.sample(-100, 50));
            assertClose(Color.BLUE, sampler.sample(1000, 50));
        }

        @Test
        void repeatCycle_wraps() {
            var b = new BoundingBox(0, 0, 100, 100);
            var g = new LinearGradient(
                0, 0, 1, 0,
                true, CycleMethod.REPEAT,
                new Stop(0, Color.RED),
                new Stop(1, Color.BLUE));

            var sampler = GradientUtils.newLinearGradientSampler(g, b);

            // x=150 -> t=1.5 -> REPEAT -> 0.5
            assertClose(expectedInterpolationSRGB(Color.RED, Color.BLUE, 0.5), sampler.sample(150, 50));

            // x=-50 -> t=-0.5 -> REPEAT -> 0.5
            assertClose(expectedInterpolationSRGB(Color.RED, Color.BLUE, 0.5), sampler.sample(-50, 50));
        }

        @Test
        void reflectCycle_reflects() {
            var b = new BoundingBox(0, 0, 100, 100);
            var g = new LinearGradient(
                0, 0, 1, 0,
                true, CycleMethod.REFLECT,
                new Stop(0, Color.RED),
                new Stop(1, Color.BLUE));

            var sampler = GradientUtils.newLinearGradientSampler(g, b);

            // x=150 -> t=1.5 -> REFLECT -> 0.5
            assertClose(expectedInterpolationSRGB(Color.RED, Color.BLUE, 0.5), sampler.sample(150, 50));

            // x=-50 -> t=-0.5 -> REFLECT -> 0.5
            assertClose(expectedInterpolationSRGB(Color.RED, Color.BLUE, 0.5), sampler.sample(-50, 50));

            // x=250 -> t=2.5 -> 2.5 % 2 = 0.5 -> REFLECT -> 0.5
            assertClose(expectedInterpolationSRGB(Color.RED, Color.BLUE, 0.5), sampler.sample(250, 50));
        }

        @Test
        void degenerateStartEnd_returnsFirstStop() {
            var b = new BoundingBox(0, 0, 100, 100);
            var g = new LinearGradient(
                0, 0, 0, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(1, Color.BLUE));

            var sampler = GradientUtils.newLinearGradientSampler(g, b);
            assertClose(Color.RED, sampler.sample(50, 50));
        }

        @Test
        void proportional_usesBoundsOrigin() {
            var b = new BoundingBox(10, 0, 100, 100);
            var g = new LinearGradient(
                0, 0, 1, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(1, Color.BLUE));

            var sampler = GradientUtils.newLinearGradientSampler(g, b);

            // With minX=10: x0=10, x1=110, so at x=60 => t=(60-10)/100=0.5
            assertClose(expectedInterpolationSRGB(Color.RED, Color.BLUE, 0.5), sampler.sample(60, 50));
        }

        @Test
        void premultipliedStopInterpolation_preservesHueWhenFadingToTransparent() {
            var b = new BoundingBox(0, 0, 100, 100);
            var g = new LinearGradient(
                0, 0, 1, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0, 0, 1)),  // opaque red
                new Stop(1, Color.color(1, 0, 0, 0))); // transparent red

            var sampler = GradientUtils.newLinearGradientSampler(g, b);
            var mid = sampler.sample(50, 0);

            assertEquals(1.0, mid.getRed(), EPS, "red");
            assertEquals(0.0, mid.getGreen(), EPS, "green");
            assertEquals(0.0, mid.getBlue(), EPS, "blue");
            assertEquals(0.5, mid.getOpacity(), EPS, "opacity");
        }
    }

    @Nested
    class RadialGradientSamplerTest {

        @Test
        void noCycle_basic_centerMidEdge() {
            var b = new BoundingBox(0, 0, 100, 100);
            var g = new RadialGradient(
                0, 0,          // focusAngle, focusDistance
                0.5, 0.5,      // center
                0.5,           // radius
                true,          // proportional
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(1, Color.BLACK));

            var sampler = GradientUtils.newRadialGradientSampler(g, b);

            // Center -> t=0
            assertClose(Color.WHITE, sampler.sample(50, 50));

            // Mid radius (25 of 50) -> t=0.5
            assertClose(expectedInterpolationSRGB(Color.WHITE, Color.BLACK, 0.5), sampler.sample(75, 50));

            // Edge -> t=1
            assertClose(Color.BLACK, sampler.sample(100, 50));

            // Outside -> clamp at 1 for NO_CYCLE
            assertClose(Color.BLACK, sampler.sample(150, 50));
        }

        @Test
        void repeatCycle_wrapsOutsideBackToStart() {
            var b = new BoundingBox(0, 0, 100, 100);
            var g = new RadialGradient(
                0, 0,
                0.5, 0.5,
                0.5,
                true,
                CycleMethod.REPEAT,
                new Stop(0, Color.WHITE),
                new Stop(1, Color.BLACK));

            var sampler = GradientUtils.newRadialGradientSampler(g, b);

            // x=150 gives t=2 (see math), REPEAT -> 0
            assertClose(Color.WHITE, sampler.sample(150, 50));
        }

        @Test
        void reflectCycle_reflectsOutsideToStart() {
            var b = new BoundingBox(0, 0, 100, 100);
            var g = new RadialGradient(
                0, 0,
                0.5, 0.5,
                0.5,
                true,
                CycleMethod.REFLECT,
                new Stop(0, Color.WHITE),
                new Stop(1, Color.BLACK));

            var sampler = GradientUtils.newRadialGradientSampler(g, b);

            // Same outside case gives t=2 -> REFLECT -> 0
            assertClose(Color.WHITE, sampler.sample(150, 50));
        }

        @Test
        void proportional_usesBoundsOriginForCenter() {
            var b = new BoundingBox(10, 20, 100, 100);
            var g = new RadialGradient(
                0, 0,
                0.5, 0.5,
                0.5,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(1, Color.BLACK));

            var sampler = GradientUtils.newRadialGradientSampler(g, b);

            // Center must be (minX + 0.5*width, minY + 0.5*height) = (60, 70)
            assertClose(Color.WHITE, sampler.sample(60, 70));
        }

        @Test
        void radiusZero_returnsFirstStop() {
            var b = new BoundingBox(0, 0, 100, 100);
            var g = new RadialGradient(
                0, 0,
                0.5, 0.5,
                0.0,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.YELLOW),
                new Stop(1, Color.BLACK));

            var sampler = GradientUtils.newRadialGradientSampler(g, b);
            assertClose(Color.YELLOW, sampler.sample(50, 50));
            assertClose(Color.YELLOW, sampler.sample(0, 0));
        }
    }

    private static void assertClose(Color expected, Color actual) {
        assertEquals(expected.getRed(), actual.getRed(), EPS, "red");
        assertEquals(expected.getGreen(), actual.getGreen(), EPS, "green");
        assertEquals(expected.getBlue(), actual.getBlue(), EPS, "blue");
        assertEquals(expected.getOpacity(), actual.getOpacity(), EPS, "opacity");
    }

    private static Color expectedInterpolationSRGB(Color first, Color second, double t) {
        double ao = first.getOpacity(), bo = second.getOpacity();
        double o = ao + (bo - ao) * t;

        // Premultiply in sRGB component space
        double arP = first.getRed() * ao;
        double agP = first.getGreen() * ao;
        double abP = first.getBlue() * ao;
        double brP = second.getRed() * bo;
        double bgP = second.getGreen() * bo;
        double bbP = second.getBlue() * bo;

        // Interpolate premultiplied channels
        double rP = arP + (brP - arP) * t;
        double gP = agP + (bgP - agP) * t;
        double bP = abP + (bbP - abP) * t;

        // Unpremultiply
        double r = rP / o;
        double g = gP / o;
        double b = bP / o;

        return new Color(r, g, b, Math.clamp(o, 0, 1));
    }
}

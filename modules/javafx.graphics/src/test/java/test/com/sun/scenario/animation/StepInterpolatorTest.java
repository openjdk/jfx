/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.util.Utils;
import com.sun.scenario.animation.StepInterpolator;
import org.junit.jupiter.api.Test;

import javafx.animation.StepPosition;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step easing functions as defined by
 * <a href="https://www.w3.org/TR/css-easing-1/#step-easing-functions">CSS Easing Functions Level 1</a>
 */
public class StepInterpolatorTest {

    /*
     *    1 ─┤            ‗‗‗‗‗
     *       │           ⁞
     *       │      ‗‗‗‗‗⁞
     *  0.5 ─┤     ⁞
     *       │‗‗‗‗‗⁞
     *       ⁞
     *    0 ─⁞_________________
     *       0       0.5      1
     */
    @Test
    public void testStart() {
        var interpolator = new StepInterpolatorMock(3, 1D / 3D, StepPosition.START);
        assertRise(interpolator, 0, 0);
        assertRise(interpolator, 1D / 3D, 1D / 3D);
        assertRise(interpolator, 2D / 3D, 2D / 3D);
        assertEquals(1, interpolator.curve(1), 0.001);
    }

    /*
     *    1 ─┤                 ●
     *       │                 ⁞
     *       │            ‗‗‗‗‗⁞
     *  0.5 ─┤           ⁞
     *       │      ‗‗‗‗‗⁞
     *       │     ⁞
     *    0 ─┤‗‗‗‗‗⁞____________
     *       0       0.5       1
     */
    @Test
    public void testEnd() {
        var interpolator = new StepInterpolatorMock(3, 1D / 3D, StepPosition.END);
        assertEquals(0, interpolator.curve(0), 0.001);
        assertRise(interpolator, 0, 1D / 3D);
        assertRise(interpolator, 1D / 3D, 2D / 3D);
        assertRise(interpolator, 2D / 3D, 1);
    }

    /*
     *    1 ─┤            ‗‗‗‗‗
     *       │           ⁞
     *       │           ⁞
     *  0.5 ─┤      ‗‗‗‗‗⁞
     *       │     ⁞
     *       │     ⁞
     *    0 ─┤‗‗‗‗‗⁞___________
     *       0       0.5      1
     */
    @Test
    public void testNone() {
        var interpolator = new StepInterpolatorMock(3, 0.5, StepPosition.NONE);
        assertEquals(0, interpolator.curve(0), 0.001);
        assertRise(interpolator, 0, 1D / 3D);
        assertRise(interpolator, 0.5, 2D / 3D);
        assertEquals(1, interpolator.curve(1), 0.001);
    }

    /*
     *    1 ─┤              ●
     *       │          ‗‗‗‗⁞
     *  0.5 ─┤     ‗‗‗‗⁞
     *       │‗‗‗‗⁞
     *    0 ─⁞_______________
     *       0      0.5     1
     */
    @Test
    public void testBoth() {
        var interpolator = new StepInterpolatorMock(3, 0.25, StepPosition.BOTH);
        assertRise(interpolator, 0, 0);
        assertRise(interpolator, 0.25, 1D / 3D);
        assertRise(interpolator, 0.5, 2D / 3D);
        assertRise(interpolator, 0.75, 1);
    }

    private static void assertRise(StepInterpolatorMock interpolator, double v, double t) {
        assertEquals(v, interpolator.curve(t - 0.001), 0.001);
        assertEquals(v + interpolator.stepSize, interpolator.curve(t), 0.001);
        assertEquals(v + interpolator.stepSize, interpolator.curve(t + 0.001), 0.001);
    }

    private static class StepInterpolatorMock extends StepInterpolator {
        double stepSize;

        StepInterpolatorMock(int intervals, double stepSize, StepPosition position) {
            super(intervals, position);
            this.stepSize = stepSize;
        }

        @Override
        public double curve(double t) {
            if (!isValidBeforeInterval()) {
                return super.curve(Utils.clamp(0, t, 1));
            }

            return super.curve(t);
        }
    }

}

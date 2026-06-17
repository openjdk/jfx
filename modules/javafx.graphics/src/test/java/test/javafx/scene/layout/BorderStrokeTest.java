/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Insets;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static javafx.scene.layout.BorderStrokeStyle.*;
import static javafx.scene.paint.Color.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BorderStroke.
 */
public class BorderStrokeTest {
    // be sure to test the innerEdge and outerEdge, I had bugs there!
    @Test
    public void dummy() { }

    @Nested
    class InterpolationTests {
        @Test
        public void interpolateBetweenDifferentValuesReturnsNewInstance() {
            var startValue = new BorderStroke(RED, SOLID, new CornerRadii(10), new BorderWidths(5), new Insets(2));
            var endValue = new BorderStroke(GREEN, DOTTED, new CornerRadii(20), new BorderWidths(15), new Insets(6));
            var expect = new BorderStroke(RED.interpolate(GREEN, 0.5), DOTTED, new CornerRadii(15), new BorderWidths(10), new Insets(4));
            assertEquals(expect, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolateBetweenEqualValuesReturnsStartInstance() {
            var startValue = new BorderStroke(RED, SOLID, new CornerRadii(10), new BorderWidths(5), new Insets(2));
            var endValue = new BorderStroke(RED, SOLID, new CornerRadii(10), new BorderWidths(5), new Insets(2));
            assertSame(startValue, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolationFactorZeroReturnsStartInstance() {
            var startValue = new BorderStroke(RED, SOLID, new CornerRadii(10), new BorderWidths(5), new Insets(2));
            var endValue = new BorderStroke(GREEN, SOLID, new CornerRadii(20), new BorderWidths(15), new Insets(6));
            assertSame(startValue, startValue.interpolate(endValue, 0));
        }

        @Test
        public void interpolationFactorOneReturnsEndInstance() {
            var startValue = new BorderStroke(RED, SOLID, new CornerRadii(10), new BorderWidths(5), new Insets(2));
            var endValue = new BorderStroke(GREEN, SOLID, new CornerRadii(20), new BorderWidths(15), new Insets(6));
            assertSame(endValue, startValue.interpolate(endValue, 1));
        }

        @Test
        public void interpolationFactorLessThanZero() {
            var startValue = new BorderStroke(new Color(0.5, 0.5, 0.5, 1), SOLID, new CornerRadii(10), new BorderWidths(5), new Insets(2));
            var endValue = new BorderStroke(new Color(1, 1, 1, 1), SOLID, new CornerRadii(20), new BorderWidths(15), new Insets(6));
            assertEquals(new BorderStroke(new Color(0, 0, 0, 1), SOLID, new CornerRadii(0), new BorderWidths(0), new Insets(-2)),
                         startValue.interpolate(endValue, -1));
            assertEquals(new BorderStroke(new Color(0, 0, 0, 1), SOLID, new CornerRadii(0), new BorderWidths(0), new Insets(-6)),
                         startValue.interpolate(endValue, -2));
        }

        @Test
        public void interpolationFactorGreaterThanOne() {
            var startValue = new BorderStroke(new Color(0.5, 0.5, 0.5, 1), SOLID, new CornerRadii(10), new BorderWidths(5), new Insets(2));
            var endValue = new BorderStroke(new Color(1, 1, 1, 1), SOLID, new CornerRadii(20), new BorderWidths(15), new Insets(6));
            assertEquals(new BorderStroke(new Color(1, 1, 1, 1), SOLID, new CornerRadii(30), new BorderWidths(25), new Insets(10)),
                         startValue.interpolate(endValue, 2));
            assertEquals(new BorderStroke(new Color(1, 1, 1, 1), SOLID, new CornerRadii(40), new BorderWidths(35), new Insets(14)),
                         startValue.interpolate(endValue, 3));
        }
    }
}

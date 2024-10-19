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

import javafx.geometry.Insets;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
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
        public void interpolationFactorSmallerThanOrEqualToZeroReturnsStartInstance() {
            var startValue = new BorderStroke(RED, SOLID, new CornerRadii(10), new BorderWidths(5), new Insets(2));
            var endValue = new BorderStroke(GREEN, SOLID, new CornerRadii(20), new BorderWidths(15), new Insets(6));
            assertSame(startValue, startValue.interpolate(endValue, 0));
            assertSame(startValue, startValue.interpolate(endValue, -0.5));
        }

        @Test
        public void interpolationFactorGreaterThanOrEqualToOneReturnsEndInstance() {
            var startValue = new BorderStroke(RED, SOLID, new CornerRadii(10), new BorderWidths(5), new Insets(2));
            var endValue = new BorderStroke(GREEN, SOLID, new CornerRadii(20), new BorderWidths(15), new Insets(6));
            assertSame(endValue, startValue.interpolate(endValue, 1));
            assertSame(endValue, startValue.interpolate(endValue, 1.5));
        }
    }
}

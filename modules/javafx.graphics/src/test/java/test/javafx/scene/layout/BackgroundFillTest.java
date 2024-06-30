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

import com.sun.javafx.scene.paint.PaintUtils;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests for BackgroundFill
 */
public class BackgroundFillTest {
    @Test
    public void nullPaintDefaultsToTransparent() {
        BackgroundFill fill = new BackgroundFill(null, new CornerRadii(3), new Insets(4));
        assertEquals(Color.TRANSPARENT, fill.getFill());
    }

    @Test public void nullRadiusDefaultsToEmpty() {
        BackgroundFill fill = new BackgroundFill(Color.ORANGE, null, new Insets(2));
        assertEquals(CornerRadii.EMPTY, fill.getRadii());
    }

    @Test public void nullInsetsDefaultsToEmpty() {
        BackgroundFill fill = new BackgroundFill(Color.ORANGE, new CornerRadii(2), null);
        assertEquals(Insets.EMPTY, fill.getInsets());
    }

    @Test public void equivalentFills() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        assertEquals(a, b);
    }

    @Test public void differentFills() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        BackgroundFill b = new BackgroundFill(Color.RED, new CornerRadii(2), new Insets(3));
        assertFalse(a.equals(b));
    }

    @Test public void differentFills2() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(1), new Insets(3));
        assertFalse(a.equals(b));
    }

    @Test public void differentFills3() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(1));
        assertFalse(a.equals(b));
    }

    @Test public void equalsAgainstNull() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        assertFalse(a.equals(null));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test public void equalsAgainstRandomObject() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        assertFalse(a.equals("Some random object"));
    }

    @Test public void equivalentHaveSameHash() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void toStringCausesNoError() {
        BackgroundFill f = new BackgroundFill(null, null, null);
        f.toString();
    }

    @Nested
    class InterpolationTests {
        @Test
        public void twoColorFills() {
            BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(2));
            BackgroundFill b = new BackgroundFill(Color.RED, new CornerRadii(4), new Insets(6));
            BackgroundFill r = a.interpolate(b, 0.5);
            assertEquals(Color.ORANGE.interpolate(Color.RED, 0.5), r.getFill());
            assertEquals(new CornerRadii(3), r.getRadii());
            assertEquals(new Insets(4), r.getInsets());
        }

        @Test
        public void twoLinearGradientFills() {
            var gradient1 = LinearGradient.valueOf("linear-gradient(to left top, red, blue)");
            var gradient2 = LinearGradient.valueOf("linear-gradient(to left top, yellow, white)");
            BackgroundFill a = new BackgroundFill(gradient1, new CornerRadii(2), new Insets(2));
            BackgroundFill b = new BackgroundFill(gradient2, new CornerRadii(4), new Insets(6));
            BackgroundFill r = a.interpolate(b, 0.5);
            assertEquals(gradient1.interpolate(gradient2, 0.5), r.getFill());
            assertEquals(new CornerRadii(3), r.getRadii());
            assertEquals(new Insets(4), r.getInsets());
        }

        @Test
        public void linearGradientAndColorFills() {
            var gradient = LinearGradient.valueOf("linear-gradient(to left top, red, blue)");
            BackgroundFill a = new BackgroundFill(gradient, new CornerRadii(2), new Insets(2));
            BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(4), new Insets(6));

            BackgroundFill r = a.interpolate(b, 0.5);
            assertEquals(gradient.interpolate(PaintUtils.newSolidGradient(gradient, Color.ORANGE), 0.5), r.getFill());
            assertEquals(new CornerRadii(3), r.getRadii());
            assertEquals(new Insets(4), r.getInsets());

            r = b.interpolate(a, 0.5);
            assertEquals(PaintUtils.newSolidGradient(gradient, Color.ORANGE).interpolate(gradient, 0.5), r.getFill());
            assertEquals(new CornerRadii(3), r.getRadii());
            assertEquals(new Insets(4), r.getInsets());
        }

        @Test
        public void twoRadialGradientFills() {
            var gradient1 = RadialGradient.valueOf("radial-gradient(radius 100%, red, blue)");
            var gradient2 = RadialGradient.valueOf("radial-gradient(radius 50%, yellow, white)");
            BackgroundFill a = new BackgroundFill(gradient1, new CornerRadii(2), new Insets(2));
            BackgroundFill b = new BackgroundFill(gradient2, new CornerRadii(4), new Insets(6));
            BackgroundFill r = a.interpolate(b, 0.5);
            assertEquals(gradient1.interpolate(gradient2, 0.5), r.getFill());
            assertEquals(new CornerRadii(3), r.getRadii());
            assertEquals(new Insets(4), r.getInsets());
        }

        @Test
        public void radialGradientAndColorFills() {
            var gradient = RadialGradient.valueOf("radial-gradient(radius 100%, red, blue)");
            BackgroundFill a = new BackgroundFill(gradient, new CornerRadii(2), new Insets(2));
            BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(4), new Insets(6));

            BackgroundFill r = a.interpolate(b, 0.5);
            assertEquals(gradient.interpolate(PaintUtils.newSolidGradient(gradient, Color.ORANGE), 0.5), r.getFill());
            assertEquals(new CornerRadii(3), r.getRadii());
            assertEquals(new Insets(4), r.getInsets());

            r = b.interpolate(a, 0.5);
            assertEquals(PaintUtils.newSolidGradient(gradient, Color.ORANGE).interpolate(gradient, 0.5), r.getFill());
            assertEquals(new CornerRadii(3), r.getRadii());
            assertEquals(new Insets(4), r.getInsets());
        }

        @Test
        public void incompatibleFillsReturnsEndFillWhenInterpolationFactorIsLargerThanZero() {
            var pattern = new ImagePattern(new Image(new ByteArrayInputStream(new byte[] {})));
            BackgroundFill a = new BackgroundFill(pattern, new CornerRadii(2), new Insets(2));
            BackgroundFill b = new BackgroundFill(Color.RED, new CornerRadii(4), new Insets(6));
            BackgroundFill c = a.interpolate(b, 0);
            assertSame(a, c);

            c = a.interpolate(b, 0.5);
            assertEquals(b.getFill(), c.getFill());
            assertEquals(new CornerRadii(3), c.getRadii());
            assertEquals(new Insets(4), c.getInsets());
        }

        @Test
        public void twoEqualFillsReturnsExistingInstance() {
            BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(2));
            BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(2));
            assertSame(a, a.interpolate(b, 0.5));
        }

        @Test
        public void interpolationFactorSmallerThanOrEqualToZeroReturnsStartInstance() {
            BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(2));
            BackgroundFill b = new BackgroundFill(Color.RED, new CornerRadii(4), new Insets(6));
            assertSame(a, a.interpolate(b, 0));
            assertSame(a, a.interpolate(b, -1));
        }

        @Test
        public void interpolationFactorGreaterThanOrEqualToOneReturnsEndInstance() {
            BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(2));
            BackgroundFill b = new BackgroundFill(Color.RED, new CornerRadii(4), new Insets(6));
            assertSame(b, a.interpolate(b, 1));
            assertSame(b, a.interpolate(b, 1.5));
        }
    }
}

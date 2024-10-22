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

package test.javafx.scene.layout;

import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderImage;
import javafx.scene.layout.BorderRepeat;
import javafx.scene.layout.BorderWidths;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BorderImageTest {

    private static final Image IMAGE_1 = new Image("test/javafx/scene/layout/red.png");
    private static final Image IMAGE_2 = new Image("test/javafx/scene/layout/blue.png");

    @Nested
    class InterpolationTests {
        final BorderImage BORDER_IMAGE_A = new BorderImage(
            IMAGE_1, new BorderWidths(10), new Insets(8), new BorderWidths(6),
            false, BorderRepeat.REPEAT, BorderRepeat.REPEAT);

        final BorderImage BORDER_IMAGE_B = new BorderImage(
            IMAGE_2, new BorderWidths(20), new Insets(4), new BorderWidths(12),
            false, BorderRepeat.REPEAT, BorderRepeat.REPEAT);

        @Test
        public void interpolateBetweenDifferentValuesReturnsNewInstance() {
            var image = new BorderImage(
                IMAGE_2, new BorderWidths(15), new Insets(6), new BorderWidths(9),
                false, BorderRepeat.REPEAT, BorderRepeat.REPEAT);

            assertEquals(image, BORDER_IMAGE_A.interpolate(BORDER_IMAGE_B, 0.5));
        }

        @Test
        public void interpolateBetweenEqualValuesReturnsStartInstance() {
            var image = new BorderImage(
                IMAGE_1, new BorderWidths(10), new Insets(8), new BorderWidths(6),
                false, BorderRepeat.REPEAT, BorderRepeat.REPEAT);

            assertSame(image, image.interpolate(BORDER_IMAGE_A, 0.5));
        }

        @Test
        public void interpolationFactorSmallerThanOrEqualToZeroReturnsStartInstance() {
            assertSame(BORDER_IMAGE_A, BORDER_IMAGE_A.interpolate(BORDER_IMAGE_B, 0));
            assertSame(BORDER_IMAGE_A, BORDER_IMAGE_A.interpolate(BORDER_IMAGE_B, -0.5));
        }

        @Test
        public void interpolationFactorGreaterThanOrEqualToOneReturnsEndInstance() {
            assertSame(BORDER_IMAGE_B, BORDER_IMAGE_A.interpolate(BORDER_IMAGE_B, 1));
            assertSame(BORDER_IMAGE_B, BORDER_IMAGE_A.interpolate(BORDER_IMAGE_B, 1.5));
        }
    }
}

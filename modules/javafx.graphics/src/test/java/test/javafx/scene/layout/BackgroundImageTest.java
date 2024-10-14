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

import javafx.geometry.Side;
import javafx.scene.image.Image;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundSize;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static javafx.scene.layout.BackgroundRepeat.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 */
public class BackgroundImageTest {
    private static final BackgroundPosition POS_1 = new BackgroundPosition(Side.LEFT, .5, true, Side.TOP, 10, false);
    private static final BackgroundPosition POS_2 = BackgroundPosition.DEFAULT;

    private static final BackgroundSize SIZE_1 = new BackgroundSize(1, 1, true, true, false, true);
    private static final BackgroundSize SIZE_2 = BackgroundSize.DEFAULT;

    private static final Image IMAGE_1 = new Image("test/javafx/scene/layout/red.png");
    private static final Image IMAGE_2 = new Image("test/javafx/scene/layout/blue.png");

    @Test
    public void instanceCreation() {
        BackgroundImage image = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_1);
        assertEquals(IMAGE_1,  image.getImage());
        assertEquals(REPEAT, image.getRepeatX());
        assertEquals(REPEAT, image.getRepeatY());
        assertEquals(POS_1, image.getPosition());
        assertEquals(SIZE_1, image.getSize());
    }

    @Test
    public void instanceCreation2() {
        BackgroundImage image = new BackgroundImage(IMAGE_2, NO_REPEAT, ROUND, POS_2, SIZE_2);
        assertEquals(IMAGE_2,  image.getImage());
        assertEquals(NO_REPEAT, image.getRepeatX());
        assertEquals(ROUND, image.getRepeatY());
        assertEquals(POS_2, image.getPosition());
        assertEquals(SIZE_2, image.getSize());
    }

    @Test
    public void instanceCreationNullImage() {
        assertThrows(NullPointerException.class, () -> {
            new BackgroundImage(null, NO_REPEAT, ROUND, POS_2, SIZE_2);
        });
    }

    @Test
    public void instanceCreationNullRepeatXDefaultsToREPEAT() {
        BackgroundImage image = new BackgroundImage(IMAGE_1, null, REPEAT, POS_1, SIZE_1);
        assertEquals(IMAGE_1,  image.getImage());
        assertEquals(REPEAT, image.getRepeatX());
        assertEquals(REPEAT, image.getRepeatY());
        assertEquals(POS_1, image.getPosition());
        assertEquals(SIZE_1, image.getSize());
    }

    @Test
    public void instanceCreationNullRepeatYDefaultsToREPEAT() {
        BackgroundImage image = new BackgroundImage(IMAGE_1, REPEAT, null, POS_1, SIZE_1);
        assertEquals(IMAGE_1,  image.getImage());
        assertEquals(REPEAT, image.getRepeatX());
        assertEquals(REPEAT, image.getRepeatY());
        assertEquals(POS_1, image.getPosition());
        assertEquals(SIZE_1, image.getSize());
    }

    @Test
    public void instanceCreationNullPositionDefaultsToDEFAULT() {
        BackgroundImage image = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, null, SIZE_1);
        assertEquals(IMAGE_1,  image.getImage());
        assertEquals(REPEAT, image.getRepeatX());
        assertEquals(REPEAT, image.getRepeatY());
        assertEquals(BackgroundPosition.DEFAULT, image.getPosition());
        assertEquals(SIZE_1, image.getSize());
    }

    @Test
    public void instanceCreationNullSizeDefaultsToDEFAULT() {
        BackgroundImage image = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, null);
        assertEquals(IMAGE_1,  image.getImage());
        assertEquals(REPEAT, image.getRepeatX());
        assertEquals(REPEAT, image.getRepeatY());
        assertEquals(POS_1, image.getPosition());
        assertEquals(BackgroundSize.DEFAULT, image.getSize());
    }

    @Test
    public void equivalent() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_1);
        assertEquals(a, b);
    }

    @Test
    public void equivalent2() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, SPACE, REPEAT, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, SPACE, REPEAT, POS_1, SIZE_1);
        assertEquals(a, b);
    }

    @Test
    public void equivalent3() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, ROUND, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, ROUND, POS_1, SIZE_1);
        assertEquals(a, b);
    }

    @Test
    public void equivalent4() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_2, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_2, SIZE_1);
        assertEquals(a, b);
    }

    @Test
    public void equivalent5() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_2);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_2);
        assertEquals(a, b);
    }

    @Test
    public void equivalent6() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_2, SIZE_2);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
        assertEquals(a, b);
    }

    @Test
    public void equivalentHasSameHashCode() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_1);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equivalentHasSameHashCode2() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, SPACE, REPEAT, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, SPACE, REPEAT, POS_1, SIZE_1);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equivalentHasSameHashCode3() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, ROUND, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, ROUND, POS_1, SIZE_1);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equivalentHasSameHashCode4() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_2, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_2, SIZE_1);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equivalentHasSameHashCode5() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_2);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_2);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equivalentHasSameHashCode6() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_2, SIZE_2);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEquivalent() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_2, REPEAT, REPEAT, POS_1, SIZE_1);
        assertFalse(a.equals(b));
    }

    @Test
    public void notEquivalent2() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, SPACE, REPEAT, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, ROUND, REPEAT, POS_1, SIZE_1);
        assertFalse(a.equals(b));
    }

    @Test
    public void notEquivalent3() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, ROUND, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_1);
        assertFalse(a.equals(b));
    }

    @Test
    public void notEquivalent4() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_2, SIZE_1);
        assertFalse(a.equals(b));
    }

    @Test
    public void notEquivalent5() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_1);
        BackgroundImage b = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_1, SIZE_2);
        assertFalse(a.equals(b));
    }

    @Test
    public void notEquivalentWithNull() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_2, SIZE_2);
        assertFalse(a.equals(null));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void notEquivalentWithRandom() {
        BackgroundImage a = new BackgroundImage(IMAGE_1, REPEAT, REPEAT, POS_2, SIZE_2);
        assertFalse(a.equals("Some random string"));
    }

    @Nested
    class InterpolationTests {
        final BackgroundImage BACKGROUND_IMAGE_A = new BackgroundImage(
            IMAGE_1, NO_REPEAT, NO_REPEAT,
            new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false),
            new BackgroundSize(50, 100, false, false, false, false));

        final BackgroundImage BACKGROUND_IMAGE_B = new BackgroundImage(
            IMAGE_2, REPEAT, SPACE,
            new BackgroundPosition(Side.LEFT, 10, false, Side.TOP, 20, false),
            new BackgroundSize(100, 200, false, false, false, false));

        @Test
        public void interpolateBetweenDifferentValuesReturnsNewInstance() {
            var expect = new BackgroundImage(
                IMAGE_2, REPEAT, SPACE,
                new BackgroundPosition(Side.LEFT, 5, false, Side.TOP, 10, false),
                new BackgroundSize(75, 150, false, false, false, false));

            var actual = BACKGROUND_IMAGE_A.interpolate(BACKGROUND_IMAGE_B, 0.5);

            assertEquals(expect, actual);
            assertNotSame(expect, actual);
        }

        @Test
        public void interpolateBetweenEqualValuesReturnsStartInstance() {
            var startValue = new BackgroundImage(
                IMAGE_1, NO_REPEAT, NO_REPEAT,
                new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false),
                new BackgroundSize(50, 100, false, false, false, false));

            var endValue = new BackgroundImage(
                IMAGE_1, NO_REPEAT, NO_REPEAT,
                new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false),
                new BackgroundSize(50, 100, false, false, false, false));

            assertSame(startValue, startValue.interpolate(endValue, 0.5));
        }

        @Test
        public void interpolationFactorSmallerThanOrEqualToZeroReturnsStartInstance() {
            assertSame(BACKGROUND_IMAGE_A, BACKGROUND_IMAGE_A.interpolate(BACKGROUND_IMAGE_B, 0));
            assertSame(BACKGROUND_IMAGE_A, BACKGROUND_IMAGE_A.interpolate(BACKGROUND_IMAGE_B, -0.5));
        }

        @Test
        public void interpolationFactorGreaterThanOrEqualToOneReturnsEndInstance() {
            assertSame(BACKGROUND_IMAGE_B, BACKGROUND_IMAGE_A.interpolate(BACKGROUND_IMAGE_B, 1));
            assertSame(BACKGROUND_IMAGE_B, BACKGROUND_IMAGE_A.interpolate(BACKGROUND_IMAGE_B, 1.5));
        }
    }
}

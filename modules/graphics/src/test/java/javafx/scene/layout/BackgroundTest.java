/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.junit.Test;

import static javafx.scene.layout.BackgroundRepeat.*;
import static org.junit.Assert.*;

/**
 */
public class BackgroundTest {
    private static final BackgroundFill[] FILLS_1 = new BackgroundFill[] {
            new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY),
    };
    private static final BackgroundFill[] FILLS_2 = new BackgroundFill[] {
            new BackgroundFill(Color.GREEN, new CornerRadii(3), new Insets(4)),
            new BackgroundFill(Color.BLUE, new CornerRadii(6), new Insets(8)),
    };

    private static final Image IMAGE_1 = new Image("javafx/scene/layout/red.png");
    private static final Image IMAGE_2 = new Image("javafx/scene/layout/blue.png");
    private static final Image IMAGE_3 = new Image("javafx/scene/layout/green.png");
    private static final Image IMAGE_4 = new Image("javafx/scene/layout/yellow.png");

    private static final BackgroundImage[] IMAGES_1 = new BackgroundImage[] {
            new BackgroundImage(IMAGE_1, null, null, null, null)
    };

    private static final BackgroundImage[] IMAGES_2 = new BackgroundImage[] {
            new BackgroundImage(IMAGE_2, SPACE, SPACE, null, null),
            new BackgroundImage(IMAGE_3, ROUND, ROUND, null, null),
            new BackgroundImage(IMAGE_4, NO_REPEAT, NO_REPEAT, null, null)
    };

    @Test public void instanceCreation() {
        Background b = new Background(FILLS_1, IMAGES_1);
        assertEquals(FILLS_1.length, b.getFills().size(), 0);
        assertEquals(FILLS_1[0], b.getFills().get(0));
        assertEquals(IMAGES_1.length, b.getImages().size(), 0);
        assertEquals(IMAGES_1[0], b.getImages().get(0));
    }

    @Test public void instanceCreation2() {
        Background b = new Background(FILLS_2, IMAGES_2);
        assertEquals(FILLS_2.length, b.getFills().size(), 0);
        assertEquals(FILLS_2[0], b.getFills().get(0));
        assertEquals(FILLS_2[1], b.getFills().get(1));
        assertEquals(IMAGES_2.length, b.getImages().size(), 0);
        assertEquals(IMAGES_2[0], b.getImages().get(0));
        assertEquals(IMAGES_2[1], b.getImages().get(1));
        assertEquals(IMAGES_2[2], b.getImages().get(2));
    }

    @Test public void instanceCreationNullFills() {
        Background b = new Background(null, IMAGES_1);
        assertEquals(0, b.getFills().size(), 0);
        assertEquals(IMAGES_1.length, b.getImages().size(), 0);
        assertEquals(IMAGES_1[0], b.getImages().get(0));
    }

    @Test public void instanceCreationEmptyFills() {
        Background b = new Background(new BackgroundFill[0], IMAGES_1);
        assertEquals(0, b.getFills().size(), 0);
        assertEquals(IMAGES_1.length, b.getImages().size(), 0);
        assertEquals(IMAGES_1[0], b.getImages().get(0));
    }

    @Test public void instanceCreationNullImages() {
        Background b = new Background(FILLS_1, null);
        assertEquals(FILLS_1.length, b.getFills().size(), 0);
        assertEquals(FILLS_1[0], b.getFills().get(0));
        assertEquals(0, b.getImages().size(), 0);
    }

    @Test public void instanceCreationEmptyImages() {
        Background b = new Background(FILLS_1, new BackgroundImage[0]);
        assertEquals(FILLS_1.length, b.getFills().size(), 0);
        assertEquals(FILLS_1[0], b.getFills().get(0));
        assertEquals(0, b.getImages().size(), 0);
    }

    @Test public void instanceCreationWithNullsInTheFillArray() {
        final BackgroundFill[] fills = new BackgroundFill[] {
                null,
                new BackgroundFill(Color.GREEN, new CornerRadii(3), new Insets(4)),
                new BackgroundFill(Color.BLUE, new CornerRadii(6), new Insets(8)),
        };
        Background b = new Background(fills, null);
        assertEquals(FILLS_2.length, b.getFills().size(), 0);
        assertEquals(FILLS_2[0], b.getFills().get(0));
        assertEquals(FILLS_2[1], b.getFills().get(1));
    }

    @Test public void instanceCreationWithNullsInTheFillArray2() {
        final BackgroundFill[] fills = new BackgroundFill[] {
                new BackgroundFill(Color.GREEN, new CornerRadii(3), new Insets(4)),
                null,
                new BackgroundFill(Color.BLUE, new CornerRadii(6), new Insets(8)),
        };
        Background b = new Background(fills, null);
        assertEquals(FILLS_2.length, b.getFills().size(), 0);
        assertEquals(FILLS_2[0], b.getFills().get(0));
        assertEquals(FILLS_2[1], b.getFills().get(1));
    }

    @Test public void instanceCreationWithNullsInTheFillArray3() {
        final BackgroundFill[] fills = new BackgroundFill[] {
                new BackgroundFill(Color.GREEN, new CornerRadii(3), new Insets(4)),
                new BackgroundFill(Color.BLUE, new CornerRadii(6), new Insets(8)),
                null
        };
        Background b = new Background(fills, null);
        assertEquals(FILLS_2.length, b.getFills().size(), 0);
        assertEquals(FILLS_2[0], b.getFills().get(0));
        assertEquals(FILLS_2[1], b.getFills().get(1));
    }

    @Test public void instanceCreationWithNullsInTheFillArray4() {
        final BackgroundFill[] fills = new BackgroundFill[] {
                null
        };
        Background b = new Background(fills, null);
        assertEquals(0, b.getFills().size(), 0);
    }

    @Test public void instanceCreationWithNullsInTheImageArray() {
        final BackgroundImage[] images = new BackgroundImage[] {
                null,
                new BackgroundImage(IMAGE_2, SPACE, SPACE, null, null),
                new BackgroundImage(IMAGE_3, ROUND, ROUND, null, null),
                new BackgroundImage(IMAGE_4, NO_REPEAT, NO_REPEAT, null, null)
        };
        Background b = new Background(null, images);
        assertEquals(IMAGES_2.length, b.getImages().size(), 0);
        assertEquals(IMAGES_2[0], b.getImages().get(0));
        assertEquals(IMAGES_2[1], b.getImages().get(1));
        assertEquals(IMAGES_2[2], b.getImages().get(2));
    }

    @Test public void instanceCreationWithNullsInTheImageArray2() {
        final BackgroundImage[] images = new BackgroundImage[] {
                new BackgroundImage(IMAGE_2, SPACE, SPACE, null, null),
                null,
                new BackgroundImage(IMAGE_3, ROUND, ROUND, null, null),
                new BackgroundImage(IMAGE_4, NO_REPEAT, NO_REPEAT, null, null)
        };
        Background b = new Background(null, images);
        assertEquals(IMAGES_2.length, b.getImages().size(), 0);
        assertEquals(IMAGES_2[0], b.getImages().get(0));
        assertEquals(IMAGES_2[1], b.getImages().get(1));
        assertEquals(IMAGES_2[2], b.getImages().get(2));
    }

    @Test public void instanceCreationWithNullsInTheImageArray3() {
        final BackgroundImage[] images = new BackgroundImage[] {
                new BackgroundImage(IMAGE_2, SPACE, SPACE, null, null),
                new BackgroundImage(IMAGE_3, ROUND, ROUND, null, null),
                new BackgroundImage(IMAGE_4, NO_REPEAT, NO_REPEAT, null, null),
                null
        };
        Background b = new Background(null, images);
        assertEquals(IMAGES_2.length, b.getImages().size(), 0);
        assertEquals(IMAGES_2[0], b.getImages().get(0));
        assertEquals(IMAGES_2[1], b.getImages().get(1));
        assertEquals(IMAGES_2[2], b.getImages().get(2));
    }

    @Test public void instanceCreationWithNullsInTheImageArray4() {
        final BackgroundImage[] images = new BackgroundImage[] {
                null
        };
        Background b = new Background(null, images);
        assertEquals(0, b.getImages().size(), 0);
    }

    @Test public void suppliedBackgroundFillsMutatedLaterDoNotChangeFills() {
        final BackgroundFill fill = new BackgroundFill(Color.GREEN, new CornerRadii(3), new Insets(4));
        final BackgroundFill[] fills = new BackgroundFill[] { fill };
        Background b = new Background(fills, null);
        Background b2 = new Background(fills);
        fills[0] = null;
        assertEquals(1, b.getFills().size());
        assertEquals(1, b2.getFills().size());
        assertSame(fill, b.getFills().get(0));
        assertSame(fill, b2.getFills().get(0));
    }

    @Test public void suppliedBackgroundImagesMutatedLaterDoNotChangeImages() {
        final BackgroundImage image = new BackgroundImage(IMAGE_2, SPACE, SPACE, null, null);
        final BackgroundImage[] images = new BackgroundImage[] { image };
        Background b = new Background(null, images);
        Background b2 = new Background(images);
        images[0] = null;
        assertEquals(1, b.getImages().size());
        assertEquals(1, b2.getImages().size());
        assertSame(image, b.getImages().get(0));
        assertSame(image, b2.getImages().get(0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fillsIsUnmodifiable() {
        final BackgroundFill fill = new BackgroundFill(Color.GREEN, new CornerRadii(3), new Insets(4));
        final BackgroundFill[] fills = new BackgroundFill[] { fill };
        Background b = new Background(fills);
        b.getFills().add(new BackgroundFill(Color.BLUE, new CornerRadii(6), new Insets(8)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void imagesIsUnmodifiable() {
        final BackgroundImage image = new BackgroundImage(IMAGE_2, SPACE, SPACE, null, null);
        final BackgroundImage[] images = new BackgroundImage[] { image };
        Background b = new Background(images);
        b.getImages().add(new BackgroundImage(IMAGE_3, ROUND, ROUND, null, null));
    }

    @Test public void backgroundOutsetsAreDefinedByFills() {
        final BackgroundFill[] fills = new BackgroundFill[] {
                new BackgroundFill(Color.RED, new CornerRadii(3), new Insets(-1, 5, 5, 5)),
                new BackgroundFill(Color.GREEN, new CornerRadii(6), new Insets(8)),
                new BackgroundFill(Color.BLUE, new CornerRadii(6), new Insets(2, -1, 3, 5)),
                new BackgroundFill(Color.MAGENTA, new CornerRadii(6), new Insets(-7, -2, 4, 4)),
                new BackgroundFill(Color.CYAN, new CornerRadii(6), new Insets(0, 0, -8, 0)),
                new BackgroundFill(Color.YELLOW, new CornerRadii(6), new Insets(4, -1, 3, 5)),
                new BackgroundFill(Color.BLACK, new CornerRadii(6), new Insets(0, 0, 0, -8))
        };

        Background b = new Background(fills, null);
        assertEquals(new Insets(7, 2, 8, 8), b.getOutsets());
    }

    @Test public void backgroundImagesDoNotContributeToOutsets() {
        final BackgroundImage[] images = new BackgroundImage[] {
                new BackgroundImage(IMAGE_1, null, null,
                        new BackgroundPosition(Side.LEFT, -10, false, Side.TOP, -10, false),
                        null)
        };

        Background b = new Background(null, images);
        assertEquals(Insets.EMPTY, b.getOutsets());
    }

    @Test public void equivalent() {
        Background a = new Background((BackgroundFill[])null, null);
        Background b = new Background((BackgroundFill[])null, null);
        assertEquals(a, b);
    }

    @Test public void equivalent2() {
        Background a = new Background(FILLS_2, null);
        Background b = new Background(FILLS_2, null);
        assertEquals(a, b);
    }

    @Test public void equivalent3() {
        Background a = new Background(null, IMAGES_2);
        Background b = new Background(null, IMAGES_2);
        assertEquals(a, b);
    }

    @Test public void equivalentHasSameHashCode() {
        Background a = new Background((BackgroundFill[])null, null);
        Background b = new Background((BackgroundFill[])null, null);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHasSameHashCode2() {
        Background a = new Background(FILLS_2, null);
        Background b = new Background(FILLS_2, null);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHasSameHashCode3() {
        Background a = new Background(null, IMAGES_2);
        Background b = new Background(null, IMAGES_2);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void notEqual() {
        Background a = new Background(FILLS_1, null);
        Background b = new Background((BackgroundFill[])null, null);
        assertFalse(a.equals(b));
    }

    @Test public void notEqual2() {
        Background a = new Background((BackgroundFill[])null, null);
        Background b = new Background(FILLS_2, null);
        assertFalse(a.equals(b));
    }

    @Test public void notEqual3() {
        Background a = new Background(null, IMAGES_1);
        Background b = new Background((BackgroundFill[])null, null);
        assertFalse(a.equals(b));
    }

    @Test public void notEqual4() {
        Background a = new Background((BackgroundFill[])null, null);
        Background b = new Background(null, IMAGES_2);
        assertFalse(a.equals(b));
    }

    @Test public void notEqualWithNull() {
        Background a = new Background((BackgroundFill[])null, null);
        assertFalse(a.equals(null));
    }

    @Test public void notEqualWithRandom() {
        Background a = new Background((BackgroundFill[])null, null);
        assertFalse(a.equals("Some random String"));
    }

    /**************************************************************************
     *                                                                        *
     * Tests for getting the computed opaque insets. Like normal insets,      *
     * these are positive going inwards.                                      *
     *                                                                        *
     *************************************************************************/

    @Test public void opaqueInsets_nullFillsResultsInNaN() {
        Background b = new Background((BackgroundFill[])null, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertTrue(Double.isNaN(trbl[0]));
        assertTrue(Double.isNaN(trbl[1]));
        assertTrue(Double.isNaN(trbl[2]));
        assertTrue(Double.isNaN(trbl[3]));
    }

    @Test public void opaqueInsets_transparentFillsResultsInNaN() {
        BackgroundFill f = new BackgroundFill(Color.TRANSPARENT, null, null);
        Background b = new Background(new BackgroundFill[] { f }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertTrue(Double.isNaN(trbl[0]));
        assertTrue(Double.isNaN(trbl[1]));
        assertTrue(Double.isNaN(trbl[2]));
        assertTrue(Double.isNaN(trbl[3]));
    }

    @Test public void opaqueInsets_transparentFillsResultsInNaN2() {
        BackgroundFill f = new BackgroundFill(Color.rgb(255, 0, 0, 0), null, null);
        Background b = new Background(new BackgroundFill[] { f }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertTrue(Double.isNaN(trbl[0]));
        assertTrue(Double.isNaN(trbl[1]));
        assertTrue(Double.isNaN(trbl[2]));
        assertTrue(Double.isNaN(trbl[3]));
    }

    @Test public void opaqueInsets_transparentFillsResultsInNaN3() {
        BackgroundFill f = new BackgroundFill(Color.TRANSPARENT, null, null);
        BackgroundFill f2 = new BackgroundFill(Color.rgb(255, 0, 0, 0), null, null);
        Background b = new Background(new BackgroundFill[] { f, f2 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertTrue(Double.isNaN(trbl[0]));
        assertTrue(Double.isNaN(trbl[1]));
        assertTrue(Double.isNaN(trbl[2]));
        assertTrue(Double.isNaN(trbl[3]));
    }

    @Test public void opaqueInsets_transparentFillsMixedWithNonTransparentFills() {
        BackgroundFill f = new BackgroundFill(Color.TRANSPARENT, null, null);
        BackgroundFill f2 = new BackgroundFill(Color.RED, null, new Insets(1));
        Background b = new Background(new BackgroundFill[] { f, f2 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(1, trbl[0], 0);
        assertEquals(1, trbl[1], 0);
        assertEquals(1, trbl[2], 0);
        assertEquals(1, trbl[3], 0);
    }

    @Test public void opaqueInsets_transparentFillsMixedWithNonTransparentFills2() {
        BackgroundFill f = new BackgroundFill(Color.TRANSPARENT, null, null);
        BackgroundFill f2 = new BackgroundFill(Color.RED, null, new Insets(-1));
        Background b = new Background(new BackgroundFill[] { f, f2 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(-1, trbl[0], 0);
        assertEquals(-1, trbl[1], 0);
        assertEquals(-1, trbl[2], 0);
        assertEquals(-1, trbl[3], 0);
    }

    @Test public void opaqueInsets_nestedOpaqueRectangles_LargestRectangleUsed() {
        BackgroundFill f = new BackgroundFill(Color.RED, null, new Insets(0));
        BackgroundFill f2 = new BackgroundFill(Color.GREEN, null, new Insets(1));
        BackgroundFill f3 = new BackgroundFill(Color.BLUE, null, new Insets(2));
        Background b = new Background(new BackgroundFill[] { f, f2, f3 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(0, trbl[0], 0);
        assertEquals(0, trbl[1], 0);
        assertEquals(0, trbl[2], 0);
        assertEquals(0, trbl[3], 0);
    }

    @Test public void opaqueInsets_nestedOpaqueRectangles_LargestRectangleUsed2() {
        BackgroundFill f = new BackgroundFill(Color.RED, null, new Insets(-1));
        BackgroundFill f2 = new BackgroundFill(Color.GREEN, null, new Insets(0));
        BackgroundFill f3 = new BackgroundFill(Color.BLUE, null, new Insets(1));
        Background b = new Background(new BackgroundFill[] { f, f2, f3 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(-1, trbl[0], 0);
        assertEquals(-1, trbl[1], 0);
        assertEquals(-1, trbl[2], 0);
        assertEquals(-1, trbl[3], 0);
    }

    @Test public void opaqueInsets_nestedOpaqueRectangles_LargestRectangleUsed3() {
        BackgroundFill f = new BackgroundFill(Color.RED, null, new Insets(10));
        BackgroundFill f2 = new BackgroundFill(Color.GREEN, null, new Insets(1));
        BackgroundFill f3 = new BackgroundFill(Color.BLUE, null, new Insets(2));
        Background b = new Background(new BackgroundFill[] { f, f2, f3 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(1, trbl[0], 0);
        assertEquals(1, trbl[1], 0);
        assertEquals(1, trbl[2], 0);
        assertEquals(1, trbl[3], 0);
    }

    @Test public void opaqueInsets_nestedOpaqueRectangles_LargestRectangleUsed4() {
        BackgroundFill f = new BackgroundFill(Color.RED, null, new Insets(0));
        BackgroundFill f2 = new BackgroundFill(Color.GREEN, null, new Insets(1));
        BackgroundFill f3 = new BackgroundFill(Color.BLUE, null, new Insets(-2));
        Background b = new Background(new BackgroundFill[] { f, f2, f3 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(-2, trbl[0], 0);
        assertEquals(-2, trbl[1], 0);
        assertEquals(-2, trbl[2], 0);
        assertEquals(-2, trbl[3], 0);
    }

    @Test public void opaqueInsets_offsetOpaqueRectangles_completelyContained_LargestRectangleUsed() {
        BackgroundFill f = new BackgroundFill(Color.RED, null, new Insets(0));
        BackgroundFill f2 = new BackgroundFill(Color.GREEN, null, new Insets(1, 0, 0, 0));
        BackgroundFill f3 = new BackgroundFill(Color.BLUE, null, new Insets(0, 1, 0, 0));
        BackgroundFill f4 = new BackgroundFill(Color.YELLOW, null, new Insets(0, 0, 1, 0));
        BackgroundFill f5 = new BackgroundFill(Color.CYAN, null, new Insets(0, 0, 0, 1));
        Background b = new Background(new BackgroundFill[] { f, f2, f3, f4, f5 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(0, trbl[0], 0);
        assertEquals(0, trbl[1], 0);
        assertEquals(0, trbl[2], 0);
        assertEquals(0, trbl[3], 0);
    }

    // Even when the big rectangle is not the first, does it still work?
    @Test public void opaqueInsets_offsetOpaqueRectangles_completelyContained_LargestRectangleUsed2() {
        BackgroundFill f = new BackgroundFill(Color.YELLOW, null, new Insets(0, 0, 1, 0));
        BackgroundFill f2 = new BackgroundFill(Color.GREEN, null, new Insets(1, 0, 0, 0));
        BackgroundFill f3 = new BackgroundFill(Color.BLUE, null, new Insets(0, 1, 0, 0));
        BackgroundFill f4 = new BackgroundFill(Color.RED, null, new Insets(0));
        BackgroundFill f5 = new BackgroundFill(Color.CYAN, null, new Insets(0, 0, 0, 1));
        Background b = new Background(new BackgroundFill[] { f, f2, f3, f4, f5 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(0, trbl[0], 0);
        assertEquals(0, trbl[1], 0);
        assertEquals(0, trbl[2], 0);
        assertEquals(0, trbl[3], 0);
    }

    @Test public void opaqueInsets_offsetOpaqueRectangles_UnionUsed() {
        BackgroundFill f = new BackgroundFill(Color.RED, null, new Insets(10, 0, 0, 0));
        BackgroundFill f2 = new BackgroundFill(Color.GREEN, null, new Insets(0, 10, 0, 0));
        BackgroundFill f3 = new BackgroundFill(Color.BLUE, null, new Insets(0, 0, 10, 0));
        BackgroundFill f4 = new BackgroundFill(Color.YELLOW, null, new Insets(0, 0, 0, 10));
        Background b = new Background(new BackgroundFill[] { f, f2, f3, f4 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(0, trbl[0], 0);
        assertEquals(0, trbl[1], 0);
        assertEquals(0, trbl[2], 0);
        assertEquals(0, trbl[3], 0);
    }

    @Test public void opaqueInsets_offsetOpaqueRectangles_UnionUsed2() {
        BackgroundFill f = new BackgroundFill(Color.RED, null, new Insets(10, 0, 0, 0));
        BackgroundFill f2 = new BackgroundFill(Color.GREEN, null, new Insets(10, 10, 0, 0));
        BackgroundFill f3 = new BackgroundFill(Color.BLUE, null, new Insets(10, 10, 10, 0));
        BackgroundFill f4 = new BackgroundFill(Color.YELLOW, null, new Insets(10, 10, 10, 10));
        Background b = new Background(new BackgroundFill[] { f, f2, f3, f4 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(10, trbl[0], 0);
        assertEquals(0, trbl[1], 0);
        assertEquals(0, trbl[2], 0);
        assertEquals(0, trbl[3], 0);
    }

    // There are actually 3 possible outcomes from this test, and the one being tested is
    // the one currently being used. We could use the bounds of f, the bounds of f2, or the
    // intersection of the bounds of f and f2. It turns out in this case, the intersection
    // would be smaller, but the bounds of f and f2 are equal in size.
    @Test public void opaqueInsets_offsetOpaqueRectangles_LargestUsed() {
        BackgroundFill f = new BackgroundFill(Color.RED, null, new Insets(10));
        BackgroundFill f2 = new BackgroundFill(Color.GREEN, null, new Insets(20, 0, 0, 20));
        Background b = new Background(new BackgroundFill[] { f, f2 }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(10, trbl[0], 0);
        assertEquals(10, trbl[1], 0);
        assertEquals(10, trbl[2], 0);
        assertEquals(10, trbl[3], 0);
    }

    // NOTE: For these corner radii tests, I only need to know that the opaque region
    // of a single fill is correct for any given set of corner radii, because after
    // the opaque region for a single fill is computed, thereafter the rest of the
    // implementation is all the same
    @Test public void opaqueInsets_uniformCornerRadii() {
        BackgroundFill f = new BackgroundFill(Color.RED, new CornerRadii(3), null);
        Background b = new Background(new BackgroundFill[] { f }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(1.5, trbl[0], 0);
        assertEquals(1.5, trbl[1], 0);
        assertEquals(1.5, trbl[2], 0);
        assertEquals(1.5, trbl[3], 0);
    }

    @Test public void opaqueInsets_nonUniformCornerRadii() {
        BackgroundFill f = new BackgroundFill(Color.RED, new CornerRadii(1, 2, 3, 4, false), null);
        Background b = new Background(new BackgroundFill[] { f }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(1, trbl[0], 0);
        assertEquals(1.5, trbl[1], 0);
        assertEquals(2, trbl[2], 0);
        assertEquals(2, trbl[3], 0);
    }

    @Test public void opaqueInsets_nonUniformCornerRadii2() {
        BackgroundFill f = new BackgroundFill(Color.RED, new CornerRadii(1, 2, 3, 4, 5, 6, 7, 8,
                                                                         false, false, false, false,
                                                                         false, false, false, false), null);
        Background b = new Background(new BackgroundFill[] { f }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(1.5, trbl[0], 0);
        assertEquals(2.5, trbl[1], 0);
        assertEquals(3.5, trbl[2], 0);
        assertEquals(4, trbl[3], 0);
    }

    @Test public void opaqueInsetsPercent_uniformCornerRadii() {
        BackgroundFill f = new BackgroundFill(Color.RED, new CornerRadii(.1, true), null);
        Background b = new Background(new BackgroundFill[] { f }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(2.5, trbl[0], 0);
        assertEquals(5, trbl[1], 0);
        assertEquals(2.5, trbl[2], 0);
        assertEquals(5, trbl[3], 0);
    }

    @Test public void opaqueInsetsPercent_nonUniformCornerRadii() {
        BackgroundFill f = new BackgroundFill(Color.RED, new CornerRadii(.1, .2, .3, .4, true), null);
        Background b = new Background(new BackgroundFill[] { f }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(5, trbl[0], 0); // top-left-vertical is 2.5, but top-right-vertical is 5
        assertEquals(15, trbl[1], 0); // top-right-horizontal is 5, but bottom-right-horizontal is 15
        assertEquals(10, trbl[2], 0); // bottom-right-vertical is 7.5, but bottom-left-vertical is 10
        assertEquals(20, trbl[3], 0); // bottom-left-horizontal dominates at 20
    }

    @Test public void opaqueInsetsPercent_nonUniformCornerRadii2() {
        BackgroundFill f = new BackgroundFill(Color.RED, new CornerRadii(.1, .15, .2, .25, .3, .35, .4, .45,
                                                                         true, true, true, true,
                                                                         true, true, true, true), null);
        Background b = new Background(new BackgroundFill[] { f }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(5, trbl[0], 0); // top-left-vertical is 3.75, but top-right-vertical is 5
        assertEquals(15, trbl[1], 0); // top-right-horizontal is 12.5, but bottom-right-horizontal is 15
        assertEquals(10, trbl[2], 0); // bottom-right-vertical is 8.75, but bottom-left-vertical is 10
        assertEquals(22.5, trbl[3], 0); // bottom-left-horizontal dominates at 22.5
    }

    @Test public void opaqueInsetsPercent_nonUniformCornerRadii3() {
        BackgroundFill f = new BackgroundFill(Color.RED, new CornerRadii(.1, 15, .2, 25, .3, 35, .4, 45,
                                                                         true, false, true, false,
                                                                         true, false, true, false), null);
        Background b = new Background(new BackgroundFill[] { f }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(7.5, trbl[0], 0); // top-left-vertical is 7.5, and top-right-vertical is 5
        assertEquals(15, trbl[1], 0); // top-right-horizontal is 12.5, but bottom-right-horizontal is 15
        assertEquals(17.5, trbl[2], 0); // bottom-right-vertical is 17.5, and bottom-left-vertical is 10
        assertEquals(22.5, trbl[3], 0); // bottom-left-horizontal dominates at 22.5
    }

    @Test public void opaqueInsetsPercent_nonUniformCornerRadii4() {
        BackgroundFill f = new BackgroundFill(Color.RED, new CornerRadii(10, .15, 20, .25, 30, .35, 40, .45,
                                                                         false, true, false, true,
                                                                         false, true, false, true), null);
        Background b = new Background(new BackgroundFill[] { f }, null);
        final double[] trbl = new double[4];
        b.computeOpaqueInsets(100, 50, trbl);
        assertEquals(10, trbl[0], 0); // top-left-vertical is 3.75, but top-right-vertical is 10
        assertEquals(15, trbl[1], 0); // top-right-horizontal is 12.5, but bottom-right-horizontal is 15
        assertEquals(20, trbl[2], 0); // bottom-right-vertical is 8.75, but bottom-left-vertical is 20
        assertEquals(22.5, trbl[3], 0); // bottom-left-horizontal dominates at 22.5
    }

    // TODO: What happens if the corner radii become so big that we would end up with a negative opaque
    // inset in one dimension?

    // TODO: What happens if the insets are so big that they cross in either dimension?

    // TODO: Test having insets on images, and a combination of insets on images and fills
}

/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;
import org.junit.Test;

import static javafx.scene.layout.BorderRepeat.*;
import static org.junit.Assert.*;

/**
 * Tests for Border.
 */
public class BorderTest {
    private static final BorderStroke[] STROKES_1 = new BorderStroke[] {
            new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.EMPTY)
    };
    private static final BorderStroke[] STROKES_2 = new BorderStroke[] {
            new BorderStroke(Color.GREEN, BorderStrokeStyle.DASHED, new CornerRadii(3), new BorderWidths(4)),
            new BorderStroke(Color.BLUE, BorderStrokeStyle.DOTTED, new CornerRadii(6), new BorderWidths(8))
    };

    private static final Image IMAGE_1 = new Image("javafx/scene/layout/red.png");
    private static final Image IMAGE_2 = new Image("javafx/scene/layout/blue.png");
    private static final Image IMAGE_3 = new Image("javafx/scene/layout/green.png");
    private static final Image IMAGE_4 = new Image("javafx/scene/layout/yellow.png");

    private static final BorderImage[] IMAGES_1 = new BorderImage[] {
            new BorderImage(IMAGE_1, BorderWidths.DEFAULT, Insets.EMPTY, BorderWidths.EMPTY, false, SPACE, SPACE)
    };

    private static final BorderImage[] IMAGES_2 = new BorderImage[] {
            new BorderImage(IMAGE_2, new BorderWidths(3), new Insets(4), BorderWidths.EMPTY, false, REPEAT, REPEAT),
            new BorderImage(IMAGE_3, new BorderWidths(6), new Insets(2), BorderWidths.EMPTY, false, SPACE, ROUND),
            new BorderImage(IMAGE_4, new BorderWidths(3), Insets.EMPTY, new BorderWidths(3, 4, 5, 6), true, STRETCH, SPACE)
    };

    @Test public void instanceCreation() {
        Border b = new Border(STROKES_1, IMAGES_1);
        assertEquals(STROKES_1.length, b.getStrokes().size(), 0);
        assertEquals(STROKES_1[0], b.getStrokes().get(0));
        assertEquals(IMAGES_1.length, b.getImages().size(), 0);
        assertEquals(IMAGES_1[0], b.getImages().get(0));
    }

    @Test public void instanceCreation2() {
        Border b = new Border(STROKES_2, IMAGES_2);
        assertEquals(STROKES_2.length, b.getStrokes().size(), 0);
        assertEquals(STROKES_2[0], b.getStrokes().get(0));
        assertEquals(STROKES_2[1], b.getStrokes().get(1));
        assertEquals(IMAGES_2.length, b.getImages().size(), 0);
        assertEquals(IMAGES_2[0], b.getImages().get(0));
        assertEquals(IMAGES_2[1], b.getImages().get(1));
        assertEquals(IMAGES_2[2], b.getImages().get(2));
    }

    @Test public void instanceCreationNullstrokes() {
        Border b = new Border(null, IMAGES_1);
        assertEquals(0, b.getStrokes().size(), 0);
        assertEquals(IMAGES_1.length, b.getImages().size(), 0);
        assertEquals(IMAGES_1[0], b.getImages().get(0));
    }

    @Test public void instanceCreationEmptystrokes() {
        Border b = new Border(new BorderStroke[0], IMAGES_1);
        assertEquals(0, b.getStrokes().size(), 0);
        assertEquals(IMAGES_1.length, b.getImages().size(), 0);
        assertEquals(IMAGES_1[0], b.getImages().get(0));
    }

    @Test public void instanceCreationNullImages() {
        Border b = new Border(STROKES_1, null);
        assertEquals(STROKES_1.length, b.getStrokes().size(), 0);
        assertEquals(STROKES_1[0], b.getStrokes().get(0));
        assertEquals(0, b.getImages().size(), 0);
    }

    @Test public void instanceCreationEmptyImages() {
        Border b = new Border(STROKES_1, new BorderImage[0]);
        assertEquals(STROKES_1.length, b.getStrokes().size(), 0);
        assertEquals(STROKES_1[0], b.getStrokes().get(0));
        assertEquals(0, b.getImages().size(), 0);
    }

    @Test public void instanceCreationWithNullsInTheFillArray() {
        final BorderStroke[] strokes = new BorderStroke[] {
                null,
                new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, new CornerRadii(3), new BorderWidths(4)),
                new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID,new CornerRadii(6), new BorderWidths(8)),
        };
        Border b = new Border(strokes, null);
        assertEquals(2, b.getStrokes().size(), 0);
        assertEquals(strokes[1], b.getStrokes().get(0));
        assertEquals(strokes[2], b.getStrokes().get(1));
    }

    @Test public void instanceCreationWithNullsInTheFillArray2() {
        final BorderStroke[] strokes = new BorderStroke[] {
                new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, new CornerRadii(3), new BorderWidths(4)),
                null,
                new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID,new CornerRadii(6), new BorderWidths(8)),
        };
        Border b = new Border(strokes, null);
        assertEquals(2, b.getStrokes().size(), 0);
        assertEquals(strokes[0], b.getStrokes().get(0));
        assertEquals(strokes[2], b.getStrokes().get(1));
    }

    @Test public void instanceCreationWithNullsInTheFillArray3() {
        final BorderStroke[] strokes = new BorderStroke[] {
                new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, new CornerRadii(3), new BorderWidths(4)),
                new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(8)),
                null
        };
        Border b = new Border(strokes, null);
        assertEquals(2, b.getStrokes().size(), 0);
        assertEquals(strokes[0], b.getStrokes().get(0));
        assertEquals(strokes[1], b.getStrokes().get(1));
    }

    @Test public void instanceCreationWithNullsInTheFillArray4() {
        final BorderStroke[] strokes = new BorderStroke[] {
                null
        };
        Border b = new Border(strokes, null);
        assertEquals(0, b.getStrokes().size(), 0);
    }

    @Test public void instanceCreationWithNullsInTheImageArray() {
        final BorderImage[] images = new BorderImage[] {
                null,
                new BorderImage(IMAGE_2, new BorderWidths(3), new Insets(4), BorderWidths.EMPTY, false, REPEAT, REPEAT),
                new BorderImage(IMAGE_3, new BorderWidths(6), new Insets(2), BorderWidths.EMPTY, false, SPACE, ROUND),
                new BorderImage(IMAGE_4, new BorderWidths(3), Insets.EMPTY, new BorderWidths(3, 4, 5, 6), true, STRETCH, SPACE)
        };
        Border b = new Border(null, images);
        assertEquals(IMAGES_2.length, b.getImages().size(), 0);
        assertEquals(IMAGES_2[0], b.getImages().get(0));
        assertEquals(IMAGES_2[1], b.getImages().get(1));
        assertEquals(IMAGES_2[2], b.getImages().get(2));
    }

    @Test public void instanceCreationWithNullsInTheImageArray2() {
        final BorderImage[] images = new BorderImage[] {
                new BorderImage(IMAGE_2, new BorderWidths(3), new Insets(4), BorderWidths.EMPTY, false, REPEAT, REPEAT),
                null,
                new BorderImage(IMAGE_3, new BorderWidths(6), new Insets(2), BorderWidths.EMPTY, false, SPACE, ROUND),
                new BorderImage(IMAGE_4, new BorderWidths(3), Insets.EMPTY, new BorderWidths(3, 4, 5, 6), true, STRETCH, SPACE)
        };
        Border b = new Border(null, images);
        assertEquals(IMAGES_2.length, b.getImages().size(), 0);
        assertEquals(IMAGES_2[0], b.getImages().get(0));
        assertEquals(IMAGES_2[1], b.getImages().get(1));
        assertEquals(IMAGES_2[2], b.getImages().get(2));
    }

    @Test public void instanceCreationWithNullsInTheImageArray3() {
        final BorderImage[] images = new BorderImage[] {
                new BorderImage(IMAGE_2, new BorderWidths(3), new Insets(4), BorderWidths.EMPTY, false, REPEAT, REPEAT),
                new BorderImage(IMAGE_3, new BorderWidths(6), new Insets(2), BorderWidths.EMPTY, false, SPACE, ROUND),
                new BorderImage(IMAGE_4, new BorderWidths(3), Insets.EMPTY, new BorderWidths(3, 4, 5, 6), true, STRETCH, SPACE),
                null
        };
        Border b = new Border(null, images);
        assertEquals(IMAGES_2.length, b.getImages().size(), 0);
        assertEquals(IMAGES_2[0], b.getImages().get(0));
        assertEquals(IMAGES_2[1], b.getImages().get(1));
        assertEquals(IMAGES_2[2], b.getImages().get(2));
    }

    @Test public void instanceCreationWithNullsInTheImageArray4() {
        final BorderImage[] images = new BorderImage[] {
                null
        };
        Border b = new Border(null, images);
        assertEquals(0, b.getImages().size(), 0);
    }

    @Test public void suppliedBorderStrokesMutatedLaterDoNotChangeStrokes() {
        final BorderStroke stroke = new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, new CornerRadii(3), new BorderWidths(4));
        final BorderStroke[] strokes = new BorderStroke[] { stroke };
        Border b = new Border(strokes, null);
        Border b2 = new Border(strokes);
        strokes[0] = null;
        assertEquals(1, b.getStrokes().size());
        assertEquals(1, b2.getStrokes().size());
        assertSame(stroke, b.getStrokes().get(0));
        assertSame(stroke, b2.getStrokes().get(0));
    }

    @Test public void suppliedBorderImagesMutatedLaterDoNotChangeImages() {
        final BorderImage image = new BorderImage(IMAGE_2, new BorderWidths(3), new Insets(4),
                                                  BorderWidths.EMPTY, false, REPEAT, REPEAT);
        final BorderImage[] images = new BorderImage[] { image };
        Border b = new Border(null, images);
        Border b2 = new Border(images);
        images[0] = null;
        assertEquals(1, b.getImages().size());
        assertEquals(1, b2.getImages().size());
        assertSame(image, b.getImages().get(0));
        assertSame(image, b2.getImages().get(0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void strokesIsUnmodifiable() {
        final BorderStroke stroke = new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, new CornerRadii(3), new BorderWidths(4));
        final BorderStroke[] strokes = new BorderStroke[] { stroke };
        Border b = new Border(strokes);
        b.getStrokes().add(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(8)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void imagesIsUnmodifiable() {
        final BorderImage image = new BorderImage(IMAGE_2, new BorderWidths(3), new Insets(4),
                                                  BorderWidths.EMPTY, false, REPEAT, REPEAT);
        final BorderImage[] images = new BorderImage[] { image };
        Border b = new Border(images);
        b.getImages().add(
                new BorderImage(
                        IMAGE_4, new BorderWidths(3), Insets.EMPTY,
                        new BorderWidths(3, 4, 5, 6), true, STRETCH, SPACE));
    }

    @Test public void insetsAndOutsets_twoPixelOuterStroke() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.OUTSIDE, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(2));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        // This is a 2 pixel solid stroke painted on the OUTSIDE. This means that the
        // outsets should be 2 pixel.
        assertEquals(new Insets(2), border.getOutsets());
        // The insets should be 0, because the stroke is on the OUTSIDE
        assertEquals(new Insets(0), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelOuterStroke() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.OUTSIDE, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(1));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        // This is a 1 pixel solid stroke painted on the OUTSIDE. This means that the
        // outsets should be 1 pixel.
        assertEquals(new Insets(1), border.getOutsets());
        // The insets should be 0, because the stroke is on the OUTSIDE
        assertEquals(new Insets(0), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelCenteredStroke() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.CENTERED, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(1));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        // This is a 1 pixel solid stroke painted CENTERED. This means that the
        // outsets should be .5 pixel.
        assertEquals(new Insets(.5), border.getOutsets());
        // The insets should be .5, because the stroke is CENTERED
        assertEquals(new Insets(.5), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelInnerStroke() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.INSIDE, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(1));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        // This is a 1 pixel solid stroke painted on the INSIDE. This means that the
        // outsets should be 0 pixel.
        assertEquals(new Insets(0), border.getOutsets());
        // The insets should be 1, because the stroke is on the INSIDE
        assertEquals(new Insets(1), border.getInsets());
    }

    @Test public void insetsAndOutsets_twoPixelOuterStroke_PositiveInsets() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.OUTSIDE, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(2), new Insets(1, 2, 3, 4));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        assertEquals(new Insets(1, 0, 0, 0), border.getOutsets());
        assertEquals(new Insets(1, 2, 3, 4), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelOuterStroke_PositiveInsets() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.OUTSIDE, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(1), new Insets(1, 2, 3, 4));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        assertEquals(new Insets(0), border.getOutsets());
        assertEquals(new Insets(1, 2, 3, 4), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelCenteredStroke_PositiveInsets() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.CENTERED, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(1), new Insets(1, 2, 3, 4));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        assertEquals(new Insets(0), border.getOutsets());
        assertEquals(new Insets(1.5, 2.5, 3.5, 4.5), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelInnerStroke_PositiveInsets() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.INSIDE, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(1), new Insets(1, 2, 3, 4));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        assertEquals(new Insets(0), border.getOutsets());
        assertEquals(new Insets(2, 3, 4, 5), border.getInsets());
    }

    @Test public void insetsAndOutsets_twoPixelOuterStroke_NegativeInsets() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.OUTSIDE, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(2), new Insets(-1, -2, -3, -4));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        assertEquals(new Insets(3, 4, 5, 6), border.getOutsets());
        assertEquals(new Insets(0), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelOuterStroke_NegativeInsets() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.OUTSIDE, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(1), new Insets(-1, -2, -3, -4));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        assertEquals(new Insets(2, 3, 4, 5), border.getOutsets());
        assertEquals(new Insets(0), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelCenteredStroke_NegativeInsets() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.CENTERED, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(1), new Insets(-1, -2, -3, -4));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        assertEquals(new Insets(1.5, 2.5, 3.5, 4.5), border.getOutsets());
        assertEquals(new Insets(0), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelInnerStroke_NegativeInsets() {
        BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.INSIDE, null, null, 0, 0, null);
        BorderStroke stroke = new BorderStroke(Color.ORANGE, style, new CornerRadii(5), new BorderWidths(1), new Insets(-1, -2, -3, -4));
        Border border = new Border(new BorderStroke[] { stroke }, null);
        assertEquals(new Insets(1, 2, 3, 4), border.getOutsets());
        assertEquals(new Insets(0), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelImageWidthNoInsets() {
        BorderImage image = new BorderImage(IMAGE_1, new BorderWidths(1), new Insets(0), null, false, null, null);
        Border border = new Border(null, new BorderImage[] { image });
        assertEquals(new Insets(0), border.getOutsets());
        assertEquals(new Insets(1), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelImageWidthWithPositiveInsets() {
        BorderImage image = new BorderImage(IMAGE_1, new BorderWidths(1), new Insets(10), null, false, null, null);
        Border border = new Border(null, new BorderImage[] { image });
        assertEquals(new Insets(0), border.getOutsets());
        assertEquals(new Insets(11), border.getInsets());
    }

    @Test public void insetsAndOutsets_singlePixelImageWidthWithNegativeInsets() {
        BorderImage image = new BorderImage(IMAGE_1, new BorderWidths(1), new Insets(-10), null, false, null, null);
        Border border = new Border(null, new BorderImage[] { image });
        assertEquals(new Insets(10), border.getOutsets());
        assertEquals(new Insets(0), border.getInsets());
    }

    @Test public void insetsAndOutsets_triplePixelImageWidthWithNegativeInsets() {
        BorderImage image = new BorderImage(IMAGE_1, new BorderWidths(3), new Insets(-1), null, false, null, null);
        Border border = new Border(null, new BorderImage[] { image });
        assertEquals(new Insets(1), border.getOutsets());
        assertEquals(new Insets(2), border.getInsets());
    }

    @Test public void equivalent() {
        Border a = new Border((BorderStroke[])null, null);
        Border b = new Border((BorderStroke[])null, null);
        assertEquals(a, b);
    }

    @Test public void equivalent2() {
        Border a = new Border(STROKES_2, null);
        Border b = new Border(STROKES_2, null);
        assertEquals(a, b);
    }

    @Test public void equivalent2b() {
        final BorderStroke[] strokes = new BorderStroke[] {
            new BorderStroke(Color.GREEN, BorderStrokeStyle.DASHED, new CornerRadii(3), new BorderWidths(4)),
            new BorderStroke(Color.BLUE, BorderStrokeStyle.DOTTED, new CornerRadii(6), new BorderWidths(8))
        };

        Border a = new Border(STROKES_2, null);
        Border b = new Border(strokes, null);

        assertEquals(a, b);
    }

    @Test public void equivalent2c() {
        final BorderStroke[] strokes = new BorderStroke[] {
            new BorderStroke(Color.GREEN, BorderStrokeStyle.DASHED, new CornerRadii(3), new BorderWidths(4)),
            new BorderStroke(Color.BLUE, BorderStrokeStyle.DOTTED, new CornerRadii(6), new BorderWidths(8))
        };
        Border a = new Border(STROKES_2, null);
        Border b = new Border(strokes, null);
        assertEquals(a, b);
    }

    @Test public void equivalent3() {
        Border a = new Border(null, IMAGES_2);
        Border b = new Border(null, IMAGES_2);
        assertEquals(a, b);
    }

    @Test public void equivalent3b() {
        final BorderImage[] images = new BorderImage[] {
                new BorderImage(IMAGE_2, new BorderWidths(3), new Insets(4), BorderWidths.EMPTY, false, REPEAT, REPEAT),
                new BorderImage(IMAGE_3, new BorderWidths(6), new Insets(2), BorderWidths.EMPTY, false, SPACE, ROUND),
                new BorderImage(IMAGE_4, new BorderWidths(3), Insets.EMPTY, new BorderWidths(3, 4, 5, 6), true, STRETCH, SPACE)
        };
        Border a = new Border(null, images);
        Border b = new Border(null, IMAGES_2);
        assertEquals(a, b);
    }

    @Test public void equivalent3c() {
        final BorderImage[] images = new BorderImage[] {
                new BorderImage(IMAGE_2, new BorderWidths(3), new Insets(4), BorderWidths.EMPTY, false, REPEAT, REPEAT),
                new BorderImage(IMAGE_3, new BorderWidths(6), new Insets(2), BorderWidths.EMPTY, false, SPACE, ROUND),
                new BorderImage(IMAGE_4, new BorderWidths(3), Insets.EMPTY, new BorderWidths(3, 4, 5, 6), true, STRETCH, SPACE)
        };
        Border a = new Border(null, images);
        Border b = new Border(null, IMAGES_2);
        assertEquals(a, b);
    }

    @Test public void equivalentWithSelf() {
        Border a = new Border(null, IMAGES_2);
        assertTrue(a.equals(a));
    }

    @Test public void equivalentHasSameHashCode() {
        Border a = new Border((BorderStroke[])null, null);
        Border b = new Border((BorderStroke[])null, null);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHasSameHashCode2() {
        Border a = new Border(STROKES_2, null);
        Border b = new Border(STROKES_2, null);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHasSameHashCode3() {
        Border a = new Border(null, IMAGES_2);
        Border b = new Border(null, IMAGES_2);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentWithSelfHashCode() {
        Border a = new Border(null, IMAGES_2);
        assertEquals(a.hashCode(), a.hashCode());
    }

    @Test public void notEqual() {
        Border a = new Border(STROKES_1, null);
        Border b = new Border((BorderStroke[])null, null);
        assertFalse(a.equals(b));
    }

    @Test public void notEqual2() {
        Border a = new Border((BorderStroke[])null, null);
        Border b = new Border(STROKES_2, null);
        assertFalse(a.equals(b));
    }

    @Test public void notEqual3() {
        Border a = new Border(null, IMAGES_1);
        Border b = new Border((BorderStroke[])null, null);
        assertFalse(a.equals(b));
    }

    @Test public void notEqual4() {
        Border a = new Border((BorderStroke[])null, null);
        Border b = new Border(null, IMAGES_2);
        assertFalse(a.equals(b));
    }

    @Test public void notEqual5() {
        Border a = new Border(null, IMAGES_1);
        Border b = new Border(null, IMAGES_2);
        assertFalse(a.equals(b));
    }

    @Test public void notEqual6() {
        Border a = new Border(STROKES_1, null);
        Border b = new Border(STROKES_2, null);
        assertFalse(a.equals(b));
    }

    @Test public void notEqualButHaveSameHashCode() {
        Border a = new Border(STROKES_1, null);
        Border b = new Border(STROKES_2, null);
        // Because Border is final, the only way to test this
        // appropriately is to use reflection to set the hash
        try {
            Field f = Border.class.getDeclaredField("hash");
            f.setAccessible(true);
            f.set(a, 100);
            f.set(b, 100);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Failed to reflectively set the hash value");
        }

        assertFalse(a.equals(b));
    }

    @Test public void notEqualButHaveSameHashCode2() {
        Border a = new Border(null, IMAGES_1);
        Border b = new Border(null, IMAGES_2);
        // Because Border is final, the only way to test this
        // appropriately is to use reflection to set the hash
        try {
            Field f = Border.class.getDeclaredField("hash");
            f.setAccessible(true);
            f.set(a, 100);
            f.set(b, 100);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Failed to reflectively set the hash value");
        }

        assertFalse(a.equals(b));
    }

    @Test public void notEqualWithNull() {
        Border a = new Border((BorderStroke[])null, null);
        assertFalse(a.equals(null));
    }

    @Test public void notEqualWithRandom() {
        Border a = new Border((BorderStroke[])null, null);
        assertFalse(a.equals("Some random String"));
    }
}

/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.paint.Color;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for Region picking. By default, Region has pickOnBounds set to true, so picking
 * anything within the bounds of the region will return true, anything outside the bounds
 * false. However, due to RT-25066, the bounds of a region defined by a shape will be 0x0,
 * in which case it will never be picked.
 *
 * If pickOnBounds is false, then an entire different code path is executed. We don't care
 * whether a fill is transparent or not (transparent fills can still cause the region to
 * pick), but we do care about picking within the "shape" and picking within the rounded
 * rectangle which is created as a result of background fills / background images.
 *
 * In the case of background images, we don't care about whether we are picking a transparent
 * pixel or not, but just based on the insets etc (since we don't have a good means for testing
 * the image pixel value).
 */
public class RegionPickTest {
    private static final double X = 0;
    private static final double Y = 0;
    private static final double WIDTH = 100;
    private static final double HEIGHT = 100;
    private static final double CENTER_X = X + (WIDTH / 2.0);
    private static final double CENTER_Y = Y + (HEIGHT / 2.0);
    private static final double LEFT_OF = X - 10;
    private static final double ABOVE = Y - 10;
    private static final double RIGHT_OF = X + WIDTH + 10;
    private static final double BELOW = Y + HEIGHT + 10;

    private Region region;

    @Before public void setup() {
        region = new Region();
        region.resizeRelocate(X, Y, WIDTH, HEIGHT);
        region.setPickOnBounds(false);
    }

    /**************************************************************************
     *                                                                        *
     * Set of tests to ensure that picking within / without a region with     *
     * pickOnBounds set to true (the normal default) results in expected      *
     * behavior (pick when within bounds, not when without bounds)            *
     *                                                                        *
     *************************************************************************/

    @Test public void pickingNormalRegion() {
        region.setPickOnBounds(true);
        assertFalse(region.contains(LEFT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, ABOVE));
        assertFalse(region.contains(RIGHT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, BELOW));
        assertTrue(region.contains(CENTER_X, CENTER_Y));
    }

    /**************************************************************************
     *                                                                        *
     * Test for a Region which has no fills of any kind, but has              *
     * pickOnBounds set to false. Such a Region should never pick.            *
     *                                                                        *
     *************************************************************************/

    @Test public void pickingEmptyRegionDoesNotWork() {
        assertFalse(region.contains(LEFT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, ABOVE));
        assertFalse(region.contains(RIGHT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, BELOW));
        assertFalse(region.contains(CENTER_X, CENTER_Y));
    }

    /**************************************************************************
     *                                                                        *
     * Test behavior when picking a region with fills, but no shape or border *
     * or images.                                                             *
     *                                                                        *
     *************************************************************************/

    @Test public void pickingRectangularFillWorks() {
        region.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
        assertFalse(region.contains(LEFT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, ABOVE));
        assertFalse(region.contains(RIGHT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, BELOW));
        assertTrue(region.contains(CENTER_X, CENTER_Y));
    }

    @Test public void pickingRectangularFillWithInsetsWorks() {
        // With insets of 10, we ought to not pick inside the region until we get to position 10
        region.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, new Insets(10))));
        assertFalse(region.contains(X + 9, CENTER_Y));
        assertFalse(region.contains(CENTER_X, Y + 9));
        assertFalse(region.contains(X + WIDTH - 9, CENTER_Y));
        assertFalse(region.contains(CENTER_X, Y + HEIGHT - 9));
        assertTrue(region.contains(X + 10, CENTER_Y));
        assertTrue(region.contains(CENTER_X, Y + 10));
        assertTrue(region.contains(X + WIDTH - 10, CENTER_Y));
        assertTrue(region.contains(CENTER_X, Y + HEIGHT - 10));
        assertTrue(region.contains(CENTER_X, CENTER_Y));
    }

    @Test public void pickingRectangularFillWithUniformRadiusWorks() {
        region.setBackground(new Background(new BackgroundFill(Color.RED, new CornerRadii(10), Insets.EMPTY)));
        // Check points in the top-left corner area
        assertTrue(region.contains(X, Y + 10));
        assertTrue(region.contains(X + 10, Y));
        assertTrue(region.contains(X + 10 - (10 * Math.cos(45)), Y + 10 - (10 * Math.sin(45))));
        assertTrue(region.contains(X + 10 - (9 * Math.cos(45)), Y + 10 - (9 * Math.sin(45))));
        assertFalse(region.contains(X + 10 - (11 * Math.cos(45)), Y + 10 - (11 * Math.sin(45))));
        // Check points in the top-right corner area
        assertTrue(region.contains(X + WIDTH, Y + 10));
        assertTrue(region.contains(X + WIDTH - 10, Y));
        assertTrue(region.contains(X + WIDTH - 10 + (10 * Math.cos(45)), Y + 10 - (10 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 10 + (9 * Math.cos(45)), Y + 10 - (9 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 10 + (11 * Math.cos(45)), Y + 10 - (11 * Math.sin(45))));
        // Check points in the bottom-right corner area
        assertTrue(region.contains(X + WIDTH, Y + HEIGHT - 10));
        assertTrue(region.contains(X + WIDTH - 10, Y + HEIGHT));
        assertTrue(region.contains(X + WIDTH - 10 + (10 * Math.cos(45)), Y + HEIGHT - 10 + (10 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 10 + (9 * Math.cos(45)), Y + HEIGHT - 10 + (9 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 10 + (11 * Math.cos(45)), Y + HEIGHT - 10 + (11 * Math.sin(45))));
        // Check points in the bottom-left corner area
        assertTrue(region.contains(X, Y + HEIGHT - 10));
        assertTrue(region.contains(X + 10, Y + HEIGHT));
        assertTrue(region.contains(X + 10 - (10 * Math.cos(45)), Y + HEIGHT - 10 + (10 * Math.sin(45))));
        assertTrue(region.contains(X + 10 - (9 * Math.cos(45)), Y + HEIGHT - 10 + (9 * Math.sin(45))));
        assertFalse(region.contains(X + 10 - (11 * Math.cos(45)), Y + HEIGHT - 10 + (11 * Math.sin(45))));
        // Check the center
        assertTrue(region.contains(CENTER_X, CENTER_Y));
    }

    @Test public void pickingRectangularFillWithUniformRadiusWithInsetsWorks() {
        region.setBackground(new Background(new BackgroundFill(Color.RED, new CornerRadii(10), new Insets(10))));
        // Check points in the top-left corner area
        assertTrue(region.contains(X + 10, Y + 20));
        assertTrue(region.contains(X + 20, Y + 10));
        assertTrue(region.contains(X + 20 - (10 * Math.cos(45)), Y + 20 - (10 * Math.sin(45))));
        assertTrue(region.contains(X + 20 - (9 * Math.cos(45)), Y + 20 - (9 * Math.sin(45))));
        assertFalse(region.contains(X + 20 - (11 * Math.cos(45)), Y + 20 - (11 * Math.sin(45))));
        // Check points in the top-right corner area
        assertTrue(region.contains(X + WIDTH - 10, Y + 20));
        assertTrue(region.contains(X + WIDTH - 20, Y + 10));
        assertTrue(region.contains(X + WIDTH - 20 + (10 * Math.cos(45)), Y + 20 - (10 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 20 + (9 * Math.cos(45)), Y + 20 - (9 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 20 + (11 * Math.cos(45)), Y + 20 - (11 * Math.sin(45))));
        // Check points in the bottom-right corner area
        assertTrue(region.contains(X + WIDTH - 10, Y + HEIGHT - 20));
        assertTrue(region.contains(X + WIDTH - 20, Y + HEIGHT - 10));
        assertTrue(region.contains(X + WIDTH - 20 + (10 * Math.cos(45)), Y + HEIGHT - 20 + (10 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 20 + (9 * Math.cos(45)), Y + HEIGHT - 20 + (9 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 20 + (11 * Math.cos(45)), Y + HEIGHT - 20 + (11 * Math.sin(45))));
        // Check points in the bottom-left corner area
        assertTrue(region.contains(X + 10, Y + HEIGHT - 20));
        assertTrue(region.contains(X + 20, Y + HEIGHT - 10));
        assertTrue(region.contains(X + 20 - (10 * Math.cos(45)), Y + HEIGHT - 20 + (10 * Math.sin(45))));
        assertTrue(region.contains(X + 20 - (9 * Math.cos(45)), Y + HEIGHT - 20 + (9 * Math.sin(45))));
        assertFalse(region.contains(X + 20 - (11 * Math.cos(45)), Y + HEIGHT - 20 + (11 * Math.sin(45))));
        // Check the center
        assertTrue(region.contains(CENTER_X, CENTER_Y));
    }

    // test with really really large corner radius
    @Test public void pickingRectangularFillWithUniformVERYLARGERadiusWorks() {
        region.setBackground(new Background(new BackgroundFill(Color.RED, new CornerRadii(10000000), Insets.EMPTY)));
        // This produces an effective radius of 50 due to my width/height being 100x100
        // Check points in the top-left corner area
        assertTrue(region.contains(X, Y + 50));
        assertTrue(region.contains(X + 50, Y));
        assertTrue(region.contains(X + 50 - (50 * Math.cos(45)), Y + 50 - (50 * Math.sin(45))));
        assertTrue(region.contains(X + 50 - (49 * Math.cos(45)), Y + 50 - (49 * Math.sin(45))));
        assertFalse(region.contains(X + 50 - (51 * Math.cos(45)), Y + 50 - (51 * Math.sin(45))));
        // Check points in the top-right corner area
        assertTrue(region.contains(X + WIDTH, Y + 50));
        assertTrue(region.contains(X + WIDTH - 50, Y));
        assertTrue(region.contains(X + WIDTH - 50 + (50 * Math.cos(45)), Y + 50 - (50 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 50 + (49 * Math.cos(45)), Y + 50 - (49 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 50 + (51 * Math.cos(45)), Y + 50 - (51 * Math.sin(45))));
        // Check points in the bottom-right corner area
        assertTrue(region.contains(X + WIDTH, Y + HEIGHT - 50));
        assertTrue(region.contains(X + WIDTH - 50, Y + HEIGHT));
        assertTrue(region.contains(X + WIDTH - 50 + (50 * Math.cos(45)), Y + HEIGHT - 50 + (50 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 50 + (49 * Math.cos(45)), Y + HEIGHT - 50 + (49 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 50 + (51 * Math.cos(45)), Y + HEIGHT - 50 + (51 * Math.sin(45))));
        // Check points in the bottom-left corner area
        assertTrue(region.contains(X, Y + HEIGHT - 50));
        assertTrue(region.contains(X + 50, Y + HEIGHT));
        assertTrue(region.contains(X + 50 - (50 * Math.cos(45)), Y + HEIGHT - 50 + (50 * Math.sin(45))));
        assertTrue(region.contains(X + 50 - (49 * Math.cos(45)), Y + HEIGHT - 50 + (49 * Math.sin(45))));
        assertFalse(region.contains(X + 50 - (51 * Math.cos(45)), Y + HEIGHT - 50 + (51 * Math.sin(45))));
        // Check the center
        assertTrue(region.contains(CENTER_X, CENTER_Y));
    }

    @Test public void pickingRectangularFillWithIndependentRadiusWorks() {
        region.setBackground(new Background(new BackgroundFill(Color.RED, new CornerRadii(1, 2, 3, 4, false),
                                                               Insets.EMPTY)));
        // Check points in the top-left corner area
        assertTrue(region.contains(X, Y + 1));
        assertTrue(region.contains(X + 1, Y));
        assertTrue(region.contains(X + 1 - (1 * Math.cos(45)), Y + 1 - (1 * Math.sin(45))));
        assertTrue(region.contains(X + 1 - (.5 * Math.cos(45)), Y + 1 - (.5 * Math.sin(45))));
        assertFalse(region.contains(X + 1 - (2 * Math.cos(45)), Y + 1 - (2 * Math.sin(45))));
        // Check points in the top-right corner area
        assertTrue(region.contains(X + WIDTH, Y + 2));
        assertTrue(region.contains(X + WIDTH - 2, Y));
        assertTrue(region.contains(X + WIDTH - 2 + (2 * Math.cos(45)), Y + 2 - (2 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 2 + (1 * Math.cos(45)), Y + 2 - (1 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 2 + (3 * Math.cos(45)), Y + 2 - (3 * Math.sin(45))));
        // Check points in the bottom-right corner area
        assertTrue(region.contains(X + WIDTH, Y + HEIGHT - 3));
        assertTrue(region.contains(X + WIDTH - 3, Y + HEIGHT));
        assertTrue(region.contains(X + WIDTH - 3 + (3 * Math.cos(45)), Y + HEIGHT - 3 + (3 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 3 + (2 * Math.cos(45)), Y + HEIGHT - 3 + (2 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 3 + (4 * Math.cos(45)), Y + HEIGHT - 3 + (4 * Math.sin(45))));
        // Check points in the bottom-left corner area
        assertTrue(region.contains(X, Y + HEIGHT - 4));
        assertTrue(region.contains(X + 4, Y + HEIGHT));
        assertTrue(region.contains(X + 4 - (4 * Math.cos(45)), Y + HEIGHT - 4 + (4 * Math.sin(45))));
        assertTrue(region.contains(X + 4 - (3 * Math.cos(45)), Y + HEIGHT - 4 + (3 * Math.sin(45))));
        assertFalse(region.contains(X + 4 - (5 * Math.cos(45)), Y + HEIGHT - 4 + (5 * Math.sin(45))));
        // Check the center
        assertTrue(region.contains(CENTER_X, CENTER_Y));
    }

    @Test public void pickingRectangularFillWithIndependentRadiusWorks2() {
        region.setBackground(new Background(new BackgroundFill(Color.RED,
            new CornerRadii(1, 2, 3, 4, 5, 6, 7, 8, false, false, false, false, false, false, false, false),
            Insets.EMPTY)));
        // Check points in the top-left corner area
        assertTrue(region.contains(X, Y + 2));
        assertTrue(region.contains(X + 1, Y));
        assertTrue(region.contains(X + 1 - (1 * Math.cos(45)), Y + 2 - (2 * Math.sin(45))));
        assertTrue(region.contains(X + 1 - (.5 * Math.cos(45)), Y + 2 - (1 * Math.sin(45))));
        assertFalse(region.contains(X + 1 - (2 * Math.cos(45)), Y + 2 - (3 * Math.sin(45))));
        // Check points in the top-right corner area
        assertTrue(region.contains(X + WIDTH, Y + 3));
        assertTrue(region.contains(X + WIDTH - 4, Y));
        assertTrue(region.contains(X + WIDTH - 4 + (4 * Math.cos(45)), Y + 3 - (3 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 4 + (3 * Math.cos(45)), Y + 3 - (2 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 4 + (5 * Math.cos(45)), Y + 3 - (4 * Math.sin(45))));
        // Check points in the bottom-right corner area
        assertTrue(region.contains(X + WIDTH, Y + HEIGHT - 6));
        assertTrue(region.contains(X + WIDTH - 5, Y + HEIGHT));
        assertTrue(region.contains(X + WIDTH - 5 + (5 * Math.cos(45)), Y + HEIGHT - 6 + (6 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 5 + (4 * Math.cos(45)), Y + HEIGHT - 6 + (5 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 5 + (6 * Math.cos(45)), Y + HEIGHT - 6 + (7 * Math.sin(45))));
        // Check points in the bottom-left corner area
        assertTrue(region.contains(X, Y + HEIGHT - 7));
        assertTrue(region.contains(X + 8, Y + HEIGHT));
        assertTrue(region.contains(X + 8 - (8 * Math.cos(45)), Y + HEIGHT - 7 + (7 * Math.sin(45))));
        assertTrue(region.contains(X + 8 - (7 * Math.cos(45)), Y + HEIGHT - 7 + (6 * Math.sin(45))));
        assertFalse(region.contains(X + 8 - (9 * Math.cos(45)), Y + HEIGHT - 7 + (8 * Math.sin(45))));
        // Check the center
        assertTrue(region.contains(CENTER_X, CENTER_Y));
    }

    @Test public void pickingRectangularFillWithIndependentRadiusWithInsetsWorks() {
        region.setBackground(new Background(new BackgroundFill(Color.RED,
            new CornerRadii(1, 2, 3, 4, 5, 6, 7, 8, false, false, false, false, false, false, false, false),
            new Insets(4, 3, 2, 1))));
        // Check points in the top-left corner area
        assertTrue(region.contains(X + 1, Y + 2 + 4));
        assertTrue(region.contains(X + 1 + 1, Y + 4));
        assertTrue(region.contains(X + 1 + 1 - (1 * Math.cos(45)), Y + 2 + 4 - (2 * Math.sin(45))));
        assertTrue(region.contains(X + 1 + 1 - (.5 * Math.cos(45)), Y + 2 + 4 - (1 * Math.sin(45))));
        assertFalse(region.contains(X + 1 + 1 - (2 * Math.cos(45)), Y + 2 + 4 - (3 * Math.sin(45))));
        // Check points in the top-right corner area
        assertTrue(region.contains(X + WIDTH - 3, Y + 3 + 4));
        assertTrue(region.contains(X + WIDTH - 4 - 3, Y + 4));
        assertTrue(region.contains(X + WIDTH - 4 - 3 + (4 * Math.cos(45)), Y + 4 + 3 - (3 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 4 - 3 + (3 * Math.cos(45)), Y + 4 + 3 - (2 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 4 - 3 + (5 * Math.cos(45)), Y + 4 + 3 - (4 * Math.sin(45))));
        // Check points in the bottom-right corner area
        assertTrue(region.contains(X + WIDTH - 3, Y + HEIGHT - 2 - 6));
        assertTrue(region.contains(X + WIDTH - 3 - 5, Y + HEIGHT - 2));
        assertTrue(region.contains(X + WIDTH - 3 - 5 + (5 * Math.cos(45)), Y + HEIGHT - 2 - 6 + (6 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 3 - 5 + (4 * Math.cos(45)), Y + HEIGHT - 2 - 6 + (5 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 3 - 5 + (6 * Math.cos(45)), Y + HEIGHT - 2 - 6 + (7 * Math.sin(45))));
        // Check points in the bottom-left corner area
        assertTrue(region.contains(X + 1, Y + HEIGHT - 2 - 7));
        assertTrue(region.contains(X + 1 + 8, Y + HEIGHT - 2));
        assertTrue(region.contains(X + 1 + 8 - (8 * Math.cos(45)), Y + HEIGHT - 2 - 7 + (7 * Math.sin(45))));
        assertTrue(region.contains(X + 1 + 8 - (7 * Math.cos(45)), Y + HEIGHT - 2 - 7 + (6 * Math.sin(45))));
        assertFalse(region.contains(X + 1 + 8 - (9 * Math.cos(45)), Y + HEIGHT - 2 - 7 + (8 * Math.sin(45))));
        // Check the center
        assertTrue(region.contains(CENTER_X, CENTER_Y));
    }

    // TODO test with really really large corner radius

    /**************************************************************************
     *                                                                        *
     * Test behavior when picking a region with borders, but no shape or      *
     * or images.                                                             *
     *                                                                        *
     *************************************************************************/

    @Test public void pickingRectangularBorderWorks() {
        region.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                                                     new BorderWidths(1))));
        assertFalse(region.contains(LEFT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, ABOVE));
        assertFalse(region.contains(RIGHT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, BELOW));
        // Note that the center is empty and should not be picked
        assertFalse(region.contains(CENTER_X, CENTER_Y));
    }

    @Test public void pickingRectangularBorderWithThickBorder() {
        region.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                                                     new BorderWidths(10))));
        assertFalse(region.contains(LEFT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, ABOVE));
        assertFalse(region.contains(RIGHT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, BELOW));
        assertFalse(region.contains(CENTER_X, CENTER_Y));

        assertTrue(region.contains(X, Y));
        assertTrue(region.contains(X+5, Y+5));
        assertFalse(region.contains(X+10, Y+10));
    }

    @Test public void pickingRectangularBorderWithIndependentBorderWidths() {
        region.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                                                     new BorderWidths(5, 10, 15, 20))));
        assertFalse(region.contains(LEFT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, ABOVE));
        assertFalse(region.contains(RIGHT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, BELOW));
        assertFalse(region.contains(CENTER_X, CENTER_Y));

        // Top. Test first and last pixels, and one-past
        assertTrue(region.contains(CENTER_X, Y));
        assertTrue(region.contains(CENTER_X, Y + 4));
        assertFalse(region.contains(CENTER_X, Y + 5));

        // Right. Test first and last pixels, and one-past
        assertTrue(region.contains(WIDTH, CENTER_Y));
        assertTrue(region.contains(WIDTH - 9, CENTER_Y));
        assertFalse(region.contains(WIDTH - 10, CENTER_Y));

        // Bottom. Test first and last pixels, and one-past
        assertTrue(region.contains(CENTER_X, HEIGHT));
        assertTrue(region.contains(CENTER_X, HEIGHT - 14));
        assertFalse(region.contains(CENTER_X, HEIGHT - 15));

        // Left. Test first and last pixels, and one-past
        assertTrue(region.contains(X, CENTER_Y));
        assertTrue(region.contains(X + 19, CENTER_Y));
        assertFalse(region.contains(X + 20, CENTER_Y));
    }

    @Test public void pickingRectangularBorderWithIndependentPercentageBorderWidths() {
        region.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                                                     new BorderWidths(.05, .10, .15, .20, true, true, true, true))));
        assertFalse(region.contains(LEFT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, ABOVE));
        assertFalse(region.contains(RIGHT_OF, CENTER_Y));
        assertFalse(region.contains(CENTER_X, BELOW));
        assertFalse(region.contains(CENTER_X, CENTER_Y));

        // Top. Test first and last pixels, and one-past
        assertTrue(region.contains(CENTER_X, Y));
        assertTrue(region.contains(CENTER_X, Y + 4));
        assertFalse(region.contains(CENTER_X, Y + 5));

        // Right. Test first and last pixels, and one-past
        assertTrue(region.contains(WIDTH, CENTER_Y));
        assertTrue(region.contains(WIDTH - 9, CENTER_Y));
        assertFalse(region.contains(WIDTH - 10, CENTER_Y));

        // Bottom. Test first and last pixels, and one-past
        assertTrue(region.contains(CENTER_X, HEIGHT));
        assertTrue(region.contains(CENTER_X, HEIGHT - 14));
        assertFalse(region.contains(CENTER_X, HEIGHT - 15));

        // Left. Test first and last pixels, and one-past
        assertTrue(region.contains(X, CENTER_Y));
        assertTrue(region.contains(X + 19, CENTER_Y));
        assertFalse(region.contains(X + 20, CENTER_Y));
    }

    @Test public void pickingRectangularBorderWithIndependentBorderWidthsAndInsets() {
        region.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                                                     new BorderWidths(5, 10, 15, 20), new Insets(1, 2, 3, 4))));
        // Top. Test first and last pixels, and one-past
        assertFalse(region.contains(CENTER_X, Y));
        assertTrue(region.contains(CENTER_X, Y+1));
        assertTrue(region.contains(CENTER_X, Y+1 + 4));
        assertFalse(region.contains(CENTER_X, Y+1 + 5));

        // Right. Test first and last pixels, and one-past
        assertFalse(region.contains(WIDTH-1, CENTER_Y));
        assertTrue(region.contains(WIDTH-2, CENTER_Y));
        assertTrue(region.contains(WIDTH-2 - 9, CENTER_Y));
        assertFalse(region.contains(WIDTH-2 - 10, CENTER_Y));

        // Bottom. Test first and last pixels, and one-past
        assertFalse(region.contains(CENTER_X, HEIGHT-2));
        assertTrue(region.contains(CENTER_X, HEIGHT-3));
        assertTrue(region.contains(CENTER_X, HEIGHT-3 - 14));
        assertFalse(region.contains(CENTER_X, HEIGHT-3 - 15));

        // Left. Test first and last pixels, and one-past
        assertFalse(region.contains(X+3, CENTER_Y));
        assertTrue(region.contains(X+4, CENTER_Y));
        assertTrue(region.contains(X+4 + 19, CENTER_Y));
        assertFalse(region.contains(X+4 + 20, CENTER_Y));
    }

    @Test public void pickingRectangularBorderWithIndependentRadiusWithInsetsWorks() {
        region.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID,
            new CornerRadii(1, 2, 3, 4, 5, 6, 7, 8, false, false, false, false, false, false, false, false),
            new BorderWidths(5, 10, 15, 20), new Insets(4, 3, 2, 1))));
        // Check points in the top-left corner area
        assertTrue(region.contains(X + 1, Y + 2 + 4));
        assertTrue(region.contains(X + 1 + 1, Y + 4));
        assertTrue(region.contains(X + 1 + 1 - (1 * Math.cos(45)), Y + 2 + 4 - (2 * Math.sin(45))));
        assertTrue(region.contains(X + 1 + 1 - (.5 * Math.cos(45)), Y + 2 + 4 - (1 * Math.sin(45))));
        assertFalse(region.contains(X + 1 + 1 - (2 * Math.cos(45)), Y + 2 + 4 - (3 * Math.sin(45))));
        // Check points in the top-right corner area
        assertTrue(region.contains(X + WIDTH - 3, Y + 3 + 4));
        assertTrue(region.contains(X + WIDTH - 4 - 3, Y + 4));
        assertTrue(region.contains(X + WIDTH - 4 - 3 + (4 * Math.cos(45)), Y + 4 + 3 - (3 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 4 - 3 + (3 * Math.cos(45)), Y + 4 + 3 - (2 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 4 - 3 + (5 * Math.cos(45)), Y + 4 + 3 - (4 * Math.sin(45))));
        // Check points in the bottom-right corner area
        assertTrue(region.contains(X + WIDTH - 3, Y + HEIGHT - 2 - 6));
        assertTrue(region.contains(X + WIDTH - 3 - 5, Y + HEIGHT - 2));
        assertTrue(region.contains(X + WIDTH - 3 - 5 + (5 * Math.cos(45)), Y + HEIGHT - 2 - 6 + (6 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 3 - 5 + (4 * Math.cos(45)), Y + HEIGHT - 2 - 6 + (5 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 3 - 5 + (6 * Math.cos(45)), Y + HEIGHT - 2 - 6 + (7 * Math.sin(45))));
        // Check points in the bottom-left corner area
        assertTrue(region.contains(X + 1, Y + HEIGHT - 2 - 7));
        assertTrue(region.contains(X + 1 + 8, Y + HEIGHT - 2));
        assertTrue(region.contains(X + 1 + 8 - (8 * Math.cos(45)), Y + HEIGHT - 2 - 7 + (7 * Math.sin(45))));
        assertTrue(region.contains(X + 1 + 8 - (7 * Math.cos(45)), Y + HEIGHT - 2 - 7 + (6 * Math.sin(45))));
        assertFalse(region.contains(X + 1 + 8 - (9 * Math.cos(45)), Y + HEIGHT - 2 - 7 + (8 * Math.sin(45))));
        // Check the center
        assertFalse(region.contains(CENTER_X, CENTER_Y));
        // TODO Could stand to have more tests testing the inside hit edge
    }

    @Test public void pickingRectangularBorderWithIndependentPercentageRadiusWithInsetsWorks() {
        region.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID,
            new CornerRadii(.01, .02, .03, .04, .05, .06, .07, .08, true, true, true, true, true, true, true, true),
            new BorderWidths(5, 10, 15, 20), new Insets(4, 3, 2, 1))));
        // Check points in the top-left corner area
        assertTrue(region.contains(X + 1, Y + 2 + 4));
        assertTrue(region.contains(X + 1 + 1, Y + 4));
        assertTrue(region.contains(X + 1 + 1 - (1 * Math.cos(45)), Y + 2 + 4 - (2 * Math.sin(45))));
        assertTrue(region.contains(X + 1 + 1 - (.5 * Math.cos(45)), Y + 2 + 4 - (1 * Math.sin(45))));
        assertFalse(region.contains(X + 1 + 1 - (2 * Math.cos(45)), Y + 2 + 4 - (3 * Math.sin(45))));
        // Check points in the top-right corner area
        assertTrue(region.contains(X + WIDTH - 3, Y + 3 + 4));
        assertTrue(region.contains(X + WIDTH - 4 - 3, Y + 4));
        assertTrue(region.contains(X + WIDTH - 4 - 3 + (4 * Math.cos(45)), Y + 4 + 3 - (3 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 4 - 3 + (3 * Math.cos(45)), Y + 4 + 3 - (2 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 4 - 3 + (5 * Math.cos(45)), Y + 4 + 3 - (4 * Math.sin(45))));
        // Check points in the bottom-right corner area
        assertTrue(region.contains(X + WIDTH - 3, Y + HEIGHT - 2 - 6));
        assertTrue(region.contains(X + WIDTH - 3 - 5, Y + HEIGHT - 2));
        assertTrue(region.contains(X + WIDTH - 3 - 5 + (5 * Math.cos(45)), Y + HEIGHT - 2 - 6 + (6 * Math.sin(45))));
        assertTrue(region.contains(X + WIDTH - 3 - 5 + (4 * Math.cos(45)), Y + HEIGHT - 2 - 6 + (5 * Math.sin(45))));
        assertFalse(region.contains(X + WIDTH - 3 - 5 + (6 * Math.cos(45)), Y + HEIGHT - 2 - 6 + (7 * Math.sin(45))));
        // Check points in the bottom-left corner area
        assertTrue(region.contains(X + 1, Y + HEIGHT - 2 - 7));
        assertTrue(region.contains(X + 1 + 8, Y + HEIGHT - 2));
        assertTrue(region.contains(X + 1 + 8 - (8 * Math.cos(45)), Y + HEIGHT - 2 - 7 + (7 * Math.sin(45))));
        assertTrue(region.contains(X + 1 + 8 - (7 * Math.cos(45)), Y + HEIGHT - 2 - 7 + (6 * Math.sin(45))));
        assertFalse(region.contains(X + 1 + 8 - (9 * Math.cos(45)), Y + HEIGHT - 2 - 7 + (8 * Math.sin(45))));
        // Check the center
        assertFalse(region.contains(CENTER_X, CENTER_Y));
        // TODO Could stand to have more tests testing the inside hit edge
    }

    /**************************************************************************
     *                                                                        *
     * Test behavior when picking a shaped region. We have to test all the    *
     * positionShape / scaleShape variants to make sure we are always picking *
     * based on the perceived (rendered) shape                                *
     *                                                                        *
     *************************************************************************/

    // TODO implement along with fix for RT-27775
}

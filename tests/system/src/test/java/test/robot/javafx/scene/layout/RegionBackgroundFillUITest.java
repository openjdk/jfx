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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.robot.javafx.scene.layout;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.util.concurrent.TimeUnit;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 */
@Timeout(value=20000, unit=TimeUnit.MILLISECONDS)
public class RegionBackgroundFillUITest extends RegionUITestBase {

    /**************************************************************************
     *                                                                        *
     * Tests for background fills. We start with a series of simple tests     *
     * with a single solid fill, including exercising different insets and    *
     * corner radii.                                                          *
     *                                                                        *
     *************************************************************************/

    final String EXPECTED_WARNING = "EXPECTED WARNING: This is a negative test"
        + " to verify that negative value is not accepted for -fx-background-radius."
        + " A 'No radii value may be < 0' warning message is expected.";

    @Test
    public void basicFill() {
        setStyle("-fx-background-color: red;");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void translucentFill() {
        setStyle("-fx-background-color: rgba(255, 0, 0, .2);");
        // multiply through the alpha
        checkRegionCornersAndBoundariesOfBackgroundFill(
                region.getBackground().getFills().get(0), Color.rgb(255, 204, 204), SCENE_FILL);
    }

    @Test
    public void basicFill_Insets1() {
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 5");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_Insets2() {
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 5 10");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_Insets3() {
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 5 10 15");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_Insets4() {
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 5 10 15 20");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_NegativeInsets1() {
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: -5");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_NegativeInsets2() {
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: -5 -10");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_NegativeInsets3() {
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: -5 -10 -15");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_NegativeInsets4() {
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: -5 -10 -15 -20");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_MixedInsets() {
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 10 10 -10 10");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_Radius1() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 10");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_Radius2() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 10 20");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_Radius3() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 10 20 30");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void basicFill_Radius4() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 10 20 30 40");
        checkRegionCornersAndBoundariesForFills();
    }

    // TODO need to write tests for percentage based radii
//    public void basicFill_PercentageRadius() {
//        setStyle(
//                "-fx-background-color: red;" +
//                "-fx-background-radius: 5% 10% 15% 20%");
//    }

    // TODO need to check on the syntax for the 8 independent corner radii...
//    public void basicFill_Radius8() {
//        setStyle(
//                "-fx-background-color: red;" +
//                "-fx-background-radius: 10 20 30 40 50 60 70 80");
//    }

    @Test
    public void basicFill_RadiusAndInsets() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 10 20 30 40;" +
                "-fx-background-insets: 5 10 15 20");
        checkRegionCornersAndBoundariesForFills();
    }

    // NOTE: A negative radius from CSS is treated as 0.
    @Test
    public void basicFill_NegativeRadius1() {
        System.err.println(EXPECTED_WARNING);
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: -10");
        checkRegionCornersAndBoundariesForFills();
    }

    // NOTE: A negative radius from CSS is treated as 0.
    @Test
    public void basicFill_NegativeRadius2() {
        System.err.println(EXPECTED_WARNING);
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: -10 -20");
        checkRegionCornersAndBoundariesForFills();
    }

    // NOTE: A negative radius from CSS is treated as 0.
    @Test
    public void basicFill_NegativeRadius3() {
        System.err.println(EXPECTED_WARNING);
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: -10 -20 -30");
        checkRegionCornersAndBoundariesForFills();
    }

    // NOTE: A negative radius from CSS is treated as 0.
    @Test
    public void basicFill_NegativeRadius4() {
        System.err.println(EXPECTED_WARNING);
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: -10 -20 -30 -40");
        checkRegionCornersAndBoundariesForFills();
    }

    /**************************************************************************
     *                                                                        *
     * Tests for ImagePattern fills and gradient fills                        *
     *                                                                        *
     *************************************************************************/

    // NOTE: These tests could be even more precise with different images / gradients such that
    // I actually could predict the color under a point, rather than just asserting it isn't
    // the Scene's fill.

    @Test
    public void imageFill() {
        setStyle("-fx-background-color: repeating-image-pattern('test/robot/javafx/scene/layout/test20x20.png');");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void imageFill_MixedInsets() {
        setStyle(
                "-fx-background-color: repeating-image-pattern('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-insets: 5 10 -15 20");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void imageFill_Radius4() {
        setStyle(
                "-fx-background-color: repeating-image-pattern('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-radius: 10 20 30 40");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void imageFill_MissingImage() {
        setStyle(
                "-fx-background-color: repeating-image-pattern('test/robot/javafx/scene/layout/missing.png');" +
                "-fx-background-radius: 10 20 30 40");
        assertColorEquals(SCENE_FILL, WIDTH / 2, HEIGHT / 2, TOLERANCE);
    }

    @Test
    public void imageFill_Stretched() {
        setStyle("-fx-background-color: image-pattern('test/robot/javafx/scene/layout/test20x20.png');");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void imageFill_Stretched2() {
        setStyle("-fx-background-color: image-pattern('test/robot/javafx/scene/layout/test20x20.png', 0, 0, 1, 1);");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void imageFill_Stretched3() {
        setStyle("-fx-background-color: image-pattern('test/robot/javafx/scene/layout/test20x20.png', 0, 0, 1, 1, true);");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void imageFill_Tiled() {
        setStyle("-fx-background-color: image-pattern('test/robot/javafx/scene/layout/test20x20.png', 0, 0, 40, 40, false);");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void linearFill() {
        setStyle("-fx-background-color: linear-gradient(to bottom, red 0%, blue 100%);");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void linearFill2() {
        setStyle("-fx-background-color: linear-gradient(to right, red 0%, blue 100%);");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void linearFill_MixedInsets() {
        setStyle(
                "-fx-background-color: linear-gradient(to bottom, red 0%, blue 100%);" +
                "-fx-background-insets: 5 10 -15 20");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void linearFill_Radius4() {
        setStyle(
                "-fx-background-color: linear-gradient(to bottom, red 0%, blue 100%);" +
                "-fx-background-radius: 10 20 30 40");
        checkRegionCornersAndBoundariesForFills();
    }

    // TODO I could write more tests here for other linear fill options, like repeat etc, also radial fill

    /**************************************************************************
     *                                                                        *
     * Tests for stacking fills with different radii etc                      *
     *                                                                        *
     *************************************************************************/

    @Test
    public void testScenario1() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 0 0 -10 0, 0, 10, 20;" +
                "-fx-background-radius: 10 20 30 40;" +
                "-fx-padding: 10 20 30 40;");
        checkRegionCornersAndBoundariesForFills();
    }

    @Test
    public void testScenario2() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red, green, blue, grey;" +
                "-fx-background-insets: 0 0 -10 0, 0, 10, 20;" +
                "-fx-background-radius: 5 10 15 20, 25, 30 35 40 45;" +
                "-fx-padding: 10 20 30 40;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill green = region.getBackground().getFills().get(1);
        BackgroundFill blue = region.getBackground().getFills().get(2);
        BackgroundFill grey = region.getBackground().getFills().get(3);

        checkRegionLeftBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionTopLeftCorner(red, SCENE_FILL);
        checkRegionTopBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionTopRightCorner(red, SCENE_FILL);
        checkRegionRightBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionBottomRightCorner(red, SCENE_FILL);
        checkRegionBottomBoundary(red, SCENE_FILL);
        checkRegionBottomLeftCorner(red, SCENE_FILL);

        checkRegionLeftBoundary(green, SCENE_FILL);
        checkRegionTopLeftCorner(green, Color.RED);
        checkRegionTopBoundary(green, SCENE_FILL);
        checkRegionTopRightCorner(green, Color.RED);
        checkRegionRightBoundary(green, SCENE_FILL);
        checkRegionBottomRightCorner(green, Color.RED);
        checkRegionBottomBoundary(green, Color.RED);
        checkRegionBottomLeftCorner(green, Color.RED);

        checkRegionCornersAndBoundariesOfBackgroundFill(blue, Color.GREEN);
        checkRegionCornersAndBoundariesOfBackgroundFill(grey, Color.BLUE);
    }

    @Test
    public void testScenario3() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red, green, blue, grey;" +
                "-fx-background-insets: 0 0 -10 0, 0, 10, 20;" +
                "-fx-background-radius: 10 20 30 40;" +
                "-fx-padding: 10 20 30 40;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill green = region.getBackground().getFills().get(1);
        BackgroundFill blue = region.getBackground().getFills().get(2);
        BackgroundFill grey = region.getBackground().getFills().get(3);

        checkRegionLeftBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionTopLeftCorner(red, Color.GREEN, SCENE_FILL);
        checkRegionTopBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionTopRightCorner(red, Color.GREEN, SCENE_FILL);
        checkRegionRightBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionBottomRightCorner(red, SCENE_FILL);
        checkRegionBottomBoundary(red, SCENE_FILL);
        checkRegionBottomLeftCorner(red, SCENE_FILL);

        checkRegionLeftBoundary(green, SCENE_FILL);
        checkRegionTopLeftCorner(green, SCENE_FILL);
        checkRegionTopBoundary(green, SCENE_FILL);
        checkRegionTopRightCorner(green, SCENE_FILL);
        checkRegionRightBoundary(green, SCENE_FILL);
        checkRegionBottomRightCorner(green, Color.RED);
        checkRegionBottomBoundary(green, Color.RED);
        checkRegionBottomLeftCorner(green, Color.RED);

        checkRegionCornersAndBoundariesOfBackgroundFill(blue, Color.GREEN);
        checkRegionCornersAndBoundariesOfBackgroundFill(grey, Color.BLUE);
    }

    @Test
    public void testScenario4() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red, green, blue, repeating-image-pattern('test/robot/javafx/scene/layout/test20x20.png');" +
                "-fx-background-insets: 0 0 -10 0, 0, 10, 20;" +
                "-fx-background-radius: 10 20 30 40;" +
                "-fx-padding: 10 20 30 40;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill green = region.getBackground().getFills().get(1);
        BackgroundFill blue = region.getBackground().getFills().get(2);
        BackgroundFill image = region.getBackground().getFills().get(3);

        checkRegionLeftBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionTopLeftCorner(red, Color.GREEN, SCENE_FILL);
        checkRegionTopBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionTopRightCorner(red, Color.GREEN, SCENE_FILL);
        checkRegionRightBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionBottomRightCorner(red, SCENE_FILL);
        checkRegionBottomBoundary(red, SCENE_FILL);
        checkRegionBottomLeftCorner(red, SCENE_FILL);

        checkRegionLeftBoundary(green, SCENE_FILL);
        checkRegionTopLeftCorner(green, SCENE_FILL);
        checkRegionTopBoundary(green, SCENE_FILL);
        checkRegionTopRightCorner(green, SCENE_FILL);
        checkRegionRightBoundary(green, SCENE_FILL);
        checkRegionBottomRightCorner(green, Color.RED);
        checkRegionBottomBoundary(green, Color.RED);
        checkRegionBottomLeftCorner(green, Color.RED);

        checkRegionCornersAndBoundariesOfBackgroundFill(blue, Color.GREEN);
//        checkRegionCornersAndBoundariesOfBackgroundFill(image, Color.BLUE);
    }

    @Test
    public void testScenario5() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red, green, repeating-image-pattern('test/robot/javafx/scene/layout/test20x20.png'), blue;" +
                "-fx-background-insets: 0 0 -10 0, 0, 10, 20;" +
                "-fx-background-radius: 10 20 30 40;" +
                "-fx-padding: 10 20 30 40;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill green = region.getBackground().getFills().get(1);
        BackgroundFill image = region.getBackground().getFills().get(2);
        BackgroundFill blue = region.getBackground().getFills().get(3);

        checkRegionLeftBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionTopLeftCorner(red, Color.GREEN, SCENE_FILL);
        checkRegionTopBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionTopRightCorner(red, Color.GREEN, SCENE_FILL);
        checkRegionRightBoundary(red, Color.GREEN, SCENE_FILL);
        checkRegionBottomRightCorner(red, SCENE_FILL);
        checkRegionBottomBoundary(red, SCENE_FILL);
        checkRegionBottomLeftCorner(red, SCENE_FILL);

        checkRegionLeftBoundary(green, SCENE_FILL);
        checkRegionTopLeftCorner(green, SCENE_FILL);
        checkRegionTopBoundary(green, SCENE_FILL);
        checkRegionTopRightCorner(green, SCENE_FILL);
        checkRegionRightBoundary(green, SCENE_FILL);
        checkRegionBottomRightCorner(green, Color.RED);
        checkRegionBottomBoundary(green, Color.RED);
        checkRegionBottomLeftCorner(green, Color.RED);

        checkRegionCornersAndBoundariesOfBackgroundFill(image, Color.GREEN);
//        checkRegionCornersAndBoundariesOfBackgroundFill(blue, Color.BLUE, null);
    }

    @Test
    public void testExample1() {
        setStyle(
                "-fx-background-color: red, green, blue;" +
                "-fx-background-insets: 4, 8, 12, 16;" + // An extra value here, which should be ignored
                "-fx-background-radius: 14;");

        checkRegionCornersAndBoundariesForFills();
    }

    /**************************************************************************
     *                                                                        *
     * Tests for odd edge cases                                               *
     *                                                                        *
     *************************************************************************/

    @Test
    public void testOnePixelTopInset() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 1 0 0 0;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill yellow = region.getBackground().getFills().get(1);

        checkRegionLeftBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(red, Color.RED, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(red, Color.RED, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(red, Color.RED, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);

        checkRegionLeftBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(yellow, Color.YELLOW, Color.RED, 0, .2);
        checkRegionTopRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
    }

    @Test
    public void testOnePixelRightInset() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 1 0 0;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill yellow = region.getBackground().getFills().get(1);

        checkRegionLeftBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(red, Color.RED, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(red, Color.RED, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(red, Color.RED, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);

        checkRegionLeftBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(yellow, Color.YELLOW, Color.RED, 0, .2);
        checkRegionBottomRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
    }

    @Test
    public void testOnePixelBottomInset() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 0 1 0;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill yellow = region.getBackground().getFills().get(1);

        checkRegionLeftBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(red, Color.RED, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(red, Color.RED, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(red, Color.RED, SCENE_FILL, 0, .2);

        checkRegionLeftBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(yellow, Color.YELLOW, Color.RED, 0, .2);
        checkRegionBottomLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
    }

    @Test
    public void testOnePixelLeftInset() {
        assumeTrue(checkIntegralUIScale());
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 0 0 1;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill yellow = region.getBackground().getFills().get(1);

        checkRegionLeftBoundary(red, Color.RED, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(red, Color.RED, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(red, Color.RED, SCENE_FILL, 0, .2);

        checkRegionLeftBoundary(yellow, Color.YELLOW, Color.RED, 0, .2);
        checkRegionTopLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
    }

    @Disabled("RT-33446")
    @Test
    public void testHalfPixelTopInset() {
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, .5 0 0 0;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill yellow = region.getBackground().getFills().get(1);
        Color blended = Color.rgb(254, 127, 27);

        checkRegionLeftBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(red, blended, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(red, blended, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(red, blended, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);

        checkRegionLeftBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
    }

    @Disabled("RT-33446")
    @Test
    public void testHalfPixelRightInset() {
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 .5 0 0;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill yellow = region.getBackground().getFills().get(1);
        Color blended = Color.rgb(254, 127, 27);

        checkRegionLeftBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(red, blended, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(red, blended, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(red, blended, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);

        checkRegionLeftBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
    }

    @Disabled("RT-33446")
    @Test
    public void testHalfPixelBottomInset() {
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 0 .5 0;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill yellow = region.getBackground().getFills().get(1);
        Color blended = Color.rgb(254, 127, 27);

        checkRegionLeftBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(red, blended, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(red, blended, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(red, blended, SCENE_FILL, 0, .2);

        checkRegionLeftBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(yellow, blended, SCENE_FILL, 0, .2);
    }

    @Disabled("RT-33446")
    @Test
    public void testHalfPixelLeftInset() {
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 0 0 .5;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill yellow = region.getBackground().getFills().get(1);
        Color blended = Color.rgb(254, 127, 27);

        checkRegionLeftBoundary(red, blended, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(red, blended, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(red, blended, SCENE_FILL, 0, .2);

        checkRegionLeftBoundary(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionTopLeftCorner(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(yellow, blended, SCENE_FILL, 0, .2);
    }

    @Disabled("RT-33446")
    @Test
    public void testHalfPixelTopLeftInset() {
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, .5 0 0 .5;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill yellow = region.getBackground().getFills().get(1);
        Color blended = Color.rgb(254, 127, 27);

        checkRegionLeftBoundary(red, blended, SCENE_FILL, 0, .2);
        // I'm not sure about this one. We actually blend the top and left together,
        // which makes the corner darker than it really probably should be. I think this is a bug.
//        checkRegionTopLeftCorner(red, blended, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(red, blended, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(red, blended, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(red, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(red, blended, SCENE_FILL, 0, .2);

        checkRegionLeftBoundary(yellow, blended, SCENE_FILL, 0, .2);
        // I'm not sure about this one. We actually blend the top and left together,
        // which makes the corner darker than it really probably should be. I think this is a bug.
//        checkRegionTopLeftCorner(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionTopBoundary(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionTopRightCorner(yellow, blended, SCENE_FILL, 0, .2);
        checkRegionRightBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomRightCorner(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomBoundary(yellow, Color.YELLOW, SCENE_FILL, 0, .2);
        checkRegionBottomLeftCorner(yellow, blended, SCENE_FILL, 0, .2);
    }

    @Test
    public void testNoInsets() {
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0;");

        BackgroundFill red = region.getBackground().getFills().get(0);
        BackgroundFill yellow = region.getBackground().getFills().get(1);
        checkRegionCornersAndBoundariesOfBackgroundFill(red, Color.YELLOW, SCENE_FILL);
        checkRegionCornersAndBoundariesOfBackgroundFill(yellow, SCENE_FILL);
    }

    @Test
    public void testYellowOnRed() {
        setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 40;");
        checkRegionCornersAndBoundariesForFills();
    }

    // TODO to be thorough, I wonder if we can write a test where NaN or +/- Infinity can be passed via CSS to radii?

    // This just plain shouldn't work.
//    public void testImageFill() {
//        setStyle("-fx-background-color: url('test/robot/javafx/scene/layout/test20x20.png');");
//    }
}

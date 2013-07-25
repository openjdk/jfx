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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package region;

import javafx.scene.layout.Region;

/**
 *
 */
public class RegionBackgroundFillUITest extends RegionUITestBase {
    public static void main(String[] args) {
        launch(args);
    }

    /**************************************************************************
     *                                                                        *
     * Tests for background fills. We start with a series of simple tests     *
     * with a single solid fill, including exercising different insets and    *
     * corner radii.                                                          *
     *                                                                        *
     *************************************************************************/

    public void basicFill(Region region) {
        region.setStyle("-fx-background-color: red;");
    }

    public void translucentFill(Region region) {
        region.setStyle("-fx-background-color: rgba(255, 0, 0, .2);");
    }

    public void basicFill_Insets1(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 5");
    }

    public void basicFill_Insets2(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 5 10");
    }

    public void basicFill_Insets3(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 5 10 15");
    }

    public void basicFill_Insets4(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 5 10 15 20");
    }

    public void basicFill_NegativeInsets1(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: -5");
    }

    public void basicFill_NegativeInsets2(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: -5 -10");
    }

    public void basicFill_NegativeInsets3(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: -5 -10 -15");
    }

    public void basicFill_NegativeInsets4(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: -5 -10 -15 -20");
    }

    public void basicFill_MixedInsets(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 10 10 -10 10");
    }

    public void basicFill_Radius1(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 10");
    }

    public void basicFill_Radius2(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 10 20");
    }

    public void basicFill_Radius3(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 10 20 30");
    }

    public void basicFill_Radius4(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 10 20 30 40");
    }

    // TODO need to write tests for percentage based radii
//    public void basicFill_PercentageRadius(Region region) {
//        region.setStyle(
//                "-fx-background-color: red;" +
//                "-fx-background-radius: 5% 10% 15% 20%");
//    }

    // TODO need to check on the syntax for the 8 independent corner radii...
//    public void basicFill_Radius8(Region region) {
//        region.setStyle(
//                "-fx-background-color: red;" +
//                "-fx-background-radius: 10 20 30 40 50 60 70 80");
//    }

    public void basicFill_RadiusAndInsets(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: 10 20 30 40;" +
                "-fx-background-insets: 5 10 15 20");
    }

    // NOTE: A negative radius from CSS is treated as 0.
    public void basicFill_NegativeRadius1(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: -10");
    }

    // NOTE: A negative radius from CSS is treated as 0.
    public void basicFill_NegativeRadius2(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: -10 -20");
    }

    // NOTE: A negative radius from CSS is treated as 0.
    public void basicFill_NegativeRadius3(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: -10 -20 -30");
    }

    // NOTE: A negative radius from CSS is treated as 0.
    public void basicFill_NegativeRadius4(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-radius: -10 -20 -30 -40");
    }

    /**************************************************************************
     *                                                                        *
     * Tests for ImagePattern fills and gradient fills                        *
     *                                                                        *
     *************************************************************************/

    public void imageFill(Region region) {
        region.setStyle("-fx-background-color: repeating-image-pattern('region/test20x20.png');");
    }

    public void imageFill_MixedInsets(Region region) {
        region.setStyle(
                "-fx-background-color: repeating-image-pattern('region/test20x20.png');" +
                "-fx-background-insets: 5 10 -15 20");
    }

    public void imageFill_Radius4(Region region) {
        region.setStyle(
                "-fx-background-color: repeating-image-pattern('region/test20x20.png');" +
                "-fx-background-radius: 10 20 30 40");
    }

    public void imageFill_MissingImage(Region region) {
        region.setStyle(
                "-fx-background-color: repeating-image-pattern('region/missing.png');" +
                "-fx-background-radius: 10 20 30 40");
    }

    public void imageFill_Stretched(Region region) {
        region.setStyle("-fx-background-color: image-pattern('region/test20x20.png');");
    }

    public void imageFill_Stretched2(Region region) {
        region.setStyle("-fx-background-color: image-pattern('region/test20x20.png', 0, 0, 1, 1);");
    }

    public void imageFill_Stretched3(Region region) {
        region.setStyle("-fx-background-color: image-pattern('region/test20x20.png', 0, 0, 1, 1, true);");
    }

    public void imageFill_Tiled(Region region) {
        region.setStyle("-fx-background-color: image-pattern('region/test20x20.png', 0, 0, 40, 40, false);");
    }

    public void linearFill(Region region) {
        region.setStyle("-fx-background-color: linear-gradient(to bottom, red 0%, blue 100%);");
    }

    public void linearFill2(Region region) {
        region.setStyle("-fx-background-color: linear-gradient(to right, red 0%, blue 100%);");
    }

    public void linearFill_MixedInsets(Region region) {
        region.setStyle(
                "-fx-background-color: linear-gradient(to bottom, red 0%, blue 100%);" +
                "-fx-background-insets: 5 10 -15 20");
    }

    public void linearFill_Radius4(Region region) {
        region.setStyle(
                "-fx-background-color: linear-gradient(to bottom, red 0%, blue 100%);" +
                "-fx-background-radius: 10 20 30 40");
    }

    // TODO I could write more tests here for other linear fill options, like repeat etc, also radial fill

    /**************************************************************************
     *                                                                        *
     * Tests for stacking fills with different radii etc                      *
     *                                                                        *
     *************************************************************************/

    public void testScenario1(Region region) {
        region.setStyle(
                "-fx-background-color: red;" +
                "-fx-background-insets: 0 0 -10 0, 0, 10, 20;" +
                "-fx-background-radius: 10 20 30 40;" +
                "-fx-padding: 10 20 30 40;");
    }

    public void testScenario2(Region region) {
        region.setStyle(
                "-fx-background-color: red, green, blue, grey;" +
                "-fx-background-insets: 0 0 -10 0, 0, 10, 20;" +
                "-fx-background-radius: 5 10 15 20, 25, 30 35 40 45;" +
                "-fx-padding: 10 20 30 40;");
    }

    public void testScenario3(Region region) {
        region.setStyle(
                "-fx-background-color: red, green, blue, grey;" +
                "-fx-background-insets: 0 0 -10 0, 0, 10, 20;" +
                "-fx-background-radius: 10 20 30 40;" +
                "-fx-padding: 10 20 30 40;");
    }

    public void testScenario4(Region region) {
        region.setStyle(
                "-fx-background-color: red, green, blue, repeating-image-pattern('region/test20x20.png');" +
                "-fx-background-insets: 0 0 -10 0, 0, 10, 20;" +
                "-fx-background-radius: 10 20 30 40;" +
                "-fx-padding: 10 20 30 40;");
    }

    public void testScenario5(Region region) {
        region.setStyle(
                "-fx-background-color: red, green, repeating-image-pattern('region/test20x20.png'), blue;" +
                "-fx-background-insets: 0 0 -10 0, 0, 10, 20;" +
                "-fx-background-radius: 10 20 30 40;" +
                "-fx-padding: 10 20 30 40;");
    }

    public void testExample1(Region region) {
        region.setStyle(
                "-fx-background-color: red, green, blue;" +
                "-fx-background-insets: 4, 8, 12, 16;" + // An extra value here, which should be ignored
                "-fx-background-radius: 14;");
    }

    /**************************************************************************
     *                                                                        *
     * Tests for odd edge cases                                               *
     *                                                                        *
     *************************************************************************/

    public void testOnePixelTopInset(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 1 0 0 0;");
    }

    public void testOnePixelRightInset(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 1 0 0;");
    }

    public void testOnePixelBottomInset(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 0 1 0;");
    }

    public void testOnePixelLeftInset(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 0 0 1;");
    }

    public void testHalfPixelTopInset(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, .5 0 0 0;");
    }

    public void testHalfPixelRightInset(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 .5 0 0;");
    }

    public void testHalfPixelBottomInset(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 0 .5 0;");
    }

    public void testHalfPixelLeftInset(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 0 0 .5;");
    }

    public void testHalfPixelTopLeftInset(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, .5 0 0 .5;");
    }

    public void testNoInsets(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 0 ;");
    }

    public void testYellowOnRed(Region region) {
        region.setStyle(
                "-fx-background-color: red, yellow;" +
                "-fx-background-insets: 0, 40;");
    }

    // TODO to be thorough, I wonder if we can write a test where NaN or +/- Infinity can be passed via CSS to radii?

    // This just plain shouldn't work.
//    public void testImageFill(Region region) {
//        region.setStyle("-fx-background-color: url('region/test20x20.png');");
//    }
}

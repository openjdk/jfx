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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.scene.layout;

import org.junit.Test;

/**
 */
public class RegionShapeUITest extends RegionUITestBase {
    private static final String ARROW = "\"M0 0 L100 0 L50 100 Z\"";


    @Test public void dummy() { }

//    /**************************************************************************
//     *                                                                        *
//     * Tests for background fills. We start with a series of simple tests     *
//     * with a single solid fill, to exercise the most basic aspects of the    *
//     * shape support (such as making sure the shape is positioned in the      *
//     * expected places, etc).                                                 *
//     *                                                                        *
//     *************************************************************************/
//
//    public void noFills(Region region) {
//        region.setStyle("-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidFill(Region region) {
//        region.setStyle("-fx-background-color: red;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidFill_NotScaled(Region region) {
//        region.setStyle("-fx-background-color: red;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidFill_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-background-color: red;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void transparentFill(Region region) {
//        region.setStyle("-fx-background-color: rgba(255, 0, 0, .2);" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void transparentFill_NotScaled(Region region) {
//        region.setStyle("-fx-background-color: rgba(255, 0, 0, .2);" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void transparentFill_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-background-color: rgba(255, 0, 0, .2);" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    /**************************************************************************
//     *                                                                        *
//     * Tests for more complex fill scenarios, including the use of image      *
//     * fills and gradient fills                                               *
//     *                                                                        *
//     *************************************************************************/
//
//    public void twoSolidFills(Region region) {
//        region.setStyle("-fx-background-color: red, green;" +
//                        "-fx-background-insets: 0, 30;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void twoSolidFills_NotScaled(Region region) {
//        region.setStyle("-fx-background-color: red, green;" +
//                        "-fx-background-insets: 0, 30;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void twoSolidFills_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-background-color: red, green;" +
//                        "-fx-background-insets: 0, 30;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void twoSolidFills_independentInsets(Region region) {
//        region.setStyle("-fx-background-color: red, green;" +
//                        "-fx-background-insets: 0, 10 20 30 40;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void twoSolidFills_independentInsets_NotScaled(Region region) {
//        region.setStyle("-fx-background-color: red, green;" +
//                        "-fx-background-insets: 0, 10 20 30 40;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void twoSolidFills_independentInsets_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-background-color: red, green;" +
//                        "-fx-background-insets: 0, 10 20 30 40;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void linearGradientFill(Region region) {
//        region.setStyle("-fx-background-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    // Test this to make sure the gradient is relative to the shape and not the position
//    // of the shape in the region
//    public void linearGradientFill_NotScaled(Region region) {
//        region.setStyle("-fx-background-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    // Test this to make sure the gradient is relative to the shape and not the position
//    // of the shape in the region
//    public void linearGradientFill_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-background-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void imageFill(Region region) {
//        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void imageFill_NotScaled(Region region) {
//        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void imageFill_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void imageFill_cover(Region region) {
//        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
//                        "-fx-background-size: cover;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void imageFill_cover_NotScaled(Region region) {
//        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
//                        "-fx-background-size: cover;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void imageFill_cover_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
//                        "-fx-background-size: cover;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void multipleFills(Region region) {
//        region.setStyle("-fx-background-color: red, repeating-image-pattern('region/test20x20.png'), blue;" +
//                        "-fx-background-insets: 0, 20, 40;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void multipleFills_NotScaled(Region region) {
//        region.setStyle("-fx-background-color: red, repeating-image-pattern('region/test20x20.png'), blue;" +
//                        "-fx-background-insets: 0, 20, 40;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void multipleFills_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-background-color: red, repeating-image-pattern('region/test20x20.png'), blue;" +
//                        "-fx-background-insets: 0, 20, 40;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    /**************************************************************************
//     *                                                                        *
//     * Tests for strokes.                                                     *
//     *                                                                        *
//     *************************************************************************/
//
//    public void solidStroke(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidStroke_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidStroke_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    // TODO These should work, but presently the parser doesn't support thin, medium, and thick, I think.
////    public void solidStroke_Thin(Region region) {
////        region.setStyle("-fx-border-style: solid;" +
////                        "-fx-background-color: lightgray;" +
////                        "-fx-border-color: red;" +
////                        "-fx-border-width: thin;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
////
////    public void solidStroke_Thin_NotScaled(Region region) {
////        region.setStyle("-fx-border-style: solid;" +
////                        "-fx-background-color: lightgray;" +
////                        "-fx-border-color: red;" +
////                        "-fx-border-width: thin;" +
////                        "-fx-scale-shape: false;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
////
////    public void solidStroke_Thin_NotScaledAndNotCentered(Region region) {
////        region.setStyle("-fx-border-style: solid;" +
////                        "-fx-background-color: lightgray;" +
////                        "-fx-border-color: red;" +
////                        "-fx-border-width: thin;" +
////                        "-fx-position-shape: false;" +
////                        "-fx-scale-shape: false;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
////
////    public void solidStroke_Medium(Region region) {
////        region.setStyle("-fx-border-style: solid;" +
////                        "-fx-background-color: lightgray;" +
////                        "-fx-border-color: red;" +
////                        "-fx-border-width: medium;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
////
////    public void solidStroke_Medium_NotScaled(Region region) {
////        region.setStyle("-fx-border-style: solid;" +
////                        "-fx-background-color: lightgray;" +
////                        "-fx-border-color: red;" +
////                        "-fx-border-width: medium;" +
////                        "-fx-scale-shape: false;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
////
////    public void solidStroke_Medium_NotScaledAndNotCentered(Region region) {
////        region.setStyle("-fx-border-style: solid;" +
////                        "-fx-background-color: lightgray;" +
////                        "-fx-border-color: red;" +
////                        "-fx-border-width: medium;" +
////                        "-fx-position-shape: false;" +
////                        "-fx-scale-shape: false;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
////
////    public void solidStroke_Thick(Region region) {
////        region.setStyle("-fx-border-style: solid;" +
////                        "-fx-background-color: lightgray;" +
////                        "-fx-border-color: red;" +
////                        "-fx-border-width: thick;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
////
////    public void solidStroke_Thick_NotScaled(Region region) {
////        region.setStyle("-fx-border-style: solid;" +
////                        "-fx-background-color: lightgray;" +
////                        "-fx-border-color: red;" +
////                        "-fx-border-width: thick;" +
////                        "-fx-scale-shape: false;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
////
////    public void solidStroke_Thick_NotScaledAndNotCentered(Region region) {
////        region.setStyle("-fx-border-style: solid;" +
////                        "-fx-background-color: lightgray;" +
////                        "-fx-border-color: red;" +
////                        "-fx-border-width: thick;" +
////                        "-fx-position-shape: false;" +
////                        "-fx-scale-shape: false;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
//
//    public void solidStroke_1(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 1;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidStroke_1_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 1;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidStroke_1_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 1;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidStroke_3(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 3;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidStroke_3_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 3;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidStroke_3_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 3;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidStroke_5(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidTranslucentStroke_5(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: rgba(255, 0, 0, .3);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidTranslucentStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: rgba(255, 0, 0, .3);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidTranslucentStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: rgba(255, 0, 0, .3);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidLinearGradientStroke_5(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidLinearGradientStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidLinearGradientStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidImageStroke_5(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: repeating-image-pattern('region/test20x20.png');" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidImageStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: repeating-image-pattern('region/test20x20.png');" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void solidImageStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: solid;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: repeating-image-pattern('region/test20x20.png');" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedStroke_1(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 1;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedStroke_1_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 1;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedStroke_1_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 1;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedStroke_3(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 3;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedStroke_3_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 3;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedStroke_3_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 3;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedStroke_5(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedTranslucentStroke_5(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: rgba(255, 0, 0, .3);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedTranslucentStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: rgba(255, 0, 0, .3);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedTranslucentStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: rgba(255, 0, 0, .3);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedLinearGradientStroke_5(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedLinearGradientStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedLinearGradientStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedImageStroke_5(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: repeating-image-pattern('region/test20x20.png');" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedImageStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: repeating-image-pattern('region/test20x20.png');" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dashedImageStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dashed;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: repeating-image-pattern('region/test20x20.png');" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedStroke_1(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 1;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedStroke_1_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 1;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedStroke_1_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 1;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedStroke_3(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 3;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedStroke_3_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 3;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedStroke_3_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 3;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedStroke_5(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: red;" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedTranslucentStroke_5(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: rgba(255, 0, 0, .3);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedTranslucentStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: rgba(255, 0, 0, .3);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedTranslucentStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: rgba(255, 0, 0, .3);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedLinearGradientStroke_5(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedLinearGradientStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedLinearGradientStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: linear-gradient(to bottom, red 0%, blue 100%);" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedImageStroke_5(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: repeating-image-pattern('region/test20x20.png');" +
//                        "-fx-border-width: 5;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedImageStroke_5_NotScaled(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: repeating-image-pattern('region/test20x20.png');" +
//                        "-fx-border-width: 5;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    public void dottedImageStroke_5_NotScaledAndNotCentered(Region region) {
//        region.setStyle("-fx-border-style: dotted;" +
//                        "-fx-background-color: lightgray;" +
//                        "-fx-border-color: repeating-image-pattern('region/test20x20.png');" +
//                        "-fx-border-width: 5;" +
//                        "-fx-position-shape: false;" +
//                        "-fx-scale-shape: false;" +
//                        "-fx-shape: " + ARROW + ";");
//    }
//
//    // TODO need to do more complete testing which includes the full range of available
//    // properties for a border stroke
////    public void shape_arrow_stroke(Region region) {
////        region.setStyle("-fx-border-style: solid;" +
////                        "-fx-background-color: grey;" +
////                        "-fx-border-color: rgba(255, 0, 0, .2);" +
////                        "-fx-border-width: 5;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
//
//    /**************************************************************************
//     *                                                                        *
//     * Tests for parameters to background fills, background images,           *
//     * border strokes, and border images that should be ignored by the        *
//     * region when drawing a shape, and have no effect whatsoever.            *
//     *                                                                        *
//     *************************************************************************/
//
//    // This is a test case that should have no effect
////    public void shape_arrow_imageFill_repeatX(Region region) {
////        region.setStyle("-fx-background-image: url('region/test20x20.png');" +
////                        "-fx-background-repeat: repeat-x;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
//
////    public void shape_arrow_RadiusIgnored(Region region) {
////        region.setStyle("-fx-background-color: red;" +
////                        "-fx-background-radius: 300;" +
////                        "-fx-shape: " + ARROW + ";");
////    }
//
//    /**************************************************************************
//     *                                                                        *
//     * Shapes that are larger than the region, or defined outside the bounds  *
//     * of the region.                                                         *
//     *                                                                        *
//     *************************************************************************/
//
//    public void shapeLargerThanRegion_Centered(Region region) {
//        region.setStyle(
//                "-fx-border-color:blue;" +
//                "-fx-shape:'M 0 0 L 400 0 L 200 400 Z';" +
//                "-fx-scale-shape: false;" +
//                "-fx-background-color: red;");
//    }
//
//    public void shapeLargerThanRegion_Scaled(Region region) {
//        region.setStyle(
//                "-fx-border-color:blue;" +
//                "-fx-shape:'M 0 0 L 400 0 L 200 400 Z';" +
//                "-fx-position-shape: false;" +
//                "-fx-background-color: red;");
//    }
//
//    public void shapeDefinedOutsideRegion_Centered(Region region) {
//        region.setStyle(
//                "-fx-border-color:blue;" +
//                "-fx-shape:'M 400 400 L 450 400 L 425 450 Z';" +
//                "-fx-scale-shape: false;" +
//                "-fx-background-color: red;");
//    }
//
//    public void shapeDefinedOutsideRegion_ScaledCentered(Region region) {
//        region.setStyle(
//                "-fx-border-color:blue;" +
//                "-fx-shape:'M 400 400 L 450 400 L 425 450 Z';" +
//                "-fx-background-color: red;");
//    }
//
//    public void shapeDefinedOutsideRegion_Centered_Negative(Region region) {
//        region.setStyle(
//                "-fx-border-color:blue;" +
//                "-fx-shape:'M -400 -400 L -350 -400 L -375 -350 Z';" +
//                "-fx-scale-shape: false;" +
//                "-fx-background-color: red;");
//    }
//
//    public void shapeDefinedOutsideRegion_ScaledCentered_Negative(Region region) {
//        region.setStyle(
//                "-fx-border-color:blue;" +
//                "-fx-shape:'M -400 -400 L -350 -400 L -375 -350 Z';" +
//                "-fx-background-color: red;");
//    }
//
//    public void shapeDefinedOutsideRegion_Centered_Insets(Region region) {
//        region.setStyle(
//                "-fx-border-color:blue;" +
//                "-fx-shape:'M 400 400 L 450 400 L 425 450 Z';" +
//                "-fx-scale-shape: false;" +
//                "-fx-background-color: red;" +
//                "-fx-background-insets: 10");
//    }
//
//    public void shapeDefinedOutsideRegion_Centered_Outsets(Region region) {
//        region.setStyle(
//                "-fx-border-color:blue;" +
//                "-fx-shape:'M 400 400 L 450 400 L 425 450 Z';" +
//                "-fx-scale-shape: false;" +
//                "-fx-background-color: red;" +
//                "-fx-background-insets: -10");
//    }
//
//    public void shapeNotCenteredOrScaled_Insets(Region region) {
//        region.setStyle(
//                "-fx-border-color:blue;" +
//                "-fx-shape:'M 100 -50 L 200 -50 L 150 100 Z';" +
//                "-fx-scale-shape: false;" +
//                "-fx-position-shape: false;" +
//                "-fx-background-color: red;" +
//                "-fx-background-insets: 10");
//    }
//
//    /**************************************************************************
//     *                                                                        *
//     * Tests for edge conditions                                              *
//     *                                                                        *
//     *************************************************************************/
//
    // TODO Need to write a test for what the rendering is like for an Image that hasn't finished loading yet.
}

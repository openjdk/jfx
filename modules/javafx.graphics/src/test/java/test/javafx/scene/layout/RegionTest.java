/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import test.javafx.scene.layout.MockBiased;
import test.javafx.scene.layout.MockRegion;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import test.javafx.scene.image.ImageForTesting;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.stage.Stage;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.sg.prism.NGRegion;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.NodeShim;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderImage;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.RegionShim;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import test.com.sun.javafx.pgstub.StubToolkit;

/**
 *
 */
public class RegionTest {

    @Test public void testPaddingEmptyByDefault() {
        Region region = new Region();

        assertEquals(Insets.EMPTY, region.getPadding());
    }

    @Test public void testPaddingCannotBeSetToNull() {
        Region region = new Region();

        try {
            region.setPadding(null);
            fail("NullPointerException expected if padding set to null.");
        } catch (Exception e) {
            // expected
        }

        try {
            region.paddingProperty().set(null);
            fail("NullPointerException expected if padding set to null.");
        } catch (Exception e) {
            // expected
        }
    }

    @Test public void testInsetsEqualsPaddingByDefault() {
        Region region = new Region();

        assertEquals(region.getInsets(), region.getPadding());
    }

    @Test public void testBoundedSizeReturnsPrefWhenPrefBetweenMinAndMax() {
        assertEquals(200, RegionShim.boundedSize(100, 200, 300), 0);
    }

    @Test public void testBoundedSizeReturnsMinWhenMinGreaterThanPrefButLessThanMax() {
        assertEquals(200, RegionShim.boundedSize(200, 100, 300), 0);
    }

    @Test public void testBoundedSizeReturnsMinWhenMinGreaterThanPrefAndMax() {
        assertEquals(300, RegionShim.boundedSize(300, 100, 200), 0);
    }

    @Test public void testBoundedSizeReturnsMaxWhenMaxLessThanPrefButGreaterThanMin() {
        assertEquals(200, RegionShim.boundedSize(100, 300, 200), 0);
    }

    @Test public void testBoundedSizeReturnsMinWhenMaxLessThanPrefAndMin() {
        assertEquals(200, RegionShim.boundedSize(200, 300, 100), 0);
    }

    @Test public void testBoundedSizeReturnsMinWhenMaxLessThanPrefAndMinAndPrefLessThanMin() {
        assertEquals(300, RegionShim.boundedSize(300, 200, 100), 0);
    }

    @Test public void testMinWidthOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(10, region.minWidth(-1), 1e-100);
        region.setMinWidth(25.0);
        assertEquals(25, region.getMinWidth(), 1e-100);
        assertEquals(25, region.minWidth(-1), 1e-100);
    }

    @Test public void testMinWidthOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setMinWidth(75.0);
        region.setMinWidth(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getMinWidth(), 1e-100);
        assertEquals(10, region.minWidth(-1), 1e-100);
    }

    @Test public void testMinWidthNaNTreatedAsZero() {
        Region region = new Region();
        region.setMinWidth(Double.NaN);
        assertEquals(0, region.minWidth(-1), 0);
        assertEquals(0, region.minWidth(5), 0);
    }

    @Test public void testMinWidthNegativeTreatedAsZero() {
        Region region = new Region();
        region.setMinWidth(-10);
        assertEquals(0, region.minWidth(-1), 0);
        assertEquals(0, region.minWidth(5), 0);
    }

    @Test public void testMinHeightOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(20, region.minHeight(-1), 1e-100);
        region.setMinHeight(30.0);
        assertEquals(30, region.getMinHeight(), 1e-100);
        assertEquals(30, region.minHeight(-1), 1e-100);
    }

    @Test public void testMinHeightOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setMinHeight(75.0);
        region.setMinHeight(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getMinHeight(), 1e-100);
        assertEquals(20, region.minHeight(-1), 1e-100);
    }

    @Test public void testMinHeightNaNTreatedAsZero() {
        Region region = new Region();
        region.setMinHeight(Double.NaN);
        assertEquals(0, region.minHeight(-1), 0);
        assertEquals(0, region.minHeight(5), 0);
    }

    @Test public void testMinHeightNegativeTreatedAsZero() {
        Region region = new Region();
        region.setMinHeight(-10);
        assertEquals(0, region.minHeight(-1), 0);
        assertEquals(0, region.minHeight(5), 0);
    }

    @Test public void testMinWidthOverrideSetToPref() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(10, region.minWidth(-1), 1e-100);
        region.setMinWidth(Region.USE_PREF_SIZE);
        assertEquals(Region.USE_PREF_SIZE, region.getMinWidth(), 1e-100);
        assertEquals(100, region.minWidth(-1), 1e-100);
    }

    @Test public void testMinHeightOverrideSetToPref() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(20, region.minHeight(-1), 1e-100);
        region.setMinHeight(Region.USE_PREF_SIZE);
        assertEquals(Region.USE_PREF_SIZE, region.getMinHeight(), 1e-100);
        assertEquals(200, region.minHeight(-1), 1e-100);
    }

    @Test public void testPrefWidthOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(100, region.prefWidth(-1), 1e-100);
        region.setPrefWidth(150.0);
        assertEquals(150, region.getPrefWidth(), 1e-100);
        assertEquals(150, region.prefWidth(-1), 1e-100);
    }

    @Test public void testPrefWidthOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setPrefWidth(150.0);
        region.setPrefWidth(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getPrefWidth(), 1e-100);
        assertEquals(100, region.prefWidth(-1), 1e-100);
    }

    @Test public void testPrefWidthNaNTreatedAsZero() {
        Region region = new Region();
        region.setPrefWidth(Double.NaN);
        assertEquals(0, region.prefWidth(-1), 0);
        assertEquals(0, region.prefWidth(5), 0);
    }

    @Test public void testPrefWidthNegativeTreatedAsZero() {
        Region region = new Region();
        region.setPrefWidth(-10);
        assertEquals(0, region.prefWidth(-1), 0);
        assertEquals(0, region.prefWidth(5), 0);
    }

    @Test public void testPrefHeightOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(200, region.prefHeight(-1), 1e-100);
        region.setPrefHeight(300.0);
        assertEquals(300, region.getPrefHeight(), 1e-100);
        assertEquals(300, region.prefHeight(-1), 1e-100);
    }

    @Test public void testPrefHeightOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setPrefHeight(250);
        region.setPrefHeight(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getPrefHeight(), 1e-100);
        assertEquals(200, region.prefHeight(-1), 1e-100);
    }

    @Test public void testPrefHeightNaNTreatedAsZero() {
        Region region = new Region();
        region.setPrefHeight(Double.NaN);
        assertEquals(0, region.prefHeight(-1), 0);
        assertEquals(0, region.prefHeight(5), 0);
    }

    @Test public void testPrefHeightNegativeTreatedAsZero() {
        Region region = new Region();
        region.setPrefHeight(-10);
        assertEquals(0, region.prefHeight(-1), 0);
        assertEquals(0, region.prefHeight(5), 0);
    }

    @Test public void testMaxWidthOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(500, region.maxWidth(-1), 1e-100);
        region.setMaxWidth(550);
        assertEquals(550, region.getMaxWidth(), 1e-100);
        assertEquals(550, region.maxWidth(-1), 1e-100);
    }

    @Test public void testMaxWidthOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setMaxWidth(1000);
        region.setMaxWidth(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getMaxWidth(), 1e-100);
        assertEquals(500, region.maxWidth(-1), 1e-100);
    }

    @Test public void testMaxWidthNaNTreatedAsZero() {
        Region region = new Region();
        region.setMaxWidth(Double.NaN);
        assertEquals(0, region.maxWidth(-1), 0);
        assertEquals(0, region.maxWidth(5), 0);
    }

    @Test public void testMaxWidthNegativeTreatedAsZero() {
        Region region = new Region();
        region.setMaxWidth(-10);
        assertEquals(0, region.maxWidth(-1), 0);
        assertEquals(0, region.maxWidth(5), 0);
    }

    @Test public void testMaxHeightOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(600, region.maxHeight(-1), 1e-100);
        region.setMaxHeight(650);
        assertEquals(650, region.getMaxHeight(), 1e-100);
        assertEquals(650, region.maxHeight(-1), 1e-100);
    }

    @Test public void testMaxHeightOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setMaxHeight(800);
        region.setMaxHeight(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getMaxHeight(), 0);
        assertEquals(600, region.maxHeight(-1), 1e-100);
    }

    @Test public void testMaxHeightNaNTreatedAsZero() {
        Region region = new Region();
        region.setMaxHeight(Double.NaN);
        assertEquals(0, region.maxHeight(-1), 0);
        assertEquals(0, region.maxHeight(5), 0);
    }

    @Test public void testMaxHeightNegativeTreatedAsZero() {
        Region region = new Region();
        region.setMaxHeight(-10);
        assertEquals(0, region.maxHeight(-1), 0);
        assertEquals(0, region.maxHeight(5), 0);
    }

    @Test public void testMaxWidthOverrideSetToPref() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(500, region.maxWidth(-1), 1e-100);
        region.setMaxWidth(Region.USE_PREF_SIZE);
        assertEquals(Region.USE_PREF_SIZE, region.getMaxWidth(), 0);
        assertEquals(100, region.maxWidth(-1), 1e-100);
    }

    @Test public void testMaxHeightOverrideSetToPref() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(600, region.maxHeight(-1), 1e-100);
        region.setMaxHeight(Region.USE_PREF_SIZE);
        assertEquals(Region.USE_PREF_SIZE, region.getMaxHeight(), 0);
        assertEquals(200, region.maxHeight(-1), 1e-100);
    }

    @Test public void testPositionInAreaForResizableForResizableTopLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.TOP);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForResizableTopCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.TOP);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(45, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForResizableTopRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.RIGHT, VPos.TOP);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(80, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForResizableCenterLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.CENTER);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(40, child.getLayoutY(), 1e-100);
    }

//    // See RT-19282
//    @Test public void testPositionInAreaForNONResizableCenterLeft() {
//        Pane pane = new Pane(); // Region extension which makes children sequence public
//
//        Rectangle child = new Rectangle(50.4, 50.4);
//        pane.getChildren().add(child);
//
//        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.CENTER);
//
//        assertEquals(10, child.getLayoutX(), .01);
//        assertEquals(34.8, child.getLayoutY(), .01);
//    }


    @Test public void testPositionInAreaForResizableCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(45, child.getLayoutX(), 1e-100);
        assertEquals(40, child.getLayoutY(), 1e-100);
    }

//    // See RT-19282
//    @Test public void testPositionInAreaForNONResizableCenter() {
//        Pane pane = new Pane(); // Region extension which makes children sequence public
//
//        Rectangle child = new Rectangle(50.4, 50.4);
//        pane.getChildren().add(child);
//
//        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.CENTER);
//
//        assertEquals(34.8, child.getLayoutX(), .01);
//        assertEquals(34.8, child.getLayoutY(), .01);
//    }

    @Test public void testPositionInAreaForResizableCenterRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.RIGHT, VPos.CENTER);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(80, child.getLayoutX(), 1e-100);
        assertEquals(40, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForResizableBottomLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.BOTTOM);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(70, child.getLayoutY(), 1e-100);
    }

//    // See RT-19282
//    @Test public void testPositionInAreaForNONResizableBottomLeft() {
//        Pane pane = new Pane(); // Region extension which makes children sequence public
//
//        Rectangle child = new Rectangle(50.4, 50.4);
//        pane.getChildren().add(child);
//
//        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.BOTTOM);
//
//        assertEquals(10, child.getLayoutX(), .01);
//        assertEquals(59.6, child.getLayoutY(), .01);
//    }

    @Test public void testPositionInAreaForResizableBottomCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.BOTTOM);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(45, child.getLayoutX(), 1e-100);
        assertEquals(70, child.getLayoutY(), 1e-100);
    }

//    // See RT-19282
//    @Test public void testPositionInAreaForNONResizableBottomCenter() {
//        Pane pane = new Pane(); // Region extension which makes children sequence public
//
//        Rectangle child = new Rectangle(50.4, 50.4);
//        pane.getChildren().add(child);
//
//        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.BOTTOM);
//
//        assertEquals(34.8, child.getLayoutX(), .01);
//        assertEquals(59.6, child.getLayoutY(), .01);
//    }

    @Test public void testPositionInAreaForResizableBottomRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.RIGHT, VPos.BOTTOM);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(80, child.getLayoutX(), 1e-100);
        assertEquals(70, child.getLayoutY(), 1e-100);
    }

//    // See RT-19282
//    @Test public void testPositionInAreaForNONResizableBottomRight() {
//        Pane pane = new Pane(); // Region extension which makes children sequence public
//
//        Rectangle child = new Rectangle(50.4, 50.4);
//        pane.getChildren().add(child);
//
//        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 0, HPos.RIGHT, VPos.BOTTOM);
//
//        assertEquals(59.6, child.getLayoutX(), .01);
//        assertEquals(59.6, child.getLayoutY(), .01);
//    }

    @Test public void testPositionInAreaForResizableBaselineLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60); //baseline = 30
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 50, HPos.LEFT, VPos.BASELINE);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
    }

//    // See RT-19282
//    @Test public void testPositionInAreaForNONResizableBaselineLeft() {
//        Pane pane = new Pane(); // Region extension which makes children sequence public
//
//        Rectangle child = new Rectangle(50.4, 50.4);
//        pane.getChildren().add(child);
//
//        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 60, HPos.LEFT, VPos.BASELINE);
//
//        assertEquals(10, child.getLayoutX(), .01);
//        assertEquals(19.6, child.getLayoutY(), .01);
//    }

    @Test public void testPositionInAreaForResizableBaselineCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60); //baseline = 30
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 50, HPos.CENTER, VPos.BASELINE);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(45, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
    }

//    // See RT-19282
//    @Test public void testPositionInAreaForNONResizableBaselineCenter() {
//        Pane pane = new Pane(); // Region extension which makes children sequence public
//
//        Rectangle child = new Rectangle(50.4, 50.4);
//        pane.getChildren().add(child);
//
//        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 60, HPos.CENTER, VPos.BASELINE);
//
//        assertEquals(34.8, child.getLayoutX(), .01);
//        assertEquals(19.6, child.getLayoutY(), .01);
//    }

    @Test public void testPositionInAreaForResizableBaselineRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60); //baseline = 30
        pane.getChildren().add(child);

        child.autosize();
        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 50, HPos.RIGHT, VPos.BASELINE);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(80, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
    }

//    // See RT-19282
//    @Test public void testPositionInAreaForNONResizableBaselineRight() {
//        Pane pane = new Pane(); // Region extension which makes children sequence public
//
//        Rectangle child = new Rectangle(50.4, 50.4);
//        pane.getChildren().add(child);
//
//        RegionShim.positionInArea(pane,child, 10, 10, 100, 100, 60, HPos.RIGHT, VPos.BASELINE);
//
//        assertEquals(59.6, child.getLayoutX(), .01);
//        assertEquals(19.6, child.getLayoutY(), .01);
//    }

    @Test public void testLayoutInAreaWithLargerMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 300,300);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(100, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);

    }

    @Test public void testLayoutInAreaWithSmallerMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(50, child.getWidth(), 1e-100);
        assertEquals(60, child.getHeight(), 1e-100);
        assertEquals(35, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);

    }

    @Test public void testLayoutInAreaWithLargerMin() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 5, 5, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(10, child.getWidth(), 1e-100);
        assertEquals(20, child.getHeight(), 1e-100);
        assertEquals(8, child.getLayoutX(), 1e-100);
        assertEquals(3, child.getLayoutY(), 1e-100);

    }

    @Test public void testLayoutInAreaWithSizeOverrides() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 30,40, 50,60);
        child.setMinSize(50,60);
        child.setPrefSize(100,200);
        child.setMaxSize(500, 500);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(300, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);

    }

    @Test public void testLayoutInAreaWithMaxConstrainedToPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 30,40, 500,500);
        child.setMinSize(50,60);
        child.setPrefSize(100,200);
        child.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(200, child.getHeight(), 1e-100);
        assertEquals(110, child.getLayoutX(), 1e-100);
        assertEquals(60, child.getLayoutY(), 1e-100);

    }

    @Test public void testLayoutInAreaHonorsMaxWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMaxWidth(100); // max less than pref
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(300, child.getHeight(), 1e-100);
        assertEquals(110, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);

    }

    @Test public void testLayoutInAreaHonorsMaxHeightOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMaxHeight(100); // max less than pref
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(100, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(110, child.getLayoutY(), 1e-100);

    }

    @Test public void testLayoutInAreaHonorsMinWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinWidth(400); // max less than pref
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(400, child.getWidth(), 1e-100);
        assertEquals(300, child.getHeight(), 1e-100);
        assertEquals(-40, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsMinHeightOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinHeight(400); // max less than pref
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(-40, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsMinWidthOverMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinWidth(600); // max less than min
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(600, child.getWidth(), 1e-100);
        assertEquals(300, child.getHeight(), 1e-100);
        assertEquals(-140, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsMinHeightOverMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinHeight(600); // max less than min
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(600, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(-140, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsAreaWidthOverPrefWithFillWidth() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 100, 400, 0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsAreaHeightOverPrefWithFillHeight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 100, 0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER);

        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(100, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsAreaWidthOverPrefWithNOFill() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 100, 400, 0, Insets.EMPTY, false, false, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(300, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(60, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsAreaHeightOverPrefWithNOFill() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 100, 0, Insets.EMPTY, false, false, HPos.CENTER, VPos.CENTER);

        assertEquals(200, child.getWidth(), 1e-100);
        assertEquals(100, child.getHeight(), 1e-100);
        assertEquals(60, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaWithBaselineOffset() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Region c1 = new MockBiased(Orientation.HORIZONTAL, 100, 100);
        Region c2 = new MockBiased(Orientation.VERTICAL, 100, 100);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);
        RegionShim.layoutInArea(pane, c1, 10, 10, 300, 100, 20, Insets.EMPTY, false, false, HPos.CENTER, VPos.BASELINE);
        RegionShim.layoutInArea(pane, c2, 10, 10, 300, 100, 20, Insets.EMPTY, false, false, HPos.CENTER, VPos.BASELINE);
        RegionShim.layoutInArea(pane, c3, 10, 10, 300, 100, 20, Insets.EMPTY, false, false, HPos.CENTER, VPos.BASELINE);

        assertEquals(100, c1.getHeight(), 1e-100); // min height == pref height
        // As the other 2 Regions don't have a baseline offset, their baseline offset is "same as height", therefore
        // they can be max 20px tall
        assertEquals(20, c2.getHeight(), 1e-100);
        assertEquals(20, c3.getHeight(), 1e-100);
    }

    @Test public void testComputeChildPrefAreaWidthHonorsMaxWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMaxWidth(100); // max less than pref
        pane.getChildren().add(child);

        assertEquals(100, RegionShim.computeChildPrefAreaWidth(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test public void testComputeChildPrefAreaHeightHonorsMaxWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMaxHeight(100); // max less than pref
        pane.getChildren().add(child);

        assertEquals(100, RegionShim.computeChildPrefAreaHeight(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test public void testComputeChildPrefAreaWidthHonorsMinWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinWidth(400); // max less than pref
        pane.getChildren().add(child);

        assertEquals(400, RegionShim.computeChildPrefAreaWidth(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test public void testComputeChildPrefAreaHeightHonorsMinWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinHeight(400); // max less than pref
        pane.getChildren().add(child);

        assertEquals(400, RegionShim.computeChildPrefAreaHeight(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test public void testComputeChildPrefAreaWidthHonorsMinWidthOverMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinWidth(600); // max less than pref
        pane.getChildren().add(child);

        assertEquals(600, RegionShim.computeChildPrefAreaWidth(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test public void testComputeChildPrefAreaHeightHonorsMinWidthOverMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinHeight(600); // max less than pref
        pane.getChildren().add(child);

        assertEquals(600, RegionShim.computeChildPrefAreaHeight(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test public void testChildMinAreaWidth() {
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.HORIZONTAL, 100, 100);
        Region c2 = new MockBiased(Orientation.VERTICAL, 100, 100);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c1, new Insets(1)), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c1, 0, new Insets(1), 50, false), 1e-100);
        assertEquals(102, RegionShim.computeChildMinAreaWidth(pane, c2, new Insets(1)), 1e-100);
        assertEquals(2 + Math.ceil(100*100/48.0), RegionShim.computeChildMinAreaWidth(pane, c2, -1, new Insets(1), 50, false), 1e-100); // vertically biased, effective height is 49
        assertEquals(2 + Math.ceil(100*100/38.0), RegionShim.computeChildMinAreaWidth(pane, c2, 10, new Insets(1), 50, false), 1e-100); // vertically biased, effective height is 49
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c3, new Insets(1)), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c3, 0, new Insets(1), 50, false), 1e-100);

    }

    @Test public void testChildMinAreaHeight() {
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.HORIZONTAL, 100, 100);
        Region c2 = new MockBiased(Orientation.VERTICAL, 100, 100);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        assertEquals(3, RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), -1), 1e-100); /*Insets + minimal for biased is 1 */
        assertEquals(2 + Math.ceil(100*100/48.0), RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), 50), 1e-100);
        assertEquals(12 + Math.ceil(100*100/48.0), RegionShim.computeChildMinAreaHeight(pane, c1, 10, new Insets(1), 50), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), -1), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), 50), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), -1), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), 50), 1e-100);
    }

    @Test public void testChildMaxAreaWidth() {
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.HORIZONTAL, 100, 100);
        Region c2 = new MockBiased(Orientation.VERTICAL, 100, 100);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        assertEquals(10002, RegionShim.computeChildMaxAreaWidth(pane,c1, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(10002, RegionShim.computeChildMaxAreaWidth(pane,c1, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(102, RegionShim.computeChildMaxAreaWidth(pane,c2, -1,  new Insets(1), -1, false), 1e-100); // Vertival bias is not applied when no height/baseline offset is set
        assertEquals(2 + Math.ceil(100*100/48.0), RegionShim.computeChildMaxAreaWidth(pane,c2, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(2 + Math.ceil(100*100/38.0), RegionShim.computeChildMaxAreaWidth(pane,c2, 10, new Insets(1), 50, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaWidth(pane,c3, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaWidth(pane,c3, -1, new Insets(1), 50, false), 1e-100);
    }

    @Test public void testChildMaxAreaHeight() {
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.HORIZONTAL, 100, 100);
        Region c2 = new MockBiased(Orientation.VERTICAL, 100, 100);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane,c1, -1, new Insets(1), -1), 1e-100);
        assertEquals(2 + Math.ceil(100*100/48.0), RegionShim.computeChildMaxAreaHeight(pane,c1, -1, new Insets(1), 50), 1e-100);
        assertEquals(12 + Math.ceil(100*100/48.0), RegionShim.computeChildMaxAreaHeight(pane,c1, 10, new Insets(1), 50), 1e-100);
        assertEquals(10002, RegionShim.computeChildMaxAreaHeight(pane,c2, -1, new Insets(1), -1), 1e-100);
        assertEquals(10002, RegionShim.computeChildMaxAreaHeight(pane,c2, -1, new Insets(1), 50), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane,c3, -1, new Insets(1), -1), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane,c3, -1, new Insets(1), 50), 1e-100);
    }

    /**************************************************************************
     *                                                                        *
     *    Test that images which are background loaded, or images which can   *
     *    change (such as WritableImage or animated gif) will cause a         *
     *    listener to be installed. Also that a non-animating background      *
     *    loaded image will have the listener removed when the image          *
     *    finishes loading, and that all listeners are removed from images    *
     *    which have been removed from the Region. Also that any animating    *
     *    or background loaded image will cause a repaint to happen when the  *
     *    underlying platform image changes.                                  *
     *                                                                        *
     *************************************************************************/

    @Test public void testBackgroundLoadedBackgroundImageHasListenerInstalled() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);

        assertTrue(r.listenerAdded.get());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().finish();
    }

    @Test public void testBackgroundLoadedBackgroundImageStillLoadingButRemovedFromRegionHasListenerRemoved() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);
        r.setBackground(null);

        assertFalse(r.listenerAdded.get());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().finish();
    }

    @Test public void testBackgroundLoadedBackgroundImageWhichFinishesLoadingHasListenerRemoved() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);
        image.updateProgress(1);
        image.updateVisuals();

        assertFalse(r.listenerAdded.get());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().finish();
    }

    @Test public void testBackgroundLoadedBackgroundImageWhichFinishesLoadingCausesRepaint() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);
        r.clearDirty();
        assertFalse(r.willBeRepainted());
        image.updateProgress(1);
        image.updateVisuals();

        assertTrue(r.willBeRepainted());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().finish();
    }

    @Test public void testBackgroundLoadedBorderImageHasListenerInstalled() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);

        assertTrue(r.listenerAdded.get());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().finish();
    }

    @Test public void testBackgroundLoadedBorderImageStillLoadingButRemovedFromRegionHasListenerRemoved() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);
        r.setBorder(null);

        assertFalse(r.listenerAdded.get());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().finish();
    }

    @Test public void testBackgroundLoadedBorderImageWhichFinishesLoadingHasListenerRemoved() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);
        image.updateProgress(1);
        image.updateVisuals();

        assertFalse(r.listenerAdded.get());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().cancel();
    }

    @Test public void testBackgroundLoadedBorderImageWhichFinishesLoadingCausesRepaint() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);
        r.clearDirty();
        assertFalse(r.willBeRepainted());
        image.updateProgress(1);
        image.updateVisuals();

        assertTrue(r.willBeRepainted());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().cancel();
    }

    @Test public void testAnimatedBackgroundImageHasListenerInstalled() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);
        assertTrue(r.listenerAdded.get());
    }

    @Test public void testAnimatedBackgroundImageRemovedFromRegionHasListenerRemoved() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);
        r.setBackground(null);
        assertFalse(r.listenerAdded.get());
    }

    @Test public void testAnimatedBackgroundImageCausesRepaintWhenAnimationChanges() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);
        r.clearDirty();
        assertFalse(r.willBeRepainted());
        image.getPixelWriter().setArgb(0, 0, 100);
        assertTrue(r.willBeRepainted());
    }

    @Test public void testAnimatedBorderImageHasListenerInstalled() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);
        assertTrue(r.listenerAdded.get());
    }

    @Test public void testAnimatedBorderImageRemovedFromRegionHasListenerRemoved() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);
        r.setBorder(null);
        assertFalse(r.listenerAdded.get());
    }

    @Test public void testAnimatedBorderImageCausesRepaintWhenAnimationChanges() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);
        r.clearDirty();
        assertFalse(r.willBeRepainted());
        image.getPixelWriter().setArgb(0, 0, 100);
        assertTrue(r.willBeRepainted());
    }

    @Test public void testBorderChangeUpdatesTheInsets() {
        Region r = new Region();

        r.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, BorderWidths.DEFAULT, new Insets(10))));
        assertEquals(new Insets(11), r.getInsets());

        r.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null, new Insets(20))));

        assertEquals(new Insets(21), r.getInsets());
    }

    static final class ImageRegion extends RegionShim {
        AtomicBoolean listenerAdded = new AtomicBoolean(false);

        @Override public void addImageListener(Image image) {
            super.addImageListener(image);
            listenerAdded.set(true);
        }

        @Override public void removeImageListener(Image image) {
            super.removeImageListener(image);
            listenerAdded.set(false);
        }

        public boolean willBeRepainted() {
            return NodeShim.isDirty(this, DirtyBits.NODE_CONTENTS);
        }

        public void clearDirty() {
            NodeShim.clearDirty(this, DirtyBits.NODE_CONTENTS);
        }
    };

    // Test for RT-13820
    @Test public void changingShapeElementsShouldResultInRender() {
        Region r = new Region();
        r.setPrefWidth(640);
        r.setPrefHeight(480);
        LineTo lineTo;
        Path p = new Path(
                new MoveTo(0, 0),
                lineTo = new LineTo(100, 0),
                new LineTo(50, 100),
                new ClosePath()
        );
        r.setBackground(new Background(new BackgroundFill(Color.BLUE, null, null)));
        r.setCenterShape(true);
        r.setScaleShape(true);
        r.setShape(p);
        NodeHelper.syncPeer(r);

        NGRegion peer = NodeHelper.getPeer(r);
        assertFalse(peer.isClean());
        peer.clearDirtyTree();
        assertTrue(peer.isClean());

        lineTo.setX(200);
        NodeHelper.syncPeer(p);
        NodeHelper.syncPeer(r);
        assertFalse(peer.isClean());
    }

    // Test for JDK-8255415
    @Test public void snappingASnappedValueGivesTheSameValueTest() {
        Stage stage = new Stage();
        Region region = new Region();
        Scene scene = new Scene(region);
        stage.setScene(scene);

        double[] scales = new double[] {1.0, 1.25, 1.5, 1.75, 2.0, 1.374562997};

        // test snapSizeX/snapSizeY methods

        for (double scale : scales) {
            stage.setRenderScaleX(scale);
            for (int j = 0; j < 1000; j++) {
                double value = new Random().nextDouble() * 100 - 50;
                double snappedValue = region.snapSizeX(value);
                double snapOfSnappedValue = region.snapSizeX(snappedValue);
                assertEquals(snappedValue, snapOfSnappedValue, 1.0e-14);
            }
        }

        for (double scale : scales) {
            stage.setRenderScaleY(scale);
            for (int j = 0; j < 1000; j++) {
                double value = new Random().nextDouble() * 100 - 50;
                double snappedValue = region.snapSizeY(value);
                double snapOfSnappedValue = region.snapSizeY(snappedValue);
                assertEquals(snappedValue, snapOfSnappedValue, 1.0e-14);
            }
        }

        // test snapPortionX/snapPortionY methods

        for (double scale : scales) {
            stage.setRenderScaleX(scale);
            for (int j = 0; j < 1000; j++) {
                double value = new Random().nextDouble() * 100 - 50;
                double snappedValue = RegionShim.snapPortionX(region, value);
                double snapOfSnappedValue = RegionShim.snapPortionX(region, snappedValue);
                assertEquals(snappedValue, snapOfSnappedValue, 1.0e-14);
            }
        }

        for (double scale : scales) {
            stage.setRenderScaleY(scale);
            for (int j = 0; j < 1000; j++) {
                double value = new Random().nextDouble() * 100 - 50;
                double snappedValue = RegionShim.snapPortionY(region, value);
                double snapOfSnappedValue = RegionShim.snapPortionY(region, snappedValue);
                assertEquals(snappedValue, snapOfSnappedValue, 1.0e-14);
            }
        }
    }
}

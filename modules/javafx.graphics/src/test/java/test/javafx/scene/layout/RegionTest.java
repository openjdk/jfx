/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
import test.com.sun.javafx.pgstub.StubToolkit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */
public class RegionTest {

    @Test
    public void testPaddingEmptyByDefault() {
        Region region = new Region();

        assertEquals(Insets.EMPTY, region.getPadding());
    }

    @Test
    public void testPaddingCannotBeSetToNull() {
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

    @Test
    public void testInsetsEqualsPaddingByDefault() {
        Region region = new Region();

        assertEquals(region.getInsets(), region.getPadding());
    }

    @Test
    public void testBoundedSizeReturnsPrefWhenPrefBetweenMinAndMax() {
        assertEquals(200, RegionShim.boundedSize(100, 200, 300), 0);
    }

    @Test
    public void testBoundedSizeReturnsMinWhenMinGreaterThanPrefButLessThanMax() {
        assertEquals(200, RegionShim.boundedSize(200, 100, 300), 0);
    }

    @Test
    public void testBoundedSizeReturnsMinWhenMinGreaterThanPrefAndMax() {
        assertEquals(300, RegionShim.boundedSize(300, 100, 200), 0);
    }

    @Test
    public void testBoundedSizeReturnsMaxWhenMaxLessThanPrefButGreaterThanMin() {
        assertEquals(200, RegionShim.boundedSize(100, 300, 200), 0);
    }

    @Test
    public void testBoundedSizeReturnsMinWhenMaxLessThanPrefAndMin() {
        assertEquals(200, RegionShim.boundedSize(200, 300, 100), 0);
    }

    @Test
    public void testBoundedSizeReturnsMinWhenMaxLessThanPrefAndMinAndPrefLessThanMin() {
        assertEquals(300, RegionShim.boundedSize(300, 200, 100), 0);
    }

    @Test
    public void testMinWidthOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(10, region.minWidth(-1), 1e-100);
        region.setMinWidth(25.0);
        assertEquals(25, region.getMinWidth(), 1e-100);
        assertEquals(25, region.minWidth(-1), 1e-100);
    }

    @Test
    public void testMinWidthOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setMinWidth(75.0);
        region.setMinWidth(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getMinWidth(), 1e-100);
        assertEquals(10, region.minWidth(-1), 1e-100);
    }

    @Test
    public void testMinWidthNaNTreatedAsZero() {
        Region region = new Region();
        region.setMinWidth(Double.NaN);
        assertEquals(0, region.minWidth(-1), 0);
        assertEquals(0, region.minWidth(5), 0);
    }

    @Test
    public void testMinWidthNegativeTreatedAsZero() {
        Region region = new Region();
        region.setMinWidth(-10);
        assertEquals(0, region.minWidth(-1), 0);
        assertEquals(0, region.minWidth(5), 0);
    }

    @Test
    public void testMinHeightOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(20, region.minHeight(-1), 1e-100);
        region.setMinHeight(30.0);
        assertEquals(30, region.getMinHeight(), 1e-100);
        assertEquals(30, region.minHeight(-1), 1e-100);
    }

    @Test
    public void testMinHeightOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setMinHeight(75.0);
        region.setMinHeight(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getMinHeight(), 1e-100);
        assertEquals(20, region.minHeight(-1), 1e-100);
    }

    @Test
    public void testMinHeightNaNTreatedAsZero() {
        Region region = new Region();
        region.setMinHeight(Double.NaN);
        assertEquals(0, region.minHeight(-1), 0);
        assertEquals(0, region.minHeight(5), 0);
    }

    @Test
    public void testMinHeightNegativeTreatedAsZero() {
        Region region = new Region();
        region.setMinHeight(-10);
        assertEquals(0, region.minHeight(-1), 0);
        assertEquals(0, region.minHeight(5), 0);
    }

    @Test
    public void testMinWidthOverrideSetToPref() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(10, region.minWidth(-1), 1e-100);
        region.setMinWidth(Region.USE_PREF_SIZE);
        assertEquals(Region.USE_PREF_SIZE, region.getMinWidth(), 1e-100);
        assertEquals(100, region.minWidth(-1), 1e-100);
    }

    @Test
    public void testMinHeightOverrideSetToPref() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(20, region.minHeight(-1), 1e-100);
        region.setMinHeight(Region.USE_PREF_SIZE);
        assertEquals(Region.USE_PREF_SIZE, region.getMinHeight(), 1e-100);
        assertEquals(200, region.minHeight(-1), 1e-100);
    }

    @Test
    public void testPrefWidthOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(100, region.prefWidth(-1), 1e-100);
        region.setPrefWidth(150.0);
        assertEquals(150, region.getPrefWidth(), 1e-100);
        assertEquals(150, region.prefWidth(-1), 1e-100);
    }

    @Test
    public void testPrefWidthOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setPrefWidth(150.0);
        region.setPrefWidth(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getPrefWidth(), 1e-100);
        assertEquals(100, region.prefWidth(-1), 1e-100);
    }

    @Test
    public void testPrefWidthNaNTreatedAsZero() {
        Region region = new Region();
        region.setPrefWidth(Double.NaN);
        assertEquals(0, region.prefWidth(-1), 0);
        assertEquals(0, region.prefWidth(5), 0);
    }

    @Test
    public void testPrefWidthNegativeTreatedAsZero() {
        Region region = new Region();
        region.setPrefWidth(-10);
        assertEquals(0, region.prefWidth(-1), 0);
        assertEquals(0, region.prefWidth(5), 0);
    }

    @Test
    public void testPrefHeightOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(200, region.prefHeight(-1), 1e-100);
        region.setPrefHeight(300.0);
        assertEquals(300, region.getPrefHeight(), 1e-100);
        assertEquals(300, region.prefHeight(-1), 1e-100);
    }

    @Test
    public void testPrefHeightOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setPrefHeight(250);
        region.setPrefHeight(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getPrefHeight(), 1e-100);
        assertEquals(200, region.prefHeight(-1), 1e-100);
    }

    @Test
    public void testPrefHeightNaNTreatedAsZero() {
        Region region = new Region();
        region.setPrefHeight(Double.NaN);
        assertEquals(0, region.prefHeight(-1), 0);
        assertEquals(0, region.prefHeight(5), 0);
    }

    @Test
    public void testPrefHeightNegativeTreatedAsZero() {
        Region region = new Region();
        region.setPrefHeight(-10);
        assertEquals(0, region.prefHeight(-1), 0);
        assertEquals(0, region.prefHeight(5), 0);
    }

    @Test
    public void testMaxWidthOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(500, region.maxWidth(-1), 1e-100);
        region.setMaxWidth(550);
        assertEquals(550, region.getMaxWidth(), 1e-100);
        assertEquals(550, region.maxWidth(-1), 1e-100);
    }

    @Test
    public void testMaxWidthOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setMaxWidth(1000);
        region.setMaxWidth(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getMaxWidth(), 1e-100);
        assertEquals(500, region.maxWidth(-1), 1e-100);
    }

    @Test
    public void testMaxWidthNaNTreatedAsZero() {
        Region region = new Region();
        region.setMaxWidth(Double.NaN);
        assertEquals(0, region.maxWidth(-1), 0);
        assertEquals(0, region.maxWidth(5), 0);
    }

    @Test
    public void testMaxWidthNegativeTreatedAsZero() {
        Region region = new Region();
        region.setMaxWidth(-10);
        assertEquals(0, region.maxWidth(-1), 0);
        assertEquals(0, region.maxWidth(5), 0);
    }

    @Test
    public void testMaxHeightOverride() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(600, region.maxHeight(-1), 1e-100);
        region.setMaxHeight(650);
        assertEquals(650, region.getMaxHeight(), 1e-100);
        assertEquals(650, region.maxHeight(-1), 1e-100);
    }

    @Test
    public void testMaxHeightOverrideThenRestoreComputedSize() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        region.setMaxHeight(800);
        region.setMaxHeight(Region.USE_COMPUTED_SIZE); // reset
        assertEquals(Region.USE_COMPUTED_SIZE, region.getMaxHeight(), 0);
        assertEquals(600, region.maxHeight(-1), 1e-100);
    }

    @Test
    public void testMaxHeightNaNTreatedAsZero() {
        Region region = new Region();
        region.setMaxHeight(Double.NaN);
        assertEquals(0, region.maxHeight(-1), 0);
        assertEquals(0, region.maxHeight(5), 0);
    }

    @Test
    public void testMaxHeightNegativeTreatedAsZero() {
        Region region = new Region();
        region.setMaxHeight(-10);
        assertEquals(0, region.maxHeight(-1), 0);
        assertEquals(0, region.maxHeight(5), 0);
    }

    @Test
    public void testMaxWidthOverrideSetToPref() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(500, region.maxWidth(-1), 1e-100);
        region.setMaxWidth(Region.USE_PREF_SIZE);
        assertEquals(Region.USE_PREF_SIZE, region.getMaxWidth(), 0);
        assertEquals(100, region.maxWidth(-1), 1e-100);
    }

    @Test
    public void testMaxHeightOverrideSetToPref() {
        Region region = new MockRegion(10,20, 100,200, 500,600);
        assertEquals(600, region.maxHeight(-1), 1e-100);
        region.setMaxHeight(Region.USE_PREF_SIZE);
        assertEquals(Region.USE_PREF_SIZE, region.getMaxHeight(), 0);
        assertEquals(200, region.maxHeight(-1), 1e-100);
    }

    @Test
    public void testPositionInAreaForResizableForResizableTopLeft() {
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

    @Test
    public void testPositionInAreaForResizableTopCenter() {
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

    @Test
    public void testPositionInAreaForResizableTopRight() {
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

    @Test
    public void testPositionInAreaForResizableCenterLeft() {
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

//    // See JDK-8127910
//    @Test
//    public void testPositionInAreaForNONResizableCenterLeft() {
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


    @Test
    public void testPositionInAreaForResizableCenter() {
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

//    // See JDK-8127910
//    @Test
//    public void testPositionInAreaForNONResizableCenter() {
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

    @Test
    public void testPositionInAreaForResizableCenterRight() {
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

    @Test
    public void testPositionInAreaForResizableBottomLeft() {
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

//    // See JDK-8127910
//    @Test
//    public void testPositionInAreaForNONResizableBottomLeft() {
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

    @Test
    public void testPositionInAreaForResizableBottomCenter() {
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

//    // See JDK-8127910
//    @Test
//    public void testPositionInAreaForNONResizableBottomCenter() {
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

    @Test
    public void testPositionInAreaForResizableBottomRight() {
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

//    // See JDK-8127910
//    @Test
//    public void testPositionInAreaForNONResizableBottomRight() {
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

    @Test
    public void testPositionInAreaForResizableBaselineLeft() {
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

//    // See JDK-8127910
//    @Test
//    public void testPositionInAreaForNONResizableBaselineLeft() {
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

    @Test
    public void testPositionInAreaForResizableBaselineCenter() {
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

//    // See JDK-8127910
//    @Test
//    public void testPositionInAreaForNONResizableBaselineCenter() {
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

    @Test
    public void testPositionInAreaForResizableBaselineRight() {
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

//    // See JDK-8127910
//    @Test
//    public void testPositionInAreaForNONResizableBaselineRight() {
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

    @Test
    public void testLayoutInAreaWithLargerMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 300,300);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(100, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);

    }

    @Test
    public void testLayoutInAreaWithSmallerMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(50, child.getWidth(), 1e-100);
        assertEquals(60, child.getHeight(), 1e-100);
        assertEquals(35, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);

    }

    @Test
    public void testLayoutInAreaWithLargerMin() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 5, 5, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(10, child.getWidth(), 1e-100);
        assertEquals(20, child.getHeight(), 1e-100);
        assertEquals(8, child.getLayoutX(), 1e-100);
        assertEquals(3, child.getLayoutY(), 1e-100);

    }

    @Test
    public void testLayoutInAreaWithSizeOverrides() {
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

    @Test
    public void testLayoutInAreaWithMaxConstrainedToPref() {
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

    @Test
    public void testLayoutInAreaHonorsMaxWidthOverPref() {
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

    @Test
    public void testLayoutInAreaHonorsMaxHeightOverPref() {
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

    @Test
    public void testLayoutInAreaHonorsMinWidthOverPref() {
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

    @Test
    public void testLayoutInAreaHonorsMinHeightOverPref() {
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

    @Test
    public void testLayoutInAreaHonorsMinWidthOverMax() {
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

    @Test
    public void testLayoutInAreaHonorsMinHeightOverMax() {
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

    @Test
    public void testLayoutInAreaHonorsAreaWidthOverPrefWithFillWidth() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 100, 400, 0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test
    public void testLayoutInAreaHonorsAreaHeightOverPrefWithFillHeight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 100, 0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER);

        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(100, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test
    public void testLayoutInAreaHonorsAreaWidthOverPrefWithNOFill() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 100, 400, 0, Insets.EMPTY, false, false, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(300, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(60, child.getLayoutY(), 1e-100);
    }

    @Test
    public void testLayoutInAreaHonorsAreaHeightOverPrefWithNOFill() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        RegionShim.layoutInArea(pane, child, 10, 10, 300, 100, 0, Insets.EMPTY, false, false, HPos.CENTER, VPos.CENTER);

        assertEquals(200, child.getWidth(), 1e-100);
        assertEquals(100, child.getHeight(), 1e-100);
        assertEquals(60, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test
    public void testLayoutInAreaWithBaselineOffset() {
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

    @Test
    public void testComputeChildPrefAreaWidthHonorsMaxWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMaxWidth(100); // max less than pref
        pane.getChildren().add(child);

        assertEquals(100, RegionShim.computeChildPrefAreaWidth(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test
    public void testComputeChildPrefAreaHeightHonorsMaxWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMaxHeight(100); // max less than pref
        pane.getChildren().add(child);

        assertEquals(100, RegionShim.computeChildPrefAreaHeight(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test
    public void testComputeChildPrefAreaWidthHonorsMinWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinWidth(400); // max less than pref
        pane.getChildren().add(child);

        assertEquals(400, RegionShim.computeChildPrefAreaWidth(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test
    public void testComputeChildPrefAreaHeightHonorsMinWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinHeight(400); // max less than pref
        pane.getChildren().add(child);

        assertEquals(400, RegionShim.computeChildPrefAreaHeight(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test
    public void testComputeChildPrefAreaWidthHonorsMinWidthOverMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinWidth(600); // max less than pref
        pane.getChildren().add(child);

        assertEquals(600, RegionShim.computeChildPrefAreaWidth(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test
    public void testComputeChildPrefAreaHeightHonorsMinWidthOverMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinHeight(600); // max less than pref
        pane.getChildren().add(child);

        assertEquals(600, RegionShim.computeChildPrefAreaHeight(pane, child, Insets.EMPTY), 1e-100);
    }

    @Test
    public void testChildMinAreaWidthExtensively() {
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.VERTICAL, 100, 200);
        Region c2 = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        /*
         * Ensure that no fillHeight/height combinations have effect on controls that are not vertically biased:
         */

        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c2, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c2, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c2, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c2, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c2, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c2, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c2, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c2, -1, new Insets(1), 50000, true), 1e-100);

        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c3, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c3, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c3, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c3, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c3, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c3, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c3, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaWidth(pane, c3, -1, new Insets(1), 50000, true), 1e-100);

        /*
         * Tests biased control with no available height provided:
         *
         * Note: MockBiased returns a minimum height based on its preferred width.
         *
         * Expect 102 == insets + minWidth(-1)
         * - insets are 1 + 1 = 2
         * - minWidth(-1) returns 100 as MockBiased will base the minimum width on a
         *   reasonable height (in this case the result of prefHeight(-1) which is 200).
         *   When the MockBiased is 200 high, it becomes 100 wide.
         */
        assertEquals(2 + 100, RegionShim.computeChildMinAreaWidth(pane, c1, -1, new Insets(1), -1, false), 1e-100);

        /*
         * Ensure that fillHeight has no effect when there is no available height provided:
         */

        assertEquals(2 + 100, RegionShim.computeChildMinAreaWidth(pane, c1, -1, new Insets(1), -1, true), 1e-100);

        /*
         * When the given available height is less than a vertically biased control's preferred height, then
         * fillHeight should not have any effect as in both cases the height to use for determine the width
         * is capped at the smallest of the two height values; expect the control to be resized to the
         * available height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMinAreaWidth(pane, c1, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMinAreaWidth(pane, c1, -1, new Insets(1), 50, false), 1e-100);

        // with baseline
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2 - 10)), RegionShim.computeChildMinAreaWidth(pane, c1, 10, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2 - 10)), RegionShim.computeChildMinAreaWidth(pane, c1, 10, new Insets(1), 50, false), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's preferred height, then
         * fillHeight decides which of the two is used; when fillHeight is true, expect the control to be resized to the
         * available height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (500.0 - 2)), RegionShim.computeChildMinAreaWidth(pane, c1, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (500.0 - 2 - 10)), RegionShim.computeChildMinAreaWidth(pane, c1, 10, new Insets(1), 500, true), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's preferred height, then
         * fillHeight decides which of the two is used; when fillHeight is false, expect the control to be resized to the
         * its preferred height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(102, RegionShim.computeChildMinAreaWidth(pane, c1, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(102, RegionShim.computeChildMinAreaWidth(pane, c1, 10, new Insets(1), 500, false), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's maximum height, then
         * fillHeight decides which of the two is used; when fillHeight is true, expect the control to be resized to the
         * available height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(2 + 1, RegionShim.computeChildMinAreaWidth(pane, c1, -1, new Insets(1), 50000, true), 1e-100);
        assertEquals(2 + 1, RegionShim.computeChildMinAreaWidth(pane, c1, 10, new Insets(1), 50000, true), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's maximum height, then
         * fillHeight decides which of the two is used; when fillHeight is false, expect the control to be resized to the
         * its maximum height, and that its width will be derived from this height as it is vertically biased:
         *
         * Note: MockBiased returns a maximum height based on its preferred width.
         */

        assertEquals(2 + 100, RegionShim.computeChildMinAreaWidth(pane, c1, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(2 + 100, RegionShim.computeChildMinAreaWidth(pane, c1, 10, new Insets(1), 50000, false), 1e-100);
    }

    @Test
    public void testChildMinAreaHeightExtensively() {
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        Region c2 = new MockBiased(Orientation.VERTICAL, 100, 200);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        /*
         * Ensure that no fillWidth/width combinations have effect on controls that are not horizontally biased:
         */

        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), 50000, true), 1e-100);

        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), 50000, true), 1e-100);

        /*
         * Tests biased control with no available width provided:
         *
         * Note: MockBiased returns a minimum width based on its preferred height.
         *
         * Expect 202 == insets + minHeight(-1)
         * - insets are 1 + 1 = 2
         * - minHeight(-1) returns 200 as MockBiased will base the minimum height on a
         *   reasonable width (in this case the result of prefWidth(-1) which is 100).
         *   When the MockBiased is 100 wide, it becomes 200 high.
         */
        assertEquals(2 + 200, RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), -1, false), 1e-100);

        /*
         * Ensure that fillWidth has no effect when there is no available width provided:
         */

        assertEquals(2 + 200, RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), -1, true), 1e-100);

        /*
         * When the given available width is less than a horizontally biased control's preferred width, then
         * fillWidth should not have any effect as in both cases the width to use for determine the height
         * is capped at the smallest of the two width values; expect the control to be resized to the
         * available width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), 50, false), 1e-100);

        // with baseline
        assertEquals(2 + 10 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMinAreaHeight(pane, c1, 10, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + 10 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMinAreaHeight(pane, c1, 10, new Insets(1), 50, false), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's preferred width, then
         * fillWidth decides which of the two is used; when fillWidth is true, expect the control to be resized to the
         * available width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (500.0 - 2)), RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(2 + 10 + Math.ceil(100 * 200 / (500.0 - 2)), RegionShim.computeChildMinAreaHeight(pane, c1, 10, new Insets(1), 500, true), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's preferred width, then
         * fillWidth decides which of the two is used; when fillWidth is false, expect the control to be resized to the
         * its preferred width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + 200, RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(2 + 10 + 200, RegionShim.computeChildMinAreaHeight(pane, c1, 10, new Insets(1), 500, false), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's maximum width, then
         * fillWidth decides which of the two is used; when fillWidth is true, expect the control to be resized to the
         * available width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + 1, RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), 50000, true), 1e-100);
        assertEquals(2 + 10 + 1, RegionShim.computeChildMinAreaHeight(pane, c1, 10, new Insets(1), 50000, true), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's maximum width, then
         * fillWidth decides which of the two is used; when fillWidth is false, expect the control to be resized to the
         * its maximum width, and that its height will be derived from this width as it is horizontally biased:
         *
         * Note: MockBiased returns a maximum width based on its preferred height.
         */

        assertEquals(2 + 200, RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(2 + 10 + 200, RegionShim.computeChildMinAreaHeight(pane, c1, 10, new Insets(1), 50000, false), 1e-100);
    }

    @Test
    public void testChilPrefAreaWidthExtensively() {
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.VERTICAL, 100, 200);
        Region c2 = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        /*
         * Ensure that no fillHeight/height combinations have effect on controls that are not vertically biased:
         */

        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c2, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c2, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c2, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c2, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c2, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c2, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c2, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c2, -1, new Insets(1), 50000, true), 1e-100);

        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c3, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c3, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c3, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c3, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c3, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c3, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c3, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c3, -1, new Insets(1), 50000, true), 1e-100);

        /*
         * Tests biased control with no available height provided:
         *
         * Note: MockBiased returns a minimum height based on its preferred width.
         *
         * Expect 102 == insets + minWidth(-1)
         * - insets are 1 + 1 = 2
         * - maxWidth(-1) returns 100 as MockBiased will base the maximum width on a
         *   reasonable height (in this case the result of prefHeight(-1) which is 200).
         *   When the MockBiased is 200 high, it becomes 100 wide.
         */
        assertEquals(2 + 100, RegionShim.computeChildPrefAreaWidth(pane, c1, -1, new Insets(1), -1, false), 1e-100);

        /*
         * Ensure that fillHeight has no effect when there is no available height provided:
         */

        assertEquals(2 + 100, RegionShim.computeChildPrefAreaWidth(pane, c1, -1, new Insets(1), -1, true), 1e-100);

        /*
         * When the given available height is less than a vertically biased control's preferred height, then
         * fillHeight should not have any effect as in both cases the height to use for determine the width
         * is capped at the smallest of the two height values; expect the control to be resized to the
         * available height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildPrefAreaWidth(pane, c1, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildPrefAreaWidth(pane, c1, -1, new Insets(1), 50, false), 1e-100);

        // with baseline
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2 - 10)), RegionShim.computeChildPrefAreaWidth(pane, c1, 10, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2 - 10)), RegionShim.computeChildPrefAreaWidth(pane, c1, 10, new Insets(1), 50, false), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's preferred height, then
         * fillHeight decides which of the two is used; when fillHeight is true, expect the control to be resized to the
         * available height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (500.0 - 2)), RegionShim.computeChildPrefAreaWidth(pane, c1, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (500.0 - 2 - 10)), RegionShim.computeChildPrefAreaWidth(pane, c1, 10, new Insets(1), 500, true), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's preferred height, then
         * fillHeight decides which of the two is used; when fillHeight is false, expect the control to be resized to the
         * its preferred height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c1, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaWidth(pane, c1, 10, new Insets(1), 500, false), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's maximum height, then
         * fillHeight decides which of the two is used; when fillHeight is true, expect the control to be resized to the
         * available height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(2 + 1, RegionShim.computeChildPrefAreaWidth(pane, c1, -1, new Insets(1), 50000, true), 1e-100);
        assertEquals(2 + 1, RegionShim.computeChildPrefAreaWidth(pane, c1, 10, new Insets(1), 50000, true), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's maximum height, then
         * fillHeight decides which of the two is used; when fillHeight is false, expect the control to be resized to the
         * its maximum height, and that its width will be derived from this height as it is vertically biased:
         *
         * Note: MockBiased returns a maximum height based on its preferred width.
         */

        assertEquals(2 + 100, RegionShim.computeChildPrefAreaWidth(pane, c1, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(2 + 100, RegionShim.computeChildPrefAreaWidth(pane, c1, 10, new Insets(1), 50000, false), 1e-100);
    }

    @Test
    public void testChildPrefAreaHeightExtensively() {
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        Region c2 = new MockBiased(Orientation.VERTICAL, 100, 200);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        /*
         * Ensure that no fillWidth/width combinations have effect on controls that are not horizontally biased:
         */

        assertEquals(202, RegionShim.computeChildPrefAreaHeight(pane, c2, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(202, RegionShim.computeChildPrefAreaHeight(pane, c2, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(202, RegionShim.computeChildPrefAreaHeight(pane, c2, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(202, RegionShim.computeChildPrefAreaHeight(pane, c2, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(202, RegionShim.computeChildPrefAreaHeight(pane, c2, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(202, RegionShim.computeChildPrefAreaHeight(pane, c2, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(202, RegionShim.computeChildPrefAreaHeight(pane, c2, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(202, RegionShim.computeChildPrefAreaHeight(pane, c2, -1, new Insets(1), 50000, true), 1e-100);

        assertEquals(102, RegionShim.computeChildPrefAreaHeight(pane, c3, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaHeight(pane, c3, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaHeight(pane, c3, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaHeight(pane, c3, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaHeight(pane, c3, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaHeight(pane, c3, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaHeight(pane, c3, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(102, RegionShim.computeChildPrefAreaHeight(pane, c3, -1, new Insets(1), 50000, true), 1e-100);

        /*
         * Tests biased control with no available width provided:
         *
         * Note: MockBiased returns a maximum width based on its preferred height.
         *
         * Expect 202 == insets + maxHeight(-1)
         * - insets are 1 + 1 = 2
         * - maxHeight(-1) returns 200 as MockBiased will base the maximum height on a
         *   reasonable width (in this case the result of prefWidth(-1) which is 100).
         *   When the MockBiased is 100 wide, it becomes 200 high.
         */
        assertEquals(2 + 200, RegionShim.computeChildPrefAreaHeight(pane, c1, -1, new Insets(1), -1, false), 1e-100);

        /*
         * Ensure that fillWidth has no effect when there is no available width provided:
         */

        assertEquals(2 + 200, RegionShim.computeChildPrefAreaHeight(pane, c1, -1, new Insets(1), -1, true), 1e-100);

        /*
         * When the given available width is less than a horizontally biased control's preferred width, then
         * fillWidth should not have any effect as in both cases the width to use for determine the height
         * is capped at the smallest of the two width values; expect the control to be resized to the
         * available width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildPrefAreaHeight(pane, c1, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildPrefAreaHeight(pane, c1, -1, new Insets(1), 50, false), 1e-100);

        // with baseline
        assertEquals(2 + 10 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildPrefAreaHeight(pane, c1, 10, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + 10 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildPrefAreaHeight(pane, c1, 10, new Insets(1), 50, false), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's preferred width, then
         * fillWidth decides which of the two is used; when fillWidth is true, expect the control to be resized to the
         * available width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (500.0 - 2)), RegionShim.computeChildPrefAreaHeight(pane, c1, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(2 + 10 + Math.ceil(100 * 200 / (500.0 - 2)), RegionShim.computeChildPrefAreaHeight(pane, c1, 10, new Insets(1), 500, true), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's preferred width, then
         * fillWidth decides which of the two is used; when fillWidth is false, expect the control to be resized to the
         * its preferred width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + 200, RegionShim.computeChildPrefAreaHeight(pane, c1, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(2 + 10 + 200, RegionShim.computeChildPrefAreaHeight(pane, c1, 10, new Insets(1), 500, false), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's maximum width, then
         * fillWidth decides which of the two is used; when fillWidth is true, expect the control to be resized to the
         * available width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + 1, RegionShim.computeChildPrefAreaHeight(pane, c1, -1, new Insets(1), 50000, true), 1e-100);
        assertEquals(2 + 10 + 1, RegionShim.computeChildPrefAreaHeight(pane, c1, 10, new Insets(1), 50000, true), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's maximum width, then
         * fillWidth decides which of the two is used; when fillWidth is false, expect the control to be resized to the
         * its maximum width, and that its height will be derived from this width as it is horizontally biased:
         *
         * Note: MockBiased returns a maximum width based on its preferred height.
         */

        assertEquals(2 + 200, RegionShim.computeChildPrefAreaHeight(pane, c1, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(2 + 10 + 200, RegionShim.computeChildPrefAreaHeight(pane, c1, 10, new Insets(1), 50000, false), 1e-100);
    }

    @Test
    public void testChildMaxAreaWidthExtensively() {
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.VERTICAL, 100, 200);
        Region c2 = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        /*
         * Ensure that no fillHeight/height combinations have effect on controls that are not vertically biased:
         */

        assertEquals(20002, RegionShim.computeChildMaxAreaWidth(pane, c2, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaWidth(pane, c2, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaWidth(pane, c2, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaWidth(pane, c2, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaWidth(pane, c2, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaWidth(pane, c2, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaWidth(pane, c2, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaWidth(pane, c2, -1, new Insets(1), 50000, true), 1e-100);

        assertEquals(1002, RegionShim.computeChildMaxAreaWidth(pane, c3, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaWidth(pane, c3, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaWidth(pane, c3, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaWidth(pane, c3, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaWidth(pane, c3, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaWidth(pane, c3, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaWidth(pane, c3, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaWidth(pane, c3, -1, new Insets(1), 50000, true), 1e-100);

        /*
         * Tests biased control with no available height provided:
         *
         * Note: MockBiased returns a minimum height based on its preferred width.
         *
         * Expect 102 == insets + minWidth(-1)
         * - insets are 1 + 1 = 2
         * - maxWidth(-1) returns 100 as MockBiased will base the maximum width on a
         *   reasonable height (in this case the result of prefHeight(-1) which is 200).
         *   When the MockBiased is 200 high, it becomes 100 wide.
         */
        assertEquals(2 + 100, RegionShim.computeChildMaxAreaWidth(pane, c1, -1, new Insets(1), -1, false), 1e-100);

        /*
         * Ensure that fillHeight has no effect when there is no available height provided:
         */

        assertEquals(2 + 100, RegionShim.computeChildMaxAreaWidth(pane, c1, -1, new Insets(1), -1, true), 1e-100);

        /*
         * When the given available height is less than a vertically biased control's preferred height, then
         * fillHeight should not have any effect as in both cases the height to use for determine the width
         * is capped at the smallest of the two height values; expect the control to be resized to the
         * available height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMaxAreaWidth(pane, c1, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMaxAreaWidth(pane, c1, -1, new Insets(1), 50, false), 1e-100);

        // with baseline
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2 - 10)), RegionShim.computeChildMaxAreaWidth(pane, c1, 10, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2 - 10)), RegionShim.computeChildMaxAreaWidth(pane, c1, 10, new Insets(1), 50, false), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's preferred height, then
         * fillHeight decides which of the two is used; when fillHeight is true, expect the control to be resized to the
         * available height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (500.0 - 2)), RegionShim.computeChildMaxAreaWidth(pane, c1, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (500.0 - 2 - 10)), RegionShim.computeChildMaxAreaWidth(pane, c1, 10, new Insets(1), 500, true), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's preferred height, then
         * fillHeight decides which of the two is used; when fillHeight is false, expect the control to be resized to the
         * its preferred height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(102, RegionShim.computeChildMaxAreaWidth(pane, c1, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(102, RegionShim.computeChildMaxAreaWidth(pane, c1, 10, new Insets(1), 500, false), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's maximum height, then
         * fillHeight decides which of the two is used; when fillHeight is true, expect the control to be resized to the
         * available height, and that its width will be derived from this height as it is vertically biased:
         */

        assertEquals(2 + 1, RegionShim.computeChildMaxAreaWidth(pane, c1, -1, new Insets(1), 50000, true), 1e-100);
        assertEquals(2 + 1, RegionShim.computeChildMaxAreaWidth(pane, c1, 10, new Insets(1), 50000, true), 1e-100);

        /*
         * When the given available height is greater than a vertically biased control's maximum height, then
         * fillHeight decides which of the two is used; when fillHeight is false, expect the control to be resized to the
         * its maximum height, and that its width will be derived from this height as it is vertically biased:
         *
         * Note: MockBiased returns a maximum height based on its preferred width.
         */

        assertEquals(2 + 100, RegionShim.computeChildMaxAreaWidth(pane, c1, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(2 + 100, RegionShim.computeChildMaxAreaWidth(pane, c1, 10, new Insets(1), 50000, false), 1e-100);
    }

    @Test
    public void testChildMaxAreaHeightExtensively() {
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        Region c2 = new MockBiased(Orientation.VERTICAL, 100, 200);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        /*
         * Ensure that no fillWidth/width combinations have effect on controls that are not horizontally biased:
         */

        assertEquals(20002, RegionShim.computeChildMaxAreaHeight(pane, c2, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaHeight(pane, c2, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaHeight(pane, c2, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaHeight(pane, c2, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaHeight(pane, c2, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaHeight(pane, c2, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaHeight(pane, c2, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(20002, RegionShim.computeChildMaxAreaHeight(pane, c2, -1, new Insets(1), 50000, true), 1e-100);

        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane, c3, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane, c3, -1, new Insets(1), -1, true), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane, c3, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane, c3, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane, c3, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane, c3, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane, c3, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane, c3, -1, new Insets(1), 50000, true), 1e-100);

        /*
         * Tests biased control with no available width provided:
         *
         * Note: MockBiased returns a maximum width based on its preferred height.
         *
         * Expect 202 == insets + maxHeight(-1)
         * - insets are 1 + 1 = 2
         * - maxHeight(-1) returns 200 as MockBiased will base the maximum height on a
         *   reasonable width (in this case the result of prefWidth(-1) which is 100).
         *   When the MockBiased is 100 wide, it becomes 200 high.
         */
        assertEquals(2 + 200, RegionShim.computeChildMaxAreaHeight(pane, c1, -1, new Insets(1), -1, false), 1e-100);

        /*
         * Ensure that fillWidth has no effect when there is no available width provided:
         */

        assertEquals(2 + 200, RegionShim.computeChildMaxAreaHeight(pane, c1, -1, new Insets(1), -1, true), 1e-100);

        /*
         * When the given available width is less than a horizontally biased control's preferred width, then
         * fillWidth should not have any effect as in both cases the width to use for determine the height
         * is capped at the smallest of the two width values; expect the control to be resized to the
         * available width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMaxAreaHeight(pane, c1, -1, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMaxAreaHeight(pane, c1, -1, new Insets(1), 50, false), 1e-100);

        // with baseline
        assertEquals(2 + 10 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMaxAreaHeight(pane, c1, 10, new Insets(1), 50, true), 1e-100);
        assertEquals(2 + 10 + Math.ceil(100 * 200 / (50.0 - 2)), RegionShim.computeChildMaxAreaHeight(pane, c1, 10, new Insets(1), 50, false), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's preferred width, then
         * fillWidth decides which of the two is used; when fillWidth is true, expect the control to be resized to the
         * available width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + Math.ceil(100 * 200 / (500.0 - 2)), RegionShim.computeChildMaxAreaHeight(pane, c1, -1, new Insets(1), 500, true), 1e-100);
        assertEquals(2 + 10 + Math.ceil(100 * 200 / (500.0 - 2)), RegionShim.computeChildMaxAreaHeight(pane, c1, 10, new Insets(1), 500, true), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's preferred width, then
         * fillWidth decides which of the two is used; when fillWidth is false, expect the control to be resized to the
         * its preferred width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + 200, RegionShim.computeChildMaxAreaHeight(pane, c1, -1, new Insets(1), 500, false), 1e-100);
        assertEquals(2 + 10 + 200, RegionShim.computeChildMaxAreaHeight(pane, c1, 10, new Insets(1), 500, false), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's maximum width, then
         * fillWidth decides which of the two is used; when fillWidth is true, expect the control to be resized to the
         * available width, and that its height will be derived from this width as it is horizontally biased:
         */

        assertEquals(2 + 1, RegionShim.computeChildMaxAreaHeight(pane, c1, -1, new Insets(1), 50000, true), 1e-100);
        assertEquals(2 + 10 + 1, RegionShim.computeChildMaxAreaHeight(pane, c1, 10, new Insets(1), 50000, true), 1e-100);

        /*
         * When the given available width is greater than a horizontally biased control's maximum width, then
         * fillWidth decides which of the two is used; when fillWidth is false, expect the control to be resized to the
         * its maximum width, and that its height will be derived from this width as it is horizontally biased:
         *
         * Note: MockBiased returns a maximum width based on its preferred height.
         */

        assertEquals(2 + 200, RegionShim.computeChildMaxAreaHeight(pane, c1, -1, new Insets(1), 50000, false), 1e-100);
        assertEquals(2 + 10 + 200, RegionShim.computeChildMaxAreaHeight(pane, c1, 10, new Insets(1), 50000, false), 1e-100);
    }

    @Test
    public void testChildMinAreaWidth() {  // See improved version of this test above
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

    @Test
    public void testChildMinAreaHeight() {  // See improved version of this test above
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.HORIZONTAL, 100, 100);
        Region c2 = new MockBiased(Orientation.VERTICAL, 100, 100);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        // This used to assert 3, but this is incorrect. The control involved is biased
        // and its minWidth(-1) will never return 1 (see MockBiased code). Instead,
        // minWidth(-1) returns 100 as it assumes a reasonable height (in this case
        // the result of prefHeight(-1) which is 100).
        assertEquals(102, RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), -1, false), 1e-100);

        assertEquals(2 + Math.ceil(100*100/48.0), RegionShim.computeChildMinAreaHeight(pane, c1, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(12 + Math.ceil(100*100/48.0), RegionShim.computeChildMinAreaHeight(pane, c1, 10, new Insets(1), 50, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c2, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(12, RegionShim.computeChildMinAreaHeight(pane, c3, -1, new Insets(1), 50, false), 1e-100);
    }

    @Test
    public void testChildMaxAreaWidth() {  // See improved version of this test above
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

    @Test
    public void testChildMaxAreaHeight() {  // See improved version of this test above
        Pane pane = new Pane();

        Region c1 = new MockBiased(Orientation.HORIZONTAL, 100, 100);
        Region c2 = new MockBiased(Orientation.VERTICAL, 100, 100);
        Region c3 = new MockRegion(10, 10, 100, 100, 1000, 1000);

        pane.getChildren().addAll(c1, c2, c3);

        // This used to assert 1002, but this is incorrect.
        //
        // This control is horizontally biased, but no available width
        // is provided (it is set to -1). This means that no bias should be used
        // and the result should simply be the result of maxHeight(-1). The
        // MockBiased instance will then return 100 (see MockBiased#maxHeight
        // implementation).
        //
        // How did the old code arrive at 1002?
        //
        // There was a bug in computeChildMaxAreaHeight where the bias logic was
        // still partially executed. It would call "child.minWidth(-1)" which
        // is 10 for a horizontal MockBiased instance. Even though the bias logic
        // should be skipped, it would still use this value to do its call to maxHeight
        // instead of using -1. A call of child.maxHeight(10) is basically asking
        // what should the height be if the width is 10? As MockBiased tries to
        // always display exactly prefWidth*prefHeight pixels (100 * 100 in this
        // case) the answer is (100 * 100) / 10 = 1000. Adding the insets gets
        // us 1002.
        assertEquals(102, RegionShim.computeChildMaxAreaHeight(pane,c1, -1, new Insets(1), -1, false), 1e-100);

        assertEquals(2 + Math.ceil(100*100/48.0), RegionShim.computeChildMaxAreaHeight(pane,c1, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(12 + Math.ceil(100*100/48.0), RegionShim.computeChildMaxAreaHeight(pane,c1, 10, new Insets(1), 50, false), 1e-100);
        assertEquals(10002, RegionShim.computeChildMaxAreaHeight(pane,c2, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(10002, RegionShim.computeChildMaxAreaHeight(pane,c2, -1, new Insets(1), 50, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane,c3, -1, new Insets(1), -1, false), 1e-100);
        assertEquals(1002, RegionShim.computeChildMaxAreaHeight(pane,c3, -1, new Insets(1), 50, false), 1e-100);
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

    @Test
    public void testBackgroundLoadedBackgroundImageHasListenerInstalled() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);

        assertTrue(r.listenerAdded.get());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().finish();
    }

    @Test
    public void testBackgroundLoadedBackgroundImageStillLoadingButRemovedFromRegionHasListenerRemoved() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);
        r.setBackground(null);

        assertFalse(r.listenerAdded.get());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().finish();
    }

    @Test
    public void testBackgroundLoadedBackgroundImageWhichFinishesLoadingHasListenerRemoved() {
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

    @Test
    public void testBackgroundLoadedBackgroundImageWhichFinishesLoadingCausesRepaint() {
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

    @Test
    public void testBackgroundLoadedBorderImageHasListenerInstalled() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);

        assertTrue(r.listenerAdded.get());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().finish();
    }

    @Test
    public void testBackgroundLoadedBorderImageStillLoadingButRemovedFromRegionHasListenerRemoved() {
        final ImageForTesting image = new ImageForTesting("http://something.png", true);
        assertTrue(image.getProgress() < 1);

        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);
        r.setBorder(null);

        assertFalse(r.listenerAdded.get());

        ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory().getLastAsyncImageLoader().finish();
    }

    @Test
    public void testBackgroundLoadedBorderImageWhichFinishesLoadingHasListenerRemoved() {
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

    @Test
    public void testBackgroundLoadedBorderImageWhichFinishesLoadingCausesRepaint() {
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

    @Test
    public void testAnimatedBackgroundImageHasListenerInstalled() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);
        assertTrue(r.listenerAdded.get());
    }

    @Test
    public void testAnimatedBackgroundImageRemovedFromRegionHasListenerRemoved() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);
        r.setBackground(null);
        assertFalse(r.listenerAdded.get());
    }

    @Test
    public void testAnimatedBackgroundImageCausesRepaintWhenAnimationChanges() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Background background = new Background(new BackgroundImage(image, null, null, null, null));
        r.setBackground(background);
        r.clearDirty();
        assertFalse(r.willBeRepainted());
        image.getPixelWriter().setArgb(0, 0, 100);
        assertTrue(r.willBeRepainted());
    }

    @Test
    public void testAnimatedBorderImageHasListenerInstalled() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);
        assertTrue(r.listenerAdded.get());
    }

    @Test
    public void testAnimatedBorderImageRemovedFromRegionHasListenerRemoved() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);
        r.setBorder(null);
        assertFalse(r.listenerAdded.get());
    }

    @Test
    public void testAnimatedBorderImageCausesRepaintWhenAnimationChanges() {
        final WritableImage image = new WritableImage(10, 10);
        ImageRegion r = new ImageRegion();
        final Border border = new Border(new BorderImage(image, null, null, null, false, null, null));
        r.setBorder(border);
        r.clearDirty();
        assertFalse(r.willBeRepainted());
        image.getPixelWriter().setArgb(0, 0, 100);
        assertTrue(r.willBeRepainted());
    }

    @Test
    public void testBorderChangeUpdatesTheInsets() {
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
    }

    // Test for JDK-8112908
    @Test
    public void changingShapeElementsShouldResultInRender() {
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
        peer.clearDirty();
        assertTrue(peer.isClean());

        lineTo.setX(200);
        NodeHelper.syncPeer(p);
        NodeHelper.syncPeer(r);
        assertFalse(peer.isClean());
    }

    @Test
    public void snapFunctionsShouldHandleExtremelyLargeValuesWithoutReturningNaN() {
        Stage stage = new Stage();
        Region region = new Region();
        Scene scene = new Scene(region);
        stage.setScene(scene);

        // Size functions:

        assertEquals(Double.MAX_VALUE, region.snapSizeX(Double.MAX_VALUE), Math.ulp(Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, region.snapSizeY(Double.MAX_VALUE), Math.ulp(Double.MAX_VALUE));
        assertEquals(Double.NEGATIVE_INFINITY, region.snapSizeX(-Double.MAX_VALUE), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, region.snapSizeY(-Double.MAX_VALUE), 0.0);

        assertEquals(Double.POSITIVE_INFINITY, region.snapSizeX(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, region.snapSizeY(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, region.snapSizeX(Double.NEGATIVE_INFINITY), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, region.snapSizeY(Double.NEGATIVE_INFINITY), 0.0);

        // Space functions:

        // These are bugged because they use Math.round instead of Math.rint, but better than Double.NaN...
        assertEquals(Long.MAX_VALUE, region.snapSpaceX(Double.MAX_VALUE), 0.0);
        assertEquals(Long.MAX_VALUE, region.snapSpaceY(Double.MAX_VALUE), 0.0);
        assertEquals(Long.MIN_VALUE, region.snapSpaceX(-Double.MAX_VALUE), 0.0);
        assertEquals(Long.MIN_VALUE, region.snapSpaceY(-Double.MAX_VALUE), 0.0);

        assertEquals(Long.MAX_VALUE, region.snapSpaceX(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(Long.MAX_VALUE, region.snapSpaceY(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(Long.MIN_VALUE, region.snapSpaceX(Double.NEGATIVE_INFINITY), 0.0);
        assertEquals(Long.MIN_VALUE, region.snapSpaceY(Double.NEGATIVE_INFINITY), 0.0);

        stage.setRenderScaleX(1.5);
        stage.setRenderScaleY(1.5);

        // Size functions:

        assertEquals(Double.MAX_VALUE, region.snapSizeX(Double.MAX_VALUE), Math.ulp(Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, region.snapSizeY(Double.MAX_VALUE), Math.ulp(Double.MAX_VALUE));
        assertEquals(-Double.MAX_VALUE, region.snapSizeX(-Double.MAX_VALUE), 0.0);
        assertEquals(-Double.MAX_VALUE, region.snapSizeY(-Double.MAX_VALUE), 0.0);

        assertEquals(Double.POSITIVE_INFINITY, region.snapSizeX(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, region.snapSizeY(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, region.snapSizeX(Double.NEGATIVE_INFINITY), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, region.snapSizeY(Double.NEGATIVE_INFINITY), 0.0);

        // Space functions:

        // These are even more bugged, they divide the long max/min value by scale after using the round instead of rint
        assertEquals(Long.MAX_VALUE / 1.5, region.snapSpaceX(Double.MAX_VALUE), 0.0);
        assertEquals(Long.MAX_VALUE / 1.5, region.snapSpaceY(Double.MAX_VALUE), 0.0);
        assertEquals(Long.MIN_VALUE / 1.5, region.snapSpaceX(-Double.MAX_VALUE), 0.0);
        assertEquals(Long.MIN_VALUE / 1.5, region.snapSpaceY(-Double.MAX_VALUE), 0.0);

        assertEquals(Long.MAX_VALUE / 1.5, region.snapSpaceX(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(Long.MAX_VALUE / 1.5, region.snapSpaceY(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(Long.MIN_VALUE / 1.5, region.snapSpaceX(Double.NEGATIVE_INFINITY), 0.0);
        assertEquals(Long.MIN_VALUE / 1.5, region.snapSpaceY(Double.NEGATIVE_INFINITY), 0.0);
    }

    // Test for JDK-8255415
    @Test
    public void snappingASnappedValueGivesTheSameValueTest() {
        Stage stage = new Stage();
        Region region = new Region();
        Scene scene = new Scene(region);
        stage.setScene(scene);

        double[] scales = new double[] {1.0, 1.25, 1.5, 1.75, 2.0, 1.374562997, 20.0};
        Random random = new Random();
        long seed = random.nextLong();

        // test snapSizeX/snapSizeY methods

        String failMessage = "Seed was: " + seed;

        random.setSeed(seed);

        for (double scale : scales) {
            stage.setRenderScaleX(scale);
            for (int j = 0; j < 1000; j++) {
                double value = random.nextDouble() * Integer.MAX_VALUE;
                double snappedValue = region.snapSizeX(value);
                double snapOfSnappedValue = region.snapSizeX(snappedValue);
                assertEquals(snappedValue, snapOfSnappedValue, 0.0, failMessage);
            }
        }

        for (double scale : scales) {
            stage.setRenderScaleY(scale);
            for (int j = 0; j < 1000; j++) {
                double value = random.nextDouble() * Integer.MAX_VALUE;
                double snappedValue = region.snapSizeY(value);
                double snapOfSnappedValue = region.snapSizeY(snappedValue);
                assertEquals(snappedValue, snapOfSnappedValue, 0.0, failMessage);
            }
        }

        // test snapPortionX/snapPortionY methods

        for (double scale : scales) {
            stage.setRenderScaleX(scale);
            for (int j = 0; j < 1000; j++) {
                double value = random.nextDouble() * Integer.MAX_VALUE;
                double snappedValue = RegionShim.snapPortionX(region, value);
                double snapOfSnappedValue = RegionShim.snapPortionX(region, snappedValue);
                assertEquals(snappedValue, snapOfSnappedValue, 0.0, failMessage);
            }
        }

        for (double scale : scales) {
            stage.setRenderScaleY(scale);
            for (int j = 0; j < 1000; j++) {
                double value = random.nextDouble() * Integer.MAX_VALUE;
                double snappedValue = RegionShim.snapPortionY(region, value);
                double snapOfSnappedValue = RegionShim.snapPortionY(region, snappedValue);
                assertEquals(snappedValue, snapOfSnappedValue, 0.0, failMessage);
            }
        }
    }
}

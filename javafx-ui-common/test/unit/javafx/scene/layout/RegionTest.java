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

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.shape.Rectangle;
import org.junit.Test;

import static org.junit.Assert.*;

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
        assertEquals(200, Region.boundedSize(100, 200, 300), 0);
    }

    @Test public void testBoundedSizeReturnsMinWhenMinGreaterThanPrefButLessThanMax() {
        assertEquals(200, Region.boundedSize(200, 100, 300), 0);
    }

    @Test public void testBoundedSizeReturnsMinWhenMinGreaterThanPrefAndMax() {
        assertEquals(300, Region.boundedSize(300, 100, 200), 0);
    }

    @Test public void testBoundedSizeReturnsMaxWhenMaxLessThanPrefButGreaterThanMin() {
        assertEquals(200, Region.boundedSize(100, 300, 200), 0);
    }

    @Test public void testBoundedSizeReturnsMinWhenMaxLessThanPrefAndMin() {
        assertEquals(200, Region.boundedSize(200, 300, 100), 0);
    }

    @Test public void testBoundedSizeReturnsMinWhenMaxLessThanPrefAndMinAndPrefLessThanMin() {
        assertEquals(300, Region.boundedSize(300, 200, 100), 0);
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
        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.TOP);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableTopLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.TOP);

        assertEquals(10, child.getLayoutX(), .01);
        assertEquals(10, child.getLayoutY(), .01);
    }

    @Test public void testPositionInAreaForResizableTopCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.TOP);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(45, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableTopCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.TOP);

        assertEquals(34.8, child.getLayoutX(), .01);
        assertEquals(10, child.getLayoutY(), .01);
    }

    @Test public void testPositionInAreaForResizableTopRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.RIGHT, VPos.TOP);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(80, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableTopRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.RIGHT, VPos.TOP);

        assertEquals(59.6, child.getLayoutX(), .01);
        assertEquals(10, child.getLayoutY(), .01);
    }

    @Test public void testPositionInAreaForResizableCenterLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.CENTER);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(40, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableCenterLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.CENTER);

        assertEquals(10, child.getLayoutX(), .01);
        assertEquals(34.8, child.getLayoutY(), .01);
    }


    @Test public void testPositionInAreaForResizableCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(45, child.getLayoutX(), 1e-100);
        assertEquals(40, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(34.8, child.getLayoutX(), .01);
        assertEquals(34.8, child.getLayoutY(), .01);
    }
    
    @Test public void testPositionInAreaForResizableCenterRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.RIGHT, VPos.CENTER);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(80, child.getLayoutX(), 1e-100);
        assertEquals(40, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableCenterRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.RIGHT, VPos.CENTER);

        assertEquals(59.6, child.getLayoutX(), .01);
        assertEquals(34.8, child.getLayoutY(), .01);
    }

    @Test public void testPositionInAreaForResizableBottomLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.BOTTOM);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(70, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableBottomLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.LEFT, VPos.BOTTOM);

        assertEquals(10, child.getLayoutX(), .01);
        assertEquals(59.6, child.getLayoutY(), .01);
    }

    @Test public void testPositionInAreaForResizableBottomCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.BOTTOM);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(45, child.getLayoutX(), 1e-100);
        assertEquals(70, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableBottomCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.BOTTOM);

        assertEquals(34.8, child.getLayoutX(), .01);
        assertEquals(59.6, child.getLayoutY(), .01);
    }

    @Test public void testPositionInAreaForResizableBottomRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.RIGHT, VPos.BOTTOM);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(80, child.getLayoutX(), 1e-100);
        assertEquals(70, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableBottomRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 0, HPos.RIGHT, VPos.BOTTOM);

        assertEquals(59.6, child.getLayoutX(), .01);
        assertEquals(59.6, child.getLayoutY(), .01);
    }

    @Test public void testPositionInAreaForResizableBaselineLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60); //baseline = 30
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 50, HPos.LEFT, VPos.BASELINE);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableBaselineLeft() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 60, HPos.LEFT, VPos.BASELINE);

        assertEquals(10, child.getLayoutX(), .01);
        assertEquals(19.6, child.getLayoutY(), .01);
    }

    @Test public void testPositionInAreaForResizableBaselineCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60); //baseline = 30
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 50, HPos.CENTER, VPos.BASELINE);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(45, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableBaselineCenter() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 60, HPos.CENTER, VPos.BASELINE);

        assertEquals(34.8, child.getLayoutX(), .01);
        assertEquals(19.6, child.getLayoutY(), .01);
    }

    @Test public void testPositionInAreaForResizableBaselineRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60); //baseline = 30
        pane.getChildren().add(child);

        child.autosize();
        pane.positionInArea(child, 10, 10, 100, 100, 50, HPos.RIGHT, VPos.BASELINE);

        assertEquals(30, child.getWidth(), 1e-100);
        assertEquals(40, child.getHeight(), 1e-100);
        assertEquals(80, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
    }

    @Test public void testPositionInAreaForNONResizableBaselineRight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        Rectangle child = new Rectangle(50.4, 50.4);
        pane.getChildren().add(child);

        pane.positionInArea(child, 10, 10, 100, 100, 60, HPos.RIGHT, VPos.BASELINE);

        assertEquals(59.6, child.getLayoutX(), .01);
        assertEquals(19.6, child.getLayoutY(), .01);
    }

    @Test public void testLayoutInAreaWithLargerMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 300,300);
        pane.getChildren().add(child);

        pane.layoutInArea(child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(100, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
        
    }

    @Test public void testLayoutInAreaWithSmallerMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        pane.layoutInArea(child, 10, 10, 100, 100, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(50, child.getWidth(), 1e-100);
        assertEquals(60, child.getHeight(), 1e-100);
        assertEquals(35, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
        
    }

    @Test public void testLayoutInAreaWithLargerMin() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockResizable child = new MockResizable(10,20, 30,40, 50,60);
        pane.getChildren().add(child);

        pane.layoutInArea(child, 10, 10, 5, 5, 0, HPos.CENTER, VPos.CENTER);

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

        pane.layoutInArea(child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

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

        pane.layoutInArea(child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

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

        pane.layoutInArea(child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

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

        pane.layoutInArea(child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

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

        pane.layoutInArea(child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

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

        pane.layoutInArea(child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

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

        pane.layoutInArea(child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

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

        pane.layoutInArea(child, 10, 10, 300, 300, 0, HPos.CENTER, VPos.CENTER);

        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(600, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(-140, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsAreaWidthOverPrefWithFillWidth() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        pane.layoutInArea(child, 10, 10, 100, 400, 0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsAreaHeightOverPrefWithFillHeight() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        pane.layoutInArea(child, 10, 10, 300, 100, 0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER);

        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(100, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsAreaWidthOverPrefWithNOFill() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        pane.layoutInArea(child, 10, 10, 100, 400, 0, Insets.EMPTY, false, false, HPos.CENTER, VPos.CENTER);

        assertEquals(100, child.getWidth(), 1e-100);
        assertEquals(300, child.getHeight(), 1e-100);
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(60, child.getLayoutY(), 1e-100);
    }

    @Test public void testLayoutInAreaHonorsAreaHeightOverPrefWithNOFill() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        pane.getChildren().add(child);

        pane.layoutInArea(child, 10, 10, 300, 100, 0, Insets.EMPTY, false, false, HPos.CENTER, VPos.CENTER);

        assertEquals(200, child.getWidth(), 1e-100);
        assertEquals(100, child.getHeight(), 1e-100);
        assertEquals(60, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
    }

    @Test public void testComputeChildPrefAreaWidthHonorsMaxWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMaxWidth(100); // max less than pref
        pane.getChildren().add(child);

        assertEquals(100, pane.computeChildPrefAreaWidth(child, Insets.EMPTY), 1e-100);
    }

    @Test public void testComputeChildPrefAreaHeightHonorsMaxWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMaxHeight(100); // max less than pref
        pane.getChildren().add(child);

        assertEquals(100, pane.computeChildPrefAreaHeight(child, Insets.EMPTY), 1e-100);
    }

    @Test public void testComputeChildPrefAreaWidthHonorsMinWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinWidth(400); // max less than pref
        pane.getChildren().add(child);

        assertEquals(400, pane.computeChildPrefAreaWidth(child, Insets.EMPTY), 1e-100);
    }

    @Test public void testComputeChildPrefAreaHeightHonorsMinWidthOverPref() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinHeight(400); // max less than pref
        pane.getChildren().add(child);

        assertEquals(400, pane.computeChildPrefAreaHeight(child, Insets.EMPTY), 1e-100);
    }

    @Test public void testComputeChildPrefAreaWidthHonorsMinWidthOverMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinWidth(600); // max less than pref
        pane.getChildren().add(child);

        assertEquals(600, pane.computeChildPrefAreaWidth(child, Insets.EMPTY), 1e-100);
    }

    @Test public void testComputeChildPrefAreaHeightHonorsMinWidthOverMax() {
        Pane pane = new Pane(); // Region extension which makes children sequence public

        MockRegion child = new MockRegion(10,20, 200,300, 500,500);
        child.setMinHeight(600); // max less than pref
        pane.getChildren().add(child);

        assertEquals(600, pane.computeChildPrefAreaHeight(child, Insets.EMPTY), 1e-100);
    }
}

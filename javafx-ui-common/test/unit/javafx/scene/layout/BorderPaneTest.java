/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import javafx.geometry.Orientation;
import org.junit.Before;
import org.junit.Ignore;

import org.junit.Test;


public class BorderPaneTest {

    BorderPane borderpane;

    @Before public void setUp() {
        this.borderpane = new BorderPane();
    }

    @Test public void testEmptyBorderPane() {
        assertNull(borderpane.getTop());
        assertNull(borderpane.getCenter());
        assertNull(borderpane.getBottom());
        assertNull(borderpane.getLeft());
        assertNull(borderpane.getRight());
    }

    @Test public void testCenterChildOnly() {
        MockResizable center = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setCenter(center);

        assertEquals(center, borderpane.getCenter());
        assertEquals(1, borderpane.getChildren().size());
        assertEquals(center, borderpane.getChildren().get(0));
        assertEquals(10, borderpane.minWidth(-1), 1e-100);
        assertEquals(20, borderpane.minHeight(-1), 1e-100);
        assertEquals(100, borderpane.prefWidth(-1), 1e-100);
        assertEquals(200, borderpane.prefHeight(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, center.getLayoutX(), 1e-100);
        assertEquals(0, center.getLayoutY(), 1e-100);
        assertEquals(100, center.getWidth(), 1e-100);
        assertEquals(200, center.getHeight(), 1e-100);

    }

    @Test public void testTopChildOnly() {
        MockResizable top = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setTop(top);

        assertEquals(top, borderpane.getTop());
        assertEquals(1, borderpane.getChildren().size());
        assertEquals(top, borderpane.getChildren().get(0));
        assertEquals(10, borderpane.minWidth(-1), 1e-100);
        assertEquals(20, borderpane.minHeight(-1), 1e-100);
        assertEquals(100, borderpane.prefWidth(-1), 1e-100);
        assertEquals(200, borderpane.prefHeight(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, top.getLayoutX(), 1e-100);
        assertEquals(0, top.getLayoutY(), 1e-100);
        assertEquals(100, top.getWidth(), 1e-100);
        assertEquals(200, top.getHeight(), 1e-100);
    }

    @Test public void testBottomChildOnly() {
        MockResizable bottom = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setBottom(bottom);

        assertEquals(bottom, borderpane.getBottom());
        assertEquals(1, borderpane.getChildren().size());
        assertEquals(bottom, borderpane.getChildren().get(0));
        assertEquals(10, borderpane.minWidth(-1), 1e-100);
        assertEquals(20, borderpane.minHeight(-1), 1e-100);
        assertEquals(100, borderpane.prefWidth(-1), 1e-100);
        assertEquals(200, borderpane.prefHeight(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, bottom.getLayoutX(), 1e-100);
        assertEquals(0, bottom.getLayoutY(), 1e-100);
        assertEquals(100, bottom.getWidth(), 1e-100);
        assertEquals(200, bottom.getHeight(), 1e-100);
    }

    @Test public void testRightChildOnly() {
        MockResizable right = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setRight(right);

        assertEquals(right, borderpane.getRight());
        assertEquals(1, borderpane.getChildren().size());
        assertEquals(right, borderpane.getChildren().get(0));
        assertEquals(10, borderpane.minWidth(-1), 1e-100);
        assertEquals(20, borderpane.minHeight(-1), 1e-100);
        assertEquals(100, borderpane.prefWidth(-1), 1e-100);
        assertEquals(200, borderpane.prefHeight(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, right.getLayoutX(), 1e-100);
        assertEquals(0, right.getLayoutY(), 1e-100);
        assertEquals(100, right.getWidth(), 1e-100);
        assertEquals(200, right.getHeight(), 1e-100);
    }

    @Test public void testLeftChildOnly() {
        MockResizable left = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setLeft(left);

        assertEquals(left, borderpane.getLeft());
        assertEquals(1, borderpane.getChildren().size());
        assertEquals(left, borderpane.getChildren().get(0));
        assertEquals(10, borderpane.minWidth(-1), 1e-100);
        assertEquals(20, borderpane.minHeight(-1), 1e-100);
        assertEquals(100, borderpane.prefWidth(-1), 1e-100);
        assertEquals(200, borderpane.prefHeight(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, left.getLayoutX(), 1e-100);
        assertEquals(0, left.getLayoutY(), 1e-100);
        assertEquals(100, left.getWidth(), 1e-100);
        assertEquals(200, left.getHeight(), 1e-100);
    }

  //  @Ignore
    @Test public void testChildrenInAllPositions() {
        MockResizable center = new MockResizable(10,20, 100,200, 1000,1000);
        borderpane.setCenter(center);
        MockResizable top = new MockResizable(100,10, 200, 20, 1000,1000);
        borderpane.setTop(top);
        MockResizable bottom = new MockResizable(80,8, 220,22, 1000,1000);
        borderpane.setBottom(bottom);
        MockResizable left = new MockResizable(10,100, 15,150, 1000,1000);
        borderpane.setLeft(left);
        MockResizable right = new MockResizable(50,115, 60,120, 1000,1000);
        borderpane.setRight(right);

        assertEquals(5, borderpane.getChildren().size());
        assertEquals(center, borderpane.getCenter());
        assertEquals(top, borderpane.getTop());
        assertEquals(bottom, borderpane.getBottom());
        assertEquals(left, borderpane.getLeft());
        assertEquals(right, borderpane.getRight());
        assertTrue(borderpane.getChildren().contains(center));
        assertTrue(borderpane.getChildren().contains(top));
        assertTrue(borderpane.getChildren().contains(bottom));
        assertTrue(borderpane.getChildren().contains(left));
        assertTrue(borderpane.getChildren().contains(right));

        assertEquals(100, borderpane.minWidth(-1), 1e-100);
        assertEquals(133, borderpane.minHeight(-1), 1e-100);
        assertEquals(220, borderpane.prefWidth(-1), 1e-100);
        assertEquals(242, borderpane.prefHeight(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

// TODO Amy: re-enable the following tests once they all pass

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, top.getLayoutX(), 1e-100);
        assertEquals(0, top.getLayoutY(), 1e-100);
        assertEquals(220, top.getWidth(), 1e-100);
        assertEquals(20, top.getHeight(), 1e-100);

        assertEquals(0, left.getLayoutX(), 1e-100);
        assertEquals(20, left.getLayoutY(), 1e-100);
        assertEquals(15, left.getWidth(), 1e-100);
        assertEquals(200, left.getHeight(), 1e-100);

        assertEquals(15, center.getLayoutX(), 1e-100);
        assertEquals(20, center.getLayoutY(), 1e-100);
        assertEquals(145, center.getWidth(), 1e-100);
        assertEquals(200, center.getHeight(), 1e-100);

        assertEquals(160, right.getLayoutX(), 1e-100);
        assertEquals(20, right.getLayoutY(), 1e-100);
        assertEquals(60, right.getWidth(), 1e-100);
        assertEquals(200, right.getHeight(), 1e-100);

        assertEquals(0, bottom.getLayoutX(), 1e-100);
        assertEquals(220, bottom.getLayoutY(), 1e-100);
        assertEquals(220, bottom.getWidth(), 1e-100);
        assertEquals(22, bottom.getHeight(), 1e-100);

    }
    
    @Test public void testWithBiasedChildren() {
        MockBiased top = new MockBiased(Orientation.HORIZONTAL, 100, 20); // 280 x 7.1428
        borderpane.setTop(top);

        MockBiased left = new MockBiased(Orientation.VERTICAL, 40, 100); // 20 x 200
        borderpane.setLeft(left);

        MockBiased center = new MockBiased(Orientation.HORIZONTAL, 200, 200);
        borderpane.setCenter(center);

        MockBiased right = new MockBiased(Orientation.VERTICAL, 60, 200); // 60 x 200
        borderpane.setRight(right);

        MockBiased bottom = new MockBiased(Orientation.HORIZONTAL, 200, 20); // 280 x 14.284
        borderpane.setBottom(bottom);

        assertEquals(280, borderpane.prefWidth(-1), 1e-100);
        assertEquals(240, borderpane.prefHeight(-1), 1e-10);
        
        borderpane.resize(280, 240);
        borderpane.layout();

        assertEquals(0, top.getLayoutX(), 1e-100);
        assertEquals(0, top.getLayoutY(), 1e-100);
        assertEquals(280, top.getWidth(), 1e-100);
        assertEquals(8, top.getHeight(), 1e-100);

        assertEquals(0, left.getLayoutX(), 1e-100);
        assertEquals(8, left.getLayoutY(), 1e-100);
        assertEquals(19, left.getWidth(), 1e-100);
        assertEquals(217, left.getHeight(), 1e-100);

        assertEquals(40, center.getLayoutX(), 1e-100);
        assertEquals(5, center.getLayoutY(), 1e-100);
        assertEquals(180, center.getWidth(), 1e-100);
        assertEquals(223, center.getHeight(), 1e-100);

        assertEquals(224, right.getLayoutX(), 1e-100);
        assertEquals(8, right.getLayoutY(), 1e-100);
        assertEquals(56, right.getWidth(), 1e-100);
        assertEquals(217, right.getHeight(), 1e-100);

        assertEquals(0, bottom.getLayoutX(), 1e-100);
        assertEquals(225, bottom.getLayoutY(), 1e-100);
        assertEquals(280, bottom.getWidth(), 1e-100);
        assertEquals(15, bottom.getHeight(), 1e-100);
    }

    @Test public void testWithHorizontalBiasedChildrenAtPrefSize() {
        MockResizable top = new MockResizable(400, 100);
        borderpane.setTop(top);
        
        MockResizable left = new MockResizable(100, 100);
        borderpane.setLeft(left);

        MockBiased center = new MockBiased(Orientation.HORIZONTAL, 200, 100);
        borderpane.setCenter(center);

        MockResizable right = new MockResizable(100, 100);
        borderpane.setRight(right);

        MockResizable bottom = new MockResizable(400, 100);
        borderpane.setBottom(bottom);

        assertEquals(Orientation.HORIZONTAL, borderpane.getContentBias());
        assertEquals(400, borderpane.prefWidth(-1), 1e-200);
        assertEquals(300, borderpane.prefHeight(-1), 1e-200);

        borderpane.autosize();
        borderpane.layout();
        
        assertEquals(0, top.getLayoutX(), 1e-200);
        assertEquals(0, top.getLayoutY(), 1e-200);
        assertEquals(400, top.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, top.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, left.getLayoutX(), 1e-200);
        assertEquals(100, left.getLayoutY(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(100, center.getLayoutX(), 1e-200);
        assertEquals(100, center.getLayoutY(), 1e-200);
        assertEquals(200, center.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, center.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(300, right.getLayoutX(), 1e-200);
        assertEquals(100, right.getLayoutY(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, bottom.getLayoutX(), 1e-200);
        assertEquals(200, bottom.getLayoutY(), 1e-200);
        assertEquals(400, bottom.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, bottom.getLayoutBounds().getHeight(), 1e-200);
    }

    @Test public void testWithHorizontalBiasedChildrenGrowing() {
        MockBiased top = new MockBiased(Orientation.HORIZONTAL, 200, 100);
        borderpane.setTop(top);

        MockResizable left = new MockResizable(100, 100);
        borderpane.setLeft(left);

        MockBiased center = new MockBiased(Orientation.HORIZONTAL, 200, 100);
        borderpane.setCenter(center);

        MockResizable right = new MockResizable(100, 100);
        borderpane.setRight(right);

        MockResizable bottom = new MockResizable(400, 100);
        borderpane.setBottom(bottom);

        assertEquals(240, borderpane.prefHeight(500), 1e-200);
        borderpane.resize(500, 240);
        borderpane.layout();

        assertEquals(0, top.getLayoutX(), 1e-200);
        assertEquals(0, top.getLayoutY(), 1e-200);
        assertEquals(500, top.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(40, top.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, left.getLayoutX(), 1e-200);
        assertEquals(40, left.getLayoutY(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(100, center.getLayoutX(), 1e-200);
        // 57 because the default alignment is Pos.CENTER
        assertEquals(57, center.getLayoutY(), 1e-200);
        assertEquals(300, center.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(67, center.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(400, right.getLayoutX(), 1e-200);
        assertEquals(40, right.getLayoutY(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, bottom.getLayoutX(), 1e-200);
        assertEquals(140, bottom.getLayoutY(), 1e-200);
        assertEquals(500, bottom.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, bottom.getLayoutBounds().getHeight(), 1e-200);
    }

    @Test public void testWithHorizontalBiasedChildrenShrinking() {
        MockBiased top = new MockBiased(Orientation.HORIZONTAL, 200, 100);
        borderpane.setTop(top);

        MockResizable left = new MockResizable(100, 100);
        borderpane.setLeft(left);

        MockBiased center = new MockBiased(Orientation.HORIZONTAL, 200, 100);
        borderpane.setCenter(center);

        MockResizable right = new MockResizable(100, 100);
        borderpane.setRight(right);

        MockResizable bottom = new MockResizable(400, 100);
        borderpane.setBottom(bottom);

        assertEquals(367, borderpane.prefHeight(300), 1e-200);
        borderpane.resize(300, 367);
        borderpane.layout();

        assertEquals(0, top.getLayoutX(), 1e-200);
        assertEquals(0, top.getLayoutY(), 1e-200);
        assertEquals(300, top.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(67, top.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, left.getLayoutX(), 1e-200);
        assertEquals(67, left.getLayoutY(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(200, left.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(100, center.getLayoutX(), 1e-200);
        assertEquals(67, center.getLayoutY(), 1e-200);
        assertEquals(100, center.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(200, center.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(200, right.getLayoutX(), 1e-200);
        assertEquals(67, right.getLayoutY(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(200, right.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, bottom.getLayoutX(), 1e-200);
        assertEquals(267, bottom.getLayoutY(), 1e-200);
        assertEquals(300, bottom.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, bottom.getLayoutBounds().getHeight(), 1e-200);
    }

    @Test public void testWithVerticalBiasedChildrenAtPrefSize() {
        MockResizable top = new MockResizable(400, 100);
        borderpane.setTop(top);

        MockResizable left = new MockResizable(100, 100);
        borderpane.setLeft(left);

        MockBiased center = new MockBiased(Orientation.VERTICAL, 200, 100);
        borderpane.setCenter(center);

        MockResizable right = new MockResizable(100, 100);
        borderpane.setRight(right);

        MockResizable bottom = new MockResizable(400, 100);
        borderpane.setBottom(bottom);

        assertEquals(Orientation.VERTICAL, borderpane.getContentBias());
        assertEquals(400, borderpane.prefWidth(-1), 1e-200);
        assertEquals(300, borderpane.prefHeight(-1), 1e-200);

        borderpane.autosize();
        borderpane.layout();

        assertEquals(0, top.getLayoutX(), 1e-200);
        assertEquals(0, top.getLayoutY(), 1e-200);
        assertEquals(400, top.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, top.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, left.getLayoutX(), 1e-200);
        assertEquals(100, left.getLayoutY(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(100, center.getLayoutX(), 1e-200);
        assertEquals(100, center.getLayoutY(), 1e-200);
        assertEquals(200, center.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, center.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(300, right.getLayoutX(), 1e-200);
        assertEquals(100, right.getLayoutY(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, bottom.getLayoutX(), 1e-200);
        assertEquals(200, bottom.getLayoutY(), 1e-200);
        assertEquals(400, bottom.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, bottom.getLayoutBounds().getHeight(), 1e-200);
    }

    @Test public void testWithVerticalBiasedChildrenGrowing() {
        MockBiased top = new MockBiased(Orientation.VERTICAL, 200, 100);
        borderpane.setTop(top);

        MockResizable left = new MockResizable(100, 100);
        borderpane.setLeft(left);

        MockBiased center = new MockBiased(Orientation.VERTICAL, 200, 100);
        borderpane.setCenter(center);

        MockResizable right = new MockResizable(100, 100);
        borderpane.setRight(right);

        MockResizable bottom = new MockResizable(400, 100);
        borderpane.setBottom(bottom);
       
        assertEquals(400, borderpane.prefWidth(500), 1e-200);
        borderpane.resize(400, 500);
        borderpane.layout();

        assertEquals(0, top.getLayoutX(), 1e-200);
        assertEquals(0, top.getLayoutY(), 1e-200);
        assertEquals(100, top.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(200, top.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, left.getLayoutX(), 1e-200);
        assertEquals(200, left.getLayoutY(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(200, left.getLayoutBounds().getHeight(), 1e-200);

        // 150 because the default alignment is Pos.CENTER
        assertEquals(150, center.getLayoutX(), 1e-200);
        assertEquals(200, center.getLayoutY(), 1e-200);
        assertEquals(100, center.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(200, center.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(300, right.getLayoutX(), 1e-200);
        assertEquals(200, right.getLayoutY(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(200, right.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, bottom.getLayoutX(), 1e-200);
        assertEquals(400, bottom.getLayoutY(), 1e-200);
        assertEquals(400, bottom.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, bottom.getLayoutBounds().getHeight(), 1e-200);
    }

    @Test public void testWithVerticalBiasedChildrenShrinking() {
        MockBiased top = new MockBiased(Orientation.VERTICAL, 200, 100);
        borderpane.setTop(top);

        MockResizable left = new MockResizable(100, 100);
        borderpane.setLeft(left);

        MockBiased center = new MockBiased(Orientation.VERTICAL, 200, 100);
        borderpane.setCenter(center);

        MockResizable right = new MockResizable(100, 100);
        borderpane.setRight(right);

        MockResizable bottom = new MockResizable(400, 100);
        borderpane.setBottom(bottom);
        
        assertEquals(1000, borderpane.prefWidth(250), 1e-200);
        borderpane.resize(1000, 250);
        borderpane.layout();

        assertEquals(0, top.getLayoutX(), 1e-200);
        assertEquals(0, top.getLayoutY(), 1e-200);
        assertEquals(800, top.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(25, top.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, left.getLayoutX(), 1e-200);
        assertEquals(25, left.getLayoutY(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(125, left.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(420, center.getLayoutX(), 1e-200);
        assertEquals(25, center.getLayoutY(), 1e-200);
        assertEquals(160, center.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(125, center.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(900, right.getLayoutX(), 1e-200);
        assertEquals(25, right.getLayoutY(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(125, right.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, bottom.getLayoutX(), 1e-200);
        assertEquals(150, bottom.getLayoutY(), 1e-200);
        assertEquals(1000, bottom.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, bottom.getLayoutBounds().getHeight(), 1e-200);
    }

    @Test public void testFitsTopChildWithinBounds() {
        MockResizable child = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setTop(child);

        borderpane.resize(50,50);
        borderpane.layout();

        assertEquals(50, child.getWidth(), 1e-100);
        assertEquals(50, child.getHeight(), 1e-100);
    }

    @Test public void testFitsBottomChildWithinBounds() {
        MockResizable child = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setBottom(child);

        borderpane.resize(50,50);
        borderpane.layout();

        assertEquals(50, child.getWidth(), 1e-100);
        assertEquals(50, child.getHeight(), 1e-100);
    }

    @Test public void testFitsLeftChildWithinBounds() {
        MockResizable child = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setLeft(child);

        borderpane.resize(50,50);
        borderpane.layout();

        assertEquals(50, child.getWidth(), 1e-100);
        assertEquals(50, child.getHeight(), 1e-100);
    }

    @Test public void testFitsRightChildWithinBounds() {
        MockResizable child = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setRight(child);

        borderpane.resize(50,50);
        borderpane.layout();

        assertEquals(50, child.getWidth(), 1e-100);
        assertEquals(50, child.getHeight(), 1e-100);
    }

    @Test public void testFitsCenterChildWithinBounds() {
        MockResizable child = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setCenter(child);

        borderpane.resize(50,50);
        borderpane.layout();

        assertEquals(50, child.getWidth(), 1e-100);
        assertEquals(50, child.getHeight(), 1e-100);
    }

    @Test public void testFitsAllChildrenWithinBounds() {
        MockResizable top = new MockResizable(10,20, 200,100, 700,800);
        MockResizable bottom = new MockResizable(10,20, 200,100, 700,800);
        MockResizable left = new MockResizable(10,20, 100,200, 700,800);
        MockResizable right = new MockResizable(10,20, 100,200, 700,800);
        MockResizable center = new MockResizable(10,20, 200,200, 700,800);

        borderpane.setTop(top);
        borderpane.setBottom(bottom);
        borderpane.setLeft(left);
        borderpane.setRight(right);
        borderpane.setCenter(center);

        borderpane.resize(300,300);
        borderpane.layout();

        assertEquals(300, top.getWidth(), 1e-100);
        assertEquals(100, top.getHeight(), 1e-100);
        assertEquals(0,   top.getLayoutX(), 1e-100);
        assertEquals(0,   top.getLayoutY(), 1e-100);
        assertEquals(300, bottom.getWidth(), 1e-100);
        assertEquals(100, bottom.getHeight(), 1e-100);
        assertEquals(0,   bottom.getLayoutX(), 1e-100);
        assertEquals(200, bottom.getLayoutY(), 1e-100);
        assertEquals(100, left.getWidth(), 1e-100);
        assertEquals(100, left.getHeight(), 1e-100);
        assertEquals(0,   left.getLayoutX(), 1e-100);
        assertEquals(100, left.getLayoutY(), 1e-100);
        assertEquals(100, right.getWidth(), 1e-100);
        assertEquals(100, right.getHeight(), 1e-100);
        assertEquals(200, right.getLayoutX(), 1e-100);
        assertEquals(100, right.getLayoutY(), 1e-100);
        assertEquals(100, center.getWidth(), 1e-100);
        assertEquals(100, center.getHeight(), 1e-100);
        assertEquals(100, center.getLayoutX(), 1e-100);
        assertEquals(100, center.getLayoutY(), 1e-100);
    }

}

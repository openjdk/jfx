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

import javafx.geometry.Insets;
import static org.junit.Assert.*;
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

    @Test public void testChildrenRemovedDirectly() {
        MockResizable node = new MockResizable(10,20, 100,200, 700,800);

        borderpane.setCenter(node);
        assertSame(node, borderpane.getCenter());
        assertNull(borderpane.getLeft());
        assertNull(borderpane.getRight());
        assertNull(borderpane.getBottom());
        assertNull(borderpane.getTop());

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, node.getLayoutX(), 1e-100);
        assertEquals(0, node.getLayoutY(), 1e-100);
        assertEquals(100, node.getWidth(), 1e-100);
        assertEquals(200, node.getHeight(), 1e-100);

        borderpane.getChildren().remove(node);
        assertNull(borderpane.getCenter());

        borderpane.setLeft(node);
        assertNull(borderpane.getCenter());
        assertSame(node, borderpane.getLeft());
        assertNull(borderpane.getRight());
        assertNull(borderpane.getBottom());
        assertNull(borderpane.getTop());

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, node.getLayoutX(), 1e-100);
        assertEquals(0, node.getLayoutY(), 1e-100);
        assertEquals(100, node.getWidth(), 1e-100);
        assertEquals(200, node.getHeight(), 1e-100);

        borderpane.getChildren().remove(node);
        assertNull(borderpane.getLeft());

        borderpane.setRight(node);
        assertNull(borderpane.getCenter());
        assertNull(borderpane.getLeft());
        assertSame(node, borderpane.getRight());
        assertNull(borderpane.getBottom());
        assertNull(borderpane.getTop());

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, node.getLayoutX(), 1e-100);
        assertEquals(0, node.getLayoutY(), 1e-100);
        assertEquals(100, node.getWidth(), 1e-100);
        assertEquals(200, node.getHeight(), 1e-100);

        borderpane.getChildren().remove(node);
        assertNull(borderpane.getRight());

        borderpane.setBottom(node);
        assertNull(borderpane.getCenter());
        assertNull(borderpane.getLeft());
        assertNull(borderpane.getRight());
        assertSame(node, borderpane.getBottom());
        assertNull(borderpane.getTop());

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, node.getLayoutX(), 1e-100);
        assertEquals(0, node.getLayoutY(), 1e-100);
        assertEquals(100, node.getWidth(), 1e-100);
        assertEquals(200, node.getHeight(), 1e-100);

        borderpane.getChildren().remove(node);
        assertNull(borderpane.getBottom());

        borderpane.setTop(node);
        assertNull(borderpane.getCenter());
        assertNull(borderpane.getLeft());
        assertNull(borderpane.getRight());
        assertNull(borderpane.getBottom());
        assertSame(node, borderpane.getTop());

        borderpane.autosize();
        borderpane.layout();
        assertEquals(0, node.getLayoutX(), 1e-100);
        assertEquals(0, node.getLayoutY(), 1e-100);
        assertEquals(100, node.getWidth(), 1e-100);
        assertEquals(200, node.getHeight(), 1e-100);

        borderpane.getChildren().remove(node);
        assertNull(borderpane.getTop());

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
        assertEquals(200, borderpane.minHeight(-1), 1e-100); // Top is always at it's pref height
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
        assertEquals(200, borderpane.minHeight(-1), 1e-100);  // Bottom is always at it's pref height
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
        assertEquals(100, borderpane.minWidth(-1), 1e-100);
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
        assertEquals(100, borderpane.minWidth(-1), 1e-100);
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
        assertEquals(top.prefHeight(-1) + bottom.prefHeight(-1) +
                Math.max(center.minHeight(-1), Math.max(left.minHeight(-1), right.minHeight(-1))),
                borderpane.minHeight(-1), 1e-100);
        assertEquals(220, borderpane.prefWidth(-1), 1e-100);
        assertEquals(242, borderpane.prefHeight(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

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
        MockBiased top = new MockBiased(Orientation.HORIZONTAL, 100, 20); // 300 x 6.666
        borderpane.setTop(top);

        MockBiased left = new MockBiased(Orientation.VERTICAL, 40, 100); // 20 x 200
        borderpane.setLeft(left);

        MockBiased center = new MockBiased(Orientation.HORIZONTAL, 200, 200);
        borderpane.setCenter(center);

        MockBiased right = new MockBiased(Orientation.VERTICAL, 60, 200); // 60 x 200
        borderpane.setRight(right);

        MockBiased bottom = new MockBiased(Orientation.HORIZONTAL, 200, 20); // 300 x 13.333
        borderpane.setBottom(bottom);

        assertEquals(40/*l*/ + 60/*r*/ + 200/*c*/, borderpane.prefWidth(-1), 1e-100);
        assertEquals(240 /* l + r + c*/, borderpane.prefHeight(-1), 1e-10);
        assertEquals(110, borderpane.minWidth(-1), 1e-100); /* min center + 2x pref width (l, r) */
        assertEquals(50, borderpane.minHeight(-1), 1e-10);
        assertEquals(110, borderpane.minWidth(240), 1e-100);
        assertEquals(221, borderpane.minHeight(300), 1e-10);

        borderpane.resize(300, 240);
        borderpane.layout();

        assertEquals(0, top.getLayoutX(), 1e-100);
        assertEquals(0, top.getLayoutY(), 1e-100);
        assertEquals(300, top.getWidth(), 1e-100);
        assertEquals(7, top.getHeight(), 1e-100);

        assertEquals(0, bottom.getLayoutX(), 1e-100);
        assertEquals(226, bottom.getLayoutY(), 1e-100);
        assertEquals(300, bottom.getWidth(), 1e-100);
        assertEquals(14, bottom.getHeight(), 1e-100);

        assertEquals(0, left.getLayoutX(), 1e-100);
        assertEquals(7, left.getLayoutY(), 1e-100);
        assertEquals(240 - 14 - 7, left.getHeight(), 1e-100);
        final double leftWidth = Math.ceil(40*100.0/(240 - 14 - 7));
        assertEquals(leftWidth, left.getWidth(), 1e-100);

        assertEquals(7, right.getLayoutY(), 1e-100);
        assertEquals(240 - 14 - 7, right.getHeight(), 1e-100);
        final double rightWidth = Math.ceil(60*200.0/(240 - 14 - 7));
        assertEquals(rightWidth, right.getWidth(), 1e-100);
        assertEquals(300 - rightWidth, right.getLayoutX(), 1e-100);

        // Center is HORIZONTALLY biased, so when width is stretched, height is lower
        double centerWidth = 300 - leftWidth - rightWidth;
        double centerHeight = Math.ceil(200 * 200 / centerWidth);

        assertEquals(19, center.getLayoutX(), 1e-100);
        // center alignment, Math.round == snapPosition
        assertEquals(Math.round(7 + (240 - 7 - 14 - centerHeight) / 2), center.getLayoutY(), 1e-100);
        assertEquals(centerWidth, center.getWidth(), 1e-100);
        assertEquals(centerHeight, center.getHeight(), 1e-100);

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
        assertEquals(200, top.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, top.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, left.getLayoutX(), 1e-200);
        assertEquals(100, left.getLayoutY(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(300, left.getLayoutBounds().getHeight(), 1e-200); // 500 - 100(top) - 100 (bottom)

        // not growing to max width, because the bias is vertical (pref width depends on height) and
        // the default alignment is Pos.CENTER
        double centerWidth = Math.round((200 * 100) /*original area*/ / 300.0) /*new height*/;
        assertEquals(100 /*x position == left width*/ + centerWidth,
                center.getLayoutX(), 1e-200);
        assertEquals(100, center.getLayoutY(), 1e-200);
        assertEquals(centerWidth, center.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(300, center.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(300, right.getLayoutX(), 1e-200);
        assertEquals(100, right.getLayoutY(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(300, right.getLayoutBounds().getHeight(), 1e-200);

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

        // The total pref height of top + (l c r) + bottom is 300
        // Shrinking this to 250 will lead to shrinking the center to 50
        // which means center pref width will grow to 200 * 100 / 50 = 400
        // (l, r) prefwidth is 100, which means 100 + 200 + 100 = 600,
        // bottom prefwidth is 400
        assertEquals(600, borderpane.prefWidth(250), 1e-200);
        borderpane.resize(600, 250);
        borderpane.layout();

        assertEquals(0, top.getLayoutX(), 1e-200);
        assertEquals(0, top.getLayoutY(), 1e-200);
        assertEquals(200, top.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, top.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, left.getLayoutX(), 1e-200);
        assertEquals(100, left.getLayoutY(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(50, left.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(100, center.getLayoutX(), 1e-200);
        assertEquals(100, center.getLayoutY(), 1e-200);
        assertEquals(400, center.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(50, center.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(500, right.getLayoutX(), 1e-200);
        assertEquals(100, right.getLayoutY(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(50, right.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, bottom.getLayoutX(), 1e-200);
        assertEquals(150, bottom.getLayoutY(), 1e-200);
        assertEquals(600, bottom.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, bottom.getLayoutBounds().getHeight(), 1e-200);


        // Now make it a little wider (center should be in the middle now)
        borderpane.resize(700, 250);
        borderpane.layout();

        assertEquals(0, top.getLayoutX(), 1e-200);
        assertEquals(0, top.getLayoutY(), 1e-200);
        assertEquals(200, top.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, top.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, left.getLayoutX(), 1e-200);
        assertEquals(100, left.getLayoutY(), 1e-200);
        assertEquals(100, left.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(50, left.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(150, center.getLayoutX(), 1e-200);
        assertEquals(100, center.getLayoutY(), 1e-200);
        assertEquals(400, center.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(50, center.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(600, right.getLayoutX(), 1e-200);
        assertEquals(100, right.getLayoutY(), 1e-200);
        assertEquals(100, right.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(50, right.getLayoutBounds().getHeight(), 1e-200);

        assertEquals(0, bottom.getLayoutX(), 1e-200);
        assertEquals(150, bottom.getLayoutY(), 1e-200);
        assertEquals(700, bottom.getLayoutBounds().getWidth(), 1e-200);
        assertEquals(100, bottom.getLayoutBounds().getHeight(), 1e-200);
    }

    @Test public void testFitsTopChildWithinBounds() {
        MockResizable child = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setTop(child);

        borderpane.resize(50,50);
        borderpane.layout();

        assertEquals(50, child.getWidth(), 1e-100);
        assertEquals(200, child.getHeight(), 1e-100); //Top always at it's pref height
    }

    @Test public void testFitsBottomChildWithinBounds() {
        MockResizable child = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setBottom(child);

        borderpane.resize(50,50);
        borderpane.layout();

        assertEquals(50, child.getWidth(), 1e-100);
        assertEquals(200, child.getHeight(), 1e-100); //Bottom always at it's pref height
    }

    @Test public void testFitsLeftChildWithinBounds() {
        MockResizable child = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setLeft(child);

        borderpane.resize(50,50);
        borderpane.layout();

        assertEquals(100, child.getWidth(), 1e-100); //Left always at it's pref width
        assertEquals(50, child.getHeight(), 1e-100);
    }

    @Test public void testFitsRightChildWithinBounds() {
        MockResizable child = new MockResizable(10,20, 100,200, 700,800);
        borderpane.setRight(child);

        borderpane.resize(50,50);
        borderpane.layout();

        assertEquals(100, child.getWidth(), 1e-100); //Right always at it's pref width
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

    @Test public void testTopChildWithMargin() {
        MockResizable top = new MockResizable(10,10,130,30,150,50);
        MockResizable center = new MockResizable(10,10,100,100,200,200);
        Insets insets = new Insets(10, 5, 20, 30);
        BorderPane.setMargin(top, insets);
        borderpane.setTop(top);
        borderpane.setCenter(center);

        assertEquals(45, borderpane.minWidth(-1), 1e-100);
        // 10 + 20 (margin) + 30 (top.prefHeight) + 10 (center.minHeight)
        // Top is always at it's pref height
        assertEquals(70, borderpane.minHeight(-1), 1e-100);
        assertEquals(165, borderpane.prefWidth(-1), 1e-100);
        assertEquals(160, borderpane.prefHeight(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

        borderpane.autosize();
        borderpane.layout();
        assertEquals(30, top.getLayoutX(), 1e-100);
        assertEquals(10, top.getLayoutY(), 1e-100);
        assertEquals(130, top.getWidth(), 1e-100);
        assertEquals(30, top.getHeight(), 1e-100);

        assertEquals(0, center.getLayoutX(), 1e-100);
        assertEquals(60, center.getLayoutY(), 1e-100);
    }

    @Test public void testBottomChildWithMargin() {
        MockResizable bottom = new MockResizable(10,10,130,30,150,50);
        MockResizable center = new MockResizable(10,10,100,100,200,200);
        Insets insets = new Insets(10, 5, 20, 30);
        BorderPane.setMargin(bottom, insets);
        borderpane.setBottom(bottom);
        borderpane.setCenter(center);

        assertEquals(45, borderpane.minWidth(-1), 1e-100);
        assertEquals(70, borderpane.minHeight(-1), 1e-100);
        assertEquals(165, borderpane.prefWidth(-1), 1e-100);
        assertEquals(160, borderpane.prefHeight(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

        borderpane.autosize();
        borderpane.layout();
        assertEquals(30, bottom.getLayoutX(), 1e-100);
        assertEquals(110, bottom.getLayoutY(), 1e-100);
        assertEquals(130, bottom.getWidth(), 1e-100);
        assertEquals(30, bottom.getHeight(), 1e-100);

        assertEquals(0, center.getLayoutX(), 1e-100);
        assertEquals(0, center.getLayoutY(), 1e-100);
    }

    @Test public void testLeftChildWithMargin() {
        MockResizable left = new MockResizable(10,10,130,130,150,150);
        MockResizable center = new MockResizable(10,10,100,100,200,200);
        Insets insets = new Insets(5, 10, 30, 20);
        BorderPane.setMargin(left, insets);
        borderpane.setLeft(left);
        borderpane.setCenter(center);

        assertEquals(45, borderpane.minHeight(-1), 1e-100);
        assertEquals(170, borderpane.minWidth(-1), 1e-100);
        assertEquals(165, borderpane.prefHeight(-1), 1e-100);
        assertEquals(260, borderpane.prefWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

        borderpane.autosize();
        borderpane.layout();
        assertEquals(20, left.getLayoutX(), 1e-100);
        assertEquals(5, left.getLayoutY(), 1e-100);
        assertEquals(130, left.getWidth(), 1e-100);
        assertEquals(130, left.getHeight(), 1e-100);

        assertEquals(160, center.getLayoutX(), 1e-100);
        assertEquals(0, center.getLayoutY(), 1e-100);
    }

    @Test public void testRightChildWithMargin() {
        MockResizable right = new MockResizable(10,10,130,130,150,150);
        MockResizable center = new MockResizable(10,10,100,100,200,200);
        Insets insets = new Insets(5, 10, 30, 20);
        BorderPane.setMargin(right, insets);
        borderpane.setRight(right);
        borderpane.setCenter(center);

        assertEquals(45, borderpane.minHeight(-1), 1e-100);
        assertEquals(170, borderpane.minWidth(-1), 1e-100);
        assertEquals(165, borderpane.prefHeight(-1), 1e-100);
        assertEquals(260, borderpane.prefWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

        borderpane.autosize();
        borderpane.layout();
        assertEquals(120, right.getLayoutX(), 1e-100);
        assertEquals(5, right.getLayoutY(), 1e-100);
        assertEquals(130, right.getWidth(), 1e-100);
        assertEquals(130, right.getHeight(), 1e-100);

        assertEquals(0, center.getLayoutX(), 1e-100);
        assertEquals(0, center.getLayoutY(), 1e-100);
    }

    @Test public void testCenterChildWithMargin() {
        MockResizable center = new MockResizable(10,10,100,100,200,200);
        Insets insets = new Insets(5, 10, 30, 20);
        BorderPane.setMargin(center, insets);
        borderpane.setCenter(center);

        assertEquals(45, borderpane.minHeight(-1), 1e-100);
        assertEquals(40, borderpane.minWidth(-1), 1e-100);
        assertEquals(135, borderpane.prefHeight(-1), 1e-100);
        assertEquals(130, borderpane.prefWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxWidth(-1), 1e-100);
        assertEquals(Double.MAX_VALUE, borderpane.maxHeight(-1), 1e-100);

        borderpane.autosize();
        borderpane.layout();

        assertEquals(20, center.getLayoutX(), 1e-100);
        assertEquals(5, center.getLayoutY(), 1e-100);
    }

    @Test
    public void testResizeBelowMinimum() {
        MockResizable left = new MockResizable(10,10,100,100,150,150);
        MockResizable center = new MockResizable(30,30,100,100,200,200);

        borderpane.setCenter(center);
        borderpane.setLeft(left);

        borderpane.resize(30, 30);

        borderpane.layout();

        assertEquals(100, left.getWidth(), 1e-100); // Always at pref.
        assertEquals(30, left.getHeight(), 1e-100);

        assertEquals(100, center.getLayoutX(), 1e-100);
        assertEquals(0, center.getLayoutY(), 1e-100);
        assertEquals(30, center.getWidth(), 1e-100);
        assertEquals(30, center.getHeight(), 1e-100);
    }

}

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
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.shape.Rectangle;

import org.junit.Test;


public class AnchorPaneTest {

    @Test public void testNoAnchorsSet() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        child.relocate(10, 20); // should honor position if no anchors set
        anchorpane.getChildren().add(child);

        assertEquals(110, anchorpane.minWidth(-1), 1e-100);
        assertEquals(220, anchorpane.minHeight(-1), 1e-100);
        assertEquals(310, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(420, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testTopAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setTopAnchor(child, 10.0);
        anchorpane.getChildren().add(child);

        assertEquals(100, anchorpane.minWidth(-1), 1e-100);
        assertEquals(210, anchorpane.minHeight(-1), 1e-100);
        assertEquals(300, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(410, anchorpane.prefHeight(-1), 1e-100);
        
        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testLeftAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setLeftAnchor(child, 10.0);
        anchorpane.getChildren().add(child);

        assertEquals(110, anchorpane.minWidth(-1), 1e-100);
        assertEquals(200, anchorpane.minHeight(-1), 1e-100);
        assertEquals(310, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(400, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(0, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(0, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testBottomAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setBottomAnchor(child, 10.0);
        anchorpane.getChildren().add(child);

        assertEquals(100, anchorpane.minWidth(-1), 1e-100);
        assertEquals(210, anchorpane.minHeight(-1), 1e-100);
        assertEquals(300, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(410, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(0, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(90, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testRightAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setRightAnchor(child, 10.0);
        anchorpane.getChildren().add(child);

        assertEquals(110, anchorpane.minWidth(-1), 1e-100);
        assertEquals(200, anchorpane.minHeight(-1), 1e-100);
        assertEquals(310, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(400, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(0, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(190, child.getLayoutX(), 1e-100);
        assertEquals(0, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testTopLeftAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setTopAnchor(child,20.0);
        anchorpane.setLeftAnchor(child, 10.0);
        anchorpane.getChildren().add(child);

        assertEquals(110, anchorpane.minWidth(-1), 1e-100);
        assertEquals(220, anchorpane.minHeight(-1), 1e-100);
        assertEquals(310, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(420, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testTopBottomAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setTopAnchor(child,20.0);
        anchorpane.setBottomAnchor(child, 10.0);
        anchorpane.getChildren().add(child);

        assertEquals(100, anchorpane.minWidth(-1), 1e-100);
        assertEquals(230, anchorpane.minHeight(-1), 1e-100);
        assertEquals(300, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(430, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(470, child.getHeight(), 1e-100);
    }

    @Test public void testTopRightAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setTopAnchor(child,20.0);
        anchorpane.setRightAnchor(child, 10.0);
        anchorpane.getChildren().add(child);

        assertEquals(110, anchorpane.minWidth(-1), 1e-100);
        assertEquals(220, anchorpane.minHeight(-1), 1e-100);
        assertEquals(310, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(420, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(190, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testLeftBottomAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setBottomAnchor(child,20.0);
        anchorpane.setLeftAnchor(child, 10.0);
        anchorpane.getChildren().add(child);

        assertEquals(110, anchorpane.minWidth(-1), 1e-100);
        assertEquals(220, anchorpane.minHeight(-1), 1e-100);
        assertEquals(310, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(420, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(0, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(80, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testLeftRightAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setRightAnchor(child,20.0);
        anchorpane.setLeftAnchor(child, 10.0);
        anchorpane.getChildren().add(child);

        assertEquals(130, anchorpane.minWidth(-1), 1e-100);
        assertEquals(200, anchorpane.minHeight(-1), 1e-100);
        assertEquals(330, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(400, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(0, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(0, child.getLayoutY(), 1e-100);
        assertEquals(470, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testLeftTopRightAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setRightAnchor(child,20.0);
        anchorpane.setLeftAnchor(child, 10.0);
        anchorpane.setTopAnchor(child, 30.0);
        anchorpane.getChildren().add(child);

        assertEquals(130, anchorpane.minWidth(-1), 1e-100);
        assertEquals(230, anchorpane.minHeight(-1), 1e-100);
        assertEquals(330, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(430, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
        assertEquals(470, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testLeftBottomRightAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setRightAnchor(child,20.0);
        anchorpane.setLeftAnchor(child, 10.0);
        anchorpane.setBottomAnchor(child, 30.0);
        anchorpane.getChildren().add(child);

        assertEquals(130, anchorpane.minWidth(-1), 1e-100);
        assertEquals(230, anchorpane.minHeight(-1), 1e-100);
        assertEquals(330, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(430, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(0, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(10, child.getLayoutX(), 1e-100);
        assertEquals(70, child.getLayoutY(), 1e-100);
        assertEquals(470, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testTopLeftBottomAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setTopAnchor(child,20.0);
        anchorpane.setBottomAnchor(child, 10.0);
        anchorpane.setLeftAnchor(child, 30.0);
        anchorpane.getChildren().add(child);

        assertEquals(130, anchorpane.minWidth(-1), 1e-100);
        assertEquals(230, anchorpane.minHeight(-1), 1e-100);
        assertEquals(330, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(430, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(30, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(30, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(470, child.getHeight(), 1e-100);
    }

    @Test public void testTopRightBottomAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setTopAnchor(child,20.0);
        anchorpane.setBottomAnchor(child, 10.0);
        anchorpane.setRightAnchor(child, 30.0);
        anchorpane.getChildren().add(child);

        assertEquals(130, anchorpane.minWidth(-1), 1e-100);
        assertEquals(230, anchorpane.minHeight(-1), 1e-100);
        assertEquals(330, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(430, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(170, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(470, child.getHeight(), 1e-100);
    }

    @Test public void testAllSidesAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setTopAnchor(child,20.0);
        anchorpane.setBottomAnchor(child, 10.0);
        anchorpane.setRightAnchor(child, 30.0);
        anchorpane.setLeftAnchor(child, 40.0);
        anchorpane.getChildren().add(child);

        assertEquals(170, anchorpane.minWidth(-1), 1e-100);
        assertEquals(230, anchorpane.minHeight(-1), 1e-100);
        assertEquals(370, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(430, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(40, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(40, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(430, child.getWidth(), 1e-100);
        assertEquals(470, child.getHeight(), 1e-100);
    }

    @Test public void testAllSidesAnchoredWithPadding() {
        AnchorPane anchorpane = new AnchorPane();
        anchorpane.setPadding(new Insets(10,20,30,40));
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        anchorpane.setTopAnchor(child,20.0);
        anchorpane.setBottomAnchor(child, 10.0);
        anchorpane.setRightAnchor(child, 30.0);
        anchorpane.setLeftAnchor(child, 40.0);
        anchorpane.getChildren().add(child);

        assertEquals(230, anchorpane.minWidth(-1), 1e-100);
        assertEquals(270, anchorpane.minHeight(-1), 1e-100);
        assertEquals(430, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(470, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(80, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        assertEquals(80, child.getLayoutX(), 1e-100);
        assertEquals(30, child.getLayoutY(), 1e-100);
        assertEquals(370, child.getWidth(), 1e-100);
        assertEquals(430, child.getHeight(), 1e-100);
    }

    @Test public void testNonresizableAllSidesAnchored() {
        AnchorPane anchorpane = new AnchorPane();
        Rectangle child = new Rectangle(300,400);
        anchorpane.setTopAnchor(child,20.0);
        anchorpane.setBottomAnchor(child, 10.0);
        anchorpane.setRightAnchor(child, 30.0);
        anchorpane.setLeftAnchor(child, 40.0);
        anchorpane.getChildren().add(child);

        assertEquals(370, anchorpane.minWidth(-1), 1e-100);
        assertEquals(430, anchorpane.minHeight(-1), 1e-100);
        assertEquals(370, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(430, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(40, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);

        anchorpane.resize(500,500);
        anchorpane.layout();
        // ends up being anchored just at the top-left (bottom-right anchors ignored)
        assertEquals(40, child.getLayoutX(), 1e-100);
        assertEquals(20, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testAnchorPaneWithHorizontalBiasedChild() {
        AnchorPane anchorpane = new AnchorPane();

        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100,100);
        Rectangle rect = new Rectangle(200,200);

        AnchorPane.setTopAnchor(biased, 10.0);
        AnchorPane.setLeftAnchor(biased, 10.0);
        AnchorPane.setRightAnchor(biased, 10.0);

        AnchorPane.setTopAnchor(rect, 10.0);
        AnchorPane.setLeftAnchor(rect, 10.0);
        AnchorPane.setBottomAnchor(rect, 10.0);
        AnchorPane.setRightAnchor(rect, 10.0);

        anchorpane.getChildren().addAll(biased, rect);

        assertEquals(220, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(220, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(10.0, biased.getLayoutX(), 1e-100);
        assertEquals(10.0, biased.getLayoutY(), 1e-100);
        assertEquals(200, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(50, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(10.0, rect.getLayoutX(), 1e-100);
        assertEquals(10.0, rect.getLayoutY(), 1e-100);
        assertEquals(200, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, rect.getLayoutBounds().getHeight(), 1e-100);

        anchorpane.resize(420, 420);
        anchorpane.layout();
        assertEquals(10.0, biased.getLayoutX(), 1e-100);
        assertEquals(10.0, biased.getLayoutY(), 1e-100);
        assertEquals(400, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(25, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(10, rect.getLayoutX(), 1e-100);
        assertEquals(10, rect.getLayoutY(), 1e-100);
        assertEquals(200, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, rect.getLayoutBounds().getHeight(), 1e-100);


    }

    @Test public void testAnchorPaneWithVerticalBiasedChild() {
        AnchorPane anchorpane = new AnchorPane();

        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100,100);
        Rectangle rect = new Rectangle(200,200);

        AnchorPane.setTopAnchor(biased, 10.0);
        AnchorPane.setLeftAnchor(biased, 10.0);
        AnchorPane.setBottomAnchor(biased, 10.0);

        AnchorPane.setTopAnchor(rect, 10.0);
        AnchorPane.setLeftAnchor(rect, 10.0);
        AnchorPane.setBottomAnchor(rect, 10.0);
        AnchorPane.setRightAnchor(rect, 10.0);

        anchorpane.getChildren().addAll(biased, rect);

        assertEquals(220, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(220, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(10, biased.getLayoutX(), 1e-100);
        assertEquals(10, biased.getLayoutY(), 1e-100);
        assertEquals(50, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(10, rect.getLayoutX(), 1e-100);
        assertEquals(10, rect.getLayoutY(), 1e-100);
        assertEquals(200, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, rect.getLayoutBounds().getHeight(), 1e-100);

        anchorpane.resize(420, 420);
        anchorpane.layout();
        assertEquals(10, biased.getLayoutX(), 1e-100);
        assertEquals(10, biased.getLayoutY(), 1e-100);
        assertEquals(25, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(10, rect.getLayoutX(), 1e-100);
        assertEquals(10, rect.getLayoutY(), 1e-100);
        assertEquals(200, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, rect.getLayoutBounds().getHeight(), 1e-100);


    }

    @Test public void testAnchorPaneWithChildPrefSizeLessThanMinSize() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable resizable = new MockResizable(30, 30, 20, 20, Double.MAX_VALUE, Double.MAX_VALUE);        
        anchorpane.getChildren().add(resizable);

        anchorpane.autosize();
        anchorpane.layout();
        
        assertEquals(0, resizable.getLayoutX(), 1e-100);
        assertEquals(0, resizable.getLayoutY(), 1e-100);
        assertEquals(30, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(30, resizable.getLayoutBounds().getHeight(), 1e-100);
    }
    
    @Test public void testAnchorPanePrefHeightWithHorizontalBiasedChild_RT21745() {
        AnchorPane anchorpane = new AnchorPane();
        
        AnchorPane internalAnchorpane = new AnchorPane();        

        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 30, 256);
                        
        internalAnchorpane.getChildren().add(biased);
        anchorpane.getChildren().add(internalAnchorpane);
      
        anchorpane.resize(500, 500);
        anchorpane.layout();        
        
        assertEquals(30, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(256, anchorpane.prefHeight(-1), 1e-100);
        assertEquals(30, internalAnchorpane.prefWidth(-1), 1e-100);
        assertEquals(256, internalAnchorpane.prefHeight(-1), 1e-100);        
    }    
}

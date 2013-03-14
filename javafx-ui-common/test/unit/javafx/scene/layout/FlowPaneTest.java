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
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import org.junit.Before;

import org.junit.Test;


public class FlowPaneTest {
    FlowPane flowpane;

    @Before public void setUp() {
        this.flowpane = new FlowPane();
    }

    @Test public void testFlowPaneDefaults() {
        assertEquals(Orientation.HORIZONTAL, flowpane.getOrientation());
        assertEquals(0, flowpane.getHgap(), 1e-100);
        assertEquals(0, flowpane.getVgap(), 1e-100);
        assertEquals(Pos.TOP_LEFT, flowpane.getAlignment());
        assertEquals(VPos.CENTER, flowpane.getRowValignment());
        assertEquals(HPos.LEFT, flowpane.getColumnHalignment());
        assertEquals(400, flowpane.getPrefWrapLength(), 1e-100);
    }

    @Test public void testFlowPaneNulls() {
        flowpane.setAlignment(null);
        flowpane.setColumnHalignment(null);
        flowpane.setRowValignment(null);
        flowpane.setOrientation(null);

        // this musn't throw NPE
        flowpane.autosize();
        flowpane.layout();

        assertNull(flowpane.getOrientation());
        assertNull(flowpane.getAlignment());
        assertNull(flowpane.getRowValignment());
        assertNull(flowpane.getColumnHalignment());
        assertNull(flowpane.orientationProperty().get());
        assertNull(flowpane.alignmentProperty().get());
        assertNull(flowpane.rowValignmentProperty().get());
        assertNull(flowpane.columnHalignmentProperty().get());
    }

    @Test public void testSimpleFlowPane() {
        for(int i = 0; i < 3; i++) { // 6 children
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }

        assertEquals(100, flowpane.minWidth(-1), 1e-100);
        assertEquals(900, flowpane.minHeight(100), 1e-100);
        assertEquals(400, flowpane.prefWidth(-1), 1e-100);
        assertEquals(400, flowpane.prefHeight(-1), 1e-100);

        flowpane.autosize();
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, last.getLayoutX(), 1e-100);
        assertEquals(250, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);

        flowpane.resize(800,800);
        flowpane.layout();
        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(500, last.getLayoutX(), 1e-100);
        assertEquals(50, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testEmptyHorizontalFlowPaneMinWidthIsZero() {
        FlowPane flowpane = new FlowPane();

        assertEquals(0, flowpane.minWidth(-1), 0);
    }

    @Test public void testEmptyHorizontalFlowPaneMinHeightIsZero() {
        FlowPane flowpane = new FlowPane();

        assertEquals(0, flowpane.minHeight(-1), 0);
    }

    @Test public void testEmptyVerticalFlowPaneMinWidthIsZero() {
        FlowPane flowpane = new FlowPane(Orientation.VERTICAL);

        assertEquals(0, flowpane.minWidth(-1), 0);
    }

    @Test public void testEmptyVerticalFlowPaneMinHeightIsZero() {
        FlowPane flowpane = new FlowPane(Orientation.VERTICAL);

        assertEquals(0, flowpane.minHeight(-1), 0);
    }

    @Test public void testHorizontalFlowPaneAlignmentTopLeft() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_LEFT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, last.getLayoutX(), 1e-100);
        assertEquals(250, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneAlignmentTopCenter() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_CENTER);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(25, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(225, last.getLayoutX(), 1e-100);
        assertEquals(250, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneAlignmentTopRight() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_RIGHT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(350, last.getLayoutX(), 1e-100);
        assertEquals(250, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneAlignmentCenterLeft() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER_LEFT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(25, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, last.getLayoutX(), 1e-100);
        assertEquals(275, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneAlignmentCenter() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(25, first.getLayoutX(), 1e-100);
        assertEquals(25, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(225, last.getLayoutX(), 1e-100);
        assertEquals(275, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneAlignmentCenterRight() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER_RIGHT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(25, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(350, last.getLayoutX(), 1e-100);
        assertEquals(275, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneAlignmentBottomLeft() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_LEFT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(50, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, last.getLayoutX(), 1e-100);
        assertEquals(300, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneAlignmentBottomCenter() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_CENTER);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(25, first.getLayoutX(), 1e-100);
        assertEquals(50, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(225, last.getLayoutX(), 1e-100);
        assertEquals(300, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneAlignmentBottomRight() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_RIGHT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(50, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(350, last.getLayoutX(), 1e-100);
        assertEquals(300, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneAlignmentTopLeft() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_LEFT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, last.getLayoutX(), 1e-100);
        assertEquals(300, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneAlignmentTopCenter() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_CENTER);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(450, last.getLayoutX(), 1e-100);
        assertEquals(300, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneAlignmentTopRight() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_RIGHT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(100, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(500, last.getLayoutX(), 1e-100);
        assertEquals(300, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneAlignmentCenterLeft() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER_LEFT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(100, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, last.getLayoutX(), 1e-100);
        assertEquals(400, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneAlignmentCenter() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(100, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(450, last.getLayoutX(), 1e-100);
        assertEquals(400, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneAlignmentCenterRight() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER_RIGHT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(100, first.getLayoutX(), 1e-100);
        assertEquals(100, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(500, last.getLayoutX(), 1e-100);
        assertEquals(400, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneAlignmentBottomLeft() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_LEFT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(200, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, last.getLayoutX(), 1e-100);
        assertEquals(500, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneAlignmentBottomCenter() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_CENTER);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(50, first.getLayoutX(), 1e-100);
        assertEquals(200, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(450, last.getLayoutX(), 1e-100);
        assertEquals(500, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneAlignmentBottomRight() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_RIGHT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(100, first.getLayoutX(), 1e-100);
        assertEquals(200, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(500, last.getLayoutX(), 1e-100);
        assertEquals(500, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneRowVAlignmentTop() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setRowValignment(VPos.TOP);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, last.getLayoutX(), 1e-100);
        assertEquals(200, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneRowVAlignmentCenter() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setRowValignment(VPos.CENTER);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, last.getLayoutX(), 1e-100);
        assertEquals(250, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneRowVAlignmentBaseline() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200); //baseline=190
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setRowValignment(VPos.BASELINE);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, last.getLayoutX(), 1e-100);
        assertEquals(290, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHorizontalFlowPaneRowVAlignmentBottom() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setRowValignment(VPos.BOTTOM);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, last.getLayoutX(), 1e-100);
        assertEquals(300, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneRowHAlignmentLeft() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setColumnHalignment(HPos.LEFT);

        flowpane.resize(600,800);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, last.getLayoutX(), 1e-100);
        assertEquals(300, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneRowHAlignmentCenter() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setColumnHalignment(HPos.CENTER);

        flowpane.resize(600,800);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(250, last.getLayoutX(), 1e-100);
        assertEquals(300, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneRowHAlignmentRight() {
        flowpane.setOrientation(Orientation.VERTICAL);

        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(200,300);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }
        flowpane.setColumnHalignment(HPos.RIGHT);

        flowpane.resize(600,800);
        flowpane.layout();

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        assertEquals(0, first.getLayoutX(), 1e-100);
        assertEquals(0, first.getLayoutY(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, last.getLayoutX(), 1e-100);
        assertEquals(300, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }
    
    @Test public void testFlowPaneSetMarginConstraint() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);

        assertNull(FlowPane.getMargin(child1));
        
        Insets margin = new Insets(10,20,30,40);
        FlowPane.setMargin(child1, margin);
        assertEquals(margin, FlowPane.getMargin(child1));

        FlowPane.setMargin(child1, null);
        assertNull(FlowPane.getMargin(child1));
    }

    @Test public void testFlowPaneMarginConstraint() {
        for(int i = 0; i < 3; i++) {
            MockResizable child1 = new MockResizable(100,200);
            Rectangle child2 = new Rectangle(100, 100);
            flowpane.getChildren().addAll(child1, child2);
        }

        // test a handful
        Node first = flowpane.getChildren().get(0);
        Node last = flowpane.getChildren().get(5);

        FlowPane.setMargin(first, new Insets(10,20,30,40));

        assertEquals(100, flowpane.minWidth(-1), 1e-100);
        assertEquals(940, flowpane.minHeight(100), 1e-100);
        assertEquals(400, flowpane.prefWidth(-1), 1e-100);
        assertEquals(440, flowpane.prefHeight(-1), 1e-100);

        flowpane.autosize();
        flowpane.layout();

        assertEquals(40, first.getLayoutX(), 1e-100);
        assertEquals(10, first.getLayoutY(), 1e-100);
        assertEquals(100, first.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, first.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, last.getLayoutX(), 1e-100);
        assertEquals(290, last.getLayoutY(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, last.getLayoutBounds().getHeight(), 1e-100);
    }

    // FlowPane does not shrink their children to be smaller than their preferred sizes 
    @Test public void testHorizontalFlowPaneFitsChildWithinHeightIfPossible() {
        MockResizable child = new MockResizable(10,20, 200,200, 500,500);

        flowpane.getChildren().add(child);

        flowpane.resize(100,100);
        flowpane.layout();

        assertEquals(200, child.getWidth(), 1e-100);
        assertEquals(200, child.getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneFitsChildWithinWidthIfPossible() {
        flowpane.setOrientation(Orientation.VERTICAL);
        MockResizable child = new MockResizable(10,20, 200,200, 500,500);

        flowpane.getChildren().add(child);

        flowpane.resize(100,100);
        System.out.println("******************");
        flowpane.layout();
        System.out.println("*****************");

        assertEquals(200, child.getWidth(), 1e-100);
        assertEquals(200, child.getHeight(), 1e-100);
    }
}

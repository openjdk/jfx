/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.util.stream.Stream;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.ParentShim;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class FlowPaneTest {

    private static final double EPSILON = 0.000001;

    FlowPane flowpane;
    Stage stage;

    @BeforeEach public void setUp() {
        this.flowpane = new FlowPane();
    }

    @AfterEach public void tearDown() {
        if (stage != null) {
            stage.hide();
        }
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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }

        assertEquals(100, flowpane.minWidth(-1), 1e-100);
        assertEquals(900, flowpane.minHeight(100), 1e-100);
        assertEquals(400, flowpane.prefWidth(-1), 1e-100);
        assertEquals(400, flowpane.prefHeight(-1), 1e-100);

        flowpane.autosize();
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_LEFT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_CENTER);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_RIGHT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER_LEFT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER_RIGHT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_LEFT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_CENTER);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_RIGHT);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_LEFT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_CENTER);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.TOP_RIGHT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER_LEFT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.CENTER_RIGHT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_LEFT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_CENTER);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setAlignment(Pos.BOTTOM_RIGHT);

        flowpane.resize(700,600);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setRowValignment(VPos.TOP);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setRowValignment(VPos.CENTER);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setRowValignment(VPos.BASELINE);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setRowValignment(VPos.BOTTOM);

        flowpane.resize(450,450);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setColumnHalignment(HPos.LEFT);

        flowpane.resize(600,800);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setColumnHalignment(HPos.CENTER);

        flowpane.resize(600,800);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }
        flowpane.setColumnHalignment(HPos.RIGHT);

        flowpane.resize(600,800);
        flowpane.layout();

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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
            ParentShim.getChildren(flowpane).addAll(child1, child2);
        }

        // test a handful
        Node first = ParentShim.getChildren(flowpane).get(0);
        Node last = ParentShim.getChildren(flowpane).get(5);

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

        ParentShim.getChildren(flowpane).add(child);

        flowpane.resize(100,100);
        flowpane.layout();

        assertEquals(200, child.getWidth(), 1e-100);
        assertEquals(200, child.getHeight(), 1e-100);
    }

    @Test public void testVerticalFlowPaneFitsChildWithinWidthIfPossible() {
        flowpane.setOrientation(Orientation.VERTICAL);
        MockResizable child = new MockResizable(10,20, 200,200, 500,500);

        ParentShim.getChildren(flowpane).add(child);

        flowpane.resize(100,100);
        flowpane.layout();

        assertEquals(200, child.getWidth(), 1e-100);
        assertEquals(200, child.getHeight(), 1e-100);
    }

    @Test
    void shouldFlowBiasedControlsCorrectly() {
        MockBiased c1 = new MockBiased(Orientation.HORIZONTAL, 100, 100);
        MockBiased c2 = new MockBiased(Orientation.HORIZONTAL, 100, 100);
        MockBiased c3 = new MockBiased(Orientation.HORIZONTAL, 100, 100);

        flowpane.getChildren().setAll(c1, c2, c3);

        flowpane.resize(100, 300);
        flowpane.layout();

        LayoutAssertions.assertBounds(
            """
            +------------+
            | 1: 100x100 |
            +------------+
            | 2: 100x100 |
            +------------+
            | 3: 100x100 |
            +------------+
            """,
            flowpane
        );

        flowpane.resize(200, 200);
        flowpane.layout();

        LayoutAssertions.assertBounds(
            """
            +------------+------------+
            | 1: 100x100 | 2: 100x100 |
            +------------+------------+
            | 3: 100x100 |
            +------------+
            """,
            flowpane
        );

        flowpane.resize(300, 100);
        flowpane.layout();

        LayoutAssertions.assertBounds(
            """
            +------------+------------+------------+
            | 1: 100x100 | 2: 100x100 | 3: 100x100 |
            +------------+------------+------------+
            """,
            flowpane
        );

        flowpane.resize(300, 200);
        flowpane.layout();

        LayoutAssertions.assertBounds(
            """
            +------------+------------+------------+
            | 1: 100x100 | 2: 100x100 | 3: 100x100 |
            +------------+------------+------------+
            |                0x100                 |
            +--------------------------------------+
            """,
            flowpane
        );

        FlowPane.setMargin(c2, new Insets(1, 2, 3, 4));
        flowpane.resize(306, 204);
        flowpane.layout();

        LayoutAssertions.assertBounds(
            """
            +------------+--------------------+------------+
            |     0x2    |          1         |     0x2    |
            +------------+   +------------+   +------------+
            | 1: 100x100 | 4 | 2: 100x100 | 2 | 3: 100x100 |
            +------------+   +------------+   +------------+
            |     0x2    |          3         |     0x2    |
            +------------+--------------------+------------+
            |                     0x100                    |
            +----------------------------------------------+
            """,
            flowpane
        );
    }

    @Test
    void shouldFlowBiasedControlsCorrectlyWithMaxWidthTextFlows() {
        TextFlow c1 = createTextFlow();
        TextFlow c2 = createTextFlow();
        TextFlow c3 = createTextFlow();

        flowpane.getChildren().setAll(c1, c2, c3);

        flowpane.resize(204, 288);
        flowpane.layout();

        LayoutAssertions.assertBounds(
            """
            +-----------+
            | 1: 204x96 |
            +-----------+
            | 2: 204x96 |
            +-----------+
            | 3: 204x96 |
            +-----------+
            """,
            flowpane
        );

        flowpane.resize(404, 192);
        flowpane.layout();

        LayoutAssertions.assertBounds(
            """
            +-----------+------+-----------+
            | 1: 204x96 | -4x0 | 2: 204x96 |
            +-----------+------+-----------+
            | 3: 204x96 |
            +-----------+
            """,
            flowpane
        );

        flowpane.resize(604, 96);
        flowpane.layout();

        LayoutAssertions.assertBounds(
            """
            +-----------+------+-----------+------+-----------+
            | 1: 204x96 | -4x0 | 2: 204x96 | -4x0 | 3: 204x96 |
            +-----------+------+-----------+------+-----------+
            """,
            flowpane
        );
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    void shouldSnapInsetsBeforeMeasuringAndWrapping(double scaleX, double scaleY) {
        double pixelX = 1 / scaleX;
        double pixelY = 1 / scaleY;
        var subpixelInsets = new Insets(0.4 / scaleY, 0.4 / scaleX, 0.4 / scaleY, 0.4 / scaleX);

        var h1 = new MockResizable(pixelX, pixelY);
        var h2 = new MockResizable(pixelX, pixelY);
        var horizontal = new FlowPane(h1, h2);
        horizontal.setPadding(subpixelInsets);
        horizontal.setPrefWrapLength(pixelX);

        var v1 = new MockResizable(pixelX, pixelY);
        var v2 = new MockResizable(pixelX, pixelY);
        var vertical = new FlowPane(Orientation.VERTICAL, v1, v2);
        vertical.setPadding(subpixelInsets);
        vertical.setPrefWrapLength(pixelY);

        attachToStage(scaleX, scaleY, horizontal, vertical);

        assertEquals(pixelX, horizontal.minWidth(-1), EPSILON);
        assertEquals(pixelX, horizontal.prefWidth(-1), EPSILON);
        assertEquals(pixelY, vertical.minHeight(-1), EPSILON);
        assertEquals(pixelY, vertical.prefHeight(-1), EPSILON);

        horizontal.resize(2 * pixelX, 2 * pixelY);
        horizontal.layout();
        assertEquals(pixelX, h2.getLayoutX(), EPSILON);
        assertEquals(0, h2.getLayoutY(), EPSILON);

        vertical.resize(2 * pixelX, 2 * pixelY);
        vertical.layout();
        assertEquals(0, v2.getLayoutX(), EPSILON);
        assertEquals(pixelY, v2.getLayoutY(), EPSILON);
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    void shouldUseSnappedWrapLengthForMeasurementAndLayout(double scaleX, double scaleY) {
        double pixelX = 1 / scaleX;
        double pixelY = 1 / scaleY;

        var h1 = new MockResizable(pixelX, pixelY);
        var h2 = new MockResizable(pixelX, pixelY);
        var horizontal = new FlowPane(h1, h2);
        horizontal.setPrefWrapLength(1.6 / scaleX);

        var v1 = new MockResizable(pixelX, pixelY);
        var v2 = new MockResizable(pixelX, pixelY);
        var vertical = new FlowPane(Orientation.VERTICAL, v1, v2);
        vertical.setPrefWrapLength(1.6 / scaleY);

        attachToStage(scaleX, scaleY, horizontal, vertical);

        assertEquals(2 * pixelX, horizontal.prefWidth(-1), EPSILON);
        assertEquals(pixelY, horizontal.prefHeight(-1), EPSILON);
        horizontal.resize(1.6 / scaleX, pixelY);
        horizontal.layout();
        assertEquals(0, h2.getLayoutY(), EPSILON);

        assertEquals(pixelX, vertical.prefWidth(-1), EPSILON);
        assertEquals(2 * pixelY, vertical.prefHeight(-1), EPSILON);
        vertical.resize(pixelX, 1.6 / scaleY);
        vertical.layout();
        assertEquals(0, v2.getLayoutX(), EPSILON);
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    void shouldResnapCumulativeRunLengths(double scaleX, double scaleY) {
        int childCount = 34;
        double pixelX = 1 / scaleX;
        double pixelY = 1 / scaleY;
        var horizontal = new FlowPane();
        var vertical = new FlowPane(Orientation.VERTICAL);

        for (int i = 0; i < childCount; i++) {
            horizontal.getChildren().add(new MockResizable(pixelX, pixelY));
            vertical.getChildren().add(new MockResizable(pixelX, pixelY));
        }

        horizontal.setPrefWrapLength(childCount * pixelX);
        vertical.setPrefWrapLength(childCount * pixelY);
        attachToStage(scaleX, scaleY, horizontal, vertical);

        assertEquals(pixelY, horizontal.prefHeight(-1), EPSILON);
        horizontal.resize(childCount * pixelX, pixelY);
        horizontal.layout();
        assertEquals(0, horizontal.getChildren().get(childCount - 1).getLayoutY(), EPSILON);

        assertEquals(pixelX, vertical.prefWidth(-1), EPSILON);
        vertical.resize(pixelX, childCount * pixelY);
        vertical.layout();
        assertEquals(0, vertical.getChildren().get(childCount - 1).getLayoutX(), EPSILON);
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    void shouldResnapChildAreaAfterAddingMargins(double scaleX, double scaleY) {
        double contentWidth = 1 / scaleX;
        double contentHeight = 1 / scaleY;
        double marginX = 4 / scaleX;
        double marginY = 4 / scaleY;
        var child = new MockResizable(contentWidth, contentHeight);
        var pane = new FlowPane(child);
        pane.setPrefWrapLength(0);
        FlowPane.setMargin(child, new Insets(0, marginX, marginY, 0));

        attachToStage(scaleX, scaleY, pane);

        double expectedWidth = 5 / scaleX;
        double expectedHeight = 5 / scaleY;
        assertEquals(expectedWidth, pane.prefWidth(-1), EPSILON);
        assertEquals(expectedHeight, pane.prefHeight(-1), EPSILON);

        pane.resize(expectedWidth, expectedHeight);
        pane.layout();
        assertEquals(0, child.getLayoutX(), EPSILON);
        assertEquals(0, child.getLayoutY(), EPSILON);
        assertEquals(contentWidth, child.getWidth(), EPSILON);
        assertEquals(contentHeight, child.getHeight(), EPSILON);
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    void shouldMeasureBiasedChildrenUsingSnappedDependentDimension(double scaleX, double scaleY) {
        AreaBiasedRegion horizontalChild = new AreaBiasedRegion(Orientation.HORIZONTAL, 1.2 / scaleX, 10 / scaleY);
        FlowPane horizontalPane = new FlowPane(horizontalChild);
        horizontalPane.setPrefWrapLength(0);

        AreaBiasedRegion verticalChild = new AreaBiasedRegion(Orientation.VERTICAL, 10 / scaleX, 1.2 / scaleY);
        FlowPane verticalPane = new FlowPane(verticalChild);
        verticalPane.setPrefWrapLength(0);

        attachToStage(scaleX, scaleY, horizontalPane, verticalPane);

        double horizontalWidth = horizontalPane.snapSizeX(horizontalChild.prefWidth(-1));
        double horizontalHeight = horizontalPane.snapSizeY(horizontalChild.prefHeight(horizontalWidth));
        assertEquals(horizontalWidth, horizontalPane.prefWidth(-1), EPSILON);
        assertEquals(horizontalHeight, horizontalPane.prefHeight(-1), EPSILON);
        horizontalPane.resize(horizontalWidth, horizontalHeight);
        horizontalPane.layout();
        assertEquals(horizontalWidth, horizontalChild.getWidth(), EPSILON);
        assertEquals(horizontalHeight, horizontalChild.getHeight(), EPSILON);

        double verticalHeight = verticalPane.snapSizeY(verticalChild.prefHeight(-1));
        double verticalWidth = verticalPane.snapSizeX(verticalChild.prefWidth(verticalHeight));
        assertEquals(verticalWidth, verticalPane.prefWidth(-1), EPSILON);
        assertEquals(verticalHeight, verticalPane.prefHeight(-1), EPSILON);
        verticalPane.resize(verticalWidth, verticalHeight);
        verticalPane.layout();
        assertEquals(verticalWidth, verticalChild.getWidth(), EPSILON);
        assertEquals(verticalHeight, verticalChild.getHeight(), EPSILON);
    }

    @ParameterizedTest
    @MethodSource("renderScales")
    void shouldInvalidateRunCacheWhenSnapPolicyChanges(double scaleX, double scaleY) {
        double rawWidth = 0.4 / scaleX;
        double rawHeight = 0.4 / scaleY;
        MockResizable verticalChild = new MockResizable(rawWidth, 10 / scaleY);
        FlowPane vertical = new FlowPane(Orientation.VERTICAL, verticalChild);
        vertical.setPrefWrapLength(10 / scaleY);
        MockResizable horizontalChild = new MockResizable(10 / scaleX, rawHeight);
        FlowPane horizontal = new FlowPane(horizontalChild);
        horizontal.setPrefWrapLength(10 / scaleX);

        attachToStage(scaleX, scaleY, vertical, horizontal);

        double availableHeight = 10 / scaleY;
        double availableWidth = 10 / scaleX;
        assertNotEquals(rawWidth, vertical.prefWidth(availableHeight), EPSILON);
        assertNotEquals(rawHeight, horizontal.prefHeight(availableWidth), EPSILON);

        vertical.setSnapToPixel(false);
        horizontal.setSnapToPixel(false);
        assertEquals(rawWidth, vertical.prefWidth(availableHeight), EPSILON);
        assertEquals(rawHeight, horizontal.prefHeight(availableWidth), EPSILON);
    }

    @ParameterizedTest
    @MethodSource("renderScaleChanges")
    void shouldInvalidateRunCacheWhenRenderScalesChange(double initialScaleX, double initialScaleY,
                                                        double newScaleX, double newScaleY) {
        double rawSize = 0.41;
        MockResizable verticalChild = new MockResizable(rawSize, 100);
        FlowPane vertical = new FlowPane(Orientation.VERTICAL, verticalChild);
        vertical.setPrefWrapLength(100);
        MockResizable horizontalChild = new MockResizable(100, rawSize);
        FlowPane horizontal = new FlowPane(horizontalChild);
        horizontal.setPrefWrapLength(100);

        attachToStage(initialScaleX, initialScaleY, vertical, horizontal);

        double initialWidth = vertical.snapSizeX(rawSize);
        double initialHeight = horizontal.snapSizeY(rawSize);
        assertEquals(initialWidth, vertical.prefWidth(100), EPSILON);
        assertEquals(initialHeight, horizontal.prefHeight(100), EPSILON);

        stage.setRenderScaleX(newScaleX);
        stage.setRenderScaleY(newScaleY);
        double newWidth = vertical.snapSizeX(rawSize);
        double newHeight = horizontal.snapSizeY(rawSize);
        assertNotEquals(initialWidth, newWidth, EPSILON);
        assertNotEquals(initialHeight, newHeight, EPSILON);
        assertEquals(newWidth, vertical.prefWidth(100), EPSILON);
        assertEquals(newHeight, horizontal.prefHeight(100), EPSILON);
    }

    static Stream<Arguments> renderScales() {
        return Stream.of(
            Arguments.of(1.0, 1.0),
            Arguments.of(1.25, 1.25),
            Arguments.of(1.5, 1.5),
            Arguments.of(1.75, 1.75),
            Arguments.of(2.0, 2.0),
            Arguments.of(1.25, 1.75),
            Arguments.of(1.5, 2.0),
            Arguments.of(2.0, 1.25));
    }

    static Stream<Arguments> renderScaleChanges() {
        return Stream.of(
            Arguments.of(1.0, 1.0, 1.5, 2.0),
            Arguments.of(1.25, 1.75, 2.0, 1.5),
            Arguments.of(1.5, 2.0, 1.25, 1.75),
            Arguments.of(2.0, 1.25, 1.75, 2.0));
    }

    private void attachToStage(double scaleX, double scaleY, Node... nodes) {
        Pane root = new Pane(nodes);
        stage = new Stage();
        stage.setRenderScaleX(scaleX);
        stage.setRenderScaleY(scaleY);
        stage.setScene(new Scene(root));
    }

    private static final class AreaBiasedRegion extends Region {
        private final Orientation bias;
        private final double prefWidth;
        private final double prefHeight;
        private final double area;

        AreaBiasedRegion(Orientation bias, double prefWidth, double prefHeight) {
            this.bias = bias;
            this.prefWidth = prefWidth;
            this.prefHeight = prefHeight;
            this.area = prefWidth * prefHeight;
        }

        @Override public Orientation getContentBias() {
            return bias;
        }

        @Override protected double computeMinWidth(double height) {
            return 0;
        }

        @Override protected double computeMinHeight(double width) {
            return 0;
        }

        @Override protected double computePrefWidth(double height) {
            return bias == Orientation.HORIZONTAL ? prefWidth :
                    area / (height == -1 ? prefHeight : height);
        }

        @Override protected double computePrefHeight(double width) {
            return bias == Orientation.VERTICAL ? prefHeight :
                    area / (width == -1 ? prefWidth : width);
        }

        @Override protected double computeMaxWidth(double height) {
            return Double.MAX_VALUE;
        }

        @Override protected double computeMaxHeight(double width) {
            return Double.MAX_VALUE;
        }
    }

    private static TextFlow createTextFlow() {
        TextFlow textFlow = new TextFlow(
            new Text("this is a long text that will be wrapped"),
            new Text("this is a long text that will be wrapped"),
            new Text("this is a long text that will be wrapped")
        );

        textFlow.setMaxWidth(200);

        return textFlow;
    }
}

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
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.Test;


public class StackPaneTest {
    StackPane stack;

    @Before public void setUp() {
        this.stack = new StackPane();
    }

    @Test public void testStackPaneDefaults() {
        assertEquals(Pos.CENTER, stack.getAlignment());
    }

    @Test public void testStackPaneNulls() {
        stack.setAlignment(null);

        // this musn't throw NPE
        stack.autosize();
        stack.layout();

        assertNull(null, stack.getAlignment());
        assertNull(null, stack.alignmentProperty().get());
    }

    @Test public void testSimpleStackPane() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        assertEquals(100, stack.minWidth(-1), 1e-100);
        assertEquals(200, stack.minHeight(-1), 1e-100);
        assertEquals(300, stack.prefWidth(-1), 1e-100);
        assertEquals(400, stack.prefHeight(-1), 1e-100);

        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, child2.getLayoutX(), 1e-100);
        assertEquals(150, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        stack.resize(500,500);
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(200, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentTopLeft() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.TOP_LEFT);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentTopCenter() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.TOP_CENTER);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentTopRight() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.TOP_RIGHT);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentCenterLeft() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.CENTER_LEFT);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(150, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentCenter() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.CENTER);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, child2.getLayoutX(), 1e-100);
        assertEquals(150, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentCenterRight() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.CENTER_RIGHT);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(150, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentBottomLeft() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.BOTTOM_LEFT);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(300, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentBottomCenter() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.BOTTOM_CENTER);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, child2.getLayoutX(), 1e-100);
        assertEquals(300, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentBottomRight() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.BOTTOM_RIGHT);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(300, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentBaselineLeft() {
        MockResizable child1 = new MockResizable(300,300);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.BASELINE_LEFT);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(190, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentBaselineCenter() {
        MockResizable child1 = new MockResizable(300,300);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.BASELINE_CENTER);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, child2.getLayoutX(), 1e-100);
        assertEquals(190, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentBaselineRight() {
        MockResizable child1 = new MockResizable(300,300);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.BASELINE_RIGHT);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(190, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentBaselineLeftComputed() {
        MockResizable child1 = new MockResizable(300,300) {
            @Override public double getBaselineOffset() {
                return BASELINE_OFFSET_SAME_AS_HEIGHT; // should be prefHeight
            }
        };
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.BASELINE_LEFT);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(200, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentBaselineCenterComputed() {
        MockResizable child1 = new MockResizable(300,300) {
            @Override public double getBaselineOffset() {
                return BASELINE_OFFSET_SAME_AS_HEIGHT; // should be prefHeight
            }
        };
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.BASELINE_CENTER);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, child2.getLayoutX(), 1e-100);
        assertEquals(200, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneAlignmentBaselineRightComputed() {
        MockResizable child1 = new MockResizable(300,300) {
            @Override public double getBaselineOffset() {
                return BASELINE_OFFSET_SAME_AS_HEIGHT; // should be prefHeight
            }
        };        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        stack.setAlignment(Pos.BASELINE_RIGHT);
        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(200, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneContentBiasNullNoChildHasContentBias() {
        Rectangle r = new Rectangle(100,100);
        MockResizable child = new MockResizable(100,200);
        stack.getChildren().addAll(r, child);

        assertNull(stack.getContentBias());
    }

    @Test public void testStackPaneContentBiasHORIZONTALIfChildHORIZONTAL() {
        Rectangle r = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        MockResizable child = new MockResizable(100,100);
        stack.getChildren().addAll(r, biased, child);

        assertEquals(Orientation.HORIZONTAL, stack.getContentBias());
        assertEquals(100, stack.prefWidth(-1), 0);
        assertEquals(200, stack.prefHeight(100), 0);
        assertEquals(100, stack.prefHeight(200), 0);
    }

    @Test public void testStackPaneContentBiasVERTICALIfChildVERTICAL() {
        Rectangle r = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100, 200);
        MockResizable child = new MockResizable(100,100);
        stack.getChildren().addAll(r, biased, child);

        assertEquals(Orientation.VERTICAL, stack.getContentBias());
        assertEquals(200, stack.prefHeight(-1), 0);
        assertEquals(100, stack.prefWidth(200), 0);
        assertEquals(200, stack.prefWidth(100), 0);
    }

    @Test public void testStackPaneSetMarginConstraint() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);

        assertNull(StackPane.getMargin(child1));

        Insets margin = new Insets(10,20,30,40);
        StackPane.setMargin(child1, margin);
        assertEquals(margin, StackPane.getMargin(child1));

        StackPane.setMargin(child1, null);
        assertNull(StackPane.getMargin(child1));
    }

    @Test public void testStackPaneMarginConstraint() {
        StackPane stack = new StackPane();
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        StackPane.setMargin(child1, new Insets(10,20,30,40));

        assertEquals(160, stack.minWidth(-1), 1e-100);
        assertEquals(240, stack.minHeight(-1), 1e-100);
        assertEquals(360, stack.prefWidth(-1), 1e-100);
        assertEquals(440, stack.prefHeight(-1), 1e-100);

        stack.autosize();
        stack.layout();
        assertEquals(40, child1.getLayoutX(), 1e-100);
        assertEquals(10, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(130, child2.getLayoutX(), 1e-100);
        assertEquals(170, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        stack.resize(500,500);
        stack.layout();
        assertEquals(40, child1.getLayoutX(), 1e-100);
        assertEquals(10, child1.getLayoutY(), 1e-100);
        assertEquals(440, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(460, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(200, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneSetAlignmentConstraint() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);

        assertNull(StackPane.getAlignment(child1));

        StackPane.setAlignment(child1, Pos.TOP_LEFT);
        assertEquals(Pos.TOP_LEFT, StackPane.getAlignment(child1));

        StackPane.setAlignment(child1, null);
        assertNull(StackPane.getAlignment(child1));
    }

    @Test public void testStackPaneAlignmentConstraint() {
        StackPane stack = new StackPane();
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        stack.getChildren().addAll(child1, child2);

        StackPane.setAlignment(child2, Pos.TOP_LEFT);

        assertEquals(100, stack.minWidth(-1), 1e-100);
        assertEquals(200, stack.minHeight(-1), 1e-100);
        assertEquals(300, stack.prefWidth(-1), 1e-100);
        assertEquals(400, stack.prefHeight(-1), 1e-100);

        stack.autosize();
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        stack.resize(500,500);
        stack.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneWithHorizontalBiasedChild() {
        StackPane stack = new StackPane();

        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100,100);

        stack.getChildren().add(biased);

        assertEquals(100, stack.prefWidth(-1), 1e-100);
        assertEquals(100, stack.prefHeight(-1), 1e-100);

        stack.autosize();
        stack.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(100, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, biased.getLayoutBounds().getHeight(), 1e-100);

        stack.resize(200, 200);
        stack.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(75, biased.getLayoutY(), 1e-100);
        assertEquals(200, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(50, biased.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testStackPaneWithVerticalBiasedChild() {
        StackPane stack = new StackPane();

        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100,100);

        stack.getChildren().add(biased);

        assertEquals(100, stack.prefWidth(-1), 1e-100);
        assertEquals(100, stack.prefHeight(-1), 1e-100);

        stack.autosize();
        stack.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(100, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, biased.getLayoutBounds().getHeight(), 1e-100);

        stack.resize(200, 200);
        stack.layout();
        assertEquals(75, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(50, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased.getLayoutBounds().getHeight(), 1e-100);
    }

}

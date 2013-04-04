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
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.Test;


public class HBoxTest {
    HBox hbox;

    @Before public void setUp() {
        this.hbox = new HBox();
    }


    @Test public void testHBoxDefaults() {
        assertEquals(0, hbox.getSpacing(), 1e-100);
        assertTrue(hbox.isFillHeight());
        assertEquals(Pos.TOP_LEFT, hbox.getAlignment());
    }

    @Test public void testHBoxNulls() {
        hbox.setAlignment(null);

        // this musn't throw NPE
        hbox.autosize();
        hbox.layout();

        assertNull(null, hbox.getAlignment());
        assertNull(null, hbox.alignmentProperty().get());
    }

    @Test public void testSimpleHBox() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        assertEquals(200, hbox.minWidth(-1), 1e-100);
        assertEquals(200, hbox.minHeight(-1), 1e-100);
        assertEquals(400, hbox.prefWidth(-1), 1e-100);
        assertEquals(400, hbox.prefHeight(-1), 1e-100);

        hbox.autosize();
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        hbox.resize(500,500);
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxSpacing() {
        hbox.setSpacing(10);
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        assertEquals(210, hbox.minWidth(-1), 1e-100);
        assertEquals(200, hbox.minHeight(-1), 1e-100);
        assertEquals(410, hbox.prefWidth(-1), 1e-100);
        assertEquals(400, hbox.prefHeight(-1), 1e-100);

        hbox.autosize();
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(310, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        hbox.resize(500,500);
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(310, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxFillHeight() {
        hbox.setFillHeight(false);
        hbox.setAlignment(Pos.TOP_CENTER);
        MockResizable child1 = new MockResizable(100,100, 200,300, 500,600);
        MockResizable child2 = new MockResizable(100,100, 100, 400, 800, 800);
        hbox.getChildren().addAll(child1, child2);

        assertEquals(200, hbox.minWidth(-1), 1e-100);
        assertEquals(100, hbox.minHeight(-1), 1e-100);
        assertEquals(300, hbox.prefWidth(-1), 1e-100);
        assertEquals(400, hbox.prefHeight(-1), 1e-100);

        hbox.autosize();
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(200, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child2.getLayoutBounds().getHeight(), 1e-100);

        hbox.resize(500,500);
        hbox.layout();
        assertEquals(100, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(200, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentTopLeft() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        hbox.setAlignment(Pos.TOP_LEFT);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentTopCenter() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        hbox.setAlignment(Pos.TOP_CENTER);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(50, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(350, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentTopRight() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        hbox.setAlignment(Pos.TOP_RIGHT);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(100, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentCenterLeft() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, child2.getLayoutX(), 1e-100);
        assertEquals(200, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentCenter() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        hbox.setAlignment(Pos.CENTER);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(50, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(350, child2.getLayoutX(), 1e-100);
        assertEquals(200, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentCenterRight() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(100, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, child2.getLayoutX(), 1e-100);
        assertEquals(200, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentBottomLeft() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        hbox.setAlignment(Pos.BOTTOM_LEFT);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentBottomCenter() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        hbox.setAlignment(Pos.BOTTOM_CENTER);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(50, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(350, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentBottomRight() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        hbox.setAlignment(Pos.BOTTOM_RIGHT);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(100, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentBaselineLeft() {
        MockResizable child1 = new MockResizable(300,300); // baseline=290
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        // BASELINE makes fillHeight false
        hbox.setAlignment(Pos.BASELINE_LEFT);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, child2.getLayoutX(), 1e-100);
        assertEquals(190, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentBaselineCenter() {
        MockResizable child1 = new MockResizable(300,300); // baseline=290
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        // BASELINE makes fillHeight false
        hbox.setAlignment(Pos.BASELINE_CENTER);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(50, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(350, child2.getLayoutX(), 1e-100);
        assertEquals(190, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxAlignmentBaselineRight() {
        MockResizable child1 = new MockResizable(300,300); // baseline=290
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        // BASELINE makes fillHeight false
        hbox.setAlignment(Pos.BASELINE_RIGHT);
        hbox.resize(500,500);
        hbox.layout();
        assertEquals(100, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, child2.getLayoutX(), 1e-100);
        assertEquals(190, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    /* Content Bias */

    @Test public void testHBoxContentBiasNullNoChildHasContentBias() {
        Rectangle r = new Rectangle(100,100);
        MockResizable child = new MockResizable(200,100);
        hbox.getChildren().addAll(r, child);

        assertNull(hbox.getContentBias());
        assertEquals(300, hbox.prefWidth(-1), 0);
        assertEquals(100, hbox.prefHeight(-1), 0);
        assertEquals(300, hbox.prefWidth(200), 0);
    }

    @Test public void testHBoxContentBiasVERTICALIfChildVERTICAL() {
        Rectangle r = new Rectangle(100,50);
        MockBiased biased = new MockBiased(Orientation.VERTICAL, 200, 100);
        MockResizable child = new MockResizable(100,100);
        hbox.getChildren().addAll(r, biased, child);

        assertEquals(Orientation.VERTICAL, hbox.getContentBias());

    }

    @Test public void testHBoxWithVerticalContentBiasAtPrefSize() {
        Rectangle rect1 = new Rectangle(100,50);
        MockBiased biased2 = new MockBiased(Orientation.VERTICAL, 200, 100);
        MockResizable resizable3 = new MockResizable(100,100);
        hbox.getChildren().addAll(rect1, biased2, resizable3);

        assertEquals(100, hbox.prefHeight(-1), 0);
        assertEquals(400, hbox.prefWidth(-1), 0);

        hbox.autosize(); // 400 x 100
        hbox.layout();
        assertEquals(0, rect1.getLayoutX(), 1e-100);
        assertEquals(0, rect1.getLayoutY(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, biased2.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, biased2.getLayoutX(), 1e-100);
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(100, resizable3.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, resizable3.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, resizable3.getLayoutX(), 1e-100);
        assertEquals(0, resizable3.getLayoutY(), 1e-100);
    }

    @Test public void testHBoxWithVerticalContentBiasWithVerticalShrinking() {
        Rectangle rect1 = new Rectangle(100,50);
        MockBiased biased2 = new MockBiased(Orientation.VERTICAL, 200, 100);
        MockResizable resizable3 = new MockResizable(100,100);
        hbox.getChildren().addAll(rect1, biased2, resizable3);

        assertEquals(600, hbox.prefWidth(50), 0);

        hbox.resize(600,50); // height smaller than pref
        hbox.layout();
        assertEquals(0, rect1.getLayoutX(), 1e-100);
        assertEquals(0, rect1.getLayoutY(), 1e-100);
        // Only the width of the child that has a content bias should change.
        assertEquals(400, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(50, biased2.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, biased2.getLayoutX(), 1e-100);
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(100, resizable3.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(50, resizable3.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(500, resizable3.getLayoutX(), 1e-100);
        assertEquals(0, resizable3.getLayoutY(), 1e-100);
    }

    @Test public void testHBoxWithVerticalContentBiasWithVerticalGrowing() {
        Rectangle rect1 = new Rectangle(100,50);
        MockBiased biased2 = new MockBiased(Orientation.VERTICAL, 200, 100);
        MockResizable resizable3 = new MockResizable(100,100);
        hbox.getChildren().addAll(rect1, biased2, resizable3);

        assertEquals(300, hbox.prefWidth(200), 0);

        hbox.resize(300,200); // height larger than pref
        hbox.layout();
        assertEquals(0, rect1.getLayoutX(), 1e-100);
        assertEquals(0, rect1.getLayoutY(), 1e-100);
        assertEquals(100, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, biased2.getLayoutX(), 1e-100);
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(100, resizable3.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, resizable3.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, resizable3.getLayoutX(), 1e-100);
        assertEquals(0, resizable3.getLayoutY(), 1e-100);
    }

    @Test public void testHBoxContentBiasHORIZONTALIfChildHORIZONTAL() {
        Rectangle r = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 200, 100);
        MockResizable child = new MockResizable(100,100);
        hbox.getChildren().addAll(r, biased, child);

        assertEquals(Orientation.HORIZONTAL, hbox.getContentBias());
    }

    @Test public void testHBoxWithHorizontalContentBiasAtPrefSize() {
        Rectangle rect1 = new Rectangle(100,50);
        MockBiased biased2 = new MockBiased(Orientation.HORIZONTAL, 200, 100);
        MockResizable resizable3 = new MockResizable(100,100);
        hbox.getChildren().addAll(rect1, biased2, resizable3);

        assertEquals(400, hbox.prefWidth(-1), 0);
        assertEquals(100, hbox.prefHeight(-1), 0);

        hbox.autosize(); // 400 x 100
        hbox.layout();
        assertEquals(0, rect1.getLayoutX(), 1e-100);
        assertEquals(0, rect1.getLayoutY(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, biased2.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, biased2.getLayoutX(), 1e-100);
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(100, resizable3.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, resizable3.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, resizable3.getLayoutX(), 1e-100);
        assertEquals(0, resizable3.getLayoutY(), 1e-100);
    }

    @Test public void testHBoxWithHorizontalContentBiasWithHorizontalShrinking() {
        Rectangle rect1 = new Rectangle(100,50);
        MockBiased biased2 = new MockBiased(Orientation.HORIZONTAL, 200, 100);
        MockResizable resizable3 = new MockResizable(100,100);
        hbox.getChildren().addAll(rect1, biased2, resizable3);

        assertEquals(134, hbox.prefHeight(300), 0);

        hbox.resize(300,134); // width less than preferred
        hbox.layout();
        assertEquals(0, rect1.getLayoutX(), 1e-100);
        assertEquals(0, rect1.getLayoutY(), 1e-100);
        assertEquals(150, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(134, biased2.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, biased2.getLayoutX(), 1e-100);
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(50, resizable3.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(134, resizable3.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(250, resizable3.getLayoutX(), 1e-100);
        assertEquals(0, resizable3.getLayoutY(), 1e-100);
    }
    
    @Test public void testHBoxWithHorizontalContentBiasWithHorizontalGrowingFillHeightFalse() {
        hbox.setFillHeight(false);

        Rectangle rect1 = new Rectangle(100,50);
        MockBiased biased2 = new MockBiased(Orientation.HORIZONTAL, 200, 100);
        MockResizable resizable3 = new MockResizable(100,100);
        hbox.getChildren().addAll(rect1, biased2, resizable3);

        HBox.setHgrow(biased2, Priority.ALWAYS);

        assertEquals(100, hbox.prefHeight(500), 0);

        hbox.resize(500,100); // width greater than preferred
        hbox.layout();
        assertEquals(0, rect1.getLayoutX(), 1e-100);
        assertEquals(0, rect1.getLayoutY(), 1e-100);
        assertEquals(300, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(67, biased2.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, biased2.getLayoutX(), 1e-100);
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(100, resizable3.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, resizable3.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, resizable3.getLayoutX(), 1e-100);
        assertEquals(0, resizable3.getLayoutY(), 1e-100);
    }

    @Test public void testHBoxWithHorizontalContentBiasWithHorizontalGrowingFillHeightTrue() {
        hbox.setFillHeight(true);

        Rectangle rect1 = new Rectangle(100,50);
        MockBiased biased2 = new MockBiased(Orientation.HORIZONTAL, 200, 100);
        MockResizable resizable3 = new MockResizable(100,100);
        hbox.getChildren().addAll(rect1, biased2, resizable3);

        HBox.setHgrow(biased2, Priority.ALWAYS);

        assertEquals(100, hbox.prefHeight(500), 0);

        hbox.resize(500,100); // width greater than preferred
        hbox.layout();
        assertEquals(0, rect1.getLayoutX(), 1e-100);
        assertEquals(0, rect1.getLayoutY(), 1e-100);
        assertEquals(300, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(67, biased2.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, biased2.getLayoutX(), 1e-100);
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(100, resizable3.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, resizable3.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, resizable3.getLayoutX(), 1e-100);
        assertEquals(0, resizable3.getLayoutY(), 1e-100);
    }




//    @Test public void testHBoxContentBiasNullIfFillHeightFalse() {
//        hbox.setFillHeight(false);
//        Rectangle r = new Rectangle(100,100);
//        MockBiased biased = new MockBiased(Orientation.VERTICAL, 200, 100);
//        MockResizable child = new MockResizable(100,100);
//        hbox.getChildren().addAll(r, biased, child);
//
//        assertNull(hbox.getContentBias());
//        assertEquals(100, hbox.prefHeight(-1), 0);
//        assertEquals(400, hbox.prefWidth(-1), 0);
//        assertEquals(400, hbox.prefWidth(200), 0);
//    }

//    @Test public void testHBoxContentBiasNullIfChildHORIZONTALAndFillHeightTrue() {
//        hbox.setFillHeight(true);
//        Rectangle r = new Rectangle(100,100);
//        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100, 200);
//        MockResizable child = new MockResizable(100,100);
//        hbox.getChildren().addAll(r, biased, child);
//
//        assertNull(hbox.getContentBias());
//        assertEquals(300, hbox.prefWidth(-1), 0);
//        assertEquals(200, hbox.prefHeight(-1), 0);
//        assertEquals(200, hbox.prefHeight(200), 0);
//    }
//
//    @Test public void testHBoxContentBiasNullIfChildHORIZONTALAndFillHeightFalse() {
//        hbox.setFillHeight(false);
//        Rectangle r = new Rectangle(100,100);
//        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100, 200);
//        MockResizable child = new MockResizable(100,100);
//        hbox.getChildren().addAll(r, biased, child);
//
//        assertNull(hbox.getContentBias());
//        assertEquals(300, hbox.prefWidth(-1), 0);
//        assertEquals(200, hbox.prefHeight(-1), 0);
//        assertEquals(200, hbox.prefHeight(200), 0);
//    }

    @Test public void testHBoxSetMarginConstraint() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);

        assertNull(HBox.getMargin(child1));

        Insets margin = new Insets(10,20,30,40);
        HBox.setMargin(child1, margin);
        assertEquals(margin, HBox.getMargin(child1));

        HBox.setMargin(child1, null);
        assertNull(HBox.getMargin(child1));
    }

    @Test public void testHBoxMarginConstraint() {
        HBox hbox = new HBox();
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        HBox.setMargin(child1, new Insets(10,20,30,40));

        assertEquals(260, hbox.minWidth(-1), 1e-100);
        assertEquals(240, hbox.minHeight(-1), 1e-100);
        assertEquals(460, hbox.prefWidth(-1), 1e-100);
        assertEquals(440, hbox.prefHeight(-1), 1e-100);

        hbox.autosize();
        hbox.layout();
        assertEquals(40, child1.getLayoutX(), 1e-100);
        assertEquals(10, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(360, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        hbox.resize(500,500);
        hbox.layout();
        assertEquals(40, child1.getLayoutX(), 1e-100);
        assertEquals(10, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(460, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(360, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxSetHgrowConstraint() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);

        assertNull(HBox.getHgrow(child1));

        HBox.setHgrow(child1, Priority.ALWAYS);
        assertEquals(Priority.ALWAYS, HBox.getHgrow(child1));

        HBox.setHgrow(child1, null);
        assertNull(HBox.getHgrow(child1));
    }

    @Test public void testHBoxHgrowConstraint() {
        HBox hbox = new HBox();
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        hbox.getChildren().addAll(child1, child2);

        HBox.setHgrow(child1, Priority.ALWAYS);

        assertEquals(200, hbox.minWidth(-1), 1e-100);
        assertEquals(200, hbox.minHeight(-1), 1e-100);
        assertEquals(400, hbox.prefWidth(-1), 1e-100);
        assertEquals(400, hbox.prefHeight(-1), 1e-100);

        hbox.autosize();
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(300, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        hbox.resize(500,500);
        hbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, child2.getLayoutX(), 1e-100);
        assertEquals(0, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testHBoxWithHorizontalBiasedChild() {
        HBox hbox = new HBox();
        System.out.println("************************************");

        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100,100);
        biased.setId("test");
        MockBiased biased2 = new MockBiased(Orientation.HORIZONTAL, 200, 200);
        biased2.setId("test");
        HBox.setHgrow(biased, Priority.ALWAYS);

        hbox.getChildren().addAll(biased, biased2);

        assertEquals(300, hbox.prefWidth(-1), 1e-100);
        assertEquals(200, hbox.prefHeight(-1), 1e-100);
        System.out.println("************************************");

        hbox.autosize();
        hbox.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(100, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(100, biased2.getLayoutX(), 1e-100);
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getHeight(), 1e-100);

        hbox.resize(400, 400);
        hbox.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(200, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(50, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, biased2.getLayoutX(), 1e-100);
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getHeight(), 1e-100);


    }

    @Test public void testHBoxWithVerticalBiasedChild() {
        HBox hbox = new HBox();

        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100,100);
        MockBiased biased2 = new MockBiased(Orientation.VERTICAL, 200, 200);
        HBox.setHgrow(biased, Priority.ALWAYS);

        hbox.getChildren().addAll(biased, biased2);

        assertEquals(300, hbox.prefWidth(-1), 1e-100);
        assertEquals(200, hbox.prefHeight(-1), 1e-100);

        hbox.autosize();
        hbox.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(50, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(50, biased2.getLayoutX(), 1e-100);
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getHeight(), 1e-100);

        hbox.resize(400, 400);
        hbox.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(25, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(25, biased2.getLayoutX(), 1e-100); //
        assertEquals(0, biased2.getLayoutY(), 1e-100);
        assertEquals(100, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, biased2.getLayoutBounds().getHeight(), 1e-100);

    }

    @Test public void testMaxWidthHonoredWhenGrowing() {
        MockRegion child = new MockRegion(10,10,100,100,300,300);
        MockRegion child2 = new MockRegion(10,10,100,100,200,200);
        HBox.setHgrow(child, Priority.ALWAYS);
        HBox.setHgrow(child2, Priority.ALWAYS);

        hbox.getChildren().addAll(child, child2);

        hbox.resize(600,600);
        hbox.layout();
        assertEquals(300, child.getWidth(), 0);
        assertEquals(0, child.getLayoutX(), 0);
        assertEquals(200, child2.getWidth(), 0);
        assertEquals(300, child2.getLayoutX(), 0);
    }

}

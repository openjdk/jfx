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


public class VBoxTest {
    VBox vbox;

    @Before public void setUp() {
        this.vbox = new VBox();
    }

    @Test public void testVBoxDefaults() {
        assertEquals(0, vbox.getSpacing(), 1e-100);
        assertTrue(vbox.isFillWidth());
        assertEquals(Pos.TOP_LEFT, vbox.getAlignment());
    }

    @Test public void testSimpleVBox() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        assertEquals(100, vbox.minWidth(-1), 1e-100);
        assertEquals(300, vbox.minHeight(-1), 1e-100);
        assertEquals(300, vbox.prefWidth(-1), 1e-100);
        assertEquals(500, vbox.prefHeight(-1), 1e-100);

        vbox.autosize();
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        vbox.resize(500,500);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxSpacing() {
        vbox.setSpacing(10);
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        assertEquals(100, vbox.minWidth(-1), 1e-100);
        assertEquals(310, vbox.minHeight(-1), 1e-100);
        assertEquals(300, vbox.prefWidth(-1), 1e-100);
        assertEquals(510, vbox.prefHeight(-1), 1e-100);

        vbox.autosize();
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(410, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        vbox.resize(500,500);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(390, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxFillWidth() {
        vbox.setFillWidth(false);
        vbox.setAlignment(Pos.TOP_CENTER);
        MockResizable child1 = new MockResizable(100,100, 200,300, 500,600);
        MockResizable child2 = new MockResizable(100,100, 100, 400, 800, 800);
        vbox.getChildren().addAll(child1, child2);

        assertEquals(100, vbox.minWidth(-1), 1e-100);
        assertEquals(200, vbox.minHeight(-1), 1e-100);
        assertEquals(200, vbox.prefWidth(-1), 1e-100);
        assertEquals(700, vbox.prefHeight(-1), 1e-100);

        vbox.autosize();
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(200, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(50, child2.getLayoutX(), 1e-100);
        assertEquals(300, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child2.getLayoutBounds().getHeight(), 1e-100);

        vbox.resize(500,800);
        vbox.layout();
        assertEquals(150, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(200, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(300, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxAlignmentTopLeft() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        vbox.setAlignment(Pos.TOP_LEFT);
        vbox.resize(500,600);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxAlignmentTopCenter() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.resize(500,600);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxAlignmentTopRight() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        vbox.setAlignment(Pos.TOP_RIGHT);
        vbox.resize(500,600);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxAlignmentCenterLeft() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        vbox.setAlignment(Pos.CENTER_LEFT);
        vbox.resize(500,600);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(50, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(450, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxAlignmentCenter() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        vbox.setAlignment(Pos.CENTER);
        vbox.resize(500,600);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(50, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(450, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxAlignmentCenterRight() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        vbox.setAlignment(Pos.CENTER_RIGHT);
        vbox.resize(500,600);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(50, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, child2.getLayoutX(), 1e-100);
        assertEquals(450, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxAlignmentBottomLeft() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        vbox.setAlignment(Pos.BOTTOM_LEFT);
        vbox.resize(500,600);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(100, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(500, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxAlignmentBottomCenter() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        vbox.setAlignment(Pos.BOTTOM_CENTER);
        vbox.resize(500,600);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(100, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(200, child2.getLayoutX(), 1e-100);
        assertEquals(500, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxAlignmentBottomRight() {
        MockResizable child1 = new MockResizable(300,400);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        vbox.setAlignment(Pos.BOTTOM_RIGHT);
        vbox.resize(500,600);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(100, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(400, child2.getLayoutX(), 1e-100);
        assertEquals(500, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxContentBiasNullNoChildHasContentBias() {
        Rectangle r = new Rectangle(100,100);
        MockResizable child = new MockResizable(100,200);
        vbox.getChildren().addAll(r, child);

        assertNull(vbox.getContentBias());
        assertEquals(100, vbox.prefWidth(-1), 0);
        assertEquals(300, vbox.prefHeight(-1), 0);
        assertEquals(300, vbox.prefHeight(200), 0);
    }

    @Test public void testVBoxContentBiasHORIZONTALifChildHORIZONTAL() {
        Rectangle r = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        MockResizable child = new MockResizable(100,100);
        vbox.getChildren().addAll(r, biased, child);

        assertEquals(Orientation.HORIZONTAL, vbox.getContentBias());        
    }

    @Test public void testVBoxWithHorizontalContentBiasAtPrefSize() {
        Rectangle rect = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        MockResizable resizable = new MockResizable(100,100);
        vbox.getChildren().addAll(rect, biased, resizable);

        assertEquals(Orientation.HORIZONTAL, vbox.getContentBias());
        assertEquals(100, vbox.prefWidth(-1), 0);
        assertEquals(400, vbox.prefHeight(-1), 0);

        vbox.autosize(); // 100 x 400
        vbox.layout();
        assertEquals(0, rect.getLayoutX(), 1e-100);
        assertEquals(0, rect.getLayoutY(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(100, biased.getLayoutY(), 1e-100);
        assertEquals(100, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, resizable.getLayoutX(), 1e-100);
        assertEquals(300, resizable.getLayoutY(), 1e-100);
        assertEquals(100, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, resizable.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxWithHorizontalContentBiasWithHorizontalShrinking() {
        Rectangle rect = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        MockResizable resizable = new MockResizable(100,100);
        vbox.getChildren().addAll(rect, biased, resizable);

        assertEquals(Orientation.HORIZONTAL, vbox.getContentBias());        
        assertEquals(600, vbox.prefHeight(50), 0);

        vbox.resize(50, 600);
        vbox.layout();
        assertEquals(0, rect.getLayoutX(), 1e-100);
        assertEquals(0, rect.getLayoutY(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(100, biased.getLayoutY(), 1e-100);
        assertEquals(50, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, resizable.getLayoutX(), 1e-100);
        assertEquals(500, resizable.getLayoutY(), 1e-100);
        assertEquals(50, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, resizable.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxWithHorizontalContentBiasWithHorizontalGrowing() {
        Rectangle rect = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        MockResizable resizable = new MockResizable(100,100);
        vbox.getChildren().addAll(rect, biased, resizable);

        assertEquals(Orientation.HORIZONTAL, vbox.getContentBias());
        assertEquals(267, vbox.prefHeight(300), 0);

        vbox.resize(300, 267);
        vbox.layout();
        assertEquals(0, rect.getLayoutX(), 1e-100);
        assertEquals(0, rect.getLayoutY(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(100, biased.getLayoutY(), 1e-100);
        assertEquals(300, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(67, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, resizable.getLayoutX(), 1e-100);
        assertEquals(167, resizable.getLayoutY(), 1e-100);
        assertEquals(300, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, resizable.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxContentBiasVERTICALIfChildVERTICAL() {
        Rectangle r = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100, 200);
        MockResizable child = new MockResizable(100,100);
        vbox.getChildren().addAll(r, biased, child);

        assertEquals(Orientation.VERTICAL, vbox.getContentBias());
    }

    @Test public void testVBoxWithVerticalContentBiasAtPrefSize() {
        Rectangle rect = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100, 200);
        MockResizable resizable = new MockResizable(100,100);
        vbox.getChildren().addAll(rect, biased, resizable);

        assertEquals(Orientation.VERTICAL, vbox.getContentBias());
        assertEquals(100, vbox.prefWidth(-1), 0);
        assertEquals(400, vbox.prefHeight(-1), 0);

        vbox.autosize(); // 100 x 400
        vbox.layout();
        assertEquals(0, rect.getLayoutX(), 1e-100);
        assertEquals(0, rect.getLayoutY(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(100, biased.getLayoutY(), 1e-100);
        assertEquals(100, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, resizable.getLayoutX(), 1e-100);
        assertEquals(300, resizable.getLayoutY(), 1e-100);
        assertEquals(100, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, resizable.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxWithVerticalContentBiasWithVerticalShrinking() {
        Rectangle rect = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100, 200);
        MockResizable resizable = new MockResizable(100,100);
        vbox.getChildren().addAll(rect, biased, resizable);

        assertEquals(Orientation.VERTICAL, vbox.getContentBias());
        assertEquals(134, vbox.prefWidth(300), 0);

        vbox.resize(134, 300);
        vbox.layout();
        assertEquals(0, rect.getLayoutX(), 1e-100);
        assertEquals(0, rect.getLayoutY(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(100, biased.getLayoutY(), 1e-100);
        assertEquals(134, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(150, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, resizable.getLayoutX(), 1e-100);
        assertEquals(250, resizable.getLayoutY(), 1e-100);
        assertEquals(134, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(50, resizable.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxWithVerticalContentBiasWithVerticalGrowingFillWidthFalse() {
        vbox.setFillWidth(false);
        Rectangle rect = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100, 200);
        MockResizable resizable = new MockResizable(100,100);
        vbox.getChildren().addAll(rect, biased, resizable);

        vbox.setVgrow(biased, Priority.ALWAYS);
        
        assertEquals(Orientation.VERTICAL, vbox.getContentBias());
        assertEquals(100, vbox.prefWidth(500), 0);

        vbox.resize(100, 500);
        vbox.layout();
        assertEquals(0, rect.getLayoutX(), 1e-100);
        assertEquals(0, rect.getLayoutY(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(100, biased.getLayoutY(), 1e-100);
        assertEquals(67, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, resizable.getLayoutX(), 1e-100);
        assertEquals(400, resizable.getLayoutY(), 1e-100);
        assertEquals(100, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, resizable.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxWithVerticalContentBiasWithVerticalGrowingFillWidthTrue() {
        vbox.setFillWidth(true);
        Rectangle rect = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100, 200);
        MockResizable resizable = new MockResizable(100,100);
        vbox.getChildren().addAll(rect, biased, resizable);

        vbox.setVgrow(biased, Priority.ALWAYS);

        assertEquals(Orientation.VERTICAL, vbox.getContentBias());
        assertEquals(100, vbox.prefWidth(500), 0);

        vbox.resize(100, 500);
        vbox.layout();
        assertEquals(0, rect.getLayoutX(), 1e-100);
        assertEquals(0, rect.getLayoutY(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, rect.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(100, biased.getLayoutY(), 1e-100);
        assertEquals(67, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(300, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, resizable.getLayoutX(), 1e-100);
        assertEquals(400, resizable.getLayoutY(), 1e-100);
        assertEquals(100, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, resizable.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxContentBiasHORIZONTALIfChildHORIZONTALAndFillWidthTrue() {
        vbox.setFillWidth(true);
        Rectangle r = new Rectangle(100,100);
        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100, 200);
        MockResizable child = new MockResizable(100,100);
        vbox.getChildren().addAll(r, biased, child);

        assertEquals(Orientation.HORIZONTAL, vbox.getContentBias());
        assertEquals(100, vbox.prefWidth(-1), 0);
        assertEquals(400, vbox.prefHeight(100), 0);
        assertEquals(300, vbox.prefHeight(200), 0);
    }

//    These test are no longer valid.  If a content bias is set getContentBias()
//    should never return null.
//    @Test public void testVBoxContentBiasNullIfChildHORIZONTALAndFillWidthFalse() {
//        vbox.setFillWidth(false);
//        Rectangle r = new Rectangle(100,100);
//        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100, 200);
//        MockResizable child = new MockResizable(100,100);
//        vbox.getChildren().addAll(r, biased, child);
//
//        assertNull(vbox.getContentBias());
//        assertEquals(100, vbox.prefWidth(-1), 0);
//        assertEquals(400, vbox.prefHeight(-1), 0);
//        assertEquals(400, vbox.prefHeight(200), 0);
//    }
//
//    @Test public void testVBoxContentBiasNullIfChildVERTICALAndFillWidthTrue() {
//        vbox.setFillWidth(true);
//        Rectangle r = new Rectangle(100,100);
//        MockBiased biased = new MockBiased(Orientation.VERTICAL, 200, 100);
//        MockResizable child = new MockResizable(100,100);
//        vbox.getChildren().addAll(r, biased, child);
//
//        assertNull(vbox.getContentBias());
//        assertEquals(300, vbox.prefHeight(-1), 0);
//        assertEquals(200, vbox.prefWidth(-1), 0);
//        assertEquals(200, vbox.prefWidth(200), 0);
//    }
//
//    @Test public void testVBoxContentBiasNullIfChildVERTICALAndFillWidthFalse() {
//        vbox.setFillWidth(false);
//        Rectangle r = new Rectangle(100,100);
//        MockBiased biased = new MockBiased(Orientation.VERTICAL, 200, 100);
//        MockResizable child = new MockResizable(100,100);
//        vbox.getChildren().addAll(r, biased, child);
//
//        assertNull(vbox.getContentBias());
//        assertEquals(300, vbox.prefHeight(-1), 0);
//        assertEquals(200, vbox.prefWidth(-1), 0);
//        assertEquals(200, vbox.prefWidth(200), 0);
//    }
    
    @Test public void testVBoxSetMarginConstraint() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);

        assertNull(VBox.getMargin(child1));
        
        Insets margin = new Insets(10,20,30,40);
        VBox.setMargin(child1, margin);
        assertEquals(margin, VBox.getMargin(child1));

        VBox.setMargin(child1, null);
        assertNull(VBox.getMargin(child1));
    }

    @Test public void testVBoxMarginConstraint() {
        VBox vbox = new VBox();
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        VBox.setMargin(child1, new Insets(10,20,30,40));

        assertEquals(160, vbox.minWidth(-1), 1e-100);
        assertEquals(340, vbox.minHeight(-1), 1e-100);
        assertEquals(360, vbox.prefWidth(-1), 1e-100);
        assertEquals(540, vbox.prefHeight(-1), 1e-100);

        vbox.autosize();
        vbox.layout();
        assertEquals(40, child1.getLayoutX(), 1e-100);
        assertEquals(10, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(440, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        vbox.resize(500,500);
        vbox.layout();
        assertEquals(40, child1.getLayoutX(), 1e-100);
        assertEquals(10, child1.getLayoutY(), 1e-100);
        assertEquals(440, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(360, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxSetVgrowConstraint() {
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);

        assertNull(VBox.getVgrow(child1));

        VBox.setVgrow(child1, Priority.ALWAYS);
        assertEquals(Priority.ALWAYS, VBox.getVgrow(child1));

        VBox.setVgrow(child1, null);
        assertNull(VBox.getVgrow(child1));
    }

    @Test public void testVBoxHgrowConstraint() {
        VBox vbox = new VBox();
        MockResizable child1 = new MockResizable(100,200, 300,400, 500,600);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        VBox.setVgrow(child1, Priority.ALWAYS);

        assertEquals(100, vbox.minWidth(-1), 1e-100);
        assertEquals(300, vbox.minHeight(-1), 1e-100);
        assertEquals(300, vbox.prefWidth(-1), 1e-100);
        assertEquals(500, vbox.prefHeight(-1), 1e-100);

        vbox.autosize();
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(300, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(400, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(400, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        vbox.resize(500,600);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(500, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(500, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxWithHorizontalBiasedChild() {
        VBox vbox = new VBox();

        MockBiased biased = new MockBiased(Orientation.HORIZONTAL, 100,100);
        MockBiased biased2 = new MockBiased(Orientation.HORIZONTAL, 200, 200);
        VBox.setVgrow(biased, Priority.ALWAYS);

        vbox.getChildren().addAll(biased, biased2);

        assertEquals(200, vbox.prefWidth(-1), 1e-100);
        assertEquals(300, vbox.prefHeight(-1), 1e-100);

        vbox.autosize();
        vbox.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(200, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(50, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, biased2.getLayoutX(), 1e-100);
        assertEquals(50, biased2.getLayoutY(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testVBoxWithVerticalBiasedChild() {
        VBox vbox = new VBox();

        MockBiased biased = new MockBiased(Orientation.VERTICAL, 100,100);
        MockBiased biased2 = new MockBiased(Orientation.VERTICAL, 200, 200);
        VBox.setVgrow(biased, Priority.ALWAYS);

        vbox.getChildren().addAll(biased, biased2);

        assertEquals(200, vbox.prefWidth(-1), 1e-100);
        assertEquals(300, vbox.prefHeight(-1), 1e-100);

        vbox.autosize();
        vbox.layout();
        assertEquals(0, biased.getLayoutX(), 1e-100);
        assertEquals(0, biased.getLayoutY(), 1e-100);
        assertEquals(100, biased.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, biased.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, biased2.getLayoutX(), 1e-100);
        assertEquals(100, biased2.getLayoutY(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, biased2.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testMaxHeightHonoredWhenGrowing() {
        MockRegion child = new MockRegion(10,10,100,100,300,300);
        MockRegion child2 = new MockRegion(10,10,100,100,200,200);
        VBox.setVgrow(child, Priority.ALWAYS);
        VBox.setVgrow(child2, Priority.ALWAYS);

        vbox.getChildren().addAll(child, child2);

        vbox.resize(600,600);
        vbox.layout();
        assertEquals(300, child.getHeight(), 0);
        assertEquals(0, child.getLayoutY(), 0);
        assertEquals(200, child2.getHeight(), 0);
        assertEquals(300, child2.getLayoutY(), 0);
    }

    @Test public void testLayoutWhenChildrenAreRemoved_RT19406() {
        Rectangle child1 = new Rectangle(100,100);
        Rectangle child2 = new Rectangle(100, 100);
        vbox.getChildren().addAll(child1, child2);

        assertEquals(100, vbox.prefWidth(-1), 1e-100);
        assertEquals(200, vbox.prefHeight(-1), 1e-100);

        vbox.resize(100, 200);
        vbox.layout();
        assertEquals(0, child1.getLayoutX(), 1e-100);
        assertEquals(0, child1.getLayoutY(), 1e-100);
        assertEquals(100, child1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child1.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, child2.getLayoutX(), 1e-100);
        assertEquals(100, child2.getLayoutY(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(100, child2.getLayoutBounds().getHeight(), 1e-100);

        vbox.getChildren().remove(child2);
        assertEquals(100, vbox.prefWidth(-1), 1e-100);
        assertEquals(100, vbox.prefHeight(-1), 1e-100);
    }
}

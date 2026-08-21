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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.ParentShim;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnchorPaneTest {

    private static final double EPSILON = 0.000000001;

    private Stage stage;

    @AfterEach
    public void teardown() {
        if (stage != null) {
            stage.close();
        }
    }

    @Test
    public void testNoAnchorsSet() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(100,200, 300,400, 500,600);
        child.relocate(10, 20); // should honor position if no anchors set
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(310, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(420, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(300, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(410, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(310, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(400, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(300, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(410, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(310, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(400, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(310, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(420, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(300, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(310, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(420, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(310, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(420, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(130, anchorpane.minWidth(-1), 1e-100);
        assertEquals(400, anchorpane.minHeight(-1), 1e-100); // Not restricted, will be always at pref. size
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(130, anchorpane.minWidth(-1), 1e-100);
        assertEquals(430, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(130, anchorpane.minWidth(-1), 1e-100);
        assertEquals(430, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(330, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
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
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(330, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
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
        ParentShim.getChildren(anchorpane).add(child);

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
        ParentShim.getChildren(anchorpane).add(child);

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
        ParentShim.getChildren(anchorpane).add(child);

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

        ParentShim.getChildren(anchorpane).addAll(biased, rect);

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

        ParentShim.getChildren(anchorpane).addAll(biased, rect);

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
        ParentShim.getChildren(anchorpane).add(resizable);

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

        ParentShim.getChildren(internalAnchorpane).add(biased);
        ParentShim.getChildren(anchorpane).add(internalAnchorpane);

        anchorpane.resize(500, 500);
        anchorpane.layout();

        assertEquals(30, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(256, anchorpane.prefHeight(-1), 1e-100);
        assertEquals(30, internalAnchorpane.prefWidth(-1), 1e-100);
        assertEquals(256, internalAnchorpane.prefHeight(-1), 1e-100);
    }

    @Test
    public void testTopAnchoredMinSizeOverridden() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(300, 400, 100, 100, 500, 600);
        anchorpane.setTopAnchor(child, 10.0);
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(300, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(410, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
        assertEquals(300, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(410, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(10, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test
    public void testBottomAnchoredMinSizeOverridden() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(300,400, 100,100, 500,600);
        anchorpane.setBottomAnchor(child, 10.0);
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(300, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(410, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
        assertEquals(300, anchorpane.prefWidth(-1), 1e-100);
        assertEquals(410, anchorpane.prefHeight(-1), 1e-100);

        anchorpane.autosize();
        anchorpane.layout();
        assertEquals(0, child.getLayoutX(), 1e-100);
        assertEquals(0, child.getLayoutY(), 1e-100);
        assertEquals(300, child.getWidth(), 1e-100);
        assertEquals(400, child.getHeight(), 1e-100);
    }

    @Test public void testLeftAnchoredMinSizeOverridden() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(300,400, 100,100, 500,600);
        anchorpane.setLeftAnchor(child, 10.0);
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(310, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(400, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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

    @Test public void testRightAnchoredMinSizeOverridden() {
        AnchorPane anchorpane = new AnchorPane();
        MockResizable child = new MockResizable(300,400, 100,100, 500,600);
        anchorpane.setRightAnchor(child, 10.0);
        ParentShim.getChildren(anchorpane).add(child);

        assertEquals(310, anchorpane.minWidth(-1), 1e-100); // Not restricted, at pref. width
        assertEquals(400, anchorpane.minHeight(-1), 1e-100); // Not restricted, at pref. height
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

    /**
     * Tests the {@link Insets} snapping of the {@link AnchorPane} with different scales.
     *
     * @param scale the scale which is used as render scale on the {@link Stage}
     * @see <a href="https://bugs.openjdk.org/browse/JDK-8295078">JDK-8295078</a>
     */
    @ValueSource(doubles = { 1.0, 1.25, 1.5, 1.75, 2.0 })
    @ParameterizedTest
    void testAnchorPaneInsetsSnappingWithDifferentScales(double scale) {
        double padding = 9.6;

        StackPane child = new StackPane();
        AnchorPane anchorPane = new AnchorPane(child);
        anchorPane.setStyle("-fx-padding: " + padding + "px;");

        AnchorPane.setTopAnchor(child, 0d);
        AnchorPane.setLeftAnchor(child, 0d);
        AnchorPane.setBottomAnchor(child, 0d);
        AnchorPane.setRightAnchor(child, 0d);

        DoubleProperty renderScaleProperty = new SimpleDoubleProperty(scale);

        stage = new Stage();
        stage.renderScaleXProperty().bind(renderScaleProperty);
        stage.renderScaleYProperty().bind(renderScaleProperty);

        int widthHeight = 500;
        Scene scene = new Scene(anchorPane, widthHeight, widthHeight);
        stage.setScene(scene);
        stage.show();

        double areaWidth = anchorPane.snapSpaceX(anchorPane.getWidth());
        double areaHeight = anchorPane.snapSpaceY(anchorPane.getHeight());
        double snappedPaddingX = anchorPane.snapSpaceX(padding);
        double snappedPaddingY = anchorPane.snapSpaceY(padding);

        assertEquals(snappedPaddingX, child.getLayoutX());
        assertEquals(snappedPaddingY, child.getLayoutY());

        double expectedMaxX = anchorPane.snapPositionX(areaWidth - snappedPaddingX);
        assertEquals(expectedMaxX, child.getLayoutX() + child.getWidth(), EPSILON);

        double expectedMaxY = anchorPane.snapPositionY(areaHeight - snappedPaddingY);
        assertEquals(expectedMaxY, child.getLayoutY() + child.getHeight(), EPSILON);

        double expectedWidth = anchorPane.snapSpaceX(areaWidth - snappedPaddingX * 2);
        assertEquals(expectedWidth, child.getWidth());

        double expectedHeight = anchorPane.snapSpaceY(areaHeight - snappedPaddingY * 2);
        assertEquals(expectedHeight, child.getHeight());
    }

    /**
     * Tests the anchor snapping of the {@link AnchorPane} with different scales.
     *
     * @param scale the scale which is used as render scale on the {@link Stage}
     * @see <a href="https://bugs.openjdk.org/browse/JDK-8295078">JDK-8295078</a>
     */
    @ValueSource(doubles = { 1.0, 1.25, 1.5, 1.75, 2.0 })
    @ParameterizedTest
    void testAnchorPaneAnchorSnappingWithDifferentScales(double scale) {
        StackPane child = new StackPane();
        AnchorPane anchorPane = new AnchorPane(child);

        double topAnchor = 4d;
        double leftAnchor = 5d;
        double bottomAnchor = 6d;
        double rightAnchor = 7d;

        AnchorPane.setTopAnchor(child, topAnchor);
        AnchorPane.setLeftAnchor(child, leftAnchor);
        AnchorPane.setBottomAnchor(child, bottomAnchor);
        AnchorPane.setRightAnchor(child, rightAnchor);

        DoubleProperty renderScaleProperty = new SimpleDoubleProperty(scale);

        stage = new Stage();
        stage.renderScaleXProperty().bind(renderScaleProperty);
        stage.renderScaleYProperty().bind(renderScaleProperty);

        int widthHeight = 500;
        Scene scene = new Scene(anchorPane, widthHeight, widthHeight);
        stage.setScene(scene);
        stage.show();

        double areaWidth = anchorPane.snapSpaceX(anchorPane.getWidth());
        double areaHeight = anchorPane.snapSpaceY(anchorPane.getHeight());
        double snappedLeftAnchor = anchorPane.snapSpaceX(leftAnchor);
        double snappedRightAnchor = anchorPane.snapSpaceX(rightAnchor);
        double snappedTopAnchor = anchorPane.snapSpaceY(topAnchor);
        double snappedBottomAnchor = anchorPane.snapSpaceY(bottomAnchor);

        assertEquals(snappedLeftAnchor, child.getLayoutX());
        assertEquals(snappedTopAnchor, child.getLayoutY());

        double expectedMaxX = anchorPane.snapPositionX(areaWidth - snappedRightAnchor);
        assertEquals(expectedMaxX, child.getLayoutX() + child.getWidth(), EPSILON);

        double expectedMaxY = anchorPane.snapPositionY(areaHeight - snappedBottomAnchor);
        assertEquals(expectedMaxY, child.getLayoutY() + child.getHeight(), EPSILON);

        double expectedWidth = anchorPane.snapSpaceX(areaWidth - snappedLeftAnchor - snappedRightAnchor);
        assertEquals(expectedWidth, child.getWidth());

        double expectedHeight = anchorPane.snapSpaceY(areaHeight - snappedTopAnchor - snappedBottomAnchor);
        assertEquals(expectedHeight, child.getHeight());
    }

    @Test
    void testDualAnchoredChildSnapsFractionalAllocatedSize() {
        Region child = new Region();
        AnchorPane anchorPane = new AnchorPane(child);
        AnchorPane.setTopAnchor(child, 0.0);
        AnchorPane.setRightAnchor(child, 0.0);
        AnchorPane.setBottomAnchor(child, 0.0);
        AnchorPane.setLeftAnchor(child, 0.0);

        anchorPane.resize(100.4, 50.4);
        anchorPane.layout();

        assertEquals(anchorPane.snapSpaceX(anchorPane.getWidth()), child.getWidth());
        assertEquals(anchorPane.snapSpaceY(anchorPane.getHeight()), child.getHeight());
        assertEquals(0.0, child.getLayoutX());
        assertEquals(0.0, child.getLayoutY());
    }

    @Test
    void testDualAnchoredChildClampsNegativeAllocatedSizeToZero() {
        Region child = new Region();
        AnchorPane anchorPane = new AnchorPane(child);
        AnchorPane.setTopAnchor(child, 6.0);
        AnchorPane.setRightAnchor(child, 5.0);
        AnchorPane.setBottomAnchor(child, 5.0);
        AnchorPane.setLeftAnchor(child, 6.0);

        anchorPane.resize(10, 10);
        anchorPane.layout();

        assertEquals(0.0, child.getWidth());
        assertEquals(0.0, child.getHeight());
    }

    @Test
    void testUnsnappedPreservesFractionalAnchorGeometry() {
        Region child = new Region();
        AnchorPane anchorPane = new AnchorPane(child);
        anchorPane.setSnapToPixel(false);
        AnchorPane.setTopAnchor(child, 0.2);
        AnchorPane.setRightAnchor(child, 0.3);
        AnchorPane.setBottomAnchor(child, 0.4);
        AnchorPane.setLeftAnchor(child, 0.1);

        anchorPane.resize(100.4, 50.4);
        anchorPane.layout();

        assertEquals(0.1, child.getLayoutX(), EPSILON);
        assertEquals(0.2, child.getLayoutY(), EPSILON);
        assertEquals(100.4 - 0.1 - 0.3, child.getWidth(), EPSILON);
        assertEquals(50.4 - 0.2 - 0.4, child.getHeight(), EPSILON);
    }

    @Test
    void testRightBottomAnchoredChildSnapsFractionalPosition() {
        Region child = new Region();
        child.setMinSize(10, 10);
        child.setPrefSize(10, 10);
        child.setMaxSize(10, 10);

        AnchorPane anchorPane = new AnchorPane(child);
        AnchorPane.setRightAnchor(child, 0.0);
        AnchorPane.setBottomAnchor(child, 0.0);

        anchorPane.resize(100.4, 50.4);
        anchorPane.layout();

        double expectedX = anchorPane.snapPositionX(anchorPane.snapSpaceX(anchorPane.getWidth()) - child.getWidth());
        double expectedY = anchorPane.snapPositionY(anchorPane.snapSpaceY(anchorPane.getHeight()) - child.getHeight());

        assertEquals(expectedX, child.getLayoutX());
        assertEquals(expectedY, child.getLayoutY());
    }

    @Test
    void testDualAnchoredMinimumSizeSnapsChildContentSize() {
        Region child = new Region();
        child.setMinSize(0.4, 0.4);
        child.setPrefSize(0.4, 0.4);
        child.setMaxSize(0.4, 0.4);

        AnchorPane anchorPane = new AnchorPane(child);
        AnchorPane.setTopAnchor(child, 0.0);
        AnchorPane.setRightAnchor(child, 0.0);
        AnchorPane.setBottomAnchor(child, 0.0);
        AnchorPane.setLeftAnchor(child, 0.0);

        assertEquals(anchorPane.snapSizeX(0.4), anchorPane.minWidth(-1));
        assertEquals(anchorPane.snapSizeY(0.4), anchorPane.minHeight(-1));
        assertEquals(anchorPane.snapSizeX(0.4), anchorPane.prefWidth(-1));
        assertEquals(anchorPane.snapSizeY(0.4), anchorPane.prefHeight(-1));
    }

    @Test
    void testVerticalBiasUsesStretchedHeightForMeasurementAndLayout() {
        VerticalBiasedRegion child = new VerticalBiasedRegion(10);
        AnchorPane anchorPane = new AnchorPane(child);
        AnchorPane.setTopAnchor(child, 0.0);
        AnchorPane.setBottomAnchor(child, 0.0);
        AnchorPane.setLeftAnchor(child, 0.0);

        assertEquals(10.0, anchorPane.prefWidth(100));
        assertEquals(100.0, child.lastHeight);

        anchorPane.resize(200, 100);
        anchorPane.layout();

        assertEquals(100.0, child.getHeight());
        assertEquals(10.0, child.getWidth());
        assertEquals(child.getHeight(), child.lastHeight);
    }

    @Test
    void testBiasedMeasurementSubtractsSnappedInsetsOnce() {
        VerticalBiasedRegion child = new VerticalBiasedRegion(1000);
        AnchorPane anchorPane = new AnchorPane(child);
        anchorPane.setPadding(new Insets(0.6));
        AnchorPane.setTopAnchor(child, 0.0);
        AnchorPane.setBottomAnchor(child, 0.0);
        AnchorPane.setLeftAnchor(child, 0.0);

        double expectedHeight = anchorPane.snapSpaceY(anchorPane.snapSpaceY(100) - 2 * anchorPane.snapSpaceY(0.6));
        anchorPane.prefWidth(100);
        assertEquals(expectedHeight, child.lastHeight);

        anchorPane.resize(50, 100);
        anchorPane.layout();
        assertEquals(expectedHeight, child.getHeight());
        assertEquals(child.getHeight(), child.lastHeight);
    }

    @Test
    void testHorizontalBiasUsesSameSnappedWidthForMeasurementAndLayout() {
        HorizontalBiasedRegion child = new HorizontalBiasedRegion();
        AnchorPane anchorPane = new AnchorPane(child);
        AnchorPane.setLeftAnchor(child, 0.0);
        AnchorPane.setRightAnchor(child, 0.0);
        AnchorPane.setTopAnchor(child, 0.0);

        anchorPane.resize(100.4, 50);
        anchorPane.layout();

        assertEquals(anchorPane.snapSpaceX(anchorPane.getWidth()), child.getWidth());
        assertEquals(child.getWidth(), child.lastWidth);
    }

    @Test
    void testHorizontalBiasReceivesZeroForNegativeStretchedWidth() {
        HorizontalBiasedRegion child = new HorizontalBiasedRegion();
        AnchorPane anchorPane = new AnchorPane(child);
        AnchorPane.setLeftAnchor(child, 6.0);
        AnchorPane.setRightAnchor(child, 5.0);

        anchorPane.resize(10, 100);
        anchorPane.layout();

        assertEquals(0.0, child.getWidth());
        assertEquals(0.0, child.lastWidth);
    }

    @Test
    void testVerticalBiasReceivesZeroForNegativeStretchedHeight() {
        VerticalBiasedRegion child = new VerticalBiasedRegion(10);
        AnchorPane anchorPane = new AnchorPane(child);
        AnchorPane.setTopAnchor(child, 6.0);
        AnchorPane.setBottomAnchor(child, 5.0);

        anchorPane.resize(100, 10);
        anchorPane.layout();

        assertEquals(0.0, child.getHeight());
        assertEquals(0.0, child.lastHeight);
    }

    @Test
    void testRightBottomAnchoringUsesActualNonResizableSize() {
        Rectangle child = new Rectangle(1.4, 1.4);
        AnchorPane anchorPane = new AnchorPane(child);
        AnchorPane.setRightAnchor(child, 0.0);
        AnchorPane.setBottomAnchor(child, 0.0);

        anchorPane.resize(100, 100);
        anchorPane.layout();

        double expectedX = anchorPane.snapPositionX(anchorPane.snapSpaceX(anchorPane.getWidth()) - child.getWidth());
        double expectedY = anchorPane.snapPositionY(anchorPane.snapSpaceY(anchorPane.getHeight()) - child.getHeight());

        assertEquals(expectedX, child.getLayoutX());
        assertEquals(expectedY, child.getLayoutY());
    }

    @Test
    void testRightBottomAnchoredOversizedChildKeepsNegativePosition() {
        Region child = new Region();
        child.setMinSize(50, 50);
        child.setPrefSize(50, 50);
        child.setMaxSize(50, 50);

        AnchorPane anchorPane = new AnchorPane(child);
        AnchorPane.setRightAnchor(child, 5.0);
        AnchorPane.setBottomAnchor(child, 5.0);

        anchorPane.resize(40, 40);
        anchorPane.layout();

        assertEquals(-15.0, child.getLayoutX());
        assertEquals(-15.0, child.getLayoutY());
        assertEquals(35.0, child.getLayoutX() + child.getWidth());
        assertEquals(35.0, child.getLayoutY() + child.getHeight());
    }

    @Test
    void testFinalMeasurementAndPositionArithmeticIsResnapped() {
        Region child = new Region();
        child.setMinSize(0.1, 10);
        child.setPrefSize(0.1, 10);
        child.setMaxSize(0.1, 10);

        AnchorPane anchorPane = new AnchorPane(child);
        anchorPane.setPadding(new Insets(0.8, 0, 0, 0));
        AnchorPane.setTopAnchor(child, 1.6);
        AnchorPane.setRightAnchor(child, 1.6);

        stage = new Stage();
        stage.renderScaleXProperty().bind(new SimpleDoubleProperty(1.25));
        stage.renderScaleYProperty().bind(new SimpleDoubleProperty(1.25));
        stage.setScene(new Scene(anchorPane, 100, 100));
        stage.show();

        double expectedPrefWidth = anchorPane.snapSpaceX(anchorPane.snapSizeX(0.1) + anchorPane.snapSpaceX(1.6));
        assertEquals(expectedPrefWidth, anchorPane.prefWidth(-1));

        double expectedY = anchorPane.snapPositionY(anchorPane.snapSpaceY(0.8) + anchorPane.snapSpaceY(1.6));
        assertEquals(expectedY, child.getLayoutY());
    }

    @Test
    void testUsesOwningPanePolicyWithDifferentAxisScales() {
        StackPane child = new StackPane();
        child.setSnapToPixel(false);
        AnchorPane anchorPane = new AnchorPane(child);

        double topAnchor = 1.0;
        double leftAnchor = 1.0;
        double bottomAnchor = 1.4;
        double rightAnchor = 1.4;
        AnchorPane.setTopAnchor(child, topAnchor);
        AnchorPane.setLeftAnchor(child, leftAnchor);
        AnchorPane.setBottomAnchor(child, bottomAnchor);
        AnchorPane.setRightAnchor(child, rightAnchor);

        stage = new Stage();
        stage.renderScaleXProperty().bind(new SimpleDoubleProperty(1.25));
        stage.renderScaleYProperty().bind(new SimpleDoubleProperty(1.5));
        stage.setScene(new Scene(anchorPane, 100, 100));
        stage.show();

        double left = anchorPane.snapSpaceX(leftAnchor);
        double right = anchorPane.snapSpaceX(rightAnchor);
        double top = anchorPane.snapSpaceY(topAnchor);
        double bottom = anchorPane.snapSpaceY(bottomAnchor);
        double areaWidth = anchorPane.snapSpaceX(anchorPane.getWidth());
        double areaHeight = anchorPane.snapSpaceY(anchorPane.getHeight());

        assertEquals(anchorPane.snapPositionX(left), child.getLayoutX());
        assertEquals(anchorPane.snapPositionY(top), child.getLayoutY());
        assertEquals(anchorPane.snapSpaceX(areaWidth - left - right), child.getWidth());
        assertEquals(anchorPane.snapSpaceY(areaHeight - top - bottom), child.getHeight());

        assertEquals(leftAnchor, AnchorPane.getLeftAnchor(child));
        assertEquals(topAnchor, AnchorPane.getTopAnchor(child));
    }

    private static final class HorizontalBiasedRegion extends Region {
        private double lastWidth = -1;

        @Override public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override protected double computeMinWidth(double height) {
            return 0;
        }

        @Override protected double computePrefWidth(double height) {
            return 10;
        }

        @Override protected double computeMaxWidth(double height) {
            return 1000;
        }

        @Override protected double computeMinHeight(double width) {
            lastWidth = width;
            return 1;
        }

        @Override protected double computePrefHeight(double width) {
            lastWidth = width;
            return 10;
        }

        @Override protected double computeMaxHeight(double width) {
            lastWidth = width;
            return 1000;
        }
    }

    private static final class VerticalBiasedRegion extends Region {
        private final double preferredHeight;
        private double lastHeight = -1;

        private VerticalBiasedRegion(double preferredHeight) {
            this.preferredHeight = preferredHeight;
        }

        @Override public Orientation getContentBias() {
            return Orientation.VERTICAL;
        }

        @Override protected double computeMinWidth(double height) {
            lastHeight = height;
            return 1;
        }

        @Override protected double computePrefWidth(double height) {
            lastHeight = height;
            return height == -1 ? 100 : 1000 / height;
        }

        @Override protected double computeMaxWidth(double height) {
            lastHeight = height;
            return 1000;
        }

        @Override protected double computeMinHeight(double width) {
            return 0;
        }

        @Override protected double computePrefHeight(double width) {
            return preferredHeight;
        }

        @Override protected double computeMaxHeight(double width) {
            return 1000;
        }
    }
}

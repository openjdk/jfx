/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import org.junit.Test;
/**
 * Tests resizability apis of Node and key subclasses.
 *
 */
public class ResizabilityTest {

    // test isResizable on key base classes

    @Test public void testNodeNotResizable() {
        Node node = new MockNode();
        assertFalse(node.isResizable());
    }

    @Test public void testShapeNotResizable() {
        Rectangle rect = new Rectangle();
        assertFalse(rect.isResizable());
    }

    @Test public void testTextNotResizable() {
        Text text = new Text();
        assertFalse(text.isResizable());
    }

// consider making ImageView resizable!
//    @Test public void testImageViewNotResizable() {
//        ImageView imageview = new ImageView();
//        assertFalse(imageview.isResizable());
//    }

    @Test public void testParentNotResizable() {
        Parent parent = new MockParent();
        assertFalse(parent.isResizable());
    }

    @Test public void testGroupNotResizable() {
        Group group = new Group();
        assertFalse(group.isResizable());
    }

    @Test public void testRegionResizable() {
        Region container = new Region();
        assertTrue(container.isResizable());
    }

    //test min, pref, max sizes on key base classes

    @Test public void testShapeMinPrefMaxWidthEqualLayoutBounds() {
        Rectangle rect = new Rectangle(100,200);
        assertTrue(rect.getLayoutBounds().getWidth() == rect.minWidth(-1) &&
                   rect.getLayoutBounds().getWidth() == rect.prefWidth(-1) &&
                   rect.getLayoutBounds().getWidth() == rect.maxWidth(-1));
    }

    @Test public void testShapeMinPrefMaxHeightEqualLayoutBounds() {
        Rectangle rect = new Rectangle(100,200);
        assertTrue(rect.getLayoutBounds().getHeight() == rect.minHeight(-1) &&
                   rect.getLayoutBounds().getHeight() == rect.prefHeight(-1) &&
                   rect.getLayoutBounds().getHeight() == rect.maxHeight(-1));
    }

    @Test public void testTextMinPrefMaxWidthEqualLayoutBounds() {
        Text text = new Text("something");
        assertTrue(text.getLayoutBounds().getWidth() == text.minWidth(-1) &&
                   text.getLayoutBounds().getWidth() == text.prefWidth(-1) &&
                   text.getLayoutBounds().getWidth() == text.maxWidth(-1));
    }

    @Test public void testTextMinPrefMaxHeightEqualLayoutBounds() {
        Text text = new Text("something");
        assertTrue(text.getLayoutBounds().getHeight() == text.minHeight(-1) &&
                   text.getLayoutBounds().getHeight() == text.prefHeight(-1) &&
                   text.getLayoutBounds().getHeight() == text.maxHeight(-1));
    }

    @Test public void testParentMinPrefMaxWidthAreEqual() {
        Parent parent = new MockParent();
        assertTrue(parent.prefWidth(-1) == parent.minWidth(-1) &&
                   parent.prefWidth(-1) == parent.maxWidth(-1));
    }
    
    @Test public void testParentMinPrefMaxHeightAreEqual() {
        Parent parent = new MockParent();
        assertTrue(parent.prefHeight(-1) == parent.minHeight(-1) &&
                   parent.prefHeight(-1) == parent.maxHeight(-1));
    }

    // test that parent computes pref size based on chldren

    @Test public void testParentPrefWidthQueriesChildPrefWidth() {
        Parent parent = new MockParent();
        assertEquals(110, parent.prefWidth(-1), 1e-100);
    }

    @Test public void testParentPrefHeightQueriesChildPrefHeight() {
        Parent parent = new MockParent();
        assertEquals(220, parent.prefHeight(-1), 1e-100);
    }

    @Test public void testPanePrefWidthQueriesChildPrefWidth() {
        Pane container = new Pane();
        Rectangle r = new Rectangle(-10,-20,100,200);
        container.getChildren().add(r);
        MockResizable tr = new MockResizable(100,200);
        container.getChildren().add(tr);
        assertEquals(110, container.prefWidth(-1), 1e-100);
    }

    @Test public void testPanePrefHeightQueriesChildPrefHeight() {
        Pane container = new Pane();
        Rectangle r = new Rectangle(-10,-20,100,200);
        container.getChildren().add(r);
        MockResizable tr = new MockResizable(100,200);
        container.getChildren().add(tr);
        assertEquals(220, container.prefHeight(-1), 1e-100);
    }

    @Test public void testPanePrefWidthIncludesPadding() {
        Pane container = new Pane();
        container.setPadding(new Insets(10,20,30,40));
        Rectangle r = new Rectangle(-10,-20,100,200);
        container.getChildren().add(r);
        MockResizable tr = new MockResizable(100,200);
        container.getChildren().add(tr);
        assertEquals(170, container.prefWidth(-1), 1e-100);
    }

    @Test public void testPanePrefHeightIncludesPadding() {
        Pane container = new Pane();
        container.setPadding(new Insets(10,20,30,40));
        Rectangle r = new Rectangle(-10,-20,100,200);
        container.getChildren().add(r);
        MockResizable tr = new MockResizable(100,200);
        container.getChildren().add(tr);
        assertEquals(260, container.prefHeight(-1), 1e-100);
    }

    // test relocate, resize, resizeRelocate

    @Test public void testRelocateNonResizable() {
        Rectangle r = new Rectangle(10, 20, 100, 200);
        r.relocate(0,0);

        assertEquals(-10, r.getLayoutX(), 1e-100);
        assertEquals(-20, r.getLayoutY(), 1e-100);
    }

    @Test public void testRelocateResizable() {
        MockResizable resizable = new MockResizable(100,200);
        resizable.relocate(50,50);

        assertEquals(50, resizable.getLayoutX(), 1e-100);
        assertEquals(50, resizable.getLayoutY(), 1e-100);
    }

    @Test public void testResizeNonResizableIsNoOp() {
        Rectangle r = new Rectangle(10, 20, 100, 200);
        r.resize(400,400);

        assertEquals(100, r.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, r.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testResizeResizable() {
        MockResizable resizable = new MockResizable(100,200);
        resizable.resize(30,40);

        assertEquals(30, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(40, resizable.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testAutosize() {
        MockResizable resizable = new MockResizable(100,200);
        resizable.resize(30,40);

        assertEquals(30, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(40, resizable.getLayoutBounds().getHeight(), 1e-100);

        resizable.autosize(); //back to preferred
        assertEquals(100, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, resizable.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testResizeRelocateNonResizable() {
        Rectangle r = new Rectangle(10, 20, 100, 200);
        r.resizeRelocate(0, 0, 400,400);

        assertEquals(-10, r.getLayoutX(), 1e-100);
        assertEquals(-20, r.getLayoutY(), 1e-100);
        assertEquals(100, r.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(200, r.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void testResizeRelocateResizable() {
        MockResizable resizable = new MockResizable(100,200);
        resizable.resizeRelocate(50,50,30,40);

        assertEquals(50, resizable.getLayoutX(), 1e-100);
        assertEquals(50, resizable.getLayoutY(), 1e-100);
        assertEquals(30, resizable.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(40, resizable.getLayoutBounds().getHeight(), 1e-100);
    }


}

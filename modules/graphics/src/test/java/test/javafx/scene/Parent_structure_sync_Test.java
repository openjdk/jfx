/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import com.sun.javafx.scene.NodeHelper;
import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.javafx.tk.Toolkit;
import java.util.List;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.ParentShim;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests to make sure the synchronization of children between a Parent and PGGroup
 * works as expected.
 */
public class Parent_structure_sync_Test {
    private Rectangle r1, r2, r3, r4, r5;
    private Parent parent;
    private NGGroup peer;

    @Before public void setup() {
        parent = new Group();
        r1 = new Rectangle(0, 0, 10, 10);
        r2 = new Rectangle(0, 0, 10, 10);
        r3 = new Rectangle(0, 0, 10, 10);
        r4 = new Rectangle(0, 0, 10, 10);
        r5 = new Rectangle(0, 0, 10, 10);
        peer = NodeHelper.getPeer(parent);

        Scene scene = new Scene(parent);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        sync();
    }

    private void sync() {
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
    }

    @Test public void emptyParentShouldHaveEmptyPGGroup() {
        assertTrue(peer.getChildren().isEmpty());
    }

    @Test public void childAddedToEmptyParentShouldBeInPGGroup() {
        ParentShim.getChildren(parent).add(r1);
        sync();
        assertEquals(1, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
    }

    @Test public void childrenAddedToEmptyParentShouldAllBeInPGGroup() {
        ParentShim.getChildren(parent).addAll(r1, r2, r3);
        sync();
        assertEquals(3, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
    }

    @Test public void addingAChildToTheBack() {
        ParentShim.getChildren(parent).addAll(r2, r3, r4);
        sync();
        ParentShim.getChildren(parent).add(0, r1);
        sync();
        assertEquals(4, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
        assertSame(NodeHelper.getPeer(r4), peer.getChildren().get(3));
    }

    @Test public void addingAChildToTheFront() {
        ParentShim.getChildren(parent).addAll(r1, r2, r3);
        sync();
        ParentShim.getChildren(parent).add(r4);
        sync();
        assertEquals(4, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
        assertSame(NodeHelper.getPeer(r4), peer.getChildren().get(3));
    }

    @Test public void addingAChildToTheCenter() {
        ParentShim.getChildren(parent).addAll(r1, r2, r4);
        sync();
        ParentShim.getChildren(parent).add(2, r3);
        sync();
        assertEquals(4, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
        assertSame(NodeHelper.getPeer(r4), peer.getChildren().get(3));
    }

    @Test public void removingAChildFromTheFront() {
        ParentShim.getChildren(parent).addAll(r1, r2, r3, r4);
        sync();
        ParentShim.getChildren(parent).remove(3);
        sync();
        assertEquals(3, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
    }

    @Test public void removingAChildFromTheBack() {
        ParentShim.getChildren(parent).addAll(r4, r1, r2, r3);
        sync();
        ParentShim.getChildren(parent).remove(0);
        sync();
        assertEquals(3, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
    }

    @Test public void removingAChildFromTheCenter() {
        ParentShim.getChildren(parent).addAll(r1, r2, r4, r3);
        sync();
        ParentShim.getChildren(parent).remove(2);
        sync();
        assertEquals(3, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
    }

    @Test public void movingAChildFromTheBackToTheFront() {
        ParentShim.getChildren(parent).addAll(r4, r1, r2, r3);
        sync();
        r4.toFront();
        sync();
        assertEquals(4, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
        assertSame(NodeHelper.getPeer(r4), peer.getChildren().get(3));
    }

    @Test public void movingAChildFromTheBackToTheFrontAndAddingAChild() {
        ParentShim.getChildren(parent).addAll(r4, r1, r2, r3);
        sync();
        r4.toFront();
        ParentShim.getChildren(parent).add(r5);
        sync();
        assertEquals(5, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
        assertSame(NodeHelper.getPeer(r4), peer.getChildren().get(3));
        assertSame(NodeHelper.getPeer(r5), peer.getChildren().get(4));
    }

    @Test public void movingAChildFromTheBackToTheFrontAndRemovingAChild() {
        ParentShim.getChildren(parent).addAll(r3, r1, r4, r2);
        sync();
        r3.toFront();
        ParentShim.getChildren(parent).remove(1);
        sync();
        assertEquals(3, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
    }

    @Test public void movingAChildFromTheBackToTheFrontAndThenRemovingTheChild() {
        ParentShim.getChildren(parent).addAll(r4, r1, r2, r3);
        sync();
        r4.toFront();
        ParentShim.getChildren(parent).remove(3);
        sync();
        assertEquals(3, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
    }

    @Test public void movingAChildFromTheCenterToTheFront() {
        ParentShim.getChildren(parent).addAll(r1, r2, r4, r3);
        sync();
        r4.toFront();
        sync();
        assertEquals(4, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
        assertSame(NodeHelper.getPeer(r4), peer.getChildren().get(3));
    }

    @Test public void movingAChildFromTheCenterToTheBack() {
        ParentShim.getChildren(parent).addAll(r2, r3, r1, r4);
        sync();
        r1.toBack();
        sync();
        assertEquals(4, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
        assertSame(NodeHelper.getPeer(r4), peer.getChildren().get(3));
    }

    @Test public void movingAChildFromTheCenterToTheFrontAndAddingAChild() {
        ParentShim.getChildren(parent).addAll(r1, r2, r4, r3);
        sync();
        r4.toFront();
        ParentShim.getChildren(parent).add(r5);
        sync();
        assertEquals(5, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
        assertSame(NodeHelper.getPeer(r4), peer.getChildren().get(3));
        assertSame(NodeHelper.getPeer(r5), peer.getChildren().get(4));
    }

    @Test public void movingAChildFromTheCenterToTheFrontAndRemovingAChild() {
        ParentShim.getChildren(parent).addAll(r1, r2, r3, r4);
        sync();
        r3.toFront();
        ParentShim.getChildren(parent).remove(2);
        sync();
        assertEquals(3, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
    }

    @Test public void movingAChildFromTheCenterToTheFrontAndThenRemovingTheChild() {
        ParentShim.getChildren(parent).addAll(r1, r2, r4, r3);
        sync();
        r4.toFront();
        ParentShim.getChildren(parent).remove(3);
        sync();
        assertEquals(3, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
    }

    @Test public void movingAChildFromTheFrontToTheBack() {
        ParentShim.getChildren(parent).addAll(r2, r3, r4, r1);
        sync();
        r1.toBack();
        sync();
        assertEquals(4, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
        assertSame(NodeHelper.getPeer(r4), peer.getChildren().get(3));
    }

    @Test public void movingAChildFromTheFrontToTheBackAndAddingAChild() {
        ParentShim.getChildren(parent).addAll(r2, r3, r4, r1);
        sync();
        r1.toBack();
        ParentShim.getChildren(parent).add(r5);
        sync();
        assertEquals(5, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
        assertSame(NodeHelper.getPeer(r4), peer.getChildren().get(3));
        assertSame(NodeHelper.getPeer(r5), peer.getChildren().get(4));
    }

    @Test public void movingAChildFromTheFrontToTheBackAndRemovingAChild() {
        ParentShim.getChildren(parent).addAll(r2, r3, r4, r1);
        sync();
        r1.toBack();
        ParentShim.getChildren(parent).remove(3);
        sync();
        assertEquals(3, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
    }

    @Test public void movingAChildFromTheFrontToTheBackAndThenRemovingTheChild() {
        ParentShim.getChildren(parent).addAll(r1, r2, r3, r4);
        sync();
        r4.toBack();
        ParentShim.getChildren(parent).remove(0);
        sync();
        assertEquals(3, peer.getChildren().size());
        assertSame(NodeHelper.getPeer(r1), peer.getChildren().get(0));
        assertSame(NodeHelper.getPeer(r2), peer.getChildren().get(1));
        assertSame(NodeHelper.getPeer(r3), peer.getChildren().get(2));
    }

    @Test
    public void validateParentsRemovedList() {
        Group parent2 = new Group();
        ParentShim.getChildren(parent2).addAll(r1, r2);
        ParentShim.getChildren(parent).add(parent2);
        sync();
        ParentShim.getChildren(parent2).remove(r1);
        assertNotNull(ParentShim.test_getRemoved(parent2));
        assertFalse(ParentShim.test_getRemoved(parent2).isEmpty());
        sync();
        assertTrue(ParentShim.test_getRemoved(parent2).isEmpty());
        ParentShim.getChildren(parent2).remove(r2);
        parent.setVisible(false);
        assertFalse(ParentShim.test_getRemoved(parent2).isEmpty());
        sync();
        assertTrue(ParentShim.test_getRemoved(parent2).isEmpty());
    }

    @Test
    public void validateParentsRemovedList2() {
        BorderPane borderPane = new BorderPane(r1);
        sync();
        borderPane.setCenter(null);
        assertNotNull(ParentShim.test_getRemoved(borderPane));
        assertTrue(ParentShim.test_getRemoved(borderPane).isEmpty());
        sync();
        assertTrue(ParentShim.test_getRemoved(borderPane).isEmpty());
    }

    @Test
    public void validateParentsRemovedList3() {
        BorderPane borderPane = new BorderPane(r1);
        parent.getScene().setRoot(borderPane);
        sync();
        borderPane.getScene().setRoot(parent);
        borderPane.setCenter(null);
        assertNotNull(ParentShim.test_getRemoved(borderPane));
        assertTrue(ParentShim.test_getRemoved(borderPane).isEmpty());
        sync();
        assertTrue(ParentShim.test_getRemoved(borderPane).isEmpty());
    }

    @Test
    public void validateParentsViewOrderChildrenList1() {
        Group root = new Group();
        r1.setViewOrder(1);
        ParentShim.getChildren(root).add(r1);
        ParentShim.getChildren(parent).add(root);
        sync();
        assertNotNull(ParentShim.test_getViewOrderChildren(root));
        assertFalse(ParentShim.test_getViewOrderChildren(root).isEmpty());

        r1.setViewOrder(0);
        sync();
        assertTrue(ParentShim.test_getViewOrderChildren(root).isEmpty());

        ParentShim.getChildren(root).add(r2);
        sync();
        assertTrue(ParentShim.test_getViewOrderChildren(root).isEmpty());

        r2.setViewOrder(-1);
        sync();
        assertFalse(ParentShim.test_getViewOrderChildren(root).isEmpty());

        ParentShim.getChildren(root).remove(r1);
        sync();
        assertFalse(ParentShim.test_getViewOrderChildren(root).isEmpty());

        ParentShim.getChildren(root).remove(r2);
        sync();
        assertTrue(ParentShim.test_getViewOrderChildren(root).isEmpty());
    }

    @Test
    public void validateParentsViewOrderChildrenList2() {
        Group root = new Group();
        r1.setViewOrder(0);
        r2.setViewOrder(0);
        r3.setViewOrder(0);
        ParentShim.getChildren(root).addAll(r1, r2, r3);
        ParentShim.getChildren(parent).add(root);
        sync();
        assertNotNull(ParentShim.test_getViewOrderChildren(root));
        assertTrue(ParentShim.test_getViewOrderChildren(root).isEmpty());

        // 0 1 2  C B A
        r2.setViewOrder(1);
        r3.setViewOrder(2);
        sync();
        List<Node> viewOrderChildren = ParentShim.test_getViewOrderChildren(root);
        assertFalse(ParentShim.test_getViewOrderChildren(root).isEmpty());
        assertEquals(viewOrderChildren.get(0), r3);
        assertEquals(viewOrderChildren.get(1), r2);
        assertEquals(viewOrderChildren.get(2), r1);

        // 0 2 2  B C A
        r2.setViewOrder(2);
        sync();
        assertFalse(ParentShim.test_getViewOrderChildren(root).isEmpty());
        assertEquals(viewOrderChildren.get(0), r2);
        assertEquals(viewOrderChildren.get(1), r3);
        assertEquals(viewOrderChildren.get(2), r1);
    }
}

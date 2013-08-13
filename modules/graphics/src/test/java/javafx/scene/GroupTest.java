/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import javafx.collections.ObservableList;
import javafx.scene.shape.Rectangle;

import org.junit.Test;

/**
 * Tests various aspects of Group.
 *
 */
public class GroupTest {
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    // TODO need to remove tests from here that are redundant with StructureTest

    // Things to test:
        // Test that when a Group of nodes is added to a Scene, then the scene
        // variable is set on all the nodes
            // TODO one way to perhaps do this efficiently is by lazily binding
            // the scene to the parent's scene, but this won't work because
            // then the scene cannot be set on the rootmost parent!

        // Test that when a Group of nodes is removed from a Scene, then the
        // scene variable is set back to null

        // Test lookup works from a group

        // Test that changing visibility affects child nodes
        // TODO do we actually change the visible state of a child node? I don't
        // think we should.

        // Test that layoutBounds is something reasonable (need to talk with Amy
        // to remember what reasonable is. I think it is the union of all the
        // layoutBounds of the children, as opposed to the current implementation
        // for Group)

        // Basic tests for boundsInGroup, boundsInLocal

        // Tests for contains() and intersects()
        // especially when there are transforms on the Group

        // Test that a brand new Group is marked as needing layout

        // Test that when nodes are added to the group, if any of the nodes
        // added need layout then the group is marked as needing layout

        // Test that scene variable is set for children if the Group is already
        // part of a Scene

        // Test that scene variable is set when Group is added to a scene

        // Add a test to make sure that if a subclass of Group performs layout,
        // that doing so will cause the bounds of the group to be updated.
        // Really this just means, if a group's needLayout flag is true, and
        // then layout() is called, bounds should be updated appropriately.

    // TODO this exception handling infrastructure should be reconciled
    // with the infrastructure in StructureTest.fx. Perhaps all the
    // exception-throwing tests should be moved into the same file.

    /***************************************************************************
     *                                                                         *
     *                          Testing Group Content                          *
     *                                                                         *
     **************************************************************************/

    // Utility function to check the internal consistency of a group node
    // and its SG counterpart.
    void checkSGConsistency(Group g) {
// TODO disable this because it depends on TestGroup
//        var sgGroup: TestGroup = g.impl_getPGNode() as TestGroup;
//        var sgChildren: java.util.List = sgGroup.getChildren();
//        assertNotNull(sgChildren);
//        assertEquals(sizeof(g.getChildren()), sgChildren.size());
//        for (index in [0..sizeof(g.getChildren())-1]) {
//            assertNotNull(g.getChildren()[index]);
//            assertSame(g.getChildren()[index].impl_getPGNode(), sgChildren.get(index));
//        }
    }


    @Test
    public void testVarArgConstructor() {
        Rectangle r1 = new Rectangle();
        Rectangle r2 = new Rectangle();

        Group g = new Group(r1, r2);
        assertTrue(g.getChildren().contains(r1));
        assertTrue(g.getChildren().contains(r2));
    }

    @Test
    public void testCollectionConstructor() {
        Rectangle r1 = new Rectangle();
        Rectangle r2 = new Rectangle();
        Set s = new HashSet();
        s.add(r1);
        s.add(r2);

        Group g = new Group(s);
        assertTrue(g.getChildren().contains(r1));
        assertTrue(g.getChildren().contains(r2));
    }

    // Test the creation of a cyclic graph
    @Test
    public void testCyclicGraph() {
        Group group1 = new Group();
        Group group2 = new Group();
        assertNull(group1.getParent());
        assertNull(group2.getParent());
        assertEquals(0, group1.getChildren().size());
        assertEquals(0, group2.getChildren().size());
        checkSGConsistency(group1);
        checkSGConsistency(group2);

        group1.getChildren().add(group2);
        assertNull(group1.getParent());
        assertEquals(group1, group2.getParent());
        assertEquals(1, group1.getChildren().size());
        assertEquals(0, group2.getChildren().size());
        checkSGConsistency(group1);
        checkSGConsistency(group2);

        ObservableList<Node> content = group2.getChildren();
        try {
            content.add(group1);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertNull(group1.getParent());
        assertEquals(group1, group2.getParent());
        assertEquals(1, group1.getChildren().size());
        assertEquals(0, group2.getChildren().size());
        checkSGConsistency(group1);
        checkSGConsistency(group2);
    }

    // Add and remove content of a group
    @Test
    public void testAddRemove() {
        Group group = new Group();
        Rectangle node = new Rectangle();
        assertEquals(0, group.getChildren().size());
        assertNull(node.getParent());
        assertNull(group.getParent());
        checkSGConsistency(group);

        group.getChildren().add(node);
        assertEquals(1, group.getChildren().size());
        assertEquals(node, group.getChildren().get(0));
        assertEquals(group, node.getParent());
        assertNull(group.getParent());
        checkSGConsistency(group);

        group.getChildren().remove(node);
        assertEquals(0, group.getChildren().size());
        assertNull(node.getParent());
        assertNull(group.getParent());
        checkSGConsistency(group);
    }

    // Add/remove test with multiple children
    @Test
    public void testAddRemove2() {
        Group group = new Group();
        Rectangle node1 = new Rectangle();
        node1.setX(1);
        Rectangle node2 = new Rectangle();
        node2.setX(2);

        assertEquals(0, group.getChildren().size());
        assertNull(node1.getParent());
        assertNull(node2.getParent());
        assertNull(group.getParent());
        checkSGConsistency(group);

        group.getChildren().add(node1);
        assertEquals(1, group.getChildren().size());
        assertEquals(node1, group.getChildren().get(0));
        assertNull(node2.getParent());
        assertEquals(group, node1.getParent());
        assertNull(group.getParent());
        checkSGConsistency(group);

        group.getChildren().add(node2);
        assertEquals(2, group.getChildren().size());
        assertEquals(node1, group.getChildren().get(0));
        assertEquals(node2, group.getChildren().get(1));
        assertEquals(group, node1.getParent());
        assertEquals(group, node2.getParent());
        assertNull(group.getParent());
        checkSGConsistency(group);

        group.getChildren().remove(node1);
        assertEquals(1, group.getChildren().size());
        assertNull(node1.getParent());
        assertEquals(group, node2.getParent());
        assertEquals(node2, group.getChildren().get(0));
        assertNull(group.getParent());
        checkSGConsistency(group);

        group.getChildren().add(node1);
        assertEquals(2, group.getChildren().size());
        assertEquals(node1, group.getChildren().get(1));
        assertEquals(node2, group.getChildren().get(0));
        assertEquals(group, node1.getParent());
        assertEquals(group, node2.getParent());
        assertNull(group.getParent());
        checkSGConsistency(group);

        Rectangle node3 = new Rectangle();
        node3.setX(3);
        Rectangle node4 = new Rectangle();
        node4.setX(4);
        assertNull(node3.getParent());
        assertNull(node4.getParent());
        assertNull(group.getParent());
        checkSGConsistency(group);

        group.getChildren().add(0, node3);
        group.getChildren().add(node4);
        assertEquals(4, group.getChildren().size());
        assertEquals(node1, group.getChildren().get(2));
        assertEquals(node2, group.getChildren().get(1));
        assertEquals(node3, group.getChildren().get(0));
        assertEquals(node4, group.getChildren().get(3));
        assertEquals(group, node1.getParent());
        assertEquals(group, node2.getParent());
        assertEquals(group, node3.getParent());
        assertEquals(group, node4.getParent());
        assertNull(group.getParent());
        checkSGConsistency(group);

        group.getChildren().clear();
        assertEquals(0, group.getChildren().size());
        assertNull(node1.getParent());
        assertNull(node2.getParent());
        assertNull(node3.getParent());
        assertNull(node4.getParent());
        assertNull(group.getParent());
        checkSGConsistency(group);
    }

    // Initialize two different group's content with the same child
    @Test
    public void testMultiInit() {
        Rectangle node = new Rectangle();
        assertNull(node.getParent());

        Group group1 = new Group();
        group1.getChildren().add(node);
        assertEquals(1, group1.getChildren().size());
        assertEquals(node, group1.getChildren().get(0));
        assertEquals(group1, node.getParent());
        assertNull(group1.getParent());
        checkSGConsistency(group1);

        Group group2 = new Group();
        try {
            group2.getChildren().add(node);
        } catch (Throwable t) {
            assertNull("unexpected exception", t);
        }

        assertEquals(0, group1.getChildren().size());
        assertEquals(1, group2.getChildren().size());
        assertEquals(node, group2.getChildren().get(0));
        assertSame(group2, node.getParent());
        assertNull(group1.getParent());
        assertNull(group2.getParent());
        checkSGConsistency(group1);
        checkSGConsistency(group2);
    }

    // Initialize the same group with two references to the same child instance
    // Then initialize a second group with the same instance
    @Test
    public void testMultiInit2() {
        Rectangle node = new Rectangle();
        assertNull(node.getParent());

        Group group1 = new Group();
        ObservableList<Node> content = group1.getChildren();
        try {
            content.addAll(node, node);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertEquals(0, group1.getChildren().size());
        assertNull(node.getParent());
        assertNull(group1.getParent());
        checkSGConsistency(group1);

        Group group2 = new Group();
        group2.getChildren().add(node);
        assertEquals(1, group2.getChildren().size());
        assertSame(node, group2.getChildren().get(0));
        assertSame(group2, node.getParent());
        assertEquals(0, group1.getChildren().size());
        assertNull(group1.getParent());
        assertNull(group2.getParent());
        checkSGConsistency(group1);
        checkSGConsistency(group2);
    }

    // Insert the same child into two different group's content observableArrayList
    @Test
    public void testMultiAdd() {
        Rectangle node = new Rectangle();
        Group group1 = new Group();
        Group group2 = new Group();
        assertNull(node.getParent());
        assertEquals(0, group1.getChildren().size());
        assertEquals(0, group2.getChildren().size());
        assertNull(group1.getParent());
        assertNull(group2.getParent());
        checkSGConsistency(group1);
        checkSGConsistency(group2);

        group1.getChildren().add(node);
        assertEquals(1, group1.getChildren().size());
        assertSame(node, group1.getChildren().get(0));
        assertSame(group1, node.getParent());
        assertNull(group1.getParent());
        checkSGConsistency(group1);

        try {
            group2.getChildren().add(node);
        } catch(Throwable t) {
            assertNull("unexpected exception", t);
        }

        assertEquals(0, group1.getChildren().size());
        assertEquals(1, group2.getChildren().size());
        assertSame(node, group2.getChildren().get(0));
        assertSame(group2, node.getParent());
        assertNull(group1.getParent());
        assertNull(group2.getParent());
        checkSGConsistency(group1);
        checkSGConsistency(group2);
    }

    // Insert the same child twice into a group's content observableArrayList
    // Then insert it into a second group
    @Test
    public void testMultiAdd2() {
        Rectangle node = new Rectangle();
        Group group1 = new Group();
        Group group2 = new Group();
        assertNull(node.getParent());
        assertEquals(0, group1.getChildren().size());
        assertEquals(0, group2.getChildren().size());
        assertNull(group1.getParent());
        assertNull(group2.getParent());
        checkSGConsistency(group1);
        checkSGConsistency(group2);

        group1.getChildren().add(node);
        assertEquals(1, group1.getChildren().size());
        assertSame(node, group1.getChildren().get(0));
        assertSame(group1, node.getParent());
        assertNull(group1.getParent());
        checkSGConsistency(group1);

        ObservableList<Node> content = group1.getChildren();
        try {
            content.add(node);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertEquals(1, group1.getChildren().size());
        assertSame(node, group1.getChildren().get(0));
        assertSame(group1, node.getParent());
        assertNull(group1.getParent());
        checkSGConsistency(group1);

        try {
            group2.getChildren().add(node);
        } catch (Throwable t) {
            assertNull("unexpected exception", t);
        }

        assertEquals(0, group1.getChildren().size());
        assertEquals(1, group2.getChildren().size());
        assertSame(node, group2.getChildren().get(0));
        assertSame(group2, node.getParent());
        assertNull(group1.getParent());
        assertNull(group2.getParent());
        checkSGConsistency(group1);
        checkSGConsistency(group2);
    }

    // More complex test of inserting children into a group when they are already
    // a child of another group (or the same group more than once)
    @Test
    public void testMultiAdd3() {
        Rectangle node = new Rectangle();
        Group group1 = new Group();
        Group group2 = new Group();
        assertNull(node.getParent());
        assertEquals(0, group1.getChildren().size());
        assertEquals(0, group2.getChildren().size());
        assertNull(group1.getParent());
        assertNull(group2.getParent());
        checkSGConsistency(group1);
        checkSGConsistency(group2);

        // TODO
        // ...
    }

    @Test
    public void testPrefWidthDoesNotIncludeInvisibleChild() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        region.setVisible(false);
        javafx.scene.layout.MockRegion region2 = new javafx.scene.layout.MockRegion(50,75);
        group.getChildren().addAll(region,region2);

        assertEquals(50, group.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightDoesNotIncludeInvisibleChild() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        region.setVisible(false);
        javafx.scene.layout.MockRegion region2 = new javafx.scene.layout.MockRegion(50,75);
        group.getChildren().addAll(region,region2);

        assertEquals(75, group.prefHeight(-1), 0);
    }

    @Test
    public void testPrefWidthWithResizableChild() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        group.getChildren().add(region);

        assertEquals(100, group.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightWithResizableChild() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        group.getChildren().add(region);

        assertEquals(150, group.prefHeight(-1), 0);
    }

    @Test
    public void testPrefWidthIncludesResizableChildScaleX() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        region.setScaleX(2.0);
        group.getChildren().add(region);

        assertEquals(200, group.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightIncludesResizableChildScaleY() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        region.setScaleY(2.0);
        group.getChildren().add(region);

        assertEquals(300, group.prefHeight(-1), 0);
    }

    @Test
    public void testPrefWidthIncludesResizableChildsClip() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        region.setClip(new Rectangle(50,75));
        group.getChildren().add(region);

        assertEquals(50, group.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightIncludesResizableChildsClip() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        region.setClip(new Rectangle(50,75));
        group.getChildren().add(region);

        assertEquals(75, group.prefHeight(-1), 0);
    }

    @Test
    public void testPrefWidthIncludesResizableChildsRotation() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        region.setRotate(90);
        group.getChildren().add(region);

        assertEquals(150, group.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightIncludesResizableChildsRotation() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        region.setRotate(90);
        group.getChildren().add(region);

        assertEquals(100, group.prefHeight(-1), 0);
    }

    @Test
    public void testPrefWidthIncludesResizableChildsTranslateX() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region1 = new javafx.scene.layout.MockRegion(100,150);
        javafx.scene.layout.MockRegion region2 = new javafx.scene.layout.MockRegion(100,150);
        region2.setTranslateX(50);
        group.getChildren().addAll(region1,region2);

        assertEquals(150, group.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightIncludesResizableChildsTranslateY() {
        Group group = new Group();

        javafx.scene.layout.MockRegion region1 = new javafx.scene.layout.MockRegion(100,150);
        javafx.scene.layout.MockRegion region2 = new javafx.scene.layout.MockRegion(100,150);
        region2.setTranslateY(50);
        group.getChildren().addAll(region1,region2);

        assertEquals(200, group.prefHeight(-1), 0);
    }

    @Test
    public void testPrefWidthDoesNotIncludeScaleX() {
        Group group = new Group();
        group.setScaleX(2.0);

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        group.getChildren().add(region);

        assertEquals(100, group.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightDoesNotIncludeScaleY() {
        Group group = new Group();
        group.setScaleY(2.0);

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        group.getChildren().add(region);

        assertEquals(150, group.prefHeight(-1), 0);
    }

    @Test
    public void testPrefWidthDoesNotIncludeClip() {
        Group group = new Group();
        group.setClip(new Rectangle(50,75));

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        group.getChildren().add(region);

        assertEquals(100, group.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightDoesNotIncludeClip() {
        Group group = new Group();
        group.setClip(new Rectangle(50,75));

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        group.getChildren().add(region);

        assertEquals(150, group.prefHeight(-1), 0);
    }

    @Test
    public void testPrefWidthDoesNotIncludeRotation() {
        Group group = new Group();
        group.setRotate(45);

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        group.getChildren().add(region);

        assertEquals(100, group.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightDoesNotIncludeRotation() {
        Group group = new Group();
        group.setRotate(45);

        javafx.scene.layout.MockRegion region = new javafx.scene.layout.MockRegion(100,150);
        group.getChildren().add(region);

        assertEquals(150, group.prefHeight(-1), 0);
    }

    @Test
    public void testPrefWidthDoesNotIncludeTranslateX() {
        Group group = new Group();
        group.setTranslateX(50);

        javafx.scene.layout.MockRegion region1 = new javafx.scene.layout.MockRegion(100,150);
        javafx.scene.layout.MockRegion region2 = new javafx.scene.layout.MockRegion(100,150);
        group.getChildren().addAll(region1,region2);

        assertEquals(100, group.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightDoesNotIncludeTranslateY() {
        Group group = new Group();
        group.setTranslateY(50);

        javafx.scene.layout.MockRegion region1 = new javafx.scene.layout.MockRegion(100,150);
        javafx.scene.layout.MockRegion region2 = new javafx.scene.layout.MockRegion(100,150);
        group.getChildren().addAll(region1,region2);

        assertEquals(150, group.prefHeight(-1), 0);
    }

    @Test
    public void testNonResizableChildDoesNotTriggerLayout() {
        Group group = new Group();
        Rectangle rect = new Rectangle(50,50);
        group.getChildren().add(rect);

        group.layout(); // clear dirty flag

        rect.setWidth(100);
        assertFalse(group.isNeedsLayout());
    }

    @Test
    public void testMinMaxSizesCorrespondToPreferred() {
        Group group = new Group();
        Rectangle rect = new Rectangle(50,50);
        group.getChildren().add(rect);

        assertEquals(50, group.minWidth(-1), 1e-100);
        assertEquals(50, group.minHeight(-1), 1e-100);
        assertEquals(50, group.maxWidth(-1), 1e-100);
        assertEquals(50, group.maxHeight(-1), 1e-100);

        rect.setWidth(100);

        assertEquals(100, group.minWidth(-1), 1e-100);
        assertEquals(50, group.minHeight(-1), 1e-100);
        assertEquals(100, group.maxWidth(-1), 1e-100);
        assertEquals(50, group.maxHeight(-1), 1e-100);

        rect.setHeight(200);

        assertEquals(100, group.minWidth(-1), 1e-100);
        assertEquals(200, group.minHeight(-1), 1e-100);
        assertEquals(100, group.maxWidth(-1), 1e-100);
        assertEquals(200, group.maxHeight(-1), 1e-100);
    }
}

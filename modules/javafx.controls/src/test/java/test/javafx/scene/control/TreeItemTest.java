/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Jonathan
 */
public class TreeItemTest {

    private static TreeItem root;
    private static TreeItem rootChild1;
    private static TreeItem rootChild2;
    private static TreeItem rootChild3;
    private static TreeItem rootChild4;

    @BeforeClass public static void setUp() {
        root = new TreeItem("Root");

        rootChild1 = new TreeItem("Root Child 1");
        rootChild2 = new TreeItem("Root Child 2");
        rootChild3 = new TreeItem("Root Child 3");
        rootChild4 = new TreeItem("Root Child 4");

        root.getChildren().setAll(rootChild1, rootChild2, rootChild3, rootChild4);
    }

    @Test public void testUnattachedTreeItem() {
        final TreeItem node = new TreeItem("Node");
        assertNull(node.getGraphic());
        assertEquals("Node", node.getValue());
        assertNull(node.getParent());
        assertTrue(node.isLeaf());
    }

    @Test public void testTreeItemWithChild() {
        final TreeItem root = new TreeItem("Node");
        final TreeItem node = new TreeItem("Node");
        root.getChildren().setAll(node);

        assertNull(root.getParent());
        assertNotNull(node.getParent());
        assertFalse(root.isLeaf());
        assertTrue(node.isLeaf());
        assertEquals(1, root.getChildren().size());
        assertEquals(0, node.getChildren().size());
        assertEquals(node, root.getChildren().get(0));
    }

    @Test public void testGetSiblingsWithNoSiblings() {
        assertNull(root.nextSibling());
        assertNull(root.previousSibling());

        assertNull(root.nextSibling(null));
        assertNull(root.previousSibling(null));

        assertNull(root.nextSibling(root));
        assertNull(root.previousSibling(root));
    }

    @Test public void testGetSiblingsOfRootsChildren() {
        assertNull(rootChild1.previousSibling());
        assertEquals(rootChild2, rootChild1.nextSibling());

        assertEquals(rootChild1, rootChild2.previousSibling());
        assertEquals(rootChild3, rootChild2.nextSibling());

        assertEquals(rootChild2, rootChild3.previousSibling());
        assertEquals(rootChild4, rootChild3.nextSibling());

        assertEquals(rootChild3, rootChild4.previousSibling());
        assertNull(rootChild4.nextSibling());
    }

    @Test public void testGetSiblingsFromNullSibling() {
        assertNull(rootChild2.previousSibling(null));
        assertNull(rootChild2.nextSibling(null));
    }

    @Test public void testGetSiblingsFromSelf() {
        assertEquals(rootChild1, rootChild2.previousSibling(rootChild2));
        assertEquals(rootChild3, rootChild2.nextSibling(rootChild2));
    }

    @Test public void testGetSiblingsFromGivenSibling() {
        assertNull(rootChild1.previousSibling(rootChild1));
        assertEquals(rootChild1, rootChild1.previousSibling(rootChild2));
        assertEquals(rootChild2, rootChild1.previousSibling(rootChild3));
        assertEquals(rootChild3, rootChild1.previousSibling(rootChild4));

        assertNull(rootChild4.nextSibling(rootChild4));
        assertEquals(rootChild2, rootChild4.nextSibling(rootChild1));
        assertEquals(rootChild3, rootChild4.nextSibling(rootChild2));
        assertEquals(rootChild4, rootChild4.nextSibling(rootChild3));
    }

    @Test public void testSetEmptyChildren() {
        final TreeItem root = new TreeItem("Node");
        assertTrue(root.isLeaf());
        root.getChildren().setAll();
        assertTrue(root.isLeaf());
    }

    @Test public void testSetChildren() {
        final TreeItem root = new TreeItem("Node");
        final TreeItem child = new TreeItem("child");

        assertTrue(root.isLeaf());
        root.getChildren().setAll(child);
        assertFalse(root.isLeaf());
        assertEquals(child, root.getChildren().get(0));
    }

    @Test public void testOverwriteChildrenWithSet() {
        final TreeItem root = new TreeItem("Node");
        final TreeItem child1 = new TreeItem("child1");
        final TreeItem child2 = new TreeItem("child2");

        root.getChildren().setAll(child1);
        assertEquals(child1, root.getChildren().get(0));

        root.getChildren().setAll(child2);
        assertEquals(child2, root.getChildren().get(0));
        assertTrue(root.getChildren().size() == 1);
    }

    @Test public void testAddChildren() {
        final TreeItem root = new TreeItem("Node");
        final TreeItem child = new TreeItem("child");

        assertTrue(root.isLeaf());
        root.getChildren().addAll(child);
        assertFalse(root.isLeaf());
        assertEquals(child, root.getChildren().get(0));
    }

    @Test public void testAddSiblingsWithAdd() {
        final TreeItem root = new TreeItem("Node");
        final TreeItem child1 = new TreeItem("child1");
        final TreeItem child2 = new TreeItem("child2");

        root.getChildren().addAll(child1);
        assertEquals(child1, root.getChildren().get(0));

        root.getChildren().addAll(child2);
        assertEquals(child2, root.getChildren().get(1));
        assertTrue(root.getChildren().size() == 2);
    }

    @Test public void testRemoveChild() {
        final TreeItem root = new TreeItem("Node");
        final TreeItem child1 = new TreeItem("child1");
        final TreeItem child2 = new TreeItem("child2");
        root.getChildren().addAll(child1, child2);
        assertTrue(root.getChildren().size() == 2);

//        root.removeChildren();
//        assertTrue(root.getChildren().size() == 2);

        root.getChildren().remove(child2);
        assertTrue(root.getChildren().size() == 1);
        assertEquals(child1, root.getChildren().get(0));
    }

    @SuppressWarnings("deprecation")
    @Test public void ensureRootNodeHas0Level() {
        final TreeItem root = new TreeItem("Node");
        assertEquals(0, TreeView.getNodeLevel(root));
    }

    @SuppressWarnings("deprecation")
    @Test public void ensureChildOfRootNodeHas1Level() {
        final TreeItem root = new TreeItem("Node");
        final TreeItem child1 = new TreeItem("child1");
        root.getChildren().addAll(child1);
        assertEquals(1, TreeView.getNodeLevel(child1));
    }

    @SuppressWarnings("deprecation")
    @Test public void ensureGrandchildOfRootNodeHas2Level() {
        final TreeItem root = new TreeItem("Node");
        final TreeItem child1 = new TreeItem("child1");
        final TreeItem grandchild1 = new TreeItem("grandchild1");
        root.getChildren().addAll(child1);
        child1.getChildren().addAll(grandchild1);
        assertEquals(2, TreeView.getNodeLevel(grandchild1));
    }

    @SuppressWarnings("deprecation")
    @Test public void detachNodeFromParent_observeThatLevelDecreases() {
        final TreeItem root = new TreeItem("Node");
        final TreeItem child1 = new TreeItem("child1");
        final TreeItem grandchild1 = new TreeItem("grandchild1");
        root.getChildren().addAll(child1);
        child1.getChildren().addAll(grandchild1);

        // detach child1 from the root node, thus decreasing the level of itself
        // and all children
        root.getChildren().clear();
        assertNull(child1.getParent());
        assertEquals(0, TreeView.getNodeLevel(child1));
        assertEquals(1, TreeView.getNodeLevel(grandchild1));
    }
}

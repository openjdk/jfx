/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.ObservableList;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.AbstractNode;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.javafx.sg.prism.NGNode;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.NodeShim;
import javafx.scene.Parent;
import javafx.scene.ParentShim;
import javafx.scene.Scene;
import test.com.sun.javafx.scene.StubNodeHelper;
import test.com.sun.javafx.scene.StubParentHelper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Tests structural aspects of scene graph manipulation. See RT-4095.
 *
 * The following notation is used in test names to indicate various
 * relationships:
 *   CL = clip child
 *   G  = Group child
 *   S  = Scene child
 *
 * Several relationships are checked in each test, typically in
 * the following order:
 *
 * parent's clip
 * parent's children (for Group and Region)
 * relationships of second parent (if any)
 * child's clipParent
 * child's parent
 * child's scene
 */

public class StructureTest {
    // TODO:
    //  - various nasty observableArrayList updates to Group.content and Scene.content.
    //  - various bind expressions.

    //-----------------------
    // Setup and teardown. //
    //-----------------------

    //--------------------
    // Helper Functions //
    //--------------------

    int occurs(Node child, ObservableList<Node> content) {
        int count = 0;
        if (content != null) {
            for (Node node : content) {
                if (node == child) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Checks whether child occurs exactly once in the content observableArrayList.
     */
    boolean occursOnce(Node child, ObservableList<Node> content) {
        return 1 == occurs(child, content);
    }

    /**
     * Checks whether child does not occur in the content observableArrayList.
     */
    boolean occursZero(Node child, ObservableList<Node> content) {
        return 0 == occurs(child, content);
    }

    /**
     * Checks whether the child node appears exactly once in the
     * Group's content observableArrayList.
     */
    boolean isChild(Node child, Group group) {
        return occursOnce(child, ParentShim.getChildren(group));
    }

    /**
     * Checks whether the child node is the root of the Scene.
     */
    boolean isRoot(Parent root, Scene scene) {
        return root == scene.getRoot();
    }

    /**
     * Checks whether the child node does not appear in the
     * Group's content observableArrayList.
     *
     * We use assertTrue(notChild(child, parent)) instead of
     * assertFalse(isChild(child, parent)) because we want
     * to catch cases where child occurs more than once in
     * the parent's content observableArrayList.
     */
    boolean notChild(Node child, Group group) {
        return occursZero(child, ParentShim.getChildren(group));
    }

    /**
     * Checks whether the child node does not appear as the root of the
     * Scene.
     */
    boolean notRoot(Parent root, Scene scene) {
        return root != scene.getRoot();
    }

    //-----------------------------------
    // Simple Structural Relationships //
    //-----------------------------------

    @Test
    public void testOrphan() {
        Node n = new StubNode();

        assertNull(NodeShim.getClipParent(n), "clipParent is null");
        assertNull(n.getParent(), "parent is null");
        assertNull(n.getScene(), "scene is null");
    }

    @Test
    public void testSimpleCL() {
        StubNode parent = new StubNode();
        StubNode child = new StubNode();
        parent.setClip(child);

        assertSame(child, parent.getClip(), "parent.clip is child");
        assertSame(parent, NodeShim.getClipParent(child), "child.clipParent is parent");
        assertNull(child.getParent(), "child.parent is null");
        assertNull(child.getScene(), "scene is null");
    }

    @Test
    public void testSimpleG() {
        StubNode child = new StubNode();
        Group group = new Group(child);

        assertNull(group.getClip(), "group.clip is null");
        assertTrue(isChild(child, group), "isChild of group");
        assertNull(NodeShim.getClipParent(child), "child.clipParent is null");
        assertSame(group, child.getParent(), "child.parent is parent");
        assertNull(child.getScene(), "child.getScene() is null");
    }

    @Test
    public void testSimpleS() {
        StubParent root = new StubParent();
        Scene scene = new Scene(root);

        assertTrue(isRoot(root, scene), "isChild of scene");
        assertNull(NodeShim.getClipParent(root), "child.clipParent is null");
        assertSame(scene, root.getScene(), "child.getScene() is scene");
    }

    @Test
    public void testSceneInsertGroup1() {
        StubNode child = new StubNode();
        Group group = new Group();
        Scene scene = new Scene(group);
        ParentShim.getChildren(group).add(child);

        assertSame(scene, group.getScene(), "group.getScene() is scene");
        assertSame(scene, child.getScene(), "child.getScene() is scene");
    }

    @Test
    public void testSceneInsertGroup2() {
        StubNode child = new StubNode();
        Group group = new Group();
        Scene scene = new Scene(group);
        ParentShim.getChildren(group).add(child);


        assertSame(scene, group.getScene(), "group.getScene() is scene");
        assertSame(scene, child.getScene(), "child.getScene() is scene");
    }

    @Test
    public void testUnparentCL() {
        StubNode child = new StubNode();
        StubNode parent = new StubNode();
        parent.setClip(child);
        parent.setClip(null);

        assertNull(parent.getClip(), "parent.clip is null");
        assertNull(NodeShim.getClipParent(child), "child.clipParent is null");
    }

    @Test
    public void testUnparentG() {
        StubNode child = new StubNode();
        Group parent = new Group(child);

        ParentShim.getChildren(parent).remove(child);


        assertEquals(0, ParentShim.getChildren(parent).size(), "parent.content is zero size");
        assertNull(child.getParent(), "child.parent is null");
    }

    //----------------------------------
    // Illegal Structure Change Tests //
    //----------------------------------

    // Test attempts to switch from one part of the scene graph to another.
    // This is the cross product: {CL,CU,G,S}x{CL,CU,G,S} so there
    // are sixteen cases.

    @Test
    public void testSwitchCLCL() {
        StubNode child = new StubNode();
        StubNode p1 = new StubNode();
        p1.setClip(child);
        StubNode p2 = new StubNode();
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                p2.setClip(child);
            } catch (final IllegalArgumentException e) {
                assertSame(child, p1.getClip(), "p1.clip is child");
                assertNull(p2.getClip(), "p2.clip is null");
                assertSame(p1, NodeShim.getClipParent(child),
                           "child.clipParent is p1");
                assertNull(child.getParent(), "child.parent is null");
                assertNull(child.getScene(), "child.getScene() is null");
                throw e;
            }
        });
    }

    @Test
    public void testSwitchCLG() {
        StubNode child = new StubNode();
        StubNode p1 = new StubNode();
        p1.setClip(child);
        Group p2 = new Group();
        ObservableList<Node> content = ParentShim.getChildren(p2);
        try {
            content.add(child);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertSame(child, p1.getClip(), "p1.clip is child");
        assertNull(p2.getClip(), "p2.clip is null");
        assertTrue(notChild(child, p2), "notChild of p2");
        assertSame(p1, NodeShim.getClipParent(child), "child.clipParent is p1");
        assertNull(child.getParent(), "child.parent is null");
        assertNull(child.getScene(), "child.getScene() is null");
    }

    @Test
    public void testSwitchCLS() {
        StubParent clipNode = new StubParent();
        StubNode p1 = new StubNode();
        p1.setClip(clipNode);
        try {
            Scene p2 = new Scene(clipNode);
            fail("IllegalArgument should have been thrown.");
        } catch (Throwable t) {
            //expected
        }
        assertSame(clipNode, p1.getClip(), "p1.clip is child");
        assertSame(p1, NodeShim.getClipParent(clipNode), "child.clipParent is p1");
        assertNull(clipNode.getParent(), "child.parent is null");
        assertNull(clipNode.getScene(), "child.getScene() is null");
    }

    @Test
    public void testSwitchGCL() {
        StubNode child = new StubNode();
        Group p1 = new Group(child);
        StubNode p2 = new StubNode();
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                p2.setClip(child);
            } catch (final IllegalArgumentException e) {
                assertNull(p1.getClip(), "p1.clip is null");
                assertTrue(isChild(child, p1), "isChild of p1");
                assertNull(p2.getClip(), "p2.clip is null");
                assertNull(NodeShim.getClipParent(child), "child.clipParent is null");
                assertSame(p1, child.getParent(), "child.parent is p1");
                assertNull(child.getScene(), "child.getScene() is null");
                throw e;
            }
        });
    }

// TODO XXX TEMPORARY STOPGAP POLICY RT-4095 -- TEST DISABLED

//    @Test
//    public void testSwitchGG() {
//        var child = new StubNode();
//        var p1 = Group { content: [ child ] };
//        setHandler();
//        var p2 = Group { content: [ child ] };
//
//        assertTrue(caught instanceof IllegalArgumentException, "caught IllegalArgumentException");
//        assertNull(p1.clip, "p1.clip is null");
//        assertTrue(isChild(child, p1), "isChild of p1");
//        assertNull(p2.clip, "p2.clip is null");
//        assertTrue(notChild(child, p2), "notChild of p2");
//        assertNull(NodeShim.getClipParent(child), "child.clipParent is null");
//        assertSame(p1, child.parent, "child.parent is p1");
//        assertNull(child.getScene(), "child.getScene() is null");
//    }

// TODO XXX TEMPORARY STOPGAP POLICY RT-4095 -- TEST DISABLED

//    @Test
//    public void testSwitchGS() {
//        var child = new StubNode();
//        var p1 = Group { content: [ child ] };
//        setHandler();
//        var p2 = Scene { content: [ child ] };
//
//        assertTrue(caught instanceof IllegalArgumentException, "caught IllegalArgumentException");
//        assertNull(p1.clip, "p1.clip is null");
//        assertTrue(isChild(child, p1), "isChild of p1");
//        assertTrue(notChild(child, p2), "notChild of p2");
//        assertNull(NodeShim.getClipParent(child), "child.clipParent is null");
//        assertSame(p1, child.parent, "child.parent is p1");
//        assertNull(child.getScene(), "child.getScene() is null");
//    }

// TODO XXX TEMPORARY STOPGAP POLICY RT-4095 -- TEST OF STOPGAP POLICY

    @Test
    public void testSwitchGGStopgap() {
        StubNode child = new StubNode();
        Group p1 = new Group(child);
        Group p2 = new Group(child);

        assertTrue(notChild(child, p1), "notChild of p1");
        assertTrue(isChild(child, p2), "isChild of p2");
        assertSame(p2, child.getParent(), "child.parent is p2");
    }

    @Test
    public void testSwitchSCL() {
        StubParent root = new StubParent();
        Scene scene = new Scene(root);
        StubNode p2 = new StubNode();
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                p2.setClip(root);
            } catch (final IllegalArgumentException e) {
                assertTrue(isRoot(root, scene), "isRoot of scene");
                assertNull(p2.getClip(), "p2.clip is null");
                assertNull(NodeShim.getClipParent(root), "root.clipParent is null");
                assertSame(scene, root.getScene(), "root.getScene() is scene");
                throw e;
            }
        });
    }


// TODO XXX TEMPORARY STOPGAP POLICY RT-4095 -- TEST DISABLED

//    @Test
//    public void testSwitchSG() {
//        var child = new StubNode();
//        var p1 = Scene { content: [ child ] };
//        setHandler();
//        var p2 = Group { content: [ child ] };
//
//        assertTrue(caught instanceof IllegalArgumentException, "caught IllegalArgumentException");
//        assertTrue(isChild(child, p1), "isChild of p1");
//        assertNull(p2.clip, "p2.clip is null");
//        assertTrue(notChild(child, p2), "notChild of p2");
//        assertNull(NodeShim.getClipParent(child), "child.clipParent is null");
//        assertSame(p1.impl_root, child.parent, "child.parent is p1.impl_root");
//        assertSame(p1, child.getScene(), "child.getScene() is p1");
//    }

// TODO XXX TEMPORARY STOPGAP POLICY RT-4095 -- TEST DISABLED

//    @Test
//    public void testSwitchSS() {
//        var child = new StubNode();
//        var p1 = Scene { content: [ child ] };
//        setHandler();
//        var p2 = Scene { content: [ child ] };
//
//        assertTrue(isChild(child, p1), "isChild of p1");
//        assertTrue(notChild(child, p2), "notChild of p2");
//        assertNull(NodeShim.getClipParent(child), "child.clipParent is null");
//        assertSame(p1.impl_root, child.parent, "child.parent is p1.impl_root");
//        assertSame(p1, child.getScene(), "child.getScene() is p1");
//    }

    @Test
    public void testGroupInsert() {
        StubNode n0 = new StubNode();
        n0.setId("n0");
        StubNode n1 = new StubNode();
        n1.setId("n1");
        StubNode n2 = new StubNode();
        n2.setId("n2");
        Group g = new Group(n0, n1, n2);

        ObservableList<Node> content = ParentShim.getChildren(g);
        try {
            content.add(n1);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertEquals(3, ParentShim.getChildren(g).size(), "g.content is size 3");
        assertSame(n0, ParentShim.getChildren(g).get(0), "g.content[0] is n0");
        assertSame(n1, ParentShim.getChildren(g).get(1), "g.content[1] is n1");
        assertSame(n2, ParentShim.getChildren(g).get(2), "g.content[2] is n2");

    }


    @Test
    public void testGroupReplace1() {
        StubNode n0 = new StubNode();
        n0.setId("n0");
        StubNode n1 = new StubNode();
        n1.setId("n1");
        StubNode n2 = new StubNode();
        n2.setId("n2");
        Group g = new Group(n0, n1, n2);

        ParentShim.getChildren(g).remove(1);
        ObservableList<Node> n = javafx.collections.FXCollections.<Node>observableArrayList();
        n.addAll(n1,n1);
        ObservableList<Node> content = ParentShim.getChildren(g);
        try {
            content.addAll(1, n);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertEquals(2, ParentShim.getChildren(g).size(), "g.content is size 2");
        assertSame(n0, ParentShim.getChildren(g).get(0), "g.content[0] is n0");
        assertSame(n2, ParentShim.getChildren(g).get(1), "g.content[1] is n2");
    }

    @Test
    public void testGroupReplace2() {
        StubNode n0 = new StubNode();
        n0.setId("n0");
        StubNode n1 = new StubNode();
        n1.setId("n1");
        StubNode n2 = new StubNode();
        n2.setId("n2");
        Group g = new Group(n0, n1, n2);

        try {
            ParentShim.getChildren(g).set(1, n0);
            fail("No exception thrown.");
        } catch (IllegalArgumentException e) {
            //Expected
        }

        assertEquals(3, ParentShim.getChildren(g).size(), "g.content is size 3");
        assertSame(n0, ParentShim.getChildren(g).get(0), "g.content[0] is n0");
        assertSame(n1, ParentShim.getChildren(g).get(1), "g.content[1] is n1");
        assertSame(n2, ParentShim.getChildren(g).get(2), "g.content[2] is n2");
    }

    @Test
    public void testGroupReplace3() {
        StubNode n0 = new StubNode();
        n0.setId("n0");
        StubNode n1 = new StubNode();
        n1.setId("n1");
        StubNode n2 = new StubNode();
        n2.setId("n2");
        Group g = new Group(n0, n1, n2);
        ParentShim.getChildren(g).set(1, n1);
        ParentShim.getChildren(g).set(2, n2);

        assertEquals(3, ParentShim.getChildren(g).size(), "g.content is size 3");
        assertSame(n0, ParentShim.getChildren(g).get(0), "g.content[0] is n0");
        assertSame(n1, ParentShim.getChildren(g).get(1), "g.content[1] is n1");
        assertSame(n2, ParentShim.getChildren(g).get(2), "g.content[2] is n2");
    }

    //---------------------
    // Circularity Tests //
    //---------------------

    // General form is: given an existing relationship of one kind, add
    // another relationship of some kind that would cause a circularity.
    // This is the cross product: {CL,CU,G}x{CL,CU,G}. Also test degenerate
    // circularities where a node is its own clip, its own child.
    // The Scene relationship does not occur here, because a Scene cannot
    // participate in any circular relationship as it has no parent.

    // Test only {CL,G}x{CL,G} for now.

    @Test
    public void testCircularCLCL() {
        StubNode node1 = new StubNode();
        StubNode node2 = new StubNode();
        node2.setClip(node1);
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                node1.setClip(node2);
            } catch (final IllegalArgumentException e) {
                assertNull(node1.getClip(), "node1.clip is null");
                assertSame(node2,
                           NodeShim.getClipParent(node1),
                           "node1.clipParent is node2");
                assertSame(node1, node2.getClip(), "node2.clip is node1");
                assertNull(NodeShim.getClipParent(node2), "node2.clipParent is null");
                throw e;
            }
        });
    }

    @Test
    public void testCircularCLG() {
        StubNode node1 = new StubNode();
        Group node2 = new Group(node1);
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                node1.setClip(node2);
            } catch (final IllegalArgumentException e) {
                assertNull(node1.getClip(), "node1.clip is null");
                assertNull(NodeShim.getClipParent(node1), "node1.clipParent is null");
                assertSame(node2, node1.getParent(), "node1.parent is node2");
                assertNull(node2.getClip(), "node2.clip is null");
                assertNull(NodeShim.getClipParent(node2), "node2.clipParent is null");
                assertTrue(isChild(node1, node2), "node1 is child of node2");
                throw e;
            }
        });
    }

    @Test
    public void testCircularGCL() {
        Group node1 = new Group();
        StubNode node2 = new StubNode();
        node2.setClip(node1);

        ObservableList<Node> content = ParentShim.getChildren(node1);
        try {
            content.add(node2);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertNull(node1.getClip(), "node1.clip is null");
        assertSame(node2, NodeShim.getClipParent(node1), "node1.clipParent is node2");
        assertTrue(notChild(node2, node1), "node2 is not child of node1");
        assertSame(node1, node2.getClip(), "node2.clip is node1");
        assertNull(NodeShim.getClipParent(node2), "node2.clipParent is null");
        assertNull(node2.getParent(), "node2.parent is null");
    }

    @Test
    public void testCircularGG() {
        Group node1 = new Group();
        Group node2 = new Group(node1);

        ObservableList<Node> content = ParentShim.getChildren(node1);
        try {
            content.add(node2);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertSame(node2, node1.getParent(), "node1.parent is node2");
        assertTrue(notChild(node2, node1), "node2 is not a child of node1");
        assertNull(node2.getParent(), "node2.parent is null");
        assertTrue(isChild(node1, node2), "node1 is child of node2");
    }

    @Test
    public void testCircularSelfCL() {
        StubNode node1 = new StubNode();
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                node1.setClip(node1);
            } catch (final IllegalArgumentException e) {
                assertNull(node1.getClip(), "node1.clip is null");
                assertNull(NodeShim.getClipParent(node1), "node1.clipParent is null");
                throw e;
            }
        });
    }

    @Test
    public void testCircularSelfG() {
        Group node1 = new Group();

        ObservableList<Node> content = ParentShim.getChildren(node1);
        try {
            content.add(node1);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertTrue(notChild(node1, node1), "node1 is not a child of itself");
        assertNull(node1.getParent(), "node1.parent is null");
    }

    //------------------------
    // Bound Variable Tests //
    //------------------------

    // Test various cases where a structure variable (Node.clip,
    // Group.content, Scene.content) is initialized to a bind-expression.
    // If the trigger attempts to roll back a change to a variable
    // initialized this way, the attempt will fail and will throw an
    // exception. This will leave the invariant violation in place!
    // We can't do anything about this without language support, so
    // don't test these cases for now.

// FAILS:
//    @Test
//    public void testBindClip() {
//        var c:Node = null;
//        var p1 = StubNode { clip: bind c id: "p1" };
//        var p2 = StubNode { clip: bind c id: "p2" };
//        c = StubNode { id: "c" };
//
//        println("testBindClip");
//        println("p1 = {p1}");
//        println("p2 = {p2}");
//        println("c = {c}");
//        println("p1.clip = {p1.clip}");
//        println("p2.clip = {p2.clip}");
//        println("c.clipParent = {c.getClipParent()}");
//    }

    //------------------
    // Helper Classes //
    //------------------

    //
    // * A stub node that contains as little functionality as possible.
    // *
    public static final class StubNode extends AbstractNode {
        static {
            StubNodeHelper.setStubNodeAccessor(new StubNodeHelper.StubNodeAccessor() {
                @Override
                public NGNode doCreatePeer(Node node) {
                    return ((StubNode) node).doCreatePeer();
                }
                @Override
                public BaseBounds doComputeGeomBounds(Node node,
                        BaseBounds bounds, BaseTransform tx) {
                    return ((StubNode) node).doComputeGeomBounds(bounds, tx);
                }
                @Override
                public boolean doComputeContains(Node node, double localX, double localY) {
                    return ((StubNode) node).doComputeContains(localX, localY);
                }
            });
        }

        {
            // To initialize the class helper at the begining each constructor of this class
            StubNodeHelper.initHelper(this);
        }
        public StubNode() {
            super();
        }

        // * Returning null causes crashes so return a NGGroup.
        private NGNode doCreatePeer() {
            return new NGGroup();
        }

        private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
            return bounds;
        }

        /*
         * Note: This method MUST only be called via its accessor method.
         */
        private boolean doComputeContains(double localX, double localY) {
            // TODO: Missing code.
            return false;
        }
    }

    public static final class StubParent extends Parent {
        static {
            StubParentHelper.setStubParentAccessor(new StubParentHelper.StubParentAccessor() {
                @Override
                public NGNode doCreatePeer(Node node) {
                    return ((StubParent) node).doCreatePeer();
                }

                @Override
                public BaseBounds doComputeGeomBounds(Node node,
                        BaseBounds bounds, BaseTransform tx) {
                    return ((StubParent) node).doComputeGeomBounds(bounds, tx);
                }

                @Override
                public boolean doComputeContains(Node node, double localX, double localY) {
                    return ((StubParent) node).doComputeContains(localX, localY);
                }
            });
        }

        {
            // To initialize the class helper at the begining each constructor of this class
            StubParentHelper.initHelper(this);
        }
        public StubParent() {
            super();
        }

        // * Returning null causes crashes so return a PGGroup.
        private NGNode doCreatePeer() {
            return new NGGroup();
        }

        private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
            return bounds;
        }

        /*
         * Note: This method MUST only be called via its accessor method.
         */
        private boolean doComputeContains(double localX, double localY) {
            // TODO: Missing code.
            return false;
        }
    }

}

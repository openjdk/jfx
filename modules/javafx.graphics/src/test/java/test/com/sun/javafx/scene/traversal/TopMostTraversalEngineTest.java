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

package test.com.sun.javafx.scene.traversal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.ParentShim;
import javafx.scene.shape.Rectangle;
import javafx.scene.traversal.TraversalDirection;
import javafx.scene.traversal.TraversalMethod;
import javafx.scene.traversal.TraversalPolicy;
import org.junit.Before;
import org.junit.Test;
import com.sun.javafx.scene.traversal.ContainerTabOrderShim;
import com.sun.javafx.scene.traversal.OverridableTraversalPolicy;
import com.sun.javafx.scene.traversal.TopMostTraversalEngine;

public class TopMostTraversalEngineTest {
    private TraversalPolicy engine;
    private Group root;

    @Before
    public void setUp() {
        root = new Group();
        engine = new ContainerTabOrderShim();
    }

    @Test
    public void selectFirst() {
        final Node focusableNode = createFocusableNode();
        Group g = new Group(focusableNode, createFocusableNode());
        ParentShim.getChildren(root).add(g);

        assertEquals(focusableNode, engine.selectFirst(root));
    }

    @Test
    public void selectFirstSkipInvisible() {
        final Node n1 = createFocusableDisabledNode();
        final Node n2 = createFocusableNode();
        ParentShim.getChildren(root).addAll(n1, n2);

        assertEquals(n2, engine.selectFirst(root));
    }

    @Test
    public void selectFirstUseParentEngine() {
        Group g = new Group(createFocusableNode());
        g.setTraversalPolicy(new TraversalPolicy() {
            @Override
            public Node select(Parent root, Node owner, TraversalDirection dir) {
                return null;
            }

            @Override
            public Node selectFirst(Parent root) {
                return null;
            }

            @Override
            public Node selectLast(Parent root) {
                return null;
            }
        });
        g.setDisable(true);
        ParentShim.getChildren(root).add(g);

        final Node focusableNode = createFocusableNode();
        g = new Group(createFocusableNode(), focusableNode, createFocusableNode());
        g.setTraversalPolicy(new TraversalPolicy() {
            @Override
            public Node select(Parent root, Node owner, TraversalDirection dir) {
                fail();
                return null;
            }

            @Override
            public Node selectFirst(Parent root) {
                return focusableNode;
            }

            @Override
            public Node selectLast(Parent root) {
                fail();
                return null;
            }
        });

        ParentShim.getChildren(root).add(g);

        assertEquals(focusableNode, engine.selectFirst(root));
    }

    @Test
    public void selectFirstFocusableParent() {
        Group g = new Group(createFocusableNode(), createFocusableNode());
        g.setFocusTraversable(true);
        ParentShim.getChildren(root).add(g);

        assertEquals(g, engine.selectFirst(root));
    }

    @Test
    public void selectFirstTraverseOverride() {
        Group g = new Group(createFocusableNode(), createFocusableNode());
        g.setFocusTraversable(true);
        OverridableTraversalPolicy policy = new OverridableTraversalPolicy();
        policy.setOverriddenFocusTraversability(false);
        g.setTraversalPolicy(policy);

        ParentShim.getChildren(root).add(g);

        assertEquals(ParentShim.getChildren(g).get(0), engine.selectFirst(root));
    }


    @Test
    public void selectLast() {
        final Node focusableNode = createFocusableNode();
        Group g = new Group(createFocusableNode(), focusableNode);
        ParentShim.getChildren(root).add(g);

        assertEquals(focusableNode, engine.selectLast(root));
    }

    @Test
    public void selectLastSkipInvisible() {
        final Node n1 = createFocusableNode();
        final Node n2 = createFocusableDisabledNode();
        ParentShim.getChildren(root).addAll(n1, n2);

        assertEquals(n1, engine.selectFirst(root));
    }

    @Test
    public void selectLastUseParentEngine() {
        final Node focusableNode = createFocusableNode();
        Group g = new Group(createFocusableNode(), focusableNode, createFocusableNode());
        g.setTraversalPolicy(new TraversalPolicy() {
            @Override
            public Node select(Parent root, Node owner, TraversalDirection dir) {
                fail();
                return null;
            }

            @Override
            public Node selectFirst(Parent root) {
                fail();
                return null;
            }

            @Override
            public Node selectLast(Parent root) {
                return focusableNode;
            }
        });

        ParentShim.getChildren(root).add(g);


        g = new Group(createFocusableNode());
        g.setTraversalPolicy(new TraversalPolicy() {
            @Override
            public Node select(Parent root, Node owner, TraversalDirection dir) {
                return null;
            }

            @Override
            public Node selectFirst(Parent root) {
                return null;
            }

            @Override
            public Node selectLast(Parent root) {
                return null;
            }
        });
        g.setDisable(true);
        ParentShim.getChildren(root).add(g);


        assertEquals(focusableNode, engine.selectLast(root));
    }

    @Test
    public void selectLastFocusableParent() {
        final Node focusableNode = createFocusableNode();
        Group g = new Group(createFocusableNode(), focusableNode);
        g.setFocusTraversable(true);
        ParentShim.getChildren(root).add(g);

        assertEquals(focusableNode, engine.selectLast(root));
    }

    @Test
    public void selectLastFocusableParent_2() {
        Group g = new Group(new Rectangle());
        g.setFocusTraversable(true);
        ParentShim.getChildren(root).add(g);

        assertEquals(g, engine.selectLast(root));
    }

    @Test
    public void selectLastTraverseOverride() {
        Group g = new Group();
        g.setFocusTraversable(true);
        OverridableTraversalPolicy policy = new OverridableTraversalPolicy();
        policy.setOverriddenFocusTraversability(false);
        g.setTraversalPolicy(policy);

        Node focusableNode = createFocusableNode();

        ParentShim.getChildren(root).addAll(focusableNode, g);

        assertEquals(focusableNode, engine.selectLast(root));
    }

    @Test
    public void selectNext() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n1, new Rectangle(), n2, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextFromParent() {
        Node ng1 = createFocusableNode();
        Node n1 = new Group(new Rectangle(), createFocusableDisabledNode(), ng1, createFocusableNode());
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n1, new Rectangle(), n2, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(ng1, TopMostTraversalEngine.trav(root, n1, TraversalDirection.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextFromParent_2() {
        Node n1 = new Group(createFocusableDisabledNode(), createFocusableDisabledNode());
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n1, new Rectangle(), n2, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextInParentSibling() {
        Node n1 = createFocusableNode();
        final Node n2 = createFocusableNode();

        ParentShim.getChildren(root).addAll(createFocusableNode(), new Group(new Group(n1, createFocusableDisabledNode(), createFocusableDisabledNode()),
                new Group(createFocusableDisabledNode())), new Group(n2));

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextFocusableParent() {
        Node n1 = createFocusableNode();
        Group g = new Group(createFocusableNode());
        g.setFocusTraversable(true);

        ParentShim.getChildren(root).addAll(new Group(createFocusableNode(), n1, createFocusableDisabledNode(), g));

        assertEquals(g, TopMostTraversalEngine.trav(root, n1, TraversalDirection.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextInOverridenAlgorithm() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();
        Group g = new Group(n1, createFocusableNode(), n2);
        g.setTraversalPolicy(new TraversalPolicy() {
            @Override
            public Node select(Parent root, Node owner, TraversalDirection dir) {
                assertEquals(TraversalDirection.NEXT, dir);
                return n2;
            }

            @Override
            public Node selectFirst(Parent root) {
                fail();
                return null;
            }

            @Override
            public Node selectLast(Parent root) {
                fail();
                return null;
            }
        });

        ParentShim.getChildren(root).add(g);

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.NEXT, TraversalMethod.DEFAULT));
    }


    @Test
    public void selectNextInOverridenAlgorithm_NothingSelected() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();
        Group g = new Group(n1, createFocusableNode(), n2);
        g.setFocusTraversable(true);
        g.setTraversalPolicy(new TraversalPolicy() {
            @Override
            public Node select(Parent root, Node owner, TraversalDirection dir) {
                assertEquals(TraversalDirection.NEXT, dir);
                return null;
            }

            @Override
            public Node selectFirst(Parent root) {
                fail();
                return null;
            }

            @Override
            public Node selectLast(Parent root) {
                fail();
                return null;
            }
        });

        final Node n3 = createFocusableNode();
        ParentShim.getChildren(root).addAll(g, n3);

        assertEquals(n3, TopMostTraversalEngine.trav(root, n1, TraversalDirection.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextInLine() {
        Node n1 = new Group(createFocusableNode(), createFocusableNode());
        n1.setFocusTraversable(true);
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n1, new Rectangle(), n2, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.NEXT_IN_LINE, TraversalMethod.DEFAULT));
    }


    @Test
    public void selectPrevious() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n2, new Rectangle(), n1, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectPreviousFromParent() {
        Node ng1 = createFocusableNode();
        Node n1 = new Group(new Rectangle(), createFocusableDisabledNode(), ng1, createFocusableNode());
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n2, new Rectangle(), n1, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectPreviousFromParent_2() {
        Node n1 = new Group(createFocusableDisabledNode(), createFocusableDisabledNode());
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n2, new Rectangle(), n1, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectPreviousInParentSibling() {
        Node n1 = createFocusableNode();
        final Node n2 = createFocusableNode();

        ParentShim.getChildren(root).addAll(new Group(n2), new Group(createFocusableDisabledNode()),
                new Group(new Group(createFocusableDisabledNode(), n1, createFocusableDisabledNode())),
                 createFocusableNode());

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectPreviousFocusableParentsNode() {
        Node n1 = createFocusableNode();
        final Node n2 = createFocusableNode();
        Group g = new Group(n2);
        g.setFocusTraversable(true);

        ParentShim.getChildren(root).addAll(new Group(createFocusableNode(), g, n1, createFocusableDisabledNode()));

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectPreviousFocusableParent() {
        Node n1 = createFocusableNode();
        final Node n2 = createFocusableNode();
        Group g = new Group(createFocusableDisabledNode(), n2);
        g.setFocusTraversable(true);

        ParentShim.getChildren(root).addAll(new Group(createFocusableNode(), n1, g, createFocusableDisabledNode()));

        assertEquals(g, TopMostTraversalEngine.trav(root, n2, TraversalDirection.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextToLast() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();

        ParentShim.getChildren(root).addAll(new Group(n2), new Group(createFocusableNode(), n1));

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.NEXT, TraversalMethod.DEFAULT));
    }


    @Test
    public void selectPreviousToFirst() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();

        ParentShim.getChildren(root).addAll(new Group(n1, createFocusableNode()), new Group(n2));

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.PREVIOUS, TraversalMethod.DEFAULT));
    }


    @Test
    public void selectPreviousInOverridenAlgorithm() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();
        Group g = new Group(n2, createFocusableNode(), n1);
        g.setTraversalPolicy(new TraversalPolicy() {
            @Override
            public Node select(Parent root, Node owner, TraversalDirection dir) {
                assertEquals(TraversalDirection.PREVIOUS, dir);
                return n2;
            }

            @Override
            public Node selectFirst(Parent root) {
                fail();
                return null;
            }

            @Override
            public Node selectLast(Parent root) {
                fail();
                return null;
            }
        });

        ParentShim.getChildren(root).add(g);

        assertEquals(n2, TopMostTraversalEngine.trav(root, n1, TraversalDirection.PREVIOUS, TraversalMethod.DEFAULT));
    }

    private Node createFocusableNode() {
        Node n =  new Rectangle();
        n.setFocusTraversable(true);
        return n;
    }

    private Node createFocusableDisabledNode() {
        Node n = createFocusableNode();
        n.setDisable(true);
        return n;
    }
}

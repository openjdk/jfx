/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.ParentHelper;
import com.sun.javafx.scene.traversal.Algorithm;
import com.sun.javafx.scene.traversal.ContainerTabOrderShim;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.ParentTraversalEngine;
import com.sun.javafx.scene.traversal.TopMostTraversalEngineShim;
import com.sun.javafx.scene.traversal.TraversalContext;
import com.sun.javafx.scene.traversal.TraversalMethod;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.ParentShim;
import javafx.scene.shape.Rectangle;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TopMostTraversalEngineTest {
    private TopMostTraversalEngineShim engine;
    private Group root;

    @Before
    public void setUp() {
        root = new Group();
        engine = new TopMostTraversalEngineShim(new ContainerTabOrderShim()) {
            @Override
            protected Parent getRoot() {
                return root;
            }
        };
    }

    @Test
    public void selectFirst() {
        final Node focusableNode = createFocusableNode();
        Group g = new Group(focusableNode, createFocusableNode());
        ParentShim.getChildren(root).add(g);

        assertEquals(focusableNode, engine.selectFirst());
    }

    @Test
    public void selectFirstSkipInvisible() {
        final Node n1 = createFocusableDisabledNode();
        final Node n2 = createFocusableNode();
        ParentShim.getChildren(root).addAll(n1, n2);

        assertEquals(n2, engine.selectFirst());
    }

    @Test
    public void selectFirstUseParentEngine() {
        Group g = new Group(createFocusableNode());
        ParentHelper.setTraversalEngine(g, new ParentTraversalEngine(g, new Algorithm() {
            @Override
            public Node select(Node owner, Direction dir, TraversalContext context) {
                return null;
            }

            @Override
            public Node selectFirst(TraversalContext context) {
                return null;
            }

            @Override
            public Node selectLast(TraversalContext context) {
                return null;
            }
        }));
        g.setDisable(true);
        ParentShim.getChildren(root).add(g);

        final Node focusableNode = createFocusableNode();
        g = new Group(createFocusableNode(), focusableNode, createFocusableNode());
        ParentHelper.setTraversalEngine(g, new ParentTraversalEngine(g, new Algorithm() {
            @Override
            public Node select(Node owner, Direction dir, TraversalContext context) {
                fail();
                return null;
            }

            @Override
            public Node selectFirst(TraversalContext context) {
                return focusableNode;
            }

            @Override
            public Node selectLast(TraversalContext context) {
                fail();
                return null;
            }
        }));

        ParentShim.getChildren(root).add(g);

        assertEquals(focusableNode, engine.selectFirst());
    }

    @Test
    public void selectFirstFocusableParent() {
        Group g = new Group(createFocusableNode(), createFocusableNode());
        g.setFocusTraversable(true);
        ParentShim.getChildren(root).add(g);

        assertEquals(g, engine.selectFirst());
    }

    @Test
    public void selectFirstTraverseOverride() {
        Group g = new Group(createFocusableNode(), createFocusableNode());
        g.setFocusTraversable(true);
        final ParentTraversalEngine pEngine = new ParentTraversalEngine(g);
        pEngine.setOverriddenFocusTraversability(false);
        ParentHelper.setTraversalEngine(g, pEngine);

        ParentShim.getChildren(root).add(g);

        assertEquals(ParentShim.getChildren(g).get(0), engine.selectFirst());
    }


    @Test
    public void selectLast() {
        final Node focusableNode = createFocusableNode();
        Group g = new Group(createFocusableNode(), focusableNode);
        ParentShim.getChildren(root).add(g);

        assertEquals(focusableNode, engine.selectLast());
    }

    @Test
    public void selectLastSkipInvisible() {
        final Node n1 = createFocusableNode();
        final Node n2 = createFocusableDisabledNode();
        ParentShim.getChildren(root).addAll(n1, n2);

        assertEquals(n1, engine.selectFirst());
    }

    @Test
    public void selectLastUseParentEngine() {
        final Node focusableNode = createFocusableNode();
        Group g = new Group(createFocusableNode(), focusableNode, createFocusableNode());
        ParentHelper.setTraversalEngine(g, new ParentTraversalEngine(g, new Algorithm() {
            @Override
            public Node select(Node owner, Direction dir, TraversalContext context) {
                fail();
                return null;
            }

            @Override
            public Node selectFirst(TraversalContext context) {
                fail();
                return null;
            }

            @Override
            public Node selectLast(TraversalContext context) {
                return focusableNode;
            }
        }));

        ParentShim.getChildren(root).add(g);


        g = new Group(createFocusableNode());
        ParentHelper.setTraversalEngine(g, new ParentTraversalEngine(g, new Algorithm() {
            @Override
            public Node select(Node owner, Direction dir, TraversalContext context) {
                return null;
            }

            @Override
            public Node selectFirst(TraversalContext context) {
                return null;
            }

            @Override
            public Node selectLast(TraversalContext context) {
                return null;
            }
        }));
        g.setDisable(true);
        ParentShim.getChildren(root).add(g);


        assertEquals(focusableNode, engine.selectLast());
    }

    @Test
    public void selectLastFocusableParent() {
        final Node focusableNode = createFocusableNode();
        Group g = new Group(createFocusableNode(), focusableNode);
        g.setFocusTraversable(true);
        ParentShim.getChildren(root).add(g);

        assertEquals(focusableNode, engine.selectLast());
    }

    @Test
    public void selectLastFocusableParent_2() {
        Group g = new Group(new Rectangle());
        g.setFocusTraversable(true);
        ParentShim.getChildren(root).add(g);

        assertEquals(g, engine.selectLast());
    }

    @Test
    public void selectLastTraverseOverride() {
        Group g = new Group();
        g.setFocusTraversable(true);
        final ParentTraversalEngine pEngine = new ParentTraversalEngine(g);
        pEngine.setOverriddenFocusTraversability(false);
        ParentHelper.setTraversalEngine(g, pEngine);

        Node focusableNode = createFocusableNode();

        ParentShim.getChildren(root).addAll(focusableNode, g);

        assertEquals(focusableNode, engine.selectLast());
    }

    @Test
    public void selectNext() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n1, new Rectangle(), n2, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, engine.trav(n1, Direction.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextFromParent() {
        Node ng1 = createFocusableNode();
        Node n1 = new Group(new Rectangle(), createFocusableDisabledNode(), ng1, createFocusableNode());
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n1, new Rectangle(), n2, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(ng1, engine.trav(n1, Direction.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextFromParent_2() {
        Node n1 = new Group(createFocusableDisabledNode(), createFocusableDisabledNode());
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n1, new Rectangle(), n2, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, engine.trav(n1, Direction.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextInParentSibling() {
        Node n1 = createFocusableNode();
        final Node n2 = createFocusableNode();

        ParentShim.getChildren(root).addAll(createFocusableNode(), new Group(new Group(n1, createFocusableDisabledNode(), createFocusableDisabledNode()),
                new Group(createFocusableDisabledNode())), new Group(n2));

        assertEquals(n2, engine.trav(n1, Direction.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextFocusableParent() {
        Node n1 = createFocusableNode();
        Group g = new Group(createFocusableNode());
        g.setFocusTraversable(true);

        ParentShim.getChildren(root).addAll(new Group(createFocusableNode(), n1, createFocusableDisabledNode(), g));

        assertEquals(g, engine.trav(n1, Direction.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextInOverridenAlgorithm() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();
        Group g = new Group(n1, createFocusableNode(), n2);
        ParentHelper.setTraversalEngine(g, new ParentTraversalEngine(g, new Algorithm() {
            @Override
            public Node select(Node owner, Direction dir, TraversalContext context) {
                assertEquals(Direction.NEXT, dir);
                return n2;
            }

            @Override
            public Node selectFirst(TraversalContext context) {
                fail();
                return null;
            }

            @Override
            public Node selectLast(TraversalContext context) {
                fail();
                return null;
            }
        }));

        ParentShim.getChildren(root).add(g);

        assertEquals(n2, engine.trav(n1, Direction.NEXT, TraversalMethod.DEFAULT));
    }


    @Test
    public void selectNextInOverridenAlgorithm_NothingSelected() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();
        Group g = new Group(n1, createFocusableNode(), n2);
        g.setFocusTraversable(true);
        ParentHelper.setTraversalEngine(g, new ParentTraversalEngine(g, new Algorithm() {
            @Override
            public Node select(Node owner, Direction dir, TraversalContext context) {
                assertEquals(Direction.NEXT, dir);
                return null;
            }

            @Override
            public Node selectFirst(TraversalContext context) {
                fail();
                return null;
            }

            @Override
            public Node selectLast(TraversalContext context) {
                fail();
                return null;
            }
        }));

        final Node n3 = createFocusableNode();
        ParentShim.getChildren(root).addAll(g, n3);

        assertEquals(n3, engine.trav(n1, Direction.NEXT, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextInLine() {
        Node n1 = new Group(createFocusableNode(), createFocusableNode());
        n1.setFocusTraversable(true);
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n1, new Rectangle(), n2, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, engine.trav(n1, Direction.NEXT_IN_LINE, TraversalMethod.DEFAULT));
    }


    @Test
    public void selectPrevious() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n2, new Rectangle(), n1, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, engine.trav(n1, Direction.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectPreviousFromParent() {
        Node ng1 = createFocusableNode();
        Node n1 = new Group(new Rectangle(), createFocusableDisabledNode(), ng1, createFocusableNode());
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n2, new Rectangle(), n1, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, engine.trav(n1, Direction.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectPreviousFromParent_2() {
        Node n1 = new Group(createFocusableDisabledNode(), createFocusableDisabledNode());
        Node n2 = createFocusableNode();
        Group g = new Group(createFocusableNode(), n2, new Rectangle(), n1, createFocusableNode());

        ParentShim.getChildren(root).addAll(g);

        assertEquals(n2, engine.trav(n1, Direction.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectPreviousInParentSibling() {
        Node n1 = createFocusableNode();
        final Node n2 = createFocusableNode();

        ParentShim.getChildren(root).addAll(new Group(n2), new Group(createFocusableDisabledNode()),
                new Group(new Group(createFocusableDisabledNode(), n1, createFocusableDisabledNode())),
                 createFocusableNode());

        assertEquals(n2, engine.trav(n1, Direction.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectPreviousFocusableParentsNode() {
        Node n1 = createFocusableNode();
        final Node n2 = createFocusableNode();
        Group g = new Group(n2);
        g.setFocusTraversable(true);

        ParentShim.getChildren(root).addAll(new Group(createFocusableNode(), g, n1, createFocusableDisabledNode()));

        assertEquals(n2, engine.trav(n1, Direction.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectPreviousFocusableParent() {
        Node n1 = createFocusableNode();
        final Node n2 = createFocusableNode();
        Group g = new Group(createFocusableDisabledNode(), n2);
        g.setFocusTraversable(true);

        ParentShim.getChildren(root).addAll(new Group(createFocusableNode(), n1, g, createFocusableDisabledNode()));

        assertEquals(g, engine.trav(n2, Direction.PREVIOUS, TraversalMethod.DEFAULT));
    }

    @Test
    public void selectNextToLast() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();

        ParentShim.getChildren(root).addAll(new Group(n2), new Group(createFocusableNode(), n1));

        assertEquals(n2, engine.trav(n1, Direction.NEXT, TraversalMethod.DEFAULT));
    }


    @Test
    public void selectPreviousToFirst() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();

        ParentShim.getChildren(root).addAll(new Group(n1, createFocusableNode()), new Group(n2));

        assertEquals(n2, engine.trav(n1, Direction.PREVIOUS, TraversalMethod.DEFAULT));
    }


    @Test
    public void selectPreviousInOverridenAlgorithm() {
        Node n1 = createFocusableNode();
        Node n2 = createFocusableNode();
        Group g = new Group(n2, createFocusableNode(), n1);
        ParentHelper.setTraversalEngine(g, new ParentTraversalEngine(g, new Algorithm() {
            @Override
            public Node select(Node owner, Direction dir, TraversalContext context) {
                assertEquals(Direction.PREVIOUS, dir);
                return n2;
            }

            @Override
            public Node selectFirst(TraversalContext context) {
                fail();
                return null;
            }

            @Override
            public Node selectLast(TraversalContext context) {
                fail();
                return null;
            }
        }));

        ParentShim.getChildren(root).add(g);

        assertEquals(n2, engine.trav(n1, Direction.PREVIOUS, TraversalMethod.DEFAULT));
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

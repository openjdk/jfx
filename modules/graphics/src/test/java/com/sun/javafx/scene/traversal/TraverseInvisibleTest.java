/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.traversal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests TraversalEngine with invisible nodes, using the default ContainerTabOrder algorithm,
 */
@RunWith(Parameterized.class)
public final class TraverseInvisibleTest {
    private final int fromNumber;
    private final Direction direction;
    private final int invisibleNumber;
    private final int toNumber;

    private Stage stage;
    private Scene scene;
    private Node[] keypadNodes;
    private SceneTraversalEngine traversalEngine;

    /*
    **
    ** Parameters: [fromNumber], [direction], [invisibleNumber], [toNumber]
    ** The Grid looks like :
    **    0 1 2
    **    3 4 5
    **    6 7 8
    */
    @Parameters
    public static Collection data() {
        return Arrays.asList(new Object[][] {
            { 3, Direction.RIGHT, 4, 5},
            { 5, Direction.LEFT, 4, 3},
            { 4, Direction.NEXT, 5, 6},
            { 6, Direction.PREVIOUS, 5, 4},
            { 8, Direction.UP, 5, 2 },
            { 2, Direction.DOWN, 5, 8 }
        });
    }

    public TraverseInvisibleTest(final int fromNumber,
                         final Direction direction,
                         final int invisibleNumber,
                         final int toNumber) {
        this.fromNumber = fromNumber;
        this.direction = direction;
        this.invisibleNumber = invisibleNumber;
        this.toNumber = toNumber;
    }

    @Before
    public void setUp() {
        stage = new Stage();
        scene = new Scene(new Group(), 500, 500);
        stage.setScene(scene);

        traversalEngine = new SceneTraversalEngine(scene);

        keypadNodes = createKeypadNodesInScene(scene, traversalEngine);
        stage.show();
        stage.requestFocus();
    }

    @After
    public void tearDown() {
        stage = null;
        scene = null;
        keypadNodes = null;
        traversalEngine = null;
    }

    @Test
    public void traverseOverInvisible() {
        keypadNodes[fromNumber].requestFocus();
        keypadNodes[invisibleNumber].setVisible(false);
        traversalEngine.trav(keypadNodes[fromNumber], direction);

        assertTrue(keypadNodes[toNumber].isFocused());

        keypadNodes[invisibleNumber - 1].setVisible(true);
    }



    private static Node[] createKeypadNodesInScene(
            final Scene scene,
            final TraversalEngine traversalEngine) {
        final Node[] keypad = new Node[9];

        int index = 0;
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 3; ++column) {
                final Node keyNode = new Rectangle(10 + column * 50,
                                                   10 + row * 50,
                                                   40, 40);
                keyNode.setFocusTraversable(true);

                keypad[index++] = keyNode;
                ((Group)scene.getRoot()).getChildren().add(keyNode);
            }
        }

        return keypad;
    }

    private static final class TraverseListenerImpl
            implements TraverseListener {
        private int callCounter;
        private Node lastNode;

        public int getCallCounter() {
            return callCounter;
        }
        
        public Node getLastNode() {
            return lastNode;
        }

        @Override
        public void onTraverse(final Node node, final Bounds bounds) {
            ++callCounter;
            lastNode = node;
        }
    }
}

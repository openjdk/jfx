/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.SceneTraversalEngine;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.scene.traversal.TraversalMethod;
import com.sun.javafx.scene.traversal.TraverseListener;

import java.util.stream.Stream;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for TraversalEngine with the default ContainerTabOrder algorithm,
 * tests if using the WeightedClosestCorner algorithm have been
 * left in comments.
 */
public final class TraversalTest {
    private Stage stage;
    private Scene scene;
    /**
     * 3x3 keypad.
     * <p>
     * Untransformed keypad:
     * <ul>
     *   <li>1 2 3</li>
     *   <li>4 5 6</li>
     *   <li>7 8 9</li>
     * </ul>
     * <p>
     * Transformed keypad:
     * <ul>
     *   <li>7 4 1</li>
     *   <li>8 5 2</li>
     *   <li>9 6 3</li>
     * </ul>
     */
    private Node[] keypadNodes;
    private SceneTraversalEngine traversalEngine;

    /*
     * Parameters: [fromNumber], [direction], [toNumber], [toNumberTransformed]
     */
    public static Stream<Arguments> data() {
        return Stream.of(
            /* traversal from center */
            Arguments.of( 5, Direction.LEFT, 4, 8 ),
            Arguments.of( 5, Direction.RIGHT, 6, 2 ),
            Arguments.of( 5, Direction.UP, 2, 4 ),
            Arguments.of( 5, Direction.DOWN, 8, 6 ),

            // using WeightedClosestCorner, target varies according to transform
            //Arguments.of( 5, Direction.PREVIOUS, 4, 8 ),
            //Arguments.of( 5, Direction.NEXT, 6, 2 ),

            // using ContainerTabOrder, target is always the same
            Arguments.of( 5, Direction.PREVIOUS, 4, 4 ),
            Arguments.of( 5, Direction.NEXT, 6, 6 ),

            /* traversal from borders (untransformed) */
            Arguments.of( 4, Direction.LEFT, 4, 7 ),
            Arguments.of( 6, Direction.RIGHT, 6, 3 ),
            Arguments.of( 2, Direction.UP, 2, 1 ),
            Arguments.of( 8, Direction.DOWN, 8, 9 ),

            // using WeightedClosestCorner, target varies according to transform
            //Arguments.of( 4, Direction.PREVIOUS, 3, 7 ),
            //Arguments.of( 1, Direction.PREVIOUS, 9, 4 ),
            //Arguments.of( 6, Direction.NEXT, 7, 3 ),
            //Arguments.of( 9, Direction.NEXT, 1, 6 ),

            // using ContainerTabOrder, target always the same
            Arguments.of( 4, Direction.PREVIOUS, 3, 3 ),
            Arguments.of( 1, Direction.PREVIOUS, 9, 9 ),
            Arguments.of( 6, Direction.NEXT, 7, 7 ),
            Arguments.of( 9, Direction.NEXT, 1, 1 ),

            /* traversal from borders (transformed) */
            Arguments.of( 2, Direction.RIGHT, 3, 2 ),
            Arguments.of( 8, Direction.LEFT, 7, 8 ),
            Arguments.of( 4, Direction.UP, 1, 4 ),
            Arguments.of( 6, Direction.DOWN, 9, 6 ),

            // using WeightedClosestCorner, target varies according to transform
            //Arguments.of( 8, Direction.PREVIOUS, 7, 1 ),
            //Arguments.of( 7, Direction.PREVIOUS, 6, 3 ),
            //Arguments.of( 2, Direction.NEXT, 3, 9 ),
            //Arguments.of( 3, Direction.NEXT, 4, 7)}

            // using ContainerTabOrder, target always the same
            Arguments.of( 8, Direction.PREVIOUS, 7, 7 ),
            Arguments.of( 7, Direction.PREVIOUS, 6, 6 ),
            Arguments.of( 2, Direction.NEXT, 3, 3 ),
            Arguments.of( 3, Direction.NEXT, 4, 4)
        );
    }

    @BeforeEach
    public void setUp() {
        stage = new Stage();
        scene = new Scene(new Group(), 500, 500);
        stage.setScene(scene);

        traversalEngine = new SceneTraversalEngine(scene);

        keypadNodes = createKeypadNodesInScene(scene, traversalEngine);

        stage.show();
        stage.requestFocus();
    }

    @AfterEach
    public void tearDown() {
        stage = null;
        scene = null;
        keypadNodes = null;
        traversalEngine = null;
    }

    @ParameterizedTest
    @MethodSource("data")
    public void untransformedTraversalTest(int fromNumber,
                                           Direction direction,
                                           int toNumber,
                                           int toNumberTransformed) {
        keypadNodes[fromNumber - 1].requestFocus();
        traversalEngine.trav(keypadNodes[fromNumber - 1], direction, TraversalMethod.DEFAULT);
        assertTrue(keypadNodes[toNumber - 1].isFocused());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void transformedTraversalTest(int fromNumber,
                                         Direction direction,
                                         int toNumber,
                                         int toNumberTransformed) {
        scene.getRoot().setRotate(90);
        keypadNodes[fromNumber - 1].requestFocus();
        traversalEngine.trav(keypadNodes[fromNumber - 1], direction, TraversalMethod.DEFAULT);
        assertTrue(keypadNodes[toNumberTransformed - 1].isFocused());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void traverseListenerTest(int fromNumber,
                                     Direction direction,
                                     int toNumber,
                                     int toNumberTransformed) {
        final TraverseListenerImpl traverseListener =
                new TraverseListenerImpl();
        traversalEngine.addTraverseListener(traverseListener);
        keypadNodes[fromNumber - 1].requestFocus();
        traversalEngine.trav(keypadNodes[fromNumber - 1], direction, TraversalMethod.DEFAULT);
        if (fromNumber != toNumber) {
            assertEquals(1, traverseListener.getCallCounter());
            assertSame(keypadNodes[toNumber - 1],
                       traverseListener.getLastNode());
        } else {
            assertEquals(0, traverseListener.getCallCounter());
        }
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

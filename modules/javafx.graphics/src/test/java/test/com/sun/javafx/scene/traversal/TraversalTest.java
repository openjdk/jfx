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

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.stream.Stream;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.scene.traversal.TraversalDirection;
import javafx.scene.traversal.TraversalMethod;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.scene.traversal.TopMostTraversalEngine;

/**
 * Tests for TraversalEngine with the default ContainerTabOrder policy.
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

    /*
     * Parameters: [fromNumber], [traversalDirection], [toNumber], [toNumberTransformed]
     */
    private static Stream<Arguments> parameters() {
        return Stream.of(
            /* traversal from center */
            Arguments.of( 5, TraversalDirection.LEFT, 4, 8 ),
            Arguments.of( 5, TraversalDirection.RIGHT, 6, 2 ),
            Arguments.of( 5, TraversalDirection.UP, 2, 4 ),
            Arguments.of( 5, TraversalDirection.DOWN, 8, 6 ),

            // using ContainerTabOrder, target is always the same
            Arguments.of( 5, TraversalDirection.PREVIOUS, 4, 4 ),
            Arguments.of( 5, TraversalDirection.NEXT, 6, 6 ),

            /* traversal from borders (untransformed) */
            Arguments.of( 4, TraversalDirection.LEFT, 4, 7 ),
            Arguments.of( 6, TraversalDirection.RIGHT, 6, 3 ),
            Arguments.of( 2, TraversalDirection.UP, 2, 1 ),
            Arguments.of( 8, TraversalDirection.DOWN, 8, 9 ),

            // using ContainerTabOrder, target always the same
            Arguments.of( 4, TraversalDirection.PREVIOUS, 3, 3 ),
            Arguments.of( 1, TraversalDirection.PREVIOUS, 9, 9 ),
            Arguments.of( 6, TraversalDirection.NEXT, 7, 7 ),
            Arguments.of( 9, TraversalDirection.NEXT, 1, 1 ),

            /* traversal from borders (transformed) */
            Arguments.of( 2, TraversalDirection.RIGHT, 3, 2 ),
            Arguments.of( 8, TraversalDirection.LEFT, 7, 8 ),
            Arguments.of( 4, TraversalDirection.UP, 1, 4 ),
            Arguments.of( 6, TraversalDirection.DOWN, 9, 6 ),

            // using ContainerTabOrder, target always the same
            Arguments.of( 8, TraversalDirection.PREVIOUS, 7, 7 ),
            Arguments.of( 7, TraversalDirection.PREVIOUS, 6, 6 ),
            Arguments.of( 2, TraversalDirection.NEXT, 3, 3 ),
            Arguments.of( 3, TraversalDirection.NEXT, 4, 4)
        );
    }

    @BeforeEach
    public void setUp() {
        stage = new Stage();
        scene = new Scene(new Group(), 500, 500);
        stage.setScene(scene);

        keypadNodes = createKeypadNodesInScene(scene);

        stage.show();
        stage.requestFocus();
    }

    @AfterEach
    public void tearDown() {
        if (stage != null) {
            stage.hide();
        }
        stage = null;
        scene = null;
        keypadNodes = null;
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void untransformedTraversalTest(
        int fromNumber,
        TraversalDirection direction,
        int toNumber,
        int toNumberTransformed)
    {
        keypadNodes[fromNumber - 1].requestFocus();
        TopMostTraversalEngine.trav(scene.getRoot(), keypadNodes[fromNumber - 1], direction, TraversalMethod.DEFAULT);
        assertTrue(keypadNodes[toNumber - 1].isFocused());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void transformedTraversalTest(
        int fromNumber,
        TraversalDirection direction,
        int toNumber,
        int toNumberTransformed)
    {
        scene.getRoot().setRotate(90);
        keypadNodes[fromNumber - 1].requestFocus();
        TopMostTraversalEngine.trav(scene.getRoot(), keypadNodes[fromNumber - 1], direction, TraversalMethod.DEFAULT);
        assertTrue(keypadNodes[toNumberTransformed - 1].isFocused());
    }

    private static Node[] createKeypadNodesInScene(final Scene scene) {
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
}

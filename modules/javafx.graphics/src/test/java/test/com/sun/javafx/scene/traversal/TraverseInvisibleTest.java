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
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.scene.traversal.TopMostTraversalEngine;
import com.sun.javafx.scene.traversal.TraversalDirectionInternal;

/**
 * Tests TraversalEngine with invisible nodes, using the default ContainerTabOrder algorithm,
 */
public final class TraverseInvisibleTest {
    private Stage stage;
    private Scene scene;
    private Node[] keypadNodes;

    /*
    ** Parameters: [fromNumber], [direction], [invisibleNumber], [toNumber]
    ** The Grid looks like :
    **    0 1 2
    **    3 4 5
    **    6 7 8
    */
    private static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of( 3, TraversalDirectionInternal.RIGHT, 4, 5),
            Arguments.of( 5, TraversalDirectionInternal.LEFT, 4, 3),
            Arguments.of( 4, TraversalDirectionInternal.NEXT, 5, 6),
            Arguments.of( 6, TraversalDirectionInternal.PREVIOUS, 5, 4),
            Arguments.of( 8, TraversalDirectionInternal.UP, 5, 2 ),
            Arguments.of( 2, TraversalDirectionInternal.DOWN, 5, 8)
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
        stage = null;
        scene = null;
        keypadNodes = null;
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void traverseOverInvisible(
        int fromNumber,
        TraversalDirectionInternal direction,
        int invisibleNumber,
        int toNumber)
    {
        keypadNodes[fromNumber].requestFocus();
        keypadNodes[invisibleNumber].setVisible(false);
        TopMostTraversalEngine.trav(scene.getRoot(), keypadNodes[fromNumber], direction, false);

        assertTrue(keypadNodes[toNumber].isFocused());

        keypadNodes[invisibleNumber - 1].setVisible(true);
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

/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.TraversalDirection;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests Node focus traversal public API.
 */
public final class NodeTraversalTest {
    private Stage stage;
    private Group root;
    private Scene scene;
    /**
     * 3x3 grid of nodes:
     * <pre>
     *   1  2  3
     *   4  5  6
     *   7  8  9
     * </pre>
     */
    private Node[] nodes;

    // Parameters: [from, direction, to]
    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(1, TraversalDirection.NEXT, 2),
            Arguments.of(1, TraversalDirection.PREVIOUS, 9),
            Arguments.of(1, TraversalDirection.UP, 1),
            Arguments.of(1, TraversalDirection.DOWN, 4),
            Arguments.of(1, TraversalDirection.LEFT, 1),
            Arguments.of(1, TraversalDirection.RIGHT, 2),

            Arguments.of(2, TraversalDirection.NEXT, 3),
            Arguments.of(2, TraversalDirection.PREVIOUS, 1),
            Arguments.of(2, TraversalDirection.UP, 2),
            Arguments.of(2, TraversalDirection.DOWN, 5),
            Arguments.of(2, TraversalDirection.LEFT, 1),
            Arguments.of(2, TraversalDirection.RIGHT, 3),

            Arguments.of(3, TraversalDirection.NEXT, 4),
            Arguments.of(3, TraversalDirection.PREVIOUS, 2),
            Arguments.of(3, TraversalDirection.UP, 3),
            Arguments.of(3, TraversalDirection.DOWN, 6),
            Arguments.of(3, TraversalDirection.LEFT, 2),
            Arguments.of(3, TraversalDirection.RIGHT, 3),

            Arguments.of(4, TraversalDirection.NEXT, 5),
            Arguments.of(4, TraversalDirection.PREVIOUS, 3),
            Arguments.of(4, TraversalDirection.UP, 1),
            Arguments.of(4, TraversalDirection.DOWN, 7),
            Arguments.of(4, TraversalDirection.LEFT, 4),
            Arguments.of(4, TraversalDirection.RIGHT, 5),

            Arguments.of(5, TraversalDirection.NEXT, 6),
            Arguments.of(5, TraversalDirection.PREVIOUS, 4),
            Arguments.of(5, TraversalDirection.UP, 2),
            Arguments.of(5, TraversalDirection.DOWN, 8),
            Arguments.of(5, TraversalDirection.LEFT, 4),
            Arguments.of(5, TraversalDirection.RIGHT, 6),

            Arguments.of(6, TraversalDirection.NEXT, 7),
            Arguments.of(6, TraversalDirection.PREVIOUS, 5),
            Arguments.of(6, TraversalDirection.UP, 3),
            Arguments.of(6, TraversalDirection.DOWN, 9),
            Arguments.of(6, TraversalDirection.LEFT, 5),
            Arguments.of(6, TraversalDirection.RIGHT, 6),

            Arguments.of(7, TraversalDirection.NEXT, 8),
            Arguments.of(7, TraversalDirection.PREVIOUS, 6),
            Arguments.of(7, TraversalDirection.UP, 4),
            Arguments.of(7, TraversalDirection.DOWN, 7),
            Arguments.of(7, TraversalDirection.LEFT, 7),
            Arguments.of(7, TraversalDirection.RIGHT, 8),

            Arguments.of(8, TraversalDirection.NEXT, 9),
            Arguments.of(8, TraversalDirection.PREVIOUS, 7),
            Arguments.of(8, TraversalDirection.UP, 5),
            Arguments.of(8, TraversalDirection.DOWN, 8),
            Arguments.of(8, TraversalDirection.LEFT, 7),
            Arguments.of(8, TraversalDirection.RIGHT, 9),

            Arguments.of(9, TraversalDirection.NEXT, 1),
            Arguments.of(9, TraversalDirection.PREVIOUS, 8),
            Arguments.of(9, TraversalDirection.UP, 6),
            Arguments.of(9, TraversalDirection.DOWN, 9),
            Arguments.of(9, TraversalDirection.LEFT, 8),
            Arguments.of(9, TraversalDirection.RIGHT, 9));
    }

    @BeforeEach
    public void beforeEach() {
        stage = new Stage();
        root = new Group();
        scene = new Scene(root, 500, 500);
        stage.setScene(scene);

        nodes = createNodes();

        stage.show();
        stage.requestFocus();
    }

    @AfterEach
    public void afterEach() {
        stage.hide();
        stage = null;
        scene = null;
        nodes = null;
    }

    @ParameterizedTest
    @MethodSource("data")
    public void requestFocusTraversal(int from, TraversalDirection dir, int to) {
        // focus the start node
        nodes[from - 1].requestFocus();
        // attempt traversal
        boolean success = nodes[from - 1].requestFocusTraversal(dir);
        // validate the focused node
        assertTrue(nodes[to - 1].isFocused(), message("Focused", Node::isFocused));
        if (success) {
            // validate that success resulted in focus-visible node
            assertTrue(nodes[to - 1].isFocusVisible(), message("FocusVisible", Node::isFocusVisible));
        }
    }

    private String message(String prefix, Predicate<Node> func) {
        for (Node n: nodes) {
            if (func.test(n)) {
                return "(" + prefix + ":" + n.getId() + ")";
            }
        }
        return "none";
    }

    private Node[] createNodes() {
        Node[] ns = new Node[9];
        int ix = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Node n = new Rectangle(10 + col * 50, 10 + row * 50, 40, 40);
                n.setFocusTraversable(true);
                n.setId(String.valueOf(ix + 1));
                ns[ix] = n;
                root.getChildren().add(n);
                ix++;
            }
        }
        return ns;
    }
}

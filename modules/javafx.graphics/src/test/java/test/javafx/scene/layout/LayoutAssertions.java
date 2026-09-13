/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.layout;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Utility methods for verifying JavaFX layout bounds using
 * an ASCII-art representation.
 * <p>
 * The ASCII representation describes expected node bounds using
 * boxed regions. Named boxes correspond to 1-based indices of
 * nodes passed to the assertion methods.
 */
public final class LayoutAssertions {
    private static final Comparator<Map.Entry<String, AsciiArtParser.Bounds>> COMPARATOR = Comparator.comparing(e -> e.getKey());

    private LayoutAssertions() {
        // utility class
    }

    /**
     * Asserts that the given nodes match the bounds defined in the ASCII
     * representation.
     * <p>
     * Any non-whitespace character other than letters, digits, {@code '.'}
     * or {@code ':'} is treated as a boundary. In practice this means
     * you can use characters like {@code -}, {@code |} and {@code +} to construct
     * the boundaries.
     * <p>
     * Each boxed region must contain a size ({@code width x height}),
     * optionally prefixed by a 1-based index followed by {@code ':'}.
     * Unnamed boxes are treated as spacers.
     * <p>
     * Margins can be indicated by a box which surrounds another box. A margin must
     * have 4 numbers corresponding to the top, left, right and bottom inset size.
     * <p>
     * An example visual representation:
     * <pre>
     * +-------------+-----+--------------------------+
     * |  1: 40x20   |     |  2: 100x20               |
     * +-------------+     +--------------------------+------------------+
     * |             |     |                      5                      |
     * |             |     |     +----------------------+----------+     |
     * |  3: 40x28   | 4x0 |  2  |   4: 100x20          | 5: 50x20 |  4  |
     * |             |     |     +----------------------+----------+     |
     * |             |     |                      3                      |
     * +-------------+     +--------------------------+------------------+
     * |  6: 40x20   |     |  7: 100x20               |
     * +-------------+-----+--------------------------+
     * </pre>
     * In the above representation, there are 7 named boxes representing node positions and
     * their sizes. There is one spacer of 4 pixels wide right of node 1. There is a margin
     * around nodes 4 and 5. Next to nodes 2 and 7 there is some empty space.
     *
     * @param expected ASCII-art representation of expected layout, cannot be {@code null}
     * @param nodes nodes referenced by index in the ASCII representation, cannot be {@code null}
     * @return the {@link AsciiArtParser} used for parsing
     * @throws NullPointerException if any argument is {@code null}
     */
    public static AsciiArtParser assertChildBounds(String expected, Node... nodes) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(nodes, "nodes");

        AsciiArtParser parser = new AsciiArtParser(expected.lines().toList());

        for (int i = 0; i < nodes.length; i++) {
            Node node = Objects.requireNonNull(nodes[i], "nodes[" + i + "]");
            int index = i + 1;

            AsciiArtParser.Bounds expectedBounds = parser.getBounds(String.valueOf(index));
            Bounds actualBounds = toSceneBounds(node);

            if (expectedBounds == null) {
                fail("No bounds were given for child " + index + " in:\n" + expected);
            }
            else if (expectedBounds.x() != actualBounds.getMinX() || expectedBounds.y() != actualBounds.getMinY()) {
                fail(
                    "Expected child " + index + " location ("
                        + expectedBounds.x() + ", " + expectedBounds.y()
                        + ") but was: ("
                        + actualBounds.getMinX() + ", " + actualBounds.getMinY()
                        + ") in:\n" + expected
                        + "\nParsed bounds of all areas:\n"
                        + parser.getAllBounds().entrySet().stream().sorted(COMPARATOR).map(Object::toString).collect(Collectors.joining("\n")));
            }
            else if (expectedBounds.width() != actualBounds.getWidth() || expectedBounds.height() != actualBounds.getHeight()) {
                fail(
                    "Expected child " + index + " size ("
                        + expectedBounds.width() + ", " + expectedBounds.height()
                        + ") but was: ("
                        + actualBounds.getWidth() + ", " + actualBounds.getHeight()
                        + ") in:\n" + expected
                        + "\nParsed bounds of all areas:\n"
                        + parser.getAllBounds().entrySet().stream().sorted(COMPARATOR).map(Object::toString).collect(Collectors.joining("\n")));
            }
        }

        return parser;
    }

    /**
     * Asserts that a container and its managed children match the ASCII
     * representation.
     * <p>
     * Any non-whitespace character other than letters, digits, {@code '.'}
     * or {@code ':'} is treated as a boundary. In practice this means
     * you can use characters like {@code -}, {@code |} and {@code +} to construct
     * the boundaries.
     * <p>
     * Each boxed region must contain a size ({@code width x height}),
     * optionally prefixed by a 1-based index followed by {@code ':'}.
     * Unnamed boxes are treated as spacers.
     * <p>
     * Margins can be indicated by a box which surrounds another box. A margin must
     * have 4 numbers corresponding to the top, left, right and bottom inset size.
     * <p>
     * An example visual representation:
     * <pre>
     * +-------------+-----+--------------------------+
     * |  1: 40x20   |     |  2: 100x20               |
     * +-------------+     +--------------------------+------------------+
     * |             |     |                      5                      |
     * |             |     |     +----------------------+----------+     |
     * |  3: 40x28   | 4x0 |  2  |   4: 100x20          | 5: 50x20 |  4  |
     * |             |     |     +----------------------+----------+     |
     * |             |     |                      3                      |
     * +-------------+     +--------------------------+------------------+
     * |  6: 40x20   |     |  7: 100x20               |
     * +-------------+-----+--------------------------+
     * </pre>
     * In the above representation, there are 7 named boxes representing node positions and
     * their sizes. There is one spacer of 4 pixels wide right of node 1. There is a margin
     * around nodes 4 and 5. Next to nodes 2 and 7 there is some empty space.
     *
     * @param expected  ASCII-art representation of expected layout, must not be {@code null}
     * @param container container whose children will be verified, must not be {@code null}
     * @return the {@link AsciiArtParser} used for parsing
     * @throws NullPointerException if any argument is {@code null}
     */
    public static AsciiArtParser assertBounds(String expected, Region container) {
        Objects.requireNonNull(container, "container");

        Node[] managedChildren = container.getChildrenUnmodifiable().stream()
            .filter(Node::isManaged)
            .toArray(Node[]::new);

        AsciiArtParser parser = assertChildBounds(expected, managedChildren);

        double expectedWidth = parser.getBounds().width();
        double expectedHeight = parser.getBounds().height();

        if (expectedWidth != container.getWidth() || expectedHeight != container.getHeight()) {
            fail(
                "Expected container size ("
                    + expectedWidth + "x" + expectedHeight
                    + ") but was: ("
                    + container.getWidth() + "x" + container.getHeight()
                    + ") in:\n" + expected
                    + "\nParsed bounds of all areas:\n"
                    + parser.getAllBounds().entrySet().stream().sorted(COMPARATOR).map(Object::toString).collect(Collectors.joining("\n")));
        }

        return parser;
    }

    /**
     * Converts a node's local bounds to scene (root) coordinates.
     */
    private static Bounds toSceneBounds(Node node) {
        Bounds bounds = node.getBoundsInLocal();
        Node current = node;

        while (current.getParent() != null) {
            bounds = current.localToParent(bounds);
            current = current.getParent();
        }

        return bounds;
    }
}

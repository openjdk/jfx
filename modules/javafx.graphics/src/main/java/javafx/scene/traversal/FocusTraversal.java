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
package javafx.scene.traversal;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import com.sun.javafx.scene.traversal.TraversalDirection;
import com.sun.javafx.scene.traversal.TraversalUtils;

/**
 * Provides the methods for focus traversal within the JavaFX application.
 * <p>
 * The methods provided in this class allow for transferring focus away from the
 * specific {@code Node} (which serves as a reference and does not have to be focused or
 * focusable), within the owning {@link Scene} or {@link SubScene}.
 *
 * @since 24
 */
public final class FocusTraversal {
    /**
     * Traverse focus downward.
     * A successful traversal results in the newly focused {@code Node} visible indicating its focused state.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseDown(Node node) {
        return TraversalUtils.traverse(node, TraversalDirection.DOWN, true);
    }

    /**
     * Traverse focus left.
     * A successful traversal results in the newly focused {@code Node} visible indicating its focused state.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseLeft(Node node) {
        return TraversalUtils.traverse(node, TraversalDirection.LEFT, true);
    }

    /**
     * Traverse focus to the next focuseable {@code Node}.
     * A successful traversal results in the newly focused {@code Node} visible indicating its focused state.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseNext(Node node) {
        return TraversalUtils.traverse(node, TraversalDirection.NEXT, true);
    }

    /**
     * Traverse focus to the previous focusable Node.
     * A successful traversal results in the newly focused {@code Node} visible indicating its focused state.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traversePrevious(Node node) {
        return TraversalUtils.traverse(node, TraversalDirection.PREVIOUS, true);
    }

    /**
     * Traverse focus right.
     * A successful traversal results in the newly focused {@code Node} visible indicating its focused state.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseRight(Node node) {
        return TraversalUtils.traverse(node, TraversalDirection.RIGHT, true);
    }

    /**
     * Traverse focus upward.
     * A successful traversal results in the newly focused {@code Node} visible indicating its focused state.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseUp(Node node) {
        return TraversalUtils.traverse(node, TraversalDirection.UP, true);
    }

    private FocusTraversal() {
    }
}

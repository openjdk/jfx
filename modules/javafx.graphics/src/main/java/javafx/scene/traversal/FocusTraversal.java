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
import javafx.scene.TraversalDirection;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.traversal.TopMostTraversalEngine;

/**
 * Provides the methods for focus traversal within the JavaFX application.
 * <p>
 * The methods provided in this class allow for transferring focus away from the
 * specific {@code Node} (which serves as a reference and does not have to be focused or
 * focusable), within the owning {@link Scene} or {@link SubScene}, as determined
 * by the default and custom {@link TraversalPolicy} set on the adjacent {@code Node}s.
 *
 * @since 24
 */
@Deprecated // FIX remove
public final class FocusTraversal {
    /**
     * Traverses focus to the adjacent node as specified by the direction.
     *
     * @param node the node to traverse focus from
     * @param dir the direction of traversal
     * @param focusVisible whether the focused Node should visible indicate focus
     * @return true if traversal was successful
     */
    public static boolean traverse(Node node, TraversalDirection dir, boolean focusVisible) {
        System.out.println(dir + " node=" + node);
        if (node != null) {
            SubScene ss = NodeHelper.getSubScene(node);
            if (ss != null) {
                return TopMostTraversalEngine.trav(ss.getRoot(), node, dir, focusVisible) != null;
            }

            Scene sc = node.getScene();
            if (sc != null) {
                return TopMostTraversalEngine.trav(sc.getRoot(), node, dir, focusVisible) != null;
            }
        }
        return false;
    }

    /**
     * Traverse focus downward as a response to pressing a key.
     * <p>
     * This convenience method is equivalent to calling
     * {@code traverse(node, TraversalDirection.DOWN, true);}
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseDown(Node node) {
        return traverse(node, TraversalDirection.DOWN, true);
    }

    /**
     * Traverse focus left as a response to pressing a key.
     * <p>
     * This convenience method is equivalent to calling
     * {@code traverse(node, TraversalDirection.LEFT, true)}.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseLeft(Node node) {
        return traverse(node, TraversalDirection.LEFT, true);
    }

    /**
     * Traverse focus to the next focuseable Node as a response to pressing a key.
     * <p>
     * This convenience method is equivalent to calling
     * {@code traverse(node, TraversalDirection.NEXT, true)}.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseNext(Node node) {
        return traverse(node, TraversalDirection.NEXT, true);
    }

    /**
     * Traverse focus to the next focuseable Node as a response to pressing a key.
     * This method does not traverse into the current parent.
     * <p>
     * This convenience method is equivalent to calling
     * {@code traverse(node, TraversalDirection.NEXT_IN_LINE, true)}.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseNextInLine(Node node) {
        return traverse(node, TraversalDirection.NEXT_IN_LINE, true);
    }

    /**
     * Traverse focus to the previous focusable Node as a response to pressing a key.
     * <p>
     * This convenience method is equivalent to calling
     * {@code traverse(node, TraversalDirection.PREVIOUS, true)}.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traversePrevious(Node node) {
        return traverse(node, TraversalDirection.PREVIOUS, true);
    }

    /**
     * Traverse focus right as a response to pressing a key.
     * <p>
     * This convenience method is equivalent to calling
     * {@code traverse(node, TraversalDirection.RIGHT, true)}.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseRight(Node node) {
        return traverse(node, TraversalDirection.RIGHT, true);
    }

    /**
     * Traverse focus upward as a response to pressing a key.
     * <p>
     * This convenience method is equivalent to calling
     * {@code traverse(node, TraversalDirection.UP, true)}.
     *
     * @param node the node to traverse focus from
     * @return true if traversal was successful
     */
    public static boolean traverseUp(Node node) {
        return traverse(node, TraversalDirection.UP, true);
    }

    private FocusTraversal() {
    }
}

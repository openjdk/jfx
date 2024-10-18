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
package com.sun.javafx.scene.traversal;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.ParentHelper;

public final class TraversalUtils {
    public static final TraversalPolicy DEFAULT_POLICY = PlatformImpl.isContextual2DNavigation() ? new Heuristic2D() : new ContainerTabOrder();
    public static final TraversalPolicy EMPTY_POLICY = initEmptyTraversablePolicy();
    private static final Bounds INITIAL_BOUNDS = new BoundingBox(0, 0, 1, 1);

    private TraversalUtils() {
    }

    public static TraversalPolicy createDefaultTraversalAlgorithm() {
        return PlatformImpl.isContextual2DNavigation() ? new Heuristic2D() : new ContainerTabOrder();
    }

    /**
     * Gets the appropriate bounds for the given node, transformed into the specified node's coordinates.
     * This method returns {@code null} if {@code n} or {@code forParent} is null
     * or the node is not a part of the scene graph.
     * @return bounds of node in {@code forParent} coordinates, or null
     */
    public static Bounds getLayoutBounds(Node n, Parent forParent) {
        if ((n != null) && (forParent != null)) {
            Bounds b = n.localToScene(n.getLayoutBounds());
            if (b != null) {
                return forParent.sceneToLocal(b);
            }
        }
        return null;
    }

    /**
     * Gets the appropriate bounds for the given node, transformed into the scene's coordinates.
     * @return bounds of node in scene coordinates
     */
    public static Bounds getLayoutBoundsInSceneCoordinates(Node n) {
        if (n != null) {
            return n.localToScene(n.getLayoutBounds());
        } else {
            return INITIAL_BOUNDS;
        }
    }

    private static TraversalPolicy initEmptyTraversablePolicy() {
        return new TraversalPolicy() {
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
        };
    }

    /**
     * Returns all possible targets within the traversal root.
     *
     * @param root the traversal root
     * @return the List of all possible targets within the traversal root
     */
    public static final List<Node> getAllTargetNodes(Parent root) {
        final List<Node> targetNodes = new ArrayList<>();
        addFocusableChildrenToList(targetNodes, root);
        return targetNodes;
    }

    private static final void addFocusableChildrenToList(List<Node> list, Parent parent) {
        List<Node> parentsNodes = parent.getChildrenUnmodifiable();
        for (Node n : parentsNodes) {
            if (n.isFocusTraversable() && !n.isFocused() && NodeHelper.isTreeVisible(n) && !n.isDisabled()) {
                list.add(n);
            }
            if (n instanceof Parent p) {
                addFocusableChildrenToList(list, p);
            }
        }
    }

    public static Node findNextFocusableNode(Parent root, Node node, boolean traverseIntoCurrent) {
        return TabOrderHelper.findNextFocusablePeer(node, root, traverseIntoCurrent);
    }

    public static Node findPreviousFocusableNode(Parent root, Node node) {
        return TabOrderHelper.findPreviousFocusablePeer(node, root);
    }

    /**
     * Traverses focus to the adjacent node as specified by the direction.
     *
     * @param node the node to traverse focus from
     * @param dir the direction of traversal
     * @param focusVisible whether the focused Node should visible indicate focus
     * @return true if traversal was successful
     */
    public static boolean traverse(Node node, TraversalDirection dir, boolean focusVisible) {
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

    public static TraversalPolicy getTraversalPolicy(Parent parent) {
        return ParentHelper.getTraversalPolicy(parent);
    }

    public static void setTraversalPolicy(Parent parent, TraversalPolicy policy) {
        ParentHelper.setTraversalPolicy(parent, policy);
    }
}
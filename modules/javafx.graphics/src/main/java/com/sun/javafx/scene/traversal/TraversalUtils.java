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
import javafx.scene.traversal.TraversalDirection;
import javafx.scene.traversal.TraversalPolicy;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.scene.NodeHelper;

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
     * Gets the appropriate bounds for the given node, transformed into
     * the scene's or the specified node's coordinates.
     * @return bounds of node in {@code forParent} coordinates or scene coordinates if {@code forParent} is null
     */
    public static Bounds getLayoutBounds(Node n, Parent forParent) {
        final Bounds bounds;
        if (n != null) {
            if (forParent == null) {
                bounds = n.localToScene(n.getLayoutBounds());
            } else {
                bounds = forParent.sceneToLocal(n.localToScene(n.getLayoutBounds()));
            }
        } else {
            bounds = INITIAL_BOUNDS;
        }
        return bounds;
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
}
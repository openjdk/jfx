/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.traversal.TraversalDirection;
import javafx.scene.traversal.TraversalEvent;
import javafx.scene.traversal.TraversalMethod;
import javafx.scene.traversal.TraversalPolicy;
import com.sun.javafx.scene.NodeHelper;

/**
 * This is the class for all top-level traversal engines in scenes and subscenes.
 * These traversal engines are created automatically and can only have the default algorithm.
 *
 * These engines should be used by calling {@link #trav(javafx.scene.Node, TraversalDirection)}, {@link #traverseToFirst()} and
 * {@link #traverseToLast()} methods. These methods do the actual traversal - selecting the Node that's should be focused next and
 * focusing it. Also, listener calls are handled by top-most traversal engines.
 * select* methods can be used as well, but will *not* transfer the focus to the result, they are just query methods.
 */
public final class TopMostTraversalEngine {
    /**
     * Traverse the focus to the next node in the specified direction.
     *
     * @param node The starting node to traverse from
     * @param dir the traversal direction
     * @param method the traversal method
     * @return the new focus owner or null if none found (in that case old focus owner is still valid)
     */
    public static final Node trav(Parent root, Node node, TraversalDirection dir, TraversalMethod method) {
        Node newNode = null;
        Parent p = node.getParent();
        Node traverseNode = node;
        while (p != null) {
            // First find the nearest traversal policy override
            TraversalPolicy policy = p.getTraversalPolicy();
            if (policy != null) {
                newNode = policy.select(p, node, dir);
                if (newNode != null) {
                    break;
                } else {
                    // The inner traversal engine wasn't able to select anything in the specified direction.
                    // So now we try to traverse from the whole parent (associated with that traversal engine)
                    // by a traversal engine that's higher in the hierarchy
                    traverseNode = p;
                    if (dir == TraversalDirection.NEXT) {
                        dir = TraversalDirection.NEXT_IN_LINE;
                    }
                }
            }
            p = p.getParent();
        }
        // No engine override was able to find the Node in the specified direction, so
        if (newNode == null) {
            newNode = TraversalPolicy.getDefault().select(root, traverseNode, dir);
        }
        if (newNode == null) {
            if (dir == TraversalDirection.NEXT || dir == TraversalDirection.NEXT_IN_LINE) {
                newNode = TraversalPolicy.getDefault().selectFirst(root);
            } else if (dir == TraversalDirection.PREVIOUS) {
                newNode = TraversalPolicy.getDefault().selectLast(root);
            }
        }
        if (newNode != null) {
            focusAndNotify(root, newNode, method);
        }
        return newNode;
    }

    private static void focusAndNotify(Parent root, Node n, TraversalMethod method) {
        if (method == TraversalMethod.KEY) {
            NodeHelper.requestFocusVisible(n);
        } else {
            n.requestFocus();
        }

        n.fireEvent(new TraversalEvent(n, TraversalUtils.getLayoutBounds(n, root), TraversalEvent.NODE_TRAVERSED));
    }

    /**
     * Set focus on the first Node in this context (if any)
     * @return the first node or null if there's none
     */
    public static final Node traverseToFirst(Parent root) {
        Node n = TraversalPolicy.getDefault().selectFirst(root);
        if (n != null) {
            focusAndNotify(root, n, TraversalMethod.DEFAULT);
        }
        return n;
    }
}

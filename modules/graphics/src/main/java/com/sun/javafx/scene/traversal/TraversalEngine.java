/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.ArrayList;
import java.util.List;

public abstract class TraversalEngine {

    protected final Algorithm algorithm;

    private final Bounds initialBounds =  new BoundingBox(0, 0, 1, 1);
    final ArrayList<TraverseListener> listeners = new ArrayList<>();

    protected TraversalEngine(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    protected TraversalEngine() {
        this.algorithm = null;
    }

    public final void addTraverseListener(TraverseListener listener) {
        listeners.add(listener);
    }

    void notifyTraversedTo(Node newNode) {
        for (TraverseListener l : listeners) {
            l.onTraverse(newNode, getLayoutBounds(newNode, getRoot()));
        }
    }

    public final Node select(Node from, Direction dir) {
        return algorithm.select(from, dir, this);
    }

    public final Node selectFirst() {
        return algorithm.selectFirst(this);
    }

    public final Node selectLast() {
        return algorithm.selectLast(this);
    }

    // get all focusable nodes in tree...
    public final List<Node> getAllTargetNodes() {
        final List<Node> targetNodes = new ArrayList<>();
        addFocusableChildrenToList(targetNodes, getRoot());
        return targetNodes;
    }

    private void addFocusableChildrenToList(List<Node> list, Parent parent) {
        List<Node> parentsNodes = parent.getChildrenUnmodifiable();
        for (Node n : parentsNodes) {
            if (n.isFocusTraversable() && !n.isFocused() && n.impl_isTreeVisible() && !n.isDisabled()) {
                list.add(n);
            }
            if (n instanceof Parent) {
                addFocusableChildrenToList(list, (Parent)n);
            }
        }
    }

    protected abstract Parent getRoot();

    public final Bounds getSceneLayoutBounds(Node n) {
        return getLayoutBounds(n, null);
    }

    public final boolean canTraverse() {
        return algorithm != null;
    }

    /**
     * Gets the appropriate bounds for the given node, transformed into
     * the scene's or the traversal root's coordinates.
     */
    private Bounds getLayoutBounds(Node n, Parent forParent) {
        final Bounds bounds;
        if (n != null) {
            if (forParent == null) {
                bounds = n.localToScene(n.getLayoutBounds());
            } else {
                bounds = forParent.sceneToLocal(n.localToScene(n.getLayoutBounds()));
            }
        } else {
            bounds = initialBounds;
        }
        return bounds;
    }
}

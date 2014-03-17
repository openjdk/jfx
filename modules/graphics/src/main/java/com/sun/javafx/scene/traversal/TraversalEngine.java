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

import com.sun.javafx.application.PlatformImpl;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.ArrayList;
import java.util.List;

public abstract class TraversalEngine{

    static final Algorithm DEFAULT_ALGORITHM = PlatformImpl.isContextual2DNavigation() ? new Hueristic2D() : new ContainerTabOrder();

    private final TraversalContext context = new EngineContext();
    private final TempEngineContext tempEngineContext = new TempEngineContext();
    protected final Algorithm algorithm;

    private final Bounds initialBounds =  new BoundingBox(0, 0, 1, 1);
    private final ArrayList<TraverseListener> listeners = new ArrayList<>();

    protected TraversalEngine(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    protected TraversalEngine() {
        this.algorithm = null;
    }

    public final void addTraverseListener(TraverseListener listener) {
        listeners.add(listener);
    }

    final void notifyTraversedTo(Node newNode) {
        for (TraverseListener l : listeners) {
            l.onTraverse(newNode, getLayoutBounds(newNode, getRoot()));
        }
    }

    public final Node select(Node from, Direction dir) {
        return algorithm.select(from, dir, context);
    }

    public final Node selectFirst() {
        return algorithm.selectFirst(context);
    }

    public final Node selectLast() {
        return algorithm.selectLast(context);
    }

    protected abstract Parent getRoot();

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

    private final class EngineContext extends BaseEngineContext {
        @Override
        public Parent getRoot() {
            return TraversalEngine.this.getRoot();
        }
    }

    private final class TempEngineContext extends BaseEngineContext {
        private Parent root;

        @Override
        public Parent getRoot() {
            return root;
        }

        public void setRoot(Parent root) {
            this.root = root;
        }
    }

    private abstract class BaseEngineContext implements TraversalContext {

        @Override
        public List<Node> getAllTargetNodes() {
            final List<Node> targetNodes = new ArrayList<>();
            addFocusableChildrenToList(targetNodes, getRoot());
            return targetNodes;
        }

        public Bounds getSceneLayoutBounds(Node n) {
            return getLayoutBounds(n, null);
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

        @Override
        public Node selectFirstInParent(Parent parent) {
            tempEngineContext.setRoot(parent);
            return DEFAULT_ALGORITHM.selectFirst(tempEngineContext);
        }

        @Override
        public Node selectLastInParent(Parent parent) {
            tempEngineContext.setRoot(parent);
            return DEFAULT_ALGORITHM.selectLast(tempEngineContext);
        }

        @Override
        public Node selectInSubtree(Parent subTreeRoot, Node from, Direction dir) {
            tempEngineContext.setRoot(subTreeRoot);
            return DEFAULT_ALGORITHM.select(from, dir, tempEngineContext);
        }
    }
}

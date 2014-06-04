/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Node;
import javafx.scene.Parent;

public abstract class TopMostTraversalEngine extends TraversalEngine{

    protected TopMostTraversalEngine() {
        /*
         * for 2d behaviour from TAB use :
         *    algorithm = new WeightedClosestCorner();
         * for Container sequence TAB behaviour and 2d arrow behaviour use :
         *    algorithm = new ContainerTabOrder();
         * for 2D arrow behaviour with a target bias and a stack use :
         *    algorithm = new Biased2DWithStack();
         */
        super(DEFAULT_ALGORITHM);
    }

    public final Node trav(Node node, Direction dir) {
        Node newNode = null;
        Parent p = node.getParent();
        Node traverseNode = node;
        while (p != null) {
            ParentTraversalEngine engine = p.getImpl_traversalEngine();
            if (engine != null && engine.canTraverse()) {
                newNode = engine.select(node, dir);
                if (newNode != null) {
                    break;
                } else {
                    // The inner traversal engine wasn't able to select anything in the specified direction.
                    // So now we try to traverse from the whole parent (associated with that traversal engine)
                    // by a traversal engine that's higher in the hierarchy
                    traverseNode = p;
                    if (dir == Direction.NEXT) {
                        dir = Direction.NEXT_IN_LINE;
                    }
                }
            }
            p = p.getParent();
        }
        if (newNode == null) {
            newNode = select(traverseNode, dir);
        }
        if (newNode == null) {
            if (dir == Direction.NEXT || dir == Direction.NEXT_IN_LINE) {
                newNode = selectFirst();
            } else if (dir == Direction.PREVIOUS) {
                newNode = selectLast();
            }
        }
        if (newNode != null) {
            focusAndNotify(newNode);
        }
        return newNode;
    }

    private void focusAndNotify(Node newNode) {
        newNode.requestFocus();
        notifyTreeTraversedTo(newNode);
    }

    private void notifyTreeTraversedTo(Node newNode) {
        Parent p = newNode.getParent();
        while (p != null) {
            final ParentTraversalEngine impl_traversalEngine = p.getImpl_traversalEngine();
            if (impl_traversalEngine != null) {
                impl_traversalEngine.notifyTraversedTo(newNode);
            }
            p = p.getParent();
        }
        notifyTraversedTo(newNode);
    }

    public final Node traverseToFirst() {
        Node n = selectFirst();
        if (n != null) focusAndNotify(n);
        return n;
    }


    public final Node traverseToLast() {
        Node n = selectLast();
        if (n != null) focusAndNotify(n);
        return n;
    }
}

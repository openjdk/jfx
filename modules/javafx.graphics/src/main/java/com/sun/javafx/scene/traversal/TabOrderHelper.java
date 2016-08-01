/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.ParentHelper;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.List;

final class TabOrderHelper {
    private static Node findPreviousFocusableInList(List<Node> nodeList, int startIndex) {
        for (int i = startIndex ; i >= 0 ; i--) {
            Node prevNode = nodeList.get(i);
            // ParentTraverEngine can override traversability, so we need to check it first
            if (isDisabledOrInvisible(prevNode)) continue;
            final ParentTraversalEngine traversalEngine = prevNode instanceof Parent
                    ? ParentHelper.getTraversalEngine((Parent) prevNode) : null;
            if (prevNode instanceof Parent) {
                if (traversalEngine != null && traversalEngine.canTraverse()) {
                    Node selected = traversalEngine.selectLast();
                    if (selected != null) {
                        return selected;
                    }
                } else {
                    List<Node> prevNodesList = ((Parent) prevNode).getChildrenUnmodifiable();
                    if (prevNodesList.size() > 0) {
                        Node newNode = findPreviousFocusableInList(prevNodesList, prevNodesList.size() - 1);
                        if (newNode != null) {
                            return newNode;
                        }
                    }
                }
            }
            if (traversalEngine != null
                    ? traversalEngine.isParentTraversable()
                    : prevNode.isFocusTraversable()) {
                return prevNode;
            }
        }
        return null;
    }

    private static boolean isDisabledOrInvisible(Node prevNode) {
        return prevNode.isDisabled() || !NodeHelper.isTreeVisible(prevNode);
    }

    public static Node findPreviousFocusablePeer(Node node, Parent root) {
        Node startNode = node;
        Node newNode = null;
        List<Node> parentNodes = findPeers(startNode);

        if (parentNodes == null) {
            // We are at top level, so select the last focusable node
            ObservableList<Node> rootChildren = ((Parent) node).getChildrenUnmodifiable();
            return findPreviousFocusableInList(rootChildren, rootChildren.size() - 1);
        }

        int ourIndex = parentNodes.indexOf(startNode);

        // Start with the siblings "to the left"
        newNode = findPreviousFocusableInList(parentNodes, ourIndex - 1);

        /*
        ** we've reached the end of the peer nodes, and none have been selected,
        ** time to look at our parents peers.....
        */
        while (newNode == null && startNode.getParent() != root) {
            List<Node> peerNodes;
            int parentIndex;

            Parent parent = startNode.getParent();
            if (parent != null) {
                // If the parent itself is traversable, select it
                final ParentTraversalEngine parentEngine
                        = ParentHelper.getTraversalEngine(parent);
                if (parentEngine != null ? parentEngine.isParentTraversable() : parent.isFocusTraversable()) {
                    newNode = parent;
                } else {
                    peerNodes = findPeers(parent);
                    if (peerNodes != null) {
                        parentIndex = peerNodes.indexOf(parent);
                        newNode = findPreviousFocusableInList(peerNodes, parentIndex - 1);
                    }
                }
            }
            startNode = parent;
        }

        return newNode;
    }

    private static List<Node> findPeers(Node node) {
        List<Node> parentNodes = null;
        Parent parent = node.getParent();
        /*
        ** check that we haven't hit the top-level
        */
        if (parent != null) {
            parentNodes = parent.getChildrenUnmodifiable();
        }
        return parentNodes;
    }

    private static Node findNextFocusableInList(List<Node> nodeList, int startIndex) {
        for (int i = startIndex ; i < nodeList.size() ; i++) {
            Node nextNode = nodeList.get(i);
            if (isDisabledOrInvisible(nextNode)) continue;
            final ParentTraversalEngine traversalEngine = nextNode instanceof Parent
                    ? ParentHelper.getTraversalEngine((Parent) nextNode) : null;
            // ParentTraverEngine can override traversability, so we need to check it first
            if (traversalEngine != null
                    ? traversalEngine.isParentTraversable()
                    : nextNode.isFocusTraversable()) {
                return nextNode;
            }
            else if (nextNode instanceof Parent) {
                if (traversalEngine!= null && traversalEngine.canTraverse()) {
                    Node selected = traversalEngine.selectFirst();
                    if (selected != null) {
                        return selected;
                    } else {
                        // If the Parent has it's own engine, but no selection can be done, skip it
                        continue;
                    }
                }
                List<Node> nextNodesList = ((Parent)nextNode).getChildrenUnmodifiable();
                if (nextNodesList.size() > 0) {
                    Node newNode = findNextFocusableInList(nextNodesList, 0);
                    if (newNode != null) {
                        return newNode;
                    }
                }
            }
        }
        return null;
    }

    public static Node findNextFocusablePeer(Node node, Parent root, boolean traverseIntoCurrent) {
        Node startNode = node;
        Node newNode = null;

        // First, try to find next peer among the node children
        if (traverseIntoCurrent && node instanceof Parent) {
            newNode = findNextFocusableInList(((Parent)node).getChildrenUnmodifiable(), 0);
        }

        // Next step is to select the siblings "to the right"
        if (newNode == null) {
            List<Node> parentNodes = findPeers(startNode);
            if (parentNodes == null) {
                // We got a top level Node that has no focusable children (we know that from the first step above), so
                // there's nothing to do.
                return null;
            }
            int ourIndex = parentNodes.indexOf(startNode);
            newNode = findNextFocusableInList(parentNodes, ourIndex + 1);
        }

        /*
        ** we've reached the end of the peer nodes, and none have been selected,
        ** time to look at our parents peers.....
        */
        while (newNode == null && startNode.getParent() != root) {
            List<Node> peerNodes;
            int parentIndex;

            Parent parent = startNode.getParent();
            if (parent != null) {
                peerNodes = findPeers(parent);
                if (peerNodes != null) {
                    parentIndex = peerNodes.indexOf(parent);
                    newNode = findNextFocusableInList(peerNodes, parentIndex + 1);
                }
            }
            startNode = parent;
        }

        return newNode;
    }

    public static Node getFirstTargetNode(Parent p) {
        if (p == null || isDisabledOrInvisible(p)) return null;
        final ParentTraversalEngine traversalEngine
                = ParentHelper.getTraversalEngine(p);
        if (traversalEngine!= null && traversalEngine.canTraverse()) {
            Node selected = traversalEngine.selectFirst();
            if (selected != null) {
                return selected;
            }
        }
        List<Node> parentsNodes = p.getChildrenUnmodifiable();
        for (Node n : parentsNodes) {
            if (isDisabledOrInvisible(n)) continue;
            final ParentTraversalEngine parentEngine = n instanceof Parent
                    ? ParentHelper.getTraversalEngine((Parent)n) : null;
            if (parentEngine != null ? parentEngine.isParentTraversable() : n.isFocusTraversable()) {
                return n;
            }
            if (n instanceof Parent) {
                Node result = getFirstTargetNode((Parent)n);
                if (result != null) return result;
            }
        }
        return null;
    }

    public static Node getLastTargetNode(Parent p) {
        if (p == null || isDisabledOrInvisible(p)) return null;
        final ParentTraversalEngine traversalEngine
                = ParentHelper.getTraversalEngine(p);
        if (traversalEngine!= null && traversalEngine.canTraverse()) {
            Node selected = traversalEngine.selectLast();
            if (selected != null) {
                return selected;
            }
        }
        List<Node> parentsNodes = p.getChildrenUnmodifiable();
        for (int i = parentsNodes.size() - 1; i >= 0; --i) {
            Node n = parentsNodes.get(i);
            if (isDisabledOrInvisible(n)) continue;

            if (n instanceof Parent) {
                Node result = getLastTargetNode((Parent) n);
                if (result != null) return result;
            }
            final ParentTraversalEngine parentEngine = n instanceof Parent
                    ? ParentHelper.getTraversalEngine((Parent) n) : null;
            if (parentEngine != null ? parentEngine.isParentTraversable() : n.isFocusTraversable()) {
                return n;
            }
        }
        return null;
    }
}

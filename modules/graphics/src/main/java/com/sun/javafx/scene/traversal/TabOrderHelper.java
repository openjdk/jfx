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

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.List;

final class TabOrderHelper {
    static Node findPreviousFocusableInList(List<Node> nodeList, int startIndex) {
        Node newNode = null;

        for (int i = startIndex ; i >= 0 ; i--) {
            Node prevNode = nodeList.get(i);
            if (isDisabledOrInvisible(prevNode)) continue;

            if (prevNode instanceof javafx.scene.Parent) {
                List<Node> prevNodesList = ((Parent)prevNode).getChildrenUnmodifiable();
                if (prevNodesList.size() > 0) {
                    newNode = findPreviousFocusableInList(prevNodesList, prevNodesList.size()-1);
                    if (newNode != null) {
                        break;
                    }
                }
            }
            if (prevNode.isFocusTraversable()) {
                newNode = prevNode;
                break;
            }
        }
        return newNode;
    }

    private static boolean isDisabledOrInvisible(Node prevNode) {
        return prevNode.isDisabled() || !prevNode.impl_isTreeVisible();
    }

    static Node findPreviousFocusablePeer(Node node) {
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
        while (newNode == null && startNode != null) {
            List<Node> peerNodes;
            int parentIndex;

            Parent parent = startNode.getParent();
            if (parent != null) {
                // If the parent itself is traversable, select it
                if (parent.isFocusTraversable()) {
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

        // None of the ancestor siblings is traversable, so start from the last traversable item
        if (newNode == null) {
            Parent parent = null;
            Parent p1 = node.getParent();
            while (p1 != null) {
                parent = p1;
                p1 = p1.getParent();
            }

            parentNodes = parent.getChildrenUnmodifiable();
            newNode = findPreviousFocusableInList(parentNodes, parentNodes.size() - 1);
        }

        return newNode;
    }

    static List<Node> findPeers(Node node) {
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

    static Node findNextFocusableInList(List<Node> nodeList, int startIndex) {
        Node newNode = null;

        for (int i = startIndex ; i < nodeList.size() ; i++) {

            Node nextNode = nodeList.get(i);
            if (isDisabledOrInvisible(nextNode)) continue;
            if (nextNode.isFocusTraversable()) {
                newNode = nextNode;
                break;
            }
            else if (nextNode instanceof Parent) {
                List<Node> nextNodesList = ((Parent)nextNode).getChildrenUnmodifiable();
                if (nextNodesList.size() > 0) {
                    newNode = findNextFocusableInList(nextNodesList, 0);
                    if (newNode != null) {
                        break;
                    }
                }
            }
        }
        return newNode;
    }

    static Node findNextFocusablePeer(Node node) {
        Node startNode = node;
        Node newNode = null;

        // First, try to find next peer among the node children
        if (node instanceof Parent) {
            newNode = findNextFocusableInList(((Parent)node).getChildrenUnmodifiable(), 0);
        }

        // Next step is to traverse the siblings "to the right"
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
        while (newNode == null && startNode != null) {
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

        // None of the ancestors siblings is traversable, so find the first traversable Node from the root
        if (newNode == null) {
            Parent parent = null;
            Parent p1 = node.getParent();
            while (p1 != null) {
                parent = p1;
                p1 = p1.getParent();
            }
            List<Node> parentNodes = parent.getChildrenUnmodifiable();
            newNode = findNextFocusableInList(parentNodes, 0);
        }

        return newNode;
    }
}

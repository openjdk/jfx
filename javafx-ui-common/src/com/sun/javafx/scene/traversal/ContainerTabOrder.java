/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import com.sun.javafx.Logging;
import sun.util.logging.PlatformLogger;

import static com.sun.javafx.scene.traversal.Direction.*;

public class ContainerTabOrder implements Algorithm {

    PlatformLogger focusLogger;

    ContainerTabOrder() {
        focusLogger = Logging.getFocusLogger();
    }

    public Node traverse(Node node, Direction dir, TraversalEngine engine) {
        Node newNode = null;
        int newNodeIndex = -1;

        if (focusLogger.isLoggable(PlatformLogger.FINER)) {
            focusLogger.finer("old focus owner : "+node+", bounds : "+engine.getBounds(node));
        }

        if (NEXT.equals(dir)) {
            newNode = findNextFocusablePeer(node);
        }
        else if (PREVIOUS.equals(dir)) {
            newNode = (findPreviousFocusablePeer(node));
        }
        else if (UP.equals(dir) || DOWN.equals(dir) || LEFT.equals(dir) || RIGHT.equals(dir) ) {
            List<Node> nodes = engine.getAllTargetNodes();
            List<Bounds> bounds = engine.getTargetBounds(nodes);

            int target = trav2D(engine.getBounds(node), dir, bounds);
            if (target != -1) {
                newNode = nodes.get(target);
            }
            nodes.clear();
            bounds.clear();

        }

        if (focusLogger.isLoggable(PlatformLogger.FINER)) {
            if (newNode != null) {
                focusLogger.finer("new focus owner : "+newNode+", bounds : "+engine.getBounds(newNode));
            }
            else {
                focusLogger.finer("no focus transfer");
            }
        }

        return newNode;
    }

    private Node findNextFocusablePeer(Node node) {
        Node startNode = node;
        Node newNode = null;
        List<Node> parentNodes = findPeers(startNode);
        if (parentNodes == null) {
            if (focusLogger.isLoggable(PlatformLogger.FINER)) {
                focusLogger.finer("can't find peers for a node without a parent");
            }
            return null;
        }

        int ourIndex = parentNodes.indexOf(startNode);

        if (ourIndex == -1) {
            if (focusLogger.isLoggable(PlatformLogger.FINER)) {
                focusLogger.finer("index not founds, no focus transfer");
            }
            return null;
        }
        
        newNode = findNextFocusableInList(parentNodes, ourIndex+1);

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
                    newNode = findNextFocusableInList(peerNodes, parentIndex+1);
                }
            }
            startNode = parent;
        }
        
        if (newNode == null) {
            /*
            ** find the top-most parent which is not at it's end-of-list
            */
            Parent parent = null;
            Parent p1 = node.getParent();
            while (p1 != null) {
                parent = p1;
                p1 = p1.getParent();
            }
            parentNodes = parent.getChildrenUnmodifiable();
            newNode = findNextFocusableInList(parentNodes, 0);
        }
        
        return newNode;
    }

    private Node findNextParent(Node node) {
        return null;
    }
    
    private Node findNextFocusableInList(List<Node> nodeList, int startIndex) {
        Node newNode = null;

        for (int i = startIndex ; i < nodeList.size() ; i++) {

            Node nextNode = nodeList.get(i);
            if (nextNode.isFocusTraversable() == true && nextNode.isDisabled() == false && nextNode.impl_isTreeVisible() == true) {
                newNode = nextNode;
                break;
            }
            else if (nextNode instanceof javafx.scene.Parent) {
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

    private Node findPreviousFocusablePeer(Node node) {
        Node startNode = node;
        Node newNode = null;
        List<Node> parentNodes = findPeers(startNode);

        int ourIndex = parentNodes.indexOf(startNode);

        if (ourIndex == -1) {
            if (focusLogger.isLoggable(PlatformLogger.FINER)) {
                focusLogger.finer("index not founds, no focus transfer");
            }
            return null;
        }
        
        newNode = findPreviousFocusableInList(parentNodes, ourIndex-1);

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
                    newNode = findPreviousFocusableInList(peerNodes, parentIndex-1);
                }
            }
            startNode = parent;
        }
        
        if (newNode == null) {
            /*
            ** find the top-most parent which is not at it's end-of-list
            */
            Parent parent = null;
            Parent p1 = node.getParent();
            while (p1 != null) {
                parent = p1;
                p1 = p1.getParent();
            }

            parentNodes = parent.getChildrenUnmodifiable();
            newNode = findPreviousFocusableInList(parentNodes, parentNodes.size()-1);
        }
        
        return newNode;
    }


    private Node findPreviousFocusableInList(List<Node> nodeList, int startIndex) {
        Node newNode = null;

        for (int i = startIndex ; i >= 0 ; i--) {
            Node prevNode = nodeList.get(i);
            if (prevNode.isFocusTraversable() == true && prevNode.isDisabled() == false && prevNode.impl_isTreeVisible() == true) {
                newNode = prevNode;
                break;
            }
            else if (prevNode instanceof javafx.scene.Parent) {
                List<Node> prevNodesList = ((Parent)prevNode).getChildrenUnmodifiable();
                if (prevNodesList.size() > 0) {
                    newNode = findPreviousFocusableInList(prevNodesList, prevNodesList.size()-1);
                    if (newNode != null) {
                        break;
                    }
                }
            }
        }
        return newNode;
    }


    private List<Node> findPeers(Node node) {
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

    private static Parent getParent(Node child) {
        return (child.getParent() instanceof Group) ? (child.getParent().getParent()) : (child.getParent());
    }

    private int trav2D(Bounds origin, Direction dir, List<Bounds> targets) {

        Bounds bestBounds = null;
        double bestMetric = 0.0;
        int bestIndex = -1;

        for (int i = 0; i < targets.size(); i++) {
            final Bounds targetBounds = targets.get(i);
            final double outd = outDistance(dir, origin, targetBounds);
            final double metric;

            if (isOnAxis(dir, origin, targetBounds)) {
                metric = outd + centerSideDistance(dir, origin, targetBounds) / 100;
            }
            else {
                final double cosd = cornerSideDistance(dir, origin, targetBounds);
                metric = 100000 + outd*outd + 9*cosd*cosd;
            }

            if (outd < 0.0) {
                continue;
            }

            if (bestBounds == null || metric < bestMetric) {
                bestBounds = targetBounds;
                bestMetric = metric;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private boolean isOnAxis(Direction dir, Bounds cur, Bounds tgt) {

        final double cmin, cmax, tmin, tmax;

        if (dir == UP || dir == DOWN) {
            cmin = cur.getMinX();
            cmax = cur.getMaxX();
            tmin = tgt.getMinX();
            tmax = tgt.getMaxX();
        }
        else { // dir == LEFT || dir == RIGHT
            cmin = cur.getMinY();
            cmax = cur.getMaxY();
            tmin = tgt.getMinY();
            tmax = tgt.getMaxY();
        }

        return tmin <= cmax && tmax >= cmin;
    }

    /**
     * Compute the out-distance to the near edge of the target in the
     * traversal direction. Negative means the near edge is "behind".
     */
    private double outDistance(Direction dir, Bounds cur, Bounds tgt) {

        final double distance;

        if (dir == UP) {
            distance = cur.getMinY() - tgt.getMaxY();
        }
        else if (dir == DOWN) {
            distance = tgt.getMinY() - cur.getMaxY();
        }
        else if (dir == LEFT) {
            distance = cur.getMinX() - tgt.getMaxX();
        }
        else { // dir == RIGHT
            distance = tgt.getMinX() - cur.getMaxX();
        }

        return distance;
    }

    /**
     * Computes the side distance from current center to target center.
     * Always positive. This is only used for on-axis nodes.
     */
    private double centerSideDistance(Direction dir, Bounds cur, Bounds tgt) {

        final double cc; // current center
        final double tc; // target center

        if (dir == UP || dir == DOWN) {
            cc = cur.getMinX() + cur.getWidth() / 2.0f;
            tc = tgt.getMinX() + tgt.getWidth() / 2.0f;
        }
        else { // dir == LEFT || dir == RIGHT
            cc = cur.getMinY() + cur.getHeight() / 2.0f;
            tc = tgt.getMinY() + tgt.getHeight() / 2.0f;
        }

        return Math.abs(tc - cc);
        //return (tc > cc) ? tc - cc : cc - tc;
    }

    /**
     * Computes the side distance between the closest corners of the current
     * and target. Always positive. This is only used for off-axis nodes.
     */
    private double cornerSideDistance(Direction dir, Bounds cur, Bounds tgt) {

        final double distance;

        if (dir == UP || dir == DOWN) {

            if (tgt.getMinX() > cur.getMaxX()) {
                // on the right
                distance = tgt.getMinX() - cur.getMaxX();
            }
            else {
                // on the left
                distance = cur.getMinX() - tgt.getMaxX();
            }
        }
        else { // dir == LEFT or dir == RIGHT

            if (tgt.getMinY() > cur.getMaxY()) {
                // below
                distance = tgt.getMinY() - cur.getMaxY();
            }
            else {
                // above
                distance = cur.getMinY() - tgt.getMaxY();
            }
        }
        return distance;
    }

}

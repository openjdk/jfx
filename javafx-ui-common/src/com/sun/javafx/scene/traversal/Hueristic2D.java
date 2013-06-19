/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Stack;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import com.sun.javafx.Logging;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.Level;

import static com.sun.javafx.scene.traversal.Direction.*;


public class Hueristic2D implements Algorithm {

    PlatformLogger focusLogger;

    Hueristic2D() {
        focusLogger = Logging.getFocusLogger();
    }

    public Node traverse(Node node, Direction dir, TraversalEngine engine) {
        Node newNode = null;
        int newNodeIndex = -1;

        cacheTraversal(node, dir, engine);

        if (focusLogger.isLoggable(Level.FINER)) {
            focusLogger.finer("old focus owner : "+node+", bounds : "+engine.getBounds(node));
        }

        if (NEXT.equals(dir)) {
            newNode = findNextFocusablePeer(node);
        }
        else if (PREVIOUS.equals(dir)) {
            newNode = (findPreviousFocusablePeer(node));
        }
        else if (UP.equals(dir) || DOWN.equals(dir) || LEFT.equals(dir) || RIGHT.equals(dir) ) {
            /*
            ** if there is a node top of stack then make sure it's traversable
            */
            if (reverseDirection == true && !traversalNodeStack.empty()) {
                if (!traversalNodeStack.peek().isFocusTraversable()) {
                    traversalNodeStack.clear();
                }
                else {
                    newNode = traversalNodeStack.pop();
                }
            }

            Bounds currentB = node.localToScene(node.getLayoutBounds());
            BoundingBox bb = null;

            if (cacheStartTraversalNode != null) {
                Bounds cachedB = cacheStartTraversalNode.localToScene(cacheStartTraversalNode.getLayoutBounds());
                switch (dir) {
                case UP:
                    newNode = getNearestNodeUp(currentB, cachedB, engine, node, newNode);
                    break;
                case DOWN:
                    newNode = getNearestNodeDown(currentB, cachedB, engine, node, newNode);
                    break;
                case LEFT:
                    newNode = getNearestNodeLeft(currentB, cachedB, engine, node, newNode);
                    break;
                case RIGHT:
                    newNode = getNearestNodeRight(currentB, cachedB, engine, node, newNode);
                    break;
                default:
                    break;
                }
            }
        }

        if (focusLogger.isLoggable(Level.FINER)) {
            if (newNode != null) {
                focusLogger.finer("new focus owner : "+newNode+", bounds : "+engine.getBounds(newNode));
            }
            else {
                focusLogger.finer("no focus transfer");
            }
        }

        /*
        ** newNode will be null if there are no
        ** possible targets in the direction.
        ** don't cache null, there's no coming back from that!
        */
        if (newNode != null) {
            cacheLastTraversalNode = newNode;
            if (reverseDirection == false) {
                traversalNodeStack.push(node);
            }
        }
        return newNode;
    }

    private Node findNextFocusablePeer(Node node) {
        Node startNode = node;
        Node newNode = null;
        List<Node> parentNodes = findPeers(startNode);
        if (parentNodes == null) {
            if (focusLogger.isLoggable(Level.FINER)) {
                focusLogger.finer("can't find peers for a node without a parent");
            }
            return null;
        }

        int ourIndex = parentNodes.indexOf(startNode);

        if (ourIndex == -1) {
            if (focusLogger.isLoggable(Level.FINER)) {
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
            if (focusLogger.isLoggable(Level.FINER)) {
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

    protected Node cacheStartTraversalNode = null;
    protected Direction cacheStartTraversalDirection = null;
    protected boolean reverseDirection = false;
    protected Node cacheLastTraversalNode = null;
    protected Stack<Node> traversalNodeStack = new Stack();

    private void cacheTraversal(Node node, Direction dir, TraversalEngine engine) {
        if (!traversalNodeStack.empty() && node != cacheLastTraversalNode) {
            /*
            ** we didn't get here by arrow key,
            ** dump the cache
            */
            traversalNodeStack.clear();
        }
        /*
        ** Next or Previous cancels the row caching
        */
        if (dir == Direction.NEXT || dir == Direction.PREVIOUS) {
            traversalNodeStack.clear();
            reverseDirection = false;
        }
        else {
            if (cacheStartTraversalNode == null || dir != cacheStartTraversalDirection) {

                if ((dir == UP && cacheStartTraversalDirection == DOWN) ||
                    (dir == DOWN && cacheStartTraversalDirection == UP) ||
                    (dir == LEFT && cacheStartTraversalDirection == RIGHT) ||
                    (dir == RIGHT && cacheStartTraversalDirection == LEFT) && !traversalNodeStack.empty()) {
                    reverseDirection = true;
                }
                else {
                    /*
                    ** if we don't have a row set, or the direction has changed, then
                    ** make the current node the row.
                    ** otherwise we are moving in the same direction as last time, so
                    ** we'll just leave it alone.
                    */
                    cacheStartTraversalNode = node;
                    cacheStartTraversalDirection = dir;
                    reverseDirection = false;
                    traversalNodeStack.clear();
                }
            }
            else {
                /*
                ** we're going this way again!
                */
                reverseDirection = false;
            }
        }
    }

    protected Node getNearestNodeUp(Bounds currentB, Bounds originB, TraversalEngine engine, Node node, Node reversingNode) {

        List<Node> nodes = engine.getAllTargetNodes();

        Bounds biasedB = new BoundingBox(originB.getMinX(), currentB.getMinY(), originB.getWidth(), currentB.getHeight());

        Point2D currentMid2D = new Point2D(currentB.getMinX()+(currentB.getWidth()/2), currentB.getMinY());
        Point2D currentTopLeft2D = new Point2D(currentB.getMinX(), currentB.getMinY());
        Point2D currentTopRight2D = new Point2D(currentB.getMaxX(), currentB.getMinY());

        Point2D originTopLeft2D = new Point2D(originB.getMinX(), originB.getMinY());

        TargetNode reversingTargetNode = null;
        TargetNode targetNode = new TargetNode();
        TargetNode nearestNodeCurrentSimple2D = null;
        TargetNode nearestNodeOriginSimple2D = null;
        TargetNode nearestNodeAverageUp = null;
        TargetNode nearestNodeOnOriginX = null;
        TargetNode nearestNodeOnCurrentX = null;
        TargetNode nearestNodeTopLeft = null;
        TargetNode nearestNodeAnythingAnywhereUp = null;

        if (nodes.size() > 0) {
            int nodeIndex;

            if (reversingNode != null) {
                Bounds targetBounds = reversingNode.localToScene(reversingNode.getLayoutBounds());
                reversingTargetNode = new TargetNode();

                reversingTargetNode.node = reversingNode;
                reversingTargetNode.bounds = targetBounds;

                /*
                ** closest biased : simple 2d
                */
                double outdB = outDistance(Direction.UP, biasedB, targetBounds);

                if (isOnAxis(Direction.UP, biasedB, targetBounds)) {
                    reversingTargetNode.biased2DMetric = outdB + centerSideDistance(Direction.UP, biasedB, targetBounds) / 100;
                }
                else {
                    final double cosd = cornerSideDistance(Direction.UP, biasedB, targetBounds);
                    reversingTargetNode.biased2DMetric = 100000 + outdB*outdB + 9*cosd*cosd;
                }

                /*
                ** closest current : simple 2d
                */
                double outdC = outDistance(Direction.UP, currentB, targetBounds);

                if (isOnAxis(Direction.UP, currentB, targetBounds)) {
                    reversingTargetNode.current2DMetric = outdC + centerSideDistance(Direction.UP, currentB, targetBounds) / 100;
                }
                else {
                    final double cosd = cornerSideDistance(Direction.UP, currentB, targetBounds);
                    reversingTargetNode.current2DMetric = 100000 + outdC*outdC + 9*cosd*cosd;
                }


                reversingTargetNode.bottomLeftDistance = currentTopLeft2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                reversingTargetNode.midDistance = currentMid2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMaxY());
                reversingTargetNode.bottomRightDistance = currentTopRight2D.distance(originB.getMaxX(), targetBounds.getMaxY());

                double currentTopLeftToTargetMidDistance = currentTopLeft2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMaxY());
                double currentTopLeftToTargetBottomRightDistance = currentTopLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());
                double currentTopRightToTargetBottomLeftDistance = currentTopRight2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                double currentTopRightToTargetMidDistance = currentTopRight2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMaxY());
                double currentTopRightToTargetBottomRightDistance = currentTopRight2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());
                double currentMidToTargetBottomLeftDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                double currentMidToTargetMidDistance = currentMid2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMaxY());
                double currentMidToTargetBottomRightDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());

                double biasTopLeftToTargetMidDistance = currentTopLeft2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMaxY());
                double biasTopLeftToTargetBottomRightDistance = currentTopLeft2D.distance(originB.getMaxX(), targetBounds.getMaxY());
                double biasTopRightToTargetMidDistance = currentTopRight2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMaxY());
                double biasMidToTargetBottomRightDistance = currentMid2D.distance(originB.getMaxX(), targetBounds.getMaxY());

                reversingTargetNode.averageDistance =
                    (reversingTargetNode.bottomLeftDistance+biasTopLeftToTargetMidDistance+biasTopLeftToTargetBottomRightDistance+
                     currentTopRightToTargetBottomLeftDistance+reversingTargetNode.bottomRightDistance+biasTopRightToTargetMidDistance+reversingTargetNode.midDistance)/7;

                reversingTargetNode.biasShortestDistance =
                    findMin9(reversingTargetNode.bottomLeftDistance, biasTopLeftToTargetMidDistance, biasTopLeftToTargetBottomRightDistance,
                             currentTopRightToTargetBottomLeftDistance, biasTopRightToTargetMidDistance, reversingTargetNode.bottomRightDistance,
                             currentMidToTargetBottomLeftDistance, reversingTargetNode.midDistance, biasMidToTargetBottomRightDistance);

                reversingTargetNode.shortestDistance =
                    findMin9(reversingTargetNode.bottomLeftDistance, currentTopLeftToTargetMidDistance, currentTopLeftToTargetBottomRightDistance,
                             currentTopRightToTargetBottomLeftDistance, currentTopRightToTargetMidDistance, currentTopRightToTargetBottomRightDistance,
                             currentMidToTargetBottomLeftDistance, currentMidToTargetMidDistance, currentMidToTargetBottomRightDistance);

            }

            for (nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {

                Bounds targetBounds = nodes.get(nodeIndex).localToScene(nodes.get(nodeIndex).getLayoutBounds());
                /*
                ** check that the target node starts after we start target.minX > origin.minX
                ** and the target node ends after we end target.maxX > origin.maxX
                */
                if ((currentB.getMinY() > targetBounds.getMaxY())) {

                    targetNode.node = nodes.get(nodeIndex);
                    targetNode.bounds = targetBounds;

                    /*
                    ** closest biased : simple 2d
                    */
                    double outdB = outDistance(Direction.UP, biasedB, targetBounds);

                    if (isOnAxis(Direction.UP, biasedB, targetBounds)) {
                        targetNode.biased2DMetric = outdB + centerSideDistance(Direction.UP, biasedB, targetBounds) / 100;
                    }
                    else {
                        final double cosd = cornerSideDistance(Direction.UP, biasedB, targetBounds);
                        targetNode.biased2DMetric = 100000 + outdB*outdB + 9*cosd*cosd;
                    }
                    /*
                    ** closest current : simple 2d
                    */
                    double outdC = outDistance(Direction.UP, currentB, targetBounds);

                    if (isOnAxis(Direction.UP, currentB, targetBounds)) {
                        targetNode.current2DMetric = outdC + centerSideDistance(Direction.UP, currentB, targetBounds) / 100;
                    }
                    else {
                        final double cosd = cornerSideDistance(Direction.UP, currentB, targetBounds);
                        targetNode.current2DMetric = 100000 + outdC*outdC + 9*cosd*cosd;
                    }


                    targetNode.bottomLeftDistance = currentTopLeft2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                    targetNode.midDistance = currentMid2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMaxY());
                    targetNode.bottomRightDistance = currentTopRight2D.distance(originB.getMaxX(), targetBounds.getMaxY());

                    double currentTopLeftToTargetMidDistance = currentTopLeft2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMaxY());
                    double currentTopLeftToTargetBottomRightDistance = currentTopLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());
                    double currentTopRightToTargetBottomLeftDistance = currentTopRight2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                    double currentTopRightToTargetMidDistance = currentTopRight2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMaxY());
                    double currentTopRightToTargetBottomRightDistance = currentTopRight2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());
                    double currentMidToTargetBottomLeftDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                    double currentMidToTargetMidDistance = currentMid2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMaxY());
                    double currentMidToTargetBottomRightDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());

                    double biasTopLeftToTargetMidDistance = currentTopLeft2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMaxY());
                    double biasTopLeftToTargetBottomRightDistance = currentTopLeft2D.distance(originB.getMaxX(), targetBounds.getMaxY());
                    double biasTopRightToTargetMidDistance = currentTopRight2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMaxY());
                    double biasMidToTargetBottomRightDistance = currentMid2D.distance(originB.getMaxX(), targetBounds.getMaxY());

                    targetNode.averageDistance =
                        (targetNode.bottomLeftDistance+biasTopLeftToTargetMidDistance+biasTopLeftToTargetBottomRightDistance+
                         currentTopRightToTargetBottomLeftDistance+targetNode.bottomRightDistance+biasTopRightToTargetMidDistance+targetNode.midDistance)/7;

                    targetNode.biasShortestDistance =
                        findMin9(targetNode.bottomLeftDistance, biasTopLeftToTargetMidDistance, biasTopLeftToTargetBottomRightDistance,
                                 currentTopRightToTargetBottomLeftDistance, biasTopRightToTargetMidDistance, targetNode.bottomRightDistance,
                                 currentMidToTargetBottomLeftDistance, targetNode.midDistance, biasMidToTargetBottomRightDistance);

                    targetNode.shortestDistance =
                        findMin9(targetNode.bottomLeftDistance, currentTopLeftToTargetMidDistance, currentTopLeftToTargetBottomRightDistance,
                                 currentTopRightToTargetBottomLeftDistance, currentTopRightToTargetMidDistance, currentTopRightToTargetBottomRightDistance,
                                 currentMidToTargetBottomLeftDistance, currentMidToTargetMidDistance, currentMidToTargetBottomRightDistance);

                    /*
                    ** closest biased : simple 2d
                    */
                    if (outdC >= 0.0) {
                        if (nearestNodeOriginSimple2D == null || targetNode.biased2DMetric < nearestNodeOriginSimple2D.biased2DMetric) {

                            if (nearestNodeOriginSimple2D == null) {
                                nearestNodeOriginSimple2D = new TargetNode();
                            }
                            nearestNodeOriginSimple2D.copy(targetNode);
                        }
                    }
                    /*
                    ** closest current : simple 2d
                    */
                    if (outdC >= 0.0) {
                        if (nearestNodeCurrentSimple2D == null || targetNode.current2DMetric < nearestNodeCurrentSimple2D.current2DMetric) {

                            if (nearestNodeCurrentSimple2D == null) {
                                nearestNodeCurrentSimple2D = new TargetNode();
                            }
                            nearestNodeCurrentSimple2D.copy(targetNode);
                        }
                    }
                    /*
                    ** on the Origin X
                    */
                    if ((originB.getMaxX() > targetBounds.getMinX()) && (targetBounds.getMaxX() > originB.getMinX())) {
                        if (nearestNodeOnOriginX == null || nearestNodeOnOriginX.biasShortestDistance > targetNode.biasShortestDistance) {

                            if (nearestNodeOnOriginX == null) {
                                nearestNodeOnOriginX = new TargetNode();
                            }
                            nearestNodeOnOriginX.copy(targetNode);
                        }
                    }
                    /*
                    ** on the Current X
                    */
                    if ((currentB.getMaxX() > targetBounds.getMinX()) && (targetBounds.getMaxX() > currentB.getMinX())) {
                        if (nearestNodeOnCurrentX == null || nearestNodeOnCurrentX.biasShortestDistance > targetNode.biasShortestDistance) {

                            if (nearestNodeOnCurrentX == null) {
                                nearestNodeOnCurrentX = new TargetNode();
                            }
                            nearestNodeOnCurrentX.copy(targetNode);
                        }
                    }
                    /*
                    ** Closest top left / bottom left corners.
                    */
                    if (nearestNodeTopLeft == null || nearestNodeTopLeft.bottomLeftDistance > targetNode.bottomLeftDistance) {
                        if (((originB.getMinY() >= currentB.getMinY()) && (targetBounds.getMinY() >= currentB.getMinY()))  ||
                            ((originB.getMinY() <= currentB.getMinY()) && (targetBounds.getMinY() <= currentB.getMinY()))) {

                            if (nearestNodeTopLeft == null) {
                                nearestNodeTopLeft = new TargetNode();
                            }
                            nearestNodeTopLeft.copy(targetNode);
                        }
                    }

                    if (nearestNodeAverageUp == null || nearestNodeAverageUp.averageDistance > targetNode.averageDistance) {
                        if (((originB.getMinX() >= currentB.getMinX()) && (targetBounds.getMinX() >= currentB.getMinX()))  ||
                            ((originB.getMinX() <= currentB.getMinX()) && (targetBounds.getMinX() <= currentB.getMinX()))) {

                            if (nearestNodeAverageUp == null) {
                                nearestNodeAverageUp = new TargetNode();
                            }
                            nearestNodeAverageUp.copy(targetNode);
                        }
                    }

                    if (nearestNodeAnythingAnywhereUp == null || nearestNodeAnythingAnywhereUp.shortestDistance > targetNode.shortestDistance) {

                        if (nearestNodeAnythingAnywhereUp == null) {
                            nearestNodeAnythingAnywhereUp = new TargetNode();
                        }
                        nearestNodeAnythingAnywhereUp.copy(targetNode);
                    }
                }
            }
        }
        nodes.clear();

        if (reversingTargetNode != null) {
            reversingTargetNode.originTopLeftDistance = originTopLeft2D.distance(reversingTargetNode.bounds.getMinX(), reversingTargetNode.bounds.getMaxY());
        }

        if (nearestNodeOriginSimple2D != null) {
            nearestNodeOriginSimple2D.originTopLeftDistance = originTopLeft2D.distance(nearestNodeOriginSimple2D.bounds.getMinX(), nearestNodeOriginSimple2D.bounds.getMaxY());
        }

        if (nearestNodeCurrentSimple2D != null) {
            nearestNodeCurrentSimple2D.originTopLeftDistance = originTopLeft2D.distance(nearestNodeCurrentSimple2D.bounds.getMinX(), nearestNodeCurrentSimple2D.bounds.getMaxY());
        }

        if (nearestNodeOnOriginX != null) {
            nearestNodeOnOriginX.originTopLeftDistance = originTopLeft2D.distance(nearestNodeOnOriginX.bounds.getMinX(), nearestNodeOnOriginX.bounds.getMaxY());
        }

        if (nearestNodeOnCurrentX != null) {
            nearestNodeOnCurrentX.originTopLeftDistance = originTopLeft2D.distance(nearestNodeOnCurrentX.bounds.getMinX(), nearestNodeOnCurrentX.bounds.getMaxY());
        }

        if (nearestNodeAverageUp != null) {
            nearestNodeAverageUp.originTopLeftDistance = originTopLeft2D.distance(nearestNodeAverageUp.bounds.getMinX(), nearestNodeAverageUp.bounds.getMaxY());
        }

        if (nearestNodeTopLeft != null) {
            nearestNodeTopLeft.originTopLeftDistance = originTopLeft2D.distance(nearestNodeTopLeft.bounds.getMinX(), nearestNodeTopLeft.bounds.getMaxY());
        }

        if (nearestNodeAnythingAnywhereUp != null) {
            nearestNodeAnythingAnywhereUp.originTopLeftDistance = originTopLeft2D.distance(nearestNodeAnythingAnywhereUp.bounds.getMinX(), nearestNodeAnythingAnywhereUp.bounds.getMaxY());
        }

        if (focusLogger.isLoggable(Level.FINER)) {
            if (reversingTargetNode != null) {
                focusLogger.finer("reversingTargetNode.node : "+reversingTargetNode.node);
            }
            if (nearestNodeOriginSimple2D != null) {
                focusLogger.finer("nearestNodeOriginSimple2D.node : "+nearestNodeOriginSimple2D.node);
            }
            if (nearestNodeCurrentSimple2D != null) {
                focusLogger.finer("nearestNodeCurrentSimple2D.node : "+nearestNodeCurrentSimple2D.node);
            }
            if (nearestNodeOnOriginX != null) {
                focusLogger.finer("nearestNodeOnOriginX.node : "+nearestNodeOnOriginX.node);
            }
            if (nearestNodeOnCurrentX != null) {
                focusLogger.finer("nearestNodeOnCurrentX.node : "+nearestNodeOnCurrentX.node);
            }
            if (nearestNodeAverageUp != null) {
                focusLogger.finer("nearestNodeAverageUp.node : "+nearestNodeAverageUp.node);
            }
            if (nearestNodeTopLeft != null) {
                focusLogger.finer("nearestNodeTopLeft.node : "+nearestNodeTopLeft.node);
            }
            if (nearestNodeAnythingAnywhereUp != null) {
                focusLogger.finer("nearestNodeAnythingAnywhereUp.node : "+nearestNodeAnythingAnywhereUp.node);
            }
        }

        /*
        ** we've just changed direction, and have a node on stack.
        ** there is a strong preference for this node, just make sure
        ** it's not a bad choice, as sometimes we got here as a last-chance
        */
        if (reversingTargetNode != null) {
            return reversingNode;
        }

        if (nearestNodeOnOriginX != null && nearestNodeOnOriginX.biasShortestDistance < Double.MAX_VALUE) {
            /*
            ** there's a preference, all else being equal, to return nearestNodeOnOriginX
            */
            if (nearestNodeOnCurrentX != null && nearestNodeAverageUp != null &&
                nearestNodeOnOriginX.node == nearestNodeOnCurrentX.node && nearestNodeOnOriginX.node == nearestNodeAverageUp.node) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeOnCurrentX != null && nearestNodeOriginSimple2D != null &&
                nearestNodeOnOriginX.node == nearestNodeOnCurrentX.node && nearestNodeOnOriginX.node == nearestNodeOriginSimple2D.node) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeOnCurrentX != null && nearestNodeTopLeft != null &&
                nearestNodeOnOriginX.node == nearestNodeOnCurrentX.node && nearestNodeOnOriginX.node == nearestNodeTopLeft.node) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeOnCurrentX != null && nearestNodeAnythingAnywhereUp != null &&
                nearestNodeOnOriginX.node == nearestNodeOnCurrentX.node && nearestNodeOnOriginX.node == nearestNodeAnythingAnywhereUp.node) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeAverageUp != null && nearestNodeOnOriginX.node == nearestNodeAverageUp.node) {
                return nearestNodeOnOriginX.node;
            }

            if (nearestNodeOnCurrentX != null && nearestNodeOnCurrentX.biasShortestDistance < Double.MAX_VALUE) {
                if (nearestNodeOnOriginX == null || (nearestNodeOnCurrentX.bottomLeftDistance < nearestNodeOnOriginX.bottomLeftDistance) &&
                    (nearestNodeOnCurrentX.originTopLeftDistance < nearestNodeOnOriginX.originTopLeftDistance) &&
                    (nearestNodeOnCurrentX.bounds.getMinX() - currentTopLeft2D.getX()) < (nearestNodeOnOriginX.bounds.getMinX() - currentTopLeft2D.getX()) )  {

                    return nearestNodeOnCurrentX.node;
                }
                else if (nearestNodeOnOriginX != null) {
                    if (nearestNodeAverageUp == null || nearestNodeOnOriginX.averageDistance < nearestNodeAverageUp.averageDistance) {
                        return nearestNodeOnOriginX.node;
                    }
                }
            }

        }
        else {
            if (nearestNodeOnOriginX == null && nearestNodeOnCurrentX == null && nearestNodeCurrentSimple2D != null) {
                if (nearestNodeAverageUp != null && nearestNodeTopLeft != null && nearestNodeAnythingAnywhereUp != null && (nearestNodeAverageUp.node == nearestNodeTopLeft.node && nearestNodeAverageUp.node == nearestNodeAnythingAnywhereUp.node)) {
                    return nearestNodeAverageUp.node;
                }
                return nearestNodeCurrentSimple2D.node;
            }
            else if (nearestNodeAverageUp != null && nearestNodeTopLeft != null && nearestNodeAnythingAnywhereUp != null &&
                     nearestNodeAverageUp.biasShortestDistance == nearestNodeTopLeft.biasShortestDistance &&
                     nearestNodeAverageUp.biasShortestDistance == nearestNodeAnythingAnywhereUp.biasShortestDistance &&
                     nearestNodeAverageUp.biasShortestDistance < Double.MAX_VALUE) {

                if (nearestNodeOnOriginX != null && nearestNodeOnOriginX.originTopLeftDistance < nearestNodeAverageUp.originTopLeftDistance) {
                    return nearestNodeOnOriginX.node;
                }
                else {
                    return nearestNodeAverageUp.node;
                }
            }
        }

        /*
        ** is the average closer?
        */
        if (nearestNodeAverageUp != null && (nearestNodeOnOriginX == null || (nearestNodeAverageUp.biasShortestDistance < nearestNodeOnOriginX.biasShortestDistance))) {
            /*
            ** but is one in the way
            */
            if (nearestNodeOnOriginX != null && (nearestNodeOnOriginX.bounds.getMaxY() >= nearestNodeAverageUp.bounds.getMaxY())) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeOriginSimple2D != null) {
                if (nearestNodeOriginSimple2D.current2DMetric <= nearestNodeAverageUp.current2DMetric) {
                    return nearestNodeOriginSimple2D.node;
                }
                if (nearestNodeOriginSimple2D.bounds.getMaxY() >= nearestNodeAverageUp.bounds.getMaxY()) {
                    return nearestNodeOriginSimple2D.node;
                }
            }
            return nearestNodeAverageUp.node;
        }

        /*
        ** this is an odd one, in that is isn't the closest on current, or on the
        ** origin, but it looks better for most cases...
        */
        if ((nearestNodeCurrentSimple2D != null && nearestNodeOnCurrentX != null && nearestNodeAverageUp != null && nearestNodeTopLeft != null && nearestNodeAnythingAnywhereUp != null) &&
            (nearestNodeCurrentSimple2D.node == nearestNodeOnCurrentX.node) &&
            (nearestNodeCurrentSimple2D.node ==  nearestNodeAverageUp.node) &&
            (nearestNodeCurrentSimple2D.node == nearestNodeTopLeft.node) &&
            (nearestNodeCurrentSimple2D.node == nearestNodeAnythingAnywhereUp.node)) {
            return nearestNodeCurrentSimple2D.node;
        }

        if (nearestNodeOnOriginX != null && (nearestNodeOnCurrentX == null || (nearestNodeOnOriginX.bottomRightDistance < nearestNodeOnCurrentX.bottomRightDistance))) {
            return nearestNodeOnOriginX.node;
        }
        /*
        ** There isn't a clear winner, just go to the one nearest the current
        ** focus owner, or if invalid then try the other contenders.
        */
        if (nearestNodeOnOriginX != null) {
            return nearestNodeOnOriginX.node;
        }
        else if (nearestNodeOriginSimple2D != null) {
            return nearestNodeOriginSimple2D.node;
        }
        else if (nearestNodeOnCurrentX != null) {
            return nearestNodeOnCurrentX.node;
        }
        else if (nearestNodeAverageUp != null) {
            return nearestNodeAverageUp.node;
        }
        else if (nearestNodeTopLeft != null) {
            return nearestNodeTopLeft.node;
        }
        else if (nearestNodeAnythingAnywhereUp != null) {
            return nearestNodeAnythingAnywhereUp.node;
        }
        return null;
    }


    protected Node getNearestNodeDown(Bounds currentB, Bounds originB, TraversalEngine engine, Node node, Node reversingNode) {

        List<Node> nodes = engine.getAllTargetNodes();

        Bounds biasedB = new BoundingBox(originB.getMinX(), currentB.getMinY(), originB.getWidth(), currentB.getHeight());

        Point2D currentMid2D = new Point2D(currentB.getMinX()+(currentB.getWidth()/2), currentB.getMaxY());
        Point2D currentBottomLeft2D = new Point2D(currentB.getMinX(), currentB.getMaxY());
        Point2D currentBottomRight2D = new Point2D(currentB.getMaxX(), currentB.getMaxY());

        Point2D originBottomLeft2D = new Point2D(originB.getMinX(), originB.getMaxY());

        TargetNode reversingTargetNode = null;
        TargetNode targetNode = new TargetNode();
        TargetNode nearestNodeCurrentSimple2D = null;
        TargetNode nearestNodeOriginSimple2D = null;
        TargetNode nearestNodeAverageDown = null;
        TargetNode nearestNodeOnOriginX = null;
        TargetNode nearestNodeOnCurrentX = null;
        TargetNode nearestNodeBottomLeft = null;
        TargetNode nearestNodeAnythingAnywhereDown = null;

        if (nodes.size() > 0) {
            int nodeIndex;

            if (reversingNode != null) {
                Bounds targetBounds = reversingNode.localToScene(reversingNode.getLayoutBounds());
                reversingTargetNode = new TargetNode();

                reversingTargetNode.node = reversingNode;
                reversingTargetNode.bounds = targetBounds;

                /*
                ** closest biased : simple 2d
                */
                double outdB = outDistance(Direction.DOWN, biasedB, targetBounds);

                if (isOnAxis(Direction.DOWN, biasedB, targetBounds)) {
                    reversingTargetNode.biased2DMetric = outdB + centerSideDistance(Direction.DOWN, biasedB, targetBounds) / 100;
                }
                else {
                    final double cosd = cornerSideDistance(Direction.DOWN, biasedB, targetBounds);
                    reversingTargetNode.biased2DMetric = 100000 + outdB*outdB + 9*cosd*cosd;
                }

                /*
                ** closest current : simple 2d
                */
                double outdC = outDistance(Direction.DOWN, currentB, targetBounds);

                if (isOnAxis(Direction.DOWN, currentB, targetBounds)) {
                    reversingTargetNode.current2DMetric = outdC + centerSideDistance(Direction.DOWN, currentB, targetBounds) / 100;
                }
                else {
                    final double cosd = cornerSideDistance(Direction.DOWN, currentB, targetBounds);
                    reversingTargetNode.current2DMetric = 100000 + outdC*outdC + 9*cosd*cosd;
                }

                reversingTargetNode.topLeftDistance = currentBottomLeft2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                reversingTargetNode.midDistance = currentMid2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMinY());
                reversingTargetNode.topRightDistance = currentBottomRight2D.distance(originB.getMaxX(), targetBounds.getMinY());

                double currentBottomLeftToTargetTopRightDistance = currentBottomLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                double currentBottomLeftToTargetMidDistance = currentBottomLeft2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMinY());
                double currentBottomRightToTargetTopLeftDistance = currentBottomRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                double currentBottomRightToTargetTopRightDistance = currentBottomRight2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                double currentBottomRightToTargetMidDistance = currentBottomRight2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMinY());
                double currentMidToTargetTopLeftDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                double currentMidToTargetTopRightDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                double currentMidToTargetMidDistance = currentMid2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMinY());

                double biasBottomLeftToTargetTopRightDistance = currentBottomLeft2D.distance(originB.getMaxX(), targetBounds.getMinY());
                double biasBottomLeftToTargetMidDistance = currentBottomLeft2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMinY());
                double biasBottomRightToTargetMidDistance = currentBottomRight2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMinY());
                double biasMidToTargetTopRightDistance = currentMid2D.distance(originB.getMaxX(), targetBounds.getMinY());

                reversingTargetNode.averageDistance =
                    (reversingTargetNode.topLeftDistance+biasBottomLeftToTargetMidDistance+biasBottomLeftToTargetTopRightDistance+
                     currentBottomRightToTargetTopLeftDistance+reversingTargetNode.topRightDistance+biasBottomRightToTargetMidDistance+reversingTargetNode.midDistance)/7;

                reversingTargetNode.biasShortestDistance =
                    findMin9(reversingTargetNode.topLeftDistance, biasBottomLeftToTargetMidDistance, biasBottomLeftToTargetTopRightDistance,
                             currentBottomRightToTargetTopLeftDistance, biasBottomRightToTargetMidDistance, reversingTargetNode.topRightDistance,
                             currentMidToTargetTopLeftDistance, reversingTargetNode.midDistance, biasMidToTargetTopRightDistance);

                reversingTargetNode.shortestDistance =
                    findMin9(reversingTargetNode.topLeftDistance, currentBottomLeftToTargetMidDistance, currentBottomLeftToTargetTopRightDistance,
                             currentBottomRightToTargetTopLeftDistance, currentBottomRightToTargetMidDistance, currentBottomRightToTargetTopRightDistance,
                             currentMidToTargetTopLeftDistance, currentMidToTargetMidDistance, currentMidToTargetTopRightDistance);

            }

            for (nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {

                Bounds targetBounds = nodes.get(nodeIndex).localToScene(nodes.get(nodeIndex).getLayoutBounds());
                /*
                ** check that the target node starts after we start target.minX > origin.minX
                ** and the target node ends after we end target.maxX > origin.maxX
                */
                if ((currentB.getMaxY() < targetBounds.getMinY())) {

                    targetNode.node = nodes.get(nodeIndex);
                    targetNode.bounds = targetBounds;

                    /*
                    ** closest biased : simple 2d
                    */
                    double outdB = outDistance(Direction.DOWN, biasedB, targetBounds);

                    if (isOnAxis(Direction.DOWN, biasedB, targetBounds)) {
                        targetNode.biased2DMetric = outdB + centerSideDistance(Direction.DOWN, biasedB, targetBounds) / 100;
                    }
                    else {
                        final double cosd = cornerSideDistance(Direction.DOWN, biasedB, targetBounds);
                        targetNode.biased2DMetric = 100000 + outdB*outdB + 9*cosd*cosd;
                    }
                    /*
                    ** closest current : simple 2d
                    */
                    double outdC = outDistance(Direction.DOWN, currentB, targetBounds);

                    if (isOnAxis(Direction.DOWN, currentB, targetBounds)) {
                        targetNode.current2DMetric = outdC + centerSideDistance(Direction.DOWN, currentB, targetBounds) / 100;
                    }
                    else {
                        final double cosd = cornerSideDistance(Direction.DOWN, currentB, targetBounds);
                        targetNode.current2DMetric = 100000 + outdC*outdC + 9*cosd*cosd;
                    }

                    targetNode.topLeftDistance = currentBottomLeft2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                    targetNode.midDistance = currentMid2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMinY());
                    targetNode.topRightDistance = currentBottomRight2D.distance(originB.getMaxX(), targetBounds.getMinY());

                    double currentBottomLeftToTargetTopRightDistance = currentBottomLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                    double currentBottomLeftToTargetMidDistance = currentBottomLeft2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMinY());
                    double currentBottomRightToTargetTopLeftDistance = currentBottomRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                    double currentBottomRightToTargetTopRightDistance = currentBottomRight2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                    double currentBottomRightToTargetMidDistance = currentBottomRight2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMinY());
                    double currentMidToTargetTopLeftDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                    double currentMidToTargetTopRightDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                    double currentMidToTargetMidDistance = currentMid2D.distance(targetBounds.getMinX()+(targetBounds.getWidth()/2), targetBounds.getMinY());

                    double biasBottomLeftToTargetTopRightDistance = currentBottomLeft2D.distance(originB.getMaxX(), targetBounds.getMinY());
                    double biasBottomLeftToTargetMidDistance = currentBottomLeft2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMinY());
                    double biasBottomRightToTargetMidDistance = currentBottomRight2D.distance(targetBounds.getMinX()+(originB.getWidth()/2), targetBounds.getMinY());
                    double biasMidToTargetTopRightDistance = currentMid2D.distance(originB.getMaxX(), targetBounds.getMinY());

                    targetNode.averageDistance =
                        (targetNode.topLeftDistance+biasBottomLeftToTargetMidDistance+biasBottomLeftToTargetTopRightDistance+
                         currentBottomRightToTargetTopLeftDistance+targetNode.topRightDistance+biasBottomRightToTargetMidDistance+targetNode.midDistance)/7;

                    targetNode.biasShortestDistance =
                        findMin9(targetNode.topLeftDistance, biasBottomLeftToTargetMidDistance, biasBottomLeftToTargetTopRightDistance,
                                 currentBottomRightToTargetTopLeftDistance, biasBottomRightToTargetMidDistance, targetNode.topRightDistance,
                                 currentMidToTargetTopLeftDistance, targetNode.midDistance, biasMidToTargetTopRightDistance);

                    targetNode.shortestDistance =
                        findMin9(targetNode.topLeftDistance, currentBottomLeftToTargetMidDistance, currentBottomLeftToTargetTopRightDistance,
                                 currentBottomRightToTargetTopLeftDistance, currentBottomRightToTargetMidDistance, currentBottomRightToTargetTopRightDistance,
                                 currentMidToTargetTopLeftDistance, currentMidToTargetMidDistance, currentMidToTargetTopRightDistance);


                    /*
                    ** closest biased : simple 2d
                    */
                    if (outdB >= 0.0) {
                        if (nearestNodeOriginSimple2D == null || targetNode.biased2DMetric < nearestNodeOriginSimple2D.biased2DMetric) {

                            if (nearestNodeOriginSimple2D == null) {
                                nearestNodeOriginSimple2D = new TargetNode();
                            }
                            nearestNodeOriginSimple2D.copy(targetNode);
                        }
                    }
                    /*
                    ** closest current : simple 2d
                    */
                    if (outdC >= 0.0) {
                        if (nearestNodeCurrentSimple2D == null || targetNode.current2DMetric < nearestNodeCurrentSimple2D.current2DMetric) {

                            if (nearestNodeCurrentSimple2D == null) {
                                nearestNodeCurrentSimple2D = new TargetNode();
                            }
                            nearestNodeCurrentSimple2D.copy(targetNode);
                        }
                    }
                    /*
                    ** on the Origin X
                    */
                    if ((originB.getMaxX() > targetBounds.getMinX()) && (targetBounds.getMaxX() > originB.getMinX())) {
                        if (nearestNodeOnOriginX == null || nearestNodeOnOriginX.biasShortestDistance > targetNode.biasShortestDistance) {

                            if (nearestNodeOnOriginX == null) {
                                nearestNodeOnOriginX = new TargetNode();
                            }
                            nearestNodeOnOriginX.copy(targetNode);
                        }
                    }
                    /*
                    ** on the Current X
                    */
                    if ((currentB.getMaxX() > targetBounds.getMinX()) && (targetBounds.getMaxX() > currentB.getMinX())) {
                        if (nearestNodeOnCurrentX == null || nearestNodeOnCurrentX.biasShortestDistance > targetNode.biasShortestDistance) {

                            if (nearestNodeOnCurrentX == null) {
                                nearestNodeOnCurrentX = new TargetNode();
                            }
                            nearestNodeOnCurrentX.copy(targetNode);
                        }
                    }
                    /*
                    ** Closest bottom left / top left corners.
                    */
                    if (nearestNodeBottomLeft == null || nearestNodeBottomLeft.topLeftDistance > targetNode.topLeftDistance) {
                        if (((originB.getMinX() >= currentB.getMinX()) && (targetBounds.getMinX() >= currentB.getMinX()))  ||
                            ((originB.getMinX() <= currentB.getMinX()) && (targetBounds.getMinX() <= currentB.getMinX()))) {

                            if (nearestNodeBottomLeft == null) {
                                nearestNodeBottomLeft = new TargetNode();
                            }
                            nearestNodeBottomLeft.copy(targetNode);
                        }
                    }

                    if (nearestNodeAverageDown == null || nearestNodeAverageDown.averageDistance > targetNode.averageDistance) {
                        if (((originB.getMinX() >= currentB.getMinX()) && (targetBounds.getMinX() >= currentB.getMinX()))  ||
                            ((originB.getMinX() <= currentB.getMinX()) && (targetBounds.getMinX() <= currentB.getMinX()))) {

                            if (nearestNodeAverageDown == null) {
                                nearestNodeAverageDown = new TargetNode();
                            }
                            nearestNodeAverageDown.copy(targetNode);
                        }
                    }

                    if (nearestNodeAnythingAnywhereDown == null || nearestNodeAnythingAnywhereDown.shortestDistance > targetNode.shortestDistance) {
                        if (nearestNodeAnythingAnywhereDown == null) {
                            nearestNodeAnythingAnywhereDown = new TargetNode();
                        }
                        nearestNodeAnythingAnywhereDown.copy(targetNode);
                    }
                }
            }
        }
        nodes.clear();

        if (reversingTargetNode != null) {
            reversingTargetNode.originBottomLeftDistance = originBottomLeft2D.distance(reversingTargetNode.bounds.getMinX(), reversingTargetNode.bounds.getMinY());
        }
        if (nearestNodeOriginSimple2D != null) {
            nearestNodeOriginSimple2D.originBottomLeftDistance = originBottomLeft2D.distance(nearestNodeOriginSimple2D.bounds.getMinX(), nearestNodeOriginSimple2D.bounds.getMinY());
        }
        if (nearestNodeCurrentSimple2D != null) {
            nearestNodeCurrentSimple2D.originBottomLeftDistance = originBottomLeft2D.distance(nearestNodeCurrentSimple2D.bounds.getMinX(), nearestNodeCurrentSimple2D.bounds.getMinY());
        }
        if (nearestNodeOnOriginX != null) {
            nearestNodeOnOriginX.originBottomLeftDistance = originBottomLeft2D.distance(nearestNodeOnOriginX.bounds.getMinX(), nearestNodeOnOriginX.bounds.getMinY());
        }
        if (nearestNodeOnCurrentX != null) {
            nearestNodeOnCurrentX.originBottomLeftDistance = originBottomLeft2D.distance(nearestNodeOnCurrentX.bounds.getMinX(), nearestNodeOnCurrentX.bounds.getMinY());
        }
        if (nearestNodeAverageDown != null) {
            nearestNodeAverageDown.originBottomLeftDistance = originBottomLeft2D.distance(nearestNodeAverageDown.bounds.getMinX(), nearestNodeAverageDown.bounds.getMinY());
        }
        if (nearestNodeBottomLeft != null) {
            nearestNodeBottomLeft.originBottomLeftDistance = originBottomLeft2D.distance(nearestNodeBottomLeft.bounds.getMinX(), nearestNodeBottomLeft.bounds.getMinY());
        }
        if (nearestNodeAnythingAnywhereDown != null) {
            nearestNodeAnythingAnywhereDown.originBottomLeftDistance = originBottomLeft2D.distance(nearestNodeAnythingAnywhereDown.bounds.getMinX(), nearestNodeAnythingAnywhereDown.bounds.getMinY());
        }

        if (focusLogger.isLoggable(Level.FINER)) {
            if (reversingTargetNode != null) {
                focusLogger.finer("reversingTargetNode.node : "+reversingTargetNode.node);
            }
            if (nearestNodeOriginSimple2D != null) {
                focusLogger.finer("nearestNodeOriginSimple2D.node : "+nearestNodeOriginSimple2D.node);
            }
            if (nearestNodeCurrentSimple2D != null) {
                focusLogger.finer("nearestNodeCurrentSimple2D.node : "+nearestNodeCurrentSimple2D.node);
            }
            if (nearestNodeOnOriginX != null) {
                focusLogger.finer("nearestNodeOnOriginX.node : "+nearestNodeOnOriginX.node);
            }
            if (nearestNodeOnCurrentX != null) {
                focusLogger.finer("nearestNodeOnCurrentX.node : "+nearestNodeOnCurrentX.node);
            }
            if (nearestNodeAverageDown != null) {
                focusLogger.finer("nearestNodeAverageDown.node : "+nearestNodeAverageDown.node);
            }
            if (nearestNodeBottomLeft != null) {
                focusLogger.finer("nearestNodeTopLeft.node : "+nearestNodeBottomLeft.node);
            }
            if (nearestNodeAnythingAnywhereDown != null) {
                focusLogger.finer("nearestNodeAnythingAnywhereDown.node : "+nearestNodeAnythingAnywhereDown.node);
            }
        }

        /*
        ** we've just changed direction, and have a node on stack.
        ** there is a strong preference for this node, just make sure
        ** it's not a bad choice, as sometimes we got here as a last-chance
        */
        if (reversingTargetNode != null) {
            return reversingNode;
        }
        if (nearestNodeOnOriginX != null && nearestNodeOnOriginX.biasShortestDistance < Double.MAX_VALUE) {
            /*
            ** there's a preference, all else being equal, to return nearestNodeOnOriginX
            */
            if (nearestNodeOnCurrentX != null && nearestNodeAverageDown != null &&
                nearestNodeOnOriginX.node == nearestNodeOnCurrentX.node && nearestNodeOnOriginX.node == nearestNodeAverageDown.node) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeOnCurrentX != null && nearestNodeOriginSimple2D != null &&
                nearestNodeOnOriginX.node == nearestNodeOnCurrentX.node && nearestNodeOnOriginX.node == nearestNodeOriginSimple2D.node) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeOnCurrentX != null && nearestNodeBottomLeft != null && nearestNodeOnOriginX.node == nearestNodeOnCurrentX.node && nearestNodeOnOriginX.node == nearestNodeBottomLeft.node) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeOnCurrentX != null && nearestNodeAnythingAnywhereDown != null && nearestNodeOnOriginX.node == nearestNodeOnCurrentX.node && nearestNodeOnOriginX.node == nearestNodeAnythingAnywhereDown.node) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeAverageDown != null && nearestNodeOnOriginX.node == nearestNodeAverageDown.node) {
                return nearestNodeOnOriginX.node;
            }

            if (nearestNodeOnCurrentX != null && nearestNodeOnCurrentX.biasShortestDistance < Double.MAX_VALUE) {
                if (nearestNodeOnOriginX == null || (nearestNodeOnCurrentX.topLeftDistance < nearestNodeOnOriginX.topLeftDistance) &&
                    (nearestNodeOnCurrentX.originBottomLeftDistance < nearestNodeOnOriginX.originBottomLeftDistance) &&
                    (nearestNodeOnCurrentX.bounds.getMinX() - currentBottomLeft2D.getX()) < (nearestNodeOnOriginX.bounds.getMinX() - currentBottomLeft2D.getX())  )  {

                    return nearestNodeOnCurrentX.node;
                }
                else  if (nearestNodeOnOriginX != null) {
                    if (nearestNodeAverageDown == null || nearestNodeOnOriginX.averageDistance < nearestNodeAverageDown.averageDistance) {
                        return nearestNodeOnOriginX.node;
                    }
                }
            }
        }
        else {
            if (nearestNodeOnOriginX == null && nearestNodeOnCurrentX == null && nearestNodeCurrentSimple2D != null) {
                if (nearestNodeAverageDown != null && nearestNodeBottomLeft != null && nearestNodeAnythingAnywhereDown != null && (nearestNodeAverageDown.node == nearestNodeBottomLeft.node && nearestNodeAverageDown.node == nearestNodeAnythingAnywhereDown.node)) {
                    return nearestNodeAverageDown.node;
                }
                return nearestNodeCurrentSimple2D.node;
            }
            else if (nearestNodeAverageDown != null && nearestNodeBottomLeft != null && nearestNodeAnythingAnywhereDown != null &&
                     nearestNodeAverageDown.biasShortestDistance == nearestNodeBottomLeft.biasShortestDistance &&
                     nearestNodeAverageDown.biasShortestDistance == nearestNodeAnythingAnywhereDown.biasShortestDistance &&
                     nearestNodeAverageDown.biasShortestDistance < Double.MAX_VALUE) {

                if (nearestNodeOnOriginX != null && nearestNodeOnOriginX.originBottomLeftDistance < nearestNodeAverageDown.originBottomLeftDistance) {
                    return nearestNodeOnOriginX.node;
                }
                else {
                    return nearestNodeAverageDown.node;
                }
            }
        }

        /*
        ** is the average closer?
        */
        if (nearestNodeAverageDown != null && (nearestNodeOnOriginX == null || (nearestNodeAverageDown.biasShortestDistance < nearestNodeOnOriginX.biasShortestDistance))) {
            /*
            ** but is one in the way
            */
            if (nearestNodeOnOriginX != null && (nearestNodeOnOriginX.bounds.getMinY() <= nearestNodeAverageDown.bounds.getMinY())) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeOriginSimple2D != null) {
                if (nearestNodeOriginSimple2D.current2DMetric <= nearestNodeAverageDown.current2DMetric) {
                    return nearestNodeOriginSimple2D.node;
                }
                if (nearestNodeOnOriginX.bounds.getMinY() <= nearestNodeAverageDown.bounds.getMinY()) {
                    return nearestNodeOriginSimple2D.node;
                }
            }
            return nearestNodeAverageDown.node;
        }

        /*
        ** this is an odd one, in that is isn't the closest on current, or on the
        ** origin, but it looks better for most cases...
        */
        if ((nearestNodeCurrentSimple2D != null && nearestNodeOnCurrentX != null && nearestNodeAverageDown != null && nearestNodeBottomLeft != null && nearestNodeAnythingAnywhereDown != null) &&
            (nearestNodeCurrentSimple2D.node == nearestNodeOnCurrentX.node) &&
            (nearestNodeCurrentSimple2D.node ==  nearestNodeAverageDown.node) &&
            (nearestNodeCurrentSimple2D.node == nearestNodeBottomLeft.node) &&
            (nearestNodeCurrentSimple2D.node == nearestNodeAnythingAnywhereDown.node)) {
            return nearestNodeCurrentSimple2D.node;
        }

        if (nearestNodeOnOriginX != null && (nearestNodeOnCurrentX == null || (nearestNodeOnOriginX.topRightDistance < nearestNodeOnCurrentX.topRightDistance))) {
            return nearestNodeOnOriginX.node;
        }
        /*
        ** There isn't a clear winner, just go to the one nearest the current
        ** focus owner, or if invalid then try the other contenders.
        */
        if (nearestNodeOnOriginX != null) {
            return nearestNodeOnOriginX.node;
        }
        else if (nearestNodeOriginSimple2D != null) {
            return nearestNodeOriginSimple2D.node;
        }
        else if (nearestNodeOnCurrentX != null) {
            return nearestNodeOnCurrentX.node;
        }
        else if (nearestNodeAverageDown != null) {
            return nearestNodeAverageDown.node;
        }
        else if (nearestNodeBottomLeft != null) {
            return nearestNodeBottomLeft.node;
        }
        else if (nearestNodeAnythingAnywhereDown != null) {
            return nearestNodeAnythingAnywhereDown.node;
        }
        return null;
    }

    protected Node getNearestNodeLeft(Bounds currentB, Bounds originB, TraversalEngine engine, Node node, Node reversingNode) {

        List<Node> nodes = engine.getAllTargetNodes();

        Bounds biasedB = new BoundingBox(currentB.getMinX(), originB.getMinY(), currentB.getWidth(), originB.getHeight());

        Point2D currentMid2D = new Point2D(currentB.getMinX(), currentB.getMinY()+(currentB.getHeight()/2));
        Point2D currentTopLeft2D = new Point2D(currentB.getMinX(), currentB.getMinY());
        Point2D currentBottomLeft2D = new Point2D(currentB.getMinX(), currentB.getMaxY());

        Point2D originTopLeft2D = new Point2D(originB.getMinX(), originB.getMinY());

        TargetNode reversingTargetNode = null;
        TargetNode targetNode = new TargetNode();
        TargetNode nearestNodeCurrentSimple2D = null;
        TargetNode nearestNodeOriginSimple2D = null;
        TargetNode nearestNodeAverageLeft = null;
        TargetNode nearestNodeOnOriginY = null;
        TargetNode nearestNodeOnCurrentY = null;
        TargetNode nearestNodeTopLeft = null;
        TargetNode nearestNodeAnythingAnywhereLeft = null;

        if (nodes.size() > 0) {
            int nodeIndex;

            if (reversingNode != null) {
                Bounds targetBounds = reversingNode.localToScene(reversingNode.getLayoutBounds());
                reversingTargetNode = new TargetNode();

                reversingTargetNode.node = reversingNode;
                reversingTargetNode.bounds = targetBounds;

                /*
                ** closest biased : simple 2d
                */
                double outdB = outDistance(Direction.LEFT, biasedB, targetBounds);

                if (isOnAxis(Direction.LEFT, biasedB, targetBounds)) {
                    reversingTargetNode.biased2DMetric = outdB + centerSideDistance(Direction.LEFT, biasedB, targetBounds) / 100;
                }
                else {
                    final double cosd = cornerSideDistance(Direction.LEFT, biasedB, targetBounds);
                    reversingTargetNode.biased2DMetric = 100000 + outdB*outdB + 9*cosd*cosd;
                }
                /*
                ** closest current : simple 2d
                */
                double outdC = outDistance(Direction.LEFT, currentB, targetBounds);

                if (isOnAxis(Direction.LEFT, currentB, targetBounds)) {
                    reversingTargetNode.current2DMetric = outdC + centerSideDistance(Direction.LEFT, currentB, targetBounds) / 100;
                }
                else {
                    final double cosd = cornerSideDistance(Direction.LEFT, currentB, targetBounds);
                    reversingTargetNode.current2DMetric = 100000 + outdC*outdC + 9*cosd*cosd;
                }

                reversingTargetNode.topRightDistance = currentTopLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                reversingTargetNode.midDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(originB.getHeight()/2));
                reversingTargetNode.bottomRightDistance = currentBottomLeft2D.distance(originB.getMaxX(), targetBounds.getMaxY());

                double currentTopLeftToTargetBottomRightDistance = currentTopLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());
                double currentTopLeftToTargetMidDistance = currentTopLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));
                double currentBottomLeftToTargetTopRightDistance = currentBottomLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                double currentBottomLeftToTargetBottomRightDistance = currentBottomLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());
                double currentBottomLeftToTargetMidDistance = currentBottomLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));
                double currentMidToTargetTopRightDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                double currentMidToTargetBottomRightDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());
                double currentMidToTargetMidDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));

                double biasTopLeftToTargetBottomRightDistance = currentTopLeft2D.distance(originB.getMaxX(), targetBounds.getMaxY());
                double biasTopLeftToTargetMidDistance = currentTopLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(originB.getHeight()/2));
                double biasBottomLeftToTargetMidDistance = currentBottomLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(originB.getHeight()/2));
                double biasMidToTargetBottomRightDistance = currentMid2D.distance(originB.getMaxX(), targetBounds.getMaxY());

                reversingTargetNode.averageDistance =
                    (reversingTargetNode.topRightDistance+biasTopLeftToTargetBottomRightDistance+biasTopLeftToTargetMidDistance+
                     currentBottomLeftToTargetTopRightDistance+reversingTargetNode.bottomRightDistance+biasBottomLeftToTargetMidDistance)/7;

                reversingTargetNode.biasShortestDistance = findMin9(reversingTargetNode.topRightDistance, biasTopLeftToTargetBottomRightDistance, biasTopLeftToTargetMidDistance,
                                                currentBottomLeftToTargetTopRightDistance, reversingTargetNode.bottomRightDistance, biasBottomLeftToTargetMidDistance,
                                                currentMidToTargetTopRightDistance, biasMidToTargetBottomRightDistance, reversingTargetNode.midDistance);

                reversingTargetNode.shortestDistance = findMin9(reversingTargetNode.topRightDistance, currentTopLeftToTargetBottomRightDistance, currentTopLeftToTargetMidDistance,
                                            currentBottomLeftToTargetTopRightDistance, currentBottomLeftToTargetBottomRightDistance, currentBottomLeftToTargetMidDistance,
                                            currentMidToTargetTopRightDistance, currentMidToTargetBottomRightDistance, currentMidToTargetMidDistance);
            }


            for (nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {

                Bounds targetBounds = nodes.get(nodeIndex).localToScene(nodes.get(nodeIndex).getLayoutBounds());
                /*
                ** check that the target node starts after we start target.minX > origin.minX
                ** and the target node ends after we end target.maxX > origin.maxX
                */
                if (currentB.getMinX() >  targetBounds.getMinX()) {

                    targetNode.node = nodes.get(nodeIndex);
                    targetNode.bounds = targetBounds;

                    /*
                    ** closest biased : simple 2d
                    */
                    double outdB = outDistance(Direction.LEFT, biasedB, targetBounds);

                    if (isOnAxis(Direction.LEFT, biasedB, targetBounds)) {
                        targetNode.biased2DMetric = outdB + centerSideDistance(Direction.LEFT, biasedB, targetBounds) / 100;
                    }
                    else {
                        final double cosd = cornerSideDistance(Direction.LEFT, biasedB, targetBounds);
                        targetNode.biased2DMetric = 100000 + outdB*outdB + 9*cosd*cosd;
                    }
                    /*
                    ** closest current : simple 2d
                    */
                    double outdC = outDistance(Direction.LEFT, currentB, targetBounds);

                    if (isOnAxis(Direction.LEFT, currentB, targetBounds)) {
                        targetNode.current2DMetric = outdC + centerSideDistance(Direction.LEFT, currentB, targetBounds) / 100;
                    }
                    else {
                        final double cosd = cornerSideDistance(Direction.LEFT, currentB, targetBounds);
                        targetNode.current2DMetric = 100000 + outdC*outdC + 9*cosd*cosd;
                    }

                    targetNode.topRightDistance = currentTopLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                    targetNode.midDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(originB.getHeight()/2));
                    targetNode.bottomRightDistance = currentBottomLeft2D.distance(originB.getMaxX(), targetBounds.getMaxY());

                    double currentTopLeftToTargetBottomRightDistance = currentTopLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());
                    double currentTopLeftToTargetMidDistance = currentTopLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));
                    double currentBottomLeftToTargetTopRightDistance = currentBottomLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                    double currentBottomLeftToTargetBottomRightDistance = currentBottomLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());
                    double currentBottomLeftToTargetMidDistance = currentBottomLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));
                    double currentMidToTargetTopRightDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMinY());
                    double currentMidToTargetBottomRightDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMaxY());
                    double currentMidToTargetMidDistance = currentMid2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));

                    double biasTopLeftToTargetBottomRightDistance = currentTopLeft2D.distance(originB.getMaxX(), targetBounds.getMaxY());
                    double biasTopLeftToTargetMidDistance = currentTopLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(originB.getHeight()/2));
                    double biasBottomLeftToTargetMidDistance = currentBottomLeft2D.distance(targetBounds.getMaxX(), targetBounds.getMinY()+(originB.getHeight()/2));
                    double biasMidToTargetBottomRightDistance = currentMid2D.distance(originB.getMaxX(), targetBounds.getMaxY());

                    targetNode.averageDistance =
                        (targetNode.topRightDistance+biasTopLeftToTargetBottomRightDistance+biasTopLeftToTargetMidDistance+
                         currentBottomLeftToTargetTopRightDistance+targetNode.bottomRightDistance+biasBottomLeftToTargetMidDistance)/7;

                    targetNode.biasShortestDistance =
                        findMin9(targetNode.topRightDistance, biasTopLeftToTargetBottomRightDistance, biasTopLeftToTargetMidDistance,
                                 currentBottomLeftToTargetTopRightDistance, targetNode.bottomRightDistance, biasBottomLeftToTargetMidDistance,
                                 currentMidToTargetTopRightDistance, biasMidToTargetBottomRightDistance, targetNode.midDistance);

                    targetNode.shortestDistance =
                        findMin9(targetNode.topRightDistance, currentTopLeftToTargetBottomRightDistance, currentTopLeftToTargetMidDistance,
                                 currentBottomLeftToTargetTopRightDistance, currentBottomLeftToTargetBottomRightDistance, currentBottomLeftToTargetMidDistance,
                                 currentMidToTargetTopRightDistance, currentMidToTargetBottomRightDistance, currentMidToTargetMidDistance);


                    /*
                    ** closest biased : simple 2d
                    */
                    if (outdB >= 0.0) {
                        if (nearestNodeOriginSimple2D == null || targetNode.biased2DMetric < nearestNodeOriginSimple2D.biased2DMetric) {

                            if (nearestNodeOriginSimple2D == null) {
                                nearestNodeOriginSimple2D = new TargetNode();
                            }
                            nearestNodeOriginSimple2D.copy(targetNode);
                        }
                    }
                    /*
                    ** closest current : simple 2d
                    */
                    if (outdC >= 0.0) {
                        if (nearestNodeCurrentSimple2D == null || targetNode.current2DMetric < nearestNodeCurrentSimple2D.current2DMetric) {

                            if (nearestNodeCurrentSimple2D == null) {
                                nearestNodeCurrentSimple2D = new TargetNode();
                            }
                            nearestNodeCurrentSimple2D.copy(targetNode);
                        }
                    }
                    /*
                    ** on the Origin Y
                    */
                    if ((originB.getMaxY() > targetBounds.getMinY()) && (targetBounds.getMaxY() > originB.getMinY())) {
                        if (nearestNodeOnOriginY == null || nearestNodeOnOriginY.topRightDistance > targetNode.topRightDistance) {

                            if (nearestNodeOnOriginY == null) {
                                nearestNodeOnOriginY = new TargetNode();
                            }
                            nearestNodeOnOriginY.copy(targetNode);
                        }
                    }
                    /*
                    ** on the Current Y
                    */
                    if ((currentB.getMaxY() > targetBounds.getMinY()) && (targetBounds.getMaxY() > currentB.getMinY())) {
                        if (nearestNodeOnCurrentY == null || nearestNodeOnCurrentY.topRightDistance > targetNode.topRightDistance) {

                            if (nearestNodeOnCurrentY == null) {
                                nearestNodeOnCurrentY = new TargetNode();
                            }
                            nearestNodeOnCurrentY.copy(targetNode);
                        }
                    }
                    /*
                    ** Closest top left / top right corners.
                    */
                    if (nearestNodeTopLeft == null || nearestNodeTopLeft.topRightDistance > targetNode.topRightDistance) {

                        if (nearestNodeTopLeft == null) {
                            nearestNodeTopLeft = new TargetNode();
                        }
                        nearestNodeTopLeft.copy(targetNode);
                    }

                    if (nearestNodeAverageLeft == null || nearestNodeAverageLeft.averageDistance > targetNode.averageDistance) {

                        if (nearestNodeAverageLeft == null) {
                            nearestNodeAverageLeft = new TargetNode();
                        }
                        nearestNodeAverageLeft.copy(targetNode);
                    }

                    if (nearestNodeAnythingAnywhereLeft == null || nearestNodeAnythingAnywhereLeft.shortestDistance > targetNode.shortestDistance) {

                        if (nearestNodeAnythingAnywhereLeft == null) {
                            nearestNodeAnythingAnywhereLeft = new TargetNode();
                        }
                        nearestNodeAnythingAnywhereLeft.copy(targetNode);
                    }
                }
            }
        }
        nodes.clear();

        if (reversingTargetNode != null) {
            reversingTargetNode.originTopRightDistance = originTopLeft2D.distance(reversingTargetNode.bounds.getMinX(), reversingTargetNode.bounds.getMinY());
        }
        if (nearestNodeOriginSimple2D != null) {
            nearestNodeOriginSimple2D.originTopRightDistance = originTopLeft2D.distance(nearestNodeOriginSimple2D.bounds.getMinX(), nearestNodeOriginSimple2D.bounds.getMinY());
        }
        if (nearestNodeCurrentSimple2D != null) {
            nearestNodeCurrentSimple2D.originTopRightDistance = originTopLeft2D.distance(nearestNodeCurrentSimple2D.bounds.getMinX(), nearestNodeCurrentSimple2D.bounds.getMinY());
        }
        if (nearestNodeOnOriginY != null) {
            nearestNodeOnOriginY.originTopRightDistance = originTopLeft2D.distance(nearestNodeOnOriginY.bounds.getMinX(), nearestNodeOnOriginY.bounds.getMinY());
        }
        if (nearestNodeOnCurrentY != null) {
            nearestNodeOnCurrentY.originTopRightDistance = originTopLeft2D.distance(nearestNodeOnCurrentY.bounds.getMinX(), nearestNodeOnCurrentY.bounds.getMinY());
        }
        if (nearestNodeAverageLeft != null) {
            nearestNodeAverageLeft.originTopRightDistance = originTopLeft2D.distance(nearestNodeAverageLeft.bounds.getMinX(), nearestNodeAverageLeft.bounds.getMinY());
        }
        if (nearestNodeTopLeft != null) {
            nearestNodeTopLeft.originTopRightDistance = originTopLeft2D.distance(nearestNodeTopLeft.bounds.getMinX(), nearestNodeTopLeft.bounds.getMinY());
        }
        if (nearestNodeAnythingAnywhereLeft != null) {
            nearestNodeAnythingAnywhereLeft.originTopRightDistance = originTopLeft2D.distance(nearestNodeAnythingAnywhereLeft.bounds.getMinX(), nearestNodeAnythingAnywhereLeft.bounds.getMinY());
        }

        if (nearestNodeOnCurrentY == null && nearestNodeOnOriginY == null && reversingTargetNode == null) {
            cacheStartTraversalNode = null;
            cacheStartTraversalDirection = null;
            reverseDirection = false;
            traversalNodeStack.clear();
        }

        if (focusLogger.isLoggable(Level.FINER)) {
            if (reversingTargetNode != null) {
                focusLogger.finer("reversingTargetNode.node : "+reversingTargetNode.node);
            }
            if (nearestNodeOriginSimple2D != null) {
                focusLogger.finer("nearestNodeOriginSimple2D.node : "+nearestNodeOriginSimple2D.node);
            }
            if (nearestNodeCurrentSimple2D != null) {
                focusLogger.finer("nearestNodeCurrentSimple2D.node : "+nearestNodeCurrentSimple2D.node);
            }
            if (nearestNodeOnOriginY != null) {
                focusLogger.finer("nearestNodeOnOriginY.node : "+nearestNodeOnOriginY.node);
            }
            if (nearestNodeOnCurrentY != null) {
                focusLogger.finer("nearestNodeOnCurrentY.node : "+nearestNodeOnCurrentY.node);
            }
            if (nearestNodeAverageLeft != null) {
                focusLogger.finer("nearestNodeAverageLeft.node : "+nearestNodeAverageLeft.node);
            }
            if (nearestNodeTopLeft != null) {
                focusLogger.finer("nearestNodeTopLeft.node : "+nearestNodeTopLeft.node);
            }
            if (nearestNodeAnythingAnywhereLeft != null) {
                focusLogger.finer("nearestNodeAnythingAnywhereLeft.node : "+nearestNodeAnythingAnywhereLeft.node);
            }
        }

        /*
        ** we've just changed direction, and have a node on stack.
        ** there is a strong preference for this node, just make sure
        ** it's not a bad choice, as sometimes we got here as a last-chance
        */
        if (reversingTargetNode != null) {
            return reversingNode;
        }

        if (nearestNodeOnOriginY != null && nearestNodeOnOriginY.biasShortestDistance < Double.MAX_VALUE) {
            /*
            ** there's a preference, all else being equal, to return nearestNodeOnOriginY
            */
            if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null &&
                (nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node)) {
                return nearestNodeOnOriginY.node;
            }
            if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null && nearestNodeAverageLeft != null &&
                (nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node) && (nearestNodeOnOriginY.node == nearestNodeAverageLeft.node)) {
                return nearestNodeOnOriginY.node;
            }
            if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null && nearestNodeTopLeft != null &&
                (nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node) && (nearestNodeOnOriginY.node == nearestNodeTopLeft.node)) {
                return nearestNodeOnOriginY.node;
            }
            if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null && nearestNodeAnythingAnywhereLeft != null &&
                (nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node) && (nearestNodeOnOriginY.node == nearestNodeAnythingAnywhereLeft.node)) {
                return nearestNodeOnOriginY.node;
            }
            if (nearestNodeOnOriginY != null && nearestNodeAverageLeft != null &&
                (nearestNodeOnOriginY.node == nearestNodeAverageLeft.node)) {
                return nearestNodeOnOriginY.node;
            }

            if (nearestNodeOnCurrentY != null && nearestNodeOnCurrentY.biasShortestDistance < Double.MAX_VALUE) {
                if (nearestNodeOnOriginY == null ||
                    (nearestNodeOnCurrentY.bottomRightDistance < nearestNodeOnOriginY.bottomRightDistance) &&
                    (nearestNodeOnCurrentY.originTopRightDistance < nearestNodeOnOriginY.originTopRightDistance) &&
                    (nearestNodeOnCurrentY.bounds.getMinY() - currentTopLeft2D.getY()) < (nearestNodeOnOriginY.bounds.getMinY() - currentTopLeft2D.getY())  )  {

                    return nearestNodeOnCurrentY.node;
                }
                else if (nearestNodeOnOriginY != null) {
                    if (nearestNodeAverageLeft != null || nearestNodeOnOriginY.averageDistance < nearestNodeAverageLeft.averageDistance) {
                        return nearestNodeOnOriginY.node;
                    }
                }
            }
        }
        else {
            if (nearestNodeOnOriginY == null && nearestNodeOnCurrentY == null && nearestNodeCurrentSimple2D != null) {
                if (nearestNodeAverageLeft != null && nearestNodeTopLeft != null && nearestNodeAverageLeft != null &&  nearestNodeAnythingAnywhereLeft != null &&
                    nearestNodeAverageLeft.node == nearestNodeTopLeft.node && nearestNodeAverageLeft.node == nearestNodeAnythingAnywhereLeft.node) {
                    return nearestNodeAverageLeft.node;
                }
                return nearestNodeCurrentSimple2D.node;
            }
            else if (nearestNodeAverageLeft != null && nearestNodeTopLeft != null && nearestNodeAnythingAnywhereLeft != null &&
                     nearestNodeAverageLeft.biasShortestDistance == nearestNodeTopLeft.biasShortestDistance &&
                     nearestNodeAverageLeft.biasShortestDistance == nearestNodeAnythingAnywhereLeft.biasShortestDistance &&
                     nearestNodeAverageLeft.biasShortestDistance < Double.MAX_VALUE) {

                if (nearestNodeOnOriginY != null && nearestNodeOnOriginY.originTopRightDistance < nearestNodeAverageLeft.originTopRightDistance) {
                    return nearestNodeOnOriginY.node;
                }
                else {
                    return nearestNodeAverageLeft.node;
                }
            }
        }

        /*
        ** is the average closer?
        */
        if (nearestNodeAverageLeft != null && (nearestNodeOnOriginY == null || nearestNodeAverageLeft.biasShortestDistance < nearestNodeOnOriginY.biasShortestDistance)) {
            /*
            ** but is one in the way
            */
            if (nearestNodeOnOriginY != null && (nearestNodeOnOriginY.bounds.getMaxX() >= nearestNodeAverageLeft.bounds.getMaxX())) {
                return nearestNodeOnOriginY.node;
            }
            /*
            ** maybe Origin is better than this?
            */
            if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null && nearestNodeOnOriginY.biasShortestDistance < Double.MAX_VALUE && (nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node)) {
                return nearestNodeOnOriginY.node;
            }

            if (nearestNodeOnCurrentY != null && nearestNodeOnOriginY != null && nearestNodeOnCurrentY.biasShortestDistance < Double.MAX_VALUE && (nearestNodeOnCurrentY.biasShortestDistance < nearestNodeOnOriginY.biasShortestDistance)) {
                return nearestNodeOnCurrentY.node;
            }

            if (nearestNodeOnOriginY != null &&  nearestNodeAverageLeft != null && nearestNodeOnOriginY.biasShortestDistance < Double.MAX_VALUE && (nearestNodeOnOriginY.originTopRightDistance < nearestNodeAverageLeft.originTopRightDistance)) {
                return nearestNodeOnOriginY.node;
            }
            return nearestNodeAverageLeft.node;
        }


        if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null && nearestNodeOnOriginY.bottomRightDistance < nearestNodeOnCurrentY.bottomRightDistance) {
            return nearestNodeOnOriginY.node;
        }

        /*
        ** if any of the remaining match we'll take that
        */
        if (nearestNodeOnCurrentY != null && nearestNodeTopLeft != null && nearestNodeOnCurrentY.biasShortestDistance < Double.MAX_VALUE && (nearestNodeOnCurrentY.node == nearestNodeTopLeft.node)) {
            return nearestNodeOnCurrentY.node;
        }
        /*
        ** There isn't a clear winner, just go to the one nearest the current
        ** focus owner, or if invalid then try the other contenders.
        */
        if (nearestNodeOnOriginY != null) {
            return nearestNodeOnOriginY.node;
        }
        else if (nearestNodeOriginSimple2D != null) {
            return nearestNodeOriginSimple2D.node;
        }
        else if (nearestNodeOnCurrentY != null) {
            return nearestNodeOnCurrentY.node;
        }
        else if (nearestNodeAverageLeft != null) {
            return nearestNodeAverageLeft.node;
        }
        else if (nearestNodeTopLeft != null) {
            return nearestNodeTopLeft.node;
        }
        else if (nearestNodeAnythingAnywhereLeft != null) {
            return nearestNodeAnythingAnywhereLeft.node;
        }
        return null;
    }

    protected Node getNearestNodeRight(Bounds currentB, Bounds originB, TraversalEngine engine, Node node, Node reversingNode) {

        List<Node> nodes = engine.getAllTargetNodes();

        Bounds biasedB = new BoundingBox(currentB.getMinX(), originB.getMinY(), currentB.getWidth(), originB.getHeight());

        Point2D currentMid2D = new Point2D(currentB.getMaxX(), currentB.getMinY()+(currentB.getHeight()/2));
        Point2D currentTopRight2D = new Point2D(currentB.getMaxX(), currentB.getMinY());
        Point2D currentBottomRight2D = new Point2D(currentB.getMaxX(), currentB.getMaxY());

        Point2D originTopRight2D = new Point2D(originB.getMaxX(), originB.getMinY());

        TargetNode reversingTargetNode = null;
        TargetNode targetNode = new TargetNode();
        TargetNode nearestNodeCurrentSimple2D = null;
        TargetNode nearestNodeOriginSimple2D = null;
        TargetNode nearestNodeAverageRight = null;
        TargetNode nearestNodeOnOriginY = null;
        TargetNode nearestNodeOnCurrentY = null;
        TargetNode nearestNodeTopRight = null;
        TargetNode nearestNodeAnythingAnywhereRight = null;

        if (nodes.size() > 0) {
            int nodeIndex;

            if (reversingNode != null) {
                Bounds targetBounds = reversingNode.localToScene(reversingNode.getLayoutBounds());
                reversingTargetNode = new TargetNode();

                reversingTargetNode.node = reversingNode;
                reversingTargetNode.bounds = targetBounds;

                /*
                ** closest biased : simple 2d
                */
                double outdB = outDistance(Direction.RIGHT, biasedB, targetBounds);

                if (isOnAxis(Direction.RIGHT, biasedB, targetBounds)) {
                    reversingTargetNode.biased2DMetric = outdB + centerSideDistance(Direction.RIGHT, biasedB, targetBounds) / 100;
                }
                else {
                    final double cosd = cornerSideDistance(Direction.RIGHT, biasedB, targetBounds);
                    reversingTargetNode.biased2DMetric = 100000 + outdB*outdB + 9*cosd*cosd;
                }
                /*
                ** closest current : simple 2d
                */
                double outdC = outDistance(Direction.RIGHT, currentB, targetBounds);

                if (isOnAxis(Direction.RIGHT, currentB, targetBounds)) {
                    reversingTargetNode.current2DMetric = outdC + centerSideDistance(Direction.RIGHT, currentB, targetBounds) / 100;
                }
                else {
                    final double cosd = cornerSideDistance(Direction.RIGHT, currentB, targetBounds);
                    reversingTargetNode.current2DMetric = 100000 + outdC*outdC + 9*cosd*cosd;
                }

                reversingTargetNode.topLeftDistance = currentTopRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                reversingTargetNode.midDistance  = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(originB.getHeight()/2));
                reversingTargetNode.bottomLeftDistance = currentBottomRight2D.distance(originB.getMinX(), targetBounds.getMaxY());

                double currentTopRightToTargetBottomLeftDistance = currentTopRight2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                double currentTopRightToTargetMidDistance = currentTopRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));
                double currentBottomRightToTargetTopLeftDistance = currentBottomRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                double currentBottomRightToTargetBottomLeftDistance = currentBottomRight2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                double currentBottomRightToTargetMidDistance = currentBottomRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));
                double currentMidToTargetTopLeftDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                double currentMidToTargetBottomLeftDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                double currentMidToTargetMidDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));

                double biasTopRightToTargetBottomLeftDistance = currentTopRight2D.distance(originB.getMinX(), targetBounds.getMaxY());
                double biasTopRightToTargetMidDistance = currentTopRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(originB.getHeight()/2));
                double biasBottomRightToTargetMidDistance = currentBottomRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(originB.getHeight()/2));
                double biasMidToTargetBottomLeftDistance = currentMid2D.distance(originB.getMinX(), targetBounds.getMaxY());

                reversingTargetNode.averageDistance =
                    (reversingTargetNode.topLeftDistance+biasTopRightToTargetBottomLeftDistance+biasTopRightToTargetMidDistance+
                     currentBottomRightToTargetTopLeftDistance+reversingTargetNode.bottomLeftDistance+biasBottomRightToTargetMidDistance)/7;


                reversingTargetNode.biasShortestDistance =
                    findMin9(reversingTargetNode.topLeftDistance, biasTopRightToTargetBottomLeftDistance, biasTopRightToTargetMidDistance,
                             currentBottomRightToTargetTopLeftDistance, reversingTargetNode.bottomLeftDistance, biasBottomRightToTargetMidDistance,
                             currentMidToTargetTopLeftDistance, biasMidToTargetBottomLeftDistance, reversingTargetNode.midDistance);

                reversingTargetNode.shortestDistance =
                    findMin9(reversingTargetNode.topLeftDistance, currentTopRightToTargetBottomLeftDistance, currentTopRightToTargetMidDistance,
                             currentBottomRightToTargetTopLeftDistance, currentBottomRightToTargetBottomLeftDistance, currentBottomRightToTargetMidDistance,
                             currentMidToTargetTopLeftDistance, currentMidToTargetBottomLeftDistance, currentMidToTargetMidDistance);
            }


            for (nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {

                Bounds targetBounds = nodes.get(nodeIndex).localToScene(nodes.get(nodeIndex).getLayoutBounds());
                /*
                ** check that the target node starts after we start target.minX > origin.minX
                ** and the target node ends after we end target.maxX > origin.maxX
                */
                if (currentB.getMaxX() <  targetBounds.getMaxX()) {

                    targetNode.node = nodes.get(nodeIndex);
                    targetNode.bounds = targetBounds;

                    /*
                    ** closest biased : simple 2d
                    */
                    double outdB = outDistance(Direction.RIGHT, biasedB, targetBounds);

                    if (isOnAxis(Direction.RIGHT, biasedB, targetBounds)) {
                        targetNode.biased2DMetric = outdB + centerSideDistance(Direction.RIGHT, biasedB, targetBounds) / 100;
                    }
                    else {
                        final double cosd = cornerSideDistance(Direction.RIGHT, biasedB, targetBounds);
                        targetNode.biased2DMetric = 100000 + outdB*outdB + 9*cosd*cosd;
                    }
                    /*
                    ** closest current : simple 2d
                    */
                    double outdC = outDistance(Direction.RIGHT, currentB, targetBounds);

                    if (isOnAxis(Direction.RIGHT, currentB, targetBounds)) {
                        targetNode.current2DMetric = outdC + centerSideDistance(Direction.RIGHT, currentB, targetBounds) / 100;
                    }
                    else {
                        final double cosd = cornerSideDistance(Direction.RIGHT, currentB, targetBounds);
                        targetNode.current2DMetric = 100000 + outdC*outdC + 9*cosd*cosd;
                    }

                    targetNode.topLeftDistance = currentTopRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                    targetNode.midDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(originB.getHeight()/2));
                    targetNode.bottomLeftDistance = currentBottomRight2D.distance(originB.getMinX(), targetBounds.getMaxY());

                    double currentTopRightToTargetBottomLeftDistance = currentTopRight2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                    double currentTopRightToTargetMidDistance = currentTopRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));
                    double currentBottomRightToTargetTopLeftDistance = currentBottomRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                    double currentBottomRightToTargetBottomLeftDistance = currentBottomRight2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                    double currentBottomRightToTargetMidDistance = currentBottomRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));
                    double currentMidToTargetTopLeftDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMinY());
                    double currentMidToTargetBottomLeftDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMaxY());
                    double currentMidToTargetMidDistance = currentMid2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(targetBounds.getHeight()/2));

                    double biasTopRightToTargetBottomLeftDistance = currentTopRight2D.distance(originB.getMinX(), targetBounds.getMaxY());
                    double biasTopRightToTargetMidDistance = currentTopRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(originB.getHeight()/2));
                    double biasBottomRightToTargetMidDistance = currentBottomRight2D.distance(targetBounds.getMinX(), targetBounds.getMinY()+(originB.getHeight()/2));
                    double biasMidToTargetBottomLeftDistance = currentMid2D.distance(originB.getMinX(), targetBounds.getMaxY());

                    targetNode.averageDistance =
                        (targetNode.topLeftDistance+biasTopRightToTargetBottomLeftDistance+biasTopRightToTargetMidDistance+
                         currentBottomRightToTargetTopLeftDistance+targetNode.bottomLeftDistance+biasBottomRightToTargetMidDistance)/7;

                    targetNode.biasShortestDistance =
                        findMin9(targetNode.topLeftDistance, biasTopRightToTargetBottomLeftDistance, biasTopRightToTargetMidDistance,
                                 currentBottomRightToTargetTopLeftDistance, targetNode.bottomLeftDistance, biasBottomRightToTargetMidDistance,
                                 currentMidToTargetTopLeftDistance, biasMidToTargetBottomLeftDistance, targetNode.midDistance);

                    targetNode.shortestDistance =
                        findMin9(targetNode.topLeftDistance, currentTopRightToTargetBottomLeftDistance, currentTopRightToTargetMidDistance,
                                 currentBottomRightToTargetTopLeftDistance, currentBottomRightToTargetBottomLeftDistance, currentBottomRightToTargetMidDistance,
                                 currentMidToTargetTopLeftDistance, currentMidToTargetBottomLeftDistance, currentMidToTargetMidDistance);


                    /*
                    ** closest biased : simple 2d
                    */
                    if (outdB >= 0.0) {
                        if (nearestNodeOriginSimple2D == null || targetNode.biased2DMetric < nearestNodeOriginSimple2D.biased2DMetric) {

                            if (nearestNodeOriginSimple2D == null) {
                                nearestNodeOriginSimple2D = new TargetNode();
                            }
                            nearestNodeOriginSimple2D.copy(targetNode);
                        }
                    }
                    /*
                    ** closest current : simple 2d
                    */
                    if (outdC >= 0.0) {
                        if (nearestNodeCurrentSimple2D == null || targetNode.current2DMetric < nearestNodeCurrentSimple2D.current2DMetric) {

                            if (nearestNodeCurrentSimple2D == null) {
                                nearestNodeCurrentSimple2D = new TargetNode();
                            }
                            nearestNodeCurrentSimple2D.copy(targetNode);
                        }
                    }
                    /*
                    ** on the Origin Y
                    */
                    if ((originB.getMaxY() > targetBounds.getMinY()) && (targetBounds.getMaxY() > originB.getMinY())) {
                        if (nearestNodeOnOriginY == null || nearestNodeOnOriginY.topLeftDistance > targetNode.topLeftDistance) {

                            if (nearestNodeOnOriginY == null) {
                                nearestNodeOnOriginY = new TargetNode();
                            }
                            nearestNodeOnOriginY.copy(targetNode);
                        }
                    }
                    /*
                    ** on the Current Y
                    */
                    if ((currentB.getMaxY() > targetBounds.getMinY()) && (targetBounds.getMaxY() > currentB.getMinY())) {
                        if (nearestNodeOnCurrentY == null || nearestNodeOnCurrentY.topLeftDistance > targetNode.topRightDistance) {

                            if (nearestNodeOnCurrentY == null) {
                                nearestNodeOnCurrentY = new TargetNode();
                            }
                            nearestNodeOnCurrentY.copy(targetNode);
                        }
                    }
                    /*
                    ** Closest top right / top left corners.
                    */
                    if (nearestNodeTopRight == null || nearestNodeTopRight.topLeftDistance > targetNode.topLeftDistance) {
                        if (nearestNodeTopRight == null) {
                            nearestNodeTopRight = new TargetNode();
                        }
                        nearestNodeTopRight.copy(targetNode);
                    }

                    if (nearestNodeAverageRight == null || nearestNodeAverageRight.averageDistance > targetNode.averageDistance) {
                        if (nearestNodeAverageRight == null) {
                            nearestNodeAverageRight = new TargetNode();
                        }
                        nearestNodeAverageRight.copy(targetNode);
                    }

                    if (nearestNodeAnythingAnywhereRight == null || nearestNodeAnythingAnywhereRight.shortestDistance > targetNode.shortestDistance) {

                        if (nearestNodeAnythingAnywhereRight == null) {
                            nearestNodeAnythingAnywhereRight = new TargetNode();
                        }
                        nearestNodeAnythingAnywhereRight.copy(targetNode);
                    }
                }
            }
        }
        nodes.clear();

        if (reversingTargetNode != null) {
            reversingTargetNode.originTopLeftDistance = originTopRight2D.distance(reversingTargetNode.bounds.getMinX(), reversingTargetNode.bounds.getMinY());
        }
        if (nearestNodeOriginSimple2D != null) {
            nearestNodeOriginSimple2D.originTopLeftDistance = originTopRight2D.distance(nearestNodeOriginSimple2D.bounds.getMinX(), nearestNodeOriginSimple2D.bounds.getMinY());
        }
        if (nearestNodeCurrentSimple2D != null) {
            nearestNodeCurrentSimple2D.originTopLeftDistance = originTopRight2D.distance(nearestNodeCurrentSimple2D.bounds.getMinX(), nearestNodeCurrentSimple2D.bounds.getMinY());
        }
        if (nearestNodeOnOriginY != null) {
            nearestNodeOnOriginY.originTopLeftDistance = originTopRight2D.distance(nearestNodeOnOriginY.bounds.getMinX(), nearestNodeOnOriginY.bounds.getMinY());
        }
        if (nearestNodeOnCurrentY != null) {
            nearestNodeOnCurrentY.originTopLeftDistance = originTopRight2D.distance(nearestNodeOnCurrentY.bounds.getMinX(), nearestNodeOnCurrentY.bounds.getMinY());
        }
        if (nearestNodeAverageRight != null) {
            nearestNodeAverageRight.originTopLeftDistance = originTopRight2D.distance(nearestNodeAverageRight.bounds.getMinX(), nearestNodeAverageRight.bounds.getMinY());
        }
        if (nearestNodeTopRight != null) {
            nearestNodeTopRight.originTopLeftDistance = originTopRight2D.distance(nearestNodeTopRight.bounds.getMinX(), nearestNodeTopRight.bounds.getMinY());
        }
        if (nearestNodeAnythingAnywhereRight != null) {
            nearestNodeAnythingAnywhereRight.originTopLeftDistance = originTopRight2D.distance(nearestNodeAnythingAnywhereRight.bounds.getMinX(), nearestNodeAnythingAnywhereRight.bounds.getMinY());
        }

        if (nearestNodeOnCurrentY == null && nearestNodeOnOriginY == null && reversingTargetNode == null) {
            cacheStartTraversalNode = null;
            cacheStartTraversalDirection = null;
            reverseDirection = false;
            traversalNodeStack.clear();
        }

        if (focusLogger.isLoggable(Level.FINER)) {
            if (reversingTargetNode != null) {
                focusLogger.finer("reversingTargetNode.node : "+reversingTargetNode.node);
            }
            if (nearestNodeOriginSimple2D != null) {
                focusLogger.finer("nearestNodeOriginSimple2D.node : "+nearestNodeOriginSimple2D.node);
            }
            if (nearestNodeCurrentSimple2D != null) {
                focusLogger.finer("nearestNodeCurrentSimple2D.node : "+nearestNodeCurrentSimple2D.node);
            }
            if (nearestNodeOnOriginY != null) {
                focusLogger.finer("nearestNodeOnOriginY.node : "+nearestNodeOnOriginY.node);
            }
            if (nearestNodeOnCurrentY != null) {
                focusLogger.finer("nearestNodeOnCurrentY.node : "+nearestNodeOnCurrentY.node);
            }
            if (nearestNodeAverageRight != null) {
                focusLogger.finer("nearestNodeAverageRight.node : "+nearestNodeAverageRight.node);
            }
            if (nearestNodeTopRight != null) {
                focusLogger.finer("nearestNodeTopRight.node : "+nearestNodeTopRight.node);
            }
            if (nearestNodeAnythingAnywhereRight != null) {
                focusLogger.finer("nearestNodeAnythingAnywhereRight.node : "+nearestNodeAnythingAnywhereRight.node);
            }
        }

        /*
        ** we've just changed direction, and have a node on stack.
        ** there is a strong preference for this node, just make sure
        ** it's not a bad choice, as sometimes we got here as a last-chance
        */
        if (reversingTargetNode != null) {
            return reversingNode;
        }
        if (nearestNodeOnOriginY != null && nearestNodeOnOriginY.biasShortestDistance < Double.MAX_VALUE) {
            /*
            ** there's a preference, all else being equal, to return nearestNodeOnOriginY
            */
            if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null &&
                (nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node)) {
                return nearestNodeOnOriginY.node;
            }
            if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null && nearestNodeAverageRight != null &&
                (nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node) && (nearestNodeOnOriginY.node == nearestNodeAverageRight.node)) {
                return nearestNodeOnOriginY.node;
            }
            if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null && nearestNodeTopRight != null &&
                (nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node) && (nearestNodeOnOriginY.node == nearestNodeTopRight.node)) {
                return nearestNodeOnOriginY.node;
            }
            if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null && nearestNodeAnythingAnywhereRight != null &&
                (nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node) && (nearestNodeOnOriginY.node == nearestNodeAnythingAnywhereRight.node)) {
                return nearestNodeOnOriginY.node;
            }
            if (nearestNodeOnOriginY != null && nearestNodeAverageRight != null &&
                (nearestNodeOnOriginY.node == nearestNodeAverageRight.node)) {
                return nearestNodeOnOriginY.node;
            }

            if (nearestNodeOnCurrentY != null && nearestNodeOnCurrentY.biasShortestDistance < Double.MAX_VALUE) {
                if (nearestNodeOnOriginY == null ||
                    (nearestNodeOnCurrentY.topLeftDistance < nearestNodeOnOriginY.topLeftDistance) &&
                    (nearestNodeOnCurrentY.originTopLeftDistance < nearestNodeOnOriginY.originTopLeftDistance) &&
                    (nearestNodeOnCurrentY.bounds.getMinY() - currentTopRight2D.getY()) < (nearestNodeOnOriginY.bounds.getMinY() - currentTopRight2D.getY())  )  {

                    return nearestNodeOnCurrentY.node;
                }
                else if (nearestNodeOnOriginY != null) {
                    if (nearestNodeAverageRight != null || nearestNodeOnOriginY.averageDistance < nearestNodeAverageRight.averageDistance) {
                        return nearestNodeOnOriginY.node;
                    }
                }
            }
        }
        else {
            if (nearestNodeOnOriginY == null && nearestNodeOnCurrentY == null && nearestNodeCurrentSimple2D != null) {
                if (nearestNodeAverageRight != null && nearestNodeTopRight != null && nearestNodeAverageRight != null && nearestNodeAnythingAnywhereRight != null &&
                    nearestNodeAverageRight.node == nearestNodeTopRight.node && nearestNodeAverageRight.node == nearestNodeAnythingAnywhereRight.node) {
                    return nearestNodeAverageRight.node;
                }
                return nearestNodeCurrentSimple2D.node;
            }
            else if (nearestNodeAverageRight != null && nearestNodeTopRight != null && nearestNodeAnythingAnywhereRight != null &&
                     nearestNodeAverageRight.biasShortestDistance == nearestNodeTopRight.biasShortestDistance &&
                     nearestNodeAverageRight.biasShortestDistance == nearestNodeAnythingAnywhereRight.biasShortestDistance &&
                     nearestNodeAverageRight.biasShortestDistance < Double.MAX_VALUE) {

                if (nearestNodeOnOriginY != null && nearestNodeOnOriginY.originTopLeftDistance < nearestNodeAverageRight.originTopLeftDistance) {
                    return nearestNodeOnOriginY.node;
                }
                else {
                    return nearestNodeAverageRight.node;
                }
            }
        }
        /*
        ** is the average closer?
        */
        if (nearestNodeAverageRight != null && (nearestNodeOnOriginY == null || nearestNodeAverageRight.biasShortestDistance < nearestNodeOnOriginY.biasShortestDistance)) {
            /*
            ** but is one in the way
            */
            if (nearestNodeOnOriginY != null && (nearestNodeOnOriginY.bounds.getMinX() >= nearestNodeAverageRight.bounds.getMinX())) {
                return nearestNodeOnOriginY.node;
            }
            /*
            ** maybe Origin is better than this?
            */
            if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null &&  nearestNodeOnOriginY.biasShortestDistance < Double.MAX_VALUE && (nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node)) {
                return nearestNodeOnOriginY.node;
            }

            if (nearestNodeOnCurrentY != null && nearestNodeOnOriginY != null && nearestNodeOnCurrentY.biasShortestDistance < Double.MAX_VALUE && (nearestNodeOnCurrentY.biasShortestDistance < nearestNodeOnOriginY.biasShortestDistance)) {
                return nearestNodeOnCurrentY.node;
            }
            if (nearestNodeOnOriginY != null &&  nearestNodeAverageRight != null && nearestNodeOnOriginY.biasShortestDistance < Double.MAX_VALUE && (nearestNodeOnOriginY.originTopLeftDistance < nearestNodeAverageRight.originTopLeftDistance)) {
                return nearestNodeOnOriginY.node;
            }
            return nearestNodeAverageRight.node;
        }

        if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null && nearestNodeOnOriginY.bottomLeftDistance < nearestNodeOnCurrentY.bottomLeftDistance) {
            return nearestNodeOnOriginY.node;
        }

        /*
        ** if any of the remaining match we'll take that
        */
        if (nearestNodeOnCurrentY != null && nearestNodeTopRight != null && nearestNodeOnCurrentY.biasShortestDistance < Double.MAX_VALUE && (nearestNodeOnCurrentY.node == nearestNodeTopRight.node)) {
            return nearestNodeOnCurrentY.node;
        }
        /*
        ** There isn't a clear winner, just go to the one nearest the current
        ** focus owner, or if invalid then try the other contenders.
        */
        if (nearestNodeOnOriginY != null) {
            return nearestNodeOnOriginY.node;
        }
        else if (nearestNodeOriginSimple2D != null) {
            return nearestNodeOriginSimple2D.node;
        }
        else if (nearestNodeOnCurrentY != null) {
            return nearestNodeOnCurrentY.node;
        }
        else if (nearestNodeAverageRight != null) {
            return nearestNodeAverageRight.node;
        }
        else if (nearestNodeTopRight != null) {
            return nearestNodeTopRight.node;
        }
        else if (nearestNodeAnythingAnywhereRight != null) {
            return nearestNodeAnythingAnywhereRight.node;
        }
        return null;
    }


    static final class TargetNode {
        Node node = null;
        Bounds bounds = null;
        double biased2DMetric = Double.MAX_VALUE;
        double current2DMetric = Double.MAX_VALUE;

        double bottomLeftDistance = Double.MAX_VALUE;
        double midDistance = Double.MAX_VALUE;
        double bottomRightDistance = Double.MAX_VALUE;

        double shortestDistance = Double.MAX_VALUE;
        double biasShortestDistance = Double.MAX_VALUE;
        double averageDistance = Double.MAX_VALUE;

        double topLeftDistance = Double.MAX_VALUE;
        double topRightDistance = Double.MAX_VALUE;
        double originTopLeftDistance = Double.MAX_VALUE;
        double originTopRightDistance = Double.MAX_VALUE;
        double originBottomLeftDistance = Double.MAX_VALUE;
        double originBottomRightDistance = Double.MAX_VALUE;

        void copy(TargetNode source) {
            node = source.node;
            bounds = source.bounds;
            biased2DMetric = source.biased2DMetric;
            current2DMetric = source.current2DMetric;

            bottomLeftDistance = source.bottomLeftDistance;
            midDistance = source.midDistance;
            bottomRightDistance = source.bottomRightDistance;

            shortestDistance = source.shortestDistance;
            biasShortestDistance = source.biasShortestDistance;
            averageDistance = source.averageDistance;

            topLeftDistance = source.topLeftDistance;
            topRightDistance = source.topRightDistance;
            originTopLeftDistance = source.originTopLeftDistance;
            originTopRightDistance = source.originTopRightDistance;
            originBottomLeftDistance = source.originBottomLeftDistance;
            originBottomRightDistance = source.originBottomRightDistance;
        }
    }

    public static double findMin9(double d0, double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
        double doubleArray[] = new double[9];
        doubleArray[1] = d1;
        doubleArray[2] = d2;
        doubleArray[3] = d3;
        doubleArray[4] = d4;
        doubleArray[5] = d5;
        doubleArray[6] = d6;
        doubleArray[7] = d7;
        doubleArray[8] = d8;

        double minValue = d0;
        for (int i = 1 ; i < doubleArray.length ; i++) {
            minValue = (minValue <= doubleArray[i]) ? minValue : doubleArray[i];
        }
        return minValue;
    }
}

/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Node;

import static com.sun.javafx.scene.traversal.Direction.*;
import java.util.function.Function;


public class Hueristic2D implements Algorithm {

    Hueristic2D() {
    }

    @Override
    public Node select(Node node, Direction dir, TraversalContext context) {
        Node newNode = null;

        cacheTraversal(node, dir);

        if (NEXT.equals(dir) || NEXT_IN_LINE.equals(dir)) {
            newNode = TabOrderHelper.findNextFocusablePeer(node, context.getRoot(), dir == NEXT);
        }
        else if (PREVIOUS.equals(dir)) {
            newNode = TabOrderHelper.findPreviousFocusablePeer(node, context.getRoot());
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

            if (newNode == null) {
                Bounds currentB = node.localToScene(node.getLayoutBounds());
                if (cacheStartTraversalNode != null) {
                    Bounds cachedB = cacheStartTraversalNode.localToScene(cacheStartTraversalNode.getLayoutBounds());
                    switch (dir) {
                        case UP:
                        case DOWN:
                            newNode = getNearestNodeUpOrDown(currentB, cachedB, context, dir);
                            break;
                        case LEFT:
                        case RIGHT:
                            newNode = getNearestNodeLeftOrRight(currentB, cachedB, context, dir);
                            break;
                        default:
                            break;
                    }
                }
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

    @Override
    public Node selectFirst(TraversalContext context) {
        return TabOrderHelper.getFirstTargetNode(context.getRoot());
    }

    @Override
    public Node selectLast(TraversalContext context) {
        return TabOrderHelper.getLastTargetNode(context.getRoot());
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

    private void cacheTraversal(Node node, Direction dir) {
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
        } else {
            if (cacheStartTraversalNode == null || dir != cacheStartTraversalDirection) {

                if ((dir == UP && cacheStartTraversalDirection == DOWN) ||
                    (dir == DOWN && cacheStartTraversalDirection == UP) ||
                    (dir == LEFT && cacheStartTraversalDirection == RIGHT) ||
                    (dir == RIGHT && cacheStartTraversalDirection == LEFT) && !traversalNodeStack.empty()) {
                    reverseDirection = true;
                } else {
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
            } else {
                /*
                ** we're going this way again!
                */
                reverseDirection = false;
            }
        }
    }

    private static final Function<Bounds, Double> BOUNDS_TOP_SIDE = t -> t.getMinY();

    private static final Function<Bounds, Double> BOUNDS_BOTTOM_SIDE = t -> t.getMaxY();

    protected Node getNearestNodeUpOrDown(Bounds currentB, Bounds originB, TraversalContext context, Direction dir) {

        List<Node> nodes = context.getAllTargetNodes();

        Function<Bounds, Double> ySideInDirection = dir == DOWN ? BOUNDS_BOTTOM_SIDE : BOUNDS_TOP_SIDE;
        Function<Bounds, Double> ySideInOpositeDirection = dir == DOWN ? BOUNDS_TOP_SIDE : BOUNDS_BOTTOM_SIDE;

        Bounds biasedB = new BoundingBox(originB.getMinX(), currentB.getMinY(), originB.getWidth(), currentB.getHeight());

        Point2D currentMid2D = new Point2D(currentB.getMinX()+(currentB.getWidth()/2), ySideInDirection.apply(currentB));
        Point2D biasedMid2D = new Point2D(originB.getMinX()+(originB.getWidth()/2), ySideInDirection.apply(currentB));
        Point2D currentLeftCorner2D = new Point2D(currentB.getMinX(),ySideInDirection.apply(currentB));
        Point2D biasedLeftCorner2D = new Point2D(originB.getMinX(),ySideInDirection.apply(currentB));
        Point2D currentRightCorner2D = new Point2D(currentB.getMaxX(), ySideInDirection.apply(currentB));
        Point2D biasedRightCorner2D = new Point2D(originB.getMaxX(), ySideInDirection.apply(currentB));

        Point2D originLeftCorner2D = new Point2D(originB.getMinX(), ySideInDirection.apply(originB));

        TargetNode targetNode = new TargetNode();
        TargetNode nearestNodeCurrentSimple2D = null;
        TargetNode nearestNodeOriginSimple2D = null;
        TargetNode nearestNodeAverage = null;
        TargetNode nearestNodeOnOriginX = null;
        TargetNode nearestNodeOnCurrentX = null;
        TargetNode nearestNodeLeft = null;
        TargetNode nearestNodeAnythingAnywhere = null;

        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            final Node n = nodes.get(nodeIndex);

            Bounds targetBounds = n.localToScene(n.getLayoutBounds());
            /*
             ** check that the target node starts after we
             ** and the target node ends after we end
             */
            if (dir == UP ? (currentB.getMinY() > targetBounds.getMaxY())
                    : currentB.getMaxY() < targetBounds.getMinY()) {

                targetNode.node = n;
                targetNode.bounds = targetBounds;

                /*
                 * closest biased : simple 2d
                 * Negative means the Node is on the same Y axis. This will result in outdB == 0, making this a preferred Node.
                 */
                double outdB = Math.max(0, outDistance(dir, biasedB, targetBounds));

                if (isOnAxis(dir, biasedB, targetBounds)) {
                    targetNode.biased2DMetric = outdB + centerSideDistance(dir, biasedB, targetBounds) / 100;
                } else {
                    final double cosd = cornerSideDistance(dir, biasedB, targetBounds);
                    targetNode.biased2DMetric = 100000 + outdB * outdB + 9 * cosd * cosd;
                }
                /*
                 * closest current : simple 2d
                 * Negative means the Node is on the same Y axis. This will result in outdB == 0, making this a preferred Node.
                 */
                double outdC = Math.max(0, outDistance(dir, currentB, targetBounds));

                if (isOnAxis(dir, currentB, targetBounds)) {
                    targetNode.current2DMetric = outdC + centerSideDistance(dir, currentB, targetBounds) / 100;
                } else {
                    final double cosd = cornerSideDistance(dir, currentB, targetBounds);
                    targetNode.current2DMetric = 100000 + outdC * outdC + 9 * cosd * cosd;
                }

                targetNode.leftCornerDistance = currentLeftCorner2D.distance(targetBounds.getMinX(), ySideInOpositeDirection.apply(targetBounds));
                targetNode.rightCornerDistance = currentRightCorner2D.distance(targetBounds.getMaxX(), ySideInOpositeDirection.apply(targetBounds));

                double midDistance = currentMid2D.distance(targetBounds.getMinX() + (targetBounds.getWidth() / 2), ySideInOpositeDirection.apply(targetBounds));
                double currentLeftToTargetMidDistance = currentLeftCorner2D.distance(targetBounds.getMinX() + (targetBounds.getWidth() / 2), ySideInOpositeDirection.apply(targetBounds));
                double currentLeftToTargetRightDistance = currentLeftCorner2D.distance(targetBounds.getMaxX(), ySideInOpositeDirection.apply(targetBounds));
                double currentRightToTargetLeftDistance = currentRightCorner2D.distance(targetBounds.getMinX(), ySideInOpositeDirection.apply(targetBounds));
                double currentRightToTargetMidDistance = currentRightCorner2D.distance(targetBounds.getMinX() + (targetBounds.getWidth() / 2), ySideInOpositeDirection.apply(targetBounds));
                double currentRightToTargetRightDistance = currentRightCorner2D.distance(targetBounds.getMaxX(), ySideInOpositeDirection.apply(targetBounds));
                double currentMidToTargetLeftDistance = currentMid2D.distance(targetBounds.getMinX(), ySideInOpositeDirection.apply(targetBounds));
                double currentMidToTargetMidDistance = currentMid2D.distance(targetBounds.getMinX() + (targetBounds.getWidth() / 2), ySideInOpositeDirection.apply(targetBounds));
                double currentMidToTargetRightDistance = currentMid2D.distance(targetBounds.getMaxX(), ySideInOpositeDirection.apply(targetBounds));

                double biasLeftToTargetMidDistance = biasedLeftCorner2D.distance(targetBounds.getMinX() + (targetBounds.getWidth() / 2), ySideInOpositeDirection.apply(targetBounds));
                double biasLeftToTargetRightDistance = biasedLeftCorner2D.distance(targetBounds.getMaxX(), ySideInOpositeDirection.apply(targetBounds));
                double biasRightToTargetMidDistance = biasedRightCorner2D.distance(targetBounds.getMinX() + (targetBounds.getWidth() / 2), ySideInOpositeDirection.apply(targetBounds));
                double biasMidToTargetRightDistance = biasedMid2D.distance(targetBounds.getMaxX(), ySideInOpositeDirection.apply(targetBounds));

                targetNode.averageDistance
                        = (targetNode.leftCornerDistance + biasLeftToTargetMidDistance + biasLeftToTargetRightDistance
                        + currentRightToTargetLeftDistance + targetNode.rightCornerDistance + biasRightToTargetMidDistance + midDistance) / 7;

                targetNode.biasShortestDistance
                        = findMin(targetNode.leftCornerDistance, biasLeftToTargetMidDistance, biasLeftToTargetRightDistance,
                                currentRightToTargetLeftDistance, biasRightToTargetMidDistance, targetNode.rightCornerDistance,
                                currentMidToTargetLeftDistance, midDistance, biasMidToTargetRightDistance);

                targetNode.shortestDistance
                        = findMin(targetNode.leftCornerDistance, currentLeftToTargetMidDistance, currentLeftToTargetRightDistance,
                                currentRightToTargetLeftDistance, currentRightToTargetMidDistance, currentRightToTargetRightDistance,
                                currentMidToTargetLeftDistance, currentMidToTargetMidDistance, currentMidToTargetRightDistance);

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
                 ** Closest top left / bottom left corners.
                 */
                if (nearestNodeLeft == null || nearestNodeLeft.leftCornerDistance > targetNode.leftCornerDistance) {
                    if (((originB.getMinX() >= currentB.getMinX()) && (targetBounds.getMinX() >= currentB.getMinX()))
                            || ((originB.getMinX() <= currentB.getMinX()) && (targetBounds.getMinX() <= currentB.getMinX()))) {

                        if (nearestNodeLeft == null) {
                            nearestNodeLeft = new TargetNode();
                        }
                        nearestNodeLeft.copy(targetNode);
                    }
                }

                if (nearestNodeAverage == null || nearestNodeAverage.averageDistance > targetNode.averageDistance) {
                    if (((originB.getMinX() >= currentB.getMinX()) && (targetBounds.getMinX() >= currentB.getMinX()))
                            || ((originB.getMinX() <= currentB.getMinX()) && (targetBounds.getMinX() <= currentB.getMinX()))) {

                        if (nearestNodeAverage == null) {
                            nearestNodeAverage = new TargetNode();
                        }
                        nearestNodeAverage.copy(targetNode);
                    }
                }

                if (nearestNodeAnythingAnywhere == null || nearestNodeAnythingAnywhere.shortestDistance > targetNode.shortestDistance) {

                    if (nearestNodeAnythingAnywhere == null) {
                        nearestNodeAnythingAnywhere = new TargetNode();
                    }
                    nearestNodeAnythingAnywhere.copy(targetNode);
                }
            }
        }
        nodes.clear();

        if (nearestNodeOnOriginX != null) {
            nearestNodeOnOriginX.originLeftCornerDistance = originLeftCorner2D.distance(nearestNodeOnOriginX.bounds.getMinX(), ySideInOpositeDirection.apply(nearestNodeOnOriginX.bounds));
        }

        if (nearestNodeOnCurrentX != null) {
            nearestNodeOnCurrentX.originLeftCornerDistance = originLeftCorner2D.distance(nearestNodeOnCurrentX.bounds.getMinX(), ySideInOpositeDirection.apply(nearestNodeOnCurrentX.bounds));
        }

        if (nearestNodeAverage != null) {
            nearestNodeAverage.originLeftCornerDistance = originLeftCorner2D.distance(nearestNodeAverage.bounds.getMinX(), ySideInOpositeDirection.apply(nearestNodeAverage.bounds));
        }

        if (nearestNodeOnOriginX != null) {
            /*
            ** there's a preference, all else being equal, to return nearestNodeOnOriginX
            */
            if (nearestNodeOnCurrentX != null && nearestNodeOnOriginX.node == nearestNodeOnCurrentX.node
                    && ((nearestNodeAverage != null && nearestNodeOnOriginX.node == nearestNodeAverage.node)
                    || (nearestNodeOriginSimple2D != null && nearestNodeOnOriginX.node == nearestNodeOriginSimple2D.node)
                    || (nearestNodeLeft != null && nearestNodeOnOriginX.node == nearestNodeLeft.node)
                    || (nearestNodeAnythingAnywhere != null && nearestNodeOnOriginX.node == nearestNodeAnythingAnywhere.node))) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeAverage != null && nearestNodeOnOriginX.node == nearestNodeAverage.node) {
                return nearestNodeOnOriginX.node;
            }

            if (nearestNodeOnCurrentX != null) {
                if ((nearestNodeOnCurrentX.leftCornerDistance < nearestNodeOnOriginX.leftCornerDistance) &&
                    (nearestNodeOnCurrentX.originLeftCornerDistance < nearestNodeOnOriginX.originLeftCornerDistance) &&
                (nearestNodeOnCurrentX.bounds.getMinX() - currentLeftCorner2D.getX()) < (nearestNodeOnOriginX.bounds.getMinX() - currentLeftCorner2D.getX())) {

                    return nearestNodeOnCurrentX.node;
                } else if (nearestNodeAverage == null || nearestNodeOnOriginX.averageDistance < nearestNodeAverage.averageDistance) {
                    return nearestNodeOnOriginX.node;
                }
            }
        } else {
            if (nearestNodeOnCurrentX == null && nearestNodeCurrentSimple2D != null) {
                if (nearestNodeAverage != null && nearestNodeLeft != null && (nearestNodeAverage.node == nearestNodeLeft.node && nearestNodeAverage.node == nearestNodeAnythingAnywhere.node)) {
                    return nearestNodeAverage.node;
                }
                return nearestNodeCurrentSimple2D.node;
            } else if (nearestNodeAverage != null && nearestNodeLeft != null && nearestNodeAnythingAnywhere != null
                    &&     nearestNodeAverage.biasShortestDistance == nearestNodeLeft.biasShortestDistance &&
                     nearestNodeAverage.biasShortestDistance == nearestNodeAnythingAnywhere.biasShortestDistance &&
                     nearestNodeAverage.biasShortestDistance < Double.MAX_VALUE) {

                    return nearestNodeAverage.node;
            }
        }

        /*
        ** is the average closer?
        */
        if (nearestNodeAverage != null && (nearestNodeOnOriginX == null || (nearestNodeAverage.biasShortestDistance < nearestNodeOnOriginX.biasShortestDistance))) {
            /*
            ** but is one in the way
            */
            if (nearestNodeOnOriginX != null && (ySideInOpositeDirection.apply(nearestNodeOnOriginX.bounds) >= ySideInOpositeDirection.apply(nearestNodeAverage.bounds))) {
                return nearestNodeOnOriginX.node;
            }
            if (nearestNodeOriginSimple2D != null) {
                if (nearestNodeOriginSimple2D.current2DMetric <= nearestNodeAverage.current2DMetric) {
                    return nearestNodeOriginSimple2D.node;
                }
                if (ySideInOpositeDirection.apply(nearestNodeOriginSimple2D.bounds) >= ySideInOpositeDirection.apply(nearestNodeAverage.bounds)) {
                    return nearestNodeOriginSimple2D.node;
                }
            }
            return nearestNodeAverage.node;
        }

        /*
        ** this is an odd one, in that is isn't the closest on current, or on the
        ** origin, but it looks better for most cases...
        */
        if ((nearestNodeCurrentSimple2D != null && nearestNodeOnCurrentX != null && nearestNodeAverage != null && nearestNodeLeft != null && nearestNodeAnythingAnywhere != null) &&
            (nearestNodeCurrentSimple2D.node == nearestNodeOnCurrentX.node) &&
            (nearestNodeCurrentSimple2D.node ==  nearestNodeAverage.node) &&
            (nearestNodeCurrentSimple2D.node == nearestNodeLeft.node) &&
            (nearestNodeCurrentSimple2D.node == nearestNodeAnythingAnywhere.node)) {
            return nearestNodeCurrentSimple2D.node;
        }

        if (nearestNodeOnOriginX != null && (nearestNodeOnCurrentX == null || (nearestNodeOnOriginX.rightCornerDistance < nearestNodeOnCurrentX.rightCornerDistance))) {
            return nearestNodeOnOriginX.node;
        }
        /*
        ** There isn't a clear winner, just go to the one nearest the current
         ** focus owner, or if invalid then try the other contenders.
         */
        if (nearestNodeOnOriginX != null) {
            return nearestNodeOnOriginX.node;
        } else if (nearestNodeOriginSimple2D != null) {
            return nearestNodeOriginSimple2D.node;
        } else if (nearestNodeOnCurrentX != null) {
            return nearestNodeOnCurrentX.node;
        } else if (nearestNodeAverage != null) {
            return nearestNodeAverage.node;
        } else if (nearestNodeLeft != null) {
            return nearestNodeLeft.node;
        } else if (nearestNodeAnythingAnywhere != null) {
            return nearestNodeAnythingAnywhere.node;
        }
        return null;
    }

    private static final Function<Bounds, Double> BOUNDS_LEFT_SIDE = t -> t.getMinX();

    private static final Function<Bounds, Double> BOUNDS_RIGHT_SIDE = t -> t.getMaxX();

    protected Node getNearestNodeLeftOrRight(Bounds currentB, Bounds originB, TraversalContext context, Direction dir) {

        List<Node> nodes = context.getAllTargetNodes();

        Function<Bounds, Double> xSideInDirection = dir == LEFT ? BOUNDS_LEFT_SIDE : BOUNDS_RIGHT_SIDE;
        Function<Bounds, Double> xSideInOpositeDirection = dir == LEFT ? BOUNDS_RIGHT_SIDE : BOUNDS_LEFT_SIDE;

        Bounds biasedB = new BoundingBox(currentB.getMinX(), originB.getMinY(), currentB.getWidth(), originB.getHeight());

        Point2D currentMid2D = new Point2D(xSideInDirection.apply(currentB), currentB.getMinY()+(currentB.getHeight()/2));
        Point2D biasedMid2D = new Point2D(xSideInDirection.apply(currentB), originB.getMinY()+(originB.getHeight()/2));
        Point2D currentTopCorner2D = new Point2D(xSideInDirection.apply(currentB), currentB.getMinY());
        Point2D biasedTopCorner2D = new Point2D(xSideInDirection.apply(currentB), originB.getMinY());
        Point2D currentBottomCorner2D = new Point2D(xSideInDirection.apply(currentB), currentB.getMaxY());
        Point2D biasedBottomCorner2D = new Point2D(xSideInDirection.apply(currentB), originB.getMaxY());

        Point2D originTopCorner2D = new Point2D(xSideInDirection.apply(originB), originB.getMinY());

        TargetNode targetNode = new TargetNode();
        TargetNode nearestNodeCurrentSimple2D = null;
        TargetNode nearestNodeOriginSimple2D = null;
        TargetNode nearestNodeAverage = null;
        TargetNode nearestNodeOnOriginY = null;
        TargetNode nearestNodeOnCurrentY = null;
        TargetNode nearestNodeTopLeft = null;
        TargetNode nearestNodeAnythingAnywhereLeft = null;

        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            final Node n = nodes.get(nodeIndex);

            Bounds targetBounds = n.localToScene(n.getLayoutBounds());
            /*
             ** check that the target node starts after we start
             ** and the target node ends after we end
             */
            if (dir == LEFT ? currentB.getMinX() > targetBounds.getMinX()
                    : currentB.getMaxX() < targetBounds.getMaxX()) {

                targetNode.node = n;
                targetNode.bounds = targetBounds;

                /*
                 * closest biased : simple 2d.
                 * Negative means the Node is on the same Y axis. This will result in outdB == 0, making this a preferred Node.
                 */
                double outdB = Math.max(0, outDistance(dir, biasedB, targetBounds));

                if (isOnAxis(dir, biasedB, targetBounds)) {
                    targetNode.biased2DMetric = outdB + centerSideDistance(dir, biasedB, targetBounds) / 100;
                } else {
                    final double cosd = cornerSideDistance(dir, biasedB, targetBounds);
                    targetNode.biased2DMetric = 100000 + outdB * outdB + 9 * cosd * cosd;
                }
                /*
                 * closest current : simple 2d
                 *Negative means the Node is on the same Y axis. This will result in outdB == 0, making this a preferred Node.
                 */
                double outdC = Math.max(0, outDistance(dir, currentB, targetBounds));

                if (isOnAxis(dir, currentB, targetBounds)) {
                    targetNode.current2DMetric = outdC + centerSideDistance(dir, currentB, targetBounds) / 100;
                } else {
                    final double cosd = cornerSideDistance(dir, currentB, targetBounds);
                    targetNode.current2DMetric = 100000 + outdC * outdC + 9 * cosd * cosd;
                }

                targetNode.topCornerDistance = currentTopCorner2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMinY());
                targetNode.bottomCornerDistance = currentBottomCorner2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMaxY());

                double midDistance = currentMid2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMinY() + (targetBounds.getHeight() / 2));
                double currentTopToTargetBottomDistance = currentTopCorner2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMaxY());
                double currentTopToTargetMidDistance = currentTopCorner2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMinY() + (targetBounds.getHeight() / 2));
                double currentBottomToTargetTopDistance = currentBottomCorner2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMinY());
                double currentBottomToTargetBottomDistance = currentBottomCorner2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMaxY());
                double currentBottomToTargetMidDistance = currentBottomCorner2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMinY() + (targetBounds.getHeight() / 2));
                double currentMidToTargetTopDistance = currentMid2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMinY());
                double currentMidToTargetBottomDistance = currentMid2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMaxY());
                double currentMidToTargetMidDistance = currentMid2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMinY() + (targetBounds.getHeight() / 2));

                double biasTopToTargetBottomDistance = biasedTopCorner2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMaxY());
                double biasTopToTargetMidDistance = biasedTopCorner2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMinY() + (targetBounds.getHeight() / 2));
                double biasBottomToTargetMidDistance = biasedBottomCorner2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMinY() + (targetBounds.getHeight() / 2));
                double biasMidToTargetBottomDistance = biasedMid2D.distance(xSideInOpositeDirection.apply(targetBounds), targetBounds.getMaxY());

                targetNode.averageDistance
                        = (targetNode.topCornerDistance + biasTopToTargetBottomDistance + biasTopToTargetMidDistance
                        + currentBottomToTargetTopDistance + targetNode.bottomCornerDistance + biasBottomToTargetMidDistance + midDistance) / 7;

                targetNode.biasShortestDistance
                        = findMin(targetNode.topCornerDistance, biasTopToTargetBottomDistance, biasTopToTargetMidDistance,
                                currentBottomToTargetTopDistance, targetNode.bottomCornerDistance, biasBottomToTargetMidDistance,
                                currentMidToTargetTopDistance, biasMidToTargetBottomDistance, midDistance);

                targetNode.shortestDistance
                        = findMin(targetNode.topCornerDistance, currentTopToTargetBottomDistance, currentTopToTargetMidDistance,
                                currentBottomToTargetTopDistance, currentBottomToTargetBottomDistance, currentBottomToTargetMidDistance,
                                currentMidToTargetTopDistance, currentMidToTargetBottomDistance, currentMidToTargetMidDistance);

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
                    if (nearestNodeOnOriginY == null || nearestNodeOnOriginY.topCornerDistance > targetNode.topCornerDistance) {

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
                    if (nearestNodeOnCurrentY == null || nearestNodeOnCurrentY.topCornerDistance > targetNode.topCornerDistance) {

                        if (nearestNodeOnCurrentY == null) {
                            nearestNodeOnCurrentY = new TargetNode();
                        }
                        nearestNodeOnCurrentY.copy(targetNode);
                    }
                }
                /*
                 ** Closest top left / top right corners.
                 */
                if (nearestNodeTopLeft == null || nearestNodeTopLeft.topCornerDistance > targetNode.topCornerDistance) {

                    if (nearestNodeTopLeft == null) {
                        nearestNodeTopLeft = new TargetNode();
                    }
                    nearestNodeTopLeft.copy(targetNode);
                }

                if (nearestNodeAverage == null || nearestNodeAverage.averageDistance > targetNode.averageDistance) {

                    if (nearestNodeAverage == null) {
                        nearestNodeAverage = new TargetNode();
                    }
                    nearestNodeAverage.copy(targetNode);
                }

                if (nearestNodeAnythingAnywhereLeft == null || nearestNodeAnythingAnywhereLeft.shortestDistance > targetNode.shortestDistance) {

                    if (nearestNodeAnythingAnywhereLeft == null) {
                        nearestNodeAnythingAnywhereLeft = new TargetNode();
                    }
                    nearestNodeAnythingAnywhereLeft.copy(targetNode);
                }
            }
        }
        nodes.clear();

        if (nearestNodeOnOriginY != null) {
            nearestNodeOnOriginY.originTopCornerDistance = originTopCorner2D.distance(xSideInOpositeDirection.apply(nearestNodeOnOriginY.bounds), nearestNodeOnOriginY.bounds.getMinY());
        }

        if (nearestNodeOnCurrentY != null) {
            nearestNodeOnCurrentY.originTopCornerDistance = originTopCorner2D.distance(xSideInOpositeDirection.apply(nearestNodeOnCurrentY.bounds), nearestNodeOnCurrentY.bounds.getMinY());
        }

        if (nearestNodeAverage != null) {
            nearestNodeAverage.originTopCornerDistance = originTopCorner2D.distance(xSideInOpositeDirection.apply(nearestNodeAverage.bounds), nearestNodeAverage.bounds.getMinY());
        }

        if (nearestNodeOnCurrentY == null && nearestNodeOnOriginY == null) {
            cacheStartTraversalNode = null;
            cacheStartTraversalDirection = null;
            reverseDirection = false;
            traversalNodeStack.clear();
        }

        if (nearestNodeOnOriginY != null) {
            /*
             ** there's a preference, all else being equal, to return nearestNodeOnOriginY
             */
            if (nearestNodeOnCurrentY != null && nearestNodeOnOriginY.node == nearestNodeOnCurrentY.node
                    && ((nearestNodeAverage != null && nearestNodeOnOriginY.node == nearestNodeAverage.node)
                    || (nearestNodeTopLeft != null && nearestNodeOnOriginY.node == nearestNodeTopLeft.node)
                    || (nearestNodeAnythingAnywhereLeft != null && nearestNodeOnOriginY.node == nearestNodeAnythingAnywhereLeft.node))) {
                return nearestNodeOnOriginY.node;
            }

            if (nearestNodeAverage != null && nearestNodeOnOriginY.node == nearestNodeAverage.node) {
                return nearestNodeOnOriginY.node;
            }

            if (nearestNodeOnCurrentY != null) {
                if ((nearestNodeOnCurrentY.bottomCornerDistance < nearestNodeOnOriginY.bottomCornerDistance)
                        && (nearestNodeOnCurrentY.originTopCornerDistance < nearestNodeOnOriginY.originTopCornerDistance)
                        && (nearestNodeOnCurrentY.bounds.getMinY() - currentTopCorner2D.getY()) < (nearestNodeOnOriginY.bounds.getMinY() - currentTopCorner2D.getY())) {

                    return nearestNodeOnCurrentY.node;
                } else if (nearestNodeAverage == null || nearestNodeOnOriginY.averageDistance < nearestNodeAverage.averageDistance) {
                    return nearestNodeOnOriginY.node;
                }
            }
        } else {
            if (nearestNodeOnCurrentY == null && nearestNodeCurrentSimple2D != null) {
                if (nearestNodeAverage != null && nearestNodeTopLeft != null
                        && nearestNodeAverage.node == nearestNodeTopLeft.node && nearestNodeAverage.node == nearestNodeAnythingAnywhereLeft.node) {
                    return nearestNodeAverage.node;
                }
                return nearestNodeCurrentSimple2D.node;
            } else if (nearestNodeAverage != null && nearestNodeTopLeft != null && nearestNodeAnythingAnywhereLeft != null
                    && nearestNodeAverage.biasShortestDistance == nearestNodeTopLeft.biasShortestDistance
                    && nearestNodeAverage.biasShortestDistance == nearestNodeAnythingAnywhereLeft.biasShortestDistance
                    && nearestNodeAverage.biasShortestDistance < Double.MAX_VALUE) {
                    return nearestNodeAverage.node;
            }
        }

        /*
        ** is the average closer?
        */
        if (nearestNodeAverage != null && (nearestNodeOnOriginY == null || nearestNodeAverage.biasShortestDistance < nearestNodeOnOriginY.biasShortestDistance)) {
            /*
            ** but is one in the way
            */
            if (nearestNodeOnOriginY != null && (xSideInOpositeDirection.apply(nearestNodeOnOriginY.bounds) >= xSideInOpositeDirection.apply(nearestNodeAverage.bounds))) {
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

            if (nearestNodeOnOriginY != null && nearestNodeOnOriginY.biasShortestDistance < Double.MAX_VALUE && (nearestNodeOnOriginY.originTopCornerDistance < nearestNodeAverage.originTopCornerDistance)) {
                return nearestNodeOnOriginY.node;
            }
            return nearestNodeAverage.node;
        }


        if (nearestNodeOnOriginY != null && nearestNodeOnCurrentY != null && nearestNodeOnOriginY.bottomCornerDistance < nearestNodeOnCurrentY.bottomCornerDistance) {
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
        } else if (nearestNodeOriginSimple2D != null) {
            return nearestNodeOriginSimple2D.node;
        } else if (nearestNodeOnCurrentY != null) {
            return nearestNodeOnCurrentY.node;
        } else if (nearestNodeAverage != null) {
            return nearestNodeAverage.node;
        } else if (nearestNodeTopLeft != null) {
            return nearestNodeTopLeft.node;
        } else if (nearestNodeAnythingAnywhereLeft != null) {
            return nearestNodeAnythingAnywhereLeft.node;
        }
        return null;
    }

    static final class TargetNode {
        Node node = null;
        Bounds bounds = null;
        double biased2DMetric = Double.MAX_VALUE;
        double current2DMetric = Double.MAX_VALUE;

        double leftCornerDistance = Double.MAX_VALUE;
        double rightCornerDistance = Double.MAX_VALUE;
        double topCornerDistance = Double.MAX_VALUE;
        double bottomCornerDistance = Double.MAX_VALUE;

        double shortestDistance = Double.MAX_VALUE;
        double biasShortestDistance = Double.MAX_VALUE;
        double averageDistance = Double.MAX_VALUE;

        double originLeftCornerDistance = Double.MAX_VALUE;
        double originTopCornerDistance = Double.MAX_VALUE;

        void copy(TargetNode source) {
            node = source.node;
            bounds = source.bounds;
            biased2DMetric = source.biased2DMetric;
            current2DMetric = source.current2DMetric;

            leftCornerDistance = source.leftCornerDistance;
            rightCornerDistance = source.rightCornerDistance;

            shortestDistance = source.shortestDistance;
            biasShortestDistance = source.biasShortestDistance;
            averageDistance = source.averageDistance;

            topCornerDistance = source.topCornerDistance;
            bottomCornerDistance = source.bottomCornerDistance;
            originLeftCornerDistance = source.originLeftCornerDistance;
            originTopCornerDistance = source.originTopCornerDistance;
        }
    }

    public static double findMin(double... values) {

        double minValue = Double.MAX_VALUE;

        for (int i = 0 ; i < values.length ; i++) {
            minValue = (minValue < values[i]) ? minValue : values[i];
        }
        return minValue;
    }
}

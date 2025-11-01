/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.TraversalDirection;
import javafx.scene.TraversalPolicy;

public class ContainerTabOrder extends TraversalPolicy {

    ContainerTabOrder() {
    }

    @Override
    public Node select(Parent root, Node node, TraversalDirection dir) {
        switch (dir) {
            case NEXT:
                return findNextFocusableNode(root, node);
            case NEXT_IN_LINE:
                return findNextInLineFocusableNode(root, node);
            case PREVIOUS:
                return findPreviousFocusableNode(root, node);
            case UP:
            case DOWN:
            case LEFT:
            case RIGHT:
                List<Node> nodes = TraversalUtils.getAllTargetNodes(root);
                int target = trav2D(TraversalUtils.getLayoutBoundsInSceneCoordinates(node), dir, nodes, root);
                if (target != -1) {
                    return nodes.get(target);
                }
        }
        return null;
    }

    @Override
    public Node selectFirst(Parent root) {
        return TabOrderHelper.getFirstTargetNode(root);
    }

    @Override
    public Node selectLast(Parent root) {
        return TabOrderHelper.getLastTargetNode(root);
    }

    private int trav2D(Bounds origin, TraversalDirection dir, List<Node> peers, Parent root) {

        Bounds bestBounds = null;
        double bestMetric = 0.0;
        int bestIndex = -1;

        for (int i = 0; i < peers.size(); i++) {
            final Bounds targetBounds = TraversalUtils.getLayoutBoundsInSceneCoordinates(peers.get(i));
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

    private boolean isOnAxis(TraversalDirection dir, Bounds cur, Bounds tgt) {

        final double cmin, cmax, tmin, tmax;

        if (dir == TraversalDirection.UP || dir == TraversalDirection.DOWN) {
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
    private double outDistance(TraversalDirection dir, Bounds cur, Bounds tgt) {

        final double distance;

        if (dir == TraversalDirection.UP) {
            distance = cur.getMinY() - tgt.getMaxY();
        }
        else if (dir == TraversalDirection.DOWN) {
            distance = tgt.getMinY() - cur.getMaxY();
        }
        else if (dir == TraversalDirection.LEFT) {
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
    private double centerSideDistance(TraversalDirection dir, Bounds cur, Bounds tgt) {

        final double cc; // current center
        final double tc; // target center

        if (dir == TraversalDirection.UP || dir == TraversalDirection.DOWN) {
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
    private double cornerSideDistance(TraversalDirection dir, Bounds cur, Bounds tgt) {

        final double distance;

        if (dir == TraversalDirection.UP || dir == TraversalDirection.DOWN) {

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

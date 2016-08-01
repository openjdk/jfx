/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

/**
 * This TouchFilter merges together points that are closer together than a
 * certain threshold.
 */
final class NearbyPointsTouchFilter implements TouchFilter {

    private final int radius = TouchInput.getInstance()
            .getTouchRadius();

    @Override
    public int getPriority() {
        return PRIORITY_PRE_ID;
    }

    @Override
    public boolean filter(TouchState state) {
        // keep merging points until the closest points are further part than
        // radius
        while (mergeClosestPoints(state)) {
        }
        return false;
    }

    private boolean mergeClosestPoints(TouchState state) {
        int pointIndex1 = -1, pointIndex2 = -1;
        int closestDistanceSquared = Integer.MAX_VALUE;
        for (int i = 0; i < state.getPointCount(); i++) {
            for (int j = i + 1; j < state.getPointCount(); j++) {
                TouchState.Point p1 = state.getPoint(i);
                TouchState.Point p2 = state.getPoint(j);
                int dx = p1.x - p2.x;
                int dy = p1.y - p2.y;
                int distanceSquared = dx * dx + dy * dy;
                if (distanceSquared < closestDistanceSquared) {
                    closestDistanceSquared = distanceSquared;
                    pointIndex1 = i;
                    pointIndex2 = j;
                }
            }
        }
        if (closestDistanceSquared < radius * radius) {
            TouchState.Point p1 = state.getPoint(pointIndex1);
            TouchState.Point p2 = state.getPoint(pointIndex2);
            p1.x = (p1.x + p2.x) / 2;
            p1.y = (p1.y + p2.y) / 2;
            state.removePointForID(p2.id);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean flush(TouchState state) {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NearbyPointsTouchFilter;
    }


    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "NearbyPoints";
    }

}

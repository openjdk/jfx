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

import java.util.Arrays;

final class AssignPointIDTouchFilter implements TouchFilter {

    private final TouchState oldState = new TouchState();
    private int[] mappedIndices = new int[1];
    private IntSet ids = new IntSet();
    private int nextID = 1;

    AssignPointIDTouchFilter() {
        TouchInput.getInstance().getState(oldState);
    }

    /** Acquire a touch point ID */
    private int acquireID() {
        ids.addInt(nextID);
        return nextID ++;
    }

    /** Release a touch point ID */
    private void releaseID(int id) {
        ids.removeInt(id);
        nextID = 1;
        for (int i = 0; i < ids.size(); i++) {
            nextID = Math.max(ids.get(i) + 1, nextID);
        }
    }

    @Override
    public int getPriority() {
        return PRIORITY_ID;
    }

    /** Assign touch point IDs, for protocol A multitouch drivers that do not
     * assign IDs themselves. */
    @Override
    public boolean filter(TouchState state) {
        if (oldState.getPointCount() == 0) {
            for (int i = 0; i < state.getPointCount(); i++) {
                state.getPoint(i).id = acquireID();
            }
        } else if (state.getPointCount() >= oldState.getPointCount()) {
            // For each existing touch point, find the closest pending touch
            // point.
            // mappedIndices contains 0 for every unmapped pending touch point
            // index  and 1 for every pending touch point index that has
            // already been mapped to an existing touch point.
            if (mappedIndices.length < state.getPointCount()) {
                mappedIndices = new int[state.getPointCount()];
            } else {
                Arrays.fill(mappedIndices, 0);
            }
            int mappedIndexCount = 0;
            for (int i = 0; i < oldState.getPointCount(); i++) {
                TouchState.Point oldPoint = oldState.getPoint(i);
                int x = oldPoint.x;
                int y = oldPoint.y;
                int closestDistanceSquared = Integer.MAX_VALUE;
                int mappedIndex = -1;
                for (int j = 0; j < state.getPointCount(); j++) {
                    if (mappedIndices[j] == 0) {
                        TouchState.Point newPoint = state.getPoint(j);
                        int distanceX = x - newPoint.x;
                        int distanceY = y - newPoint.y;
                        int distanceSquared = distanceX * distanceX + distanceY * distanceY;
                        if (distanceSquared < closestDistanceSquared) {
                            mappedIndex = j;
                            closestDistanceSquared = distanceSquared;
                        }
                    }
                }
                assert(mappedIndex >= 0);
                state.getPoint(mappedIndex).id = oldPoint.id;
                mappedIndexCount ++;
                mappedIndices[mappedIndex] = 1;
            }
            if (mappedIndexCount < state.getPointCount()) {
                for (int i = 0; i < state.getPointCount(); i++) {
                    if (mappedIndices[i] == 0) {
                        state.getPoint(i).id = acquireID();
                    }
                }
            }
        } else {
            // There are more existing touch points than pending touch points.
            // For each pending touch point, find the closest existing touch
            // point.
            // mappedIndices contains 0 for every unmapped pre-existing touch
            // index and 1 for every pre-existing touch index that has already
            // been mapped to a pending touch point
            if (mappedIndices.length < oldState.getPointCount()) {
                mappedIndices = new int[oldState.getPointCount()];
            } else {
                Arrays.fill(mappedIndices, 0);
            }
            int mappedIndexCount = 0;
            for (int i = 0; i < state.getPointCount()
                    && mappedIndexCount < oldState.getPointCount(); i++) {
                TouchState.Point newPoint = state.getPoint(i);
                int x = newPoint.x;
                int y = newPoint.y;
                int j;
                int closestDistanceSquared = Integer.MAX_VALUE;
                int mappedIndex = -1;
                for (j = 0; j < oldState.getPointCount(); j++) {
                    if (mappedIndices[j] == 0) {
                        TouchState.Point oldPoint = oldState.getPoint(j);
                        int distanceX = x - oldPoint.x;
                        int distanceY = y - oldPoint.y;
                        int distanceSquared = distanceX * distanceX + distanceY * distanceY;
                        if (distanceSquared < closestDistanceSquared) {
                            mappedIndex = j;
                            closestDistanceSquared = distanceSquared;
                        }
                    }
                }
                assert(mappedIndex >= 0);
                state.getPoint(i).id = oldState.getPoint(mappedIndex).id;
                mappedIndexCount ++;
                mappedIndices[mappedIndex] = 1;
            }
        }
        // Release unused IDs. This can only be done after we have finished
        // assigning all new IDs.
        for (int i = 0; i < oldState.getPointCount(); i++) {
            int id = oldState.getPoint(i).id;
            TouchState.Point p = state.getPointForID(id);
            if (p == null) {
                releaseID(id);
            }
        }
        state.copyTo(oldState);
        return false;
    }

    @Override
    public boolean flush(TouchState state) {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AssignPointIDTouchFilter;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "AssignPointID";
    }

}

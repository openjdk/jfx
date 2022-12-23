/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Comparator;

/**
 * TouchState is a snapshot of touch points and their coordinates.
 * TouchState is used both to store the current state of touch input and to
 * describe changes to that state.
 *
 * A TouchState contains a number of Points. Each point has a unique ID,
 * which is either assigned by a touch driver, or by JavaFX's input
 * processing code. One touch point is defined as the primary touch point; it
 * is this touch point's coordinates that are used to determine the
 * coordinates of synthesized mouse events.
 */
class TouchState {

    /** Describes a single touch point */
    static class Point {
        int id;
        int x;
        int y;

        /**
         * Copies a touch point's data to a target Point
         *
         * @param target the Point object to which to copy this object's data
         */
        void copyTo(Point target) {
            target.id = id;
            target.x = x;
            target.y = y;
        }
        @Override
        public String toString() {
            return "TouchState.Point[id=" + id + ",x=" + x + ",y="  + y + "]";
        }
    }

    private static Comparator<Point> pointIdComparator = (p1, p2) -> p1.id - p2.id;

    private Point[] points = new Point[1];
    private int pointCount = 0;
    private int primaryID = -1;
    private MonocleWindow window;

    /** Returns the Glass window on which this event state is located.
     * assignPrimaryID() should be called before this method.
     *
     * @param recalculateCache true if the cached value should be discarded and
     *                         recomputed
     * @param fallback the window to use if no primary ID is available
     */
    MonocleWindow getWindow(boolean recalculateCache, MonocleWindow fallback) {
        if (window == null) {
            window = fallback;
        }
        if (recalculateCache) {
            window = fallback;
            if (primaryID >= 0) {
                Point p = getPointForID(primaryID);
                if (p != null) {
                    window = MonocleWindowManager.getInstance().getWindowForLocation(p.x, p.y);
                }
            }
        }
        return window;
    }

    /**
     * Returns the nth point in the toich point list, for index n
     *
     * @param index The index of the point point to return. index should be less
     *              than the value returned by getPointCount().
     * @return A touch point.
     */
    Point getPoint(int index) {
        return points[index];
    }

    /** Gets the Point matching the given ID. if available
     * @param id The Point ID to match. A value of -1 matches any Point.
     * @return a matching Point, or null if there is no point with that ID.
     */
    Point getPointForID(int id) {
        for (int i = 0; i < pointCount; i++) {
            if (id == -1 || points[i].id == id) {
                return points[i];
            }
        }
        return null;
    }

    /** Returns the touch point ID of the primary point. */
    int getPrimaryID() {
        return primaryID;
    }

    /** Updates the primary point ID */
    void assignPrimaryID() {
        if (pointCount == 0) {
            primaryID = -1;
        } else if (primaryID <= 0) {
            // No primary ID is assigned. Assign a new ID arbitrarily.
            primaryID = points[0].id;
        } else {
            for (int i = 0; i < pointCount; i++) {
                if (points[i].id == primaryID) {
                    // The old primary ID is still valid
                    return;
                }
            }
            // assign a new primary ID
            primaryID = points[0].id;
        }
    }

    /** Returns the number of touch points pressed.
     *
     * @return the number of touch points
     */
    int getPointCount() {
        return pointCount;
    }

    /** Removes all touch points from this state. */
    void clear() {
        pointCount = 0;
    }

    /** Clears the cached window. */
    void clearWindow() {
        window = null;
    }

    /** Adds a Point to this state object.
     *
     * @param p the Point describing the data to add, or null if no data is
     *          available yet for this point. p is not modified,
     *          but its contents are copied to the object describing the new
     *          Point.
     * @return the Point with the data for the new touch point. The fields of
     * this Point may be modified directly to change the data for the new
     * touch point.
     */
    Point addPoint(Point p) {
        if (points.length == pointCount) {
            points = Arrays.copyOf(points, points.length * 2);
        }
        if (points[pointCount] == null) {
            points[pointCount] = new Point();
        }
        if (p != null) {
            p.copyTo(points[pointCount]);
        }
        return points[pointCount++];
    }

    /** Removes the point with the given ID
     *
     * @param id The ID of the touch point which is to be removed.
     */
    void removePointForID(int id) {
        for (int i = 0; i < pointCount; i++) {
            if (points[i].id == id) {
                if (i < pointCount - 1) {
                    System.arraycopy(points, i + 1, points, i, pointCount - i - 1);
                    points[pointCount - 1] = null;
                }
                pointCount --;
            }
        }
    }

    /** Replaces the touch point data at the given index with the given touch
     *    point data
     *
     * @param index the index at which to change the touch point data
     * @param p the data to copy to the given index.
     */
    void setPoint(int index, Point p) {
        if (index >= pointCount) {
            throw new IndexOutOfBoundsException();
        }
        p.copyTo(points[index]);
    }

    /** Copies the contents of this state object to another.
     *
     * @param target The TouchState to which to copy this state's data.
     */
    void copyTo(TouchState target) {
        target.clear();
        for (int i = 0; i < pointCount; i++) {
            target.addPoint(points[i]);
        }
        target.primaryID = primaryID;
        target.window = window;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("TouchState[" + pointCount);
        for (int i = 0; i < pointCount; i++) {
            sb.append(",");
            sb.append(points[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Modifies the ordering touch points in this state object so that the
     * points are sorted in increasing order of ID.
     */
    void sortPointsByID() {
        Arrays.sort(points, 0, pointCount, pointIdComparator);
    }

    /** Compare two non-null states whose points are sorted by ID */
    boolean equalsSorted(TouchState ts) {
        if (ts.pointCount == pointCount
                && ts.primaryID == primaryID
                && ts.window == window) {
            for (int i = 0; i < pointCount; i++) {
                Point p1 = ts.points[i];
                Point p2 = points[i];
                if (p1.x != p2.x || p1.y != p2.y || p1.id != p2.id) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /** Finds out whether two non-null states are identical in everything but
     * their touch point coordinates
     *
     * @param ts the TouchState to compare to
     * @param ignoreIDs if true, ignore IDs when comparing points
     */
    boolean canBeFoldedWith(TouchState ts, boolean ignoreIDs) {
        if (ts.pointCount != pointCount) {
            return false;
        }
        if (ignoreIDs) {
            return true;
        }
        for (int i = 0; i < pointCount; i++) {
            if (ts.points[i].id != points[i].id) {
                return false;
            }
        }
        return true;
    }

}

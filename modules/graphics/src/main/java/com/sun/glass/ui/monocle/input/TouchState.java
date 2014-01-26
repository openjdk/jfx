/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.input;

import com.sun.glass.ui.monocle.MonocleWindow;
import com.sun.glass.ui.monocle.MonocleWindowManager;

import java.util.Arrays;
import java.util.Comparator;

public class TouchState {

    public static class Point {
        public int id;
        public int x;
        public int y;
        public void copyTo(Point target) {
            target.id = id;
            target.x = x;
            target.y = y;
        }
        public String toString() {
            return "TouchState.Point[id=" + id + ",x=" + x + ",y="  + y + "]";
        }
    }

    static Comparator<Point> pointIdComparator = new Comparator<Point>() {
        @Override
        public int compare(Point p1, Point p2) {
            return p1.id - p2.id;
        }
    };

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
        if (window == null || recalculateCache) {
            window = fallback;
            if (primaryID >= 0) {
                Point p = getPointForID(primaryID, false);
                if (p != null) {
                    window = (MonocleWindow)
                            MonocleWindowManager.getInstance()
                                    .getWindowForLocation(p.x, p.y);
                }
            }
        }
        return window;
    }

    public Point getPoint(int index) {
        return points[index];
    }

    /** Gets the Point matching the given ID, optionally reinstating the point
     * from the previous touch state.
     * @param id The Point ID to match. A value of -1 matches any Point.
     * @return a matching Point, or a new Point if there was no match and
     * reinstatement was requested; null otherwise
     */
    public Point getPointForID(int id, boolean reinstate) {
        for (int i = 0; i < pointCount; i++) {
            if (id == -1 || points[i].id == id) {
                return points[i];
            }
        }
        if (reinstate) {
            Point p = addPoint(TouchInput.getInstance().getPointForID(id));
            p.id = id;
            return p;
        } else {
            return null;
        }
    }

    int getPrimaryID() {
        return primaryID;
    }

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

    public int getPointCount() {
        return pointCount;
    }

    public void clear() {
        pointCount = 0;
    }

    public Point addPoint(Point p) {
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

    public void removePointForID(int id) {
        for (int i = 0; i < pointCount; i++) {
            if (points[i].id == id) {
                if (i < pointCount - 1) {
                    System.arraycopy(points, i + 1, points, i, pointCount - i - 1);
                }
                pointCount --;
            }
        }
    }

    public void setPoint(int index, Point p) {
        if (index >= pointCount) {
            throw new IndexOutOfBoundsException();
        }
        p.copyTo(points[index]);
    }

    public void copyTo(TouchState target) {
        target.clear();
        for (int i = 0; i < pointCount; i++) {
            target.addPoint(points[i]);
        }
        target.primaryID = primaryID;
        target.window = window;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("TouchState[" + pointCount);
        for (int i = 0; i < pointCount; i++) {
            sb.append(",");
            sb.append(points[i]);
        }
        sb.append("]");
        return sb.toString();
    }

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

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

import java.util.Arrays;

public class TouchState {

    public static class Point {
        public int id;
        public float x;
        public float y;
        public void copyTo(Point target) {
            target.id = id;
            target.x = x;
            target.y = y;
        }
        public String toString() {
            return "TouchState.Point[id=" + id + ",x=" + x + ",y="  + y + "]";
        }
    }

    private Point[] points = new Point[1];
    private int pointCount = 0;

    public Point getPoint(int index) {
        return points[index];
    }

    public int getPointCount() {
        return pointCount;
    }

    public void clear() {
        pointCount = 0;
    }

    public void addPoint(Point p) {
        if (points.length == pointCount) {
            points = Arrays.copyOf(points, points.length * 2);
        }
        if (points[pointCount] == null) {
            points[pointCount] = new Point();
        }
        p.copyTo(points[pointCount]);
        pointCount ++;
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

    void copyTo(TouchState target) {
        if (target.points.length < points.length) {
            target.points = Arrays.copyOf(points, points.length);
        } else {
            System.arraycopy(points, 0, target.points, 0, points.length);
        }
        target.pointCount = pointCount;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("TouchState[" + pointCount + ",");
        for (int i = 0; i < pointCount; i++) {
            sb.append(points[i]);
            if (i < pointCount - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

}

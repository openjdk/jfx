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

package com.sun.glass.ui.monocle.input.devices;

import com.sun.glass.ui.monocle.input.TestApplication;
import com.sun.glass.ui.monocle.input.TestLog;
import javafx.geometry.Rectangle2D;

import java.util.HashMap;
import java.util.Map;

public abstract class TestTouchDevice extends TestDevice {

    protected final int[] transformedXs;
    protected final int[] transformedYs;
    protected final double[] xs;
    protected final double[] ys;
    protected final boolean[] points;
    protected int pressedPoints = 0;
    protected int previousPressedPoints = 0;
    private double absXMax, absYMax;
    private Map<Integer, Integer> ids = new HashMap<>();

    public TestTouchDevice(int maxPointCount) {
        this.transformedXs = new int[maxPointCount];
        this.transformedYs = new int[maxPointCount];
        this.xs = new double[maxPointCount];
        this.ys = new double[maxPointCount];
        this.points = new boolean[maxPointCount];
    }

    public int getPointCount() {
        return points.length;
    }

    protected int getID(int p) {
        if (ids.containsKey(p)) {
            return ids.get(p);
        } else {
            // assign a new ID
            int id = 1;
            while (ids.containsValue(id)) {
                id ++;
            }
            ids.put(p, id);
            return id;
        }
    }

    protected int transformX(double x) {
        if (absXMax == 0.0) {
            return (int) Math.round(x);
        } else {
            Rectangle2D r = TestApplication.getScreenBounds();
            return (int) Math.round(x * absXMax / r.getWidth());
        }
    }

    protected int transformY(double y) {
        if (absXMax == 0.0) {
            return (int) Math.round(y);
        } else {
            Rectangle2D r = TestApplication.getScreenBounds();
            return (int) Math.round(y * absYMax / r.getHeight());
        }
    }

    public int addPoint(double x, double y) {
        int point = -1;
        for (int i = 0; i < points.length; i++) {
            if (!points[i]) {
                point = i;
                break;
            }
        }
        if (point == -1) {
            throw new IllegalStateException("Cannot add any more points");
        }
        TestLog.format("TestTouchDevice: addPoint %d, %.0f, %.0f\n",
                       point, x, y);
        xs[point] = x;
        ys[point] = y;
        transformedXs[point] = transformX(x);
        transformedYs[point] = transformY(y);
        points[point] = true;
        pressedPoints ++;
        return point;
    }

    public void removePoint(int point) {
        TestLog.format("TestTouchDevice: removePoint %d\n", point);
        if (!points[point]) {
            throw new IllegalStateException("Point not pressed");
        }
        points[point] = false;
        pressedPoints --;
        ids.remove(point);
    }

    public void setPoint(int point, double x, double y) {
        TestLog.format("TestTouchDevice: setPoint %d, %.0f, %.0f\n",
                       point, x, y);
        if (!points[point]) {
            throw new IllegalStateException("Point not pressed");
        }
        xs[point] = x;
        ys[point] = y;
        transformedXs[point] = transformX(x);
        transformedYs[point] = transformY(y);
    }

    public void setAndRemovePoint(int point, double x, double y) {
        TestLog.format("TestTouchDevice: setAndRemovePoint %d, %.0f, %.0f\n",
                       point, x, y);
        setPoint(point, x, y);
        removePoint(point);
    }

    @Override
    public void sync() {
        TestLog.log("TestTouchDevice: sync");
        super.sync();
        previousPressedPoints = pressedPoints;
    }

    public void resendStateAndSync() {
        TestLog.log("TestTouchDevice: sync");
        sync();
    }

    protected void setAbsScale(int absXMax, int absYMax) {
        this.absXMax = (double) absXMax;
        this.absYMax = (double) absYMax;
    }

    public int getTapRadius() {
        return TestApplication.getTapRadius();
    }

    public String toString() {
        if (getClass().getPackage().equals(TestTouchDevice.class.getPackage())) {
            return getClass().getSimpleName();
        } else {
            return getClass().getName();
        }
    }

}

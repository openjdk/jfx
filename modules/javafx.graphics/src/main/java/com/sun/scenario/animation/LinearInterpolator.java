/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.animation;

import javafx.animation.Interpolator;
import javafx.geometry.Point2D;
import java.util.Arrays;

/**
 * Implementation of a piecewise linear interpolator as described by
 * <a href="https://www.w3.org/TR/css-easing-2/#the-linear-easing-function">CSS Easing Functions Level 2</a>
 */
public final class LinearInterpolator extends Interpolator {

    // Control points stored as [x0, y0, x1, y1, ...]
    private final double[] controlPoints;

    public LinearInterpolator(Point2D[] controlPoints) {
        if (controlPoints == null) {
            throw new NullPointerException("controlPoints cannot be null");
        }

        if (controlPoints.length == 0) {
            throw new IllegalArgumentException("controlPoints cannot be empty");
        }

        int n = controlPoints.length;
        this.controlPoints = new double[n * 2];

        for (int i = 0; i < n; i++) {
            Point2D p = controlPoints[i];
            this.controlPoints[2 * i] = p.getX();
            this.controlPoints[2 * i + 1] = p.getY();
        }

        canonicalize(this.controlPoints, n);
    }

    private static void canonicalize(double[] controlPoints, int n) {
        // If the first control point has no input progress value, set it to 0.
        if (Double.isNaN(controlPoints[0])) {
            controlPoints[0] = 0.0;
        }

        // If the last control point has no input progress value, set it to 1.
        int lastIdx = 2 * (n - 1);
        if (Double.isNaN(controlPoints[lastIdx])) {
            controlPoints[lastIdx] = 1.0;
        }

        // Ensure that the input progress value of each control point is greater than or equal to the
        // input progress values of all preceding control points (monotonically non-decreasing).
        double largestX = controlPoints[0];
        for (int i = 1; i < n; ++i) {
            double curX = controlPoints[2 * i];
            if (curX < largestX) {
                controlPoints[2 * i] = largestX;
            } else if (!Double.isNaN(curX)) {
                largestX = curX;
            }
        }

        // For all control points without input progress value: determine an appropriate input progress value
        // by equally spacing the control points between neighboring control points.
        for (int i = 1; i < n; ++i) {
            if (!Double.isNaN(controlPoints[2 * i])) {
                continue;
            }

            int j = i;
            while (j < n && Double.isNaN(controlPoints[2 * j])) {
                ++j;
            }

            double x0 = controlPoints[2 * (i - 1)];
            double x1 = controlPoints[2 * j];
            double xStep = (x1 - x0) / (j - i + 1);
            double x = x0;
            for (int k = i; k < j; ++k) {
                x += xStep;
                controlPoints[2 * k] = x;
            }

            i = j;
        }
    }

    @Override
    public double curve(double t) {
        int n = controlPoints.length / 2;

        // Only a single control point: always return its output value.
        if (n == 1) {
            return controlPoints[1];
        }

        // If t matches the x of one or more control points, return the y of the last such point.
        double lastMatchY = Double.NaN;
        for (int i = 0; i < n; ++i) {
            double x = controlPoints[2 * i];
            if (t < x) { // Xs are monotonically non-decreasing; no more matches possible
                break;
            }

            if (t == x) {
                lastMatchY = controlPoints[2 * i + 1];
            }
        }

        if (!Double.isNaN(lastMatchY)) {
            return lastMatchY;
        }

        double xFirst = controlPoints[0];
        double yFirst = controlPoints[1];
        double xLast  = controlPoints[2 * (n - 1)];
        double yLast  = controlPoints[2 * (n - 1) + 1];
        double ax, ay, bx, by;

        // If t is smaller than any x, use the first segment.
        if (t < xFirst) {
            ax = xFirst;
            ay = yFirst;
            bx = controlPoints[2];
            by = controlPoints[3];
        }
        // If t is larger than any x, use the last segment.
        else if (t > xLast) {
            ax = controlPoints[2 * (n - 2)];
            ay = controlPoints[2 * (n - 2) + 1];
            bx = xLast;
            by = yLast;
        }
        // t is between first.x and last.x (and not equal to any x).
        else {
            int indexA = -1; // last point with x < t
            int indexB = -1; // first point with x > t

            for (int i = 0; i < n; i++) {
                double x = controlPoints[2 * i];
                if (x < t) {
                    indexA = i;
                } else if (x > t) {
                    indexB = i;
                    break;
                }
            }

            if (indexA < 0) {
                ax = xFirst;
                ay = yFirst;
            } else {
                ax = controlPoints[2 * indexA];
                ay = controlPoints[2 * indexA + 1];
            }

            if (indexB < 0) {
                bx = xLast;
                by = yLast;
            } else {
                bx = controlPoints[2 * indexB];
                by = controlPoints[2 * indexB + 1];
            }
        }

        double dx = bx - ax;
        double dy = by - ay;

        // Degenerate vertical segment -> step
        if (dx == 0) {
            return ay;
        }

        // Horizontal segment -> constant, avoids 0 * inf NaN
        if (dy == 0) {
            return ay;
        }

        // Linearly interpolate (or extrapolate) along the segment (ax, ay) -> (bx, by).
        double proportion = (t - ax) / dx;
        if (Double.isFinite(proportion)) {
            return ay + dy * proportion;
        }

        // In case we overflow/underflow to infinity, we return the signed infinity consistent with the line.
        double sign =
            Math.signum(dy)       // tells us whether we are extrapolating left or right of A
            * Math.signum(t - ax) // tells us the direction from A to B
            * Math.signum(dx);    // tells us whether the line goes up or down

        return sign >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
    }

    @Override
    public String toString() {
        return "LinearInterpolator " + Arrays.toString(controlPoints);
    }
}

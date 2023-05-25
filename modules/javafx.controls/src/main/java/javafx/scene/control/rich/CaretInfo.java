/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.rich;

import java.util.Objects;
import javafx.scene.control.rich.util.Util;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;

/**
 * Captures a local caret position and bounds in the {@link VFlow} coordinates.
 */
public class CaretInfo {
    private final double xmin;
    private final double xmax;
    private final double ymin;
    private final double ymax;
    private final PathElement[] path;

    private CaretInfo(double xmin, double xmax, double ymin, double ymax, PathElement[] path) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.path = path;
    }

    public static CaretInfo create(double dx, double dy, PathElement[] path) {
        Objects.requireNonNull(path);
        if (path.length == 0) {
            throw new IllegalArgumentException("non-empty path is required");
        }

        double xmin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;

        // also translate full path
        int sz = path.length;
        PathElement[] pe = new PathElement[sz];
        for (int i = 0; i < sz; i++) {
            PathElement em = path[i];
            if (em instanceof LineTo lineto) {
                double x = lineto.getX() + dx;
                double y = lineto.getY() + dy;
                pe[i] = new LineTo(x, y);

                x = Util.halfPixel(x);
                if (x < xmin) {
                    xmin = x;
                } else if (x > xmax) {
                    xmax = x;
                }

                y = Util.halfPixel(y);
                if (y < ymin) {
                    ymin = y;
                } else if (y > ymax) {
                    ymax = y;
                }
            } else if (em instanceof MoveTo moveto) {
                double x = moveto.getX() + dx;
                double y = moveto.getY() + dy;
                pe[i] = new MoveTo(x, y);

                x = Util.halfPixel(x);
                if (x < xmin) {
                    xmin = x;
                } else if (x > xmax) {
                    xmax = x;
                }

                y = Util.halfPixel(y);
                if (y < ymin) {
                    ymin = y;
                } else if (y > ymax) {
                    ymax = y;
                }
            } else {
                throw new IllegalArgumentException("Unexpected PathElement: " + em);
            }
        }

        return new CaretInfo(xmin, xmax, ymin, ymax, pe);
    }

    public final double getMinX() {
        return xmin;
    }

    public final double getMaxX() {
        return xmax;
    }

    public final double getMinY() {
        return ymin;
    }

    public final double getMaxY() {
        return ymax;
    }

    /** returns the caret path in vflow coordinates */
    public final PathElement[] path() {
        return path;
    }

    public final boolean containsY(double y) {
        return (y >= ymin) && (y < ymax);
    }

    public String toString() {
        return "CaretInfo{xmin=" + xmin + ", xmax=" + xmax + ", ymin=" + ymin + ", ymax=" + ymax + "}";
    }
}

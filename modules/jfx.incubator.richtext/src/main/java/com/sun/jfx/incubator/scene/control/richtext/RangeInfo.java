/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.richtext;

import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;

/**
 * Represents the text geometry as a sequence of bounding rectangles.
 */
public class RangeInfo {
    /** the sequence of rectangles encoded as [xmin, ymin, xmax, ymax], ... */
    private final double[] data;
    private final double ymin;
    private final double ymax;

    private RangeInfo(double[] data, double ymin, double ymax) {
        this.data = data;
        this.ymin = ymin;
        this.ymax = ymax;
    }

    public static RangeInfo of(double width, double height) {
        double[] d = { 0.0, 0.0, width, height };
        return new RangeInfo(d, 0.0, height);
    }

    public static RangeInfo of(PathElement[] elements, double lineSpacing) {
        // this code depends on the current implementation (see PrismLayout::getRange)
        // which generates path elements with the following pattern:
        //   result.add(new MoveTo(x + l,  y + top));
        //   result.add(new LineTo(x + r, y + top));
        //   result.add(new LineTo(x + r, y + bottom));
        //   result.add(new LineTo(x + l,  y + bottom));
        //   result.add(new LineTo(x + l,  y + top));
        int sz = (elements.length / 5);
        double[] d = new double[sz * 4];
        int srcIndex = 0;
        int tgtIndex = 0;
        double ymin = Double.POSITIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < sz; i++) {
            // we could do extra checking here, but the hope is that we will create a new API
            // for the caret info and text range which would contain information we need.
            MoveTo m = (MoveTo)elements[srcIndex];
            double x = m.getX();
            double y = m.getY();
            d[tgtIndex++] = x;
            d[tgtIndex++] = y;
            if (y < ymin) {
                ymin = y;
            }
            if (y > ymax) {
                ymax = y;
            }

            LineTo t = (LineTo)elements[srcIndex + 2];
            x = t.getX();
            y = t.getY() + lineSpacing;
            d[tgtIndex++] = x;
            d[tgtIndex++] = y;
            if (y < ymin) {
                ymin = y;
            }
            if (y > ymax) {
                ymax = y;
            }

            srcIndex += 5;
        }
        return new RangeInfo(d, ymin, ymax);
    }

    public int getSegmentCount() {
        return data.length / 4;
    }

    public boolean contains(int ix, double x, double y) {
        ix *= 4;
        if (data[ix++] <= x) {
            if (data[ix++] <= y) {
                if (data[ix++] >= x) {
                    if (data[ix] >= y) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean containsX(int ix, double x) {
        ix *= 4;
        return (data[ix] <= x) && (data[ix + 2] >= x);
    }

    public double midPointY(int ix) {
        ix *= 4;
        return (data[ix + 1] + data[ix + 3]) / 2.0;
    }

    public double getMinY(int ix) {
        return data[ix * 4 + 1];
    }

    public double getMaxY(int ix) {
        return data[ix * 4 + 3];
    }

    public boolean insideY(double y) {
        return (ymin <= y) && (y <= ymax);
    }
}

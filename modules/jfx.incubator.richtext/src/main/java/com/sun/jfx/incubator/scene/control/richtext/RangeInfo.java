/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import javafx.geometry.Rectangle2D;
import javafx.scene.text.LayoutInfo;
import javafx.scene.text.TextLineInfo;

/**
 * Represents the text geometry as a sequence of bounding rectangles
 * in the TextFlow coordinates for the purposes of vertical navigation
 * within the VFlow.
 */
public final class RangeInfo {
    /// contains one or more pairs of {miny, maxy} values, or null
    private final double[] lines;
    private final double ymin;
    private final double ymax;

    private RangeInfo(double[] lines, double ymin, double ymax) {
        this.lines = lines;
        this.ymin = ymin;
        this.ymax = ymax;
    }

    public static RangeInfo of(double height) {
        return new RangeInfo(null, 0.0, height);
    }

    public static RangeInfo of(LayoutInfo la, double lineSpacing, double height) {
        List<TextLineInfo> lines = la.getTextLines(true);
        if (lines.size() == 0) {
            return new RangeInfo(null, 0.0, height);
        }
        Rectangle2D r = la.getLogicalBounds(false);
        double ymin = r.getMinY();
        double ymax = r.getMaxY();
        int sz = lines.size();
        double[] d = new double[sz + sz];
        int dest = 0;
        for (int i = 0; i < sz; i++) {
            r = lines.get(i).bounds();
            d[dest++] = r.getMinY();
            d[dest++] = r.getMaxY();
        }
        // remove line spacing from the last line to force navigating to the next cell
        if (d.length > 0) {
            d[d.length - 1] -= lineSpacing;
        }
        return new RangeInfo(d, ymin, ymax);
    }

    public double getFirstLineMidY() {
        if ((lines != null) && (lines.length > 0)) {
            double min = lines[0];
            double max = lines[1];
            return midPoint(min, max);
        }
        return midPoint(ymin, ymax);
    }

    public double getLastLineMidY() {
        if (lines != null) {
            int ix = lines.length;
            if (ix > 0) {
                double max = lines[--ix];
                double min = lines[--ix];
                return midPoint(min, max);
            }
        }
        return midPoint(ymin, ymax);
    }

    public double findHitMidpoint(double y) {
        if (lines != null) {
            int sz = lines.length;
            if (y < lines[0]) {
                return midPoint(lines[0], lines[1]);
            } else if (y >= lines[sz - 1]) {
                return midPoint(lines[sz - 2], lines[sz - 1]);
            }
            for (int i = 0; i < sz;) {
                double min = lines[i++];
                double max = lines[i++];
                if ((y >= min) && (y < max)) {
                    return midPoint(min, max);
                }
            }
        }
        return midPoint(ymin, ymax);
    }

    private static double midPoint(double min, double max) {
        return (min + max) / 2.0;
    }

    public boolean isOutsideTextRangeY(double y) {
        return (y < ymin) || (y >= ymax);
    }
}

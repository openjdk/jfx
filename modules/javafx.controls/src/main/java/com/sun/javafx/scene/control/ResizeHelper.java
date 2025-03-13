/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import java.util.BitSet;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableColumnBase;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/**
 * Helper class for Tree/TableView constrained column resize policies.
 */
public class ResizeHelper {
    private static final int SMALL_DELTA = 32;
    private static final double EPSILON = 0.000001;
    private final ResizeFeaturesBase rf;
    private final double target;
    private final List<? extends TableColumnBase<?,?>> columns;
    private final int count;
    private final ConstrainedColumnResize.ResizeMode mode;
    private final double[] size;
    private final double[] min;
    private final double[] pref;
    private final double[] max;
    private final BitSet skip;
    private final Region snap;

    public ResizeHelper(ResizeFeaturesBase rf,
                        double target,
                        List<? extends TableColumnBase<?,?>> columns,
                        ConstrainedColumnResize.ResizeMode mode) {
        this.rf = rf;
        this.snap = (rf.getTableControl().isSnapToPixel() ? rf.getTableControl() : null);
        this.columns = columns;
        this.mode = mode;
        this.target = snapRound(target);
        this.count = columns.size();

        size = new double[count];
        min = new double[count];
        pref = new double[count];
        max = new double[count];
        skip = new BitSet(count);

        for (int i = 0; i < count; i++) {
            TableColumnBase<?,?> c = columns.get(i);
            size[i] = snapCeil(c.getWidth());

            if (c.isResizable()) {
                double cmin = snapCeil(c.getMinWidth()); // always honor min width!
                double cmax = snapCeil(c.getMaxWidth());
                min[i] = cmin;
                max[i] = cmax;
                pref[i] = clip(snapCeil(c.getPrefWidth()), cmin, cmax);
                // skip fixed columns
                if (cmin == cmax) {
                    skip.set(i, true);
                }
            } else {
                skip.set(i, true);
            }
        }
    }

    public void resizeToContentWidth() {
        if (skip.cardinality() == count) {
            return;
        }

        // compute delta (snapped)
        // if delta < 0 (and sum(width) < sum(pref)) -> distribute from size to min
        // if delta < 0 (and sum(width) > sum(pref)) -> distribute from size to pref
        // if delta > 0 (and sum(width) < sum(pref)) -> distribute from size to pref
        // else -> distribute from size to max
        //
        double sumWidths = sum(size);
        double sumPrefs = sum(pref);

        double delta = target - sumWidths;
        if (delta < 0.0) {
            // shrink
            if (target < sumPrefs) {
                distribute(delta, min);
            } else {
                distribute(delta, pref);
            }
        } else if (delta > 0.0) {
            // grow
            if (target > sumPrefs) {
                distribute(delta, max);
            } else {
                distribute(delta, pref);
            }
        }
    }

    /** distibuting delta (positive when growing and negative when shrinking) */
    private void distribute(double delta, double[] desired) {
        if (Math.abs(delta) > SMALL_DELTA) {
            distributeLargeDelta(delta, desired);
        } else {
            distributeSmallDelta(delta, desired);
        }
    }

    private void distributeLargeDelta(double delta, double[] desired) {
        boolean grow = delta > 0.0;
        double total = 0.0;
        for (int i = 0; i < count; i++) {
            if (!skip.get(i)) {
                total += avail(grow, desired, i);
            }
        }

        if (isZero(total)) {
            return;
        }

        double unsnapped = 0.0;
        double snapped = 0.0;
        for (int i = 0; i < count; i++) {
            if (!skip.get(i)) {
                double d = delta * avail(grow, desired, i) / total;
                double x = unsnapped + size[i] + d;
                double w = snapRound(x) - snapped;

                size[i] = w;

                unsnapped = x;
                snapped += w;
            }
        }
    }

    private void distributeSmallDelta(double delta) {
        double[] desired = delta < 0 ? min : max;
        distributeSmallDelta(delta, desired);
    }

    /**
     * for small deltas, we use a simpler, but more expensive algorithm to distribute space in small steps,
     * each time favoring a column that is further away from its desired width.
     */
    private void distributeSmallDelta(double delta, double[] desired) {
        double pixel = 1.0 / snapScale();
        double halfPixel = pixel / 2.0;
        if (delta < 0) {
            while (delta < -halfPixel) {
                double d = -pixel;
                if (smallShrink(d, desired)) {
                    return;
                }
                delta -= d;
            }
        } else {
            while (delta > halfPixel) {
                double d = pixel;
                if (smallGrow(d, desired)) {
                    return;
                }
                delta -= d;
            }
        }
    }

    /**
     * Finds the best column to shrink, then reduces its width by delta, which is expected
     * to be one display pixel exactly.
     * @return true if no candidate has been found and the process should stop
     */
    private boolean smallShrink(double delta, double[] desired) {
        double dist = Double.NEGATIVE_INFINITY;
        int ix = -1;
        for (int i = 0; i < count; i++) {
            if (!skip.get(i)) {
                double d = size[i] - desired[i];
                if (d > dist) {
                    dist = d;
                    ix = i;
                }
            }
        }

        if (ix < 0) {
            return true;
        } else {
            size[ix] += delta;
            return false;
        }
    }

    /**
     * Finds the best column to grow, then increases its width by delta, which is expected
     * to be one display pixel exactly.
     * @return true if no candidate has been found and the process should stop
     */
    private boolean smallGrow(double delta, double[] desired) {
        double dist = Double.NEGATIVE_INFINITY;
        int ix = -1;
        for (int i = 0; i < count; i++) {
            if (!skip.get(i)) {
                double d = desired[i] - size[i];
                if (d > dist) {
                    dist = d;
                    ix = i;
                }
            }
        }

        if (ix < 0) {
            return true;
        } else {
            size[ix] += delta;
            return false;
        }
    }

    private double avail(boolean grow, double[] desired, int ix) {
        double d = desired[ix];
        double s = size[ix];
        if (grow) {
            if (d < s) {
                return 0.0;
            }
        } else {
            if (d > s) {
                return 0.0;
            }
        }
        return d - s;
    }

    /**
     * Applies computed column widths to the tree/table columns,
     * snapping coordinates if required.
     *
     * @return true if sum of columns equals or greater than the target width
     */
    public boolean applySizes() {
        double total = 0.0;
        for (int i = 0; i < count; i++) {
            TableColumnBase<?, ?> c = columns.get(i);
            double w = size[i];
            if (c.isResizable()) {
                rf.setColumnWidth(c, w);
            }
            total += c.getWidth();
        }

        return (total > target);
    }

    private static double clip(double v, double min, double max) {
        if (v < min) {
            return min;
        } else if (v > max) {
            return max;
        }
        return v;
    }

    public boolean resizeColumn(TableColumnBase<?, ?> column) {
        double delta = rf.getDelta();
        // need to find the last leaf column of the given column - it is this
        // column that we actually resize from. If this column is a leaf, then we use it.
        TableColumnBase<?, ?> leafColumn = column;
        while (leafColumn.getColumns().size() > 0) {
            leafColumn = leafColumn.getColumns().get(leafColumn.getColumns().size() - 1);
        }

        if (!leafColumn.isResizable()) {
            return false;
        }

        int ix = columns.indexOf(leafColumn);
        boolean expanding = delta > 0.0;
        double allowedDelta = getAllowedDelta(ix, expanding);
        if (isZero(allowedDelta)) {
            return false;
        }

        int ct = markOppositeColumns(ix);
        if (ct == 0) {
            return false;
        }

        double d = computeAllowedDelta(!expanding);
        if (isZero(d)) {
            return false;
        }

        allowedDelta = snapRound(Math.min(Math.abs(delta), Math.min(allowedDelta, d)));
        if (!expanding) {
            allowedDelta = -allowedDelta;
        }

        if (isCornerCase(allowedDelta, ix)) {
            return false;
        }

        return distributeDelta(ix, allowedDelta);
    }

    private boolean isCornerCase(double delta, int ix) {
        boolean isResizingLastColumn = (ix == count - 2);
        if (isResizingLastColumn) {
            if (delta > 0) {
                int i = count - 1;
                if (size[i] <= min[i]) {
                    // last column hit min constraint
                    return true;
                }
            }
        }

        return false;
    }

    /** non-negative */
    private double getAllowedDelta(int ix, boolean expanding) {
        if (expanding) {
            return Math.abs(max[ix] - size[ix]);
        } else {
            return Math.abs(min[ix] - size[ix]);
        }
    }

    /** updates skip bitset with columns that might be resized, and returns the number of the opposite columns */
    private int markOppositeColumns(int ix) {
        switch (mode) {
        case AUTO_RESIZE_NEXT_COLUMN:
            setSkip(0, ix + 1);
            setSkip(ix + 2, columns.size());
            break;
        case AUTO_RESIZE_FLEX_HEAD:
        case AUTO_RESIZE_FLEX_TAIL:
        case AUTO_RESIZE_SUBSEQUENT_COLUMNS:
            setSkip(0, ix + 1);
            break;
        case AUTO_RESIZE_LAST_COLUMN:
            setSkip(0, Math.max(ix + 1, columns.size() - 1));
            break;
        case AUTO_RESIZE_ALL_COLUMNS:
        default:
            setSkip(ix, ix + 1);
            break;
        }

        return count - skip.cardinality();
    }

    /** range set with limit check */
    private void setSkip(int from, int toExclusive) {
        if (from < 0) {
            from = 0;
        } else if (from >= count) {
            return;
        }
        int to = Math.min(count, toExclusive);
        if (from < to) {
            skip.set(from, to);
        }
    }

    /** returns the allowable delta for all of the opposite columns */
    private double computeAllowedDelta(boolean expanding) {
        double delta = 0;
        int i = 0;
        for (;;) {
            i = skip.nextClearBit(i);
            // are we at the end?
            if (i >= count) {
                break;
            }

            if (expanding) {
                delta += (max[i] - size[i]);
            } else {
                delta += (size[i] - min[i]);
            }

            i++;
        }
        return delta;
    }

    private boolean distributeDelta(int ix, double delta) {
        int ct = count - skip.cardinality();
        switch (ct) {
        case 0:
            return false;
        case 1:
            int oppx = skip.nextClearBit(0);
            size[ix] += delta;
            size[oppx] -= delta;
            return true;
        default:
            size[ix] += delta;
            double adj;

            switch (mode) {
            case AUTO_RESIZE_FLEX_HEAD:
                adj = distributeDeltaFlexHead(-delta);
                break;
            case AUTO_RESIZE_FLEX_TAIL:
                adj = distributeDeltaFlexTail(-delta);
                break;
            default:
                distributeDeltaRemainingColumns(-delta);
                adj = 0.0;
                break;
            }

            size[ix] += adj;

            return true;
        }
    }

    private void distributeDeltaRemainingColumns(double delta) {
        double[] desired = delta < 0 ? min : max;
        distribute(delta, desired);
    }

    private double distributeDeltaFlexHead(double delta) {
        if (delta < 0) {
            // when shrinking, first resize columns that are wider than their preferred width
            for (int i = 0; i < count; i++) {
                if (skip.get(i)) {
                    continue;
                }

                if (size[i] > pref[i]) {
                    delta = resize(i, delta);
                    if (isZero(delta)) {
                        break;
                    }
                }
            }
        } else {
            // when expanding, first resize columns that are narrower than their preferred width
            for (int i = 0; i < count; i++) {
                if (skip.get(i)) {
                    continue;
                }

                if (size[i] < pref[i]) {
                    delta = resize(i, delta);
                    if (isZero(delta)) {
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < count; i++) {
            if (skip.get(i)) {
                continue;
            }

            delta = resize(i, delta);
            if (isZero(delta)) {
                break;
            }
        }
        return delta;
    }

    private double distributeDeltaFlexTail(double delta) {
        if (delta < 0) {
            // when shrinking, first resize columns that are wider than their preferred width
            for (int i = count - 1; i >= 0; --i) {
                if (skip.get(i)) {
                    continue;
                }

                if (size[i] > pref[i]) {
                    delta = resize(i, delta);
                    if (isZero(delta)) {
                        break;
                    }
                }
            }
        } else {
            // when expanding, first resize columns that are narrower than their preferred width
            for (int i = count - 1; i >= 0; --i) {
                if (skip.get(i)) {
                    continue;
                }

                if (size[i] < pref[i]) {
                    delta = resize(i, delta);
                    if (isZero(delta)) {
                        break;
                    }
                }
            }
        }

        for (int i = count - 1; i >= 0; --i) {
            if (skip.get(i)) {
                continue;
            }

            delta = resize(i, delta);
            if (isZero(delta)) {
                break;
            }
        }
        return delta;
    }

    private double resize(int ix, double delta) {
        double w = size[ix] + delta;
        if (w < min[ix]) {
            delta = (w - min[ix]);
            w = min[ix];
        } else if (w > max[ix]) {
            delta = (w - max[ix]);
            w = max[ix];
        } else {
            delta = 0.0;
        }

        size[ix] = w;
        return delta;
    }

    private double sumSizes() {
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += size[i];
        }
        return sum;
    }

    private double sum(double[] widths) {
        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += widths[i];
        }
        return sum;
    }

    private static boolean isZero(double x) {
        return Math.abs(x) < EPSILON;
    }

    private double snapCeil(double x) {
        if (snap != null) {
            return snap.snapSizeX(x);
        }
        return x;
    }

    private double snapRound(double x) {
        if (snap != null) {
            return snap.snapPositionX(x);
        }
        return x;
    }

    /**
     * implementation copied from {@link Region}.
     * @return returns scene render scale x value
     */
    private double snapScale() {
        if (snap != null) {
            Scene scene = snap.getScene();
            if (scene != null) {
                Window window = scene.getWindow();
                if (window != null) {
                    return window.getRenderScaleX();
                }
            }
        }
        return 1.0;
    }
}

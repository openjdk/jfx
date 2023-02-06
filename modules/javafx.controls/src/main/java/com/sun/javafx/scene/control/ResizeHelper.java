/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableColumnBase;
import javafx.scene.layout.Region;

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
        this.target = snap(target);
        this.count = columns.size();

        size = new double[count];
        min = new double[count];
        pref = new double[count];
        max = new double[count];
        skip = new BitSet(count);

        for (int i = 0; i < count; i++) {
            TableColumnBase<?,?> c = columns.get(i);
            size[i] = c.getWidth();

            if (c.isResizable()) {
                double cmin = c.getMinWidth();
                double cmax = c.getMaxWidth();
                min[i] = cmin;
                max[i] = cmax;
                pref[i] = clip(c.getPrefWidth(), cmin, cmax);
            } else {
                skip.set(i, true);
            }
        }
    }

    public void resizeToContentWidth() {
        boolean needsAnotherPass;
        do {
            needsAnotherPass = false;
            double sumWidths = 0.0;
            double sumMins = 0;
            for (int i = 0; i < count; i++) {
                sumWidths += size[i];
                sumMins += min[i];
            }

            if (sumMins >= target) {
                return;
            }

            double delta = target - snap(sumWidths);
            if (isZero(delta)) {
                return;
            }

            // remove fixed and skipped columns from consideration
            double total = 0;
            for (int i = 0; i < count; i++) {
                if (!skip.get(i)) {
                    total += pref[i];
                }
            }

            if (isZero(total)) {
                return;
            }

            if (Math.abs(delta) < SMALL_DELTA) {
                distributeSmallDelta(delta);
                return;
            }

            for (int i = 0; i < count; i++) {
                if (skip.get(i)) {
                    continue;
                }

                double w = size[i] + (delta * pref[i] / total);
                if (w < min[i]) {
                    w = min[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                } else if (w > max[i]) {
                    w = max[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                }

                size[i] = w;

                if (needsAnotherPass) {
                    resetSizeChanges();
                    break;
                }
            }
        } while (needsAnotherPass);
    }

    /**
     * Applies computed column widths to the tree/table columns,
     * snapping coordinates if required.
     *
     * @return true if sum of columns equals or greater than the target width
     */
    public boolean applySizes() {
        double pos = 0.0;
        double prev = 0.0;

        for (int i = 0; i < count; i++) {
            TableColumnBase<?, ?> c = columns.get(i);
            if (c.isResizable()) {
                pos = snap(pos + size[i]);
                double w = (pos - prev);
                rf.setColumnWidth(c, w);
            } else {
                pos = pos + size[i];
            }
            prev = pos;
        }

        return (pos > target);
    }

    protected static double clip(double v, double min, double max) {
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

        allowedDelta = Math.min(Math.abs(delta), Math.min(allowedDelta, d));
        if (!expanding) {
            allowedDelta = -allowedDelta;
        }

        if (isCornerCase(allowedDelta, ix)) {
            return false;
        }

        return distributeDelta(ix, allowedDelta);
    }

    protected boolean isCornerCase(double delta, int ix) {
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
    protected double getAllowedDelta(int ix, boolean expanding) {
        if (expanding) {
            return Math.abs(max[ix] - size[ix]);
        } else {
            return Math.abs(min[ix] - size[ix]);
        }
    }

    /** updates skip bitset with columns that might be resized, and returns the number of the opposite columns */
    protected int markOppositeColumns(int ix) {
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
    protected void setSkip(int from, int toExclusive) {
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
    protected double computeAllowedDelta(boolean expanding) {
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

    protected boolean distributeDelta(int ix, double delta) {
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
                if (Math.abs(delta) < SMALL_DELTA) {
                    distributeSmallDelta(-delta);
                } else {
                    distributeDeltaRemainingColumns(-delta);
                }
                adj = 0.0;
                break;
            }

            size[ix] += adj;

            return true;
        }
    }

    protected double distributeDeltaFlexHead(double delta) {
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

    protected double distributeDeltaFlexTail(double delta) {
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

    protected double resize(int ix, double delta) {
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

    protected void distributeDeltaRemainingColumns(double delta) {
        boolean needsAnotherPass;

        do {
            double total = 0;
            for (int i = 0; i < count; i++) {
                if (!skip.get(i)) {
                    total += pref[i];
                }
            }

            if (isZero(total)) {
                return;
            }

            needsAnotherPass = false;

            for (int i = 0; i < count; i++) {
                if (skip.get(i)) {
                    continue;
                }

                double w = size[i] + (delta * pref[i] / total);
                if (w < min[i]) {
                    w = min[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                    delta -= (w - size[i]);
               } else if (w > max[i]) {
                    w = max[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                    delta -= (w - size[i]);
                }

                size[i] = w;

                if (needsAnotherPass) {
                    resetSizeChanges();
                    break;
                }
            }
        } while (needsAnotherPass);
    }

    /**
     * for small deltas, we use a simpler, but more expensive algorithm to distribute space in small steps,
     * each time favoring a column that is further away from its preferred width.
     */
    protected void distributeSmallDelta(double delta) {
        if (delta < 0) {
            while (delta < 0.0) {
                double d = Math.max(-1.0, delta);
                double rem = shrinkSmall(d);
                if (Double.isNaN(rem)) {
                    return;
                }

                delta -= (d - rem);
            }
        } else {
            while (delta > 0.0) {
                double d = Math.min(1.0, delta);
                double rem = expandSmall(d);
                if (Double.isNaN(rem)) {
                    return;
                }

                delta -= (d - rem);
            }
        }
    }

    /**
     * Finds the best column to shrink, then reduces its width.
     * Adds the column to skip list if the column width hits a constraint after adjustement.
     * @return unused portion of delta, or Double.NaN if it did not find a good candidate
     */
    protected double shrinkSmall(double delta) {
        double dist = Double.NEGATIVE_INFINITY;
        int ix = -1;
        for (int i = 0; i < count; i++) {
            if (!skip.get(i)) {
                double d = size[i] - pref[i];
                if (d > dist) {
                    dist = d;
                    ix = i;
                }
            }
        }

        if (ix < 0) {
            return Double.NaN;
        }

        double rem = 0.0;
        double w = size[ix] + delta;
        if (w < min[ix]) {
            rem = (w - min[ix]);
            w = min[ix];
            skip.set(ix);
        }
        size[ix] = w;
        return rem;
    }

    /**
     * Finds the best column to shrink, then reduces its width.
     * Adds the column to skip list if the column width hits a constraint after adjustement.
     * @return unused portion of delta, or Double.NaN if it did not find a good candidate
     */
    protected double expandSmall(double delta) {
        double dist = Double.NEGATIVE_INFINITY;
        int ix = -1;
        for (int i = 0; i < count; i++) {
            if (!skip.get(i)) {
                double d = pref[i] - size[i];
                if (d > dist) {
                    dist = d;
                    ix = i;
                }
            }
        }

        if (ix < 0) {
            return Double.NaN;
        }

        double rem = 0.0;
        double w = size[ix] + delta;
        if (w > max[ix]) {
            rem = (w - max[ix]);
            w = max[ix];
            skip.set(ix);
        }
        size[ix] = w;
        return rem;
    }

    protected void resetSizeChanges() {
        for (int i = 0; i < count; i++) {
            if (!skip.get(i)) {
                size[i] = columns.get(i).getWidth();
            }
        }
    }

    protected double sumSizes() {
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += size[i];
        }
        return sum;
    }

    protected static boolean isZero(double x) {
        return Math.abs(x) < EPSILON;
    }

    protected double snap(double x) {
        if (snap == null) {
            return x;
        }
        return snap.snapSpaceX(x);
    }
}

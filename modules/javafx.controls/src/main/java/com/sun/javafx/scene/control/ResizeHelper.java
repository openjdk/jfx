/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Helps resize Tree/TableView columns.
 */
public class ResizeHelper {
    protected static final double EPSILON = 0.0000001;

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

    public ResizeHelper(ResizeFeaturesBase rf,
                        double target,
                        List<? extends TableColumnBase<?,?>> columns,
                        ConstrainedColumnResize.ResizeMode mode) {
        this.rf = rf;
        this.target = target;
        this.columns = columns;
        this.mode = mode;

        this.count = columns.size();
        size = new double[count];
        min = new double[count];
        pref = new double[count];
        max = new double[count];
        skip = new BitSet(count);

        for (int i = 0; i < count; i++) {
            TableColumnBase<?,?> c = columns.get(i);
            double w = c.getWidth();
            size[i] = w;

            if (c.isResizable()) {
                min[i] = c.getMinWidth();
                max[i] = c.getMaxWidth();
                // TODO use integers or round
                pref[i] = clip(c.getPrefWidth(), c.getMinWidth(), c.getMaxWidth());
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
            double sumMins = 0.0;
            for (int i = 0; i < count; i++) {
                sumWidths += size[i];
                sumMins += min[i];
            }

            if(sumMins >= target) {
                return;
            }

            double delta = target - sumWidths;
            if (isZero(delta)) {
                return;
            }

            // remove fixed and skipped columns from consideration
            double total = 0.0;
            for (int i = 0; i < count; i++) {
                if (!skip.get(i)) {
                    total += step1(i);
                }
            }

            if (isZero(total)) {
                return;
            }

            double acc = 0.0; // accumulating widths of processed columns
            double rem = 0.0; // remainder from previous column

            for (int i = 0; i < count; i++) {
                if (skip.get(i)) {
                    continue;
                }

                double dw = rem + (delta * step1(i) / total);
                double w = Math.round(size[i] + dw);
                if (w < min[i]) {
                    rem = (w - min[i]);
                    w = min[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                } else if (w > max[i]) {
                    rem = (w - max[i]);
                    w = max[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                } else {
                    rem = dw - (w - size[i]);
                }

                acc += w;
                size[i] = w;
            }

            if (isZero(target - acc)) {
                needsAnotherPass = false;
            }

        } while (needsAnotherPass);
    }

    protected double step1(int ix) {
        double w = pref[ix] - size[ix];
        if(w <= 0) {
            return size[ix];
        } else {
            return pref[ix];
        }
    }

    /**
     * Applies computed column widths to the tree/table columns.
     * @return true if sum of columns equals or greater than the target area
     */
    public boolean applySizes() {
        double w = 0.0;
        for (int i = 0; i < count; i++) {
            TableColumnBase<?,?> c = columns.get(i);
            if (c.isResizable()) {
                rf.setColumnWidth(c, size[i]);
                w += size[i];
            }
        }

        return (w > (target - 1.0));
    }

    protected static boolean isZero(double x) {
        return Math.abs(x) < EPSILON;
    }

    protected static double clip(double v, double min, double max) {
        if (v < min) {
            return min;
        } else if (v > max) {
            return max;
        }
        return v;
    }

    public boolean resizeColumn(TableColumnBase<?,?> column) {
        double delta = rf.getDelta();
        // need to find the last leaf column of the given column - it is this
        // column that we actually resize from. If this column is a leaf, then we
        // use it.
        TableColumnBase<?,?> leafColumn = column;
        while (leafColumn.getColumns().size() > 0) {
            leafColumn = leafColumn.getColumns().get(leafColumn.getColumns().size() - 1);
        }

        if (!leafColumn.isResizable()) {
            return false;
        }

        int ix = columns.indexOf(leafColumn);
        boolean expanding = delta > 0.0;
        double allowedDelta;
        if (mode == ConstrainedColumnResize.ResizeMode.AUTO_RESIZE_ALL_COLUMNS) {
            allowedDelta = Math.abs(delta);
        } else {
            allowedDelta = getAllowedDelta(ix, expanding);
            if (isZero(allowedDelta)) {
                return false;
            }
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
        allowedDelta = (expanding ? 1 : -1) * Math.floor(allowedDelta);

        if (isCornerCase(allowedDelta, ix)) {
            return false;
        }

        return distributeDelta(ix, allowedDelta);
    }

    protected boolean isCornerCase(double delta, int ix) {
        boolean isResizingLastColumn = (ix == count - 2);
        if (isResizingLastColumn) {
            if (delta > 0.0) {
                int i = count - 1;
                if (isZero(size[i] - min[i])) {
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
        double delta = 0.0;
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
        if (ct == 0) {
            return false;
        } else if (ct == 1) {
            int oppx = skip.nextClearBit(0);
            size[ix] += delta;
            size[oppx] -= delta;
            return true;
        } else {
            double w1 = sumSizes(); // FIX
            size[ix] += delta;
            double adj;
            switch(mode) {
            case AUTO_RESIZE_FLEX_HEAD:
                adj = distributeDeltaFlexHead(-delta);
                break;
            case AUTO_RESIZE_FLEX_TAIL:
                adj = distributeDeltaFlexTail(-delta);
                break;
            default:
                distributeDeltaRemainingColumns(-delta);
                adj = 0.0;

                double w2 = sumSizes(); // FIX
                if(w1 != w2) {
                    System.err.println("ERR 2 sum sizes before="  + w1 + " after=" + w2 + " adj=" + adj + " delta=" + delta);
//                    adj = w1-w2;
                }

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

    protected double resize(int i, double delta) {
        double w = Math.round(size[i] + delta);
        if (w < min[i]) {
            delta = (w - min[i]);
            w = min[i];
        } else if (w > max[i]) {
            delta = (w - max[i]);
            w = max[i];
        } else {
            delta = 0.0;
        }

        size[i] = w;
        return delta;
    }

    protected void distributeDeltaRemainingColumns(double delta) {
        System.err.println("distributeDeltaRemainingColumns delta=" + delta);

        boolean needsAnotherPass = false;
        do {
            double total = 0.0;
            for (int i = 0; i < count; i++) {
                if (!skip.get(i)) {
                    total += pref[i];
                }
            }

            if (isZero(total)) {
                System.err.println("zero total");
                return;
            }

            double rem = 0.0; // remainder from the previous column
            needsAnotherPass = false;

            for (int i = 0; i < count; i++) {
                if (skip.get(i)) {
                    continue;
                }

                double dw = rem + (delta * pref[i] / total);
                double w = Math.round(size[i] + dw);
                if (w < min[i]) {
                    rem = (w - min[i]);
                    w = min[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                    double ch = (w - size[i]);
                    if(ch != 0) {
                        System.err.println("min ch=" + ch);
                    }
                    delta -= ch;
               } else if (w > max[i]) {
                    rem = (w - max[i]);
                    w = max[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                    double ch = (w - size[i]);
                    if(ch != 0) {
                        System.err.println("max ch=" + ch);
                    }
                    delta -= ch;
                } else {
                    rem = dw - (w - size[i]);
                }

                size[i] = w;

                if(needsAnotherPass) {
                    System.err.println("needsAnotherPass"); // FIX
                    resetSizeChanges();
                    break;
                }
            }

//            if (Math.abs(delta) < 1.0) {
//                if (Math.abs(delta) >= 0.5) {
//                    adj = Math.signum(delta);
//                }
//                needsAnotherPass = false;
//            }
        } while(needsAnotherPass);
    }

    private void resetSizeChanges() {
        // reset size changes
        for (int i = 0; i < count; i++) {
            if (!skip.get(i)) {
                size[i] = columns.get(i).getWidth();
            }
        }
    }

    private double sumSizes() {
        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += size[i];
        }
        return sum;
    }
}

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
package javafx.scene.control;

import java.text.DecimalFormat;
import java.util.BitSet;
import java.util.List;
import javafx.scene.control.ConstrainedColumnResize.ResizeMode;

/**
 * Helps resize Tree/TableView columns.
 */
public class ResizeHelper {
    protected static final double EPSILON = 0.0000001;

    private final ResizeFeaturesBase rf;
    private final double target;
    private final List<? extends TableColumnBase<?,?>> columns;
    private final ResizeMode mode;
    private final double[] size;
    private final double[] min;
    private final double[] pref;
    private final double[] max;
    private final BitSet skip;

    public ResizeHelper(ResizeFeaturesBase rf,
                        double target,
                        List<? extends TableColumnBase<?,?>> columns,
                        ResizeMode mode) {
        this.rf = rf;
        this.target = target;
        this.columns = columns;
        this.mode = mode;

        int sz = columns.size();
        size = new double[sz];
        min = new double[sz];
        pref = new double[sz];
        max = new double[sz];
        skip = new BitSet(sz);

        for (int i = 0; i < sz; i++) {
            TableColumnBase<?,?> c = columns.get(i);
            double w = c.getWidth();
            size[i] = w;

            if (c.isResizable()) {
                min[i] = c.getMinWidth();
                pref[i] = clip(c.getPrefWidth(), c.getMinWidth(), c.getMaxWidth());
                max[i] = c.getMaxWidth();
            } else {
                skip.set(i, true);
            }
        }
    }

    public void resizeToContentWidth() {
        boolean needsAnotherPass = false;

        do {
            double sumWidths = 0.0;
            for (double x: size) {
                sumWidths += x;
            }

            double delta = target - sumWidths;
            if (isZero(delta)) {
                return;
            }

            // remove fixed and skipped columns from consideration
            double total = 0.0;
            for (int i = 0; i < count(); i++) {
                if (!skip.get(i)) {
                    total += pref[i];
                }
            }

            if (isZero(total)) {
                return;
            }

            for (int i = 0; i < count(); i++) {
                if (skip.get(i)) {
                    continue;
                }

                double dw = delta * pref[i] / total;
                double w = Math.round(size[i] + dw);
                if (w < min[i]) {
                    dw -= (w - min[i]);
                    w = min[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                } else if (w > max[i]) {
                    dw -= (w - max[i]);
                    w = max[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                } else {
                    dw = (w - size[i]);
                }

                delta -= dw;
                total -= pref[i];
                size[i] = w;
            }

            if (Math.abs(delta) < 0.5) {
                needsAnotherPass = false;
            }

            if(needsAnotherPass) System.out.println("*** another pass"); // FIX
        } while(needsAnotherPass);

        check();
    }

    public void applySizes() {
        for (int i = 0; i < count(); i++) {
            TableColumnBase<?,?> c = columns.get(i);
            if (c.isResizable()) {
                rf.setColumnWidth(c, size[i]);
            }
        }
    }

    public int count() {
        return columns.size();
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

    @Override
    public String toString() {
        return
        //            "sumMin=" + p(sumMin) +
        //            " sumPref=" + p(sumPref) +
        //            " sumMax=" + p(sumMax) +
        " target=" + p(target);
    }

    protected static String p(double x) { // FIX remove
        return new DecimalFormat("0.#").format(x);
    }

    public String dump() {
        double sum = 0.0;
        for(double x: size) {
            sum += x;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("target=");
        sb.append(p(target));
        sb.append(" [");
        for(int i=0; i<count(); i++) {
            if(i > 0) {
                sb.append(",");
            }
            sb.append(p(size[i]));
        }
        sb.append("]");
        sb.append(" sum=");
        sb.append(sum);
        return sb.toString();
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
        // FIX don't need in case of multiple columns?
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
        allowedDelta = (expanding ? 1 : -1) * Math.floor(allowedDelta); // TODO use original value, round in ct==1 case

        if (isCornerCase(allowedDelta, ix)) {
            return false;
        }

        return distributeDelta(ix, allowedDelta);
    }

    protected boolean isCornerCase(double delta, int ix) {
        boolean isResizingLastColumn = (ix == count() - 2);
        if (isResizingLastColumn) {
            if (delta > 0.0) {
                int i = count() - 1;
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

        return count() - skip.cardinality();
    }

    /** range set with limit check */
    protected void setSkip(int from, int toExclusive) {
        int sz = count();
        if (from < 0) {
            from = 0;
        } else if (from >= sz) {
            return;
        }
        int to = Math.min(sz, toExclusive);
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
            if (i >= count()) {
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
        int ct = count() - skip.cardinality();
        if (ct == 0) {
            return false;
        } else if (ct == 1) {
            int oppx = skip.nextClearBit(0);
            size[ix] += delta;
            size[oppx] -= delta;
            return true;
        } else {
            size[ix] += delta;
            distributeDeltaMultipleColumns(-delta);
            return true;
        }
    }

    protected void distributeDeltaMultipleColumns(double delta) {
        boolean needsAnotherPass = false;

        do {
            double total = 0.0;
            for (int i = 0; i < count(); i++) {
                if (!skip.get(i)) {
                    total += size[i];
                }
            }

            if (isZero(total)) {
                return;
            }

            for (int i = 0; i < count(); i++) {
                if (skip.get(i)) {
                    continue;
                }

                double dw = delta * size[i] / total;
                double w = Math.round(size[i] + dw);
                if (w < min[i]) {
                    dw -= (w - min[i]);
                    w = min[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                } else if (w > max[i]) {
                    dw -= (w - max[i]);
                    w = max[i];
                    skip.set(i, true);
                    needsAnotherPass = true;
                } else {
                    dw = (w - size[i]);
                }

                delta -= dw;
                total -= size[i];
                size[i] = w;
            }

            if (Math.abs(delta) < 0.5) {
                needsAnotherPass = false;
            }

            if (needsAnotherPass) System.out.println("*** another pass (delta=" + delta + ")"); // FIX

        } while (needsAnotherPass);

        check();
    }

    @Deprecated // FIX
    protected void check() {
        double total = 0.0;
        for (int i = 0; i < count(); i++) {
            total += size[i];
        }

        if (!isZero(total - target)) {
            System.out.println("  FAILED check total=" + total + " target=" + target); // FIX
        }
    }
}

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;

/**
 * Constrained columns resize algorithm which:
 * - honors minimal, preferred, and maximum widths
 * - unconditionally suppresses the horizontal scroll bar
 * 
 * @since 20
 */
public class ConstrainedColumnResize {
    protected static final double EPSILON = 0.0000001;
    private boolean firstRun = true;
    
    public ConstrainedColumnResize() {
    }

    public static TablePolicy forTable() {
        return new TablePolicy();
    }

    public static TreeTablePolicy forTreeTable() {
        return new TreeTablePolicy();
    }

    public static class TablePolicy
        extends ConstrainedColumnResize
        implements Callback<TableView.ResizeFeatures,Boolean> {

        @Override
        public Boolean call(TableView.ResizeFeatures f) {
            List<? extends TableColumnBase<?,?>> visibleLeafColumns = f.getTable().getVisibleLeafColumns();
            return constrainedResize(f, f.getContentWidth(), visibleLeafColumns);
        }
        
        @Override
        public String toString() {
            return "new-constrained-resize";
        }
    }
    
    public static class TreeTablePolicy
        extends ConstrainedColumnResize
        implements Callback<TreeTableView.ResizeFeatures,Boolean> {
        
        @Override
        public Boolean call(TreeTableView.ResizeFeatures f) {
            List<? extends TableColumnBase<?,?>> visibleLeafColumns = f.getTable().getVisibleLeafColumns();
            return constrainedResize(f, f.getContentWidth(), visibleLeafColumns);
        }
        
        @Override
        public String toString() {
            return "new-constrained-resize";
        }
    }

    /**
     * The constrained resize algorithm used by TableView and TreeTableView.
     * TODO meaning of return?
     */
    public boolean constrainedResize(ResizeFeaturesBase rf,
                                     double contentWidth,
                                     List<? extends TableColumnBase<?,?>> visibleLeafColumns) {
        boolean result = constrainedResize(rf, firstRun, rf.getContentWidth(), visibleLeafColumns);
        firstRun = !firstRun ? false : !result;
        return result;
    }

    protected boolean constrainedResize(ResizeFeaturesBase rf,
                                        boolean isFirstRun,
                                        double tableWidth,
                                        List<? extends TableColumnBase<?,?>> visibleLeafColumns) {
        /*
         * There are two phases to the constrained resize policy:
         *   1) Ensuring internal consistency (i.e. table width == sum of all visible
         *      columns width). This is often called when the table is resized.
         *   2) Resizing the given column by __up to__ the given delta.
         *
         * It is possible that phase 1 occur and there be no need for phase 2 to
         * occur.
         */

        if (tableWidth == 0.0) { 
            return false;
        }
        
        // FIX
        isFirstRun = false;

        /*
         * PHASE 1: Check to ensure we have internal consistency. Based on the
         *          Swing JTable implementation.
         */
        
        // determine the width of all visible columns, and their preferred width
        double colWidth = 0.0;
        for (TableColumnBase<?,?> col: visibleLeafColumns) {
            colWidth += col.getWidth();
        }
        
        boolean isShrinking;

        if (Math.abs(colWidth - tableWidth) > EPSILON) {
            // if we are here we have an inconsistency - these two values should be
            // equal when this resizing policy is being used.
            isShrinking = colWidth > tableWidth;
            double target = tableWidth;

            if (isFirstRun) {
                double totalLowerBound = 0.0;
                double totalUpperBound = 0.0;

                for (TableColumnBase<?,?> col: visibleLeafColumns) {
                    totalLowerBound += col.getMinWidth();
                    totalUpperBound += col.getMaxWidth();
                }

                // We run into trouble if the numbers are set to infinity later on
                // FIX NEGATIVE_INFINITY??
                totalUpperBound = (totalUpperBound == Double.POSITIVE_INFINITY) ? 
                    Double.MAX_VALUE :
                    (totalUpperBound == Double.NEGATIVE_INFINITY ? Double.MIN_VALUE : totalUpperBound);

                for (TableColumnBase col: visibleLeafColumns) {
                    // TODO pref width
                    double min = col.getMinWidth();
                    double max = col.getMaxWidth();

                    // Check for zero. This happens when the distribution of the delta
                    // finishes early due to a series of "fixed" entries at the end.
                    // In this case, lowerBound == upperBound, for all subsequent terms.
                    double newSize;
                    if (Math.abs(totalLowerBound - totalUpperBound) < EPSILON) {
                        newSize = min;
                    } else {
                        // TODO use pref here?
                        double f = (target - totalLowerBound) / (totalUpperBound - totalLowerBound);
                        newSize = Math.round(min + f * (max - min));
                    }

                    double remainder = resize(rf, col, newSize - col.getWidth());

                    target -= newSize + remainder;
                    totalLowerBound -= min;
                    totalUpperBound -= max;
                }

                isFirstRun = false; // TODO why?  will never execute this line again FIX remove
            } else {
                double actualDelta = tableWidth - colWidth;
                List<? extends TableColumnBase<?,?>> cols = visibleLeafColumns;
                resizeColumns(rf, cols, actualDelta);
            }
        }

        // At this point we can be happy in the knowledge that we have internal
        // consistency, i.e. table width == sum of the width of all visible
        // leaf columns.

        /*
         * Column may be null if we just changed the resize policy, and we
         * just wanted to enforce internal consistency, as mentioned above.
         */
        TableColumnBase<?,?> column = rf.getColumn();
        if (column == null) {
            return false;
        }

        /*
         * PHASE 2: Handling actual column resizing (by the user). Based on my own
         *          implementation (based on the UX spec).
         */

        double delta = rf.getDelta();
        isShrinking = delta < 0;

        // need to find the last leaf column of the given column - it is this
        // column that we actually resize from. If this column is a leaf, then we
        // use it.
        TableColumnBase<?,?> leafColumn = column;
        while (leafColumn.getColumns().size() > 0) {
            leafColumn = leafColumn.getColumns().get(leafColumn.getColumns().size() - 1);
        }

        int colPos = visibleLeafColumns.indexOf(leafColumn);
        int endColPos = visibleLeafColumns.size() - 1;

        double remainingDelta = delta;
        while (endColPos > colPos && remainingDelta != 0) {
            TableColumnBase<?,?> resizingCol = visibleLeafColumns.get(endColPos);
            endColPos--;

            // if the column width is fixed, break out and try the next column
            if (!resizingCol.isResizable()) {
                continue;
            }

            // for convenience we discern between the shrinking and growing columns
            TableColumnBase<?,?> shrinkingCol = isShrinking ? leafColumn : resizingCol;
            TableColumnBase<?,?> growingCol = !isShrinking ? leafColumn : resizingCol;

            if (growingCol.getWidth() > growingCol.getPrefWidth()) {
                // growingCol is willing to be generous in this case - it goes
                // off to find a potentially better candidate to grow
                List<? extends TableColumnBase> seq = visibleLeafColumns.subList(colPos + 1, endColPos + 1);
                for (int i = seq.size() - 1; i >= 0; i--) {
                    TableColumnBase<?,?> c = seq.get(i);
                    if (c.getWidth() < c.getPrefWidth()) {
                        growingCol = c;
                        break;
                    }
                }
            }

            double sdiff = Math.min(Math.abs(remainingDelta), shrinkingCol.getWidth() - shrinkingCol.getMinWidth());
            double delta1 = resize(rf, shrinkingCol, -sdiff);
            double delta2 = resize(rf, growingCol, sdiff);
            remainingDelta += isShrinking ? sdiff : -sdiff;
        }
        // TODO EPSILON?
        return remainingDelta == 0;
    }

    // function used to actually perform the resizing of the given column,
    // whilst ensuring it stays within the min and max bounds set on the column.
    // Returns the remaining delta if it could not all be applied.
    protected double resize(ResizeFeaturesBase rf, TableColumnBase column, double delta) {
        if (delta == 0) {
            return 0.0;
        }
        if (!column.isResizable()) {
            return delta;
        }

        final boolean isShrinking = delta < 0;
        final List<TableColumnBase<?,?>> resizingChildren = getResizableChildren(column, isShrinking);

        if (resizingChildren.size() > 0) {
            return resizeColumns(rf, resizingChildren, delta);
        } else {
            double newWidth = column.getWidth() + delta;
            if (newWidth > column.getMaxWidth()) {
                rf.setColumnWidth(column, column.getMaxWidth());
                return newWidth - column.getMaxWidth();
            } else if (newWidth < column.getMinWidth()) {
                rf.setColumnWidth(column, column.getMinWidth());
                return newWidth - column.getMinWidth();
            } else {
                rf.setColumnWidth(column, newWidth);
                return 0.0F;
            }
        }
    }

    // Returns all children columns of the given column that are able to be
    // resized. This is based on whether they are visible, resizable, and have
    // not space before they hit the min / max values.
    protected List<TableColumnBase<?,?>> getResizableChildren(TableColumnBase<?,?> column, boolean isShrinking) {
        if (column == null || column.getColumns().isEmpty()) {
            return Collections.emptyList();
        }

        List<TableColumnBase<?,?>> tablecolumns = new ArrayList<TableColumnBase<?,?>>();
        for (TableColumnBase c: column.getColumns()) {
            if (!c.isVisible()) {
                continue;
            }
            if (!c.isResizable()) {
                continue;
            }

            if (isShrinking && c.getWidth() > c.getMinWidth()) {
                tablecolumns.add(c);
            } else if (!isShrinking && c.getWidth() < c.getMaxWidth()) {
                tablecolumns.add(c);
            }
        }
        return tablecolumns;
    }

    protected double resizeColumns(ResizeFeaturesBase rf, List<? extends TableColumnBase<?,?>> columns, double delta) {
        // distribute space between all visible children who can be resized.
        // To do this we need to work out if we're shrinking or growing the
        // children, and then which children can be resized based on their
        // min/pref/max/fixed properties. The results of this are in the
        // resizingChildren observableArrayList above.
        final int columnCount = columns.size();

        // work out how much of the delta we should give to each child. It should
        // be an equal amount (at present), although perhaps we'll allow for
        // functions to calculate this at a later date.
        double colDelta = delta / columnCount;

        // we maintain a count of the amount of delta remaining to ensure that
        // the column resize operation accurately reflects the location of the
        // mouse pointer. Every time this value is not 0, the UI is a teeny bit
        // more inaccurate whilst the user continues to resize.
        double remainingDelta = delta;

        // We maintain a count of the current column that we're on in case we
        // need to redistribute the remainingDelta among remaining sibling.
        int col = 0;

        // This is a bit hacky - often times the leftOverDelta is zero, but
        // remainingDelta doesn't quite get down to 0. In these instances we
        // short-circuit and just return 0.0.
        boolean isClean = true;
        for (TableColumnBase<?,?> childCol: columns) {
            col++;

            // resize each child column
            double leftOverDelta = resize(rf, childCol, colDelta);

            // calculate the remaining delta if the was anything left over in
            // the last resize operation
            remainingDelta = remainingDelta - colDelta + leftOverDelta;

            if (leftOverDelta != 0) {
                isClean = false;
                // and recalculate the distribution of the remaining delta for
                // the remaining siblings.
                colDelta = remainingDelta / (columnCount - col);
            }
        }

        // see isClean above for why this is done
        return isClean ? 0.0 : remainingDelta;
    }
}

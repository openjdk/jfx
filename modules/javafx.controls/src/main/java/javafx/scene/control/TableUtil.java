/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * A package protected util class used by TableView and TreeTableView to reduce
 * the level of code duplication.
 */
class TableUtil {

    /**
     * Constant to consider floating-point arithmetic precision.
      */
    private static final double EPSILON = .0000001;

    private TableUtil() {
        // no-op
    }

    static void removeTableColumnListener(List<? extends TableColumnBase> list,
                        final InvalidationListener columnVisibleObserver,
                        final InvalidationListener columnSortableObserver,
                        final InvalidationListener columnSortTypeObserver,
                        final InvalidationListener columnComparatorObserver) {

        if (list == null) return;
        for (TableColumnBase col : list) {
            col.visibleProperty().removeListener(columnVisibleObserver);
            col.sortableProperty().removeListener(columnSortableObserver);
            col.comparatorProperty().removeListener(columnComparatorObserver);

//            col.sortTypeProperty().removeListener(columnSortTypeObserver);
            if (col instanceof TableColumn) {
                ((TableColumn)col).sortTypeProperty().removeListener(columnSortTypeObserver);
            } else if (col instanceof TreeTableColumn) {
                ((TreeTableColumn)col).sortTypeProperty().removeListener(columnSortTypeObserver);
            }

            removeTableColumnListener(col.getColumns(),
                                      columnVisibleObserver,
                                      columnSortableObserver,
                                      columnSortTypeObserver,
                                      columnComparatorObserver);
        }
    }

    static void addTableColumnListener(List<? extends TableColumnBase> list,
                        final InvalidationListener columnVisibleObserver,
                        final InvalidationListener columnSortableObserver,
                        final InvalidationListener columnSortTypeObserver,
                        final InvalidationListener columnComparatorObserver) {

        if (list == null) return;
        for (TableColumnBase col : list) {
            col.visibleProperty().addListener(columnVisibleObserver);
            col.sortableProperty().addListener(columnSortableObserver);
            col.comparatorProperty().addListener(columnComparatorObserver);

            if (col instanceof TableColumn) {
                ((TableColumn)col).sortTypeProperty().addListener(columnSortTypeObserver);
            } else if (col instanceof TreeTableColumn) {
                ((TreeTableColumn)col).sortTypeProperty().addListener(columnSortTypeObserver);
            }

            addTableColumnListener(col.getColumns(),
                                   columnVisibleObserver,
                                   columnSortableObserver,
                                   columnSortTypeObserver,
                                   columnComparatorObserver);
        }
    }

    static void removeColumnsListener(List<? extends TableColumnBase> list, ListChangeListener cl) {
        if (list == null) return;

        for (TableColumnBase col : list) {
            col.getColumns().removeListener(cl);
            removeColumnsListener(col.getColumns(), cl);
        }
    }

    static void addColumnsListener(List<? extends TableColumnBase> list, ListChangeListener cl) {
        if (list == null) return;

        for (TableColumnBase col : list) {
            col.getColumns().addListener(cl);
            addColumnsListener(col.getColumns(), cl);
        }
    }

    static void handleSortFailure(ObservableList<? extends TableColumnBase> sortOrder,
            SortEventType sortEventType, final Object... supportInfo) {
        // if the sort event is consumed we need to back out the previous
        // action so that the UI is not in an incorrect state
        if (sortEventType == SortEventType.COLUMN_SORT_TYPE_CHANGE) {
            // go back to the previous sort type
            final TableColumnBase changedColumn = (TableColumnBase) supportInfo[0];
            revertSortType(changedColumn);
        } else if (sortEventType == SortEventType.SORT_ORDER_CHANGE) {
            // Revert the sortOrder list to what it was previously
            ListChangeListener.Change change = (ListChangeListener.Change) supportInfo[0];

            final List toRemove = new ArrayList();
            final List toAdd = new ArrayList();
            while (change.next()) {
                if (change.wasAdded()) {
                    toRemove.addAll(change.getAddedSubList());
                }

                if (change.wasRemoved()) {
                    toAdd.addAll(change.getRemoved());
                }
            }

            sortOrder.removeAll(toRemove);
            sortOrder.addAll(toAdd);
        } else if (sortEventType == SortEventType.COLUMN_SORTABLE_CHANGE) {
            // no-op - it is ok for the sortable type to remain as-is
        } else if (sortEventType == SortEventType.COLUMN_COMPARATOR_CHANGE) {
            // no-op - it is ok for the comparator to remain as-is
        }
    }

    private static void revertSortType(TableColumnBase changedColumn) {
        if (changedColumn instanceof TableColumn) {
            TableColumn tableColumn = (TableColumn)changedColumn;
            final TableColumn.SortType sortType = tableColumn.getSortType();
            if (sortType == TableColumn.SortType.ASCENDING) {
                tableColumn.setSortType(null);
            } else if (sortType == TableColumn.SortType.DESCENDING) {
                tableColumn.setSortType(TableColumn.SortType.ASCENDING);
            } else if (sortType == null) {
                tableColumn.setSortType(TableColumn.SortType.DESCENDING);
            }
        } else if (changedColumn instanceof TreeTableColumn) {
            TreeTableColumn tableColumn = (TreeTableColumn)changedColumn;
            final TreeTableColumn.SortType sortType = tableColumn.getSortType();
            if (sortType == TreeTableColumn.SortType.ASCENDING) {
                tableColumn.setSortType(null);
            } else if (sortType == TreeTableColumn.SortType.DESCENDING) {
                tableColumn.setSortType(TreeTableColumn.SortType.ASCENDING);
            } else if (sortType == null) {
                tableColumn.setSortType(TreeTableColumn.SortType.DESCENDING);
            }
        }
    }

    static enum SortEventType {
         SORT_ORDER_CHANGE,
         COLUMN_SORT_TYPE_CHANGE,
         COLUMN_SORTABLE_CHANGE,
         COLUMN_COMPARATOR_CHANGE
     }





    /**
     * The constrained resize algorithm used by TableView and TreeTableView.
     * @param prop
     * @param isFirstRun
     * @param tableWidth
     * @param visibleLeafColumns
     * @return
     */
    static boolean constrainedResize(ResizeFeaturesBase prop,
                                     boolean isFirstRun,
                                     double tableWidth,
                                     List<? extends TableColumnBase<?,?>> visibleLeafColumns) {
        TableColumnBase<?,?> column = prop.getColumn();
        double delta = prop.getDelta();

        /*
         * There are two phases to the constrained resize policy:
         *   1) Ensuring internal consistency (i.e. table width == sum of all visible
         *      columns width). This is often called when the table is resized.
         *   2) Resizing the given column by __up to__ the given delta.
         *
         * It is possible that phase 1 occur and there be no need for phase 2 to
         * occur.
         */

        boolean isShrinking;
        double target;
        double totalLowerBound = 0;
        double totalUpperBound = 0;

        if (tableWidth == 0) return false;

        /*
         * PHASE 1: Check to ensure we have internal consistency. Based on the
         *          Swing JTable implementation.
         */
        // determine the width of all visible columns, and their preferred width
        double colWidth = 0;
        for (TableColumnBase<?,?> col : visibleLeafColumns) {
            colWidth += col.getWidth();
        }

        if (Math.abs(colWidth - tableWidth) > EPSILON) {
            isShrinking = colWidth > tableWidth;
            target = tableWidth;

            if (isFirstRun) {
                // if we are here we have an inconsistency - these two values should be
                // equal when this resizing policy is being used.
                for (TableColumnBase<?,?> col : visibleLeafColumns) {
                    totalLowerBound += col.getMinWidth();
                    totalUpperBound += col.getMaxWidth();
                }

                // We run into trouble if the numbers are set to infinity later on
                totalUpperBound = totalUpperBound == Double.POSITIVE_INFINITY ?
                    Double.MAX_VALUE :
                    (totalUpperBound == Double.NEGATIVE_INFINITY ? Double.MIN_VALUE : totalUpperBound);

                for (TableColumnBase col : visibleLeafColumns) {
                    double lowerBound = col.getMinWidth();
                    double upperBound = col.getMaxWidth();

                    // Check for zero. This happens when the distribution of the delta
                    // finishes early due to a series of "fixed" entries at the end.
                    // In this case, lowerBound == upperBound, for all subsequent terms.
                    double newSize;
                    if (Math.abs(totalLowerBound - totalUpperBound) < EPSILON) {
                        newSize = lowerBound;
                    } else {
                        double f = (target - totalLowerBound) / (totalUpperBound - totalLowerBound);
                        newSize = Math.round(lowerBound + f * (upperBound - lowerBound));
                    }

                    double remainder = resize(col, newSize - col.getWidth());

                    target -= newSize + remainder;
                    totalLowerBound -= lowerBound;
                    totalUpperBound -= upperBound;
                }

                isFirstRun = false;
            } else {
                double actualDelta = tableWidth - colWidth;
                List<? extends TableColumnBase<?,?>> cols = visibleLeafColumns;
                resizeColumns(cols, actualDelta);
            }
        }

        // At this point we can be happy in the knowledge that we have internal
        // consistency, i.e. table width == sum of the width of all visible
        // leaf columns.

        /*
         * Column may be null if we just changed the resize policy, and we
         * just wanted to enforce internal consistency, as mentioned above.
         */
        if (column == null) {
            return false;
        }

        /*
         * PHASE 2: Handling actual column resizing (by the user). Based on my own
         *          implementation (based on the UX spec).
         */

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

        // we now can split the observableArrayList into two subobservableArrayLists, representing all
        // columns that should grow, and all columns that should shrink
        //    var growingCols = if (isShrinking)
        //        then table.visibleLeafColumns[colPos+1..endColPos]
        //        else table.visibleLeafColumns[0..colPos];
        //    var shrinkingCols = if (isShrinking)
        //        then table.visibleLeafColumns[0..colPos]
        //        else table.visibleLeafColumns[colPos+1..endColPos];


        double remainingDelta = delta;
        while (endColPos > colPos && remainingDelta != 0) {
            TableColumnBase<?,?> resizingCol = visibleLeafColumns.get(endColPos);
            endColPos--;

            // if the column width is fixed, break out and try the next column
            if (! resizingCol.isResizable()) continue;

            // for convenience we discern between the shrinking and growing columns
            TableColumnBase<?,?> shrinkingCol = isShrinking ? leafColumn : resizingCol;
            TableColumnBase<?,?> growingCol = !isShrinking ? leafColumn : resizingCol;

            //        (shrinkingCol.width == shrinkingCol.minWidth) or (growingCol.width == growingCol.maxWidth)

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
            //
            //        if (shrinkingCol.width < shrinkingCol.prefWidth) {
            //            for (c in reverse table.visibleLeafColumns[colPos+1..endColPos]) {
            //                if (c.width > c.prefWidth) {
            //                    shrinkingCol = c;
            //                    break;
            //                }
            //            }
            //        }



            double sdiff = Math.min(Math.abs(remainingDelta), shrinkingCol.getWidth() - shrinkingCol.getMinWidth());

//                System.out.println("\tshrinking " + shrinkingCol.getText() + " and growing " + growingCol.getText());
//                System.out.println("\t\tMath.min(Math.abs("+remainingDelta+"), "+shrinkingCol.getWidth()+" - "+shrinkingCol.getMinWidth()+") = " + sdiff);

            double delta1 = resize(shrinkingCol, -sdiff);
            double delta2 = resize(growingCol, sdiff);
            remainingDelta += isShrinking ? sdiff : -sdiff;
        }
        return remainingDelta == 0;
    }

    // function used to actually perform the resizing of the given column,
    // whilst ensuring it stays within the min and max bounds set on the column.
    // Returns the remaining delta if it could not all be applied.
    static double resize(TableColumnBase column, double delta) {
        if (delta == 0) return 0.0F;
        if (! column.isResizable()) return delta;

        final boolean isShrinking = delta < 0;
        final List<TableColumnBase<?,?>> resizingChildren = getResizableChildren(column, isShrinking);

        if (resizingChildren.size() > 0) {
            return resizeColumns(resizingChildren, delta);
        } else {
            double newWidth = column.getWidth() + delta;

            if (newWidth > column.getMaxWidth()) {
                column.doSetWidth(column.getMaxWidth());
                return newWidth - column.getMaxWidth();
            } else if (newWidth < column.getMinWidth()) {
                column.doSetWidth(column.getMinWidth());
                return newWidth - column.getMinWidth();
            } else {
                column.doSetWidth(newWidth);
                return 0.0F;
            }
        }
    }

    // Returns all children columns of the given column that are able to be
    // resized. This is based on whether they are visible, resizable, and have
    // not space before they hit the min / max values.
    private static List<TableColumnBase<?,?>> getResizableChildren(TableColumnBase<?,?> column, boolean isShrinking) {
        if (column == null || column.getColumns().isEmpty()) {
            return Collections.emptyList();
        }

        List<TableColumnBase<?,?>> tablecolumns = new ArrayList<TableColumnBase<?,?>>();
        for (TableColumnBase c : column.getColumns()) {
            if (! c.isVisible()) continue;
            if (! c.isResizable()) continue;

            if (isShrinking && c.getWidth() > c.getMinWidth()) {
                tablecolumns.add(c);
            } else if (!isShrinking && c.getWidth() < c.getMaxWidth()) {
                tablecolumns.add(c);
            }
        }
        return tablecolumns;
    }

    private static double resizeColumns(List<? extends TableColumnBase<?,?>> columns, double delta) {
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
        for (TableColumnBase<?,?> childCol : columns) {
            col++;

            // resize each child column
            double leftOverDelta = resize(childCol, colDelta);

            // calculate the remaining delta if the was anything left over in
            // the last resize operation
            remainingDelta = remainingDelta - colDelta + leftOverDelta;

            //      println("\tResized {childCol.text} with {colDelta}, but {leftOverDelta} was left over. RemainingDelta is now {remainingDelta}");

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

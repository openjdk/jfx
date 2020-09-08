/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * The abstract base class for MultipleSelectionModel implementations that are used within
 * table-like controls (most notably {@link TableView} and {@link TreeTableView}.
 *
 * @param <T> The type of the underlying data model for the UI control.
 * @since JavaFX 8.0
 */
public abstract class TableSelectionModel<T> extends MultipleSelectionModelBase<T> {

    /**
     * Constructor for subclasses to call.
     */
    public TableSelectionModel() {
    }

    /**
     * Convenience function which tests whether the given row and column index
     * is currently selected in this table instance. If the table control is in its
     * 'cell selection' mode (where individual cells can be selected, rather than
     * entire rows), and if the column argument is null, this method should return
     * true only if all cells in the given row are selected.
     * @param row the row
     * @param column the column
     * @return true if the given row and column index is currently selected in
     * this table instance
     */
    public abstract boolean isSelected(int row, TableColumnBase<T,?> column);

    /**
     * Selects the cell at the given row/column intersection. If the table control is in its
     * 'cell selection' mode (where individual cells can be selected, rather than
     * entire rows), and if the column argument is null, this method should select
     * all cells in the given row.
     * @param row the row
     * @param column the column
     */
    public abstract void select(int row, TableColumnBase<T,?> column);

    /**
     * Clears all selection, and then selects the cell at the given row/column
     * intersection. If the table control is in its
     * 'cell selection' mode (where individual cells can be selected, rather than
     * entire rows), and if the column argument is null, this method should select
     * all cells in the given row.
     * @param row the row
     * @param column the column
     */
    public abstract void clearAndSelect(int row, TableColumnBase<T,?> column);

    /**
     * Removes selection from the specified row/column position (in view indexes).
     * If this particular cell (or row if the column value is -1) is not selected,
     * nothing happens. If the table control is in its
     * 'cell selection' mode (where individual cells can be selected, rather than
     * entire rows), and if the column argument is null, this method should deselect
     * all cells in the given row.
     * @param row the row
     * @param column the column
     */
    public abstract void clearSelection(int row, TableColumnBase<T,?> column);

    /**
     * Selects the cell to the left of the currently selected cell.
     */
    public abstract void selectLeftCell();

    /**
     * Selects the cell to the right of the currently selected cell.
     */
    public abstract void selectRightCell();

    /**
     * Selects the cell directly above the currently selected cell.
     */
    public abstract void selectAboveCell();

    /**
     * Selects the cell directly below the currently selected cell.
     */
    public abstract void selectBelowCell();

    /**
     * Selects the cells in the range (minRow, minColumn) to (maxRow, maxColumn),
     * inclusive.
     * @param minRow the minRow
     * @param minColumn the minColumn
     * @param maxRow the maxRow
     * @param maxColumn the maxColumn
     */
    public abstract void selectRange(int minRow, TableColumnBase<T,?> minColumn,
                                     int maxRow, TableColumnBase<T,?> maxColumn);

    /**
     * A boolean property used to represent whether the table is in
     * row or cell selection modes. By default a table is in row selection
     * mode which means that individual cells can not be selected. Setting
     * <code>cellSelectionEnabled</code> to be true results in cells being
     * able to be selected (but not rows).
     */
    private BooleanProperty cellSelectionEnabled =
           new SimpleBooleanProperty(this, "cellSelectionEnabled");
    public final BooleanProperty cellSelectionEnabledProperty() {
       return cellSelectionEnabled;
    }
    public final void setCellSelectionEnabled(boolean value) {
       cellSelectionEnabledProperty().set(value);
    }
    public final boolean isCellSelectionEnabled() {
       return cellSelectionEnabled == null ? false : cellSelectionEnabled.get();
    }
}

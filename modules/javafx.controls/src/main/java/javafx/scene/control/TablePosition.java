/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.List;

import javafx.beans.NamedArg;

/**
 * This class is used to represent a single row/column/cell in a TableView.
 * This is used throughout the TableView API to represent which rows/columns/cells
 * are currently selected, focused, being edited, etc. Note that this class is
 * immutable once it is created.
 *
 * <p>Because the TableView can have different
 * {@link SelectionMode selection modes}, the row and column properties in
 * TablePosition can be 'disabled' to represent an entire row or column. This is
 * done by setting the unrequired property to -1 or null.
 *
 * @param <S> The type of the items contained within the TableView (i.e. the same
 *      generic type as the S in TableView&lt;S&gt;).
 * @param <T> The type of the items contained within the TableColumn.
 * @see TableView
 * @see TableColumn
 * @since JavaFX 2.0
 */
public class TablePosition<S,T> extends TablePositionBase<TableColumn<S,T>> {

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructs a TablePosition instance to represent the given row/column
     * position in the given TableView instance. Both the TableView and
     * TableColumn are referenced weakly in this class, so it is possible that
     * they will be null when their respective getters are called.
     *
     * @param tableView The TableView that this position is related to.
     * @param row The row that this TablePosition is representing.
     * @param tableColumn The TableColumn instance that this TablePosition represents.
     */
    public TablePosition(@NamedArg("tableView") TableView<S> tableView, @NamedArg("row") int row, @NamedArg("tableColumn") TableColumn<S,T> tableColumn) {
        super(row, tableColumn);
        this.controlRef = new WeakReference<>(tableView);

        List<S> items = tableView != null ? tableView.getItems() : null;
        this.itemRef = new WeakReference<>(
                items != null && row >= 0 && row < items.size() ? items.get(row) : null);

        nonFixedColumnIndex = tableView == null || tableColumn == null ? -1 : tableView.getVisibleLeafIndex(tableColumn);
    }



    /* *************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    private final WeakReference<TableView<S>> controlRef;
    private final WeakReference<S> itemRef;
    int fixedColumnIndex = -1;
    private final int nonFixedColumnIndex;

    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The column index that this TablePosition represents in the TableView. It
     * is -1 if the TableView or TableColumn instances are null at the time the class
     * is instantiated (i.e. it is computed at construction).
     */
    @Override public int getColumn() {
        if (fixedColumnIndex > -1) {
            return fixedColumnIndex;
        }

        return nonFixedColumnIndex;
    }

    /**
     * The TableView that this TablePosition is related to.
     * @return the TableView
     */
    public final TableView<S> getTableView() {
        return controlRef.get();
    }

    /** {@inheritDoc} */
    @Override public final TableColumn<S,T> getTableColumn() {
        // Forcing the return type to be TableColumn<S,T>, not TableColumnBase<S,T>
        return super.getTableColumn();
    }

    /**
     * Returns the item that backs the {@link #getRow()} row}, at the point
     * in time when this TablePosition was created.
     */
    final S getItem() {
        return itemRef == null ? null : itemRef.get();
    }

    /**
     * Returns a string representation of this {@code TablePosition} object.
     * @return a string representation of this {@code TablePosition} object.
     */
    @Override public String toString() {
        return "TablePosition [ row: " + getRow() + ", column: " + getTableColumn() + ", "
                + "tableView: " + getTableView() + " ]";
    }
}

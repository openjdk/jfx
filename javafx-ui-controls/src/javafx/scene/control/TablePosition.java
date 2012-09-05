/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
public class TablePosition<S,T> {
    
    /***************************************************************************
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
    public TablePosition(TableView<S> tableView, int row, TableColumn<S,T> tableColumn) {
        this.row = row;
        this.tableColumnRef = new WeakReference<TableColumn<S, T>>(tableColumn);
        this.tableViewRef = new WeakReference<TableView<S>>(tableView);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    private final int row;
    private final WeakReference<TableColumn<S,T>> tableColumnRef;
    private final WeakReference<TableView<S>> tableViewRef;



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/
    
    /**
     * The row that this TablePosition represents in the TableView.
     */
    public final int getRow() {
        return row;
    }
    
    /**
     * The column index that this TablePosition represents in the TableView. It
     * is -1 if the TableView or TableColumn instances are null.
     */
    public final int getColumn() {
        TableView tableView = getTableView();
        TableColumn tableColumn = getTableColumn();
        return tableView == null || tableColumn == null ? -1 : 
                tableView.getVisibleLeafIndex(tableColumn);
    }
    
    /**
     * The TableView that this TablePosition is related to.
     */
    public final TableView<S> getTableView() {
        return tableViewRef.get();
    }
    
    /**
     * The TableColumn that this TablePosition represents in the TableView.
     */
    public final TableColumn<S,T> getTableColumn() {
        return tableColumnRef.get();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is equal to the {@code obj} argument; {@code false} otherwise.
     */
    @Override public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        final TablePosition<S,T> other = (TablePosition<S,T>) obj;
        if (this.row != other.row) {
            return false;
        }
        TableColumn tableColumn = getTableColumn();
        TableColumn otherTableColumn = other.getTableColumn();
        if (tableColumn != otherTableColumn && (tableColumn == null || !tableColumn.equals(otherTableColumn))) {
            return false;
        }
        TableView tableView = getTableView();
        TableView otherTableView = other.getTableView();
        if (tableView != otherTableView && (tableView == null || !tableView.equals(otherTableView))) {
            return false;
        }
        return true;
    }

    /**
     * Returns a hash code for this {@code TablePosition} object.
     * @return a hash code for this {@code TablePosition} object.
     */ 
    @Override public int hashCode() {
        int hash = 5;
        hash = 79 * hash + this.row;
        hash = 79 * hash + (getTableColumn() != null ? getTableColumn().hashCode() : 0);
        hash = 79 * hash + (getTableView() != null ? getTableView().hashCode() : 0);
        return hash;
    }

    /**
     * Returns a string representation of this {@code TablePosition} object.
     * @return a string representation of this {@code TablePosition} object.
     */ 
    @Override public String toString() {
        return "TablePosition [ row: " + row + ", column: " + getTableColumn() + ", "
                + "tableView: " + getTableView() + " ]";
    }
}

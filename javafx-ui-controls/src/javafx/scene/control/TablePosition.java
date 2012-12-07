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

import com.sun.javafx.beans.annotations.NoBuilder;
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
@NoBuilder
public class TablePosition<S,T> extends TablePositionBase<TableColumn<S,T>> {
    
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
        super(row, tableColumn);
        this.controlRef = new WeakReference<TableView<S>>(tableView);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    private final WeakReference<TableView<S>> controlRef;


    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/
    
    /**
     * The column index that this TablePosition represents in the TableView. It
     * is -1 if the TableView or TableColumn instances are null.
     */
    @Override public int getColumn() {
        TableView tableView = getTableView();
        TableColumn tableColumn = getTableColumn();
        return tableView == null || tableColumn == null ? -1 : 
                tableView.getVisibleLeafIndex(tableColumn);
    }
    
    /**
     * The TableView that this TablePosition is related to.
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
     * Returns a string representation of this {@code TablePosition} object.
     * @return a string representation of this {@code TablePosition} object.
     */ 
    @Override public String toString() {
        return "TablePosition [ row: " + getRow() + ", column: " + getTableColumn() + ", "
                + "tableView: " + getTableView() + " ]";
    }
}

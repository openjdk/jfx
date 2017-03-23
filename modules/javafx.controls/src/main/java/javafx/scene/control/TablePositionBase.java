/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * This class is used to represent a single row/column/cell in a table. Concrete
 * subclasses of this abstract class are used in the {@link TableView} and
 * {@link TreeTableView} APIs to represent which rows/columns/cells
 * are currently selected, focused, being edited, etc. Note that this class is
 * immutable once it is created.
 *
 * <p>Because the TableView and TreeTableView controls can have different
 * {@link SelectionMode selection modes}, the row and column properties in
 * TablePositionBase can be 'disabled' to represent an entire row or column. This is
 * done by setting the unrequired property to -1 or null.
 *
 * @see TablePosition
 * @see TreeTablePosition
 * @since JavaFX 8.0
 */
public abstract class TablePositionBase<TC extends TableColumnBase> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructs a TablePositionBase instance to represent the given row/column
     * position in the underlying table instance (which is not part of the
     * abstract TablePositionBase class, but is part of concrete subclasses such
     * as {@link TablePosition} and {@link TreeTablePosition}). In all cases,
     * all fields inside TablePositionBase instances are referenced weakly so as
     * to prevent memory leaks. This means that it is possible (but unlikely)
     * that the get methods will return null.
     *
     * @param row The row that this TablePosition is representing.
     * @param tableColumn The TableColumn instance that this TablePosition represents.
     */
    protected TablePositionBase(int row, TC tableColumn) {
        this.row = row;
        this.tableColumnRef = new WeakReference<TC>(tableColumn);
    }



    /***************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    private final int row;
    private final WeakReference<TC> tableColumnRef;



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The row that this TablePosition represents in the TableView.
     * @return the row that this TablePosition represents in the TableView
     */
    public int getRow() {
        return row;
    }

    /**
     * The column index that this TablePosition represents in the TableView. It
     * is -1 if the TableView or TableColumn instances are null.
     * @return the column index that this TablePosition represents in the
     * TableView
     */
    public abstract int getColumn();

    /**
     * The TableColumn that this TablePosition represents in the TableView.
     * @return the TableColumn that this TablePosition represents in the TableView
     */
    public TC getTableColumn() {
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
        final TablePositionBase other = (TablePositionBase) obj;
        if (this.row != other.row) {
            return false;
        }
        TC tableColumn = getTableColumn();
        TableColumnBase otherTableColumn = other.getTableColumn();
        if (tableColumn != otherTableColumn && (tableColumn == null || !tableColumn.equals(otherTableColumn))) {
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
        TableColumnBase tableColumn = getTableColumn();
        hash = 79 * hash + (tableColumn != null ? tableColumn.hashCode() : 0);
        return hash;
    }
}

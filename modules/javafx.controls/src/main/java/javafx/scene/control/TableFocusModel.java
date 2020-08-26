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

/**
 * The abstract base class for FocusModel implementations that are used within
 * table-like controls (most notably {@link TableView} and {@link TreeTableView}.
 *
 * @param <T> The type of the underlying data model for the UI control.
 * @param <TC> The concrete subclass of {@link TableColumnBase} that is used by the
 *      underlying UI control (e.g. {@link TableColumn} or {@link TreeTableColumn}.
 * @since JavaFX 8.0
 */
public abstract class TableFocusModel<T, TC extends TableColumnBase<T,?>> extends FocusModel<T> {

    /***********************************************************************
     *                                                                     *
     * Public API                                                          *
     *                                                                     *
     **********************************************************************/

    /**
     * Constructor for subclasses to call.
     */
    public TableFocusModel() {
    }

    /**
     * Causes the item at the given index to receive the focus.
     *
     * @param row The row index of the item to give focus to.
     * @param column The column of the item to give focus to. Can be null.
     */
    public abstract void focus(int row, TC column);

    /**
     * Tests whether the row / cell at the given location currently has the
     * focus within the UI control.
     * @param row the row
     * @param column the column
     * @return true if the row / cell at the given location currently has the
     * focus within the UI control
     */
    public abstract boolean isFocused(int row, TC column);

    /**
     * Attempts to move focus to the cell above the currently focused cell.
     */
    public abstract void focusAboveCell();

    /**
     * Attempts to move focus to the cell below the currently focused cell.
     */
    public abstract void focusBelowCell();

    /**
     * Attempts to move focus to the cell to the left of the currently focused cell.
     */
    public abstract void focusLeftCell();

    /**
     * Attempts to move focus to the cell to the right of the the currently focused cell.
     */
    public abstract void focusRightCell();



     /***********************************************************************
     *                                                                     *
     * Private Implementation                                              *
     *                                                                     *
     **********************************************************************/

}

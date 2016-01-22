/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.behavior;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TablePositionBase;
import javafx.scene.control.TableView;

import javafx.scene.control.TableView.TableViewFocusModel;

/**
 */
public class TableCellBehavior<S,T> extends TableCellBehaviorBase<S, T, TableColumn<S,?>, TableCell<S, T>> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public TableCellBehavior(TableCell<S,T> control) {
        super(control);
    }



    /***************************************************************************
     *                                                                         *
     * Implement TableCellBehaviorBase Abstract API                            *
     *                                                                         *
     **************************************************************************/

    /** @{@inheritDoc} */
    @Override protected TableView<S> getCellContainer() {
        return getNode().getTableView();
    }

    /** @{@inheritDoc} */
    @Override protected TableColumn<S,T> getTableColumn() {
        return getNode().getTableColumn();
    }

    /** @{@inheritDoc} */
    @Override protected int getItemCount() {
        return getCellContainer().getItems().size();
    }

    /** @{@inheritDoc} */
    @Override protected TableView.TableViewSelectionModel<S> getSelectionModel() {
        return getCellContainer().getSelectionModel();
    }

    /** @{@inheritDoc} */
    @Override protected TableViewFocusModel<S> getFocusModel() {
        return getCellContainer().getFocusModel();
    }

    /** @{@inheritDoc} */
    @Override protected TablePositionBase getFocusedCell() {
        return getCellContainer().getFocusModel().getFocusedCell();
    }

    /** @{@inheritDoc} */
    @Override protected boolean isTableRowSelected() {
        return getNode().getTableRow().isSelected();
    }

    /** @{@inheritDoc} */
    @Override protected int getVisibleLeafIndex(TableColumnBase tc) {
        return getCellContainer().getVisibleLeafIndex((TableColumn) tc);
    }

    /** @{@inheritDoc} */
    @Override protected void focus(int row, TableColumnBase tc) {
        getFocusModel().focus(row, (TableColumn)tc);
    }

    /** @{@inheritDoc} */
    @Override protected void edit(TableCell<S,T> cell) {
        if (cell == null) {
            getCellContainer().edit(-1, null);
        } else {
            getCellContainer().edit(cell.getIndex(), cell.getTableColumn());
        }
    }
}

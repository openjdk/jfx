/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableFocusModel;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TablePositionBase;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import com.sun.javafx.scene.control.skin.Utils;


public class TableViewBehavior<T> extends TableViewBehaviorBase<TableView<T>, T, TableColumn<T, ?>> {

    /**************************************************************************
     *                                                                        *
     * Listeners                                                              *
     *                                                                        *
     *************************************************************************/

    private final ChangeListener<TableViewSelectionModel<T>> selectionModelListener =
            (observable, oldValue, newValue) -> {
                if (oldValue != null) {
                    oldValue.getSelectedCells().removeListener(weakSelectedCellsListener);
                }
                if (newValue != null) {
                    newValue.getSelectedCells().addListener(weakSelectedCellsListener);
                }
            };

    private final WeakChangeListener<TableViewSelectionModel<T>> weakSelectionModelListener =
            new WeakChangeListener<TableViewSelectionModel<T>>(selectionModelListener);

    private TwoLevelFocusBehavior tlFocus;



    /**************************************************************************
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     *************************************************************************/

    public TableViewBehavior(TableView<T> control) {
        super(control);

        // Fix for RT-16565
        control.selectionModelProperty().addListener(weakSelectionModelListener);
        TableViewSelectionModel<T> sm = control.getSelectionModel();
        if (sm != null) {
            sm.getSelectedCells().addListener(selectedCellsListener);
        }

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusBehavior(control); // needs to be last.
        }
    }

    @Override public void dispose() {
        if (tlFocus != null) tlFocus.dispose();
        super.dispose();
    }

    /**************************************************************************
     *                                                                        *
     * Implement TableViewBehaviorBase abstract methods                       *
     *                                                                        *
     *************************************************************************/

    /** {@inheritDoc}  */
    @Override protected int getItemCount() {
        return getNode().getItems() == null ? 0 : getNode().getItems().size();
    }

    /** {@inheritDoc}  */
    @Override protected TableFocusModel getFocusModel() {
        return getNode().getFocusModel();
    }

    /** {@inheritDoc}  */
    @Override protected TableSelectionModel<T> getSelectionModel() {
        return getNode().getSelectionModel();
    }

    /** {@inheritDoc}  */
    @Override protected ObservableList<TablePosition> getSelectedCells() {
        return getNode().getSelectionModel().getSelectedCells();
    }

    /** {@inheritDoc}  */
    @Override protected TablePositionBase getFocusedCell() {
        return getNode().getFocusModel().getFocusedCell();
    }

    /** {@inheritDoc}  */
    @Override protected int getVisibleLeafIndex(TableColumnBase tc) {
        return getNode().getVisibleLeafIndex((TableColumn)tc);
    }

    /** {@inheritDoc}  */
    @Override protected TableColumn<T,?> getVisibleLeafColumn(int index) {
        return getNode().getVisibleLeafColumn(index);
    }

    /** {@inheritDoc} */
    @Override protected boolean isControlEditable() {
        return getNode().isEditable();
    }

    /** {@inheritDoc}  */
    @Override protected void editCell(int row, TableColumnBase tc) {
        getNode().edit(row, (TableColumn)tc);
    }

    /** {@inheritDoc}  */
    @Override protected ObservableList<TableColumn<T,?>> getVisibleLeafColumns() {
        return getNode().getVisibleLeafColumns();
    }

    /** {@inheritDoc}  */
    @Override protected TablePositionBase<TableColumn<T, ?>>
            getTablePosition(int row, TableColumnBase<T, ?> tc) {
        return new TablePosition(getNode(), row, (TableColumn)tc);
    }



    /**************************************************************************
     *                                                                        *
     * Modify TableViewBehaviorBase behavior                                  *
     *                                                                        *
     *************************************************************************/

    /** {@inheritDoc} */
    @Override protected void selectAllToFocus(boolean setAnchorToFocusIndex) {
        // Fix for RT-31241
        if (getNode().getEditingCell() != null) return;

        super.selectAllToFocus(setAnchorToFocusIndex);
    }
}

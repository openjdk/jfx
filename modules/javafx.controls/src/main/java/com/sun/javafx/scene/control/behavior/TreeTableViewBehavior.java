/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.input.KeyCode.*;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableFocusModel;
import javafx.scene.control.TablePositionBase;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.util.Callback;

public class TreeTableViewBehavior<T> extends TableViewBehaviorBase<TreeTableView<T>, TreeItem<T>, TreeTableColumn<T, ?>> {

    /**************************************************************************
     *                                                                        *
     * Listeners                                                              *
     *                                                                        *
     *************************************************************************/

    private final ChangeListener<TreeTableView.TreeTableViewSelectionModel<T>> selectionModelListener =
            (observable, oldValue, newValue) -> {
                if (oldValue != null) {
                    oldValue.getSelectedCells().removeListener(weakSelectedCellsListener);
                }
                if (newValue != null) {
                    newValue.getSelectedCells().addListener(weakSelectedCellsListener);
                }
            };

    private final WeakChangeListener<TreeTableView.TreeTableViewSelectionModel<T>> weakSelectionModelListener =
            new WeakChangeListener<>(selectionModelListener);



    /**************************************************************************
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     *************************************************************************/

    public TreeTableViewBehavior(TreeTableView<T>  control) {
        super(control);

        // Add these bindings as a child input map, so they take precedence
        InputMap<TreeTableView<T>> expandCollapseInputMap = new InputMap<>(control);
        expandCollapseInputMap.getMappings().addAll(
            // these should be read as 'if RTL, use the first method, otherwise use the second'
            new InputMap.KeyMapping(LEFT, e -> rtl(control, this::expandRow, this::collapseRow)),
            new InputMap.KeyMapping(KP_LEFT, e -> rtl(control, this::expandRow, this::collapseRow)),
            new InputMap.KeyMapping(RIGHT, e -> rtl(control, this::collapseRow, this::expandRow)),
            new InputMap.KeyMapping(KP_RIGHT, e -> rtl(control, this::collapseRow, this::expandRow)),

            new InputMap.KeyMapping(MULTIPLY, e -> expandAll()),
            new InputMap.KeyMapping(ADD, e -> expandRow()),
            new InputMap.KeyMapping(SUBTRACT, e -> collapseRow())
        );
        addDefaultChildMap(getInputMap(), expandCollapseInputMap);


        // Fix for RT-16565
        control.selectionModelProperty().addListener(weakSelectionModelListener);
        if (getSelectionModel() != null) {
            control.getSelectionModel().getSelectedCells().addListener(selectedCellsListener);
        }
    }



    /**************************************************************************
     *                                                                        *
     * Implement TableViewBehaviorBase abstract methods                       *
     *                                                                        *
     *************************************************************************/

    /** {@inheritDoc}  */
    @Override protected int getItemCount() {
        return getNode().getExpandedItemCount();
    }

    /** {@inheritDoc}  */
    @Override protected TableFocusModel getFocusModel() {
        return getNode().getFocusModel();
    }

    /** {@inheritDoc}  */
    @Override protected TableSelectionModel<TreeItem<T>> getSelectionModel() {
        return getNode().getSelectionModel();
    }

    /** {@inheritDoc}  */
    @Override protected ObservableList<TreeTablePosition<T,?>> getSelectedCells() {
        return getNode().getSelectionModel().getSelectedCells();
    }

    /** {@inheritDoc}  */
    @Override protected TablePositionBase getFocusedCell() {
        return getNode().getFocusModel().getFocusedCell();
    }

    /** {@inheritDoc}  */
    @Override protected int getVisibleLeafIndex(TableColumnBase tc) {
        return getNode().getVisibleLeafIndex((TreeTableColumn)tc);
    }

    /** {@inheritDoc}  */
    @Override protected TreeTableColumn getVisibleLeafColumn(int index) {
        return getNode().getVisibleLeafColumn(index);
    }

    /** {@inheritDoc} */
    @Override protected boolean isControlEditable() {
        return getNode().isEditable();
    }

    /** {@inheritDoc}  */
    @Override protected void editCell(int row, TableColumnBase tc) {
        getNode().edit(row, (TreeTableColumn)tc);
    }

    /** {@inheritDoc}  */
    @Override protected ObservableList<TreeTableColumn<T,?>> getVisibleLeafColumns() {
        return getNode().getVisibleLeafColumns();
    }

    /** {@inheritDoc}  */
    @Override protected TablePositionBase<TreeTableColumn<T, ?>>
            getTablePosition(int row, TableColumnBase<TreeItem<T>, ?> tc) {
        return new TreeTablePosition(getNode(), row, (TreeTableColumn)tc);
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

    /**************************************************************************
     *                                                                        *
     * Tree-related implementation                                            *
     *                                                                        *
     *************************************************************************/

    /**
     * The next methods handle the left/right arrow input differently depending
     * on whether we are in row or cell selection.
     */
    private void rightArrowPressed() {
        if (getNode().getSelectionModel().isCellSelectionEnabled()) {
            if (isRTL()) {
                selectLeftCell();
            } else {
                selectRightCell();
            }
        } else {
            expandRow();
        }
    }

    private void leftArrowPressed() {
        if (getNode().getSelectionModel().isCellSelectionEnabled()) {
            if (isRTL()) {
                selectRightCell();
            } else {
                selectLeftCell();
            }
        } else {
            collapseRow();
        }
    }

    private void expandRow() {
        Callback<TreeItem<T>, Integer> getIndex = p -> getNode().getRow(p);
        TreeViewBehavior.expandRow(getNode().getSelectionModel(), getIndex);
    }

    private void expandAll() {
        TreeViewBehavior.expandAll(getNode().getRoot());
    }

    private void collapseRow() {
        TreeTableView<T> control = getNode();
        TreeViewBehavior.collapseRow(control.getSelectionModel(), control.getRoot(), control.isShowRoot());
    }
}

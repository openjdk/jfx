/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableFocusModel;
import javafx.scene.control.TablePositionBase;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.input.MouseButton;

public abstract class TableCellBehaviorBase<S, T, TC extends TableColumnBase<S, ?>, C extends IndexedCell<T>> extends CellBehaviorBase<C> {

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public TableCellBehaviorBase(C control) {
        super(control);
    }



    /**************************************************************************
     *                                                                        *
     * Abstract API                                                           *
     *                                                                        *
     *************************************************************************/

    protected abstract TableColumnBase<S, T> getTableColumn(); // getControl().getTableColumn()
    protected abstract int getItemCount();
    protected abstract TableSelectionModel<S> getSelectionModel();
    protected abstract TableFocusModel<S,TC> getFocusModel();
    protected abstract TablePositionBase getFocusedCell();
    protected abstract boolean isTableRowSelected(); // tableCell.getTableRow().isSelected()

    /**
     * Returns the position of the given table column in the visible leaf columns
     * list of the underlying control.
     */
    protected abstract int getVisibleLeafIndex(TableColumnBase<S,T> tc);

    protected abstract void focus(int row, TableColumnBase<S,T> tc); //fm.focus(new TreeTablePosition(tableView, row, tableColumn));



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/



    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    protected void doSelect(final double x, final double y, final MouseButton button,
                          final int clickCount, final boolean shiftDown, final boolean shortcutDown) {
        // Note that table.select will reset selection
        // for out of bounds indexes. So, need to check
        final C tableCell = getNode();

        // If the mouse event is not contained within this tableCell, then
        // we don't want to react to it.
        if (! tableCell.contains(x, y)) return;

        final Control tableView = getCellContainer();
        if (tableView == null) return;

        int count = getItemCount();
        if (tableCell.getIndex() >= count) return;

        TableSelectionModel<S> sm = getSelectionModel();
        if (sm == null) return;

        final boolean selected = isSelected();
        final int row = tableCell.getIndex();
        final int column = getColumn();
        final TableColumnBase<S,T> tableColumn = getTableColumn();

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TablePositionBase focusedCell = getFocusedCell();

        // if the user has clicked on the disclosure node, we do nothing other
        // than expand/collapse the tree item (if applicable). We do not do editing!
        if (handleDisclosureNode(x, y)) {
            return;
        }

        // if shift is down, and we don't already have the initial focus index
        // recorded, we record the focus index now so that subsequent shift+clicks
        // result in the correct selection occuring (whilst the focus index moves
        // about).
        if (shiftDown) {
            if (! hasNonDefaultAnchor(tableView)) {
                setAnchor(tableView, focusedCell, false);
            }
        } else {
            removeAnchor(tableView);
        }

        // we must update the table appropriately, and this is determined by
        // what modifiers the user held down as they released the mouse.
        if (button == MouseButton.PRIMARY || (button == MouseButton.SECONDARY && !selected)) {
            if (sm.getSelectionMode() == SelectionMode.SINGLE) {
                simpleSelect(button, clickCount, shortcutDown);
            } else {
                if (shortcutDown) {
                    if (selected) {
                        // we remove this row/cell from the current selection
                        sm.clearSelection(row, tableColumn);
                        fm.focus(row, tableColumn);
                    } else {
                        // We add this cell/row to the current selection
                        sm.select(row, tableColumn);
                    }
                } else if (shiftDown) {
                    // we add all cells/rows between the current selection focus and
                    // this cell/row (inclusive) to the current selection.
                    final TablePositionBase anchor = getAnchor(tableView, focusedCell);

                    final int anchorRow = anchor.getRow();

                    final boolean asc = anchorRow < row;

                    // clear selection, but maintain the anchor
                    sm.clearSelection();

                    // and then determine all row and columns which must be selected
                    int minRow = Math.min(anchorRow, row);
                    int maxRow = Math.max(anchorRow, row);
                    TableColumnBase<S,T> minColumn = anchor.getColumn() < column ? anchor.getTableColumn() : tableColumn;
                    TableColumnBase<S,T> maxColumn = anchor.getColumn() >= column ? anchor.getTableColumn() : tableColumn;

                    // and then perform the selection.
                    // RT-21444: We need to put the range in the correct
                    // order or else the last selected row will not be the
                    // last item in the selectedItems list of the selection
                    // model,
                    if (asc) {
                        sm.selectRange(minRow, minColumn, maxRow, maxColumn);
                    } else {
                        sm.selectRange(maxRow, minColumn, minRow, maxColumn);
                    }

                    // This line of code below was disabled as a fix for RT-30394.
                    // Unit tests were written, so if by disabling this code I
                    // have introduced regressions elsewhere, it is allowable to
                    // re-enable this code as tests will fail if it is done so
                    // without taking care of RT-30394 in an alternative manner.

                    // return selection back to the focus owner
                    // focus(anchor.getRow(), tableColumn);
                } else {
                    simpleSelect(button, clickCount, shortcutDown);
                }
            }
        }
    }

    protected void simpleSelect(MouseButton button, int clickCount, boolean shortcutDown) {
        final TableSelectionModel<S> sm = getSelectionModel();
        boolean isAlreadySelected;

        if (sm == null) {
            isAlreadySelected = false;
        } else {
            final int row = getNode().getIndex();
            final TableColumnBase<S,T> column = getTableColumn();
            isAlreadySelected = sm.isSelected(row, column);

            if (isAlreadySelected && shortcutDown) {
                sm.clearSelection(row, column);
                getFocusModel().focus(row, (TC) column);
                isAlreadySelected = false;
            } else {
                // we check if cell selection is enabled to fix RT-33897
                sm.clearAndSelect(row, column);
            }
        }

        handleClicks(button, clickCount, isAlreadySelected);
    }

    private int getColumn() {
        // this method will not be called if selection model is null
        if (getSelectionModel().isCellSelectionEnabled()) {
            TableColumnBase<S,T> tc = getTableColumn();
            return getVisibleLeafIndex(tc);
        }

        return -1;
    }

    @Override
    protected boolean isSelected() {
        TableSelectionModel<S> sm = getSelectionModel();
        if (sm == null) return false;

        if (sm.isCellSelectionEnabled()) {
            final C cell = getNode();
            return cell.isSelected();
        } else {
            return isTableRowSelected();
        }
    }
}

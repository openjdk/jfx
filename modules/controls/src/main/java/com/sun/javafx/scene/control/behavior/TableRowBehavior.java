/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TableRowBehavior<T> extends CellBehaviorBase<TableRow<T>> {

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    // To support touch devices, we have to slightly modify this behavior, such
    // that selection only happens on mouse release, if only minimal dragging
    // has occurred.
    private boolean latePress = false;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public TableRowBehavior(TableRow<T> control) {
        super(control, Collections.EMPTY_LIST);
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    @Override public void mousePressed(MouseEvent e) {
        // we only care about clicks to the right of the right-most column
        if (! isClickOutsideCellBounds(e.getX())) return;

        if (e.isSynthesized()) {
            latePress = true;
        } else {
            latePress  = getControl().isSelected();
            if (!latePress) {
                doSelect(e.getX(), e.getY(), e.getButton(), e.getClickCount(),
                         e.isShiftDown(), e.isShortcutDown());
            }
        }
    }

    @Override public void mouseReleased(MouseEvent e) {
        if (latePress) {
            latePress = false;
            doSelect(e.getX(), e.getY(), e.getButton(), e.getClickCount(),
                     e.isShiftDown(), e.isShortcutDown());
        }
    }

    @Override public void mouseDragged(MouseEvent e) {
        latePress = false;
    }

    @Override public void contextMenuRequested(ContextMenuEvent e) {
        doSelect(e.getX(), e.getY(), MouseButton.SECONDARY, 1, false, false);
    }



    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void doSelect(final double x, final double y, final MouseButton button,
                          final int clickCount, final boolean shiftDown,
                          final boolean shortcutDown) {
        final TableRow<T> tableRow = getControl();
        final TableView<T> table = tableRow.getTableView();
        if (table == null) return;
        final TableSelectionModel<T> sm = table.getSelectionModel();
        if (sm == null || sm.isCellSelectionEnabled()) return;
        
        final int index = getControl().getIndex();
        final boolean isAlreadySelected = sm.isSelected(index);
        if (clickCount == 1) {
            // we only care about clicks to the right of the right-most column
            if (! isClickOutsideCellBounds(x)) return;
            
            // In the case of clicking to the right of the rightmost
            // TreeTableCell, we should still support selection, so that
            // is what we are doing here.
            if (isAlreadySelected && shortcutDown) {
                sm.clearSelection(index);
            } else {
                if (shortcutDown) {
                    sm.select(tableRow.getIndex());
                } else if (shiftDown) {
                    // we add all rows between the current focus and
                    // this row (inclusive) to the current selection.
                    TablePositionBase anchor = TableCellBehavior.getAnchor(table, table.getFocusModel().getFocusedCell());
                    final int anchorRow = anchor.getRow();
                    final boolean asc = anchorRow < index;

                    // and then determine all row and columns which must be selected
                    int minRow = Math.min(anchorRow, index);
                    int maxRow = Math.max(anchorRow, index);

                    // To prevent RT-32119, we make a copy of the selected indices
                    // list first, so that we are not iterating and modifying it
                    // concurrently.
                    List<Integer> selectedIndices = new ArrayList<>(sm.getSelectedIndices());
                    for (int i = 0, max = selectedIndices.size(); i < max; i++) {
                        int selectedIndex = selectedIndices.get(i);
                        if (selectedIndex < minRow || selectedIndex > maxRow) {
                            sm.clearSelection(selectedIndex);
                        }
                    }

                    if (minRow == maxRow) {
                        // RT-32560: This prevents the anchor 'sticking' in
                        // the wrong place when a range is selected and then
                        // selection goes back to the anchor position.
                        // (Refer to the video in RT-32560 for more detail).
                        sm.select(minRow);
                    } else {
                        // RT-21444: We need to put the range in the correct
                        // order or else the last selected row will not be the
                        // last item in the selectedItems list of the selection
                        // model,
                        if (asc) {
                            sm.selectRange(minRow, maxRow + 1);
                        } else {
                            sm.selectRange(maxRow, minRow - 1);
                        }
                    }
                } else {
                    sm.clearAndSelect(tableRow.getIndex());
                }
            }
        }
    }

    private boolean isClickOutsideCellBounds(final double x) {
        // get width of all visible columns (we only care about clicks to the
        // right of the right-most column)
        final TableRow<T> tableRow = getControl();
        final TableView<T> table = tableRow.getTableView();
        if (table == null) return false;
        List<TableColumn<T, ?>> columns = table.getVisibleLeafColumns();
        double width = 0.0;
        for (int i = 0; i < columns.size(); i++) {
            width += columns.get(i).getWidth();
        }

        return x > width;
    }
}

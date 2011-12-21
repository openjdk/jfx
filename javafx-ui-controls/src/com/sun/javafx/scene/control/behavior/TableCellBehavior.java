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
package com.sun.javafx.scene.control.behavior;

import java.util.List;
import java.util.WeakHashMap;

import javafx.scene.control.*;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.input.MouseEvent;

/**
 */
public class TableCellBehavior extends CellBehaviorBase<TableCell> {
    // global map used to store the focus cell for a table view when it is first
    // shift-clicked. This allows for proper keyboard interactions, in particular
    // resolving RT-11446
    private static final WeakHashMap<TableView, TablePosition> map = new WeakHashMap<TableView, TablePosition>();
    
    static TablePosition getAnchor(TableView table) {
        TableViewFocusModel fm = table.getFocusModel();
        if (fm == null) return null;
        
        return map.containsKey(table) ? map.get(table) : fm.getFocusedCell();
    }
    
    static void setAnchor(TableView table, TablePosition anchor) {
        if (table != null && anchor == null) {
            map.remove(table);
        } else {
            map.put(table, anchor);
        }
    }
    
    // For RT-17456: have selection occur as fast as possible with mouse input.
    // The idea is (consistently with some native applications we've tested) to 
    // do the action as soon as you can. It takes a bit more coding but provides
    // the best feel:
    //  - when you click on a not-selected item, you can select immediately on press
    //  - when you click on a selected item, you need to wait whether DragDetected or Release comes first 
    private boolean selected = false;
    private boolean latePress = false;

    public TableCellBehavior(TableCell control) {
        super(control);
    }
    
    @Override public void mousePressed(MouseEvent event) {
        if (selected) {
            latePress = true;
            return;
        }
        
        doSelect(event);
    }
    
    @Override public void mouseReleased(MouseEvent event) {
        if (latePress) {
            latePress = false;
            doSelect(event);
        }
    }
    
    @Override public void mouseDragged(MouseEvent event) {
        latePress = false;
    }

    private void doSelect(MouseEvent e) {
        // Note that table.select will reset selection
        // for out of bounds indexes. So, need to check
        final TableCell tableCell = getControl();

        // If the mouse event is not contained within this tableCell, then
        // we don't want to react to it.
        if (! tableCell.contains(e.getX(), e.getY())) return;

        final TableView tableView = tableCell.getTableView();
        if (tableView == null) return;
        
        List<?> items = tableView.getItems();
        if (items == null || tableCell.getIndex() >= items.size()) return;

        TableView.TableViewSelectionModel sm = tableView.getSelectionModel();
        if (sm == null) return;

        final boolean selected = ! sm.isCellSelectionEnabled() ? tableCell.getTableRow().isSelected() : tableCell.isSelected();
        final int row = tableCell.getIndex();
        final int column = getColumn();
        final TableColumn<?,?> tableColumn = getControl().getTableColumn();

        TableViewFocusModel fm = tableView.getFocusModel();
        if (fm == null) return;
        
        // if shift is down, and we don't already have the initial focus index
        // recorded, we record the focus index now so that subsequent shift+clicks
        // result in the correct selection occuring (whilst the focus index moves
        // about).
        if (e.isShiftDown()) {
            if (! map.containsKey(tableView)) {
                map.put(tableView, fm.getFocusedCell());
            }
        } else {
            map.remove(tableView);
        }

        // we must update the table appropriately, and this is determined by
        // what modifiers the user held down as they released the mouse.
        if (sm.getSelectionMode() == SelectionMode.SINGLE) {
            simpleSelect(e);
        } else {
            if (e.isControlDown() || e.isMetaDown()) {
                if (selected) {
                    // we remove this row/cell from the current selection
                    sm.clearSelection(row, tableColumn);
                } else {
                    // We add this cell/row to the current selection
                    sm.select(row, tableColumn);
                }
            } else if (e.isShiftDown()) {
                // we add all cells/rows between the current selection focus and
                // this cell/row (inclusive) to the current selection.
                TablePosition focusedCell = map.containsKey(tableView) ? map.get(tableView) : fm.getFocusedCell();

                // and then determine all row and columns which must be selected
                int minRow = Math.min(focusedCell.getRow(), row);
                int maxRow = Math.max(focusedCell.getRow(), row);
                int minColumn = Math.min(focusedCell.getColumn(), column);
                int maxColumn = Math.max(focusedCell.getColumn(), column);

                // clear selection
                sm.clearSelection();

                // and then perform the selection
                if (sm.isCellSelectionEnabled()) {
                    for (int _row = minRow; _row <= maxRow; _row++) {
                        for (int _col = minColumn; _col <= maxColumn; _col++) {
                            sm.select(_row, tableView.getVisibleLeafColumn(_col));
                        }
                    }
                } else {
                    sm.selectRange(minRow, maxRow + 1);
                }

                // return selection back to the focus owner
                fm.focus(new TablePosition(tableView, row, tableColumn));
            } else {
                simpleSelect(e);
            }
        }
    }

    private void simpleSelect(MouseEvent e) {
        TableView tv = getControl().getTableView();
        TableView.TableViewSelectionModel sm = tv.getSelectionModel();
        int row = getControl().getIndex();
        boolean isAlreadySelected = sm.isSelected(row, getControl().getTableColumn());

        tv.getSelectionModel().clearAndSelect(row, getControl().getTableColumn());

        if (e.getClickCount() == 1 && isAlreadySelected) {
            tv.edit(row, getControl().getTableColumn());
        } else if (e.getClickCount() == 1) {
            // cancel editing
            tv.edit(-1, null);
        } else if (e.getClickCount() == 2 && getControl().isEditable()) {
            // edit at the specified row and column
            tv.edit(row, getControl().getTableColumn());
        }
    }

    private int getColumn() {
        if (getControl().getTableView().getSelectionModel().isCellSelectionEnabled()) {
            TableColumn tc = getControl().getTableColumn();
            TableView tv = getControl().getTableView();
            if (tv == null || tc == null) {
                return -1;
            }
            return tv.getVisibleLeafColumns().indexOf(tc);
        }

        return -1;
    }
}

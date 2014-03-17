/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
public abstract class TableCellBehaviorBase<S, T, TC extends TableColumnBase<S, ?>, C extends IndexedCell<T>> extends CellBehaviorBase<C> {
    
    /***************************************************************************
     *                                                                         *
     * Private static implementation                                           *
     *                                                                         *
     **************************************************************************/
    
    private static final String ANCHOR_PROPERTY_KEY = "table.anchor";
    
    static TablePositionBase getAnchor(Control table, TablePositionBase focusedCell) {
        return hasAnchor(table) ? 
                (TablePositionBase) table.getProperties().get(ANCHOR_PROPERTY_KEY) : 
                focusedCell;
    }
    
    static void setAnchor(Control table, TablePositionBase anchor) {
        if (table != null && anchor == null) {
            removeAnchor(table);
        } else {
            table.getProperties().put(ANCHOR_PROPERTY_KEY, anchor);
        }
    }
    
    static boolean hasAnchor(Control table) {
        return table.getProperties().get(ANCHOR_PROPERTY_KEY) != null;
    }
    
    static void removeAnchor(Control table) {
        table.getProperties().remove(ANCHOR_PROPERTY_KEY);
    }
    
    
    
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

    public TableCellBehaviorBase(C control) {
        super(control, Collections.EMPTY_LIST);
    }
    
    
    
    /**************************************************************************
     *                                                                        *
     * Abstract API                                                           *
     *                                                                        *  
     *************************************************************************/  
    
    abstract Control getTableControl(); // tableCell.getTreeTableView()
    abstract TableColumnBase<S, T> getTableColumn(); // getControl().getTableColumn()
    abstract int getItemCount();        // tableView.impl_getTreeItemCount()
    abstract TableSelectionModel<S> getSelectionModel();
    abstract TableFocusModel<S,TC> getFocusModel();
    abstract TablePositionBase getFocusedCell();
    abstract boolean isTableRowSelected(); // tableCell.getTreeTableRow().isSelected()
    abstract TableColumnBase<S,T> getVisibleLeafColumn(int index);

    /**
     * Returns the position of the given table column in the visible leaf columns
     * list of the underlying control.
     */
    protected abstract int getVisibleLeafIndex(TableColumnBase<S,T> tc);
    
    abstract void focus(int row, TableColumnBase<S,T> tc); //fm.focus(new TreeTablePosition(tableView, row, tableColumn));
    abstract void edit(int row, TableColumnBase<S,T> tc); 
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/    
    
    @Override public void mousePressed(MouseEvent e) {
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
                          final int clickCount, final boolean shiftDown, final boolean shortcutDown) {
        // Note that table.select will reset selection
        // for out of bounds indexes. So, need to check
        final C tableCell = getControl();

        // If the mouse event is not contained within this tableCell, then
        // we don't want to react to it.
        if (! tableCell.contains(x, y)) return;

        final Control tableView = getTableControl();
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
        boolean disclosureClicked = checkDisclosureNodeClick(x, y);
        if (disclosureClicked) {
            return;
        }
        
        // if shift is down, and we don't already have the initial focus index
        // recorded, we record the focus index now so that subsequent shift+clicks
        // result in the correct selection occuring (whilst the focus index moves
        // about).
        if (shiftDown) {
            if (! hasAnchor(tableView)) {
                setAnchor(tableView, focusedCell);
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
                    
                    // and then determine all row and columns which must be selected
                    int minRow = Math.min(anchor.getRow(), row);
                    int maxRow = Math.max(anchor.getRow(), row);
                    TableColumnBase<S,T> minColumn = anchor.getColumn() < column ? anchor.getTableColumn() : tableColumn;
                    TableColumnBase<S,T> maxColumn = anchor.getColumn() >= column ? anchor.getTableColumn() : tableColumn;

                    if (sm.isCellSelectionEnabled()) {
                        // clear selection, but maintain the anchor
                        sm.clearSelection();

                        // and then perform the selection
                        sm.selectRange(minRow, minColumn, maxRow, maxColumn);
                    } else {
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
        final int row = getControl().getIndex();
        final TableColumnBase<S,T> column = getTableColumn();
        boolean isAlreadySelected = sm.isSelected(row, sm.isCellSelectionEnabled() ? column : null);

        if (isAlreadySelected && shortcutDown) {
            sm.clearSelection(row, column);
            getFocusModel().focus(row, (TC) (sm.isCellSelectionEnabled() ? column : null));
            isAlreadySelected = false;
        } else {
            // we check if cell selection is enabled to fix RT-33897
            sm.clearAndSelect(row, sm.isCellSelectionEnabled() ? column : null);
        }

        // handle editing, which only occurs with the primary mouse button
        if (button == MouseButton.PRIMARY) {
            if (clickCount == 1 && isAlreadySelected) {
                edit(row, column);
            } else if (clickCount == 1) {
                // cancel editing
                edit(-1, null);
            } else if (clickCount == 2 && getControl().isEditable()) {
                // edit at the specified row and column
                edit(row, column);
            }
        }
    }

    protected boolean checkDisclosureNodeClick(double x, double y) {
        // by default we don't care about disclosure nodes
        return false;
    }

    private int getColumn() {
        if (getSelectionModel().isCellSelectionEnabled()) {
            TableColumnBase<S,T> tc = getTableColumn();
            return getVisibleLeafIndex(tc);
        }

        return -1;
    }

    private boolean isSelected() {
        TableSelectionModel<S> sm = getSelectionModel();
        if (sm == null) return false;

        if (sm.isCellSelectionEnabled()) {
            final C cell = getControl();
            return cell.isSelected();
        } else {
            return isTableRowSelected();
        }
    }
}

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

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeTableRowBehavior<T> extends CellBehaviorBase<TreeTableRow<T>> {

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

    public TreeTableRowBehavior(TreeTableRow<T> control) {
        super(control, Collections.EMPTY_LIST);
    }

    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    @Override public void mousePressed(MouseEvent event) {
        // we only care about clicks to the right of the right-most column
        if (! isClickOutsideCellBounds(event.getX())) return;

        if (event.isSynthesized()) {
            latePress = true;
        } else {
            latePress  = getControl().isSelected();
            if (!latePress) {
                doSelect(event);
            }
        }
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




    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void doSelect(MouseEvent e) {
        super.mouseReleased(e);
        
        if (e.getButton() != MouseButton.PRIMARY) return;
        
        TreeTableRow<T> treeTableRow = getControl();
        TreeItem<T> treeItem = treeTableRow.getTreeItem();
        if (treeItem == null) return;
        
        // if the user has clicked on the disclosure node, we do nothing other
        // than expand/collapse the tree item (if applicable). We do not do editing!
        Node disclosureNode = treeTableRow.getDisclosureNode();
        if (disclosureNode != null) {
            if (disclosureNode.getBoundsInParent().contains(e.getX(), e.getY())) {
                treeItem.setExpanded(! treeItem.isExpanded());
                return;
            }
        }

        TreeTableView<T> table = treeTableRow.getTreeTableView();
        if (table == null) return;
        final TreeTableViewSelectionModel<T> sm = table.getSelectionModel();
        if (sm == null || sm.isCellSelectionEnabled()) return;
        
        final int index = getControl().getIndex();
        final boolean isAlreadySelected = sm.isSelected(index);
        int clickCount = e.getClickCount();
        if (clickCount == 1) {
            // get width of all visible columns (we only care about clicks to the
            // right of the right-most column)
            List<TreeTableColumn<T, ?>> columns = getControl().getTreeTableView().getVisibleLeafColumns();
            double width = 0.0;
            for (int i = 0; i < columns.size(); i++) {
                width += columns.get(i).getWidth();
            }
            
            if (e.getX() < width) return;
            
            // In the case of clicking to the right of the rightmost
            // TreeTableCell, we should still support selection, so that
            // is what we are doing here.
            if (isAlreadySelected && e.isShortcutDown()) {
                sm.clearSelection(index);
            } else {
                if (e.isShortcutDown()) {
                    sm.select(treeTableRow.getIndex());
                } else if (e.isShiftDown()) {
                    // we add all rows between the current focus and
                    // this row (inclusive) to the current selection.
                    TablePositionBase anchor = TreeTableCellBehavior.getAnchor(table, table.getFocusModel().getFocusedCell());
                    final int anchorRow = anchor.getRow();
                    final boolean asc = anchorRow < index;

                    // and then determine all row and columns which must be selected
                    int minRow = Math.min(anchor.getRow(), index);
                    int maxRow = Math.max(anchor.getRow(), index);

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
                    sm.clearAndSelect(treeTableRow.getIndex());
                }
            }
        }
    }

    private boolean isClickOutsideCellBounds(final double x) {
        // get width of all visible columns (we only care about clicks to the
        // right of the right-most column)
        final TreeTableRow<T> tableRow = getControl();
        final TreeTableView<T> table = tableRow.getTreeTableView();
        if (table == null) return false;
        List<TreeTableColumn<T, ?>> columns = table.getVisibleLeafColumns();
        double width = 0.0;
        for (int i = 0; i < columns.size(); i++) {
            width += columns.get(i).getWidth();
        }

        return x > width;
    }
}

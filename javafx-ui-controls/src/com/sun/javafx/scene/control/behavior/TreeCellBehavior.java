/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.scene.control.Logging;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

/**
 */
public class TreeCellBehavior extends CellBehaviorBase<TreeCell<?>> {
    // global map used to store the focus index for a tree view when it is first
    // shift-clicked. This allows for proper keyboard interactions, in particular
    // resolving RT-11446
    private static final Map<TreeView, Integer> map = new HashMap<TreeView, Integer>();
    
    private ListChangeListener<Integer> selectedIndicesListener = new ListChangeListener<Integer>() {
        @Override public void onChanged(ListChangeListener.Change c) {
            while (c.next()) {
                // there are no selected items, so lets clear out the anchor
                if (c.getList().isEmpty()) {
                    map.remove(getControl().getTreeView());
                }
            }
        }
    };
    
    public TreeCellBehavior(final TreeCell control) {
        super(control);
        
//        // Fix for RT-16565
        // Currently this fix is commented out, on account of the performance
        // regression at RT-17926
//        control.getTreeView().selectionModelProperty().addListener(new ChangeListener<MultipleSelectionModel>() {
//            @Override public void changed(ObservableValue observable, MultipleSelectionModel oldValue, MultipleSelectionModel newValue) {
//                if (oldValue != null) {
//                    oldValue.getSelectedIndices().removeListener(selectedIndicesListener);
//                }
//                if (newValue != null) {
//                    newValue.getSelectedIndices().addListener(selectedIndicesListener);
//                }
//            }
//        });
//        if (control.getTreeView().getSelectionModel() != null) {
//            control.getTreeView().getSelectionModel().getSelectedIndices().addListener(selectedIndicesListener);
//        }
    }

    @Override public void mousePressed(MouseEvent e) {
        // we update the cell to point to the new tree node
        TreeCell<?> treeCell = getControl();
        TreeView treeView = treeCell.getTreeView();

        // If the mouse event is not contained within this TreeCell, then
        // we don't want to react to it.
        if (treeCell.isEmpty() || ! treeCell.contains(e.getX(), e.getY())) {
            final PlatformLogger logger = Logging.getControlsLogger();
            if (treeCell.isEmpty() && logger.isLoggable(PlatformLogger.WARNING)) {
//                logger.warning("TreeCell is empty, so mouse pressed event is "
//                        + "ignored. If you've created a custom cell and overridden "
//                        + "updateItem, be sure to call super.updateItem(item, empty)");
            }
            return;
        }

        int index = treeCell.getIndex();
        boolean selected = treeCell.isSelected();
        MultipleSelectionModel sm = treeView.getSelectionModel();
        if (sm == null) return;
        
        FocusModel fm = treeView.getFocusModel();
        if (fm == null) return;
        
        // if the user has clicked on the disclosure node, we do nothing other
        // than expand/collapse the tree item (if applicable). We do not do editing!
        Node disclosureNode = treeCell.getDisclosureNode();
        if (disclosureNode != null) {
            if (disclosureNode.getBoundsInParent().contains(e.getX(), e.getY())) {
                if (treeCell.getTreeItem() != null) {
                    treeCell.getTreeItem().setExpanded(! treeCell.getTreeItem().isExpanded());
                }
                return;
            }
        }
        
        // if shift is down, and we don't already have the initial focus index
        // recorded, we record the focus index now so that subsequent shift+clicks
        // result in the correct selection occuring (whilst the focus index moves
        // about).
        if (e.isShiftDown()) {
            if (! map.containsKey(treeView)) {
                map.put(treeView, fm.getFocusedIndex());
            }
        } else {
            map.remove(treeView);
        }

        if (sm.getSelectionMode() == SelectionMode.SINGLE) {
            simpleSelect(e);
        } else {
            if (e.isControlDown() || e.isMetaDown()) {
                if (selected) {
                    // we remove this row from the current selection
                    sm.clearSelection(index);
                } else {
                    // We add this row to the current selection
                    sm.select(index);
                }
            } else if (e.isShiftDown()) {
                // we add all rows between the current selection focus and
                // this row (inclusive) to the current selection.
                final int focusedIndex = map.containsKey(treeView) ? map.get(treeView) : fm.getFocusedIndex();

                // and then determine all row and columns which must be selected
                int minRow = Math.min(focusedIndex, index);
                int maxRow = Math.max(focusedIndex, index);

                // and then perform the selection
                sm.clearSelection();
                sm.selectRange(minRow, maxRow+1);
                
                fm.focus(index);
            } else {
                simpleSelect(e);
            }
        }
    }

    private void simpleSelect(MouseEvent e) {
        TreeView tv = getControl().getTreeView();
        int index = getControl().getIndex();
        MultipleSelectionModel sm = tv.getSelectionModel();
        boolean isAlreadySelected = sm.isSelected(index);

        tv.getSelectionModel().clearAndSelect(index);

        // handle editing
        if (e.getClickCount() == 1 && isAlreadySelected) {
            tv.edit(getControl().getTreeItem());
        } else if (e.getClickCount() == 1) {
            // cancel editing
            tv.edit(null);
        } else if (e.getClickCount() == 2 && getControl().isEditable()) {
            // try to expand/collapse tree item
            getControl().getTreeItem().setExpanded(! getControl().getTreeItem().isExpanded());
        }
    }
}

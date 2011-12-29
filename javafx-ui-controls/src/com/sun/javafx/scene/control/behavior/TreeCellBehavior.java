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
import java.util.WeakHashMap;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

/**
 */
public class TreeCellBehavior extends CellBehaviorBase<TreeCell<?>> {
    // global map used to store the focus index for a tree view when it is first
    // shift-clicked. This allows for proper keyboard interactions, in particular
    // resolving RT-11446
    private static final WeakHashMap<TreeView, Integer> map = new WeakHashMap<TreeView, Integer>();
    
    static int getAnchor(TreeView tree) {
        FocusModel fm = tree.getFocusModel();
        if (fm == null) return -1;
        
        return map.containsKey(tree) ? map.get(tree) : fm.getFocusedIndex();
    }
    
    static void setAnchor(TreeView tree, int anchor) {
        if (tree != null && anchor < 0) {
            map.remove(tree);
        } else {
            map.put(tree, anchor);
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
    
    public TreeCellBehavior(final TreeCell control) {
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
    
    private void doSelect(MouseEvent event) {
        // we update the cell to point to the new tree node
        TreeCell<?> treeCell = getControl();
        TreeView treeView = treeCell.getTreeView();
        if (treeView == null) return;

        // If the mouse event is not contained within this TreeCell, then
        // we don't want to react to it.
        if (treeCell.isEmpty() || ! treeCell.contains(event.getX(), event.getY())) {
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
            if (disclosureNode.getBoundsInParent().contains(event.getX(), event.getY())) {
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
        if (event.isShiftDown()) {
            if (! map.containsKey(treeView)) {
                map.put(treeView, fm.getFocusedIndex());
            }
        } else {
            map.remove(treeView);
        }

        if (sm.getSelectionMode() == SelectionMode.SINGLE) {
            simpleSelect(event);
        } else {
            if (event.isControlDown() || event.isMetaDown()) {
                if (selected) {
                    // we remove this row from the current selection
                    sm.clearSelection(index);
                } else {
                    // We add this row to the current selection
                    sm.select(index);
                }
            } else if (event.isShiftDown()) {
                // we add all rows between the current selection focus and
                // this row (inclusive) to the current selection.
                final int focusedIndex = getAnchor(treeView);

                // and then determine all row and columns which must be selected
                int minRow = Math.min(focusedIndex, index);
                int maxRow = Math.max(focusedIndex, index);

                // and then perform the selection
                sm.clearSelection();
                sm.selectRange(minRow, maxRow+1);
                
                fm.focus(index);
            } else {
                simpleSelect(event);
            }
        }
    }

    private void simpleSelect(MouseEvent e) {
        TreeView tv = getControl().getTreeView();
        int index = getControl().getIndex();
        MultipleSelectionModel sm = tv.getSelectionModel();
        boolean isAlreadySelected = sm.isSelected(index);

        tv.getSelectionModel().clearAndSelect(index);

        // handle editing, which only occurs with the primary mouse button
        if (e.isPrimaryButtonDown()) {
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
}

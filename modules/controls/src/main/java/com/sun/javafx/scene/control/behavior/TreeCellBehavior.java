/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.Logging;
import javafx.scene.Node;
import javafx.scene.control.FocusModel;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
public class TreeCellBehavior<T> extends CellBehaviorBase<TreeCell<T>> {

    /***************************************************************************
     *                                                                         *
     * Private static implementation                                           *
     *                                                                         *
     **************************************************************************/

    private static final String ANCHOR_PROPERTY_KEY = "list.anchor";

    static int getAnchor(TreeView<?> tree) {
        FocusModel<?> fm = tree.getFocusModel();
        if (fm == null) return -1;

        return hasAnchor(tree) ?
                (int)tree.getProperties().get(ANCHOR_PROPERTY_KEY) :
                fm.getFocusedIndex();
    }

    static void setAnchor(TreeView<?> tree, int anchor) {
        if (tree != null && anchor < 0) {
            removeAnchor(tree);
        } else {
            tree.getProperties().put(ANCHOR_PROPERTY_KEY, anchor);
        }
    }

    static boolean hasAnchor(TreeView<?> tree) {
        return tree.getProperties().get(ANCHOR_PROPERTY_KEY) != null;
    }

    static void removeAnchor(TreeView<?> tree) {
        tree.getProperties().remove(ANCHOR_PROPERTY_KEY);
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

    public TreeCellBehavior(final TreeCell<T> control) {
        super(control, Collections.EMPTY_LIST);
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    @Override public void mousePressed(MouseEvent event) {

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

    private void doSelect(MouseEvent event) {
        // we update the cell to point to the new tree node
        TreeCell<T> treeCell = getControl();
        TreeView<T> treeView = treeCell.getTreeView();
        if (treeView == null) return;

        // If the mouse event is not contained within this TreeCell, then
        // we don't want to react to it.
        if (treeCell.isEmpty() || ! treeCell.contains(event.getX(), event.getY())) {
            final PlatformLogger logger = Logging.getControlsLogger();
            if (treeCell.isEmpty() && logger.isLoggable(Level.WARNING)) {
//                logger.warning("TreeCell is empty, so mouse pressed event is "
//                        + "ignored. If you've created a custom cell and overridden "
//                        + "updateItem, be sure to call super.updateItem(item, empty)");
            }
            return;
        }

        int index = treeCell.getIndex();
        boolean selected = treeCell.isSelected();
        MultipleSelectionModel<TreeItem<T>> sm = treeView.getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = treeView.getFocusModel();
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
            if (! hasAnchor(treeView)) {
                setAnchor(treeView, fm.getFocusedIndex());
            }
        } else {
            removeAnchor(treeView);
        }

        MouseButton button = event.getButton();
        if (button == MouseButton.PRIMARY || (button == MouseButton.SECONDARY && !selected)) {
            if (sm.getSelectionMode() == SelectionMode.SINGLE) {
                simpleSelect(event);
            } else {
                if (event.isControlDown() || event.isMetaDown()) {
                    if (selected) {
                        // we remove this row from the current selection
                        sm.clearSelection(index);
                        fm.focus(index);
                    } else {
                        // We add this row to the current selection
                        sm.select(index);
                    }
                } else if (event.isShiftDown() && event.getClickCount() == 1) {
                    // we add all rows between the current selection focus and
                    // this row (inclusive) to the current selection.
                    final int focusedIndex = getAnchor(treeView);
                    final boolean asc = focusedIndex < index;

                    // and then determine all row and columns which must be selected
                    int minRow = Math.min(focusedIndex, index);
                    int maxRow = Math.max(focusedIndex, index);

                    // and then perform the selection
                    // We do this by deselecting the elements that are not in
                    // range, and then selecting all elements that are in range
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

                    // RT-21444: We need to put the range in in the correct
                    // order or else the last selected row will not be the
                    // last item in the selectedItems list of the selection
                    // model,
                    if (asc) {
                        sm.selectRange(minRow, maxRow + 1);
                    } else {
                        sm.selectRange(maxRow, minRow - 1);
                    }

                    fm.focus(index);
                } else {
                    simpleSelect(event);
                }
            }
        }
    }

    private void simpleSelect(MouseEvent e) {
        TreeView<T> tv = getControl().getTreeView();
        TreeItem<T> treeItem = getControl().getTreeItem();
        int index = getControl().getIndex();
        MultipleSelectionModel<TreeItem<T>> sm = tv.getSelectionModel();
        boolean isAlreadySelected = sm.isSelected(index);

        if (isAlreadySelected && (e.isControlDown() || e.isMetaDown())) {
            sm.clearSelection(index);
            tv.getFocusModel().focus(index);
            isAlreadySelected = false;
        } else {
            sm.clearAndSelect(index);
        }

        // handle editing, which only occurs with the primary mouse button
        if (e.getButton() == MouseButton.PRIMARY) {
            if (e.getClickCount() == 1 && isAlreadySelected) {
                tv.edit(treeItem);
            } else if (e.getClickCount() == 1) {
                // cancel editing
                tv.edit(null);
            } else if (e.getClickCount() == 2 && treeItem.isLeaf()) {
                // attempt to edit
                tv.edit(treeItem);
            } else if (e.getClickCount() % 2 == 0) {
                // try to expand/collapse branch tree item
                treeItem.setExpanded(! treeItem.isExpanded());
            }
        }
    }
}

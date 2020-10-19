/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TablePositionBase;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;

/**
 */
public class TreeTableCellBehavior<S,T> extends TableCellBehaviorBase<TreeItem<S>, T, TreeTableColumn<S, ?>, TreeTableCell<S,T>> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public TreeTableCellBehavior(TreeTableCell<S,T> control) {
        super(control);
    }



    /***************************************************************************
     *                                                                         *
     * Implement TableCellBehaviorBase Abstract API                            *
     *                                                                         *
     **************************************************************************/

    /** @{@inheritDoc} */
    @Override protected TreeTableView<S> getCellContainer() {
        return getNode().getTreeTableView();
    }

    /** @{@inheritDoc} */
    @Override protected TreeTableColumn<S,T> getTableColumn() {
        return getNode().getTableColumn();
    }

    /** @{@inheritDoc} */
    @Override protected int getItemCount() {
        return getCellContainer().getExpandedItemCount();
    }

    /** @{@inheritDoc} */
    @Override protected TreeTableView.TreeTableViewSelectionModel<S> getSelectionModel() {
        return getCellContainer().getSelectionModel();
    }

    /** @{@inheritDoc} */
    @Override protected TreeTableView.TreeTableViewFocusModel<S> getFocusModel() {
        return getCellContainer().getFocusModel();
    }

    /** @{@inheritDoc} */
    @Override protected TablePositionBase getFocusedCell() {
        return getCellContainer().getFocusModel().getFocusedCell();
    }

    /** @{@inheritDoc} */
    @Override protected boolean isTableRowSelected() {
        return getNode().getTreeTableRow().isSelected();
    }

    /** @{@inheritDoc} */
    @Override protected int getVisibleLeafIndex(TableColumnBase tc) {
        return getCellContainer().getVisibleLeafIndex((TreeTableColumn)tc);
    }

    /** @{@inheritDoc} */
    @Override protected void focus(int row, TableColumnBase tc) {
        getFocusModel().focus(row, (TreeTableColumn)tc);
    }

    /** @{@inheritDoc} */
    @Override protected void edit(TreeTableCell<S,T> cell) {
        if (cell == null) {
            getCellContainer().edit(-1, null);
        } else {
            getCellContainer().edit(cell.getIndex(), cell.getTableColumn());
        }
    }

    @Override protected boolean handleDisclosureNode(double x, double y) {
        final TreeItem<S> treeItem = getNode().getTreeTableRow().getTreeItem();

        final TreeTableView<S> treeTableView = getNode().getTreeTableView();
        final TreeTableColumn<S,T> column = getTableColumn();
        final TreeTableColumn<S,?> treeColumn = treeTableView.getTreeColumn() == null ?
                treeTableView.getVisibleLeafColumn(0) : treeTableView.getTreeColumn();

        if (column == treeColumn) {
            final Node disclosureNode = getNode().getTreeTableRow().getDisclosureNode();
          // fix JDK-8253597: check disclosure node for visibility along with existence
          if (disclosureNode != null && disclosureNode.isVisible()) {
                double startX = 0;
                for (TreeTableColumn<S,?> tc : treeTableView.getVisibleLeafColumns()) {
                    if (tc == treeColumn) break;
                    startX += tc.getWidth();
                }
                final double endX = disclosureNode.getBoundsInParent().getMaxX();
                if (x < (endX - startX)) {
                    if (treeItem != null) {
                        treeItem.setExpanded(!treeItem.isExpanded());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void handleClicks(MouseButton button, int clickCount, boolean isAlreadySelected) {
        // handle editing, which only occurs with the primary mouse button
        TreeItem<S> treeItem = getNode().getTreeTableRow().getTreeItem();
        if (button == MouseButton.PRIMARY) {
            if (clickCount == 1 && isAlreadySelected) {
                edit(getNode());
            } else if (clickCount == 1) {
                // cancel editing
                edit(null);
            } else if (clickCount == 2 && treeItem.isLeaf()) {
                // attempt to edit
                edit(getNode());
            } else if (clickCount % 2 == 0) {
                // try to expand/collapse branch tree item
                treeItem.setExpanded(! treeItem.isExpanded());
            }
        }
    }
}

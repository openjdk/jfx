/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treetableview;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyItem;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treetableview.HierarchyTreeTableCell.Column;
import java.util.List;
import java.util.Set;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import static javafx.geometry.Orientation.HORIZONTAL;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;

/**
 * Hierarchy panel controller based on the TreeTableView control.
 */
public class HierarchyTreeTableViewController extends AbstractHierarchyPanelController {

    @FXML
    protected TreeTableView<HierarchyItem> treeTableView;
    @FXML
    protected TreeTableColumn<HierarchyItem, HierarchyItem> classNameColumn;
    @FXML
    protected TreeTableColumn<HierarchyItem, HierarchyItem> displayInfoColumn;


    /*
     * Public
     */
    public HierarchyTreeTableViewController(EditorController editorController) {
        super(HierarchyTreeTableViewController.class.getResource("HierarchyTreeTableView.fxml"), editorController); //NOI18N
    }

    @Override
    public Control getPanelControl() {
        return treeTableView;
    }

    @Override
    public ObservableList<TreeItem<HierarchyItem>> getSelectedItems() {
        return treeTableView.getSelectionModel().getSelectedItems();
    }

    @Override
    protected void initializePanel() {
        assert treeTableView != null;
        super.initializePanel();
        // Initialize and configure tree table view
        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Class name column
        classNameColumn.setCellValueFactory(new CellValueFactoryCallback());
        classNameColumn.setCellFactory(new Callback<TreeTableColumn<HierarchyItem, HierarchyItem>, TreeTableCell<HierarchyItem, HierarchyItem>>() {
            @Override
            public TreeTableCell<HierarchyItem, HierarchyItem> call(TreeTableColumn<HierarchyItem, HierarchyItem> p) {
                return new HierarchyTreeTableCell<>(HierarchyTreeTableViewController.this, Column.CLASS_NAME);
            }
        });

        // Display info column
        displayInfoColumn.setCellValueFactory(new CellValueFactoryCallback());
        displayInfoColumn.setCellFactory(new Callback<TreeTableColumn<HierarchyItem, HierarchyItem>, TreeTableCell<HierarchyItem, HierarchyItem>>() {
            @Override
            public TreeTableCell<HierarchyItem, HierarchyItem> call(TreeTableColumn<HierarchyItem, HierarchyItem> p) {
                return new HierarchyTreeTableCell<>(HierarchyTreeTableViewController.this, Column.DISPLAY_INFO);
            }
        });

        // Row factory
        treeTableView.setRowFactory(new Callback<TreeTableView<HierarchyItem>, TreeTableRow<HierarchyItem>>() {
            @Override
            public TreeTableRow<HierarchyItem> call(TreeTableView<HierarchyItem> p) {
                return new HierarchyTreeTableRow<>(HierarchyTreeTableViewController.this);
            }
        });

        // We do not use the platform editing feature because 
        // editing is started on selection + simple click instead of double click
        treeTableView.setEditable(false);
        
        treeTableView.setPlaceholder(promptLabel);
    }

    @Override
    protected void updatePanel() {
        if (treeTableView != null) {
            // First update rootTreeItem + children TreeItems 
            updateTreeItems();
            // Then update the TreeTableView with the updated rootTreeItem
            stopListeningToTreeItemSelection();
            treeTableView.setRoot(rootTreeItem);
            startListeningToTreeItemSelection();
        }
    }

    @Override
    protected void clearSelection() {
        assert treeTableView != null;
        treeTableView.getSelectionModel().clearSelection();
    }

    @Override
    protected void select(final TreeItem<HierarchyItem> treeItem) {
        assert treeTableView != null;
        treeTableView.getSelectionModel().select(treeItem);
    }

    @Override
    public void scrollTo(final TreeItem<HierarchyItem> treeItem) {
        assert treeTableView != null;
        treeTableView.scrollTo(treeTableView.getRow(treeItem));
    }

    @Override
    public Cell<?> getCell(final TreeItem<?> treeItem) {
        assert treeTableView != null;
        final TreeTableRow<?> treeTableRow
                = HierarchyTreeTableViewUtils.getTreeTableRow(treeTableView, treeItem);
        return treeTableRow;
    }

    /**
     * Returns the height of the TreeTableView column header.
     *
     * @return the height of the TreeTableView column header
     */
    public double getColumnHeaderHeight() {
        final Set<Node> headers = treeTableView.lookupAll(".column-header"); //NOI18N
        // TreeTableView column headers share the same height
        assert !headers.isEmpty();
        final Node header = headers.iterator().next();
        final Bounds headerBounds = header.getLayoutBounds();
        return headerBounds.getHeight();
    }

    /**
     * Returns the Y coordinate of the panel content TOP. Used to define the
     * zone for auto scrolling.
     *
     * @return the Y coordinate of the panel content TOP
     */
    @Override
    public double getContentTopY() {
        final Bounds bounds = treeTableView.getLayoutBounds();
        final Point2D point = treeTableView.localToParent(bounds.getMinX(), bounds.getMinY());
        double headerHeight = getColumnHeaderHeight();
        return point.getY() + headerHeight;
    }

    /**
     * Returns the Y coordinate of the panel content BOTTOM. Used to define the
     * zone for auto scrolling.
     *
     * @return the Y coordinate of the panel content BOTTOM
     */
    @Override
    public double getContentBottomY() {
        final Bounds bounds = treeTableView.getLayoutBounds();
        final Point2D point = treeTableView.localToParent(bounds.getMinX(), bounds.getMinY());
        final double topY = point.getY();
        final double height = bounds.getHeight();
        final ScrollBar horizontalScrollBar = getScrollBar(HORIZONTAL);
        final double bottomY;
        if (horizontalScrollBar != null && horizontalScrollBar.isVisible()) {
            bottomY = topY + height - horizontalScrollBar.getLayoutBounds().getHeight();
        } else {
            bottomY = topY + height;
        }
        return bottomY;
    }

    @Override
    protected void startListeningToTreeItemSelection() {
        treeTableView.getSelectionModel().getSelectedItems().addListener(treeItemSelectionListener);
    }

    @Override
    protected void stopListeningToTreeItemSelection() {
        treeTableView.getSelectionModel().getSelectedItems().removeListener(treeItemSelectionListener);
    }

    @Override
    protected void startEditingDisplayInfo() {
        final List<TreeItem<HierarchyItem>> selectedTreeItems
                = treeTableView.getSelectionModel().getSelectedItems();
        if (selectedTreeItems.size() == 1) {
            final TreeItem<HierarchyItem> selectedTreeItem = selectedTreeItems.get(0);
            final HierarchyItem item = selectedTreeItem.getValue();
            final DisplayOption option = getDisplayOption();
            if (item != null && item.hasDisplayInfo(option)) {
                final TreeTableCell<?, ?> ttc = HierarchyTreeTableViewUtils.getTreeTableCell(
                        treeTableView, Column.DISPLAY_INFO, selectedTreeItem);
                assert ttc instanceof HierarchyTreeTableCell;
                final HierarchyTreeTableCell<?, ?> httc = (HierarchyTreeTableCell) ttc;
                httc.startEditingDisplayInfo();
            }
        }
    }

    /**
     * *************************************************************************
     * Parent ring
     * *************************************************************************
     */
    @Override
    public void clearBorderColor() {
        assert treeTableView != null;
        final Set<Node> cells = HierarchyTreeTableViewUtils.getTreeTableRows(treeTableView);
        assert cells != null;
        for (Node node : cells) {
            assert node instanceof Cell;
            clearBorderColor((Cell) node);
        }
    }

    @Override
    public void updateParentRing() {
        assert treeTableView != null;

        // Do not update parent ring while performing some operations 
        // like DND within the hierarchy panel
        if (isParentRingEnabled() == false) {
            return;
        }

        final Set<Node> treeTableRows = HierarchyTreeTableViewUtils.getTreeTableRows(treeTableView);
        final List<TreeItem<HierarchyItem>> selectedTreeItems = treeTableView.getSelectionModel().getSelectedItems();

        // First clear previous parent ring if any
        clearBorderColor();

        // Dirty selection
        for (TreeItem<?> selectedTreeItem : selectedTreeItems) {
            if (selectedTreeItem == null) {
                return;
            }
        }

        // Then update parent ring if selection is not empty
        if (!selectedTreeItems.isEmpty()) {

            // Single selection is ROOT TreeItem
            final TreeItem<HierarchyItem> treeItemRoot = treeTableView.getRoot();
            if (selectedTreeItems.size() == 1 && selectedTreeItems.get(0) == treeItemRoot) {
                final TreeTableRow<?> treeTableRowRoot
                        = HierarchyTreeTableViewUtils.getTreeTableRow(treeTableRows, treeItemRoot);
                if (treeTableRowRoot != null) {
                    treeTableRowRoot.getStyleClass().add(CELL_BORDER_TOP_RIGHT_BOTTOM_LEFT);
                }
                return;
            }

            int treeTableRowTopIndex, treeTableRowBottomIndex;

            // TOP TreeItem is the common parent TreeItem
            final TreeItem<HierarchyItem> treeItemTop
                    = HierarchyTreeTableViewUtils.getCommonParentTreeItem(selectedTreeItems);
            final TreeTableRow<?> treeTableRowTop
                    = HierarchyTreeTableViewUtils.getTreeTableRow(treeTableRows, treeItemTop);
            if (treeTableRowTop != null) {
                treeTableRowTop.getStyleClass().add(CELL_BORDER_TOP_RIGHT_LEFT);
                treeTableRowTopIndex = treeTableRowTop.getIndex();
            } else {
                treeTableRowTopIndex = 0;
            }

            // BOTTOM TreeItem is the last child of the common parent TreeItem
            final int size = treeItemTop.getChildren().size();
            assert size >= 1;
            final TreeItem<HierarchyItem> treeItemBottom = treeItemTop.getChildren().get(size - 1);
            final TreeTableRow<?> treeTableRowBottom = HierarchyTreeTableViewUtils.getTreeTableRow(treeTableRows, treeItemBottom);
            if (treeTableRowBottom != null) {
                treeTableRowBottom.getStyleClass().add(CELL_BORDER_RIGHT_BOTTOM_LEFT);
                treeTableRowBottomIndex = treeTableRowBottom.getIndex();
            } else {
                treeTableRowBottomIndex = treeTableRows.size() - 1;
            }

            // MIDDLE TreeItems
            for (Node node : treeTableRows) {
                assert node instanceof TreeTableRow;
                final TreeTableRow<?> treeTableRow = (TreeTableRow) node;
                final int index = treeTableRow.getIndex();
                if (index > treeTableRowTopIndex && index < treeTableRowBottomIndex) {
                    treeTableRow.getStyleClass().add(CELL_BORDER_RIGHT_LEFT);
                }
            }
        }
    }

    /**
     * *************************************************************************
     * Static inner class
     * *************************************************************************
     */
    private static class CellValueFactoryCallback implements
            Callback<TreeTableColumn.CellDataFeatures<HierarchyItem, HierarchyItem>, ObservableValue<HierarchyItem>> {

        @Override
        public ObservableValue<HierarchyItem> call(TreeTableColumn.CellDataFeatures<HierarchyItem, HierarchyItem> p) {
            final HierarchyItem value = p.getValue().getValue();
            // The returned value is used as the parameter of the cell factory updateItem method
            return new ReadOnlyObjectWrapper<>(value);
        }
    }
}

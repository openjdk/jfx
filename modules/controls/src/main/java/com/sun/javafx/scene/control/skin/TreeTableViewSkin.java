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

package com.sun.javafx.scene.control.skin;

import com.sun.javafx.collections.NonIterableChange;
import com.sun.javafx.scene.control.ReadOnlyUnbackedObservableList;

import javafx.event.WeakEventHandler;
import javafx.scene.accessibility.Attribute;
import javafx.scene.accessibility.Role;
import javafx.scene.control.*;

import com.sun.javafx.scene.control.behavior.TreeTableViewBehavior;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

public class TreeTableViewSkin<S> extends TableViewSkinBase<S, TreeItem<S>, TreeTableView<S>, TreeTableViewBehavior<S>, TreeTableRow<S>, TreeTableColumn<S,?>> {
    
    public TreeTableViewSkin(final TreeTableView<S> treeTableView) {
        super(treeTableView, new TreeTableViewBehavior<S>(treeTableView));
        
        this.treeTableView = treeTableView;
        this.tableBackingList = new TreeTableViewBackingList<S>(treeTableView);
        this.tableBackingListProperty = new SimpleObjectProperty<ObservableList<TreeItem<S>>>(tableBackingList);
        
        flow.setFixedCellSize(treeTableView.getFixedCellSize());
        
        super.init(treeTableView);
        
        setRoot(getSkinnable().getRoot());

        EventHandler<MouseEvent> ml = event -> {
            // RT-15127: cancel editing on scroll. This is a bit extreme
            // (we are cancelling editing on touching the scrollbars).
            // This can be improved at a later date.
            if (treeTableView.getEditingCell() != null) {
                treeTableView.edit(-1, null);
            }

            // This ensures that the table maintains the focus, even when the vbar
            // and hbar controls inside the flow are clicked. Without this, the
            // focus border will not be shown when the user interacts with the
            // scrollbars, and more importantly, keyboard navigation won't be
            // available to the user.
            if (treeTableView.isFocusTraversable()) {
                treeTableView.requestFocus();
            }
        };
        flow.getVbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);
        flow.getHbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);

        // init the behavior 'closures'
        TreeTableViewBehavior<S> behavior = getBehavior();
        behavior.setOnFocusPreviousRow(() -> { onFocusPreviousCell(); });
        behavior.setOnFocusNextRow(() -> { onFocusNextCell(); });
        behavior.setOnMoveToFirstCell(() -> { onMoveToFirstCell(); });
        behavior.setOnMoveToLastCell(() -> { onMoveToLastCell(); });
        behavior.setOnScrollPageDown(isFocusDriven -> onScrollPageDown(isFocusDriven));
        behavior.setOnScrollPageUp(isFocusDriven -> onScrollPageUp(isFocusDriven));
        behavior.setOnSelectPreviousRow(() -> { onSelectPreviousCell(); });
        behavior.setOnSelectNextRow(() -> { onSelectNextCell(); });
        behavior.setOnSelectLeftCell(() -> { onSelectLeftCell(); });
        behavior.setOnSelectRightCell(() -> { onSelectRightCell(); });
        
        registerChangeListener(treeTableView.rootProperty(), "ROOT");
        registerChangeListener(treeTableView.showRootProperty(), "SHOW_ROOT");
        registerChangeListener(treeTableView.rowFactoryProperty(), "ROW_FACTORY");
        registerChangeListener(treeTableView.expandedItemCountProperty(), "TREE_ITEM_COUNT");
        registerChangeListener(treeTableView.fixedCellSizeProperty(), "FIXED_CELL_SIZE");
    }

    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        
        if ("ROOT".equals(p)) {
            setRoot(getSkinnable().getRoot());
        } else if ("SHOW_ROOT".equals(p)) {
            // if we turn off showing the root, then we must ensure the root
            // is expanded - otherwise we end up with no visible items in
            // the tree.
            if (! getSkinnable().isShowRoot() && getRoot() != null) {
                 getRoot().setExpanded(true);
            }
            // update the item count in the flow and behavior instances
            updateRowCount();
        } else if ("ROW_FACTORY".equals(p)) {
            flow.recreateCells();
        } else if ("TREE_ITEM_COUNT".equals(p)) {
            rowCountDirty = true;
        } else if ("FIXED_CELL_SIZE".equals(p)) {
            flow.setFixedCellSize(getSkinnable().getFixedCellSize());
        }
    }
    
    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/
    
    
    
    /***************************************************************************
     *                                                                         *
     * Internal Fields                                                         *
     *                                                                         *
     **************************************************************************/

    private TreeTableViewBackingList<S> tableBackingList;
    private ObjectProperty<ObservableList<TreeItem<S>>> tableBackingListProperty;
    private TreeTableView<S> treeTableView;
    private WeakReference<TreeItem<S>> weakRootRef;
    
    private EventHandler<TreeItem.TreeModificationEvent<S>> rootListener = e -> {
        if (e.wasAdded() && e.wasRemoved() && e.getAddedSize() == e.getRemovedSize()) {
            // Fix for RT-14842, where the children of a TreeItem were changing,
            // but because the overall item count was staying the same, there was
            // no event being fired to the skin to be informed that the items
            // had changed. So, here we just watch for the case where the number
            // of items being added is equal to the number of items being removed.
            rowCountDirty = true;
            getSkinnable().requestLayout();
        } else if (e.getEventType().equals(TreeItem.valueChangedEvent())) {
            // Fix for RT-14971 and RT-15338.
            needCellsRebuilt = true;
            getSkinnable().requestLayout();
        } else {
            // Fix for RT-20090. We are checking to see if the event coming
            // from the TreeItem root is an event where the count has changed.
            EventType<?> eventType = e.getEventType();
            while (eventType != null) {
                if (eventType.equals(TreeItem.<S>expandedItemCountChangeEvent())) {
                    rowCountDirty = true;
                    getSkinnable().requestLayout();
                    break;
                }
                eventType = eventType.getSuperType();
            }
        }
    };
    
    private WeakEventHandler<TreeModificationEvent<S>> weakRootListener;
            
    
//    private WeakReference<TreeItem> weakRoot;
    private TreeItem<S> getRoot() {
        return weakRootRef == null ? null : weakRootRef.get();
    }
    private void setRoot(TreeItem<S> newRoot) {
        if (getRoot() != null && weakRootListener != null) {
            getRoot().removeEventHandler(TreeItem.<S>treeNotificationEvent(), weakRootListener);
        }
        weakRootRef = new WeakReference<>(newRoot);
        if (getRoot() != null) {
            weakRootListener = new WeakEventHandler<>(rootListener);
            getRoot().addEventHandler(TreeItem.<S>treeNotificationEvent(), weakRootListener);
        }
        
        updateRowCount();
    }
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/  
    
    /** {@inheritDoc} */
    @Override protected ObservableList<TreeTableColumn<S, ?>> getVisibleLeafColumns() {
        return treeTableView.getVisibleLeafColumns();
    }
    
    @Override protected int getVisibleLeafIndex(TreeTableColumn<S,?> tc) {
        return treeTableView.getVisibleLeafIndex(tc);
    }

    @Override protected TreeTableColumn<S,?> getVisibleLeafColumn(int col) {
        return treeTableView.getVisibleLeafColumn(col);
    }

    /** {@inheritDoc} */
    @Override protected TreeTableView.TreeTableViewFocusModel<S> getFocusModel() {
        return treeTableView.getFocusModel();
    }
    
    /** {@inheritDoc} */
    @Override protected TreeTablePosition<S, ?> getFocusedCell() {
        return treeTableView.getFocusModel().getFocusedCell();
    }

    /** {@inheritDoc} */
	@Override protected TableSelectionModel<TreeItem<S>> getSelectionModel() {
        return treeTableView.getSelectionModel();
    }

    /** {@inheritDoc} */
    @Override protected ObjectProperty<Callback<TreeTableView<S>, TreeTableRow<S>>> rowFactoryProperty() {
        return treeTableView.rowFactoryProperty();
    }

    /** {@inheritDoc} */
    @Override protected ObjectProperty<Node> placeholderProperty() {
        return treeTableView.placeholderProperty();
    }

    /** {@inheritDoc} */
    @Override protected ObjectProperty<ObservableList<TreeItem<S>>> itemsProperty() {
        return tableBackingListProperty;
    }

    /** {@inheritDoc} */
    @Override protected ObservableList<TreeTableColumn<S,?>> getColumns() {
        return treeTableView.getColumns();
    }
    
    /** {@inheritDoc} */
    @Override protected BooleanProperty tableMenuButtonVisibleProperty() {
        return treeTableView.tableMenuButtonVisibleProperty();
    }

    /** {@inheritDoc} */
    @Override protected ObjectProperty<Callback<ResizeFeaturesBase, Boolean>> columnResizePolicyProperty() {
        // TODO Ugly!
        return (ObjectProperty<Callback<ResizeFeaturesBase, Boolean>>) (Object) treeTableView.columnResizePolicyProperty();
    }

    /** {@inheritDoc} */
    @Override protected ObservableList<TreeTableColumn<S,?>> getSortOrder() {
        return treeTableView.getSortOrder();
    }
    
    @Override protected boolean resizeColumn(TreeTableColumn<S,?> tc, double delta) {
        return treeTableView.resizeColumn(tc, delta);
    }

    /*
     * FIXME: Naive implementation ahead
     * Attempts to resize column based on the pref width of all items contained
     * in this column. This can be potentially very expensive if the number of
     * rows is large.
     */
    @Override protected void resizeColumnToFitContent(TreeTableColumn<S,?> tc, int maxRows) {
        final TreeTableColumn col = tc;
        List<?> items = itemsProperty().get();
        if (items == null || items.isEmpty()) return;
    
        Callback cellFactory = col.getCellFactory();
        if (cellFactory == null) return;
    
        TreeTableCell<S,?> cell = (TreeTableCell) cellFactory.call(col);
        if (cell == null) return;
        
        // set this property to tell the TableCell we want to know its actual
        // preferred width, not the width of the associated TableColumnBase
        cell.getProperties().put(TableCellSkin.DEFER_TO_PARENT_PREF_WIDTH, Boolean.TRUE);
        
        // determine cell padding
        double padding = 10;
        Node n = cell.getSkin() == null ? null : cell.getSkin().getNode();
        if (n instanceof Region) {
            Region r = (Region) n;
            padding = r.snappedLeftInset() + r.snappedRightInset();
        } 
        
        TreeTableRow<S> treeTableRow = new TreeTableRow<>();
        treeTableRow.updateTreeTableView(treeTableView);
        
        int rows = maxRows == -1 ? items.size() : Math.min(items.size(), maxRows);
        double maxWidth = 0;
        for (int row = 0; row < rows; row++) {
            treeTableRow.updateIndex(row);
            treeTableRow.updateTreeItem(treeTableView.getTreeItem(row));
            
            cell.updateTreeTableColumn(col);
            cell.updateTreeTableView(treeTableView);
            cell.updateTreeTableRow(treeTableRow);
            cell.updateIndex(row);
            
            if ((cell.getText() != null && !cell.getText().isEmpty()) || cell.getGraphic() != null) {
                getChildren().add(cell);
                cell.applyCss();
                
                double w = cell.prefWidth(-1);
                
                maxWidth = Math.max(maxWidth, w);
                getChildren().remove(cell);
            }
        }

        // dispose of the cell to prevent it retaining listeners (see RT-31015)
        cell.updateIndex(-1);

        // RT-36855 - take into account the column header text / graphic widths.
        // Magic 10 is to allow for sort arrow to appear without text truncation.
        TableColumnHeader header = getTableHeaderRow().getColumnHeaderFor(tc);
        double headerTextWidth = Utils.computeTextWidth(header.label.getFont(), tc.getText(), -1);
        Node graphic = header.label.getGraphic();
        double headerGraphicWidth = graphic == null ? 0 : graphic.prefWidth(-1) + header.label.getGraphicTextGap();
        double headerWidth = headerTextWidth + headerGraphicWidth + 10 + header.snappedLeftInset() + header.snappedRightInset();
        maxWidth = Math.max(maxWidth, headerWidth);
        
        // RT-23486
        maxWidth += padding;
        if(treeTableView.getColumnResizePolicy() == TreeTableView.CONSTRAINED_RESIZE_POLICY) {
            maxWidth = Math.max(maxWidth, col.getWidth());
        }

        col.impl_setWidth(maxWidth);
    }
    
    /** {@inheritDoc} */
    @Override public int getItemCount() {
        return treeTableView.getExpandedItemCount();
    }
    
    /** {@inheritDoc} */
    @Override public TreeTableRow<S> createCell() {
        TreeTableRow<S> cell;

        if (treeTableView.getRowFactory() != null) {
            cell = treeTableView.getRowFactory().call(treeTableView);
        } else {
            cell = new TreeTableRow<S>();
        }

        // If there is no disclosure node, then add one of my own
        if (cell.getDisclosureNode() == null) {
            final StackPane disclosureNode = new StackPane() {
                @Override public Object accGetAttribute(Attribute attribute, Object... parameters) {
                    switch (attribute) {
                        case ROLE: return Role.DISCLOSURE_NODE;
                        default: return super.accGetAttribute(attribute, parameters);
                    }
                }
            };
            disclosureNode.getStyleClass().setAll("tree-disclosure-node");
            disclosureNode.setMouseTransparent(true);

            final StackPane disclosureNodeArrow = new StackPane();
            disclosureNodeArrow.getStyleClass().setAll("arrow");
            disclosureNode.getChildren().add(disclosureNodeArrow);

            cell.setDisclosureNode(disclosureNode);
        }

        cell.updateTreeTableView(treeTableView);
        return cell;
    }

    @Override protected void horizontalScroll() {
        super.horizontalScroll();
        if (getSkinnable().getFixedCellSize() > 0) {
            flow.requestCellLayout();
        }
    }

    @Override
    public Object accGetAttribute(Attribute attribute, Object... parameters) {
        switch (attribute) {
            case ROW_AT_INDEX: {
                final int rowIndex = (Integer)parameters[0];
                return rowIndex < 0 ? null : flow.getPrivateCell(rowIndex);
            }
            case SELECTED_CELLS: {
                List<Node> selection = new ArrayList<>();
                TreeTableView.TreeTableViewSelectionModel<S> sm = getSkinnable().getSelectionModel();
                for (TreeTablePosition<S,?> pos : sm.getSelectedCells()) {
                    TreeTableRow<S> row = flow.getPrivateCell(pos.getRow());
                    if (row != null) selection.add(row);
                }
                return FXCollections.observableArrayList(selection);
            }

            case FOCUS_ITEM: // TableViewSkinBase
            case CELL_AT_ROW_COLUMN: // TableViewSkinBase
            case COLUMN_AT_INDEX: // TableViewSkinBase
            case HEADER: // TableViewSkinBase
            case VERTICAL_SCROLLBAR: // TableViewSkinBase
            case HORIZONTAL_SCROLLBAR: // TableViewSkinBase
            default: return super.accGetAttribute(attribute, parameters);
        }
    }
    
    
    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/    
    

    
    
    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/
    
    @Override protected void updateRowCount() {
        updatePlaceholderRegionVisibility();

        tableBackingList.resetSize();
        
        int oldCount = flow.getCellCount();
        int newCount = getItemCount();
        
        // if this is not called even when the count is the same, we get a 
        // memory leak in VirtualFlow.sheet.children. This can probably be 
        // optimised in the future when time permits.
        flow.setCellCount(newCount);
        
        if (forceCellRecreate) {
            needCellsRecreated = true;
            forceCellRecreate = false;
        } else if (newCount != oldCount) {
            needCellsRebuilt = true;
        } else {
            needCellsReconfigured = true;
        }
    }

    /**
     * A simple read only list structure that maps into the TreeTableView tree
     * structure.
     */
    private static class TreeTableViewBackingList<S> extends ReadOnlyUnbackedObservableList<TreeItem<S>> {
        private final TreeTableView<S> treeTable;
        
        private int size = -1;
        
        TreeTableViewBackingList(TreeTableView<S> treeTable) {
            this.treeTable = treeTable;
        }
        
        void resetSize() {
            int oldSize = size;
            size = -1;
            
            // TODO we can certainly make this better....but it may not really matter
            callObservers(new NonIterableChange.GenericAddRemoveChange<TreeItem<S>>(
                    0, oldSize, FXCollections.<TreeItem<S>>emptyObservableList(), this));
        }
        
        @Override public TreeItem<S> get(int i) {
            return treeTable.getTreeItem(i);
        }

        @Override public int size() {
            if (size == -1) {
                size = treeTable.getExpandedItemCount();
            }
            return size;
        }
    }
}

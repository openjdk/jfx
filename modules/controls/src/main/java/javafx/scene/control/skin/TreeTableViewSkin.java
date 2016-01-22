/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import com.sun.javafx.collections.NonIterableChange;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.ReadOnlyUnbackedObservableList;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.event.WeakEventHandler;
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
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

/**
 * Default skin implementation for the {@link TreeTableView} control.
 *
 * @see TreeTableView
 * @since 9
 */
public class TreeTableViewSkin<T> extends TableViewSkinBase<T, TreeItem<T>, TreeTableView<T>, TreeTableRow<T>, TreeTableColumn<T,?>> {

    /***************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    private TreeTableViewBackingList<T> tableBackingList;
    private ObjectProperty<ObservableList<TreeItem<T>>> tableBackingListProperty;
    private WeakReference<TreeItem<T>> weakRootRef;
    private final TreeTableViewBehavior<T>  behavior;



    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private EventHandler<TreeItem.TreeModificationEvent<T>> rootListener = e -> {
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
                if (eventType.equals(TreeItem.<T>expandedItemCountChangeEvent())) {
                    rowCountDirty = true;
                    getSkinnable().requestLayout();
                    break;
                }
                eventType = eventType.getSuperType();
            }
        }

        // fix for RT-37853
        getSkinnable().edit(-1, null);
    };

    private WeakEventHandler<TreeModificationEvent<T>> weakRootListener;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new TreeTableViewSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public TreeTableViewSkin(final TreeTableView<T> control) {
        super(control);

        // install default input map for the TreeTableView control
        behavior = new TreeTableViewBehavior<>(control);
//        control.setInputMap(behavior.getInputMap());

        flow.setFixedCellSize(control.getFixedCellSize());
        flow.setCellFactory(flow -> createCell());

        setRoot(getSkinnable().getRoot());

        EventHandler<MouseEvent> ml = event -> {
            // RT-15127: cancel editing on scroll. This is a bit extreme
            // (we are cancelling editing on touching the scrollbars).
            // This can be improved at a later date.
            if (control.getEditingCell() != null) {
                control.edit(-1, null);
            }

            // This ensures that the table maintains the focus, even when the vbar
            // and hbar controls inside the flow are clicked. Without this, the
            // focus border will not be shown when the user interacts with the
            // scrollbars, and more importantly, keyboard navigation won't be
            // available to the user.
            if (control.isFocusTraversable()) {
                control.requestFocus();
            }
        };
        flow.getVbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);
        flow.getHbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);

        // init the behavior 'closures'
        behavior.setOnFocusPreviousRow(() -> onFocusPreviousCell());
        behavior.setOnFocusNextRow(() -> onFocusNextCell());
        behavior.setOnMoveToFirstCell(() -> onMoveToFirstCell());
        behavior.setOnMoveToLastCell(() -> onMoveToLastCell());
        behavior.setOnScrollPageDown(isFocusDriven -> onScrollPageDown(isFocusDriven));
        behavior.setOnScrollPageUp(isFocusDriven -> onScrollPageUp(isFocusDriven));
        behavior.setOnSelectPreviousRow(() -> onSelectPreviousCell());
        behavior.setOnSelectNextRow(() -> onSelectNextCell());
        behavior.setOnSelectLeftCell(() -> onSelectLeftCell());
        behavior.setOnSelectRightCell(() -> onSelectRightCell());

        registerChangeListener(control.rootProperty(), e -> {
            // fix for RT-37853
            getSkinnable().edit(-1, null);

            setRoot(getSkinnable().getRoot());
        });
        registerChangeListener(control.showRootProperty(), e -> {
            // if we turn off showing the root, then we must ensure the root
            // is expanded - otherwise we end up with no visible items in
            // the tree.
            if (! getSkinnable().isShowRoot() && getRoot() != null) {
                getRoot().setExpanded(true);
            }
            // update the item count in the flow and behavior instances
            updateRowCount();
        });
        registerChangeListener(control.rowFactoryProperty(), e -> flow.recreateCells());
        registerChangeListener(control.expandedItemCountProperty(), e -> rowCountDirty = true);
        registerChangeListener(control.fixedCellSizeProperty(), e -> flow.setFixedCellSize(getSkinnable().getFixedCellSize()));
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case ROW_AT_INDEX: {
                final int rowIndex = (Integer)parameters[0];
                return rowIndex < 0 ? null : flow.getPrivateCell(rowIndex);
            }
            case SELECTED_ITEMS: {
                List<Node> selection = new ArrayList<>();
                TreeTableView.TreeTableViewSelectionModel<T> sm = getSkinnable().getSelectionModel();
                for (TreeTablePosition<T,?> pos : sm.getSelectedCells()) {
                    TreeTableRow<T> row = flow.getPrivateCell(pos.getRow());
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
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    @Override
    protected void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case SHOW_ITEM: {
                Node item = (Node)parameters[0];
                if (item instanceof TreeTableCell) {
                    @SuppressWarnings("unchecked")
                    TreeTableCell<T, ?> cell = (TreeTableCell<T, ?>)item;
                    flow.scrollTo(cell.getIndex());
                }
                break;
            }
            case SET_SELECTED_ITEMS: {
                @SuppressWarnings("unchecked")
                ObservableList<Node> items = (ObservableList<Node>)parameters[0];
                if (items != null) {
                    TreeTableView.TreeTableViewSelectionModel<T> sm = getSkinnable().getSelectionModel();
                    if (sm != null) {
                        sm.clearSelection();
                        for (Node item : items) {
                            if (item instanceof TreeTableCell) {
                                @SuppressWarnings("unchecked")
                                TreeTableCell<T, ?> cell = (TreeTableCell<T, ?>)item;
                                sm.select(cell.getIndex(), cell.getTableColumn());
                            }
                        }
                    }
                }
                break;
            }
            default: super.executeAccessibleAction(action, parameters);
        }
    }



    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    private TreeTableRow<T> createCell() {
        TreeTableRow<T> cell;

        TreeTableView<T> treeTableView = getSkinnable();
        if (treeTableView.getRowFactory() != null) {
            cell = treeTableView.getRowFactory().call(treeTableView);
        } else {
            cell = new TreeTableRow<T>();
        }

        // If there is no disclosure node, then add one of my own
        if (cell.getDisclosureNode() == null) {
            final StackPane disclosureNode = new StackPane();
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

    private TreeItem<T> getRoot() {
        return weakRootRef == null ? null : weakRootRef.get();
    }
    private void setRoot(TreeItem<T> newRoot) {
        if (getRoot() != null && weakRootListener != null) {
            getRoot().removeEventHandler(TreeItem.<T>treeNotificationEvent(), weakRootListener);
        }
        weakRootRef = new WeakReference<>(newRoot);
        if (getRoot() != null) {
            weakRootListener = new WeakEventHandler<>(rootListener);
            getRoot().addEventHandler(TreeItem.<T>treeNotificationEvent(), weakRootListener);
        }

        updateRowCount();
    }

    /** {@inheritDoc} */
    @Override ObservableList<TreeTableColumn<T, ?>> getVisibleLeafColumns() {
        return getSkinnable().getVisibleLeafColumns();
    }

    @Override int getVisibleLeafIndex(TreeTableColumn<T,?> tc) {
        return getSkinnable().getVisibleLeafIndex(tc);
    }

    @Override TreeTableColumn<T,?> getVisibleLeafColumn(int col) {
        return getSkinnable().getVisibleLeafColumn(col);
    }

    /** {@inheritDoc} */
    @Override TreeTableView.TreeTableViewFocusModel<T> getFocusModel() {
        return getSkinnable().getFocusModel();
    }

    /** {@inheritDoc} */
    @Override TreeTablePosition<T, ?> getFocusedCell() {
        return getSkinnable().getFocusModel().getFocusedCell();
    }

    /** {@inheritDoc} */
    @Override TableSelectionModel<TreeItem<T>> getSelectionModel() {
        return getSkinnable().getSelectionModel();
    }

    /** {@inheritDoc} */
    @Override ObjectProperty<Callback<TreeTableView<T>, TreeTableRow<T>>> rowFactoryProperty() {
        return getSkinnable().rowFactoryProperty();
    }

    /** {@inheritDoc} */
    @Override ObjectProperty<Node> placeholderProperty() {
        return getSkinnable().placeholderProperty();
    }

    /** {@inheritDoc} */
    @Override ObjectProperty<ObservableList<TreeItem<T>>> itemsProperty() {
        if (tableBackingListProperty == null) {
            this.tableBackingList = new TreeTableViewBackingList<>(getSkinnable());
            this.tableBackingListProperty = new SimpleObjectProperty<>(tableBackingList);
        }
        return tableBackingListProperty;
    }

    /** {@inheritDoc} */
    @Override ObservableList<TreeTableColumn<T,?>> getColumns() {
        return getSkinnable().getColumns();
    }

    /** {@inheritDoc} */
    @Override BooleanProperty tableMenuButtonVisibleProperty() {
        return getSkinnable().tableMenuButtonVisibleProperty();
    }

    /** {@inheritDoc} */
    @Override ObjectProperty<Callback<ResizeFeaturesBase, Boolean>> columnResizePolicyProperty() {
        return (ObjectProperty<Callback<ResizeFeaturesBase, Boolean>>) (Object) getSkinnable().columnResizePolicyProperty();
    }

    /** {@inheritDoc} */
    @Override ObservableList<TreeTableColumn<T,?>> getSortOrder() {
        return getSkinnable().getSortOrder();
    }

    @Override boolean resizeColumn(TreeTableColumn<T,?> tc, double delta) {
        return getSkinnable().resizeColumn(tc, delta);
    }

    /*
     * FIXME: Naive implementation ahead
     * Attempts to resize column based on the pref width of all items contained
     * in this column. This can be potentially very expensive if the number of
     * rows is large.
     */
    @Override void resizeColumnToFitContent(TreeTableColumn<T,?> tc, int maxRows) {
        final TreeTableColumn col = tc;
        List<?> items = itemsProperty().get();
        if (items == null || items.isEmpty()) return;

        Callback cellFactory = col.getCellFactory();
        if (cellFactory == null) return;

        TreeTableCell<T,?> cell = (TreeTableCell) cellFactory.call(col);
        if (cell == null) return;

        // set this property to tell the TableCell we want to know its actual
        // preferred width, not the width of the associated TableColumnBase
        cell.getProperties().put(Properties.DEFER_TO_PARENT_PREF_WIDTH, Boolean.TRUE);

        // determine cell padding
        double padding = 10;
        Node n = cell.getSkin() == null ? null : cell.getSkin().getNode();
        if (n instanceof Region) {
            Region r = (Region) n;
            padding = r.snappedLeftInset() + r.snappedRightInset();
        }

        final TreeTableView<T> treeTableView = getSkinnable();

        TreeTableRow<T> treeTableRow = new TreeTableRow<>();
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
    @Override int getItemCount() {
        return getSkinnable().getExpandedItemCount();
    }

    /** {@inheritDoc} */
    @Override void horizontalScroll() {
        super.horizontalScroll();
        if (getSkinnable().getFixedCellSize() > 0) {
            flow.requestCellLayout();
        }
    }

    /** {@inheritDoc} */
    @Override void updateRowCount() {
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



    /***************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/

    /**
     * A simple read only list structure that maps into the TreeTableView tree
     * structure.
     */
    private static class TreeTableViewBackingList<T> extends ReadOnlyUnbackedObservableList<TreeItem<T>> {
        private final TreeTableView<T> treeTable;

        private int size = -1;

        TreeTableViewBackingList(TreeTableView<T> treeTable) {
            this.treeTable = treeTable;
        }

        void resetSize() {
            int oldSize = size;
            size = -1;

            // TODO we can certainly make this better....but it may not really matter
            callObservers(new NonIterableChange.GenericAddRemoveChange<TreeItem<T>>(
                    0, oldSize, FXCollections.<TreeItem<T>>emptyObservableList(), this));
        }

        @Override public TreeItem<T> get(int i) {
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

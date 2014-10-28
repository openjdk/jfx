/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.event.WeakEventHandler;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.scene.control.behavior.TreeViewBehavior;

public class TreeViewSkin<T> extends VirtualContainerBase<TreeView<T>, TreeViewBehavior<T>, TreeCell<T>> {

    // RT-34744 : IS_PANNABLE will be false unless
    // com.sun.javafx.scene.control.skin.TreeViewSkin.pannable
    // is set to true. This is done in order to make TreeView functional
    // on embedded systems with touch screens which do not generate scroll
    // events for touch drag gestures.
    private static final boolean IS_PANNABLE =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("com.sun.javafx.scene.control.skin.TreeViewSkin.pannable"));


    public TreeViewSkin(final TreeView treeView) {
        super(treeView, new TreeViewBehavior(treeView));

        // init the VirtualFlow
        flow.setPannable(IS_PANNABLE);
        flow.setCreateCell(flow1 -> TreeViewSkin.this.createCell());
        flow.setFixedCellSize(treeView.getFixedCellSize());
        getChildren().add(flow);
        
        setRoot(getSkinnable().getRoot());
        
        EventHandler<MouseEvent> ml = event -> {
            // RT-15127: cancel editing on scroll. This is a bit extreme
            // (we are cancelling editing on touching the scrollbars).
            // This can be improved at a later date.
            if (treeView.getEditingItem() != null) {
                treeView.edit(null);
            }

            // This ensures that the tree maintains the focus, even when the vbar
            // and hbar controls inside the flow are clicked. Without this, the
            // focus border will not be shown when the user interacts with the
            // scrollbars, and more importantly, keyboard navigation won't be
            // available to the user.
            if (treeView.isFocusTraversable()) {
                treeView.requestFocus();
            }
        };
        flow.getVbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);
        flow.getHbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);

        // init the behavior 'closures'
        getBehavior().setOnFocusPreviousRow(() -> { onFocusPreviousCell(); });
        getBehavior().setOnFocusNextRow(() -> { onFocusNextCell(); });
        getBehavior().setOnMoveToFirstCell(() -> { onMoveToFirstCell(); });
        getBehavior().setOnMoveToLastCell(() -> { onMoveToLastCell(); });
        getBehavior().setOnScrollPageDown(isFocusDriven -> onScrollPageDown(isFocusDriven));
        getBehavior().setOnScrollPageUp(isFocusDriven -> onScrollPageUp(isFocusDriven));
        getBehavior().setOnSelectPreviousRow(() -> { onSelectPreviousCell(); });
        getBehavior().setOnSelectNextRow(() -> { onSelectNextCell(); });

        registerChangeListener(treeView.rootProperty(), "ROOT");
        registerChangeListener(treeView.showRootProperty(), "SHOW_ROOT");
        registerChangeListener(treeView.cellFactoryProperty(), "CELL_FACTORY");
        registerChangeListener(treeView.fixedCellSizeProperty(), "FIXED_CELL_SIZE");
        
        updateRowCount();
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
        } else if ("CELL_FACTORY".equals(p)) {
            flow.recreateCells();
        } else if ("FIXED_CELL_SIZE".equals(p)) {
            flow.setFixedCellSize(getSkinnable().getFixedCellSize());
        }
    }
    
//    private boolean needItemCountUpdate = false;
    private boolean needCellsRebuilt = true;
    private boolean needCellsReconfigured = false;
    
    private EventHandler<TreeModificationEvent<T>> rootListener = e -> {
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
        getSkinnable().edit(null);
    };
    
    private WeakEventHandler<TreeModificationEvent<T>> weakRootListener;
            
    
    private WeakReference<TreeItem<T>> weakRoot;
    private TreeItem<T> getRoot() {
        return weakRoot == null ? null : weakRoot.get();
    }
    private void setRoot(TreeItem<T> newRoot) {
        if (getRoot() != null && weakRootListener != null) {
            getRoot().removeEventHandler(TreeItem.<T>treeNotificationEvent(), weakRootListener);
        }
        weakRoot = new WeakReference<>(newRoot);
        if (getRoot() != null) {
            weakRootListener = new WeakEventHandler<>(rootListener);
            getRoot().addEventHandler(TreeItem.<T>treeNotificationEvent(), weakRootListener);
        }

        updateRowCount();
    }

    @Override public int getItemCount() {
        return getSkinnable().getExpandedItemCount();
    }

    @Override protected void updateRowCount() {
//        int oldCount = flow.getCellCount();
        int newCount = getItemCount();
        
        // if this is not called even when the count is the same, we get a 
        // memory leak in VirtualFlow.sheet.children. This can probably be 
        // optimised in the future when time permits.
        flow.setCellCount(newCount);

        // Ideally we would be more nuanced here, toggling a cheaper needs* 
        // field, but if we do we hit issues such as those identified in 
        // RT-27852, where the expended item count of the new root equals the
        // EIC of the old root, which would lead to the visuals not updating
        // properly. 
        needCellsRebuilt = true;
        getSkinnable().requestLayout();
    }

    @Override public TreeCell<T> createCell() {
        final TreeCell<T> cell;
        if (getSkinnable().getCellFactory() != null) {
            cell = getSkinnable().getCellFactory().call(getSkinnable());
        } else {
            cell = createDefaultCellImpl();
        }

        // If there is no disclosure node, then add one of my own
        if (cell.getDisclosureNode() == null) {
            final StackPane disclosureNode = new StackPane();

            /* This code is intentionally commented.
             * Currently as it stands it does provided any functionality and interferes
             * with TreeView. The VO cursor move over the DISCLOSURE_NODE instead of the 
             * tree item itself. This is possibly caused by the order of item's children 
             * (the Labeled and the disclosure node).
             */
//            final StackPane disclosureNode = new StackPane() {
//                @Override protected Object accGetAttribute(Attribute attribute, Object... parameters) {
//                    switch (attribute) {
//                        case ROLE: return Role.DISCLOSURE_NODE;
//                        default: return super.accGetAttribute(attribute, parameters);
//                    }
//                }
//            };
            disclosureNode.getStyleClass().setAll("tree-disclosure-node");

            final StackPane disclosureNodeArrow = new StackPane();
            disclosureNodeArrow.getStyleClass().setAll("arrow");
            disclosureNode.getChildren().add(disclosureNodeArrow);

            cell.setDisclosureNode(disclosureNode);
        }

        cell.updateTreeView(getSkinnable());

        return cell;
    }

    // Note: This is a copy/paste of javafx.scene.control.cell.DefaultTreeCell,
    // which is package-protected
    private TreeCell<T> createDefaultCellImpl() {
        return new TreeCell<T>() {
            private HBox hbox;
            
            private WeakReference<TreeItem<T>> treeItemRef;
            
            private InvalidationListener treeItemGraphicListener = observable -> {
                updateDisplay(getItem(), isEmpty());
            };
            
            private InvalidationListener treeItemListener = new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    TreeItem<T> oldTreeItem = treeItemRef == null ? null : treeItemRef.get();
                    if (oldTreeItem != null) {
                        oldTreeItem.graphicProperty().removeListener(weakTreeItemGraphicListener);
                    }
                    
                    TreeItem<T> newTreeItem = getTreeItem();
                    if (newTreeItem != null) {
                        newTreeItem.graphicProperty().addListener(weakTreeItemGraphicListener);
                        treeItemRef = new WeakReference<TreeItem<T>>(newTreeItem);
                    }
                }
            };
            
            private WeakInvalidationListener weakTreeItemGraphicListener =
                    new WeakInvalidationListener(treeItemGraphicListener);
            
            private WeakInvalidationListener weakTreeItemListener =
                    new WeakInvalidationListener(treeItemListener);
            
            {
                treeItemProperty().addListener(weakTreeItemListener);
                
                if (getTreeItem() != null) {
                    getTreeItem().graphicProperty().addListener(weakTreeItemGraphicListener);
                }
            }
            
            private void updateDisplay(T item, boolean empty) {
                if (item == null || empty) {
                    hbox = null;
                    setText(null);
                    setGraphic(null);
                } else {
                    // update the graphic if one is set in the TreeItem
                    TreeItem<T> treeItem = getTreeItem();
                    Node graphic = treeItem == null ? null : treeItem.getGraphic();
                    if (graphic != null) {
                        if (item instanceof Node) {
                            setText(null);
                            
                            // the item is a Node, and the graphic exists, so 
                            // we must insert both into an HBox and present that
                            // to the user (see RT-15910)
                            if (hbox == null) {
                                hbox = new HBox(3);
                            }
                            hbox.getChildren().setAll(graphic, (Node)item);
                            setGraphic(hbox);
                        } else {
                            hbox = null;
                            setText(item.toString());
                            setGraphic(graphic);
                        }
                    } else {
                        hbox = null;
                        if (item instanceof Node) {
                            setText(null);
                            setGraphic((Node)item);
                        } else {
                            setText(item.toString());
                            setGraphic(null);
                        }
                    }
                }                
            }
            
            @Override public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                updateDisplay(item, empty);
            }
        };
    }
    
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return computePrefHeight(-1, topInset, rightInset, bottomInset, leftInset) * 0.618033987;
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return 400;
    }

    @Override
    protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        super.layoutChildren(x, y, w, h);
        
        if (needCellsRebuilt) {
            flow.rebuildCells();
        } else if (needCellsReconfigured) {
            flow.reconfigureCells();
        } 
        
        needCellsRebuilt = false;
        needCellsReconfigured = false;
        
        flow.resizeRelocate(x, y, w, h);
    }
    
    private void onFocusPreviousCell() {
        FocusModel<TreeItem<T>> fm = getSkinnable().getFocusModel();
        if (fm == null) return;
        flow.show(fm.getFocusedIndex());
    }

    private void onFocusNextCell() {
        FocusModel<TreeItem<T>> fm = getSkinnable().getFocusModel();
        if (fm == null) return;
        flow.show(fm.getFocusedIndex());
    }

    private void onSelectPreviousCell() {
        int row = getSkinnable().getSelectionModel().getSelectedIndex();
        flow.show(row);
    }

    private void onSelectNextCell() {
        int row = getSkinnable().getSelectionModel().getSelectedIndex();
        flow.show(row);
    }

    private void onMoveToFirstCell() {
        flow.show(0);
        flow.setPosition(0);
    }

    private void onMoveToLastCell() {
        flow.show(getItemCount());
        flow.setPosition(1);
    }

    /**
     * Function used to scroll the container down by one 'page'.
     */
    public int onScrollPageDown(boolean isFocusDriven) {
        TreeCell<T> lastVisibleCell = flow.getLastVisibleCellWithinViewPort();
        if (lastVisibleCell == null) return -1;

        final SelectionModel<TreeItem<T>> sm = getSkinnable().getSelectionModel();
        final FocusModel<TreeItem<T>> fm = getSkinnable().getFocusModel();
        if (sm == null || fm == null) return -1;

        int lastVisibleCellIndex = lastVisibleCell.getIndex();

        // isSelected represents focus OR selection
        boolean isSelected = false;
        if (isFocusDriven) {
            isSelected = lastVisibleCell.isFocused() || fm.isFocused(lastVisibleCellIndex);
        } else {
            isSelected = lastVisibleCell.isSelected() || sm.isSelected(lastVisibleCellIndex);
        }

        if (isSelected) {
            boolean isLeadIndex = (isFocusDriven && fm.getFocusedIndex() == lastVisibleCellIndex)
                    || (! isFocusDriven && sm.getSelectedIndex() == lastVisibleCellIndex);

            if (isLeadIndex) {
                // if the last visible cell is selected, we want to shift that cell up
                // to be the top-most cell, or at least as far to the top as we can go.
                flow.showAsFirst(lastVisibleCell);

                TreeCell<T> newLastVisibleCell = flow.getLastVisibleCellWithinViewPort();
                lastVisibleCell = newLastVisibleCell == null ? lastVisibleCell : newLastVisibleCell;
            }
        } else {
            // if the selection is not on the 'bottom' most cell, we firstly move
            // the selection down to that, without scrolling the contents, so
            // this is a no-op
        }

        int newSelectionIndex = lastVisibleCell.getIndex();
        flow.show(lastVisibleCell);
        return newSelectionIndex;
    }

    /**
     * Function used to scroll the container up by one 'page'.
     */
    public int onScrollPageUp(boolean isFocusDriven) {
        TreeCell<T> firstVisibleCell = flow.getFirstVisibleCellWithinViewPort();
        if (firstVisibleCell == null) return -1;

        final SelectionModel<TreeItem<T>> sm = getSkinnable().getSelectionModel();
        final FocusModel<TreeItem<T>> fm = getSkinnable().getFocusModel();
        if (sm == null || fm == null) return -1;

        int firstVisibleCellIndex = firstVisibleCell.getIndex();

        // isSelected represents focus OR selection
        boolean isSelected = false;
        if (isFocusDriven) {
            isSelected = firstVisibleCell.isFocused() || fm.isFocused(firstVisibleCellIndex);
        } else {
            isSelected = firstVisibleCell.isSelected() || sm.isSelected(firstVisibleCellIndex);
        }

        if (isSelected) {
            boolean isLeadIndex = (isFocusDriven && fm.getFocusedIndex() == firstVisibleCellIndex)
                    || (! isFocusDriven && sm.getSelectedIndex() == firstVisibleCellIndex);

            if (isLeadIndex) {
                // if the first visible cell is selected, we want to shift that cell down
                // to be the bottom-most cell, or at least as far to the bottom as we can go.
                flow.showAsLast(firstVisibleCell);

                TreeCell<T> newFirstVisibleCell = flow.getFirstVisibleCellWithinViewPort();
                firstVisibleCell = newFirstVisibleCell == null ? firstVisibleCell : newFirstVisibleCell;
            }
        } else {
            // if the selection is not on the 'top' most cell, we firstly move
            // the selection up to that, without scrolling the contents, so
            // this is a no-op
        }

        int newSelectionIndex = firstVisibleCell.getIndex();
        flow.show(firstVisibleCell);
        return newSelectionIndex;
    }

    @Override
    protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case FOCUS_ITEM: {
                FocusModel<?> fm = getSkinnable().getFocusModel();
                int focusedIndex = fm.getFocusedIndex();
                if (focusedIndex == -1) {
                    if (getItemCount() > 0) {
                        focusedIndex = 0;
                    } else {
                        return null;
                    }
                }
                return flow.getPrivateCell(focusedIndex);
            }
            case ROW_AT_INDEX: {
                final int rowIndex = (Integer)parameters[0];
                return rowIndex < 0 ? null : flow.getPrivateCell(rowIndex);
            }
            case SELECTED_ITEMS: {
                MultipleSelectionModel<TreeItem<T>> sm = getSkinnable().getSelectionModel();
                ObservableList<Integer> indices = sm.getSelectedIndices();
                List<Node> selection = new ArrayList<>(indices.size());
                for (int i : indices) {
                    TreeCell<T> row = flow.getPrivateCell(i);
                    if (row != null) selection.add(row);
                }
                return FXCollections.observableArrayList(selection);
            }
            case VERTICAL_SCROLLBAR: return flow.getVbar();
            case HORIZONTAL_SCROLLBAR: return flow.getHbar();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    @Override
    protected void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case SHOW_ITEM: {
                Node item = (Node)parameters[0];
                if (item instanceof TreeCell) {
                    @SuppressWarnings("unchecked")
                    TreeCell<T> cell = (TreeCell<T>)item;
                    flow.show(cell.getIndex());
                }
                break;
            }
            case SET_SELECTED_ITEMS: {
                @SuppressWarnings("unchecked")
                ObservableList<Node> items = (ObservableList<Node>)parameters[0];
                if (items != null) {
                    MultipleSelectionModel<TreeItem<T>> sm = getSkinnable().getSelectionModel();
                    if (sm != null) {
                        sm.clearSelection();
                        for (Node item : items) {
                            if (item instanceof TreeCell) {
                                @SuppressWarnings("unchecked")
                                TreeCell<T> cell = (TreeCell<T>)item;
                                sm.select(cell.getIndex());
                            }
                        }
                    }
                }
                break;
            }
            default: super.executeAccessibleAction(action, parameters);
        }
    }
}

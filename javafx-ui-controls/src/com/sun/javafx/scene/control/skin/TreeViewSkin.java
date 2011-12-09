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

package com.sun.javafx.scene.control.skin;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import com.sun.javafx.scene.control.WeakEventHandler;
import com.sun.javafx.scene.control.behavior.TreeViewBehavior;
import java.lang.ref.WeakReference;
import javafx.scene.control.*;
import javafx.util.Callback;

public class TreeViewSkin<T> extends VirtualContainerBase<TreeView<T>, TreeViewBehavior<T>, TreeCell<T>> {

    public TreeViewSkin(final TreeView treeView) {
        super(treeView, new TreeViewBehavior(treeView));

        // init the VirtualFlow
        flow = new VirtualFlow();
        flow.setPannable(false);
        flow.setFocusTraversable(getSkinnable().isFocusTraversable());
        flow.setCreateCell(new Callback<VirtualFlow, TreeCell>() {
            @Override public TreeCell call(VirtualFlow flow) {
                return TreeViewSkin.this.createCell();
            }
        });
        getChildren().add(flow);

        setRoot(getSkinnable().getRoot());
        
        EventHandler<MouseEvent> ml = new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) { 
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
                treeView.requestFocus(); }
        };
        flow.getVbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);
        flow.getHbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);

        // init the behavior 'closures'
        getBehavior().setOnFocusPreviousRow(new Runnable() {
            @Override public void run() { onFocusPreviousCell(); }
        });
        getBehavior().setOnFocusNextRow(new Runnable() {
            @Override public void run() { onFocusNextCell(); }
        });
        getBehavior().setOnMoveToFirstCell(new Runnable() {
            @Override public void run() { onMoveToFirstCell(); }
        });
        getBehavior().setOnMoveToLastCell(new Runnable() {
            @Override public void run() { onMoveToLastCell(); }
        });
        getBehavior().setOnScrollPageDown(new Callback<Void, Integer>() {
            @Override public Integer call(Void param) { return onScrollPageDown(); }
        });
        getBehavior().setOnScrollPageUp(new Callback<Void, Integer>() {
            @Override public Integer call(Void param) { return onScrollPageUp(); }
        });
        getBehavior().setOnSelectPreviousRow(new Runnable() {
            @Override public void run() { onSelectPreviousCell(); }
        });
        getBehavior().setOnSelectNextRow(new Runnable() {
            @Override public void run() { onSelectNextCell(); }
        });

        registerChangeListener(treeView.rootProperty(), "ROOT");
        registerChangeListener(treeView.showRootProperty(), "SHOW_ROOT");
        registerChangeListener(treeView.cellFactoryProperty(), "CELL_FACTORY");
        registerChangeListener(treeView.impl_treeItemCountProperty(), "TREE_ITEM_COUNT");
        registerChangeListener(treeView.focusTraversableProperty(), "FOCUS_TRAVERSABLE");
        
        updateItemCount();
    }
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        
        if (p == "ROOT") {
            setRoot(getSkinnable().getRoot());
        } else if (p == "SHOW_ROOT") {
            // if we turn off showing the root, then we must ensure the root
            // is expanded - otherwise we end up with no visible items in
            // the tree.
            if (! getSkinnable().isShowRoot() && getRoot() != null) {
                 getRoot().setExpanded(true);

                 // update the item count in the flow and behavior instances
                updateItemCount();
            }
        } else if (p == "CELL_FACTORY") {
            flow.recreateCells();
        } else if (p == "TREE_ITEM_COUNT") {
            updateItemCount();
        } else if (p == "FOCUS_TRAVERSABLE") {
            flow.setFocusTraversable(getSkinnable().isFocusTraversable());
        }
    }
    
    private boolean needItemCountUpdate = false;
    private boolean needCellsRecreated = false;
    
    private EventHandler<TreeModificationEvent> rootListener = new EventHandler<TreeModificationEvent>() {
        @Override public void handle(TreeModificationEvent e) {
            if (e.wasAdded() && e.wasRemoved() && e.getAddedSize() == e.getRemovedSize()) {
                // Fix for RT-14842, where the children of a TreeItem were changing,
                // but because the overall item count was staying the same, there was 
                // no event being fired to the skin to be informed that the items
                // had changed. So, here we just watch for the case where the number
                // of items being added is equal to the number of items being removed.
                needItemCountUpdate = true;
                requestLayout();
            } else if ((e.wasAdded() && e.getAddedSize() == e.getTreeItem().getChildren().size())
                    || (e.wasRemoved() && e.getTreeItem().getChildren().isEmpty())) {
                
                // Fix for RT-14850, where a TreeItem with no children would not
                // show a disclosure node when a child was added. Similarly, if
                // a TreeItem with children had all children removed, the 
                // disclosure node would not be removed. This forces the cells
                // to repaint themselves.
                // The alternative approach is to have many listeners, one for
                // each TreeCell, watching the children list of the assigned 
                // TreeItem. This is likely much less efficient, so for now this
                // approach seems the most sane.
                needCellsRecreated = true;
                requestLayout();
            } else if (e.getEventType().equals(TreeItem.valueChangedEvent())) {
                // Fix for RT-14971 and RT-15338. 
                needCellsRecreated = true;
                requestLayout();
            }
        }
    };
    
    private WeakEventHandler weakRootListener;
            
    
    private WeakReference<TreeItem> weakRoot;
    private TreeItem getRoot() {
        return weakRoot == null ? null : weakRoot.get();
    }
    private void setRoot(TreeItem newRoot) {
        if (getRoot() != null && weakRootListener != null) {
            getRoot().removeEventHandler(TreeItem.<T>treeNotificationEvent(), weakRootListener);
        }
        weakRoot = new WeakReference<TreeItem>(newRoot);
        if (getRoot() != null) {
            weakRootListener = new WeakEventHandler(getRoot(), TreeItem.<T>treeNotificationEvent(), rootListener);
            getRoot().addEventHandler(TreeItem.<T>treeNotificationEvent(), weakRootListener);
        }
    }

    @Override public int getItemCount() {
        return getSkinnable().impl_getTreeItemCount();
    }

    private void updateItemCount() {
        // we're about to recreate all cells - but before that we detach them
        // from the TreeView, such that their listeners can be uninstalled.
        // If we don't do this, we start to get multiple events firing when
        // properties on the TreeView trigger listeners in the cells.
        for (int i = 0; i < flow.cells.size(); i++) {
            ((TreeCell)flow.cells.get(i)).updateTreeView(null);
        }
        
        int oldCount = flow.getCellCount();
        int newCount = getItemCount();
        
        // if this is not called even when the count is the same, we get a 
        // memory leak in VirtualFlow.sheet.children. This can probably be 
        // optimised in the future when time permits.
        flow.setCellCount(newCount);
        
        if (newCount != oldCount) {
            flow.recreateCells();
        } else {
            flow.reconfigureCells();
        }
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
            disclosureNode.getStyleClass().setAll("tree-disclosure-node");

            final StackPane disclosureNodeArrow = new StackPane();
            disclosureNodeArrow.getStyleClass().setAll("arrow");
            disclosureNode.getChildren().add(disclosureNodeArrow);

            cell.setDisclosureNode(disclosureNode);
        }

        cell.updateTreeView(getSkinnable());

        return cell;
    }

    private TreeCell<T> createDefaultCellImpl() {
        return new TreeCell() {
            private HBox hbox;
            
            @Override public void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    hbox = null;
                    setText(null);
                    setGraphic(null);
                } else {
                    // update the graphic if one is set in the TreeItem
                    TreeItem<?> treeItem = (TreeItem<?>)getTreeItem();
                    if (treeItem != null && treeItem.getGraphic() != null) {
                        if (item instanceof Node) {
                            setText(null);
                            
                            // the item is a Node, and the graphic exists, so 
                            // we must insert both into an HBox and present that
                            // to the user (see RT-15910)
                            if (hbox == null) {
                                hbox = new HBox(3);
                            }
                            hbox.getChildren().setAll(treeItem.getGraphic(), (Node)item);
                            setGraphic(hbox);
                        } else {
                            hbox = null;
                            setText(item.toString());
                            setGraphic(treeItem.getGraphic());
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
        };
    }
    
    @Override protected double computePrefWidth(double height) {
        return computePrefHeight(-1) * 0.618033987;
    }

    @Override protected double computePrefHeight(double width) {
        return 400;
    }

    @Override
    protected void layoutChildren() {
        if (needCellsRecreated) {
            flow.recreateCells();
            needCellsRecreated = false;
        } else if (needItemCountUpdate) {
            updateItemCount();
            needItemCountUpdate = false;
        }
        super.layoutChildren();
    }
    
    private void onFocusPreviousCell() {
        FocusModel fm = getSkinnable().getFocusModel();
        if (fm == null) return;
        flow.show(fm.getFocusedIndex());
    }

    private void onFocusNextCell() {
        FocusModel fm = getSkinnable().getFocusModel();
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
     * Function used to scroll the container down by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the right.
     */
    public int onScrollPageDown() {
        IndexedCell lastVisibleCell = flow.getLastVisibleCellWithinViewPort();
        if (lastVisibleCell == null) return -1;

        int newSelectionIndex = -1;
        if (! (lastVisibleCell.isSelected() || lastVisibleCell.isFocused())) {
            // if the selection is not on the 'bottom' most cell, we firstly move
            // the selection down to that, without scrolling the contents
            newSelectionIndex = lastVisibleCell.getIndex();
        } else {
            // if the last visible cell is selected, we want to shift that cell up
            // to be the top-most cell, or at least as far to the top as we can go.
            flow.showAsFirst(lastVisibleCell);
            
            lastVisibleCell = flow.getLastVisibleCellWithinViewPort();
            newSelectionIndex = lastVisibleCell.getIndex();
        } 

        flow.show(lastVisibleCell);
        
        return newSelectionIndex;
    }

    /**
     * Function used to scroll the container up by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the left.
     */
    public int onScrollPageUp() {
        IndexedCell firstVisibleCell = flow.getFirstVisibleCellWithinViewPort();
        if (firstVisibleCell == null) return -1;

        int newSelectionIndex = -1;
        if (! (firstVisibleCell.isSelected() || firstVisibleCell.isFocused())) {
            // if the selection is not on the 'top' most cell, we firstly move
            // the selection up to that, without scrolling the contents
            newSelectionIndex = firstVisibleCell.getIndex();
        } else {
            // if the first visible cell is selected, we want to shift that cell down
            // to be the bottom-most cell, or at least as far to the bottom as we can go.
            flow.showAsLast(firstVisibleCell);
            
            firstVisibleCell = flow.getFirstVisibleCellWithinViewPort();
            newSelectionIndex = firstVisibleCell.getIndex();
        } 

        flow.show(firstVisibleCell);
        
        return newSelectionIndex;
    }
}

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

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.FocusModel;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import javafx.scene.input.MouseEvent;

import com.sun.javafx.scene.control.WeakListChangeListener;
import com.sun.javafx.scene.control.behavior.ListViewBehavior;
import javafx.util.Callback;

/**
 *
 */
public class ListViewSkin<T> extends VirtualContainerBase<ListView<T>, ListViewBehavior<T>, ListCell<T>> {

    private ObservableList<T> listViewItems;

    public ListViewSkin(final ListView<T> listView) {
        super(listView, new ListViewBehavior(listView));

        updateListViewItems();

        // init the VirtualFlow
        flow.setId("virtual-flow");
        flow.setPannable(false);
        flow.setVertical(getSkinnable().getOrientation() == Orientation.VERTICAL);
        flow.setFocusTraversable(getSkinnable().isFocusTraversable());
        flow.setCreateCell(new Callback<VirtualFlow, ListCell>() {
            @Override public ListCell call(VirtualFlow flow) {
                return ListViewSkin.this.createCell();
            }
        });
        getChildren().add(flow);
        
        EventHandler<MouseEvent> ml = new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) { 
                // RT-15127: cancel editing on scroll. This is a bit extreme
                // (we are cancelling editing on touching the scrollbars).
                // This can be improved at a later date.
                if (listView.getEditingIndex() > -1) {
                    listView.edit(-1);
                }
        
                // This ensures that the list maintains the focus, even when the vbar
                // and hbar controls inside the flow are clicked. Without this, the
                // focus border will not be shown when the user interacts with the
                // scrollbars, and more importantly, keyboard navigation won't be
                // available to the user.
                listView.requestFocus(); 
            }
        };
        flow.getVbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);
        flow.getHbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);
        
        updateCellCount();

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
        getBehavior().setOnScrollPageDown(new Callback<Integer, Integer>() {
            @Override public Integer call(Integer anchor) { return onScrollPageDown(anchor); }
        });
        getBehavior().setOnScrollPageUp(new Callback<Integer, Integer>() {
            @Override public Integer call(Integer anchor) { return onScrollPageUp(anchor); }
        });
        getBehavior().setOnSelectPreviousRow(new Runnable() {
            @Override public void run() { onSelectPreviousCell(); }
        });
        getBehavior().setOnSelectNextRow(new Runnable() {
            @Override public void run() { onSelectNextCell(); }
        });

        // Register listeners
        registerChangeListener(listView.itemsProperty(), "ITEMS");
        registerChangeListener(listView.orientationProperty(), "ORIENTATION");
        registerChangeListener(listView.cellFactoryProperty(), "CELL_FACTORY");
        registerChangeListener(listView.parentProperty(), "PARENT");
        registerChangeListener(listView.focusTraversableProperty(), "FOCUS_TRAVERSABLE");
    }

    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if (p == "ITEMS") {
            updateListViewItems();
        } else if (p == "ORIENTATION") {
            flow.setVertical(getSkinnable().getOrientation() == Orientation.VERTICAL);
        } else if (p == "CELL_FACTORY") {
            flow.recreateCells();
        } else if (p == "PARENT") {
            if (getSkinnable().getParent() != null && getSkinnable().isVisible()) {
                requestLayout();
            }
        } else if (p == "FOCUS_TRAVERSABLE") {
            flow.setFocusTraversable(getSkinnable().isFocusTraversable());
        }
    }

    private boolean itemCountDirty;
    private final ListChangeListener listViewItemsListener = new ListChangeListener() {
        @Override public void onChanged(Change c) {
            itemCountDirty = true;
            requestLayout();
        }
    };
    
    private final WeakListChangeListener weakListViewItemsListener =
            new WeakListChangeListener(listViewItemsListener);

    public void updateListViewItems() {
        if (listViewItems != null) {
            listViewItems.removeListener(weakListViewItemsListener);
        }

        this.listViewItems = getSkinnable().getItems();

        if (listViewItems != null) {
            listViewItems.addListener(weakListViewItemsListener);
        }

        itemCountDirty = true;
        requestLayout();
    }

    @Override public int getItemCount() {
        return listViewItems == null ? 0 : listViewItems.size();
    }

    void updateCellCount() {
        if (flow == null) return;
        
        int oldCount = flow.getCellCount();
        int newCount = getItemCount();
        
        flow.setCellCount(getItemCount());
        
        if (newCount != oldCount) {
            flow.recreateCells();
        } else {
            flow.reconfigureCells();
        }
    }

    @Override public ListCell<T> createCell() {
        ListCell<T> cell;
        if (getSkinnable().getCellFactory() != null) {
            cell = getSkinnable().getCellFactory().call(getSkinnable());
        } else {
            cell = createDefaultCellImpl();
        }

        cell.updateListView(getSkinnable());

        return cell;
    }

    private ListCell createDefaultCellImpl() {
        return new ListCell() {
            @Override public void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof Node) {
                    setText(null);
                    Node currentNode = getGraphic();
                    Node newNode = (Node) item;
                    if (currentNode == null || ! currentNode.equals(newNode)) {
                        setGraphic(newNode);
                    }
                } else {
                    /**
                     * This label is used if the item associated with this cell is to be
                     * represented as a String. While we will lazily instantiate it
                     * we never clear it, being more afraid of object churn than a minor
                     * "leak" (which will not become a "major" leak).
                     */
                    setText(item == null ? "null" : item.toString());
                    setGraphic(null);
                }
            }
        };
    }

//    @Override public void configCell(ListCell listCell) {
//        final int cellIndex = listCell.getIndex();
//
//        // Compute whether the index for this cell is for a real item
//        boolean valid = cellIndex >=0 && cellIndex < getItemCount();
//
//        // Cause the cell to update itself
//        if (valid) {
//            T newItem = listViewItems.get(cellIndex);
//            if (newItem == null || ! newItem.equals(listCell.getItem())) {
//                listCell.updateItem(newItem, false);
//            }
//        } else {
//            listCell.updateItem(null, true);
//        }
//    }

    @Override
    protected void layoutChildren() {
        if (itemCountDirty) {
            updateCellCount();
            itemCountDirty = false;
        }
        
        double x = getInsets().getLeft();
        double y = getInsets().getTop();
        double w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
        double h = getHeight() - (getInsets().getTop() + getInsets().getBottom());
        
        flow.resizeRelocate(x, y, w, h);
    }
    
    @Override protected double computePrefWidth(double height) {
//        return getInsets().getLeft() + flow.computePrefWidth(height) + getInsets().getRight();
        return computePrefHeight(-1) * 0.618033987;
    }

    @Override protected double computePrefHeight(double width) {
//        return getInsets().getTop() + flow.computePrefHeight(width) + getInsets().getBottom();
        return 400;
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
        SelectionModel sm = getSkinnable().getSelectionModel();
        if (sm == null) return;

        int pos = sm.getSelectedIndex();
        flow.show(pos);

        // Fix for RT-11299
        IndexedCell cell = flow.getFirstVisibleCell();
        if (cell == null || pos < cell.getIndex()) {
            flow.setPosition(pos / (double) getItemCount());
        }
    }

    private void onSelectNextCell() {
        SelectionModel sm = getSkinnable().getSelectionModel();
        if (sm == null) return;

        int pos = sm.getSelectedIndex();
        flow.show(pos);

        // Fix for RT-11299
        IndexedCell cell = flow.getLastVisibleCell();
        if (cell == null || cell.getIndex() < pos) {
            flow.setPosition(pos / (double) getItemCount());
        }
    }

    private void onMoveToFirstCell() {
        flow.show(0);
        flow.setPosition(0);
    }

    private void onMoveToLastCell() {
//        SelectionModel sm = getSkinnable().getSelectionModel();
//        if (sm == null) return;
//
        int endPos = getItemCount() - 1;
//        sm.select(endPos);
        flow.show(endPos);
        flow.setPosition(1);
    }

    /**
     * Function used to scroll the container down by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the right.
     */
    private int onScrollPageDown(int anchor) {
        IndexedCell lastVisibleCell = flow.getLastVisibleCellWithinViewPort();
        if (lastVisibleCell == null) return -1;

        int newSelectionIndex = -1;
        int lastVisibleCellIndex = lastVisibleCell.getIndex();
        if (! (lastVisibleCell.isSelected() || lastVisibleCell.isFocused()) || (lastVisibleCellIndex != anchor)) {
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
    private int onScrollPageUp(int anchor) {
        IndexedCell firstVisibleCell = flow.getFirstVisibleCellWithinViewPort();
        if (firstVisibleCell == null) return -1;

        int newSelectionIndex = -1;
        int firstVisibleCellIndex = firstVisibleCell.getIndex();
        if (! (firstVisibleCell.isSelected() || firstVisibleCell.isFocused()) || (firstVisibleCellIndex != anchor)) {
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

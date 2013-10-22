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

package com.sun.javafx.scene.control.skin;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.FocusModel;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import com.sun.javafx.scene.control.behavior.ListViewBehavior;
import com.sun.javafx.scene.control.skin.resources.ControlResources;

/**
 *
 */
public class ListViewSkin<T> extends VirtualContainerBase<ListView<T>, ListViewBehavior<T>, ListCell<T>> {
    
    /**
     * Region placed over the top of the flow (and possibly the header row) if
     * there is no data.
     */
    // FIXME this should not be a StackPane
    private StackPane placeholderRegion;
    private Node placeholderNode;
//    private Label placeholderLabel;
    private static final String EMPTY_LIST_TEXT = ControlResources.getString("ListView.noContent");

    private ObservableList<T> listViewItems;

    public ListViewSkin(final ListView<T> listView) {
        super(listView, new ListViewBehavior<T>(listView));

        updateListViewItems();

        // init the VirtualFlow
        flow.setId("virtual-flow");
        flow.setPannable(false);
        flow.setVertical(getSkinnable().getOrientation() == Orientation.VERTICAL);
        flow.setFocusTraversable(getSkinnable().isFocusTraversable());
        flow.setCreateCell(new Callback<VirtualFlow, ListCell<T>>() {
            @Override public ListCell<T> call(VirtualFlow flow) {
                return ListViewSkin.this.createCell();
            }
        });
        flow.setFixedCellSize(listView.getFixedCellSize());
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
        
        updateRowCount();

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
        registerChangeListener(listView.placeholderProperty(), "PLACEHOLDER");
        registerChangeListener(listView.fixedCellSizeProperty(), "FIXED_CELL_SIZE");
    }
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if ("ITEMS".equals(p)) {
            updateListViewItems();
        } else if ("ORIENTATION".equals(p)) {
            flow.setVertical(getSkinnable().getOrientation() == Orientation.VERTICAL);
        } else if ("CELL_FACTORY".equals(p)) {
            flow.recreateCells();
        } else if ("PARENT".equals(p)) {
            if (getSkinnable().getParent() != null && getSkinnable().isVisible()) {
                getSkinnable().requestLayout();
            }
        } else if ("FOCUS_TRAVERSABLE".equals(p)) {
            flow.setFocusTraversable(getSkinnable().isFocusTraversable());
        } else if ("PLACEHOLDER".equals(p)) {
            updatePlaceholderRegionVisibility();
        } else if ("FIXED_CELL_SIZE".equals(p)) {
            flow.setFixedCellSize(getSkinnable().getFixedCellSize());
        }
    }

    private final ListChangeListener<T> listViewItemsListener = new ListChangeListener<T>() {
        @Override public void onChanged(Change<? extends T> c) {
            while (c.next()) {
                if (c.wasReplaced()) {
                    // RT-28397: Support for when an item is replaced with itself (but
                    // updated internal values that should be shown visually)
                    itemCount = 0;
                    break;
                } else if (c.getRemovedSize() == itemCount) {
                    // RT-22463: If the user clears out an items list then we
                    // should reset all cells (in particular their contained
                    // items) such that a subsequent addition to the list of
                    // an item which equals the old item (but is rendered
                    // differently) still displays as expected (i.e. with the
                    // updated display, not the old display).
                    itemCount = 0;
                    break;
                }
            }
            
            rowCountDirty = true;
            getSkinnable().requestLayout();
        }
    };
    
    private final WeakListChangeListener<T> weakListViewItemsListener =
            new WeakListChangeListener<T>(listViewItemsListener);

    public void updateListViewItems() {
        if (listViewItems != null) {
            listViewItems.removeListener(weakListViewItemsListener);
        }

        this.listViewItems = getSkinnable().getItems();

        if (listViewItems != null) {
            listViewItems.addListener(weakListViewItemsListener);
        }

        rowCountDirty = true;
        getSkinnable().requestLayout();
    }
    
    private int itemCount = -1;

    @Override public int getItemCount() {
//        return listViewItems == null ? 0 : listViewItems.size();
        return itemCount;
    }
    
    private boolean needCellsRebuilt = true;
    private boolean needCellsReconfigured = false;

    @Override protected void updateRowCount() {
        if (flow == null) return;
        
        int oldCount = itemCount;
        int newCount = listViewItems == null ? 0 : listViewItems.size();
        
        itemCount = newCount;
        
        flow.setCellCount(newCount);
        
        updatePlaceholderRegionVisibility();
        if (newCount != oldCount) {
            needCellsRebuilt = true;
        } else {
            needCellsReconfigured = true;
        }
    }
    
    protected final void updatePlaceholderRegionVisibility() {
        boolean visible = getItemCount() == 0;
        
        if (visible) {
            placeholderNode = getSkinnable().getPlaceholder();
            if (placeholderNode == null && (EMPTY_LIST_TEXT != null && ! EMPTY_LIST_TEXT.isEmpty())) {
                placeholderNode = new Label();
                ((Label)placeholderNode).setText(EMPTY_LIST_TEXT);
            }

            if (placeholderNode != null) {
                if (placeholderRegion == null) {
                    placeholderRegion = new StackPane();
                    placeholderRegion.getStyleClass().setAll("placeholder");
                    getChildren().add(placeholderRegion);
                }

                placeholderRegion.getChildren().setAll(placeholderNode);
            }
        }

        flow.setVisible(! visible);
        if (placeholderRegion != null) {
            placeholderRegion.setVisible(visible);
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

    private static <T> ListCell<T> createDefaultCellImpl() {
        return new ListCell<T>() {
            @Override public void updateItem(T item, boolean empty) {
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

    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        super.layoutChildren(x, y, w, h);
        
        if (needCellsRebuilt) {
            flow.rebuildCells();
        } else if (needCellsReconfigured) {
            flow.reconfigureCells();
        } 
        
        needCellsRebuilt = false;
        needCellsReconfigured = false;
        
        if (getItemCount() == 0) {
            // show message overlay instead of empty listview
            if (placeholderRegion != null) {
                placeholderRegion.setVisible(w > 0 && h > 0);
                placeholderRegion.resizeRelocate(x, y, w, h);
            }
        } else {
            flow.resizeRelocate(x, y, w, h);
        }
    }
    
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        checkState();

        if (getItemCount() == 0) {
            if (placeholderRegion == null) {
                updatePlaceholderRegionVisibility();
            }
            if (placeholderRegion != null) {
                return placeholderRegion.prefWidth(height) + leftInset + rightInset;
            }
        }

        return computePrefHeight(-1, topInset, rightInset, bottomInset, leftInset) * 0.618033987;
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return 400;
    }
    
    private void onFocusPreviousCell() {
        FocusModel<T> fm = getSkinnable().getFocusModel();
        if (fm == null) return;
        flow.show(fm.getFocusedIndex());
    }

    private void onFocusNextCell() {
        FocusModel<T> fm = getSkinnable().getFocusModel();
        if (fm == null) return;
        flow.show(fm.getFocusedIndex());
    }

    private void onSelectPreviousCell() {
        SelectionModel<T> sm = getSkinnable().getSelectionModel();
        if (sm == null) return;

        int pos = sm.getSelectedIndex();
        flow.show(pos);

        // Fix for RT-11299
        IndexedCell<T> cell = flow.getFirstVisibleCell();
        if (cell == null || pos < cell.getIndex()) {
            flow.setPosition(pos / (double) getItemCount());
        }
    }

    private void onSelectNextCell() {
        SelectionModel<T> sm = getSkinnable().getSelectionModel();
        if (sm == null) return;

        int pos = sm.getSelectedIndex();
        flow.show(pos);

        // Fix for RT-11299
        ListCell<T> cell = flow.getLastVisibleCell();
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
        ListCell<T> lastVisibleCell = flow.getLastVisibleCellWithinViewPort();
        if (lastVisibleCell == null) return -1;

        final SelectionModel sm = getSkinnable().getSelectionModel();
        final FocusModel fm = getSkinnable().getFocusModel();
        if (sm == null || fm == null) return -1;

        int lastVisibleCellIndex = lastVisibleCell.getIndex();
        if (sm.isSelected(lastVisibleCellIndex) || fm.isFocused(lastVisibleCellIndex) || lastVisibleCellIndex == anchor) {
            // if the last visible cell is selected, we want to shift that cell up
            // to be the top-most cell, or at least as far to the top as we can go.
            flow.showAsFirst(lastVisibleCell);

            ListCell<T> newLastVisibleCell = flow.getLastVisibleCellWithinViewPort();
            lastVisibleCell = newLastVisibleCell == null ? lastVisibleCell : newLastVisibleCell;
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
     * Function used to scroll the container up by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the left.
     */
    private int onScrollPageUp(int anchor) {
        ListCell<T> firstVisibleCell = flow.getFirstVisibleCellWithinViewPort();
        if (firstVisibleCell == null) return -1;

        final SelectionModel sm = getSkinnable().getSelectionModel();
        final FocusModel fm = getSkinnable().getFocusModel();
        if (sm == null || fm == null) return -1;

        int firstVisibleCellIndex = firstVisibleCell.getIndex();
        if (sm.isSelected(firstVisibleCellIndex) || fm.isFocused(firstVisibleCellIndex) || firstVisibleCellIndex == anchor) {
            // if the first visible cell is selected, we want to shift that cell down
            // to be the bottom-most cell, or at least as far to the bottom as we can go.
            flow.showAsLast(firstVisibleCell);

            ListCell<T> newFirstVisibleCell = flow.getFirstVisibleCellWithinViewPort();
            firstVisibleCell = newFirstVisibleCell == null ? firstVisibleCell : newFirstVisibleCell;
        } else {
            // if the selection is not on the 'top' most cell, we firstly move
            // the selection up to that, without scrolling the contents, so
            // this is a no-op
        }

        int newSelectionIndex = firstVisibleCell.getIndex();
        flow.show(firstVisibleCell);
        return newSelectionIndex;
    }
}

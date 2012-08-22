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

import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import com.sun.javafx.scene.control.behavior.TableViewBehavior;
import javafx.collections.WeakListChangeListener;
import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.VPos;

public class TableViewSkin<T> extends VirtualContainerBase<TableView<T>, TableViewBehavior<T>, TableRow<T>> {
    
    private final TableView<T> tableView;

    public TableViewSkin(final TableView tableView) {
        super(tableView, new TableViewBehavior(tableView));
        
        this.tableView = tableView;

        // init the VirtualFlow
        flow.setPannable(false);
        flow.setFocusTraversable(tableView.isFocusTraversable());
        flow.setCreateCell(new Callback<VirtualFlow, TableRow>() {
            @Override public TableRow call(VirtualFlow flow) {
                return TableViewSkin.this.createCell();
            }
        });
        
        EventHandler<MouseEvent> ml = new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) { 
                // RT-15127: cancel editing on scroll. This is a bit extreme
                // (we are cancelling editing on touching the scrollbars).
                // This can be improved at a later date.
                if (tableView.getEditingCell() != null) {
                    tableView.edit(-1, null);
                }
                
                // This ensures that the table maintains the focus, even when the vbar
                // and hbar controls inside the flow are clicked. Without this, the
                // focus border will not be shown when the user interacts with the
                // scrollbars, and more importantly, keyboard navigation won't be
                // available to the user.
                tableView.requestFocus(); 
            }
        };
        flow.getVbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);
        flow.getHbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);

        columnReorderLine = new Region();
        columnReorderLine.getStyleClass().setAll("column-resize-line");
        columnReorderLine.setManaged(false);
        columnReorderLine.setVisible(false);

        columnReorderOverlay = new Region();
        columnReorderOverlay.getStyleClass().setAll("column-overlay");
        columnReorderOverlay.setVisible(false);
        columnReorderOverlay.setManaged(false);

        tableHeaderRow = new TableHeaderRow(tableView, flow);
        tableHeaderRow.setColumnReorderLine(columnReorderLine);
        tableHeaderRow.setTablePadding(getInsets());
        tableHeaderRow.setFocusTraversable(false);
        tableView.paddingProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                tableHeaderRow.setTablePadding(getInsets());
            }
        });

        getChildren().addAll(tableHeaderRow, flow, columnReorderOverlay, columnReorderLine);

        updateVisibleColumnCount();
        updateVisibleLeafColumnWidthListeners(tableView.getVisibleLeafColumns(), FXCollections.<TableColumn<T,?>>emptyObservableList());

        tableHeaderRow.reorderingProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                requestLayout();
            }
        });

        tableView.getVisibleLeafColumns().addListener(weakVisibleLeafColumnsListener);
        
        updateTableItems(null, tableView.getItems());
        tableView.itemsProperty().addListener(weakItemsChangeListener);

        tableView.getProperties().addListener(new MapChangeListener<Object, Object>() {
            @Override public void onChanged(Change<? extends Object, ? extends Object> c) {
                if (c.wasAdded() && "TableView.refresh".equals(c.getKey())) {
                    refreshView();
                    tableView.getProperties().remove("TableView.refresh");
                }
            }
        });

        // flow and flow.vbar width observer
        InvalidationListener widthObserver = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                contentWidthDirty = true;
                requestLayout();
            }
        };
        flow.widthProperty().addListener(widthObserver);
        flow.getVbar().widthProperty().addListener(widthObserver);

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
        getBehavior().setOnSelectLeftCell(new Runnable() {
            @Override public void run() { onSelectLeftCell(); }
        });
        getBehavior().setOnSelectRightCell(new Runnable() {
            @Override public void run() { onSelectRightCell(); }
        });

        registerChangeListener(tableView.rowFactoryProperty(), "ROW_FACTORY");
        registerChangeListener(tableView.placeholderProperty(), "PLACEHOLDER");
        registerChangeListener(tableView.focusTraversableProperty(), "FOCUS_TRAVERSABLE");
        registerChangeListener(tableView.widthProperty(), "WIDTH");
    }

    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);

        if ("ROW_FACTORY".equals(p)) {
            Callback<TableView<T>, ? extends TableRow<T>> oldFactory = rowFactory;
            rowFactory = tableView.getRowFactory();

            // TODO tighten this up
            if (oldFactory != rowFactory) {
                flow.recreateCells();
            }
        } else if ("PLACEHOLDER".equals(p)) {
            updatePlaceholderRegionVisibility();
        } else if ("FOCUS_TRAVERSABLE".equals(p)) {
            flow.setFocusTraversable(tableView.isFocusTraversable());
        } else if ("WIDTH".equals(p)) {
            tableHeaderRow.setTablePadding(getInsets());
        }
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/
    
    private ListChangeListener rowCountListener = new ListChangeListener() {
        @Override public void onChanged(Change c) {
            rowCountDirty = true;
            requestLayout();
        }
    };
    
    private ListChangeListener<TableColumn<T,?>> visibleLeafColumnsListener = 
        new ListChangeListener<TableColumn<T,?>>() {
            @Override public void onChanged(Change<? extends TableColumn<T,?>> c) {
                updateVisibleColumnCount();
                while (c.next()) {
                    updateVisibleLeafColumnWidthListeners(c.getAddedSubList(), c.getRemoved());
                }
            }
    };
    
    private InvalidationListener widthListener = new InvalidationListener() {
        @Override public void invalidated(Observable observable) {
            // This forces the horizontal scrollbar to show when the column
            // resizing occurs. It is not ideal, but will work for now.
            // FIXME this is very, very inefficient, but ensures we don't run
            // in to RT-13717.
            flow.reconfigureCells();
        }
    };
    
    private ChangeListener<ObservableList<T>> itemsChangeListener = 
        new ChangeListener<ObservableList<T>>() {
            @Override public void changed(ObservableValue<? extends ObservableList<T>> observable, 
                    ObservableList<T> oldList, ObservableList<T> newList) {
                updateTableItems(oldList, newList);
            }
    };
    
    private WeakListChangeListener<T> weakRowCountListener =
            new WeakListChangeListener<T>(rowCountListener);
    private WeakListChangeListener<TableColumn<T,?>> weakVisibleLeafColumnsListener =
            new WeakListChangeListener<TableColumn<T,?>>(visibleLeafColumnsListener);
    private WeakInvalidationListener weakWidthListener = 
            new WeakInvalidationListener(widthListener);
    private WeakChangeListener<ObservableList<T>> weakItemsChangeListener = 
            new WeakChangeListener<ObservableList<T>>(itemsChangeListener);
    
    
    
    /***************************************************************************
     *                                                                         *
     * Internal Fields                                                         *
     *                                                                         *
     **************************************************************************/

    private boolean rowCountDirty;
    private boolean contentWidthDirty = true;
    
    private double scrollX;
    
    /**
     * This region is used to overlay atop the table when the user is performing
     * a column resize operation or a column reordering operation. It is a line
     * that runs the height of the table to indicate either the final width of
     * of the selected column, or the position the column will be 'dropped' into
     * when the reordering operation completes.
     */
    private Region columnReorderLine;

    /**
     * A region which is resized and positioned such that it perfectly matches
     * the dimensions of any TableColumn that is being reordered by the user.
     * This is useful, for example, as a semi-transparent overlay to give
     * feedback to the user as to which column is currently being moved.
     */
    private Region columnReorderOverlay;

    /**
     * The entire header region for all columns. This header region handles
     * column reordering and resizing. It also handles the positioning and
     * resizing of thte columnReorderLine and columnReorderOverlay.
     */
    private TableHeaderRow tableHeaderRow;
    
    private Callback<TableView<T>, ? extends TableRow<T>> rowFactory;

    /**
     * Region placed over the top of the flow (and possibly the header row) if
     * there is no data and/or there are no columns specified.
     */
    // TODO externalise the strings so they can be i18n
    // FIXME this should not be a StackPane
    private StackPane placeholderRegion;
    private Label placeholderLabel;
    private static final String EMPTY_TABLE_TEXT = ControlResources.getString("TableView.noContent");
    private static final String NO_COLUMNS_TEXT = ControlResources.getString("TableView.noColumns");

    private int visibleColCount;
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/  
    
    /**
     * 
     */
    public TableHeaderRow getTableHeaderRow() {
        return tableHeaderRow;
    }
    
    /** {@inheritDoc} */
    @Override public int getItemCount() {
        return tableView.getItems() == null ? 0 : tableView.getItems().size();
    }
    
    /** {@inheritDoc} */
    @Override public TableRow createCell() {
        TableRow cell;

        if (tableView.getRowFactory() != null) {
            cell = tableView.getRowFactory().call(tableView);
        } else {
            cell = new TableRow();
        }

        cell.updateTableView(tableView);
        return cell;
    }

    /**
     * Function used to scroll the container down by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the right.
     */
    public int onScrollPageDown() {
        TableRow lastVisibleCell = (TableRow) flow.getLastVisibleCellWithinViewPort();
        if (lastVisibleCell == null) return -1;
        
        int lastVisibleCellIndex = lastVisibleCell.getIndex();
        
        boolean isSelected = lastVisibleCell.isSelected() || 
                lastVisibleCell.isFocused() || 
                isCellSelected(lastVisibleCellIndex) ||
                isCellFocused(lastVisibleCellIndex);
        
        if (isSelected) {
            // if the last visible cell is selected, we want to shift that cell up
            // to be the top-most cell, or at least as far to the top as we can go.
            flow.showAsFirst(lastVisibleCell);
            lastVisibleCell = (TableRow) flow.getLastVisibleCellWithinViewPort();
        } 

        int newSelectionIndex = lastVisibleCell.getIndex();
        flow.show(newSelectionIndex);
        return newSelectionIndex;
    }

    /**
     * Function used to scroll the container up by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the left.
     */
    public int onScrollPageUp() {
        TableRow firstVisibleCell = (TableRow) flow.getFirstVisibleCellWithinViewPort();
        if (firstVisibleCell == null) return -1;
        
        int firstVisibleCellIndex = firstVisibleCell.getIndex();
        
        boolean isSelected = firstVisibleCell.isSelected() || 
                firstVisibleCell.isFocused() || 
                isCellSelected(firstVisibleCellIndex) ||
                isCellFocused(firstVisibleCellIndex);
        
        if (isSelected) {
            // if the first visible cell is selected, we want to shift that cell down
            // to be the bottom-most cell, or at least as far to the bottom as we can go.
            flow.showAsLast(firstVisibleCell);
            firstVisibleCell = (TableRow) flow.getFirstVisibleCellWithinViewPort();
        } 

        int newSelectionIndex = firstVisibleCell.getIndex();
        flow.show(newSelectionIndex);
        return newSelectionIndex;
    }
    

    
    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/    
    
    private static final double GOLDEN_RATIO_MULTIPLIER = 0.618033987;

    @Override protected double computePrefHeight(double width) {
        return 400;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        double prefHeight = computePrefHeight(-1);
        
        List<TableColumn<T,?>> cols = tableView.getVisibleLeafColumns();
        if (cols == null || cols.isEmpty()) {
            return prefHeight * GOLDEN_RATIO_MULTIPLIER;
        } 
        
        double pw = getInsets().getLeft() + getInsets().getRight();
        for (int i = 0, max = cols.size(); i < max; i++) {
            TableColumn tc = cols.get(i);
            pw += Math.max(tc.getPrefWidth(), tc.getMinWidth());
        }
//        return pw;
        return Math.max(pw, prefHeight * GOLDEN_RATIO_MULTIPLIER);
    }
    
    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, double y,
            final double w, final double h) {
        if (rowCountDirty) {
            updateRowCount();
            rowCountDirty = false;
        }
        
        final double baselineOffset = tableView.getLayoutBounds().getHeight() / 2;

        // position the table header
        double tableHeaderRowHeight = tableHeaderRow.prefHeight(-1);
        layoutInArea(tableHeaderRow, x, y, w, tableHeaderRowHeight, baselineOffset, 
                HPos.CENTER, VPos.CENTER);
        y += tableHeaderRowHeight;

        // let the virtual flow take up all remaining space
        // TODO this calculation is to ensure the bottom border is visible when
        // placed in a Pane. It is not ideal, but will suffice for now. See 
        // RT-14335 for more information.
        double flowHeight = Math.floor(h - tableHeaderRowHeight);
        if (getItemCount() == 0 || visibleColCount == 0) {
            // show message overlay instead of empty table
            layoutInArea(placeholderRegion, x, y,
                    w, flowHeight,
                    baselineOffset, HPos.CENTER, VPos.CENTER);
        } else {
            layoutInArea(flow, x, y,
                    w, flowHeight,
                    baselineOffset, HPos.CENTER, VPos.CENTER);
        }
        
        // painting the overlay over the column being reordered
        if (tableHeaderRow.getReorderingRegion() != null) {
            TableColumnHeader reorderingColumnHeader = tableHeaderRow.getReorderingRegion();
            TableColumn reorderingColumn = reorderingColumnHeader.getTableColumn();
            if (reorderingColumn != null) {
                Node n = tableHeaderRow.getReorderingRegion();
                
                // determine where to draw the column header overlay, it's 
                // either from the left-edge of the column, or 0, if the column
                // is off the left-side of the TableView (i.e. horizontal 
                // scrolling has occured).
                double minX = tableHeaderRow.sceneToLocal(n.localToScene(n.getBoundsInLocal())).getMinX();
                double overlayWidth = reorderingColumnHeader.getWidth();
                if (minX < 0) {
                    overlayWidth += minX;
                }
                minX = minX < 0 ? 0 : minX;
                
                // prevent the overlay going out the right-hand side of the 
                // TableView
                if (minX + overlayWidth > w) {
                    overlayWidth = w - minX;
                    
                    if (flow.getVbar().isVisible()) {
                        overlayWidth -= flow.getVbar().getWidth() - 1;
                    }
                }
                
                double contentAreaHeight = flowHeight;
                if (flow.getHbar().isVisible()) {
                    contentAreaHeight -= flow.getHbar().getHeight();
                }
                
                columnReorderOverlay.resize(overlayWidth, contentAreaHeight);
                
                columnReorderOverlay.setLayoutX(minX);
                columnReorderOverlay.setLayoutY(tableHeaderRow.getHeight());
            }
            
            // paint the reorder line as well
            Insets rp = columnReorderLine.getInsets();
            double cw = rp.getLeft() + rp.getRight();
            double lineHeight = h - (flow.getHbar().isVisible() ? flow.getHbar().getHeight() - 1 : 0);
            columnReorderLine.resizeRelocate(0, rp.getTop(), cw, lineHeight);
        }
        
        columnReorderLine.setVisible(tableHeaderRow.isReordering());
        columnReorderOverlay.setVisible(tableHeaderRow.isReordering());
        
        // we test for item count here to resolve RT-14855, where the column
        // widths weren't being resized properly when in constrained layout mode
        // if there were no items.
        if (contentWidthDirty || getItemCount() == 0) {
            updateContentWidth();
            contentWidthDirty = false;
        }
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/
    
    public void updateTableItems(ObservableList<T> oldList, ObservableList<T> newList) {
        if (oldList != null) {
            oldList.removeListener(weakRowCountListener);
        }

        if (newList != null) {
            newList.addListener(weakRowCountListener);
        }

        rowCountDirty = true;
        requestLayout();
    }

    /**
     * Keeps track of how many leaf columns are currently visible in this table.
     */
    private void updateVisibleColumnCount() {
        visibleColCount = tableView.getVisibleLeafColumns().size();

        updatePlaceholderRegionVisibility();
        reconfigureCells();
    }
    
    private void updateVisibleLeafColumnWidthListeners(
            List<? extends TableColumn<T,?>> added, List<? extends TableColumn<T,?>> removed) {
        
        for (int i = 0, max = removed.size(); i < max; i++) {
            TableColumn tc = removed.get(i);
            tc.widthProperty().removeListener(weakWidthListener);
        }
        for (int i = 0, max = added.size(); i < max; i++) {
            TableColumn tc = added.get(i);
            tc.widthProperty().addListener(weakWidthListener);
        }
        flow.reconfigureCells();
    }

    private void updatePlaceholderRegionVisibility() {
        boolean visible = visibleColCount == 0 || getItemCount() == 0;
        
        if (visible) {
            if (placeholderRegion == null) {
                placeholderRegion = new StackPane();
                placeholderRegion.getStyleClass().setAll("placeholder");
                getChildren().add(placeholderRegion);
            }
            
            Node placeholderNode = tableView.getPlaceholder();

            if (placeholderNode == null) {
                if (placeholderLabel == null) {
                    placeholderLabel = new Label();
                }
                String s = visibleColCount == 0 ? NO_COLUMNS_TEXT : EMPTY_TABLE_TEXT;
                placeholderLabel.setText(s);

                placeholderRegion.getChildren().setAll(placeholderLabel);
            } else {
                placeholderRegion.getChildren().setAll(placeholderNode);
            }
        }

        flow.setVisible(! visible);
        if (placeholderRegion != null) {
            placeholderRegion.setVisible(visible);
        }
    }

    /*
     * It's often important to know how much width is available for content
     * within the table, and this needs to exclude the width of any vertical
     * scrollbar.
     */
    private void updateContentWidth() {
        double contentWidth = flow.getWidth();
        
        if (flow.getVbar().isVisible()) {
            contentWidth -= flow.getVbar().getWidth();
        }
        
        if (contentWidth <= 0) {
            // Fix for RT-14855 when there is no content in the TableView.
            contentWidth = getWidth() - (getInsets().getLeft() + getInsets().getRight());
        }

        // FIXME this isn't perfect, but it prevents RT-14885, which results in
        // undesired horizontal scrollbars when in constrained resize mode
        tableView.getProperties().put("TableView.contentWidth", Math.floor(contentWidth));
    }

    private void refreshView() {
        rowCountDirty = true;
        requestLayout();
    }

    private void reconfigureCells() {
        flow.reconfigureCells();
    }

    private void updateRowCount() {
        updatePlaceholderRegionVisibility();

        int oldCount = flow.getCellCount();
        int newCount = getItemCount();
        
        // if this is not called even when the count is the same, we get a 
        // memory leak in VirtualFlow.sheet.children. This can probably be 
        // optimised in the future when time permits.
        flow.setCellCount(newCount);
        
        if (newCount != oldCount) {
            // FIXME updateRowCount is called _a lot_. Perhaps we can make recreateCells
            // smarter. Imagine if items has one million items added - do we really
            // need to recreateCells a million times?
            flow.recreateCells();
        } else {
            flow.reconfigureCells();
        }
    }

    private void onFocusPreviousCell() {
        TableViewFocusModel fm = tableView.getFocusModel();
        if (fm == null) return;

        flow.show(fm.getFocusedIndex());
    }

    private void onFocusNextCell() {
        TableViewFocusModel fm = tableView.getFocusModel();
        if (fm == null) return;

        flow.show(fm.getFocusedIndex());
    }

    private void onSelectPreviousCell() {
        SelectionModel sm = tableView.getSelectionModel();
        if (sm == null) return;

        flow.show(sm.getSelectedIndex());
    }

    private void onSelectNextCell() {
        SelectionModel sm = tableView.getSelectionModel();
        if (sm == null) return;

        flow.show(sm.getSelectedIndex());
    }

    private void onSelectLeftCell() {
        scrollHorizontally();
    }

    private void onSelectRightCell() {
        scrollHorizontally();
    }
    
//    private void moveToLeftMostColumn() {
//        scrollHorizontally(tableView.getVisibleLeafColumn(0));
//    }
//    
//    private void moveToRightMostColumn() {
//        scrollHorizontally(tableView.getVisibleLeafColumn(tableView.getVisibleLeafColumns().size() - 1));
//    }

    // Handles the horizontal scrolling when the selection mode is cell-based
    // and the newly selected cell belongs to a column which is not totally
    // visible.
    private void scrollHorizontally() {
        TableViewFocusModel fm = tableView.getFocusModel();
        if (fm == null) return;

        TableColumn col = fm.getFocusedCell().getTableColumn();
        scrollHorizontally(col);
    }
    
//    private double flowScrollX = -1.0;
//    private final Map<TableColumn, Boolean> columnVisibilityMap = new HashMap<TableColumn, Boolean>();
//    boolean isColumnPartiallyOrFullyVisible(TableColumn col) {
//        if (col == null || !col.isVisible()) return false;
//        
//        double pos = flow.getHbar().getValue(); 
//        if (pos == flowScrollX && columnVisibilityMap.containsKey(col)) {
//            return columnVisibilityMap.get(col);
//        } else if (pos != flowScrollX) {
//            columnVisibilityMap.clear();
//        }
//
//        // work out where this column header is, and it's width (start -> end)
//        double start = scrollX;
//        for (TableColumn c : tableView.getVisibleLeafColumns()) {
//            if (c.equals(col)) break;
//            start += c.getWidth();
//        }
//        double end = start + col.getWidth();
//
//        // determine the width of the table
//        double headerWidth = tableView.getWidth() - getInsets().getLeft() + getInsets().getRight();
//        
//        boolean isVisible =(start >= pos || end > pos) && (start < (headerWidth + pos) || end <= (headerWidth + pos));
//        
//        columnVisibilityMap.put(col, isVisible);
//        flowScrollX = pos;
//        
//        return isVisible;
//    }

//    @Override
//    public void resize(double width, double height) {
//        columnVisibilityMap.clear();
//        impl_reapplyCSS();
//        
//        super.resize(width, height);
//    }
    
    private void scrollHorizontally(TableColumn col) {
        if (col == null || !col.isVisible()) return;

        // work out where this column header is, and it's width (start -> end)
        double start = scrollX;
        for (TableColumn c : tableView.getVisibleLeafColumns()) {
            if (c.equals(col)) break;
            start += c.getWidth();
        }
        double end = start + col.getWidth();

        // determine the width of the table
        double headerWidth = tableView.getWidth() - getInsets().getLeft() + getInsets().getRight();

        // determine by how much we need to translate the table to ensure that
        // the start position of this column lines up with the left edge of the
        // tableview, and also that the columns don't become detached from the
        // right edge of the table
        double pos = flow.getHbar().getValue();
        double max = flow.getHbar().getMax();
        double newPos;
        
        if (start < pos && start >= 0) {
            newPos = start;
        } else {
            double delta = start < 0 || end > headerWidth ? start : 0;
            newPos = pos + delta > max ? max : pos + delta;
        }

        // FIXME we should add API in VirtualFlow so we don't end up going
        // direct to the hbar.
        // actually shift the flow - this will result in the header moving
        // as well
        flow.getHbar().setValue(newPos);
    }

    private void onMoveToFirstCell() {
        SelectionModel sm = tableView.getSelectionModel();
        if (sm == null) return;

        flow.show(0);
        flow.setPosition(0);
    }

    private void onMoveToLastCell() {
        SelectionModel sm = tableView.getSelectionModel();
        if (sm == null) return;

        int endPos = getItemCount();
        flow.show(endPos);
        flow.setPosition(1);
    }

//    private void updateSelection(TableRow tableRow) {
//        TableViewSelectionModel sm = tableView.getSelectionModel();
//        if (sm == null) return;
//
//        TableViewFocusModel fm = tableView.getFocusModel();
//        if (fm == null) return;
//
//        int row = tableRow.getIndex();
//        TableColumn<T,?> column = fm.getFocusedCell().getTableColumn();
//
//        sm.clearAndSelect(row, column);
//        flow.show(tableRow);
//    }
    
    private boolean isCellSelected(int row) {
        TableView.TableViewSelectionModel sm = tableView.getSelectionModel();
        if (sm == null) return false;
        if (! sm.isCellSelectionEnabled()) return false;

        int columnCount = tableView.getVisibleLeafColumns().size();
        for (int col = 0; col < columnCount; col++) {
            if (sm.isSelected(row, tableView.getVisibleLeafColumn(col))) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isCellFocused(int row) {
        TableViewFocusModel fm = tableView.getFocusModel();
        if (fm == null) return false;

        int columnCount = tableView.getVisibleLeafColumns().size();
        for (int col = 0; col < columnCount; col++) {
            if (fm.isFocused(row, tableView.getVisibleLeafColumn(col))) {
                return true;
            }
        }
        return false;
    }
}

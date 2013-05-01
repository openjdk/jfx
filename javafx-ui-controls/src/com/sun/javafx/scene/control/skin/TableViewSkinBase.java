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

import com.sun.javafx.scene.control.behavior.BehaviorBase;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionModel;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import javafx.collections.WeakListChangeListener;
import com.sun.javafx.scene.control.skin.resources.ControlResources;
import java.util.List;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.ScrollToEvent;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableFocusModel;
import javafx.scene.control.TablePositionBase;
import javafx.scene.control.TableSelectionModel;

public abstract class TableViewSkinBase<S, C extends Control, B extends BehaviorBase<C>, I extends IndexedCell> extends VirtualContainerBase<C, B, I> {
    
    public static final String REFRESH = "tableRefreshKey";
    public static final String RECREATE = "tableRecreateKey";
    
//    protected abstract void requestControlFocus(); // tableView.requestFocus();
    protected abstract TableSelectionModel getSelectionModel(); // tableView.getSelectionModel()
    protected abstract TableFocusModel getFocusModel(); // tableView.getSelectionModel()
    protected abstract TablePositionBase getFocusedCell();
    protected abstract ObservableList<? extends TableColumnBase/*<S,?>*/> getVisibleLeafColumns();
    protected abstract int getVisibleLeafIndex(TableColumnBase tc);
    protected abstract TableColumnBase getVisibleLeafColumn(int col);
    protected abstract ObservableList<? extends TableColumnBase/*<S,?>*/> getColumns();
//    protected abstract ObservableList<S> getItems();
    protected abstract ObservableList<? extends TableColumnBase/*<S,?>*/> getSortOrder();
    
    protected abstract ObjectProperty<ObservableList<S>> itemsProperty();
    protected abstract ObjectProperty<Callback<C, I>> rowFactoryProperty();
    protected abstract ObjectProperty<Node> placeholderProperty();  // tableView.getPlaceholder();
//    protected abstract BooleanProperty focusTraversableProperty();
    protected abstract BooleanProperty tableMenuButtonVisibleProperty();
    protected abstract ObjectProperty<Callback<ResizeFeaturesBase, Boolean>> columnResizePolicyProperty();
    
    protected abstract boolean resizeColumn(TableColumnBase tc, double delta);
    protected abstract void resizeColumnToFitContent(TableColumnBase tc, int maxRows);

    public TableViewSkinBase(final C control, final B behavior) {
        super(control, behavior);
        
        // init(control) should not be called here - it should be called by the
        // subclass after initialising itself. This is to prevent NPEs (for 
        // example, getVisibleLeafColumns() throws a NPE as the control itself
        // is not yet set in subclasses).
    }
    
    protected void init(final C control) {
        // init the VirtualFlow
        flow.setPannable(false);
        flow.setFocusTraversable(control.isFocusTraversable());
        flow.setCreateCell(new Callback<VirtualFlow, I>() {
            @Override public I call(VirtualFlow flow) {
                return TableViewSkinBase.this.createCell();
            }
        });
        
        // TEMPORARY CODE (RT-24795)
        // we check the TableView to see if a fixed cell length is specified
        ObservableMap p = control.getProperties();
        String k = VirtualFlow.FIXED_CELL_LENGTH_KEY;
        final double fixedCellLength = (Double) (p.containsKey(k) ? p.get(k) : 0.0);
        final boolean fixedCellLengthEnabled = fixedCellLength > 0;
        flow.setFixedCellLength(fixedCellLength);
        // --- end of TEMPORARY CODE
        
        /*
         * Listening for scrolling along the X axis, but we need to be careful
         * to handle the situation appropriately when the hbar is invisible.
         */
        final InvalidationListener hbarValueListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                tableHeaderRow.updateScrollX();
                
                if (fixedCellLengthEnabled) {
                    flow.requestCellLayout();
                }
            }
        };
        flow.getHbar().valueProperty().addListener(hbarValueListener);

        columnReorderLine = new Region();
        columnReorderLine.getStyleClass().setAll("column-resize-line");
        columnReorderLine.setManaged(false);
        columnReorderLine.setVisible(false);

        columnReorderOverlay = new Region();
        columnReorderOverlay.getStyleClass().setAll("column-overlay");
        columnReorderOverlay.setVisible(false);
        columnReorderOverlay.setManaged(false);

        tableHeaderRow = createTableHeaderRow();
        tableHeaderRow.setColumnReorderLine(columnReorderLine);
        tableHeaderRow.setTablePadding(getSkinnable().getInsets());
        tableHeaderRow.setFocusTraversable(false);
        control.paddingProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                C c = getSkinnable();
                tableHeaderRow.setTablePadding(c == null ? Insets.EMPTY : c.getInsets());
            }
        });

        getChildren().addAll(tableHeaderRow, flow, columnReorderOverlay, columnReorderLine);

        updateVisibleColumnCount();
        updateVisibleLeafColumnWidthListeners(getVisibleLeafColumns(), FXCollections.<TableColumnBase<S,?>>emptyObservableList());

        tableHeaderRow.reorderingProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                getSkinnable().requestLayout();
            }
        });

        getVisibleLeafColumns().addListener(weakVisibleLeafColumnsListener);
        
        updateTableItems(null, itemsProperty().get());
        itemsProperty().addListener(weakItemsChangeListener);

        control.getProperties().addListener(new MapChangeListener<Object, Object>() {
            @Override public void onChanged(MapChangeListener.Change<? extends Object, ? extends Object> c) {
                if (! c.wasAdded()) return;
                if (REFRESH.equals(c.getKey())) {
                    refreshView();
                    control.getProperties().remove(REFRESH);
                } else if (RECREATE.equals(c.getKey())) {
                    forceCellRecreate = true;
                    refreshView();
                    control.getProperties().remove(RECREATE);
                }
            }
        });
        
        control.addEventHandler(ScrollToEvent.<TableColumnBase>scrollToColumn(), new EventHandler<ScrollToEvent<TableColumnBase>>() {
            @Override public void handle(ScrollToEvent<TableColumnBase> event) {
                scrollHorizontally(event.getScrollTarget());
            }
        });   

        // flow and flow.vbar width observer
        InvalidationListener widthObserver = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                contentWidthDirty = true;
                getSkinnable().requestLayout();
            }
        };
        flow.widthProperty().addListener(widthObserver);
        flow.getVbar().widthProperty().addListener(widthObserver);

        registerChangeListener(rowFactoryProperty(), "ROW_FACTORY");
        registerChangeListener(placeholderProperty(), "PLACEHOLDER");
        registerChangeListener(control.focusTraversableProperty(), "FOCUS_TRAVERSABLE");
        registerChangeListener(control.widthProperty(), "WIDTH");
    }
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);

        if ("ROW_FACTORY".equals(p)) {
            Callback<C, I> oldFactory = rowFactory;
            rowFactory = rowFactoryProperty().get();
            if (oldFactory != rowFactory) {
                needCellsRebuilt = true;
                getSkinnable().requestLayout();
            }
        } else if ("PLACEHOLDER".equals(p)) {
            updatePlaceholderRegionVisibility();
        } else if ("FOCUS_TRAVERSABLE".equals(p)) {
            flow.setFocusTraversable(getSkinnable().isFocusTraversable());
        } else if ("WIDTH".equals(p)) {
            tableHeaderRow.setTablePadding(getSkinnable().getInsets());
        }
    }
    
    protected TableHeaderRow createTableHeaderRow() {
        return new TableHeaderRow(this);
    } 
    
    
    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/
    
    private ListChangeListener rowCountListener = new ListChangeListener() {
        @Override public void onChanged(Change c) {
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
    
    private ListChangeListener<TableColumnBase> visibleLeafColumnsListener = 
        new ListChangeListener<TableColumnBase>() {
            @Override public void onChanged(Change<? extends TableColumnBase> c) {
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
            
            // using 'needCellsReconfigured' here rather than 'needCellsRebuilt'
            // as otherwise performance suffers massively (RT-27831)
            needCellsReconfigured = true;
            getSkinnable().requestLayout();
        }
    };
    
    private ChangeListener<ObservableList<S>> itemsChangeListener = 
        new ChangeListener<ObservableList<S>>() {
            @Override public void changed(ObservableValue<? extends ObservableList<S>> observable, 
                    ObservableList<S> oldList, ObservableList<S> newList) {
                updateTableItems(oldList, newList);
            }
    };
    
    private WeakListChangeListener<S> weakRowCountListener =
            new WeakListChangeListener<S>(rowCountListener);
    private WeakListChangeListener<TableColumnBase> weakVisibleLeafColumnsListener =
            new WeakListChangeListener<TableColumnBase>(visibleLeafColumnsListener);
    private WeakInvalidationListener weakWidthListener = 
            new WeakInvalidationListener(widthListener);
    private WeakChangeListener<ObservableList<S>> weakItemsChangeListener = 
            new WeakChangeListener<ObservableList<S>>(itemsChangeListener);
    
    
    
    /***************************************************************************
     *                                                                         *
     * Internal Fields                                                         *
     *                                                                         *
     **************************************************************************/

    private boolean contentWidthDirty = true;
    
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
    
    private Callback<C, I> rowFactory;

    /**
     * Region placed over the top of the flow (and possibly the header row) if
     * there is no data and/or there are no columns specified.
     */
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
    
    /**
     * Function used to scroll the container down by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the right.
     */
    public int onScrollPageDown() {
        I lastVisibleCell = flow.getLastVisibleCellWithinViewPort();
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
            lastVisibleCell = flow.getLastVisibleCellWithinViewPort();
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
        I firstVisibleCell = flow.getFirstVisibleCellWithinViewPort();
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
            firstVisibleCell = flow.getFirstVisibleCellWithinViewPort();
        } 

        int newSelectionIndex = firstVisibleCell.getIndex();
        flow.show(newSelectionIndex);
        return newSelectionIndex;
    }
    
     boolean isColumnPartiallyOrFullyVisible(TableColumnBase col) {
        if (col == null || !col.isVisible()) return false;
        
        double scrollX = flow.getHbar().getValue(); 

        // work out where this column header is, and it's width (start -> end)
        double start = 0;
        final ObservableList<? extends TableColumnBase> visibleLeafColumns = getVisibleLeafColumns();
        for (int i = 0, max = visibleLeafColumns.size(); i < max; i++) {
            TableColumnBase<S,?> c = visibleLeafColumns.get(i);
            if (c.equals(col)) break;
            start += c.getWidth();
        }
        double end = start + col.getWidth();

        // determine the width of the table
        final Insets padding = getSkinnable().getPadding();
        double headerWidth = getSkinnable().getWidth() - padding.getLeft() + padding.getRight();
        
        return (start >= scrollX || end > scrollX) && (start < (headerWidth + scrollX) || end <= (headerWidth + scrollX));
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/    
    
    private static final double GOLDEN_RATIO_MULTIPLIER = 0.618033987;

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return 400;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double prefHeight = computePrefHeight(-1, topInset, rightInset, bottomInset, leftInset);
        
        List<? extends TableColumnBase> cols = getVisibleLeafColumns();
        if (cols == null || cols.isEmpty()) {
            return prefHeight * GOLDEN_RATIO_MULTIPLIER;
        } 

        double pw = leftInset + rightInset;
        for (int i = 0, max = cols.size(); i < max; i++) {
            TableColumnBase tc = cols.get(i);
            pw += Math.max(tc.getPrefWidth(), tc.getMinWidth());
        }
//        return pw;
        return Math.max(pw, prefHeight * GOLDEN_RATIO_MULTIPLIER);
    }
    
    protected boolean needCellsRebuilt = true;
    protected boolean needCellsRecreated = true;
    protected boolean needCellsReconfigured = false;
    
    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, double y,
            final double w, final double h) {
        super.layoutChildren(x, y, w, h);
        
        if (needCellsRecreated) {
            flow.recreateCells();
        } else if (needCellsReconfigured) {
            flow.reconfigureCells();
        } else if (needCellsRebuilt) {
            flow.rebuildCells();
        } 
        
        needCellsRebuilt = false;
        needCellsRecreated = false;
        needCellsReconfigured = false;
        
        final double baselineOffset = getSkinnable().getLayoutBounds().getHeight() / 2;

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
            TableColumnBase reorderingColumn = reorderingColumnHeader.getTableColumn();
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
            double cw = columnReorderLine.snappedLeftInset() + columnReorderLine.snappedRightInset();
            double lineHeight = h - (flow.getHbar().isVisible() ? flow.getHbar().getHeight() - 1 : 0);
            columnReorderLine.resizeRelocate(0, columnReorderLine.snappedTopInset(), cw, lineHeight);
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
    
    public void updateTableItems(ObservableList<S> oldList, ObservableList<S> newList) {
        if (oldList != null) {
            oldList.removeListener(weakRowCountListener);
        }

        if (newList != null) {
            newList.addListener(weakRowCountListener);
        }

        rowCountDirty = true;
        getSkinnable().requestLayout();
    }

    /**
     * Keeps track of how many leaf columns are currently visible in this table.
     */
    private void updateVisibleColumnCount() {
        visibleColCount = getVisibleLeafColumns().size();

        updatePlaceholderRegionVisibility();
        needCellsRebuilt = true;
        getSkinnable().requestLayout();
    }
    
    private void updateVisibleLeafColumnWidthListeners(
            List<? extends TableColumnBase> added, List<? extends TableColumnBase> removed) {
        
        for (int i = 0, max = removed.size(); i < max; i++) {
            TableColumnBase tc = removed.get(i);
            tc.widthProperty().removeListener(weakWidthListener);
        }
        for (int i = 0, max = added.size(); i < max; i++) {
            TableColumnBase tc = added.get(i);
            tc.widthProperty().addListener(weakWidthListener);
        }
        needCellsRebuilt = true;
        getSkinnable().requestLayout();
    }

    protected final void updatePlaceholderRegionVisibility() {
        boolean visible = visibleColCount == 0 || getItemCount() == 0;
        
        if (visible) {
            if (placeholderRegion == null) {
                placeholderRegion = new StackPane();
                placeholderRegion.getStyleClass().setAll("placeholder");
                getChildren().add(placeholderRegion);
            }
            
            Node placeholderNode = placeholderProperty().get();

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
            Control c = getSkinnable();
            contentWidth = c.getWidth() - (snappedLeftInset() + snappedRightInset());
        }

        // FIXME this isn't perfect, but it prevents RT-14885, which results in
        // undesired horizontal scrollbars when in constrained resize mode
        getSkinnable().getProperties().put("TableView.contentWidth", Math.floor(contentWidth));
    }

    private void refreshView() {
        rowCountDirty = true;
        Control c = getSkinnable();
        if (c != null) {
            c.requestLayout();
        }
    }

    private int itemCount = -1;
    protected boolean forceCellRecreate = false;
    
    @Override protected void updateRowCount() {
        updatePlaceholderRegionVisibility();

        int oldCount = itemCount;
        int newCount = getItemCount();
        
        itemCount = newCount;
        
        // if this is not called even when the count is the same, we get a 
        // memory leak in VirtualFlow.sheet.children. This can probably be 
        // optimised in the future when time permits.
        flow.setCellCount(newCount);
        
        if (forceCellRecreate) {
            needCellsRecreated = true;
            forceCellRecreate = false;
        } else if (newCount != oldCount) {
            // FIXME updateRowCount is called _a lot_. Perhaps we can make rebuildCells
            // smarter. Imagine if items has one million items added - do we really
            // need to rebuildCells a million times? Maybe this is better now that
            // we do rebuildCells instead of recreateCells.
            needCellsRebuilt = true;
        } else {
            needCellsReconfigured = true;
        }
    }

    protected void onFocusPreviousCell() {
        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        flow.show(fm.getFocusedIndex());
    }

    protected void onFocusNextCell() {
        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        flow.show(fm.getFocusedIndex());
    }

    protected void onSelectPreviousCell() {
        SelectionModel sm = getSelectionModel();
        if (sm == null) return;

        flow.show(sm.getSelectedIndex());
    }

    protected void onSelectNextCell() {
        SelectionModel sm = getSelectionModel();
        if (sm == null) return;

        flow.show(sm.getSelectedIndex());
    }

    protected void onSelectLeftCell() {
        scrollHorizontally();
    }

    protected void onSelectRightCell() {
        scrollHorizontally();
    }
    
    // Handles the horizontal scrolling when the selection mode is cell-based
    // and the newly selected cell belongs to a column which is not totally
    // visible.
    private void scrollHorizontally() {
        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TableColumnBase col = getFocusedCell().getTableColumn();
        scrollHorizontally(col);
    }
    
    private void scrollHorizontally(TableColumnBase col) {
        if (col == null || !col.isVisible()) return;
        
        final Control control = getSkinnable();

        // work out where this column header is, and it's width (start -> end)
        double start = 0;//scrollX;
        for (TableColumnBase c : getVisibleLeafColumns()) {
            if (c.equals(col)) break;
            start += c.getWidth();
        }
        double end = start + col.getWidth();

        // determine the visible width of the table
        double headerWidth = control.getWidth() - snappedLeftInset() - snappedRightInset();

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
            double delta = start < 0 || end > headerWidth ? start - pos : 0;
            newPos = pos + delta > max ? max : pos + delta;
        }

        // FIXME we should add API in VirtualFlow so we don't end up going
        // direct to the hbar.
        // actually shift the flow - this will result in the header moving
        // as well
        flow.getHbar().setValue(newPos);
    }

    protected void onMoveToFirstCell() {
        flow.show(0);
        flow.setPosition(0);
    }

    protected void onMoveToLastCell() {
        int endPos = getItemCount();
        flow.show(endPos);
        flow.setPosition(1);
    }
    
    private boolean isCellSelected(int row) {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return false;
        if (! sm.isCellSelectionEnabled()) return false;

        int columnCount = getVisibleLeafColumns().size();
        for (int col = 0; col < columnCount; col++) {
            if (sm.isSelected(row, getVisibleLeafColumn(col))) {
                return true;
            }
        }

        return false;
    }
    
    private boolean isCellFocused(int row) {
        TableFocusModel fm = getFocusModel();
        if (fm == null) return false;

        int columnCount = getVisibleLeafColumns().size();
        for (int col = 0; col < columnCount; col++) {
            if (fm.isFocused(row, getVisibleLeafColumn(col))) {
                return true;
            }
        }
        
        return false;
    }
}

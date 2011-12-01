/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import com.sun.javafx.scene.control.behavior.CellBehaviorBase;
import com.sun.javafx.scene.control.WeakListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 */
public class TableRowSkin<T> extends CellSkinBase<TableRow<T>, CellBehaviorBase<TableRow<T>>> {

    // Specifies the number of times we will call 'recreateCells()' before we blow
    // out the cellsMap structure and rebuild all cells. This helps to prevent
    // against memory leaks in certain extreme circumstances.
    private static final int DEFAULT_FULL_REFRESH_COUNTER = 100;

    /*
     * A map that maps from TableColumn to TableCell (i.e. model to view).
     * This is recreated whenever the leaf columns change, however to increase
     * efficiency we create cells for all columns, even if they aren't visible,
     * and we only create new cells if we don't already have it cached in this
     * map.
     *
     * Note that this means that it is possible for this map to therefore be
     * a memory leak if an application uses TableView and is creating and removing
     * a large number of tableColumns. This is mitigated in the recreateCells()
     * function below - refer to that to learn more.
     */
    private WeakHashMap<TableColumn, TableCell> cellsMap;

    // This observableArrayList contains the currently visible table cells for this row.
    private final List<TableCell> cells = new ArrayList<TableCell>();
    
    private int fullRefreshCounter = DEFAULT_FULL_REFRESH_COUNTER;

    private boolean showColumns = true;
    
    private boolean isDirty = false;
    private boolean updateCells = false;
    
    private ListChangeListener visibleLeafColumnsListener = new ListChangeListener() {
        @Override public void onChanged(Change c) {
            isDirty = true;
            requestLayout();
        }
    };

    public TableRowSkin(TableRow<T> tableRow) {
        super(tableRow, new CellBehaviorBase<TableRow<T>>(tableRow));

        recreateCells();
        updateCells(true);

        initBindings();

        registerChangeListener(tableRow.editingProperty(), "EDITING");
        registerChangeListener(tableRow.indexProperty(), "ROW");
        registerChangeListener(tableRow.tableViewProperty(), "TABLE_VIEW");
    }

    @Override protected void handleControlPropertyChanged(String p) {
        // we run this before the super call because we want to update whether
        // we are showing columns or the node (if it isn't null) before the
        // parent class updates the content
        if (p == "TEXT" || p == "GRAPHIC" || p == "EDITING") {
            updateShowColumns();
        }

        super.handleControlPropertyChanged(p);

        if (p == "ROW") {
            updateCells = true;
            requestLayout();
        } else if (p == "TABLE_VIEW") {
            for (int i = 0; i < getChildren().size(); i++) {
                Node n = getChildren().get(i);
                if (n instanceof TableCell) {
                    ((TableCell)n).updateTableView(getSkinnable().getTableView());
                }
            }
        }
    }

    private void updateShowColumns() {
        boolean newValue = (isIgnoreText() && isIgnoreGraphic());
        if (showColumns == newValue) return;
        
        showColumns = newValue;

        requestLayout();
    }
    
    private void initBindings() {
        // watches for any change in the leaf columns observableArrayList - this will indicate
        // that the column order has changed and that we should update the row
        // such that the cells are in the new order
        if (getSkinnable() == null) {
            throw new IllegalStateException("TableRowSkin does not have a Skinnable set to a TableRow instance");
        }
        if (getSkinnable().getTableView() == null) {
            throw new IllegalStateException("TableRow not have the TableView property set");
        }
        
        getSkinnable().getTableView().getVisibleLeafColumns().addListener(
                new WeakListChangeListener(visibleLeafColumnsListener));
    }
    
    private void doUpdateCheck() {
        if (isDirty) {
            recreateCells();
            updateCells(true);
            isDirty = false;
        } else if (updateCells) {
            updateCells(false);
            updateCells = false;
        }
    }

    @Override protected void layoutChildren() {
        doUpdateCheck();
        
        TableView<T> table = getSkinnable().getTableView();
        if (table == null) return;
        if (cellsMap.isEmpty()) return;
        
        if (showColumns && ! table.getVisibleLeafColumns().isEmpty()) {
            // layout the individual column cells
            TableColumn<T,?> col;
            TableCell cell;
            double x = getInsets().getLeft();
            double width;
            double height;
            List<TableColumn<T,?>> leafColumns = table.getVisibleLeafColumns();
            
            double verticalPadding = getInsets().getTop() + getInsets().getBottom();
            double horizontalPadding = getInsets().getLeft() + getInsets().getRight();
            
            for (int i = 0; i < leafColumns.size(); i++) {
                col = leafColumns.get(i);
                cell = cellsMap.get(col);
                if (cell == null) continue;

                width = snapSize(cell.prefWidth(-1) - horizontalPadding);
                height = Math.max(getHeight(), cell.prefHeight(-1));
                height = snapSize(height - verticalPadding);
                
                cell.resize(width, height);
                cell.relocate(x, getInsets().getTop());
                x += width;
            }
        } else {
            super.layoutChildren();
        }
    }

    private void recreateCells() {
        // This function is smart in the sense that we don't recreate all
        // TableCell instances every time this function is called. Instead we
        // only create TableCells for TableColumns we haven't already encountered.
        // To avoid a potential memory leak (when the TableColumns in the
        // TableView are created/inserted/removed/deleted, we have a 'refresh
        // counter' that when we reach 0 will delete all cells in this row
        // and recreate all of them.
        
        TableView<T> table = getSkinnable().getTableView();
        if (table == null) {
            clearCellsMap();
            return;
        }
        
        ObservableList<TableColumn<T,?>> columns = table.getVisibleLeafColumns();
        
        if (fullRefreshCounter == 0 || cellsMap == null) {
            clearCellsMap();
            cellsMap = new WeakHashMap<TableColumn, TableCell>(columns.size());
            fullRefreshCounter = DEFAULT_FULL_REFRESH_COUNTER;
        }
        fullRefreshCounter--;
        
        for (TableColumn col : columns) {
            if (cellsMap.containsKey(col)) {
                continue;
            }
            
            // we must create a TableCell for each table column
            TableCell cell = (TableCell) col.getCellFactory().call(col);

            // we set it's TableColumn, TableView and TableRow
            cell.updateTableColumn(col);
            cell.updateTableView(table);
            cell.updateTableRow(getSkinnable());

            // and store this in our HashMap until needed
            cellsMap.put(col, cell);
        }
    }
    
    private void clearCellsMap() {
        if (cellsMap != null) cellsMap.clear();
    }

    private void updateCells(boolean resetChildren) {
        // if delete isn't called first, we can run into situations where the
        // cells aren't updated properly.
        cells.clear();

        TableView<T> table = getSkinnable().getTableView();
        if (table != null) {
            for (TableColumn<T,?> col : table.getVisibleLeafColumns()) {
                TableCell cell = cellsMap.get(col);
                if (cell == null) continue;

                cell.updateIndex(getSkinnable().getIndex());
                cell.updateTableRow(getSkinnable());
                cells.add(cell);
            }
        }

        // update children of each row
        if (resetChildren) {
            if (showColumns) {
                if (cells.isEmpty()) {
                    getChildren().clear();
                } else {
                    // TODO we can optimise this by only showing cells that are 
                    // visible based on the table width and the amount of horizontal
                    // scrolling.
                    getChildren().setAll(cells);
                }
            } else {
                getChildren().clear();

                if (!isIgnoreText() || !isIgnoreGraphic()) {
                    getChildren().add(getSkinnable());
                }
            }
        }
    }
    
    @Override protected double computePrefWidth(double height) {
        doUpdateCheck();
        
        if (showColumns) {
            double prefWidth = 0.0F;

            if (getSkinnable().getTableView() != null) {
                for (TableColumn<T,?> tableColumn : getSkinnable().getTableView().getVisibleLeafColumns()) {
                    prefWidth += tableColumn.getWidth();
                }
            }

            return prefWidth;
        } else {
            return super.computePrefWidth(height);
        }
    }
    
    @Override protected double computePrefHeight(double width) {
        doUpdateCheck();
        
        if (showColumns) {
            // FIXME according to profiling, this method is slow and should
            // be optimised
            double prefHeight = 0.0f;
            final int count = cells.size();
            for (int i=0; i<count; i++) {
                final TableCell tableCell = cells.get(i);
                prefHeight = Math.max(prefHeight, tableCell.prefHeight(-1));
            }
            return Math.max(prefHeight, Math.max(getCellSize(), minHeight(-1)));
        } else {
            return super.computePrefHeight(width);
        }
    }
}

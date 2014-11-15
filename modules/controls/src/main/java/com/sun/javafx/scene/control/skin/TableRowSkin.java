/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import com.sun.javafx.scene.control.behavior.CellBehaviorBase;
import com.sun.javafx.scene.control.behavior.TableRowBehavior;

import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView.TableViewFocusModel;

/**
 */
public class TableRowSkin<T> extends TableRowSkinBase<T, TableRow<T>, CellBehaviorBase<TableRow<T>>, TableCell<T,?>> {

    private TableView<T> tableView;
    private TableViewSkin<T> tableViewSkin;
    
    public TableRowSkin(TableRow<T> tableRow) {
        super(tableRow, new TableRowBehavior<T>(tableRow));
        
        this.tableView = tableRow.getTableView();
        updateTableViewSkin();
        
        super.init(tableRow);

        registerChangeListener(tableRow.tableViewProperty(), "TABLE_VIEW");
    }

    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if ("TABLE_VIEW".equals(p)) {
            updateTableViewSkin();
            
            for (int i = 0, max = cells.size(); i < max; i++) {
                Node n = cells.get(i);
                if (n instanceof TableCell) {
                    ((TableCell)n).updateTableView(getSkinnable().getTableView());
                }
            }
            
            this.tableView = getSkinnable().getTableView();
        }
    }
    
    @Override protected TableCell<T, ?> getCell(TableColumnBase tcb) {
        TableColumn tableColumn = (TableColumn<T,?>) tcb;
        TableCell cell = (TableCell) tableColumn.getCellFactory().call(tableColumn);

        // we set it's TableColumn, TableView and TableRow
        cell.updateTableColumn(tableColumn);
        cell.updateTableView(tableColumn.getTableView());
        cell.updateTableRow(getSkinnable());
        
        return cell;
    }

    @Override protected ObservableList<TableColumn<T, ?>> getVisibleLeafColumns() {
        return tableView.getVisibleLeafColumns();
    }

    @Override protected void updateCell(TableCell<T, ?> cell, TableRow<T> row) {
        cell.updateTableRow(row);
    }

    @Override protected DoubleProperty fixedCellSizeProperty() {
        return tableView.fixedCellSizeProperty();
    }

    @Override protected boolean isColumnPartiallyOrFullyVisible(TableColumnBase tc) {
        return tableViewSkin == null ? false : tableViewSkin.isColumnPartiallyOrFullyVisible((TableColumn)tc);
    }

    @Override protected TableColumn<T, ?> getTableColumnBase(TableCell<T, ?> cell) {
        return cell.getTableColumn();
    }

    @Override protected ObjectProperty<Node> graphicProperty() {
        return null;
    }

    @Override protected Control getVirtualFlowOwner() {
        return getSkinnable().getTableView();
    }
    
    private void updateTableViewSkin() {
        TableView<T> tableView = getSkinnable().getTableView();
        if (tableView.getSkin() instanceof TableViewSkin) {
            tableViewSkin = (TableViewSkin)tableView.getSkin();
        }
    }

    @Override
    protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case SELECTED_ITEMS: {
                // FIXME this could be optimised to iterate over cellsMap only
                // (selectedCells could be big, cellsMap is much smaller)
                List<Node> selection = new ArrayList<>();
                int index = getSkinnable().getIndex();
                for (TablePosition<T,?> pos : tableView.getSelectionModel().getSelectedCells()) {
                    if (pos.getRow() == index) {
                        TableColumn<T,?> column = pos.getTableColumn();
                        if (column == null) {
                            /* This is the row-based case */
                            column = tableView.getVisibleLeafColumn(0);
                        }
                        TableCell<T,?> cell = cellsMap.get(column);
                        if (cell != null) selection.add(cell);
                    }
                    return FXCollections.observableArrayList(selection);
                }
            }
            case CELL_AT_ROW_COLUMN: {
                int colIndex = (Integer)parameters[1];
                TableColumn<T,?> column = tableView.getVisibleLeafColumn(colIndex);
                return cellsMap.get(column);
            }
            case FOCUS_ITEM: {
                TableViewFocusModel<T> fm = tableView.getFocusModel();
                TablePosition<T,?> focusedCell = fm.getFocusedCell();
                TableColumn<T,?> column = focusedCell.getTableColumn();
                if (column == null) {
                    /* This is the row-based case */
                    column = tableView.getVisibleLeafColumn(0);
                }
                return cellsMap.get(column);
            }
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

}

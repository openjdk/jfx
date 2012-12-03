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

import com.sun.javafx.scene.control.behavior.TableViewBehavior;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.layout.Region;

public class TableViewSkin<T> extends TableViewSkinBase<T, TableView<T>, TableViewBehavior<T>, TableRow<T>> {
    
    private final TableView<T> tableView;

    public TableViewSkin(final TableView tableView) {
        super(tableView, new TableViewBehavior(tableView));
        
        this.tableView = tableView;
        
        super.init(tableView);

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

        // init the behavior 'closures'
        TableViewBehavior behavior = getBehavior();
        behavior.setOnFocusPreviousRow(new Runnable() {
            @Override public void run() { onFocusPreviousCell(); }
        });
        behavior.setOnFocusNextRow(new Runnable() {
            @Override public void run() { onFocusNextCell(); }
        });
        behavior.setOnMoveToFirstCell(new Runnable() {
            @Override public void run() { onMoveToFirstCell(); }
        });
        behavior.setOnMoveToLastCell(new Runnable() {
            @Override public void run() { onMoveToLastCell(); }
        });
        behavior.setOnScrollPageDown(new Callback<Void, Integer>() {
            @Override public Integer call(Void param) { return onScrollPageDown(); }
        });
        behavior.setOnScrollPageUp(new Callback<Void, Integer>() {
            @Override public Integer call(Void param) { return onScrollPageUp(); }
        });
        behavior.setOnSelectPreviousRow(new Runnable() {
            @Override public void run() { onSelectPreviousCell(); }
        });
        behavior.setOnSelectNextRow(new Runnable() {
            @Override public void run() { onSelectNextCell(); }
        });
        behavior.setOnSelectLeftCell(new Runnable() {
            @Override public void run() { onSelectLeftCell(); }
        });
        behavior.setOnSelectRightCell(new Runnable() {
            @Override public void run() { onSelectRightCell(); }
        });
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

    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/  
    
    /** {@inheritDoc} */
    @Override protected ObservableList<TableColumn<T, ?>> getVisibleLeafColumns() {
        return tableView.getVisibleLeafColumns();
    }

    @Override protected int getVisibleLeafIndex(TableColumnBase tc) {
        return tableView.getVisibleLeafIndex((TableColumn)tc);
    }
    
    @Override protected TableColumnBase getVisibleLeafColumn(int col) {
        return tableView.getVisibleLeafColumn(col);
    }
    
    /** {@inheritDoc} */
    @Override protected TableViewFocusModel getFocusModel() {
        return tableView.getFocusModel();
    }

    /** {@inheritDoc} */
    @Override protected TableSelectionModel getSelectionModel() {
        return tableView.getSelectionModel();
    }

    /** {@inheritDoc} */
    @Override protected ObjectProperty<Callback<TableView<T>, TableRow<T>>> rowFactoryProperty() {
        return tableView.rowFactoryProperty();
    }

    /** {@inheritDoc} */
    @Override protected ObjectProperty<Node> placeholderProperty() {
        return tableView.placeholderProperty();
    }

    /** {@inheritDoc} */
    @Override protected ObjectProperty<ObservableList<T>> itemsProperty() {
        return tableView.itemsProperty();
    }

    /** {@inheritDoc} */
    @Override protected ObservableList<TableColumn<T, ?>> getColumns() {
        return tableView.getColumns();
    }

    /** {@inheritDoc} */
    @Override protected BooleanProperty tableMenuButtonVisibleProperty() {
        return tableView.tableMenuButtonVisibleProperty();
    }

    /** {@inheritDoc} */
    @Override protected ObjectProperty<Callback<ResizeFeaturesBase, Boolean>> columnResizePolicyProperty() {
        // TODO Ugly!
        return (ObjectProperty<Callback<ResizeFeaturesBase, Boolean>>) (Object) tableView.columnResizePolicyProperty();
    }

    /** {@inheritDoc} */
    @Override protected ObservableList<TableColumn<T,?>> getSortOrder() {
        return tableView.getSortOrder();
    }

    @Override protected boolean resizeColumn(TableColumnBase tc, double delta) {
        return tableView.resizeColumn((TableColumn)tc, delta);
    }

    /*
     * FIXME: Naive implementation ahead
     * Attempts to resize column based on the pref width of all items contained
     * in this column. This can be potentially very expensive if the number of
     * rows is large.
     */
    @Override protected void resizeColumnToFitContent(TableColumnBase tc, int maxRows) {
        final TableColumn col = (TableColumn) tc;
        List<?> items = itemsProperty().get();
        if (items == null || items.isEmpty()) return;
    
        Callback cellFactory = col.getCellFactory();
        if (cellFactory == null) return;
    
        TableCell cell = (TableCell) cellFactory.call(col);
        if (cell == null) return;
        
        // set this property to tell the TableCell we want to know its actual
        // preferred width, not the width of the associated TableColumnBase
        cell.getProperties().put(TableCellSkin.DEFER_TO_PARENT_PREF_WIDTH, Boolean.TRUE);
        
        // determine cell padding
        double padding = 10;
        Node n = cell.getSkin() == null ? null : cell.getSkin().getNode();
        if (n instanceof Region) {
            Region r = (Region) n;
            padding = r.getInsets().getLeft() + r.getInsets().getRight();
        } 
        
        int rows = maxRows == -1 ? items.size() : Math.min(items.size(), maxRows);
        double maxWidth = 0;
        for (int row = 0; row < rows; row++) {
            cell.updateTableColumn(col);
            cell.updateTableView(tableView);
            cell.updateIndex(row);
            
            if ((cell.getText() != null && !cell.getText().isEmpty()) || cell.getGraphic() != null) {
                getChildren().add(cell);
                cell.impl_processCSS(false);
                maxWidth = Math.max(maxWidth, cell.prefWidth(-1));
                getChildren().remove(cell);
            }
        }
        
        col.impl_setWidth(maxWidth + padding);
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
    
}

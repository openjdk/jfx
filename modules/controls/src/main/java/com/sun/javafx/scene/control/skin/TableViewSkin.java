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

import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.accessibility.Attribute;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import com.sun.javafx.scene.control.behavior.TableViewBehavior;

public class TableViewSkin<T> extends TableViewSkinBase<T, T, TableView<T>, TableViewBehavior<T>, TableRow<T>, TableColumn<T, ?>> {
    
    private final TableView<T> tableView;

    public TableViewSkin(final TableView<T> tableView) {
        super(tableView, new TableViewBehavior<T>(tableView));
        
        this.tableView = tableView;
        flow.setFixedCellSize(tableView.getFixedCellSize());
        
        super.init(tableView);

        EventHandler<MouseEvent> ml = event -> {
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
        };
        flow.getVbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);
        flow.getHbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);

        // init the behavior 'closures'
        TableViewBehavior<T> behavior = getBehavior();
        behavior.setOnFocusPreviousRow(() -> { onFocusPreviousCell(); });
        behavior.setOnFocusNextRow(() -> { onFocusNextCell(); });
        behavior.setOnMoveToFirstCell(() -> { onMoveToFirstCell(); });
        behavior.setOnMoveToLastCell(() -> { onMoveToLastCell(); });
        behavior.setOnScrollPageDown(isFocusDriven -> onScrollPageDown(isFocusDriven));
        behavior.setOnScrollPageUp(isFocusDriven -> onScrollPageUp(isFocusDriven));
        behavior.setOnSelectPreviousRow(() -> { onSelectPreviousCell(); });
        behavior.setOnSelectNextRow(() -> { onSelectNextCell(); });
        behavior.setOnSelectLeftCell(() -> { onSelectLeftCell(); });
        behavior.setOnSelectRightCell(() -> { onSelectRightCell(); });

        registerChangeListener(tableView.fixedCellSizeProperty(), "FIXED_CELL_SIZE");

    }

    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);

        if ("FIXED_CELL_SIZE".equals(p)) {
            flow.setFixedCellSize(getSkinnable().getFixedCellSize());
        }
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

    @Override protected int getVisibleLeafIndex(TableColumn<T, ?> tc) {
        return tableView.getVisibleLeafIndex(tc);
    }
    
    @Override protected TableColumn<T, ?> getVisibleLeafColumn(int col) {
        return tableView.getVisibleLeafColumn(col);
    }
    
    /** {@inheritDoc} */
    @Override protected TableViewFocusModel<T> getFocusModel() {
        return tableView.getFocusModel();
    }

    /** {@inheritDoc} */
    @Override protected TablePosition<T, ?> getFocusedCell() {
        return tableView.getFocusModel().getFocusedCell();
    }
    
    /** {@inheritDoc} */
    @Override protected TableSelectionModel<T> getSelectionModel() {
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

    @Override protected boolean resizeColumn(TableColumn<T, ?> tc, double delta) {
        return tableView.resizeColumn(tc, delta);
    }

    /*
     * FIXME: Naive implementation ahead
     * Attempts to resize column based on the pref width of all items contained
     * in this column. This can be potentially very expensive if the number of
     * rows is large.
     */
    @Override protected void resizeColumnToFitContent(TableColumn<T, ?> tc, int maxRows) {
        final TableColumn<T, ?> col = tc;
        List<?> items = itemsProperty().get();
        if (items == null || items.isEmpty()) return;
    
        Callback/*<TableColumn<T, ?>, TableCell<T,?>>*/ cellFactory = col.getCellFactory();
        if (cellFactory == null) return;
    
        TableCell<T,?> cell = (TableCell<T, ?>) cellFactory.call(col);
        if (cell == null) return;
        
        // set this property to tell the TableCell we want to know its actual
        // preferred width, not the width of the associated TableColumnBase
        cell.getProperties().put(TableCellSkin.DEFER_TO_PARENT_PREF_WIDTH, Boolean.TRUE);
        
        // determine cell padding
        double padding = 10;
        Node n = cell.getSkin() == null ? null : cell.getSkin().getNode();
        if (n instanceof Region) {
            Region r = (Region) n;
            padding = r.snappedLeftInset() + r.snappedRightInset();
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

        // dispose of the cell to prevent it retaining listeners (see RT-31015)
        cell.updateIndex(-1);
        
        // RT-23486
        double widthMax = maxWidth + padding;
        if(tableView.getColumnResizePolicy() == TableView.CONSTRAINED_RESIZE_POLICY) {
             widthMax = Math.max(widthMax, col.getWidth());
        }

        col.impl_setWidth(widthMax); 
    }
    
    /** {@inheritDoc} */
    @Override public int getItemCount() {
        return tableView.getItems() == null ? 0 : tableView.getItems().size();
    }
    
    /** {@inheritDoc} */
    @Override public TableRow<T> createCell() {
        TableRow<T> cell;

        if (tableView.getRowFactory() != null) {
            cell = tableView.getRowFactory().call(tableView);
        } else {
            cell = new TableRow<T>();
        }

        cell.updateTableView(tableView);
        return cell;
    }

    @Override protected void horizontalScroll() {
        super.horizontalScroll();
        if (getSkinnable().getFixedCellSize() > 0) {
            flow.requestCellLayout();
        }
    }
    
    @Override
    public Object accGetAttribute(Attribute attribute, Object... parameters) {
        switch (attribute) {
            case SELECTED_CELLS: {
                List<Node> selection = new ArrayList<>();
                TableViewSelectionModel<T> sm = getSkinnable().getSelectionModel();
                for (TablePosition pos : sm.getSelectedCells()) {
                    TableRow<T> row = flow.getCell(pos.getRow());
                    if (row != null) selection.add(row);
                }
                return FXCollections.observableArrayList(selection);
            }
            case FOCUS_ITEM: // TableViewSkinBase
            case CELL_AT_ROW_COLUMN: // TableViewSkinBase
            case COLUMN_AT_INDEX: // TableViewSkinBase
            case HEADER: // TableViewSkinBase
            default: return super.accGetAttribute(attribute, parameters);
        }
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

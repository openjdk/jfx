/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;


import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.TableRowBehavior;

import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;

/**
 * Default skin implementation for the {@link TableRow} control.
 *
 * @see TableRow
 * @since 9
 */
public class TableRowSkin<T> extends TableRowSkinBase<T, TableRow<T>, TableCell<T,?>> {

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final BehaviorBase<TableRow<T>> behavior;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new TableRowSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public TableRowSkin(TableRow<T> control) {
        super(control);

        // install default input map for the TableRow control
        behavior = new TableRowBehavior<>(control);

        registerChangeListener(control.tableViewProperty(), e -> {
            for (int i = 0, max = cells.size(); i < max; i++) {
                Node n = cells.get(i);
                if (n instanceof TableCell) {
                    ((TableCell)n).updateTableView(getSkinnable().getTableView());
                }
            }
        });

        setupTreeTableViewListeners();
    }

    // FIXME: replace listener to fixedCellSize with direct lookup - JDK-8277000
    private void setupTreeTableViewListeners() {
        TableView<T> tableView = getSkinnable().getTableView();
        if (tableView == null) {
            registerInvalidationListener(getSkinnable().tableViewProperty(), e -> {
                unregisterInvalidationListeners(getSkinnable().tableViewProperty());
                setupTreeTableViewListeners();
            });
        } else {
            DoubleProperty fixedCellSizeProperty = tableView.fixedCellSizeProperty();
            if (fixedCellSizeProperty != null) {
                registerChangeListener(fixedCellSizeProperty, e -> {
                    fixedCellSize = fixedCellSizeProperty.get();
                    fixedCellSizeEnabled = fixedCellSize > 0;
                });
                fixedCellSize = fixedCellSizeProperty.get();
                fixedCellSizeEnabled = fixedCellSize > 0;

                // JDK-8144500:
                // When in fixed cell size mode, we must listen to the width of the virtual flow, so
                // that when it changes, we can appropriately add / remove cells that may or may not
                // be required (because we remove all cells that are not visible).
                VirtualFlow<TableRow<T>> virtualFlow = getVirtualFlow();
                if (virtualFlow != null) {
                    registerChangeListener(virtualFlow.widthProperty(), e -> tableView.requestLayout());
                }
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case SELECTED_ITEMS: {
                if (getTableView().getSelectionModel() != null) {
                    // FIXME this could be optimised to iterate over cellsMap only
                    // (selectedCells could be big, cellsMap is much smaller)
                    List<Node> selection = new ArrayList<>();
                    int index = getSkinnable().getIndex();
                    for (TablePosition<T,?> pos : getTableView().getSelectionModel().getSelectedCells()) {
                        if (pos.getRow() == index) {
                            TableColumn<T,?> column = pos.getTableColumn();
                            if (column == null) {
                                /* This is the row-based case */
                                column = getTableView().getVisibleLeafColumn(0);
                            }
                            TableCell<T,?> cell = cellsMap.get(column).get();
                            if (cell != null) selection.add(cell);
                        }
                        return FXCollections.observableArrayList(selection);
                    }
                }
                return FXCollections.observableArrayList();
            }
            case CELL_AT_ROW_COLUMN: {
                int colIndex = (Integer)parameters[1];
                TableColumn<T,?> column = getTableView().getVisibleLeafColumn(colIndex);
                if (cellsMap.containsKey(column)) {
                    return cellsMap.get(column).get();
                }
                return null;
            }
            case FOCUS_ITEM: {
                TableViewFocusModel<T> fm = getTableView().getFocusModel();
                TablePosition<T,?> focusedCell = fm.getFocusedCell();
                TableColumn<T,?> column = focusedCell.getTableColumn();
                if (column == null) {
                    /* This is the row-based case */
                    column = getTableView().getVisibleLeafColumn(0);
                }
                if (cellsMap.containsKey(column)) {
                    return cellsMap.get(column).get();
                }
                return null;
            }
            default:
                return super.queryAccessibleAttribute(attribute, parameters);
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected TableCell<T, ?> createCell(TableColumnBase tcb) {
        TableColumn tableColumn = (TableColumn<T,?>) tcb;
        TableCell cell = (TableCell) tableColumn.getCellFactory().call(tableColumn);

        // we set it's TableColumn, TableView and TableRow
        cell.updateTableColumn(tableColumn);
        cell.updateTableView(tableColumn.getTableView());
        cell.updateTableRow(getSkinnable());

        return cell;
    }

    /** {@inheritDoc} */
    @Override protected ObservableList<TableColumn<T, ?>> getVisibleLeafColumns() {
        return getTableView() == null ? FXCollections.emptyObservableList() : getTableView().getVisibleLeafColumns();
    }

    /** {@inheritDoc} */
    @Override protected void updateCell(TableCell<T, ?> cell, TableRow<T> row) {
        cell.updateTableRow(row);
    }

    /** {@inheritDoc} */
    @Override protected TableColumn<T, ?> getTableColumn(TableCell<T, ?> cell) {
        return cell.getTableColumn();
    }

    private TableView<T> getTableView() {
        return getSkinnable().getTableView();
    }

    // test-only
    TableViewSkin<T> getTableViewSkin() {
        TableView<T> tableView = getSkinnable().getTableView();
        if (tableView != null && tableView.getSkin() instanceof TableViewSkin) {
            return (TableViewSkin)tableView.getSkin();
        }
        return null;
    }
}

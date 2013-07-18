/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Callback;

import com.sun.javafx.tk.Toolkit;
import org.junit.Before;
import org.junit.Test;

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.*;

/**
 */
public class TableCellTest {
    private TableCell<String,String> cell;
    private TableView<String> table;
    private ObservableList<String> model;

    @Before public void setup() {
        cell = new TableCell<String,String>();
        model = FXCollections.observableArrayList("Four", "Five", "Fear"); // "Flop", "Food", "Fizz"
        table = new TableView<String>(model);
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void styleClassIs_table_cell_byDefault() {
        assertStyleClassContains(cell, "table-cell");
    }

    // The item should be null by default because the index is -1 by default
    @Test public void itemIsNullByDefault() {
        assertNull(cell.getItem());
    }

    /*********************************************************************
     * Tests for the tableView property                                   *
     ********************************************************************/

    @Test public void tableViewIsNullByDefault() {
        assertNull(cell.getTableView());
        assertNull(cell.tableViewProperty().get());
    }

    @Test public void updateTableViewUpdatesTableView() {
        cell.updateTableView(table);
        assertSame(table, cell.getTableView());
        assertSame(table, cell.tableViewProperty().get());
    }

    @Test public void canSetTableViewBackToNull() {
        cell.updateTableView(table);
        cell.updateTableView(null);
        assertNull(cell.getTableView());
        assertNull(cell.tableViewProperty().get());
    }

    @Test public void tableViewPropertyReturnsCorrectBean() {
        assertSame(cell, cell.tableViewProperty().getBean());
    }

    @Test public void tableViewPropertyNameIs_tableView() {
        assertEquals("tableView", cell.tableViewProperty().getName());
    }

    @Test public void updateTableViewWithNullFocusModelResultsInNoException() {
        cell.updateTableView(table);
        table.setFocusModel(null);
        cell.updateTableView(new TableView());
    }

    @Test public void updateTableViewWithNullFocusModelResultsInNoException2() {
        table.setFocusModel(null);
        cell.updateTableView(table);
        cell.updateTableView(new TableView());
    }

    @Test public void updateTableViewWithNullFocusModelResultsInNoException3() {
        cell.updateTableView(table);
        TableView table2 = new TableView();
        table2.setFocusModel(null);
        cell.updateTableView(table2);
    }

    @Test public void updateTableViewWithNullSelectionModelResultsInNoException() {
        cell.updateTableView(table);
        table.setSelectionModel(null);
        cell.updateTableView(new TableView());
    }

    @Test public void updateTableViewWithNullSelectionModelResultsInNoException2() {
        table.setSelectionModel(null);
        cell.updateTableView(table);
        cell.updateTableView(new TableView());
    }

    @Test public void updateTableViewWithNullSelectionModelResultsInNoException3() {
        cell.updateTableView(table);
        TableView table2 = new TableView();
        table2.setSelectionModel(null);
        cell.updateTableView(table2);
    }

    @Test public void updateTableViewWithNullItemsResultsInNoException() {
        cell.updateTableView(table);
        table.setItems(null);
        cell.updateTableView(new TableView());
    }

    @Test public void updateTableViewWithNullItemsResultsInNoException2() {
        table.setItems(null);
        cell.updateTableView(table);
        cell.updateTableView(new TableView());
    }

    @Test public void updateTableViewWithNullItemsResultsInNoException3() {
        cell.updateTableView(table);
        TableView table2 = new TableView();
        table2.setItems(null);
        cell.updateTableView(table2);
    }

    private int rt_29923_count = 0;
    @Test public void test_rt_29923() {
        // setup test
        cell = new TableCell<String,String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                rt_29923_count++;
            }
        };
        TableColumn col = new TableColumn("TEST");
        col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures, ObservableValue>() {
            @Override public ObservableValue call(TableColumn.CellDataFeatures param) {
                return null;
            }
        });
        table.getColumns().add(col);
        cell.updateTableColumn(col);
        cell.updateTableView(table);

        // set index to 0, which results in the cell value factory returning
        // null, but because the number of items is 3, this is a valid value
        cell.updateIndex(0);
        assertNull(cell.getItem());
        assertFalse(cell.isEmpty());
        assertEquals(1, rt_29923_count);

        cell.updateIndex(1);
        assertNull(cell.getItem());
        assertFalse(cell.isEmpty());
        assertEquals(1, rt_29923_count);    // even though the index has changed,
                                            // the item is the same, so we don't
                                            // update the cell item.
    }
}

/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.junit.Before;
import org.junit.Test;

import static javafx.scene.control.ControlTestUtils.assertStyleClassContains;
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
}

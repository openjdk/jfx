/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.tk.Toolkit;

import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableCellShim;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableCellSkin;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

/**
 */
public class TableCellTest {
    private TableCell<String,String> cell;
    private TableView<String> table;
    private TableColumn<String, String> editingColumn;
    private TableRow<String> row;
    private ObservableList<String> model;
    private StageLoader stageLoader;

    @Before public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });

        cell = new TableCell<String,String>();
        model = FXCollections.observableArrayList("Four", "Five", "Fear"); // "Flop", "Food", "Fizz"
        table = new TableView<String>(model);
        editingColumn = new TableColumn<>("TEST");

        row = new TableRow<>();
    }

    @After
    public void cleanup() {
        if (stageLoader != null) stageLoader.dispose();
        Thread.currentThread().setUncaughtExceptionHandler(null);
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
        cell = new TableCellShim<String,String>() {
            @Override public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                rt_29923_count++;
            }
        };
        TableColumn col = new TableColumn("TEST");
        col.setCellValueFactory(param -> null);
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

        // This test used to be as shown below....but due to RT-33108, it changed
        // to the enabled code beneath. Refer to the first comment in RT-33108
        // for more detail, but in short we can't optimise and not call updateItem
        // when the new and old items are the same - doing so means we can end
        // up with bad bindings, etc in the individual cells (in other words,
        // even if their item has not changed, the rest of their state may have)
//        assertEquals(1, rt_29923_count);    // even though the index has changed,
//                                            // the item is the same, so we don't
//                                            // update the cell item.
        assertEquals(2, rt_29923_count);
    }

    @Test public void test_rt_33106() {
        cell.updateTableView(table);
        table.setItems(null);
        cell.updateIndex(1);
    }

    @Test public void test_rt36715_idIsNullAtStartup() {
        assertNull(cell.getId());
    }

    @Test public void test_rt36715_idIsSettable() {
        cell.setId("test-id");
        assertEquals("test-id", cell.getId());
    }

    @Test public void test_rt36715_columnHeaderIdMirrorsTableColumnId_setIdBeforeHeaderInstantiation() {
        test_rt36715_cellPropertiesMirrorTableColumnProperties(true, true, false, false, false);
    }

    @Test public void test_rt36715_columnHeaderIdMirrorsTableColumnId_setIdAfterHeaderInstantiation() {
        test_rt36715_cellPropertiesMirrorTableColumnProperties(true, false, false, false, false);
    }

    @Test public void test_rt36715_columnHeaderIdMirrorsTableColumnId_setIdBeforeHeaderInstantiation_setValueOnCell() {
        test_rt36715_cellPropertiesMirrorTableColumnProperties(true, true, false, false, true);
    }

    @Test public void test_rt36715_columnHeaderIdMirrorsTableColumnId_setIdAfterHeaderInstantiation_setValueOnCell() {
        test_rt36715_cellPropertiesMirrorTableColumnProperties(true, false, false, false, true);
    }

    @Test public void test_rt36715_styleIsEmptyStringAtStartup() {
        assertEquals("", cell.getStyle());
    }

    @Test public void test_rt36715_styleIsSettable() {
        cell.setStyle("-fx-border-color: red");
        assertEquals("-fx-border-color: red", cell.getStyle());
    }

    @Test public void test_rt36715_columnHeaderStyleMirrorsTableColumnStyle_setStyleBeforeHeaderInstantiation() {
        test_rt36715_cellPropertiesMirrorTableColumnProperties(false, false, true, true, false);
    }

    @Test public void test_rt36715_columnHeaderStyleMirrorsTableColumnStyle_setStyleAfterHeaderInstantiation() {
        test_rt36715_cellPropertiesMirrorTableColumnProperties(false, false, true, false, false);
    }

    @Test public void test_rt36715_columnHeaderStyleMirrorsTableColumnStyle_setStyleBeforeHeaderInstantiation_setValueOnCell() {
        test_rt36715_cellPropertiesMirrorTableColumnProperties(false, false, true, true, true);
    }

    @Test public void test_rt36715_columnHeaderStyleMirrorsTableColumnStyle_setStyleAfterHeaderInstantiation_setValueOnCell() {
        test_rt36715_cellPropertiesMirrorTableColumnProperties(false, false, true, false, true);
    }

    private void test_rt36715_cellPropertiesMirrorTableColumnProperties(
            boolean setId, boolean setIdBeforeHeaderInstantiation,
            boolean setStyle, boolean setStyleBeforeHeaderInstantiation,
            boolean setValueOnCell) {

        TableColumn column = new TableColumn("Column");
        table.getColumns().add(column);

        if (setId && setIdBeforeHeaderInstantiation) {
            column.setId("test-id");
        }
        if (setStyle && setStyleBeforeHeaderInstantiation) {
            column.setStyle("-fx-border-color: red");
        }

        StageLoader sl = new StageLoader(table);
        TableCell cell = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 0);

        // the default value takes precedence over the value set in the TableColumn
        if (setValueOnCell) {
            if (setId) {
                cell.setId("cell-id");
            }
            if (setStyle) {
                cell.setStyle("-fx-border-color: green");
            }
        }

        if (setId && ! setIdBeforeHeaderInstantiation) {
            column.setId("test-id");
        }
        if (setStyle && ! setStyleBeforeHeaderInstantiation) {
            column.setStyle("-fx-border-color: red");
        }

        if (setId) {
            if (setValueOnCell) {
                assertEquals("cell-id", cell.getId());
            } else {
                assertEquals("test-id", cell.getId());
            }
        }
        if (setStyle) {
            if (setValueOnCell) {
                assertEquals("-fx-border-color: green", cell.getStyle());
            } else {
                assertEquals("-fx-border-color: red", cell.getStyle());
            }
        }

        sl.dispose();
    }

    @Test public void test_jdk_8151524() {
        TableCell cell = new TableCell();
        cell.setSkin(new TableCellSkin(cell));
    }

    /**
     * Table: Editable<br>
     * Row: Not editable<br>
     * Column: Editable<br>
     * Expected: Cell can not be edited because the row is not editable.
     */
    @Test
    public void testCellInUneditableRowIsNotEditable() {
        table.setEditable(true);
        row.setEditable(false);

        TableColumn<String, String> tableColumn = new TableColumn<>();
        tableColumn.setEditable(true);
        table.getColumns().add(tableColumn);

        cell.updateTableColumn(tableColumn);
        cell.updateTableRow(row);
        cell.updateTableView(table);

        cell.updateIndex(0);
        cell.startEdit();

        assertFalse(cell.isEditing());
    }

    /**
     * Table: Not editable<br>
     * Row: Editable<br>
     * Column: Editable<br>
     * Expected: Cell can not be edited because the table is not editable.
     */
    @Test
    public void testCellInUneditableTableIsNotEditable() {
        table.setEditable(false);
        row.setEditable(true);

        TableColumn<String, String> tableColumn = new TableColumn<>();
        tableColumn.setEditable(true);
        table.getColumns().add(tableColumn);

        cell.updateTableColumn(tableColumn);
        cell.updateTableRow(row);
        cell.updateTableView(table);

        cell.updateIndex(0);
        cell.startEdit();

        assertFalse(cell.isEditing());
    }

    /**
     * Table: Editable<br>
     * Row: Editable<br>
     * Column: Not editable<br>
     * Expected: Cell can not be edited because the column is not editable.
     */
    @Test
    public void testCellInUneditableColumnIsNotEditable() {
        table.setEditable(true);
        row.setEditable(true);

        TableColumn<String, String> tableColumn = new TableColumn<>();
        tableColumn.setEditable(false);
        table.getColumns().add(tableColumn);

        cell.updateTableColumn(tableColumn);
        cell.updateTableRow(row);
        cell.updateTableView(table);

        cell.updateIndex(0);
        cell.startEdit();

        assertFalse(cell.isEditing());
    }

    /**
     * Basic config of table-/cell to allow testing of editEvents:
     * table is editable, has editingColumn and cell is configured with table and column.
     */
    private void setupForEditing() {
        table.setEditable(true);
        table.getColumns().add(editingColumn);
        // FIXME: default cell (of tableColumn) needs not-null value for firing cancel
        editingColumn.setCellValueFactory(cc -> new SimpleObjectProperty<>(""));

        cell.updateTableView(table);
        cell.updateTableColumn(editingColumn);
    }

    @Test
    public void testEditCancelEventAfterCancelOnCell() {
        setupForEditing();
        int editingIndex = 1;
        cell.updateIndex(editingIndex);
        table.edit(editingIndex, editingColumn);
        TablePosition<?, ?> editingPosition = table.getEditingCell();
        List<CellEditEvent<?, ?>> events = new ArrayList<>();
        editingColumn.setOnEditCancel(events::add);
        cell.cancelEdit();
        assertEquals("column must have received editCancel", 1, events.size());
        assertEquals("editing location of cancel event", editingPosition, events.get(0).getTablePosition());
    }

    @Test
    public void testEditCancelEventAfterCancelOnTable() {
        setupForEditing();
        int editingIndex = 1;
        cell.updateIndex(editingIndex);
        table.edit(editingIndex, editingColumn);
        TablePosition<?, ?> editingPosition = table.getEditingCell();
        List<CellEditEvent<?, ?>> events = new ArrayList<>();
        editingColumn.setOnEditCancel(events::add);
        table.edit(-1, null);
        assertEquals("column must have received editCancel", 1, events.size());
        assertEquals("editing location of cancel event", editingPosition, events.get(0).getTablePosition());
    }

    @Test
    public void testEditCancelEventAfterCellReuse() {
        setupForEditing();
        int editingIndex = 1;
        cell.updateIndex(editingIndex);
        table.edit(editingIndex, editingColumn);
        TablePosition<?, ?> editingPosition = table.getEditingCell();
        List<CellEditEvent<?, ?>> events = new ArrayList<>();
        editingColumn.setOnEditCancel(events::add);
        cell.updateIndex(0);
        assertEquals("column must have received editCancel", 1, events.size());
        assertEquals("editing location of cancel event", editingPosition, events.get(0).getTablePosition());
    }

    @Test
    public void testEditCancelEventAfterModifyItems() {
        setupForEditing();
        stageLoader = new StageLoader(table);
        int editingIndex = 1;
        table.edit(editingIndex, editingColumn);
        TablePosition<?, ?> editingPosition = table.getEditingCell();
        List<CellEditEvent<?, ?>> events = new ArrayList<>();
        editingColumn.setOnEditCancel(events::add);
        table.getItems().add(0, "added");
        Toolkit.getToolkit().firePulse();
        assertEquals("column must have received editCancel", 1, events.size());
        assertEquals("editing location of cancel event", editingPosition, events.get(0).getTablePosition());
    }

    /**
     * Test that removing the editing item implicitly cancels an ongoing
     * edit and fires a correct cancel event.
     */
    @Test
    public void testEditCancelEventAfterRemoveEditingItem() {
        setupForEditing();
        stageLoader = new StageLoader(table);
        int editingIndex = 1;
        table.edit(editingIndex, editingColumn);
        TablePosition<?, ?> editingPosition = table.getEditingCell();
        List<CellEditEvent<?, ?>> events = new ArrayList<>();
        editingColumn.setOnEditCancel(events::add);
        table.getItems().remove(editingIndex);
        Toolkit.getToolkit().firePulse();
        assertNull("sanity: editing terminated on items modification", table.getEditingCell());
        assertEquals("column must have received editCancel", 1, events.size());
        assertEquals("editing location of cancel event", editingPosition, events.get(0).getTablePosition());
    }

    /**
     * Test that removing the editing item does not cause a memory leak.
     */
    @Test
    public void testEditCancelMemoryLeakAfterRemoveEditingItem() {
        TableView<MenuItem> table = new TableView<>(FXCollections.observableArrayList(
                new MenuItem("some"), new MenuItem("other")));
        TableColumn<MenuItem, String> editingColumn = new TableColumn<>("Text");
        editingColumn.setCellValueFactory(cc -> new SimpleObjectProperty<>(""));
        table.setEditable(true);
        table.getColumns().add(editingColumn);
        stageLoader = new StageLoader(table);
        int editingIndex = 1;
        MenuItem editingItem = table.getItems().get(editingIndex);
        WeakReference<MenuItem> itemRef = new WeakReference<>(editingItem);
        table.edit(editingIndex, editingColumn);
        table.getItems().remove(editingIndex);
        editingItem = null;
        Toolkit.getToolkit().firePulse();
        attemptGC(itemRef);
        assertEquals("item must be gc'ed", null, itemRef.get());
    }

    @Test
    public void testEditStartFiresEvent() {
        setupForEditing();
        cell.updateIndex(1);
        List<CellEditEvent<?, ?>> events = new ArrayList<>();
        editingColumn.setOnEditStart(events::add);
        cell.startEdit();
        assertEquals("startEdit must fire", 1, events.size());
    }
    
    @Test
    public void testEditStartDoesNotFireEventWhileEditing() {
        setupForEditing();
        cell.updateIndex(1);
        cell.startEdit();
        List<CellEditEvent<?, ?>> events = new ArrayList<>();
        editingColumn.setOnEditStart(events::add);
        cell.startEdit();
        assertEquals("startEdit must not fire while editing", 0, events.size());
    }


    /**
     * Test that cell.cancelEdit can switch table editing off
     * even if a subclass violates its contract.
     *
     * For details, see https://bugs.openjdk.java.net/browse/JDK-8265206
     *
     */
    @Test
    public void testMisbehavingCancelEditTerminatesEdit() {
        // setup for editing
        TableCell<String, String> cell = new MisbehavingOnCancelTableCell<>();
        table.setEditable(true);
        TableColumn<String, String> editingColumn = new TableColumn<>("TEST");
        editingColumn.setCellValueFactory(param -> null);
        table.getColumns().add(editingColumn);
        cell.updateTableView(table);
        cell.updateTableColumn(editingColumn);
        // test editing: first round
        // switch cell off editing by table api
        int editingIndex = 1;
        int intermediate = 0;
        cell.updateIndex(editingIndex);
        table.edit(editingIndex, editingColumn);
        assertTrue("sanity: ", cell.isEditing());
        try {
            table.edit(intermediate, editingColumn);
        } catch (Exception ex) {
            // just catching to test in finally
        } finally {
            assertFalse("cell must not be editing", cell.isEditing());
            assertEquals("table must be editing at intermediate index", intermediate, table.getEditingCell().getRow());
        }
        // test editing: second round
        // switch cell off editing by cell api
        table.edit(editingIndex, editingColumn);
        assertTrue("sanity: ", cell.isEditing());
        try {
            cell.cancelEdit();
        } catch (Exception ex) {
            // just catching to test in finally
        } finally {
            assertFalse("cell must not be editing", cell.isEditing());
            assertNull("table editing must be cancelled by cell", table.getEditingCell());
        }
    }

    public static class MisbehavingOnCancelTableCell<S, T> extends TableCell<S, T> {

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            throw new RuntimeException("violating contract");
        }

    }
}

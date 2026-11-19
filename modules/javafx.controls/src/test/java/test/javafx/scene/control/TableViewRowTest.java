/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

/**
 * Contains TableViewRow tests.
 */
public class TableViewRowTest {

    StageLoader stageLoader;

    @AfterEach
    public void after() {
        if (stageLoader != null) {
            stageLoader.dispose();
        }
    }

    /** TableView with cell selection enabled should not select TableRows, see JDK-8292353 */
    @Test
    public void test_TableView_select_all() {
        TableView<String> table = ControlUtils.createTableView();

        stageLoader = new StageLoader(table);
        TableView.TableViewSelectionModel<String> sm = table.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.clearSelection();

        TableColumn<String,?> col0 = table.getColumns().get(0);
        TableColumn<String,?> col1 = table.getColumns().get(1);
        TableColumn<String,?> col2 = table.getColumns().get(2);
        TableRow row = ControlUtils.getTableRow(table, 0);
        TableCell c0 = ControlUtils.getTableCell(table, 0, 0);
        TableCell c1 = ControlUtils.getTableCell(table, 0, 1);
        TableCell c2 = ControlUtils.getTableCell(table, 0, 2);

        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select all cells in the first row
        sm.select(0, col0);
        sm.select(0, col1);
        sm.select(0, col2);

        assertTrue(c0.isSelected());
        assertTrue(c1.isSelected());
        assertTrue(c2.isSelected());
        assertFalse(row.isSelected());
    }

    /**
     * TableView with cell selection enabled should not select TableRows,
     * even when selected as a group, see JDK-8292353
     */
    @Test
    public void test_TableView_select_all_as_group() {
        TableView<String> table = ControlUtils.createTableView();

        stageLoader = new StageLoader(table);
        TableView.TableViewSelectionModel<String> sm = table.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.clearSelection();

        TableColumn<String,?> col0 = table.getColumns().get(0);
        TableColumn<String,?> col1 = table.getColumns().get(1);
        TableColumn<String,?> col2 = table.getColumns().get(2);
        TableRow row = ControlUtils.getTableRow(table, 0);
        TableCell c0 = ControlUtils.getTableCell(table, 0, 0);
        TableCell c1 = ControlUtils.getTableCell(table, 0, 1);
        TableCell c2 = ControlUtils.getTableCell(table, 0, 2);

        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select all cells in the first row as a group
        sm.select(0, null);

        assertTrue(c0.isSelected());
        assertTrue(c1.isSelected());
        assertTrue(c2.isSelected());
        assertFalse(row.isSelected());
    }

    /** TableView with cell selection enabled should not select TableRows, see JDK-8292353 */
    @Test
    public void test_TableView_select_all_but_one() {
        TableView<String> table = ControlUtils.createTableView();

        stageLoader = new StageLoader(table);
        TableView.TableViewSelectionModel<String> sm = table.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.clearSelection();

        TableColumn<String,?> col1 = table.getColumns().get(1);
        TableRow row = ControlUtils.getTableRow(table, 0);
        TableCell c0 = ControlUtils.getTableCell(table, 0, 0);
        TableCell c1 = ControlUtils.getTableCell(table, 0, 1);
        TableCell c2 = ControlUtils.getTableCell(table, 0, 2);

        assertFalse(c0.isSelected());
        assertFalse(c1.isSelected());
        assertFalse(c2.isSelected());
        assertFalse(row.isSelected());

        // select 0:0 and 0:2
        sm.select(0, null);
        sm.clearSelection(0, col1);

        assertTrue(c0.isSelected());
        assertFalse(c1.isSelected());
        assertTrue(c2.isSelected());
        assertFalse(row.isSelected());
    }

    /**
     * Same index and underlying item should not cause the updateItem(..) method to be called.
     */
    @Test
    public void testSameIndexAndItemShouldNotUpdateItem() {
        AtomicInteger counter = new AtomicInteger();

        TableView<String> table = ControlUtils.createTableView();
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                counter.incrementAndGet();
                super.updateItem(item, empty);
            }
        });

        stageLoader = new StageLoader(table);

        counter.set(0);
        TableRow<String> row = ControlUtils.getTableRow(table, 0);
        row.updateIndex(0);

        assertEquals(0, counter.get());
    }

    /**
     * The contract of a {@link TableRow} is that isItemChanged(..)
     * is called when the index is 'changed' to the same number as the old one, to evaluate if we need to call
     * updateItem(..).
     */
    @Test
    public void testSameIndexIsItemsChangedShouldBeCalled() {
        AtomicBoolean isItemChangedCalled = new AtomicBoolean();

        TableView<String> table = ControlUtils.createTableView();
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected boolean isItemChanged(String oldItem, String newItem) {
                isItemChangedCalled.set(true);
                return super.isItemChanged(oldItem, newItem);
            }
        });

        stageLoader = new StageLoader(table);

        TableRow<String> row = ControlUtils.getTableRow(table, 0);
        row.updateIndex(0);

        assertTrue(isItemChangedCalled.get());
    }

    @Test
    void testUpdateRowIndexManually() {
        TableView<String> table = ControlUtils.createTableView();

        TableRow<String> row = new TableRow<>();
        row.updateTableView(table);

        stageLoader = new StageLoader(row);

        row.updateIndex(0);

        List<TableCell<String, String>> cells = row.getChildrenUnmodifiable().stream()
                .filter(TableCell.class::isInstance).map(e -> (TableCell<String, String>) e).toList();
        for (TableCell<String, String> cell : cells) {
            assertEquals(0, cell.getIndex());
        }

        row.updateIndex(1);

        cells = row.getChildrenUnmodifiable().stream()
                .filter(TableCell.class::isInstance).map(e -> (TableCell<String, String>) e).toList();
        for (TableCell<String, String> cell : cells) {
            assertEquals(1, cell.getIndex());
        }
    }

    @Test
    public void testEditOnTableViewStartsEditingInRow() {
        TableView<String> table = createEditableTableView();
        TableRow<String> row = createRow(table, 1);

        table.edit(1, null);

        assertTrue(row.isEditing());
    }

    @Test
    public void testEditOnTableViewWithAnotherIndexDoesNotStartEditingInRow() {
        TableView<String> table = createEditableTableView();
        TableRow<String> row = createRow(table, 1);

        table.edit(0, null);

        assertFalse(row.isEditing());
    }

    @Test
    public void testStartEditOnNonEditableTableViewDoesNothing() {
        TableView<String> table = createEditableTableView();
        table.setEditable(false);
        TableRow<String> row = createRow(table, 1);

        row.startEdit();

        assertFalse(row.isEditing());
        assertNull(table.getEditingCell());
    }

    @Test
    public void testStartEditOnNonEditableRowDoesNothing() {
        TableView<String> table = createEditableTableView();
        TableRow<String> row = createRow(table, 1);
        row.setEditable(false);

        row.startEdit();

        assertFalse(row.isEditing());
    }

    @Test
    public void testStartEditWithoutTableViewDoesNotThrow() {
        TableRow<String> row = new TableRow<>();
        row.updateIndex(1);

        row.startEdit();
    }

    @Test
    public void testStartEditFiresEditStartEvent() {
        TableView<String> table = createEditableTableView();
        TableRow<String> row = createRow(table, 1);

        AtomicInteger counter = new AtomicInteger();
        table.addEventHandler(TableView.editStartEvent(), event -> counter.incrementAndGet());

        row.startEdit();

        assertTrue(row.isEditing());
        assertEquals(1, counter.get());
    }

    @Test
    public void testCommitEditFiresEditCommitEventWithOldAndNewValue() {
        TableView<String> table = createEditableTableView();
        TableRow<String> row = createRow(table, 1);
        row.startEdit();

        List<TableView.EditEvent<String>> events = new ArrayList<>();
        table.addEventHandler(TableView.<String>editCommitEvent(), events::add);

        row.commitEdit("Watermelon");

        assertEquals(1, events.size());
        assertEquals("Oranges", events.get(0).getOldValue());
        assertEquals("Watermelon", events.get(0).getNewValue());
        assertSame(table, events.get(0).getSource());
    }

    @Test
    public void testCommitEditUpdatesTheRowAndStopsEditing() {
        TableView<String> table = createEditableTableView();
        TableRow<String> row = createRow(table, 1);
        row.startEdit();

        row.commitEdit("Watermelon");

        assertEquals("Watermelon", row.getItem());
        assertFalse(row.isEditing());
        assertNull(table.getEditingCell());
    }

    @Test
    public void testCommitEditWithoutEditingDoesNothing() {
        TableView<String> table = createEditableTableView();
        TableRow<String> row = createRow(table, 1);

        AtomicInteger counter = new AtomicInteger();
        table.addEventHandler(TableView.editCommitEvent(), event -> counter.incrementAndGet());

        row.commitEdit("Watermelon");

        assertEquals("Oranges", row.getItem());
        assertEquals(0, counter.get());
    }

    @Test
    public void testCancelEditFiresEditCancelEventAndStopsEditing() {
        TableView<String> table = createEditableTableView();
        TableRow<String> row = createRow(table, 1);
        row.startEdit();

        AtomicInteger counter = new AtomicInteger();
        table.addEventHandler(TableView.editCancelEvent(), event -> counter.incrementAndGet());

        row.cancelEdit();

        assertEquals(1, counter.get());
        assertFalse(row.isEditing());
        assertNull(table.getEditingCell());
        assertEquals("Oranges", row.getItem());
    }

    @Test
    public void testCancelEditWithoutEditingDoesNothing() {
        TableView<String> table = createEditableTableView();
        TableRow<String> row = createRow(table, 1);

        AtomicInteger counter = new AtomicInteger();
        table.addEventHandler(TableView.editCancelEvent(), event -> counter.incrementAndGet());

        row.cancelEdit();

        assertEquals(0, counter.get());
    }

    @Test
    public void testEditEventsAreSubTypesOfEditAnyEvent() {
        TableView<String> table = createEditableTableView();
        TableRow<String> row = createRow(table, 1);

        AtomicInteger counter = new AtomicInteger();
        table.addEventHandler(TableView.editAnyEvent(), event -> counter.incrementAndGet());

        row.startEdit();
        row.commitEdit("Watermelon");

        assertEquals(2, counter.get());
    }

    @Test
    public void testStartEditOnEmptyRowDoesNotFireEditStartEvent() {
        TableView<String> table = createEditableTableView();
        // an index past the items, so that the row is empty
        TableRow<String> row = createRow(table, table.getItems().size());

        AtomicInteger counter = new AtomicInteger();
        table.addEventHandler(TableView.editStartEvent(), event -> counter.incrementAndGet());

        row.startEdit();

        assertTrue(row.isEmpty());
        assertFalse(row.isEditing());
        assertEquals(0, counter.get());
    }

    private static TableView<String> createEditableTableView() {
        TableView<String> table =
                new TableView<>(FXCollections.observableArrayList("Apples", "Oranges", "Pears"));
        table.getColumns().add(new TableColumn<>("C0"));
        table.setEditable(true);
        return table;
    }

    private static TableRow<String> createRow(TableView<String> table, int index) {
        TableRow<String> row = new TableRow<>();
        row.updateTableView(table);
        row.updateIndex(index);
        return row;
    }

    /**
     * A row with a null item at a valid index is not empty, so it must be cleaned up when its index is moved off-range.
     */
    @Test
    public void testNullItemUpdateIndexNegative() {
        TableRow<String> row = setupNullItemRow();
        row.updateIndex(0);
        assertInRangeNullItemState(row, 0);
        row.updateIndex(-1);
        assertOffRangeState(row, -1);
    }

    @Test
    public void testNullItemUpdateIndexOffRange() {
        TableRow<String> row = setupNullItemRow();
        row.updateIndex(0);
        assertInRangeNullItemState(row, 0);
        row.updateIndex(nullItemModel.size());
        assertOffRangeState(row, nullItemModel.size());
    }

    private ObservableList<String> nullItemModel;

    private TableRow<String> setupNullItemRow() {
        nullItemModel = FXCollections.observableArrayList(null, "Oranges", "Pears");
        TableRow<String> row = new TableRow<>();
        row.updateTableView(new TableView<>(nullItemModel));
        return row;
    }

    private void assertInRangeNullItemState(TableRow<String> row, int index) {
        assertEquals(index, row.getIndex(), "in range index");
        assertNull(row.getItem(), "in range row item must be null");
        assertFalse(row.isEmpty(), "in range row with null item must not be empty");
    }

    private void assertOffRangeState(TableRow<String> row, int index) {
        assertEquals(index, row.getIndex(), "off range index");
        assertNull(row.getItem(), "off range row item must be null");
        assertTrue(row.isEmpty(), "off range row must be empty");
    }
}

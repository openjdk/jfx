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

package javafx.scene.control;

import java.util.List;

import com.sun.javafx.scene.control.test.Person;
import com.sun.javafx.tk.Toolkit;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.scene.control.behavior.TableViewAnchorRetriever;
import com.sun.javafx.scene.control.infrastructure.KeyModifier;
import com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

import static org.junit.Assert.*;

//@Ignore("Disabling tests as they fail with OOM in continuous builds")
public class TableViewMouseInputTest {
    private TableView<String> tableView;
    private TableView.TableViewSelectionModel<?> sm;
    private TableView.TableViewFocusModel<String> fm;

    private final TableColumn<String, String> col0 = new TableColumn<>("col0");
    private final TableColumn<String, String> col1 = new TableColumn<>("col1");
    private final TableColumn<String, String> col2 = new TableColumn<>("col2");
    private final TableColumn<String, String> col3 = new TableColumn<>("col3");
    private final TableColumn<String, String> col4 = new TableColumn<>("col4");
    
    @Before public void setup() {
        tableView = new TableView<>();
        sm = tableView.getSelectionModel();
        fm = tableView.getFocusModel();
        
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);
        
        tableView.getItems().setAll("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
        tableView.getColumns().setAll(col0, col1, col2, col3, col4);
        
        sm.clearAndSelect(0);
    }
    
    @After public void tearDown() {
        if (tableView.getSkin() != null) {
            tableView.getSkin().dispose();
        }
    }
    
    /***************************************************************************
     * Util methods
     **************************************************************************/
    
    private String debug() {
        StringBuilder sb = new StringBuilder("Selected Cells: [");
        
        List<TablePosition> cells = sm.getSelectedCells();
        for (TablePosition<String,?> tp : cells) {
            sb.append("(");
            sb.append(tp.getRow());
            sb.append(",");
            sb.append(tp.getColumn());
            sb.append("), ");
        }
        
        sb.append("] \nFocus: (" + fm.getFocusedCell().getRow() + ", " + fm.getFocusedCell().getColumn() + ")");
        sb.append(" \nAnchor: (" + getAnchor().getRow() + ", " + getAnchor().getColumn() + ")");
        return sb.toString();
    }
    
    // Returns true if ALL indices are selected
    private boolean isSelected(int... indices) {
        for (int index : indices) {
            if (! sm.isSelected(index)) return false;
        }
        return true;
    }
    
    // Returns true if ALL indices are NOT selected
    private boolean isNotSelected(int... indices) {
        for (int index : indices) {
            if (sm.isSelected(index)) return false;
        }
        return true;
    }
    
    private TablePosition getAnchor() {
        return TableViewAnchorRetriever.getAnchor(tableView);
    }
    
    private boolean isAnchor(int row) {
        TablePosition tp = new TablePosition(tableView, row, null);
        return getAnchor() != null && getAnchor().equals(tp);
    }
    
    private boolean isAnchor(int row, int col) {
        TablePosition tp = new TablePosition(tableView, row, tableView.getColumns().get(col));
        return getAnchor() != null && getAnchor().equals(tp);
    }
    
    
    /***************************************************************************
     * Tests for specific bug reports
     **************************************************************************/
    
    @Test public void test_rt29833_mouse_select_upwards() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.clearAndSelect(9);
        
        // select all from 9 - 7
        VirtualFlowTestUtils.clickOnRow(tableView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(7,8,9));
        
        // select all from 9 - 7 - 5
        VirtualFlowTestUtils.clickOnRow(tableView, 5, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }
    
    @Test public void test_rt29833_mouse_select_downwards() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.clearAndSelect(5);
        
        // select all from 5 - 7
        VirtualFlowTestUtils.clickOnRow(tableView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(5,6,7));
        
        // select all from 5 - 7 - 9
        VirtualFlowTestUtils.clickOnRow(tableView, 9, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }

    private int rt30394_count = 0;
    @Test public void test_rt30394() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        final TableFocusModel fm = tableView.getFocusModel();
        fm.focus(-1);

        fm.focusedIndexProperty().addListener((observable, oldValue, newValue) -> {
            rt30394_count++;
            assertEquals(0, fm.getFocusedIndex());
        });

        // test pre-conditions
        assertEquals(0,rt30394_count);
        assertFalse(fm.isFocused(0));

        // select the first row with the shift key held down. The focus event
        // should only fire once - for focus on 0 (never -1 as this bug shows).
        VirtualFlowTestUtils.clickOnRow(tableView, 0, KeyModifier.SHIFT);
        assertEquals(1, rt30394_count);
        assertTrue(fm.isFocused(0));
    }

    @Test public void test_rt32119() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        // select rows 2, 3, and 4
        VirtualFlowTestUtils.clickOnRow(tableView, 2);
        VirtualFlowTestUtils.clickOnRow(tableView, 4, KeyModifier.SHIFT);
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertTrue(sm.isSelected(3));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));

        // now shift click on the 2nd row - this should make only row 2 be
        // selected. The bug is that row 4 remains selected also.
        VirtualFlowTestUtils.clickOnRow(tableView, 2, KeyModifier.SHIFT);
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertFalse(sm.isSelected(3));
        assertFalse(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt31020() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        // set all the columns to be very narrow (so the mouse click happens
        // to the right of them all, out in no-mans land
        tableView.setMinWidth(200);
        tableView.setPrefWidth(200);
        tableView.getColumns().clear();
        col0.setMaxWidth(10);
        tableView.getColumns().add(col0);

        // select rows 1, 2, 3, 4, and 5
        VirtualFlowTestUtils.clickOnRow(tableView, 1, true);
        VirtualFlowTestUtils.clickOnRow(tableView, 5, true, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertTrue(sm.isSelected(3));
        assertTrue(sm.isSelected(4));
        assertTrue(sm.isSelected(5));
    }

    @Test public void test_rt21444_up_cell() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }

        final int selectRow = 3;

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 3", sm.getSelectedItem());

        VirtualFlowTestUtils.clickOnRow(tableView, selectRow - 1, KeyModifier.SHIFT);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("Row 2", sm.getSelectedItem());
        assertEquals("Row 2", sm.getSelectedItems().get(0));
    }

    @Test public void test_rt21444_down_cell() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }

        final int selectRow = 3;

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 3", sm.getSelectedItem());

        VirtualFlowTestUtils.clickOnRow(tableView, selectRow + 1, KeyModifier.SHIFT);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("Row 4", sm.getSelectedItem());
        assertEquals("Row 4", sm.getSelectedItems().get(1));
    }

    @Test public void test_rt21444_up_row() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }

        final int selectRow = 3;

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 3", sm.getSelectedItem());

        VirtualFlowTestUtils.clickOnRow(tableView, selectRow - 1, true, KeyModifier.SHIFT);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("Row 2", sm.getSelectedItem());
        assertEquals("Row 2", sm.getSelectedItems().get(0));
    }

    @Test public void test_rt21444_down_row() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }

        final int selectRow = 3;

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(selectRow);

        assertEquals(selectRow, sm.getSelectedIndex());
        assertEquals("Row 3", sm.getSelectedItem());

        VirtualFlowTestUtils.clickOnRow(tableView, selectRow + 1, true, KeyModifier.SHIFT);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("Row 4", sm.getSelectedItem());
        assertEquals("Row 4", sm.getSelectedItems().get(1));
    }

    @Test public void test_rt32560_cell() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());

        VirtualFlowTestUtils.clickOnRow(tableView, 5, KeyModifier.SHIFT);
        assertEquals(5, sm.getSelectedIndex());
        assertEquals(5, fm.getFocusedIndex());
        assertEquals(6, sm.getSelectedItems().size());

        VirtualFlowTestUtils.clickOnRow(tableView, 0, KeyModifier.SHIFT);
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedItems().size());
    }

    @Test public void test_rt32560_row() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }

        StageLoader sl = new StageLoader(tableView);

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());

        VirtualFlowTestUtils.clickOnRow(tableView, 5, true, KeyModifier.SHIFT);
        assertEquals(5, sm.getSelectedIndex());
        assertEquals(5, fm.getFocusedIndex());
        assertEquals(6, sm.getSelectedItems().size());

        VirtualFlowTestUtils.clickOnRow(tableView, 0, true, KeyModifier.SHIFT);
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedItems().size());

        sl.dispose();
    }

    private int rt_30626_count = 0;
    @Test public void test_rt_30626() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }

        final TableSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);
        sm.clearAndSelect(0);

        tableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener) c -> {
            while (c.next()) {
                rt_30626_count++;
            }
        });

        assertEquals(0, rt_30626_count);
        VirtualFlowTestUtils.clickOnRow(tableView, 1);
        assertEquals(1, rt_30626_count);

        VirtualFlowTestUtils.clickOnRow(tableView, 1);
        assertEquals(1, rt_30626_count);
    }

    @Test public void test_rt_33897_rowSelection() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }

        final TableView.TableViewSelectionModel<String> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);

        VirtualFlowTestUtils.clickOnRow(tableView, 1);
        assertEquals(1, sm.getSelectedCells().size());

        TablePosition pos = sm.getSelectedCells().get(0);
        assertEquals(1, pos.getRow());
        assertNotNull(pos.getTableColumn());
    }

    @Test public void test_rt_33897_cellSelection() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }

        final TableView.TableViewSelectionModel<String> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        VirtualFlowTestUtils.clickOnRow(tableView, 1);
        assertEquals(1, sm.getSelectedCells().size());

        TablePosition pos = sm.getSelectedCells().get(0);
        assertEquals(1, pos.getRow());
        assertNotNull(pos.getTableColumn());
    }

    @Test public void test_rt_34649() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        final FocusModel fm = tableView.getFocusModel();
        sm.setSelectionMode(SelectionMode.SINGLE);

        assertFalse(sm.isSelected(4));
        assertFalse(fm.isFocused(4));
        VirtualFlowTestUtils.clickOnRow(tableView, 4, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(4));
        assertTrue(fm.isFocused(4));

        VirtualFlowTestUtils.clickOnRow(tableView, 4, KeyModifier.getShortcutKey());
        assertFalse(sm.isSelected(4));
        assertTrue(fm.isFocused(4));
    }

    @Test public void test_rt_35338() {
        tableView.getItems().setAll("Row 0");
        tableView.getColumns().setAll(col0);

        col0.setWidth(20);
        tableView.setMinWidth(1000);
        tableView.setMinWidth(1000);

        TableRow row = (TableRow) VirtualFlowTestUtils.getCell(tableView, 4);
        assertNotNull(row);
        assertNull(row.getItem());
        assertEquals(4, row.getIndex());

        MouseEventFirer mouse = new MouseEventFirer(row);
        mouse.fireMousePressAndRelease(1, 100, 10);
        mouse.dispose();
    }

    @Test public void test_rt_37069() {
        final int items = 8;
        tableView.getItems().clear();
        for (int i = 0; i < items; i++) {
            tableView.getItems().add("Row " + i);
        }
        tableView.setFocusTraversable(false);

        Button btn = new Button("Button");
        VBox vbox = new VBox(btn, tableView);

        StageLoader sl = new StageLoader(vbox);
        sl.getStage().requestFocus();
        btn.requestFocus();
        Toolkit.getToolkit().firePulse();
        Scene scene = sl.getStage().getScene();

        assertTrue(btn.isFocused());
        assertFalse(tableView.isFocused());

        ScrollBar vbar = VirtualFlowTestUtils.getVirtualFlowVerticalScrollbar(tableView);
        MouseEventFirer mouse = new MouseEventFirer(vbar);
        mouse.fireMousePressAndRelease();

        assertTrue(btn.isFocused());
        assertFalse(tableView.isFocused());

        sl.dispose();
    }

    @Test public void test_rt_38306_selectFirstRow() {
        test_rt_38306(false);
    }

    @Test public void test_rt_38306_selectFirstTwoRows() {
        test_rt_38306(true);
    }

    private void test_rt_38306(boolean selectTwoRows) {
        final ObservableList<Person> data =
                FXCollections.observableArrayList(
                        new Person("Jacob", "Smith", "jacob.smith@example.com"),
                        new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
                        new Person("Ethan", "Williams", "ethan.williams@example.com"),
                        new Person("Emma", "Jones", "emma.jones@example.com"),
                        new Person("Michael", "Brown", "michael.brown@example.com"));

        TableView<Person> table = new TableView<>();
        table.setItems(data);

        TableView.TableViewSelectionModel sm = table.getSelectionModel();
        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));

        TableColumn emailCol = new TableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        sm.select(0, firstNameCol);

        assertTrue(sm.isSelected(0, firstNameCol));
        assertEquals(1, sm.getSelectedCells().size());

        TableCell cell_0_0 = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 0);
        TableCell cell_0_1 = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 1);
        TableCell cell_0_2 = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 2);

        TableCell cell_1_0 = (TableCell) VirtualFlowTestUtils.getCell(table, 1, 0);
        TableCell cell_1_1 = (TableCell) VirtualFlowTestUtils.getCell(table, 1, 1);
        TableCell cell_1_2 = (TableCell) VirtualFlowTestUtils.getCell(table, 1, 2);

        MouseEventFirer mouse = selectTwoRows ?
                new MouseEventFirer(cell_1_2) : new MouseEventFirer(cell_0_2);

        mouse.fireMousePressAndRelease(KeyModifier.SHIFT);

        assertTrue(sm.isSelected(0, firstNameCol));
        assertTrue(sm.isSelected(0, lastNameCol));
        assertTrue(sm.isSelected(0, emailCol));

        if (selectTwoRows) {
            assertTrue(sm.isSelected(1, firstNameCol));
            assertTrue(sm.isSelected(1, lastNameCol));
            assertTrue(sm.isSelected(1, emailCol));
        }

        assertEquals(selectTwoRows ? 6 : 3, sm.getSelectedCells().size());

        assertTrue(cell_0_0.isSelected());
        assertTrue(cell_0_1.isSelected());
        assertTrue(cell_0_2.isSelected());

        if (selectTwoRows) {
            assertTrue(cell_1_0.isSelected());
            assertTrue(cell_1_1.isSelected());
            assertTrue(cell_1_2.isSelected());
        }
    }

    @Test public void test_rt_38464_rowSelection() {
        final ObservableList<Person> data =
                FXCollections.observableArrayList(
                        new Person("Jacob", "Smith", "jacob.smith@example.com"),
                        new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
                        new Person("Ethan", "Williams", "ethan.williams@example.com"),
                        new Person("Emma", "Jones", "emma.jones@example.com"),
                        new Person("Michael", "Brown", "michael.brown@example.com"));

        TableView<Person> table = new TableView<>();
        table.setItems(data);

        sm = table.getSelectionModel();
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));

        TableColumn emailCol = new TableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        sm.clearSelection();
        sm.select(0, lastNameCol);

        assertTrue(sm.isSelected(0, lastNameCol));
        assertEquals(1, sm.getSelectedCells().size());

        TableCell cell_4_2 = (TableCell) VirtualFlowTestUtils.getCell(table, 4, 1);

        MouseEventFirer mouse = new MouseEventFirer(cell_4_2);
        mouse.fireMousePressAndRelease(KeyModifier.SHIFT);

        // we are in row selection mode, so all cells in the selected rows should
        // be selected. We test this per-cell, but also per-row.
        for (int row = 0; row < 5; row++) {
            // test that the selection model is accurate
            assertTrue(sm.isSelected(row, firstNameCol));
            assertTrue(sm.isSelected(row, lastNameCol));
            assertTrue(sm.isSelected(row, emailCol));
            assertTrue(sm.isSelected(row));

            // and assert that the visuals are accurate
            // (TableCells should not be selected, but TableRows should be)
            for (int column = 0; column < 3; column++) {
                if (row == 4 && column == 2) {
                    // bizarrely cell (4,2), i.e. the bottom-right cell consisting
                    // of Michael Brown's email address, doesn't exist.
                    continue;
                }
                TableCell cell = (TableCell) VirtualFlowTestUtils.getCell(table, row, column);
                assertFalse("cell[row: " + row + ", column: " + column + "] is selected, but shouldn't be", cell.isSelected());
            }
            TableRow cell = (TableRow) VirtualFlowTestUtils.getCell(table, row);
            assertTrue(cell.isSelected());
        }
    }

    @Test public void test_rt_38464_cellSelection() {
        final ObservableList<Person> data =
                FXCollections.observableArrayList(
                        new Person("Jacob", "Smith", "jacob.smith@example.com"),
                        new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
                        new Person("Ethan", "Williams", "ethan.williams@example.com"),
                        new Person("Emma", "Jones", "emma.jones@example.com"),
                        new Person("Michael", "Brown", "michael.brown@example.com"));

        TableView<Person> table = new TableView<>();
        table.setItems(data);

        sm = table.getSelectionModel();
        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));

        TableColumn emailCol = new TableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        sm.clearSelection();
        sm.select(0, emailCol);
        table.getFocusModel().focus(0, emailCol);

        assertTrue(sm.isSelected(0, emailCol));
        assertEquals(1, sm.getSelectedCells().size());

        TableCell cell_4_2 = (TableCell) VirtualFlowTestUtils.getCell(table, 4, 2);
        assertEquals(emailCol, cell_4_2.getTableColumn());

        new MouseEventFirer(cell_4_2).fireMousePressAndRelease(KeyModifier.SHIFT);

        for (int row = 0; row < 5; row++) {
            // test that the selection model is accurate
            assertFalse(sm.isSelected(row, firstNameCol));
            assertFalse(sm.isSelected(row, lastNameCol));
            assertTrue(sm.isSelected(row, emailCol));
            assertFalse(sm.isSelected(row));

            // and assert that the visuals are accurate
            // (some TableCells should be selected, but TableRows should not be)
            for (int column = 0; column < 3; column++) {
                if (row == 4 && column == 2) {
                    // bizarrely cell (4,2), i.e. the bottom-right cell consisting
                    // of Michael Brown's email address, doesn't exist.
                    continue;
                }
                TableCell cell = (TableCell) VirtualFlowTestUtils.getCell(table, row, column);
                assertEquals(column == 2 ? true : false, cell.isSelected());
            }
            TableRow cell = (TableRow) VirtualFlowTestUtils.getCell(table, row);
            assertFalse(cell.isSelected());
        }
    }

    @Test public void test_rt_38464_selectedColumnChangesWhenCellsInRowClicked_cellSelection_singleSelection() {
        test_rt_38464_selectedColumnChangesWhenCellsInRowClicked(true, true);
    }

    @Test public void test_rt_38464_selectedColumnChangesWhenCellsInRowClicked_cellSelection_multipleSelection() {
        test_rt_38464_selectedColumnChangesWhenCellsInRowClicked(true, false);
    }

    @Test public void test_rt_38464_selectedColumnChangesWhenCellsInRowClicked_rowSelection_singleSelection() {
        test_rt_38464_selectedColumnChangesWhenCellsInRowClicked(false, true);
    }

    @Test public void test_rt_38464_selectedColumnChangesWhenCellsInRowClicked_rowSelection_multipleSelection() {
        test_rt_38464_selectedColumnChangesWhenCellsInRowClicked(false, false);
    }

    private void test_rt_38464_selectedColumnChangesWhenCellsInRowClicked(boolean cellSelection, boolean singleSelection) {
        final ObservableList<Person> data =
                FXCollections.observableArrayList(
                        new Person("Jacob", "Smith", "jacob.smith@example.com"),
                        new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
                        new Person("Ethan", "Williams", "ethan.williams@example.com"),
                        new Person("Emma", "Jones", "emma.jones@example.com"),
                        new Person("Michael", "Brown", "michael.brown@example.com"));

        TableView<Person> table = new TableView<>();
        table.setItems(data);

        sm = table.getSelectionModel();
        sm.setCellSelectionEnabled(cellSelection);
        sm.setSelectionMode(singleSelection ? SelectionMode.SINGLE : SelectionMode.MULTIPLE);

        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));

        TableColumn emailCol = new TableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        TableCell cell_0_0 = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 0);
        TableCell cell_0_1 = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 1);
        TableCell cell_0_2 = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 2);

        sm.clearSelection();

        // click on cell (0,0).
        new MouseEventFirer(cell_0_0).fireMousePressAndRelease();

        if (cellSelection) {
            // Because we are in cell selection mode, this has the effect of
            // selecting just the one cell.
            assertFalse(sm.isSelected(0));
            assertTrue(sm.isSelected(0, firstNameCol));
            assertFalse(sm.isSelected(0, lastNameCol));
            assertFalse(sm.isSelected(0, emailCol));
            assertEquals(1, sm.getSelectedCells().size());
            assertEquals(0, sm.getSelectedCells().get(0).getRow());
            assertEquals(firstNameCol, sm.getSelectedCells().get(0).getTableColumn());
        } else {
            // Because we are in row selection mode, this has
            // the effect of selecting all cells and the backing row. However, the
            // selected cell will be (0, firstNameCol) only
            assertTrue(sm.isSelected(0));
            assertTrue(sm.isSelected(0, firstNameCol));
            assertTrue(sm.isSelected(0, lastNameCol));
            assertTrue(sm.isSelected(0, emailCol));
            assertEquals(1, sm.getSelectedCells().size());
            assertEquals(0, sm.getSelectedCells().get(0).getRow());
            assertEquals(firstNameCol, sm.getSelectedCells().get(0).getTableColumn());
        }

        // click on cell (0,1).
        new MouseEventFirer(cell_0_1).fireMousePressAndRelease();

        if (cellSelection) {
            // Everything should remain the same, except the
            // column of the single selected cell should change to lastNameCol.
            assertFalse(sm.isSelected(0));
            assertFalse(sm.isSelected(0, firstNameCol));
            assertTrue(sm.isSelected(0, lastNameCol));
            assertFalse(sm.isSelected(0, emailCol));
            assertEquals(1, sm.getSelectedCells().size());
            TablePosition<?,?> cell = sm.getSelectedCells().get(0);
            assertEquals(0, cell.getRow());
            assertEquals(lastNameCol, cell.getTableColumn());
        } else {
            // Everything should remain the same, except the
            // column of the single selected cell should change to lastNameCol.
            assertTrue(sm.isSelected(0));
            assertTrue(sm.isSelected(0, firstNameCol));
            assertTrue(sm.isSelected(0, lastNameCol));
            assertTrue(sm.isSelected(0, emailCol));
            assertEquals(1, sm.getSelectedCells().size());
            TablePosition<?,?> cell = sm.getSelectedCells().get(0);
            assertEquals(0, cell.getRow());
            assertEquals(lastNameCol, cell.getTableColumn());
        }
    }
}

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
    private TableView.TableViewSelectionModel<String> sm;
    private TableView.TableViewFocusModel<String> fm;

    private final TableColumn<String, String> col0 = new TableColumn<String, String>("col0");
    private final TableColumn<String, String> col1 = new TableColumn<String, String>("col1");
    private final TableColumn<String, String> col2 = new TableColumn<String, String>("col2");
    private final TableColumn<String, String> col3 = new TableColumn<String, String>("col3");
    private final TableColumn<String, String> col4 = new TableColumn<String, String>("col4");
    
    @Before public void setup() {
        tableView = new TableView<String>();
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
        assertNull(pos.getTableColumn());
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

        TableCell cell_0_1 = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 1);
        TableCell cell_0_2 = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 2);
        TableCell cell_0_3 = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 3);

        TableCell cell_1_1 = (TableCell) VirtualFlowTestUtils.getCell(table, 1, 1);
        TableCell cell_1_2 = (TableCell) VirtualFlowTestUtils.getCell(table, 1, 2);
        TableCell cell_1_3 = (TableCell) VirtualFlowTestUtils.getCell(table, 1, 3);

        MouseEventFirer mouse = selectTwoRows ?
                new MouseEventFirer(cell_1_3) : new MouseEventFirer(cell_0_3);

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

        assertTrue(cell_0_1.isSelected());
        assertTrue(cell_0_2.isSelected());
        assertTrue(cell_0_3.isSelected());

        if (selectTwoRows) {
            assertTrue(cell_1_1.isSelected());
            assertTrue(cell_1_2.isSelected());
            assertTrue(cell_1_3.isSelected());
        }
    }
}

/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TableViewShim;
import javafx.scene.control.TreeViewShim;
import javafx.scene.control.cell.PropertyValueFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.test.Person;

/**
 * Unit tests that are specific for the TableViewSelectionModel API. Other tests
 * for the MultipleSelectionModel API (from which TableViewSelectionModel
 * derives) can be found in SelectionModelImplTest.
 *
 * @author Jonathan Giles
 */
public class TableViewSelectionModelImplTest {

    private TableViewSelectionModel<String> model;
    private TableViewFocusModel focusModel;

    // converted from parameterized test with just one choice
    private static final Class<? extends TableViewSelectionModel>modelClass = TableViewShim.get_TableViewArrayListSelectionModel_class();

    // ListView model data
    private static ObservableList<String> defaultData = FXCollections.<String>observableArrayList();
    private static ObservableList<String> data = FXCollections.<String>observableArrayList();
    private static final String ROW_1_VALUE = "Row 1";
    private static final String ROW_2_VALUE = "Row 2";
    private static final String ROW_5_VALUE = "Row 5";
    private static final String ROW_20_VALUE = "Row 20";

    // TableView
    private static final TableView<String> tableView;
    private static final TableColumn<String,String> col0;
    private static final TableColumn<String,String> col1;
    private static final TableColumn<String,String> col2;

    static {
        defaultData.addAll(ROW_1_VALUE, ROW_2_VALUE, "Long Row 3", "Row 4", ROW_5_VALUE, "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", ROW_20_VALUE);

        data.setAll(defaultData);

        // TableView init
        tableView = new TableView();
        tableView.setItems(data);
        tableView.getColumns().addAll(
            col0 = new TableColumn<>(),
            col1 = new TableColumn<>(),
            col2 = new TableColumn<>()
        );
    }

    @AfterAll
    public static void tearDownClass() throws Exception {    }

    @BeforeEach
    public void setUp() throws Exception {
        // reset the data model
        data.setAll(defaultData);

        if (TableViewSelectionModel.class.isAssignableFrom(modelClass)) {
            // recreate the selection model
            model = TreeViewShim.newInstance_from_class(modelClass, tableView);
            tableView.setSelectionModel(model);

            // create a new focus model
            focusModel = new TableViewFocusModel(tableView);
            tableView.setFocusModel(focusModel);
        }
    }

    @AfterEach
    public void tearDown() {
        model = null;
    }

    private Object getValue(Object item) {
        return item;
    }

    private String indices(MultipleSelectionModel sm) {
        return "Selected Indices: " + sm.getSelectedIndices();
    }

    private String items(MultipleSelectionModel sm) {
        return "Selected Items: " + sm.getSelectedItems();
    }

    private String cells(TableViewSelectionModel<?> sm) {
        StringBuilder sb = new StringBuilder("Selected Cells: ");
        for (TablePosition tp : sm.getSelectedCells()) {
            sb.append("(");
            sb.append(tp.getRow());
            sb.append(",");
            sb.append(tp.getColumn());
            sb.append(") ");
        }
        return sb.toString();
    }

    private String focusedCell() {
        return "Focused Cell: " + focusModel.getFocusedCell();
    }

    private TablePosition pos(int row, TableColumn<String,?> col) {
        return new TablePosition(tableView, row, col);
    }

    @Test public void selectRowWhenInSingleCellSelectionMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        assertFalse(model.isSelected(3, col0));
        model.select(1);  // put in another selection to make sure it is wiped out
        model.select(3);
        assertFalse(model.isSelected(1, col0));
        assertFalse(model.isSelected(3, col0));
        assertFalse(model.isSelected(3, null), cells(model));
        assertTrue(model.isSelected(3));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectRowWhenInSingleCellSelectionMode2() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(1, null);  // put in another selection to make sure it is wiped out
        model.select(3, null);

        assertFalse(model.isSelected(1, col0));
        assertFalse(model.isSelected(3, col0));
        assertFalse(model.isSelected(3, null), cells(model));
        assertTrue(model.isSelected(3));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectRowWhenInMultipleCellSelectionMode() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(1);
        model.select(3);

        assertTrue(model.isSelected(1, col0));
        assertTrue(model.isSelected(3, col0));
        assertTrue(model.isSelected(3, null)); // we are in cell selection mode and all cells are selected
        assertTrue(model.isSelected(3));       // this is equivalent to the previous line, so is also true
        assertEquals(6, model.getSelectedCells().size());
    }

    @Test public void selectCellWhenInSingleCellSelectionMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        assertFalse(model.isSelected(3, col0));
        model.select(1, col0);  // put in another selection to make sure it is wiped out
        model.select(3, col0);
        assertFalse(model.isSelected(1, col0));
        assertTrue(model.isSelected(3, col0));
        assertEquals(1, model.getSelectedCells().size());
        assertEquals(pos(3, col0), model.getSelectedCells().get(0));
    }

    @Test public void selectCellWhenInMultipleCellSelectionMode() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        assertFalse(model.isSelected(3, col0));
        model.clearSelection();
        model.select(1, col0);
        model.select(3, col0);
        assertTrue(model.isSelected(1, col0));
        assertTrue(model.isSelected(3, col0));
        assertEquals(2, model.getSelectedCells().size());
        assertEquals(pos(1, col0), model.getSelectedCells().get(0));
        assertEquals(pos(3, col0), model.getSelectedCells().get(1));
    }

    @Test public void selectLeftCell() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(2, col2);
        assertTrue(model.isSelected(2, col2));
        model.selectLeftCell();
        assertFalse(model.isSelected(2, col2));
        assertTrue(model.isSelected(2, col1));
    }

    @Test public void selectRightCell() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(2, col1);
        assertTrue(model.isSelected(2, col1));
        model.selectRightCell();
        assertFalse(model.isSelected(2, col1));
        assertTrue(model.isSelected(2, col2));
    }

    @Test public void selectPreviousCell() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(2, col1);
        assertTrue(model.isSelected(2, col1));
        model.selectAboveCell();
        assertFalse(model.isSelected(2, col1));
        assertTrue(model.isSelected(1, col1));
    }

    @Test public void selectNextCell() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(2, col1);
        assertTrue(model.isSelected(2, col1));
        model.selectBelowCell();
        assertFalse(model.isSelected(2, col1));
        assertTrue(model.isSelected(3, col1));
    }

    @Test public void selectLeftCellWhenAtEdge() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(0, col0);
        assertTrue(model.isSelected(0, col0));
        model.selectLeftCell();
        assertTrue(model.isSelected(0, col0));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectRightCellWhenAtEdge() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        TableColumn<String,?> rightEdge = tableView.getVisibleLeafColumn(tableView.getVisibleLeafColumns().size() - 1);
        model.select(0, rightEdge);
        assertTrue(model.isSelected(0, rightEdge));
        model.selectRightCell();
        assertTrue(model.isSelected(0, rightEdge));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectPreviousCellWhenAtEdge() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(0, col0);
        assertTrue(model.isSelected(0, col0));
        model.selectAboveCell();
        assertTrue(model.isSelected(0, col0));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectNextCellWhenAtEdge() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        int count = data.size();
        model.select(count - 1, col0);
        assertTrue(model.isSelected(count - 1, col0));
        model.selectBelowCell();
        assertTrue(model.isSelected(count - 1, col0));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectNextRowInCellSelectionMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(2, col1);
        assertTrue(model.isSelected(2, col1));
        model.selectNext();
        assertFalse(model.isSelected(2, col1), cells(model));
        assertTrue(model.isSelected(2, col2), cells(model));
    }

    @Test public void selectPreviousRowInCellSelectionMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(2, col1);
        assertTrue(model.isSelected(2, col1));
        model.selectPrevious();
        assertFalse(model.isSelected(2, col1));
        assertTrue(model.isSelected(2, col0));
    }

    @Test public void selectPreviousCellWithMultipleSelection() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(2, col1);
        model.selectAboveCell();
        assertTrue(model.isSelected(2, col1));
        assertTrue(model.isSelected(1, col1));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void selectNextCellWithMultipleSelection() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(2, col1);
        model.selectBelowCell();
        assertTrue(model.isSelected(2, col1));
        assertTrue(model.isSelected(3, col1));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void selectLeftCellWithMultipleSelection() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(2, col1);
        model.selectLeftCell();
        assertTrue(model.isSelected(2, col1), cells(model));
        assertTrue(model.isSelected(2, col0), cells(model));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void selectRightCellWithMultipleSelection() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(2, col1);
        model.selectRightCell();
        assertTrue(model.isSelected(2, col1), cells(model));
        assertTrue(model.isSelected(2, col2), cells(model));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void testIsSelectedWithNullColumnInput() {
        model.setCellSelectionEnabled(true);
        assertFalse(model.isSelected(0, null));

        model.select(10);
        assertFalse(model.isSelected(10, null), cells(model));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void ensureCellSelectionIsNoOpWhenDisabled() {
        model.setCellSelectionEnabled(false);
        model.select(2, col2);
        assertEquals(2, model.getSelectedCells().get(0).getRow());
        assertEquals(col2, model.getSelectedCells().get(0).getTableColumn());
        assertTrue(model.isSelected(2, col2), cells(model));
        assertTrue(model.isSelected(2), cells(model));
    }

    @Test public void clearSelectionOfSelectedCell() {
        model.setCellSelectionEnabled(true);
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.select(2, col2);
        model.select(3, col2);
        assertTrue(model.isSelected(2, col2));
        assertTrue(model.isSelected(3, col2));
        model.clearSelection(2, col2);
        assertFalse(model.isSelected(2, col2));
        assertTrue(model.isSelected(3, col2));
    }

    @Test public void clearSelectionOfCellWhenInRowSelectionMode() {
        model.setCellSelectionEnabled(false);
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.select(2);
        model.select(3);
        assertTrue(model.isSelected(2));
        assertTrue(model.isSelected(3));
        model.clearSelection(2);
        assertFalse(model.isSelected(2), indices(model));
        assertTrue(model.isSelected(3), indices(model));
    }

    @Test public void selectNextCellWhenAtFirstCell() {
        model.setCellSelectionEnabled(true);
        model.select(0, col0);
        model.selectBelowCell();
        assertTrue(model.isSelected(1, col0), cells(model));
    }



    @Test public void selectFirstRowInSingleSelectionRowMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(false);
        model.select(4);
        model.selectFirst();
        assertTrue(model.isSelected(0, null), cells(model));
        assertTrue(model.isSelected(0), cells(model));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectFirstRowInSingleSelectionCellMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(4, col1);
        model.selectFirst();

        // we should go to the top of the currently focused cells column (i.e column 1)
        assertTrue(model.isSelected(0, col1), cells(model));
        assertFalse(model.isSelected(0, null), cells(model));
        assertFalse(model.isSelected(0, col0), cells(model));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectFirstRowInMultipleSelectionRowMode() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(false);
        model.select(4);
        model.selectFirst();
        assertTrue(model.isSelected(0), cells(model));
        assertTrue(model.isSelected(0, null), cells(model));
        assertTrue(model.isSelected(4), cells(model));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void selectFirstRowInMultipleSelectionCellMode() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(4, col1);
        model.selectFirst();
        assertTrue(model.isSelected(0, col1), cells(model));
        assertTrue(model.isSelected(4, col1), cells(model));
        assertEquals(2, model.getSelectedCells().size());
    }



    @Test public void selectLastRowInSingleSelectionRowMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(false);
        model.select(4);
        model.selectLast();
        assertTrue(model.isSelected(tableView.getItems().size() - 1), cells(model));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectLastRowInSingleSelectionCellMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(4, col1);
        model.selectLast();
        assertTrue(model.isSelected(tableView.getItems().size() - 1, col1), cells(model));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectLastRowInMultipleSelectionRowMode() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(false);
        model.clearSelection();
        model.select(4);
        model.selectLast();
        assertTrue(model.isSelected(tableView.getItems().size() - 1), cells(model));
        assertTrue(model.isSelected(4), cells(model));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void selectLastRowInMultipleSelectionCellMode() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(4, col1);
        model.selectLast();
        assertTrue(model.isSelected(tableView.getItems().size() - 1, col1), cells(model));
        assertTrue(model.isSelected(4, col1), cells(model));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void selectCellInRowSelectionMode_expectCellInformationToRemain() {
        model.setCellSelectionEnabled(false);
        model.select(4, col0);
        assertEquals(col0, model.getSelectedCells().get(0).getTableColumn(), cells(model));
        assertEquals(col0, focusModel.getFocusedCell().getTableColumn());
        assertTrue(model.isSelected(4, col0));
        assertTrue(model.isSelected(4));
    }

    @Test
    public void selectIndividualCells() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();

        model.select(0, col0);
        assertTrue(model.isSelected(0), cells(model));
        assertFalse(model.isSelected(1), cells(model));
        assertFalse(model.isSelected(2), cells(model));

        model.select(1, col0);
        model.select(1, col1);
        assertTrue(model.isSelected(0), cells(model));
        assertTrue(model.isSelected(1), cells(model));
        assertFalse(model.isSelected(2), cells(model));

        model.select(2, col0);
        model.select(2, col1);
        model.select(2, col2);
        assertTrue(model.isSelected(0), cells(model));
        assertTrue(model.isSelected(1), cells(model));
        assertTrue(model.isSelected(2), cells(model));

        assertFalse(model.isSelected(3), cells(model));

        assertEquals(6, model.getSelectedCells().size());

        model.clearSelection(0, col0);
        assertFalse(model.isSelected(0), cells(model));

        model.clearSelection(1, col0);
        assertTrue(model.isSelected(1), cells(model));
        model.clearSelection(1, col1);
        assertFalse(model.isSelected(1), cells(model));

        model.clearSelection(2, col0);
        assertTrue(model.isSelected(2), cells(model));
        model.clearSelection(2, col1);
        assertTrue(model.isSelected(2), cells(model));
        model.clearSelection(2, col2);
        assertFalse(model.isSelected(2), cells(model));

        assertEquals(0, model.getSelectedCells().size());
    }

    /***************************************************************************
     *
     * FOCUS TESTS
     *
     **************************************************************************/

    @Test public void focusOnRow() {
        focusModel.focus(3);
        assertTrue(focusModel.isFocused(3), focusedCell());
        assertTrue(focusModel.isFocused(3, null), focusedCell());
        assertEquals(new TablePosition(tableView, 3, null), focusModel.getFocusedCell());
        assertEquals(tableView.getItems().get(3), focusModel.getFocusedItem());
    }

    @Test public void focusOnNegativeRowIndex() {
        focusModel.focus(-20);
        assertEquals(new TablePosition(tableView, -1, null), focusModel.getFocusedCell());
        assertFalse(focusModel.isFocused(-20, null), focusedCell());
    }

    @Test public void focusOutOfColumnsBounds() {
        focusModel.focus(3, null);
        assertEquals(new TablePosition(tableView, 3, null), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(3, null), focusedCell());
    }

    @Test public void focusPreviousRow() {
        focusModel.focus(3);
        assertEquals(new TablePosition(tableView, 3, null), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(3), focusedCell());
        assertTrue(focusModel.isFocused(3, null), focusedCell());

        focusModel.focusPrevious();
        assertEquals(new TablePosition(tableView, 2, null), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(2), focusedCell());
        assertTrue(focusModel.isFocused(2, null), focusedCell());
    }

    @Test public void focusPreviousRowImmediately() {
        focusModel.focusPrevious();
        assertEquals(new TablePosition(tableView, 0, col0), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(0, null), focusedCell());
    }

    @Test public void focusPreviousRowFromFirstRow() {
        focusModel.focus(0);
        focusModel.focusPrevious();
        assertEquals(new TablePosition(tableView, 0, null), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(0), focusedCell());
        assertTrue(focusModel.isFocused(0, null), focusedCell());
    }

    @Test public void focusNextRow() {
        focusModel.focus(3);
        focusModel.focusNext();

        assertEquals(new TablePosition(tableView, 4, null), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(4), focusedCell());
        assertTrue(focusModel.isFocused(4, null), focusedCell());
    }

    @Test public void focusNextRowImmediately() {
        assertEquals(new TablePosition(tableView, 0, col0), focusModel.getFocusedCell());

        focusModel.focusNext();

        assertEquals(new TablePosition(tableView, 1, col0), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(1), focusedCell());
        assertTrue(focusModel.isFocused(1, null), focusedCell());
    }

    @Test public void focusNextRowFromLastRow() {
        int rowCount = tableView.getItems().size() - 1;
        focusModel.focus(rowCount);
        focusModel.focusNext();

        assertEquals(new TablePosition(tableView, rowCount, null), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(rowCount), focusedCell());
        assertTrue(focusModel.isFocused(rowCount, null), focusedCell());
    }

    @Test public void focusAboveCell() {
        focusModel.focus(3, col1);
        assertEquals(new TablePosition(tableView, 3, col1), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(3, col1), focusedCell());

        focusModel.focusAboveCell();
        assertEquals(new TablePosition(tableView, 2, col1), focusModel.getFocusedCell());
        // not sure about this - a row probably shouldn't be focused when we've given it specifically to a cell
        assertTrue(focusModel.isFocused(2), focusedCell());
        assertTrue(focusModel.isFocused(2, col1), focusedCell());
    }

    @Test public void focusAboveCellFromFirstRow() {
        focusModel.focus(0, col1);
        focusModel.focusAboveCell();
        assertEquals(new TablePosition(tableView, 0, col1), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(0), focusedCell());
        assertTrue(focusModel.isFocused(0, col1), focusedCell());
    }

    @Test public void focusBelowCell() {
        focusModel.focus(3, col1);
        focusModel.focusBelowCell();

        assertEquals(new TablePosition(tableView, 4, col1), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(4), focusedCell());
        assertTrue(focusModel.isFocused(4, col1), focusedCell());
    }

    @Test public void focusBelowCellFromLastRow() {
        int rowCount = tableView.getItems().size() - 1;
        focusModel.focus(rowCount, col1);
        focusModel.focusBelowCell();

        assertEquals(new TablePosition(tableView, rowCount, col1), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(rowCount), focusedCell());
        assertTrue(focusModel.isFocused(rowCount, col1), focusedCell());
    }

    @Test public void focusLeftCell() {
        focusModel.focus(3, col1);
        focusModel.focusLeftCell();

        assertEquals(new TablePosition(tableView, 3, col0), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(3), focusedCell());
        assertTrue(focusModel.isFocused(3, col0), focusedCell());
    }

    @Test public void focusLeftCellFromFirstColumn() {
        focusModel.focus(3, col0);
        focusModel.focusLeftCell();

        assertEquals(new TablePosition(tableView, 3, col0), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(3), focusedCell());
        assertTrue(focusModel.isFocused(3, col0), focusedCell());
    }

    @Test public void focusLeftCellFromNullColumn() {
        focusModel.focus(3, null);
        focusModel.focusLeftCell();

        assertEquals(new TablePosition(tableView, 3, null), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(3), focusedCell());
        assertTrue(focusModel.isFocused(3, null), focusedCell());
    }

    @Test public void focusRightCell() {
        focusModel.focus(3, col0);
        focusModel.focusRightCell();

        assertEquals(new TablePosition(tableView, 3, col1), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(3), focusedCell());
        assertTrue(focusModel.isFocused(3, col1), focusedCell());
    }

    @Test public void focusRightCellFromEndColumn() {
        model.clearSelection();
        TableColumn<String,?> rightEdge = tableView.getVisibleLeafColumn(tableView.getVisibleLeafColumns().size() - 1);

        focusModel.focus(3, rightEdge);
        focusModel.focusRightCell();

        assertEquals(new TablePosition(tableView, 3, rightEdge), focusModel.getFocusedCell());
        assertTrue(focusModel.isFocused(3), focusedCell());
        assertTrue(focusModel.isFocused(3, rightEdge), focusedCell());
    }

    @Test public void test_rt33442() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();

        assertTrue(model.getSelectedCells().isEmpty());

        // select from (0,0) to (4,2) -> 5 x 3 cells = 15 cells in total
        model.selectRange(0, col0, 4, col2);
        assertEquals(15, model.getSelectedCells().size());

        for (int row = 0; row <= 4; row++) {
            for (int column = 0; column <= 2; column++) {
                assertTrue(model.isSelected(row, tableView.getVisibleLeafColumn(column)));
            }
        }
    }

    @Test public void test_rt33442_changeSelectionModeClearsSelection() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);

        assertTrue(model.getSelectedCells().isEmpty());

        // select from (0,0) to (4,2) -> 5 x 3 cells = 15 cells in total
        model.selectRange(0, col0, 4, col2);
        assertEquals(15, model.getSelectedCells().size());

        model.setSelectionMode(SelectionMode.SINGLE);
        assertEquals(1, model.getSelectedCells().size());
        for (int row = 0; row <= 4; row++) {
            for (int column = 0; column <= 2; column++) {
                // the last item will be selected
                if (row == 4 && column == 2) {
                    assertTrue(model.isSelected(row, tableView.getVisibleLeafColumn(column)));
                } else {
                    assertFalse(model.isSelected(row, tableView.getVisibleLeafColumn(column)));
                }
            }
        }
    }

    @Test public void test_jdk_8143594() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(false);

        tableView.getItems().set(3, null);

        model.select(0);
        model.clearAndSelect(3);
        model.clearAndSelect(0);
        model.clearAndSelect(3);
    }

    @Test public void test_cellSelection_nullColumn_isSelected_noCellsSelected() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        assertFalse(model.isSelected(0, null));
    }

    @Test public void test_cellSelection_nullColumn_isSelected_oneCellSelected() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);

        model.select(0, col0);
        assertTrue(model.isSelected(0, col0));
        assertFalse(model.isSelected(0, null));
    }

    @Test public void test_cellSelection_nullColumn_isSelected_allCellsSelected() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);

        // when null is passed in for the column, all cells in the row are selected
        model.select(0, null);

        // when null is passed in to isSelected, all cells in the row must be selected.
        assertTrue(model.isSelected(0, null));
    }

    @Test public void test_cellSelection_nullColumn_selectAllCellsInRow() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);

        // when null is passed in for the column, all cells in the row are selected
        model.select(0, null);
        assertTrue(model.isSelected(0, col0));
        assertTrue(model.isSelected(0, col1));
        assertTrue(model.isSelected(0, col2));
        assertEquals(3, model.getSelectedCells().size());
    }

    @Test public void test_cellSelection_nullColumn_clearAndSelectAllCellsInRow() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);

        model.select(1, col1);

        // when null is passed in for the column, all cells in the row are selected
        model.clearAndSelect(0, null);
        assertTrue(model.isSelected(0, col0));
        assertTrue(model.isSelected(0, col1));
        assertTrue(model.isSelected(0, col2));
        assertEquals(3, model.getSelectedCells().size());
    }

    @Test public void test_cellSelection_nullColumn_clearSelection_noCellsSelected() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);

        // when null is passed in for the column, all cells in the row are unselected
        model.clearSelection(0, null);
        assertFalse(model.isSelected(0, col0));
        assertFalse(model.isSelected(0, col1));
        assertFalse(model.isSelected(0, col2));
        assertEquals(0, model.getSelectedCells().size());
    }

    @Test public void test_cellSelection_nullColumn_clearSelection_allCellsSelected() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);

        model.select(0, col1);
        assertTrue(model.isSelected(0, col1));

        // when null is passed in for the column, all cells in the row are unselected
        model.clearSelection(0, null);
        assertFalse(model.isSelected(0, col0));
        assertFalse(model.isSelected(0, col1));
        assertFalse(model.isSelected(0, col2));
        assertEquals(0, model.getSelectedCells().size());
    }

    @Test public void test_cellSelection_nullColumn_clearSelection_oneCellSelected() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);

        model.select(0, null);

        // when null is passed in for the column, all cells in the row are unselected
        model.clearSelection(0, null);
        assertFalse(model.isSelected(0, col0));
        assertFalse(model.isSelected(0, col1));
        assertFalse(model.isSelected(0, col2));
        assertEquals(0, model.getSelectedCells().size());
    }

    @Test public void test_jdk_8144501() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.select(2);
        model.select(3);
        ListChangeListener<String> listener = change -> {
            while (change.next()) {
                assertNotNull(change.getList());
                assertEquals(1, change.getList().size());
                assertNotNull(change.getList().get(0));
            }
        };
        model.getSelectedItems().addListener(listener);
        model.clearSelection(2);
        model.getSelectedItems().removeListener(listener);
    }

    /**
     * Analysing failing tests when fixing JDK-8219720.
     *
     * Suspect: isSelected(int row) violates contract.
     *
     * @see #selectRowWhenInSingleCellSelectionMode()
     * @see #selectRowWhenInSingleCellSelectionMode2()
     */
    @Test
    public void testSelectRowWhenInSingleCellSelectionModeIsSelected() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(3);
        // test against contract
        assertEquals(3, model.getSelectedIndex(), "selected index");
        assertTrue(model.getSelectedIndices().contains(3), "contained in selected indices");
        // test against spec
        assertEquals(model.getSelectedIndices().contains(3), model.isSelected(3), "is selected index");
    }

    @Test
    public void testRowSelectionAfterSelectAndHideLastColumnMultipleCellEnabled() {
        assertRowSelectionAfterSelectAndHideLastColumn(SelectionMode.MULTIPLE, true);
    }

    @Test
    public void testRowSelectionAfterSelectAndHideLastColumnMultipleNotCellEnabled() {
        assertRowSelectionAfterSelectAndHideLastColumn(SelectionMode.MULTIPLE, false);
    }

    @Test
    public void testRowSelectionAfterSelectAndHideLastColumnSingleCellEnabled() {
        assertRowSelectionAfterSelectAndHideLastColumn(SelectionMode.SINGLE, true);
    }

    @Test
    public void testRowSelectionAfterSelectAndHideLastColumnSingleNotCellEnabled() {
        assertRowSelectionAfterSelectAndHideLastColumn(SelectionMode.SINGLE, false);
    }

    public void assertRowSelectionAfterSelectAndHideLastColumn(SelectionMode mode, boolean cellEnabled) {
        TableView<Person> table = createPersonTableView();

        TableView.TableViewSelectionModel<Person> sm = table.getSelectionModel();
        sm.setCellSelectionEnabled(cellEnabled);
        sm.setSelectionMode(mode);
        int row = 1;
        int col = table.getColumns().size() - 1;
        assertRowSelectionAfterSelectAndHideColumn(table, row, col);
    }

    private void assertRowSelectionAfterSelectAndHideColumn(TableView<Person> table, int row, int col) {
        TableViewSelectionModel<Person> sm = table.getSelectionModel();
        TableColumn<Person, ?> column = table.getColumns().get(col);

        sm.select(row, column);
        assertTrue(sm.getSelectedIndices().contains(row), "sanity: row " + row + "contained in selectedIndices");
        assertTrue(sm.isSelected(row), "sanity: row must be selected");
        column.setVisible(false);
        assertTrue(sm.getSelectedIndices().contains(row), "after hiding column: row " + row + "contained in selectedIndices");
        assertTrue(sm.isSelected(row), "after hiding column: row must be selected");
    }

    /**
     * Creates and returns a TableView with Persons and columns for all their properties.
     */
    private TableView<Person> createPersonTableView() {
        final ObservableList<Person> data =
                FXCollections.observableArrayList(
                        new Person("Jacob", "Smith", "jacob.smith@example.com"),
                        new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
                        new Person("Ethan", "Williams", "ethan.williams@example.com"),
                        new Person("Emma", "Jones", "emma.jones@example.com"),
                        new Person("Michael", "Brown", "michael.brown@example.com"));

        TableView<Person> table = new TableView<>();
        table.setItems(data);

        TableColumn<Person, String> firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn<Person, String> lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));

        TableColumn<Person, String> emailCol = new TableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        return table;
    }
}

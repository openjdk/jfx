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

package javafx.scene.control;

import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.util.Callback;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Unit tests that are specific for the TreeTableViewSelectionModel API. Other tests
 * for the MultipleSelectionModel API (from which TableViewSelectionModel
 * derives) can be found in SelectionModelImplTest.
 */
public class TreeTableViewSelectionModelImplTest {

    private TreeTableView.TreeTableViewSelectionModel<String> model;
    private TreeTableView.TreeTableViewFocusModel focusModel;

    private TreeTableView<String> tableView;
    private TreeTableColumn<String,String> col0;
    private TreeTableColumn<String,String> col1;
    private TreeTableColumn<String,String> col2;

    @Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { TreeTableView.TreeTableViewArrayListSelectionModel.class }
        });
    }

    @AfterClass public static void tearDownClass() throws Exception {    }

    @Before public void setUp() throws Exception {
        TreeItem<String> root = new TreeItem<>("Root");
        root.setExpanded(true);
        for (int i = 0; i < 20; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView = new TreeTableView(root);
        tableView.getColumns().addAll(
                col0 = new TreeTableColumn<>(),
                col1 = new TreeTableColumn<>(),
                col2 = new TreeTableColumn<>()
        );

        model = tableView.getSelectionModel();
        focusModel = tableView.getFocusModel();
    }

    @After public void tearDown() {
        model = null;
    }

    private String indices(MultipleSelectionModel sm) {
        return "Selected Indices: " + sm.getSelectedIndices();
    }

    private String items(MultipleSelectionModel sm) {
        return "Selected Items: " + sm.getSelectedItems();
    }

    private String cells(TreeTableViewSelectionModel<?> sm) {
        StringBuilder sb = new StringBuilder("Selected Cells: ");
        for (TreeTablePosition tp : sm.getSelectedCells()) {
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

    private TreeTablePosition pos(int row, TreeTableColumn<String,?> col) {
        return new TreeTablePosition(tableView, row, col);
    }

    @Test public void selectRowWhenInSingleCellSelectionMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        assertFalse(model.isSelected(3, col0));
        model.select(1);  // put in another selection to make sure it is wiped out
        model.select(3);
        assertFalse(model.isSelected(1, col0));
        assertFalse(model.isSelected(3, col0));
        assertFalse(cells(model), model.isSelected(3, null));
        assertFalse(model.isSelected(3));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectRowWhenInSingleCellSelectionMode2() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(1, null);  // put in another selection to make sure it is wiped out
        model.select(3, null);

        assertFalse(model.isSelected(1, col0));
        assertFalse(model.isSelected(3, col0));
        assertFalse(cells(model), model.isSelected(3, null));
        assertFalse(model.isSelected(3));
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
        assertFalse(model.isSelected(3, null));
        assertFalse(model.isSelected(3));
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
        TreeTableColumn<String,?> rightEdge = tableView.getVisibleLeafColumn(tableView.getVisibleLeafColumns().size() - 1);
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
        int count = tableView.getExpandedItemCount();
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
        assertFalse(cells(model), model.isSelected(2, col1));
        assertTrue(cells(model), model.isSelected(2, col2));
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
        assertTrue(cells(model), model.isSelected(2, col1));
        assertTrue(cells(model), model.isSelected(2, col0));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void selectRightCellWithMultipleSelection() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(2, col1);
        model.selectRightCell();
        assertTrue(cells(model), model.isSelected(2, col1));
        assertTrue(cells(model), model.isSelected(2, col2));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void testIsSelectedWithNullColumnInput() {
        model.setCellSelectionEnabled(true);
        assertFalse(model.isSelected(0, null));

        model.select(10);
        assertFalse(cells(model), model.isSelected(10, null));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void ensureCellSelectionIsNoOpWhenDisabled() {
        model.setCellSelectionEnabled(false);
        model.select(2, col2);
        assertEquals(2, model.getSelectedCells().get(0).getRow());
        assertEquals(col2, model.getSelectedCells().get(0).getTableColumn());
        assertTrue(cells(model), model.isSelected(2, col2));
        assertTrue(cells(model), model.isSelected(2));
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
        assertFalse(indices(model), model.isSelected(2));
        assertTrue(indices(model), model.isSelected(3));
    }

    @Test public void selectNextCellWhenAtFirstCell() {
        model.setCellSelectionEnabled(true);
        model.select(0, col0);
        model.selectBelowCell();
        assertTrue(cells(model), model.isSelected(1, col0));
    }



    @Test public void selectFirstRowInSingleSelectionRowMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(false);
        model.select(4);
        model.selectFirst();
        assertTrue(cells(model), model.isSelected(0, null));
        assertTrue(cells(model), model.isSelected(0));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectFirstRowInSingleSelectionCellMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(4, col1);
        model.selectFirst();

        // we should go to the top of the currently focused cells column (i.e column 1)
        assertTrue(cells(model), model.isSelected(0, col1));
        assertFalse(cells(model), model.isSelected(0, null));
        assertFalse(cells(model), model.isSelected(0, col0));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectFirstRowInMultipleSelectionRowMode() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(false);
        model.select(4);
        model.selectFirst();
        assertTrue(cells(model), model.isSelected(0));
        assertTrue(cells(model), model.isSelected(0, null));
        assertTrue(cells(model), model.isSelected(4));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void selectFirstRowInMultipleSelectionCellMode() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(4, col1);
        model.selectFirst();
        assertTrue(cells(model), model.isSelected(0, col1));
        assertTrue(cells(model), model.isSelected(4, col1));
        assertEquals(2, model.getSelectedCells().size());
    }



    @Test public void selectLastRowInSingleSelectionRowMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(false);
        model.select(4);
        model.selectLast();
        assertTrue(cells(model), model.isSelected(tableView.getExpandedItemCount() - 1));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectLastRowInSingleSelectionCellMode() {
        model.setSelectionMode(SelectionMode.SINGLE);
        model.setCellSelectionEnabled(true);
        model.select(4, col1);
        model.selectLast();
        assertTrue(cells(model), model.isSelected(tableView.getExpandedItemCount() - 1, col1));
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void selectLastRowInMultipleSelectionRowMode() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(false);
        model.clearSelection();
        model.select(4);
        model.selectLast();
        assertTrue(cells(model), model.isSelected(tableView.getExpandedItemCount() - 1));
        assertTrue(cells(model), model.isSelected(4));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void selectLastRowInMultipleSelectionCellMode() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();
        model.select(4, col1);
        model.selectLast();
        assertTrue(cells(model), model.isSelected(tableView.getExpandedItemCount() - 1, col1));
        assertTrue(cells(model), model.isSelected(4, col1));
        assertEquals(2, model.getSelectedCells().size());
    }

    @Test public void selectCellInRowSelectionMode_expectCellInformationToRemain() {
        model.setCellSelectionEnabled(false);
        model.select(4, col0);
        assertEquals(cells(model), col0, model.getSelectedCells().get(0).getTableColumn());
        assertEquals(col0, focusModel.getFocusedCell().getTableColumn());
        assertTrue(model.isSelected(4, col0));
        assertTrue(model.isSelected(4));
    }

    /***************************************************************************
     *
     * FOCUS TESTS
     *
     **************************************************************************/

    @Test public void focusOnRow() {
        focusModel.focus(3);
        assertTrue(focusedCell(), focusModel.isFocused(3));
        assertTrue(focusedCell(), focusModel.isFocused(3, null));
        assertEquals(new TreeTablePosition(tableView, 3, null), focusModel.getFocusedCell());
        assertEquals(tableView.getTreeItem(3), focusModel.getFocusedItem());
    }

    @Test public void focusOnNegativeRowIndex() {
        focusModel.focus(-20);
        assertEquals(new TreeTablePosition(tableView, -1, null), focusModel.getFocusedCell());
        assertFalse(focusedCell(), focusModel.isFocused(-20, null));
    }

    @Test public void focusOutOfColumnsBounds() {
        focusModel.focus(3, null);
        assertEquals(new TreeTablePosition(tableView, 3, null), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(3, null));
    }

    @Test public void focusPreviousRow() {
        focusModel.focus(3);
        assertEquals(new TreeTablePosition(tableView, 3, null), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(3));
        assertTrue(focusedCell(), focusModel.isFocused(3, null));

        focusModel.focusPrevious();
        assertEquals(new TreeTablePosition(tableView, 2, null), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(2));
        assertTrue(focusedCell(), focusModel.isFocused(2, null));
    }

    @Test public void focusPreviousRowImmediately() {
        focusModel.focusPrevious();
        assertEquals(new TreeTablePosition(tableView, 0, null), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(0, null));
    }

    @Test public void focusPreviousRowFromFirstRow() {
        focusModel.focus(0);
        focusModel.focusPrevious();
        assertEquals(new TreeTablePosition(tableView, 0, null), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(0));
        assertTrue(focusedCell(), focusModel.isFocused(0, null));
    }

    @Test public void focusNextRow() {
        focusModel.focus(3);
        focusModel.focusNext();

        assertEquals(new TreeTablePosition(tableView, 4, null), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(4));
        assertTrue(focusedCell(), focusModel.isFocused(4, null));
    }

    @Test public void focusNextRowImmediately() {
        assertEquals(new TreeTablePosition(tableView, 0, null), focusModel.getFocusedCell());

        focusModel.focusNext();

        assertEquals(new TreeTablePosition(tableView, 1, null), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(1));
        assertTrue(focusedCell(), focusModel.isFocused(1, null));
    }

    @Test public void focusNextRowFromLastRow() {
        int rowCount = tableView.getExpandedItemCount() - 1;
        focusModel.focus(rowCount);
        focusModel.focusNext();

        assertEquals(new TreeTablePosition(tableView, rowCount, null), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(rowCount));
        assertTrue(focusedCell(), focusModel.isFocused(rowCount, null));
    }

    @Test public void focusAboveCell() {
        focusModel.focus(3, col1);
        assertEquals(new TreeTablePosition(tableView, 3, col1), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(3, col1));

        focusModel.focusAboveCell();
        assertEquals(new TreeTablePosition(tableView, 2, col1), focusModel.getFocusedCell());
        // not sure about this - a row probably shouldn't be focused when we've given it specifically to a cell
        assertTrue(focusedCell(), focusModel.isFocused(2));
        assertTrue(focusedCell(), focusModel.isFocused(2, col1));
    }

    @Test public void focusAboveCellFromFirstRow() {
        focusModel.focus(0, col1);
        focusModel.focusAboveCell();
        assertEquals(new TreeTablePosition(tableView, 0, col1), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(0));
        assertTrue(focusedCell(), focusModel.isFocused(0, col1));
    }

    @Test public void focusBelowCell() {
        focusModel.focus(3, col1);
        focusModel.focusBelowCell();

        assertEquals(new TreeTablePosition(tableView, 4, col1), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(4));
        assertTrue(focusedCell(), focusModel.isFocused(4, col1));
    }

    @Test public void focusBelowCellFromLastRow() {
        int rowCount = tableView.getExpandedItemCount() - 1;
        focusModel.focus(rowCount, col1);
        focusModel.focusBelowCell();

        assertEquals(new TreeTablePosition(tableView, rowCount, col1), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(rowCount));
        assertTrue(focusedCell(), focusModel.isFocused(rowCount, col1));
    }

    @Test public void focusLeftCell() {
        focusModel.focus(3, col1);
        focusModel.focusLeftCell();

        assertEquals(new TreeTablePosition(tableView, 3, col0), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(3));
        assertTrue(focusedCell(), focusModel.isFocused(3, col0));
    }

    @Test public void focusLeftCellFromFirstColumn() {
        focusModel.focus(3, col0);
        focusModel.focusLeftCell();

        assertEquals(new TreeTablePosition(tableView, 3, col0), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(3));
        assertTrue(focusedCell(), focusModel.isFocused(3, col0));
    }

    @Test public void focusLeftCellFromNullColumn() {
        focusModel.focus(3, null);
        focusModel.focusLeftCell();

        assertEquals(new TreeTablePosition(tableView, 3, null), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(3));
        assertTrue(focusedCell(), focusModel.isFocused(3, null));
    }

    @Test public void focusRightCell() {
        focusModel.focus(3, col0);
        focusModel.focusRightCell();

        assertEquals(new TreeTablePosition(tableView, 3, col1), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(3));
        assertTrue(focusedCell(), focusModel.isFocused(3, col1));
    }

    @Test public void focusRightCellFromEndColumn() {
        TreeTableColumn<String,?> rightEdge = tableView.getVisibleLeafColumn(tableView.getVisibleLeafColumns().size() - 1);

        focusModel.focus(3, rightEdge);
        focusModel.focusRightCell();

        assertEquals(new TreeTablePosition(tableView, 3, rightEdge), focusModel.getFocusedCell());
        assertTrue(focusedCell(), focusModel.isFocused(3));
        assertTrue(focusedCell(), focusModel.isFocused(3, rightEdge));
    }

    @Test public void test_rt33442() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);

        assertFalse(model.getSelectedCells().isEmpty());

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
        model.clearSelection();

        assertTrue(model.getSelectedCells().isEmpty());

        // select from (0,0) to (4,2) -> 5 x 3 cells = 15 cells in total
        model.selectRange(0, col0, 4, col2);
        assertEquals(15, model.getSelectedCells().size());

        model.setSelectionMode(SelectionMode.SINGLE);
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

    @Test public void test_rt34103_cellSelection() {
        tableView.setRoot(new TreeItem("Root"));
        tableView.getRoot().setExpanded(true);

        for (int i = 0; i < 4; i++) {
            TreeItem parent = new TreeItem("item - " + i);
            tableView.getRoot().getChildren().add(parent);

            for (int j = 0; j < 4; j++) {
                TreeItem child = new TreeItem("item - " + i + " " + j);
                parent.getChildren().add(child);
            }
        }

        TreeTableColumn name = new TreeTableColumn("Name");
        name.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures, ObservableValue>() {
            @Override public ObservableValue call(TreeTableColumn.CellDataFeatures p) {
                return new ReadOnlyStringWrapper((String)p.getValue().getValue());
            }
        });

        tableView.getColumns().setAll(name);
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();

        TreeItem item0 = tableView.getTreeItem(1);
        assertEquals("item - 0", item0.getValue());
        item0.setExpanded(true);

        model.selectRange(1, name, 3, name);
        assertEquals(3, model.getSelectedCells().size());

        item0.setExpanded(false);
        Toolkit.getToolkit().firePulse();
        assertEquals(1, model.getSelectedCells().size());
    }

    @Test public void test_rt34103_rowSelection() {
        tableView.setRoot(new TreeItem("Root"));
        tableView.getRoot().setExpanded(true);

        for (int i = 0; i < 4; i++) {
            TreeItem parent = new TreeItem("item - " + i);
            tableView.getRoot().getChildren().add(parent);

            for (int j = 0; j < 4; j++) {
                TreeItem child = new TreeItem("item - " + i + " " + j);
                parent.getChildren().add(child);
            }
        }

        TreeTableColumn name = new TreeTableColumn("Name");
        name.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures, ObservableValue>() {
            @Override
            public ObservableValue call(TreeTableColumn.CellDataFeatures p) {
                return new ReadOnlyStringWrapper((String)p.getValue().getValue());
            }
        });

        tableView.getColumns().addAll(name);
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(false);

        TreeItem item0 = tableView.getTreeItem(1);
        assertEquals("item - 0", item0.getValue());
        item0.setExpanded(true);

        model.clearSelection();
        model.selectIndices(1,2,3);
        assertEquals(3, model.getSelectedCells().size());

        item0.setExpanded(false);
        Toolkit.getToolkit().firePulse();
        assertEquals(1, model.getSelectedCells().size());
    }
}

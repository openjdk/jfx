/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.tk.Toolkit;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.MultipleSelectionModelShim;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.scene.control.TreeTableViewShim;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.util.Callback;
import test.com.sun.javafx.scene.control.test.Person;

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
            { TreeTableViewShim.get_TreeTableViewArrayListSelectionModel_class() }
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
        return "Selected Indices: " + MultipleSelectionModelShim.getSelectedIndices(sm);
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
        assertTrue(model.isSelected(3));
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

    @Test
    public void selectIndividualCells() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(true);
        model.clearSelection();

        model.select(0, col0);
        assertTrue(cells(model), model.isSelected(0));
        assertFalse(cells(model), model.isSelected(1));
        assertFalse(cells(model), model.isSelected(2));

        model.select(1, col0);
        model.select(1, col1);
        assertTrue(cells(model), model.isSelected(0));
        assertTrue(cells(model), model.isSelected(1));
        assertFalse(cells(model), model.isSelected(2));

        model.select(2, col0);
        model.select(2, col1);
        model.select(2, col2);
        assertTrue(cells(model), model.isSelected(0));
        assertTrue(cells(model), model.isSelected(1));
        assertTrue(cells(model), model.isSelected(2));

        assertFalse(cells(model), model.isSelected(3));

        assertEquals(6, model.getSelectedCells().size());

        model.clearSelection(0, col0);
        assertFalse(cells(model), model.isSelected(0));

        model.clearSelection(1, col0);
        assertTrue(cells(model), model.isSelected(1));
        model.clearSelection(1, col1);
        assertFalse(cells(model), model.isSelected(1));

        model.clearSelection(2, col0);
        assertTrue(cells(model), model.isSelected(2));
        model.clearSelection(2, col1);
        assertTrue(cells(model), model.isSelected(2));
        model.clearSelection(2, col2);
        assertFalse(cells(model), model.isSelected(2));

        assertEquals(0, model.getSelectedCells().size());
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

    @Test public void test_jdk8131924_showRoot() {
        test_jdk8131924(true);
    }

    @Test public void test_jdk8131924_hideRoot() {
        test_jdk8131924(false);
    }

    private void test_jdk8131924(boolean showRoot) {
        tableView.setRoot(new TreeItem("Root"));
        tableView.getRoot().setExpanded(true);

        tableView.setShowRoot(showRoot);

        for (int i = 0; i < 4; i++) {
            tableView.getRoot().getChildren().add(new TreeItem("" + i));
        }

        TreeTableColumn<String, String> col = new TreeTableColumn("Name");
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getValue()));

        tableView.getColumns().addAll(col);
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(false);

        int startIndex = showRoot ? 2 : 1;
        model.select(startIndex);
        assertEquals(startIndex, model.getSelectedIndex());
        assertEquals(1, model.getSelectedIndices().size());
        assertEquals("1", model.getSelectedItem().getValue());

        // add a new item where the selection is, pushing the selection down one so that it remains on the same item
        tableView.getRoot().getChildren().add(startIndex + (showRoot ? -1 : 0), new TreeItem<>("NEW"));
        assertEquals("1", model.getSelectedItem().getValue());
        assertEquals(startIndex + 1, model.getSelectedIndex());
        assertEquals(1, model.getSelectedIndices().size());
        assertEquals(1, model.getSelectedItems().size());

        // now delete the item that was selected initially. Selection should move up one to startIndex, where
        // the "NEW" element is
        tableView.getRoot().getChildren().remove(startIndex + (showRoot ? 0 : 1));
        assertEquals(1, model.getSelectedIndices().size());
        assertEquals(startIndex, model.getSelectedIndex());
        assertEquals("NEW", model.getSelectedItem().getValue());
        assertEquals(1, model.getSelectedItems().size());
    }

    @Test public void test_jdk_8143594_nullValue() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(false);

        tableView.setShowRoot(false);
        tableView.getRoot().getChildren().get(3).setValue(null);

        model.select(0);
        model.clearAndSelect(3);
        model.clearAndSelect(0);
        model.clearAndSelect(3);
    }

    @Test public void test_jdk_8143594_nullTreeItem() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.setCellSelectionEnabled(false);

        tableView.setShowRoot(false);
        tableView.getRoot().getChildren().set(3, null);

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
        ListChangeListener<TreeItem<String>> listener = change -> {
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
        assertEquals("selected index", 3, model.getSelectedIndex());
        assertTrue("contained in selected indices", model.getSelectedIndices().contains(3));
        // test against spec
        assertEquals("is selected index", model.getSelectedIndices().contains(3), model.isSelected(3));
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
        TreeTableView<Person> table = createPersonTreeTableView();

        TreeTableViewSelectionModel<Person> sm = table.getSelectionModel();
        sm.setCellSelectionEnabled(cellEnabled);
        sm.setSelectionMode(mode);
        int row = 1;
        int col = table.getColumns().size() - 1;
        assertRowSelectionAfterSelectAndHideColumn(table, row, col);
    }

    private void assertRowSelectionAfterSelectAndHideColumn(TreeTableView<Person> table, int row, int col) {
        TreeTableViewSelectionModel<Person> sm = table.getSelectionModel();
        TreeTableColumn<Person, ?> column = table.getColumns().get(col);

        sm.select(row, column);
        assertTrue("sanity: row " + row + "contained in selectedIndices", sm.getSelectedIndices().contains(row));
        assertTrue("sanity: row must be selected" , sm.isSelected(row));
        column.setVisible(false);
        assertTrue("after hiding column: row " + row + "contained in selectedIndices", sm.getSelectedIndices().contains(row));
        assertTrue("after hiding column: row must be selected" , sm.isSelected(row));
    }

    /**
     * Creates and returns a TreeTableView with Persons and columns for all their properties.
     */
    private TreeTableView<Person> createPersonTreeTableView() {
        TreeItem<Person> root = new TreeItem<>();
        root.setExpanded(true);
        root.getChildren().addAll(
            new TreeItem<>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
            new TreeItem<>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
            new TreeItem<>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
            new TreeItem<>(new Person("Emma", "Jones", "emma.jones@example.com")),
            new TreeItem<>(new Person("Michael", "Brown", "michael.brown@example.com"))
        );

        TreeTableView<Person> table = new TreeTableView<>();
        table.setRoot(root);

        TreeTableColumn<Person, String> firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn<Person, String> lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));

        TreeTableColumn<Person, String> emailCol = new TreeTableColumn("Email");
        emailCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        return table;
    }
}

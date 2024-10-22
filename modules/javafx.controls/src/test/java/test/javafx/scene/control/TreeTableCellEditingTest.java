/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellEditEvent;
import javafx.scene.control.TreeTableView;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test TreeTableCell editing state updated on re-use (aka after updateIndex(old, new)).
 *
 * This test is parameterized in cellIndex and editingIndex.
 *
 */
public class TreeTableCellEditingTest {
    private TreeTableCell<String,String> cell;
    private TreeTableView<String> table;
    private TreeTableColumn<String, String> editingColumn;
    private ObservableList<TreeItem<String>> model;

//--------------- change off editing index

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOffEditingIndex(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(editingIndex);
        table.edit(editingIndex, editingColumn);
        cell.updateIndex(cellIndex);
        assertEquals(cellIndex, cell.getIndex(), "sanity: cell index changed");
        assertEquals(editingIndex, table.getEditingCell().getRow(), "sanity: treeTable editingIndex must be unchanged");
        assertEquals(editingColumn, table.getEditingCell().getTableColumn(), "sanity: treeTable editingColumn must be unchanged");
        assertFalse(cell.isEditing(), "cell must not be editing on update from editingIndex" + editingIndex + " to cellIndex " + cellIndex);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testCancelOffEditingIndex(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(editingIndex);
        table.edit(editingIndex, editingColumn);
        List<CellEditEvent<String, String>> events = new ArrayList<>();
        editingColumn.setOnEditCancel(e -> {
            events.add(e);
        });
        cell.updateIndex(cellIndex);
        assertEquals(1, events.size(), "cell must have fired edit cancel");
        assertEquals(editingIndex, events.get(0).getTreeTablePosition().getRow(), "cancel event index must be same as editingIndex");
        assertEquals(editingIndex, table.getEditingCell().getRow(), "cancel event index must be same as editingIndex");
    }

//--------------- change to editing index

    @ParameterizedTest
    @MethodSource("parameters")
    public void testToEditingIndex(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(cellIndex);
        table.edit(editingIndex, editingColumn);
        cell.updateIndex(editingIndex);
        assertEquals(editingIndex, cell.getIndex(), "sanity: cell at editing index");
        assertEquals(editingIndex, table.getEditingCell().getRow(), "sanity: treeTable editingIndex must be unchanged");
        assertEquals(editingColumn, table.getEditingCell().getTableColumn(), "sanity: treeTable editingColumn must be unchanged");
        assertTrue(cell.isEditing(), "cell must be editing on update from " + cellIndex + " to editingIndex " + editingIndex);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testStartEvent(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(cellIndex);
        table.edit(editingIndex, editingColumn);
        List<CellEditEvent<String, String>> events = new ArrayList<>();
        editingColumn.setOnEditStart(e -> {
            events.add(e);
        });
        cell.updateIndex(editingIndex);
        assertEquals(1, events.size(), "cell must have fired edit start on update from " + cellIndex + " to " + editingIndex);
        assertEquals(editingIndex, events.get(0).getTreeTablePosition().getRow(), "start event index must be same as editingIndex");
    }

//------------- parameterized

    // (name = "{index}: cellIndex {0}, editingIndex {1}")
    private static Stream<Arguments> parameters() {
        return Stream.of(
            // [0] is cellIndex, [1] is editingIndex
            Arguments.of(1, 2), // normal
            Arguments.of(0, 1), // zero cell index
            Arguments.of(1, 0), // zero editing index
            Arguments.of(-1, 1) // negative cell - JDK-8265206
        );
    }

//-------------- setup and sanity

    /**
     * Sanity: cell editing state updated when on editing index.
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testEditOnCellIndex(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(editingIndex);
        table.edit(editingIndex, editingColumn);
        assertTrue(cell.isEditing(), "sanity: cell must be editing");
    }

    /**
     * Sanity: cell editing state unchanged when off editing index.
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testEditOffCellIndex(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(cellIndex);
        table.edit(editingIndex, editingColumn);
        assertFalse(cell.isEditing(), "sanity: cell editing must be unchanged");
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    private void setup(int cellIndex, int editingIndex) {
        cell = new TreeTableCell<>();
        model = FXCollections.observableArrayList(new TreeItem<>("Four"),
                new TreeItem<>("Five"), new TreeItem<>("Fear")); // "Flop", "Food", "Fizz"
        TreeItem<String> root = new TreeItem<>("root");
        root.getChildren().addAll(model);
        root.setExpanded(true);
        table = new TreeTableView<>(root);
        table.setEditable(true);
        editingColumn = new TreeTableColumn<>("TEST");
        editingColumn.setCellValueFactory(param -> null);
        table.getColumns().add(editingColumn);
        cell.updateTreeTableView(table);
        cell.updateTableColumn(editingColumn);
        // make sure that focus change doesn't interfere with tests
        // (editing cell losing focus will be canceled from focusListener in Cell)
        // Note: not really needed for Tree/TableCell because the cell is never focused
        // if !cellSelectionEnabled nor if not in Tree/TableRow
        // done here for consistency across analogous tests for List/Tree/Cell
        table.getFocusModel().focus(-1);
        assertFalse(cellIndex == editingIndex, "sanity: cellIndex not same as editingIndex");
        assertTrue(editingIndex < model.size(), "sanity: valid editingIndex");
    }
}

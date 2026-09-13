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
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeView.EditEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test TreeCell editing state updated on re-use (aka: updateIndex(old, new)).
 *
 * This test is parameterized in cellIndex and editingIndex.
 *
 */
public class TreeCellEditingTest {
    private TreeCell<String> cell;
    private TreeView<String> tree;
    private ObservableList<TreeItem<String>> model;

//--------------- change off editing index

    @ParameterizedTest
    @MethodSource("parameters")
    public void testOffEditingIndex(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(editingIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        assertTrue(cell.isEditing(), "sanity: cell is editing");
        cell.updateIndex(cellIndex);
        assertEquals(cellIndex, cell.getIndex(), "sanity: cell index changed");
        assertFalse(cell.isEditing(), "cell must not be editing on update from editingIndex " + editingIndex + " to cellIndex " + cellIndex);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testCancelOffEditingIndex(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(editingIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        cell.updateIndex(cellIndex);
        assertEquals(editingItem, tree.getEditingItem(), "sanity: tree editing unchanged");
        assertEquals(editingIndex, tree.getRow(editingItem), "sanity: editingIndex unchanged");
        assertEquals(1, events.size(), "cell must have fired edit cancel");
        assertEquals(editingItem, events.get(0).getTreeItem(), "cancel on updateIndex from " + editingIndex + " to " + cellIndex + "\n  ");
    }

//--------------- change to editing index

    @ParameterizedTest
    @MethodSource("parameters")
    public void testToEditingIndex(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(cellIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        assertFalse(cell.isEditing(), "sanity: cell must not be editing");
        cell.updateIndex(editingIndex);
        assertEquals(editingIndex, cell.getIndex(), "sanity: cell at editing index");
        assertTrue(cell.isEditing(), "cell must be editing on update from " + cellIndex + " to editingIndex " + editingIndex);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testStartEvent(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(cellIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditStart(events::add);
        cell.updateIndex(editingIndex);
        assertEquals(1, events.size(), "cell must have fired edit start on update from " + cellIndex + " to " + editingIndex);
        assertEquals(editingItem, events.get(0).getTreeItem(), "treeItem of start event ");
    }

//------------- parameterized

    private static Stream<Arguments> parameters() {
        // (name = "{index}: cellIndex {0}, editingIndex {1}")
        return Stream.of(
            Arguments.of(1, 2), // normal
            Arguments.of(0, 1), // zero cell index
            Arguments.of(1, 0), // zero editing index
            Arguments.of(-1, 1) // negative cell
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
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
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
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        assertFalse(cell.isEditing(), "sanity: cell editing must be unchanged");
    }

    /**
     * Test do-nothing block in indexChanged (was JDK-8123482, is JDK-8123482)
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testUpdateSameIndexWhileEdititing(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(editingIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        tree.setOnEditCommit(events::add);
        tree.setOnEditStart(events::add);
        cell.updateIndex(editingIndex);
        assertEquals(editingItem, tree.getEditingItem());
        assertTrue(cell.isEditing());
        assertEquals(0, events.size());
    }

    /**
     * Test do-nothing block in indexChanged (was JDK-8123482, is JDK-8123482)
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testUpdateSameIndexWhileNotEdititing(int cellIndex, int editingIndex) {
        setup(cellIndex, editingIndex);
        cell.updateIndex(cellIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        tree.setOnEditCommit(events::add);
        tree.setOnEditStart(events::add);
        cell.updateIndex(cellIndex);
        assertEquals(editingItem, tree.getEditingItem());
        assertFalse(cell.isEditing());
        assertEquals(0, events.size());
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    private void setup(int cellIndex, int editingIndex) {
        cell = new TreeCell<>();
        model = FXCollections.observableArrayList(new TreeItem<>("zero"),
                new TreeItem<>("one"), new TreeItem<>("two"));
        TreeItem<String> root = new TreeItem<>("root");
        root.getChildren().addAll(model);
        root.setExpanded(true);
        tree = new TreeView<>(root);
        tree.setEditable(true);
        tree.setShowRoot(false);
        // make sure that focus change doesn't interfere with tests
        // (editing cell losing focus will be canceled from focusListener in Cell)
        tree.getFocusModel().focus(-1);
        cell.updateTreeView(tree);
        assertFalse(cellIndex == editingIndex, "sanity: cellIndex not same as editingIndex");
        assertTrue(editingIndex < model.size(), "sanity: valid editingIndex");
    }
}

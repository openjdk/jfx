/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeView.EditEvent;

/**
 * Test TreeCell editing state updated on re-use (aka: updateIndex(old, new)).
 *
 * This test is parameterized in cellIndex and editingIndex.
 *
 */
@RunWith(Parameterized.class)
public class TreeCellEditingTest {
    private TreeCell<String> cell;
    private TreeView<String> tree;
    private ObservableList<TreeItem<String>> model;

    private int cellIndex;
    private int editingIndex;

//--------------- change off editing index

    @Test
    public void testOffEditingIndex() {
        cell.updateIndex(editingIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        assertTrue("sanity: cell is editing", cell.isEditing());
        cell.updateIndex(cellIndex);
        assertEquals("sanity: cell index changed", cellIndex, cell.getIndex());
        assertFalse("cell must not be editing on update from editingIndex " + editingIndex
                + " to cellIndex " + cellIndex, cell.isEditing());
    }

    @Test
    public void testCancelOffEditingIndex() {
        cell.updateIndex(editingIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        cell.updateIndex(cellIndex);
        assertEquals("sanity: tree editing unchanged", editingItem, tree.getEditingItem());
        assertEquals("sanity: editingIndex unchanged", editingIndex, tree.getRow(editingItem));
        assertEquals("cell must have fired edit cancel", 1, events.size());
    }

    /**
     * Extracted from testCancelOffEditingIndex to formally ignore
     * FIXME: move the assert to the other method, once the issue is solved
     */
    @Ignore("JDK-8267094")
    @Test
    public void testCancelOffEditingIndexEventIndex() {
        cell.updateIndex(editingIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        cell.updateIndex(cellIndex);
        assertEquals("cancel on updateIndex from " + editingIndex + " to " + cellIndex + "\n  ",
                editingItem, events.get(0).getTreeItem());
    }

//--------------- change to editing index

    @Test
    public void testToEditingIndex() {
        cell.updateIndex(cellIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        assertFalse("sanity: cell must not be editing", cell.isEditing());
        cell.updateIndex(editingIndex);
        assertEquals("sanity: cell at editing index", editingIndex, cell.getIndex());
        assertTrue("cell must be editing on update from " + cellIndex
                + " to editingIndex " + editingIndex, cell.isEditing());
    }

    @Test
    public void testStartEvent() {
        cell.updateIndex(cellIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditStart(events::add);
        cell.updateIndex(editingIndex);
        assertEquals("cell must have fired edit start on update from " + cellIndex + " to " + editingIndex,
                1, events.size());
        assertEquals("treeItem of start event ", editingItem, events.get(0).getTreeItem());
    }

//------------- parameterized

    // Note: name property not supported before junit 4.11
    @Parameterized.Parameters //(name = "{index}: cellIndex {0}, editingIndex {1}")
    public static Collection<Object[]> data() {
     // [0] is cellIndex, [1] is editingIndex
        Object[][] data = new Object[][] {
            {1, 2}, // normal
            {0, 1}, // zero cell index
            {1, 0}, // zero editing index
            {-1, 1}, // negative cell
        };
        return Arrays.asList(data);
    }

    public TreeCellEditingTest(int cellIndex, int editingIndex) {
        this.cellIndex = cellIndex;
        this.editingIndex = editingIndex;
    }

//-------------- setup and sanity

    /**
     * Sanity: cell editing state updated when on editing index.
     */
    @Test
    public void testEditOnCellIndex() {
        cell.updateIndex(editingIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        assertTrue("sanity: cell must be editing", cell.isEditing());
    }

    /**
     * Sanity: cell editing state unchanged when off editing index.
     */
    @Test
    public void testEditOffCellIndex() {
        cell.updateIndex(cellIndex);
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        assertFalse("sanity: cell editing must be unchanged", cell.isEditing());
    }

    /**
     * Test do-nothing block in indexChanged (was RT-31165, is JDK-8123482)
     */
    @Test
    public void testUpdateSameIndexWhileEdititing() {
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
     * Test do-nothing block in indexChanged (was RT-31165, is JDK-8123482)
     */
    @Test
    public void testUpdateSameIndexWhileNotEdititing() {
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

    @Before public void setup() {
        cell = new TreeCell<String>();
        model = FXCollections.observableArrayList(new TreeItem<String>("zero"),
                new TreeItem<String>("one"), new TreeItem<String>("two"));
        TreeItem<String> root = new TreeItem<>("root");
        root.getChildren().addAll(model);
        root.setExpanded(true);
        tree = new TreeView<String>(root);
        tree.setEditable(true);
        tree.setShowRoot(false);
        // make sure that focus change doesn't interfere with tests
        // (editing cell loosing focus will be canceled from focusListener in Cell)
        tree.getFocusModel().focus(-1);
        cell.updateTreeView(tree);
        assertFalse("sanity: cellIndex not same as editingIndex", cellIndex == editingIndex);
        assertTrue("sanity: valid editingIndex", editingIndex < model.size());
    }

}

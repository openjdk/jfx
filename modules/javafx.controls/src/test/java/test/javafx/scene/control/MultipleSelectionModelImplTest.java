/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.FocusModel;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ListViewShim;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.MultipleSelectionModelShim;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TableViewShim;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.TreeTableViewFocusModel;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.scene.control.TreeTableViewShim;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeViewShim;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import test.javafx.collections.MockListObserver;

/**
 * Unit tests for the SelectionModel abstract class used by ListView
 * and TreeView. This unit test attempts to test all known implementations, and
 * as such contains some conditional logic to handle the various controls as
 * simply as possible.
 */
@RunWith(Parameterized.class)
public class MultipleSelectionModelImplTest {

    private MultipleSelectionModel model;
    private FocusModel focusModel;

    private Class<? extends MultipleSelectionModel> modelClass;
    private Control currentControl;

    // ListView
    private ListView<String> listView;

    // ListView model data
    private static ObservableList<String> defaultData = FXCollections.<String>observableArrayList();
    private static ObservableList<String> data = FXCollections.<String>observableArrayList();
    private static final String ROW_1_VALUE = "Row 1";
    private static final String ROW_2_VALUE = "Row 2";
    private static final String ROW_3_VALUE = "Long Row 3";
    private static final String ROW_5_VALUE = "Row 5";
    private static final String ROW_20_VALUE = "Row 20";

    // TreeView
    private TreeView treeView;
    private TreeItem<String> root;
    private TreeItem<String> ROW_2_TREE_VALUE;
    private TreeItem<String> ROW_5_TREE_VALUE;

    // TreeTableView
    private TreeTableView treeTableView;

    // TableView
    private TableView tableView;

    @Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { ListViewShim.get_ListViewBitSetSelectionModel_class() },
            { TreeViewShim.get_TreeViewBitSetSelectionModel_class() },
            { TableViewShim.get_TableViewArrayListSelectionModel_class() },
            { TreeTableViewShim.get_TreeTableViewArrayListSelectionModel_class() },
        });
    }

    public MultipleSelectionModelImplTest(Class<? extends MultipleSelectionModel> modelClass) {
        this.modelClass = modelClass;
    }

    @AfterClass public static void tearDownClass() throws Exception {    }

    @Before public void setUp() throws Exception {
        // ListView init
        defaultData.setAll(ROW_1_VALUE, ROW_2_VALUE, ROW_3_VALUE, "Row 4", ROW_5_VALUE, "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", ROW_20_VALUE);

        data.setAll(defaultData);
        listView = new ListView<>(data);
        // --- ListView init

        // TreeView init
        root = new TreeItem<>(ROW_1_VALUE);
        root.setExpanded(true);
        for (int i = 1; i < data.size(); i++) {
            root.getChildren().add(new TreeItem<>(data.get(i)));
        }
        ROW_2_TREE_VALUE = root.getChildren().get(0);
        ROW_5_TREE_VALUE = root.getChildren().get(3);

        treeView = new TreeView(root);
        // --- TreeView init

        // TreeTableView init
        treeTableView = new TreeTableView(root);
        // --- TreeView init

        // TableView init
        tableView = new TableView();
        tableView.setItems(data);
        // --- TableView init

        try {
            // reset the data model
            data = FXCollections.<String>observableArrayList();
            data.setAll(defaultData);

            // we create a new SelectionModel per test to ensure it is always back
            // at the default settings
            if (modelClass.equals(ListViewShim.get_ListViewBitSetSelectionModel_class())) {
                // recreate the selection model
                model = MultipleSelectionModelShim.newInstance_from_class(modelClass, ListView.class, listView);
                listView.setSelectionModel((MultipleSelectionModel<String>)model);

                // create a new focus model
                focusModel = ListViewShim.getListViewFocusModel(listView);
                listView.setFocusModel(focusModel);
                currentControl = listView;
            } else if (modelClass.equals(TreeViewShim.get_TreeViewBitSetSelectionModel_class())) {
                model = MultipleSelectionModelShim.newInstance_from_class(modelClass, TreeView.class, treeView);
                treeView.setSelectionModel((MultipleSelectionModel<String>)model);
                focusModel = treeView.getFocusModel();

                // create a new focus model
                focusModel = TreeViewShim.get_TreeViewFocusModel(treeView);
                treeView.setFocusModel(focusModel);
                currentControl = treeView;
            } else if (TableViewSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = MultipleSelectionModelShim.newInstance_from_class(modelClass, TableView.class, tableView);
                tableView.setSelectionModel((TableViewSelectionModel) model);

                // create a new focus model
                focusModel = new TableViewFocusModel(tableView);
                tableView.setFocusModel((TableViewFocusModel) focusModel);
                currentControl = tableView;
            } else if (TreeTableViewSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = MultipleSelectionModelShim.newInstance_from_class(modelClass, TreeTableView.class, treeTableView);
                treeTableView.setSelectionModel((TreeTableViewSelectionModel) model);

                // create a new focus model
                focusModel = new TreeTableViewFocusModel(treeTableView);
                treeTableView.setFocusModel((TreeTableViewFocusModel) focusModel);
                currentControl = treeTableView;
            }

            // ensure the selection mode is set to multiple
            model.setSelectionMode(SelectionMode.MULTIPLE);
        } catch (Exception ex) {
            throw ex;
        }
    }

    @After public void tearDown() {
        model = null;
    }

    private Object getValue(Object item) {
        if (item instanceof TreeItem) {
            return ((TreeItem)item).getValue();
        }
        return item;
    }

    private String indices(MultipleSelectionModel sm) {
        return "Selected Indices: " + sm.getSelectedIndices();
    }

    private String items(MultipleSelectionModel sm) {
        return "Selected Items: " + sm.getSelectedItems();
    }

    private MultipleSelectionModel msModel() {
        return model;
    }

    private boolean isTree() {
        return TreeViewShim.is_TreeViewBitSetSelectionModel(model) ||
               TreeTableViewShim.instanceof_TreeTableViewArrayListSelectionModel(model);
    }

    private void ensureInEmptyState() {
        assertEquals(-1, model.getSelectedIndex());
        assertNull(model.getSelectedItem());

        if (focusModel != null) {
            assertEquals(-1, focusModel.getFocusedIndex());
            assertNull(focusModel.getFocusedItem());
        }

        assertNotNull(msModel().getSelectedIndices());
        assertNotNull(msModel().getSelectedItems());
        assertEquals(0, msModel().getSelectedIndices().size());
        assertEquals(0, msModel().getSelectedItems().size());
    }

    @Test public void ensureInDefaultState() {
        assertEquals(-1, model.getSelectedIndex());
        assertNull(model.getSelectedItem());

        if (focusModel != null) {
            assertEquals(0, focusModel.getFocusedIndex());
            assertNotNull(focusModel.getFocusedItem());
        }

        assertNotNull(msModel().getSelectedIndices());
        assertNotNull(msModel().getSelectedItems());
        assertEquals(0, msModel().getSelectedIndices().size());
        assertEquals(0, msModel().getSelectedItems().size());
    }

    @Test public void selectValidIndex() {
        int index = 4;
        model.clearSelection();
        model.select(index);

        assertEquals(index, model.getSelectedIndex());
        assertNotNull(model.getSelectedItem());
        assertEquals(ROW_5_VALUE, getValue(model.getSelectedItem()));

        if (focusModel != null) {
            assertEquals(index, focusModel.getFocusedIndex());
            assertNotNull(focusModel.getFocusedItem());
            assertEquals(ROW_5_VALUE, getValue(focusModel.getFocusedItem()));
        }

        assertNotNull(msModel().getSelectedIndices());
        assertNotNull(msModel().getSelectedItems());
        assertEquals(1, msModel().getSelectedIndices().size());
        assertEquals(index, msModel().getSelectedIndices().get(0));
        assertEquals(1, msModel().getSelectedItems().size());
        assertEquals(ROW_5_VALUE, getValue(msModel().getSelectedItems().get(0)));
    }

    @Test public void testSelectAllWithSingleSelection() {
        msModel().setSelectionMode(SelectionMode.SINGLE);
        msModel().selectAll();
        ensureInDefaultState();
    }

    @Test public void testSelectAllWithMultipleSelection() {
        msModel().clearSelection();
        msModel().selectAll();

        assertEquals(19, model.getSelectedIndex());
        assertNotNull(model.getSelectedItem());
        assertEquals(ROW_20_VALUE, getValue(model.getSelectedItem()));
        assertEquals(19, focusModel.getFocusedIndex());
        assertNotNull(focusModel.getFocusedItem());
        assertEquals(ROW_20_VALUE, getValue(focusModel.getFocusedItem()));
        assertNotNull(msModel().getSelectedIndices());
        assertNotNull(msModel().getSelectedItems());
        assertEquals(20, msModel().getSelectedIndices().size());
        assertEquals(20, msModel().getSelectedItems().size());
    }

    @Test public void clearAllSelection() {
        msModel().selectAll();
        model.clearSelection();
        ensureInEmptyState();
    }

    @Test public void clearPartialSelectionWithSingleSelection() {
        model.clearSelection();
        assertFalse(model.isSelected(5));
        model.select(5);
        assertTrue(model.isSelected(5));
        model.clearSelection(5);
        assertFalse(model.isSelected(5));

        assertEquals(0, msModel().getSelectedIndices().size());
        assertEquals(0, msModel().getSelectedItems().size());
    }

    @Test public void clearPartialSelectionWithMultipleSelection() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectAll();
        model.clearSelection(5);

        assertTrue(model.isSelected(4));
        assertFalse(model.isSelected(5));
        assertTrue(model.isSelected(6));
    }

    @Test public void selectedIndicesListenerReportsCorrectIndexOnClearSelection() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.select(1);
        model.select(5);
        MockListObserver<Integer> observer = new MockListObserver<>();
        model.getSelectedIndices().addListener(observer);
        model.clearSelection(5);

        observer.check1();
        observer.checkAddRemove(0, model.getSelectedIndices(), List.of(5), 1, 1);
    }

    @Test public void testSelectedIndicesObservableListIsEmpty() {
        assertTrue(msModel().getSelectedIndices().isEmpty());
    }

    @Test public void testSelectedIndicesIteratorIsNotNull() {
        assertNotNull(msModel().getSelectedIndices().iterator());
    }

    @Test public void testSelectedIndicesIteratorHasNoNext() {
        assertFalse(msModel().getSelectedIndices().iterator().hasNext());
    }

    @Test public void testSelectedIndicesIteratorWorksWithSingleSelection() {
        msModel().clearSelection();
        model.select(5);

        Iterator<Integer> it = msModel().getSelectedIndices().iterator();
        assertEquals(5, (int) it.next());
        assertFalse(it.hasNext());
    }

    @Test public void testSelectedIndicesIteratorWorksWithMultipleSelection() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectIndices(1, 2, 5);

        Iterator<Integer> it = msModel().getSelectedIndices().iterator();
        assertEquals(indices(msModel()), 1, (int) it.next());
        assertEquals(indices(msModel()), 2, (int) it.next());
        assertEquals(indices(msModel()), 5, (int) it.next());
        assertFalse(it.hasNext());
    }

    @Test public void testSelectedIndicesContains() {
        model.select(5);
        assertTrue(msModel().getSelectedIndices().contains(5));
    }

    @Test public void testSelectedItemsObservableListIsEmpty() {
        assertTrue(msModel().getSelectedItems().isEmpty());
    }

    @Test public void testSelectedItemsIndexOf() {
        msModel().clearSelection();
        if (isTree()) {
            model.select(ROW_2_TREE_VALUE);
            assertEquals(1, msModel().getSelectedItems().size());
            assertEquals(0, msModel().getSelectedItems().indexOf(ROW_2_TREE_VALUE));
        } else {
            model.select(ROW_2_VALUE);
            assertEquals(1, msModel().getSelectedItems().size());
            assertEquals(0, msModel().getSelectedItems().indexOf(ROW_2_VALUE));
        }
    }

    @Test public void testSelectedItemsIteratorIsNotNull() {
        assertNotNull(msModel().getSelectedIndices().iterator());
    }

    @Test public void testSelectedItemsLastIndexOf() {
        msModel().clearSelection();
        if (isTree()) {
            model.select(ROW_2_TREE_VALUE);
            assertEquals(1, msModel().getSelectedItems().size());
            assertEquals(0, msModel().getSelectedItems().lastIndexOf(ROW_2_TREE_VALUE));
        } else {
            model.select(ROW_2_VALUE);
            assertEquals(1, msModel().getSelectedItems().size());
            assertEquals(0, msModel().getSelectedItems().lastIndexOf(ROW_2_VALUE));
        }
    }

    @Test public void testSelectedItemsContains() {
        model.select(5);
        assertTrue(msModel().getSelectedIndices().contains(5));
    }

    @Test public void testSingleSelectionMode() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.SINGLE);
        assertTrue(model.isEmpty());

        model.select(5);
        assertTrue(model.isSelected(5));

        model.select(10);
        assertTrue(model.isSelected(10));
        assertFalse(model.isSelected(5));

        assertEquals(1, msModel().getSelectedIndices().size());
        assertEquals(1, msModel().getSelectedItems().size());
    }

    @Test public void testMultipleSelectionMode() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        assertTrue(model.isEmpty());

        model.select(5);
        assertTrue(model.isSelected(5));

        model.select(10);
        assertTrue(model.isSelected(10));
        assertTrue(model.isSelected(5));
        assertEquals(2, msModel().getSelectedIndices().size());
        assertEquals(2, msModel().getSelectedItems().size());
    }

    @Test public void testChangeSelectionMode() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectIndices(5, 10, 15);
        assertEquals(indices(msModel()), 3, msModel().getSelectedIndices().size());
        assertEquals(3, msModel().getSelectedItems().size());
        assertTrue(model.isSelected(5));
        assertTrue(model.isSelected(10));
        assertTrue(model.isSelected(15));

        msModel().setSelectionMode(SelectionMode.SINGLE);
        assertEquals(1, msModel().getSelectedIndices().size());
        assertEquals(1, msModel().getSelectedItems().size());
        assertFalse(indices(msModel()), model.isSelected(5));
        assertFalse(indices(msModel()), model.isSelected(10));
        assertTrue(indices(msModel()), model.isSelected(15));
    }

//    @Test public void testSelectNullObject() {
//        model.select(null);
//    }

    @Test public void testSelectRange() {
        // should select all indices starting at 5, and finishing just before
        // the 10th item
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectRange(5, 10);
        assertEquals(indices(msModel()), 5, msModel().getSelectedIndices().size());
        assertEquals(5, msModel().getSelectedItems().size());
        assertFalse(model.isSelected(4));
        assertTrue(model.isSelected(5));
        assertTrue(model.isSelected(6));
        assertTrue(model.isSelected(7));
        assertTrue(model.isSelected(8));
        assertTrue(model.isSelected(9));
        assertFalse(model.isSelected(10));
    }

    @Test public void testDeselectionFromASelectionRange() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectRange(2, 10);
        model.clearSelection(5);
        assertTrue(indices(msModel()), model.isSelected(4));
        assertFalse(indices(msModel()), model.isSelected(5));
        assertTrue(indices(msModel()), model.isSelected(6));
    }

    @Test public void testAccurateItemSelection() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectRange(2, 5);
        ObservableList selectedItems = msModel().getSelectedItems();
        assertEquals(3, selectedItems.size());

        if (isTree()) {
            assertFalse(selectedItems.contains(root.getChildren().get(0)));
            assertTrue(selectedItems.contains(root.getChildren().get(1)));
            assertTrue(selectedItems.contains(root.getChildren().get(2)));
            assertTrue(selectedItems.contains(root.getChildren().get(3)));
            assertFalse(selectedItems.contains(root.getChildren().get(4)));
        } else {
            assertFalse(selectedItems.contains(data.get(1)));
            assertTrue(selectedItems.contains(data.get(2)));
            assertTrue(selectedItems.contains(data.get(3)));
            assertTrue(selectedItems.contains(data.get(4)));
            assertFalse(selectedItems.contains(data.get(5)));
        }
    }

    @Test public void ensureSelectedIndexAndItemIsAlwaysTheLastSelectionWithSelect() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        model.select(3);
        assertEquals(3, model.getSelectedIndex());
        if (isTree()) {
            assertEquals(root.getChildren().get(2), model.getSelectedItem());
        } else {
            assertEquals(data.get(3), model.getSelectedItem());
        }

        model.select(1);
        assertEquals(1, model.getSelectedIndex());
        if (isTree()) {
            assertEquals(root.getChildren().get(0), model.getSelectedItem());
        } else {
            assertEquals(data.get(1), model.getSelectedItem());
        }

        model.select(5);
        assertEquals(5, model.getSelectedIndex());
        if (isTree()) {
            assertEquals(root.getChildren().get(4), model.getSelectedItem());
        } else {
            assertEquals(data.get(5), model.getSelectedItem());
        }

        model.select(1);
        assertEquals(1, model.getSelectedIndex());
        if (isTree()) {
            assertEquals(root.getChildren().get(0), model.getSelectedItem());
        } else {
            assertEquals(data.get(1), model.getSelectedItem());
        }
    }

    @Test public void ensureSelectedIndexAndItemIsAlwaysTheLastSelectionWithMultipleSelect() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectIndices(3,4,5);
        assertEquals(5, model.getSelectedIndex());

        if (isTree()) {
            assertEquals(root.getChildren().get(4), model.getSelectedItem());
        } else {
            assertEquals(data.get(5), model.getSelectedItem());
        }

        model.select(1);
        assertEquals(1, model.getSelectedIndex());

        if (isTree()) {
            assertEquals(root.getChildren().get(0), model.getSelectedItem());
        } else {
            assertEquals(data.get(1), model.getSelectedItem());
        }

        msModel().selectIndices(8,7,6);
        assertEquals(6, model.getSelectedIndex());
        if (isTree()) {
            assertEquals(root.getChildren().get(5), model.getSelectedItem());
        } else {
            assertEquals(data.get(6), model.getSelectedItem());
        }
    }

    @Test public void ensureSelectedIndexAndItemIsAlwaysTheLastSelectionWithSelectRange() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectRange(3,10);
        assertEquals(9, model.getSelectedIndex());
        if (isTree()) {
            assertEquals(root.getChildren().get(8), model.getSelectedItem());
        } else {
            assertEquals(data.get(9), model.getSelectedItem());
        }

        model.select(1);
        assertEquals(1, model.getSelectedIndex());
        if (isTree()) {
            assertEquals(root.getChildren().get(0), model.getSelectedItem());
        } else {
            assertEquals(data.get(1), model.getSelectedItem());
        }

        msModel().selectRange(6, 8);
        assertEquals(7, model.getSelectedIndex());
        if (isTree()) {
            assertEquals(root.getChildren().get(6), model.getSelectedItem());
        } else {
            assertEquals(data.get(7), model.getSelectedItem());
        }
    }

    @Test public void testMultipleSelectionWithEmptyArray() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectIndices(3,new int[] { });
        assertEquals(3, model.getSelectedIndex());
        assertEquals(1, msModel().getSelectedIndices().size());
        assertEquals(1, msModel().getSelectedItems().size());

        if (isTree()) {
            assertEquals(root.getChildren().get(2), model.getSelectedItem());
        } else {
            assertEquals(data.get(3), model.getSelectedItem());
        }
    }

    @Test public void selectOnlyValidIndicesInSingleSelection() {
        msModel().setSelectionMode(SelectionMode.SINGLE);
        msModel().selectIndices(750397, 3, 709709375, 4, 8597998, 47929);
        assertEquals(4, model.getSelectedIndex());
        assertFalse(model.isSelected(3));
        assertTrue(model.isSelected(4));
        assertEquals(1, msModel().getSelectedIndices().size());
        assertEquals(1, msModel().getSelectedItems().size());

        if (isTree()) {
            assertEquals(root.getChildren().get(3), model.getSelectedItem());
        } else {
            assertEquals(data.get(4), model.getSelectedItem());
        }
    }

    @Test public void selectOnlyValidIndicesInMultipleSelection() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        model.clearSelection();
        msModel().selectIndices(750397, 3, 709709375, 4, 8597998, 47929);
        assertEquals(4, model.getSelectedIndex());
        assertTrue(model.isSelected(3));
        assertTrue(model.isSelected(4));
        assertEquals(2, msModel().getSelectedIndices().size());
        assertEquals(2, msModel().getSelectedItems().size());

        if (isTree()) {
            assertEquals(root.getChildren().get(3), model.getSelectedItem());
        } else {
            assertEquals(data.get(4), model.getSelectedItem());
        }
    }

    @Test public void testNullArrayInMultipleSelection() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectIndices(-20, null);
        assertEquals(-1, model.getSelectedIndex());
        assertNull(model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test public void testMultipleSelectionWithInvalidIndices() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectIndices(-20, 23505, 78125);
        assertEquals(-1, model.getSelectedIndex());
        assertNull(model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test public void testInvalidSelection() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.SINGLE);
        msModel().selectIndices(-20, null);
        assertEquals(-1, model.getSelectedIndex());
        assertNull(model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test public void ensureSwappedSelectRangeWorks() {
        // first test a valid range - there should be 6 selected items
        model.clearSelection();
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.selectRange(3, 10);
        assertEquals(indices(model), 7, model.getSelectedIndices().size());
        assertEquals(9, model.getSelectedIndex());
        if (isTree()) {
            assertEquals(root.getChildren().get(8), model.getSelectedItem());
        } else {
            assertEquals(data.get(9), model.getSelectedItem());
        }

        model.clearSelection();

        // we should select the range from 10 - 4 inclusive
        model.selectRange(10, 3);
        assertEquals(7, model.getSelectedIndices().size());
        assertEquals(indices(model), 4, model.getSelectedIndex());
        if (isTree()) {
            assertEquals(root.getChildren().get(3), model.getSelectedItem());
        } else {
            assertEquals(data.get(4), model.getSelectedItem());
        }
    }

    @Test public void testInvalidSelectRange() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectRange(200, 220);
        assertEquals(-1, model.getSelectedIndex());
        assertEquals(null, model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test public void testEmptySelectRange() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectRange(10, 10);
        assertEquals(-1, model.getSelectedIndex());
        assertEquals(null, model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test public void testNegativeSelectRange() {
        msModel().clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectRange(-10, -1);
        assertEquals(-1, model.getSelectedIndex());
        assertEquals(null, model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullListViewInSelectionModel() {
        ListViewShim.getListViewBitSetSelectionModel(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullTreeViewInSelectionModel() {
        TreeViewShim.<String>get_TreeViewBitSetSelectionModel(null);
    }

    @Test public void selectAllInEmptySingleSelectionMode() {
        model.clearSelection();
        msModel().setSelectionMode(SelectionMode.SINGLE);
        assertTrue(model.isEmpty());
        msModel().selectAll();
        assertTrue(model.isEmpty());
    }

    @Test public void selectAllInSingleSelectionModeWithSelectedRow() {
        msModel().setSelectionMode(SelectionMode.SINGLE);
        model.clearSelection();
        assertTrue(model.isEmpty());
        model.select(3);
        msModel().selectAll();
        assertTrue(model.isSelected(3));
        assertEquals(1, msModel().getSelectedIndices().size());
    }

    @Test public void selectionModePropertyHasReferenceToBean() {
        assertSame(model, model.selectionModeProperty().getBean());
    }

    @Test public void selectionModePropertyHasName() {
        assertSame("selectionMode", model.selectionModeProperty().getName());
    }

    @Ignore("Not yet implemented in TreeView and TableView")
    @Test public void testSelectionChangesWhenItemIsInsertedAtStartOfModel() {
        /* Select the fourth item, and insert a new item at the start of the
         * data model. The end result should be that the fourth item should NOT
         * be selected, and the fifth item SHOULD be selected.
         */
        model.select(3);
        assertTrue(model.isSelected(3));
        data.add(0, "Inserted String");
        assertFalse(model.isSelected(3));
        assertTrue(model.isSelected(4));
    }

    private int rt_28615_row_1_hit_count = 0;
    private int rt_28615_row_2_hit_count = 0;
    @Test public void test_rt_28615() {
        model.clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);

        msModel().getSelectedItems().addListener((ListChangeListener.Change change) -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Object item : change.getAddedSubList()) {
                        if (isTree()) {
                            if (root.equals(item)) {
                                rt_28615_row_1_hit_count++;
                            } else if (ROW_2_TREE_VALUE.equals(item)) {
                                rt_28615_row_2_hit_count++;
                            }
                        } else {
                            if (ROW_1_VALUE.equals(item)) {
                                rt_28615_row_1_hit_count++;
                            } else if (ROW_2_VALUE.equals(item)) {
                                rt_28615_row_2_hit_count++;
                            }
                        }
                    }
                }
            }
        });

        assertEquals(0, rt_28615_row_1_hit_count);
        assertEquals(0, rt_28615_row_2_hit_count);

        msModel().select(0);
        assertEquals(1, rt_28615_row_1_hit_count);
        assertEquals(0, rt_28615_row_2_hit_count);

        msModel().select(1);
        assertEquals(1, rt_28615_row_1_hit_count);
        assertEquals(1, rt_28615_row_2_hit_count);
    }

    private int rt_29860_size_count = 0;
    @Test public void test_rt_29860_add() {
        model.clearSelection();
        msModel().setSelectionMode(SelectionMode.MULTIPLE);

        msModel().getSelectedIndices().addListener((ListChangeListener.Change change) -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    rt_29860_size_count += change.getAddedSize();
                }
            }
        });

        assertEquals(0, rt_29860_size_count);

        // 0,1,2,3 are all selected. The bug is not that the msModel().getSelectedIndices()
        // list is wrong (it isn't - it's correct). The bug is that the addedSize
        // reported in the callback above is incorrect.
        msModel().selectIndices(0, 1, 2, 3);
        assertEquals(msModel().getSelectedIndices().toString(), 4, rt_29860_size_count);
        rt_29860_size_count = 0;

        msModel().selectIndices(0,1,2,3,4);
        assertEquals(msModel().getSelectedIndices().toString(), 1, rt_29860_size_count);   // only 4 was selected
        rt_29860_size_count = 0;

        msModel().selectIndices(6,7,8);
        assertEquals(3, rt_29860_size_count);   // 6,7,8 was selected
    }

    @Test public void test_rt_29821() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);

        IndexedCell cell_3 = VirtualFlowTestUtils.getCell(currentControl, 3);
        assertNotNull(cell_3);
        assertFalse(cell_3.isSelected());

        msModel().clearSelection();
        msModel().select(3);
        assertTrue(cell_3.isSelected());
        assertEquals(1, msModel().getSelectedIndices().size());

        // in multiple selection passing in select(null) is a no-op. In single
        // selection (tested elsewhere), this would result in a clearSelection()
        // call
        msModel().select(null);
        assertTrue(msModel().isSelected(3));
        assertTrue(cell_3.isSelected());
    }

    private int rt_32411_add_count = 0;
    private int rt_32411_remove_count = 0;
    @Test public void test_rt_32411_selectedItems() {
        model.clearSelection();

        model.getSelectedItems().addListener((ListChangeListener.Change change) -> {
            while (change.next()) {
                rt_32411_remove_count += change.getRemovedSize();
                rt_32411_add_count += change.getAddedSize();
            }
        });

        // reset fields
        rt_32411_add_count = 0;
        rt_32411_remove_count = 0;

        // select a row - no problems here
        model.select(2);
        assertEquals(1, rt_32411_add_count);
        assertEquals(0, rt_32411_remove_count);

        // clear and select a new row. We should receive a remove event followed
        // by an added event - but this bug shows we don't get the remove event.
        model.clearAndSelect(4);
        assertEquals(2, rt_32411_add_count);
        assertEquals(1, rt_32411_remove_count);
    }

    @Test public void test_rt_32411_selectedIndices() {
        model.clearSelection();

        model.getSelectedIndices().addListener((ListChangeListener.Change change) -> {
            while (change.next()) {
                rt_32411_remove_count += change.getRemovedSize();
                rt_32411_add_count += change.getAddedSize();
            }
        });

        // reset fields
        rt_32411_add_count = 0;
        rt_32411_remove_count = 0;

        // select a row - no problems here
        model.select(2);
        assertEquals(1, rt_32411_add_count);
        assertEquals(0, rt_32411_remove_count);

        // clear and select a new row. We should receive a remove event followed
        // by an added event - but this bug shows we don't get the remove event.
        model.clearAndSelect(4);
        assertEquals(2, rt_32411_add_count);
        assertEquals(1, rt_32411_remove_count);
    }

    private int rt32618_count = 0;
    @Test public void test_rt32618_multipleSelection() {
        model.selectedItemProperty().addListener((ov, t, t1) -> rt32618_count++);

        assertEquals(0, rt32618_count);

        model.select(1);
        assertEquals(1, rt32618_count);
        assertEquals(ROW_2_VALUE, getValue(model.getSelectedItem()));

        model.clearAndSelect(2);
        assertEquals(2, rt32618_count);
        assertEquals(ROW_3_VALUE, getValue(model.getSelectedItem()));
    }

    @Test public void test_rt33324_selectedIndices() {
        // pre-select item 0
        model.select(0);

        // install listener
        model.getSelectedIndices().addListener((ListChangeListener.Change change) -> {
            while (change.next()) {
                assertTrue(change.wasRemoved());
                assertTrue(change.wasAdded());
                assertTrue(change.wasReplaced());

                assertFalse(change.wasPermutated());
            }
        });

        // change selection to index 1. This should result in a change event
        // being fired where wasAdded() is true, wasRemoved() is true, but most
        // importantly, wasReplaced() is true
        model.clearAndSelect(1);
    }

    @Test public void test_rt33324_selectedItems() {
        // pre-select item 0
        model.select(0);

        // install listener
        model.getSelectedItems().addListener((ListChangeListener.Change change) -> {
            while (change.next()) {
                assertTrue(change.wasRemoved());
                assertTrue(change.wasAdded());
                assertTrue(change.wasReplaced());

                assertFalse(change.wasPermutated());
            }
        });

        // change selection to index 1. This should result in a change event
        // being fired where wasAdded() is true, wasRemoved() is true, but most
        // importantly, wasReplaced() is true
        model.clearAndSelect(1);
    }

    @Test public void test_rt33324_selectedCells() {
        if (! (msModel() instanceof TableViewSelectionModel)) {
            return;
        }

        TableViewSelectionModel tableSM = (TableViewSelectionModel) msModel();

        // pre-select item 0
        tableSM.select(0);

        // install listener
        tableSM.getSelectedCells().addListener((ListChangeListener.Change change) -> {
            while (change.next()) {
                assertTrue(change.wasRemoved());
                assertTrue(change.wasAdded());
                assertTrue(change.wasReplaced());

                assertFalse(change.wasPermutated());
            }
        });

        // change selection to index 1. This should result in a change event
        // being fired where wasAdded() is true, wasRemoved() is true, but most
        // importantly, wasReplaced() is true
        tableSM.clearAndSelect(1);
    }

    @Test public void test_rt35624_selectedIndices_downwards() {
        model.clearSelection();
        model.select(2);

        msModel().getSelectedIndices().addListener((ListChangeListener.Change change) -> {
            while (change.next()) {
                // we expect two items in the added list: 3 and 4, as index
                // 2 has previously been selected
                Assert.assertEquals(2, change.getAddedSize());
                Assert.assertEquals(FXCollections.observableArrayList(3, 4), change.getAddedSubList());
            }

            // In the actual list, we expect three items: 2, 3 and 4
            Assert.assertEquals(3, change.getList().size());
            Assert.assertEquals(FXCollections.observableArrayList(2, 3, 4), change.getList());
        });

        model.selectIndices(2, 3, 4);
    }

    @Test public void test_rt35624_selectedIndices_upwards() {
        model.clearSelection();
        model.select(4);

        msModel().getSelectedIndices().addListener(((ListChangeListener.Change change) -> {
            while (change.next()) {
                // we expect two items in the added list: 3 and 2, as index
                // 4 has previously been selected
                assertEquals(2, change.getAddedSize());
                assertEquals(FXCollections.observableArrayList(2, 3), change.getAddedSubList());
            }

            // In the actual list, we expect three items: 2, 3 and 4
            assertEquals(3, change.getList().size());
            assertEquals(FXCollections.observableArrayList(2, 3, 4), change.getList());
        }));

        model.selectIndices(4, 3, 2);
    }

    @Test public void test_rt35624_selectedItems_downwards() {
        model.clearSelection();
        model.select(2);

        msModel().getSelectedItems().addListener(((ListChangeListener.Change change) -> {
            while (change.next()) {
                // we expect two items in the added list: the items in index
                // 3 and 4, as index 2 has previously been selected
                assertEquals(2, change.getAddedSize());

                if (isTree()) {
                    assertEquals(FXCollections.observableArrayList(
                            root.getChildren().get(2),
                            root.getChildren().get(3)
                    ), change.getAddedSubList());
                } else {
                    assertEquals(FXCollections.observableArrayList(
                            data.get(3),
                            data.get(4)
                    ), change.getAddedSubList());
                }
            }

            // In the actual list, we expect three items: the values at index
            // 2, 3 and 4
            assertEquals(3, change.getList().size());

            if (isTree()) {
                assertEquals(FXCollections.observableArrayList(
                        root.getChildren().get(1),
                        root.getChildren().get(2),
                        root.getChildren().get(3)
                ), change.getList());
            } else {
                assertEquals(FXCollections.observableArrayList(
                        data.get(2),
                        data.get(3),
                        data.get(4)
                ), change.getList());
            }
        }));

        model.selectIndices(2, 3, 4);
    }

    @Test public void test_rt35624_selectedItems_upwards() {
        model.clearSelection();
        model.select(4);

        msModel().getSelectedItems().addListener(((ListChangeListener.Change change) -> {
            while (change.next()) {
                // we expect two items in the added list: the items in index
                // 2 and 3, as index 4 has previously been selected
                assertEquals(2, change.getAddedSize());

                if (isTree()) {
                    assertEquals(FXCollections.observableArrayList(
                            root.getChildren().get(1),
                            root.getChildren().get(2)
                    ), change.getAddedSubList());
                } else {
                    assertEquals(FXCollections.observableArrayList(
                            data.get(2),
                            data.get(3)
                    ), change.getAddedSubList());
                }
            }

            // In the actual list, we expect three items: the values at index
            // 2, 3 and 4
            assertEquals(3, change.getList().size());

            if (isTree()) {
                assertEquals(FXCollections.observableArrayList(
                        root.getChildren().get(1),
                        root.getChildren().get(2),
                        root.getChildren().get(3)
                ), change.getList());
            } else {
                assertEquals(FXCollections.observableArrayList(
                        data.get(2),
                        data.get(3),
                        data.get(4)
                ), change.getList());
            }
        }));

        model.selectIndices(4, 3, 2);
    }

    @Test public void test_rt39548_positiveValue_outOfRange() {
        // for this test we want there to be no data in the controls
        clearModelData();

        model.clearAndSelect(10);
    }

    @Test public void test_rt39548_negativeValue() {
        // for this test we want there to be no data in the controls
        clearModelData();

        model.clearAndSelect(-1);
    }

    @Test public void test_rt38884_invalidChange() {
        model.select(3);
        int removedSize = model.getSelectedItems().size();
        ListChangeListener l = (ListChangeListener.Change c) -> {
            c.next();
            assertEquals(removedSize, c.getRemovedSize());
        };
        model.getSelectedItems().addListener(l);
        clearModelData();
    }

    private void clearModelData() {
        listView.getItems().clear();
        tableView.getItems().clear();
        treeView.setRoot(null);
        treeTableView.setRoot(null);
    }

    @Test public void test_rt40804() {
        StageLoader sl = new StageLoader(currentControl);
        model.setSelectionMode(SelectionMode.MULTIPLE);
        model.select(0);
        model.select(1);
        model.clearSelection();
        ControlTestUtils.runWithExceptionHandler(() -> {
            model.select(3); // this is where the test failed
        });

        sl.dispose();
    }

    @Test public void test_jdk_8088752() {
        // FIXME for now this test does not cover TreeView / TreeTableView
        if (isTree()) {
            return;
        }

        Object uncontained = isTree() ? new TreeItem<>("uncontained") : "uncontained";

        model.selectRange(3, 5);
        model.select(uncontained);

        assertEquals("sanity: having uncontained selectedItem", uncontained, model.getSelectedItem());
        assertEquals("sanity: selected index removed ", -1, model.getSelectedIndex());

        // insert uncontained to items
        int insertIndex = 3;
        addItem(insertIndex, uncontained);
        assertEquals("selectedItem unchanged", uncontained, model.getSelectedItem());
        assertEquals("selectedIndex updated", insertIndex, model.getSelectedIndex());
    }

    private void addItem(int index, Object item) {
        if (currentControl instanceof ListView) {
            ((ListView) currentControl).getItems().add(index, item);
        } else if (currentControl instanceof TableView) {
            ((TableView) currentControl).getItems().add(index, item);
        } else if (currentControl instanceof TreeView || currentControl instanceof TreeTableView) {
            root.getChildren().add(index, (TreeItem)item);
        } else {
            throw new RuntimeException("Unsupported control type");
        }
    }

    @Test
    public void test_jdk_8088467_selectedIndicesReselect() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        IntegerProperty counter = new SimpleIntegerProperty();

        model.selectAll();
        Object selected = model.getSelectedItem();
        int selectedIndex = model.getSelectedIndex();
        List<String> selectedItems = new ArrayList(model.getSelectedItems());
        List<Integer> selectedIndices = new ArrayList(model.getSelectedIndices());

        model.getSelectedIndices().addListener((ListChangeListener<Integer>) c -> counter.set(counter.get() +1));

        // add selectedIndex - changes nothing as it is already selected
        model.select(selectedIndex);
        assertSame("sanity: state unchanged", selected, model.getSelectedItems().get(0));
        assertEquals("sanity: state unchanged", 0, selectedIndex);
        assertEquals("sanity: state unchanged", selectedItems, model.getSelectedItems());
        assertEquals("sanity: state unchanged", selectedIndices, model.getSelectedIndices());
        assertEquals("must not fire if nothing changed", 0, counter.get());
    }

    @Test
    public void test_jdk_8088467_selectedItemsReselect() {
        model.setSelectionMode(SelectionMode.MULTIPLE);
        IntegerProperty counter = new SimpleIntegerProperty();

        model.selectAll();
        Object selected = model.getSelectedItem();
        int selectedIndex = model.getSelectedIndex();
        List<String> selectedItems = new ArrayList(model.getSelectedItems());
        List<Integer> selectedIndices = new ArrayList(model.getSelectedIndices());

        model.getSelectedItems().addListener((ListChangeListener<String>) c -> counter.set(counter.get() +1));

        // add selectedIndex - changes nothing as it is already selected
        model.select(selectedIndex);
        assertSame("sanity: state unchanged", selected, model.getSelectedItems().get(0));
        assertEquals("sanity: state unchanged", 0, selectedIndex);
        assertEquals("sanity: state unchanged", selectedItems, model.getSelectedItems());
        assertEquals("sanity: state unchanged", selectedIndices, model.getSelectedIndices());
        assertEquals("must not fire if nothing changed", 0, counter.get());
    }

    @Test
    public void test_jdk_8088896() {
        model.setSelectionMode(SelectionMode.MULTIPLE);

        model.selectRange(2, 4);
        assertEquals(2, model.getSelectedIndices().size());
        assertEquals(2, model.getSelectedItems().size());

        AtomicInteger counter = new AtomicInteger();
        model.getSelectedIndices().addListener((ListChangeListener)c -> {
            counter.incrementAndGet();
            while (c.next()) {
                if (c.wasAdded()) {
                    assertTrue(c.getAddedSubList().contains(4));
                }
                if (c.wasRemoved()) {
                    assertTrue(c.getRemoved().contains(2));
                }
            }
        });

        assertEquals(0, counter.get());
        if (isTree()) {
            addItem(0, new TreeItem<>("new item"));
        } else {
            addItem(0, "new item");
        }
        assertEquals(2, model.getSelectedIndices().size());
        assertEquals(2, model.getSelectedItems().size());
        assertEquals(1, counter.get());
    }
}

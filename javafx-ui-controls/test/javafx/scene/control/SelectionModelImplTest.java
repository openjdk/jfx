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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ChoiceBox.ChoiceBoxSelectionModel;
import javafx.scene.control.ListView.ListViewFocusModel;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TreeView.TreeViewFocusModel;
import javafx.scene.control.ComboBox.ComboBoxSelectionModel;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit tests for the SelectionModel abstract class used by ListView
 * and TreeView. This unit test attempts to test all known implementations, and
 * as such contains some conditional logic to handle the various controls as
 * simply as possible.
 *
 * @author Jonathan Giles
 */
@RunWith(Parameterized.class)
public class SelectionModelImplTest {

    private SelectionModel model;
    private FocusModel focusModel;

    private Class<? extends SelectionModel> modelClass;

    // ListView
    private static final ListView<String> listView;

    // ListView model data
    private static ObservableList<String> defaultData = FXCollections.<String>observableArrayList();
    private static ObservableList<String> data = FXCollections.<String>observableArrayList();
    private static final String ROW_1_VALUE = "Row 1";
    private static final String ROW_2_VALUE = "Row 2";
    private static final String ROW_5_VALUE = "Row 5";
    private static final String ROW_20_VALUE = "Row 20";

    // TreeView
    private static final TreeView treeView;
    private static final TreeItem<String> root;
    private static final TreeItem<String> ROW_2_TREE_VALUE;
    private static final TreeItem<String> ROW_5_TREE_VALUE;

    // TableView
    private static final TableView tableView;

    // ChoiceBox
    private static final ChoiceBox choiceBox;
    
    // ComboBox
    private static final ComboBox comboBox;

    static {
        // ListView init
        defaultData.addAll(ROW_1_VALUE, ROW_2_VALUE, "Long Row 3", "Row 4", ROW_5_VALUE, "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", ROW_20_VALUE);

        data.setAll(defaultData);
        listView = new ListView<String>(data);
        // --- ListView init

        // TreeView init
        root = new TreeItem<String>(ROW_1_VALUE);
        root.setExpanded(true);
        for (int i = 1; i < data.size(); i++) {
            root.getChildren().add(new TreeItem<String>(data.get(i)));
        }
        ROW_2_TREE_VALUE = root.getChildren().get(0);
        ROW_5_TREE_VALUE = root.getChildren().get(3);

        treeView = new TreeView(root);
        // --- TreeView init

        // TableView init
        tableView = new TableView();
        tableView.setItems(data);
//        tableView.getColumns().add(new TableColumn());
        // --- TableView init
        
        // ChoiceBox init
        choiceBox = new ChoiceBox();
        choiceBox.setItems(data);
        // --- ChoiceBox init
        
        // ComboBox init
        comboBox = new ComboBox();
        comboBox.setItems(data);
        // --- ComboBox init
    }
    // --- ListView model data

    @Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { ListView.ListViewBitSetSelectionModel.class },
            { TreeView.TreeViewBitSetSelectionModel.class },
            { TableView.TableViewArrayListSelectionModel.class },
            { ChoiceBox.ChoiceBoxSelectionModel.class },
            { ComboBox.ComboBoxSelectionModel.class }
        });
    }

    public SelectionModelImplTest(Class<? extends SelectionModel> modelClass) {
        this.modelClass = modelClass;
    }

    @AfterClass public static void tearDownClass() throws Exception {    }

    @Before public void setUp() {
        try {
            // reset the data model
            data.setAll(defaultData);

            // we create a new SelectionModel per test to ensure it is always back
            // at the default settings
            if (modelClass.equals(ListView.ListViewBitSetSelectionModel.class)) {
                // recreate the selection model
                model = modelClass.getConstructor(ListView.class).newInstance(listView);
                listView.setSelectionModel((MultipleSelectionModel<String>)model);

                // create a new focus model
                focusModel = new ListViewFocusModel(listView);
                listView.setFocusModel(focusModel);
            } else if (modelClass.equals(TreeView.TreeViewBitSetSelectionModel.class)) {
                model = modelClass.getConstructor(TreeView.class).newInstance(treeView);
                treeView.setSelectionModel((MultipleSelectionModel<String>)model);
                focusModel = treeView.getFocusModel();

                // create a new focus model
                focusModel = new TreeViewFocusModel(treeView);
                treeView.setFocusModel(focusModel);
            } else if (TableViewSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = modelClass.getConstructor(TableView.class).newInstance(tableView);
                tableView.setSelectionModel((TableViewSelectionModel) model);

                // create a new focus model
                focusModel = new TableViewFocusModel(tableView);
                tableView.setFocusModel((TableViewFocusModel) focusModel);
            } else if (ChoiceBoxSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = modelClass.getConstructor(ChoiceBox.class).newInstance(choiceBox);
                choiceBox.setSelectionModel((ChoiceBoxSelectionModel) model);

                // create a new focus model
                focusModel = null;
            } else if (ComboBoxSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = modelClass.getConstructor(ComboBox.class).newInstance(comboBox);
                comboBox.setSelectionModel((ComboBoxSelectionModel) model);

                // create a new focus model
                focusModel = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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

    @Test public void ensureInEmptyState() {
        assertEquals(-1, model.getSelectedIndex());
        assertNull(model.getSelectedItem());

        if (focusModel != null) {
            assertEquals(-1, focusModel.getFocusedIndex());
            assertNull(focusModel.getFocusedItem());
        }
    }

    @Test public void selectInvalidIndex() {
        // there isn't 100 rows, so selecting this shouldn't be possible
        model.select(100);
        ensureInEmptyState();
    }

    @Test public void selectRowAfterInvalidIndex() {
        // there isn't 100 rows, so selecting this shouldn't be possible.
        // The end result should be that we remain at the -1 index
        model.select(100);

        // go to the 0th index
        model.selectNext();
        assertEquals(0, model.getSelectedIndex());
        if (focusModel != null) assertEquals(0, focusModel.getFocusedIndex());

        // go to the 1st index
        model.selectNext();
        assertEquals(1, model.getSelectedIndex());
        if (focusModel != null) assertEquals(1, focusModel.getFocusedIndex());
    }

    @Test public void selectInvalidItem() {
        Object obj = new TreeItem("DUMMY");
        model.select(obj);
        assertEquals(-1, model.getSelectedIndex());
        assertSame(obj, model.getSelectedItem());
    }

    @Test public void selectValidIndex() {
        int index = 4;
        model.select(index);

        assertEquals(index, model.getSelectedIndex());
        assertNotNull(model.getSelectedItem());
        assertEquals(ROW_5_VALUE, getValue(model.getSelectedItem()));

        if (focusModel != null) {
            assertEquals(index, focusModel.getFocusedIndex());
            assertNotNull(focusModel.getFocusedItem());
            assertEquals(ROW_5_VALUE, getValue(focusModel.getFocusedItem()));
        }
    }

    @Test public void clearPartialSelectionWithSingleSelection() {
        assertFalse(model.isSelected(5));
        model.select(5);
        assertTrue(model.isSelected(5));
        model.clearSelection(5);
        assertFalse(model.isSelected(5));
    }

    @Test public void ensureIsEmptyIsAccurate() {
        assertTrue(model.isEmpty());
        model.select(5);
        assertFalse(model.isEmpty());
        model.clearSelection();
        assertTrue(model.isEmpty());
    }

    @Test public void testSingleSelectionMode() {
        assertTrue(model.isEmpty());

        model.select(5);
        assertTrue("Selected: " + model.getSelectedIndex() + ", expected: 5",  model.isSelected(5));

        model.select(10);
        assertTrue(model.isSelected(10));
        assertFalse(model.isSelected(5));
    }

    @Test public void testSelectNullObject() {
        model.select(null);
    }

    @Test public void testFocusOnNegativeIndex() {
        if (focusModel == null) return;
        assertEquals(-1, focusModel.getFocusedIndex());
        focusModel.focus(-1);
        assertEquals(-1, focusModel.getFocusedIndex());
        assertFalse(focusModel.isFocused(-1));
    }

    @Test public void testFocusOnOutOfBoundsIndex() {
        if (focusModel == null) return;
        assertEquals(-1, focusModel.getFocusedIndex());
        focusModel.focus(Integer.MAX_VALUE);
        assertEquals(-1, focusModel.getFocusedIndex());
        assertNull(focusModel.getFocusedItem());
        assertFalse(focusModel.isFocused(Integer.MAX_VALUE));
    }

    @Test public void testFocusOnValidIndex() {
        if (focusModel == null) return;
        assertEquals(-1, focusModel.getFocusedIndex());
        focusModel.focus(1);
        assertEquals(1, focusModel.getFocusedIndex());
        assertTrue(focusModel.isFocused(1));
        
        if (model instanceof TreeView.TreeViewBitSetSelectionModel) {
            assertEquals(root.getChildren().get(0), focusModel.getFocusedItem());
        } else {
            assertEquals(data.get(1), focusModel.getFocusedItem());
        }
    }

    @Ignore("Not yet implemented in TreeView")
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
}

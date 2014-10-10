/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;

import com.sun.javafx.tk.Toolkit;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

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
    private Control currentControl;

    // ListView
    private ListView<String> listView;

    // ListView model data
//    private static ObservableList<String> defaultData = FXCollections.<String>observableArrayList();
    private static ObservableList<String> data = FXCollections.<String>observableArrayList();
    private static final String ROW_1_VALUE = "Row 1";
    private static final String ROW_2_VALUE = "Row 2";
    private static final String ROW_3_VALUE = "Row 3";
    private static final String ROW_5_VALUE = "Row 5";
    private static final String ROW_20_VALUE = "Row 20";

    // TreeView
    private TreeView treeView;
    private TreeItem<String> root;
    private TreeItem<String> ROW_2_TREE_VALUE;
    private TreeItem<String> ROW_3_TREE_VALUE;
    private TreeItem<String> ROW_5_TREE_VALUE;

    // TableView
    private TableView tableView;

    // TreeTableView
    private TreeTableView treeTableView;

    // ChoiceBox
    private ChoiceBox choiceBox;
    
    // ComboBox
    private ComboBox comboBox;

    // --- ListView model data

    @Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { ListView.ListViewBitSetSelectionModel.class },
            { TreeView.TreeViewBitSetSelectionModel.class },
            { TableView.TableViewArrayListSelectionModel.class },
            { TreeTableView.TreeTableViewArrayListSelectionModel.class }
//            { ChoiceBox.ChoiceBoxSelectionModel.class } TODO re-enable
//            { ComboBox.ComboBoxSelectionModel.class }  TODO re-enable
        });
    }

    public SelectionModelImplTest(Class<? extends SelectionModel> modelClass) {
        this.modelClass = modelClass;
    }

    @AfterClass public static void tearDownClass() throws Exception {    }

    @Before public void setUp() {
        // reset the data model
        data.setAll(ROW_1_VALUE, ROW_2_VALUE, ROW_3_VALUE, "Row 4", ROW_5_VALUE, "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", ROW_20_VALUE);

        // ListView init
        listView = new ListView<>(data);
        // --- ListView init

        // TreeView init
        root = new TreeItem<>(ROW_1_VALUE);
        root.setExpanded(true);
        for (int i = 1; i < data.size(); i++) {
            root.getChildren().add(new TreeItem<>(data.get(i)));
        }
        ROW_2_TREE_VALUE = root.getChildren().get(0);
        ROW_3_TREE_VALUE = root.getChildren().get(1);
        ROW_5_TREE_VALUE = root.getChildren().get(3);

        treeView = new TreeView(root);
        // --- TreeView init

        // TableView init
        tableView = new TableView();
        tableView.setItems(data);
//        tableView.getColumns().add(new TableColumn());
        // --- TableView init

        // TreeTableView init
        treeTableView = new TreeTableView(root);
        // --- TreeTableView init

        // ChoiceBox init
        choiceBox = new ChoiceBox();
        choiceBox.setItems(data);
        // --- ChoiceBox init

        // ComboBox init
        comboBox = new ComboBox();
        comboBox.setItems(data);
        // --- ComboBox init



        try {
            // we create a new SelectionModel per test to ensure it is always back
            // at the default settings
            if (modelClass.equals(ListView.ListViewBitSetSelectionModel.class)) {
                // recreate the selection model
                model = modelClass.getConstructor(ListView.class).newInstance(listView);
                listView.setSelectionModel((MultipleSelectionModel<String>)model);

                // create a new focus model
                focusModel = new ListViewFocusModel(listView);
                listView.setFocusModel(focusModel);
                currentControl = listView;
            } else if (modelClass.equals(TreeView.TreeViewBitSetSelectionModel.class)) {
                model = modelClass.getConstructor(TreeView.class).newInstance(treeView);
                treeView.setSelectionModel((MultipleSelectionModel<String>)model);
                focusModel = treeView.getFocusModel();

                // create a new focus model
                focusModel = new TreeViewFocusModel(treeView);
                treeView.setFocusModel(focusModel);
                currentControl = treeView;
            } else if (TableViewSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = modelClass.getConstructor(TableView.class).newInstance(tableView);
                tableView.setSelectionModel((TableViewSelectionModel) model);

                // create a new focus model
                focusModel = new TableViewFocusModel(tableView);
                tableView.setFocusModel((TableViewFocusModel) focusModel);
                currentControl = tableView;
            } else if (TreeTableView.TreeTableViewSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = modelClass.getConstructor(TreeTableView.class).newInstance(treeTableView);
                treeTableView.setSelectionModel((TreeTableView.TreeTableViewSelectionModel) model);

                // create a new focus model
                focusModel = new TreeTableView.TreeTableViewFocusModel(treeTableView);
                treeTableView.setFocusModel((TreeTableView.TreeTableViewFocusModel) focusModel);
                currentControl = treeTableView;
            } else if (ChoiceBoxSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = modelClass.getConstructor(ChoiceBox.class).newInstance(choiceBox);
                choiceBox.setSelectionModel((ChoiceBoxSelectionModel) model);

                // create a new focus model
                focusModel = null;
                currentControl = choiceBox;
            } else if (ComboBoxSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = modelClass.getConstructor(ComboBox.class).newInstance(comboBox);
                comboBox.setSelectionModel((ComboBoxSelectionModel) model);

                // create a new focus model
                focusModel = null;
                currentControl = comboBox;
            }

            // ensure the selection mode is set to single (if the selection model
            // is actually a MultipleSelectionModel subclass)
            if (model instanceof MultipleSelectionModel) {
                ((MultipleSelectionModel)model).setSelectionMode(SelectionMode.SINGLE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @After public void tearDown() {
        model = null;
    }

    private boolean isTree() {
        return (model instanceof TreeView.TreeViewBitSetSelectionModel) ||
               (model instanceof TreeTableView.TreeTableViewArrayListSelectionModel);
    }

    private Object getValue(Object item) {
        if (item instanceof TreeItem) {
            return ((TreeItem)item).getValue();
        }
        return item;
    }

    @Test public void testDefaultState() {
        assertEquals(-1, model.getSelectedIndex());
        assertNull(getValue(model.getSelectedItem()));

        if (focusModel != null) {
            assertEquals(0, focusModel.getFocusedIndex());
            assertEquals(ROW_1_VALUE, getValue(focusModel.getFocusedItem()));
        }
    }

    @Test public void selectInvalidIndex() {
        // there isn't 100 rows, so selecting this shouldn't be possible
        model.select(100);

        // we should be in a default state
        testDefaultState();
    }

    @Test public void selectRowAfterInvalidIndex() {
        // there isn't 100 rows, so selecting this shouldn't be possible.
        // The end result should be that we remain at the 0 index
        model.select(100);

        // go to the 1st index
        model.selectNext();
        assertEquals(1, model.getSelectedIndex());
        if (focusModel != null) assertEquals(1, focusModel.getFocusedIndex());

        // go to the 2nd index
        model.selectNext();
        assertEquals(2, model.getSelectedIndex());
        if (focusModel != null) assertEquals(2, focusModel.getFocusedIndex());
    }

    @Test public void selectInvalidItem() {
        assertEquals(-1, model.getSelectedIndex());

        Object obj = new TreeItem("DUMMY");
        model.select(obj);

        assertSame(obj, model.getSelectedItem());
        assertEquals(-1, model.getSelectedIndex());
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
        model.clearSelection();
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
        assertEquals(0, focusModel.getFocusedIndex());
        focusModel.focus(-1);
        assertEquals(-1, focusModel.getFocusedIndex());
        assertFalse(focusModel.isFocused(-1));
    }

    @Test public void testFocusOnOutOfBoundsIndex() {
        if (focusModel == null) return;
        assertEquals(0, focusModel.getFocusedIndex());
        focusModel.focus(Integer.MAX_VALUE);
        assertEquals(-1, focusModel.getFocusedIndex());
        assertNull(focusModel.getFocusedItem());
        assertFalse(focusModel.isFocused(Integer.MAX_VALUE));
    }

    @Test public void testFocusOnValidIndex() {
        if (focusModel == null) return;
        assertEquals(0, focusModel.getFocusedIndex());
        focusModel.focus(1);
        assertEquals(1, focusModel.getFocusedIndex());
        assertTrue(focusModel.isFocused(1));
        
        if (isTree()) {
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
    
    @Test public void test_rt_29821() {
        // in single selection passing in select(null) should clear selection. 
        // In multiple selection (tested elsewhere), this would result in a no-op
        
        if (currentControl instanceof ChoiceBox) {
            model.clearSelection();
            
            model.select(3);
            assertNotNull(choiceBox.getValue());
    
            model.select(null);
            assertFalse(model.isSelected(3));
            assertNull(choiceBox.getValue());
        } else {
            IndexedCell cell_3 = VirtualFlowTestUtils.getCell(currentControl, 3);
            assertNotNull(cell_3);
            assertFalse(cell_3.isSelected());
    
            model.clearSelection();
            model.select(3);
            assertTrue(cell_3.isSelected());
    
            model.select(null);
            assertFalse(model.isSelected(3));
            
            if (currentControl instanceof ComboBox) {
                ((ComboBox)currentControl).layoutChildren();
            }
            
            assertFalse(cell_3.isSelected());
        }
    }

    @Test public void test_rt_30356_selectRowAtIndex0() {
        setUp();

        // this test selects the 0th row, then removes it, and sees what happens
        // to the selection.

        if (isTree()) {
            // we hide the root, so we have a bunch of children at the same level
            if (currentControl instanceof TreeView) {
                ((TreeView)currentControl).setShowRoot(false);
            } else if (currentControl instanceof TreeTableView) {
                ((TreeTableView)currentControl).setShowRoot(false);
            }

            model.select(0);

            // for tree / tree table
            assertEquals(ROW_2_TREE_VALUE, model.getSelectedItem());

            root.getChildren().remove(0);
            assertEquals(ROW_3_TREE_VALUE, model.getSelectedItem());
        } else if (currentControl instanceof ChoiceBox || currentControl instanceof ComboBox) {
            // TODO
        } else {
            // for list / table
            model.select(0);
            assertEquals("model is " + model, ROW_1_VALUE, model.getSelectedItem());

            data.remove(0);
            assertEquals(ROW_2_VALUE, model.getSelectedItem());
        }

        // we also check that the cell itself is selected (as often the selection
        // model and the visuals disagree in this case).
        // TODO remove the ComboBox conditional and test for that too
        if (! (currentControl instanceof ChoiceBox || currentControl instanceof ComboBox)) {
            IndexedCell cell = VirtualFlowTestUtils.getCell(currentControl, 0);
            assertTrue(cell.isSelected());
        }
    }

    @Test public void test_rt_30356_selectRowAtIndex1() {
        setUp();

        // this test selects the 1st row, then removes it, and sees what happens
        // to the selection.

        if (isTree()) {
            // we hide the root, so we have a bunch of children at the same level
            if (currentControl instanceof TreeView) {
                ((TreeView)currentControl).setShowRoot(false);
            } else if (currentControl instanceof TreeTableView) {
                ((TreeTableView)currentControl).setShowRoot(false);
            }

            // select row 1, which is 'Row 3' because the root isn't showing
            model.select(1);

            assertEquals(ROW_3_TREE_VALUE, model.getSelectedItem());
            assertTrue(root.isExpanded());
            assertEquals(19, root.getChildren().size());

            // remove row 1 (i.e. 'Row 3')
            TreeItem<String> removed = root.getChildren().remove(1);
            assertEquals("Row 3", getValue(removed));

            // check where the selection moves to...
            assertEquals(ROW_2_TREE_VALUE, model.getSelectedItem());
        } else if (currentControl instanceof ChoiceBox || currentControl instanceof ComboBox) {
            // TODO
        } else {
            // for list / table
            model.select(1);
            assertEquals(ROW_2_VALUE, model.getSelectedItem());
            assertEquals(1, model.getSelectedIndex());
            data.remove(1);
            assertEquals(ROW_1_VALUE, model.getSelectedItem());
            assertEquals(0, model.getSelectedIndex());
        }

        // we also check that the cell itself is selected (as often the selection
        // model and the visuals disagree in this case).
        // TODO remove the ComboBox conditional and test for that too
        if (! (currentControl instanceof ChoiceBox || currentControl instanceof ComboBox)) {
            // selection moves up from 1 to 0 in the current impl
            int index = model.getSelectedIndex();
            IndexedCell cell = VirtualFlowTestUtils.getCell(currentControl, index);
            assertTrue("Cell in index " + index + " should be selected, but isn't", cell.isSelected());
        }
    }

    private int rt32618_count = 0;
    @Test public void test_rt32618_singleSelection() {
        model.selectedItemProperty().addListener((ov, t, t1) -> {
            rt32618_count++;
        });

        assertEquals(0, rt32618_count);

        model.select(1);
        assertEquals(1, rt32618_count);
        assertEquals(ROW_2_VALUE, getValue(model.getSelectedItem()));

        model.clearAndSelect(2);
        assertEquals(2, rt32618_count);
        assertEquals(ROW_3_VALUE, getValue(model.getSelectedItem()));
    }
}

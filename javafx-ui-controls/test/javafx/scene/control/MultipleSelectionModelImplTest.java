/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView.ListViewFocusModel;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TreeView.TreeViewFocusModel;
import javafx.scene.control.TreeTableView.TreeTableViewFocusModel;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

/**
 * Unit tests for the SelectionModel abstract class used by ListView
 * and TreeView. This unit test attempts to test all known implementations, and
 * as such contains some conditional logic to handle the various controls as
 * simply as possible.
 *
 * @author Jonathan Giles
 */
@RunWith(Parameterized.class)
public class MultipleSelectionModelImplTest {

    private MultipleSelectionModel model;
    private FocusModel focusModel;

    private Class<? extends MultipleSelectionModel> modelClass;

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
    
    // TreeTableView
    private static final TreeTableView treeTableView;

    // TableView
    private static final TableView tableView;

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

        // TreeTableView init
//        root = new TreeItem<String>(ROW_1_VALUE);
//        root.setExpanded(true);
//        for (int i = 1; i < data.size(); i++) {
//            root.getChildren().add(new TreeItem<String>(data.get(i)));
//        }
//        ROW_2_TREE_VALUE = root.getChildren().get(0);
//        ROW_5_TREE_VALUE = root.getChildren().get(3);

        treeTableView = new TreeTableView(root);
        // --- TreeView init
        
        // TableView init
        tableView = new TableView();
        tableView.setItems(data);
//        tableView.getColumns().add(new TableColumn());
        // --- TableView init
    }
    // --- ListView model data

    @Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { ListView.ListViewBitSetSelectionModel.class },
            { TreeView.TreeViewBitSetSelectionModel.class },
            { TableView.TableViewArrayListSelectionModel.class },
            { TreeTableView.TreeTableViewArrayListSelectionModel.class },
        });
    }

    public MultipleSelectionModelImplTest(Class<? extends MultipleSelectionModel> modelClass) {
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
            } else if (TreeTableViewSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = modelClass.getConstructor(TreeTableView.class).newInstance(treeTableView);
                treeTableView.setSelectionModel((TreeTableViewSelectionModel) model);

                // create a new focus model
                focusModel = new TreeTableViewFocusModel(treeTableView);
                treeTableView.setFocusModel((TreeTableViewFocusModel) focusModel);
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
        return model instanceof TreeView.TreeViewBitSetSelectionModel ||
               model instanceof TreeTableView.TreeTableViewArrayListSelectionModel;
    }

    @Test public void ensureInEmptyState() {
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

        assertNotNull(msModel().getSelectedIndices());
        assertNotNull(msModel().getSelectedItems());
        assertEquals(1, msModel().getSelectedIndices().size());
        assertEquals(index, msModel().getSelectedIndices().get(0));
        assertEquals(1, msModel().getSelectedItems().size());
        assertEquals(ROW_5_VALUE, getValue(msModel().getSelectedItems().get(0)));
    }

    @Test public void testSelectAllWithSingleSelection() {
        msModel().selectAll();
        ensureInEmptyState();
    }

    @Test public void testSelectAllWithMultipleSelection() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
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

    @Test public void testSelectedIndicesObservableListIsEmpty() {
        assertTrue(msModel().getSelectedIndices().isEmpty());
    }

    @Test public void testSelectedIndicesIteratorIsNotNull() {
        assertNotNull(msModel().getSelectedIndices().iterator());
    }

    @Test public void testSelectedIndicesIteratorDoesNotHaveNext() {
        assertFalse(msModel().getSelectedIndices().iterator().hasNext());
    }

    @Test public void testSelectedIndicesIteratorWorksWithSingleSelection() {
        model.select(5);

        Iterator<Integer> it = msModel().getSelectedIndices().iterator();
        assertEquals(5, (int) it.next());
        assertFalse(it.hasNext());
    }

    @Test public void testSelectedIndicesIteratorWorksWithMultipleSelection() {
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
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectIndices(-20, null);
        assertEquals(-1, model.getSelectedIndex());
        assertNull(model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test public void testMultipleSelectionWithInvalidIndices() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectIndices(-20, 23505, 78125);
        assertEquals(-1, model.getSelectedIndex());
        assertNull(model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test public void testInvalidSelection() {
        msModel().setSelectionMode(SelectionMode.SINGLE);
        msModel().selectIndices(-20, null);
        assertEquals(-1, model.getSelectedIndex());
        assertNull(model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test public void ensureSwappedSelectRangeWorks() {
        // first test a valid range - there should be 6 selected items
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
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectRange(200, 220);
        assertEquals(-1, model.getSelectedIndex());
        assertEquals(null, model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test public void testEmptySelectRange() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectRange(10, 10);
        assertEquals(-1, model.getSelectedIndex());
        assertEquals(null, model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test public void testNegativeSelectRange() {
        msModel().setSelectionMode(SelectionMode.MULTIPLE);
        msModel().selectRange(-10, -1);
        assertEquals(-1, model.getSelectedIndex());
        assertEquals(null, model.getSelectedItem());
        assertEquals(indices(msModel()), 0, msModel().getSelectedIndices().size());
        assertEquals(items(msModel()), 0, msModel().getSelectedItems().size());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullListViewInSelectionModel() {
        new ListView.ListViewBitSetSelectionModel(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullTreeViewInSelectionModel() {
        new TreeView.TreeViewBitSetSelectionModel(null);
    }

    @Test public void selectAllInEmptySingleSelectionMode() {
        msModel().setSelectionMode(SelectionMode.SINGLE);
        assertTrue(model.isEmpty());
        msModel().selectAll();
        assertTrue(model.isEmpty());
    }

    @Test public void selectAllInSingleSelectionModeWithSelectedRow() {
        msModel().setSelectionMode(SelectionMode.SINGLE);
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
}

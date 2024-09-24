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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Collection;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceBoxShim;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxShim;
import javafx.scene.control.Control;
import javafx.scene.control.ControlShim;
import javafx.scene.control.FocusModel;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ListViewShim;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SelectionModelShim;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TableViewShim;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableViewShim;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeViewShim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

/**
 * Unit tests for the SelectionModel abstract class used by ListView
 * and TreeView. This unit test attempts to test all known implementations, and
 * as such contains some conditional logic to handle the various controls as
 * simply as possible.
 *
 * @author Jonathan Giles
 */
public class SelectionModelImplTest {

    private SelectionModel model;
    private FocusModel focusModel;
    private Control currentControl;

    // ListView
    private ListView<String> listView;

    // ListView model data
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

    private static Collection<Class<? extends SelectionModel>> parameters() {
        return List.of(
            ListViewShim.get_ListViewBitSetSelectionModel_class(),
            TreeViewShim.get_TreeViewBitSetSelectionModel_class(),
            TableViewShim.get_TableViewArrayListSelectionModel_class(),
            TreeTableViewShim.get_TreeTableViewArrayListSelectionModel_class()
//          ChoiceBox.ChoiceBoxSelectionModel.class, TODO re-enable
//          ComboBox.ComboBoxSelectionModel.class  TODO re-enable
        );
    }

    @AfterAll
    public static void tearDownClass() throws Exception {    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setUp(Class<? extends SelectionModel> modelClass) {
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
            if (modelClass.equals(ListViewShim.get_ListViewBitSetSelectionModel_class())) {
                // recreate the selection model
                model = SelectionModelShim.newInstance_from_class(modelClass, ListView.class, listView);
                listView.setSelectionModel((MultipleSelectionModel<String>)model);

                // create a new focus model
                focusModel = ListViewShim.getListViewFocusModel(listView);
                listView.setFocusModel(focusModel);
                currentControl = listView;
            } else if (modelClass.equals(TreeViewShim.get_TreeViewBitSetSelectionModel_class())) {
                model = SelectionModelShim.newInstance_from_class(modelClass, TreeView.class, treeView);
                treeView.setSelectionModel((MultipleSelectionModel<String>)model);
                focusModel = treeView.getFocusModel();

                // create a new focus model
                focusModel = TreeViewShim.get_TreeViewFocusModel(treeView);
                treeView.setFocusModel(focusModel);
                currentControl = treeView;
            } else if (TableViewSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = SelectionModelShim.newInstance_from_class(modelClass, TableView.class, tableView);
                tableView.setSelectionModel((TableViewSelectionModel) model);

                // create a new focus model
                focusModel = new TableViewFocusModel(tableView);
                tableView.setFocusModel((TableViewFocusModel) focusModel);
                currentControl = tableView;
            } else if (TreeTableView.TreeTableViewSelectionModel.class.isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = SelectionModelShim.newInstance_from_class(modelClass, TreeTableView.class, treeTableView);
                treeTableView.setSelectionModel((TreeTableView.TreeTableViewSelectionModel) model);

                // create a new focus model
                focusModel = new TreeTableView.TreeTableViewFocusModel(treeTableView);
                treeTableView.setFocusModel((TreeTableView.TreeTableViewFocusModel) focusModel);
                currentControl = treeTableView;
            } else if (ChoiceBoxShim.ChoiceBoxSelectionModel_isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = SelectionModelShim.newInstance_from_class(modelClass, ChoiceBox.class, choiceBox);
                choiceBox.setSelectionModel((SingleSelectionModel) model);

                // create a new focus model
                focusModel = null;
                currentControl = choiceBox;
            } else if (ComboBoxShim.ComboBoxSelectionModel_isAssignableFrom(modelClass)) {
                // recreate the selection model
                model = SelectionModelShim.newInstance_from_class(modelClass, ComboBox.class, comboBox);
                comboBox.setSelectionModel((SingleSelectionModel) model);

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
            fail(ex);
        }
    }

    @AfterEach
    public void tearDown() {
        model = null;
    }

    private boolean isTree() {
        return (TreeViewShim.is_TreeViewBitSetSelectionModel(model)) ||
               (TreeTableViewShim.instanceof_TreeTableViewArrayListSelectionModel(model));
    }

    private Object getValue(Object item) {
        if (item instanceof TreeItem) {
            return ((TreeItem)item).getValue();
        }
        return item;
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testDefaultState(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
        testDefaultState2();
    }

    private void testDefaultState2() {
        assertEquals(-1, model.getSelectedIndex());
        assertNull(getValue(model.getSelectedItem()));

        if (focusModel != null) {
            assertEquals(0, focusModel.getFocusedIndex());
            assertEquals(ROW_1_VALUE, getValue(focusModel.getFocusedItem()));
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void selectInvalidIndex(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
        // there isn't 100 rows, so selecting this shouldn't be possible
        model.select(100);

        // we should be in a default state
        testDefaultState2();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void selectRowAfterInvalidIndex(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void selectInvalidItem(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
        assertEquals(-1, model.getSelectedIndex());

        Object obj = new TreeItem("DUMMY");
        model.select(obj);

        assertSame(obj, model.getSelectedItem());
        assertEquals(-1, model.getSelectedIndex());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void selectValidIndex(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void clearPartialSelectionWithSingleSelection(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
        assertFalse(model.isSelected(5));
        model.select(5);
        assertTrue(model.isSelected(5));
        model.clearSelection(5);
        assertFalse(model.isSelected(5));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void ensureIsEmptyIsAccurate(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
        assertTrue(model.isEmpty());
        model.select(5);
        assertFalse(model.isEmpty());
        model.clearSelection();
        assertTrue(model.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSingleSelectionMode(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
        model.clearSelection();
        assertTrue(model.isEmpty());

        model.select(5);
        assertTrue(model.isSelected(5), "Selected: " + model.getSelectedIndex() + ", expected: 5");

        model.select(10);
        assertTrue(model.isSelected(10));
        assertFalse(model.isSelected(5));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSelectNullObject(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
        model.select(null);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testFocusOnNegativeIndex(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
        if (focusModel == null) return;
        assertEquals(0, focusModel.getFocusedIndex());
        focusModel.focus(-1);
        assertEquals(-1, focusModel.getFocusedIndex());
        assertFalse(focusModel.isFocused(-1));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testFocusOnOutOfBoundsIndex(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
        if (focusModel == null) return;
        assertEquals(0, focusModel.getFocusedIndex());
        focusModel.focus(Integer.MAX_VALUE);
        assertEquals(-1, focusModel.getFocusedIndex());
        assertNull(focusModel.getFocusedItem());
        assertFalse(focusModel.isFocused(Integer.MAX_VALUE));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testFocusOnValidIndex(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
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

    @Disabled("Not yet implemented in TreeView")
    @ParameterizedTest
    @MethodSource("parameters")
    public void testSelectionChangesWhenItemIsInsertedAtStartOfModel(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void test_rt_29821(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
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
                ControlShim.layoutChildren(currentControl);
            }

            assertFalse(cell_3.isSelected());
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void test_rt_30356_selectRowAtIndex0(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);

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
            assertEquals(ROW_1_VALUE, model.getSelectedItem(), "model is " + model);

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

    @ParameterizedTest
    @MethodSource("parameters")
    public void test_rt_30356_selectRowAtIndex1(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);

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

//        // we also check that the cell itself is selected (as often the selection
//        // model and the visuals disagree in this case).
//        // TODO remove the ComboBox conditional and test for that too
//        if (! (currentControl instanceof ChoiceBox || currentControl instanceof ComboBox)) {
//            // selection moves up from 1 to 0 in the current impl
//            int index = model.getSelectedIndex();
//            IndexedCell cell = VirtualFlowTestUtils.getCell(currentControl, index);
//            assertTrue("Cell in index " + index + " should be selected, but isn't", cell.isSelected());
//        }
    }

    private int rt32618_count = 0;
    @ParameterizedTest
    @MethodSource("parameters")
    public void test_rt32618_singleSelection(Class<? extends SelectionModel> modelClass) {
        setUp(modelClass);
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

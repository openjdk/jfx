/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.util.Callback;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;
import static org.junit.Assert.*;

public class TreeTableRowTest {
    private TreeTableRow<String> cell;
    private TreeTableView<String> tree;

    private static final String ROOT = "Root";
    private static final String APPLES = "Apples";
    private static final String ORANGES = "Oranges";
    private static final String PEARS = "Pears";

    private TreeItem<String> root;
    private TreeItem<String> apples;
    private TreeItem<String> oranges;
    private TreeItem<String> pears;

    @Before public void setup() {
        cell = new TreeTableRow<String>();

        root = new TreeItem<>(ROOT);
        apples = new TreeItem<>(APPLES);
        oranges = new TreeItem<>(ORANGES);
        pears = new TreeItem<>(PEARS);
        root.getChildren().addAll(apples, oranges, pears);

        tree = new TreeTableView<String>(root);
        root.setExpanded(true);
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void styleClassIs_tree_table_row_cell_byDefault() {
        assertStyleClassContains(cell, "tree-table-row-cell");
    }

    // The item should be null by default because the index is -1 by default
    @Test public void itemIsNullByDefault() {
        assertNull(cell.getItem());
    }

    /*********************************************************************
     * Tests for the treeView property                                   *
     ********************************************************************/

    @Test public void treeViewIsNullByDefault() {
        assertNull(cell.getTreeTableView());
        assertNull(cell.treeTableViewProperty().get());
    }

    @Test public void updateTreeTableViewUpdatesTreeTableView() {
        cell.updateTreeTableView(tree);
        assertSame(tree, cell.getTreeTableView());
        assertSame(tree, cell.treeTableViewProperty().get());
    }

    @Test public void canSetTreeTableViewBackToNull() {
        cell.updateTreeTableView(tree);
        cell.updateTreeTableView(null);
        assertNull(cell.getTreeTableView());
        assertNull(cell.treeTableViewProperty().get());
    }

    @Test public void treeViewPropertyReturnsCorrectBean() {
        assertSame(cell, cell.treeTableViewProperty().getBean());
    }

    @Test public void treeViewPropertyNameIs_treeView() {
        assertEquals("treeTableView", cell.treeTableViewProperty().getName());
    }

    @Test public void updateTreeTableViewWithNullFocusModelResultsInNoException() {
        cell.updateTreeTableView(tree);
        tree.setFocusModel(null);
        cell.updateTreeTableView(new TreeTableView());
    }

    @Test public void updateTreeTableViewWithNullFocusModelResultsInNoException2() {
        tree.setFocusModel(null);
        cell.updateTreeTableView(tree);
        cell.updateTreeTableView(new TreeTableView());
    }

    @Test public void updateTreeTableViewWithNullFocusModelResultsInNoException3() {
        cell.updateTreeTableView(tree);
        TreeTableView tree2 = new TreeTableView();
        tree2.setFocusModel(null);
        cell.updateTreeTableView(tree2);
    }

    @Test public void updateTreeTableViewWithNullSelectionModelResultsInNoException() {
        cell.updateTreeTableView(tree);
        tree.setSelectionModel(null);
        cell.updateTreeTableView(new TreeTableView());
    }

    @Test public void updateTreeTableViewWithNullSelectionModelResultsInNoException2() {
        tree.setSelectionModel(null);
        cell.updateTreeTableView(tree);
        cell.updateTreeTableView(new TreeTableView());
    }

    @Test public void updateTreeTableViewWithNullSelectionModelResultsInNoException3() {
        cell.updateTreeTableView(tree);
        TreeTableView tree2 = new TreeTableView();
        tree2.setSelectionModel(null);
        cell.updateTreeTableView(tree2);
    }

    @Test public void updateTreeTableViewWithNullItemsResultsInNoException() {
        cell.updateTreeTableView(tree);
        tree.setRoot(null);
        cell.updateTreeTableView(new TreeTableView());
    }

    @Test public void updateTreeTableViewWithNullItemsResultsInNoException2() {
        tree.setRoot(null);
        cell.updateTreeTableView(tree);
        cell.updateTreeTableView(new TreeTableView());
    }

    @Test public void updateTreeTableViewWithNullItemsResultsInNoException3() {
        cell.updateTreeTableView(tree);
        TreeTableView tree2 = new TreeTableView();
        tree2.setRoot(null);
        cell.updateTreeTableView(tree2);
    }

    /*********************************************************************
     * Tests for the item property. It should be updated whenever the    *
     * index, or treeView changes, including the treeView's items.       *
     ********************************************************************/

    @Test public void itemMatchesIndexWithinTreeItems() {
        cell.updateIndex(0);
        cell.updateTreeTableView(tree);
        assertSame(ROOT, cell.getItem());
        assertSame(root, cell.getTreeItem());
        cell.updateIndex(1);
        assertSame(APPLES, cell.getItem());
        assertSame(apples, cell.getTreeItem());
    }

    @Test public void itemMatchesIndexWithinTreeItems2() {
        cell.updateTreeTableView(tree);
        cell.updateIndex(0);
        assertSame(ROOT, cell.getItem());
        assertSame(root, cell.getTreeItem());
        cell.updateIndex(1);
        assertSame(APPLES, cell.getItem());
        assertSame(apples, cell.getTreeItem());
    }

    @Test public void itemIsNullWhenIndexIsOutOfRange() {
        cell.updateIndex(50);
        cell.updateTreeTableView(tree);
        assertNull(cell.getItem());
    }

    @Test public void treeItemIsNullWhenIndexIsOutOfRange() {
        cell.updateIndex(50);
        cell.updateTreeTableView(tree);
        assertNull(cell.getTreeItem());
    }

    @Test public void itemIsNullWhenIndexIsOutOfRange2() {
        cell.updateTreeTableView(tree);
        cell.updateIndex(50);
        assertNull(cell.getItem());
    }

    // Above were the simple tests. Now we check various circumstances
    // to make sure the item is updated correctly.

    @Ignore // TODO file bug!
    @Test public void itemIsUpdatedWhenItWasOutOfRangeButUpdatesToTreeTableViewItemsMakesItInRange() {
        cell.updateIndex(4);
        cell.updateTreeTableView(tree);
        root.getChildren().addAll(new TreeItem<String>("Pumpkin"), new TreeItem<>("Lemon"));
        assertSame("Pumpkin", cell.getItem());
    }

    @Ignore // TODO file bug!
    @Test public void itemIsUpdatedWhenItWasInRangeButUpdatesToTreeTableViewItemsMakesItOutOfRange() {
        cell.updateIndex(2);
        cell.updateTreeTableView(tree);
        assertSame(ORANGES, cell.getItem());
        root.getChildren().remove(oranges);
        assertNull(cell.getTreeItem());
        assertNull(cell.getItem());
    }

    @Ignore // TODO file bug!
    @Test public void itemIsUpdatedWhenTreeTableViewItemsIsUpdated() {
        // set cell index to point to 'Apples'
        cell.updateIndex(1);
        cell.updateTreeTableView(tree);
        assertSame(APPLES, cell.getItem());
        assertSame(apples, cell.getTreeItem());

        // then update the root children list so that the 1st item (including root),
        // is no longer 'Apples', but 'Lime'
        root.getChildren().set(0, new TreeItem<>("Lime"));
        assertEquals("Lime", cell.getItem());
    }

    @Ignore // TODO file bug!
    @Test public void itemIsUpdatedWhenTreeTableViewItemsHasNewItemInsertedBeforeIndex() {
        cell.updateIndex(2);
        cell.updateTreeTableView(tree);
        assertSame(ORANGES, cell.getItem());
        assertSame(oranges, cell.getTreeItem());
        String previous = APPLES;
        root.getChildren().add(0, new TreeItem<>("Lime"));
        assertEquals(previous, cell.getItem());
    }

//    @Test public void itemIsUpdatedWhenTreeTableViewItemsHasItemRemovedBeforeIndex() {
//        cell.updateIndex(1);
//        cell.updateTreeTableView(tree);
//        assertSame(model.get(1), cell.getItem());
//        String other = model.get(2);
//        model.remove(0);
//        assertEquals(other, cell.getItem());
//    }

    @Ignore // TODO file bug!
    @Test public void itemIsUpdatedWhenTreeTableViewItemsIsReplaced() {
        cell.updateIndex(1);
        cell.updateTreeTableView(tree);
        root.getChildren().setAll(new TreeItem<>("Water"), new TreeItem<>("Juice"), new TreeItem<>("Soda"));
        assertEquals("Water", cell.getItem());
    }

    @Test public void itemIsUpdatedWhenTreeTableViewIsReplaced() {
        cell.updateIndex(2);
        cell.updateTreeTableView(tree);
        TreeItem<String> newRoot = new TreeItem<>();
        newRoot.setExpanded(true);
        newRoot.getChildren().setAll(new TreeItem<>("Water"), new TreeItem<>("Juice"), new TreeItem<>("Soda"));
        TreeTableView<String> treeView2 = new TreeTableView<String>(newRoot);
        cell.updateTreeTableView(treeView2);
        assertEquals("Juice", cell.getItem());
    }

    @Ignore // TODO file bug!
    @Test public void replaceItemsWithANull() {
        cell.updateIndex(0);
        cell.updateTreeTableView(tree);
        tree.setRoot(null);
        assertNull(cell.getItem());
    }

//    @Test public void replaceItemsWithANull_ListenersRemovedFromFormerList() {
//        cell.updateIndex(0);
//        cell.updateTreeTableView(tree);
//        ListChangeListener listener = getListChangeListener(cell, "weakItemsListener");
//        assertListenerListContains(model, listener);
//        tree.setRoot(null);
//        assertListenerListDoesNotContain(model, treeener);
//    }
//
    @Ignore // TODO file bug!
    @Test public void replaceANullItemsWithNotNull() {
        cell.updateIndex(1);
        cell.updateTreeTableView(tree);
        tree.setRoot(null);

        TreeItem<String> newRoot = new TreeItem<>();
        newRoot.setExpanded(true);
        newRoot.getChildren().setAll(new TreeItem<>("Water"), new TreeItem<>("Juice"), new TreeItem<>("Soda"));
        tree.setRoot(newRoot);
        assertEquals("Water", cell.getItem());
    }

    /*********************************************************************
     * Tests for the selection listener                                  *
     ********************************************************************/

    @Test public void selectionOnSelectionModelIsReflectedInCells() {
        cell.updateTreeTableView(tree);
        cell.updateIndex(0);

        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
        other.updateTreeTableView(tree);
        other.updateIndex(1);

        tree.getSelectionModel().selectFirst();
        assertTrue(cell.isSelected());
        assertFalse(other.isSelected());
    }

    @Ignore // TODO file bug!
    @Test public void changesToSelectionOnSelectionModelAreReflectedInCells() {
        cell.updateTreeTableView(tree);
        cell.updateIndex(0);

        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
        other.updateTreeTableView(tree);
        other.updateIndex(1);

        // Because the TreeTableView is in single selection mode, calling
        // selectNext causes a loss of focus for the first cell.
        tree.getSelectionModel().selectFirst();
        tree.getSelectionModel().selectNext();
        assertFalse(cell.isSelected());
        assertTrue(other.isSelected());
    }

//    @Test public void replacingTheSelectionModelCausesSelectionOnCellsToBeUpdated() {
//        // Cell is configured to represent row 0, which is selected.
//        cell.updateTreeTableView(tree);
//        cell.updateIndex(0);
//        tree.getSelectionModel().select(0);
//
//        // Other is configured to represent row 1 which is not selected.
//        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
//        other.updateTreeTableView(tree);
//        other.updateIndex(1);
//
//        // The replacement selection model has row 1 selected, not row 0
//        SelectionModelMock selectionModel = new SelectionModelMock();
//        selectionModel.select(1);
//
//        tree.setSelectionModel(selectionModel);
//        assertFalse(cell.isSelected());
//        assertTrue(other.isSelected());
//    }

    @Ignore // TODO file bug!
    @Test public void changesToSelectionOnSelectionModelAreReflectedInCells_MultipleSelection() {
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cell.updateTreeTableView(tree);
        cell.updateIndex(0);

        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
        other.updateTreeTableView(tree);
        other.updateIndex(1);

        tree.getSelectionModel().selectFirst();
        tree.getSelectionModel().selectNext();
        assertTrue(cell.isSelected());
        assertTrue(other.isSelected());
    }

//    @Test public void replacingTheSelectionModelCausesSelectionOnCellsToBeUpdated_MultipleSelection() {
//        // Cell is configured to represent row 0, which is selected.
//        cell.updateTreeTableView(tree);
//        cell.updateIndex(0);
//        tree.getSelectionModel().select(0);
//
//        // Other is configured to represent row 1 which is not selected.
//        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
//        other.updateTreeTableView(tree);
//        other.updateIndex(1);
//
//        // The replacement selection model has row 0 and 1 selected
//        SelectionModelMock selectionModel = new SelectionModelMock();
//        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
//        selectionModel.selectIndices(0, 1);
//
//        tree.setSelectionModel(selectionModel);
//        assertTrue(cell.isSelected());
//        assertTrue(other.isSelected());
//    }
//
//    @Test public void replaceANullSelectionModel() {
//        // Cell is configured to represent row 0, which is selected.
//        tree.setSelectionModel(null);
//        cell.updateIndex(0);
//        cell.updateTreeTableView(tree);
//
//        // Other is configured to represent row 1 which is not selected.
//        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
//        other.updateTreeTableView(tree);
//        other.updateIndex(1);
//
//        // The replacement selection model has row 1 selected
//        SelectionModelMock selectionModel = new SelectionModelMock();
//        selectionModel.select(1);
//
//        tree.setSelectionModel(selectionModel);
//        assertFalse(cell.isSelected());
//        assertTrue(other.isSelected());
//    }

    @Test public void setANullSelectionModel() {
        // Cell is configured to represent row 0, which is selected.
        cell.updateIndex(0);
        cell.updateTreeTableView(tree);

        // Other is configured to represent row 1 which is not selected.
        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
        other.updateTreeTableView(tree);
        other.updateIndex(1);

        // Replace with a null selection model, which should clear selection
        tree.setSelectionModel(null);
        assertFalse(cell.isSelected());
        assertFalse(other.isSelected());
    }

//    @Ignore @Test public void replacingTheSelectionModelRemovesTheListenerFromTheOldModel() {
//        cell.updateIndex(0);
//        cell.updateTreeTableView(tree);
//        MultipleSelectionModel<TreeItem<String>> sm = tree.getSelectionModel();
//        ListChangeListener listener = getListChangeListener(cell, "weakSelectedListener");
//        assertListenerListContains(sm.getSelectedIndices(), listener);
//        tree.setSelectionModel(new SelectionModelMock());
//        assertListenerListDoesNotContain(sm.getSelectedIndices(), listener);
//    }

    /*********************************************************************
     * Tests for the focus listener                                      *
     ********************************************************************/

    @Test public void focusOnFocusModelIsReflectedInCells() {
        cell.updateTreeTableView(tree);
        cell.updateIndex(0);

        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
        other.updateTreeTableView(tree);
        other.updateIndex(1);

        tree.getFocusModel().focus(0);
        assertTrue(cell.isFocused());
        assertFalse(other.isFocused());
    }

    @Ignore // TODO file bug!
    @Test public void changesToFocusOnFocusModelAreReflectedInCells() {
        cell.updateTreeTableView(tree);
        cell.updateIndex(0);

        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
        other.updateTreeTableView(tree);
        other.updateIndex(1);

        tree.getFocusModel().focus(0);
        tree.getFocusModel().focus(1);
        assertFalse(cell.isFocused());
        assertTrue(other.isFocused());
    }

//    @Test public void replacingTheFocusModelCausesFocusOnCellsToBeUpdated() {
//        // Cell is configured to represent row 0, which is focused.
//        cell.updateTreeTableView(tree);
//        cell.updateIndex(0);
//        tree.getFocusModel().focus(0);
//
//        // Other is configured to represent row 1 which is not focused.
//        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
//        other.updateTreeTableView(tree);
//        other.updateIndex(1);
//
//        // The replacement focus model has row 1 selected, not row 0
//        TreeTableView.TreeTableViewFocusModel<String> focusModel = new FocusModelMock();
//        focusModel.focus(1);
//
//        tree.setFocusModel(focusModel);
//        assertFalse(cell.isFocused());
//        assertTrue(other.isFocused());
//    }
//
//    @Test public void replaceANullFocusModel() {
//        // Cell is configured to represent row 0, which is focused.
//        tree.setFocusModel(null);
//        cell.updateIndex(0);
//        cell.updateTreeTableView(tree);
//
//        // Other is configured to represent row 1 which is not focused
//        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
//        other.updateTreeTableView(tree);
//        other.updateIndex(1);
//
//        // The replacement focus model has row 1 focused
//        TreeTableView.TreeTableViewFocusModel<String> focusModel = new FocusModelMock();
//        focusModel.focus(1);
//
//        tree.setFocusModel(focusModel);
//        assertFalse(cell.isFocused());
//        assertTrue(other.isFocused());
//    }

    @Test public void setANullFocusModel() {
        // Cell is configured to represent row 0, which is focused.
        cell.updateIndex(0);
        cell.updateTreeTableView(tree);

        // Other is configured to represent row 1 which is not focused.
        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
        other.updateTreeTableView(tree);
        other.updateIndex(1);

        // Replace with a null focus model, which should clear selection
        tree.setFocusModel(null);
        assertFalse(cell.isSelected());
        assertFalse(other.isSelected());
    }

//    @Test public void replacingTheFocusModelRemovesTheListenerFromTheOldModel() {
//        cell.updateIndex(0);
//        cell.updateTreeTableView(tree);
//        FocusModel<TreeItem<String>> fm = tree.getFocusModel();
//        InvalidationListener listener = getInvalidationListener(cell, "weakFocusedListener");
//        assertValueListenersContains(fm.focusedIndexProperty(), listener);
//        tree.setFocusModel(new FocusModelMock());
//        assertValueListenersDoesNotContain(fm.focusedIndexProperty(), listener);
//    }

    /*********************************************************************
     * Tests for all things related to editing one of these guys         *
     ********************************************************************/

    // startEdit()
    @Test public void editOnTreeTableViewResultsInEditingInCell() {
        tree.setEditable(true);
        cell.updateTreeTableView(tree);
        cell.updateIndex(1);
        tree.edit(1, null);
        assertTrue(cell.isEditing());
    }

    @Test public void editOnTreeTableViewResultsInNotEditingInCellWhenDifferentIndex() {
        tree.setEditable(true);
        cell.updateTreeTableView(tree);
        cell.updateIndex(1);
        tree.edit(0, null);
        assertFalse(cell.isEditing());
    }

    @Test public void editCellWithNullTreeTableViewResultsInNoExceptions() {
        cell.updateIndex(1);
        cell.startEdit();
    }

    @Test public void editCellOnNonEditableTreeDoesNothing() {
        cell.updateIndex(1);
        cell.updateTreeTableView(tree);
        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(tree.getEditingCell());
    }

    @Ignore // TODO file bug!
    @Test public void editCellWithTreeResultsInUpdatedEditingIndexProperty() {
        tree.setEditable(true);
        cell.updateTreeTableView(tree);
        cell.updateIndex(1);
        cell.startEdit();
        assertEquals(apples, tree.getEditingCell().getTreeItem());
    }

//    @Test public void editCellFiresEventOnTree() {
//        tree.setEditable(true);
//        cell.updateTreeTableView(tree);
//        cell.updateIndex(2);
//        final boolean[] called = new boolean[] { false };
//        tree.setOnEditStart(new EventHandler<TreeTableView.EditEvent<String>>() {
//            @Override public void handle(TreeTableView.EditEvent<String> event) {
//                called[0] = true;
//            }
//        });
//        cell.startEdit();
//        assertTrue(called[0]);
//    }

    // commitEdit()
    @Test public void commitWhenTreeIsNullIsOK() {
        cell.updateIndex(1);
        cell.startEdit();
        cell.commitEdit("Watermelon");
    }

    @Test public void commitWhenTreeIsNotNullWillUpdateTheItemsTree() {
        tree.setEditable(true);
        cell.updateTreeTableView(tree);
        cell.updateIndex(1);
        cell.startEdit();
        cell.commitEdit("Watermelon");
        assertEquals("Watermelon", tree.getRoot().getChildren().get(0).getValue());
    }

//    @Test public void commitSendsEventToTree() {
//        tree.setEditable(true);
//        cell.updateTreeTableView(tree);
//        cell.updateIndex(1);
//        cell.startEdit();
//        final boolean[] called = new boolean[] { false };
//        tree.setOnEditCommit(new EventHandler<TreeTableView.EditEvent<String>>() {
//            @Override public void handle(TreeTableView.EditEvent<String> event) {
//                called[0] = true;
//            }
//        });
//        cell.commitEdit("Watermelon");
//        assertTrue(called[0]);
//    }

    @Test public void afterCommitTreeTableViewEditingCellIsNull() {
        tree.setEditable(true);
        cell.updateTreeTableView(tree);
        cell.updateIndex(1);
        cell.startEdit();
        cell.commitEdit("Watermelon");
        assertNull(tree.getEditingCell());
        assertFalse(cell.isEditing());
    }

    // cancelEdit()
    @Test public void cancelEditCanBeCalledWhileTreeTableViewIsNull() {
        cell.updateIndex(1);
        cell.startEdit();
        cell.cancelEdit();
    }

//    @Test public void cancelEditFiresChangeEvent() {
//        tree.setEditable(true);
//        cell.updateTreeTableView(tree);
//        cell.updateIndex(1);
//        cell.startEdit();
//        final boolean[] called = new boolean[] { false };
//        tree.setOnEditCancel(new EventHandler<TreeTableView.EditEvent<String>>() {
//            @Override public void handle(TreeTableView.EditEvent<String> event) {
//                called[0] = true;
//            }
//        });
//        cell.cancelEdit();
//        assertTrue(called[0]);
//    }

    @Test public void cancelSetsTreeTableViewEditingCellIsNull() {
        tree.setEditable(true);
        cell.updateTreeTableView(tree);
        cell.updateIndex(1);
        cell.startEdit();
        cell.cancelEdit();
        assertNull(tree.getEditingCell());
        assertFalse(cell.isEditing());
    }

    @Ignore // TODO file bug!
    @Test public void movingTreeCellEditingIndexCausesCurrentlyInEditCellToCancel() {
        tree.setEditable(true);
        cell.updateTreeTableView(tree);
        cell.updateIndex(0);
        cell.startEdit();

        TreeTableCell<String,String> other = new TreeTableCell<String,String>();
        other.updateTreeTableView(tree);
        other.updateIndex(1);
        tree.edit(1, null);

        assertTrue(other.isEditing());
        assertFalse(cell.isEditing());
    }

    // When the tree view item's change and affects a cell that is editing, then what?
    // When the tree cell's index is changed while it is editing, then what?


//    private final class SelectionModelMock extends TreeTableView.TreeTableViewSelectionModel<String> {
//
//        public SelectionModelMock() {
//            super(tree);
//        }
//
//        @Override public ObservableList<TreeTablePosition<String, ?>> getSelectedCells() {
//            throw new RuntimeException("Not implemented");
//        }
//
//        @Override public boolean isSelected(int row, TreeTableColumn<String, ?> column) {
//            throw new RuntimeException("Not implemented");
//        }
//
//        @Override public void select(int row, TreeTableColumn<String, ?> column) {
//            throw new RuntimeException("Not implemented");
//        }
//
//        @Override public void clearAndSelect(int row, TreeTableColumn<String, ?> column) {
//            throw new RuntimeException("Not implemented");
//        }
//
//        @Override public void clearSelection(int row, TreeTableColumn<String, ?> column) {
//            throw new RuntimeException("Not implemented");
//        }
//
//        @Override public void selectLeftCell() {
//            throw new RuntimeException("Not implemented");
//        }
//
//        @Override public void selectRightCell() {
//            throw new RuntimeException("Not implemented");
//        }
//
//        @Override public void selectAboveCell() {
//            throw new RuntimeException("Not implemented");
//        }
//
//        @Override public void selectBelowCell() {
//            throw new RuntimeException("Not implemented");
//        }
//
//        @Override protected int getItemCount() {
//            return root.getChildren().size() + 1;
//        }
//
//        @Override public TreeItem<String> getModelItem(int index) {
//            return index == 0 ? root : root.getChildren().get(index - 1);
//        }
////
////        @Override protected void focus(int index) {
////            // no op
////        }
////
////        @Override protected int getFocusedIndex() {
////            return tree.getFocusModel().getFocusedIndex();
////        }
////
////        @Override
////        public void select(int row, TreeTableColumn<String, ?> column) {
////            //To change body of implemented methods use File | Settings | File Templates.
////        }
//    };
//
//    private final class FocusModelMock extends TreeTableView.TreeTableViewFocusModel {
//
//        public FocusModelMock() {
//            super(tree);
//        }
//
//        @Override protected int getItemCount() {
//            return root.getChildren().size() + 1;
//        }
//
//        @Override protected TreeItem<String> getModelItem(int index) {
//            return index == 0 ? root : root.getChildren().get(index - 1);
//        }
//    }



    /*********************************************************************
     * Tests for the treeTableView property                              *
     ********************************************************************/

    @Test public void tableViewIsNullByDefault() {
        assertNull(cell.getTreeTableView());
        assertNull(cell.treeTableViewProperty().get());
    }

    @Test public void updateTreeTableViewUpdatesTableView() {
        cell.updateTreeTableView(tree);
        assertSame(tree, cell.getTreeTableView());
        assertSame(tree, cell.treeTableViewProperty().get());
    }

    @Test public void canSetTableViewBackToNull() {
        cell.updateTreeTableView(tree);
        cell.updateTreeTableView(null);
        assertNull(cell.getTreeTableView());
        assertNull(cell.treeTableViewProperty().get());
    }

    @Test public void tableViewPropertyReturnsCorrectBean() {
        assertSame(cell, cell.treeTableViewProperty().getBean());
    }

    @Test public void tableViewPropertyNameIs_treeTableView() {
        assertEquals("treeTableView", cell.treeTableViewProperty().getName());
    }
}

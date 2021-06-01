/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.tk.Toolkit;

import static javafx.scene.control.ControlShim.*;
import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.FocusModel;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.MultipleSelectionModelBaseShim;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeView.EditEvent;
import javafx.scene.control.skin.TreeCellSkin;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

public class TreeCellTest {
    private TreeCell<String> cell;
    private TreeView<String> tree;

    private static final String ROOT = "Root";
    private static final String APPLES = "Apples";
    private static final String ORANGES = "Oranges";
    private static final String PEARS = "Pears";

    private TreeItem<String> root;
    private TreeItem<String> apples;
    private TreeItem<String> oranges;
    private TreeItem<String> pears;
    private StageLoader stageLoader;

    @Before public void setup() {
        cell = new TreeCell<String>();

        root = new TreeItem<>(ROOT);
        apples = new TreeItem<>(APPLES);
        oranges = new TreeItem<>(ORANGES);
        pears = new TreeItem<>(PEARS);
        root.getChildren().addAll(apples, oranges, pears);

        tree = new TreeView<String>(root);
        root.setExpanded(true);
    }

    @After
    public void cleanup() {
        if (stageLoader != null) stageLoader.dispose();
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void styleClassIs_tree_cell_byDefault() {
        assertStyleClassContains(cell, "tree-cell");
    }

    // The item should be null by default because the index is -1 by default
    @Test public void itemIsNullByDefault() {
        assertNull(cell.getItem());
    }

    /*********************************************************************
     * Tests for the treeView property                                   *
     ********************************************************************/

    @Test public void treeViewIsNullByDefault() {
        assertNull(cell.getTreeView());
        assertNull(cell.treeViewProperty().get());
    }

    @Test public void updateTreeViewUpdatesTreeView() {
        cell.updateTreeView(tree);
        assertSame(tree, cell.getTreeView());
        assertSame(tree, cell.treeViewProperty().get());
    }

    @Test public void canSetTreeViewBackToNull() {
        cell.updateTreeView(tree);
        cell.updateTreeView(null);
        assertNull(cell.getTreeView());
        assertNull(cell.treeViewProperty().get());
    }

    @Test public void treeViewPropertyReturnsCorrectBean() {
        assertSame(cell, cell.treeViewProperty().getBean());
    }

    @Test public void treeViewPropertyNameIs_treeView() {
        assertEquals("treeView", cell.treeViewProperty().getName());
    }

    @Test public void updateTreeViewWithNullFocusModelResultsInNoException() {
        cell.updateTreeView(tree);
        tree.setFocusModel(null);
        cell.updateTreeView(new TreeView());
    }

    @Test public void updateTreeViewWithNullFocusModelResultsInNoException2() {
        tree.setFocusModel(null);
        cell.updateTreeView(tree);
        cell.updateTreeView(new TreeView());
    }

    @Test public void updateTreeViewWithNullFocusModelResultsInNoException3() {
        cell.updateTreeView(tree);
        TreeView tree2 = new TreeView();
        tree2.setFocusModel(null);
        cell.updateTreeView(tree2);
    }

    @Test public void updateTreeViewWithNullSelectionModelResultsInNoException() {
        cell.updateTreeView(tree);
        tree.setSelectionModel(null);
        cell.updateTreeView(new TreeView());
    }

    @Test public void updateTreeViewWithNullSelectionModelResultsInNoException2() {
        tree.setSelectionModel(null);
        cell.updateTreeView(tree);
        cell.updateTreeView(new TreeView());
    }

    @Test public void updateTreeViewWithNullSelectionModelResultsInNoException3() {
        cell.updateTreeView(tree);
        TreeView tree2 = new TreeView();
        tree2.setSelectionModel(null);
        cell.updateTreeView(tree2);
    }

    @Test public void updateTreeViewWithNullItemsResultsInNoException() {
        cell.updateTreeView(tree);
        tree.setRoot(null);
        cell.updateTreeView(new TreeView());
    }

    @Test public void updateTreeViewWithNullItemsResultsInNoException2() {
        tree.setRoot(null);
        cell.updateTreeView(tree);
        cell.updateTreeView(new TreeView());
    }

    @Test public void updateTreeViewWithNullItemsResultsInNoException3() {
        cell.updateTreeView(tree);
        TreeView tree2 = new TreeView();
        tree2.setRoot(null);
        cell.updateTreeView(tree2);
    }

    /*********************************************************************
     * Tests for the item property. It should be updated whenever the    *
     * index, or treeView changes, including the treeView's items.       *
     ********************************************************************/

    @Test public void itemMatchesIndexWithinTreeItems() {
        cell.updateIndex(0);
        cell.updateTreeView(tree);
        assertSame(ROOT, cell.getItem());
        assertSame(root, cell.getTreeItem());
        cell.updateIndex(1);
        assertSame(APPLES, cell.getItem());
        assertSame(apples, cell.getTreeItem());
    }

    @Test public void itemMatchesIndexWithinTreeItems2() {
        cell.updateTreeView(tree);
        cell.updateIndex(0);
        assertSame(ROOT, cell.getItem());
        assertSame(root, cell.getTreeItem());
        cell.updateIndex(1);
        assertSame(APPLES, cell.getItem());
        assertSame(apples, cell.getTreeItem());
    }

    @Test public void itemIsNullWhenIndexIsOutOfRange() {
        cell.updateIndex(50);
        cell.updateTreeView(tree);
        assertNull(cell.getItem());
    }

    @Test public void treeItemIsNullWhenIndexIsOutOfRange() {
        cell.updateIndex(50);
        cell.updateTreeView(tree);
        assertNull(cell.getTreeItem());
    }

    @Test public void itemIsNullWhenIndexIsOutOfRange2() {
        cell.updateTreeView(tree);
        cell.updateIndex(50);
        assertNull(cell.getItem());
    }

    // Above were the simple tests. Now we check various circumstances
    // to make sure the item is updated correctly.

    @Ignore // TODO file bug!
    @Test public void itemIsUpdatedWhenItWasOutOfRangeButUpdatesToTreeViewItemsMakesItInRange() {
        cell.updateIndex(4);
        cell.updateTreeView(tree);
        root.getChildren().addAll(new TreeItem<String>("Pumpkin"), new TreeItem<>("Lemon"));
        assertSame("Pumpkin", cell.getItem());
    }

    @Ignore // TODO file bug!
    @Test public void itemIsUpdatedWhenItWasInRangeButUpdatesToTreeViewItemsMakesItOutOfRange() {
        cell.updateIndex(2);
        cell.updateTreeView(tree);
        assertSame(ORANGES, cell.getItem());
        root.getChildren().remove(oranges);
        assertNull(cell.getTreeItem());
        assertNull(cell.getItem());
    }

    @Ignore // TODO file bug!
    @Test public void itemIsUpdatedWhenTreeViewItemsIsUpdated() {
        // set cell index to point to 'Apples'
        cell.updateIndex(1);
        cell.updateTreeView(tree);
        assertSame(APPLES, cell.getItem());
        assertSame(apples, cell.getTreeItem());

        // then update the root children list so that the 1st item (including root),
        // is no longer 'Apples', but 'Lime'
        root.getChildren().set(0, new TreeItem<>("Lime"));
        assertEquals("Lime", cell.getItem());
    }

    @Ignore // TODO file bug!
    @Test public void itemIsUpdatedWhenTreeViewItemsHasNewItemInsertedBeforeIndex() {
        cell.updateIndex(2);
        cell.updateTreeView(tree);
        assertSame(ORANGES, cell.getItem());
        assertSame(oranges, cell.getTreeItem());
        String previous = APPLES;
        root.getChildren().add(0, new TreeItem<>("Lime"));
        assertEquals(previous, cell.getItem());
    }

//    @Test public void itemIsUpdatedWhenTreeViewItemsHasItemRemovedBeforeIndex() {
//        cell.updateIndex(1);
//        cell.updateTreeView(tree);
//        assertSame(model.get(1), cell.getItem());
//        String other = model.get(2);
//        model.remove(0);
//        assertEquals(other, cell.getItem());
//    }

    @Ignore // TODO file bug!
    @Test public void itemIsUpdatedWhenTreeViewItemsIsReplaced() {
        cell.updateIndex(1);
        cell.updateTreeView(tree);
        root.getChildren().setAll(new TreeItem<>("Water"), new TreeItem<>("Juice"), new TreeItem<>("Soda"));
        assertEquals("Water", cell.getItem());
    }

    @Test public void itemIsUpdatedWhenTreeViewIsReplaced() {
        cell.updateIndex(2);
        cell.updateTreeView(tree);
        TreeItem<String> newRoot = new TreeItem<>();
        newRoot.setExpanded(true);
        newRoot.getChildren().setAll(new TreeItem<>("Water"), new TreeItem<>("Juice"), new TreeItem<>("Soda"));
        TreeView<String> treeView2 = new TreeView<String>(newRoot);
        cell.updateTreeView(treeView2);
        assertEquals("Juice", cell.getItem());
    }

    @Test public void replaceItemsWithANull() {
        cell.updateIndex(0);
        cell.updateTreeView(tree);
        tree.setRoot(null);
        assertNull(cell.getItem());
    }

//    @Test public void replaceItemsWithANull_ListenersRemovedFromFormerList() {
//        cell.updateIndex(0);
//        cell.updateTreeView(tree);
//        ListChangeListener listener = getListChangeListener(cell, "weakItemsListener");
//        assertListenerListContains(model, listener);
//        tree.setRoot(null);
//        assertListenerListDoesNotContain(model, treeener);
//    }
//
    @Test public void replaceANullItemsWithNotNull() {
        cell.updateIndex(1);
        cell.updateTreeView(tree);
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
        cell.updateTreeView(tree);
        cell.updateIndex(0);

        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        tree.getSelectionModel().selectFirst();
        assertTrue(cell.isSelected());
        assertFalse(other.isSelected());
    }

    @Test public void changesToSelectionOnSelectionModelAreReflectedInCells() {
        cell.updateTreeView(tree);
        cell.updateIndex(0);

        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        // Because the TreeView is in single selection mode, calling
        // selectNext causes a loss of focus for the first cell.
        tree.getSelectionModel().selectFirst();
        tree.getSelectionModel().selectNext();
        assertFalse(cell.isSelected());
        assertTrue(other.isSelected());
    }

    @Test public void replacingTheSelectionModelCausesSelectionOnCellsToBeUpdated() {
        // Cell is configured to represent row 0, which is selected.
        cell.updateTreeView(tree);
        cell.updateIndex(0);
        tree.getSelectionModel().select(0);

        // Other is configured to represent row 1 which is not selected.
        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        // The replacement selection model has row 1 selected, not row 0
        MultipleSelectionModel<TreeItem<String>> selectionModel = new SelectionModelMock();
        selectionModel.select(1);

        tree.setSelectionModel(selectionModel);
        assertFalse(cell.isSelected());
        assertTrue(other.isSelected());
    }

    @Test public void changesToSelectionOnSelectionModelAreReflectedInCells_MultipleSelection() {
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cell.updateTreeView(tree);
        cell.updateIndex(0);

        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        tree.getSelectionModel().selectFirst();
        tree.getSelectionModel().selectNext();
        assertTrue(cell.isSelected());
        assertTrue(other.isSelected());
    }

    @Test public void replacingTheSelectionModelCausesSelectionOnCellsToBeUpdated_MultipleSelection() {
        // Cell is configured to represent row 0, which is selected.
        cell.updateTreeView(tree);
        cell.updateIndex(0);
        tree.getSelectionModel().select(0);

        // Other is configured to represent row 1 which is not selected.
        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        // The replacement selection model has row 0 and 1 selected
        MultipleSelectionModel<TreeItem<String>> selectionModel = new SelectionModelMock();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
        selectionModel.selectIndices(0, 1);

        tree.setSelectionModel(selectionModel);
        assertTrue(cell.isSelected());
        assertTrue(other.isSelected());
    }

    @Test public void replaceANullSelectionModel() {
        // Cell is configured to represent row 0, which is selected.
        tree.setSelectionModel(null);
        cell.updateIndex(0);
        cell.updateTreeView(tree);

        // Other is configured to represent row 1 which is not selected.
        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        // The replacement selection model has row 1 selected
        MultipleSelectionModel<TreeItem<String>> selectionModel = new SelectionModelMock();
        selectionModel.select(1);

        tree.setSelectionModel(selectionModel);
        assertFalse(cell.isSelected());
        assertTrue(other.isSelected());
    }

    @Test public void setANullSelectionModel() {
        // Cell is configured to represent row 0, which is selected.
        cell.updateIndex(0);
        cell.updateTreeView(tree);

        // Other is configured to represent row 1 which is not selected.
        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        // Replace with a null selection model, which should clear selection
        tree.setSelectionModel(null);
        assertFalse(cell.isSelected());
        assertFalse(other.isSelected());
    }

    @Ignore @Test public void replacingTheSelectionModelRemovesTheListenerFromTheOldModel() {
        cell.updateIndex(0);
        cell.updateTreeView(tree);
        MultipleSelectionModel<TreeItem<String>> sm = tree.getSelectionModel();
        ListChangeListener listener = getListChangeListener(cell, "weakSelectedListener");
        assertListenerListContains(sm.getSelectedIndices(), listener);
        tree.setSelectionModel(new SelectionModelMock());
        assertListenerListDoesNotContain(sm.getSelectedIndices(), listener);
    }

    /*********************************************************************
     * Tests for the focus listener                                      *
     ********************************************************************/

    @Test public void focusOnFocusModelIsReflectedInCells() {
        cell.updateTreeView(tree);
        cell.updateIndex(0);

        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        tree.getFocusModel().focus(0);
        assertTrue(cell.isFocused());
        assertFalse(other.isFocused());
    }

    @Test public void changesToFocusOnFocusModelAreReflectedInCells() {
        cell.updateTreeView(tree);
        cell.updateIndex(0);

        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        tree.getFocusModel().focus(0);
        tree.getFocusModel().focus(1);
        assertFalse(cell.isFocused());
        assertTrue(other.isFocused());
    }

    @Test public void replacingTheFocusModelCausesFocusOnCellsToBeUpdated() {
        // Cell is configured to represent row 0, which is focused.
        cell.updateTreeView(tree);
        cell.updateIndex(0);
        tree.getFocusModel().focus(0);

        // Other is configured to represent row 1 which is not focused.
        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        // The replacement focus model has row 1 selected, not row 0
        FocusModel<TreeItem<String>> focusModel = new FocusModelMock();
        focusModel.focus(1);

        tree.setFocusModel(focusModel);
        assertFalse(cell.isFocused());
        assertTrue(other.isFocused());
    }

    @Test public void replaceANullFocusModel() {
        // Cell is configured to represent row 0, which is focused.
        tree.setFocusModel(null);
        cell.updateIndex(0);
        cell.updateTreeView(tree);

        // Other is configured to represent row 1 which is not focused
        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        // The replacement focus model has row 1 focused
        FocusModel<TreeItem<String>> focusModel = new FocusModelMock();
        focusModel.focus(1);

        tree.setFocusModel(focusModel);
        assertFalse(cell.isFocused());
        assertTrue(other.isFocused());
    }

    @Test public void setANullFocusModel() {
        // Cell is configured to represent row 0, which is focused.
        cell.updateIndex(0);
        cell.updateTreeView(tree);

        // Other is configured to represent row 1 which is not focused.
        TreeCell<String> other = new TreeCell<String>();
        other.updateTreeView(tree);
        other.updateIndex(1);

        // Replace with a null focus model, which should clear selection
        tree.setFocusModel(null);
        assertFalse(cell.isSelected());
        assertFalse(other.isSelected());
    }

    @Test public void replacingTheFocusModelRemovesTheListenerFromTheOldModel() {
        cell.updateIndex(0);
        cell.updateTreeView(tree);
        FocusModel<TreeItem<String>> fm = tree.getFocusModel();
        InvalidationListener listener = getInvalidationListener(cell, "weakFocusedListener");
        assertValueListenersContains(fm.focusedIndexProperty(), listener);
        tree.setFocusModel(new FocusModelMock());
        assertValueListenersDoesNotContain(fm.focusedIndexProperty(), listener);
    }

    /*********************************************************************
     * Tests for all things related to editing one of these guys         *
     ********************************************************************/

    // startEdit()
    @Test public void editOnTreeViewResultsInEditingInCell() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        cell.updateIndex(1);
        tree.edit(apples);
        assertTrue(cell.isEditing());
    }

    @Test public void editOnTreeViewResultsInNotEditingInCellWhenDifferentIndex() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        cell.updateIndex(1);
        tree.edit(root);
        assertFalse(cell.isEditing());
    }

    @Test public void editCellWithNullTreeViewResultsInNoExceptions() {
        cell.updateIndex(1);
        cell.startEdit();
    }

    @Test public void editCellOnNonEditableTreeDoesNothing() {
        cell.updateIndex(1);
        cell.updateTreeView(tree);
        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(tree.getEditingItem());
    }

    @Ignore // TODO file bug!
    @Test public void editCellWithTreeResultsInUpdatedEditingIndexProperty() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        cell.updateIndex(1);
        cell.startEdit();
        assertEquals(apples, tree.getEditingItem());
    }

    @Test public void editCellFiresEventOnTree() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        cell.updateIndex(2);
        final boolean[] called = new boolean[] { false };
        tree.setOnEditStart(event -> {
            called[0] = true;
        });
        cell.startEdit();
        assertTrue(called[0]);
    }

    // commitEdit()
    @Test public void commitWhenTreeIsNullIsOK() {
        cell.updateIndex(1);
        cell.startEdit();
        cell.commitEdit("Watermelon");
    }

    @Test public void commitWhenTreeIsNotNullWillUpdateTheItemsTree() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        cell.updateIndex(1);
        cell.startEdit();
        cell.commitEdit("Watermelon");
        assertEquals("Watermelon", tree.getRoot().getChildren().get(0).getValue());
    }

    @Test public void commitSendsEventToTree() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        cell.updateIndex(1);
        cell.startEdit();
        final boolean[] called = new boolean[] { false };
        tree.setOnEditCommit(event -> {
            called[0] = true;
        });
        cell.commitEdit("Watermelon");
        assertTrue(called[0]);
    }

    @Test public void afterCommitTreeViewEditingIndexIsNegativeOne() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        cell.updateIndex(1);
        cell.startEdit();
        cell.commitEdit("Watermelon");
        assertNull(tree.getEditingItem());
        assertFalse(cell.isEditing());
    }

    // cancelEdit()
    @Test public void cancelEditCanBeCalledWhileTreeViewIsNull() {
        cell.updateIndex(1);
        cell.startEdit();
        cell.cancelEdit();
    }

    @Test public void cancelEditFiresChangeEvent() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        cell.updateIndex(1);
        cell.startEdit();
        final boolean[] called = new boolean[] { false };
        tree.setOnEditCancel(event -> {
            called[0] = true;
        });
        cell.cancelEdit();
        assertTrue(called[0]);
    }

    @Test public void cancelSetsTreeViewEditingIndexToNegativeOne() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        cell.updateIndex(1);
        cell.startEdit();
        cell.cancelEdit();
        assertNull(tree.getEditingItem());
        assertFalse(cell.isEditing());
    }

    @Test public void movingTreeCellEditingIndexCausesCurrentlyInEditCellToCancel() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        cell.updateIndex(0);
        cell.startEdit();

        TreeCell other = new TreeCell();
        other.updateTreeView(tree);
        other.updateIndex(1);
        tree.edit(apples);

        assertTrue(other.isEditing());
        assertFalse(cell.isEditing());
    }

    @Test
    public void testEditCancelEventAfterCancelOnCell() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        int editingIndex = 1;
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        cell.updateIndex(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        cell.cancelEdit();
        assertEquals(1, events.size());
        assertEquals("editing location of cancel event", editingItem, events.get(0).getTreeItem());
    }

    @Test
    public void testEditCancelEventAfterCancelOnTree() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        int editingIndex = 1;
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        cell.updateIndex(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        tree.edit(null);
        assertEquals(1, events.size());
        assertEquals("editing location of cancel event", editingItem, events.get(0).getTreeItem());
    }

    @Test
    public void testEditCancelEventAfterCellReuse() {
        tree.setEditable(true);
        cell.updateTreeView(tree);
        int editingIndex = 1;
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        cell.updateIndex(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        cell.updateIndex(0);
        assertEquals(1, events.size());
        assertEquals("editing location of cancel event", editingItem, events.get(0).getTreeItem());
    }

    @Test
    public void testEditCancelEventAfterCollapse() {
        stageLoader = new StageLoader(tree);
        tree.setEditable(true);
        int editingIndex = 1;
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        root.setExpanded(false);
        Toolkit.getToolkit().firePulse();
        assertEquals(1, events.size());
        assertEquals("editing location of cancel event", editingItem, events.get(0).getTreeItem());
    }

    @Test
    public void testEditCancelEventAfterModifyItems() {
        stageLoader = new StageLoader(tree);
        tree.setEditable(true);
        int editingIndex = 2;
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        root.getChildren().add(0, new TreeItem<>("added"));
        Toolkit.getToolkit().firePulse();
        assertEquals(1, events.size());
        assertEquals("editing location of cancel event", editingItem, events.get(0).getTreeItem());
    }

    /**
     * Test that removing the editing item implicitly cancels an ongoing
     * edit and fires a correct cancel event.
     */
    @Test
    public void testEditCancelEventAfterRemoveEditingItem() {
        stageLoader = new StageLoader(tree);
        tree.setEditable(true);
        int editingIndex = 2;
        TreeItem<String> editingItem = tree.getTreeItem(editingIndex);
        tree.edit(editingItem);
        List<EditEvent<String>> events = new ArrayList<>();
        tree.setOnEditCancel(events::add);
        root.getChildren().remove(editingItem);
        Toolkit.getToolkit().firePulse();
        assertNull("removing item must cancel edit on tree", tree.getEditingItem());
        assertEquals(1, events.size());
        assertEquals("editing location of cancel event", editingItem, events.get(0).getTreeItem());
    }

    /**
     * Test that removing the editing item does not cause a memory leak.
     */
    @Test
    public void testEditCancelMemoryLeakAfterRemoveEditingItem() {
        stageLoader = new StageLoader(tree);
        tree.setEditable(true);
        // the item to test for being gc'ed
        TreeItem<String> editingItem = new TreeItem<>("added");
        WeakReference<TreeItem<?>> itemRef = new WeakReference<>(editingItem);
        root.getChildren().add(0, editingItem);
        Toolkit.getToolkit().firePulse();
        tree.edit(editingItem);
        root.getChildren().remove(editingItem);
        Toolkit.getToolkit().firePulse();
        assertNull("removing item must cancel edit on tree", tree.getEditingItem());
        editingItem = null;
        attemptGC(itemRef);
        assertEquals("treeItem must be gc'ed", null, itemRef.get());
    }

    /**
     * Test that removing a committed editing item does not cause a memory leak.
     */
    @Test
    public void testEditCommitMemoryLeakAfterRemoveEditingItem() {
        stageLoader = new StageLoader(tree);
        tree.setEditable(true);
        // the item to test for being gc'ed
        TreeItem<String> editingItem = new TreeItem<>("added");
        WeakReference<TreeItem<?>> itemRef = new WeakReference<>(editingItem);
        root.getChildren().add(0, editingItem);
        int editingIndex = tree.getRow(editingItem);
        Toolkit.getToolkit().firePulse();
        tree.edit(editingItem);
        TreeCell<String> editingCell = (TreeCell<String>) VirtualFlowTestUtils.getCell(tree, editingIndex);
        editingCell.commitEdit("added changed");
        root.getChildren().remove(editingItem);
        Toolkit.getToolkit().firePulse();
        assertNull("removing item must cancel edit on tree", tree.getEditingItem());
        editingItem = null;
        attemptGC(itemRef);
        assertEquals("treeItem must be gc'ed", null, itemRef.get());
    }

    // When the tree view item's change and affects a cell that is editing, then what?
    // When the tree cell's index is changed while it is editing, then what?



    @Test public void test_rt_33106() {
        cell.updateTreeView(tree);
        tree.setRoot(null);
        cell.updateIndex(1);
    }



    private final class SelectionModelMock extends MultipleSelectionModelBaseShim<TreeItem<String>> {
        @Override protected int getItemCount() {
            return root.getChildren().size() + 1;
        }

        @Override protected TreeItem<String> getModelItem(int index) {
            return index == 0 ? root : root.getChildren().get(index - 1);
        }

        @Override protected void focus(int index) {
            // no op
        }

        @Override protected int getFocusedIndex() {
            return tree.getFocusModel().getFocusedIndex();
        }
    };

    private final class FocusModelMock extends FocusModel {
        @Override protected int getItemCount() {
            return root.getChildren().size() + 1;
        }

        @Override protected TreeItem<String> getModelItem(int index) {
            return index == 0 ? root : root.getChildren().get(index - 1);
        }
    }

    @Test public void test_jdk_8151524() {
        TreeCell cell = new TreeCell();
        cell.setSkin(new TreeCellSkin(cell));
    }

    /**
     * Test that min/max/pref height respect fixedCellSize.
     * Sanity test when fixing JDK-8253634.
     */
    @Test
    public void testTreeCellHeights() {
        TreeCell<Object> cell =  new TreeCell<>();
        TreeView<Object> treeView = new TreeView<>();
        cell.updateTreeView(treeView);
        installDefaultSkin(cell);
        treeView.setFixedCellSize(100);
        assertEquals("pref height must be fixedCellSize",
                treeView.getFixedCellSize(),
                cell.prefHeight(-1), 1);
        assertEquals("min height must be fixedCellSize",
                treeView.getFixedCellSize(),
                cell.minHeight(-1), 1);
        assertEquals("max height must be fixedCellSize",
                treeView.getFixedCellSize(),
                cell.maxHeight(-1), 1);
    }

}

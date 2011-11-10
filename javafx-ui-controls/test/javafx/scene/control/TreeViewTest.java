/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import static javafx.scene.control.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TreeViewTest {
    private TreeView<String> treeView;
    private TreeItem<String> root;
    private TreeItem<String> child1;
    private TreeItem<String> child2;
    private TreeItem<String> child3;
    
    @Before public void setup() {
        treeView = new TreeView<String>();
    }

    private void installChildren() {
        root = new TreeItem<String>("Root");
        child1 = new TreeItem<String>("Child 1");
        child2 = new TreeItem<String>("Child 2");
        child3 = new TreeItem<String>("Child 3");
        root.setExpanded(true);
        root.getChildren().setAll(child1, child2, child3);
        treeView.setRoot(root);
    }
    
    @Test public void ensureCorrectInitialState() {
        installChildren();
        assertEquals(0, treeView.getRow(root));
        assertEquals(1, treeView.getRow(child1));
        assertEquals(2, treeView.getRow(child2));
        assertEquals(3, treeView.getRow(child3));
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void noArgConstructorSetsTheStyleClass() {
        assertStyleClassContains(treeView, "tree-view");
    }

    @Test public void noArgConstructorSetsNonNullSelectionModel() {
        assertNotNull(treeView.getSelectionModel());
    }

    @Test public void noArgConstructorSetsNullItems() {
        assertNull(treeView.getRoot());
    }

    @Test public void noArgConstructor_selectedItemIsNull() {
        assertNull(treeView.getSelectionModel().getSelectedItem());
    }

    @Test public void noArgConstructor_selectedIndexIsNegativeOne() {
        assertEquals(-1, treeView.getSelectionModel().getSelectedIndex());
    }

    @Test public void singleArgConstructorSetsTheStyleClass() {
        final TreeView<String> b2 = new TreeView<String>(new TreeItem<String>("Hi"));
        assertStyleClassContains(b2, "tree-view");
    }

    @Test public void singleArgConstructorSetsNonNullSelectionModel() {
        final TreeView<String> b2 = new TreeView<String>(new TreeItem<String>("Hi"));
        assertNotNull(b2.getSelectionModel());
    }

    @Test public void singleArgConstructorAllowsNullItems() {
        final TreeView<String> b2 = new TreeView<String>(null);
        assertNull(b2.getRoot());
    }

    @Test public void singleArgConstructor_selectedItemIsNull() {
        final TreeView<String> b2 = new TreeView<String>(new TreeItem<String>("Hi"));
        assertNull(b2.getSelectionModel().getSelectedItem());
    }

    @Test public void singleArgConstructor_selectedIndexIsNegativeOne() {
        final TreeView<String> b2 = new TreeView<String>(new TreeItem<String>("Hi"));
        assertEquals(-1, b2.getSelectionModel().getSelectedIndex());
    }

    /*********************************************************************
     * Tests for selection model                                         *
     ********************************************************************/

    @Test public void selectionModelCanBeNull() {
        treeView.setSelectionModel(null);
        assertNull(treeView.getSelectionModel());
    }

    @Test public void selectionModelCanBeBound() {
        MultipleSelectionModel<TreeItem<String>> sm = new TreeView.TreeViewBitSetSelectionModel<String>(treeView);
        ObjectProperty<MultipleSelectionModel<TreeItem<String>>> other = new SimpleObjectProperty<MultipleSelectionModel<TreeItem<String>>>(sm);
        treeView.selectionModelProperty().bind(other);
        assertSame(sm, treeView.getSelectionModel());
    }

    @Test public void selectionModelCanBeChanged() {
        MultipleSelectionModel<TreeItem<String>> sm = new TreeView.TreeViewBitSetSelectionModel<String>(treeView);
        treeView.setSelectionModel(sm);
        assertSame(sm, treeView.getSelectionModel());
    }

    @Test public void canSetSelectedItemToAnItemEvenWhenThereAreNoItems() {
        TreeItem<String> element = new TreeItem<String>("I AM A CRAZY RANDOM STRING");
        treeView.getSelectionModel().select(element);
        assertEquals(-1, treeView.getSelectionModel().getSelectedIndex());
        assertSame(element, treeView.getSelectionModel().getSelectedItem());
    }

    @Test public void canSetSelectedItemToAnItemNotInTheDataModel() {
        installChildren();
        TreeItem<String> element = new TreeItem<String>("I AM A CRAZY RANDOM STRING");
        treeView.getSelectionModel().select(element);
        assertEquals(-1, treeView.getSelectionModel().getSelectedIndex());
        assertSame(element, treeView.getSelectionModel().getSelectedItem());
    }

    @Test public void settingTheSelectedItemToAnItemInItemsResultsInTheCorrectSelectedIndex() {
        installChildren();
        treeView.getSelectionModel().select(child1);
        assertEquals(1, treeView.getSelectionModel().getSelectedIndex());
        assertSame(child1, treeView.getSelectionModel().getSelectedItem());
    }

    @Ignore("Not yet supported")
    @Test public void settingTheSelectedItemToANonexistantItemAndThenSettingItemsWhichContainsItResultsInCorrectSelectedIndex() {
        treeView.getSelectionModel().select(child1);
        installChildren();
        assertEquals(1, treeView.getSelectionModel().getSelectedIndex());
        assertSame(child1, treeView.getSelectionModel().getSelectedItem());
    }
    
    @Ignore("Not yet supported")
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex0() {
        installChildren();
        treeView.getSelectionModel().select(0);
        treeView.setRoot(null);
        assertEquals(-1, treeView.getSelectionModel().getSelectedIndex());
        assertEquals(null, treeView.getSelectionModel().getSelectedItem());
    }
    
    @Ignore("Not yet supported")
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex2() {
        installChildren();
        treeView.getSelectionModel().select(2);
        treeView.setRoot(null);
        assertEquals(-1, treeView.getSelectionModel().getSelectedIndex());
        assertEquals(null, treeView.getSelectionModel().getSelectedItem());
    }
    
    @Ignore("Not yet supported")
    @Test public void ensureSelectedItemRemainsAccurateWhenItemsAreCleared() {
        installChildren();
        treeView.getSelectionModel().select(2);
        treeView.setRoot(null);
        assertNull(treeView.getSelectionModel().getSelectedItem());
        assertEquals(-1, treeView.getSelectionModel().getSelectedIndex());
        
        TreeItem<String> newRoot = new TreeItem<String>("New Root");
        TreeItem<String> newChild1 = new TreeItem<String>("New Child 1");
        TreeItem<String> newChild2 = new TreeItem<String>("New Child 2");
        TreeItem<String> newChild3 = new TreeItem<String>("New Child 3");
        newRoot.setExpanded(true);
        newRoot.getChildren().setAll(newChild1, newChild2, newChild3);
        treeView.setRoot(root);
        
        treeView.getSelectionModel().select(2);
        assertEquals(newChild2, treeView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionIsCorrectWhenItemsChange() {
        installChildren();
        treeView.getSelectionModel().select(0);
        assertEquals(root, treeView.getSelectionModel().getSelectedItem());
        
        treeView.setRoot(new TreeItem<String>("New Root"));
        assertEquals(-1, treeView.getSelectionModel().getSelectedIndex());
        assertEquals(null, treeView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionRemainsOnBranchWhenExpanded() {
        installChildren();
        root.setExpanded(false);
        treeView.getSelectionModel().select(0);
        assertTrue(treeView.getSelectionModel().isSelected(0));
        root.setExpanded(true);
        assertTrue(treeView.getSelectionModel().isSelected(0));
        assertTrue(treeView.getSelectionModel().getSelectedItems().contains(root));
    }
    
    /*********************************************************************
     * Tests for misc                                                    *
     ********************************************************************/
    @Test public void ensureRootIndexIsZeroWhenRootIsShowing() {
        installChildren();
        assertEquals(0, treeView.getRow(root));
    }
    
    @Test public void ensureRootIndexIsNegativeOne1WhenRootIsNotShowing() {
        installChildren();
        treeView.setShowRoot(false);
        assertEquals(-1, treeView.getRow(root));
    }
    
    @Test public void ensureCorrectIndexWhenRootTreeItemHasParent() {
        installChildren();
        treeView.setRoot(child1);
        assertEquals(-1, treeView.getRow(root));
        assertEquals(0, treeView.getRow(child1));
        assertEquals(1, treeView.getRow(child2));
        assertEquals(2, treeView.getRow(child3));
    }
    
    @Test public void ensureCorrectIndexWhenRootTreeItemHasParentAndRootIsNotShowing() {
        installChildren();
        treeView.setRoot(child1);
        treeView.setShowRoot(false);
        
        // despite the fact there are children in this tree, in reality none are
        // visible as the root node has no children (only siblings), and the
        // root node is not visible.
        assertEquals(0, treeView.impl_getTreeItemCount());
        
        assertEquals(-1, treeView.getRow(root));
        assertEquals(-1, treeView.getRow(child1));
        assertEquals(-1, treeView.getRow(child2));
        assertEquals(-1, treeView.getRow(child3));
    }
    
    @Test public void removingLastTest() {
        TreeView tree_view = new TreeView();
        MultipleSelectionModel sm = tree_view.getSelectionModel();
        TreeItem<String> tree_model = new TreeItem<String>("Root");
        TreeItem node = new TreeItem("Data item");
        tree_model.getChildren().add(node);
        tree_view.setRoot(tree_model);
        tree_model.setExpanded(true);
        // select the 'Data item' in the selection model
        sm.select(tree_model.getChildren().get(0));
        // remove the 'Data item' from the root node
        tree_model.getChildren().remove(sm.getSelectedItem());
        // assert the there are no selected items any longer
        assertTrue("items: " + sm.getSelectedItem(), sm.getSelectedItems().isEmpty());
    }
}

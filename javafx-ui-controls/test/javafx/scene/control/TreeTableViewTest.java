/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import java.util.Arrays;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import static javafx.scene.control.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TreeTableViewTest {
    private TreeTableView<String> treeTableView;
    private TreeTableView.TreeTableViewSelectionModel sm;
    private TreeItem<String> root;
    private TreeItem<String> child1;
    private TreeItem<String> child2;
    private TreeItem<String> child3;
    
    @Before public void setup() {
        treeTableView = new TreeTableView<String>();
        sm = treeTableView.getSelectionModel();
    }

    private void installChildren() {
        root = new TreeItem<String>("Root");
        child1 = new TreeItem<String>("Child 1");
        child2 = new TreeItem<String>("Child 2");
        child3 = new TreeItem<String>("Child 3");
        root.setExpanded(true);
        root.getChildren().setAll(child1, child2, child3);
        treeTableView.setRoot(root);
    }
    
    @Test public void ensureCorrectInitialState() {
        installChildren();
        assertEquals(0, treeTableView.getRow(root));
        assertEquals(1, treeTableView.getRow(child1));
        assertEquals(2, treeTableView.getRow(child2));
        assertEquals(3, treeTableView.getRow(child3));
    }
    
    
    
    
    
    
    
    
    /***************************************************************************
     * 
     * 
     * Tests taken from TableViewTest
     * (scroll down further for the TreeViewTests)
     * 
     * 
     **************************************************************************/   

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void noArgConstructorSetsNonNullSelectionModel() {
        assertNotNull(sm);
    }

    @Test public void noArgConstructor_selectedItemIsNull() {
        assertNull(sm.getSelectedItem());
    }

    @Test public void noArgConstructor_selectedIndexIsNegativeOne() {
        assertEquals(-1, sm.getSelectedIndex());
    }

    @Test public void singleArgConstructorSetsNonNullSelectionModel() {
        final TableView<String> b2 = new TableView<String>(FXCollections.observableArrayList("Hi"));
        assertNotNull(b2.getSelectionModel());
    }

    @Test public void singleArgConstructorAllowsNullItems() {
        final TableView<String> b2 = new TableView<String>(null);
        assertNull(b2.getItems());
    }

    @Test public void singleArgConstructorTakesItems() {
        ObservableList<String> items = FXCollections.observableArrayList("Hi");
        final TableView<String> b2 = new TableView<String>(items);
        assertSame(items, b2.getItems());
    }

    @Test public void singleArgConstructor_selectedItemIsNull() {
        final TableView<String> b2 = new TableView<String>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getSelectionModel().getSelectedItem());
    }

    @Test public void singleArgConstructor_selectedIndexIsNegativeOne() {
        final TableView<String> b2 = new TableView<String>(FXCollections.observableArrayList("Hi"));
        assertEquals(-1, b2.getSelectionModel().getSelectedIndex());
    }

    /*********************************************************************
     * Tests for columns                                                 *
     ********************************************************************/

    @Test public void testColumns() {
        TreeTableColumn col1 = new TreeTableColumn();

        assertNotNull(treeTableView.getColumns());
        assertEquals(0, treeTableView.getColumns().size());

        treeTableView.getColumns().add(col1);
        assertEquals(1, treeTableView.getColumns().size());

        treeTableView.getColumns().remove(col1);
        assertEquals(0, treeTableView.getColumns().size());
    }

    @Test public void testVisibleLeafColumns() {
        TreeTableColumn col1 = new TreeTableColumn();

        assertNotNull(treeTableView.getColumns());
        assertEquals(0, treeTableView.getColumns().size());

        treeTableView.getColumns().add(col1);
        assertEquals(1, treeTableView.getVisibleLeafColumns().size());

        treeTableView.getColumns().remove(col1);
        assertEquals(0, treeTableView.getVisibleLeafColumns().size());
    }
    
    @Test public void testSortOrderCleanup() {
//        ObservableList<ObservablePerson> persons = ObservablePerson.createFXPersonList();
        TreeTableView table = new TreeTableView();
        TreeTableColumn<String,String> first = new TreeTableColumn<String,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        TreeTableColumn<String,String> second = new TreeTableColumn<String,String>("second");
        second.setCellValueFactory(new PropertyValueFactory("lastName"));
        treeTableView.getColumns().addAll(first, second);
        treeTableView.getSortOrder().setAll(first, second);
        treeTableView.getColumns().remove(first);
        assertEquals(false, treeTableView.getSortOrder().contains(first));
    } 
    
    
    /*********************************************************************
     * Tests for specific bugs                                           *
     ********************************************************************/
    @Test public void test_rt16019() {
        // RT-16019: NodeMemory TableView tests fail with 
        // IndexOutOfBoundsException (ObservableListWrapper.java:336)
        TableView treeTableView = new TableView();
        for (int i = 0; i < 1000; i++) {
            treeTableView.getItems().add("data " + i);
        }
    }
    
    @Test public void test_rt15793() {
        // ListView/TableView selectedIndex is 0 although the items list is empty
        final TableView tv = new TableView();
        final ObservableList list = FXCollections.observableArrayList();
        tv.setItems(list);
        list.add("toto");
        tv.getSelectionModel().select(0);
        assertEquals(0, tv.getSelectionModel().getSelectedIndex());
        list.remove(0);
        assertEquals(-1, tv.getSelectionModel().getSelectedIndex());
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemAddedAtFocusIndex() {
        final TableView lv = new TableView();
        FocusModel fm = lv.getFocusModel();
        lv.getItems().add("row1");
        fm.focus(0);
        assertTrue(fm.isFocused(0));
        
        lv.getItems().add(0, "row0");
        assertTrue(fm.isFocused(1));
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemAddedBeforeFocusIndex() {
        final TableView lv = new TableView();
        FocusModel fm = lv.getFocusModel();
        lv.getItems().addAll("row1", "row2");
        fm.focus(1);
        assertTrue(fm.isFocused(1));
        assertEquals("row2", fm.getFocusedItem());
        
        lv.getItems().add(1, "row0");
        assertTrue(fm.isFocused(2));
        assertEquals("row2", fm.getFocusedItem());
        assertFalse(fm.isFocused(1));
    }
    
    @Test public void test_rt17522_focusShouldNotMoveWhenItemAddedAfterFocusIndex() {
        final TableView lv = new TableView();
        FocusModel fm = lv.getFocusModel();
        lv.getItems().addAll("row1");
        fm.focus(0);
        assertTrue(fm.isFocused(0));
        assertEquals("row1", fm.getFocusedItem());
        
        lv.getItems().add(1, "row2");
        assertTrue(fm.isFocused(0));
        assertEquals("row1", fm.getFocusedItem());
        assertFalse(fm.isFocused(1));
    }
    
    @Test public void test_rt17522_focusShouldBeResetWhenFocusedItemIsRemoved() {
        final TableView lv = new TableView();
        FocusModel fm = lv.getFocusModel();
        lv.getItems().add("row1");
        fm.focus(0);
        assertTrue(fm.isFocused(0));
        
        lv.getItems().remove("row1");
        assertTrue(fm.getFocusedIndex() == -1);
        assertNull(fm.getFocusedItem());
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemRemovedBeforeFocusIndex() {
        final TableView lv = new TableView();
        FocusModel fm = lv.getFocusModel();
        lv.getItems().addAll("row1", "row2");
        fm.focus(1);
        assertTrue(fm.isFocused(1));
        assertEquals("row2", fm.getFocusedItem());
        
        lv.getItems().remove("row1");
        assertTrue(fm.isFocused(0));
        assertEquals("row2", fm.getFocusedItem());
    }
    
    @Test public void test_rt17522_focusShouldNotMoveWhenItemRemovedAfterFocusIndex() {
        final TableView lv = new TableView();
        FocusModel fm = lv.getFocusModel();
        lv.getItems().addAll("row1", "row2");
        fm.focus(0);
        assertTrue(fm.isFocused(0));
        assertEquals("row1", fm.getFocusedItem());
        
        lv.getItems().remove("row2");
        assertTrue(fm.isFocused(0));
        assertEquals("row1", fm.getFocusedItem());
    }
    
//    @Test public void test_rt18385() {
//        treeTableView.getItems().addAll("row1", "row2", "row3");
//        sm.select(1);
//        treeTableView.getItems().add("Another Row");
//        assertEquals(1, sm.getSelectedIndices().size());
//        assertEquals(1, sm.getSelectedItems().size());
//        assertEquals(1, sm.getSelectedCells().size());
//    }
    
    @Test public void test_rt18339_onlyEditWhenTableViewIsEditable_tableEditableIsFalse_columnEditableIsFalse() {
        TreeTableColumn<String,String> first = new TreeTableColumn<String,String>("first");
        first.setEditable(false);
        treeTableView.getColumns().add(first);
        treeTableView.setEditable(false);
        treeTableView.edit(1, first);
        assertEquals(null, treeTableView.getEditingCell());
    }
    
    @Test public void test_rt18339_onlyEditWhenTableViewIsEditable_tableEditableIsFalse_columnEditableIsTrue() {
        TreeTableColumn<String,String> first = new TreeTableColumn<String,String>("first");
        first.setEditable(true);
        treeTableView.getColumns().add(first);
        treeTableView.setEditable(false);
        treeTableView.edit(1, first);
        assertEquals(null, treeTableView.getEditingCell());
    }
    
    @Test public void test_rt18339_onlyEditWhenTableViewIsEditable_tableEditableIsTrue_columnEditableIsFalse() {
        TreeTableColumn<String,String> first = new TreeTableColumn<String,String>("first");
        first.setEditable(false);
        treeTableView.getColumns().add(first);
        treeTableView.setEditable(true);
        treeTableView.edit(1, first);
        assertEquals(null, treeTableView.getEditingCell());
    }
    
    @Test public void test_rt18339_onlyEditWhenTableViewIsEditable_tableEditableIsTrue_columnEditableIsTrue() {
        TreeTableColumn<String,String> first = new TreeTableColumn<String,String>("first");
        first.setEditable(true);
        treeTableView.getColumns().add(first);
        treeTableView.setEditable(true);
        treeTableView.edit(1, first);
        assertEquals(new TreeTablePosition(treeTableView, 1, first), treeTableView.getEditingCell());
    }
    
//    @Test public void test_rt14451() {
//        treeTableView.getItems().addAll("Apple", "Orange", "Banana");
//        sm.setSelectionMode(SelectionMode.MULTIPLE);
//        sm.selectRange(0, 2); // select from 0 (inclusive) to 2 (exclusive)
//        assertEquals(2, sm.getSelectedIndices().size());
//    }
//    
//    @Test public void test_rt21586() {
//        treeTableView.getItems().setAll("Apple", "Orange", "Banana");
//        treeTableView.getSelectionModel().select(1);
//        assertEquals(1, treeTableView.getSelectionModel().getSelectedIndex());
//        assertEquals("Orange", treeTableView.getSelectionModel().getSelectedItem());
//        
//        treeTableView.getItems().setAll("Kiwifruit", "Pineapple", "Grape");
//        assertEquals(-1, treeTableView.getSelectionModel().getSelectedIndex());
//        assertNull(treeTableView.getSelectionModel().getSelectedItem());
//    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /***************************************************************************
     * 
     * 
     * Tests taken from TreeViewTest
     * 
     * 
     **************************************************************************/   
    
    
    

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void noArgConstructorSetsTheStyleClass() {
        assertStyleClassContains(treeTableView, "tree-table-view");
    }

    @Test public void noArgConstructorSetsNullItems() {
        assertNull(treeTableView.getRoot());
    }

    @Test public void singleArgConstructorSetsTheStyleClass() {
        final TreeTableView<String> b2 = new TreeTableView<String>(new TreeItem<String>("Hi"));
        assertStyleClassContains(b2, "tree-table-view");
    }

    /*********************************************************************
     * Tests for selection model                                         *
     ********************************************************************/

    @Test public void selectionModelCanBeNull() {
        treeTableView.setSelectionModel(null);
        assertNull(treeTableView.getSelectionModel());
    }

    @Test public void selectionModelCanBeBound() {
        TreeTableView.TreeTableViewSelectionModel<String> sm = 
                new TreeTableView.TreeTableViewArrayListSelectionModel<String>(treeTableView);
        ObjectProperty<TreeTableView.TreeTableViewSelectionModel<String>> other = 
                new SimpleObjectProperty<TreeTableView.TreeTableViewSelectionModel<String>>(sm);
        treeTableView.selectionModelProperty().bind(other);
        assertSame(sm, treeTableView.getSelectionModel());
    }

    @Test public void selectionModelCanBeChanged() {
        TreeTableView.TreeTableViewSelectionModel<String> sm = 
                new TreeTableView.TreeTableViewArrayListSelectionModel<String>(treeTableView);
        treeTableView.setSelectionModel(sm);
        assertSame(sm, treeTableView.getSelectionModel());
    }

    @Test public void canSetSelectedItemToAnItemEvenWhenThereAreNoItems() {
        TreeItem<String> element = new TreeItem<String>("I AM A CRAZY RANDOM STRING");
        treeTableView.getSelectionModel().select(element);
        assertEquals(-1, treeTableView.getSelectionModel().getSelectedIndex());
        assertSame(element, treeTableView.getSelectionModel().getSelectedItem());
    }

    @Test public void canSetSelectedItemToAnItemNotInTheDataModel() {
        installChildren();
        TreeItem<String> element = new TreeItem<String>("I AM A CRAZY RANDOM STRING");
        treeTableView.getSelectionModel().select(element);
        assertEquals(-1, treeTableView.getSelectionModel().getSelectedIndex());
        assertSame(element, treeTableView.getSelectionModel().getSelectedItem());
    }

    @Test public void settingTheSelectedItemToAnItemInItemsResultsInTheCorrectSelectedIndex() {
        installChildren();
        treeTableView.getSelectionModel().select(child1);
        assertEquals(1, treeTableView.getSelectionModel().getSelectedIndex());
        assertSame(child1, treeTableView.getSelectionModel().getSelectedItem());
    }

    @Ignore("Not yet supported")
    @Test public void settingTheSelectedItemToANonexistantItemAndThenSettingItemsWhichContainsItResultsInCorrectSelectedIndex() {
        treeTableView.getSelectionModel().select(child1);
        installChildren();
        assertEquals(1, treeTableView.getSelectionModel().getSelectedIndex());
        assertSame(child1, treeTableView.getSelectionModel().getSelectedItem());
    }
    
    @Ignore("Not yet supported")
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex0() {
        installChildren();
        treeTableView.getSelectionModel().select(0);
        treeTableView.setRoot(null);
        assertEquals(-1, treeTableView.getSelectionModel().getSelectedIndex());
        assertEquals(null, treeTableView.getSelectionModel().getSelectedItem());
    }
    
    @Ignore("Not yet supported")
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex2() {
        installChildren();
        treeTableView.getSelectionModel().select(2);
        treeTableView.setRoot(null);
        assertEquals(-1, treeTableView.getSelectionModel().getSelectedIndex());
        assertEquals(null, treeTableView.getSelectionModel().getSelectedItem());
    }
    
    @Ignore("Not yet supported")
    @Test public void ensureSelectedItemRemainsAccurateWhenItemsAreCleared() {
        installChildren();
        treeTableView.getSelectionModel().select(2);
        treeTableView.setRoot(null);
        assertNull(treeTableView.getSelectionModel().getSelectedItem());
        assertEquals(-1, treeTableView.getSelectionModel().getSelectedIndex());
        
        TreeItem<String> newRoot = new TreeItem<String>("New Root");
        TreeItem<String> newChild1 = new TreeItem<String>("New Child 1");
        TreeItem<String> newChild2 = new TreeItem<String>("New Child 2");
        TreeItem<String> newChild3 = new TreeItem<String>("New Child 3");
        newRoot.setExpanded(true);
        newRoot.getChildren().setAll(newChild1, newChild2, newChild3);
        treeTableView.setRoot(root);
        
        treeTableView.getSelectionModel().select(2);
        assertEquals(newChild2, treeTableView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionIsCorrectWhenItemsChange() {
        installChildren();
        treeTableView.getSelectionModel().select(0);
        assertEquals(root, treeTableView.getSelectionModel().getSelectedItem());
        
        treeTableView.setRoot(new TreeItem<String>("New Root"));
        assertEquals(-1, treeTableView.getSelectionModel().getSelectedIndex());
        assertEquals(null, treeTableView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionRemainsOnBranchWhenExpanded() {
        installChildren();
        root.setExpanded(false);
        treeTableView.getSelectionModel().select(0);
        assertTrue(treeTableView.getSelectionModel().isSelected(0));
        root.setExpanded(true);
        assertTrue(treeTableView.getSelectionModel().isSelected(0));
        assertTrue(treeTableView.getSelectionModel().getSelectedItems().contains(root));
    }
    
    /*********************************************************************
     * Tests for misc                                                    *
     ********************************************************************/
    @Test public void ensureRootIndexIsZeroWhenRootIsShowing() {
        installChildren();
        assertEquals(0, treeTableView.getRow(root));
    }
    
    @Test public void ensureRootIndexIsNegativeOneWhenRootIsNotShowing() {
        installChildren();
        treeTableView.setShowRoot(false);
        assertEquals(-1, treeTableView.getRow(root));
    }
    
    @Test public void ensureCorrectIndexWhenRootTreeItemHasParent() {
        installChildren();
        treeTableView.setRoot(child1);
        assertEquals(-1, treeTableView.getRow(root));
        assertEquals(0, treeTableView.getRow(child1));
        assertEquals(1, treeTableView.getRow(child2));
        assertEquals(2, treeTableView.getRow(child3));
    }
    
    @Test public void ensureCorrectIndexWhenRootTreeItemHasParentAndRootIsNotShowing() {
        installChildren();
        treeTableView.setRoot(child1);
        treeTableView.setShowRoot(false);
        
        // despite the fact there are children in this tree, in reality none are
        // visible as the root node has no children (only siblings), and the
        // root node is not visible.
        assertEquals(0, treeTableView.getExpandedItemCount());
        
        assertEquals(-1, treeTableView.getRow(root));
        assertEquals(-1, treeTableView.getRow(child1));
        assertEquals(-1, treeTableView.getRow(child2));
        assertEquals(-1, treeTableView.getRow(child3));
    }
    
    @Test public void ensureCorrectIndexWhenRootTreeItemIsCollapsed() {
        installChildren();
        root.setExpanded(false);
        assertEquals(0, treeTableView.getRow(root));
        
        // note that the indices are still positive, representing what the values
        // would be if this row is visible
        assertEquals(1, treeTableView.getRow(child1));
        assertEquals(2, treeTableView.getRow(child2));
        assertEquals(3, treeTableView.getRow(child3));
    }
    
//    @Test public void removingLastTest() {
//        TreeTableView tree_view = new TreeTableView();
//        MultipleSelectionModel sm = tree_view.getSelectionModel();
//        TreeItem<String> tree_model = new TreeItem<String>("Root");
//        TreeItem node = new TreeItem("Data item");
//        tree_model.getChildren().add(node);
//        tree_view.setRoot(tree_model);
//        tree_model.setExpanded(true);
//        // select the 'Data item' in the selection model
//        sm.select(tree_model.getChildren().get(0));
//        // remove the 'Data item' from the root node
//        tree_model.getChildren().remove(sm.getSelectedItem());
//        // assert the there are no selected items any longer
//        assertTrue("items: " + sm.getSelectedItem(), sm.getSelectedItems().isEmpty());
//    }
    
    /*********************************************************************
     * Tests from bug reports                                            *
     ********************************************************************/  
    @Ignore @Test public void test_rt17112() {
        TreeItem<String> root1 = new TreeItem<String>("Root");
        root1.setExpanded(true);
        addChildren(root1, "child");
        for (TreeItem child : root1.getChildren()) {
            addChildren(child, (String)child.getValue());
            child.setExpanded(true);
        }

        final TreeTableView treeTableView1 = new TreeTableView();
        final MultipleSelectionModel sm = treeTableView1.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        treeTableView1.setRoot(root1);
        
        final TreeItem<String> rt17112_child1 = root1.getChildren().get(1);
        final TreeItem<String> rt17112_child1_0 = rt17112_child1.getChildren().get(0);
        final TreeItem<String> rt17112_child2 = root1.getChildren().get(2);
        
        sm.getSelectedItems().addListener(new InvalidationListener() {
            int count = 0;
            @Override public void invalidated(Observable observable) {
                if (count == 0) {
                    assertEquals(rt17112_child1_0, sm.getSelectedItem());
                    assertEquals(1, sm.getSelectedIndices().size());
                    assertEquals(6, sm.getSelectedIndex());
                    assertTrue(treeTableView1.getFocusModel().isFocused(6));
                } else if (count == 1) {
                    assertEquals(rt17112_child1, sm.getSelectedItem());
                    assertFalse(sm.getSelectedItems().contains(rt17112_child2));
                    assertEquals(1, sm.getSelectedIndices().size());
                    assertTrue(treeTableView1.getFocusModel().isFocused(5));
                }
                count++;
            }
        });
        
        // this triggers the first callback above, so that count == 0
        sm.select(rt17112_child1_0);

        // this triggers the second callback above, so that count == 1
        rt17112_child1.setExpanded(false);
    }
    private void addChildren(TreeItem parent, String name) {
        for (int i=0; i<3; i++) {
            TreeItem<String> ti = new TreeItem<String>(name+"-"+i);
            parent.getChildren().add(ti);
        }
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemAddedAtFocusIndex_1() {
        installChildren();
        FocusModel fm = treeTableView.getFocusModel();
        fm.focus(1);    // focus on child1
        assertTrue(fm.isFocused(1));
        assertEquals(child1, fm.getFocusedItem());
        
        TreeItem child0 = new TreeItem("child0");
        root.getChildren().add(0, child0);  // 0th index == position of child1 in root
        
        assertEquals(child1, fm.getFocusedItem());
        assertTrue(fm.isFocused(2));
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemAddedBeforeFocusIndex_1() {
        installChildren();
        FocusModel fm = treeTableView.getFocusModel();
        fm.focus(1);    // focus on child1
        assertTrue(fm.isFocused(1));
        
        TreeItem child0 = new TreeItem("child0");
        root.getChildren().add(0, child0);
        assertTrue("Focused index: " + fm.getFocusedIndex(), fm.isFocused(2));
    }
    
    @Test public void test_rt17522_focusShouldNotMoveWhenItemAddedAfterFocusIndex_1() {
        installChildren();
        FocusModel fm = treeTableView.getFocusModel();
        fm.focus(1);    // focus on child1
        assertTrue(fm.isFocused(1));
        
        TreeItem child4 = new TreeItem("child4");
        root.getChildren().add(3, child4);
        assertTrue("Focused index: " + fm.getFocusedIndex(), fm.isFocused(1));
    }
    
    @Test public void test_rt17522_focusShouldBeResetWhenFocusedItemIsRemoved_1() {
        installChildren();
        FocusModel fm = treeTableView.getFocusModel();
        fm.focus(1);
        assertTrue(fm.isFocused(1));
        
        root.getChildren().remove(child1);
        assertEquals(-1, fm.getFocusedIndex());
        assertNull(fm.getFocusedItem());
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemRemovedBeforeFocusIndex_1() {
        installChildren();
        FocusModel fm = treeTableView.getFocusModel();
        fm.focus(2);
        assertTrue(fm.isFocused(2));
        
        root.getChildren().remove(child1);
        assertTrue(fm.isFocused(1));
        assertEquals(child2, fm.getFocusedItem());
    }

//    This test fails as, in TreeTableView FocusModel, we do not know the index of the
//    removed tree items, which means we don't know whether they existed before
//    or after the focused item.
//    @Test public void test_rt17522_focusShouldNotMoveWhenItemRemovedAfterFocusIndex() {
//        installChildren();
//        FocusModel fm = treeTableView.getFocusModel();
//        fm.focus(1);
//        assertTrue(fm.isFocused(1));
//        
//        root.getChildren().remove(child3);
//        assertTrue("Focused index: " + fm.getFocusedIndex(), fm.isFocused(1));
//        assertEquals(child1, fm.getFocusedItem());
//    }
    
    @Test public void test_rt18385() {
        installChildren();
//        table.getItems().addAll("row1", "row2", "row3");
        treeTableView.getSelectionModel().select(1);
        treeTableView.getRoot().getChildren().add(new TreeItem("Another Row"));
        assertEquals(1, treeTableView.getSelectionModel().getSelectedIndices().size());
        assertEquals(1, treeTableView.getSelectionModel().getSelectedItems().size());
    }
    
    @Test public void test_rt18339_onlyEditWhenTreeTableViewIsEditable_editableIsFalse() {
        treeTableView.setEditable(false);
        treeTableView.edit(root);
        assertEquals(null, treeTableView.getEditingItem());
    }
    
    @Test public void test_rt18339_onlyEditWhenTreeTableViewIsEditable_editableIsTrue() {
        treeTableView.setEditable(true);
        treeTableView.edit(root);
        assertEquals(root, treeTableView.getEditingItem());
    }
    
    @Test public void test_rt14451() {
        installChildren();
        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeTableView.getSelectionModel().selectRange(0, 2); // select from 0 (inclusive) to 2 (exclusive)
        assertEquals(2, treeTableView.getSelectionModel().getSelectedIndices().size());
    }
    
    @Test public void test_rt21586() {
        installChildren();
        treeTableView.getSelectionModel().select(1);
        assertEquals(1, treeTableView.getSelectionModel().getSelectedIndex());
        assertEquals(child1, treeTableView.getSelectionModel().getSelectedItem());
        
        TreeItem root = new TreeItem<String>("New Root");
        TreeItem child1 = new TreeItem<String>("New Child 1");
        TreeItem child2 = new TreeItem<String>("New Child 2");
        TreeItem child3 = new TreeItem<String>("New Child 3");
        root.setExpanded(true);
        root.getChildren().setAll(child1, child2, child3);
        treeTableView.setRoot(root);
        assertEquals(-1, treeTableView.getSelectionModel().getSelectedIndex());
        assertNull(treeTableView.getSelectionModel().getSelectedItem());
    }
    

}

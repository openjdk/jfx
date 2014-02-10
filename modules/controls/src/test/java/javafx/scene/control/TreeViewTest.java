/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.runtime.VersionInfo;
import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import com.sun.javafx.scene.control.skin.VirtualScrollBar;
import com.sun.javafx.scene.control.test.Employee;
import com.sun.javafx.scene.control.test.Person;
import com.sun.javafx.scene.control.test.RT_22463_Person;
import com.sun.javafx.tk.Toolkit;

import java.util.*;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Callback;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TreeViewTest {
    private TreeView<String> treeView;
    private MultipleSelectionModel<TreeItem<String>> sm;
    private FocusModel<TreeItem<String>> fm;
    
    // sample data #1
    private TreeItem<String> root;
    private TreeItem<String> child1;
    private TreeItem<String> child2;
    private TreeItem<String> child3;
    
    // sample data #1
    private TreeItem<String> myCompanyRootNode;
        private TreeItem<String> salesDepartment;
            private TreeItem<String> ethanWilliams;
            private TreeItem<String> emmaJones;
            private TreeItem<String> michaelBrown;
            private TreeItem<String> annaBlack;
            private TreeItem<String> rodgerYork;
            private TreeItem<String> susanCollins;

        private TreeItem<String> itSupport;
            private TreeItem<String> mikeGraham;
            private TreeItem<String> judyMayer;
            private TreeItem<String> gregorySmith;
            
    private String debug() {
        StringBuilder sb = new StringBuilder("Selected Indices: [");
        
        List<Integer> indices = sm.getSelectedIndices();
        for (Integer index : indices) {
            sb.append(index);
            sb.append(", ");
        }
        
        sb.append("] \nFocus: " + fm.getFocusedIndex());
//        sb.append(" \nAnchor: " + getAnchor());
        return sb.toString();
    }
    
    @Before public void setup() {
        treeView = new TreeView<String>();
        sm = treeView.getSelectionModel();
        fm = treeView.getFocusModel();
        
        // build sample data #2, even though it may not be used...
        myCompanyRootNode = new TreeItem<String>("MyCompany Human Resources");
        salesDepartment = new TreeItem<String>("Sales Department");
            ethanWilliams = new TreeItem<String>("Ethan Williams");
            emmaJones = new TreeItem<String>("Emma Jones");
            michaelBrown = new TreeItem<String>("Michael Brown");
            annaBlack = new TreeItem<String>("Anna Black");
            rodgerYork = new TreeItem<String>("Rodger York");
            susanCollins = new TreeItem<String>("Susan Collins");

        itSupport = new TreeItem<String>("IT Support");
            mikeGraham = new TreeItem<String>("Mike Graham");
            judyMayer = new TreeItem<String>("Judy Mayer");
            gregorySmith = new TreeItem<String>("Gregory Smith");
            
        myCompanyRootNode.getChildren().setAll(
            salesDepartment,
            itSupport
        );
        salesDepartment.getChildren().setAll(
            ethanWilliams,
            emmaJones,
            michaelBrown, 
            annaBlack,
            rodgerYork,
            susanCollins
        );
        itSupport.getChildren().setAll(
            mikeGraham,
            judyMayer,
            gregorySmith
        );
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
    
    @Test public void ensureRootIndexIsNegativeOneWhenRootIsNotShowing() {
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
        assertEquals(0, treeView.getExpandedItemCount());
        
        assertEquals(-1, treeView.getRow(root));
        assertEquals(-1, treeView.getRow(child1));
        assertEquals(-1, treeView.getRow(child2));
        assertEquals(-1, treeView.getRow(child3));
    }
    
    @Test public void ensureCorrectIndexWhenRootTreeItemIsCollapsed() {
        installChildren();
        root.setExpanded(false);
        assertEquals(0, treeView.getRow(root));
        
        // note that the indices are still positive, representing what the values
        // would be if this row is visible
        assertEquals(1, treeView.getRow(child1));
        assertEquals(2, treeView.getRow(child2));
        assertEquals(3, treeView.getRow(child3));
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

        // Previously the selection was cleared, but this was changed to instead
        // move the selection upwards.
        // assert the there are no selected items any longer
        // assertTrue("items: " + sm.getSelectedItem(), sm.getSelectedItems().isEmpty());
        assertEquals(tree_model, sm.getSelectedItem());
    }
    
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

        final TreeView treeView1 = new TreeView();
        final MultipleSelectionModel sm = treeView1.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        treeView1.setRoot(root1);
        
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
                    assertTrue(treeView1.getFocusModel().isFocused(6));
                } else if (count == 1) {
                    assertEquals(rt17112_child1, sm.getSelectedItem());
                    assertFalse(sm.getSelectedItems().contains(rt17112_child2));
                    assertEquals(1, sm.getSelectedIndices().size());
                    assertTrue(treeView1.getFocusModel().isFocused(5));
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
    
    @Test public void test_rt17522_focusShouldMoveWhenItemAddedAtFocusIndex() {
        installChildren();
        FocusModel fm = treeView.getFocusModel();
        fm.focus(1);    // focus on child1
        assertTrue(fm.isFocused(1));
        assertEquals(child1, fm.getFocusedItem());
        
        TreeItem child0 = new TreeItem("child0");
        root.getChildren().add(0, child0);  // 0th index == position of child1 in root
        
        assertEquals(child1, fm.getFocusedItem());
        assertTrue(fm.isFocused(2));
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemAddedBeforeFocusIndex() {
        installChildren();
        FocusModel fm = treeView.getFocusModel();
        fm.focus(1);    // focus on child1
        assertTrue(fm.isFocused(1));
        
        TreeItem child0 = new TreeItem("child0");
        root.getChildren().add(0, child0);
        assertTrue("Focused index: " + fm.getFocusedIndex(), fm.isFocused(2));
    }
    
    @Test public void test_rt17522_focusShouldNotMoveWhenItemAddedAfterFocusIndex() {
        installChildren();
        FocusModel fm = treeView.getFocusModel();
        fm.focus(1);    // focus on child1
        assertTrue(fm.isFocused(1));
        
        TreeItem child4 = new TreeItem("child4");
        root.getChildren().add(3, child4);
        assertTrue("Focused index: " + fm.getFocusedIndex(), fm.isFocused(1));
    }
    
    @Test public void test_rt17522_focusShouldBeResetWhenFocusedItemIsRemoved() {
        installChildren();
        FocusModel fm = treeView.getFocusModel();
        fm.focus(1);
        assertTrue(fm.isFocused(1));
        
        root.getChildren().remove(child1);
        assertEquals(-1, fm.getFocusedIndex());
        assertNull(fm.getFocusedItem());
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemRemovedBeforeFocusIndex() {
        installChildren();
        FocusModel fm = treeView.getFocusModel();
        fm.focus(2);
        assertTrue(fm.isFocused(2));
        
        root.getChildren().remove(child1);
        assertTrue(fm.isFocused(1));
        assertEquals(child2, fm.getFocusedItem());
    }

//    This test fails as, in TreeView FocusModel, we do not know the index of the
//    removed tree items, which means we don't know whether they existed before
//    or after the focused item.
//    @Test public void test_rt17522_focusShouldNotMoveWhenItemRemovedAfterFocusIndex() {
//        installChildren();
//        FocusModel fm = treeView.getFocusModel();
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
        treeView.getSelectionModel().select(1);
        treeView.getRoot().getChildren().add(new TreeItem("Another Row"));
        assertEquals(1, treeView.getSelectionModel().getSelectedIndices().size());
        assertEquals(1, treeView.getSelectionModel().getSelectedItems().size());
    }
    
    @Test public void test_rt18339_onlyEditWhenTreeViewIsEditable_editableIsFalse() {
        treeView.setEditable(false);
        treeView.edit(root);
        assertEquals(null, treeView.getEditingItem());
    }
    
    @Test public void test_rt18339_onlyEditWhenTreeViewIsEditable_editableIsTrue() {
        treeView.setEditable(true);
        treeView.edit(root);
        assertEquals(root, treeView.getEditingItem());
    }
    
    @Test public void test_rt14451() {
        installChildren();
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.getSelectionModel().selectRange(0, 2); // select from 0 (inclusive) to 2 (exclusive)
        assertEquals(2, treeView.getSelectionModel().getSelectedIndices().size());
    }
    
    @Test public void test_rt21586() {
        installChildren();
        treeView.getSelectionModel().select(1);
        assertEquals(1, treeView.getSelectionModel().getSelectedIndex());
        assertEquals(child1, treeView.getSelectionModel().getSelectedItem());
        
        TreeItem root = new TreeItem<String>("New Root");
        TreeItem child1 = new TreeItem<String>("New Child 1");
        TreeItem child2 = new TreeItem<String>("New Child 2");
        TreeItem child3 = new TreeItem<String>("New Child 3");
        root.setExpanded(true);
        root.getChildren().setAll(child1, child2, child3);
        treeView.setRoot(root);
        assertEquals(-1, treeView.getSelectionModel().getSelectedIndex());
        assertNull(treeView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt27181() {
        myCompanyRootNode.setExpanded(true);
        treeView.setRoot(myCompanyRootNode);
        
        // start test
        salesDepartment.setExpanded(true);
        treeView.getSelectionModel().select(salesDepartment);
        
        assertEquals(1, treeView.getFocusModel().getFocusedIndex());
        itSupport.setExpanded(true);
        assertEquals(1, treeView.getFocusModel().getFocusedIndex());
    }
    
    @Test public void test_rt27185() {
        myCompanyRootNode.setExpanded(true);
        treeView.setRoot(myCompanyRootNode);
        
        // start test
        itSupport.setExpanded(true);
        treeView.getSelectionModel().select(mikeGraham);
        
        assertEquals(mikeGraham, treeView.getFocusModel().getFocusedItem());
        salesDepartment.setExpanded(true);
        assertEquals(mikeGraham, treeView.getFocusModel().getFocusedItem());
    }
    
    @Ignore("Bug hasn't been fixed yet")
    @Test public void test_rt28114() {
        myCompanyRootNode.setExpanded(true);
        treeView.setRoot(myCompanyRootNode);
        
        // start test
        itSupport.setExpanded(true);
        treeView.getSelectionModel().select(itSupport);
        assertEquals(itSupport, treeView.getFocusModel().getFocusedItem());
        assertEquals(itSupport, treeView.getSelectionModel().getSelectedItem());
        assertTrue(! itSupport.isLeaf());
        assertTrue(itSupport.isExpanded());
        
        itSupport.getChildren().remove(mikeGraham);
        assertEquals(itSupport, treeView.getFocusModel().getFocusedItem());
        assertEquals(itSupport, treeView.getSelectionModel().getSelectedItem());
        assertTrue(itSupport.isLeaf());
        assertTrue(!itSupport.isExpanded());
    }
    
    @Test public void test_rt27820_1() {
        TreeItem root = new TreeItem("root");
        root.setExpanded(true);
        TreeItem child = new TreeItem("child");
        root.getChildren().add(child);
        treeView.setRoot(root);
        
        treeView.getSelectionModel().select(0);
        assertEquals(1, treeView.getSelectionModel().getSelectedItems().size());
        assertEquals(root, treeView.getSelectionModel().getSelectedItem());
        
        treeView.setRoot(null);
        assertEquals(0, treeView.getSelectionModel().getSelectedItems().size());
        assertNull(treeView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt27820_2() {
        TreeItem root = new TreeItem("root");
        root.setExpanded(true);
        TreeItem child = new TreeItem("child");
        root.getChildren().add(child);
        treeView.setRoot(root);
        
        treeView.getSelectionModel().select(1);
        assertEquals(1, treeView.getSelectionModel().getSelectedItems().size());
        assertEquals(child, treeView.getSelectionModel().getSelectedItem());
        
        treeView.setRoot(null);
        assertEquals(0, treeView.getSelectionModel().getSelectedItems().size());
        assertNull(treeView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt28390() {
        // There should be no NPE when a TreeView is shown and the disclosure
        // node is null in a TreeCell
        TreeItem root = new TreeItem("root");
        treeView.setRoot(root);
        
        // install a custom cell factory that forces the disclosure node to be
        // null (because by default a null disclosure node will be replaced by
        // a non-null one).
        treeView.setCellFactory(new Callback() {
            @Override public Object call(Object p) {
                TreeCell treeCell = new TreeCell() {
                    {
                        disclosureNodeProperty().addListener(new ChangeListener() {
                            @Override public void changed(ObservableValue ov, Object t, Object t1) {
                                setDisclosureNode(null);
                            }
                        });
                    }
                    
                    @Override protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(item == null ? "" : item.toString());
                    }
                };
                treeCell.setDisclosureNode(null);
                return treeCell;
            }
        });
        
        try {
            Group group = new Group();
            group.getChildren().setAll(treeView);
            Scene scene = new Scene(group);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
        } catch (NullPointerException e) {
            System.out.println("A null disclosure node is valid, so we shouldn't have an NPE here.");
            e.printStackTrace();
            assertTrue(false);
        }
    }
    
    @Test public void test_rt28534() {
        TreeItem root = new TreeItem("root");
        root.getChildren().setAll(
                new TreeItem(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem(new Person("Michael", "Brown", "michael.brown@example.com")));
        root.setExpanded(true);
        
        TreeView<Person> tree = new TreeView<Person>(root);
        
        VirtualFlowTestUtils.assertRowsNotEmpty(tree, 0, 6); // rows 0 - 6 should be filled
        VirtualFlowTestUtils.assertRowsEmpty(tree, 6, -1); // rows 6+ should be empty
        
        // now we replace the data and expect the cells that have no data
        // to be empty
        root.getChildren().setAll(
                new TreeItem(new Person("*_*Emma", "Jones", "emma.jones@example.com")),
                new TreeItem(new Person("_Michael", "Brown", "michael.brown@example.com")));
        
        VirtualFlowTestUtils.assertRowsNotEmpty(tree, 0, 3); // rows 0 - 3 should be filled
        VirtualFlowTestUtils.assertRowsEmpty(tree, 3, -1); // rows 3+ should be empty
    }

    @Test public void test_rt28556() {
        List<Employee> employees = Arrays.<Employee>asList(
            new Employee("Ethan Williams", "Sales Department"),
            new Employee("Emma Jones", "Sales Department"),
            new Employee("Michael Brown", "Sales Department"),
            new Employee("Anna Black", "Sales Department"),
            new Employee("Rodger York", "Sales Department"),
            new Employee("Susan Collins", "Sales Department"),
            new Employee("Mike Graham", "IT Support"),
            new Employee("Judy Mayer", "IT Support"),
            new Employee("Gregory Smith", "IT Support"),
            new Employee("Jacob Smith", "Accounts Department"),
            new Employee("Isabella Johnson", "Accounts Department"));
    
        TreeItem<String> rootNode = new TreeItem<String>("MyCompany Human Resources");
        rootNode.setExpanded(true);
        
        List<TreeItem<String>> nodeList = FXCollections.observableArrayList();
        for (Employee employee : employees) {
            nodeList.add(new TreeItem<String>(employee.getName()));
        }
        rootNode.getChildren().setAll(nodeList);

        TreeView<String> treeView = new TreeView<String>(rootNode);
        
        final double indent = PlatformImpl.isCaspian() ? 31 : 
                        PlatformImpl.isModena()  ? 35 :
                        0;
        
        // ensure all children of the root node have the correct indentation 
        // before the sort occurs
        VirtualFlowTestUtils.assertLayoutX(treeView, 1, 11, indent);
        for (TreeItem<String> children : rootNode.getChildren()) {
            assertEquals(rootNode, children.getParent());
        }
        
        // run sort
        Collections.sort(rootNode.getChildren(), new Comparator<TreeItem<String>>() {
            @Override public int compare(TreeItem<String> o1, TreeItem<String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        
        // ensure the same indentation exists after the sort (which is where the
        // bug is - it drops down to 21.0px indentation when it shouldn't).
        VirtualFlowTestUtils.assertLayoutX(treeView, 1, 11, indent);
        for (TreeItem<String> children : rootNode.getChildren()) {
            assertEquals(rootNode, children.getParent());
        }
    }
    
    @Test public void test_rt22463() {
        RT_22463_Person rootPerson = new RT_22463_Person();
        rootPerson.setName("Root");
        TreeItem<RT_22463_Person> root = new TreeItem<RT_22463_Person>(rootPerson);
        root.setExpanded(true);
        
        final TreeView<RT_22463_Person> tree = new TreeView<RT_22463_Person>();
        tree.setRoot(root);
        
        // before the change things display fine
        RT_22463_Person p1 = new RT_22463_Person();
        p1.setId(1l);
        p1.setName("name1");
        RT_22463_Person p2 = new RT_22463_Person();
        p2.setId(2l);
        p2.setName("name2");
        root.getChildren().addAll(
                new TreeItem<RT_22463_Person>(p1), 
                new TreeItem<RT_22463_Person>(p2));
        VirtualFlowTestUtils.assertCellTextEquals(tree, 1, "name1");
        VirtualFlowTestUtils.assertCellTextEquals(tree, 2, "name2");
        
        // now we change the persons but they are still equal as the ID's don't
        // change - but the items list is cleared so the cells should update
        RT_22463_Person new_p1 = new RT_22463_Person();
        new_p1.setId(1l);
        new_p1.setName("updated name1");
        RT_22463_Person new_p2 = new RT_22463_Person();
        new_p2.setId(2l);
        new_p2.setName("updated name2");
        root.getChildren().clear();
        root.getChildren().setAll(
                new TreeItem<RT_22463_Person>(new_p1), 
                new TreeItem<RT_22463_Person>(new_p2));
        VirtualFlowTestUtils.assertCellTextEquals(tree, 1, "updated name1");
        VirtualFlowTestUtils.assertCellTextEquals(tree, 2, "updated name2");
    }
    
    @Test public void test_rt28637() {
        TreeItem<String> s1, s2, s3, s4;
        ObservableList<TreeItem<String>> items = FXCollections.observableArrayList(
                s1 = new TreeItem<String>("String1"), 
                s2 = new TreeItem<String>("String2"), 
                s3 = new TreeItem<String>("String3"), 
                s4 = new TreeItem<String>("String4"));
        
        final TreeView<String> treeView = new TreeView<String>();
        
        TreeItem<String> root = new TreeItem<String>("Root");
        root.setExpanded(true);
        treeView.setRoot(root);
        treeView.setShowRoot(false);
        root.getChildren().addAll(items);
        
        treeView.getSelectionModel().select(0);
        assertEquals((Object)s1, treeView.getSelectionModel().getSelectedItem());
        assertEquals((Object)s1, treeView.getSelectionModel().getSelectedItems().get(0));
        assertEquals(0, treeView.getSelectionModel().getSelectedIndex());
        
        root.getChildren().remove(treeView.getSelectionModel().getSelectedItem());
        assertEquals((Object)s2, treeView.getSelectionModel().getSelectedItem());
        assertEquals((Object)s2, treeView.getSelectionModel().getSelectedItems().get(0));
        assertEquals(0, treeView.getSelectionModel().getSelectedIndex());
    }
    
    @Ignore("Test passes from within IDE but not when run from command line. Needs more investigation.")
    @Test public void test_rt28678() {
        TreeItem<String> s1, s2, s3, s4;
        ObservableList<TreeItem<String>> items = FXCollections.observableArrayList(
                s1 = new TreeItem<String>("String1"), 
                s2 = new TreeItem<String>("String2"), 
                s3 = new TreeItem<String>("String3"), 
                s4 = new TreeItem<String>("String4"));
        
        final TreeView<String> treeView = new TreeView<String>();
        
        TreeItem<String> root = new TreeItem<String>("Root");
        root.setExpanded(true);
        treeView.setRoot(root);
        treeView.setShowRoot(false);
        root.getChildren().addAll(items);
        
        Node graphic = new Circle(6, Color.RED);
        
        assertNull(s2.getGraphic());
        TreeCell s2Cell = (TreeCell) VirtualFlowTestUtils.getCell(treeView, 1);
        assertNull(s2Cell.getGraphic());
        
        s2.setGraphic(graphic);
        Toolkit.getToolkit().firePulse();
                
        assertEquals(graphic, s2.getGraphic());
        assertEquals(graphic, s2Cell.getGraphic());
    }
    
    @Test public void test_rt29390() {
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
                new TreeItem<Person>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<Person>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<Person>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<Person>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<Person>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<Person>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<Person>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<Person>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<Person>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<Person>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<Person>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<Person>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<Person>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<Person>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<Person>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<Person>(new Person("Emma", "Jones", "emma.jones@example.com")
        ));
                
        TreeView<Person> treeView = new TreeView<>();
        treeView.setMaxHeight(50);
        treeView.setPrefHeight(50);
        
        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        treeView.setRoot(root);
        treeView.setShowRoot(false);
        root.getChildren().setAll(persons);
        
        Toolkit.getToolkit().firePulse();
        
        // we want the vertical scrollbar
        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowVerticalScrollbar(treeView);
        
        assertNotNull(scrollBar);
        assertTrue(scrollBar.isVisible());
        assertTrue(scrollBar.getVisibleAmount() > 0.0);
        assertTrue(scrollBar.getVisibleAmount() < 1.0);
        
        // this next test is likely to be brittle, but we'll see...If it is the
        // cause of failure then it can be commented out
        assertEquals(0.125, scrollBar.getVisibleAmount(), 0.0);
    }
    
    @Test public void test_rt27180_collapseBranch_childSelected_singleSelection() {
        sm.setSelectionMode(SelectionMode.SINGLE);
        
        treeView.setRoot(myCompanyRootNode);
        myCompanyRootNode.setExpanded(true);
        salesDepartment.setExpanded(true);
        itSupport.setExpanded(true);
        sm.select(2);                   // ethanWilliams
        assertFalse(sm.isSelected(1));  // salesDepartment
        assertTrue(sm.isSelected(2));   // ethanWilliams
        assertTrue(treeView.getFocusModel().isFocused(2));
        assertEquals(1, sm.getSelectedIndices().size());
        
        // now collapse the salesDepartment, selection should
        // not jump down to the itSupport people
        salesDepartment.setExpanded(false);
        assertTrue(sm.isSelected(1));   // salesDepartment
        assertTrue(treeView.getFocusModel().isFocused(1));
        assertEquals(1, sm.getSelectedIndices().size());
    }
    
    @Test public void test_rt27180_collapseBranch_laterSiblingSelected_singleSelection() {
        sm.setSelectionMode(SelectionMode.SINGLE);
        
        treeView.setRoot(myCompanyRootNode);
        myCompanyRootNode.setExpanded(true);
        salesDepartment.setExpanded(true);
        itSupport.setExpanded(true);
        sm.select(8);                   // itSupport
        assertFalse(sm.isSelected(1));  // salesDepartment
        assertTrue(sm.isSelected(8));   // itSupport
        assertTrue(treeView.getFocusModel().isFocused(8));
        assertEquals(1, sm.getSelectedIndices().size());
        
        salesDepartment.setExpanded(false);
        assertTrue(sm.isSelected(2));   // itSupport
        assertTrue(treeView.getFocusModel().isFocused(2));
        assertEquals(1, sm.getSelectedIndices().size());
    }
    
    @Test public void test_rt27180_collapseBranch_laterSiblingAndChildrenSelected() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        treeView.setRoot(myCompanyRootNode);
        myCompanyRootNode.setExpanded(true);
        salesDepartment.setExpanded(true);
        itSupport.setExpanded(true);
        sm.selectIndices(8, 9, 10);     // itSupport, and two people
        assertFalse(sm.isSelected(1));  // salesDepartment
        assertTrue(sm.isSelected(8));   // itSupport
        assertTrue(sm.isSelected(9));   // mikeGraham
        assertTrue(sm.isSelected(10));  // judyMayer
        assertTrue(treeView.getFocusModel().isFocused(10));
        assertEquals(3, sm.getSelectedIndices().size());
        
        salesDepartment.setExpanded(false);
        assertTrue(sm.isSelected(2));   // itSupport
        assertTrue(sm.isSelected(3));   // mikeGraham
        assertTrue(sm.isSelected(4));   // judyMayer
        assertTrue(treeView.getFocusModel().isFocused(4));
        assertEquals(3, sm.getSelectedIndices().size());
    }
    
    @Test public void test_rt27180_expandBranch_laterSiblingSelected_singleSelection() {
        sm.setSelectionMode(SelectionMode.SINGLE);
        
        treeView.setRoot(myCompanyRootNode);
        myCompanyRootNode.setExpanded(true);
        salesDepartment.setExpanded(false);
        itSupport.setExpanded(true);
        sm.select(2);                   // itSupport
        assertFalse(sm.isSelected(1));  // salesDepartment
        assertTrue(sm.isSelected(2));   // itSupport
        assertTrue(treeView.getFocusModel().isFocused(2));
        assertEquals(1, sm.getSelectedIndices().size());
        
        salesDepartment.setExpanded(true);
        assertTrue(sm.isSelected(8));   // itSupport
        assertTrue(treeView.getFocusModel().isFocused(8));
        assertEquals(1, sm.getSelectedIndices().size());
    }
    
    @Test public void test_rt27180_expandBranch_laterSiblingAndChildrenSelected() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        treeView.setRoot(myCompanyRootNode);
        myCompanyRootNode.setExpanded(true);
        salesDepartment.setExpanded(false);
        itSupport.setExpanded(true);
        sm.selectIndices(2,3,4);     // itSupport, and two people
        assertFalse(sm.isSelected(1));  // salesDepartment
        assertTrue(sm.isSelected(2));   // itSupport
        assertTrue(sm.isSelected(3));   // mikeGraham
        assertTrue(sm.isSelected(4));  // judyMayer
        assertTrue(treeView.getFocusModel().isFocused(4));
        assertEquals(3, sm.getSelectedIndices().size());
        
        salesDepartment.setExpanded(true);
        assertTrue(sm.isSelected(8));   // itSupport
        assertTrue(sm.isSelected(9));   // mikeGraham
        assertTrue(sm.isSelected(10));   // judyMayer
        assertTrue(treeView.getFocusModel().isFocused(10));
        assertEquals(3, sm.getSelectedIndices().size());
    }

    @Test public void test_rt30400() {
        // create a treeview that'll render cells using the check box cell factory
        TreeItem<String> rootItem = new TreeItem<>("root");
        treeView.setRoot(rootItem);
        treeView.setMinHeight(100);
        treeView.setPrefHeight(100);
        treeView.setCellFactory(
                CheckBoxTreeCell.forTreeView(
                        new Callback<TreeItem<String>, ObservableValue<Boolean>>() {
                            public javafx.beans.value.ObservableValue<Boolean> call(TreeItem<String> param) {
                                return new ReadOnlyBooleanWrapper(true);
                            }
                        }));

        // because only the first row has data, all other rows should be
        // empty (and not contain check boxes - we just check the first four here)
        VirtualFlowTestUtils.assertRowsNotEmpty(treeView, 0, 1);
        VirtualFlowTestUtils.assertCellNotEmpty(VirtualFlowTestUtils.getCell(treeView, 0));
        VirtualFlowTestUtils.assertCellEmpty(VirtualFlowTestUtils.getCell(treeView, 1));
        VirtualFlowTestUtils.assertCellEmpty(VirtualFlowTestUtils.getCell(treeView, 2));
        VirtualFlowTestUtils.assertCellEmpty(VirtualFlowTestUtils.getCell(treeView, 3));
    }

    @Test public void test_rt31165() {
        installChildren();
        treeView.setEditable(true);
        treeView.setCellFactory(TextFieldTreeCell.forTreeView());

        IndexedCell cell = VirtualFlowTestUtils.getCell(treeView, 1);
        assertEquals(child1.getValue(), cell.getText());
        assertFalse(cell.isEditing());

        treeView.edit(child1);

        assertEquals(child1, treeView.getEditingItem());
        assertTrue(cell.isEditing());

        VirtualFlowTestUtils.getVirtualFlow(treeView).requestLayout();
        Toolkit.getToolkit().firePulse();

        assertEquals(child1, treeView.getEditingItem());
        assertTrue(cell.isEditing());
    }

    @Test public void test_rt31404() {
        installChildren();

        IndexedCell cell = VirtualFlowTestUtils.getCell(treeView, 0);
        assertEquals("Root", cell.getText());

        treeView.setShowRoot(false);
        assertEquals("Child 1", cell.getText());
    }

    @Test public void test_rt31471() {
        installChildren();

        IndexedCell cell = VirtualFlowTestUtils.getCell(treeView, 0);
        assertEquals("Root", cell.getItem());

        treeView.setFixedCellSize(50);

        VirtualFlowTestUtils.getVirtualFlow(treeView).requestLayout();
        Toolkit.getToolkit().firePulse();

        assertEquals("Root", cell.getItem());
        assertEquals(50, cell.getHeight(), 0.00);
    }

    private int rt_31200_count = 0;
    @Test public void test_rt_31200_tableRow() {
        installChildren();
        treeView.setCellFactory(new Callback<TreeView<String>, TreeCell<String>>() {
            @Override
            public TreeCell<String> call(TreeView<String> param) {
                return new TreeCell<String>() {
                    ImageView view = new ImageView();
                    { setGraphic(view); };

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        if (getItem() == null ? item == null : getItem().equals(item)) {
                            rt_31200_count++;
                        }
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            view.setImage(null);
                            setText(null);
                        } else {
                            setText(item.toString());
                        }
                    }
                };
            }
        });

        StageLoader sl = new StageLoader(treeView);

        assertEquals(24, rt_31200_count);

        // resize the stage
        sl.getStage().setHeight(250);
        Toolkit.getToolkit().firePulse();
        sl.getStage().setHeight(50);
        Toolkit.getToolkit().firePulse();
        assertEquals(24, rt_31200_count);
    }

    @Test public void test_rt_30484() {
        installChildren();
        treeView.setCellFactory(new Callback<TreeView<String>, TreeCell<String>>() {
            @Override public TreeCell<String> call(TreeView<String> param) {
                return new TreeCell<String>() {
                    Rectangle graphic = new Rectangle(10, 10, Color.RED);
                    { setGraphic(graphic); };

                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            graphic.setVisible(false);
                            setText(null);
                        } else {
                            graphic.setVisible(true);
                            setText(item);
                        }
                    }
                };
            }
        });

        // First two four have content, so the graphic should show.
        // All other rows have no content, so graphic should not show.

        VirtualFlowTestUtils.assertGraphicIsVisible(treeView, 0);
        VirtualFlowTestUtils.assertGraphicIsVisible(treeView, 1);
        VirtualFlowTestUtils.assertGraphicIsVisible(treeView, 2);
        VirtualFlowTestUtils.assertGraphicIsVisible(treeView, 3);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(treeView, 4);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(treeView, 5);
    }

    private int rt_29650_start_count = 0;
    private int rt_29650_commit_count = 0;
    private int rt_29650_cancel_count = 0;
    @Test public void test_rt_29650() {
        installChildren();
        treeView.setOnEditStart(new EventHandler() {
            @Override public void handle(Event t) {
                rt_29650_start_count++;
            }
        });
        treeView.setOnEditCommit(new EventHandler() {
            @Override public void handle(Event t) {
                rt_29650_commit_count++;
            }
        });
        treeView.setOnEditCancel(new EventHandler() {
            @Override public void handle(Event t) {
                rt_29650_cancel_count++;
            }
        });

        treeView.setEditable(true);
        treeView.setCellFactory(TextFieldTreeCell.forTreeView());

        new StageLoader(treeView);

        treeView.edit(root);
        TreeCell rootCell = (TreeCell) VirtualFlowTestUtils.getCell(treeView, 0);
        TextField textField = (TextField) rootCell.getGraphic();
        textField.setText("Testing!");
        KeyEventFirer keyboard = new KeyEventFirer(textField);
        keyboard.doKeyPress(KeyCode.ENTER);

        assertEquals("Testing!", root.getValue());
        assertEquals(1, rt_29650_start_count);
        assertEquals(1, rt_29650_commit_count);
        assertEquals(0, rt_29650_cancel_count);
    }

    private int rt_33559_count = 0;
    @Test public void test_rt_33559() {
        installChildren();

        treeView.setShowRoot(true);
        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        treeView.getSelectionModel().getSelectedItems().addListener(new ListChangeListener() {
            @Override public void onChanged(Change c) {
                while (c.next()) {
                    System.out.println(c);
                    rt_33559_count++;
                }
            }
        });

        assertEquals(0, rt_33559_count);
        root.setExpanded(true);
        assertEquals(0, rt_33559_count);
    }

    @Test public void test_rt34103() {
        treeView.setRoot(new TreeItem("Root"));
        treeView.getRoot().setExpanded(true);

        for (int i = 0; i < 4; i++) {
            TreeItem parent = new TreeItem("item - " + i);
            treeView.getRoot().getChildren().add(parent);

            for (int j = 0; j < 4; j++) {
                TreeItem child = new TreeItem("item - " + i + " " + j);
                parent.getChildren().add(child);
            }
        }

        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TreeItem item0 = treeView.getTreeItem(1);
        assertEquals("item - 0", item0.getValue());
        item0.setExpanded(true);

        treeView.getSelectionModel().selectIndices(1,2,3);
        assertEquals(3, treeView.getSelectionModel().getSelectedIndices().size());

        item0.setExpanded(false);
        Toolkit.getToolkit().firePulse();
        assertEquals(1, treeView.getSelectionModel().getSelectedIndices().size());
    }

    @Test public void test_rt26718() {
        treeView.setRoot(new TreeItem("Root"));
        treeView.getRoot().setExpanded(true);

        for (int i = 0; i < 4; i++) {
            TreeItem parent = new TreeItem("item - " + i);
            treeView.getRoot().getChildren().add(parent);

            for (int j = 0; j < 4; j++) {
                TreeItem child = new TreeItem("item - " + i + " " + j);
                parent.getChildren().add(child);
            }
        }

        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        final TreeItem item0 = treeView.getTreeItem(1);
        final TreeItem item1 = treeView.getTreeItem(2);

        assertEquals("item - 0", item0.getValue());
        assertEquals("item - 1", item1.getValue());

        item0.setExpanded(true);
        item1.setExpanded(true);
        Toolkit.getToolkit().firePulse();

        treeView.getSelectionModel().selectRange(0, 8);
        assertEquals(8, treeView.getSelectionModel().getSelectedIndices().size());
        assertEquals(7, treeView.getSelectionModel().getSelectedIndex());
        assertEquals(7, treeView.getFocusModel().getFocusedIndex());

        // collapse item0 - but because the selected and focused indices are
        // not children of item 0, they should remain where they are (but of
        // course be shifted up). The bug was that focus was moving up to item0,
        // which makes no sense
        item0.setExpanded(false);
        Toolkit.getToolkit().firePulse();
        assertEquals(3, treeView.getSelectionModel().getSelectedIndex());
        assertEquals(3, treeView.getFocusModel().getFocusedIndex());
    }

    @Test public void test_rt26721_collapseParent_firstRootChild() {
        treeView.setRoot(new TreeItem("Root"));
        treeView.getRoot().setExpanded(true);

        for (int i = 0; i < 4; i++) {
            TreeItem parent = new TreeItem("item - " + i);
            treeView.getRoot().getChildren().add(parent);

            for (int j = 0; j < 4; j++) {
                TreeItem child = new TreeItem("item - " + i + " " + j);
                parent.getChildren().add(child);
            }
        }

        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        final TreeItem<String> item0 = treeView.getTreeItem(1);
        final TreeItem<String> item0child0 = item0.getChildren().get(0);
        final TreeItem<String> item1 = treeView.getTreeItem(2);

        assertEquals("item - 0", item0.getValue());
        assertEquals("item - 1", item1.getValue());

        item0.setExpanded(true);
        item1.setExpanded(true);
        Toolkit.getToolkit().firePulse();

        // select the first child of item0
        treeView.getSelectionModel().select(item0child0);

        assertEquals(item0child0, treeView.getSelectionModel().getSelectedItem());
        assertEquals(item0child0, treeView.getFocusModel().getFocusedItem());

        // collapse item0 - we expect the selection / focus to move up to item0
        item0.setExpanded(false);
        Toolkit.getToolkit().firePulse();
        assertEquals(item0, treeView.getSelectionModel().getSelectedItem());
        assertEquals(item0, treeView.getFocusModel().getFocusedItem());
    }

    @Test public void test_rt26721_collapseParent_lastRootChild() {
        treeView.setRoot(new TreeItem("Root"));
        treeView.getRoot().setExpanded(true);

        for (int i = 0; i < 4; i++) {
            TreeItem parent = new TreeItem("item - " + i);
            treeView.getRoot().getChildren().add(parent);

            for (int j = 0; j < 4; j++) {
                TreeItem child = new TreeItem("item - " + i + " " + j);
                parent.getChildren().add(child);
            }
        }

        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        final TreeItem<String> item3 = treeView.getTreeItem(4);
        final TreeItem<String> item3child0 = item3.getChildren().get(0);

        assertEquals("item - 3", item3.getValue());
        assertEquals("item - 3 0", item3child0.getValue());

        item3.setExpanded(true);
        Toolkit.getToolkit().firePulse();

        // select the first child of item0
        treeView.getSelectionModel().select(item3child0);

        assertEquals(item3child0, treeView.getSelectionModel().getSelectedItem());
        assertEquals(item3child0, treeView.getFocusModel().getFocusedItem());

        // collapse item3 - we expect the selection / focus to move up to item3
        item3.setExpanded(false);
        Toolkit.getToolkit().firePulse();
        assertEquals(item3, treeView.getSelectionModel().getSelectedItem());
        assertEquals(item3, treeView.getFocusModel().getFocusedItem());
    }

    @Test public void test_rt26721_collapseGrandParent() {
        treeView.setRoot(new TreeItem("Root"));
        treeView.getRoot().setExpanded(true);

        for (int i = 0; i < 4; i++) {
            TreeItem parent = new TreeItem("item - " + i);
            treeView.getRoot().getChildren().add(parent);

            for (int j = 0; j < 4; j++) {
                TreeItem child = new TreeItem("item - " + i + " " + j);
                parent.getChildren().add(child);
            }
        }

        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        final TreeItem<String> item0 = treeView.getTreeItem(1);
        final TreeItem<String> item0child0 = item0.getChildren().get(0);
        final TreeItem<String> item1 = treeView.getTreeItem(2);

        assertEquals("item - 0", item0.getValue());
        assertEquals("item - 1", item1.getValue());

        item0.setExpanded(true);
        item1.setExpanded(true);
        Toolkit.getToolkit().firePulse();

        // select the first child of item0
        treeView.getSelectionModel().select(item0child0);

        assertEquals(item0child0, treeView.getSelectionModel().getSelectedItem());
        assertEquals(item0child0, treeView.getFocusModel().getFocusedItem());

        // collapse root - we expect the selection / focus to move up to root
        treeView.getRoot().setExpanded(false);
        Toolkit.getToolkit().firePulse();
        assertEquals(treeView.getRoot(), treeView.getSelectionModel().getSelectedItem());
        assertEquals(treeView.getRoot(), treeView.getFocusModel().getFocusedItem());
    }

    @Test public void test_rt34694() {
        TreeItem treeNode = new TreeItem("Controls");
        treeNode.getChildren().addAll(
            new TreeItem("Button"),
            new TreeItem("ButtonBar"),
            new TreeItem("LinkBar"),
            new TreeItem("LinkButton"),
            new TreeItem("PopUpButton"),
            new TreeItem("ToggleButtonBar")
        );

        final TreeView treeView = new TreeView();
        treeView.setRoot(treeNode);
        treeNode.setExpanded(true);

        treeView.getSelectionModel().select(0);
        assertTrue(treeView.getSelectionModel().isSelected(0));
        assertTrue(treeView.getFocusModel().isFocused(0));

        treeNode.getChildren().clear();
        treeNode.getChildren().addAll(
                new TreeItem("Button1"),
                new TreeItem("ButtonBar1"),
                new TreeItem("LinkBar1"),
                new TreeItem("LinkButton1"),
                new TreeItem("PopUpButton1"),
                new TreeItem("ToggleButtonBar1")
        );
        Toolkit.getToolkit().firePulse();

        assertTrue(treeView.getSelectionModel().isSelected(0));
        assertTrue(treeView.getFocusModel().isFocused(0));
    }

    private int test_rt_35213_eventCount = 0;
    @Test public void test_rt35213() {
        final TreeView<String> view = new TreeView<>();

        TreeItem<String> root = new TreeItem<>("Boss");
        view.setRoot(root);

        TreeItem<String> group1 = new TreeItem<>("Group 1");
        TreeItem<String> group2 = new TreeItem<>("Group 2");
        TreeItem<String> group3 = new TreeItem<>("Group 3");

        root.getChildren().addAll(group1, group2, group3);

        TreeItem<String> employee1 = new TreeItem<>("Employee 1");
        TreeItem<String> employee2 = new TreeItem<>("Employee 2");

        group2.getChildren().addAll(employee1, employee2);

        view.expandedItemCountProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldCount, Number newCount) {

                // DEBUG OUTPUT
//                System.out.println("new expanded item count: " + newCount.intValue());
//                for (int i = 0; i < newCount.intValue(); i++) {
//                    TreeItem<String> item = view.getTreeItem(i);
//                    String text = item.getValue();
//                    System.out.println("person found at index " + i + " is " + text);
//                }
//                System.out.println("------------------------------------------");

                if (test_rt_35213_eventCount == 0) {
                    assertEquals(4, newCount);
                    assertEquals("Boss", view.getTreeItem(0).getValue());
                    assertEquals("Group 1", view.getTreeItem(1).getValue());
                    assertEquals("Group 2", view.getTreeItem(2).getValue());
                    assertEquals("Group 3", view.getTreeItem(3).getValue());
                } else if (test_rt_35213_eventCount == 1) {
                    assertEquals(6, newCount);
                    assertEquals("Boss", view.getTreeItem(0).getValue());
                    assertEquals("Group 1", view.getTreeItem(1).getValue());
                    assertEquals("Group 2", view.getTreeItem(2).getValue());
                    assertEquals("Employee 1", view.getTreeItem(3).getValue());
                    assertEquals("Employee 2", view.getTreeItem(4).getValue());
                    assertEquals("Group 3", view.getTreeItem(5).getValue());
                } else if (test_rt_35213_eventCount == 2) {
                    assertEquals(4, newCount);
                    assertEquals("Boss", view.getTreeItem(0).getValue());
                    assertEquals("Group 1", view.getTreeItem(1).getValue());
                    assertEquals("Group 2", view.getTreeItem(2).getValue());
                    assertEquals("Group 3", view.getTreeItem(3).getValue());
                }

                test_rt_35213_eventCount++;
            }
        });

        new StageLoader(view);

        root.setExpanded(true);
        Toolkit.getToolkit().firePulse();

        group2.setExpanded(true);
        Toolkit.getToolkit().firePulse();

        group2.setExpanded(false);
        Toolkit.getToolkit().firePulse();
    }

    @Test public void test_rt23245_itemIsInTree() {
        final TreeView<String> view = new TreeView<String>();
        final List<TreeItem<String>> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final TreeItem<String> item = new TreeItem<String>("Item" + i);
            item.setExpanded(true);
            items.add(item);
        }

        // link the items up so that the next item is the child of the current item
        for (int i = 0; i < 9; i++) {
            items.get(i).getChildren().add(items.get(i + 1));
        }

        view.setRoot(items.get(0));

        for (int i = 0; i < 10; i++) {
            // we expect the level of the tree item at the ith position to be
            // 0, as every iteration we are setting the ith item as the root.
            assertEquals(0, view.getTreeItemLevel(items.get(i)));

            // whilst we are testing, we should also ensure that the ith item
            // is indeed the root item, and that the ith item is indeed the item
            // at the 0th position
            assertEquals(items.get(i), view.getRoot());
            assertEquals(items.get(i), view.getTreeItem(0));

            // shuffle the next item into the root position (keeping its parent
            // chain intact - which is what exposes this issue in the first place).
            if (i < 9) {
                view.setRoot(items.get(i + 1));
            }
        }
    }

    @Test public void test_rt23245_itemIsNotInTree_noRootNode() {
        final TreeView<String> view = new TreeView<String>();
        final List<TreeItem<String>> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final TreeItem<String> item = new TreeItem<String>("Item" + i);
            item.setExpanded(true);
            items.add(item);
        }

        // link the items up so that the next item is the child of the current item
        for (int i = 0; i < 9; i++) {
            items.get(i).getChildren().add(items.get(i + 1));
        }

        for (int i = 0; i < 10; i++) {
            // because we have no root (and we are not changing the root like
            // the previous test), we expect the tree item level of the item
            // in the ith position to be i.
            assertEquals(i, view.getTreeItemLevel(items.get(i)));

            // all items requested from the TreeView should be null, as the
            // TreeView does not have a root item
            assertNull(view.getTreeItem(i));
        }
    }

    @Test public void test_rt23245_itemIsNotInTree_withUnrelatedRootNode() {
        final TreeView<String> view = new TreeView<String>();
        final List<TreeItem<String>> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final TreeItem<String> item = new TreeItem<String>("Item" + i);
            item.setExpanded(true);
            items.add(item);
        }

        // link the items up so that the next item is the child of the current item
        for (int i = 0; i < 9; i++) {
            items.get(i).getChildren().add(items.get(i + 1));
        }

        view.setRoot(new TreeItem("Unrelated root node"));

        for (int i = 0; i < 10; i++) {
            // because we have no root (and we are not changing the root like
            // the previous test), we expect the tree item level of the item
            // in the ith position to be i.
            assertEquals(i, view.getTreeItemLevel(items.get(i)));

            // all items requested from the TreeView should be null except for
            // the root node
            assertNull(view.getTreeItem(i + 1));
        }
    }

    @Test public void test_rt35039_setRoot() {
        TreeItem<String> root = new TreeItem<>("Root");
        root.setExpanded(true);
        root.getChildren().addAll(
                new TreeItem("aabbaa"),
                new TreeItem("bbc"));

        final TreeView<String> treeView = new TreeView<>();
        treeView.setRoot(root);

        new StageLoader(treeView);

        // everything should be null to start with
        assertNull(treeView.getSelectionModel().getSelectedItem());

        // select "bbc" and ensure everything is set to that
        treeView.getSelectionModel().select(2);
        assertEquals("bbc", treeView.getSelectionModel().getSelectedItem().getValue());

        // change the items list - but retain the same content. We expect
        // that "bbc" remains selected as it is still in the list
        treeView.setRoot(root);
        assertEquals("bbc", treeView.getSelectionModel().getSelectedItem().getValue());
    }

    @Test public void test_rt35039_resetRootChildren() {
        TreeItem aabbaa = new TreeItem("aabbaa");
        TreeItem bbc = new TreeItem("bbc");

        TreeItem<String> root = new TreeItem<>("Root");
        root.setExpanded(true);
        root.getChildren().setAll(aabbaa, bbc);

        final TreeView<String> treeView = new TreeView<>();
        treeView.setRoot(root);

        new StageLoader(treeView);

        // everything should be null to start with
        assertNull(treeView.getSelectionModel().getSelectedItem());

        // select "bbc" and ensure everything is set to that
        treeView.getSelectionModel().select(2);
        assertEquals("bbc", treeView.getSelectionModel().getSelectedItem().getValue());

        // change the items list - but retain the same content. We expect
        // that "bbc" remains selected as it is still in the list
        root.getChildren().setAll(aabbaa, bbc);
        assertEquals("bbc", treeView.getSelectionModel().getSelectedItem().getValue());
    }
}

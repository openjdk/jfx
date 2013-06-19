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

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static javafx.scene.control.TreeTableColumn.SortType.ASCENDING;
import static javafx.scene.control.TreeTableColumn.SortType.DESCENDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.sun.javafx.scene.control.test.Data;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TreeTableView.TreeTableViewFocusModel;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.scene.control.TableColumnComparatorBase.TreeTableColumnComparator;
import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import com.sun.javafx.scene.control.skin.VirtualScrollBar;
import com.sun.javafx.scene.control.test.Person;
import com.sun.javafx.scene.control.test.RT_22463_Person;
import com.sun.javafx.tk.Toolkit;

public class TreeTableViewTest {
    private TreeTableView<String> treeTableView;
    private TreeTableView.TreeTableViewSelectionModel sm;
    private TreeTableViewFocusModel<String> fm;
    
    
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
    
    @Before public void setup() {
        treeTableView = new TreeTableView<String>();
        sm = treeTableView.getSelectionModel();
        fm = treeTableView.getFocusModel();
        
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
        treeTableView.setRoot(root);
    }
    
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
    
    @Test public void noArgConstructorSetsNonNullSortPolicy() {
        assertNotNull(treeTableView.getSortPolicy());
    }
    
    @Test public void noArgConstructorSetsNullComparator() {
        assertNull(treeTableView.getComparator());
    }
    
    @Test public void noArgConstructorSetsNullOnSort() {
        assertNull(treeTableView.getOnSort());
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
        TreeTableView treeTableView = new TreeTableView();
        TreeTableColumn<String,String> first = new TreeTableColumn<String,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        TreeTableColumn<String,String> second = new TreeTableColumn<String,String>("second");
        second.setCellValueFactory(new PropertyValueFactory("lastName"));
        treeTableView.getColumns().addAll(first, second);
        treeTableView.getSortOrder().setAll(first, second);
        treeTableView.getColumns().remove(first);
        assertFalse(treeTableView.getSortOrder().contains(first));
    } 
    
    
    /*********************************************************************
     * Tests for new sorting API in JavaFX 8.0                           *
     ********************************************************************/
    
    private TreeItem<String> apple, orange, banana;
    
    // TODO test for sort policies returning null
    // TODO test for changing column sortType out of order
    
    private static final Callback<TreeTableView<String>, Boolean> NO_SORT_FAILED_SORT_POLICY = 
            new Callback<TreeTableView<String>, Boolean>() {
        @Override public Boolean call(TreeTableView<String> treeTableView) {
            return false;
        }
    };
    
    private static final Callback<TreeTableView<String>, Boolean> SORT_SUCCESS_ASCENDING_SORT_POLICY = 
            new Callback<TreeTableView<String>, Boolean>() {
        @Override public Boolean call(TreeTableView<String> treeTableView) {
            if (treeTableView.getSortOrder().isEmpty()) return true;
            FXCollections.sort(treeTableView.getRoot().getChildren(), new Comparator<TreeItem<String>>() {
                @Override public int compare(TreeItem<String> o1, TreeItem<String> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
            return true;
        }
    };
    
    private TreeTableColumn<String, String> initSortTestStructure() {
        TreeTableColumn<String, String> col = new TreeTableColumn<String, String>("column");
        col.setSortType(ASCENDING);
        col.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<String, String>, ObservableValue<String>>() {
            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<String, String> param) {
                return new ReadOnlyObjectWrapper<String>(param.getValue().getValue());
            }
        });
        treeTableView.getColumns().add(col);
        
        TreeItem<String> newRoot = new TreeItem<String>("root");
        newRoot.setExpanded(true);
        newRoot.getChildren().addAll(
                apple  = new TreeItem("Apple"), 
                orange = new TreeItem("Orange"), 
                banana = new TreeItem("Banana"));
        
        treeTableView.setRoot(newRoot);
        
        return col;
    }
    
    @Ignore("This test is only valid if sort event consumption should revert changes")
    @Test public void testSortEventCanBeConsumedToStopSortOccurring_changeSortOrderList() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        treeTableView.setOnSort(new EventHandler<SortEvent<TreeTableView<String>>>() {
            @Override public void handle(SortEvent<TreeTableView<String>> event) {
                event.consume();
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        treeTableView.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        
        // the sort order list should be returned back to its original state
        assertTrue(treeTableView.getSortOrder().isEmpty());
    }
    
    @Test public void testSortEventCanBeNotConsumedToAllowSortToOccur_changeSortOrderList() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        treeTableView.setOnSort(new EventHandler<SortEvent<TreeTableView<String>>>() {
            @Override public void handle(SortEvent<TreeTableView<String>> event) {
                // do not consume here - this allows the sort to happen
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        treeTableView.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, banana, orange);
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getSortOrder(), col);
    }
    
    @Ignore("This test is only valid if sort event consumption should revert changes")
    @Test public void testSortEventCanBeConsumedToStopSortOccurring_changeColumnSortType_AscendingToDescending() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        assertEquals(ASCENDING, col.getSortType());
        treeTableView.getSortOrder().add(col);
        treeTableView.setOnSort(new EventHandler<SortEvent<TreeTableView<String>>>() {
            @Override public void handle(SortEvent<TreeTableView<String>> event) {
                event.consume();
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, banana, orange);
        
        // when we change from ASCENDING to DESCENDING we don't expect the sort
        // to actually change (and in fact we expect the sort type to resort
        // back to being ASCENDING)
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, banana, orange);
        assertEquals(ASCENDING, col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getSortOrder(), col);
    }
    
    @Test public void testSortEventCanBeNotConsumedToAllowSortToOccur_changeColumnSortType_AscendingToDescending() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        assertEquals(ASCENDING, col.getSortType());
        treeTableView.getSortOrder().add(col);
        treeTableView.setOnSort(new EventHandler<SortEvent<TreeTableView<String>>>() {
            @Override public void handle(SortEvent<TreeTableView<String>> event) {
                // do not consume here - this allows the sort to happen
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, banana, orange);
        
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        assertEquals(DESCENDING, col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getSortOrder(), col);
    }
    
    @Ignore("This test is only valid if sort event consumption should revert changes")
    @Test public void testSortEventCanBeConsumedToStopSortOccurring_changeColumnSortType_DescendingToNull() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        assertEquals(DESCENDING, col.getSortType());
        treeTableView.getSortOrder().add(col);
        treeTableView.setOnSort(new EventHandler<SortEvent<TreeTableView<String>>>() {
            @Override public void handle(SortEvent<TreeTableView<String>> event) {
                event.consume();
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        
        col.setSortType(null);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        assertEquals(DESCENDING, col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getSortOrder(), col);
    }
    
    @Test public void testSortEventCanBeNotConsumedToAllowSortToOccur_changeColumnSortType_DescendingToNull() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        assertEquals(DESCENDING, col.getSortType());
        treeTableView.getSortOrder().add(col);
        treeTableView.setOnSort(new EventHandler<SortEvent<TreeTableView<String>>>() {
            @Override public void handle(SortEvent<TreeTableView<String>> event) {
                // do not consume here - this allows the sort to happen
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        
        col.setSortType(null);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        assertNull(col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getSortOrder(), col);
    }
    
    @Ignore("This test is only valid if sort event consumption should revert changes")
    @Test public void testSortEventCanBeConsumedToStopSortOccurring_changeColumnSortType_NullToAscending() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(null);
        assertNull(col.getSortType());
        treeTableView.getSortOrder().add(col);
        treeTableView.setOnSort(new EventHandler<SortEvent<TreeTableView<String>>>() {
            @Override public void handle(SortEvent<TreeTableView<String>> event) {
                event.consume();
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        
        col.setSortType(ASCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        assertNull(col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getSortOrder(), col);
    }
    
    @Test public void testSortEventCanBeNotConsumedToAllowSortToOccur_changeColumnSortType_NullToAscending() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(null);
        assertNull(col.getSortType());
        treeTableView.getSortOrder().add(col);
        treeTableView.setOnSort(new EventHandler<SortEvent<TreeTableView<String>>>() {
            @Override public void handle(SortEvent<TreeTableView<String>> event) {
                // do not consume here - this allows the sort to happen
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        
        col.setSortType(ASCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, banana, orange);
        assertEquals(ASCENDING, col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getSortOrder(), col);
    }
    
    @Test public void testSortMethodWithNullSortPolicy() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        treeTableView.setSortPolicy(null);
        assertNull(treeTableView.getSortPolicy());
        treeTableView.sort();
    }
    
    @Test public void testChangingSortPolicyUpdatesItemsList() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        treeTableView.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        treeTableView.setSortPolicy(SORT_SUCCESS_ASCENDING_SORT_POLICY);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, banana, orange);
    }
    
    @Test public void testChangingSortPolicyDoesNotUpdateItemsListWhenTheSortOrderListIsEmpty() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        
        treeTableView.setSortPolicy(SORT_SUCCESS_ASCENDING_SORT_POLICY);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortOrderAddition() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        treeTableView.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);
        
        treeTableView.getSortOrder().add(col);
        
        // no sort should be run (as we have a custom sort policy), and the 
        // sortOrder list should be empty as the sortPolicy failed
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        assertTrue(treeTableView.getSortOrder().isEmpty());
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortOrderRemoval() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        treeTableView.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        
        treeTableView.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        // even though we remove the column from the sort order here, because the
        // sort policy fails the items list should remain unchanged and the sort
        // order list should continue to have the column in it.
        treeTableView.getSortOrder().remove(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getSortOrder(), col);
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortTypeChange_ascendingToDescending() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(ASCENDING);
        treeTableView.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, banana, orange);
        
        treeTableView.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, banana, orange);
        assertEquals(ASCENDING, col.getSortType());
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortTypeChange_descendingToNull() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        treeTableView.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        
        treeTableView.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        col.setSortType(null);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        assertEquals(DESCENDING, col.getSortType());
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortTypeChange_nullToAscending() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        col.setSortType(null);
        treeTableView.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        
        treeTableView.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        col.setSortType(ASCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        assertNull(col.getSortType());
    }
    
    @Test public void testComparatorChangesInSyncWithSortOrder_1() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        assertNull(treeTableView.getComparator());
        assertTrue(treeTableView.getSortOrder().isEmpty());
        
        treeTableView.getSortOrder().add(col);
        TreeTableColumnComparator c = (TreeTableColumnComparator)treeTableView.getComparator();
        assertNotNull(c);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(c.getColumns(), col);
    }
    
    @Test public void testComparatorChangesInSyncWithSortOrder_2() {
        // same as test above
        TreeTableColumn<String, String> col = initSortTestStructure();
        assertNull(treeTableView.getComparator());
        assertTrue(treeTableView.getSortOrder().isEmpty());
        
        treeTableView.getSortOrder().add(col);
        TreeTableColumnComparator c = (TreeTableColumnComparator)treeTableView.getComparator();
        assertNotNull(c);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(c.getColumns(), col);
        
        // now remove column from sort order, and the comparator should go to
        // being null
        treeTableView.getSortOrder().remove(col);
        assertNull(treeTableView.getComparator());
    }
    
    @Test public void testFailedSortPolicyBacksOutComparatorChange_sortOrderAddition() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        final TreeTableColumnComparator oldComparator = (TreeTableColumnComparator)treeTableView.getComparator();
        
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), apple, orange, banana);
        treeTableView.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);
        
        treeTableView.getSortOrder().add(col);
        
        assertEquals(oldComparator, treeTableView.getComparator());
    }
    
    @Test public void testFailedSortPolicyBacksOutComparatorChange_sortOrderRemoval() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        TreeTableColumnComparator oldComparator = (TreeTableColumnComparator)treeTableView.getComparator();
        assertNull(oldComparator);

        col.setSortType(DESCENDING);
        treeTableView.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(treeTableView.getRoot().getChildren(), orange, banana, apple);
        oldComparator = (TreeTableColumnComparator)treeTableView.getComparator();
        VirtualFlowTestUtils.assertListContainsItemsInOrder(oldComparator.getColumns(), col);
        
        treeTableView.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);
        treeTableView.getSortOrder().remove(col);
        
        assertTrue(treeTableView.getSortOrder().contains(col));
        VirtualFlowTestUtils.assertListContainsItemsInOrder(oldComparator.getColumns(), col);
    }
    
    @Test public void testFailedSortPolicyBacksOutComparatorChange_sortTypeChange() {
        TreeTableColumn<String, String> col = initSortTestStructure();
        final TreeTableColumnComparator oldComparator = (TreeTableColumnComparator)treeTableView.getComparator();
        assertNull(oldComparator);
        
        treeTableView.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);
        treeTableView.getSortOrder().add(col);
        col.setSortType(ASCENDING);
        
        assertTrue(treeTableView.getSortOrder().isEmpty());
        assertNull(oldComparator);
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
    
    @Test public void test_rt27181() {
        myCompanyRootNode.setExpanded(true);
        treeTableView.setRoot(myCompanyRootNode);
        
        // start test
        salesDepartment.setExpanded(true);
        treeTableView.getSelectionModel().select(salesDepartment);
        
        assertEquals(1, treeTableView.getFocusModel().getFocusedIndex());
        itSupport.setExpanded(true);
        assertEquals(1, treeTableView.getFocusModel().getFocusedIndex());
    }
    
    @Test public void test_rt27185() {
        myCompanyRootNode.setExpanded(true);
        treeTableView.setRoot(myCompanyRootNode);
        
        // start test
        itSupport.setExpanded(true);
        treeTableView.getSelectionModel().select(mikeGraham);
        
        assertEquals(mikeGraham, treeTableView.getFocusModel().getFocusedItem());
        salesDepartment.setExpanded(true);
        assertEquals(mikeGraham, treeTableView.getFocusModel().getFocusedItem());
    }
    
    @Ignore("Bug hasn't been fixed yet")
    @Test public void test_rt28114() {
        myCompanyRootNode.setExpanded(true);
        treeTableView.setRoot(myCompanyRootNode);
        
        // start test
        itSupport.setExpanded(true);
        treeTableView.getSelectionModel().select(itSupport);
        assertEquals(itSupport, treeTableView.getFocusModel().getFocusedItem());
        assertEquals(itSupport, treeTableView.getSelectionModel().getSelectedItem());
        assertTrue(! itSupport.isLeaf());
        assertTrue(itSupport.isExpanded());
        
        itSupport.getChildren().remove(mikeGraham);
        assertEquals(itSupport, treeTableView.getFocusModel().getFocusedItem());
        assertEquals(itSupport, treeTableView.getSelectionModel().getSelectedItem());
        assertTrue(itSupport.isLeaf());
        assertTrue(!itSupport.isExpanded());
    }
    
    @Test public void test_rt27820_1() {
        TreeItem root = new TreeItem("root");
        root.setExpanded(true);
        TreeItem child = new TreeItem("child");
        root.getChildren().add(child);
        treeTableView.setRoot(root);
        
        treeTableView.getSelectionModel().select(0);
        assertEquals(1, treeTableView.getSelectionModel().getSelectedItems().size());
        assertEquals(root, treeTableView.getSelectionModel().getSelectedItem());
        
        treeTableView.setRoot(null);
        assertEquals(0, treeTableView.getSelectionModel().getSelectedItems().size());
        assertNull(treeTableView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt27820_2() {
        TreeItem root = new TreeItem("root");
        root.setExpanded(true);
        TreeItem child = new TreeItem("child");
        root.getChildren().add(child);
        treeTableView.setRoot(root);
        
        treeTableView.getSelectionModel().select(1);
        assertEquals(1, treeTableView.getSelectionModel().getSelectedItems().size());
        assertEquals(child, treeTableView.getSelectionModel().getSelectedItem());
        
        treeTableView.setRoot(null);
        assertEquals(0, treeTableView.getSelectionModel().getSelectedItems().size());
        assertNull(treeTableView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt28390() {
        // There should be no NPE when a TreeTableView is shown and the disclosure
        // node is null in a TreeCell
        TreeItem root = new TreeItem("root");
        treeTableView.setRoot(root);
        
        // install a custom cell factory that forces the disclosure node to be
        // null (because by default a null disclosure node will be replaced by
        // a non-null one).
        treeTableView.setRowFactory(new Callback() {
            @Override public Object call(Object p) {
                TreeTableRow treeCell = new TreeTableRow() {
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
            group.getChildren().setAll(treeTableView);
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
    
    @Ignore("This test begun failing when createDefaultCellImpl was removed from TreeTableViewSkin on 28/3/2013")
    @Test public void test_rt28534() {
        TreeItem root = new TreeItem("root");
        root.getChildren().setAll(
                new TreeItem(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem(new Person("Michael", "Brown", "michael.brown@example.com")));
        root.setExpanded(true);
        
        TreeTableView<Person> table = new TreeTableView<Person>(root);
        
        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));

        TreeTableColumn emailCol = new TreeTableColumn("Email");
        emailCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);

        VirtualFlowTestUtils.assertRowsNotEmpty(table, 0, 6); // rows 0 - 6 should be filled
        VirtualFlowTestUtils.assertRowsEmpty(table, 6, -1); // rows 6+ should be empty
        
        // now we replace the data and expect the cells that have no data
        // to be empty
        root.getChildren().setAll(
                new TreeItem(new Person("*_*Emma", "Jones", "emma.jones@example.com")),
                new TreeItem(new Person("_Michael", "Brown", "michael.brown@example.com")));
        
        VirtualFlowTestUtils.assertRowsNotEmpty(table, 0, 3); // rows 0 - 3 should be filled
        VirtualFlowTestUtils.assertRowsEmpty(table, 3, -1); // rows 3+ should be empty
    }
    
    @Test public void test_rt22463() {
        final TreeTableView<RT_22463_Person> table = new TreeTableView<RT_22463_Person>();
        table.setTableMenuButtonVisible(true);
        TreeTableColumn c1 = new TreeTableColumn("Id");
        TreeTableColumn c2 = new TreeTableColumn("Name");
        c1.setCellValueFactory(new TreeItemPropertyValueFactory<Person, Long>("id"));
        c2.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("name"));
        table.getColumns().addAll(c1, c2);
        
        RT_22463_Person rootPerson = new RT_22463_Person();
        rootPerson.setName("Root");
        TreeItem<RT_22463_Person> root = new TreeItem<RT_22463_Person>(rootPerson);
        root.setExpanded(true);
        
        table.setRoot(root);
        
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
        VirtualFlowTestUtils.assertCellTextEquals(table, 1, "1", "name1");
        VirtualFlowTestUtils.assertCellTextEquals(table, 2, "2", "name2");
        
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
        VirtualFlowTestUtils.assertCellTextEquals(table, 1, "1", "updated name1");
        VirtualFlowTestUtils.assertCellTextEquals(table, 2, "2", "updated name2");
    }
    
    @Test public void test_rt28637() {
        TreeItem<String> s1, s2, s3, s4;
        ObservableList<TreeItem<String>> items = FXCollections.observableArrayList(
                s1 = new TreeItem<String>("String1"), 
                s2 = new TreeItem<String>("String2"), 
                s3 = new TreeItem<String>("String3"), 
                s4 = new TreeItem<String>("String4"));
        
        final TreeTableView<String> treeTableView = new TreeTableView<String>();
        
        TreeItem<String> root = new TreeItem<String>("Root");
        root.setExpanded(true);
        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);
        root.getChildren().addAll(items);
        
        treeTableView.getSelectionModel().select(0);
        assertEquals((Object)s1, treeTableView.getSelectionModel().getSelectedItem());
        assertEquals((Object)s1, treeTableView.getSelectionModel().getSelectedItems().get(0));
        assertEquals(0, treeTableView.getSelectionModel().getSelectedIndex());
        
        root.getChildren().remove(treeTableView.getSelectionModel().getSelectedItem());
        assertEquals((Object)s2, treeTableView.getSelectionModel().getSelectedItem());
        assertEquals((Object)s2, treeTableView.getSelectionModel().getSelectedItems().get(0));
        assertEquals(0, treeTableView.getSelectionModel().getSelectedIndex());
    }
    
    @Test public void test_rt24844() {
        // p1 == lowest first name
        TreeItem<Person> p0, p1, p2, p3, p4;
        
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
            p3 = new TreeItem<Person>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
            p2 = new TreeItem<Person>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
            p1 = new TreeItem<Person>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
            p0 = new TreeItem<Person>(new Person("Emma", "Jones", "emma.jones@example.com")),
            p4 = new TreeItem<Person>(new Person("Michael", "Brown", "michael.brown@example.com")));
            
        TreeTableView<Person> table = new TreeTableView<>();
        
        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);
        
        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));
        
        // set dummy comparator to lock items in place until new comparator is set
        firstNameCol.setComparator(new Comparator() {
            @Override public int compare(Object t, Object t1) {
                return 0;
            }
        });

        table.getColumns().addAll(firstNameCol);
        table.getSortOrder().add(firstNameCol);
        
        // ensure the existing order is as expected
        assertEquals(p3, root.getChildren().get(0));
        assertEquals(p2, root.getChildren().get(1));
        assertEquals(p1, root.getChildren().get(2));
        assertEquals(p0, root.getChildren().get(3));
        assertEquals(p4, root.getChildren().get(4));
        
        // set a new comparator
        firstNameCol.setComparator(new Comparator() {
            Random r =  new Random();
            @Override public int compare(Object t, Object t1) {
                return t.toString().compareTo(t1.toString());
            }
        });
        
        // ensure the new order is as expected
        assertEquals(p0, root.getChildren().get(0));
        assertEquals(p1, root.getChildren().get(1));
        assertEquals(p2, root.getChildren().get(2));
        assertEquals(p3, root.getChildren().get(3));
        assertEquals(p4, root.getChildren().get(4));
    }
    
    @Test public void test_rt29331() {
        TreeTableView<Person> table = new TreeTableView<Person>();
        
        // p1 == lowest first name
        TreeItem<Person> p0, p1, p2, p3, p4;
        
        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));

        TreeTableColumn emailCol = new TreeTableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));
        
        TreeTableColumn parentColumn = new TreeTableColumn<>("Parent");
        parentColumn.getColumns().addAll(firstNameCol, lastNameCol, emailCol);
        
        table.getColumns().addAll(parentColumn);
        
        // table is setup, now hide the 'last name' column
        emailCol.setVisible(false);
        assertFalse(emailCol.isVisible());
        
        // reorder columns inside the parent column
        parentColumn.getColumns().setAll(emailCol, firstNameCol, lastNameCol);
        
        // the email column should not become visible after this, but it does
        assertFalse(emailCol.isVisible());
    }
    
    private int rt29330_count = 0;
    @Test public void test_rt29330_1() {
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
                new TreeItem<Person>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<Person>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<Person>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<Person>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<Person>(new Person("Michael", "Brown", "michael.brown@example.com")));
                
        TreeTableView<Person> table = new TreeTableView<>();
        
        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);
        
        TreeTableColumn parentColumn = new TreeTableColumn<>("Parent");
        table.getColumns().addAll(parentColumn);
        
        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));
        
        parentColumn.getColumns().addAll(firstNameCol, lastNameCol);

        table.setOnSort(new EventHandler<SortEvent<TreeTableView<Person>>>() {
            @Override public void handle(SortEvent<TreeTableView<Person>> event) {
                rt29330_count++;
            }
        });
        
        // test preconditions
        assertEquals(ASCENDING, lastNameCol.getSortType());
        assertEquals(0, rt29330_count);
        
        table.getSortOrder().add(lastNameCol);
        assertEquals(1, rt29330_count);
        
        lastNameCol.setSortType(DESCENDING);
        assertEquals(2, rt29330_count);
        
        lastNameCol.setSortType(null);
        assertEquals(3, rt29330_count);
        
        lastNameCol.setSortType(ASCENDING);
        assertEquals(4, rt29330_count);
    }
    
    @Test public void test_rt29330_2() {
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
                new TreeItem<Person>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<Person>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<Person>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<Person>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<Person>(new Person("Michael", "Brown", "michael.brown@example.com")));
                
        TreeTableView<Person> table = new TreeTableView<>();
        
        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);
        
        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));
        
        // this test differs from the previous one by installing the parent column
        // into the tableview after it has the children added into it
        TreeTableColumn parentColumn = new TreeTableColumn<>("Parent");
        parentColumn.getColumns().addAll(firstNameCol, lastNameCol);
        table.getColumns().addAll(parentColumn);

        table.setOnSort(new EventHandler<SortEvent<TreeTableView<Person>>>() {
            @Override public void handle(SortEvent<TreeTableView<Person>> event) {
                rt29330_count++;
            }
        });
        
        // test preconditions
        assertEquals(ASCENDING, lastNameCol.getSortType());
        assertEquals(0, rt29330_count);
        
        table.getSortOrder().add(lastNameCol);
        assertEquals(1, rt29330_count);
        
        lastNameCol.setSortType(DESCENDING);
        assertEquals(2, rt29330_count);
        
        lastNameCol.setSortType(null);
        assertEquals(3, rt29330_count);
        
        lastNameCol.setSortType(ASCENDING);
        assertEquals(4, rt29330_count);
    }
    
    @Test public void test_rt29313_selectedIndices() {
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
                new TreeItem<Person>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<Person>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<Person>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<Person>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<Person>(new Person("Michael", "Brown", "michael.brown@example.com")));
                
        TreeTableView<Person> table = new TreeTableView<>();
        
        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);
        
        TableSelectionModel sm = table.getSelectionModel();
        
        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));
        
        TreeTableColumn emailCol = new TreeTableColumn("Email");
        emailCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("email"));
        
        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);
        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        assertTrue(sm.getSelectedIndices().isEmpty());
        
        // only (0,0) should be selected, so selected indices should be [0]
        sm.select(0, firstNameCol);
        assertEquals(1, sm.getSelectedIndices().size());
        
        // now (0,0) and (1,0) should be selected, so selected indices should be [0, 1]
        sm.select(1, firstNameCol);
        assertEquals(2, sm.getSelectedIndices().size());
        
        // now (0,0), (1,0) and (1,1) should be selected, but selected indices 
        // should remain as [0, 1], as we don't want selected indices to become
        // [0,1,1] (which is what RT-29313 is about)
        sm.select(1, lastNameCol);
        assertEquals(2, sm.getSelectedIndices().size());
        assertEquals(0, sm.getSelectedIndices().get(0));
        assertEquals(1, sm.getSelectedIndices().get(1));
    }
    
    @Test public void test_rt29313_selectedItems() {
        TreeItem<Person> p0, p1;
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
                p0 = new TreeItem<Person>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                p1 = new TreeItem<Person>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<Person>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<Person>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<Person>(new Person("Michael", "Brown", "michael.brown@example.com")));
                
        TreeTableView<Person> table = new TreeTableView<>();
        
        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);
        
        TableSelectionModel sm = table.getSelectionModel();
        
        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));
        
        TreeTableColumn emailCol = new TreeTableColumn("Email");
        emailCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("email"));
        
        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);
        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        assertTrue(sm.getSelectedItems().isEmpty());
        
        // only (0,0) should be selected, so selected items should be [p0]
        sm.select(0, firstNameCol);
        assertEquals(1, sm.getSelectedItems().size());
        
        // now (0,0) and (1,0) should be selected, so selected items should be [p0, p1]
        sm.select(1, firstNameCol);
        assertEquals(2, sm.getSelectedItems().size());
        
        // now (0,0), (1,0) and (1,1) should be selected, but selected items 
        // should remain as [p0, p1], as we don't want selected items to become
        // [p0,p1,p1] (which is what RT-29313 is about)
        sm.select(1, lastNameCol);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals(p0, sm.getSelectedItems().get(0));
        assertEquals(p1, sm.getSelectedItems().get(1));
    }
    
    @Test public void test_rt29566() {
        ObservableList<TreeItem<Person>> persons = FXCollections.observableArrayList(
                new TreeItem<Person>(new Person("Jacob", "Smith", "jacob.smith@example.com")),
                new TreeItem<Person>(new Person("Isabella", "Johnson", "isabella.johnson@example.com")),
                new TreeItem<Person>(new Person("Ethan", "Williams", "ethan.williams@example.com")),
                new TreeItem<Person>(new Person("Emma", "Jones", "emma.jones@example.com")),
                new TreeItem<Person>(new Person("Michael", "Brown", "michael.brown@example.com")));
                
        TreeTableView<Person> table = new TreeTableView<>();
        
        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);
        
        TableSelectionModel sm = table.getSelectionModel();
        
        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeTableColumn lastNameCol = new TreeTableColumn("Last Name");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("lastName"));
        
        TreeTableColumn emailCol = new TreeTableColumn("Email");
        emailCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("email"));
        
        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);
        
        // test the state before we hide and re-add a column
        VirtualFlowTestUtils.assertCellTextEquals(table, 0, "Jacob", "Smith", "jacob.smith@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 1, "Isabella", "Johnson", "isabella.johnson@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 2, "Ethan", "Williams", "ethan.williams@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 3, "Emma", "Jones", "emma.jones@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 4, "Michael", "Brown", "michael.brown@example.com");
        
        // hide the last name column, and test cells again
        table.getColumns().remove(lastNameCol);
        VirtualFlowTestUtils.assertCellTextEquals(table, 0, "Jacob", "jacob.smith@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 1, "Isabella", "isabella.johnson@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 2, "Ethan", "ethan.williams@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 3, "Emma", "emma.jones@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 4, "Michael", "michael.brown@example.com");
        
        // re-add the last name column - we should go back to the original state.
        // However, what appears to be happening is that, for some reason, some
        // of the cells from the removed column do not reappear - meaning in this case
        // some of the last name values will not be where we expect them to be.
        // This is clearly not ideal!
        table.getColumns().add(1, lastNameCol);
        VirtualFlowTestUtils.assertCellTextEquals(table, 0, "Jacob", "Smith", "jacob.smith@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 1, "Isabella", "Johnson", "isabella.johnson@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 2, "Ethan", "Williams", "ethan.williams@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 3, "Emma", "Jones", "emma.jones@example.com");
        VirtualFlowTestUtils.assertCellTextEquals(table, 4, "Michael", "Brown", "michael.brown@example.com");
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
                
        TreeTableView<Person> table = new TreeTableView<>();
        table.setMaxHeight(50);
        table.setPrefHeight(50);
        
        TreeItem<Person> root = new TreeItem<Person>(new Person("Root", null, null));
        root.setExpanded(true);
        table.setRoot(root);
        table.setShowRoot(false);
        root.getChildren().setAll(persons);
        
        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));
        
        table.getColumns().add(firstNameCol);
        
        Toolkit.getToolkit().firePulse();
        
        // we want the vertical scrollbar
        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowVerticalScrollbar(table);
        
        assertNotNull(scrollBar);
        assertTrue(scrollBar.isVisible());
        assertTrue(scrollBar.getVisibleAmount() > 0.0);
        assertTrue(scrollBar.getVisibleAmount() < 1.0);
        
        // this next test is likely to be brittle, but we'll see...If it is the
        // cause of failure then it can be commented out
        assertEquals(0.0625, scrollBar.getVisibleAmount(), 0.0);
    }
    
    @Test public void test_rt29676_withText() {
        // set up test
        TreeTableView<Data> treeTableView = new TreeTableView<Data>();
        treeTableView.setMaxWidth(100);
        
        TreeItem<Data> root = new TreeItem<Data>(new Data("Root"));
        treeTableView.setRoot(root);
        addLevel(root, 0, 30);

        treeTableView.getRoot().setExpanded(true);
        TreeTableColumn<Data, String> column = new TreeTableColumn<Data, String>("Items' name");
        column.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<Data, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(final TreeTableColumn.CellDataFeatures<Data, String> p) {
                return new ReadOnlyStringWrapper(p.getValue().getValue().getData());
            }
        });
        treeTableView.getColumns().add(column);

        // show treeTableView
        StageLoader stageLoader = new StageLoader(treeTableView);
        stageLoader.getStage().show();
        
        // expand all collapsed branches
        root.setExpanded(true);
        for (int i = 0; i < root.getChildren().size(); i++) {
            TreeItem<Data> child = root.getChildren().get(i);
            child.setExpanded(true);
        }
        
        // get all cells and ensure their content is as expected
        int cellCount = VirtualFlowTestUtils.getCellCount(treeTableView);
        for (int i = 0; i < cellCount; i++) {
            // get the TreeTableRow
            final TreeTableRow rowCell = (TreeTableRow) VirtualFlowTestUtils.getCell(treeTableView, i);
            final TreeItem treeItem = rowCell.getTreeItem();
            if (treeItem == null) continue;
            
            final boolean isBranch = ! treeItem.isLeaf();
            
            // then check its children
            List<Node> children = rowCell.getChildrenUnmodifiable();
            for (int j = 0; j < children.size(); j++) {
                final Node child = children.get(j);
                
                assertTrue(child.isVisible());
                assertNotNull(child.getParent());
                assertNotNull(child.getScene());
                
                if (child.getStyleClass().contains("tree-disclosure-node")) {
                    // no-op
                } 
                
                if (child.getStyleClass().contains("tree-table-cell")) {
                    TreeTableCell cell = (TreeTableCell) child;
                    assertNotNull(cell.getText());
                    assertFalse(cell.getText().isEmpty());
                }
            }
        }
    }
    private void addLevel(TreeItem<Data> item, int level, int length) {
        for (int i = 0; i < 3; i++) {
            StringBuilder builder = new StringBuilder();
            builder.append("Level " + level + " Item " + item);
            if (length > 0) {
                builder.append(" l");
                for (int j = 0; j < length; j++) {
                    builder.append("o");
                }
                builder.append("ng");
            }
            String itemString = builder.toString();
            TreeItem<Data> child = new TreeItem<Data>(new Data(itemString));
            if (level < 3 - 1) {
                addLevel(child, level + 1, length);
            }
            item.getChildren().add(child);
        }
    }
    
    @Test public void test_rt27180_collapseBranch_childSelected_singleSelection() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.SINGLE);
        
        treeTableView.setRoot(myCompanyRootNode);
        myCompanyRootNode.setExpanded(true);
        salesDepartment.setExpanded(true);
        itSupport.setExpanded(true);
        sm.select(2);                   // ethanWilliams
        assertFalse(sm.isSelected(1));  // salesDepartment
        assertTrue(sm.isSelected(2));   // ethanWilliams
        assertTrue(treeTableView.getFocusModel().isFocused(2));
        assertEquals(1, sm.getSelectedCells().size());
        
        // now collapse the salesDepartment, selection should
        // not jump down to the itSupport people
        salesDepartment.setExpanded(false);
        assertTrue(sm.getSelectedIndices().toString(), sm.isSelected(1));   // salesDepartment
        assertTrue(treeTableView.getFocusModel().isFocused(1));
        assertEquals(1, sm.getSelectedCells().size());
    }
    
    @Test public void test_rt27180_collapseBranch_laterSiblingSelected_singleSelection() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.SINGLE);
        
        treeTableView.setRoot(myCompanyRootNode);
        myCompanyRootNode.setExpanded(true);
        salesDepartment.setExpanded(true);
        itSupport.setExpanded(true);
        sm.select(8);                   // itSupport
        assertFalse(sm.isSelected(1));  // salesDepartment
        assertTrue(sm.isSelected(8));   // itSupport
        assertTrue(treeTableView.getFocusModel().isFocused(8));
        assertEquals(1, sm.getSelectedIndices().size());
        
        salesDepartment.setExpanded(false);
        assertTrue(debug(), sm.isSelected(2));   // itSupport
        assertTrue(treeTableView.getFocusModel().isFocused(2));
        assertEquals(1, sm.getSelectedIndices().size());
    }
    
    @Test public void test_rt27180_collapseBranch_laterSiblingAndChildrenSelected() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);
        
        treeTableView.setRoot(myCompanyRootNode);
        myCompanyRootNode.setExpanded(true);
        salesDepartment.setExpanded(true);
        itSupport.setExpanded(true);
        sm.selectIndices(8, 9, 10);     // itSupport, and two people
        assertFalse(sm.isSelected(1));  // salesDepartment
        assertTrue(sm.isSelected(8));   // itSupport
        assertTrue(sm.isSelected(9));   // mikeGraham
        assertTrue(sm.isSelected(10));  // judyMayer
        assertTrue(treeTableView.getFocusModel().isFocused(10));
        assertEquals(3, sm.getSelectedIndices().size());
        
        salesDepartment.setExpanded(false);
        assertTrue(debug(), sm.isSelected(2));   // itSupport
        assertTrue(sm.isSelected(3));   // mikeGraham
        assertTrue(sm.isSelected(4));   // judyMayer
        assertTrue(treeTableView.getFocusModel().isFocused(4));
        assertEquals(3, sm.getSelectedIndices().size());
    }
    
    @Test public void test_rt27180_expandBranch_laterSiblingSelected_singleSelection() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.SINGLE);
        
        treeTableView.setRoot(myCompanyRootNode);
        myCompanyRootNode.setExpanded(true);
        salesDepartment.setExpanded(false);
        itSupport.setExpanded(true);
        sm.select(2);                   // itSupport
        assertFalse(sm.isSelected(1));  // salesDepartment
        assertTrue(sm.isSelected(2));   // itSupport
        assertTrue(treeTableView.getFocusModel().isFocused(2));
        assertEquals(1, sm.getSelectedIndices().size());
        
        salesDepartment.setExpanded(true);
        assertTrue(debug(), sm.isSelected(8));   // itSupport
        assertTrue(treeTableView.getFocusModel().isFocused(8));
        assertEquals(1, sm.getSelectedIndices().size());
    }
    
    @Test public void test_rt27180_expandBranch_laterSiblingAndChildrenSelected() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);
        
        treeTableView.setRoot(myCompanyRootNode);
        myCompanyRootNode.setExpanded(true);
        salesDepartment.setExpanded(false);
        itSupport.setExpanded(true);
        sm.selectIndices(2,3,4);     // itSupport, and two people
        assertFalse(sm.isSelected(1));  // salesDepartment
        assertTrue(sm.isSelected(2));   // itSupport
        assertTrue(sm.isSelected(3));   // mikeGraham
        assertTrue(sm.isSelected(4));  // judyMayer
        assertTrue(treeTableView.getFocusModel().isFocused(4));
        assertEquals(3, sm.getSelectedIndices().size());
        
        salesDepartment.setExpanded(true);
        assertTrue(debug(), sm.isSelected(8));   // itSupport
        assertTrue(sm.isSelected(9));   // mikeGraham
        assertTrue(sm.isSelected(10));   // judyMayer
        assertTrue(treeTableView.getFocusModel().isFocused(10));
        assertEquals(3, sm.getSelectedIndices().size());
    }

    @Test public void test_rt30400() {
        // create a treetableview that'll render cells using the check box cell factory
        TreeItem<String> rootItem = new TreeItem<>("root");
        final TreeTableView<String> tableView = new TreeTableView<String>(rootItem);
        tableView.setMinHeight(100);
        tableView.setPrefHeight(100);

        TreeTableColumn firstNameCol = new TreeTableColumn("First Name");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));
        firstNameCol.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(new Callback<Integer, ObservableValue<Boolean>>() {
            public javafx.beans.value.ObservableValue<Boolean> call(Integer param) {
                return new ReadOnlyBooleanWrapper(true);
            }
        }));
        tableView.getColumns().add(firstNameCol);

        // because only the first row has data, all other rows should be
        // empty (and not contain check boxes - we just check the first four here)
        VirtualFlowTestUtils.assertRowsNotEmpty(tableView, 0, 1);
        VirtualFlowTestUtils.assertCellNotEmpty(VirtualFlowTestUtils.getCell(tableView, 0));
        VirtualFlowTestUtils.assertCellEmpty(VirtualFlowTestUtils.getCell(tableView, 1));
        VirtualFlowTestUtils.assertCellEmpty(VirtualFlowTestUtils.getCell(tableView, 2));
        VirtualFlowTestUtils.assertCellEmpty(VirtualFlowTestUtils.getCell(tableView, 3));
    }
}

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

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;
import static javafx.scene.control.TableColumn.SortType.DESCENDING;
import static org.junit.Assert.*;

import java.util.*;

import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.skin.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.cell.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import com.sun.javafx.tk.Toolkit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.scene.control.TableColumnComparatorBase.TableColumnComparator;
import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import com.sun.javafx.scene.control.test.Person;
import com.sun.javafx.scene.control.test.RT_22463_Person;

import static com.sun.javafx.scene.control.skin.TableColumnHeaderRetriever.*;

public class TableViewTest {
    private TableView<String> table;
    private TableView.TableViewSelectionModel sm;

    @Before public void setup() {
        table = new TableView<String>();
        sm = table.getSelectionModel();
    }
    

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void noArgConstructorSetsTheStyleClass() {
        assertStyleClassContains(table, "table-view");
    }

    @Test public void noArgConstructorSetsNonNullSelectionModel() {
        assertNotNull(sm);
    }

    @Test public void noArgConstructorSetsNonNullItems() {
        assertNotNull(table.getItems());
    }
    
    @Test public void noArgConstructorSetsNonNullSortPolicy() {
        assertNotNull(table.getSortPolicy());
    }
    
    @Test public void noArgConstructorSetsNullComparator() {
        assertNull(table.getComparator());
    }
    
    @Test public void noArgConstructorSetsNullOnSort() {
        assertNull(table.getOnSort());
    }

    @Test public void noArgConstructor_selectedItemIsNull() {
        assertNull(sm.getSelectedItem());
    }

    @Test public void noArgConstructor_selectedIndexIsNegativeOne() {
        assertEquals(-1, sm.getSelectedIndex());
    }

    @Test public void singleArgConstructorSetsTheStyleClass() {
        final TableView<String> b2 = new TableView<String>(FXCollections.observableArrayList("Hi"));
        assertStyleClassContains(b2, "table-view");
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
     * Tests for selection model                                         *
     ********************************************************************/

    @Test public void selectionModelCanBeNull() {
        table.setSelectionModel(null);
        assertNull(table.getSelectionModel());
    }

    @Test public void selectionModelCanBeBound() {
        TableView.TableViewSelectionModel<String> sm = new TableView.TableViewArrayListSelectionModel<String>(table);
        ObjectProperty<TableView.TableViewSelectionModel<String>> other = new SimpleObjectProperty<TableView.TableViewSelectionModel<String>>(sm);
        table.selectionModelProperty().bind(other);
        assertSame(sm, sm);
    }

    @Test public void selectionModelCanBeChanged() {
        TableView.TableViewSelectionModel<String> sm = new TableView.TableViewArrayListSelectionModel<String>(table);
        table.setSelectionModel(sm);
        assertSame(sm, sm);
    }

    @Test public void canSetSelectedItemToAnItemEvenWhenThereAreNoItems() {
        final String randomString = new String("I AM A CRAZY RANDOM STRING");
        sm.select(randomString);
        assertEquals(-1, sm.getSelectedIndex());
        assertSame(randomString, sm.getSelectedItem());
    }

    @Test public void canSetSelectedItemToAnItemNotInTheDataModel() {
        table.getItems().addAll("Apple", "Orange", "Banana");
        final String randomString = new String("I AM A CRAZY RANDOM STRING");
        sm.select(randomString);
        assertEquals(-1, sm.getSelectedIndex());
        assertSame(randomString, sm.getSelectedItem());
    }

    @Test public void settingTheSelectedItemToAnItemInItemsResultsInTheCorrectSelectedIndex() {
        table.getItems().addAll("Apple", "Orange", "Banana");
        sm.select("Orange");
        assertEquals(1, sm.getSelectedIndex());
        assertSame("Orange", sm.getSelectedItem());
    }

    @Test public void settingTheSelectedItemToANonexistantItemAndThenSettingItemsWhichContainsItResultsInCorrectSelectedIndex() {
        sm.select("Orange");
        table.getItems().addAll("Apple", "Orange", "Banana");
        assertEquals(1, sm.getSelectedIndex());
        assertSame("Orange", sm.getSelectedItem());
    }
    
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex0() {
        table.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(0);
        table.getItems().clear();
        assertEquals(-1, sm.getSelectedIndex());
    }
    
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex2() {
        table.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(2);
        table.getItems().clear();
        assertEquals(-1, sm.getSelectedIndex());
    }
    
    @Test public void ensureSelectedItemRemainsAccurateWhenItemsAreCleared() {
        table.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(2);
        table.getItems().clear();
        assertNull("Selected Item: " + sm.getSelectedItem(), sm.getSelectedItem());
        assertEquals(-1, sm.getSelectedIndex());
        
        table.getItems().addAll("Kiwifruit", "Mandarin", "Pineapple");
        sm.select(2);
        assertEquals("Pineapple", sm.getSelectedItem());
    }
    
    @Ignore("Not fixed yet")
    @Test public void ensureSelectionShiftsDownWhenOneNewItemIsAdded() {
        table.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertEquals(1, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
        
        table.getItems().add(0, "Kiwifruit");
        assertEquals(2, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
    }
    
    @Ignore("Not fixed yet")
    @Test public void ensureSelectionShiftsDownWhenMultipleNewItemAreAdded() {
        table.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertEquals(1, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
        
        table.getItems().addAll(0, Arrays.asList("Kiwifruit", "Pineapple", "Mandarin"));
        assertEquals("Orange", sm.getSelectedItem());
        assertEquals(4, sm.getSelectedIndex());
    }
    
    @Ignore("Not fixed yet")
    @Test public void ensureSelectionShiftsDownWhenOneItemIsRemoved() {
        table.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertEquals(1, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
        
        table.getItems().remove("Apple");
        assertEquals(0, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
    }
    
    @Ignore("Not fixed yet")
    @Test public void ensureSelectionShiftsDownWheMultipleItemsAreRemoved() {
        table.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(2);
        assertEquals(2, sm.getSelectedIndex());
        assertEquals("Banana", sm.getSelectedItem());
        
        table.getItems().removeAll(Arrays.asList("Apple", "Orange"));
        assertEquals(0, sm.getSelectedIndex());
        assertEquals("Banana", sm.getSelectedItem());
    }
    
    @Test public void ensureSelectionIsCorrectWhenItemsChange() {
        table.setItems(FXCollections.observableArrayList("Item 1"));
        sm.select(0);
        assertEquals("Item 1", sm.getSelectedItem());
        
        table.setItems(FXCollections.observableArrayList("Item 2"));
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals(null, sm.getSelectedItem());
    }

    /*********************************************************************
     * Tests for columns                                                 *
     ********************************************************************/

    @Test public void testColumns() {
        TableColumn col1 = new TableColumn();

        assertNotNull(table.getColumns());
        assertEquals(0, table.getColumns().size());

        table.getColumns().add(col1);
        assertEquals(1, table.getColumns().size());

        table.getColumns().remove(col1);
        assertEquals(0, table.getColumns().size());
    }

    @Test public void testVisibleLeafColumns() {
        TableColumn col1 = new TableColumn();

        assertNotNull(table.getColumns());
        assertEquals(0, table.getColumns().size());

        table.getColumns().add(col1);
        assertEquals(1, table.getVisibleLeafColumns().size());

        table.getColumns().remove(col1);
        assertEquals(0, table.getVisibleLeafColumns().size());
    }
    
    @Test public void testSortOrderCleanup() {
//        ObservableList<ObservablePerson> persons = ObservablePerson.createFXPersonList();
        TableView table = new TableView();
        TableColumn<String,String> first = new TableColumn<String,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        TableColumn<String,String> second = new TableColumn<String,String>("second");
        second.setCellValueFactory(new PropertyValueFactory("lastName"));
        table.getColumns().addAll(first, second);
        table.getSortOrder().setAll(first, second);
        table.getColumns().remove(first);
        assertFalse(table.getSortOrder().contains(first));
    } 
    
    
    /*********************************************************************
     * Tests for new sorting API in JavaFX 8.0                           *
     ********************************************************************/
    
    // TODO test for sort policies returning null
    // TODO test for changing column sortType out of order
    // TODO test comparator returns to original when sort fails / is consumed
    
    private static final Callback<TableView<String>, Boolean> NO_SORT_FAILED_SORT_POLICY = 
            new Callback<TableView<String>, Boolean>() {
        @Override public Boolean call(TableView<String> tableView) {
            return false;
        }
    };
    
    private static final Callback<TableView<String>, Boolean> SORT_SUCCESS_ASCENDING_SORT_POLICY = 
            new Callback<TableView<String>, Boolean>() {
        @Override public Boolean call(TableView<String> tableView) {
            if (tableView.getSortOrder().isEmpty()) return true;
            FXCollections.sort(tableView.getItems(), new Comparator<String>() {
                @Override public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            });
            return true;
        }
    };
    
    private TableColumn<String, String> initSortTestStructure() {
        TableColumn<String, String> col = new TableColumn<String, String>("column");
        col.setSortType(ASCENDING);
        col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<String, String>, ObservableValue<String>>() {
            @Override public ObservableValue<String> call(TableColumn.CellDataFeatures<String, String> param) {
                return new ReadOnlyObjectWrapper<String>(param.getValue());
            }
        });
        table.getColumns().add(col);
        table.getItems().addAll("Apple", "Orange", "Banana");
        return col;
    }
    
    @Ignore("This test is only valid if sort event consumption should revert changes")
    @Test public void testSortEventCanBeConsumedToStopSortOccurring_changeSortOrderList() {
        TableColumn<String, String> col = initSortTestStructure();
        table.setOnSort(new EventHandler<SortEvent<TableView<String>>>() {
            @Override public void handle(SortEvent<TableView<String>> event) {
                event.consume();
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        table.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        
        // the sort order list should be returned back to its original state
        assertTrue(table.getSortOrder().isEmpty());
    }
    
    @Test public void testSortEventCanBeNotConsumedToAllowSortToOccur_changeSortOrderList() {
        TableColumn<String, String> col = initSortTestStructure();
        table.setOnSort(new EventHandler<SortEvent<TableView<String>>>() {
            @Override public void handle(SortEvent<TableView<String>> event) {
                // do not consume here - this allows the sort to happen
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        table.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getSortOrder(), col);
    }
    
    @Ignore("This test is only valid if sort event consumption should revert changes")
    @Test public void testSortEventCanBeConsumedToStopSortOccurring_changeColumnSortType_AscendingToDescending() {
        TableColumn<String, String> col = initSortTestStructure();
        assertEquals(ASCENDING, col.getSortType());
        table.getSortOrder().add(col);
        table.setOnSort(new EventHandler<SortEvent<TableView<String>>>() {
            @Override public void handle(SortEvent<TableView<String>> event) {
                event.consume();
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        
        // when we change from ASCENDING to DESCENDING we don't expect the sort
        // to actually change (and in fact we expect the sort type to resort
        // back to being ASCENDING)
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        assertEquals(ASCENDING, col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getSortOrder(), col);
    }
    
    @Test public void testSortEventCanBeNotConsumedToAllowSortToOccur_changeColumnSortType_AscendingToDescending() {
        TableColumn<String, String> col = initSortTestStructure();
        assertEquals(ASCENDING, col.getSortType());
        table.getSortOrder().add(col);
        table.setOnSort(new EventHandler<SortEvent<TableView<String>>>() {
            @Override public void handle(SortEvent<TableView<String>> event) {
                // do not consume here - this allows the sort to happen
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        assertEquals(DESCENDING, col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getSortOrder(), col);
    }
    
    @Ignore("This test is only valid if sort event consumption should revert changes")
    @Test public void testSortEventCanBeConsumedToStopSortOccurring_changeColumnSortType_DescendingToNull() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        assertEquals(DESCENDING, col.getSortType());
        table.getSortOrder().add(col);
        table.setOnSort(new EventHandler<SortEvent<TableView<String>>>() {
            @Override public void handle(SortEvent<TableView<String>> event) {
                event.consume();
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        
        col.setSortType(null);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        assertEquals(DESCENDING, col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getSortOrder(), col);
    }
    
    @Test public void testSortEventCanBeNotConsumedToAllowSortToOccur_changeColumnSortType_DescendingToNull() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        assertEquals(DESCENDING, col.getSortType());
        table.getSortOrder().add(col);
        table.setOnSort(new EventHandler<SortEvent<TableView<String>>>() {
            @Override public void handle(SortEvent<TableView<String>> event) {
                // do not consume here - this allows the sort to happen
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        
        col.setSortType(null);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        assertNull(col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getSortOrder(), col);
    }
    
    @Ignore("This test is only valid if sort event consumption should revert changes")
    @Test public void testSortEventCanBeConsumedToStopSortOccurring_changeColumnSortType_NullToAscending() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(null);
        assertNull(col.getSortType());
        table.getSortOrder().add(col);
        table.setOnSort(new EventHandler<SortEvent<TableView<String>>>() {
            @Override public void handle(SortEvent<TableView<String>> event) {
                event.consume();
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        
        col.setSortType(ASCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        assertNull(col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getSortOrder(), col);
    }
    
    @Test public void testSortEventCanBeNotConsumedToAllowSortToOccur_changeColumnSortType_NullToAscending() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(null);
        assertNull(col.getSortType());
        table.getSortOrder().add(col);
        table.setOnSort(new EventHandler<SortEvent<TableView<String>>>() {
            @Override public void handle(SortEvent<TableView<String>> event) {
                // do not consume here - this allows the sort to happen
            }
        });
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        
        col.setSortType(ASCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        assertEquals(ASCENDING, col.getSortType());
        
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getSortOrder(), col);
    }

    @Test public void testSortMethodWithNullSortPolicy() {
        TableColumn<String, String> col = initSortTestStructure();
        table.setSortPolicy(null);
        assertNull(table.getSortPolicy());
        table.sort();
    }
    
    @Test public void testChangingSortPolicyUpdatesItemsList() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        table.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        table.setSortPolicy(SORT_SUCCESS_ASCENDING_SORT_POLICY);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
    }
    
    @Test public void testChangingSortPolicyDoesNotUpdateItemsListWhenTheSortOrderListIsEmpty() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        
        table.setSortPolicy(SORT_SUCCESS_ASCENDING_SORT_POLICY);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortOrderAddition() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);
        
        table.getSortOrder().add(col);
        
        // no sort should be run (as we have a custom sort policy), and the 
        // sortOrder list should be empty as the sortPolicy failed
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        assertTrue(table.getSortOrder().isEmpty());
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortOrderRemoval() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        table.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        // even though we remove the column from the sort order here, because the
        // sort policy fails the items list should remain unchanged and the sort
        // order list should continue to have the column in it.
        table.getSortOrder().remove(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getSortOrder(), col);
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortTypeChange_ascendingToDescending() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(ASCENDING);
        table.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        assertEquals(ASCENDING, col.getSortType());
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortTypeChange_descendingToNull() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        table.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        col.setSortType(null);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        assertEquals(DESCENDING, col.getSortType());
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortTypeChange_nullToAscending() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(null);
        table.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        col.setSortType(ASCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        assertNull(col.getSortType());
    }
    
    @Test public void testComparatorChangesInSyncWithSortOrder_1() {
        TableColumn<String, String> col = initSortTestStructure();
        assertNull(table.getComparator());
        assertTrue(table.getSortOrder().isEmpty());
        
        table.getSortOrder().add(col);
        TableColumnComparator c = (TableColumnComparator)table.getComparator();
        assertNotNull(c);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(c.getColumns(), col);
    }
    
    @Ignore
    @Test public void testComparatorChangesInSyncWithSortOrder_2() {
        // same as test above
        TableColumn<String, String> col = initSortTestStructure();
        assertNull(table.getComparator());
        assertTrue(table.getSortOrder().isEmpty());
        
        table.getSortOrder().add(col);
        TableColumnComparator c = (TableColumnComparator)table.getComparator();
        assertNotNull(c);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(c.getColumns(), col);
        
        // now remove column from sort order, and the comparator should go to
        // being null
        table.getSortOrder().remove(col);
        assertNull(table.getComparator());
    }
    
    @Test public void testFailedSortPolicyBacksOutComparatorChange_sortOrderAddition() {
        TableColumn<String, String> col = initSortTestStructure();
        final TableColumnComparator oldComparator = (TableColumnComparator)table.getComparator();
        
        col.setSortType(DESCENDING);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);
        
        table.getSortOrder().add(col);
        
        assertEquals(oldComparator, table.getComparator());
    }
    
    @Test public void testFailedSortPolicyBacksOutComparatorChange_sortOrderRemoval() {
        TableColumn<String, String> col = initSortTestStructure();
        TableColumnComparator oldComparator = (TableColumnComparator)table.getComparator();
        assertNull(oldComparator);

        col.setSortType(DESCENDING);
        table.getSortOrder().add(col);
        VirtualFlowTestUtils.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        oldComparator = (TableColumnComparator)table.getComparator();
        VirtualFlowTestUtils.assertListContainsItemsInOrder(oldComparator.getColumns(), col);
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);
        table.getSortOrder().remove(col);
        
        assertTrue(table.getSortOrder().contains(col));
        VirtualFlowTestUtils.assertListContainsItemsInOrder(oldComparator.getColumns(), col);
    }
    
    @Test public void testFailedSortPolicyBacksOutComparatorChange_sortTypeChange() {
        TableColumn<String, String> col = initSortTestStructure();
        final TableColumnComparator oldComparator = (TableColumnComparator)table.getComparator();
        assertNull(oldComparator);
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);
        table.getSortOrder().add(col);
        col.setSortType(ASCENDING);
        
        assertTrue(table.getSortOrder().isEmpty());
        assertNull(oldComparator);
    }

    @Test public void testComparatorIsNullWhenSortOrderListIsEmpty() {
        TableColumn<String, String> col = initSortTestStructure();

        assertNull(table.getComparator());

        table.getSortOrder().add(col);
        assertFalse(table.getSortOrder().isEmpty());
        assertNotNull(table.getComparator());

        table.getSortOrder().clear();
        assertTrue(table.getSortOrder().isEmpty());
        assertNull(table.getComparator());
    }
    
    
    
    /*********************************************************************
     * Tests for specific bugs                                           *
     ********************************************************************/
    @Test public void test_rt16019() {
        // RT-16019: NodeMemory TableView tests fail with 
        // IndexOutOfBoundsException (ObservableListWrapper.java:336)
        TableView table = new TableView();
        for (int i = 0; i < 1000; i++) {
            table.getItems().add("data " + i);
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
    
    @Test public void test_rt18385() {
        table.getItems().addAll("row1", "row2", "row3");
        sm.select(1);
        table.getItems().add("Another Row");
        assertEquals(1, sm.getSelectedIndices().size());
        assertEquals(1, sm.getSelectedItems().size());
        assertEquals(1, sm.getSelectedCells().size());
    }
    
    @Test public void test_rt18339_onlyEditWhenTableViewIsEditable_tableEditableIsFalse_columnEditableIsFalse() {
        TableColumn<String,String> first = new TableColumn<String,String>("first");
        first.setEditable(false);
        table.getColumns().add(first);
        table.setEditable(false);
        table.edit(1, first);
        assertEquals(null, table.getEditingCell());
    }
    
    @Test public void test_rt18339_onlyEditWhenTableViewIsEditable_tableEditableIsFalse_columnEditableIsTrue() {
        TableColumn<String,String> first = new TableColumn<String,String>("first");
        first.setEditable(true);
        table.getColumns().add(first);
        table.setEditable(false);
        table.edit(1, first);
        assertEquals(null, table.getEditingCell());
    }
    
    @Test public void test_rt18339_onlyEditWhenTableViewIsEditable_tableEditableIsTrue_columnEditableIsFalse() {
        TableColumn<String,String> first = new TableColumn<String,String>("first");
        first.setEditable(false);
        table.getColumns().add(first);
        table.setEditable(true);
        table.edit(1, first);
        assertEquals(null, table.getEditingCell());
    }
    
    @Test public void test_rt18339_onlyEditWhenTableViewIsEditable_tableEditableIsTrue_columnEditableIsTrue() {
        TableColumn<String,String> first = new TableColumn<String,String>("first");
        first.setEditable(true);
        table.getColumns().add(first);
        table.setEditable(true);
        table.edit(1, first);
        assertEquals(new TablePosition(table, 1, first), table.getEditingCell());
    }
    
    @Test public void test_rt14451() {
        table.getItems().addAll("Apple", "Orange", "Banana");
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.selectRange(0, 2); // select from 0 (inclusive) to 2 (exclusive)
        assertEquals(2, sm.getSelectedIndices().size());
    }
    
    @Test public void test_rt21586() {
        table.getItems().setAll("Apple", "Orange", "Banana");
        table.getSelectionModel().select(1);
        assertEquals(1, table.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", table.getSelectionModel().getSelectedItem());
        
        table.getItems().setAll("Kiwifruit", "Pineapple", "Grape");
        assertEquals(-1, table.getSelectionModel().getSelectedIndex());
        assertNull(table.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt27820_1() {
        table.getItems().setAll("Apple", "Orange");
        table.getSelectionModel().select(0);
        assertEquals(1, table.getSelectionModel().getSelectedItems().size());
        assertEquals("Apple", table.getSelectionModel().getSelectedItem());
        
        table.getItems().clear();
        assertEquals(0, table.getSelectionModel().getSelectedItems().size());
        assertNull(table.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt27820_2() {
        table.getItems().setAll("Apple", "Orange");
        table.getSelectionModel().select(1);
        assertEquals(1, table.getSelectionModel().getSelectedItems().size());
        assertEquals("Orange", table.getSelectionModel().getSelectedItem());
        
        table.getItems().clear();
        assertEquals(0, table.getSelectionModel().getSelectedItems().size());
        assertNull(table.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt28534() {
        TableView<Person> table = new TableView<Person>();
        table.setItems(FXCollections.observableArrayList(
            new Person("Jacob", "Smith", "jacob.smith@example.com"),
            new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
            new Person("Ethan", "Williams", "ethan.williams@example.com"),
            new Person("Emma", "Jones", "emma.jones@example.com"),
            new Person("Michael", "Brown", "michael.brown@example.com")));
        
        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));

        TableColumn emailCol = new TableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));

        table.getColumns().addAll(firstNameCol, lastNameCol, emailCol);
        
        VirtualFlowTestUtils.assertRowsNotEmpty(table, 0, 5); // rows 0 - 5 should be filled
        VirtualFlowTestUtils.assertRowsEmpty(table, 5, -1); // rows 5+ should be empty
        
        // now we replace the data and expect the cells that have no data
        // to be empty
        table.setItems(FXCollections.observableArrayList(
            new Person("*_*Emma", "Jones", "emma.jones@example.com"),
            new Person("_Michael", "Brown", "michael.brown@example.com")));
        
        VirtualFlowTestUtils.assertRowsNotEmpty(table, 0, 2); // rows 0 - 2 should be filled
        VirtualFlowTestUtils.assertRowsEmpty(table, 2, -1); // rows 2+ should be empty
    }
    
    @Test public void test_rt22463() {
        final TableView<RT_22463_Person> table = new TableView<RT_22463_Person>();
        table.setTableMenuButtonVisible(true);
        TableColumn c1 = new TableColumn("Id");
        TableColumn c2 = new TableColumn("Name");
        c1.setCellValueFactory(new PropertyValueFactory<Person, Long>("id"));
        c2.setCellValueFactory(new PropertyValueFactory<Person, String>("name"));
        table.getColumns().addAll(c1, c2);
        
        // before the change things display fine
        RT_22463_Person p1 = new RT_22463_Person();
        p1.setId(1l);
        p1.setName("name1");
        RT_22463_Person p2 = new RT_22463_Person();
        p2.setId(2l);
        p2.setName("name2");
        table.setItems(FXCollections.observableArrayList(p1, p2));
        VirtualFlowTestUtils.assertCellTextEquals(table, 0, "1", "name1");
        VirtualFlowTestUtils.assertCellTextEquals(table, 1, "2", "name2");
        
        // now we change the persons but they are still equal as the ID's don't
        // change - but the items list is cleared so the cells should update
        RT_22463_Person new_p1 = new RT_22463_Person();
        new_p1.setId(1l);
        new_p1.setName("updated name1");
        RT_22463_Person new_p2 = new RT_22463_Person();
        new_p2.setId(2l);
        new_p2.setName("updated name2");
        table.getItems().clear();
        table.setItems(FXCollections.observableArrayList(new_p1, new_p2));
        VirtualFlowTestUtils.assertCellTextEquals(table, 0, "1", "updated name1");
        VirtualFlowTestUtils.assertCellTextEquals(table, 1, "2", "updated name2");
    }
    
    @Test public void test_rt28637() {
        ObservableList<String> items = FXCollections.observableArrayList("String1", "String2", "String3", "String4");
        
        final TableView<String> tableView = new TableView<String>();
        final MultipleSelectionModel sm = tableView.getSelectionModel();
        tableView.setItems(items);
        
        tableView.getSelectionModel().select(0);
        assertEquals("String1", sm.getSelectedItem());
        assertEquals(1, sm.getSelectedItems().size());
        assertEquals("String1", sm.getSelectedItems().get(0));
        assertEquals(0, sm.getSelectedIndex());
        
        items.remove(sm.getSelectedItem());
        assertEquals("String2", sm.getSelectedItem());
        assertEquals(1, sm.getSelectedItems().size());
        assertEquals("String2", sm.getSelectedItems().get(0));
        assertEquals(0, sm.getSelectedIndex());
    }
    
    @Test public void test_rt24844() {
        // p1 == lowest first name
        Person p0, p1, p2, p3, p4;
        
        TableView<Person> table = new TableView<Person>();
        table.setItems(FXCollections.observableArrayList(
            p3 = new Person("Jacob", "Smith", "jacob.smith@example.com"),
            p2 = new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
            p1 = new Person("Ethan", "Williams", "ethan.williams@example.com"),
            p0 = new Person("Emma", "Jones", "emma.jones@example.com"),
            p4 = new Person("Michael", "Brown", "michael.brown@example.com")));
        
        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));
        
        // set dummy comparator to lock items in place until new comparator is set
        firstNameCol.setComparator(new Comparator() {
            @Override public int compare(Object t, Object t1) {
                return 0;
            }
        });

        table.getColumns().addAll(firstNameCol);
        table.getSortOrder().add(firstNameCol);
        
        // ensure the existing order is as expected
        assertEquals(p3, table.getItems().get(0));
        assertEquals(p2, table.getItems().get(1));
        assertEquals(p1, table.getItems().get(2));
        assertEquals(p0, table.getItems().get(3));
        assertEquals(p4, table.getItems().get(4));
        
        // set a new comparator
        firstNameCol.setComparator(new Comparator() {
            Random r =  new Random();
            @Override public int compare(Object t, Object t1) {
                return t.toString().compareTo(t1.toString());
            }
        });
        
        // ensure the new order is as expected
        assertEquals(p0, table.getItems().get(0));
        assertEquals(p1, table.getItems().get(1));
        assertEquals(p2, table.getItems().get(2));
        assertEquals(p3, table.getItems().get(3));
        assertEquals(p4, table.getItems().get(4));
    }
    
    @Test public void test_rt29331() {
        TableView<Person> table = new TableView<Person>();
        
        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));

        TableColumn emailCol = new TableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));
        
        TableColumn parentColumn = new TableColumn<>("Parent");
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
        TableView<Person> table = new TableView<Person>();
        table.setItems(FXCollections.observableArrayList(
              new Person("Jacob", "Smith", "jacob.smith@example.com"),
              new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
              new Person("Ethan", "Williams", "ethan.williams@example.com"),
              new Person("Emma", "Jones", "emma.jones@example.com"),
              new Person("Michael", "Brown", "michael.brown@example.com")));
        
        TableColumn parentColumn = new TableColumn<>("Parent");
        table.getColumns().addAll(parentColumn);
        
        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));
        
        parentColumn.getColumns().addAll(firstNameCol, lastNameCol);

        table.setOnSort(new EventHandler<SortEvent<TableView<Person>>>() {
            @Override public void handle(SortEvent<TableView<Person>> event) {
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
        TableView<Person> table = new TableView<Person>();
        table.setItems(FXCollections.observableArrayList(
              new Person("Jacob", "Smith", "jacob.smith@example.com"),
              new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
              new Person("Ethan", "Williams", "ethan.williams@example.com"),
              new Person("Emma", "Jones", "emma.jones@example.com"),
              new Person("Michael", "Brown", "michael.brown@example.com")));
        
        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));
        
        // this test differs from the previous one by installing the parent column
        // into the tableview after it has the children added into it
        TableColumn parentColumn = new TableColumn<>("Parent");
        parentColumn.getColumns().addAll(firstNameCol, lastNameCol);
        table.getColumns().addAll(parentColumn);

        table.setOnSort(new EventHandler<SortEvent<TableView<Person>>>() {
            @Override public void handle(SortEvent<TableView<Person>> event) {
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
        TableView<Person> table = new TableView<Person>();
        table.setItems(FXCollections.observableArrayList(
              new Person("Jacob", "Smith", "jacob.smith@example.com"),
              new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
              new Person("Ethan", "Williams", "ethan.williams@example.com"),
              new Person("Emma", "Jones", "emma.jones@example.com"),
              new Person("Michael", "Brown", "michael.brown@example.com")));
        
        TableSelectionModel sm = table.getSelectionModel();
        
        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));
        
        TableColumn emailCol = new TableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));
        
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
        Person p0, p1;
        TableView<Person> table = new TableView<Person>();
        table.setItems(FXCollections.observableArrayList(
              p0 = new Person("Jacob", "Smith", "jacob.smith@example.com"),
              p1 = new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
              new Person("Ethan", "Williams", "ethan.williams@example.com"),
              new Person("Emma", "Jones", "emma.jones@example.com"),
              new Person("Michael", "Brown", "michael.brown@example.com")));
        
        TableSelectionModel sm = table.getSelectionModel();
        
        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));
        
        TableColumn emailCol = new TableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));
        
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
        TableView<Person> table = new TableView<Person>();
        table.setItems(FXCollections.observableArrayList(
              new Person("Jacob", "Smith", "jacob.smith@example.com"),
              new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
              new Person("Ethan", "Williams", "ethan.williams@example.com"),
              new Person("Emma", "Jones", "emma.jones@example.com"),
              new Person("Michael", "Brown", "michael.brown@example.com")));
        
        TableSelectionModel sm = table.getSelectionModel();
        
        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        TableColumn lastNameCol = new TableColumn("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));
        
        TableColumn emailCol = new TableColumn("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<Person, String>("email"));
        
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
        final TableView<Person> tableView = new TableView<Person>();
        tableView.setMaxHeight(50);
        tableView.setPrefHeight(50);
        tableView.setItems(FXCollections.observableArrayList(
            new Person("Jacob", "Smith", "jacob.smith@example.com"),
            new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
            new Person("Ethan", "Williams", "ethan.williams@example.com"),
            new Person("Emma", "Jones", "emma.jones@example.com"),
            new Person("Jacob", "Smith", "jacob.smith@example.com"),
            new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
            new Person("Ethan", "Williams", "ethan.williams@example.com"),
            new Person("Emma", "Jones", "emma.jones@example.com"),
            new Person("Jacob", "Smith", "jacob.smith@example.com"),
            new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
            new Person("Ethan", "Williams", "ethan.williams@example.com"),
            new Person("Emma", "Jones", "emma.jones@example.com"),
            new Person("Jacob", "Smith", "jacob.smith@example.com"),
            new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
            new Person("Ethan", "Williams", "ethan.williams@example.com"),
            new Person("Emma", "Jones", "emma.jones@example.com")
        ));
        
        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));
        
        tableView.getColumns().add(firstNameCol);
        
        // we want the vertical scrollbar
        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowVerticalScrollbar(tableView);
        
        assertNotNull(scrollBar);
        assertTrue(scrollBar.isVisible());
        assertTrue(scrollBar.getVisibleAmount() > 0.0);
        assertTrue(scrollBar.getVisibleAmount() < 1.0);
        
        // this next test is likely to be brittle, but we'll see...If it is the
        // cause of failure then it can be commented out
        assertEquals(0.0625, scrollBar.getVisibleAmount(), 0.0);
    }

    @Test public void test_rt30400() {
        // create a listview that'll render cells using the check box cell factory
        final TableView<Person> tableView = new TableView<Person>();
        tableView.setMinHeight(100);
        tableView.setPrefHeight(100);
        tableView.setItems(FXCollections.observableArrayList(
                new Person("Jacob", "Smith", "jacob.smith@example.com")
        ));

        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));
        firstNameCol.setCellFactory(CheckBoxTableCell.forTableColumn(new Callback<Integer, ObservableValue<Boolean>>() {
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

    @Test public void test_rt31165() {
        final ObservableList names = FXCollections.observableArrayList("Adam", "Alex", "Alfred", "Albert");

        final TableView<Person> tableView = new TableView<Person>();
        tableView.setEditable(true);
        tableView.setItems(FXCollections.observableArrayList(
            new Person("Jacob", "Smith", "jacob.smith@example.com"),
            new Person("Jim", "Bob", "jim.bob@example.com")
        ));

        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));
        firstNameCol.setCellFactory(ChoiceBoxTableCell.forTableColumn(names));
        firstNameCol.setEditable(true);

        tableView.getColumns().add(firstNameCol);

        IndexedCell cell = VirtualFlowTestUtils.getCell(tableView, 1, 0);
        assertEquals("Jim", cell.getText());
        assertFalse(cell.isEditing());

        tableView.edit(1, firstNameCol);

        TablePosition editingCell = tableView.getEditingCell();
        assertEquals(1, editingCell.getRow());
        assertEquals(firstNameCol, editingCell.getTableColumn());
        assertTrue(cell.isEditing());

        VirtualFlowTestUtils.getVirtualFlow(tableView).requestLayout();
        Toolkit.getToolkit().firePulse();

        editingCell = tableView.getEditingCell();
        assertEquals(1, editingCell.getRow());
        assertEquals(firstNameCol, editingCell.getTableColumn());
        assertTrue(cell.isEditing());
    }

    @Test public void test_rt31471() {
        Person jacobSmith;
        final TableView<Person> tableView = new TableView<Person>();
        tableView.setItems(FXCollections.observableArrayList(
                jacobSmith = new Person("Jacob", "Smith", "jacob.smith@example.com"),
                new Person("Jim", "Bob", "jim.bob@example.com")
        ));

        TableColumn firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));
        tableView.getColumns().add(firstNameCol);

        IndexedCell cell = VirtualFlowTestUtils.getCell(tableView, 0);
        assertEquals(jacobSmith, cell.getItem());

        tableView.setFixedCellSize(50);

        VirtualFlowTestUtils.getVirtualFlow(tableView).requestLayout();
        Toolkit.getToolkit().firePulse();

        assertEquals(jacobSmith, cell.getItem());
        assertEquals(50, cell.getHeight(), 0.00);
    }

    private int rt_31200_count = 0;
    @Test public void test_rt_31200_tableCell() {
        rt_31200_count = 0;
        final TableView<Person> tableView = new TableView<Person>();
        tableView.setItems(FXCollections.observableArrayList(
                new Person("Jacob", "Smith", "jacob.smith@example.com"),
                new Person("Jim", "Bob", "jim.bob@example.com")
        ));

        TableColumn<Person,String> firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));
        tableView.getColumns().add(firstNameCol);

        firstNameCol.setCellFactory(new Callback<TableColumn<Person,String>, TableCell<Person, String>>() {
            @Override
            public TableCell<Person, String> call(TableColumn<Person,String> param) {
                return new TableCell<Person, String>() {
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
                            setText(item);
                        }
                    }
                };
            }
        });

        StageLoader sl = new StageLoader(tableView);

        assertEquals(14, rt_31200_count);

        // resize the stage
        sl.getStage().setHeight(250);
        Toolkit.getToolkit().firePulse();
        sl.getStage().setHeight(50);
        Toolkit.getToolkit().firePulse();
        assertEquals(14, rt_31200_count);
    }

    @Test public void test_rt_31200_tableRow() {
        rt_31200_count = 0;
        final TableView<Person> tableView = new TableView<Person>();
        tableView.setItems(FXCollections.observableArrayList(
                new Person("Jacob", "Smith", "jacob.smith@example.com"),
                new Person("Jim", "Bob", "jim.bob@example.com")
        ));

        TableColumn<Person,String> firstNameCol = new TableColumn("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));
        tableView.getColumns().add(firstNameCol);

        tableView.setRowFactory(new Callback<TableView<Person>, TableRow<Person>>() {
            @Override
            public TableRow<Person> call(TableView<Person> param) {
                return new TableRow<Person>() {
                    ImageView view = new ImageView();
                    { setGraphic(view); };

                    @Override
                    protected void updateItem(Person item, boolean empty) {
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

        StageLoader sl = new StageLoader(tableView);

        assertEquals(14, rt_31200_count);

        // resize the stage
        sl.getStage().setHeight(250);
        Toolkit.getToolkit().firePulse();
        sl.getStage().setHeight(50);
        Toolkit.getToolkit().firePulse();
        assertEquals(14, rt_31200_count);
    }

    @Test public void test_rt_31727() {
        TableView table = new TableView();
        TableColumn<String,String> first = new TableColumn<String,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        TableColumn<String,String> second = new TableColumn<String,String>("second");
        second.setCellValueFactory(new PropertyValueFactory("lastName"));
        table.getColumns().addAll(first, second);

        table.setItems(FXCollections.observableArrayList(
                new Person("Jacob", "Smith", "jacob.smith@example.com"),
                new Person("Jim", "Bob", "jim.bob@example.com")
        ));

        table.setEditable(true);
        first.setEditable(true);

        // do a normal edit
        table.edit(0, first);
        TablePosition editingCell = table.getEditingCell();
        assertNotNull(editingCell);
        assertEquals(0, editingCell.getRow());
        assertEquals(0, editingCell.getColumn());
        assertEquals(first, editingCell.getTableColumn());
        assertEquals(table, editingCell.getTableView());

        // cancel editing
        table.edit(-1, null);
        editingCell = table.getEditingCell();
        assertNull(editingCell);
    }

    @Test public void test_rt_21517() {
        TableView table = new TableView();
        TableColumn<String,String> first = new TableColumn<String,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        TableColumn<String,String> second = new TableColumn<String,String>("second");
        second.setCellValueFactory(new PropertyValueFactory("lastName"));
        table.getColumns().addAll(first, second);

        Person aaa,bbb,ccc;
        table.setItems(FXCollections.observableArrayList(
                aaa = new Person("AAA", "Smith", "jacob.smith@example.com"),
                bbb = new Person("BBB", "Bob", "jim.bob@example.com"),
                ccc = new Person("CCC", "Giles", "jim.bob@example.com")
        ));

        final TableSelectionModel sm = table.getSelectionModel();

        // test pre-conditions
        assertTrue(sm.isEmpty());

        // select the 3rd row (that is, CCC)
        sm.select(2);
        assertTrue(sm.isSelected(2));
        assertEquals(2, sm.getSelectedIndex());
        assertEquals(1, sm.getSelectedIndices().size());
        assertTrue(sm.getSelectedIndices().contains(2));
        assertEquals(ccc, sm.getSelectedItem());
        assertEquals(1, sm.getSelectedItems().size());
        assertTrue(sm.getSelectedItems().contains(ccc));

        // we also want to test visually
        TableRow aaaRow = (TableRow) VirtualFlowTestUtils.getCell(table, 0);
        assertFalse(aaaRow.isSelected());
        TableRow cccRow = (TableRow) VirtualFlowTestUtils.getCell(table, 2);
        assertTrue(cccRow.isSelected());

        // sort tableview by firstname column in ascending (default) order
        // (so aaa continues to come first)
        table.getSortOrder().add(first);

        // nothing should have changed
        assertTrue(sm.isSelected(2));
        assertEquals(2, sm.getSelectedIndex());
        assertEquals(1, sm.getSelectedIndices().size());
        assertTrue(sm.getSelectedIndices().contains(2));
        assertEquals(ccc, sm.getSelectedItem());
        assertEquals(1, sm.getSelectedItems().size());
        assertTrue(sm.getSelectedItems().contains(ccc));
        aaaRow = (TableRow) VirtualFlowTestUtils.getCell(table, 0);
        assertFalse(aaaRow.isSelected());
        cccRow = (TableRow) VirtualFlowTestUtils.getCell(table, 2);
        assertTrue(cccRow.isSelected());

        // continue to sort tableview by firstname column, but now in descending
        // order, (so ccc to come first)
        first.setSortType(TableColumn.SortType.DESCENDING);

        // now test to ensure that CCC is still the only selected item, but now
        // located in index 0
        assertTrue(sm.isSelected(0));
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(1, sm.getSelectedIndices().size());
        assertTrue(sm.getSelectedIndices().contains(0));
        assertEquals(ccc, sm.getSelectedItem());
        assertEquals(1, sm.getSelectedItems().size());
        assertTrue(sm.getSelectedItems().contains(ccc));

        // we also want to test visually
        aaaRow = (TableRow) VirtualFlowTestUtils.getCell(table, 1);
        assertFalse(aaaRow.isSelected());
        cccRow = (TableRow) VirtualFlowTestUtils.getCell(table, 0);
        assertTrue(cccRow.isSelected());
    }

    @Test public void test_rt_30484_tableCell() {
        TableView<Person> table = new TableView<>();
        TableColumn<Person,String> first = new TableColumn<>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        table.getColumns().addAll(first);

        table.setItems(FXCollections.observableArrayList(
                new Person("AAA", "Smith", "jacob.smith@example.com"),
                new Person("BBB", "Bob", "jim.bob@example.com")
        ));

        first.setCellFactory(new Callback<TableColumn<Person, String>, TableCell<Person, String>>() {
            @Override
            public TableCell<Person, String> call(TableColumn<Person, String> param) {
                return new TableCell<Person, String>() {
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

        // First two rows have content, so the graphic should show.
        // All other rows have no content, so graphic should not show.

        VirtualFlowTestUtils.assertGraphicIsVisible(table,    0, 0);
        VirtualFlowTestUtils.assertGraphicIsVisible(table,    1, 0);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(table, 2, 0);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(table, 3, 0);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(table, 4, 0);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(table, 5, 0);
    }

    @Test public void test_rt_30484_tableRow() {
        TableView<Person> table = new TableView<>();
        TableColumn<Person,String> first = new TableColumn<Person,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        table.getColumns().addAll(first);

        table.setItems(FXCollections.observableArrayList(
                new Person("AAA", "Smith", "jacob.smith@example.com"),
                new Person("BBB", "Bob", "jim.bob@example.com")
        ));

        table.setRowFactory(new Callback<TableView<Person>, TableRow<Person>>() {
            @Override public TableRow<Person> call(TableView<Person> param) {
                return new TableRow<Person>() {
                    Rectangle graphic = new Rectangle(10, 10, Color.RED);
                    { setGraphic(graphic); };

                    @Override protected void updateItem(Person item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            graphic.setVisible(false);
                            setText(null);
                        } else {
                            graphic.setVisible(true);
                            setText(item.toString());
                        }
                    }
                };
            }
        });

        // First two rows have content, so the graphic should show.
        // All other rows have no content, so graphic should not show.

        VirtualFlowTestUtils.assertGraphicIsVisible(table,    0);
        VirtualFlowTestUtils.assertGraphicIsVisible(table,    1);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(table, 2);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(table, 3);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(table, 4);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(table, 5);
    }

    @Test public void test_rt_32201() {
        final int numRows = 150;
        final int numColumns = 30;

        // create data
        final ObservableList<List<Object>> bigData = FXCollections.observableArrayList();
        for (int row = bigData.size(); row < numRows; row++) {
            List<Object> line = new ArrayList<>();
            for (int col = 0; col <= numColumns; col++) {
                double value = (col == 0) ? (double)row : Math.random() * 1000;
                line.add(value);
            }
            bigData.add(line);
        }

        TableView<List<Object>> table = new TableView<>(bigData);

        // create columns
        for (int i = 0; i <= numColumns; i++) {
            TableColumn<List<Object>,Object> col = new TableColumn<>("Col" + i);
            final int coli = i;
            col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<List<Object>,Object>, ObservableValue<Object>>() {
                public ObservableValue<Object> call(TableColumn.CellDataFeatures<List<Object>,Object> p) {
                    return new ReadOnlyObjectWrapper(p.getValue().get(coli));
                }
            });

            table.getColumns().add(col);
        }

        // start test
        assertNull(table.getComparator());
        assertTrue(table.getSortOrder().isEmpty());

        table.getSortOrder().add(table.getColumns().get(0));
        assertNotNull(table.getComparator());
        assertTrue(table.getSortOrder().contains(table.getColumns().get(0)));
        assertEquals(1, table.getSortOrder().size());

        // remove column again
        try {
            table.getSortOrder().clear();
        } catch (ClassCastException e) {
            fail("Comparator should be null as the sort order list is empty.");
        }
        assertNull(table.getComparator());
        assertTrue(table.getSortOrder().isEmpty());
    }

    private int rt_31015_count = 0;
    @Test public void test_rt_31015() {
        TableView<Person> table = new TableView<>();
        table.setEditable(true);
        TableColumn<Person,String> first = new TableColumn<Person,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        table.getColumns().addAll(first);

        table.setItems(FXCollections.observableArrayList(
            new Person("John", "Smith", "jacob.smith@example.com")
        ));

        //Set cell factory for cells that allow editing
        Callback<TableColumn<Person,String>, TableCell<Person, String>> cellFactory = new Callback<TableColumn<Person,String>, TableCell<Person, String>>() {
            public TableCell<Person, String> call(TableColumn<Person, String> p) {
                return new TableCell<Person, String>() {
                    @Override public void cancelEdit() {
                        super.cancelEdit();
                        rt_31015_count++;
                    }
                };
            }
        };
        first.setCellFactory(cellFactory);

        new StageLoader(table);

        assertEquals(0, rt_31015_count);

        table.edit(0, first);
        assertEquals(0, rt_31015_count);

        table.edit(-1, null);
        assertEquals(1, rt_31015_count);
    }

    @Test public void test_rt_31653() {
        TableView<Person> table = new TableView<>();
        TableColumn<Person,String> first = new TableColumn<Person,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        table.getColumns().addAll(first);

        table.setItems(FXCollections.observableArrayList(
                new Person("John", "Smith", "jacob.smith@example.com")
        ));

        TableRow<Person> rowCell = (TableRow<Person>)VirtualFlowTestUtils.getCell(table, 0);
        final double initialWidth = rowCell.computePrefWidth(-1);

        first.setPrefWidth(200);

        final double newWidth = rowCell.computePrefWidth(-1);
        assertEquals(200, newWidth, 0.0);
        assertTrue(initialWidth != newWidth);
    }

    private int rt_29650_start_count = 0;
    private int rt_29650_commit_count = 0;
    private int rt_29650_cancel_count = 0;
    @Test public void test_rt_29650() {
        TableView<Person> table = new TableView<>();
        table.setEditable(true);
        table.setItems(FXCollections.observableArrayList(
                new Person("John", "Smith", "jacob.smith@example.com")
        ));

        TableColumn<Person,String> first = new TableColumn<Person,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        Callback<TableColumn<Person, String>, TableCell<Person, String>> factory = TextFieldTableCell.forTableColumn();
        first.setCellFactory(factory);
        table.getColumns().addAll(first);
        first.setOnEditStart(new EventHandler() {
            @Override public void handle(Event t) {
                rt_29650_start_count++;
            }
        });
        first.setOnEditCommit(new EventHandler() {
            @Override public void handle(Event t) {
                rt_29650_commit_count++;
            }
        });
        first.setOnEditCancel(new EventHandler() {
            @Override public void handle(Event t) {
                rt_29650_cancel_count++;
            }
        });

        new StageLoader(table);

        table.edit(0, first);

        Toolkit.getToolkit().firePulse();

        TableCell rootCell = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 0);
        TextField textField = (TextField) rootCell.getGraphic();
        textField.setText("Testing!");
        KeyEventFirer keyboard = new KeyEventFirer(textField);
        keyboard.doKeyPress(KeyCode.ENTER);

        // TODO should the following assert be enabled?
//        assertEquals("Testing!", listView.getItems().get(0));
        assertEquals(1, rt_29650_start_count);
        assertEquals(1, rt_29650_commit_count);
        assertEquals(0, rt_29650_cancel_count);
    }

    private int rt_29849_start_count = 0;
    @Test public void test_rt_29849() {
        TableView<Person> table = new TableView<>();
        table.setEditable(true);
        table.setItems(FXCollections.observableArrayList(
            new Person("John", "Smith", "jacob.smith@example.com")
        ));

        TableColumn<Person,String> first = new TableColumn<Person,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        Callback<TableColumn<Person, String>, TableCell<Person, String>> factory = TextFieldTableCell.forTableColumn();
        first.setCellFactory(factory);
        table.getColumns().addAll(first);
        first.setOnEditStart(new EventHandler() {
            @Override public void handle(Event t) {
                rt_29849_start_count++;
            }
        });

        // load the table so the default cells are created
        new StageLoader(table);

        // now replace the cell factory
        first.setCellFactory(TextFieldTableCell.<Person>forTableColumn());

        Toolkit.getToolkit().firePulse();

        // now start an edit and count the start edit events - it should be just 1
        table.edit(0, first);
        assertEquals(1, rt_29849_start_count);
    }

    @Test public void test_rt_32708_removeFromColumnsList() {
        TableView<Person> table = new TableView<>();
        table.setEditable(true);
        table.setItems(FXCollections.observableArrayList(
                new Person("John", "Smith", "jacob.smith@example.com")
        ));

        TableColumn<Person,String> first = new TableColumn<Person,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        TableColumn<Person,String> last = new TableColumn<Person,String>("last");
        first.setCellValueFactory(new PropertyValueFactory("lastName"));
        TableColumn<Person,String> email = new TableColumn<Person,String>("email");
        first.setCellValueFactory(new PropertyValueFactory("email"));
        table.getColumns().addAll(first, last, email);

        // load the table so the default cells are created
        new StageLoader(table);

        // test pre-conditions - last column should be visible
        VirtualFlowTestUtils.assertTableHeaderColumnExists(table, last, true);

        // remove last column from tableview
        table.getColumns().remove(last);
        Toolkit.getToolkit().firePulse();

        // test post conditions - last column should not be visible
        VirtualFlowTestUtils.assertTableHeaderColumnExists(table, last, false);
    }

    @Test public void test_rt_32708_toggleVisible() {
        TableView<Person> table = new TableView<>();
        table.setEditable(true);
        table.setItems(FXCollections.observableArrayList(
                new Person("John", "Smith", "jacob.smith@example.com")
        ));

        TableColumn<Person,String> first = new TableColumn<Person,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        TableColumn<Person,String> last = new TableColumn<Person,String>("last");
        first.setCellValueFactory(new PropertyValueFactory("lastName"));
        TableColumn<Person,String> email = new TableColumn<Person,String>("email");
        first.setCellValueFactory(new PropertyValueFactory("email"));
        table.getColumns().addAll(first, last, email);

        // load the table so the default cells are created
        new StageLoader(table);

        // test pre-conditions - last column should be visible
        VirtualFlowTestUtils.assertTableHeaderColumnExists(table, last, true);

        // hide the last column from tableview
        last.setVisible(false);
        Toolkit.getToolkit().firePulse();

        // test post conditions - last column should not be visible
        VirtualFlowTestUtils.assertTableHeaderColumnExists(table, last, false);
    }

    @Test public void test_rt_34493() {
        TableView<Person> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(
                new Person("John", "Smith", "jacob.smith@example.com")
        ));

        TableColumn<Person,String> first = new TableColumn<Person,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        TableColumn<Person,String> last = new TableColumn<Person,String>("last");
        first.setCellValueFactory(new PropertyValueFactory("lastName"));
        TableColumn<Person,String> email = new TableColumn<Person,String>("email");
        first.setCellValueFactory(new PropertyValueFactory("email"));
        table.getColumns().addAll(first, last, email);

        // load the table
        new StageLoader(table);

        // resize the last column
        last.impl_setWidth(400);
        assertEquals(400, last.getWidth(), 0.0);

        // hide the first column
        table.getColumns().remove(first);
        Toolkit.getToolkit().firePulse();

        // the last column should still be 400px, not the default width or any
        // other value (based on the width of the content in that column)
        assertEquals(400, last.getWidth(), 0.0);
    }

    @Test public void test_rt_34685_directEditCall_cellSelectionMode() {
        test_rt_34685_commitCount = 0;
        test_rt_34685(false, true);
    }

    @Test public void test_rt_34685_directEditCall_rowSelectionMode() {
        test_rt_34685_commitCount = 0;
        test_rt_34685(false, false);
    }

    @Test public void test_rt_34685_mouseDoubleClick_cellSelectionMode() {
        test_rt_34685_commitCount = 0;
        test_rt_34685(true, true);
    }

    @Test public void test_rt_34685_mouseDoubleClick_rowSelectionMode() {
        test_rt_34685_commitCount = 0;
        test_rt_34685(true, false);
    }

    private int test_rt_34685_commitCount = 0;
    private void test_rt_34685(boolean useMouseToInitiateEdit, boolean cellSelectionModeEnabled) {
        assertEquals(0, test_rt_34685_commitCount);

        TableView<Person> table = new TableView<>();
        table.getSelectionModel().setCellSelectionEnabled(cellSelectionModeEnabled);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setEditable(true);

        Person person1;
        table.setItems(FXCollections.observableArrayList(
            person1 = new Person("John", "Smith", "john.smith@example.com"),
            new Person("Jacob", "Michaels", "jacob.michaels@example.com"),
            new Person("Jim", "Bob", "jim.bob@example.com")
        ));

        TableColumn<Person,String> first = new TableColumn<Person,String>("first");
        first.setCellValueFactory(new PropertyValueFactory("firstName"));
        Callback<TableColumn<Person, String>, TableCell<Person, String>> factory = TextFieldTableCell.forTableColumn();
        first.setCellFactory(factory);       // note that only the first name col is editable

        EventHandler<TableColumn.CellEditEvent<Person, String>> onEditCommit = first.getOnEditCommit();
        first.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<Person, String>>() {
            @Override public void handle(TableColumn.CellEditEvent<Person, String> event) {
                test_rt_34685_commitCount++;
                onEditCommit.handle(event);
            }
        });

        table.getColumns().addAll(first);

        // get the cell at (0,0)
        VirtualFlowTestUtils.BLOCK_STAGE_LOADER_DISPOSE = true;
        TableCell cell = (TableCell) VirtualFlowTestUtils.getCell(table, 0, 0);
        VirtualFlowTestUtils.BLOCK_STAGE_LOADER_DISPOSE = false;
        assertTrue(cell.getSkin() instanceof TableCellSkin);
        assertNull(cell.getGraphic());
        assertEquals("John", cell.getText());
        assertEquals("John", person1.getFirstName());

        // set the table to be editing the first cell at 0,0
        if (useMouseToInitiateEdit) {
            MouseEventFirer mouse = new MouseEventFirer(cell);
            mouse.fireMousePressAndRelease(2, 10, 10);  // click 10 pixels in and 10 pixels down
        } else {
            table.edit(0,first);
        }

        Toolkit.getToolkit().firePulse();
        assertNotNull(cell.getGraphic());
        assertTrue(cell.getGraphic() instanceof TextField);

        TextField textField = (TextField) cell.getGraphic();
        assertEquals("John", textField.getText());

        textField.setText("Andrew");
        textField.requestFocus();
        Toolkit.getToolkit().firePulse();

        KeyEventFirer keyboard = new KeyEventFirer(textField);
        keyboard.doKeyPress(KeyCode.ENTER);

        VirtualFlowTestUtils.getVirtualFlow(table).requestLayout();
        Toolkit.getToolkit().firePulse();

        VirtualFlowTestUtils.assertTableCellTextEquals(table, 0, 0, "Andrew");
        assertEquals("Andrew", cell.getText());
        assertEquals("Andrew", person1.getFirstName());
        assertEquals(1, test_rt_34685_commitCount);
    }

    @Test public void test_rt_35224() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        table.getColumns().setAll(col1, col2);

        new StageLoader(table);

        Toolkit.getToolkit().firePulse();
        col1.getColumns().setAll(new TableColumn(), new TableColumn());
        Toolkit.getToolkit().firePulse();
        col2.getColumns().setAll(new TableColumn(), new TableColumn());
        Toolkit.getToolkit().firePulse();
    }

    @Test public void test_rt_35141_simple_switch_two_columns_move_col1_forward_1_place() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        table.getColumns().setAll(col1, col2);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(1, getColumnIndex(col2));

        moveColumn(col1, 1);
        assertEquals(1, getColumnIndex(col1));
        assertEquals(0, getColumnIndex(col2));
    }

    @Test public void test_rt_35141_simple_switch_two_columns_move_col2_backward_1_place() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        table.getColumns().setAll(col1, col2);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(1, getColumnIndex(col2));

        moveColumn(col2, 0);
        assertEquals(1, getColumnIndex(col1));
        assertEquals(0, getColumnIndex(col2));
    }

    @Test public void test_rt_35141_simple_switch_three_columns_move_col1_forward_1_place() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(1, getColumnIndex(col2));
        assertEquals(2, getColumnIndex(col3));

        moveColumn(col1, 1);
        assertEquals(1, getColumnIndex(col1));
        assertEquals(0, getColumnIndex(col2));
        assertEquals(2, getColumnIndex(col3));
    }

    @Test public void test_rt_35141_simple_switch_three_columns_move_col2_backward_1_place() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(1, getColumnIndex(col2));
        assertEquals(2, getColumnIndex(col3));

        moveColumn(col2, 0);
        assertEquals(1, getColumnIndex(col1));
        assertEquals(0, getColumnIndex(col2));
        assertEquals(2, getColumnIndex(col3));
    }

    @Test public void test_rt_35141_simple_switch_three_columns_move_col2_forward_1_place() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(1, getColumnIndex(col2));
        assertEquals(2, getColumnIndex(col3));

        moveColumn(col2, 2);
        assertEquals(0, getColumnIndex(col1));
        assertEquals(2, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));
    }

    @Test public void test_rt_35141_simple_switch_three_columns_move_col3_backward_1_place() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(1, getColumnIndex(col2));
        assertEquals(2, getColumnIndex(col3));

        moveColumn(col3, 1);
        assertEquals(0, getColumnIndex(col1));
        assertEquals(2, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));
    }

    @Test public void test_rt_35141_simple_switch_three_columns_move_col0_forward_2_places() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(1, getColumnIndex(col2));
        assertEquals(2, getColumnIndex(col3));

        moveColumn(col1, 2);
        assertEquals(2, getColumnIndex(col1));
        assertEquals(0, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));
    }

    @Test public void test_rt_35141_simple_switch_three_columns_move_col3_backward_2_places() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(1, getColumnIndex(col2));
        assertEquals(2, getColumnIndex(col3));

        moveColumn(col3, 0);
        assertEquals(1, getColumnIndex(col1));
        assertEquals(2, getColumnIndex(col2));
        assertEquals(0, getColumnIndex(col3));
    }

    @Test public void test_rt_35141_hidden_column_move_col1_forward_1_place() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3);

        col2.setVisible(false);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));

        moveColumn(col1, 1);
        assertEquals(1, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(0, getColumnIndex(col3));
    }

    @Test public void test_rt_35141_hidden_column_move_col1_forward_100_places() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3);

        col2.setVisible(false);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));

        moveColumn(col1, 100);
        assertEquals(1, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(0, getColumnIndex(col3));
    }

    @Test public void test_rt_35141_hidden_column_move_col3_backward_1_place() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3);

        col2.setVisible(false);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));

        moveColumn(col3, 0);
        assertEquals(1, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(0, getColumnIndex(col3));
    }

    @Test public void test_rt_35141_multiple_hidden_columns_move_col1_to_middle() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        TableColumn col4 = new TableColumn();
        TableColumn col5 = new TableColumn();
        TableColumn col6 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3, col4, col5, col6);

        col2.setVisible(false);
        col4.setVisible(false);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(2, getColumnIndex(col5));
        assertEquals(3, getColumnIndex(col6));

        moveColumn(col1, 1);    // 1 should represent the spot between col2 and col4
        assertEquals(1, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(0, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(2, getColumnIndex(col5));
        assertEquals(3, getColumnIndex(col6));
    }

    @Test public void test_rt_35141_multiple_hidden_columns_move_col1_to_end() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        TableColumn col4 = new TableColumn();
        TableColumn col5 = new TableColumn();
        TableColumn col6 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3, col4, col5, col6);

        col2.setVisible(false);
        col4.setVisible(false);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(2, getColumnIndex(col5));
        assertEquals(3, getColumnIndex(col6));

        moveColumn(col1, 3);    // 3 should represent the end place
        assertEquals(3, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(0, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(1, getColumnIndex(col5));
        assertEquals(2, getColumnIndex(col6));
    }

    @Test public void test_rt_35141_multiple_hidden_columns_move_col3_to_start() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        TableColumn col4 = new TableColumn();
        TableColumn col5 = new TableColumn();
        TableColumn col6 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3, col4, col5, col6);

        col2.setVisible(false);
        col4.setVisible(false);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(2, getColumnIndex(col5));
        assertEquals(3, getColumnIndex(col6));

        moveColumn(col3, 0);
        assertEquals(1, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(0, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(2, getColumnIndex(col5));
        assertEquals(3, getColumnIndex(col6));
    }

    @Test public void test_rt_35141_multiple_hidden_columns_move_col3_to_end() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        TableColumn col4 = new TableColumn();
        TableColumn col5 = new TableColumn();
        TableColumn col6 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3, col4, col5, col6);

        col2.setVisible(false);
        col4.setVisible(false);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(2, getColumnIndex(col5));
        assertEquals(3, getColumnIndex(col6));

        moveColumn(col3, 3);    // 3 should represent the end place
        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(3, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(1, getColumnIndex(col5));
        assertEquals(2, getColumnIndex(col6));
    }

    @Test public void test_rt_35141_multiple_hidden_columns_move_col6_to_start() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        TableColumn col4 = new TableColumn();
        TableColumn col5 = new TableColumn();
        TableColumn col6 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3, col4, col5, col6);

        col2.setVisible(false);
        col4.setVisible(false);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(2, getColumnIndex(col5));
        assertEquals(3, getColumnIndex(col6));

        moveColumn(col6, 0);
        assertEquals(1, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(2, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(3, getColumnIndex(col5));
        assertEquals(0, getColumnIndex(col6));
    }

    @Test public void test_rt_35141_multiple_hidden_columns_move_col6_to_middle() {
        TableView table = new TableView();
        TableColumn col1 = new TableColumn();
        TableColumn col2 = new TableColumn();
        TableColumn col3 = new TableColumn();
        TableColumn col4 = new TableColumn();
        TableColumn col5 = new TableColumn();
        TableColumn col6 = new TableColumn();
        table.getColumns().setAll(col1, col2, col3, col4, col5, col6);

        col2.setVisible(false);
        col4.setVisible(false);

        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(1, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(2, getColumnIndex(col5));
        assertEquals(3, getColumnIndex(col6));

        moveColumn(col6, 1);
        assertEquals(0, getColumnIndex(col1));
        assertEquals(-1, getColumnIndex(col2));
        assertEquals(2, getColumnIndex(col3));
        assertEquals(-1, getColumnIndex(col4));
        assertEquals(3, getColumnIndex(col5));
        assertEquals(1, getColumnIndex(col6));
    }
}

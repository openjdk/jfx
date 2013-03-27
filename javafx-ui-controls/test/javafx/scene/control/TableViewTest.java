/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.control.ControlTestUtils.assertStyleClassContains;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;
import static javafx.scene.control.TableColumn.SortType.DESCENDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.scene.control.TableColumnComparatorBase.TableColumnComparator;
import com.sun.javafx.scene.control.test.ControlAsserts;
import com.sun.javafx.scene.control.test.Person;
import com.sun.javafx.scene.control.test.RT_22463_Person;

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
        
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        table.getSortOrder().add(col);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        
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
        
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        table.getSortOrder().add(col);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        
        ControlAsserts.assertListContainsItemsInOrder(table.getSortOrder(), col);
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
        
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        
        // when we change from ASCENDING to DESCENDING we don't expect the sort
        // to actually change (and in fact we expect the sort type to resort
        // back to being ASCENDING)
        col.setSortType(DESCENDING);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        assertEquals(ASCENDING, col.getSortType());
        
        ControlAsserts.assertListContainsItemsInOrder(table.getSortOrder(), col);
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
        
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        
        col.setSortType(DESCENDING);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        assertEquals(DESCENDING, col.getSortType());
        
        ControlAsserts.assertListContainsItemsInOrder(table.getSortOrder(), col);
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
        
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        
        col.setSortType(null);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        assertEquals(DESCENDING, col.getSortType());
        
        ControlAsserts.assertListContainsItemsInOrder(table.getSortOrder(), col);
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
        
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        
        col.setSortType(null);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        assertNull(col.getSortType());
        
        ControlAsserts.assertListContainsItemsInOrder(table.getSortOrder(), col);
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
        
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        
        col.setSortType(ASCENDING);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        assertNull(col.getSortType());
        
        ControlAsserts.assertListContainsItemsInOrder(table.getSortOrder(), col);
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
        
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        
        col.setSortType(ASCENDING);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        assertEquals(ASCENDING, col.getSortType());
        
        ControlAsserts.assertListContainsItemsInOrder(table.getSortOrder(), col);
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
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        table.getSortOrder().add(col);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        table.setSortPolicy(SORT_SUCCESS_ASCENDING_SORT_POLICY);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
    }
    
    @Test public void testChangingSortPolicyDoesNotUpdateItemsListWhenTheSortOrderListIsEmpty() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        
        table.setSortPolicy(SORT_SUCCESS_ASCENDING_SORT_POLICY);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortOrderAddition() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);
        
        table.getSortOrder().add(col);
        
        // no sort should be run (as we have a custom sort policy), and the 
        // sortOrder list should be empty as the sortPolicy failed
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        assertTrue(table.getSortOrder().isEmpty());
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortOrderRemoval() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        table.getSortOrder().add(col);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        // even though we remove the column from the sort order here, because the
        // sort policy fails the items list should remain unchanged and the sort
        // order list should continue to have the column in it.
        table.getSortOrder().remove(col);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        ControlAsserts.assertListContainsItemsInOrder(table.getSortOrder(), col);
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortTypeChange_ascendingToDescending() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(ASCENDING);
        table.getSortOrder().add(col);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        col.setSortType(DESCENDING);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Banana", "Orange");
        assertEquals(ASCENDING, col.getSortType());
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortTypeChange_descendingToNull() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(DESCENDING);
        table.getSortOrder().add(col);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        col.setSortType(null);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        assertEquals(DESCENDING, col.getSortType());
    }
    
    @Test public void testFailedSortPolicyBacksOutLastChange_sortTypeChange_nullToAscending() {
        TableColumn<String, String> col = initSortTestStructure();
        col.setSortType(null);
        table.getSortOrder().add(col);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);

        col.setSortType(ASCENDING);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
        assertNull(col.getSortType());
    }
    
    @Test public void testComparatorChangesInSyncWithSortOrder_1() {
        TableColumn<String, String> col = initSortTestStructure();
        assertNull(table.getComparator());
        assertTrue(table.getSortOrder().isEmpty());
        
        table.getSortOrder().add(col);
        TableColumnComparator c = (TableColumnComparator)table.getComparator();
        assertNotNull(c);
        ControlAsserts.assertListContainsItemsInOrder(c.getColumns(), col);
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
        ControlAsserts.assertListContainsItemsInOrder(c.getColumns(), col);
        
        // now remove column from sort order, and the comparator should go to
        // being null
        table.getSortOrder().remove(col);
        assertNull(table.getComparator());
    }
    
    @Test public void testFailedSortPolicyBacksOutComparatorChange_sortOrderAddition() {
        TableColumn<String, String> col = initSortTestStructure();
        final TableColumnComparator oldComparator = (TableColumnComparator)table.getComparator();
        
        col.setSortType(DESCENDING);
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Apple", "Orange", "Banana");
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
        ControlAsserts.assertListContainsItemsInOrder(table.getItems(), "Orange", "Banana", "Apple");
        oldComparator = (TableColumnComparator)table.getComparator();
        ControlAsserts.assertListContainsItemsInOrder(oldComparator.getColumns(), col);
        
        table.setSortPolicy(NO_SORT_FAILED_SORT_POLICY);
        table.getSortOrder().remove(col);
        
        assertTrue(table.getSortOrder().contains(col));
        ControlAsserts.assertListContainsItemsInOrder(oldComparator.getColumns(), col);
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
        
        ControlAsserts.assertRowsNotEmpty(table, 0, 5); // rows 0 - 5 should be filled
        ControlAsserts.assertRowsEmpty(table, 5, -1); // rows 5+ should be empty
        
        // now we replace the data and expect the cells that have no data
        // to be empty
        table.setItems(FXCollections.observableArrayList(
            new Person("*_*Emma", "Jones", "emma.jones@example.com"),
            new Person("_Michael", "Brown", "michael.brown@example.com")));
        
        ControlAsserts.assertRowsNotEmpty(table, 0, 2); // rows 0 - 2 should be filled
        ControlAsserts.assertRowsEmpty(table, 2, -1); // rows 2+ should be empty
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
        ControlAsserts.assertCellTextEquals(table, 0, "1", "name1");
        ControlAsserts.assertCellTextEquals(table, 1, "2", "name2");
        
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
        ControlAsserts.assertCellTextEquals(table, 0, "1", "updated name1");
        ControlAsserts.assertCellTextEquals(table, 1, "2", "updated name2");
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
}

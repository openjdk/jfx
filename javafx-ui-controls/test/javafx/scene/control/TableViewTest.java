/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import static javafx.scene.control.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.*;

import java.util.Arrays;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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
        assertEquals(false, table.getSortOrder().contains(first));
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
}

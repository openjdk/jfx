/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Before;
import org.junit.Test;

public class ListViewTest {
    private ListView<String> listView;
    private MultipleSelectionModel<String> sm;
    
    @Before public void setup() {
        listView = new ListView<String>();
        sm = listView.getSelectionModel();
    }
    
    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void noArgConstructorSetsTheStyleClass() {
        assertStyleClassContains(listView, "list-view");
    }
    
    @Test public void noArgConstructorSetsNonNullSelectionModel() {
        assertNotNull(sm);
    }
    
    @Test public void noArgConstructorSetsNonNullItems() {
        assertNotNull(listView.getItems());
    }
    
    @Test public void noArgConstructor_selectedItemIsNull() {
        assertNull(sm.getSelectedItem());
    }
    
    @Test public void noArgConstructor_selectedIndexIsNegativeOne() {
        assertEquals(-1, sm.getSelectedIndex());
    }
    
    @Test public void singleArgConstructorSetsTheStyleClass() {
        final ListView<String> b2 = new ListView<String>(FXCollections.observableArrayList("Hi"));
        assertStyleClassContains(b2, "list-view");
    }
    
    @Test public void singleArgConstructorSetsNonNullSelectionModel() {
        final ListView<String> b2 = new ListView<String>(FXCollections.observableArrayList("Hi"));
        assertNotNull(b2.getSelectionModel());
    }

    @Test public void singleArgConstructorAllowsNullItems() {
        final ListView<String> b2 = new ListView<String>(null);
        assertNull(b2.getItems());
    }
    
    @Test public void singleArgConstructorTakesItems() {
        ObservableList<String> items = FXCollections.observableArrayList("Hi");
        final ListView<String> b2 = new ListView<String>(items);
        assertSame(items, b2.getItems());
    }
    
    @Test public void singleArgConstructor_selectedItemIsNull() {
        final ListView<String> b2 = new ListView<String>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getSelectionModel().getSelectedItem());
    }
    
    @Test public void singleArgConstructor_selectedIndexIsNegativeOne() {
        final ListView<String> b2 = new ListView<String>(FXCollections.observableArrayList("Hi"));
        assertEquals(-1, b2.getSelectionModel().getSelectedIndex());
    }
    
    /*********************************************************************
     * Tests for selection model                                         *
     ********************************************************************/
    
    @Test public void selectionModelCanBeNull() {
        listView.setSelectionModel(null);
        assertNull(listView.getSelectionModel());
    }

    @Test public void selectionModelCanBeBound() {
        MultipleSelectionModel<String> sm = new ListView.ListViewBitSetSelectionModel<String>(listView);
        ObjectProperty<MultipleSelectionModel<String>> other = new SimpleObjectProperty<MultipleSelectionModel<String>>(sm);
        listView.selectionModelProperty().bind(other);
        assertSame(sm, sm);
    }

    @Test public void selectionModelCanBeChanged() {
        MultipleSelectionModel<String> sm = new ListView.ListViewBitSetSelectionModel<String>(listView);
        listView.setSelectionModel(sm);
        assertSame(sm, sm);
    }
    
    @Test public void canSetSelectedItemToAnItemEvenWhenThereAreNoItems() {
        final String randomString = new String("I AM A CRAZY RANDOM STRING");
        sm.select(randomString);
        assertEquals(-1, sm.getSelectedIndex());
        assertSame(randomString, sm.getSelectedItem());
    }
        
    @Test public void canSetSelectedItemToAnItemNotInTheDataModel() {
        listView.getItems().addAll("Apple", "Orange", "Banana");
        final String randomString = new String("I AM A CRAZY RANDOM STRING");
        sm.select(randomString);
        assertEquals(-1, sm.getSelectedIndex());
        assertSame(randomString, sm.getSelectedItem());
    }
        
    @Test public void settingTheSelectedItemToAnItemInItemsResultsInTheCorrectSelectedIndex() {
        listView.getItems().addAll("Apple", "Orange", "Banana");
        sm.select("Orange");
        assertEquals(1, sm.getSelectedIndex());
        assertSame("Orange", sm.getSelectedItem());
    }
    
    @Test public void settingTheSelectedItemToANonexistantItemAndThenSettingItemsWhichContainsItResultsInCorrectSelectedIndex() {
        sm.select("Orange");
        listView.getItems().addAll("Apple", "Orange", "Banana");
        assertEquals(1, sm.getSelectedIndex());
        assertSame("Orange", sm.getSelectedItem());
    }
    
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex0() {
        listView.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(0);
        listView.getItems().clear();
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals(null, sm.getSelectedItem());
    }
    
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex2() {
        listView.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(2);
        listView.getItems().clear();
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals(null, sm.getSelectedItem());
    }
    
    @Test public void ensureSelectedItemRemainsAccurateWhenItemsAreCleared() {
        listView.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(2);
        listView.getItems().clear();
        assertNull(sm.getSelectedItem());
        assertEquals(-1, sm.getSelectedIndex());
        
        listView.getItems().addAll("Kiwifruit", "Mandarin", "Pineapple");
        sm.select(2);
        assertEquals("Pineapple", sm.getSelectedItem());
    }
    
    @Test public void ensureSelectionShiftsDownWhenOneNewItemIsAdded() {
        listView.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertEquals(1, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
        
        listView.getItems().add(0, "Kiwifruit");
        assertEquals(2, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
    }
    
    @Test public void ensureSelectionShiftsDownWhenMultipleNewItemAreAdded() {
        listView.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertEquals(1, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
        
        listView.getItems().addAll(0, Arrays.asList("Kiwifruit", "Pineapple", "Mandarin"));
        assertEquals("Orange", sm.getSelectedItem());
        assertEquals(4, sm.getSelectedIndex());
    }
    
    @Test public void ensureSelectionShiftsUpWhenOneItemIsRemoved() {
        listView.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertEquals(1, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
        
        listView.getItems().remove("Apple");
        assertEquals(0, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
    }
    
    @Test public void ensureSelectionShiftsUpWheMultipleItemsAreRemoved() {
        listView.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(2);
        assertEquals(2, sm.getSelectedIndex());
        assertEquals("Banana", sm.getSelectedItem());
        
        listView.getItems().removeAll(Arrays.asList("Apple", "Orange"));
        assertEquals(0, sm.getSelectedIndex());
        assertEquals("Banana", sm.getSelectedItem());
    }
    
    @Test public void ensureSelectionIsCorrectWhenItemsChange() {
        listView.setItems(FXCollections.observableArrayList("Item 1"));
        sm.select(0);
        assertEquals("Item 1", sm.getSelectedItem());
        
        listView.setItems(FXCollections.observableArrayList("Item 2"));
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals(null, sm.getSelectedItem());
    }
    
    @Test public void test_rt15793() {
        // ListView selectedIndex is 0 although the items list is empty
        final ListView lv = new ListView();
        final ObservableList list = FXCollections.observableArrayList();
        lv.setItems(list);
        list.add("toto");
        lv.getSelectionModel().select(0);
        assertEquals(0, lv.getSelectionModel().getSelectedIndex());
        list.remove(0);
        assertEquals(-1, lv.getSelectionModel().getSelectedIndex());
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemAddedAtFocusIndex() {
        final ListView lv = new ListView();
        FocusModel fm = lv.getFocusModel();
        lv.getItems().add("row1");
        fm.focus(0);
        assertTrue(fm.isFocused(0));
        
        lv.getItems().add(0, "row0");
        assertTrue(fm.isFocused(1));
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemAddedBeforeFocusIndex() {
        final ListView lv = new ListView();
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
        final ListView lv = new ListView();
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
        final ListView lv = new ListView();
        FocusModel fm = lv.getFocusModel();
        lv.getItems().add("row1");
        fm.focus(0);
        assertTrue(fm.isFocused(0));
        
        lv.getItems().remove("row1");
        assertTrue(fm.getFocusedIndex() == -1);
        assertNull(fm.getFocusedItem());
    }
    
    @Test public void test_rt17522_focusShouldMoveWhenItemRemovedBeforeFocusIndex() {
        final ListView lv = new ListView();
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
        final ListView lv = new ListView();
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
        listView.getItems().addAll("row1", "row2", "row3");
        sm.select(1);
        listView.getItems().add("Another Row");
        assertEquals(1, sm.getSelectedIndices().size());
        assertEquals(1, sm.getSelectedItems().size());
    }
    
    @Test public void test_rt18339_onlyEditWhenListViewIsEditable_editableIsFalse() {
        listView.setEditable(false);
        listView.edit(1);
        assertEquals(-1, listView.getEditingIndex());
    }
    
    @Test public void test_rt18339_onlyEditWhenListViewIsEditable_editableIsTrue() {
        listView.setEditable(true);
        listView.edit(1);
        assertEquals(1, listView.getEditingIndex());
    }
    
    @Test public void test_rt14451() {
        listView.getItems().addAll("Apple", "Orange", "Banana");
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.selectRange(0, 2); // select from 0 (inclusive) to 2 (exclusive)
        assertEquals(2, sm.getSelectedIndices().size());
    }
    
    private int hitCount = 0;
    @Test public void test_rt18969() {
        hitCount = 0;
        ObservableList<String> emptyModel = FXCollections.observableArrayList();
        listView.setItems(emptyModel);
        assertTrue(listView.getItems().isEmpty());
        
        sm.selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue<? extends String> observable, String oldValue, final String newValue) {
                hitCount++;
            }
        });
        
        ObservableList<String> mod = FXCollections.observableArrayList();
        mod.add(System.currentTimeMillis()+"");
        listView.getItems().setAll(mod);
        
        sm.select(0);
        assertTrue(sm.isSelected(0));
        assertEquals(1, hitCount);
        
        // the list is totally changing (it is being cleared), so we should 
        // be nulling out the selection model state
        mod = FXCollections.observableArrayList();
        mod.add(System.currentTimeMillis()+"");
        listView.getItems().setAll(mod);
        
        // it should be three, as there is a null event in between
        assertEquals(3, hitCount);
    }
}

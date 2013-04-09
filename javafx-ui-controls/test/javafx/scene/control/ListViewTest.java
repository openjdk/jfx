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

import static javafx.scene.control.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import com.sun.javafx.scene.control.skin.VirtualScrollBar;
import com.sun.javafx.scene.control.test.ControlAsserts;
import com.sun.javafx.scene.control.test.Person;
import com.sun.javafx.scene.control.test.RT_22463_Person;
import com.sun.javafx.tk.Toolkit;

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
    
    private int rt_18969_hitCount = 0;
    @Test public void test_rt18969() {
        rt_18969_hitCount = 0;
        ObservableList<String> emptyModel = FXCollections.observableArrayList();
        listView.setItems(emptyModel);
        assertTrue(listView.getItems().isEmpty());
        
        sm.selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue<? extends String> observable, String oldValue, final String newValue) {
                rt_18969_hitCount++;
            }
        });
        
        ObservableList<String> mod = FXCollections.observableArrayList();
        mod.add(System.currentTimeMillis()+"");
        listView.getItems().setAll(mod);
        
        sm.select(0);
        assertTrue(sm.isSelected(0));
        assertEquals(1, rt_18969_hitCount);
        
        // sleep for 100ms so that the currentTimeMillis is guaranteed to be
        // a different value than the first one
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        
        // the list is totally changing (it is being cleared), so we should 
        // be nulling out the selection model state
        mod = FXCollections.observableArrayList();
        mod.add(System.currentTimeMillis()+"");
        listView.getItems().setAll(mod);
        
        // it should be two, as there is no null event in between (although there
        // used to be, so the test used to be for three hits)
        assertEquals(2, rt_18969_hitCount);
    }
    
    @Test public void test_rt21586() {
        listView.getItems().setAll("Apple", "Orange", "Banana");
        listView.getSelectionModel().select(1);
        assertEquals(1, listView.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", listView.getSelectionModel().getSelectedItem());
        
        listView.getItems().setAll("Kiwifruit", "Pineapple", "Grape");
        assertEquals(-1, listView.getSelectionModel().getSelectedIndex());
        assertNull(listView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt27820_1() {
        listView.getItems().setAll("Apple", "Orange");
        listView.getSelectionModel().select(0);
        assertEquals(1, listView.getSelectionModel().getSelectedItems().size());
        assertEquals("Apple", listView.getSelectionModel().getSelectedItem());
        
        listView.getItems().clear();
        assertEquals(0, listView.getSelectionModel().getSelectedItems().size());
        assertNull(listView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt27820_2() {
        listView.getItems().setAll("Apple", "Orange");
        listView.getSelectionModel().select(1);
        assertEquals(1, listView.getSelectionModel().getSelectedItems().size());
        assertEquals("Orange", listView.getSelectionModel().getSelectedItem());
        
        listView.getItems().clear();
        assertEquals(0, listView.getSelectionModel().getSelectedItems().size());
        assertNull(listView.getSelectionModel().getSelectedItem());
    }
    
    @Test public void test_rt28534() {
        ListView<Person> list = new ListView<Person>();
        list.setItems(FXCollections.observableArrayList(
            new Person("Jacob", "Smith", "jacob.smith@example.com"),
            new Person("Isabella", "Johnson", "isabella.johnson@example.com"),
            new Person("Ethan", "Williams", "ethan.williams@example.com"),
            new Person("Emma", "Jones", "emma.jones@example.com"),
            new Person("Michael", "Brown", "michael.brown@example.com")));
        
        ControlAsserts.assertRowsNotEmpty(list, 0, 5); // rows 0 - 5 should be filled
        ControlAsserts.assertRowsEmpty(list, 5, -1); // rows 5+ should be empty
        
        // now we replace the data and expect the cells that have no data
        // to be empty
        list.setItems(FXCollections.observableArrayList(
            new Person("*_*Emma", "Jones", "emma.jones@example.com"),
            new Person("_Michael", "Brown", "michael.brown@example.com")));
        
        ControlAsserts.assertRowsNotEmpty(list, 0, 2); // rows 0 - 2 should be filled
        ControlAsserts.assertRowsEmpty(list, 2, -1); // rows 2+ should be empty
    }
    
    @Test public void test_rt22463() {
        final ListView<RT_22463_Person> list = new ListView<RT_22463_Person>();
        
        // before the change things display fine
        RT_22463_Person p1 = new RT_22463_Person();
        p1.setId(1l);
        p1.setName("name1");
        RT_22463_Person p2 = new RT_22463_Person();
        p2.setId(2l);
        p2.setName("name2");
        list.setItems(FXCollections.observableArrayList(p1, p2));
        ControlAsserts.assertCellTextEquals(list, 0, "name1");
        ControlAsserts.assertCellTextEquals(list, 1, "name2");
        
        // now we change the persons but they are still equal as the ID's don't
        // change - but the items list is cleared so the cells should update
        RT_22463_Person new_p1 = new RT_22463_Person();
        new_p1.setId(1l);
        new_p1.setName("updated name1");
        RT_22463_Person new_p2 = new RT_22463_Person();
        new_p2.setId(2l);
        new_p2.setName("updated name2");
        list.getItems().clear();
        list.setItems(FXCollections.observableArrayList(new_p1, new_p2));
        ControlAsserts.assertCellTextEquals(list, 0, "updated name1");
        ControlAsserts.assertCellTextEquals(list, 1, "updated name2");
    }
    
    @Test public void test_rt28637() {
        ObservableList<String> items = FXCollections.observableArrayList("String1", "String2", "String3", "String4");
        
        final ListView<String> listView = new ListView<String>();
        listView.setItems(items);
        
        listView.getSelectionModel().select(0);
        assertEquals("String1", listView.getSelectionModel().getSelectedItem());
        assertEquals("String1", listView.getSelectionModel().getSelectedItems().get(0));
        assertEquals(0, listView.getSelectionModel().getSelectedIndex());
        
        items.remove(listView.getSelectionModel().getSelectedItem());
        assertEquals("String2", listView.getSelectionModel().getSelectedItem());
        assertEquals("String2", listView.getSelectionModel().getSelectedItems().get(0));
        assertEquals(0, listView.getSelectionModel().getSelectedIndex());
    }
    
    @Test public void test_rt28819_1() {
        ObservableList<String> emptyModel = FXCollections.observableArrayList();
        
        final ListView<String> listView = new ListView<String>();
        listView.setItems(emptyModel);
        ControlAsserts.assertRowsEmpty(listView, 0, 5);
        
        ObservableList<String> mod = FXCollections.observableArrayList();
        String value = System.currentTimeMillis()+"";
        mod.add(value);
        listView.setItems(mod);
        ControlAsserts.assertCellCount(listView, 1);
        ControlAsserts.assertCellTextEquals(listView, 0, value);
    }
    
    @Test public void test_rt28819_2() {
        ObservableList<String> emptyModel = FXCollections.observableArrayList();
        
        final ListView<String> listView = new ListView<String>();
        listView.setItems(emptyModel);
        ControlAsserts.assertRowsEmpty(listView, 0, 5);
        
        ObservableList<String> mod1 = FXCollections.observableArrayList();
        String value1 = System.currentTimeMillis()+"";
        mod1.add(value1);
        listView.getItems().setAll(mod1);
        ControlAsserts.assertCellCount(listView, 1);
        ControlAsserts.assertCellTextEquals(listView, 0, value1);
    }
    
    @Test public void test_rt29390() {
        ObservableList<String> items = FXCollections.observableArrayList(
                "String1", "String2", "String3", "String4",
                "String1", "String2", "String3", "String4",
                "String1", "String2", "String3", "String4",
                "String1", "String2", "String3", "String4"
        );
        
        final ListView<String> listView = new ListView<String>(items);
        listView.setMaxHeight(50);
        listView.setPrefHeight(50);
        
        // we want the vertical scrollbar
        VirtualScrollBar scrollBar = ControlAsserts.getVirtualFlowVerticalScrollbar(listView);
        
        assertNotNull(scrollBar);
        assertTrue(scrollBar.isVisible());
        assertTrue(scrollBar.getVisibleAmount() > 0.0);
        assertTrue(scrollBar.getVisibleAmount() < 1.0);
        
        // this next test is likely to be brittle, but we'll see...If it is the
        // cause of failure then it can be commented out
        assertEquals(0.125, scrollBar.getVisibleAmount(), 0.0);
    }
}

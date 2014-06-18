/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.skin.VirtualScrollBar;
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

        sm.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            rt_18969_hitCount++;
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

        VirtualFlowTestUtils.assertRowsNotEmpty(list, 0, 5); // rows 0 - 5 should be filled
        VirtualFlowTestUtils.assertRowsEmpty(list, 5, -1); // rows 5+ should be empty

        // now we replace the data and expect the cells that have no data
        // to be empty
        list.setItems(FXCollections.observableArrayList(
                new Person("*_*Emma", "Jones", "emma.jones@example.com"),
                new Person("_Michael", "Brown", "michael.brown@example.com")));

        VirtualFlowTestUtils.assertRowsNotEmpty(list, 0, 2); // rows 0 - 2 should be filled
        VirtualFlowTestUtils.assertRowsEmpty(list, 2, -1); // rows 2+ should be empty
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
        VirtualFlowTestUtils.assertCellTextEquals(list, 0, "name1");
        VirtualFlowTestUtils.assertCellTextEquals(list, 1, "name2");

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
        VirtualFlowTestUtils.assertCellTextEquals(list, 0, "updated name1");
        VirtualFlowTestUtils.assertCellTextEquals(list, 1, "updated name2");
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
        VirtualFlowTestUtils.assertRowsEmpty(listView, 0, 5);

        ObservableList<String> mod = FXCollections.observableArrayList();
        String value = System.currentTimeMillis()+"";
        mod.add(value);
        listView.setItems(mod);
        VirtualFlowTestUtils.assertCellCount(listView, 1);
        VirtualFlowTestUtils.assertCellTextEquals(listView, 0, value);
    }

    @Test public void test_rt28819_2() {
        ObservableList<String> emptyModel = FXCollections.observableArrayList();

        final ListView<String> listView = new ListView<String>();
        listView.setItems(emptyModel);
        VirtualFlowTestUtils.assertRowsEmpty(listView, 0, 5);

        ObservableList<String> mod1 = FXCollections.observableArrayList();
        String value1 = System.currentTimeMillis()+"";
        mod1.add(value1);
        listView.getItems().setAll(mod1);
        VirtualFlowTestUtils.assertCellCount(listView, 1);
        VirtualFlowTestUtils.assertCellTextEquals(listView, 0, value1);
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
        VirtualScrollBar scrollBar = VirtualFlowTestUtils.getVirtualFlowVerticalScrollbar(listView);

        assertNotNull(scrollBar);
        assertTrue(scrollBar.isVisible());
        assertTrue(scrollBar.getVisibleAmount() > 0.0);
        assertTrue(scrollBar.getVisibleAmount() < 1.0);

        // this next test is likely to be brittle, but we'll see...If it is the
        // cause of failure then it can be commented out
        assertEquals(0.125, scrollBar.getVisibleAmount(), 0.0);
    }

    @Test public void test_rt30400() {
        // create a listview that'll render cells using the check box cell factory
        ObservableList<String> items = FXCollections.observableArrayList("String1");
        final ListView<String> listView = new ListView<String>(items);
        listView.setMinHeight(100);
        listView.setPrefHeight(100);
        listView.setCellFactory(CheckBoxListCell.forListView(param -> new ReadOnlyBooleanWrapper(true)));

        // because only the first row has data, all other rows should be 
        // empty (and not contain check boxes - we just check the first four here)
        VirtualFlowTestUtils.assertRowsNotEmpty(listView, 0, 1);
        VirtualFlowTestUtils.assertCellNotEmpty(VirtualFlowTestUtils.getCell(listView, 0));
        VirtualFlowTestUtils.assertCellEmpty(VirtualFlowTestUtils.getCell(listView, 1));
        VirtualFlowTestUtils.assertCellEmpty(VirtualFlowTestUtils.getCell(listView, 2));
        VirtualFlowTestUtils.assertCellEmpty(VirtualFlowTestUtils.getCell(listView, 3));
    }

    @Test public void test_rt29420() {
        final ListView<String> listView = new ListView<String>();

        VBox vbox = new VBox(listView);
        StageLoader sl = new StageLoader(vbox);

        // the initial width of a ListView should be the golden rectangle where
        // the height is hardcoded to be 400
        final double initialWidth = listView.prefWidth(-1);
        assertEquals(400 * 0.618033987, initialWidth, 0.00);

        // add in some items, and re-measure - seeing as the items are narrow,
        // the width shouldn't change
        listView.getItems().addAll("one", "two", "three", "four", "five", "six");
        Toolkit.getToolkit().firePulse();
        final double withContentWidth = listView.prefWidth(-1);
        assertEquals(initialWidth, withContentWidth, 0.00);

        // remove the items - and the width should remain the same
        listView.getItems().clear();
        Toolkit.getToolkit().firePulse();
        final double afterEmptiedWidth = listView.prefWidth(-1);
        assertEquals(initialWidth, afterEmptiedWidth, 0.00);

        sl.dispose();
    }

    @Test public void test_rt31165() {
        final ObservableList names = FXCollections.observableArrayList("Adam", "Alex", "Alfred", "Albert");
        final ObservableList data = FXCollections.observableArrayList();
        for (int i = 0; i < 18; i++) {
            data.add(""+i);
        }

        final ListView listView = new ListView(data);
        listView.setPrefSize(200, 250);
        listView.setEditable(true);
        listView.setCellFactory(ComboBoxListCell.forListView(names));

        IndexedCell cell = VirtualFlowTestUtils.getCell(listView, 1);
        assertEquals("1", cell.getText());
        assertFalse(cell.isEditing());

        listView.edit(1);

        assertEquals(1, listView.getEditingIndex());
        assertTrue(cell.isEditing());

        VirtualFlowTestUtils.getVirtualFlow(listView).requestLayout();
        Toolkit.getToolkit().firePulse();

        assertEquals(1, listView.getEditingIndex());
        assertTrue(cell.isEditing());
    }

    @Test public void test_rt31471() {
        final ObservableList names = FXCollections.observableArrayList("Adam", "Alex", "Alfred", "Albert");
        final ListView listView = new ListView(names);

        IndexedCell cell = VirtualFlowTestUtils.getCell(listView, 0);
        assertEquals("Adam", cell.getItem());

        listView.setFixedCellSize(50);

        VirtualFlowTestUtils.getVirtualFlow(listView).requestLayout();
        Toolkit.getToolkit().firePulse();

        assertEquals("Adam", cell.getItem());
        assertEquals(50, cell.getHeight(), 0.00);
    }

    private int rt_31200_count = 0;
    @Test public void test_rt_31200() {
        final ListView listView = new ListView();
        listView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
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
        listView.getItems().setAll("one", "two", "three", "four", "five");

        StageLoader sl = new StageLoader(listView);

        assertEquals(24, rt_31200_count);

        // resize the stage
        sl.getStage().setHeight(250);
        Toolkit.getToolkit().firePulse();
        sl.getStage().setHeight(50);
        Toolkit.getToolkit().firePulse();
        assertEquals(24, rt_31200_count);

        sl.dispose();
    }

    @Test public void test_rt_30484() {
        final ListView listView = new ListView();
        listView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
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
        listView.getItems().setAll("one", "two");

        VirtualFlowTestUtils.assertGraphicIsVisible(listView, 0);
        VirtualFlowTestUtils.assertGraphicIsVisible(listView, 1);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(listView, 2);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(listView, 3);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(listView, 4);
        VirtualFlowTestUtils.assertGraphicIsNotVisible(listView, 5);
    }

    private int rt_29650_start_count = 0;
    private int rt_29650_commit_count = 0;
    private int rt_29650_cancel_count = 0;
    @Test public void test_rt_29650() {
        listView.setOnEditStart(t -> {
            rt_29650_start_count++;
        });
        listView.setOnEditCommit(t -> {
            rt_29650_commit_count++;
        });
        listView.setOnEditCancel(t -> {
            rt_29650_cancel_count++;
        });

        listView.getItems().setAll("one", "two", "three", "four", "five");
        listView.setEditable(true);
        listView.setCellFactory(TextFieldListCell.forListView());

        StageLoader sl = new StageLoader(listView);

        listView.edit(0);

        Toolkit.getToolkit().firePulse();

        ListCell rootCell = (ListCell) VirtualFlowTestUtils.getCell(listView, 0);
        TextField textField = (TextField) rootCell.getGraphic();
        textField.setText("Testing!");
        KeyEventFirer keyboard = new KeyEventFirer(textField);
        keyboard.doKeyPress(KeyCode.ENTER);

        // TODO should the following assert be enabled?
//        assertEquals("Testing!", listView.getItems().get(0));
        assertEquals(1, rt_29650_start_count);
        assertEquals(1, rt_29650_commit_count);
        assertEquals(0, rt_29650_cancel_count);

        sl.dispose();
    }

    @Test public void test_rt35039() {
        final List<String> data = new ArrayList<>();
        data.add("aabbaa");
        data.add("bbc");

        final ListView<String> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(data));

        StageLoader sl = new StageLoader(listView);

        // everything should be null to start with
        assertNull(listView.getSelectionModel().getSelectedItem());

        // select "bbc" and ensure everything is set to that
        listView.getSelectionModel().select(1);
        assertEquals("bbc", listView.getSelectionModel().getSelectedItem());

        // change the items list - but retain the same content. We expect
        // that "bbc" remains selected as it is still in the list
        listView.setItems(FXCollections.observableArrayList(data));
        assertEquals("bbc", listView.getSelectionModel().getSelectedItem());

        sl.dispose();
    }

    @Test public void test_rt35857() {
        ObservableList<String> fxList = FXCollections.observableArrayList("A", "B", "C");
        final ListView<String> listView = new ListView<String>(fxList);

        listView.getSelectionModel().select(0);

        ObservableList<String> selectedItems = listView.getSelectionModel().getSelectedItems();
        assertEquals(1, selectedItems.size());
        assertEquals("A", selectedItems.get(0));

        listView.getItems().removeAll(selectedItems);
        assertEquals(2, fxList.size());
        assertEquals("B", fxList.get(0));
        assertEquals("C", fxList.get(1));
    }

    private int rt_35889_cancel_count = 0;
    @Test public void test_rt35889() {
        final ListView<String> textFieldListView = new ListView<String>();
        textFieldListView.setItems(FXCollections.observableArrayList("A", "B", "C"));
        textFieldListView.setEditable(true);
        textFieldListView.setCellFactory(TextFieldListCell.forListView());
        textFieldListView.setOnEditCancel(t -> {
            rt_35889_cancel_count++;
            System.out.println("On Edit Cancel: " + t);
        });

        ListCell cell0 = (ListCell) VirtualFlowTestUtils.getCell(textFieldListView, 0);
        assertNull(cell0.getGraphic());
        assertEquals("A", cell0.getText());

        textFieldListView.edit(0);
        TextField textField = (TextField) cell0.getGraphic();
        assertNotNull(textField);

        assertEquals(0, rt_35889_cancel_count);

        textField.setText("Z");
        textField.getOnAction().handle(new ActionEvent());

        assertEquals(0, rt_35889_cancel_count);
    }

    @Test public void test_rt25679() {
        Button focusBtn = new Button("Focus here");

        final ListView<String> listView = new ListView<String>();
        SelectionModel sm = listView.getSelectionModel();
        listView.setItems(FXCollections.observableArrayList("A", "B", "C"));

        VBox vbox = new VBox(focusBtn, listView);

        StageLoader sl = new StageLoader(vbox);
        sl.getStage().requestFocus();
        focusBtn.requestFocus();
        Toolkit.getToolkit().firePulse();

        // test initial state
        assertEquals(sl.getStage().getScene().getFocusOwner(), focusBtn);
        assertTrue(focusBtn.isFocused());
        assertEquals(-1, sm.getSelectedIndex());
        assertNull(sm.getSelectedItem());

        // move focus to the listview
        listView.requestFocus();

        // ensure that there is a selection (where previously there was not one)
        assertEquals(sl.getStage().getScene().getFocusOwner(), listView);
        assertTrue(listView.isFocused());
        assertEquals(0, sm.getSelectedIndex());
        assertEquals("A", sm.getSelectedItem());

        sl.dispose();
    }

    private int rt_37061_index_counter = 0;
    private int rt_37061_item_counter = 0;
    @Test public void test_rt_37061() {
        ListView<Integer> tv = new ListView<>();
        tv.getItems().add(1);
        tv.getSelectionModel().select(0);

        // note we add the listeners after the selection is made, so the counters
        // at this point are still both at zero.
        tv.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            rt_37061_index_counter++;
        });

        tv.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            rt_37061_item_counter++;
        });

        // add a new item. This does not impact the selected index or selected item
        // so the counters should remain at zero.
        tv.getItems().add(2);
        assertEquals(0, rt_37061_index_counter);
        assertEquals(0, rt_37061_item_counter);
    }
}

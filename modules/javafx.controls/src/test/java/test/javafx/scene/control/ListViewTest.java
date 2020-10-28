/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import com.sun.javafx.scene.control.VirtualScrollBar;
import com.sun.javafx.scene.control.behavior.FocusTraversalInputMap;
import com.sun.javafx.scene.control.behavior.ListCellBehavior;
import com.sun.javafx.scene.control.behavior.ListViewBehavior;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import com.sun.javafx.tk.Toolkit;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.FocusModel;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListCellShim;
import javafx.scene.control.ListView;
import javafx.scene.control.ListViewShim;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static javafx.collections.FXCollections.*;

import test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import test.com.sun.javafx.scene.control.test.Person;
import test.com.sun.javafx.scene.control.test.RT_22463_Person;

public class ListViewTest {
    private ListView<String> listView;
    private MultipleSelectionModel<String> sm;
    private FocusModel<String> fm;

    @Before public void setup() {
        listView = new ListView<>();
        sm = listView.getSelectionModel();
        fm = listView.getFocusModel();
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
        final ListView<String> b2 = new ListView<>(FXCollections.observableArrayList("Hi"));
        assertStyleClassContains(b2, "list-view");
    }

    @Test public void singleArgConstructorSetsNonNullSelectionModel() {
        final ListView<String> b2 = new ListView<>(FXCollections.<String>observableArrayList("Hi"));
        assertNotNull(b2.getSelectionModel());
    }

    @Test public void singleArgConstructorAllowsNullItems() {
        final ListView<String> b2 = new ListView<String>(null);
        assertNull(b2.getItems());
    }

    @Test public void singleArgConstructorTakesItems() {
        ObservableList<String> items = FXCollections.observableArrayList("Hi");
        final ListView<String> b2 = new ListView<>(items);
        assertSame(items, b2.getItems());
    }

    @Test public void singleArgConstructor_selectedItemIsNull() {
        final ListView<String> b2 = new ListView<>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getSelectionModel().getSelectedItem());
    }

    @Test public void singleArgConstructor_selectedIndexIsNegativeOne() {
        final ListView<String> b2 = new ListView<>(FXCollections.observableArrayList("Hi"));
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
        MultipleSelectionModel<String> sm = ListViewShim.<String>getListViewBitSetSelectionModel(listView);
        ObjectProperty<MultipleSelectionModel<String>> other = new SimpleObjectProperty<MultipleSelectionModel<String>>(sm);
        listView.selectionModelProperty().bind(other);
        assertSame(sm, sm);
    }

    @Test public void selectionModelCanBeChanged() {
        MultipleSelectionModel<String> sm = ListViewShim.<String>getListViewBitSetSelectionModel(listView);
        listView.setSelectionModel(sm);
        assertSame(sm, sm);
    }

    @Test public void testCtrlAWhenSwitchingSelectionModel() {
        ListView<String> listView = new ListView<>();
        listView.getItems().addAll("a", "b", "c", "d");

        MultipleSelectionModel<String> sm;
        StageLoader sl = new StageLoader(listView);
        KeyEventFirer keyboard = new KeyEventFirer(listView);

        MultipleSelectionModel<String> smMultiple = ListViewShim.<String>getListViewBitSetSelectionModel(listView);
        smMultiple.setSelectionMode(SelectionMode.MULTIPLE);
        MultipleSelectionModel<String> smSingle = ListViewShim.<String>getListViewBitSetSelectionModel(listView);
        smSingle.setSelectionMode(SelectionMode.SINGLE);

        listView.setSelectionModel(smMultiple);
        sm = listView.getSelectionModel();

        assertEquals(0, sm.getSelectedItems().size());
        sm.clearAndSelect(0);
        assertEquals(1, sm.getSelectedItems().size());
        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
        assertEquals(4, sm.getSelectedItems().size());

        listView.setSelectionModel(smSingle);
        sm = listView.getSelectionModel();

        assertEquals(0, sm.getSelectedItems().size());
        sm.clearAndSelect(0);
        assertEquals(1, sm.getSelectedItems().size());
        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
        assertEquals(1, sm.getSelectedItems().size());

        sl.dispose();
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
        assertNull(sm.getSelectedItem());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals("Item 2", fm.getFocusedItem());
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
                return new ListCellShim<String>() {
                    ImageView view = new ImageView();
                    { setGraphic(view); };

                    @Override
                    public void updateItem(String item, boolean empty) {
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
                return new ListCellShim<String>() {
                    Rectangle graphic = new Rectangle(10, 10, Color.RED);
                    { setGraphic(graphic); };

                    @Override public void updateItem(String item, boolean empty) {
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

        // selection starts off on row -1
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

//--------- regression testing of JDK-8093144 (was: RT-35857)

    @Test
    public void test_rt35857_selectLast_retainAllSelected() {
        final ListView<String> listView = new ListView<String>(observableArrayList("A", "B", "C"));
        listView.getSelectionModel().select(listView.getItems().size() - 1);

        assert_rt35857(listView.getItems(), listView.getSelectionModel(), true);
    }

    @Test
    public void test_rt35857_selectLast_removeAllSelected() {
        final ListView<String> listView = new ListView<String>(observableArrayList("A", "B", "C"));
        listView.getSelectionModel().select(listView.getItems().size() - 1);

        assert_rt35857(listView.getItems(), listView.getSelectionModel(), false);
    }

    @Test
    public void test_rt35857_selectFirst_retainAllSelected() {
        final ListView<String> listView = new ListView<String>(observableArrayList("A", "B", "C"));
        listView.getSelectionModel().select(0);

        assert_rt35857(listView.getItems(), listView.getSelectionModel(), true);
    }

    /**
     * Modifies the items by retain/removeAll (depending on the given flag) selectedItems
     * of the selectionModels and asserts the state of the items.
     */
    protected <T> void assert_rt35857(ObservableList<T> items, MultipleSelectionModel<T> sm, boolean retain) {
        T selectedItem = sm.getSelectedItem();
        assertNotNull("sanity: ", selectedItem);
        ObservableList<T> expected;
        if (retain) {
            expected = FXCollections.observableArrayList(selectedItem);
            items.retainAll(sm.getSelectedItems());
        } else {
            expected = FXCollections.observableArrayList(items);
            expected.remove(selectedItem);
            items.removeAll(sm.getSelectedItems());
        }
        String modified = (retain ? " retainAll " : " removeAll ") + " selectedItems ";
        assertEquals("expected list after" + modified, expected, items);
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

//-------- end regression testing of JDK-8093144

    private int rt_35889_cancel_count = 0;
    @Test public void test_rt35889() {
        final ListView<String> textFieldListView = new ListView<String>();
        textFieldListView.setItems(FXCollections.observableArrayList("A", "B", "C"));
        textFieldListView.setEditable(true);
        textFieldListView.setCellFactory(TextFieldListCell.forListView());
        textFieldListView.setOnEditCancel(t -> {
            rt_35889_cancel_count++;
            //System.out.println("On Edit Cancel: " + t);
        });

        ListCell cell0 = (ListCell) VirtualFlowTestUtils.getCell(textFieldListView, 0);
        assertNull(cell0.getGraphic());
        assertEquals("A", cell0.getText());

        textFieldListView.edit(0);
        TextField textField = (TextField) cell0.getGraphic();
        assertNotNull(textField);

        assertEquals(0, rt_35889_cancel_count);

        textField.setText("Z");
        KeyEventFirer keyboard = new KeyEventFirer(textField);
        keyboard.doKeyPress(KeyCode.ENTER);

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
        assertEquals(-1, sm.getSelectedIndex());
        assertNull(sm.getSelectedItem());

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

    private int rt_37538_count = 0;
    @Test public void test_rt_37538_noCNextCall() {
        test_rt_37538(false, false);
    }

    @Test public void test_rt_37538_callCNextOnce() {
        test_rt_37538(true, false);
    }

    @Test public void test_rt_37538_callCNextInLoop() {
        test_rt_37538(false, true);
    }

    private void test_rt_37538(boolean callCNextOnce, boolean callCNextInLoop) {
        ListView<Integer> list = new ListView<>();
        for ( int i = 1; i <= 50; i++ ) {
            list.getItems().add(i);
        }

        list.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change<? extends Integer> c) -> {
            if (callCNextOnce) {
                c.next();
            } else if (callCNextInLoop) {
                while (c.next()) {
                    // no-op
                }
            }

            if (rt_37538_count >= 1) {
                Thread.dumpStack();
                fail("This method should only be called once");
            }

            rt_37538_count++;
        });

        StageLoader sl = new StageLoader(list);
        assertEquals(0, rt_37538_count);
        list.getSelectionModel().select(0);
        assertEquals(1, rt_37538_count);
        sl.dispose();
    }

    @Test
    public void test_rt_35395_fixedCellSize() {
        test_rt_35395(true);
    }

    @Test
    public void test_rt_35395_notFixedCellSize() {
        test_rt_35395(false);
    }

    private int rt_35395_counter;

    private void test_rt_35395(boolean useFixedCellSize) {
        rt_35395_counter = 0;

        ObservableList<String> items = FXCollections.observableArrayList();
        for (int i = 0; i < 20; ++i) {
            items.addAll("red", "green", "blue", "purple");
        }

        ListView<String> listView = new ListView<>(items);
        if (useFixedCellSize) {
            listView.setFixedCellSize(24);
        }
        listView.setCellFactory(lv -> new ListCellShim<String>() {
            @Override
            public void updateItem(String color, boolean empty) {
                rt_35395_counter += 1;
                super.updateItem(color, empty);
                setText(null);
                if (empty) {
                    setGraphic(null);
                } else {
                    Rectangle rect = new Rectangle(16, 16);
                    rect.setStyle("-fx-fill: " + color);
                    setGraphic(rect);
                }
            }
        });

        StageLoader sl = new StageLoader(listView);

        Platform.runLater(() -> {
            rt_35395_counter = 0;
            items.set(10, "yellow");
            Platform.runLater(() -> {
                Toolkit.getToolkit().firePulse();
                assertEquals(1, rt_35395_counter);
                rt_35395_counter = 0;
                items.set(30, "yellow");
                Platform.runLater(() -> {
                    Toolkit.getToolkit().firePulse();
                    assertEquals(0, rt_35395_counter);
                    rt_35395_counter = 0;
                    items.remove(12);
                    Platform.runLater(() -> {
                        Toolkit.getToolkit().firePulse();
                        assertEquals(useFixedCellSize ? 39 : 45, rt_35395_counter);
                        rt_35395_counter = 0;
                        items.add(12, "yellow");
                        Platform.runLater(() -> {
                            Toolkit.getToolkit().firePulse();
                            assertEquals(useFixedCellSize ? 39 : 45, rt_35395_counter);
                            rt_35395_counter = 0;
                            listView.scrollTo(5);
                            Platform.runLater(() -> {
                                Toolkit.getToolkit().firePulse();
                                assertEquals(5, rt_35395_counter);
                                rt_35395_counter = 0;
                                listView.scrollTo(55);
                                Platform.runLater(() -> {
                                    Toolkit.getToolkit().firePulse();
                                    assertEquals(useFixedCellSize ? 17 : 53, rt_35395_counter);
                                    sl.dispose();
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    @Test public void test_rt_37632() {
        final ObservableList<String> listOne = FXCollections.observableArrayList("A", "B", "C");
        final ObservableList<String> listTwo = FXCollections.observableArrayList("C");

        final ListView<String> listView = new ListView<>();
        MultipleSelectionModel<String> sm = listView.getSelectionModel();
        listView.setItems(listOne);
        listView.getSelectionModel().selectFirst();

        assertEquals(0, sm.getSelectedIndex());
        assertEquals("A", sm.getSelectedItem());
        assertEquals(1, sm.getSelectedIndices().size());
        assertEquals(0, (int) sm.getSelectedIndices().get(0));
        assertEquals(1, sm.getSelectedItems().size());
        assertEquals("A", sm.getSelectedItems().get(0));

        listView.setItems(listTwo);

        assertEquals(-1, sm.getSelectedIndex());
        assertNull(sm.getSelectedItem());
        assertEquals(0, sm.getSelectedIndices().size());
        assertEquals(0, sm.getSelectedItems().size());
    }

    private int rt_37853_cancelCount;
    private int rt_37853_commitCount;
    @Test public void test_rt_37853() {
        listView.setCellFactory(TextFieldListCell.forListView());
        listView.setEditable(true);

        for (int i = 0; i < 10; i++) {
            listView.getItems().add("" + i);
        }

        StageLoader sl = new StageLoader(listView);

        listView.setOnEditCancel(editEvent -> rt_37853_cancelCount++);
        listView.setOnEditCommit(editEvent -> rt_37853_commitCount++);

        assertEquals(0, rt_37853_cancelCount);
        assertEquals(0, rt_37853_commitCount);

        listView.edit(1);
        assertNotNull(listView.getEditingIndex());

        listView.getItems().clear();
        assertEquals(1, rt_37853_cancelCount);
        assertEquals(0, rt_37853_commitCount);

        sl.dispose();
    }

    @Test public void test_rt_38787_remove_b() {
        // selection moves to "a"
        test_rt_38787("a", 0, "b");
    }

    @Test public void test_rt_38787_remove_b_c() {
        // selection moves to "a"
        test_rt_38787("a", 0, "b", "c");
    }

    @Test public void test_rt_38787_remove_c_d() {
        // selection moves to "b"
        test_rt_38787("b", 1, "c", "d");
    }

    @Test public void test_rt_38787_remove_a() {
        // selection moves to "b", now in index 0
        test_rt_38787("b", 0, "a");
    }

    @Test public void test_rt_38787_remove_z() {
        // selection shouldn't move as 'z' doesn't exist
        test_rt_38787("b", 1, "z");
    }

    private void test_rt_38787(String expectedItem, int expectedIndex, String... itemsToRemove) {
        ListView<String> stringListView = new ListView<>();
        stringListView.getItems().addAll("a","b","c","d");

        MultipleSelectionModel<String> sm = stringListView.getSelectionModel();
        sm.select("b");

        // test pre-conditions
        assertEquals(1, sm.getSelectedIndex());
        assertEquals(1, (int)sm.getSelectedIndices().get(0));
        assertEquals("b", sm.getSelectedItem());
        assertEquals("b", sm.getSelectedItems().get(0));
        assertFalse(sm.isSelected(0));
        assertTrue(sm.isSelected(1));
        assertFalse(sm.isSelected(2));

        // removing items
        stringListView.getItems().removeAll(itemsToRemove);

        // testing against expectations
        assertEquals(expectedIndex, sm.getSelectedIndex());
        assertEquals(expectedIndex, (int)sm.getSelectedIndices().get(0));
        assertEquals(expectedItem, sm.getSelectedItem());
        assertEquals(expectedItem, sm.getSelectedItems().get(0));
    }

    private int rt_38341_indices_count = 0;
    private int rt_38341_items_count = 0;
    @Test public void test_rt_38341() {
        ListView<String> stringListView = new ListView<>();
        stringListView.getItems().addAll("a","b","c","d");

        MultipleSelectionModel<String> sm = stringListView.getSelectionModel();
        sm.getSelectedIndices().addListener((ListChangeListener<Integer>) c -> rt_38341_indices_count++);
        sm.getSelectedItems().addListener((ListChangeListener<String>) c -> rt_38341_items_count++);

        assertEquals(0, rt_38341_indices_count);
        assertEquals(0, rt_38341_items_count);

        // expand the first child of root, and select it (note: root isn't visible)
        sm.select(1);
        assertEquals(1, sm.getSelectedIndex());
        assertEquals(1, sm.getSelectedIndices().size());
        assertEquals(1, (int)sm.getSelectedIndices().get(0));
        assertEquals(1, sm.getSelectedItems().size());
        assertEquals("b", sm.getSelectedItem());
        assertEquals("b", sm.getSelectedItems().get(0));

        assertEquals(1, rt_38341_indices_count);
        assertEquals(1, rt_38341_items_count);

        // now delete it
        stringListView.getItems().remove(1);

        // selection should move to the childs parent in index 0
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(1, sm.getSelectedIndices().size());
        assertEquals(0, (int)sm.getSelectedIndices().get(0));
        assertEquals(1, sm.getSelectedItems().size());
        assertEquals("a", sm.getSelectedItem());
        assertEquals("a", sm.getSelectedItems().get(0));

        // we also expect there to be an event in the selection model for
        // selected indices and selected items
        assertEquals(sm.getSelectedIndices() +"", 2, rt_38341_indices_count);
        assertEquals(2, rt_38341_items_count);
    }

    @Test public void test_rt_39132() {
        ObservableList items = FXCollections.observableArrayList("one", "two", "three");
        ListView listView = new ListView<>();
        listView.setItems(items);

        MultipleSelectionModel sm = listView.getSelectionModel();
        sm.select(0);

        assertEquals(0, sm.getSelectedIndex());
        assertEquals("one", sm.getSelectedItem());

        items.add(0, "new item");
        assertEquals(1, sm.getSelectedIndex());
        assertEquals("one", sm.getSelectedItem());
    }

    private int rt_38943_index_count = 0;
    private int rt_38943_item_count = 0;
    @Test public void test_rt_38943() {
        ListView<String> listView = new ListView<>(FXCollections.observableArrayList("one", "two", "three"));

        MultipleSelectionModel sm = listView.getSelectionModel();

        sm.selectedIndexProperty().addListener((observable, oldValue, newValue) -> rt_38943_index_count++);
        sm.selectedItemProperty().addListener((observable, oldValue, newValue) -> rt_38943_item_count++);

        assertEquals(-1, sm.getSelectedIndex());
        assertNull(sm.getSelectedItem());
        assertEquals(0, rt_38943_index_count);
        assertEquals(0, rt_38943_item_count);

        sm.select(0);
        assertEquals(0, sm.getSelectedIndex());
        assertEquals("one", sm.getSelectedItem());
        assertEquals(1, rt_38943_index_count);
        assertEquals(1, rt_38943_item_count);

        sm.clearSelection(0);
        assertEquals(-1, sm.getSelectedIndex());
        assertNull(sm.getSelectedItem());
        assertEquals(2, rt_38943_index_count);
        assertEquals(2, rt_38943_item_count);
    }

    @Test public void test_rt_38884() {
        ListView<String> listView = new ListView<>();
        ObservableList<String> items = listView.getItems();

        listView.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change<? extends String> c) -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    assertTrue(c.getRemovedSize() > 0);

                    List<? extends String> removed = c.getRemoved();
                    String removedItem = null;
                    try {
                        removedItem = removed.get(0);
                    } catch (Exception e) {
                        fail();
                    }

                    assertEquals("foo", removedItem);
                }
            }
        });

        items.add("foo");
        listView.getSelectionModel().select(0);
        items.clear();
    }

    private int rt_37360_add_count = 0;
    private int rt_37360_remove_count = 0;
    @Test public void test_rt_37360() {
        ListView<String> stringListView = new ListView<>();
        stringListView.getItems().addAll("a", "b");

        MultipleSelectionModel<String> sm = stringListView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.getSelectedItems().addListener((ListChangeListener<String>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    rt_37360_add_count += c.getAddedSize();
                }
                if (c.wasRemoved()) {
                    rt_37360_remove_count += c.getRemovedSize();
                }
            }
        });

        assertEquals(0, sm.getSelectedItems().size());
        assertEquals(0, rt_37360_add_count);
        assertEquals(0, rt_37360_remove_count);

        sm.select(0);
        assertEquals(1, sm.getSelectedItems().size());
        assertEquals(1, rt_37360_add_count);
        assertEquals(0, rt_37360_remove_count);

        sm.select(1);
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals(2, rt_37360_add_count);
        assertEquals(0, rt_37360_remove_count);

        sm.clearAndSelect(1);
        assertEquals(1, sm.getSelectedItems().size());
        assertEquals(2, rt_37360_add_count);
        assertEquals(1, rt_37360_remove_count);
    }

    @Test public void test_rt_38491() {
        ListView<String> stringListView = new ListView<>();
        stringListView.getItems().addAll("a", "b");

        MultipleSelectionModel<String> sm = stringListView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        FocusModel<String> fm = stringListView.getFocusModel();

        // click on row 0
        VirtualFlowTestUtils.clickOnRow(stringListView, 0);
        assertTrue(sm.isSelected(0));
        assertEquals("a", sm.getSelectedItem());
        assertTrue(fm.isFocused(0));
        assertEquals("a", fm.getFocusedItem());
        assertEquals(0, fm.getFocusedIndex());

        int anchor = ListCellBehavior.getAnchor(stringListView, -1);
        assertTrue(ListCellBehavior.hasNonDefaultAnchor(stringListView));
        assertEquals(0, anchor);

        // now add a new item at row 0. This has the effect of pushing down
        // the selected item into row 1.
        stringListView.getItems().add(0, "z");

        // The first bug was that selection and focus were not moving down to
        // be on row 1, so we test that now
        assertFalse(sm.isSelected(0));
        assertFalse(fm.isFocused(0));
        assertTrue(sm.isSelected(1));
        assertEquals("a", sm.getSelectedItem());
        assertTrue(fm.isFocused(1));
        assertEquals("a", fm.getFocusedItem());
        assertEquals(1, fm.getFocusedIndex());

        // The second bug was that the anchor was not being pushed down as well
        // (when it should).
        anchor = ListCellBehavior.getAnchor(stringListView, -1);
        assertTrue(ListCellBehavior.hasNonDefaultAnchor(stringListView));
        assertEquals(1, anchor);
    }

    private final ObservableList<String> rt_39256_list = FXCollections.observableArrayList();
    @Test public void test_rt_39256() {
        ListView<String> stringListView = new ListView<>();
        stringListView.getItems().addAll("a","b", "c", "d");

        MultipleSelectionModel<String> sm = stringListView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);

//        rt_39256_list.addListener((ListChangeListener<String>) change -> {
//            while (change.next()) {
//                System.err.println("number of selected persons (in bound list): " + change.getList().size());
//            }
//        });

        Bindings.bindContent(rt_39256_list, sm.getSelectedItems());

        assertEquals(0, sm.getSelectedItems().size());
        assertEquals(0, rt_39256_list.size());

        sm.selectAll();
        assertEquals(4, sm.getSelectedItems().size());
        assertEquals(4, rt_39256_list.size());

        sm.selectAll();
        assertEquals(4, sm.getSelectedItems().size());
        assertEquals(4, rt_39256_list.size());

        sm.selectAll();
        assertEquals(4, sm.getSelectedItems().size());
        assertEquals(4, rt_39256_list.size());
    }

    private final ObservableList<String> rt_39482_list = FXCollections.observableArrayList();
    @Test public void test_rt_39482() {
        ListView<String> stringListView = new ListView<>();
        stringListView.getItems().addAll("a", "b", "c", "d");

        MultipleSelectionModel<String> sm = stringListView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        // Enable below prints for debug if needed
        /*sm.getSelectedItems().addListener((ListChangeListener<String>) change -> {
            while (change.next()) {
                System.out.println("sm.getSelectedItems(): " + change.getList());
            }
        });

        rt_39482_list.addListener((ListChangeListener<String>) change -> {
            while (change.next()) {
                System.out.println("rt_39482_list: " + change.getList());
            }
        });*/

        Bindings.bindContent(rt_39482_list, sm.getSelectedItems());

        assertEquals(0, sm.getSelectedItems().size());
        assertEquals(0, rt_39482_list.size());

        test_rt_39482_selectRow("a", sm, 0);
        test_rt_39482_selectRow("b", sm, 1);
        test_rt_39482_selectRow("c", sm, 2);
        test_rt_39482_selectRow("d", sm, 3);
    }

    private void test_rt_39482_selectRow(String expectedString,
                                         MultipleSelectionModel<String> sm,
                                         int rowToSelect) {
        sm.selectAll();
        assertEquals(4, sm.getSelectedIndices().size());
        assertEquals(4, sm.getSelectedItems().size());
        assertEquals(4, rt_39482_list.size());

        sm.clearAndSelect(rowToSelect);
        assertEquals(1, sm.getSelectedIndices().size());
        assertEquals(1, sm.getSelectedItems().size());
        assertEquals(expectedString, sm.getSelectedItem());
        assertEquals(expectedString, rt_39482_list.get(0));
        assertEquals(1, rt_39482_list.size());
    }

    @Test public void test_rt_39559_useSM_selectAll() {
        test_rt_39559(true);
    }

    @Test public void test_rt_39559_useKeyboard_selectAll() {
        test_rt_39559(false);
    }

    private void test_rt_39559(boolean useSMSelectAll) {
        ListView<String> stringListView = new ListView<>();
        stringListView.getItems().addAll("a", "b", "c", "d");

        MultipleSelectionModel<String> sm = stringListView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        StageLoader sl = new StageLoader(stringListView);
        KeyEventFirer keyboard = new KeyEventFirer(stringListView);

        assertEquals(0, sm.getSelectedItems().size());

        sm.clearAndSelect(0);

        if (useSMSelectAll) {
            sm.selectAll();
        } else {
            keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
        }

        assertEquals(4, sm.getSelectedItems().size());
        assertEquals(0, (int) ListCellBehavior.getAnchor(stringListView, -1));

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT);

        assertEquals(0, (int) ListCellBehavior.getAnchor(stringListView, -1));
        assertEquals(2, sm.getSelectedItems().size());
        assertEquals("a", sm.getSelectedItems().get(0));
        assertEquals("b", sm.getSelectedItems().get(1));

        sl.dispose();
    }

    @Test public void testCtrlAWhenSwitchingSelectionMode() {
        ListView<String> listView = new ListView<>();
        listView.getItems().addAll("a", "b", "c", "d");

        MultipleSelectionModel<String> sm = listView.getSelectionModel();
        StageLoader sl = new StageLoader(listView);
        KeyEventFirer keyboard = new KeyEventFirer(listView);

        assertEquals(0, sm.getSelectedItems().size());
        sm.clearAndSelect(0);
        assertEquals(1, sm.getSelectedItems().size());
        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
        assertEquals(1, sm.getSelectedItems().size());

        sm.setSelectionMode(SelectionMode.MULTIPLE);
        assertEquals(1, sm.getSelectedItems().size());
        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
        assertEquals(4, sm.getSelectedItems().size());

        sm.setSelectionMode(SelectionMode.SINGLE);
        assertEquals(1, sm.getSelectedItems().size());
        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
        assertEquals(1, sm.getSelectedItems().size());

        sl.dispose();
    }

    @Test public void test_rt_16068_firstElement_selectAndRemoveSameRow() {
        // select and then remove the 'a' item, selection and focus should both
        // stay at the first row, now 'b'
        test_rt_16068(0, 0, 0);
    }

    @Test public void test_rt_16068_firstElement_selectRowAndRemoveLaterSibling() {
        // select row 'a', and remove row 'c', selection and focus should not change
        test_rt_16068(0, 2, 0);
    }

    @Test public void test_rt_16068_middleElement_selectAndRemoveSameRow() {
        // select and then remove the 'b' item, selection and focus should both
        // move up one row to the 'a' item
        test_rt_16068(1, 1, 0);
    }

    @Test public void test_rt_16068_middleElement_selectRowAndRemoveLaterSibling() {
        // select row 'b', and remove row 'c', selection and focus should not change
        test_rt_16068(1, 2, 1);
    }

    @Test public void test_rt_16068_middleElement_selectRowAndRemoveEarlierSibling() {
        // select row 'b', and remove row 'a', selection and focus should move up
        // one row, remaining on 'b'
        test_rt_16068(1, 0, 0);
    }

    @Test public void test_rt_16068_lastElement_selectAndRemoveSameRow() {
        // select and then remove the 'd' item, selection and focus should both
        // move up one row to the 'c' item
        test_rt_16068(3, 3, 2);
    }

    @Test public void test_rt_16068_lastElement_selectRowAndRemoveEarlierSibling() {
        // select row 'd', and remove row 'a', selection and focus should move up
        // one row, remaining on 'd'
        test_rt_16068(3, 0, 2);
    }

    private void test_rt_16068(int indexToSelect, int indexToRemove, int expectedIndex) {
        ListView<String> stringListView = new ListView<>();
        stringListView.getItems().addAll("a", "b", "c", "d");

        MultipleSelectionModel<?> sm = stringListView.getSelectionModel();
        FocusModel<?> fm = stringListView.getFocusModel();

        sm.select(indexToSelect);
        assertEquals(indexToSelect, sm.getSelectedIndex());
        assertEquals(stringListView.getItems().get(indexToSelect), sm.getSelectedItem());
        assertEquals(indexToSelect, fm.getFocusedIndex());
        assertEquals(stringListView.getItems().get(indexToSelect), fm.getFocusedItem());

        stringListView.getItems().remove(indexToRemove);
        assertEquals(expectedIndex, sm.getSelectedIndex());
        assertEquals(stringListView.getItems().get(expectedIndex), sm.getSelectedItem());
        assertEquals(expectedIndex, fm.getFocusedIndex());
        assertEquals(stringListView.getItems().get(expectedIndex), fm.getFocusedItem());
    }

    @Test public void test_rt_22599() {
        ObservableList<RT22599_DataType> initialData = FXCollections.observableArrayList(
                new RT22599_DataType(1, "row1"),
                new RT22599_DataType(2, "row2"),
                new RT22599_DataType(3, "row3")
        );

        ListView<RT22599_DataType> listView = new ListView<>();
        listView.setItems(initialData);

        StageLoader sl = new StageLoader(listView);

        // testing initial state
        assertNotNull(listView.getSkin());
        assertEquals("row1", VirtualFlowTestUtils.getCell(listView, 0).getText());
        assertEquals("row2", VirtualFlowTestUtils.getCell(listView, 1).getText());
        assertEquals("row3", VirtualFlowTestUtils.getCell(listView, 2).getText());

        // change row 0 (where "row1" currently resides), keeping same id.
        // Because 'set' is called, the control should update to the new content
        // without any user interaction
        RT22599_DataType data;
        initialData.set(0, data = new RT22599_DataType(0, "row1a"));
        Toolkit.getToolkit().firePulse();
        assertEquals("row1a", VirtualFlowTestUtils.getCell(listView, 0).getText());

        // change the row 0 (where we currently have "row1a") value directly.
        // Because there is no associated property, this won't be observed, so
        // the control should still show "row1a" rather than "row1b"
        data.text = "row1b";
        Toolkit.getToolkit().firePulse();
        assertEquals("row1a", VirtualFlowTestUtils.getCell(listView, 0).getText());

        // call refresh() to force a refresh of all visible cells
        listView.refresh();
        Toolkit.getToolkit().firePulse();
        assertEquals("row1b", VirtualFlowTestUtils.getCell(listView, 0).getText());

        sl.dispose();
    }

    private static class RT22599_DataType {
        public int id = 0;
        public String text = "";

        public RT22599_DataType(int id, String text) {
            this.id = id;
            this.text = text;
        }

        @Override public String toString() {
            return text;
        }

        @Override public boolean equals(Object obj) {
            if (obj == null) return false;
            return id == ((RT22599_DataType)obj).id;
        }
    }

    private int rt_39966_count = 0;
    @Test public void test_rt_39966() {
        ObservableList<String> list = FXCollections.observableArrayList("Hello World");
        ListView<String> listView = new ListView<>(list);

        StageLoader sl = new StageLoader(listView);

        // initially there is no selection
        assertTrue(listView.getSelectionModel().isEmpty());

        listView.getSelectionModel().selectedItemProperty().addListener((value, s1, s2) -> {
            if (rt_39966_count == 0) {
                rt_39966_count++;
                assertFalse(listView.getSelectionModel().isEmpty());
            } else {
                assertTrue(listView.getSelectionModel().isEmpty());
            }
        });

        // our assertion two lines down always succeeds. What fails is our
        // assertion above within the listener.
        listView.getSelectionModel().select(0);
        assertFalse(listView.getSelectionModel().isEmpty());

        list.remove(0);
        assertTrue(listView.getSelectionModel().isEmpty());

        sl.dispose();
    }

    /**
     * Bullet 1: selected index must be updated
     * Corner case: last selected. Fails for core
     */
    @Test public void test_rt_40012_selectedAtLastOnDisjointRemoveItemsAbove() {
        ObservableList<String> items = FXCollections.observableArrayList("0", "1", "2", "3", "4", "5");
        ListView<String> listView = new ListView<>(items);
        SelectionModel sm = listView.getSelectionModel();

        int last = items.size() - 1;

        // selecting item "5"
        sm.select(last);

        // disjoint remove of 2 elements above the last selected
        // Removing "1" and "3"
        items.removeAll(items.get(1), items.get(3));

        // selection should move up two places such that it remains on item "5",
        // but in index (last - 2).
        int expected = last - 2;
        assertEquals("5", sm.getSelectedItem());
        assertEquals("selected index after disjoint removes above", expected, sm.getSelectedIndex());
    }

    /**
     * Variant of 1: if selectedIndex is not updated,
     * the old index is no longer valid
     * for accessing the items.
     */
    @Test public void test_rt_40012_accessSelectedAtLastOnDisjointRemoveItemsAbove() {
        ObservableList<String> items = FXCollections.observableArrayList("0", "1", "2", "3", "4", "5");
        ListView<String> listView = new ListView<>(items);
        SelectionModel sm = listView.getSelectionModel();

        int last = items.size() - 1;

        // selecting item "5"
        sm.select(last);

        // disjoint remove of 2 elements above the last selected
        items.removeAll(items.get(1), items.get(3));
        int selected = sm.getSelectedIndex();
        if (selected > -1) {
            items.get(selected);
        }
    }

    /**
     * Bullet 2: selectedIndex notification count
     *
     * Note that we don't use the corner case of having the last index selected
     * (which fails already on updating the index)
     */
    private int rt_40012_count = 0;
    @Test public void test_rt_40012_selectedIndexNotificationOnDisjointRemovesAbove() {
        ObservableList<String> items = FXCollections.observableArrayList("0", "1", "2", "3", "4", "5");
        ListView<String> listView = new ListView<>(items);
        SelectionModel sm = listView.getSelectionModel();

        int last = items.size() - 2;
        sm.select(last);
        assertEquals(last, sm.getSelectedIndex());

        rt_40012_count = 0;
        sm.selectedIndexProperty().addListener(o -> rt_40012_count++);

        // disjoint remove of 2 elements above the last selected
        items.removeAll(items.get(1), items.get(3));
        assertEquals("sanity: selectedIndex must be shifted by -2", last - 2, sm.getSelectedIndex());
        assertEquals("must fire single event on removes above", 1, rt_40012_count);
    }

    /**
     * Bullet 3: unchanged selectedItem must not fire change
     */
    @Test
    public void test_rt_40012_selectedItemNotificationOnDisjointRemovesAbove() {
        ObservableList<String> items = FXCollections.observableArrayList("0", "1", "2", "3", "4", "5");
        ListView<String> listView = new ListView<>(items);
        SelectionModel sm = listView.getSelectionModel();

        int last = items.size() - 2;
        Object lastItem = items.get(last);
        sm.select(last);
        assertEquals(lastItem, sm.getSelectedItem());

        rt_40012_count = 0;
        sm.selectedItemProperty().addListener(o -> rt_40012_count++);

        // disjoint remove of 2 elements above the last selected
        items.removeAll(items.get(1), items.get(3));
        assertEquals("sanity: selectedItem unchanged", lastItem, sm.getSelectedItem());
        assertEquals("must not fire on unchanged selected item", 0, rt_40012_count);
    }

    @Test public void test_rt_40185() {
        final ListView<String> lv = new ListView<>();
        final ArrayList<Integer> expected = new ArrayList<>();
        Collections.addAll(expected, 1, 2);

        lv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lv.getSelectionModel().getSelectedIndices().addListener((ListChangeListener<Integer>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    assertEquals(expected, change.getRemoved());
                }
            }
        });

        lv.getItems().addAll("-0-","-1-","-2-");
        lv.getSelectionModel().selectIndices(1, 2);
        lv.getSelectionModel().clearSelection();
    }

    /**
     * ClearAndSelect fires invalid change event if selectedIndex is unchanged.
     */
    private int rt_40212_count = 0;
    @Test public void test_rt_40212() {
        final ListView<Integer> lv = new ListView<>();
        for (int i = 0; i < 10; i++) {
            lv.getItems().add(i);
        }

        MultipleSelectionModel<Integer> sm = lv.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.selectRange(3, 5);
        int selected = sm.getSelectedIndex();

        sm.getSelectedIndices().addListener((ListChangeListener<Integer>) change -> {
            assertEquals("sanity: selectedIndex unchanged", selected, sm.getSelectedIndex());
            while(change.next()) {
                assertEquals("single event on clearAndSelect already selected", 1, ++rt_40212_count);

                boolean type = change.wasAdded() || change.wasRemoved() || change.wasPermutated() || change.wasUpdated();
                assertTrue("at least one of the change types must be true", type);
            }
        });

        sm.clearAndSelect(selected);
    }

    @Test public void test_rt_40280() {
        final ListView<String> view = new ListView<>();
        StageLoader sl = new StageLoader(view);
        view.getFocusModel().getFocusedIndex();
        sl.dispose();
    }

    /**
     * Test list change of selectedIndices on setIndices. Fails for core ..
     */
    @Test public void test_rt_40263() {
        final ListView<Integer> lv = new ListView<>();
        for (int i = 0; i < 10; i++) {
            lv.getItems().add(i);
        }

        MultipleSelectionModel<Integer> sm = lv.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        int[] indices = new int[]{2, 5, 7};
        ListChangeListener<Integer> l = c -> {
            // firstly, we expect only one change
            int subChanges = 0;
            while(c.next()) {
                subChanges++;
            }
            assertEquals(1, subChanges);

            // secondly, we expect the added size to be three, as that is the
            // number of items selected
            c.reset();
            c.next();
            //System.out.println("Added items: " + c.getAddedSubList());
            assertEquals(indices.length, c.getAddedSize());
            assertArrayEquals(indices, c.getAddedSubList().stream().mapToInt(i -> i).toArray());
        };
        sm.getSelectedIndices().addListener(l);
        sm.selectIndices(indices[0], indices);
    }

    @Test public void test_jdk8141124() {
        ListView<String> listView = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        SortedList<String> sortedItems = new SortedList<>(items);
        sortedItems.setComparator(String::compareTo);
        listView.setItems(sortedItems);

        MultipleSelectionModel<String> sm = listView.getSelectionModel();

        items.add("2");
        listView.getSelectionModel().selectFirst();
        assertEquals("2", sm.getSelectedItem());
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(0, (int) sm.getSelectedIndices().get(0));
        assertEquals("2", sm.getSelectedItems().get(0));

        items.addAll("1", "3");
        assertEquals("2", sm.getSelectedItem());
        assertEquals(1, sm.getSelectedIndex());
        assertEquals(1, (int) sm.getSelectedIndices().get(0));
        assertEquals("2", sm.getSelectedItems().get(0));
    }

    @Test public void test_jdk_8143594() {
        MultipleSelectionModel model = listView.getSelectionModel();
        model.setSelectionMode(SelectionMode.MULTIPLE);

        listView.getItems().addAll("Apple", "Orange", null);

        model.select(0);
        model.clearAndSelect(2);
        model.clearAndSelect(0);
        model.clearAndSelect(2);
    }

    @Test public void test_jdk_8145887_selectedIndices_ListIterator() {
        int selectIndices[] = { 4, 7, 9 };
        ListView<Integer> lv = new ListView<>();
        for (int i = 0; i < 10; ++i) {
            lv.getItems().add(i);
        }

        MultipleSelectionModel msm = lv.getSelectionModel();
        msm.setSelectionMode(SelectionMode.MULTIPLE);
        for (int i = 0 ; i < selectIndices.length; ++i) {
            msm.select(selectIndices[i]);
        }

        ListIterator iter = lv.getSelectionModel().getSelectedIndices().listIterator();

        // Step 1. Initial values
        assertEquals(0, iter.nextIndex());
        assertEquals(-1, iter.previousIndex());
        assertEquals(true, iter.hasNext());
        assertEquals(false, iter.hasPrevious());

        // Step 2. Iterate forward.
        assertEquals(4, iter.next());
        assertEquals(1, iter.nextIndex());
        assertEquals(0, iter.previousIndex());
        assertEquals(true, iter.hasNext());
        assertEquals(true, iter.hasPrevious());

        // Step 3. Iterate forward.
        // Values would be at similar state of Step 2.
        assertEquals(7, iter.next());

        // Step 4. Iterate forward to Last element.
        assertEquals(9, iter.next());
        assertEquals(3, iter.nextIndex());
        assertEquals(2, iter.previousIndex());
        assertEquals(false, iter.hasNext());
        assertEquals(true, iter.hasPrevious());

        // Step 5. Verify NoSuchElementException by next()
        try {
            iter.next();
        } catch (Exception e) {
            assert(e instanceof NoSuchElementException);
        }

        // Step 6. Iterate backward to Last element.
        assertEquals(9, iter.previous());
        assertEquals(2, iter.nextIndex());
        assertEquals(1, iter.previousIndex());
        assertEquals(true, iter.hasNext());
        assertEquals(true, iter.hasPrevious());

        // Step 7. Iterate forward to Last element.
        assertEquals(9, iter.next());
        assertEquals(3, iter.nextIndex());
        assertEquals(2, iter.previousIndex());
        assertEquals(false, iter.hasNext());
        assertEquals(true, iter.hasPrevious());

        // Step 8. Iterate forward to last element.
        // Values would be at Same state of Step 2.
        assertEquals(9, iter.previous());

        // Step 9. Iterate backward.
        assertEquals(7, iter.previous());
        assertEquals(1, iter.nextIndex());
        assertEquals(0, iter.previousIndex());
        assertEquals(true, iter.hasNext());
        assertEquals(true, iter.hasPrevious());

        // Step 10. Iterate back to first element.
        assertEquals(4, iter.previous());
        assertEquals(0, iter.nextIndex());
        assertEquals(-1, iter.previousIndex());
        assertEquals(true, iter.hasNext());
        assertEquals(false, iter.hasPrevious());

        // Step 11. Verify NoSuchElementException by previous()
        try {
            iter.previous();
        } catch (Exception e) {
            assert(e instanceof NoSuchElementException);
        }
    }

    @Test public void testListEditStartOnCellStandalone_JDK8187432() {
        ListView<String> control = new ListView<>(FXCollections
                .observableArrayList("Item1", "Item2", "Item3", "Item4"));
        control.setEditable(true);
        control.setCellFactory(TextFieldListCell.forListView());
        StageLoader sl = new StageLoader(control);
        int editIndex = 2;

        IndexedCell cell = VirtualFlowTestUtils.getCell(control, editIndex);
        ObjectProperty<ListView.EditEvent> editEvent = new SimpleObjectProperty<>();
        control.addEventHandler(ListView.editStartEvent(), e -> editEvent.set(e));

        // start edit on cell
        cell.startEdit();

        // test cell state
        assertTrue(cell.isEditing());
        assertEquals(editIndex, cell.getIndex());

        // test editEvent
        assertNotNull(editEvent.get());
        assertEquals("type is startEdit",
                     ListView.editStartEvent(), editEvent.get().getEventType());
        assertEquals("index on start event",
                     editIndex, editEvent.get().getIndex());

        sl.dispose();
    }

    @Test
    public void testEventIndicesOnSelectRange() {
        ObservableList<String> listItems = FXCollections.observableArrayList("zero", "one", "two", "three");
        final ListView<String> lv = new ListView<>();
        lv.setItems(listItems);
        MultipleSelectionModel<String> sm = lv.getSelectionModel();

        int selected = 1;
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.select(selected);
        sm.getSelectedIndices().addListener((ListChangeListener<Integer>) ch -> {
            if (ch.next()) {
                assertEquals("Two items should be selected.", 2, ch.getList().size());
                assertEquals("Selection range should be from index 1 ", 1, ch.getFrom());
                assertEquals("Selection range should be till index 2 ", 2, ch.getTo());
            } else {
                fail("Change event is expected when selection is changed.");
            }
        });
        int focus = lv.getFocusModel().getFocusedIndex();
        assertEquals("Selected item should be focused.", selected, focus);
        // Select the next element
        sm.selectRange(selected, focus + 2);
        assertEquals("Two items should be selected.", 2, sm.getSelectedIndices().size());
        assertEquals("List item at index 1 should be selected", 1, (int) sm.getSelectedIndices().get(0));
        assertEquals("List item at index 2 should be selected", 2, (int) sm.getSelectedIndices().get(1));
    }

    @Test public void testInterceptedKeyMappingsForComboBoxEditor() {
        ListView<String> listView = new ListView<>(FXCollections
                .observableArrayList("Item1", "Item2"));
        StageLoader sl = new StageLoader(listView);

        ListViewBehavior lvBehavior = (ListViewBehavior) ControlSkinFactory.getBehavior(listView.getSkin());
        InputMap<ListView<?>> lvInputMap = lvBehavior.getInputMap();
        ObservableList<?> inputMappings = lvInputMap.getMappings();
        // In ListViewBehavior KeyMappings for vertical orientation are added under 3rd child InputMap
        InputMap<ListView<?>> verticalInputMap = lvInputMap.getChildInputMaps().get(2);
        ObservableList<?> verticalInputMappings = verticalInputMap.getMappings();

        // Verify FocusTraversalInputMap
        for(InputMap.Mapping<?> mapping : FocusTraversalInputMap.getFocusTraversalMappings()) {
            assertTrue(inputMappings.contains(mapping));
        }

        // Verify default InputMap
        testInterceptor(inputMappings, new KeyBinding(KeyCode.HOME));
        testInterceptor(inputMappings, new KeyBinding(KeyCode.END));
        testInterceptor(inputMappings, new KeyBinding(KeyCode.HOME).shift());
        testInterceptor(inputMappings, new KeyBinding(KeyCode.END).shift());
        testInterceptor(inputMappings, new KeyBinding(KeyCode.HOME).shortcut());
        testInterceptor(inputMappings, new KeyBinding(KeyCode.END).shortcut());
        testInterceptor(inputMappings, new KeyBinding(KeyCode.A).shortcut());

        // Verify vertical child InputMap
        testInterceptor(verticalInputMappings, new KeyBinding(KeyCode.HOME).shortcut().shift());
        testInterceptor(verticalInputMappings, new KeyBinding(KeyCode.END).shortcut().shift());

        sl.dispose();
    }

    private void testInterceptor(ObservableList<?> mappings, KeyBinding binding) {
        int i = mappings.indexOf(new KeyMapping(binding, null));
        if (((KeyMapping)mappings.get(i)).getInterceptor() != null) {
            assertFalse(((KeyMapping)mappings.get(i)).getInterceptor().test(null));
        } else {
            // JDK-8209788 added interceptor for few KeyMappings
            fail("Interceptor must not be null");
        }
    }

    @Test
    public void testListViewLeak() {
        ObservableList<String> items = FXCollections.observableArrayList();
        WeakReference<ListView<String>> listViewRef = new WeakReference<>(new ListView<>(items));
        attemptGC(listViewRef, 10);
        assertNull("ListView is not GCed.", listViewRef.get());
    }

    @Test
    public void testItemLeak() {
        WeakReference<String> itemRef = new WeakReference<>(new String("Leak Item"));
        ObservableList<String> items = FXCollections.observableArrayList(itemRef.get());
        ListView<String> listView = new ListView<>(items);
        items.clear();
        attemptGC(itemRef, 10);
        assertNull("ListView item is not GCed.", itemRef.get());
    }

    private void attemptGC(WeakReference<? extends Object> weakRef, int n) {
        for (int i = 0; i < n; i++) {
            System.gc();
            System.runFinalization();

            if (weakRef.get() == null) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail("InterruptedException occurred during Thread.sleep()");
            }
        }
    }
}

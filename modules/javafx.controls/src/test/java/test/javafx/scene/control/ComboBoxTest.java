/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.behavior.FocusTraversalInputMap;
import com.sun.javafx.scene.control.behavior.ListViewBehavior;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.Utils;

import test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import javafx.css.PseudoClass;

import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import javafx.scene.control.skin.ComboBoxListViewSkin;

import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxShim;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListCellShim;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SelectionModelShim;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ComboBoxTest {
    private ComboBox<String> comboBox;
    private SingleSelectionModel<String> sm;
    private StageLoader sl; //Note : It is created and used by individual tests that need it

    /*********************************************************************
     *                                                                   *
     * Utility methods                                                   *
     *                                                                   *
     ********************************************************************/

    public ListView getListView() {
        return (ListView) ((ComboBoxListViewSkin)comboBox.getSkin()).getPopupContent();
    }

    public Node getDisplayNode() {
        return ((ComboBoxListViewSkin)comboBox.getSkin()).getDisplayNode();
    }



    /*********************************************************************
     *                                                                   *
     * Setup                                                             *
     *                                                                   *
     ********************************************************************/

    @Before public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });

        comboBox = new ComboBox<>();
        comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
        sm = comboBox.getSelectionModel();
    }

    @After public void cleanup() {
        if (sl != null) {
            sl.dispose();
        }
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    /*********************************************************************
     *                                                                   *
     * Tests for the constructors                                        *
     *                                                                   *
     ********************************************************************/

    @Test public void noArgConstructorSetsTheStyleClass() {
        assertStyleClassContains(comboBox, "combo-box");
    }

    @Test public void noArgConstructorSetsNonNullSelectionModel() {
        assertNotNull(sm);
    }

    @Test public void noArgConstructorSetsNonNullItems() {
        assertNotNull(comboBox.getItems());
    }

    @Test public void noArgConstructor_selectedItemIsNull() {
        assertNull(sm.getSelectedItem());
    }

    @Test public void noArgConstructor_selectedIndexIsNegativeOne() {
        assertEquals(-1, sm.getSelectedIndex());
    }

    @Test public void noArgConstructor_valueIsNull() {
        assertNull(comboBox.getValue());
    }

    @Test public void noArgConstructor_editableIsFalse() {
        assertFalse(comboBox.isEditable());
    }

    @Test public void noArgConstructor_showingIsFalse() {
        assertFalse(comboBox.isShowing());
    }

    @Test public void noArgConstructor_promptTextIsEmptyString() {
        assertNull(comboBox.getPromptText());
    }

    @Test public void noArgConstructor_placeholderIsNull() {
        assertNull(comboBox.getPlaceholder());
    }

    @Test public void noArgConstructor_armedIsFalse() {
        assertFalse(comboBox.isArmed());
    }

    @Test public void noArgConstructor_converterIsNotNull() {
        assertNotNull(comboBox.getConverter());
    }

    @Test public void noArgConstructor_cellFactoryIsNull() {
        assertNull(comboBox.getCellFactory());
    }

    @Test public void noArgConstructor_visibleRowFactoryIs10() {
        assertEquals(10, comboBox.getVisibleRowCount());
    }

    @Test public void singleArgConstructorSetsTheStyleClass() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertStyleClassContains(b2, "combo-box");
    }

    @Test public void singleArgConstructorSetsNonNullSelectionModel() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertNotNull(b2.getSelectionModel());
    }

    @Test public void singleArgConstructorAllowsNullItems() {
        final ComboBox<String> b2 = new ComboBox<>(null);
        assertNull(b2.getItems());
    }

    @Test public void singleArgConstructorTakesItems() {
        ObservableList<String> items = FXCollections.observableArrayList("Hi");
        final ComboBox<String> b2 = new ComboBox<>(items);
        assertSame(items, b2.getItems());
    }

    @Test public void singleArgConstructor_selectedItemIsNull() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getSelectionModel().getSelectedItem());
    }

    @Test public void singleArgConstructor_selectedIndexIsNegativeOne() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertEquals(-1, b2.getSelectionModel().getSelectedIndex());
    }

    @Test public void singleArgConstructor_valueIsNull() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getValue());
    }

    @Test public void singleArgConstructor_editableIsFalse() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertFalse(b2.isEditable());
    }

    @Test public void singleArgConstructor_showingIsFalse() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertFalse(b2.isShowing());
    }

    @Test public void singleArgConstructor_promptTextIsEmptyString() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getPromptText());
    }

    @Test public void singleArgConstructor_placeholderIsNull() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getPlaceholder());
    }

    @Test public void singleArgConstructor_armedIsFalse() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertEquals(false, b2.isArmed());
    }

    @Test public void singleArgConstructor_converterIsNotNull() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertNotNull(b2.getConverter());
    }

    @Test public void singleArgConstructor_cellFactoryIsNull() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getCellFactory());
    }

    @Test public void singleArgConstructor_visibleRowFactoryIs10() {
        final ComboBox<String> b2 = new ComboBox<>(FXCollections.observableArrayList("Hi"));
        assertEquals(10, b2.getVisibleRowCount());
    }

    /*********************************************************************
     * Tests for selection model                                         *
     ********************************************************************/

    @Test public void selectionModelCanBeNull() {
        comboBox.setSelectionModel(null);
        assertNull(comboBox.getSelectionModel());
    }

    @Test public void testNullSelectionModelDoesNotThrowNPEInSkinOnValueChange() {
        ObservableList<String> items = FXCollections.observableArrayList("ITEM1", "ITEM2");

        ListCell<String> buttonCell = new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
            }
        };
        comboBox.setButtonCell(buttonCell);
        comboBox.setItems(items);
        comboBox.setSelectionModel(null);

        // Should not throw an NPE.
        comboBox.setValue(items.get(1));

        assertEquals(items.get(1), comboBox.getValue());
        assertEquals(items.get(1), comboBox.getButtonCell().getText());
    }

    @Test public void testNullSelectionModelDoesNotThrowNPEInSkinOnLayout() {
        ObservableList<String> items = FXCollections.observableArrayList("ITEM1", "ITEM2");

        comboBox.setItems(items);

        comboBox.setValue(items.get(1));
        comboBox.setSelectionModel(null);

        // Should not throw an NPE.
        comboBox.layout();
    }

    @Test public void testNullSelectionModelDoesNotThrowNPEOnEditableChange() {
        ObservableList<String> items = FXCollections.observableArrayList("ITEM1", "ITEM2");

        comboBox.setEditable(true);
        comboBox.setItems(items);
        comboBox.setSelectionModel(null);

        // Should not throw an NPE.
        comboBox.setEditable(false);
    }

    @Test public void testNullSelectionModelDoesNotThrowNPEOnValueChange() {
        ObservableList<String> items = FXCollections.observableArrayList("ITEM1", "ITEM2");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems(items);
        comboBox.setSelectionModel(null);

        // Should not throw an NPE.
        comboBox.setValue(items.get(1));

        assertEquals(items.get(1), comboBox.getValue());
    }

    @Test public void testNullSelectionModelDoesNotThrowNPEOnListViewSelect() {
        ObservableList<String> items = FXCollections.observableArrayList("ITEM1", "ITEM2");

        comboBox.setItems(items);
        comboBox.setSelectionModel(null);
        ListView<String> listView = (ListView<String>) ((ComboBoxListViewSkin<String>) comboBox.getSkin())
                .getPopupContent();

        // Should not throw an NPE.
        listView.getSelectionModel().select(1);
    }

    @Test public void testNullSelectionModelDoesNotThrowNPEOnSkinCreation() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setSelectionModel(null);

        // Should not throw an NPE.
        comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
    }

    @Test public void selectionModelCanBeBound() {
        SingleSelectionModel<String> sm = ComboBoxShim.<String>get_ComboBoxSelectionModel(comboBox);
        ObjectProperty<SingleSelectionModel<String>> other = new SimpleObjectProperty<>(sm);
        comboBox.selectionModelProperty().bind(other);
        assertSame(sm, sm);
    }

    @Test public void selectionModelCanBeChanged() {
        SingleSelectionModel<String> sm = ComboBoxShim.<String>get_ComboBoxSelectionModel(comboBox);
        comboBox.setSelectionModel(sm);
        assertSame(sm, sm);
    }

    @Test public void canSetSelectedItemToAnItemEvenWhenThereAreNoItems() {
        final String randomString = new String("I AM A CRAZY RANDOM STRING");
        sm.select(randomString);
        assertEquals(-1, sm.getSelectedIndex());
        assertSame(randomString, sm.getSelectedItem());
    }

    @Test public void canSetSelectedItemToAnItemNotInTheDataModel() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        final String randomString = new String("I AM A CRAZY RANDOM STRING");
        sm.select(randomString);
        assertEquals(-1, sm.getSelectedIndex());
        assertSame(randomString, sm.getSelectedItem());
    }

    @Test public void settingTheSelectedItemToAnItemInItemsResultsInTheCorrectSelectedIndex() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select("Orange");
        assertEquals(1, sm.getSelectedIndex());
        assertSame("Orange", sm.getSelectedItem());
    }

    @Test public void settingTheSelectedItemToANonexistantItemAndThenSettingItemsWhichContainsItResultsInCorrectSelectedIndex() {
        sm.select("Orange");
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertEquals(1, sm.getSelectedIndex());
        assertSame("Orange", sm.getSelectedItem());
    }

    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex0() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(0);
        comboBox.getItems().clear();
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals(null, sm.getSelectedItem());
    }

    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex2() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(2);
        comboBox.getItems().clear();
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals(null, sm.getSelectedItem());
    }

    @Test public void ensureSelectedItemRemainsAccurateWhenItemsAreCleared() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(2);
        comboBox.getItems().clear();
        assertNull(sm.getSelectedItem());
        assertEquals(-1, sm.getSelectedIndex());

        comboBox.getItems().addAll("Kiwifruit", "Mandarin", "Pineapple");
        sm.select(2);
        assertEquals("Pineapple", sm.getSelectedItem());
    }

    @Test public void ensureSelectionShiftsDownWhenOneNewItemIsAdded() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertEquals(1, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());

        comboBox.getItems().add(0, "Kiwifruit");
        assertEquals(2, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
    }

    @Test public void ensureSelectionShiftsDownWhenMultipleNewItemAreAdded() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertEquals(1, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());

        comboBox.getItems().addAll(0, Arrays.asList("Kiwifruit", "Pineapple", "Mandarin"));
        assertEquals("Orange", sm.getSelectedItem());
        assertEquals(4, sm.getSelectedIndex());
    }

    @Test public void ensureSelectionShiftsUpWhenOneItemIsRemoved() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertEquals(1, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());

        comboBox.getItems().remove("Apple");
        assertEquals(0, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
    }

    @Test public void ensureSelectionShiftsUpWheMultipleItemsAreRemoved() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(2);
        assertEquals(2, sm.getSelectedIndex());
        assertEquals("Banana", sm.getSelectedItem());

        comboBox.getItems().removeAll(Arrays.asList("Apple", "Orange"));
        assertEquals(0, sm.getSelectedIndex());
        assertEquals("Banana", sm.getSelectedItem());
    }

    @Test public void ensureSelectionIsCorrectWhenItemsChange() {
        comboBox.setItems(FXCollections.observableArrayList("Item 1"));
        sm.select(0);
        assertEquals("Item 1", sm.getSelectedItem());

        comboBox.setItems(FXCollections.observableArrayList("Item 2"));
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals(null, sm.getSelectedItem());
    }

    @Test(expected=NullPointerException.class)
    public void selectionModelComboBoxReferenceCanNotBeNull() {
        ComboBoxShim.<String>get_ComboBoxSelectionModel(null);
    }

    @Test public void ensureGetModelItemOutOfBoundsWorks_1() {
        ComboBox cb = new ComboBox(null);
        cb.getSelectionModel().select(-1);
        assertEquals(-1, cb.getSelectionModel().getSelectedIndex());
    }

    @Test public void ensureGetModelItemOutOfBoundsWorks_2() {
        ComboBox cb = new ComboBox(null);
        cb.getSelectionModel().select(0);
        assertEquals(-1, cb.getSelectionModel().getSelectedIndex());
    }

    @Test public void test_rt15793() {
        // ComboBox selectedIndex is 0 although the items list is empty
        final ComboBox lv = new ComboBox();
        final ObservableList list = FXCollections.observableArrayList();
        lv.setItems(list);
        list.add("toto");
        lv.getSelectionModel().select(0);
        assertEquals(0, lv.getSelectionModel().getSelectedIndex());
        list.remove(0);
        assertEquals(-1, lv.getSelectionModel().getSelectedIndex());
    }

    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectIndex() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        sm.select(0);
        assertEquals("Apple", comboBox.getValue());
    }

    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectItem() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        sm.select("Apple");
        assertEquals("Apple", comboBox.getValue());
    }

    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectPrevious() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        sm.select(2);
        sm.selectPrevious();
        assertEquals("Orange", comboBox.getValue());
    }

    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectNext() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        sm.select("Apple");
        sm.selectNext();
        assertEquals("Orange", comboBox.getValue());
    }

    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectFirst() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        sm.selectFirst();
        assertEquals("Apple", comboBox.getValue());
    }

    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectLast() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        sm.selectLast();
        assertEquals("Banana", comboBox.getValue());
    }

    @Test public void ensureSelectionModelClearsValueProperty() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        sm.select(0);
        assertEquals("Apple", comboBox.getValue());

        sm.clearSelection();
        assertNull(comboBox.getValue());
    }

    @Test public void ensureSelectionModelClearsValuePropertyWhenNegativeOneSelected() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        sm.select(0);
        assertEquals("Apple", comboBox.getValue());

        sm.select(-1);
        assertNull("Expected null, actual value: " + comboBox.getValue(), comboBox.getValue());
    }

    @Test public void ensureValueIsCorrectWhenItemsIsAddedToWithExistingSelection() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);

        comboBox.getItems().add(0, "Kiwifruit");

        assertEquals(2, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
        assertEquals("Orange", comboBox.getValue());
    }

    @Test public void ensureValueIsCorrectWhenItemsAreRemovedWithExistingSelection() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);

        comboBox.getItems().remove("Apple");

        assertEquals(0, sm.getSelectedIndex());
        assertEquals("Orange", sm.getSelectedItem());
        assertEquals("Orange", comboBox.getValue());
    }

    @Test public void ensureValueIsUpdatedByCorrectSelectionModelWhenSelectionModelIsChanged() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        SingleSelectionModel sm1 = sm;
        sm1.select(1);
        assertEquals("Orange", comboBox.getValue());

        SingleSelectionModel sm2 = ComboBoxShim.<String>get_ComboBoxSelectionModel(comboBox);
        comboBox.setSelectionModel(sm2);

        sm1.select(2);  // value should not change as we are using old SM
        assertEquals("Orange", comboBox.getValue());

        sm2.select(0);  // value should change, as we are using new SM
        assertEquals("Apple", comboBox.getValue());
    }

    @Test public void ensureValueDoesNotChangeWhenBoundAndNoExceptions() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");

        StringProperty sp = new SimpleStringProperty("empty");
        comboBox.valueProperty().bind(sp);

        sm.select(1);
        assertEquals("empty", comboBox.getValue());
    }

    @Test public void ensureSelectionModelUpdatesWhenValueChanges() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(sm.getSelectedItem());
        comboBox.setValue("Orange");
        assertEquals("Orange", sm.getSelectedItem());
    }

    @Test public void ensureSelectionModelUpdatesWhenValueChangesToNull() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.setValue("Kiwifruit");
        assertEquals("Kiwifruit", sm.getSelectedItem());
        assertEquals("Kiwifruit", comboBox.getValue());
        comboBox.setValue(null);
        assertEquals(null, sm.getSelectedItem());
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals(null, comboBox.getValue());
    }

    @Test public void ensureValueEqualsSelectedItemWhenNotInItemsList() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        SelectionModelShim.setSelectedItem(sm, "pineapple");
        assertEquals("pineapple", sm.getSelectedItem());
        assertEquals("pineapple", comboBox.getValue());
    }

    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void checkPromptTextPropertyName() {
        assertTrue(comboBox.promptTextProperty().getName().equals("promptText"));
    }

    @Test public void checkPlaceholderPropertyName() {
        assertTrue(comboBox.placeholderProperty().getName().equals("placeholder"));
    }

    @Test public void checkValuePropertyName() {
        assertTrue(comboBox.valueProperty().getName().equals("value"));
    }

    @Test public void checkItemsPropertyName() {
        assertTrue(comboBox.itemsProperty().getName().equals("items"));
    }

    @Test public void checkConverterPropertyName() {
        assertTrue(comboBox.converterProperty().getName().equals("converter"));
    }

    @Test public void checkSelectionModelPropertyName() {
        assertTrue(comboBox.selectionModelProperty().getName().equals("selectionModel"));
    }

    @Test public void checkVisibleRowCountPropertyName() {
        assertTrue(comboBox.visibleRowCountProperty().getName().equals("visibleRowCount"));
    }

    @Test public void checkOnActionPropertyName() {
        assertTrue(comboBox.onActionProperty().getName().equals("onAction"));
    }

    @Test public void checkArmedPropertyName() {
        assertTrue(comboBox.armedProperty().getName().equals("armed"));
    }

    @Test public void checkShowingPropertyName() {
        assertTrue(comboBox.showingProperty().getName().equals("showing"));
    }

    @Test public void checkEditablePropertyName() {
        assertTrue(comboBox.editableProperty().getName().equals("editable"));
    }

    @Test public void checkCellFactoryPropertyName() {
        assertTrue(comboBox.cellFactoryProperty().getName().equals("cellFactory"));
    }

    @Test public void defaultActionHandlerIsNotDefined() {
        assertNull(comboBox.getOnAction());
    }

    @Test public void defaultConverterCanHandleStringValues() {
        StringConverter<String> sc = comboBox.getConverter();
        assertEquals("input", sc.fromString("input"));
        assertEquals("input", sc.toString("input"));
    }

    @Test public void defaultConverterCanHandleIncorrectType_1() {
        ComboBox cb = new ComboBox();
        StringConverter sc = cb.getConverter();
        assertEquals("42", sc.toString(Integer.valueOf(42)));
    }

    @Test(expected=ClassCastException.class)
    public void defaultConverterCanHandleIncorrectType_2() {
        ComboBox<Integer> cb = new ComboBox<>();
        StringConverter<Integer> sc = cb.getConverter();
        Integer value = sc.fromString("42");
    }

    @Test public void defaultConverterCanHandleNullValues() {
        StringConverter<String> sc = comboBox.getConverter();
        assertEquals(null, sc.fromString(null));
        assertEquals(null, sc.toString(null));
    }

    @Test public void ensure_getPseudoClassStateReturnsValidValue() {
        sl = new StageLoader(comboBox);


        Set<PseudoClass> pseudoClassStates = comboBox.getPseudoClassStates();
        assertFalse(comboBox.isEditable());
        assertTrue(pseudoClassStates.size() >= 0);

        comboBox.setEditable(true);
        assertTrue(pseudoClassStates.contains(PseudoClass.getPseudoClass("editable")));

        comboBox.setEditable(false);
        assertFalse(pseudoClassStates.contains(PseudoClass.getPseudoClass("editable")));

        comboBox.show();
        assertTrue(pseudoClassStates.contains(PseudoClass.getPseudoClass("showing")));

        comboBox.hide();
        assertFalse(pseudoClassStates.contains(PseudoClass.getPseudoClass("showing")));

        comboBox.arm();
        assertTrue(pseudoClassStates.contains(PseudoClass.getPseudoClass("armed")));
    }

    /*********************************************************************
     * Tests for properties                                              *
     ********************************************************************/

    @Test public void ensureAllowsNullConverter() {
        comboBox.setConverter(null);
        assertNull(comboBox.getConverter());
    }

    @Test public void ensureCanSetNonNullCellFactory() {
        Callback<ListView<String>, ListCell<String>> cf = p -> null;
        comboBox.setCellFactory(cf);
        assertEquals(cf, comboBox.getCellFactory());
    }

    @Test public void ensureEditorIsNonNullWhenComboBoxIsNotEditable() {
        assertNotNull(comboBox.getEditor());
    }

    @Test public void ensureEditorIsNonNullWhenComboBoxIsEditable() {
        comboBox.setEditable(true);
        assertNotNull(comboBox.getEditor());
    }

    @Test public void ensureEditorDoesNotChangeWhenEditableToggles() {
        comboBox.setEditable(true);
        assertNotNull(comboBox.getEditor());
        comboBox.setEditable(false);
        assertNotNull(comboBox.getEditor());
        comboBox.setEditable(true);
        assertNotNull(comboBox.getEditor());
    }

    @Test public void ensureCanSetValueToNonNullStringAndBackAgain() {
        comboBox.setValue("Test 123");
        assertEquals("Test 123", comboBox.getValue());
        comboBox.setValue(null);
        assertNull(comboBox.getValue());
    }

    @Test public void ensureCanToggleEditable() {
        comboBox.setEditable(true);
        assertTrue(comboBox.isEditable());
        comboBox.setEditable(false);
        assertFalse(comboBox.isEditable());
    }

    @Test public void ensureCanToggleShowing() {
        sl = new StageLoader(comboBox);

        comboBox.show();
        assertTrue(comboBox.isShowing());
        comboBox.hide();
        assertFalse(comboBox.isShowing());
    }

    @Test public void ensureCanNotToggleShowingWhenDisabled() {
        sl = new StageLoader(comboBox);

        comboBox.setDisable(true);
        comboBox.show();
        assertFalse(comboBox.isShowing());
        comboBox.setDisable(false);
        comboBox.show();
        assertTrue(comboBox.isShowing());
    }

    @Test public void ensureCanSetPromptText() {
        comboBox.setPromptText("Test 1 2 3");
        assertEquals("Test 1 2 3", comboBox.getPromptText());
    }

    @Test public void ensureCanSetPromptTextToNull() {
        comboBox.setPromptText("");
        assertEquals("", comboBox.getPromptText());
        comboBox.setPromptText(null);
        assertEquals(null, comboBox.getPromptText());
    }

    @Test public void ensurePromptTextStripsNewlines() {
        comboBox.setPromptText("Test\n1\n2\n3");
        assertEquals("Test123", comboBox.getPromptText());
    }

    @Test public void ensureCanSetPlaceholder() {
        Label label = new javafx.scene.control.Label("Test 1 2 3");
        comboBox.setPlaceholder(label);
        assertEquals(label, comboBox.getPlaceholder());
    }

    @Test public void ensureCanToggleArmed() {
        assertFalse(comboBox.isArmed());
        comboBox.arm();
        assertTrue(comboBox.isArmed());
        comboBox.disarm();
        assertFalse(comboBox.isArmed());
    }

    @Test public void ensureCanSetVisibleRowCount() {
        comboBox.setVisibleRowCount(13);
        assertEquals(13, comboBox.getVisibleRowCount());
    }

    @Test public void ensureCanSetVisibleRowCountToNegativeValues() {
        comboBox.setVisibleRowCount(-10);
        assertEquals(-10, comboBox.getVisibleRowCount());
    }

    @Test public void ensureCanSetOnAction() {
        EventHandler<ActionEvent> onAction = t -> { };
        comboBox.setOnAction(onAction);
        assertEquals(onAction, comboBox.getOnAction());
    }

    @Test public void ensureOnActionPropertyReferencesBean() {
        assertEquals(comboBox, comboBox.onActionProperty().getBean());
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/
    @Test public void checkPromptTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        comboBox.promptTextProperty().bind(strPr);
        assertTrue("PromptText cannot be bound", comboBox.getPromptText().equals("value"));
        strPr.setValue("newvalue");
        assertTrue("PromptText cannot be bound", comboBox.getPromptText().equals("newvalue"));
    }

    @Test public void checkValuePropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        comboBox.valueProperty().bind(strPr);
        assertTrue("value cannot be bound", comboBox.getValue().equals("value"));
        strPr.setValue("newvalue");
        assertTrue("value cannot be bound", comboBox.getValue().equals("newvalue"));
    }



    /*********************************************************************
     * Tests for bug reports                                             *
     ********************************************************************/

    @Test public void test_rt18972() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertTrue(sm.isSelected(1));

        comboBox.setEditable(true);
        comboBox.setValue("New Value");

        // there should be no selection in the selection model, as "New Value"
        // isn't an item in the list, however, it is a totally valid value for
        // the value property
        assertFalse(sm.isSelected(1));
        assertEquals("New Value", sm.getSelectedItem());
        assertEquals("New Value", comboBox.getValue());

        comboBox.setEditable(false);
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals("New Value", sm.getSelectedItem());
        assertEquals("New Value", comboBox.getValue());
    }

    @Test public void test_rt18941() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.setValue("Orange");
        assertEquals("Orange", comboBox.getValue());
        assertEquals("Orange", comboBox.getSelectionModel().getSelectedItem());
        assertTrue("Selected Index: " + sm.getSelectedIndex(), sm.isSelected(1));
    }

    @Test public void test_rt19227() {
        comboBox.getItems().addAll("0","0","0","0","0");
        comboBox.getSelectionModel().select(2);
        assertEquals("0", comboBox.getValue());
        assertEquals("0", comboBox.getSelectionModel().getSelectedItem());
        assertTrue(sm.isSelected(2));
    }

    @Ignore("JDK-8091127 Test not working as the heights being returned are not accurate")
    @Test public void test_rt20106() {
        comboBox.getItems().addAll("0","1","2","3","4","5","6","7","8","9");

        Stage stage = new Stage();
        Scene scene = new Scene(comboBox);
        stage.setScene(scene);
        comboBox.applyCss();
        comboBox.show();

        comboBox.setVisibleRowCount(5);
        double initialHeight = getListView().getHeight();
        assertFalse("initialHeight: " + initialHeight, Double.compare(0.0, initialHeight) == 0);

        comboBox.setVisibleRowCount(0);
        double smallHeight =    getListView().getHeight();
        assertTrue("smallHeight: " + smallHeight + ", initialHeight: " + initialHeight,
                smallHeight != initialHeight && smallHeight < initialHeight);

        comboBox.setVisibleRowCount(7);
        double biggerHeight = getListView().getHeight();
        assertTrue(biggerHeight != smallHeight && smallHeight < biggerHeight);
    }

    private int count = 0;
    @Test public void test_rt20103() {
        final TextField tf = new TextField();

        comboBox.setOnAction(t -> {
            count++;
        });

        assertTrue(count == 0);

        comboBox.valueProperty().bind(tf.textProperty());   // count++ here
        assertTrue("count: " + count, count == 1);

        tf.setText("Text1");                                // count++ here
        assertTrue("count: " + count, count == 2);

        comboBox.valueProperty().unbind();                  // no count++ here
        assertTrue("count: " + count, count == 2);

        comboBox.valueProperty().bindBidirectional(tf.textProperty());  // count++ here
        tf.setText("Text2");
        assertTrue("count: " + count, count == 3);
    }

    @Ignore("Test not working as the skin is not being properly instantiated")
    @Test public void test_rt20100() {
        comboBox.getItems().addAll("0","1","2","3","4","5","6","7","8","9");

        Stage stage = new Stage();
        Scene scene = new Scene(comboBox);
        stage.setScene(scene);
        comboBox.applyCss();
        comboBox.show();

        comboBox.setConverter(new StringConverter() {
            int toStringCounter = 0;
            int fromStringCounter = 0;

            @Override public String toString(Object t) {
                return "TO_STRING";
            }

            @Override public Object fromString(String string) {
                return "FROM_STRING";
            }
        });

        comboBox.getSelectionModel().select(2);
        assertEquals("2", comboBox.getValue());

        ListView listView = getListView();
//        listView.applyCss();

        assertEquals("2", listView.getSelectionModel().getSelectedItem());

        VirtualFlow flow = (VirtualFlow)listView.lookup("#virtual-flow");
        assertNotNull(flow);

        IndexedCell cell = flow.getVisibleCell(2);
        assertEquals("TO_STRING", cell.getText());
    }

    @Test public void test_rt20189() {
        comboBox.getItems().addAll("0","1","2","3","4","5","6","7","8","9");

        sl = new StageLoader(comboBox);

        comboBox.applyCss();
        comboBox.show();

        comboBox.getSelectionModel().select(2);
        Object item = sm.getSelectedItem();
        assertEquals("2", item);
        assertEquals(2, sm.getSelectedIndex());

        comboBox.setValue("test");
        item = sm.getSelectedItem();
        assertEquals("test",item);
        assertEquals(-1, sm.getSelectedIndex());

        comboBox.getSelectionModel().select(2);
        item = sm.getSelectedItem();
        assertEquals("2", item);
        assertEquals(2, sm.getSelectedIndex());
    }

    @Test public void test_rt27654() {
        comboBox.getItems().addAll("0","1","2","3","4","5","6","7","8","9");

        SingleSelectionModel sm = comboBox.getSelectionModel();

        sl = new StageLoader(comboBox);

        comboBox.applyCss();
        comboBox.show();
        ListCell<String> buttonCell = (ListCell<String>) getDisplayNode();

        sm.select(2);
        assertEquals("2", sm.getSelectedItem());
        assertEquals("2", comboBox.getValue());
        assertEquals("2", buttonCell.getText());
        assertEquals(2, sm.getSelectedIndex());

        sm.clearSelection();
        assertNull(sm.getSelectedItem());
        assertNull(comboBox.getValue());
        assertNull(buttonCell.getText());
        assertEquals(-1, sm.getSelectedIndex());

        sm.select(2);
        assertEquals("2", sm.getSelectedItem());
        assertEquals("2", comboBox.getValue());
        assertEquals("2", buttonCell.getText());
        assertEquals(2, sm.getSelectedIndex());
    }

    @Test public void test_rt24412() {
        SingleSelectionModel sm = comboBox.getSelectionModel();

        sl = new StageLoader(comboBox);

        comboBox.applyCss();
        comboBox.show();
        ListCell<String> buttonCell = (ListCell<String>) getDisplayNode();

        comboBox.getItems().setAll("0","1","2","3","4","5","6","7","8","9");

        sm.select("2");
        assertEquals("2", sm.getSelectedItem());
        assertEquals("2", comboBox.getValue());
        assertEquals("2", buttonCell.getText());
        assertEquals(2, sm.getSelectedIndex());

        sm.clearSelection();
        assertNull(sm.getSelectedItem());
        assertNull(comboBox.getValue());
        assertNull(buttonCell.getText());
        assertEquals(-1, sm.getSelectedIndex());

        sm.select("2");
        assertEquals("2", sm.getSelectedItem());
        assertEquals("2", comboBox.getValue());
        assertEquals("2", buttonCell.getText());
        assertEquals(2, sm.getSelectedIndex());
    }

    @Test public void test_rt28245() {
        final ObservableList<String> strings = FXCollections.observableArrayList(
            "Option 1", "Option 2", "Option 3"
        );

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems(strings);
        comboBox.setEditable(true);
        comboBox.valueProperty().addListener((ov, t, t1) -> {
            if (t == null && t1.isEmpty()) {
                fail("Old value is '" + t + "' and new value is '" + t1 + "'.");
            }
        });

        sl = new StageLoader(comboBox);

        assertNull(comboBox.getValue());
        assertTrue(comboBox.getEditor().getText().isEmpty());

        comboBox.requestFocus();

        var kFirer = new KeyEventFirer(comboBox);
        // Open selection
        kFirer.doKeyPress(KeyCode.DOWN, KeyModifier.ALT);
        // Select first
        kFirer.doKeyPress(KeyCode.DOWN);
        // Commit selection
        kFirer.doKeyPress(KeyCode.ENTER);
    }

    @Test public void test_rt31479() {
        ComboBox<String> comboBox = new ComboBox<>();

        sl = new StageLoader(comboBox);

        final double widthBefore = comboBox.getWidth();

        // add item
        comboBox.getItems().add("Option 1");

        // open and close combobox
        comboBox.show();
        comboBox.hide();

        // set a placeholder
        comboBox.setPlaceholder(new Circle(12, Color.RED));

        // remove item
        comboBox.getItems().clear();

        // fire pulse (this allows layout to cause the size to grow)
        Toolkit.getToolkit().firePulse();

        // test size
        assertEquals(widthBefore, comboBox.getWidth(), 0.00);
    }

    @Test public void test_rt32139() {
        final ObservableList<String> items =
                FXCollections.observableArrayList("Good value", "Bad value");

        final ComboBox<String> comboBox = new ComboBox<>(items);
        comboBox.getSelectionModel().select(0);

        comboBox.getSelectionModel().selectedIndexProperty().addListener((ov, oldIdx, newIdx) -> {
            if (newIdx.intValue() != 0) {
                comboBox.getSelectionModel().select(0);
            }
        });

        sl = new StageLoader(comboBox);

        try {
            comboBox.getSelectionModel().select(1);
        } catch (StackOverflowError e) {
            fail("Stack overflow should not happen here");
        }
    }

    @Test public void test_rt21186() {
        final ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(true);

        sl = new StageLoader(comboBox);

        assertNull(comboBox.getTooltip());
        assertNull(comboBox.getEditor().getTooltip());

        Tooltip tooltip = new Tooltip("Tooltip");
        comboBox.setTooltip(tooltip);
        assertEquals(tooltip, comboBox.getTooltip());
        assertEquals(tooltip, comboBox.getEditor().getTooltip());

        comboBox.setTooltip(null);
        assertNull(comboBox.getTooltip());
        assertNull(comboBox.getEditor().getTooltip());
    }

    @Test public void test_rt34573() {
        final ComboBox<String> comboBox = new ComboBox<>();

        final ListCell<String> customCell = new ListCellShim<>() {
            @Override public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
            }
        };
        comboBox.setButtonCell(customCell);

        sl = new StageLoader(comboBox);

        comboBox.setItems(FXCollections.observableArrayList("A","B","C","D"));
        comboBox.setValue("B");
        assertEquals("B", comboBox.getButtonCell().getText());
        assertEquals(1, comboBox.getButtonCell().getIndex());

        comboBox.setItems(FXCollections.observableArrayList("1","2","3","4"));
        assertNull(comboBox.getButtonCell().getText());
        assertEquals(-1, comboBox.getButtonCell().getIndex());
    }

    @Test public void test_rt34566() {
        final ComboBox<String> comboBox = new ComboBox<>();

        final ListCell<String> customCell = new ListCellShim<>() {
            @Override public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
            }
        };
        comboBox.setButtonCell(customCell);

        sl = new StageLoader(comboBox);

        comboBox.setItems(FXCollections.observableArrayList("A","B","C","D"));

        PseudoClass empty = PseudoClass.getPseudoClass("empty");

        comboBox.setValue("B");
        assertEquals("B", comboBox.getButtonCell().getText());
        assertEquals(1, comboBox.getButtonCell().getIndex());
        assertFalse(customCell.getPseudoClassStates().contains(empty));

        comboBox.setValue(null);
        Toolkit.getToolkit().firePulse();
        assertNull(comboBox.getButtonCell().getText());
        assertEquals(-1, comboBox.getButtonCell().getIndex());
        assertTrue(customCell.getPseudoClassStates().contains(empty));

        comboBox.setValue("A");
        assertEquals("A", comboBox.getButtonCell().getText());
        assertEquals(0, comboBox.getButtonCell().getIndex());
        assertFalse(customCell.getPseudoClassStates().contains(empty));
    }

    private int test_rt34603_count = 0;
    @Test public void test_rt34603() {
        assertEquals(0, test_rt34603_count);

        VBox hbox = new VBox(10);

        ComboBox<String> box = new ComboBox<>();
        box.getItems().add("test");
        box.setEditable(true);
        box.getSelectionModel().selectFirst();

        Button defaultButton = new Button("press");
        defaultButton.setOnAction(arg0 -> {
            test_rt34603_count++;
        });
        defaultButton.setDefaultButton(true);

        hbox.getChildren().addAll(box, defaultButton);

        sl = new StageLoader(hbox);

        box.getEditor().requestFocus();
        KeyEventFirer keyboard = new KeyEventFirer(box);
        keyboard.doKeyPress(KeyCode.ENTER);

        assertEquals(1, test_rt34603_count);
    }

    private int test_rt35586_count = 0;
    @Test public void test_rt35586() {
        assertEquals(0, test_rt35586_count);

        final ComboBox<String> cb = new ComboBox<>();
        cb.setEditable(true);
        cb.setOnAction(event -> {
            test_rt35586_count++;
            assertEquals("Test", cb.getEditor().getText());
        });

        sl = new StageLoader(cb);

        cb.requestFocus();
        cb.getEditor().setText("Test");
        KeyEventFirer keyboard = new KeyEventFirer(cb);
        keyboard.doKeyPress(KeyCode.ENTER);

        assertEquals(1, test_rt35586_count);
    }

    @Test public void test_rt35039() {
        final List<String> data = new ArrayList<>();
        data.add("aabbaa");
        data.add("bbc");

        final ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setItems(FXCollections.observableArrayList(data));

        sl = new StageLoader(combo);

        // everything should be null to start with
        assertNull(combo.getValue());
        assertTrue(combo.getEditor().getText().isEmpty());
        assertNull(combo.getSelectionModel().getSelectedItem());

        // select "bbc" and ensure everything is set to that
        combo.getSelectionModel().select(1);
        assertEquals("bbc", combo.getValue());
        assertEquals("bbc", combo.getEditor().getText());
        assertEquals("bbc", combo.getSelectionModel().getSelectedItem());

        // change the items list - but retain the same content. We expect
        // that "bbc" remains selected as it is still in the list
        combo.setItems(FXCollections.observableArrayList(data));
        assertEquals("bbc", combo.getValue());
        assertEquals("bbc", combo.getEditor().getText());
        assertEquals("bbc", combo.getSelectionModel().getSelectedItem());
    }

    @Test public void test_rt35840() {
        final ComboBox<String> cb = new ComboBox<>();
        cb.setEditable(true);
        sl = new StageLoader(cb);
        cb.requestFocus();

        KeyEventFirer keyboard = new KeyEventFirer(cb);
        keyboard.doKeyTyped(KeyCode.T);
        keyboard.doKeyTyped(KeyCode.E);
        keyboard.doKeyTyped(KeyCode.S);
        keyboard.doKeyTyped(KeyCode.T);
        assertEquals("TEST", cb.getEditor().getText());

        assertNull(cb.getValue());
        keyboard.doKeyPress(KeyCode.ENTER);
        assertEquals("TEST", cb.getValue());
    }

    @Test public void test_rt36280_nonEditable_F4ShowsPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
    }

    @Test public void test_rt36280_nonEditable_altUpShowsPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.UP, KeyModifier.ALT);  // show the popup
        assertTrue(cb.isShowing());
    }

    @Test public void test_rt36280_nonEditable_altDownShowsPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        new StageLoader(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.DOWN, KeyModifier.ALT);  // show the popup
        assertTrue(cb.isShowing());
    }

    @Test public void testEditorKeyInputsWhenPopupIsNotShowing() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b"));
        sl = new StageLoader(cb);
        KeyEventFirer keyboard = new KeyEventFirer(cb);
        cb.requestFocus();

        // Sanity
        assertFalse(cb.isShowing());
        assertEquals(null, cb.getValue());

        // Test DOWN and UP key
        keyboard.doKeyPress(KeyCode.DOWN);
        assertEquals("a", cb.getValue());
        keyboard.doKeyPress(KeyCode.DOWN);
        assertEquals("b", cb.getValue());
        keyboard.doKeyPress(KeyCode.UP);
        assertEquals("a", cb.getValue());
        keyboard.doKeyPress(KeyCode.UP);
        assertEquals("a", cb.getValue());
    }

    @Test public void testEditorKeyInputsWhenPopupIsShowing() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);
        KeyEventFirer keyboard = new KeyEventFirer(cb);

        // Show the popup
        assertFalse(cb.isShowing());
        cb.requestFocus();
        cb.getEditor().setText("ABC DEF");
        assertEquals("ABC DEF", cb.getEditor().getText());
        keyboard.doDownArrowPress(KeyModifier.ALT);
        // Sanity
        assertTrue(cb.isShowing());
        assertEquals(0, cb.getEditor().getCaretPosition());

        // LEFT, RIGHT keys with CTRL/META, SHIFT modifiers
        // Test RIGHT key
        keyboard.doRightArrowPress();
        assertEquals(1, cb.getEditor().getCaretPosition());

        // Test KP_RIGHT key
        keyboard.doKeyPress(KeyCode.KP_RIGHT);
        assertEquals(2, cb.getEditor().getCaretPosition());

        // Test LEFT key
        keyboard.doLeftArrowPress();
        assertEquals(1, cb.getEditor().getCaretPosition());

        // Test KP_LEFT key
        keyboard.doKeyPress(KeyCode.KP_LEFT);
        assertEquals(0, cb.getEditor().getCaretPosition());

        // Test SHIFT + RIGHT key
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT);
        assertEquals("A", cb.getEditor().getSelectedText());
        assertEquals(1, cb.getEditor().getCaretPosition());

        // Test SHIFT + LEFT key
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT);
        assertEquals("", cb.getEditor().getSelectedText());
        assertEquals(0, cb.getEditor().getCaretPosition());

        // Test ALT/CTRL + RIGHT key
        int expectedCaretPosition = 3;
        if (Utils.isWindows()) {
            expectedCaretPosition = 4;
        }
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.getWordTraversalKey());
        assertEquals("", cb.getEditor().getSelectedText());
        assertEquals(expectedCaretPosition, cb.getEditor().getCaretPosition());

        // Test CTRL/META + LEFT key
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.getShortcutKey());
        assertEquals("", cb.getEditor().getSelectedText());
        assertEquals(0, cb.getEditor().getCaretPosition());

        // Test ALT/CTRL + SHIFT + RIGHT key
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.getWordTraversalKey(), KeyModifier.SHIFT);
        assertEquals(expectedCaretPosition, cb.getEditor().getCaretPosition());
        assertEquals(cb.getEditor().getText().substring(0, expectedCaretPosition), cb.getEditor().getSelectedText());

        // Test CTRL/META + SHIFT + LEFT key
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertEquals("", cb.getEditor().getSelectedText());
        assertEquals(0, cb.getEditor().getCaretPosition());

        // HOME, END keys with CTRL/META, SHIFT modifiers
        // Test END key
        keyboard.doKeyPress(KeyCode.END);
        assertEquals(7, cb.getEditor().getCaretPosition());

        // Test HOME key
        keyboard.doKeyPress(KeyCode.HOME);
        assertEquals(0, cb.getEditor().getCaretPosition());

        // Test SHIFT + END key
        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT);
        assertEquals(cb.getEditor().getText(), cb.getEditor().getSelectedText());
        assertEquals(7, cb.getEditor().getCaretPosition());

        // Test SHIFT + HOME key
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT);
        assertEquals("", cb.getEditor().getSelectedText());
        assertEquals(0, cb.getEditor().getCaretPosition());

        // Test CTRL/META + END key
        keyboard.doKeyPress(KeyCode.END, KeyModifier.getShortcutKey());
        assertEquals("", cb.getEditor().getSelectedText());
        assertEquals(7, cb.getEditor().getCaretPosition());

        // Test CTRL/META + HOME key
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.getShortcutKey());
        assertEquals("", cb.getEditor().getSelectedText());
        assertEquals(0, cb.getEditor().getCaretPosition());

        // Test CTRL/META + SHIFT + END key
        keyboard.doKeyPress(KeyCode.END, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertEquals(cb.getEditor().getText(), cb.getEditor().getSelectedText());
        assertEquals(7, cb.getEditor().getCaretPosition());

        // Test CTRL/META + SHIFT + HOME key
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertEquals("", cb.getEditor().getSelectedText());
        assertEquals(0, cb.getEditor().getCaretPosition());

        // Test CTRL/META + A key
        keyboard.doLeftArrowPress();
        assertEquals("", cb.getEditor().getSelectedText());
        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
        assertEquals(cb.getEditor().getText(), cb.getEditor().getSelectedText());

        // Sanity
        assertTrue(cb.isShowing());
    }

    @Test public void testKeyInputsOnNonEditableComboBox() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        // cb.setEditable(false); // by default ComboBox is not editable
        sl = new StageLoader(cb);
        SingleSelectionModel<String> sm = cb.getSelectionModel();
        sm.select(0);
        KeyEventFirer keyboard = new KeyEventFirer(cb);

        // Show the popup
        assertFalse(cb.isShowing());
        cb.requestFocus();
        keyboard.doDownArrowPress(KeyModifier.ALT);
        // Sanity
        assertTrue(cb.isShowing());
        assertEquals(0, sm.getSelectedIndex());

        // Test RIGHT key
        keyboard.doRightArrowPress();
        assertEquals(0, sm.getSelectedIndex());

        // Test KP_RIGHT key
        keyboard.doKeyPress(KeyCode.KP_RIGHT);
        assertEquals(0, sm.getSelectedIndex());

        // Test DOWN key
        keyboard.doDownArrowPress();
        assertEquals(1, sm.getSelectedIndex());

        // Test KP_DOWN key
        keyboard.doKeyPress(KeyCode.KP_DOWN);
        assertEquals(2, sm.getSelectedIndex());

        // Test LEFT key
        keyboard.doLeftArrowPress();
        assertEquals(2, sm.getSelectedIndex());

        // Test KP_LEFT key
        keyboard.doKeyPress(KeyCode.KP_LEFT);
        assertEquals(2, sm.getSelectedIndex());

        // Test UP key
        keyboard.doUpArrowPress();
        assertEquals(1, sm.getSelectedIndex());

        // Test KP_UP key
        keyboard.doKeyPress(KeyCode.KP_UP);
        assertEquals(0, sm.getSelectedIndex());

        // Test END key
        keyboard.doKeyPress(KeyCode.END);
        assertEquals(2, sm.getSelectedIndex());

        // Test HOME key
        keyboard.doKeyPress(KeyCode.HOME);
        assertEquals(0, sm.getSelectedIndex());

        // Sanity
        assertTrue(cb.isShowing());
    }

    @Test public void testInterceptedKeyMappingsForComboBoxEditor() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        sl = new StageLoader(cb);

        ListView listView = (ListView) ((ComboBoxListViewSkin)cb.getSkin()).getPopupContent();
        ListViewBehavior lvBehavior = (ListViewBehavior)ControlSkinFactory.getBehavior(listView.getSkin());
        InputMap<ListView<?>> lvInputMap = lvBehavior.getInputMap();
        ObservableList<?> inputMappings = lvInputMap.getMappings();
        // In ListViewBehavior KeyMappings for vertical orientation are added under 3rd child InputMap
        InputMap<ListView<?>> verticalInputMap = lvInputMap.getChildInputMaps().get(2);
        ObservableList<?> verticalInputMappings = verticalInputMap.getMappings();

        cb.setEditable(true);
        testKeyMappingsForEditableCB(inputMappings);
        testCommonKeyMappings(inputMappings, verticalInputMappings);

        cb.setEditable(false);
        testKeyMappingsForNonEditableCB(inputMappings);
        testCommonKeyMappings(inputMappings, verticalInputMappings);
    }

    private void testKeyMappingsForEditableCB(ObservableList<?> inputMappings) {
        testInterceptor(inputMappings, new KeyBinding(KeyCode.HOME), true);
        testInterceptor(inputMappings, new KeyBinding(KeyCode.END), true);
    }

    private void testKeyMappingsForNonEditableCB(ObservableList<?> inputMappings) {
        testInterceptor(inputMappings, new KeyBinding(KeyCode.HOME), false);
        testInterceptor(inputMappings, new KeyBinding(KeyCode.END), false);
    }

    private void testCommonKeyMappings(ObservableList<?> inputMappings,
                                       ObservableList<?> verticalInputMappings) {
        // Verify FocusTraversalInputMap
        for(InputMap.Mapping<?> mapping : FocusTraversalInputMap.getFocusTraversalMappings()) {
            assertFalse(inputMappings.contains(mapping));
        }

        // Verify default InputMap
        testInterceptor(inputMappings, new KeyBinding(KeyCode.HOME).shift(), true);
        testInterceptor(inputMappings, new KeyBinding(KeyCode.END).shift(), true);
        testInterceptor(inputMappings, new KeyBinding(KeyCode.HOME).shortcut(), true);
        testInterceptor(inputMappings, new KeyBinding(KeyCode.END).shortcut(), true);
        testInterceptor(inputMappings, new KeyBinding(KeyCode.A).shortcut(), true);

        // Verify vertical child InputMap
        testInterceptor(verticalInputMappings, new KeyBinding(KeyCode.HOME).shortcut().shift(), true);
        testInterceptor(verticalInputMappings, new KeyBinding(KeyCode.END).shortcut().shift(), true);
    }

    private void testInterceptor(ObservableList<?> mappings, KeyBinding binding, boolean isIntercepted) {
        int i = mappings.indexOf(new KeyMapping(binding, null));
        if (((KeyMapping)mappings.get(i)).getInterceptor() != null) {
            assertEquals(isIntercepted, ((KeyMapping) mappings.get(i)).getInterceptor().test(null));
        } else {
            // JDK-8209788 added interceptor for few KeyMappings
            fail("Interceptor must not be null");
        }
    }

    @Test public void test_rt36280_nonEditable_enterHidesShowingPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = (ListView) ((ComboBoxListViewSkin)cb.getSkin()).getPopupContent();
        assertNotNull(listView);

        KeyEventFirer lvKeyboard = new KeyEventFirer(listView);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
        lvKeyboard.doKeyPress(KeyCode.ENTER);  // hide the popup
        assertFalse(cb.isShowing());
    }

    @Test public void test_rt36280_nonEditable_spaceHidesShowingPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = (ListView) ((ComboBoxListViewSkin)cb.getSkin()).getPopupContent();
        assertNotNull(listView);

        KeyEventFirer lvKeyboard = new KeyEventFirer(listView);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
        lvKeyboard.doKeyPress(KeyCode.SPACE);  // hide the popup
        assertFalse(cb.isShowing());
    }

    @Test public void test_rt36280_nonEditable_escapeHidesShowingPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = (ListView) ((ComboBoxListViewSkin)cb.getSkin()).getPopupContent();
        assertNotNull(listView);

        KeyEventFirer lvKeyboard = new KeyEventFirer(listView);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
        lvKeyboard.doKeyPress(KeyCode.ESCAPE);  // hide the popup
        assertFalse(cb.isShowing());
    }

    @Test public void test_rt36280_nonEditable_F4HidesShowingPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // hide the popup
        assertFalse(cb.isShowing());
    }

    @Test public void test_rt36280_nonEditable_arrowKeysChangeSelection() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());

        assertNull(cb.getSelectionModel().getSelectedItem());

        cbKeyboard.doDownArrowPress();
        assertEquals("a", cb.getSelectionModel().getSelectedItem());

        cbKeyboard.doDownArrowPress();
        assertEquals("b", cb.getSelectionModel().getSelectedItem());

        cbKeyboard.doUpArrowPress();
        assertEquals("a", cb.getSelectionModel().getSelectedItem());
    }

    @Test public void test_rt36280_editable_F4ShowsPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
    }

    @Test public void test_rt36280_editable_altUpShowsPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.UP, KeyModifier.ALT);  // show the popup
        assertTrue(cb.isShowing());
    }

    @Test public void test_rt36280_editable_altDownShowsPopup_onComboBox() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        assertTrue(cb.getEditor().getText().isEmpty());
        cbKeyboard.doKeyPress(KeyCode.DOWN, KeyModifier.ALT);  // show the popup
        assertTrue(cb.isShowing());
        assertTrue(cb.getEditor().getText().isEmpty());
    }

    @Test public void test_rt36280_editable_altDownShowsPopup_onTextField() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);

        KeyEventFirer tfKeyboard = new KeyEventFirer(cb.getEditor());
        assertFalse(cb.isShowing());
        assertTrue(cb.getEditor().getText().isEmpty());
        tfKeyboard.doKeyPress(KeyCode.DOWN, KeyModifier.ALT);  // show the popup
        assertTrue(cb.isShowing());
        assertTrue(cb.getEditor().getText().isEmpty());
    }

    @Test public void test_rt36280_editable_enterHidesShowingPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = (ListView) ((ComboBoxListViewSkin)cb.getSkin()).getPopupContent();
        assertNotNull(listView);

        KeyEventFirer lvKeyboard = new KeyEventFirer(listView);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
        lvKeyboard.doKeyPress(KeyCode.ENTER);  // hide the popup
        assertFalse(cb.isShowing());
    }

    @Test public void test_rt36280_editable_spaceHidesShowingPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = (ListView) ((ComboBoxListViewSkin)cb.getSkin()).getPopupContent();
        assertNotNull(listView);

        KeyEventFirer lvKeyboard = new KeyEventFirer(listView);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
        lvKeyboard.doKeyPress(KeyCode.SPACE);  // hide the popup
        assertFalse(cb.isShowing());
    }

    @Test public void test_rt36280_editable_escapeHidesShowingPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = (ListView) ((ComboBoxListViewSkin)cb.getSkin()).getPopupContent();
        assertNotNull(listView);

        KeyEventFirer lvKeyboard = new KeyEventFirer(listView);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
        lvKeyboard.doKeyPress(KeyCode.ESCAPE);  // hide the popup
        assertFalse(cb.isShowing());
    }

    @Test public void test_rt36280_editable_F4HidesShowingPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // hide the popup
        assertFalse(cb.isShowing());
    }

    @Test public void test_rt36280_editable_arrowKeysChangeSelection() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());

        assertNull(cb.getSelectionModel().getSelectedItem());

        cbKeyboard.doDownArrowPress();
        assertEquals("a", cb.getSelectionModel().getSelectedItem());

        cbKeyboard.doDownArrowPress();
        assertEquals("b", cb.getSelectionModel().getSelectedItem());

        cbKeyboard.doUpArrowPress();
        assertEquals("a", cb.getSelectionModel().getSelectedItem());
    }

    @Test public void test_rt36651() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        sl = new StageLoader(cb);

        assertNull(cb.getValue());
        assertEquals(-1, cb.getSelectionModel().getSelectedIndex());
        assertNull(cb.getSelectionModel().getSelectedItem());

        sl.getStage().requestFocus();
        cb.show();
        Toolkit.getToolkit().firePulse();

        // selection should not change just by showing the popup
        assertNull(cb.getValue());
        assertEquals(-1, cb.getSelectionModel().getSelectedIndex());
        assertNull(cb.getSelectionModel().getSelectedItem());
    }

    @Test public void test_rt36717() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        sl = new StageLoader(cb);

        // the stack overflow only occurs when a ComboBox changes from non-editable to editable
        cb.setEditable(false);
        cb.setEditable(true);
        assertNotNull(cb.getEditor());
        KeyEventFirer tfKeyboard = new KeyEventFirer(cb.getEditor());
        tfKeyboard.doKeyPress(KeyCode.ENTER);   // Stack overflow here
    }

    @Test public void test_rt36827() {
        final Button btn = new Button("focus owner");
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        VBox vbox = new VBox(btn, cb);

        sl = new StageLoader(vbox);
        sl.getStage().requestFocus();
        btn.requestFocus();
        Toolkit.getToolkit().firePulse();
        Scene scene = sl.getStage().getScene();

        assertTrue(btn.isFocused());
        assertEquals(btn, scene.getFocusOwner());

        MouseEventFirer mouse = new MouseEventFirer(cb);
        mouse.fireMousePressAndRelease();

        assertTrue(cb.isShowing());
        assertTrue(cb.isFocused());
        assertEquals(cb, scene.getFocusOwner());
    }

    @Test public void test_rt36902() {
        final ComboBox<String> cb1 = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        final ComboBox<String> cb2 = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb2.setEditable(true);
        VBox vbox = new VBox(cb1, cb2);

        // lame - I would rather have one keyboard here but I couldn't get it to
        // work, so watch out for which keyboard is used below
        KeyEventFirer cb1Keyboard = new KeyEventFirer(cb1);
        KeyEventFirer cb2Keyboard = new KeyEventFirer(cb2);

        sl = new StageLoader(vbox);
        sl.getStage().requestFocus();
        cb1.requestFocus();
        Toolkit.getToolkit().firePulse();
        Scene scene = sl.getStage().getScene();

        assertTrue(cb1.isFocused());
        assertEquals(cb1, scene.getFocusOwner());

        // move focus forward to cb2
        cb1Keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(cb2.isFocused());
        assertEquals(cb2, scene.getFocusOwner());

        // move focus forward again to cb1
        cb2Keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(cb1.isFocused());
        assertEquals(cb1, scene.getFocusOwner());

        // now start going backwards with shift-tab.
        // The first half of the bug is here - when we shift-tab into cb2, we
        // actually go into the FakeFocusTextField subcomponent, so whilst the
        // cb2.isFocused() returns true as expected, the scene focus owner is
        // not the ComboBox, but the FakeFocusTextField inside it
        cb1Keyboard.doKeyPress(KeyCode.TAB, KeyModifier.SHIFT);
        assertTrue("Expect cb2 to be focused, but actual focus owner is: " + scene.getFocusOwner(),
                cb2.isFocused());
        // Updated with fix for RT-34602: The TextField now never gets
        // focus (it's just faking it).
        // assertEquals("Expect cb2 TextField to be focused, but actual focus owner is: " + scene.getFocusOwner(),
        //         cb2.getEditor(), scene.getFocusOwner());
        assertEquals("Expect cb2 to be focused, but actual focus owner is: " + scene.getFocusOwner(),
                     cb2, scene.getFocusOwner());

        // This is where the second half of the bug appears, as we are stuck in
        // the FakeFocusTextField of cb2, we never make it to cb1
        cb2Keyboard.doKeyPress(KeyCode.TAB, KeyModifier.SHIFT);
        assertTrue(cb1.isFocused());
        assertEquals(cb1, scene.getFocusOwner());
    }

    private int rt_38901_counter;
    @Test public void test_rt_38901_selectNull() {
        test_rt_38901(true);
    }

    @Test public void test_rt_38901_selectNegativeOne() {
        test_rt_38901(false);
    }

    private void test_rt_38901(boolean selectNull) {
        rt_38901_counter = 0;

        final ComboBox<String> cb = new ComboBox<>();
        cb.setOnShowing((e) -> {
            cb.getItems().setAll("DUMMY " + (rt_38901_counter++));
        });

        assertEquals(-1, cb.getSelectionModel().getSelectedIndex());
        assertNull(cb.getSelectionModel().getSelectedItem());
        assertNull(cb.getValue());
        assertEquals(0, cb.getItems().size());

        // round one
        cb.show();
        assertEquals(1, cb.getItems().size());
        assertEquals("DUMMY 0", cb.getItems().get(0));
        cb.hide();

        cb.getSelectionModel().select(0);
        assertEquals(0, cb.getSelectionModel().getSelectedIndex());
        assertEquals("DUMMY 0", cb.getSelectionModel().getSelectedItem());
        assertEquals("DUMMY 0", cb.getValue());

        if (selectNull) cb.getSelectionModel().select(null);
        else cb.getSelectionModel().select(-1);

        assertEquals(-1, cb.getSelectionModel().getSelectedIndex());
        assertNull(cb.getSelectionModel().getSelectedItem());
        assertNull(cb.getValue());


        // round two
        cb.show();
        assertEquals(1, cb.getItems().size());
        assertEquals("DUMMY 1", cb.getItems().get(0));
        cb.hide();

        cb.getSelectionModel().select(0);
        assertEquals(0, cb.getSelectionModel().getSelectedIndex());
        assertEquals("DUMMY 1", cb.getSelectionModel().getSelectedItem());
        assertEquals("DUMMY 1", cb.getValue());

        if (selectNull) cb.getSelectionModel().select(null);
        else cb.getSelectionModel().select(-1);

        assertEquals(-1, cb.getSelectionModel().getSelectedIndex());
        assertNull(cb.getSelectionModel().getSelectedItem());
        assertNull(cb.getValue());
    }

    private int rt_22572_counter;
    @Test public void test_rt_22572() {
        rt_22572_counter = 0;

        final ComboBox<String> cb = new ComboBox<>();
        cb.setOnShowing((e) -> {
            cb.getItems().setAll("DUMMY " + (rt_22572_counter++));
        });

        sl = new StageLoader(cb);

        assertEquals(-1, cb.getSelectionModel().getSelectedIndex());
        assertNull(cb.getSelectionModel().getSelectedItem());
        assertNull(cb.getValue());
        assertEquals(0, cb.getItems().size());

        // round one
        cb.show();
        assertEquals(1, cb.getItems().size());
        assertEquals("DUMMY 0", cb.getItems().get(0));
        cb.hide();

        cb.getSelectionModel().select(0);
        assertEquals(0, cb.getSelectionModel().getSelectedIndex());
        assertEquals("DUMMY 0", cb.getSelectionModel().getSelectedItem());
        assertEquals("DUMMY 0", cb.getValue());


        // round two - even though the items change, the value should still be
        // the old value (even though it doesn't exist in the items list any longer).
        // The selectedIndex and selectedItem do get reset however.
        cb.show();
        assertEquals(1, cb.getItems().size());
        assertEquals("DUMMY 1", cb.getItems().get(0));
        cb.hide();

        assertEquals(-1, cb.getSelectionModel().getSelectedIndex());
        assertNull(cb.getSelectionModel().getSelectedItem());
        assertEquals("DUMMY 0", cb.getValue());
    }

    private int rt_22937_counter;
    @Test public void test_rt_22937() {
        rt_22937_counter = 0;

        final ComboBox<String> cb = new ComboBox<>();
        cb.setOnShowing((e) -> {
            cb.getItems().setAll("DUMMY " + (rt_22937_counter++));
        });

        cb.getItems().add("Toto");
        cb.setEditable(true);
        cb.setValue("Tata");

        sl = new StageLoader(cb);

        assertEquals(-1, cb.getSelectionModel().getSelectedIndex());
        assertEquals("Tata", cb.getSelectionModel().getSelectedItem());
        assertEquals("Tata", cb.getValue());
        assertEquals(1, cb.getItems().size());

        cb.show();
        assertEquals(1, cb.getItems().size());
        assertEquals("DUMMY 0", cb.getItems().get(0));
        cb.hide();

        cb.getSelectionModel().select(0);
        assertEquals(0, cb.getSelectionModel().getSelectedIndex());
        assertEquals("DUMMY 0", cb.getSelectionModel().getSelectedItem());
        assertEquals("DUMMY 0", cb.getValue());
    }

    @Test public void test_rt_39809() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(null, "1", "2", "3");

        sl = new StageLoader(comboBox);

        comboBox.getSelectionModel().clearAndSelect(1);
        assertEquals("1", comboBox.getSelectionModel().getSelectedItem());
        assertEquals(1, comboBox.getSelectionModel().getSelectedIndex());

        comboBox.getSelectionModel().clearAndSelect(0);
        assertEquals(null, comboBox.getSelectionModel().getSelectedItem());
        assertEquals(0, comboBox.getSelectionModel().getSelectedIndex());
    }

    @Test public void test_rt_39908() {
        ObservableList<String> model = FXCollections.observableArrayList("0", "1", "2", "3");
        ComboBox<String> comboBox = new ComboBox<>(model);

        sl = new StageLoader(comboBox);

        comboBox.getSelectionModel().clearAndSelect(1);
        assertEquals("1", comboBox.getSelectionModel().getSelectedItem());
        assertEquals(1, comboBox.getSelectionModel().getSelectedIndex());

        model.set(0, "a");
        assertEquals("1", comboBox.getSelectionModel().getSelectedItem());
        assertEquals(1, comboBox.getSelectionModel().getSelectedIndex());
    }

    /**
     * Bullet 1: selected index must be updated
     * Corner case: last selected. Fails for core
     */
    @Test public void test_rt_40012_selectedAtLastOnDisjointRemoveItemsAbove() {
        ObservableList<String> items = FXCollections.observableArrayList("0", "1", "2", "3", "4", "5");
        ComboBox<String> comboBox = new ComboBox<>(items);
        SelectionModel sm = comboBox.getSelectionModel();

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
        ComboBox<String> comboBox = new ComboBox<>(items);
        SelectionModel sm = comboBox.getSelectionModel();

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
        ComboBox<String> comboBox = new ComboBox<>(items);
        SelectionModel sm = comboBox.getSelectionModel();

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
        ComboBox<String> comboBox = new ComboBox<>(items);
        SelectionModel sm = comboBox.getSelectionModel();

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

    @Test public void test_jdk_8150946_testCommit_valid() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        assertEquals(null, comboBox.getValue());
        comboBox.getEditor().setText("ABC");
        comboBox.commitValue();
        assertEquals("ABC", comboBox.getValue());
    }

    @Test public void test_jdk_8150946_testCancel_toNull() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        assertNull(comboBox.getValue());
        assertEquals("", comboBox.getEditor().getText());
        comboBox.getEditor().setText("ABC");
        assertNull(comboBox.getValue());
        comboBox.cancelEdit();
        assertNull(comboBox.getValue());
        assertNull(comboBox.getEditor().getText());
    }

    @Test public void test_jdk_8150946_testCancel_toNonNull() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        comboBox.getEditor().setText("ABC");
        comboBox.commitValue();
        assertEquals("ABC", comboBox.getValue());
        assertEquals("ABC", comboBox.getEditor().getText());
        comboBox.getEditor().setText("DEF");
        assertEquals("DEF", comboBox.getEditor().getText());
        assertEquals("ABC", comboBox.getValue());
        comboBox.cancelEdit();
        assertEquals("ABC", comboBox.getValue());
        assertEquals("ABC", comboBox.getEditor().getText());
    }

    @Test public void test_jdk_8160493() {
        AtomicInteger count = new AtomicInteger();
        comboBox.valueProperty().addListener(o -> count.incrementAndGet());
        assertEquals(0, count.get());

        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select(1);
        assertTrue(sm.isSelected(1));
        assertEquals(1, count.get());
        assertEquals("Orange", comboBox.getValue());

        comboBox.setValue("New Value");
        assertEquals(2, count.get());
        assertEquals("New Value", comboBox.getValue());
    }

    private int skinChangedCount = 0;
    @Test public void test_JDK_8185854() {
        final FlowPane comboPane = new FlowPane(10, 10);
        ComboBox combo = new ComboBox<String>();

        combo.skinProperty().addListener((o, oldSkin, newSkin) -> {
            skinChangedCount++;
        });

        combo.setDisable(false);
        combo.setEditable(false);

        comboPane.getChildren().add(combo);

        TabPane tabPane = new TabPane();
        Tab tab = new Tab();
        tab.setText("ComboBox");
        tab.setContent(comboPane);
        tabPane.getTabs().add(tab);

        BorderPane p = new BorderPane();
        p.setCenter(tabPane);

        Scene scene = new Scene(p);
        scene.getStylesheets().add(ComboBoxTest.class.getResource("JDK_8185854.css").toExternalForm());

        Toolkit tk = Toolkit.getToolkit();

        sl = new StageLoader(scene);

        tk.firePulse();

        assertEquals("ComboBox skinProperty changed more than once, which is not expected.", 1, skinChangedCount);
    }

    //JDK-8279140
    @Test
    public void testSelectionOnItemChangeUsingSetAllThroughPropertyBinding() {
        ObservableList<String> comboBoxItemsList = FXCollections.observableArrayList();
        ObjectProperty<String> selectedValue = new SimpleObjectProperty<>();

        List<String> strings1 = List.of("A", "B", "C");
        List<String> strings2 = List.of("D", "E", "F");

        comboBox = new ComboBox<>();
        comboBox.setItems(comboBoxItemsList);

        selectedValue.addListener((obs, oldValue, newValue) -> {
            if ("D".equals(newValue) || "A".equals(newValue)) {
                List<String> newContent = "A".equals(newValue) ? strings1 : strings2;
                comboBoxItemsList.setAll(newContent);
            }
        });

        comboBox.valueProperty().bindBidirectional(selectedValue);

        selectedValue.set("A");
        assertEquals("A", comboBox.getSelectionModel().getSelectedItem());
        assertEquals("A", selectedValue.get());

        selectedValue.set("D");
        assertEquals("D", comboBox.getSelectionModel().getSelectedItem());
        assertEquals("D", selectedValue.get());
    }

    //JDK-8279139
    @Test
    public void testSelectionOnItemChangeUsingSetAllOnButtonPress() {
        ObservableList<String> comboBoxItemsList = FXCollections.observableArrayList();

        List<String> strings1 = List.of("A", "B", "C");
        List<String> strings2 = List.of("D", "B", "F");

        comboBox = new ComboBox<>();
        comboBox.setItems(comboBoxItemsList);
        comboBox.setValue("B");

        comboBoxItemsList.setAll(strings2);
        assertEquals("B", comboBox.getSelectionModel().getSelectedItem());
        comboBoxItemsList.setAll(strings1);
        assertEquals("B", comboBox.getSelectionModel().getSelectedItem());

        Button button = new Button("Change content");
        button.setOnAction(e -> {
            if (comboBoxItemsList.equals(strings1)) {
                comboBoxItemsList.setAll(strings2);
            } else {
                comboBoxItemsList.setAll(strings1);
            }
        });

        MouseEventFirer mouse = new MouseEventFirer(button);
        mouse.fireMousePressAndRelease();
        assertEquals("B", comboBox.getSelectionModel().getSelectedItem());
        mouse.fireMousePressAndRelease();
        assertEquals("B", comboBox.getSelectionModel().getSelectedItem());
    }
}

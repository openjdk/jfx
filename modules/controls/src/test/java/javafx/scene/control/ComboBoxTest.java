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

import com.sun.javafx.scene.control.infrastructure.KeyModifier;
import com.sun.javafx.tk.Toolkit;
import javafx.css.PseudoClass;

import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import com.sun.javafx.scene.control.skin.ListViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.*;

import java.util.*;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ComboBoxTest {
    private ComboBox<String> comboBox;
    private SingleSelectionModel<String> sm;
    
    /*********************************************************************
     *                                                                   *
     * Utility methods                                                   *
     *                                                                   *
     ********************************************************************/    
    
    public ListView getListView() {
        return ((ComboBoxListViewSkin)comboBox.getSkin()).getListView();
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
        comboBox = new ComboBox<String>();
        sm = comboBox.getSelectionModel();
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
        assertEquals("", comboBox.getPromptText());
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
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertStyleClassContains(b2, "combo-box");
    }
    
    @Test public void singleArgConstructorSetsNonNullSelectionModel() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertNotNull(b2.getSelectionModel());
    }

    @Test public void singleArgConstructorAllowsNullItems() {
        final ComboBox<String> b2 = new ComboBox<String>(null);
        assertNull(b2.getItems());
    }
    
    @Test public void singleArgConstructorTakesItems() {
        ObservableList<String> items = FXCollections.observableArrayList("Hi");
        final ComboBox<String> b2 = new ComboBox<String>(items);
        assertSame(items, b2.getItems());
    }
    
    @Test public void singleArgConstructor_selectedItemIsNull() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getSelectionModel().getSelectedItem());
    }
    
    @Test public void singleArgConstructor_selectedIndexIsNegativeOne() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertEquals(-1, b2.getSelectionModel().getSelectedIndex());
    }
    
    @Test public void singleArgConstructor_valueIsNull() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getValue());
    }
    
    @Test public void singleArgConstructor_editableIsFalse() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertFalse(b2.isEditable());
    }
    
    @Test public void singleArgConstructor_showingIsFalse() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertFalse(b2.isShowing());
    }
    
    @Test public void singleArgConstructor_promptTextIsEmptyString() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertEquals("", b2.getPromptText());
    }
    
    @Test public void singleArgConstructor_placeholderIsNull() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getPlaceholder());
    }
    
    @Test public void singleArgConstructor_armedIsFalse() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertEquals(false, b2.isArmed());
    }
    
    @Test public void singleArgConstructor_converterIsNotNull() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertNotNull(b2.getConverter());
    }
    
    @Test public void singleArgConstructor_cellFactoryIsNull() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertNull(b2.getCellFactory());
    }
    
    @Test public void singleArgConstructor_visibleRowFactoryIs10() {
        final ComboBox<String> b2 = new ComboBox<String>(FXCollections.observableArrayList("Hi"));
        assertEquals(10, b2.getVisibleRowCount());
    }
    
    /*********************************************************************
     * Tests for selection model                                         *
     ********************************************************************/
    
    @Test public void selectionModelCanBeNull() {
        comboBox.setSelectionModel(null);
        assertNull(comboBox.getSelectionModel());
    }

    @Test public void selectionModelCanBeBound() {
        SingleSelectionModel<String> sm = new ComboBox.ComboBoxSelectionModel<String>(comboBox);
        ObjectProperty<SingleSelectionModel<String>> other = new SimpleObjectProperty<SingleSelectionModel<String>>(sm);
        comboBox.selectionModelProperty().bind(other);
        assertSame(sm, sm);
    }

    @Test public void selectionModelCanBeChanged() {
        SingleSelectionModel<String> sm = new ComboBox.ComboBoxSelectionModel<String>(comboBox);
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
        new ComboBox.ComboBoxSelectionModel<String>(null);
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
        
        SingleSelectionModel sm2 = new ComboBox.ComboBoxSelectionModel(comboBox);
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
        sm.setSelectedItem("pineapple");
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
        assertEquals("42", sc.toString(new Integer(42)));
    }
    
    @Test(expected=ClassCastException.class)
    public void defaultConverterCanHandleIncorrectType_2() {
        ComboBox<Integer> cb = new ComboBox<Integer>();
        StringConverter<Integer> sc = cb.getConverter();
        Integer value = sc.fromString("42");
    }
    
    @Test public void defaultConverterCanHandleNullValues() {
        StringConverter<String> sc = comboBox.getConverter();
        assertEquals(null, sc.fromString(null));
        assertEquals(null, sc.toString(null));
    }
    
    @Test public void ensureImpl_getPseudoClassStateReturnsValidValue() {
        Set<PseudoClass> pseudoClassStates = comboBox.getPseudoClassStates();
        assertFalse(comboBox.isEditable());
        assertTrue(pseudoClassStates.size() >= 0);

        comboBox.setEditable(true);
        pseudoClassStates = comboBox.getPseudoClassStates();
        assertTrue(pseudoClassStates.contains(PseudoClass.getPseudoClass("editable")));

        comboBox.setEditable(false);
        pseudoClassStates = comboBox.getPseudoClassStates();
        assertTrue(pseudoClassStates.contains(PseudoClass.getPseudoClass("editable")) == false);

        comboBox.show();
        pseudoClassStates = comboBox.getPseudoClassStates();
        assertTrue(pseudoClassStates.contains(PseudoClass.getPseudoClass("showing")));

        comboBox.hide();
        pseudoClassStates = comboBox.getPseudoClassStates();
        assertTrue(pseudoClassStates.contains(PseudoClass.getPseudoClass("showing")) == false);

        comboBox.arm();
        pseudoClassStates = comboBox.getPseudoClassStates();
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
        comboBox.show();
        assertTrue(comboBox.isShowing());
        comboBox.hide();
        assertFalse(comboBox.isShowing());
    }
    
    @Test public void ensureCanNotToggleShowingWhenDisabled() {
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
    
    @Ignore("Test not working as the heights being returned are not accurate")
    @Test public void test_rt20106() {
        comboBox.getItems().addAll("0","1","2","3","4","5","6","7","8","9");
        
        Stage stage = new Stage();
        Scene scene = new Scene(comboBox);
        stage.setScene(scene);
        comboBox.impl_processCSS(true);
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
        comboBox.impl_processCSS(true);
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
//        listView.impl_processCSS(true);
        
        assertEquals("2", listView.getSelectionModel().getSelectedItem());
        
        System.out.println(listView.getSkin());
        
        VirtualFlow flow = (VirtualFlow)listView.lookup("#virtual-flow");
        assertNotNull(flow);
        
        IndexedCell cell = flow.getVisibleCell(2);
        System.out.println("cell: " + cell);
        assertEquals("TO_STRING", cell.getText());
    } 
    
    @Ignore
    @Test public void test_rt20189() {
        comboBox.getItems().addAll("0","1","2","3","4","5","6","7","8","9");
        
        Stage stage = new Stage();
        Scene scene = new Scene(comboBox);
        stage.setScene(scene);
        comboBox.impl_processCSS(true);
        comboBox.show();
        
        SelectionModel sm = getListView().getSelectionModel();
        
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
        
        Stage stage = new Stage();
        Scene scene = new Scene(comboBox);
        stage.setScene(scene);
        comboBox.impl_processCSS(true);
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
        assertEquals("", buttonCell.getText());
        assertEquals(-1, sm.getSelectedIndex());
        
        sm.select(2);
        assertEquals("2", sm.getSelectedItem());
        assertEquals("2", comboBox.getValue());
        assertEquals("2", buttonCell.getText());
        assertEquals(2, sm.getSelectedIndex());
    }
    
    @Test public void test_rt24412() {
        SingleSelectionModel sm = comboBox.getSelectionModel();
        
        Stage stage = new Stage();
        Scene scene = new Scene(comboBox);
        stage.setScene(scene);
        comboBox.impl_processCSS(true);
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
        assertEquals("", buttonCell.getText());
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
        
        ComboBox<String> comboBox = new ComboBox<String>();
        comboBox.setItems(strings);
        comboBox.setEditable(true);
        comboBox.valueProperty().addListener((ov, t, t1) -> {
            if (t == null && t1.isEmpty()) {
                fail("Old value is '" + t + "' and new value is '" + t1 + "'.");
            }
        });
        
        StageLoader stageLoader = new StageLoader(comboBox);
        stageLoader.getStage().show();
        
        assertNull(comboBox.getValue());
        assertTrue(comboBox.getEditor().getText().isEmpty());
        
        comboBox.requestFocus();
        
        new KeyEventFirer(comboBox).doKeyPress(KeyCode.ENTER);
    }

    @Test public void test_rt31479() {
        ComboBox<String> comboBox = new ComboBox<String>();

        StageLoader stageLoader = new StageLoader(comboBox);
        stageLoader.getStage().show();

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

        StageLoader stageLoader = new StageLoader(comboBox);
        stageLoader.getStage().show();

        try {
            comboBox.getSelectionModel().select(1);
        } catch (StackOverflowError e) {
            fail("Stack overflow should not happen here");
        }
    }

    @Test public void test_rt21186() {
        final ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(true);

        new StageLoader(comboBox);

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

        final ListCell<String> customCell = new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
            }
        };
        comboBox.setButtonCell(customCell);

        new StageLoader(comboBox);

        comboBox.setItems(FXCollections.observableArrayList("A","B","C","D"));
        comboBox.setValue("B");
        assertEquals("B", comboBox.getButtonCell().getText());
        assertEquals(1, comboBox.getButtonCell().getIndex());

        comboBox.setItems(FXCollections.observableArrayList("1","2","3","4"));
        assertTrue(comboBox.getButtonCell().getText().isEmpty());
        assertEquals(-1, comboBox.getButtonCell().getIndex());
    }

    @Test public void test_rt34566() {
        final ComboBox<String> comboBox = new ComboBox<>();

        final ListCell<String> customCell = new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
            }
        };
        comboBox.setButtonCell(customCell);

        new StageLoader(comboBox);

        comboBox.setItems(FXCollections.observableArrayList("A","B","C","D"));

        PseudoClass empty = PseudoClass.getPseudoClass("empty");

        comboBox.setValue("B");
        assertEquals("B", comboBox.getButtonCell().getText());
        assertEquals(1, comboBox.getButtonCell().getIndex());
        assertFalse(customCell.getPseudoClassStates().contains(empty));

        comboBox.setValue(null);
        Toolkit.getToolkit().firePulse();
        assertTrue(comboBox.getButtonCell().getText().isEmpty());
        assertEquals(-1, comboBox.getButtonCell().getIndex());
        assertTrue(customCell.getPseudoClassStates().contains(empty));

        comboBox.setValue("A");
        assertEquals("A", comboBox.getButtonCell().getText());
        assertEquals(0, comboBox.getButtonCell().getIndex());
        assertFalse(customCell.getPseudoClassStates().contains(empty));
    }

    private int test_rt34603_count = 0;
    @Ignore("Bug has not yet been resolved")
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

        new StageLoader(hbox);

        box.getEditor().requestFocus();
        KeyEventFirer keyboard = new KeyEventFirer(box);
        keyboard.doKeyPress(KeyCode.ENTER);

        assertEquals(1, test_rt34603_count);
    }

    private int test_rt35586_count = 0;
    @Test public void test_rt35586() {
        assertEquals(0, test_rt34603_count);

        final ComboBox<String> cb = new ComboBox<String>();
        cb.setEditable(true);
        cb.setOnAction(event -> {
            test_rt35586_count++;
            assertEquals("Test", cb.getEditor().getText());
        });

        new StageLoader(cb);

        cb.getEditor().requestFocus();
        cb.getEditor().setText("Test");
        KeyEventFirer keyboard = new KeyEventFirer(cb.getEditor());
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

        new StageLoader(combo);

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
        final ComboBox<String> cb = new ComboBox<String>();
        cb.setEditable(true);
        StageLoader sl = new StageLoader(cb);
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
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
    }

    @Test public void test_rt36280_nonEditable_altUpShowsPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.UP, KeyModifier.ALT);  // show the popup
        assertTrue(cb.isShowing());
    }

    @Test public void test_rt36280_nonEditable_altDownShowsPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        new StageLoader(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.DOWN, KeyModifier.ALT);  // show the popup
        assertTrue(cb.isShowing());
    }

    @Test public void test_rt36280_nonEditable_enterHidesShowingPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = ((ComboBoxListViewSkin)cb.getSkin()).getListView();
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
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = ((ComboBoxListViewSkin)cb.getSkin()).getListView();
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
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = ((ComboBoxListViewSkin)cb.getSkin()).getListView();
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
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // hide the popup
        assertFalse(cb.isShowing());
    }

    @Test public void test_rt36280_nonEditable_arrowKeysChangeSelection() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        new StageLoader(cb);
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
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(cb.isShowing());
    }

    @Test public void test_rt36280_editable_altUpShowsPopup() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        assertFalse(cb.isShowing());
        cbKeyboard.doKeyPress(KeyCode.UP, KeyModifier.ALT);  // show the popup
        assertTrue(cb.isShowing());
    }

    @Test public void test_rt36280_editable_altDownShowsPopup_onComboBox() {
        final ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
        cb.setEditable(true);
        new StageLoader(cb);
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
        new StageLoader(cb);

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
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = ((ComboBoxListViewSkin)cb.getSkin()).getListView();
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
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = ((ComboBoxListViewSkin)cb.getSkin()).getListView();
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
        new StageLoader(cb);
        KeyEventFirer cbKeyboard = new KeyEventFirer(cb);

        ListView listView = ((ComboBoxListViewSkin)cb.getSkin()).getListView();
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
        new StageLoader(cb);
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
        new StageLoader(cb);
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
}

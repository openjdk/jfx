/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.javafx.preview.control;

import javafx.scene.control.*;
import static javafx.scene.control.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.*;

import java.util.Arrays;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ComboBoxTest {
    private ComboBox<String> comboBox;
    
    @Before public void setup() {
        comboBox = new ComboBox<String>();
    }
    
    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void noArgConstructorSetsTheStyleClass() {
        assertStyleClassContains(comboBox, "combo-box");
    }
    
    @Test public void noArgConstructorSetsNonNullSelectionModel() {
        assertNotNull(comboBox.getSelectionModel());
    }
    
    @Test public void noArgConstructorSetsNonNullItems() {
        assertNotNull(comboBox.getItems());
    }
    
    @Test public void noArgConstructor_selectedItemIsNull() {
        assertNull(comboBox.getSelectionModel().getSelectedItem());
    }
    
    @Test public void noArgConstructor_selectedIndexIsNegativeOne() {
        assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
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
        SelectionModel<String> sm = new ComboBox.ComboBoxSelectionModel<String>(comboBox);
        ObjectProperty<SelectionModel<String>> other = new SimpleObjectProperty<SelectionModel<String>>(sm);
        comboBox.selectionModelProperty().bind(other);
        assertSame(sm, comboBox.getSelectionModel());
    }

    @Test public void selectionModelCanBeChanged() {
        SelectionModel<String> sm = new ComboBox.ComboBoxSelectionModel<String>(comboBox);
        comboBox.setSelectionModel(sm);
        assertSame(sm, comboBox.getSelectionModel());
    }
    
    @Test public void canSetSelectedItemToAnItemEvenWhenThereAreNoItems() {
        final String randomString = new String("I AM A CRAZY RANDOM STRING");
        comboBox.getSelectionModel().select(randomString);
        assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
        assertSame(randomString, comboBox.getSelectionModel().getSelectedItem());
    }
        
    @Test public void canSetSelectedItemToAnItemNotInTheDataModel() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        final String randomString = new String("I AM A CRAZY RANDOM STRING");
        comboBox.getSelectionModel().select(randomString);
        assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
        assertSame(randomString, comboBox.getSelectionModel().getSelectedItem());
    }
        
    @Test public void settingTheSelectedItemToAnItemInItemsResultsInTheCorrectSelectedIndex() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.getSelectionModel().select("Orange");
        assertEquals(1, comboBox.getSelectionModel().getSelectedIndex());
        assertSame("Orange", comboBox.getSelectionModel().getSelectedItem());
    }
    
    @Test public void settingTheSelectedItemToANonexistantItemAndThenSettingItemsWhichContainsItResultsInCorrectSelectedIndex() {
        comboBox.getSelectionModel().select("Orange");
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertEquals(1, comboBox.getSelectionModel().getSelectedIndex());
        assertSame("Orange", comboBox.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex0() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.getSelectionModel().select(0);
        comboBox.getItems().clear();
        assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals(null, comboBox.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionClearsWhenAllItemsAreRemoved_selectIndex2() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.getSelectionModel().select(2);
        comboBox.getItems().clear();
        assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals(null, comboBox.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectedItemRemainsAccurateWhenItemsAreCleared() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.getSelectionModel().select(2);
        comboBox.getItems().clear();
        assertNull(comboBox.getSelectionModel().getSelectedItem());
        assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
        
        comboBox.getItems().addAll("Kiwifruit", "Mandarin", "Pineapple");
        comboBox.getSelectionModel().select(2);
        assertEquals("Pineapple", comboBox.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionShiftsDownWhenOneNewItemIsAdded() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.getSelectionModel().select(1);
        assertEquals(1, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", comboBox.getSelectionModel().getSelectedItem());
        
        comboBox.getItems().add(0, "Kiwifruit");
        assertEquals(2, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", comboBox.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionShiftsDownWhenMultipleNewItemAreAdded() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.getSelectionModel().select(1);
        assertEquals(1, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", comboBox.getSelectionModel().getSelectedItem());
        
        comboBox.getItems().addAll(0, Arrays.asList("Kiwifruit", "Pineapple", "Mandarin"));
        assertEquals("Orange", comboBox.getSelectionModel().getSelectedItem());
        assertEquals(4, comboBox.getSelectionModel().getSelectedIndex());
    }
    
    @Test public void ensureSelectionShiftsUpWhenOneItemIsRemoved() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.getSelectionModel().select(1);
        assertEquals(1, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", comboBox.getSelectionModel().getSelectedItem());
        
        comboBox.getItems().remove("Apple");
        assertEquals(0, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", comboBox.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionShiftsUpWheMultipleItemsAreRemoved() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.getSelectionModel().select(2);
        assertEquals(2, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals("Banana", comboBox.getSelectionModel().getSelectedItem());
        
        comboBox.getItems().removeAll(Arrays.asList("Apple", "Orange"));
        assertEquals(0, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals("Banana", comboBox.getSelectionModel().getSelectedItem());
    }
    
    @Test public void ensureSelectionIsCorrectWhenItemsChange() {
        comboBox.setItems(FXCollections.observableArrayList("Item 1"));
        comboBox.getSelectionModel().select(0);
        assertEquals("Item 1", comboBox.getSelectionModel().getSelectedItem());
        
        comboBox.setItems(FXCollections.observableArrayList("Item 2"));
        assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals(null, comboBox.getSelectionModel().getSelectedItem());
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
        comboBox.getSelectionModel().select(0);
        assertEquals("Apple", comboBox.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectItem() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        comboBox.getSelectionModel().select("Apple");
        assertEquals("Apple", comboBox.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectPrevious() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        comboBox.getSelectionModel().select(2);
        comboBox.getSelectionModel().selectPrevious();
        assertEquals("Orange", comboBox.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectNext() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        comboBox.getSelectionModel().select("Apple");
        comboBox.getSelectionModel().selectNext();
        assertEquals("Orange", comboBox.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectFirst() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        comboBox.getSelectionModel().selectFirst();
        assertEquals("Apple", comboBox.getValue());
    }
    
    @Test public void ensureSelectionModelUpdatesValueProperty_withSelectLast() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        comboBox.getSelectionModel().selectLast();
        assertEquals("Banana", comboBox.getValue());
    }
    
    @Test public void ensureSelectionModelClearsValueProperty() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        comboBox.getSelectionModel().select(0);
        assertEquals("Apple", comboBox.getValue());
        
        comboBox.getSelectionModel().clearSelection();
        assertNull(comboBox.getValue());
    }
    
    @Test public void ensureSelectionModelClearsValuePropertyWhenNegativeOneSelected() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        assertNull(comboBox.getValue());
        comboBox.getSelectionModel().select(0);
        assertEquals("Apple", comboBox.getValue());
        
        comboBox.getSelectionModel().select(-1);
        assertNull("Expected null, actual value: " + comboBox.getValue(), comboBox.getValue());
    }
    
    @Test public void ensureValueIsCorrectWhenItemsIsAddedToWithExistingSelection() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.getSelectionModel().select(1);
        
        comboBox.getItems().add(0, "Kiwifruit");
        
        assertEquals(2, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", comboBox.getSelectionModel().getSelectedItem());
        assertEquals("Orange", comboBox.getValue());
    }
    
    @Test public void ensureValueIsCorrectWhenItemsAreRemovedWithExistingSelection() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        comboBox.getSelectionModel().select(1);
        
        comboBox.getItems().remove("Apple");
        
        assertEquals(0, comboBox.getSelectionModel().getSelectedIndex());
        assertEquals("Orange", comboBox.getSelectionModel().getSelectedItem());
        assertEquals("Orange", comboBox.getValue());
    }
    
    @Test public void ensureValueIsUpdatedByCorrectSelectionModelWhenSelectionModelIsChanged() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        SelectionModel sm1 = comboBox.getSelectionModel();
        sm1.select(1);
        assertEquals("Orange", comboBox.getValue());
        
        SelectionModel sm2 = new ComboBox.ComboBoxSelectionModel(comboBox);
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
        
        comboBox.getSelectionModel().select(1);
        assertEquals("empty", comboBox.getValue());
    }
    
    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/
    
    @Test public void checkPromptTextPropertyName() {
        assertTrue(comboBox.promptTextProperty().getName().equals("promptText"));
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
        long value1 = comboBox.impl_getPseudoClassState();
        assertTrue(value1 >= 0);
        
        comboBox.setEditable(true);
        long value2 = comboBox.impl_getPseudoClassState();
        assertTrue(value2 >= 0);
        
        comboBox.show();
        long value3 = comboBox.impl_getPseudoClassState();
        assertTrue(value3 >= 0);
        
        comboBox.arm();
        long value4 = comboBox.impl_getPseudoClassState();
        assertTrue(value4 >= 0);
        
        assertTrue(value1 != value2 && value1 != value3 && value1 != value4
                                    && value2 != value3 && value2 != value4
                                                        && value3 != value4);
    }
    
    /*********************************************************************
     * Tests for properties                                              *
     ********************************************************************/
    
    @Test public void ensureAllowsNullConverter() {
        comboBox.setConverter(null);
        assertNull(comboBox.getConverter());
    }
    
    @Test public void ensureCanSetNonNullCellFactory() {
        Callback<ListView<String>, ListCell<String>> cf = new Callback<ListView<String>, ListCell<String>>() {
            @Override public ListCell<String> call(ListView<String> p) { return null; }
        };
        comboBox.setCellFactory(cf);
        assertEquals(cf, comboBox.getCellFactory());
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
        EventHandler<ActionEvent> onAction = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) { }
        };
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
        assertTrue("PromptText cannot be bound", comboBox.getValue().equals("value"));
        strPr.setValue("newvalue");
        assertTrue("PromptText cannot be bound", comboBox.getValue().equals("newvalue"));
    }
}

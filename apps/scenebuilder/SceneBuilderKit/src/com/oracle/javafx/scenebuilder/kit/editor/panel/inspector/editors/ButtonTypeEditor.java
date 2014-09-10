/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ButtonTypePropertyMetadata;

/**
 * Editor of the DialogPane.buttonTypes property. It may contain several
 * ButtonType, that have their dedicated class (ButtonTypeItem).
 *
 *
 */
public class ButtonTypeEditor extends InlineListEditor {

    private static Map<String, ButtonType> predefinedButtonsNames = new TreeMap<>();
    private Collection<ButtonType> buttonList = new TreeSet<ButtonType>(getButtonTypeComparator());

    public ButtonTypeEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        initialize();
    }

    private void initialize() {
        setLayoutFormat(PropertyEditor.LayoutFormat.DOUBLE_LINE);
        // Build a sorted map, with button names as keys
        for (Entry<ButtonType, String> entry : ButtonTypePropertyMetadata.getButtonTypeMap().entrySet()) {
            predefinedButtonsNames.put(entry.getValue(), entry.getKey());
        }
        updateButtonLists();
        addItem(getNewButtonTypeItem());
    }

    private ButtonTypeItem getNewButtonTypeItem() {
        return new ButtonTypeItem(this, buttonList);
    }

    private void updateButtonLists() {
        // As we don't want several buttons with the same type,
        // we need to prevent this by updating the buttons types list in each
        // item.
        buttonList.clear();
        buttonList.addAll(predefinedButtonsNames.values());
        for (EditorItem item : getEditorItems()) {
            Object itemValueObj = item.getValue();
            if (itemValueObj == null) {
                continue;
            }
            assert itemValueObj instanceof ButtonType;
            ButtonType itemValue = (ButtonType) itemValueObj;
            buttonList.remove(itemValue);
        }
        for (EditorItem item : getEditorItems()) {
            assert item instanceof ButtonTypeItem;
            ((ButtonTypeItem) item).updateButtonList(buttonList);
        }
    }

    private Comparator<ButtonType> getButtonTypeComparator() {
        Comparator<ButtonType> comparator = (ButtonType bt1, ButtonType bt2) -> bt1.getText().compareTo(bt2.getText());
        return comparator;
    }

    @Override
    public Object getValue() {
        List<ButtonType> value = FXCollections.observableArrayList();
        // Group all the item values in a list
        for (EditorItem buttonTypeItem : getEditorItems()) {
            Object itemValueObj = buttonTypeItem.getValue();
            if (itemValueObj == null) {
                continue;
            }
            assert itemValueObj instanceof ButtonType;
            ButtonType itemValue = (ButtonType) itemValueObj;
            value.add(itemValue);
        }
        if (value.isEmpty()) {
            // no button type
            return super.getPropertyMeta().getDefaultValueObject();
        } else {
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (value == null) {
            reset();
            return;
        }
        assert value instanceof List;
        // Warning : value is the editing list.
        // We do not want to set the valueProperty() to editing list
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        Iterator<EditorItem> itemsIter = new ArrayList<>(getEditorItems()).iterator();
        for (ButtonType item : (List<ButtonType>) value) {
            EditorItem editorItem;
            if (itemsIter.hasNext()) {
                // re-use the current items first
                editorItem = itemsIter.next();
                assert editorItem instanceof ButtonTypeItem;
                ((ButtonTypeItem)editorItem).reset(predefinedButtonsNames.values());
            } else {
                // additional items required
                editorItem = addItem(getNewButtonTypeItem());
            }
            editorItem.setValue(item);
        }
        // Empty the remaining items, if needed
        while (itemsIter.hasNext()) {
            EditorItem editorItem = itemsIter.next();
            removeItem(editorItem);
        }

        // Update the button list
        updateButtonLists();
    }

    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
        buttonList.clear();
        buttonList.addAll(predefinedButtonsNames.values());
        // add an empty item
        addItem(getNewButtonTypeItem());
    }

    @Override
    public void requestFocus() {
        EditorItem firstItem = getEditorItems().get(0);
        assert firstItem instanceof ButtonTypeItem;
        ((ButtonTypeItem) firstItem).requestFocus();
    }

    @Override
    public void commit(EditorItem source) {
        super.commit(source);
        updateButtonLists();
    }

    @Override
    public void remove(EditorItem source) {
        super.remove(source);
        updateButtonLists();
    }

    /**
     ***************************************************************************
     *
     * ButtonType item : button types ChoiceBox, and +/- buttons.
     *
     ***************************************************************************
     */
    private class ButtonTypeItem implements EditorItem {

        @FXML
        private Button plusBt;
        @FXML
        private Button minusBt;
        @FXML
        private ChoiceBox<ButtonType> buttonTypeCb;

        private Parent root;
        private EditorItemDelegate editor;

        public ButtonTypeItem(EditorItemDelegate editor, Collection<ButtonType> buttonList) {
//            System.out.println("New ButtonTypeItem.");
            // It is an AutoSuggestEditor without MenuButton
            initialize(editor, buttonList);
        }

        // Method to please FindBugs
        private void initialize(EditorItemDelegate editor, Collection<ButtonType> buttonList) {
            this.editor = editor;
            root = EditorUtils.loadFxml("ButtonTypeEditorItem.fxml", this);//NOI18N

            buttonTypeCb.setConverter(getButtonTypeConverter());
            buttonTypeCb.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<ButtonType>() {
                public void changed(javafx.beans.value.ObservableValue<? extends ButtonType> value,
                        ButtonType prevValue, ButtonType newValue) {
                    editor.commit(ButtonTypeItem.this);
                    updatePlusMinusButtons();
                };
            });

            updateButtonList(buttonList);
            updatePlusMinusButtons();
        }

        public void updateButtonList(Collection<ButtonType> buttonList) {
            // Remove ChoiceBox items that are not in the new list (but keep the
            // current value)
            for (ButtonType buttonType : buttonList) {
                if (!buttonTypeCb.getItems().contains(buttonType)) {
                    buttonTypeCb.getItems().add(buttonType);
                }
            }
            ArrayList<ButtonType> currentItems = new ArrayList<>(buttonTypeCb.getItems());
            for (ButtonType buttonType : currentItems) {
                if (!buttonList.contains(buttonType) && buttonType != getValue()) {
                    buttonTypeCb.getItems().remove(buttonType);
                }
            }
        }

        @Override
        public final Node getNode() {
            return root;
        }

        @Override
        public Object getValue() {
            return buttonTypeCb.getSelectionModel().getSelectedItem();
        }

        @Override
        public void setValue(Object buttonType) {
            assert buttonType instanceof ButtonType;
            buttonTypeCb.getSelectionModel().select((ButtonType) buttonType);
            updatePlusMinusButtons();
        }

        @Override
        public void reset() {
            buttonTypeCb.getSelectionModel().clearSelection();
        }
        
        public void reset(Collection<ButtonType> buttonList) {
            buttonTypeCb.getItems().clear();
            updateButtonList(buttonList);
        }
        

        // Please findBugs
        public void requestFocus() {
            buttonTypeCb.requestFocus();
        }

        @Override
        public void setValueAsIndeterminate() {
            handleIndeterminate(buttonTypeCb);
        }

        @Override
        public MenuItem getMoveUpMenuItem() {
            // not used here
            return null;
        }

        @Override
        public MenuItem getMoveDownMenuItem() {
            // not used here
            return null;
        }

        @Override
        public MenuItem getRemoveMenuItem() {
            // not used here
            return null;
        }

        @Override
        public Button getPlusButton() {
            return plusBt;
        }

        @Override
        public Button getMinusButton() {
            return minusBt;
        }

        @FXML
        void add(ActionEvent event) {
            ButtonTypeEditor.ButtonTypeItem buttonTypeItem = getNewButtonTypeItem();
            editor.add(this, buttonTypeItem);
            buttonTypeItem.requestFocus();
        }

        @FXML
        void remove(ActionEvent event) {
            editor.remove(this);
        }

        @FXML
        void plusBtTyped(KeyEvent event) {
            if (event.getCode() == KeyCode.ENTER) {
                editor.add(this, getNewButtonTypeItem());
            }
        }

        private void updatePlusMinusButtons() {
            if (buttonTypeCb.getSelectionModel().isEmpty()) {
                // if no selection, or no additional possible button,
                // disable plus and minus buttons
                plusBt.setDisable(true);
                minusBt.setDisable(true);
            } else {
                // enable plus and minus
                plusBt.setDisable(false);
                minusBt.setDisable(false);
            }
        }

        @SuppressWarnings("unused")
        protected void disablePlusButton(boolean disable) {
            plusBt.setDisable(disable);
        }

        @SuppressWarnings("unused")
        protected void disableMinusButton(boolean disable) {
            minusBt.setDisable(disable);
        }

        private StringConverter<ButtonType> getButtonTypeConverter() {
            Map<ButtonType, String> predefinedButtons = ButtonTypePropertyMetadata.getButtonTypeMap();
            return new StringConverter<ButtonType>() {

                @Override
                public String toString(ButtonType buttonType) {
                    return predefinedButtons.get(buttonType);
                }

                @Override
                public ButtonType fromString(String buttonName) {
                    for (Entry<ButtonType, String> entry : predefinedButtons.entrySet()) {
                        if (entry.getValue().equals(buttonName)) {
                            return entry.getKey();
                        }
                    }
                    assert false;
                    return null;
                }
            };
        }

    }
}

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

import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Abstract editor that provide a text field with an auto-suggest popup.
 *
 *
 */
public abstract class AutoSuggestEditor extends PropertyEditor {

    @FXML
    public ListView<String> suggestedLv;
    @FXML
    public TextField textField;
    @FXML
    public DoubleField doubleField;
    @FXML
    public IntegerField integerField;
    @FXML
    public MenuButton menuButton;

    private Parent root;
    private TextField entryField;
    private List<String> suggestedList;
    private boolean suggest = true;
    private Type type = Type.ALPHA; // Default is a simple text field
    private boolean showMenuButton = true;

    public static enum Type {

        ALPHA, // TextField
        DOUBLE, // DoubleField
        INTEGER // IntegerField
    }

    public AutoSuggestEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, List<String> suggestedList) {
        super(propMeta, selectedClasses);
        preInit(suggestedList);
    }

    public AutoSuggestEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, List<String> suggestedList, Type type) {
        super(propMeta, selectedClasses);
        this.type = type;
        preInit(suggestedList);
    }

    public AutoSuggestEditor(String name, String defaultValue, List<String> suggestedList) {
        this(name, defaultValue, suggestedList, Type.ALPHA, true);
    }
    
    public AutoSuggestEditor(String name, String defaultValue, List<String> suggestedList, Type type) {
        this(name, defaultValue, suggestedList, type, true);
    }
    
    public AutoSuggestEditor(String name, String defaultValue, List<String> suggestedList, boolean showMenuButton) {
        this(name, defaultValue, suggestedList, Type.ALPHA, showMenuButton);
    }
    
    public AutoSuggestEditor(String name, String defaultValue, List<String> suggestedList, Type type, boolean showMenuButton) {
        super(name, defaultValue);
        this.type = type;
        this.showMenuButton = showMenuButton;
        preInit(suggestedList);
    }

    private void preInit(List<String> suggestedList) {
        setSuggestedList(suggestedList);
        if (type == Type.ALPHA) {
            root = EditorUtils.loadFxml("StringAutoSuggestEditor.fxml", this); //NOI18N
            assert textField != null;
            entryField = textField;
        } else if (type == Type.DOUBLE) {
            root = EditorUtils.loadFxml("DoubleAutoSuggestEditor.fxml", this); //NOI18N
            entryField = doubleField;
        } else {
            assert type == Type.INTEGER;
            root = EditorUtils.loadFxml("IntegerAutoSuggestEditor.fxml", this); //NOI18N
            entryField = integerField;
        }

        HBox.setHgrow(root, Priority.ALWAYS);
        initialize();
    }

    private void initialize() {
        entryField.focusedProperty().addListener((ChangeListener<Boolean>) (ov, prevVal, newVal) -> {
            if (newVal) {
                // Getting focus: show the popup
                suggest = true;
                handleSuggestedPopup();
            } else {
                // Loosing focus: hide the popup
                hidePopup();
            }
        });
        // Align popup with (at least its list view) with property text field
//        suggestedLv.prefWidthProperty().bind(entryField.widthProperty());

        updateMenuButtonIfNeeded();
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(root);
    }

    @Override
    public Object getValue() {
        String value = entryField.getText();
        if (value == null) {
            return null;
        }
        return EditorUtils.getPlainString(value);
    }

    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        if (value == null) {
            entryField.setText(null);
        } else {
            assert value instanceof String;
            entryField.setText((String) value); //NOI18N
        }
    }

    @Override
    protected void valueIsIndeterminate() {
        handleIndeterminate(entryField);
    }

    @Override
    public void requestFocus() {
        EditorUtils.doNextFrame(() -> entryField.requestFocus());
    }

    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, List<String> suggestedList) {
        super.reset(propMeta, selectedClasses);
        resetSuggestedList(suggestedList);
    }

    public void reset(String name, String defaultValue, List<String> suggestedList) {
        super.reset(name, defaultValue);
        resetSuggestedList(suggestedList);
    }

    protected void resetSuggestedList(List<String> suggestedList) {
        setSuggestedList(suggestedList);
        updateMenuButtonIfNeeded();
        entryField.setPromptText(null);
    }
    
    protected List<String> getSuggestedList() {
        return suggestedList;
    }
    
    private void setSuggestedList(List<String> suggestedList) {
        Collections.sort(suggestedList);
        this.suggestedList = suggestedList;
    }

    protected Parent getRoot() {
        return root;
    }

    public TextField getTextField() {
        return entryField;
    }

    @FXML
    protected void suggestedLvKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            useSuggested();
        }
        if (event.getCode() == KeyCode.ESCAPE) {
            hidePopup();
            suggest = false;
        }
    }

    @FXML
    protected void suggestedLvMousePressed(MouseEvent event) {
        useSuggested();
    }

    @FXML
    protected void textFieldKeyReleased(KeyEvent event) {
//                System.out.println("Key code : " + event.getCode());
        if ((event.getCode() == KeyCode.ENTER) || (event.getCode() == KeyCode.UP)
                || (event.getCode() == KeyCode.ESCAPE)) {
            return;
        }
        if (event.getCode() == KeyCode.DOWN) {
            // 'Down' key shows the popup even if popup has been disabled
            suggest = true;
            suggestedLv.requestFocus();
        }
        handleSuggestedPopup();
    }

    @FXML
    protected void textFieldMouseClicked(MouseEvent event) {
    }

    private void initConstants() {
        if (type == Type.DOUBLE) {
            assert entryField instanceof DoubleField;
            ((DoubleField) entryField).setConstants(suggestedList);
        } else if (type == Type.INTEGER) {
            assert entryField instanceof IntegerField;
            ((IntegerField) entryField).setConstants(suggestedList);
        }
        addConstantsInMenuButton();
    }

    private void handleSuggestedPopup() {
        String value = entryField.getText();
        if (!suggest) {
            // Suggest popup is disabled
            if (value == null || value.isEmpty()) {
                // Suggest popup is re-enabled when text is empty
                suggest = true;
            } else {
                return;
            }
        }

        List<String> suggestedItems;
        suggestedItems = getSuggestedItems(value, value);
        // If the suggested list is empty, or contains a single element equals to the current value,
        // hide the popup
        if (suggestedItems.isEmpty()
                || ((suggestedItems.size() == 1) && suggestedItems.get(0).equals(value))) {
            hidePopup();
        } else {
            showPopup(suggestedItems);
        }
    }

    private List<String> getSuggestedItems(String filter, String currentValue) {
        List<String> suggestedItems = new ArrayList<>();
        if (filter == null || currentValue == null) {
            // Return the whole suggestedList
            return suggestedList;
        }
        // We don't want to be case sensitive
        filter = filter.toLowerCase(Locale.ROOT);
        currentValue = currentValue.toLowerCase(Locale.ROOT);
        for (String suggestItem : suggestedList) {
            String suggestItemLower = suggestItem.toLowerCase(Locale.ROOT);
            if (suggestItemLower.contains(filter)) {
                // We don't want to suggest the already used value
                if (suggestItemLower.equals(currentValue)) {
                    continue;
                }
                suggestedItems.add(suggestItem);
            }
        }
        return suggestedItems;
    }

    private void showPopup(List<String> suggestedItems) {
        if (!suggestedLv.getItems().equals(suggestedItems)) {
            suggestedLv.setItems(FXCollections.observableArrayList(suggestedItems));
        }
        if (entryField.getContextMenu().isShowing() == false) {
//                System.out.println("showPopup");
            suggestedLv.getSelectionModel().clearSelection();
            // popup x coordinate need to be slightly moved, so that the popup is centered 
            entryField.getContextMenu().show(entryField, Side.BOTTOM, 0, 0);
        }
    }

    private void hidePopup() {
        if (entryField.getContextMenu().isShowing() == true) {
            entryField.getContextMenu().hide();

        }
    }

    private void useSuggested() {
        if (!suggestedLv.getSelectionModel().isEmpty()) {
            String selected = suggestedLv.getSelectionModel().getSelectedItem();
            entryField.setText(selected);
            entryField.requestFocus();
            entryField.selectEnd();
        }
        hidePopup();
    }

    private void updateMenuButtonIfNeeded() {
        assert menuButton != null;
        if (!showMenuButton || suggestedList.isEmpty()) {
            menuButton.setVisible(false);
            menuButton.setManaged(false);
        } else {
            menuButton.setVisible(true);
            menuButton.setManaged(true);
            initConstants();
        }
    }

    private void addConstantsInMenuButton() {
        assert menuButton != null;
        menuButton.getItems().clear();
        for (String suggestItem : suggestedList) {
            MenuItem menuItem = new MenuItem(suggestItem);
//            MenuItem menuItem = new MenuItem();
            menuItem.setMnemonicParsing(false);
            menuItem.setOnAction(t -> {
                entryField.setText(suggestItem);
                if (AutoSuggestEditor.this.getCommitListener() != null) {
                    AutoSuggestEditor.this.getCommitListener().handle(null);
                }
            });
            // Set the font on each item. Should be done from the FontEditor
//            Text graphic = new Text(suggestItem);
//            graphic.setFont(new Font(suggestItem, 14));
//            menuItem.setGraphic(graphic);
            menuButton.getItems().add(menuItem);
        }
    }

}

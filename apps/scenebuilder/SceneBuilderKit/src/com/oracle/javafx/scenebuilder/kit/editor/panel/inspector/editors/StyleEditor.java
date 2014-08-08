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

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import com.sun.javafx.css.CssError;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.parser.CSSParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

/**
 * Editor of the 'style' property. It may contain several css rules, that have
 * their dedicated class (StyleItem).
 *
 *
 */
public class StyleEditor extends InlineListEditor {

    private List<String> cssProperties;
    private Set<Class<?>> selectedClasses;
    private EditorController editorController;

    @SuppressWarnings("LeakingThisInConstructor")
    public StyleEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, EditorController editorController) {
        super(propMeta, selectedClasses);
        this.selectedClasses = selectedClasses;
        this.editorController = editorController;
        setLayoutFormat(LayoutFormat.DOUBLE_LINE);
        addItem(getNewStyleItem());
    }

    private StyleItem getNewStyleItem() {
        if (cssProperties == null) {
            cssProperties = CssInternal.getCssProperties(selectedClasses);
        }
        return new StyleItem(this, cssProperties);
    }

    @Override
    public void commit(EditorItem source) {
        try {
            userUpdateValueProperty(getValue());
        } catch (Exception ex) {
            editorController.getMessageLog().logWarningMessage(
                    "inspector.style.valuetypeerror", ex.getMessage());
        }
    }

    @Override
    public Object getValue() {
        // Concatenate all the item values
        String value = null;
        for (EditorItem styleItem : getEditorItems()) {
            String itemValue = styleItem.getValue();
            if (itemValue.isEmpty()) {
                continue;
            }
            if (value == null) {
                value = ""; //NOI18N
            }
            assert styleItem instanceof StyleItem;
            if (((StyleItem) styleItem).hasParsingError()) {
                editorController.getMessageLog().logWarningMessage(
                        "inspector.style.parsingerror", itemValue);
            }
            value += itemValue + " "; //NOI18N
        }
        if (value != null) {
            value = value.trim();
        }
        if (value == null) {
            // no style
            return super.getPropertyMeta().getDefaultValueObject();
        } else {
            return value;
        }
    }

    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }
        if (value == null) {
            reset();
            return;
        }
        assert value instanceof String;
        String[] itemArray = ((String) value).split(";");
        Iterator<EditorItem> itemsIter = new ArrayList<>(getEditorItems()).iterator();
        for (String item : itemArray) {
            item = item.trim();
            if (item.isEmpty()) {
                continue;
            }
            EditorItem editorItem;
            if (itemsIter.hasNext()) {
                // re-use the current items first
                editorItem = itemsIter.next();
            } else {
                // additional items required
                editorItem = addItem(getNewStyleItem());
            }
            editorItem.setValue(item);
        }
        // Empty the remaining items, if needed
        while (itemsIter.hasNext()) {
            EditorItem editorItem = itemsIter.next();
            removeItem(editorItem);
        }
    }

    @Override
    boolean isValueChanged(Object value) {
        if (((value == null) && (valueProperty().getValue() != null))
                || ((value != null) && (valueProperty().getValue() == null))) {
            return true;
        }

        if (value != null) {
            // Compare the values without spaces, since the fxml file could have 
            // a different formatting than the one we generate.
            assert value instanceof String;
            assert valueProperty().getValue() instanceof String;
            String oldNoSpace = ((String) valueProperty().getValue()).replaceAll("\\s", "");
            String newNoSpace = ((String) value).replaceAll("\\s", "");
            if (!oldNoSpace.equals(newNoSpace)) {
                return true;
            }
        }
        return false;
    }

    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, EditorController editorController) {
        super.reset(propMeta, selectedClasses);
        this.selectedClasses = selectedClasses;
        this.editorController = editorController;
        cssProperties = null;
        // add an empty item
        addItem(getNewStyleItem());
    }

    @Override
    public void requestFocus() {
        EditorItem firstItem = getEditorItems().get(0);
        assert firstItem instanceof StyleItem;
        ((StyleItem) firstItem).requestFocus();
    }

    /**
     ***************************************************************************
     *
     * Style item : property + value text fields, and +/action buttons.
     *
     ***************************************************************************
     */
    private class StyleItem extends AutoSuggestEditor implements EditorItem {

        @FXML
        private Button plusBt;
        @FXML
        private MenuItem removeMi;
        @FXML
        private MenuItem moveUpMi;
        @FXML
        private MenuItem moveDownMi;
        @FXML
        private TextField valueTf;
        @FXML
        private StackPane propertySp;

        private final Parent root;
        private TextField propertyTf;
        private String currentValue;
        private final EditorItemDelegate editor;
        private boolean parsingError = false;
        private ListChangeListener<CssError> errorListener;

        @SuppressWarnings("LeakingThisInConstructor")
        public StyleItem(EditorItemDelegate editor, List<String> suggestedList) {
//            System.out.println("New StyleItem.");
            // It is an AutoSuggestEditor without MenuButton
            super("", "", suggestedList, false);
            this.editor = editor;
            root = EditorUtils.loadFxml("StyleEditorItem.fxml", this);

            initialize();
        }

        // Method to please FindBugs
        private void initialize() {
            // Add the AutoSuggest text field in the scene graph
            propertySp.getChildren().add(super.getRoot());

            propertyTf = super.getTextField();
            EventHandler<ActionEvent> onActionListener = event -> {
//                    System.out.println("StyleItem : onActionListener");
                if (getValue().equals(currentValue)) {
                    // no change
                    return;
                }
                if (!propertyTf.getText().isEmpty() && !valueTf.getText().isEmpty()) {
//                        System.out.println("StyleEditorItem : COMMIT");
                    editor.commit(StyleItem.this);
                    if (event != null && event.getSource() instanceof TextField) {
                        ((TextField) event.getSource()).selectAll();
                    }
                }
                if (propertyTf.getText().isEmpty() && valueTf.getText().isEmpty()) {
                    remove(null);
                }

                updateButtons();
                currentValue = getValue();
            };

            ChangeListener<String> textPropertyChange = (ov, prevText, newText) -> {
                if (prevText.isEmpty() || newText.isEmpty()) {
                    // Text changed FROM empty value, or TO empty value: buttons status change
                    updateButtons();
                }
            };

            propertyTf.textProperty().addListener(textPropertyChange);
            valueTf.textProperty().addListener(textPropertyChange);
            updateButtons();

            // Do not add a generic focus listener on each of the text fields,
            // but implement a specific one.
            setTextEditorBehavior(propertyTf, onActionListener, false);
            setTextEditorBehavior(valueTf, onActionListener, false);
            ChangeListener<Boolean> focusListener = (observable, oldValue, newValue) -> {
                if (!newValue) {
                    // focus lost: commit
                    editor.editing(false, onActionListener);
                } else {
                    // got focus
                    editor.editing(true, onActionListener);
                }
            };
            propertyTf.focusedProperty().addListener(focusListener);
            valueTf.focusedProperty().addListener(focusListener);

            // Initialize menu items text
            removeMi.setText(I18N.getString("inspector.list.remove"));
            moveUpMi.setText(I18N.getString("inspector.list.moveup"));
            moveDownMi.setText(I18N.getString("inspector.list.movedown"));

            errorListener = change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        for (CssError error : change.getAddedSubList()) {
                            if (error instanceof CssError.InlineStyleParsingError) {
                                parsingError = true;
                                break;
                            }
                        }
                    }
                }
            };

        }

        @Override
        public final Node getNode() {
            return root;
        }

        @Override
        public String getValue() {
            String value;
            if (propertyTf.getText().isEmpty() && valueTf.getText().isEmpty()) {
                return ""; //NOI18N
            } else {
                String propertyVal = EditorUtils.getPlainString(propertyTf.getText()).trim();
                String valueVal = EditorUtils.getPlainString(valueTf.getText()).trim();
                value = propertyVal + ": " + valueVal + ";"; //NOI18N
            }

            // Parse the style, and set the parsingError boolean if any error
            parsingError = false;
            StyleManager.errorsProperty().addListener(errorListener);
            CSSParser.getInstance().parseInlineStyle(new StyleableStub(value));
            StyleManager.errorsProperty().removeListener(errorListener);

            return value;
        }

        public boolean hasParsingError() {
            return parsingError;
        }

        @Override
        public void setValue(String style) {
            // remove last ';' if any
            if (style.endsWith(";")) { //NOI18N
                style = style.substring(0, style.length() - 1);
            }
            // split in property and value
            int dotIndex = style.indexOf(':');
            String propertyStr;
            String valueStr = ""; //NOI18N
            if (dotIndex != -1) {
                propertyStr = style.substring(0, dotIndex);
                valueStr = style.substring(dotIndex + 1);
            } else {
                propertyStr = style;
            }
            propertyTf.setText(propertyStr);
            valueTf.setText(valueStr);
            updateButtons();
            currentValue = getValue();
        }

        @Override
        public void reset() {
            propertyTf.setText(""); //NOI18N
            valueTf.setText(""); //NOI18N
            propertyTf.setPromptText(null);
            valueTf.setPromptText(null);
        }

        // Please findBugs
        @Override
        public void requestFocus() {
            super.requestFocus();
        }

        @Override
        public void setValueAsIndeterminate() {
            handleIndeterminate(propertyTf);
            handleIndeterminate(valueTf);
        }

        @Override
        public MenuItem getMoveUpMenuItem() {
            return moveUpMi;
        }

        @Override
        public MenuItem getMoveDownMenuItem() {
            return moveDownMi;
        }

        @Override
        public MenuItem getRemoveMenuItem() {
            return removeMi;
        }

        @Override
        public Button getPlusButton() {
            return plusBt;
        }

        @FXML
        void add(ActionEvent event) {
            StyleItem styleItem = getNewStyleItem();
            editor.add(this, styleItem);
            styleItem.requestFocus();
        }

        @FXML
        void remove(ActionEvent event) {
            editor.remove(this);
        }

        @FXML
        void up(ActionEvent event) {
            editor.up(this);
        }

        @FXML
        void down(ActionEvent event) {
            editor.down(this);
        }

        @FXML
        void plusBtTyped(KeyEvent event) {
            if (event.getCode() == KeyCode.ENTER) {
                StyleItem styleItem = getNewStyleItem();
                editor.add(this, styleItem);
                styleItem.requestFocus();
            }
        }

        private void updateButtons() {
            if (propertyTf.getText().isEmpty() && valueTf.getText().isEmpty()) {
                // if no field has content, disable plus
                plusBt.setDisable(true);
                removeMi.setDisable(false);
            } else if (!propertyTf.getText().isEmpty() && !valueTf.getText().isEmpty()) {
                // if both fields have content, enable plus and minus
                plusBt.setDisable(false);
                removeMi.setDisable(false);
            } else if (!propertyTf.getText().isEmpty() || !valueTf.getText().isEmpty()) {
                // if either field has content, disable plus and enable minus
                plusBt.setDisable(true);
                removeMi.setDisable(false);
            }
        }

        @SuppressWarnings("unused")
        protected void disablePlusButton(boolean disable) {
            plusBt.setDisable(disable);
        }

        @SuppressWarnings("unused")
        protected void disableRemove(boolean disable) {
            removeMi.setDisable(disable);
        }
    }

    // Stub for style parsing
    private static class StyleableStub implements Styleable {

        private final String style;

        private StyleableStub(String style) {
            this.style = style;
        }

        @Override
        public String getTypeSelector() {
            return null;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public ObservableList<String> getStyleClass() {
            return FXCollections.emptyObservableList();
        }

        @Override
        public String getStyle() {
            return style;
        }

        @Override
        public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
            return Collections.emptyList();
        }

        @Override
        public Styleable getStyleableParent() {
            return null;
        }

        @Override
        public ObservableSet<PseudoClass> getPseudoClassStates() {
            return FXCollections.emptyObservableSet();
        }
    }

}

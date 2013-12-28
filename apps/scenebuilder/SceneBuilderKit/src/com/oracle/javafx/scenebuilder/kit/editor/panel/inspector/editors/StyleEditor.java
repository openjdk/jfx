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

import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

    @SuppressWarnings("LeakingThisInConstructor")
    public StyleEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        this.selectedClasses = selectedClasses;
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
            value += styleItem.getValue() + " "; //NOI18N
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

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
        this.selectedClasses = selectedClasses;
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
    public class StyleItem extends AutoSuggestEditor implements EditorItem {

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
            EventHandler<ActionEvent> onActionListener = new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
//                    System.out.println("StyleItem : onActionListener");
                    if (getValue().equals(currentValue)) {
                        // no change
                        return;
                    }
                    if (!propertyTf.getText().isEmpty() && !valueTf.getText().isEmpty()) {
//                        System.out.println("StyleEditorItem : COMMIT");
                        editor.commit(StyleItem.this);
                        assert event.getSource() instanceof TextField;
                        ((TextField) event.getSource()).selectAll();
                    }
                    updateButtons();
                    currentValue = getValue();
                }
            };

            ChangeListener<String> textPropertyChange = new ChangeListener<String>() {

                @Override
                public void changed(ObservableValue<? extends String> ov, String prevText, String newText) {
                    if (prevText.isEmpty() || newText.isEmpty()) {
                        // Text changed FROM empty value, or TO empty value: buttons status change
                        updateButtons();
                    }
                }
            };

            propertyTf.textProperty().addListener(textPropertyChange);
            valueTf.textProperty().addListener(textPropertyChange);
            updateButtons();

            // Do not add a generic focus listener on each of the text fields,
            // but implement a specific one.
            setTextEditorBehavior(propertyTf, onActionListener, false);
            setTextEditorBehavior(valueTf, onActionListener, false);
            ChangeListener<Boolean> focusListener = new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (!newValue) {
                        // focus lost: commit
                        onActionListener.handle(new ActionEvent(propertyTf, null));
                    }
                }
            };
            propertyTf.focusedProperty().addListener(focusListener);
            valueTf.focusedProperty().addListener(focusListener);

            // Initialize menu items text
            removeMi.setText(I18N.getString("inspector.list.remove"));
            moveUpMi.setText(I18N.getString("inspector.list.moveup"));
            moveDownMi.setText(I18N.getString("inspector.list.movedown"));
        }

        @Override
        public final Node getNode() {
            return root;
        }

        @Override
        public String getValue() {
            String value;
            if (propertyTf.getText().isEmpty() && valueTf.getText().isEmpty()) {
                return "";
            } else {
                value = propertyTf.getText().trim() + ": " + valueTf.getText().trim() + ";"; //NOI18N
            }
            return value;
        }

        @Override
        public void setValue(String style) {
            // remove last ';' if any
            if (style.endsWith(";")) { //NOI18N
                style = style.substring(0, style.length() - 1);
            }
            // split in property and value
            String[] styleItem = style.split(":");
            propertyTf.setText(styleItem[0].trim());
            // If invalid style, we may have more than 2 styleItem
            StringBuilder valueStr = new StringBuilder();
            for (int ii = 1; ii < styleItem.length; ii++) {
                valueStr.append(styleItem[ii]);
            }
            valueTf.setText(valueStr.toString().trim());
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
            editor.add(this, getNewStyleItem());
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
                editor.add(this, getNewStyleItem());
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

        protected void disablePlusButton(boolean disable) {
            plusBt.setDisable(disable);
        }

        protected void disableRemove(boolean disable) {
            removeMi.setDisable(disable);
        }
    }
}

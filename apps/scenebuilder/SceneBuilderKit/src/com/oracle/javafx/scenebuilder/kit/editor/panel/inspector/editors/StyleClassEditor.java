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
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform.Theme;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import com.oracle.javafx.scenebuilder.kit.util.URLUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

/**
 * Editor of the 'styleClass' property. It may contain several css classes, that
 * have their dedicated class (StyleClassItem).
 *
 *
 */
public class StyleClassEditor extends InlineListEditor {

    private Set<FXOMInstance> selectedInstances;
    private Map<String, String> cssClassesMap;
    private List<String> themeClasses;
    private EditorController editorController;

    @SuppressWarnings("LeakingThisInConstructor")
    public StyleClassEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses,
            Set<FXOMInstance> selectedInstances, EditorController editorController) {
        super(propMeta, selectedClasses);
        this.selectedInstances = selectedInstances;
        this.editorController = editorController;
        setLayoutFormat(PropertyEditor.LayoutFormat.DOUBLE_LINE);
        themeClasses = CssInternal.getThemeStyleClasses(editorController.getTheme());
        addItem(getNewStyleClassItem());

        // On Theme change, update the themeClasses
        editorController.themeProperty().addListener((ChangeListener<Theme>) (ov, t, t1) -> themeClasses = CssInternal.getThemeStyleClasses(StyleClassEditor.this.editorController.getTheme()));
    }

    private StyleClassItem getNewStyleClassItem() {
        if (cssClassesMap == null) {
            cssClassesMap = CssInternal.getStyleClassesMap(editorController, selectedInstances);
            // We don't want the theme classes to be suggested: remove them from the list
            for (String themeClass : themeClasses) {
                cssClassesMap.remove(themeClass);
            }
        }
        return new StyleClassItem(this, cssClassesMap);
    }

    @Override
    public Object getValue() {
        List<String> value = FXCollections.observableArrayList();
        // Group all the item values in a list
        for (EditorItem styleItem : getEditorItems()) {
            String itemValue = styleItem.getValue();
            if (itemValue.isEmpty()) {
                continue;
            }
            value.add(itemValue);
        }
        if (value.isEmpty()) {
            // no style class
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
        for (String item : (List<String>) value) {
            item = item.trim();
            if (item.isEmpty()) {
                continue;
            }
            
            // We don't want to show the default theme classes
            // (e.g. combo-box, combo-box-base for ComboBox)
            Object defaultValue = getPropertyMeta().getDefaultValueObject();
            assert defaultValue instanceof List;
            List<String> defaultClasses = (List<String>) defaultValue;
            if (defaultClasses.contains(item)) {
                continue;
            }
            
            EditorItem editorItem;
            if (itemsIter.hasNext()) {
                // re-use the current items first
                editorItem = itemsIter.next();
            } else {
                // additional items required
                editorItem = addItem(getNewStyleClassItem());
            }
            editorItem.setValue(item);
        }
        // Empty the remaining items, if needed
        while (itemsIter.hasNext()) {
            EditorItem editorItem = itemsIter.next();
            removeItem(editorItem);
        }
    }

    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses,
            Set<FXOMInstance> selectedInstances, EditorController editorController) {
        super.reset(propMeta, selectedClasses);
        this.selectedInstances = selectedInstances;
        this.editorController = editorController;
        cssClassesMap = null;
        // add an empty item
        addItem(getNewStyleClassItem());
    }

    @Override
    public void requestFocus() {
        EditorItem firstItem = getEditorItems().get(0);
        assert firstItem instanceof StyleClassItem;
        ((StyleClassItem) firstItem).requestFocus();
    }

    /**
     ***************************************************************************
     *
     * StyleClass item : styleClass text fields, and +/action buttons.
     *
     ***************************************************************************
     */
    private class StyleClassItem extends AutoSuggestEditor implements EditorItem {

        @FXML
        private Button plusBt;
        @FXML
        private MenuButton actionMb;
        @FXML
        private MenuItem removeMi;
        @FXML
        private MenuItem moveUpMi;
        @FXML
        private MenuItem moveDownMi;
        @FXML
        private MenuItem openMi;
        @FXML
        private MenuItem revealMi;
        @FXML
        private StackPane styleClassSp;

        private final Parent root;
        private TextField styleClassTf;
        private String currentValue;
        private final Map<String, String> cssClassesMap;
        private final EditorItemDelegate editor;

        @SuppressWarnings("LeakingThisInConstructor")
        public StyleClassItem(EditorItemDelegate editor, Map<String, String> cssClassesMap) {
//            System.out.println("New StyleClassItem.");
            // It is an AutoSuggestEditor without MenuButton
            super("", "", new ArrayList<>(cssClassesMap.keySet()), false); //NOI18N
            this.editor = editor;
            this.cssClassesMap = cssClassesMap;
            root = EditorUtils.loadFxml("StyleClassEditorItem.fxml", this);//NOI18N

            initialize();
        }

        // Method to please FindBugs
        private void initialize() {
            // Add the AutoSuggest text field in the scene graph
            styleClassSp.getChildren().add(super.getRoot());

            styleClassTf = super.getTextField();
            EventHandler<ActionEvent> onActionListener = event -> {
//                    System.out.println("StyleClassItem : onActionListener");
                if (getValue().equals(currentValue)) {
                    // no change
                    return;
                }
                if (styleClassTf.getText().isEmpty()) {
                    remove(null);
                }
//                        System.out.println("StyleEditorItem : COMMIT");
                editor.commit(StyleClassItem.this);
                if ((event != null) && event.getSource() instanceof TextField) {
                    ((TextField) event.getSource()).selectAll();
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
            styleClassTf.textProperty().addListener(textPropertyChange);
            updateButtons();

            setTextEditorBehavior(styleClassTf, onActionListener, false);
            ChangeListener<Boolean> focusListener = (observable, oldValue, newValue) -> {
                if (!newValue) {
                    // focus lost: commit
                    editor.editing(false, onActionListener);
                } else {
                    // got focus
                    editor.editing(true, onActionListener);
                }
            };
            styleClassTf.focusedProperty().addListener(focusListener);

            // Initialize menu items text
            removeMi.setText(I18N.getString("inspector.list.remove"));
            moveUpMi.setText(I18N.getString("inspector.list.moveup"));
            moveDownMi.setText(I18N.getString("inspector.list.movedown"));

            // Add suggested classes in the already existing action menu button, 
            // since we do not use the AutoSuggestEditor menu button for this editor.
            if (!cssClassesMap.isEmpty()) {
                actionMb.getItems().add(new SeparatorMenuItem());
            }
            for (String className : cssClassesMap.keySet()) {
                // css classes menu items
                MenuItem menuItem = new MenuItem(className);
                menuItem.setMnemonicParsing(false);
                menuItem.setOnAction(t -> {
                    styleClassTf.setText(className);
                    StyleClassItem.this.getCommitListener().handle(null);
                });
                actionMb.getItems().add(menuItem);
            }

        }

        @Override
        public final Node getNode() {
            return root;
        }

        @Override
        public String getValue() {
            return EditorUtils.getPlainString(styleClassTf.getText()).trim();
        }

        @Override
        public void setValue(String styleClass) {
            styleClassTf.setText(styleClass.trim());
            updateButtons();
            currentValue = getValue();
        }

        @Override
        public void reset() {
            styleClassTf.setText(""); //NOI18N
        }

        // Please findBugs
        @Override
        public void requestFocus() {
            super.requestFocus();
        }

        @Override
        public void setValueAsIndeterminate() {
            handleIndeterminate(styleClassTf);
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
            StyleClassEditor.StyleClassItem styleClassItem = getNewStyleClassItem();
            editor.add(this, styleClassItem);
            styleClassItem.requestFocus();

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
                editor.add(this, getNewStyleClassItem());
            }
        }

        @FXML
        void open(ActionEvent event) {
            String urlStr = cssClassesMap.get(getValue());
            if (urlStr == null) {
                return;
            }
            try {
                EditorPlatform.open(urlStr);
            } catch (IOException ex) {
                editorController.getMessageLog().logWarningMessage(
                        "inspector.stylesheet.cannotopen", urlStr); //NOI18N
            }
        }

        @FXML
        void reveal(ActionEvent event) {
            String urlStr = cssClassesMap.get(getValue());
            if (urlStr == null) {
                return;
            }
            try {
                File file = URLUtils.getFile(urlStr);
                if (file == null) { // urlStr is not a file URL
                    return;
                }
                EditorPlatform.revealInFileBrowser(file);
            } catch (URISyntaxException | IOException ex) {
                editorController.getMessageLog().logWarningMessage(
                        "inspector.stylesheet.cannotreveal", urlStr); //NOI18N
            }
        }

        private void updateButtons() {
            if (styleClassTf.getText().isEmpty()) {
                // if no content, disable plus
                plusBt.setDisable(true);
                removeMi.setDisable(false);
            } else {
                // enable plus and minus
                plusBt.setDisable(false);
                removeMi.setDisable(false);
            }
            // set text of open / reveal menu items
            String stylesheetUrl = cssClassesMap.get(getValue());
            if (stylesheetUrl == null) {
                // className is unknown: open / reveal should not be visible
                openMi.setVisible(false);
                revealMi.setVisible(false);
            } else {
                openMi.setVisible(true);
                revealMi.setVisible(true);
                String stylesheet = EditorUtils.getSimpleFileName(stylesheetUrl);
                openMi.setText(I18N.getString("inspector.list.open", stylesheet));
                if (EditorPlatform.IS_MAC) {
                    revealMi.setText(I18N.getString("inspector.list.reveal.finder", stylesheet));
                } else {
                    revealMi.setText(I18N.getString("inspector.list.reveal.explorer", stylesheet));
                }
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
}

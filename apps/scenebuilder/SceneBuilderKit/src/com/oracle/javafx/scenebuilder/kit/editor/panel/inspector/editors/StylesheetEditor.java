/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
import javafx.stage.FileChooser;

/**
 * Editor of the 'stylesheet' property. It may contain several stylesheets (css)
 * files, that are handled by an inline class (StylesheetItem).
 *
 *
 */
public class StylesheetEditor extends InlineListEditor {

    private final StackPane root = new StackPane();
    private final Parent rootInitialBt;

    @SuppressWarnings("LeakingThisInConstructor")
    public StylesheetEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        setLayoutFormat(PropertyEditor.LayoutFormat.DOUBLE_LINE);
        // Add initial button
        rootInitialBt = EditorUtils.loadFxml("StylesheetEditorInitialBt.fxml", this); //NOI18N
        root.getChildren().add(rootInitialBt);
        // Set the initial value to empty list (instead of null)
        valueProperty().setValue(FXCollections.observableArrayList());
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(root);
    }

    @Override
    public Object getValue() {
        List<String> value = FXCollections.observableArrayList();
        // Group all the item values in a list
        for (EditorItem stylesheetItem : getEditorItems()) {
            String itemValue = stylesheetItem.getValue();
            if (itemValue.isEmpty()) {
                continue;
            }
            value.add(itemValue);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValue(Object value) {
        if (value == null) {
            reset();
            return;
        }
        assert value instanceof List;
        if (((List) value).isEmpty()) {
            reset();
            return;
        }
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
            EditorItem editorItem;
            if (itemsIter.hasNext()) {
                // re-use the current items first
                editorItem = itemsIter.next();
            } else {
                // additional items required
                editorItem = addItem(new StylesheetItem(this, item));
            }
            editorItem.setValue(item);
        }
        // Empty the remaining items, if needed
        while (itemsIter.hasNext()) {
            EditorItem editorItem = itemsIter.next();
            removeItem(editorItem);
        }
        switchToItemList();
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses, true);
        switchToInitialButton();
    }

    @Override
    protected void reset() {
        super.reset(true);
        switchToInitialButton();
    }

    @Override
    public void requestFocus() {
        EditorItem firstItem = getEditorItems().get(0);
        assert firstItem instanceof StylesheetItem;
        ((StylesheetItem) firstItem).requestFocus();
    }

    @Override
    public void remove(EditorItem source) {
        super.remove(source, true);
        if (super.getEditorItems().isEmpty()) {
            // Switch to initial button
            switchToInitialButton();
        }
    }

    private void open(EditorItem source) {
        try {
            EditorPlatform.open(source.getValue());
        } catch (IOException ex) {
            System.err.println(I18N.getString("inspector.stylesheet.cannotopen", source.getValue() + " : " + ex)); // should go to message panel
        }
    }

    private void reveal(EditorItem source) {
        try {
            File file = new File((new URI(source.getValue())).toURL().getFile());
            EditorPlatform.revealInFileBrowser(file);
        } catch (IOException | URISyntaxException ex) {
            System.err.println(I18N.getString("inspector.stylesheet.cannotreveal", source.getValue() + " : " + ex)); // should go to message panel
        }
    }

    @FXML
    void chooseStylesheet(ActionEvent event) {

        String[] extensions = {"*.css"}; //NOI18N
        // !! Do we need a wrapper, as we had in SB 1.1, to allow tests to bypass the dialog ?
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18N.getString("inspector.select.css"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        I18N.getString("inspector.select.css"),
                        Arrays.asList(extensions)));
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if ((file == null)) {
            return;
        }
        String url;
        try {
            url = file.toURI().toURL().toExternalForm();
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Invalid URL", ex); //NOI18N
        }
        if (alreadyUsed(url)) {
            System.err.println(I18N.getString("inspector.stylesheet.alreadyexist", url)); // should go to message panel
            return;
        }

        switchToItemList();
        // Add editor item
        addItem(new StylesheetItem(this, url));

        userUpdateValueProperty(getValue());
    }

    @FXML
    void buttonTyped(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            chooseStylesheet(null);
        }
    }

    private void switchToItemList() {
        // Replace initial button by the item list (vbox)
        if (root.getChildren().contains(rootInitialBt)) {
            root.getChildren().remove(rootInitialBt);
            root.getChildren().add(super.getValueEditor());
        }
    }

    private void switchToInitialButton() {
        // Replace the item list (vbox) by initial button
        root.getChildren().clear();
        root.getChildren().add(rootInitialBt);
    }

    private boolean alreadyUsed(String url) {
        for (EditorItem item : super.getEditorItems()) {
            if (item.getValue().equals(url)) {
                return true;
            }
        }
        return false;
    }

    /**
     ***************************************************************************
     *
     * StyleClass item : styleClass text fields, and +/action buttons.
     *
     ***************************************************************************
     */
    public class StylesheetItem implements EditorItem {

        @FXML
        private Button plusBt;
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
        private TextField stylesheetTf;

        private final Parent root;
        private String currentValue;
        private final EditorItemDelegate editor;

        @SuppressWarnings("LeakingThisInConstructor")
        public StylesheetItem(EditorItemDelegate editor, String url) {
//            System.out.println("New StylesheetItem.");
            this.editor = editor;
            root = EditorUtils.loadFxml("StylesheetEditorItem.fxml", this);

            initialize(url);
        }

        // Method to please FindBugs
        private void initialize(String url) {
            stylesheetTf.setText(url);
            EventHandler<ActionEvent> onActionListener = new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
//                    System.out.println("StylesheetItem : onActionListener");
                    if (getValue().equals(currentValue)) {
                        // no change
                        return;
                    }
                    if (stylesheetTf.getText().isEmpty()) {
                        return;
                    }
//                        System.out.println("StyleEditorItem : COMMIT");
                    editor.commit(StylesheetItem.this);
                    assert event.getSource() instanceof TextField;
                    ((TextField) event.getSource()).selectAll();
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
            stylesheetTf.textProperty().addListener(textPropertyChange);
            updateButtons();

            setTextEditorBehavior(stylesheetTf, onActionListener);

            // Initialize menu items text
            removeMi.setText(I18N.getString("inspector.list.remove"));
            moveUpMi.setText(I18N.getString("inspector.list.moveup"));
            moveDownMi.setText(I18N.getString("inspector.list.movedown"));
            String fileName = EditorUtils.getFileName(url);
            openMi.setText(I18N.getString("inspector.list.open", fileName));
            if (EditorPlatform.IS_MAC) {
                revealMi.setText(I18N.getString("inspector.list.reveal.finder", fileName));
            } else {
                revealMi.setText(I18N.getString("inspector.list.reveal.explorer", fileName));
            }
        }

        @Override
        public final Node getNode() {
            return root;
        }

        @Override
        public String getValue() {
            String value;
            if (stylesheetTf.getText().isEmpty()) {
                return "";
            } else {
                value = stylesheetTf.getText().trim();
            }
            return value;
        }

        @Override
        public void setValue(String styleClass) {
            stylesheetTf.setText(styleClass.trim());
            updateButtons();
            currentValue = getValue();
        }

        @Override
        public void reset() {
            stylesheetTf.setText(""); //NOI18N
            stylesheetTf.setPromptText(null);
        }

        @Override
        public void setValueAsIndeterminate() {
            handleIndeterminate(stylesheetTf);
        }

        protected void requestFocus() {
            EditorUtils.doNextFrame(new Runnable() {

                @Override
                public void run() {
                    stylesheetTf.requestFocus();
                }
            });
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
        void chooseStylesheet(ActionEvent event) {
            ((StylesheetEditor) editor).chooseStylesheet(event);
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
        void open(ActionEvent event) {
            ((StylesheetEditor) editor).open(this);
        }

        @FXML
        void reveal(ActionEvent event) {
            ((StylesheetEditor) editor).reveal(this);
        }

        @FXML
        void plusBtTyped(KeyEvent event) {
            if (event.getCode() == KeyCode.ENTER) {
                chooseStylesheet(null);
            }
        }

        private void updateButtons() {
            if (stylesheetTf.getText().isEmpty()) {
                // if no content, disable plus
                plusBt.setDisable(true);
                removeMi.setDisable(false);
            } else {
                // enable plus and minus
                plusBt.setDisable(false);
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

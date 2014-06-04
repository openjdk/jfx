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

import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue.Type;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import com.oracle.javafx.scenebuilder.kit.util.URLUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
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

    private final MenuItem documentRelativeMenuItem
            = new MenuItem(I18N.getString("inspector.resource.documentrelative"));
    private final MenuItem classPathRelativeMenuItem
            = new MenuItem(I18N.getString("inspector.resource.classpathrelative"));
    private final MenuItem absoluteMenuItem
            = new MenuItem(I18N.getString("inspector.resource.absolute"));

    private Type type;
    private URL fxmlFileLocation;

    @SuppressWarnings("LeakingThisInConstructor")
    public StylesheetEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, URL fxmlFileLocation) {
        super(propMeta, selectedClasses);
        this.fxmlFileLocation = fxmlFileLocation;
        setLayoutFormat(PropertyEditor.LayoutFormat.DOUBLE_LINE);
        // Add initial button
        rootInitialBt = EditorUtils.loadFxml("StylesheetEditorInitialBt.fxml", this); //NOI18N
        root.getChildren().add(rootInitialBt);
        // Set the initial value to empty list (instead of null)
        valueProperty().setValue(FXCollections.observableArrayList());

        documentRelativeMenuItem.setOnAction(e -> switchType(Type.DOCUMENT_RELATIVE_PATH));
        classPathRelativeMenuItem.setOnAction(e -> switchType(Type.CLASSLOADER_RELATIVE_PATH));
        absoluteMenuItem.setOnAction(e -> switchType(Type.PLAIN_STRING));
        getMenu().getItems().addAll(documentRelativeMenuItem, classPathRelativeMenuItem, absoluteMenuItem);
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
        if (value.isEmpty()) {
            // no stylesheet
            return super.getPropertyMeta().getDefaultValueObject();
        } else {
            type = getType(value);
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
        if (((List<?>) value).isEmpty()) {
            reset();
            return;
        }
        // Warning : value is the editing list.
        // We do not want to set the valueProperty() to editing list
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        type = getType((List<String>) value);
        updateMenuItems();
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

    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, URL fxmlFileLocation) {
        super.reset(propMeta, selectedClasses, true);
        this.fxmlFileLocation = fxmlFileLocation;
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
        String urlStr = getUrl(source);
        if (urlStr == null) {
            return;
        }
        try {
            EditorPlatform.open(urlStr);
        } catch (IOException ex) {
            System.err.println(I18N.getString("inspector.stylesheet.cannotopen", urlStr + " : " + ex)); // should go to message panel
        }
    }

    private void reveal(EditorItem source) {
        String urlStr = getUrl(source);
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
            System.err.println(I18N.getString("inspector.stylesheet.cannotreveal", urlStr + " : " + ex)); // should go to message panel
        }
    }

    private String getUrl(EditorItem source) {
        URL url = EditorUtils.getUrl(source.getValue(), fxmlFileLocation);
        if (url == null) {
            return null;
        }
        String urlStr = url.toExternalForm();
        return urlStr;
    }

    @FXML
    void chooseStylesheet(ActionEvent event) {

        String[] extensions = {"*.css"}; //NOI18N
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18N.getString("inspector.select.css.title"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        I18N.getString("inspector.select.css.filter"),
                        Arrays.asList(extensions)));
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if ((file == null)) {
            return;
        }
        URL url;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Invalid URL", ex); //NOI18N
        }

        switchToItemList();
        // Add editor item
        String urlStr;
        if (fxmlFileLocation != null) {
            // If the document exists, make the type as document relative by default.
            urlStr = PrefixedValue.makePrefixedValue(url, fxmlFileLocation).toString();
            switchType(Type.DOCUMENT_RELATIVE_PATH);
        } else {
            urlStr = url.toExternalForm();
            switchType(Type.PLAIN_STRING);
        }
        if (alreadyUsed(url.toExternalForm())) {
            System.err.println(I18N.getString("inspector.stylesheet.alreadyexist", url)); // should go to message panel
            return;
        }
        addItem(new StylesheetItem(this, urlStr));

        // Workaround for RT-34863: Reload of an updated css file has no effect.
        // This reset the whole CSS from top. Would need to be moved on the FXOM side.
        Deprecation.reapplyCSS(root.getScene());

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

    private void switchType(Type type) {
        this.type = type;
        updateMenuItems();
        for (EditorItem editorItem : getEditorItems()) {
            assert editorItem instanceof StylesheetItem;
            StylesheetItem stylesheetItem = (StylesheetItem) editorItem;
            URL url = EditorUtils.getUrl(stylesheetItem.getValue(), fxmlFileLocation);
            String value = null;
            if ((url == null) || (type == Type.CLASSLOADER_RELATIVE_PATH)) {
                // In this case we empty the text field (i.e. suffix) content
                value = new PrefixedValue(type, "").toString(); //NOI18N
            } else if (type == Type.PLAIN_STRING) {
                value = url.toExternalForm();
            } else if (type == Type.DOCUMENT_RELATIVE_PATH) {
                value = PrefixedValue.makePrefixedValue(url, fxmlFileLocation).toString();
            }
            stylesheetItem.setValue(value);
            commit(stylesheetItem);
        }
    }

    private Type getType(List<String> styleSheets) {
        Type commonType = null;
        for (String styleSheet : styleSheets) {
            if (commonType == null) {
                commonType = getType(styleSheet);
            } else {
                if (commonType != getType(styleSheet)) {
                    // mix of different types: set all to document relative
                    commonType = Type.DOCUMENT_RELATIVE_PATH;
                    break;
                }
            }
        }
        return commonType;
    }

    private static Type getType(String styleSheet) {
        return (new PrefixedValue(styleSheet)).getType();
    }

    private void updateMenuItems() {
        documentRelativeMenuItem.setDisable(false);
        classPathRelativeMenuItem.setDisable(false);
        absoluteMenuItem.setDisable(false);
        if (fxmlFileLocation == null) {
            documentRelativeMenuItem.setDisable(true);
        }
        if (type == Type.DOCUMENT_RELATIVE_PATH) {
            documentRelativeMenuItem.setDisable(true);
        } else if (type == Type.CLASSLOADER_RELATIVE_PATH) {
            classPathRelativeMenuItem.setDisable(true);
        } else if (type == Type.PLAIN_STRING) {
            absoluteMenuItem.setDisable(true);
        }
    }

    /**
     ***************************************************************************
     *
     * StyleClass item : styleClass text fields, and +/action buttons.
     *
     ***************************************************************************
     */
    private class StylesheetItem implements EditorItem {

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
        private Label prefixLb;
        @FXML
        private TextField stylesheetTf;

        private final Pane root;
        private String currentValue;
        private final EditorItemDelegate editor;
        private Type itemType = Type.PLAIN_STRING;

        @SuppressWarnings("LeakingThisInConstructor")
        public StylesheetItem(EditorItemDelegate editor, String url) {
//            System.out.println("New StylesheetItem.");
            this.editor = editor;
            Parent parentRoot = EditorUtils.loadFxml("StylesheetEditorItem.fxml", this);
            assert parentRoot instanceof Pane;
            root = (Pane) parentRoot;

            initialize(url);
        }

        // Method to please FindBugs
        private void initialize(String url) {
            setValue(url);
            EventHandler<ActionEvent> onActionListener = event -> {
//                    System.out.println("StylesheetItem : onActionListener");
                if (getValue().equals(currentValue)) {
                    // no change
                    return;
                }
                if (stylesheetTf.getText().isEmpty()) {
                    remove(null);
                }
//                        System.out.println("StyleEditorItem : COMMIT");
                editor.commit(StylesheetItem.this);
                if (event != null && event.getSource() instanceof TextField) {
                    ((TextField) event.getSource()).selectAll();
                }
                updateButtons();
                updateOpenRevealMenuItems();
                currentValue = getValue();
            };

            ChangeListener<String> textPropertyChange = (ov, prevText, newText) -> {
                if (prevText.isEmpty() || newText.isEmpty()) {
                    // Text changed FROM empty value, or TO empty value: buttons status change
                    updateButtons();
                    updateOpenRevealMenuItems();
                }
            };
            stylesheetTf.textProperty().addListener(textPropertyChange);
            updateButtons();

            setTextEditorBehavior(stylesheetTf, onActionListener);

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
            String suffix;
            if (stylesheetTf.getText().isEmpty()) {
                return ""; //NOI18N
            } else {
                suffix = stylesheetTf.getText().trim();
            }
            return (new PrefixedValue(itemType, suffix)).toString();
        }

        @Override
        public void setValue(String styleSheet) {
            PrefixedValue prefixedValue = new PrefixedValue(styleSheet);
            itemType = prefixedValue.getType();
            handlePrefix(itemType);
            if (prefixedValue.getSuffix() != null) {
                stylesheetTf.setText(prefixedValue.getSuffix().trim());
            } else {
                // may happen if wrong style sheet
                stylesheetTf.setText("");//NOI18N
            }
            updateButtons();
            updateOpenRevealMenuItems();
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
            EditorUtils.doNextFrame(() -> stylesheetTf.requestFocus());
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

        private void updateOpenRevealMenuItems() {
            // Get the file name part of the suffix
            String suffix = new PrefixedValue(getValue()).getSuffix();
            String fileName = null;
            if (!suffix.isEmpty()) {
                fileName = EditorUtils.getSimpleFileName(suffix);
            }
            if (fileName != null) {
                openMi.setVisible(true);
                revealMi.setVisible(true);
                openMi.setText(I18N.getString("inspector.list.open", fileName));
                if (EditorPlatform.IS_MAC) {
                    revealMi.setText(I18N.getString("inspector.list.reveal.finder", fileName));
                } else {
                    revealMi.setText(I18N.getString("inspector.list.reveal.explorer", fileName));
                }
            } else {
                openMi.setVisible(false);
                revealMi.setVisible(false);
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

        @SuppressWarnings("unused")
        protected void disablePlusButton(boolean disable) {
            plusBt.setDisable(disable);
        }

        @SuppressWarnings("unused")
        protected void disableRemove(boolean disable) {
            removeMi.setDisable(disable);
        }

        protected void handlePrefix(Type type) {
            this.itemType = type;
            if (type == Type.DOCUMENT_RELATIVE_PATH) {
                setPrefix(FXMLLoader.RELATIVE_PATH_PREFIX);
            } else if (type == Type.CLASSLOADER_RELATIVE_PATH) {
                setPrefix(FXMLLoader.RELATIVE_PATH_PREFIX + "/");//NOI18N
            } else {
                // absolute
                removeLabel();
            }
        }

        private void setPrefix(String str) {
            if (!prefixLb.isVisible()) {
                prefixLb.setVisible(true);
                prefixLb.setManaged(true);
            }
            prefixLb.setText(str);
        }

        private void removeLabel() {
            prefixLb.setVisible(false);
            prefixLb.setManaged(false);
        }
    }

}

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
import java.util.Set;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * String editor with I18n + multi-line handling.
 *
 *
 */
public class I18nStringEditor extends PropertyEditor {

    private static final String PERCENT_STR = "%"; //NOI18N
    private TextInputControl textNode = new TextField();
    private HBox i18nHBox = null;
    final EventHandler<ActionEvent> valueListener;
    private final MenuItem i18nOnMenuItem = new MenuItem(I18N.getString("inspector.i18n.on"));
    private final MenuItem i18nOffMenuItem = new MenuItem(I18N.getString("inspector.i18n.off"));
    private final MenuItem multilineMenuItem = new MenuItem(I18N.getString("inspector.i18n.multiline"));
    private final MenuItem singlelineMenuItem = new MenuItem(I18N.getString("inspector.i18n.singleline"));
    // Specific states
    private boolean i18nMode;
    private boolean multiLineMode;

    @SuppressWarnings("LeakingThisInConstructor")
    public I18nStringEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);

        valueListener = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                userUpdateValueProperty(getValue());
                textNode.selectAll();
            }
        };
        setTextEditorBehavior(this, textNode, valueListener);

        getMenu().getItems().add(i18nOnMenuItem);
        getMenu().getItems().add(multilineMenuItem);

        i18nOnMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                setValue(PERCENT_STR + I18N.getString("inspector.i18n.dummykey"));
                I18nStringEditor.this.getCommitListener().handle(null);
                EditorUtils.replaceMenuItem(i18nOnMenuItem, i18nOffMenuItem);
                multilineMenuItem.setDisable(true);
            }
        });
        i18nOffMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                setValue(""); //NOI18N
                I18nStringEditor.this.getCommitListener().handle(null);
                EditorUtils.replaceMenuItem(i18nOffMenuItem, i18nOnMenuItem);
                multilineMenuItem.setDisable(false);
            }
        });
        multilineMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                switchToTextArea();
            }
        });

        singlelineMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                switchToTextField();
            }
        });
    }

    @Override
    public Object getValue() {
        return textNode.getText();
    }

    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        if (value == null) {
            // We consider a null string property as an empty string
            // TBD: should not be defined here !
            value = ""; //NOI18N
        }
        assert value instanceof String;
        String val = (String) value;
        if (containsLineFeed(val) && !multiLineMode) {
            switchToTextArea();
        } else if (multiLineMode && i18nMode) {
            switchToTextField();
        }
        if (val.startsWith(PERCENT_STR) && !i18nMode) {
            i18nMode = true;
            val = val.substring(1);
            wrapInHBox();
        } else if (!val.startsWith(PERCENT_STR) && i18nMode) {
            i18nMode = false;
            unwrapHBox();
        }
        textNode.setText(val);
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
        textNode.setPromptText(null);
    }

    @Override
    public Node getValueEditor() {
        Node valueEditor;
        if (i18nMode) {
            valueEditor = i18nHBox;
        } else {
            valueEditor = textNode;
        }

        return super.handleGenericModes(valueEditor);
    }

    @Override
    protected void valueIsIndeterminate() {
        handleIndeterminate(textNode);
    }

    protected void switchToTextArea() {
        multiLineMode = true;
        // Move the node from TextField to TextArea
        TextArea textArea = new TextArea(textNode.getText());
        setTextEditorBehavior(this, textArea, valueListener);
        textArea.setPrefHeight(50);
        setLayoutFormat(LayoutFormat.SIMPLE_LINE_TOP);
        if (textNode.getParent() != null) {
            // textNode is alrady in scene graph
            EditorUtils.replaceNode(textNode, textArea, getLayoutFormat());
        }
        textNode = textArea;

        getMenu().getItems().remove(multilineMenuItem);
        getMenu().getItems().add(singlelineMenuItem);
        i18nOnMenuItem.setDisable(true);
    }

    protected void switchToTextField() {
        multiLineMode = false;
        // Move the node from TextArea to TextField.
        // The current text is compacted to a single line.
        String val = textNode.getText().replace("\n", "");//NOI18N
        TextField textField = new TextField(val);
        setTextEditorBehavior(this, textField, valueListener);
        setLayoutFormat(LayoutFormat.SIMPLE_LINE_CENTERED);
        if (textNode.getParent() != null) {
            // textNode is alrady in scene graph
            EditorUtils.replaceNode(textNode, textField, getLayoutFormat());
        }
        textNode = textField;

        getMenu().getItems().remove(singlelineMenuItem);
        getMenu().getItems().add(multilineMenuItem);
        i18nOnMenuItem.setDisable(false);
    }

    private void wrapInHBox() {
        i18nHBox = new HBox(5);
        i18nHBox.setAlignment(Pos.CENTER);
        EditorUtils.replaceNode(textNode, i18nHBox, null);
        Label percentLabel = new Label(PERCENT_STR);
        percentLabel.setMinWidth(10);
        i18nHBox.getChildren().addAll(percentLabel, textNode);
        HBox.setHgrow(textNode, Priority.ALWAYS);
    }

    private void unwrapHBox() {
        i18nHBox.getChildren().remove(textNode);
        EditorUtils.replaceNode(i18nHBox, textNode, null);
    }

    private static boolean containsLineFeed(String str) {
        return str.contains("\n"); //NOI18N
    }

    @Override
    public void requestFocus() {
        EditorUtils.doNextFrame(new Runnable() {

            @Override
            public void run() {
                textNode.requestFocus();
            }
        });
    }
}

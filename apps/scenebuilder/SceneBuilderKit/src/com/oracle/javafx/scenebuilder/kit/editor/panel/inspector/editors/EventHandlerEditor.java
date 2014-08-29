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
import com.oracle.javafx.scenebuilder.kit.util.JavaLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

/**
 * Event handler editor. An event handler in FXML can be of 2 types: - script
 * event handler - controller method event handler.
 *
 *
 */
public class EventHandlerEditor extends AutoSuggestEditor {

    private static final String HASH_STR = "#"; //NOI18N
    private final MenuItem controllerMethodMenuItem = new MenuItem(I18N.getString("inspector.event.menu.methodmode"));
    private final MenuItem scriptMenuItem = new MenuItem(I18N.getString("inspector.event.menu.scriptmode"));
    private boolean methodNameMode;
    private StackPane root = new StackPane();
    private HBox hbox = null;
    private List<String> suggestedMethods;

    public EventHandlerEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, List<String> suggestedMethods) {
        super(propMeta, selectedClasses, suggestedMethods); //NOI18N
        initialize(suggestedMethods);
    }

    private void initialize(List<String> suggestedMethods) {
        this.suggestedMethods = suggestedMethods;

        // text field events handling
        EventHandler<ActionEvent> onActionListener = event -> {
            String tfValue = getTextField().getText();
            if (tfValue == null || tfValue.isEmpty()) {
                userUpdateValueProperty(null);
                return;
            }
            if (methodNameMode) {
                // method name should be a java identifier
                if (!JavaLanguage.isIdentifier(tfValue)) {
                    System.err.println(I18N.getString("inspector.event.invalid.method", tfValue)); // Will go in message panel
                    handleInvalidValue(tfValue);
                    return;
                }
            }
            Object value = getValue();
            assert value instanceof String;
            userUpdateValueProperty((String) value);
            getTextField().selectAll();
        };
        setTextEditorBehavior(this, getTextField(), onActionListener);

        scriptMenuItem.setOnAction(e -> {
            getTextField().setText(null);
            userUpdateValueProperty(null);
            switchToScriptMode();
        });

        controllerMethodMenuItem.setOnAction(e -> {
            getTextField().setText(null);
            userUpdateValueProperty(null);
            switchToMethodNameMode();
        });
        getMenu().getItems().add(controllerMethodMenuItem);

        // methodeName mode by default
        switchToMethodNameMode();
    }
    
    @Override
    public Object getValue() {
        String valueTf = getTextField().getText();
        if (valueTf == null || valueTf.isEmpty()) {
            return null; // default value
        }
        String value;
        if (methodNameMode) {
            value = HASH_STR + getTextField().getText();
        } else {
            value = getTextField().getText();
        }
//        System.out.println("EventHandlerEditor : getValue() returns: '" + value + "'");
        return value;
    }

    @Override
    public void setValue(Object value) {
//        System.out.println("EventHandlerEditor : setValue to '" + value + "'");
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        String valueStr;
        if (value == null) {
            if (!methodNameMode) {
                switchToMethodNameMode();
            }
            valueStr = null;
        } else {
            assert value instanceof String;
            valueStr = (String) value;
            if (valueStr.startsWith(HASH_STR)) {
                if (!methodNameMode) {
                    switchToMethodNameMode();
                }
                valueStr = valueStr.substring(1);
            } else if (!valueStr.startsWith(HASH_STR) && methodNameMode) {
                switchToScriptMode();
            }
        }
        getTextField().setText(valueStr);
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(root);
    }

    @Override
    public void reset(String name, String defaultValue, List<String> suggestedList) {
        super.reset(name, defaultValue, suggestedList);
        switchToMethodNameMode();
    }

    private void wrapInHBox() {
        hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);
        Label hashLabel = new Label(HASH_STR);
        hashLabel.getStyleClass().add("symbol-prefix");
        hbox.getChildren().addAll(hashLabel, getRoot());
        HBox.setHgrow(hashLabel, Priority.NEVER);
        root.getChildren().clear();
        root.getChildren().add(hbox);
    }

    private void unwrapHBox() {
        root.getChildren().clear();
        root.getChildren().add(getRoot());
    }

    private void switchToMethodNameMode() {
        methodNameMode = true;
        resetSuggestedList(suggestedMethods);
        replaceMenuItem(controllerMethodMenuItem, scriptMenuItem);
        wrapInHBox();
    }

    private void switchToScriptMode() {
        methodNameMode = false;
        resetSuggestedList(new ArrayList<>());
        replaceMenuItem(scriptMenuItem, controllerMethodMenuItem);
        unwrapHBox();
    }
}

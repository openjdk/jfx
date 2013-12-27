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
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

/**
 *
 * 
 */
public abstract class PropertiesEditor extends Editor {

    private final HBox nameNode;
    private MenuButton menu;
    private final MenuItem resetvalueMenuItem = new MenuItem(I18N.getString("inspector.editors.resetvalue"));
    private FadeTransition fadeTransition = null;
    private static final Image cogIcon = new Image(
            InspectorPanelController.class.getResource("images/cog.png").toExternalForm()); //NOI18N
    private final String name;

    public PropertiesEditor(String name) {
        // HBox for consistency with PropertyEditor, and potentially have an hyperlink
        this.name = name;
        nameNode = new HBox();
        nameNode.getChildren().add(new Label(name));
    }

    public HBox getNameNode() {
        return nameNode;
    }
    
    public String getPropertyNameText() {
        return name;
    }

    public abstract List<PropertyEditor> getPropertyEditors();

    @Override
    public final MenuButton getMenu() {
        if (menu == null) {
            menu = new MenuButton();
            menu.setGraphic(new ImageView(cogIcon));
            menu.getStyleClass().add("cog-button");
            menu.setOpacity(0);
            fadeTransition = new FadeTransition(Duration.millis(500), menu);
            EditorUtils.handleFading(fadeTransition, menu);
            EditorUtils.handleFading(fadeTransition, getValueEditor());

            menu.focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        // focused
                        EditorUtils.fadeTo(fadeTransition, 1);
                    } else {
                        // focus lost
                        EditorUtils.fadeTo(fadeTransition, 0);
                    }
                }
            });
            menu.getItems().add(resetvalueMenuItem);
            resetvalueMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    for (PropertyEditor propertyEditor : getPropertyEditors()) {
                        propertyEditor.setValue(propertyEditor.getPropertyMeta().getDefaultValueObject());
                    }
                }
            });
        }
        return menu;
    }

    @Override
    public void removeAllListeners() {
        for (PropertyEditor propertyEditor : getPropertyEditors()) {
            propertyEditor.removeAllListeners();
        }
    }

    protected void propertyChanged() {
        boolean allDefault = true;
        for (PropertyEditor propertyEditor : getPropertyEditors()) {
            Object value = propertyEditor.valueProperty().getValue();
            ValuePropertyMetadata propMeta = propertyEditor.getPropertyMeta();
            if (value == null) {
                if (!(propMeta.getDefaultValueObject() == null)) {
                    allDefault = false;
                    break;
                }
            } else if (!value.equals(propMeta.getDefaultValueObject())) {
                    allDefault = false;
                    break;
                }
            }
            if (allDefault) {
                resetvalueMenuItem.setDisable(true);
            } else {
                resetvalueMenuItem.setDisable(false);
            }
        }
    }

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
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors;

import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.EditorUtils;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.PropertyEditor;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;

import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Pane;

/**
 * Abstract class for all popup editors (font, paint, ...).
 *
 */
public abstract class PopupEditor extends PropertyEditor implements PopupEditorValidation {

    @FXML
    MenuButton popupMb;

    @FXML
    CustomMenuItem popupMenuItem;

    @FXML
    Pane editorHost;

    private Object value;
    private boolean initialized = false;

    public PopupEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        initializeEditor();
    }
    
    // Separate method to please FindBugs
    private void initializeEditor() {
        EditorUtils.loadPopupFxml("PopupEditor.fxml", this);
        // Lazy initialization of the editor,
        // the first time the popup is opened.
        popupMb.showingProperty().addListener((ChangeListener<Boolean>) (ov, previousVal, newVal) -> {
            if (newVal) {
                if (!initialized) {
                    initializePopup();
                    initialized = true;
                }
                setPopupContentValue(value);
            }
        });
    }

    private void initializePopup() {
        initializePopupContent();
        editorHost.getChildren().add(getPopupContentNode());
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(popupMb);
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
//        System.out.println(getPropertyNameText() + " - setValue() : " + value);
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        if (popupMb.isShowing()) {
            setPopupContentValue(value);
        }

        // Update the preview string
        popupMb.setText(getPreviewString(value));

        // Update the preview graphic
        // (Paint editor only for now)
        if (this instanceof PaintPopupEditor) {
            popupMb.setGraphic(((PaintPopupEditor) this).getPreviewGraphic(value));
        }

        commitValue(value);
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
//        System.out.println(getPropertyNameText() + " : resetPopupContent()");
        super.reset(propMeta, selectedClasses);

        popupMb.setText(null);
    }

    @Override
    protected void valueIsIndeterminate() {
        handleIndeterminate(popupMb);
    }

    @Override
    public void requestFocus() {
        EditorUtils.doNextFrame(() -> popupMb.requestFocus());
    }

    /*
     * PopupEditorValidation interface.
     * Methods to be used by concrete popup editors
     */
    @Override
    public void commitValue(Object value) {
        userUpdateValueProperty(value);
        popupMb.setText(getPreviewString(value));
        this.value = value;
    }

    @Override
    public void transientValue(Object value) {
        userUpdateTransientValueProperty(value);
    }

    @Override
    public void invalidValue(Object value) {
        // TBD
    }

    /*
     *
     * Methods to be implemented by concrete popup editors.
     *
     */
    // Initialize the popup editor content, including fxml loading
    public abstract void initializePopupContent();

    // Update the popup editor content from a value
    public abstract Node getPopupContentNode();

    // Update the popup editor content from a value
    public abstract void setPopupContentValue(Object value);

    // Return the representaton of the input value as a string
    // (to display it in the menu button text)
    public abstract String getPreviewString(Object value);

}

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
import static com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.PropertyEditor.handleIndeterminate;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Set;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;

/**
 * Image property editor (handle the url path).
 */
public class ImageEditor extends PropertyEditor {

    @FXML
    private TextField imagePathTf;

    private final Parent root;
    private Image image = null;

    @SuppressWarnings("LeakingThisInConstructor")
    public ImageEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        root = EditorUtils.loadFxml("ImageEditor.fxml", this); //NOI18N

        EventHandler<ActionEvent> valueListener = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    image = new Image(imagePathTf.getText());
                } catch (NullPointerException  | IllegalArgumentException ex) {
                    handleInvalidValue(imagePathTf.getText());
                    return;
                }
                userUpdateValueProperty(image);
            }
        };
        initialize(valueListener);
    }

    // Separate method to please FindBugs
    private void initialize(EventHandler<ActionEvent> valueListener) {
        setTextEditorBehavior(this, imagePathTf, valueListener);
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(root);
    }

    @Override
    public Object getValue() {
        return image;
    }

    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        if (value == null) {
            image = null;
        } else {
            assert value instanceof Image;
            image = (Image) value;
            imagePathTf.setText(Deprecation.getUrl(image));
        }
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
        imagePathTf.setPromptText(null);
    }

    @Override
    protected void valueIsIndeterminate() {
        handleIndeterminate(imagePathTf);
    }

    //
    // FXML methods
    //
    @FXML
    void chooseImage(ActionEvent event) {
        String[] extensions = {"*.jpg", "*.jpeg", "*.png", "*.gif"}; //NOI18N
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18N.getString("inspector.select.image"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        I18N.getString("inspector.select.image"),
                        Arrays.asList(extensions)));
        File file = fileChooser.showOpenDialog(imagePathTf.getScene().getWindow());
        if ((file == null)) {
            return;
        }
        String url;
        try {
            url = file.toURI().toURL().toExternalForm();
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Invalid URL", ex); //NOI18N
        }
        imagePathTf.setText(url);
        image = new Image(url);
        userUpdateValueProperty(getValue());
    }

    @Override
    public void requestFocus() {
        EditorUtils.doNextFrame(new Runnable() {

            @Override
            public void run() {
                imagePathTf.requestFocus();
            }
        });
    }
}

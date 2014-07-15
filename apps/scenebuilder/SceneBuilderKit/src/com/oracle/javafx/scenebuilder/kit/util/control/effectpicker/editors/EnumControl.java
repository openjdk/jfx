/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors;

import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.EffectPickerController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class EnumControl<T> extends GridPane {

    @FXML
    private Label editor_label;
    @FXML
    private ChoiceBox<T> editor_choicebox;

    private final ObjectProperty<T> value = new SimpleObjectProperty<>();
    private final EffectPickerController effectPickerController;

    public EnumControl(EffectPickerController effectPickerController,
            String label, T[] values, T initValue) {
        this.effectPickerController = effectPickerController;
        initialize(label, values, initValue);
    }
    
    public ObjectProperty<T> valueProperty() {
        return value;
    }

    public T getValue() {
        return value.get();
    }

    public void setValue(T v) {
        value.set(v);
    }

    private void initialize(String label, T[] values, T initValue) {

        final URL layoutURL = EnumControl.class.getResource("EnumControl.fxml"); //NOI18N
        try (InputStream is = layoutURL.openStream()) {
            final FXMLLoader loader = new FXMLLoader();
            loader.setController(this);
            loader.setRoot(this);
            loader.setLocation(layoutURL);
            final Parent p = (Parent) loader.load(is);
            assert p == this;
        } catch (IOException x) {
            throw new RuntimeException(x);
        }

        editor_label.setText(label);
        editor_choicebox.getItems().addAll(values);
        editor_choicebox.setValue(initValue);
        editor_choicebox.getSelectionModel().selectedItemProperty().addListener((ChangeListener<T>) (ov, t, t1) -> {
            // First update the model
            setValue(t1);
            // Then notify the controller a change occured
            effectPickerController.incrementRevision();
        });
        
        setValue(initValue);
        
        editor_choicebox.addEventHandler(ActionEvent.ACTION, (Event event) -> {
            event.consume();
        });
    }
}

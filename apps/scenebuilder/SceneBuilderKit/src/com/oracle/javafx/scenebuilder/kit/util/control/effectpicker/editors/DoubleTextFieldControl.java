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

import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.DoubleField;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.EditorUtils;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.EffectPickerController;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.Utils;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;

public class DoubleTextFieldControl extends GridPane {

    @FXML
    private Label editor_label;
    @FXML
    private DoubleField editor_textfield;

    private double mini;
    private double maxi;
    private double incDecValue;
    private final DoubleProperty value = new SimpleDoubleProperty();
    private final EffectPickerController effectPickerController;
    private final int roundingFactor = 100; // 2 decimals rounding

    public DoubleTextFieldControl(
            EffectPickerController effectPickerController,
            String labelString,
            double min,
            double max,
            double initVal,
            double incDec) {
        this.effectPickerController = effectPickerController;
        initialize(labelString, min, max, initVal, incDec);

        editor_textfield.focusedProperty().addListener((ChangeListener<Boolean>) (ov, oldValue, newValue) -> {
            // Commit the value on focus lost
            if (newValue == false) {
                double inputValue = Double.parseDouble(editor_textfield.getText());
                // First update the model
                setValue(inputValue);
                // Then notify the controller a change occured
                effectPickerController.incrementRevision();
            }
        });
        editor_textfield.setOnAction((ActionEvent event) -> {
            event.consume();
        });
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    public double getValue() {
        return value.get();
    }

    @FXML
    void textfieldTyped(KeyEvent e) {
        if (e.getCode() == KeyCode.UP) {
            // First update the model
            incOrDecValue(incDecValue);
            // Then notify the controller a change occured
            effectPickerController.incrementRevision();
        }
        if (e.getCode() == KeyCode.DOWN) {
            // First update the model
            incOrDecValue(-incDecValue);
            // Then notify the controller a change occured
            effectPickerController.incrementRevision();
        }
        if (e.getCode() == KeyCode.ENTER) {
            double inputValue = Double.parseDouble(editor_textfield.getText());
            // First update the model
            setValue(inputValue);
            // Then notify the controller a change occured
            effectPickerController.incrementRevision();
            editor_textfield.selectAll();
        }
    }

    private void incOrDecValue(double delta) {
        setValue(getValue() + delta);
//        Platform.runLater(new Runnable() {
//            @Override
//            public void run() {
//                // position caret after new value for easy editing
//                editor_textfield.positionCaret(editor_textfield.getText().length());
//            }
//        });
    }

    private void setValue(double d) {
        double val = Utils.clamp(mini, d, maxi);
        double rounded = EditorUtils.round(val, roundingFactor);
        value.set(rounded);
        editor_textfield.setText(Double.toString(rounded));
    }

    private void initialize(String labelString,
            double min, double max, double initVal, double incDec) {

        final URL layoutURL = DoubleTextFieldControl.class.getResource("NumFieldControl.fxml"); //NOI18N
        try (InputStream is = layoutURL.openStream()) {
            FXMLLoader loader = new FXMLLoader();
            loader.setController(this);
            loader.setRoot(this);
            loader.setLocation(layoutURL);
            Parent p = (Parent) loader.load(is);
            assert p == this;
        } catch (IOException x) {
            throw new RuntimeException(x);
        }

        editor_label.setText(labelString);
        incDecValue = incDec;
        mini = min;
        maxi = max;
        setValue(initVal);
    }
}

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

public class SliderControl extends GridPane {

    @FXML
    private Slider editor_slider;
    @FXML
    private Label editor_label;
    @FXML
    private DoubleField editor_textfield;

    private boolean intMode;
    private double incDecValue;
    private final DoubleProperty value = new SimpleDoubleProperty();
    private final EffectPickerController effectPickerController;
    private final int roundingFactor = 100; // 2 decimals rounding

    public SliderControl(
            EffectPickerController effectPickerController,
            String labelString,
            double min,
            double max,
            double initVal,
            double incDec,
            boolean integerMode) {
        this.effectPickerController = effectPickerController;
        initialize(labelString, min, max, initVal, incDec, integerMode);
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    public double getValue() {
        return value.get();
    }

    public Slider getSlider() {
        return editor_slider;
    }

    public TextField getTextField() {
        return editor_textfield;
    }

    @FXML
    void textfieldTyped(KeyEvent e) {
        if (e.getCode() == KeyCode.UP) {
            incOrDecValue(incDecValue);
        } else if (e.getCode() == KeyCode.DOWN) {
            incOrDecValue(-incDecValue);
        } else if (e.getCode() == KeyCode.ENTER) {
            double inputValue = Double.parseDouble(editor_textfield.getText());
            setValue(inputValue);
            editor_slider.setValue(getValue());
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

    private void setValue(Number n) {
        if (intMode) {
            long rounded = Math.round(n.doubleValue());
            value.set(rounded);
            editor_textfield.setText(Long.toString(rounded));
        } else {
            double val = Utils.clamp(editor_slider.getMin(), n.doubleValue(), editor_slider.getMax());
            double rounded = EditorUtils.round(val, roundingFactor);
            value.set(rounded);
            editor_textfield.setText(Double.toString(rounded));
        }
        editor_slider.setValue(value.get());
    }

    private void initialize(
            String labelString,
            double min,
            double max,
            double initVal,
            double incDec,
            boolean integerMode) {

        final URL layoutURL = SliderControl.class.getResource("SliderControl.fxml"); //NOI18N
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
        intMode = integerMode;
        editor_slider.setMin(min);
        editor_slider.setMax(max);
        editor_slider.setValue(initVal);
        setValue(initVal);
        editor_slider.valueProperty().addListener((ChangeListener<Number>) (ov, oldVal, newVal) -> {
            // First update the model
            setValue(newVal);
            // Then notify the controller a change occured
            effectPickerController.incrementRevision();
        });
        editor_slider.pressedProperty().addListener((ChangeListener<Boolean>) (ov, oldValue, newValue) -> effectPickerController.setLiveUpdate(newValue));

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
        
        editor_textfield.setOnAction((ActionEvent e) -> {
            e.consume();
        });
    }
}

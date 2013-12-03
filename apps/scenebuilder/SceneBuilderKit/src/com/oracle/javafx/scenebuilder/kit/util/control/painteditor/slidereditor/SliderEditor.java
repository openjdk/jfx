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
package com.oracle.javafx.scenebuilder.kit.util.control.painteditor.slidereditor;

import com.oracle.javafx.scenebuilder.kit.util.control.painteditor.gradienteditor.GradientEditor;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

public class SliderEditor extends GridPane {

    @FXML
    private Slider editor_slider;
    @FXML
    private Label editor_label;
    @FXML
    private TextField editor_textfield;

    public SliderEditor(String text, double min, double max, double initVal) {
        initialize(text, min, max, initVal);
    }

    public final Slider getSlider() {
        return editor_slider;
    }
    
    /**
     * Private
     */
    private void initialize(String text, double min, double max, double initVal) {

        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(SliderEditor.class.getResource("SliderEditor.fxml")); //NOI18N
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GradientEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        assert editor_label != null;
        assert editor_slider != null;
        assert editor_textfield != null;

        editor_label.setText(text);
        editor_slider.setMin(min);
        editor_slider.setMax(max);
        editor_slider.setValue(initVal);
        editor_textfield.setText("" + initVal); // need a setter for this that runs all the checks

        editor_slider.valueProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                editor_textfield.setText("" + newValue);
            }
        });
    }

    @FXML
    void textfieldTyped(KeyEvent e) {

        if (e.getCode() == KeyCode.UP) {
            incOrDecFieldValue(e, 0.1);
        }
        if (e.getCode() == KeyCode.DOWN) {
            incOrDecFieldValue(e, -0.1);
        }
        if (e.getCode() == KeyCode.ENTER) {
            double value = Double.valueOf(editor_textfield.getText());
            editor_slider.setValue(value);
            if (value > editor_slider.getMax()) {
                value = editor_slider.getMax();
                editor_textfield.setText("" + value);
            }
            if (value < editor_slider.getMin()) {
                value = editor_slider.getMin();
                editor_textfield.setText("" + value);
            }
            editor_textfield.selectAll();
        }
    }

    private void incOrDecFieldValue(KeyEvent e, double x) {

        if (!(e.getSource() instanceof TextField)) {
            return; // check it's a textField
        }        // increment or decrement the value
        final TextField tf = (TextField) e.getTarget();
        final String s = tf.getText();
        final Double d = (Double.valueOf(s) + x);
        final DecimalFormat df = new DecimalFormat("0.###"); //NOI18N
        tf.setText(df.format(d));
        // Avoid using runLater
        // This should be done somewhere else (need to investigate)
//        Platform.runLater(new Runnable() {
//            @Override
//            public void run() {
//                // position caret after new value for easy editing
//                tf.positionCaret(tf.getText().length());
//            }
//        });
    }
}

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
package com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.slider;

import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.gradientpicker.GradientPicker;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

public class SliderControl extends GridPane {

    @FXML
    private Slider slider_slider;
    @FXML
    private Label slider_label;
    @FXML
    private TextField slider_textfield;

    public SliderControl(String text, double min, double max, double initVal) {
        initialize(text, min, max, initVal);
    }

    public final Slider getSlider() {
        return slider_slider;
    }

    /**
     * Private
     */
    private void initialize(String text, double min, double max, double initVal) {

        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(SliderControl.class.getResource("SliderControl.fxml")); //NOI18N
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GradientPicker.class.getName()).log(Level.SEVERE, null, ex);
        }

        assert slider_label != null;
        assert slider_slider != null;
        assert slider_textfield != null;

        slider_label.setText(text);
        slider_slider.setMin(min);
        slider_slider.setMax(max);
        slider_slider.setValue(initVal);
        slider_textfield.setText("" + initVal); //NOI18N

        slider_slider.valueProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                slider_textfield.setText("" + newValue); //NOI18N
            }
        });
    }

    @FXML
    void sliderAction(ActionEvent event) {
        double value = Double.valueOf(slider_textfield.getText());
        slider_slider.setValue(value);
        if (value > slider_slider.getMax()) {
            value = slider_slider.getMax();
            slider_textfield.setText("" + value); //NOI18N
        }
        if (value < slider_slider.getMin()) {
            value = slider_slider.getMin();
            slider_textfield.setText("" + value); //NOI18N
        }
        slider_textfield.selectAll();
    }

    @FXML
    void sliderKeyPressed(KeyEvent e) {
        switch (e.getCode()) {
            case UP:
                incOrDecFieldValue(e, 0.1);
                break;
            case DOWN:
                incOrDecFieldValue(e, -0.1);
                break;
            default:
                break;
        }
    }

    private void incOrDecFieldValue(KeyEvent e, double x) {

        if (!(e.getSource() instanceof TextField)) {
            return; // check it's a textField
        }        // increment or decrement the value
        final TextField tf = (TextField) e.getSource();
        final Double newValue = Double.valueOf(tf.getText()) + x;
        tf.setText(Double.toString(newValue));
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

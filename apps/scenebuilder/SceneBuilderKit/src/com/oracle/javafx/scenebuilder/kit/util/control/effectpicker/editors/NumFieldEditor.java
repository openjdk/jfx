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
package com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors;

import com.sun.javafx.Utils;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import javafx.beans.property.DoubleProperty;

public class NumFieldEditor extends GridPane {

    @FXML
    private Label editor_label;
    @FXML
    private TextField editor_textfield;

    private boolean intMode;
    private double mini;
    private double maxi;
    private double incDecValue;
    private final DoubleProperty value = new SimpleDoubleProperty();

    public NumFieldEditor(
            String labelString,
            double min,
            double max,
            double initVal,
            double incDec,
            boolean integerMode) {
        initialize(labelString, min, max, initVal, incDec, integerMode);
    }

    public DoubleProperty getValueProperty() {
        return value;
    }

    public double getValue() {
        return value.get();
    }

    @FXML
    void textfieldTyped(KeyEvent e) {
        if (e.getCode() == KeyCode.UP) {
            incOrDecValue(incDecValue);
        }
        if (e.getCode() == KeyCode.DOWN) {
            incOrDecValue(-incDecValue);
        }
        if (e.getCode() == KeyCode.ENTER) {
            double inputValue = checkStringIsNumber(editor_textfield.getText());
            setValue(inputValue);
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
        DecimalFormat df = new DecimalFormat("#,###,###,##0.00"); //NOI18N
        value.set(new Double(df.format(n)).doubleValue());
        value.set(Utils.clamp(mini, value.get(), maxi));
        if (intMode) {
            int rounded = (int) Math.round((double) n);
            editor_textfield.setText("" + (int) Math.round((double) n)); //NOI18N
            value.set(rounded);
        } else {
            editor_textfield.setText("" + getValue()); //NOI18N
        }
    }

    private double checkStringIsNumber(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void initialize(String labelString,
            double min, double max, double initVal, double incDec, boolean integerMode) {

        final URL layoutURL = NumFieldEditor.class.getResource("NumFieldEditor.fxml"); //NOI18N
        try (InputStream is = layoutURL.openStream()) {
            FXMLLoader loader = new FXMLLoader();
            loader.setController(this);
            loader.setRoot(this);
            loader.setLocation(layoutURL);
            Parent p = (Parent) loader.load(is);
            assert p == this;

            editor_label.setText(labelString);
            incDecValue = incDec;
            intMode = integerMode;
            mini = min;
            maxi = max;
            setValue(initVal);

        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }
}

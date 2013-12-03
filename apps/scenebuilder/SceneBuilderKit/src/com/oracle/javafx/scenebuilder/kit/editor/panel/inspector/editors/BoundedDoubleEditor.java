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
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors;

import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import java.util.Set;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;

/**
 * Editor for bounded double properties. (e.g. 0 &lt;= opacity &lt;= 1)
 *
 * 
 */
public class BoundedDoubleEditor extends PropertyEditor {

    @FXML
    private Slider slider;
    @FXML
    private TextField textField;

    private final Parent root;
    // default min and max
    double min = 0;
    double max = 100;
    private int roundingFactor = 1; // no decimals
    private boolean updateFromTextField = false;
    private boolean updateFromSlider = false;

    @SuppressWarnings("LeakingThisInConstructor")
    public BoundedDoubleEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        root = EditorUtils.loadFxml("BoundedDoubleEditor.fxml", this); //NOI18N

        //
        // Text field
        //
        EventHandler<ActionEvent> onActionListener = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (isHandlingError()) {
                    // Event received because of focus lost due to error dialog
                    return;
                }
                if (isUpdateFromModel() || updateFromSlider) {
                    // nothing to do
                    return;
                }
                String valStr = textField.getText();
                double valDouble;
                try {
                    valDouble = Double.parseDouble(valStr);
                } catch (NumberFormatException e) {
                    handleInvalidValue(valStr);
                    return;
                }
                if (!((DoublePropertyMetadata) getPropertyMeta()).isValidValue(valDouble)) {
                    handleInvalidValue(valDouble);
                    return;
                }
                // If the value is less than the minimum, or more than the maximum,
                // set the value to min or max
                if (valDouble < min || valDouble > max) {
                    if (valDouble < min) {
                        valDouble = min;
                    } else if (valDouble > max) {
                        valDouble = max;
                    }
                    textField.setText(EditorUtils.valAsStr(valDouble));
                }
                textField.selectAll();
                updateFromTextField = true;
                slider.setValue(valDouble);
                updateFromTextField = false;
                userUpdateValueProperty(valDouble);
            }
        };
        initialize(onActionListener);

        //
        // Slider
        //
        configureSlider(propMeta);

        slider.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable valueModel) {
//                System.out.println("Slider : valueProperty changed!");
                if (isUpdateFromModel() || updateFromTextField) {
                    // nothing to do
                    return;
                }

                // Slider button moved or left/right key typed.
                // In this case, we want to round the value,
                // since the Slider may returns many decimals.
                double value = EditorUtils.round(slider.getValue(), roundingFactor);
                updateFromSlider = true;
                textField.setText(EditorUtils.valAsStr(value));
                updateFromSlider = false;
                userUpdateValueProperty(value);
            }
        });
    }

    // Method to please FindBugs
    private void initialize(EventHandler<ActionEvent> onActionListener) {
        setTextEditorBehavior(this, textField, onActionListener, false);
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(root);
    }

    @Override
    public Object getValue() {
        return getValueFromTextField();
    }

    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        assert (value instanceof Double);
        slider.setValue((Double) value);
        textField.setText(EditorUtils.valAsStr(value));
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
        configureSlider(propMeta);
    }

    @Override
    protected void valueIsIndeterminate() {
        handleIndeterminate(textField);
    }

    private double getValueFromTextField() {
        double valueTextField = 0;
        try {
            valueTextField = Double.parseDouble(textField.getText());
        } catch (NumberFormatException e) {
            // should not happen: already checked in text field listener
            assert false;
        }
        return valueTextField;
    }

    private void configureSlider(ValuePropertyMetadata propMeta) {
        assert propMeta instanceof DoublePropertyMetadata;
        DoublePropertyMetadata doublePropMeta = (DoublePropertyMetadata) propMeta;
        DoublePropertyMetadata.DoubleKind kind = doublePropMeta.getKind();
        if ((kind == DoublePropertyMetadata.DoubleKind.OPACITY)
                || (kind == DoublePropertyMetadata.DoubleKind.PROGRESS)) {
            min = 0;
            max = 1;
            roundingFactor = 100; // 2 decimals
        }
        slider.setMin(min);
        slider.setMax(max);
        slider.setBlockIncrement((max - min) / 20);
    }

    @Override
    protected void requestFocus() {
        EditorUtils.doNextFrame(new Runnable() {

            @Override
            public void run() {
                textField.requestFocus();
            }
        });
    }
}

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

import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * Editor for SplitPnae divider positions (list of DOuble).
 *
 *
 */
public class DividerPositionsEditor extends PropertyEditor {

    private final VBox vbox = new VBox(5);

    public DividerPositionsEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        setLayoutFormat(PropertyEditor.LayoutFormat.SIMPLE_LINE_TOP);
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(vbox);
    }

    @Override
    public Object getValue() {
        Double[] values = new Double[getDoubleFields().size()];
        int index = 0;
        for (Node node : getDoubleFields()) {
            assert node instanceof DoubleField;
            DoubleField doubleField = (DoubleField) node;
            String val = doubleField.getText();
            if (val.isEmpty()) {
                val = "0"; //NOI18N
                doubleField.setText(val);
            } else {
                try {
                    Double.parseDouble(val);
                } catch (NumberFormatException e) {
                    // should not happen, DoubleField should prevent any error
                    return null;
                }
            }
            values[index] = Double.valueOf(val);
            index++;
        }
        return Arrays.asList(values);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        assert value != null;
        assert value instanceof List;
        List<Double> doubleList = (List<Double>) value;

        // Round values : 4 decimals seems enough
        List<Double> roundedValues = new ArrayList<>();
        for (double val : doubleList) {
            roundedValues.add(EditorUtils.round(val, 10000));
        }

        fillVBox(roundedValues);
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
        vbox.getChildren().clear();
        setLayoutFormat(PropertyEditor.LayoutFormat.SIMPLE_LINE_TOP);
    }

    @Override
    protected void valueIsIndeterminate() {
        // Add a simple text field with the indeterminate char
        List<Double> value = new ArrayList<>();
        value.add(0.0);
        fillVBox(value);
        handleIndeterminate(getDoubleFields().get(0));
    }

    @Override
    public void requestFocus() {
        EditorUtils.doNextFrame(() -> getDoubleFields().get(0).requestFocus());
    }

    private void fillVBox(List<Double> values) {
        vbox.getChildren().clear();
        for (Double value : values) {
            double val = (value != null) ? value : 0;
            DoubleField doubleField = new DoubleField();
            doubleField.setText(EditorUtils.valAsStr(val));
            vbox.getChildren().add(doubleField);
            EventHandler<ActionEvent> valueListener = event -> userUpdateValueProperty(getValue());
            setNumericEditorBehavior(this, doubleField, valueListener, false);
        }
    }

    private List<Node> getDoubleFields() {
        return vbox.getChildren();
    }
}

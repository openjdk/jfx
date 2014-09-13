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

import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Point3D editor (for x/y/z fields).
 * (used for instance by Transforms.rotationAxis property)
 *
 */
public class Point3DEditor extends PropertyEditor {

    private Parent root;
    @FXML
    private DoubleField xDf;
    @FXML
    private DoubleField yDf;
    @FXML
    private DoubleField zDf;
    DoubleField[] doubleFields = new DoubleField[3];

    public Point3DEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        initialize();
    }

    //Method to please FindBugs
    private void initialize() {
        root = EditorUtils.loadFxml("Point3DEditor.fxml", this);//NOI18N

        doubleFields[0] = xDf;
        doubleFields[1] = yDf;
        doubleFields[2] = zDf;
        for (DoubleField doubleField : doubleFields) {
            EventHandler<ActionEvent> valueListener = event -> userUpdateValueProperty(getValue());
            setNumericEditorBehavior(this, doubleField, valueListener, false);
        }
        setLayoutFormat(PropertyEditor.LayoutFormat.SIMPLE_LINE_BOTTOM);
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(root);
    }

    @Override
    public Object getValue() {
        Double[] values = new Double[3];
        int index = 0;
        for (DoubleField doubleField : doubleFields) {
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
        return new Point3D(values[0], values[1], values[2]);
    }

    @Override
    public void setValue(Object value) {
        assert value != null;
        assert value instanceof Point3D;

        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        Point3D point3D = (Point3D) value;
        xDf.setText(EditorUtils.valAsStr(point3D.getX()));
        yDf.setText(EditorUtils.valAsStr(point3D.getY()));
        zDf.setText(EditorUtils.valAsStr(point3D.getZ()));
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
        setLayoutFormat(PropertyEditor.LayoutFormat.SIMPLE_LINE_BOTTOM);
    }

    @Override
    protected void valueIsIndeterminate() {
        for (DoubleField doubleField : doubleFields) {
            handleIndeterminate(doubleField);
        }
    }

    @Override
    public void requestFocus() {
        EditorUtils.doNextFrame(() -> xDf.requestFocus());
    }

}

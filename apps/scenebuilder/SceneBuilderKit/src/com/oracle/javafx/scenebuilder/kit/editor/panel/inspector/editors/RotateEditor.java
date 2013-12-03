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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

/**
 * Editor for bounded double properties. (e.g. 0 &lt;= opacity &lt;= 1)
 *
 * 
 */
public class RotateEditor extends PropertyEditor {

    @FXML
    private TextField rotateTf;

    @FXML
    private Button rotatorDial;

    @FXML
    private Button rotatorHandle;

    private final Parent root;
    private int roundingFactor = 10; // 1 decimal
    private boolean updateFromRotator = false;

    @SuppressWarnings("LeakingThisInConstructor")
    public RotateEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        root = EditorUtils.loadFxml("RotateEditor.fxml", this);

        //
        // Text field
        //
        EventHandler<ActionEvent> valueListener = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (isHandlingError()) {
                    // Event received because of focus lost due to error dialog
                    return;
                }
                String valStr = rotateTf.getText();
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
                rotate(valDouble);
                rotateTf.selectAll();
                userUpdateValueProperty(valDouble);

            }
        };
        initialize(valueListener);
    }
    
    // Method to please FindBugs
    private void initialize(EventHandler<ActionEvent> valueListener) {
        setTextEditorBehavior(this, rotateTf, valueListener, false);
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(root);
    }

    @Override
    public Object getValue() {
        return EditorUtils.round(rotatorHandle.getRotate(), roundingFactor);
    }

    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        assert (value instanceof Double);
        rotate((Double) value);
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
//        setValueGeneric(propMeta.getDefaultValueObject());
    }

    @Override
    protected void valueIsIndeterminate() {
        handleIndeterminate(rotateTf);
    }

    @FXML
    void rotatorPressed(MouseEvent e) {
        rotatorDragged(e);
    }

    @FXML
    public void rotatorDragged(MouseEvent e) {
//        System.out.println("in RotateEditor.rotatorDragged");
        updateFromRotator = true;
        Parent p = rotatorDial.getParent();
        Bounds b = rotatorDial.getLayoutBounds();
        Double centerX = b.getMinX() + (b.getWidth() / 2);
        Double centerY = b.getMinY() + (b.getHeight() / 2);
        Point2D center = p.localToParent(centerX, centerY);
        Point2D mouse = p.localToParent(e.getX(), e.getY());
        Double deltaX = mouse.getX() - center.getX();
        Double deltaY = mouse.getY() - center.getY();
        Double radians = Math.atan2(deltaY, deltaX);
        rotate(Math.toDegrees(radians));
        userUpdateValueProperty(getValue());
        updateFromRotator = false;
    }

    private void rotate(Double degrees) {
        rotatorHandle.setRotate(degrees);
        if (updateFromRotator) {
            // Round the value
            rotateTf.setText(EditorUtils.valAsStr(getValue()));
        } else {
            // Do not round the value (more decimals may be required)
            rotateTf.setText(EditorUtils.valAsStr(degrees));
        }
    }

    @Override
    protected void requestFocus() {
        EditorUtils.doNextFrame(new Runnable() {

            @Override
            public void run() {
                rotateTf.requestFocus();
            }
        });
    }

}

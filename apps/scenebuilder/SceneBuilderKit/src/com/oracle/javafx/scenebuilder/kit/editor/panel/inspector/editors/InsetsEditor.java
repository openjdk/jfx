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
import java.util.Set;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Insets editor (for top/right/bottom/left fields).
 *
 * 
 */
public class InsetsEditor extends PropertyEditor {

    private final Parent root;
    @FXML
    private Button linkBt;
    @FXML
    private TextField bottomTf;
    @FXML
    private TextField leftTf;
    @FXML
    private TextField rightTf;
    @FXML
    private TextField topTf;
    TextField[] textFields = new TextField[4];
    TextField errorTf;

    @SuppressWarnings("LeakingThisInConstructor")
    public InsetsEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        root = EditorUtils.loadFxml("InsetsEditor.fxml", this);

        initialize();
        setLayoutFormat(LayoutFormat.SIMPLE_LINE_BOTTOM);
    }

    //Method to please FindBugs
    private void initialize() {
        textFields[0] = topTf;
        textFields[1] = rightTf;
        textFields[2] = bottomTf;
        textFields[3] = leftTf;
        for (TextField tf : textFields) {
            EventHandler<ActionEvent> valueListener = new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (isHandlingError()) {
                        // Event received because of focus lost due to error dialog
                        return;
                    }
                    // !! Should check if invalid value ! 
                    userUpdateValueProperty(getValue());
                }
            };
            setNumericEditorBehavior(this, tf, valueListener, false);
        }
        linkBt.disableProperty().bind(disableProperty());
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(root);
    }

    @Override
    public Object getValue() {
        Double[] values = new Double[4];
        int index = 0;
        for (TextField tf : textFields) {
            String val = tf.getText();
            try {
                Double.parseDouble(val);
            } catch (NumberFormatException e) {
                errorTf = tf;
                handleInvalidValue(val);
                return null;
            }
            values[index] = new Double(val);
            index++;
        }
        if ((values[0] == 0.0) && (values[1] == 0.0) && (values[2] == 0.0) && (values[3] == 0.0)) {
            // This is equivalent to null
            return null;
        }
        return new Insets(values[0], values[1], values[2], values[3]);
    }

    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        // Should not be null once the metadata has been fixed
//        assert value instanceof Insets;
        if (value == null) {
            // TEMP waiting for metadata fix
            value = Insets.EMPTY;
        }
        Insets insets = (Insets) value;
        topTf.setText(EditorUtils.valAsStr(insets.getTop()));
        rightTf.setText(EditorUtils.valAsStr(insets.getRight()));
        bottomTf.setText(EditorUtils.valAsStr(insets.getBottom()));
        leftTf.setText(EditorUtils.valAsStr(insets.getLeft()));
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
        setLayoutFormat(LayoutFormat.SIMPLE_LINE_BOTTOM);
    }

    @Override
    protected void valueIsIndeterminate() {
        for (TextField tf : textFields) {
            handleIndeterminate(tf);
        }
    }

    //
    // FXML methods
    //
    @FXML
    void linkValuesAction(ActionEvent event) {
        linkValues();
    }

    @FXML
    void linkValuesKeypressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            linkValues();
        }
    }

    private void linkValues() {
        String t = topTf.getText();
        rightTf.setText(t);
        bottomTf.setText(t);
        leftTf.setText(t);
        userUpdateValueProperty(getValue());
    }

    @Override
    protected void requestFocus() {
        EditorUtils.doNextFrame(new Runnable() {

            @Override
            public void run() {
                if (errorTf != null) {
                    errorTf.requestFocus();
                } else {
                    topTf.requestFocus();
                }
            }
        });
    }

}

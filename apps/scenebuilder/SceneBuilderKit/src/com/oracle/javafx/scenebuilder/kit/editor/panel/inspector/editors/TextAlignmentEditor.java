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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.TextAlignment;

/**
 * TextAlignment property editor (left/center/right/justify toggle buttons with
 * icons).
 *
 */
public class TextAlignmentEditor extends PropertyEditor {

    private final Parent root;
    @FXML
    private ToggleButton leftTb;
    @FXML
    private ToggleButton centerTb;
    @FXML
    private ToggleButton rightTb;
    @FXML
    private ToggleButton justifyTb;

    private final ToggleButton[] toggleButtons = new ToggleButton[4];

    @SuppressWarnings("LeakingThisInConstructor")
    public TextAlignmentEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        root = EditorUtils.loadFxml("TextAlignmentEditor.fxml", this); //NOI18N

        initialize();
        setLayoutFormat(LayoutFormat.SIMPLE_LINE_BOTTOM);
    }

    //Method to please FindBugs
    private void initialize() {
        toggleButtons[0] = leftTb;
        toggleButtons[1] = centerTb;
        toggleButtons[2] = rightTb;
        toggleButtons[3] = justifyTb;
        for (ToggleButton tb : toggleButtons) {
            tb.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent t) {
                    userUpdateValueProperty(getValue());
                }
            });
            tb.disableProperty().bind(disableProperty());
        }
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(root);
    }

    @Override
    public Object getValue() {
        for (ToggleButton tb : toggleButtons) {
            if (tb.isSelected()) {
                if (tb.equals(leftTb)) {
                    return TextAlignment.LEFT.toString();
                } else if (tb.equals(centerTb)) {
                    return TextAlignment.CENTER.toString();
                } else if (tb.equals(rightTb)) {
                    return TextAlignment.RIGHT.toString();
                } else if (tb.equals(justifyTb)) {
                    return TextAlignment.JUSTIFY.toString();
                }
            }
        }
        return getPropertyMeta().getDefaultValueObject();
    }

    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        if (value == null) {
            value = getPropertyMeta().getDefaultValueObject();
        }
        assert value instanceof String;
        if (value.equals(TextAlignment.LEFT.toString())) {
            leftTb.setSelected(true);
        } else if (value.equals(TextAlignment.CENTER.toString())) {
            centerTb.setSelected(true);
        } else if (value.equals(TextAlignment.RIGHT.toString())) {
            rightTb.setSelected(true);
        } else if (value.equals(TextAlignment.JUSTIFY.toString())) {
            justifyTb.setSelected(true);
        }
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
        setLayoutFormat(LayoutFormat.SIMPLE_LINE_BOTTOM);
    }

    @Override
    protected void valueIsIndeterminate() {
        for (ToggleButton tb : toggleButtons) {
            tb.setSelected(false);
        }
    }

    @Override
    public void requestFocus() {
        EditorUtils.doNextFrame(new Runnable() {

            @Override
            public void run() {
                leftTb.requestFocus();
            }
        });
    }
}

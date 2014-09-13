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
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * Editor for Double properties, with pre-defined constants (handled by
 * auto-suggest popup).
 *
 * 
 */
public class DoubleEditor extends AutoSuggestEditor {

    private Map<String, Object> constants;

    public DoubleEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, Map<String, Object> constants) {
        super(propMeta, selectedClasses, new ArrayList<>(constants.keySet()), AutoSuggestEditor.Type.DOUBLE);
        initialize(constants);
    }
    
    private void initialize(Map<String, Object> constants) {
        this.constants = constants;

        EventHandler<ActionEvent> onActionListener = event -> {
            if (isHandlingError()) {
                // Event received because of focus lost due to error dialog
                return;
            }
            Object value = getValue();
            if ((value != null) && ((DoublePropertyMetadata) getPropertyMeta()).isValidValue((Double) value)) {
                userUpdateValueProperty(value);
                getTextField().selectAll();
            } else {
                handleInvalidValue(getTextField().getText());
            }
        };

        setNumericEditorBehavior(this, getTextField(), onActionListener);
    }

    @Override
    public Object getValue() {
        String val = getTextField().getText();
        if (val.isEmpty()) {
            val = "0"; //NOI18N
            getTextField().setText(val);
            return Double.valueOf(val);
        }
        Object constantValue = constants.get(val.toUpperCase(Locale.ROOT));
        if (constantValue != null) {
            val = EditorUtils.valAsStr(constantValue);
        }
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void setValue(Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        assert (value instanceof Double);
        // Get the corresponding constant if any
        for (Entry<String, Object> entry : constants.entrySet()) {
            if (value.equals(entry.getValue())) {
                value = entry.getKey();
            }
        }
        getTextField().setText(EditorUtils.valAsStr(value));
    }

    @Override
    public void requestFocus() {
        EditorUtils.doNextFrame(() -> getTextField().requestFocus());
    }

    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses,
            Map<String, Object> constants) {
        super.reset(propMeta, selectedClasses, new ArrayList<>(constants.keySet()));
        this.constants = constants;
    }
}

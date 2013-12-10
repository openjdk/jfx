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

import java.util.List;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * Controller class editor.
 *
 *
 */
public class ControllerClassEditor extends AutoSuggestEditor {

    private static final String PROPERTY_NAME = "Controller class";
    private static final String DEFAULT_VALUE = "";

    @SuppressWarnings("LeakingThisInConstructor")
    public ControllerClassEditor(List<String> suggestedClasses) {
        super(PROPERTY_NAME, DEFAULT_VALUE, suggestedClasses); //NOI18N

        // text field events handling
        EventHandler<ActionEvent> onActionListener = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String value = textField.getText();
                userUpdateValueProperty(value);
                textField.selectAll();
            }
        };
        setTextEditorBehavior(this, textField, onActionListener);
    }

    public void reset(List<String> suggestedClasses) {
        super.reset(PROPERTY_NAME, DEFAULT_VALUE, suggestedClasses);
    }

    @Override
    public void requestFocus() {
        EditorUtils.doNextFrame(new Runnable() {

            @Override
            public void run() {
                textField.requestFocus();
            }
        });
    }
}

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
package com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog;

import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import java.util.HashMap;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.Window;

/**
 * A modal dialog which displays a piece of text and provides a Close button
 * and a Copy button.
 * 
 * 
 */
public class TextViewDialog extends AbstractModalDialog {
    
    @FXML
    private TextArea textArea;
    
    /*
     * Public
     */
    
    
    public TextViewDialog(Window owner) {
        super(TextViewDialog.class.getResource("TextViewDialog.fxml"), null, owner); //NOI18N
        setOKButtonVisible(false);
        setActionButtonVisible(true);
        setCancelButtonTitle(I18N.getString("label.close"));
        setActionButtonTitle(I18N.getString("label.copy"));
    }
    
    public void setText(String text) {
        textArea.setText(text);
    }
    
    public String getText() {
        return textArea.getText();
    }
    
    /*
     * AbstractModalDialog
     */
    
    @Override
    protected void controllerDidLoadContentFxml() {
        assert textArea != null;
    }
    
    @Override
    protected void okButtonPressed(ActionEvent e) {
        // Should not be called because ok button is hidden
        throw new IllegalStateException();
    }
    
    @Override
    protected void cancelButtonPressed(ActionEvent e) {
        getStage().close();
    }
    
    @Override
    protected void actionButtonPressed(ActionEvent e) {
        final Map<DataFormat, Object> content = new HashMap<>();
        content.put(DataFormat.PLAIN_TEXT, getText());
        Clipboard.getSystemClipboard().setContent(content);
    }
    
}

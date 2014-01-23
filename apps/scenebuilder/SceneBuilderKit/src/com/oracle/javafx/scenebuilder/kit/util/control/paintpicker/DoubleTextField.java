/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.util.control.paintpicker;

import java.util.Locale;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;

/**
 * Define a text field that accept only doubles.
 */
public class DoubleTextField extends TextField {

    public DoubleTextField() {
    }

    @Override
    public void replaceText(int start, int end, String text) {
        String newText = getNewText(start, end, text);
        if (!text.isEmpty() // Always allow text deletion
                && (!newText.equals("-") && !newText.equals(".") && !newText.equals("-."))) { //NOI18N
            try {
                if (newText.toLowerCase(Locale.ROOT).contains("d") || newText.toLowerCase(Locale.ROOT).contains("f")) { //NOI18N
                    // 'd' and 'f' are valid for a Double,
                    // but we don't want to accept them in the editor
                    return;
                }
                // Replace ',' by '.'
                newText = newText.replace(',', '.'); //NOI18N
                Double.parseDouble(newText);
            } catch (NumberFormatException e) {
                return;
            }
        }
        // Replace ',' by '.'
        text = text.replace(',', '.'); //NOI18N
        super.replaceText(start, end, text);
    }

    @Override
    public void paste() {
        String strToPaste = Clipboard.getSystemClipboard().getString();
        try {
            Double.parseDouble(strToPaste);
        } catch (NumberFormatException e) {
            return;
        }
        super.paste();
    }

    private String getNewText(int start, int end, String text) {
        String oldText = getText();
        String toReplace = oldText.substring(start, end);
        String newText;
        if (toReplace.isEmpty()) {
            // start/end is outside oldText ==> add
            newText = oldText + text;
        } else {
            String headerStr = oldText.substring(0, start);
            String trailerStr = ""; //NOI18N
            if (end < oldText.length()) {
                trailerStr = oldText.substring(end, oldText.length());
            }
            newText = headerStr + text + trailerStr;
        }
        return newText;
     }

}

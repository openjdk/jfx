/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.tools.fx.monkey.tools;

import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import com.oracle.tools.fx.monkey.util.FX;

/**
 * Keyboard / InputMethod Event Viewer
 */
public class KeyboardEventViewer extends BorderPane {
    private final TextArea textField;

    public KeyboardEventViewer() {
        FX.name(this, "KeyboardEventViewer");

        textField = new TextArea();
        textField.setEditable(false);
        textField.addEventFilter(KeyEvent.ANY, this::handleKeyboardEvent);
        textField.setOnInputMethodTextChanged(this::inputMethodTextChangedEvent);

        Button clearButton = new Button("Clear");
        clearButton.setOnAction((ev) -> clear());

        ToolBar tp = new ToolBar(clearButton);

        setCenter(textField);
        setTop(tp);

        textField.requestFocus();
    }

    private void clear() {
        textField.clear();
    }

    private void handleKeyboardEvent(KeyEvent ev) {
        StringBuilder sb = new StringBuilder();
        sb.append("KeyEvent{");
        sb.append("type=").append(ev.getEventType());
        sb.append(", character=").append(ev.getCharacter());
        sb.append(", text=").append(ev.getText());
        sb.append(", code=").append(ev.getCode());

        if (ev.isShiftDown()) {
            sb.append(", shift");
        }
        if (ev.isControlDown()) {
            sb.append(", control");
        }
        if (ev.isAltDown()) {
            sb.append(", alt");
        }
        if (ev.isMetaDown()) {
            sb.append(", meta");
        }
        if (ev.isShortcutDown()) {
            sb.append(", shortcut");
        }
        sb.append("}\n");
        addToLog(sb.toString());
        ev.consume();
    }

    private void addToLog(String s) {
        textField.setText(textField.getText() + s);

        // scroll to the end
        int ix = textField.getLength();
        textField.selectRange(ix, ix);
    }

    private void inputMethodTextChangedEvent(InputMethodEvent ev) {
        StringBuilder sb = new StringBuilder();
        sb.append("InputMethodEvent{");
        sb.append("type=").append(ev.getEventType());
        sb.append(", caret=").append(ev.getCaretPosition());

        if (!ev.getCommitted().isEmpty()) {
            sb.append(", committed=");
            sb.append(ev.getCommitted());
        }

        if (!ev.getComposed().isEmpty()) {
            sb.append(", composed=");
            for (InputMethodTextRun run: ev.getComposed()) {
                sb.append(run.getText());
            }
        }

        sb.append("}\n");
        addToLog(sb.toString());
    }
}

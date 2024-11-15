/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
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

package com.oracle.demo.richtext.editor;

import java.io.File;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import com.oracle.demo.richtext.rta.RichTextAreaWindow;
import com.oracle.demo.richtext.util.FX;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * Rich Editor Demo window.
 *
 * @author Andy Goryachev
 */
public class RichEditorDemoWindow extends Stage {
    public final RichEditorDemoPane pane;
    public final Label status;

    public RichEditorDemoWindow() {
        pane = new RichEditorDemoPane();

        status = new Label();
        status.setPadding(new Insets(2, 10, 2, 10));

        BorderPane bp = new BorderPane();
        bp.setTop(createMenu());
        bp.setCenter(pane);
        bp.setBottom(status);

        Scene scene = new Scene(bp);

        setScene(scene);
        setWidth(1200);
        setHeight(600);

        pane.editor.caretPositionProperty().addListener((x) -> {
            updateStatus();
        });
        pane.actions.modifiedProperty().addListener((x) -> {
            updateTitle();
        });
        pane.actions.fileNameProperty().addListener((x) -> {
            updateTitle();
        });
        addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, (ev) -> {
            if (pane.actions.askToSave()) {
                ev.consume();
            }
        });

        updateStatus();
        updateTitle();
    }

    private MenuBar createMenu() {
        Actions actions = pane.actions;
        MenuBar m = new MenuBar();
        // file
        FX.menu(m, "File");
        FX.item(m, "New", actions.newDocument).setAccelerator(KeyCombination.keyCombination("shortcut+N"));
        FX.item(m, "Open...", actions.open);
        FX.separator(m);
        FX.item(m, "Save", actions.save).setAccelerator(KeyCombination.keyCombination("shortcut+S"));
        FX.item(m, "Save As...", actions.saveAs).setAccelerator(KeyCombination.keyCombination("shortcut+A"));
        FX.item(m, "Quit", actions::quit);

        // edit
        FX.menu(m, "Edit");
        FX.item(m, "Undo", actions.undo);
        FX.item(m, "Redo", actions.redo);
        FX.separator(m);
        FX.item(m, "Cut", actions.cut);
        FX.item(m, "Copy", actions.copy);
        FX.item(m, "Paste", actions.paste);
        FX.item(m, "Paste and Retain Style", actions.pasteUnformatted);

        // format
        FX.menu(m, "Format");
        FX.item(m, "Bold", actions.bold).setAccelerator(KeyCombination.keyCombination("shortcut+B"));
        FX.item(m, "Italic", actions.italic).setAccelerator(KeyCombination.keyCombination("shortcut+I"));
        FX.item(m, "Strike Through", actions.strikeThrough);
        FX.item(m, "Underline", actions.underline).setAccelerator(KeyCombination.keyCombination("shortcut+U"));

        // view
        FX.menu(m, "View");
        FX.checkItem(m, "Highlight Current Paragraph", actions.highlightCurrentLine);
        FX.checkItem(m, "Show Line Numbers", actions.lineNumbers);
        FX.checkItem(m, "Wrap Text", actions.wrapText);
        // TODO line spacing

        // view
        FX.menu(m, "Tools");
        FX.item(m, "Settings", this::openSettings);

        // help
        FX.menu(m, "Help");
        FX.item(m, "About"); // TODO

        return m;
    }

    private void updateStatus() {
        RichTextArea ed = pane.editor;
        TextPos p = ed.getCaretPosition();

        StringBuilder sb = new StringBuilder();

        if (p != null) {
            sb.append(" Line: ").append(p.index() + 1);
            sb.append("  Column: ").append(p.offset() + 1);
        }

        status.setText(sb.toString());
    }

    private void updateTitle() {
        File f = pane.actions.getFile();
        boolean modified = pane.actions.isModified();

        StringBuilder sb = new StringBuilder();
        sb.append("Rich Text Editor Demo");
        if (f != null) {
            sb.append(" - ");
            sb.append(f.getName());
        }
        if (modified) {
            sb.append(" *");
        }
        setTitle(sb.toString());
    }

    void openSettings() {
        new SettingsWindow(this).show();
    }
}

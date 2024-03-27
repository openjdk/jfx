/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.demo.rich.editor;

import java.io.File;
import javafx.application.Platform;
import javafx.geometry.Insets;
import jfx.incubator.scene.control.rich.RichTextArea;
import jfx.incubator.scene.control.rich.TextPos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.oracle.demo.rich.rta.RichTextAreaWindow;
import com.oracle.demo.rich.util.FX;

/**
 * Rich Editor Demo window
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
        scene.getStylesheets().addAll(
            // will become a part of modena.css
            RichTextAreaWindow.class.getResource("RichTextArea-Modena.css").toExternalForm()
        );

        // TODO input map for the window: add shortcut-S for saving

        setScene(scene);
        setWidth(1200);
        setHeight(600);

        pane.control.caretPositionProperty().addListener((x) -> {
            updateStatus();
        });
        pane.actions.modifiedProperty().addListener((x) -> {
            updateTitle();
        });
        pane.actions.fileNameProperty().addListener((x) -> {
            updateTitle();
        });
        updateStatus();
        updateTitle();
    }

    private MenuBar createMenu() {
        Actions actions = pane.actions;
        MenuBar m = new MenuBar();
        // file
        FX.menu(m, "File");
        FX.item(m, "New", actions.newDocument);
        FX.item(m, "Open...", actions.open);
        FX.separator(m);
        FX.item(m, "Save...", actions.save);
        // TODO print?
        FX.item(m, "Quit", () -> Platform.exit());

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
        FX.item(m, "Bold", actions.bold);
        FX.item(m, "Italic", actions.italic);
        FX.item(m, "Strike Through", actions.strikeThrough);
        FX.item(m, "Underline", actions.underline);

        // view
        FX.menu(m, "View");
        FX.checkItem(m, "Wrap Text", actions.wrapText);
        // TODO line numbers
        // TODO line spacing

        // help
        FX.menu(m, "Help");
        // TODO about
        return m;
    }

    private void updateStatus() {
        RichTextArea t = pane.control;
        TextPos p = t.getCaretPosition();

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
}

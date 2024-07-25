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
package com.oracle.demo.richtext.notebook;

import java.io.File;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.oracle.demo.richtext.notebook.data.Notebook;
import com.oracle.demo.richtext.rta.RichTextAreaWindow;
import com.oracle.demo.richtext.util.FX;

/**
 * Notebook Demo main window.
 */
public class NotebookWindow extends Stage {
    private static final String TITLE = "Interactive Notebook (Mockup)";
    private final Actions actions;
    private final NotebookPane pane;
    private final Label status;

    public NotebookWindow() {
        FX.name(this, "NotebookWindow");

        actions = new Actions(this);

        pane = new NotebookPane(actions);

        status = new Label();
        status.setPadding(new Insets(2, 10, 2, 10));

        BorderPane bp = new BorderPane();
        bp.setTop(createMenu());
        bp.setCenter(pane);
        bp.setBottom(status);

        Scene scene = new Scene(bp);
        scene.getStylesheets().addAll(
            getClass().getResource("notebook.css").toExternalForm()
        );
        scene.focusOwnerProperty().addListener((s,p,c) -> {
            handleFocusUpdate(c);
        });

        // TODO input map for the window: add shortcut-S for saving

        setScene(scene);
        setWidth(1200);
        setHeight(600);

        actions.modifiedProperty().addListener((x) -> {
            updateTitle();
        });
        actions.fileNameProperty().addListener((x) -> {
            updateTitle();
        });
        updateTitle();

        setNotebook(Demo.createSingleCodeCell());
        //setNotebook(Demo.createNotebookExample());
    }

    private MenuBar createMenu() {
        Menu m2;
        MenuBar b = new MenuBar();
        // file
        FX.menu(b, "File");
        FX.item(b, "New", actions.newDocument);
        FX.item(b, "Open...", actions.open);
        m2 = FX.submenu(b, "Open Recent");
        FX.item(m2, "Notebook Example", () -> setNotebook(Demo.createNotebookExample()));
        FX.item(m2, "Single Text Cell", () -> setNotebook(Demo.createSingleTextCell()));
        FX.item(m2, "Empty Code Cell", () -> setNotebook(Demo.createSingleCodeCell()));
        FX.separator(b);
        FX.item(b, "Save...", actions.save);
        // TODO print?
        FX.item(b, "Quit", () -> Platform.exit());

        // edit
        FX.menu(b, "Edit");
        FX.item(b, "Undo", actions.undo);
        FX.item(b, "Redo", actions.redo);
        FX.separator(b);
        FX.item(b, "Cut", actions.cut);
        FX.item(b, "Copy", actions.copy);
        FX.item(b, "Paste", actions.paste);
        FX.item(b, "Paste and Retain Style", actions.pasteUnformatted);

        // format
        FX.menu(b, "Format");
        FX.checkItem(b, "Bold", actions.bold);
        FX.checkItem(b, "Italic", actions.italic);
        FX.checkItem(b, "Strike Through", actions.strikeThrough);
        FX.checkItem(b, "Underline", actions.underline);

        // cell
        FX.menu(b, "Cell");
        FX.item(b, "Cut Cell", actions.cutCell);
        FX.item(b, "Copy Cell", actions.copyCell);
        FX.item(b, "Paste Cell Below", actions.pasteCellBelow);
        FX.separator(b);
        FX.item(b, "Insert Cell Below", actions.insertCellBelow);
        FX.separator(b);
        FX.item(b, "Move Up", actions.moveCellUp);
        FX.item(b, "Move Down", actions.moveCellDown);
        FX.separator(b);
        FX.item(b, "Split Cell", actions.splitCell);
        FX.item(b, "Merge Cell Above", actions.mergeCellAbove);
        FX.item(b, "Merge Cell Below", actions.mergeCellBelow);
        FX.separator(b);
        FX.item(b, "Delete", actions.deleteCell);

        // run
        FX.menu(b, "Run");
        FX.item(b, "Run Current Cell And Advance", actions.runAndAdvance);
        FX.item(b, "Run All Cells", actions.runAll);

        // view
        FX.menu(b, "View");
        FX.item(b, "Show Line Numbers");

        // help
        FX.menu(b, "Help");
        FX.item(b, "About");
        return b;
    }

    private void updateTitle() {
        File f = actions.getFile();
        boolean modified = actions.isModified();

        StringBuilder sb = new StringBuilder();
        sb.append(TITLE);
        if (f != null) {
            sb.append(" - ");
            sb.append(f.getName());
        }
        if (modified) {
            sb.append(" *");
        }
        setTitle(sb.toString());
    }

    private void handleFocusUpdate(Node n) {
        CellPane p = FX.findParentOf(CellPane.class, n);
        if (p != null) {
            actions.setActiveCellPane(p);
            pane.setActiveCellPane(p);
        }
    }

    public void setNotebook(Notebook b) {
        actions.setNotebook(b);
    }
}

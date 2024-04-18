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
package com.oracle.demo.rich.notebook;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import com.oracle.demo.rich.util.FX;

/**
 * Main Panel.
 */
public class NotebookPane extends BorderPane {
    public final CellContainer cellContainer;
    private final Actions actions;
    private final ComboBox<CellType> cellType;
    private final ComboBox<TextStyle> textStyle;

    public NotebookPane(Actions a) {
        FX.name(this, "RichEditorDemoPane");

        this.actions = a;

        cellContainer = new CellContainer();
        Bindings.bindContent(cellContainer.getChildren(), actions.getCellPanes());
        //cellPane.setContextMenu(createContextMenu());
        // this is a job for the InputMap!
        cellContainer.addEventFilter(KeyEvent.KEY_PRESSED, this::handleContextExecute);

        cellType = new ComboBox<>();
        cellType.getItems().setAll(CellType.values());
        cellType.setConverter(CellType.converter());
        cellType.setOnAction((ev) -> {
            updateActiveCellType();
        });

        textStyle = new ComboBox<>();
        textStyle.getItems().setAll(TextStyle.values());
        textStyle.setConverter(TextStyle.converter());
        // TODO textStyle.valueProperty().bind(actions.textStyleProperty());
        textStyle.setOnAction((ev) -> {
            updateTextStyle();
        });

        ScrollPane scroll = new ScrollPane(cellContainer);
        scroll.setFitToWidth(true);

        setTop(createToolBar());
        setCenter(scroll);

        actions.textStyleProperty().addListener((s,p,c) -> {
            setTextStyle(c);
        });
    }

    // TODO move to window?
    private ToolBar createToolBar() {
        ToolBar t = new ToolBar();
        FX.button(t, "+", "Insert a cell below", actions.insertCellBelow);
        FX.button(t, "Cu", "Cut this cell");
        FX.button(t, "Co", "Copy this cell");
        FX.button(t, "Pa", "Paste this cell from the clipboard");
        FX.add(t, cellType);
        FX.space(t);
        FX.button(t, "▶", "Run this cell and advance", actions.runAndAdvance);
        FX.button(t, "▶▶", "Run all cells", actions.runAll);
        FX.space(t);
        FX.toggleButton(t, "𝐁", "Bold text", actions.bold);
        FX.toggleButton(t, "𝐼", "Bold text", actions.italic);
        FX.toggleButton(t, "S\u0336", "Strike through text", actions.strikeThrough);
        FX.toggleButton(t, "U\u0332", "Underline text", actions.underline);
        FX.add(t, textStyle);
        return t;
    }

    // TODO use this?
    private ContextMenu createContextMenu() {
        ContextMenu m = new ContextMenu();
        FX.item(m, "Cut Cell");
        FX.item(m, "Copy Cell");
        FX.item(m, "Paste Cell Below");
        FX.separator(m);
        FX.item(m, "Delete Cell");
        FX.separator(m);
        FX.item(m, "Split Cell");
        FX.item(m, "Merge Selected Cell");
        FX.item(m, "Merge Cell Above");
        FX.item(m, "Merge Cell Below");
        FX.separator(m);
        FX.item(m, "Undo", actions.undo);
        FX.item(m, "Redo", actions.redo);
        FX.separator(m);
        FX.item(m, "Cut", actions.cut);
        FX.item(m, "Copy", actions.copy);
        FX.item(m, "Paste", actions.paste);
        FX.item(m, "Paste and Retain Style", actions.pasteUnformatted);
        FX.separator(m);
        FX.item(m, "Select All", actions.selectAll);
        return m;
    }

    public void setActiveCellPane(CellPane p) {
        CellType t = (p == null ? null : p.getCellType());
        cellType.getSelectionModel().select(t);
    }

    private void updateActiveCellType() {
        CellType t = cellType.getSelectionModel().getSelectedItem();
        actions.setActiveCellType(t);
    }

    private void handleContextExecute(KeyEvent ev) {
        if (ev.getCode() == KeyCode.ENTER) {
            if (ev.isShortcutDown()) {
                actions.runAndAdvance();
            }
        }
    }

    private void updateTextStyle() {
        TextStyle st = textStyle.getSelectionModel().getSelectedItem();
        if (st != null) {
            actions.setTextStyle(st);
        }
    }

    public void setTextStyle(TextStyle v) {
        textStyle.setValue(v);
    }
}

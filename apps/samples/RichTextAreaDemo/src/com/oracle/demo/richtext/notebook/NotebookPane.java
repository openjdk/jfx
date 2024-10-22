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

package com.oracle.demo.richtext.notebook;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import com.oracle.demo.richtext.common.TextStyle;
import com.oracle.demo.richtext.util.FX;

/**
 * Notebook Main Panel.
 *
 * @author Andy Goryachev
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
        textStyle.setOnAction((ev) -> {
            updateTextStyle();
        });
        textStyle.disableProperty().bind(actions.disabledStyleEditingProperty());

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

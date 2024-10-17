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

import java.util.List;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import com.oracle.demo.richtext.common.TextStyle;
import com.oracle.demo.richtext.util.FX;
import jfx.incubator.scene.control.input.KeyBinding;
import jfx.incubator.scene.control.richtext.RichTextArea;

/**
 * Main Panel.
 *
 * @author Andy Goryachev
 */
public class RichEditorDemoPane extends BorderPane {
    public final RichTextArea control;
    public final Actions actions;
    private final ComboBox<String> fontName;
    private final ComboBox<Integer> fontSize;
    private final ColorPicker textColor;
    private final ComboBox<TextStyle> textStyle;

    public RichEditorDemoPane() {
        FX.name(this, "RichEditorDemoPane");

        control = new RichTextArea();
        // custom function
        control.getInputMap().register(KeyBinding.shortcut(KeyCode.W), () -> {
            System.out.println("Custom function: W key is pressed");
        });

        actions = new Actions(control);
        control.setContextMenu(createContextMenu());

        fontName = new ComboBox<>();
        fontName.getItems().setAll(collectFonts());
        fontName.setOnAction((ev) -> {
            actions.setFontName(fontName.getSelectionModel().getSelectedItem());
        });

        fontSize = new ComboBox<>();
        fontSize.getItems().setAll(
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            16,
            18,
            20,
            22,
            24,
            28,
            32,
            36,
            48,
            72,
            96,
            128
        );
        fontSize.setOnAction((ev) -> {
            actions.setFontSize(fontSize.getSelectionModel().getSelectedItem());
        });

        textColor = new ColorPicker();
        // TODO save/restore custom colors
        FX.tooltip(textColor, "Text Color");
        // FIX there is no API for this!  why is this a property of a skin, not the control??
        // https://stackoverflow.com/questions/21246137/remove-text-from-colour-picker
        textColor.setStyle("-fx-color-label-visible: false ;");
        textColor.setOnAction((ev) -> {
            actions.setTextColor(textColor.getValue());
        });

        textStyle = new ComboBox<>();
        textStyle.getItems().setAll(TextStyle.values());
        textStyle.setConverter(TextStyle.converter());
        textStyle.setOnAction((ev) -> {
            updateTextStyle();
            control.requestFocus();
        });

        setTop(createToolBar());
        setCenter(control);

        actions.textStyleProperty().addListener((s,p,c) -> {
            setTextStyle(c);
        });
    }

    private ToolBar createToolBar() {
        ToolBar t = new ToolBar();
        FX.add(t, fontName);
        FX.add(t, fontSize);
        FX.add(t, textColor);
        FX.space(t);
        // TODO background
        // TODO alignment
        // TODO bullet
        // TODO space left (indent left, indent right)
        // TODO line spacing
        FX.toggleButton(t, "𝐁", "Bold text", actions.bold);
        FX.toggleButton(t, "𝐼", "Bold text", actions.italic);
        FX.toggleButton(t, "S\u0336", "Strike through text", actions.strikeThrough);
        FX.toggleButton(t, "U\u0332", "Underline text", actions.underline);
        FX.add(t, textStyle);
        FX.space(t);
        FX.toggleButton(t, "W", "Wrap Text", actions.wrapText);
        // TODO line numbers
        return t;
    }

    private ContextMenu createContextMenu() {
        ContextMenu m = new ContextMenu();
        FX.item(m, "Undo", actions.undo);
        FX.item(m, "Redo", actions.redo);
        FX.separator(m);
        FX.item(m, "Cut", actions.cut);
        FX.item(m, "Copy", actions.copy);
        FX.item(m, "Paste", actions.paste);
        FX.item(m, "Paste and Retain Style", actions.pasteUnformatted);
        FX.separator(m);
        FX.item(m, "Select All", actions.selectAll);
        FX.separator(m);
        // TODO under "Style" submenu?
        FX.item(m, "Bold", actions.bold);
        FX.item(m, "Italic", actions.italic);
        FX.item(m, "Strike Through", actions.strikeThrough);
        FX.item(m, "Underline", actions.underline);
        return m;
    }

    private static List<String> collectFonts() {
        return Font.getFamilies();
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

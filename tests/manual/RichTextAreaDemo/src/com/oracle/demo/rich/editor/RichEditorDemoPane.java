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

import java.util.List;
import javafx.incubator.scene.control.input.KeyBinding;
import javafx.incubator.scene.control.rich.RichTextArea;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import com.oracle.demo.rich.util.FX;

/**
 * Main Panel.
 */
public class RichEditorDemoPane extends BorderPane {
    public final RichTextArea control;
    public final Actions actions;
    private final ComboBox<String> fontName;
    private final ComboBox<Integer> fontSize;
    private final ColorPicker textColor;

    public RichEditorDemoPane() {
        FX.name(this, "RichEditorDemoPane");

        control = new RichTextArea();
        // custom function
        control.getInputMap().registerKey(KeyBinding.shortcut(KeyCode.W), () -> {
            System.out.println("console!");
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

        setTop(createToolBar());
        setCenter(control);
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
        FX.button(t, "B", "Bold Text", actions.bold);
        FX.button(t, "I", "Italicize Text", actions.italic);
        FX.button(t, "S", "Strike Through Text", actions.strikeThrough);
        FX.button(t, "U", "Underline Text", actions.underline);
        FX.space(t);
        FX.button(t, "W", "Wrap Text", actions.wrapText);
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
}

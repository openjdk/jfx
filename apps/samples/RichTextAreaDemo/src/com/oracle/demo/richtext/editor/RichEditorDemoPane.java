/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates.
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
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import com.oracle.demo.richtext.common.TextStyle;
import com.oracle.demo.richtext.editor.settings.EndKey;
import com.oracle.demo.richtext.util.FX;
import com.oracle.demo.richtext.util.FxAction;
import jfx.incubator.scene.control.input.KeyBinding;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Main panel: the tool bar + editor.
 *
 * @author Andy Goryachev
 */
public class RichEditorDemoPane extends BorderPane {
    public final RichTextArea editor;
    public final Actions actions;
    public final ComboBox<String> fontFamily = new ComboBox<>();
    public final ComboBox<Double> fontSize = new ComboBox<>();
    public final ColorPicker textColor = new ColorPicker();
    private final ComboBox<TextStyle> textStyle = new ComboBox<>();

    public RichEditorDemoPane() {
        FX.name(this, "RichEditorDemoPane");

        editor = new RichTextArea();

        // example of a custom function
        editor.getInputMap().register(KeyBinding.shortcut(KeyCode.W), () -> {
            System.out.println("Custom function: W key is pressed");
        });

        actions = new Actions(this, editor);
        editor.setContextMenu(createContextMenu());

        fontFamily.getItems().setAll(collectFonts());
        fontFamily.setMaxWidth(170);
        fontFamily.setOnAction((ev) -> {
            actions.setFontFamily(fontFamily.getSelectionModel().getSelectedItem());
            editor.requestFocus();
        });

        fontSize.setEditable(true);
        fontSize.setMaxWidth(70);
        fontSize.setConverter(FX.numberConverter());
        fontSize.getItems().setAll(
            8.0,
            9.0,
            10.0,
            11.0,
            12.0,
            13.0,
            14.0,
            16.0,
            18.0,
            24.0,
            32.0,
            48.0,
            96.0
        );
        fontSize.setOnAction((ev) -> {
            actions.setFontSize(fontSize.getSelectionModel().getSelectedItem());
        });

        // TODO save/restore custom colors
        FX.tooltip(textColor, "Text Color");
        // there is no API for this!  why is this a property of a skin, not the control??
        // https://stackoverflow.com/questions/21246137/remove-text-from-colour-picker
        textColor.setStyle("-fx-color-label-visible: false ;");
        textColor.setOnAction((ev) -> {
            actions.setTextColor(textColor.getValue());
        });

        textStyle.getItems().setAll(TextStyle.values());
        textStyle.setConverter(TextStyle.converter());
        textStyle.setOnAction((ev) -> {
            updateTextStyle();
            editor.requestFocus();
        });

        setTop(createToolBar());
        setCenter(editor);

        actions.textStyleProperty().addListener((s,p,c) -> {
            setTextStyle(c);
        });

        editor.insertStylesProperty().bind(Bindings.createObjectBinding(
            this::getInsertStyles,
            actions.bold.selectedProperty(),
            fontFamily.getSelectionModel().selectedItemProperty(),
            fontSize.getSelectionModel().selectedItemProperty(),
            actions.italic.selectedProperty(),
            actions.strikeThrough.selectedProperty(),
            actions.underline.selectedProperty()
        ));

        Settings.endKey.subscribe(this::setEndKey);
    }

    private StyleAttributeMap getInsertStyles() {
        StyleAttributeMap.Builder b = StyleAttributeMap.builder();
        b.
            setBold(actions.bold.isSelected()).
            setFontFamily(fontFamily.getSelectionModel().getSelectedItem()).
            setItalic(actions.italic.isSelected()).
            setStrikeThrough(actions.strikeThrough.isSelected()).
            setUnderline(actions.underline.isSelected());
        if (fontSize.getSelectionModel().getSelectedItem() != null) {
            b.setFontSize(fontSize.getSelectionModel().getSelectedItem());
        }
        return b.build();
    }

    private ToolBar createToolBar() {
        ToolBar t = new ToolBar();
        FX.add(t, fontFamily);
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
        FX.toggleButton(t, "N", "Line Numbers", actions.lineNumbers);
        FX.toggleButton(t, "W", "Wrap Text", actions.wrapText);
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
        // TODO
        // FX.item(m, "Paragraph..."
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

    private void setEndKey(EndKey v) {
        switch(v) {
        case END_OF_LINE:
            editor.getInputMap().restoreDefaultFunction(RichTextArea.Tag.MOVE_TO_LINE_END);
            break;
        case END_OF_TEXT:
            editor.getInputMap().registerFunction(RichTextArea.Tag.MOVE_TO_LINE_END, this::moveToEndOfText);
            break;
        }
    }

    // this is an illustration.  we could publish the MOVE_TO_END_OF_TEXT_ON_LINE function tag
    private void moveToEndOfText() {
        TextPos p = editor.getCaretPosition();
        if (p != null) {
            editor.executeDefault(RichTextArea.Tag.MOVE_TO_LINE_END);
            TextPos p2 = editor.getCaretPosition();
            if (p2 != null) {
                String text = editor.getPlainText(p2.index());
                int ix = findLastText(text, p2.charIndex());
                if (ix > p.charIndex()) {
                    editor.select(TextPos.ofLeading(p2.index(), ix));
                }
            }
        }
    }

    private static int findLastText(String text, int start) {
        int i = start - 1;
        while (i >= 0) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) {
                return i + 1;
            }
            --i;
        }
        return i;
    }
}

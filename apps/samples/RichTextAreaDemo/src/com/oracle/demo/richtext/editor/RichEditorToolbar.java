/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates.
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
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import com.oracle.demo.richtext.common.TextStyle;
import com.oracle.demo.richtext.util.FX;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Rich editor toolbar.
 *
 * @author Andy Goryachev
 */
public class RichEditorToolbar extends BorderPane {
    public final ComboBox<String> fontFamily = new ComboBox<>();
    public final ComboBox<Double> fontSize = new ComboBox<>();
    public final ColorPicker textColor = new ColorPicker();
    public final ComboBox<TextStyle> textStyle = new ComboBox<>();
    public final ToggleButton bold;
    public final ToggleButton italic;
    public final ToggleButton strikeThrough;
    public final ToggleButton underline;
    public final Button paragraphButton;
    public final ToggleButton lineNumbers;
    public final ToggleButton wrapText;

    public RichEditorToolbar() {
        FX.name(this, "RichEditorToolbar");
        setStyle("-fx-spacing: 1px;");
        setPadding(new Insets(2, 2, 2, 2));

        fontFamily.getItems().setAll(collectFonts());
        fontFamily.setMaxWidth(170);
        fontFamily.setPrefWidth(170);

        fontSize.setEditable(true);
        fontSize.setMaxWidth(70);
        fontSize.setPrefWidth(70);
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

        // TODO save/restore custom colors
        FX.tooltip(textColor, "Text Color");
        // there is no API for this!  why is this a property of a skin, not the control??
        // https://stackoverflow.com/questions/21246137/remove-text-from-colour-picker
        textColor.setStyle("-fx-color-label-visible: false;");
        textColor.setMaxHeight(Double.MAX_VALUE);

        textStyle.getItems().setAll(TextStyle.values());
        textStyle.setConverter(TextStyle.converter());

        ToolBar toolbar = new ToolBar();
        FX.add(toolbar, fontFamily);
        FX.add(toolbar, fontSize);
        FX.add(toolbar, textColor);
        FX.space(toolbar);
        // TODO background
        // TODO alignment
        // TODO bullet
        // TODO space left (indent left, indent right)
        // TODO line spacing
        bold = FX.toggleButton(toolbar, "𝐁", "Bold text");
        italic = FX.toggleButton(toolbar, "𝐼", "Bold text");
        strikeThrough = FX.toggleButton(toolbar, "S\u0336", "Strike through text");
        underline = FX.toggleButton(toolbar, "U\u0332", "Underline text");
        FX.add(toolbar, textStyle);
        paragraphButton = FX.button(toolbar, "P", "Paragraph Styles", null);
        FX.space(toolbar);
        lineNumbers = FX.toggleButton(toolbar, "N", "Line Numbers");
        wrapText = FX.toggleButton(toolbar, "W", "Wrap Text");

        FX.name(lineNumbers, "lineNumbers");
        FX.name(wrapText, "wrapText");

        setCenter(toolbar);
    }

    private static List<String> collectFonts() {
        return Font.getFamilies();
    }

    public void setTextStyle(TextStyle v) {
        textStyle.setValue(v);
    }

    private static boolean getBoolean(StyleAttributeMap m, StyleAttribute<Boolean> a) {
        Boolean v = m.getBoolean(a);
        return v == null ? false : v.booleanValue();
    }

    public void updateStyles(StyleAttributeMap a) {
        bold.setSelected(getBoolean(a, StyleAttributeMap.BOLD));
        italic.setSelected(getBoolean(a, StyleAttributeMap.ITALIC));
        underline.setSelected(getBoolean(a, StyleAttributeMap.UNDERLINE));
        strikeThrough.setSelected(getBoolean(a, StyleAttributeMap.STRIKE_THROUGH));
        FX.select(fontFamily, a.getFontFamily());
        FX.select(fontSize, a.getFontSize());
        textColor.setValue(a.getTextColor());
    }
}

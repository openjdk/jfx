/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
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

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.oracle.demo.richtext.util.FX;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.ParagraphDirection;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Rich Editor Demo paragraph formatting dialog.
 *
 * @author Andy Goryachev
 */
public class ParagraphDialog extends Stage {
    private final RichTextArea editor;
    private final ColorPicker background;
    private final ComboBox<TextAlignment> alignment;
    private final ComboBox<String> bullet;
    private final RadioButton ltrButton;
    private final RadioButton rtlButton;
    private final ComboBox<Double> beforeText;
    private final ComboBox<Double> afterText;
    private final ComboBox<Double> spaceAbove;
    private final ComboBox<Double> spaceBelow;
    // TODO FIRST_LINE_INDENT (* broken)

    public ParagraphDialog(RichTextArea editor) {
        this.editor = editor;
        initOwner(FX.getParentWindow(editor));
        initModality(Modality.APPLICATION_MODAL);

        // FIX cannot set <null> value
        background = new ColorPicker();
        background.setStyle("-fx-color-label-visible: false;");
        background.setValue(null);

        alignment = new ComboBox<>();
        alignment.getItems().setAll(TextAlignment.values());
        alignment.setConverter(FX.converter(this::toString));

        bullet = new ComboBox<>();
        bullet.setEditable(true);
        bullet.getItems().setAll(
            null,
            "•",
            "○",
            "●",
            "☑",
            "☐",
            "⦾",
            "⦿",
            "◉",
            "‣"
        );

        beforeText = combo();
        afterText = combo();
        spaceAbove = combo();
        spaceBelow = combo();

        ltrButton = new RadioButton("Left-to-right");
        rtlButton = new RadioButton("Right-to-left");
        new ToggleGroup().getToggles().setAll(ltrButton, rtlButton);

        Button tabsButton = new Button("Tabs...");
        tabsButton.setDisable(true); // TODO
        ButtonBar.setButtonData(tabsButton, ButtonData.LEFT);

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction((_) -> hide());
        ButtonBar.setButtonData(cancelButton, ButtonData.CANCEL_CLOSE);

        Button okButton = new Button("OK");
        okButton.setOnAction((_) -> commit());
        okButton.setDefaultButton(true);
        ButtonBar.setButtonData(okButton, ButtonData.OK_DONE);

        GridPane g = new GridPane();
        g.getColumnConstraints().setAll(
            FX.cc().fixed(20),
            FX.cc(),
            FX.cc().fill()
        );
        g.setHgap(10);
        g.setVgap(10);
        g.setMaxWidth(Double.MAX_VALUE);
        int r = 0;
        g.add(heading("General"), 0, r, 3, 1);
        r++;
        g.add(new Label("Background:"), 1, r);
        g.add(background, 2, r);
        r++;
        g.add(new Label("Bullet:"), 1, r);
        g.add(bullet, 2, r);
        r++;
        g.add(new Label("Alignment:"), 1, r);
        g.add(alignment, 2, r);
        r++;
        g.add(new Label("Direction:"), 1, r);
        g.add(new HBox(10, ltrButton, rtlButton), 2, r);
        r++;
        g.add(separator(), 0, r, 3, 1);
        r++;
        g.add(heading("Indentation"), 0, r, 3, 1);
        r++;
        g.add(new Label("Before Text:"), 1, r);
        g.add(beforeText, 2, r);
        r++;
        g.add(new Label("After Text:"), 1, r);
        g.add(afterText, 2, r);
        r++;
        g.add(separator(), 0, r, 3, 1);
        r++;
        g.add(heading("Spacing"), 0, r, 3, 1);
        r++;
        g.add(new Label("Before:"), 1, r);
        g.add(spaceAbove, 2, r);
        r++;
        g.add(new Label("After:"), 1, r);
        g.add(spaceBelow, 2, r);

        ButtonBar bb = new ButtonBar();
        bb.getButtons().setAll(
            tabsButton,
            cancelButton,
            okButton
        );

        BorderPane bp = new BorderPane();
        bp.setPadding(new Insets(10));
        bp.setCenter(g);
        bp.setBottom(bb);

        setScene(new Scene(bp, 400, 500));
        setTitle("Paragraph");
        addEventHandler(KeyEvent.KEY_PRESSED, (ev) -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                hide();
            }
        });

        // TODO center in window

        load();
    }

    private String toString(TextAlignment a) {
        if (a == null) {
            return "";
        }
        return switch (a) {
        case CENTER -> "Center";
        case JUSTIFY -> "Justify";
        case LEFT -> "Left";
        case RIGHT -> "Right";
        };
    }

    private static Label heading(String text) {
        Label n = new Label(text);
        n.setStyle("-fx-font-weight:bold;");
        n.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(n, Priority.ALWAYS);
        return n;
    }

    private static Separator separator() {
        Separator n = new Separator();
        n.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(n, Priority.ALWAYS);
        return n;
    }

    private static ComboBox<Double> combo() {
        ComboBox<Double> c = new ComboBox<>();
        c.getItems().setAll(
            null,
            0.0,
            10.0,
            50.0,
            100.0
        );
        c.setEditable(true);
        c.setConverter(FX.numberConverter());
        return c;
    }

    private void load() {
        StyleAttributeMap a = editor.getActiveStyleAttributeMap();

        background.setValue(a.getBackground());
        bullet.setValue(a.getBullet());

        FX.select(alignment, a.getTextAlignment(), TextAlignment.LEFT);

        ParagraphDirection dir = a.getParagraphDirection();
        if(dir == null) {
            dir = ParagraphDirection.LEFT_TO_RIGHT;
        }
        ltrButton.setSelected(dir == ParagraphDirection.LEFT_TO_RIGHT);
        rtlButton.setSelected(dir == ParagraphDirection.RIGHT_TO_LEFT);
        beforeText.setValue(a.getSpaceLeft());
        afterText.setValue(a.getSpaceRight());
        spaceAbove.setValue(a.getSpaceAbove());
        spaceBelow.setValue(a.getSpaceBelow());
    }

    private StyleAttributeMap getAttributes() {
        StyleAttributeMap.Builder b = StyleAttributeMap.builder();
        b.setBackground(background.getValue());
        b.setBullet(bullet.getValue());
        b.setParagraphDirection(rtlButton.isSelected() ? ParagraphDirection.RIGHT_TO_LEFT : ParagraphDirection.LEFT_TO_RIGHT);
        b.setTextAlignment(alignment.getSelectionModel().getSelectedItem());
        // perhaps Builder.setXXX(double) should accept a Double instead
        b.set(StyleAttributeMap.SPACE_LEFT, beforeText.getValue());
        b.set(StyleAttributeMap.SPACE_RIGHT, afterText.getValue());
        b.set(StyleAttributeMap.SPACE_ABOVE, spaceAbove.getValue());
        b.set(StyleAttributeMap.SPACE_BELOW, spaceBelow.getValue());
        return b.build();
    }

    private void commit() {
        TextPos p = editor.getCaretPosition();
        if (p == null) {
            // TODO default attributes?
        } else {
            StyleAttributeMap a = getAttributes();
            editor.applyStyle(p, p, a);
        }
        hide();
    }
}

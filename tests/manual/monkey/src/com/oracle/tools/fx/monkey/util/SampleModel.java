/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.model.RichTextFormatHandler;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;

/**
 * Sample model for the RichTextArea property sheet.
 */
public class SampleModel extends SimpleViewOnlyStyledModel {
    public SampleModel() {
        // Style names: see MainWindow.stylesheet()
        String BOLD = "bold";
        String CODE = "code";
        String GRAY = "gray";
        String GREEN = "green";
        String ITALIC = "italic";
        String LARGE = "large";
        String RED = "red";
        String STRIKETHROUGH = "strikethrough";
        String UNDERLINE = "underline";

        addWithInlineAndStyleNames("Read-Only Model", "-fx-font-size:200%;", UNDERLINE);
        nl(2);

        addWithStyleNames("/**", RED, CODE);
        nl();
        addWithStyleNames(" * Syntax Highlight Demo.", RED, CODE);
        nl();
        addWithStyleNames(" */", RED, CODE);
        nl();
        addWithStyleNames("public class ", GREEN, CODE);
        addWithStyleNames("SyntaxHighlightDemo ", CODE);
        addWithStyleNames("extends ", GREEN, CODE);
        addWithStyleNames("Application {", CODE);
        nl();
        addWithStyleNames("\tpublic static void", GREEN, CODE);
        addWithStyleNames(" main(String[] args) {", CODE);
        nl();
        addWithStyleNames("\t\tApplication.launch(SyntaxHighlightDemo.", CODE);
        addWithStyleNames("class", CODE, GREEN);
        addWithStyleNames(", args);", CODE);
        nl();
        addWithStyleNames("\t}", CODE);
        nl();
        addWithStyleNames("}", CODE);
        nl(2);
        // font attributes
        addWithStyleNames("BOLD ", BOLD);
        addWithStyleNames("ITALIC ", ITALIC);
        addWithStyleNames("STRIKETHROUGH ", STRIKETHROUGH);
        addWithStyleNames("UNDERLINE ", UNDERLINE);
        addWithStyleNames("ALL OF THEM ", BOLD, ITALIC, STRIKETHROUGH, UNDERLINE);
        nl(2);
        // inline nodes
        addSegment("Inline Nodes:  ");
        addNodeSegment(() -> {
            TextField f = new TextField();
            f.setPrefColumnCount(20);
            return f;
        });
        addSegment(" ");
        addNodeSegment(() -> new Button("OK"));
        addSegment(" ");
        nl(2);
        addWithInlineStyle("ABCDEFGHIJKLMNO", "-fx-font-family:monospaced;").nl();
        addWithStyleNames("        leading and trailing whitespace         ", CODE).nl();
        nl(2);
        addWithStyleNames("Various highlights, some overlapping.", LARGE);
        highlight(8, 10, Color.rgb(255, 255, 128, 0.7));
        highlight(12, 12, Color.rgb(0, 0, 128, 0.1));
        addWavyUnderline(25, 100, Color.RED);
        nl(2);
        addSegment("Styled with CSS");
        addWavyUnderline(0, 6, "squiggly-css");
        highlight(12, 3, "highlight1", "highlight2");
        nl(2);
        addSegment("Paragraph Node:");
        addParagraph(JumpingLabel::new);
        nl(2);
        addSegment("Trailing node: ");
        addNodeSegment(JumpingLabel::new);
        // one after another
        addParagraph(JumpingLabel::new);
        addNodeSegment(JumpingLabel::new);

        // rich text data handler
        registerDataFormatHandler(RichTextFormatHandler.getInstance(), true, false, 2000);
    }

    // This Label resizes itself in responds to mouse clicks.
    static class JumpingLabel extends Label {
        public JumpingLabel() {
            String text = "(click me)";
            setText(text);
            setBackground(Background.fill(new Color(1.0, 0.627451, 0.47843137, 0.5)));
            setOnMouseClicked((_) -> {
                if (text.equals(getText())) {
                    setMinWidth(200);
                    setMinHeight(100);
                    setText("(click me again)");
                } else {
                    setMinWidth(Label.USE_PREF_SIZE);
                    setText(text);
                    setMinHeight(Label.USE_PREF_SIZE);
                }
            });
        }
    }
}
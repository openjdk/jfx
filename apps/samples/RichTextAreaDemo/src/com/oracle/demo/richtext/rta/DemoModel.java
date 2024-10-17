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

package com.oracle.demo.richtext.rta;
import java.util.Arrays;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.model.RichTextFormatHandler;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;

/**
 * RichTextArea demo model.
 *
 * @author Andy Goryachev
 */
public class DemoModel extends SimpleViewOnlyStyledModel {
    private final SimpleStringProperty textField = new SimpleStringProperty();

    public DemoModel() {
        // see RichTextAreaDemo.css
        String ARABIC = "arabic";
        String CODE = "code";
        String RED = "red";
        String GREEN = "green";
        String GRAY = "gray";
        String LARGE = "large";
        String BOLD = "bold";
        String ITALIC = "italic";
        String STRIKETHROUGH = "strikethrough";
        String UNDERLINE = "underline";

        addWithInlineAndStyleNames("RichTextArea Control", "-fx-font-size:200%;", UNDERLINE);
        nl(2);

//        addParagraph(() -> {
//            Region r = new Region();
//            r.getchi
//            r.setw 300, 50);
//            r.setFill(Color.RED);
//            return r;
//        });

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
        addSegment("Inline Nodes: ");
        addNodeSegment(() -> {
            TextField f = new TextField();
            f.setPrefColumnCount(20);
            f.textProperty().bindBidirectional(textField);
            return f;
        });
        addSegment(" ");
        addNodeSegment(() -> new Button("OK"));
        addSegment(" "); // FIX cannot navigate over this segment
        nl(2);
        addWithStyleNames("A regular Arabic verb, كَتَبَ‎ kataba (to write).", ARABIC).nl();
        addWithStyleNames("Emojis: [🔥🦋😀😃😄😁😆😅🤣😂🙂🙃😉😊😇]", LARGE).nl();
        nl();
        addWithStyleNames("Halfwidth and FullWidth Forms", UNDERLINE).nl();
        addWithInlineStyle("ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯ", "-fx-font-family:monospaced;").nl();
        addWithInlineStyle("ABCDEFGHIJKLMNO", "-fx-font-family:monospaced;").nl();
        addWithStyleNames("        leading and trailing whitespace         ", CODE).nl();
        nl(3);
        addWithStyleNames("Behold various types of highlights, including overlapping highlights.", LARGE);
        highlight(7, 7, Color.rgb(255, 255, 128, 0.7));
        squiggly(36, 100, Color.RED);
        highlight(46, 11, Color.rgb(255, 255, 128, 0.7));
        highlight(50, 20, Color.rgb(0, 0, 128, 0.1));
        nl(2);
        addSegment("Behold various types of highlights, including overlapping highlights.");
        highlight(7, 7, Color.rgb(255, 255, 128, 0.7));
        squiggly(36, 100, Color.RED);
        highlight(46, 11, Color.rgb(255, 255, 128, 0.7));
        highlight(50, 20, Color.rgb(0, 0, 128, 0.1));
        nl(2);

        // FIX adding a control messes up the view with text wrap off
//        addParagraph(() -> {
//            TextField t = new TextField("yo");
//            t.setMaxWidth(100);
//            return t;
//        });
//        nl(2);

        addParagraph(this::createRect);
        nl(2);

        ParagraphAttributesDemoModel.insert(this);

        addImage(DemoModel.class.getResourceAsStream("animated.gif"));
        addWithStyleNames("  Fig. 1 Embedded animated GIF image.", GRAY, ITALIC);
        nl(2);

        /*
        Random r = new Random();
        for(int line=0; line<100; line++) {
            int ct = r.nextInt(10);
            for (int word = 0; word < ct; word++) {
                int len = 1 + r.nextInt(7);
                char c = '*';

                if (word == 0) {
                    addSegment("L" + (size() + 1), null, GRAY);
                }

                addSegment(" ");

                if (r.nextFloat() < 0.1) {
                    addSegment(word + "." + word(c, len), null, RED);
                } else {
                    addSegment(word + "." + word(c, len));
                }
            }
            nl();
        }
        */

        nl();
        addWithInlineStyle("\t\t終 The End.", "-fx-font-size:200%;");
        nl();

        registerDataFormatHandler(RichTextFormatHandler.INSTANCE, true, false, 2000);
    }

    private Region createRect() {
        Label t = new Label() {
            @Override
            protected double computePrefHeight(double w) {
                return 400;
            }
        };
        t.setPrefSize(400, 200);
        t.setMaxWidth(400);
        t.textProperty().bind(Bindings.createObjectBinding(
            () -> {
                return String.format("%.1f x %.1f", t.getWidth(), t.getHeight());
            },
            t.widthProperty(),
            t.heightProperty()
        ));
        t.setBackground(Background.fill(Color.LIGHTGRAY));

        BorderPane p = new BorderPane();
        p.setLeft(t);
        return p;
    }

    private String word(char c, int len) {
        char[] cs = new char[len];
        Arrays.fill(cs, c);
        return new String(cs);
    }
}

/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

        addSegment("RichTextArea Control", "-fx-font-size:200%;", UNDERLINE);
        nl(2);

//        addParagraph(() -> {
//            Region r = new Region();
//            r.getchi
//            r.setw 300, 50);
//            r.setFill(Color.RED);
//            return r;
//        });

        addSegment("/**", null, RED, CODE);
        nl();
        addSegment(" * Syntax Highlight Demo.", null, RED, CODE);
        nl();
        addSegment(" */", null, RED, CODE);
        nl();
        addSegment("public class ", null, GREEN, CODE);
        addSegment("SyntaxHighlightDemo ", null, CODE);
        addSegment("extends ", null, GREEN, CODE);
        addSegment("Application {", null, CODE);
        nl();
        addSegment("\tpublic static void", null, GREEN, CODE);
        addSegment(" main(String[] args) {", null, CODE);
        nl();
        addSegment("\t\tApplication.launch(SyntaxHighlightDemo.", null, CODE);
        addSegment("class", null, CODE, GREEN);
        addSegment(", args);", null, CODE);
        nl();
        addSegment("\t}", null, CODE);
        nl();
        addSegment("}", null, CODE);
        nl(2);
        // font attributes
        addSegment("BOLD ", null, BOLD);
        addSegment("ITALIC ", null, ITALIC);
        addSegment("STRIKETHROUGH ", null, STRIKETHROUGH);
        addSegment("UNDERLINE ", null, UNDERLINE);
        addSegment("ALL OF THEM ", null, BOLD, ITALIC, STRIKETHROUGH, UNDERLINE);
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
        addSegment("A regular Arabic verb, كَتَبَ‎ kataba (to write).", null, ARABIC).nl();
        addSegment("Emojis: [🔥🦋😀😃😄😁😆😅🤣😂🙂🙃😉😊😇]", null, LARGE).nl();
        nl();
        addSegment("Halfwidth and FullWidth Forms", null, UNDERLINE).nl();
        addSegment("ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯ", "-fx-font-family:monospaced;").nl();
        addSegment("ABCDEFGHIJKLMNO", "-fx-font-family:monospaced;").nl();
        addSegment("        leading and trailing whitespace         ", null, CODE).nl();
        nl(3);
        addSegment("Behold various types of highlights, including overlapping highlights.", null, LARGE);
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
        addSegment("  Fig. 1 Embedded animated GIF image.", null, GRAY, ITALIC);
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
        addSegment("\t\t終 The End.", "-fx-font-size:200%;");
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

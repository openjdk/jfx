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

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.oracle.demo.richtext.util.FX;
import jfx.incubator.scene.control.richtext.LineNumberDecorator;
import jfx.incubator.scene.control.richtext.RichTextArea;

/**
 * Test Window that stacks multiple RichTextAreas and other components either vertically or horizontally.
 *
 * @author Andy Goryachev
 */
public class MultipleStackedBoxWindow extends Stage {

    public MultipleStackedBoxWindow(boolean vertical) {
        RichTextArea a1 = new RichTextArea(NotebookModelStacked.m1());
        a1.setHighlightCurrentParagraph(true);
        a1.setWrapText(true);
        a1.setLeftDecorator(new LineNumberDecorator());
        createPopupMenu(a1);

        TextArea t1 = new TextArea("This TextArea has wrap text property set to false.");
        t1.setPrefHeight(50);

        Label t2 = new Label("Label");

        RichTextArea a2 = new RichTextArea(NotebookModelStacked.m2());
        a2.setHighlightCurrentParagraph(true);
        a2.setWrapText(true);
        a2.setLeftDecorator(new LineNumberDecorator());
        createPopupMenu(a2);

        PrefSizeTester tester = new PrefSizeTester();

        ScrollPane sp = new ScrollPane();

        if (vertical) {
            a1.setUseContentHeight(true);
            a2.setUseContentHeight(true);

            VBox vb = new VBox(
                a1,
                t1,
                a2,
                t2,
                tester
            );
            sp.setContent(vb);
            sp.setFitToWidth(true);

            setTitle("Test Vertical Stack");
            setWidth(600);
            setHeight(1200);
            FX.name(this, "VerticalStack");
        } else {
            a1.setUseContentWidth(true);
            a2.setUseContentWidth(true);

            HBox hb = new HBox(
                a1,
                t1,
                a2,
                t2,
                tester
            );
            sp.setContent(hb);
            sp.setFitToHeight(true);

            setTitle("Test Horizontal Stack");
            setWidth(1200);
            setHeight(600);
            FX.name(this, "HorizontalStack");
        }

        Scene scene = new Scene(sp);
        scene.getStylesheets().addAll(
            RichTextAreaWindow.class.getResource("RichTextAreaDemo.css").toExternalForm()
        );
        setScene(scene);
    }

    protected void createPopupMenu(RichTextArea t) {
        FX.setPopupMenu(t, () -> {
            Menu m;
            ContextMenu c = new ContextMenu();
            // left side
            m = FX.menu(c, "Left Side");
            FX.checkItem(m, "null", t.getLeftDecorator() == null, (on) -> {
                if (on) {
                    t.setLeftDecorator(null);
                }
            });
            FX.checkItem(m, "Line Numbers", t.getLeftDecorator() instanceof LineNumberDecorator, (on) -> {
                if (on) {
                    t.setLeftDecorator(new LineNumberDecorator());
                }
            });
            FX.checkItem(m, "Colors", t.getLeftDecorator() instanceof DemoColorSideDecorator, (on) -> {
                if (on) {
                    t.setLeftDecorator(new DemoColorSideDecorator());
                }
            });
            // right side
            m = FX.menu(c, "Right Side");
            FX.checkItem(m, "null", t.getRightDecorator() == null, (on) -> {
                if (on) {
                    t.setRightDecorator(null);
                }
            });
            FX.checkItem(m, "Line Numbers", t.getRightDecorator() instanceof LineNumberDecorator, (on) -> {
                if (on) {
                    t.setRightDecorator(new LineNumberDecorator());
                }
            });
            FX.checkItem(m, "Colors", t.getRightDecorator() instanceof DemoColorSideDecorator, (on) -> {
                if (on) {
                    t.setRightDecorator(new DemoColorSideDecorator());
                }
            });
            // content padding
            m = FX.menu(c, "Content Padding");
            FX.checkItem(m, "null", t.getContentPadding() == null, (on) -> {
                if (on) {
                    t.setContentPadding(null);
                }
            });
            FX.checkItem(m, "1", new Insets(1).equals(t.getContentPadding()), (on) -> {
                if (on) {
                    t.setContentPadding(new Insets(1));
                }
            });
            FX.checkItem(m, "2", new Insets(1).equals(t.getContentPadding()), (on) -> {
                if (on) {
                    t.setContentPadding(new Insets(2));
                }
            });
            FX.checkItem(m, "10", new Insets(10).equals(t.getContentPadding()), (on) -> {
                if (on) {
                    t.setContentPadding(new Insets(10));
                }
            });
            FX.checkItem(m, "55.75", new Insets(55.75).equals(t.getContentPadding()), (on) -> {
                if (on) {
                    t.setContentPadding(new Insets(55.75));
                }
            });

            FX.checkItem(c, "Wrap Text", t.isWrapText(), (on) -> t.setWrapText(on));
            return c;
        });
    }
}

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

package com.oracle.demo.rich.rta;

import javafx.geometry.Insets;
import jfx.incubator.scene.control.rich.RichTextArea;
import jfx.incubator.scene.control.rich.skin.LineNumberDecorator;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.oracle.demo.rich.util.FX;

/**
 * Test Window that stacks multiple RichTextAreas and other components either vertically or horizontally.
 */
public class MultipleStackedBoxWindow extends Stage {

    public MultipleStackedBoxWindow(boolean vertical) {
        RichTextArea a1 = new RichTextArea(NotebookModelStacked.m1());
        a1.setWrapText(true);
        a1.setLeftDecorator(new LineNumberDecorator());
        createPopupMenu(a1);

        TextArea t1 = new TextArea("This TextArea has wrap text property set to false.");

        TextArea t2 = new TextArea("This TextArea has wrap text property set to true.");
        t2.setWrapText(true);

        RichTextArea a2 = new RichTextArea(NotebookModelStacked.m2());
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
            RichTextAreaWindow.class.getResource("RichTextArea-Modena.css").toExternalForm(),
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

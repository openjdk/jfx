/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.demo.rich;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.rich.RichTextArea;
import javafx.scene.control.rich.TextPos;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Rich Text Area Demo window
 */
public class RichTextAreaWindow extends Stage {
    public final RichTextAreaDemoPane demoPane;
    public final Label status;
    
    public RichTextAreaWindow(boolean useContentSize) {
        demoPane = new RichTextAreaDemoPane(useContentSize);
        
        MenuBar mb = new MenuBar();
        // file
        FX.menu(mb, "File");
        FX.item(mb, "New Window", () -> newWindow(false));
        FX.item(mb, "New Window, Use Content Size", () -> newWindow(true));
        FX.separator(mb);
        FX.item(mb, "Close Window", this::hide);
        FX.separator(mb);
        FX.item(mb, "Quit", () -> Platform.exit());
        // tests
        FX.menu(mb, "Tests");
        FX.item(mb, "Stacked Vertically", () -> openMultipeStacked(true));
        FX.item(mb, "Stacked Horizontally", () -> openMultipeStacked(false));

        status = new Label();
        status.setPadding(new Insets(2, 10, 2, 10));
        
        BorderPane bp = new BorderPane();
        bp.setTop(mb);
        bp.setCenter(demoPane);
        bp.setBottom(status);
        
        Scene scene = new Scene(bp);
        scene.getStylesheets().add(RichTextAreaWindow.class.getResource("RichTextAreaDemo.css").toExternalForm());

        setScene(scene);
        setTitle(
            "RichTextArea Demo FX:" +
            System.getProperty("javafx.runtime.version") +
            "  JDK:" +
            System.getProperty("java.version")
        );
        setWidth(1200);
        setHeight(600);

        demoPane.control.caretPositionProperty().addListener((x) -> updateStatus());
    }

    protected void updateStatus() {
        RichTextArea t = demoPane.control;
        TextPos p = t.getCaretPosition();

        StringBuilder sb = new StringBuilder();

        if (p != null) {
            sb.append(" line=").append(p.index());
            sb.append(" col=").append(p.offset());
        }

        status.setText(sb.toString());
    }

    protected void newWindow(boolean useContentSize) {
        double offset = 20;

        RichTextAreaWindow w = new RichTextAreaWindow(useContentSize);
        w.setX(getX() + offset);
        w.setY(getY() + offset);
        w.setWidth(getWidth());
        w.setHeight(getHeight());
        w.show();
    }
    
    protected void openMultipeStacked(boolean vertical) {
        new MultipleStackedBoxWindow(vertical).show();
    }
}

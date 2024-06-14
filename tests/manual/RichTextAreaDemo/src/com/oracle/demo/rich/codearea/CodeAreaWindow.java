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
package com.oracle.demo.rich.codearea;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.oracle.demo.rich.rta.RichTextAreaWindow;
import com.oracle.demo.rich.util.FX;
import jfx.incubator.scene.control.rich.RichTextArea;
import jfx.incubator.scene.control.rich.TextPos;
import jfx.incubator.scene.control.rich.model.CodeTextModel;

/**
 * CodeArea Demo window
 */
public class CodeAreaWindow extends Stage {
    private CodeTextModel model;
    public final CodeAreaDemoPane demoPane;
    public final Label status;

    public CodeAreaWindow(CodeTextModel m) {
        model = (m == null ? new CodeTextModel() : m);
        demoPane = new CodeAreaDemoPane(model);

        MenuBar mb = new MenuBar();
        FX.menu(mb, "File");
        FX.item(mb, "New Window", this::newWindow);
        FX.separator(mb);
        FX.item(mb, "Close Window", this::hide);
        FX.separator(mb);
        FX.item(mb, "Quit", () -> Platform.exit());

        status = new Label();
        status.setPadding(new Insets(2, 10, 2, 10));

        BorderPane bp = new BorderPane();
        bp.setTop(mb);
        bp.setCenter(demoPane);
        bp.setBottom(status);

        Scene scene = new Scene(bp);
        setScene(scene);
        setTitle(
            "CodeArea Tester  JFX:" + System.getProperty("javafx.runtime.version") +
            "  JDK:" + System.getProperty("java.version")
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

    protected void newWindow() {
        double offset = 20;

        CodeAreaWindow w = new CodeAreaWindow(model);
        w.setX(getX() + offset);
        w.setY(getY() + offset);
        w.setWidth(getWidth());
        w.setHeight(getHeight());
        w.show();
    }
}

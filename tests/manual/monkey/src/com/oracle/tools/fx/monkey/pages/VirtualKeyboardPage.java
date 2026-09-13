/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.pages;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Virtual keyboard test page.
 */
public class VirtualKeyboardPage extends TestPaneBase {

    // FXVK:59
    private final static String VK_TYPE_PROP_KEY = "vkType";

    public VirtualKeyboardPage() {
        super("VirtualKeyboardPage");

        TextArea info = new TextArea("""
            The FX virtual keyboard must be enabled by adding the following command line argument:

            -Dcom.sun.javafx.virtualKeyboard=javafx
            """);
        info.setWrapText(true);
        info.setEditable(false);

        ColumnConstraints c0 = new ColumnConstraints();

        ColumnConstraints cFill = new ColumnConstraints();
        cFill.setHgrow(Priority.ALWAYS);

        RowConstraints r0 = new RowConstraints();
        r0.setVgrow(Priority.SOMETIMES);

        RowConstraints rFill = new RowConstraints();
        rFill.setVgrow(Priority.ALWAYS);

        GridPane p = new GridPane();
        p.setPadding(new Insets(10));
        p.setHgap(10);
        p.setVgap(10);
        p.getColumnConstraints().setAll(c0, c0, cFill);
        p.getRowConstraints().setAll(r0, r0, r0, r0, rFill);
        p.add(new Label("Text:"), 0, 0);
        p.add(create(0), 1, 0);
        p.add(new Label("Numeric:"), 0, 1);
        p.add(create(1), 1, 1);
        p.add(new Label("URL:"), 0, 2);
        p.add(create(2), 1, 2);
        p.add(new Label("Email:"), 0, 3);
        p.add(create(3), 1, 3);
        p.add(info, 0, 4, 3, 1);

        setContent(p);
    }

    private static TextField create(int type) {
        TextField t = new TextField();
        t.getProperties().put(VK_TYPE_PROP_KEY, type);
        return t;
    }
}

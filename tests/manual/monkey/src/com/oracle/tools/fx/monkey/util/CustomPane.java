/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Custom Pane with Typical Controls.
 */
public class CustomPane extends GridPane {
    private CustomPane(boolean innerScroll) {
        super(10, 5);

        TextField loginField = new TextField();
        PasswordField passField = new PasswordField();
        Button button = new Button("Login");
        RadioButton rb1 = new RadioButton("1");
        RadioButton rb2 = new RadioButton("2");
        RadioButton rb3 = new RadioButton("3");
        new ToggleGroup().getToggles().addAll(rb1, rb2, rb3);
        HBox spacer = new HBox(10, rb1, rb2, rb3, new Hyperlink("hyperlink"));

        setPadding(new Insets(20));
        int r = 0;
        add(new Label("Login:"), 0, r);
        add(loginField, 1, r, 2, 1);
        r++;
        add(new Label("Password:"), 0, r);
        add(passField, 1, r, 2, 1);
        r++;
        add(spacer, 1, r);
        add(button, 2, r);
        if (innerScroll) {
            r++;
            add(new ScrollPane(new CustomPane(false)), 1, r, 2, 2);
        }

        GridPane.setHgrow(loginField, Priority.ALWAYS);
        GridPane.setHgrow(passField, Priority.ALWAYS);
        GridPane.setHgrow(spacer, Priority.ALWAYS);
    }

    public static CustomPane create() {
        return new CustomPane(true);
    }
}

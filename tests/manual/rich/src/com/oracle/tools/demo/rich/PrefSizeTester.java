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

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class PrefSizeTester extends Pane {
    private final ComboBox prefWidth;
    private final ComboBox prefHeight;
    private final GridPane p;
    
    public PrefSizeTester() {
        setBackground(Background.fill(Color.LIGHTSTEELBLUE));

        prefWidth = new ComboBox();
        prefWidth.getItems().addAll(
            -1.0,
            100.0,
            200.0,
            300.0
        );
        prefWidth.setOnAction((ev) -> {
            updateWidth();
        });

        prefHeight = new ComboBox();
        prefHeight.getItems().addAll(
            -1.0,
            100.0,
            200.0,
            300.0
        );
        prefHeight.setOnAction((ev) -> {
            updateHeight();
        });

        p = new GridPane();
        p.add(new Label("Pref Width:"), 0, 0);
        p.add(prefWidth, 1, 0);
        p.add(new Label("Pref Height:"), 0, 1);
        p.add(prefHeight, 1, 1);

        getChildren().add(p);
        //setCenter(p);
    }

    private void updateWidth() {
        if (prefWidth.getValue() instanceof Number n) {
            double w = n.doubleValue();
            p.setPrefWidth(w);
        }
    }

    private void updateHeight() {
        if (prefHeight.getValue() instanceof Number n) {
            double h = n.doubleValue();
            p.setPrefHeight(h);
        }
    }
}

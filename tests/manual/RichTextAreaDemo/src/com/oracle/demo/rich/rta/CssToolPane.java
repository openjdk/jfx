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

import java.nio.charset.Charset;
import java.util.Base64;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import com.oracle.demo.rich.util.FX;

/**
 * CSS Tool
 */
public class CssToolPane extends BorderPane {
    private final TextArea cssField;
    private static String oldStylesheet;

    public CssToolPane() {
        cssField = new TextArea();
        cssField.setId("CssPlaygroundPaneCss");
        cssField.setMaxWidth(Double.POSITIVE_INFINITY);
        cssField.setMaxHeight(Double.POSITIVE_INFINITY);

        Button updateButton = FX.button("Update", this::update);

        // why can't I fill the width of the container with this grid pane??
        GridPane p = new GridPane();
        p.setPadding(new Insets(10));
        p.setHgap(5);
        p.setVgap(5);
        int r = 0;
        p.add(new Label("Custom CSS:"), 0, r);
        r++;
        p.add(cssField, 0, r, 3, 1);
        r++;
        p.add(updateButton, 2, r);
        GridPane.setHgrow(cssField, Priority.ALWAYS);
        GridPane.setVgrow(cssField, Priority.ALWAYS);

        setCenter(p);
    }

    private void update() {
        String css = cssField.getText();
        applyStyleSheet(css);
    }

    private static String toCssColor(Color c) {
        int r = toInt8(c.getRed());
        int g = toInt8(c.getGreen());
        int b = toInt8(c.getBlue());
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static int toInt8(double x) {
        int v = (int)Math.round(x * 255);
        if (v < 0) {
            return 0;
        } else if (v > 255) {
            return 255;
        }
        return v;
    }

    private static String encode(String s) {
        if (s == null) {
            return null;
        }
        Charset utf8 = Charset.forName("utf-8");
        byte[] b = s.getBytes(utf8);
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(b);
    }

    private static void applyStyleSheet(String styleSheet) {
        String ss = encode(styleSheet);
        if (ss != null) {
            for (Window w : Window.getWindows()) {
                Scene scene = w.getScene();
                if (scene != null) {
                    ObservableList<String> sheets = scene.getStylesheets();
                    if (oldStylesheet != null) {
                        sheets.remove(oldStylesheet);
                    }
                    sheets.add(ss);
                }
            }
        }
        oldStylesheet = ss;
    }
}

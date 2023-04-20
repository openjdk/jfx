/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * ColorPicker Page
 */
public class ColorPickerPage extends TestPaneBase {
    private final Button button;
    private ColorPicker picker1;
    private ColorPicker picker2;
    private Alert dialog;

    public ColorPickerPage() {
        setId("ColorPickerPage");

        button = new Button("Show in Alert");

        picker1 = new ColorPicker(Color.BLUE);
        picker1.valueProperty().addListener(event -> {
            dialog.close();
        });

        button.setOnAction(event -> {
            Point2D p = button.localToScreen(0, button.getHeight());

            dialog = new Alert(AlertType.INFORMATION);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.initOwner(getWindow());
            dialog.getDialogPane().setContent(picker1);
            dialog.setX(p.getX());
            dialog.setY(p.getY());
            dialog.show();

            Object v = picker1.getValue();
            System.out.println(v);
        });

        picker2 = new ColorPicker(Color.YELLOW);
        picker2.setOnAction((ev) -> {
            Object v = picker2.getValue();
            System.out.println(v);
        });

        OptionPane p = new OptionPane();
        p.option(button);

        setContent(picker2);
        setOptions(p);
    }
}

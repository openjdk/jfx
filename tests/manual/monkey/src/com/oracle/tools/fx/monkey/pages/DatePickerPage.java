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

import java.time.LocalDate;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.stage.StageStyle;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * DatePicker Page
 */
public class DatePickerPage extends TestPaneBase {
    private final Button button;
    private DatePicker datePicker;
    private DatePicker datePicker2;
    private Alert dialog;

    public DatePickerPage() {
        FX.name(this, "DatePickerPage");

        button = new Button("Show in Alert");

        datePicker = new DatePicker(LocalDate.now());
        datePicker.valueProperty().addListener(event -> {
            dialog.close();
        });

        button.setOnAction(event -> {
            Point2D p = button.localToScreen(0, button.getHeight());

            dialog = new Alert(AlertType.INFORMATION);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.initOwner(getWindow());
            dialog.getDialogPane().setContent(datePicker);
            dialog.setX(p.getX());
            dialog.setY(p.getY());
            dialog.show();

            LocalDate v = datePicker.getValue();
            System.out.println(v);
        });

        datePicker2 = new DatePicker(LocalDate.now());

        OptionPane p = new OptionPane();
        p.option(button);

        setContent(datePicker2);
        setOptions(p);
    }
}

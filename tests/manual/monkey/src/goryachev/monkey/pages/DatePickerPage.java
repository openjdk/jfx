/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package goryachev.monkey.pages;

import java.time.LocalDate;
import goryachev.monkey.util.TestPaneBase;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.stage.StageStyle;

/**
 *
 */
public class DatePickerPage extends TestPaneBase {
    private final Button button;
    private DatePicker datePicker;
    private Alert dialog;

    public DatePickerPage() {
        setId("DatePickerPage");

        button = new Button("Show Dialog");
        toolbar().add(button);

        datePicker = new DatePicker(LocalDate.now());
        datePicker.valueProperty().addListener(event -> {
            dialog.close();
        });

        button.setOnAction(event -> {
            dialog = new Alert(AlertType.INFORMATION);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.initOwner(getWindow());
            dialog.getDialogPane().setContent(datePicker);
            dialog.show();

            LocalDate v = datePicker.getValue();
            System.out.println(v);
        });
    }
}

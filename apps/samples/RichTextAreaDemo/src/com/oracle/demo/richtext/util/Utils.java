/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.util;

import java.text.DecimalFormat;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Miscellaneous utilities.
 *
 * @author Andy Goryachev
 */
public class Utils {

    private static final DecimalFormat FORMAT_2DP = new DecimalFormat("#0.##");

    private Utils() {
    }

    public static boolean isBlank(Object x) {
        if (x == null) {
            return true;
        }
        return (x.toString().trim().length() == 0);
    }

    public static String format2DP(Object v) {
        return FORMAT_2DP.format(v);
    }

    public static ComboBox<Double> numberField() {
        return numberField(
            null,
            0.0,
            10.0,
            50.0,
            100.0
        );
    }

    public static ComboBox<Double> numberField(Double... items) {
        ComboBox<Double> c = new ComboBox<>();
        c.getItems().setAll(items);
        c.setEditable(true);
        c.setConverter(FX.numberConverter());
        return c;
    }

    public static Label heading(String text) {
        Label n = new Label(text);
        n.setStyle("-fx-font-weight:bold;");
        n.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(n, Priority.ALWAYS);
        return n;
    }

    public static Separator separator() {
        Separator n = new Separator();
        n.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(n, Priority.ALWAYS);
        return n;
    }

    public static void closeOnEscape(Stage s) {
        s.addEventHandler(KeyEvent.KEY_PRESSED, (ev) -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                s.hide();
            }
        });
    }

    public static void centerInWindow(Stage s) {
        Window w = s.getOwner();
        if (w == null) {
            s.centerOnScreen();
        } else {
            // or use JDK-8372530
            Scene sc = s.getScene();
            double x = w.getX() + (w.getWidth() - sc.getWidth()) / 2.0;
            double y = w.getY() + (w.getHeight() - sc.getHeight()) / 2.0;
            s.setX(x);
            s.setY(y);
        }
    }
}

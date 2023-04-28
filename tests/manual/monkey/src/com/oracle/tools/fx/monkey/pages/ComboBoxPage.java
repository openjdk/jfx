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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * ComboBox Page
 */
public class ComboBoxPage extends TestPaneBase {
    private ComboBox control;

    public ComboBoxPage() {
        setId("ComboBoxPage");

        control = new ComboBox();
        control.getItems().setAll("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");

        VBox b = new VBox();
        b.getChildren().add(control);

        Button setConverterButton = new Button("Set Converter");
        setConverterButton.setOnAction((ev) -> {
            control.setConverter(new StringConverter() {
                @Override
                public String toString(Object t) {
                    return "toString-" + t;
                }

                @Override
                public Object fromString(String t) {
                    return "fromString" + t;
                }
            });
        });

        Button changeCountButton = new Button("Change Item Count");
        changeCountButton.setOnAction((x) -> {
            new Timeline(
                new KeyFrame(Duration.seconds(1.0), (ev) -> {
                    System.out.println("2");
                    control.setVisibleRowCount(2);
                }),
                new KeyFrame(Duration.seconds(2.0), (ev) -> {
                    System.out.println("20");
                    control.setVisibleRowCount(20);
                }),
                new KeyFrame(Duration.seconds(3.0), (ev) -> {
                    System.out.println("2");
                    control.setVisibleRowCount(2);
                })).play();
        });

        OptionPane p = new OptionPane();
        p.option(setConverterButton);
        p.option(changeCountButton);

        setContent(b);
        setOptions(p);
    }
}

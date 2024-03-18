/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.options;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import com.oracle.tools.fx.monkey.util.EnterTextDialog;
import com.oracle.tools.fx.monkey.util.FX;

/**
 * Simple Text Option Bound to a Property.
 * Presents a text field with an Edit button for mode complex text.
 */
// TODO combo box for history?
// TODO highlight special characters?
public class TextOption extends BorderPane {
    private final SimpleStringProperty property = new SimpleStringProperty();
    private final TextField textField;

    public TextOption(String name, StringProperty p) {
        this(name);
        property.bindBidirectional(p);
    }

    public TextOption(String name, ObjectProperty<String> p) {
        this(name);
        property.bindBidirectional(p);
    }

    private TextOption(String name) {
        FX.name(this, name);

        textField = new TextField();
        textField.setMaxWidth(Double.MAX_VALUE);
        textField.setOnAction((ev) -> {
            String v = textField.getText();
            property.set(v);
        });

        Button editButton = new Button("Edit");
        editButton.setOnAction((ev) -> editValue());

        setCenter(textField);
        setRight(editButton);
        setMargin(editButton, new Insets(0, 0, 0, 2));
        setMaxWidth(Double.MAX_VALUE);
    }

    private void editValue() {
        String text = property.get();
        new EnterTextDialog(this, text, (v) -> {
            property.set(v);
        }).show();
    }
}

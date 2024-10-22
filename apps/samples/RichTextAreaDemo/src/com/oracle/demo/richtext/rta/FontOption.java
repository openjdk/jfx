/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
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

package com.oracle.demo.richtext.rta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import com.oracle.demo.richtext.util.FX;

/**
 * Font Option Bound to a Property.
 *
 * @author Andy Goryachev
 */
public class FontOption extends HBox {
    private final SimpleObjectProperty<Font> property = new SimpleObjectProperty<>();
    private final ComboBox<String> fontField = new ComboBox<>();
    private final ComboBox<String> styleField = new ComboBox<>();
    private final ComboBox<Double> sizeField = new ComboBox<>();

    public FontOption(String name, boolean allowNull, ObjectProperty<Font> p) {
        FX.name(this, name);
        if (p != null) {
            property.bindBidirectional(p);
        }

        FX.name(fontField, name + "_FONT");
        fontField.getItems().setAll(collectFonts(allowNull));
        fontField.getSelectionModel().selectedItemProperty().addListener((x) -> {
            String fam = fontField.getSelectionModel().getSelectedItem();
            updateStyles(fam);
            update();
        });

        FX.name(styleField, name + "_STYLE");
        styleField.getSelectionModel().selectedItemProperty().addListener((x) -> {
            update();
        });

        FX.name(sizeField, name + "_SIZE");
        sizeField.getItems().setAll(
            1.0,
            2.5,
            6.0,
            8.0,
            10.0,
            11.0,
            12.0,
            16.0,
            24.0,
            32.0,
            48.0,
            72.0,
            144.0,
            480.0
        );
        sizeField.getSelectionModel().selectedItemProperty().addListener((x) -> {
            update();
        });

        getChildren().setAll(fontField, styleField, sizeField);
        setHgrow(fontField, Priority.ALWAYS);
        setMargin(sizeField, new Insets(0, 0, 0, 2));

        setFont(property.get());
    }

    public SimpleObjectProperty<Font> getProperty() {
        return property;
    }

    public void select(String name) {
        fontField.getSelectionModel().select(name);
    }

    public Font getFont() {
        String name = fontField.getSelectionModel().getSelectedItem();
        if (name == null) {
            return null;
        }
        String style = styleField.getSelectionModel().getSelectedItem();
        if (!isBlank(style)) {
            name = name + " " + style;
        }
        Double size = sizeField.getSelectionModel().getSelectedItem();
        if (size == null) {
            size = 12.0;
        }
        return new Font(name, size);
    }

    private static boolean isBlank(String s) {
        return s == null ? true : s.trim().length() == 0;
    }

    protected void updateStyles(String family) {
        String st = styleField.getSelectionModel().getSelectedItem();
        if (st == null) {
            st = "";
        }

        List<String> ss = Font.getFontNames(family);
        for (int i = 0; i < ss.size(); i++) {
            String s = ss.get(i);
            if (s.startsWith(family)) {
                s = s.substring(family.length()).trim();
                ss.set(i, s);
            }
        }
        Collections.sort(ss);

        styleField.getItems().setAll(ss);
        int ix = ss.indexOf(st);
        if (ix >= 0) {
            styleField.getSelectionModel().select(ix);
        }
    }

    protected void update() {
        Font f = getFont();
        property.set(f);
    }

    private void setFont(Font f) {
        String name;
        String style;
        double size;
        if (f == null) {
            name = null;
            style = null;
            size = 12.0;
        } else {
            name = f.getFamily();
            style = f.getStyle();
            size = f.getSize();
        }
        fontField.getSelectionModel().select(name);
        styleField.getSelectionModel().select(style);
        sizeField.getSelectionModel().select(size);
    }

    protected List<String> collectFonts(boolean allowNull) {
        ArrayList<String> rv = new ArrayList<>();
        if (allowNull) {
            rv.add(null);
        }
        rv.addAll(Font.getFamilies());
        return rv;
    }

    public void selectSystemFont() {
        FX.select(fontField, "System");
        FX.select(styleField, "");
        FX.select(sizeField, 12.0);
    }
}

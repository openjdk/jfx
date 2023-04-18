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
package com.oracle.tools.fx.monkey.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.text.Font;

/**
 * Font Selector.
 */
public class FontSelector {
    private final ComboBox<String> fontField = new ComboBox<>();
    private final ComboBox<Integer> sizeField;

    public FontSelector(String id, Consumer<Font> client) {
        fontField.setId(id + "_FONT");
        fontField.getItems().setAll(collectFonts());
        fontField.getSelectionModel().selectedItemProperty().addListener((p) -> {
            update(client);
        });

        sizeField = new ComboBox<>();
        sizeField.setId(id + "_SIZE");
        sizeField.getItems().setAll(
            8,
            12,
            24,
            48,
            72
        );
        sizeField.getSelectionModel().selectedItemProperty().addListener((x) -> {
            update(client);
        });
    }

    protected void update(Consumer<Font> client) {
        Font f = getFont();
        client.accept(f);
    }

    public Node fontNode() {
        return fontField;
    }

    public Node sizeNode() {
        return sizeField;
    }

    public void select(String name) {
        fontField.getSelectionModel().select(name);
    }

    public Font getFont() {
        String name = fontField.getSelectionModel().getSelectedItem();
        if (name == null) {
            return null;
        }
        Integer size = sizeField.getSelectionModel().getSelectedItem();
        if (size == null) {
            size = 12;
        }
        return new Font(name, size);
    }

    protected List<String> collectFonts() {
        ArrayList<String> rv = new ArrayList<>(Font.getFontNames());
        //rv.add(0, null);
        return rv;
    }

    public void selectSystemFont() {
        FX.select(fontField, "System Regular"); // windows?
        FX.select(sizeField, 12);
    }
}

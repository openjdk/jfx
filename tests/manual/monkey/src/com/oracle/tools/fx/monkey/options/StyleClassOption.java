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

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import com.oracle.tools.fx.monkey.util.EnterTextDialog;
import com.oracle.tools.fx.monkey.util.FX;

/**
 * Style Class (Observable List of Strings) Option.
 * This can be later extracted into a generic option for list of strings.
 */
// TODO instead of combo box, use r/o text field + menu button (edit / revert)
public class StyleClassOption extends BorderPane {
    private final ComboBox<String> combo;
    private final ObservableList<String> list;

    public StyleClassOption(String name, ObservableList<String> list) {
        this.list = list;

        FX.name(this, name);

        combo = new ComboBox<>();
        combo.getItems().add(listToString(list));
        combo.getSelectionModel().select(0);

        Button editButton = FX.button("Edit", () -> {
            String text = listToString(list);
            EnterTextDialog d = new EnterTextDialog(this, text, (s) -> {
                setList(s);
            });
            d.setInstructions("Whitespace-delimited list of style class names:");
            d.show();
        });

        setCenter(combo);
        setRight(editButton);
        setMargin(editButton, new Insets(0, 0, 0, 2));
        setMaxWidth(Double.MAX_VALUE);

        combo.getSelectionModel().selectedItemProperty().addListener((s, pr, val) -> {
            setList(val);
        });
    }

    private static String listToString(Collection<String> items) {
        if ((items == null) || items.isEmpty()) {
            return null;
        }
        StringJoiner sj = new StringJoiner(" ");
        for (String s: items) {
            sj.add(s);
        }
        return sj.toString();
    }

    private static List<String> parseList(String text) {
        if (text == null) {
            return List.of();
        }
        return List.of(text.split("\\s+"));
    }

    private void setList(String text) {
        List<String> items = parseList(text);
        list.setAll(items);
    }
}

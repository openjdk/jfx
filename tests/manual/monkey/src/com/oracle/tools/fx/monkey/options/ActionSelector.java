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

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.NamedValue;

/**
 * Action Selector Executes Simple Actions (Runnable's).
 */
public class ActionSelector extends BorderPane {
    private final ComboBox<NamedValue<Runnable>> field;

    public ActionSelector(String name) {
        FX.name(this, name);

        field = new ComboBox<>();
        field.setMaxWidth(Double.MAX_VALUE);
        FX.name(field, "value");

        field.getSelectionModel().selectedItemProperty().addListener((s, pr, c) -> {
            Runnable r = c.getValue();
            try {
                r.run();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });

        setCenter(field);
    }

    public Runnable getValue() {
        NamedValue<Runnable> v = field.getSelectionModel().getSelectedItem();
        return v == null ? null : v.getValue();
    }

    public void clearChoices() {
        field.getItems().clear();
    }

    public void addChoice(String name, Runnable item) {
        field.getItems().add(new NamedValue<>(name, item));
    }

    public void select(int ix) {
        if ((ix >= 0) && (ix < field.getItems().size())) {
            field.getSelectionModel().select(ix);
        }
    }

    public void selectFirst() {
        select(0);
    }

    public void addButton(String text, Runnable r) {
        Button b = FX.button(text, r);
        setRight(b);
        setMargin(b, new Insets(0, 0, 0, 2));
    }
}

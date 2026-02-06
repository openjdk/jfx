/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates.
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

package com.oracle.demo.richtext.editor;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import com.oracle.demo.richtext.common.OptionPane;
import com.oracle.demo.richtext.editor.settings.EndKey;
import com.oracle.demo.richtext.util.FX;

/**
 * Rich Editor Demo Settings window.
 *
 * NOTE: the settings are not persisted due to limitations of FxSettings in this project.
 *
 * @author Andy Goryachev
 */
public class SettingsWindow extends Stage {

    private final OptionPane op;

    public SettingsWindow(Window parent) {

        initOwner(parent);

        op = new OptionPane();
        op.section("Navigation");
        op.option("End:", enumOption(EndKey.class, Settings.endKey));
//        op.option("Home:", new ComboBox());
//        op.option("Next Word:", new ComboBox());
//        op.option("Previous Word:", new ComboBox());

        op.section("View");
        op.option("Content Padding:", insetsOption(Settings.contentPadding, 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100));

        Scene scene = new Scene(op, 700, 500);

        setScene(scene);
        setTitle("Settings");
        centerOnScreen();
    }

    private static <E extends Enum> Node enumOption(Class<E> type, Property<E> p) {
        ComboBox<E> b = new ComboBox<>();
        b.setConverter(FX.converter());
        E[] values = type.getEnumConstants();
        for (E v : values) {
            b.getItems().add(v);
        }
        b.getSelectionModel().select(p.getValue());
        p.bind(b.getSelectionModel().selectedItemProperty());
        return b;
    }

    private static Node insetsOption(Property<Insets> p, int ... values) {
        ComboBox<Integer> b = new ComboBox<>();
        for (int v : values) {
            b.getItems().add(v);
        }
        Integer initialValue = insetsToInt(p.getValue());
        b.getSelectionModel().select(initialValue);
        p.bind(Bindings.createObjectBinding(
            () -> {
                return intToInsets(b.getSelectionModel().getSelectedItem());
            },
            b.getSelectionModel().selectedItemProperty()
        ));
        return b;
    }

    private static Integer insetsToInt(Insets v) {
        return v == null ? 0 : (int)v.getTop();
    }

    private static Insets intToInsets(Integer v) {
        if (v == null) {
            return Insets.EMPTY;
        }
        return new Insets(v);
    }
}

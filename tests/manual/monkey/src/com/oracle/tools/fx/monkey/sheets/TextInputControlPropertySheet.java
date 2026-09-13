/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.sheets;

import java.util.function.UnaryOperator;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.TextInputControl;
import javafx.util.StringConverter;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.FontOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * TextInputControl Property Sheet.
 */
public class TextInputControlPropertySheet {
    public static void appendTo(OptionPane op, boolean multiLine, TextInputControl c) {
        op.section("TextInputControl");
        op.option(new BooleanOption("editable", "editable", c.editableProperty()));
        op.option("Font:", new FontOption("font", false, c.fontProperty()));
        op.option("Prompt Text:", Options.promptText("promptText", true, c.promptTextProperty()));
        op.option("Text:", Options.textOption("text", multiLine, true, c.textProperty()));
        op.option("Text Formatter:", createTextFormatterOption("textFormatter", c.textFormatterProperty()));

        ControlPropertySheet.appendTo(op, c);
    }

    private static Node createTextFormatterOption(String name, ObjectProperty<TextFormatter<?>> p) {
        ObjectOption<TextFormatter<?>> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", null);
        op.addChoiceSupplier("with FILTER", () -> createFormatter(true, false, null));
        op.addChoiceSupplier("with value \"converter\"", () -> createFormatter(false, true, null));
        op.addChoiceSupplier("with FILTER + value \"converter\"", () -> createFormatter(true, true, null));
        op.selectInitialValue();
        return op;
    }

    private static <V> TextFormatter<V> createFormatter(boolean withFilter, boolean withConverter, V defaultValue) {
        StringConverter<V> converter = null;
        if (withFilter) {
            converter = new StringConverter<V>() {
                @Override
                public String toString(Object x) {
                    return x == null ? null : "\"" + x + "\"";
                }

                @Override
                public V fromString(String s) {
                    return (V)s;
                }
            };
        }

        UnaryOperator<TextFormatter.Change> filter = null;
        if (withFilter) {
            filter = new UnaryOperator<TextFormatter.Change>() {
                @Override
                public Change apply(Change ch) {
                    ch.setText(ch.getText().toUpperCase());
                    return ch;
                }
            };
        }
        return new TextFormatter<V>(converter, defaultValue, filter);
    }
}

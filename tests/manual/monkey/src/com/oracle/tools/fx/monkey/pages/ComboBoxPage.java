/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.util.StringConverter;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.ComboBoxBasePropertySheet;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.SequenceNumber;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * ComboBox Page.
 */
public class ComboBoxPage extends TestPaneBase implements HasSkinnable {
    private final ComboBox<Object> control;

    public ComboBoxPage() {
        super("ComboBoxPage");

        control = new ComboBox<>() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };
        control.setOnAction((ev) -> addItem());

        Button addButton = FX.button("Add Item", () -> {
            control.getItems().add(newItem(""));
        });

        Button clearButton = FX.button("Clear Items", () -> {
            control.getItems().clear();
        });

        OptionPane op = new OptionPane();
        op.section("ComboBox");
        op.option("Button Cell: TODO", null); // TODO
        op.option("Cell Factory: TODO", null); // TODO
        op.option("Converter:", createConverterOptions("converter", control.converterProperty()));
        op.option("Items:", createItemsOptions("items", control.getItems()));
        op.option(Utils.buttons(addButton, clearButton));
        op.option("Placeholder:", Options.placeholderNode("placeholder", control.placeholderProperty()));
        op.option("Selection Model:", createSelectionModelOptions("selectionModel"));

        op.section("ComboBoxBase");
        ComboBoxBasePropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);
    }

    private Node createSelectionModelOptions(String name) {
        var original = control.getSelectionModel();
        ObjectSelector<Boolean> s = new ObjectSelector<>(name, (v) -> {
            control.setSelectionModel(v == null ? null : original);
        });
        s.addChoice("Single", Boolean.FALSE);
        s.addChoice("<null>", null);
        s.selectFirst();
        return s;
    }

    private void addItem() {
        Object v = control.getValue();
        if (!control.getItems().contains(v)) {
            System.out.println("added: " + v);
            control.getItems().add(0, v);
            if (control.getSelectionModel() != null) {
                control.getSelectionModel().select(0);
            }
        }
    }

    // TODO common code with ListViewPage - move to utils?
    private String newItem(Object n) {
        return n + "." + SequenceNumber.next();
    }

    private String newVariableItem(Object n) {
        int rows = 1 << new Random().nextInt(5);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(i);
        }
        return n + "." + SequenceNumber.next() + "." + sb;
    }


    private Supplier<List<Object>> createItems(int count, Function<Integer, Object> gen) {
        return () -> {
            ArrayList<Object> rv = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Object v = gen.apply(i);
                rv.add(v);
            }
            return rv;
        };
    }

    private Node createItemsOptions(String name, ObservableList<Object> items) {
        ObjectSelector<List<Object>> s = new ObjectSelector<>(name, (v) -> {
            items.setAll(v);
        });
        s.addChoice("<empty>", List.of());
        s.addChoiceSupplier("1 Row", createItems(1, this::newItem));
        s.addChoiceSupplier("10 Rows", createItems(10, this::newItem));
        s.addChoiceSupplier("200 Rows", createItems(200, this::newItem));
        s.addChoiceSupplier("10,000 Rows", createItems(10_000, this::newItem));
        s.addChoiceSupplier("10 Variable Height Rows", createItems(10, this::newVariableItem));
        s.addChoiceSupplier("200 Variable HeightRows", createItems(200, this::newVariableItem));
        s.selectFirst();
        return s;
    }

    private Node createConverterOptions(String name, ObjectProperty<StringConverter<Object>> p) {
        var original = p.get();
        ObjectOption<StringConverter<Object>> op = new ObjectOption<>(name, p);
        op.addChoiceSupplier("Quoted", () -> {
            return new StringConverter<Object>() {
                @Override
                public String toString(Object x) {
                    return "\"" + x + "\"";
                }

                @Override
                public Object fromString(String s) {
                    return s;
                }
            };
        });
        op.addChoiceSupplier("Number", () -> {
            return new StringConverter<Object>() {
                @Override
                public String toString(Object x) {
                    return x == null ? null : String.valueOf(x);
                }

                @Override
                public Object fromString(String s) {
                    return s == null ? null : Double.parseDouble(s);
                }
            };
        });
        op.addChoice("<default>", original);
        op.addChoice("<null>", null);
        op.selectInitialValue();
        return op;
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new ComboBoxListViewSkin<>(control));
    }
}

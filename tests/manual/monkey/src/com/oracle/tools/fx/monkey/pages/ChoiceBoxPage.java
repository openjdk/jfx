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
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.skin.ChoiceBoxSkin;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.SequenceNumber;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * ChoiceBox Page.
 */
public class ChoiceBoxPage extends TestPaneBase implements HasSkinnable {
    private ChoiceBox<Object> control;

    public ChoiceBoxPage() {
        super("ChoiceBoxPage");

        control = new ChoiceBox();

        OptionPane op = new OptionPane();
        op.section("ChoiceBox");
        op.option("Converter: TODO", null); // TODO
        op.option("Items:", createItemsOption("items", control.getItems()));
        op.option("Selection Model:", createSelectionModelOptions("selectionModel"));
        op.option("Value: TODO", null); // TODO

        ControlPropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);
    }

    private static String[] mk(int size) {
        String[] ss = new String[size];
        for (int i = 0; i < size; i++) {
            ss[i] = ("Item " + i);
        }
        return ss;
    }

    // TODO duplicate code in ListView and some other classes - move to utils?
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

    private Node createItemsOption(String name, ObservableList<Object> items) {
        ObjectSelector<List<Object>> s = new ObjectSelector<>(name, (v) -> {
            items.setAll(v);
        });
        s.addChoiceSupplier("1 Row", createItems(1, this::newItem));
        s.addChoiceSupplier("10 Rows", createItems(10, this::newItem));
        s.addChoiceSupplier("200 Rows", createItems(200, this::newItem));
        //s.addChoiceSupplier("10,000 Rows", createItems(10_000, this::newItem));
        s.addChoiceSupplier("10 Variable Height Rows", createItems(10, this::newVariableItem));
        s.addChoiceSupplier("200 Variable HeightRows", createItems(200, this::newVariableItem));
        s.addChoice("<empty>", List.of());
        s.selectFirst();
        return s;
    }

    // TODO may be move to common?
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

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new ChoiceBoxSkin(control));
    }
}

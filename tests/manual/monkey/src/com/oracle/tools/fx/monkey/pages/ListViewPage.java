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
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.FocusModel;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ImageTools;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.SequenceNumber;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * ListView Page.
 */
public class ListViewPage extends TestPaneBase implements HasSkinnable {
    private final ListView<Object> control;

    public ListViewPage() {
        super("ListViewPage");

        control = new ListView<>() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };
        control.setTooltip(new Tooltip("edit to 'update' to commit the change"));
        control.setOnEditCommit((ev) -> {
            int ix = ev.getIndex();
            ev.getSource().getItems().set(ix, ev.getNewValue());
        });

        Button addButton = FX.button("Add Item", () -> {
            control.getItems().add(newItem(""));
        });

        Button clearButton = FX.button("Clear Items", () -> {
            control.getItems().clear();
        });

        Button jumpButton = FX.button("Jump w/VirtualFlow", () -> {
            jump();
        });

        Button refresh = FX.button("Refresh", () -> {
            control.refresh();
        });

        OptionPane op = new OptionPane();
        op.section("ListView");
        op.option("Cell Factory:", createCellFactoryOptions());
        op.option(new BooleanOption("editable", "editable", control.editableProperty()));
        op.option("Fixed Cell Size:", Options.fixedSizeOption("fixedCellSize", control.fixedCellSizeProperty()));
        op.option("Focus Model:", createFocusModelOptions("focusModel", control.focusModelProperty()));
        op.option("Items:", createItemsOptions("items", control.getItems()));
        op.option(Utils.buttons(addButton, clearButton));
        op.option("Orientation:", new EnumOption<Orientation>("orientation", Orientation.class, control.orientationProperty()));
        op.option("Placeholder:", Options.placeholderNode("placeholder", control.placeholderProperty()));
        op.option("Selection Model:", createSelectionModelOptions("selectionModel"));

        op.separator();
        op.option(jumpButton);
        op.option(refresh);
        ControlPropertySheet.appendTo(op, control);
        setOptions(op);
        setContent(new BorderPane(control));
    }

    private void jump() {
        int sz = control.getItems().size();
        int ix = sz / 2;

        control.getSelectionModel().select(ix);
        VirtualFlow f = findVirtualFlow(control);
        f.scrollTo(ix);
        f.scrollPixels(-1.0);
    }

    private VirtualFlow findVirtualFlow(Parent parent) {
        for (Node node: parent.getChildrenUnmodifiable()) {
            if (node instanceof VirtualFlow f) {
                return f;
            }

            if (node instanceof Parent p) {
                VirtualFlow f = findVirtualFlow(p);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new ListViewSkin(control));
    }

    private Node createCellFactoryOptions() {
        var original = control.getCellFactory();
        ObjectOption<Callback> op = new ObjectOption("cellFactory", control.cellFactoryProperty());
        op.addChoice("<default>", original);
        op.addChoiceSupplier("TextFieldListCell", () -> TextFieldListCell.forListView());
        op.addChoiceSupplier("Large Icon", () -> {
            return (r) -> {
                return new ListCell<Object>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null) {
                            super.setText(null);
                            super.setGraphic(null);
                        } else {
                            String s = item.toString();
                            super.setText(s);
                            Node n = new ImageView(ImageTools.createImage(s, 256, 256));
                            super.setGraphic(n);
                        }
                    }
                };
            };
        });
        op.addChoiceSupplier("ListViewSkin", () -> {
            return (r) -> new ListCell<Object>() {
                @Override
                public void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else if (item instanceof Node) {
                        setText(null);
                        Node currentNode = getGraphic();
                        Node newNode = (Node)item;
                        if (currentNode == null || !currentNode.equals(newNode)) {
                            setGraphic(newNode);
                        }
                    } else {
                        setText(item == null ? "null" : item.toString());
                        setGraphic(null);
                    }
                }
            };
        });
        op.addChoice("<null>", null);
        return op;
    }

    private Node createFocusModelOptions(String name, ObjectProperty<FocusModel<Object>> p) {
        var original = control.getFocusModel();
        ObjectOption<FocusModel<Object>> s = new ObjectOption<>(name, p);
        s.addChoice("<default>", original);
        s.addChoice("<null>", null);
        s.selectFirst();
        return s;
    }

    private Node createSelectionModelOptions(String name) {
        var original = control.getSelectionModel();
        ObjectSelector<Boolean> s = new ObjectSelector<>(name, (v) -> {
            control.setSelectionModel(v == null ? null : original);
            original.setSelectionMode(Boolean.TRUE.equals(v) ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        });
        s.addChoice("Single", Boolean.FALSE);
        s.addChoice("Multiple", Boolean.TRUE);
        s.addChoice("<null>", null);
        s.selectFirst();
        return s;
    }

    private String newItem(Object n) {
        return n + "." + SequenceNumber.next();
    }

    private String newVariableItem(Object n) {
        int rows = 1 << new Random().nextInt(5);
        return newItem(n, rows);
    }

    private String newLargeItem(Object n) {
        return newItem(n, 200);
    }

    private String newItem(Object n, int rows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(i);
        }
        return n + "." + SequenceNumber.next() + "." + sb;
    }

    private Supplier<List<Object>> createItems(int count) {
        return () -> {
            ArrayList<Object> rv = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Object v = newItem(i);
                rv.add(v);
            }
            return rv;
        };
    }

    private Supplier<List<Object>> createVariableItems(int count) {
        return () -> {
            ArrayList<Object> rv = new ArrayList<>(count);
            int i = 0;
            for ( ; i < count; i++) {
                Object v = newVariableItem(i);
                rv.add(v);
            }
            rv.add(newLargeItem(i));
            return rv;
        };
    }

    private Node createItemsOptions(String name, ObservableList<Object> items) {
        ObjectSelector<List<Object>> s = new ObjectSelector<>(name, (v) -> {
            items.setAll(v);
        });
        s.addChoiceSupplier("1 Row", createItems(1));
        s.addChoiceSupplier("10 Rows", createItems(10));
        s.addChoiceSupplier("200 Rows", createItems(200));
        s.addChoiceSupplier("10,000 Rows", createItems(10_000));
        s.addChoiceSupplier("10 Variable Height Rows", createVariableItems(10));
        s.addChoiceSupplier("200 Variable HeightRows", createVariableItems(200));
        s.addChoiceSupplier("2,000 Variable HeightRows", createVariableItems(2000));
        s.addChoice("<empty>", List.of());
        s.selectFirst();
        return s;
    }
}

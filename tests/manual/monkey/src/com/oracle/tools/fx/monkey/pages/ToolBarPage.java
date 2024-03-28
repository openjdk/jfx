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
package com.oracle.tools.fx.monkey.pages;

import java.util.List;
import javafx.beans.property.Property;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Region;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.util.EnterTextDialog;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * ToolBar Page.
 */
public class ToolBarPage extends TestPaneBase {
    private final ToolBar control;
    private int seq;

    public ToolBarPage() {
        super("ToolBarPage");

        control = new ToolBar();

        SplitMenuButton addButton = new SplitMenuButton(
            FX.menuItem("Button", () -> add(button())),
            FX.menuItem("Label", () -> add(label("Label"))),
            FX.menuItem("TextField", () -> add(textField(20)))
        );
        addButton.setText("Add");

        OptionPane op = new OptionPane();
        op.section("ToolBar");
        op.option("Items:", createItemsOptions("items", control.getItems()));
        op.option(Utils.buttons(addButton));
        op.option("Orientation:", new EnumOption<>("orientation", Orientation.class, control.orientationProperty()));
        // TODO this control needs spacing property (in the skin)
        ControlPropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);
    }

    private void add(Node n) {
        control.getItems().add(n);
    }

    private Node createItemsOptions(String name, ObservableList<Node> items) {
        ObjectSelector<List<Node>> s = new ObjectSelector<>(name, (v) -> {
            items.setAll(v);
        });
        s.addChoiceSupplier("Buttons", () -> {
            return List.of(
                button(),
                button(),
                button(),
                button(),
                button(),
                button(),
                button(),
                button()
            );
        });
        s.addChoiceSupplier("Mixed", () -> {
            return List.of(
                button(),
                label("Find:"),
                textField(20),
                button(),
                button(),
                button(),
                button(),
                button(),
                button(),
                button()
            );
        });
        s.addChoice("<empty>", List.of());
        s.selectFirst();
        return s;
    }

    private Node button() {
        Button n = new Button("Button " + (++seq));
        setContextMenu(n);
        return n;
    }

    private Node label(String text) {
        Label n = new Label(text);
        setContextMenu(n);
        return n;
    }

    private Node textField(int cols) {
        TextField n = new TextField();
        setContextMenu(n);
        n.setPrefColumnCount(cols);
        return n;
    }

    private Property<String> getTextProperty(Node n) {
        if(n instanceof Label label) {
            return label.textProperty();
        } else if(n instanceof ButtonBase b) {
            return b.textProperty();
        } else if(n instanceof TextInputControl c) {
            return c.textProperty();
        }
        return null;
    }

    private void setContextMenu(Region n) {
        n.setOnContextMenuRequested((ev) -> {
            ContextMenu m = new ContextMenu();
            FX.item(m, "Remove", () -> {
                control.getItems().remove(n);
            });

            FX.separator(m);

            FX.item(m, "Edit Text", EnterTextDialog.getRunnable(n, getTextProperty(n)));

            FX.separator(m);

            FX.item(m, "Pref(50)", () -> {
                n.setPrefWidth(50);
            });
            FX.item(m, "Pref(200)", () -> {
                n.setPrefWidth(200);
            });
            FX.item(m, "Pref(500)", () -> {
                n.setPrefWidth(500);
            });
            m.show(n, ev.getScreenX(), ev.getScreenY());
        });
    }
}

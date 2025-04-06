/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.PaneContentOptions;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.sheets.PropertiesMonitor;
import com.oracle.tools.fx.monkey.sheets.RegionPropertySheet;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.Menus;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * GridPane Page.
 */
public class GridPanePage extends TestPaneBase {
    static record GridCoordinates(int col, int row) { }

    private final GridPane pane;

    public GridPanePage() {
        super("GridPanePage");

        pane = new GridPane() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };

        MenuButton addButton = new MenuButton("Add");
        PaneContentOptions.addChildOption(addButton.getItems(), pane.getChildren(), this::createMenu);

        Button clearButton = FX.button("Clear Items", () -> {
            pane.getChildren().clear();
        });

        OptionPane op = new OptionPane();
        op.section("GridPane");
        op.option("Alignment:", new EnumOption<Pos>("alignment", Pos.class, pane.alignmentProperty()));
        op.option("Children:", createChildrenOptions(pane.getChildren()));
        op.option(Utils.buttons(addButton, clearButton));
        op.option(new BooleanOption("gridLinesVisible", "grid lines visible", pane.gridLinesVisibleProperty()));
        op.option("HGap:", Options.spacing("hgap", pane.hgapProperty()));
        op.option("VGap:", Options.spacing("vgap", pane.vgapProperty()));
        RegionPropertySheet.appendTo(op, pane);

        setContent(pane);
        setOptions(op);
    }

    private Region createRegion() {
        Region r = new Region();
        r.setPrefWidth(100);
        r.setPrefHeight(100);
        r.setMinWidth(50);
        r.setMinHeight(50);
        r.setBackground(Background.fill(Utils.nextColor()));
        createMenu(r);
        return r;
    }

    public Node createChildrenOptions(ObservableList<Node> children) {
        ObjectSelector<Map<GridCoordinates, Node>> s = new ObjectSelector<>("children", (m) -> {
            children.clear();
            if (m != null) {
                for (Map.Entry<GridCoordinates, Node> en: m.entrySet()) {
                    GridCoordinates c = en.getKey();
                    Node n = en.getValue();
                    pane.add(n, c.col(), c.row());
                }
            }
        });
        // TODO add more templates here
        s.addChoice("<empty>", new HashMap<>());
        s.addChoiceSupplier("Diagonal", () -> {
            Map<GridCoordinates, Node> m = new HashMap<>();
            m.put(new GridCoordinates(0, 0), createRegion());
            m.put(new GridCoordinates(1, 1), createRegion());
            m.put(new GridCoordinates(2, 2), createRegion());
            return m;
        });
        return s;
    }

    private void createMenu(Node n) {
        FX.setPopupMenu(n, () -> {
            ContextMenu cm = new ContextMenu();
            Menus.intSubMenu(cm, "Column Index", (v) -> GridPane.setColumnIndex(n, v), () -> GridPane.getColumnIndex(n), 0, 10);
            Menus.intSubMenu(cm, "Column Span", (v) -> GridPane.setColumnSpan(n, v), () -> GridPane.getColumnSpan(n), 0, 10);
            Menus.booleanSubMenu(cm, "Fill Height", (v) -> GridPane.setFillHeight(n, v), () -> GridPane.isFillHeight(n));
            Menus.booleanSubMenu(cm, "Fill Width", (v) -> GridPane.setFillWidth(n, v), () -> GridPane.isFillWidth(n));
            Menus.enumSubMenu(cm, "HAlignment", HPos.class, true, (v) -> GridPane.setHalignment(n, v), () -> GridPane.getHalignment(n));
            Menus.enumSubMenu(cm, "HGrow", Priority.class, true, (v) -> GridPane.setHgrow(n, v), () -> GridPane.getHgrow(n));
            Menus.marginSubMenu(cm, (v) -> GridPane.setMargin(n, v), () -> GridPane.getMargin(n));
            Menus.intSubMenu(cm, "Row Index", (v) -> GridPane.setRowIndex(n, v), () -> GridPane.getRowIndex(n), 0, 10);
            Menus.intSubMenu(cm, "Row Span", (v) -> GridPane.setRowSpan(n, v), () -> GridPane.getRowSpan(n), 0, 10);
            Menus.enumSubMenu(cm, "Valignment", VPos.class, true, (v) -> GridPane.setValignment(n, v), () -> GridPane.getValignment(n));
            Menus.enumSubMenu(cm, "VGrow", Priority.class, true, (v) -> GridPane.setVgrow(n, v), () -> GridPane.getVgrow(n));
            if(n instanceof Region r) {
                FX.separator(cm);
                Menus.sizeSubMenus(cm, r);
            }
            FX.separator(cm);
            FX.item(cm, "Remove", () -> {
                if (n.getParent() instanceof Pane p) {
                    p.getChildren().remove(n);
                }
            });
            FX.separator(cm);
            FX.item(cm, "Show Properties Monitor...", () -> {
                PropertiesMonitor.open(n);
            });
            return cm;
        });
    }
}

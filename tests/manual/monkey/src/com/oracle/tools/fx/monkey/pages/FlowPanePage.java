/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.PaneContentOptions;
import com.oracle.tools.fx.monkey.sheets.PropertiesMonitor;
import com.oracle.tools.fx.monkey.sheets.RegionPropertySheet;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.Menus;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * FlowPane Page.
 */
public class FlowPanePage extends TestPaneBase {
    private final FlowPane pane;

    public FlowPanePage() {
        super("FlowPanePage");

        pane = new FlowPane() {
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
        op.section("FlowPane");
        op.option("Alignment:", new EnumOption<>("alignment", Pos.class, pane.alignmentProperty()));
        op.option("Children:", Utils.buttons(addButton, clearButton));
        op.option("Column HAlignment:", new EnumOption<>("columnHalignment", HPos.class, pane.columnHalignmentProperty()));
        op.option("HGap:", DoubleOption.of("hgap", pane.hgapProperty(), 0, 10, 20, 30, 100));
        op.option("Orientation:", new EnumOption<>("orientation", Orientation.class, pane.orientationProperty()));
        op.option("Pref Wrap Length:", DoubleOption.of("vgap", pane.prefWrapLengthProperty(), 0, 100, 200, 300, 400, 500));
        op.option("Row VAlignment:", new EnumOption<>("rowValignment", VPos.class, pane.rowValignmentProperty()));
        op.option("VGap:", DoubleOption.of("vgap", pane.vgapProperty(), 0, 10, 20, 30, 100));
        RegionPropertySheet.appendTo(op, pane);

        setContent(pane);
        setOptions(op);
    }

    private void createMenu(Node n) {
        FX.setPopupMenu(n, () -> {
            ContextMenu cm = new ContextMenu();
            Menus.marginSubMenu(cm, (v) -> FlowPane.setMargin(n, v), () -> FlowPane.getMargin(n));
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

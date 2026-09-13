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

import javafx.geometry.Pos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.PaneContentOptions;
import com.oracle.tools.fx.monkey.sheets.PropertiesMonitor;
import com.oracle.tools.fx.monkey.sheets.RegionPropertySheet;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.Menus;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * BorderPane Page.
 */
public class BorderPanePage extends TestPaneBase {
    private final BorderPane pane;

    public BorderPanePage() {
        super("BorderPanePage");

        pane = new BorderPane() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };

        Button clear = new Button("Remove All");
        clear.setOnAction((ev) -> {
            pane.getChildren().clear();
        });

        OptionPane op = new OptionPane();
        op.section("BorderPane");
        op.option("Bottom:", PaneContentOptions.childOption("center", pane.bottomProperty(), this::createMenu));
        op.option("Center:", PaneContentOptions.childOption("center", pane.centerProperty(), this::createMenu));
        op.option("Left:", PaneContentOptions.childOption("left", pane.leftProperty(), this::createMenu));
        op.option("Right:", PaneContentOptions.childOption("right", pane.rightProperty(), this::createMenu));
        op.option("Top:", PaneContentOptions.childOption("top", pane.topProperty(), this::createMenu));
        op.option(Utils.buttons(clear));
        RegionPropertySheet.appendTo(op, pane);

        setContent(pane);
        setOptions(op);
    }

    private void createMenu(Node n) {
        FX.setPopupMenu(n, () -> {
            ContextMenu cm = new ContextMenu();
            Menus.enumSubMenu(cm, "Alignment", Pos.class, true, (v) -> BorderPane.setAlignment(n, v), () -> BorderPane.getAlignment(n));
            Menus.marginSubMenu(cm, (v) -> BorderPane.setMargin(n, v), () -> BorderPane.getMargin(n));
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

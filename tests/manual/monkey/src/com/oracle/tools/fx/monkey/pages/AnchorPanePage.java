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

import java.util.function.Consumer;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.AnchorPane;
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
 * AnchorPane Page.
 */
public class AnchorPanePage extends TestPaneBase {
    private final AnchorPane pane;

    public AnchorPanePage() {
        super("AnchorPanePage");

        pane = new AnchorPane() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };

        MenuButton addButton = new MenuButton("Add");
        PaneContentOptions.addChildOption(addButton.getItems(), pane.getChildren(), this::createMenu);

        Button clearButton = new Button("Remove All");
        clearButton.setOnAction((ev) -> {
            pane.getChildren().clear();
        });

        OptionPane op = new OptionPane();
        op.section("AnchorPane");
        op.option("Children:", Utils.buttons(addButton, clearButton));
        RegionPropertySheet.appendTo(op, pane);

        setContent(pane);
        setOptions(op);
    }

    private void createMenu(Node n) {
        FX.setPopupMenu(n, () -> {
            ContextMenu m = new ContextMenu();
            anchorMenu(m, "Set Bottom Anchor", (off) -> {
                AnchorPane.setBottomAnchor(n, off);
            });
            anchorMenu(m, "Set Left Anchor", (off) -> {
                AnchorPane.setLeftAnchor(n, off);
            });
            anchorMenu(m, "Set Right Anchor", (off) -> {
                AnchorPane.setRightAnchor(n, off);
            });
            anchorMenu(m, "Set Top Anchor", (off) -> {
                AnchorPane.setTopAnchor(n, off);
            });
            FX.separator(m);
            FX.item(m, "Clear Anchors", () -> {
                AnchorPane.setBottomAnchor(n, null);
                AnchorPane.setLeftAnchor(n, null);
                AnchorPane.setRightAnchor(n, null);
                AnchorPane.setTopAnchor(n, null);
            });
            if(n instanceof Region r) {
                FX.separator(m);
                Menus.sizeSubMenus(m, r);
            }
            FX.separator(m);
            FX.item(m, "Delete", () -> {
                if (n.getParent() instanceof Pane p) {
                    p.getChildren().remove(n);
                }
            });
            FX.separator(m);
            FX.item(m, "Show Properties Monitor...", () -> {
                PropertiesMonitor.open(n);
            });
            return m;
        });
    }

    private static void anchorMenu(ContextMenu cm, String text, Consumer<Double> client) {
        Menu m = FX.menu(cm, text);
        anchor(m, client, null);
        anchor(m, client, -100.0);
        anchor(m, client, -50.0);
        anchor(m, client, 0.0);
        anchor(m, client, 50.0);
        anchor(m, client, 100.0);
        anchor(m, client, 200.0);
        anchor(m, client, 300.0);
    }

    private static void anchor(Menu m, Consumer<Double> client, Double value) {
        String name = value == null ? "<null>" : String.valueOf(value);
        FX.item(m, name, () -> {
            client.accept(value);
        });
    }
}

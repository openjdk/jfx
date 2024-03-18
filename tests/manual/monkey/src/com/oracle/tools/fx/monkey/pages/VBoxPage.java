/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.PaneContentOptions;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.sheets.RegionPropertySheet;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * VBox Page.
 * @see HBoxPage
 */
public class VBoxPage extends TestPaneBase {
    private final VBox box;

    public VBoxPage() {
        super("VBoxPage");

        box = new VBox();

        // TODO menu button
        Button addButton = new Button("Add Item");
        addButton.setOnAction((ev) -> {
            addItem(box.getChildren());
        });

        Button clearButton = new Button("Clear Items");
        clearButton.setOnAction((ev) -> {
            box.getChildren().clear();
        });
        
        OptionPane op = new OptionPane();
        op.section("VBox");
        op.option("Alignment:", new EnumOption<Pos>("alignment", Pos.class, box.alignmentProperty()));
        op.option("Children:", PaneContentOptions.createOptions(box.getChildren(), this::createBuilder));
        op.option(Utils.buttons(addButton, clearButton));
        op.option(new BooleanOption("fillHWidth", "fill width", box.fillWidthProperty()));
        op.option("Spacing:", Options.spacing("spacing", box.spacingProperty()));

        RegionPropertySheet.appendTo(op, box);

        BorderPane bp = new BorderPane(box);
        bp.setPadding(new Insets(0, 10, 0, 0));
        setContent(bp);
        setOptions(op);
    }

    private Region addItem(List<Node> children) {
        boolean even = (children.size() % 2) == 0;
        Background bg = Background.fill(even ? Color.GRAY : Color.LIGHTGRAY);
        Region r = createRegion();
        r.setBackground(bg);
        children.add(r);
        return r;
    }

    private Region createRegion() {
        Region r = new Region();
        r.setPrefHeight(30);
        r.setMinHeight(10);

        ContextMenu m = new ContextMenu();
        r.setOnContextMenuRequested((ev) -> {
            m.getItems().setAll(
                new MenuItem("height=" + r.getHeight()),
                new SeparatorMenuItem(),
                new MenuItem("min height=" + r.getMinHeight()),
                new MenuItem("pref height=" + r.getPrefHeight()),
                new MenuItem("max height=" + r.getMaxHeight()));
            m.show(r, ev.getScreenX(), ev.getScreenY());
        });
        return r;
    }

    private PaneContentOptions.Builder createBuilder() {
        return new PaneContentOptions.Builder(this::addItem) {
            @Override
            protected void setGrow(Node n, Priority p) {
                VBox.setVgrow(n, p);
            }

            @Override
            protected void setMin(Region r, double v) {
                r.setMinHeight(v);
            }

            @Override
            protected void setPref(Region r, double v) {
                r.setPrefHeight(v);
            }

            @Override
            protected void setMax(Region r, double v) {
                r.setMaxHeight(v);
            }
        };
    }
}

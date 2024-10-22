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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ImageTools;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * TabPane Page.
 */
public class TabPanePage extends TestPaneBase implements HasSkinnable {
    private final TabPane control;

    public TabPanePage() {
        super("TabPanePage");

        control = new TabPane() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };

        // TODO other Tab propertis in the context menu

        OptionPane op = new OptionPane();
        op.section("TabPane");
        op.option("Tabs:", createTabsOption("tabs", control.getTabs()));
        op.option(new BooleanOption("rotateGraphic", "rotate graphic", control.rotateGraphicProperty()));
        op.option("Selection Model:", createSelectionModelOptions("selectionModel"));
        op.option("Side:", new EnumOption<Side>("side", true, Side.class, control.sideProperty()));
        op.option("Tab Closing Policy:", new EnumOption<TabClosingPolicy>("tabClosingPolicy", true, TabClosingPolicy.class, control.tabClosingPolicyProperty()));
        op.option("Tab Drag Policy:", new EnumOption<TabDragPolicy>("tabDragPolicy", true, TabDragPolicy.class, control.tabDragPolicyProperty()));
        op.option("Tab Max Height", Options.tabPaneConstraints("tabMaxHeight", control.tabMaxHeightProperty()));
        op.option("Tab Max Width", Options.tabPaneConstraints("tabMaxWidth", control.tabMaxWidthProperty()));
        op.option("Tab Min Height", Options.tabPaneConstraints("tabMinHeight", control.tabMinHeightProperty()));
        op.option("Tab Min Width", Options.tabPaneConstraints("tabMinWidth", control.tabMinWidthProperty()));

        ControlPropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);
    }

    private Node createTabsOption(String name, ObservableList<Tab> items) {
        ObjectSelector<List<Tab>> s = new ObjectSelector<>(name, (v) -> {
            items.setAll(v);
        });
        s.addChoice("<empty>", List.of());
        s.addChoiceSupplier("1 Tab", tabs(1, false));
        s.addChoiceSupplier("2 Tabs", tabs(2, false));
        s.addChoiceSupplier("10 Tabs", tabs(10, false));
        s.addChoiceSupplier("200 Tabs", tabs(200, false));
        s.addChoiceSupplier("1,000 Tabs", tabs(1_000, false));
        s.addChoiceSupplier("1 Tab with Graphic", tabs(1, true));
        s.addChoiceSupplier("2 Tabs with Graphic", tabs(2, true));
        s.addChoiceSupplier("10 Tabs with Graphic", tabs(10, true));
        s.addChoiceSupplier("200 Tabs with Graphic", tabs(200, true));
        s.select(2);
        return s;
    }

    private Supplier<List<Tab>> tabs(int count, boolean graphic) {
        return () -> {
            Random r = new Random();
            ArrayList<Tab> ts = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Node n = mkContent("Content_" + i);
                String name = "T_" + i;
                Tab t = new Tab(name, n);
                if (graphic) {
                    Image im;
                    switch (r.nextInt(5)) {
                    case 0:
                        im = ImageTools.createImage(name, 70, 20);
                        break;
                    case 1:
                        im = ImageTools.createImage(name, 20, 70);
                        break;
                    case 3:
                        im = ImageTools.createImage(20, 20);
                        break;
                    default:
                        im = null;
                        break;
                    }

                    if (im != null) {
                        t.setGraphic(new ImageView(im));
                    }
                }
                ts.add(t);
            }
            return ts;
        };
    }

    private Node mkContent(String text) {
        Label label = new Label(text);

        TextField textField = new TextField();
        textField.setPromptText("focus here");

        Button button = new Button("OK");

        VBox b = new VBox(5);
        b.setPadding(new Insets(0, 20, 0, 20));
        b.setAlignment(Pos.CENTER);
        b.getChildren().add(label);
        b.getChildren().add(textField);
        b.getChildren().add(button);
        return b;
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

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new TabPaneSkin(control));
    }
}

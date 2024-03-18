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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.layout.VBox;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
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

        control = new TabPane();
        // TODO graphic, other Tab propertis in the context menu
        control.getTabs().addAll(
            new Tab("One", mkContent("Tab One Content")),
            new Tab("Two", mkContent("Tab Two Content")),
            new Tab("Three", mkContent("Tab Three Content")),
            new Tab("Four", mkContent("Tab Four Content"))
        );

        OptionPane op = new OptionPane();
        op.section("TabPane");
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

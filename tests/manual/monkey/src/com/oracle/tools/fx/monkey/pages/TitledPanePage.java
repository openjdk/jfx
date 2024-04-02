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

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.skin.TitledPaneSkin;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.LabeledPropertySheet;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * TitledPane Page.
 */
public class TitledPanePage extends TestPaneBase implements HasSkinnable {
    private final TitledPane control;

    public TitledPanePage() {
        super("TitledPane");

        control = new TitledPane();

        OptionPane op = new OptionPane();
        op.section("TitledPane");
        op.option(new BooleanOption("animated", "animated", control.animatedProperty()));
        op.option(new BooleanOption("collapsible", "collapsible", control.collapsibleProperty()));
        op.option("Content:", createContentOptions("content", control.contentProperty()));
        op.option(new BooleanOption("expanded", "expanded", control.expandedProperty()));
        LabeledPropertySheet.appendTo(op, "Labeled", false, control);

        setContent(control);
        setOptions(op);
    }

    private Node makeAnchorPane() {
        VBox b = new VBox(new TextField("First"), new TextField("Second"));
        AnchorPane p = new AnchorPane(b);
        AnchorPane.setTopAnchor(b, 10.0);
        AnchorPane.setBottomAnchor(b, 10.0);
        AnchorPane.setLeftAnchor(b, 100.0);
        AnchorPane.setRightAnchor(b, 50.0);
        return p;
    }

    private Node createContentOptions(String name, ObjectProperty<Node> p) {
        ObjectOption<Node> s = new ObjectOption<>(name, p);
        s.addChoiceSupplier("Label", () -> new Label("Label"));
        s.addChoiceSupplier("AnchorPane", () -> makeAnchorPane());
        s.addChoiceSupplier("<null>", () -> null);
        s.selectFirst();
        return s;
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new TitledPaneSkin(control));
    }
}

/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.ItemSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.Templates;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.TextSelector;

/**
 * TitledPane Page
 */
public class TitledPanePage extends TestPaneBase {
    private final TextSelector textSelector;
    private final ItemSelector<Supplier<Node>> contentSelector;
    private final CheckBox snap;
    private final TitledPane control;

    public TitledPanePage() {
        FX.name(this, "TitledPane");

        textSelector = TextSelector.fromPairs(
            "textSelector",
            (t) -> update(),
            Templates.multiLineTextPairs()
        );
        textSelector.removeChoice("Writing Systems");

        contentSelector = new ItemSelector<Supplier<Node>>(
            "contentSelector",
            (g) -> update(),
            new Object[] {
                "null", null,
                "AnchorPane", mk(() -> makeAnchorPane()),
                "Label", mk(() -> new Label("Label"))
            }
        );

        snap = new CheckBox("snap");
        FX.name(snap, "snap");

        control = new TitledPane();

        snap.selectedProperty().bindBidirectional(control.snapToPixelProperty());

        OptionPane op = new OptionPane();
        op.label("Text:");
        op.option(textSelector.node());
        op.label("Content:");
        op.option(contentSelector.node());
        op.option(snap);

        setContent(control);
        setOptions(op);

        update();
    }

    protected void update() {
        Supplier<Node> gen = contentSelector.getSelectedItem();
        Node n = (gen == null) ? null : gen.get();

        control.setText(textSelector.getSelectedText());
        control.setContent(n);
    }

    protected Supplier<Node> mk(Supplier<Node> gen) {
        return gen;
    }

    protected Node makeAnchorPane() {
        VBox b = new VBox(new TextField("First"), new TextField("Second"));
        AnchorPane p = new AnchorPane(b);
        AnchorPane.setTopAnchor(b, 10.0);
        AnchorPane.setBottomAnchor(b, 10.0);
        AnchorPane.setLeftAnchor(b, 100.0);
        AnchorPane.setRightAnchor(b, 50.0);
        return p;
    }
}

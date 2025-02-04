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

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleSpinner;
import com.oracle.tools.fx.monkey.options.DurationOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.ImageTools;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Tooltip Page.
 */
public class TooltipPage extends TestPaneBase {
    private final Tooltip tooltip;

    public TooltipPage() {
        super("TooltipPage");

        tooltip = new Tooltip("This is a tooltip with some default text, to be settable later.");
        tooltip.showDelayProperty().addListener((s, p, c) -> {
            System.out.println("showDelay=" + c);
        });

        Label content = new Label("Hover over this area to show the tooltip");
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Tooltip.install(content, tooltip);
        content.setAlignment(Pos.CENTER);

        OptionPane op = new OptionPane();
        op.section("Tooltip");
        op.option("Content Display:", new EnumOption<>("contentDisplay", ContentDisplay.class, tooltip.contentDisplayProperty()));
        op.option("Font: TODO", null); // TODO font
        op.option("Graphic:", createGraphicOptions("graphic", tooltip.graphicProperty()));
        op.option("Graphic Text Gap:", new DoubleSpinner("graphicTextGap", 0, 100, 0.1, tooltip.graphicTextGapProperty()));
        op.option("Hide Delay:", new DurationOption("hideDelay", tooltip.hideDelayProperty()));
        op.option("Show Delay:", new DurationOption("showDelay", tooltip.showDelayProperty()));
        op.option("Show Duration:", new DurationOption("showDuration", tooltip.showDurationProperty()));
        op.option("Text:", Options.textOption("text", true, true, tooltip.textProperty()));
        op.option("Text Alignment:", new EnumOption<>("textAlignment", TextAlignment.class, tooltip.textAlignmentProperty()));
        op.option("Text Overrun:", new EnumOption<>("textOverrun", OverrunStyle.class, tooltip.textOverrunProperty()));
        op.option(new BooleanOption("wrapText", "wrap text", tooltip.wrapTextProperty()));

        // TODO popup window

        setContent(new BorderPane(content));
        setOptions(op);
    }

    private Node createGraphicOptions(String string, ObjectProperty<Node> graphicProperty) {
        ObjectOption<Node> op = new ObjectOption<>("graphic", tooltip.graphicProperty());
        op.addChoice("<null>", null);
        op.addChoice("Image", ImageTools.createImageView(256, 256));
        op.addChoiceSupplier("Interactive Content", this::createInteractiveContent);
        return op;
    }

    // TODO tooltip cannot be interactive: the default behavior is to move it away from underneath the mouse pointer!
    private Node createInteractiveContent() {
        boolean autoHide = tooltip.isAutoHide();
        tooltip.setAutoHide(false);

        TextField f = new TextField();
        return f;
    }
}

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

import java.util.function.Supplier;
import javafx.beans.property.ObjectProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ImageTools;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * ScrollPane Page.
 */
public class ScrollPanePage extends TestPaneBase implements HasSkinnable {
    private final ScrollPane control;

    public ScrollPanePage() {
        super("ScrollPanePage");

        control = new ScrollPane() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };

        OptionPane op = new OptionPane();
        op.section("ScrollPane");
        op.option("Content:", createContentOptions("content", control.contentProperty()));
        op.option(new BooleanOption("fitToHeight", "fit to height", control.fitToHeightProperty()));
        op.option(new BooleanOption("fitToWidth", "fit to width", control.fitToHeightProperty()));
        op.option("HBar Policy:", new EnumOption<ScrollBarPolicy>("hbarPolicy", true, ScrollBarPolicy.class, control.hbarPolicyProperty()));
        op.option("HMax: TODO", null); // TODO
        op.option("HMin: TODO", null); // TODO
        op.option("HValue: TODO", null); // TODO
        op.option("Min Viewport Height: TODO", null); // TODO
        op.option("Min Viewport Width: TODO", null); // TODO
        op.option(new BooleanOption("pannable", "pannable", control.pannableProperty()));
        op.option("Pref Viewport Height: TODO", null); // TODO
        op.option("Pref Viewport Width: TODO", null); // TODO
        op.option("VBar Policy:", new EnumOption<ScrollBarPolicy>("vbarPolicy", true, ScrollBarPolicy.class, control.vbarPolicyProperty()));
        op.option("Viewport Bounds: TODO", null); // TODO
        op.option("VMax: TODO", null); // TODO
        op.option("VMin: TODO", null); // TODO
        op.option("VValue: TODO", null); // TODO
        ControlPropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new ScrollPaneSkin(control));
    }

    private Supplier<Node> mk(int w, int h) {
        return () -> {
            return ImageTools.createImageView(w, h);
        };
    }

    private Node createContentOptions(String name, ObjectProperty<Node> p) {
        ObjectOption<Node> op = new ObjectOption<>(name, p);
        op.addChoiceSupplier("50 x 50", mk(50, 50));
        op.addChoiceSupplier("100 x 100", mk(100, 100));
        op.addChoiceSupplier("1,000 x 1,000", mk(1_000, 1_000));
        op.addChoiceSupplier("1,000 x 50", mk(1_000, 50));
        op.addChoiceSupplier("50 x 1,000", mk(50, 1_000));
        op.addChoice("<null>", null);
        op.select(3);
        return op;
    }
}

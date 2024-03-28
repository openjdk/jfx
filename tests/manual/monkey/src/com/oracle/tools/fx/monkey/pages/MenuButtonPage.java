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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.MenuButtonSkin;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.sheets.LabeledPropertySheet;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * MenuButton Page.
 */
public class MenuButtonPage extends TestPaneBase implements HasSkinnable {
    private final MenuButton control;

    public MenuButtonPage() {
        super("MenuButtonPage");

        control = new MenuButton();

        control.setText("Menu Button");
        control.getItems().add(new MenuItem("Edit"));

        OptionPane op = new OptionPane();
        op.section("MenuButton");
        op.option("Items:", createItemsOptions("items", control.getItems()));
        op.option("Popup Side:", new EnumOption<Side>("popupSide", true, Side.class, control.popupSideProperty()));
        LabeledPropertySheet.appendTo(op, "ButtonBase", false, control);

        setContent(control);
        setOptions(op);
    }

    private Supplier<List<MenuItem>> mk(int count) {
        return () -> {
            ArrayList<MenuItem> rv = new ArrayList(count);
            for (int i = 0; i < count; i++) {
                rv.add(new MenuItem("Item_" + (i + 1)));
            }
            return rv;
        };
    }

    private Node createItemsOptions(String name, ObservableList<MenuItem> items) {
        ObjectSelector<List<MenuItem>> s = new ObjectSelector<>(name, items::setAll);
        s.addChoiceSupplier("1 Item", mk(1));
        s.addChoiceSupplier("10 Items", mk(10));
        s.addChoiceSupplier("1,000 Items", mk(1000));
        s.addChoiceSupplier("<empty>", mk(0));
        s.selectFirst();
        return s;
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new MenuButtonSkin(control));
    }
}

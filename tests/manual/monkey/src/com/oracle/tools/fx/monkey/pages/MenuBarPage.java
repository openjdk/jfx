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
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.skin.MenuBarSkin;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ImageTools;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * MenuBar Page.
 */
public class MenuBarPage extends TestPaneBase implements HasSkinnable {
    private final MenuBar control;

    public MenuBarPage() {
        super("MenuButtonPage");

        control = new MenuBar() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };

        OptionPane op = new OptionPane();
        op.section("MenuBar");
        op.option("Items:", createItemsOptions("items", control.getMenus()));
        op.option(new BooleanOption("useSystemMenuBar", "use system menu bar", control.useSystemMenuBarProperty()));
        ControlPropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);
    }

    private Supplier<List<Menu>> mk(int count) {
        return () -> {
            ArrayList<Menu> rv = new ArrayList(count);
            for (int i = 0; i < count; i++) {
                Menu m = mkMenu(String.valueOf(i + 1));
                rv.add(m);
            }
            return rv;
        };
    }

    private Menu mkMenu(String name) {
        Menu m = new Menu("Menu" + name);
        ToggleGroup g = new ToggleGroup();
        m.getItems().addAll(
            new MenuItem("MenuItem 1"),
            new MenuItem("MenuItem 2"),
            new MenuItem("MenuItem 3"),
            new SeparatorMenuItem(),
            radio("RadioMenuItem 1", g),
            radio("RadioMenuItem 2", g),
            radio("RadioMenuItem 3", g),
            new SeparatorMenuItem(),
            new CheckMenuItem("CheckMenuItem 1"),
            new CheckMenuItem("CheckMenuItem 2"),
            new CheckMenuItem("CheckMenuItem 3", ImageTools.createImageView(16, 16)),
            new SeparatorMenuItem(),
            new CustomMenuItem(new Button("CustomMenuItem 1")),
            new CustomMenuItem(new Button("CustomMenuItem 2 (auto hide)"), true),
            new CustomMenuItem(new Button("CustomMenuItem 3 (auto hide off)"), false)
        );
        Menu m2 = FX.menu(m, "_Submenu");
        FX.item(m2, "Submenu Item 1");
        FX.item(m2, "Submenu Item 2");
        FX.item(m2, "Submenu Item 3");
        FX.item(m2, "Submenu Item 4");
        return m;
    }

    private MenuItem radio(String text, ToggleGroup g) {
        RadioMenuItem mi = new RadioMenuItem(text);
        mi.setToggleGroup(g);
        return mi;
    }

    private Supplier<List<Menu>> createInvisibleDisabled() {
        return () -> {
            ArrayList<Menu> rv = new ArrayList();
            Menu m;
            rv.add(m = mkMenu("1"));
            rv.add(m = mkMenu("2"));
            m.setVisible(false);
            rv.add(m = mkMenu("3"));
            rv.add(m = mkMenu("4"));
            m.setDisable(true);
            rv.add(m = mkMenu("5"));
            return rv;
        };
    }

    private Node createItemsOptions(String name, ObservableList<Menu> items) {
        ObjectSelector<List<Menu>> s = new ObjectSelector<>(name, items::setAll);
        s.addChoiceSupplier("1 Menu", mk(1));
        s.addChoiceSupplier("5 Items", mk(5));
        s.addChoiceSupplier("Invisible/Disabled", createInvisibleDisabled());
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
        control.setSkin(new MenuBarSkin(control));
    }
}

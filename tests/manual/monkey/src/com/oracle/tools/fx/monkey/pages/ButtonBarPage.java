/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Comparator;
import javafx.beans.property.StringProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.ButtonBarSkin;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.TextChoiceOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * ButtonBar Page.
 */
public class ButtonBarPage extends TestPaneBase implements HasSkinnable {
    private final ButtonBar control;

    public ButtonBarPage() {
        super("ButtonBarPage");

        control = new ButtonBar() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };

        OptionPane op = new OptionPane();
        op.option("Buttons:", createButtonsOption());
        op.option("Button Min Width:", DoubleOption.of("buttonMinWidth", control.buttonMinWidthProperty()));
        op.option("Button Order:", createButtonOrderOption("buttonOrder", control.buttonOrderProperty()));
        ControlPropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);
    }

    private Node createButtonsOption() {
        MenuButton m = new MenuButton("Add");
        ArrayList<MenuItem> items = new ArrayList<>();
        for (ButtonData d: ButtonData.values()) {
            String name = d.toString();
            Button b = new Button(name);
            MenuItem mi = new MenuItem(name);
            mi.setOnAction((ev) -> {
                MenuItem remove = new MenuItem("Remove");
                remove.setOnAction((ev2) -> {
                    control.getButtons().remove(b);
                });
                ContextMenu cm = new ContextMenu(remove);
                b.setContextMenu(cm);
                ButtonBar.setButtonData(b, d);
                control.getButtons().add(b);
            });
            items.add(mi);
        }
        items.sort(new Comparator<MenuItem>() {
            @Override
            public int compare(MenuItem a, MenuItem b) {
                String sa = a.getText();
                String sb = b.getText();
                return sa.compareTo(sb);
            }
        });
        m.getItems().setAll(items);
        return m;
    }

    private Node createButtonOrderOption(String name, StringProperty p) {
        TextChoiceOption op = new TextChoiceOption(name, true, p);
        op.addChoice(ButtonBar.BUTTON_ORDER_NONE);
        op.addChoice(ButtonBar.BUTTON_ORDER_LINUX);
        op.addChoice(ButtonBar.BUTTON_ORDER_MAC_OS);
        op.addChoice(ButtonBar.BUTTON_ORDER_WINDOWS);
        return op;
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new ButtonBarSkin(control));
    }
}

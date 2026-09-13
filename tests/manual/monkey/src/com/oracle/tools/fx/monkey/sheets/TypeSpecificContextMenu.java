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
package com.oracle.tools.fx.monkey.sheets;

import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Populates ContextMenu based on the type of the item under cursor.
 */
public class TypeSpecificContextMenu {
    public static void populate(ContextMenu m, Node source) {
        Menu menu = findMenu(source);
        if(menu != null) {
            FX.item(m, "Set Menu Invisible", () -> menu.setVisible(false));
        }
    }

    // MenuBarButton is not accessible, so we can't get an instance of Menu directly.
    // Let's find the menu and use its text and items to determine which one we are looking at.
    private static Menu findMenu(Node n) {
        MenuButton mb = FX.getAncestorOfClass(MenuButton.class, n);
        if (mb != null) {
            MenuBar bar = FX.getAncestorOfClass(MenuBar.class, mb);
            if (bar != null) {
                String text = mb.getText();
                List<MenuItem> items = mb.getItems();
                for (Menu m: bar.getMenus()) {
                    if (Utils.eq(text, m.getText()) && Utils.eq(items, m.getItems())) {
                        return m;
                    }
                }
            }
        }
        return null;
    }
}

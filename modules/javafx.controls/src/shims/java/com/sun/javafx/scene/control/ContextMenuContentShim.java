/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.ContextMenuSkin;

import java.util.Optional;
import javafx.scene.layout.Region;

public class ContextMenuContentShim {

    private ContextMenuContentShim() {
        // no-op
    }

    public static Region get_selectedBackground(ContextMenuContent menu) {
        return menu.selectedBackground;
    }

    public static Menu getOpenSubMenu(ContextMenu menu) {
        ContextMenuContent content = getMenuContent(menu);
        return content.getOpenSubMenu();
    }

    public static Menu getShowingSubMenu(ContextMenu menu) {
        ContextMenuContent content = getMenuContent(menu);
        Menu showingSubMenu = content.getOpenSubMenu();
        ContextMenu subContextMenu = content.getSubMenu();

        while (showingSubMenu != null) {
            content = getMenuContent(subContextMenu);

            Menu newShowingMenu = content == null ? null : content.getOpenSubMenu();
            subContextMenu = content == null ? null : content.getSubMenu();

            if (newShowingMenu == null) {
                break;
            }
        }
        return showingSubMenu;
    }

    public static ObservableList<MenuItem> getShowingMenuItems(ContextMenu menu) {
        ContextMenuContent content = getMenuContent(menu);
        Menu showingSubMenu = content.getOpenSubMenu();
        ContextMenu subContextMenu = content.getSubMenu();

        if (showingSubMenu == null || subContextMenu == null) {
            return menu.getItems();
        }

        while (showingSubMenu != null) {
            content = getMenuContent(subContextMenu);

            Menu newShowingMenu = content == null ? null : content.getOpenSubMenu();
            subContextMenu = content == null ? null : content.getSubMenu();

            if (newShowingMenu == null) {
                break;
            }
        }
        return showingSubMenu.getItems();
    }

    public static Optional<ContextMenuContent> getShowingMenuContent(ContextMenu menu) {
        ContextMenuContent content = getMenuContent(menu);
        Menu showingSubMenu = content.getOpenSubMenu();
        ContextMenu subContextMenu = content.getSubMenu();
        return showingSubMenu != null &&
               subContextMenu != null &&
               subContextMenu.isShowing() ? getShowingMenuContent(subContextMenu) : Optional.of(content);
    }

    private static ContextMenuContent getMenuContent(ContextMenu menu) {
        ContextMenuSkin skin = (ContextMenuSkin) menu.getSkin();
        Node node = skin.getNode();
        if (node instanceof ContextMenuContent) {
            return (ContextMenuContent) node;
        }
        return null;
    }

    public static int getCurrentFocusedIndex(ContextMenu menu) {
//        Optional<Integer> index = getShowingMenuContent(menu).flatMap(content -> Optional.of(content.getCurrentFocusIndex()));
//        return index.orElse(-1);

        Optional<ContextMenuContent> showingMenuContent = getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            return content.getCurrentFocusIndex();
        }

        return -1;
    }

    public static MenuItem getCurrentFocusedItem(ContextMenu menu) {
        ObservableList<MenuItem> showingMenuItems = getShowingMenuItems(menu);

//        Optional<MenuItem> item = getShowingMenuContent(menu)
//                .flatMap(content -> Optional.of(content.getCurrentFocusIndex()))
//                .flatMap(index   -> Optional.of(showingMenuItems.get(index)));
//        return item.orElse(null);

        Optional<ContextMenuContent> showingMenuContent = getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            int currentFocusIndex = content.getCurrentFocusIndex();
            return currentFocusIndex == -1 ? null : showingMenuItems.get(currentFocusIndex);
        }

        return null;
    }

    public static boolean isContextMenuUpArrowVisible(ContextMenu menu) {
        ContextMenuContent content = getMenuContent(menu);
        return content.isUpArrowVisible();
    }

    public static boolean isContextMenuDownArrowVisible(ContextMenu menu) {
        ContextMenuContent content = getMenuContent(menu);
        return content.isDownArrowVisible();
    }

    public static double getContextMenuRowHeight(ContextMenu menu) {
        ContextMenuContent content = getMenuContent(menu);
        return content.getItemsContainer().getChildren().get(0).prefHeight(-1);
    }

}

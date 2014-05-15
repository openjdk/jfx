/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.skin;

import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;

import java.util.Optional;

public class ContextMenuContentRetriever {

    private ContextMenuContentRetriever() {
        // no-op
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
            return showingMenuItems.get(currentFocusIndex);
        }

        return null;
    }

    public static void pressDownKey(ContextMenu menu) {
//        getShowingMenuContent(menu).ifPresent(content -> new KeyEventFirer(content).doDownArrowPress());
        Optional<ContextMenuContent> showingMenuContent = getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            new KeyEventFirer(content).doDownArrowPress();
        }
    }

    public static void pressUpKey(ContextMenu menu) {
//        getShowingMenuContent(menu).ifPresent(content -> new KeyEventFirer(content).doUpArrowPress());
        Optional<ContextMenuContent> showingMenuContent = getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            new KeyEventFirer(content).doUpArrowPress();
        }
    }

    public static void pressLeftKey(ContextMenu menu) {
//        getShowingMenuContent(menu).ifPresent(content -> new KeyEventFirer(content).doLeftArrowPress());
        Optional<ContextMenuContent> showingMenuContent = getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            new KeyEventFirer(content).doLeftArrowPress();
        }
    }

    public static void pressRightKey(ContextMenu menu) {
//        getShowingMenuContent(menu).ifPresent(content -> new KeyEventFirer(content).doRightArrowPress());
        Optional<ContextMenuContent> showingMenuContent = getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            new KeyEventFirer(content).doRightArrowPress();
        }
    }

    public static void pressEnterKey(ContextMenu menu) {
//        getShowingMenuContent(menu).ifPresent(content -> new KeyEventFirer(content).doKeyPress(KeyCode.ENTER));
        Optional<ContextMenuContent> showingMenuContent = getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            new KeyEventFirer(content).doKeyPress(KeyCode.ENTER);
        }
    }

    public static void pressMouseButton(ContextMenu menu) {
        Optional<ContextMenuContent> showingMenuContent = getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent.MenuItemContainer itemContainer =
                    (ContextMenuContent.MenuItemContainer) showingMenuContent.get().selectedBackground;

            MenuItem item = itemContainer.getItem();
            if (item instanceof CustomMenuItem) {
                // If the item is a CustomMenuItem, we fire the event on the
                // content of that CustomMenuItem.
                // Also, note that we firea mouse _clicked_ event, as opposed to
                // a press and release. I'm not sure why this is what the
                // ContextMenuContent code expects, but I didn't want to mess with
                // it at this point.
                Node customContent = ((CustomMenuItem)item).getContent();
                new MouseEventFirer(customContent).fireMouseClicked();
            } else {
                new MouseEventFirer(itemContainer).fireMousePressAndRelease();
            }
        }
    }
}

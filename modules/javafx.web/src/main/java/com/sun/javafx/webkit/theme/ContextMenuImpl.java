/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.theme;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;

import javafx.scene.control.Separator;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.CheckMenuItem;

import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

import com.sun.webkit.ContextMenuItem;

public final class ContextMenuImpl extends com.sun.webkit.ContextMenu {

    private final static PlatformLogger log = PlatformLogger.getLogger(ContextMenuImpl.class.getName());

    private final ObservableList<ContextMenuItem> items =
            FXCollections.observableArrayList();

    @Override protected void show(final ShowContext showContext, int x, int y) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("show at [{0}, {1}]", new Object[] {x, y});
        }
        final ContextMenu popupMenu = new ContextMenu();

        popupMenu.setOnAction(t -> {
            MenuItem item = (MenuItem) t.getTarget();
            log.fine("onAction: item={0}", item);
            showContext.notifyItemSelected(((MenuItemPeer)item).getItemPeer().getAction());
        });

        popupMenu.getItems().addAll(fillMenu());
        PopupMenuImpl.doShow(popupMenu, showContext.getPage(), x, y);
    }

    @Override protected void appendItem(ContextMenuItem item) {
        insertItem(item, items.size());
    }

    @Override protected void insertItem(ContextMenuItem item, int index) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("item={0}, index={1}", new Object[] {item, index});
        }
        if (item == null) {
            return;
        }
        items.remove(item);

        if (items.size() == 0) {
            items.add(item);
        } else {
            items.add(index, item);
        }
    }

    @Override protected int getItemCount() {
        return items.size();
    }

    private MenuItem createMenuItem(ContextMenuItem item) {
        log.fine("item={0}", item);

        if (item.getType() == ContextMenuItem.SUBMENU_TYPE) {
            MenuImpl menu = new MenuImpl(item.getTitle());
            if (item.getSubmenu() != null) {
                menu.getItems().addAll(((ContextMenuImpl)item.getSubmenu()).fillMenu());
            }
            return menu;

        } else if (item.getType() == ContextMenuItem.ACTION_TYPE) {

            MenuItem mi = null;
            if (item.isChecked()) {
                mi = new CheckMenuItemImpl(item);
            } else {
                mi = new MenuItemImpl(item);
            }
            mi.setDisable(!item.isEnabled());
            return mi;

        } else if (item.getType() == ContextMenuItem.SEPARATOR_TYPE) {
            return new SeparatorImpl(item);
        }
        throw new java.lang.IllegalArgumentException("unexpected item type");
    }

    private ObservableList<MenuItem> fillMenu() {
        ObservableList<MenuItem> s = FXCollections.observableArrayList();
        for (ContextMenuItem item: items) {
            s.add(createMenuItem(item));
        }
        return s;
    }

    private interface MenuItemPeer {
        public ContextMenuItem getItemPeer();
    }

    private static final class MenuItemImpl extends MenuItem implements MenuItemPeer {
        private final ContextMenuItem itemPeer;
        private MenuItemImpl(ContextMenuItem itemPeer) { super(itemPeer.getTitle()); this.itemPeer = itemPeer; }
        @Override public ContextMenuItem getItemPeer() { return itemPeer; }
    }

    private static final class CheckMenuItemImpl extends CheckMenuItem implements MenuItemPeer {
        private final ContextMenuItem itemPeer;
        private CheckMenuItemImpl(ContextMenuItem itemPeer) { this.itemPeer = itemPeer; }
        @Override public ContextMenuItem getItemPeer() { return itemPeer; }
    }

    private static final class MenuImpl extends Menu {
        private MenuImpl(String text) { super(text); }
    }

    static final class SeparatorImpl extends MenuItem implements MenuItemPeer {
        private final ContextMenuItem itemPeer;
        SeparatorImpl(ContextMenuItem itemPeer) {
            this.itemPeer = itemPeer;
            setGraphic(new Separator());
            setDisable(true);
        }
        @Override public ContextMenuItem getItemPeer() { return itemPeer; }
    }
}


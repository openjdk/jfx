/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.MenuBarSkin;

public class MenuBarButton extends MenuButton {
    public ChangeListener<Boolean> menuListener;
    public MenuBarSkin menuBarSkin;
    public Menu menu;

    private final ListChangeListener<MenuItem> itemsListener;
    private final ListChangeListener<String> styleClassListener;

    public MenuBarButton(MenuBarSkin menuBarSkin, Menu menu) {
        super(menu.getText(), menu.getGraphic());
        this.menuBarSkin = menuBarSkin;
        setAccessibleRole(AccessibleRole.MENU);

        // listen to changes in menu items & update menuButton items
        menu.getItems().addListener(itemsListener = c -> {
            while (c.next()) {
                getItems().removeAll(c.getRemoved());
                getItems().addAll(c.getFrom(), c.getAddedSubList());
            }
        });
        menu.getStyleClass().addListener(styleClassListener = c -> {
            while(c.next()) {
                for(int i=c.getFrom(); i<c.getTo(); i++) {
                    getStyleClass().add(menu.getStyleClass().get(i));
                }
                for (String str : c.getRemoved()) {
                    getStyleClass().remove(str);
                }
            }
        });
        idProperty().bind(menu.idProperty());
    }

    public MenuBarSkin getMenuBarSkin() {
        return menuBarSkin;
    }

    public void clearHover() {
        setHover(false);
    }

    public void setHover() {
        setHover(true);

            /* Transfer the a11y focus to an item in the menu bar. */
        menuBarSkin.getSkinnable().notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_NODE);
    }

    public void dispose() {
        menu.getItems().removeListener(itemsListener);
        menu.getStyleClass().removeListener(styleClassListener);
        idProperty().unbind();
    }

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case FOCUS_ITEM: return MenuBarButton.this;
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
}

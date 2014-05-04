/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.MenuButton;

import com.sun.javafx.scene.control.behavior.MenuButtonBehavior;

/**
 * Skin for MenuButton Control.
 */
public class MenuButtonSkin extends MenuButtonSkinBase<MenuButton, MenuButtonBehavior> {

    static final String AUTOHIDE = "autoHide";
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new MenuButtonSkin for the given MenuButton
     * 
     * @param menuButton the MenuButton
     */
    public MenuButtonSkin(final MenuButton menuButton) {
        super(menuButton, new MenuButtonBehavior(menuButton));
        // MenuButton's showing does not get updated when autoHide happens,
        // as that hide happens under the covers. So we add to the menuButton's
        // properties map to which the MenuButton can react and update accordingly..
        popup.setOnAutoHide(new EventHandler<Event>() {
            @Override public void handle(Event t) {
                MenuButton menuButton = (MenuButton)getSkinnable();
                // work around for the fact autohide happens twice
                // remove this check when that is fixed.
                if (!menuButton.getProperties().containsKey(AUTOHIDE)) {
                    menuButton.getProperties().put(AUTOHIDE, Boolean.TRUE);
                }
                }
        });
        // request focus on content when the popup is shown
        popup.setOnShown(event -> {
            ContextMenuContent cmContent = (ContextMenuContent)popup.getSkin().getNode();
            if (cmContent != null) cmContent.requestFocus();
        });

        if (menuButton.getOnAction() == null) {
            menuButton.setOnAction(e -> {
                menuButton.show();
            });
        }

        label.setLabelFor(menuButton);
    }
}

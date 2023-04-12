/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;

import com.sun.javafx.scene.control.skin.*;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * The embedded context menu for a text input control.
 * The menu will be displayed horizontally underneath the cursor
 * with the available text editing options i.e. cut, copy, paste, select all.
 *
 */
public class EmbeddedTextContextMenuContent extends StackPane {

    private ContextMenu contextMenu;
    private StackPane pointer;
    private HBox menuBox;

    public EmbeddedTextContextMenuContent(final ContextMenu popupMenu) {
        this.contextMenu = popupMenu;
        this.menuBox = new HBox();
        this.pointer = new StackPane();
        pointer.getStyleClass().add("pointer");

        updateMenuItemContainer();
        getChildren().addAll(pointer, menuBox);

        contextMenu.ownerNodeProperty().addListener(arg0 -> {
            if (contextMenu.getOwnerNode() instanceof TextArea) {
                TextAreaSkin tas = (TextAreaSkin)((TextArea)contextMenu.getOwnerNode()).getSkin();
                tas.getSkinnable().getProperties().addListener(new InvalidationListener() {
                    @Override public void invalidated(Observable arg0) {
                        requestLayout();
                    }
                });
            } else if (contextMenu.getOwnerNode() instanceof TextField) {
                TextFieldSkin tfs = (TextFieldSkin)((TextField)contextMenu.getOwnerNode()).getSkin();
                tfs.getSkinnable().getProperties().addListener(new InvalidationListener() {
                    @Override public void invalidated(Observable arg0) {
                        requestLayout();
                    }
                });
            }
        });

        contextMenu.getItems().addListener((ListChangeListener<MenuItem>) c -> {
            // Listener to items in PopupMenu to update items in PopupMenuContent
            updateMenuItemContainer();
        });
    }

    private void updateMenuItemContainer() {
        menuBox.getChildren().clear();
        for (MenuItem item: contextMenu.getItems()) {
            MenuItemContainer menuItemContainer = new MenuItemContainer(item);
            menuItemContainer.visibleProperty().bind(item.visibleProperty());
            menuBox.getChildren().add(menuItemContainer);
        }
    }

    private void hideAllMenus(MenuItem item) {
        contextMenu.hide();

        Menu parentMenu;
        while ((parentMenu = item.getParentMenu()) != null) {
            parentMenu.hide();
            item = parentMenu;
        }
        if (parentMenu == null && item.getParentPopup() != null) {
            item.getParentPopup().hide();
        }
    }

    @Override protected double computePrefHeight(double width) {
        final double pointerHeight = snapSizeY(pointer.prefHeight(width));
        final double menuBoxHeight = snapSizeY(menuBox.prefHeight(width));
        return snappedTopInset() + pointerHeight + menuBoxHeight + snappedBottomInset();
    }

    @Override protected double computePrefWidth(double height) {
        final double menuBoxWidth = snapSizeX(menuBox.prefWidth(height));
        return snappedLeftInset() + menuBoxWidth + snappedRightInset();
    }

    @Override protected void layoutChildren() {
        final double left = snappedLeftInset();
        final double right = snappedRightInset();
        final double top = snappedTopInset();
        final double width = getWidth() - (left + right);
        final double pointerWidth = snapSizeX(Utils.boundedSize(pointer.prefWidth(-1), pointer.minWidth(-1), pointer.maxWidth(-1)));
        final double pointerHeight = snapSizeY(Utils.boundedSize(pointer.prefWidth(-1), pointer.minWidth(-1), pointer.maxWidth(-1)));
        final double menuBoxWidth = snapSizeX(Utils.boundedSize(menuBox.prefWidth(-1), menuBox.minWidth(-1), menuBox.maxWidth(-1)));
        final double menuBoxHeight = snapSizeY(Utils.boundedSize(menuBox.prefWidth(-1), menuBox.minWidth(-1), menuBox.maxWidth(-1)));
        double sceneX = 0;
        double screenX = 0;
        double pointerX = 0;

        // Get the positions of the cursor from the TextArea/TextField and draw the arrow underneath it.
        Map<Object,Object> properties = null;
        if (contextMenu.getOwnerNode() instanceof TextArea) {
            properties = ((TextArea)contextMenu.getOwnerNode()).getProperties();
        } else if (contextMenu.getOwnerNode() instanceof TextField) {
            properties = ((TextField)contextMenu.getOwnerNode()).getProperties();
        }

        if (properties != null) {
            if (properties.containsKey("CONTEXT_MENU_SCENE_X")) {
                sceneX = Double.valueOf(properties.get("CONTEXT_MENU_SCENE_X").toString());
                properties.remove("CONTEXT_MENU_SCENE_X");
            }

            if (properties.containsKey("CONTEXT_MENU_SCREEN_X")) {
                screenX = Double.valueOf(properties.get("CONTEXT_MENU_SCREEN_X").toString());
                properties.remove("CONTEXT_MENU_SCREEN_X");
            }
        }

        if (sceneX == 0) {
            pointerX = width/2;
        } else {
            pointerX = (screenX - sceneX - contextMenu.getX()) + sceneX;
        }

        pointer.resize(pointerWidth, pointerHeight);
        positionInArea(pointer, pointerX, top, pointerWidth, pointerHeight, 0, HPos.CENTER, VPos.CENTER);
        menuBox.resize(menuBoxWidth, menuBoxHeight);
        positionInArea(menuBox, left, top + pointerHeight, menuBoxWidth, menuBoxHeight, 0, HPos.CENTER, VPos.CENTER);
    }

    class MenuItemContainer extends Button {
        private MenuItem item;

        public MenuItemContainer(MenuItem item){
            getStyleClass().addAll(item.getStyleClass());
            setId(item.getId());
            this.item = item;
            setText(item.getText());
            setStyle(item.getStyle());

            // bind to text property in menu item
            textProperty().bind(item.textProperty());
        }

        public MenuItem getItem() {
            return item;
        }

        @Override public void fire() {
            Event.fireEvent(item, new ActionEvent());
            if (!Boolean.TRUE.equals(item.getProperties().get("refreshMenu"))) {
                hideAllMenus(item);
            }
        }
    }
}

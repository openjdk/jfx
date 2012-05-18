/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class TextInputContextMenuContent extends StackPane {

    private ContextMenu contextMenu;
    private StackPane pointer;
    private HBox menuBox;

    public TextInputContextMenuContent(final ContextMenu popupMenu) {
        this.contextMenu = popupMenu;
        this.menuBox = new HBox();
        this.pointer = new StackPane();
        pointer.getStyleClass().add("pointer");

        updateMenuItemContainer();
        getChildren().addAll(pointer, menuBox);

        contextMenu.ownerNodeProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable arg0) {
                if (contextMenu.getOwnerNode() instanceof TextArea) {
                    TextAreaSkin tas = (TextAreaSkin)((TextArea)contextMenu.getOwnerNode()).getSkin();
                    tas.getProperties().addListener(new InvalidationListener() {
                        @Override public void invalidated(Observable arg0) {
                            requestLayout();
                        }
                    });
                } else if (contextMenu.getOwnerNode() instanceof TextField) {
                    TextFieldSkin tfs = (TextFieldSkin)((TextField)contextMenu.getOwnerNode()).getSkin();
                    tfs.getProperties().addListener(new InvalidationListener() {
                        @Override public void invalidated(Observable arg0) {
                            requestLayout();
                        }
                    });
                }
            }
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
        double top = snapSpace(getInsets().getTop());
        double bottom = snapSpace(getInsets().getBottom());
        double pointerHeight = snapSize(pointer.prefHeight(width));
        double menuBoxHeight = snapSize(menuBox.prefHeight(width));

        return top + pointerHeight + menuBoxHeight + bottom;
    }

    @Override protected void layoutChildren() {
        double left = snapSpace(getInsets().getLeft());
        double right = snapSpace(getInsets().getRight());
        double top = snapSpace(getInsets().getTop());        
        double width = snapSize(getWidth() - (left + right));
        double pointerWidth = snapSize(Utils.boundedSize(pointer.prefWidth(-1), pointer.minWidth(-1), pointer.maxWidth(-1)));
        double pointerHeight = snapSize(Utils.boundedSize(pointer.prefWidth(-1), pointer.minWidth(-1), pointer.maxWidth(-1)));
        double menuBoxWidth = snapSize(Utils.boundedSize(menuBox.prefWidth(-1), menuBox.minWidth(-1), menuBox.maxWidth(-1)));
        double menuBoxHeight = snapSize(Utils.boundedSize(menuBox.prefWidth(-1), menuBox.minWidth(-1), menuBox.maxWidth(-1)));
        double sceneX = 0;
        double screenX = 0;
        double pointerX = 0;

        if (contextMenu.getOwnerNode() instanceof TextArea) {
            TextArea ta = (TextArea)contextMenu.getOwnerNode();
            TextAreaSkin tas = (TextAreaSkin)ta.getSkin();
            sceneX = Double.valueOf(tas.getProperties().get("CONTEXT_MENU_SCENE_X").toString());
            screenX = Double.valueOf(tas.getProperties().get("CONTEXT_MENU_SCREEN_X").toString());
            tas.getProperties().clear();
        } else if (contextMenu.getOwnerNode() instanceof TextField) {
            TextField tf = (TextField)contextMenu.getOwnerNode();
            TextFieldSkin tfs = (TextFieldSkin)tf.getSkin();
            sceneX = Double.valueOf(tfs.getProperties().get("CONTEXT_MENU_SCENE_X").toString());
            screenX = Double.valueOf(tfs.getProperties().get("CONTEXT_MENU_SCREEN_X").toString());
            tfs.getProperties().clear();
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
            hideAllMenus(item);
            super.fire();
        }
    }
}

/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Skin;
import javafx.scene.layout.VBox;


/**
 *
 * @author paru
 */
public class MenuBarMenuButtonRetriever {
    
    // can only access the getNodeForMenu method in MenuBarSkin from this package.
    public static MenuButton getNodeForMenu(MenuBarSkin skin, int i) {
        return skin.getNodeForMenu(i);
    }
    
    public static ContextMenu getSubMenu(ContextMenuContent cmc) {
      return cmc.getSubMenu();
    }
    
    public static Skin getPopupSkin(MenuButton mb) {
        return ((MenuButtonSkinBase)mb.getSkin()).popup.getSkin();
    }
    
    public static ContextMenuContent getMenuContent(MenuButton mb) {
        ContextMenuContent cmc = (ContextMenuContent)getPopupSkin(mb).getNode();
        return cmc;
    }
    
    public static ContextMenuContent getSubMenuContent(ContextMenuContent cmc) {
        ContextMenu cm = cmc.getSubMenu();
        return (cm != null) ? (ContextMenuContent)cm.getSkin().getNode() : null;
    }
    
    public static ContextMenuContent.MenuItemContainer getDisplayNodeForMenuItem(ContextMenuContent cmc, int i) {
        VBox itemsContainer = cmc.getItemsContainer();
        return (i < itemsContainer.getChildren().size()) ? 
            (ContextMenuContent.MenuItemContainer)itemsContainer.getChildren().get(i) : null;
    }
    
    public static void setCurrentFocusedIndex(ContextMenuContent cmc, int i) {
        cmc.setCurrentFocusedIndex(i);
    }
}

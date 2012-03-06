/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import javafx.scene.control.MenuButton;


/**
 *
 * @author paru
 */
public class MenuBarMenuButtonRetriever {
    
    // can only access the getNodeForMenu method in MenuBarSkin from this package.
    public static MenuButton getNodeForMenu(MenuBarSkin skin, int i) {
        return skin.getNodeForMenu(i);
    }
}

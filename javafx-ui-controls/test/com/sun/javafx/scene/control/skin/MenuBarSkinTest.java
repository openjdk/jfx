/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Insets;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This fails with IllegalStateException because of the toolkit's check for the FX application thread
 */
@Ignore
public class MenuBarSkinTest {
    private MenuBar menubar;
    private MenuBarSkinMock skin;

    @Before public void setup() {
        menubar = new MenuBar();
        menubar.getMenus().addAll(new Menu("File"), new Menu("Edit"));
        skin = new MenuBarSkinMock(menubar);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        menubar.setPadding(new Insets(10, 10, 10, 10));
        menubar.setSkin(skin);

    }
    
    @Test public void maxHeightTracksPreferred() {
        menubar.setPrefHeight(100);
        assertEquals(100, menubar.maxHeight(-1), 0);
    }
    
    public static final class MenuBarSkinMock extends MenuBarSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public MenuBarSkinMock(MenuBar menubar) {
            super(menubar);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Insets;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This fails with IllegalStateException because of the toolkit's check for the FX application thread
 */
@Ignore
public class MenuButtonSkinTest {
    private MenuButton menubutton;
    private MenuButtonSkinMock skin;

    @Before public void setup() {
        menubutton = new MenuButton();
        menubutton.getItems().addAll(new MenuItem("Vanilla"), new MenuItem("Chocolate"));
        skin = new MenuButtonSkinMock(menubutton);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        menubutton.setPadding(new Insets(10, 10, 10, 10));
        menubutton.setSkin(skin);

    }

    @Test public void maxWidthTracksPreferred() {
        menubutton.setPrefWidth(500);
        assertEquals(500, menubutton.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        menubutton.setPrefHeight(100);
        assertEquals(100, menubutton.maxHeight(-1), 0);
    }
    
    public static final class MenuButtonSkinMock extends MenuButtonSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public MenuButtonSkinMock(MenuButton menubutton) {
            super(menubutton);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

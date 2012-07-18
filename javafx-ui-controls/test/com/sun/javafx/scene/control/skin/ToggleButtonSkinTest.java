/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Insets;
import javafx.scene.control.ToggleButton;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class ToggleButtonSkinTest {
    private ToggleButton togglebutton;
    private ToggleButtonSkinMock skin;

    @Before public void setup() {
        togglebutton = new ToggleButton("Test");
        skin = new ToggleButtonSkinMock(togglebutton);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        togglebutton.setPadding(new Insets(10, 10, 10, 10));
        togglebutton.setSkin(skin);

    }

    @Test public void maxWidthTracksPreferred() {        
        togglebutton.setPrefWidth(500);
        assertEquals(500, togglebutton.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        togglebutton.setPrefHeight(500);
        assertEquals(500, togglebutton.maxHeight(-1), 0);
    }
    
    public static final class ToggleButtonSkinMock extends ToggleButtonSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ToggleButtonSkinMock(ToggleButton togglebutton) {
            super(togglebutton);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

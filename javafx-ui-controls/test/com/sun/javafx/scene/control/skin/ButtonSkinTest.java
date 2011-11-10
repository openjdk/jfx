/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Insets;
import javafx.scene.control.Button;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class ButtonSkinTest {
    private Button button;
    private ButtonSkinMock skin;

    @Before public void setup() {
        button = new Button("Test");
        skin = new ButtonSkinMock(button);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        skin.setPadding(new Insets(10, 10, 10, 10));
        button.setSkin(skin);

    }

    @Test public void maxWidthTracksPreferred() {
        button.setPrefWidth(500);
        assertEquals(500, button.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        button.setPrefHeight(500);
        assertEquals(500, button.maxHeight(-1), 0);
    }
    
    public static final class ButtonSkinMock extends ButtonSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ButtonSkinMock(Button button) {
            super(button);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

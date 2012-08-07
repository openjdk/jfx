/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Insets;
import javafx.scene.control.RadioButton;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class RadioButtonSkinTest {
    private RadioButton radiobutton;
    private RadioButtonSkinMock skin;

    @Before public void setup() {
        radiobutton = new RadioButton("Test");
        skin = new RadioButtonSkinMock(radiobutton);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        radiobutton.setPadding(new Insets(10, 10, 10, 10));
        radiobutton.setSkin(skin);

    }

    @Test public void maxWidthTracksPreferred() {        
        radiobutton.setPrefWidth(500);
        assertEquals(500, radiobutton.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        radiobutton.setPrefHeight(500);
        assertEquals(500, radiobutton.maxHeight(-1), 0);
    }
    
    public static final class RadioButtonSkinMock extends RadioButtonSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public RadioButtonSkinMock(RadioButton radiobutton) {
            super(radiobutton);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

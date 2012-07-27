/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class CheckBoxSkinTest {
    private CheckBox checkbox;
    private CheckBoxSkinMock skin;

    @Before public void setup() {
        checkbox = new CheckBox("Test");
        skin = new CheckBoxSkinMock(checkbox);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        checkbox.setPadding(new Insets(10, 10, 10, 10));
        checkbox.setSkin(skin);

    }

    @Test public void maxWidthTracksPreferred() {        
        checkbox.setPrefWidth(500);
        assertEquals(500, checkbox.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        checkbox.setPrefHeight(500);
        assertEquals(500, checkbox.maxHeight(-1), 0);
    }
    
    public static final class CheckBoxSkinMock extends CheckBoxSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public CheckBoxSkinMock(CheckBox checkbox) {
            super(checkbox);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

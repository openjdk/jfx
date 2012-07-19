/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Insets;
import javafx.scene.control.ChoiceBox;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This fails with IllegalStateException because of the toolkit's check for the FX application thread
 */
@Ignore
public class ChoiceBoxSkinTest {
    private ChoiceBox choicebox;
    private ChoiceBoxSkinMock skin;

    @Before public void setup() {
        choicebox = new ChoiceBox();
        skin = new ChoiceBoxSkinMock(choicebox);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        choicebox.setPadding(new Insets(10, 10, 10, 10));
        choicebox.setSkin(skin);
    }

    @Test public void maxWidthTracksPreferred() {
        choicebox.setPrefWidth(500);
        assertEquals(500, choicebox.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        choicebox.setPrefHeight(500);
        assertEquals(500, choicebox.maxHeight(-1), 0);
    }
    
    public static final class ChoiceBoxSkinMock extends ChoiceBoxSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ChoiceBoxSkinMock(ChoiceBox choicebox) {
            super(choicebox);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

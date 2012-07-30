/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class HyperlinkSkinTest {
    private Hyperlink hyperlink;
    private HyperlinkSkinMock skin;

    @Before public void setup() {
        hyperlink = new Hyperlink("Test");
        skin = new HyperlinkSkinMock(hyperlink);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        hyperlink.setPadding(new Insets(10, 10, 10, 10));
        hyperlink.setSkin(skin);

    }
    @Test public void maxWidthTracksPreferred() {
        hyperlink.setPrefWidth(500);
        assertEquals(500, hyperlink.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        hyperlink.setPrefHeight(500);
        assertEquals(500, hyperlink.maxHeight(-1), 0);
    }
    
    public static final class HyperlinkSkinMock extends HyperlinkSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public HyperlinkSkinMock(Hyperlink hyperlink) {
            super(hyperlink);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

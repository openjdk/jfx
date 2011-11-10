/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class ScrollBarSkinTest {
    private ScrollBar scrollbar;
    private ScrollBarSkinMock skin;

    @Before public void setup() {
        scrollbar = new ScrollBar();
        skin = new ScrollBarSkinMock(scrollbar);
        scrollbar.setSkin(skin);
    }

    @Test public void onVerticalMaxWidthTracksPreferred() {        
        scrollbar.setOrientation(Orientation.VERTICAL);
        scrollbar.setPrefWidth(100);
        assertEquals(100, scrollbar.maxWidth(-1), 0);
    }

    @Test public void onHorizontalMaxHeightTracksPreferred() {
        scrollbar.setOrientation(Orientation.HORIZONTAL);
        scrollbar.setPrefHeight(100);
        assertEquals(100, scrollbar.maxHeight(-1), 0);
    }
    
    public static final class ScrollBarSkinMock extends ScrollBarSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ScrollBarSkinMock(ScrollBar scrollbar) {
            super(scrollbar);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

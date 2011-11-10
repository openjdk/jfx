/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.scene.control.ProgressBar;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class ProgressBarSkinTest {
    private ProgressBar progressbar;
    private ProgressBarSkinMock skin;

    @Before public void setup() {
        progressbar = new ProgressBar();
        skin = new ProgressBarSkinMock(progressbar);
        progressbar.setSkin(skin);
    }

    @Test public void maxWidthTracksPreferred() {
        progressbar.setPrefWidth(500);
        assertEquals(500, progressbar.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        progressbar.setPrefHeight(500);
        assertEquals(500, progressbar.maxHeight(-1), 0);
    }
    
    public static final class ProgressBarSkinMock extends ProgressBarSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ProgressBarSkinMock(ProgressBar progressbar) {
            super(progressbar);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

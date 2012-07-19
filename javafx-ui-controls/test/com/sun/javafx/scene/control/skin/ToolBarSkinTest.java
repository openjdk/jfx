/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This fails with IllegalStateException because of the toolkit's check for the FX application thread
 */
@Ignore
public class ToolBarSkinTest {
    private ToolBar toolbar;
    private ToolBarSkinMock skin;

    @Before public void setup() {
        toolbar = new ToolBar();
        toolbar.getItems().addAll(new Button("Cut"), new Button("Copy"));
        skin = new ToolBarSkinMock(toolbar);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        toolbar.setPadding(new Insets(10, 10, 10, 10));
        toolbar.setSkin(skin);

    }

    @Test public void horizontalMaxHeightTracksPreferred() {
        toolbar.setOrientation(Orientation.HORIZONTAL);
        toolbar.setPrefHeight(100);
        assertEquals(100, toolbar.maxHeight(-1), 0);
    }

    @Test public void verticalMaxWidthTracksPreferred() {
        toolbar.setOrientation(Orientation.VERTICAL);
        toolbar.setPrefWidth(100);
        assertEquals(100, toolbar.maxWidth(-1), 0);
    }

    public static final class ToolBarSkinMock extends ToolBarSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ToolBarSkinMock(ToolBar toolbar) {
            super(toolbar);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

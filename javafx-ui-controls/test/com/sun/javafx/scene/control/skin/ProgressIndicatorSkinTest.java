/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class ProgressIndicatorSkinTest {
    private ProgressIndicator progressindicator;
    private ProgressIndicatorSkinMock skin;

    @Before public void setup() {
        progressindicator = new ProgressIndicator();
        skin = new ProgressIndicatorSkinMock(progressindicator);
        progressindicator.setSkin(skin);
    }

    @Test public void progressCSSTracksProgressColor() {
        progressindicator.setStyle("-fx-progress-color: green;");

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(progressindicator);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        assertEquals(Color.GREEN, skin.getProgressColor());
    }
    
    public static final class ProgressIndicatorSkinMock extends ProgressIndicatorSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ProgressIndicatorSkinMock(ProgressIndicator progressindicator) {
            super(progressindicator);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}

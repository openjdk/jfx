/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.util;

import java.util.List;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 *
 * 
 */
public abstract class AbstractWindowController {
    
    final private Window owner;
    private Parent root;
    private Scene scene;
    private Stage stage;
    private final double CLAMP_FACTOR = 0.9;

    private final EventHandler<WindowEvent> closeRequestHandler = new EventHandler<WindowEvent>() {
        @Override
        public void handle(WindowEvent event) {
            onCloseRequest(event);
            event.consume();
        }
    };
    
    public AbstractWindowController() {
        this(null);
    }
    
    public AbstractWindowController(Window owner) {
        this.owner = owner;
    }
    
    /**
     * Returns the root FX object of this window.
     * When called the first time, this method invokes {@link #makeRoot()}
     * to build the FX components of the panel.
     * 
     * @return the root object of this window (never null)
     */
    public Parent getRoot() {
        if (root == null) {
            makeRoot();
            assert root != null;
        }
        
        return root;
    }
    
    /**
     * Returns the scene of this window.
     * This method invokes {@link #getRoot()}.
     * When called the first time, it also invokes {@link #controllerDidCreateScene()}
     * just after creating the scene object.
     * 
     * @return the scene object of this window (never null)
     */
    public Scene getScene() {
        assert Platform.isFxApplicationThread();
        
        if (scene == null) {
            scene = new Scene(getRoot());
            controllerDidCreateScene();
        }
        
        return scene;
    }
    
    
    /**
     * Returns the stage of this window.
     * This method invokes {@link #getScene()}.
     * When called the first time, it also invokes {@link #controllerDidCreateStage()}
     * just after creating the stage object.
     * 
     * @return the stage object of this window (never null).
     */
    public Stage getStage() {
        assert Platform.isFxApplicationThread();
        
        if (stage == null) {
            stage = new Stage();
            stage.initOwner(owner);
            stage.setOnCloseRequest(closeRequestHandler);
            stage.setScene(getScene());
            clampWindow();
            stage.sizeToScene();
            controllerDidCreateStage();
        }
        
        return stage;
    }
    
    /**
     * Opens this window and place it in front.
     */
    public void openWindow() {
        assert Platform.isFxApplicationThread();
        
        getStage().show();
        getStage().toFront();
    }
    
    /**
     * Closes this window.
     */
    public void closeWindow() {
        assert Platform.isFxApplicationThread();
        getStage().close();
    }
    
    
    /*
     * To be implemented by subclasses
     */
    
    /**
     * Creates the FX object composing the window content.
     * This routine is called by {@link AbstractWindowController#getRoot}.
     * It *must* invoke {@link AbstractWindowController#setRoot}.
     */
    protected abstract void makeRoot();
    
    public abstract void onCloseRequest(WindowEvent event);
    
    protected void controllerDidCreateScene() {
        assert getRoot() != null;
        assert getRoot().getScene() != null;
        assert getRoot().getScene().getWindow() == null;
    }
    
    protected void controllerDidCreateStage() {
        assert getRoot() != null;
        assert getRoot().getScene() != null;
        assert getRoot().getScene().getWindow() != null;
    }
    
    /*
     * For subclasses
     */
    
    /**
     * Set the root of this panel controller.
     * This routine must be invoked by subclass's makePanel() routine.
     * 
     * @param root the root panel (non null).
     */
    protected  final void setRoot(Parent root) {
        assert root != null;
        this.root = root;
    }
    
    
    /*
     * Private
     */
    
    // See DTL-5928
    // The three approaches below do not provide any resizing, for some reason:
    // (1)
    //            stage.setHeight(newHeight);
    //            stage.setWidth(newWidth);
    // (2)
    //            scene.getWindow().setHeight(newHeight);
    //            scene.getWindow().setWidth(newWidth);
    // (3)
    //            getRoot().resize(newWidth, newHeight);
    //
    // The current implementation raises the point root of layout must be
    // a Region, which is for now acceptable but could perhaps be an issue later.
    private void clampWindow() {
        if (getRoot() instanceof Region) {
            Rectangle2D vBounds = Screen.getPrimary().getVisualBounds();
            double primaryScreenHeight = vBounds.getHeight();
            double primaryScreenWidth = vBounds.getWidth();
            double currentHeight = getRoot().prefHeight(-1);
            double currentWidth = getRoot().prefWidth(-1);

            if (currentHeight > primaryScreenHeight) {
                double newHeight = primaryScreenHeight * CLAMP_FACTOR;
    //            System.out.println("Clamp: new height is " + newHeight);
                assert getRoot() instanceof Region;
                ((Region)getRoot()).setPrefHeight(newHeight);
            }

            if (currentWidth > primaryScreenWidth) {
                double newWidth = primaryScreenWidth * CLAMP_FACTOR;
    //            System.out.println("Clamp: new width is " + newWidth);
                assert getRoot() instanceof Region;
                ((Region)getRoot()).setPrefWidth(newWidth);
            }
        }
    }

    protected Rectangle2D getBiggestViewableRectangle() {
        assert stage != null;
        
        Rectangle2D res;
        
        if (Screen.getScreens().size() == 1) {
            res = Screen.getPrimary().getVisualBounds();
        } else {
            Rectangle2D stageRect = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            List<Screen> screens = Screen.getScreensForRectangle(stageRect);
            
            // The stage is entirely rendered on one screen, which is either the
            // primary one or not, we don't care here.
//            if (screens.size() == 1) {
                res = screens.get(0).getVisualBounds();
//            } else {
                // The stage is spread over several screens.
                // We compute the surface of the stage on each on the involved
                // screen to select the biggest one == still to be implemented.
//                TreeMap<String, Screen> sortedScreens = new TreeMap<>();
//                
//                for (Screen screen : screens) {
//                    computeSurface(screen, stageRect, sortedScreens);
//                }
//                
//                res = sortedScreens.get(sortedScreens.lastKey()).getVisualBounds();
//            }
        }
        
        return res;
    }

    // Compute the percentage of the surface of stageRect which is rendered in
    // the given screen and write the result in sortedScreens (percentage is
    // rounded and turned into a String so that we benefit natural order sorting.
//    private void computeSurface(Screen screen, Rectangle2D stageRect, TreeMap<String, Screen> sortedScreens) {
//        Rectangle2D screenBounds = screen.getVisualBounds();
//        double surfaceX, surfaceY, surfaceW, surfaceH;
//        if (screenBounds.getMinX() < stageRect.getMinX()) {
//            if (screenBounds.getMinX() < 0) {
//                surfaceX = stageRect.getMinX();
//            } else {
//                surfaceX = screenBounds.getMinX();
//            }
//        } else {
//            
//        }
//    }

}

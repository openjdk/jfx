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

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.transform.Transform;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 *
 * p
 */
public abstract class AbstractPopupController {
    
    private Parent root;
    private Popup popup;
    private Node anchor;
    private Window anchorWindow;
    
    /**
     * Returns the root FX object of this popup.
     * When called the first time, this method invokes {@link #makeRoot()}
     * to build the FX components of the panel.
     * 
     * @return the root object of the panel (never null)
     */
    public Parent getRoot() {
        if (root == null) {
            makeRoot();
            assert root != null;
        }
        
        return root;
    }
    
    public Popup getPopup() {
        assert Platform.isFxApplicationThread();
        
        if (popup == null) {
            popup = new Popup();
            popup.getContent().add(getRoot());
            popup.setOnHidden(onHiddenHandler);
            controllerDidCreatePopup();
        }
        
        return popup;
    }
    
    
    public void openWindow(Node anchor) {
        assert Platform.isFxApplicationThread();
        assert anchor != null;
        assert anchor.getScene() != null;
        assert anchor.getScene().getWindow() != null;
        
        this.anchor = anchor;
        this.anchorWindow = anchor.getScene().getWindow();
        
        this.anchor.layoutBoundsProperty().addListener(layoutBoundsListener);
        this.anchor.localToSceneTransformProperty().addListener(localToSceneTransformListener);
        this.anchorWindow.xProperty().addListener(xyListener);
        
        getPopup().show(this.anchor.getScene().getWindow());
        anchorBoundsDidChange();
        updatePopupLocation();
    }
    
    public void closeWindow() {
        assert Platform.isFxApplicationThread();
        getPopup().hide(); 
        // Note : Popup.hide() will invoke onHiddenHandler() which 
        // will remove listeners set by openWindow.
    }
    
    public boolean isWindowOpened() {
        return (popup == null) ? false : popup.isShowing();
    }
    
    public Node getAnchor() {
        return anchor;
    }
    
    
    /*
     * To be implemented by subclasses
     */
    
    /**
     * Creates the FX object composing the panel.
     * This routine is called by {@link AbstractPopupController#getRoot}.
     * It *must* invoke {@link AbstractPanelController#setPanelRoot}.
     * 
     */
    protected abstract void makeRoot();
    
    protected abstract void onHidden(WindowEvent event);
    
    protected abstract void anchorBoundsDidChange();
    
    protected abstract void anchorTransformDidChange();

    protected abstract void anchorXYDidChange();

    protected void controllerDidCreatePopup() {
        assert getRoot() != null;
        assert getRoot().getScene() != null;
    }
    
    protected abstract void updatePopupLocation();
    
    
    /*
     * For subclasses
     */
    
    /**
     * Set the root of this popup controller.
     * This routine must be invoked by subclass's makeRoot() routine.
     * 
     * @param panelRoot the root panel (non null).
     */
    protected  final void setRoot(Parent panelRoot) {
        assert panelRoot != null;
        this.root = panelRoot;
    }
    
    
    /*
     * Private
     */
    
    private final ChangeListener<Bounds> layoutBoundsListener
    = (ov, t, t1) -> anchorBoundsDidChange();


    private final ChangeListener<Transform> localToSceneTransformListener
    = (ov, t, t1) -> anchorTransformDidChange();


    private final ChangeListener<Number> xyListener
    = (ov, t, t1) -> anchorXYDidChange();

    private final EventHandler<WindowEvent> onHiddenHandler = e -> {
        assert anchor != null;

        onHidden(e);

        anchor.layoutBoundsProperty().removeListener(layoutBoundsListener);
        anchor.localToSceneTransformProperty().removeListener(localToSceneTransformListener);
        anchorWindow.xProperty().removeListener(xyListener);
        
        anchor = null;
        anchorWindow = null;
    };
}

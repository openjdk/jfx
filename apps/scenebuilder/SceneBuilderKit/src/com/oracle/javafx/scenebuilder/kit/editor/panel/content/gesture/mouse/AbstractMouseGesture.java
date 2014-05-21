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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;

import javafx.scene.Node;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 *
 * 
 */
public abstract class AbstractMouseGesture extends AbstractGesture {
    
    private MouseEvent mousePressedEvent;
    private MouseEvent lastMouseEvent;
    private Observer observer;
    private boolean mouseDidDrag;
    
    protected abstract void mousePressed();
    protected abstract void mouseDragStarted();
    protected abstract void mouseDragged();
    protected abstract void mouseDragEnded();
    protected abstract void mouseReleased();
    protected abstract void keyEvent(KeyEvent e);
    protected abstract void userDidCancel();

    public AbstractMouseGesture(ContentPanelController contentPanelController) {
        super(contentPanelController);
    }
    
    /*
     * For subclasses
     */
    
    protected MouseEvent getMousePressedEvent() {
        return mousePressedEvent;
    }
    
    protected MouseEvent getLastMouseEvent() {
        return lastMouseEvent;
    }
    
    protected boolean isStarted() {
        return observer != null;
    }
    
    protected boolean isMouseDidDrag() {
        return mouseDidDrag;
    }
    
    /*
     * AbstractGesture
     */
    
    @Override
    public void start(InputEvent e, Observer observer) {
        assert e != null;
        assert e instanceof MouseEvent;
        assert e.getEventType() == MouseEvent.MOUSE_PRESSED;
        assert observer != null;
        assert mouseDidDrag == false;
        
        final Node glassLayer = contentPanelController.getGlassLayer();
        assert glassLayer.getOnDragDetected()== null;
        assert glassLayer.getOnMouseDragged() == null;
        assert glassLayer.getOnMouseReleased() == null;
        assert glassLayer.getOnKeyPressed() == null;
        assert glassLayer.getOnKeyReleased() == null;
        
        glassLayer.setOnDragDetected(e1 -> {
            lastMouseEvent = e1;
            mouseDidDrag = true;
            mouseDragStarted();
            glassLayer.setOnMouseDragged(e2 -> {
                lastMouseEvent = e2;
                mouseDragged();
            });
        });
        glassLayer.setOnMouseReleased(e1 -> {
            lastMouseEvent = e1;
            try {
                if (mouseDidDrag) {
                    try {
                        mouseDragEnded();
                    } finally {
                        glassLayer.setOnMouseDragged(null);
                    }
                }
                mouseReleased();
            } finally {
                performTermination();
            }
        });
        glassLayer.setOnKeyPressed(e1 -> handleKeyPressed(e1));
        glassLayer.setOnKeyReleased(e1 -> handleKeyReleased(e1));
        
        
        this.mousePressedEvent = (MouseEvent)e;
        this.lastMouseEvent = mousePressedEvent;
        this.observer = observer;
        
        try {
            mousePressed();
        } catch(RuntimeException x) {
            performTermination();
            throw x;
        }
    }
    
    
    /*
     * Private
     */
    
    private void handleKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) {
            if (mouseDidDrag) {
                contentPanelController.getGlassLayer().setOnMouseDragged(null);
            }
            userDidCancel();
            performTermination();
        } else {
            keyEvent(e);
        }
    }
    
    
    private void handleKeyReleased(KeyEvent e) {
        keyEvent(e);
    }
    
    
    private void performTermination() {
        final Node glassLayer = contentPanelController.getGlassLayer();
        glassLayer.setOnDragDetected(null);
        glassLayer.setOnMouseReleased(null);
        glassLayer.setOnKeyPressed(null);
        glassLayer.setOnKeyReleased(null);
        
        try {
            observer.gestureDidTerminate(this);
        } finally {
            observer = null;
            mousePressedEvent = null;
            lastMouseEvent = null;
            mouseDidDrag = false;
        }
    }
}

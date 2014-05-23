/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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

package com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.key;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;

import javafx.scene.Node;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyEvent;

/**
 *
 */
public abstract class AbstractKeyGesture extends AbstractGesture {

    private KeyEvent firstKeyPressedEvent;
    private KeyEvent lastKeyEvent;
    private Observer observer;
    
    
    public AbstractKeyGesture(ContentPanelController contentPanelController) {
        super(contentPanelController);
    }

    
    /*
     * For subclasses
     */
    
    protected abstract void keyPressed();
    protected abstract void keyReleased();
    
    protected KeyEvent getFirstKeyPressedEvent() {
        return firstKeyPressedEvent;
    }
    
    protected KeyEvent getLastKeyEvent() {
        return lastKeyEvent;
    }
    
    /*
     * AbstractGesture
     */
    
    @Override
    public void start(InputEvent e, Observer observer) {
        assert e != null;
        assert e instanceof KeyEvent;
        assert e.getEventType() == KeyEvent.KEY_PRESSED;
        assert observer != null;
        
        final Node glassLayer = contentPanelController.getGlassLayer();
        assert glassLayer.getOnKeyPressed() == null;
        assert glassLayer.getOnKeyReleased() == null;
        
        glassLayer.setOnKeyPressed(e1 -> {
            if (e1.getCode() == firstKeyPressedEvent.getCode()) {
                lastKeyEvent = e1;
                try {
                    keyPressed();
                } finally {
                    e1.consume();
                }
            }
        });
        glassLayer.setOnKeyReleased(e1 -> {
            if (e1.getCode() == firstKeyPressedEvent.getCode()) {
                lastKeyEvent = e1;
                try {
                    keyReleased();
                } finally {
                    performTermination();
                    e1.consume();
                }
            }
        });
        
        
        this.firstKeyPressedEvent = (KeyEvent)e;
        this.lastKeyEvent = this.firstKeyPressedEvent;
        this.observer = observer;
        
        try {
            keyPressed();
        } catch(RuntimeException x) {
            performTermination();
            throw x;
        }
    }
    
    
    /*
     * Private
     */
    
    private void performTermination() {
        final Node glassLayer = contentPanelController.getGlassLayer();
        glassLayer.setOnKeyPressed(null);
        glassLayer.setOnKeyReleased(null);
        
        try {
            observer.gestureDidTerminate(this);
        } finally {
            observer = null;
            firstKeyPressedEvent = null;
            lastKeyEvent = null;
        }
    }
    
}

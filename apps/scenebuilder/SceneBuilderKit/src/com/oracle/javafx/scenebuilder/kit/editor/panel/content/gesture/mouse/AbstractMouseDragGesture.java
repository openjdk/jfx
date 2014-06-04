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
import javafx.scene.input.MouseEvent;

/**
 *
 * 
 */
public abstract class AbstractMouseDragGesture extends AbstractGesture {
    
    
    private Observer observer;
    private Node eventTarget;

    public AbstractMouseDragGesture(ContentPanelController contentPanelController) {
        super(contentPanelController);
    }
    
    
    protected abstract void mousePressed(MouseEvent e);
    protected abstract void mouseDragDetected(MouseEvent e);
    protected abstract void mouseReleased(MouseEvent e);
    protected abstract void mouseExited(MouseEvent e);
    
    /*
     * AbstractGesture
     */

    @Override
    public void start(InputEvent e, Observer observer) {
        assert e != null;
        assert e instanceof MouseEvent;
        assert e.getEventType() == MouseEvent.MOUSE_PRESSED;
        assert e.getTarget() instanceof Node;
        assert observer != null;
        
        this.observer = observer;
        this.eventTarget = (Node) e.getTarget();

        assert eventTarget.getOnDragDetected() == null;
        assert eventTarget.getOnMouseReleased() == null;
        assert eventTarget.getOnMouseExited() == null;
        
        eventTarget.setOnDragDetected(e1 -> {
            try {
                mouseDragDetected(e1);
            } finally {
                performTermination();
            }
        });
        eventTarget.setOnMouseReleased(e1 -> {
            try {
                mouseReleased(e1);
            } finally {
                performTermination();
            }
        });
        eventTarget.setOnMouseExited(e1 -> {
            try {
                mouseExited(e1);
            } finally {
                performTermination();
            }
        });
        
        try {
            mousePressed((MouseEvent) e);
        } catch(RuntimeException x) {
            performTermination();
            throw x;
        }
    }
    
    
    /*
     * Private
     */
    
    private void performTermination() {
        eventTarget.setOnDragDetected(null);
        eventTarget.setOnMouseReleased(null);
        eventTarget.setOnMouseExited(null);
        
        try {
            observer.gestureDidTerminate(this);
        } finally {
            observer = null;
            eventTarget = null;
        }
    }
    
}

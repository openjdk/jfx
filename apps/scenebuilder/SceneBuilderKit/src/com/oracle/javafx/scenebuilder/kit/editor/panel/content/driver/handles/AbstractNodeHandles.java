/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.AbstractDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.DiscardGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.ResizeGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.CardinalPoint;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.transform.Transform;

/**
 *
 * 
 */
public abstract class AbstractNodeHandles<T extends Node> extends AbstractGenericHandles<T> {
    
    private final boolean resizable;
    private final DiscardGesture discardGesture;
    
    public AbstractNodeHandles(ContentPanelController contentPanelController,
            FXOMInstance fxomInstance, Class<T> sceneGraphObjectClass) {
        super(contentPanelController, fxomInstance, sceneGraphObjectClass);
        
        final AbstractDriver driver = contentPanelController.lookupDriver(fxomInstance);
        this.resizable = (driver.makeResizer(fxomInstance) != null);
        this.discardGesture = new DiscardGesture(contentPanelController);
        
        if (this.resizable == false) {
            handleNW.setCursor(Cursor.DEFAULT);
            handleNE.setCursor(Cursor.DEFAULT);
            handleSE.setCursor(Cursor.DEFAULT);
            handleSW.setCursor(Cursor.DEFAULT);
        }
    }
    
    public FXOMInstance getFxomInstance() {
        return (FXOMInstance) getFxomObject();
    }
    
    /*
     * AbstractGenericHandles
     */
    @Override
    public Bounds getSceneGraphObjectBounds() {
        return getSceneGraphObject().getLayoutBounds();
    }

    @Override
    public Transform getSceneGraphToSceneTransform() {
        return getSceneGraphObject().getLocalToSceneTransform();
    }

    @Override
    public Point2D sceneGraphObjectToScene(double x, double y) {
        return getSceneGraphObject().localToScene(x,y);
    }

    @Override
    public Point2D sceneToSceneGraphObject(double x, double y) {
        return getSceneGraphObject().sceneToLocal(x,y);
    }

    @Override
    protected void startListeningToSceneGraphObject() {
        startListeningToLayoutBounds(getSceneGraphObject());
        startListeningToLocalToSceneTransform(getSceneGraphObject());
    }

    @Override
    protected void stopListeningToSceneGraphObject() {
        stopListeningToLayoutBounds(getSceneGraphObject());
        stopListeningToLocalToSceneTransform(getSceneGraphObject());
    }

    @Override
    public AbstractGesture findGesture(Node node) {
        final AbstractGesture result;
        
        if (resizable == false) {
            result = discardGesture;
        } else if (node == handleNW) {
            result = new ResizeGesture(getContentPanelController(), 
                    getFxomInstance(), CardinalPoint.NW);
        } else if (node == handleNE) {
            result = new ResizeGesture(getContentPanelController(), 
                    getFxomInstance(), CardinalPoint.NE);
        } else if (node == handleSE) {
            result = new ResizeGesture(getContentPanelController(), 
                    getFxomInstance(), CardinalPoint.SE);
        } else if (node == handleSW) {
            result = new ResizeGesture(getContentPanelController(), 
                    getFxomInstance(), CardinalPoint.SW);
        }  else if (node == handleNN) {
            result = new ResizeGesture(getContentPanelController(), 
                    getFxomInstance(), CardinalPoint.N);
        } else if (node == handleEE) {
            result = new ResizeGesture(getContentPanelController(), 
                    getFxomInstance(), CardinalPoint.E);
        } else if (node == handleSS) {
            result = new ResizeGesture(getContentPanelController(), 
                    getFxomInstance(), CardinalPoint.S);
        } else if (node == handleWW) {
            result = new ResizeGesture(getContentPanelController(), 
                    getFxomInstance(), CardinalPoint.W);
        } else {
            result = null;
        }
        
        return result;
    }
}

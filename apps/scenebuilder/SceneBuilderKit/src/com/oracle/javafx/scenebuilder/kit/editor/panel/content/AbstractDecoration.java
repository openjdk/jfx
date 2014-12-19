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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

/**
 * @treatAsPrivate
 */
public abstract class AbstractDecoration<T> {
    
    /**
     * @treatAsPrivate
     */
    public enum State {
        CLEAN,
        NEEDS_RECONCILE,
        NEEDS_REPLACE
    }
    
    private final ContentPanelController contentPanelController;
    private final FXOMObject fxomObject;
    private final Class<T> sceneGraphClass;
    private final Group rootNode = new Group();
    private T sceneGraphObject;

    
    public AbstractDecoration(ContentPanelController contentPanelController,
            FXOMObject fxomObject, Class<T> sceneGraphClass) {
        assert contentPanelController != null;
        assert fxomObject != null;
        assert fxomObject.getSceneGraphObject() != null;
        assert fxomObject.getFxomDocument() == contentPanelController.getEditorController().getFxomDocument();
        assert sceneGraphClass != null;
        
        this.contentPanelController = contentPanelController;
        this.fxomObject = fxomObject;
        this.sceneGraphClass = sceneGraphClass;
        this.sceneGraphObject = sceneGraphClass.cast(fxomObject.getSceneGraphObject());
        
        this.rootNode.sceneProperty().addListener((ChangeListener<Scene>) (ov, v1, v2) -> rootNodeSceneDidChange());
    }

    public ContentPanelController getContentPanelController() {
        return contentPanelController;
    }
    
    public FXOMObject getFxomObject() {
        return fxomObject;
    }
    
    public T getSceneGraphObject() {
        return sceneGraphObject;
    }
    
    public Group getRootNode() {
        return rootNode;
    }
    
    public State getState() {
        final State result;
        
        if (fxomObject.getSceneGraphObject() == sceneGraphObject) {
            result = State.CLEAN;
        } else if (fxomObject.getSceneGraphObject() == null) {
            // Scene graph object became unresolved !
            result = State.NEEDS_REPLACE;
        } else if (fxomObject.getSceneGraphObject().getClass() == sceneGraphClass) {
            result = State.NEEDS_RECONCILE;
        } else {
            result = State.NEEDS_REPLACE;
        }
        
        return result;
    }
    
    public void reconcile() {
        assert getState() == State.NEEDS_RECONCILE;
        
        stopListeningToSceneGraphObject();
        updateSceneGraphObject();
        startListeningToSceneGraphObject();
        layoutDecoration();
    }
    
    public Point2D sceneGraphObjectToDecoration(double x, double y, boolean snapToPixel) {
        Point2D result = sceneGraphObjectToDecoration(x, y);
        if (snapToPixel) {
            final double rx = Math.round(result.getX());
            final double ry = Math.round(result.getY());
            result = new Point2D(rx, ry);
        }
        return result;
    }
    
    public Transform getSceneGraphObjectToDecorationTransform() {
        final Node proxy = getSceneGraphObjectProxy();
        final SubScene contentSubScene = contentPanelController.getContentSubScene();
        final Transform t0 = proxy.getLocalToSceneTransform();
        final Transform t1 = contentSubScene.getLocalToSceneTransform();
        final Transform t2 = getRootNode().getLocalToSceneTransform();
        final Transform result;
        
        try {
            final Transform i2 = t2.createInverse();
            result = i2.createConcatenation(t1).createConcatenation(t0);
        } catch(NonInvertibleTransformException x) {
            throw new RuntimeException(x);
        }
        
        return result;
    }
    
    public abstract Bounds getSceneGraphObjectBounds();
    public abstract Node getSceneGraphObjectProxy();
    protected abstract void startListeningToSceneGraphObject();
    protected abstract void stopListeningToSceneGraphObject();
    protected abstract void layoutDecoration();
    
    
    /*
     * Utilities for subclasses
     */
    
    public Point2D sceneGraphObjectToDecoration(double x, double y) {
        final Node proxy = getSceneGraphObjectProxy();
        return Deprecation.localToLocal(proxy, x, y, getRootNode());
    }
            
    protected void startListeningToLayoutBounds(Node node) {
        assert node != null;
        node.layoutBoundsProperty().addListener(layoutBoundsListener);
    }
    
    protected void stopListeningToLayoutBounds(Node node) {
        assert node != null;
        node.layoutBoundsProperty().removeListener(layoutBoundsListener);
    }

    protected void startListeningToBoundsInParent(Node node) {
        assert node != null;
        node.boundsInParentProperty().addListener(boundsInParentListener);
    }
    
    protected void stopListeningToBoundsInParent(Node node) {
        assert node != null;
        node.boundsInParentProperty().removeListener(boundsInParentListener);
    }

    protected void startListeningToLocalToSceneTransform(Node node) {
        assert node != null;
        node.localToSceneTransformProperty().addListener(localToSceneTransformListener);
        node.sceneProperty().addListener(sceneListener);
        final SubScene contentSubScene = contentPanelController.getContentSubScene();
        contentSubScene.localToSceneTransformProperty().addListener(localToSceneTransformListener);
    }
    
    protected void stopListeningToLocalToSceneTransform(Node node) {
        assert node != null;
        node.localToSceneTransformProperty().removeListener(localToSceneTransformListener);
        node.sceneProperty().removeListener(sceneListener);
        final SubScene contentSubScene = contentPanelController.getContentSubScene();
        contentSubScene.localToSceneTransformProperty().removeListener(localToSceneTransformListener);
    }
    
    /*
     * Protected
     */

    protected void rootNodeSceneDidChange() {
        if (rootNode.getScene() == null) {
            stopListeningToSceneGraphObject();
        } else {
            startListeningToSceneGraphObject();
            layoutDecoration();
        }
    }
    
    protected void updateSceneGraphObject() {
        this.sceneGraphObject = sceneGraphClass.cast(fxomObject.getSceneGraphObject());
    }
    
    /*
     * Private
     */
    
    private final ChangeListener<Bounds> layoutBoundsListener
        = (ov, v1, v2) -> layoutDecoration();
    
    private final ChangeListener<Bounds> boundsInParentListener
        = (ov, v1, v2) -> layoutDecoration();
    
    private final ChangeListener<Transform> localToSceneTransformListener
        = (ov, v1, v2) -> layoutDecoration(); 
    
    private final ChangeListener<Scene> sceneListener
        = (ov, v1, v2) -> layoutDecoration(); 
}

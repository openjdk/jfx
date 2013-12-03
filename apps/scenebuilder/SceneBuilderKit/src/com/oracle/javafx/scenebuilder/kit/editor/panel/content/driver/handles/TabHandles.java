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
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TabPaneDesignInfoX;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.DebugMouseGesture;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.transform.Transform;

/**
 *
 * 
 */

public class TabHandles extends AbstractGenericHandles<Tab> {
    
    private Node tabNode; // Skin node representing the tab
    
    public TabHandles(ContentPanelController contentPanelController,
            FXOMInstance fxomInstance) {
        super(contentPanelController, fxomInstance, Tab.class);
    }

    public FXOMInstance getFxomInstance() {
        return (FXOMInstance) getFxomObject();
    }
    
    /*
     * AbstractGenericHandles
     */
    @Override
    public Bounds getSceneGraphObjectBounds() {
        assert tabNode != null;
        final Bounds b = tabNode.getLayoutBounds();
        
        // Convert b from tabNode local space to tabPane local space
        final TabPane tabPane = getSceneGraphObject().getTabPane();
        final Point2D min = tabPane.sceneToLocal(tabNode.localToScene(b.getMinX(), b.getMinY()));
        final Point2D max = tabPane.sceneToLocal(tabNode.localToScene(b.getMaxX(), b.getMaxY()));
        return makeBoundingBox(min, max);
    }

    @Override
    public Transform getSceneGraphToSceneTransform() {
        return getSceneGraphObject().getTabPane().getLocalToSceneTransform();
    }

    @Override
    public Point2D sceneGraphObjectToDecoration(double x, double y) {
        final TabPane tabPane = getSceneGraphObject().getTabPane();
        return getRootNode().sceneToLocal(tabPane.localToScene(x, y));
    }


    @Override
    protected void startListeningToSceneGraphObject() {
        assert tabNode == null;
        
        final TabPane tabPane = getSceneGraphObject().getTabPane();
        startListeningToLayoutBounds(tabPane);
        startListeningToLocalToSceneTransform(tabPane);
        
        final TabPaneDesignInfoX di = new TabPaneDesignInfoX();
        tabNode = di.getTabNode(tabPane, getSceneGraphObject());
        startListeningToBoundsInParent(tabNode);
    }

    @Override
    protected void stopListeningToSceneGraphObject() {
        assert tabNode != null;
        
        final TabPane tabPane = getSceneGraphObject().getTabPane();
        stopListeningToLayoutBounds(tabPane);
        stopListeningToLocalToSceneTransform(tabPane);
        stopListeningToBoundsInParent(tabNode);
        
        tabNode = null;
    }

    @Override
    public AbstractGesture findGesture(Node node) {
        return new DebugMouseGesture(getContentPanelController(), "Resize gesture for Tab");
    }
    
    
    /*
     * Private
     */
    
    private static BoundingBox makeBoundingBox(Point2D p1, Point2D p2) {
        return new BoundingBox(
                Math.min(p1.getX(), p2.getX()),
                Math.min(p1.getY(), p2.getY()),
                Math.abs(p2.getX() - p1.getX()),
                Math.abs(p2.getY() - p1.getY()));
    }
}

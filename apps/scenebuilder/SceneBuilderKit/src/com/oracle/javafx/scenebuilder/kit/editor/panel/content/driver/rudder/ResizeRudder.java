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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.rudder;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.shape.Line;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.LineEquation;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;

/**
 *
 * 
 */
public class ResizeRudder extends AbstractRudder<Node> {
    
    private final Line diagonalLine = new Line();

    public ResizeRudder(ContentPanelController contentPanelController, 
            FXOMObject fxomObject) {
        super(contentPanelController, fxomObject, Node.class);
        
        diagonalLine.setMouseTransparent(true);
        diagonalLine.getStyleClass().add("resize-rudder"); //NOI18N
        getRootNode().getChildren().add(diagonalLine);
    }
    
    
    /*
     * AbstractRudder
     */
    @Override
    public Bounds getSceneGraphObjectBounds() {
        return getSceneGraphObject().getLayoutBounds();
    }

    @Override
    public Node getSceneGraphObjectProxy() {
        return getSceneGraphObject();
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
    protected void layoutDecoration() {
        final Bounds b = getSceneGraphObjectBounds();
        
        final boolean snapToPixel = true;
        final Point2D p0 = sceneGraphObjectToDecoration(b.getMinX(), b.getMinY(), snapToPixel);
        final Point2D p1 = sceneGraphObjectToDecoration(b.getMaxX(), b.getMaxY(), snapToPixel);
        
        final LineEquation eq = new LineEquation(p0.getX(), p0.getY(), p1.getX(), p1.getY());
        final double outset = 0.1;
        final Point2D d0 = eq.pointAtP(0.0 - outset);
        final Point2D d1 = eq.pointAtP(1.0 + outset);
        
        diagonalLine.setStartX(d0.getX());
        diagonalLine.setStartY(d0.getY());
        diagonalLine.setEndX(d1.getX());
        diagonalLine.setEndY(d1.getY());
    }


}

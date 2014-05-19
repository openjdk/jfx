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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring;

import java.util.List;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;

/**
 *
 * 
 */
public abstract class AbstractGenericTring<T> extends AbstractTring<T> {

    protected final Path ringPath = new Path();
    private final MoveTo moveTo0 = new MoveTo();
    private final LineTo lineTo1 = new LineTo();
    private final LineTo lineTo2 = new LineTo();
    private final LineTo lineTo3 = new LineTo();
    
    public AbstractGenericTring(ContentPanelController contentPanelController,
            FXOMObject fxomObject, Class<T> sceneGraphClass) {
        super(contentPanelController, fxomObject, sceneGraphClass);
        
        final List<PathElement> ringElements = ringPath.getElements();
        ringElements.add(moveTo0);
        ringElements.add(lineTo1);
        ringElements.add(lineTo2);
        ringElements.add(lineTo3);
        ringElements.add(new ClosePath());
        ringPath.getStyleClass().add(TARGET_RING_CLASS);
        ringPath.setMouseTransparent(true);
        getRootNode().getChildren().add(ringPath);
    }
    
    /*
     * AbstractPring
     */
    
    @Override
    protected void layoutDecoration() {
        final Bounds b = getSceneGraphObjectBounds();
        
        final boolean snapToPixel = true;
        final Point2D p0 = sceneGraphObjectToDecoration(b.getMinX(), b.getMinY(), snapToPixel);
        final Point2D p1 = sceneGraphObjectToDecoration(b.getMaxX(), b.getMinY(), snapToPixel);
        final Point2D p2 = sceneGraphObjectToDecoration(b.getMaxX(), b.getMaxY(), snapToPixel);
        final Point2D p3 = sceneGraphObjectToDecoration(b.getMinX(), b.getMaxY(), snapToPixel);
        
        moveTo0.setX(p0.getX());
        moveTo0.setY(p0.getY());
        lineTo1.setX(p1.getX());
        lineTo1.setY(p1.getY());
        lineTo2.setX(p2.getX());
        lineTo2.setY(p2.getY());
        lineTo3.setX(p3.getX());
        lineTo3.setY(p3.getY());
        
    }
    
    @Override
    public void changeStroke(Paint stroke) {
        ringPath.setStroke(stroke);
    }
}

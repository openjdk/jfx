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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.SplitPaneDesignInfoX;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.AdjustDividerGesture;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 *
 * 
 */
public class SplitPaneHandles extends AbstractNodeHandles<SplitPane> {
    
    private final Group grips = new Group();
    
    public SplitPaneHandles(ContentPanelController contentPanelController,
            FXOMInstance fxomInstance) {
        super(contentPanelController, fxomInstance, SplitPane.class);
        
        getRootNode().getChildren().add(grips); // Above handles
    }
    
    
    /*
     * AbstractNodeHandles
     */
    @Override
    protected void layoutDecoration() {
        super.layoutDecoration();
             
        // Adjusts the number of grip lines to the number of dividers
        adjustGripCount();
        
        // Updates grip positions
        final double[] positions = getSceneGraphObject().getDividerPositions();
        for (int i = 0, count = positions.length; i < count; i++) {
            layoutDivider(i);
        }
    }

    @Override
    public AbstractGesture findGesture(Node node) {
        
        int gripIndex = 0;
        final int gripCount = grips.getChildren().size();
        final List<Node> gripNodes = grips.getChildren();
        while ((gripIndex < gripCount) && (gripNodes.get(gripIndex) != node)) {
            gripIndex++;
        }
        
        final AbstractGesture result;
        if (gripIndex < gripCount) {
            assert gripNodes.get(gripIndex) == node;
            result = new AdjustDividerGesture(getContentPanelController(), 
                    getFxomInstance(), gripIndex);
        } else {
            result = super.findGesture(node);
        }
        
        return result;
    }

    
    /*
     * Private
     */
    
    private void adjustGripCount() {
        final int dividerCount = getSceneGraphObject().getDividerPositions().length;
        final List<Node> gripChildren = grips.getChildren();
        
        while (gripChildren.size() < dividerCount) {
            gripChildren.add(makeGripLine());
        }
        while (gripChildren.size() > dividerCount) {
            gripChildren.remove(gripChildren.size()-1);
        }
    }
    
    private Line makeGripLine() {
        final Line result = new Line();
        result.setStrokeWidth(SELECTION_HANDLES_SIZE);
        result.setStroke(Color.TRANSPARENT);
        switch(getSceneGraphObject().getOrientation()) {
            default:
            case HORIZONTAL:
                result.setCursor(Cursor.H_RESIZE);
                break;
            case VERTICAL:
                result.setCursor(Cursor.V_RESIZE);
                break;
        }
        attachHandles(result);
        return result;
    }
    
    private void layoutDivider(int gripIndex) {
        assert grips.getChildren().get(gripIndex) instanceof Line;
        
        
        /*
         *      HORIZONTAL
         *
         *               startX
         *                endX
         *      +----------+--------------+ startY
         *      |          |              |
         *      |          |              |
         *      |          |              |
         *      |          |              |
         *      |          |              |
         *      |          |              |
         *      |          |              |
         *      +----------+--------------+ endY
         *
         *
         *      VERTICAL
         * 
         *    startX                endX
         *      +--------------------+
         *      |                    |
         *      |                    |
         *      |                    |
         *      +--------------------+ startY endY
         *      |                    |
         *      |                    |
         *      |                    |
         *      |                    |
         *      |                    |
         *      +--------------------+
         */
        
        final SplitPaneDesignInfoX di = new SplitPaneDesignInfoX();
        final double pos = getSceneGraphObject().getDividerPositions()[gripIndex];
        final double xy = di.dividerPositionToSplitPaneLocal(getSceneGraphObject(), pos);
        final Bounds lb = getSceneGraphObject().getLayoutBounds();
        
        final double startX, startY, endX, endY;
        switch(getSceneGraphObject().getOrientation()) {
            default:
            case HORIZONTAL:
                startX = xy;
                startY = lb.getMinY();
                endX = xy;
                endY = lb.getMaxY();
                break;
            case VERTICAL:
                startX = lb.getMinX();
                startY = xy;
                endX = lb.getMaxX();
                endY = xy;
                break;
        }
        
        final boolean snapToPixel = true;
        final Point2D startPoint = sceneGraphObjectToDecoration(startX, startY, snapToPixel);
        final Point2D endPoint = sceneGraphObjectToDecoration(endX, endY, snapToPixel);
        
        final Line gripLine = (Line) grips.getChildren().get(gripIndex);
        gripLine.setStartX(startPoint.getX());
        gripLine.setStartY(startPoint.getY());
        gripLine.setEndX(endPoint.getX());
        gripLine.setEndY(endPoint.getY());
    }
    
    
    /* 
     * Wrapper to avoid the 'leaking this in constructor' warning emitted by NB.
     */
    private void attachHandles(Node node) {
        attachHandles(node, this);
    }
}

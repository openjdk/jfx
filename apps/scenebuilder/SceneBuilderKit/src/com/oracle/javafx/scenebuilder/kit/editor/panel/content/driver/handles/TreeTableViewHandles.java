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
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TreeTableViewDesignInfoX;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.ResizeTreeTableColumnGesture;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 *
 * 
 */
public class TreeTableViewHandles extends AbstractNodeHandles<Node> {
    
    private final Group grips = new Group();
    
    public TreeTableViewHandles(ContentPanelController contentPanelController,
            FXOMInstance fxomInstance) {
        super(contentPanelController, fxomInstance, Node.class);
        assert fxomInstance.getSceneGraphObject() instanceof TreeTableView;
        
        getRootNode().getChildren().add(grips); // Above handles
    }
    
    public TreeTableView<?> getTreeTableView() {
        return (TreeTableView<?>) getSceneGraphObject();
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
        for (int i = 0, count = getTreeTableView().getColumns().size(); i < count; i++) {
            layoutGrip(i);
        }
    }

    @Override
    public AbstractGesture findGesture(Node node) {
        final AbstractGesture result;
        
        final int gripIndex = grips.getChildren().indexOf(node);
        if (gripIndex != -1) {
            final DesignHierarchyMask m = new DesignHierarchyMask(getFxomInstance());
            final FXOMObject columnObject = m.getSubComponentAtIndex(gripIndex);
            assert columnObject instanceof FXOMInstance;
            result = new ResizeTreeTableColumnGesture(getContentPanelController(), 
                    (FXOMInstance) columnObject);
        } else {
            result = super.findGesture(node);
        }
        
        return result;
    }

    
    /*
     * Private
     */
    
    private void adjustGripCount() {
        final int columnCount = getTreeTableView().getColumns().size();
        final List<Node> gripChildren = grips.getChildren();
        
        while (gripChildren.size() < columnCount) {
            gripChildren.add(makeGripLine());
        }
        while (gripChildren.size() > columnCount) {
            gripChildren.remove(gripChildren.size()-1);
        }
    }
    
    private Line makeGripLine() {
        final Line result = new Line();
        result.setStrokeWidth(SELECTION_HANDLES_SIZE);
        result.setStroke(Color.TRANSPARENT);
        result.setCursor(Cursor.H_RESIZE);
        attachHandles(result);
        return result;
    }
    
    private void layoutGrip(int gripIndex) {
        assert grips.getChildren().get(gripIndex) instanceof Line;
        
        final TreeTableColumn<?,?> tc = getTreeTableView().getColumns().get(gripIndex);
        
        if (tc.isVisible()) {
            final TreeTableViewDesignInfoX di = new TreeTableViewDesignInfoX();
            final Bounds b = di.getColumnHeaderBounds(tc);
            final double startX = b.getMaxX();
            final double startY = b.getMinY();
            final double endY = b.getMaxY();

            final boolean snapToPixel = true;
            final Point2D startPoint = sceneGraphObjectToDecoration(startX, startY, snapToPixel);
            final Point2D endPoint = sceneGraphObjectToDecoration(startX, endY, snapToPixel);

            final Line gripLine = (Line) grips.getChildren().get(gripIndex);
            gripLine.setVisible(true);
            gripLine.setManaged(true);
            gripLine.setStartX(startPoint.getX());
            gripLine.setStartY(startPoint.getY());
            gripLine.setEndX(endPoint.getX());
            gripLine.setEndY(endPoint.getY());
        } else {
            final Line gripLine = (Line) grips.getChildren().get(gripIndex);
            gripLine.setVisible(false);
            gripLine.setManaged(false);
        }
    }
    
    
    /* 
     * Wrapper to avoid the 'leaking this in constructor' warning emitted by NB.
     */
    private void attachHandles(Node node) {
        attachHandles(node, this);
    }
}

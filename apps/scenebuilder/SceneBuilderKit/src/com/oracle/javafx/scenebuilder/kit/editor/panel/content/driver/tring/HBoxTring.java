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

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Line;

/**
 *
 * 
 */
public class HBoxTring extends AbstractNodeTring<HBox> {
        
    private final int targetIndex;
    private final Line crackLine = new Line();

    public HBoxTring(ContentPanelController contentPanelController, 
            FXOMInstance fxomInstance, int targetIndex) {
        super(contentPanelController, fxomInstance, HBox.class);
        assert targetIndex >= -1;
        this.targetIndex = targetIndex;
        
        crackLine.getStyleClass().add(TARGET_CRACK_CLASS);
        crackLine.setMouseTransparent(true);
        getRootNode().getChildren().add(0, crackLine);
    }

    
    /*
     * AbstractGenericTring
     */
        
    @Override
    protected void layoutDecoration() {
        
        super.layoutDecoration();
        
        final HBox hbox = getSceneGraphObject();
        final int childCount = hbox.getChildren().size();
        
        if (childCount == 0) {
            // No crack line
            crackLine.setVisible(false);
            
        } else {
            // Computes the crack x
            
            final double crackX;
            final List<Node> children = hbox.getChildren();
            if (targetIndex == -1) {
                final Node child = children.get(childCount-1);
                final Bounds cb = child.localToParent(child.getLayoutBounds());
                crackX = cb.getMaxX();
            } else {
                final Node child = children.get(targetIndex);
                final Bounds cb = child.localToParent(child.getLayoutBounds());
                crackX = cb.getMinX();
            }

            // Updates the crack line
            final boolean snapToPixel = true;
            final Bounds b = getSceneGraphObject().getLayoutBounds();
            final Point2D p0 = sceneGraphObjectToDecoration(crackX, b.getMinY(), snapToPixel);
            final Point2D p1 = sceneGraphObjectToDecoration(crackX, b.getMaxY(), snapToPixel);

            crackLine.setVisible(true);
            crackLine.setStartX(p0.getX());
            crackLine.setStartY(p0.getY());
            crackLine.setEndX(p1.getX());
            crackLine.setEndY(p1.getY());
        }
    }
}

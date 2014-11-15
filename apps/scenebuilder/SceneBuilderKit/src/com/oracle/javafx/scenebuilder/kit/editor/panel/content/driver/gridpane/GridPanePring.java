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

package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.gridpane;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.AbstractPring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.SelectAndMoveInGridGesture;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 *
 */
public class GridPanePring extends AbstractPring<GridPane> {

    private final GridPaneMosaic mosaic 
            = new GridPaneMosaic("pring", //NOI18N
                    true /* shouldShowTray */,
                    false /* shouldCreateSensors */ );
    
    public GridPanePring(ContentPanelController contentPanelController, FXOMInstance fxomInstance) {
        super(contentPanelController, fxomInstance, GridPane.class);
        
        assert fxomInstance.getSceneGraphObject() instanceof GridPane;
        getRootNode().getChildren().add(mosaic.getTopGroup());
    }

    public FXOMInstance getFxomInstance() {
        return (FXOMInstance) getFxomObject();
    }
    
    /*
     * AbstractDecoration
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
        
        if (mosaic.getGridPane() != getSceneGraphObject()) {
            mosaic.setGridPane(getSceneGraphObject());
        } else {
            mosaic.update();
        }
        
        // Mosaic update may have created new trays. Attach this pring to them.
        for (Node node : this.mosaic.getNorthTrayNodes()) {
            attachPring(node);
        }
        for (Node node : this.mosaic.getSouthTrayNodes()) {
            attachPring(node);
        }
        for (Node node : this.mosaic.getEastTrayNodes()) {
            attachPring(node);
        }
        for (Node node : this.mosaic.getWestTrayNodes()) {
            attachPring(node);
        }
        
        // Update mosaic transform
        mosaic.getTopGroup().getTransforms().clear();
        mosaic.getTopGroup().getTransforms().add(getSceneGraphObjectToDecorationTransform());
    }

    /*
     * AbstractPring
     */
    
    @Override
    public void changeStroke(Paint stroke) {
        assert stroke instanceof Color;
        mosaic.setTrayColor((Color) stroke);
    }

    @Override
    public AbstractGesture findGesture(Node node) {
        
        final GridSelectionGroup.Type feature;
        
        int trayIndex = mosaic.getNorthTrayNodes().indexOf(node);
        if (trayIndex != -1) {
            feature = GridSelectionGroup.Type.COLUMN;
        } else {
            trayIndex = mosaic.getSouthTrayNodes().indexOf(node);
            if (trayIndex != -1) {
                feature = GridSelectionGroup.Type.COLUMN;
            } else {
                trayIndex = mosaic.getWestTrayNodes().indexOf(node);
                if (trayIndex != -1) {
                    feature = GridSelectionGroup.Type.ROW;
                } else {
                    trayIndex = mosaic.getEastTrayNodes().indexOf(node);
                    feature = GridSelectionGroup.Type.ROW;
                }
            }
        }
        
        final AbstractGesture result;
        if (trayIndex == -1) {
            result = null;
        } else {
            result = new SelectAndMoveInGridGesture(getContentPanelController(),
                    getFxomInstance(), feature, trayIndex);
        }
        
        return result;
    }
    
    /*
     * Private
     */
    
    /* 
     * Wraper to avoid the 'leaking this in constructor' warning emitted by NB.
     */
    private void attachPring(Node node) {
        if (AbstractPring.lookupPring(node) == null) {
            attachPring(node, this);
        }
    }
    
}

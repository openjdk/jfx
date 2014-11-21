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
package com.oracle.javafx.scenebuilder.kit.editor.job.atomic;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.relocater.AnchorPaneRelocater;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

/**
 *
 */
public class RelocateNodeJob extends Job {

    private final FXOMInstance fxomInstance;
    private double oldLayoutX;
    private double oldLayoutY;
    private Double oldLeftAnchor;
    private Double oldRightAnchor;
    private Double oldTopAnchor;
    private Double oldBottomAnchor;
    
    private double newLayoutX;
    private double newLayoutY;
    private Double newLeftAnchor;
    private Double newRightAnchor;
    private Double newTopAnchor;
    private Double newBottomAnchor;
    
    private final DoublePropertyMetadata layoutXMeta;
    private final DoublePropertyMetadata layoutYMeta;
    private final DoublePropertyMetadata leftAnchorMeta;
    private final DoublePropertyMetadata rightAnchorMeta;
    private final DoublePropertyMetadata topAnchorMeta;
    private final DoublePropertyMetadata bottomAnchorMeta;
    
    public RelocateNodeJob(FXOMInstance fxomInstance, double newLayoutX, double newLayoutY, EditorController editorController) {
        super(editorController);
        
        assert fxomInstance != null;
        assert fxomInstance.getSceneGraphObject() instanceof Node;
        
        this.fxomInstance = fxomInstance;
        this.newLayoutX = newLayoutX; // Root scene coordinates
        this.newLayoutY = newLayoutY; // Root scene coordinates
        
        final Metadata metadata = Metadata.getMetadata();
        final Class<?> sgoClass = fxomInstance.getSceneGraphObject().getClass();
        final PropertyName layoutXName = new PropertyName("layoutX"); //NOI18N
        final PropertyName layoutYName = new PropertyName("layoutY"); //NOI18N
        final PropertyName leftAnchorName   = new PropertyName("leftAnchor",   AnchorPane.class); //NOI18N
        final PropertyName rightAnchorName  = new PropertyName("rightAnchor",  AnchorPane.class); //NOI18N
        final PropertyName topAnchorName    = new PropertyName("topAnchor",    AnchorPane.class); //NOI18N
        final PropertyName bottomAnchorName = new PropertyName("bottomAnchor", AnchorPane.class); //NOI18N
        this.layoutXMeta = (DoublePropertyMetadata) metadata.queryProperty(sgoClass, layoutXName);
        this.layoutYMeta = (DoublePropertyMetadata) metadata.queryProperty(sgoClass, layoutYName);
        this.leftAnchorMeta   = (DoublePropertyMetadata) metadata.queryProperty(sgoClass, leftAnchorName  );
        this.rightAnchorMeta  = (DoublePropertyMetadata) metadata.queryProperty(sgoClass, rightAnchorName );
        this.topAnchorMeta    = (DoublePropertyMetadata) metadata.queryProperty(sgoClass, topAnchorName   );
        this.bottomAnchorMeta = (DoublePropertyMetadata) metadata.queryProperty(sgoClass, bottomAnchorName);
    }

    public FXOMInstance getFxomInstance() {
        return fxomInstance;
    }

    public double getNewLayoutX() {
        return newLayoutX;
    }

    public double getNewLayoutY() {
        return newLayoutY;
    }
    
    public void mergeWith(RelocateNodeJob youngerJob) {
        assert ! (MathUtils.equals(this.newLayoutX, youngerJob.newLayoutX) 
               && MathUtils.equals(this.newLayoutY, youngerJob.newLayoutY));
        this.newLayoutX = youngerJob.newLayoutX;
        this.newLayoutY = youngerJob.newLayoutY;
        updateNewAnchors();
    }
    
    /*
     * Job
     */
    @Override
    public boolean isExecutable() {
        return true;
    }

    @Override
    public void execute() {
        this.oldLayoutX = layoutXMeta.getValue(fxomInstance);
        this.oldLayoutY = layoutYMeta.getValue(fxomInstance);
        this.oldLeftAnchor   = leftAnchorMeta.getValue(fxomInstance);
        this.oldRightAnchor  = rightAnchorMeta.getValue(fxomInstance);
        this.oldTopAnchor    = topAnchorMeta.getValue(fxomInstance);
        this.oldBottomAnchor = bottomAnchorMeta.getValue(fxomInstance);
        
        updateNewAnchors();
        
        redo();
    }

    @Override
    public void undo() {
        this.layoutXMeta.setValue(fxomInstance, oldLayoutX);
        this.layoutYMeta.setValue(fxomInstance, oldLayoutY);
        if (oldLeftAnchor != null) {
            leftAnchorMeta.setValue(fxomInstance, oldLeftAnchor);
        }
        if (oldRightAnchor != null) {
            rightAnchorMeta.setValue(fxomInstance, oldRightAnchor);
        }
        if (oldTopAnchor != null) {
            topAnchorMeta.setValue(fxomInstance, oldTopAnchor);
        }
        if (oldBottomAnchor != null) {
            bottomAnchorMeta.setValue(fxomInstance, oldBottomAnchor);
        }
    }

    @Override
    public void redo() {
        this.layoutXMeta.setValue(fxomInstance, newLayoutX);
        this.layoutYMeta.setValue(fxomInstance, newLayoutY);
        if (newLeftAnchor != null) {
            leftAnchorMeta.setValue(fxomInstance, newLeftAnchor);
        }
        if (newRightAnchor != null) {
            rightAnchorMeta.setValue(fxomInstance, newRightAnchor);
        }
        if (newTopAnchor != null) {
            topAnchorMeta.setValue(fxomInstance, newTopAnchor);
        }
        if (newBottomAnchor != null) {
            bottomAnchorMeta.setValue(fxomInstance, newBottomAnchor);
        }
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName(); // Not expected to reach the user
    }
    
    
    /*
     * Private
     */
    
    private void updateNewAnchors() {
        if ((this.oldLeftAnchor == null) && (this.oldRightAnchor == null)) {
            this.newLeftAnchor = null;
            this.newRightAnchor = null;
        } else {
            final Node sceneGraphNode = (Node)fxomInstance.getSceneGraphObject();
            final Bounds nodeLayoutBounds = sceneGraphNode.getLayoutBounds();
            final Bounds parentLayoutBounds = sceneGraphNode.getParent().getLayoutBounds();
            if (this.oldLeftAnchor != null) {
                this.newLeftAnchor = AnchorPaneRelocater.computeLeftAnchor(parentLayoutBounds, nodeLayoutBounds, newLayoutX);
            } else {
                this.newLeftAnchor = null;
            }
            if (this.oldRightAnchor != null) {
                this.newRightAnchor = AnchorPaneRelocater.computeRightAnchor(parentLayoutBounds, nodeLayoutBounds, newLayoutX);
            } else {
                this.newRightAnchor = null;
            }
        }
        if ((this.oldTopAnchor == null) && (this.oldBottomAnchor == null)) {
            this.newTopAnchor = null;
            this.newBottomAnchor = null;
        } else {
            final Node sceneGraphNode = (Node)fxomInstance.getSceneGraphObject();
            final Bounds nodeLayoutBounds = sceneGraphNode.getLayoutBounds();
            final Bounds parentLayoutBounds = sceneGraphNode.getParent().getLayoutBounds();
            if (this.oldTopAnchor != null) {
                this.newTopAnchor = AnchorPaneRelocater.computeTopAnchor(parentLayoutBounds, nodeLayoutBounds, newLayoutY);
            } else {
                this.newTopAnchor = null;
            }
            if (this.oldBottomAnchor != null) {
                this.newBottomAnchor = AnchorPaneRelocater.computeBottomAnchor(parentLayoutBounds, nodeLayoutBounds, newLayoutY);
            } else {
                this.newBottomAnchor = null;
            }
        }
    }
}

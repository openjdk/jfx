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
package com.oracle.javafx.scenebuilder.kit.editor.job;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import javafx.scene.Node;

/**
 *
 */
public class RelocateNodeJob extends Job {

    private final FXOMInstance fxomInstance;
    private final double oldLayoutX;
    private final double oldLayoutY;
    private double newLayoutX;
    private double newLayoutY;
    private final DoublePropertyMetadata layoutXMeta;
    private final DoublePropertyMetadata layoutYMeta;
    
    public RelocateNodeJob(FXOMInstance fxomInstance, double newLayoutX, double newLayoutY, EditorController editorController) {
        super(editorController);
        
        assert fxomInstance != null;
        assert fxomInstance.getSceneGraphObject() instanceof Node;
        
        this.fxomInstance = fxomInstance;
        this.newLayoutX = newLayoutX;
        this.newLayoutY = newLayoutY;
        
        final Metadata metadata = Metadata.getMetadata();
        final Class<?> sgoClass = fxomInstance.getSceneGraphObject().getClass();
        final PropertyName layoutXName = new PropertyName("layoutX");
        final PropertyName layoutYName = new PropertyName("layoutY");
        this.layoutXMeta = (DoublePropertyMetadata) metadata.queryProperty(sgoClass, layoutXName);
        this.layoutYMeta = (DoublePropertyMetadata) metadata.queryProperty(sgoClass, layoutYName);
        this.oldLayoutX = layoutXMeta.getValue(fxomInstance);
        this.oldLayoutY = layoutYMeta.getValue(fxomInstance);
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
        this.newLayoutX = youngerJob.newLayoutX;
        this.newLayoutY = youngerJob.newLayoutY;
    }
    
    /*
     * Job
     */
    @Override
    public boolean isExecutable() {
        final boolean sameX = MathUtils.equals(newLayoutX, oldLayoutX);
        final boolean sameY = MathUtils.equals(newLayoutY, oldLayoutY);
        return (sameX == false) || (sameY == false);
    }

    @Override
    public void execute() {
        redo();
    }

    @Override
    public void undo() {
        this.layoutXMeta.setValue(fxomInstance, oldLayoutX);
        this.layoutYMeta.setValue(fxomInstance, oldLayoutY);

        final Node sceneGraphNode = (Node) fxomInstance.getSceneGraphObject();
        sceneGraphNode.setLayoutX(oldLayoutX);
        sceneGraphNode.setLayoutY(oldLayoutY);
    }

    @Override
    public void redo() {
        this.layoutXMeta.setValue(fxomInstance, newLayoutX);
        this.layoutYMeta.setValue(fxomInstance, newLayoutY);

        final Node sceneGraphNode = (Node) fxomInstance.getSceneGraphObject();
        sceneGraphNode.setLayoutX(newLayoutX);
        sceneGraphNode.setLayoutY(newLayoutY);
    }

    @Override
    public String getDescription() {
        final StringBuilder result = new StringBuilder();
        result.append("Set layoutX/layoutY on ");
        result.append(fxomInstance.getSceneGraphObject().getClass().getSimpleName());
        return result.toString();
    }
    
}

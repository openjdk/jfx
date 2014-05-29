/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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

package com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.key;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.JobManager;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.RelocateSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.InputEvent;

/**
 *
 */
public class MoveWithKeyGesture extends AbstractKeyGesture {
    
    private double vectorX;
    private double vectorY;

    public MoveWithKeyGesture(ContentPanelController contentPanelController) {
        super(contentPanelController);
        assert RelocateSelectionJob.isSelectionMovable(contentPanelController.getEditorController()); // (1)
    }
    
    /*
     * AbstractKeyGesture
     */
    
    @Override
    protected void keyPressed() {
        
        final double extend = getLastKeyEvent().isShiftDown() ? 10.0 : 1.0;
        final double moveX = extend * vectorX;
        final double moveY = extend * vectorY;

        final Selection selection 
                = contentPanelController.getEditorController().getSelection();
        assert selection.getGroup() instanceof ObjectSelectionGroup; // Because (1)
        final ObjectSelectionGroup osg
                = (ObjectSelectionGroup) selection.getGroup();
        
        /*
         * Updates layoutX/layoutY of the selected scene graph objects.
         */
        for (FXOMObject selectedObject : osg.getFlattenItems()) {
            assert selectedObject.isNode(); // Because (1)
            final Node node = (Node) selectedObject.getSceneGraphObject();
            node.setLayoutX(node.getLayoutX() + moveX);
            node.setLayoutY(node.getLayoutY() + moveY);
        }
    }

    @Override
    protected void keyReleased() {
        keyPressed();
        
        final EditorController editorController
                = contentPanelController.getEditorController();
        final Selection selection 
                = editorController.getSelection();
        assert selection.getGroup() instanceof ObjectSelectionGroup; // Because (1)
        
        final ObjectSelectionGroup osg
                = (ObjectSelectionGroup) selection.getGroup();
        
        // Builds a RelocateSelectionJob
        final Map<FXOMObject, Point2D> locationMap = new HashMap<>();
        for (FXOMObject selectedObject : osg.getItems()) {
            assert selectedObject.isNode(); // Because (1)
            assert selectedObject instanceof FXOMInstance;
            
            final Node node = (Node) selectedObject.getSceneGraphObject();
            final Point2D layoutXY = new Point2D(node.getLayoutX(), node.getLayoutY());
            locationMap.put(selectedObject, layoutXY);
        }
        final RelocateSelectionJob newRelocateJob
                = new RelocateSelectionJob(locationMap, editorController);
        
        // ... and pushes it
        // If the current job is already a RelocateSelectionJob,
        // then we see if the new job can be merged with it.
        final JobManager jobManager = editorController.getJobManager();
        final Job currentJob = jobManager.getCurrentJob();
        if (currentJob instanceof RelocateSelectionJob) {
            final RelocateSelectionJob currentRelocateJob = (RelocateSelectionJob) currentJob;
            if (currentRelocateJob.canBeMergedWith(newRelocateJob)) {
                newRelocateJob.execute();
                currentRelocateJob.mergeWith(newRelocateJob);
            } else {
                jobManager.push(newRelocateJob);
            }
        } else {
            jobManager.push(newRelocateJob);
        }
    }

    /*
     * AbstractGesture
     */
    @Override
    public void start(InputEvent e, Observer observer) {
        super.start(e, observer);

        switch(getFirstKeyPressedEvent().getCode()) {
            case DOWN:
                vectorX = +0.0;
                vectorY = +1.0;
                break;
            case UP:
                vectorX = +0.0;
                vectorY = -1.0;
                break;
            case LEFT:
                vectorX = -1.0;
                vectorY = +0.0;
                break;
            case RIGHT:
                vectorX = +1.0;
                vectorY = +0.0;
                break;
            default:
                assert false : "Unexpected key code" + getFirstKeyPressedEvent().getCode();
                vectorX = 0.0;
                vectorY = 0.0;
                break;
        }
    }
    
    
}

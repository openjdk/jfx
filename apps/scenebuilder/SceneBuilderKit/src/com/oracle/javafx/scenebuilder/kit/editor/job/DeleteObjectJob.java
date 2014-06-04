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
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.RemovePropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.RemovePropertyValueJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import javafx.scene.chart.Axis;

/**
 *
 */
public class DeleteObjectJob extends Job {
    
    private final FXOMObject targetFxomObject;
    private String description; // final but initialized lazily

    private BatchJob subJob;

    public DeleteObjectJob(FXOMObject fxomObject, EditorController editorController) {
        super(editorController);
        
        assert fxomObject != null;
        
        this.targetFxomObject = fxomObject;
    }
    
    
    /*
     * Job
     */

    @Override
    public boolean isExecutable() {
        final boolean result;
        
        if (targetFxomObject == targetFxomObject.getFxomDocument().getFxomRoot()) {
            // targetFxomObject is the root
            result = true;
        } else if (targetFxomObject.getSceneGraphObject() instanceof Axis) {
            // Axis cannot be deleted from their parent Chart
            result = false;
        } else {
            result = (targetFxomObject.getParentProperty() != null);
        }
        
        return result;
    }

    @Override
    public void execute() {
        assert subJob == null;
        
        subJob = new BatchJob(getEditorController(), true /* shouldRefreshSceneGraph */, null);
        
        if (targetFxomObject.getParentProperty() == null) { 
            /*
             * targetFxomObject is the root object
             */
            subJob.addSubJob(new SetDocumentRootJob(null, getEditorController()));
            
        } else {
            
            /*
             * Two cases:
             *    1) targetFxomObject is the last value of its parent property
             *       => the property itself must be removed from its parent instance
             *    2) targetFxomObject is not the last value of its parent property
             *       => targetFxomObject is removed from its parent property
             *
             * Note : in case #1, we also remove targetFxomObject from its 
             *   parent property ; like this, it can safely be reinserted else
             *   in the FXOM tree.
             */

            final FXOMPropertyC parentProperty = targetFxomObject.getParentProperty();
            final FXOMInstance parentInstance = parentProperty.getParentInstance();

            if ((parentProperty.getValues().size() == 1) && (parentInstance != null)) {
                // We make a job for removing the property
                final Job removePropJob = new RemovePropertyJob(
                        targetFxomObject.getParentProperty(), 
                        getEditorController());
                subJob.addSubJob(removePropJob);
            }

            // Then we make a job for removing the value
            final Job removeValueJob = new RemovePropertyValueJob(
                    targetFxomObject, 
                    getEditorController());
            subJob.addSubJob(removeValueJob);
            
        }
        
        // Now execute the batch
        subJob.execute();
        
        assert targetFxomObject.getParentProperty() == null;
    }

    @Override
    public void undo() {
        assert subJob != null;
        subJob.undo();
    }

    @Override
    public void redo() {
        assert subJob != null;
        getEditorController().getSelection().clear();
        subJob.redo();
    }

    @Override
    public String getDescription() {
        if (description == null) {
            final StringBuilder sb = new StringBuilder();

            sb.append("Delete ");

            if (targetFxomObject instanceof FXOMInstance) {
                final Object sceneGraphObject = targetFxomObject.getSceneGraphObject();
                if (sceneGraphObject != null) {
                    sb.append(sceneGraphObject.getClass().getSimpleName());
                } else {
                    sb.append("Unresolved Object");
                }
            } else if (targetFxomObject instanceof FXOMCollection) {
                sb.append("Collection");
            } else if (targetFxomObject instanceof FXOMIntrinsic) {
                sb.append(targetFxomObject.getGlueElement().getTagName());
            } else {
                assert false;
                sb.append(targetFxomObject.getClass().getSimpleName());
            }

            description = sb.toString();
        }
        
        return description;
    }
    
    FXOMObject getTargetFxomObject() {
        return targetFxomObject;
    }
}

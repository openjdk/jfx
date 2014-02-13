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
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.ClearSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIndex;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;

/**
 *
 */
public class TrimSelectionJob extends Job {
    
    private BatchJob batchJob;
    

    public TrimSelectionJob(EditorController editorController) {
        super(editorController);
    }

    
    /*
     * Job
     */
    
    @Override
    public boolean isExecutable() {
        final Selection selection = getEditorController().getSelection();
        final boolean result;
        
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
            if (osg.getItems().size() == 1) {
                // We can trim if:
                //  - object is an FXOMInstance
                //  - object is not already the root
                //  - object is self contained
                final FXOMObject fxomObject = osg.getItems().iterator().next();
                if (fxomObject instanceof FXOMInstance) {
                    final FXOMDocument fxomDocument = fxomObject.getFxomDocument();
                    result = (fxomObject != fxomDocument.getFxomRoot())
                            && FXOMIndex.isSelfContainedObject(fxomObject);
                } else {
                    result = false;
                }
            } else {
                // Cannot trim when multiple objects are selected
                result = false;
            }
        } else {
            // selection.getGroup() instanceof GridSelectionGroup
            //      => cannot trim a selected row/column in a grid pane
            result = false;
        }
        
        return result;
    }

    @Override
    public void execute() {
        assert batchJob == null;
        assert isExecutable(); // (1)
        
        final Selection selection = getEditorController().getSelection();
        assert selection.getGroup() instanceof ObjectSelectionGroup; // Because (1)
        final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
        assert osg.getItems().size() == 1;
        final FXOMObject oldRoot = getEditorController().getFxomDocument().getFxomRoot();
        final FXOMObject candidateRoot = osg.getItems().iterator().next();
        
        /*
         *  This job is composed of subjobs:
         *      0) Remove fx:controller/fx:root (if defined) from the old root object if any
         *      1) Unselect the candidate
         *          => ClearSelectionJob
         *      2) Disconnect the candidate from its existing parent
         *          => DeleteObjectJob
         *      3) Set the candidate as the root of the document
         *          => SetDocumentRootJob
         *      4) Add fx:controller/fx:root (if defined) to the new root object
         */
        
        batchJob = new BatchJob(getEditorController());
        
        assert oldRoot instanceof FXOMInstance;
        boolean isFxRoot = ((FXOMInstance) oldRoot).isFxRoot();
        final String fxController = oldRoot.getFxController();
        // First remove the fx:controller/fx:root from the old root object
        if (isFxRoot) {
            final ToggleFxRootJob fxRootJob = new ToggleFxRootJob(getEditorController());
            batchJob.addSubJob(fxRootJob);
        }
        if (fxController != null) {
            final ModifyFxControllerJob fxControllerJob
                    = new ModifyFxControllerJob(oldRoot, null, getEditorController());
            batchJob.addSubJob(fxControllerJob);
        }
        
        final Job clearSelectionJob = new ClearSelectionJob(getEditorController());
        batchJob.addSubJob(clearSelectionJob);
        
        final Job deleteNewRoot = new DeleteObjectJob(candidateRoot, getEditorController());
        batchJob.addSubJob(deleteNewRoot);
        
        final Job setDocumentRoot = new SetDocumentRootJob(candidateRoot, getEditorController());
        batchJob.addSubJob(setDocumentRoot);

        // Finally add the fx:controller/fx:root to the new root object
        if (isFxRoot) {
            final ToggleFxRootJob fxRootJob = new ToggleFxRootJob(getEditorController());
            batchJob.addSubJob(fxRootJob);
        }
        if (fxController != null) {
            final ModifyFxControllerJob fxControllerJob
                    = new ModifyFxControllerJob(candidateRoot, fxController, getEditorController());
            batchJob.addSubJob(fxControllerJob);
        }

        // Now execute the batch
        batchJob.execute();
    }

    @Override
    public void undo() {
        assert batchJob != null;
        batchJob.undo();
    }

    @Override
    public void redo() {
        assert batchJob != null;
        batchJob.redo();
    }

    @Override
    public String getDescription() {
        return I18N.getString("label.action.edit.trim");
    }
    
}

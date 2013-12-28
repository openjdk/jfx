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
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;

/**
 *
 */
public class SetDocumentRootJob extends Job {

    private final FXOMObject newRoot;
    private FXOMObject oldRoot;
    private BatchJob trimJob;
    
    public SetDocumentRootJob(FXOMObject newRoot, EditorController editorController) {
        super(editorController);
        
        assert editorController.getFxomDocument() != null;
        assert (newRoot == null) || (newRoot.getFxomDocument() == editorController.getFxomDocument());
        
        this.newRoot = newRoot;
    }

    public FXOMObject getNewRoot() {
        return newRoot;
    }
    
    
    /*
     * Job
     */
    
    @Override
    public boolean isExecutable() {
        return newRoot != getEditorController().getFxomDocument().getFxomRoot();
    }

    @Override
    public void execute() {
        assert oldRoot == null;
        assert trimJob == null;
        
        // Saves the current root
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        oldRoot = fxomDocument.getFxomRoot();
        
        // Before setting newRoot as the root of the fxom document,
        // we must remove its static properties. 
        // We create a RemovePropertyJob for each existing static property
        trimJob = new BatchJob(getEditorController(), true /* refreshSceneGraph */, null);
        if (newRoot instanceof FXOMInstance) {
            final FXOMInstance newRootInstance = (FXOMInstance) newRoot;
            final Metadata metadata = Metadata.getMetadata();
            for (FXOMProperty p : newRootInstance.getProperties().values()) {
                if (metadata.isPropertyTrimmingNeeded(p.getName())) {
                    final Job j = new RemovePropertyJob(p, getEditorController());
                    trimJob.addSubJob(j);
                }
            }
        }
        
        // Now execute jobs
        final Selection selection = getEditorController().getSelection();
        selection.beginUpdate();
        fxomDocument.beginUpdate();
        trimJob.execute();
        fxomDocument.setFxomRoot(newRoot);
        fxomDocument.endUpdate();
        selection.endUpdate();
    }

    @Override
    public void undo() {
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();
        
        assert fxomDocument.getFxomRoot() == newRoot;
        
        selection.beginUpdate();
        fxomDocument.beginUpdate();
        fxomDocument.setFxomRoot(oldRoot);
        trimJob.undo();
        fxomDocument.endUpdate();
        selection.endUpdate();
        
        assert fxomDocument.getFxomRoot() == oldRoot;
    }

    @Override
    public void redo() {
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();
        
        assert fxomDocument.getFxomRoot() == oldRoot;
        
        selection.beginUpdate();
        fxomDocument.beginUpdate();
        trimJob.redo();
        fxomDocument.setFxomRoot(newRoot);
        fxomDocument.endUpdate();
        selection.endUpdate();
        
        assert fxomDocument.getFxomRoot() == newRoot;
    }

    @Override
    public String getDescription() {
        // Not expected to reach the user
        return getClass().getSimpleName();
    }
}

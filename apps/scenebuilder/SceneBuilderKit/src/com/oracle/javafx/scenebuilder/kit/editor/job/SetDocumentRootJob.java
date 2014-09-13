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
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.SetFxomRootJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SetDocumentRootJob extends BatchSelectionJob {

    private final FXOMObject newRoot;
    private final boolean usePredefinedSize;
    private final String description;

    public SetDocumentRootJob(FXOMObject newRoot, 
            boolean usePredefinedSize, 
            String description,
            EditorController editorController) {
        super(editorController);

        assert editorController.getFxomDocument() != null;
        assert (newRoot == null) || (newRoot.getFxomDocument() == editorController.getFxomDocument());
        assert description != null;

        this.newRoot = newRoot;
        this.usePredefinedSize = usePredefinedSize;
        this.description = description;
    }
    
    public SetDocumentRootJob(FXOMObject newRoot, EditorController editorController) {
        this(newRoot, false /* usePredefinedSize */, 
                SetDocumentRootJob.class.getSimpleName(), editorController);
    }

    public FXOMObject getNewRoot() {
        return newRoot;
    }

    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result = new ArrayList<>();
        if (newRoot != getEditorController().getFxomDocument().getFxomRoot()) {
            // Before setting newRoot as the root of the fxom document,
            // we must remove its static properties.
            // We create a RemovePropertyJob for each existing static property
            if (newRoot != null) {
                result.add(new PrunePropertiesJob(newRoot, null, getEditorController()));
            }
            
            // Adds job that effectively modifes the root
            result.add(new SetFxomRootJob(newRoot, getEditorController()));
            
            // If need, we add a job for resizing the root object
            if ((newRoot != null) && usePredefinedSize) {
                final DesignHierarchyMask mask = new DesignHierarchyMask(newRoot);
                if (mask.needResizeWhenTopElement()) {
                    result.add(new UsePredefinedSizeJob(getEditorController(), 
                            EditorController.Size.SIZE_DEFAULT, newRoot));
                }
            }
        }
        return result;
    }

    @Override
    protected String makeDescription() {
        return description;
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        if (newRoot == null) {
            return null;
        }
        List<FXOMObject> newObjects = new ArrayList<>();
        newObjects.add(newRoot);
        return new ObjectSelectionGroup(newObjects, newRoot, null);
    }
}

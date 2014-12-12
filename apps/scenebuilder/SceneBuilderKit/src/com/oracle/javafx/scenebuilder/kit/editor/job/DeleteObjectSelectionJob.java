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
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Delete job for ObjectSelectionGroup.
 */
public class DeleteObjectSelectionJob extends BatchSelectionJob {

    public DeleteObjectSelectionJob(EditorController editorController) {
        super(editorController);
    }

    @Override
    protected List<Job> makeSubJobs() {
        final Selection selection = getEditorController().getSelection();
        assert selection.getGroup() instanceof ObjectSelectionGroup;
        final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
        final List<Job> result = new ArrayList<>();
        
        // Next we make one DeleteObjectJob for each selected objects
        int cannotDeleteCount = 0;
        for (FXOMObject candidate : osg.getFlattenItems()) {
            final DeleteObjectJob subJob
                    = new DeleteObjectJob(candidate, getEditorController());
            if (subJob.isExecutable()) {
                result.add(subJob);
            } else {
                cannotDeleteCount++;
            }
        }
        
        // If some objects cannot be deleted, then we clear all to
        // make this job not executable.
        if (cannotDeleteCount >= 1) {
            result.clear();
        }
        
        return result;
    }

    @Override
    protected String makeDescription() {
        final String result;
        final int subJobCount = getSubJobs().size();
        
        switch (subJobCount) {
            case 0:
                result = "Unexecutable Delete"; // NO18N
                break;
            case 1: // one delete
                result = getSubJobs().get(0).getDescription();
                break;
            default:
                result = I18N.getString("label.action.edit.delete.n", subJobCount);
                break;
        }
        
        return result;
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        // Selection emptied
        return null;
    }
}

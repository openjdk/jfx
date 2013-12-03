/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.DeleteColumnJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.DeleteRowJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;

/**
 * Delete job for GridSelectionGroup.
 * This job manages either RemoveRow or RemoveColumn jobs depending on the selection.
 */
public class DeleteGridSelectionJob extends Job {

    private Job subJob;
    private AbstractSelectionGroup selectionSnapshot;

    public DeleteGridSelectionJob(EditorController editorController) {
        super(editorController);
        buildSubJobs();
    }

    /*
     * Job
     */
    @Override
    public boolean isExecutable() {
        return subJob != null && subJob.isExecutable();
    }

    @Override
    public void execute() {
        final Selection selection = getEditorController().getSelection();
        final AbstractSelectionGroup asg = selection.getGroup();
        assert asg instanceof GridSelectionGroup;
        final GridSelectionGroup gsg = (GridSelectionGroup) asg;
        final FXOMObject targetGridPane = gsg.getParentObject();

        try {
            selectionSnapshot = selection.getGroup().clone();
        } catch (CloneNotSupportedException x) {
            // Emergency code
            throw new RuntimeException(x);
        }
        selection.clear();
        selection.beginUpdate();
        subJob.execute();
        selection.select(targetGridPane);
        selection.endUpdate();
    }

    @Override
    public void undo() {
        final Selection selection = getEditorController().getSelection();

        selection.beginUpdate();
        subJob.undo();
        selection.select(selectionSnapshot);
        selection.endUpdate();
    }

    @Override
    public void redo() {
        final Selection selection = getEditorController().getSelection();
        final AbstractSelectionGroup asg = selection.getGroup();
        assert asg instanceof GridSelectionGroup;
        final GridSelectionGroup gsg = (GridSelectionGroup) asg;
        final FXOMObject targetGridPane = gsg.getParentObject();

        selection.clear();
        selection.beginUpdate();
        subJob.redo();
        selection.select(targetGridPane);
        selection.endUpdate();
    }

    @Override
    public String getDescription() {
        return subJob.getDescription();
    }

    /*
     * Private
     */
    private void buildSubJobs() {

        final Selection selection = getEditorController().getSelection();
        assert selection.getGroup() instanceof GridSelectionGroup;

        final GridSelectionGroup gsg = (GridSelectionGroup) selection.getGroup();
        switch (gsg.getType()) {
            case COLUMN:
                subJob = new DeleteColumnJob(getEditorController());
                break;
            case ROW:
                subJob = new DeleteRowJob(getEditorController());
                break;
            default:
                assert false;
                break;
        }
    }
}

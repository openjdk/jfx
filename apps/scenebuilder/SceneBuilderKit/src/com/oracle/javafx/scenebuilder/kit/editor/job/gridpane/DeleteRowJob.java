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
package com.oracle.javafx.scenebuilder.kit.editor.job.gridpane;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.togglegroup.AdjustAllToggleGroupJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Job invoked when removing rows.
 */
public class DeleteRowJob extends Job {

    private BatchJob subJob;
    private FXOMObject targetGridPane;
    private final List<Integer> targetIndexes = new ArrayList<>();
    private String description; // Final but constructed lazily

    public DeleteRowJob(EditorController editorController) {
        super(editorController);
        buildSubJobs();
    }

    @Override
    public boolean isExecutable() {
        return subJob != null && subJob.isExecutable();
    }

    @Override
    public void execute() {
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();

        assert isExecutable(); // (1)
        assert targetIndexes.isEmpty() == false; // Because of (1)

        fxomDocument.beginUpdate();
        subJob.execute();
        fxomDocument.endUpdate();
    }

    @Override
    public void undo() {
        assert subJob != null;
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();

        fxomDocument.beginUpdate();
        subJob.undo();
        fxomDocument.endUpdate();
    }

    @Override
    public void redo() {
        assert subJob != null;
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();

        fxomDocument.beginUpdate();
        subJob.redo();
        fxomDocument.endUpdate();
    }

    @Override
    public String getDescription() {
        if (description == null) {
            buildDescription();
        }

        return description;
    }

    private void buildSubJobs() {

        if (GridPaneJobUtils.canPerformRemove(getEditorController())) { // (1)

            // Create sub job
            subJob = new BatchJob(getEditorController(),
                    true /* shouldUpdateSceneGraph */, null);

            // Retrieve the target GridPane
            final Selection selection = getEditorController().getSelection();
            final AbstractSelectionGroup asg = selection.getGroup();
            assert asg instanceof GridSelectionGroup; // Because of (1)
            final GridSelectionGroup gsg = (GridSelectionGroup) asg;

            targetGridPane = gsg.getParentObject();
            targetIndexes.addAll(gsg.getIndexes());

            // Add sub jobs
            // First remove the row constraints
            final Job removeConstraints = new RemoveRowConstraintsJob(
                    getEditorController(), targetGridPane, targetIndexes);
            subJob.addSubJob(removeConstraints);
            // Then remove the row content
            final Job removeContent = new RemoveRowContentJob(
                    getEditorController(), targetGridPane, targetIndexes);
            subJob.addSubJob(removeContent);
            subJob.addSubJob(new AdjustAllToggleGroupJob(getEditorController()));
            // Finally shift the row content
            moveRowContent();
        }
    }

    private void moveRowContent() {

        assert subJob != null;

        final DesignHierarchyMask targetGridPaneMask
                = new DesignHierarchyMask(targetGridPane);
        final int rowsSize = targetGridPaneMask.getRowsSize();
        final Iterator<Integer> iterator = targetIndexes.iterator();

        int shiftIndex = 0;
        int targetIndex, nextTargetIndex;
        targetIndex = iterator.next();
        while (targetIndex != -1) {
            // Move the rows content :
            // - from the target index 
            // - to the next target index if any or the last row index otherwise
            int fromIndex, toIndex;

            // fromIndex excluded
            // toIndex excluded
            fromIndex = targetIndex + 1;
            if (iterator.hasNext()) {
                nextTargetIndex = iterator.next();
                toIndex = nextTargetIndex - 1;
            } else {
                nextTargetIndex = -1;
                toIndex = rowsSize - 1;
            }

            // When we delete 2 consecutive rows 
            // => no content to move between the 2 rows
            // When we delete the last row 
            // => no row content to move below the last row
            if (nextTargetIndex != (targetIndex + 1)
                    && fromIndex < rowsSize) {
                final int offset = -1 + shiftIndex;
                final List<Integer> indexes
                        = GridPaneJobUtils.getIndexes(fromIndex, toIndex);
                final ReIndexRowContentJob reIndexJob = new ReIndexRowContentJob(
                        getEditorController(), offset, targetGridPane, indexes);
                subJob.addSubJob(reIndexJob);
            }

            targetIndex = nextTargetIndex;
            shiftIndex--;
        }
    }

    private void buildDescription() {
        switch (targetIndexes.size()) {
            case 0:
                description = "Unexecutable Delete"; //NO18N
                break;
            case 1:
                description = "Delete Row"; //NO18N
                break;
            default:
                description = makeMultipleSelectionDescription();
                break;
        }
    }

    private String makeMultipleSelectionDescription() {
        final StringBuilder result = new StringBuilder();

        result.append("Delete ");
        result.append(targetIndexes.size());
        result.append(" Rows");

        return result.toString();
    }
}

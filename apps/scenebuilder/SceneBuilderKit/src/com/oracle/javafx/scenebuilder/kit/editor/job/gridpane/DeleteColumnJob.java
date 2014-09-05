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
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Job invoked when removing columns.
 */
public class DeleteColumnJob extends BatchSelectionJob {

    private FXOMObject targetGridPane;
    private final List<Integer> targetIndexes = new ArrayList<>();

    public DeleteColumnJob(EditorController editorController) {
        super(editorController);
    }

    @Override
    protected List<Job> makeSubJobs() {

        final List<Job> result = new ArrayList<>();

        if (GridPaneJobUtils.canPerformRemove(getEditorController())) { // (1)

            // Retrieve the target GridPane
            final Selection selection = getEditorController().getSelection();
            final AbstractSelectionGroup asg = selection.getGroup();
            assert asg instanceof GridSelectionGroup; // Because of (1)
            final GridSelectionGroup gsg = (GridSelectionGroup) asg;

            targetGridPane = gsg.getParentObject();
            targetIndexes.addAll(gsg.getIndexes());

            // Add sub jobs
            // First remove the column constraints
            final Job removeConstraints = new RemoveColumnConstraintsJob(
                    getEditorController(), targetGridPane, targetIndexes);
            result.add(removeConstraints);
            // Then remove the column content
            final Job removeContent = new RemoveColumnContentJob(
                    getEditorController(), targetGridPane, targetIndexes);
            result.add(removeContent);
            // Finally shift the column content
            result.addAll(moveColumnContent());
        }
        return result;
    }

    @Override
    protected String makeDescription() {
        String result;
        switch (targetIndexes.size()) {
            case 0:
                result = "Unexecutable Delete"; //NO18N
                break;
            case 1:
                result = "Delete Column"; //NO18N
                break;
            default:
                result = makeMultipleSelectionDescription();
                break;
        }
        return result;
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        // Selection emptied
        return null;
    }

    private List<Job> moveColumnContent() {

        final List<Job> result = new ArrayList<>();

        final DesignHierarchyMask targetGridPaneMask
                = new DesignHierarchyMask(targetGridPane);
        final int columnsSize = targetGridPaneMask.getColumnsSize();
        final Iterator<Integer> iterator = targetIndexes.iterator();

        int shiftIndex = 0;
        int targetIndex, nextTargetIndex;
        targetIndex = iterator.next();
        while (targetIndex != -1) {
            // Move the columns content :
            // - from the target index 
            // - to the next target index if any or the last column index otherwise
            int fromIndex, toIndex;

            // fromIndex excluded
            // toIndex excluded
            fromIndex = targetIndex + 1;
            if (iterator.hasNext()) {
                nextTargetIndex = iterator.next();
                toIndex = nextTargetIndex - 1;
            } else {
                nextTargetIndex = -1;
                toIndex = columnsSize - 1;
            }

            // When we delete 2 consecutive columns 
            // => no content to move between the 2 columns
            // When we delete the last column 
            // => no column content to move after the last column
            if (nextTargetIndex != (targetIndex + 1)
                    && fromIndex < columnsSize) {
                final int offset = -1 + shiftIndex;
                final List<Integer> indexes
                        = GridPaneJobUtils.getIndexes(fromIndex, toIndex);
                final ReIndexColumnContentJob reIndexJob = new ReIndexColumnContentJob(
                        getEditorController(), offset, targetGridPane, indexes);
                result.add(reIndexJob);
            }

            targetIndex = nextTargetIndex;
            shiftIndex--;
        }
        return result;
    }

    private String makeMultipleSelectionDescription() {
        final StringBuilder result = new StringBuilder();

        result.append("Delete ");
        result.append(targetIndexes.size());
        result.append(" Columns");

        return result.toString();
    }
}

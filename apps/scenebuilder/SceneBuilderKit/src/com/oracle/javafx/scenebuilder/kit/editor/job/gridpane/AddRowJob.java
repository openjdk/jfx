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
package com.oracle.javafx.scenebuilder.kit.editor.job.gridpane;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.GridPaneJobUtils.Position;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup.Type;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Job invoked when adding rows.
 *
 * This job handles multi-selection as follows :
 * - if multiple GridPanes are selected, no row can be selected.
 * We add the new row to each GP, either at first position (add above)
 * or last position (add below).
 * - if multiple rows are selected, a single GridPane is selected.
 * We add new rows for each selected row, either above or below.
 *
 */
public class AddRowJob extends Job {

    private BatchJob subJob;
    private AbstractSelectionGroup selectionSnapshot;
    // Key = target GridPane instance
    // Value = list of target row indexes for this GridPane
    private final Map<FXOMObject, List<Integer>> targetGridPanes = new HashMap<>();
    private final Position position;

    public AddRowJob(final EditorController editorController, final Position position) {
        super(editorController);
        assert position == Position.ABOVE || position == Position.BELOW;
        this.position = position;
        buildSubJobs();
    }

    @Override
    public boolean isExecutable() {
        return subJob != null && subJob.isExecutable();
    }

    @Override
    public void execute() {
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();

        assert isExecutable(); // (1)
        assert targetGridPanes.isEmpty() == false; // Because of (1)

        try {
            selectionSnapshot = selection.getGroup().clone();
        } catch (CloneNotSupportedException x) {
            // Emergency code
            throw new RuntimeException(x);
        }
        selection.clear();
        selection.beginUpdate();
        fxomDocument.beginUpdate();
        subJob.execute();
        fxomDocument.endUpdate();
        updateSelection();
        selection.endUpdate();
    }

    @Override
    public void undo() {
        assert subJob != null;
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();

        selection.beginUpdate();
        fxomDocument.beginUpdate();
        subJob.undo();
        fxomDocument.endUpdate();
        selection.select(selectionSnapshot);
        selection.endUpdate();
    }

    @Override
    public void redo() {
        assert subJob != null;
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();

        selection.clear();
        selection.beginUpdate();
        fxomDocument.beginUpdate();
        subJob.redo();
        fxomDocument.endUpdate();
        updateSelection();
        selection.endUpdate();
    }

    @Override
    public String getDescription() {
        return "Add Row " + position.name(); //NOI18N
    }

    private void buildSubJobs() {

        if (GridPaneJobUtils.canPerformAdd(getEditorController())) {

            // Create sub job
            subJob = new BatchJob(getEditorController(),
                    true /* shouldUpdateSceneGraph */, null);

            // Populate the target GridPane map
            assert targetGridPanes.isEmpty() == true;
            final List<FXOMObject> objectList
                    = GridPaneJobUtils.getTargetGridPanes(getEditorController());
            for (FXOMObject object : objectList) {
                final List<Integer> indexList
                        = getTargetIndexes(getEditorController(), object);
                targetGridPanes.put(object, indexList);
            }

            // Add sub jobs
            // First add the new row constraints
            final Job addConstraints = new AddRowConstraintsJob(
                    getEditorController(), position, targetGridPanes);
            subJob.addSubJob(addConstraints);
            // Then move the row content
            moveRowContent();
        }
    }

    private void moveRowContent() {

        assert subJob != null;

        for (FXOMObject targetGridPane : targetGridPanes.keySet()) {

            final List<Integer> targetIndexes = targetGridPanes.get(targetGridPane);

            final DesignHierarchyMask mask = new DesignHierarchyMask(targetGridPane);
            final int rowsSize = mask.getRowsSize();
            final Iterator<Integer> iterator = targetIndexes.iterator();

            int shiftIndex = 0;
            int targetIndex = iterator.next();
            while (targetIndex != -1) {
                // Move the rows content :
                // - from the target index 
                // - to the next target index if any or the last row index otherwise
                int fromIndex, toIndex;

                switch (position) {
                    case ABOVE:
                        // fromIndex included
                        // toIndex excluded
                        fromIndex = targetIndex;
                        if (iterator.hasNext()) {
                            targetIndex = iterator.next();
                            toIndex = targetIndex - 1;
                        } else {
                            targetIndex = -1;
                            toIndex = rowsSize - 1;
                        }
                        break;
                    case BELOW:
                        // fromIndex excluded
                        // toIndex included
                        fromIndex = targetIndex + 1;
                        if (iterator.hasNext()) {
                            targetIndex = iterator.next();
                            toIndex = targetIndex;
                        } else {
                            targetIndex = -1;
                            toIndex = rowsSize - 1;
                        }
                        break;
                    default:
                        assert false;
                        return;
                }

                // If fromIndex >= rowsSize, we are below the last existing row 
                // => no row content to move
                if (fromIndex < rowsSize) {
                    final int offset = 1 + shiftIndex;
                    final List<Integer> indexes
                            = GridPaneJobUtils.getIndexes(fromIndex, toIndex);
                    final ReIndexRowContentJob reIndexJob = new ReIndexRowContentJob(
                            getEditorController(), offset, targetGridPane, indexes);
                    subJob.addSubJob(reIndexJob);
                }

                shiftIndex++;
            }
        }
    }

    private void updateSelection() {
        final Selection selection = getEditorController().getSelection();
        // Update new selection :
        // - if there is more than 1 GridPane, we select the GridPane instances
        // - if there is a single GridPane, we select the added rows
        if (targetGridPanes.size() > 1) {
            selection.select(targetGridPanes.keySet());
        } else {
            assert targetGridPanes.size() == 1;
            final FXOMInstance targetGridPane
                    = (FXOMInstance) targetGridPanes.keySet().iterator().next();
            final List<Integer> targetIndexes = targetGridPanes.get(targetGridPane);
            assert targetIndexes.size() >= 1;
            final List<Integer> addedIndexes
                    = GridPaneJobUtils.getAddedIndexes(targetIndexes, position);

            // Selection has been cleared at execution time
            assert selection.isEmpty();
            // Select added rows
            for (int addedIndex : addedIndexes) {
                // Selection is empty => just toggle selection
                selection.toggleSelection(targetGridPane,
                        GridSelectionGroup.Type.ROW, addedIndex);
            }
        }
    }

    /**
     * Returns the list of target row indexes for the specified GridPane
     * instance.
     *
     * @return the list of target indexes
     */
    private List<Integer> getTargetIndexes(
            final EditorController editorController,
            final FXOMObject targetGridPane) {

        final Selection selection = editorController.getSelection();
        final AbstractSelectionGroup asg = selection.getGroup();

        final List<Integer> result = new ArrayList<>();

        // Selection == GridPane rows
        // => return the list of selected rows
        if (asg instanceof GridSelectionGroup
                && ((GridSelectionGroup) asg).getType() == Type.ROW) {
            final GridSelectionGroup gsg = (GridSelectionGroup) asg;
            result.addAll(gsg.getIndexes());
        } //
        // Selection == GridPanes or Selection == GridPane columns
        // => return either the first (ABOVE) or the last (BELOW) row index
        else {
            switch (position) {
                case ABOVE:
                    result.add(0);
                    break;
                case BELOW:
                    final DesignHierarchyMask mask
                            = new DesignHierarchyMask(targetGridPane);
                    final int size = mask.getRowsSize();
                    result.add(size - 1);
                    break;
                default:
                    assert false;
                    break;
            }
        }

        return result;
    }
}

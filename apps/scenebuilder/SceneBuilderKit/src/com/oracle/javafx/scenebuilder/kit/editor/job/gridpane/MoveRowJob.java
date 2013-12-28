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
import com.oracle.javafx.scenebuilder.kit.editor.job.DeleteObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.GridPaneJobUtils.Position;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.AddPropertyValueJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.RowConstraints;

/**
 * Job invoked when moving rows ABOVE or BELOW.
 */
public class MoveRowJob extends Job {

    private BatchJob subJob;
    private AbstractSelectionGroup selectionSnapshot;
    private FXOMObject targetGridPane;
    private final List<Integer> targetIndexes = new ArrayList<>();
    private final Position position;

    public MoveRowJob(final EditorController editorController, final Position position) {
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
        assert targetIndexes.isEmpty() == false; // Because of (1)

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
        return "Move Row " + position.name(); //NOI18N
    }

    private void buildSubJobs() {

        if (GridPaneJobUtils.canPerformMove(getEditorController(), position)) {

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
            // First move the row constraints
            moveRowConstraints();
            // Then move the row content
            moveRowContent();
        }
    }

    private void moveRowConstraints() {

        assert subJob != null;

        // Retrieve the constraints property for the specified target GridPane
        final PropertyName propertyName = new PropertyName("rowConstraints"); //NOI18N
        assert targetGridPane instanceof FXOMInstance;
        FXOMProperty constraintsProperty
                = ((FXOMInstance) targetGridPane).getProperties().get(propertyName);
        // GridPane has no constraints property => no constraints to move
        if (constraintsProperty == null) {
            return;
        }

        final DesignHierarchyMask mask = new DesignHierarchyMask(targetGridPane);
        for (int targetIndex : targetIndexes) {

            final int positionIndex;
            switch (position) {
                case ABOVE:
                    positionIndex = targetIndex - 1;
                    break;
                case BELOW:
                    positionIndex = targetIndex + 1;
                    break;
                default:
                    assert false;
                    return;
            }

            // Retrieve the target constraints
            final FXOMObject targetConstraints
                    = mask.getRowConstraintsAtIndex(targetIndex);

            // If the target index is associated to an existing constraints value :
            // we remove the target constraints and add it back at new position
            // No need to move the constraints of the row above/below :
            // indeed, they are automatically shifted while updating the target ones 
            if (targetConstraints != null) {
                // First remove current target constraints
                final Job removeValueJob = new DeleteObjectJob(
                        targetConstraints,
                        getEditorController());
                subJob.addSubJob(removeValueJob);

                // Then add the target constraints at new positionIndex
                final Job addValueJob = new AddPropertyValueJob(
                        targetConstraints,
                        (FXOMPropertyC) constraintsProperty,
                        positionIndex, getEditorController());
                subJob.addSubJob(addValueJob);
            }//
            // The target index is not associated to an existing constraints value :
            // we may need to move the constraints above the target one if any
            else if (position == Position.ABOVE) {
                // Retrieve the constraints above the target one
                final FXOMObject aboveConstraints
                        = mask.getRowConstraintsAtIndex(targetIndex - 1);

                // The index above is associated to an existing constraints value :
                // we insert a new constraints with default values at the position index
                if (aboveConstraints != null) {
                    // Create new empty constraints for the target row
                    final FXOMInstance addedConstraints = makeRowConstraintsInstance();
                    final Job addValueJob = new AddPropertyValueJob(
                            addedConstraints,
                            (FXOMPropertyC) constraintsProperty,
                            positionIndex, getEditorController());
                    subJob.addSubJob(addValueJob);
                }
            }
        }
    }

    private void moveRowContent() {

        assert subJob != null;

        for (int targetIndex : targetIndexes) {

            switch (position) {
                case ABOVE:
                    // First move the target row content
                    subJob.addSubJob(new ReIndexRowContentJob(
                            getEditorController(),
                            -1, targetGridPane, targetIndex));
                    int aboveIndex = targetIndex - 1;
                    // Then move the content of the row above the target one
                    // If the index above is not part of the target indexes (selected indexes),
                    // we move the row content as many times as consecutive target indexes
                    if (targetIndexes.contains(aboveIndex) == false) {
                        int shiftIndex = 1;
                        while (targetIndexes.contains(targetIndex + shiftIndex)) {
                            shiftIndex++;
                        }
                        subJob.addSubJob(new ReIndexRowContentJob(
                                getEditorController(),
                                shiftIndex, targetGridPane, aboveIndex));
                    }
                    break;
                case BELOW:
                    // First move the target row content
                    subJob.addSubJob(new ReIndexRowContentJob(
                            getEditorController(),
                            +1, targetGridPane, targetIndex));
                    int belowIndex = targetIndex + 1;
                    // Then move the content of the row below the target one
                    // If the index below is not part of the target indexes (selected indexes),
                    // we move the row content as many times as consecutive target indexes
                    if (targetIndexes.contains(belowIndex) == false) {
                        int shiftIndex = -1;
                        while (targetIndexes.contains(targetIndex + shiftIndex)) {
                            shiftIndex--;
                        }
                        subJob.addSubJob(new ReIndexRowContentJob(
                                getEditorController(),
                                shiftIndex, targetGridPane, belowIndex));
                    }
                    break;
                default:
                    assert false;
                    return;
            }
        }
    }

    private FXOMInstance makeRowConstraintsInstance() {

        // Create new constraints instance
        final FXOMDocument newDocument = new FXOMDocument();
        final FXOMInstance result
                = new FXOMInstance(newDocument, RowConstraints.class);
        newDocument.setFxomRoot(result);
        result.moveToFxomDocument(getEditorController().getFxomDocument());

        return result;
    }

    private void updateSelection() {
        final Selection selection = getEditorController().getSelection();

        // Selection has been cleared at execution time
        assert selection.isEmpty();
        for (int targetIndex : targetIndexes) {
            int positionIndex = position == Position.ABOVE
                    ? targetIndex - 1 : targetIndex + 1;

            // Selection is empty => just toggle selection
            selection.toggleSelection((FXOMInstance) targetGridPane,
                    GridSelectionGroup.Type.ROW, positionIndex);
        }
    }
}

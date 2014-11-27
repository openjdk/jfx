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
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.GridPaneJobUtils.Position;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.AddPropertyValueJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RemoveObjectJob;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.layout.ColumnConstraints;

/**
 * Job invoked when moving columns BEFORE or AFTER.
 */
public class MoveColumnJob extends BatchSelectionJob {

    private FXOMObject targetGridPane;
    private final List<Integer> targetIndexes = new ArrayList<>();
    private final GridPaneJobUtils.Position position;

    public MoveColumnJob(final EditorController editorController, final Position position) {
        super(editorController);
        assert position == Position.BEFORE || position == Position.AFTER;
        this.position = position;
    }

    @Override
    protected List<Job> makeSubJobs() {

        final List<Job> result = new ArrayList<>();

        if (GridPaneJobUtils.canPerformMove(getEditorController(), position)) {

            // Retrieve the target GridPane
            final Selection selection = getEditorController().getSelection();
            final AbstractSelectionGroup asg = selection.getGroup();
            assert asg instanceof GridSelectionGroup; // Because of (1)
            final GridSelectionGroup gsg = (GridSelectionGroup) asg;

            targetGridPane = gsg.getParentObject();
            targetIndexes.addAll(gsg.getIndexes());

            // Add sub jobs
            // First move the column constraints
            result.addAll(moveColumnConstraints());
            // Then move the column content
            result.addAll(moveColumnContent());
        }
        return result;
    }

    @Override
    protected String makeDescription() {
        return "Move Column " + position.name(); //NOI18N
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        final Set<Integer> movedIndexes = new HashSet<>();
        for (int targetIndex : targetIndexes) {
            int movedIndex = position == Position.BEFORE
                    ? targetIndex - 1 : targetIndex + 1;
            movedIndexes.add(movedIndex);
        }
        return new GridSelectionGroup(targetGridPane, GridSelectionGroup.Type.COLUMN, movedIndexes);
    }

    private List<Job> moveColumnConstraints() {

        final List<Job> result = new ArrayList<>();

        // Retrieve the constraints property for the specified target GridPane
        final PropertyName propertyName = new PropertyName("columnConstraints"); //NOI18N
        assert targetGridPane instanceof FXOMInstance;
        FXOMProperty constraintsProperty
                = ((FXOMInstance) targetGridPane).getProperties().get(propertyName);
        // GridPane has no constraints property => no constraints to move
        if (constraintsProperty == null) {
            return result;
        }

        final DesignHierarchyMask mask = new DesignHierarchyMask(targetGridPane);
        for (int targetIndex : targetIndexes) {

            final int positionIndex;
            switch (position) {
                case BEFORE:
                    positionIndex = targetIndex - 1;
                    break;
                case AFTER:
                    positionIndex = targetIndex + 1;
                    break;
                default:
                    assert false;
                    return result;
            }

            // Retrieve the target constraints
            final FXOMObject targetConstraints
                    = mask.getColumnConstraintsAtIndex(targetIndex);

            // The target index is associated to an existing constraints value :
            // we remove the target constraints and add it back at new position
            // No need to move the constraints of the column before/after :
            // indeed, they are automatically shifted while updating the target ones 
            if (targetConstraints != null) {
                // First remove current target constraints
                final Job removeValueJob = new RemoveObjectJob(
                        targetConstraints,
                        getEditorController());
                result.add(removeValueJob);

                // Then add the target constraints at new positionIndex
                final Job addValueJob = new AddPropertyValueJob(
                        targetConstraints,
                        (FXOMPropertyC) constraintsProperty,
                        positionIndex, getEditorController());
                result.add(addValueJob);
            }//
            // The target index is not associated to an existing constraints value :
            // we may need to move the constraints before the target one if any
            else if (position == Position.BEFORE) {
                // Retrieve the constraints before the target one
                final FXOMObject beforeConstraints
                        = mask.getColumnConstraintsAtIndex(targetIndex - 1);

                // The index before is associated to an existing constraints value :
                // we insert a new constraints with default values at the position index
                if (beforeConstraints != null) {
                    // Create new empty constraints for the target column
                    final FXOMInstance addedConstraints = makeColumnConstraintsInstance();
                    final Job addValueJob = new AddPropertyValueJob(
                            addedConstraints,
                            (FXOMPropertyC) constraintsProperty,
                            positionIndex, getEditorController());
                    result.add(addValueJob);
                }
            }
        }
        return result;
    }

    private List<Job> moveColumnContent() {

        final List<Job> result = new ArrayList<>();

        for (int targetIndex : targetIndexes) {

            switch (position) {
                case BEFORE:
                    // First move the target column content
                    result.add(new ReIndexColumnContentJob(
                            getEditorController(),
                            -1, targetGridPane, targetIndex));
                    int beforeIndex = targetIndex - 1;
                    // Then move the content of the column before the target one
                    // If the index before is not part of the target indexes (selected indexes),
                    // we move the column content as many times as consecutive target indexes
                    if (targetIndexes.contains(beforeIndex) == false) {
                        int shiftIndex = 1;
                        while (targetIndexes.contains(targetIndex + shiftIndex)) {
                            shiftIndex++;
                        }
                        result.add(new ReIndexColumnContentJob(
                                getEditorController(),
                                shiftIndex, targetGridPane, beforeIndex));
                    }
                    break;
                case AFTER:
                    // First move the target column content
                    result.add(new ReIndexColumnContentJob(
                            getEditorController(),
                            +1, targetGridPane, targetIndex));
                    int afterIndex = targetIndex + 1;
                    // Then move the content of the column after the target one
                    // If the index after is not part of the target indexes (selected indexes),
                    // we move the column content as many times as consecutive target indexes
                    if (targetIndexes.contains(afterIndex) == false) {
                        int shiftIndex = -1;
                        while (targetIndexes.contains(targetIndex + shiftIndex)) {
                            shiftIndex--;
                        }
                        result.add(new ReIndexColumnContentJob(
                                getEditorController(),
                                shiftIndex, targetGridPane, afterIndex));
                    }
                    break;
                default:
                    assert false;
            }
        }
        return result;
    }

    private FXOMInstance makeColumnConstraintsInstance() {

        // Create new constraints instance
        final FXOMDocument newDocument = new FXOMDocument();
        final FXOMInstance result
                = new FXOMInstance(newDocument, ColumnConstraints.class);
        newDocument.setFxomRoot(result);
        result.moveToFxomDocument(getEditorController().getFxomDocument());

        return result;
    }
}
